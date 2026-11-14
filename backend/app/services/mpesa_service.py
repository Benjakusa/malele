"""M-Pesa (Daraja) integration: STK Push, callback handling, status query."""
import structlog
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config.settings import get_settings
from app.credits import ledger
from app.models.credits import CreditPurchase
from daraja_sdk import MpesaClient, MpesaSTKError, MpesaQueryError


logger = structlog.get_logger()
_settings = get_settings()


def _client() -> MpesaClient:
    return MpesaClient(
        consumer_key=_settings.MPESA_CONSUMER_KEY,
        consumer_secret=_settings.MPESA_CONSUMER_SECRET,
        shortcode=_settings.MPESA_SHORTCODE,
        passkey=_settings.MPESA_PASSKEY,
        callback_url=_settings.MPESA_CALLBACK_URL,
        environment=_settings.MPESA_ENV,
    )


def normalize_phone(phone: str) -> str:
    """Normalize to 2547XXXXXXXX format. Accepts 0712345678, +254712345678, 254712345678."""
    p = phone.strip().replace(" ", "").replace("-", "")
    if p.startswith("+"):
        p = p[1:]
    if p.startswith("0"):
        p = "254" + p[1:]
    if not p.startswith("254") or len(p) != 12:
        raise ValueError(f"Invalid Kenyan phone: {phone}")
    return p


async def initiate_stk_push(
    db: AsyncSession,
    *,
    user_id: UUID,
    phone: str,
    amount_kes: int,
) -> CreditPurchase:
    """Start an STK Push and record a pending CreditPurchase row."""
    if amount_kes < 10:
        raise ValueError("Minimum top-up is KES 10")
    if amount_kes > 250_000:
        raise ValueError("Maximum top-up is KES 250,000")

    normalized = normalize_phone(phone)
    credits = int(amount_kes * _settings.CREDITS_PER_KES)
    if credits <= 0:
        raise ValueError("Top-up amount yields zero credits")

    ref = f"MALELE-{user_id.hex[:8]}"

    try:
        async with _client() as client:
            response = await client.stk_push(
                phone=normalized,
                amount=amount_kes,
                account_reference=ref,
                transaction_desc="Malele credits",
            )
    except MpesaSTKError as e:
        logger.error("mpesa_stk_push_failed", user_id=str(user_id), error=str(e)[:200])
        raise

    checkout_request_id = response.get("CheckoutRequestID") or ""
    merchant_request_id = response.get("MerchantRequestID") or ""
    if not checkout_request_id:
        raise ValueError(f"Daraja returned no CheckoutRequestID: {response}")

    purchase = CreditPurchase(
        user_id=user_id,
        checkout_request_id=checkout_request_id,
        merchant_request_id=merchant_request_id,
        phone=normalized,
        amount_kes=amount_kes,
        amount_credits=credits,
        status="pending",
    )
    db.add(purchase)
    await db.flush()

    logger.info(
        "mpesa_stk_push_started",
        user_id=str(user_id),
        checkout_request_id=checkout_request_id,
        amount_kes=amount_kes,
        credits=credits,
    )
    return purchase


async def handle_callback(db: AsyncSession, payload: dict) -> None:
    """Process a Daraja callback. Idempotent on MpesaReceiptNumber."""
    body = payload.get("Body", {}).get("stkCallback", {})
    checkout_id = body.get("CheckoutRequestID")
    merchant_id = body.get("MerchantRequestID")
    result_code = body.get("ResultCode")
    result_desc = body.get("ResultDesc", "")

    if not checkout_id:
        logger.warning("mpesa_callback_missing_checkout_id")
        return

    result = await db.execute(
        select(CreditPurchase).where(CreditPurchase.checkout_request_id == checkout_id)
    )
    purchase = result.scalar_one_or_none()
    if purchase is None:
        logger.warning("mpesa_callback_unknown_checkout", checkout_id=checkout_id)
        return

    if purchase.status == "completed":
        logger.info("mpesa_callback_already_completed", checkout_id=checkout_id)
        return

    purchase.result_code = result_code
    purchase.result_desc = result_desc

    if result_code != 0:
        purchase.status = "failed"
        await db.flush()
        logger.info("mpesa_payment_failed", checkout_id=checkout_id, result_code=result_code)
        return

    # Success — extract receipt from CallbackMetadata
    meta_items = body.get("CallbackMetadata", {}).get("Item", [])
    receipt = None
    for item in meta_items:
        if item.get("Name") == "MpesaReceiptNumber":
            receipt = str(item.get("Value"))
            break

    if not receipt:
        logger.error("mpesa_callback_no_receipt", checkout_id=checkout_id)
        return

    purchase.mpesa_receipt = receipt
    purchase.status = "completed"

    await ledger.grant(
        db,
        user_id=purchase.user_id,
        amount=purchase.amount_credits,
        reason=f"mpesa_topup:{receipt}",
    )
    await db.flush()

    logger.info(
        "mpesa_payment_completed",
        checkout_id=checkout_id,
        receipt=receipt,
        credits=purchase.amount_credits,
        user_id=str(purchase.user_id),
    )


async def query_and_reconcile(db: AsyncSession, *, checkout_request_id: str) -> str:
    """Fallback (A+C): query Daraja directly if the callback never arrived."""
    result = await db.execute(
        select(CreditPurchase).where(CreditPurchase.checkout_request_id == checkout_request_id)
    )
    purchase = result.scalar_one_or_none()
    if purchase is None:
        return "not_found"
    if purchase.status == "completed":
        return "completed"

    try:
        async with _client() as client:
            q = await client.query_stk(checkout_request_id=checkout_request_id)
    except MpesaQueryError as e:
        logger.error("mpesa_query_failed", checkout_id=checkout_request_id, error=str(e)[:200])
        return "query_failed"

    if q.result_code == 0:
        purchase.status = "completed"
        purchase.result_code = q.result_code
        await ledger.grant(
            db,
            user_id=purchase.user_id,
            amount=purchase.amount_credits,
            reason=f"mpesa_topup:reconciled:{checkout_request_id}",
        )
        await db.flush()
        logger.info("mpesa_reconciled", checkout_id=checkout_request_id)
        return "completed"

    purchase.status = "failed"
    purchase.result_code = q.result_code
    await db.flush()
    return "failed"

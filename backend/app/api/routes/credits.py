"""Credit purchase and balance endpoints (M-Pesa top-up)."""
import structlog
from typing import Annotated

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.errors import ApiError
from app.api.routes.auth import get_current_user
from app.credits import ledger
from app.database.session import get_db
from app.models.auth import User
from app.models.credits import CreditAccount, CreditPurchase
from app.services import mpesa_service
from daraja_sdk import MpesaSTKError, MpesaQueryError


logger = structlog.get_logger()
router = APIRouter(prefix="/v1/credits", tags=["credits"])


class PurchaseRequest(BaseModel):
    phone: str = Field(min_length=9, max_length=15)
    amount_kes: int = Field(ge=10, le=250_000)


class PurchaseResponse(BaseModel):
    checkout_request_id: str
    amount_kes: int
    amount_credits: int
    message: str = "Enter your M-Pesa PIN to complete payment."


class VerifyRequest(BaseModel):
    checkout_request_id: str


class VerifyResponse(BaseModel):
    status: str


class BalanceResponse(BaseModel):
    balance: int
    reserved: int
    available: int


class CallbackAck(BaseModel):
    ResultCode: int = 0
    ResultDesc: str = "Accepted"


@router.get("/balance", response_model=BalanceResponse)
async def get_balance(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> BalanceResponse:
    """Return the user's current credit balance (for app-side polling)."""
    account = await ledger.get_or_create_account(db, user.id)
    return BalanceResponse(
        balance=account.balance,
        reserved=account.reserved,
        available=account.balance - account.reserved,
    )


@router.post("/purchase", response_model=PurchaseResponse, status_code=201)
async def purchase_credits(
    body: PurchaseRequest,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> PurchaseResponse:
    """Initiate an STK Push to top up credits."""
    try:
        purchase = await mpesa_service.initiate_stk_push(
            db,
            user_id=user.id,
            phone=body.phone,
            amount_kes=body.amount_kes,
        )
    except ValueError as e:
        raise ApiError("INVALID_PURCHASE_REQUEST", str(e), 400) from e
    except MpesaSTKError as e:
        logger.error("mpesa_stk_error", error=str(e)[:200])
        raise ApiError(
            "MPESA_UNAVAILABLE",
            "M-Pesa is currently unavailable. Please try again in a moment.",
            502,
        ) from e

    await db.commit()

    return PurchaseResponse(
        checkout_request_id=purchase.checkout_request_id,
        amount_kes=purchase.amount_kes,
        amount_credits=purchase.amount_credits,
    )


@router.post("/callback", response_model=CallbackAck)
async def mpesa_callback(
    body: dict,
    db: Annotated[AsyncSession, Depends(get_db)],
) -> CallbackAck:
    """Safaricom POSTs payment result here. Public — no auth.

    Returns 200 with ResultCode=0 to stop Safaricom retries. Idempotent
    on MpesaReceiptNumber via unique index.
    """
    try:
        await mpesa_service.handle_callback(db, body)
        await db.commit()
        return CallbackAck(ResultCode=0, ResultDesc="Accepted")
    except Exception as e:
        logger.error("mpesa_callback_error", error=str(e)[:300])
        await db.rollback()
        # Still return 200 — Safaricom will retry if we return 500, which
        # could cause double-processing. Our handler is idempotent anyway.
        return CallbackAck(ResultCode=0, ResultDesc="Accepted")


@router.post("/verify", response_model=VerifyResponse)
async def verify_purchase(
    body: VerifyRequest,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> VerifyResponse:
    """Fallback for A+C: query Daraja directly if callback never arrived."""
    # Ownership check — user can only verify their own purchases
    result = await db.execute(
        select(CreditPurchase).where(
            CreditPurchase.checkout_request_id == body.checkout_request_id,
            CreditPurchase.user_id == user.id,
        )
    )
    if result.scalar_one_or_none() is None:
        raise ApiError("PURCHASE_NOT_FOUND", "No such purchase for this user", 404)

    try:
        status = await mpesa_service.query_and_reconcile(
            db, checkout_request_id=body.checkout_request_id,
        )
        await db.commit()
        return VerifyResponse(status=status)
    except MpesaQueryError as e:
        logger.error("mpesa_verify_error", error=str(e)[:200])
        raise ApiError("MPESA_QUERY_FAILED", "Could not verify with M-Pesa", 502) from e

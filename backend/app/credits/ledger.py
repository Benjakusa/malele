from datetime import UTC, datetime
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.credits import CreditAccount, CreditReservation, CreditTransaction


class CreditError(Exception):
    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code
        self.message = message


async def get_or_create_account(db: AsyncSession, user_id: UUID) -> CreditAccount:
    result = await db.execute(
        select(CreditAccount).where(CreditAccount.user_id == user_id).with_for_update()
    )
    account = result.scalar_one_or_none()
    if account is None:
        account = CreditAccount(user_id=user_id, balance=0, reserved=0)
        db.add(account)
        await db.flush()
    return account


async def reserve(db: AsyncSession, *, user_id: UUID, job_id: UUID, amount: int) -> CreditReservation:
    """Hold credits for a pending job. Raises CreditError if insufficient."""
    if amount <= 0:
        raise CreditError("CREDITS_INVALID_AMOUNT", "Reserve amount must be positive")
    existing = await db.execute(
        select(CreditReservation).where(CreditReservation.job_id == job_id)
    )
    if existing.scalar_one_or_none() is not None:
        raise CreditError("CREDITS_DUPLICATE_RESERVATION", "Reservation already exists for this job")
    account = await get_or_create_account(db, user_id)
    available = account.balance - account.reserved
    if available < amount:
        raise CreditError("CREDITS_INSUFFICIENT", f"Need {amount}, available {available}")
    account.reserved += amount
    reservation = CreditReservation(user_id=user_id, job_id=job_id, amount=amount)
    db.add(reservation)
    await db.flush()
    return reservation


async def debit_reserved(db: AsyncSession, *, job_id: UUID, reason: str = "generation_charge") -> None:
    """Convert a reservation into an actual charge. Idempotent."""
    result = await db.execute(
        select(CreditReservation).where(CreditReservation.job_id == job_id).with_for_update()
    )
    reservation = result.scalar_one_or_none()
    if reservation is None:
        raise CreditError("CREDITS_NO_RESERVATION", "No reservation to debit")
    if reservation.charged_at is not None:
        return  # already charged — idempotent
    if reservation.released_at is not None:
        raise CreditError("CREDITS_ALREADY_RELEASED", "Reservation was already released")
    account_result = await db.execute(
        select(CreditAccount).where(CreditAccount.user_id == reservation.user_id).with_for_update()
    )
    account = account_result.scalar_one()
    account.reserved -= reservation.amount
    account.balance -= reservation.amount
    reservation.charged_at = datetime.now(UTC)
    db.add(CreditTransaction(
        user_id=reservation.user_id,
        created_at=datetime.now(UTC),
        delta=-reservation.amount,
        reason=reason,
        reference_id=job_id,
    ))
    await db.flush()


async def refund_reserved(db: AsyncSession, *, job_id: UUID, reason: str = "generation_refund") -> None:
    """Release a reservation without charging. Idempotent."""
    result = await db.execute(
        select(CreditReservation).where(CreditReservation.job_id == job_id).with_for_update()
    )
    reservation = result.scalar_one_or_none()
    if reservation is None:
        return  # nothing to refund — idempotent
    if reservation.released_at is not None:
        return  # already refunded — idempotent
    if reservation.charged_at is not None:
        raise CreditError("CREDITS_ALREADY_CHARGED", "Cannot refund a charged reservation")
    account_result = await db.execute(
        select(CreditAccount).where(CreditAccount.user_id == reservation.user_id).with_for_update()
    )
    account = account_result.scalar_one()
    account.reserved -= reservation.amount
    reservation.released_at = datetime.now(UTC)
    await db.flush()


async def grant(db: AsyncSession, *, user_id: UUID, amount: int, reason: str) -> None:
    """Give credits to a user (signup bonus, admin adjustment, etc)."""
    account = await get_or_create_account(db, user_id)
    account.balance += amount
    db.add(CreditTransaction(
        user_id=user_id,
        created_at=datetime.now(UTC),
        delta=amount,
        reason=reason,
    ))
    await db.flush()

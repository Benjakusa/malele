import pytest
from httpx import AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config.settings import get_settings
from app.credits import ledger
from app.models.credits import CreditAccount, CreditReservation, CreditTransaction
from app.models.jobs import GenerationJob, JobState
from app.schemas.auth import RegisterRequest

pytestmark = pytest.mark.asyncio


async def _register_and_get_token(client: AsyncClient, db: AsyncSession) -> tuple[str, str]:
    """Register a user, grant signup credits, return (token, user_id)."""
    settings = get_settings()
    email = f"gen-test-{id(db)}@malele.app"
    r = await client.post("/v1/auth/register", json={
        "email": email,
        "password": "password123",
    })
    assert r.status_code == 201, r.text
    body = r.json()
    user_id = body["user"]["id"]
    from uuid import UUID
    await ledger.grant(db, user_id=UUID(user_id), amount=settings.CREDITS_SIGNUP_BONUS, reason="signup_bonus")
    await db.commit()
    return body["tokens"]["access_token"], user_id


async def test_submit_generation_reserves_credits(client: AsyncClient, db: AsyncSession) -> None:
    token, user_id = await _register_and_get_token(client, db)
    settings = get_settings()
    r = await client.post(
        "/v1/generations",
        json={"prompt": "A Benga love song", "genre": "benga", "duration_seconds": 60},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert r.status_code == 201, r.text
    body = r.json()
    assert body["job"]["state"] == "queued"
    assert body["job"]["credits_reserved"] == settings.CREDITS_COST_PER_SONG
    from uuid import UUID
    acc = (await db.execute(
        select(CreditAccount).where(CreditAccount.user_id == UUID(user_id))
    )).scalar_one()
    assert acc.reserved == settings.CREDITS_COST_PER_SONG
    assert acc.balance == settings.CREDITS_SIGNUP_BONUS


async def test_poll_completes_and_debits_credits(client: AsyncClient, db: AsyncSession) -> None:
    import asyncio
    token, user_id = await _register_and_get_token(client, db)
    settings = get_settings()
    r = await client.post(
        "/v1/generations",
        json={"prompt": "test"},
        headers={"Authorization": f"Bearer {token}"},
    )
    job_id = r.json()["job"]["id"]
    await asyncio.sleep(2.2)  # mock provider PROCESSING_SECONDS = 2
    r2 = await client.post(
        f"/v1/generations/{job_id}/poll",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert r2.status_code == 200, r2.text
    assert r2.json()["state"] == "completed"
    from uuid import UUID
    acc = (await db.execute(
        select(CreditAccount).where(CreditAccount.user_id == UUID(user_id))
    )).scalar_one()
    assert acc.reserved == 0
    assert acc.balance == settings.CREDITS_SIGNUP_BONUS - settings.CREDITS_COST_PER_SONG


async def test_refund_on_failure(client: AsyncClient, db: AsyncSession) -> None:
    """Cancel a queued job → credits refunded, no charge."""
    token, user_id = await _register_and_get_token(client, db)
    settings = get_settings()
    r = await client.post(
        "/v1/generations",
        json={"prompt": "test refund"},
        headers={"Authorization": f"Bearer {token}"},
    )
    job_id = r.json()["job"]["id"]
    r2 = await client.post(
        f"/v1/generations/{job_id}/cancel",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert r2.status_code == 200
    assert r2.json()["state"] == "cancelled"
    from uuid import UUID
    acc = (await db.execute(
        select(CreditAccount).where(CreditAccount.user_id == UUID(user_id))
    )).scalar_one()
    assert acc.reserved == 0
    assert acc.balance == settings.CREDITS_SIGNUP_BONUS  # no charge
    # No debit transaction should exist for this job
    txns = (await db.execute(
        select(CreditTransaction).where(CreditTransaction.reference_id == UUID(job_id))
    )).scalars().all()
    assert len(txns) == 0


async def test_insufficient_credits_returns_402(client: AsyncClient, db: AsyncSession) -> None:
    """User with 0 credits cannot submit."""
    email = f"broke-{id(db)}@malele.app"
    r = await client.post("/v1/auth/register", json={
        "email": email,
        "password": "password123",
    })
    token = r.json()["tokens"]["access_token"]
    # No grant — 0 credits
    r = await client.post(
        "/v1/generations",
        json={"prompt": "should fail"},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert r.status_code == 402, r.text
    assert r.json()["error"]["code"] == "CREDITS_INSUFFICIENT"


async def test_songs_empty_for_new_user(client: AsyncClient, db: AsyncSession) -> None:
    token, _ = await _register_and_get_token(client, db)
    r = await client.get(
        "/v1/songs",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert r.status_code == 200
    assert r.json() == []


async def test_duplicate_reservation_prevented(client: AsyncClient, db: AsyncSession) -> None:
    """Calling reserve twice for the same job_id raises CreditError."""
    from uuid import UUID
    from app.models.jobs import GenerationJob, JobState
    token, user_id = await _register_and_get_token(client, db)
    # Create a real job so the FK is satisfied
    real_job = GenerationJob(
        user_id=UUID(user_id),
        state=JobState.QUEUED,
        provider="mock",
        prompt_original="x",
        request_payload={},
        credits_reserved=5,
    )
    db.add(real_job)
    await db.flush()
    await ledger.reserve(db, user_id=UUID(user_id), job_id=real_job.id, amount=5)
    await db.commit()
    import pytest as _pytest
    with _pytest.raises(ledger.CreditError) as exc_info:
        await ledger.reserve(db, user_id=UUID(user_id), job_id=real_job.id, amount=5)
    assert exc_info.value.code == "CREDITS_DUPLICATE_RESERVATION"

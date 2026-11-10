from datetime import UTC, datetime
from uuid import UUID, uuid4

from sqlalchemy.ext.asyncio import AsyncSession

from app.config.settings import get_settings
from app.credits import ledger
from app.models.jobs import GenerationJob, JobEvent, JobState
from app.providers.base import (
    GenerationRequest,
    ProviderJobRef,
    ProviderJobState,
    ProviderKind,
)
from app.providers.registry import get_registry


_settings = get_settings()


class GenerationError(Exception):
    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code
        self.message = message


async def _select_provider(request: GenerationRequest):
    """Ask the registry for a provider that satisfies the request."""
    registry = get_registry()
    order = None
    if request.preferred_provider is not None:
        order = [request.preferred_provider, ProviderKind.ELEVENLABS, ProviderKind.FAL, ProviderKind.MOCK]
    return await registry.select(request, preferred_order=order)


def _get_provider_by_kind(kind: ProviderKind):
    """Return the live provider instance for a given kind (used during polling)."""
    registry = get_registry()
    provider = registry.get(kind)
    if provider is None:
        raise GenerationError("PROVIDER_NOT_FOUND", f"Provider {kind.value} is not registered")
    return provider


async def submit(
    db: AsyncSession,
    *,
    user_id: UUID,
    prompt: str,
    genre: str | None = None,
    vocals: bool = True,
    instrumental: bool = False,
    lyrics: str | None = None,
    duration_seconds: int = 120,
    language: str | None = None,
) -> GenerationJob:
    """Reserve credits and create a queued generation job."""
    if not _settings.GENERATION_ENABLED:
        raise GenerationError("GENERATION_DISABLED", "Generation is currently disabled")

    job_id = uuid4()
    cost = _settings.CREDITS_COST_PER_SONG

    # Create the job row FIRST so credit_reservations.job_id FK is satisfied
    job = GenerationJob(
        id=job_id,
        user_id=user_id,
        state=JobState.QUEUED,
        provider="pending",
        provider_job_id=None,
        prompt_original=prompt,
        request_payload={
            "genre": genre,
            "vocals": vocals,
            "instrumental": instrumental,
            "lyrics": lyrics,
            "duration_seconds": duration_seconds,
            "language": language,
        },
        credits_reserved=cost,
    )
    db.add(job)
    await db.flush()

    try:
        await ledger.reserve(db, user_id=user_id, job_id=job_id, amount=cost)
    except ledger.CreditError as e:
        await db.rollback()
        raise GenerationError(e.code, e.message) from e

    req = GenerationRequest(
        prompt=prompt,
        genre=genre,
        vocals=vocals,
        instrumental=instrumental,
        lyrics=lyrics,
        duration_seconds=duration_seconds,
        language=language,
    )

    try:
        provider = await _select_provider(req)
    except Exception as e:
        await ledger.refund_reserved(db, job_id=job_id)
        await db.rollback()
        raise GenerationError("NO_PROVIDER_AVAILABLE", str(e)) from e

    try:
        ref: ProviderJobRef = await provider.submit(req)
    except Exception as e:
        await ledger.refund_reserved(db, job_id=job_id)
        raise GenerationError("PROVIDER_SUBMIT_FAILED", str(e)) from e

    job.provider = ref.provider.value
    job.provider_job_id = ref.provider_job_id
    db.add(JobEvent(
        job_id=job_id,
        created_at=datetime.now(UTC),
        event="submitted",
        detail={"provider": ref.provider.value, "cost": cost},
    ))
    await db.flush()
    return job


async def poll_and_complete(db: AsyncSession, *, job_id: UUID) -> GenerationJob:
    """Poll provider. On COMPLETED, debit + fetch. On FAILED, refund."""
    result = await db.get(GenerationJob, job_id)
    if result is None:
        raise GenerationError("JOB_NOT_FOUND", f"Job {job_id} not found")
    job = result
    if job.state in (JobState.COMPLETED, JobState.FAILED, JobState.CANCELLED):
        return job

    provider = _get_provider_by_kind(ref_kind(job.provider))
    ref = ProviderJobRef(provider=ref_kind(job.provider), provider_job_id=job.provider_job_id or "")
    status = await provider.poll(ref)

    if status.state == ProviderJobState.PROCESSING:
        job.state = JobState.PROCESSING
    elif status.state == ProviderJobState.COMPLETED:
        try:
            result_data = await provider.fetch(ref)
        except Exception as e:
            job.state = JobState.FAILED
            job.error_code = "PROVIDER_FETCH_FAILED"
            job.error_message = str(e)
            await ledger.refund_reserved(db, job_id=job_id)
            return job
        job.state = JobState.COMPLETED
        job.result_payload = {
            "audio_size": len(result_data.audio_bytes),
            "content_type": result_data.content_type,
            "duration_seconds": result_data.duration_seconds,
            "metadata": result_data.metadata,
        }
        job.credits_charged = job.credits_reserved
        await ledger.debit_reserved(db, job_id=job_id)
        db.add(JobEvent(
            job_id=job_id,
            created_at=datetime.now(UTC),
            event="completed",
            detail={"duration_seconds": result_data.duration_seconds},
        ))
    elif status.state == ProviderJobState.FAILED:
        job.state = JobState.FAILED
        job.error_code = "PROVIDER_FAILED"
        job.error_message = status.error or "unknown"
        await ledger.refund_reserved(db, job_id=job_id)

    await db.flush()
    return job


def ref_kind(name: str):
    from app.providers.base import ProviderKind
    return ProviderKind(name)

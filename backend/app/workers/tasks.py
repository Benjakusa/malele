import asyncio
import structlog
from uuid import UUID

from sqlalchemy.ext.asyncio import AsyncSession

from app.database.session import AsyncSessionLocal
from app.services import generation_service
from app.workers.celery_app import celery_app


logger = structlog.get_logger()


def _run_async(coro):
    """Run an async coroutine from a sync Celery task."""
    return asyncio.get_event_loop().run_until_complete(coro)


@celery_app.task(
    name="generation.poll_job",
    bind=True,
    max_retries=5,
    default_retry_delay=5,
    acks_late=True,
)
def poll_generation_job(self, job_id: str) -> dict:
    """Poll a provider job and update DB. Idempotent — safe to retry."""
    async def _inner() -> dict:
        async with AsyncSessionLocal() as db:
            try:
                job = await generation_service.poll_and_complete(db, job_id=UUID(job_id))
                await db.commit()
                logger.info(
                    "job_poll_success",
                    job_id=str(job.id),
                    state=job.state.value,
                )
                return {"job_id": str(job.id), "state": job.state.value}
            except generation_service.GenerationError as e:
                await db.rollback()
                logger.error("job_poll_generation_error", job_id=job_id, code=e.code)
                raise
            except Exception as e:
                await db.rollback()
                logger.error("job_poll_unexpected", job_id=job_id, error=str(e))
                raise self.retry(exc=e)

    return _run_async(_inner())

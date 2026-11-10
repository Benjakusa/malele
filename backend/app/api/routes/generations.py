from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.errors import ApiError
from app.api.routes.auth import get_current_user
from app.database.session import get_db
from app.models.auth import User
from app.models.jobs import GenerationJob
from app.models.songs import Song
from app.schemas.generation import (
    GenerationJobOut,
    GenerationSubmitRequest,
    GenerationSubmitResponse,
    SongOut,
)
from app.services import generation_service


router = APIRouter(prefix="/v1", tags=["generation"])


async def _job_for_user(db: AsyncSession, job_id: UUID, user_id: UUID) -> GenerationJob:
    job = await db.get(GenerationJob, job_id)
    if job is None or job.user_id != user_id:
        raise ApiError("RESOURCE_NOT_FOUND", "Job not found", 404)
    return job


@router.post("/generations", response_model=GenerationSubmitResponse, status_code=201)
async def submit_generation(
    body: GenerationSubmitRequest,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> GenerationSubmitResponse:
    try:
        job = await generation_service.submit(
            db,
            user_id=user.id,
            prompt=body.prompt,
            genre=body.genre,
            vocals=body.vocals,
            instrumental=body.instrumental,
            lyrics=body.lyrics,
            duration_seconds=body.duration_seconds,
            language=body.language,
        )
    except generation_service.GenerationError as e:
        status = 402 if e.code == "CREDITS_INSUFFICIENT" else 400
        raise ApiError(e.code, e.message, status) from e
    await db.refresh(job, ["updated_at", "events"])
    return GenerationSubmitResponse(job=GenerationJobOut.model_validate(job))


@router.get("/generations/{job_id}", response_model=GenerationJobOut)
async def get_job(
    job_id: UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> GenerationJobOut:
    job = await _job_for_user(db, job_id, user.id)
    await db.refresh(job, ["updated_at", "events"])
    return GenerationJobOut.model_validate(job)


@router.post("/generations/{job_id}/poll", response_model=GenerationJobOut)
async def poll_job(
    job_id: UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> GenerationJobOut:
    await _job_for_user(db, job_id, user.id)
    try:
        job = await generation_service.poll_and_complete(db, job_id=job_id)
    except generation_service.GenerationError as e:
        raise ApiError(e.code, e.message, 400) from e
    await db.refresh(job, ["updated_at", "events"])
    return GenerationJobOut.model_validate(job)


@router.post("/generations/{job_id}/cancel", response_model=GenerationJobOut)
async def cancel_job(
    job_id: UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> GenerationJobOut:
    job = await _job_for_user(db, job_id, user.id)
    if job.state in ("completed", "failed", "cancelled"):
        return GenerationJobOut.model_validate(job)
    job.state = "cancelled"
    from app.credits import ledger
    await ledger.refund_reserved(db, job_id=job_id)
    await db.flush()
    await db.refresh(job, ["updated_at", "events"])
    return GenerationJobOut.model_validate(job)


@router.get("/songs", response_model=list[SongOut])
async def list_songs(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
) -> list[SongOut]:
    result = await db.execute(
        select(Song).where(Song.user_id == user.id)
        .order_by(Song.created_at.desc())
        .limit(limit).offset(offset)
    )
    return [SongOut.model_validate(s) for s in result.scalars().all()]


@router.get("/songs/{song_id}", response_model=SongOut)
async def get_song(
    song_id: UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> SongOut:
    song = await db.get(Song, song_id)
    if song is None or song.user_id != user.id:
        raise ApiError("RESOURCE_NOT_FOUND", "Song not found", 404)
    return SongOut.model_validate(song)

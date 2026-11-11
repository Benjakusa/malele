from typing import Annotated
from uuid import UUID

from pathlib import Path

from fastapi import APIRouter, Depends, Query
from fastapi.responses import FileResponse
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.errors import ApiError
from app.api.routes.auth import get_current_user
from app.database.session import get_db
from app.models.auth import User
from app.models.jobs import GenerationJob
from app.models.songs import Song
from app.providers.base import SectionRequest
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
        sections = None
        if body.sections:
            sections = [
                SectionRequest(
                    name=s.name,
                    duration_seconds=s.duration_seconds,
                    lyrics=s.lyrics,
                    instruments=s.instruments,
                    vocals=s.vocals,
                    mood=s.mood,
                    negative=s.negative,
                )
                for s in body.sections
            ]
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
            sections=sections,
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

@router.get("/generations/{job_id}/audio")
async def get_generation_audio(
    job_id: UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> FileResponse:
    """Serve the generated audio file for playback."""
    job = await _job_for_user(db, job_id, user.id)
    if job.state != "completed":
        raise ApiError("JOB_NOT_COMPLETED", f"Job state is {job.state}", 409)
    audio_path_str = (job.result_payload or {}).get("audio_path")
    if not audio_path_str:
        raise ApiError("AUDIO_NOT_FOUND", "No audio associated with this job", 404)
    audio_path = Path(audio_path_str)
    if not audio_path.exists():
        raise ApiError("AUDIO_FILE_MISSING", "Audio file was deleted or moved", 404)
    media_type = "audio/wav" if audio_path.suffix == ".wav" else "audio/mpeg"
    return FileResponse(
        path=str(audio_path),
        media_type=media_type,
        filename=f"malele_{str(job_id)[:8]}{audio_path.suffix}",
    )

from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.errors import ApiError
from app.api.routes.auth import get_current_user
from app.database.session import get_db
from app.models.auth import User
from app.schemas.prompt import (
    EnhancePromptRequest,
    EnhancePromptResponse,
    GenerateLyricsRequest,
    GenerateLyricsResponse,
)
from app.services import prompt_service


router = APIRouter(prefix="/v1", tags=["prompt"])


@router.post("/prompt/enhance", response_model=EnhancePromptResponse)
async def enhance_prompt(
    body: EnhancePromptRequest,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> EnhancePromptResponse:
    try:
        enhanced = await prompt_service.enhance(
            db,
            user_prompt=body.user_prompt,
            genre=body.genre,
            mood=body.mood,
        )
    except prompt_service.PromptError as e:
        raise ApiError(e.code, e.message, 400) from e
    return EnhancePromptResponse(original=body.user_prompt, enhanced=enhanced)


@router.post("/lyrics/generate", response_model=GenerateLyricsResponse)
async def generate_lyrics(
    body: GenerateLyricsRequest,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> GenerateLyricsResponse:
    try:
        sections = await prompt_service.generate_lyrics(
            db,
            theme=body.theme,
            genre=body.genre,
            language=body.language,
            mood=body.mood,
            sections=body.sections,
        )
    except prompt_service.PromptError as e:
        raise ApiError(e.code, e.message, 400) from e
    return GenerateLyricsResponse(sections=sections)

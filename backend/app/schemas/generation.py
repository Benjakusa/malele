from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, Field


class GenerationSubmitRequest(BaseModel):
    prompt: str = Field(min_length=1, max_length=2000)
    genre: str | None = None
    vocals: bool = True
    instrumental: bool = False
    lyrics: str | None = Field(default=None, max_length=10000)
    duration_seconds: int = Field(default=120, ge=3, le=600)
    language: str | None = None


class JobEventOut(BaseModel):
    event: str
    created_at: datetime
    detail: dict

    model_config = {"from_attributes": True}


class GenerationJobOut(BaseModel):
    id: UUID
    state: str
    provider: str
    prompt_original: str
    credits_reserved: int
    credits_charged: int
    error_code: str | None
    error_message: str | None
    created_at: datetime
    updated_at: datetime
    events: list[JobEventOut] = []

    model_config = {"from_attributes": True}


class GenerationSubmitResponse(BaseModel):
    job: GenerationJobOut


class SongOut(BaseModel):
    id: UUID
    title: str
    genre: str | None
    mood: str | None
    language: str | None
    is_favourite: bool
    is_public: bool
    created_at: datetime

    model_config = {"from_attributes": True}

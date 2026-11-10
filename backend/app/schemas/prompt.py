from pydantic import BaseModel, Field


class EnhancePromptRequest(BaseModel):
    user_prompt: str = Field(min_length=1, max_length=2000)
    genre: str | None = None
    mood: str | None = None


class EnhancePromptResponse(BaseModel):
    original: str
    enhanced: str


class GenerateLyricsRequest(BaseModel):
    theme: str = Field(min_length=1, max_length=500)
    genre: str | None = None
    language: str = "en"
    mood: str | None = None
    sections: list[str] | None = None


class GenerateLyricsResponse(BaseModel):
    sections: dict[str, str]

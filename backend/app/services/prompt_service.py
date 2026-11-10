import structlog

from sqlalchemy.ext.asyncio import AsyncSession

from app.providers.lyrics.anthropic import AnthropicError, AnthropicLyricsProvider


logger = structlog.get_logger()


class PromptError(Exception):
    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code
        self.message = message


_provider = AnthropicLyricsProvider()


async def enhance(
    db: AsyncSession,
    *,
    user_prompt: str,
    genre: str | None = None,
    mood: str | None = None,
) -> str:
    """Enhance a simple prompt into a detailed music-generation prompt."""
    if not user_prompt or not user_prompt.strip():
        raise PromptError("PROMPT_EMPTY", "Prompt cannot be empty")
    if len(user_prompt) > 2000:
        raise PromptError("PROMPT_TOO_LONG", "Prompt exceeds 2000 characters")

    try:
        enhanced = await _provider.enhance_prompt(
            user_prompt=user_prompt,
            genre=genre,
            mood=mood,
        )
    except AnthropicError as e:
        logger.error("prompt_enhance_failed", error=str(e)[:200])
        raise PromptError("PROMPT_ENHANCE_FAILED", str(e)[:200]) from e

    if not enhanced or len(enhanced) < 20:
        raise PromptError("PROMPT_ENHANCE_EMPTY", "Enhancement returned empty response")

    return enhanced


async def generate_lyrics(
    db: AsyncSession,
    *,
    theme: str,
    genre: str | None = None,
    language: str = "en",
    mood: str | None = None,
    sections: list[str] | None = None,
) -> dict[str, str]:
    """Generate structured lyrics via the Anthropic provider."""
    if not theme or not theme.strip():
        raise PromptError("LYRICS_EMPTY_THEME", "Theme cannot be empty")

    try:
        lyrics = await _provider.generate_lyrics(
            theme=theme,
            genre=genre,
            language=language,
            mood=mood,
            sections=sections,
        )
    except AnthropicError as e:
        logger.error("lyrics_generate_failed", error=str(e)[:200])
        raise PromptError("LYRICS_GENERATE_FAILED", str(e)[:200]) from e

    return lyrics

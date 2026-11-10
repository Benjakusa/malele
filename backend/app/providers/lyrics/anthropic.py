import structlog

import httpx

from app.config.settings import get_settings


logger = structlog.get_logger()


class AnthropicError(Exception):
    def __init__(self, message: str, status_code: int | None = None) -> None:
        super().__init__(message)
        self.status_code = status_code


class AnthropicLyricsProvider:
    """Anthropic Claude lyrics + prompt enhancement adapter (contract per API_INTEGRATIONS.md §4)."""

    BASE_URL = "https://api.anthropic.com/v1/messages"
    API_VERSION = "2023-06-01"

    def __init__(self) -> None:
        self._settings = get_settings()

    def _headers(self) -> dict[str, str]:
        return {
            "x-api-key": self._settings.ANTHROPIC_API_KEY,
            "anthropic-version": self.API_VERSION,
            "content-type": "application/json",
        }

    def _model(self) -> str:
        return self._settings.ANTHROPIC_MODEL or "claude-sonnet-4-5-20250929"


    async def generate_lyrics(
        self,
        *,
        theme: str,
        genre: str | None = None,
        language: str = "en",
        mood: str | None = None,
        sections: list[str] | None = None,
    ) -> dict[str, str]:
        """Generate structured lyrics. Returns {section_name: lyrics_text}."""
        sections = sections or ["verse1", "chorus", "verse2", "bridge", "outro"]
        system_prompt = (
            "You are a professional songwriter. Generate original song lyrics. "
            "Do NOT reproduce copyrighted lyrics, do NOT reference real artists or songs. "
            f"Write the lyrics in this language: {language}. "
            "Return ONLY a JSON object with one key per section, each value being the lyrics text."
        )
        user_prompt = (
            f"Theme: {theme}\n"
            f"Genre: {genre or 'unspecified'}\n"
            f"Mood: {mood or 'unspecified'}\n"
            f"Sections to write: {', '.join(sections)}\n"
            "Return JSON."
        )

        payload = {
            "model": self._model(),
            "max_tokens": 2000,
            "system": system_prompt,
            "messages": [{"role": "user", "content": user_prompt}],
            "tools": [
                {
                    "name": "emit_lyrics",
                    "description": "Emit the generated lyrics as structured JSON",
                    "input_schema": {
                        "type": "object",
                        "properties": {s: {"type": "string"} for s in sections},
                        "required": sections,
                    },
                }
            ],
            "tool_choice": {"type": "tool", "name": "emit_lyrics"},
        }

        logger.info("anthropic_lyrics_start", theme_len=len(theme), language=language)
        try:
            async with httpx.AsyncClient(timeout=120.0) as client:
                resp = await client.post(self.BASE_URL, json=payload, headers=self._headers())
        except httpx.RequestError as e:
            raise AnthropicError(f"Network error: {e}") from e

        if resp.status_code != 200:
            raise AnthropicError(
                f"Anthropic returned {resp.status_code}: {resp.text[:200]}",
                status_code=resp.status_code,
            )

        body = resp.json()
        tool_use = next(
            (b for b in body.get("content", []) if b.get("type") == "tool_use"),
            None,
        )
        if tool_use is None:
            raise AnthropicError("Anthropic did not return tool_use content")

        lyrics = tool_use.get("input", {})
        logger.info("anthropic_lyrics_ok", sections=len(lyrics))
        return lyrics

    async def enhance_prompt(
        self,
        *,
        user_prompt: str,
        genre: str | None = None,
        mood: str | None = None,
    ) -> str:
        """Expand a simple prompt into a detailed music-generation prompt."""
        system_prompt = (
            "You are a music production assistant. Expand the user request into a detailed "
            "music-generation prompt for an AI music model. Include: instrumentation, "
            "rhythm, tempo range, arrangement, vocal characteristics. "
            "Do NOT name real artists. Do NOT reference copyrighted songs. "
            "Return ONLY the enhanced prompt as plain text, 50-200 words."
        )
        user_prompt_full = (
            f"Original request: {user_prompt}\n"
            f"Genre: {genre or 'unspecified'}\n"
            f"Mood: {mood or 'unspecified'}\n"
        )
        payload = {
            "model": self._model(),
            "max_tokens": 500,
            "system": system_prompt,
            "messages": [{"role": "user", "content": user_prompt_full}],
        }

        logger.info("anthropic_enhance_start", prompt_len=len(user_prompt))
        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                resp = await client.post(self.BASE_URL, json=payload, headers=self._headers())
        except httpx.RequestError as e:
            raise AnthropicError(f"Network error: {e}") from e

        if resp.status_code != 200:
            raise AnthropicError(
                f"Anthropic returned {resp.status_code}: {resp.text[:200]}",
                status_code=resp.status_code,
            )

        body = resp.json()
        content = body.get("content", [])
        text_block = next((b for b in content if b.get("type") == "text"), None)
        if text_block is None:
            raise AnthropicError("Anthropic did not return text content")

        enhanced = text_block["text"].strip()
        logger.info("anthropic_enhance_ok", chars=len(enhanced))
        return enhanced

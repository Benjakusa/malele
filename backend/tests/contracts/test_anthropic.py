import pytest
from pytest_httpx import HTTPXMock

from app.providers.lyrics.anthropic import AnthropicError, AnthropicLyricsProvider

pytestmark = pytest.mark.asyncio


async def test_generate_lyrics_success(httpx_mock: HTTPXMock) -> None:
    httpx_mock.add_response(
        method="POST",
        url="https://api.anthropic.com/v1/messages",
        status_code=200,
        json={
            "content": [
                {
                    "type": "tool_use",
                    "name": "emit_lyrics",
                    "input": {
                        "verse1": "First verse here",
                        "chorus": "Chorus here",
                        "verse2": "Second verse",
                        "bridge": "Bridge here",
                        "outro": "Outro here",
                    },
                }
            ],
            "stop_reason": "tool_use",
        },
    )
    provider = AnthropicLyricsProvider()
    lyrics = await provider.generate_lyrics(theme="a love song", language="en")
    assert lyrics["verse1"] == "First verse here"
    assert lyrics["chorus"] == "Chorus here"
    assert len(lyrics) == 5


async def test_generate_lyrics_missing_tool_use(httpx_mock: HTTPXMock) -> None:
    httpx_mock.add_response(
        method="POST",
        url="https://api.anthropic.com/v1/messages",
        status_code=200,
        json={"content": [{"type": "text", "text": "no tool here"}], "stop_reason": "end_turn"},
    )
    provider = AnthropicLyricsProvider()
    with pytest.raises(AnthropicError) as exc_info:
        await provider.generate_lyrics(theme="x")
    assert "tool_use" in str(exc_info.value)


async def test_generate_lyrics_401_raises(httpx_mock: HTTPXMock) -> None:
    httpx_mock.add_response(
        method="POST",
        url="https://api.anthropic.com/v1/messages",
        status_code=401,
        json={"error": {"type": "authentication_error"}},
    )
    provider = AnthropicLyricsProvider()
    with pytest.raises(AnthropicError) as exc_info:
        await provider.generate_lyrics(theme="x")
    assert exc_info.value.status_code == 401


async def test_enhance_prompt_success(httpx_mock: HTTPXMock) -> None:
    httpx_mock.add_response(
        method="POST",
        url="https://api.anthropic.com/v1/messages",
        status_code=200,
        json={
            "content": [
                {
                    "type": "text",
                    "text": "An energetic Benga track with interlocking guitars, driving bass, and warm male vocals.",
                }
            ],
            "stop_reason": "end_turn",
        },
    )
    provider = AnthropicLyricsProvider()
    enhanced = await provider.enhance_prompt(user_prompt="make a benga song", genre="benga")
    assert "Benga" in enhanced or "benga" in enhanced
    assert len(enhanced) > 20


async def test_enhance_prompt_401_raises(httpx_mock: HTTPXMock) -> None:
    httpx_mock.add_response(
        method="POST",
        url="https://api.anthropic.com/v1/messages",
        status_code=401,
        json={"error": {"type": "authentication_error"}},
    )
    provider = AnthropicLyricsProvider()
    with pytest.raises(AnthropicError) as exc_info:
        await provider.enhance_prompt(user_prompt="x")
    assert exc_info.value.status_code == 401

import pytest
from httpx import AsyncClient
from pytest_httpx import HTTPXMock
from sqlalchemy.ext.asyncio import AsyncSession

from tests.integration.test_generation import _register_and_get_token

pytestmark = pytest.mark.asyncio


async def test_enhance_prompt_via_http(
    client: AsyncClient, db: AsyncSession, httpx_mock: HTTPXMock
) -> None:
    token, _ = await _register_and_get_token(client, db)
    httpx_mock.add_response(
        method="POST",
        url="https://api.anthropic.com/v1/messages",
        status_code=200,
        json={
            "content": [{"type": "text", "text": "An energetic Benga track with interlocking guitars and warm male vocals."}],
            "stop_reason": "end_turn",
        },
    )
    r = await client.post(
        "/v1/prompt/enhance",
        json={"user_prompt": "make a benga song", "genre": "benga"},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["original"] == "make a benga song"
    assert "Benga" in body["enhanced"] or "benga" in body["enhanced"]


async def test_enhance_prompt_requires_auth(client: AsyncClient) -> None:
    r = await client.post("/v1/prompt/enhance", json={"user_prompt": "x"})
    assert r.status_code == 401


async def test_generate_lyrics_via_http(
    client: AsyncClient, db: AsyncSession, httpx_mock: HTTPXMock
) -> None:
    token, _ = await _register_and_get_token(client, db)
    httpx_mock.add_response(
        method="POST",
        url="https://api.anthropic.com/v1/messages",
        status_code=200,
        json={
            "content": [{
                "type": "tool_use",
                "name": "emit_lyrics",
                "input": {
                    "verse1": "First verse text",
                    "chorus": "Chorus text",
                    "verse2": "Second verse text",
                    "bridge": "Bridge text",
                    "outro": "Outro text",
                },
            }],
            "stop_reason": "tool_use",
        },
    )
    r = await client.post(
        "/v1/lyrics/generate",
        json={"theme": "a love song", "genre": "benga", "language": "sw"},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["sections"]["verse1"] == "First verse text"
    assert len(body["sections"]) == 5

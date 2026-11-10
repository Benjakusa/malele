import pytest
from httpx import Response
from pytest_httpx import HTTPXMock

from app.providers.base import GenerationRequest, ProviderJobState, ProviderKind
from app.providers.music.elevenlabs import ElevenLabsError, ElevenLabsMusicProvider

pytestmark = pytest.mark.asyncio


FAKE_MP3 = b"ID3" + b"\x00" * 5000 + b"\xff\xfb\x90" + b"\x00" * 3000


async def test_submit_success(httpx_mock: HTTPXMock) -> None:
    httpx_mock.add_response(
        method="POST",
        url="https://api.elevenlabs.io/v1/music",
        status_code=200,
        content=FAKE_MP3,
    )
    provider = ElevenLabsMusicProvider()
    req = GenerationRequest(
        prompt="short acoustic melody",
        duration_seconds=20,
        instrumental=True,
    )
    ref = await provider.submit(req)
    assert ref.provider == ProviderKind.ELEVENLABS
    assert ref.provider_job_id.startswith("el-")
    status = await provider.poll(ref)
    assert status.state == ProviderJobState.COMPLETED
    result = await provider.fetch(ref)
    assert len(result.audio_bytes) == len(FAKE_MP3)
    assert result.content_type == "audio/mpeg"
    assert result.metadata["provider"] == "elevenlabs"


async def test_submit_401_raises_elevenlabs_error(httpx_mock: HTTPXMock) -> None:
    httpx_mock.add_response(
        method="POST",
        url="https://api.elevenlabs.io/v1/music",
        status_code=401,
        json={"detail": {"code": "invalid_api_key"}},
    )
    provider = ElevenLabsMusicProvider()
    req = GenerationRequest(prompt="x", duration_seconds=20, instrumental=True)
    with pytest.raises(ElevenLabsError) as exc_info:
        await provider.submit(req)
    assert exc_info.value.status_code == 401


async def test_instrumental_flag_sent(httpx_mock: HTTPXMock) -> None:
    httpx_mock.add_response(
        method="POST",
        url="https://api.elevenlabs.io/v1/music",
        status_code=200,
        content=FAKE_MP3,
    )
    provider = ElevenLabsMusicProvider()
    req = GenerationRequest(prompt="x", duration_seconds=20, instrumental=True)
    await provider.submit(req)
    request = httpx_mock.get_request()
    import json
    body = json.loads(request.content)
    assert body["force_instrumental"] is True
    assert body["model_id"] == "music_v2"
    assert body["music_length_ms"] == 20_000
    assert request.headers["xi-api-key"]

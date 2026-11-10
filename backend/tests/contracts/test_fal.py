import pytest
from pytest_httpx import HTTPXMock

from app.providers.base import GenerationRequest, ProviderJobState
from app.providers.music.fal import FalError, FalMusicProvider

pytestmark = pytest.mark.asyncio


async def test_submit_success(httpx_mock: HTTPXMock) -> None:
    httpx_mock.add_response(
        method="POST",
        url="https://queue.fal.run/fal-ai/minimax-music/v2.6",
        status_code=200,
        json={"request_id": "req-abc-123", "status": "queued"},
    )
    provider = FalMusicProvider()
    req = GenerationRequest(prompt="test", duration_seconds=60, instrumental=True)
    ref = await provider.submit(req)
    assert ref.provider.value == "fal"
    assert ref.provider_job_id == "req-abc-123"


async def test_poll_processing_then_complete(httpx_mock: HTTPXMock) -> None:
    httpx_mock.add_response(
        method="GET",
        url="https://queue.fal.run/fal-ai/minimax-music/v2.6/requests/req-abc-123/status",
        status_code=200,
        json={"status": "IN_PROGRESS"},
    )
    httpx_mock.add_response(
        method="GET",
        url="https://queue.fal.run/fal-ai/minimax-music/v2.6/requests/req-abc-123/status",
        status_code=200,
        json={"status": "COMPLETED"},
    )
    provider = FalMusicProvider()
    from app.providers.base import ProviderJobRef, ProviderKind
    ref = ProviderJobRef(provider=ProviderKind.FAL, provider_job_id="req-abc-123")
    status1 = await provider.poll(ref)
    assert status1.state == ProviderJobState.PROCESSING
    status2 = await provider.poll(ref)
    assert status2.state == ProviderJobState.COMPLETED


async def test_submit_401_raises(httpx_mock: HTTPXMock) -> None:
    httpx_mock.add_response(
        method="POST",
        url="https://queue.fal.run/fal-ai/minimax-music/v2.6",
        status_code=401,
        json={"detail": "Unauthorized"},
    )
    provider = FalMusicProvider()
    req = GenerationRequest(prompt="test", duration_seconds=60, instrumental=True)
    with pytest.raises(FalError) as exc_info:
        await provider.submit(req)
    assert exc_info.value.status_code == 401


async def test_fetch_success(httpx_mock: HTTPXMock) -> None:
    fake_audio = b"ID3" + b"\x00" * 5000
    httpx_mock.add_response(
        method="GET",
        url="https://queue.fal.run/fal-ai/minimax-music/v2.6/requests/req-abc-123",
        status_code=200,
        json={"audio": {"url": "https://cdn.fal.run/abc.mp3"}},
    )
    httpx_mock.add_response(
        method="GET",
        url="https://cdn.fal.run/abc.mp3",
        status_code=200,
        content=fake_audio,
        headers={"content-type": "audio/mpeg"},
    )
    provider = FalMusicProvider()
    from app.providers.base import ProviderJobRef, ProviderKind
    ref = ProviderJobRef(provider=ProviderKind.FAL, provider_job_id="req-abc-123")
    result = await provider.fetch(ref)
    assert len(result.audio_bytes) == len(fake_audio)
    assert result.content_type == "audio/mpeg"
    assert result.metadata["provider"] == "fal"
    assert result.metadata["fal_request_id"] == "req-abc-123"

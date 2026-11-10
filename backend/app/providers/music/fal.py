import structlog
from uuid import uuid4

import httpx

from app.config.settings import get_settings
from app.providers.base import (
    GenerationRequest,
    GenerationResult,
    MusicProvider,
    ProviderCapabilities,
    ProviderJobRef,
    ProviderJobState,
    ProviderJobStatus,
    ProviderKind,
)


logger = structlog.get_logger()


class FalError(Exception):
    def __init__(self, message: str, status_code: int | None = None) -> None:
        super().__init__(message)
        self.status_code = status_code


class FalMusicProvider(MusicProvider):
    """fal.ai queue-based music adapter. Model configurable via FAL_MODEL env var."""

    kind = ProviderKind.FAL
    BASE_URL = "https://queue.fal.run"

    def __init__(self) -> None:
        self._settings = get_settings()
        self._model = getattr(self._settings, "FAL_MODEL", "") or "fal-ai/minimax-music/v2.6"

    async def capabilities(self) -> ProviderCapabilities:
        return ProviderCapabilities(
            full_song=True,
            vocals=True,
            custom_lyrics=True,
            instrumental=True,
            inpainting=False,
            streaming=False,
            stems=False,
            max_duration_seconds=180,
        )

    async def submit(self, request: GenerationRequest) -> ProviderJobRef:
        payload = {
            "prompt": request.prompt,
            "is_instrumental": request.instrumental,
        }
        if request.lyrics:
            payload["lyrics"] = request.lyrics
        headers = {"Authorization": f"Key {self._settings.FAL_KEY}"}
        url = f"{self.BASE_URL}/{self._model}"

        logger.info("fal_submit_start", model=self._model)

        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                resp = await client.post(url, json=payload, headers=headers)
        except httpx.RequestError as e:
            raise FalError(f"Network error: {e}") from e

        if resp.status_code not in (200, 202):
            raise FalError(
                f"fal returned {resp.status_code}: {resp.text[:200]}",
                status_code=resp.status_code,
            )

        body = resp.json()
        request_id = body.get("request_id")
        if not request_id:
            raise FalError("fal did not return request_id")

        logger.info("fal_submit_ok", request_id=request_id)
        return ProviderJobRef(provider=self.kind, provider_job_id=request_id)

    async def poll(self, job: ProviderJobRef) -> ProviderJobStatus:
        url = f"{self.BASE_URL}/{self._model}/requests/{job.provider_job_id}/status"
        headers = {"Authorization": f"Key {self._settings.FAL_KEY}"}

        try:
            async with httpx.AsyncClient(timeout=20.0) as client:
                resp = await client.get(url, headers=headers)
        except httpx.RequestError as e:
            raise FalError(f"Network error: {e}") from e

        if resp.status_code != 200:
            raise FalError(
                f"fal poll returned {resp.status_code}: {resp.text[:200]}",
                status_code=resp.status_code,
            )

        body = resp.json()
        status = body.get("status", "").lower()
        if status in ("completed", "success"):
            return ProviderJobStatus(state=ProviderJobState.COMPLETED, progress=100)
        if status in ("failed", "error"):
            return ProviderJobStatus(state=ProviderJobState.FAILED, error=body.get("error", "unknown"))
        return ProviderJobStatus(state=ProviderJobState.PROCESSING, progress=50)

    async def fetch(self, job: ProviderJobRef) -> GenerationResult:
        url = f"{self.BASE_URL}/{self._model}/requests/{job.provider_job_id}"
        headers = {"Authorization": f"Key {self._settings.FAL_KEY}"}

        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                resp = await client.get(url, headers=headers)
        except httpx.RequestError as e:
            raise FalError(f"Network error: {e}") from e

        if resp.status_code != 200:
            raise FalError(
                f"fal fetch returned {resp.status_code}: {resp.text[:200]}",
                status_code=resp.status_code,
            )

        body = resp.json()
        audio_url = body.get("audio", {}).get("url") or body.get("audio_url")
        if not audio_url:
            raise FalError("fal response missing audio URL")

        try:
            async with httpx.AsyncClient(timeout=120.0) as client:
                audio_resp = await client.get(audio_url)
        except httpx.RequestError as e:
            raise FalError(f"Audio download failed: {e}") from e

        if audio_resp.status_code != 200:
            raise FalError(f"Audio download returned {audio_resp.status_code}")

        return GenerationResult(
            audio_bytes=audio_resp.content,
            content_type=audio_resp.headers.get("content-type", "audio/mpeg"),
            duration_seconds=0.0,
            metadata={**_meta(self), "fal_request_id": job.provider_job_id},
        )

    async def cancel(self, job: ProviderJobRef) -> None:
        url = f"{self.BASE_URL}/{self._model}/requests/{job.provider_job_id}/cancel"
        headers = {"Authorization": f"Key {self._settings.FAL_KEY}"}
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                await client.put(url, headers=headers)
        except httpx.RequestError:
            pass  # cancel is best-effort


def _meta(provider: "FalMusicProvider") -> dict:
    return {"provider": "fal", "model": provider._model}

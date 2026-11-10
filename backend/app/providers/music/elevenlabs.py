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


class ElevenLabsError(Exception):
    def __init__(self, message: str, status_code: int | None = None) -> None:
        super().__init__(message)
        self.status_code = status_code


class ElevenLabsMusicProvider(MusicProvider):
    """ElevenLabs Music v2 adapter. Synchronous: audio returned on submit."""

    kind = ProviderKind.ELEVENLABS
    BASE_URL = "https://api.elevenlabs.io/v1/music"
    MODEL_ID = "music_v2"

    def __init__(self) -> None:
        self._settings = get_settings()
        self._audio_cache: dict[str, bytes] = {}
        self._meta_cache: dict[str, dict] = {}

    async def capabilities(self) -> ProviderCapabilities:
        return ProviderCapabilities(
            full_song=True,
            vocals=True,
            custom_lyrics=True,
            instrumental=True,
            inpainting=True,
            streaming=True,
            stems=False,
            max_duration_seconds=600,
        )

    async def submit(self, request: GenerationRequest) -> ProviderJobRef:
        payload = {
            "prompt": request.prompt,
            "music_length_ms": min(request.duration_seconds * 1000, 600_000),
            "model_id": self.MODEL_ID,
            "force_instrumental": request.instrumental,
            "output_format": "mp3_44100_128",
        }
        if request.lyrics:
            payload["composition_plan"] = {
                "sections": [{
                    "section_name": "main",
                    "duration_ms": payload["music_length_ms"],
                    "lines": request.lyrics.split(chr(10)),
                }]
            }
            payload.pop("prompt", None)  # composition_plan replaces prompt

        job_id = f"el-{uuid4()}"
        headers = {"xi-api-key": self._settings.ELEVENLABS_API_KEY}

        logger.info("elevenlabs_submit_start", job_id=job_id, duration_s=request.duration_seconds)

        try:
            async with httpx.AsyncClient(timeout=180.0) as client:
                resp = await client.post(self.BASE_URL, json=payload, headers=headers)
        except httpx.RequestError as e:
            raise ElevenLabsError(f"Network error: {e}") from e

        if resp.status_code != 200:
            raise ElevenLabsError(
                f"ElevenLabs returned {resp.status_code}: {resp.text[:200]}",
                status_code=resp.status_code,
            )

        audio = resp.content
        if not audio or len(audio) < 1000:
            raise ElevenLabsError(f"Received empty audio ({len(audio)} bytes)")

        self._audio_cache[job_id] = audio
        self._meta_cache[job_id] = {
            "provider": "elevenlabs",
            "model": self.MODEL_ID,
            "mime": "audio/mpeg",
        }
        logger.info("elevenlabs_submit_ok", job_id=job_id, bytes=len(audio))
        return ProviderJobRef(provider=self.kind, provider_job_id=job_id)

    async def poll(self, job: ProviderJobRef) -> ProviderJobStatus:
        if job.provider_job_id in self._audio_cache:
            return ProviderJobStatus(state=ProviderJobState.COMPLETED, progress=100)
        return ProviderJobStatus(state=ProviderJobState.FAILED, error="audio not in cache")

    async def fetch(self, job: ProviderJobRef) -> GenerationResult:
        audio = self._audio_cache.get(job.provider_job_id)
        if audio is None:
            raise ElevenLabsError(f"No cached audio for job {job.provider_job_id}")
        meta = self._meta_cache.get(job.provider_job_id, {})
        return GenerationResult(
            audio_bytes=audio,
            content_type="audio/mpeg",
            duration_seconds=0.0,  # caller reads from request; can add ID3 parse later
            metadata=meta,
        )

    async def cancel(self, job: ProviderJobRef) -> None:
        self._audio_cache.pop(job.provider_job_id, None)
        self._meta_cache.pop(job.provider_job_id, None)

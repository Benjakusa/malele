import asyncio
import time
from uuid import uuid4

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
from app.providers.mock_audio import generate_demo_wav


class MockMusicProvider(MusicProvider):
    """Deterministic mock provider for Phase 1. Returns synthesized WAV."""
    kind = ProviderKind.MOCK

    # Simulate processing time so polling state machine is exercised.
    PROCESSING_SECONDS = 2

    def __init__(self) -> None:
        self._jobs: dict[str, float] = {}

    async def capabilities(self) -> ProviderCapabilities:
        return ProviderCapabilities(
            full_song=True,
            vocals=True,
            custom_lyrics=True,
            instrumental=True,
            inpainting=False,
            streaming=False,
            stems=False,
            max_duration_seconds=600,
        )

    async def submit(self, request: GenerationRequest) -> ProviderJobRef:
        # Encode submit timestamp in job_id so poll() works after backend restart
        job_id = f"mock-{uuid4()}-{int(time.time())}"
        self._jobs[job_id] = time.monotonic()
        return ProviderJobRef(provider=self.kind, provider_job_id=job_id)

    async def poll(self, job: ProviderJobRef) -> ProviderJobStatus:
        # Prefer in-memory lookup (fast path, respects PROCESSING_SECONDS exactly)
        started = self._jobs.get(job.provider_job_id)
        if started is not None:
            elapsed = time.monotonic() - started
        else:
            # Fallback: parse timestamp from job_id (survives backend restart)
            try:
                ts_str = job.provider_job_id.rsplit("-", 1)[-1]
                started_at = int(ts_str)
                elapsed = time.time() - started_at
            except (ValueError, IndexError):
                return ProviderJobStatus(state=ProviderJobState.FAILED, error="unknown job")
        if elapsed < self.PROCESSING_SECONDS:
            progress = max(1, int((elapsed / self.PROCESSING_SECONDS) * 100))
            return ProviderJobStatus(state=ProviderJobState.PROCESSING, progress=progress)
        return ProviderJobStatus(state=ProviderJobState.COMPLETED, progress=100)

    async def fetch(self, job: ProviderJobRef) -> GenerationResult:
        # Always 5 seconds for determinism
        wav = generate_demo_wav(5)
        return GenerationResult(
            audio_bytes=wav,
            content_type="audio/wav",
            duration_seconds=5.0,
            metadata={
                "provider": self.kind.value,
                "mock": True,
                "note": "MOCK audio — synthesized sine chord, not musical",
            },
        )

    async def cancel(self, job: ProviderJobRef) -> None:
        self._jobs.pop(job.provider_job_id, None)

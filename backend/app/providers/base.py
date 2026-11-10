from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Protocol


class ProviderKind(str, Enum):
    MOCK = "mock"
    ELEVENLABS = "elevenlabs"
    FAL = "fal"


@dataclass
class ProviderCapabilities:
    full_song: bool = True
    vocals: bool = True
    custom_lyrics: bool = True
    instrumental: bool = True
    inpainting: bool = False
    streaming: bool = False
    stems: bool = False
    max_duration_seconds: int = 180


@dataclass
class GenerationRequest:
    prompt: str
    genre: str | None = None
    vocals: bool = True
    instrumental: bool = False
    lyrics: str | None = None
    duration_seconds: int = 120
    language: str | None = None
    preferred_provider: "ProviderKind | None" = None
    metadata: dict[str, Any] = field(default_factory=dict)


@dataclass
class ProviderCost:
    """Cost recorded when a provider call succeeds. Flows into CreditTransaction.metadata."""
    provider: ProviderKind
    amount_usd: float
    credits_charged: int
    detail: str = ""


@dataclass
class ProviderJobRef:
    provider: ProviderKind
    provider_job_id: str


class ProviderJobState(str, Enum):
    QUEUED = "queued"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"


@dataclass
class ProviderJobStatus:
    state: ProviderJobState
    progress: int = 0
    error: str | None = None


@dataclass
class GenerationResult:
    audio_bytes: bytes
    content_type: str
    duration_seconds: float
    metadata: dict[str, Any] = field(default_factory=dict)


class MusicProvider(Protocol):
    """Contract per ARCHITECTURE.md §2. Every provider adapter implements this."""
    kind: ProviderKind

    async def capabilities(self) -> ProviderCapabilities: ...
    async def submit(self, request: GenerationRequest) -> ProviderJobRef: ...
    async def poll(self, job: ProviderJobRef) -> ProviderJobStatus: ...
    async def fetch(self, job: ProviderJobRef) -> GenerationResult: ...
    async def cancel(self, job: ProviderJobRef) -> None: ...

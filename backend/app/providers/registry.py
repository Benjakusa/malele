import structlog

from app.config.settings import get_settings
from app.providers.base import (
    GenerationRequest,
    MusicProvider,
    ProviderCapabilities,
    ProviderKind,
)
from app.providers.music.elevenlabs import ElevenLabsMusicProvider
from app.providers.music.mock import MockMusicProvider


logger = structlog.get_logger()
_settings = get_settings()


class NoProviderAvailable(Exception):
    def __init__(self, reason: str) -> None:
        super().__init__(reason)
        self.reason = reason


class ProviderRegistry:
    """Holds provider instances and selects by capability."""

    def __init__(self) -> None:
        self._providers: dict[ProviderKind, MusicProvider] = {}

    def register(self, provider: MusicProvider) -> None:
        self._providers[provider.kind] = provider

    def get(self, kind: ProviderKind) -> MusicProvider | None:
        return self._providers.get(kind)

    def all(self) -> list[MusicProvider]:
        return list(self._providers.values())

    async def select(
        self, request: GenerationRequest, preferred_order: list[ProviderKind] | None = None
    ) -> MusicProvider:
        """Return the best provider that satisfies the request.

        Preferred order defaults to: ElevenLabs -> fal -> mock.
        Skips providers that are disabled by env flag or missing capability.
        """
        order = preferred_order or [
            ProviderKind.ELEVENLABS,
            ProviderKind.FAL,
            ProviderKind.MOCK,
        ]
        for kind in order:
            provider = self._providers.get(kind)
            if provider is None:
                continue
            if not self._enabled(kind):
                continue
            caps = await provider.capabilities()
            if not self._satisfies(caps, request):
                continue
            logger.info("provider_selected", kind=kind.value, prompt_len=len(request.prompt))
            return provider
        raise NoProviderAvailable(
            "No enabled provider satisfies the request capabilities"
        )

    def _enabled(self, kind: ProviderKind) -> bool:
        if kind == ProviderKind.MOCK:
            return _settings.PROVIDER_MOCK_ENABLED
        if kind == ProviderKind.ELEVENLABS:
            return _settings.ELEVENLABS_ENABLED and bool(_settings.ELEVENLABS_API_KEY)
        if kind == ProviderKind.FAL:
            return _settings.FAL_ENABLED and bool(_settings.FAL_KEY)
        return False

    @staticmethod
    def _satisfies(caps: ProviderCapabilities, request: GenerationRequest) -> bool:
        if request.instrumental and not caps.instrumental:
            return False
        if request.vocals and not request.instrumental and not caps.vocals:
            return False
        if request.lyrics and not caps.custom_lyrics:
            return False
        if request.duration_seconds > caps.max_duration_seconds:
            return False
        return True


_registry: ProviderRegistry | None = None


def get_registry() -> ProviderRegistry:
    """Lazily create and populate the registry (singleton)."""
    global _registry
    if _registry is None:
        _registry = ProviderRegistry()
        _registry.register(MockMusicProvider())
        # ElevenLabs adapter — used when ELEVENLABS_ENABLED=true
        if _settings.ELEVENLABS_API_KEY:
            _registry.register(ElevenLabsMusicProvider())
        # fal adapter — added in Step 4.
    return _registry

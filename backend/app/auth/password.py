from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError, VerificationError, InvalidHash

from app.config.settings import get_settings


_settings = get_settings()

# Argon2id params from ADR (SECURITY.md §2). Tuned for ~250ms on production hardware.
_hasher = PasswordHasher(
    time_cost=_settings.PASSWORD_HASH_TIME_COST,
    memory_cost=_settings.PASSWORD_HASH_MEMORY_COST,
    parallelism=_settings.PASSWORD_HASH_PARALLELISM,
)


def hash_password(password: str) -> str:
    """Return an Argon2id hash of the given password."""
    return _hasher.hash(password)


def verify_password(password: str, hashed: str) -> bool:
    """Verify a password against a hash. Returns False on any failure."""
    try:
        _hasher.verify(hashed, password)
        return True
    except (VerifyMismatchError, VerificationError, InvalidHash):
        return False


def needs_rehash(hashed: str) -> bool:
    """Return True if the hash uses outdated params and should be rehashed on next login."""
    return _hasher.check_needs_rehash(hashed)

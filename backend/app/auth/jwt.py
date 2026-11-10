import secrets
from datetime import UTC, datetime, timedelta
from typing import Any
from uuid import UUID, uuid4

from jose import JWTError, jwt

from app.config.settings import get_settings


_settings = get_settings()


class TokenError(Exception):
    """Raised when a JWT is invalid, expired, or malformed."""


def create_access_token(user_id: UUID, role: str) -> str:
    """Create a signed JWT access token."""
    now = datetime.now(UTC)
    payload: dict[str, Any] = {
        "sub": str(user_id),
        "role": role,
        "jti": str(uuid4()),
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(minutes=_settings.ACCESS_TOKEN_TTL_MINUTES)).timestamp()),
        "type": "access",
    }
    return jwt.encode(payload, _settings.JWT_SECRET, algorithm=_settings.JWT_ALGORITHM)


def decode_access_token(token: str) -> dict[str, Any]:
    """Decode and validate an access token. Raises TokenError on any failure."""
    try:
        payload = jwt.decode(
            token, _settings.JWT_SECRET, algorithms=[_settings.JWT_ALGORITHM]
        )
    except JWTError as e:
        raise TokenError(f"Invalid token: {e}") from e

    if payload.get("type") != "access":
        raise TokenError("Token is not an access token")
    if "sub" not in payload:
        raise TokenError("Token missing subject")
    return payload


def create_refresh_token() -> str:
    """Create an opaque refresh token (32 random bytes, hex-encoded)."""
    return secrets.token_hex(32)


def refresh_expires_at() -> datetime:
    """Return the expiry timestamp for a new refresh token."""
    return datetime.now(UTC) + timedelta(days=_settings.REFRESH_TOKEN_TTL_DAYS)

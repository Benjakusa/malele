import hashlib
from datetime import UTC, datetime

from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.jwt import (
    TokenError,
    create_access_token,
    create_refresh_token,
    refresh_expires_at,
)
from app.auth.password import hash_password, verify_password
from app.config.settings import get_settings
from app.models.auth import Session, User
from app.repositories import sessions as sessions_repo
from app.credits import ledger
from app.repositories import users as users_repo


_settings = get_settings()


class AuthError(Exception):
    """Base class for auth failures. Carries a stable error code."""
    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code
        self.message = message


def _hash_refresh_token(token: str) -> str:
    """Hash a refresh token for storage. Never store the raw token."""
    return hashlib.sha256(token.encode()).hexdigest()


async def register(
    db: AsyncSession,
    *,
    email: str,
    password: str,
    display_name: str | None,
) -> tuple[User, str, str]:
    """Create a user, issue tokens. Returns (user, access_token, refresh_token)."""
    existing = await users_repo.get_by_email(db, email)
    if existing is not None:
        raise AuthError("AUTH_EMAIL_TAKEN", "An account with this email already exists")
    user = await users_repo.create(
        db,
        email=email,
        password_hash=hash_password(password),
        display_name=display_name,
    )

    # Grant signup bonus (idempotent — safe even if a policy changes)
    await ledger.grant(
        db,
        user_id=user.id,
        amount=_settings.CREDITS_SIGNUP_BONUS,
        reason="signup_bonus",
    )

    return await _issue_tokens(db, user)


async def login(
    db: AsyncSession,
    *,
    email: str,
    password: str,
) -> tuple[User, str, str]:
    """Verify credentials, issue tokens. Returns (user, access_token, refresh_token)."""
    user = await users_repo.get_by_email(db, email)
    if user is None or not verify_password(password, user.password_hash):
        raise AuthError("AUTH_INVALID_CREDENTIALS", "Email or password is incorrect")
    if not user.is_active:
        raise AuthError("AUTH_ACCOUNT_DISABLED", "This account has been disabled")
    return await _issue_tokens(db, user)


async def refresh(
    db: AsyncSession,
    *,
    refresh_token: str,
) -> tuple[User, str, str]:
    """Exchange a refresh token for a new token pair. Old token is revoked."""
    token_hash = _hash_refresh_token(refresh_token)
    session = await sessions_repo.get_active_by_hash(db, token_hash)
    if session is None:
        raise AuthError("AUTH_INVALID_REFRESH", "Refresh token is invalid or expired")
    await sessions_repo.revoke(db, session.id)
    user = await users_repo.get_by_id(db, session.user_id)
    if user is None or not user.is_active:
        raise AuthError("AUTH_ACCOUNT_DISABLED", "This account has been disabled")
    return await _issue_tokens(db, user)


async def logout(
    db: AsyncSession,
    *,
    refresh_token: str,
) -> None:
    """Revoke a refresh token. Idempotent."""
    token_hash = _hash_refresh_token(refresh_token)
    session = await sessions_repo.get_active_by_hash(db, token_hash)
    if session is not None:
        await sessions_repo.revoke(db, session.id)


async def _issue_tokens(db: AsyncSession, user: User) -> tuple[User, str, str]:
    """Create access + refresh tokens, persist the refresh token hash."""
    access = create_access_token(user.id, user.role.value)
    refresh = create_refresh_token()
    await sessions_repo.create(
        db,
        user_id=user.id,
        refresh_token_hash=_hash_refresh_token(refresh),
        expires_at=refresh_expires_at(),
    )
    return user, access, refresh

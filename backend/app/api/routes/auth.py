from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.errors import ApiError
from app.auth.jwt import TokenError, decode_access_token
from app.database.session import get_db
from app.models.auth import User
from app.repositories import users as users_repo
from app.schemas.auth import (
    LoginRequest,
    LoginResponse,
    RefreshRequest,
    RegisterRequest,
    RegisterResponse,
    TokenPair,
    UserOut,
)
from app.services import auth_service


router = APIRouter(prefix="/v1/auth", tags=["auth"])
me_router = APIRouter(prefix="/v1", tags=["me"])

_bearer = HTTPBearer(auto_error=False)


async def get_current_user(
    creds: Annotated[HTTPAuthorizationCredentials | None, Depends(_bearer)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> User:
    if creds is None:
        raise ApiError("AUTH_MISSING_TOKEN", "Authentication required", 401)
    try:
        payload = decode_access_token(creds.credentials)
    except TokenError as e:
        raise ApiError("AUTH_INVALID_TOKEN", str(e), 401) from e
    from uuid import UUID
    user = await users_repo.get_by_id(db, UUID(payload["sub"]))
    if user is None or not user.is_active:
        raise ApiError("AUTH_INVALID_TOKEN", "User not found or inactive", 401)
    return user


def _token_pair(access: str, refresh: str, settings) -> TokenPair:
    return TokenPair(
        access_token=access,
        refresh_token=refresh,
        expires_in=settings.ACCESS_TOKEN_TTL_MINUTES * 60,
    )


@router.post("/register", response_model=RegisterResponse, status_code=201)
async def register(
    body: RegisterRequest,
    db: Annotated[AsyncSession, Depends(get_db)],
) -> RegisterResponse:
    from app.config.settings import get_settings
    settings = get_settings()
    try:
        user, access, refresh = await auth_service.register(
            db,
            email=body.email,
            password=body.password,
            display_name=body.display_name,
        )
    except auth_service.AuthError as e:
        code = e.code
        status_map = {"AUTH_EMAIL_TAKEN": 409}
        raise ApiError(code, e.message, status_map.get(code, 400)) from e
    return RegisterResponse(
        user=UserOut.model_validate(user),
        tokens=_token_pair(access, refresh, settings),
    )


@router.post("/login", response_model=LoginResponse)
async def login(
    body: LoginRequest,
    db: Annotated[AsyncSession, Depends(get_db)],
) -> LoginResponse:
    from app.config.settings import get_settings
    settings = get_settings()
    try:
        user, access, refresh = await auth_service.login(db, email=body.email, password=body.password)
    except auth_service.AuthError as e:
        code = e.code
        status_map = {"AUTH_INVALID_CREDENTIALS": 401, "AUTH_ACCOUNT_DISABLED": 403}
        raise ApiError(code, e.message, status_map.get(code, 401)) from e
    return LoginResponse(
        user=UserOut.model_validate(user),
        tokens=_token_pair(access, refresh, settings),
    )


@router.post("/refresh", response_model=TokenPair)
async def refresh(
    body: RefreshRequest,
    db: Annotated[AsyncSession, Depends(get_db)],
) -> TokenPair:
    from app.config.settings import get_settings
    settings = get_settings()
    try:
        _, access, new_refresh = await auth_service.refresh(db, refresh_token=body.refresh_token)
    except auth_service.AuthError as e:
        raise ApiError(e.code, e.message, 401) from e
    return _token_pair(access, new_refresh, settings)


@router.post("/logout", status_code=204)
async def logout(
    body: RefreshRequest,
    db: Annotated[AsyncSession, Depends(get_db)],
) -> None:
    await auth_service.logout(db, refresh_token=body.refresh_token)


@me_router.get("/me", response_model=UserOut)
async def me(user: Annotated[User, Depends(get_current_user)]) -> UserOut:
    return UserOut.model_validate(user)

from app.schemas.auth import UpdateMeRequest


@me_router.patch("/me", response_model=UserOut)
async def update_me(
    body: UpdateMeRequest,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
) -> UserOut:
    """Update current user's profile (display name)."""
    if body.display_name is not None:
        user.display_name = body.display_name
        await db.flush()
    return UserOut.model_validate(user)

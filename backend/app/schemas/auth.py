from uuid import UUID

from pydantic import BaseModel, EmailStr, Field


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    display_name: str | None = Field(default=None, min_length=1, max_length=100)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class RefreshRequest(BaseModel):
    refresh_token: str


class TokenPair(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int  # seconds until access token expires


class UserOut(BaseModel):
    id: UUID
    email: EmailStr
    role: str
    display_name: str | None
    email_verified: bool
    is_active: bool

    model_config = {"from_attributes": True}


class RegisterResponse(BaseModel):
    user: UserOut
    tokens: TokenPair


class LoginResponse(BaseModel):
    user: UserOut
    tokens: TokenPair

class UpdateMeRequest(BaseModel):
    display_name: str | None = Field(default=None, min_length=1, max_length=100)

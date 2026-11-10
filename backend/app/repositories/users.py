from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.auth import User, UserRole


async def get_by_id(db: AsyncSession, user_id: UUID) -> User | None:
    return await db.get(User, user_id)


async def get_by_email(db: AsyncSession, email: str) -> User | None:
    result = await db.execute(select(User).where(User.email == email.lower()))
    return result.scalar_one_or_none()


async def create(
    db: AsyncSession,
    *,
    email: str,
    password_hash: str,
    display_name: str | None = None,
    role: UserRole = UserRole.USER,
) -> User:
    user = User(
        email=email.lower(),
        password_hash=password_hash,
        display_name=display_name,
        role=role,
        email_verified=False,
        is_active=True,
    )
    db.add(user)
    await db.flush()
    return user

from collections.abc import AsyncGenerator

from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

from app.config.settings import get_settings


settings = get_settings()

# Convert psycopg URL to asyncpg URL if needed. settings.DATABASE_URL uses psycopg by default
# for Alembic, but the runtime uses asyncpg for async queries.
_db_url = str(settings.DATABASE_URL).replace("postgresql+psycopg://", "postgresql+asyncpg://")

# asyncpg doesn't accept the libpq-style 'sslmode' query param.
# Pass ssl explicitly via connect_args so both URL forms work.
_connect_args = {}
if "localhost" not in _db_url and "127.0.0.1" not in _db_url:
    _connect_args = {"ssl": "require"}
elif "sslmode" in _db_url:
    # Strip sslmode from local URLs so asyncpg doesn't choke on it
    _db_url = _db_url.replace("?sslmode=require", "").replace("&sslmode=require", "")

engine = create_async_engine(
    _db_url,
    echo=settings.APP_DEBUG,
    pool_size=10,
    max_overflow=20,
    pool_pre_ping=True,
    connect_args=_connect_args,
)

AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autoflush=False,
)


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """FastAPI dependency yielding a session per request."""
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise

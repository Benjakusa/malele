from collections.abc import AsyncGenerator

from sqlalchemy.engine import make_url
from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

from app.config.settings import get_settings


settings = get_settings()

# Force psycopg driver for consistency across environments.
# Render may auto-select asyncpg if both are installed; psycopg uses sslmode=require.
_raw_url = str(settings.DATABASE_URL)
_raw_url = _raw_url.replace("postgresql+asyncpg://", "postgresql+psycopg://")
if _raw_url.startswith("postgresql://"):
    _raw_url = _raw_url.replace("postgresql://", "postgresql+psycopg://", 1)

# Strip all libpq-style SSL query params; connect_args is the single source of truth.
_parsed = make_url(_raw_url)
_bad_keys = {"ssl", "sslmode", "channel_binding"}
_clean_query = {k: v for k, v in _parsed.query.items() if k not in _bad_keys}
_db_url = _parsed.set(query=_clean_query).render_as_string(hide_password=False)

_connect_args = {}
if "localhost" not in _db_url and "127.0.0.1" not in _db_url:
    _connect_args = {"sslmode": "require"}

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

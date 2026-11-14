from collections.abc import AsyncGenerator

from sqlalchemy.engine import make_url
from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

from app.config.settings import get_settings


settings = get_settings()

_db_url = str(settings.DATABASE_URL)

# psycopg (the default driver for postgresql://) rejects the "ssl" parameter.
# It expects "sslmode" instead. Strip invalid params from the URL and pass sslmode via connect_args.
_parsed = make_url(_db_url)
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

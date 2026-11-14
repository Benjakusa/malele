import asyncio
import structlog
from contextlib import asynccontextmanager
from typing import AsyncIterator

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.errors import install_error_handlers
from app.api.routes.auth import me_router, router as auth_router
from app.api.routes.credits import router as credits_router
from app.api.routes.generations import router as generations_router
from app.api.routes.prompts import router as prompts_router
from app.config.settings import get_settings


logger = structlog.get_logger()


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    """Startup and shutdown hooks."""
    settings = get_settings()
    if settings.APP_ENV == "production" and settings.JWT_SECRET.startswith("CHANGE_ME"):
        raise RuntimeError("Refusing to start in production with placeholder JWT_SECRET")
    logger.info(
        "malele_backend_startup",
        env=settings.APP_ENV,
        debug=settings.APP_DEBUG,
        mock_provider_enabled=settings.PROVIDER_MOCK_ENABLED,
        generation_enabled=settings.GENERATION_ENABLED,
    )

    # Kick off schema creation in the background so a cold Neon database
    # cannot block port binding (Render's port scanner has a short timeout).
    schema_task = asyncio.create_task(_ensure_schema_ready())

    yield

    logger.info("malele_backend_shutdown")
    if not schema_task.done():
        schema_task.cancel()
        try:
            await schema_task
        except (asyncio.CancelledError, Exception):
            pass


async def _ensure_schema_ready() -> None:
    """Create DB schema if missing, retrying once on a cold-DB timeout.

    Runs in the background so port binding is never blocked. A failure
    here is logged but does NOT crash the app — Render's /health check
    still responds, and the DB will be retried on the next deploy.
    """
    from app.database.base import Base
    from app.database.session import engine
    import app.models.auth, app.models.jobs, app.models.songs, app.models.credits  # noqa: F401

    for attempt in range(1, 3):
        try:
            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all)
            logger.info("malele_database_schema_ready", attempt=attempt)
            return
        except Exception as exc:
            logger.warning(
                "malele_database_schema_failed",
                attempt=attempt,
                error=str(exc),
            )
            if attempt < 2:
                await asyncio.sleep(3)
    logger.error("malele_database_schema_gave_up")


def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(
        title="Malele API",
        version="0.1.0",
        description="Malele backend — AI music creation platform",
        docs_url="/docs" if settings.APP_DEBUG else None,
        redoc_url="/redoc" if settings.APP_DEBUG else None,
        openapi_url="/openapi.json" if settings.APP_DEBUG else None,
        lifespan=lifespan,
    )
    app.add_middleware(
        CORSMiddleware,
        allow_origins=[settings.APP_FRONTEND_URL],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    install_error_handlers(app)
    app.include_router(auth_router)
    app.include_router(me_router)
    app.include_router(generations_router)
    app.include_router(prompts_router)
    app.include_router(credits_router)

    @app.get("/health", tags=["meta"])
    async def health() -> dict[str, str]:
        return {"status": "ok", "service": "malele-backend"}

    return app


app = create_app()

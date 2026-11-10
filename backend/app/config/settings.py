from functools import lru_cache
from typing import Literal

from pydantic import Field, PostgresDsn, RedisDsn
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    APP_ENV: Literal["development", "staging", "production", "test"] = "development"
    APP_DEBUG: bool = True
    APP_NAME: str = "Malele"
    APP_BASE_URL: str = "http://localhost:8000"
    APP_FRONTEND_URL: str = "http://localhost:3000"
    LOG_LEVEL: str = "INFO"

    JWT_SECRET: str = Field(min_length=32)
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_TTL_MINUTES: int = 15
    REFRESH_TOKEN_TTL_DAYS: int = 30
    PASSWORD_HASH_TIME_COST: int = 3
    PASSWORD_HASH_MEMORY_COST: int = 65536
    PASSWORD_HASH_PARALLELISM: int = 4

    DATABASE_URL: PostgresDsn
    REDIS_URL: RedisDsn
    CELERY_BROKER_URL: RedisDsn
    CELERY_RESULT_BACKEND: RedisDsn

    STORAGE_BACKEND: Literal["local", "s3"] = "local"
    STORAGE_LOCAL_PATH: str = "./storage"
    S3_ENDPOINT: str = ""
    S3_BUCKET: str = "malele-dev"
    S3_ACCESS_KEY: str = ""
    S3_SECRET_KEY: str = ""
    S3_REGION: str = "us-east-1"
    SIGNED_URL_TTL_SECONDS: int = 300

    PROVIDER_MOCK_ENABLED: bool = True
    ELEVENLABS_API_KEY: str = ""
    FAL_KEY: str = ""
    ANTHROPIC_API_KEY: str = ""
    ANTHROPIC_MODEL: str = ""
    OPENAI_API_KEY: str = ""
    OPENAI_IMAGE_MODEL: str = ""

    CREDITS_SIGNUP_BONUS: int = 50
    CREDITS_COST_PER_SONG: int = 10
    CREDITS_DAILY_MAX_PER_USER: int = 100
    CREDITS_DAILY_MAX_GLOBAL: int = 10000

    GENERATION_ENABLED: bool = True
    ELEVENLABS_ENABLED: bool = False
    FAL_ENABLED: bool = False


@lru_cache
def get_settings() -> Settings:
    return Settings()

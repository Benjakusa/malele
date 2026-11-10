# Malele Backend — production and dev image
FROM python:3.13-slim AS base

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# System dependencies:
# - libpq5: Postgres client library for psycopg
# - curl: healthcheck and debugging
# - ca-certificates: HTTPS trust store
# ffmpeg is NOT installed here. It comes from the static-ffmpeg pip package
# which ships a prebuilt static binary. This avoids Debian trixie's
# 182-package ffmpeg metapackage (see KNOWN_GAPS.md).
RUN apt-get update && apt-get install -y --no-install-recommends \
        libpq5 \
        curl \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY pyproject.toml ./
RUN pip install --upgrade pip && pip install -e ".[dev]"

COPY . .

RUN useradd --create-home --shell /bin/bash malele \
    && chown -R malele:malele /app
USER malele

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -fsS http://localhost:8000/health || exit 1

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

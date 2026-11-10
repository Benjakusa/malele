# Architecture

**Status:** Phase 0.
**Last updated:** Phase 0.

How Malele is structured. Enforced as a contract — if code violates this document, the code is wrong.

---

## 1. System overview

```
┌──────────────────────────────────────────────────────────────┐
│  Android app (Kotlin, Compose)                                │
│  ─────────────────────────────                                │
│  UI (Compose)  ──►  ViewModels (StateFlow)  ──►  Repositories │
│                                                       │       │
│                                                       ▼       │
│                                       Retrofit + OkHttp        │
│                                       Room (local cache)       │
│                                       Media3 ExoPlayer         │
└───────────────────────────────────┬──────────────────────────┘
                                    │  HTTPS + JWT
                                    ▼
┌──────────────────────────────────────────────────────────────┐
│  Backend (FastAPI, Python 3.13)                               │
│  ─────────────────────────────                                │
│  API routes ──► Services ──► Repositories ──► SQLAlchemy      │
│       │             │                                         │
│       │             ├──► ProviderRouter                       │
│       │             │       ├──► ElevenLabsMusicProvider       │
│       │             │       └──► FalMusicProvider              │
│       │             ├──► AnthropicLyricsProvider               │
│       │             └──► OpenAIImageProvider                   │
│       │                                                       │
│       └──► Celery task queue ──► Workers                      │
│                                                               │
│  Postgres (source of truth)  +  Redis (queue + cache)          │
│  S3-compatible object storage (audio, artwork)                 │
└──────────────────────────────────────────────────────────────┘
```

**Rules:**
1. Android never talks to a provider. All provider calls go through backend.
2. Provider keys live only in backend environment variables.
3. Every provider call is behind a `Provider` interface. No provider name outside its adapter.
4. Long-running work (generation, mixdown) is a job, never a synchronous request.
5. Every state transition is persisted in Postgres before the next action fires.
6. Postgres is the source of truth; Redis is a queue + cache.

---

## 2. Android architecture

**Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Room, Retrofit, OkHttp, Kotlinx Serialization, Media3 ExoPlayer, Navigation Compose, WorkManager.

**Module structure (per master prompt §5):**

```
android/
  app/                       application module (MainActivity, nav host, DI graph)
  core/
    common/                  utilities, dispatchers, Result types
    model/                   domain models (Song, Job, User, LyricsSection, ...)
    network/                 Retrofit, OkHttp, interceptors, DTOs, mappers
    database/                Room entities, DAOs, migrations
    audio/                   ExoPlayer wrapper, Media3 utilities
    storage/                 file I/O, cache, download manager
    design/                  theme (Color, Type), reusable Compose components
  feature/
    auth/                    login, register, password reset
    create/                  Simple + Custom creation modes
    lyrics/                  lyrics editor + AI generation
    generation/              job submission, progress, notifications
    player/                  song player, waveform, background audio
    library/                 personal library, search, sort, filter
    editor/                  edit operations (extend, remix, etc.) + versions
    studio/                  Studio Lite timeline, mixer, recording
    explore/                 discovery, search, trending
    creator/                 creator profile, follow, likes
    profile/                 user settings, account
    credits/                 credit balance, history
    settings/                app settings
```

**Layering rule (enforced by code review):**
- `feature/*` depends on `core/*` — **not** on other `feature/*`.
- `core/*` does not depend on `feature/*`.
- `feature/*` exposes one public entry point (`XxxFeatureNavGraph` or `XxxEntry`) to `app/`.
- Business logic lives in repositories + use cases, never in Compose functions.

**State management:**
- Every screen: one `ViewModel` with `StateFlow<UiState>`.
- `UiState` is a sealed class: `Loading`, `Success(data)`, `Error(code, message)`, `Empty`, `Offline`.
- No Compose `remember` for business state — only for local UI (scroll position, expanded flags).
- ViewModel survives config change and process death via `SavedStateHandle`.

**Error strategy:**
- Network layer returns `Result<T, ApiError>`; `ApiError` carries a stable code (e.g., `AUTH_INVALID_CREDENTIALS`, `GENERATION_QUOTA_EXHAUSTED`).
- ViewModel maps `ApiError` to user-facing strings in the UI layer.
- Retry is manual by the user, except for background job polling (WorkManager with backoff).

**Testing rules:**
- Every ViewModel: unit test with fake repository.
- Every repository: contract test with mock API server (MockWebServer).
- Critical UI flows: one Espresso test each.

---

## 3. Backend architecture

**Stack:** Python 3.13, FastAPI, Pydantic v2, SQLAlchemy 2.x, Alembic, PostgreSQL, Redis, Celery, boto3 (S3), pytest.

**Module structure (per master prompt §5):**

```
backend/app/
  api/             HTTP route handlers only — no business logic
  auth/            JWT, password hashing, session, roles
  config/          Pydantic Settings, env loading
  database/        SQLAlchemy engine, session, base
  models/          SQLAlchemy ORM models
  schemas/         Pydantic request/response schemas
  repositories/    DB access, queries, transactions
  services/        business logic — the only layer that calls providers
  providers/       MusicProvider, LyricsProvider, ImageProvider + adapters
  prompts/         versioned prompt templates (Jinja2 or .txt)
  lyrics/          lyric domain logic + copyright validator
  moderation/      content filters for prompts, lyrics, artwork
  generation/      job state machine, orchestration
  audio/           FFmpeg wrappers, waveform, loudness
  artwork/         image processing
  storage/         S3 abstraction, signed URLs
  notifications/   push + email stubs
  credits/         ledger, reservations, refunds, policy
  publishing/      visibility rules, public URL generation
  discovery/       trending, search, recommendations (SQL first)
  workers/         Celery tasks
  middleware/      auth, rate limit, request ID, error handler
```

**Layering rule:**
- Routes call services. Services call repositories + providers. Repositories call DB only.
- No provider call in a route handler. **Never**.
- No DB query in a provider adapter. **Never**.
- No FastAPI types (`Request`, `Depends`, `HTTPException`) in services or providers.

**Dependency injection:** Pydantic Settings for config + module-level factory functions. Not a DI framework — Python services are wired explicitly in `app/services/__init__.py`.

**Background jobs:**
- Celery task per job state transition that involves external I/O.
- Task name: `backend.app.workers.{domain}.{action}` — stable, versioned, never renamed.
- Task args: only IDs. Task fetches state from DB.
- Idempotency: every Celery task checks a `JobEvent` row before mutating; re-delivery is safe.
- Retries: exponential backoff, max 5 attempts, then `FAILED` + refund.

**Testing rules:**
- Every service: unit test with fake repositories and fake providers.
- Every repository: integration test against a real Postgres in Docker (`pytest-postgresql`).
- Every provider adapter: contract test against a recorded fixture (VCR-style) plus live test gated behind env flag.
- Every API endpoint: one integration test covering happy path + one covering an error path.

---

## 4. Data flow — generation job (end-to-end)

1. **Android** `POST /v1/generations` with `{ prompt, genre, vocals, lyrics, ... }` + JWT.
2. **API** validates request, checks user quota, calls `generation_service.submit()`.
3. **Service** creates `GenerationJob` row (state=`QUEUED`), reserves credits in a DB transaction, writes `JobEvent(submitted)`.
4. **Service** calls `provider_router.select(request)` → returns `ElevenLabsMusicProvider`.
5. **Provider adapter** calls ElevenLabs `POST /v1/music`. Stores `ProviderJobRef` on the job.
6. **Service** enqueues Celery task `workers.generation.poll` with job ID.
7. **Worker** polls provider. On `completed`: downloads audio, uploads to S3, extracts metadata (duration, waveform peaks, loudness), writes `JobEvent(downloaded)`.
8. **Service** updates job to `COMPLETED`, credits are debited (reservation released as charge).
9. **Worker** enqueues `workers.generation.notify` — push notification + optional email.
10. **Android** polls `GET /v1/generations/{id}` or receives push. On `COMPLETED`, calls `GET /v1/generations/{id}/results` and downloads via signed URL.

**Failure paths:**
- Provider 4xx (bad request) → job `FAILED`, refund, `JobEvent(failed_reason=...)`.
- Provider 5xx → retry up to 3× with backoff, then try fallback provider if capability-satisfying; else `FAILED` + refund.
- Download timeout → retry 3×; then `FAILED` + refund.
- Worker crash → Celery re-delivers; idempotency check prevents double download/charge.

**Visibility of intermediate state:** every step writes a `JobEvent`. `GET /v1/generations/{id}` returns the job state plus event history; UI can show progress honestly.

---

## 5. Database architecture

**Engine:** PostgreSQL 16+ (production), SQLite (dev-only fallback, documented in `.env.example`).

**Migrations:** Alembic. Every schema change is a migration file under `backend/migrations/versions/`. Never edit a migration that has already run in production.

**Core tables** (from master prompt §5):
- `users`, `user_profiles`, `user_roles`, `sessions`, `user_settings`
- `songs`, `song_versions`, `song_lyrics`, `song_sections`
- `generation_jobs`, `job_events`
- `providers`, `provider_jobs`, `provider_capabilities`
- `generation_prompts` (original + enhanced)
- `audio_assets`, `artwork_assets`, `audio_tracks`, `studio_projects`
- `playlists`, `playlist_songs`, `follows`, `likes`, `reports`, `notifications`
- `credit_accounts`, `credit_transactions`, `credit_reservations`
- `genres`, `genre_configurations`, `languages`, `publications`

**Rules:**
- All IDs are UUIDs (except seeded lookup tables like `genres` which may use human-readable IDs).
- Every user-scoped table has `user_id UUID NOT NULL` with an index.
- Every foreign key has an index (Postgres does not auto-index FK columns).
- Time columns: `TIMESTAMPTZ NOT NULL DEFAULT now()`.
- Soft delete only where required by law (e.g., `credit_transactions` — immutable ledger).
- Everything else: hard delete with cascade. Account deletion purges audio + artwork from S3 immediately; DB rows purged after audit log written.
- No `TEXT` columns that should be `JSONB` — prompt metadata, provider responses, job payloads go in `JSONB`.
- Every credit mutation is inside a `SERIALIZABLE` transaction or uses `SELECT ... FOR UPDATE` — no double-charge.

**Isolation:** every query that touches user data filters by `user_id`. Enforced by repository-layer helper `scoped_to(user_id)`. A test in Phase 1 asserts 2 users cannot read each other’s songs.

---

## 6. Storage architecture

**Interface:** `backend/app/storage/base.py` defines `Storage` protocol.
**Dev:** local disk under `backend/storage/` (gitignored).
**Prod:** S3-compatible (AWS S3, Cloudflare R2, Backblaze B2 — configuration only, no code change).

**Bucket layout:**
```
malele-prod/
  songs/{song_id}/{version_id}/audio.mp3
  songs/{song_id}/{version_id}/audio.wav
  songs/{song_id}/artwork/{hash}.png
  waveforms/{audio_asset_id}.json
  uploads/{user_id}/{upload_id}/{filename}
  exports/{user_id}/{export_id}/mix.wav
```

**Rules:**
- All objects **private** by default. Access only via short-lived signed URLs (5 min TTL, generated on `GET /v1/songs/{id}`).
- Public songs use a controlled public URL that goes through a backend endpoint — never a raw S3 public bucket.
- No user-uploaded file ever served directly from storage — always through the backend, which validates ownership.
- Deletion is a two-step process: mark in DB, then a Celery task deletes from S3 within 1 hour.

---

## 7. Cross-cutting technical rules

| Concern | Rule |
| --- | --- |
| Secrets | Only in backend env vars. `.env` in `.gitignore`. `.env.example` lists names only. |
| Logging | Structured JSON. Include `request_id`, `user_id` (if authed), `job_id` (if in a job). Never log tokens or provider keys. |
| Timeouts | Every external HTTP call has a timeout. No unbounded waits. |
| Retries | Only idempotent operations. Exponential backoff. Never retry non-idempotent operations without idempotency key. |
| Idempotency | Generation submit uses `Idempotency-Key` header. Same key = same job, no double charge. |
| Pagination | Cursor-based, default 20, max 100. Never offset-based beyond 1000. |
| Errors | Stable error codes (`AUTH_INVALID_CREDENTIALS`, `GENERATION_QUOTA_EXHAUSTED`, ...) in a central registry. User-facing messages stored alongside codes. |
| Rate limits | Per-user and per-IP. Enforced in middleware. 429 with `Retry-After`. |
| Tracing | Every request has a UUID `request_id`; passed through to Celery tasks and provider calls. |
| i18n | No hard-coded strings in backend responses that appear in UI. Codes only. UI translates. |

---

## 8. Deployment topologies (informal, formalized in Phase 9)

**Local dev:**
- `docker compose up` runs postgres + redis + api + worker.
- Android connects to `http://10.0.2.2:8000` (emulator) or LAN IP (device).

**Staging:**
- Single VPS, docker compose, managed Postgres, self-hosted Redis.
- HTTPS via Caddy or nginx + Let’s Encrypt.
- Real providers enabled with low daily caps.

**Production:**
- Scale backend horizontally; workers separate auto-scaling group.
- Managed Postgres (RDS/Cloud SQL/Supabase).
- Managed Redis (ElastiCache/Upstash).
- CDN in front of audio (Cloudflare/Fastly).
- Kill switches via env vars, hot-reloadable.

---

**End of ARCHITECTURE.md (Phase 0).**

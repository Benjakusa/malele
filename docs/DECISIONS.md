# Decisions

**Status:** Phase 0 — living document, appended as new decisions are made.
**Last updated:** Phase 0.

Architecture Decision Records (ADRs). Each decision: context, choice, alternatives, rationale, consequences. **Never delete an entry** — supersede with a new ADR that references the old one.

---

## ADR-001 — Provider strategy: ElevenLabs primary, fal.ai fallback

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Master prompt Module F requires provider-independent architecture with a fallback path. Search research (Phase 0.2) verified both ElevenLabs (music_v2, inpainting, C2PA) and fal.ai (minimax-music/v2.6, diffrhythm, lyria).
**Decision:** ElevenLabs is primary for full songs, vocals, inpainting. fal.ai is fallback and also the cheap-test provider (diffrhythm $0.001/s).
**Alternatives considered:** (a) ElevenLabs only — no fallback if outage or price spike. (b) fal.ai only — no inpainting. (c) Lyria as primary — rejected because lyria-002 max 32.8s, English-only, no vocals (see ADR-002).
**Consequences:** Two adapters to maintain. `ProviderRouter` must implement capability-based selection. Cost model must show both paths (see COST_MODEL.md).

---

## ADR-002 — Reject Google Lyria-002 as primary music provider

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Master prompt §6 asks to evaluate Google Lyria.
**Decision:** Lyria-002 is **rejected as primary**. Max 32.8 s clip, English-only prompts, no vocals, no custom lyrics, no structure control. It cannot satisfy Malele core requirement (2–4 min African-language songs with vocals and custom lyrics).
**Alternatives:** Use Lyria for short instrumental loops only (Phase 7+). Not in MVP.
**Consequences:** Do not wire Lyria in Phase 2. Add to KNOWN_GAPS as deferred.

---

## ADR-003 — Python 3.13 (not 3.12)

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Developer environment has Python 3.13.7 via pyenv. Master prompt suggests 3.12.
**Decision:** Use Python 3.13.7.
**Rationale:** FastAPI, Pydantic v2, SQLAlchemy 2.x, Alembic, Celery, pytest all support 3.13 (verified Phase 0.2 research). pyenv pin ensures reproducibility across machines.
**Consequences:** Pin Python 3.13 in `backend/.python-version`. Any new dependency must be checked for 3.13 support at Phase 1 kickoff.

---

## ADR-004 — Package ID `com.malele.app` (placeholder)

**Date:** Phase 0.
**Status:** Accepted, pending publish.
**Context:** Application ID cannot be changed after first Play Store publish.
**Decision:** Use `com.malele.app` for development. **Before first Play Store upload**, replace with a domain the project controls (e.g., `com.<owner>.malele`).
**Consequences:** Tracked as pre-publish TODO in KNOWN_GAPS. Kotlin root package `com.malele` can stay — only `applicationId` in `app/build.gradle.kts` needs the change.

---

## ADR-005 — Audio backend: AudioTrack first, Oboe later (if at all)

**Date:** Phase 0.
**Status:** Accepted (supersedes earlier draft decision on Oboe-first).
**Context:** Master prompt requires low-latency audio. Oboe (C++/NDK) is best-in-class but adds NDK build complexity and risks delaying delivery.
**Decision:** Start with `AudioTrack` in `USAGE_MEDIA`/`LOW_LATENCY` mode behind an `AudioEngine` interface. Only add Oboe if real-device round-trip exceeds 40 ms.
**Rationale:** AudioTrack low-latency is sufficient for playback and mixing. Studio Lite is not a live-instrument DAW. Ship first, optimize if measured.
**Consequences:** No NDK in MVP. `AudioEngine` interface must remain backend-neutral (already designed this way).

---

## ADR-006 — Media3 ExoPlayer for playback, Media3 Transformer for mixdown

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Need background playback, notifications, lockscreen, and mixdown export.
**Decision:** Use Media3 ExoPlayer for playback; Media3 Transformer for mixdown to WAV/M4A. Do not build a custom audio engine from scratch.
**Alternatives:** Legacy ExoPlayer 2 (EOL, avoid); custom PCM pipeline (over-engineering).
**Consequences:** All audio cache + queue handled by Media3. Follow Media3 upgrade notes each Android dependency bump.

---

## ADR-007 — Compose-only UI, no XML layouts

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Master prompt §5 mandates Jetpack Compose + Material 3.
**Decision:** All screens in Compose. No new XML layouts. Only XML: `values/` strings/colors/themes, manifest, launcher icons.
**Alternatives:** Hybrid Compose + View system (rejected — doubles complexity).
**Consequences:** Legacy XML views from any pre-existing code (none in this repo) would be migrated; we start clean.

---

## ADR-008 — Lyrics validated for copyright before submission

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Legal risk from copyrighted lyrics in user prompts and AI-generated lyrics. Master prompt §7 prohibits reproducing copyrighted songs.
**Decision:** Every lyric string (user-typed or AI-generated) passes through a copyright validator service before being stored or sent to a provider. Validator = substring match against a curated fingerprint DB + LLM-based paraphrase detector flag.
**Alternatives:** Trust-the-user (rejected — legal exposure). Strip all lyrics (rejects the product).
**Consequences:** Extra service in backend (`lyrics/copyright.py`). False positives must give a clear, appealable error message. Fingerprint DB starts small (top 100 published songs) and grows.

---

## ADR-009 — Credits ledger is append-only

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Financial correctness requires an auditable trail. Silent edits to balances are a security and legal risk.
**Decision:** `credit_transactions` is append-only. Corrections are new rows (refund, adjustment) referencing the originals. Balances are computed, not stored (or stored as materialized aggregates refreshed on every transaction, with a periodic reconciliation job).
**Alternatives:** Mutable balance column (rejected — no audit trail). Event sourcing from day 1 (over-engineered for MVP; can migrate later).
**Consequences:** Every credit-consuming operation must produce a transaction row. Reconciliation test in Phase 1 verifies sum(transactions) == displayed balance.

---

## ADR-010 — Celery + Redis for background jobs (not FastAPI BackgroundTasks)

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Music generation is 30–180 s per job; jobs must survive API restarts; must retry on provider failure; must scale workers independently of API.
**Decision:** Celery worker processes poll providers and download results. Redis as broker. API enqueues only.
**Alternatives:** FastAPI BackgroundTasks (dies with process — rejected); RQ (viable but Celery has richer retry/observability ecosystem); AWS SQS (adds cloud lock-in).
**Consequences:** Extra service in docker-compose. Worker count = separate scaling dimension. Task names are stable contract (see ARCHITECTURE.md §3).

---

## ADR-011 — Room for local cache, not as a source of truth

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Android needs offline library, cached song metadata, job history. But server is authoritative for jobs, credits, and public content.
**Decision:** Room caches server state for offline reading. Writes go to server first, then to Room. No offline-only writes for jobs, credits, or publishing.
**Alternatives:** Full offline-first with sync (over-scoped for MVP).
**Consequences:** `IMPLEMENTATION_STATUS.md` must track which entities are cache-only. Sync service in Phase 5 handles reconcile on reconnect.

---

## ADR-012 — JWT auth: access (short) + refresh (long) with rotation

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Mobile app needs persistent sessions without storing passwords. Provider API keys never leave backend.
**Decision:** Access token TTL 15 min. Refresh token TTL 30 days, rotated on each refresh. Refresh tokens revocable (stored in `sessions` table). Password reset and email verification via short-lived one-time tokens.
**Alternatives:** Session cookies (bad for mobile — rejected); long-lived access tokens (worse security).
**Consequences:** Refresh endpoint must be idempotent-safe. Logout revokes refresh token server-side. Access token never persisted in plaintext on device — stored in EncryptedSharedPreferences.

---

## ADR-013 — Object storage abstraction, S3-compatible

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Need to store audio, artwork, waveforms, uploads. Must be portable across AWS S3, Cloudflare R2, Backblaze B2 without code change.
**Decision:** `Storage` protocol in `backend/app/storage/base.py`. Local-disk implementation for dev, S3-compatible for prod. All URLs are signed with short TTL (5 min). No public bucket policies.
**Alternatives:** Direct SDK calls in services (rejected — non-portable); public bucket for song URLs (rejected — copyright + cost risk).
**Consequences:** Every `songs/{id}/` result endpoint must regenerate a signed URL on each request. CDN layers on top in Phase 9.

---

## ADR-014 — UI palette: warm gold / deep green / African red / indigo

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Master prompt §4 requires a distinctive African-inspired accent palette, dark mode default, light mode available.
**Decision:** Palette:
- Background dark: `#0B0B0B`
- Surface dark: `#161616` / `#232323`
- Primary: warm gold `#E0A93B`
- Secondary: deep green `#1B3D2F`
- Tertiary: African red `#8E2B22`
- Accent: indigo `#2A2F6E`
- Text: off-white `#F2ECDD`
Light mode: inverted with same accents, off-white background, near-black text.
**Alternatives:** Purple/teal AS template (generic — rejected). Monochrome (fails §4 identity requirement).
**Consequences:** All UI colors come from `core/design/Color.kt`. No hard-coded hex anywhere else. Accessibility contrast checked (≥ 4.5:1 body, ≥ 3:1 large).

---

## ADR-015 — MOCK provider in Phase 1, real provider in Phase 2

**Date:** Phase 0.
**Status:** Accepted.
**Context:** Phase 1 backend must be testable without spending real money or requiring real provider keys. Master prompt §8 mandates MOCK provider for Phase 1.
**Decision:** Phase 1 ships `MockMusicProvider` returning a bundled royalty-free sample. Every mock job is labeled `provider="mock"` in DB and response. UI shows "MOCK" badge. Phase 2 replaces mock in production but keeps it for tests.
**Alternatives:** Skip mock, wire real provider immediately (rejected — tests need network + cost). Fake mock that returns same result instantly (rejected — must simulate realistic 5–10 s delay to test polling correctly).
**Consequences:** Mock behavior documented in TESTING.md. No real provider is enabled in Phase 1. Phase 2 acceptance gate includes deleting the mock badge in production but keeping the mock provider class for automated tests.

---

## Open questions (to be resolved by new ADRs)

- ElevenLabs pricing per minute — when confirmed, add ADR.\
- Anthropic model tier choice (Haiku vs Sonnet) — Phase 2 ADR.\
- Which OpenAI image model is primary — Phase 8 ADR.\
- Whether to expose "Composition Plan" or hide behind AI assistant — Phase 4 ADR.\
- Retention policy details (30-day default) — Phase 9 ADR.\

---

**End of DECISIONS.md (Phase 0).**

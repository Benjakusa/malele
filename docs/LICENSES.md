# Licenses

**Status:** Phase 0. Live document — updated whenever a dependency is added or upgraded.
**Last updated:** Phase 0.

Every dependency and asset must have a commercially compatible license. This document is the audit trail. **Never add a dependency without adding a row here first.**

---

## 1. License policy

- **Allowed:** MIT, Apache-2.0, BSD-2-Clause, BSD-3-Clause, ISC, MPL-2.0, LGPL-2.1/3.0 (dynamic linking only), CC-BY-4.0, CC-BY-SA-4.0 (with attribution), Unlicense, Public Domain.
- **Not allowed:** GPL-2.0, GPL-3.0, AGPL-3.0, SSPL, Commons Clause, non-commercial-only, proprietary licenses that forbid mobile distribution, "free for non-production".
- **LGPL:** allowed **only** for dynamically-linked libraries. FFmpeg must be an LGPL build — no `--enable-gpl` flag.
- **Samples and audio assets:** only CC0, CC-BY, or explicit commercial license with written record. Never scrape audio.

---

## 2. Backend dependencies

| Package | Version pin | License | Notes |
| --- | --- | --- | --- |
| fastapi | latest 0.x | MIT | Core web framework |
| pydantic | v2 | MIT | Schemas |
| pydantic-settings | latest | MIT | Config |
| sqlalchemy | 2.x | MIT | ORM |
| alembic | latest | MIT | Migrations |
| psycopg | v3 | LGPL-3.0 | Postgres driver. LGPL dynamic — OK. |
| asyncpg | latest | Apache-2.0 | Alternative PG driver |
| redis | latest | MIT | Python client |
| celery | latest | BSD-3-Clause | Task queue |
| httpx | latest | BSD-3-Clause | HTTP client |
| uvicorn | latest | BSD-3-Clause | ASGI server |
| gunicorn | latest | MIT | Process manager in prod |
| python-jose or pyjwt | latest | MIT | JWT |
| argon2-cffi | latest | MIT | Password hashing |
| boto3 | latest | Apache-2.0 | S3 SDK |
| pillow | latest | HPND (BSD-like) | Image processing |
| librosa | latest | ISC | Audio analysis |
| numpy | latest | BSD-3-Clause | Numeric |
| pytest | latest | MIT | Tests |
| pytest-postgresql | latest | MIT | Test fixtures |
| pydantic-extra-types | latest | MIT | Extra validators |
| structlog | latest | Apache-2.0 | Structured logging |
| python-multipart | latest | Apache-2.0 | Multipart parsing |

**System dependencies:**
- **FFmpeg** — LGPL-2.1+ build required. Do not ship `--enable-gpl`. Configured in `backend/Dockerfile`. If building from source, pin the exact version and its config flags in this doc.
- **PostgreSQL 16** — PostgreSQL License (BSD-like).
- **Redis 7** — RSALv2 / SSPL dual-license. **Note:** Redis 7 is not OSI-approved anymore. Alternative: **Valkey** (BSD-3-Clause fork) or **KeyDB**. Decision tracked in KNOWN_GAPS. For MVP we use Valkey.
- **Docker, docker compose** — Apache-2.0 / MIT.

---

## 3. Android dependencies

| Package | License | Notes |
| --- | --- | --- |
| Kotlin stdlib | Apache-2.0 | |
| AndroidX (all artifacts) | Apache-2.0 | |
| Jetpack Compose (BOM) | Apache-2.0 | |
| Material 3 | Apache-2.0 | |
| Material Icons Extended | Apache-2.0 | |
| Hilt | Apache-2.0 | |
| Room | Apache-2.0 | |
| Navigation Compose | Apache-2.0 | |
| Retrofit | Apache-2.0 | |
| OkHttp | Apache-2.0 | |
| Kotlinx Serialization | Apache-2.0 | |
| Kotlinx Coroutines | Apache-2.0 | |
| Kotlinx Datetime | Apache-2.0 | |
| Media3 ExoPlayer | Apache-2.0 | |
| Media3 Transformer | Apache-2.0 | |
| WorkManager | Apache-2.0 | |
| Security Crypto (EncryptedSharedPreferences) | Apache-2.0 | |
| Coil (image loading) | Apache-2.0 | |
| Accompanist (if used) | Apache-2.0 | |

---

## 4. AI provider license terms

These are **commercial terms**, not code licenses — read carefully before enabling any provider in production.

| Provider | Service terms URL | Commercial use | Attribution | Restriction |
| --- | --- | --- | --- | --- |
| ElevenLabs Music | elevenlabs.io/docs/legal/music-terms | Yes on paid plans | Some plans require "Made with ElevenLabs Music" | No model resale, no music-library redistribution |
| fal.ai (per model) | fal.ai/legal/terms | Per-model | Per-model | Per-model — verify each before enabling |
| Anthropic Claude | anthropic.com/legal | Yes | No | No content generation that reproduces copyrighted works verbatim |
| OpenAI (images) | openai.com/policies | Yes | No | No real-person likeness, no copyrighted characters |

---

## 5. Audio sample licensing

The bundled royalty-free sample for `MockMusicProvider` in Phase 1 must be **CC0 or explicitly commercially licensed**.

**Sources allowed:**
- freesound.org with CC0 filter and email-verified download record.
- Free Music Archive with explicit commercial license.
- Self-generated with a documented tool (our own DAW, or licensed generation).

**Sources forbidden:**
- YouTube rips, Spotify downloads, StreamRip, any file without a license proof.
- "Royalty-free" libraries with terms that forbid commercial redistribution (e.g., some "free" sample packs).

**Phase 1 acceptance requires:** the exact sample file, its source URL, its license text, and its author recorded in `assets/samples/LICENSE.txt`.

---

## 6. Fonts

System default (Roboto) at Phase 0. If a custom display font is added:
- Must be SIL Open Font License or Apache-2.0.
- Full license text shipped in `assets/fonts/LICENSE.txt`.
- Not Google Fonts CDN — bundle locally.

---

## 7. Attribution obligations

Ship an in-app "Open source licenses" screen (Phase 9) generated from all license files. Minimum content:
- Every Apache-2.0 dependency — full NOTICE text preserved.
- Every MIT dependency — copyright line + license text.
- Every CC-BY sample — author, source, license, link.
- Provider attribution as required by their ToS (e.g., "Music generated by ElevenLabs" where applicable).

---

## 8. Review protocol

- **Before adding a dependency:** check license, add row here, then run `pip install` or `gradle add`.
- **On every dependency bump:** verify license unchanged, update version pin here.
- **Before Play Store publish (Phase 9):** `pip-licenses` for Python, `gradle-license-report` for Android. Diff against this doc. Zero unexplained entries allowed.

---

**End of LICENSES.md (Phase 0).**

# Malele

**AI music creation platform with a strong African music identity.**

Generate full songs from text, write lyrics with AI, create instrumentals, record your own vocals, mix in a lightweight studio, and share with a creator community — with first-class support for Benga, Rhumba, Soukous, Gospel, Afro-dance, Reggae, and Slow African styles.

**Status:** Phase 0 (planning and research) complete. Phase 1 (backend foundation) awaits approval.

---

## Repository layout

```
android/    Kotlin + Jetpack Compose + Material 3 app
backend/    FastAPI + PostgreSQL + Redis + Celery
docs/       11 mandatory project documents
scripts/    Developer + deployment scripts
assets/     Shared sample audio, icons, reference artwork
```

---

## Documentation index — `docs/`

| Doc | Purpose |
| --- | --- |
| [PLAN.md](docs/PLAN.md) | 10-phase roadmap with acceptance gates |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | System, Android, backend, DB, storage architecture |
| [DECISIONS.md](docs/DECISIONS.md) | 15 ADRs — every non-obvious technical choice |
| [IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md) | Live status board, updated each phase |
| [KNOWN_GAPS.md](docs/KNOWN_GAPS.md) | What we do not have, cannot verify, or deferred |
| [TESTING.md](docs/TESTING.md) | 4-layer test strategy + coverage policy |
| [SECURITY.md](docs/SECURITY.md) | Threat model, auth, authz, incident response |
| [LICENSES.md](docs/LICENSES.md) | Every dependency, every license, every obligation |
| [API_INTEGRATIONS.md](docs/API_INTEGRATIONS.md) | Provider research: ElevenLabs, fal, Lyria, Anthropic, OpenAI |
| [COST_MODEL.md](docs/COST_MODEL.md) | Unit cost + 5-scale monthly cost, ceilings, kill switches |

---

## Getting started

### Prerequisites
- JDK 17
- Android SDK — platform 35, build-tools 35.0.0
- Python 3.13 (via pyenv)
- Docker + Docker Compose

### Local setup
```
git clone <repo>
cd malele
python3 -m venv ~/.venvs/malele
source ~/.venvs/malele/bin/activate
```

### Phase 1 (when approved)
```
cd backend
cp .env.example .env  # fill in values
docker compose up -d
alembic upgrade head
pytest
```

---

## Development phases

Phase 0 — Planning and research (COMPLETE)
Phase 1 — Backend foundation (auth, jobs, mock provider)
Phase 2 — Real AI provider integrations (ElevenLabs, fal, Anthropic)
Phase 3 — Android foundation and design system
Phase 4 — Create experience, lyrics, AI assistant
Phase 5 — Song results, player, library
Phase 6 — Song editing and version control
Phase 7 — Studio Lite and vocal recording
Phase 8 — Cover artwork, explore, creator community
Phase 9 — Credits, settings, production readiness

**One phase per session. No phase begins without approval of the previous completion report.** See [PLAN.md](docs/PLAN.md).

---

## Key rules

- Provider API keys **never** in the Android app. Only in the backend env vars.
- Every external integration goes through a `Provider` interface. No provider name outside its adapter.
- If it is a mock, it is labeled **MOCK** in the code and UI.
- Real tests, not stubs. No phase closes without green tests.
- Cost is tracked per operation. See [COST_MODEL.md](docs/COST_MODEL.md).

---

## License

Proprietary — see [LICENSES.md](docs/LICENSES.md) for dependency licenses and third-party obligations.

---

**Malele — Phase 0 complete. Awaiting Phase 1 approval.**

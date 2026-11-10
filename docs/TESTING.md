# Testing

**Status:** Phase 0. Live document — updated at every phase.
**Last updated:** Phase 0.

How Malele is tested. **A phase does not close until its tests are green.**

---

## 1. Testing strategy

Four layers, each with a distinct purpose:

| Layer | What | Speed | Cost | Run when |
| --- | --- | --- | --- | --- |
| **Unit** | Pure functions, algorithms, parsers, mappers, validators | ms | Free | On every save |
| **Integration** | Real DB, real Redis, real HTTP against backend | sec | Free | Before every commit |
| **Contract** | Provider adapters against recorded fixtures (VCR) | sec | Free | Before every commit |
| **End-to-end (E2E)** | Real device + real backend + real provider | min | Costs money | Before every release, plus a small daily smoke |

**Rule:** every test is either **fast and free** (unit, integration, contract) or **deliberately expensive** (E2E). Never write a test that costs money but runs by accident.

---

## 2. Backend testing

### Unit tests — `backend/tests/unit/`
- Password hashing (round-trip + Argon2 params check)
- JWT encode/decode/expiry/revocation
- Rate limit algorithm
- Credit arithmetic: reserve, debit, refund, idempotency
- Job state machine: legal transitions only
- Prompt template rendering
- Lyric copyright validator: substring + paraphrase
- Audio metadata: duration, waveform peaks, loudness
- FFmpeg wrapper: argument construction, output validation
- Error code mapping

### Integration tests — `backend/tests/integration/`
- `POST /v1/auth/register` → user in DB, JWT returned
- `POST /v1/auth/login` → new JWT, old session still valid
- Refresh token rotation: old token invalidated after use
- Logout: session removed, access token rejected
- Two-user isolation: user A cannot read user B songs/jobs (parametrized over all user-scoped endpoints)
- `POST /v1/generations` → job queued, credits reserved
- Mock job → `COMPLETED`, credits debited, refund on failure
- Idempotency: same `Idempotency-Key` twice → same job, single charge
- Rate limit: 429 after threshold, `Retry-After` header present
- Signed URL: valid within TTL, invalid after
- Account deletion: audio purged from storage, DB rows purged

### Contract tests — `backend/tests/contracts/`
- Each provider adapter against a recorded request/response fixture (e.g., `elevenlabs_compose_ok.json`)
- Fixture refresh: `pytest --record-fixtures` against real provider with a **single cheap call**, gated behind `LIVE_PROVIDER_TESTS=1` env var and a `--max-cost` limit
- Adapter error handling: 4xx fixture → provider raises correct internal error; 5xx fixture → retry policy
- Never a live call in CI unless explicitly enabled.

### End-to-end — `backend/tests/e2e/`
- One real ElevenLabs song generation with all acceptance criteria from Phase 2
- One real fal song generation
- One real Anthropic lyrics generation
- Cost tracked and logged; test asserts cost is within expected range
- African genre fidelity: 8 genres × 1 sample each → human listening notes into `AFRICAN_GENRE_TESTS.md`
- **Daily smoke** (optional): a scheduled job runs one cheap provider call to detect outages early

**Run commands:**
- `pytest` — unit + integration, no network, no cost, < 60 s target
- `pytest -m contract` — contract tests only
- `pytest -m e2e` — real providers, requires env vars + budget
- `pytest --record-fixtures` — refresh fixtures (requires `LIVE_PROVIDER_TESTS=1`)

---

## 3. Android testing

### Unit tests — `android/core/**/src/test/` and `android/feature/**/src/test/`
- Every ViewModel: state transitions with a fake repository
- Every repository: contract test against MockWebServer
- Every mapper: DTO → domain model round-trip
- Audio: waveform peak extraction, seek math
- Date/duration formatting

### Instrumented tests — `android/**/src/androidTest/`
- Critical UI flows: login, create song flow, play song, download
- Rotation on the player screen preserves state
- Process death: `SavedStateHandle` restores in-progress creation form
- Offline: airplane mode + cached song plays
- Audio focus: incoming call pauses playback; resume after
- Headphone unplug pauses playback
- Requires a real device or Android emulator with API 26+

### Manual test checklist (run before every Android release)
- Install clean on device
- First launch: onboarding → login → Create tab loads
- Create a song (mock in Phase 1–3, real from Phase 4)
- Cancel a generation mid-flight; verify credits refunded
- Play a generated song; verify no gaps, waveform sync
- Background: home button → audio continues → notification visible
- Kill app: return → job status preserved, resumes from server
- Airplane mode: cached song plays, new generation blocked with clear error
- Re-enable network: pending job resumes
- Export WAV and M4A: both play on device and on desktop
- Account delete: user data gone from device and server

---

## 4. End-to-end verification workflow (the integration test)

**The canonical flow that must work end-to-end before Phase 9 is approved:**

1. Register new user → email verification
2. Login → JWT
3. Create song: describe + genre + vocals + language
4. AI writes lyrics for the description
5. User edits lyrics (adds a verse, moves the bridge)
6. Enhance prompt: original vs enhanced shown
7. Submit generation → credits reserved
8. Job progress: queued → processing → downloading → completed
9. Notification received
10. Play song (exo player, background OK)
11. Two variations exist and can be compared
12. Favourite + download
13. Kill and reopen app: song persists in library, plays offline
14. Extend the song (Phase 6+)
15. Export to M4A
16. Publish → visible in Explore
17. Follow self from second account → sees published song
18. Report own song → moderation queue entry created
19. Delete account → songs, audio, artwork, credits gone

Each step is scripted where possible (backend), tested manually on a real device where not.

---

## 5. Coverage policy

- **Backend:** target 80% line coverage of `services/` and `providers/`. Below 70% blocks phase close.
- **Android:** target 60% line coverage of ViewModels and Repositories. UI composables excluded from coverage target.
- **Coverage is a floor, not a goal.** A 95% covered module with one missing refund test is a failed phase.

---

## 6. What is NOT tested by automated tests

- **African genre fidelity** — requires human listening. Documented in `AFRICAN_GENRE_TESTS.md`.
- **African language lyric quality** — requires native speaker. Same doc.
- **Provider cost drift** — daily reconciliation alert; not a unit test.
- **Visual polish** — spot-checked manually before release; not screenshot-diffed at Phase 0.
- **Provider outage recovery** — chaos-tested manually in Phase 2, not in CI.

---

## 7. CI plan (to be implemented in Phase 1)

- On every push: unit + integration + contract tests (no live providers).
- On PR to main: full test suite, lint (ruff for Python, ktlint for Kotlin), license check.
- Nightly: optional E2E with `LIVE_PROVIDER_TESTS=1` at low budget.
- No merge without green CI.

---

**End of TESTING.md (Phase 0).**

# Plan

**Status:** Phase 0 planning document.
**Last updated:** Phase 0.

Program-level roadmap. Translates master prompt §8 into an actionable schedule with explicit acceptance gates.

---

## 1. Non-negotiable principles

1. **One phase per session.** No phase begins without explicit user approval of the previous phase completion report.
2. **No fake features.** If something is a mock, it is labeled MOCK in the code and in the UI.
3. **Provider-independent.** No backend code references a specific provider name outside its adapter.
4. **No provider keys on Android.** Only the backend sees provider API keys.
5. **Real tests.** Each phase acceptance gate includes automated tests.
6. **Docs first.** Update the 11 docs at the start and end of every phase.
7. **Cost-aware.** No feature ships without its cost-per-use recorded in COST_MODEL.md.
8. **African genre fidelity.** Any claim about Benga, Rhumba, Soukous, Gospel fidelity must be backed by listening tests, not documentation.

---

## 2. Phase map

| Phase | Title | Acceptance gate |
| --- | --- | --- |
| 0 | Repository, planning, research | 11 docs approved by user |
| 1 | Backend foundation | Auth works, mock song generated end-to-end, tests pass |
| 2 | Real AI providers | ElevenLabs + fal + Anthropic real generation works, cost tracked |
| 3 | Android foundation | App builds, auth works against backend, nav works |
| 4 | Create, lyrics, assistant | Full user flow: describe → generate → notify |
| 5 | Results, player, library | Song plays, background audio, offline cache, export |
| 6 | Editing + versioning | Supported edits work through real API |
| 7 | Studio + vocals | Import, record, mix, export all work |
| 8 | Artwork + explore + community | Public songs sharable, moderation works |
| 9 | Credits + production | Release APK, deployable backend, caps enforced |

Provider decision: **ElevenLabs primary, fal.ai fallback** (option C — approved Phase 0.2).

---

## 3. Phase 0 — Repository inspection + planning + research

**Goal:** Document the project. No application code.

**Sub-steps:**
- 0.1 (DONE) Repository skeleton + README + .gitignore + venv
- 0.2 (DONE) API_INTEGRATIONS.md + COST_MODEL.md
- 0.3 (IN PROGRESS) PLAN.md + ARCHITECTURE.md + DECISIONS.md + IMPLEMENTATION_STATUS.md + KNOWN_GAPS.md
- 0.4 (NOT STARTED) SECURITY.md + LICENSES.md + TESTING.md + final README.md

**Status:** 2 / 11 docs done.

---

## 4. Phase 1 — Backend foundation

**Goal:** Working FastAPI backend that can register, log in, accept a generation request, run a mock job to completion, and return downloadable mock audio. All credit accounting works.

**Deliverables:**
- FastAPI structure matching master prompt §5
- SQLAlchemy 2.x + Alembic migrations (Postgres)
- JWT auth (access + refresh) + password reset + email verification stubs
- Roles: guest / user / creator / admin / superadmin
- Redis + Celery worker
- `MusicProvider` interface + `MockMusicProvider` (bundled royalty-free audio)
- Generation job model + state machine: draft → queued → processing → downloading → completed/failed/cancelled
- Credit ledger with reservations, transactions, idempotent refunds
- Rate limiting middleware + structured errors with stable codes
- Endpoints: `/v1/auth/*`, `/v1/me`, `/v1/prompt/build`, `/v1/prompt/enhance`, `/v1/generations`, `/v1/generations/{id}`, `/v1/generations/{id}/results`, `/v1/songs`, `/v1/songs/{id}`
- Docker Compose: postgres + redis + api + worker
- Pytest: auth, jobs, credits, refunds, isolation
- OpenAPI auto-generated

**Acceptance gate:**
1. `docker compose up` starts all 4 services.
2. `alembic upgrade head` runs.
3. Register → login → new JWT works.
4. `POST /v1/generations` → job queued, credits reserved.
5. Worker processes mock job → `GET /v1/generations/{id}` shows `completed`.
6. Mock audio downloadable.
7. Failed job → refund issued.
8. Two users cannot see each other songs.
9. `pytest` all green.

**Not in Phase 1:** real providers, Android, image gen, publishing, discovery, studio.

---

## 5. Phase 2 — Real AI provider integrations

**Goal:** Replace MockMusicProvider with real ElevenLabs (primary) + fal.ai (fallback). Wire real Anthropic lyrics. Prove capability routing and fallback.

**Deliverables:**
- `ElevenLabsMusicProvider` — compose, composition plan, inpainting, streaming, word timestamps
- `FalMusicProvider` — queue API, `model_id` from DB, per-model capability flags
- `AnthropicLyricsProvider` — strict JSON output + copyrighted-lyrics validator
- `PromptEnhancer` service using Anthropic; saves original + enhanced prompts
- `ProviderRouter` — capability selection + fallback per API_INTEGRATIONS.md §2
- Provider cost tracker writes to `CreditTransaction.metadata`
- Provider failure → automatic refund
- African genre fidelity test harness: 8 genres × 3 durations = 24 generations per provider
- `docs/AFRICAN_GENRE_TESTS.md` with honest quality notes

**Acceptance gate:**
1. One real song through ElevenLabs end-to-end.
2. One real song through fal end-to-end.
3. One real lyrics generation through Anthropic.
4. Instrumental-only request correctly routed.
5. Simulated provider 500 → fallback only if capability-satisfying.
6. `pytest` all green.
7. **African genre test results documented** — go/no-go for product identity.

**Blocking question:** ElevenLabs price per song — must confirm from account before deployment.

---

## 6. Phase 3 — Android foundation + design system

**Goal:** Compose shell that authenticates against the real backend, navigates between 4 main tabs, switches dark/light theme.

**Deliverables:**
- Android Studio project — Kotlin, Compose, Material 3, Hilt, Room, Retrofit, Navigation
- Modular structure per master prompt §5
- Theme system (African-inspired palette, dark + light) driven by `Color.kt`
- Bottom nav: Create, Explore, Library, Profile
- Auth flow (login/register) against real backend
- API client with token refresh interceptor
- Room schema for local cache (songs, jobs)
- Repository layer between ViewModels and data sources
- Reusable components: buttons, inputs, genre chips, song cards, artwork cards, loading states
- Empty/error/loading screens for every tab

**Acceptance gate:**
1. `./gradlew :app:assembleDebug` produces APK.
2. Login with backend credentials succeeds and persists across restart.
3. All 4 tabs navigate correctly with state preservation.
4. Theme switches and survives restart.
5. Logged-out users see public tabs, logged-in users see all.

**Blocking question:** Backend HTTPS URL and cert chain for the Android build.

---

## 7. Phase 4 — Create, lyrics, AI assistant

**Goal:** Complete creation workflow — describe song → AI lyrics → enhance prompt → submit generation → progress → notification.

**Deliverables:**
- Simple creation mode (prompt + genre + vocals + language + duration)
- Custom creation mode (full 25+ controls per master prompt Module B)
- Genre picker driven by backend `GenreConfiguration`
- Lyrics editor with section labels (intro, verse, pre-chorus, chorus, verse 2, bridge, instrumental, final chorus, outro)
- AI lyrics generation with section-level regeneration
- AI music assistant conversational flow
- Prompt enhancement with before/after diff
- Job submission + progress screen + cancel + completion notification
- Preserve user input across navigation + process death

**Acceptance gate:**
1. Full flow: register → describe → generate lyrics → edit → enhance → submit → progress → notification.
2. Prompt enhancement shows original vs enhanced with editable result.
3. Language selection (Swahili, Luo, Lingala, English) reaches the request payload.
4. Failed generation shows a friendly error and refunds credits.
5. Navigating away and back preserves form state.

**Blocking question:** Anthropic model tier — Haiku (cheap) vs Sonnet (quality).

---

## 8. Phase 5 — Results, player, library

**Goal:** Play generated songs, compare variations, save to library, work offline, export.

**Deliverables:**
- Song result screen with artwork, metadata, player
- Variation switcher (2 variations default)
- ExoPlayer with background playback + lockscreen controls + audio focus
- Waveform display (peaks from backend, not client-decoded)
- Seek via waveform tap
- Library screen: search, sort, filter, favourite, rename, duplicate, delete
- Offline cache of downloaded songs + artwork
- Export to WAV + M4A via Media3 Transformer
- Share sheet with "Made with Malele"

**Acceptance gate:**
1. Generated song plays without gaps.
2. Two variations both playable and comparable.
3. Audio continues in background; media notification shows.
4. Downloaded song plays in airplane mode.
5. Export produces valid WAV/M4A that plays on device.
6. Share sheet opens with correct text.

---

## 9. Phase 6 — Song editing + version control

**Goal:** Extend, replace-section, remix, instrument version, with full version history. **Only operations the active provider actually supports are shown.**

**Deliverables:**
- Extend (ElevenLabs inpainting)
- Replace-section (ElevenLabs inpainting with `start_ms`, `end_ms`, `prompt`)
- Add intro, add outro (via inpainting)
- Regenerate lyrics (new song, same lyrics)
- Instrumental version
- Remix
- Version tree UI (parent → child versions, cost, provider, date)
- Restore previous version
- Unsupported ops hidden with explanation

**Acceptance gate:**
1. Each supported edit produces a new version, original preserved.
2. Version tree shows correct parentage.
3. Restore works.
4. Unsupported ops (e.g., inpainting on fal) hidden with explanation.
5. Cost recorded per version.

**Blocking question:** ElevenLabs inpainting pricing — confirm from account.

---

## 10. Phase 7 — Studio Lite + own vocals

**Goal:** Lightweight production workspace: timeline, clips, mixer, recording, mixdown, export.

**Deliverables:**
- Studio project type (separate from song)
- Multi-track timeline with waveform, trim, split, move, fade
- Import stems + import user audio
- Recording with count-in, input meter, multiple takes
- Mixer: volume, pan, mute, solo, master output
- Mixdown via Media3 Transformer to WAV + M4A
- Non-destructive edits until export

**Acceptance gate:**
1. Import audio, drag to timeline, trim, split, export plays correctly.
2. Record vocal with count-in, playback aligns with track.
3. Mixer mute/solo/volume changes affect output.
4. Memory + CPU stay within budget on a mid-range device.

---

## 11. Phase 8 — Artwork, explore, creator community

**Goal:** Artwork generation + publish + follow + like + playlists + discovery + moderation.

**Deliverables:**
- Image generation service (OpenAI `gpt-image-1.5`) + upload + regenerate
- Publish / unpublish with private / public / unlisted visibility
- Creator profiles with real data
- Follow / unfollow / like persistence
- Playlists (create, add, remove, order)
- Explore tabs: trending, featured, by genre, by language, by mood
- Search across titles, creators, genres, languages
- Content reporting + moderation queue
- Notifications

**Acceptance gate:**
1. Public song has a shareable link that opens without auth.
2. Private song is not visible to other users.
3. Follow/like persists across restart.
4. Explore only shows real published content.
5. Report submission creates a moderation record.

**Blocking question:** OpenAI image content-policy edge cases for African cultural prompts — 20-prompt sample required.

---

## 12. Phase 9 — Credits, settings, production readiness

**Goal:** Full credit accounting, account deletion, security audit, release APK, deployment docs.

**Deliverables:**
- User-visible credit balance, history, estimates
- Credit reservation before generation, refund on failure
- Usage limits per user / per IP / global
- Provider spending ceilings + kill switches
- Account settings, notification settings, privacy settings, account deletion
- Data retention enforcement + storage cleanup
- Security audit pass (auth, injection, SSRF, rate limits)
- Release APK signed + tested
- Deployment guide

**Acceptance gate:**
1. Credit accounting correct in every scenario.
2. Duplicate requests cannot double-charge.
3. Failed jobs refunded.
4. Data deletion works and is verified in DB.
5. Release APK builds, installs, runs.
6. Backend deployable from documented steps.
7. Cost ceilings + kill switches demonstrably work.

---

## 13. Cross-cutting concerns (applied at every phase)

- **Testing:** unit + integration tests every phase. Acceptance gate requires green.
- **Security:** no secrets in source, no provider keys in Android, all auth validated, all user data isolated.
- **Performance:** p95 latency, memory, battery — measured and recorded.
- **Accessibility:** screen reader descriptions, scalable text, contrast, touch targets ≥48dp.
- **Observability:** structured logs, request IDs, provider cost telemetry.
- **Documentation:** 11 docs kept current. IMPLEMENTATION_STATUS.md updated at phase end.

---

## 14. Risk register

| Risk | Likelihood | Impact | Mitigation |
| --- | --- | --- | --- |
| Provider cost spikes burn budget | Medium | High | Kill switches + hard cost caps (COST_MODEL §4) |
| African genre fidelity is poor | Medium | High | Phase 2 listening tests. If poor, adjust prompt engineering; may require different provider |
| African language lyric quality is poor | Medium | High | Phase 2 native speaker tests. Fall back to Swahili + English if needed |
| Provider outage | High | Medium | Fallback provider (fal) + graceful error UX |
| Provider ToS change | Low | High | Provider-independent architecture; swap adapter |
| Copyright / content policy violation | Medium | High | Lyric validator + moderation + reporting; do not generate artist likenesses |
| Android audio latency | Medium | Medium | Test on real device; Media3 is well-trodden |
| Cost underestimation in COST_MODEL | High | Medium | Refresh every phase; add 30% buffer |
| Scope creep (MVP → DAW) | High | High | Phase gates enforced; Studio Lite, not a DAW |

---

**End of PLAN.md (Phase 0).**

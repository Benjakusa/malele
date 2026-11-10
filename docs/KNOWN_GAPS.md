# Known Gaps

**Status:** Phase 0. Live document — update at the end of every phase.
**Last updated:** Phase 0.

Things Malele **does not have yet**, **cannot verify yet**, or has **deferred**. This is the "truth" page — a gap is only closed by updating this doc and linking to the change.

---

## 1. Provider uncertainties (must resolve before production)

| Gap | Impact | Resolve by |
| --- | --- | --- |
| ElevenLabs Music per-song price not published on docs | Cost model is estimate only | Pull from account dashboard in Phase 2 |
| fal.ai MiniMax 2.6 per-second price not verified | Fallback cost is estimate | fal dashboard in Phase 2 |
| fal.ai MiniMax 2.6 commercial-use status not confirmed | Cannot serve public songs through fal until confirmed | Written confirmation from fal in Phase 2 |
| African genre fidelity unverified | Product identity risk | Listening tests Phase 2 (see `AFRICAN_GENRE_TESTS.md`) |
| African language lyric quality unverified | Adoption risk | Native speaker review Phase 2/4 |
| ElevenLabs inpainting cost per operation unknown | Extend/replace-section cost model rough | Confirm in Phase 2 |
| Anthropic model tier not chosen | Lyrics cost + quality unknown | Benchmark in Phase 2 |
| OpenAI image content-policy edge cases for African cultural prompts | Artwork rejection rate unknown | Sample 20 prompts in Phase 8 |

---

## 2. Deferred features (deliberately not in MVP)

| Feature | Why deferred | Phase planned |
| --- | --- | --- |
| Google Lyria integration | English-only, 32.8 s max, no vocals — does not meet song requirements | Post-MVP, or never |
| Voice cloning / impersonation | Prohibited by product policy | Never |
| Payment processing / subscriptions | Not needed until usage validated | Phase 9+ separately |
| Stems (true separation) | Providers return full mix; real separation is extra cost + licensing | Phase 7+ if provider returns stems |
| Live instrument / MIDI input | Studio Lite is not a DAW | Not planned |
| Collaborative editing | Not in scope | Post-launch |
| Web app | Not in scope | Post-launch |
| Lyrics in extra languages beyond EN/SW/LUO/LINGALA/KIKUYU/LUHYA/KAMBA | Only ship what we can test | Grow per phase |

---

## 3. Technical gaps (known but not blocking)

| Gap | Impact | Mitigation |
| --- | --- | --- |
| Package ID — final `com.benjakusa.malele` chosen | **Resolved at Phase 3 start** | No action needed |
| No HTTPS in local dev | Android may need `network_security_config.xml` exception | Documented in Phase 3 |
| Push notifications not yet wired | Phase 1 uses polling only | FCM in Phase 4 or 5 |
| No app-level crash reporting (Sentry/Crashlytics) | Crashes invisible in beta | Add in Phase 3 |
| Email sending is a stub | Password reset / verification is manual | Real SMTP in Phase 9 |
| Redis used only as Celery broker, not yet as cache | Redundant DB hits | Add cache in Phase 5 |
| FFmpeg is a system dependency, not bundled | Deploy friction | Dockerfile bakes it; document in Phase 9 |
| No CDN in Phase 0–8 | Audio downloads slow at scale | Phase 9 |
| No automated license scanning in CI | License drift risk | Phase 9 |
| No mobile-side encryption audit yet | Tokens stored in EncryptedSharedPreferences but untested | Security pass in Phase 9 |

---

## 4. Provider capability gaps (documented but not supported)

| Capability | ElevenLabs | fal.ai (MiniMax 2.6) | Notes |
| --- | --- | --- | --- |
| Extend song | ✅ (inpainting) | ❌ | Hide in UI when fal is the active provider |
| Replace section | ✅ (inpainting) | ❌ | Same |
| Streaming | ✅ | Partial (queue, not streaming) | SSE in Phase 6 |
| Stems | ❌ (returns mix) | ❌ (returns mix) | Never claim stems from these |
| Word-level timestamps | ✅ | ❌ | Only ElevenLabs path can do karaoke sync |
| Custom composition plan | ✅ (`create-composition-plan`) | ⚠️ (varies by model) | Provider-dependent |

**Product rule (from master prompt §8 Module I):** unsupported operations are **hidden** with a clear explanation, never shown as fake-working controls.

---

## 5. Security and compliance gaps

| Gap | Impact | Phase to close |
| --- | --- | --- |
| No threat model document yet | Blind spots | Phase 1 |
| No pen-test scheduled | Pre-launch risk | Before Play Store publish |
| Copyright validator fingerprint DB is small (100 songs) | False negatives possible | Grow continuously; log rejections |
| GDPR / Kenya DPA compliance not audited | Legal risk in EU/KE | Before public launch |
| Moderation queue is rule-based in Phase 1 | Misses subtle abuse | Add ML classifier in Phase 8 |

---

## 6. Cost gaps

| Gap | Impact | Phase to close |
| --- | --- | --- |
| COST_MODEL is a range, not a single number | Unit economics fuzzy | Phase 2 (real prices) |
| No per-user cost tracker yet | Cannot bill or cap precisely | Phase 1 (backend) + Phase 9 (user-facing) |
| No provider daily quota tracking | Surprise rate limit hits | Phase 2 |

---

## 7. What is NOT a gap (deliberate design)

These are **choices**, not omissions — listed here so they are not re-opened by accident:
- No XML layouts (ADR-007) — Compose-only.
- No Oboe / NDK in MVP (ADR-005) — AudioTrack is sufficient.
- No Elasticsearch / Meilisearch — Postgres full-text search is enough until 100k+ songs.
- No Kubernetes — Docker Compose + single VPS until scale demands it.
- No GraphQL — REST is enough; FastAPI OpenAPI is the contract.
- No custom audio DSP — Media3 Transformer covers mixdown.

---

## 8. Review protocol

At the end of every phase:
1. **Close** gaps resolved by that phase (delete row, note in IMPLEMENTATION_STATUS).
2. **Add** new gaps discovered during that phase.
3. **Never** silently drop a gap — if superseded, mark it `[SUPERSEDED by ADR-NNN]`.

**End of KNOWN_GAPS.md (Phase 0).**

---

## 9. Phase 2 gaps (added Phase 2 completion)

The following were planned for Phase 2 but deferred:

| Gap | Impact | Planned resolution |
| --- | --- | --- |
| Live ElevenLabs song generation not run | Real API integration unverified; only contract tests pass | Phase 3 or 4 acceptance |
| Live fal.ai generation not run | fal balance $0 | After topping up |
| Live Anthropic lyrics generation not run | Real Claude outputs unverified (only mocked tool_use tested) | Phase 3 or 4 |
| African genre fidelity not assessed | **Product identity risk** — cannot claim go/no-go | Requires live listening (Phase 3) |
| African language lyric quality not assessed | Swahili/Luo/Lingala/Kikuyu output unverified | Requires native speaker review |
| Provider cost per song not measured | Cost model is estimate-only | Add tracking to `CreditTransaction.metadata` when live |
| ElevenLabs inpainting not exercised | Extend/replace-section only contract-tested | Phase 6 |

**These are real gaps, not minor ones.** Phase 2's acceptance criteria from `PLAN.md` §5 are **NOT** fully met:

- ❌ One real song through ElevenLabs — **not done**
- ❌ One real song through fal — **not done** (no balance)
- ❌ One real lyrics generation through Anthropic — **not done**
- ❌ African genre test results — **not done**
- ✅ Instrumental routing, capability selection, fallback logic, credit flow — **all proven via contract tests + mock provider**

**Impact on downstream phases:** Phase 3 (Android foundation) does not require live providers — the app authenticates, navigates, and displays data from the API. The live test can and should happen **before Phase 4** (Create experience), because Phase 4 is the first phase that needs real song output.

**Verdict:** Phase 2 code complete, but **not approved** for Phase 4 start without a live acceptance test.


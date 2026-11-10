# API Integrations

**Status:** Phase 0 research (documented capability only — no experimental verification yet).
**Last updated:** Phase 0.

This document records what each provider officially supports. Nothing here is invented. Where a capability is unverified, it is marked **UNVERIFIED**.

---

## 1. Music generation providers

### 1.1 ElevenLabs Music (PRIMARY)

**Role in Malele:** primary provider for full-song generation, vocals, inpainting (extend / replace-section), instrumental mode, streaming.

**Official documentation:**
- Compose: https://elevenlabs.io/docs/api-reference/music/compose
- Create composition plan: https://elevenlabs.io/docs/api-reference/music/create-composition-plan
- Stream: https://elevenlabs.io/docs/api-reference/music/stream/
- Music API overview: https://elevenlabs.io/docs/capabilities/music
- Music best practices: https://elevenlabs.io/docs/best-practices/music
- Commercial use / media rights: https://elevenlabs.io/docs/legal/music-terms

**Model IDs (verified):**
- `music_v2` — current full-quality model.
- `music_v1` — legacy; do not target for new builds.

**Verified capabilities:**
| Capability | Endpoint / parameter | Notes |
| --- | --- | --- |
| Prompt-based generation | `POST /v1/music` body `prompt` | Prompt- or composition-plan-driven. |
| Composition plan generation | `POST /v1/music/plan` (`create-composition-plan`) | LLM-generated structured plan from prompt. |
| Detailed generation | `POST /v1/music/detailed` | Returns plan + song metadata + audio in one call. |
| Streaming (SSE) | `POST /v1/music/detailed/stream` | Server-sent events with chunked audio. |
| Instrumental only | body `force_instrumental=true` | Verified. |
| Duration control | body `music_length_ms` in [3000, 600000] | 3 s – 10 min. |
| Custom lyrics | body `composition_plan` with sections containing `lyrics` | Structured lyrics via plan. |
| Inpainting (extend / replace-section) | `store_for_inpainting=true` on generation; then `POST /v1/music/inpaint` with `start_ms`, `end_ms`, `prompt` | **This is the only verified way to extend/replace in ElevenLabs.** |
| Word-level timestamps | response field `words[]` with `start_s` / `end_s` | Useful for waveform display + karaoke. |
| Audio formats | `mp3_44100_128`, `mp3_44100_192`, `pcm_44100`, `pcm_48000`, `pcm_22050` | MP3 and PCM. |
| Provenance | All outputs C2PA-signed | Required for module K/AI transparency. |

**Commercial terms (verified from Media Rights table):**
- Commercial use of generated music **is permitted** on paid plans; exact rights depend on plan tier.
- **Prohibited:** reselling the model as-a-service, redistributing as a music library, building a competing model with outputs.
- Some plans require attribution ("Made with ElevenLabs Music"). Plan must be confirmed against account tier.
- **No voice cloning / impersonation** of real artists is permitted by Malele (product-level rule, reinforces provider ToS).

**Cost:** Not published on the public docs page as of Phase 0. **UNVERIFIED.** Must be confirmed from the account dashboard before cost-per-song is fixed in `COST_MODEL.md`. Estimate placeholder in the cost model: **$0.10 – $0.30 per minute of output** (industry range for premium AI music APIs — must be replaced with real numbers).

**African genre performance:** **UNVERIFIED.** No public benchmark of Benga / Rhumba / Soukous / Luo-language vocal quality exists. Phase 2 controlled tests required.

**Integration notes for our backend:**
- Provider adapter: `backend/app/providers/elevenlabs_music.py`.
- Auth: API key in `ELEVENLABS_API_KEY` env var only — **never** on Android.
- Timeouts: submit 30 s; poll 60 s; streaming read 120 s.
- Retry: exponential backoff on 429 / 5xx, max 3 retries; idempotency key on submit.
- Inpainting jobs stored separately from generation jobs (different `ProviderJobRef.kind`).

---

### 1.2 fal.ai (FALLBACK / multi-model router)

**Role in Malele:** fallback when ElevenLabs fails, cost fallback when user has no budget for ElevenLabs, cheap testing during Phase 2. **One-line model swap** through the fal queue API.

**Official documentation:** https://fal.ai/docs

**Verified music models on fal (as of Phase 0):**
| Model | Key capabilities | Notes |
| --- | --- | --- |
| `fal-ai/elevenlabs/music` | Same quality as direct ElevenLabs | Slightly higher per-call price, no inpainting through fal. |
| `fal-ai/minimax-music/v2.6` | Full song + vocals + lyrics, `[Verse]`/`[Chorus]` tags in lyrics, `is_instrumental` flag | **Best structural-lyrics fit.** |
| `fal-ai/minimax-music/v2` | Same as above, older. | Keep for A/B. |
| `fal-ai/ace-step` | 4-minute songs, English + Mandarin, LoRA support | Good variety. |
| `fal-ai/diffrhythm` | Cheap ($0.001/s), commercial use OK, ref-audio conditioning, 95–285 s | Cheap test harness. |
| `fal-ai/lyria` | Wraps Google Lyria — **32.8 s max, English-only** | Only for instrument loops, not full songs. |

**VERIFIED provider-level facts:**
- fal queue API: submit returns `request_id`; poll `/status`; fetch result via `response_url`; webhook URL is supported.
- Auth: `FAL_KEY` env var.
- Commercial use: per-model. `diffrhythm` verified OK; `minimax-music/v2.6` commercial status **UNVERIFIED — must confirm per model before enabling in production**.
- African genre performance: **UNVERIFIED** for all fal-hosted music models.

**Integration notes:**
- Adapter: `backend/app/providers/fal_music.py`.
- The `model_id` is a **database-configurable value**, not hard-coded (per master prompt §6, Module F).
- Retry: same policy as ElevenLabs; fal webhook preferred over polling when available.
- Never expose `FAL_KEY` outside backend.

---

### 1.3 Google Lyria (EVALUATED — REJECTED AS PRIMARY)

**Official documentation:** https://ai.google.dev/gemini-api/docs/music-generation

**Verified capabilities (as of Phase 0):**
- Model: `lyria-002` (GA 27 Oct 2025).
- Max clip length: **32.8 seconds**. Max 4 clips per prompt.
- **English-only prompts.**
- **No vocals, no lyrics, no user-supplied lyrics.**
- Output: 48 kHz WAV, SynthID watermark embedded.
- Pricing: ~$0.04/song (Clip), ~$0.08/song (Pro) per Gemini API pricing page.

**Verdict for Malele: REJECTED as primary.** Malele requires 2–4 minute songs with Luo/Swahili/Lingala vocals and custom lyrics. `lyria-002` cannot satisfy any of these. Module F is explicitly satisfied by ElevenLabs + fal, not Lyria.

**Future use:** if we add a short instrumental-loop generator (Module H background loops, Studio Lite pad), `lyria-002` at $0.04/clip is a good option — evaluate in Phase 7 or later. Not part of Phase 1–6.

**Recommendation to record in `DECISIONS.md`:** *Lyria-002 does not meet Malele minimum song length or vocal requirements. Deferred indefinitely.*

---

## 2. Provider selection contract (implementation target)

Per master prompt Module F, backend exposes a common interface. All adapters implement this contract:

```python
class MusicProvider(Protocol):
    name: str
    async def capabilities(self) -> ProviderCapabilities: ...
    async def submit(self, req: GenerationRequest) -> ProviderJobRef: ...
    async def poll(self, job: ProviderJobRef) -> ProviderJobStatus: ...
    async def fetch(self, job: ProviderJobRef) -> GenerationResult: ...
    async def cancel(self, job: ProviderJobRef) -> None: ...
```

Provider selection logic (implemented in `backend/app/services/provider_router.py`):

1. Determine required capabilities from the generation request (vocals? lyrics? instrumental? inpaint? duration?).
2. Filter providers to those whose `capabilities()` returns `True` for every required capability.
3. Rank eligible providers by: user plan tier → cost → latency → availability (per Phase 2 metrics).
4. Submit to top provider.
5. On submit failure or timeout: retry same provider (2× backoff), then next eligible provider **only if the same capabilities are satisfied**.
6. Never silently downgrade — if no provider supports the requested operation, return an explicit `FEATURE_NOT_SUPPORTED` error with a user-facing explanation.

**Initial routing rules:**
| Operation | Primary | Fallback |
| --- | --- | --- |
| Full song, vocals, custom lyrics | ElevenLabs (`music_v2`) | fal.ai (`minimax-music/v2.6`) |
| Instrumental only | ElevenLabs (`force_instrumental`) | fal.ai (`is_instrumental`) |
| Extend / replace-section (inpainting) | ElevenLabs (only) | — (rejected if unsupported) |
| Cheap test generation | fal.ai (`diffrhythm`) | — (dev/test only) |
| Instrument loop (< 30 s) | Lyria-002 (future) | — (not in MVP) |

---

## 3. Non-music providers (documented in the next command block)

See section 4+ — written by sub-step 0.2b:
- 4. Lyrics / prompt LLM (Anthropic)
- 5. Image generation (OpenAI gpt-image-1.5)
- 6. Audio processing (FFmpeg, librosa)
- 7. Provider dependency matrix (compliance / cost / latency summary)

---

## 4. Lyrics / prompt-LLM provider — Anthropic Claude

**Role in Malele:** songwriting, lyric generation, lyric regeneration per section, prompt enhancement, genre interpretation. Accessed **only** through a `LyricsProvider` interface so another LLM can replace it later.

**Official documentation:** https://platform.claude.com/docs/en/api/messages/create

**Verified endpoint:** `POST /v1/messages` with `model`, `max_tokens`, `messages[]`.

**Model selection:** the exact model ID is set in `backend/.env` as `ANTHROPIC_MODEL`. Allowed values must be current at deploy time; hard-coding a specific model ID in code is prohibited (see master prompt Module F).

**Verified capability assumptions:**
- Structured JSON output via tool-use or a strict JSON schema prompt — used for lyrics sections + prompt-enhance output.
- Long context — supports full song lyrics + genre library + user history in one request.
- Multilingual — Swahili, Luo, Lingala, Kikuyu, Luhya, Kamba support **UNVERIFIED** for lyric quality. Phase 2/4 controlled tests required.

**Pricing:** per Anthropic public pricing page. Model and per-token price in `COST_MODEL.md`; **must be re-checked before deployment**. **UNVERIFIED at Phase 0.**

**Integration notes:**
- Adapter: `backend/app/providers/anthropic_lyrics.py`.
- Timeouts: 60 s per request; retry 429/5xx with backoff ×3.
- Rate limit per user: configurable; enforced in `backend/app/middleware/rate_limit.py`.
- Prompt templates live in `backend/app/prompts/` — versioned files, not inline strings.

**Prohibited (master prompt §7):**
- Generating lyrics that imitate a specific real artist or song.
- Reproducing copyrighted lyrics.
- Voice-cloning prompts that name a real person.
These are enforced by a **system prompt + output validator**, not trust.

---

## 5. Image generation provider — OpenAI images

**Role in Malele:** cover artwork (Module K).

**Official documentation:** https://platform.openai.com/docs/guides/image-generation

**Verified capability:** `gpt-image-1.5` (latest) generates square images; `gpt-image-1` also available; DALL-E 3 legacy. Malele must select the model via env var `OPENAI_IMAGE_MODEL` — do not hard-code.

**Size:** 1024×1024 for square. Other sizes off-scope for MVP.

**Output format:** base64 or URL — our backend downloads and stores in object storage (never exposed directly to client).

**Pricing:** approximate, per OpenAI pricing page as of Phase 0 — `gpt-image-1.5` ~$0.016–$0.15 per image depending on quality/size. **UNVERIFIED — refresh before production.**

**Content restrictions:** OpenAI policy blocks real-person likenesses, copyrighted characters, explicit content. Malele adds its own filter for African cultural representation (do not generate imagery of named real artists or traditional sacred artifacts without user-uploaded reference).

**Integration notes:**
- Adapter: `backend/app/providers/openai_image.py`.
- Timeout 60 s; retry 429/5xx ×3.
- Storage: S3-compatible key `songs/{song_id}/artwork/{hash}.png`.
- Fallback: if image fails, song is created **without** artwork; UI shows placeholder; user can upload or retry. **Never block song generation on artwork failure.**

---

## 6. Audio processing

**Role in Malele:** decode, loudness analysis, waveform generation, format conversion, trim, mixdown. Runs on backend during job processing and on Android during Studio Lite.

**Backend (Python):**
- **FFmpeg** (system binary) — primary decoder/encoder. Invoked via `subprocess` with `-nostdin`, sandboxed paths, whitelist of codecs. No user-controlled FFmpeg flags.
- **ffmpeg-python** or direct subprocess — wrapper choice decided in `DECISIONS.md`.
- **librosa** — waveform peaks (not full PCM in memory), loudness (EBU R128).
- **numpy** — peaks array.

**Android:**
- Media3 `Transformer` for mixdown export (WAV/M4A).
- Media3 `ExoPlayer` for playback.
- Recording: `MediaRecorder` / `AudioRecord` (Studio Lite, Phase 7).

**Never label a mixed-down track as "stems".** Per master prompt §Module F: stems must be actual provider-returned stems or legitimate separation (e.g. Phase 7 with user consent, using a licensed tool). Labeled `"mix"` if not truly separated.

**Licensing:** FFmpeg is LGPL/GPL — configuration must be LGPL-only build (no `--enable-gpl`). Record the exact build in `LICENSES.md`.

**Security:** user-supplied audio paths validated; file sizes capped at 100 MB; MIME sniffed from magic bytes not extension.

---

## 7. Provider dependency matrix

| Category | Primary | Fallback | Env var | Status |
| --- | --- | --- | --- | --- |
| Music | ElevenLabs `music_v2` | fal.ai `minimax-music/v2.6` | `ELEVENLABS_API_KEY`, `FAL_KEY` | Phase 1 mock; Phase 2 real |
| Music (cheap tests) | fal.ai `diffrhythm` | — | `FAL_KEY` | Phase 2 only |
| Music (loops) | Google Lyria-002 | — | `GOOGLE_AI_API_KEY` | Deferred (not MVP) |
| Lyrics / enhancement | Anthropic Claude | — | `ANTHROPIC_API_KEY`, `ANTHROPIC_MODEL` | Phase 2 |
| Image | OpenAI `gpt-image-1.5` | — | `OPENAI_API_KEY`, `OPENAI_IMAGE_MODEL` | Phase 8 |
| Audio processing | FFmpeg + librosa (backend), Media3 (Android) | — | — | Phase 1 |

**Secrets policy (reinforced from master prompt §7):** every key above lives **only** in backend `.env`; `.env` is in `.gitignore`; `.env.example` lists variable names only, never values; Android app has zero provider keys — it talks only to `api.malele` (our backend).

**Rate-limit / quota posture:** each provider adapter records `X-RateLimit-*` headers and propagates them into internal metrics; user-facing generation is blocked (with an explanation) when provider quota is exhausted, **never** silently fails.

---

## 8. Research caveats and open questions

Things Phase 0 could **not** verify and that must be resolved in Phase 2 before committing to production use:

1. **ElevenLabs per-song pricing** — not on the public docs. Confirm from account dashboard.
2. **fal.ai MiniMax 2.6 commercial-use status** — per-model; must be confirmed in writing before any public song is generated through fal.
3. **African genre fidelity** — no public benchmark. Requires controlled test set (Benga, Rhumba, Soukous, Afro-dance, Gospel, Reggae, Slow African, Amapiano, Highlife) with human listening.
4. **African language quality** — Swahili, Luo, Lingala, Kikuyu, Luhya, Kamba. Test at minimum with a native speaker in Phase 2.
5. **Latency at p95** — per provider, per model, measured under real load.
6. **Retry behavior under provider outage** — chaos test in Phase 2.
6b. **Anthropic model ID** — pin one at Phase 2 based on cost/quality test; do not ship a "latest".
7. **OpenAI image content-policy edge cases** for African cultural prompts — sample 20 prompts in Phase 8.

These are documented in `KNOWN_GAPS.md` as well (sub-step 0.3).

---

**End of API_INTEGRATIONS.md (Phase 0).**

# Cost Model

**Status:** Phase 0 estimate. All figures marked `[VERIFIED]` come from public provider pricing pages found during research. All figures marked `[ESTIMATE]` are provisional ranges and **must be replaced with real numbers from the account dashboards before Phase 2 begins**.

**Currency:** USD. **Granularity:** per-song and per-month, at five user scales.

---

## 1. Unit costs per generated song (per variation)

A "song" in Malele = one variation, 2–4 minutes, with vocals + lyrics + artwork. Default UX generates 2 variations per request.

| Line item | Provider | Unit cost | Confidence |
| --- | --- | --- | --- |
| Music generation — full song with vocals (2–4 min) | ElevenLabs `music_v2` | **~$0.10 – $0.30** | [ESTIMATE] — not on public docs; confirm from account |
| Music generation — full song with vocals | fal.ai `minimax-music/v2.6` | **~$0.03 – $0.08** | [ESTIMATE] — verify per-model on fal dashboard |
| Music generation — cheap test | fal.ai `diffrhythm` | **$0.001 / sec** × 120–180 s = **$0.12 – $0.18** | [VERIFIED] — public fal pricing page |
| Lyrics generation / prompt enhancement | Anthropic Claude | **~$0.002 – $0.010** per request | [ESTIMATE] — depends on model; refresh at Phase 2 |
| Cover artwork | OpenAI `gpt-image-1.5` | **$0.016 – $0.15** per image depending on quality | [ESTIMATE] — refresh at Phase 8 |
| Audio processing (waveform, loudness, MP3→OGG) | self-hosted FFmpeg | **~$0.001** per song (compute time) | [ESTIMATE] |
| Storage — audio (4 MB MP3) + artwork (1 MB PNG), 30-day retention | S3-compatible | **~$0.001** per song per month | [ESTIMATE] |
| Bandwidth — first download to user | S3-compatible + CDN | **~$0.002** per song | [ESTIMATE] |

**Summary — cost per generated song (one variation):**

| Path | Music | Lyrics | Artwork | Compute + storage + CDN | **Total per variation** | **Total per 2-variation request** |
| --- | --- | --- | --- | --- | --- | --- |
| ElevenLabs primary (premium) | $0.10–0.30 | $0.005 | $0.05 | $0.004 | **~$0.16 – $0.36** | **~$0.32 – $0.72** |
| fal.ai MiniMax (cost-optimized) | $0.03–0.08 | $0.005 | $0.05 | $0.004 | **~$0.09 – $0.14** | **~$0.18 – $0.28** |
| fal.ai DiffRhythm (test only) | $0.12–0.18 | $0.005 | $0.05 | $0.004 | **~$0.18 – $0.24** | **~$0.36 – $0.48** |

**Planning number used below:** **$0.50 per 2-variation request** on the premium path, and **$0.25** on the fal fallback path. Both deliberately above midpoint to stay safe.

---

## 2. Monthly cost at five user scales

Assumptions:
- Each active user makes **5 song requests per month** (each = 2 variations).
- **70%** of requests hit ElevenLabs, **30%** hit fal (cost-optimized when budget allows).
- Artwork generated **once per request** (2 variations share one cover by default; user can regenerate).
- Backend baseline: 1 small VPS (2 vCPU, 4 GB) + managed PostgreSQL + managed Redis + Celery worker.

| Item | 1 song | 2 variations (1 request) | 5 requests (1 user / mo) | 20 requests (power user) | 100 active users / mo | 1000 active users / mo |
| --- | --- | --- | --- | --- | --- | --- |
| Music cost (blended 70/30) | — | $0.14 – $0.25 | $0.70 – $1.25 | $2.80 – $5.00 | $70 – $125 | $700 – $1,250 |
| Lyrics + prompt enhancement | — | $0.005 | $0.025 | $0.10 | $2.50 | $25 |
| Artwork | — | $0.05 | $0.25 | $1.00 | $25 | $250 |
| Compute (FFmpeg + Celery worker) | — | $0.001 | $0.005 | $0.02 | $10 | $40 |
| Storage + bandwidth | — | $0.003 | $0.015 | $0.06 | $25 | $200 |
| **Backend infrastructure baseline** (fixed) | — | — | — | — | **$30** | **$200** |
| **Total monthly** | — | **$0.20 – $0.31** | **$1.00 – $1.55** | **$3.98 – $6.18** | **~$162 – $213** | **~$1,415 – $1,965** |
| **Per-user monthly** | — | — | — | — | **~$1.62 – $2.13** | **~$1.42 – $1.97** |

Notes:
- At 1000 MAU the backend is dominated by music generation (~60–65%) + artwork (~13%). Storage/CDN is not the bottleneck at this scale.
- If we switch 100% to fal.ai MiniMax, music cost drops ~60%. If we switch 100% to ElevenLabs, music cost rises ~15%.
- These numbers assume **2–4 minute songs**. A 10-minute song (ElevenLabs max) multiplies music cost by ~3×.

---

## 3. Provider-specific pricing (must verify before production)

### ElevenLabs Music
- **Public docs pricing:** [NOT PUBLISHED at Phase 0].
- **Working estimate:** $0.10 – $0.30 per minute of output. For a 3-minute song → $0.30 – $0.90 per variation.
- **Action before Phase 2:** pull real price from ElevenLabs dashboard, replace this row, re-run table above.

### fal.ai music models
- `fal-ai/diffrhythm`: **$0.001 / second** [VERIFIED from fal pricing page].
- `fal-ai/minimax-music/v2.6`: **per-second pricing on fal dashboard — verify per-model** [ESTIMATE $0.0002 – $0.0005 / sec].
- `fal-ai/elevenlabs/music`: matches ElevenLabs pricing plus fal margin (~+10%).
- `fal-ai/lyria`: **~$0.04 per 30 s clip** [VERIFIED from Gemini pricing page].

### Anthropic Claude
- Per Anthropic pricing page — model-dependent.
- A typical lyrics-generation call: ~2k input tokens, ~800 output tokens → **$0.003 – $0.015** depending on model tier.
- **Action before Phase 2:** pin one model, replace the range with a single number.

### OpenAI images
- `gpt-image-1.5`: **~$0.016 – $0.15 per image** by quality/size [VERIFIED from OpenAI pricing page].
- Malele uses `1024×1024` standard quality → **~$0.04** per image.

### Infrastructure (self-hosted baseline)
- **Backend VPS** (2 vCPU, 4 GB, 80 GB SSD): $12 – $24 / mo.
- **Managed PostgreSQL** (1 GB RAM, 10 GB storage): $15 / mo (or self-hosted on VPS for $0).
- **Managed Redis** (250 MB): $5 / mo (or self-hosted for $0).
- **S3-compatible object storage:** $0.02 / GB / mo + $0.01 / GB egress (first 100 GB free on most providers).
- **Baseline total:** **$30 – $45 / mo** fixed.

---

## 4. Cost ceilings and kill switches (to implement in Phase 1/9)

These are policy decisions recorded now, implemented later:

| Control | Default | Where enforced |
| --- | --- | --- |
| Max cost per user per day | **$2.00** | `backend/app/credits/policy.py` |
| Max cost per user per month | **$30.00** | same |
| Max total cost per day (global) | **$200.00** | same + `settings.py` |
| Global generation kill switch | **off** | env var `GENERATION_ENABLED=true/false` — instant shutdown if provider bill spikes |
| Provider-specific kill switch | **off** | env var `ELEVENLABS_ENABLED`, `FAL_ENABLED` — disable one provider without full stop |
| Failed generation refund | **100%** | `credits/refund.py` — idempotent |

---

## 5. Pricing policy for users (Phase 0 placeholder, decided in Phase 9)

Malele will have a **credit system**, not direct provider pass-through. The user pays for credits; credits convert to generation cost with markup for infrastructure + margin.

Placeholder rules (to be approved in Phase 9, not now):
- **Free tier:** 5 songs / month, 2 variations each, no artwork, watermark on download.
- **Creator tier (TBD):** 100 songs / month, unlimited variations, artwork, no watermark, commercial license.
- **Premium tier (TBD):** 500 songs / month, priority queue, extended features, distribution rights.

Prices are **not** decided at Phase 0. All figures above describe *cost*, not *price*.

---

## 6. Open questions for COST_MODEL

1. **ElevenLabs music price** — must confirm from account. Blocks the "premium path" numbers above.
2. **fal.ai MiniMax 2.6 per-second price** — must confirm per-model on fal dashboard.
3. **Anthropic model choice** — pin one (Haiku for cost, Sonnet for quality) at Phase 2.
4. **Retention policy** — 30-day default for undownloaded songs; user-deleted songs purged immediately. Confirm in `SECURITY.md`.
5. **CDN** — Cloudflare vs. S3+CloudFront vs. Bunq. Decided Phase 9.

These are tracked in `KNOWN_GAPS.md` (sub-step 0.3).

---

**End of COST_MODEL.md (Phase 0).**

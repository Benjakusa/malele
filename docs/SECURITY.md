# Security

**Status:** Phase 0. Live document — updated at the end of every phase.
**Last updated:** Phase 0.

Threat model, security controls, and compliance posture. This document is a **contract** — code that weakens a control here must update this doc first.

---

## 1. Threat model

**Assets:**
- User accounts — email, password hashes, sessions
- User-generated prompts and lyrics — private, potentially sensitive
- Generated songs and uploaded vocals — copyright and privacy
- Credit balances — financial
- Provider API keys — ElevenLabs, fal, Anthropic, OpenAI
- Backend infrastructure — compute, storage, DB

**Adversaries:**
- Anonymous internet scanners and bots
- Registered users attempting privilege escalation or resource abuse
- Users attempting to reproduce copyrighted content or impersonate artists
- Compromised accounts from credential stuffing or phishing
- Provider-side compromise — respond by rotating keys plus fallback
- Insider threat from our own team — mitigated by least-privilege plus audit logs

**Assets vs. threats — top 8:**

| Threat | Vector | Control |
| --- | --- | --- |
| Provider key leak | Compromised backend or CI leak | Keys only in env vars; no keys in code, logs, or error messages; rotate quarterly; separate keys per environment |
| User data leak | SQL injection, IDOR, misconfigured storage | Parameterized queries; every DB access scoped by user_id; private S3 buckets plus signed URLs only |
| Credit fraud | Race conditions, replay, duplicate submit | Idempotency keys; SERIALIZABLE credit transactions; per-user daily caps; rate limits |
| Copyright violation | User prompts or AI lyrics reproducing existing songs | Lyric copyright validator as in ADR-008; reject before submit; log rejections |
| Artist impersonation | Prompt asking for voice of X | System prompt filter plus output classifier; explicit product rule forbids voice cloning; provider TOS backstop |
| Account takeover | Credential stuffing, token theft | Argon2 password hashing; 15-minute access tokens; refresh rotation; device-session list; optional 2FA post-MVP |
| Denial of service | Request flood, large-file upload | Rate limits per-IP and per-user; upload size cap 100 MB; 30-second request timeout; Cloudflare in prod |
| Prompt injection via lyrics or prompt | User controls provider prompt directly | Backend always wraps user input in a system prompt template; never pass raw user text as instructions to an LLM; output JSON schema validated |

---

## 2. Authentication

- **Password hashing:** Argon2id. Not bcrypt, not PBKDF2. Parameters tuned to ~250 ms per hash on production hardware; recorded in `backend/app/auth/password.py`.
- **Access tokens:** JWT, HS256 symmetric algorithm, 15 min TTL, `sub` = user id, `jti` = token id for revocation.
- **Refresh tokens:** opaque random 32-byte hex string, 30-day TTL, stored **hashed** in `sessions` table, rotated on every refresh.
- **Session storage on Android:** tokens in `EncryptedSharedPreferences` backed by Android Keystore. Never in plain SharedPreferences, never in logs.
- **Logout:** server-side session invalidation. Access token revocation via `jti` blacklist in Redis, TTL = token remaining life.
- **Password reset:** one-time token, 15 min TTL, single use, sent via email. SMTP stub in Phase 1, real in Phase 9.
- **Email verification:** optional at registration, required before publishing in Phase 8.
- **OAuth Google:** deferred. Optional in Phase 9. If added, must use PKCE flow and audit the library.
- **2FA:** not in MVP. Tracked in KNOWN_GAPS.
- **Account lockout:** after 10 failed logins per IP per 15 min, block for 15 min. Not per-account to avoid lockout DoS.

---

## 3. Authorization

- **Every endpoint** declares its required role: `guest`, `user`, `creator`, `admin`, `superadmin`.
- **Every resource access** filters by `user_id` — enforced by `scoped_to(user_id)` helper in the repository layer.
- **Cross-user isolation test** in Phase 1: user A cannot GET, PATCH, or DELETE user B songs or jobs.
- **Admin actions** — moderation queue, user suspension — require `admin` role and write to `audit_log`.
- **Superadmin** exists only for break-glass scenarios: kill switch, emergency key rotation. 2FA mandatory when implemented.

---

## 4. Data protection

- **At rest:** Postgres encryption at rest — RDS/Supabase default. S3 default encryption with SSE-S3.
- **In transit:** TLS 1.3 minimum. HSTS with 1-year max-age. No HTTP fallback in production.
- **Backups:** daily Postgres snapshots, retained 30 days. S3 versioning enabled. Restore tested monthly.
- **PII:** only email, hashed password, optional display name. No address, no phone, no payment info in MVP.
- **Deletion:** account deletion purges audio and artwork from S3 within 1 hour, DB rows within 24 hours after audit log writes. Confirmed by user via email link.
- **Logs:** request IDs, user ID, timings. Never tokens, provider keys, passwords, or full request bodies. Structured JSON.

---

## 5. Compliance posture

- **GDPR:** target compliance for EU users. Data export endpoint in Phase 9.
- **Kenya DPA:** target compliance. Data residency in EU region initially; note in KNOWN_GAPS.
- **COPPA / minors:** product is 18+; age gate on registration.
- **Provider TOS:** each provider adapter documents the terms in `API_INTEGRATIONS.md`. Non-compliance blocks deployment.

---

## 6. Incident response

**Sev 1 — provider key leaked:**
1. Rotate key in provider dashboard immediately.
2. Update env var, restart backend.
3. Check provider usage dashboard for suspicious calls since last rotation.
4. Write post-mortem in `SECURITY.md §7`.

**Sev 1 — user data breach:**
1. Isolate affected system.
2. Notify affected users within 72 hours per GDPR.
3. Notify Kenya ODPC if applicable.
4. Full post-mortem, published to users.

**Sev 2 — credit anomaly:**
1. Enable global generation kill switch.
2. Reconcile ledger, identify affected accounts.
3. Manually correct via adjustment transactions. Never edit historical rows.

---

## 7. Post-mortem log

Empty at Phase 0. Each entry: date, severity, summary, root cause, fix, follow-up gaps added to KNOWN_GAPS.

---

**End of SECURITY.md (Phase 0).**

# African Genre Tests

**Status:** NOT RUN.

**Date:** Phase 2 completion (not executed).

**Reason:** Live provider tests deferred per Phase 2 decision. Contract tests pass, but no real song has been generated through any provider.

---

## What this document would contain

When the African genre tests are run (via `scripts/african_genre_test.py` with `LIVE_PROVIDER_TESTS=1`), this file is regenerated with:

| Genre | Status | Size | File | Notes |
| --- | --- | --- | --- | --- |
| Benga | — | — | — | — |
| Rhumba | — | — | — | — |
| Soukous | — | — | — | — |
| Gospel | — | — | — | — |
| Afro-dance | — | — | — | — |
| Reggae | — | — | — | — |
| Slow African | — | — | — | — |
| Amapiano | — | — | — | — |


**Rating criteria** (per genre, 1-5):
- Instrumentation accuracy
- Rhythm accuracy
- Cultural authenticity
- Production quality

**Verdict:** go / no-go for Malele product identity.

---

## Why this matters

From `PLAN.md` section 5: "African genre test results documented with honest quality notes. This is the go/no-go for the product identity."

**Without this test, we cannot claim Malele generates authentic African music.** The product thesis depends on it.

**Where it fits now:** Phase 3 (Android foundation) does not require this test. **Phase 4 (Create experience) does** - users will press Generate and expect a Benga song, not a generic pop track.

**Action required:** run scripts/african_genre_test.py with LIVE_PROVIDER_TESTS=1 before Phase 4 begins.

---

## How to run

    cd ~/malele/backend
    source ~/.venvs/malele/bin/activate
    export LIVE_PROVIDER_TESTS=1
    export ELEVENLABS_ENABLED=true
    python ../scripts/african_genre_test.py

**Cost:** ~$0.80 (8 x 30-second instrumentals)

**Prerequisites:**
- ElevenLabs balance greater than $1.00
- ELEVENLABS_API_KEY in .env

**After running:**
1. Listen to each file in docs/african_tests/
2. Fill in the Notes column of this document
3. Rate each genre 1-5 on the four criteria
4. Write a verdict: go / no-go / go-with-caveats

---

**End of AFRICAN_GENRE_TESTS.md (Phase 2 placeholder).**

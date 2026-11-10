"""African genre fidelity test harness.

Usage:
    LIVE_PROVIDER_TESTS=1 python scripts/african_genre_test.py

Generates one short instrumental test song per African genre via ElevenLabs
and writes listening notes to docs/AFRICAN_GENRE_TESTS.md.

COSTS MONEY. Only run when LIVE_PROVIDER_TESTS=1.
"""
import asyncio
import os
import sys
from datetime import datetime
from pathlib import Path

from app.providers.base import GenerationRequest
from app.providers.music.elevenlabs import ElevenLabsMusicProvider


GENRES = [
    {"name": "Benga", "prompt": "Fast Kenyan Benga music with interlocking electric guitars, driving bass, steady kick, continuous hi-hat, and lively percussion. Instrumental only."},
    {"name": "Rhumba", "prompt": "Congolese Rhumba with smooth guitar arpeggios, flowing bass, clave-based rhythm, and warm percussion. Instrumental only."},
    {"name": "Soukous", "prompt": "Fast-paced Soukous with rapid interlocking guitar lines, syncopated drums, and energetic bass. Instrumental only."},
    {"name": "Gospel", "prompt": "Uplifting African Gospel instrumental with organ, piano, warm bass, gentle kick, tambourine, and choir pad. Instrumental only."},
    {"name": "Afro-dance", "prompt": "Upbeat Afro-dance track with syncopated 3-3-2 kick, busy shakers and hats, congas, and bouncy bass. Instrumental only."},
    {"name": "Reggae", "prompt": "Roots Reggae instrumental with one-drop drum pattern, offbeat guitar skank, walking bass, and dub-style space. Instrumental only."},
    {"name": "Slow African", "prompt": "Slow African ballad with soft kick, light shaker, sparse percussion, warm pads, and gentle bass. Instrumental only."},
    {"name": "Amapiano", "prompt": "South African Amapiano with log drum bass, airy pads, shakers, and laid-back percussion. Instrumental only."},
]

OUTPUT = Path("docs/AFRICAN_GENRE_TESTS.md")


async def run() -> None:
    if os.environ.get("LIVE_PROVIDER_TESTS") != "1":
        print("Refusing to run: LIVE_PROVIDER_TESTS is not set to 1")
        print("This test COSTS MONEY (~$0.80). Set the env var when ready.")
        sys.exit(1)

    provider = ElevenLabsMusicProvider()
    started_at = datetime.now().isoformat()
    results = []

    for entry in GENRES:
        print(f"Generating: {entry['name']}...")
        req = GenerationRequest(
            prompt=entry["prompt"],
            genre=entry["name"].lower(),
            vocals=False,
            instrumental=True,
            duration_seconds=30,
        )
        try:
            ref = await provider.submit(req)
            status = await provider.poll(ref)
            if status.state.value != "completed":
                results.append({"name": entry["name"], "status": "FAILED", "detail": status.error})
                continue
            audio = await provider.fetch(ref)
            out_path = Path(f"docs/african_tests/{entry['name'].lower().replace(' ', '_')}.mp3")
            out_path.parent.mkdir(exist_ok=True, parents=True)
            out_path.write_bytes(audio.audio_bytes)
            results.append({"name": entry["name"], "status": "OK", "bytes": len(audio.audio_bytes), "file": str(out_path)})
        except Exception as e:
            results.append({"name": entry["name"], "status": "ERROR", "detail": str(e)[:200]})

    lines = [f"# African Genre Tests — {started_at}", "", "Listening notes to be filled by reviewer.", "", "## Results", ""]
    lines.append("| Genre | Status | Size | File | Notes |")
    lines.append("| --- | --- | --- | --- | --- |")
    for r in results:
        notes = "" if r["status"] != "OK" else "_fill in after listening_"
        lines.append(f"| {r['name']} | {r['status']} | {r.get('bytes', '-')} | {r.get('file', '-')} | {notes} |")
    lines.append("")
    lines.append("## Rating criteria")
    lines.append("")
    lines.append("For each genre, listen and rate 1-5 on:")
    lines.append("- **Instrumentation accuracy** — does it use the right instruments?")
    lines.append("- **Rhythm accuracy** — does the pattern match the genre?")
    lines.append("- **Cultural authenticity** — does it feel like real African music, not a caricature?")
    lines.append("- **Production quality** — is the mix clean?")
    lines.append("")
    lines.append("**Verdict:** go / no-go for Malele product identity.")

    OUTPUT.write_text("\n".join(lines))
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    asyncio.run(run())

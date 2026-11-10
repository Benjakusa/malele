import io
import math
import struct
import wave
from functools import lru_cache


@lru_cache(maxsize=8)
def generate_demo_wav(duration_seconds: int = 5, sample_rate: int = 44100) -> bytes:
    """Generate a short demo WAV: sine-wave chord with fade in/out.

    Deterministic (same duration -> same bytes). Synthesized from scratch,
    so zero licensing concern. Not musical — just a valid WAV for pipeline tests.
    """
    duration_seconds = max(1, min(duration_seconds, 30))
    n_samples = int(sample_rate * duration_seconds)
    buf = io.BytesIO()
    with wave.open(buf, "wb") as wf:
        wf.setnchannels(2)
        wf.setsampwidth(2)
        wf.setframerate(sample_rate)
        frames = bytearray()
        for i in range(n_samples):
            t = i / sample_rate
            envelope = min(1.0, t * 2, (duration_seconds - t) * 2)
            # Simple major chord: A4 (440), C#5 (554.37), E5 (659.25)
            sample = 0.15 * envelope * (
                math.sin(2 * math.pi * 440.0 * t)
                + math.sin(2 * math.pi * 554.37 * t)
                + math.sin(2 * math.pi * 659.25 * t)
            )
            int_sample = int(max(-1.0, min(1.0, sample)) * 32767)
            packed = struct.pack("<h", int_sample)
            frames.extend(packed)
            frames.extend(packed)  # stereo duplicate
        wf.writeframes(bytes(frames))
    return buf.getvalue()

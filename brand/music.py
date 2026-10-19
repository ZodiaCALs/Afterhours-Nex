"""A quiet ambient bed for the promo: slow chords, soft bells at each scene, and a gentle reverb."""
import numpy as np, wave
SR, LENGTH = 48000, 48.0
n = int(SR * LENGTH)
out = np.zeros((n, 2))
hz = lambda m: 440.0 * 2 ** ((m - 69) / 12)
chords = [[50, 57, 61, 64, 66], [47, 54, 57, 61, 62], [43, 50, 54, 57, 59], [45, 52, 54, 59, 61]]   # Dmaj9, Bm9, Gmaj9, A6/9
step = 6.0
for i in range(int(LENGTH // step) + 1):
    start, dur = i * step - 1.0, step + 2.6
    s0, s1 = max(0, int(start * SR)), min(n, int((start + dur) * SR))
    if s1 <= s0: continue
    tt = np.arange(s0, s1) / SR - start
    env = np.clip(np.minimum(tt / 2.2, (dur - tt) / 2.6), 0, 1) ** 1.6
    for j, m in enumerate(chords[i % 4]):
        for det, pan in ((0.9985, 0.25), (1.0015, 0.75)):
            ph = 2 * np.pi * hz(m) * det * tt
            v = (np.sin(ph) + 0.16 * np.sin(2 * ph) + 0.05 * np.sin(3 * ph)) * env * (0.075 if j == 0 else 0.05)
            out[s0:s1, 0] += v * (1 - pan); out[s0:s1, 1] += v * pan
for when, m in [(0.5, 78), (2.3, 85), (7.0, 81), (12.6, 76), (19.2, 78), (25.8, 81), (33.2, 74), (38.6, 78), (43.4, 81), (44.6, 85)]:
    s0 = int(when * SR); tt = np.arange(n - s0) / SR
    env = np.exp(-tt / 1.8) * np.minimum(1, tt / 0.008)
    v = (np.sin(2 * np.pi * hz(m) * tt) + 0.25 * np.sin(2 * np.pi * hz(m) * 2.756 * tt) * np.exp(-tt / 0.4)) * env * 0.045
    out[s0:, 0] += v * 0.55; out[s0:, 1] += v * 0.45
spec = np.fft.rfft(out, axis=0)                                    # soften: a gentle low-pass above ~3 kHz
f = np.fft.rfftfreq(n, 1 / SR)
spec *= (1 / np.sqrt(1 + (f / 3200.0) ** 4))[:, None]
dry = np.fft.irfft(spec, n=n, axis=0)
rng = np.random.default_rng(3)
ir_len = int(3.2 * SR); tir = np.arange(ir_len) / SR
ir = rng.standard_normal((ir_len, 2)) * np.exp(-tir / 0.9)[:, None]
ir[: int(0.02 * SR)] = 0
size = 1 << int(np.ceil(np.log2(n + ir_len)))
wet = np.fft.irfft(np.fft.rfft(dry, size, axis=0) * np.fft.rfft(ir, size, axis=0), size, axis=0)[:n]
wet *= np.sqrt(np.mean(dry ** 2) / max(np.mean(wet ** 2), 1e-12))
mix = 0.72 * dry + 0.42 * wet
fade = np.ones(n); a, b = int(2.0 * SR), int(3.2 * SR)
fade[:a] = np.linspace(0, 1, a) ** 2; fade[-b:] = np.linspace(1, 0, b) ** 2
mix *= fade[:, None]
mix *= 0.5 / np.max(np.abs(mix))                                    # peaks at about -6 dB
with wave.open("music.wav", "wb") as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((np.clip(mix, -1, 1) * 32767).astype("<i2").tobytes())
print("music.wav:", LENGTH, "s; RMS %.1f dBFS" % (20 * np.log10(np.sqrt(np.mean(mix ** 2)))))

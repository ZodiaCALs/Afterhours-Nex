"""The film's soundtrack: slow chords, bells at each scene, and the sounds of the x being written."""
import json, numpy as np, wave
T = json.load(open("timing.json"))
SR, LENGTH = 48000, T["END"]
n = int(SR * LENGTH); t_all = np.arange(n) / SR
out = np.zeros((n, 2))
hz = lambda m: 440.0 * 2 ** ((m - 69) / 12)
chords = [[50, 57, 61, 64, 66], [47, 54, 57, 61, 62], [43, 50, 54, 57, 59], [45, 52, 54, 59, 61]]
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
def bell(when, m, level=0.045, decay=1.8):
    s0 = int(when * SR); tt = np.arange(n - s0) / SR
    env = np.exp(-tt / decay) * np.minimum(1, tt / 0.008)
    v = (np.sin(2 * np.pi * hz(m) * tt) + 0.25 * np.sin(2 * np.pi * hz(m) * 2.756 * tt) * np.exp(-tt / 0.4)) * env * level
    out[s0:, 0] += v * 0.55; out[s0:, 1] += v * 0.45
for when, m in [(0.9, 78), (2.4, 85), (10.4, 81), (12.0, 78), (19.6, 76), (29.4, 78), (38.8, 81), (48.4, 74), (58.4, 78), (66.4, 81), (75.2, 78), (76.4, 85), (78.0, 81)]:
    bell(when, m)
spec = np.fft.rfft(out, axis=0); f = np.fft.rfftfreq(n, 1 / SR)
spec *= (1 / np.sqrt(1 + (f / 3200.0) ** 4))[:, None]
dry = np.fft.irfft(spec, n=n, axis=0)

rng = np.random.default_rng(9)
def onepole(x, a):                                       # a simple low-pass with a moving cutoff
    y = np.empty_like(x); acc = 0.0
    for i in range(len(x)): acc += a[i] * (x[i] - acc); y[i] = acc
    return y
def place(sig, when, pan=0.5):
    s0 = int(when * SR); e = min(n, s0 + len(sig))
    dry[s0:e, 0] += sig[: e - s0] * (1 - pan); dry[s0:e, 1] += sig[: e - s0] * pan
# gathering: a rising breath of air into the strike
g_len = T["T_BOLD"] - T["TX"] + 0.05; m = int(g_len * SR); u = np.arange(m) / m
place(onepole(rng.standard_normal(m), 0.01 + 0.25 * u ** 2) * (u ** 2.2) * 0.22, T["TX"], 0.4)
# the strokes: quick bright swishes
for when, length, level, pan in ((T["T_BOLD"], .34, .30, .35), (T["T_FINE"], .30, .24, .65)):
    m = int(length * SR); u = np.arange(m) / m
    noise = rng.standard_normal(m); noise = noise - onepole(noise, np.full(m, 0.08))   # keep the hiss, drop the rumble
    place(noise * np.sin(np.pi * np.clip(u * 1.4, 0, 1)) ** 2 * (1 - u) * level, when, pan)
# the impact: a deep boom with a falling pitch, a crack, and a shimmer of bells
m = int(1.6 * SR); u = np.arange(m) / SR
freq = 42 + 58 * np.exp(-u / 0.12); phase = 2 * np.pi * np.cumsum(freq) / SR
boom = np.sin(phase) * np.exp(-u / 0.42) * np.minimum(1, u / 0.004) * 0.62
crack = rng.standard_normal(m) * np.exp(-u / 0.018) * 0.32
place(boom + crack, T["T_IMPACT"], 0.5)
for k, mn in enumerate((90, 93, 97, 102)):
    s0 = int((T["T_IMPACT"] + 0.1 + k * 0.07) * SR); tt = np.arange(n - s0) / SR
    v = np.sin(2 * np.pi * hz(mn) * tt) * np.exp(-tt / 0.9) * np.minimum(1, tt / 0.004) * 0.03
    dry[s0:, 0] += v * (0.3 + 0.15 * k); dry[s0:, 1] += v * (0.7 - 0.15 * k)

ir_len = int(3.2 * SR); tir = np.arange(ir_len) / SR
ir = rng.standard_normal((ir_len, 2)) * np.exp(-tir / 0.9)[:, None]; ir[: int(0.02 * SR)] = 0
size = 1 << int(np.ceil(np.log2(n + ir_len)))
wet = np.fft.irfft(np.fft.rfft(dry, size, axis=0) * np.fft.rfft(ir, size, axis=0), size, axis=0)[:n]
wet *= np.sqrt(np.mean(dry ** 2) / max(np.mean(wet ** 2), 1e-12))
mix = 0.72 * dry + 0.42 * wet
fade = np.ones(n); a, b = int(1.6 * SR), int(3.0 * SR)
fade[:a] = np.linspace(0, 1, a) ** 2; fade[-b:] = np.linspace(1, 0, b) ** 2
mix *= fade[:, None]
mix *= 0.72 / np.max(np.abs(mix)); mix = np.tanh(1.25 * mix) / np.tanh(1.25)   # gentle limiting
with wave.open("music2.wav", "wb") as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((np.clip(mix * 0.89, -1, 1) * 32767).astype("<i2").tobytes())
music = mix[int(20 * SR):int(70 * SR)]
print(f"music2.wav: {LENGTH} s; RMS of the music {20 * np.log10(np.sqrt(np.mean(music ** 2))):.1f} dBFS; impact at {T['T_IMPACT']:.3f} s")

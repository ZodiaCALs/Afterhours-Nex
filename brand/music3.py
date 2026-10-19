"""The film's soundtrack: a 96 BPM late-night groove, arranged to the picture (37 bars)."""
import json, numpy as np, wave
T = json.load(open("timing3.json"))
SR, BPM, LENGTH = 48000, T["BPM"], T["END"]
BEAT = 60 / BPM; BAR = 4 * BEAT
def B(bar, beat=0.0): return (bar - 1) * BAR + beat * BEAT
n = int(SR * LENGTH)
music = np.zeros((n, 2)); drums = np.zeros((n, 2))
rng = np.random.default_rng(12)
hz = lambda m: 440.0 * 2 ** ((m - 69) / 12)
CH = [[50, 57, 61, 64, 66], [47, 54, 57, 61, 62], [43, 50, 54, 57, 59], [45, 52, 54, 59, 61]]   # Dmaj9, Bm9, Gmaj9, A6/9
ROOT = [38, 35, 31, 33]
def chord_at(bar): return ((bar - 1) // 2) % 4
def add(bus, sig, when, pan=0.5, gain=1.0):
    s0 = int(when * SR)
    if s0 < 0: sig, s0 = sig[-s0:], 0          # a sound that starts before the film: keep the part after zero
    e = min(n, s0 + len(sig))
    if s0 >= n or e <= s0: return
    bus[s0:e, 0] += sig[: e - s0] * gain * (1 - pan); bus[s0:e, 1] += sig[: e - s0] * gain * pan
def env(length, attack, decay):
    t = np.arange(int(length * SR)) / SR
    return t, np.minimum(1, t / max(attack, 1e-4)) * np.exp(-t / decay)
# pads, one chord every two bars
for bar in range(1, 38, 2):
    start, dur = B(bar) - .6, 2 * BAR + 1.8
    t = np.arange(int(dur * SR)) / SR
    e = np.clip(np.minimum(t / 1.1, (dur - t) / 1.6), 0, 1) ** 1.5
    for j, m in enumerate(CH[chord_at(bar)]):
        for det, pan in ((0.9985, .25), (1.0015, .75)):
            ph = 2 * np.pi * hz(m) * det * t
            add(music, (np.sin(ph) + .15 * np.sin(2 * ph) + .05 * np.sin(3 * ph)) * e * (.06 if j == 0 else .04), start, pan)
def bell(when, m, level=.05, decay=1.6):
    t, e = env(decay * 4, .006, decay)
    add(music, (np.sin(2 * np.pi * hz(m) * t) + .25 * np.sin(2 * np.pi * hz(m) * 2.756 * t) * np.exp(-t / .4)) * e * level, when, .55)
def pluck(when, m, level=.05, pan=.5):
    t, e = env(1.2, .003, .28)
    add(music, (np.sin(2 * np.pi * hz(m) * t) + .3 * np.sin(4 * np.pi * hz(m) * t) * np.exp(-t / .08)) * e * level, when, pan)
for when, m in [(T["BOLD0"], 81), (T["FINE0"], 85), (T["FINE1"], 88)]: bell(when, m, .045)
for k, when in enumerate([B(5, 0), B(5, 1), B(5, 2), *[B(6, i * .5) for i in range(6)], B(7, 0), B(7, 1), B(7, 2), B(7, 3)]):
    pluck(when, [74, 78, 81, 76, 78, 81, 83, 81, 78, 74, 76, 78, 81][k % 13], .045, .35 + .3 * (k % 2))
for bar in (9, 13, 15, 17, 19, 22, 24, 26, 28, 30): bell(B(bar), [81, 78, 83, 81, 85, 81, 78, 83, 81, 86][[9, 13, 15, 17, 19, 22, 24, 26, 28, 30].index(bar)], .04)
# drums
def kick(level):
    t, e = env(.45, .001, .22)
    f = 48 + 70 * np.exp(-t / .035)
    return (np.sin(2 * np.pi * np.cumsum(f) / SR) * e + rng.standard_normal(len(t)) * np.exp(-t / .004) * .15) * level
def rim(level):
    t, e = env(.25, .001, .07)
    noise = rng.standard_normal(len(t)); noise = noise - np.concatenate([[0], noise[:-1]]) * .9
    return (noise * .5 * np.exp(-t / .05) + np.sin(2 * np.pi * 195 * t) * np.exp(-t / .045)) * level
def hat(level, open_=False):
    t, e = env(.3 if open_ else .08, .0005, .09 if open_ else .022)
    noise = rng.standard_normal(len(t)); noise = np.diff(np.diff(noise, prepend=0), prepend=0)
    return noise * e * level
for bar in range(5, 37):
    groove = 9 <= bar <= 31
    if 5 <= bar <= 8:
        add(drums, kick(.35), B(bar, 0)); add(drums, kick(.22), B(bar, 2))
    if groove:
        add(drums, kick(.55), B(bar, 0)); add(drums, kick(.5), B(bar, 2))
        if bar % 2 == 0: add(drums, kick(.32), B(bar, 2.5))
        add(drums, rim(.32), B(bar, 1), .45); add(drums, rim(.32), B(bar, 3), .45)
        for i in range(8):
            swing = .06 * BEAT if i % 2 else 0
            add(drums, hat(.11 if i % 2 else .16), B(bar, i * .5) + swing, .62)
        if bar % 2 == 1: add(drums, hat(.09, True), B(bar, 3.5) + .06 * BEAT, .65)
        if bar == 31: add(drums, rim(.24), B(bar, 3.5), .5); add(drums, rim(.2), B(bar, 3.75), .5)
        r = ROOT[chord_at(bar)]
        for beat, step, length in ((0, 0, .9), (1.5, 0, .4), (2, 7, .9), (3.5, 12, .4)):
            t = np.arange(int(length * BEAT * SR)) / SR
            e = np.minimum(1, t / .006) * np.exp(-t / .45) * np.clip((length * BEAT - t) / .05, 0, 1)
            ph = 2 * np.pi * hz(r + step) * t
            add(drums, (np.sin(ph) + .25 * np.sin(2 * ph)) * e * .3, B(bar, beat))
        if bar >= 17:
            tones = [m + 12 for m in CH[chord_at(bar)]]
            for i in range(8): pluck(B(bar, i * .5) + (.06 * BEAT if i % 2 else 0), tones[(i * 2 + bar) % len(tones)], .022, .3 + .4 * (i % 2))
    if bar in (34, 35, 36): add(drums, kick(.42), B(bar, 0))
# breakdown riser and the finale's hit
rl = B(34) - B(32); m = int(rl * SR); u = np.arange(m) / m
noise = rng.standard_normal(m); y = np.empty(m); acc = 0.0
coef = .01 + .3 * u ** 2
for i in range(m): acc += coef[i] * (noise[i] - acc); y[i] = acc
add(music, y * u ** 2.4 * .3, B(32), .5)
t, e = env(2.5, .002, .5); f = 40 + 50 * np.exp(-t / .1)
add(drums, np.sin(2 * np.pi * np.cumsum(f) / SR) * e * .6, B(34))
t, e = env(3.0, .002, .9); add(music, rng.standard_normal(len(t)) * e * .05, B(34), .5)
bell(B(34), 81, .06, 2.2); bell(B(34, .5), 85, .045, 2.0)
# finish: soften the pads, add reverb to the music, and a small room to the drums
spec = np.fft.rfft(music, axis=0); f = np.fft.rfftfreq(n, 1 / SR); spec *= (1 / np.sqrt(1 + (f / 3600.0) ** 4))[:, None]
music = np.fft.irfft(spec, n=n, axis=0)
def reverb(x, seconds, decay, mix):
    L = int(seconds * SR); tir = np.arange(L) / SR
    ir = rng.standard_normal((L, 2)) * np.exp(-tir / decay)[:, None]; ir[: int(.015 * SR)] = 0
    size = 1 << int(np.ceil(np.log2(n + L)))
    wet = np.fft.irfft(np.fft.rfft(x, size, axis=0) * np.fft.rfft(ir, size, axis=0), size, axis=0)[:n]
    wet *= np.sqrt(np.mean(x ** 2) / max(np.mean(wet ** 2), 1e-12))
    return x + mix * wet
mix = .8 * reverb(music, 3.2, .9, .55) + reverb(drums, .5, .12, .18)
fade = np.ones(n); fa, fb = int(1.2 * SR), int(2.4 * SR)
fade[:fa] = np.linspace(0, 1, fa) ** 2; fade[-fb:] = np.linspace(1, 0, fb) ** 2
mix *= fade[:, None]
mix *= .8 / np.max(np.abs(mix)); mix = np.tanh(1.3 * mix) / np.tanh(1.3)
with wave.open("music3.wav", "wb") as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((np.clip(mix * .9, -1, 1) * 32767).astype("<i2").tobytes())
groove = mix[int(B(10) * SR):int(B(30) * SR)]
print(f"music3.wav: {LENGTH} s at {BPM} BPM; groove RMS {20 * np.log10(np.sqrt(np.mean(groove ** 2))):.1f} dBFS")

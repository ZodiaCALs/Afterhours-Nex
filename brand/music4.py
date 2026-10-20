"""The 1.2 film's soundtrack: a warm 90 BPM groove in F, arranged to the chapters (27 bars, 72 s)."""
import json, numpy as np, wave
T = json.load(open("timing4.json")); SR, BPM, LENGTH = 48000, T["BPM"], T["END"]
BEAT = 60 / BPM; BAR = 4 * BEAT
def B(bar, beat=0.0): return (bar - 1) * BAR + beat * BEAT
n = int(SR * LENGTH); keys = np.zeros((n, 2)); drums = np.zeros((n, 2)); rng = np.random.default_rng(7)
hz = lambda m: 440.0 * 2 ** ((m - 69) / 12)
CH = [[53, 57, 60, 64, 67], [52, 55, 59, 62, 66], [50, 53, 57, 60, 64], [46, 50, 53, 57, 62]]   # Fmaj9, Em7, Dm9, B♭maj7 (voiced)
ROOT = [41, 40, 38, 34]
CHAPTERS = [4, 7, 9, 12, 15, 19, 22, 24]
def add(bus, sig, when, pan=.5, gain=1.0):
    s0 = int(when * SR)
    if s0 < 0: sig, s0 = sig[-s0:], 0
    e = min(n, s0 + len(sig))
    if e > s0: bus[s0:e, 0] += sig[:e - s0] * gain * (1 - pan); bus[s0:e, 1] += sig[:e - s0] * gain * pan
def env(length, attack, decay):
    t = np.arange(int(length * SR)) / SR
    return t, np.minimum(1, t / max(attack, 1e-4)) * np.exp(-t / decay)
def rhodes(when, m, level, length=2.4):
    t, e = env(length, .004, .9)
    f = hz(m); trem = 1 + .06 * np.sin(2 * np.pi * 4.2 * t)
    sig = (np.sin(2 * np.pi * f * t + .6 * np.sin(2 * np.pi * f * t) * np.exp(-t / .25)) + .25 * np.sin(4 * np.pi * f * t) * np.exp(-t / .3)) * e * trem
    add(keys, sig * level, when, .35 + .3 * ((m % 5) / 5))
def bell(when, m, level=.05):
    t, e = env(4.0, .003, 1.3)
    add(keys, (np.sin(2 * np.pi * hz(m) * t) + .3 * np.sin(2 * np.pi * hz(m) * 2.76 * t) * np.exp(-t / .5)) * e * level, when, .6)
def pad(bar0, bars, ch, level):
    start, dur = B(bar0) - .4, bars * BAR + 1.6
    t = np.arange(int(dur * SR)) / SR
    e = np.clip(np.minimum(t / 1.4, (dur - t) / 1.6), 0, 1) ** 1.6
    for j, m in enumerate(ch):
        for det, pan in ((.9984, .25), (1.0016, .75)):
            ph = 2 * np.pi * hz(m + 12 * (j == 0) * 0) * det * t
            add(keys, (np.sin(ph) + .12 * np.sin(2 * ph)) * e * level, start, pan)
def kick(level):
    t, e = env(.5, .001, .24); f = 46 + 64 * np.exp(-t / .03)
    return (np.sin(2 * np.pi * np.cumsum(f) / SR) * e + rng.standard_normal(len(t)) * np.exp(-t / .003) * .1) * level
def brush(level, length=.18):
    t, e = env(length, .006, length / 3); noise = rng.standard_normal(len(t)); noise = noise - .85 * np.concatenate([[0], noise[:-1]])
    return noise * e * level
def rim(level):
    t, e = env(.2, .001, .05)
    return (np.sin(2 * np.pi * 330 * t) * np.exp(-t / .03) + rng.standard_normal(len(t)) * .3 * np.exp(-t / .02)) * level
for bar in range(1, 28):
    ci = (bar - 1) % 4; ch = CH[ci]
    if bar % 2 == 1: pad(bar, 2, CH[ci], .028 if bar < 24 else .04)
    if bar <= 3:                                              # the opening: chords bloom, nothing else
        for j, m in enumerate(ch): rhodes(B(bar) + j * .06, m + 12, .05)
        continue
    groove = 4 <= bar <= 21 or 24 <= bar <= 25
    for beat, hit in ((0, 1.0), (1.5, .55), (2.5, .7), (3.5, .45)):   # comping, with a lilt
        if beat == 0 or groove:
            for j, m in enumerate(ch[1:]): rhodes(B(bar, beat) + j * .015, m + 12, .022 * hit, 1.6)
    if groove:
        add(drums, kick(.5), B(bar, 0)); add(drums, kick(.38), B(bar, 2.5))
        if bar % 2 == 0: add(drums, kick(.26), B(bar, 3.75))
        add(drums, rim(.2), B(bar, 1), .45); add(drums, rim(.2), B(bar, 3), .45)
        for i in range(8):
            swing = .07 * BEAT if i % 2 else 0
            add(drums, brush(.09 if i % 2 else .13), B(bar, i * .5) + swing, .62)
        r = ROOT[ci]
        for beat, step, length in ((0, 0, 1.4), (1.5, 7, .45), (2.5, 0, .9), (3.5, 12, .4)):
            t = np.arange(int(length * BEAT * SR)) / SR
            e = np.minimum(1, t / .008) * np.exp(-t / .6) * np.clip((length * BEAT - t) / .05, 0, 1)
            add(drums, (np.sin(2 * np.pi * hz(r + step) * t) + .2 * np.sin(4 * np.pi * hz(r + step) * t)) * e * .32, B(bar, beat))
    elif 22 <= bar <= 23:                                     # the breakdown: just rim and air
        add(drums, rim(.14), B(bar, 1), .45); add(drums, rim(.14), B(bar, 3), .45)
for k, bar in enumerate(CHAPTERS): bell(B(bar), [77, 81, 84, 79, 81, 84, 86, 89][k], .045)
# a soft rise into the finale, and its bloom
m = int(2 * BAR * SR); u = np.arange(m) / m; y = np.zeros(m); acc = 0.0; noise = rng.standard_normal(m); coef = .01 + .25 * u ** 2
for i in range(m): acc += coef[i] * (noise[i] - acc); y[i] = acc
add(keys, y * u ** 2.2 * .22, B(22))
for j, mm in enumerate([53, 60, 64, 67, 72, 76]): bell(B(24) + j * .05, mm + 12, .03)
# finish: soften, reverb the keys, a small room on the drums, fade and level
spec = np.fft.rfft(keys, axis=0); f = np.fft.rfftfreq(n, 1 / SR); spec *= (1 / np.sqrt(1 + (f / 4200.0) ** 4))[:, None]; keys = np.fft.irfft(spec, n=n, axis=0)
def reverb(x, seconds, decay, mix):
    L = int(seconds * SR); tir = np.arange(L) / SR
    ir = rng.standard_normal((L, 2)) * np.exp(-tir / decay)[:, None]; ir[:int(.012 * SR)] = 0
    size = 1 << int(np.ceil(np.log2(n + L)))
    wet = np.fft.irfft(np.fft.rfft(x, size, axis=0) * np.fft.rfft(ir, size, axis=0), size, axis=0)[:n]
    wet *= np.sqrt(np.mean(x ** 2) / max(np.mean(wet ** 2), 1e-12)); return x + mix * wet
mix = .85 * reverb(keys, 3.0, .85, .5) + reverb(drums, .45, .1, .15)
fade = np.ones(n); fa, fb = int(1.0 * SR), int(2.6 * SR); fade[:fa] = np.linspace(0, 1, fa) ** 2; fade[-fb:] = np.linspace(1, 0, fb) ** 2
mix *= fade[:, None]; mix *= .8 / np.max(np.abs(mix)); mix = np.tanh(1.25 * mix) / np.tanh(1.25)
with wave.open("music4.wav", "wb") as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes((np.clip(mix * .9, -1, 1) * 32767).astype("<i2").tobytes())
print(f"music4.wav: {LENGTH:.1f} s at {BPM} BPM")

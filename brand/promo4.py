"""AfterhoursNex 1.2 film: chapters on a 90 BPM groove (27 bars, 72 s, 60 fps). A new layout and rhythm."""
import json
import promo2 as P2                       # the wordmark's letter shapes and the icon
BPM = 90; BEAT = 60 / BPM; BAR = 4 * BEAT
def B(bar, beat=0.0): return (bar - 1) * BAR + beat * BEAT
END = B(28)
EO = "cubic-bezier(.1,.9,.2,1)"; EI = "cubic-bezier(.9,.1,1,.2)"; ES = "cubic-bezier(.33,0,.67,1)"
def a(name, dur, delay, ease=EO, iters="1"): return f"animation:{name} {dur:.3f}s {ease} {delay:.3f}s {iters} both;"
def scene(start, end, body, cls=""): return f'<section class="sc {cls}" style="{a("sc", end - start + .35, start, "linear")}">{body}</section>'
def rise(t, d=.9): return a("rise", d, t)
def chapter(num, kick, head, body, t, top=250):
    return (f'<div class="ch" style="top:{top}px"><div class="num serif" style="{a("numin", 1.4, t)}">{num}</div>'
            f'<div class="kick" style="{rise(t + .1)}">{kick}</div><h2 class="serif" style="{a("unveil", 1.1, t + BEAT * .5)}">{head}</h2>'
            f'<p class="body" style="{rise(t + BEAT * 1.2)}">{body}</p></div>')

# ---- the wordmark, from the brand's letter shapes ----
def wordmark(scale, cx, base, gid):
    F, G = P2.F * scale, P2.G
    x0 = cx - (G["after"]["width"] + 6 + G["nex"]["width"]) * F / 2; nx = x0 + (G["after"]["width"] + 6) * F
    after = "".join(f'<path d="{P2.gd("after", g, x0, base, F)}"/>' for g in G["after"]["glyphs"])
    nex = "".join(f'<path d="{P2.gd("nex", g, nx, base, F)}"/>' for g in G["nex"]["glyphs"])
    return (f'<defs><linearGradient id="{gid}" x1="{nx:.0f}" y1="{base - 140 * scale:.0f}" x2="{nx + G["nex"]["width"] * F:.0f}" y2="{base:.0f}" gradientUnits="userSpaceOnUse">'
            f'<stop offset="0" stop-color="#FEBB5F"/><stop offset=".35" stop-color="#F7926D"/><stop offset=".62" stop-color="#F37377"/><stop offset="1" stop-color="#B6448D"/></linearGradient></defs>'
            f'<g fill="#F4F4F7">{after}</g><g fill="url(#{gid})">{nex}</g>')

blobs = ('<div class="blob b1"></div><div class="blob b2"></div><div class="blob b3"></div>')

# ---- 0. opening ----
s0 = scene(0, B(4), f'''<div class="hair" style="{a("draw", 1.8, B(1, .5))[:-1]}, out .6s {EI} {B(2) - .2:.3f}s 1 forwards;"></div>
  <div class="pre" style="{rise(B(1, 2))[:-1]}, out .6s {EI} {B(2) - .2:.3f}s 1 forwards;">A podcast player for the late hours</div>
  <img class="op-icon" src="shots/icon.png" style="{a("pop", 1.0, B(2))}">
  <svg class="op-word" width="1920" height="1080" viewBox="0 0 1920 1080" style="{rise(B(2, 1), 1.2)}">{wordmark(0.9, 960, 610, "g0")}</svg>
  <div class="v12 serif" style="{a("v12", 1.6, B(3))}">1.2</div>
  <div class="tag" style="{rise(B(3, 2))}">Lighter. Everywhere. Yours.</div>''')

# ---- 1. find ----
stations = [("Tokyo Night FM", "Tokyo · city pop", "#FF82AE,#B6448D"), ("Radio Lumière", "Paris · jazz", "#FEBB5F,#F37377"),
            ("Seoul After Dark", "Seoul · lo-fi", "#8E7CFF,#4B3FD1"), ("Harbour City Radio", "Hong Kong · talk", "#4FD1C5,#2C7A9B")]
radio = "".join(f'<div class="st" style="{rise(B(4, 1.5) + k * BEAT / 2, .7)}"><i style="background:linear-gradient(135deg,{c})"></i><div><b>{n}</b><span>{m}</span></div>'
                f'{"<em class=eq><u></u><u></u><u></u></em>" if k == 0 else "<em class=live>● LIVE</em>"}</div>' for k, (n, m, c) in enumerate(stations))
results = [("Edgerunners, ten years on", "Night City Notes · 48 min", "#FF82AE,#6A3FD1"), ("Why the soundtrack still hits", "Anime After Hours · 32 min", "#FEBB5F,#B6448D"),
           ("A love letter to the anime", "Second Screen · 55 min", "#4FD1C5,#4B3FD1")]
found = "".join(f'<div class="res" style="{rise(B(5, 1.5) + k * BEAT / 2, .7)}"><i style="background:linear-gradient(135deg,{c})"></i><div><b>{t}</b><span>{m}</span></div></div>'
                for k, (t, m, c) in enumerate(results))
s1 = scene(B(4), B(7), chapter("01", "Find", "Radio, topics<br>and previews", "Public stations from around the world. Single episodes about anything. And a look before you add.", B(4)) + f'''
  <div class="glass p-radio" style="{a("panel", 1.1, B(4, 1))}"><div class="pt">Radio <span>· Near you</span></div>{radio}</div>
  <div class="glass p-search" style="{a("panel", 1.1, B(4, 3))}"><div class="field"><u></u><b class="typed" style="{a("type", 1.5, B(5), "steps(28, end)")}">cyberpunk edgerunners review</b><s style="{a("blink", 1, B(4, 3), "steps(2, start)", "4")}"></s></div>
    <div class="chips"><span>Shows</span><span class="on">Episodes &amp; topics</span></div>{found}</div>
  <div class="glass p-prev" style="{a("panel", 1.1, B(6))}"><i class="cover" style="background:linear-gradient(135deg,#FF82AE,#6A3FD1)"></i><div><div class="k">Episode</div>
    <b>Edgerunners, ten years on</b><span>Night City Notes · 48 min</span><div class="pills"><em class="fill">▶ Play</em><em>Open show</em></div></div></div>''')

# ---- 2. hear anything ----
bars = "".join(f'<u style="animation:wave {BEAT:.3f}s {ES} {B(7, 2) + (k % 7) * .07:.3f}s infinite alternate both;height:{30 + (k * 37) % 90}px"></u>' for k in range(44))
files = "".join(f'<span class="file" style="{a("fly", .9, B(8) + k * BEAT / 2)};--dx:{-260 + k * 30}px">{f}</span>' for k, f in enumerate(["MP3", "OGG", "FLAC", "WAV", "M4A", "OPUS"]))
s2 = scene(B(7), B(9), chapter("02", "Hear anything", "Videos as audio.<br>Your own files, too.", "YouTube and Bilibili play as audio. Files from your computer gather in On this device.", B(7)) + f'''
  <div class="glass vid" style="{a("panel", 1.0, B(7, .5))}"><div class="screen" style="{a("tovoice", .9, B(7, 2))}"><b class="play">▶</b><i class="prog"></i></div>
    <div class="wave" style="{a("fadein", .6, B(7, 2.2))}">{bars}</div></div>
  <div class="srcs" style="{rise(B(7, 3))}"><span>YouTube</span><span>Bilibili</span></div>
  <div class="glass stack" style="{a("panel", 1.0, B(8))}"><div class="k">On this device</div><div class="files">{files}</div></div>''')

# ---- 3. read along: full-width rolling captions, translations arriving underneath ----
LY = [("The city never really sleeps.", "この街は本当には眠らない。"), ("It just turns the lights down low,", "ただ明かりを落とすだけ、"),
      ("and waits for someone to listen.", "そして誰かが耳を傾けるのを待っている。"), ("Tonight, that someone is you.", "今夜、その誰かはあなた。"), ("So let’s begin.", "さあ、始めよう。")]
T0, STEP, PITCH, LS, LL = B(9, .5), 2 * BEAT, 150, B(9), B(12) - B(9) + .35
def pct(t): return max(0.0, min(100.0, (t - LS) / LL * 100))
ly_css = "@keyframes roll { 0% { transform: translateY(0); }"
for k in range(1, len(LY)):
    c = T0 + k * STEP
    ly_css += f" {pct(c):.3f}% {{ transform: translateY({-(k - 1) * PITCH}px); animation-timing-function: {EO}; }} {pct(c + .7):.3f}% {{ transform: translateY({-k * PITCH}px); }}"
ly_css += f" 100% {{ transform: translateY({-(len(LY) - 1) * PITCH}px); }} }}\n"
for i in range(len(LY)):
    on, off = T0 + i * STEP, T0 + (i + 1) * STEP
    last = i == len(LY) - 1
    ly_css += (f"@keyframes l{i} {{ 0% {{ color: rgba(244,244,247,.28); transform: scale(.975); }} {pct(on):.3f}% {{ color: rgba(244,244,247,.28); transform: scale(.975); }}"
               f" {pct(on + .45):.3f}% {{ color: #fff; transform: none; }}" + ("" if last else f" {pct(off):.3f}% {{ color: #fff; transform: none; }} {pct(off + .45):.3f}% {{ color: rgba(244,244,247,.28); transform: scale(.975); }}")
               + f" 100% {{ color: {'#fff' if last else 'rgba(244,244,247,.28)'}; transform: {'none' if last else 'scale(.975)'}; }} }}\n")
lines = "".join(f'<div class="ly" style="animation:l{i} {LL:.3f}s linear {LS:.3f}s both"><div>{en}</div><div class="jp" style="{a("fadein", .8, T0 + i * STEP + BEAT * .6)}">{jp}</div></div>' for i, (en, jp) in enumerate(LY))
s3 = scene(B(9), B(12), f'''<div class="corner" style="{rise(B(9))}"><span class="kick">03 · Read along</span></div>
  <div class="lyrics"><div class="roll" style="animation:roll {LL:.3f}s linear {LS:.3f}s both">{lines}</div></div>
  <div class="note" style="{rise(B(10, 2))}">Captions roll like lyrics, with translations underneath.<br>The motion stays steady as they arrive.</div>''')

# ---- 4. feel: liquid glass ----
toggles = "".join(f'<div class="tg" style="{a("tgtrack", .7, B(12, 2) + k * BEAT * 2, ES)}"><i style="{a("tgknob", .7, B(12, 2) + k * BEAT * 2)}"></i></div>' for k in range(3))
s4 = scene(B(12), B(15), chapter("04", "Feel", "Liquid glass,<br>your way", "Switches made of glass. Transparency you set yourself. A calm dark mode that’s easy to read.", B(12)) + f'''
  <div class="art-card" style="{a("panel", 1.1, B(12, .5))}"><i class="c1"></i><i class="c2"></i><i class="c3"></i>
    <div class="glass feel" style="{a("seethrough", 2.6, B(14), ES)}"><div class="row">{toggles}</div>
      <div class="sl"><span>Transparency</span><div class="track"><b style="{a("slide", 2.6, B(14), ES)}"></b></div></div></div></div>''')

# ---- 5. everywhere ----
s5 = scene(B(15), B(19), f'''<div class="ch top"><div class="kick" style="{rise(B(15))}">05 · Everywhere</div>
    <h2 class="serif" style="{a("unveil", 1.1, B(15, .5))}">The same app,<br>on every screen</h2></div>
  <div class="win mac" style="{a("panel", 1.1, B(15, 1))}"><div class="lights"><i></i><i></i><i></i></div><img src="shots/d_library.png"></div>
  <div class="win pc" style="{a("panel", 1.1, B(16))}"><div class="caps"><i>—</i><i>□</i><i>✕</i></div><img src="shots/d_show.png"></div>
  <div class="phone4" style="{a("panel", 1.1, B(17))}"><img src="shots/p_np.png"></div>
  <svg class="link" width="1920" height="1080" viewBox="0 0 1920 1080"><path d="M 1660 380 C 1560 150, 1060 140, 860 290" style="{a("dash", 1.4, B(17, 1), ES)}"/></svg>
  <div class="linklabel" style="{rise(B(17, 2))}">Captions and videos from your computer</div>
  <div class="win web" style="{a("panel", 1.1, B(18))}"><div class="tab"><i>AfterhoursNex</i><span>127.0.0.1:8790</span></div><img src="shots/d_library.png"></div>
  <div class="plats">{"".join(f'<span style="animation:lit .5s ease-out {B(15 + k, 1):.3f}s both">{n}</span>' for k, n in enumerate(["macOS", "Windows · Linux", "Android", "Web"]))}</div>''')

# ---- 6. light ----
facts = [("One light player for every format", "built on FFmpeg and your system’s own sound"), ("Speech models load only while they work", "and let go of their memory when captions are done"),
         ("Smaller caches, and Reduce effects", "for the lightest graphics when you need it")]
fl = "".join(f'<div class="fact" style="{rise(B(20) + k * BEAT, .8)}"><i>✓</i><div><b>{h}</b><span>{d}</span></div></div>' for k, (h, d) in enumerate(facts))
s6 = scene(B(19), B(22), chapter("06", "Light", "Light on<br>memory", "Made to stay out of your way, on old computers and new ones.", B(19)) + f'''
  <div class="big"><span class="n serif" style="{a("count", 1.8, B(19, 1), "cubic-bezier(.2,.8,.2,1)")}"></span><span class="mb">MB</span></div>
  <div class="bigcap" style="{rise(B(19, 3))}">The AfterhoursNex Web helper, measured.</div>
  <div class="facts">{fl}</div>''')

# ---- 7. yours: the sidebar menu, a show dragged into place, and updates ----
rows = ["Primer", "Liner Notes", "Heat Rocks", "Song Exploder", "Hard Fork", "Rebuild"]
cols = ["#FF82AE,#B6448D", "#FEBB5F,#F37377", "#8E7CFF,#4B3FD1", "#4FD1C5,#2C7A9B", "#F7926D,#B6448D", "#9AE6B4,#2F855A"]
sb = ""
for k, (r, c) in enumerate(zip(rows, cols)):
    anim = (a("liftmove", 1.2, B(22, 1)) if k == 1 else a("shiftup", .6, B(22, 1.6), "cubic-bezier(.3,1.4,.5,1)") if k in (2, 3) else "")
    sb += f'<div class="sr" style="{anim}"><i style="background:linear-gradient(135deg,{c})"></i><span>{r}</span></div>'
s7 = scene(B(22), B(24), chapter("07", "Yours", "Yours to<br>arrange", "Rename, pin, group and drag shows into order. And it tells you when there’s something new.", B(22)) + f'''
  <div class="glass side" style="{a("panel", 1.0, B(22, .5))}"><div class="k">Your shows</div>{sb}
    <div class="menu" style="{a("menu", .5, B(23, .5))}"><span>Rename…</span><span>Pin to top</span><span>Move to group…</span><span class="red">Remove…</span></div></div>
  <div class="glass toast" style="{a("toast", .9, B(23, 1.5))}"><img src="shots/icon.png"><div><b>AfterhoursNex</b><span>A new release is on GitHub</span></div><em>Download</em></div>''')

# ---- 8. finale ----
s8 = scene(B(24), END + 1, f'''<img class="fin-icon" src="shots/icon.png" style="{a("pop", 1.1, B(24))}">
  <svg class="fin-word" width="1920" height="1080" viewBox="0 0 1920 1080" style="{rise(B(24, .5), 1.4)}">{wordmark(0.95, 960, 600, "g8")}</svg>
  <div class="fin-slogan" style="{rise(B(25), 1.4)}">How many times do I have left to see you again in my life?</div>
  <div class="fin-plats" style="{rise(B(26), 1.2)}">macOS · Windows · Linux · Android · Web</div>
  <div class="fin-free" style="{rise(B(26, 2), 1.2)}">Free · private · no account needed</div>''')

CSS = open("promo4.css", encoding="utf-8").read() if False else ""
CSS = r"""
@property --i { syntax: '<integer>'; inherits: false; initial-value: 0; }
@property --d { syntax: '<integer>'; inherits: false; initial-value: 0; }
* { box-sizing: border-box; }
html, body { margin: 0; width: 1920px; height: 1080px; overflow: hidden; background: #0E0E12; color: #F4F4F7; font-family: Carlito, 'DejaVu Sans', sans-serif; }
.serif { font-family: 'GFS Baskerville', 'Liberation Serif', serif; font-weight: 400; }
.blob { position: absolute; border-radius: 50%; filter: blur(90px); opacity: .55; }
.b1 { width: 900px; height: 900px; left: -200px; top: -300px; background: radial-gradient(circle, rgba(200,50,108,.55), transparent 65%); animation: drift1 72s linear 0s both; }
.b2 { width: 1000px; height: 1000px; right: -300px; bottom: -400px; background: radial-gradient(circle, rgba(106,63,209,.5), transparent 65%); animation: drift2 72s linear 0s both; }
.b3 { width: 700px; height: 700px; left: 40%; top: 30%; background: radial-gradient(circle, rgba(254,187,95,.22), transparent 65%); animation: drift3 72s linear 0s both; }
@keyframes drift1 { to { transform: translate(380px, 260px); } } @keyframes drift2 { to { transform: translate(-420px, -180px); } } @keyframes drift3 { to { transform: translate(-300px, 160px) scale(1.2); } }
.sc { position: absolute; inset: 0; opacity: 0; }
@keyframes sc { 0% { opacity: 0; transform: translateY(18px) scale(.99); filter: blur(6px); animation-timing-function: cubic-bezier(.1,.9,.2,1); }
  7% { opacity: 1; transform: none; filter: none; } 93% { opacity: 1; transform: none; filter: none; animation-timing-function: cubic-bezier(.9,.1,1,.2); }
  100% { opacity: 0; transform: scale(1.015); filter: blur(8px); } }
@keyframes rise { from { opacity: 0; transform: translateY(34px); } to { opacity: 1; transform: none; } }
@keyframes fadein { from { opacity: 0; } to { opacity: 1; } }
@keyframes out { to { opacity: 0; } }
.v12, .big .n, .ch .num { font-family: "Liberation Serif", serif !important; }
@keyframes pop { from { opacity: 0; transform: scale(.82); } to { opacity: 1; transform: none; } }
@keyframes panel { from { opacity: 0; transform: translateY(40px) scale(.965); } to { opacity: 1; transform: none; } }
@keyframes unveil { from { opacity: 0; transform: translateY(.45em); clip-path: inset(0 0 100% 0); } 55% { opacity: 1; } to { opacity: 1; transform: none; clip-path: inset(-10% -5% -25% -5%); } }
@keyframes numin { from { opacity: 0; transform: translateY(60px); } to { opacity: 1; transform: none; } }
@keyframes draw { from { transform: translateX(-50%) scaleX(0); } to { transform: translateX(-50%) scaleX(1); } }
@keyframes v12 { 0% { opacity: 0; transform: translateX(-50%) translateY(30px); background-position: 200% 0; } 40% { opacity: 1; } 100% { opacity: 1; transform: translateX(-50%); background-position: 0 0; } }
@keyframes type { from { width: 0; } to { width: 14.2em; } }
@keyframes blink { to { visibility: hidden; } }
@keyframes wave { from { transform: scaleY(.35); } to { transform: scaleY(1); } }
@keyframes tovoice { to { opacity: 0; transform: scale(.92); } }
@keyframes fly { from { opacity: 0; transform: translate(420px, -60px) rotate(8deg); } to { opacity: 1; transform: none; } }
@keyframes tgtrack { from { background: rgba(120,120,128,.34); } to { background: linear-gradient(180deg, #4BE06F, #2DB54F); } }
@keyframes tgknob { 0% { left: 7px; width: 76px; } 50% { width: 104px; } 100% { left: 67px; width: 76px; } }
@keyframes seethrough { from { background: rgba(26,26,32,.86); } to { background: rgba(26,26,32,.28); } }
@keyframes slide { from { left: 18%; } to { left: 78%; } }
@keyframes dash { from { stroke-dashoffset: 1200; opacity: 0; } 10% { opacity: 1; } to { stroke-dashoffset: 0; opacity: 1; } }
@keyframes lit { from { color: rgba(244,244,247,.3); text-shadow: none; } to { color: #FFF1DE; text-shadow: 0 0 22px rgba(255,130,174,.55); } }
@keyframes count { from { --i: 0; --d: 0; } to { --i: 31; --d: 8; } }
@keyframes liftmove { 0% { transform: none; box-shadow: none; } 20% { transform: scale(1.04); box-shadow: 0 16px 40px rgba(0,0,0,.45); background: rgba(255,255,255,.1); }
  80% { transform: translateY(104px) scale(1.04); box-shadow: 0 16px 40px rgba(0,0,0,.45); background: rgba(255,255,255,.1); } 100% { transform: translateY(104px); box-shadow: none; background: rgba(255,255,255,.06); } }
@keyframes shiftup { from { transform: none; } to { transform: translateY(-52px); } }
@keyframes menu { from { opacity: 0; transform: scale(.9); } to { opacity: 1; transform: none; } }
@keyframes toast { from { opacity: 0; transform: translateY(-50px); } to { opacity: 1; transform: none; } }
.glass { position: absolute; background: rgba(30,30,38,.55); border: 1px solid rgba(255,255,255,.12); border-radius: 26px; backdrop-filter: blur(26px) saturate(1.2);
  box-shadow: 0 30px 80px rgba(0,0,0,.45), inset 0 1px 0 rgba(255,255,255,.08); opacity: 0; }
.ch { position: absolute; left: 140px; width: 640px; }
.ch .num { position: absolute; left: -16px; top: -150px; font-size: 230px; line-height: 1; color: rgba(255,255,255,.07); opacity: 0; }
.kick { font-size: 22px; letter-spacing: .3em; text-transform: uppercase; color: #FF82AE; font-weight: 700; opacity: 0; }
.ch h2 { font-size: 86px; line-height: 1.02; margin: 18px 0 0; opacity: 0; }
.ch .body { font-size: 30px; line-height: 1.45; color: rgba(244,244,247,.68); margin: 28px 0 0; max-width: 560px; opacity: 0; }
.ch.top { top: 90px; left: 110px; } .ch.top h2 { font-size: 64px; }
.hair { position: absolute; left: 50%; top: 540px; width: 920px; height: 1px; background: linear-gradient(90deg, transparent, rgba(255,130,174,.9), transparent); transform-origin: center; }
.pre { position: absolute; left: 0; right: 0; top: 486px; text-align: center; font-size: 22px; letter-spacing: .34em; text-transform: uppercase; color: rgba(244,244,247,.7); opacity: 0; }
.op-icon { position: absolute; left: 890px; top: 250px; width: 140px; height: 140px; border-radius: 32px; opacity: 0; }
.op-word, .fin-word { position: absolute; left: 0; top: 0; opacity: 0; }
.v12 { position: absolute; left: 50%; top: 640px; font-size: 190px; line-height: 1; color: transparent; -webkit-text-stroke: 1.4px rgba(255,255,255,.55);
  background: linear-gradient(90deg, #FEBB5F, #F37377 40%, transparent 60%) 200% 0 / 200% 100%; -webkit-background-clip: text; background-clip: text; opacity: 0; }
.tag { position: absolute; left: 0; right: 0; top: 880px; text-align: center; font-size: 30px; letter-spacing: .12em; color: rgba(244,244,247,.75); opacity: 0; }
.p-radio { left: 880px; top: 170px; width: 470px; padding: 26px 26px 14px; } .p-search { left: 1380px; top: 250px; width: 430px; padding: 24px; }
.p-prev { left: 1000px; top: 640px; width: 560px; padding: 22px; display: flex; gap: 22px; align-items: center; }
.pt { font-size: 28px; font-weight: 700; margin-bottom: 14px; } .pt span { color: rgba(244,244,247,.5); font-weight: 400; }
.st, .res { display: flex; align-items: center; gap: 16px; padding: 12px 0; border-top: 1px solid rgba(255,255,255,.07); opacity: 0; }
.st i, .res i { width: 54px; height: 54px; border-radius: 14px; flex: none; } .st b, .res b { display: block; font-size: 22px; } .st span, .res span { font-size: 17px; color: rgba(244,244,247,.55); }
.st > div, .res > div { flex: 1; } .live { font-style: normal; font-size: 14px; letter-spacing: .1em; color: #FF6B6B; }
.eq { display: flex; gap: 3px; align-items: flex-end; height: 22px; } .eq u { width: 4px; background: #FF82AE; border-radius: 2px; animation: wave .4s ease-in-out infinite alternate; height: 22px; }
.eq u:nth-child(2) { animation-delay: .15s; } .eq u:nth-child(3) { animation-delay: .3s; }
.field { display: flex; align-items: center; gap: 10px; height: 54px; padding: 0 18px; border-radius: 14px; background: rgba(120,120,128,.22); font-size: 21px; }
.field u { width: 16px; height: 16px; border: 2px solid rgba(244,244,247,.6); border-radius: 50%; flex: none; }
.typed { font-weight: 400; white-space: nowrap; overflow: hidden; display: inline-block; width: 0; } .field s { width: 2px; height: 26px; background: #FF82AE; }
.chips { display: flex; gap: 8px; margin: 16px 0 6px; } .chips span { padding: 7px 14px; border-radius: 20px; background: rgba(120,120,128,.22); font-size: 16px; } .chips .on { background: #F4F4F7; color: #111; }
.p-prev .cover { width: 150px; height: 150px; border-radius: 18px; flex: none; } .k { font-size: 15px; letter-spacing: .2em; text-transform: uppercase; color: #FF82AE; font-weight: 700; }
.p-prev b { display: block; font-size: 30px; margin-top: 6px; font-family: 'GFS Baskerville', serif; font-weight: 400; } .p-prev span { font-size: 18px; color: rgba(244,244,247,.55); }
.pills { display: flex; gap: 10px; margin-top: 14px; } .pills em { font-style: normal; padding: 9px 18px; border-radius: 22px; background: rgba(255,130,174,.16); color: #FF82AE; font-size: 17px; font-weight: 700; }
.pills .fill { background: #FF82AE; color: #2B0715; }
.vid { left: 940px; top: 200px; width: 760px; height: 400px; overflow: hidden; }
.vid .screen { position: absolute; inset: 18px; border-radius: 16px; background: linear-gradient(135deg, #3B2A6E, #8E3B6E 60%, #F37377); display: flex; align-items: center; justify-content: center; }
.vid .play { width: 96px; height: 96px; border-radius: 50%; background: rgba(255,255,255,.18); display: flex; align-items: center; justify-content: center; font-size: 40px; padding-left: 8px; }
.vid .prog { position: absolute; left: 24px; right: 24px; bottom: 22px; height: 5px; border-radius: 3px; background: linear-gradient(90deg, #fff 35%, rgba(255,255,255,.25) 35%); }
.wave { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; gap: 7px; opacity: 0; }
.wave u { width: 9px; border-radius: 5px; background: linear-gradient(180deg, #FEBB5F, #FF82AE); transform-origin: center; }
.srcs { position: absolute; left: 940px; top: 630px; display: flex; gap: 12px; opacity: 0; } .srcs span, .file { padding: 10px 20px; border-radius: 22px; background: rgba(255,255,255,.1); font-size: 20px; font-weight: 700; }
.stack { left: 1240px; top: 690px; width: 460px; padding: 22px 24px; } .files { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 12px; } .file { opacity: 0; background: rgba(255,130,174,.16); color: #FFC2D6; }
.corner { position: absolute; left: 140px; top: 110px; opacity: 0; } .corner .kick { opacity: 1; }
.lyrics { position: absolute; left: 260px; right: 260px; top: 150px; height: 760px; overflow: hidden; -webkit-mask-image: linear-gradient(transparent, #000 18%, #000 72%, transparent); }
.roll { padding-top: 260px; } .ly { height: 150px; transform-origin: 0 50%; font-family: 'GFS Baskerville', serif; font-size: 62px; line-height: 1.1; color: rgba(244,244,247,.28); }
.ly .jp { font-family: IPAGothic, sans-serif; font-size: 26px; color: rgba(255,194,214,.85); margin-top: 10px; opacity: 0; }
.note { position: absolute; left: 140px; bottom: 90px; font-size: 24px; line-height: 1.5; color: rgba(244,244,247,.6); opacity: 0; }
.art-card { position: absolute; left: 900px; top: 170px; width: 880px; height: 740px; border-radius: 34px; overflow: hidden; opacity: 0; background: #1A1424; }
.art-card .c1, .art-card .c2, .art-card .c3 { position: absolute; border-radius: 50%; filter: blur(40px); }
.c1 { width: 420px; height: 420px; left: -60px; top: -40px; background: #FF82AE; } .c2 { width: 460px; height: 460px; right: -80px; top: 180px; background: #6A3FD1; }
.c3 { width: 380px; height: 380px; left: 220px; bottom: -120px; background: #FEBB5F; }
.feel { left: 90px; top: 110px; width: 700px; height: 520px; padding: 60px 56px; opacity: 1; background: rgba(26,26,32,.86); }
.feel .row { display: flex; gap: 40px; justify-content: center; margin-top: 30px; }
.tg { position: relative; width: 150px; height: 90px; border-radius: 45px; background: rgba(120,120,128,.34); border: 1.5px solid rgba(255,255,255,.35); overflow: hidden; }
.tg::before { content: ""; position: absolute; inset: 0 0 50% 0; background: linear-gradient(rgba(255,255,255,.35), transparent); border-radius: 45px 45px 0 0; }
.tg i { position: absolute; top: 7px; left: 7px; width: 76px; height: 76px; border-radius: 38px; background: linear-gradient(#fff, #EDEDF2); box-shadow: 0 4px 12px rgba(0,0,0,.25); }
.sl { margin-top: 90px; } .sl span { font-size: 24px; color: rgba(244,244,247,.8); } .track { position: relative; height: 8px; margin-top: 24px; border-radius: 4px; background: linear-gradient(90deg, #FF82AE 0%, rgba(120,120,128,.35) 0%); }
.track b { position: absolute; top: -14px; width: 36px; height: 36px; margin-left: -18px; border-radius: 50%; background: #fff; box-shadow: 0 4px 14px rgba(0,0,0,.3); }
.win { position: absolute; border-radius: 16px; overflow: hidden; background: #111114; border: 1px solid rgba(255,255,255,.12); box-shadow: 0 30px 80px rgba(0,0,0,.5); opacity: 0; }
.win img { display: block; width: 100%; }
.mac { left: 100px; top: 340px; width: 780px; } .lights { position: absolute; left: 14px; top: 12px; display: flex; gap: 8px; } .lights i { width: 13px; height: 13px; border-radius: 50%; background: #FF5F57; }
.lights i:nth-child(2) { background: #FEBC2E; } .lights i:nth-child(3) { background: #28C840; }
.pc { left: 960px; top: 300px; width: 520px; } .caps { display: flex; justify-content: flex-end; height: 30px; background: #18181C; } .caps i { width: 44px; text-align: center; font-style: normal; font-size: 14px; line-height: 30px; color: rgba(244,244,247,.7); }
.web { left: 960px; top: 650px; width: 520px; } .tab { display: flex; align-items: center; gap: 14px; height: 40px; padding: 0 14px; background: #1C1C22; font-size: 15px; }
.tab i { font-style: normal; padding: 5px 14px; border-radius: 8px 8px 0 0; background: #2A2A31; } .tab span { flex: 1; padding: 4px 12px; border-radius: 8px; background: #111114; color: rgba(244,244,247,.6); }
.phone4 { position: absolute; left: 1560px; top: 400px; width: 250px; height: 527px; border-radius: 36px; border: 8px solid #1E1E24; overflow: hidden; background: #000; box-shadow: 0 30px 80px rgba(0,0,0,.5); opacity: 0; }
.phone4 img { width: 100%; height: 100%; object-fit: cover; object-position: top; }
.link path { fill: none; stroke: #FF82AE; stroke-width: 2.5; stroke-dasharray: 10 12; opacity: 0; }
.linklabel { position: absolute; left: 980px; top: 168px; width: 560px; text-align: center; font-size: 20px; color: #FFC2D6; opacity: 0; }
.plats { position: absolute; left: 0; right: 0; bottom: 30px; display: flex; justify-content: center; gap: 70px; font-size: 24px; letter-spacing: .22em; text-transform: uppercase; color: rgba(244,244,247,.3); }
.big { position: absolute; left: 980px; top: 230px; display: flex; align-items: baseline; gap: 16px; }
.big .n { font-size: 260px; line-height: 1; background: linear-gradient(180deg, #FFF, #FFC2D6); -webkit-background-clip: text; background-clip: text; color: transparent; counter-reset: i var(--i) d var(--d); }
.big .n::before { content: counter(i) "." counter(d); } .big .mb { font-size: 64px; color: rgba(244,244,247,.6); }
.bigcap { position: absolute; left: 990px; top: 530px; font-size: 24px; color: rgba(244,244,247,.6); opacity: 0; }
.facts { position: absolute; left: 990px; top: 620px; width: 780px; } .fact { display: flex; gap: 18px; align-items: flex-start; margin-bottom: 22px; opacity: 0; }
.fact i { font-style: normal; width: 36px; height: 36px; border-radius: 50%; background: rgba(52,208,88,.18); color: #34D058; display: flex; align-items: center; justify-content: center; font-size: 20px; flex: none; }
.fact b { display: block; font-size: 26px; } .fact span { font-size: 19px; color: rgba(244,244,247,.55); }
.side { left: 940px; top: 220px; width: 360px; padding: 22px 16px; } .side .k { padding: 0 10px 10px; }
.sr { display: flex; align-items: center; gap: 14px; height: 52px; padding: 0 10px; border-radius: 12px; font-size: 22px; position: relative; }
.sr i { width: 32px; height: 32px; border-radius: 8px; }
.menu { position: absolute; left: 230px; top: 250px; width: 250px; padding: 8px; border-radius: 14px; background: rgba(40,40,48,.95); border: 1px solid rgba(255,255,255,.12); box-shadow: 0 20px 50px rgba(0,0,0,.5); opacity: 0; transform-origin: top left; }
.menu span { display: block; padding: 11px 14px; font-size: 19px; border-radius: 8px; } .menu span:first-child { background: rgba(255,255,255,.08); } .menu .red { color: #FF8A8A; }
.toast { left: 1360px; top: 260px; width: 430px; padding: 18px; display: flex; gap: 16px; align-items: center; }
.toast img { width: 56px; height: 56px; border-radius: 14px; } .toast b { display: block; font-size: 21px; } .toast span { font-size: 17px; color: rgba(244,244,247,.6); } .toast > div { flex: 1; }
.toast em { font-style: normal; padding: 8px 16px; border-radius: 20px; background: #FF82AE; color: #2B0715; font-weight: 700; font-size: 16px; }
.fin-icon { position: absolute; left: 885px; top: 200px; width: 150px; height: 150px; border-radius: 34px; opacity: 0; }
.fin-slogan { position: absolute; left: 0; right: 0; top: 680px; text-align: center; font-family: 'Liberation Serif', serif; font-style: italic; font-size: 40px; color: rgba(244,244,247,.85); opacity: 0; }
.fin-plats { position: absolute; left: 0; right: 0; top: 790px; text-align: center; font-size: 24px; letter-spacing: .3em; text-transform: uppercase; color: rgba(255,194,214,.8); opacity: 0; }
.fin-free { position: absolute; left: 0; right: 0; top: 850px; text-align: center; font-size: 22px; color: rgba(244,244,247,.55); opacity: 0; }
.fadeout { position: absolute; inset: 0; background: #000; opacity: 0; animation: fadein 1.2s linear """ + f"{END - 1.2:.3f}" + r"""s both; }
.fadein0 { position: absolute; inset: 0; background: #000; animation: gone 1.0s linear 0s both; } @keyframes gone { from { opacity: 1; } to { opacity: 0; } }
"""
page = f"""<!doctype html><html><head><meta charset="utf-8"><title>AfterhoursNex 1.2</title><style>{CSS}{ly_css}</style></head>
<body>{blobs}{s0}{s1}{s2}{s3}{s4}{s5}{s6}{s7}{s8}<div class="fadein0"></div><div class="fadeout"></div>
<script>
window.seek = t => {{ for (const an of document.getAnimations()) {{ an.pause(); an.currentTime = t * 1000; }} }};
window.LENGTH = {END};
</script></body></html>"""
open("promo4.html", "w", encoding="utf-8").write(page)
json.dump({"END": END, "BPM": BPM}, open("timing4.json", "w"))
print(f"promo4.html: {len(page) // 1024} KB; {END:.1f} s at {BPM} BPM")

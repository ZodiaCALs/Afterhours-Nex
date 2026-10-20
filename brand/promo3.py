"""AfterhoursNex film, third cut: 37 bars at 96 BPM (92.5 s, 60 fps). Everything lands on the beat."""
import json, random
import promo as P                                   # device mock-ups and base styles
import promo2 as P2                                 # letter geometry, the x's pen paths, stars
# The app's screens in the film are screenshots of its design (shared with the Afterhours web app), from shots/capture.py.
def shot(name, t, extra="", dark=False, overlay="", small=False):
    return (f'<div class="phone{" small" if small else ""}" style="{P.a("pm-phone", 1.1, t)}{extra}"><div class="screen shot{" dk" if dark else ""}">'
            f'<img src="shots/{name}.png">{P.status()}{overlay}</div></div>')
BPM = 96; BEAT = 60 / BPM; BAR = 4 * BEAT
def B(bar, beat=0.0): return (bar - 1) * BAR + beat * BEAT
END = B(38)                                          # 92.5 s
W, H = 1920, 1080
a, EASE = P.a, P.EASE

# ---- the title: letters written by a flowing light; the x written in the same hand ----
T = {"FRONT0": B(1, 3.2), "FRONT1": B(3), "BOLD0": B(3), "BOLD1": B(3, 1.0), "LIFT1": B(3, 1.4), "FINE0": B(3, 1.4), "FINE1": B(3, 2.3),
     "SHEEN": B(3, 2.6), "RULE": B(3, 3), "PUSH": B(4, 2), "OUT": B(4, 3.2), "X0": P2.X0, "XL": P2.XL, "NEX_X": P2.NEX_X, "BASE": P2.BASE,
     "ICX": P2.ICX, "ICY": P2.ICY, "IS": P2.IS, "W": W, "XT": P2.XT, "XH": P2.XH}
json.dump({k: T[k] for k in ("BOLD0", "FINE0", "FINE1", "SHEEN")} | {"END": END, "BPM": BPM}, open("timing3.json", "w"))
lift = "M %s C %s" % (P2.fmt(P2.pt(.80, .98)), P2.fmt(P2.pt(1.18, .78), P2.pt(1.06, .05), P2.pt(.83, .035)))   # the pen lifting to the top of the fine stroke
after = "".join(f'<path class="L" data-cx="{P2.X0 + (g["x"] + 34) * P2.F:.1f}" d="{P2.gd("after", g, P2.X0, P2.BASE)}"/>' for g in P2.G["after"]["glyphs"])
ne = "".join(f'<path class="L" data-cx="{P2.NEX_X + (g["x"] + 40) * P2.F:.1f}" d="{P2.gd("nex", g, P2.NEX_X, P2.BASE)}"/>' for g in P2.G["nex"]["glyphs"][:2])
x_d = P2.gd("nex", P2.xg, P2.NEX_X, P2.BASE)
nex_all = "".join(f'<path d="{P2.gd("nex", g, P2.NEX_X, P2.BASE)}"/>' for g in P2.G["nex"]["glyphs"])
ICX, ICY, IS, BASE = P2.ICX, P2.ICY, P2.IS, P2.BASE
title = f'''<svg id="title" class="title" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
  <linearGradient id="t-sunset" x1="{P2.NEX_X:.0f}" y1="{BASE - 160}" x2="{P2.NEX_X + P2.G["nex"]["width"] * P2.F:.0f}" y2="{BASE + 16}" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="#FEBB5F"/><stop offset=".35" stop-color="#F7926D"/><stop offset=".62" stop-color="#F37377"/><stop offset="1" stop-color="#B6448D"/></linearGradient>
  <linearGradient id="t-write" x1="0" y1="0" x2="70" y2="0" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#fff"/><stop offset="1" stop-color="#000"/></linearGradient>
  <mask id="t-wmask" maskUnits="userSpaceOnUse" x="0" y="0" width="{W}" height="{H}"><rect width="{W}" height="{H}" fill="url(#t-write)"/></mask>
  <filter id="t-feather" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="3.2"/></filter>
  <mask id="t-xmask" maskUnits="userSpaceOnUse" x="0" y="0" width="{W}" height="{H}"><g filter="url(#t-feather)">
    <path id="t-bold" d="{P2.BOLD}" fill="none" stroke="#fff" stroke-width="{P2.XW * .29:.1f}" stroke-linecap="round"/>
    <path id="t-fine" d="{P2.FINE}" fill="none" stroke="#fff" stroke-width="{P2.XW * .19:.1f}" stroke-linecap="round"/></g></mask>
  <path id="t-lift" d="{lift}"/>
  <linearGradient id="t-shine" x1="0" y1="0" x2="150" y2="0" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".5" stop-color="#fff" stop-opacity=".9"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
  <radialGradient id="t-halog"><stop offset="0" stop-color="#F7926D" stop-opacity=".42"/><stop offset=".5" stop-color="#CC5286" stop-opacity=".12"/><stop offset="1" stop-color="#CC5286" stop-opacity="0"/></radialGradient>
  <radialGradient id="t-frontg"><stop offset="0" stop-color="#FFF4E6" stop-opacity=".55"/><stop offset="1" stop-color="#FFF4E6" stop-opacity="0"/></radialGradient>
  <radialGradient id="t-penlight"><stop offset="0" stop-color="#FFF4E6" stop-opacity=".8"/><stop offset=".35" stop-color="#FFE3C0" stop-opacity=".35"/><stop offset="1" stop-color="#FFE3C0" stop-opacity="0"/></radialGradient>
  <linearGradient id="t-rule" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#CC5286" stop-opacity="0"/><stop offset=".5" stop-color="#F7926D"/><stop offset="1" stop-color="#CC5286" stop-opacity="0"/></linearGradient>
  <filter id="t-blur" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="8"/></filter>
  <clipPath id="t-iconclip"><rect x="{ICX - IS / 2}" y="{ICY - IS / 2}" width="{IS}" height="{IS}" rx="{IS * .22:.0f}"/></clipPath>
</defs>
<g id="t-cam">
  <circle id="t-halo" cx="{ICX}" cy="{ICY}" r="300" fill="url(#t-halog)" opacity="0"/>
  <rect id="t-ring" x="{ICX - IS / 2}" y="{ICY - IS / 2}" width="{IS}" height="{IS}" rx="{IS * .24:.0f}" fill="none" stroke="#F7926D" stroke-width="1.6" opacity="0"/>
  <g id="t-icon" opacity="0"><image href="data:image/png;base64,{P2.ICON}" x="{ICX - IS / 2}" y="{ICY - IS / 2}" width="{IS}" height="{IS}"/>
    <rect id="t-iconshine" clip-path="url(#t-iconclip)" x="{ICX - IS / 2}" y="{ICY - IS / 2}" width="{IS}" height="{IS}" fill="url(#t-shine)" opacity="0"/></g>
  <g id="t-word">
    <ellipse id="t-front" cx="0" cy="{BASE - 80}" rx="46" ry="150" fill="url(#t-frontg)" opacity="0"/>
    <g mask="url(#t-wmask)"><g fill="#F5EEF4">{after}</g><g fill="url(#t-sunset)">{ne}</g></g>
    <g id="t-xrise">
      <g mask="url(#t-xmask)"><g id="t-xglow" filter="url(#t-blur)" opacity="0" fill="#FFD9A0"><path d="{x_d}"/></g></g>
      <g mask="url(#t-xmask)" fill="url(#t-sunset)"><path d="{x_d}"/></g>
    </g>
    <g id="t-nexshine" fill="url(#t-shine)" opacity="0">{nex_all}</g>
  </g>
  <circle id="t-pen" r="46" fill="url(#t-penlight)" opacity="0"/>
  <rect id="t-ruleL" y="{BASE + 58}" height="1.6" fill="url(#t-rule)" width="0"/><rect id="t-ruleR" y="{BASE + 58}" height="1.6" fill="url(#t-rule)" width="0"/>
  <rect id="t-gem" width="11" height="11" fill="#F7926D" opacity="0"/>
</g></svg>'''
TITLE_JS = f"""
const T = {json.dumps(T)};
const $ = id => document.getElementById(id);
const clamp = (v, a = 0, b = 1) => Math.min(b, Math.max(a, v));
const smooth = u => u * u * (3 - 2 * u);
const sine = u => .5 - .5 * Math.cos(Math.PI * u);
const outCubic = u => 1 - Math.pow(1 - u, 3);
const outBack = u => {{ const s = 1.7; u -= 1; return u * u * ((s + 1) * u + s) + 1; }};
const bold = $('t-bold'), fine = $('t-fine'), liftPath = $('t-lift');
const LB = bold.getTotalLength(), LF = fine.getTotalLength(), LL = liftPath.getTotalLength();
bold.setAttribute('stroke-dasharray', LB + ' ' + (LB + 40)); fine.setAttribute('stroke-dasharray', LF + ' ' + (LF + 40));
const letters = [...document.querySelectorAll('#title .L')];
window.renderTitle = t => {{
  const ic = outCubic(clamp((t - .4) / 1.6));
  $('t-icon').setAttribute('opacity', ic);
  $('t-icon').setAttribute('transform', `translate(0 ${{(1 - ic) * 40}}) translate(${{T.ICX}} ${{T.ICY}}) scale(${{.9 + .1 * ic}}) translate(${{-T.ICX}} ${{-T.ICY}})`);
  $('t-halo').setAttribute('opacity', smooth(clamp((t - .6) / 1.8)) * (.85 + .15 * Math.sin(t * 1.3)));
  const r = clamp((t - 1.6) / 2.2); $('t-ring').setAttribute('opacity', r > 0 && r < 1 ? .6 * (1 - r) : 0);
  $('t-ring').setAttribute('transform', `translate(${{T.ICX}} ${{T.ICY}}) scale(${{1 + .6 * outCubic(r)}}) translate(${{-T.ICX}} ${{-T.ICY}})`);
  const sh = clamp((t - 1.8) / 1.2); $('t-iconshine').setAttribute('opacity', sh > 0 && sh < 1 ? .55 : 0);
  $('t-iconshine').setAttribute('x', T.ICX - T.IS * 1.2 + sh * T.IS * 2.1);
  // "Afterhours" and "Ne": the front of light flows along the line, and each letter rises as it passes
  const wp = clamp((t - T.FRONT0) / (T.FRONT1 - T.FRONT0)), front = T.X0 - 70 + (T.XL + 4 - (T.X0 - 70)) * sine(wp);
  $('t-write').setAttribute('x1', front - 70); $('t-write').setAttribute('x2', front);
  $('t-front').setAttribute('cx', front); $('t-front').setAttribute('opacity', wp > 0 && wp < 1 ? Math.sin(Math.PI * Math.min(1, wp * 1.15)) * .9 : 0);
  for (const l of letters) {{ const p = outCubic(clamp((front - +l.dataset.cx) / 190)); l.setAttribute('transform', `translate(0 ${{(1 - p) * 20}})`); }}
  // the x, in the same hand: the bold stroke, a lift of the pen, the fine stroke; the light rides the pen
  const pb = sine(clamp((t - T.BOLD0) / (T.BOLD1 - T.BOLD0))), pl = sine(clamp((t - T.BOLD1) / (T.LIFT1 - T.BOLD1))), pf = sine(clamp((t - T.FINE0) / (T.FINE1 - T.FINE0)));
  bold.setAttribute('stroke-dashoffset', LB * (1 - pb) + (pb === 0 ? 40 : 0)); fine.setAttribute('stroke-dashoffset', LF * (1 - pf) + (pf === 0 ? 40 : 0));
  let q = null, pen = 0;
  if (t >= T.BOLD0 - .12 && t < T.BOLD1) {{ q = bold.getPointAtLength(LB * pb); pen = smooth(clamp((t - (T.BOLD0 - .12)) / .2)); }}
  else if (t >= T.BOLD1 && t < T.LIFT1) {{ q = liftPath.getPointAtLength(LL * pl); pen = .55 + .45 * Math.abs(Math.cos(Math.PI * pl)); }}
  else if (t >= T.FINE0 && t < T.FINE1 + .45) {{ q = fine.getPointAtLength(LF * pf); pen = 1 - smooth(clamp((t - T.FINE1) / .45)); }}
  if (q) {{ $('t-pen').setAttribute('cx', q.x); $('t-pen').setAttribute('cy', q.y); }}
  $('t-pen').setAttribute('opacity', pen * .9);
  const xr = sine(clamp((t - T.BOLD0) / (T.FINE1 - T.BOLD0 + .3)));
  $('t-xrise').setAttribute('transform', `translate(0 ${{(1 - xr) * 18}})`);
  $('t-xglow').setAttribute('opacity', t < T.FINE1 ? .5 * Math.max(pb * (1 - pl * .3), pf) : .5 * (1 - smooth(clamp((t - T.FINE1) / 1.1))));
  const ns = clamp((t - T.SHEEN) / 1.2); $('t-nexshine').setAttribute('opacity', ns > 0 && ns < 1 ? .8 : 0);
  $('t-shine').setAttribute('gradientTransform', `translate(${{T.NEX_X - 220 + sine(ns) * 640}} 0)`);
  const rl = outCubic(clamp((t - T.RULE) / 1.1)), reach = 330 * rl;
  $('t-ruleL').setAttribute('x', T.W / 2 - 12 - reach); $('t-ruleL').setAttribute('width', reach);
  $('t-ruleR').setAttribute('x', T.W / 2 + 12); $('t-ruleR').setAttribute('width', reach);
  const gm = clamp((t - T.RULE) / .6), gs = gm > 0 ? outBack(gm) : 0;
  $('t-gem').setAttribute('opacity', gm > 0 ? 1 : 0); $('t-gem').setAttribute('transform', `translate(${{T.W / 2}} ${{T.BASE + 58.8}}) rotate(45) scale(${{gs}}) translate(-5.5 -5.5)`);
  const push = 1 + .06 * sine(clamp((t - T.PUSH) / (T.OUT + .8 - T.PUSH)));
  $('t-cam').setAttribute('transform', `translate(${{T.W / 2}} ${{T.BASE - 120}}) scale(${{push}}) translate(${{-T.W / 2}} ${{-(T.BASE - 120)}})`);
  $('title').style.opacity = 1 - smooth(clamp((t - T.OUT) / .8));
}};
"""
open("title3.part.json", "w").write(json.dumps({"svg_len": len(title)}))
print(f"title: x bold stroke {T['BOLD0']:.3f}–{T['BOLD1']:.3f} s, lift to {T['LIFT1']:.3f} s, fine stroke to {T['FINE1']:.3f} s; film {END} s")

# ---- the scenes ----
FL = "cubic-bezier(.1,.9,.2,1)"                       # the app's Fluent ease-out
def scene(start, end, body, cls=""):
    d = end - start + .3
    return (f'<section class="scene {cls}" style="{a("pm-scene3", d, start, "linear")}">'
            f'<div class="cam" style="animation:pm-cam {d:.2f}s linear {start:.2f}s both">{body}</div></section>')
def up(t, dur=.9): return a("pm-up", dur, t)
def listen_inner(t0):
    return (f'<div class="pt">Listen Now</div><div class="ps">Good evening. Here’s what’s new on your shelf.</div>'
            f'<div class="sec">Up Next</div><div class="row">{P2.up_next(t0 + .5)}</div><div class="sec">Latest episodes</div>{P2.ep_rows(t0 + .9)}'
            f'{P.mini("The locked-room problem", "Midnight Mysteries", 0)}{P.navbar("home")}')
def playing_inner(t0, caption=""):
    return (f'<div class="nph">{P.icon("down", 26)}<span>Now Playing</span><i></i></div>'
            f'<div class="npart" style="{a("pm-ken", 8, t0, "ease-in-out")}">{P.art(7, 300, 22, 96)}</div>'
            f'<div class="npt">The locked-room problem</div><div class="nps">Midnight Mysteries</div>'
            f'<div class="slider"><i style="--a:.30;--b:.42;{a("pm-progress", 7, t0, "linear")}"></i><b style="animation:pm-knob3 7s linear {t0:.2f}s both"></b></div>'
            f'<div class="times"><span>18:42</span><span>−27:10</span></div>'
            f'<div class="ctl"><span class="sk">{P.icon("rew", 30)}<small>15</small></span><span class="big">{P.icon("pause", 40)}</span><span class="sk">{P.icon("fwd", 30)}<small>30</small></span></div>'
            + (caption or f'<div class="chips"><span class="chip">{P.icon("speed", 18)}<span>1.2×</span></span><span class="chip">{P.icon("moon", 18)}<span>30 min left</span></span>{P.icon("next", 26)}</div>'))
def discover_inner(rows, t0):
    out = '<div class="pt">Discover</div><div class="ps">Related shows, picked around what you love.</div>'
    for k, (sec, picks) in enumerate(rows):
        out += f'<div class="sec">{sec}</div><div class="row">' + "".join(P2.pick(i, ti, why, t0 + k * .15 + j * .08) for j, (i, ti, why) in enumerate(picks)) + "</div>"
    return out + P.navbar("explore")
def lock_inner(t0):
    return (f'<div class="lock"><div class="lt" style="{up(t0 + .2)}">23:47</div><div class="ld" style="{up(t0 + .3)}">Monday 19 October</div>'
            f'<div class="media" style="{up(t0 + .6)}"><div class="mh">{P.art(0, 64, 12)}<div><b>The locked-room problem</b><span>Midnight Mysteries</span></div></div>'
            f'<div class="mbar"><i style="--a:.42;--b:.5;{a("pm-progress", 5, t0 + .8, "linear")}"></i></div>'
            f'<div class="mc">{P.icon("prev", 30)}{P.icon("rew", 30)}<span class="mplay">{P.icon("pause", 34)}</span>{P.icon("fwd", 30)}{P.icon("next", 30)}</div></div></div>')
def copy_top(kicker, title, t, cls="copy-top"):
    return f'<div class="{cls}"><div class="kick" style="{up(t, .8)}">{kicker}</div><h2 style="{a("pm-titlein", 1.1, t + BEAT / 2, FL)}">{title}</h2></div>'
def glass(icon_name, label, x, y, t):
    return (f'<div class="chipx" style="left:{x}px;top:{y}px;{a("pm-pop2", .7, t)}"><div class="chipf" style="animation:pm-float 5s ease-in-out {t:.2f}s infinite both">'
            f'{P.icon(icon_name, 26) if icon_name in P.I else ICONS2[icon_name]}<span>{label}</span></div></div>')
ICONS2 = {k: f'<svg class="ic" width="26" height="26" viewBox="0 0 24 24"><path d="{d}"/></svg>' for k, d in {
    "headphones": "M12 3a9 9 0 0 0-9 9v7a2 2 0 0 0 2 2h2v-8H5v-1a7 7 0 0 1 14 0v1h-2v8h2a2 2 0 0 0 2-2v-7a9 9 0 0 0-9-9z",
    "car": "M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm11 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zM5 11l1.5-4.5h11L19 11H5z",
    "lock": "M18 8h-1V6A5 5 0 0 0 7 6v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2zm-6 9a2 2 0 1 1 0-4 2 2 0 0 1 0 4zm3.1-9H8.9V6a3.1 3.1 0 0 1 6.2 0v2z",
    "text": "M5 4v3h5.5v12h3V7H19V4H5z"}.items()}

slogan = scene(B(5), B(9), f'''<div class="glow2" style="{a("pm-breathe", 10, B(5), "ease-in-out")}"></div>
  <div class="motes">{"".join(f'<i style="left:{random.Random(i).uniform(0, 100):.1f}%;top:{random.Random(i + 99).uniform(30, 100):.1f}%;animation:pm-mote {random.Random(i + 7).uniform(7, 12):.1f}s linear {B(5) + random.Random(i + 3).uniform(-6, 0):.2f}s infinite both"></i>' for i in range(40))}</div>
  <div class="slogan2" style="{a("pm-push", 10.3, B(5), "linear")}">
    <div>{"".join(f'<span class="sw" style="{a("pm-word", 1.0, B(5, k))}">{w}</span> ' for k, w in enumerate("How many times".split()))}</div>
    <div>{"".join(f'<span class="sw" style="{a("pm-word", 1.0, B(6, k * .5))}">{w}</span> ' for k, w in enumerate("do I have left to see you".split()))}</div>
    <div>{"".join(f'<span class="sw" style="{a("pm-word", 1.1, B(7, k))}">{w}</span> ' for k, w in enumerate("again in my life?".split()))}</div></div>
  <div class="sub" style="{up(B(8), 1.4)}">AfterhoursNex · a podcast player for the late hours</div>''')

tiles = "".join(f'<div class="wt" style="{a("pm-wave", 1.0, B(9) + .15 + (k % 10) * BEAT / 4 + (k // 10) * BEAT / 2)}">{P.art(k, 180, 20, 66)}</div>' for k in range(40))
s1 = scene(B(9), B(13), f'''<div class="wall"><div class="wall-in" style="animation:pm-wall {B(13) - B(9) + .4:.2f}s linear {B(9):.2f}s both">{tiles}</div></div><div class="shade-l"></div>
  {P.copy("Your shelf", "Thirty shows,<br>ready on first launch", "Hand-picked shows find themselves in Apple’s podcast directory, or on their own websites when there’s no listing.", B(9, 1))}
  <div class="tilt" style="animation:pm-tilt 1.5s {EASE} {B(9, 2):.2f}s both"><div class="pos-r"><div class="phone" style="{a("pm-phone", 1.1, B(9, 2))}"><div class="screen shot">
    <img class="tall" src="shots/p_listen_tall.png" style="--to:-760px;animation:pm-scrolltall 3.2s {FL} {B(11):.2f}s both">
    <img class="tabbar" src="shots/p_tabbar.png">{P.status()}</div></div></div></div>''')

flying = "".join(f'''<div class="fly" style="left:{860 + k * 330}px;top:{330 + (k % 2) * 70}px;animation:pm-fly 1.2s {EASE} {B(13, .5 + k * .5):.2f}s both">
  <div class="flyc" style="animation:pm-float 6s ease-in-out {B(13, k):.2f}s infinite both">{P.art(k, 280, 24, 100)}<b>{tt}</b><span>{P.SHOWS[k][0]}</span>
  <div class="bar"><i style="--a:0;--b:{w};{a("pm-progress", 1.3, B(13, 1.2 + k * .5))}"></i></div><em>{left}</em></div></div>'''
    for k, (tt, w, left) in enumerate([("The locked-room problem", ".62", "17 min left"), ("Why the night sky is dark", ".35", "31 min left"), ("Rainy evenings in Tokyo", ".8", "6 min left")]))
s2 = scene(B(13), B(15), f'<div class="pos-l2"><div class="phone" style="{a("pm-phone", 1.1, B(13) - .15)}"><div class="screen shot"><img src="shots/p_listen_light.png"><img src="shots/p_listen_playing.png" style="animation:pm-fadein .4s {FL} {B(14):.2f}s both"><img class="band" src="shots/p_listen_playing.png" style="animation:pm-bandup .75s {FL} {B(14):.2f}s both">{P.status()}</div></div></div>{flying}' + copy_top("Up Next", "Every episode keeps your place", B(13), "copy-tr"))

rowsA = [("City pop &amp; anime songs", [(3, "City Pop Diaries", "One album a week from 1980s Tokyo."), (6, "Pixel Archive", "Songs from the games you grew up with.")]),
         ("Late-night jazz", [(4, "Late Jazz Notes", "One ballad, a whole hour."), (1, "Slow Radio", "Long, quiet interviews.")])]
rowsB = [("Mystery novels", [(0, "Midnight Mysteries", "Golden-age crime, one book at a time."), (2, "The Quiet Library", "Readers on the books that stayed.")]),
         ("Big ideas", [(2, "Small Hours Science", "The universe, explained after dark."), (7, "Slow Radio", "Conversations worth the time.")])]
rowsC = [("Gadgets &amp; tech", [(5, "Gadget Night", "The week in tech, without the noise."), (6, "Pixel Archive", "Games and the people who made them.")]),
         ("Japanese picks", [(3, "City Pop Diaries", "シティポップの名盤を一枚ずつ。"), (1, "The Quiet Library", "本と夜の話。")])]
fan = "".join(f'<div class="fan" style="animation:pm-fan{d} 1.3s {EASE} {B(15, .5):.2f}s both">{shot(rows, B(15) - .1 + k * .08)}</div>'
              for k, (d, rows) in enumerate([("L", "p_library"), ("R", "p_show"), ("C", "p_discover")]))
s3 = scene(B(15), B(17), fan + copy_top("Discover", "Find the next show to fall for", B(15)))

results = [("City Pop Diaries", "Night Shift Radio"), ("Tokyo After Dark", "Kaze Studio"), ("Crate Digging Japan", "Low Light"), ("Plastic Love Hours", "Midnight FM")]
def rbtn(k): return (f'<span class="btn"><span class="b1" style="{a("pm-out", .35, B(18, 1.5))}">Add</span><span class="b2" style="{a("pm-in", .4, B(18, 1.6))}">On your shelf</span></span>'
                     if k == 0 else '<span class="btn"><span>Add</span></span>')
s4 = scene(B(17), B(19), f'''<div class="kick center" style="{up(B(17), .8)}">Search</div>
  <div class="bigq"><span class="bq-ic">{P.icon("search", 70)}</span><span class="typed" style="{a("pm-type", 1.25, B(17, .5), "steps(8, end)")}">city pop</span><span class="caret big" style="animation:pm-blink 1s steps(2, start) {B(17):.2f}s 4 both"></span></div>
  <div class="rcards">{"".join(f'<div class="rc" style="{up(B(17, 2.5 + k * .5))}">{P.art(k + 3, 220, 20, 80)}<b>{n}</b><span>{m}</span>{rbtn(k)}</div>' for k, (n, m) in enumerate(results))}</div>
  {P.tap(843, 900, B(18, 1.3))}
  <div class="foot" style="{up(B(18, 2))}">Or paste any feed, Apple Podcasts link or website.</div>''')

rows_sp = [("0.75×", ""), ("1×", "Normal"), ("1.25×", ""), ("1.5×", ""), ("2×", "")]
speedcard = (f'<div class="speedcard" style="{a("pm-pop2", .8, B(19, 3))}"><div class="sc-h">Playback speed</div>'
             f'<div class="sc-hl" style="animation:pm-scmove .6s {FL} {B(20, 2):.2f}s both"></div>'
             + "".join(f'<div class="sc-r"><span>{v}</span><em>{n}</em></div>' for v, n in rows_sp)
             + f'<i class="sc-ck" style="animation:pm-scmove .6s {FL} {B(20, 2):.2f}s both">✓</i></div>')
orbit = "".join(glass(*c) for c in [("rew", "Back 15", 330, 600, B(19, 3)), ("fwd", "Forward 30", 1300, 330, B(20, 0)),
                                    ("moon", "Sleep · 30 min", 1290, 620, B(20, 2)), ("next", "Plays the next episode", 1200, 880, B(21, 0)), ("library", "Remembers your place", 190, 880, B(21, 1))])
s5 = scene(B(19), B(22), f'<div class="backart" style="{a("pm-ken2", 8, B(19), "ease-in-out")}"></div><div class="pos-c"><div class="phone" style="{a("pm-phone", 1.1, B(19, .25))}"><div class="screen shot"><img src="shots/p_listen_playing.png"><img src="shots/p_np.png" style="animation:pm-nprise .62s {FL} {B(19, 1.5):.2f}s both">{P.status()}<div class="sbdark" style="animation:pm-fadein .4s {FL} {B(19, 1.5):.2f}s both">{P.status()}</div></div></div></div>{speedcard}{orbit}'
           + copy_top("Now Playing", "Pick up exactly where you left off", B(19), "copy-top hi"))

kar_lines = ["The door was locked from the inside.", "The key was still in the lock,", "and the window had been painted shut for years.",
             "So how did the murderer leave the room?", "That’s the question every locked-room story asks,", "and tonight, we’re going to answer it."]
step = BEAT * 1.2
kar = "".join(f'<div class="kl" style="animation:pm-hl {step:.3f}s ease-out {B(22, .5) + k * step:.2f}s both">{l}</div>' for k, l in enumerate(kar_lines))
caps = "".join(f'<span style="animation:pm-capwin {step:.3f}s linear {B(22, .5) + k * step:.2f}s both">{l}</span>' for k, l in enumerate(kar_lines))
s6 = scene(B(22), B(24), f'''<div class="kar"><div style="animation:pm-scrollk {B(24) - B(22):.2f}s {EASE} {B(22, .5):.2f}s both">{kar}</div></div>
  <div class="kick kar-k" style="{up(B(22), .8)}">Captions and transcripts</div>
  <div class="pos-r2">{shot("p_np", B(22, .1), dark=True, overlay=f'<div class="capover">{caps}</div>')}</div>''')

tlines = ["Welcome back to Late Jazz Notes.", "Tonight it’s one ballad, and a whole hour on it.", "Blue in Green, slowly, the way it was first played.",
          "Listen to how the piano leaves the space open…", "and how the trumpet walks into it."]
auto = f'''<div class="laptop lap2" style="{a("pm-phone", 1.3, B(24, .25))}"><div class="lscreen"><div class="win"><div class="wbar"><i></i><i></i><i></i><span>AfterhoursNex</span></div>
  <div class="acap"><div class="acap-l">{P.art(4, 300, 20, 96)}<b>Blue in green, slowly</b><span>Late Jazz Notes</span>
    <div class="arec">Auto captions: recognising speech</div><div class="bar wide"><i style="--a:.12;--b:.58;{a("pm-progress", 4.6, B(24, .6), "linear")}"></i></div>
    <div class="aw">Whisper · on this computer</div></div>
  <div class="acap-r"><div class="aright">Transcript</div>{"".join(f'<div class="tl" style="{up(B(24, 1) + k * BEAT, .7)}">{l}</div>' for k, l in enumerate(tlines))}</div></div></div></div><div class="lbase"></div></div>'''
s7 = scene(B(24), B(26), auto + copy_top("Auto captions", "Captions made on your computer", B(24), "copy-top s7c")
           + f'<div class="sub2" style="{up(B(25, 2))}">Whisper runs privately on Windows, macOS and Linux</div>')

badges = "".join(glass(*c) for c in [("headphones", "Headphone controls", 300, 330, B(26, 1)), ("lock", "Lock-screen controls", 1290, 360, B(26, 2)),
                                     ("car", "Car controls", 1330, 680, B(26, 3)), ("next", "Keeps going in the background", 220, 700, B(27, 0))])
s8 = scene(B(26), B(28), f'<div class="pos-c">{P.phone(lock_inner(B(26)), B(26, .1), "")}</div>{badges}' + copy_top("Android", "Keeps playing with the screen off", B(26), "copy-top hi"))

dark_phone = shot("p_listen_dark", B(28), dark=True)
light_phone = shot("p_listen_light", B(28))
s9 = scene(B(28), B(30), f'''<div class="pos-c">{dark_phone}</div><div class="pos-c wipe" style="animation:pm-wipe 1.6s {EASE} {B(28, 1.5):.2f}s both">{light_phone}</div>
  <div class="lbl" style="left:380px;{up(B(28, 2.5))}">Light</div><div class="lbl" style="left:1340px;{up(B(28, 3))}">Dark</div>''' + copy_top("Appearance", "Light or dark, as you like it", B(28), "copy-top hi"))

grid = "".join(f'<div class="tile" style="{up(B(30) + .6 + k * .06, .8)}">{P.art(k, 132, 12)}<b>{P.SHOWS[k % 8][0]}</b></div>' for k in range(10))
desk = f'''<div class="laptop" style="{a("pm-phone", 1.4, B(30, .2))}"><div class="lscreen"><div class="win shotwin"><div class="wbar light"><i></i><i></i><i></i><span>AfterhoursNex</span></div>
  <img src="shots/d_library.png"><img class="drill" src="shots/d_show.png" style="animation:pm-drill .6s {FL} {B(31):.2f}s both"></div></div><div class="lbase"></div></div>
  {shot("p_listen_light", B(30, .6), small=True)}'''
plats = "".join(f'<span style="animation:pm-lit .5s ease-out {B(30, k):.2f}s both">{n}</span>' for k, n in enumerate(["WINDOWS", "MACOS", "LINUX", "ANDROID"]))
s10 = scene(B(30), B(32), f'<div class="center-copy"><h2 style="{a("pm-titlein", 1.1, B(30), FL)}">One app. Four systems.</h2></div>{desk}<div class="plats">{plats}</div>', "s7")

bd = scene(B(32), B(34), f'''<div class="glow2" style="opacity:.7"></div><div class="bd">
  <div style="{a("pm-word", 1.4, B(32))}">How many times</div><div style="{a("pm-word", 1.4, B(32, 2))}">do I have left to see you</div><div style="{a("pm-word", 1.6, B(33))}">again in my life?</div></div>''')

fw = .82; F = P2.F; G = P2.G
fx0 = W / 2 - (G["after"]["width"] + 6 + G["nex"]["width"]) * F * fw / 2; fnx = fx0 + (G["after"]["width"] + 6) * F * fw; fb = 470
fin_after = "".join(f'<path d="{P2.gd("after", g, fx0, fb, F * fw)}"/>' for g in G["after"]["glyphs"])
fin_nex = "".join(f'<path d="{P2.gd("nex", g, fnx, fb, F * fw)}"/>' for g in G["nex"]["glyphs"])
final_word = (f'<svg class="fword" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><defs>'
              f'<linearGradient id="f3-sun" x1="{fnx:.0f}" y1="{fb - 140}" x2="{fnx + G["nex"]["width"] * F * fw:.0f}" y2="{fb + 14}" gradientUnits="userSpaceOnUse">'
              f'<stop offset="0" stop-color="#FEBB5F"/><stop offset=".35" stop-color="#F7926D"/><stop offset=".62" stop-color="#F37377"/><stop offset="1" stop-color="#B6448D"/></linearGradient>'
              f'<linearGradient id="f3-shine" x1="0" y1="0" x2="140" y2="0" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".5" stop-color="#fff" stop-opacity=".9"/><stop offset="1" stop-color="#fff" stop-opacity="0"/>'
              f'<animateTransform attributeName="gradientTransform" type="translate" values="{fnx - 200:.0f} 0; {fnx + 520:.0f} 0" begin="{B(34, 2):.3f}s" dur="1.4s" fill="freeze"/></linearGradient></defs>'
              f'<image href="data:image/png;base64,{P2.ICON}" x="{W / 2 - 85}" y="150" width="170" height="170"/>'
              f'<g fill="#F5EEF4">{fin_after}</g><g fill="url(#f3-sun)">{fin_nex}</g><g fill="url(#f3-shine)">{fin_nex}</g></svg>')
finale = scene(B(34), END + 1, f'''<div class="fin" style="{a("pm-end", 2.0, B(34), "cubic-bezier(.2,.7,.2,1)")}">{final_word}</div>
  <div class="fslogan" style="{up(B(35), 1.6)}">How many times do I have left to see you again in my life?</div>
  <div class="ffree" style="{up(B(36), 1.4)}">Free · private · no account needed</div>
  <div class="fplat" style="{up(B(36, 2), 1.4)}">WINDOWS · MACOS · LINUX · ANDROID</div>''', "s8")


# ---- the captions scene, rebuilt around the app's rolling captions ----
LY = ["The door was locked from the inside.", "The key was still in the lock,", "and the window had been painted shut for years.",
      "So how did the murderer leave the room?", "That’s the question every locked-room story asks,", "and tonight, we’re going to answer it."]
LY_T0, LY_STEP, LY_PITCH, LY_ACTIVE = B(22, .5), 2 * BEAT, 92, 4
LY_START, LY_LEN = B(22), B(24) - B(22) + .3
def _pct(t): return max(0.0, min(100.0, (t - LY_START) / LY_LEN * 100))
ly_css = "@keyframes ly-roll { 0% { transform: translateY(0); }"
for k in range(1, LY_ACTIVE):
    c = LY_T0 + k * LY_STEP
    ly_css += f" {_pct(c):.3f}% {{ transform: translateY({-(k - 1) * LY_PITCH}px); animation-timing-function: {FL}; }} {_pct(c + .68):.3f}% {{ transform: translateY({-k * LY_PITCH}px); }}"
ly_css += f" 100% {{ transform: translateY({-(LY_ACTIVE - 1) * LY_PITCH}px); }} }}\n"
DIM, ON = "rgba(255,255,255,.32)", "#fff"
for i in range(LY_ACTIVE):
    on, off = LY_T0 + i * LY_STEP, LY_T0 + (i + 1) * LY_STEP
    last = i == LY_ACTIVE - 1
    ly_css += (f"@keyframes ly-{i} {{ 0% {{ color: {DIM}; transform: scale(.975); }} {_pct(on):.3f}% {{ color: {DIM}; transform: scale(.975); }}"
               f" {_pct(on + .5):.3f}% {{ color: {ON}; transform: none; }}"
               + ("" if last else f" {_pct(off):.3f}% {{ color: {ON}; transform: none; }} {_pct(off + .5):.3f}% {{ color: {DIM}; transform: scale(.975); }}")
               + f" 100% {{ color: {ON if last else DIM}; transform: {'none' if last else 'scale(.975)'}; }} }}\n")
lines_html = "".join(f'<div class="ly" style="{f"animation:ly-{i} {LY_LEN:.2f}s linear {LY_START:.2f}s both" if i < LY_ACTIVE else ""}">{l}</div>' for i, l in enumerate(LY))
lyr_phone = f'''<div class="phone" style="{a("pm-phone", 1.1, B(22, .1))}"><div class="screen shot dk lyr">
  <img class="lyr-bg" src="shots/p_np.png"><div class="lyr-dim"></div>
  <div class="lyr-head">{P.art(0, 56, 10, 24)}<div><b>Listener questions</b><span>Shedunnit</span></div></div>
  <div class="lyr-win"><div class="lyr-list" style="animation:ly-roll {LY_LEN:.2f}s linear {LY_START:.2f}s both">{lines_html}</div></div>
  <div class="lyr-bar"><i style="--a:.31;--b:.36;{a("pm-progress", 5, B(22), "linear")}"></i></div>
  <div class="lyr-ctl"><span>15</span><b></b><span>30</span></div>
  <div class="sbdark">{P.status()}</div></div></div>'''
s6 = scene(B(22), B(24), f'''<div class="pos-r2">{lyr_phone}</div>
  {P.copy("Captions that roll", "Read along,<br>line by line", "The spoken line lights up and glides into place. Tap any line to jump there. Shows’ transcripts, your caption files, or captions made on your computer.", B(22, .25))}''')

MOTION_CSS = """
@keyframes pm-scene3 {
  0% { opacity: 0; transform: translateY(26px) scale(.985); filter: blur(8px); animation-timing-function: cubic-bezier(.1,.9,.2,1); }
  8% { opacity: 1; transform: none; filter: none; }
  91% { opacity: 1; transform: none; filter: none; animation-timing-function: cubic-bezier(.9,.1,1,.2); }
  100% { opacity: 0; transform: scale(1.025); filter: blur(10px); } }
.cam { position: absolute; inset: 0; transform-origin: 50% 46%; }
@keyframes pm-cam { from { transform: scale(1); } to { transform: scale(1.035); } }
@keyframes pm-titlein { from { opacity: 0; transform: translateY(.5em); clip-path: inset(0 0 100% 0); } 55% { opacity: 1; } to { opacity: 1; transform: none; clip-path: inset(-10% -5% -25% -5%); } }
@keyframes pm-fadein { from { opacity: 0; } to { opacity: 1; } }
@keyframes pm-scrolltall { from { transform: none; } to { transform: translateY(var(--to)); } }
@keyframes pm-bandup { from { opacity: 0; transform: translateY(54px); } to { opacity: 1; transform: none; } }
@keyframes pm-nprise { from { opacity: 0; transform: translateY(40px) scale(.985); } to { opacity: 1; transform: none; } }
@keyframes pm-drill { from { opacity: 0; transform: scale(.965); } to { opacity: 1; transform: none; } }
@keyframes pm-scmove { from { transform: none; } to { transform: translateY(46px); } }
.screen.shot > img.tall { height: auto; bottom: auto; object-fit: fill; }
.screen.shot > img.tabbar { top: auto; bottom: 0; height: auto; z-index: 2; }
.screen.shot > img.band { clip-path: inset(83.5% 0 8.2% 0); z-index: 1; }
.screen.shot > img[style*="pm-nprise"], .screen.shot > img[style*="pm-fadein"] { opacity: 0; }
.sbdark { position: absolute; inset: 0 0 auto 0; z-index: 3; opacity: 0; } .lyr .sbdark { opacity: 1; }
.sbdark .sb { position: absolute; left: 0; right: 0; top: 0; padding: 18px 34px 8px; color: #fff; } .sbdark .sb-r i { background: #fff; }
.win.shotwin { position: relative; } .win.shotwin > img.drill { position: absolute; left: 0; top: 40px; clip-path: inset(0 0 0 17.3%); transform-origin: 58% 0; opacity: 0; }
.speedcard { position: absolute; left: 236px; top: 236px; width: 250px; padding: 8px 0 10px; border-radius: 18px; background: rgba(30,22,52,.62);
  border: 1px solid rgba(255,255,255,.16); backdrop-filter: blur(14px); box-shadow: 0 24px 60px rgba(0,0,0,.35); color: #F5EEF4; opacity: 0; }
.sc-h { padding: 8px 20px 8px; font-size: 14px; font-weight: 600; color: rgba(255,255,255,.6); }
.sc-r { position: relative; z-index: 1; display: flex; align-items: baseline; gap: 10px; height: 46px; line-height: 46px; padding: 0 20px; font-size: 21px; font-weight: 600; }
.sc-r em { font-style: normal; font-size: 15px; font-weight: 500; color: rgba(255,255,255,.55); }
.sc-hl { position: absolute; left: 8px; right: 8px; top: 92px; height: 46px; border-radius: 12px; background: rgba(255,110,159,.28); }
.sc-ck { position: absolute; right: 22px; top: 92px; height: 46px; line-height: 46px; font-style: normal; font-size: 20px; font-weight: 700; color: #FF6E9F; z-index: 2; }
.lyr .lyr-bg { position: absolute; inset: -10%; width: 120%; height: 120%; object-fit: cover; filter: blur(30px) saturate(1.25) brightness(.62); }
.lyr .lyr-dim { position: absolute; inset: 0; background: linear-gradient(rgba(10,8,20,.25), rgba(10,8,20,.55)); }
.lyr-head { position: absolute; left: 30px; right: 30px; top: 78px; display: flex; align-items: center; gap: 14px; color: #fff; z-index: 2; }
.lyr-head b { display: block; font-size: 19px; } .lyr-head span { font-size: 15px; color: rgba(255,255,255,.65); }
.lyr-win { position: absolute; left: 30px; right: 26px; top: 168px; height: 560px; overflow: hidden; z-index: 2;
  -webkit-mask-image: linear-gradient(transparent, #000 14%, #000 72%, transparent); }
.lyr-list { padding-top: 120px; }
.ly { height: 74px; margin-bottom: 18px; font-size: 25px; line-height: 1.24; font-weight: 700; letter-spacing: -.015em; color: rgba(255,255,255,.32);
  transform-origin: 0 50%; transform: scale(.975); }
.lyr-bar { position: absolute; left: 30px; right: 30px; bottom: 132px; height: 4px; border-radius: 4px; background: rgba(255,255,255,.25); z-index: 2; }
.lyr-bar i { position: absolute; inset: 0 auto 0 0; border-radius: 4px; background: #fff; }
.lyr-ctl { position: absolute; left: 0; right: 0; bottom: 48px; display: flex; justify-content: center; align-items: center; gap: 54px; z-index: 2; }
.lyr-ctl span { width: 40px; height: 40px; border-radius: 50%; border: 3px solid #fff; color: #fff; font-size: 13px; font-weight: 800; display: flex; align-items: center; justify-content: center; }
.lyr-ctl b { width: 70px; height: 70px; border-radius: 50%; background: #fff; position: relative; }
.lyr-ctl b::before, .lyr-ctl b::after { content: ""; position: absolute; top: 22px; width: 8px; height: 26px; border-radius: 3px; background: #18181A; }
.lyr-ctl b::before { left: 24px; } .lyr-ctl b::after { right: 24px; }
"""

CSS3 = """
.stars { animation: pm-drift 92.5s linear 0s both; }
.pos-r .phone { left: 1250px; top: 75px; } .pos-l2 .phone { left: 170px; top: 75px; } .pos-c { position: absolute; inset: 0; transform: scale(.84); transform-origin: 960px 1062px; } .pos-c .phone { left: 732px; top: 75px; } .pos-r2 .phone { left: 1260px; top: 75px; }
.tilt { position: absolute; inset: 0; transform-origin: 1480px 540px; }
@keyframes pm-tilt { from { opacity: 0; transform: perspective(1800px) rotateY(-32deg) rotateX(8deg) translateX(140px); } to { opacity: 1; transform: perspective(1800px) rotateY(-14deg) rotateX(3deg); } }
.wall { position: absolute; left: -320px; top: -300px; width: 2700px; height: 1800px; transform: perspective(1500px) rotateX(32deg) rotateZ(-16deg); }
.wall-in { position: absolute; left: 0; top: 0; display: grid; grid-template-columns: repeat(10, 180px); gap: 30px; padding: 140px; }
@keyframes pm-wall { from { translate: 0 0; } to { translate: -280px -130px; } }
.wt { opacity: 0; } @keyframes pm-wave { from { opacity: 0; transform: translateY(46px) scale(.88); } to { opacity: .82; transform: none; } }
.shade-l { position: absolute; inset: 0; background: linear-gradient(90deg, rgba(17,12,43,.94) 0%, rgba(17,12,43,.82) 36%, rgba(17,12,43,.2) 66%, rgba(17,12,43,.4) 100%); }
.fly { position: absolute; width: 280px; opacity: 0; } .flyc b { display: block; font-size: 24px; margin-top: 16px; } .flyc span { font-size: 18px; color: #C9BFD8; }
.flyc .bar { margin-top: 14px; height: 6px; } .flyc em { display: block; font-style: normal; margin-top: 8px; font-size: 16px; color: #F7926D; }
@keyframes pm-fly { from { opacity: 0; transform: translate(-620px, 140px) rotateY(60deg) scale(.55); } to { opacity: 1; transform: none; } }
.copy-top, .copy-tr { position: absolute; left: 0; right: 0; top: 70px; text-align: center; } .copy-tr { left: 820px; right: 80px; top: 120px; text-align: left; }
.copy-top > *, .copy-tr > * { opacity: 0; } .copy-top h2, .copy-tr h2 { font-family: NexSerif; font-weight: 400; font-size: 66px; margin: 10px 0 0; color: #F5EEF4; }
.copy-top.hi { top: 40px; } .copy-top.hi h2 { font-size: 54px; } .copy-top.s7c { top: 50px; } .copy-top.s7c h2 { font-size: 58px; }
.fan { position: absolute; inset: 0; transform-origin: 960px 1500px; } .fan .phone { left: 732px; top: 210px; }
@keyframes pm-fanL { from { transform: rotate(0); } to { transform: rotate(-19deg) translateX(-30px); } }
@keyframes pm-fanR { from { transform: rotate(0); } to { transform: rotate(19deg) translateX(30px); } }
@keyframes pm-fanC { from { transform: translateY(50px); } to { transform: none; } }
.kick.center { position: absolute; left: 0; right: 0; top: 140px; text-align: center; opacity: 0; }
.bigq { position: absolute; left: 0; right: 0; top: 200px; display: flex; justify-content: center; align-items: center; gap: 26px; font-family: NexSerif; font-style: italic; font-size: 176px; line-height: 1.2; color: #F5EEF4; }
.bq-ic { display: flex; color: #F7926D; } .caret.big { width: 4px; height: 150px; margin-left: -14px; }
.rcards { position: absolute; left: 0; right: 0; top: 520px; display: flex; justify-content: center; gap: 44px; }
.rc { width: 220px; opacity: 0; display: flex; flex-direction: column; } .rc b { font-size: 21px; margin-top: 14px; } .rc span { font-size: 16px; color: #C9BFD8; } .rc .btn { margin-top: 14px; align-self: flex-start; min-width: 140px; }
.foot { position: absolute; left: 0; right: 0; bottom: 70px; text-align: center; font-family: NexSerif; font-style: italic; font-size: 32px; color: #B7A9D6; opacity: 0; }
.backart { position: absolute; inset: -80px; background: radial-gradient(600px 500px at 30% 40%, rgba(254,187,95,.55), transparent 70%), radial-gradient(700px 600px at 72% 60%, rgba(182,68,141,.6), transparent 70%), linear-gradient(150deg, #2A1C58, #140F33); filter: blur(40px); opacity: .7; }
@keyframes pm-ken2 { from { transform: scale(1); } to { transform: scale(1.08); } }
.chipx { position: absolute; opacity: 0; }
.chipf { display: flex; align-items: center; gap: 12px; padding: 16px 26px; border-radius: 40px; background: rgba(30,22,52,.6); border: 1px solid rgba(255,255,255,.16); backdrop-filter: blur(14px);
         color: #F5EEF4; font-size: 22px; font-weight: 600; box-shadow: 0 24px 60px rgba(0,0,0,.35); white-space: nowrap; } .chipf .ic { color: #FEBB5F; }
@keyframes pm-pop2 { from { opacity: 0; transform: scale(.7) translateY(24px); } to { opacity: 1; transform: none; } }
.kar { position: absolute; left: 170px; top: 300px; width: 900px; height: 600px; overflow: hidden; -webkit-mask-image: linear-gradient(transparent, #000 14%, #000 80%, transparent); }
.kl { font-family: NexSerif; font-size: 50px; line-height: 1.3; padding: 10px 0; color: #B7A9D6; opacity: .35; }
.kar-k { position: absolute; left: 170px; top: 210px; opacity: 0; }
@keyframes pm-hl { 0% { opacity: .35; color: #B7A9D6; } 16% { opacity: 1; color: #FFF4E6; } 80% { opacity: 1; color: #FFF4E6; } 100% { opacity: .5; color: #CDB9D9; } }
@keyframes pm-scrollk { from { transform: translateY(40px); } to { transform: translateY(-300px); } }
.capline { display: grid; margin: 22px 8px 0; min-height: 64px; text-align: center; font-size: 18px; font-weight: 600; color: #FFF4E6; }
.capline span { grid-area: 1 / 1; opacity: 0; } @keyframes pm-capwin { 0% { opacity: 0; } 12% { opacity: 1; } 88% { opacity: 1; } 100% { opacity: 0; } }
@keyframes pm-knob3 { from { left: calc(30% - 9px); } to { left: calc(42% - 9px); } }
.lap2 { position: absolute; left: 290px; top: 230px; width: 1340px; opacity: 0; } .lap2 .lscreen { height: 700px; }
.acap { display: flex; height: calc(100% - 40px); } .acap-l { width: 400px; padding: 30px; border-right: 1px solid #2A292C; display: flex; flex-direction: column; }
.acap-l b { font-size: 24px; margin-top: 18px; } .acap-l span { font-size: 17px; color: #FF6E9F; margin-top: 4px; }
.arec { margin-top: 26px; font-size: 15px; color: #B9B3B8; } .bar.wide { height: 6px; margin-top: 10px; } .aw { margin-top: 18px; font-size: 14px; color: #FEBB5F; letter-spacing: .5px; }
.acap-r { flex: 1; padding: 28px 34px; } .aright { font-size: 26px; font-weight: 700; margin-bottom: 12px; } .tl { font-size: 21px; line-height: 1.5; padding: 10px 14px; border-radius: 10px; opacity: 0; }
.tl:last-child { background: #3A2A31; }
.sub2 { position: absolute; left: 0; right: 0; bottom: 34px; text-align: center; font-family: NexSerif; font-style: italic; font-size: 28px; color: #B7A9D6; opacity: 0; }
.wipe { position: absolute; inset: 0; } @keyframes pm-wipe { from { clip-path: polygon(0 0, 0 0, 0 100%, 0 100%); } to { clip-path: polygon(0 0, 52% 0, 48% 100%, 0 100%); } }
.lbl { position: absolute; top: 480px; font-family: NexSerif; font-style: italic; font-size: 48px; color: #F5EEF4; opacity: 0; }
.screen.light { background: #FBF8F9; color: #1F1A1D; } .screen.light .ps, .screen.light .desc, .screen.light .card span { color: #5B5559; }
.screen.light .meta { color: #C8326C; } .screen.light .ep { border-bottom-color: #ECE5E8; } .screen.light .pb { background: #F3E2E8; color: #6B1A3F; }
.screen.light .mini { background: #F1ECEE; } .screen.light .mini .ic { color: #1F1A1D; } .screen.light .mt span { color: #5B5559; } .screen.light .nav { background: #F4EFF1; }
.screen.light .nv { color: #5B5559; } .screen.light .nv.on { color: #1F1A1D; } .screen.light .nv.on .pill { background: #FFD9E3; color: #3E0020; }
.screen.light .sb-r i { background: #1F1A1D; } .screen.light .bar { background: #E4DCE0; } .screen.light .bar i, .screen.light .mprog { background: #C8326C; }
.plats { position: absolute; left: 0; right: 0; bottom: 46px; display: flex; justify-content: center; gap: 80px; font-family: NexSerif; font-size: 32px; letter-spacing: 5px; color: #6E6290; }
@keyframes pm-lit { from { color: #6E6290; text-shadow: none; } to { color: #FFF1DE; text-shadow: 0 0 26px rgba(254,187,95,.65); } }
.s7 .laptop { top: 250px; } .s7 .phone.small { top: 290px; }
.bd { position: absolute; left: 0; right: 0; top: 300px; text-align: center; font-family: NexSerif; font-style: italic; font-size: 96px; line-height: 1.25; color: #F5EEF4; } .bd div { opacity: 0; }
.screen.shot { padding: 0; background: #FFFFFF; } .screen.shot.dk { background: #18181A; }
.screen.shot > img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; object-position: top center; }
.screen.shot .sb { position: absolute; left: 0; right: 0; top: 0; padding: 18px 34px 8px; z-index: 2; color: #1D1D1F; }
.screen.shot .sb-r i { background: #1D1D1F; } .screen.shot.dk .sb { color: #F5F5F7; } .screen.shot.dk .sb-r i { background: #F5F5F7; }
.capover { position: absolute; left: 30px; right: 30px; top: 568px; z-index: 3; display: grid; text-align: left; font-size: 19px; line-height: 1.35; font-weight: 600; color: #fff; }
.capover span { grid-area: 1 / 1; opacity: 0; }
.win.shotwin { background: #FFFFFF; } .win.shotwin > img { display: block; width: 100%; height: calc(100% - 40px); object-fit: cover; object-position: top left; }
.wbar.light { background: #F2F2F5; color: #6E6E73; }
.lap2 .win { background: #FFFFFF; color: #1D1D1F; } .lap2 .wbar { background: #F2F2F5; color: #6E6E73; }
.lap2 .acap-l { border-right: 1px solid rgba(60,60,67,.12); } .lap2 .acap-l span { color: #C8326C; } .lap2 .arec { color: #6E6E73; }
.lap2 .aw { color: #C8326C; } .lap2 .tl:last-child { background: rgba(200,50,108,.1); } .lap2 .aright { color: #1D1D1F; }
.lap2 .bar { background: rgba(118,118,128,.18); } .lap2 .bar i { background: #C8326C; }
.fadeout3 { position: absolute; inset: 0; background: #000; opacity: 0; animation: pm-fade 1s linear 91.5s both; }
"""
page = f"""<!doctype html><html><head><meta charset="utf-8"><title>AfterhoursNex film</title><style>{P.CSS}{P2.CSS2}{CSS3}{MOTION_CSS}{ly_css}</style></head>
<body><div class="bg"></div><div class="dusk"></div>{P2.starfield}{title}{slogan}{s1}{s2}{s3}{s4}{s5}{s6}{s7}{s8}{s9}{s10}{bd}{finale}<div class="fadein"></div><div class="fadeout3"></div>
<script>{TITLE_JS}
window.seek = t => {{
  for (const s of document.querySelectorAll('svg')) if (s.pauseAnimations) {{ s.pauseAnimations(); s.setCurrentTime(t); }}
  for (const an of document.getAnimations()) {{ an.pause(); an.currentTime = t * 1000; }}
  window.renderTitle(t);
}};
window.LENGTH = {END};
</script></body></html>"""
open("promo3.html", "w", encoding="utf-8").write(page)
print(f"promo3.html: {len(page) // 1024} KB; 37 bars, {END} s")

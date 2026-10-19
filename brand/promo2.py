"""The longer AfterhoursNex film (86 s, 60 fps): a written title with a powerful x, the slogan, six feature scenes and a finale."""
import json, math, random
import promo as P                                     # the device mock-ups and styles from the first film
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

G = json.load(open("glyphs.json"))
REG, ITA = (TTFont("../shared/src/commonMain/composeResources/font/liberation_serif_%s.ttf" % s) for s in ("regular", "italic"))
FONT = {"after": REG, "nex": ITA}
W, H, END = 1920, 1080, 86.0
S = 228; F = S / G["after"]["size"]
X0 = W / 2 - (G["after"]["width"] + 6 + G["nex"]["width"]) * F / 2
NEX_X = X0 + (G["after"]["width"] + 6) * F
BASE = 650

def gd(run, g, x0, base, f=F):
    font = FONT[run]; gs = font.getGlyphSet(); name = font.getBestCmap()[ord(g["ch"])]
    k = G[run]["scale"] * f
    pen = SVGPathPen(gs, ntos=lambda v: ("%.2f" % v).rstrip("0").rstrip("."))
    gs[name].draw(TransformPen(pen, (k, 0, 0, -k, x0 + g["x"] * f, base)))
    return pen.getCommands()

# ---- the x: box, pen paths (following the italic x's real strokes) and the moment the strokes cross ----
xg = G["nex"]["glyphs"][2]; kx = G["nex"]["scale"] * F
bx0, by0, bx1, by1 = xg["bounds"]
XL, XR = NEX_X + xg["x"] * F + bx0 * kx, NEX_X + xg["x"] * F + bx1 * kx
XT, XB = BASE - by1 * kx, BASE - by0 * kx
XW, XH = XR - XL, XB - XT
def pt(fx, fy): return (XL + fx * XW, XT + fy * XH)
def fmt(*pts): return " ".join("%.1f %.1f" % p for p in pts)
BOLD = "M %s C %s" % (fmt(pt(.29, .02)), fmt(pt(.45, .33), pt(.63, .66), pt(.80, .98)))
BOLD_TRAIL = BOLD + " L %s" % fmt(pt(.96, 1.30))                                      # the follow-through past the foot
FINE = "M %s Q %s L %s Q %s" % (fmt(pt(.83, .035)), fmt(pt(1.0, .07), pt(.90, .20)), fmt(pt(.15, .86)), fmt(pt(.04, .98), pt(.20, .985)))
CROSS = pt(.55, .51)
def ease(u, a=.7, b=.15):                              # cubic-bezier(a, 0, b, 1): slow wind-up, a fast strike, a soft landing
    lo, hi = 0.0, 1.0
    for _ in range(40):
        m = (lo + hi) / 2
        x = 3 * (1 - m) ** 2 * m * a + 3 * (1 - m) * m * m * b + m ** 3
        lo, hi = (m, hi) if x < u else (lo, m)
    m = (lo + hi) / 2
    return 3 * (1 - m) * m * m + m ** 3
def fine_length_fraction_at_cross():                   # how far along the fine stroke the crossing lies
    def quad(p0, p1, p2, t): return tuple((1 - t) ** 2 * p0[i] + 2 * (1 - t) * t * p1[i] + t * t * p2[i] for i in (0, 1))
    pts = [quad(pt(.83, .035), pt(1.0, .07), pt(.90, .20), i / 60) for i in range(61)]
    pts += [tuple(pt(.90, .20)[i] + (pt(.15, .86)[i] - pt(.90, .20)[i]) * j / 60 for i in (0, 1)) for j in range(1, 61)]
    pts += [quad(pt(.15, .86), pt(.04, .98), pt(.20, .985), i / 60) for i in range(1, 61)]
    seg = [math.dist(pts[i], pts[i + 1]) for i in range(len(pts) - 1)]
    best = min(range(len(pts)), key=lambda i: math.dist(pts[i], CROSS))
    return sum(seg[:best]) / sum(seg)
TX = 5.35                                              # the x begins
T_BOLD, D_BOLD = TX + .45, .30                         # the bold stroke
T_FINE, D_FINE = TX + 1.12, .30                        # the fine stroke
c = fine_length_fraction_at_cross()
lo, hi = 0.0, 1.0
for _ in range(40):
    m = (lo + hi) / 2
    lo, hi = (m, hi) if ease(m, .55, .2) < c else (lo, m)
T_IMPACT = T_FINE + D_FINE * (lo + hi) / 2
json.dump({"TX": TX, "T_BOLD": T_BOLD, "T_FINE": T_FINE, "T_IMPACT": T_IMPACT, "END": END}, open("timing.json", "w"))

rnd = random.Random(5)
SPARKS = []
for i in range(22):
    ang = rnd.uniform(0, 2 * math.pi) if i % 3 else math.atan2(pt(.15, .86)[1] - pt(.90, .20)[1], pt(.15, .86)[0] - pt(.90, .20)[0]) + rnd.uniform(-.6, .6)
    SPARKS.append({"a": ang, "v": rnd.uniform(260, 620), "len": rnd.uniform(10, 30), "life": rnd.uniform(.45, .95), "w": rnd.uniform(1.4, 2.8), "d": rnd.uniform(0, .05)})

after = "".join(f'<path class="L" data-cx="{X0 + (g["x"] + G["after"]["glyphs"][min(i + 1, 9)]["x"] if i < 9 else g["x"] + 40) * F / 2:.1f}" d="{gd("after", g, X0, BASE)}"/>'
                for i, g in enumerate(G["after"]["glyphs"]))
ne = "".join(f'<path class="L" data-cx="{NEX_X + (g["x"] + 40) * F:.1f}" d="{gd("nex", g, NEX_X, BASE)}"/>' for g in G["nex"]["glyphs"][:2])
x_d = gd("nex", xg, NEX_X, BASE)
nex_all = "".join(f'<path d="{gd("nex", g, NEX_X, BASE)}"/>' for g in G["nex"]["glyphs"])
ICON = P.b64("../shared/src/commonMain/composeResources/drawable/afterhours_icon.png")
ICX, ICY, IS = W / 2, 330, 210
title = f'''<svg id="title" class="title" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
  <linearGradient id="t-sunset" x1="{NEX_X:.0f}" y1="{BASE - 160}" x2="{NEX_X + G["nex"]["width"] * F:.0f}" y2="{BASE + 16}" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="#FEBB5F"/><stop offset=".35" stop-color="#F7926D"/><stop offset=".62" stop-color="#F37377"/><stop offset="1" stop-color="#B6448D"/></linearGradient>
  <linearGradient id="t-write" x1="0" y1="0" x2="60" y2="0" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#fff"/><stop offset="1" stop-color="#000"/></linearGradient>
  <mask id="t-wmask" maskUnits="userSpaceOnUse" x="0" y="0" width="{W}" height="{H}"><rect width="{W}" height="{H}" fill="url(#t-write)"/></mask>
  <mask id="t-xmask" maskUnits="userSpaceOnUse" x="0" y="0" width="{W}" height="{H}">
    <path id="t-bold" d="{BOLD}" fill="none" stroke="#fff" stroke-width="{XW * .27:.1f}" stroke-linecap="round"/>
    <path id="t-fine" d="{FINE}" fill="none" stroke="#fff" stroke-width="{XW * .17:.1f}" stroke-linecap="round"/></mask>
  <linearGradient id="t-shine" x1="0" y1="0" x2="150" y2="0" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".5" stop-color="#fff" stop-opacity=".95"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
  <radialGradient id="t-halog"><stop offset="0" stop-color="#F7926D" stop-opacity=".42"/><stop offset=".5" stop-color="#CC5286" stop-opacity=".12"/><stop offset="1" stop-color="#CC5286" stop-opacity="0"/></radialGradient>
  <radialGradient id="t-tipg"><stop offset="0" stop-color="#FFFDF8"/><stop offset=".25" stop-color="#FFE7B8" stop-opacity=".95"/><stop offset=".6" stop-color="#FEBB5F" stop-opacity=".35"/><stop offset="1" stop-color="#FEBB5F" stop-opacity="0"/></radialGradient>
  <radialGradient id="t-flashg"><stop offset="0" stop-color="#FFFFFF" stop-opacity="1"/><stop offset=".16" stop-color="#FFFFFF" stop-opacity=".85"/><stop offset=".42" stop-color="#FFE3B0" stop-opacity=".36"/><stop offset="1" stop-color="#F37377" stop-opacity="0"/></radialGradient>
  <radialGradient id="t-frontg"><stop offset="0" stop-color="#FFF4E6" stop-opacity=".55"/><stop offset="1" stop-color="#FFF4E6" stop-opacity="0"/></radialGradient>
  <linearGradient id="t-rule" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#CC5286" stop-opacity="0"/><stop offset=".5" stop-color="#F7926D"/><stop offset="1" stop-color="#CC5286" stop-opacity="0"/></linearGradient>
  <filter id="t-blur" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="6"/></filter>
  <filter id="t-blur2" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="14"/></filter>
  <clipPath id="t-iconclip"><rect x="{ICX - IS / 2}" y="{ICY - IS / 2}" width="{IS}" height="{IS}" rx="{IS * .22:.0f}"/></clipPath>
</defs>
<g id="t-cam">
  <circle id="t-halo" cx="{ICX}" cy="{ICY}" r="300" fill="url(#t-halog)" opacity="0"/>
  <rect id="t-ring" x="{ICX - IS / 2}" y="{ICY - IS / 2}" width="{IS}" height="{IS}" rx="{IS * .24:.0f}" fill="none" stroke="#F7926D" stroke-width="1.6" opacity="0"/>
  <g id="t-icon" opacity="0"><image href="data:image/png;base64,{ICON}" x="{ICX - IS / 2}" y="{ICY - IS / 2}" width="{IS}" height="{IS}"/>
    <rect id="t-iconshine" clip-path="url(#t-iconclip)" x="{ICX - IS / 2}" y="{ICY - IS / 2}" width="{IS}" height="{IS}" fill="url(#t-shine)" opacity="0"/></g>
  <g id="t-word">
    <ellipse id="t-front" cx="0" cy="{BASE - 80}" rx="46" ry="150" fill="url(#t-frontg)" opacity="0"/>
    <g mask="url(#t-wmask)"><g fill="#F5EEF4">{after}</g><g fill="url(#t-sunset)">{ne}</g></g>
    <g mask="url(#t-xmask)"><g id="t-xglow" filter="url(#t-blur)" opacity="0" fill="#FFD9A0"><path d="{x_d}"/></g></g>
    <g id="t-x" mask="url(#t-xmask)" fill="url(#t-sunset)"><path d="{x_d}"/></g>
    <g id="t-nexshine" fill="url(#t-shine)" opacity="0">{nex_all}</g>
  </g>
  <path id="t-trail" d="{BOLD_TRAIL}" fill="none" stroke="#FFE3B0" stroke-width="7" stroke-linecap="round" filter="url(#t-blur)" opacity="0"/>
  <path id="t-trail2" d="{FINE}" fill="none" stroke="#FFE3B0" stroke-width="5" stroke-linecap="round" filter="url(#t-blur)" opacity="0"/>
  <circle id="t-tip" r="34" fill="url(#t-tipg)" opacity="0"/>
  <circle id="t-flash" cx="{CROSS[0]:.1f}" cy="{CROSS[1]:.1f}" r="240" fill="url(#t-flashg)" opacity="0"/>
  <circle id="t-wave" cx="{CROSS[0]:.1f}" cy="{CROSS[1]:.1f}" r="10" fill="none" stroke="#FEBB5F" opacity="0"/>
  <circle id="t-wave2" cx="{CROSS[0]:.1f}" cy="{CROSS[1]:.1f}" r="10" fill="none" stroke="#F37377" opacity="0"/>
  <g id="t-sparks" stroke-linecap="round">{"".join(f'<line stroke="{"#FFF6E8" if i % 2 else "#FEBB5F"}" opacity="0"/>' for i in range(len(SPARKS)))}</g>
  <rect id="t-ruleL" y="{BASE + 58}" height="1.6" fill="url(#t-rule)" width="0"/><rect id="t-ruleR" y="{BASE + 58}" height="1.6" fill="url(#t-rule)" width="0"/>
  <rect id="t-gem" width="11" height="11" fill="#F7926D" opacity="0"/>
</g></svg>'''

TITLE_JS = f"""
const T = {json.dumps({"TX": TX, "T_BOLD": T_BOLD, "D_BOLD": D_BOLD, "T_FINE": T_FINE, "D_FINE": D_FINE, "T_IMPACT": T_IMPACT,
                        "X0": X0, "NEX_X": NEX_X, "XL": XL, "BASE": BASE, "CROSS": CROSS, "ICX": ICX, "ICY": ICY, "IS": IS, "W": W})};
const SPARKS = {json.dumps(SPARKS)};
const $ = id => document.getElementById(id);
const clamp = (v, a = 0, b = 1) => Math.min(b, Math.max(a, v));
const smooth = u => u * u * (3 - 2 * u);
const outCubic = u => 1 - Math.pow(1 - u, 3);
const outBack = u => {{ const s = 1.9; u -= 1; return u * u * ((s + 1) * u + s) + 1; }};
function bezier(a, b) {{ return u => {{ let lo = 0, hi = 1; for (let i = 0; i < 30; i++) {{ const m = (lo + hi) / 2;
  const x = 3 * (1 - m) * (1 - m) * m * a + 3 * (1 - m) * m * m * b + m * m * m; if (x < u) lo = m; else hi = m; }}
  const m = (lo + hi) / 2; return 3 * (1 - m) * m * m + m * m * m; }}; }}
const strike = bezier(.7, .15), cut = bezier(.55, .2);
const bold = $('t-bold'), fine = $('t-fine'), trail = $('t-trail'), trail2 = $('t-trail2');
const LB = bold.getTotalLength(), LF = fine.getTotalLength(), LT = trail.getTotalLength();
bold.setAttribute('stroke-dasharray', LB + ' ' + LB); fine.setAttribute('stroke-dasharray', LF + ' ' + LF);
const letters = [...document.querySelectorAll('#title .L')];
function seg(path, total, a, b) {{ path.setAttribute('stroke-dasharray', `0 ${{Math.max(0, a)}} ${{Math.max(0, b - a)}} ${{total + 10}}`); }}
window.renderTitle = t => {{
  // the icon rises, the halo blooms, a ring of light passes
  const ic = outCubic(clamp((t - .6) / 1.8));
  $('t-icon').setAttribute('opacity', ic);
  $('t-icon').setAttribute('transform', `translate(0 ${{(1 - ic) * 40}}) translate(${{T.ICX}} ${{T.ICY}}) scale(${{.9 + .1 * ic}}) translate(${{-T.ICX}} ${{-T.ICY}})`);
  $('t-halo').setAttribute('opacity', smooth(clamp((t - .8) / 1.8)) * (.85 + .15 * Math.sin(t * 1.3)));
  const r = clamp((t - 1.9) / 2.2); $('t-ring').setAttribute('opacity', r > 0 && r < 1 ? .65 * (1 - r) : 0);
  $('t-ring').setAttribute('transform', `translate(${{T.ICX}} ${{T.ICY}}) scale(${{1 + .6 * outCubic(r)}}) translate(${{-T.ICX}} ${{-T.ICY}})`);
  const sh = clamp((t - 2.0) / 1.2); $('t-iconshine').setAttribute('opacity', sh > 0 && sh < 1 ? .55 : 0);
  $('t-iconshine').setAttribute('x', T.ICX - T.IS * 1.2 + sh * T.IS * 2.1);
  // "Afterhours" and "Ne" are written by a flowing front of light
  const wp = clamp((t - 2.2) / 3.0), front = T.X0 - 70 + (T.XL - 6 - (T.X0 - 70)) * (wp < .5 ? 4 * wp * wp * wp : 1 - Math.pow(-2 * wp + 2, 3) / 2);
  $('t-write').setAttribute('x1', front - 60); $('t-write').setAttribute('x2', front);
  $('t-front').setAttribute('cx', front); $('t-front').setAttribute('opacity', wp > 0 && wp < 1 ? Math.sin(Math.PI * wp) * .9 : 0);
  for (const l of letters) {{ const p = outCubic(clamp((front - +l.dataset.cx) / 190)); l.setAttribute('transform', `translate(0 ${{(1 - p) * 20}})`); }}
  // the x: gathering, the bold strike, the follow-through, then the fine cut and the impact
  const tip = $('t-tip');
  let tipOn = 0, tx = 0, ty = 0, tipScale = 1;
  const gather = clamp((t - T.TX) / .45);
  const pb = strike(clamp((t - T.T_BOLD) / T.D_BOLD)), pf = cut(clamp((t - T.T_FINE) / T.D_FINE));
  bold.setAttribute('stroke-dashoffset', LB * (1 - pb)); fine.setAttribute('stroke-dashoffset', LF * (1 - pf));
  if (t >= T.TX && t < T.T_BOLD) {{
    const s = bold.getPointAtLength(0), d = bold.getPointAtLength(6), back = smooth(gather) * 12;
    const ux = (s.x - d.x) / 6, uy = (s.y - d.y) / 6; tx = s.x + ux * back; ty = s.y + uy * back;
    tipOn = smooth(gather); tipScale = .5 + .7 * smooth(gather) + .12 * Math.sin(gather * 18) * gather;
  }} else if (t >= T.T_BOLD && t < T.T_BOLD + T.D_BOLD + .06) {{
    const q = bold.getPointAtLength(LB * pb); tx = q.x; ty = q.y; tipOn = 1; tipScale = 1.15;
  }} else if (t >= T.T_FINE - .2 && t < T.T_FINE + T.D_FINE + .25) {{
    const g2 = clamp((t - (T.T_FINE - .2)) / .2), q = fine.getPointAtLength(LF * pf); tx = q.x; ty = q.y;
    tipOn = t < T.T_FINE ? smooth(g2) : 1 - clamp((t - (T.T_FINE + T.D_FINE)) / .25); tipScale = t < T.T_FINE ? .6 + .5 * g2 : 1.05;
  }}
  tip.setAttribute('opacity', tipOn); tip.setAttribute('cx', tx); tip.setAttribute('cy', ty); tip.setAttribute('r', 34 * tipScale);
  const fb = clamp((t - T.T_BOLD) / (T.D_BOLD + .45));   // the bold stroke's streak, carried past the foot
  if (fb > 0 && fb < 1) {{ const head = LT * Math.min(1, pb * (LB / LT) + clamp((t - T.T_BOLD - T.D_BOLD) / .4) * (1 - LB / LT)); seg(trail, LT, head - 150, head); trail.setAttribute('opacity', (1 - smooth(clamp((t - T.T_BOLD - T.D_BOLD) / .45))) * .95); }}
  else trail.setAttribute('opacity', 0);
  const ff = clamp((t - T.T_FINE) / (T.D_FINE + .3));
  if (ff > 0 && ff < 1) {{ const head = LF * pf; seg(trail2, LF, head - 120, head); trail2.setAttribute('opacity', (1 - smooth(clamp((t - T.T_FINE - T.D_FINE) / .3))) * .9); }}
  else trail2.setAttribute('opacity', 0);
  // the impact where the strokes cross
  const k = t - T.T_IMPACT;
  $('t-flash').setAttribute('opacity', k > 0 ? Math.exp(-k / .11) * .95 : 0);
  for (const [id, delay, reach] of [['t-wave', 0, 190], ['t-wave2', .07, 280]]) {{
    const w = clamp((k - delay) / .75), el = $(id);
    el.setAttribute('r', 10 + reach * outCubic(w)); el.setAttribute('stroke-width', 3.2 * (1 - w) + .4); el.setAttribute('opacity', w > 0 && w < 1 ? .85 * (1 - w) : 0);
  }}
  document.querySelectorAll('#t-sparks line').forEach((ln, i) => {{
    const s = SPARKS[i], u = k - s.d;
    if (u <= 0 || u > s.life) {{ ln.setAttribute('opacity', 0); return; }}
    const dist = s.v * .22 * (1 - Math.exp(-u / .22)), cx = T.CROSS[0] + Math.cos(s.a) * dist, cy = T.CROSS[1] + Math.sin(s.a) * dist + 60 * u * u;
    const len = s.len * (1 - u / s.life), ex = cx - Math.cos(s.a) * len, ey = cy - Math.sin(s.a) * len;
    ln.setAttribute('x1', cx); ln.setAttribute('y1', cy); ln.setAttribute('x2', ex); ln.setAttribute('y2', ey);
    ln.setAttribute('stroke-width', s.w * (1 - u / s.life) + .3); ln.setAttribute('opacity', 1 - smooth(u / s.life));
  }});
  $('t-xglow').setAttribute('opacity', k > 0 ? Math.exp(-k / .5) * .9 : (pb > 0 ? .35 * pb : 0));
  const shk = k > 0 ? 5 * Math.exp(-k / .14) : 0, pulse = k > 0 ? 1 + .022 * (k / .06) * Math.exp(1 - k / .06) : 1;
  const cx0 = T.W / 2, cy0 = T.BASE - 60;
  $('t-word').setAttribute('transform', `translate(${{cx0}} ${{cy0}}) scale(${{pulse}}) translate(${{-cx0}} ${{-cy0}})`);
  // a sheen runs through "Nex", and the rule draws outward from a small diamond
  const ns = clamp((t - T.T_IMPACT - .3) / 1.1); $('t-nexshine').setAttribute('opacity', ns > 0 && ns < 1 ? .85 : 0);
  $('t-shine').setAttribute('gradientTransform', `translate(${{T.NEX_X - 220 + ns * 620}} 0)`);
  const rl = outCubic(clamp((t - (T.T_IMPACT + .55)) / 1.0)), reach = 330 * rl;
  $('t-ruleL').setAttribute('x', T.W / 2 - 12 - reach); $('t-ruleL').setAttribute('width', reach);
  $('t-ruleR').setAttribute('x', T.W / 2 + 12); $('t-ruleR').setAttribute('width', reach);
  const gm = clamp((t - (T.T_IMPACT + .5)) / .6), gs = 11 * (gm > 0 ? outBack(gm) : 0);
  $('t-gem').setAttribute('opacity', gm > 0 ? 1 : 0); $('t-gem').setAttribute('transform', `translate(${{T.W / 2}} ${{T.BASE + 58.8}}) rotate(45) scale(${{gs / 11}}) translate(-5.5 -5.5)`);
  // the camera: a small shake at the impact, then a slow push in as the title gives way to the slogan
  const push = 1 + .05 * smooth(clamp((t - 8.6) / 2.4)), sx = shk * Math.sin(t * 107), sy = shk * Math.cos(t * 83);
  $('t-cam').setAttribute('transform', `translate(${{sx}} ${{sy}}) translate(${{T.W / 2}} ${{T.BASE - 120}}) scale(${{push}}) translate(${{-T.W / 2}} ${{-(T.BASE - 120)}})`);
  $('title').style.opacity = 1 - smooth(clamp((t - 10.0) / .9));
}};
"""
# ---- the feature scenes, re-timed for the longer film ----
a, EASE = P.a, P.EASE
def scene(start, end, body, cls=""): return f'<section class="scene {cls}" style="{a("pm-scene", end - start, start, "linear")}">{body}</section>'
def floating(html, t): return f'<div class="float" style="animation:pm-float 7s ease-in-out {t:.2f}s infinite both">{html}</div>'

S1, S2, S3, S4, S5, S6, S7, S8 = 10.4, 19.6, 29.4, 38.8, 48.4, 58.4, 66.4, 75.2
slogan = scene(S1, S2 + .4, f'''<div class="glow2" style="{a("pm-breathe", 9, S1, "ease-in-out")}"></div>
  <div class="motes">{"".join(f'<i style="left:{random.Random(i).uniform(0, 100):.1f}%;top:{random.Random(i + 99).uniform(30, 100):.1f}%;animation:pm-mote {random.Random(i + 7).uniform(7, 12):.1f}s linear {S1 + random.Random(i + 3).uniform(-6, 0):.2f}s infinite both"></i>' for i in range(40))}</div>
  <div class="slogan2" style="{a("pm-push", 9.4, S1, "linear")}"><div>{P.words("How many times", S1 + .7, .2)}</div><div>{P.words("do I have left to see you", S1 + 1.6, .17)}</div><div>{P.words("again in my life?", S1 + 3.0, .22)}</div></div>
  <div class="sub" style="{a("pm-up", 1.4, S1 + 5.0)}">AfterhoursNex · a podcast player for the late hours</div>''')

def ep_rows(t0):
    rows = [(0, "Midnight Mysteries · Today", "The locked-room problem", "Why the impossible crime still fascinates readers, and how the golden-age writers solved it."),
            (4, "Late Jazz Notes · Sunday", "Blue in green, slowly", "A whole hour on one ballad and the musicians who changed it."),
            (2, "Small Hours Science · Saturday", "Why the night sky is dark", "Olbers’ paradox, the age of the universe, and light that hasn’t arrived yet.")]
    return "".join(P.episode_row(i, m, ti, d, t0 + k * .22) for k, (i, m, ti, d) in enumerate(rows))
def up_next(t0):
    return "".join(f'<div class="card" style="{a("pm-up", 1.0, t0 + k * .18)}">{P.art(k, 150, 14)}<div class="bar"><i style="--a:.1;--b:{w};{a("pm-progress", 2.4, t0 + .6 + k * .2)}"></i></div><b>{tt}</b><span>{P.SHOWS[k][0]}</span></div>'
                   for k, (tt, w) in enumerate([("The locked-room problem", ".62"), ("Why the night sky is dark", ".35"), ("Rainy evenings in Tokyo", ".8")]))
listen = P.phone(f'''<div class="pt">Listen Now</div><div class="ps">Good evening. Here’s what’s new on your shelf.</div>
  <div class="sec">Up Next</div><div class="row">{up_next(S2 + 1.4)}</div><div class="sec">Latest episodes</div>{ep_rows(S2 + 2.2)}
  {P.mini("The locked-room problem", "Midnight Mysteries", 0)}{P.navbar("home")}''', S2 + .3)
f1 = scene(S2, S3 + .4, P.copy("Listen Now", "Your shelf,<br>ready on first launch", "Thirty hand-picked shows find themselves in Apple’s podcast directory, or on their own websites when there’s no listing.", S2 + .6) + floating(listen, S2))

def pick(i, title, why, t0, added=None):
    btn = (f'<span class="addb"><span style="{a("pm-out", .35, added)}">+ Add</span><span class="on" style="{a("pm-in", .4, added + .1)}">On your shelf</span></span>' if added
           else '<span class="addb"><span>+ Add</span></span>')
    return f'<div class="pick" style="{a("pm-up", .9, t0)}">{P.art(i, 156, 14)}<b>{title}</b><span>{why}</span>{btn}</div>'
discover = P.phone(f'''<div class="pt">Discover</div><div class="ps">Related shows, picked around what you love.</div>
  <div class="sec">City pop &amp; anime songs</div>
  <div class="rowwrap"><div class="row slide" style="animation:pm-slide 6.5s {EASE} {S3 + 2.2:.2f}s both">{pick(3, "City Pop Diaries", "One album a week from 1980s Tokyo.", S3 + 1.3, S3 + 6.2)}{pick(6, "Pixel Archive", "Songs from the games you grew up with.", S3 + 1.45)}{pick(1, "Slow Radio", "Long, quiet interviews for late evenings.", S3 + 1.6)}{pick(5, "Gadget Night", "The week in tech, without the noise.", S3 + 1.75)}</div></div>
  <div class="sec">Mystery novels</div>
  <div class="row">{pick(0, "Midnight Mysteries", "Golden-age crime, one book at a time.", S3 + 2.0)}{pick(2, "The Quiet Library", "Readers on the books that stayed.", S3 + 2.15)}</div>
  {P.tap(118, 610, S3 + 6.0)}{P.navbar("explore")}''', S3 + .3)
f2 = scene(S3, S4 + .4, P.copy("Discover", "Find the next show<br>to fall for", "Related shows picked around what you already love, from city pop to golden-age mysteries. Press Add, or open a show to preview it first.", S3 + .6) + floating(discover, S3))

results = "".join(f'''<div class="res" style="{a("pm-up", .8, S4 + 3.0 + k * .16)}">{P.art(k + 3, 64, 10)}<div class="rt"><b>{n}</b><span>{m}</span></div>
  {'<span class="btn"><span class="b1" style="' + a("pm-out", .35, S4 + 5.2) + '">Add</span><span class="b2" style="' + a("pm-in", .4, S4 + 5.3) + '">Open</span></span>' if k == 0 else '<span class="btn"><span>Add</span></span>'}</div>'''
                  for k, (n, m) in enumerate([("City Pop Diaries", "Night Shift Radio · Music"), ("Tokyo After Dark", "Kaze Studio · Music"), ("Crate Digging Japan", "Low Light · Music")]))
search = P.phone(f'''<div class="pt">Search</div>
  <div class="field">{P.icon("search", 22)}<span class="typed" style="{a("pm-type", 1.5, S4 + 1.4, "steps(8, end)")}">city pop</span><span class="caret" style="animation:pm-blink 1s steps(2, start) {S4:.2f}s 8 both"></span></div>
  <div class="hint" style="{a("pm-fade", .6, S4 + .8)}">Shows, topics, or a feed link</div>{results}{P.tap(360, 287, S4 + 5.0)}
  <div class="toast" style="{a("pm-pop", .6, S4 + 5.6)}">Added City Pop Diaries to your library.</div>{P.navbar("search")}''', S4 + .3)
f3 = scene(S4, S5 + .4, P.copy("Search", "Find any show,<br>or paste a link", "Search Apple’s directory, or paste a feed, an Apple Podcasts link or a website. If a feed stops working, AfterhoursNex reads the show’s website instead.", S4 + .6) + floating(search, S4))

playing = P.phone(f'''<div class="nph">{P.icon("down", 26)}<span>Now Playing</span><i></i></div>
  <div class="npart" style="{a("pm-ken", 10, S5, "ease-in-out")}">{P.art(7, 300, 22, 96)}</div>
  <div class="npt">The locked-room problem</div><div class="nps">Midnight Mysteries</div>
  <div class="slider"><i style="--a:.30;--b:.47;{a("pm-progress", 9.4, S5 + .6, "linear")}"></i><b style="animation:pm-knob2 9.4s linear {S5 + .6:.2f}s both"></b></div>
  <div class="times"><span>18:42</span><span>−27:10</span></div>
  <div class="ctl"><span class="sk">{P.icon("rew", 30)}<small>15</small></span><span class="big">{P.icon("pause", 40)}</span><span class="sk">{P.icon("fwd", 30)}<small>30</small></span></div>
  <div class="chips"><span class="chip">{P.icon("speed", 18)}<span class="swap"><span style="{a("pm-out", .35, S5 + 3.0)}">1.0×</span><span style="{a("pm-in", .4, S5 + 3.1)}">1.2×</span></span></span>
    <span class="chip">{P.icon("moon", 18)}<span class="swap"><span style="{a("pm-out", .35, S5 + 6.4)}">Sleep timer</span><span style="{a("pm-in", .4, S5 + 6.5)}">30 min left</span></span></span>{P.icon("next", 26)}</div>
  {P.tap(132, 744, S5 + 2.8)}{P.tap(266, 744, S5 + 4.6)}
  <div class="menu" style="animation:pm-menu 2.2s {EASE} {S5 + 4.9:.2f}s both"><div>Off</div><div>15 minutes</div><div class="sel">30 minutes</div><div>45 minutes</div><div>End of this episode</div></div>''', S5 + .3)
f4 = scene(S5, S6 + .4, P.copy("Now Playing", "Pick up exactly<br>where you left off", "Speeds from 0.8× to 2×, back 15 and forward 30 seconds, and a sleep timer that can stop at the end of the episode.", S5 + .6) + floating(playing, S5))

lock = P.phone(f'''<div class="lock"><div class="lt" style="{a("pm-up", 1.1, S6 + .7)}">23:47</div><div class="ld" style="{a("pm-up", 1.1, S6 + .85)}">Monday 19 October</div>
  <div class="media" style="{a("pm-up", 1.1, S6 + 1.4)}"><div class="mh">{P.art(0, 64, 12)}<div><b>The locked-room problem</b><span>Midnight Mysteries</span></div></div>
  <div class="mbar"><i style="--a:.47;--b:.56;{a("pm-progress", 6.5, S6 + 1.6, "linear")}"></i></div>
  <div class="mc">{P.icon("prev", 30)}{P.icon("rew", 30)}<span class="mplay">{P.icon("pause", 34)}</span>{P.icon("fwd", 30)}{P.icon("next", 30)}</div></div></div>''', S6 + .3, "")
f5 = scene(S6, S7 + .4, P.copy("In the background", "Keeps playing<br>with the screen off", "Lock-screen, notification, headphone and car controls on Android, so the story carries on while you do something else.", S6 + .6) + floating(lock, S6))

grid = "".join(f'<div class="tile" style="{a("pm-up", .8, S7 + 1.2 + k * .07)}">{P.art(k, 132, 12)}<b>{P.SHOWS[k % 8][0]}</b></div>' for k in range(10))
desk = f'''<div class="laptop" style="{a("pm-phone", 1.4, S7 + .4)}"><div class="lscreen"><div class="win"><div class="wbar"><i></i><i></i><i></i><span>AfterhoursNex</span></div>
  <div class="wbody"><div class="rail">{"".join(f'<span class="{c}">{P.icon(k, 24)}</span>' for k, c in [("home", ""), ("explore", ""), ("library", "on"), ("search", ""), ("gear", "")])}</div>
  <div class="wcontent"><div class="wt">Library</div><div class="wsub">30 shows</div><div class="grid">{grid}</div></div></div>
  {P.mini("Blue in green, slowly", "Late Jazz Notes", 4)}</div></div><div class="lbase"></div></div>
  <div class="phone small" style="{a("pm-phone", 1.3, S7 + .9)}"><div class="screen">{P.status()}<div class="pt">Listen Now</div><div class="ps">Good evening.</div><div class="sec">Up Next</div><div class="row">{up_next(S7 + 1.6)}</div>{P.navbar("home")}</div></div>'''
f6 = scene(S7, S8 + .4, f'<div class="center-copy"><h2 style="{a("pm-up", 1.2, S7 + .3)}">One app. Four systems.</h2><p style="{a("pm-up", 1.2, S7 + .55)}">Windows · macOS · Linux · Android</p></div>' + floating(desk, S7), "s7")

fw = 0.82                                              # the finale's wordmark, written in full
fx0 = W / 2 - (G["after"]["width"] + 6 + G["nex"]["width"]) * F * fw / 2; fnx = fx0 + (G["after"]["width"] + 6) * F * fw; fb = 470
final_word = (f'<svg class="fword" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><defs><linearGradient id="f-sun" x1="{fnx:.0f}" y1="{fb - 140}" x2="{fnx + G["nex"]["width"] * F * fw:.0f}" y2="{fb + 14}" gradientUnits="userSpaceOnUse">'
              f'<stop offset="0" stop-color="#FEBB5F"/><stop offset=".35" stop-color="#F7926D"/><stop offset=".62" stop-color="#F37377"/><stop offset="1" stop-color="#B6448D"/></linearGradient>'
              f'<linearGradient id="f-shine" x1="0" y1="0" x2="140" y2="0" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".5" stop-color="#fff" stop-opacity=".9"/><stop offset="1" stop-color="#fff" stop-opacity="0"/>'
              f'<animateTransform attributeName="gradientTransform" type="translate" values="{fnx - 200:.0f} 0; {fnx + 520:.0f} 0" begin="{S8 + 2.2}s" dur="1.4s" fill="freeze"/></linearGradient></defs>'
              f'<image href="data:image/png;base64,{ICON}" x="{W / 2 - 85}" y="150" width="170" height="170"/>'
              f'<g fill="#F5EEF4">{"".join(f"<path d=\"{gd(chr(97) + chr(102) + chr(116) + chr(101) + chr(114), g, fx0, fb, F * fw)}\"/>" for g in G["after"]["glyphs"])}</g>'
              f'<g fill="url(#f-sun)">{"".join(f"<path d=\"{gd(chr(110) + chr(101) + chr(120), g, fnx, fb, F * fw)}\"/>" for g in G["nex"]["glyphs"])}</g>'
              f'<g fill="url(#f-shine)">{"".join(f"<path d=\"{gd(chr(110) + chr(101) + chr(120), g, fnx, fb, F * fw)}\"/>" for g in G["nex"]["glyphs"])}</g></svg>')
finale = scene(S8, END + 1, f'''<div class="fin" style="{a("pm-end", 2.2, S8, "cubic-bezier(.2,.7,.2,1)")}">{final_word}</div>
  <div class="fslogan" style="{a("pm-up", 1.6, S8 + 1.6)}">How many times do I have left to see you again in my life?</div>
  <div class="ffree" style="{a("pm-up", 1.4, S8 + 3.4)}">Free · private · no account needed</div>
  <div class="fplat" style="{a("pm-up", 1.4, S8 + 4.2)}">WINDOWS · MACOS · LINUX · ANDROID</div>''', "s8")

rs = random.Random(21)
starfield = '<svg class="stars" width="2100" height="1200" viewBox="0 0 2100 1200">' + "".join(
    f'<circle cx="{rs.uniform(0, 2100):.0f}" cy="{rs.uniform(0, 1200):.0f}" r="{rs.choice([.7, 1, 1.3, 1.7]):.1f}" fill="#FFF3E6" style="animation:pm-twinkle {rs.uniform(3, 7):.1f}s ease-in-out {rs.uniform(-7, 0):.1f}s infinite both"/>'
    for _ in range(140)) + "</svg>"
CSS2 = """
.stars { position: absolute; left: -90px; top: -60px; animation: pm-drift 86s linear 0s both; }
@keyframes pm-drift { from { transform: translate(0, 0); } to { transform: translate(70px, 40px); } }
@keyframes pm-twinkle { 0%, 100% { opacity: .15; } 50% { opacity: .9; } }
.title { position: absolute; left: 0; top: 0; }
.float { position: absolute; inset: 0; }
@keyframes pm-float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }
@keyframes pm-breathe { 0% { opacity: 0; } 20% { opacity: 1; } 100% { opacity: .85; } }
@keyframes pm-push { from { transform: scale(1); } to { transform: scale(1.045); } }
@keyframes pm-mote { from { transform: translateY(0); opacity: 0; } 15% { opacity: .7; } 85% { opacity: .5; } to { transform: translateY(-260px); opacity: 0; } }
@keyframes pm-slide { 0%, 25% { transform: translateX(0); } 70%, 100% { transform: translateX(-170px); } }
@keyframes pm-knob2 { from { left: calc(30% - 9px); } to { left: calc(47% - 9px); } }
.glow2 { position: absolute; left: 50%; top: 50%; width: 1500px; height: 800px; transform: translate(-50%, -50%); background: radial-gradient(closest-side, rgba(247,146,109,.24), rgba(204,82,134,.08) 55%, transparent); opacity: 0; }
.motes { position: absolute; inset: 0; } .motes i { position: absolute; width: 3px; height: 3px; border-radius: 50%; background: #FFE3B0; box-shadow: 0 0 8px #FEBB5F; opacity: 0; }
.slogan2 { position: absolute; inset: 250px 0 auto; text-align: center; font-family: NexSerif; font-style: italic; font-size: 100px; line-height: 1.22; color: #F5EEF4; transform-origin: 50% 40%; }
.rowwrap { overflow: hidden; margin: 0 -22px; padding: 0 22px; } .slide { width: max-content; }
.pick { width: 156px; flex: none; opacity: 0; display: flex; flex-direction: column; } .pick b { font-size: 15px; margin-top: 8px; } .pick > span:not(.addb) { font-size: 13px; color: #B9B3B8; line-height: 1.3; height: 34px; overflow: hidden; }
.addb { display: inline-grid; margin-top: 8px; color: #FF6E9F; font-weight: 700; font-size: 14px; } .addb > span { grid-area: 1 / 1; white-space: nowrap; } .addb .on { opacity: 0; color: #FEBB5F; }
.fin { position: absolute; inset: 0; opacity: 0; } .fword { position: absolute; left: 0; top: 0; }
.fslogan { position: absolute; left: 0; right: 0; top: 560px; text-align: center; font-family: NexSerif; font-style: italic; font-size: 46px; color: #ECE0F0; opacity: 0; }
.ffree { position: absolute; left: 0; right: 0; top: 690px; text-align: center; font-family: NexSerif; font-size: 32px; color: #F7926D; letter-spacing: 1px; opacity: 0; }
.fplat { position: absolute; left: 0; right: 0; top: 760px; text-align: center; font-family: NexSerif; font-size: 22px; letter-spacing: 7px; color: #A99BCB; opacity: 0; }
.fadein { position: absolute; inset: 0; background: #000; animation: pm-gone 1.4s linear 0s both; } @keyframes pm-gone { from { opacity: 1; } to { opacity: 0; } }
.fadeout2 { position: absolute; inset: 0; background: #000; opacity: 0; animation: pm-fade 1.2s linear 84.8s both; }
"""
page = f"""<!doctype html><html><head><meta charset="utf-8"><title>AfterhoursNex film</title><style>{P.CSS}{CSS2}</style></head>
<body><div class="bg"></div><div class="dusk"></div>{starfield}{title}{slogan}{f1}{f2}{f3}{f4}{f5}{f6}{finale}<div class="fadein"></div><div class="fadeout2"></div>
<script>{TITLE_JS}
window.seek = t => {{
  for (const s of document.querySelectorAll('svg')) if (s.pauseAnimations) {{ s.pauseAnimations(); s.setCurrentTime(t); }}
  for (const an of document.getAnimations()) {{ an.pause(); an.currentTime = t * 1000; }}
  window.renderTitle(t);
}};
window.LENGTH = {END};
</script></body></html>"""
open("promo2.html", "w", encoding="utf-8").write(page)
print(f"promo2.html: {len(page) // 1024} KB, {END} s; the x crosses at {T_IMPACT:.3f} s ({c:.2f} of the fine stroke)")

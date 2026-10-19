"""The animated AfterhoursNex banner (1280 x 640), built from real letter outlines so it looks the same everywhere."""
import base64, io, json, math, random
from PIL import Image

G = json.load(open("glyphs.json"))
W, H = 1280, 640
BASE = 384                       # wordmark baseline
GAP = 6                          # between "Afterhours" and the italic "Nex"
total = G["after"]["width"] + GAP + G["nex"]["width"]
X0 = W / 2 - total / 2
NEX_X = X0 + G["after"]["width"] + GAP
SUNSET = ["#FEBB5F", "#F7926D", "#F37377", "#CC5286", "#B6448D"]

from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
_REG, _ITA = (TTFont("../shared/src/commonMain/composeResources/font/liberation_serif_%s.ttf" % f) for f in ("regular", "italic"))
FONT = {"after": _REG, "nex": _ITA, "slogan": _ITA, "platforms": _REG}

def glyph_d(run, g, x0, base):
    """The letter's outline, already placed in banner pixels (so gradients line up across letters)."""
    f = FONT[run]; gs = f.getGlyphSet(); name = f.getBestCmap().get(ord(g["ch"]))
    if not name or g["ch"] == " ":
        return ""
    k = G[run]["scale"]
    pen = SVGPathPen(gs, ntos=lambda v: ("%.2f" % v).rstrip("0").rstrip("."))
    gs[name].draw(TransformPen(pen, (k, 0, 0, -k, x0 + g["x"], base)))
    return pen.getCommands()

def glyph_path(run, g, x0, base, extra=""):
    return f'<path d="{glyph_d(run, g, x0, base)}"{extra}/>'

def letters():
    out = []
    for i, g in enumerate(G["after"]["glyphs"]):
        out.append(f'<g class="ch" style="animation-delay:{0.95 + i * 0.065:.3f}s">{glyph_path("after", g, X0, BASE)}</g>')
    for i, g in enumerate(G["nex"]["glyphs"][:2]):
        out.append(f'<g class="ch" style="animation-delay:{1.72 + i * 0.09:.3f}s">{glyph_path("nex", g, NEX_X, BASE, " fill=\"url(#sunset)\"")}</g>')
    return "".join(out)

# ---- the x, drawn like two pen strokes ----
xg = G["nex"]["glyphs"][2]
k = G["nex"]["scale"]
bx0, by0, bx1, by1 = xg["bounds"]
left, right = NEX_X + xg["x"] + bx0 * k, NEX_X + xg["x"] + bx1 * k
top, bottom = BASE - by1 * k, BASE - by0 * k
xw, xh = right - left, bottom - top
cx, cy = left + xw / 2, top + xh / 2
def band(sx, sy, ex, ey, begin, dur, idn):
    ang = math.degrees(math.atan2(ey - sy, ex - sx))
    length = math.hypot(ex - sx, ey - sy) + 16
    h = xh * 0.62
    return (f'<rect x="{sx - 8:.1f}" y="{sy - h / 2:.1f}" width="0" height="{h:.1f}" transform="rotate({ang:.2f} {sx:.1f} {sy:.1f})">'
            f'<animate attributeName="width" values="0;{length:.1f}" keyTimes="0;1" calcMode="spline" keySplines=".55 0 .25 1" begin="{begin}s" dur="{dur}s" fill="freeze"/></rect>')
x_clip = (band(left + xw * 0.04, top, right, bottom, 2.02, 0.40, "a")      # the bold stroke, top left to bottom right
          + band(right, top, left, bottom, 2.38, 0.34, "b"))               # the fine stroke, crossing it
x_mark = glyph_path("nex", xg, NEX_X, BASE, ' fill="url(#sunset)"')
STAR = "M0 -1 Q0 0 1 0 Q0 0 0 1 Q0 0 -1 0 Q0 0 0 -1Z"

def run_words(run, base, delay0, step, cls):
    g = G[run]; x0 = W / 2 - g["width"] / 2
    words, cur = [], []
    for gl in g["glyphs"]:
        if gl["ch"] == " ":
            if cur: words.append(cur); cur = []
        else:
            cur.append(gl)
    if cur: words.append(cur)
    return "".join(f'<g class="{cls}" style="animation-delay:{delay0 + j * step:.3f}s">' + "".join(glyph_path(run, gl, x0, base) for gl in w) + "</g>" for j, w in enumerate(words))

def icon_data(size=300):
    im = Image.open("../shared/src/commonMain/composeResources/drawable/afterhours_icon.png").convert("RGBA").resize((size, size), Image.LANCZOS)
    buf = io.BytesIO(); im.save(buf, "PNG", optimize=True)
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()

def stars():
    rnd = random.Random(11); out = []
    while len(out) < 34:
        x, y = rnd.uniform(24, W - 24), rnd.uniform(18, H - 18)
        if 200 < x < 1080 and 270 < y < 560: continue     # keep the lettering clear
        if 520 < x < 760 and 60 < y < 270: continue        # and the icon
        out.append(f'<circle class="star" cx="{x:.0f}" cy="{y:.0f}" r="{rnd.choice([0.8, 1.1, 1.4, 1.8]):.1f}" style="animation-delay:{rnd.uniform(0, 4):.2f}s;animation-duration:{rnd.uniform(3, 6):.2f}s"/>')
    return "".join(out)

ICX, ICY, IS = W / 2, 168, 150
css = f"""
  .ch {{ opacity: 0; animation: letter 1s cubic-bezier(.2,.75,.25,1) both; }}
  @keyframes letter {{ 0% {{ opacity: 0; transform: translateY(26px); filter: blur(7px); }} 55% {{ opacity: 1; }} 100% {{ opacity: 1; transform: none; filter: blur(0); }} }}
  .settle {{ transform-origin: {cx:.1f}px {cy:.1f}px; animation: settle .9s cubic-bezier(.3,1.6,.45,1) 2.62s both; }}
  @keyframes settle {{ from {{ transform: rotate(-9deg) scale(.96); }} to {{ transform: none; }} }}
  .spark {{ opacity: 0; transform-box: fill-box; transform-origin: center; animation: spark 1.1s cubic-bezier(.2,.7,.3,1) 2.66s both; }}
  .spark.small {{ animation-delay: 2.82s; animation-duration: .9s; }}
  @keyframes spark {{ 0% {{ opacity: 0; transform: scale(0) rotate(0deg); }} 35% {{ opacity: 1; transform: scale(1) rotate(35deg); }} 100% {{ opacity: 0; transform: scale(.5) rotate(90deg); }} }}
  .bloom {{ opacity: 0; transform-box: fill-box; transform-origin: center; animation: bloom 1.4s ease-out 2.62s both; }}
  @keyframes bloom {{ 0% {{ opacity: 0; transform: scale(.4); }} 30% {{ opacity: .9; }} 100% {{ opacity: 0; transform: scale(1.6); }} }}
  .icon {{ opacity: 0; transform-origin: {ICX:.0f}px {ICY + IS / 2:.0f}px; animation: rise 1.2s cubic-bezier(.2,.8,.2,1) .15s both; }}
  @keyframes rise {{ from {{ opacity: 0; transform: translateY(30px) scale(.92); }} to {{ opacity: 1; transform: none; }} }}
  .halo {{ opacity: 0; animation: halo 1.6s ease-out .3s forwards, breathe 6s ease-in-out 1.9s infinite alternate; }}
  @keyframes halo {{ to {{ opacity: .85; }} }}
  @keyframes breathe {{ from {{ opacity: .85; }} to {{ opacity: 1; }} }}
  .ring {{ fill: none; stroke: url(#ringLine); stroke-width: 1.6; opacity: 0; transform-origin: {ICX:.0f}px {ICY:.0f}px; animation: ring 3.6s cubic-bezier(.2,.6,.4,1) infinite; }}
  .r1 {{ animation-delay: 1.1s; }} .r2 {{ animation-delay: 2.3s; }} .r3 {{ animation-delay: 3.5s; }}
  @keyframes ring {{ 0% {{ opacity: 0; transform: scale(.95); }} 15% {{ opacity: .7; }} 100% {{ opacity: 0; transform: scale(1.75); }} }}
  .star {{ fill: #FFF3E6; opacity: .15; animation: twinkle 4s ease-in-out infinite; }}
  @keyframes twinkle {{ 0%, 100% {{ opacity: .1; }} 50% {{ opacity: .85; }} }}
  .w {{ opacity: 0; animation: word .9s cubic-bezier(.2,.75,.25,1) both; }}
  @keyframes word {{ from {{ opacity: 0; transform: translateY(9px); filter: blur(3px); }} to {{ opacity: 1; transform: none; filter: blur(0); }} }}
  .gem {{ opacity: 0; transform-box: fill-box; transform-origin: center; animation: gem .7s cubic-bezier(.3,1.5,.5,1) 3.0s both; }}
  @keyframes gem {{ from {{ opacity: 0; transform: rotate(45deg) scale(0); }} to {{ opacity: 1; transform: rotate(45deg) scale(1); }} }}
  .plat {{ opacity: 0; animation: word 1s ease-out 4.55s both; }}
  @media (prefers-reduced-motion: reduce) {{ .ring, .star, .halo {{ animation: none; opacity: .6; }} }}
"""
FX = 640
svg = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}" role="img" aria-label="AfterhoursNex. How many times do I have left to see you again in my life?">
<title>AfterhoursNex</title>
<style>{css}</style>
<defs>
  <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#110C2B"/><stop offset=".6" stop-color="#1B1444"/><stop offset="1" stop-color="#2A1C58"/></linearGradient>
  <radialGradient id="dusk" cx="640" cy="760" r="760" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#F37377" stop-opacity=".30"/><stop offset=".35" stop-color="#CC5286" stop-opacity=".12"/><stop offset="1" stop-color="#CC5286" stop-opacity="0"/></radialGradient>
  <radialGradient id="glow" cx="{ICX:.0f}" cy="{ICY:.0f}" r="230" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#F7926D" stop-opacity=".34"/><stop offset=".45" stop-color="#CC5286" stop-opacity=".10"/><stop offset="1" stop-color="#CC5286" stop-opacity="0"/></radialGradient>
  <linearGradient id="ringLine" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FEBB5F"/><stop offset="1" stop-color="#CC5286"/></linearGradient>
  <linearGradient id="sunset" x1="{NEX_X:.0f}" y1="{BASE - 95}" x2="{NEX_X + G["nex"]["width"]:.0f}" y2="{BASE + 10}" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="{SUNSET[0]}"/><stop offset=".35" stop-color="{SUNSET[1]}"/><stop offset=".6" stop-color="{SUNSET[2]}"/><stop offset="1" stop-color="{SUNSET[4]}"/>
  </linearGradient>
  <linearGradient id="shine" x1="0" y1="0" x2="120" y2="0" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="#FFF" stop-opacity="0"/><stop offset=".5" stop-color="#FFF" stop-opacity=".9"/><stop offset="1" stop-color="#FFF" stop-opacity="0"/>
    <animateTransform attributeName="gradientTransform" type="translate" values="{NEX_X - 160:.0f} 0; {NEX_X + G["nex"]["width"] + 60:.0f} 0" begin="3.3s" dur="1.2s" fill="freeze"/>
  </linearGradient>
  <linearGradient id="iconShine" x1="0" y1="0" x2="80" y2="80" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="#FFF" stop-opacity="0"/><stop offset=".5" stop-color="#FFF" stop-opacity=".45"/><stop offset="1" stop-color="#FFF" stop-opacity="0"/>
    <animateTransform attributeName="gradientTransform" type="translate" values="{ICX - IS:.0f} {ICY - IS:.0f}; {ICX + IS / 2:.0f} {ICY + IS / 2:.0f}" begin="1.25s" dur="1.1s" fill="freeze"/>
  </linearGradient>
  <linearGradient id="ruleL" x1="1" y1="0" x2="0" y2="0"><stop offset="0" stop-color="#F7926D"/><stop offset="1" stop-color="#CC5286" stop-opacity="0"/></linearGradient>
  <linearGradient id="ruleR" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#F7926D"/><stop offset="1" stop-color="#CC5286" stop-opacity="0"/></linearGradient>
  <radialGradient id="sparkGlow"><stop offset="0" stop-color="#FFF3DC" stop-opacity=".95"/><stop offset=".4" stop-color="#FEBB5F" stop-opacity=".45"/><stop offset="1" stop-color="#FEBB5F" stop-opacity="0"/></radialGradient>
  <clipPath id="iconShape"><rect x="{ICX - IS / 2:.0f}" y="{ICY - IS / 2:.0f}" width="{IS}" height="{IS}" rx="{IS * 0.22:.0f}"/></clipPath>
  <clipPath id="xdraw">{x_clip}</clipPath>
</defs>
<rect width="{W}" height="{H}" fill="url(#sky)"/>
<rect width="{W}" height="{H}" fill="url(#dusk)"/>
<g>{stars()}</g>
<circle class="halo" cx="{ICX:.0f}" cy="{ICY:.0f}" r="230" fill="url(#glow)"/>
<rect class="ring r1" x="{ICX - IS / 2:.0f}" y="{ICY - IS / 2:.0f}" width="{IS}" height="{IS}" rx="{IS * 0.24:.0f}"/>
<rect class="ring r2" x="{ICX - IS / 2:.0f}" y="{ICY - IS / 2:.0f}" width="{IS}" height="{IS}" rx="{IS * 0.24:.0f}"/>
<rect class="ring r3" x="{ICX - IS / 2:.0f}" y="{ICY - IS / 2:.0f}" width="{IS}" height="{IS}" rx="{IS * 0.24:.0f}"/>
<g class="icon">
  <image href="{icon_data()}" x="{ICX - IS / 2:.0f}" y="{ICY - IS / 2:.0f}" width="{IS}" height="{IS}"/>
  <rect clip-path="url(#iconShape)" x="{ICX - IS / 2:.0f}" y="{ICY - IS / 2:.0f}" width="{IS}" height="{IS}" fill="url(#iconShine)"/>
</g>
<g fill="#F5EEF4">{letters()}</g>
<g class="settle"><g clip-path="url(#xdraw)">{x_mark}</g></g>
<g fill="url(#shine)" aria-hidden="true">{"".join(glyph_path("nex", g, NEX_X, BASE) for g in G["nex"]["glyphs"])}</g>
<circle class="bloom" cx="{cx:.1f}" cy="{cy:.1f}" r="34" fill="url(#sparkGlow)"/>
<path class="spark" d="{STAR}" transform="translate({cx:.1f} {cy:.1f}) scale(22)" fill="#FFF6E8"/>
<path class="spark small" d="{STAR}" transform="translate({cx + xw * 0.62:.1f} {cy - xh * 0.62:.1f}) scale(9)" fill="#FEBB5F"/>
<rect x="{FX - 14}" y="418.3" width="0" height="1.4" fill="url(#ruleL)">
  <animate attributeName="x" values="{FX - 14};{FX - 250}" keyTimes="0;1" calcMode="spline" keySplines=".2 .8 .2 1" begin="3.05s" dur=".9s" fill="freeze"/>
  <animate attributeName="width" values="0;236" keyTimes="0;1" calcMode="spline" keySplines=".2 .8 .2 1" begin="3.05s" dur=".9s" fill="freeze"/>
</rect>
<rect x="{FX + 14}" y="418.3" width="0" height="1.4" fill="url(#ruleR)">
  <animate attributeName="width" values="0;236" keyTimes="0;1" calcMode="spline" keySplines=".2 .8 .2 1" begin="3.05s" dur=".9s" fill="freeze"/>
</rect>
<rect class="gem" x="{FX - 4.5}" y="414.5" width="9" height="9" fill="url(#ruleR)"/>
<g fill="#ECE0F0">{run_words("slogan", 480, 3.4, 0.085, "w")}</g>
<g class="plat" fill="#A99BCB">{run_words("platforms", 540, 0, 0, "")}</g>
</svg>
'''
open("afterhoursnex-intro.svg", "w", encoding="utf-8").write(svg)
print(f"banner: {len(svg) // 1024} KB; x drawn at ({cx:.0f}, {cy:.0f}), {xw:.0f} x {xh:.0f}px")

"""Letter outlines (Liberation Serif, a Times New Roman-compatible font) with kerned positions, for the brand artwork."""
import json
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.boundsPen import BoundsPen
from playwright.sync_api import sync_playwright

FONTS = {"regular": "../shared/src/commonMain/composeResources/font/liberation_serif_regular.ttf",
         "italic": "../shared/src/commonMain/composeResources/font/liberation_serif_italic.ttf"}
RUNS = [  # name, text, font, size in px, extra tracking in px
    ("after", "Afterhours", "regular", 132, 0.0),
    ("nex", "Nex", "italic", 132, 0.0),
    ("slogan", "How many times do I have left to see you again in my life?", "italic", 31, 0.2),
    ("platforms", "WINDOWS · MACOS · LINUX · ANDROID", "regular", 16, 4.2),
]
fonts = {k: TTFont(v) for k, v in FONTS.items()}

def outline(font, ch):
    gs = font.getGlyphSet()
    name = font.getBestCmap().get(ord(ch))
    if name is None or ch == " ":
        return "", None
    pen = SVGPathPen(gs); gs[name].draw(pen)
    bp = BoundsPen(gs); gs[name].draw(bp)
    return pen.getCommands(), bp.bounds

with sync_playwright() as p:
    b = p.chromium.launch(); pg = b.new_page()
    import base64
    face = lambda f: "data:font/ttf;base64," + base64.b64encode(open(f, "rb").read()).decode()
    pg.set_content("<style>@font-face{font-family:R;src:url(%s)}@font-face{font-family:I;src:url(%s)}</style><canvas id=c></canvas>" % (face(FONTS["regular"]), face(FONTS["italic"])))
    pg.evaluate("Promise.all([document.fonts.load('40px R'), document.fonts.load('40px I')])")
    out = {}
    for name, text, face, size, track in RUNS:
        widths = pg.evaluate("""([t, f, s]) => { const c = document.getElementById('c').getContext('2d'); c.font = s + 'px ' + f; c.fontKerning = 'normal';
            const w = []; for (let i = 0; i <= t.length; i++) w.push(c.measureText(t.slice(0, i)).width); return w; }""", [text, "R" if face == "regular" else "I", size])
        font = fonts[face]
        upm = font["head"].unitsPerEm
        glyphs = []
        for i, ch in enumerate(text):
            d, bounds = outline(font, ch)
            glyphs.append({"ch": ch, "x": widths[i] + i * track, "d": d, "bounds": bounds})
        out[name] = {"text": text, "size": size, "scale": size / upm, "width": widths[-1] + (len(text) - 1) * track, "glyphs": glyphs}
    b.close()
json.dump(out, open("glyphs.json", "w"))
for k, v in out.items():
    print(f"{k:10s} {v['size']:>4}px  width {v['width']:.0f}px  {len(v['glyphs'])} glyphs")
x = out["nex"]["glyphs"][2]
print("italic x bounds (font units):", x["bounds"])

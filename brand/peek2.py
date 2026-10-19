import io, json, sys
from PIL import Image, ImageDraw
from playwright.sync_api import sync_playwright
T = json.load(open("timing.json"))
spec = sys.argv[1]
if spec == "x":
    moments = [("gathering", T["TX"] + .32), ("the strike", T["T_BOLD"] + .14), ("follow-through", T["T_BOLD"] + .36), ("the cut", T["T_FINE"] + .06),
               ("impact", T["T_IMPACT"] + .025), ("sparks", T["T_IMPACT"] + .16), ("the sheen", T["T_IMPACT"] + .75), ("settled", 9.4)]
    box, cols = (1080, 330, 1730, 800), 4
else:
    moments = [(m, float(m)) for m in spec.split(",")]; box, cols = None, 4
with sync_playwright() as p:
    b = p.chromium.launch(); pg = b.new_page(viewport={"width": 1920, "height": 1080})
    pg.goto(__import__("pathlib").Path("promo2.html").resolve().as_uri()); pg.evaluate("document.fonts.ready"); pg.wait_for_timeout(500)
    shots = []
    for label, t in moments:
        pg.evaluate("t => window.seek(t)", t); pg.wait_for_timeout(30)
        im = Image.open(io.BytesIO(pg.screenshot(type="png"))).convert("RGB")
        im = im.crop(box).resize((390, 282), Image.LANCZOS) if box else im.resize((480, 270), Image.LANCZOS)
        ImageDraw.Draw(im).text((8, 6), f"{label}  {t:.2f}s", fill=(255, 255, 255))
        shots.append(im)
    b.close()
w, h = shots[0].size
sheet = Image.new("RGB", (w * cols, h * ((len(shots) + cols - 1) // cols)))
for i, s in enumerate(shots): sheet.paste(s, ((i % cols) * w, (i // cols) * h))
sheet.save("/tmp/peek2.png"); print(len(shots), "frames")

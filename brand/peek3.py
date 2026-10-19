import io, json, sys
from PIL import Image, ImageDraw
from playwright.sync_api import sync_playwright
T = json.load(open("timing3.json")); spec = sys.argv[1]
if spec == "x":
    moments = [("front reaching the x", T["BOLD0"] - .25), ("bold stroke", T["BOLD0"] + .3), ("the lift", T["BOLD0"] + .74), ("fine stroke", T["FINE0"] + .25),
               ("written", T["FINE1"] + .1), ("bloom", T["FINE1"] + .45), ("sheen", T["SHEEN"] + .55), ("settled", 9.0)]
    box, size, cols = (1080, 330, 1730, 800), (390, 282), 4
else:
    moments = [(m, float(m)) for m in spec.split(",")]; box, size, cols = None, (480, 270), 4
with sync_playwright() as p:
    b = p.chromium.launch(); pg = b.new_page(viewport={"width": 1920, "height": 1080})
    pg.goto(__import__("pathlib").Path("promo3.html").resolve().as_uri()); pg.evaluate("document.fonts.ready"); pg.wait_for_timeout(500)
    shots = []
    for label, t in moments:
        pg.evaluate("t => window.seek(t)", t); pg.wait_for_timeout(30)
        im = Image.open(io.BytesIO(pg.screenshot(type="png"))).convert("RGB")
        im = (im.crop(box) if box else im).resize(size, Image.LANCZOS)
        ImageDraw.Draw(im).text((8, 6), f"{label}  {t:.2f}s", fill=(255, 255, 255)); shots.append(im)
    b.close()
w, h = size
sheet = Image.new("RGB", (w * cols, h * ((len(shots) + cols - 1) // cols)))
for i, s in enumerate(shots): sheet.paste(s, ((i % cols) * w, (i // cols) * h))
sheet.save("/tmp/peek3.png"); print(len(shots), "frames")

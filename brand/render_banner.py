import io, os, sys
from PIL import Image
from playwright.sync_api import sync_playwright
SET = """t => { const s = document.documentElement; s.pauseAnimations(); s.setCurrentTime(t);
  for (const a of document.getAnimations()) { a.pause(); a.currentTime = t * 1000; } }"""
times = [i / 15 for i in range(0, 87)]          # 0 to 5.73 seconds
peek = [float(x) for x in sys.argv[1:]]
frames = {}
with sync_playwright() as p:
    b = p.chromium.launch(); pg = b.new_page(viewport={"width": 1280, "height": 640})
    pg.goto(__import__("pathlib").Path("afterhoursnex-intro.svg").resolve().as_uri()); pg.wait_for_timeout(300)
    for t in sorted(set(times + peek)):
        pg.evaluate(SET, t); pg.wait_for_timeout(12)
        frames[t] = Image.open(io.BytesIO(pg.screenshot())).convert("RGB")
    b.close()
frames[times[-1]].save("afterhoursnex-banner.png", optimize=True)
if peek:
    sheet = Image.new("RGB", (1280, 320 * ((len(peek) + 1) // 2)))
    for i, t in enumerate(peek):
        sheet.paste(frames[t].resize((640, 320), Image.LANCZOS), ((i % 2) * 640, (i // 2) * 320))
    sheet.save("/tmp/banner-peek.png")
small = [frames[t].resize((720, 360), Image.LANCZOS) for t in times]
pal = small[-1].quantize(colors=200, method=Image.Quantize.MEDIANCUT)
gif = [f.quantize(palette=pal, dither=Image.Dither.FLOYDSTEINBERG) for f in small]
gif[0].save("afterhoursnex-intro.gif", save_all=True, append_images=gif[1:], duration=[67] * (len(gif) - 1) + [3000], loop=0, optimize=True, disposal=1)
print("GIF", os.path.getsize("afterhoursnex-intro.gif") // 1024, "KB,", len(gif), "frames; PNG", os.path.getsize("afterhoursnex-banner.png") // 1024, "KB")

import subprocess, time
from playwright.sync_api import sync_playwright
FPS, LENGTH = 60, 72.0
ff = subprocess.Popen(["ffmpeg", "-y", "-loglevel", "error", "-f", "image2pipe", "-framerate", str(FPS), "-c:v", "mjpeg", "-i", "-",
                       "-i", "music4.wav", "-c:v", "libx264", "-preset", "medium", "-crf", "17", "-pix_fmt", "yuv420p",
                       "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart", "-shortest", "AfterhoursNex-1.2-film.mp4"], stdin=subprocess.PIPE)
began = time.time()
with sync_playwright() as p:
    b = p.chromium.launch(); pg = b.new_page(viewport={"width": 1920, "height": 1080})
    pg.goto(__import__("pathlib").Path("promo4.html").resolve().as_uri()); pg.evaluate("document.fonts.ready"); pg.wait_for_timeout(600)
    total = int(LENGTH * FPS)
    for i in range(total):
        pg.evaluate("t => window.seek(t)", i / FPS)
        ff.stdin.write(pg.screenshot(type="jpeg", quality=92))
        if i % 300 == 0: print(f"frame {i}/{total}  {time.time() - began:.0f}s", flush=True)
    b.close()
ff.stdin.close(); ff.wait()
print("done", ff.returncode, f"{time.time() - began:.0f}s", flush=True)

"""More screenshots for the film's moving app screens: a full-length Listen Now to scroll, the tab bar on its own,
Listen Now with the mini player, and the desktop show page. Needs mock.py serving the web page, as capture.py does."""
from playwright.sync_api import sync_playwright
from PIL import Image
URL = "http://127.0.0.1:8765/"
CSS = ":root{padding-top:44px !important} .tabbar{padding-bottom:22px !important}"
with sync_playwright() as p:
    b = p.chromium.launch(args=["--autoplay-policy=no-user-gesture-required"])
    ctx = b.new_context(viewport={"width": 390, "height": 822}, device_scale_factor=2)
    pg = ctx.new_page(); pg.clock.set_fixed_time("2026-10-20T21:30:00")
    pg.goto(URL); pg.wait_for_timeout(9000); pg.add_style_tag(content=CSS); pg.wait_for_timeout(600)
    pg.screenshot(path="p_tabbar_full.png")
    pg.add_style_tag(content=".tabbar{display:none !important}"); pg.wait_for_timeout(300)
    pg.screenshot(path="p_listen_tall.png", full_page=True)
    pg.add_style_tag(content=".tabbar{display:flex !important}"); pg.wait_for_timeout(300)
    pg.locator("button.pill").first.click(); pg.wait_for_timeout(2500)
    pg.mouse.move(5, 5); pg.wait_for_timeout(300)
    pg.screenshot(path="p_listen_playing.png")
    d = b.new_context(viewport={"width": 1440, "height": 773}).new_page(); d.clock.set_fixed_time("2026-10-20T21:30:00")
    d.goto(URL); d.wait_for_timeout(9000)
    d.locator('text="Shedunnit" >> visible=true').first.click(); d.wait_for_timeout(3000); d.screenshot(path="d_show.png")
    b.close()
full = Image.open("p_tabbar_full.png"); w, h = full.size
full.crop((0, h - 156, w, h)).save("p_tabbar.png")          # the bottom 78 points: the tab bar
print("captured; tall Listen Now is", Image.open("p_listen_tall.png").size)

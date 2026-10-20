"""Screenshots of the Afterhours web design (which AfterhoursNex now shares) for the film's phones and laptop.
Needs the web app's page served with sample data at http://127.0.0.1:8765 (see mock.py)."""
from playwright.sync_api import sync_playwright
URL = "http://127.0.0.1:8765/"
CSS = ":root{padding-top:44px !important} .tabbar{padding-bottom:22px !important}"
def phone(b, dark=False):
    ctx = b.new_context(viewport={"width": 390, "height": 822}, device_scale_factor=2, color_scheme="dark" if dark else "light")
    pg = ctx.new_page(); pg.clock.set_fixed_time("2026-10-20T21:30:00")
    pg.goto(URL); pg.wait_for_timeout(9000); pg.add_style_tag(content=CSS); pg.wait_for_timeout(600)
    return pg
with sync_playwright() as p:
    b = p.chromium.launch(args=["--autoplay-policy=no-user-gesture-required"])
    pg = phone(b); pg.screenshot(path="p_listen_light.png")
    pg.locator('nav.tabbar >> text="Discover"').click(); pg.wait_for_timeout(2500); pg.screenshot(path="p_discover.png")
    pg.locator('nav.tabbar >> text="Library"').click(); pg.wait_for_timeout(2500); pg.screenshot(path="p_library.png")
    pg.locator('text="Shedunnit" >> visible=true').first.click(); pg.wait_for_timeout(3000); pg.screenshot(path="p_show.png")
    pg.locator("button.pill").first.click(); pg.wait_for_timeout(2500)
    pg.locator("section.bar").click(position={"x": 40, "y": 30}); pg.wait_for_timeout(2500)
    pg.evaluate("""() => { for (const e of document.querySelectorAll('section.np *')) if (e.children.length === 0 && /No captions/.test(e.textContent)) e.style.visibility = 'hidden'; }""")
    pg.screenshot(path="p_np.png")
    dk = phone(b, dark=True); dk.screenshot(path="p_listen_dark.png")
    ctx = b.new_context(viewport={"width": 1440, "height": 773}, device_scale_factor=1)
    d = ctx.new_page(); d.clock.set_fixed_time("2026-10-20T21:30:00"); d.goto(URL); d.wait_for_timeout(9000)
    d.locator('text="Library" >> visible=true').first.click(); d.wait_for_timeout(2500); d.screenshot(path="d_library.png")
    b.close()
print("captured")

"""The AfterhoursNex promotional video, as a page whose animation runs on one timeline (rendered frame by frame)."""
import base64, io

def b64(path): return base64.b64encode(open(path, "rb").read()).decode()
SERIF_R = b64("../shared/src/commonMain/composeResources/font/liberation_serif_regular.ttf")
SERIF_I = b64("../shared/src/commonMain/composeResources/font/liberation_serif_italic.ttf")
BANNER = open("afterhoursnex-intro.svg", encoding="utf-8").read().replace('width="1280" height="640"', 'width="1920" height="960"', 1)
import os
ENDCARD = "data:image/png;base64," + b64("endcard.png" if os.path.exists("endcard.png") else "../assets/afterhoursnex-banner.png")
EASE = "cubic-bezier(.2,.75,.25,1)"

def a(name, dur, delay, ease=EASE, fill="both"):
    return f"animation:{name} {dur}s {ease} {delay:.2f}s {fill};"

I = {  # Material-style icons
    "play": "M8 5v14l11-7z", "pause": "M6 5h4v14H6zM14 5h4v14h-4z",
    "rew": "M11 18V6l-8.5 6 8.5 6zm.5-6 8.5 6V6l-8.5 6z", "fwd": "M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z",
    "home": "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z", "search": "M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16a6.47 6.47 0 0 0 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
    "explore": "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm2.2 12.2-7.7 3.3 3.3-7.7 7.7-3.3-3.3 7.7zM12 10.9a1.1 1.1 0 1 0 0 2.2 1.1 1.1 0 0 0 0-2.2z",
    "library": "M4 6H2v14a2 2 0 0 0 2 2h14v-2H4V6zm16-4H8a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2zm-2 5h-3v5.5a2.5 2.5 0 1 1-2.5-2.5c.57 0 1.08.19 1.5.51V5h4v2z",
    "gear": "M19.14 12.94c.04-.3.06-.61.06-.94s-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.48.48 0 0 0-.48-.41h-3.84a.47.47 0 0 0-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96a.48.48 0 0 0-.59.22L2.74 8.87a.47.47 0 0 0 .12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32a.48.48 0 0 0-.12-.61l-2.01-1.58zM12 15.6a3.6 3.6 0 1 1 0-7.2 3.6 3.6 0 0 1 0 7.2z",
    "moon": "M12 3a9 9 0 1 0 9 9c0-.46-.04-.92-.1-1.36A5.39 5.39 0 0 1 12.55 3.1 9.22 9.22 0 0 0 12 3z",
    "speed": "M20.38 8.57l-1.23 1.85a8 8 0 0 1-.22 7.58H5.07A8 8 0 0 1 15.58 6.85l1.85-1.23A10 10 0 0 0 3.35 19a2 2 0 0 0 1.72 1h13.85a2 2 0 0 0 1.74-1 10 10 0 0 0-.27-10.44zm-9.79 6.84a2 2 0 0 0 2.83 0l5.66-8.49-8.49 5.66a2 2 0 0 0 0 2.83z",
    "next": "M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z", "prev": "M6 6h2v12H6zm3.5 6 8.5 6V6z", "down": "M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z",
    "plus": "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z", "check": "M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z",
}
def icon(n, size=24, extra=""): return f'<svg class="ic" width="{size}" height="{size}" viewBox="0 0 24 24"{extra}><path d="{I[n]}"/></svg>'
ART = ["linear-gradient(135deg,#FEBB5F,#CC5286)", "linear-gradient(140deg,#8C6BFF,#221952)", "linear-gradient(160deg,#F37377,#392983)",
       "linear-gradient(135deg,#3BB7A8,#1B1444)", "linear-gradient(135deg,#F7926D,#6B2C70)", "linear-gradient(135deg,#B6448D,#140F33)",
       "linear-gradient(135deg,#9B7BFF,#F37377)", "linear-gradient(150deg,#FFD38A,#F37377 60%,#392983)"]
SHOWS = [("Midnight Mysteries", "MM"), ("The Quiet Library", "QL"), ("Small Hours Science", "SH"), ("City Pop Diaries", "CP"),
         ("Late Jazz Notes", "LJ"), ("Gadget Night", "GN"), ("Pixel Archive", "PA"), ("Slow Radio", "SR")]
def art(i, size, radius=14, font=None):
    name, ini = SHOWS[i % len(SHOWS)]
    return f'<div class="art" style="width:{size}px;height:{size}px;border-radius:{radius}px;background:{ART[i % len(ART)]};font-size:{font or size * .34:.0f}px">{ini}</div>'

def status(): return '<div class="sb"><span>23:47</span><span class="sb-r"><i></i><i></i><i class="bat"></i></span></div>'
def navbar(active):
    items = [("home", "Listen Now"), ("explore", "Discover"), ("library", "Library"), ("search", "Search"), ("gear", "Settings")]
    return '<div class="nav">' + "".join(f'<div class="nv{" on" if k == active else ""}"><span class="pill">{icon(k, 22)}</span><b>{l}</b></div>' for k, l in items) + "</div>"
def mini(title, show, i):
    return f'<div class="mini"><div class="mprog"></div>{art(i, 44, 8)}<div class="mt"><b>{title}</b><span>{show}</span></div>{icon("rew", 24)}{icon("pause", 26)}</div>'
def tap(x, y, t): return f'<div class="tap" style="left:{x}px;top:{y}px;{a("pm-tap", .7, t, "ease-out")}"></div>'

def phone(inner, t, extra=""): return f'<div class="phone" style="{a("pm-phone", 1.1, t)}{extra}"><div class="screen">{status()}{inner}</div></div>'
def copy(kicker, title, body, t):
    return (f'<div class="copy"><div class="kick" style="{a("pm-up", .9, t)}">{kicker}</div>'
            f'<h2 style="{a("pm-up", 1.0, t + .12)}">{title}</h2><p style="{a("pm-up", 1.0, t + .3)}">{body}</p></div>')
def scene(start, dur, body, cls=""): return f'<section class="scene {cls}" style="{a("pm-scene", dur, start, "linear")}">{body}</section>'
def words(text, t0, step, cls="sw"):
    return " ".join(f'<span class="{cls}" style="{a("pm-word", 1.0, t0 + i * step)}">{w}</span>' for i, w in enumerate(text.split()))

# ---- scenes ----
S2, S3, S4, S5, S6, S7, S8, END = 7.0, 12.6, 19.2, 25.8, 33.2, 38.6, 43.4, 48.0
opening = scene(0, S2, f'<div class="banner">{BANNER}</div>')
slogan = scene(S2, S3 - S2, f'''<div class="glow"></div>
  <div class="slogan"><div>{words("How many times", S2 + .5, .14)}</div><div>{words("do I have left to see you", S2 + .95, .12)}</div><div>{words("again in my life?", S2 + 1.75, .14)}</div></div>
  <div class="sub" style="{a("pm-up", 1.0, S2 + 3.0)}">AfterhoursNex · a podcast player for the late hours</div>''')

def episode_row(i, meta, title, desc, t):
    return (f'<div class="ep" style="{a("pm-up", .8, t)}"><div class="ept"><span class="meta">{meta}</span><b>{title}</b><span class="desc">{desc}</span></div>'
            f'<span class="pb">{icon("play", 22)}</span></div>')
up_next = "".join(f'<div class="card" style="{a("pm-up", .8, S3 + 1.1 + k * .12)}">{art(k, 150, 14)}<div class="bar"><i style="--a:.1;--b:{w};{a("pm-progress", 1.6, S3 + 1.6 + k * .15)}"></i></div><b>{tt}</b><span>{SHOWS[k][0]}</span></div>'
                  for k, (tt, w) in enumerate([("The locked-room problem", ".62"), ("Why the night sky is dark", ".35"), ("Rainy evenings in Tokyo", ".8")]))
listen = phone(f'''<div class="pt">Listen Now</div><div class="ps">Good evening. Here’s what’s new on your shelf.</div>
  <div class="sec">Up Next</div><div class="row">{up_next}</div>
  <div class="sec">Latest episodes</div>
  {episode_row(0, "Midnight Mysteries · Today", "The locked-room problem", "Why the impossible crime still fascinates readers, and how the golden age writers solved it.", S3 + 1.7)}
  {episode_row(4, "Late Jazz Notes · Sunday", "Blue in green, slowly", "A whole hour on one ballad and the musicians who changed it.", S3 + 1.85)}
  {episode_row(2, "Small Hours Science · Saturday", "Why the night sky is dark", "Olbers’ paradox, the age of the universe, and light that hasn’t arrived yet.", S3 + 2.0)}
  {mini("The locked-room problem", "Midnight Mysteries", 0)}{navbar("home")}''', S3 + .25)
s3 = scene(S3, S4 - S3, copy("Listen Now", "Your shelf,<br>ready on first launch", "Thirty hand-picked shows find themselves in Apple’s podcast directory, or on their own websites when there’s no listing.", S3 + .45) + listen)

results = "".join(f'''<div class="res" style="{a("pm-up", .7, S4 + 2.5 + k * .12)}">{art(k + 3, 64, 10)}<div class="rt"><b>{n}</b><span>{m}</span></div>
  {'<span class="btn"><span class="b1" style="' + a("pm-out", .3, S4 + 3.9) + '">Add</span><span class="b2" style="' + a("pm-in", .35, S4 + 4.0) + '">Open</span></span>' if k == 0 else '<span class="btn"><span>Add</span></span>'}</div>'''
                  for k, (n, m) in enumerate([("City Pop Diaries", "Night Shift Radio · Music"), ("Tokyo After Dark", "Kaze Studio · Music"), ("Crate Digging Japan", "Low Light · Music")]))
search = phone(f'''<div class="pt">Search</div>
  <div class="field">{icon("search", 22)}<span class="typed" style="{a("pm-type", 1.0, S4 + 1.2, "steps(8, end)")}">city pop</span><span class="caret" style="animation:pm-blink 1s steps(2, start) {S4:.2f}s 6 both"></span></div>
  <div class="hint" style="{a("pm-fade", .5, S4 + .6)}">Shows, topics, or a feed link</div>
  {results}{tap(360, 287, S4 + 3.7)}
  <div class="toast" style="{a("pm-pop", .5, S4 + 4.3)}">Added City Pop Diaries to your library.</div>{navbar("search")}''', S4 + .25)
s4 = scene(S4, S5 - S4, copy("Search", "Find any show,<br>or paste a link", "Search Apple’s directory, or paste a feed, an Apple Podcasts link or a website. If a feed stops working, AfterhoursNex reads the show’s website instead.", S4 + .45) + search)

playing = phone(f'''<div class="nph">{icon("down", 26)}<span>Now Playing</span><i></i></div>
  <div class="npart" style="{a("pm-ken", 7, S5, "ease-in-out")}">{art(7, 300, 22, 96)}</div>
  <div class="npt">The locked-room problem</div><div class="nps">Midnight Mysteries</div>
  <div class="slider"><i style="--a:.30;--b:.46;{a("pm-progress", 6.5, S5 + .8, "linear")}"></i><b style="{a("pm-knob", 6.5, S5 + .8, "linear")}"></b></div>
  <div class="times"><span>18:42</span><span>−27:10</span></div>
  <div class="ctl"><span class="sk">{icon("rew", 30)}<small>15</small></span><span class="big">{icon("pause", 40)}</span><span class="sk">{icon("fwd", 30)}<small>30</small></span></div>
  <div class="chips"><span class="chip">{icon("speed", 18)}<span class="swap"><span style="{a("pm-out", .3, S5 + 2.6)}">1.0×</span><span style="{a("pm-in", .35, S5 + 2.7)}">1.2×</span></span></span>
    <span class="chip">{icon("moon", 18)}<span class="swap"><span style="{a("pm-out", .3, S5 + 4.6)}">Sleep timer</span><span style="{a("pm-in", .35, S5 + 4.7)}">30 min left</span></span></span>{icon("next", 26)}</div>
  {tap(132, 744, S5 + 2.4)}{tap(266, 744, S5 + 4.4)}
  <div class="menu" style="animation:pm-menu 1.2s {EASE} {S5 + 3.5:.2f}s both"><div>Off</div><div>15 minutes</div><div class="sel">30 minutes</div><div>45 minutes</div><div>End of this episode</div></div>''', S5 + .25)
s5 = scene(S5, S6 - S5, copy("Now Playing", "Pick up exactly<br>where you left off", "Speeds from 0.8× to 2×, back 15 and forward 30 seconds, and a sleep timer that can stop at the end of the episode.", S5 + .45) + playing)

lock = phone(f'''<div class="lock"><div class="lt" style="{a("pm-up", .9, S6 + .5)}">23:47</div><div class="ld" style="{a("pm-up", .9, S6 + .6)}">Monday 19 October</div>
  <div class="media" style="{a("pm-up", .9, S6 + 1.0)}"><div class="mh">{art(0, 64, 12)}<div><b>The locked-room problem</b><span>Midnight Mysteries</span></div></div>
    <div class="mbar"><i style="--a:.46;--b:.55;{a("pm-progress", 4, S6 + 1.4, "linear")}"></i></div>
    <div class="mc">{icon("prev", 30)}{icon("rew", 30)}<span class="mplay">{icon("pause", 34)}</span>{icon("fwd", 30)}{icon("next", 30)}</div></div></div>''', S6 + .25, "")
s6 = scene(S6, S7 - S6, copy("In the background", "Keeps playing<br>with the screen off", "Lock-screen, notification, headphone and car controls on Android, so the story carries on while you do something else.", S6 + .45) + lock)

grid = "".join(f'<div class="tile" style="{a("pm-up", .7, S7 + .9 + k * .05)}">{art(k, 132, 12)}<b>{SHOWS[k % 8][0]}</b></div>' for k in range(10))
desktop = f'''<div class="laptop" style="{a("pm-phone", 1.2, S7 + .3)}"><div class="lscreen"><div class="win"><div class="wbar"><i></i><i></i><i></i><span>AfterhoursNex</span></div>
  <div class="wbody"><div class="rail">{"".join(f'<span class="{c}">{icon(k, 24)}</span>' for k, c in [("home", ""), ("explore", ""), ("library", "on"), ("search", ""), ("gear", "")])}</div>
  <div class="wcontent"><div class="wt">Library</div><div class="wsub">30 shows</div><div class="grid">{grid}</div></div></div>
  {mini("Blue in green, slowly", "Late Jazz Notes", 4)}</div></div><div class="lbase"></div></div>
  <div class="phone small" style="{a("pm-phone", 1.1, S7 + .7)}"><div class="screen">{status()}<div class="pt">Listen Now</div><div class="ps">Good evening.</div><div class="sec">Up Next</div><div class="row">{up_next}</div>{navbar("home")}</div></div>'''
s7 = scene(S7, S8 - S7, f'<div class="center-copy"><h2 style="{a("pm-up", 1.0, S7 + .2)}">One app. Four systems.</h2>'
           f'<p style="{a("pm-up", 1.0, S7 + .4)}">Windows · macOS · Linux · Android</p></div>{desktop}', "s7")
endcard = scene(S8, END - S8 + 1, f'''<img class="endimg" src="{ENDCARD}" style="{a("pm-end", 4.6, S8, "cubic-bezier(.2,.7,.2,1)")}">
  <div class="free" style="{a("pm-up", 1.0, S8 + 1.2)}">Free · private · no account needed</div>''', "s8")

CSS = f"""
@font-face {{ font-family: NexSerif; src: url(data:font/ttf;base64,{SERIF_R}); }}
@font-face {{ font-family: NexSerif; font-style: italic; src: url(data:font/ttf;base64,{SERIF_I}); }}
* {{ box-sizing: border-box; }}
html, body {{ margin: 0; width: 1920px; height: 1080px; overflow: hidden; background: #110C2B; color: #F5EEF4; font-family: "Liberation Sans", Arial, sans-serif; }}
.bg {{ position: absolute; inset: 0; background: linear-gradient(#110C2B, #1B1444 60%, #2A1C58); }}
.dusk {{ position: absolute; inset: 0; background: radial-gradient(1500px 620px at 50% 118%, rgba(243,115,119,.30), rgba(204,82,134,.10) 42%, transparent 72%); }}
.scene {{ position: absolute; inset: 0; opacity: 0; }}
.banner {{ position: absolute; left: 0; top: 60px; width: 1920px; height: 960px; }}
@keyframes pm-scene {{ 0% {{ opacity: 0; transform: scale(1.012); }} 7% {{ opacity: 1; transform: none; }} 93% {{ opacity: 1; }} 100% {{ opacity: 0; transform: scale(.992); }} }}
@keyframes pm-up {{ from {{ opacity: 0; transform: translateY(28px); filter: blur(6px); }} to {{ opacity: 1; transform: none; filter: blur(0); }} }}
@keyframes pm-word {{ from {{ opacity: 0; transform: translateY(22px); filter: blur(8px); }} to {{ opacity: 1; transform: none; filter: blur(0); }} }}
@keyframes pm-fade {{ from {{ opacity: 0; }} to {{ opacity: 1; }} }}
@keyframes pm-phone {{ from {{ opacity: 0; transform: translateY(70px) rotate(1.5deg); }} to {{ opacity: 1; transform: none; }} }}
@keyframes pm-progress {{ from {{ transform: scaleX(var(--a)); }} to {{ transform: scaleX(var(--b)); }} }}
@keyframes pm-knob {{ from {{ left: calc(30% - 9px); }} to {{ left: calc(46% - 9px); }} }}
@keyframes pm-type {{ from {{ clip-path: inset(0 100% 0 0); }} to {{ clip-path: inset(0 0 0 0); }} }}
@keyframes pm-blink {{ to {{ visibility: hidden; }} }}
@keyframes pm-out {{ from {{ opacity: 1; transform: none; }} to {{ opacity: 0; transform: translateY(-10px); }} }}
@keyframes pm-in {{ from {{ opacity: 0; transform: translateY(10px); }} to {{ opacity: 1; transform: none; }} }}
@keyframes pm-pop {{ from {{ opacity: 0; transform: translate(-50%, 20px) scale(.92); }} to {{ opacity: 1; transform: translate(-50%, 0); }} }}
@keyframes pm-tap {{ 0% {{ opacity: 0; transform: translate(-50%, -50%) scale(.2); }} 25% {{ opacity: .55; }} 100% {{ opacity: 0; transform: translate(-50%, -50%) scale(1.5); }} }}
@keyframes pm-ken {{ from {{ transform: scale(1); }} to {{ transform: scale(1.05); }} }}
@keyframes pm-menu {{ 0% {{ opacity: 0; transform: translateY(16px) scale(.96); }} 20% {{ opacity: 1; transform: none; }} 80% {{ opacity: 1; transform: none; }} 100% {{ opacity: 0; transform: translateY(8px); }} }}
@keyframes pm-end {{ from {{ opacity: 0; transform: scale(1.06); filter: blur(10px); }} to {{ opacity: 1; transform: none; filter: blur(0); }} }}
.ic {{ fill: currentColor; flex: none; }}
/* the slogan */
.glow {{ position: absolute; left: 50%; top: 52%; width: 1300px; height: 700px; transform: translate(-50%, -50%); background: radial-gradient(closest-side, rgba(247,146,109,.22), rgba(204,82,134,.08) 55%, transparent); }}
.slogan {{ position: absolute; inset: 250px 0 auto; text-align: center; font-family: NexSerif; font-style: italic; font-size: 96px; line-height: 1.22; color: #F5EEF4; }}
.sw {{ display: inline-block; opacity: 0; }}
.sub {{ position: absolute; left: 0; right: 0; top: 760px; text-align: center; font-family: NexSerif; font-size: 30px; letter-spacing: 3px; color: #B7A9D6; opacity: 0; }}
/* copy beside the device */
.copy {{ position: absolute; left: 190px; top: 330px; width: 760px; }}
.copy > * {{ opacity: 0; }}
.kick {{ font-family: NexSerif; font-style: italic; font-size: 30px; color: #F7926D; letter-spacing: 1px; }}
.copy h2, .center-copy h2 {{ font-family: NexSerif; font-weight: 400; font-size: 86px; line-height: 1.06; margin: 16px 0 26px; color: #F5EEF4; }}
.copy p {{ font-size: 28px; line-height: 1.5; color: #C9BFD8; margin: 0; max-width: 680px; }}
/* the phone */
.phone {{ position: absolute; left: 1180px; top: 86px; width: 456px; height: 930px; border-radius: 68px; padding: 14px; background: linear-gradient(145deg, #2C2540, #0C0A14); box-shadow: 0 50px 120px rgba(0,0,0,.55), inset 0 0 0 2px rgba(255,255,255,.08); opacity: 0; }}
.phone.small {{ left: 1430px; top: 300px; transform-origin: top left; width: 330px; height: 672px; border-radius: 52px; padding: 10px; }}
.screen {{ position: relative; width: 100%; height: 100%; border-radius: 56px; overflow: hidden; background: #18181A; color: #EDE7EC; padding: 0 22px; }}
.phone.small .screen {{ border-radius: 44px; padding: 0 16px; zoom: .72; width: 138.9%; height: 138.9%; }}
.sb {{ display: flex; justify-content: space-between; padding: 22px 12px 10px; font-size: 17px; font-weight: 700; }}
.sb-r {{ display: flex; gap: 6px; align-items: center; }} .sb-r i {{ width: 16px; height: 11px; border-radius: 3px; background: #EDE7EC; opacity: .9; }} .sb-r i.bat {{ width: 26px; }}
.pt {{ font-size: 38px; font-weight: 700; margin-top: 14px; }}
.ps {{ font-size: 16px; color: #B9B3B8; margin-top: 4px; }}
.sec {{ font-size: 23px; font-weight: 700; margin: 22px 0 10px; }}
.row {{ display: flex; gap: 14px; }}
.card {{ width: 150px; flex: none; opacity: 0; }} .card b {{ display: block; font-size: 15px; margin-top: 8px; line-height: 1.25; }} .card span {{ font-size: 13px; color: #B9B3B8; }}
.bar {{ height: 4px; border-radius: 2px; background: #3A3640; margin-top: 8px; overflow: hidden; }} .bar i {{ display: block; height: 100%; background: #FF6E9F; transform-origin: left; transform: scaleX(var(--b)); }}
.art {{ display: flex; align-items: center; justify-content: center; font-family: NexSerif; font-style: italic; color: rgba(255,255,255,.92); flex: none; box-shadow: inset 0 0 0 1px rgba(255,255,255,.08); }}
.ep {{ display: flex; gap: 12px; padding: 12px 0; border-bottom: 1px solid #2A292C; opacity: 0; }} .ept {{ flex: 1; display: flex; flex-direction: column; gap: 3px; }}
.meta {{ font-size: 13px; color: #FF6E9F; font-weight: 700; }} .ept b {{ font-size: 17px; }} .desc {{ font-size: 14px; color: #B9B3B8; line-height: 1.35; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }}
.pb {{ width: 46px; height: 46px; border-radius: 50%; background: #3A2A31; color: #FFD9E3; display: flex; align-items: center; justify-content: center; flex: none; align-self: center; }}
.mini {{ position: absolute; left: 0; right: 0; bottom: 96px; display: flex; align-items: center; gap: 12px; padding: 10px 22px; background: #232226; }} .mini .ic {{ color: #EDE7EC; }}
.mprog {{ position: absolute; left: 0; top: 0; height: 2px; width: 46%; background: #FF6E9F; }}
.mt {{ flex: 1; display: flex; flex-direction: column; min-width: 0; }} .mt b {{ font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }} .mt span {{ font-size: 13px; color: #B9B3B8; }}
.nav {{ position: absolute; left: 0; right: 0; bottom: 0; height: 96px; display: flex; justify-content: space-around; align-items: flex-start; padding-top: 12px; background: #1F1E22; }}
.nv {{ display: flex; flex-direction: column; align-items: center; gap: 4px; color: #B9B3B8; font-size: 12px; }} .nv b {{ font-weight: 600; }}
.nv .pill {{ width: 58px; height: 32px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }} .nv.on .pill {{ background: #5C1236; color: #FFD9E3; }} .nv.on {{ color: #EDE7EC; }}
.field {{ display: flex; align-items: center; gap: 12px; margin-top: 18px; height: 60px; padding: 0 18px; border-radius: 6px; border: 2px solid #FF6E9F; font-size: 20px; }}
.typed {{ clip-path: inset(0 100% 0 0); }} .caret {{ width: 2px; height: 26px; background: #FF6E9F; margin-left: -10px; }}
.hint {{ font-size: 14px; color: #B9B3B8; margin: 8px 4px 10px; opacity: 0; }}
.res {{ display: flex; align-items: center; gap: 14px; padding: 12px 0; opacity: 0; }} .rt {{ flex: 1; display: flex; flex-direction: column; }} .rt b {{ font-size: 17px; }} .rt span {{ font-size: 14px; color: #B9B3B8; }}
.btn {{ position: relative; display: inline-grid; min-width: 84px; height: 40px; border-radius: 20px; background: #3A2A31; color: #FFD9E3; font-weight: 700; font-size: 15px; }}
.btn > span {{ grid-area: 1 / 1; display: flex; align-items: center; justify-content: center; }} .b2 {{ opacity: 0; color: #FF6E9F; }}
.toast {{ position: absolute; left: 50%; bottom: 120px; width: 380px; transform: translateX(-50%); background: #EDE7EC; color: #18181A; border-radius: 10px; padding: 16px 18px; font-size: 16px; font-weight: 600; opacity: 0; }}
.tap {{ position: absolute; width: 90px; height: 90px; border-radius: 50%; background: radial-gradient(circle, rgba(255,255,255,.75), rgba(255,255,255,0) 70%); opacity: 0; pointer-events: none; }}
.nph {{ display: flex; align-items: center; justify-content: space-between; margin-top: 8px; font-size: 16px; font-weight: 700; }} .nph i {{ width: 26px; }}
.npart {{ margin: 26px auto 22px; width: 300px; height: 300px; border-radius: 22px; overflow: hidden; box-shadow: 0 30px 60px rgba(0,0,0,.45); }}
.npt {{ text-align: center; font-size: 25px; font-weight: 700; }} .nps {{ text-align: center; font-size: 17px; color: #FF6E9F; margin-top: 6px; }}
.slider {{ position: relative; height: 6px; border-radius: 3px; background: #3A3640; margin: 30px 6px 8px; }} .slider i {{ position: absolute; inset: 0; border-radius: 3px; background: #FF6E9F; transform-origin: left; transform: scaleX(var(--b)); }}
.slider b {{ position: absolute; top: -6px; width: 18px; height: 18px; border-radius: 50%; background: #FF6E9F; left: calc(46% - 9px); }}
.times {{ display: flex; justify-content: space-between; font-size: 13px; color: #B9B3B8; margin: 0 6px; }}
.ctl {{ display: flex; align-items: center; justify-content: center; gap: 40px; margin-top: 16px; }} .sk {{ display: flex; flex-direction: column; align-items: center; font-size: 12px; color: #EDE7EC; }}
.big {{ width: 84px; height: 84px; border-radius: 50%; background: #FF6E9F; color: #3E0020; display: flex; align-items: center; justify-content: center; }}
.chips {{ display: flex; align-items: center; justify-content: center; gap: 14px; margin-top: 20px; color: #FF6E9F; }}
.chip {{ display: flex; align-items: center; gap: 8px; height: 40px; padding: 0 14px; border-radius: 20px; font-size: 16px; font-weight: 700; }}
.swap {{ display: inline-grid; }} .swap > span {{ grid-area: 1 / 1; white-space: nowrap; }} .swap > span:last-child {{ opacity: 0; }}
.menu {{ position: absolute; left: 150px; top: 520px; width: 240px; background: #2A292C; border-radius: 6px; padding: 8px 0; box-shadow: 0 16px 40px rgba(0,0,0,.5); opacity: 0; font-size: 16px; }}
.menu div {{ padding: 12px 20px; }} .menu .sel {{ background: #3A2A31; color: #FFD9E3; }}
.lock {{ position: absolute; inset: 0; background: radial-gradient(500px 420px at 50% 95%, rgba(243,115,119,.45), transparent 70%), linear-gradient(#140F33, #2A1C58); padding: 120px 22px 0; text-align: center; }}
.lt {{ font-size: 112px; font-weight: 300; letter-spacing: -2px; opacity: 0; }} .ld {{ font-size: 20px; color: #D9CFE6; opacity: 0; }}
.media {{ margin-top: 70px; text-align: left; background: rgba(28,22,48,.78); border: 1px solid rgba(255,255,255,.1); border-radius: 26px; padding: 20px; opacity: 0; }}
.mh {{ display: flex; gap: 14px; align-items: center; }} .mh div {{ display: flex; flex-direction: column; }} .mh b {{ font-size: 18px; }} .mh span {{ font-size: 15px; color: #C9BFD8; }}
.mbar {{ height: 5px; border-radius: 3px; background: rgba(255,255,255,.18); margin: 18px 0 12px; overflow: hidden; }} .mbar i {{ display: block; height: 100%; background: #FEBB5F; transform-origin: left; transform: scaleX(var(--b)); }}
.mc {{ display: flex; justify-content: space-around; align-items: center; color: #F5EEF4; }} .mplay {{ width: 64px; height: 64px; border-radius: 50%; background: #F5EEF4; color: #221952; display: flex; align-items: center; justify-content: center; }}
.center-copy {{ position: absolute; left: 0; right: 0; top: 70px; text-align: center; }} .center-copy > * {{ opacity: 0; }}
.center-copy h2 {{ margin: 0 0 10px; font-size: 76px; }} .center-copy p {{ font-family: NexSerif; font-size: 30px; letter-spacing: 6px; color: #B7A9D6; margin: 0; }}
.s7 .laptop {{ position: absolute; left: 230px; top: 300px; width: 1180px; opacity: 0; }}
.lscreen {{ height: 690px; border-radius: 26px 26px 0 0; background: #0C0A14; padding: 18px; box-shadow: 0 40px 100px rgba(0,0,0,.5), inset 0 0 0 2px rgba(255,255,255,.07); }}
.lbase {{ height: 30px; margin: 0 -60px; border-radius: 0 0 26px 26px; background: linear-gradient(#3A3350, #1A1626); }}
.win {{ position: relative; height: 100%; border-radius: 10px; overflow: hidden; background: #18181A; }}
.wbar {{ height: 40px; display: flex; align-items: center; gap: 8px; padding: 0 16px; background: #222126; font-size: 14px; color: #B9B3B8; }} .wbar i {{ width: 13px; height: 13px; border-radius: 50%; background: #F37377; }} .wbar i:nth-child(2) {{ background: #FEBB5F; }} .wbar i:nth-child(3) {{ background: #6BCB77; }} .wbar span {{ margin-left: 14px; }}
.wbody {{ display: flex; height: calc(100% - 40px - 64px); }}
.rail {{ width: 88px; display: flex; flex-direction: column; align-items: center; gap: 26px; padding-top: 24px; color: #B9B3B8; background: #1D1C20; }} .rail span {{ width: 56px; height: 32px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }} .rail .on {{ background: #5C1236; color: #FFD9E3; }}
.wcontent {{ flex: 1; padding: 22px 30px; overflow: hidden; }} .wt {{ font-size: 32px; font-weight: 700; }} .wsub {{ font-size: 15px; color: #B9B3B8; margin-bottom: 16px; }}
.grid {{ display: grid; grid-template-columns: repeat(5, 132px); gap: 22px 26px; }} .tile {{ opacity: 0; }} .tile b {{ display: block; font-size: 14px; margin-top: 8px; }}
.win .mini {{ bottom: 0; padding: 10px 24px; }}
.s8 .endimg {{ position: absolute; left: 0; top: 40px; width: 1920px; height: 960px; opacity: 0; }}
.free {{ position: absolute; left: 0; right: 0; top: 968px; text-align: center; font-family: NexSerif; font-style: italic; font-size: 30px; color: #E9DDEE; opacity: 0; }}
.fadeout {{ position: absolute; inset: 0; background: #000; opacity: 0; animation: pm-fade .7s linear {END - .7:.2f}s both; }}
"""
page = f"""<!doctype html><html><head><meta charset="utf-8"><title>AfterhoursNex promotional video</title><style>{CSS}</style></head>
<body><div class="bg"></div><div class="dusk"></div>{opening}{slogan}{s3}{s4}{s5}{s6}{s7}{endcard}<div class="fadeout"></div>
<script>
window.seek = t => {{
  for (const s of document.querySelectorAll('svg')) if (s.pauseAnimations) {{ s.pauseAnimations(); s.setCurrentTime(t); }}
  for (const an of document.getAnimations()) {{ an.pause(); an.currentTime = t * 1000; }}
}};
window.LENGTH = {END};
</script></body></html>"""
open("promo.html", "w", encoding="utf-8").write(page)
print("promo.html:", len(page) // 1024, "KB; length", END, "seconds")

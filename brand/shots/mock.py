"""A stand-in for the Afterhours helper, so the web app can be photographed with shows and episodes in it.
Run: python3 mock.py path/to/afterhours-web-page.html, then python3 capture.py."""
import json, re, io, hashlib, colorsys, struct, wave, datetime, threading, sys
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from PIL import Image, ImageDraw, ImageFont
PAGE = open(sys.argv[1] if len(sys.argv) > 1 else 'index.html', encoding='utf-8').read().replace('<span class="brand-name">Afterhours</span>', '<span class="brand-name">Afterhours<span style="color:var(--accent)">Nex</span></span>')
CATALOG = json.loads(re.search(r'<script[^>]*id="catalog"[^>]*>(.*?)</script>', PAGE, re.S).group(1))
SHOWS = {}
def walk(o):
    if isinstance(o, dict):
        if 'term' in o and 'title' in o: SHOWS[o['term']] = o
        for v in o.values(): walk(v)
    elif isinstance(o, list):
        for v in o: walk(v)
walk(CATALOG)
def key(o): return o.get('key') or hashlib.md5(o['title'].encode()).hexdigest()[:8]
BYKEY = {key(o): o for o in SHOWS.values()}
PORT = 8765
BASE = f'http://127.0.0.1:{PORT}'
def result(o):
    k = key(o); cid = int(hashlib.md5(k.encode()).hexdigest()[:7], 16)
    return {"collectionId": cid, "trackId": cid, "collectionName": o.get('match') or o['title'], "trackName": o.get('match') or o['title'],
            "artistName": o.get('author') or o.get('artist') or '', "feedUrl": f"{BASE}/feed/{k}", "artworkUrl600": f"{BASE}/art/{k}.png",
            "artworkUrl100": f"{BASE}/art/{k}.png", "primaryGenreName": "Podcasts", "trackCount": 120, "releaseDate": "2026-10-19T08:00:00Z",
            "collectionViewUrl": f"https://podcasts.apple.com/podcast/id{cid}"}
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
ART = {}
def art(k):
    if k in ART: return ART[k]
    o = BYKEY.get(k, {'title': k}); h = int(hashlib.md5(k.encode()).hexdigest()[:4], 16) / 65535
    size = 600; im = Image.new('RGB', (size, size)); d = ImageDraw.Draw(im)
    c1 = colorsys.hls_to_rgb(h, .55, .55); c2 = colorsys.hls_to_rgb((h + .12) % 1, .32, .6)
    for y in range(size):
        t = y / size; d.line([(0, y), (size, y)], fill=tuple(int(255 * (a * (1 - t) + b * t)) for a, b in zip(c1, c2)))
    letter = (o.get('title') or '?').strip()[:1].upper()
    f = ImageFont.truetype(FONT, 300); w = d.textlength(letter, font=f)
    d.text(((size - w) / 2, 110), letter, font=f, fill=(255, 255, 255))
    d.text((40, 520), (o.get('title') or '')[:26], font=ImageFont.truetype(FONT, 34), fill=(255, 255, 255))
    buf = io.BytesIO(); im.save(buf, 'PNG'); ART[k] = buf.getvalue(); return ART[k]
EPS = ["Where it all began", "The night shift", "A record that changed everything", "Listener questions", "Behind the scenes",
       "The long interview", "Notes from the road", "Season finale", "Rainy evenings in Tokyo", "The locked-room problem", "Blue in green, slowly", "Why the night sky is dark"]
def feed(k):
    o = BYKEY.get(k, {'title': k, 'author': ''}); now = datetime.datetime(2026, 10, 20, 7, 0)
    items = []
    off = int(hashlib.md5(k.encode()).hexdigest()[:4], 16) % len(EPS)
    for i, t in enumerate(EPS[off:] + EPS[:off]):
        day = now - datetime.timedelta(days=i * 3 + (i > 0))
        dur = 1500 + (hash(k + t) % 3000)
        items.append(f"""<item><title>{t}</title><guid>{k}-{i}</guid><pubDate>{day.strftime('%a, %d %b %Y %H:%M:%S')} +0000</pubDate>
<description>In this episode of {o['title']}, a closer look at {t.lower()}, with stories, music and a few surprises along the way.</description>
<itunes:duration>{dur}</itunes:duration><enclosure url="{BASE}/audio.wav" type="audio/wav" length="1000000"/></item>""")
    return f"""<?xml version="1.0" encoding="UTF-8"?><rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel>
<title>{o.get('match') or o['title']}</title><itunes:author>{o.get('author','')}</itunes:author><description>{o.get('why','')}</description>
<itunes:image href="{BASE}/art/{k}.png"/>{''.join(items)}</channel></rss>""".encode()
buf = io.BytesIO()
with wave.open(buf, 'wb') as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(8000); w.writeframes(b'\0\0' * 8000 * 120)
AUDIO = buf.getvalue()
class H(BaseHTTPRequestHandler):
    def log_message(self, *a): pass
    def send(self, body, type_='application/json', code=200):
        if isinstance(body, (dict, list)): body = json.dumps(body).encode()
        self.send_response(code); self.send_header('Content-Type', type_); self.send_header('Content-Length', str(len(body)))
        self.send_header('Accept-Ranges', 'bytes'); self.end_headers(); self.wfile.write(body)
    def do_GET(self):
        u = urlparse(self.path); q = {k: v[0] for k, v in parse_qs(u.query).items()}; p = u.path
        if p in ('/', '/index.html'): return self.send(PAGE.encode(), 'text/html; charset=utf-8')
        if p == '/api/ping': return self.send({"ok": True, "version": "4.1.0", "client": "local", "platform": "mac"})
        if p == '/api/search':
            term = q.get('term', ''); o = SHOWS.get(term)
            res = [result(o)] if o else [result(x) for x in list(SHOWS.values())[:6]]
            return self.send({"resultCount": len(res), "results": res})
        if p == '/api/lookup': return self.send({"results": []})
        if p == '/api/fetch':
            url = q.get('url', ''); m = re.search(r'/feed/([^/?#]+)', url)
            return self.send(feed(m.group(1)) if m else b'', 'application/rss+xml; charset=utf-8', 200 if m else 404)
        if p == '/api/image':
            m = re.search(r'/art/([^/.]+)', q.get('url', '')); return self.send(art(m.group(1)) if m else b'', 'image/png')
        if p.startswith('/art/'): return self.send(art(p[5:].split('.')[0]), 'image/png')
        if p.startswith('/feed/'): return self.send(feed(p[6:]), 'application/rss+xml')
        if p == '/audio.wav': return self.send(AUDIO, 'audio/wav')
        if p == '/api/prefs': return self.send({"window": True, "browser": "", "native": False})
        if p == '/api/phone': return self.send({"on": False, "url": "", "address": ""})
        if p == '/api/captions': return self.send({"available": True, "engine": "faster-whisper", "models": ["base", "small"], "sizes": {}, "versions": {}})
        if p.startswith('/api/captions/'): return self.send({"state": "idle"})
        return self.send({"error": "not found"}, code=404)
    def do_POST(self): return self.send({"ok": True})
srv = ThreadingHTTPServer(('127.0.0.1', PORT), H)
print(f"mock helper on {BASE}: {len(SHOWS)} shows in the catalogue", flush=True)
srv.serve_forever()

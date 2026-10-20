# Brand artwork

These scripts make the AfterhoursNex banner and promotional video. They need Python 3 with `fontTools`, `Pillow`, `numpy` and `playwright` (run `playwright install chromium` once), plus `ffmpeg`.

Run them from this folder, in order:

| Script | What it makes |
| --- | --- |
| `glyphs.py` | `glyphs.json`: the letter outlines and their kerned positions, in Liberation Serif |
| `banner.py` | `afterhoursnex-intro.svg`: the animated banner |
| `render_banner.py` | `afterhoursnex-intro.gif` and `afterhoursnex-banner.png` |
| `music.py` | `music.wav`: the promo's soundtrack |
| `promo.py` | `promo.html`: the promo's scenes. It needs `endcard.png`, the banner's last frame at 1920 × 960. |
| `render_promo.py` | `AfterhoursNex-promo.mp4`: 48 seconds at 1080p and 30 frames a second |
| `promo2.py` | `promo2.html` and `timing.json`: the longer film, with the written title and the pen-drawn x. It reuses the device mock-ups from `promo.py`. |
| `music2.py` | `music2.wav`: the film's soundtrack, with the x's sounds timed from `timing.json` |
| `render2.py` | `AfterhoursNex-film.mp4`: 86 seconds at 1080p and 60 frames a second (about 15 minutes to render) |
| `promo3.py` | `promo3.html` and `timing3.json`: the current film, 37 bars at 96 BPM (92.5 seconds), with every scene, entrance and stroke of the x timed to the beat. It builds on `promo.py` and `promo2.py`. |
| `music3.py` | `music3.wav`: the film's soundtrack, a late-night groove arranged to the picture |
| `render3.py` | `AfterhoursNex-film.mp4`: 92.5 seconds at 1080p and 60 frames a second (about 15 minutes to render) |
| `promo4.py`, `music4.py`, `render4.py` | The 1.2 film: seven chapters on a 90 BPM groove (72 seconds, 60 fps), in the app's own type and colours. They make `AfterhoursNex-1.2-film.mp4`. |
| `shots/` | The app screens shown in the film: screenshots of the design AfterhoursNex shares with the Afterhours web app. `mock.py` serves the web page with sample shows; `capture.py` takes the phone, dark-mode, Now Playing and desktop screenshots, and `capture2.py` the ones the film animates (a full-length Listen Now, the tab bar, the mini player and a desktop show page). |
| `peek3.py` | Close-ups of the x being written (`python3 peek3.py x`), or frames at given times, to check before rendering |
| `peek2.py` | Close-ups of the x sequence (`python3 peek2.py x`), or frames at given times (`python3 peek2.py 3.5,24,81`), to check before rendering |

To change the slogan, edit the `slogan` text in `glyphs.py` and the slogan scene in `promo.py`, then run the scripts again.

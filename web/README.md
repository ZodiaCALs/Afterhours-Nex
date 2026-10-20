# AfterhoursNex Web

The lightest way to run AfterhoursNex: in your browser, with a small helper on your computer.

- **Run it:** `python3 afterhoursnex-web.py`. It needs Python 3.9 or later. It opens http://127.0.0.1:8790 in your browser, and you can add the page to your dock or home screen from there.
- **Memory:** the helper uses only Python's standard library, around 30 MB, plus the browser tab.
- **Optional add-ons**, loaded only if you ask for them:
  - **Auto captions:** `python3 afterhoursnex-web.py --setup-captions` installs faster-whisper.
  - **Its own window:** `--window` uses pywebview instead of a browser tab, at some cost in memory.
- **Your data:** the library lives in the browser. The helper keeps captions and preferences in `~/.afterhoursnex-web`.
- **On your phone:** turn on phone access in Settings, and open its link on a phone on the same Wi-Fi.

The web version has the Afterhours web app's features: your shelf, Discover, search, playback with speeds and a sleep timer, captions and transcripts, three languages, and phone access. Radio, video podcasts and audio imports are in the desktop and Android apps.

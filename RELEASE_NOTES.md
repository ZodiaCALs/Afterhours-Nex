# AfterhoursNex 1.2.0

*How many times do I have left to see you again in my life?*

AfterhoursNex 1.2.0 is a big release:
- **Where it runs:** a borderless window, and the same features on every system.
- **What you can hear:** radio from around the world, single episodes on any topic, YouTube and Bilibili videos as audio, and your own audio files.
- **Staying current:** it lets you know when there's a new version.
- **Weight:** it's much lighter on memory.

## What's new in 1.2.0

### Light, and in your browser too

- **AfterhoursNex Web.** The lightest way to run AfterhoursNex: in your browser, with a small helper that uses about 30 MB of memory. Download `AfterhoursNex-1.2.0-Web.zip` and run `python3 afterhoursnex-web.py`.
- **A much lighter desktop player.** The desktop app now plays through its built-in FFmpeg and the system's own sound output, instead of a full media framework.
  - **Memory:** far less of it.
  - **Formats:** every one, including Opus, Ogg and FLAC, with no converting first.
  - **Radio:** every station, whatever its format.
- **Reduce effects.** Settings › Appearance can turn off the blurred cover behind Now Playing and the glow behind pages. They take the most graphics memory.
- **Tighter memory settings.** The desktop app uses a simpler garbage collector and compiler, which hand memory back to the system. Graphics, cover-art and show-note caches are smaller, and the blurred backdrop in Now Playing uses a small copy of the cover.

### The same on every system

- **Your computer, for your phone.** On Android, Settings › Your computer pairs the phone with AfterhoursNex on your computer, using the phone link from the computer's Settings › Use on your phone.
  - **What the phone gets:** auto captions and video podcasts, made on the computer and played on the phone over Wi-Fi.
  - **What it costs the phone:** nothing heavy, because there's no speech model or video tool on the phone itself.
- **Borderless windows resize from every edge** on Windows and Linux, as on macOS.

### Your shelf, your way

- **A menu for every show.** Right-click a show in the sidebar, or long-press it in the Library, to rename, pin, group, change its cover or remove it.
  - **Rename:** your names stay through refreshes.
  - **Pin to top:** pinned shows stay at the top.
  - **Groups:** shows gather under headings you choose.
- **Drag to reorder.** Hold a show in the sidebar and drag it. It lifts and follows your pointer, and the others slide aside to make room.
- **Covers of your own.**
  - **Upload an image,** or design a cover from the show's name or any words, with a choice of gradients and serif or sans-serif type.
  - **Renamed shows** take their new name's initial on their cover.
- **Undo.** Removing a show offers Undo, which brings it back in its place, with its episodes.

### Your own audio

- **Import audio files.** Bring in MP3, AAC, M4A, Ogg, Opus, FLAC, WAV and more, with **Import audio** in the Library.
  - **On this device:** they gather in a show of their own, called "On this device".
  - **Like any episode:** they keep their place, and can be captioned.
  - **On Android:** the files stay playable after a restart.

### Staying up to date

- **Update checker.** Once a day, AfterhoursNex looks for a new release on GitHub.
  - **When one is out:** Listen Now says so, with a link to the right download for your system.
  - **Settings › Updates:** turn it off, or check now.

### Look and feel

- **Readable everywhere.** In dark mode, every setting's title is now clearly visible.
- **Now Playing:** its controls sit clear of the macOS window buttons, and caption lines are a calmer size.
- **Real frosted glass.** The player bar, the mini player and the tab bar now blur what passes beneath them, as Apple, Microsoft and Google's systems do. On phones, the tab bar floats over the page. Reduce effects turns the blur off.
- **Menus, redesigned.** The right-click menu, and the speed and sleep menus, are rounded glass panels with soft shadows. Choices are highlighted as you point at them, and the one in use has a tick. The right-click menu opens right where you click.
- **One glass material.** Buttons, fields, chips, the playing row, cards, menus and messages share one translucent glass, with a fine rim and a soft sheen. Buttons follow Apple's style: tinted or filled, and they dim gently when pressed.
- **Liquid-glass switches.**
  - **The track:** glass, with a bright rim and sheen.
  - **The knob:** glossy, it stretches as it slides and swells when you press it.
- **Transparency you choose.** Settings › Appearance has a slider, from solid to see-through, for the sidebar, player and tab bar.
- **Covers in Discover.** Every suggested show now shows its cover art. It's fetched as the card comes into view, and remembered.

- **A borderless window.**
  - **macOS:** the window buttons sit over the sidebar, and the title bar is part of the app.
  - **Windows and Linux:** AfterhoursNex draws its own window buttons. Drag the top to move the window.
- **Translucent surfaces.** The sidebar, player bar and mini player sit over a soft glow, with fine edges.
- **A calmer, more readable dark mode.** A deeper background, and brighter secondary text that's easy to read. Light mode's grey text is firmer too.

### Finding things to hear

- **Episode previews.** In topic search, tap an episode, or a video, to see its cover, show, date, length and notes, then play it or open its show.
- **Preview before you add.** In Discover and Search, a show opens as a preview: its episodes play from there, and **Add to library** keeps it.
- **Search by topic.** Switch Search to **Episodes & topics** and type something like *cyberpunk edgerunners review*. You get single episodes about it, best matches first, as well as the shows.
- **Radio.** A new Radio tab plays public FM and AM stations live, from your region or a dozen others, with a station search. Stations come from Radio Browser, a free directory of public radio streams.
- **Video podcasts, as audio.** One click in Settings, or in Search, adds them. AfterhoursNex downloads its video tool, about 35 MB, only if you want it, and keeps it up to date. YouTube and Bilibili then work as audio:
  - **Videos** turn up in topic searches.
  - **Channels and playlists** can be added as shows, by pasting their link.
  - **Audio** is fetched in the background, with its progress in the player bar.

### Captions

- **Steady captions with translations.** The rolling captions now follow the spoken line with one smooth, easing motion, at an even pace.
  - **With translation on,** translations arriving and lines growing no longer make the list race or stutter.
  - **Your own scrolling** is still respected: the list waits until you've stopped.

### Settings

- **Translation, right where you need it.** A Translate button beside the captions in Now Playing picks the language on the spot. Settings has a translation manager: try the service, and see or clear the translations kept for each language.

- **Models.** Settings › Models lists every speech recognition model, with its size and status, to download, use or delete.
  - **On desktop, with Argos Translate installed:** translation language packs too, for a LibreTranslate server on your computer.

### Lighter and faster

- **Much less memory.**
  - **The desktop app:** it runs with a capped memory budget, and hands memory back to the system while it's idle.
  - **The speech model:** it's let go 45 seconds after captions are made.
  - **Cover art:** it keeps a smaller cache.
  - **Show notes:** very long ones are trimmed when stored.
- **Still light.** No new libraries. The optional tools, yt-dlp and Argos Translate, stay outside the app and are used only if you install them.

## Also in AfterhoursNex

- **The Afterhours design, natively:** a sidebar with your shows on wide windows, a tab bar and mini player on phones, the web app's line icons, and its motion.
- **Captions that roll like lyrics,** with translation underneath (MyMemory, or your own LibreTranslate server).
- **Clean cache** in Settings › Storage, with each part in Pro mode.
- **In English, 简体中文 and 日本語,** and **phone access** from the desktop app over Wi-Fi.

- **Native on four systems.** One codebase, built with Kotlin Multiplatform and Compose Multiplatform. The desktop installers include their own runtime, so there's nothing else to install.
- **Plays in the background on Android,** with lock-screen, notification, headphone and car controls.
- **Your shelf, ready on first launch.** The 30 shows from the Afterhours poster find themselves in Apple's podcast directory, or on their own websites when there's no listing.
- **Shows from anywhere.** Add one from Search or Discover, or paste a feed, an Apple Podcasts link or the show's website. When a feed stops working, AfterhoursNex looks on the show's website, and **Change source** points a show somewhere new.
- **Listening:**
  - picks up where you left off, with **Up Next** on Listen Now
  - back 15 and forward 30 seconds, and speeds from 0.5× to 3×, picked from a list
  - a sleep timer (15 to 60 minutes, or the end of the episode)
  - plays the next episode automatically
- **Opening animation.** The sunset icon rises, "AfterhoursNex" is written letter by letter in a Times-style serif, and the x is drawn in two pen strokes with a sparkle where they cross. Then the slogan appears. A tap skips it, and Settings can turn it off.
- **Captions and transcripts.** Shows' own transcripts (WebVTT, SubRip and podcast JSON) appear line by line in Now Playing, rolling like lyrics, and any line you tap jumps there. You can load caption files too.
- **Auto captions on desktop.** Whisper, running on your computer through whisper.cpp, captions episodes that have none, starting from where you're listening. The model downloads once, and the captions are saved.
- **Pro mode** for auto captions, with the model, spoken language, translation into English, threads, beam size, temperature, silence threshold, line length and a vocabulary hint.
- **More formats.** The desktop app plays Opus, Ogg, FLAC, WebM, WMA and more by converting them with its built-in FFmpeg. Android plays them directly.
- **OPML import and export.**

## Downloads

| System | File |
| --- | --- |
| Windows 10 or 11 | `AfterhoursNex-1.2.0.exe` (installer) or `AfterhoursNex-1.2.0.msi` |
| macOS 13 or later, Apple silicon | `AfterhoursNex-1.2.0.dmg` |
| Ubuntu, Debian and similar | `afterhoursnex_1.2.0-1_amd64.deb` |
| Fedora, openSUSE and similar | `afterhoursnex-1.2.0-1.x86_64.rpm` |
| Windows or Linux, no installer | `AfterhoursNex-1.2.0-Windows-portable.zip`, `AfterhoursNex-1.2.0-Linux-portable.tar.gz` |
| Any computer with Python 3.9 or later, in a browser | `AfterhoursNex-1.2.0-Web.zip` (AfterhoursNex Web) |
| Android 8 or later | `AfterhoursNex-1.2.0-Android.apk`, or the `.aab` for Google Play |

`SHA256SUMS.txt` lists a checksum for every file.

## Before you open it

- **macOS:** if the build isn't signed, macOS says it can't check the app. Control-click AfterhoursNex in Applications and choose **Open**, or allow it in **System Settings › Privacy & Security**.
- **Windows:** if SmartScreen stops the installer, choose **More info › Run anyway**.
- **Linux:** install with `sudo apt install ./afterhoursnex_1.2.0-1_amd64.deb`. Audio needs the FFmpeg libraries (`sudo apt install ffmpeg`).
- **Android:** open the APK on your phone and allow installing apps from that source.

## Known limitations

- On phone access in a browser, each phone keeps its own shelf and progress, separate from the computer's.
- On Android, auto captions need a connected computer, and work for episodes from the web, not for files imported on the phone.
- AfterhoursNex starts with a fresh shelf; it doesn't read an Afterhours library.
- Video podcasts need yt-dlp on your computer (`brew install yt-dlp`, `winget install yt-dlp` or `pip install yt-dlp`). On a phone, they and auto captions come from your computer once you've connected it in Settings › Your computer.
- On desktop, media keys and the system's Now Playing controls aren't connected yet. Episodes in Opus, Ogg or FLAC take a moment to convert before they start.

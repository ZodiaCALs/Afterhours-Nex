# AfterhoursNex 1.0.0

*How many times do I have left to see you again in my life?*

The first release of AfterhoursNex: Afterhours rebuilt as a native app for Windows, macOS, Linux and Android from one Kotlin codebase. It draws its own interface, needs no browser and no Python, and plays in the background like any other podcast app.

## Highlights

- **Native on four systems.** One codebase, built with Kotlin Multiplatform and Compose Multiplatform. The desktop installers include their own runtime, so there's nothing else to install.
- **Plays in the background on Android,** with lock-screen, notification, headphone and car controls.
- **Your shelf, ready on first launch.** The 30 shows from the Afterhours poster find themselves in Apple's podcast directory, or on their own websites when there's no listing.
- **Shows from anywhere.** Add one from Search or Discover, or paste a feed, an Apple Podcasts link or the show's website. When a feed stops working, AfterhoursNex looks on the show's website, and **Change source** points a show somewhere new.
- **Listening:**
  - picks up where you left off, with **Up Next** on Listen Now
  - back 15 and forward 30 seconds, and speeds from 0.8× to 2×
  - a sleep timer (15 to 60 minutes, or the end of the episode)
  - plays the next episode automatically
- **Opening animation.** The sunset icon rises, "AfterhoursNex" is written letter by letter in a Times-style serif, and the x is drawn in two pen strokes with a sparkle where they cross. Then the slogan appears. A tap skips it, and Settings can turn it off.
- **Captions and transcripts.** Shows' own transcripts (WebVTT, SubRip and podcast JSON) appear line by line in Now Playing, and the full transcript jumps to any line you tap. You can load caption files too.
- **Auto captions on desktop.** Whisper, running on your computer through whisper.cpp, captions episodes that have none, starting from where you're listening. The model downloads once, and the captions are saved.
- **Pro mode** for auto captions, with the model, spoken language, translation into English, threads, beam size, temperature, silence threshold, line length and a vocabulary hint.
- **More formats.** The desktop app plays Opus, Ogg, FLAC, WebM, WMA and more by converting them with its built-in FFmpeg. Android plays them directly.
- **OPML import and export.**
- **Fits any screen:** a bottom bar on phones, and a side rail on tablets and desktop windows. Light and dark appearance.
- **In English, 简体中文 and 日本語.** The whole interface, with dates and durations, follows your system language, or the one you pick in Settings.
- **Use it on your phone.** The desktop app can share itself with phones on the same Wi-Fi through a private link and a QR code.

## Downloads

| System | File |
| --- | --- |
| Windows 10 or 11 | `AfterhoursNex-1.0.0.exe` (installer) or `AfterhoursNex-1.0.0.msi` |
| macOS 13 or later, Apple silicon | `AfterhoursNex-1.0.0.dmg` |
| Ubuntu, Debian and similar | `afterhoursnex_1.0.0-1_amd64.deb` |
| Fedora, openSUSE and similar | `afterhoursnex-1.0.0-1.x86_64.rpm` |
| Windows or Linux, no installer | `AfterhoursNex-1.0.0-Windows-portable.zip`, `AfterhoursNex-1.0.0-Linux-portable.tar.gz` |
| Android 8 or later | `AfterhoursNex-1.0.0-Android.apk`, or the `.aab` for Google Play |

`SHA256SUMS.txt` lists a checksum for every file.

## Before you open it

- **macOS:** if the build isn't signed, macOS says it can't check the app. Control-click AfterhoursNex in Applications and choose **Open**, or allow it in **System Settings › Privacy & Security**.
- **Windows:** if SmartScreen stops the installer, choose **More info › Run anyway**.
- **Linux:** install with `sudo apt install ./afterhoursnex_1.0.0-1_amd64.deb`. Audio needs the FFmpeg libraries (`sudo apt install ffmpeg`).
- **Android:** open the APK on your phone and allow installing apps from that source.

## Known limitations

- On phone access, each phone keeps its own shelf and progress, separate from the computer's, and auto captions aren't available there yet.
- Auto captions are on Windows, macOS and Linux only; on Android, shows' transcripts and caption files work.
- AfterhoursNex starts with a fresh shelf; it doesn't read an Afterhours library.
- On desktop, media keys and the system's Now Playing controls aren't connected yet. Episodes in Opus, Ogg or FLAC take a moment to convert before they start.

# AfterhoursNex 1.1.0

*How many times do I have left to see you again in my life?*

AfterhoursNex now looks and moves like Afterhours on the web, rolls its captions like lyrics, speaks three languages, and can share itself with your phone.

## What's new in 1.1.0

- **The Afterhours design.** The look of the Afterhours web app, natively:
  - **Wide windows:** a sidebar with your shows, Auto captions and Settings, and a floating player bar.
  - **Phones:** a translucent tab bar and a floating mini player.
  - **Everywhere:** episode rows with play pills, Discover's chips and banner, the Library grouped by category, show pages tinted with the show's colour, and Now Playing over the blurred cover.
- **The web app's motion.** On the same Fluent curves as the web app:
  - Pages arrive with its fade and rise, and shows open with its gentle zoom.
  - Now Playing rises in, and the player bar slides into place.
  - Everything you press gives a little, and lights up under a mouse pointer.
- **Captions that roll.** Now Playing shows the whole transcript as a rolling list:
  - The spoken line is bright and full-size; the rest are dimmed, and the edges fade out.
  - It glides to each new line, waits while you scroll, and jumps the audio to any line you tap.
- **Captions in your language too.** Turn on caption translation, and each line shows its translation underneath. It translates only the lines around the one being spoken, remembers them, and uses MyMemory (free, no account) or your own LibreTranslate server.
- **Clean cache.** Settings › Storage shows how much space cover art, episode lists, converted audio and translations take, and cleans them in one go. In Pro mode, it shows each part with its size and number of files, including saved captions and speech models, so you can clean them one at a time.
- **Finer icons.** The web app's own line icons, throughout.
- **Speed from a list.** Pick any speed from 0.5× to 3×, in Now Playing, the player bar and Settings.
- **Smoother lists.** Episode rows redraw only when their own episode changes, covers draw their monogram without layout work, and rows animate into place when episodes arrive.
- **In English, 简体中文 and 日本語.** The whole interface, with dates and durations, follows your system language, or the one you pick in Settings.
- **Use it on your phone.** The desktop app can share itself with phones on the same Wi-Fi through a private link and a QR code.
- **Up to date.** Built with Kotlin 2.4, Compose Multiplatform 1.12, Android Gradle Plugin 9.4 and Gradle 9.8. The Android app targets Android 16 (API 36).

## Also in AfterhoursNex

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
| Windows 10 or 11 | `AfterhoursNex-1.1.0.exe` (installer) or `AfterhoursNex-1.1.0.msi` |
| macOS 13 or later, Apple silicon | `AfterhoursNex-1.1.0.dmg` |
| Ubuntu, Debian and similar | `afterhoursnex_1.1.0-1_amd64.deb` |
| Fedora, openSUSE and similar | `afterhoursnex-1.1.0-1.x86_64.rpm` |
| Windows or Linux, no installer | `AfterhoursNex-1.1.0-Windows-portable.zip`, `AfterhoursNex-1.1.0-Linux-portable.tar.gz` |
| Android 8 or later | `AfterhoursNex-1.1.0-Android.apk`, or the `.aab` for Google Play |

`SHA256SUMS.txt` lists a checksum for every file.

## Before you open it

- **macOS:** if the build isn't signed, macOS says it can't check the app. Control-click AfterhoursNex in Applications and choose **Open**, or allow it in **System Settings › Privacy & Security**.
- **Windows:** if SmartScreen stops the installer, choose **More info › Run anyway**.
- **Linux:** install with `sudo apt install ./afterhoursnex_1.1.0-1_amd64.deb`. Audio needs the FFmpeg libraries (`sudo apt install ffmpeg`).
- **Android:** open the APK on your phone and allow installing apps from that source.

## Known limitations

- On phone access, each phone keeps its own shelf and progress, separate from the computer's, and auto captions aren't available there yet.
- Auto captions are on Windows, macOS and Linux only; on Android, shows' transcripts and caption files work.
- AfterhoursNex starts with a fresh shelf; it doesn't read an Afterhours library.
- On desktop, media keys and the system's Now Playing controls aren't connected yet. Episodes in Opus, Ogg or FLAC take a moment to convert before they start.

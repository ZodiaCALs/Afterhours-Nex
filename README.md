# AfterhoursNex

![AfterhoursNex](assets/afterhoursnex-intro.svg)

*How many times do I have left to see you again in my life?*

AfterhoursNex is a small, private podcast player for Windows, macOS, Linux and Android. It's built from one Kotlin codebase with Kotlin Multiplatform and Compose Multiplatform, so it's a real app on each system. It draws its own interface, needs no browser, and plays in the background.

**Version 1.1.0.** See [RELEASE_NOTES.md](RELEASE_NOTES.md) for what's new.

## Download

Get the file for your system from the project's GitHub Releases page.

| System | File | To install |
| --- | --- | --- |
| Windows 10 or 11 | `AfterhoursNex-1.1.0.exe` or `.msi` | Run it. It installs just for you; no administrator rights needed. |
| macOS 13 or later (Apple silicon) | `AfterhoursNex-1.1.0.dmg` | Open it and drag AfterhoursNex to Applications. |
| Ubuntu, Debian and similar | `afterhoursnex_1.1.0-1_amd64.deb` | `sudo apt install ./afterhoursnex_1.1.0-1_amd64.deb` |
| Fedora, openSUSE and similar | `afterhoursnex-1.1.0-1.x86_64.rpm` | `sudo dnf install ./afterhoursnex-1.1.0-1.x86_64.rpm` |
| Windows or Linux, without installing | `AfterhoursNex-1.1.0-Windows-portable.zip` or `AfterhoursNex-1.1.0-Linux-portable.tar.gz` | Unpack it anywhere and run AfterhoursNex inside. |
| Android 8 or later | `AfterhoursNex-1.1.0-Android.apk` | Open it on the phone and allow installing from that source. |

The desktop installers include their own Java runtime and FFmpeg, so there's nothing else to install. On Linux, JavaFX's audio playback also uses the system's FFmpeg libraries (`sudo apt install ffmpeg`).

**The first time you open it:**
- **macOS:** an unsigned app is stopped by Gatekeeper. Control-click it in Applications and choose **Open**, or allow it in **System Settings › Privacy & Security**.
- **Windows:** SmartScreen may warn about a new, unsigned installer. Choose **More info › Run anyway**.

## What it does

- **A ready-made shelf.** The 30 shows from the Afterhours poster find themselves in Apple's podcast directory on first launch, or on their own websites when there's no listing.
- **Shows from anywhere.** Add shows from Search or Discover, or paste a feed, an Apple Podcasts link or a show's website.
  - When a feed stops working, AfterhoursNex looks on the show's website instead.
  - **Change source** points a show at a different feed or website.
- **Listening.**
  - It remembers where you were, and **Up Next** collects what you're partway through.
  - Back 15 and forward 30 seconds, and speeds from 0.8× to 2×.
  - A sleep timer, from 15 minutes to the end of the episode.
  - It can play the next episode automatically.
- **Captions and transcripts.** When a show publishes a transcript (WebVTT, SubRip or the podcast-standard JSON), the current line appears in Now Playing. **Transcript** shows all of it, highlights where you are, and jumps to any line you tap. You can also load a caption file yourself.
- **Auto captions on Windows, macOS and Linux.** Episodes without captions are captioned on your computer by OpenAI's Whisper models (through whisper.cpp), starting from where you're listening. The speech model downloads once (142 MB for the default), and captions are saved so they load instantly next time. Nothing is sent anywhere.
- **Pro mode** for auto captions: the model (tiny to large-v3), the spoken language, translating into English, processor threads, beam size, temperature, the silence threshold, the longest caption line, and a vocabulary hint.
- **Plays more formats.** Android plays MP3, AAC, Opus, Ogg, FLAC and WAV. The desktop app plays MP3, AAC and WAV directly, and converts Opus, Ogg, FLAC, WebM, WMA and other formats with its built-in FFmpeg before playing them.
- **OPML import and export,** to bring your shows from another podcast app or take them to one.
- **In English, 简体中文 and 日本語.** Settings › Language follows the system by default. The interface, dates, durations and messages change language. Podcast titles and descriptions stay as their publishers wrote them.
- **Use it on your phone from your computer.** With Settings › Use on your phone turned on, the desktop app gives a private link and a QR code. Opening it on a phone on the same Wi-Fi brings up the phone version of Afterhours, which can be added to the home screen. Feeds, search and websites go through the computer. The phone keeps its own shelf and progress, and auto captions aren’t available there yet.
- **Background playback on Android,** with lock-screen, notification, headphone and car controls.
- **The Afterhours design.** The same look as the Afterhours web app: a sidebar with your shows on wide windows, a tab bar and floating mini player on phones, episode rows with play pills, a floating player bar, and Now Playing over the blurred cover. Light and dark appearance.
- **An opening animation.** The sunset icon rises, the name is written letter by letter, and the x of "Nex" is drawn in two pen strokes before the slogan appears. A tap skips it, and Settings can turn it off.

## Building it

You need a **full JDK 17 or 21** (Temurin, Microsoft Build of OpenJDK or Zulu, not just a Java runtime) and **Gradle 9.6 or later**; it’s built with Gradle 9.8.1. The Gradle from Homebrew, SDKMAN or gradle.org works. For Android you also need the Android SDK. Android Studio and IntelliJ IDEA bring all of this.

| To | Run |
| --- | --- |
| Run the desktop app | `gradle :desktopApp:run` |
| Run the Android app | Open this folder in Android Studio and run **androidApp** |
| Test the shared core | `gradle :shared:desktopTest` |

There's no Gradle wrapper in the project yet. Run `gradle wrapper --gradle-version 9.8.1` once in this folder. After that, `./gradlew` works in place of `gradle` and always uses that version.

## Building release packages

### Desktop installers (.exe, .msi, .dmg, .deb, .rpm)

Compose Multiplatform makes the installers with the JDK's `jpackage` tool. Each installer has to be built **on its own system**: a `.dmg` on a Mac, an `.exe` or `.msi` on Windows, and a `.deb` or `.rpm` on Linux. To build them all from one place, use the GitHub workflow below.

| Package | Build on | Command | You'll find it in |
| --- | --- | --- | --- |
| `.exe` installer | Windows | `gradle :desktopApp:packageExe` | `desktopApp/build/compose/binaries/main/exe/` |
| `.msi` installer | Windows | `gradle :desktopApp:packageMsi` | `desktopApp/build/compose/binaries/main/msi/` |
| `.dmg` disk image | macOS | `gradle :desktopApp:packageDmg` | `desktopApp/build/compose/binaries/main/dmg/` |
| `.deb` package | Debian or Ubuntu | `gradle :desktopApp:packageDeb` | `desktopApp/build/compose/binaries/main/deb/` |
| `.rpm` package | Fedora or openSUSE | `gradle :desktopApp:packageRpm` | `desktopApp/build/compose/binaries/main/rpm/` |
| Every format for this system | any | `gradle :desktopApp:packageDistributionForCurrentOS` | the folders above |
| A portable app folder, no installer | any | `gradle :desktopApp:createDistributable` | `desktopApp/build/compose/binaries/main/app/` |

Some packages need extra tools:
- **Windows:** the WiX toolset that `.exe` and `.msi` packaging needs is downloaded for you on the first build.
- **Debian and Ubuntu:** `.deb` packages need `dpkg-deb` and `fakeroot` (`sudo apt install fakeroot`).
- **Fedora and openSUSE:** `.rpm` packages need `rpm-build`.

A Mac builds a `.dmg` for its own processor. Apple silicon Macs, including GitHub's `macos-latest` machines, make Apple silicon builds; an Intel build has to be made on an Intel Mac.

The app's name, version, icons and installer options are set in `desktopApp/build.gradle.kts`, under `nativeDistributions`.

### All of them at once, on GitHub

The **AfterhoursNex** workflow (`.github/workflows/kmp.yml`) runs on every change to this folder:
1. It tests the shared core.
2. It builds the Android app.
3. It builds the `.exe` and `.msi` on Windows, the `.dmg` on macOS and the `.deb` on Linux.

Each run's files appear under its **Artifacts**. To publish a release with everything attached, push a version tag:

```
git tag nex-v1.1.0
git push origin nex-v1.1.0
```

The release uses this folder's `RELEASE_NOTES.md` as its description and includes `SHA256SUMS.txt`.

### Android (.apk and .aab)

| Package | Command | You'll find it in |
| --- | --- | --- |
| `.apk`, to install directly | `gradle :androidApp:assembleRelease` | `androidApp/build/outputs/apk/release/` |
| `.aab`, for Google Play | `gradle :androidApp:bundleRelease` | `androidApp/build/outputs/bundle/release/` |

Release builds are signed with your key when these environment variables are set:

| Variable | What it is |
| --- | --- |
| `ANDROID_KEYSTORE` | Path to your keystore file |
| `ANDROID_KEYSTORE_PASSWORD` | The keystore's password |
| `ANDROID_KEY_ALIAS` | The key's alias |
| `ANDROID_KEY_PASSWORD` | The key's password |

Without them, release builds use Android's debug key. That's fine for installing on your own devices, but Google Play needs your own key. To make one (keep it safe: every update has to be signed with the same key):

```
keytool -genkeypair -v -keystore afterhoursnex.jks -alias afterhoursnex -keyalg RSA -keysize 4096 -validity 10000
```

For the GitHub workflow, add these repository secrets:
- `ANDROID_KEYSTORE_BASE64`: the keystore, made with `base64 -i afterhoursnex.jks`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

### Signing the desktop apps

Unsigned installers work, but macOS and Windows warn people before opening them.

- **macOS:** you need an Apple Developer account and a *Developer ID Application* certificate in your keychain. Set these environment variables:
  - `MACOS_SIGNING_IDENTITY`, for example `Developer ID Application: Your Name (TEAMID)`
  - `APPLE_ID`
  - `APPLE_APP_PASSWORD`, an app-specific password from appleid.apple.com
  - `APPLE_TEAM_ID`

  Then run `gradle :desktopApp:packageDmg :desktopApp:notarizeDmg`. The app is signed while it's packaged, then sent to Apple to be notarised, so it opens without a warning.
- **Windows:** sign the installers with a code-signing certificate and Microsoft's `signtool`:

  ```
  signtool sign /fd SHA256 /tr http://timestamp.digicert.com /td SHA256 /f certificate.pfx /p PASSWORD AfterhoursNex-1.1.0.exe
  ```

  Do the same for the `.msi`. Even signed, a new certificate shows SmartScreen warnings until it builds up a reputation.

### Publishing on each platform

The tag release on GitHub covers every platform at once. Beyond it, each platform has its own places to publish:

- **macOS.**
  - **Direct download:** sign the `.dmg` with a Developer ID certificate and notarise it (see "Signing the desktop apps" above), so it opens without a warning. This needs a membership of the Apple Developer Program.
  - **Homebrew:** to offer `brew install --cask`, submit a cask to `homebrew/homebrew-cask` that points to the release's `.dmg`, with its SHA-256 from `SHA256SUMS.txt`.
- **Windows.**
  - **Direct download:** sign the `.exe` and `.msi` with a code-signing certificate (`signtool`, above). Signed installers stop showing SmartScreen warnings once they build a reputation, sooner with an EV certificate or Azure Trusted Signing.
  - **winget:** run `wingetcreate new` with the release's `.msi` link, and submit the manifest it makes to `microsoft/winget-pkgs`.
  - **Microsoft Store:** submit the signed `.msi` or `.exe` as a Win32 app in Partner Center.
- **Linux.**
  - **The release files:** publish the `.deb` (Debian, Ubuntu), `.rpm` (Fedora, openSUSE) and the portable `.tar.gz`.
  - **Flathub:** listing the app there needs a Flatpak manifest, which isn't part of this project yet.
- **Android.**
  - **Direct download:** publish the release `.apk`, signed with your own key (see "Android" above). Readers allow installs from their browser or file manager.
  - **Google Play:**
    1. Create the app in Play Console and turn on Play App Signing.
    2. Upload the `.aab` from `bundleRelease` to a testing track.
    3. Fill in the store listing, data safety and content rating.
    4. Promote the release to production. Check the Play Console for current testing requirements, because new personal accounts may need a closed test first.
  - **Updates:** each update needs a higher `versionCode`.

### Changing the version

Update these three places together:
- `AppInfo.VERSION` in `shared/src/commonMain/kotlin/app/afterhours/core/Model.kt`
- `versionName`, and a higher `versionCode`, in `androidApp/build.gradle.kts`
- `packageVersion` in `desktopApp/build.gradle.kts`

macOS requires the version to be three numbers with the first above 0, such as `1.0.1`.

### Releasing a new version

1. Change the version in the three places above, and write what's new at the top of `RELEASE_NOTES.md`.
2. Test it on your own computer:
   - `./gradlew :shared:desktopTest` runs the core's tests.
   - `./gradlew :desktopApp:run` starts the app: play an episode, open Now Playing, try captions, speeds and the sleep timer, and switch languages and light or dark.
   - `./gradlew :desktopApp:packageDistributionForCurrentOS` makes your system's installer: install it and open the installed app once.
   - With an Android phone connected, `./gradlew :androidApp:installDebug` puts the app on it.
3. Commit and push. The workflow builds every package; check that the run is green, and try the files under its **Artifacts**.
4. Tag the release, and push the tag:
   ```
   git tag nex-v1.1.0
   git push origin nex-v1.1.0
   ```
   When every build passes, the workflow publishes the GitHub release with all the files and `SHA256SUMS.txt`.
5. If a build fails after tagging, fix it, then move the tag to the fix and push it again:
   ```
   git tag -d nex-v1.1.0 && git push --delete origin nex-v1.1.0
   git tag nex-v1.1.0 && git push origin nex-v1.1.0
   ```

## How it's put together

| Path | What it is |
| --- | --- |
| `shared/src/commonMain/kotlin/app/afterhours/core` | The core, with no platform code: feeds, Apple's directory, the website fallback, the library and playback |
| `shared/src/commonMain/kotlin/app/afterhours/ui` | The interface, in Compose: every screen, Now Playing and the opening animation |
| `shared/src/commonTest` | Tests for the core |
| `androidApp` | Android: the shared interface, and Media3 for background playback and system controls |
| `desktopApp` | Windows, macOS and Linux: the shared interface in a native window, with JavaFX playing the audio, plus the installer settings and icons |
| `shared/src/commonMain/composeResources` | The app icon and the Liberation Serif font used by the opening animation |
| `assets` | The animated banner (`afterhoursnex-intro.svg`), and the same as a GIF and a still PNG |
| `licenses` | The font's license |

The project's versions are pinned in `gradle/libs.versions.toml`:

| Component | Version |
| --- | --- |
| Gradle | 9.8.1 |
| Kotlin | 2.4.21 |
| Compose Multiplatform | 1.12.2, with Material 3 1.12.0-alpha03 |
| Android Gradle Plugin | 9.4.0 (compile SDK 37, target SDK 36) |
| Ktor | 3.6.0 |
| kotlinx.coroutines and kotlinx.serialization | 1.11.0 |
| Coil | 3.6.1 |
| Media3 | 1.11.1 |
| JavaFX | 21.0.12 |
| whisper-jni | 1.7.1 |
| JAVE2 (FFmpeg) | 3.6.0 |
| ZXing | 3.5.4 |

## The look

- **Lettering:** Liberation Serif, a Times New Roman-compatible serif. "Afterhours" is set upright and "Nex" in italic.
- **Colours:** the app icon's sunset (gold `#FEBB5F`, coral `#F37377`, rose `#CC5286`, magenta `#B6448D`) on its indigo night sky.
- **The banner:** `assets/afterhoursnex-intro.svg` animates by itself wherever it's shown, including at the top of this page. `afterhoursnex-intro.gif` is the same animation for places that don't play SVG, and `afterhoursnex-banner.png` is its last frame, sized for GitHub's social preview.
- **The font's license:** Liberation Serif 2.1.5 is licensed under the SIL Open Font License 1.1 (see `licenses/LiberationSerif-OFL.txt`). The app bundles it unmodified, and the banner uses its letter shapes.

## Third-party software

AfterhoursNex is built on open-source software, including Kotlin, Compose Multiplatform, Ktor, Coil, Media3, JavaFX, whisper.cpp and FFmpeg. The desktop installers include FFmpeg as a separate program. See `licenses/THIRD_PARTY.md` for the components, their licenses, and what FFmpeg's license asks of anyone distributing the installers.

## Your data

- **Your library, progress and settings stay on your device:**
  - on Android, in the app's private storage
  - on Windows, in `%APPDATA%\AfterhoursNex`
  - on macOS, in `~/Library/Application Support/AfterhoursNex`
  - on Linux, in `~/.local/share/afterhoursnex`
- **No account, no tracking.** AfterhoursNex only connects to the podcasts you follow, their websites, Apple's public podcast directory when it looks for shows, and Hugging Face once, to download a speech model for auto captions.
- **Auto captions stay on your computer.** The audio is recognised locally; the speech models are kept in the data folder above, under `models`.
- **Phone access is off until you turn it on.** It only listens on your computer’s address on the local network, and only answers phones that opened the private link. **New link** signs every phone out. The link’s key is kept in the data folder, in `phone.properties`.
  - Your firewall may ask whether to allow AfterhoursNex. Allow it on private networks only.

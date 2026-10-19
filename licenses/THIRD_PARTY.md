# Third-party software in AfterhoursNex

| Component | Used for | License |
| --- | --- | --- |
| Kotlin, kotlinx.coroutines, kotlinx.serialization | The language and its libraries | Apache 2.0 |
| Compose Multiplatform | The interface | Apache 2.0 |
| Ktor | Fetching feeds and websites | Apache 2.0 |
| Coil | Cover art | Apache 2.0 |
| AndroidX Media3 (ExoPlayer) | Playback on Android | Apache 2.0 |
| OpenJFX (JavaFX) | Playback on desktop | GPL 2.0 with the Classpath Exception |
| whisper.cpp | Speech recognition on desktop | MIT |
| ZXing core | The QR code for phone access | Apache 2.0 |
| whisper-jni (github.com/GiviMAD/whisper-jni) | Connecting whisper.cpp to the app | See its repository |
| OpenAI Whisper models (ggml format, downloaded on first use) | Speech recognition | MIT |
| FFmpeg (from JAVE2's platform packages, github.com/a-schild/jave2) | Converting audio formats and reading audio for recognition | LGPL 2.1 or later; builds with GPL parts are GPL 2 or later |
| Liberation Serif 2.1.5 | The lettering and the opening animation | SIL Open Font License 1.1 (see `LiberationSerif-OFL.txt`) |

## FFmpeg

The desktop installers include the FFmpeg program and run it as a separate process; AfterhoursNex doesn't link to it. FFmpeg's license still applies to the copy you distribute. Before you publish installers, check how the FFmpeg build in JAVE2's packages is configured, and be ready to provide FFmpeg's source code for that build (from ffmpeg.org, or JAVE2's build scripts), as its license requires.

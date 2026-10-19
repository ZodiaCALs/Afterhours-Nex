import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

// JavaFX plays the audio; its native parts differ per system, so pick the ones for the computer building the app.
val fxPlatform: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch").let { it == "aarch64" || it == "arm64" }
    when {
        os.contains("win") -> "win"
        os.contains("mac") -> if (arm) "mac-aarch64" else "mac"
        else -> if (arm) "linux-aarch64" else "linux"
    }
}

// FFmpeg comes from JAVE's platform package (only the program, run separately; see licenses/THIRD_PARTY.md).
val ffmpegPlatform: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch").let { it == "aarch64" || it == "arm64" }
    when {
        os.contains("win") -> "win64"
        os.contains("mac") -> if (arm) "osxm1" else "osx64"
        else -> if (arm) "linux-arm64" else "linux64"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.zxing.core)
    implementation("io.github.givimad:whisper-jni:${libs.versions.whisperJni.get()}")
    implementation("ws.schild:jave-nativebin-$ffmpegPlatform:${libs.versions.jave.get()}")
    implementation(compose.desktop.currentOs)
    implementation(libs.coroutines.swing)
    for (module in listOf("base", "graphics", "media", "swing")) {
        implementation("org.openjfx:javafx-$module:${libs.versions.javafx.get()}:$fxPlatform")
    }
}

compose.desktop {
    application {
        mainClass = "app.afterhours.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "AfterhoursNex"
            packageVersion = "1.0.0"
            description = "A small, private podcast player"
            vendor = "AfterhoursNex"
            copyright = "© 2026 AfterhoursNex"
            includeAllModules = true   // JavaFX and the network stack need modules jlink can't see, so ship them all
            macOS {
                bundleID = "app.afterhoursnex"
                dockName = "AfterhoursNex"
                iconFile.set(project.file("icons/AfterhoursNex.icns"))
                // Signing and notarising, when the identity and Apple account are in the environment (see the README).
                System.getenv("MACOS_SIGNING_IDENTITY")?.let { id ->
                    signing {
                        sign.set(true)
                        identity.set(id)
                    }
                    notarization {
                        appleID.set(System.getenv("APPLE_ID"))
                        password.set(System.getenv("APPLE_APP_PASSWORD"))
                        teamID.set(System.getenv("APPLE_TEAM_ID"))
                    }
                }
            }
            windows {
                iconFile.set(project.file("icons/AfterhoursNex.ico"))
                menuGroup = "AfterhoursNex"
                upgradeUuid = "25aa657f-6912-5b77-a79d-e31d148d4887"
                perUserInstall = true
                shortcut = true
                dirChooser = true
            }
            linux {
                iconFile.set(project.file("icons/AfterhoursNex.png"))
                packageName = "afterhoursnex"
                shortcut = true
                appCategory = "AudioVideo"
                menuGroup = "AudioVideo;Audio;Player"
            }
        }
    }
}

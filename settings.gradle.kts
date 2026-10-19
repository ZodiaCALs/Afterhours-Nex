pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// The Android Gradle Plugin this project uses (9.4) needs Gradle 9.6 or later.
run {
    val current = org.gradle.util.GradleVersion.current()
    if (current < org.gradle.util.GradleVersion.version("9.6") || current >= org.gradle.util.GradleVersion.version("10.0")) {
        throw GradleException(
            "AfterhoursNex builds with Gradle 9.6 or later (9.8.1 is what it's built with), but this is Gradle ${current.version}. " +
                "Use ./gradlew, or install a current Gradle from https://gradle.org/releases."
        )
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AfterhoursNex"
include(":shared", ":androidApp", ":desktopApp")

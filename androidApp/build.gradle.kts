import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)   // compiles Kotlin itself (built-in Kotlin, Android Gradle Plugin 9)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "app.afterhours.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.afterhoursnex"
        minSdk = 26
        targetSdk = 36   // the level Google Play requires; Android 17 (API 37) behaviour hasn't been tested yet
        versionCode = 3
        versionName = "1.2.0"
    }

    // A release key from the environment (see the README). Without one, release builds use the debug key,
    // which is fine for installing them yourself but not for a store.
    val keystore = System.getenv("ANDROID_KEYSTORE")
    signingConfigs {
        create("release") {
            if (keystore != null) {
                storeFile = file(keystore)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (keystore != null) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.activity.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.coroutines.android)
}

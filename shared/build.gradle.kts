import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidKmpLibrary)
}

kotlin {
    // The Android target, built by Android Gradle Plugin 9's library plugin for Kotlin Multiplatform.
    android {
        namespace = "app.afterhours.shared"
        compileSdk = 37
        minSdk = 26
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        androidResources { enable = true }   // the opening animation's font and icon are Compose resources
    }
    // The desktop target: the core's tests run here now, and the Windows, macOS and Linux apps come next.
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.ktor.core)
            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
            implementation(libs.coil.compose)
            implementation(libs.coil.ktor)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.okhttp)
            implementation(libs.coroutines.android)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.okhttp)
                implementation(libs.coroutines.swing)
            }
        }
    }
}

// The font and icon in composeResources become the Res class used by the opening animation.
compose.resources {
    packageOfResClass = "app.afterhours.resources"
    publicResClass = false
}

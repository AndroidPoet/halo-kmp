@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import io.github.androidpoet.liveactivities.Configuration

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.kover)
}

kotlin {
    explicitApi()
    jvmToolchain(17)

    androidTarget { publishLibraryVariants("release") }
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":live-activities"))
            implementation(compose.runtime)
        }
        androidMain.dependencies {
            // LocalContext, to build the Android manager.
            implementation(compose.ui)
        }
    }
}

android {
    namespace = "io.github.androidpoet.liveactivities.compose"
    compileSdk = Configuration.COMPILE_SDK
    defaultConfig { minSdk = Configuration.MIN_SDK }
}

mavenPublishing {
    coordinates(Configuration.GROUP, "live-activities-compose", Configuration.VERSION)
}

@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import io.github.androidpoet.halo.Configuration

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.kover)
}

kotlin {
    explicitApi()
    jvmToolchain(17)

    androidTarget { publishLibraryVariants("release") }
    jvm()
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.compilations.getByName("main").cinterops.create("haloBridge") {
            defFile(project.file("src/nativeInterop/cinterop/haloBridge.def"))
        }
    }
    macosX64()
    macosArm64()
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "io.github.androidpoet.halo"
    compileSdk = Configuration.COMPILE_SDK
    defaultConfig { minSdk = Configuration.MIN_SDK }
    testOptions.unitTests.isReturnDefaultValues = true
}

mavenPublishing {
    coordinates(Configuration.GROUP, "halo", Configuration.VERSION)
}

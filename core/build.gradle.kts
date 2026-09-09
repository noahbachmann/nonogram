@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
}

// The puzzle model and the Solver, with no Compose and no Firebase: `:solverWorker` links this and
// nothing else, so its bundle stays small enough to boot in a web worker. See docs/web-architecture.md.
kotlin {
    js {
        browser()
    }

    wasmJs {
        browser()
    }

    android {
        namespace = "com.trainpaths.nonogram.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

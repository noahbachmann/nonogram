@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

// A second web worker beside sqlite.worker.js, this one running the Solver. It depends on `:core`
// and nothing else so the bundle it produces is small enough to be worth spawning; see
// docs/web-architecture.md. `:webApp` copies its distribution next to its own.
kotlin {
    js {
        browser()
        binaries.executable()
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(libs.kotlinx.browser)
        }
    }
}

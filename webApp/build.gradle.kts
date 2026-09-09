@file:OptIn(ExperimentalWasmDsl::class)

import org.gradle.api.tasks.AbstractCopyTask
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val nonogramEnv = providers.gradleProperty("nonogram.env").getOrElse("dev")

kotlin {
    js {
        browser()
        binaries.executable()
        // Firebase externals are @JsModule-only, which UMD can't link
        useEsModules()
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)

            implementation(libs.compose.ui)
        }
        webMain {
            kotlin.srcDir("src/$nonogramEnv/kotlin")
            dependencies {
                implementation(libs.koin.core)
                implementation(libs.koin.compose.viewmodel)
                implementation(npm("@sqlite.org/sqlite-wasm", "3.50.4-build1"))
                implementation(devNpm("copy-webpack-plugin", "13.0.1"))
            }
        }
    }
}

// The Solver worker ships as a second bundle beside this one. Folding its distribution into this
// module's resources is what puts it where both the dev server and `browserDistribution` serve
// from, the same place `sqlite.worker.js` lands from `src/webMain/resources`. Referenced by output
// directory plus a task path rather than by task object, so configuring `:webApp` never has to wait
// on `:solverWorker` being configured first.
val solverWorker = project(":solverWorker")
listOf("js", "wasmJs").forEach { target ->
    tasks.named<AbstractCopyTask>("${target}ProcessResources") {
        dependsOn("${solverWorker.path}:${target}BrowserDistribution")
        from(solverWorker.layout.buildDirectory.dir("dist/$target/productionExecutable"))
    }
}

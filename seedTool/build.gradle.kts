plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxSerialization)
    application
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}

application {
    mainClass.set("com.trainpaths.nonogram.seed.MainKt")
}

// Never hooked into `check`/`build`: the export talks to Firestore, and CI has no network.
tasks.named<JavaExec>("run") {
    workingDir = rootDir
}

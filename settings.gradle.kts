pluginManagement {
    repositories {
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.5.+"
}

stonecutter {
    // The project uses Groovy build scripts.
    kotlinController = false
    centralScript = "build.gradle"

    create(rootProject) {
        versions("1.19.4", "1.20", "1.20.1")
        vcsVersion = "1.19.4"
    }
}

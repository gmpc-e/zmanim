pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Pin plugin versions here in ONE place
    plugins {
        // Stable AGP that works with Gradle 8.13
        id("com.android.application") version "8.7.2"

        // Kotlin 2.0.x + compose plugin (required from Kotlin 2.0)
        id("org.jetbrains.kotlin.android") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Zmanim"
include(":app")

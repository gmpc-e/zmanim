plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.elad.zmanim"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.elad.zmanim"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // If you later drop minSdk < 26 and need java.time on old devices:
        // isCoreLibraryDesugaringEnabled = true
    }

    // If you prefer not to use the toolchain block below, you can do:
    // kotlinOptions { jvmTarget = "17" }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Material components (gives you Theme.Material3.DayNight.NoActionBar)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // Icons and coroutines
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Networking (simple, no Retrofit)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Compose Material 3
    implementation("androidx.compose.material3:material3:1.2.1")

    // Core + Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Glance for app widget (stable).  DO NOT use glance-material3 here.
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance:1.0.0")

    // Zmanim
    implementation("com.kosherjava:zmanim:2.5.0")

    // Compose tooling
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")

    // If you enable desugaring above:
    // coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}

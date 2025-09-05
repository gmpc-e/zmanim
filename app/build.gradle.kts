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
        versionCode = 2
        versionName = "1.1"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // If you need java.time on very old APIs, enable desugaring and add the dependency below.
        // isCoreLibraryDesugaringEnabled = true
    }
}

kotlin {
    // Ensures both Java & Kotlin compile with JDK 17
    jvmToolchain(17)
}

dependencies {
    // --- Compose BOM (aligns compose artifacts; works well with Kotlin 2.0.x) ---
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    // Compose UI & tooling
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Material3 (Compose)
    implementation("androidx.compose.material3:material3")

    // Icons
    implementation("androidx.compose.material:material-icons-extended")

    // Activity + Lifecycle
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.core:core-ktx:1.13.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Networking (optional)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // --- Glance (Home Screen App Widget) ---
    // Keep to 1.0.0 for maximum stability; provides androidx.glance.unit.dp
    implementation("androidx.glance:glance:1.0.0")
    implementation("androidx.glance:glance-appwidget:1.0.0")
    // (Do NOT add glance-material3 here.)

    // lottie
    implementation("com.airbnb.android:lottie-compose:6.4.0")      // keep if you already use it

    // Zmanim
    implementation("com.kosherjava:zmanim:2.5.0")


    // If you enabled desugaring above:
    // coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}

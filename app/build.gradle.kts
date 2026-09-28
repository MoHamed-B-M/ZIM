@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.zimapp.zim"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.zimapp.zim"
        minSdk = 26 // Dynamic color needs 31+ at runtime; 26 keeps install base wide
        targetSdk = 36
        // 1.0.0 baseline; CI stamps workflow versions via APP_VERSION_* for the
        // updater's versionCode comparison to work across beta builds.
        versionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("APP_VERSION_NAME") ?: "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }
    signingConfigs {
        // CI (build.yaml) exports KEYSTORE_PATH/… — release.keystore for real
        // secrets, ephemeral preview keystore otherwise. Local builds without
        // those env vars fall back to the default debug signing.
        create("release") {
            val ksPath = System.getenv("KEYSTORE_PATH")
            if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            // Only use the release keystore when CI actually provided one.
            System.getenv("KEYSTORE_PATH")
                ?.takeIf { it.isNotBlank() && file(it).exists() }
                ?.let { signingConfig = signingConfigs.getByName("release") }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.material3.adaptive.navigation.suite)
    implementation(libs.material3.window.size)
    implementation(libs.material.icons.core)
    implementation(libs.material.icons.extended)
    implementation(libs.navigation.compose)

    // Persistence — Room via KSP 2.3.x (verified on Maven Central for Kotlin 2.3.x)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DI — Koin, no annotation processing of its own
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Background sync (opt-in only)
    implementation(libs.workmanager.ktx)

    // Serialization for type-safe nav + export/import + REST sync
    implementation(libs.kotlinx.serialization.json)

    // Network for WebDAV / REST providers (lazy — unused when sync disabled)
    implementation(libs.okhttp)
    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}

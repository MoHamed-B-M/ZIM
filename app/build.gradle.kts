plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.zimapp.zim"
    compileSdk = 36
    flavorDimensions += "store"

    productFlavors {
        create("fdroid") {
            dimension = "store"
            applicationId = "com.zimapp.zim"
            versionNameSuffix = "-fdroid"
            isDefault = true
        }

        create("playstore") {
            dimension = "store"
            applicationId = "com.zimapp.zim"
            versionNameSuffix = "-playstore"
        }
    }

    defaultConfig {
        applicationId = "com.zimapp.zim"
        minSdk = 26
        targetSdk = 36
        // CI stamps workflow versions (updater compares versionCode); local fallback keeps theirs.
        versionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 14
        versionName = System.getenv("APP_VERSION_NAME") ?: "1.7"
        vectorDrawables {
            useSupportLibrary = true
        }

        // https://developer.android.com/guide/topics/resources/app-languages#gradle-config
        resourceConfigurations.plus(
            listOf("en", "ar", "de", "es", "fa", "fil", "fr", "hi", "it", "ja", "ru", "sk", "tr", "da", "nl", "pl", "tr", "uk", "vi", "ota", "pt-rBR", "sr", "zh-rCN")
        )
    }

    signingConfigs {
        // CI (build.yaml) exports KEYSTORE_PATH/… (stable secret keystore, or
        // ephemeral preview keystore). Paths are repo-root relative.
        create("release") {
            val ksPath = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
            val ksFile = ksPath?.let { rootProject.file(it) }
            if (ksFile != null) {
                check(ksFile.exists()) { "KEYSTORE_PATH points to missing file: $ksPath" }
                storeFile = ksFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                val debugKs = File(System.getProperty("user.home"), ".android/debug.keystore")
                if (debugKs.exists()) {
                    storeFile = debugKs
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildToolsVersion = "36.0.0"
}

dependencies {
    implementation(libs.m3color)
    implementation(libs.androidx.biometric.ktx)
    implementation(libs.androidx.glance)
    implementation(libs.coil.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.glance.appwidget)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.compile)
    implementation(libs.hilt.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)
    "playstoreImplementation"(libs.billing)
}

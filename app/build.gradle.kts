plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.location_reminder"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.location_reminder"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = project.findProperty("RELEASE_STORE_FILE") as? String
                ?: System.getenv("RELEASE_STORE_FILE")
            if (keystorePath != null && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = (project.findProperty("RELEASE_STORE_PASSWORD") as? String)
                    ?: System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = (project.findProperty("RELEASE_KEY_ALIAS") as? String)
                    ?: System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = (project.findProperty("RELEASE_KEY_PASSWORD") as? String)
                    ?: System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_compiler_config.conf"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Jetpack Compose & Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)

    // Lifecycle & ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Google Play Services Location & Geofencing
    implementation(libs.google.play.services.location)

    // MapLibre Native Android SDK (OSM / Protomaps / OpenFreeMap / Esri Satellite)
    implementation("org.maplibre.gl:android-sdk:11.8.0")

    // DataStore Preferences for user settings
    implementation(libs.androidx.datastore.preferences)

    // Android SplashScreen API
    implementation(libs.androidx.core.splashscreen)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
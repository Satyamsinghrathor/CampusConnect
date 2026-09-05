plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    //    serializable
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.campusconnect"
    compileSdk {
        version = release(37) {
        }
    }

    defaultConfig {
        applicationId = "com.example.campusconnect"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
//    icons
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // For Navigation
    implementation("androidx.navigation:navigation-compose:2.10.0")
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}
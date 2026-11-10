plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.project_lttbd"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.project_lttbd"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // CameraX
    val cameraxVersion = "1.4.2"

    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Firebase: quản lý phiên bản chung
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))

    // Đăng ký, đăng nhập và quản lý phiên
    implementation("com.google.firebase:firebase-auth")

    // Credential Manager và đăng nhập Google
    implementation("androidx.credentials:credentials:1.3.0")
    implementation(
        "androidx.credentials:credentials-play-services-auth:1.3.0"
    )
    implementation(
        "com.google.android.libraries.identity.googleid:googleid:1.1.1"
    )

    // Cloud Firestore
    implementation("com.google.firebase:firebase-firestore")

    // Thông báo đẩy và Analytics
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")

    // Retrofit: gọi API FastAPI
    implementation("com.squareup.retrofit2:retrofit:3.0.0")

    // Gson: chuyển đổi JSON giữa backend và các lớp dữ liệu Kotlin
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
}
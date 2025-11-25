import com.android.build.gradle.internal.dependency.CONFIG_NAME_ANDROID_JDK_IMAGE

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.example.easymedia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.easymedia"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    configurations.all {
        exclude(mapOf("group" to "com.google.protobuf", "module" to "protobuf-kotlin"))
        exclude(mapOf("group" to "com.google.protobuf", "module" to "protobuf-kotlin-lite"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}


dependencies {
    // CometChat UIKit
    implementation("com.cometchat:chat-uikit-android:5.2.2")
    // Optional: voice/video calling
    implementation("com.cometchat:calls-sdk-android:4.3.1")
    implementation("com.tomtom.sdk.maps:map-display:1.26.3") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }

    implementation("com.tomtom.sdk.search:search-online:1.26.3") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }

    implementation("com.tomtom.sdk.location:provider-default:1.26.3") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }

    implementation("com.tomtom.sdk.location:provider-map-matched:1.26.3") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }

    implementation("com.tomtom.sdk.location:provider-simulation:1.26.3") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }

    implementation("com.tomtom.sdk.location:provider-api:1.26.3") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
    implementation("com.google.protobuf:protobuf-javalite:3.25.5")
    implementation("com.google.android.libraries.places:places:3.4.0")
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.libraries.places:places:3.3.0")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.palette:palette:1.0.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("io.github.parksanggwon:tedimagepicker:1.7.3")
    implementation("com.antonkarpenko:ffmpeg-kit-full-gpl:2.1.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.tbuonomo:dotsindicator:5.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    // Firebase BoM

    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    // Firebase SDK cần thiết
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.play.services.maps)

    // AndroidX & UI
    val fragment_version = "1.8.3"
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.fragment:fragment-ktx:$fragment_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Version Catalog (giữ nguyên nếu dự án đang dùng)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.swiperefreshlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}

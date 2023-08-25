import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.woohyman.xml"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.woohyman.emulator"
        minSdk = 24
        targetSdk = 33
        versionCode = 4
        versionName = "0.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    viewBinding {
        isEnabled = true
    }

    signingConfigs {
        create("demokey") {
            keyAlias = "demokey"
            keyPassword = "demokey"
            storeFile = file("demokey.jks")
            storePassword = "demokey"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("demokey")
        }
        release {
            signingConfig = signingConfigs.getByName("demokey")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.1.0")
    implementation("androidx.activity:activity-ktx:1.2.0")
    implementation("androidx.fragment:fragment-ktx:1.3.0")
    implementation("androidx.databinding:databinding-runtime:8.1.1")
    implementation("org.jetbrains.kotlinx:atomicfu:0.17.2")
    val lifecycle_version = "2.5.1"

    implementation(project(":domain:keyboard"))
    implementation(project(":data:nes"))
    api("com.google.dagger:hilt-android:2.44")
    kapt("com.google.dagger:hilt-android-compiler:2.44")

    // Lifecycles only (without ViewModel or LiveData)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
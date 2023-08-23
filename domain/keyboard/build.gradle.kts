plugins {
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("com.android.library")
    id("org.jetbrains.kotlin.android")

}

android {
    namespace = "com.woohyman.mylibrary"
    compileSdk = 33

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    api("com.google.dagger:hilt-android:2.44")
    kapt("com.google.dagger:hilt-android-compiler:2.44")

    api("androidx.appcompat:appcompat:1.6.1")
    api("com.google.android.material:material:1.8.0")
    testApi("junit:junit:4.13.2")
    androidTestApi("androidx.test.ext:junit:1.1.5")
    androidTestApi("androidx.test.espresso:espresso-core:3.5.1")

    api("androidx.appcompat:appcompat:1.6.1")
    api("com.google.android.material:material:1.8.0")
    api("com.blankj:utilcodex:1.31.1")
    api("com.liulishuo.filedownloader:library:1.7.7")
    api("me.laoyuyu.aria:core:3.8.16")
    api("androidx.core:core-ktx:1.10.1")
    annotationProcessor("me.laoyuyu.aria:compiler:3.8.16")
    api("com.squareup.okhttp3:okhttp:4.11.0")

    implementation("com.liulishuo.filedownloader:library:1.7.7")
}
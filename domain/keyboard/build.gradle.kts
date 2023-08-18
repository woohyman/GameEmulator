plugins {
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

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    api("androidx.appcompat:appcompat:1.6.1")
    api("com.google.android.material:material:1.8.0")
    api("com.blankj:utilcodex:1.31.1")
    api("com.liulishuo.filedownloader:library:1.7.7")
    api("me.laoyuyu.aria:core:3.8.16")
    implementation("androidx.core:core-ktx:1.10.1")
    annotationProcessor("me.laoyuyu.aria:compiler:3.8.16")
    api("com.liulishuo.okdownload:okdownload:1.0.5") //核心库
    api("com.liulishuo.okdownload:sqlite:1.0.5") //存储断点信息的数据库
    api("com.liulishuo.okdownload:okhttp:1.0.5") //提供okhttp连接，如果使用的话，需要引入okhttp网络请求库
    api("com.squareup.okhttp3:okhttp:4.11.0")
}
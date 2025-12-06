plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.test_filesync"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.test_filesync"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 极光推送配置
        manifestPlaceholders["JPUSH_PKGNAME"] = "com.example.test_filesync"
        manifestPlaceholders["JPUSH_APPKEY"] = "d1c65931bf8444682740fa07"
        manifestPlaceholders["JPUSH_CHANNEL"] = "developer-default"
        manifestPlaceholders["HONOR_APPID"] = "104534030"
        manifestPlaceholders["HONOR_APPKEY"] = "577e629d0a4eac5218f8754a3faf9abc6a301b58b9af9fd094156b6946327a98"
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
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.baidu.lbsyun:BaiduMapSDK_Location_All:9.6.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.work:work-runtime:2.8.0")


    // 极光推送SDK
    implementation("cn.jiguang.sdk:jpush:5.8.0")

    // 荣耀推送SDK (本地文件)
    // implementation(files("libs/jpush-android-plugin-honor-v5.9.0.jar"))
    implementation("cn.jiguang.sdk.plugin:honor:5.6.0")
    implementation(group = "", name = "HiPushSDK-8.0.12.307", ext = "aar")
    // implementation(files("libs/hipush.jar"))
}

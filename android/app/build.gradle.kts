plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.benjakusa.malele"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.benjakusa.malele"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
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
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // Core
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.androidxLifecycleViewModelCompose)
    implementation(libs.androidxLifecycleRuntimeCompose)
    implementation(libs.androidxActivityCompose)
    implementation(libs.androidxNavigationCompose)

    // Compose
    implementation(platform(libs.composeBom))
    implementation(libs.composeUi)
    implementation(libs.composeUiGraphics)
    implementation(libs.composeUiToolingPreview)
    implementation(libs.composeMaterial3)
    implementation(libs.composeMaterialIcons)
    debugImplementation(libs.composeUiTooling)

    // Hilt
    implementation(libs.hiltAndroid)
    implementation(libs.hiltNavigationCompose)
    ksp(libs.hiltCompiler)

    // Room
    implementation(libs.roomRuntime)
    implementation(libs.roomKtx)
    ksp(libs.roomCompiler)

    // Coroutines + Serialization
    implementation(libs.kotlinxCoroutinesAndroid)
    implementation(libs.kotlinxSerializationJson)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofitKotlinxSerialization)
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)

    // Media3
    implementation(libs.media3ExoPlayer)
    implementation(libs.media3Ui)
    implementation(libs.media3Session)

    // Storage
    implementation(libs.coilCompose)
    implementation(libs.securityCrypto)
    implementation(libs.datastorePreferences)

    // WorkManager
    implementation(libs.workRuntime)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidxTestJunit)
    androidTestImplementation(libs.androidxTestRunner)
    androidTestImplementation(libs.espressoCore)
    androidTestImplementation(platform(libs.composeBom))
}

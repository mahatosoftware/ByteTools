plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.ksp)
}

android {
    namespace = "in.mahato.bytetools"
    compileSdk = 36

    // Redirect build files to local drive if needed (can be uncommented for CLI builds)
    // layout.buildDirectory.set(file("/Users/debasish/AndroidBuilds/ByteTools/app"))

    defaultConfig {
        applicationId = "in.mahato.bytetools"
        minSdk = 24
        targetSdk = 36
        versionCode = 34
        versionName = "1.0.34"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    androidResources {
        ignoreAssetsPattern = "!.DS_Store:!._*"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            excludes += "**/libimage_processing_util_jni.so"
            excludes += "**/libdatastore_shared_counter.so"
            pickFirsts.add("**/libjpeg.so")
        }
        dex {
            useLegacyPackaging = false
        }
        resources {
            excludes += "**/._*"
            excludes += "**/.DS_Store"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713" // Test ID
            buildConfigField("String", "AD_PDF_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "AD_IMAGE_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "AD_GPS_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "AD_QR_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["admobAppId"] = "ca-app-pub-8382655413286804~1272846618" // Production AdMob App ID
            buildConfigField("String", "AD_PDF_INTERSTITIAL_ID", "\"ca-app-pub-8382655413286804/6027313993\"")
            buildConfigField("String", "AD_IMAGE_INTERSTITIAL_ID", "\"ca-app-pub-8382655413286804/8278339712\"")
            buildConfigField("String", "AD_GPS_INTERSTITIAL_ID", "\"ca-app-pub-8382655413286804/5694606658\"")
            buildConfigField("String", "AD_QR_INTERSTITIAL_ID", "\"ca-app-pub-8382655413286804/3401150656\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.com.google.android.material.material)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.guava)
    
    // ML Kit
    implementation(libs.play.services.mlkit.barcode.scanning)
    
    // QR Generation
    implementation("com.google.zxing:core:3.5.3")
    
    // Ads
    implementation(libs.play.services.ads)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.mlkit.document.scanner)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    
    // Permissions
    implementation(libs.accompanist.permissions)
    // Image Processing
    implementation(libs.androidx.exifinterface)
    implementation(libs.coil.compose)

    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.pdfbox.android)
    implementation(libs.tesseract.android)
    implementation(libs.android.pdf.viewer)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}


import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.smish.wheresmymoney"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.smish.wheresmymoney"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val props = Properties()
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                props.load(localPropsFile.inputStream())
            }
            val storeFilePath = props.getProperty("RELEASE_STORE_FILE")
            if (!storeFilePath.isNullOrEmpty()) {
                storeFile = rootProject.file(storeFilePath)
            }
            storePassword = props.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = props.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = props.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
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
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.haze)
    implementation(libs.hazeMaterials)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)

    // Credentials & Google Sign-In
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Splash screen
    implementation(libs.androidx.core.splashscreen)

    // Coil Image Loading
    implementation(libs.coil.compose)

    // Jetpack Glance App Widget
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
}
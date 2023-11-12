import java.util.Properties
import java.io.FileInputStream

val secretsProperties = Properties()

secretsProperties.load(FileInputStream(rootProject.file("secrets.properties")))

plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "us.huseli.thoucylinder"
    compileSdk = 34

    defaultConfig {
        val youtubeApiKey = secretsProperties["youtubeApiKey"] as String
        val discogsApiKey = secretsProperties["discogsApiKey"] as String
        val discogsApiSecret = secretsProperties["discogsApiSecret"] as String

        applicationId = "us.huseli.thoucylinder"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        setProperty("archivesBaseName", "thoucylinder_$versionName")
        buildConfigField("String", "youtubeApiKey", "\"$youtubeApiKey\"")
        buildConfigField("String", "discogsApiKey", "\"$discogsApiKey\"")
        buildConfigField("String", "discogsApiSecret", "\"$discogsApiSecret\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
            isRenderscriptDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packaging {
        resources {
            // excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val lifecycleVersion = "2.6.2"
val roomVersion = "2.6.0"
val daggerVersion = "2.48.1"
val composeVersion = "1.5.4"
val media3Version = "1.1.1"
val pagingVersion = "3.3.0-alpha02"

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Compose:
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeVersion")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Material:
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material:material-icons-extended:1.6.0-alpha08")
    // implementation("androidx.compose.material:material-icons-extended:$composeVersion")

    // Lifecycle:
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")

    // Hilt:
    implementation("com.google.dagger:hilt-android:$daggerVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    kapt("com.google.dagger:hilt-compiler:$daggerVersion")

    // Media:
    implementation("androidx.media3:media3-common:$media3Version")
    implementation("androidx.media3:media3-exoplayer:$media3Version")

    // Room:
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // Gson:
    implementation("com.google.code.gson:gson:2.10.1")

    // Theme etc:
    implementation("com.github.Eboreg:RetainTheme:2.4.0")

    // FFMPEG:
    implementation(files("ffmpeg-kit.aar"))
    implementation("com.arthenica:smart-exception-java:0.2.1")

    // Splashscreen:
    implementation("androidx.core:core-splashscreen:1.0.1")

    // For reading document tree:
    // implementation("androidx.documentfile:documentfile:1.0.1")

    // Paging:
    // https://developer.android.com/topic/libraries/architecture/paging/v3-overview
    implementation("androidx.paging:paging-common-ktx:$pagingVersion")
    implementation("androidx.paging:paging-compose-android:$pagingVersion")
    implementation("androidx.room:room-paging:$roomVersion")

    // Reorder:
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
}

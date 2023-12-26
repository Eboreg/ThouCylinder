import java.util.Properties
import java.io.FileInputStream

val keystoreProperties = Properties()
val secretsProperties = Properties()

keystoreProperties.load(FileInputStream(rootProject.file("keystore.properties")))
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

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    defaultConfig {
        manifestPlaceholders += mapOf()
        val youtubeApiKey = secretsProperties["youtubeApiKey"] as String
        val discogsApiKey = secretsProperties["discogsApiKey"] as String
        val discogsApiSecret = secretsProperties["discogsApiSecret"] as String
        val spotifyClientId = secretsProperties["spotifyClientId"] as String
        val spotifyClientSecret = secretsProperties["spotifyClientSecret"] as String
        val lastFmApiKey = secretsProperties["lastFmApiKey"] as String
        val lastFmApiSecret = secretsProperties["lastFmApiSecret"] as String

        applicationId = "us.huseli.thoucylinder"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.2.0"
        manifestPlaceholders["redirectSchemeName"] = "klaatu"
        manifestPlaceholders["redirectHostName"] = "thoucylinder"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        setProperty("archivesBaseName", "thoucylinder_$versionName")
        buildConfigField("String", "youtubeApiKey", "\"$youtubeApiKey\"")
        buildConfigField("String", "discogsApiKey", "\"$discogsApiKey\"")
        buildConfigField("String", "discogsApiSecret", "\"$discogsApiSecret\"")
        buildConfigField("String", "spotifyClientId", "\"$spotifyClientId\"")
        buildConfigField("String", "spotifyClientSecret", "\"$spotifyClientSecret\"")
        buildConfigField("String", "lastFmApiKey", "\"$lastFmApiKey\"")
        buildConfigField("String", "lastFmApiSecret", "\"$lastFmApiSecret\"")
        signingConfig = signingConfigs.getByName("release")
    }

    buildTypes {
        debug {
            isDebuggable = true
            // isRenderscriptDebuggable = true
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
val roomVersion = "2.6.1"
val daggerVersion = "2.50"
val media3Version = "1.2.0"
val pagingVersion = "3.3.0-alpha02"

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Compose:
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))

    // Material:
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose related:
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.paging:paging-compose-android:$pagingVersion")

    // Lifecycle:
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")

    // Hilt:
    implementation("com.google.dagger:hilt-android:$daggerVersion")
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
    implementation("com.github.Eboreg:RetainTheme:3.1.0")

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
    implementation("androidx.room:room-paging:$roomVersion")

    // Reorder:
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    // Spotify:
    implementation("com.spotify.android:auth:2.1.1")

    // Levenshtein string distance:
    implementation("org.apache.commons:commons-text:1.11.0")

    // SimpleStorage for easier file handling:
    implementation("com.anggrayudi:storage:1.5.5")

    // XStream to parse XML:
    implementation("com.thoughtworks.xstream:xstream:1.4.20")
}

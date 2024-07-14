import dagger.hilt.android.plugin.util.capitalize
import java.io.FileInputStream
import java.util.Properties

val currentVersionCode = 19
val currentVersionName = "0.8.0"
val keystoreProperties = Properties()
val secretsProperties = Properties()

keystoreProperties.load(FileInputStream(rootProject.file("keystore.properties")))
secretsProperties.load(FileInputStream(rootProject.file("secrets.properties")))

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sentry)
    id("kotlin-parcelize")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "us.huseli.thoucylinder"
    compileSdk = 34

    applicationVariants.all {
        outputs.all {
            val variantName = name
            val taskSuffix = variantName.capitalize()
            val assembleTaskName = "assemble$taskSuffix"
            val bundleTaskName = "bundle$taskSuffix"

            val archiveTask = tasks.create(name = "archive$taskSuffix") {
                actions.add(
                    Action {
                        val inDir = outputFile.parentFile
                        val outDir =
                            File("${inDir?.parentFile?.path}/$variantName-$versionName").apply { mkdirs() }

                        inDir?.listFiles()?.filter { it.isFile && it.name.contains(versionName) }?.forEach { file ->
                            file.copyTo(File(outDir, file.name), overwrite = true)
                        }
                    }
                )
            }

            tasks[assembleTaskName]?.finalizedBy(archiveTask)
            tasks[bundleTaskName]?.finalizedBy(archiveTask)
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    defaultConfig {
        manifestPlaceholders += mapOf("redirectSchemeName" to "klaatu")
        val discogsApiKey = secretsProperties["discogsApiKey"] as String
        val discogsApiSecret = secretsProperties["discogsApiSecret"] as String
        val spotifyClientId = secretsProperties["spotifyClientId"] as String
        val spotifyClientSecret = secretsProperties["spotifyClientSecret"] as String
        val lastFmApiKey = secretsProperties["lastFmApiKey"] as String
        val lastFmApiSecret = secretsProperties["lastFmApiSecret"] as String

        applicationId = "us.huseli.fistopy"
        minSdk = 26
        targetSdk = 34
        versionCode = currentVersionCode
        versionName = currentVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        setProperty("archivesBaseName", "fistopy_$versionName")

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
            manifestPlaceholders["hostName"] = "fistopy.debug"
            manifestPlaceholders["redirectHostName"] = "fistopy.debug"
            buildConfigField("String", "hostName", "\"fistopy.debug\"")
        }
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            manifestPlaceholders["hostName"] = "fistopy"
            manifestPlaceholders["redirectHostName"] = "fistopy"
            buildConfigField("String", "hostName", "\"fistopy\"")
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
}

composeCompiler {
    enableStrongSkippingMode = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)

    // Compose:
    implementation(platform(libs.androidx.compose.bom))

    // Material:
    implementation(libs.androidx.material3)
    implementation(libs.material.icons.extended)

    // Compose related:
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Lifecycle:
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Hilt:
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Media3:
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)

    // Room:
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Gson:
    implementation(libs.gson)

    // Theme etc:
    implementation(libs.retain.theme)

    // FFMPEG:
    implementation(files("ffmpeg-kit.aar"))
    // implementation("com.arthenica:ffmpeg-kit-audio:6.0-2")
    implementation(libs.smart.exception.java)

    // Splashscreen:
    implementation(libs.androidx.core.splashscreen)

    // Reorder:
    implementation(libs.reorderable)

    // Levenshtein string distance:
    implementation(libs.commons.text)

    // SimpleStorage for easier file handling:
    implementation(libs.storage)

    // XStream to parse XML:
    implementation(libs.xstream)

    // Glance for widget:
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Trying out "immutable collection":
    implementation(libs.kotlinx.collections.immutable)

    // Track amplitude waveform shit:
    implementation(libs.amplituda)
    implementation(libs.compose.audiowaveform)

    // Compose tracing for debugging/optimization:
    implementation(libs.androidx.runtime.tracing)

    // Coil for async image loading:
    // implementation(libs.coil.base)
    implementation(libs.coil.compose)

    // Sentry
    implementation(libs.sentry)
    implementation(libs.sentry.compose)
}


sentry {
    org.set("huselius")
    projectName.set("fistopy")

    // this will upload your source code to Sentry to show it as part of the stack traces
    // disable if you don't want to expose your sources
    includeSourceContext.set(true)
}

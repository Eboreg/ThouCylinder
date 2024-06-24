// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.dagger.hilt.android) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.sentry) apply false
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            val buildDir = project.layout.buildDirectory.asFile.get().absolutePath

            freeCompilerArgs.addAll(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${buildDir}/compose_compiler",
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${buildDir}/compose_compiler",
            )
        }
        /*
        kotlinOptions {
            val buildDir = project.layout.buildDirectory.asFile.get().absolutePath

            freeCompilerArgs += listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${buildDir}/compose_compiler",
            )
            freeCompilerArgs += listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${buildDir}/compose_compiler",
            )
        }
         */
    }
}

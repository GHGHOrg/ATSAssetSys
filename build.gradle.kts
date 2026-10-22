// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("core/src/**/*.kt", "app/src/**/*.kt")
        ktlint("1.8.0")
            .editorConfigOverride(
                mapOf<String, Any>("ktlint_function_naming_ignore_when_annotated_with" to "Composable"),
            )
    }
    kotlinGradle {
        target("*.gradle.kts", "core/*.gradle.kts", "app/*.gradle.kts")
        ktlint("1.8.0")
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

// No price source key is built into the app (30 decision 0): the key is entered in Settings and kept in the encrypted
// database. The build reads no key, so every build, CI's included, is the same app.
android {
    namespace = "app.ghghorg.atsassets"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.ghghorg.atsassets"
        minSdk = 33
        targetSdk = 37
        versionCode = 46
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 36 decision 1: after how many minutes a backup is due; 0 means spec section 9's 7 days (core's BackupReminder).
        // Only the check build sets another time (decision 2); a unit test checks the debug build's is 0.
        buildConfigField("long", "BACKUP_DUE_AFTER_MINUTES", "0L")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
        // 34 decision 2: the debug app under another id and name ("ATS Check", app/src/check), for phone checks that
        // must not touch your data, such as a restore. Built only when asked (assembleCheck); CI builds debug only.
        create("check") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".check"
            versionNameSuffix = "-check"
            // 36 decision 2: ATS Check treats a backup as due after 10 minutes, for 32's phone check part B.
            buildConfigField("long", "BACKUP_DUE_AFTER_MINUTES", "10L")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // 36 decision 1: for BACKUP_DUE_AFTER_MINUTES.
        buildConfig = true
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room3.runtime)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.sqlcipher.android) {
        artifact {
            name = "sqlcipher-android"
            type = "aar"
        }
    }
    implementation(libs.androidx.sqlite)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(testFixtures(project(":core")))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

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
        // 38a decisions 2 and 2d: MAJOR.MINOR.PATCH; the first release (38c) is 0.38.0, the first productive one (40)
        // 1.0.0. versionCode rises with every build that is installed.
        versionCode = 53
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 36 decision 1: after how many minutes a backup is due; 0 means spec section 9's 7 days (core's BackupReminder).
        // The release build, your daily app, keeps 0 (BackupDueReleaseTest, run in CI; 38a decision 1).
        buildConfigField("long", "BACKUP_DUE_AFTER_MINUTES", "0L")
    }

    buildTypes {
        release {
            // R8 on the libraries only, not the app's own code (open item 3). 38b decision 5a: Kotlin's part classes
            // keep their names (src/main/keepRules/rules.keep), after R8 moved one away from its package-private parent
            // and the release crashed at start (open item 72).
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
        // AC-65 (CR4, 38a): the debug build is the development app, "ATS Dev" (app/src/debug), under its own id next
        // to your app, with test data only. It replaces the check build ("ATS Check", 34 and 36) and keeps its
        // 10 minutes for the backup reminder (BackupDueDebugTest).
        debug {
            applicationIdSuffix = ".debug"
            // 38a decision 2a: the development app shows x.y.z-dev.n, n its versionCode (1.2.0-dev.53).
            versionNameSuffix = "-dev.${defaultConfig.versionCode}"
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
    // 41 step 5 (AC-71, plan/release.md section 3): the schemas Room writes are the screen tests' assets, so
    // MigrationFrom14Test builds 1.0.0's database from 14.json.
    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

// 41 decision 1 (CR5, AC-70): the release build's runtime library groups, written for LicenceListTest, which compares
// them with About's list (ui/Licences.kt), so a library added or removed without the list turns the build red. A platform
// (a BOM) ships no code and is left out; the app's own modules are not libraries.
abstract class RuntimeGroups : DefaultTask() {
    @get:Input
    abstract val groups: SetProperty<String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun write() {
        output.get().asFile.writeText(groups.get().sorted().joinToString("\n", postfix = "\n"))
    }
}

object LibraryGroups {
    private val PLATFORMS = setOf("platform", "enforced-platform")

    fun of(root: ResolvedComponentResult): Set<String> {
        val seen = mutableSetOf<ComponentIdentifier>()
        val groups = sortedSetOf<String>()
        val pending = ArrayDeque(listOf(root))
        while (pending.isNotEmpty()) {
            val component = pending.removeFirst()
            if (!seen.add(component.id)) continue
            val id = component.id
            if (id is ModuleComponentIdentifier && !isPlatform(component)) groups.add(id.group)
            component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { pending.add(it.selected) }
        }
        return groups
    }

    private fun isPlatform(component: ResolvedComponentResult): Boolean =
        component.variants.any { variant ->
            val category = variant.attributes.keySet().firstOrNull { it.name == "org.gradle.category" }
            category != null && variant.attributes.getAttribute(category).toString() in PLATFORMS
        }
}

class RuntimeGroupsArgument(
    @get:Internal val file: Provider<RegularFile>,
) : CommandLineArgumentProvider {
    override fun asArguments(): List<String> = listOf("-Datsassets.runtimeGroups=${file.get().asFile.absolutePath}")
}

val runtimeGroups =
    tasks.register<RuntimeGroups>("runtimeGroups") {
        groups.set(
            configurations.named("releaseRuntimeClasspath").flatMap { classpath ->
                classpath.incoming.resolutionResult.rootComponent
                    .map { root -> LibraryGroups.of(root) }
            },
        )
        output.set(layout.buildDirectory.file("licences/runtime-groups.txt"))
    }

tasks.withType<Test>().configureEach {
    val groupsFile = runtimeGroups.flatMap { it.output }
    inputs.file(groupsFile).withPropertyName("runtimeGroups").withPathSensitivity(PathSensitivity.NONE)
    jvmArgumentProviders.add(RuntimeGroupsArgument(groupsFile))
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
    // 41 step 5 (your A, 2026-11-24): Room's MigrationTestHelper, a second check of the migration from 14 beside
    // MigrationFrom14Test's file built by hand; screen tests only, not in the app.
    androidTestImplementation(libs.androidx.room3.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

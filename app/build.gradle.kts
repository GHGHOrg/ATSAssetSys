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

// 44a step 3 (CR7, spec K9, AC-61 (e)): the design token files in design/tokens/ are the source of the theme; the unit
// tests read them from there, and a change to them runs the tests again.
class DesignTokensArgument(
    @get:Internal val dir: File,
    @get:Internal val icons: File,
) : CommandLineArgumentProvider {
    override fun asArguments(): List<String> = listOf("-Datsassets.tokens=${dir.absolutePath}", "-Datsassets.icons=${icons.absolutePath}")
}

val designTokens: File = rootProject.file("design/tokens")
val designIcons: File = rootProject.file("design/icons")

// 44a step 4 (spec K9, design/decisions.md D-22; 44a decision 6): the theme is built from the token files. This task reads
// design/tokens/*.tokens.json (W3C design tokens, DTCG 2025.10) and writes DesignTokens.kt into the build folder before the
// app compiles; the theme (ui/theme/) uses it, so a token change reaches the app with no Kotlin edit. ThemeTokensTest
// compares the generated values with the files.
abstract class DesignTokensKotlin : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokens: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun write() {
        val light = TokenFile.read(tokens.file("color.light.tokens.json").get().asFile)
        val dark = TokenFile.read(tokens.file("color.dark.tokens.json").get().asFile)
        val base = TokenFile.read(tokens.file("base.tokens.json").get().asFile)
        val dir = output.get().asFile.resolve("app/ghghorg/atsassets/ui/theme")
        dir.deleteRecursively()
        dir.mkdirs()
        dir.resolve("DesignTokens.kt").writeText(TokenFile.kotlin(light, dark, base))
    }
}

object TokenFile {
    class Token(
        val type: String?,
        val value: Any?,
    )

    fun read(file: File): Map<String, Token> {
        val into = linkedMapOf<String, Token>()
        collect(groovy.json.JsonSlurper().parse(file) as Map<*, *>, "", null, into)
        return into
    }

    private fun collect(
        group: Map<*, *>,
        prefix: String,
        inherited: String?,
        into: MutableMap<String, Token>,
    ) {
        val type = group["\$type"] as String? ?: inherited
        for ((key, child) in group) {
            key as String
            if (key.startsWith("$") || child !is Map<*, *>) continue
            val name = if (prefix.isEmpty()) key else "$prefix.$key"
            val isToken = child.containsKey("\$value")
            if (isToken) {
                into[name] = Token(child["\$type"] as String? ?: type, child["\$value"])
            } else {
                collect(child, name, type, into)
            }
        }
    }

    private fun resolve(
        all: Map<String, Token>,
        name: String,
    ): Token {
        var token = all.getValue(name)
        while ((token.value as? String)?.startsWith("{") == true) token = all.getValue((token.value as String).trim('{', '}'))
        return token
    }

    private fun camel(name: String): String {
        val parts = name.split('-')
        return parts.first() + parts.drop(1).joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }
    }

    private fun argb(color: Map<*, *>): String {
        val hex = (color["hex"] as String).removePrefix("#").uppercase()
        val alpha = Math.round(((color["alpha"] as Number?)?.toDouble() ?: 1.0) * 255).toInt()
        return "0x" + "%02X".format(alpha) + hex
    }

    private fun px(value: Any?): Int = if (value is Map<*, *>) (value["value"] as Number).toInt() else (value as Number).toInt()

    fun kotlin(
        light: Map<String, Token>,
        dark: Map<String, Token>,
        base: Map<String, Token>,
    ): String {
        val colors = light.filter { it.value.type == "color" }.keys.map { it.removePrefix("color.") }
        val darkColors = dark.filter { it.value.type == "color" }.keys.map { it.removePrefix("color.") }
        require(darkColors == colors) { "light and dark differ (D-04)" }

        fun argbMap(all: Map<String, Token>) =
            colors.joinToString(",\n") { "            \"${camel(it)}\" to ${argb(resolve(all, "color.$it").value as Map<*, *>)}" }

        fun shadow(all: Map<String, Token>) =
            (all.getValue("shadow.card").value as List<*>).joinToString(", ") { layer ->
                layer as Map<*, *>
                val color = argb(layer["color"] as Map<*, *>)
                val sizes = listOf("offsetX", "offsetY", "blur", "spread").joinToString(", ") { px(layer[it]).toString() }
                "TokenShadowLayer(Color($color), $sizes)"
            }
        val roles =
            base.keys
                .filter { it.startsWith("type.") }
                .map { it.split('.')[1] }
                .distinct()
        val types =
            roles.joinToString(",\n") { role ->
                val family = resolve(base, "type.$role.family").value as String
                val size = px(base.getValue("type.$role.size").value)
                val line = px(base.getValue("type.$role.line-height").value)
                val weight = px(base.getValue("type.$role.weight").value)
                "            \"${camel(role)}\" to TokenType(\"$family\", $size, $line, $weight)"
            }
        val sizeTokens = base.filter { (name, token) -> !name.startsWith("type.") && token.type in setOf("dimension", "number") }
        val sizes =
            sizeTokens.entries.joinToString(",\n") { (name, token) ->
                "            \"${camel(name.replace('.', '-'))}\" to ${px(token.value)}"
            }
        return buildString {
            appendLine("// Generated from design/tokens/ by app/build.gradle.kts (DesignTokensKotlin; 44a step 4). Do not edit:")
            appendLine("// change the token files (spec K9, design/decisions.md D-22).")
            appendLine("package app.ghghorg.atsassets.ui.theme")
            appendLine()
            appendLine("import androidx.compose.ui.graphics.Color")
            appendLine()
            appendLine("internal data class TokenShadowLayer(")
            appendLine("    val color: Color,")
            listOf("offsetX", "offsetY", "blur", "spread").forEach { appendLine("    val $it: Int,") }
            appendLine(")")
            appendLine()
            appendLine("internal data class TokenType(val family: String, val size: Int, val lineHeight: Int, val weight: Int)")
            appendLine()
            appendLine("internal class TokenPalette(private val argb: Map<String, Long>, val cardShadow: List<TokenShadowLayer>) {")
            colors.forEach { appendLine("    val ${camel(it)}: Color get() = Color(argb.getValue(\"${camel(it)}\"))") }
            appendLine("}")
            appendLine()
            appendLine("internal object TokenColors {")
            appendLine("    val lightArgb: Map<String, Long> =")
            appendLine("        mapOf(")
            appendLine(argbMap(light) + ",")
            appendLine("        )")
            appendLine("    val darkArgb: Map<String, Long> =")
            appendLine("        mapOf(")
            appendLine(argbMap(dark) + ",")
            appendLine("        )")
            appendLine("    val light = TokenPalette(lightArgb, listOf(${shadow(light)}))")
            appendLine("    val dark = TokenPalette(darkArgb, listOf(${shadow(dark)}))")
            appendLine("}")
            appendLine()
            appendLine("internal object TokenBase {")
            appendLine("    val sizes: Map<String, Int> =")
            appendLine("        mapOf(")
            appendLine("$sizes,")
            appendLine("        )")
            appendLine("    val types: Map<String, TokenType> =")
            appendLine("        mapOf(")
            appendLine("$types,")
            appendLine("        )")
            appendLine("}")
        }
    }
}

val designTokensKotlin =
    tasks.register<DesignTokensKotlin>("designTokensKotlin") {
        tokens.set(designTokens)
        output.set(layout.buildDirectory.dir("generated/designTokens"))
    }

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(designTokensKotlin, DesignTokensKotlin::output)
    }
}

tasks.withType<Test>().configureEach {
    val groupsFile = runtimeGroups.flatMap { it.output }
    inputs.file(groupsFile).withPropertyName("runtimeGroups").withPathSensitivity(PathSensitivity.NONE)
    jvmArgumentProviders.add(RuntimeGroupsArgument(groupsFile))
    inputs.files(fileTree(designTokens)).withPropertyName("designTokens").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree(designIcons)).withPropertyName("designIcons").withPathSensitivity(PathSensitivity.RELATIVE)
    jvmArgumentProviders.add(DesignTokensArgument(designTokens, designIcons))
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

package app.ghghorg.atsassets.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

// 44a step 4 (CR7; spec K9 and AC-61 (e); 44a decision 6): the theme is built from design/tokens/. The Kotlin the build
// generates from the token files holds every color and type role as the files say, and the color scheme the theme
// gives Material is made of those colors, never the wallpaper's (spec 6, Theme: dynamic color off).
class ThemeTokensTest {
    private fun tokens(name: String): Map<String, Pair<String?, JsonElement>> {
        val path = System.getProperty("atsassets.tokens")
        assertNotNull("The build passes the design token folder", path)
        val into = linkedMapOf<String, Pair<String?, JsonElement>>()
        collect(Json.parseToJsonElement(File(path!!, name).readText()).jsonObject, "", null, into)
        return into
    }

    private fun collect(
        group: JsonObject,
        prefix: String,
        inherited: String?,
        into: MutableMap<String, Pair<String?, JsonElement>>,
    ) {
        val type = (group["\$type"] as? JsonPrimitive)?.content ?: inherited
        for ((key, child) in group) {
            if (key.startsWith("$") || child !is JsonObject) continue
            val name = if (prefix.isEmpty()) key else "$prefix.$key"
            if ("\$value" in child) {
                into[name] = ((child["\$type"] as? JsonPrimitive)?.content ?: type) to child.getValue("\$value")
            } else {
                collect(child, name, type, into)
            }
        }
    }

    // The ARGB of a color token, references followed, alpha included.
    private fun argb(
        all: Map<String, Pair<String?, JsonElement>>,
        name: String,
    ): Long {
        var value = all.getValue(name).second
        while (value is JsonPrimitive && value.content.startsWith("{")) value = all.getValue(value.content.trim('{', '}')).second
        val color = value.jsonObject
        val hex = color.getValue("hex").jsonPrimitive.content.removePrefix("#").toLong(16)
        val alpha = ((color["alpha"] as? JsonPrimitive)?.double ?: 1.0).let { (it * 255).roundToInt().toLong() }
        return (alpha shl 24) or hex
    }

    private fun camel(name: String) = name.split('-').mapIndexed { i, part -> if (i == 0) part else part.replaceFirstChar { it.uppercase() } }.joinToString("")

    @Test
    fun theGeneratedColors_areTheTokenFiles_inBothThemes() {
        for ((file, generated) in listOf("color.light.tokens.json" to TokenColors.lightArgb, "color.dark.tokens.json" to TokenColors.darkArgb)) {
            val all = tokens(file)
            val expected = all.filter { it.value.first == "color" }.keys.associate { camel(it.removePrefix("color.")) to argb(all, it) }
            assertEquals(file, expected, generated)
        }
    }

    @Test
    fun theGeneratedTypeRoles_areTheTokenFiles() {
        val all = tokens("base.tokens.json")
        val roles = all.keys.filter { it.startsWith("type.") }.map { it.split('.')[1] }.toSet()
        assertEquals(roles.map { camel(it) }.toSet(), TokenBase.types.keys)
        for (role in roles) {
            val type = TokenBase.types.getValue(camel(role))
            var family = all.getValue("type.$role.family").second.jsonPrimitive.content
            if (family.startsWith("{")) family = all.getValue(family.trim('{', '}')).second.jsonPrimitive.content
            assertEquals("$role family", family, type.family)
            assertEquals("$role size", all.getValue("type.$role.size").second.jsonObject.getValue("value").jsonPrimitive.int, type.size)
            assertEquals("$role line height", all.getValue("type.$role.line-height").second.jsonObject.getValue("value").jsonPrimitive.int, type.lineHeight)
            assertEquals("$role weight", all.getValue("type.$role.weight").second.jsonPrimitive.int, type.weight)
        }
    }

    @Test
    fun theGeneratedSizes_areTheTokenFiles() {
        val all = tokens("base.tokens.json")
        val sizes =
            all.filter { (name, token) -> !name.startsWith("type.") && (token.first == "dimension" || token.first == "number") }
                .map { (name, token) ->
                    val value = token.second
                    camel(name.replace('.', '-')) to (if (value is JsonObject) value.getValue("value") else value).jsonPrimitive.double.toFloat()
                }.toMap()
        assertEquals(sizes, TokenBase.sizes)
    }

    // Spec 6, Theme: the app's own colors only; the scheme Material draws with comes from the tokens.
    @Test
    fun theColorScheme_isMadeOfTheTokens_inBothThemes() {
        for ((palette, argb) in listOf(TokenColors.light to TokenColors.lightArgb, TokenColors.dark to TokenColors.darkArgb)) {
            val scheme = atsColorScheme(palette, dark = palette === TokenColors.dark)
            fun color(name: String) = Color(argb.getValue(name)).toArgb()
            assertEquals(color("page"), scheme.background.toArgb())
            assertEquals(color("text"), scheme.onBackground.toArgb())
            assertEquals(color("page"), scheme.surface.toArgb())
            assertEquals(color("text"), scheme.onSurface.toArgb())
            assertEquals(color("card"), scheme.surfaceVariant.toArgb())
            assertEquals(color("textSecondary"), scheme.onSurfaceVariant.toArgb())
            assertEquals(color("gold"), scheme.primary.toArgb())
            assertEquals(color("onGold"), scheme.onPrimary.toArgb())
            assertEquals(color("buttonText"), scheme.secondary.toArgb())
            assertEquals(color("fieldLine"), scheme.outline.toArgb())
            assertEquals(color("divider"), scheme.outlineVariant.toArgb())
            assertEquals(color("error"), scheme.error.toArgb())
        }
    }
}

package app.ghghorg.atsassets.ui.theme

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

// 44a step 3 (CR7; spec 6, Visual design; K9; design/decisions.md D-04, D-20, D-21): the design token files in
// design/tokens/ are W3C design tokens (DTCG 2025.10). Light and dark hold the same tokens, every color is a DTCG
// color object whose hex matches its components, every reference resolves, and the values spec 6 names are the ones
// in the files.
class TokenFilesTest {
    private class Token(
        val type: String?,
        val value: JsonElement,
    )

    private fun dir(): File {
        val path = System.getProperty("atsassets.tokens")
        assertNotNull("The build passes the design token folder (app/build.gradle.kts)", path)
        return File(path!!)
    }

    private fun read(name: String): Map<String, Token> {
        val file = File(dir(), name)
        assertTrue("$name exists in design/tokens/", file.isFile)
        val tokens = linkedMapOf<String, Token>()
        collect(Json.parseToJsonElement(file.readText()).jsonObject, "", null, tokens)
        return tokens
    }

    // A DTCG token is an object with "$value"; a group passes its "$type" down to the tokens inside it.
    private fun collect(
        group: JsonObject,
        prefix: String,
        inherited: String?,
        into: MutableMap<String, Token>,
    ) {
        val type = (group["\$type"] as? JsonPrimitive)?.content ?: inherited
        for ((key, child) in group) {
            if (key.startsWith("$") || child !is JsonObject) continue
            val name = if (prefix.isEmpty()) key else "$prefix.$key"
            if ("\$value" in child) {
                into[name] = Token((child["\$type"] as? JsonPrimitive)?.content ?: type, child.getValue("\$value"))
            } else {
                collect(child, name, type, into)
            }
        }
    }

    private val light by lazy { read("color.light.tokens.json") }
    private val dark by lazy { read("color.dark.tokens.json") }
    private val base by lazy { read("base.tokens.json") }

    private fun resolve(
        tokens: Map<String, Token>,
        name: String,
    ): Token {
        var token = tokens[name]
        assertNotNull("Token $name", token)
        repeat(4) {
            val text = (token!!.value as? JsonPrimitive)?.content
            if (text == null || !text.startsWith("{")) return token!!
            val target = text.removePrefix("{").removeSuffix("}")
            token = tokens[target]
            assertNotNull("$name refers to $target, which exists", token)
        }
        return token!!
    }

    private fun hex(
        tokens: Map<String, Token>,
        name: String,
    ): String {
        val token = resolve(tokens, name)
        assertEquals("$name is a color", "color", token.type)
        return token.value.jsonObject.getValue("hex").jsonPrimitive.content.uppercase()
    }

    private fun alpha(
        tokens: Map<String, Token>,
        name: String,
    ): Double = (resolve(tokens, name).value.jsonObject["alpha"] as? JsonPrimitive)?.double ?: 1.0

    private fun px(name: String): Int {
        val token = resolve(base, name)
        assertEquals("$name is a dimension", "dimension", token.type)
        val value = token.value.jsonObject
        assertEquals("$name is in px", "px", value.getValue("unit").jsonPrimitive.content)
        return value.getValue("value").jsonPrimitive.int
    }

    @Test
    fun lightAndDark_holdTheSameTokens_ofTheSameTypes() {
        assertEquals(light.keys, dark.keys)
        for (name in light.keys) assertEquals(name, light.getValue(name).type, dark.getValue(name).type)
    }

    @Test
    fun everyColor_isADtcgSrgbObject_whoseHexMatchesItsComponents() {
        for (tokens in listOf(light, dark)) {
            for ((name, token) in tokens) {
                if (token.type != "color" || token.value is JsonPrimitive) continue
                val color = token.value.jsonObject
                assertEquals("$name colorSpace", "srgb", color.getValue("colorSpace").jsonPrimitive.content)
                val components = color.getValue("components").jsonArray.map { it.jsonPrimitive.double }
                assertEquals("$name has 3 components", 3, components.size)
                components.forEach { assertTrue("$name component $it in 0..1", it in 0.0..1.0) }
                val hex = color.getValue("hex").jsonPrimitive.content
                assertTrue("$name hex $hex has 6 digits", Regex("#[0-9A-Fa-f]{6}").matches(hex))
                val fromComponents = components.joinToString("", "#") { "%02X".format((it * 255).roundToInt()) }
                assertEquals("$name hex against its components", hex.uppercase(), fromComponents)
                val alpha = (color["alpha"] as? JsonPrimitive)?.double ?: 1.0
                assertTrue("$name alpha $alpha in 0..1", alpha in 0.0..1.0)
            }
        }
    }

    @Test
    fun everyReference_resolvesToATokenOfTheSameType() {
        for (tokens in listOf(light, dark, base)) {
            for ((name, token) in tokens) {
                val text = (token.value as? JsonPrimitive)?.content ?: continue
                if (!text.startsWith("{")) continue
                assertEquals("$name has the type of what it refers to", token.type, resolve(tokens, name).type)
            }
        }
    }

    @Test
    fun theCardShadow_hasTwoLayers_inEachTheme() {
        for (tokens in listOf(light, dark)) {
            val shadow = tokens.getValue("shadow.card")
            assertEquals("shadow", shadow.type)
            assertEquals(2, (shadow.value as JsonArray).size)
        }
    }

    // Spec 6, Visual design (1), Number format and color, and the icon colors of (4) (design/decisions.md D-46).
    @Test
    fun theColorsSpec6Names_areTheOnesInTheFiles() {
        val expected =
            listOf(
                Triple("color.page", "#EBF0F8", "#0A1A36"),
                Triple("color.card", "#DFE7F6", "#0E2B47"),
                Triple("color.band", "#0A1A36", "#0A1A36"),
                Triple("color.on-band", "#FFFFFF", "#E6ECF5"),
                Triple("color.summary", "#153D5A", "#1C4F6C"),
                Triple("color.text", "#0A1A36", "#E6ECF5"),
                Triple("color.text-secondary", "#295C85", "#B0C4E8"),
                Triple("color.gold", "#C9A227", "#E0BC4A"),
                Triple("color.gain", "#1B6B1F", "#7FD47A"),
                Triple("color.loss", "#B3261E", "#FFB4AB"),
                Triple("color.gain-on-summary", "#8FE08A", "#8FE08A"),
                Triple("color.loss-on-summary", "#FFC2BB", "#FFC2BB"),
                Triple("color.error", "#7A541F", "#E2BF8D"),
                Triple("color.icon-action", "#0E2B47", "#E0BC4A"),
                Triple("color.icon-chevron", "#295C85", "#B0C4E8"),
                Triple("color.icon-band", "#FFFFFF", "#E6ECF5"),
            )
        for ((name, inLight, inDark) in expected) {
            assertEquals("$name in light", inLight, hex(light, name))
            assertEquals("$name in dark", inDark, hex(dark, name))
        }
        assertEquals("The dark bar has no pill", 0.0, alpha(dark, "color.bar-pill"), 0.0)
    }

    // Spec 6, Visual design (2), (3), (4) and (5).
    @Test
    fun theSizesSpec6Names_areTheOnesInTheFiles() {
        assertEquals(8, px("radius.default"))
        assertEquals(4, px("radius.checkbox"))
        assertEquals(12, px("space.margin"))
        assertEquals(8, px("space.gap"))
        assertEquals(10, px("space.inset"))
        assertEquals(48, px("space.touch"))
        assertEquals(24, px("icon.size"))
        assertEquals(2, resolve(base, "icon.stroke").value.jsonPrimitive.int)
        assertEquals(72, px("bar.height"))
        assertEquals(84, px("bar.pill-min-width"))
        val sizes =
            mapOf(
                "type.screen-title" to 22,
                "type.big-figure" to 32,
                "type.big-figure-fallback" to 28,
                "type.body" to 16,
                "type.small" to 14,
                "type.button" to 14,
                "type.one-day" to 12,
            )
        for ((role, size) in sizes) assertEquals(role, size, px("$role.size"))
        assertEquals("Asap", resolve(base, "type.body.family").value.jsonPrimitive.content)
        assertEquals("Asap Condensed", resolve(base, "type.one-day.family").value.jsonPrimitive.content)
        assertEquals(400, resolve(base, "type.body.weight").value.jsonPrimitive.int)
        assertEquals(500, resolve(base, "type.screen-title.weight").value.jsonPrimitive.int)
    }
}

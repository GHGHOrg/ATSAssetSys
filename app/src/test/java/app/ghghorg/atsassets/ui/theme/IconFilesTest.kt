package app.ghghorg.atsassets.ui.theme

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// 44a step 3 (CR7; spec 6, Visual design (4); C27; design/decisions.md D-19, D-24, D-26, D-47; 44a decisions 3 and 4):
// design/icons/icons.json names each icon the app uses, from the pinned Lucide release. Each has its SVG unchanged in
// design/icons/lucide/ and its vector drawable in res/drawable/, converted once by Android Studio's own converter; the
// only change to the converter's output is the stroke color, set to black so the theme's icon color tints it.
class IconFilesTest {
    private fun dir(): File {
        val path = System.getProperty("atsassets.icons")
        assertNotNull("The build passes the design icon folder (app/build.gradle.kts)", path)
        return File(path!!)
    }

    private val index by lazy {
        val file = File(dir(), "icons.json")
        assertTrue("design/icons/icons.json exists", file.isFile)
        Json.parseToJsonElement(file.readText()).jsonObject
    }

    private val icons by lazy { index.getValue("icons").jsonArray.map { it.jsonObject } }

    // Unit tests run in the app module's folder.
    private val drawables = File("src/main/res/drawable")

    @Test
    fun theIndex_pinsLucide_1_72_0() {
        assertEquals("1.72.0", index.getValue("lucide").jsonPrimitive.content)
    }

    @Test
    fun everyIcon_hasItsSvg_andItsDrawable() {
        assertTrue("At least the 24 icons of the drawings", icons.size >= 24)
        for (icon in icons) {
            val file = icon.getValue("file").jsonPrimitive.content
            val drawable = icon.getValue("drawable").jsonPrimitive.content
            assertTrue("$file.svg in design/icons/lucide/", File(dir(), "lucide/$file.svg").isFile)
            assertTrue("$drawable.xml in res/drawable/", File(drawables, "$drawable.xml").isFile)
            assertTrue("$drawable is named after its file", drawable == "ic_lucide_" + file.replace('-', '_'))
        }
    }

    @Test
    fun everyDrawable_keepsLucidesStroke_andHasAPlainColor() {
        for (icon in icons) {
            val drawable = icon.getValue("drawable").jsonPrimitive.content
            val xml = File(drawables, "$drawable.xml").readText()
            assertFalse("$drawable has no currentColor", "currentColor" in xml)
            assertTrue("$drawable is 24 by 24", "android:viewportWidth=\"24\"" in xml && "android:viewportHeight=\"24\"" in xml)
            assertTrue("$drawable keeps the stroke of 2 (D-24)", "android:strokeWidth=\"2\"" in xml)
            assertFalse("$drawable has no other stroke width", Regex("strokeWidth=\"(?!2\")").containsMatchIn(xml))
            assertTrue("$drawable keeps round caps and joins", "strokeLineCap=\"round\"" in xml && "strokeLineJoin=\"round\"" in xml)
        }
    }

    @Test
    fun theDrawingsNames_areEachInTheIndex() {
        val names = icons.map { it.getValue("drawingName").jsonPrimitive.content }.toSet()
        val drawn =
            setOf(
                "pie-chart", "briefcase", "wallet", "more-horizontal", "chevron-left", "chevron-right", "chevron-down",
                "chevron-up", "ellipsis-vertical", "plus", "x", "calendar", "check", "circle-alert", "circle-check",
                "triangle-alert", "info", "loader-circle", "lock", "eye", "eye-off", "settings", "arrow-up", "arrow-down",
            )
        assertEquals(emptySet<String>(), drawn - names)
    }

    // 44a step 5: the app's list of icons (LucideIcon) is the index's, file for file and drawable for drawable.
    @Test
    fun theAppsIcons_areTheIndexs() {
        assertEquals(icons.map { it.getValue("file").jsonPrimitive.content }, LucideIcon.entries.map { it.file })
        for (icon in LucideIcon.entries) {
            assertEquals(icon.file, "ic_lucide_" + icon.file.replace('-', '_'), resourceName(icon.res))
        }
    }

    private fun resourceName(res: Int): String {
        val fields = app.ghghorg.atsassets.R.drawable::class.java.fields
        return fields.first { it.getInt(null) == res }.name
    }

    @Test
    fun lucidesLicence_isKeptWithTheIcons() {
        val licence = File(dir(), "LICENSE-lucide")
        assertTrue("design/icons/LICENSE-lucide exists", licence.isFile)
        val text = licence.readText()
        assertTrue("ISC", text.startsWith("ISC License"))
        assertTrue("MIT for the Feather icons", "The MIT License (MIT)" in text && "Cole Bemis" in text)
    }
}

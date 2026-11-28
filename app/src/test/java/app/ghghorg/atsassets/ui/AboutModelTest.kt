package app.ghghorg.atsassets.ui

import org.junit.Assert.assertEquals
import org.junit.Test

// 41 (CR5, AC-70): About as the screen draws it (wireframe 11; CR2).
class AboutModelTest {
    @Test
    fun theVersion_isAsTheAppReportsIt_andEveryRowStartsClosed() {
        val model = About.of("1.1.0-dev.51", emptySet())
        assertEquals("1.1.0-dev.51", model.version)
        assertEquals(Licences.ALL.map { it.name }, model.rows.map { it.name })
        assertEquals(false, model.rows.any { it.open })
    }

    @Test
    fun anOpenedRow_isOpen_andMoreThanOneCanBe() {
        val model = About.of("1.1.0", setOf("SQLCipher for Android", "kotlinx.coroutines and kotlinx.serialization"))
        assertEquals(
            listOf("kotlinx.coroutines and kotlinx.serialization", "SQLCipher for Android"),
            model.rows.filter { it.open }.map { it.name },
        )
    }

    // 44a step 8 group (e) (C27): the font and the icons are rows with their own licences, beside SQLCipher's.
    @Test
    fun sqlCipher_theFont_andTheIcons_carryTheirOwnLicences_andTheOthersApache() {
        val model = About.of("1.1.0", emptySet())
        val own =
            mapOf(
                "SQLCipher for Android" to Licence.SQLCIPHER,
                "Asap and Asap Condensed (fonts)" to Licence.OFL_1_1,
                "Lucide (icons)" to Licence.ISC_MIT,
            )
        for ((name, licence) in own) assertEquals(name, licence, model.rows.single { it.name == name }.licence)
        assertEquals(setOf(Licence.APACHE_2_0), model.rows.filter { it.name !in own }.map { it.licence }.toSet())
    }
}

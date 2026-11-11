package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 23a: K10's lists and effective value (spec 3, 4.9; C14; decisions 2 and 4). */
class ClassificationTest {
    private val sectors = listOf("Technology", "Diversified")
    private val geographies = listOf("United States", "Global")

    @Test
    fun aSourceTextOnTheList_isTheEffectiveValue_fromTheSource() {
        assertEquals(EffectiveClass("Technology", ClassOrigin.SOURCE), Classification.effective(sectors, "Technology"))
    }

    @Test
    fun aSourceText_mapsToTheListsOwnSpelling_ignoringCaseAndSpaces() {
        // D7 and AC-58: "united  states" and "technology " match.
        assertEquals("United States", Classification.valueFor(geographies, " united  states "))
        assertEquals("Technology", Classification.valueFor(sectors, "technology "))
        assertEquals(EffectiveClass("United States", ClassOrigin.SOURCE), Classification.effective(geographies, "UNITED STATES"))
        assertNull(Classification.valueFor(sectors, "Energy"))
    }

    @Test
    fun noText_anEmptyText_orNA_isNoValue_soUnclassified() {
        // Decision 2 and C14: an ETF's empty profile, a missing field, an empty country, "N/A".
        for (text in listOf(null, "", "   ", "N/A", "n/a", " N/A ")) {
            assertNull("[$text]", Classification.sourceText(text))
            assertEquals("[$text]", EffectiveClass(null, ClassOrigin.UNCLASSIFIED), Classification.effective(sectors, text))
        }
    }

    @Test
    fun aValueText_isTrimmed() {
        assertEquals("Technology", Classification.sourceText("  Technology "))
        assertEquals("US", Classification.sourceText("US"))
    }

    @Test
    fun aNewText_isAddedOnce_trimmed_atTheEnd() {
        // 4.9: neither a remembered text (none before 25, decision 4) nor a name matches: the text is added.
        assertEquals(listOf("Technology", "Diversified", "Energy"), Classification.withSource(sectors, " Energy "))
        val once = Classification.withSource(emptyList(), "Technology")
        assertEquals(listOf("Technology"), once)
        assertEquals(once, Classification.withSource(once, "technology"))
        assertEquals(once, Classification.withSource(once, " TECHNOLOGY  "))
    }

    @Test
    fun noValueText_addsNothing() {
        for (text in listOf(null, "", "  ", "N/A")) {
            assertEquals("[$text]", sectors, Classification.withSource(sectors, text))
        }
    }

    @Test
    fun additions_areEachNewValueTextOnce_sectorsFirst_eachInProfileOrder() {
        // Step 5: what a refresh's profiles add to the lists (4.9), saved in the same batch as the prices.
        val lists = ClassLists(sectors = listOf("Technology"))
        val profiles =
            listOf(
                SourceProfile("technology", "US"),
                SourceProfile("Energy", "us"),
                SourceProfile(null, "N/A"),
                SourceProfile("Energy ", "Canada"),
            )
        assertEquals(
            listOf(ClassList.SECTOR to "Energy", ClassList.GEOGRAPHY to "US", ClassList.GEOGRAPHY to "Canada"),
            Classification.additions(lists, profiles),
        )
        assertEquals(emptyList<Pair<ClassList, String>>(), Classification.additions(lists, listOf(SourceProfile(null, null))))
        assertEquals(emptyList<Pair<ClassList, String>>(), Classification.additions(lists, emptyList()))
    }

    @Test
    fun extended_addsTheAdditionsToTheirLists() {
        val lists = ClassLists(sectors = listOf("Technology"))
        val extended = Classification.extended(lists, listOf(SourceProfile("Energy", "US"), SourceProfile("technology", null)))
        assertEquals(ClassLists(sectors = listOf("Technology", "Energy"), geographies = listOf("US")), extended)
    }

    @Test
    fun aTextNotYetOnTheList_isStillItsOwnValue_fromTheSource() {
        // The list is updated with each refresh (step 5); until then the source's own text is shown, trimmed.
        assertEquals(EffectiveClass("Energy", ClassOrigin.SOURCE), Classification.effective(sectors, " Energy"))
    }
}

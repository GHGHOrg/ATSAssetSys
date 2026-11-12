package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.ClassOverride
import app.ghghorg.atsassets.core.ListRefusal
import app.ghghorg.atsassets.core.ListResult
import app.ghghorg.atsassets.core.ListUsers
import app.ghghorg.atsassets.core.RememberedText
import app.ghghorg.atsassets.core.SourceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** 25: the list screen's model (section 6, wireframe 10f frames F and G; CR2; 4.9, AC-58). */
class ClassListModelTest {
    // AC-58's sample: Technology also matches "Information Technology" and a second text; AAPL uses Technology.
    private val lists =
        ClassLists(
            sectors = listOf("Technology", "Diversified"),
            geographies = listOf("United States"),
            remembered =
            listOf(
                RememberedText(ClassList.SECTOR, "Information Technology", "Technology"),
                RememberedText(ClassList.SECTOR, "Tech", "Technology"),
            ),
        )
    private val users =
        ListUsers(
            holdings = listOf("p1" to "AAPL", "p2" to "AAPL", "p1" to "VT"),
            profiles = mapOf("AAPL" to SourceProfile("Information Technology", "United States")),
            overrides = listOf(ClassOverride("p1", "VT", ClassList.SECTOR, "Diversified")),
        )

    private fun model(
        edit: ClassValueEdit? = null,
        outcome: ListResult? = null,
        saveRefusal: SaveRefusal? = null,
        saving: Boolean = false,
        list: ClassList = ClassList.SECTOR,
    ) = ClassListScreens.of(list, lists, users, edit, outcome, saveRefusal, saving)

    // Frame F: each value in the list's order, its holdings (an override counts, 4.9) and its remembered texts.
    @Test
    fun eachValue_inTheListsOrder_withItsHoldings_andTheTextsThatMapToIt() {
        assertEquals(
            listOf(
                ClassValueRowModel("Technology", holdings = 2, alsoMatches = "Information Technology, Tech"),
                ClassValueRowModel("Diversified", holdings = 1, alsoMatches = null),
            ),
            model().rows,
        )
        assertEquals(listOf(ClassValueRowModel("United States", holdings = 2, alsoMatches = null)), model(list = ClassList.GEOGRAPHY).rows)
        assertEquals(ClassList.GEOGRAPHY, model(list = ClassList.GEOGRAPHY).list)
    }

    @Test
    fun anEmptyList_hasNoRows() {
        assertEquals(emptyList<ClassValueRowModel>(), ClassListScreens.of(ClassList.SECTOR, ClassLists(), ListUsers(), null, null, null, false).rows)
    }

    // Frame G, marker 15: the line under the list after each action, the layer closed.
    @Test
    fun theLineUnderTheList_saysWhatTheLastActionDid() {
        val after = lists
        assertEquals(ClassListLine.Added("Energy"), model(outcome = ListResult.Added(after, "Energy")).line)
        assertEquals(ClassListLine.Renamed("Diversified", "Multi"), model(outcome = ListResult.Renamed(after, "Diversified", "Multi", null)).line)
        assertEquals(
            ClassListLine.RenamedRemembering("United States", "USA"),
            model(outcome = ListResult.Renamed(after, "United States", "USA", "United States")).line,
        )
        assertEquals(ClassListLine.Deleted("Global"), model(outcome = ListResult.Deleted(after, "Global", emptyList())).line)
        assertEquals(ClassListLine.InUse("Technology", 2), model(outcome = ListResult.Refused(ListRefusal.InUse("Technology", 2))).line)
        assertEquals(ClassListLine.NotDeleted, model(saveRefusal = SaveRefusal.SAVE_FAILED).line)
        assertNull(model().line)
        assertNull(model().edit)
    }

    // Frame G: the Name layer with the typed text and why it was refused; the screen's line waits until it closes.
    @Test
    fun theNameLayer_carriesTheTypedText_andItsOwnLine() {
        val add = ClassValueEdit(renaming = null, text = "technology ")
        assertEquals(ClassNameEditModel(null, "technology ", null), model(edit = add).edit)
        assertEquals(
            ClassNameEditModel(null, "technology ", ClassNameLine.Duplicate("Technology")),
            model(edit = add, outcome = ListResult.Refused(ListRefusal.Duplicate("Technology"))).edit,
        )
        val rename = ClassValueEdit(renaming = "Diversified", text = "tech")
        assertEquals(
            ClassNameEditModel("Diversified", "tech", ClassNameLine.Remembered("Tech", "Technology")),
            model(edit = rename, outcome = ListResult.Refused(ListRefusal.Remembered("Tech", "Technology"))).edit,
        )
        assertEquals(ClassNameLine.Blank, model(edit = add, outcome = ListResult.Refused(ListRefusal.Blank)).edit!!.line)
        assertEquals(ClassNameLine.NotSaved, model(edit = add, saveRefusal = SaveRefusal.SAVE_FAILED).edit!!.line)
        assertNull(model(edit = add, saveRefusal = SaveRefusal.SAVE_FAILED).line)
    }

    @Test
    fun whileASaveRuns_theScreenIsNotEnabled() {
        assertFalse(model(saving = true).enabled)
        assertEquals(true, model().enabled)
    }
}

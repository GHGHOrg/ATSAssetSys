package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 25: K10's remembered texts, add, rename and delete, and the holdings that use a value (spec 4.9, AC-22, AC-58; decisions 1 to 3). */
class ClassListEditTest {
    // AC-58's sample file: Technology also matches "Information Technology"; AAPL is the one holding.
    private val sample = ClassLists(
        sectors = listOf("Technology", "Diversified"),
        geographies = listOf("United States", "Global"),
        remembered = listOf(RememberedText(ClassList.SECTOR, "Information Technology", "Technology")),
    )
    private val aapl = SourceProfile("Information Technology", "United States")
    private val users = ListUsers(holdings = listOf("p1" to "AAPL"), profiles = mapOf("AAPL" to aapl))

    private fun count(
        lists: ClassLists,
        list: ClassList,
        value: String,
        with: ListUsers = users,
    ) = Classification.holdingsUsing(lists, list, value, with)

    @Test
    fun ac58_eachValueShowsTheHoldingsThatUseIt() {
        assertEquals(1, count(sample, ClassList.SECTOR, "Technology"))
        assertEquals(0, count(sample, ClassList.SECTOR, "Diversified"))
        assertEquals(1, count(sample, ClassList.GEOGRAPHY, "United States"))
        assertEquals(0, count(sample, ClassList.GEOGRAPHY, "Global"))
    }

    @Test
    fun ac58_addingANameThatMatchesAValue_isRefusedAsADuplicate() {
        assertEquals(ListResult.Refused(ListRefusal.Duplicate("Technology")), Classification.add(sample, ClassList.SECTOR, "technology "))
        assertEquals(
            ListResult.Refused(ListRefusal.Duplicate("United States")),
            Classification.add(sample, ClassList.GEOGRAPHY, "united  states"),
        )
    }

    @Test
    fun ac58_deletingAValueInUse_isRefused() {
        assertEquals(
            ListResult.Refused(ListRefusal.InUse("Technology", 1)),
            Classification.delete(sample, ClassList.SECTOR, "Technology", users),
        )
        assertEquals(
            ListResult.Refused(ListRefusal.InUse("United States", 1)),
            Classification.delete(sample, ClassList.GEOGRAPHY, "United States", users),
        )
    }

    @Test
    fun ac58_deletingAnUnusedValue_removesIt() {
        assertEquals(
            ListResult.Deleted(sample.copy(geographies = listOf("United States")), "Global", emptyList()),
            Classification.delete(sample, ClassList.GEOGRAPHY, "Global", users),
        )
    }

    @Test
    fun ac58_renamingTechnologyToTech_keepsInformationTechnologyMappingToTech() {
        val result = Classification.rename(sample, ClassList.SECTOR, "Technology", "Tech", listOf(aapl))
        // Decision 1: no source text matches "Technology" itself, so nothing new is remembered.
        val after = ClassLists(
            sectors = listOf("Tech", "Diversified"),
            geographies = sample.geographies,
            remembered = listOf(RememberedText(ClassList.SECTOR, "Information Technology", "Tech")),
        )
        assertEquals(ListResult.Renamed(after, "Technology", "Tech", null), result)
        assertEquals("Tech", Classification.valueFor(after.sectors, "Information Technology", after.rememberedOn(ClassList.SECTOR)))
        assertEquals(1, count(after, ClassList.SECTOR, "Tech"))
        assertEquals(ListResult.Refused(ListRefusal.Duplicate("Tech")), Classification.add(after, ClassList.SECTOR, "Tech"))
    }

    @Test
    fun ac58_renamingUnitedStatesToUsa_changesAaplsGeographyToUsa() {
        val result = Classification.rename(sample, ClassList.GEOGRAPHY, "United States", "USA", listOf(aapl))
        val after = sample.copy(
            geographies = listOf("USA", "Global"),
            remembered = sample.remembered + RememberedText(ClassList.GEOGRAPHY, "United States", "USA"),
        )
        assertEquals(ListResult.Renamed(after, "United States", "USA", "United States"), result)
        assertEquals(
            EffectiveClass("USA", ClassOrigin.SOURCE),
            Classification.effective(after.geographies, aapl.geography, null, after.rememberedOn(ClassList.GEOGRAPHY)),
        )
        assertEquals(1, count(after, ClassList.GEOGRAPHY, "USA"))
    }

    @Test
    fun rule49Example_afterTechIsRenamedTechnology_techMapsToTechnology_andANewTechIsRefused() {
        val lists = ClassLists(sectors = listOf("Tech"))
        val profiles = listOf(SourceProfile("Tech", null))
        val renamed = Classification.rename(lists, ClassList.SECTOR, "Tech", "Technology", profiles) as ListResult.Renamed
        assertEquals("Tech", renamed.remembered)
        val after = renamed.lists
        assertEquals(listOf(RememberedText(ClassList.SECTOR, "Tech", "Technology")), after.remembered)
        assertEquals("Technology", Classification.valueFor(after.sectors, "tech ", after.rememberedOn(ClassList.SECTOR)))
        assertEquals(ListResult.Refused(ListRefusal.Remembered("Tech", "Technology")), Classification.add(after, ClassList.SECTOR, "Tech"))
        assertEquals(ListResult.Refused(ListRefusal.Remembered("Tech", "Technology")), Classification.add(after, ClassList.SECTOR, " TECH"))
    }

    @Test
    fun aRememberedText_isMatchedBeforeAName() {
        // Only stored data that breaks 4.9 has both; the order is 4.9's ("Remembered texts are checked before names").
        val remembered = listOf(RememberedText(ClassList.SECTOR, "Tech", "Technology"))
        assertEquals("Technology", Classification.valueFor(listOf("Tech", "Technology"), "tech", remembered))
    }

    @Test
    fun aSourceTextMatchingARememberedText_isFromTheSource_andAddsNothing() {
        val remembered = sample.rememberedOn(ClassList.SECTOR)
        assertEquals(
            EffectiveClass("Technology", ClassOrigin.SOURCE),
            Classification.effective(sample.sectors, " information  TECHNOLOGY", null, remembered),
        )
        assertEquals(sample.sectors, Classification.withSource(sample.sectors, "information technology", remembered))
        assertEquals(emptyList<Pair<ClassList, String>>(), Classification.additions(sample, listOf(aapl)))
    }

    @Test
    fun decision1_renamingAValueNoSourceTextMatches_remembersNothing_andItCanBeRenamedBack() {
        val renamed = Classification.rename(sample, ClassList.SECTOR, "Diversified", "Diversifed", listOf(aapl)) as ListResult.Renamed
        assertNull(renamed.remembered)
        assertEquals(sample.remembered, renamed.lists.remembered)
        val back = Classification.rename(renamed.lists, ClassList.SECTOR, "Diversifed", "Diversified", listOf(aapl))
        assertEquals(ListResult.Renamed(sample, "Diversifed", "Diversified", null), back)
    }

    @Test
    fun decision2_renamingBackToItsOwnRememberedText_isRefused() {
        val renamed = Classification.rename(sample, ClassList.GEOGRAPHY, "United States", "USA", listOf(aapl)) as ListResult.Renamed
        assertEquals(
            ListResult.Refused(ListRefusal.Remembered("United States", "USA")),
            Classification.rename(renamed.lists, ClassList.GEOGRAPHY, "USA", "united states", listOf(aapl)),
        )
    }

    @Test
    fun renamingToAnotherValuesName_orRememberedText_isRefused() {
        assertEquals(
            ListResult.Refused(ListRefusal.Duplicate("Technology")),
            Classification.rename(sample, ClassList.SECTOR, "Diversified", " TECHNOLOGY", listOf(aapl)),
        )
        assertEquals(
            ListResult.Refused(ListRefusal.Remembered("Information Technology", "Technology")),
            Classification.rename(sample, ClassList.SECTOR, "Diversified", "information technology", listOf(aapl)),
        )
    }

    @Test
    fun aRenameOfCaseOrSpacesOnly_isAllowed_andRemembersNothing() {
        val profiles = listOf(SourceProfile("Technology", null))
        val result = Classification.rename(sample, ClassList.SECTOR, "Technology", " TECHNOLOGY ", profiles)
        val after = sample.copy(
            sectors = listOf("TECHNOLOGY", "Diversified"),
            remembered = listOf(RememberedText(ClassList.SECTOR, "Information Technology", "TECHNOLOGY")),
        )
        assertEquals(ListResult.Renamed(after, "Technology", "TECHNOLOGY", null), result)
    }

    @Test
    fun aBlankName_isRefused() {
        for (name in listOf("", "   ", "\t")) {
            assertEquals("[$name]", ListResult.Refused(ListRefusal.Blank), Classification.add(sample, ClassList.SECTOR, name))
            assertEquals(
                "[$name]",
                ListResult.Refused(ListRefusal.Blank),
                Classification.rename(sample, ClassList.SECTOR, "Diversified", name, emptyList()),
            )
        }
    }

    @Test
    fun add_trimsTheName_andAddsItLast_inTheTypedCase() {
        assertEquals(
            ListResult.Added(sample.copy(sectors = listOf("Technology", "Diversified", "energy  Sector")), "energy  Sector"),
            Classification.add(sample, ClassList.SECTOR, "  energy  Sector "),
        )
        assertEquals(
            ListResult.Added(sample.copy(geographies = listOf("United States", "Global", "US")), "US"),
            Classification.add(sample, ClassList.GEOGRAPHY, "US"),
        )
    }

    @Test
    fun rename_trimsTheName_andKeepsTheValuesPlace() {
        val result = Classification.rename(sample, ClassList.SECTOR, "Technology", "  Tech ", emptyList()) as ListResult.Renamed
        assertEquals("Tech", result.to)
        assertEquals(listOf("Tech", "Diversified"), result.lists.sectors)
    }

    @Test
    fun delete_removesTheValuesRememberedTexts_only() {
        val lists = sample.copy(remembered = sample.remembered + RememberedText(ClassList.SECTOR, "Multi", "Diversified"))
        val result = Classification.delete(lists, ClassList.SECTOR, "Diversified", users)
        assertEquals(ListResult.Deleted(sample.copy(sectors = listOf("Technology")), "Diversified", emptyList()), result)
    }

    @Test
    fun anOverride_usesItsValue_andAnOverriddenSourceValueIsStillUsed() {
        // AAPL's override wins on the tab, but its source text still maps to Technology (my reading of "uses", section 4).
        val withOverride = users.copy(overrides = listOf(ClassOverride("p1", "AAPL", ClassList.SECTOR, "Diversified")))
        assertEquals(1, count(sample, ClassList.SECTOR, "Diversified", withOverride))
        assertEquals(1, count(sample, ClassList.SECTOR, "Technology", withOverride))
        assertEquals(
            ListResult.Refused(ListRefusal.InUse("Diversified", 1)),
            Classification.delete(sample, ClassList.SECTOR, "Diversified", withOverride),
        )
    }

    @Test
    fun aHoldingThatUsesAValueTwice_countsOnce() {
        val both = users.copy(overrides = listOf(ClassOverride("p1", "AAPL", ClassList.SECTOR, "Technology")))
        assertEquals(1, count(sample, ClassList.SECTOR, "Technology", both))
    }

    @Test
    fun aTickerInTwoPortfolios_isTwoHoldings_andAnInstrumentNotHeld_isNone() {
        val profiles = mapOf("AAPL" to aapl, "MSFT" to SourceProfile("Technology", "Global"))
        val twice = ListUsers(holdings = listOf("p1" to "AAPL", "p2" to "AAPL"), profiles = profiles)
        assertEquals(2, count(sample, ClassList.SECTOR, "Technology", twice))
        assertEquals(0, count(sample, ClassList.GEOGRAPHY, "Global", twice))
    }

    @Test
    fun aValueOfTheSameNameOnTheOtherList_isNotCounted() {
        val lists = sample.copy(sectors = sample.sectors + "Global")
        val global = ListUsers(
            holdings = listOf("p1" to "AAPL"),
            overrides = listOf(ClassOverride("p1", "AAPL", ClassList.GEOGRAPHY, "Global")),
        )
        assertEquals(1, count(lists, ClassList.GEOGRAPHY, "Global", global))
        assertEquals(0, count(lists, ClassList.SECTOR, "Global", global))
    }

    @Test
    fun decision3_deletingAValue_removesTheOverridesOfHoldingsNoLongerThere() {
        val gone = ClassOverride("p9", "GONE", ClassList.SECTOR, "Diversified")
        val other = ClassOverride("p8", "OLD", ClassList.SECTOR, "Technology")
        val withLeftovers = users.copy(overrides = listOf(gone, other))
        assertEquals(0, count(sample, ClassList.SECTOR, "Diversified", withLeftovers))
        assertEquals(
            ListResult.Deleted(sample.copy(sectors = listOf("Technology")), "Diversified", listOf(gone)),
            Classification.delete(sample, ClassList.SECTOR, "Diversified", withLeftovers),
        )
    }
}

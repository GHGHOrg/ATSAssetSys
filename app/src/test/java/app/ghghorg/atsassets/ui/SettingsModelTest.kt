package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TabOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 20: Settings' model (wireframe 10f frames B and C; CR2). */
class SettingsModelTest {
    @Test
    fun theAutoLockRow_showsTheChoiceInForce() {
        for (choice in AutoLock.entries) {
            assertEquals(choice, Settings.of(choice, refusal = null).autoLock)
        }
    }

    @Test
    fun aChoiceNotYetLoaded_showsEveryLeave_theRuleTheLockKeeps() {
        assertEquals(AutoLock.EVERY_LEAVE, Settings.of(null, refusal = null).autoLock)
    }

    @Test
    fun theFourChoices_inTheOrderOfSection9_onlyEveryLeaveMarkedDefault() {
        val options = Settings.of(AutoLock.IDLE_5_MIN, refusal = null).options
        assertEquals(
            listOf(
                AutoLockOption(AutoLock.EVERY_LEAVE, isDefault = true),
                AutoLockOption(AutoLock.IDLE_1_MIN, isDefault = false),
                AutoLockOption(AutoLock.IDLE_5_MIN, isDefault = false),
                AutoLockOption(AutoLock.PHONE_LOCK_OR_RESTART, isDefault = false),
            ),
            options,
        )
    }

    // 21: the screenshot window's time left (marker 8), in whole seconds rounded up, so 3:00 shows at the start.
    @Test
    fun theScreenshotWindow_offShowsNoTime() {
        assertNull(Settings.of(AutoLock.EVERY_LEAVE, refusal = null).screenshotLeft)
        assertNull(Settings.of(AutoLock.EVERY_LEAVE, refusal = null, screenshotLeftMs = null).screenshotLeft)
        assertNull(Settings.of(AutoLock.EVERY_LEAVE, refusal = null, screenshotLeftMs = 0L).screenshotLeft)
    }

    @Test
    fun theScreenshotWindow_showsMinutesAndSeconds_roundedUp() {
        val cases =
            mapOf(
                180_000L to ScreenshotTimeLeft("3:00", 3, 0),
                179_001L to ScreenshotTimeLeft("3:00", 3, 0),
                179_000L to ScreenshotTimeLeft("2:59", 2, 59),
                161_000L to ScreenshotTimeLeft("2:41", 2, 41),
                160_001L to ScreenshotTimeLeft("2:41", 2, 41),
                60_000L to ScreenshotTimeLeft("1:00", 1, 0),
                59_000L to ScreenshotTimeLeft("0:59", 0, 59),
                1L to ScreenshotTimeLeft("0:01", 0, 1),
            )
        for ((ms, expected) in cases) {
            assertEquals("$ms ms", expected, Settings.of(AutoLock.EVERY_LEAVE, refusal = null, screenshotLeftMs = ms).screenshotLeft)
        }
    }

    @Test
    fun aFailedSave_isShown_andNoRefusalShowsNothing() {
        assertTrue(Settings.of(AutoLock.EVERY_LEAVE, SaveRefusal.SAVE_FAILED).notSaved)
        assertFalse(Settings.of(AutoLock.EVERY_LEAVE, refusal = null).notSaved)
    }

    // 22: the Navigation section's Tab order row shows the order in force (marker 5).
    @Test
    fun theTabOrderRow_showsTheOrderInForce_theDefaultWhenNoneIsGiven() {
        val overallFirst = TabOrder.of(listOf(Tab.OVERALL, Tab.MORE, Tab.CASH, Tab.PORTFOLIOS))!!
        val shown = Settings.of(AutoLock.EVERY_LEAVE, refusal = null, tabOrder = overallFirst).tabOrder
        assertEquals(listOf(Tab.OVERALL, Tab.MORE, Tab.CASH, Tab.PORTFOLIOS), shown)
        assertEquals(listOf(Tab.MORE, Tab.CASH, Tab.PORTFOLIOS, Tab.OVERALL), Settings.of(AutoLock.EVERY_LEAVE, refusal = null).tabOrder)
    }

    // 25: the Lists section (marker 6), Sectors then Geographies, each with its number of values.
    @Test
    fun theListsSection_showsEachListsNumberOfValues() {
        val lists = ClassLists(sectors = listOf("Technology", "Diversified"), geographies = listOf("Global"))
        assertEquals(
            listOf(SettingsListRow(ClassList.SECTOR, 2), SettingsListRow(ClassList.GEOGRAPHY, 1)),
            Settings.of(AutoLock.EVERY_LEAVE, refusal = null, classLists = lists).lists,
        )
        assertEquals(
            listOf(SettingsListRow(ClassList.SECTOR, 0), SettingsListRow(ClassList.GEOGRAPHY, 0)),
            Settings.of(AutoLock.EVERY_LEAVE, refusal = null).lists,
        )
    }

    // 30: the Prices section's row (marker 9), as given; none when the model is built without one.
    @Test
    fun thePricesSection_showsThePriceSourceRowItIsGiven() {
        val row = PriceKeys.row("Finnhub", keySet = true, ending = "a1b2")
        assertEquals(row, Settings.of(AutoLock.EVERY_LEAVE, refusal = null, priceSource = row).priceSource)
        assertNull(Settings.of(AutoLock.EVERY_LEAVE, refusal = null).priceSource)
    }
}

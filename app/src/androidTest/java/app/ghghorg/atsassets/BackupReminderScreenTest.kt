package app.ghghorg.atsassets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BackupOutcome
import app.ghghorg.atsassets.ui.BackupStage
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.OverallScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun reminderText(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

// 2026-10-09 12:30 in New York, daylight time: "2026-10-09 EDT" (C15), and a moment 30 days later.
private val REMINDER_BACKED_UP: Instant = Instant.parse("2026-10-09T16:30:00Z")
private val REMINDER_NOW: Instant = REMINDER_BACKED_UP.plus(Duration.ofDays(30))
private val REMINDER_ZONE: ZoneId = ZoneId.of("America/New_York")
private const val REMINDER_DATE = "2026-10-09"
private const val REMINDER_ZONE_NAME = "EDT"

/** Overall with $10,000.00 cash and no portfolio; [lastBackup] against [REMINDER_NOW] decides whether a backup is due. */
private fun overallState(
    lastBackup: Instant? = null,
    dismissed: Boolean = false,
    stage: BackupStage? = null,
    outcome: BackupOutcome? = null,
): AppState = AppState(
    screen = Screen.TABS,
    tab = Tab.OVERALL,
    entries = listOf(CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)),
    today = LocalDate.of(2026, 11, 8),
    lastBackup = lastBackup,
    now = REMINDER_NOW,
    zone = REMINDER_ZONE,
    reminderDismissed = dismissed,
    backupStage = stage,
    backupOutcome = outcome,
    saving = stage == BackupStage.WRITING,
)

/**
 * Overall's backup reminder for TalkBack (32; spec section 9; AC-18, AC-56, AC-61 (d)): the banner says in words why a
 * backup is due, with the last backup's date and zone when there is one; Back up now and Dismiss are buttons, unavailable
 * while an export is under way; with nothing due, or after Dismiss, there is no banner; an export's line shows under the
 * banner, or alone once the backup is made.
 */
@RunWith(AndroidJUnit4::class)
class BackupReminderTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var backUps = 0
    private var dismissals = 0

    private fun show(state: AppState) {
        composeRule.setContent {
            ATSAssetSystemTheme { OverallScreen(state, {}, {}, onBackUpNow = { backUps++ }, onDismissReminder = { dismissals++ }) }
        }
    }

    @Test
    fun withNoBackupYet_theBannerSaysSo_andBothButtonsTakeTheirTaps() {
        show(overallState())
        composeRule.onNodeWithText(string(R.string.backup_due_never)).assertExists()
        composeRule.onNode(button(R.string.backup_now)).assertIsEnabled().performClick()
        composeRule.onNode(button(R.string.backup_due_dismiss)).assertIsEnabled().performClick()
        assertEquals("Back up now took its tap", 1, backUps)
        assertEquals("Dismiss took its tap", 1, dismissals)
        // Overall itself stays under the banner.
        composeRule.onNode(hasContentDescription(reminderText(R.string.net_worth_spoken, "10,000.00"))).assertExists()
    }

    @Test
    fun withAnOldBackup_theBannerReadsItsDateAndZone() {
        show(overallState(lastBackup = REMINDER_BACKED_UP))
        composeRule.onNodeWithText(reminderText(R.string.backup_due_old, REMINDER_DATE, REMINDER_ZONE_NAME)).assertExists()
        composeRule.onNode(button(R.string.backup_now)).assertIsEnabled()
        composeRule.onNode(button(R.string.backup_due_dismiss)).assertIsEnabled()
    }

    // Exactly 7 days old is not due (C15: more than 7 days of elapsed time).
    @Test
    fun withABackupNotOlderThan7Days_thereIsNoBanner() {
        show(overallState(lastBackup = REMINDER_NOW.minus(Duration.ofDays(7))))
        composeRule.onNode(button(R.string.backup_now)).assertDoesNotExist()
        composeRule.onNode(button(R.string.backup_due_dismiss)).assertDoesNotExist()
        composeRule.onNode(hasContentDescription(reminderText(R.string.net_worth_spoken, "10,000.00"))).assertExists()
    }

    @Test
    fun afterDismiss_thereIsNoBanner() {
        show(overallState(dismissed = true))
        composeRule.onNodeWithText(string(R.string.backup_due_never)).assertDoesNotExist()
        composeRule.onNode(button(R.string.backup_now)).assertDoesNotExist()
        composeRule.onNode(button(R.string.backup_due_dismiss)).assertDoesNotExist()
    }

    // Decision 1: a cancelled or failed export keeps the banner, with its line under it.
    @Test
    fun anExportThatDidNotSave_showsItsLineUnderTheBanner() {
        show(overallState(outcome = BackupOutcome.NOT_SAVED))
        composeRule.onNodeWithText(string(R.string.backup_due_never)).assertExists()
        composeRule.onNodeWithText(string(R.string.backup_not_saved)).assertExists()
        composeRule.onNode(button(R.string.backup_now)).assertIsEnabled()
    }

    // Decision 1: once the backup is made the banner is gone, and the saved line stands in its place.
    @Test
    fun aSavedBackup_showsItsLineAlone_withTheDateAndTheZone() {
        show(overallState(lastBackup = REMINDER_NOW, outcome = BackupOutcome.SAVED))
        composeRule.onNodeWithText(reminderText(R.string.backup_saved, "2026-11-08", "EST")).assertExists()
        composeRule.onNode(button(R.string.backup_now)).assertDoesNotExist()
        composeRule.onNode(button(R.string.backup_due_dismiss)).assertDoesNotExist()
    }

    @Test
    fun whileTheFileIsWritten_bothButtonsAreUnavailable_andTheIndicatorShows() {
        show(overallState(stage = BackupStage.WRITING))
        composeRule.onNode(button(R.string.backup_now)).assertIsNotEnabled()
        composeRule.onNode(button(R.string.backup_due_dismiss)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.backup_writing)).assertExists()
    }
}

/**
 * AC-39 (AC-61 (a)'s care for the largest font in landscape): Overall with the banner of an old backup and the longest
 * result line under it, drawn above the bottom bar as in the app. The banner scrolls with the screen: its text, both
 * buttons and the line can be scrolled to and are shown, each button takes its tap, and net worth under it is reached.
 */
@RunWith(AndroidJUnit4::class)
class BackupReminderLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun everyPart_canBeReached_andBothButtonsTapped() {
        composeRule.turn(landscape = true)
        var backUps = 0
        var dismissals = 0
        val state = overallState(lastBackup = REMINDER_BACKED_UP, outcome = BackupOutcome.NOT_SAVED_FILE_MAY_REMAIN)
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            OverallScreen(state, {}, {}, onBackUpNow = { backUps++ }, onDismissReminder = { dismissals++ })
                        }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.OVERALL, {})
                    }
                }
            }
        }
        composeRule
            .onNodeWithText(reminderText(R.string.backup_due_old, REMINDER_DATE, REMINDER_ZONE_NAME))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNode(button(R.string.backup_now)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("Back up now took its tap", 1, backUps)
        composeRule.onNode(button(R.string.backup_due_dismiss)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("Dismiss took its tap", 1, dismissals)
        composeRule.onNodeWithText(string(R.string.backup_not_saved_file_remains)).performScrollTo().assertIsDisplayed()
        composeRule
            .onNode(hasContentDescription(reminderText(R.string.net_worth_spoken, "10,000.00")))
            .performScrollTo()
            .assertIsDisplayed()
        // Upright again before the next test (18, #150).
        composeRule.turn(landscape = false)
    }
}

/**
 * AC-18 on the real MainActivity, unlocked: the emulator's app holds data and never backed up, so Overall shows the
 * banner under its title; Dismiss hides it with the window's focus kept, and it stays hidden across a tab change. Each
 * test starts MainActivity anew, so the banner is back for the next one. Back up now is not tapped: it would open
 * Android's own file screen, which these tests do not drive.
 */
@RunWith(AndroidJUnit4::class)
class BackupReminderOnOverallTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun theBannerShows_andDismissHidesIt_untilTheAppIsNextOpened() {
        composeRule.unlockApp()
        // A fresh app shows the guided steps and no banner (nothing to back up): set an opening balance first.
        val setOpening = string(R.string.guided_set_opening_balance)
        if (composeRule.onAllNodes(hasText(setOpening)).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNode(button(R.string.backup_due_dismiss)).assertDoesNotExist()
            composeRule.onNodeWithText(setOpening).performClick()
            composeRule.onNode(hasSetTextAction() and hasText(string(R.string.amount_label))).performTextInput("1000")
            composeRule.onNode(button(R.string.review_button)).performScrollTo().performClick()
            composeRule.onNode(button(R.string.confirm_button)).performScrollTo().performClick()
            composeRule.waitUntil(10_000) { composeRule.onAllNodes(button(R.string.confirm_button)).fetchSemanticsNodes().isEmpty() }
        }
        val overallTab = isSelectable() and hasText(string(R.string.overall_title))
        composeRule.onNode(overallTab).performClick()
        val dismiss = button(R.string.backup_due_dismiss)
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(dismiss).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(string(R.string.backup_due_never)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.backup_now)).performScrollTo().assertIsDisplayed().assertIsEnabled()
        composeRule.assertFocusHeld("Overall after Dismiss") {
            composeRule.onNode(dismiss).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(dismiss).fetchSemanticsNodes().isEmpty() }
        }
        composeRule.onNodeWithText(string(R.string.backup_due_never)).assertDoesNotExist()
        composeRule.onNode(button(R.string.backup_now)).assertDoesNotExist()
        // Moving about the app does not bring it back.
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_cash))).performClick()
        composeRule.onNode(overallTab).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(dismiss).assertDoesNotExist()
        composeRule.onNode(hasContentDescription(string(R.string.net_worth_label), substring = true)).assertExists()
    }
}

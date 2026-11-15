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
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.ui.BackupActions
import app.ghghorg.atsassets.ui.BackupModel
import app.ghghorg.atsassets.ui.BackupOutcome
import app.ghghorg.atsassets.ui.BackupScreen
import app.ghghorg.atsassets.ui.BackupStage
import app.ghghorg.atsassets.ui.Backups
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.More
import app.ghghorg.atsassets.ui.MoreScreen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

private fun backupText(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

// 2026-10-09 12:30 in New York, daylight time: "2026-10-09 EDT" (C15: the date and the zone's abbreviation).
private val BACKED_UP: Instant = Instant.parse("2026-10-09T16:30:00Z")
private val NEW_YORK: ZoneId = ZoneId.of("America/New_York")
private const val SHOWN_DATE = "2026-10-09"
private const val SHOWN_ZONE = "EDT"

private fun backupModel(
    lastBackup: Instant? = BACKED_UP,
    interrupted: Boolean = false,
    holdsData: Boolean = true,
    stage: BackupStage? = null,
    outcome: BackupOutcome? = null,
): BackupModel = Backups.screen(lastBackup, NEW_YORK, interrupted, holdsData, stage, outcome, saving = stage == BackupStage.WRITING)

/**
 * More's last backup line and Backup and restore for TalkBack (31; AC-18, AC-58, AC-61 (d)): the line reads the date and
 * the zone; More's fourth entry is a button; the screen's title is a heading; Back up now is a button, unavailable with
 * no data and while an export is under way, each with its line; every result has its line; an interrupted backup says so
 * on both.
 */
@RunWith(AndroidJUnit4::class)
class BackupTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        model: BackupModel,
        onBackUpNow: () -> Unit = {},
    ) {
        composeRule.setContent { ATSAssetSystemTheme { BackupScreen(model, BackupActions(onBack = {}, onBackUpNow = onBackUpNow)) } }
    }

    @Test
    fun moresLine_readsTheDateAndTheZone_andItsFourthEntryOpensBackupAndRestore() {
        var opened = 0
        composeRule.setContent {
            ATSAssetSystemTheme { MoreScreen(More.of(BACKED_UP, NEW_YORK), onOpenNetWorth = {}, onOpenBackup = { opened++ }) }
        }
        composeRule.onNodeWithText(backupText(R.string.last_backup_on, SHOWN_DATE, SHOWN_ZONE)).assertExists()
        // The line is information only (wireframe 10f marker 1): no tap on it.
        composeRule.onNode(hasText(backupText(R.string.last_backup_on, SHOWN_DATE, SHOWN_ZONE)) and hasClickAction()).assertDoesNotExist()
        composeRule.onNode(hasContentDescription(string(R.string.backup_title)) and hasClickAction()).performScrollTo().performClick()
        assertEquals(1, opened)
    }

    @Test
    fun more_withNoBackup_saysNever_andAfterAnInterruptedOne_saysSo() {
        composeRule.setContent { ATSAssetSystemTheme { MoreScreen(More.of(backupInterrupted = true), onOpenNetWorth = {}) } }
        composeRule.onNodeWithText(string(R.string.last_backup_never)).assertExists()
        composeRule.onNodeWithText(string(R.string.backup_interrupted)).assertExists()
    }

    @Test
    fun theScreen_hasItsTitleAsAHeading_theLine_andBackUpNowAsAButton() {
        var asked = 0
        show(backupModel(), onBackUpNow = { asked++ })
        composeRule.onNode(isHeading() and hasText(string(R.string.backup_title))).assertExists()
        composeRule.onNode(hasContentDescription(string(R.string.back_button)) and hasClickAction()).assertIsEnabled()
        composeRule.onNodeWithText(backupText(R.string.last_backup_on, SHOWN_DATE, SHOWN_ZONE)).assertExists()
        composeRule.onNodeWithText(string(R.string.backup_note)).assertExists()
        composeRule.onNode(button(R.string.backup_now)).assertIsEnabled().performClick()
        assertEquals(1, asked)
        composeRule.onNodeWithText(string(R.string.backup_interrupted)).assertDoesNotExist()
    }

    // 31 decision 6: read by TalkBack as unavailable, with one line saying why.
    @Test
    fun withNoData_backUpNowIsUnavailable_andALineSaysWhy() {
        show(backupModel(lastBackup = null, holdsData = false))
        composeRule.onNodeWithText(string(R.string.last_backup_never)).assertExists()
        composeRule.onNode(button(R.string.backup_now)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.backup_no_data)).assertExists()
    }

    @Test
    fun whileTheFileIsWritten_backUpNowAndBackAreUnavailable_andTheIndicatorShows() {
        show(backupModel(stage = BackupStage.WRITING))
        composeRule.onNode(button(R.string.backup_now)).assertIsNotEnabled()
        composeRule.onNode(hasContentDescription(string(R.string.back_button)) and hasClickAction()).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.backup_writing)).assertExists()
    }

    @Test
    fun aSavedBackup_saysSo_withTheDateAndTheZone() {
        show(backupModel(outcome = BackupOutcome.SAVED))
        composeRule.onNodeWithText(backupText(R.string.backup_saved, SHOWN_DATE, SHOWN_ZONE)).assertExists()
        composeRule.onNode(button(R.string.backup_now)).assertIsEnabled()
    }

    @Test
    fun aBackupThatWasNotSaved_saysSo() {
        show(backupModel(outcome = BackupOutcome.NOT_SAVED))
        composeRule.onNodeWithText(string(R.string.backup_not_saved)).assertExists()
    }

    // 31 decision 4.
    @Test
    fun aFailedBackupWhoseFileMayRemain_saysSo() {
        show(backupModel(outcome = BackupOutcome.NOT_SAVED_FILE_MAY_REMAIN))
        composeRule.onNodeWithText(string(R.string.backup_not_saved_file_remains)).assertExists()
    }

    @Test
    fun aWrittenBackupWhoseDateWasNotSaved_saysSo() {
        show(backupModel(outcome = BackupOutcome.SAVED_NOT_RECORDED))
        composeRule.onNodeWithText(string(R.string.backup_saved_not_recorded)).assertExists()
    }

    // 31 decision 7.
    @Test
    fun anInterruptedBackup_saysSoOnTheScreen() {
        show(backupModel(interrupted = true))
        composeRule.onNodeWithText(string(R.string.backup_interrupted)).assertExists()
        composeRule.onNode(button(R.string.backup_now)).assertIsEnabled()
    }
}

/**
 * AC-39 (AC-61 (a)'s care for the largest font in landscape): Backup and restore with its longest lines, the interrupted
 * warning and the "a file may remain" result, drawn above the bottom bar as in the app: every part can be scrolled to and
 * is shown, and Back up now takes its tap.
 */
@RunWith(AndroidJUnit4::class)
class BackupLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun everyPart_canBeReached_andBackUpNowTapped() {
        composeRule.turn(landscape = true)
        var asked = 0
        val model = backupModel(interrupted = true, outcome = BackupOutcome.NOT_SAVED_FILE_MAY_REMAIN)
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            BackupScreen(model, BackupActions(onBack = {}, onBackUpNow = { asked++ }))
                        }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.MORE, {})
                    }
                }
            }
        }
        composeRule.onNode(isHeading() and hasText(string(R.string.backup_title))).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(backupText(R.string.last_backup_on, SHOWN_DATE, SHOWN_ZONE)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.backup_interrupted)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.backup_now)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("Back up now took its tap", 1, asked)
        composeRule.onNodeWithText(string(R.string.backup_note)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.backup_not_saved_file_remains)).performScrollTo().assertIsDisplayed()
        // Upright again before the next test (18, #150).
        composeRule.turn(landscape = false)
    }
}

/**
 * AC-58 on the real MainActivity, unlocked: More's fourth entry opens Backup and restore under the bottom bar, with the
 * window's focus kept; the emulator never backed up, so the line says so; "<" returns to More. Back up now is not tapped:
 * it would open Android's own file screen, which these tests do not drive.
 */
@RunWith(AndroidJUnit4::class)
class BackupOpensFromMoreTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun moresEntry_opensBackupAndRestore_andBackReturnsToMore() {
        composeRule.unlockApp()
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_more))).performClick()
        val entry = hasContentDescription(string(R.string.backup_title)) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(entry).fetchSemanticsNodes().isNotEmpty() }
        val title = isHeading() and hasText(string(R.string.backup_title))
        composeRule.assertFocusHeld("Backup and restore") {
            composeRule.onNode(entry).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(title).fetchSemanticsNodes().isNotEmpty() }
        }
        composeRule.onNodeWithText(string(R.string.last_backup_never)).assertExists()
        composeRule.onNode(button(R.string.backup_now)).assertExists()
        // The bar stays under the screen (spec 6: always visible).
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_more))).assertExists()
        composeRule.onNode(hasContentDescription(string(R.string.back_button)) and hasClickAction()).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(entry).fetchSemanticsNodes().isNotEmpty() }
    }
}

package app.ghghorg.atsassets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.RestoreCounts
import app.ghghorg.atsassets.core.RestoreError
import app.ghghorg.atsassets.core.RestoreReason
import app.ghghorg.atsassets.core.RestoreSummary
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TickerRefusal
import app.ghghorg.atsassets.prices.CheckRefusal
import app.ghghorg.atsassets.ui.BackupActions
import app.ghghorg.atsassets.ui.BackupOutcome
import app.ghghorg.atsassets.ui.BackupScreen
import app.ghghorg.atsassets.ui.BackupStage
import app.ghghorg.atsassets.ui.Backups
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.CheckPhase
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.RestoreActions
import app.ghghorg.atsassets.ui.RestoreModel
import app.ghghorg.atsassets.ui.RestoreScreen
import app.ghghorg.atsassets.ui.RestoreStep
import app.ghghorg.atsassets.ui.RestoreWizard
import app.ghghorg.atsassets.ui.Restores
import app.ghghorg.atsassets.ui.TickerError
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

private fun restoreText(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

private const val SOURCE = "Test source"

// Wireframe 10e frame E's three errors: a quantity, a dependent row, and cash negative on a date by an amount.
private val REPORT_ERRORS =
    listOf(
        RestoreError(14, "quantity", RestoreReason.QuantityNotPositive),
        RestoreError(17, "lot_picks", RestoreReason.DependsOn(14)),
        RestoreError(21, null, RestoreReason.CashNegative(LocalDate.of(2026, 2, 10), 20_000L)),
    )

// One error as the report shows it: "<place>: <reason>".
private fun reportLine(
    place: String,
    reason: String,
): String = restoreText(R.string.restore_report_line, place, reason)

// The three errors as the report shows them.
private val REPORT_LINES =
    listOf(
        reportLine(restoreText(R.string.restore_place_line_column, "14", "quantity"), string(R.string.restore_quantity_not_positive)),
        reportLine(restoreText(R.string.restore_place_line_column, "17", "lot_picks"), restoreText(R.string.restore_depends_on, "14")),
        reportLine(restoreText(R.string.restore_place_line, "21"), restoreText(R.string.restore_cash_negative, "2026-02-10", "$200.00")),
    )

// spec/example-backup.csv's summary (AC-56): portfolios 2, transactions 8, cash entries 4, $9,425.00, 1 holding.
private val EXAMPLE_SUMMARY =
    RestoreSummary(portfolios = 2, transactions = 8, cashEntries = 4, cashBalanceCents = 942_500L, holdingsWithShares = 1)

// 35: what an app that holds data holds now (wireframe 10e marker 13), with a count above 999 for its thousands comma.
private val NOW_COUNTS = RestoreCounts(portfolios = 1, transactions = 1_234, cashEntries = 3)

// The report's count line for the three errors.
private val REPORT_COUNT: String get() = restoreText(R.string.restore_errors_in_lines, "3", "3")

// [holdsData]: section 3's "app holds data"; 33b's frames are drawn as on an app that holds data (five steps).
private fun model(
    wizard: RestoreWizard,
    holdsData: Boolean = true,
): RestoreModel = Restores.screen(wizard, SOURCE, holdsData)

// 35: step 4 on an app that holds data (wireframe 10e frame G), with the word [typed], an export at [stage] or ended with
// [outcome], and the last backup at 2026-10-26 12:00 UTC.
private fun step4Model(
    typed: String,
    stage: BackupStage? = null,
    outcome: BackupOutcome? = null,
): RestoreModel = Restores.screen(
    RestoreWizard(step = RestoreStep.CONFIRM, summary = EXAMPLE_SUMMARY, now = NOW_COUNTS, typed = typed),
    SOURCE,
    holdsData = true,
    stage = stage,
    outcome = outcome,
    lastBackup = Backups.lastBackup(Instant.parse("2026-10-26T12:00:00Z"), ZoneId.of("UTC")),
)

// Step 4's field for the word, found by its label.
private val wordField: SemanticsMatcher get() = hasSetTextAction() and hasText(string(R.string.restore_type_word))

// The report is a lazy list (the Deploy review's finding 2): an item out of view is not drawn until the list scrolls to it.
private fun SemanticsNodeInteractionsProvider.reportItem(matcher: SemanticsMatcher): SemanticsNodeInteraction {
    onNode(hasScrollToNodeAction()).performScrollToNode(matcher)
    return onNode(matcher)
}

/** What the wizard's buttons were asked: each count, and the text Copy report gave. */
private class Asked {
    var back = 0
    var choose = 0
    var check = 0
    var cancel = 0
    var retry = 0
    var another = 0
    var close = 0
    var restore = 0
    var overall = 0
    var next = 0
    var backUps = 0
    var typed: String? = null
    var copied: String? = null

    val actions =
        RestoreActions(
            onBack = { back++ },
            onChooseFile = { choose++ },
            onCheck = { check++ },
            onCancelCheck = { cancel++ },
            onRetry = { retry++ },
            onCopyReport = { copied = it },
            onChooseAnother = { another++ },
            onClose = { close++ },
            onRestore = { restore++ },
            onGoToOverall = { overall++ },
            onContinue = { next++ },
            onType = { typed = it },
            onBackUpNow = { backUps++ },
        )
}

/**
 * 34: the restore onto an app with no data, for TalkBack (section 8, restore step 7; wireframe 10e frame F marker 15 and
 * frame H; AC-56, AC-61 (d)): the summary's step line counts four steps and offers Restore with what it does; on an app
 * that holds data there is no Restore; while the restore is written nothing can be tapped, Back included; the result is a
 * heading, reads each count with its label, its notes, and Go to Overall; a failure says so, with Close.
 */
@RunWith(AndroidJUnit4::class)
class RestoreLoadTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val asked = Asked()

    private fun show(
        wizard: RestoreWizard,
        holdsData: Boolean,
    ) {
        composeRule.setContent { ATSAssetSystemTheme { RestoreScreen(model(wizard, holdsData), SOURCE, asked.actions) } }
    }

    private val backButton = hasContentDescription(string(R.string.back_button)) and hasClickAction()

    @Test
    fun onAnAppWithNoData_theSummaryOffersRestore_inFourSteps() {
        show(RestoreWizard(step = RestoreStep.SUMMARY, summary = EXAMPLE_SUMMARY), holdsData = false)
        composeRule.onNodeWithText(restoreText(R.string.restore_step3, "4")).assertExists()
        composeRule.onNode(hasText(string(R.string.restore_cash_after)) and hasText("$9,425.00")).assertExists()
        composeRule.onNodeWithText(string(R.string.restore_empty_note)).performScrollTo().assertExists()
        composeRule.onNodeWithText(string(R.string.restore_data_note)).assertDoesNotExist()
        composeRule.onNode(button(R.string.restore_continue)).assertDoesNotExist()
        composeRule.onNode(button(R.string.restore_now)).performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, asked.restore)
        composeRule.onNode(button(R.string.cancel_button)).performScrollTo().performClick()
        assertEquals(1, asked.close)
    }

    // 35 (wireframe 10e frame F, marker 13; AC-61 (d)): on an app that holds data the summary reads each count now with
    // the file's, offers Continue with its line, and no Restore (replacing 34 decision 3's line).
    @Test
    fun onAnAppThatHoldsData_theSummaryReadsTheCountsNow_andOffersContinue() {
        show(RestoreWizard(step = RestoreStep.SUMMARY, summary = EXAMPLE_SUMMARY, now = NOW_COUNTS), holdsData = true)
        composeRule.onNodeWithText(restoreText(R.string.restore_step3, "5")).assertExists()
        val rows = listOf(R.string.restore_portfolios to ("1" to "2"), R.string.restore_transactions to ("1,234" to "8"), R.string.restore_cash_entries to ("3" to "4"))
        for ((label, counts) in rows) {
            val spoken = restoreText(R.string.restore_count_row, string(label), counts.first, counts.second)
            composeRule.onNode(hasContentDescription(spoken)).performScrollTo().assertExists()
        }
        composeRule.onNode(hasText(string(R.string.restore_cash_after)) and hasText("$9,425.00")).performScrollTo().assertExists()
        composeRule.onNodeWithText(string(R.string.restore_data_note)).performScrollTo().assertExists()
        composeRule.onNode(button(R.string.restore_now)).assertDoesNotExist()
        composeRule.onNode(button(R.string.restore_continue)).performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, asked.next)
    }

    @Test
    fun whileTheRestoreIsWritten_nothingCanBeTapped() {
        show(RestoreWizard(step = RestoreStep.RESTORING, summary = EXAMPLE_SUMMARY), holdsData = false)
        composeRule.onNodeWithText(restoreText(R.string.restore_restoring, "4")).assertExists()
        composeRule.onNode(backButton).assertIsNotEnabled()
        composeRule.onNode(button(R.string.restore_now)).assertDoesNotExist()
        composeRule.onNode(button(R.string.cancel_button)).assertDoesNotExist()
    }

    // Frame H, marker 18: the app now holds the file's data; no way back into the wizard.
    @Test
    fun theResult_readsWhatWasLoaded_andGoesToOverall() {
        show(RestoreWizard(step = RestoreStep.RESULT, summary = EXAMPLE_SUMMARY), holdsData = true)
        composeRule.onNode(isHeading() and hasText(string(R.string.restore_complete))).assertExists()
        val rows = listOf(R.string.restore_portfolios to "2", R.string.restore_transactions to "8", R.string.restore_cash_entries to "4")
        for ((label, figure) in rows) composeRule.onNode(hasText(string(label)) and hasText(figure)).assertExists()
        composeRule.onNodeWithText(string(R.string.restore_no_price_note)).performScrollTo().assertExists()
        composeRule.onNodeWithText(string(R.string.restore_never_backed_up_note)).performScrollTo().assertExists()
        composeRule.onNodeWithText(string(R.string.restore_key_note)).performScrollTo().assertExists()
        composeRule.onNode(backButton).assertIsNotEnabled()
        composeRule.onNode(button(R.string.restore_go_overall)).performScrollTo().performClick()
        assertEquals(1, asked.overall)
    }

    // Frame H, marker 19.
    @Test
    fun aFailure_saysNothingWasChanged_withClose() {
        show(RestoreWizard(step = RestoreStep.FAILED, summary = EXAMPLE_SUMMARY), holdsData = false)
        composeRule.onNodeWithText(string(R.string.restore_failed)).assertExists()
        composeRule.onNode(button(R.string.restore_go_overall)).assertDoesNotExist()
        composeRule.onNode(button(R.string.restore_close)).performClick()
        assertEquals(1, asked.close)
    }
}

/**
 * 35: step 4 for TalkBack (wireframe 10e frame G; section 8, restore step 6; AC-17, AC-56, AC-61 (d); decision 2): its
 * step line; the export offer as a heading with its note and Back up now; what Replace all data does; the field named
 * by its label, whose text reaches the wizard; Replace all data unavailable until the word is typed; while an export is
 * written, its line, and nothing to tap but the wait; its result line once it ends; and five steps to a restore's end.
 */
@RunWith(AndroidJUnit4::class)
class RestoreOverDataTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val asked = Asked()

    private fun show(model: RestoreModel) {
        composeRule.setContent { ATSAssetSystemTheme { RestoreScreen(model, SOURCE, asked.actions) } }
    }

    @Test
    fun step4_readsTheExportOffer_andReplaceWaitsForTheWord() {
        show(step4Model(""))
        composeRule.onNodeWithText(restoreText(R.string.restore_step4, "5")).assertExists()
        composeRule.onNode(isHeading() and hasText(string(R.string.restore_backup_first))).assertExists()
        composeRule.onNodeWithText(string(R.string.restore_backup_first_note)).assertExists()
        composeRule.onNode(button(R.string.backup_now)).performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, asked.backUps)
        composeRule.onNodeWithText(string(R.string.restore_replace_note)).performScrollTo().assertExists()
        composeRule.onNode(wordField).performScrollTo().assertExists()
        composeRule.onNode(button(R.string.restore_replace)).performScrollTo().assertIsNotEnabled().performClick()
        assertEquals(0, asked.restore)
        composeRule.onNode(button(R.string.cancel_button)).performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, asked.close)
    }

    @Test
    fun step4_typing_reachesTheWizard() {
        show(step4Model(""))
        composeRule.onNode(wordField).performScrollTo().performTextInput("RESTORE")
        assertEquals("RESTORE", asked.typed)
    }

    // Decision 2: the word in any case, with spaces.
    @Test
    fun step4_withTheWordTyped_replacesAllData() {
        show(step4Model(" Re store"))
        composeRule.onNode(wordField and hasText(" Re store")).performScrollTo().assertExists()
        composeRule.onNode(button(R.string.restore_replace)).performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, asked.restore)
    }

    // Wireframe 10e marker 16; 31's indicator: while the file is written nothing can be tapped, Back and Cancel included.
    @Test
    fun step4_whileTheExportIsWritten_nothingCanBeTapped() {
        show(step4Model("RESTORE", stage = BackupStage.WRITING))
        composeRule.onNodeWithText(string(R.string.backup_writing)).performScrollTo().assertExists()
        composeRule.onNode(button(R.string.backup_now)).performScrollTo().assertIsNotEnabled()
        composeRule.onNode(button(R.string.restore_replace)).performScrollTo().assertIsNotEnabled()
        composeRule.onNode(button(R.string.cancel_button)).performScrollTo().assertIsNotEnabled()
        composeRule.onNode(hasContentDescription(string(R.string.back_button)) and hasClickAction()).assertIsNotEnabled()
    }

    // Wireframe 10e marker 16: a completed export shows "Backup saved" on step 4, with the new last backup.
    @Test
    fun step4_afterTheExport_saysBackupSaved() {
        show(step4Model("", outcome = BackupOutcome.SAVED))
        composeRule.onNodeWithText(restoreText(R.string.backup_saved, "2026-10-26", "UTC")).performScrollTo().assertExists()
        composeRule.onNode(button(R.string.backup_now)).performScrollTo().assertIsEnabled()
    }

    // A restore over data had five steps: its "Restoring…" line counts five.
    @Test
    fun restoringOverData_countsFiveSteps() {
        show(Restores.screen(RestoreWizard(step = RestoreStep.RESTORING, summary = EXAMPLE_SUMMARY, now = NOW_COUNTS), SOURCE, holdsData = true))
        composeRule.onNodeWithText(restoreText(R.string.restore_restoring, "5")).assertExists()
    }
}

/**
 * The restore wizard for TalkBack (33b; section 8; wireframe 10e frames C to F; AC-16, AC-56, AC-61 (d); C16): the title
 * is a heading; step 1's buttons; step 2's phases, a problem of the source with Try again; the report's count line as a
 * heading and each error read with its line, column and figures, and Copy report gives exactly that text; the second
 * round names each ticker and the source; the summary reads each label with its figure; Restore on Backup and restore is
 * unavailable offline with its line.
 */
@RunWith(AndroidJUnit4::class)
class RestoreTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val asked = Asked()

    private fun show(wizard: RestoreWizard) {
        composeRule.setContent { ATSAssetSystemTheme { RestoreScreen(model(wizard), SOURCE, asked.actions) } }
    }

    @Test
    fun step1_choosesAFile_andChecksItOnceChosen() {
        show(RestoreWizard())
        composeRule.onNode(isHeading() and hasText(string(R.string.restore_title))).assertExists()
        composeRule.onNodeWithText(restoreText(R.string.restore_step1, "5")).assertExists()
        composeRule.onNodeWithText(string(R.string.restore_no_file)).assertExists()
        composeRule.onNode(button(R.string.restore_check_file)).assertIsNotEnabled()
        composeRule.onNode(button(R.string.restore_choose_file)).assertIsEnabled().performClick()
        assertEquals(1, asked.choose)
        composeRule.onNodeWithText(string(R.string.restore_date_help)).assertExists()
        composeRule.onNodeWithText(string(R.string.date_note)).assertExists()
    }

    @Test
    fun step1_withAFileChosen_showsItsName_andCheckFileTakesItsTap() {
        show(RestoreWizard(fileName = "backup-2026-10-04.csv"))
        composeRule.onNodeWithText(restoreText(R.string.restore_file_name, "backup-2026-10-04.csv")).assertExists()
        composeRule.onNode(button(R.string.restore_check_file)).assertIsEnabled().performClick()
        assertEquals(1, asked.check)
        composeRule.onNode(hasContentDescription(string(R.string.back_button)) and hasClickAction()).performClick()
        assertEquals(1, asked.back)
    }

    // Wireframe 10e frame D: the first phase done, the second counting; Cancel.
    @Test
    fun step2_readsItsPhases_andCancelTakesItsTap() {
        show(RestoreWizard(step = RestoreStep.CHECKING, fileName = "b.csv", phase = CheckPhase.VERIFYING, verified = 1, toVerify = 3))
        composeRule.onNodeWithText(restoreText(R.string.restore_step2, "5")).assertExists()
        composeRule.onNodeWithText(string(R.string.restore_reading_done)).assertExists()
        composeRule.onNodeWithText(restoreText(R.string.restore_verify_progress, "2", "3")).assertExists()
        composeRule.onNodeWithText(restoreText(R.string.restore_source_line, SOURCE)).assertExists()
        composeRule.onNode(button(R.string.restore_try_again)).assertDoesNotExist()
        composeRule.onNode(button(R.string.cancel_button)).performClick()
        assertEquals(1, asked.cancel)
    }

    // Decision 3: a problem of the source stops the round, says so, and offers Try again.
    @Test
    fun step2_withASourceProblem_saysSo_andOffersTryAgain() {
        val problem = CheckRefusal.TooManyCalls
        show(RestoreWizard(step = RestoreStep.CHECKING, phase = CheckPhase.VERIFYING, toVerify = 2, sourceProblem = problem))
        composeRule.onNodeWithText(string(R.string.restore_source_too_many_calls)).assertExists()
        composeRule.onNode(button(R.string.restore_try_again)).performClick()
        assertEquals(1, asked.retry)
    }

    // Section 8; AC-16; C16: the count line is a heading, each error is read with its line, column and figures, and Copy
    // report copies exactly the shown text, nothing more.
    @Test
    fun theReport_readsEachError_andCopyReportCopiesExactlyTheShownText() {
        show(RestoreWizard(step = RestoreStep.REPORT, fileName = "b.csv", errors = REPORT_ERRORS))
        val countLine = REPORT_COUNT
        composeRule.onNode(isHeading() and hasText(countLine)).assertExists()
        for (line in REPORT_LINES) composeRule.reportItem(hasText(line)).assertExists()
        composeRule.reportItem(hasText(string(R.string.restore_nothing_changed))).assertExists()
        composeRule.reportItem(button(R.string.restore_copy_report)).performClick()
        assertEquals((listOf(countLine) + REPORT_LINES).joinToString("\n"), asked.copied)
        composeRule.reportItem(button(R.string.restore_choose_another)).performClick()
        assertEquals(1, asked.another)
    }

    // The Deploy review's finding 2: a report of 2,000 errors is a lazy list; its last error is reached, and Copy report
    // still copies every error, not only those drawn.
    @Test
    fun aReportOf2000Errors_reachesItsLastError_andCopiesThemAll() {
        val errors = (2..2001).map { RestoreError(it, "date", RestoreReason.DateNotReal) }
        show(RestoreWizard(step = RestoreStep.REPORT, errors = errors))
        composeRule.onNode(isHeading() and hasText(restoreText(R.string.restore_errors_in_lines, "2,000", "2,000"))).assertExists()
        val last = reportLine(restoreText(R.string.restore_place_line_column, "2,001", "date"), string(R.string.restore_date_not_real))
        composeRule.reportItem(hasText(last)).assertIsDisplayed()
        composeRule.reportItem(button(R.string.restore_copy_report)).performClick()
        assertEquals(2_001, asked.copied?.lines()?.size)
        assertEquals(last, asked.copied?.lines()?.last())
    }

    // Section 8, restore step 3; wireframe 10e marker 12: the second round names the ticker and the source.
    @Test
    fun theSecondRound_namesTheTicker_andTheSource() {
        show(RestoreWizard(step = RestoreStep.REPORT, tickerErrors = listOf(TickerError("XYZ", TickerRefusal.NotFound, line = 9))))
        composeRule.onNode(isHeading() and hasText(string(R.string.restore_ticker_error))).assertExists()
        val place = restoreText(R.string.restore_place_line_ticker, "9", "XYZ")
        val line = reportLine(place, restoreText(R.string.restore_ticker_not_found, SOURCE))
        composeRule.onNodeWithText(line).assertExists()
    }

    // Section 8, restore step 5; AC-56's figures; decision 1: each label is read with its figure, and Cancel closes.
    @Test
    fun theSummary_readsEachLabelWithItsFigure() {
        show(RestoreWizard(step = RestoreStep.SUMMARY, summary = EXAMPLE_SUMMARY))
        composeRule.onNodeWithText(restoreText(R.string.restore_step3, "5")).assertExists()
        val rows =
            listOf(
                R.string.restore_portfolios to "2",
                R.string.restore_transactions to "8",
                R.string.restore_cash_entries to "4",
                R.string.restore_cash_after to "$9,425.00",
                R.string.restore_holdings to "1",
            )
        for ((label, figure) in rows) composeRule.onNode(hasText(string(label)) and hasText(figure)).assertExists()
        composeRule.onNode(button(R.string.cancel_button)).performScrollTo().performClick()
        assertEquals(1, asked.close)
    }

    // Wireframe 10e frame A, marker 4; AC-08: offline, Restore is unavailable and a line says why.
    @Test
    fun backupAndRestore_offline_hasRestoreUnavailable_withItsLine() {
        var restores = 0
        val offline = Backups.screen(null, ZoneId.of("UTC"), false, true, null, null, saving = false, online = false)
        val actions = BackupActions(onBack = {}, onBackUpNow = {}, onRestore = { restores++ })
        composeRule.setContent { ATSAssetSystemTheme { BackupScreen(offline, actions) } }
        composeRule.onNodeWithText(string(R.string.restore_note)).performScrollTo().assertExists()
        composeRule.onNodeWithText(string(R.string.restore_offline)).performScrollTo().assertExists()
        composeRule.onNode(button(R.string.restore_button)).performScrollTo().assertIsNotEnabled()
        assertEquals(0, restores)
    }

    @Test
    fun backupAndRestore_online_hasRestoreAvailable() {
        var restores = 0
        val online = Backups.screen(null, ZoneId.of("UTC"), false, false, null, null, saving = false, online = true)
        val actions = BackupActions(onBack = {}, onBackUpNow = {}, onRestore = { restores++ })
        composeRule.setContent { ATSAssetSystemTheme { BackupScreen(online, actions) } }
        composeRule.onNodeWithText(string(R.string.restore_offline)).assertDoesNotExist()
        composeRule.onNode(button(R.string.restore_button)).performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, restores)
    }
}

/**
 * AC-39 (AC-61 (a)'s care for the largest font in landscape): the wizard's screens drawn above the bottom bar, as in the
 * app. Nothing stays in place: every line and button can be scrolled to, is shown, and the buttons take their taps.
 */
@RunWith(AndroidJUnit4::class)
class RestoreLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    private val asked = Asked()

    private fun show(
        wizard: RestoreWizard,
        holdsData: Boolean = true,
    ) = show(model(wizard, holdsData))

    private fun show(model: RestoreModel) {
        composeRule.turn(landscape = true)
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) { RestoreScreen(model, SOURCE, asked.actions) }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.MORE, {})
                    }
                }
            }
        }
    }

    // 35, frame G: step 4's line, the export offer, the field, Replace all data and Cancel are reached and take their taps.
    @Test
    fun step4_everyPart_canBeReached() {
        show(step4Model("RESTORE"))
        composeRule.onNodeWithText(restoreText(R.string.restore_step4, "5")).performScrollTo().assertIsDisplayed()
        composeRule.onNode(isHeading() and hasText(string(R.string.restore_backup_first))).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.backup_now)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText(string(R.string.restore_replace_note)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(wordField).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.restore_replace)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNode(button(R.string.cancel_button)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(1, asked.backUps)
        assertEquals(1, asked.restore)
        assertEquals(1, asked.close)
        composeRule.turn(landscape = false)
    }

    @Test
    fun step1_everyPart_canBeReached() {
        show(RestoreWizard(fileName = "backup-2026-10-04.csv"))
        composeRule.onNode(isHeading() and hasText(string(R.string.restore_title))).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.restore_choose_file)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNode(button(R.string.restore_check_file)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText(string(R.string.date_note)).performScrollTo().assertIsDisplayed()
        composeRule.waitForIdle()
        assertEquals(1, asked.choose)
        assertEquals(1, asked.check)
        composeRule.turn(landscape = false)
    }

    @Test
    fun theReport_everyError_andBothButtons_canBeReached() {
        show(RestoreWizard(step = RestoreStep.REPORT, errors = REPORT_ERRORS))
        composeRule.reportItem(isHeading() and hasText(REPORT_COUNT)).assertIsDisplayed()
        for (line in REPORT_LINES) composeRule.reportItem(hasText(line)).assertIsDisplayed()
        composeRule.reportItem(button(R.string.restore_copy_report)).assertIsDisplayed().performClick()
        composeRule.reportItem(button(R.string.restore_choose_another)).assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(1, asked.another)
        assertEquals((listOf(REPORT_COUNT) + REPORT_LINES).joinToString("\n"), asked.copied)
        composeRule.turn(landscape = false)
    }

    // 35: on an app that holds data, with the Now column, its line and Continue.
    @Test
    fun theSummary_everyFigure_andCancel_canBeReached() {
        show(RestoreWizard(step = RestoreStep.SUMMARY, summary = EXAMPLE_SUMMARY, now = NOW_COUNTS))
        composeRule.onNodeWithText(string(R.string.restore_now_column)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.restore_cash_after)) and hasText("$9,425.00")).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.restore_holdings)) and hasText("1")).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.restore_data_note)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.restore_continue)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.cancel_button)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(1, asked.close)
        composeRule.turn(landscape = false)
    }

    // 34: the summary on an app with no data: its note and Restore are reached and Restore takes its tap.
    @Test
    fun theSummaryWithRestore_everyPart_canBeReached() {
        show(RestoreWizard(step = RestoreStep.SUMMARY, summary = EXAMPLE_SUMMARY), holdsData = false)
        composeRule.onNodeWithText(restoreText(R.string.restore_step3, "4")).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.restore_empty_note)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.restore_now)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(1, asked.restore)
        composeRule.turn(landscape = false)
    }

    // 34, frame H: the result's heading, counts, notes and Go to Overall are reached and the button takes its tap.
    @Test
    fun theResult_everyPart_canBeReached() {
        show(RestoreWizard(step = RestoreStep.RESULT, summary = EXAMPLE_SUMMARY))
        composeRule.onNode(isHeading() and hasText(string(R.string.restore_complete))).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.restore_cash_entries)) and hasText("4")).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.restore_never_backed_up_note)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.restore_key_note)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.restore_go_overall)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(1, asked.overall)
        composeRule.turn(landscape = false)
    }
}

/**
 * 35, AC-61 (c): step 4's field at the largest font in landscape, the screen drawn above the bottom bar as in the app: with
 * the keyboard open, the field lies fully above it, the screen's room ends at the keyboard's top (phone check 12,
 * decision 4), and once the word is typed Replace all data can be scrolled to above it too. Drawn alone, not on MainActivity: reaching step 4 there needs Android's file screen,
 * which these tests do not drive. The phone check covers the phone's higher keyboard (CLAUDE.md pitfalls).
 */
@RunWith(AndroidJUnit4::class)
class RestoreKeyboardTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun step4_theWordsField_andReplace_stayAboveTheKeyboard() {
        composeRule.turn(landscape = true)
        composeRule.setContent {
            var typed by remember { mutableStateOf("") }
            val actions = RestoreActions({}, {}, {}, {}, {}, {}, {}, {}, onType = { typed = it })
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) { RestoreScreen(step4Model(typed), SOURCE, actions) }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.MORE, {})
                    }
                }
            }
        }
        composeRule.onNode(wordField).performScrollTo().performClick()
        val keyboardTop = composeRule.keyboardTop()
        composeRule.waitForIdle()
        val field = composeRule.onNode(wordField).fetchSemanticsNode().boundsInWindow.bottom
        assertTrue("The word's field ends at $field px, below the keyboard's top at $keyboardTop px", field <= keyboardTop)
        // Phone check 12 (decision 4): the screen's room ends at the keyboard's top, give or take 4 dp, not the bar's
        // height above it. On the emulator the field fits either way; on the phone, with its higher keyboard, it did not.
        val room = composeRule.onNode(hasScrollAction() and hasAnyDescendant(wordField)).fetchSemanticsNode().boundsInWindow.bottom
        val slack = with(composeRule.density) { 4.dp.toPx() }
        assertTrue(
            "The screen's room ends at $room px, ${keyboardTop - room} px above the keyboard's top at $keyboardTop px",
            abs(keyboardTop - room) <= slack,
        )
        composeRule.onNode(wordField).performTextInput("restore")
        val replace = button(R.string.restore_replace)
        composeRule.onNode(replace).performScrollTo().assertIsEnabled()
        val top = composeRule.keyboardTop()
        composeRule.waitForIdle()
        val bottom = composeRule.onNode(replace).fetchSemanticsNode().boundsInWindow.bottom
        assertTrue("Replace all data ends at $bottom px, below the keyboard's top at $top px", bottom <= top)
        composeRule.turn(landscape = false)
    }
}

/**
 * AC-56 and AC-08 on the real MainActivity, unlocked: Backup and restore's Restore part; online, Restore from backup
 * file opens step 1 with the window's focus kept (no layer, no separate window), Check file waits for a file, and "<"
 * returns to Backup and restore. Offline (the emulator's network is not the test's to choose), Restore is unavailable
 * with its line. Choose file is not tapped: it would open Android's own file screen, which these tests do not drive.
 */
@RunWith(AndroidJUnit4::class)
class RestoreOpensFromBackupTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun restore_opensStep1_andBackReturns() {
        composeRule.unlockApp()
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_more))).performClick()
        val entry = hasContentDescription(string(R.string.backup_title)) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(entry).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(entry).performScrollTo().performClick()
        val restore = button(R.string.restore_button)
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(restore).fetchSemanticsNodes().isNotEmpty() }
        if (composeRule.onAllNodes(hasText(string(R.string.restore_offline))).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNode(restore).performScrollTo().assertIsNotEnabled()
            return
        }
        // 34: four steps on an app with no data, five on one that holds data; the emulator's app may be either.
        val step1 = hasText(restoreText(R.string.restore_step1, "4")) or hasText(restoreText(R.string.restore_step1, "5"))
        composeRule.assertFocusHeld("Restore, step 1") {
            composeRule.onNode(restore).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(step1).fetchSemanticsNodes().isNotEmpty() }
        }
        composeRule.onNode(isHeading() and hasText(string(R.string.restore_title))).assertExists()
        composeRule.onNode(button(R.string.restore_check_file)).assertIsNotEnabled()
        composeRule.onNode(button(R.string.restore_choose_file)).assertIsEnabled()
        // The bar stays under the wizard (spec 6: always visible).
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_more))).assertExists()
        composeRule.onNode(hasContentDescription(string(R.string.back_button)) and hasClickAction()).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(restore).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(step1).assertDoesNotExist()
    }
}

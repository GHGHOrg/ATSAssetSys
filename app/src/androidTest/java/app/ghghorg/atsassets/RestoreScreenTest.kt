package app.ghghorg.atsassets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.RestoreError
import app.ghghorg.atsassets.core.RestoreReason
import app.ghghorg.atsassets.core.RestoreSummary
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TickerRefusal
import app.ghghorg.atsassets.prices.CheckRefusal
import app.ghghorg.atsassets.ui.BackupActions
import app.ghghorg.atsassets.ui.BackupScreen
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

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

// The report's count line for the three errors.
private val REPORT_COUNT: String get() = restoreText(R.string.restore_errors_in_lines, "3", "3")

private fun model(wizard: RestoreWizard): RestoreModel = Restores.screen(wizard, SOURCE)

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
        )
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
        composeRule.onNodeWithText(string(R.string.restore_step1)).assertExists()
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
        composeRule.onNodeWithText(string(R.string.restore_step2)).assertExists()
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
        show(RestoreWizard(step = RestoreStep.REPORT, tickerErrors = listOf(TickerError("XYZ", TickerRefusal.NotFound))))
        composeRule.onNode(isHeading() and hasText(string(R.string.restore_ticker_error))).assertExists()
        val line = reportLine(restoreText(R.string.restore_place_ticker, "XYZ"), restoreText(R.string.restore_ticker_not_found, SOURCE))
        composeRule.onNodeWithText(line).assertExists()
    }

    // Section 8, restore step 5; AC-56's figures; decision 1: each label is read with its figure, and Cancel closes.
    @Test
    fun theSummary_readsEachLabelWithItsFigure() {
        show(RestoreWizard(step = RestoreStep.SUMMARY, summary = EXAMPLE_SUMMARY))
        composeRule.onNodeWithText(string(R.string.restore_step3)).assertExists()
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

    private fun show(wizard: RestoreWizard) {
        composeRule.turn(landscape = true)
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) { RestoreScreen(model(wizard), SOURCE, asked.actions) }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.MORE, {})
                    }
                }
            }
        }
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

    @Test
    fun theSummary_everyFigure_andCancel_canBeReached() {
        show(RestoreWizard(step = RestoreStep.SUMMARY, summary = EXAMPLE_SUMMARY))
        composeRule.onNode(hasText(string(R.string.restore_cash_after)) and hasText("$9,425.00")).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.restore_holdings)) and hasText("1")).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.restore_summary_note)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.cancel_button)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(1, asked.close)
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
        val step1 = hasText(string(R.string.restore_step1))
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

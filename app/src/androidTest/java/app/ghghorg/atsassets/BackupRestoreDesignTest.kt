package app.ghghorg.atsassets

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.core.RestoreError
import app.ghghorg.atsassets.core.RestoreReason
import app.ghghorg.atsassets.core.RestoreSummary
import app.ghghorg.atsassets.ui.BackupActions
import app.ghghorg.atsassets.ui.BackupOutcome
import app.ghghorg.atsassets.ui.BackupScreen
import app.ghghorg.atsassets.ui.Backups
import app.ghghorg.atsassets.ui.CheckPhase
import app.ghghorg.atsassets.ui.RestoreActions
import app.ghghorg.atsassets.ui.RestoreScreen
import app.ghghorg.atsassets.ui.RestoreStep
import app.ghghorg.atsassets.ui.RestoreWizard
import app.ghghorg.atsassets.ui.Restores
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.Asap
import app.ghghorg.atsassets.ui.theme.TokenColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

private val noRestoreActions = RestoreActions({}, {}, {}, {}, {}, {}, {}, {})

/**
 * Backup and restore and the restore wizard's look (44a step 8, group (e) part 2; spec 6, Visual design; AC-61 (e);
 * drawing 10e-backup-restore, D-31, D-33 to D-35): the title in the band in Asap 22, Back up now gold, Restore an
 * outline button, the catalog's disabled one offline; an export's result as the notice banner with info, not bronze;
 * checking's phases with check and loader-circle; the report's count in the blocked card in medium bronze; the result
 * with circle-check and Go to Overall gold.
 */
@RunWith(AndroidJUnit4::class)
class BackupRestoreDesignTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun icons(): List<String> = composeRule.drawnIcons().map { it.first }

    private fun colorIn(
        id: Int,
        inset: Int,
    ) = composeRule.onNode(hasText(string(id)) and hasClickAction()).let { node ->
        node.performScrollTo()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        composeRule.onRoot().colorAt(bounds.left + with(composeRule.density) { inset.dp.toPx() }, bounds.center.y)
    }

    @Test
    fun backup_hasItsBand_aGoldBackUpNow_andRestoreAsAnOutlineButton_disabledOffline() {
        val offline = Backups.screen(null, ZoneId.of("UTC"), false, true, null, null, saving = false, online = false)
        composeRule.setContent { ATSAssetSystemTheme { BackupScreen(offline, BackupActions({}, {})) } }
        composeRule.waitForIdle()
        val title = composeRule.onNode(isHeading() and hasText(string(R.string.backup_title)), useUnmergedTree = true).laidOutStyle()
        assertEquals(Asap, title.fontFamily)
        assertEquals(22.sp, title.fontSize)
        val backUp = colorIn(R.string.backup_now, 12)
        assertTrue("Back up now ${describe(backUp)}, expected ${describe(TokenColors.light.gold)}", sameColor(backUp, TokenColors.light.gold))
        composeRule.onNode(hasText(string(R.string.restore_button)) and hasClickAction()).assertIsNotEnabled()
        composeRule.onNode(hasText(string(R.string.restore_offline)), useUnmergedTree = true).assertExists()
    }

    @Test
    fun anExportsResult_isTheNoticeBanner_withInfo() {
        val saved = Backups.screen(Instant.parse("2026-10-09T03:00:00Z"), ZoneId.of("UTC"), false, true, null, BackupOutcome.SAVED, saving = false)
        composeRule.setContent { ATSAssetSystemTheme { BackupScreen(saved, BackupActions({}, {})) } }
        composeRule.waitForIdle()
        assertTrue("The banner's info: ${icons()}", "info" in icons())
    }

    @Test
    fun checking_showsADonePhase_andARunningOne() {
        val wizard = RestoreWizard(step = RestoreStep.CHECKING, fileName = "b.csv", phase = CheckPhase.VERIFYING, verified = 1, toVerify = 3)
        composeRule.setContent { ATSAssetSystemTheme { RestoreScreen(Restores.screen(wizard, "Test source", true), "Test source", noRestoreActions) } }
        composeRule.waitForIdle()
        assertTrue("Back, the done phase's check and the running one's loader-circle: ${icons()}", icons().containsAll(listOf("chevron-left", "check", "loader-circle")))
    }

    @Test
    fun theReportsCount_isInTheBlockedCard_inMediumBronze() {
        val errors = listOf(RestoreError(14, "quantity", RestoreReason.QuantityNotPositive))
        val wizard = RestoreWizard(step = RestoreStep.REPORT, fileName = "b.csv", errors = errors)
        composeRule.setContent { ATSAssetSystemTheme { RestoreScreen(Restores.screen(wizard, "Test source", true), "Test source", noRestoreActions) } }
        composeRule.waitForIdle()
        assertTrue("The blocked card's circle-alert: ${icons()}", "circle-exclamation-point" in icons())
        val count = composeRule.onNode(isHeading() and hasText(string(R.string.restore_error_in_line)), useUnmergedTree = true).laidOutStyle()
        assertEquals(FontWeight.W500, count.fontWeight)
        assertTrue("The count ${describe(count.color)}, expected ${describe(TokenColors.light.error)}", sameColor(count.color, TokenColors.light.error))
    }

    @Test
    fun theResult_hasCircleCheck_andAGoldGoToOverall() {
        val summary = RestoreSummary(portfolios = 2, transactions = 8, cashEntries = 4, cashBalanceCents = 942_500L, holdingsWithShares = 1)
        val wizard = RestoreWizard(step = RestoreStep.RESULT, summary = summary)
        composeRule.setContent { ATSAssetSystemTheme { RestoreScreen(Restores.screen(wizard, "Test source", true), "Test source", noRestoreActions) } }
        composeRule.waitForIdle()
        assertTrue("The heading's circle-check: ${icons()}", "circle-check" in icons())
        val go = colorIn(R.string.restore_go_overall, 12)
        assertTrue("Go to Overall ${describe(go)}, expected ${describe(TokenColors.light.gold)}", sameColor(go, TokenColors.light.gold))
    }
}

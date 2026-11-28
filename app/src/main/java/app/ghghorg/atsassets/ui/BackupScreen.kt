package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsNoticeBanner
import app.ghghorg.atsassets.ui.design.AtsProgressRow
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/**
 * The Backup and restore screen's callbacks, passed together. [onBackUpNow] is MainActivity's: it opens Android's file
 * screen. [onRestore] opens the restore wizard (33b).
 */
class BackupActions(
    val onBack: () -> Unit,
    val onBackUpNow: () -> Unit,
    val onRestore: () -> Unit = {},
)

/**
 * Backup and restore (section 6, Backup export and restore entry; wireframe 10e frames A and B; 31), drawn from its model
 * (CR2), in the design system (44a step 8 group (e); drawing spec/wireframes/design/10e-backup-restore.html frames A and
 * B): under the title band, how the last export ended as D-33's notice banner (a cancelled export is not an error, so
 * not bronze; note 4); then two cards, each with its action. The backup card: the last backup line as on More, the line
 * of an interrupted backup, what Back up now does, Back up now gold (the screen's main action), and the line while the
 * file is written or why the button is inactive. The restore card (33b): what a restore does, and Restore from backup
 * file as an outline button, the catalog's disabled one offline with the line why (marker 4; AC-08; note 3, no dashed
 * outline). No layer and no text field: everything is on the screen, which scrolls. Back returns to More.
 */
@Composable
fun BackupScreen(
    model: BackupModel,
    actions: BackupActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = model.backEnabled, onBack = actions.onBack)
    val colors = AtsTheme.colors
    AtsScreen(
        title = stringResource(R.string.backup_title),
        modifier = modifier,
        onBack = actions.onBack,
        backLabel = stringResource(R.string.back_button),
        backEnabled = model.backEnabled,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
        ) {
            if (model.outcome != null) BackupStatusLine(writing = false, noData = false, model.outcome, model.lastBackup, banner = true)
            AtsCard {
                LastBackupLine(model.lastBackup)
                if (model.interrupted) BackupInterruptedLine()
                Text(text = stringResource(R.string.backup_note), style = typeStyle("small"), color = colors.textSecondary)
                AtsButton(stringResource(R.string.backup_now), actions.onBackUpNow, kind = ButtonKind.FILLED, enabled = model.canBackUp)
                BackupStatusLine(model.writing, noData = model.blocked == BackupBlocked.NO_DATA, outcome = null, model.lastBackup)
            }
            AtsCard {
                Text(text = stringResource(R.string.restore_note), style = typeStyle("body"), color = colors.text)
                AtsButton(stringResource(R.string.restore_button), actions.onRestore, enabled = model.canRestore)
                if (model.restoreOffline) {
                    Text(
                        text = stringResource(R.string.restore_offline),
                        style = typeStyle("note"),
                        color = colors.textSecondary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }
    }
}

/** "Last backup: <date> <zone>" or "Last backup: never backed up" (section 9, C15), as on More; information only. */
@Composable
internal fun LastBackupLine(lastBackup: LastBackup?) {
    Text(
        text =
        if (lastBackup == null) {
            stringResource(R.string.last_backup_never)
        } else {
            stringResource(R.string.last_backup_on, lastBackup.date, lastBackup.zone)
        },
        style = typeStyle("body"),
        color = AtsTheme.colors.text,
    )
}

/** The warning after a backup whose write never ended (31 decision 7): in bronze, its meaning in its words (D-16). */
@Composable
internal fun BackupInterruptedLine() {
    Text(text = stringResource(R.string.backup_interrupted), style = typeStyle("message"), color = AtsTheme.colors.error)
}

// One line at most, read by TalkBack when it appears: the reason the button is inactive, loader-circle turning while the
// file is written (D-35), or the last export's result, never bronze (D-33). With [banner], the result as D-33's notice
// banner at the top of Backup and restore (10e frame B). Overall's reminder and the wizard's step 4 show the same line for
// an export started there (32 decision 1); they never have the no-data reason.
@Composable
internal fun BackupStatusLine(
    writing: Boolean,
    noData: Boolean,
    outcome: BackupOutcome?,
    last: LastBackup?,
    banner: Boolean = false,
) {
    val colors = AtsTheme.colors
    val spoken = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    if (writing) {
        AtsProgressRow(stringResource(R.string.backup_writing), done = false, modifier = spoken)
        return
    }
    val text =
        when {
            noData -> stringResource(R.string.backup_no_data)
            outcome == BackupOutcome.SAVED && last != null -> stringResource(R.string.backup_saved, last.date, last.zone)
            outcome == BackupOutcome.NOT_SAVED -> stringResource(R.string.backup_not_saved)
            outcome == BackupOutcome.NOT_SAVED_FILE_MAY_REMAIN -> stringResource(R.string.backup_not_saved_file_remains)
            outcome == BackupOutcome.SAVED_NOT_RECORDED -> stringResource(R.string.backup_saved_not_recorded)
            else -> return
        }
    if (banner) {
        AtsNoticeBanner(text, modifier = spoken)
    } else {
        Text(text = text, style = typeStyle(if (noData) "note" else "small"), color = if (noData) colors.textSecondary else colors.text, modifier = spoken)
    }
}

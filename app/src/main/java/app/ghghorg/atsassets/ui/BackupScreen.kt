package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R

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
 * (CR2): "<" and the title, the last backup line as on More, the line of an interrupted backup, Back up now and what it
 * does, then one line for where the export stands: why the button is inactive, the indicator while the file is
 * written, or how the last export ended. Then the Restore part of frame A (33b): what a restore does, and Restore from
 * backup file, inactive offline with the line why (marker 4; AC-08). No layer and no text field: everything is on the
 * screen, which scrolls. Back returns to More.
 */
@Composable
fun BackupScreen(
    model: BackupModel,
    actions: BackupActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = model.backEnabled, onBack = actions.onBack)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.backup_title), actions.onBack, enabled = model.backEnabled)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LastBackupLine(model.lastBackup)
                if (model.interrupted) BackupInterruptedLine()
                BackUpNowButton(model.canBackUp, actions.onBackUpNow)
                Text(
                    text = stringResource(R.string.backup_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BackupStatusLine(model.writing, noData = model.blocked == BackupBlocked.NO_DATA, model.outcome, model.lastBackup)
            }
        }
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.restore_note), style = MaterialTheme.typography.bodyLarge)
                if (model.restoreOffline) {
                    Text(
                        text = stringResource(R.string.restore_offline),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                InactiveDashedButton(stringResource(R.string.restore_button), model.canRestore, actions.onRestore)
            }
        }
    }
}

// Restore from backup file: inactive as Back up now is, dashed and read by TalkBack as unavailable (marker 4).
@Composable
private fun InactiveDashedButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = ButtonDefaults.outlinedShape
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        border = if (enabled) ButtonDefaults.outlinedButtonBorder(enabled = true) else null,
        modifier = if (enabled) Modifier else Modifier.dashedOutline(shape, MaterialTheme.colorScheme.outline),
    ) {
        Text(text = label)
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
        style = MaterialTheme.typography.bodyLarge,
    )
}

/** The warning after a backup whose write never ended (31 decision 7): plain text, no color alone. */
@Composable
internal fun BackupInterruptedLine() {
    Text(
        text = stringResource(R.string.backup_interrupted),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
    )
}

// Inactive as the app's other inactive controls: greyed by Material, read by TalkBack as unavailable, with a dashed
// outline (wireframe 10e marker 4's form for an inactive control).
@Composable
private fun BackUpNowButton(
    enabled: Boolean,
    onBackUpNow: () -> Unit,
) = InactiveDashedButton(stringResource(R.string.backup_now), enabled, onBackUpNow)

// One line at most, read by TalkBack when it appears: the reason the button is inactive, the indicator while the file
// is written, or the last export's result (frame B: plain text, one or the other). Overall's reminder shows the same
// line for an export started from it (32 decision 1); it never has the no-data reason.
@Composable
internal fun BackupStatusLine(
    writing: Boolean,
    noData: Boolean,
    outcome: BackupOutcome?,
    last: LastBackup?,
) {
    val text =
        when {
            writing -> stringResource(R.string.backup_writing)
            noData -> stringResource(R.string.backup_no_data)
            outcome == BackupOutcome.SAVED && last != null -> stringResource(R.string.backup_saved, last.date, last.zone)
            outcome == BackupOutcome.NOT_SAVED -> stringResource(R.string.backup_not_saved)
            outcome == BackupOutcome.NOT_SAVED_FILE_MAY_REMAIN -> stringResource(R.string.backup_not_saved_file_remains)
            outcome == BackupOutcome.SAVED_NOT_RECORDED -> stringResource(R.string.backup_saved_not_recorded)
            else -> return
        }
    val failed = outcome != null && outcome != BackupOutcome.SAVED
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

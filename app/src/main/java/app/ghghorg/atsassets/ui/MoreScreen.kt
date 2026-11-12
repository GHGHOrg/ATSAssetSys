package app.ghghorg.atsassets.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R

/**
 * More (section 6, wireframe 10f), drawn from its model (CR2, 17): the title with the gear at the top right that opens
 * Settings (marker 2; 20), the last backup line, then the entries built so far, each a row that opens its screen (17
 * decision 1: Net Worth (Total); 19 decision 1: Allocation; 26: Realized gains). Backup/restore comes later (31).
 */
@Composable
fun MoreScreen(
    model: MoreModel,
    onOpenNetWorth: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAllocation: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenRealizedGains: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.tab_more),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            SettingsGear(onOpenSettings)
        }
        // Always null until backups exist (31), which brings the "Last backup: <date> <zone>" line.
        if (model.lastBackup == null) {
            Text(
                text = stringResource(R.string.last_backup_never),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                model.entries.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    when (entry) {
                        MoreEntry.NET_WORTH -> EntryRow(stringResource(R.string.net_worth_title), onOpenNetWorth)
                        MoreEntry.ALLOCATION -> EntryRow(stringResource(R.string.allocation_title), onOpenAllocation)
                        MoreEntry.REALIZED_GAINS -> EntryRow(stringResource(R.string.realized_gains_title), onOpenRealizedGains)
                    }
                }
            }
        }
    }
}

/**
 * The gear (marker 2): read by TalkBack as "Settings" and a button, at least 48 dp. The gear is a text sign in its plain
 * (not emoji) form, so no icon library is added.
 */
@Composable
private fun SettingsGear(onOpen: () -> Unit) {
    val label = stringResource(R.string.settings_title)
    IconButton(onClick = onOpen, modifier = Modifier.size(48.dp).semantics { contentDescription = label }) {
        Text(text = GEAR, style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics { })
    }
}

// U+2699 GEAR with U+FE0E, the text presentation selector.
private const val GEAR = "⚙︎"

/** One entry: its name and ">", read by TalkBack as one button. */
@Composable
private fun EntryRow(
    name: String,
    onOpen: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = name
                role = Role.Button
                onClick {
                    onOpen()
                    true
                }
            }.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = ">", style = MaterialTheme.typography.bodyLarge)
    }
}

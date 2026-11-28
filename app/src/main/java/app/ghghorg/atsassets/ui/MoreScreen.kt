package app.ghghorg.atsassets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsIconButton
import app.ghghorg.atsassets.ui.design.AtsLinkRow
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp

/**
 * More (section 6, wireframe 10f), drawn from its model (CR2, 17), in the design system (44a step 8 group (e); drawing
 * spec/wireframes/design/10f-more-settings.html frame A): the title band with Lucide's settings icon at its end, 48 to tap
 * and read as "Settings", which opens Settings (marker 2; 20); the last backup line in its own card (marker 1: information
 * only; 31) with the line of an interrupted backup (31 decision 7); then the five entries in one card, each a row with a
 * chevron that opens its screen (17 decision 1: Net Worth (Total); 19 decision 1: Allocation; 26: Realized gains; 31:
 * Backup and restore; CR5, 41: About).
 */
@Composable
fun MoreScreen(
    model: MoreModel,
    onOpenNetWorth: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAllocation: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenRealizedGains: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    AtsScreen(
        title = stringResource(R.string.tab_more),
        modifier = modifier,
        actions = {
            AtsIconButton(LucideIcon.SETTINGS, stringResource(R.string.settings_title), onOpenSettings, tint = AtsTheme.colors.iconBand)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
        ) {
            AtsCard {
                LastBackupLine(model.lastBackup)
                if (model.backupInterrupted) BackupInterruptedLine()
            }
            AtsCard(padded = false) {
                model.entries.forEachIndexed { index, entry ->
                    if (index > 0) AtsRowDivider()
                    when (entry) {
                        MoreEntry.NET_WORTH -> AtsLinkRow(stringResource(R.string.net_worth_title), onOpenNetWorth)
                        MoreEntry.ALLOCATION -> AtsLinkRow(stringResource(R.string.allocation_title), onOpenAllocation)
                        MoreEntry.REALIZED_GAINS -> AtsLinkRow(stringResource(R.string.realized_gains_title), onOpenRealizedGains)
                        MoreEntry.BACKUP -> AtsLinkRow(stringResource(R.string.backup_title), onOpenBackup)
                        MoreEntry.ABOUT -> AtsLinkRow(stringResource(R.string.about_title), onOpenAbout)
                    }
                }
            }
        }
    }
}

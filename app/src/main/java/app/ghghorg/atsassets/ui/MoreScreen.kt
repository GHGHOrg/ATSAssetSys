package app.ghghorg.atsassets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R

/** More (spec 6), a stub in iteration 02b: the last backup line only. The entries and the gear come later. */
@Composable
fun MoreScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.tab_more),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.last_backup_never),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

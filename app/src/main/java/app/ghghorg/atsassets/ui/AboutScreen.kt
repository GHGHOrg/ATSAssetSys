package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R

/** About's callbacks, passed together. */
class AboutActions(
    val onBack: () -> Unit,
    val onToggle: (String) -> Unit,
)

/**
 * About (CR5, 41; spec section 6, More and Settings entry; wireframe 11), drawn from its model (CR2): "<" and the title,
 * the app's name as the phone shows it and its version (marker 2), then "Open-source licences": one row per library with
 * its licence's name, each opening in place to show the licence's text (marker 3). Drawn in the activity's window, not as
 * a pop-up, so MainActivity keeps its focus (CLAUDE.md section 6). One scrolling screen; Back returns to More.
 */
@Composable
fun AboutScreen(
    model: AboutModel,
    actions: AboutActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.about_title), actions.onBack)
        val name = stringResource(R.string.app_name)
        val version = stringResource(R.string.about_version, model.version)
        val spoken = stringResource(R.string.about_card_spoken, name, version)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clearAndSetSemantics { contentDescription = spoken },
            ) {
                Text(text = name, style = MaterialTheme.typography.titleLarge)
                Text(text = version, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = stringResource(R.string.about_licences),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                model.rows.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    LicenceRow(row) { actions.onToggle(row.name) }
                }
            }
        }
    }
}

/** One library: its name and its licence's name with ▾ or ▴; open, the licence's text under it. One button for TalkBack. */
@Composable
private fun LicenceRow(
    row: AboutRowModel,
    onToggle: () -> Unit,
) {
    val licenceName = stringResource(licenceName(row.licence))
    val state = stringResource(if (row.open) R.string.about_row_open else R.string.about_row_closed)
    val spoken = stringResource(R.string.about_row_spoken, row.name, licenceName)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .clearAndSetSemantics {
                    contentDescription = spoken
                    stateDescription = state
                    role = Role.Button
                    onClick {
                        onToggle()
                        true
                    }
                }.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = row.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = stringResource(if (row.open) R.string.about_licence_open else R.string.about_licence_closed, licenceName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (row.open) {
            val resources = LocalResources.current
            val id = licenceText(row.licence)
            val text = remember(id) { resources.openRawResource(id).bufferedReader().use { it.readText() } }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            )
        }
    }
}

private fun licenceName(licence: Licence): Int = when (licence) {
    Licence.APACHE_2_0 -> R.string.licence_apache_2_0
    Licence.SQLCIPHER -> R.string.licence_sqlcipher
}

private fun licenceText(licence: Licence): Int = when (licence) {
    Licence.APACHE_2_0 -> R.raw.licence_apache_2_0
    Licence.SQLCIPHER -> R.raw.licence_sqlcipher
}

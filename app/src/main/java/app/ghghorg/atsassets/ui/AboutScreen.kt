package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsSectionHeading
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/** About's callbacks, passed together. */
class AboutActions(
    val onBack: () -> Unit,
    val onToggle: (String) -> Unit,
)

/**
 * About (CR5, 41; spec section 6, More and Settings entry; wireframe 11), drawn from its model (CR2), in the design system
 * (44a step 8 group (e); drawing spec/wireframes/design/09-11-hidden-holdings-about.html frames C and D): under the title
 * band, a plain card with the app's name as the phone shows it in 22 and its version in secondary 14 (marker 2; note 2),
 * then "Open-source licences" over one card with a 48 row per library, the font and the icons among them (C27): its name,
 * its licence's name in secondary 14 at the end, and a chevron-down that turns up when open; open, the licence's text in
 * a block under its row, Asap 14 on the page color, keeping its own line breaks (marker 3; note 4). Drawn in the
 * activity's window, not as a pop-up, so MainActivity keeps its focus (CLAUDE.md section 6). Back returns to More.
 */
@Composable
fun AboutScreen(
    model: AboutModel,
    actions: AboutActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)
    val colors = AtsTheme.colors
    AtsScreen(
        title = stringResource(R.string.about_title),
        modifier = modifier,
        onBack = actions.onBack,
        backLabel = stringResource(R.string.back_button),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
        ) {
            val name = stringResource(R.string.app_name)
            val version = stringResource(R.string.about_version, model.version)
            val spoken = stringResource(R.string.about_card_spoken, name, version)
            AtsCard(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }) {
                Text(text = name, style = typeStyle("screenTitle"), color = colors.text)
                Text(text = version, style = typeStyle("small"), color = colors.textSecondary)
            }
            AtsSectionHeading(stringResource(R.string.about_licences))
            AtsCard(padded = false) {
                model.rows.forEachIndexed { index, row ->
                    if (index > 0) AtsRowDivider()
                    LicenceRow(row) { actions.onToggle(row.name) }
                }
            }
        }
    }
}

/** One library: its name, its licence's name and a chevron; open, the licence's text under it. One button for TalkBack. */
@Composable
private fun LicenceRow(
    row: AboutRowModel,
    onToggle: () -> Unit,
) {
    val colors = AtsTheme.colors
    val licenceName = stringResource(licenceName(row.licence))
    val state = stringResource(if (row.open) R.string.about_row_open else R.string.about_row_closed)
    val spoken = stringResource(R.string.about_row_spoken, row.name, licenceName)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = tokenDp("spaceTouch"))
                .clickable(role = Role.Button, onClick = onToggle)
                .clearAndSetSemantics {
                    contentDescription = spoken
                    stateDescription = state
                    role = Role.Button
                    onClick {
                        onToggle()
                        true
                    }
                }.padding(horizontal = tokenDp("spaceInset"), vertical = tokenDp("fieldDensePadding")),
            horizontalArrangement = Arrangement.spacedBy(tokenDp("rowControlGap")),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = row.name, style = typeStyle("body"), color = colors.text, modifier = Modifier.weight(1f))
            Text(text = licenceName, style = typeStyle("small"), color = colors.textSecondary)
            AtsIcon(if (row.open) LucideIcon.CHEVRON_UP else LucideIcon.CHEVRON_DOWN, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
        }
        if (row.open) {
            val resources = LocalResources.current
            val id = licenceText(row.licence)
            val text = remember(id) { resources.openRawResource(id).bufferedReader().use { it.readText() } }
            Text(
                text = text,
                style = typeStyle("small"),
                color = colors.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = tokenDp("spaceInset"), end = tokenDp("spaceInset"), bottom = tokenDp("spaceInset"))
                    .background(colors.page)
                    .padding(tokenDp("spaceInset")),
            )
        }
    }
}

private fun licenceName(licence: Licence): Int = when (licence) {
    Licence.APACHE_2_0 -> R.string.licence_apache_2_0
    Licence.SQLCIPHER -> R.string.licence_sqlcipher
    Licence.OFL_1_1 -> R.string.licence_ofl_1_1
    Licence.ISC_MIT -> R.string.licence_isc_mit
}

private fun licenceText(licence: Licence): Int = when (licence) {
    Licence.APACHE_2_0 -> R.raw.licence_apache_2_0
    Licence.SQLCIPHER -> R.raw.licence_sqlcipher
    Licence.OFL_1_1 -> R.raw.licence_ofl_1_1
    Licence.ISC_MIT -> R.raw.licence_isc_mit
}

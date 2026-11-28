package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.design.ButtonSize
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/** Hidden holdings' callbacks, passed together. */
class HiddenHoldingsActions(
    val onBack: () -> Unit,
    val onOpenHolding: (String) -> Unit,
    val onUnhide: (String) -> Unit,
)

/**
 * Hidden holdings (view 9, wireframe 09; iteration 15), drawn from its model (CR2, open item 48) in the design system
 * (44a step 8 group (b); drawing spec/wireframes/design/09-11-hidden-holdings-about.html frames A and B): the title band
 * with Back, the portfolio named at the top in a card with no chooser (D-44), then a card with one row per hidden holding
 * in ticker order (ticker, "0 shares", the last transaction date with "ET", Unhide, a chevron), or one plain line with
 * none hidden. A row opens Holding detail; Back returns here (15 decision 3).
 */
@Composable
fun HiddenHoldingsScreen(
    state: AppState,
    actions: HiddenHoldingsActions,
    modifier: Modifier = Modifier,
) {
    val model = state.hiddenHoldings ?: return
    BackHandler(onBack = actions.onBack)
    val colors = AtsTheme.colors
    AtsScreen(
        title = stringResource(R.string.hidden_title),
        modifier = modifier,
        onBack = actions.onBack,
        backLabel = stringResource(R.string.back_button),
        backEnabled = model.canAct,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
        ) {
            val portfolioSpoken = stringResource(R.string.hidden_title_spoken, model.portfolioName)
            AtsCard(modifier = Modifier.clearAndSetSemantics { contentDescription = portfolioSpoken }) {
                Text(text = stringResource(R.string.hidden_portfolio_label), style = typeStyle("small"), color = colors.textSecondary)
                Text(text = model.portfolioName, style = typeStyle("sectionHeading"), color = colors.text)
            }
            if (model.saveFailed) {
                Text(
                    text = stringResource(R.string.save_failed),
                    style = typeStyle("small"),
                    color = colors.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            if (model.rows.isEmpty()) {
                AtsCard {
                    Text(text = stringResource(R.string.hidden_none), style = typeStyle("body"), color = colors.textSecondary)
                }
            } else {
                AtsCard(padded = false) {
                    model.rows.forEachIndexed { index, row ->
                        if (index > 0) AtsRowDivider()
                        HiddenRowLine(row, enabled = model.canAct, actions)
                    }
                }
            }
        }
    }
}

/**
 * One hidden holding (drawing 09 frame A, note 1): the ticker in medium 16, "0 shares" and the last transaction in
 * secondary 14, Unhide as a small text button with Lucide's eye under them, and a chevron. The row opens Holding detail
 * and is read by TalkBack as one button; Unhide is its own button inside it.
 */
@Composable
private fun HiddenRowLine(
    row: HiddenHoldingRowModel,
    enabled: Boolean,
    actions: HiddenHoldingsActions,
) {
    val colors = AtsTheme.colors
    val shares = stringResource(R.string.holding_shares, row.shares)
    val spoken = stringResource(R.string.hidden_row_spoken, row.ticker, shares, row.lastTransaction.orEmpty())
    val unhideSpoken = stringResource(R.string.unhide_spoken, row.ticker)
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .clickable(enabled = enabled, role = Role.Button, onClick = { actions.onOpenHolding(row.ticker) })
            .semantics { contentDescription = spoken }
            .padding(tokenDp("spaceInset")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // The row's sentence reads these; Unhide below stays a button of its own.
            Column(modifier = Modifier.clearAndSetSemantics { }) {
                Text(text = row.ticker, style = typeStyle("sectionHeading"), color = colors.text)
                Text(text = shares, style = typeStyle("small"), color = colors.textSecondary)
                row.lastTransaction?.let {
                    Text(text = stringResource(R.string.hidden_last, it), style = typeStyle("small"), color = colors.textSecondary)
                }
            }
            AtsButton(
                stringResource(R.string.unhide_button),
                { actions.onUnhide(row.ticker) },
                modifier = Modifier.semantics { contentDescription = unhideSpoken },
                kind = ButtonKind.TEXT,
                size = ButtonSize.SMALL,
                enabled = enabled,
                icon = LucideIcon.EYE,
            )
        }
        AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
    }
}

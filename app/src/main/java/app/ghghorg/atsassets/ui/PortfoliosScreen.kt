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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.PortfolioRefusal
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsDialog
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsTextField
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.gainLossColor
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/**
 * Portfolios (view 3, wireframe 3 frame A), in the design system (44a step 8 group (b); drawing
 * spec/wireframes/design/03-portfolios.html frames A and D): under the title band, one card of rows in the order they
 * were created, each opening its portfolio (08 decision 3), then "Updating prices" and the refresh-failed line (marker 1;
 * 16b), then New portfolio as an outline button with Lucide's plus. Before the opening balance exists, New portfolio asks
 * for it instead of opening the dialog (4.2). Pulling down refreshes the prices (16b decision 2).
 */
@Composable
fun PortfoliosScreen(
    state: AppState,
    onOpenPortfolio: (String) -> Unit,
    onNewPortfolio: () -> Unit,
    onSetOpeningBalance: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
) {
    AtsScreen(stringResource(R.string.tab_portfolios), modifier) {
        PricePullToRefresh(state.refreshing, onRefresh, Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
                verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
            ) {
                if (state.portfolios.isEmpty()) {
                    NoPortfoliosYet(onNewPortfolio)
                } else {
                    AtsCard(padded = false) {
                        state.portfolios.forEachIndexed { index, portfolio ->
                            if (index > 0) AtsRowDivider()
                            PortfolioRow(portfolio, state.holdingsOf(portfolio.id), state.portfolioFigures(portfolio.id)) { onOpenPortfolio(portfolio.id) }
                        }
                    }
                }
                PriceRefreshLines(state.priceRefresh.refreshing, state.priceRefresh.failed, state.priceRefresh.noKey)
                if (state.portfolios.isNotEmpty()) AtsButton(stringResource(R.string.new_portfolio), onNewPortfolio, icon = LucideIcon.PLUS)
                if (state.portfolioNeedsOpeningBalance) {
                    AtsCard {
                        Text(
                            text = stringResource(R.string.opening_balance_first),
                            style = typeStyle("body"),
                            color = AtsTheme.colors.text,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        AtsButton(stringResource(R.string.guided_set_opening_balance), onSetOpeningBalance, kind = ButtonKind.FILLED)
                    }
                }
            }
        }
    }
}

/**
 * "No portfolios yet" with Create a portfolio (wireframe 3; Overall, 07 decision 2): D-32's nothing-to-enter card, the
 * line in medium 16 and the button outline (drawing 03 frame D).
 */
@Composable
internal fun NoPortfoliosYet(onCreate: () -> Unit) {
    AtsCard {
        Text(text = stringResource(R.string.no_portfolios_yet), style = typeStyle("sectionHeading"), color = AtsTheme.colors.text)
        AtsButton(stringResource(R.string.create_portfolio), onCreate)
    }
}

// Wireframe 3 marker 1, drawn from its model (CR2, 16a decision 1): name, number of holdings, the value with each
// holding at its latest price or at cost (4.8), and the gain/loss in green or red with its sign, with "(all holdings at
// cost)" while no holding with shares has a price (wording 4). No age line on a row (view 3); a portfolio's screen has
// it. The whole row is one tap target, read by TalkBack as one button with the figures (08 decision 3, wording 5). In
// the design system (drawing 03 frame A, note 1): the name in 16 with the count in secondary 14 under it, the figures on
// the right, and a chevron.
@Composable
private fun PortfolioRow(
    portfolio: Portfolio,
    holdings: List<Holding>,
    figures: PortfolioFiguresModel,
    onOpen: () -> Unit,
) {
    val count =
        if (holdings.isEmpty()) {
            stringResource(R.string.portfolio_no_holdings)
        } else {
            pluralStringResource(R.plurals.portfolio_holding_count, holdings.size, holdings.size)
        }
    val value = figures.value
    // CR6 (43): with its percentage; all at cost has none (no priced cost, 4.8).
    val gain = gainFigures(figures.gain)
    val atCost = figures.allAtCost
    val dollars = figures.spokenValue
    // Wireframe 3 shows no gain/loss line on a portfolio with no holdings ("Retirement").
    val spoken =
        if (holdings.isEmpty()) {
            stringResource(R.string.portfolio_row_empty_spoken, portfolio.name, count, dollars)
        } else {
            val base = stringResource(R.string.portfolio_row_spoken, portfolio.name, count, dollars, gainSpoken(figures.gain))
            // CR5 (41), AC-67: then its 1-day line.
            daySpoken(if (atCost) stringResource(R.string.portfolio_row_at_cost_spoken, base) else base, figures.day)
        }
    val colors = AtsTheme.colors
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .clickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick {
                    onOpen()
                    true
                }
            }.padding(tokenDp("spaceInset")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = portfolio.name, style = typeStyle("body"), color = colors.text)
            Text(text = count, style = typeStyle("small"), color = colors.textSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = value, style = typeStyle("body"), color = colors.text, textAlign = TextAlign.End)
            if (holdings.isNotEmpty()) {
                Text(
                    text = if (atCost) "$gain ${stringResource(R.string.holdings_at_cost)}" else gain,
                    style = typeStyle("small"),
                    color = gainLossColor(sign(figures.gain)),
                    textAlign = TextAlign.End,
                )
                // CR5 (41), wireframe 03 marker 1: the 1-day line under the gain/loss.
                figures.day?.let { DayRowText(it) }
            }
        }
        AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
    }
}

/** The wording that differs between New portfolio and Rename portfolio (wireframe 10d frames A and B). */
internal class PortfolioNameWording(
    val title: String,
    val helper: String,
    val confirm: String,
)

/**
 * The New portfolio and Rename portfolio dialog (wireframe 10d frames A to C; 44a step 8 group (e), drawing
 * spec/wireframes/design/10d-portfolio-dialogs.html): the catalog's dialog with one Name field, the helper as its note
 * line, a refusal on the field in place of the note (all three on the field, the duplicate included; 10d note 3), Cancel a
 * text button and the confirm button gold. The confirm button is always tappable. Drawn inside the activity's window, not
 * as a separate window: a separate window takes the focus, and MainActivity covers the app whenever its window loses
 * focus (07 phone check 2). A tap outside does nothing, so the typed text is not lost; Back closes it like Cancel.
 */
@Composable
internal fun PortfolioNameDialog(
    wording: PortfolioNameWording,
    dialog: PortfolioDialog,
    saving: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    BackHandler(enabled = !saving, onBack = onCancel)
    val error = portfolioMessage(dialog)
    AtsDialog(
        title = wording.title,
        actions = {
            AtsButton(stringResource(R.string.cancel_button), onCancel, kind = ButtonKind.TEXT, enabled = !saving)
            AtsButton(wording.confirm, onConfirm, kind = ButtonKind.FILLED, enabled = !saving)
        },
    ) {
        AtsTextField(
            value = dialog.nameText,
            onValueChange = onNameChange,
            label = stringResource(R.string.portfolio_name_label),
            modifier = Modifier.fillMaxWidth(),
            refusal = error,
            note = if (error == null) wording.helper else null,
            raised = true,
            announce = true,
        )
    }
}

@Composable
private fun portfolioMessage(dialog: PortfolioDialog): String? {
    if (dialog.save == SaveRefusal.SAVE_FAILED) return stringResource(R.string.save_failed)
    return when (val refusal = dialog.refusal) {
        null -> null

        PortfolioRefusal.TooShort -> stringResource(R.string.portfolio_name_too_short)

        PortfolioRefusal.NoLetter -> stringResource(R.string.portfolio_name_no_letter)

        is PortfolioRefusal.Duplicate -> stringResource(R.string.portfolio_name_duplicate, refusal.otherName)

        // The dialog opens only once the opening balance exists; the same words as the tab's line, should it happen.
        PortfolioRefusal.NoOpeningBalance -> stringResource(R.string.opening_balance_first)
    }
}

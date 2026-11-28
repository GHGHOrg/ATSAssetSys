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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.design.ButtonSize
import app.ghghorg.atsassets.ui.design.CardKind
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.gainLossColor
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/** Realized gains' callbacks, passed together. */
class RealizedGainsActions(
    val onBack: () -> Unit,
    val onFromChange: (String) -> Unit,
    val onToChange: (String) -> Unit,
    val onClearDates: () -> Unit,
    val onToggleHolding: (String, String) -> Unit,
    val onOpenSale: (String) -> Unit,
)

/**
 * Realized gains (view 5, wireframe 05; 26), drawn from its model (CR2): the title band with Back; From and To with the
 * date note and Clear dates (marker 1); the total with its count line (marker 2); "By holding" (marker 3), each row
 * expanding its sales (markers 5 and 6), each sale opening the sell (decision 1); with no sale, one plain line (marker
 * 7). One scrolling screen with no fixed part and no layer. Gains green with a plus sign, losses red with a minus sign,
 * zero uncolored; the sign is in the text, so the meaning never rests on the color. In the design system (44a step 8
 * group (c); drawing spec/wireframes/design/05-realized-gains.html; D-28, D-31, D-38 to D-40): standard date fields, the
 * total as the summary card, the holdings card with its column head, a sale as a block inside it.
 */
@Composable
fun RealizedGainsScreen(
    model: RealizedGainsModel,
    actions: RealizedGainsActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)
    AtsScreen(
        title = stringResource(R.string.realized_gains_title),
        modifier = modifier,
        onBack = actions.onBack,
        backLabel = stringResource(R.string.back_button),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
        ) {
            Dates(model, actions)
            TotalCard(model)
            if (model.holdings.isEmpty()) {
                PlainLineCard(stringResource(R.string.gains_none))
            } else {
                HoldingsCard(model, actions)
            }
        }
    }
}

/**
 * From and To as standard date fields side by side, the date note under them and Clear dates as a small text button at
 * the right of that line (marker 1; drawing 05 note 1). The calendar opens the filters' dense picker (spec 6).
 */
@Composable
private fun Dates(
    model: RealizedGainsModel,
    actions: RealizedGainsActions,
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
            FilterDateField(
                label = stringResource(R.string.filter_from),
                text = model.fromText,
                error = dateMessage(model.fromRefusal),
                onChange = actions.onFromChange,
                modifier = Modifier.weight(1f),
                dense = false,
            )
            FilterDateField(
                label = stringResource(R.string.filter_to),
                text = model.toText,
                error = dateMessage(model.toRefusal),
                onChange = actions.onToChange,
                modifier = Modifier.weight(1f),
                dense = false,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.date_note),
                style = typeStyle("note"),
                color = AtsTheme.colors.textSecondary,
                modifier = Modifier.weight(1f).padding(start = tokenDp("fieldPadding")),
            )
            AtsButton(stringResource(R.string.gains_clear_dates), actions.onClearDates, kind = ButtonKind.TEXT, size = ButtonSize.SMALL)
        }
    }
}

/**
 * The total as the summary card (marker 2; drawing 05 note 2): "Total realized gain" and the 32 figure in the summary
 * card's own gain or loss color, uncolored at zero, read as one line; the count line in condensed 12 under it.
 */
@Composable
private fun TotalCard(model: RealizedGainsModel) {
    val colors = AtsTheme.colors
    val spokenTotal = stringResource(R.string.gains_total_spoken, spoken(model.total.spoken))
    AtsCard(kind = CardKind.SUMMARY) {
        Column(modifier = Modifier.clearAndSetSemantics { contentDescription = spokenTotal }) {
            Text(text = stringResource(R.string.gains_total_label), style = typeStyle("small"), color = colors.onSummaryMuted)
            Text(text = model.total.text, style = typeStyle("bigFigure"), color = summaryGainColor(model.total.signum))
        }
        Text(text = countLine(model.countLine), style = typeStyle("oneDay"), color = colors.onSummaryMuted)
    }
}

@Composable
private fun countLine(line: GainsCountLine): String = when (line) {
    is GainsCountLine.AllDates -> pluralStringResource(R.plurals.gains_count_all, line.count, line.count)
    is GainsCountLine.Between -> pluralStringResource(R.plurals.gains_count_between, line.count, line.count, line.from, line.to)
    is GainsCountLine.From -> pluralStringResource(R.plurals.gains_count_from, line.count, line.count, line.from)
    is GainsCountLine.UpTo -> pluralStringResource(R.plurals.gains_count_up_to, line.count, line.count, line.to)
}

/** "By holding" with its column head (marker 3; drawing 05), then each holding, its sales under it while open. */
@Composable
private fun HoldingsCard(
    model: RealizedGainsModel,
    actions: RealizedGainsActions,
) {
    val colors = AtsTheme.colors
    AtsCard(padded = false) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = tokenDp("spaceInset"), end = tokenDp("spaceInset"), top = tokenDp("spaceInset")),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.gains_by_holding),
                style = typeStyle("sectionHeading"),
                color = colors.text,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(
                text = stringResource(R.string.gains_column),
                style = typeStyle("columnHead"),
                color = colors.textSecondary,
                modifier = Modifier.padding(end = tokenDp("fieldMarkSize") + tokenDp("spaceInset")).clearAndSetSemantics { },
            )
        }
        model.holdings.forEachIndexed { index, holding ->
            key(holding.portfolioId, holding.ticker) {
                if (index > 0) AtsRowDivider()
                HoldingRow(holding, actions)
            }
        }
    }
}

/**
 * One holding (marker 3; drawing 05 note 3): the ticker in medium 16, "Portfolio P, 1 sale" in secondary 14, the total in
 * 16 in green or red, and chevron-down closed or chevron-up open; read by TalkBack as one button with its name, portfolio,
 * count and total with its sign, and "expanded" or "collapsed". Its sales follow while expanded.
 */
@Composable
private fun HoldingRow(
    holding: RealizedHoldingModel,
    actions: RealizedGainsActions,
) {
    val colors = AtsTheme.colors
    val spokenRow =
        pluralStringResource(R.plurals.gains_holding_spoken, holding.count, holding.ticker, holding.portfolioName, holding.count, spoken(holding.total.spoken))
    val state = stringResource(if (holding.expanded) R.string.gains_expanded else R.string.gains_collapsed)
    val toggle = { actions.onToggleHolding(holding.portfolioId, holding.ticker) }
    Column {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = tokenDp("spaceTouch"))
                .clickable(role = Role.Button, onClick = toggle)
                .clearAndSetSemantics {
                    contentDescription = spokenRow
                    stateDescription = state
                    role = Role.Button
                    onClick {
                        toggle()
                        true
                    }
                }.padding(tokenDp("spaceInset")),
            horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StartEndRow(
                modifier = Modifier.weight(1f),
                start = {
                    Column {
                        Text(text = holding.ticker, style = typeStyle("sectionHeading"), color = colors.text)
                        Text(
                            text = pluralStringResource(R.plurals.gains_holding_line, holding.count, holding.portfolioName, holding.count),
                            style = typeStyle("small"),
                            color = colors.textSecondary,
                        )
                    }
                },
                end = {
                    Text(text = holding.total.text, style = typeStyle("body"), color = gainLossColor(holding.total.signum), textAlign = TextAlign.End)
                },
            )
            AtsIcon(if (holding.expanded) LucideIcon.CHEVRON_UP else LucideIcon.CHEVRON_DOWN, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
        }
        holding.sales.forEach { sale ->
            key(sale.sellId) { SaleBlock(sale, actions.onOpenSale) }
        }
    }
}

/**
 * One sale (marker 6; drawing 05 note 4), a block inside the card, indented under a line: its date with "ET" and its
 * gain/loss as a medium line, then quantity, proceeds and the cost of the lots used as label-and-value rows (D-31), and a
 * chevron, as the block opens the sell (decision 1); read by TalkBack as one button with each figure.
 */
@Composable
private fun SaleBlock(
    sale: RealizedSaleModel,
    onOpen: (String) -> Unit,
) {
    val colors = AtsTheme.colors
    val spokenSale = stringResource(R.string.gains_sale_spoken, sale.date, spoken(sale.gain.spoken), sale.quantity, sale.spokenProceeds, sale.spokenCost)
    val open = { onOpen(sale.sellId) }
    Column(modifier = Modifier.padding(start = tokenDp("spaceInset") + tokenDp("spaceMargin"))) {
        AtsRowDivider()
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = open)
                .clearAndSetSemantics {
                    contentDescription = spokenSale
                    role = Role.Button
                    onClick {
                        open()
                        true
                    }
                }.padding(tokenDp("spaceInset")),
            horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokenDp("cardGap"))) {
                StartEndRow(
                    modifier = Modifier.fillMaxWidth(),
                    start = { Text(text = stringResource(R.string.gains_sale_date, sale.date), style = typeStyle("sectionHeading"), color = colors.text) },
                    end = {
                        Text(
                            text = sale.gain.text,
                            style = typeStyle("sectionHeading"),
                            color = gainLossColor(sale.gain.signum),
                            textAlign = TextAlign.End,
                        )
                    },
                )
                SaleFigure(stringResource(R.string.gains_quantity), sale.quantity)
                SaleFigure(stringResource(R.string.gains_proceeds), sale.proceeds)
                SaleFigure(stringResource(R.string.gains_cost), sale.cost)
            }
            AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
        }
    }
}

@Composable
private fun SaleFigure(
    label: String,
    value: String,
) {
    val colors = AtsTheme.colors
    StartEndRow(
        modifier = Modifier.fillMaxWidth(),
        start = { Text(text = label, style = typeStyle("body"), color = colors.textSecondary) },
        end = { Text(text = value, style = typeStyle("body"), color = colors.text, textAlign = TextAlign.End) },
    )
}

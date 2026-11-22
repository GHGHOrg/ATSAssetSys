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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.theme.gainLossColor

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
 * Realized gains (view 5, wireframe 05; 26), drawn from its model (CR2): "<" and the title; From and To with the date
 * note and Clear dates (marker 1); the total with its count line (marker 2); "By holding" (marker 3), each row expanding
 * its sales (markers 5 and 6), each sale opening the sell (decision 1); with no sale, one plain line (marker 7). One
 * scrolling screen with no fixed part and no layer. Gains green with a plus sign, losses red with a minus sign, zero
 * uncolored; the sign is in the text, so the meaning never rests on the color.
 */
@Composable
fun RealizedGainsScreen(
    model: RealizedGainsModel,
    actions: RealizedGainsActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.realized_gains_title), actions.onBack)
        DatesCard(model, actions)
        TotalCard(model)
        if (model.holdings.isEmpty()) {
            Text(text = stringResource(R.string.gains_none), style = MaterialTheme.typography.bodyLarge)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.gains_by_holding),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.gains_column),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                model.holdings.forEachIndexed { index, holding ->
                    key(holding.portfolioId, holding.ticker) {
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        HoldingRow(holding, actions)
                    }
                }
            }
        }
    }
}

/** From and To with the date note and Clear dates (marker 1): the Cash list's fields, row and lines (decision 3). */
@Composable
private fun DatesCard(
    model: RealizedGainsModel,
    actions: RealizedGainsActions,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDateField(
                    label = stringResource(R.string.filter_from),
                    text = model.fromText,
                    error = dateMessage(model.fromRefusal),
                    onChange = actions.onFromChange,
                    modifier = Modifier.weight(1f),
                )
                FilterDateField(
                    label = stringResource(R.string.filter_to),
                    text = model.toText,
                    error = dateMessage(model.toRefusal),
                    onChange = actions.onToChange,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(R.string.date_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = actions.onClearDates) {
                Text(text = stringResource(R.string.gains_clear_dates))
            }
        }
    }
}

/** The total and its count line (marker 2), read by TalkBack as one line each. */
@Composable
private fun TotalCard(model: RealizedGainsModel) {
    val spokenTotal = stringResource(R.string.gains_total_spoken, spoken(model.total.spoken))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(modifier = Modifier.clearAndSetSemantics { contentDescription = spokenTotal }) {
            Text(text = stringResource(R.string.gains_total_label), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = model.total.text,
                style = MaterialTheme.typography.headlineSmall,
                color = gainLossColor(model.total.signum),
            )
        }
        Text(text = countLine(model.countLine), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun countLine(line: GainsCountLine): String = when (line) {
    is GainsCountLine.AllDates -> pluralStringResource(R.plurals.gains_count_all, line.count, line.count)
    is GainsCountLine.Between -> pluralStringResource(R.plurals.gains_count_between, line.count, line.count, line.from, line.to)
    is GainsCountLine.From -> pluralStringResource(R.plurals.gains_count_from, line.count, line.count, line.from)
    is GainsCountLine.UpTo -> pluralStringResource(R.plurals.gains_count_up_to, line.count, line.count, line.to)
}

/**
 * One holding (marker 3): ticker, "Portfolio P, 1 sale" and the total, with "v" or "^"; read by TalkBack as one button
 * with its name, portfolio, count and total with its sign, and "expanded" or "collapsed". Its sales follow while expanded.
 */
@Composable
private fun HoldingRow(
    holding: RealizedHoldingModel,
    actions: RealizedGainsActions,
) {
    val spokenRow =
        pluralStringResource(R.plurals.gains_holding_spoken, holding.count, holding.ticker, holding.portfolioName, holding.count, spoken(holding.total.spoken))
    val state = stringResource(if (holding.expanded) R.string.gains_expanded else R.string.gains_collapsed)
    val toggle = { actions.onToggleHolding(holding.portfolioId, holding.ticker) }
    Column {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = toggle)
                .clearAndSetSemantics {
                    contentDescription = spokenRow
                    stateDescription = state
                    role = Role.Button
                    onClick {
                        toggle()
                        true
                    }
                }.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = holding.ticker, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = pluralStringResource(R.plurals.gains_holding_line, holding.count, holding.portfolioName, holding.count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text = holding.total.text, style = MaterialTheme.typography.bodyLarge, color = gainLossColor(holding.total.signum))
            Text(text = if (holding.expanded) "^" else "v", style = MaterialTheme.typography.bodyLarge)
        }
        holding.sales.forEach { sale ->
            key(sale.sellId) { SaleRow(sale, actions.onOpenSale) }
        }
    }
}

/**
 * One sale (marker 6): its date with "ET" and its gain/loss, then quantity, proceeds and the cost of the lots used, and
 * ">"; read by TalkBack as one button with each figure, opening the sell (decision 1).
 */
@Composable
private fun SaleRow(
    sale: RealizedSaleModel,
    onOpen: (String) -> Unit,
) {
    val spokenSale = stringResource(R.string.gains_sale_spoken, sale.date, spoken(sale.gain.spoken), sale.quantity, sale.spokenProceeds, sale.spokenCost)
    val open = { onOpen(sale.sellId) }
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
            }.padding(start = 28.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row {
                Text(
                    text = stringResource(R.string.gains_sale_date, sale.date),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(text = sale.gain.text, style = MaterialTheme.typography.bodyLarge, color = gainLossColor(sale.gain.signum))
            }
            SaleFigure(stringResource(R.string.gains_quantity), sale.quantity)
            SaleFigure(stringResource(R.string.gains_proceeds), sale.proceeds)
            SaleFigure(stringResource(R.string.gains_cost), sale.cost)
        }
        Text(text = ">", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SaleFigure(
    label: String,
    value: String,
) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

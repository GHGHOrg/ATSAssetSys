package app.ghghorg.atsassets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.theme.gainLossColor

/*
 * The 1-day line (CR5, 41; spec section 6, the 1-day line; wireframes 01, 02, 03 and 06), drawn from its model (CR2):
 * the screen only picks the approved label and puts the model's figures in.
 */

/**
 * "1-day gain/loss  +$16.00 (+1.27%)" as one line, indented and smaller in a card or header ([indented]), colored by the
 * dollar figure's sign (section 6); no percentage at a start of $0.00. TalkBack reads it as part of its card or row
 * ([daySpoken]), so it adds no node of its own.
 */
@Composable
internal fun DayLineRow(
    day: DayLineModel,
    indented: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = if (indented) 16.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.day_label),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = dayFigures(day),
            style = MaterialTheme.typography.bodySmall,
            color = gainLossColor(daySign(day)),
            textAlign = TextAlign.End,
        )
    }
}

/** A card's line of its own for TalkBack (a portfolio's summary): the day line with its sentence. */
@Composable
internal fun DaySummaryLine(day: DayLineModel) {
    val dollars = spoken(day.spoken)
    val spoken =
        day.spokenPercent?.let { stringResource(R.string.day_line_spoken, dollars, spoken(it)) }
            ?: stringResource(R.string.day_line_spoken_no_percent, dollars)
    DayLineRow(day, indented = true, modifier = Modifier.clearAndSetSemantics { contentDescription = spoken })
}

/** In a row, under its gain/loss: "1-day +$16.00 (+1.27%)", small and colored by the dollar figure's sign. */
@Composable
internal fun DayRowText(day: DayLineModel) {
    Text(
        text = stringResource(R.string.day_row, dayFigures(day)),
        style = MaterialTheme.typography.bodySmall,
        color = gainLossColor(daySign(day)),
        textAlign = TextAlign.End,
    )
}

/** "+$16.00 (+1.27%)", or "+$16.00" with no percentage. */
@Composable
internal fun dayFigures(day: DayLineModel): String = day.percent?.let { stringResource(R.string.day_figures, day.text, it) } ?: day.text

/** [base] with the day line read after it, for a card's or a row's one TalkBack sentence; [base] alone without one. */
@Composable
internal fun daySpoken(
    base: String,
    day: DayLineModel?,
): String {
    if (day == null) return base
    val dollars = spoken(day.spoken)
    val percent = day.spokenPercent?.let { spoken(it) }
    return if (percent == null) {
        stringResource(R.string.day_spoken_no_percent, base, dollars)
    } else {
        stringResource(R.string.day_spoken, base, dollars, percent)
    }
}

// The color's sign is the dollar figure's (section 6): green, red or none.
private fun daySign(day: DayLineModel): Int = when (day.spoken.sign) {
    FigureSign.PLUS -> 1
    FigureSign.MINUS -> -1
    FigureSign.NONE -> 0
}

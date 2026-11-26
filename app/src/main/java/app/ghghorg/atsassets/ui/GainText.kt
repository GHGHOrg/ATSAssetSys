package app.ghghorg.atsassets.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.ghghorg.atsassets.R

/*
 * The unrealized gain/loss with its percentage as every screen shows and reads it (CR6, 43; spec section 6, number format;
 * C26). The screens put the model's values into the approved strings; nothing is worked out here.
 */

/**
 * "+$80.00 (+6.67%)", or "+$80.00" with no percentage (a cost of $0.00, 4.8). One text with a space between the two, so
 * where they do not fit on one line the percentage wraps to the line under the dollar figure (C26).
 */
@Composable
internal fun gainFigures(gain: GainModel.Figure): String = gain.percent?.let { stringResource(R.string.gain_figures, gain.text, it.text) } ?: gain.text

/** "plus 80.00 dollars, plus 6.67 percent", or "plus 80.00 dollars" with no percentage, for TalkBack (section 6). */
@Composable
internal fun gainSpoken(gain: GainModel.Figure): String {
    val dollars = spoken(gain.spoken)
    return gain.percent?.let { stringResource(R.string.gain_spoken, dollars, spoken(it.spoken)) }
        ?: stringResource(R.string.gain_spoken_no_percent, dollars)
}

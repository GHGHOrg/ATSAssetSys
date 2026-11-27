package app.ghghorg.atsassets.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 44a step 6 (spec K9): a size or an opacity from the tokens (base.tokens.json), by its generated name, so no part of a
// screen writes a number of its own.

/** A dimension token in dp, for example "buttonMediumHeight". */
internal fun tokenDp(name: String): Dp = TokenBase.sizes.getValue(name).dp

/** A number token, for example "opacityDisabled". */
internal fun tokenNumber(name: String): Float = TokenBase.sizes.getValue(name)

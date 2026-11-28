package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Quantity

/*
 * Hidden holdings as the screen draws it (view 9, wireframe 09; CR2, open item 48; 44a step 8 group (b)).
 * Plain Kotlin: the screen picks the approved string for each.
 */

/** One hidden holding: its ticker, its shares as shown (always "0": only a holding at 0 shares is hidden), its last date. */
data class HiddenHoldingRowModel(
    val ticker: String,
    val shares: String,
    val lastTransaction: String?,
)

/**
 * [portfolioName] is the portfolio named at the top; [rows] are in ticker order (view 9, marker 2); [saveFailed] is the
 * "Not saved" line after a failed Unhide; [canAct] is false while a save runs, so no row or Unhide can be tapped.
 */
data class HiddenHoldingsModel(
    val portfolioName: String,
    val rows: List<HiddenHoldingRowModel>,
    val saveFailed: Boolean,
    val canAct: Boolean,
)

object HiddenHoldingsView {
    fun of(
        portfolioName: String,
        rows: List<HiddenRow>,
        saveFailed: Boolean,
        saving: Boolean,
    ): HiddenHoldingsModel = HiddenHoldingsModel(
        portfolioName = portfolioName,
        rows = rows.map { HiddenHoldingRowModel(it.ticker, Quantity.format(Fraction.ZERO), it.lastTransaction?.toString()) },
        saveFailed = saveFailed,
        canAct = !saving,
    )
}

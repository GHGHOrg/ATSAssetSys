package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TabOrder

/*
 * The Tab order screen as it draws it (section 6, More and Settings entry; wireframe 10f frame E, marker 11; CR2, 22):
 * one row per tab, left to right, with its place and whether Up and Down move it. Plain Kotlin: the screen picks the
 * approved string for each.
 */

/**
 * [tab] at [position] (from 1) of [count], read by TalkBack as "More, position 1 of 4" (decision 2); Up shown inactive
 * on the first row and Down on the last (decision 1).
 */
data class TabOrderRow(
    val tab: Tab,
    val position: Int,
    val count: Int,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
)

/** [notSaved] when the last move could not be saved; the order shown is then the one before it. */
data class TabOrderModel(
    val rows: List<TabOrderRow>,
    val notSaved: Boolean,
)

object TabOrders {
    fun of(
        order: TabOrder,
        refusal: SaveRefusal?,
    ): TabOrderModel = TabOrderModel(
        rows = order.tabs.mapIndexed { index, tab ->
            TabOrderRow(
                tab = tab,
                position = index + 1,
                count = order.tabs.size,
                canMoveUp = order.canMoveUp(tab),
                canMoveDown = order.canMoveDown(tab),
            )
        },
        notSaved = refusal != null,
    )
}

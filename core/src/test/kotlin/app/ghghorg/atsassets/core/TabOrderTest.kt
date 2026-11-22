package app.ghghorg.atsassets.core

import app.ghghorg.atsassets.core.Tab.CASH
import app.ghghorg.atsassets.core.Tab.MORE
import app.ghghorg.atsassets.core.Tab.OVERALL
import app.ghghorg.atsassets.core.Tab.PORTFOLIOS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabOrderTest {
    private fun orderOf(vararg tabs: Tab): TabOrder = TabOrder.of(tabs.toList())!!

    /** Every order of the four tabs, 24 in all. */
    private fun everyOrder(rest: List<Tab> = Tab.entries): List<List<Tab>> {
        if (rest.isEmpty()) return listOf(emptyList())
        return rest.flatMap { tab -> everyOrder(rest - tab).map { listOf(tab) + it } }
    }

    @Test
    fun default_isMoreCashPortfoliosOverall() {
        // Spec 6, navigation outline, and spec 8's "If missing".
        assertEquals(listOf(MORE, CASH, PORTFOLIOS, OVERALL), TabOrder.DEFAULT.tabs)
        assertEquals("more;cash;portfolios;overall", TabOrder.DEFAULT.text)
    }

    @Test
    fun of_eachTabOnce_isAnOrder() {
        assertEquals(listOf(OVERALL, MORE, CASH, PORTFOLIOS), TabOrder.of(listOf(OVERALL, MORE, CASH, PORTFOLIOS))?.tabs)
    }

    @Test
    fun of_aTabMissingOrTwice_isRefused() {
        assertNull(TabOrder.of(listOf(MORE, CASH, PORTFOLIOS)))
        assertNull(TabOrder.of(listOf(MORE, MORE, CASH, OVERALL)))
        assertNull(TabOrder.of(listOf(MORE, CASH, PORTFOLIOS, OVERALL, MORE)))
        assertNull(TabOrder.of(emptyList()))
    }

    @Test
    fun moveDown_movesTheTabOnePlaceRight() {
        assertEquals(listOf(CASH, MORE, PORTFOLIOS, OVERALL), TabOrder.DEFAULT.moveDown(MORE).tabs)
        assertEquals(listOf(MORE, CASH, OVERALL, PORTFOLIOS), TabOrder.DEFAULT.moveDown(PORTFOLIOS).tabs)
    }

    @Test
    fun moveUp_movesTheTabOnePlaceLeft() {
        assertEquals(listOf(MORE, CASH, OVERALL, PORTFOLIOS), TabOrder.DEFAULT.moveUp(OVERALL).tabs)
        assertEquals(listOf(CASH, MORE, PORTFOLIOS, OVERALL), TabOrder.DEFAULT.moveUp(CASH).tabs)
    }

    @Test
    fun upOnTheFirst_andDownOnTheLast_changeNothing() {
        // Decision 1: those two buttons are shown but inactive.
        assertFalse(TabOrder.DEFAULT.canMoveUp(MORE))
        assertFalse(TabOrder.DEFAULT.canMoveDown(OVERALL))
        assertTrue(TabOrder.DEFAULT.canMoveUp(CASH))
        assertTrue(TabOrder.DEFAULT.canMoveDown(PORTFOLIOS))
        assertEquals(TabOrder.DEFAULT, TabOrder.DEFAULT.moveUp(MORE))
        assertEquals(TabOrder.DEFAULT, TabOrder.DEFAULT.moveDown(OVERALL))
    }

    @Test
    fun moves_keepEachTabOnce() {
        // More, Cash, Portfolios, Overall -> Cash, More, Portfolios, Overall -> Cash, Portfolios, More, Overall
        // -> Cash, Portfolios, Overall, More -> Portfolios, Cash, Overall, More.
        val moved = TabOrder.DEFAULT.moveDown(MORE).moveDown(MORE).moveUp(OVERALL).moveDown(CASH)
        assertEquals(listOf(PORTFOLIOS, CASH, OVERALL, MORE), moved.tabs)
        assertEquals(moved, TabOrder.of(moved.tabs))
    }

    @Test
    fun parse_readsTheTextForm_caseIgnored() {
        // Spec 8: "Values (case ignored)".
        assertEquals(listOf(OVERALL, CASH, PORTFOLIOS, MORE), TabOrder.parse("overall;Cash;PORTFOLIOS;more")?.tabs)
        assertEquals("overall;cash;portfolios;more", TabOrder.parse("OVERALL;CASH;PORTFOLIOS;MORE")?.text)
    }

    @Test
    fun parse_refusesATabMissingTwiceOrUnknown() {
        // Spec 8, state rows: "a tab order that is not each tab once".
        assertNull(TabOrder.parse("more;cash;portfolios"))
        assertNull(TabOrder.parse("more;cash;cash;overall"))
        assertNull(TabOrder.parse("more;cash;portfolios;overall;more"))
        assertNull(TabOrder.parse("more;cash;portfolios;settings"))
        assertNull(TabOrder.parse("more,cash,portfolios,overall"))
        assertNull(TabOrder.parse("more;cash;portfolios;overall;"))
        assertNull(TabOrder.parse(""))
    }

    @Test
    fun text_readsBackAsTheSameOrder_forEveryOrder() {
        val orders = everyOrder()
        assertEquals(24, orders.size)
        for (tabs in orders) {
            val order = orderOf(*tabs.toTypedArray())
            assertEquals(order, TabOrder.parse(order.text))
        }
    }
}

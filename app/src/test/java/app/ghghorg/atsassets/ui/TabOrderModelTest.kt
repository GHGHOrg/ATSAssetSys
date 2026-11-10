package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TabOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 22: the Tab order screen's model (wireframe 10f frame E, marker 11; CR2). */
class TabOrderModelTest {
    private val overallFirst = TabOrder.of(listOf(Tab.OVERALL, Tab.MORE, Tab.CASH, Tab.PORTFOLIOS))!!

    @Test
    fun oneRowPerTab_inTheOrder_numberedFromOne() {
        val rows = TabOrders.of(TabOrder.DEFAULT, refusal = null).rows
        assertEquals(listOf(Tab.MORE, Tab.CASH, Tab.PORTFOLIOS, Tab.OVERALL), rows.map { it.tab })
        assertEquals(listOf(1, 2, 3, 4), rows.map { it.position })
        assertEquals(listOf(4, 4, 4, 4), rows.map { it.count })
    }

    @Test
    fun theRows_followTheOrderInForce() {
        val rows = TabOrders.of(overallFirst, refusal = null).rows
        assertEquals(listOf(Tab.OVERALL, Tab.MORE, Tab.CASH, Tab.PORTFOLIOS), rows.map { it.tab })
        assertEquals(TabOrderRow(Tab.OVERALL, position = 1, count = 4, canMoveUp = false, canMoveDown = true), rows.first())
    }

    @Test
    fun theFirstCannotMoveUp_theLastCannotMoveDown_theOthersBoth() {
        // Decision 1: those two buttons are shown but inactive.
        val rows = TabOrders.of(TabOrder.DEFAULT, refusal = null).rows
        assertEquals(listOf(false, true, true, true), rows.map { it.canMoveUp })
        assertEquals(listOf(true, true, true, false), rows.map { it.canMoveDown })
    }

    @Test
    fun aFailedSave_isShown_andNoRefusalShowsNothing() {
        assertTrue(TabOrders.of(TabOrder.DEFAULT, SaveRefusal.SAVE_FAILED).notSaved)
        assertFalse(TabOrders.of(TabOrder.DEFAULT, refusal = null).notSaved)
    }
}

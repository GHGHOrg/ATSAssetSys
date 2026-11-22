package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.DeletionCheck
import app.ghghorg.atsassets.core.DeletionSummary
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.time.LocalDate

// 29 step 5 (CR2): the Delete dialog and the blocked message as models, worked out without drawing the screen.
class PortfolioDeletionModelTest {
    private val p = Portfolio("p1", "Portfolio P", 1L)

    // Example A (AC-55): 3 transactions, 3 trade cash lines, +$800.00, $9,600.00 to $10,000.00.
    private val exampleA = DeletionSummary(p, 3, 3, Fraction.ofCents(80_000L), 960_000L, 1_000_000L)

    @Test
    fun exampleA_theSummary_asShownAndAsRead() {
        val model = PortfolioDeletions.dialog(DeleteDialog(exampleA), saving = false)
        assertEquals("Portfolio P", model.name)
        assertEquals(3, model.transactionCount)
        assertEquals(3, model.tradeLineCount)
        assertEquals("+$800.00", model.realizedGain)
        assertEquals(SpokenFigure("800.00", FigureSign.PLUS), model.spokenRealizedGain)
        assertEquals("$9,600.00", model.balanceNow)
        assertEquals("$10,000.00", model.balanceAfter)
        assertEquals(SpokenFigure("10,000.00", FigureSign.NONE), model.spokenBalanceAfter)
        assertEquals("", model.typedName)
        assertFalse(model.deleteEnabled)
        assertFalse(model.saveFailed)
    }

    // AC-55: Delete portfolio is active only once the typed name matches, case and spaces ignored.
    @Test
    fun deletePortfolio_isActive_onlyWithTheName() {
        assertFalse(PortfolioDeletions.dialog(DeleteDialog(exampleA, "Portfolio"), false).deleteEnabled)
        assertTrue(PortfolioDeletions.dialog(DeleteDialog(exampleA, " portfolio  P"), false).deleteEnabled)
        assertFalse(PortfolioDeletions.dialog(DeleteDialog(exampleA, "Portfolio P"), saving = true).deleteEnabled)
    }

    // Section 6: a loss with a minus sign, zero with none; the gain is rounded to cents once (4.10).
    @Test
    fun aLoss_andZero_andARoundedGain() {
        val loss = PortfolioDeletions.dialog(DeleteDialog(exampleA.copy(realizedGain = Fraction.ofCents(-12_345L))), false)
        assertEquals("−$123.45", loss.realizedGain)
        assertEquals(SpokenFigure("123.45", FigureSign.MINUS), loss.spokenRealizedGain)
        val zero = PortfolioDeletions.dialog(DeleteDialog(exampleA.copy(realizedGain = Fraction.ZERO)), false)
        assertEquals("$0.00", zero.realizedGain)
        assertEquals(FigureSign.NONE, zero.spokenRealizedGain.sign)
        val third = Fraction.of(BigInteger.ONE, BigInteger.valueOf(3))
        assertEquals("+$0.33", PortfolioDeletions.dialog(DeleteDialog(exampleA.copy(realizedGain = third)), false).realizedGain)
    }

    @Test
    fun aFailedSave_andTheTypedName_reachTheModel() {
        val model = PortfolioDeletions.dialog(DeleteDialog(exampleA, "Portfolio P", SaveRefusal.SAVE_FAILED), false)
        assertTrue(model.saveFailed)
        assertEquals("Portfolio P", model.typedName)
        assertTrue(model.deleteEnabled)
    }

    // Example E (AC-55): the date, $200.00 and the day before as the example.
    @Test
    fun exampleE_theBlockedMessage() {
        val model = PortfolioDeletions.blocked(DeletionCheck.Blocked(LocalDate.parse("2026-02-10"), 20_000L))
        assertEquals(DeleteBlockedModel(false, "2026-02-10", "$200.00", "2026-02-09"), model)
    }

    @Test
    fun theOpeningDateCase_hasNoExample() {
        val model = PortfolioDeletions.blocked(DeletionCheck.CannotDelete(LocalDate.parse("2026-01-01"), 30_000L))
        assertEquals(DeleteBlockedModel(true, "2026-01-01", "$300.00", null), model)
    }

    @Test
    fun noMessage_withoutABlockedCheck() {
        assertNull(PortfolioDeletions.blocked(null))
        assertNull(PortfolioDeletions.blocked(DeletionCheck.NotFound))
    }
}

package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 15a (CR2): Holding detail's Transactions rows as a model, worked out without drawing the screen.
class HoldingRowModelTest {
    private fun transaction(
        id: String,
        type: TransactionType,
        date: String,
        seq: Long,
        quantity: String = "0",
        totalCents: Long = 0L,
        ticker: String = "MA",
        portfolioId: String = "p1",
        quantityRatio: SplitRatio? = null,
        split: SplitRatio? = null,
        reversesId: String? = null,
        allocations: List<SellAllocation> = emptyList(),
    ): Transaction = Transaction(
        id = id,
        portfolioId = portfolioId,
        ticker = ticker,
        type = type,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        reversesId = reversesId,
        allocations = allocations,
        split = split,
        quantityRatio = quantityRatio,
    )

    // 14c phone check 3's MA: a buy of 1, a 1 for 3 the same day, the buy reversed and re-entered with ratio 1 to 3,
    // then a 3 for 1.
    private val ma =
        listOf(
            transaction("b1", TransactionType.BUY, "2026-11-03", 2L, "1", 10_000L),
            transaction("x1", TransactionType.SPLIT, "2026-11-03", 3L, split = SplitRatio(1, 3)),
            transaction("r1", TransactionType.REVERSAL, "2026-11-03", 4L, reversesId = "b1"),
            transaction("n1", TransactionType.BUY, "2026-11-03", 5L, "1", 10_000L, quantityRatio = SplitRatio(1, 3)),
            transaction("x2", TransactionType.SPLIT, "2026-11-03", 6L, split = SplitRatio(3, 1)),
        )

    @Test
    fun theRowsAreNewestFirst_andOnASharedDateTheLaterEntryFirst() {
        assertEquals(listOf("x2", "n1", "r1", "x1", "b1"), HoldingRows.of(ma, "p1", "MA").map { it.transactionId })
    }

    // The 14c Deploy finding: a re-entered buy's row showed its stored quantity, "1 share".
    @Test
    fun aBuyWithAQuantityRatio_showsItsExactShares() {
        val row = HoldingRows.of(ma, "p1", "MA").first { it.transactionId == "n1" }
        assertEquals(HoldingRowModel.Trade("n1", "2026-11-03", sell = false, shares = "0.33333333", oneShare = false, amount = "$100.00"), row)
    }

    @Test
    fun aBuyOfOneShare_isOneShare_andASell_isASell() {
        val sold = transaction("s1", TransactionType.SELL, "2026-11-04", 7L, "2.5", 25_050L, allocations = listOf(SellAllocation("b9", Fraction.of(BigDecimal("2.5")))))
        val rows = HoldingRows.of(ma + sold, "p1", "MA")
        assertEquals(HoldingRowModel.Trade("b1", "2026-11-03", sell = false, shares = "1", oneShare = true, amount = "$100.00"), rows.first { it.transactionId == "b1" })
        assertEquals(HoldingRowModel.Trade("s1", "2026-11-04", sell = true, shares = "2.5", oneShare = false, amount = "$250.50"), rows.first())
    }

    @Test
    fun aSplit_showsItsRatio_andAReversal_namesItsOriginal() {
        val rows = HoldingRows.of(ma, "p1", "MA")
        assertEquals(HoldingRowModel.Split("x1", "2026-11-03", SplitRatio(1, 3)), rows.first { it.transactionId == "x1" })
        assertEquals(
            HoldingRowModel.Reversal("r1", "2026-11-03", originalType = TransactionType.BUY, originalDate = "2026-11-03"),
            rows.first { it.transactionId == "r1" },
        )
    }

    @Test
    fun onlyTheHoldingsOwnTransactions_areRows() {
        val other = transaction("o1", TransactionType.BUY, "2026-11-05", 8L, "1", 1_000L, ticker = "V")
        val elsewhere = transaction("o2", TransactionType.BUY, "2026-11-05", 9L, "1", 1_000L, portfolioId = "p2")
        assertEquals(5, HoldingRows.of(ma + other + elsewhere, "p1", "MA").size)
    }
}

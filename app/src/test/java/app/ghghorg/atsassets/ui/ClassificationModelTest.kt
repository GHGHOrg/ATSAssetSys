package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.ClassOrigin
import app.ghghorg.atsassets.core.EffectiveClass
import app.ghghorg.atsassets.core.SourceProfile
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** 23a: Holding detail's Classification tab model (view 6, wireframe 06 frame C; CR2; C9, C14). */
class ClassificationModelTest {
    private val lists = ClassLists(sectors = listOf("Technology"), geographies = listOf("US"))
    private val unclassified = EffectiveClass(null, ClassOrigin.UNCLASSIFIED)

    private fun model(
        profile: SourceProfile?,
        transactions: List<Transaction> = emptyList(),
    ): ClassificationModel = HoldingDetail.classification(profile, lists, transactions, "p1", "AAPL")

    private val buy =
        Transaction(
            id = "b1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.BUY,
            date = LocalDate.of(2026, 1, 2),
            enteredSeq = 2L,
            quantity = BigDecimal("10"),
            totalCents = 150_000L,
        )

    private fun split(
        id: String,
        date: LocalDate,
        ratio: SplitRatio,
        seq: Long,
        portfolioId: String = "p1",
        ticker: String = "AAPL",
    ): Transaction = Transaction(
        id = id,
        portfolioId = portfolioId,
        ticker = ticker,
        type = TransactionType.SPLIT,
        date = date,
        enteredSeq = seq,
        quantity = BigDecimal.ZERO,
        totalCents = 0L,
        split = ratio,
    )

    @Test
    fun theSourcesValues_showInTheListsSpelling_fromTheSource() {
        val shown = model(SourceProfile("technology", "US"))
        assertEquals(EffectiveClass("Technology", ClassOrigin.SOURCE), shown.sector)
        assertEquals(EffectiveClass("US", ClassOrigin.SOURCE), shown.geography)
    }

    @Test
    fun notAskedYet_orNoValue_isUnclassified() {
        assertEquals(unclassified, model(null).sector)
        assertEquals(unclassified, model(null).geography)
        val etf = model(SourceProfile(null, null))
        assertEquals(unclassified, etf.sector)
        assertEquals(unclassified, etf.geography)
        assertEquals(unclassified, model(SourceProfile(null, "US")).sector)
    }

    @Test
    fun theSplitHistory_isEverySplitOfTheHolding_newestFirst_aReversedOneMarked() {
        // Your B: a reversed split is listed and marked; another holding's split is not.
        val first = split("s1", LocalDate.of(2026, 3, 1), SplitRatio(2, 1), 3L)
        val second = split("s2", LocalDate.of(2026, 5, 1), SplitRatio(3, 1), 4L)
        val elsewhere = split("s3", LocalDate.of(2026, 4, 1), SplitRatio(2, 1), 5L, ticker = "MSFT")
        val reversal =
            Transaction(
                id = "r1",
                portfolioId = "p1",
                ticker = "AAPL",
                type = TransactionType.REVERSAL,
                date = LocalDate.of(2026, 3, 2),
                enteredSeq = 6L,
                quantity = BigDecimal.ZERO,
                totalCents = 0L,
                reversesId = "s1",
            )
        val splits = model(null, listOf(buy, first, second, elsewhere, reversal)).splits
        assertEquals(
            listOf(
                SplitHistoryRowModel("s2", "2026-05-01", SplitRatio(3, 1), reversed = false),
                SplitHistoryRowModel("s1", "2026-03-01", SplitRatio(2, 1), reversed = true),
            ),
            splits,
        )
    }

    @Test
    fun noSplit_givesAnEmptyHistory() {
        assertEquals(emptyList<SplitHistoryRowModel>(), model(null, listOf(buy)).splits)
    }
}

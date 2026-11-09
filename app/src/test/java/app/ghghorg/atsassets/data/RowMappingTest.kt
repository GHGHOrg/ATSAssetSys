package app.ghghorg.atsassets.data

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.UnreadableDataException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate

class RowMappingTest {
    private val buy =
        Transaction(
            id = "t1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.BUY,
            date = LocalDate.of(2026, 2, 1),
            enteredSeq = 4L,
            quantity = BigDecimal("2.50"),
            totalCents = 150_000L,
        )

    @Test
    fun transaction_isStoredAsText_andReadBackUnchanged() {
        val row = buy.toRow()
        assertEquals("2026-02-01", row.date)
        assertEquals("BUY", row.type)
        assertEquals(null, row.reversesId)
        assertEquals(buy, row.toTransaction())
    }

    @Test
    fun quantity_keepsItsExactTextAndScale() {
        assertEquals("2.50", buy.toRow().quantity)
        assertEquals(2, buy.toRow().toTransaction().quantity.scale())
        assertEquals("0.00000001", buy.copy(quantity = BigDecimal("0.00000001")).toRow().quantity)
        assertEquals("1234567.12345678", buy.copy(quantity = BigDecimal("1234567.12345678")).toRow().quantity)
    }

    @Test
    fun instrument_isStoredAndReadBackUnchanged() {
        val instrument = Instrument(ticker = "SPY", assetType = AssetType.ETF, verified = true)
        assertEquals("ETF", instrument.toRow().assetType)
        assertEquals(instrument, instrument.toRow().toInstrument())
    }

    private val spy = Instrument(ticker = "SPY", assetType = AssetType.ETF, verified = true)
    private val stalePrice = Price(BigDecimal("348.9100"), Instant.ofEpochSecond(1_793_304_000), stale = true)

    @Test
    fun instrument_withAPrice_storesItAsExactText_andReadsItBackUnchanged() {
        val row = spy.copy(price = stalePrice).toRow()
        assertEquals("348.9100", row.price)
        assertEquals(1_793_304_000L, row.priceTime)
        assertEquals(true, row.priceStale)
        assertEquals(spy.copy(price = stalePrice), row.toInstrument())
        assertEquals(4, row.toInstrument().price!!.perShare.scale())
        val fresh = Price(BigDecimal("0.0089"), Instant.ofEpochSecond(1_793_232_000))
        assertEquals(spy.copy(price = fresh), spy.copy(price = fresh).toRow().toInstrument())
    }

    @Test
    fun instrument_withoutAPrice_leavesThePriceColumnsEmpty() {
        val row = spy.toRow()
        assertEquals(null, row.price)
        assertEquals(null, row.priceTime)
        assertEquals(false, row.priceStale)
        assertEquals(spy, row.toInstrument())
    }

    @Test
    fun aDamagedStoredPrice_isUnreadable_withNoValueInTheMessage() {
        val damaged =
            listOf(
                spy.toRow().copy(price = "abc", priceTime = 1L),
                spy.toRow().copy(price = "0", priceTime = 1L),
                spy.toRow().copy(price = "-1.5", priceTime = 1L),
                spy.toRow().copy(price = "348.91", priceTime = 0L),
                spy.toRow().copy(price = "348.91", priceTime = null),
                spy.toRow().copy(price = null, priceTime = 1L),
                spy.toRow().copy(priceStale = true),
            )
        for (row in damaged) {
            val error = assertThrows(row.toString(), UnreadableDataException::class.java) { row.toInstrument() }
            assertFalse(row.toString(), error.message!!.contains("SPY") || error.message!!.contains("348"))
        }
    }

    @Test
    fun savingAnInstrumentAgain_keepsItsStoredPrice() {
        val stored = spy.copy(price = stalePrice).toRow()
        val again = spy.copy(assetType = AssetType.STOCK).toRow()
        assertEquals(again.copy(price = "348.9100", priceTime = 1_793_304_000L, priceStale = true), again.withPriceOf(stored))
        assertEquals(again, again.withPriceOf(null))
        assertEquals(again, again.withPriceOf(spy.toRow()))
    }

    private val laterBuy = buy.copy(id = "t2", date = LocalDate.of(2026, 2, 3), enteredSeq = 5L, quantity = BigDecimal("10"))

    private val sell =
        buy.copy(
            id = "s1",
            type = TransactionType.SELL,
            date = LocalDate.of(2026, 3, 1),
            enteredSeq = 6L,
            quantity = BigDecimal("3.5"),
            totalCents = 70_000L,
            allocations =
            listOf(
                SellAllocation("t1", Fraction.of(BigDecimal("2.5"))),
                SellAllocation("t2", Fraction.of(BigInteger.ONE, BigInteger.ONE)),
            ),
        )

    @Test
    fun sellAllocations_areStoredAsReducedFractions_andReadBackIntoTheirSell() {
        val rows = sell.allocationRows()
        assertEquals(listOf("s1", "s1"), rows.map { it.sellId })
        assertEquals(listOf("t1", "t2"), rows.map { it.lotId })
        assertEquals(listOf("5/2", "1/1"), rows.map { it.quantity })
        assertEquals("SELL", sell.toRow().type)
        val read = withAllocations(listOf(buy.toRow(), laterBuy.toRow(), sell.toRow()).map { it.toTransaction() }, rows)
        assertEquals(listOf(buy, laterBuy, sell), read)
        assertEquals(emptyList<SellAllocationRow>(), buy.allocationRows())
    }

    @Test
    fun sellAllocations_areReadBackOldestLotFirst_whateverTheRowOrder() {
        val read = withAllocations(listOf(buy, laterBuy, sell.copy(allocations = emptyList())), sell.allocationRows().reversed())
        assertEquals(listOf("t1", "t2"), read.last().allocations.map { it.lotId })
    }

    @Test
    fun aDamagedAllocation_aSellWithoutAllocations_orAnAllocationOfNoSell_isUnreadable() {
        val bare = listOf(buy, laterBuy, sell.copy(allocations = emptyList()))
        val damaged = sell.allocationRows().map { it.copy(quantity = "10/2") }
        assertThrows(UnreadableDataException::class.java) { withAllocations(bare, damaged) }
        assertThrows(UnreadableDataException::class.java) { withAllocations(bare, emptyList()) }
        val stray = sell.allocationRows() + SellAllocationRow(sellId = "s9", lotId = "t1", quantity = "1/1")
        assertThrows(UnreadableDataException::class.java) { withAllocations(bare, stray) }
    }

    private val split =
        buy.copy(
            id = "x1",
            type = TransactionType.SPLIT,
            date = LocalDate.of(2026, 3, 1),
            enteredSeq = 7L,
            quantity = BigDecimal.ZERO,
            totalCents = 0L,
            split = SplitRatio(1, 3),
        )

    @Test
    fun aSplit_keepsItsRatioInTwoColumns_andEveryOtherRowLeavesThemEmpty() {
        val row = split.toRow()
        assertEquals("SPLIT", row.type)
        assertEquals(1, row.splitTo)
        assertEquals(3, row.splitFrom)
        assertEquals(split, row.toTransaction())
        assertEquals(null, buy.toRow().splitTo)
        assertEquals(null, sell.toRow().splitFrom)
        assertEquals(1_000_000, split.copy(split = SplitRatio(1_000_000, 1)).toRow().toTransaction().split?.to)
    }

    @Test
    fun aSplitWithoutAnAllowedRatio_orABuyOrSellWithOne_isUnreadable() {
        val row = split.toRow()
        val damaged =
            listOf(
                row.copy(splitTo = null),
                row.copy(splitFrom = null),
                row.copy(splitTo = 0),
                row.copy(splitFrom = -1),
                row.copy(splitTo = 1_000_001),
                row.copy(splitTo = 3),
                buy.toRow().copy(splitTo = 2),
                buy.toRow().copy(splitFrom = 3),
                buy.toRow().copy(splitTo = 0, splitFrom = 3),
                buy.toRow().copy(splitTo = 1, splitFrom = -3),
                buy.toRow().copy(splitTo = 3, splitFrom = 3),
                sell.toRow().copy(splitFrom = 1),
                sell.toRow().copy(splitTo = 1, splitFrom = 3),
            )
        damaged.forEach { assertThrows(UnreadableDataException::class.java) { it.toTransaction() } }
    }

    // 14c (CR1): a buy re-entered with its quantity left as filled in keeps its quantity ratio in the split columns.
    @Test
    fun aBuyWithAQuantityRatio_keepsItInTheSplitColumns_andIsReadBackUnchanged() {
        val kept = buy.copy(quantity = BigDecimal("1"), quantityRatio = SplitRatio(1, 3))
        val row = kept.toRow()
        assertEquals(1, row.splitTo)
        assertEquals(3, row.splitFrom)
        assertEquals(kept, row.toTransaction())
        assertEquals(null, row.toTransaction().split)
        val large = kept.copy(quantityRatio = SplitRatio(1, Int.MAX_VALUE))
        assertEquals(large, large.toRow().toTransaction())
    }

    private val reversal =
        buy.copy(
            id = "r1",
            type = TransactionType.REVERSAL,
            date = LocalDate.of(2026, 3, 2),
            enteredSeq = 8L,
            quantity = BigDecimal.ZERO,
            totalCents = 0L,
            reversesId = "t1",
        )

    @Test
    fun aReversal_keepsOnlyItsLink_andIsReadBackUnchanged() {
        val row = reversal.toRow()
        assertEquals("REVERSAL", row.type)
        assertEquals("t1", row.reversesId)
        assertEquals("0", row.quantity)
        assertEquals(0L, row.totalCents)
        assertEquals(null, row.splitTo)
        assertEquals(reversal, row.toTransaction())
        assertEquals(listOf(buy, reversal), checkReversals(listOf(buy, reversal)))
    }

    @Test
    fun aReversalWithoutItsLink_orABuyWithOne_isUnreadable() {
        assertThrows(UnreadableDataException::class.java) { reversal.toRow().copy(reversesId = null).toTransaction() }
        assertThrows(UnreadableDataException::class.java) { buy.toRow().copy(reversesId = "t9").toTransaction() }
    }

    @Test
    fun aReversalOfNothing_ofAReversal_aSecondOne_oneDatedBeforeItsOriginal_orInAnotherHolding_isUnreadable() {
        val damaged =
            listOf(
                listOf(reversal),
                listOf(buy, reversal, reversal.copy(id = "r2", reversesId = "r1")),
                listOf(buy, reversal, reversal.copy(id = "r2", enteredSeq = 9L)),
                listOf(buy, reversal.copy(date = LocalDate.of(2026, 1, 31))),
                listOf(buy, reversal.copy(ticker = "SPY")),
                listOf(buy, reversal.copy(portfolioId = "p2")),
            )
        damaged.forEach { assertThrows(UnreadableDataException::class.java) { checkReversals(it) } }
    }

    // 14a: a Reverse and re-enter pair is stored as a reversal and a normal entry; the new entry has no link (4.6).
    @Test
    fun aBuyPair_withTheNewBuyDatedBeforeItsReversal_isReadBackUnchanged() {
        val later = reversal.copy(date = LocalDate.of(2026, 3, 3))
        val newBuy = buy.copy(id = "n1", enteredSeq = 9L, quantity = BigDecimal("100"))
        val rows = listOf(buy, later, newBuy).map { it.toRow() }
        assertEquals(null, rows.last().reversesId)
        assertEquals(listOf(buy, later, newBuy), checkReversals(withAllocations(rows.map { it.toTransaction() }, emptyList())))
        assertEquals(emptyList<SellAllocationRow>(), later.allocationRows())
    }

    // 14b: a split's pair is a reversal of the split and a new split, neither with a cash line or allocations.
    @Test
    fun aSplitPair_isReadBackUnchanged_throughTheStoredChecks() {
        val splitReversal = reversal.copy(id = "r3", reversesId = "x1", date = LocalDate.of(2026, 3, 1), enteredSeq = 9L)
        val newSplit = split.copy(id = "n3", enteredSeq = 10L, split = SplitRatio(1, 2))
        val transactions = listOf(buy, split, splitReversal, newSplit)
        assertEquals(emptyList<SellAllocationRow>(), newSplit.allocationRows())
        assertEquals(null, newSplit.toRow().reversesId)
        val read = checkReversals(withAllocations(transactions.map { it.toRow().toTransaction() }, emptyList()))
        assertEquals(transactions, read)
    }

    @Test
    fun aSellPair_isReadBackWithTheOriginalsAndTheNewSellsAllocations_eachInItsOwnSell() {
        val sellReversal = reversal.copy(id = "r2", reversesId = "s1", date = LocalDate.of(2026, 3, 1), enteredSeq = 9L)
        val newSell =
            sell.copy(
                id = "n2",
                enteredSeq = 10L,
                totalCents = 80_000L,
                allocations = listOf(SellAllocation("t1", Fraction.of(BigInteger.valueOf(5), BigInteger.valueOf(3)))),
            )
        val transactions = listOf(buy, laterBuy, sell, sellReversal, newSell)
        val rows = sell.allocationRows() + newSell.allocationRows()
        assertEquals(listOf("5/3"), newSell.allocationRows().map { it.quantity })
        val read = checkReversals(withAllocations(transactions.map { it.toRow().toTransaction() }, rows))
        assertEquals(transactions, read)
    }

    // 15: a hidden holding is a portfolio id and a ticker (section 8's hidden row).
    @Test
    fun aHiddenHolding_isStoredAsItsPortfolioAndTicker_andReadBackUnchanged() {
        val hidden = HiddenHolding("p1", "AAPL")
        val row = hidden.toRow()
        assertEquals("p1", row.portfolioId)
        assertEquals("AAPL", row.ticker)
        assertEquals(hidden, row.toHidden())
    }

    private val soldOut =
        sell.copy(
            id = "s2",
            quantity = BigDecimal("2.50"),
            allocations = listOf(SellAllocation("t1", Fraction.of(BigDecimal("2.50")))),
        )

    // A hidden holding always has 0 shares, counting every entry (4.4, section 8; 15 decision 1): one with shares,
    // or with no transaction at all, is damaged data and is not guessed at.
    @Test
    fun aHiddenHoldingAtZero_isReadable_andOneWithShares_orWithNoHolding_isUnreadable() {
        val hidden = listOf(HiddenHolding("p1", "AAPL"))
        assertEquals(hidden, checkHidden(listOf(buy, soldOut), hidden))
        assertThrows(UnreadableDataException::class.java) { checkHidden(listOf(buy), hidden) }
        assertThrows(UnreadableDataException::class.java) { checkHidden(listOf(buy, soldOut), listOf(HiddenHolding("p1", "MSFT"))) }
    }

    // 20: the auto-lock choice in the settings row (decision 3).
    @Test
    fun noSettingsRow_isTheDefault_everyLeave() {
        val none: SettingsRow? = null
        assertEquals(AutoLock.EVERY_LEAVE, none.toAutoLock())
    }

    @Test
    fun eachAutoLockChoice_isStoredByName_andReadBack() {
        for (choice in AutoLock.entries) {
            assertEquals(choice, SettingsRow(autoLock = choice.name).toAutoLock())
        }
    }

    @Test
    fun anUnknownAutoLockChoice_isUnreadable_notTheDefault() {
        for (text in listOf("", "NEVER", "idle_1_min", "IDLE_10_MIN")) {
            assertThrows(text, UnreadableDataException::class.java) { SettingsRow(autoLock = text).toAutoLock() }
        }
    }
}

package app.ghghorg.atsassets.data

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.BackupRecords
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.ClassOverride
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.RememberedText
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.TabOrder
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

// 34 step 2 (K2; spec section 8, restore step 7; decisions 1 and 4): the rows a restore writes in place of everything
// stored: the file's records as K1 made them, its lists with the texts and overrides naming their values, its settings
// with no price key and no backup moment, and the held tickers' instruments with no price.
class RestoreRowsTest {
    private val day = LocalDate.parse("2026-01-02")

    private val buy = Transaction("b1", "p1", "AAPL", TransactionType.BUY, day, 2L, BigDecimal.TEN, 100_000L)

    private val sell =
        Transaction("s1", "p1", "AAPL", TransactionType.SELL, day.plusDays(1), 3L, BigDecimal("4"), 50_000L)
            .copy(allocations = listOf(SellAllocation("b1", Fraction.of(BigDecimal("4")))))

    private val records =
        BackupRecords(
            cashEntries =
            listOf(
                CashEntry("o1", LocalDate.parse("2026-01-01"), 1_000_000L, CashType.OPENING_BALANCE, "", 1L),
                CashEntry("c-b1", day, -100_000L, CashType.TRADE, "", 2L, transactionId = "b1"),
                CashEntry("c-s1", day.plusDays(1), 50_000L, CashType.TRADE, "", 3L, transactionId = "s1"),
            ),
            portfolios = listOf(Portfolio("p1", "Main", 1L)),
            transactions = listOf(buy, sell),
            hidden = listOf(HiddenHolding("p1", "OLDCO")),
            overrides = listOf(ClassOverride("p1", "AAPL", ClassList.GEOGRAPHY, "United States")),
            lists =
            ClassLists(
                sectors = listOf("Technology", "Diversified"),
                geographies = listOf("United States"),
                remembered = listOf(RememberedText(ClassList.SECTOR, "Information Technology", "Diversified")),
            ),
            autoLock = AutoLock.IDLE_5_MIN,
            tabOrder = requireNotNull(TabOrder.parse("overall;portfolios;cash;more")),
            priceSource = "finnhub",
        )

    // An instrument the app had verified before, with a price: only its ticker and type are stored ("no price" until the
    // first refresh, section 8 step 7).
    private val priced =
        Instrument("AAPL", AssetType.STOCK, verified = true, price = Price(BigDecimal("190.5"), Instant.ofEpochSecond(1_700_000_000), stale = false))

    @Test
    fun theRecords_becomeTheirRows() {
        val rows = restoreRows(records, listOf(priced))
        assertEquals(listOf("o1", "c-b1", "c-s1"), rows.cash.map { it.id })
        assertEquals(listOf(PortfolioRow("p1", "Main", 1L)), rows.portfolios)
        assertEquals(listOf("b1", "s1"), rows.transactions.map { it.id })
        // The quantity in its stored form, a reduced fraction (11 decision 3), as every saved sell's.
        assertEquals(listOf(SellAllocationRow("s1", "b1", "4/1")), rows.allocations)
        assertEquals(listOf(HiddenHoldingRow("p1", "OLDCO")), rows.hidden)
    }

    // The list rows get their own numbers, and the texts and overrides name the row of their value (24 decision 3; 25).
    @Test
    fun theLists_theirTexts_andTheOverrides_nameTheSameRows() {
        val rows = restoreRows(records, emptyList())
        assertEquals(
            listOf(ListValueRow(1L, "SECTOR", "Technology"), ListValueRow(2L, "SECTOR", "Diversified"), ListValueRow(3L, "GEOGRAPHY", "United States")),
            rows.listValues,
        )
        assertEquals(listOf(SourceTextRow(1L, "SECTOR", 2L, "Information Technology")), rows.sourceTexts)
        assertEquals(listOf(ClassOverrideRow("p1", "AAPL", "GEOGRAPHY", 3L)), rows.overrides)
        // What the store reads back from these rows is what the file held.
        assertEquals(records.lists, rows.listValues.toClassLists(rows.sourceTexts))
        assertEquals(records.overrides, rows.overrides.toOverrides(rows.listValues))
    }

    // Decision 1: no price key; the last backup reset and no mark (section 8 step 7); the file's lock and tab order.
    @Test
    fun theSettings_areTheFiles_withNoKeyAndNoBackup() {
        val settings = restoreRows(records, emptyList()).settings
        assertEquals(AutoLock.IDLE_5_MIN, settings.toAutoLock())
        assertEquals("overall;portfolios;cash;more", settings.toTabOrder().text)
        assertNull(settings.priceKey)
        assertNull(settings.lastBackupAt)
        assertNull(settings.backupUnfinished)
    }

    @Test
    fun theInstruments_areStoredVerified_withNoPrice() {
        val rows = restoreRows(records, listOf(priced))
        assertEquals(listOf(InstrumentRow(ticker = "AAPL", assetType = "STOCK", verified = true)), rows.instruments)
    }

    // An override naming a value its list does not hold cannot be written; the check refuses such a file before this.
    @Test
    fun anOverrideNamingNoListValue_isRefused() {
        val bad = records.copy(overrides = listOf(ClassOverride("p1", "AAPL", ClassList.SECTOR, "Energy")))
        assertThrows(IllegalArgumentException::class.java) { restoreRows(bad, emptyList()) }
    }
}

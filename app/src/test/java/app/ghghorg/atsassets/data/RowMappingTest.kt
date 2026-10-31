package app.ghghorg.atsassets.data

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
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
}

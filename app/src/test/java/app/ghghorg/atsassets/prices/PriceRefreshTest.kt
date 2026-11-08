package app.ghghorg.atsassets.prices

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Lookup
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.PriceSource
import app.ghghorg.atsassets.core.Quote
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.FakeStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

// 16a: which tickers a refresh asks for (decision 3) and what each answer means (spec 7, 4.8).
class PriceRefreshTest {
    private fun bought(
        id: String,
        portfolioId: String,
        ticker: String,
    ): Transaction = Transaction(
        id = id,
        portfolioId = portfolioId,
        ticker = ticker,
        type = TransactionType.BUY,
        date = LocalDate.of(2026, 1, 2),
        enteredSeq = id.length.toLong(),
        quantity = BigDecimal("1"),
        totalCents = 100L,
    )

    private class Source(
        val quotes: Map<String, Quote>,
    ) : PriceSource {
        val asked = mutableListOf<String>()

        override suspend fun lookUp(ticker: String): Lookup = error("not used")

        override suspend fun quote(ticker: String): Quote {
            asked.add(ticker)
            return quotes[ticker] ?: Quote.NoQuote
        }
    }

    private val at = Instant.ofEpochSecond(1_793_304_000L)

    private fun priced(text: String): Quote = Quote.Priced(BigDecimal(text), at)

    @Test
    fun tickers_areEveryTickerHeldInAPortfolioWhereItIsNotHidden_inTickerOrder() {
        val transactions =
            listOf(
                bought("1", "p1", "MSFT"),
                bought("2", "p1", "AAPL"),
                bought("3", "p1", "KO"),
                bought("4", "p2", "KO"),
                bought("5", "p1", "OLD"),
                bought("6", "p1", "AAPL"),
            )
        val hidden = listOf(HiddenHolding("p1", "KO"), HiddenHolding("p1", "OLD"))
        // KO is hidden in p1 but held in p2, so it is still asked for; OLD is hidden in its only portfolio.
        assertEquals(listOf("AAPL", "KO", "MSFT"), PriceRefresh.tickers(transactions, hidden))
        assertEquals(emptyList<String>(), PriceRefresh.tickers(emptyList(), emptyList()))
    }

    @Test
    fun run_online_keepsEachPrice_andAnUnpricedTickerFails_withoutFailingTheRefresh() = runTest {
        val source = Source(mapOf("AAPL" to priced("160"), "MSFT" to priced("400.25")))
        val outcome = PriceRefresh.run(listOf("AAPL", "DEAD", "MSFT"), online = true, source = source)
        assertEquals(listOf("AAPL", "DEAD", "MSFT"), source.asked)
        assertEquals(mapOf("AAPL" to Price(BigDecimal("160"), at), "MSFT" to Price(BigDecimal("400.25"), at)), outcome.fresh)
        assertEquals(listOf("DEAD"), outcome.failed)
        assertEquals(false, outcome.refreshFailed)
    }

    @Test
    fun run_offline_asksNothing_andFailsEveryTicker() = runTest {
        val source = Source(mapOf("AAPL" to priced("160")))
        val outcome = PriceRefresh.run(listOf("AAPL", "MSFT"), online = false, source = source)
        assertEquals(emptyList<String>(), source.asked)
        assertEquals(emptyMap<String, Price>(), outcome.fresh)
        assertEquals(listOf("AAPL", "MSFT"), outcome.failed)
        assertEquals(true, outcome.refreshFailed)
    }

    @Test
    fun run_withNothingToAsk_isNotAFailure_evenOffline() = runTest {
        assertEquals(PriceRefresh.Outcome(emptyMap(), emptyList(), refreshFailed = false), PriceRefresh.run(emptyList(), false, Source(emptyMap())))
    }

    @Test
    fun run_stopsAtAnAnswerThatWillNotChangeForTheNextTicker_andFails() = runTest {
        for (stop in listOf(Quote.NoKey, Quote.KeyRefused(401), Quote.TooManyCalls, Quote.NoAnswer(status = null))) {
            val source = Source(mapOf("AAPL" to priced("160"), "KO" to stop, "MSFT" to priced("400")))
            val outcome = PriceRefresh.run(listOf("AAPL", "KO", "MSFT"), online = true, source = source)
            assertEquals(stop.toString(), listOf("AAPL", "KO"), source.asked)
            assertEquals(stop.toString(), setOf("AAPL"), outcome.fresh.keys)
            assertEquals(stop.toString(), listOf("KO", "MSFT"), outcome.failed)
            assertEquals(stop.toString(), true, outcome.refreshFailed)
        }
    }

    @Test
    fun run_goesOnAfterAnAnswerForOneTickerOnly_andFails() = runTest {
        for (other in listOf(Quote.Unreadable, Quote.NoAnswer(status = 500))) {
            val source = Source(mapOf("AAPL" to priced("160"), "KO" to other, "MSFT" to priced("400")))
            val outcome = PriceRefresh.run(listOf("AAPL", "KO", "MSFT"), online = true, source = source)
            assertEquals(other.toString(), listOf("AAPL", "KO", "MSFT"), source.asked)
            assertEquals(other.toString(), setOf("AAPL", "MSFT"), outcome.fresh.keys)
            assertEquals(other.toString(), listOf("KO"), outcome.failed)
            assertEquals(other.toString(), true, outcome.refreshFailed)
        }
    }

    // Deploy finding L1: K4 writes the prices itself (section 14), as TickerVerifier writes instruments.
    @Test
    fun refresh_savesItsOwnOutcome_once() = runTest {
        val store = FakeStore()
        val source = Source(mapOf("AAPL" to priced("160")))
        val outcome = PriceRefresh.refresh(listOf("AAPL", "DEAD"), online = true, source = source, store = store)
        assertEquals(listOf(mapOf("AAPL" to Price(BigDecimal("160"), at)) to listOf("DEAD")), store.savedPrices)
        assertEquals(PriceRefresh.Outcome(mapOf("AAPL" to Price(BigDecimal("160"), at)), listOf("DEAD"), refreshFailed = false), outcome)
    }

    @Test
    fun refresh_offline_savesEveryTickerAsFailed() = runTest {
        val store = FakeStore()
        PriceRefresh.refresh(listOf("AAPL", "MSFT"), online = false, source = Source(emptyMap()), store = store)
        assertEquals(listOf(emptyMap<String, Price>() to listOf("AAPL", "MSFT")), store.savedPrices)
    }

    @Test
    fun applied_setsFreshPrices_andMarksTheFailedOnesStale_asTheStoreDoes() {
        val old = Price(BigDecimal("150"), Instant.ofEpochSecond(1_000L))
        val instruments =
            listOf(
                Instrument("AAPL", AssetType.STOCK, true, old),
                Instrument("KO", AssetType.STOCK, true, old),
                Instrument("NEW", AssetType.STOCK, true),
                Instrument("SPY", AssetType.ETF, true, old),
            )
        val fresh = Price(BigDecimal("160"), at)
        val outcome = PriceRefresh.Outcome(mapOf("AAPL" to fresh), listOf("KO", "NEW"), refreshFailed = false)
        assertEquals(
            listOf(
                Instrument("AAPL", AssetType.STOCK, true, fresh),
                Instrument("KO", AssetType.STOCK, true, old.copy(stale = true)),
                Instrument("NEW", AssetType.STOCK, true),
                Instrument("SPY", AssetType.ETF, true, old),
            ),
            PriceRefresh.applied(instruments, outcome),
        )
    }
}

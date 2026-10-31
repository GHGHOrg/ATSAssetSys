package app.ghghorg.atsassets.prices

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Listing
import app.ghghorg.atsassets.core.Lookup
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.PriceSource
import app.ghghorg.atsassets.core.RecordStore
import app.ghghorg.atsassets.core.TickerRefusal
import app.ghghorg.atsassets.core.Transaction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TickerVerifierTest {
    private class Source(
        val answer: Lookup,
    ) : PriceSource {
        val asked = mutableListOf<String>()

        override suspend fun lookUp(ticker: String): Lookup {
            asked.add(ticker)
            return answer
        }
    }

    /** Only instruments are used here; the other calls are not expected. */
    private class Store : RecordStore {
        val savedInstruments = mutableListOf<Instrument>()

        override suspend fun saveInstrument(instrument: Instrument) {
            savedInstruments.add(instrument)
        }

        override suspend fun loadCashEntries(): List<CashEntry> = error("not used")

        override suspend fun saveCashEntries(entries: List<CashEntry>) = error("not used")

        override suspend fun loadPortfolios(): List<Portfolio> = error("not used")

        override suspend fun savePortfolio(portfolio: Portfolio) = error("not used")

        override suspend fun renamePortfolio(
            id: String,
            name: String,
        ) = error("not used")

        override suspend fun loadTransactions(): List<Transaction> = error("not used")

        override suspend fun saveBuy(
            transaction: Transaction,
            cashLine: CashEntry,
        ) = error("not used")

        override suspend fun loadInstruments(): List<Instrument> = error("not used")
    }

    private val aapl = Listing(type = "Common Stock", exchange = "XNAS", currency = "USD")

    private suspend fun verify(
        answer: Lookup,
        online: Boolean = true,
        instruments: List<Instrument> = emptyList(),
        ticker: String = "AAPL",
    ): Triple<Verification, Source, Store> {
        val source = Source(answer)
        val store = Store()
        return Triple(TickerVerifier.verify(ticker, instruments, online, source, store), source, store)
    }

    @Test
    fun anAcceptedTicker_isVerified_andSavedAsAnInstrument() = runTest {
        val (result, source, store) = verify(Lookup.Found(aapl))
        val instrument = Instrument("AAPL", AssetType.STOCK, verified = true)
        assertEquals(Verification.Verified(instrument), result)
        assertEquals(listOf("AAPL"), source.asked)
        assertEquals(listOf(instrument), store.savedInstruments)
    }

    @Test
    fun anEtp_isSavedAsAnEtf() = runTest {
        val (result, _, store) = verify(Lookup.Found(Listing("ETP", "ARCX", "USD")), ticker = "SPY")
        assertEquals(Verification.Verified(Instrument("SPY", AssetType.ETF, verified = true)), result)
        assertEquals(AssetType.ETF, store.savedInstruments.single().assetType)
    }

    @Test
    fun aVerifiedTicker_isNotCheckedAgain_evenOffline() = runTest {
        val known = Instrument("AAPL", AssetType.STOCK, verified = true)
        val (result, source, store) = verify(Lookup.NoAnswer(null), online = false, instruments = listOf(known))
        assertEquals(Verification.Verified(known), result)
        assertEquals(emptyList<String>(), source.asked)
        assertEquals(emptyList<Instrument>(), store.savedInstruments)
    }

    @Test
    fun offline_isRefused_withoutAsking() = runTest {
        val (result, source, _) = verify(Lookup.Found(aapl), online = false)
        assertEquals(Verification.Refused(CheckRefusal.Offline), result)
        assertEquals(emptyList<String>(), source.asked)
    }

    @Test
    fun eachLookupThatFails_isItsOwnRefusal_andSavesNothing() = runTest {
        val cases =
            listOf(
                Lookup.NotFound to CheckRefusal.Ticker(TickerRefusal.NotFound),
                Lookup.Unreadable to CheckRefusal.Unreadable,
                Lookup.KeyRefused(401) to CheckRefusal.KeyRefused(401),
                Lookup.KeyRefused(403) to CheckRefusal.KeyRefused(403),
                Lookup.TooManyCalls to CheckRefusal.TooManyCalls,
                Lookup.NoAnswer(503) to CheckRefusal.NoAnswer(503),
                Lookup.NoAnswer(null) to CheckRefusal.NoAnswer(null),
                Lookup.NoKey to CheckRefusal.NoKey,
            )
        for ((answer, expected) in cases) {
            val (result, _, store) = verify(answer)
            assertEquals(answer.toString(), Verification.Refused(expected), result)
            assertEquals(answer.toString(), emptyList<Instrument>(), store.savedInstruments)
        }
    }

    @Test
    fun aListingTheCheckRefuses_isRefusedWithTheCheckReason() = runTest {
        val cases =
            listOf(
                Listing("Common Stock", "OOTC", "USD") to TickerRefusal.NotUsExchange("OOTC"),
                Listing("Open-End Fund", "XNAS", "USD") to TickerRefusal.MutualFund,
                Listing("Common Stock", "XNAS", "EUR") to TickerRefusal.NotUsd("EUR"),
            )
        for ((listing, expected) in cases) {
            val (result, _, store) = verify(Lookup.Found(listing))
            assertEquals(listing.toString(), Verification.Refused(CheckRefusal.Ticker(expected)), result)
            assertEquals(emptyList<Instrument>(), store.savedInstruments)
        }
    }
}

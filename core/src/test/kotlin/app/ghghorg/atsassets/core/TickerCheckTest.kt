package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TickerCheckTest {
    private fun listing(
        type: String? = "Common Stock",
        mic: String? = "XNAS",
        currency: String? = "USD",
    ): Listing = Listing(type = type, exchange = mic, currency = currency)

    private fun check(listing: Listing?): TickerVerdict = TickerCheck.verdict(listing)

    @Test
    fun theSixListedMicCodes_areAccepted() {
        for (mic in listOf("XNAS", "XNYS", "ARCX", "BATS", "XASE", "IEXG")) {
            assertEquals(mic, TickerVerdict.Accepted(AssetType.STOCK), check(listing(mic = mic)))
        }
    }

    @Test
    fun aMicCode_isMatchedIgnoringCaseAndSpaces() {
        assertEquals(TickerVerdict.Accepted(AssetType.STOCK), check(listing(mic = " xnas ")))
    }

    @Test
    fun theOtcMarket_isRefused_namingWhatTheSourceReported() {
        assertEquals(TickerVerdict.Refused(TickerRefusal.NotUsExchange("OOTC")), check(listing(mic = "OOTC")))
    }

    @Test
    fun anUnknownCode_isRefused() {
        assertEquals(TickerVerdict.Refused(TickerRefusal.NotUsExchange("XNGS")), check(listing(mic = "XNGS")))
        assertEquals(TickerVerdict.Refused(TickerRefusal.NotUsExchange("OTCM")), check(listing(mic = "OTCM")))
    }

    @Test
    fun anSecName_orAFormerName_isAccepted_ignoringCaseAndSpaces() {
        assertEquals(TickerVerdict.Accepted(AssetType.STOCK), check(listing(mic = "NYSE Arca, Inc.")))
        assertEquals(TickerVerdict.Accepted(AssetType.STOCK), check(listing(mic = "  nyse   arca, INC. ")))
        assertEquals(TickerVerdict.Accepted(AssetType.STOCK), check(listing(mic = "NYSE MKT LLC")))
    }

    @Test
    fun noExchangeStated_isAccepted() {
        assertEquals(TickerVerdict.Accepted(AssetType.STOCK), check(listing(mic = null)))
        assertEquals(TickerVerdict.Accepted(AssetType.STOCK), check(listing(mic = " ")))
    }

    @Test
    fun anOpenEndFund_isRefusedAsAMutualFund() {
        assertEquals(TickerVerdict.Refused(TickerRefusal.MutualFund), check(listing(type = "Open-End Fund")))
        assertEquals(TickerVerdict.Refused(TickerRefusal.MutualFund), check(listing(type = "open-end  fund")))
    }

    @Test
    fun aPriceCurrencyOtherThanUsd_isRefused_namingIt() {
        assertEquals(TickerVerdict.Refused(TickerRefusal.NotUsd("EUR")), check(listing(currency = "EUR")))
    }

    @Test
    fun usdInAnyCase_orNoCurrencyStated_isAccepted() {
        assertEquals(TickerVerdict.Accepted(AssetType.STOCK), check(listing(currency = "usd")))
        assertEquals(TickerVerdict.Accepted(AssetType.STOCK), check(listing(currency = null)))
        assertEquals(TickerVerdict.Accepted(AssetType.STOCK), check(listing(currency = "")))
    }

    @Test
    fun notFound_isRefused() {
        assertEquals(TickerVerdict.Refused(TickerRefusal.NotFound), check(null))
    }

    @Test
    fun etp_isAnEtf() {
        assertEquals(TickerVerdict.Accepted(AssetType.ETF), check(listing(type = "ETP", mic = "ARCX")))
        assertEquals(TickerVerdict.Accepted(AssetType.ETF), check(listing(type = "etp")))
    }

    @Test
    fun everyOtherType_includingNone_isAStock() {
        for (type in listOf("Common Stock", "ADR", "REIT", "Closed-End Fund", "Equity WRT", "Unit", "", null)) {
            assertEquals(type.toString(), TickerVerdict.Accepted(AssetType.STOCK), check(listing(type = type)))
        }
    }

    @Test
    fun theFirstRuleThatFails_decides_mutualFundThenExchangeThenCurrency() {
        assertEquals(TickerVerdict.Refused(TickerRefusal.MutualFund), check(Listing("Open-End Fund", "OOTC", "EUR")))
        assertEquals(TickerVerdict.Refused(TickerRefusal.NotUsExchange("OOTC")), check(Listing("Common Stock", "OOTC", "EUR")))
    }

    @Test
    fun theTable_holdsThe29SecNames_andMapsTheSixCodes() {
        assertEquals(29, TickerCheck.EXCHANGES.size)
        assertEquals("The Nasdaq Stock Market", TickerCheck.secName("XNAS"))
        assertEquals("New York Stock Exchange LLC", TickerCheck.secName("XNYS"))
        assertEquals("NYSE Arca, Inc.", TickerCheck.secName("ARCX"))
        assertEquals("Cboe BZX Exchange, Inc.", TickerCheck.secName("BATS"))
        assertEquals("NYSE American LLC", TickerCheck.secName("XASE"))
        assertEquals("Investors Exchange LLC", TickerCheck.secName("IEXG"))
        assertEquals(null, TickerCheck.secName("OOTC"))
    }

    @Test
    fun noAlias_belongsToTwoExchanges() {
        val keys = TickerCheck.EXCHANGES.flatMap { (name, aliases) -> (aliases + name).map(Portfolios::nameKey) }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun normalize_trimsAndCapitalizes_andRefusesEmpty() {
        assertEquals("BRK.B", TickerCheck.normalize("  brk.b "))
        assertEquals(null, TickerCheck.normalize("   "))
    }
}

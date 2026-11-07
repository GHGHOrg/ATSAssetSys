package app.ghghorg.atsassets.prices.finnhub

import app.ghghorg.atsassets.core.Listing
import app.ghghorg.atsassets.core.Lookup
import app.ghghorg.atsassets.core.Quote
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Rows shaped like Finnhub's /stock/symbol?exchange=US answer of 2026-10-30 (price-source note 10.2), with the
 * figi fields left out to keep the lines short; the reader ignores them.
 */
private const val US_LIST =
    """[
  {"currency":"USD","description":"APPLE INC","displaySymbol":"AAPL","isin":null,"mic":"XNAS","symbol":"AAPL","type":"Common Stock"},
  {"currency":"USD","description":"SPDR S&P 500 ETF TRUST","displaySymbol":"SPY","isin":null,"mic":"ARCX","symbol":"SPY","type":"ETP"},
  {"currency":"USD","description":"ASIA BROADBAND INC","displaySymbol":"AABB","isin":null,"mic":"OOTC","symbol":"AABB","type":"Common Stock"},
  {"currency":"","description":"NO TYPE OR CURRENCY","displaySymbol":"NTNC","isin":null,"mic":"XNYS","symbol":"NTNC","type":""},
  {"description":"FIELDS MISSING","displaySymbol":"MISS","symbol":"MISS"},
  {"currency":"USD","description":"BERKSHIRE HATHAWAY","displaySymbol":"BRK.B","isin":null,"mic":"XNYS","symbol":"BRK.B","type":"Common Stock"}
]"""

class FinnhubSourceTest {
    private fun find(ticker: String): Listing? = FinnhubList.find(ByteArrayInputStream(US_LIST.toByteArray()), ticker)

    @Test
    fun find_returnsTheTypeMicAndCurrencyOfTheRow() {
        assertEquals(Listing(type = "Common Stock", exchange = "XNAS", currency = "USD"), find("AAPL"))
        assertEquals(Listing(type = "ETP", exchange = "ARCX", currency = "USD"), find("SPY"))
        assertEquals(Listing(type = "Common Stock", exchange = "OOTC", currency = "USD"), find("AABB"))
        assertEquals(Listing(type = "Common Stock", exchange = "XNYS", currency = "USD"), find("BRK.B"))
    }

    @Test
    fun find_keepsEmptyFieldsEmpty_andMissingFieldsNull() {
        assertEquals(Listing(type = "", exchange = "XNYS", currency = ""), find("NTNC"))
        assertEquals(Listing(type = null, exchange = null, currency = null), find("MISS"))
    }

    @Test
    fun find_aTickerNotListed_isNull() {
        assertEquals(null, find("AAPPL"))
        assertEquals(null, find("aapl"))
    }

    private fun source(
        key: String = "test-key",
        connection: FakeConnection = FakeConnection(200, US_LIST),
    ): Pair<FinnhubSource, FakeConnection> = FinnhubSource(key) { url ->
        connection.also { it.opened = url }
    } to connection

    @Test
    fun lookUp_found_sendsTheKeyInTheHeaderOnly() = runTest {
        val (source, connection) = source()
        assertEquals(Lookup.Found(Listing("ETP", "ARCX", "USD")), source.lookUp("SPY"))
        assertEquals("https://finnhub.io/api/v1/stock/symbol?exchange=US", connection.opened.toString())
        assertEquals("test-key", connection.getRequestProperty("X-Finnhub-Token"))
        assertFalse(connection.opened.toString().contains("test-key"))
        assertEquals("GET", connection.requestMethod)
        assertTrue(connection.connectTimeout > 0)
        assertTrue(connection.readTimeout > 0)
        assertTrue(connection.disconnected)
    }

    @Test
    fun lookUp_notListed_isNotFound() = runTest {
        assertEquals(Lookup.NotFound, source().first.lookUp("AAPPL"))
    }

    @Test
    fun lookUp_withAnEmptyKey_isNoKey_andConnectsNowhere() = runTest {
        var connected = false
        val source = FinnhubSource("") {
            connected = true
            FakeConnection(200, US_LIST)
        }
        assertEquals(Lookup.NoKey, source.lookUp("AAPL"))
        assertFalse(connected)
    }

    private suspend fun lookUpWithStatus(code: Int): Lookup {
        val (source, connection) = source(connection = FakeConnection(code, """{"error":"x"}"""))
        val result = source.lookUp("AAPL")
        assertTrue(connection.disconnected)
        return result
    }

    @Test
    fun lookUp_status401or403_isKeyRefused_withTheStatus() = runTest {
        assertEquals(Lookup.KeyRefused(401), lookUpWithStatus(401))
        assertEquals(Lookup.KeyRefused(403), lookUpWithStatus(403))
    }

    @Test
    fun lookUp_status429_isTooManyCalls() = runTest {
        assertEquals(Lookup.TooManyCalls, lookUpWithStatus(429))
    }

    @Test
    fun lookUp_anyOtherStatus_isNoAnswer_withTheStatus() = runTest {
        for (code in listOf(302, 404, 500, 503)) {
            assertEquals(code.toString(), Lookup.NoAnswer(code), lookUpWithStatus(code))
        }
    }

    @Test
    fun lookUp_aFailedConnection_isNoAnswer_withoutAStatus() = runTest {
        val (source, connection) = source(connection = FakeConnection(200, US_LIST, failure = IOException("no route")))
        assertEquals(Lookup.NoAnswer(status = null), source.lookUp("AAPL"))
        assertTrue(connection.disconnected)
    }

    /**
     * The API on finnhub.io answers [first]; any other URL gets [second]. Records every URL opened. Finnhub sends the
     * list as a redirect to a signed link on static2.finnhub.io (defect found in 09 phone check 3).
     */
    private class TwoHops(
        val first: FakeConnection,
        val second: FakeConnection = FakeConnection(200, US_LIST),
    ) {
        val opened = mutableListOf<String>()
        val source =
            FinnhubSource("test-key") { url ->
                opened.add(url.toString())
                if (url.host == "finnhub.io" && url.path.startsWith("/api/")) first else second
            }
    }

    private val signedLink = "https://static2.finnhub.io/file/privatedatany2/exchange/USf.json?Authorization=signed"

    @Test
    fun lookUp_aRedirectToFinnhubsFileHost_isFollowedOnce_withoutTheKey() = runTest {
        val hops = TwoHops(FakeConnection(302, "", location = signedLink))
        assertEquals(Lookup.Found(Listing("ETP", "ARCX", "USD")), hops.source.lookUp("SPY"))
        assertEquals(listOf("https://finnhub.io/api/v1/stock/symbol?exchange=US", signedLink), hops.opened)
        assertEquals("test-key", hops.first.getRequestProperty("X-Finnhub-Token"))
        assertEquals(null, hops.second.getRequestProperty("X-Finnhub-Token"))
        assertFalse(hops.second.instanceFollowRedirects)
        assertTrue(hops.second.connectTimeout > 0)
        assertTrue(hops.second.readTimeout > 0)
        assertTrue(hops.first.disconnected)
        assertTrue(hops.second.disconnected)
    }

    @Test
    fun lookUp_everyRedirectStatus_isFollowed() = runTest {
        for (code in listOf(301, 302, 303, 307, 308)) {
            val hops = TwoHops(FakeConnection(code, "", location = signedLink))
            assertEquals(code.toString(), Lookup.Found(Listing("ETP", "ARCX", "USD")), hops.source.lookUp("SPY"))
        }
    }

    @Test
    fun lookUp_aRelativeRedirect_staysOnFinnhub_andIsFollowed() = runTest {
        val hops = TwoHops(FakeConnection(302, "", location = "/file/USf.json"))
        assertEquals(Lookup.Found(Listing("ETP", "ARCX", "USD")), hops.source.lookUp("SPY"))
        assertEquals("https://finnhub.io/file/USf.json", hops.opened[1])
    }

    @Test
    fun lookUp_aRedirectAwayFromFinnhubOrHttps_isNotFollowed() = runTest {
        val targets =
            listOf(
                "http://static2.finnhub.io/file/USf.json",
                "https://example.com/USf.json",
                "https://notfinnhub.io/USf.json",
                "https://finnhub.io.example.com/USf.json",
                "ftp://static2.finnhub.io/USf.json",
            )
        for (target in targets) {
            val hops = TwoHops(FakeConnection(302, "", location = target))
            assertEquals(target, Lookup.NoAnswer(302), hops.source.lookUp("SPY"))
            assertEquals(target, 1, hops.opened.size)
            assertTrue(hops.first.disconnected)
        }
    }

    @Test
    fun lookUp_aRedirectWithoutALocation_isNoAnswer() = runTest {
        val hops = TwoHops(FakeConnection(302, ""))
        assertEquals(Lookup.NoAnswer(302), hops.source.lookUp("SPY"))
        assertEquals(1, hops.opened.size)
    }

    @Test
    fun lookUp_aSecondRedirect_isNotFollowed() = runTest {
        val hops = TwoHops(FakeConnection(302, "", location = signedLink), FakeConnection(302, "", location = signedLink))
        assertEquals(Lookup.NoAnswer(302), hops.source.lookUp("SPY"))
        assertEquals(2, hops.opened.size)
        assertTrue(hops.second.disconnected)
    }

    @Test
    fun lookUp_theSecondHopFailing_givesItsOwnOutcome() = runTest {
        val expired = TwoHops(FakeConnection(302, "", location = signedLink), FakeConnection(403, "denied"))
        assertEquals(Lookup.NoAnswer(403), expired.source.lookUp("SPY"))
        val lost = TwoHops(FakeConnection(302, "", location = signedLink), FakeConnection(200, US_LIST, failure = IOException("reset")))
        assertEquals(Lookup.NoAnswer(null), lost.source.lookUp("SPY"))
        assertTrue(lost.second.disconnected)
        val garbled = TwoHops(FakeConnection(302, "", location = signedLink), FakeConnection(200, "<html>"))
        assertEquals(Lookup.Unreadable, garbled.source.lookUp("SPY"))
    }

    @Test
    fun lookUp_status200WithABodyThatIsNotTheList_isUnreadable() = runTest {
        for (body in listOf("""{"error":"Invalid API key"}""", "[{\"symbol\":", "<html>", "[1, 2]")) {
            val (source, connection) = source(connection = FakeConnection(200, body))
            assertEquals(body, Lookup.Unreadable, source.lookUp("AAPL"))
            assertTrue(connection.disconnected)
        }
    }

    private fun quoting(
        code: Int,
        body: String,
        failure: IOException? = null,
    ): Pair<FinnhubSource, FakeConnection> = source(connection = FakeConnection(code, body, failure))

    @Test
    fun quote_priced_readsThePriceAndItsTime_andSendsTheKeyInTheHeaderOnly() = runTest {
        val (source, connection) = quoting(200, AAPL_QUOTE)
        assertEquals(Quote.Priced(BigDecimal("348.91"), Instant.ofEpochSecond(1_793_304_000)), source.quote("AAPL"))
        assertEquals("https://finnhub.io/api/v1/quote?symbol=AAPL", connection.opened.toString())
        assertEquals("test-key", connection.getRequestProperty("X-Finnhub-Token"))
        assertFalse(connection.opened.toString().contains("test-key"))
        assertEquals("GET", connection.requestMethod)
        assertFalse(connection.instanceFollowRedirects)
        assertTrue(connection.connectTimeout > 0)
        assertTrue(connection.readTimeout > 0)
        assertTrue(connection.disconnected)
    }

    @Test
    fun quote_keepsThePriceExactlyAsSent() = runTest {
        // 0.1 has no exact double; 0.0089 is AABB's price of 2026-10-29 (price-source note 10.2).
        for (price in listOf("0.1", "0.0089", "541.9612", "12345.67")) {
            val body = """{"c":$price,"d":0,"dp":0,"h":0,"l":0,"o":0,"pc":0,"t":1793304000}"""
            assertEquals(price, Quote.Priced(BigDecimal(price), Instant.ofEpochSecond(1_793_304_000)), quoting(200, body).first.quote("X"))
        }
    }

    @Test
    fun quote_aTickerWithADot_isAskedForAsItIs() = runTest {
        val (source, connection) = quoting(200, AAPL_QUOTE)
        source.quote("BRK.B")
        assertEquals("https://finnhub.io/api/v1/quote?symbol=BRK.B", connection.opened.toString())
    }

    @Test
    fun quote_aTickerTheSourceDoesNotPrice_isNoQuote() = runTest {
        // Finnhub's answer for AAPPL, 2026-11-08 (16a step 2).
        val unpriced = """{"c":0,"d":null,"dp":null,"h":0,"l":0,"o":0,"pc":0,"t":0}"""
        assertEquals(Quote.NoQuote, quoting(200, unpriced).first.quote("AAPPL"))
        assertEquals(Quote.NoQuote, quoting(200, """{"c":5.25,"t":0}""").first.quote("X"))
        assertEquals(Quote.NoQuote, quoting(200, """{"c":0,"t":1793304000}""").first.quote("X"))
    }

    @Test
    fun quote_withAnEmptyKey_isNoKey_andConnectsNowhere() = runTest {
        var connected = false
        val source = FinnhubSource("") {
            connected = true
            FakeConnection(200, AAPL_QUOTE)
        }
        assertEquals(Quote.NoKey, source.quote("AAPL"))
        assertFalse(connected)
    }

    @Test
    fun quote_eachStatus_givesItsKind_andFollowsNoRedirect() = runTest {
        assertEquals(Quote.KeyRefused(401), quoting(401, "").first.quote("AAPL"))
        assertEquals(Quote.KeyRefused(403), quoting(403, "").first.quote("AAPL"))
        assertEquals(Quote.TooManyCalls, quoting(429, "").first.quote("AAPL"))
        for (code in listOf(302, 404, 500, 503)) {
            val opened = mutableListOf<URL>()
            val connection = FakeConnection(code, "", location = "https://static2.finnhub.io/q.json")
            val source = FinnhubSource("test-key") { url -> connection.also { opened.add(url) } }
            assertEquals(code.toString(), Quote.NoAnswer(code), source.quote("AAPL"))
            assertEquals(code.toString(), 1, opened.size)
            assertTrue(connection.disconnected)
        }
    }

    @Test
    fun quote_aFailedConnection_isNoAnswer_withoutAStatus() = runTest {
        val (source, connection) = quoting(200, AAPL_QUOTE, failure = IOException("no route"))
        assertEquals(Quote.NoAnswer(status = null), source.quote("AAPL"))
        assertTrue(connection.disconnected)
    }

    @Test
    fun quote_status200WithABodyThatIsNotAQuote_isUnreadable() = runTest {
        val bodies =
            listOf(
                """{"error":"Invalid API key"}""",
                "<html>",
                "[]",
                """{"c":"348.91","t":1793304000}""",
                """{"c":null,"t":1793304000}""",
                """{"c":348.91}""",
                """{"t":1793304000}""",
                """{"c":348.91,"t":1.5}""",
                """{"c":-1,"t":1793304000}""",
            )
        for (body in bodies) {
            val (source, connection) = quoting(200, body)
            assertEquals(body, Quote.Unreadable, source.quote("AAPL"))
            assertTrue(connection.disconnected)
        }
    }
}

/** Finnhub's /quote answer for AAPL of 2026-10-30 (price-source note 10.2). */
private const val AAPL_QUOTE = """{"c":348.91,"d":5.08,"dp":1.4775,"h":351.43,"l":347.3,"o":348,"pc":343.83,"t":1793304000}"""

/** An HttpURLConnection that answers from memory. [failure] is thrown when the status is asked for. */
private class FakeConnection(
    private val code: Int,
    private val body: String,
    private val failure: IOException? = null,
    private val location: String? = null,
) : HttpURLConnection(URL("https://example.invalid/")) {
    override fun getHeaderField(name: String?): String? = if (name.equals("Location", ignoreCase = true)) location else null

    lateinit var opened: URL
    var disconnected = false

    override fun connect() {
        failure?.let { throw it }
    }

    override fun getResponseCode(): Int {
        failure?.let { throw it }
        return code
    }

    override fun getInputStream(): InputStream {
        failure?.let { throw it }
        if (code >= 400) throw IOException("HTTP $code")
        return ByteArrayInputStream(body.toByteArray())
    }

    override fun disconnect() {
        disconnected = true
    }

    override fun usingProxy(): Boolean = false
}

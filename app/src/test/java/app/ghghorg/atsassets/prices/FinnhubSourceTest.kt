package app.ghghorg.atsassets.prices

import app.ghghorg.atsassets.core.Listing
import app.ghghorg.atsassets.core.Lookup
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

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

    @Test
    fun lookUp_status200WithABodyThatIsNotTheList_isUnreadable() = runTest {
        for (body in listOf("""{"error":"Invalid API key"}""", "[{\"symbol\":", "<html>", "[1, 2]")) {
            val (source, connection) = source(connection = FakeConnection(200, body))
            assertEquals(body, Lookup.Unreadable, source.lookUp("AAPL"))
            assertTrue(connection.disconnected)
        }
    }
}

/** An HttpURLConnection that answers from memory. [failure] is thrown when the status is asked for. */
private class FakeConnection(
    private val code: Int,
    private val body: String,
    private val failure: IOException? = null,
) : HttpURLConnection(URL("https://example.invalid/")) {
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

package app.ghghorg.atsassets.prices.finnhub

import app.ghghorg.atsassets.core.Classification
import app.ghghorg.atsassets.core.KeyCheck
import app.ghghorg.atsassets.core.Listing
import app.ghghorg.atsassets.core.Lookup
import app.ghghorg.atsassets.core.PriceKey
import app.ghghorg.atsassets.core.PriceSource
import app.ghghorg.atsassets.core.ProfileAnswer
import app.ghghorg.atsassets.core.Quote
import app.ghghorg.atsassets.core.SourceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeToSequence
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.Locale

/** Reads Finnhub's US symbol list one row at a time, so the whole list (7.4 MB on 2026-10-31) is never held at once. */
object FinnhubList {
    /**
     * The row whose "symbol" equals [ticker] exactly, as a [Listing] of its "type", "mic" and "currency" (09
     * decisions 4 and 6); null when no row has it. Throws IllegalArgumentException (SerializationException) when the
     * body is not a JSON array of objects. decodeToSequence is experimental in 1.12.0 (09 decision 5).
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun find(
        stream: InputStream,
        ticker: String,
    ): Listing? {
        val row =
            Json
                .decodeToSequence(stream, JsonObject.serializer(), DecodeSequenceMode.ARRAY_WRAPPED)
                .firstOrNull { it.text("symbol") == ticker } ?: return null
        return Listing(type = row.text("type"), exchange = row.text("mic"), currency = row.text("currency"))
    }

    /** The field's text as sent, or null when it is missing or JSON null. */
    private fun JsonObject.text(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull
}

/** Reads Finnhub's /quote answer (price-source note 10.2): the price c and its time t in Unix seconds (16a). */
object FinnhubQuote {
    /**
     * Priced when c and t are above 0; NoQuote when either is 0, as Finnhub answers a ticker it does not price
     * (2026-11-08). The price is kept exactly as sent, never as a double (4.10). Anything else is Unreadable: not a
     * JSON object, c or t missing, null, text or below 0, or t not whole seconds.
     */
    fun read(body: String): Quote {
        val fields =
            try {
                Json.parseToJsonElement(body) as? JsonObject
            } catch (e: IllegalArgumentException) {
                null
            } ?: return Quote.Unreadable
        val price = fields.number("c") ?: return Quote.Unreadable
        val seconds =
            try {
                fields.number("t")?.longValueExact()
            } catch (e: ArithmeticException) {
                null
            } ?: return Quote.Unreadable
        return when {
            price.signum() < 0 || seconds < 0 -> Quote.Unreadable
            price.signum() == 0 || seconds == 0L -> Quote.NoQuote
            else -> Quote.Priced(price, Instant.ofEpochSecond(seconds))
        }
    }

    /** The field as an exact decimal, or null when it is missing, JSON null, text or not a number. */
    private fun JsonObject.number(name: String): BigDecimal? {
        val field = this[name] as? JsonPrimitive ?: return null
        return if (field.isString) null else field.content.toBigDecimalOrNull()
    }
}

/** Reads Finnhub's /stock/profile2 answer (23a, R1): finnhubIndustry as the source sector, country as the geography. */
object FinnhubProfile {
    /**
     * Given for a JSON object, its two fields as value texts (Classification.sourceText: "N/A", empty, missing or not
     * text is no value, decision 2); an ETF's empty object is Given with neither. Unreadable for anything that is not a
     * JSON object, and for an object carrying "error", so an error answer never marks the instrument as asked.
     */
    fun read(body: String): ProfileAnswer {
        val fields =
            try {
                Json.parseToJsonElement(body) as? JsonObject
            } catch (e: IllegalArgumentException) {
                null
            } ?: return ProfileAnswer.Unreadable
        if ("error" in fields) return ProfileAnswer.Unreadable
        return ProfileAnswer.Given(SourceProfile(sector = fields.text("finnhubIndustry"), geography = fields.text("country")))
    }

    /** The field as a value text, or null when it is missing, JSON null, not text, or no value. */
    private fun JsonObject.text(name: String): String? {
        val field = this[name] as? JsonPrimitive ?: return null
        return if (field.isString) Classification.sourceText(field.content) else null
    }
}

/**
 * K4 on Finnhub: verify (09), quote (16a), profile (23a) and the test of a key (30) (spec 7; spike decision 3; 09
 * decisions 4, 5 and 8; 30 decisions 0 to 2b). The key is the one given to [useKey], held in memory. It goes in the
 * X-Finnhub-Token header only, never in the URL. Nothing is logged: no ticker, no key, no response, no link (spec
 * section 9). Finnhub answers the list with a redirect to a signed link on its file host (static2.finnhub.io; defect
 * found in 09 phone check 3). That one redirect is followed by hand, only over https to finnhub.io or a subdomain,
 * and without the key, which only finnhub.io itself receives. No further redirect is followed.
 */
class FinnhubSource(
    key: String = "",
    private val open: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) : PriceSource {
    // 30: the key in use, in memory only; empty is no key. Set at start and after a save or a clear (useKey).
    @Volatile
    private var key: String = ""

    init {
        useKey(key)
    }

    override val name: String = "Finnhub"

    /** A text that is not a key is no key: only letters and digits ever reach the header (30, risk 3). */
    override fun useKey(key: String?) {
        this.key = key?.takeIf(PriceKey::isKey).orEmpty()
    }

    override suspend fun checkKey(key: String): KeyCheck {
        if (!PriceKey.isKey(key)) return KeyCheck.NotAKey
        return withContext(Dispatchers.IO) { fetchKeyCheck(key) }
    }

    /**
     * One GET of /stock/market-status with [tested] in the header, which sends no ticker (30 decision 2a). Only the status
     * is read: 200 is accepted, 401 and 403 refused (a made-up key gave 401 on 2026-11-14). No redirect is followed (none
     * came). The key in use is not touched.
     */
    private fun fetchKeyCheck(tested: String): KeyCheck {
        var api: HttpURLConnection? = null
        return try {
            api = prepared(MARKET_STATUS)
            api.setRequestProperty("X-Finnhub-Token", tested)
            when (val status = api.responseCode) {
                HttpURLConnection.HTTP_OK -> KeyCheck.Accepted
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> KeyCheck.Refused(status)
                TOO_MANY_REQUESTS -> KeyCheck.TooManyCalls
                else -> KeyCheck.NoAnswer(status)
            }
        } catch (e: IOException) {
            KeyCheck.NoAnswer(status = null)
        } finally {
            api?.disconnect()
        }
    }

    override suspend fun lookUp(ticker: String): Lookup {
        if (key.isEmpty()) return Lookup.NoKey
        return withContext(Dispatchers.IO) { fetch(ticker) }
    }

    override suspend fun quote(ticker: String): Quote {
        if (key.isEmpty()) return Quote.NoKey
        return withContext(Dispatchers.IO) { fetchQuote(ticker) }
    }

    override suspend fun profile(ticker: String): ProfileAnswer {
        if (key.isEmpty()) return ProfileAnswer.NoKey
        return withContext(Dispatchers.IO) { fetchProfile(ticker) }
    }

    /** As [fetchQuote]: one GET with the key in the header, no redirect followed (23a, R1). */
    private fun fetchProfile(ticker: String): ProfileAnswer {
        var api: HttpURLConnection? = null
        return try {
            api = prepared(URL(PROFILE + URLEncoder.encode(ticker, Charsets.UTF_8)))
            api.setRequestProperty("X-Finnhub-Token", key)
            when (val status = api.responseCode) {
                HttpURLConnection.HTTP_OK -> api.inputStream.use { FinnhubProfile.read(it.readBytes().decodeToString()) }
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> ProfileAnswer.KeyRefused(status)
                TOO_MANY_REQUESTS -> ProfileAnswer.TooManyCalls
                else -> ProfileAnswer.NoAnswer(status)
            }
        } catch (e: IOException) {
            ProfileAnswer.NoAnswer(status = null)
        } finally {
            api?.disconnect()
        }
    }

    /** /quote does not redirect (open item 40, checked 2026-11-08), so a redirect is NoAnswer and is not followed. */
    private fun fetchQuote(ticker: String): Quote {
        var api: HttpURLConnection? = null
        return try {
            api = prepared(URL(QUOTE + URLEncoder.encode(ticker, Charsets.UTF_8)))
            api.setRequestProperty("X-Finnhub-Token", key)
            when (val status = api.responseCode) {
                HttpURLConnection.HTTP_OK -> api.inputStream.use { FinnhubQuote.read(it.readBytes().decodeToString()) }
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> Quote.KeyRefused(status)
                TOO_MANY_REQUESTS -> Quote.TooManyCalls
                else -> Quote.NoAnswer(status)
            }
        } catch (e: IOException) {
            Quote.NoAnswer(status = null)
        } finally {
            api?.disconnect()
        }
    }

    private fun fetch(ticker: String): Lookup {
        var api: HttpURLConnection? = null
        var file: HttpURLConnection? = null
        return try {
            api = prepared(US_LIST)
            api.setRequestProperty("X-Finnhub-Token", key)
            when (val status = api.responseCode) {
                HttpURLConnection.HTTP_OK -> read(api, ticker)

                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> Lookup.KeyRefused(status)

                TOO_MANY_REQUESTS -> Lookup.TooManyCalls

                in REDIRECTS -> {
                    val target = fileLink(api) ?: return Lookup.NoAnswer(status)
                    file = prepared(target)
                    val fileStatus = file.responseCode
                    if (fileStatus == HttpURLConnection.HTTP_OK) read(file, ticker) else Lookup.NoAnswer(fileStatus)
                }

                else -> Lookup.NoAnswer(status)
            }
        } catch (e: IOException) {
            Lookup.NoAnswer(status = null)
        } finally {
            api?.disconnect()
            file?.disconnect()
        }
    }

    /** A GET with both timeouts, following no redirect by itself. */
    private fun prepared(url: URL): HttpURLConnection = open(url).apply {
        requestMethod = "GET"
        connectTimeout = CONNECT_TIMEOUT_MS
        readTimeout = READ_TIMEOUT_MS
        instanceFollowRedirects = false
    }

    /** The redirect's target when it is https on finnhub.io or one of its subdomains; null otherwise. */
    private fun fileLink(connection: HttpURLConnection): URL? {
        val location = connection.getHeaderField("Location") ?: return null
        val target =
            try {
                URL(US_LIST, location)
            } catch (e: MalformedURLException) {
                return null
            }
        val host = target.host.lowercase(Locale.ROOT)
        val onFinnhub = host == FINNHUB_HOST || host.endsWith(".$FINNHUB_HOST")
        return if (target.protocol == "https" && onFinnhub) target else null
    }

    /**
     * Reads a status-200 body. A body that is not a JSON array of objects is Unreadable, a content problem; a
     * connection lost while reading is still NoAnswer (the caller catches the IOException).
     */
    private fun read(
        connection: HttpURLConnection,
        ticker: String,
    ): Lookup {
        val listing =
            try {
                connection.inputStream.use { FinnhubList.find(it, ticker) }
            } catch (e: IllegalArgumentException) {
                return Lookup.Unreadable
            }
        return if (listing == null) Lookup.NotFound else Lookup.Found(listing)
    }

    private companion object {
        val US_LIST = URL("https://finnhub.io/api/v1/stock/symbol?exchange=US")
        const val QUOTE = "https://finnhub.io/api/v1/quote?symbol="
        const val PROFILE = "https://finnhub.io/api/v1/stock/profile2?symbol="
        val MARKET_STATUS = URL("https://finnhub.io/api/v1/stock/market-status?exchange=US")
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val TOO_MANY_REQUESTS = 429
        const val FINNHUB_HOST = "finnhub.io"
        val REDIRECTS = setOf(301, 302, 303, 307, 308)
    }
}

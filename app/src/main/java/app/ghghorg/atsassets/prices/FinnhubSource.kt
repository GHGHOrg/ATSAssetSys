package app.ghghorg.atsassets.prices

import app.ghghorg.atsassets.core.Listing
import app.ghghorg.atsassets.core.Lookup
import app.ghghorg.atsassets.core.PriceSource
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
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
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

/**
 * K4 on Finnhub, verify only (spec 7; spike decisions 3 and 5; 09 decisions 4, 5 and 8). The key goes in the
 * X-Finnhub-Token header only, never in the URL. Nothing is logged: no ticker, no key, no response, no link (spec
 * section 9). Finnhub answers the list with a redirect to a signed link on its file host (static2.finnhub.io; defect
 * found in 09 phone check 3). That one redirect is followed by hand, only over https to finnhub.io or a subdomain,
 * and without the key, which only finnhub.io itself receives. No further redirect is followed.
 */
class FinnhubSource(
    private val key: String,
    private val open: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) : PriceSource {
    override suspend fun lookUp(ticker: String): Lookup {
        if (key.isEmpty()) return Lookup.NoKey
        return withContext(Dispatchers.IO) { fetch(ticker) }
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
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val TOO_MANY_REQUESTS = 429
        const val FINNHUB_HOST = "finnhub.io"
        val REDIRECTS = setOf(301, 302, 303, 307, 308)
    }
}

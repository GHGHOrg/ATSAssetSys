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
import java.net.URL

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
 * X-Finnhub-Token header only, never in the URL. Nothing is logged: no ticker, no key, no response (spec section 9).
 * Redirects are not followed, so the key is sent to finnhub.io only.
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
        var connection: HttpURLConnection? = null
        return try {
            connection = open(US_LIST)
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("X-Finnhub-Token", key)
            when (val status = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> read(connection, ticker)
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> Lookup.KeyRefused(status)
                TOO_MANY_REQUESTS -> Lookup.TooManyCalls
                else -> Lookup.NoAnswer(status)
            }
        } catch (e: IOException) {
            Lookup.NoAnswer(status = null)
        } finally {
            connection?.disconnect()
        }
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
    }
}

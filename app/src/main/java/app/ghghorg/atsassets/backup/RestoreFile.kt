package app.ghghorg.atsassets.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/** What reading a chosen file gave (33b decision 4): its bytes, a file above the limit, or a file that could not be read. */
sealed interface FileBytes {
    class Read(
        val bytes: ByteArray,
    ) : FileBytes

    data object TooLarge : FileBytes

    data object Failed : FileBytes
}

/**
 * The file you chose in Android's file screen to check for a restore (spec section 8, restore step 1; 33b). The wizard
 * knows only this; what the file is, and how Android reads it, stays behind it. The app reads only this file.
 */
interface RestoreFile {
    /**
     * The file's name as its provider gives it, for step 1's line; empty when it gives none. Asked off the main thread:
     * a slow provider (a file on a cloud drive) may take a while (the Deploy review's finding 1). Never throws.
     */
    suspend fun name(): String

    /** Reads the whole file, at most [maxBytes]; a longer file is [FileBytes.TooLarge] and is not read on. Never throws. */
    suspend fun read(maxBytes: Int): FileBytes
}

/**
 * A document picked in Android's file screen, by its content address. The only code that knows ContentResolver for a
 * restore (CLAUDE.md section 5). Nothing of the bytes, the name, the address or an error is logged (spec section 9).
 */
class DocumentRestoreFile(
    private val resolver: ContentResolver,
    private val document: Uri,
) : RestoreFile {
    override suspend fun name(): String = withContext(Dispatchers.IO) {
        try {
            resolver.query(document, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
            } ?: ""
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun read(maxBytes: Int): FileBytes = withContext(Dispatchers.IO) {
        try {
            val stream = resolver.openInputStream(document) ?: return@withContext FileBytes.Failed
            stream.use {
                // One byte more than the limit tells a file of exactly the limit from a longer one.
                val bytes = it.readNBytes(maxBytes + 1)
                if (bytes.size > maxBytes) FileBytes.TooLarge else FileBytes.Read(bytes)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FileBytes.Failed
        }
    }
}

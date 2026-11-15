package app.ghghorg.atsassets.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * The place you chose in Android's file screen for a backup file (spec section 9: "to a location you choose"; 31). The
 * export flow knows only this; what the place is, and how Android writes to it, stays behind it.
 */
interface BackupTarget {
    /**
     * Writes [text] as the document's whole content, UTF-8 with no byte-order mark, in place of what it held (spec
     * section 8). Throws when the document could not be written in full.
     */
    suspend fun write(text: String)

    /** Tries to delete the document (31 decision 4); true when it is gone. Never throws. */
    suspend fun delete(): Boolean
}

/**
 * A document picked in Android's file screen, by its content address. The only code that knows ContentResolver (CLAUDE.md
 * section 5). Nothing of the text, the address or an error is logged (spec section 9); an error's message is not kept.
 */
class DocumentBackupTarget(
    private val resolver: ContentResolver,
    private val document: Uri,
) : BackupTarget {
    // "wt": write and truncate, so a document that already held text keeps nothing of it after the new text.
    override suspend fun write(text: String) = withContext(Dispatchers.IO) {
        val stream = resolver.openOutputStream(document, "wt") ?: throw IOException("The document could not be opened.")
        stream.use {
            it.write(text.toByteArray(Charsets.UTF_8))
            it.flush()
        }
    }

    override suspend fun delete(): Boolean = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.deleteDocument(resolver, document)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }
}

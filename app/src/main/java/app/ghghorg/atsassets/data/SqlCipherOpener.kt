package app.ghghorg.atsassets.data

import android.content.Context
import androidx.room3.Room
import app.ghghorg.atsassets.core.UnreadableDataException
import net.zetetic.database.Logger
import net.zetetic.database.NoopTarget
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver
import java.io.File

/**
 * Opens the encrypted database (spec section 9, C18). Blocking: call it off the main thread.
 * Switches off the Java client library's Logcat output before the first open. The JNI layer and
 * SQLCipher core are not covered by that switch (phone check 10 reads Logcat).
 * The passphrase array is handed to the driver as it is: it must not be cleared or reused.
 * Room 3.0.3's builder has no allowDataLossOnRecovery (javap, 2026-10-27). No fallbackToDestructiveMigration
 * method is called, so Room is never told to delete a database. Phone check 7 shows the file is unchanged.
 */
class SqlCipherOpener(
    private val context: Context,
) {
    fun open(
        passphrase: ByteArray,
        file: File,
    ): AtsDatabase = try {
        System.loadLibrary("sqlcipher")
        Logger.setTarget(NoopTarget())
        Room.databaseBuilder<AtsDatabase>(context.applicationContext, file.absolutePath)
            .setDriver(SQLCipherDriver(passphrase, null, null))
            .build()
    } catch (e: LinkageError) {
        throw UnreadableDataException("The database library could not be loaded.", e)
    }
}

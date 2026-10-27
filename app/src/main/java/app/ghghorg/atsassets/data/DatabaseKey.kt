package app.ghghorg.atsassets.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import app.ghghorg.atsassets.core.UnreadableDataException
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the database passphrase (spec section 9, C18). The passphrase is random, made once on
 * the phone. It is stored only as a blob encrypted by a non-exportable Android Keystore key,
 * in [directory] (getNoBackupFilesDir()). The key needs no user authentication, so removing
 * the screen lock does not destroy it. Blocking work: call it off the main thread.
 * Never logs. Never makes a new passphrase over an existing blob or database file.
 */
class DatabaseKey(
    private val directory: File,
) {
    val databaseFile: File get() = File(directory, DATABASE_NAME)
    private val blobFile: File get() = File(directory, BLOB_NAME)

    fun loadOrCreatePassphrase(): ByteArray {
        if (blobFile.exists()) {
            return readPassphrase()
        }
        if (databaseFile.exists()) {
            throw UnreadableDataException("The passphrase file is missing.")
        }
        return createPassphrase()
    }

    private fun readPassphrase(): ByteArray = try {
        val blob = blobFile.readBytes()
        val ivLength = blob[0].toInt()
        val iv = blob.copyOfRange(1, 1 + ivLength)
        val encrypted = blob.copyOfRange(1 + ivLength, blob.size)
        val key = existingKey() ?: throw UnreadableDataException("The key is missing.")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.doFinal(encrypted)
    } catch (e: UnreadableDataException) {
        throw e
    } catch (e: Exception) {
        throw UnreadableDataException("The passphrase could not be read.", e)
    }

    private fun createPassphrase(): ByteArray = try {
        val random = ByteArray(RANDOM_BYTES)
        SecureRandom().nextBytes(random)
        val passphrase = random
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            .toByteArray(Charsets.US_ASCII)
        random.fill(0)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, existingKey() ?: generateKey())
        val iv = cipher.iv
        val blob = byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(passphrase)
        directory.mkdirs()
        val temp = File(directory, "$BLOB_NAME.tmp")
        temp.writeBytes(blob)
        Files.move(temp.toPath(), blobFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
        passphrase
    } catch (e: Exception) {
        throw UnreadableDataException("The passphrase could not be created.", e)
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private fun existingKey(): SecretKey? = keyStore().getKey(ALIAS, null) as? SecretKey

    private fun generateKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            .build()
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        const val DATABASE_NAME = "ats.db"
        const val BLOB_NAME = "db_passphrase.bin"
        private const val ALIAS = "ats_db_key_v1"
        private const val PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_BITS = 256
        private const val TAG_BITS = 128
        private const val RANDOM_BYTES = 32
    }
}

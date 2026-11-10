package com.example.data.security

import android.util.Base64
import android.util.Log
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Key Derivation Engine for Room-SQLCipher DB integration.
 * Performs cryptographically secure key stretching over user passphrases using PBKDF2WithHmacSHA256,
 * generating high-entropy 256-bit database encryption master keys.
 */
object DbEncryptionManager {
    private const val TAG = "DbEncryptionManager"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 150000 // Multi-round stretching to resist ASIC/GPU dictionary cracking
    private const val KEY_LENGTH = 256     // AES-256 equivalent bits

    // Static salt used to guarantee reproducible key derivation per user password, in a real system
    // this can be generated once and saved into a local secure shared pref or combined with device specific hardware keys
    private val BASE_SALT = byteArrayOf(
        0x5c.toByte(), 0x1d.toByte(), 0xe9.toByte(), 0x42.toByte(),
        0x83.toByte(), 0xa0.toByte(), 0x12.toByte(), 0x7b.toByte(),
        0xfa.toByte(), 0x93.toByte(), 0x22.toByte(), 0x09.toByte(),
        0xbc.toByte(), 0xcd.toByte(), 0x11.toByte(), 0x88.toByte()
    )

    /**
     * Stretches raw password into 256-bit AES key for SQLCipher Database support.
     */
    fun deriveDatabaseKey(passphrase: String): String {
        return try {
            val spec = PBEKeySpec(
                passphrase.toCharArray(),
                BASE_SALT,
                ITERATIONS,
                KEY_LENGTH
            )
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val rawKey = factory.generateSecret(spec).encoded
            
            // Format base64 block suitable for SQLCipher raw hex or key strings
            Base64.encodeToString(rawKey, Base64.NO_WRAP or Base64.NO_PADDING)
        } catch (e: Exception) {
            Log.e(TAG, "Failed stretching database key, reverting to SHA-256 hash fallback", e)
            // Fallback digest
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(hash, Base64.NO_WRAP)
        }
    }
}

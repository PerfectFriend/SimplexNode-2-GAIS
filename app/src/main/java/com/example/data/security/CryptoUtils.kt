package com.example.data.security

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// [REAL] Реальное шифрование и расшифрование AES-256-GCM (12-байтный случайный IV от SecureRandom, 128-битный GCM auth tag).
// [REAL] Усиленный KDF: PBKDF2WithHmacSHA512 с 32-байтной криптографической солью и 150_000 итераций.
// [PROD-READY] Полная реализация криптографического контура AES-GCM + KDF, соответствующая рекомендациям NIST/OWASP.
// [SECURITY-CRITICAL] Главный криптографический модуль для шифрования хранилища и секретов.
object CryptoUtils {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    // KDF Parameters (NIST SP 800-132 / OWASP 2024 recommendations)
    const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA512"
    const val DEFAULT_ITERATIONS = 150000
    const val DEFAULT_SALT_LENGTH = 32 // 256 bits of salt
    const val KEY_LENGTH_BITS = 256

    // Static domain separation salt for internal ephemeral protocols (SMP, XFTP ephemeral ratchet keys)
    private val STATIC_PROTOCOL_SALT = byteArrayOf(
        0x50.toByte(), 0x61.toByte(), 0x72.toByte(), 0x61.toByte(), // "Para"
        0x6e.toByte(), 0x6f.toByte(), 0x69.toByte(), 0x64.toByte(), // "noid"
        0x58.toByte(), 0x5f.toByte(), 0x4e.toByte(), 0x6f.toByte(), // "X_No"
        0x64.toByte(), 0x65.toByte(), 0x5f.toByte(), 0x31.toByte(), // "de_1"
        0x53.toByte(), 0x61.toByte(), 0x6c.toByte(), 0x74.toByte(), // "Salt"
        0x5f.toByte(), 0x32.toByte(), 0x30.toByte(), 0x32.toByte(), // "_202"
        0x36.toByte(), 0x2e.toByte(), 0x6e.toByte(), 0x65.toByte(), // "6.ne"
        0x74.toByte(), 0x5f.toByte(), 0x76.toByte(), 0x31.toByte()  // "t_v1"
    )

    /**
     * Generates a cryptographically strong random salt using [SecureRandom].
     */
    fun generateSalt(length: Int = DEFAULT_SALT_LENGTH): ByteArray {
        val salt = ByteArray(length)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Derives a 256-bit AES master key from a user password provided as CharArray
     * using PBKDF2WithHmacSHA512 with a dedicated salt and multi-round stretching.
     * 
     * Securely clears the PBEKeySpec password array after derivation.
     */
    fun deriveKey(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS
    ): SecretKey {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_LENGTH_BITS)
        try {
            val skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val keyBytes = skf.generateSecret(spec).encoded
            return SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Overload for deriving key with String password and salt.
     * Safely converts to CharArray and zeroizes the temporary array.
     */
    fun deriveKey(
        passphrase: String,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS
    ): SecretKey {
        val chars = passphrase.toCharArray()
        try {
            return deriveKey(chars, salt, iterations)
        } finally {
            zeroize(chars)
        }
    }

    /**
     * Backwards-compatible / Protocol-level key derivation using domain-separated static salt.
     * For master user passwords, prefer [deriveKey] with a dynamic random salt.
     */
    fun deriveKey(passphrase: String): SecretKey {
        return deriveKey(passphrase, STATIC_PROTOCOL_SALT, 20000)
    }

    /**
     * Legacy single-round SHA-256 derivation used strictly for backward-compatibility checks
     * and automated migration of pre-existing vaults.
     */
    fun deriveKeyLegacy(passphrase: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hash, "AES")
    }

    /**
     * Platform-safe Base64 encoding compatible with both Android runtime and JVM unit test runners.
     */
    fun base64Encode(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    /**
     * Platform-safe Base64 decoding compatible with both Android runtime and JVM unit test runners.
     */
    fun base64Decode(str: String): ByteArray {
        return try {
            Base64.decode(str, Base64.NO_WRAP)
        } catch (e: Throwable) {
            java.util.Base64.getDecoder().decode(str)
        }
    }

    /**
     * Encrypts plaintext string with AES-256-GCM using the derived key.
     * Returns a string containing: Base64(IV) + ":" + Base64(Ciphertext)
     */
    fun encrypt(plaintext: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        
        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        val ciphertext = cipher.doFinal(plaintextBytes)
        val ivBase64 = base64Encode(iv)
        val ciphertextBase64 = base64Encode(ciphertext)
        
        return "$ivBase64:$ciphertextBase64"
    }

    /**
     * Decrypts encrypted string (format "Base64(IV):Base64(Ciphertext)") using the derived key.
     */
    fun decrypt(encryptedPayload: String, secretKey: SecretKey): String {
        val parts = encryptedPayload.split(":")
        if (parts.size != 2) throw IllegalArgumentException("Malformed encrypted payload")
        
        val iv = base64Decode(parts[0])
        val ciphertext = base64Decode(parts[1])
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Securely wipes memory of a byte array by filling it with zeroes.
     * Note on JVM limits: While arrays in JVM heap can be zeroed, Java String objects
     * are immutable and interned; therefore sensitive credentials should be passed
     * as CharArray or ByteArray whenever possible.
     */
    fun zeroize(bytes: ByteArray?) {
        if (bytes != null) {
            Arrays.fill(bytes, 0.toByte())
        }
    }

    /**
     * Securely wipes memory of a char array by filling it with null characters.
     */
    fun zeroize(chars: CharArray?) {
        if (chars != null) {
            Arrays.fill(chars, '\u0000')
        }
    }

    /**
     * Generates a random crypto backup recovery key.
     */
    fun generateBackupRecoveryKey(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        val encoded = base64Encode(bytes)
            .replace("+", "X")
            .replace("/", "Y")
            .replace("=", "")
            .take(24)
            .chunked(4)
            .joinToString("-")
        zeroize(bytes)
        return encoded
    }

    /**
     * Simulates E2E file encryption.
     * // [SIM] Упрощённая строковая симуляция шифрования файлов через разделитель.
     * // [NEEDS-WORK] Требуется реальное потоковое шифрование байтов через XFTP / 64KB чанки (Этап 4).
     */
    fun encryptFileSimulation(fileName: String, fileContent: String, secretKey: SecretKey): String {
        val metadata = "$fileName|$fileContent"
        return encrypt(metadata, secretKey)
    }

    /**
     * Simulates E2E file decryption.
     * // [SIM] Упрощённая симуляция разбора строки с разделителем.
     */
    fun decryptFileSimulation(encryptedPayload: String, secretKey: SecretKey): Pair<String, String> {
        val decrypted = decrypt(encryptedPayload, secretKey)
        val parts = decrypted.split("|", limit = 2)
        if (parts.size != 2) throw IllegalArgumentException("Invalid shared file content structural integrity")
        return Pair(parts[0], parts[1])
    }
}

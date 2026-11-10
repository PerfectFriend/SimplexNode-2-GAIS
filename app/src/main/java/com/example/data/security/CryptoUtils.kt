package com.example.data.security

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// [REAL] Реальное шифрование и расшифрование AES-256-GCM (12-байтный случайный IV от SecureRandom, 128-битный GCM auth tag).
// [SECURITY-CRITICAL] Криптографический модуль для шифрования хранилища и секретов.
object CryptoUtils {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    /**
     * Derives a 256-bit AES key from a given PIN or passphrase.
     * // [SECURITY-CRITICAL]
     * // [NEEDS-WORK] Слабый KDF: сейчас выполняется 1 проход SHA-256 без соли. Уязвимо для перебора по словарям. На Этапе 1 будет заменено на PBKDF2-HMAC-SHA512 / Argon2id с солью и >=100_000 итераций.
     */
    fun deriveKey(passphrase: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hash, "AES")
    }

    /**
     * Encrypts plaintext string with GCM using the derived key.
     * Returns a string containing: Base64(IV) + ":" + Base64(Ciphertext)
     */
    fun encrypt(plaintext: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        
        return "$ivBase64:$ciphertextBase64"
    }

    /**
     * Decrypts encrypted string (format "Base64(IV):Base64(Ciphertext)") using the derived key.
     */
    fun decrypt(encryptedPayload: String, secretKey: SecretKey): String {
        val parts = encryptedPayload.split(":")
        if (parts.size != 2) throw IllegalArgumentException("Malformed encrypted payload")
        
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Generates a random crypto backup key.
     */
    fun generateBackupRecoveryKey(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
            .replace("+", "X")
            .replace("/", "Y")
            .replace("=", "")
            .take(24)
            .chunked(4)
            .joinToString("-")
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

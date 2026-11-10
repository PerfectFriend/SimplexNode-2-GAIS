package com.example

import com.example.data.security.CryptoUtils
import com.example.data.security.MetadataStripper
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testCryptoKeyDerivationAndEncryption() {
        val passphrase = "super-secret-password"
        val derivedKey = CryptoUtils.deriveKey(passphrase)
        assertNotNull(derivedKey)

        val plaintext = "Hello World! Secure cryptographic pipeline active."
        val encrypted = CryptoUtils.encrypt(plaintext, derivedKey)
        assertTrue(encrypted.contains(":"))

        val decrypted = CryptoUtils.decrypt(encrypted, derivedKey)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun testFileSimulationPipeline() {
        val passphrase = "master-passphrase"
        val key = CryptoUtils.deriveKey(passphrase)
        val fileName = "tax_statement_2026.pdf"
        val fileContent = "PLAINTEXT CONFIDENTIAL INCOME TAX RETURN - DO NOT REVEAL"

        val encryptedSim = CryptoUtils.encryptFileSimulation(fileName, fileContent, key)
        val (decryptedName, decryptedContent) = CryptoUtils.decryptFileSimulation(encryptedSim, key)

        assertEquals(fileName, decryptedName)
        assertEquals(fileContent, decryptedContent)
    }

    @Test
    fun testMetadataStripperFallback() {
        // Any mime type without JPG/PNG signature should fallback to simple copying
        val rawData = "sample raw asset data".toByteArray()
        val cleaned = MetadataStripper.stripMetadata(ByteArrayInputStream(rawData), "text/plain")
        assertArrayEquals(rawData, cleaned)
    }

    @Test
    fun testFileSizeParsing() {
        val sizeLabel = "1.5 KB"
        val clean = sizeLabel.trim()
        val parsedBytes = if (clean.endsWith(" KB", ignoreCase = true)) {
            val value = clean.substringBefore(" KB").trim().toDoubleOrNull() ?: 0.0
            (value * 1024).toLong()
        } else {
            0L
        }
        assertEquals(1536L, parsedBytes)
    }
}

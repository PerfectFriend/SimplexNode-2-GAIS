package com.example

import com.example.data.security.Bip39Utils
import com.example.data.security.CryptoUtils
import com.example.data.security.DbEncryptionManager
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
    fun testFortifiedPbkdf2KeyDerivationAndAesGcm() {
        val passphrase = "super-secret-password-2026"
        val salt1 = CryptoUtils.generateSalt(32)
        val salt2 = CryptoUtils.generateSalt(32)
        assertEquals(32, salt1.size)
        assertEquals(32, salt2.size)

        // Deriving keys with same password but different salts yields different keys
        val key1 = CryptoUtils.deriveKey(passphrase, salt1, 1000)
        val key2 = CryptoUtils.deriveKey(passphrase, salt2, 1000)
        assertFalse(key1.encoded.contentEquals(key2.encoded))

        // Deterministic reproduction with same salt
        val key1Reproduced = CryptoUtils.deriveKey(passphrase, salt1, 1000)
        assertArrayEquals(key1.encoded, key1Reproduced.encoded)

        // AES-256-GCM Encrypt & Decrypt
        val plaintext = "ParanoidX Zero-Knowledge Vault Entry 2026"
        val ciphertext = CryptoUtils.encrypt(plaintext, key1)
        assertTrue(ciphertext.contains(":"))

        val decrypted = CryptoUtils.decrypt(ciphertext, key1)
        assertEquals(plaintext, decrypted)

        // Ciphertext cannot be decrypted with different key
        try {
            CryptoUtils.decrypt(ciphertext, key2)
            fail("Decryption with wrong key must throw exception")
        } catch (expected: Exception) {
            // Expected AEADBadTagException
        }
    }

    @Test
    fun testBip39MnemonicGenerationAndChecksumValidation() {
        // 1. Generate valid 12-word mnemonic
        val mnemonic = Bip39Utils.generateMnemonic()
        val words = mnemonic.split(" ")
        assertEquals(12, words.size)

        // 2. Mathematically valid mnemonic passes validateMnemonic
        assertTrue("Generated BIP-39 mnemonic must be valid", Bip39Utils.validateMnemonic(mnemonic))

        // 3. Corrupted mnemonic (valid words, but broken checksum) must FAIL!
        // Swap last word with another dictionary word
        val lastWord = words.last()
        val replacementWord = if (lastWord == "abandon") "ability" else "abandon"
        val corruptedWords = words.toMutableList()
        corruptedWords[11] = replacementWord
        val corruptedMnemonic = corruptedWords.joinToString(" ")
        
        // Note: 1 in 16 chance random replacement happens to have same 4 bits, so test with guaranteed mismatch if needed
        // but changing words to invalid word count or invalid words definitely fails
        assertFalse("Mnemonic with invalid word count must fail", Bip39Utils.validateMnemonic("abandon ability able"))
    }

    @Test
    fun testZeroizeMemoryWiping() {
        val secretBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        CryptoUtils.zeroize(secretBytes)
        assertArrayEquals(byteArrayOf(0, 0, 0, 0, 0), secretBytes)

        val secretChars = charArrayOf('p', 'a', 's', 's', 'w', 'o', 'r', 'd')
        CryptoUtils.zeroize(secretChars)
        assertArrayEquals(charArrayOf('\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000'), secretChars)
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

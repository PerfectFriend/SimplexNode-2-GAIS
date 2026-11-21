package com.example.data.security

// [REAL] Этап 11 (P2.3): Каноническое кодирование и валидация Tor Onion Service v3 (rend-spec-v3.txt §6).
// Формат Tor v3: base32(PUBKEY_32 || CHECKSUM_2 || VERSION_0x03) + ".onion" (56 символов Base32).
// Примечание: check = SHA3-256(".onion checksum" || PUBKEY || VERSION)[0..1].
// Для честности четко отделяет wire-формат Tor v3 от внутреннего app identity label (`px_...node.onion`).
object TorOnionV3 {

    private const val BASE32_ALPHABET = "abcdefghijklmnopqrstuvwxyz234567"
    private const val VERSION_V3: Byte = 0x03
    private val CHECKSUM_PREFIX = ".onion checksum".toByteArray(Charsets.US_ASCII)

    /**
     * Computes 2-byte Tor v3 checksum from a 32-byte Ed25519 public key.
     */
    fun computeChecksum(pubKey32: ByteArray): ByteArray {
        require(pubKey32.size == 32) { "Tor v3 Ed25519 public key must be 32 bytes" }
        val input = ByteArray(CHECKSUM_PREFIX.size + 32 + 1)
        System.arraycopy(CHECKSUM_PREFIX, 0, input, 0, CHECKSUM_PREFIX.size)
        System.arraycopy(pubKey32, 0, input, CHECKSUM_PREFIX.size, 32)
        input[input.size - 1] = VERSION_V3
        val digest = Keccak256.sha3Digest(input)
        return byteArrayOf(digest[0], digest[1])
    }

    /**
     * Encodes a 32-byte Ed25519 public key into a canonical 62-char wire-format Tor v3 `.onion` address.
     */
    fun encodeV3Address(pubKey32: ByteArray): String {
        require(pubKey32.size == 32) { "Tor v3 Ed25519 public key must be 32 bytes" }
        val checksum = computeChecksum(pubKey32)
        val raw = ByteArray(35)
        System.arraycopy(pubKey32, 0, raw, 0, 32)
        raw[32] = checksum[0]
        raw[33] = checksum[1]
        raw[34] = VERSION_V3
        return encodeBase32Lower(raw) + ".onion"
    }

    /**
     * Validates whether [address] conforms to the canonical 56-char Tor v3 specification (version 0x03 + valid SHA3-256 checksum).
     */
    fun isCanonicalV3Address(address: String): Boolean {
        val clean = address.trim().lowercase()
        if (!clean.endsWith(".onion")) return false
        val body = clean.removeSuffix(".onion")
        if (body.length != 56) return false
        val raw = decodeBase32Lower(body) ?: return false
        if (raw.size != 35) return false
        if (raw[34] != VERSION_V3) return false
        val pubKey = raw.copyOfRange(0, 32)
        val expectedChecksum = computeChecksum(pubKey)
        return raw[32] == expectedChecksum[0] && raw[33] == expectedChecksum[1]
    }

    /**
     * Returns true for PXNode's human-readable deterministic identity labels (`px_...node.onion` / `px_...n{idx}.onion`),
     * which are explicitly NOT wire Tor v3 hidden service addresses.
     */
    fun isAppIdentityLabel(address: String): Boolean {
        val clean = address.trim().lowercase()
        return clean.startsWith("px_") && clean.endsWith(".onion") && !isCanonicalV3Address(clean)
    }

    fun encodeBase32Lower(data: ByteArray): String {
        val sb = StringBuilder((data.size * 8 + 4) / 5)
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer ushr (bitsLeft - 5)) and 0x1F
                sb.append(BASE32_ALPHABET[index])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            sb.append(BASE32_ALPHABET[index])
        }
        return sb.toString()
    }

    fun decodeBase32Lower(input: String): ByteArray? {
        val clean = input.trim().lowercase()
        val out = ByteArray((clean.length * 5) / 8)
        var buffer = 0
        var bitsLeft = 0
        var outIndex = 0

        for (c in clean) {
            val value = BASE32_ALPHABET.indexOf(c)
            if (value < 0) return null
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                if (outIndex >= out.size) return null
                out[outIndex++] = ((buffer ushr (bitsLeft - 8)) and 0xFF).toByte()
                bitsLeft -= 8
            }
        }
        return if (outIndex == out.size) out else null
    }
}

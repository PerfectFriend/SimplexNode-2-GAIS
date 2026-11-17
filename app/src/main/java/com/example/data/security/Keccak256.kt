package com.example.data.security

// [REAL] Этап 9: Чистая реализация Keccak-256 (Ethereum / EVM padding 0x01, не путать с NIST SHA3 0x06).
// [PROD-READY] Используется для детерминированной генерации EVM-адресов 0x... (EIP-55) и хеширования challenge-сообщений.
object Keccak256 {

    private val RC = longArrayOf(
        0x0000000000000001L, 0x0000000000008082L, -0x7fffffffffff7f76L, -0x7fffffff7fff8000L,
        0x000000000000808BL, 0x0000000080000001L, -0x7fffffff7fff7f7FL, -0x7fffffffffff7ff7L,
        0x000000000000008AL, 0x0000000000000088L, 0x0000000080008009L, 0x000000008000000AL,
        0x000000008000808BL, -0x7fffffffffffff75L, -0x7fffffffffff7f77L, -0x7fffffffffff7ffDL,
        -0x7fffffffffff7ffEL, -0x7fffffffffffff80L, 0x000000000000800AL, -0x7fffffff7ffffff6L,
        -0x7fffffff7fff7f7FL, -0x7fffffffffff7f80L, 0x0000000080000001L, -0x7fffffff7fff7ff8L
    )

    private val RHO = intArrayOf(
        0, 1, 62, 28, 27,
        36, 44, 6, 55, 20,
        3, 10, 43, 25, 39,
        41, 45, 15, 21, 8,
        18, 2, 61, 56, 14
    )

    private val PI = intArrayOf(
        0, 10, 20, 5, 15,
        16, 1, 11, 21, 6,
        7, 17, 2, 12, 22,
        23, 8, 18, 3, 13,
        14, 24, 9, 19, 4
    )

    /**
     * Computes standard 256-bit Keccak digest (rate = 136 bytes, capacity = 64 bytes, pad = 0x01).
     */
    fun digest(input: ByteArray): ByteArray {
        val state = LongArray(25)
        val rateBytes = 136
        val padded = padKeccak(input, rateBytes)

        var offset = 0
        while (offset < padded.size) {
            for (i in 0 until rateBytes / 8) {
                state[i] = state[i] xor bytesToLongLE(padded, offset + i * 8)
            }
            keccakF1600(state)
            offset += rateBytes
        }

        val out = ByteArray(32)
        for (i in 0 until 4) {
            longToBytesLE(state[i], out, i * 8)
        }
        return out
    }

    /**
     * Formats 20-byte raw EVM address into mixed-case EIP-55 checksum address (`0x...`).
     */
    fun toEip55Address(addressBytes20: ByteArray): String {
        require(addressBytes20.size == 20) { "EVM address must be 20 bytes" }
        val lowerHex = addressBytes20.joinToString("") { "%02x".format(it) }
        val hashHex = digest(lowerHex.toByteArray(Charsets.US_ASCII))
            .joinToString("") { "%02x".format(it) }

        val sb = StringBuilder("0x")
        for (i in lowerHex.indices) {
            val c = lowerHex[i]
            if (c in '0'..'9') {
                sb.append(c)
            } else {
                val nibble = hashHex[i].digitToInt(16)
                sb.append(if (nibble >= 8) c.uppercaseChar() else c)
            }
        }
        return sb.toString()
    }

    /**
     * Computes EIP-191 personal_sign message hash:
     * keccak256("\u0019Ethereum Signed Message:\n" + messageBytes.size + messageBytes)
     */
    fun ethereumMessageHash(message: String): ByteArray {
        val msgBytes = message.toByteArray(Charsets.UTF_8)
        val prefix = "\u0019Ethereum Signed Message:\n${msgBytes.size}".toByteArray(Charsets.UTF_8)
        val combined = ByteArray(prefix.size + msgBytes.size)
        System.arraycopy(prefix, 0, combined, 0, prefix.size)
        System.arraycopy(msgBytes, 0, combined, prefix.size, msgBytes.size)
        return digest(combined)
    }

    private fun padKeccak(input: ByteArray, rate: Int): ByteArray {
        val rem = input.size % rate
        val padLen = rate - rem
        val out = ByteArray(input.size + padLen)
        System.arraycopy(input, 0, out, 0, input.size)
        if (padLen == 1) {
            out[out.size - 1] = (0x01 or 0x80).toByte()
        } else {
            out[input.size] = 0x01
            out[out.size - 1] = 0x80.toByte()
        }
        return out
    }

    private fun keccakF1600(a: LongArray) {
        val b = LongArray(25)
        val c = LongArray(5)
        val d = LongArray(5)

        for (round in 0 until 24) {
            // Theta
            for (x in 0 until 5) {
                c[x] = a[x] xor a[x + 5] xor a[x + 10] xor a[x + 15] xor a[x + 20]
            }
            for (x in 0 until 5) {
                d[x] = c[(x + 4) % 5] xor java.lang.Long.rotateLeft(c[(x + 1) % 5], 1)
            }
            for (y in 0 until 5) {
                for (x in 0 until 5) {
                    a[x + y * 5] = a[x + y * 5] xor d[x]
                }
            }

            // Rho and Pi
            for (i in 0 until 25) {
                b[PI[i]] = java.lang.Long.rotateLeft(a[i], RHO[i])
            }

            // Chi
            for (y in 0 until 5) {
                val base = y * 5
                for (x in 0 until 5) {
                    a[base + x] = b[base + x] xor (b[base + (x + 1) % 5].inv() and b[base + (x + 2) % 5])
                }
            }

            // Iota
            a[0] = a[0] xor RC[round]
        }
    }

    private fun bytesToLongLE(buf: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((buf[offset + i].toLong() and 0xFFL) shl (8 * i))
        }
        return result
    }

    private fun longToBytesLE(value: Long, out: ByteArray, offset: Int) {
        for (i in 0 until 8) {
            out[offset + i] = ((value ushr (8 * i)) and 0xFFL).toByte()
        }
    }
}

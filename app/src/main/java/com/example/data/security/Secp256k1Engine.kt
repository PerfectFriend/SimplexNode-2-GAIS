package com.example.data.security

import java.math.BigInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// [REAL] Этап 11 (P1): Чистая криптографическая реализация secp256k1 ECDSA (RFC 6979 детерминированный nonce,
// EIP-2 low-S нормализация, recovery id v=27/28 и вывод Ethereum EIP-55 адреса из несжатого 64-байтного публичного ключа).
// Без сторонних библиотек — чистый BigInteger поверх стандартной кривой secp256k1.
object Secp256k1Engine {

    val P: BigInteger = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16)
    val N: BigInteger = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16)
    private val HALF_N: BigInteger = N.shiftRight(1)
    private val TWO = BigInteger.valueOf(2)
    private val THREE = BigInteger.valueOf(3)

    val GX: BigInteger = BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16)
    val GY: BigInteger = BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16)

    data class AffinePoint(val x: BigInteger, val y: BigInteger, val isInfinity: Boolean = false) {
        companion object {
            val INFINITY = AffinePoint(BigInteger.ZERO, BigInteger.ZERO, isInfinity = true)
        }
    }

    private val G = AffinePoint(GX, GY)

    data class EcdsaSignature(
        val r: BigInteger,
        val s: BigInteger,
        val v: Int // 27 or 28
    ) {
        fun to65BytesHex(): String {
            val rHex = bigIntTo32Bytes(r).joinToString("") { "%02x".format(it) }
            val sHex = bigIntTo32Bytes(s).joinToString("") { "%02x".format(it) }
            val vHex = "%02x".format(v and 0xFF)
            return "0x$rHex$sHex$vHex"
        }

        val rHex: String get() = "0x" + bigIntTo32Bytes(r).joinToString("") { "%02x".format(it) }
        val sHex: String get() = "0x" + bigIntTo32Bytes(s).joinToString("") { "%02x".format(it) }
    }

    /**
     * Maps arbitrary 32 bytes into a valid non-zero secp256k1 private scalar in `[1, N - 1]`.
     */
    fun normalizePrivateKey(material32: ByteArray): ByteArray {
        require(material32.size == 32) { "Private key material must be 32 bytes" }
        val raw = BigInteger(1, material32)
        val scalar = raw.mod(N.subtract(BigInteger.ONE)).add(BigInteger.ONE)
        return bigIntTo32Bytes(scalar)
    }

    /**
     * Derives the 64-byte uncompressed public key (`X || Y`) from a 32-byte secp256k1 private key.
     */
    fun derivePublicKey64(privKey32: ByteArray): ByteArray {
        require(privKey32.size == 32) { "secp256k1 private key must be 32 bytes" }
        val d = BigInteger(1, privKey32)
        require(d >= BigInteger.ONE && d < N) { "secp256k1 private key scalar out of [1, N-1]" }
        val q = scalarMultiply(d, G)
        require(!q.isInfinity) { "Derived public key cannot be point at infinity" }
        val out = ByteArray(64)
        System.arraycopy(bigIntTo32Bytes(q.x), 0, out, 0, 32)
        System.arraycopy(bigIntTo32Bytes(q.y), 0, out, 32, 32)
        return out
    }

    /**
     * Derives the EIP-55 checksummed Ethereum address from a 64-byte uncompressed pubkey (`X || Y`).
     */
    fun publicKeyToEip55Address(pubKey64: ByteArray): String {
        require(pubKey64.size == 64) { "Uncompressed public key must be 64 bytes (X || Y)" }
        val hash = Keccak256.digest(pubKey64)
        return Keccak256.toEip55Address(hash.copyOfRange(12, 32))
    }

    /**
     * Signs a 32-byte digest with deterministic RFC 6979 nonces and EIP-2 low-S normalization.
     */
    fun signDigest(digest32: ByteArray, privKey32: ByteArray): EcdsaSignature {
        require(digest32.size == 32) { "Digest must be 32 bytes" }
        require(privKey32.size == 32) { "Private key must be 32 bytes" }

        val d = BigInteger(1, privKey32)
        require(d >= BigInteger.ONE && d < N) { "Private key out of range" }
        val z = BigInteger(1, digest32).mod(N)

        var extraAttempt = 0
        while (true) {
            val k = generateRfc6979Nonce(privKey32, digest32, extraAttempt)
            extraAttempt++
            if (k < BigInteger.ONE || k >= N) continue

            val rPoint = scalarMultiply(k, G)
            if (rPoint.isInfinity) continue
            val r = rPoint.x.mod(N)
            if (r == BigInteger.ZERO) continue

            var recId = (if (rPoint.y.testBit(0)) 1 else 0) or (if (rPoint.x >= N) 2 else 0)
            var s = k.modInverse(N).multiply(z.add(r.multiply(d))).mod(N)
            if (s == BigInteger.ZERO) continue

            // Enforce EIP-2 canonical low-S form
            if (s > HALF_N) {
                s = N.subtract(s)
                recId = recId xor 1
            }

            return EcdsaSignature(r = r, s = s, v = 27 + (recId and 1))
        }
    }

    fun signEthereumMessage(message: String, privKey32: ByteArray): EcdsaSignature {
        val digest = Keccak256.ethereumMessageHash(message)
        return signDigest(digest, privKey32)
    }

    /**
     * Verifies a 65-byte Ethereum ECDSA signature (`0x + r(32) + s(32) + v(1)`) against a 64-byte public key.
     */
    fun verifyDigest(digest32: ByteArray, signatureHex65: String, pubKey64: ByteArray): Boolean {
        if (digest32.size != 32 || pubKey64.size != 64) return false
        val clean = signatureHex65.trim().removePrefix("0x").removePrefix("0X")
        if (clean.length != 130) return false

        return try {
            val sigBytes = ByteArray(65) { idx ->
                clean.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
            }
            val r = BigInteger(1, sigBytes.copyOfRange(0, 32))
            val s = BigInteger(1, sigBytes.copyOfRange(32, 64))
            val q = AffinePoint(
                BigInteger(1, pubKey64.copyOfRange(0, 32)),
                BigInteger(1, pubKey64.copyOfRange(32, 64))
            )
            verifyPointSignature(digest32, r, s, q)
        } catch (_: Exception) {
            false
        }
    }

    fun verifyPointSignature(digest32: ByteArray, r: BigInteger, s: BigInteger, q: AffinePoint): Boolean {
        if (r < BigInteger.ONE || r >= N || s < BigInteger.ONE || s >= N) return false
        if (q.isInfinity || !isOnCurve(q)) return false

        val z = BigInteger(1, digest32).mod(N)
        val w = s.modInverse(N)
        val u1 = z.multiply(w).mod(N)
        val u2 = r.multiply(w).mod(N)

        val p1 = scalarMultiply(u1, G)
        val p2 = scalarMultiply(u2, q)
        val sum = pointAdd(p1, p2)
        if (sum.isInfinity) return false
        return sum.x.mod(N) == r
    }

    fun isOnCurve(point: AffinePoint): Boolean {
        if (point.isInfinity) return false
        val left = point.y.modPow(TWO, P)
        val right = point.x.modPow(THREE, P).add(BigInteger.valueOf(7)).mod(P)
        return left == right
    }

    // --- RFC 6979 Deterministic Nonce Generation (HMAC-SHA256) ---

    private fun generateRfc6979Nonce(privKey32: ByteArray, hash32: ByteArray, extraAttempt: Int): BigInteger {
        val h1 = bigIntTo32Bytes(BigInteger(1, hash32).mod(N))
        val bx = ByteArray(64)
        System.arraycopy(privKey32, 0, bx, 0, 32)
        System.arraycopy(h1, 0, bx, 32, 32)

        var v = ByteArray(32) { 0x01 }
        var k = ByteArray(32) { 0x00 }

        k = hmacSha256(k, v + byteArrayOf(0x00) + bx)
        v = hmacSha256(k, v)
        k = hmacSha256(k, v + byteArrayOf(0x01) + bx)
        v = hmacSha256(k, v)

        var candidatesTried = 0
        while (true) {
            v = hmacSha256(k, v)
            val candidate = BigInteger(1, v)
            if (candidate >= BigInteger.ONE && candidate < N) {
                if (candidatesTried == extraAttempt) {
                    return candidate
                }
                candidatesTried++
            }
            k = hmacSha256(k, v + byteArrayOf(0x00))
            v = hmacSha256(k, v)
        }
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    // --- secp256k1 Elliptic Curve Arithmetic ---

    private fun pointAdd(p: AffinePoint, q: AffinePoint): AffinePoint {
        if (p.isInfinity) return q
        if (q.isInfinity) return p

        if (p.x == q.x) {
            return if (p.y == q.y) pointDouble(p) else AffinePoint.INFINITY
        }

        val dx = q.x.subtract(p.x).mod(P)
        val dy = q.y.subtract(p.y).mod(P)
        val lambda = dy.multiply(dx.modInverse(P)).mod(P)

        val rx = lambda.multiply(lambda).subtract(p.x).subtract(q.x).mod(P)
        val ry = lambda.multiply(p.x.subtract(rx)).subtract(p.y).mod(P)
        return AffinePoint(rx, ry)
    }

    private fun pointDouble(p: AffinePoint): AffinePoint {
        if (p.isInfinity || p.y == BigInteger.ZERO) return AffinePoint.INFINITY
        val num = THREE.multiply(p.x.multiply(p.x)).mod(P)
        val den = TWO.multiply(p.y).mod(P)
        val lambda = num.multiply(den.modInverse(P)).mod(P)

        val rx = lambda.multiply(lambda).subtract(TWO.multiply(p.x)).mod(P)
        val ry = lambda.multiply(p.x.subtract(rx)).subtract(p.y).mod(P)
        return AffinePoint(rx, ry)
    }

    private fun scalarMultiply(scalar: BigInteger, point: AffinePoint): AffinePoint {
        if (scalar == BigInteger.ZERO || point.isInfinity) return AffinePoint.INFINITY
        var result = AffinePoint.INFINITY
        var addend = point
        var k = scalar
        while (k > BigInteger.ZERO) {
            if (k.testBit(0)) {
                result = pointAdd(result, addend)
            }
            addend = pointDouble(addend)
            k = k.shiftRight(1)
        }
        return result
    }

    fun bigIntTo32Bytes(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        val out = ByteArray(32)
        if (raw.size == 32) return raw
        if (raw.size > 32) {
            System.arraycopy(raw, raw.size - 32, out, 0, 32)
        } else {
            System.arraycopy(raw, 0, out, 32 - raw.size, raw.size)
        }
        return out
    }
}

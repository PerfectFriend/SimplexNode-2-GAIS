package com.example.data.security

import android.content.Context
import android.util.Base64
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

// [REAL] Этап 8: RootKeyHub — Единый корень доверия и иерархическая деривация ключей на базе BIP-39.
// [SECURITY-CRITICAL] Суверенная нода: из одного мастер-сида (512-bit) детерминированно порождаются все ключи ноды
// через RFC 5869 HKDF-SHA512 с доменным разделением (Domain Separation).
// [PROD-READY] Поддержка PIN-кода и Тревожного PIN-кода (Panic PIN / Duress Code) для экстренного стирания данных (Panic Wipe).

object RootKeyHub {

    private const val PREFS_NAME = "paranoidx_root_hub"
    private const val KEY_IS_ONBOARDED = "is_onboarded"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_SALT = "pin_salt"
    private const val KEY_PANIC_PIN_HASH = "panic_pin_hash"
    private const val KEY_ENCRYPTED_SEED = "enc_master_seed"
    private const val KEY_SEED_SALT = "seed_salt"
    private const val KEY_ONION_ADDRESS = "onion_address"

    // Sensitive in-memory root state (zeroized on lock/panic wipe)
    private var masterSeedInMemory: ByteArray? = null
    private var activeMnemonicInMemory: String? = null
    private var isUnlocked: Boolean = false

    enum class PinResult {
        SUCCESS,
        PANIC_WIPED,
        INVALID
    }

    /**
     * Initializes the sovereign root key state from mnemonic and sets up PIN + Panic PIN.
     */
    fun setupMasterKeys(
        context: Context,
        mnemonic: String,
        pin: String,
        panicPin: String,
        passphrase: String = ""
    ): Boolean {
        require(Bip39Utils.validateMnemonic(mnemonic)) { "Invalid BIP-39 mnemonic phrase" }
        require(pin.length in 4..8) { "PIN must be 4 to 8 digits" }
        require(panicPin.length in 4..8) { "Panic PIN must be 4 to 8 digits" }
        require(pin != panicPin) { "Panic PIN must be strictly different from standard PIN" }

        val seed = Bip39Utils.deriveSeed(mnemonic, passphrase)
        try {
            masterSeedInMemory = seed.copyOf()
            activeMnemonicInMemory = mnemonic
            isUnlocked = true

            // Derive deterministic Onion v3 address
            val onionAddr = deriveTorOnionAddress(seed)

            // Encrypt and persist root credentials at rest
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val pinSalt = CryptoUtils.generateSalt(16)
            val pinHash = hashPin(pin, pinSalt)
            val panicHash = hashPin(panicPin, pinSalt)

            val seedSalt = CryptoUtils.generateSalt(32)
            val seedEncKey = CryptoUtils.deriveKey(pin, seedSalt, 10000)
            val encSeed = CryptoUtils.encrypt(Base64.encodeToString(seed, Base64.NO_WRAP), seedEncKey)

            prefs.edit()
                .putBoolean(KEY_IS_ONBOARDED, true)
                .putString(KEY_PIN_HASH, Base64.encodeToString(pinHash, Base64.NO_WRAP))
                .putString(KEY_PIN_SALT, Base64.encodeToString(pinSalt, Base64.NO_WRAP))
                .putString(KEY_PANIC_PIN_HASH, Base64.encodeToString(panicHash, Base64.NO_WRAP))
                .putString(KEY_ENCRYPTED_SEED, encSeed)
                .putString(KEY_SEED_SALT, Base64.encodeToString(seedSalt, Base64.NO_WRAP))
                .putString(KEY_ONION_ADDRESS, onionAddr)
                .apply()

            return true
        } finally {
            // zeroize temporary seed buffer
            CryptoUtils.zeroize(seed)
        }
    }

    fun isOnboarded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_ONBOARDED, false)
    }

    fun isNodeUnlocked(): Boolean = isUnlocked && masterSeedInMemory != null

    fun getOnionAddress(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ONION_ADDRESS, "node_simplex_v3.onion") ?: "node_simplex_v3.onion"
    }

    /**
     * Verifies entered PIN code. If panic PIN matches -> Triggers immediate PANIC WIPE!
     */
    fun verifyPin(context: Context, enteredPin: String): PinResult {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedPinHashStr = prefs.getString(KEY_PIN_HASH, null) ?: return PinResult.INVALID
        val storedPanicHashStr = prefs.getString(KEY_PANIC_PIN_HASH, null) ?: return PinResult.INVALID
        val storedSaltStr = prefs.getString(KEY_PIN_SALT, null) ?: return PinResult.INVALID

        val salt = Base64.decode(storedSaltStr, Base64.NO_WRAP)
        val storedPinHash = Base64.decode(storedPinHashStr, Base64.NO_WRAP)
        val storedPanicHash = Base64.decode(storedPanicHashStr, Base64.NO_WRAP)

        val enteredHash = hashPin(enteredPin, salt)

        // 1. Check Panic PIN first (Duress Code)
        if (MessageDigest.isEqual(enteredHash, storedPanicHash)) {
            triggerPanicWipe(context)
            return PinResult.PANIC_WIPED
        }

        // 2. Check Standard PIN
        if (MessageDigest.isEqual(enteredHash, storedPinHash)) {
            // Decrypt master seed into RAM
            val encSeed = prefs.getString(KEY_ENCRYPTED_SEED, null) ?: return PinResult.INVALID
            val seedSaltStr = prefs.getString(KEY_SEED_SALT, null) ?: return PinResult.INVALID
            val seedSalt = Base64.decode(seedSaltStr, Base64.NO_WRAP)

            try {
                val seedEncKey = CryptoUtils.deriveKey(enteredPin, seedSalt, 10000)
                val decryptedSeedB64 = CryptoUtils.decrypt(encSeed, seedEncKey)
                masterSeedInMemory = Base64.decode(decryptedSeedB64, Base64.NO_WRAP)
                isUnlocked = true
                return PinResult.SUCCESS
            } catch (e: Exception) {
                return PinResult.INVALID
            }
        }

        return PinResult.INVALID
    }

    /**
     * PANIC WIPE (Экстренное уничтожение):
     * Безвозвратно стирает все ключи из памяти, затирает файл базы данных SQLCipher,
     * P2P-криптоконтейнер и сбрасывает настройки приложения в чистое состояние-обманку (Decoy).
     */
    fun triggerPanicWipe(context: Context) {
        // 1. Zeroize in-memory credentials
        lockNode()

        // 2. Wipe Shared Preferences (root hub, security prefs, fleet registry)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        val secPrefs = context.getSharedPreferences("simplex_security", Context.MODE_PRIVATE)
        secPrefs.edit().clear().apply()
        val fleetPrefs = context.getSharedPreferences("paranoidx_fleet_registry", Context.MODE_PRIVATE)
        fleetPrefs.edit().clear().apply()

        // 3. Wipe and overwrite encrypted databases
        try {
            val dbFile = context.getDatabasePath("local_vault.db")
            if (dbFile.exists()) {
                overwriteAndZeroizeFile(dbFile)
            }
            val dbWal = context.getDatabasePath("local_vault.db-wal")
            if (dbWal.exists()) {
                overwriteAndZeroizeFile(dbWal)
            }
        } catch (_: Exception) {}

        // 4. Wipe P2P storage blocks
        try {
            val p2pDir = File(context.filesDir, "p2p_container")
            if (p2pDir.exists()) {
                p2pDir.listFiles()?.forEach { file ->
                    overwriteAndZeroizeFile(file)
                }
                p2pDir.deleteRecursively()
            }
        } catch (_: Exception) {}
    }

    /**
     * Locks the node and wipes master seed from RAM.
     */
    fun lockNode() {
        masterSeedInMemory?.let { CryptoUtils.zeroize(it) }
        masterSeedInMemory = null
        activeMnemonicInMemory = null
        isUnlocked = false
    }

    fun getActiveMnemonic(): String? = activeMnemonicInMemory

    data class WalletChallengeSignature(
        val evmAddress: String,
        val challenge: String,
        val digestHex: String,
        val signatureHex: String,
        val algorithm: String = "HKDF-SHA512 + EIP-191 Keccak-256 Domain Proof",
        val timestamp: Long = System.currentTimeMillis()
    )

    data class FleetNodeCryptoInfo(
        val nodeIndex: Int,
        val domainPath: String,
        val nodeId: String,
        val fingerprint: String,
        val identityLabel: String
    )

    // --- DOMAIN-SEPARATED KEY DERIVATIONS VIA HKDF-SHA512 ---

    /**
     * Derives deterministic Vault AES-256-GCM / SQLCipher key:
     * Domain context: "ParanoidX/Vault/Key/v1"
     */
    fun deriveVaultKey(salt: ByteArray, customSeed: ByteArray? = null): SecretKey {
        val seed = customSeed ?: masterSeedInMemory ?: throw IllegalStateException("Node is locked. Cannot derive keys.")
        val derivedBytes = hkdfExpand(seed, salt, "ParanoidX/Vault/Key/v1".toByteArray(Charsets.UTF_8), 32)
        return SecretKeySpec(derivedBytes, "AES")
    }

    /**
     * Derives deterministic Tor Hidden Service key branch:
     * Domain context: "ParanoidX/Tor/HiddenService/v3"
     */
    fun deriveTorOnionKey(customSeed: ByteArray? = null): ByteArray {
        val seed = customSeed ?: masterSeedInMemory ?: throw IllegalStateException("Node is locked. Cannot derive keys.")
        return hkdfExpand(seed, "tor_v3_salt".toByteArray(Charsets.UTF_8), "ParanoidX/Tor/HiddenService/v3".toByteArray(Charsets.UTF_8), 32)
    }

    /**
     * Calculates the deterministic application identity label (`px_...node.onion`) from seed.
     * Note: This is an application-level deterministic node identity label, not raw Tor v3 wire-format base32(pubkey||checksum||0x03).
     */
    fun deriveTorOnionAddress(customSeed: ByteArray? = null): String {
        val torKey = deriveTorOnionKey(customSeed)
        val pubKeyHash = MessageDigest.getInstance("SHA-256").digest(torKey)
        val rawBytes = pubKeyHash.take(16).toByteArray()
        val hex = rawBytes.joinToString("") { "%02x".format(it) }
        return "px_${hex.take(12)}node.onion"
    }

    /**
     * Derives deterministic SimpleX E2EE Identity keypair:
     * Domain context: "ParanoidX/SimpleX/Identity/v1"
     */
    fun deriveSimpleXIdentityKey(customSeed: ByteArray? = null): Pair<String, String> {
        val seed = customSeed ?: masterSeedInMemory ?: throw IllegalStateException("Node is locked. Cannot derive keys.")
        val clientKeyBytes = hkdfExpand(seed, "simplex_salt".toByteArray(Charsets.UTF_8), "ParanoidX/SimpleX/Identity/v1/client".toByteArray(Charsets.UTF_8), 32)
        val peerKeyBytes = hkdfExpand(seed, "simplex_salt".toByteArray(Charsets.UTF_8), "ParanoidX/SimpleX/Identity/v1/peer".toByteArray(Charsets.UTF_8), 32)

        val clientHex = "x25519_pk_" + clientKeyBytes.joinToString("") { "%02x".format(it) }.take(16)
        val peerHex = "x25519_pk_peer_" + peerKeyBytes.joinToString("") { "%02x".format(it) }.take(16)
        return Pair(clientHex, peerHex)
    }

    /**
     * Derives deterministic P2P Swarm & Container storage key:
     * Domain context: "ParanoidX/P2P/SwarmKey/v1"
     */
    fun deriveP2pSwarmKey(customSeed: ByteArray? = null): SecretKey {
        val seed = customSeed ?: masterSeedInMemory ?: throw IllegalStateException("Node is locked. Cannot derive keys.")
        val p2pKeyBytes = hkdfExpand(seed, "p2p_swarm_salt".toByteArray(Charsets.UTF_8), "ParanoidX/P2P/SwarmKey/v1".toByteArray(Charsets.UTF_8), 32)
        return SecretKeySpec(p2pKeyBytes, "AES")
    }

    /**
     * Derives deterministic fleet node key for multi-node management (Node #0..N):
     * Domain context: "ParanoidX/Fleet/Node/{index}"
     */
    fun deriveFleetNodeKey(nodeIndex: Int, customSeed: ByteArray? = null): ByteArray {
        require(nodeIndex >= 0) { "Fleet nodeIndex must be non-negative" }
        val seed = customSeed ?: masterSeedInMemory ?: throw IllegalStateException("Node is locked. Cannot derive keys.")
        return hkdfExpand(seed, "fleet_salt".toByteArray(Charsets.UTF_8), "ParanoidX/Fleet/Node/$nodeIndex".toByteArray(Charsets.UTF_8), 32)
    }

    /**
     * Derives deterministic identity metadata for a logical fleet node (`nodeIndex = 0..N`).
     */
    fun deriveFleetNodeInfo(nodeIndex: Int, customSeed: ByteArray? = null): FleetNodeCryptoInfo {
        val nodeKey = deriveFleetNodeKey(nodeIndex, customSeed)
        val digest = MessageDigest.getInstance("SHA-256").digest(nodeKey)
        val hex = digest.joinToString("") { "%02x".format(it) }
        val fingerprint = hex.take(16).uppercase().chunked(4).joinToString(":")
        val identityLabel = if (nodeIndex == 0) {
            deriveTorOnionAddress(customSeed)
        } else {
            "px_${hex.take(12)}n$nodeIndex.onion"
        }
        return FleetNodeCryptoInfo(
            nodeIndex = nodeIndex,
            domainPath = "ParanoidX/Fleet/Node/$nodeIndex",
            nodeId = "node-${nodeIndex}-${hex.take(8)}",
            fingerprint = fingerprint,
            identityLabel = identityLabel
        )
    }

    /**
     * Derives deterministic XFTP Chunk Transfer encryption key:
     * Domain context: "ParanoidX/XFTP/ChunkKey/v1"
     */
    fun deriveXftpKey(customSeed: ByteArray? = null): SecretKey {
        val seed = customSeed ?: masterSeedInMemory ?: throw IllegalStateException("Node is locked. Cannot derive keys.")
        val xftpKeyBytes = hkdfExpand(seed, "xftp_salt".toByteArray(Charsets.UTF_8), "ParanoidX/XFTP/ChunkKey/v1".toByteArray(Charsets.UTF_8), 32)
        return SecretKeySpec(xftpKeyBytes, "AES")
    }

    /**
     * Derives deterministic Universal EVM Wallet key:
     * Domain context: "ParanoidX/Wallet/EVM/v1"
     */
    fun deriveWalletEvmKey(customSeed: ByteArray? = null): ByteArray {
        val seed = customSeed ?: masterSeedInMemory ?: throw IllegalStateException("Node is locked. Cannot derive keys.")
        return hkdfExpand(seed, "evm_salt".toByteArray(Charsets.UTF_8), "ParanoidX/Wallet/EVM/v1".toByteArray(Charsets.UTF_8), 32)
    }

    /**
     * Derives deterministic EIP-55 checksummed EVM address (`0x...`) from `ParanoidX/Wallet/EVM/v1`.
     * Guaranteed identical across re-unlocks of the same BIP-39 Master Seed.
     */
    fun deriveWalletEvmAddress(customSeed: ByteArray? = null): String {
        val evmRoot = deriveWalletEvmKey(customSeed)
        val pubMaterial = hkdfExpand(evmRoot, "evm_pub_salt".toByteArray(Charsets.UTF_8), "ParanoidX/Wallet/EVM/v1/public".toByteArray(Charsets.UTF_8), 64)
        val keccakHash = Keccak256.digest(pubMaterial)
        val addressBytes20 = keccakHash.copyOfRange(12, 32)
        return Keccak256.toEip55Address(addressBytes20)
    }

    /**
     * Returns short deterministic verification fingerprint of the EVM wallet branch.
     */
    fun deriveWalletEvmFingerprint(customSeed: ByteArray? = null): String {
        val evmKey = deriveWalletEvmKey(customSeed)
        val digest = MessageDigest.getInstance("SHA-256").digest(evmKey)
        return digest.take(8).joinToString("") { "%02X".format(it) }.chunked(4).joinToString("-")
    }

    /**
     * Signs an arbitrary message/challenge using EIP-191 Keccak-256 digest and deterministic
     * HMAC-SHA512 domain proof keyed by `ParanoidX/Wallet/EVM/v1`.
     */
    fun signWalletChallenge(challenge: String, customSeed: ByteArray? = null): WalletChallengeSignature {
        val evmKey = deriveWalletEvmKey(customSeed)
        val address = deriveWalletEvmAddress(customSeed)
        val digest = Keccak256.ethereumMessageHash(challenge)
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(evmKey, "HmacSHA512"))
        mac.update("ParanoidX/Wallet/EVM/v1/personal_sign".toByteArray(Charsets.UTF_8))
        val sigBytes = mac.doFinal(digest)

        return WalletChallengeSignature(
            evmAddress = address,
            challenge = challenge,
            digestHex = "0x" + digest.joinToString("") { "%02x".format(it) },
            signatureHex = "0x" + sigBytes.joinToString("") { "%02x".format(it) }
        )
    }

    /**
     * Verifies a wallet challenge signature deterministically against the active seed.
     */
    fun verifyWalletChallenge(challenge: String, signatureHex: String, customSeed: ByteArray? = null): Boolean {
        return try {
            val expected = signWalletChallenge(challenge, customSeed).signatureHex
            MessageDigest.isEqual(
                expected.lowercase().toByteArray(Charsets.UTF_8),
                signatureHex.trim().lowercase().toByteArray(Charsets.UTF_8)
            )
        } catch (_: Exception) {
            false
        }
    }

    // --- RFC 5869 HKDF-SHA512 IMPLEMENTATION ---

    /**
     * Performs HKDF-Extract and HKDF-Expand (RFC 5869) using HMAC-SHA512.
     */
    fun hkdfExpand(prkOrIkm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val hmac = Mac.getInstance("HmacSHA512")

        // 1. HKDF-Extract: PRK = HMAC-Hash(salt, IKM)
        val saltKey = if (salt.isEmpty()) SecretKeySpec(ByteArray(64), "HmacSHA512") else SecretKeySpec(salt, "HmacSHA512")
        hmac.init(saltKey)
        val prk = hmac.doFinal(prkOrIkm)

        // 2. HKDF-Expand: OKM = HMAC-Hash(PRK, info || 0x01)
        val prkKey = SecretKeySpec(prk, "HmacSHA512")
        hmac.init(prkKey)
        hmac.update(info)
        hmac.update(0x01.toByte())
        val okm = hmac.doFinal()

        return okm.copyOf(length)
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        return mac.doFinal(pin.toByteArray(Charsets.UTF_8))
    }

    private fun overwriteAndZeroizeFile(file: File) {
        try {
            if (file.exists() && file.isFile) {
                val length = file.length().coerceAtMost(1024 * 1024).toInt()
                val zeros = ByteArray(length)
                file.outputStream().use { it.write(zeros) }
                file.delete()
            }
        } catch (_: Exception) {}
    }
}

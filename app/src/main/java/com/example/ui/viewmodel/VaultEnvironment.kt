package com.example.ui.viewmodel

import android.content.Context
import com.example.data.model.VaultItem
import com.example.data.security.ClipboardSecurityManager
import com.example.data.security.CryptoUtils
import com.example.data.security.DbEncryptionManager
import com.example.data.security.RootKeyHub
import org.json.JSONArray
import org.json.JSONObject

/**
 * Abstracts platform security storage, RootKeyHub/SQLCipher/clipboard hooks, and JSON serialization
 * so [VaultCoordinator] remains testable in JVM unit tests without mocking Android framework stubs.
 */
interface VaultEnvironment {
    data class SecurityConfig(
        val saltB64: String? = null,
        val iterations: Int = CryptoUtils.DEFAULT_ITERATIONS,
        val algorithm: String? = null,
        val passHash: String? = null,
        val encryptedMnemonic: String? = null
    )

    data class BackupEnvelope(
        val versionedSaltB64: String?,
        val iterations: Int,
        val payload: String
    )

    fun readSecurityConfig(): SecurityConfig
    fun writeSecurityConfig(
        saltB64: String,
        iterations: Int,
        algorithm: String,
        passHash: String,
        encryptedMnemonic: String? = null
    )

    fun isRootOnboarded(): Boolean
    fun isRootUnlocked(): Boolean
    fun setupRootMasterKeys(mnemonic: String, pin: String, panicPin: String): Boolean
    fun verifyRootPin(enteredPin: String): RootKeyHub.PinResult
    fun lockRootNode()
    fun triggerRootPanicWipe()

    fun updateDbMasterPassphrase(passphrase: String)
    fun clearDbMemory()
    fun wipeClipboardNow()

    fun encodeBackupPayload(items: List<VaultItem>): String
    fun encodeBackupEnvelope(
        saltB64: String,
        iterations: Int,
        algorithm: String,
        encryptedPayload: String
    ): String

    fun parseBackupEnvelope(raw: String, defaultIterations: Int): BackupEnvelope
    fun parseBackupItems(decryptedJson: String): List<VaultItem>
}

class AndroidVaultEnvironment(
    private val context: Context
) : VaultEnvironment {

    private fun prefs() = context.getSharedPreferences("simplex_security", Context.MODE_PRIVATE)

    override fun readSecurityConfig(): VaultEnvironment.SecurityConfig {
        val sp = prefs()
        return VaultEnvironment.SecurityConfig(
            saltB64 = sp.getString("kdf_salt", null),
            iterations = sp.getInt("kdf_iterations", CryptoUtils.DEFAULT_ITERATIONS),
            algorithm = sp.getString("kdf_algorithm", null),
            passHash = sp.getString("pass_hash", null),
            encryptedMnemonic = sp.getString("encrypted_mnemonic", null)
        )
    }

    override fun writeSecurityConfig(
        saltB64: String,
        iterations: Int,
        algorithm: String,
        passHash: String,
        encryptedMnemonic: String?
    ) {
        prefs().edit()
            .putString("kdf_salt", saltB64)
            .putInt("kdf_iterations", iterations)
            .putString("kdf_algorithm", algorithm)
            .putString("pass_hash", passHash)
            .apply {
                if (encryptedMnemonic != null) {
                    putString("encrypted_mnemonic", encryptedMnemonic)
                }
            }
            .apply()
    }

    override fun isRootOnboarded(): Boolean = RootKeyHub.isOnboarded(context)

    override fun isRootUnlocked(): Boolean = RootKeyHub.isNodeUnlocked()

    override fun setupRootMasterKeys(mnemonic: String, pin: String, panicPin: String): Boolean =
        RootKeyHub.setupMasterKeys(context, mnemonic, pin, panicPin)

    override fun verifyRootPin(enteredPin: String): RootKeyHub.PinResult =
        RootKeyHub.verifyPin(context, enteredPin)

    override fun lockRootNode() = RootKeyHub.lockNode()

    override fun triggerRootPanicWipe() = RootKeyHub.triggerPanicWipe(context)

    override fun updateDbMasterPassphrase(passphrase: String) =
        DbEncryptionManager.updateMasterPassphrase(context, passphrase)

    override fun clearDbMemory() = DbEncryptionManager.clearMemory()

    override fun wipeClipboardNow() {
        ClipboardSecurityManager.wipeClipboardNow(context)
    }

    override fun encodeBackupPayload(items: List<VaultItem>): String {
        val databaseArray = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("type", item.type)
            obj.put("title", item.title)
            obj.put("username", item.username ?: "")
            obj.put("value", item.sensitiveValue)
            obj.put("notes", item.notes ?: "")
            obj.put("createdAt", item.createdAt)
            databaseArray.put(obj)
        }
        val payload = JSONObject()
        payload.put("version", 2)
        payload.put("client", "SimplexPrivacy")
        payload.put("vaultData", databaseArray)
        return payload.toString()
    }

    override fun encodeBackupEnvelope(
        saltB64: String,
        iterations: Int,
        algorithm: String,
        encryptedPayload: String
    ): String {
        return JSONObject().apply {
            put("salt", saltB64)
            put("iterations", iterations)
            put("kdf", algorithm)
            put("payload", encryptedPayload)
        }.toString()
    }

    override fun parseBackupEnvelope(raw: String, defaultIterations: Int): VaultEnvironment.BackupEnvelope {
        val envelope = try { JSONObject(raw) } catch (_: Exception) { null }
        return if (envelope != null && envelope.has("salt") && envelope.has("payload")) {
            VaultEnvironment.BackupEnvelope(
                versionedSaltB64 = envelope.getString("salt"),
                iterations = envelope.optInt("iterations", defaultIterations),
                payload = envelope.getString("payload")
            )
        } else {
            VaultEnvironment.BackupEnvelope(
                versionedSaltB64 = null,
                iterations = defaultIterations,
                payload = raw
            )
        }
    }

    override fun parseBackupItems(decryptedJson: String): List<VaultItem> {
        val payload = JSONObject(decryptedJson)
        val array = payload.getJSONArray("vaultData")
        val list = ArrayList<VaultItem>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                VaultItem(
                    type = obj.getString("type"),
                    title = obj.getString("title"),
                    username = obj.getString("username"),
                    sensitiveValue = obj.getString("value"),
                    notes = obj.getString("notes"),
                    createdAt = obj.getLong("createdAt")
                )
            )
        }
        return list
    }
}

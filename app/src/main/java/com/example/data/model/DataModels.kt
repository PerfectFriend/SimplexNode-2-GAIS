package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Vault items to store sensitive credentials, notes, recovery keys, and personal profiles.
 */
@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "CREDENTIAL", "PERSONAL_DATA", "NOTE", "ROUTING_KEY"
    val title: String, // Encrypted, or plain-text preview depending on search
    val username: String? = null, // Encrypted payload
    val sensitiveValue: String, // Encrypted payload (Password, onion key, or SSH credentials)
    val notes: String? = null, // Encrypted notes
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Historical activity logs for Simplex Server and file exchange movements.
 */
@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String, // "CONNECTION", "MESSAGE_SENT", "MESSAGE_RECEIVED", "FILE_UPLOAD", "FILE_DOWNLOAD", "TOR_ROUTE", "BACKUP"
    val description: String,
    val status: String // "SUCCESS", "PENDING", "FAILED"
)

/**
 * Simplex Chat server config / mock connections.
 * In Simplex, queue nodes (or servers) relay E2E messages without global user IDs.
 */
@Entity(tableName = "simplex_servers")
data class SimplexServer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serverName: String,
    val address: String, // onion, tailscale IP, or clearnet uri
    val status: String, // "CONNECTED", "DISCONNECTED", "UNREACHABLE"
    val latencyMs: Int = 0,
    val isTorOnion: Boolean = false,
    val isTailscale: Boolean = false,
    val channelId: String // simplex connection queue id
)

/**
 * Shared E2E File representation.
 */
@Entity(tableName = "shared_files")
data class SharedFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val sizeLabel: String,
    val encryptedPayload: String, // Base64 cipher block
    val senderAlias: String,
    val recipientAlias: String,
    val accessUrl: String, // generated tor/tailscale path
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val lastAccessTimestamp: Long = System.currentTimeMillis()
)

package com.example.data.model

// [REAL] Этап 9: Локальная модель узла флота ParanoidX (Fleet Command Center).
// Каждая нода флота детерминированно привязана к nodeIndex из RootKeyHub ("ParanoidX/Fleet/Node/{index}").
enum class FleetNodeStatus(val displayLabel: String) {
    ACTIVE_LOCAL("LOCAL ACTIVE"),
    DEGRADED_LOCAL("LOCAL STANDBY"),
    STANDBY_REMOTE("REMOTE • STANDBY (NO LINK)"),
    UNCONFIGURED_REMOTE("REMOTE • UNCONFIGURED")
}

data class FleetNodeEntry(
    val nodeIndex: Int,
    val label: String,
    val domainPath: String,
    val nodeId: String,
    val fingerprint: String,
    val identityLabel: String,
    val isLocal: Boolean,
    val transportHint: String,
    val createdAt: Long = System.currentTimeMillis()
)

package com.example.data.model

// [REAL] Этап 9–11: Модель узла флота PXNode (Fleet Command Center) с честным статусом удалённого линка.
// Каждая нода флота детерминированно привязана к nodeIndex из RootKeyHub ("ParanoidX/Fleet/Node/{index}").
enum class FleetNodeStatus(val displayLabel: String) {
    ACTIVE_LOCAL("LOCAL ACTIVE"),
    DEGRADED_LOCAL("LOCAL STANDBY"),
    LINK_REACHABLE("REMOTE • LINK OK"),
    LINK_UNREACHABLE("REMOTE • UNREACHABLE"),
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
    val remoteEndpoint: String = "",
    val linkProbeStatus: String = if (isLocal) "LOCAL" else "NOT_PROBED", // NOT_PROBED, REACHABLE, UNREACHABLE
    val lastProbeRttMs: Int? = null,
    val lastProbeDetail: String = "",
    val lastProbeTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

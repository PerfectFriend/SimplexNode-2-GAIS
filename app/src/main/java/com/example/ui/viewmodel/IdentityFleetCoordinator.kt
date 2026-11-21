package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import com.example.data.model.FleetNodeEntry
import com.example.data.security.RootKeyHub
import com.example.data.security.Socks5ProxyClient
import com.example.network.p2p.P2pCloudEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket

// [REAL] Этап 12 (Декомпозиция MainViewModel):
// Координатор криптоидентичности ноды (.onion v3), Dual-Mode Wallet (PROOF HMAC / SPEND secp256k1)
// и локального реестра флота (Fleet Command Center) с честным ручным зондированием и timestamp.
class IdentityFleetCoordinator(
    private val application: Application,
    private val scope: CoroutineScope,
    private val onLog: (actionType: String, description: String, status: String) -> Unit
) {

    private val _activeOnionAddress = MutableStateFlow("px_uninitialized_label.onion")
    val activeOnionAddress: StateFlow<String> = _activeOnionAddress.asStateFlow()

    private val _activeCanonicalOnionV3 = MutableStateFlow("")
    val activeCanonicalOnionV3: StateFlow<String> = _activeCanonicalOnionV3.asStateFlow()

    // --- Universal Wallet: PROOF mode (HMAC-SHA512 domain proof) ---
    private val _walletEvmAddress = MutableStateFlow("")
    val walletEvmAddress: StateFlow<String> = _walletEvmAddress.asStateFlow()

    private val _walletEvmFingerprint = MutableStateFlow("")
    val walletEvmFingerprint: StateFlow<String> = _walletEvmFingerprint.asStateFlow()

    private val _walletLastSignature = MutableStateFlow<RootKeyHub.WalletChallengeSignature?>(null)
    val walletLastSignature: StateFlow<RootKeyHub.WalletChallengeSignature?> = _walletLastSignature.asStateFlow()

    private val _walletSignatureVerified = MutableStateFlow<Boolean?>(null)
    val walletSignatureVerified: StateFlow<Boolean?> = _walletSignatureVerified.asStateFlow()

    // --- Universal Wallet: SPEND mode (true secp256k1 ECDSA) ---
    private val _walletSpendAddress = MutableStateFlow("")
    val walletSpendAddress: StateFlow<String> = _walletSpendAddress.asStateFlow()

    private val _walletSpendPublicKeyHex = MutableStateFlow("")
    val walletSpendPublicKeyHex: StateFlow<String> = _walletSpendPublicKeyHex.asStateFlow()

    private val _walletSpendFingerprint = MutableStateFlow("")
    val walletSpendFingerprint: StateFlow<String> = _walletSpendFingerprint.asStateFlow()

    private val _walletSpendLastSignature = MutableStateFlow<RootKeyHub.WalletSpendSignature?>(null)
    val walletSpendLastSignature: StateFlow<RootKeyHub.WalletSpendSignature?> = _walletSpendLastSignature.asStateFlow()

    private val _walletSpendSignatureVerified = MutableStateFlow<Boolean?>(null)
    val walletSpendSignatureVerified: StateFlow<Boolean?> = _walletSpendSignatureVerified.asStateFlow()

    // --- Fleet Registry ---
    private val _fleetNodes = MutableStateFlow<List<FleetNodeEntry>>(emptyList())
    val fleetNodes: StateFlow<List<FleetNodeEntry>> = _fleetNodes.asStateFlow()

    fun setUnitializedLabels() {
        _activeOnionAddress.value = "px_uninitialized_label.onion"
        _activeCanonicalOnionV3.value = ""
    }

    fun clearOnLockOrWipe() {
        _walletEvmAddress.value = ""
        _walletEvmFingerprint.value = ""
        _walletLastSignature.value = null
        _walletSignatureVerified.value = null
        _walletSpendAddress.value = ""
        _walletSpendPublicKeyHex.value = ""
        _walletSpendFingerprint.value = ""
        _walletSpendLastSignature.value = null
        _walletSpendSignatureVerified.value = null
        _fleetNodes.value = emptyList()
    }

    fun refreshFromHub(): Pair<String, String>? {
        if (!RootKeyHub.isNodeUnlocked()) return null
        return try {
            val appLabel = RootKeyHub.deriveTorOnionAddress()
            val canonicalV3 = RootKeyHub.deriveTorOnionV3Canonical()
            _activeOnionAddress.value = appLabel
            _activeCanonicalOnionV3.value = canonicalV3

            _walletEvmAddress.value = RootKeyHub.deriveWalletEvmAddress()
            _walletEvmFingerprint.value = RootKeyHub.deriveWalletEvmFingerprint()

            _walletSpendAddress.value = RootKeyHub.deriveWalletSpendAddress()
            _walletSpendPublicKeyHex.value = RootKeyHub.deriveWalletSpendPublicKeyHex()
            _walletSpendFingerprint.value = RootKeyHub.deriveWalletSpendFingerprint()

            loadOrInitializeFleetRegistry()
            Pair(appLabel, canonicalV3)
        } catch (e: Exception) {
            Log.e("IdentityFleetCoord", "Failed deriving wallet/fleet keys from RootKeyHub", e)
            null
        }
    }

    fun signWalletChallenge(challenge: String) {
        if (!RootKeyHub.isNodeUnlocked() || challenge.isBlank()) return
        try {
            val sig = RootKeyHub.signWalletChallenge(challenge)
            _walletLastSignature.value = sig
            _walletSignatureVerified.value = null
            onLog("WALLET_PROOF_SIGN", "Signed challenge via ParanoidX/Wallet/EVM/v1 (${sig.evmAddress.take(10)}...)", "SUCCESS")
        } catch (e: Exception) {
            onLog("WALLET_PROOF_SIGN", "Challenge signing failed: ${e.message}", "FAILED")
        }
    }

    fun verifyLastWalletChallenge(): Boolean {
        val last = _walletLastSignature.value ?: return false
        val ok = RootKeyHub.verifyWalletChallenge(last.challenge, last.signatureHex)
        _walletSignatureVerified.value = ok
        onLog("WALLET_PROOF_VERIFY", "Local PROOF signature check: ${if (ok) "VALID" else "INVALID"}", if (ok) "SUCCESS" else "FAILED")
        return ok
    }

    fun signWalletSpendChallenge(challenge: String) {
        if (!RootKeyHub.isNodeUnlocked() || challenge.isBlank()) return
        try {
            val sig = RootKeyHub.signWalletSpendChallenge(challenge)
            _walletSpendLastSignature.value = sig
            _walletSpendSignatureVerified.value = null
            onLog("WALLET_SPEND_SIGN", "Signed EIP-191 digest with secp256k1 ECDSA (${sig.evmAddress.take(10)}..., v=${sig.v})", "SUCCESS")
        } catch (e: Exception) {
            onLog("WALLET_SPEND_SIGN", "secp256k1 signing failed: ${e.message}", "FAILED")
        }
    }

    fun verifyLastWalletSpendChallenge(): Boolean {
        val last = _walletSpendLastSignature.value ?: return false
        val ok = RootKeyHub.verifyWalletSpendChallenge(last.challenge, last.signatureHex)
        _walletSpendSignatureVerified.value = ok
        onLog("WALLET_SPEND_VERIFY", "ECDSA secp256k1 verification: ${if (ok) "VALID" else "INVALID"}", if (ok) "SUCCESS" else "FAILED")
        return ok
    }

    private fun loadOrInitializeFleetRegistry() {
        val prefs = application.getSharedPreferences("paranoidx_fleet_registry", Application.MODE_PRIVATE)
        val rawJson = prefs.getString("fleet_nodes_json", null)
        val parsed = mutableListOf<FleetNodeEntry>()

        if (!rawJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(rawJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val idx = obj.getInt("nodeIndex")
                    val crypto = RootKeyHub.deriveFleetNodeInfo(idx)
                    parsed.add(
                        FleetNodeEntry(
                            nodeIndex = idx,
                            label = obj.optString("label", if (idx == 0) "Primary Sovereign Node" else "Logical Node #$idx"),
                            domainPath = crypto.domainPath,
                            nodeId = crypto.nodeId,
                            fingerprint = crypto.fingerprint,
                            identityLabel = crypto.identityLabel,
                            isLocal = idx == 0,
                            transportHint = obj.optString("transportHint", if (idx == 0) "Local Device (Tor + P2P)" else "Tor / Tailscale Standby"),
                            remoteEndpoint = obj.optString("remoteEndpoint", ""),
                            linkProbeStatus = obj.optString("linkProbeStatus", if (idx == 0) "LOCAL" else "NOT_PROBED"),
                            lastProbeRttMs = if (obj.has("lastProbeRttMs") && !obj.isNull("lastProbeRttMs")) obj.getInt("lastProbeRttMs") else null,
                            lastProbeDetail = obj.optString("lastProbeDetail", ""),
                            lastProbeTimestamp = if (obj.has("lastProbeTimestamp") && !obj.isNull("lastProbeTimestamp")) obj.getLong("lastProbeTimestamp") else null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("IdentityFleetCoord", "Error reading fleet registry JSON", e)
            }
        }

        if (parsed.none { it.nodeIndex == 0 }) {
            val primaryCrypto = RootKeyHub.deriveFleetNodeInfo(0)
            parsed.add(
                0,
                FleetNodeEntry(
                    nodeIndex = 0,
                    label = "Primary Sovereign Node",
                    domainPath = primaryCrypto.domainPath,
                    nodeId = primaryCrypto.nodeId,
                    fingerprint = primaryCrypto.fingerprint,
                    identityLabel = primaryCrypto.identityLabel,
                    isLocal = true,
                    transportHint = "Local Device (Tor + SimpleX + P2P)",
                    remoteEndpoint = "127.0.0.1:${P2pCloudEngine.serverPort}",
                    linkProbeStatus = "LOCAL"
                )
            )
        }

        _fleetNodes.value = parsed.sortedBy { it.nodeIndex }
        saveFleetRegistry(_fleetNodes.value)
    }

    fun addLogicalFleetNode(label: String, transportHint: String = "Tor / Tailscale Remote", remoteEndpoint: String = "") {
        if (!RootKeyHub.isNodeUnlocked()) return
        val current = _fleetNodes.value
        val nextIndex = (current.maxOfOrNull { it.nodeIndex } ?: 0) + 1
        val crypto = RootKeyHub.deriveFleetNodeInfo(nextIndex)
        val cleanLabel = label.trim().ifBlank { "Logical Node #$nextIndex" }
        val entry = FleetNodeEntry(
            nodeIndex = nextIndex,
            label = cleanLabel,
            domainPath = crypto.domainPath,
            nodeId = crypto.nodeId,
            fingerprint = crypto.fingerprint,
            identityLabel = crypto.identityLabel,
            isLocal = false,
            transportHint = transportHint.trim().ifBlank { "Tor / Tailscale Remote" },
            remoteEndpoint = remoteEndpoint.trim(),
            linkProbeStatus = "NOT_PROBED"
        )
        val updated = (current + entry).sortedBy { it.nodeIndex }
        _fleetNodes.value = updated
        saveFleetRegistry(updated)
        onLog("FLEET", "Provisioned logical node #$nextIndex ('$cleanLabel') via ${crypto.domainPath}", "SUCCESS")
    }

    fun probeRemoteFleetNode(
        nodeIndex: Int,
        endpointInput: String,
        torActive: Boolean,
        socksHost: String,
        socksPort: Int,
        socksAuthMode: String,
        socksUser: String,
        socksPass: String,
        hostPortParser: (String) -> Pair<String, Int>
    ) {
        val cleanEndpoint = endpointInput.trim()
        val nowMs = System.currentTimeMillis()
        if (cleanEndpoint.isBlank()) {
            _fleetNodes.value = _fleetNodes.value.map {
                if (it.nodeIndex == nodeIndex) {
                    it.copy(
                        linkProbeStatus = "UNCONFIGURED",
                        lastProbeDetail = "No host:port endpoint",
                        lastProbeTimestamp = nowMs
                    )
                } else it
            }
            return
        }

        scope.launch {
            val (host, port) = hostPortParser(cleanEndpoint)
            val startNs = System.nanoTime()
            val result: Triple<String, Int?, String> = withContext(Dispatchers.IO) {
                try {
                    if (host.endsWith(".onion")) {
                        if (!torActive) {
                            return@withContext Triple("UNREACHABLE", null, "TOR_REQUIRED for .onion endpoint")
                        }
                        val ok = Socks5ProxyClient.testConnection(
                            proxyHost = socksHost,
                            proxyPort = socksPort,
                            authMode = socksAuthMode,
                            username = socksUser,
                            password = socksPass,
                            targetHost = host,
                            targetPort = port,
                            dnsLeakProtection = true,
                            callback = object : Socks5ProxyClient.LogCallback {
                                override fun onLog(message: String) {}
                            }
                        )
                        val rtt = ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceAtLeast(1)
                        if (ok) Triple("REACHABLE", rtt, "Tor SOCKS5 tunnel OK")
                        else Triple("UNREACHABLE", null, "SOCKS5 tunnel refused")
                    } else {
                        Socket().use { sock ->
                            sock.connect(InetSocketAddress(host, port), 2500)
                        }
                        val rtt = ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceAtLeast(1)
                        Triple("REACHABLE", rtt, "TCP handshake $host:$port")
                    }
                } catch (e: Exception) {
                    Triple("UNREACHABLE", null, e.message ?: "Connect failed")
                }
            }

            val probeTs = System.currentTimeMillis()
            val updated = _fleetNodes.value.map {
                if (it.nodeIndex == nodeIndex) {
                    it.copy(
                        remoteEndpoint = cleanEndpoint,
                        linkProbeStatus = result.first,
                        lastProbeRttMs = result.second,
                        lastProbeDetail = result.third,
                        lastProbeTimestamp = probeTs
                    )
                } else it
            }
            _fleetNodes.value = updated
            saveFleetRegistry(updated)
            onLog(
                "FLEET_PROBE",
                "Node #$nodeIndex ($cleanEndpoint): ${result.first}${result.second?.let { " ${it}ms" } ?: ""} (${result.third})",
                if (result.first == "REACHABLE") "SUCCESS" else "FAILED"
            )
        }
    }

    fun removeLogicalFleetNode(nodeIndex: Int) {
        if (nodeIndex == 0) return
        val updated = _fleetNodes.value.filterNot { it.nodeIndex == nodeIndex }
        _fleetNodes.value = updated
        saveFleetRegistry(updated)
        onLog("FLEET", "Removed logical node #$nodeIndex from local fleet registry", "SUCCESS")
    }

    private fun saveFleetRegistry(nodes: List<FleetNodeEntry>) {
        val arr = JSONArray()
        nodes.forEach { n ->
            arr.put(
                JSONObject().apply {
                    put("nodeIndex", n.nodeIndex)
                    put("label", n.label)
                    put("transportHint", n.transportHint)
                    put("remoteEndpoint", n.remoteEndpoint)
                    put("linkProbeStatus", n.linkProbeStatus)
                    if (n.lastProbeRttMs != null) put("lastProbeRttMs", n.lastProbeRttMs)
                    put("lastProbeDetail", n.lastProbeDetail)
                    if (n.lastProbeTimestamp != null) put("lastProbeTimestamp", n.lastProbeTimestamp)
                    put("createdAt", n.createdAt)
                }
            )
        }
        application
            .getSharedPreferences("paranoidx_fleet_registry", Application.MODE_PRIVATE)
            .edit()
            .putString("fleet_nodes_json", arr.toString())
            .apply()
    }
}

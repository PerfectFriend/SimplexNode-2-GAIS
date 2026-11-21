package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ActivityLog
import com.example.network.vpn.VpnConfigParser
import com.example.network.vpn.VpnPingResult
import com.example.network.vpn.VpnProfile
import com.example.network.vpn.VpnProtocolManager
import com.example.network.vpn.VpnProtocolType
import com.example.service.ParanoidVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// [REAL] Этап 12 (Декомпозиция MainViewModel + VPN Suite Honesty):
// Выделенный ViewModel управления VPN-профилями, системным TUN и честной TCP-диагностикой.
class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val logDao = AppDatabase.getDatabase(application).activityLogDao()

    val vpnProfiles: StateFlow<List<VpnProfile>> = VpnProtocolManager.profiles
    val selectedVpnProfile: StateFlow<VpnProfile?> = VpnProtocolManager.selectedProfile
    val vpnLogs: StateFlow<List<String>> = VpnProtocolManager.vpnLogs
    val vpnLastPingResult: StateFlow<VpnPingResult?> = VpnProtocolManager.lastPingResult

    val isVpnConnected: StateFlow<Boolean> = ParanoidVpnService.isVpnConnected
    val activeVpnProtocol: StateFlow<VpnProtocolType> = ParanoidVpnService.activeProtocol
    val vpnThroughput: StateFlow<String> = ParanoidVpnService.currentThroughput
    val vpnBytesIn: StateFlow<Long> = ParanoidVpnService.bytesIn
    val vpnBytesOut: StateFlow<Long> = ParanoidVpnService.bytesOut
    val vpnDuration: StateFlow<Long> = ParanoidVpnService.connectionDurationSeconds
    val vpnStatusDetail: StateFlow<String> = ParanoidVpnService.vpnStatusDetail
    val vpnEngineMode: StateFlow<String> = ParanoidVpnService.vpnEngineMode

    fun prepareVpnConsentIntent(): Intent? {
        return try {
            VpnService.prepare(getApplication())
        } catch (_: Exception) {
            null
        }
    }

    fun selectVpnProfile(profile: VpnProfile) {
        VpnProtocolManager.selectProfile(profile)
    }

    fun connectVpn(profile: VpnProfile) {
        VpnProtocolManager.startVpnTunnel(getApplication(), profile)
        recordLog("VPN_CONNECT", "Requested TUN interface for ${profile.protocol.displayName} (${profile.serverAddress}:${profile.serverPort})", "PENDING")
    }

    fun disconnectVpn() {
        VpnProtocolManager.stopVpnTunnel(getApplication())
        recordLog("VPN_DISCONNECT", "VPN TUN interface closed by user", "SUCCESS")
    }

    fun pingVpnProfile(profile: VpnProfile) {
        viewModelScope.launch {
            val res = VpnProtocolManager.probeEndpoint(profile)
            if (res.reachable) {
                recordLog("VPN_PING", "TCP handshake OK ${res.host}:${res.port} (${res.rttMs}ms)", "SUCCESS")
            } else {
                recordLog("VPN_PING", "TCP probe FAILED ${res.host}:${res.port} (${res.detail})", "FAILED")
            }
        }
    }

    fun importCustomVpnProfile(name: String, protocol: VpnProtocolType, rawConfig: String) {
        val parsed = when (protocol) {
            VpnProtocolType.WIREGUARD -> VpnConfigParser.parseWireGuardConfig(rawConfig, name)
            VpnProtocolType.OPENVPN -> VpnConfigParser.parseOpenVpnConfig(rawConfig, name)
            VpnProtocolType.SHADOWSOCKS -> VpnConfigParser.parseShadowsocksUri(rawConfig, name)
            else -> VpnProfile(
                name = name,
                protocol = protocol,
                serverAddress = "127.0.0.1",
                serverPort = protocol.defaultPort,
                rawConfig = rawConfig
            )
        }
        VpnProtocolManager.addCustomProfile(parsed)
        recordLog("VPN_IMPORT", "Imported ${protocol.displayName} profile '$name' (${parsed.serverAddress}:${parsed.serverPort})", "SUCCESS")
    }

    fun markPermissionDenied() {
        ParanoidVpnService.vpnStatusDetail.value = "UNAVAILABLE • User denied OS VpnService consent"
        VpnProtocolManager.log("OS VpnService consent denied by user")
        recordLog("VPN_PERMISSION", "User denied OS VPN consent", "FAILED")
    }

    private fun recordLog(actionType: String, description: String, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                logDao.insertLog(ActivityLog(actionType = actionType, description = description, status = status))
            } catch (_: Exception) {
            }
        }
    }
}

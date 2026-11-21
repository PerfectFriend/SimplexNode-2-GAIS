package com.example.network.vpn

import android.content.Context
import android.content.Intent
import com.example.service.ParanoidVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

import java.net.InetSocketAddress
import java.net.Socket

// [REAL] Этап 12: Оркестратор VPN-профилей с честной TCP-проверкой endpoint-а (без random/delay).
// [SIM] Собственные нативные движки WireGuard/OpenVPN не вшиты — ParanoidVpnService поднимает системный TUN без user-space шифрующего демона.
object VpnProtocolManager {

    private val _profiles = MutableStateFlow<List<VpnProfile>>(VpnConfigParser.getSampleProfiles())
    val profiles = _profiles.asStateFlow()

    private val _selectedProfile = MutableStateFlow<VpnProfile?>(VpnConfigParser.getSampleProfiles().first())
    val selectedProfile = _selectedProfile.asStateFlow()

    private val _vpnLogs = MutableStateFlow<List<String>>(emptyList())
    val vpnLogs = _vpnLogs.asStateFlow()

    private val _lastPingResult = MutableStateFlow<VpnPingResult?>(null)
    val lastPingResult = _lastPingResult.asStateFlow()

    fun log(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val entry = "[$time] [VPN] $msg"
        _vpnLogs.value = listOf(entry) + _vpnLogs.value.take(49)
    }

    fun selectProfile(profile: VpnProfile) {
        _selectedProfile.value = profile
        log("Selected profile: ${profile.name} (${profile.protocol.displayName})")
    }

    fun addCustomProfile(profile: VpnProfile) {
        _profiles.value = _profiles.value + profile
        _selectedProfile.value = profile
        log("Added custom VPN profile: ${profile.name} (${profile.protocol.displayName})")
    }

    fun deleteProfile(profileId: String) {
        _profiles.value = _profiles.value.filter { it.id != profileId }
        if (_selectedProfile.value?.id == profileId) {
            _selectedProfile.value = _profiles.value.firstOrNull()
        }
        log("Deleted VPN profile: $profileId")
    }

    /**
     * Dispatches intent to ParanoidVpnService to request OS TUN interface creation.
     */
    fun startVpnTunnel(context: Context, profile: VpnProfile) {
        log("CONNECT: Requesting OS TUN interface for ${profile.name} (${profile.protocol.displayName})...")
        val intent = Intent(context, ParanoidVpnService::class.java).apply {
            action = ParanoidVpnService.ACTION_CONNECT
            putExtra(ParanoidVpnService.EXTRA_PROFILE_NAME, profile.name)
            putExtra(ParanoidVpnService.EXTRA_PROTOCOL, profile.protocol.name)
            putExtra(ParanoidVpnService.EXTRA_SERVER, profile.serverAddress)
            putExtra(ParanoidVpnService.EXTRA_PORT, profile.serverPort)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        log("TUN intent dispatched for ${profile.serverAddress}:${profile.serverPort} (note: user-space WG/OVPN engine not bundled).")
    }

    fun stopVpnTunnel(context: Context) {
        log("DISCONNECT: Terminating VPN TUN interface...")
        val intent = Intent(context, ParanoidVpnService::class.java).apply {
            action = ParanoidVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
        log("Tunnel closed. System routing restored.")
    }

    /**
     * Performs an honest TCP connect latency probe against serverAddress:serverPort.
     * Never invents random ping values; returns VpnPingResult(reachable=false, rttMs=null) on failure.
     */
    suspend fun probeEndpoint(profile: VpnProfile, timeoutMs: Int = 2500): VpnPingResult = withContext(Dispatchers.IO) {
        log("TCP probe -> ${profile.serverAddress}:${profile.serverPort}...")
        val startNs = System.nanoTime()
        val result = try {
            Socket().use { sock ->
                sock.connect(InetSocketAddress(profile.serverAddress, profile.serverPort), timeoutMs)
            }
            val measuredMs = ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceAtLeast(1)
            VpnPingResult(
                profileId = profile.id,
                host = profile.serverAddress,
                port = profile.serverPort,
                reachable = true,
                rttMs = measuredMs,
                detail = "TCP handshake OK"
            )
        } catch (e: Exception) {
            VpnPingResult(
                profileId = profile.id,
                host = profile.serverAddress,
                port = profile.serverPort,
                reachable = false,
                rttMs = null,
                detail = e.message ?: "Connection failed"
            )
        }

        _lastPingResult.value = result
        val updatedPing = result.rttMs ?: -1
        _profiles.value = _profiles.value.map {
            if (it.id == profile.id) it.copy(lastPingMs = updatedPing) else it
        }
        if (_selectedProfile.value?.id == profile.id) {
            _selectedProfile.value = _selectedProfile.value?.copy(lastPingMs = updatedPing)
        }

        if (result.reachable) {
            log("REACHABLE ${profile.serverAddress}:${profile.serverPort} (${result.rttMs}ms)")
        } else {
            log("UNREACHABLE ${profile.serverAddress}:${profile.serverPort} (${result.detail})")
        }
        result
    }

    suspend fun pingEndpoint(profile: VpnProfile, timeoutMs: Int = 2500): Int {
        return probeEndpoint(profile, timeoutMs).rttMs ?: -1
    }
}

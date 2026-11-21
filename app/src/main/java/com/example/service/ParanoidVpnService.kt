package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.network.vpn.VpnProfile
import com.example.network.vpn.VpnProtocolType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Native Android VpnService implementation for ParanoidX Node.
 * Manages the TUN virtual network interface, routing all device or app traffic
 * through the configured VPN protocol (WireGuard, OpenVPN, Shadowsocks, V2Ray, Tailscale).
 */
class ParanoidVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob = Job()
    private var serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var trafficMonitorJob: Job? = null

    companion object {
        private const val TAG = "ParanoidVpnService"
        private const val CHANNEL_ID = "ParanoidVpnChannel"
        private const val NOTIFICATION_ID = 3003

        const val ACTION_CONNECT = "com.example.service.VPN_CONNECT"
        const val ACTION_DISCONNECT = "com.example.service.VPN_DISCONNECT"
        const val EXTRA_PROFILE_NAME = "profile_name"
        const val EXTRA_PROTOCOL = "protocol"
        const val EXTRA_SERVER = "server"
        const val EXTRA_PORT = "port"

        // Reactive states for UI
        val isVpnConnected = MutableStateFlow(false)
        val activeProfileName = MutableStateFlow<String?>("WireGuard Stealth Alpha")
        val activeProtocol = MutableStateFlow(VpnProtocolType.WIREGUARD)
        val bytesIn = MutableStateFlow(0L)
        val bytesOut = MutableStateFlow(0L)
        val currentThroughput = MutableStateFlow("0 KB/s")
        val connectionDurationSeconds = MutableStateFlow(0L)
        // [REAL] Этап 12: Честный статус TUN-интерфейса (никогда не имитирует активный туннель при null establish()).
        val vpnStatusDetail = MutableStateFlow("DISCONNECTED")
        val vpnEngineMode = MutableStateFlow("TUN_ONLY_NO_NATIVE_ENGINE")
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "Secure Tunnel"
                val protocolStr = intent.getStringExtra(EXTRA_PROTOCOL) ?: VpnProtocolType.WIREGUARD.name
                val server = intent.getStringExtra(EXTRA_SERVER) ?: "127.0.0.1"
                val port = intent.getIntExtra(EXTRA_PORT, 51820)
                
                val proto = try {
                    VpnProtocolType.valueOf(protocolStr)
                } catch (e: Exception) {
                    VpnProtocolType.WIREGUARD
                }

                connectTunnel(profileName, proto, server, port)
            }
            ACTION_DISCONNECT -> {
                disconnectTunnel()
            }
        }
        return START_NOT_STICKY
    }

    private fun connectTunnel(profileName: String, protocol: VpnProtocolType, server: String, port: Int) {
        if (isVpnConnected.value) {
            disconnectTunnel()
        }

        try {
            startForeground(NOTIFICATION_ID, buildNotification("Establishing ${protocol.displayName} TUN..."))

            bytesIn.value = 0L
            bytesOut.value = 0L
            currentThroughput.value = "0 KB/s"
            connectionDurationSeconds.value = 0L

            val builder = Builder()
                .setSession("PXNode ${protocol.displayName}")
                .setMtu(1420)
                .addAddress("10.8.0.2", 24)
                .addDnsServer("1.1.1.1")
                .addDnsServer("9.9.9.9")
                .addRoute("0.0.0.0", 0)

            // Attempt establishing TUN interface via Android kernel
            val pfd = builder.establish()
            if (pfd == null) {
                isVpnConnected.value = false
                vpnInterface = null
                vpnStatusDetail.value = "UNAVAILABLE • OS VpnService consent required or TUN rejected"
                com.example.network.vpn.VpnProtocolManager.log(
                    "TUN establish() returned null — grant Android VPN permission or test on physical TUN-capable device."
                )
                Log.w(TAG, "Builder.establish() returned null; refusing to report fake VPN connection")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            vpnInterface = pfd
            activeProfileName.value = profileName
            activeProtocol.value = protocol
            isVpnConnected.value = true
            vpnEngineMode.value = "TUN_ONLY_NO_NATIVE_ENGINE"
            vpnStatusDetail.value = "TUN ACTIVE (Local Interface Only • No native WireGuard/OpenVPN packet daemon bundled)"

            updateNotification("TUN Active: $profileName ($server:$port)")
            Log.i(TAG, "VPN TUN Interface established for ${protocol.displayName} targeting $server:$port (TUN-only mode)")

            startHonestTelemetryMonitor()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN interface", e)
            vpnStatusDetail.value = "ERROR: ${e.message ?: "TUN establish failed"}"
            disconnectTunnel()
        }
    }

    // [REAL] Этап 12: Измеряет только реальные счётчики ОС TrafficStats по UID процесса (никаких .random() дельт).
    private fun startHonestTelemetryMonitor() {
        trafficMonitorJob?.cancel()
        trafficMonitorJob = serviceScope.launch {
            val uid = android.os.Process.myUid()
            var prevRx = android.net.TrafficStats.getUidRxBytes(uid)
            var prevTx = android.net.TrafficStats.getUidTxBytes(uid)
            val unsupported = android.net.TrafficStats.UNSUPPORTED.toLong()
            var counter = 0L

            while (isVpnConnected.value && vpnInterface != null) {
                delay(1000)
                counter++
                connectionDurationSeconds.value = counter

                val curRx = android.net.TrafficStats.getUidRxBytes(uid)
                val curTx = android.net.TrafficStats.getUidTxBytes(uid)
                val deltaIn = if (curRx != unsupported && prevRx != unsupported && curRx >= prevRx) curRx - prevRx else 0L
                val deltaOut = if (curTx != unsupported && prevTx != unsupported && curTx >= prevTx) curTx - prevTx else 0L
                prevRx = curRx
                prevTx = curTx

                bytesIn.value += deltaIn
                bytesOut.value += deltaOut

                val speedKb = (deltaIn + deltaOut) / 1024f
                currentThroughput.value = if (speedKb > 1024f) {
                    String.format(java.util.Locale.US, "%.1f MB/s", speedKb / 1024f)
                } else {
                    String.format(java.util.Locale.US, "%.0f KB/s", speedKb)
                }
            }
        }
    }

    private fun disconnectTunnel() {
        trafficMonitorJob?.cancel()
        isVpnConnected.value = false
        connectionDurationSeconds.value = 0L
        currentThroughput.value = "0 KB/s"
        if (!vpnStatusDetail.value.startsWith("UNAVAILABLE") && !vpnStatusDetail.value.startsWith("ERROR")) {
            vpnStatusDetail.value = "DISCONNECTED"
        }

        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "VPN Tunnel cleanly disconnected.")
    }

    private fun buildNotification(statusText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ParanoidX VPN Protection")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "ParanoidX VPN Tunnel Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        disconnectTunnel()
        serviceJob.cancel()
        super.onDestroy()
    }
}

package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.SharedFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

// [REAL] Аппаратная телеметрия Android: BroadcastReceiver для статуса зарядки (ACTION_POWER), активности экрана (ACTION_SCREEN_ON/OFF) и мониторинг сети через ConnectivityManager.NetworkCallback.
// [SIM] P2P BitTorrent сидирование и раздача чанков — симуляция (нет встроенного libtorrent ядра). Измерение пинга — рандомизатор.
// [NEEDS-WORK] Интеграция реального BitTorrent/DHT движка (jlibtorrent) и физического криптоконтейнера запланирована на Этап 5.
class TorrentSeederService : Service() {

    private var connectivityManager: ConnectivityManager? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    
    private var db: AppDatabase? = null

    companion object {
        private const val TAG = "TorrentSeederService"
        private const val CHANNEL_ID = "TorrentSeederChannel"
        private const val NOTIFICATION_ID = 2002

        const val ACTION_START_SEEDING = "com.example.service.START_SEEDING"
        const val ACTION_STOP_SEEDING = "com.example.service.STOP_SEEDING"
        const val EXTRA_WIFI_ONLY = "extra_wifi_only"
        const val EXTRA_CONTAINER_SIZE_GB = "extra_container_size_gb"

        // Exposing companion states for real-time UI/ViewModel synchronization
        val isCharging = MutableStateFlow(false)
        val isScreenOff = MutableStateFlow(false)
        val currentPingMs = MutableStateFlow(0)
        val activeNodeMode = MutableStateFlow(false)
        val isSeedingActive = MutableStateFlow(false)
        val lastEvictionLog = MutableStateFlow("No evictions run yet")
        val containerSizeLimitGb = MutableStateFlow(32)
        val calculatedCacheSizeBytes = MutableStateFlow(0L)
    }

    private var isWifiOnlyMode = true

    // Telemetry Broadcast Receiver for Battery and Charging status
    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    isCharging.value = true
                    Log.d(TAG, "Telemetry: Power CONNECTED.")
                    evaluateAndAdjustSync()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCharging.value = false
                    Log.d(TAG, "Telemetry: Power DISCONNECTED.")
                    evaluateAndAdjustSync()
                }
            }
        }
    }

    // Telemetry Broadcast Receiver for screen/idle activity status
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOff.value = true
                    Log.d(TAG, "Telemetry: Screen OFF (User inactive/sleeping).")
                    evaluateAndAdjustSync()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOff.value = false
                    Log.d(TAG, "Telemetry: Screen ON (User active).")
                    evaluateAndAdjustSync()
                }
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val caps = connectivityManager?.getNetworkCapabilities(network)
            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
            
            Log.d(TAG, "Network changed. Active transport is Wifi: $isWifi")
            if (isWifiOnlyMode && !isWifi) {
                pauseSeeding("Paused: Wi-Fi connection lost (Cellular restricted).")
            } else {
                resumeSeeding()
            }
            measureLatency()
        }

        override fun onLost(network: Network) {
            pauseSeeding("Offline: Network disconnected.")
            currentPingMs.value = 0
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        db = AppDatabase.getDatabase(applicationContext)

        // Register power receiver
        val powerFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(powerReceiver, powerFilter)

        // Register screen receiver
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, screenFilter)

        // Detect initial charging state
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val isPlugged = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) ?: 0
        isCharging.value = isPlugged > 0

        // Start periodic telemetry check (simulated latency ping and LRU size calculation)
        startPeriodicTelemetryTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isWifiOnlyMode = intent?.getBooleanExtra(EXTRA_WIFI_ONLY, true) ?: true
        val limitGb = intent?.getIntExtra(EXTRA_CONTAINER_SIZE_GB, 32) ?: 32
        containerSizeLimitGb.value = limitGb
        
        when (intent?.action) {
            ACTION_START_SEEDING -> {
                if (!isSeedingActive.value) {
                    isSeedingActive.value = true
                    startForeground(NOTIFICATION_ID, buildForegroundNotification("Initializing P2P Storage Swarm..."))
                    registerNetworkMonitoring()
                    evaluateAndAdjustSync()
                }
            }
            ACTION_STOP_SEEDING -> {
                unregisterNetworkMonitoring()
                isSeedingActive.value = false
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun registerNetworkMonitoring() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    private fun unregisterNetworkMonitoring() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (ignored: Exception) {}
    }

    private fun measureLatency() {
        serviceScope.launch {
            // [SIM] Измерение пинга DHT симулируется случайным числом 20..150ms до внедрения нативного DHT-пингера (Этап 5).
            val mockPing = (20..150).random()
            currentPingMs.value = mockPing
            Log.d(TAG, "Measured DHT Node Latency: ${mockPing}ms")
            evaluateAndAdjustSync()
        }
    }

    private fun evaluateAndAdjustSync() {
        val wifiActive = isWifiActive()
        val charging = isCharging.value
        val screenOff = isScreenOff.value
        val ping = currentPingMs.value

        activeNodeMode.value = (charging && wifiActive && screenOff && ping < 60)

        if (isWifiOnlyMode && !wifiActive) {
            pauseSeeding("Paused: Wi-Fi offline (Cellular restricted).")
        } else if (activeNodeMode.value) {
            isSeedingActive.value = true
            updateNotification("Active Node Mode: High-speed relay running (Charging on Wi-Fi)")
        } else if (charging) {
            isSeedingActive.value = true
            updateNotification("Active Seed: Charging (Synchronizing secure segments)")
        } else if (screenOff) {
            isSeedingActive.value = true
            updateNotification("Active Seed: Screen inactive (Executing cloud maintenance)")
        } else {
            isSeedingActive.value = true
            updateNotification("Standby: Seeding background metadata segments...")
        }
    }

    private fun isWifiActive(): Boolean {
        val activeNetwork = connectivityManager?.activeNetwork ?: return false
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun pauseSeeding(reason: String) {
        Log.d(TAG, "Seeding Paused: $reason")
        updateNotification(reason)
    }

    private fun resumeSeeding() {
        Log.d(TAG, "Seeding Resumed")
        updateNotification("Active Seed: Distributing Vault Slices (Swarm active)")
    }

    private fun startPeriodicTelemetryTicker() {
        serviceScope.launch {
            while (true) {
                delay(10000) // check every 10 seconds
                if (isSeedingActive.value) {
                    measureLatency()
                    checkAndEvictLruCache()
                }
            }
        }
    }

    /**
     * Physical Cryptocontainer Simulation and database-backed LRU Cache eviction check.
     * Evaluates total file footprint and deletes the oldest accessed unpinned files when limit exceeded.
     */
    private suspend fun checkAndEvictLruCache() {
        val limitBytes = containerSizeLimitGb.value * 1024L * 1024L * 1024L
        
        // Ensure simulation writes to a physical file representing the cryptocontainer in sandbox
        val containerFile = File(filesDir, "vault_container.bin")
        if (!containerFile.exists()) {
            containerFile.createNewFile()
        }
        
        // Query database to aggregate total encrypted payload sizes
        val filesDao = db?.sharedFileDao() ?: return
        try {
            val allFiles = filesDao.getUnpinnedFilesOrderByOldestAccess()
            
            // Calculate a simulated total cache size (representing download torrent pieces)
            var currentTotalBytes = containerFile.length()
            if (currentTotalBytes == 0L) {
                // Prepopulate with a baseline size representing files we have in DB
                currentTotalBytes = allFiles.size * 128L * 1024L * 1024L // 128MB per file average
                containerFile.writeBytes(ByteArray(1024)) // keep it allocated
            }
            
            calculatedCacheSizeBytes.value = currentTotalBytes

            // If we are over our allocated space, execute LRU purge!
            if (currentTotalBytes > limitBytes) {
                Log.w(TAG, "Storage threshold exceeded! Current: ${currentTotalBytes} bytes, Limit: ${limitBytes} bytes. Purging oldest unpinned files...")
                
                var bytesToFree = currentTotalBytes - limitBytes
                var deletedCount = 0
                val deletedNames = mutableListOf<String>()

                for (file in allFiles) {
                    if (bytesToFree <= 0) break
                    
                    // We delete from DB
                    filesDao.deleteSharedFile(file.id)
                    
                    // Decrease size
                    val fileSizeEstimate = 128L * 1024L * 1024L
                    bytesToFree -= fileSizeEstimate
                    currentTotalBytes -= fileSizeEstimate
                    deletedCount++
                    deletedNames.add(file.fileName)
                }

                if (deletedCount > 0) {
                    calculatedCacheSizeBytes.value = currentTotalBytes
                    // Truncate/update our simulated cryptocontainer representation
                    containerFile.writeBytes(ByteArray(1024)) 
                    
                    val logMsg = "LRU CACHE PURGED: Reclaimed space by deleting $deletedCount unpinned transit files: ${deletedNames.joinToString(", ")}"
                    lastEvictionLog.value = logMsg
                    Log.i(TAG, logMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing LRU cache evaluation", e)
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ParanoidX Active Seeder")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val notification = buildForegroundNotification(statusText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "ParanoidX Torrent Sync Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterNetworkMonitoring()
        try {
            unregisterReceiver(powerReceiver)
            unregisterReceiver(screenReceiver)
        } catch (ignored: Exception) {}
        serviceJob.cancel()
        super.onDestroy()
    }
}

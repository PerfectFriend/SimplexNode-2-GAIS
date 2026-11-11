package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.p2p.P2pBlockStorage
import com.example.network.p2p.P2pCloudEngine
import com.example.service.TorrentSeederService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// [REAL] Этап 6 (Бонус) — Декомпозиция God-object: P2pViewModel управляет состоянием P2P облака, криптоконтейнера и сидера.
class P2pViewModel(application: Application) : AndroidViewModel(application) {

    val isSeedingActive: StateFlow<Boolean> = TorrentSeederService.isSeedingActive.asStateFlow()
    val activeNodeMode: StateFlow<Boolean> = TorrentSeederService.activeNodeMode.asStateFlow()
    val currentPingMs: StateFlow<Int> = TorrentSeederService.currentPingMs.asStateFlow()
    val cacheSizeBytes: StateFlow<Long> = TorrentSeederService.calculatedCacheSizeBytes.asStateFlow()
    val containerSizeLimitGb: StateFlow<Int> = TorrentSeederService.containerSizeLimitGb.asStateFlow()
    val lastEvictionLog: StateFlow<String> = TorrentSeederService.lastEvictionLog.asStateFlow()

    val downloadSpeed: StateFlow<String> = P2pCloudEngine.downloadSpeed
    val uploadSpeed: StateFlow<String> = P2pCloudEngine.uploadSpeed
    val nodeStatus: StateFlow<P2pCloudEngine.NodeStatus> = P2pCloudEngine.nodeStatus
    val activePeers: StateFlow<List<String>> = P2pCloudEngine.activePeers
    val cloudLogs: StateFlow<List<String>> = P2pCloudEngine.cloudLogs

    fun toggleSeeding(enabled: Boolean, wifiOnly: Boolean = true) {
        val app = getApplication<Application>()
        if (enabled) {
            val intent = Intent(app, TorrentSeederService::class.java).apply {
                action = TorrentSeederService.ACTION_START_SEEDING
                putExtra(TorrentSeederService.EXTRA_WIFI_ONLY, wifiOnly)
                putExtra(TorrentSeederService.EXTRA_CONTAINER_SIZE_GB, containerSizeLimitGb.value)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                app.startForegroundService(intent)
            } else {
                app.startService(intent)
            }
        } else {
            val intent = Intent(app, TorrentSeederService::class.java).apply {
                action = TorrentSeederService.ACTION_STOP_SEEDING
            }
            app.startService(intent)
        }
    }

    fun setContainerSizeGb(sizeGb: Int) {
        TorrentSeederService.containerSizeLimitGb.value = sizeGb
    }
}

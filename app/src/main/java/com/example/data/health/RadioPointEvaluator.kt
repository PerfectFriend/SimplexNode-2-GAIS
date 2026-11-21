package com.example.data.health

// [REAL] Этап 10: Честный детерминированный оценщик состояния здоровья ноды («Радиоточка»).
// [HONEST-TELEMETRY] Никаких генераторов случайных чисел: сигнал эфира зависит строго от реального
// состояния сети (ConnectivityManager), статуса Tor-демона, VPN и P2P-транспорта.

enum class RadioSignalState(
    val code: String,
    val badgeLabel: String,
    val isLiveSignal: Boolean
) {
    ON_AIR_CLEAR("ON_AIR", "ON AIR", true),
    DEGRADED_CLEARNET_ONLY("DEGRADED", "DEGRADED", true),
    SILENCE_OFFLINE("SILENCE", "SILENCE", false),
    ALARM_ERROR("ALARM", "ALARM", false)
}

data class RadioPointSnapshot(
    val state: RadioSignalState,
    val isNetworkAvailable: Boolean,
    val isWifi: Boolean,
    val torStatus: String,
    val isVpnConnected: Boolean,
    val seederActive: Boolean,
    val activePeers: Int,
    val headlineRu: String,
    val detailRu: String,
    val evaluatedAtMs: Long = System.currentTimeMillis()
)

object RadioPointEvaluator {

    fun evaluate(
        isNetworkAvailable: Boolean,
        isWifi: Boolean,
        torStatus: String,
        isVpnConnected: Boolean,
        seederActive: Boolean,
        activePeers: Int,
        nowMs: Long = System.currentTimeMillis()
    ): RadioPointSnapshot {
        val normalizedTor = torStatus.trim().uppercase()

        val state: RadioSignalState
        val headline: String
        val detail: String

        when {
            normalizedTor == "ERROR" -> {
                state = RadioSignalState.ALARM_ERROR
                headline = "ТРЕВОГА • Сбой Tor-демона"
                detail = "Критическая ошибка маршрутизатора Tor. Передача в анонимный эфир остановлена."
            }
            !isNetworkAvailable -> {
                state = RadioSignalState.SILENCE_OFFLINE
                headline = "ТИШИНА • Нет сети"
                detail = "Устройство оффлайн (Wi-Fi / сотовая сеть недоступны). Несущая радиоточки отключена."
            }
            normalizedTor == "CONNECTED" -> {
                state = RadioSignalState.ON_AIR_CLEAR
                val transportNote = buildString {
                    append(if (isWifi) "Wi-Fi" else "Network")
                    if (isVpnConnected) append(" + VPN")
                    if (seederActive) append(" • P2P($activePeers)")
                }
                headline = "ЭФИР ЧИСТ • Нода на связи"
                detail = "Сеть активна и Tor SOCKS5 готов ($transportNote)."
            }
            else -> {
                state = RadioSignalState.DEGRADED_CLEARNET_ONLY
                val torDesc = when {
                    normalizedTor.startsWith("BOOTSTRAPPING") -> "Tor поднимает цепь ($normalizedTor)"
                    normalizedTor == "STARTING" || normalizedTor.startsWith("EXTRACTING") -> "Tor запускается ($normalizedTor)"
                    normalizedTor == "SIMULATION" -> "Tor работает в fallback-режиме ($normalizedTor)"
                    else -> "Tor отключён ($normalizedTor)"
                }
                headline = "ДЕГРАДАЦИЯ • Сеть без Tor"
                detail = "Физическая сеть доступна, но $torDesc."
            }
        }

        return RadioPointSnapshot(
            state = state,
            isNetworkAvailable = isNetworkAvailable,
            isWifi = isWifi,
            torStatus = torStatus,
            isVpnConnected = isVpnConnected,
            seederActive = seederActive,
            activePeers = activePeers,
            headlineRu = headline,
            detailRu = detail,
            evaluatedAtMs = nowMs
        )
    }
}

package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.health.RadioPointSnapshot
import com.example.data.health.RadioSignalState
import com.example.ui.theme.LocalParanoidPalette
import kotlin.math.PI
import kotlin.math.sin

// [REAL] Этап 10: Визуально-звуковой индикатор здоровья ноды «Радиоточка».
// [HONEST-TELEMETRY] Отрисовка волны и статус строго привязаны к RadioSignalState без random-имитации.

@Composable
fun RadioPointTopBarChip(
    snapshot: RadioPointSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalParanoidPalette.current
    val signalColor = signalColorFor(snapshot.state, palette.accent, palette.warning, palette.danger, palette.textMuted)

    val infiniteTransition = rememberInfiniteTransition(label = "radio_chip_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (snapshot.state == RadioSignalState.SILENCE_OFFLINE) 0.45f else 0.35f,
        targetValue = if (snapshot.state == RadioSignalState.SILENCE_OFFLINE) 0.45f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (snapshot.state) {
                    RadioSignalState.ALARM_ERROR -> 420
                    RadioSignalState.ON_AIR_CLEAR -> 1100
                    else -> 1500
                }
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(signalColor.copy(alpha = 0.14f))
            .border(1.dp, signalColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .testTag("radio_point_chip")
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(signalColor.copy(alpha = pulseAlpha))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = when (snapshot.state) {
                RadioSignalState.ON_AIR_CLEAR -> Icons.Default.Sensors
                RadioSignalState.DEGRADED_CLEARNET_ONLY -> Icons.Default.WifiTetheringError
                RadioSignalState.SILENCE_OFFLINE -> Icons.Default.SensorsOff
                RadioSignalState.ALARM_ERROR -> Icons.Default.Warning
            },
            contentDescription = "Radio Point Health",
            tint = signalColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = snapshot.state.badgeLabel,
            color = signalColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun RadioPointDetailDialog(
    snapshot: RadioPointSnapshot,
    audioBeaconEnabled: Boolean,
    onToggleAudioBeacon: (Boolean) -> Unit,
    onTestBeaconPulse: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalParanoidPalette.current
    val signalColor = signalColorFor(snapshot.state, palette.accent, palette.warning, palette.danger, palette.textMuted)

    val phaseTransition = rememberInfiniteTransition(label = "radio_wave_phase")
    val wavePhase by phaseTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (snapshot.state) {
                    RadioSignalState.ON_AIR_CLEAR -> 1400
                    RadioSignalState.DEGRADED_CLEARNET_ONLY -> 2400
                    RadioSignalState.ALARM_ERROR -> 650
                    RadioSignalState.SILENCE_OFFLINE -> 3000
                },
                easing = LinearEasing
            )
        ),
        label = "wave_phase"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, signalColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .testTag("radio_point_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Radio Point",
                            tint = signalColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "РАДИОТОЧКА • ЭФИР НОДЫ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = palette.textPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Surface(
                        color = signalColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = snapshot.state.badgeLabel,
                            color = signalColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Deterministic Carrier Waveform Canvas (NO Random)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.background)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val midY = size.height / 2f
                        val width = size.width

                        // Center baseline
                        drawLine(
                            color = palette.border,
                            start = Offset(0f, midY),
                            end = Offset(width, midY),
                            strokeWidth = 1.dp.toPx()
                        )

                        when (snapshot.state) {
                            RadioSignalState.SILENCE_OFFLINE -> {
                                // Flatline silence: zero carrier
                                drawLine(
                                    color = signalColor,
                                    start = Offset(0f, midY),
                                    end = Offset(width, midY),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                            RadioSignalState.ON_AIR_CLEAR, RadioSignalState.DEGRADED_CLEARNET_ONLY -> {
                                val amp = if (snapshot.state == RadioSignalState.ON_AIR_CLEAR) size.height * 0.34f else size.height * 0.15f
                                val cycles = if (snapshot.state == RadioSignalState.ON_AIR_CLEAR) 3.5f else 2.0f
                                val path = Path()
                                val steps = 64
                                for (i in 0..steps) {
                                    val t = i.toFloat() / steps.toFloat()
                                    val x = t * width
                                    val y = midY + sin((t * cycles * 2f * PI.toFloat()) + wavePhase) * amp
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(
                                    path = path,
                                    color = signalColor,
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            RadioSignalState.ALARM_ERROR -> {
                                // Alarm pulse square/spike bars
                                val barCount = 12
                                val barW = width / (barCount * 2f)
                                for (i in 0 until barCount) {
                                    val x = i * (barW * 2f) + barW / 2f
                                    val phasePulse = ((sin(wavePhase + i * 0.7f) + 1f) / 2f)
                                    val barH = size.height * (0.25f + 0.55f * phasePulse)
                                    drawLine(
                                        color = signalColor,
                                        start = Offset(x, midY - barH / 2f),
                                        end = Offset(x, midY + barH / 2f),
                                        strokeWidth = barW.coerceAtLeast(3.dp.toPx()),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = snapshot.headlineRu,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = signalColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = snapshot.detailRu,
                    fontSize = 11.sp,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Real telemetry signals list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.background)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    TelemetryFactRow(
                        label = "Сеть ОС (ConnectivityManager)",
                        value = if (snapshot.isNetworkAvailable) (if (snapshot.isWifi) "ONLINE (Wi-Fi)" else "ONLINE (Network)") else "OFFLINE",
                        ok = snapshot.isNetworkAvailable
                    )
                    TelemetryFactRow(
                        label = "Tor SOCKS5 демон",
                        value = snapshot.torStatus,
                        ok = snapshot.torStatus == "CONNECTED"
                    )
                    TelemetryFactRow(
                        label = "VPN туннель",
                        value = if (snapshot.isVpnConnected) "ACTIVE" else "INACTIVE",
                        ok = snapshot.isVpnConnected
                    )
                    TelemetryFactRow(
                        label = "P2P Swarm",
                        value = if (snapshot.seederActive) "READY (${snapshot.activePeers} peers)" else "STOPPED",
                        ok = snapshot.activePeers > 0
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Optional audio tone beacon toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Звуковой маяк эфира",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Короткий тональный сигнал при проверке статуса",
                            fontSize = 10.sp,
                            color = palette.textSecondary
                        )
                    }
                    Switch(
                        checked = audioBeaconEnabled,
                        onCheckedChange = onToggleAudioBeacon,
                        modifier = Modifier.testTag("switch_radio_audio_beacon")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onTestBeaconPulse,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_probe_radio_beacon"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = signalColor)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Проверить сигнал", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_close_radio_dialog")
                    ) {
                        Text("Закрыть", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryFactRow(label: String, value: String, ok: Boolean) {
    val palette = LocalParanoidPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, color = palette.textSecondary)
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (ok) palette.accent else palette.warning
        )
    }
}

fun signalColorFor(
    state: RadioSignalState,
    accent: Color,
    warning: Color,
    danger: Color,
    muted: Color
): Color = when (state) {
    RadioSignalState.ON_AIR_CLEAR -> accent
    RadioSignalState.DEGRADED_CLEARNET_ONLY -> warning
    RadioSignalState.SILENCE_OFFLINE -> muted
    RadioSignalState.ALARM_ERROR -> danger
}

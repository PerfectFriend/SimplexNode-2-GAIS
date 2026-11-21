package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FleetNodeEntry
import com.example.data.model.FleetNodeStatus
import com.example.ui.components.InAppGboardKeyboard
import com.example.ui.theme.LocalParanoidPalette
import com.example.ui.viewmodel.MainViewModel

// [REAL] Этап 9/10: Fleet Command Center — Штаб управления локальным флотом нод ParanoidX.
// [SECURITY-CRITICAL] Все узлы #0..N детерминированно выводятся из единого Master Seed (ParanoidX/Fleet/Node/{index}).
// [HONEST-TELEMETRY] Удалённые ноды без реального сетевого канала честно маркируются как STANDBY (NO LINK).
// [AIR-GAP] Ввод имени и транспорта новой ноды выполняется строго через встроенную клавиатуру InAppGboardKeyboard.

@Composable
fun FleetScreen(viewModel: MainViewModel) {
    val palette = LocalParanoidPalette.current
    val fleetNodes by viewModel.fleetNodes.collectAsState()
    val torStatus by viewModel.torStatus.collectAsState()
    val seederStatus by viewModel.torrentSeedingEnabled.collectAsState()
    val activePeers by viewModel.activeTorrentPeersCount.collectAsState()
    val simplexServers by viewModel.simplexServers.collectAsState()

    val clipboardManager = LocalClipboardManager.current

    var copiedNodeIndex by remember { mutableStateOf<Int?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var probeTargetNode by remember { mutableStateOf<FleetNodeEntry?>(null) }

    val primaryLocalStatus = if (torStatus == "CONNECTED" || activePeers > 0) {
        FleetNodeStatus.ACTIVE_LOCAL
    } else {
        FleetNodeStatus.DEGRADED_LOCAL
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. FLEET COMMAND CENTER OVERVIEW
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.secondaryAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(palette.secondaryAccent.copy(alpha = 0.14f))
                                    .border(1.dp, palette.secondaryAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = "Fleet Command Center",
                                    tint = palette.secondaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "FLEET COMMAND CENTER",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = palette.textPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Зарегистрировано нод: ${fleetNodes.size} (Node #0..#${(fleetNodes.maxOfOrNull { it.nodeIndex } ?: 0)})",
                                    fontSize = 11.sp,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        Surface(
                            color = palette.surfaceElevated,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "HKDF-SHA512",
                                color = palette.accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Honest Live Telemetry of Primary Node #0
                    Text(
                        text = "ТЕЛЕМЕТРИЯ ЛОКАЛЬНОЙ НОДЫ #0",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryChip(
                            label = "TOR",
                            value = torStatus,
                            isPositive = torStatus == "CONNECTED",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryChip(
                            label = "P2P SWARM",
                            value = if (seederStatus) "READY ($activePeers)" else "IDLE",
                            isPositive = activePeers > 0,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryChip(
                            label = "SIMPLEX",
                            value = "${simplexServers.size} RELAYS",
                            isPositive = simplexServers.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Примечание: px_…node.onion — детерминированный identity-label приложения. Удалённые слоты без активного линка честно отображаются в режиме STANDBY.",
                        fontSize = 10.sp,
                        color = palette.textMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_open_add_fleet_node")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Add Logical Node",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Добавить логическую ноду (Next Index)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Empty / Single-node guidance state
        if (fleetNodes.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.warning.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = palette.warning)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Реестр флота заблокирован. Разблокируйте ноду PIN-кодом для вывода Node #0..N.",
                            fontSize = 12.sp,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        } else if (fleetNodes.size == 1) {
            item {
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = palette.secondaryAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Активен только локальный узел #0. Добавьте логическую ноду #1+, чтобы сгенерировать заготовку под remote-узел из того же seed.",
                            fontSize = 11.sp,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }

        // 2. FLEET NODE LIST
        items(fleetNodes, key = { it.nodeIndex }) { node ->
            val honestStatus = when {
                node.isLocal -> primaryLocalStatus
                node.linkProbeStatus == "REACHABLE" -> FleetNodeStatus.LINK_REACHABLE
                node.linkProbeStatus == "UNREACHABLE" -> FleetNodeStatus.LINK_UNREACHABLE
                else -> FleetNodeStatus.STANDBY_REMOTE
            }
            FleetNodeCard(
                node = node,
                status = honestStatus,
                isCopied = copiedNodeIndex == node.nodeIndex,
                onCopyFingerprint = {
                    clipboardManager.setText(AnnotatedString("${node.nodeId} | ${node.fingerprint} | ${node.identityLabel}"))
                    copiedNodeIndex = node.nodeIndex
                },
                onProbeLink = {
                    if (node.remoteEndpoint.isNotBlank()) {
                        viewModel.probeRemoteFleetNode(node.nodeIndex, node.remoteEndpoint)
                    } else {
                        probeTargetNode = node
                    }
                },
                onConfigureEndpoint = {
                    probeTargetNode = node
                },
                onRemove = {
                    viewModel.removeLogicalFleetNode(node.nodeIndex)
                }
            )
        }
    }

    if (showAddDialog) {
        AddLogicalFleetNodeDialog(
            nextIndex = (fleetNodes.maxOfOrNull { it.nodeIndex } ?: 0) + 1,
            onDismiss = { showAddDialog = false },
            onConfirm = { label, transportHint, endpoint ->
                viewModel.addLogicalFleetNode(label, transportHint, endpoint)
                showAddDialog = false
            }
        )
    }

    probeTargetNode?.let { target ->
        ProbeFleetEndpointDialog(
            node = target,
            onDismiss = { probeTargetNode = null },
            onProbe = { endpoint ->
                viewModel.probeRemoteFleetNode(target.nodeIndex, endpoint)
                probeTargetNode = null
            }
        )
    }
}

@Composable
private fun TelemetryChip(
    label: String,
    value: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalParanoidPalette.current
    val accent = if (isPositive) palette.accent else palette.warning
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.headerBg)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = palette.textMuted,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 10.sp,
            color = accent,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FleetNodeCard(
    node: FleetNodeEntry,
    status: FleetNodeStatus,
    isCopied: Boolean,
    onCopyFingerprint: () -> Unit,
    onProbeLink: () -> Unit,
    onConfigureEndpoint: () -> Unit,
    onRemove: () -> Unit
) {
    val palette = LocalParanoidPalette.current
    val statusColor = when (status) {
        FleetNodeStatus.ACTIVE_LOCAL -> palette.accent
        FleetNodeStatus.DEGRADED_LOCAL -> palette.secondaryAccent
        FleetNodeStatus.LINK_REACHABLE -> palette.accent
        FleetNodeStatus.LINK_UNREACHABLE -> palette.danger
        FleetNodeStatus.STANDBY_REMOTE -> palette.warning
        FleetNodeStatus.UNCONFIGURED_REMOTE -> palette.textMuted
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .testTag("fleet_node_card_${node.nodeIndex}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.14f))
                            .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${node.nodeIndex}",
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = node.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = node.domainPath,
                            fontSize = 10.sp,
                            color = palette.secondaryAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.border(0.5.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = status.displayLabel,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.headerBg)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "NODE ID: ${node.nodeId}",
                    fontSize = 10.sp,
                    color = palette.textPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "FINGERPRINT: ${node.fingerprint}",
                    fontSize = 10.sp,
                    color = palette.accent,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "IDENTITY LABEL (NON-WIRE): ${node.identityLabel}",
                    fontSize = 10.sp,
                    color = palette.textSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "TRANSPORT HINT: ${node.transportHint}",
                    fontSize = 10.sp,
                    color = palette.textMuted,
                    fontFamily = FontFamily.Monospace
                )
                if (node.remoteEndpoint.isNotBlank() || node.lastProbeDetail.isNotBlank() || node.lastProbeTimestamp != null) {
                    val tsSuffix = node.lastProbeTimestamp?.let { ts ->
                        val fmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                        " • ${fmt.format(java.util.Date(ts))} (Manual)"
                    } ?: ""
                    Text(
                        text = "LINK PROBE: ${node.remoteEndpoint.ifBlank { "not set" }} • ${node.linkProbeStatus}${node.lastProbeRttMs?.let { " (${it}ms)" } ?: ""}$tsSuffix",
                        fontSize = 10.sp,
                        color = if (node.linkProbeStatus == "REACHABLE") palette.accent else palette.textMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyFingerprint,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_copy_fleet_fp_${node.nodeIndex}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy Node Identity",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isCopied) "Скопировано" else "Copy ID / FP", fontSize = 12.sp)
                }

                if (!node.isLocal) {
                    OutlinedButton(
                        onClick = onProbeLink,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_probe_fleet_node_${node.nodeIndex}"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.secondaryAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Probe Remote Link",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Probe", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = onConfigureEndpoint,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_edit_fleet_endpoint_${node.nodeIndex}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsEthernet,
                            contentDescription = "Configure Endpoint",
                            tint = palette.textSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_remove_fleet_node_${node.nodeIndex}"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.danger)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove Logical Node",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddLogicalFleetNodeDialog(
    nextIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (label: String, transportHint: String, remoteEndpoint: String) -> Unit
) {
    val palette = LocalParanoidPalette.current
    var label by remember { mutableStateOf("Relay Node #$nextIndex") }
    var transportHint by remember { mutableStateOf("Tor Hidden Relay") }
    var remoteEndpoint by remember { mutableStateOf("") }
    var activeField by remember { mutableStateOf("label") } // "label", "transport", "endpoint"

    val transportPresets = listOf(
        "Tor Hidden Relay",
        "Tailscale Mesh Node",
        "Air-Gap Cold Node"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, palette.border, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "НОВАЯ ЛОГИЧЕСКАЯ НОДА #$nextIndex",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = palette.accent,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ключ и отпечаток будут детерминированно выведены по пути ParanoidX/Fleet/Node/$nextIndex.",
                    fontSize = 11.sp,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Название узла", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) activeField = "label" }
                        .clickable { activeField = "label" }
                        .testTag("fleet_node_label_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = transportHint,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Транспортная заготовка (Hint)", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) activeField = "transport" }
                        .clickable { activeField = "transport" }
                        .testTag("fleet_node_transport_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = remoteEndpoint,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Remote Endpoint (host:port / .onion, опц.)", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) activeField = "endpoint" }
                        .clickable { activeField = "endpoint" }
                        .testTag("fleet_node_endpoint_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    transportPresets.forEach { preset ->
                        FilterChip(
                            selected = transportHint == preset,
                            onClick = { transportHint = preset },
                            label = { Text(preset, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_cancel_add_fleet_node")
                    ) {
                        Text("Отмена", color = palette.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(label, transportHint, remoteEndpoint) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_confirm_add_fleet_node")
                    ) {
                        Text("Создать #$nextIndex", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val currentText = when (activeField) {
                    "label" -> label
                    "transport" -> transportHint
                    else -> remoteEndpoint
                }
                InAppGboardKeyboard(
                    text = currentText,
                    onTextChange = { updated ->
                        when (activeField) {
                            "label" -> label = updated
                            "transport" -> transportHint = updated
                            else -> remoteEndpoint = updated
                        }
                    },
                    onSendOrDone = { onConfirm(label, transportHint, remoteEndpoint) },
                    sendButtonLabel = "ADD",
                    modifier = Modifier.testTag("fleet_add_inapp_keyboard")
                )
            }
        }
    }
}

@Composable
private fun ProbeFleetEndpointDialog(
    node: FleetNodeEntry,
    onDismiss: () -> Unit,
    onProbe: (endpoint: String) -> Unit
) {
    val palette = LocalParanoidPalette.current
    var endpoint by remember { mutableStateOf(node.remoteEndpoint.ifBlank { "127.0.0.1:9050" }) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, palette.border, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PROBE REMOTE LINK • NODE #${node.nodeIndex}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = palette.secondaryAccent,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Реальная проверка TCP/SOCKS5 доступности без симуляции.",
                    fontSize = 11.sp,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = endpoint,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Endpoint (host:port)", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fleet_probe_endpoint_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Отмена", color = palette.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onProbe(endpoint) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.secondaryAccent),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_confirm_probe_fleet")
                    ) {
                        Text("Проверить", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                InAppGboardKeyboard(
                    text = endpoint,
                    onTextChange = { endpoint = it },
                    onSendOrDone = { onProbe(endpoint) },
                    sendButtonLabel = "PROBE",
                    modifier = Modifier.testTag("fleet_probe_inapp_keyboard")
                )
            }
        }
    }
}

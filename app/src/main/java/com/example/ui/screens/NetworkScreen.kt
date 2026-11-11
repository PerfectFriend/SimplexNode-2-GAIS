package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.vpn.VpnProfile
import com.example.network.vpn.VpnProtocolType
import com.example.network.smp.SmpProtocolEngine
import com.example.network.xftp.XftpProtocolEngine
import com.example.ui.theme.Localization
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(viewModel: MainViewModel) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Tor state
    val torEnabled by viewModel.torEnabled.collectAsState()
    val torStatus by viewModel.torStatus.collectAsState()
    val torLatency by viewModel.torLatency.collectAsState()
    val torStability by viewModel.torStability.collectAsState()
    val onionAddress by viewModel.simplexOnionAddress.collectAsState()
    val torCircuitNodes by viewModel.torCircuitNodes.collectAsState()
    val currentExitIp by viewModel.currentExitIp.collectAsState()
    val torEngineLogs by viewModel.torEngineLogs.collectAsState()

    // SOCKS5 state
    val socks5ProxyEnabled by viewModel.socks5ProxyEnabled.collectAsState()
    val socks5ProxyHost by viewModel.socks5ProxyHost.collectAsState()
    val socks5ProxyPort by viewModel.socks5ProxyPort.collectAsState()
    val socks5AuthMode by viewModel.socks5AuthMode.collectAsState()
    val socks5Username by viewModel.socks5Username.collectAsState()
    val socks5Password by viewModel.socks5Password.collectAsState()
    val socks5DnsLeakProtection by viewModel.socks5DnsLeakProtection.collectAsState()
    val socks5Logs by viewModel.socks5Logs.collectAsState()
    val isTestingSocks5 by viewModel.isTestingSocks5.collectAsState()

    // VPN state
    val vpnProfiles by viewModel.vpnProfiles.collectAsState()
    val selectedVpnProfile by viewModel.selectedVpnProfile.collectAsState()
    val isVpnConnected by viewModel.isVpnConnected.collectAsState()
    val activeVpnProtocol by viewModel.activeVpnProtocol.collectAsState()
    val vpnThroughput by viewModel.vpnThroughput.collectAsState()
    val vpnBytesIn by viewModel.vpnBytesIn.collectAsState()
    val vpnBytesOut by viewModel.vpnBytesOut.collectAsState()
    val vpnDuration by viewModel.vpnDuration.collectAsState()
    val vpnLogs by viewModel.vpnLogs.collectAsState()

    // SMP state
    val smpQueues by viewModel.smpQueues.collectAsState()
    val smpServers by viewModel.smpServers.collectAsState()
    val smpLogs by viewModel.smpLogs.collectAsState()

    // XFTP state
    val xftpTransfers by viewModel.xftpTransfers.collectAsState()
    val xftpLogs by viewModel.xftpLogs.collectAsState()

    // Sub-tab selection: "tor", "vpn", "smp", "xftp", "logs"
    var activeSubTab by remember { mutableStateOf("tor") }

    // Dialog states
    var showImportVpnDialog by remember { mutableStateOf(false) }
    var showCreateQueueDialog by remember { mutableStateOf(false) }
    var showSendSmpDialog by remember { mutableStateOf<SmpProtocolEngine.SmpQueue?>(null) }
    var smpMessageInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // App Title Section
        Column(modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)) {
            Text(
                text = Localization.get("net_title", currentLanguage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Privacy Router: Tor, SimpleX (SMP/XFTP) & Multi-Protocol VPNs",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Sub-Tab Navigation Bar
        ScrollableTabRow(
            selectedTabIndex = when (activeSubTab) {
                "tor" -> 0
                "vpn" -> 1
                "smp" -> 2
                "xftp" -> 3
                "logs" -> 4
                else -> 0
            },
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("network_sub_tabs")
        ) {
            Tab(
                selected = activeSubTab == "tor",
                onClick = { activeSubTab = "tor" },
                icon = { Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(16.dp)) },
                text = { Text("TOR / ONION", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("net_sub_tab_tor")
            )
            Tab(
                selected = activeSubTab == "vpn",
                onClick = { activeSubTab = "vpn" },
                icon = { Icon(Icons.Outlined.VpnLock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                text = { Text("VPN SUITE", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("net_sub_tab_vpn")
            )
            Tab(
                selected = activeSubTab == "smp",
                onClick = { activeSubTab = "smp" },
                icon = { Icon(Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(16.dp)) },
                text = { Text("SMP QUEUES", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("net_sub_tab_smp")
            )
            Tab(
                selected = activeSubTab == "xftp",
                onClick = { activeSubTab = "xftp" },
                icon = { Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp)) },
                text = { Text("XFTP CHUNKS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("net_sub_tab_xftp")
            )
            Tab(
                selected = activeSubTab == "logs",
                onClick = { activeSubTab = "logs" },
                icon = { Icon(Icons.Outlined.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) },
                text = { Text("LOGS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("net_sub_tab_logs")
            )
        }

        // Active Tab Screen Content
        Box(modifier = Modifier.weight(1f)) {
            when (activeSubTab) {
                // ==========================================
                // 1. TOR & SOCKS5 TAB
                // ==========================================
                "tor" -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Security,
                                                contentDescription = "Tor Proxy",
                                                tint = if (torEnabled) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Tor Onion Routing Daemon",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Status: $torStatus | Exit IP: ${currentExitIp ?: "Isolated"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 10.sp,
                                                    color = if (torStatus == "CONNECTED") Color(0xFF00E676) else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = torEnabled,
                                            onCheckedChange = { viewModel.toggleTor(it) },
                                            modifier = Modifier.testTag("tor_proxy_switch")
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Onion Address Section
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "LOCAL HIDDEN SERVICE (.ONION)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = onionAddress,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { clipboardManager.setText(AnnotatedString(onionAddress)) },
                                                modifier = Modifier.size(32.dp).testTag("copy_onion_address")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy address",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Circuit Node Path Section
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "TOR MULTI-HOP CIRCUIT ENCRYPTION",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            torCircuitNodes.forEachIndexed { index, node ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    val role = when (index) {
                                                        0 -> "GUARD"
                                                        1 -> "MIDDLE"
                                                        else -> "EXIT"
                                                    }
                                                    Text(role, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    Text(node, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                                                }
                                                if (index < torCircuitNodes.size - 1) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowForward,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.outline,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action buttons: Rotate NEWNYM & Launch Orbot
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.rotateTorIdentity() },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("rotate_tor_identity_button")
                                        ) {
                                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Rotate Circuit", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("org.torproject.android"))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.torproject.android"))
                                                    try { context.startActivity(marketIntent) } catch (_: Exception) {}
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("launch_orbot_button")
                                        ) {
                                            Icon(Icons.Outlined.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Orbot Manager", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // SOCKS5 Configuration & Handshake Diagnostic
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("SOCKS5 Proxy Socket Engine", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Switch(
                                            checked = socks5ProxyEnabled,
                                            onCheckedChange = { viewModel.toggleSocks5Proxy(it) },
                                            modifier = Modifier.testTag("socks5_proxy_switch")
                                        )
                                    }

                                    AnimatedVisibility(visible = socks5ProxyEnabled) {
                                        Column {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = socks5ProxyHost,
                                                    onValueChange = { viewModel.setSocks5Host(it) },
                                                    label = { Text("Proxy Host", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(2f),
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                                )
                                                OutlinedTextField(
                                                    value = socks5ProxyPort.toString(),
                                                    onValueChange = { viewModel.setSocks5Port(it.toIntOrNull() ?: 9050) },
                                                    label = { Text("Port", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("DNS Leak Isolation", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text("Resolve DNS remotely inside Tor", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                                }
                                                Switch(
                                                    checked = socks5DnsLeakProtection,
                                                    onCheckedChange = { viewModel.toggleSocks5DnsLeakProtection(it) }
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Button(
                                                onClick = { viewModel.runSocks5HandshakeTest() },
                                                enabled = !isTestingSocks5,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().testTag("socks5_test_button")
                                            ) {
                                                if (isTestingSocks5) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Performing RFC-1928 Handshake...", fontSize = 11.sp)
                                                } else {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Verify SOCKS5 Handshake (Port $socks5ProxyPort)", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 2. VPN SUITE TAB (WireGuard, OpenVPN, Shadowsocks, V2Ray, Tailscale)
                // ==========================================
                "vpn" -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Live VPN Status Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isVpnConnected) Color(0xFF0F291E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("vpn_status_card")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isVpnConnected) Color(0xFF00E676) else Color(0xFFFF5252))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = if (isVpnConnected) "VPN TUNNEL ACTIVE" else "VPN TUNNEL DISCONNECTED",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isVpnConnected) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Protocol: ${activeVpnProtocol.displayName}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // Connect/Disconnect Button
                                        Button(
                                            onClick = {
                                                if (isVpnConnected) {
                                                    viewModel.disconnectVpn()
                                                } else {
                                                    selectedVpnProfile?.let { viewModel.connectVpn(it) }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isVpnConnected) Color(0xFFFF5252) else Color(0xFF00E676),
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("vpn_toggle_button")
                                        ) {
                                            Text(
                                                text = if (isVpnConnected) "Disconnect" else "Connect",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Live Metrics Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("THROUGHPUT", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                            Text(if (isVpnConnected) vpnThroughput else "0 KB/s", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                        Column {
                                            Text("BYTES IN", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                            Text("${vpnBytesIn / 1024} KB", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                        Column {
                                            Text("BYTES OUT", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                            Text("${vpnBytesOut / 1024} KB", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                        Column {
                                            Text("UPTIME", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                            val mins = vpnDuration / 60
                                            val secs = vpnDuration % 60
                                            Text(String.format("%02d:%02d", mins, secs), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }

                        // Profiles Section Header & Import Button
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AVAILABLE PROTOCOL ENDPOINTS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { showImportVpnDialog = true },
                                    modifier = Modifier.testTag("import_vpn_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import Profile", fontSize = 11.sp)
                                }
                            }
                        }

                        // List of VPN Profiles
                        items(vpnProfiles) { profile ->
                            val isSelected = selectedVpnProfile?.id == profile.id
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.selectVpnProfile(profile) }
                                    .testTag("vpn_profile_${profile.protocol.name}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.selectVpnProfile(profile) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                                    Text(profile.protocol.displayName, fontSize = 8.sp)
                                                }
                                            }
                                            Text(
                                                text = "${profile.serverAddress}:${profile.serverPort}",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = { viewModel.pingVpnProfile(profile) },
                                            modifier = Modifier.testTag("ping_profile_${profile.id}")
                                        ) {
                                            Icon(Icons.Outlined.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ping", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 3. SMP PROTOCOL TAB (SimpleX Messaging)
                // ==========================================
                "smp" -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("SMP Unidirectional Queues", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Metadata-free decoupled queues over Tor", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Button(
                                            onClick = { showCreateQueueDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("create_smp_queue_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("New Queue", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Active SMP Queues
                        items(smpQueues) { queue ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("smp_queue_${queue.queueId.take(8)}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Badge(containerColor = if (queue.role == "RECEIVE") Color(0xFF00E676) else Color(0xFF00B0FF)) {
                                                Text(queue.role, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Queue: ${queue.queueId.take(12)}...", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteSmpQueue(queue.queueId) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Relay: ${queue.serverHost}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    Text("Status: ${queue.lastDeliveryStatus} | Messages: ${queue.messageCount}", fontSize = 10.sp, color = if (queue.lastDeliveryStatus == "DELIVERED" || queue.lastDeliveryStatus == "REGISTERED") Color(0xFF00E676) else MaterialTheme.colorScheme.primary)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                showSendSmpDialog = queue
                                                smpMessageInput = ""
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.testTag("send_smp_packet_${queue.queueId.take(8)}")
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Send Envelope", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // SMP Servers List
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("SMP RELAY CLUSTER", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        items(smpServers) { server ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(server.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("${server.address}:${server.port} | Onion: ${server.isOnion}", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Badge(containerColor = when (server.status) {
                                        "ONLINE" -> Color(0xFF00E676)
                                        "ERROR" -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.outline
                                    }) {
                                        Text(server.status, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (server.pingMs > 0) {
                                        Text("${server.pingMs}ms", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.pingSmpRelay(server) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Ping Relay", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 4. XFTP TRANSFER TAB (SimpleX File Transfer)
                // ==========================================
                "xftp" -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("XFTP Chunk Segmentation Engine", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("64KB blocks with SHA-256 Merkle root verification", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            // Segment a sample encrypted payload
                                            val samplePayload = "PARANOID_PAYLOAD_TEST_DATA_${System.currentTimeMillis()}".repeat(2000).toByteArray()
                                            viewModel.createXftpTransfer("confidential_payload.bin", samplePayload)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("xftp_chunk_demo_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Chunk & Segment Payload (64KB blocks)", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        items(xftpTransfers) { manifest ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("xftp_manifest_${manifest.transferId.take(8)}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(manifest.fileName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Size: ${manifest.totalBytes} B | Chunks: ${manifest.totalChunks}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Badge(containerColor = if (manifest.status == "COMPLETED") Color(0xFF00E676) else MaterialTheme.colorScheme.primaryContainer) {
                                            Text(manifest.status, fontSize = 8.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Merkle Root: ${manifest.rootHash.take(16)}...", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { clipboardManager.setText(AnnotatedString(manifest.xftpUri)) },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy URI", fontSize = 10.sp)
                                        }

                                        Button(
                                            onClick = { viewModel.uploadXftpTransfer(manifest) },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f).testTag("broadcast_xftp_button")
                                        ) {
                                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Broadcast Chunks", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 5. LIVE CONSOLE LOGS TAB
                // ==========================================
                "logs" -> {
                    var logFilter by remember { mutableStateOf("ALL") }
                    val allLogs = remember(torEngineLogs, vpnLogs, smpLogs, xftpLogs, socks5Logs, logFilter) {
                        when (logFilter) {
                            "TOR" -> torEngineLogs
                            "VPN" -> vpnLogs
                            "SMP" -> smpLogs
                            "XFTP" -> xftpLogs
                            "SOCKS5" -> socks5Logs
                            else -> (torEngineLogs + vpnLogs + smpLogs + xftpLogs + socks5Logs).sortedDescending()
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filter Chips
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("ALL", "TOR", "VPN", "SMP", "XFTP", "SOCKS5").forEach { filter ->
                                FilterChip(
                                    selected = logFilter == filter,
                                    onClick = { logFilter = filter },
                                    label = { Text(filter, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        // Terminal Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF04060A))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            if (allLogs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Console idle. Protocol activities will stream here.", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(allLogs) { log ->
                                        Text(
                                            text = log,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = when {
                                                log.contains("SUCCESS") || log.contains("CONNECTED") || log.contains("OK") -> Color(0xFF00FF66)
                                                log.contains("FAILED") || log.contains("ERROR") -> Color(0xFFFF5252)
                                                log.contains("PENDING") -> Color(0xFFFFD600)
                                                else -> Color(0xFF94A3B8)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG: IMPORT CUSTOM VPN PROFILE
    // ==========================================
    if (showImportVpnDialog) {
        var profileName by remember { mutableStateOf("") }
        var selectedProtocol by remember { mutableStateOf(VpnProtocolType.WIREGUARD) }
        var rawConfigText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showImportVpnDialog = false },
            title = { Text("Import Custom VPN Profile", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Profile Name", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Protocol:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(VpnProtocolType.WIREGUARD, VpnProtocolType.OPENVPN, VpnProtocolType.SHADOWSOCKS).forEach { proto ->
                            FilterChip(
                                selected = selectedProtocol == proto,
                                onClick = { selectedProtocol = proto },
                                label = { Text(proto.displayName, fontSize = 8.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rawConfigText,
                        onValueChange = { rawConfigText = it },
                        label = { Text("Config text (.conf, .ovpn, ss://)", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (profileName.isNotBlank() && rawConfigText.isNotBlank()) {
                            viewModel.importCustomVpnProfile(profileName.trim(), selectedProtocol, rawConfigText.trim())
                            showImportVpnDialog = false
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportVpnDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // DIALOG: CREATE SMP QUEUE
    // ==========================================
    if (showCreateQueueDialog) {
        var serverHost by remember { mutableStateOf("smp.simplex.im") }
        var queueRole by remember { mutableStateOf("RECEIVE") }

        AlertDialog(
            onDismissRequest = { showCreateQueueDialog = false },
            title = { Text("Create Unidirectional SMP Queue", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Role:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = queueRole == "RECEIVE",
                            onClick = { queueRole = "RECEIVE" },
                            label = { Text("RECEIVE (Inbound)", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = queueRole == "SEND",
                            onClick = { queueRole = "SEND" },
                            label = { Text("SEND (Outbound)", fontSize = 10.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverHost,
                        onValueChange = { serverHost = it },
                        label = { Text("Relay Server Host", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createSmpQueue(serverHost.trim(), queueRole)
                        showCreateQueueDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateQueueDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // DIALOG: SEND SMP ENVELOPE
    // ==========================================
    showSendSmpDialog?.let { queue ->
        AlertDialog(
            onDismissRequest = { showSendSmpDialog = null },
            title = { Text("Send SMP Envelope", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Target Queue: ${queue.queueId.take(12)}...", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = smpMessageInput,
                        onValueChange = { smpMessageInput = it },
                        label = { Text("Message payload", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (smpMessageInput.isNotBlank()) {
                            viewModel.sendSmpEnvelope(queue, smpMessageInput.trim())
                            showSendSmpDialog = null
                        }
                    }
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSendSmpDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.theme.Localization
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsScreen(viewModel: MainViewModel) {
    val logs by viewModel.activityLogs.collectAsState()
    val nodeState by viewModel.nodeState.collectAsState()
    
    // Diagnostics State
    val isDiagnosing by viewModel.isDiagnosing.collectAsState()
    val diagnosticsProgress by viewModel.diagnosticsProgress.collectAsState()
    val diagnosticsResults by viewModel.diagnosticsResults.collectAsState()
    
    // Reports State
    val securityReports by viewModel.securityReports.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    
    var activeTab by remember { mutableStateOf("controls") } // "controls", "tests", "logs", "reports"
    val dateFormat = SimpleDateFormat("HH:mm:ss.SS", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Module Title Heading
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Localization.get("debug_title", currentLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = Localization.get("debug_subtitle", currentLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Node Status Badge with clean layout
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (nodeState) {
                            "RUNNING" -> Color(0xFF00FF66).copy(alpha = 0.12f)
                            "STOPPED" -> Color(0xFFEF4444).copy(alpha = 0.12f)
                            else -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when (nodeState) {
                            "RUNNING" -> Color(0xFF00FF66)
                            "STOPPED" -> Color(0xFFEF4444)
                            else -> Color(0xFFF59E0B)
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (nodeState) {
                                    "RUNNING" -> Color(0xFF00FF66)
                                    "STOPPED" -> Color(0xFFEF4444)
                                    else -> Color(0xFFF59E0B)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (nodeState) {
                            "RUNNING" -> Localization.get("node_active", currentLanguage)
                            "STOPPED" -> Localization.get("node_stopped", currentLanguage)
                            else -> Localization.get("node_rebooting", currentLanguage)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = when (nodeState) {
                            "RUNNING" -> Color(0xFF00FF66)
                            "STOPPED" -> Color(0xFFEF4444)
                            else -> Color(0xFFF59E0B)
                        }
                    )
                }
            }
        }

        // Segmented Tabs with elegant icons next to text to save horizontal width and add visual rhythm
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                "controls" to Pair(Localization.get("tab_control", currentLanguage), Icons.Default.ToggleOn),
                "tests" to Pair(Localization.get("tab_tests", currentLanguage), Icons.Default.CheckCircle),
                "logs" to Pair(Localization.get("tab_logs", currentLanguage), Icons.Default.List),
                "reports" to Pair(Localization.get("tab_guide", currentLanguage), Icons.Default.MenuBook)
            )

            tabs.forEach { (tabKey, tabData) ->
                val (tabLabel, icon) = tabData
                val isSelected = activeTab == tabKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { activeTab = tabKey }
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tabLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // --- TAB BODY RENDER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeTab) {
                "controls" -> {
                    // --- TAB: NODE CONTROLS (Старт/Стоп/Перезапуск ноды) ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = Localization.get("daemon_control", currentLanguage),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = Localization.get("daemon_desc", currentLanguage),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "COMMAND CONSOLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // Operational Buttons Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // START BUTTON
                                Button(
                                    onClick = { viewModel.startNode() },
                                    enabled = nodeState != "RUNNING",
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00FF66),
                                        disabledContainerColor = Color(0xFF00FF66).copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = Localization.get("start_node", currentLanguage).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // STOP BUTTON
                                Button(
                                    onClick = { viewModel.stopNode() },
                                    enabled = nodeState != "STOPPED",
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEF4444),
                                        disabledContainerColor = Color(0xFFEF4444).copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = Localization.get("stop_node", currentLanguage).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // RESTART BUTTON
                                Button(
                                    onClick = { viewModel.restartNode() },
                                    enabled = nodeState == "RUNNING",
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF59E0B),
                                        disabledContainerColor = Color(0xFFF59E0B).copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = Localization.get("restart_node", currentLanguage).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Attributes panel
                        Text(
                            text = "SECURITY OPTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Auto-rotate Identity Keys",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Switch(checked = true, onCheckedChange = {})
                            }
                        }
                    }
                }

                "tests" -> {
                    // --- TAB: SECURITY DIAGNOSTICS & TESTS ---
                    Column(modifier = Modifier.fillMaxSize()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "INTEGRITY & LEAK DIAGNOSTICS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Automated audit suite checks for potential DNS leaks, active SOCKS5 handshakes, double-ratchet key rotations, and onion network propagation states.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Run diagnostics button
                        Button(
                            onClick = { viewModel.runDiagnostics() },
                            enabled = !isDiagnosing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isDiagnosing) Localization.get("diag_running", currentLanguage).uppercase()
                                           else Localization.get("diagnose_now", currentLanguage).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Active Testing Progress Indicator
                        if (isDiagnosing) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { diagnosticsProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF00FF66),
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "DIAGNOSTIC TEST OUTCOMES:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (diagnosticsResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Launch diagnostics to evaluate crypt node integrity.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(diagnosticsResults) { (testName, status) ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Passed",
                                                    tint = Color(0xFF00FF66),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = testName,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color(0xFF00FF66).copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "PASSED",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00FF66),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "logs" -> {
                    // --- TAB: LOGS (Потоковые логи ядра) ---
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "LIVE NODE EVENTS MONITOR:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        if (logs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Active events log is empty.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(logs) { log ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            when (log.status) {
                                                                "SUCCESS" -> Color(0xFF00FF66).copy(alpha = 0.15f)
                                                                "PENDING" -> Color.Yellow.copy(alpha = 0.15f)
                                                                else -> Color.Red.copy(alpha = 0.15f)
                                                            }
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = log.actionType,
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when (log.status) {
                                                            "SUCCESS" -> Color(0xFF00E676)
                                                            "PENDING" -> Color(0xFFFFD600)
                                                            else -> Color(0xFFFF1744)
                                                        }
                                                    )
                                                }

                                                Text(
                                                    text = dateFormat.format(Date(log.timestamp)),
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(text = log.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "reports" -> {
                    // --- TAB: REPORTS & DETAILED USER GUIDE (Интерактивное руководство) ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = Localization.get("reports_guide", currentLanguage).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                securityReports.forEachIndexed { index, report ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "${index + 1}. $report", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { /* Export simulation */ },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export Reports to Encrypted ZIP", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }

                        // --- USER GUIDE SECTION (РУКОВОДСТВО ПОЛЬЗОВАТЕЛЯ) ---
                        Text(
                            text = Localization.get("user_guide_title", currentLanguage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Section 1
                                Text("SECTION 1. Node Setup and Deployment", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1. Crypt Vault: Navigate to the 'Vault' tab. Initialize or unlock your secure database using a robust master passphrase. All keys and file metadata are encrypted symmetrically using ChaCha20-Poly1305.\n\n" +
                                           "2. Secure Routing: Go to the 'Network' tab. Enable the built-in Tor Daemon or define custom SOCKS5 proxied endpoints. Always check 'DNS Leak Shield' to guarantee no metadata spills outside your secure routing tunnel.\n\n" +
                                           "3. SimpleX Messaging Relays: Add federation peering servers to gossip message topologies. This ensures decentralized queues remain metadata-resistant and active.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Section 2
                                Text("SECTION 2. Connecting Client via Matrix Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1. Matrix UI Switch: In the 'Chat' screen, toggle the 'Matrix UI Mode'. This emulates an E2EE room-based terminal specifically engineered to control your underlying daemon.\n\n" +
                                           "2. Command Console Room: Use the system room '#control:simplex-node.onion'. Commands inputted into this channel route directly to the core daemon with sub-millisecond execution times.\n\n" +
                                           "3. Operational Command List:\n" +
                                           "   - /status : retrieve current node performance metrics\n" +
                                           "   - /start : launch the Tor network daemon\n" +
                                           "   - /stop : safe shutdown of node queue synchronization\n" +
                                           "   - /restart : trigger soft-reload of node process\n" +
                                           "   - /test : trigger comprehensive leak tests and integrity checks\n" +
                                           "   - /logs : fetch the latest live events feed from node buffer",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Section 3
                                Text("SECTION 3. Inputs Shield (Private Keyboard)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1. Gboard Telemetry Blocking: Standard soft keyboards are restricted inside the E2E Chat channel. This protects your passphrases from Gboard cloud-sync telemetry pipelines.\n\n" +
                                           "2. Isolated Key Tap Sandbox: Leverage only the built-in custom keyboard keys on the Matrix Chat screen. Keystrokes are parsed in-memory with strict sandbox policies, protecting you from on-device keylogger malware.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

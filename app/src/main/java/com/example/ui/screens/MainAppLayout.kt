package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.Localization

@Composable
fun MainAppLayout(viewModel: MainViewModel) {
    var activeTab by remember { mutableStateOf("vault") }
    var showSplashScreen by remember { mutableStateOf(true) }

    val torStatus by viewModel.torStatus.collectAsState()
    val tailscaleStatus by viewModel.tailscaleStatus.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    var showLanguageMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "vault",
                    onClick = { activeTab = "vault" },
                    icon = {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Vault Lock Indicator"
                        )
                    },
                    label = { Text(Localization.get("nav_vault", currentLanguage), fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_vault")
                )

                NavigationBarItem(
                    selected = activeTab == "chat",
                    onClick = { activeTab = "chat" },
                    icon = { Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = "Simplex chat queues") },
                    label = { Text(Localization.get("nav_chat", currentLanguage), fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_tab_chat")
                )

                NavigationBarItem(
                    selected = activeTab == "network",
                    onClick = { activeTab = "network" },
                    icon = { Icon(imageVector = Icons.Default.Security, contentDescription = "SOCKS5 VPN proxies") },
                    label = { Text(Localization.get("nav_net", currentLanguage), fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_network")
                )

                NavigationBarItem(
                    selected = activeTab == "file",
                    onClick = { activeTab = "file" },
                    icon = { Icon(imageVector = Icons.Default.FolderZip, contentDescription = "Encrypted segment files") },
                    label = { Text(Localization.get("nav_files", currentLanguage), fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_file")
                )

                NavigationBarItem(
                    selected = activeTab == "logs",
                    onClick = { activeTab = "logs" },
                    icon = { Icon(imageVector = Icons.Default.BugReport, contentDescription = "Отладка") },
                    label = { Text(Localization.get("nav_debug", currentLanguage), fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_logs")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = innerPadding.calculateBottomPadding())
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                            viewModel.resetInactivityTimer()
                        }
                    }
                }
        ) {
            // High comfort micro headers indicating proxy connectivity status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.OfflineBolt,
                        contentDescription = "Security Suite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.get("app_title", currentLanguage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tor Indicator bubble
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (torStatus == "CONNECTED") Color(0xFF00FF66) else Color(0xFFFF1744))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Localization.get("tor_status", currentLanguage),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (torStatus == "CONNECTED") Color(0xFF00FF66) else MaterialTheme.colorScheme.outline
                        )
                    }

                    // Tailscale bubble
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (tailscaleStatus == "ACTIVE") Color(0xFF00B0FF) else Color(0xFFFF1744))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Localization.get("mesh_status", currentLanguage),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tailscaleStatus == "ACTIVE") Color(0xFF00B0FF) else MaterialTheme.colorScheme.outline
                        )
                    }

                    // Dynamic Language Switcher Box + DropdownMenu
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showLanguageMenu = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("language_selector")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Select Language",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentLanguage,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            listOf(
                                "RU" to "Русский",
                                "EN" to "English",
                                "ES" to "Español",
                                "FR" to "Français",
                                "DE" to "Deutsch",
                                "ZH" to "中文"
                            ).forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name, fontSize = 12.sp) },
                                    onClick = {
                                        viewModel.setLanguage(code)
                                        showLanguageMenu = false
                                    },
                                    modifier = Modifier.testTag("lang_option_$code")
                                )
                            }
                        }
                    }

                    // Button to replay the Cyberpunk-Van Gogh-Matrix-Gaudi WOW Splash
                    IconButton(
                        onClick = { showSplashScreen = true },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x2200FF66))
                            .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .testTag("button_open_splash")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Replay Cyberpunk Intro",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Screen switcher frame container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    "vault" -> LocalVaultScreen(viewModel)
                    "chat" -> SimplexChatScreen(viewModel)
                    "network" -> NetworkScreen(viewModel)
                    "file" -> FileSharingScreen(viewModel)
                    "logs" -> LogsScreen(viewModel)
                }
            }
        }
    }

    // High-Impact Animated Splash Screen with WOW Effect
    AnimatedVisibility(
        visible = showSplashScreen,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(450))
    ) {
        CyberpunkMatrixSplashScreen(
            onDismiss = { showSplashScreen = false }
        )
    }
}
}

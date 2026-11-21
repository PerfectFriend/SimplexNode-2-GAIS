package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.RadioPointDetailDialog
import com.example.ui.components.RadioPointTopBarChip
import com.example.ui.theme.LocalParanoidPalette
import com.example.ui.theme.Localization
import com.example.ui.theme.NodeThemeProfile
import com.example.ui.theme.paletteFor
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// [REAL] Этап 6 — UI Core Refactor: Telegram-style shell с боковым NavigationDrawer и чистым полноэкранным чатом.
// [REAL] Главный экран = Чат (SimpleX E2EE). Жирная нижняя панель ликвидирована в пользу полезного пространства экрана.
// [REAL] Единый стек навигации с поддержкой BackHandler и мгновенным возвратом в чат.
// [REAL] Сохранение 100% функционала: Сейф, Tor, Сеть, XFTP, P2P Облако и Логи доступны из единого бокового меню.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: MainViewModel) {
    val isOnboarded by viewModel.isOnboarded.collectAsState()
    val isPinLocked by viewModel.isPinLocked.collectAsState()
    val activeOnionAddress by viewModel.activeOnionAddress.collectAsState()

    // [REAL] Этап 8: Sovereign Onboarding Gate (Zero system IME)
    if (!isOnboarded) {
        OnboardingScreen(
            viewModel = viewModel,
            onOnboardingFinished = { /* State is handled in MainViewModel */ }
        )
        return
    }

    // [REAL] Этап 8: PIN & Panic PIN Lock Gate (Zero system IME)
    if (isPinLocked) {
        PinLockScreen(
            viewModel = viewModel,
            onUnlockSuccess = { /* State is handled in MainViewModel */ },
            onPanicWiped = { /* State is handled in MainViewModel */ }
        )
        return
    }

    // [REAL] Главный экран по умолчанию — Чат в стиле Telegram
    var activeTab by remember { mutableStateOf("chat") }
    var showSplashScreen by remember { mutableStateOf(false) }

    val torStatus by viewModel.torStatus.collectAsState()
    val tailscaleStatus by viewModel.tailscaleStatus.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val seederStatus by viewModel.torrentSeedingEnabled.collectAsState()
    val activePeers by viewModel.activeTorrentPeersCount.collectAsState()
    val walletEvmAddress by viewModel.walletEvmAddress.collectAsState()
    val fleetNodes by viewModel.fleetNodes.collectAsState()

    val activeTheme by viewModel.activeTheme.collectAsState()
    val radioSnapshot by viewModel.radioSnapshot.collectAsState()
    val radioAudioBeaconEnabled by viewModel.radioAudioBeaconEnabled.collectAsState()
    val palette = LocalParanoidPalette.current

    var showLanguageMenu by remember { mutableStateOf(false) }
    var showRadioDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // [REAL] BackHandler: нажатие кнопки "Назад" закрывает Drawer либо возвращает на главный экран (Чат)
    BackHandler(enabled = drawerState.isOpen || activeTab != "chat") {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (activeTab != "chat") {
            activeTab = "chat"
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxHeight(),
                drawerContainerColor = palette.surface,
                drawerTonalElevation = 6.dp
            ) {
                // --- TELEGRAM-STYLE DRAWER HEADER ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.headerBg)
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Node Avatar / Shield Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(palette.surfaceElevated)
                                .border(2.dp, palette.accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Node Shield",
                                tint = palette.accent,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Tor Status Pill
                        val torPillColor = if (torStatus == "CONNECTED") palette.accent else palette.danger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(torPillColor.copy(alpha = 0.14f))
                                .border(1.dp, torPillColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(torPillColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TOR $torStatus",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = torPillColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "PXNode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "$activeOnionAddress • E2EE Mesh",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.textSecondary
                    )
                }

                HorizontalDivider(color = palette.border, thickness = 1.dp)

                // --- NAVIGATION ITEMS ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    // 1. CHATS (Primary / Telegram Core)
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "Chats",
                                tint = if (activeTab == "chat") palette.accent else palette.textSecondary
                            )
                        },
                        label = {
                            Text(
                                text = Localization.get("nav_chat", currentLanguage),
                                fontWeight = if (activeTab == "chat") FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == "chat") palette.textPrimary else palette.textSecondary
                            )
                        },
                        badge = {
                            Badge(containerColor = palette.accent) {
                                Text("SimpleX", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        selected = activeTab == "chat",
                        onClick = {
                            activeTab = "chat"
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = palette.surfaceElevated,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .testTag("nav_tab_chat")
                            .testTag("nav_drawer_chat")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. VAULT (Cryptographic Storage)
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Vault",
                                tint = if (isUnlocked) palette.accent else palette.warning
                            )
                        },
                        label = {
                            Text(
                                text = Localization.get("nav_vault", currentLanguage),
                                fontWeight = if (activeTab == "vault") FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == "vault") palette.textPrimary else palette.textSecondary
                            )
                        },
                        badge = {
                            Text(
                                text = if (isUnlocked) "UNLOCKED" else "LOCKED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) palette.accent else palette.warning
                            )
                        },
                        selected = activeTab == "vault",
                        onClick = {
                            activeTab = "vault"
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = palette.surfaceElevated,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .testTag("nav_tab_vault")
                            .testTag("nav_drawer_vault")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 3. NETWORK (Tor / SOCKS5 / SMP Routing)
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.VpnLock,
                                contentDescription = "Network",
                                tint = if (activeTab == "network") palette.secondaryAccent else palette.textSecondary
                            )
                        },
                        label = {
                            Text(
                                text = Localization.get("nav_net", currentLanguage),
                                fontWeight = if (activeTab == "network") FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == "network") palette.textPrimary else palette.textSecondary
                            )
                        },
                        badge = {
                            Text(
                                text = "TOR/SOCKS5",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = palette.secondaryAccent
                            )
                        },
                        selected = activeTab == "network",
                        onClick = {
                            activeTab = "network"
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = palette.surfaceElevated,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .testTag("nav_tab_network")
                            .testTag("nav_drawer_network")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4. FILES & P2P CLOUD (XFTP & Seeder)
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Files & P2P Cloud",
                                tint = if (seederStatus) palette.accent else palette.textSecondary
                            )
                        },
                        label = {
                            Text(
                                text = Localization.get("nav_files", currentLanguage),
                                fontWeight = if (activeTab == "file") FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == "file") palette.textPrimary else palette.textSecondary
                            )
                        },
                        badge = {
                            Text(
                                text = if (seederStatus) "SWARM: $activePeers" else "IDLE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (seederStatus) palette.accent else palette.textMuted
                            )
                        },
                        selected = activeTab == "file",
                        onClick = {
                            activeTab = "file"
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = palette.surfaceElevated,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .testTag("nav_tab_file")
                            .testTag("nav_drawer_file")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4.5 UNIVERSAL WALLET (Stage 9)
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Universal Wallet",
                                tint = if (activeTab == "wallet") palette.accent else palette.textSecondary
                            )
                        },
                        label = {
                            Text(
                                text = Localization.get("nav_wallet", currentLanguage),
                                fontWeight = if (activeTab == "wallet") FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == "wallet") palette.textPrimary else palette.textSecondary
                            )
                        },
                        badge = {
                            Text(
                                text = if (walletEvmAddress.isNotBlank()) "0x..${walletEvmAddress.takeLast(4)}" else "EVM",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent
                            )
                        },
                        selected = activeTab == "wallet",
                        onClick = {
                            activeTab = "wallet"
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = palette.surfaceElevated,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("nav_drawer_wallet")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4.6 FLEET COMMAND CENTER (Stage 9)
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "Fleet Command Center",
                                tint = if (activeTab == "fleet") palette.secondaryAccent else palette.textSecondary
                            )
                        },
                        label = {
                            Text(
                                text = Localization.get("nav_fleet", currentLanguage),
                                fontWeight = if (activeTab == "fleet") FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == "fleet") palette.textPrimary else palette.textSecondary
                            )
                        },
                        badge = {
                            Text(
                                text = "${fleetNodes.size} NODES",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = palette.secondaryAccent
                            )
                        },
                        selected = activeTab == "fleet",
                        onClick = {
                            activeTab = "fleet"
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = palette.surfaceElevated,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("nav_drawer_fleet")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 5. DIAGNOSTICS & LOGS (Terminal)
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Terminal & Logs",
                                tint = if (activeTab == "logs") palette.accent else palette.textSecondary
                            )
                        },
                        label = {
                            Text(
                                text = Localization.get("nav_debug", currentLanguage),
                                fontWeight = if (activeTab == "logs") FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == "logs") palette.textPrimary else palette.textSecondary
                            )
                        },
                        selected = activeTab == "logs",
                        onClick = {
                            activeTab = "logs"
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = palette.surfaceElevated,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .testTag("nav_tab_logs")
                            .testTag("nav_drawer_logs")
                    )
                }

                HorizontalDivider(color = palette.border, thickness = 1.dp)

                // --- DRAWER FOOTER (THEME SKINS, RADIO HEALTH, LANGUAGE, LOCK, INTRO) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Stage 10: 3 Switchable Visual Themes (Matrix / Obsidian / Aurora)
                    Text(
                        text = "ТЕМА ИНТЕРФЕЙСА",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NodeThemeProfile.entries.forEach { profile ->
                            val isSelected = activeTheme == profile
                            val themeColors = paletteFor(profile)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) themeColors.accent.copy(alpha = 0.18f) else palette.surfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) themeColors.accent else palette.border,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setThemeProfile(profile) }
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                    .testTag("theme_chip_${profile.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(themeColors.accent)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = profile.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) themeColors.accent else palette.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Language Switcher Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.surfaceElevated)
                            .clickable { showLanguageMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("language_selector"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Язык / Language: $currentLanguage",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = palette.textSecondary
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Replay Intro Button
                    OutlinedButton(
                        onClick = { showSplashScreen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button_open_splash"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.accent.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Matrix & Gaudi Intro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (isUnlocked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.lockNode()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_lock_node"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.danger),
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.danger.copy(alpha = 0.5f))
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Заблокировать Ноду (PIN)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { viewModel.lockVault() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.warning),
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.warning.copy(alpha = 0.5f))
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Заблокировать Сейф", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) {
        // --- MAIN APPLICATION SCREEN SHELL ---
        Scaffold(
            topBar = {
                // Telegram-like clean TopAppBar
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = palette.headerBg,
                        titleContentColor = palette.textPrimary
                    ),
                    navigationIcon = {
                        if (activeTab == "chat") {
                            // Hamburger menu button to open side drawer
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("button_open_drawer")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Navigation Menu",
                                    tint = palette.textPrimary
                                )
                            }
                        } else {
                            // Back button to return to Chat
                            IconButton(
                                onClick = { activeTab = "chat" },
                                modifier = Modifier.testTag("button_back_to_chat")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Chat",
                                    tint = palette.textPrimary
                                )
                            }
                        }
                    },
                    title = {
                        Column {
                            val title = when (activeTab) {
                                "chat" -> "ParanoidX Chat"
                                "vault" -> Localization.get("nav_vault", currentLanguage)
                                "network" -> Localization.get("nav_net", currentLanguage)
                                "file" -> Localization.get("nav_files", currentLanguage)
                                "wallet" -> Localization.get("nav_wallet", currentLanguage)
                                "fleet" -> Localization.get("nav_fleet", currentLanguage)
                                "logs" -> Localization.get("nav_debug", currentLanguage)
                                else -> "ParanoidX"
                            }
                            val subtitle = when (activeTab) {
                                "chat" -> "SimpleX E2EE • Tor $torStatus"
                                "vault" -> if (isUnlocked) "Сейф активен (SQLCipher)" else "Заблокировано"
                                "network" -> "SOCKS5 127.0.0.1:9050"
                                "file" -> "XFTP & P2P Swarm ($activePeers peers)"
                                "wallet" -> "ParanoidX/Wallet/EVM/v1 • Proof MVP"
                                "fleet" -> "Fleet Command • ${fleetNodes.size} Nodes"
                                "logs" -> "Диагностика узла"
                                else -> ""
                            }
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            if (subtitle.isNotEmpty()) {
                                Text(
                                    text = subtitle,
                                    fontSize = 10.sp,
                                    color = if (torStatus == "CONNECTED") palette.accent else palette.textSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    },
                    actions = {
                        // Stage 10: Radio-Point Honest Health Indicator + Drawer Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            RadioPointTopBarChip(
                                snapshot = radioSnapshot,
                                onClick = { showRadioDialog = true }
                            )

                            // Drawer toggle button if not on chat screen
                            if (activeTab != "chat") {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu",
                                        tint = palette.textSecondary
                                    )
                                }
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            // [REAL] Контент занимает всё пространство экрана — никакой жирной нижней панели!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                                viewModel.resetInactivityTimer()
                            }
                        }
                    }
            ) {
                when (activeTab) {
                    "chat" -> SimplexChatScreen(viewModel)
                    "vault" -> LocalVaultScreen(viewModel)
                    "network" -> NetworkScreen(viewModel)
                    "file" -> FileSharingScreen(viewModel)
                    "wallet" -> WalletScreen(viewModel)
                    "fleet" -> FleetScreen(viewModel)
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

    if (showRadioDialog) {
        RadioPointDetailDialog(
            snapshot = radioSnapshot,
            audioBeaconEnabled = radioAudioBeaconEnabled,
            onToggleAudioBeacon = { viewModel.toggleRadioAudioBeacon(it) },
            onTestBeaconPulse = { viewModel.probeRadioBeacon() },
            onDismiss = { showRadioDialog = false }
        )
    }
}

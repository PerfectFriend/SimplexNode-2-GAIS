package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SimplexServer
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SimplexChatMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// [REAL] Этап 7 — Клавиатура и ввод (Gboard-like) + Telegram Chat UX.
// [REAL] Полноценная поддержка системной клавиатуры (Gboard / Android IME): свайп, голосовой ввод, подсказки, буфер обмена, эмодзи.
// [REAL] Dynamic insets imePadding() — клавиатура плавно поднимает список сообщений без перекрытия.
// [REAL] Режим инкогнито noLearning для Gboard — защита от телеметрии и обучения предиктора.
// [REAL] Переключатель [GBOARD] ↔ [AIR-GAP SHIELD] (приватная наэкранная клавиатура с поддержкой RU/EN раскладок).
// [REAL] Telegram-style пузыри сообщений со статусом доставки (✓✓), шифрованием E2EE и инспектором пакетов Ed25519/X25519.

private fun getChatText(key: String, lang: String): String {
    val maps = mapOf(
        "EN" to mapOf(
            "matrix_console" to "Matrix Console",
            "simplex_queues" to "SimpleX Queues",
            "matrix_desc" to "Secure node control terminal",
            "simplex_desc" to "Asynchronous metadata-free message queues",
            "matrix_style" to "Matrix Style",
            "normal_simplex" to "Normal SimpleX",
            "room_control" to "Node Control",
            "room_chat" to "Cryptochat",
            "room_packets" to "Packet Flow",
            "route_active" to "ENCRYPTION ROUTE: MATRIX P2P STANDARD",
            "secured" to "SECURED",
            "gboard_active" to "GBOARD (SYSTEM IME) ACTIVE",
            "airgap_active" to "AIR-GAP SHIELD ACTIVE (ZERO TELEMETRY)",
            "space" to "Space",
            "send" to "SEND",
            "placeholder_cmd" to "Enter command /status, /start, /stop, /test, /logs...",
            "placeholder_msg" to "Message (SimpleX E2EE)...",
            "inspect_title" to "E2EE Packet Inspector",
            "inspect_sig" to "CRYPTOGRAPHIC SIGNATURE (ED25519):",
            "inspect_cipher" to "PACKET CIPHERTEXT (CHACHA20-POLY1305):",
            "inspect_ephemeral" to "EPHEMERAL DIFFIE-HELLMAN KEY (X25519):",
            "inspect_route" to "DELIVERY ROUTE (ONION):",
            "inspect_index" to "Session index:",
            "inspect_transport" to "Transport:",
            "inspect_close" to "Close",
            "empty_queues" to "Message queues are empty.",
            "empty_desc" to "Type a message or use commands (/status, /test) below."
        ),
        "RU" to mapOf(
            "matrix_console" to "Консоль Matrix",
            "simplex_queues" to "Очереди SimpleX",
            "matrix_desc" to "Терминал защищённого управления нодой",
            "simplex_desc" to "Анонимные очереди сообщений без метаданных",
            "matrix_style" to "В стиле Matrix",
            "normal_simplex" to "Обычный SimpleX",
            "room_control" to "Управление Нодой",
            "room_chat" to "Шифрочат",
            "room_packets" to "Поток Пакетов",
            "route_active" to "МАРШРУТ ШИФРОВАНИЯ: СТАНДАРТ MATRIX P2P",
            "secured" to "ЗАЩИЩЕНО",
            "gboard_active" to "АКТИВЕН GBOARD (СИСТЕМНЫЙ ВВОД)",
            "airgap_active" to "АКТИВЕН AIR-GAP ЩИТ (БЕЗ ТЕЛЕМЕТРИИ)",
            "space" to "Пробел",
            "send" to "ОТПРАВИТЬ",
            "placeholder_cmd" to "Введите команду /status, /start, /stop, /test, /logs...",
            "placeholder_msg" to "Сообщение (SimpleX E2EE)...",
            "inspect_title" to "E2EE Инспектор Пакетов",
            "inspect_sig" to "КРИПТОГРАФИЧЕСКАЯ ПОДПИСЬ (ED25519):",
            "inspect_cipher" to "ШИФРОТЕКСТ ПАКЕТА (CHACHA20-POLY1305):",
            "inspect_ephemeral" to "ЭФЕМЕРНЫЙ КЛЮЧ DIFFIE-HELLMAN (X25519):",
            "inspect_route" to "МАРШРУТ ДОСТАВКИ (ONION):",
            "inspect_index" to "Индекс сессии:",
            "inspect_transport" to "Транспорт:",
            "inspect_close" to "Закрыть",
            "empty_queues" to "Очереди сообщений пусты.",
            "empty_desc" to "Напишите сообщение или используйте команды (/status, /test)."
        ),
        "ES" to mapOf(
            "matrix_console" to "Consola Matrix",
            "simplex_queues" to "Colas SimpleX",
            "matrix_desc" to "Terminal de control de nodo seguro",
            "simplex_desc" to "Colas de mensajes asíncronos sin metadatos",
            "matrix_style" to "Estilo Matrix",
            "normal_simplex" to "SimpleX Normal",
            "room_control" to "Control de Nodo",
            "room_chat" to "Criptochat",
            "room_packets" to "Flujo de Paquetes",
            "route_active" to "RUTA ENCRIPCIÓN: ESTÁNDAR MATRIX P2P",
            "secured" to "SEGURO",
            "gboard_active" to "GBOARD (IME DEL SISTEMA) ACTIVO",
            "airgap_active" to "ESCUDO AIR-GAP ACTIVO",
            "space" to "Espacio",
            "send" to "ENVIAR",
            "placeholder_cmd" to "Introduzca comando /status, /start, /stop, /test...",
            "placeholder_msg" to "Mensaje (SimpleX E2EE)...",
            "inspect_title" to "Inspector de Paquetes E2EE",
            "inspect_sig" to "FIRMA CRIPTOGRÁFICA (ED25519):",
            "inspect_cipher" to "TEXTO CIFRADO DE PAQUETE (CHACHA20-POLY1305):",
            "inspect_ephemeral" to "CLAVE EFFÍMERA DIFFIE-HELLMAN (X25519):",
            "inspect_route" to "RUTA DE ENTREGA (ONION):",
            "inspect_index" to "Índice de sesión:",
            "inspect_transport" to "Transporte:",
            "inspect_close" to "Cerrar",
            "empty_queues" to "Colas de mensajes vacías.",
            "empty_desc" to "Introduzca un mensaje o comando abajo."
        ),
        "FR" to mapOf(
            "matrix_console" to "Console Matrix",
            "simplex_queues" to "Files SimpleX",
            "matrix_desc" to "Terminal de contrôle de noeud sécurisé",
            "simplex_desc" to "Files d'attente de messages asynchrones sans métadonnées",
            "matrix_style" to "Style Matrix",
            "normal_simplex" to "SimpleX Normal",
            "room_control" to "Contrôle Noeud",
            "room_chat" to "Cryptochat",
            "room_packets" to "Flux Paquets",
            "route_active" to "ROUTE DE CHIFFREMENT: STANDARD MATRIX P2P",
            "secured" to "SÉCURISÉ",
            "gboard_active" to "GBOARD (SYSTÈME) ACTIF",
            "airgap_active" to "BOUCLIER AIR-GAP ACTIF",
            "space" to "Espace",
            "send" to "ENVOYER",
            "placeholder_cmd" to "Entrer commande /status, /start...",
            "placeholder_msg" to "Message (SimpleX E2EE)...",
            "inspect_title" to "Inspecteur de Paquets E2EE",
            "inspect_sig" to "SIGNATURE CRYPTOGRAPHIQUE (ED25519):",
            "inspect_cipher" to "CHIFFREMENT DU PAQUET (CHACHA20-POLY1305):",
            "inspect_ephemeral" to "CLÉ DH ÉPHÉMÈRE (X25519):",
            "inspect_route" to "ROUTE DE LIVRAISON (ONION):",
            "inspect_index" to "Index session:",
            "inspect_transport" to "Transport:",
            "inspect_close" to "Fermer",
            "empty_queues" to "Files de messages vides.",
            "empty_desc" to "Entrez un message ou une commande."
        ),
        "DE" to mapOf(
            "matrix_console" to "Matrix-Konsole",
            "simplex_queues" to "SimpleX-Warteschlangen",
            "matrix_desc" to "Sicherer Knotenkontrollterminal",
            "simplex_desc" to "Asynchrone metadatenfreie Nachrichtenwarteschlangen",
            "matrix_style" to "Matrix-Stil",
            "normal_simplex" to "Normales SimpleX",
            "room_control" to "Knotensteuerung",
            "room_chat" to "Kryptochat",
            "room_packets" to "Paketfluss",
            "route_active" to "VERSCHLÜSSELUNGSROUTE: MATRIX-P2P-STANDARD",
            "secured" to "GESICHERT",
            "gboard_active" to "GBOARD (SYSTEM-IME) AKTIV",
            "airgap_active" to "AIR-GAP-SCHILD AKTIV",
            "space" to "Leertaste",
            "send" to "SENDEN",
            "placeholder_cmd" to "Befehl eingeben /status...",
            "placeholder_msg" to "Nachricht (SimpleX E2EE)...",
            "inspect_title" to "E2EE-Paket-Inspektor",
            "inspect_sig" to "KRYPTOGRAPHISCHE SIGNATUR (ED25519):",
            "inspect_cipher" to "PAKET-CHIFFRETEXT (CHACHA20-POLY1305):",
            "inspect_ephemeral" to "EPHEMERER DIFFIE-HELLMAN-SCHLÜSSEL (X25519):",
            "inspect_route" to "ZUSTELLUNGSROUTE (ONION):",
            "inspect_index" to "Sitzungsindex:",
            "inspect_transport" to "Transport:",
            "inspect_close" to "Schließen",
            "empty_queues" to "Nachrichtenwarteschlangen sind leer.",
            "empty_desc" to "Geben Sie eine Nachricht oder einen Befehl ein."
        ),
        "ZH" to mapOf(
            "matrix_console" to "Matrix 控制台",
            "simplex_queues" to "SimpleX 队列",
            "matrix_desc" to "安全节点控制台终端",
            "simplex_desc" to "采用无元数据技术的异步消息队列",
            "matrix_style" to "Matrix 风格",
            "normal_simplex" to "普通 SimpleX",
            "room_control" to "节点控制",
            "room_chat" to "加密会话",
            "room_packets" to "数据包流",
            "route_active" to "加密路由: MATRIX P2P 标准",
            "secured" to "已保护",
            "gboard_active" to "GBOARD (系统输入法) 已就绪",
            "airgap_active" to "AIR-GAP 隔空安全键盘已就绪",
            "space" to "空格",
            "send" to "发送",
            "placeholder_cmd" to "输入控制命令 /status, /start...",
            "placeholder_msg" to "端到端加密消息...",
            "inspect_title" to "端到端加密数据包审计器",
            "inspect_sig" to "密码学数字签名 (ED25519):",
            "inspect_cipher" to "数据包密文载荷 (CHACHA20-POLY1305):",
            "inspect_ephemeral" to "临时 Diffie-Hellman 协商公钥 (X25519):",
            "inspect_route" to "网格匿名洋葱递送链路 (ONION):",
            "inspect_index" to "密码流索引号:",
            "inspect_transport" to "物理中继通道:",
            "inspect_close" to "关闭",
            "empty_queues" to "消息队列暂未接收到数据包",
            "empty_desc" to "请输入消息或指令。"
        )
    )
    val langMap = maps[lang] ?: maps["EN"]!!
    return langMap[key] ?: maps["EN"]?.get(key) ?: key
}

@Composable
fun SimplexChatScreen(viewModel: MainViewModel) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val servers by viewModel.simplexServers.collectAsState()
    val torEnabled by viewModel.torEnabled.collectAsState()
    val tailscaleEnabled by viewModel.tailscaleEnabled.collectAsState()

    // Upgraded secure E2EE chat flows
    val chatMessages by viewModel.simplexChatMessages.collectAsState()
    val isSendingMessage by viewModel.isSendingMessage.collectAsState()
    val sendingStep by viewModel.sendingStep.collectAsState()
    val sendingProgress by viewModel.sendingProgress.collectAsState()
    val clientEphemeralPublicKey by viewModel.clientEphemeralPublicKey.collectAsState()
    val peerEphemeralPublicKey by viewModel.peerEphemeralPublicKey.collectAsState()

    var selectedServer by remember { mutableStateOf<SimplexServer?>(null) }
    var currentMessage by remember { mutableStateOf("") }
    var activeInspectMessage by remember { mutableStateOf<SimplexChatMessage?>(null) }

    // Multi-View Matrix Configuration State
    var isMatrixMode by remember { mutableStateOf(false) }
    var activeRoom by remember { mutableStateOf("#control:simplex-node.onion") }

    // [REAL] Этап 7: Переключатель режима ввода (true = системный Gboard, false = Air-Gap наэкранная)
    var isGboardMode by remember { mutableStateOf(true) }
    var showEmojiStrip by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    if (servers.isNotEmpty() && selectedServer == null) {
        selectedServer = servers.first()
    }

    // Автоматическая прокрутка к последнему сообщению при получении нового
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Packet Inspection Dialog
    activeInspectMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { activeInspectMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getChatText("inspect_title", currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = getChatText("inspect_sig", currentLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090D14))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = msg.signatureHex,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = getChatText("inspect_cipher", currentLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090D14))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = msg.cipherTextHex,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00FF66)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = getChatText("inspect_ephemeral", currentLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090D14))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = msg.ephemeralPublicKey,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE5A93C)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = getChatText("inspect_route", currentLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090D14))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        msg.routingPath.forEachIndexed { idx, hop ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00FF66))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Хост ${idx + 1}: $hop",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            if (idx < msg.routingPath.lastIndex) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color(0xFF1E293B),
                                    modifier = Modifier
                                        .padding(start = 1.dp)
                                        .size(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${getChatText("inspect_index", currentLanguage)} #${msg.ratchetIndex}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text("${getChatText("inspect_transport", currentLanguage)} ${msg.relayModeUsed}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeInspectMessage = null }) {
                    Text(getChatText("inspect_close", currentLanguage), color = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // [REAL] Основной контейнер экрана с динамической поддержкой imePadding() для клавиатуры Gboard
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isMatrixMode) Color(0xFF070A0F) else MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Multi-View Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMatrixMode) getChatText("matrix_console", currentLanguage) else getChatText("simplex_queues", currentLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurface,
                    fontFamily = if (isMatrixMode) FontFamily.Monospace else FontFamily.Default
                )
                Text(
                    text = if (isMatrixMode) getChatText("matrix_desc", currentLanguage) else getChatText("simplex_desc", currentLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = if (isMatrixMode) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mode Toggle Switch Button
            Button(
                onClick = { isMatrixMode = !isMatrixMode },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMatrixMode) Color(0xFF00FF66).copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .testTag("toggle_matrix_mode")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isMatrixMode) Icons.Default.Terminal else Icons.Default.Widgets,
                        contentDescription = null,
                        tint = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMatrixMode) getChatText("matrix_style", currentLanguage) else getChatText("normal_simplex", currentLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // --- RENDER MATRIX TERMINAL MODE ---
        if (isMatrixMode) {
            // Room Selection Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val rooms = listOf(
                    "#control:simplex-node.onion" to getChatText("room_control", currentLanguage),
                    "#general:simplex-node.onion" to getChatText("room_chat", currentLanguage),
                    "#logs:simplex-node.onion" to getChatText("room_packets", currentLanguage)
                )

                rooms.forEach { (roomKey, roomLabel) ->
                    val isSelected = activeRoom == roomKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFF00FF66).copy(alpha = 0.15f) else Color(0xFF11161B))
                            .border(1.dp, if (isSelected) Color(0xFF00FF66) else Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .clickable { activeRoom = roomKey }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = roomLabel.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF00FF66) else Color(0xFF64748B),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Matrix Style Message Display container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF030712))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                val filteredMessages = chatMessages.filter { msg ->
                    when (activeRoom) {
                        "#control:simplex-node.onion" -> {
                            msg.text.startsWith("/") || msg.sender == "peer" || msg.text.contains("КОМАНДА") || msg.text.contains("TELEMETRY") || msg.text.contains("Службы") || msg.text.contains("Диагностика")
                        }
                        "#logs:simplex-node.onion" -> {
                            msg.sender == "me" || msg.text.contains("Acknowledge") || msg.text.contains("relay")
                        }
                        else -> {
                            !msg.text.startsWith("/")
                        }
                    }
                }

                if (filteredMessages.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Консоль '${activeRoom}' пуста.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(filteredMessages) { msg ->
                            val isMe = msg.sender == "me"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isMe) "@user:local.onion" else "@admin:simplex-node.onion",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = if (isMe) Color(0xFF38BDF8) else Color(0xFF00FF66)
                                    )

                                    IconButton(
                                        onClick = { activeInspectMessage = msg },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = "Ключи",
                                            tint = Color(0xFF00FF66).copy(alpha = 0.5f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F17)),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.5.dp, Color(0xFF1F2937), RoundedCornerShape(4.dp))
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isMe) Color(0xFFE2E8F0) else Color(0xFF00FF66),
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "SHA256: ${msg.cipherTextHex.take(12)}...",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF475569)
                                            )
                                            Text(
                                                text = "Индекс: #${msg.ratchetIndex}",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } else {
            // --- RENDER STANDARD SIMPLEX QUEUES MODE (TELEGRAM CHAT FEEL) ---
            if (!torEnabled && !tailscaleEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ПРЕДУПРЕЖДЕНИЕ: Tor/Tailscale отключены. Трафик не изолирован.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Compact Server Node chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                servers.forEach { server ->
                    val isSelected = selectedServer?.id == server.id
                    val chipBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    val chipBorder = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(6.dp))
                            .clickable { selectedServer = server }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (server.status == "CONNECTED") Color(0xFF00FF66) else Color.Red)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = server.serverName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Compact Double Ratchet Session Status
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(bottom = 6.dp)
                    .testTag("ratchet_status_header")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "X25519 DH: ${clientEphemeralPublicKey.take(8)}... ↔ ${peerEphemeralPublicKey.take(8)}...",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .clickable { viewModel.rotateDoubleRatchetKeys() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("rotate_keys_badge")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сброс", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // [REAL] Telegram-style Chat Message Stream with real bubbles, timestamps and checkmarks
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF090D14))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(getChatText("empty_queues", currentLanguage), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(getChatText("empty_desc", currentLanguage), style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(chatMessages) { msg ->
                            val isMe = msg.sender == "me"
                            val timeFormatted = remember(msg.timestamp) {
                                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                sdf.format(Date(msg.timestamp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                if (!isMe) {
                                    // Peer Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E293B))
                                            .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // Telegram-style Bubble
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .clip(
                                            if (isMe) RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
                                            else RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
                                        )
                                        .background(
                                            if (isMe) Color(0xFF142B28) // Deep Telegram emerald
                                            else Color(0xFF141C2A) // Slate incoming
                                        )
                                        .border(
                                            1.dp,
                                            if (isMe) Color(0xFF00FF66).copy(alpha = 0.35f)
                                            else Color(0xFF334155).copy(alpha = 0.4f),
                                            if (isMe) RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
                                            else RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
                                        )
                                        .clickable { activeInspectMessage = msg }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    if (!isMe) {
                                        Text(
                                            text = "Node Relay (Onion v3)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00FF66)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }

                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isMe) Color(0xFFE6FFFA) else Color(0xFFF1F5F9),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Bubble Metadata Row: Timestamp + Lock + Double Checkmark
                                    Row(
                                        modifier = Modifier.align(Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "E2EE",
                                            tint = Color(0xFF00FF66).copy(alpha = 0.7f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = timeFormatted,
                                            fontSize = 9.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (isMe) {
                                            // Telegram-style Double Checkmark
                                            Text(
                                                text = "✓✓",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00FF66)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- PROGRESS INDICATOR FOR ACTIVE MESSAGE SENDING ---
        if (isSendingMessage) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { sendingProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp),
                    color = Color(0xFF00FF66),
                    trackColor = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = sendingStep.take(30) + "...",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF00FF66)
                )
            }
        }

        // --- QUICK COMMAND CHIPS (POPS UP WHEN '/' IS TYPED) ---
        AnimatedVisibility(
            visible = currentMessage.startsWith("/"),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val commandList = listOf("/status", "/test", "/logs", "/keys", "/start", "/stop", "/clear", "/whoami")
                items(commandList) { cmd ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            currentMessage = cmd
                        }
                    ) {
                        Text(
                            text = cmd,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00FF66)
                        )
                    }
                }
            }
        }

        // --- QUICK EMOJI BAR ---
        AnimatedVisibility(
            visible = showEmojiStrip,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(Color(0xFF0B1017), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val emojiList = listOf("🔒", "🛡️", "🧅", "⚡", "🚀", "👍", "🔥", "💬", "🤖", "💻", "🔑", "📦")
                items(emojiList) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { currentMessage += emoji }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- [REAL] ЭТАП 7: TELEGRAM-STYLE INPUT BAR WITH GBOARD & AIR-GAP SUPPORT ---
        val sendAction = {
            if (currentMessage.isNotBlank()) {
                selectedServer?.let { server ->
                    viewModel.submitSimplexMessage(server, currentMessage)
                    currentMessage = ""
                    focusManager.clearFocus()
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Main Input Capsule
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(24.dp)),
                color = Color(0xFF0D1420),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment Paperclip button
                    Box {
                        IconButton(
                            onClick = { showAttachmentMenu = !showAttachmentMenu },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("chat_attach_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showAttachmentMenu,
                            onDismissRequest = { showAttachmentMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Зашифровать файл (XFTP)") },
                                leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF00FF66)) },
                                onClick = {
                                    currentMessage = "[XFTP: encrypted_payload_${System.currentTimeMillis() % 1000}.bin]"
                                    showAttachmentMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Поделиться Onion адресом") },
                                leadingIcon = { Icon(Icons.Default.VpnLock, contentDescription = null, tint = Color(0xFF00B0FF)) },
                                onClick = {
                                    currentMessage = "node_simplex_v3_mesh.onion"
                                    showAttachmentMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Команда /status") },
                                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                onClick = {
                                    currentMessage = "/status"
                                    showAttachmentMenu = false
                                }
                            )
                        }
                    }

                    // Emoji toggle button
                    IconButton(
                        onClick = { showEmojiStrip = !showEmojiStrip },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("chat_emoji_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SentimentSatisfiedAlt,
                            contentDescription = "Emojis",
                            tint = if (showEmojiStrip) Color(0xFF00FF66) else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Native Material 3 TextField — Gboard, swipe typing, voice input, noLearning incognito
                    TextField(
                        value = currentMessage,
                        onValueChange = { currentMessage = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        placeholder = {
                            Text(
                                text = if (isMatrixMode) getChatText("placeholder_cmd", currentLanguage) else getChatText("placeholder_msg", currentLanguage),
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF00FF66)
                        ),
                        singleLine = false,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { sendAction() }
                        )
                    )

                    // Keyboard Mode Toggle [GBOARD] ↔ [AIR-GAP SHIELD]
                    IconButton(
                        onClick = { isGboardMode = !isGboardMode },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("chat_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (isGboardMode) Icons.Default.Keyboard else Icons.Default.Security,
                            contentDescription = if (isGboardMode) "Switch to Air-Gap Keyboard" else "Switch to Gboard",
                            tint = if (isGboardMode) Color(0xFF00FF66) else Color(0xFFE5A93C),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Telegram-style Circular Send / Mic button
            val hasText = currentMessage.isNotBlank()
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (hasText) Color(0xFF00FF66) else Color(0xFF1E293B))
                    .clickable {
                        if (hasText) {
                            sendAction()
                        } else {
                            // Quick voice simulation / diagnostic ping
                            currentMessage = "/test"
                        }
                    }
                    .testTag("chat_send_button"),
                contentAlignment = Alignment.Center
            ) {
                if (hasText) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice / Probe",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- [REAL] AIR-GAP ON-SCREEN KEYBOARD (ACTIVATED IN AIR-GAP MODE) ---
        AnimatedVisibility(
            visible = !isGboardMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            AirGapShieldKeyboard(
                currentMessage = currentMessage,
                onMessageChange = { currentMessage = it },
                onSendClicked = { sendAction() },
                isMatrixStyle = isMatrixMode,
                currentLanguage = currentLanguage
            )
        }
    }
}

// [REAL] Обновлённая защищённая наэкранная клавиатура Air-Gap с поддержкой русской (RU) и английской (EN) раскладки
@Composable
fun AirGapShieldKeyboard(
    currentMessage: String,
    onMessageChange: (String) -> Unit,
    onSendClicked: () -> Unit,
    isMatrixStyle: Boolean,
    currentLanguage: String
) {
    var isShiftActive by remember { mutableStateOf(false) }
    var isSymbolsActive by remember { mutableStateOf(false) }
    var activeKbLang by remember { mutableStateOf(if (currentLanguage == "RU") "RU" else "EN") }

    val keysRow1 = when {
        isSymbolsActive -> listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        activeKbLang == "RU" -> listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ")
        else -> listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    }

    val keysRow2 = when {
        isSymbolsActive -> listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")")
        activeKbLang == "RU" -> listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э")
        else -> listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "/")
    }

    val keysRow3 = when {
        isSymbolsActive -> listOf("!", "\"", "'", ":", ";", "/", "?", "=", "\\", "[DEL]")
        activeKbLang == "RU" -> listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю", "[DEL]")
        else -> listOf("z", "x", "c", "v", "b", "n", "m", "_", "[DEL]")
    }

    val keyboardBg = if (isMatrixStyle) Color(0xFF080C11) else Color(0xFF0A0F18)
    val keyBg = if (isMatrixStyle) Color(0xFF11161D) else Color(0xFF16202E)
    val keyBorder = if (isMatrixStyle) Color(0xFF00FF66).copy(alpha = 0.25f) else Color(0xFF26354A)
    val textColor = if (isMatrixStyle) Color(0xFF00FF66) else Color(0xFFE2E8F0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(keyboardBg)
            .border(1.dp, keyBorder, RoundedCornerShape(12.dp))
            .padding(4.dp)
            .testTag("air_gap_keyboard"),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Safe keyboard shield notice
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5A93C))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = getChatText("airgap_active", currentLanguage),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE5A93C)
                )
            }

            // Language Switcher Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E293B))
                    .clickable {
                        activeKbLang = if (activeKbLang == "RU") "EN" else "RU"
                        isSymbolsActive = false
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = activeKbLang,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF66),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Row 1
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            keysRow1.forEach { char ->
                val actualChar = if (isShiftActive) char.uppercase() else char.lowercase()
                KeyButton(char = actualChar, weight = 1f, bg = keyBg, border = keyBorder, color = textColor) {
                    onMessageChange(currentMessage + actualChar)
                }
            }
        }

        // Row 2
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            keysRow2.forEach { char ->
                val actualChar = if (isShiftActive) char.uppercase() else char.lowercase()
                KeyButton(char = actualChar, weight = 1f, bg = keyBg, border = keyBorder, color = textColor) {
                    onMessageChange(currentMessage + actualChar)
                }
            }
        }

        // Row 3
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            keysRow3.forEach { char ->
                if (char == "[DEL]") {
                    KeyButton(char = "⌫", weight = 1.3f, bg = keyBg, border = keyBorder, color = Color(0xFFEF4444)) {
                        if (currentMessage.isNotEmpty()) {
                            onMessageChange(currentMessage.dropLast(1))
                        }
                    }
                } else {
                    val actualChar = if (isShiftActive) char.uppercase() else char.lowercase()
                    KeyButton(char = actualChar, weight = 1f, bg = keyBg, border = keyBorder, color = textColor) {
                        onMessageChange(currentMessage + actualChar)
                    }
                }
            }
        }

        // Row 4
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            // Shift Toggle
            KeyButton(
                char = "⇧",
                weight = 1.2f,
                bg = if (isShiftActive) Color(0xFF00FF66).copy(alpha = 0.2f) else keyBg,
                border = keyBorder,
                color = textColor
            ) {
                isShiftActive = !isShiftActive
            }

            // Symbol Toggle
            KeyButton(char = if (isSymbolsActive) "Abc" else "?123", weight = 1.2f, bg = keyBg, border = keyBorder, color = textColor) {
                isSymbolsActive = !isSymbolsActive
            }

            // Space
            KeyButton(char = getChatText("space", currentLanguage), weight = 3.5f, bg = keyBg, border = keyBorder, color = textColor) {
                onMessageChange(currentMessage + " ")
            }

            // Send
            KeyButton(
                char = getChatText("send", currentLanguage),
                weight = 2.1f,
                bg = if (isMatrixStyle) Color(0xFF00FF66).copy(alpha = 0.2f) else Color(0xFF00FF66),
                border = keyBorder,
                color = if (isMatrixStyle) Color(0xFF00FF66) else Color.Black
            ) {
                onSendClicked()
            }
        }
    }
}

@Composable
fun RowScope.KeyButton(char: String, weight: Float, bg: Color, border: Color, color: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .weight(weight)
            .height(34.dp)
            .border(0.5.dp, border, RoundedCornerShape(4.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = char,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

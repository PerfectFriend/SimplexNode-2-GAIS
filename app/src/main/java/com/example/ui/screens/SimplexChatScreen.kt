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
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SimplexServer
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SimplexChatMessage

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
            "keyboard_active" to "PRIVATE KEYBOARD ACTIVE: GBOARD BLOCKED",
            "space" to "Space",
            "send" to "SEND",
            "placeholder_cmd" to "Enter command /status, /start, /stop, /test, /logs...",
            "placeholder_msg" to "Enter E2EE message via private keyboard...",
            "inspect_title" to "E2EE Packet Inspector",
            "inspect_sig" to "CRYPTOGRAPHIC SIGNATURE (ED25519):",
            "inspect_cipher" to "PACKET CIPHERTEXT (CHACHA20-POLY1305):",
            "inspect_ephemeral" to "EPHEMERAL DIFFIE-HELLMAN KEY (X25519):",
            "inspect_route" to "DELIVERY ROUTE (ONION):",
            "inspect_index" to "Session index:",
            "inspect_transport" to "Transport:",
            "inspect_close" to "Close",
            "empty_queues" to "Message queues are empty.",
            "empty_desc" to "Enter a command (/status, /test) or message below."
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
            "keyboard_active" to "ПРИВАТНАЯ КЛАВИАТУРА АКТИВНА: GBOARD ЗАБЛОКИРОВАН",
            "space" to "Пробел",
            "send" to "SEND",
            "placeholder_cmd" to "Введите команду /status, /start, /stop, /test, /logs...",
            "placeholder_msg" to "Введите E2EE сообщение через приватную клавиатуру...",
            "inspect_title" to "E2EE Инспектор Пакетов",
            "inspect_sig" to "КРИПТОГРАФИЧЕСКАЯ ПОДПИСЬ (ED25519):",
            "inspect_cipher" to "ШИФРОТЕКСТ ПАКЕТА (CHACHA20-POLY1305):",
            "inspect_ephemeral" to "ЭФЕМЕРНЫЙ КЛЮЧ DIFFIE-HELLMAN (X25519):",
            "inspect_route" to "МАРШРУТ ДОСТАВКИ (ONION):",
            "inspect_index" to "Индекс сессии:",
            "inspect_transport" to "Транспорт:",
            "inspect_close" to "Закрыть",
            "empty_queues" to "Очереди сообщений пусты.",
            "empty_desc" to "Введите команду (/status, /test) или сообщение ниже."
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
            "route_active" to "RUTA ENCRIPCIÓND: ESTÁNDAR MATRIX P2P",
            "secured" to "SEGURO",
            "keyboard_active" to "TECLADO PRIVADO ACTIVO: GBOARD BLOQUEADO",
            "space" to "Espacio",
            "send" to "ENVIAR",
            "placeholder_cmd" to "Introduzca comando /status, /start, /stop, /test, /logs...",
            "placeholder_msg" to "Introduzca mensaje E2EE mediante teclado privado...",
            "inspect_title" to "Inspector de Paquetes E2EE",
            "inspect_sig" to "FIRMA CRIPTOGRÁFICA (ED25519):",
            "inspect_cipher" to "TEXTO CIFRADO DE PAQUETE (CHACHA20-POLY1305):",
            "inspect_ephemeral" to "CLAVE EFFÍMERA DIFFIE-HELLMAN (X25519):",
            "inspect_route" to "RUTA DE ENTREGA (ONION):",
            "inspect_index" to "Índice de sesión:",
            "inspect_transport" to "Transporte:",
            "inspect_close" to "Cerrar",
            "empty_queues" to "Colas de mensajes vacías.",
            "empty_desc" to "Introduzca un comando (/status, /test) o mensaje abajo."
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
            "keyboard_active" to "CLAVIER PRIVÉ ACTIF: GBOARD BLOQUÉ",
            "space" to "Espace",
            "send" to "ENVOYER",
            "placeholder_cmd" to "Entrer commande /status, /start, /stop, /test, /logs...",
            "placeholder_msg" to "Entrer message E2EE via le clavier privé...",
            "inspect_title" to "Inspecteur de Paquets E2EE",
            "inspect_sig" to "SIGNATURE CRYPTOGRAPHIQUE (ED25519):",
            "inspect_cipher" to "CHIFFREMENT DU PAQUET (CHACHA20-POLY1305):",
            "inspect_ephemeral" to "CLÉ DH ÉPHÉMÈRE (X25519):",
            "inspect_route" to "ROUTE DE LIVRAISON (ONION):",
            "inspect_index" to "Index session:",
            "inspect_transport" to "Transport:",
            "inspect_close" to "Fermer",
            "empty_queues" to "Files de messages vides.",
            "empty_desc" to "Entrez une commande (/status, /test) ou un message ci-dessous."
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
            "keyboard_active" to "PRIVATES TASTATURFELD AKTIV: GBOARD BLOCKIERT",
            "space" to "Leertaste",
            "send" to "SENDEN",
            "placeholder_cmd" to "Befehl eingeben /status, /start, /stop, /test, /logs...",
            "placeholder_msg" to "E2EE-Nachricht über private Tastatur eingeben...",
            "inspect_title" to "E2EE-Paket-Inspektor",
            "inspect_sig" to "KRYPTOGRAPHISCHE SIGNATUR (ED25519):",
            "inspect_cipher" to "PAKET-CHIFFRETEXT (CHACHA20-POLY1305):",
            "inspect_ephemeral" to "EPHEMERER DIFFIE-HELLMAN-SCHLÜSSEL (X25519):",
            "inspect_route" to "ZUSTELLUNGSROUTE (ONION):",
            "inspect_index" to "Sitzungsindex:",
            "inspect_transport" to "Transport:",
            "inspect_close" to "Schließen",
            "empty_queues" to "Nachrichtenwarteschlangen sind leer.",
            "empty_desc" to "Geben Sie einen Befehl (/status, /test) oder eine Nachricht unten ein."
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
            "keyboard_active" to "安全虚拟键盘已启用: 已拦截 GBOARD telemetry 收集",
            "space" to "空格",
            "send" to "发送",
            "placeholder_cmd" to "输入控制命令 /status, /start, /stop, /test, /logs...",
            "placeholder_msg" to "请使用内置安全键盘输入端到端加密消息...",
            "inspect_title" to "端到端加密数据包审计器",
            "inspect_sig" to "密码学数字签名 (ED25519):",
            "inspect_cipher" to "数据包密文载荷 (CHACHA20-POLY1305):",
            "inspect_ephemeral" to "临时 Diffie-Hellman 协商公钥 (X25519):",
            "inspect_route" to "网格匿名洋葱递送链路 (ONION):",
            "inspect_index" to "密码流索引号:",
            "inspect_transport" to "物理中继通道:",
            "inspect_close" to "关闭",
            "empty_queues" to "消息队列暂未接收到数据包",
            "empty_desc" to "请使用下方内置键盘输入调试指令或会话消息。"
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

    // Collect upgraded secure E2EE chat flows
    val chatMessages by viewModel.simplexChatMessages.collectAsState()
    val isSendingMessage by viewModel.isSendingMessage.collectAsState()
    val sendingStep by viewModel.sendingStep.collectAsState()
    val sendingProgress by viewModel.sendingProgress.collectAsState()
    val sendingPacketHex by viewModel.sendingPacketHex.collectAsState()
    val doubleRatchetSequence by viewModel.doubleRatchetSequence.collectAsState()
    val clientEphemeralPublicKey by viewModel.clientEphemeralPublicKey.collectAsState()
    val peerEphemeralPublicKey by viewModel.peerEphemeralPublicKey.collectAsState()
    val relayMode by viewModel.relayMode.collectAsState()

    var selectedServer by remember { mutableStateOf<SimplexServer?>(null) }
    var currentMessage by remember { mutableStateOf("") }
    var activeInspectMessage by remember { mutableStateOf<SimplexChatMessage?>(null) }

    // Multi-View Matrix Configuration State
    var isMatrixMode by remember { mutableStateOf(false) }
    var activeRoom by remember { mutableStateOf("#control:simplex-node.onion") } // #control:..., #general:..., #logs:...

    if (servers.isNotEmpty() && selectedServer == null) {
        selectedServer = servers.first()
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
                        text = "E2EE Инспектор Пакетов",
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
                        text = "КРИПТОГРАФИЧЕСКАЯ ПОДПИСЬ (ED25519):",
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
                        text = "ШИФРОТЕКСТ ПАКЕТА (CHACHA20-POLY1305):",
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
                        text = "ЭФЕМЕРНЫЙ КЛЮЧ DIFFIE-HELLMAN (X25519):",
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
                        text = "МАРШРУТ ДОСТАВКИ (ONION):",
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

    // Top-Level Screen Box wrapping E2EE chats and Matrix interface
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isMatrixMode) Color(0xFF070A0F) else MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        // Multi-View Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMatrixMode) getChatText("matrix_console", currentLanguage) else getChatText("simplex_queues", currentLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurface,
                    fontFamily = if (isMatrixMode) FontFamily.Monospace else FontFamily.Default
                )
                Text(
                    text = if (isMatrixMode) getChatText("matrix_desc", currentLanguage) else getChatText("simplex_desc", currentLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
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
            // Room Selection Headers for Matrix view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
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
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = roomLabel.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF00FF66) else Color(0xFF64748B),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = roomKey.take(8) + "...",
                                fontSize = 8.sp,
                                color = if (isSelected) Color(0xFF00FF66).copy(alpha = 0.7f) else Color(0xFF475569),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Connection warnings & Encryption Status
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1015)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = getChatText("route_active", currentLanguage),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF66)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF00FF66).copy(alpha = 0.12f))
                            .border(0.5.dp, Color(0xFF00FF66), RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(getChatText("secured", currentLanguage), fontSize = 8.sp, color = Color(0xFF00FF66), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
                    .padding(10.dp)
            ) {
                // Filter messages depending on selected active room simulation
                val filteredMessages = chatMessages.filter { msg ->
                    when (activeRoom) {
                        "#control:simplex-node.onion" -> {
                            // Control room matches commands and automated terminal replies
                            msg.text.startsWith("/") || msg.sender == "peer" || msg.text.contains("КОМАНДА") || msg.text.contains("TELEMETRY") || msg.text.contains("Службы") || msg.text.contains("Диагностика")
                        }
                        "#logs:simplex-node.onion" -> {
                            // Simulation of packet logs or hex payloads
                            msg.sender == "me" || msg.text.contains("Acknowledge") || msg.text.contains("relay")
                        }
                        else -> {
                            // General discussions
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
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Консоль '${activeRoom}' пуста.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Введите команду или текст ниже.",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF475569)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredMessages) { msg ->
                            val isMe = msg.sender == "me"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isMe) "@user:local.onion" else "@admin:simplex-node.onion",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = if (isMe) Color(0xFF38BDF8) else Color(0xFF00FF66)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isMe) "[Локальный]" else "[Администратор]",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    // Inspect E2EE parameters
                                    IconButton(
                                        onClick = { activeInspectMessage = msg },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = "Ключи",
                                            tint = Color(0xFF00FF66).copy(alpha = 0.5f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F17)),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.5.dp, Color(0xFF1F2937), RoundedCornerShape(4.dp))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isMe) Color(0xFFE2E8F0) else Color(0xFF00FF66),
                                            lineHeight = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
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
            // --- RENDER STANDARD SIMPLEX QUEUES MODE ---
            // Connection Warnings
            if (!torEnabled && !tailscaleEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ПРЕДУПРЕЖДЕНИЕ: Маршрутизаторы Tor/Tailscale отключены. Безопасность трафика ограничена.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Server node chips/selection list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
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
                            .padding(vertical = 8.dp, horizontal = 4.dp),
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Active Server Details
            selectedServer?.let { server ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("АКТИВНЫЙ ТРАНСПОРТНЫЙ УЗЕЛ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Адрес: ${server.address}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ID Очереди: ${server.channelId}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // --- DOUBLE RATCHET KEY STATE DISPLAY ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .padding(bottom = 10.dp)
                    .testTag("ratchet_status_header")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Криптографическая сессия Double Ratchet",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Rotate Keys action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .clickable { viewModel.rotateDoubleRatchetKeys() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("rotate_keys_badge")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сбросить ключи", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Мой Ключ X25519 DH", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                            Text(clientEphemeralPublicKey.take(18) + "...", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFFE5A93C))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Ключ Собеседника", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                            Text(peerEphemeralPublicKey.take(18) + "...", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                        }
                    }
                }
            }

            // Normal mode Chat list
            Text(
                text = "СООБЩЕНИЯ В ОЧЕРЕДИ SIMPLEX:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF090D14))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Очереди сообщений пусты.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text("Введите команду (/status, /test) или сообщение ниже.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(chatMessages) { msg ->
                            val isMe = msg.sender == "me"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                Text(
                                    text = if (isMe) "Вы (Локальный клиент)" else "Администратор (Simplex Транспорт)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    if (!isMe) {
                                        IconButton(
                                            onClick = { activeInspectMessage = msg },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    Column(
                                        modifier = Modifier
                                            .widthIn(max = 240.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(text = msg.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(10.dp))
                                            Text(text = "Ratchet Index: #${msg.ratchetIndex}", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- COMMON ENCRYPTED DYNAMIC INPUT & PRIVATE KEYBOARD MODULE ---
        Spacer(modifier = Modifier.height(8.dp))

        // Visual Display of typed text with Animated Blinking Cursor
        var showCursor by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(500)
                showCursor = !showCursor
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isMatrixMode) Color(0xFF0B1015) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .border(
                    1.dp,
                    if (isMatrixMode) Color(0xFF00FF66).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(8.dp)
                )
                .clickable { /* Prevents standard system keyboard focus completely! */ }
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isMatrixMode) Color(0xFF00FF66) else Color(0xFF00E676),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    if (currentMessage.isEmpty()) {
                        Text(
                            text = if (isMatrixMode) getChatText("placeholder_cmd", currentLanguage) else getChatText("placeholder_msg", currentLanguage),
                            fontSize = 11.sp,
                            fontFamily = if (isMatrixMode) FontFamily.Monospace else FontFamily.Default,
                            color = if (isMatrixMode) Color(0xFF64748B) else MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentMessage,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurface
                            )
                            if (showCursor) {
                                Text(
                                    text = "_",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMatrixMode) Color(0xFF00FF66) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 1.dp)
                                )
                            }
                        }
                    }
                }
                
                if (currentMessage.isNotEmpty()) {
                    IconButton(
                        onClick = { currentMessage = "" },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Render Private Matrix Styled Keyboard
        PrivateComposeKeyboard(
            currentMessage = currentMessage,
            onMessageChange = { currentMessage = it },
            onSendClicked = {
                if (currentMessage.isNotBlank()) {
                    selectedServer?.let { server ->
                        viewModel.submitSimplexMessage(server, currentMessage)
                        currentMessage = ""
                    }
                }
            },
            isMatrixStyle = isMatrixMode,
            currentLanguage = currentLanguage
        )
    }
}

@Composable
fun PrivateComposeKeyboard(
    currentMessage: String,
    onMessageChange: (String) -> Unit,
    onSendClicked: () -> Unit,
    isMatrixStyle: Boolean,
    currentLanguage: String
) {
    var isShiftActive by remember { mutableStateOf(false) }
    var isSymbolsActive by remember { mutableStateOf(false) }

    val keysRow1 = if (isSymbolsActive) listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0") 
                   else listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val keysRow2 = if (isSymbolsActive) listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")")
                   else listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "/")
    val keysRow3 = if (isSymbolsActive) listOf("+", "-", "=", "_", "<", ">", "?", ":", ";", "[DEL]")
                   else listOf("z", "x", "c", "v", "b", "n", "m", "_", "[DEL]")

    val keyboardBg = if (isMatrixStyle) Color(0xFF080C11) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val keyBg = if (isMatrixStyle) Color(0xFF11161D) else MaterialTheme.colorScheme.surface
    val keyBorder = if (isMatrixStyle) Color(0xFF00FF66).copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    val textColor = if (isMatrixStyle) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(keyboardBg, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .border(1.dp, keyBorder, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Safe keyboard shield notice
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isShiftActive) Color.Yellow else Color(0xFF00FF66))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = getChatText("keyboard_active", currentLanguage),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isMatrixStyle) Color(0xFF00FF66).copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = if (isShiftActive) "CAPS: ON" else "CAPS: OFF",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isMatrixStyle) Color(0xFF00FF66).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
            )
        }

        // Row 1
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            keysRow1.forEach { char ->
                val actualChar = if (isShiftActive) char.uppercase() else char.lowercase()
                KeyButton(char = actualChar, weight = 1f, bg = keyBg, border = keyBorder, color = textColor) {
                    onMessageChange(currentMessage + actualChar)
                }
            }
        }

        // Row 2
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            keysRow2.forEach { char ->
                val actualChar = if (isShiftActive) char.uppercase() else char.lowercase()
                KeyButton(char = actualChar, weight = 1f, bg = keyBg, border = keyBorder, color = textColor) {
                    onMessageChange(currentMessage + actualChar)
                }
            }
        }

        // Row 3
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
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
                bg = if (isMatrixStyle) Color(0xFF00FF66).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary, 
                border = keyBorder, 
                color = if (isMatrixStyle) Color(0xFF00FF66) else MaterialTheme.colorScheme.onPrimary
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
            .height(38.dp)
            .border(1.dp, border, RoundedCornerShape(4.dp))
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

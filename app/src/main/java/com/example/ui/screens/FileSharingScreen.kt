package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel

// Localized helper specifically tailored for File Sharing Screen
fun getFileText(key: String, lang: String): String {
    val dict = mapOf(
        "EN" to mapOf(
            "files_title" to "E2E Encrypted Storage",
            "files_subtitle" to "Protect sensitive files locally and generate E2E metadata mappings with AES-GCM-256 keys.",
            "locked_warning" to "Encryption Vault is LOCKED. Authenticate in Vault tab to encrypt and share files.",
            "cloud_title" to "Decentralized Crypto Cloud",
            "active_swarm" to "ACTIVE SWARM: %s Peering Nodes",
            "cloud_offline" to "CLOUD OFFLINE: Seeding Disengaged",
            "collapse" to "Collapse",
            "expand" to "Expand Details",
            "container_limit" to "CONTAINER LIMIT",
            "swarm_sync" to "SWARM SYNC RATE",
            "allocation_size" to "CRYPTOCONTAINER LOCAL ALLOCATION SIZE:",
            "wifi_only" to "WI-FI ONLY",
            "conserve_data" to "Conserve mobile data",
            "seeding" to "SEEDING ACTIVE",
            "broadcast" to "Broadcasting state",
            "torrent_map" to "DISTRIBUTED TORRENT PIECES ACQUISITION MAP (75% COMPLETED SECTORS):",
            "sync_terminal" to "DISTRIBUTED CLOUD SYNC STATE TERMINAL:",
            "clear_logs" to "Clear Logs",
            "terminal_empty" to "Terminal history empty.",
            "managed_repos" to "MANAGED REPOSITORIES:",
            "secure_segment" to "Secure File Segment",
            "empty_encrypted_title" to "Encrypted storage empty.",
            "empty_encrypted_desc" to "Create secure localized crypt files above.",
            "encrypted_chunk_size" to "Encrypted Chunk Size:",
            "payload_header" to "PAYLOAD (AES-GCM-256 CIPHER BLOCK):",
            "sharing_endpoint" to "ONION SHARING ENDPOINT ACTION",
            "seal_title" to "Seal Segment with AES-256 GCM",
            "label_name" to "File / Segment Label Name",
            "label_contents" to "Raw Segment Contents / SSH Credentials",
            "label_recipient" to "Recipient Onion / Host Alias (Optional)",
            "btn_seal" to "Seal & Publish",
            "btn_cancel" to "Cancel"
        ),
        "RU" to mapOf(
            "files_title" to "Шифрование и Обмен",
            "files_subtitle" to "Защищайте конфиденциальные документы локально и раздавайте сегменты децентрализованно через P2P.",
            "locked_warning" to "Шифровальный сейф заблокирован! Авторизуйтесь во вкладке Сейф для работы с файлами.",
            "cloud_title" to "Децентрализованное Криптооблако",
            "active_swarm" to "АКТИВНЫЙ РОЙ: %s пиринговых узлов",
            "cloud_offline" to "ОБЛАКО ОФФЛАЙН: Раздача отключена",
            "collapse" to "Свернуть",
            "expand" to "Подробнее",
            "container_limit" to "ЛИМИТ КОНТЕЙНЕРА",
            "swarm_sync" to "СКОРОСТЬ СИНХРОНИЗАЦИИ",
            "allocation_size" to "ЛОКАЛЬНЫЙ РАЗМЕР КРИПТОКОНТЕЙНЕРА:",
            "wifi_only" to "ТОЛЬКО WI-FI",
            "conserve_data" to "Экономия мобильного трафика",
            "seeding" to "РАЗДАЧА",
            "broadcast" to "Трансляция сегментов",
            "torrent_map" to "КАРТА СЕГМЕНТОВ РАСПРЕДЕЛЕННОГО ТОРРЕНТА (75% СЕКТОРОВ ЗАВЕРШЕНО):",
            "sync_terminal" to "ТЕРМИНАЛ СИНХРОНИЗАЦИИ ДЕЦЕНТРАЛИЗОВАННОГО ОБЛАКА:",
            "clear_logs" to "Очистить логи",
            "terminal_empty" to "История терминала пуста.",
            "managed_repos" to "КОНФИДЕНЦИАЛЬНЫЕ ФАЙЛЫ:",
            "secure_segment" to "Зашифровать файл",
            "empty_encrypted_title" to "Шифрованное хранилище пусто.",
            "empty_encrypted_desc" to "Зашифруйте локальные файлы через кнопку выше.",
            "encrypted_chunk_size" to "Размер зашифрованного сегмента:",
            "payload_header" to "ДАННЫЕ (ШИФРБЛОК AES-GCM-256):",
            "sharing_endpoint" to "ОДНОРАЗОВАЯ ONION-ССЫЛКА ДОСТУПА",
            "seal_title" to "Запечатать сегмент ключом AES-256 GCM",
            "label_name" to "Метка / Имя файла",
            "label_contents" to "Конфиденциальное содержимое / Пароли",
            "label_recipient" to "Onion получателя / Хост-алиас (опционально)",
            "btn_seal" to "Запечатать и опубликовать",
            "btn_cancel" to "Отмена"
        ),
        "ES" to mapOf(
            "files_title" to "Almacenamiento Cifrado E2E",
            "files_subtitle" to "Proteja archivos confidenciales localmente y genere mapas de metadatos E2E con claves AES-GCM-256.",
            "locked_warning" to "La bóveda de cifrado está BLOQUEADA. Autentíquese en la pestaña Bóveda para cifrar y compartir.",
            "cloud_title" to "Nube Criptográfica Descentralizada",
            "active_swarm" to "ENJAMBRE ACTIVO: %s Nodos de Peering",
            "cloud_offline" to "NUBE DESCONECTADA: Envío desactivado",
            "collapse" to "Colapsar",
            "expand" to "Expandir detalles",
            "container_limit" to "LÍMITE DEL CONTENEDOR",
            "swarm_sync" to "TASA DE SINC DE ENJAMBRE",
            "allocation_size" to "TAMAÑO DE ASIGNACIÓN LOCAL DEL CRIPTOCONTENEDOR:",
            "wifi_only" to "SOLO WI-FI",
            "conserve_data" to "Conservar datos móviles",
            "seeding" to "TRANSMISIÓN",
            "broadcast" to "Transmitiendo estado",
            "torrent_map" to "MAPA DE ADQUISICIÓN DE PIEZAS TORRENT (75% COMPLETADO):",
            "sync_terminal" to "TERMINAL DE ESTADO DE SINC DE NUBE:",
            "clear_logs" to "Limpiar logs",
            "terminal_empty" to "Historial de terminal vacío.",
            "managed_repos" to "REPOSITORIOS GESTIONADOS:",
            "secure_segment" to "Asegurar Segmento de Archivo",
            "empty_encrypted_title" to "Almacenamiento cifrado vacío.",
            "empty_encrypted_desc" to "Cree archivos cifrados localizados arriba.",
            "encrypted_chunk_size" to "Tamaño del fragmento cifrado:",
            "payload_header" to "CARGA ÚTIL (BLOQUE AES-GCM-256):",
            "sharing_endpoint" to "PUNTO DE ACCESO DE COMPARTIDO ONION",
            "seal_title" to "Sellar Segmento con AES-256 GCM",
            "label_name" to "Nombre de etiqueta del segmento",
            "label_contents" to "Contenido del segmento / Credenciales",
            "label_recipient" to "Onion del destinatario / Alias de host (Opcional)",
            "btn_seal" to "Sellar y Publicar",
            "btn_cancel" to "Cancelar"
        ),
        "FR" to mapOf(
            "files_title" to "Stockage Chiffré E2E",
            "files_subtitle" to "Protégez les fichiers sensibles localement et générez des mappages de métadonnées E2E avec des clés AES-GCM-256.",
            "locked_warning" to "Le coffre-fort de chiffrement est VERROUILLÉ. Authentifiez-vous dans l'onglet Coffre-fort.",
            "cloud_title" to "Nuage Cryptographique Décentralisé",
            "active_swarm" to "ESSAIM ACTIF : %s nœuds de peering",
            "cloud_offline" to "CLOUD HORS LIGNE: Diffusion désactivée",
            "collapse" to "Réduire",
            "expand" to "Développer",
            "container_limit" to "LIMITE DU CONTENEUR",
            "swarm_sync" to "TAUX DE SYNC DE L'ESSAIM",
            "allocation_size" to "TAILLE D'ALLOCATION LOCALE DU CRYPTOCONTENEUR :",
            "wifi_only" to "WI-FI UNIQUEMENT",
            "conserve_data" to "Économiser les données mobiles",
            "seeding" to "DIFFUSION",
            "broadcast" to "Diffusion de l'état",
            "torrent_map" to "CARTE D'ACQUISITION DES PIÈCES TORRENT (75% COMPLÉTÉ) :",
            "sync_terminal" to "TERMINAL DE SYNC DU CLOUD DISTRIBUÉ :",
            "clear_logs" to "Effacer",
            "terminal_empty" to "Historique vide.",
            "managed_repos" to "RÉPERTOIRES GÉRÉS :",
            "secure_segment" to "Sécuriser le segment",
            "empty_encrypted_title" to "Stockage chiffré vide.",
            "empty_encrypted_desc" to "Créez des fichiers cryptés ci-dessus.",
            "encrypted_chunk_size" to "Taille du morceau chiffré :",
            "payload_header" to "CHARGE UTILE (BLOC AES-GCM-256) :",
            "sharing_endpoint" to "POINT D'ACCÈS ONION DE PARTAGE",
            "seal_title" to "Sceller le segment avec AES-256 GCM",
            "label_name" to "Nom de l'étiquette",
            "label_contents" to "Contenu brut / Identifiants",
            "label_recipient" to "Onion destinataire / Alias (Optionnel)",
            "btn_seal" to "Sceller & Publier",
            "btn_cancel" to "Annuler"
        ),
        "DE" to mapOf(
            "files_title" to "E2E Verschlüsselter Speicher",
            "files_subtitle" to "Schützen Sie sensible Dateien lokal und generieren Sie E2E-Metadatenzuordnungen mit AES-GCM-256-Schlüsseln.",
            "locked_warning" to "Verschlüsselungstresor ist GESPERRT. Bitte im Tresor-Tab entsperren.",
            "cloud_title" to "Dezentrale Krypto-Cloud",
            "active_swarm" to "AKTIVER SCHWARM: %s Peering-Knoten",
            "cloud_offline" to "CLOUD OFFLINE: Seeding deaktiviert",
            "collapse" to "Einklappen",
            "expand" to "Ausklappen",
            "container_limit" to "CONTAINER-LIMIT",
            "swarm_sync" to "SCHWARM-SYNCHRONISATIONSRATE",
            "allocation_size" to "LOKALE ZUORDNUNGSGRÖSSE DES KRYPTOCONTAINERS:",
            "wifi_only" to "NUR WLAN",
            "conserve_data" to "Mobile Daten sparen",
            "seeding" to "SEEDING",
            "broadcast" to "Sendezustand",
            "torrent_map" to "VERTEILTE TORRENT-ERFASSUNGSKARTE (75% BEENDET):",
            "sync_terminal" to "VERTEILTER CLOUD-SYNC-STATUS-TERMINAL:",
            "clear_logs" to "Logs löschen",
            "terminal_empty" to "Terminal-Verlauf leer.",
            "managed_repos" to "VERWALTETE REPOSITORIES:",
            "secure_segment" to "Dateisegment sichern",
            "empty_encrypted_title" to "Verschlüsselter Speicher leer.",
            "empty_encrypted_desc" to "Erstellen Sie oben verschlüsselte Dateien.",
            "encrypted_chunk_size" to "Verschlüsselte Segmentgröße:",
            "payload_header" to "PAYLOAD (AES-GCM-256 CHIFFREBLOCK):",
            "sharing_endpoint" to "ONION-TEILUNGS-ENDPUNKT",
            "seal_title" to "Segment mit AES-256 GCM versiegeln",
            "label_name" to "Segment Label Name",
            "label_contents" to "Roher Segmentinhalt / SSH-Daten",
            "label_recipient" to "Empfänger Onion / Host-Alias (Optional)",
            "btn_seal" to "Versiegeln & veröff.",
            "btn_cancel" to "Abbrechen"
        ),
        "ZH" to mapOf(
            "files_title" to "E2E 端到端加密存储",
            "files_subtitle" to "在本地保护敏感文件，并使用 AES-GCM-256 密钥生成 E2E 元数据映射。",
            "locked_warning" to "加密保险库已锁定！请在安全箱选项卡中进行身份验证以操作文件。",
            "cloud_title" to "去中心化加密云端",
            "active_swarm" to "活动群集: %s 个对等节点",
            "cloud_offline" to "云端离线: 播种分发已停止",
            "collapse" to "折叠",
            "expand" to "展开详情",
            "container_limit" to "容器限制",
            "swarm_sync" to "群集同步速率",
            "allocation_size" to "本地加密容器分配大小:",
            "wifi_only" to "仅限 WI-FI",
            "conserve_data" to "节省移动数据流量",
            "seeding" to "播种分发",
            "broadcast" to "广播状态信息",
            "torrent_map" to "分布式种子块采集地图 (已完成 75%):",
            "sync_terminal" to "分布式云同步状态终端:",
            "clear_logs" to "清除日志",
            "terminal_empty" to "终端历史记录为空。",
            "managed_repos" to "托管存储库:",
            "secure_segment" to "加密保护新文件",
            "empty_encrypted_title" to "加密存储库为空。",
            "empty_encrypted_desc" to "请通过上方的按钮创建安全加密文件。",
            "encrypted_chunk_size" to "加密分块大小:",
            "payload_header" to "加密载荷 (AES-GCM-256 密码块):",
            "sharing_endpoint" to "ONION 共享端点地址",
            "seal_title" to "使用 AES-256 GCM 封印文件段",
            "label_name" to "文件 / 分段标签名称",
            "label_contents" to "原始分段内容 / 敏感凭据",
            "label_recipient" to "接收者 Onion / 主机别名 (可选)",
            "btn_seal" to "封印并发布",
            "btn_cancel" to "取消"
        )
    )
    val langMap = dict[lang] ?: dict["EN"]!!
    return langMap[key] ?: (dict["EN"]!![key] ?: key)
}

@Composable
fun FileSharingScreen(viewModel: MainViewModel) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val sharedFiles by viewModel.sharedFiles.collectAsState()

    val cryptoContainerSizeGb by viewModel.cryptoContainerSizeGb.collectAsState()
    val wifiOnlySync by viewModel.wifiOnlySync.collectAsState()
    val torrentSeedingEnabled by viewModel.torrentSeedingEnabled.collectAsState()
    val activeTorrentPeersCount by viewModel.activeTorrentPeersCount.collectAsState()
    val torrentDownloadSpeed by viewModel.torrentDownloadSpeed.collectAsState()
    val torrentUploadSpeed by viewModel.torrentUploadSpeed.collectAsState()
    val torrentPieces by viewModel.torrentPieces.collectAsState()
    val cloudSyncLogs by viewModel.cloudSyncLogs.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }
    var filePayloadText by remember { mutableStateOf("") }
    var recipientAlias by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(isUnlocked) {
        if (!isUnlocked) {
            showAddDialog = false
            fileName = ""
            filePayloadText = ""
            recipientAlias = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Upper text elements
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = getFileText("files_title", currentLanguage),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = getFileText("files_subtitle", currentLanguage),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isUnlocked) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = getFileText("locked_warning", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // --- DECENTRALIZED CRYPTO CLOUD & TORRENT SYNCHRONIZER ---
        var isCloudPanelExpanded by remember { mutableStateOf(false) }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Crypto Cloud",
                            tint = if (torrentSeedingEnabled) Color(0xFF00B0FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = getFileText("cloud_title", currentLanguage),
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val infoStr = if (torrentSeedingEnabled) {
                                String.format(getFileText("active_swarm", currentLanguage), activeTorrentPeersCount.toString())
                            } else {
                                getFileText("cloud_offline", currentLanguage)
                            }
                            Text(
                                text = infoStr,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = if (torrentSeedingEnabled) Color(0xFF00FF66) else MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    TextButton(
                        onClick = { isCloudPanelExpanded = !isCloudPanelExpanded },
                        modifier = Modifier.testTag("toggle_cloud_panel_button")
                    ) {
                        Text(
                            text = if (isCloudPanelExpanded) getFileText("collapse", currentLanguage) else getFileText("expand", currentLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Basic Quick Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Container size indicator
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(getFileText("container_limit", currentLanguage), fontSize = 8.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${cryptoContainerSizeGb} GB Sparse", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Seeding speed indicators
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(2f).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(getFileText("swarm_sync", currentLanguage), fontSize = 8.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = Color(0xFF00FF66), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(torrentDownloadSpeed, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF00FF66))
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = Color(0xFF00B0FF), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(torrentUploadSpeed, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF00B0FF))
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = isCloudPanelExpanded) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Configuration inputs section
                        Text(getFileText("allocation_size", currentLanguage), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val containerSizes = listOf(16, 32, 64, 128)
                            containerSizes.forEach { size ->
                                val isSelected = cryptoContainerSizeGb == size
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.setCryptoContainerSize(size) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${size} GB",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Controls Row (Wi-Fi sync and Seeding)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Wi-Fi Switch
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(getFileText("wifi_only", currentLanguage), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(getFileText("conserve_data", currentLanguage), fontSize = 7.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Switch(
                                        checked = wifiOnlySync,
                                        onCheckedChange = { viewModel.toggleWifiOnlySync(it) },
                                        modifier = Modifier.scale(0.8f).testTag("wifi_only_sync_switch")
                                    )
                                }
                            }

                            // Torrent Broadcast Switch
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(getFileText("seeding", currentLanguage), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(getFileText("broadcast", currentLanguage), fontSize = 7.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Switch(
                                        checked = torrentSeedingEnabled,
                                        onCheckedChange = { viewModel.toggleTorrentSeeding(it) },
                                        modifier = Modifier.scale(0.8f).testTag("seeding_switch")
                                    )
                                }
                            }
                        }

                        // --- TORRENT PIECES VISUAL MAP ---
                        Spacer(modifier = Modifier.height(12.dp))
                        val completedCount = torrentPieces.count { it }
                        val totalCount = torrentPieces.size.coerceAtLeast(1)
                        val completionPct = completedCount * 100 / totalCount
                        Text(
                            text = "P2P CRYPTOCONTAINER SECTORS ($completedCount/$totalCount BLOCKS — $completionPct% STORED):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Draw a grid representing the piece blocks
                        val activeColor = if (torrentSeedingEnabled) Color(0xFF00FF66) else Color(0x3300FF66)
                        val idleColor = Color(0xFF1E293B)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF04060A))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                repeat(5) { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        repeat(20) { col ->
                                            val index = row * 20 + col
                                            val isDownloaded = torrentPieces.getOrElse(index) { false }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(1.dp))
                                                    .background(if (isDownloaded) activeColor else idleColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // --- ENGINE EVENT LOG MONITOR ---
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getFileText("sync_terminal", currentLanguage),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                            Text(
                                text = getFileText("clear_logs", currentLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.clearCloudSyncLogs() }
                                    .testTag("clear_cloud_sync_logs")
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF04060A))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            if (cloudSyncLogs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(getFileText("terminal_empty", currentLanguage), style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(cloudSyncLogs) { log ->
                                        Text(
                                            text = log,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (log.contains("COMPLETE")) Color(0xFF00FF66) else Color(0xFF94A3B8)
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

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getFileText("managed_repos", currentLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )

            Button(
                onClick = { showAddDialog = true },
                enabled = isUnlocked,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("add_file_share_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(getFileText("secure_segment", currentLanguage), fontSize = 11.sp)
            }
        }

        // Active items view
        if (sharedFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.FolderCopy,
                        contentDescription = "Empty file sharing directory",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = getFileText("empty_encrypted_title", currentLanguage),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = getFileText("empty_encrypted_desc", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sharedFiles) { file ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.EnhancedEncryption,
                                        contentDescription = "Encrypted segment",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = file.fileName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${getFileText("encrypted_chunk_size", currentLanguage)} ${file.sizeLabel}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.toggleFilePin(file.id, !file.isPinned) }) {
                                        Icon(
                                            imageVector = if (file.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                            contentDescription = "Pin/Unpin",
                                            tint = if (file.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteSharedFile(file.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Revoke item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Encryption Key Signature details
                            Text(getFileText("payload_header", currentLanguage), fontSize = 9.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF090D14))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = file.encryptedPayload.take(120) + "...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color(0xFF00FF66),
                                    fontSize = 9.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action share address line
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(getFileText("sharing_endpoint", currentLanguage), fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = file.accessUrl,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                                Row {
                                    IconButton(modifier = Modifier.size(24.dp), onClick = { clipboardManager.setText(AnnotatedString(file.encryptedPayload)) }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy encryption payload", modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(modifier = Modifier.size(24.dp), onClick = { clipboardManager.setText(AnnotatedString(file.accessUrl)) }) {
                                        Icon(Icons.Default.Share, contentDescription = "Copy Onion address link", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(getFileText("seal_title", currentLanguage), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text(getFileText("label_name", currentLanguage), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("input_file_name")
                    )
                    OutlinedTextField(
                        value = filePayloadText,
                        onValueChange = { filePayloadText = it },
                        label = { Text(getFileText("label_contents", currentLanguage), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("input_file_contents")
                    )
                    OutlinedTextField(
                        value = recipientAlias,
                        onValueChange = { recipientAlias = it },
                        label = { Text(getFileText("label_recipient", currentLanguage), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("input_recipient_alias")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileName.isNotBlank() && filePayloadText.isNotBlank()) {
                            viewModel.encryptAndShareFile(
                                name = fileName,
                                rawContent = filePayloadText,
                                recipient = recipientAlias.ifBlank { "Unassigned" }
                            )
                            fileName = ""
                            filePayloadText = ""
                            recipientAlias = ""
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_seal_confirm_button")
                ) {
                    Text(getFileText("btn_seal", currentLanguage), fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(getFileText("btn_cancel", currentLanguage), fontSize = 11.sp)
                }
            }
        )
    }
}

// Helper expansion for float scaling
fun Modifier.scale(scale: Float): Modifier = this

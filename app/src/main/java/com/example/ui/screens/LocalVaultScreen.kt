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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.theme.Localization
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.OpenableColumns
import android.content.ContentResolver

@Composable
fun LocalVaultScreen(viewModel: MainViewModel) {
    val isPasswordSetup by viewModel.isPasswordSetup.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val decryptedItems by viewModel.decryptedVaultItems.collectAsState()

    // Cryptographic pipeline states
    val vaultFileEncrypting by viewModel.vaultFileEncrypting.collectAsState()
    val vaultFileProgress by viewModel.vaultFileProgress.collectAsState()
    val vaultFileStatus by viewModel.vaultFileStatus.collectAsState()
    val vaultFileLogs by viewModel.vaultFileLogs.collectAsState()

    // Zero-Knowledge cloud backup states
    val zkCloudBackupEnabled by viewModel.zkCloudBackupEnabled.collectAsState()
    val zkCloudBackupUrl by viewModel.zkCloudBackupUrl.collectAsState()
    val zkCloudSyncFrequencyMinutes by viewModel.zkCloudSyncFrequencyMinutes.collectAsState()
    val zkCloudBackupStatus by viewModel.zkCloudBackupStatus.collectAsState()
    val zkLastCloudSyncTimestamp by viewModel.zkLastCloudSyncTimestamp.collectAsState()
    val cloudSyncLogs by viewModel.cloudSyncLogs.collectAsState()

    // BIP-39 & Seeder Telemetry states
    val bip39Mnemonic by viewModel.bip39Mnemonic.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()
    val isScreenOff by viewModel.isScreenOff.collectAsState()
    val seederPingMs by viewModel.seederPingMs.collectAsState()
    val isActiveNodeMode by viewModel.isActiveNodeMode.collectAsState()
    val lastLruEvictionLog by viewModel.lastLruEvictionLog.collectAsState()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                val name = getFileName(contentResolver, uri) ?: "imported_file.bin"
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                viewModel.encryptAndSaveVaultFile(name, bytes, mimeType)
            } catch (e: Exception) {
                viewModel.addSystemLog("VAULT_PICK", "Failed to load picked file: ${e.localizedMessage}", "FAILED")
            }
        }
    }

    var masterPasswordInput by remember { mutableStateOf("") }
    var showAddCredentialDialog by remember { mutableStateOf(false) }
    var showDashboard by remember { mutableStateOf(true) }

    // Backup & Restore state
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var backupPassphrase by remember { mutableStateOf("") }
    var outputBackupPayload by remember { mutableStateOf("") }
    var inputBackupRestorePayload by remember { mutableStateOf("") }

    // Forms fields
    var credentialTitle by remember { mutableStateOf("") }
    var credentialUser by remember { mutableStateOf("") }
    var credentialSecret by remember { mutableStateOf("") }
    var credentialNotes by remember { mutableStateOf("") }

    // File Vault Drop Zone and interactive states
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Credentials, 1 = File Vault
    var isFileVaultUnlocked by remember { mutableStateOf(false) }
    var fileVaultUnlockPassword by remember { mutableStateOf("") }
    var fileVaultUnlockError by remember { mutableStateOf(false) }
    var droppedFileName by remember { mutableStateOf("") }
    var droppedFileSizeLabel by remember { mutableStateOf("") }
    var droppedFileContent by remember { mutableStateOf("") }
    var showCreateCustomFileDialog by remember { mutableStateOf(false) }
    var customFileNameInput by remember { mutableStateOf("") }
    var customFileContentInput by remember { mutableStateOf("") }
    var viewingFileItem by remember { mutableStateOf<MainViewModel.DecryptedVaultItem?>(null) }

    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(isUnlocked) {
        if (!isUnlocked) {
            masterPasswordInput = ""
            showAddCredentialDialog = false
            showBackupRestoreDialog = false
            backupPassphrase = ""
            outputBackupPayload = ""
            inputBackupRestorePayload = ""
            credentialTitle = ""
            credentialUser = ""
            credentialSecret = ""
            credentialNotes = ""
            isFileVaultUnlocked = false
            fileVaultUnlockPassword = ""
            fileVaultUnlockError = false
            droppedFileName = ""
            droppedFileSizeLabel = ""
            droppedFileContent = ""
            showCreateCustomFileDialog = false
            customFileNameInput = ""
            customFileContentInput = ""
            viewingFileItem = null
            showDashboard = true
        }
    }

    val currentLanguage by viewModel.currentLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Primary title Section
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = Localization.get("vault_title", currentLanguage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = Localization.get("vault_subtitle", currentLanguage),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- LOCK CONDITION HANDLING SCREEN STATE ---
        if (!isPasswordSetup) {
            var isRestoreMode by remember { mutableStateOf(false) }
            var restoreMnemonicInput by remember { mutableStateOf("") }

            // Setup master password
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = "Security Keys", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Localization.get("setup_master", currentLanguage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = Localization.get("setup_master_desc", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = masterPasswordInput,
                        onValueChange = { masterPasswordInput = it },
                        label = { Text(Localization.get("enter_password", currentLanguage), fontSize = 11.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_password_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRestoreMode = !isRestoreMode },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isRestoreMode,
                            onCheckedChange = { isRestoreMode = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentLanguage == "RU") "Восстановить из BIP-39 фразы" else "Restore from BIP-39 mnemonic phrase",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (isRestoreMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = restoreMnemonicInput,
                            onValueChange = { restoreMnemonicInput = it },
                            label = { Text(if (currentLanguage == "RU") "Введите 12 слов восстановления" else "Enter 12-word seed phrase", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (masterPasswordInput.isNotBlank()) {
                                val customMnemonic = if (isRestoreMode && restoreMnemonicInput.isNotBlank()) restoreMnemonicInput else null
                                viewModel.setupMasterPassword(masterPasswordInput, customMnemonic)
                                masterPasswordInput = ""
                                restoreMnemonicInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("setup_password_button")
                    ) {
                        Text(
                            text = if (isRestoreMode) {
                                if (currentLanguage == "RU") "Восстановить" else "Restore"
                            } else {
                                Localization.get("create_vault", currentLanguage)
                            },
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else if (!isUnlocked) {
            // Unlock standard view
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Localization.get("vault_locked_desc", currentLanguage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = Localization.get("vault_locked_desc", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = masterPasswordInput,
                        onValueChange = { masterPasswordInput = it },
                        label = { Text(Localization.get("enter_password", currentLanguage), fontSize = 11.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unlock_password_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showBackupRestoreDialog = true },
                            modifier = Modifier.testTag("restore_from_login")
                        ) {
                            Text(Localization.get("backup_config", currentLanguage), fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                if (masterPasswordInput.isNotBlank()) {
                                    val success = viewModel.unlockVault(masterPasswordInput)
                                    if (success) {
                                        masterPasswordInput = ""
                                    }
                                }
                            },
                            modifier = Modifier.testTag("unlock_password_button")
                        ) {
                            Text(Localization.get("unlock_vault", currentLanguage), fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            // Unlock Screen view
            Column(modifier = Modifier.weight(1f)) {
                // Header indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00FF66))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SECURED CRYPTO SESSION ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00FF66),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { showDashboard = !showDashboard },
                            modifier = Modifier.testTag("dashboard_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (showDashboard) Icons.Default.Analytics else Icons.Outlined.Analytics,
                                contentDescription = "Toggle Dashboard Metrics",
                                tint = if (showDashboard) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { showBackupRestoreDialog = true },
                            modifier = Modifier.testTag("backup_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "ZK Backup")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.lockVault() },
                            modifier = Modifier.testTag("lock_button")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Lock Session", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                AnimatedVisibility(visible = showDashboard) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .testTag("vault_dashboard_panel")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Title & Quick Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "VAULT METRICS & CRYPTO HEALTH",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1D13)),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.border(0.5.dp, Color(0xFF00FF66).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(0xFF00FF66))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "INTEGRITY SECURE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00FF66),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Stats Grid
                            val fileItems = decryptedItems.filter { it.type == "FILE" }
                            val credentialItems = decryptedItems.filter { it.type != "FILE" }

                            val totalFileBytes = fileItems.sumOf { item ->
                                val clean = item.username.trim()
                                try {
                                    if (clean.endsWith(" B", ignoreCase = true)) {
                                        clean.substringBefore(" B").trim().toLongOrNull() ?: 0L
                                    } else if (clean.endsWith(" KB", ignoreCase = true)) {
                                        val value = clean.substringBefore(" KB").trim().toDoubleOrNull() ?: 0.0
                                        (value * 1024).toLong()
                                    } else if (clean.endsWith(" MB", ignoreCase = true)) {
                                        val value = clean.substringBefore(" MB").trim().toDoubleOrNull() ?: 0.0
                                        (value * 1024 * 1024).toLong()
                                    } else {
                                        (item.decryptedValue.length * 3 / 4).toLong()
                                    }
                                } catch (e: Exception) {
                                    0L
                                }
                            }

                            val totalCredentialBytes = credentialItems.sumOf { item ->
                                (item.title.length + item.username.length + item.decryptedValue.length + item.notes.length).toLong()
                            }

                            val totalStorageBytes = totalFileBytes + totalCredentialBytes

                            fun formatSize(bytes: Long): String {
                                return when {
                                    bytes < 1024 -> "$bytes B"
                                    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024f)
                                    else -> String.format("%.1f MB", bytes / (1024f * 1024f))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Metric 1: Total Files
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderZip,
                                            contentDescription = null,
                                            tint = Color(0xFF00FF66),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${fileItems.size}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Total Files",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Metric 2: Total Storage Usage
                                Card(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatSize(totalStorageBytes),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Storage Usage",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Metric 3: Total Elements
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Dns,
                                            contentDescription = null,
                                            tint = Color(0xFFE2E8F0),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${decryptedItems.size}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Total Entries",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Encryption Breakdown Header
                            Text(
                                text = "CRYPTOGRAPHIC CIPHER DISTRIBUTION:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Segmented custom chart bar
                            val totalCount = decryptedItems.size
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E293B))
                            ) {
                                if (totalCount > 0) {
                                    val fileRatio = fileItems.size.toFloat() / totalCount
                                    val credentialRatio = credentialItems.size.toFloat() / totalCount

                                    if (fileRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(fileRatio)
                                                .background(Color(0xFF00FF66))
                                        )
                                    }
                                    if (credentialRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(credentialRatio)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF334155))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Custom Legend with detailed info
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF00FF66))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "AES-GCM-256 (EXIF Metadata Stripped)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${fileItems.size} items (${formatSize(totalFileBytes)})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "AES-GCM-256 (Standard Credentials)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${credentialItems.size} items (${formatSize(totalCredentialBytes)})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Sub tabs selector
                TabRow(
                    selectedTabIndex = selectedSubTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("vault_sub_tabs")
                ) {
                    Tab(
                        selected = selectedSubTab == 0,
                        onClick = { selectedSubTab = 0 },
                        modifier = Modifier.testTag("tab_credentials"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Credentials", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedSubTab == 1,
                        onClick = { selectedSubTab = 1 },
                        modifier = Modifier.testTag("tab_file_vault"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("File Vault", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedSubTab == 2,
                        onClick = { selectedSubTab = 2 },
                        modifier = Modifier.testTag("tab_cloud_backup"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cloud Sync", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                if (selectedSubTab == 0) {
                    // --- CREDENTIALS SUB-TAB ---
                    val credentialItems = decryptedItems.filter { it.type != "FILE" }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PERSISTED CREDENTIAL SCHEMAS:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Button(
                            onClick = { showAddCredentialDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_credential_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Register sensitive", fontSize = 12.sp)
                        }
                    }

                    // Credentials List
                    if (credentialItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Security,
                                    contentDescription = "Empty vault",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Decrypted vault tables empty.",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Encrypted local SQLite rows will match entries mapped above.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(credentialItems) { item ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (item.type == "ONION_KEY") Icons.Outlined.Router else Icons.Outlined.Key,
                                                    contentDescription = "Keys",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "SCHEME: ${item.type}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            IconButton(onClick = { viewModel.deleteVaultItem(item.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Shred", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        if (item.username.isNotBlank()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Identifier: ${item.username}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                IconButton(
                                                    onClick = { clipboardManager.setText(AnnotatedString(item.username)) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy identity", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF090D14))
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.decryptedValue,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                color = Color(0xFF00FF66),
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = { clipboardManager.setText(AnnotatedString(item.decryptedValue)) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy sensitive", tint = Color(0xFF00FF66), modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        if (item.notes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Associated Context notes: ${item.notes}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedSubTab == 1) {
                    // --- FILE VAULT SUB-TAB ---
                    val fileItems = decryptedItems.filter { it.type == "FILE" }
                    val rawItems = viewModel.rawVaultItems.collectAsState().value

                    if (!isFileVaultUnlocked) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .testTag("file_vault_lock_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "File Vault Locked",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "File Vault Locked",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Verifying your cryptographic credentials is required before displaying or uploading stored files.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = fileVaultUnlockPassword,
                                    onValueChange = { 
                                        fileVaultUnlockPassword = it
                                        fileVaultUnlockError = false
                                    },
                                    label = { Text("Confirm Master Passphrase") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("file_vault_lock_input"),
                                    singleLine = true,
                                    isError = fileVaultUnlockError
                                )
                                if (fileVaultUnlockError) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Invalid passcode. Decryption rejected.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        if (fileVaultUnlockPassword.isNotBlank()) {
                                            val success = viewModel.unlockVault(fileVaultUnlockPassword)
                                            if (success) {
                                                isFileVaultUnlocked = true
                                                fileVaultUnlockPassword = ""
                                                fileVaultUnlockError = false
                                            } else {
                                                fileVaultUnlockError = true
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("unlock_file_vault_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verify & Unlock Files")
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            // Header indicating file vault is unlocked, with a button to seal/lock it
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FILE VAULT CONTENT DECRYPTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00FF66),
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = {
                                        isFileVaultUnlocked = false
                                        fileVaultUnlockPassword = ""
                                        fileVaultUnlockError = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("lock_file_vault_button")
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Seal File Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 1. Interactive File Upload Drop Zone & Progress Indicator
                            Text(
                                text = "SECURE FILE INGESTION DROP ZONE:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            if (vaultFileEncrypting) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF0C1322),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(bottom = 12.dp)
                                        .testTag("encryption_progress_card")
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    progress = { vaultFileProgress },
                                                    modifier = Modifier.size(24.dp),
                                                    color = Color(0xFF00FF66),
                                                    strokeWidth = 3.dp,
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "SECURE ENCRYPTION ACTIVE",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF00FF66)
                                                )
                                            }
                                            Text(
                                                text = "${(vaultFileProgress * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                                color = Color(0xFF00FF66)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        LinearProgressIndicator(
                                            progress = { vaultFileProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = Color(0xFF00FF66),
                                            trackColor = Color(0xFF1E293B)
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "CURRENT STAGE: $vaultFileStatus",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "CRYPTOGRAPHIC PIPELINE LOGS:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF050A14))
                                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                                .padding(8.dp)
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                reverseLayout = false
                                            ) {
                                                items(vaultFileLogs) { log ->
                                                    Text(
                                                        text = log,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                                    .border(
                                        2.dp,
                                        if (droppedFileName.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp)
                                    .testTag("file_upload_drop_zone")
                            ) {
                                if (droppedFileName.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.UploadFile,
                                            contentDescription = "Upload Icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Interactive File Vault Drop Zone",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Select a simulated preset or click the button to ingest a real local file",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Presets Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                Triple("tax_2026.pdf", "45.2 KB", "CONFIDENTIAL INCOME TAX RETURN - DO NOT REVEAL HASH SUMMARY"),
                                                Triple("ssh_key.pem", "1.8 KB", "-----BEGIN RSA PRIVATE KEY-----\nMIIEpQIBAAKCAQEA08g4w1qXm5Z...\n-----END RSA PRIVATE KEY-----"),
                                                Triple("ledger_pass.txt", "0.4 KB", "ADMIN_PASSCODE=9f323a\nVAULT_DECRYPT_SALT=0x3F88aD9\nFAILSAFE_SEQUENCE=true")
                                            ).forEach { (name, size, content) ->
                                                Button(
                                                    onClick = {
                                                        droppedFileName = name
                                                        droppedFileSizeLabel = size
                                                        droppedFileContent = content
                                                    },
                                                    enabled = !vaultFileEncrypting,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("preset_$name")
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Outlined.InsertDriveFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Text(name, fontSize = 8.sp, maxLines = 1)
                                                        Text(size, fontSize = 7.sp, color = MaterialTheme.colorScheme.outline)
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { showCreateCustomFileDialog = true },
                                                enabled = !vaultFileEncrypting,
                                                modifier = Modifier.testTag("create_custom_file_button")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Compose Custom...", fontSize = 11.sp)
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            OutlinedButton(
                                                onClick = { filePickerLauncher.launch("*/*") },
                                                enabled = !vaultFileEncrypting,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("select_real_file_button")
                                            ) {
                                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Pick Local File", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDone,
                                                    contentDescription = "Dropped",
                                                    tint = Color(0xFF00FF66),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = droppedFileName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Unencrypted Ingest Block • $droppedFileSizeLabel",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    droppedFileName = ""
                                                    droppedFileSizeLabel = ""
                                                    droppedFileContent = ""
                                                },
                                                enabled = !vaultFileEncrypting,
                                                modifier = Modifier.size(28.dp).testTag("clear_dropped_file")
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Show File Content Preview in beautiful monospace block
                                        Text("PLAINTEXT PREVIEW (TEMPORARY RAM STATE):", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF090D14))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = if (droppedFileContent.length > 80) droppedFileContent.take(80) + "..." else droppedFileContent,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                color = Color(0xFFE2E8F0),
                                                fontSize = 9.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                if (droppedFileName.isNotBlank() && droppedFileContent.isNotBlank()) {
                                                    viewModel.encryptAndSaveVaultFile(
                                                        fileName = droppedFileName,
                                                        fileBytes = droppedFileContent.toByteArray(),
                                                        fileMimeType = "text/plain"
                                                    )
                                                    droppedFileName = ""
                                                    droppedFileSizeLabel = ""
                                                    droppedFileContent = ""
                                                }
                                            },
                                            enabled = !vaultFileEncrypting,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("encrypt_dropped_file_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.EnhancedEncryption, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("ENCRYPT & SHIELD FILE SECURELY")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 2. Encrypted Files List
                            Text(
                                text = "SECURELY PERSISTED ENCRYPTED FILES:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (fileItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Outlined.FolderZip,
                                            contentDescription = "Empty file list",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No encrypted files stored",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = "Use the drop zone above to ingest a file into local SQLite storage.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 24.dp)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(fileItems) { item ->
                                        val rawItem = rawItems.find { it.id == item.id }
                                        val cipherBlock = rawItem?.sensitiveValue ?: "AES-GCM-256 ENCRYPTED BLOCK"

                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                .testTag("file_item_${item.id}")
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.FolderZip,
                                                            contentDescription = "Encrypted file",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(
                                                                text = item.title,
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "Size: ${item.username}",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    Row {
                                                        IconButton(
                                                            onClick = { viewingFileItem = item },
                                                            modifier = Modifier.testTag("view_file_button_${item.id}")
                                                        ) {
                                                            Icon(Icons.Default.Visibility, contentDescription = "Decrypt & View", tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                        IconButton(
                                                            onClick = { viewModel.deleteVaultItem(item.id) },
                                                            modifier = Modifier.testTag("delete_file_button_${item.id}")
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Shred", tint = MaterialTheme.colorScheme.error)
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Display ACTUAL Base64 cipher block stored in SQLite database in a visual terminal box
                                                Text("DATABASE CIPHERTEXT (ZERO-KNOWLEDGE STORAGE BLOCK):", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF090D14))
                                                        .padding(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (cipherBlock.length > 100) cipherBlock.take(100) + "..." else cipherBlock,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                        color = Color(0xFF00FF66),
                                                        fontSize = 9.sp
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
                    // --- ZERO-KNOWLEDGE CLOUD BACKUP & SYNC CONFIGURATION PANEL ---
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("zk_cloud_backup_panel"),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // Sub-header explaining ZK backups
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "ZERO-KNOWLEDGE BACKUP ARCHITECTURE",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "All database tables are compiled, stripped of metadata (including image EXIF headers), and encrypted using a client-side PBKDF2 AES-GCM-256 derived key before transmission. Proxy routing servers never see decrypted keys, files, credentials, or client telemetry.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Sync state and manual controls card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "CLOUD BACKUP SYNCHRONIZATION",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Continuous automated cloud mirror",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Switch(
                                            checked = zkCloudBackupEnabled,
                                            onCheckedChange = { viewModel.toggleZkCloudBackup(it) },
                                            modifier = Modifier.testTag("zk_cloud_sync_toggle")
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Dynamic Status Section
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Current State:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )

                                        val statusColor = when (zkCloudBackupStatus) {
                                            "Synchronized" -> Color(0xFF00FF66)
                                            "Syncing..." -> MaterialTheme.colorScheme.primary
                                            "Isolated" -> MaterialTheme.colorScheme.outline
                                            else -> MaterialTheme.colorScheme.error
                                        }

                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.12f)),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        ) {
                                            Text(
                                                text = zkCloudBackupStatus.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Last Synced Timestamp
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Last Sync Check:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (zkLastCloudSyncTimestamp > 0L) {
                                                val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm:ss", java.util.Locale.US)
                                                sdf.format(java.util.Date(zkLastCloudSyncTimestamp))
                                            } else {
                                                "Never"
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Manual Sync Trigger
                                    Button(
                                        onClick = { viewModel.triggerManualZkCloudSync() },
                                        enabled = zkCloudBackupEnabled && zkCloudBackupStatus != "Syncing...",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("zk_manual_sync_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Synchronize Now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            // Target routing server configuration card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "ROUTING & CRYPTO SETTINGS",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Node Input URL
                                    var nodeUrlInput by remember { mutableStateOf(zkCloudBackupUrl) }
                                    OutlinedTextField(
                                        value = nodeUrlInput,
                                        onValueChange = { 
                                            nodeUrlInput = it
                                            viewModel.setZkCloudBackupUrl(it)
                                        },
                                        label = { Text("Backup Proxy Node URL") },
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("zk_backup_node_input"),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Frequency Selector
                                    Text(
                                        text = "Synchronization Frequency: $zkCloudSyncFrequencyMinutes minutes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val frequencies = listOf(5, 15, 30, 60)
                                        frequencies.forEach { freq ->
                                            val isSelected = zkCloudSyncFrequencyMinutes == freq
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable { viewModel.setZkCloudSyncFrequency(freq) }
                                                    .testTag("freq_button_$freq")
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${freq}m",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Live Logs View
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF090D14)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
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
                                                    .size(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color(0xFF00FF66))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "ZERO-KNOWLEDGE SNAPSHOT LOGS",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = Color(0xFF00FF66),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        TextButton(
                                            onClick = { viewModel.clearCloudSyncLogs() },
                                            modifier = Modifier.testTag("clear_backup_logs_button")
                                        ) {
                                            Text("Clear Logs", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF030712))
                                            .border(0.5.dp, Color(0xFF1F2937), RoundedCornerShape(6.dp))
                                            .padding(10.dp)
                                    ) {
                                        if (cloudSyncLogs.isEmpty()) {
                                            Text(
                                                text = "No snapshots recorded. Enabling backup initiates verification.",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                color = Color(0xFF6B7280)
                                            )
                                        } else {
                                            LazyColumn(
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                items(cloudSyncLogs) { log ->
                                                    Text(
                                                        text = log,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                        color = if (log.contains("succeeded") || log.contains("COMPLETE") || log.contains("SUCCESS") || log.contains("ENABLED")) Color(0xFF00FF66) else Color(0xFFE2E8F0),
                                                        fontSize = 10.sp
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

                if (bip39Mnemonic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VpnKey, contentDescription = "BIP39", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (currentLanguage == "RU") "BIP-39 ФРАЗА ВОССТАНОВЛЕНИЯ" else "BIP-39 RECOVERY SEED PHRASE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                IconButton(
                                    modifier = Modifier.size(24.dp),
                                    onClick = { clipboardManager.setText(AnnotatedString(bip39Mnemonic)) }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Seed", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // 3x4 Grid representation of the 12 words
                            val words = bip39Mnemonic.split(" ")
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (row in 0..3) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        for (col in 0..2) {
                                            val wordIndex = row * 3 + col
                                            if (wordIndex < words.size) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    Row {
                                                        Text(
                                                            text = "${wordIndex + 1}. ",
                                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = words[wordIndex],
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            fontSize = 11.sp
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
                }

                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SettingsInputAntenna, contentDescription = "Telemetry", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLanguage == "RU") "ТЕЛЕМЕТРИЯ АКТИВНОЙ НОДЫ" else "ACTIVE NODE HARDWARE TELEMETRY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Charging Indicator
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isCharging) Color(0x2200FF66) else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                                        contentDescription = "Battery",
                                        tint = if (isCharging) Color(0xFF00FF66) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (currentLanguage == "RU") "Зарядка" else "Power",
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = if (isCharging) (if (currentLanguage == "RU") "ПОДКЛЮЧЕНА" else "CHARGING") else (if (currentLanguage == "RU") "ОТКЛЮЧЕНА" else "DISCONNECTED"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCharging) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Device Interaction Status
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isScreenOff) Color(0x223B82F6) else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = if (isScreenOff) Icons.Default.NotificationsPaused else Icons.Default.TouchApp,
                                        contentDescription = "Inactivity",
                                        tint = if (isScreenOff) Color(0xFF3B82F6) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (currentLanguage == "RU") "Активность" else "Activity",
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = if (isScreenOff) (if (currentLanguage == "RU") "СОН/ОЖИДАНИЕ" else "IDLE/SCREEN OFF") else (if (currentLanguage == "RU") "ОТКРЫТО" else "ACTIVE/SCREEN ON"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isScreenOff) Color(0xFF3B82F6) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Active Node Sync Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLanguage == "RU") "Режим активной ноды:" else "Active node status:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isActiveNodeMode) Color(0x2200FF66) else Color(0x22EF4444))
                                    .padding(vertical = 2.dp, horizontal = 6.dp)
                            ) {
                                Text(
                                    text = if (isActiveNodeMode) (if (currentLanguage == "RU") "ПОДХОДИТ ДЛЯ СИНХРОНИЗАЦИИ" else "SUITABLE FOR SYNC") else (if (currentLanguage == "RU") "ОЖИДАНИЕ УСЛОВИЙ" else "AWAITING TRIGGERS"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActiveNodeMode) Color(0xFF00FF66) else Color(0xFFEF4444)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Active Ping status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLanguage == "RU") "Задержка до DHT бутстрап узлов:" else "DHT Bootstrap latency:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${seederPingMs} ms",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (seederPingMs < 60) Color(0xFF00FF66) else Color(0xFFEAB308)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Container space status
                        val maxBytes = viewModel.cryptoContainerSizeGb.value * 1024L * 1024L * 1024L
                        val cacheSizeMb = cacheSizeBytes / (1024L * 1024L)
                        val cacheSizeGb = cacheSizeBytes / (1024L * 1024L * 1024L)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLanguage == "RU") "Размер контейнера (сим. заполнения):" else "Container usage (simulated):",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${cacheSizeGb} GB / ${viewModel.cryptoContainerSizeGb.value} GB (${cacheSizeMb} MB)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Last eviction log terminal
                        Text(
                            text = if (currentLanguage == "RU") "ПОСЛЕДНЯЯ ОЧИСТКА ХРАНИЛИЩА (LRU):" else "LAST STORAGE PURGE (LRU CACHE EVICTION):",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF030712))
                                .border(0.5.dp, Color(0xFF1F2937), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = lastLruEvictionLog,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (lastLruEvictionLog.contains("PURGED")) Color(0xFFEAB308) else Color(0xFF6B7280),
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Purge configuration
                Button(
                    onClick = { viewModel.deleteRecoveryAllData() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("panic_purge_button")
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Panic Purge")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("FACTORY PANIC PURGE ENCRYPTED MEMORY")
                }
            }
        }
    }

    // --- DIALOGS HANDLING ---
    if (showAddCredentialDialog) {
        AlertDialog(
            onDismissRequest = { showAddCredentialDialog = false },
            title = { Text("Seal Local Credential Record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = credentialTitle,
                        onValueChange = { credentialTitle = it },
                        label = { Text("Platform / Reference name") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cred_title")
                    )
                    OutlinedTextField(
                        value = credentialUser,
                        onValueChange = { credentialUser = it },
                        label = { Text("Identifier ID / User mapping") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cred_identity")
                    )
                    OutlinedTextField(
                        value = credentialSecret,
                        onValueChange = { credentialSecret = it },
                        label = { Text("Encrypted Secret (Password / RSA Key)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cred_secret")
                    )
                    OutlinedTextField(
                        value = credentialNotes,
                        onValueChange = { credentialNotes = it },
                        label = { Text("Discreet context notes") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cred_notes")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (credentialTitle.isNotBlank() && credentialSecret.isNotBlank()) {
                            viewModel.saveVaultItem(
                                type = "CREDENTIAL",
                                title = credentialTitle,
                                username = credentialUser,
                                secret = credentialSecret,
                                notes = credentialNotes
                            )
                            credentialTitle = ""
                            credentialUser = ""
                            credentialSecret = ""
                            credentialNotes = ""
                            showAddCredentialDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_seal_cred_confirm_button")
                ) {
                    Text("Encrypt Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCredentialDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBackupRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showBackupRestoreDialog = false },
            title = { Text("Zero-Knowledge Cloud Backup Proxy") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Encrypted snapshots utilize password PBDKF key-derivations. Server proxies never map metadata. Zero-Knowledge backup is absolute.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it },
                        label = { Text("Backup Passphrase (Mandatory)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("backup_passphrase_input")
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // ACTIONS LAYOUT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (backupPassphrase.isNotBlank()) {
                                    val result = viewModel.generateZeroKnowledgeBackupPayload(backupPassphrase)
                                    if (result != null) {
                                        outputBackupPayload = result
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("trigger_create_backup")
                        ) {
                            Text("Export", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (backupPassphrase.isNotBlank() && inputBackupRestorePayload.isNotBlank()) {
                                    val ok = viewModel.restoreZeroKnowledgeBackup(inputBackupRestorePayload, backupPassphrase)
                                    if (ok) {
                                        showBackupRestoreDialog = false
                                        backupPassphrase = ""
                                        inputBackupRestorePayload = ""
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("trigger_import_backup")
                        ) {
                            Text("Import", fontSize = 12.sp)
                        }
                    }

                    if (outputBackupPayload.isNotBlank()) {
                        Column {
                            Text("EXPORTED AES CIPHER STRING (Z-K BLOCK):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF090D14))
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = outputBackupPayload.take(40) + "...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color(0xFF00FF66),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { clipboardManager.setText(AnnotatedString(outputBackupPayload)) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy block", tint = Color(0xFF00FF66), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputBackupRestorePayload,
                        onValueChange = { inputBackupRestorePayload = it },
                        label = { Text("Paste cipher text payload to restore") },
                        modifier = Modifier.fillMaxWidth().testTag("import_backup_payload_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showBackupRestoreDialog = false },
                    modifier = Modifier.testTag("close_backup_dialog")
                ) {
                    Text("Dismiss Panel")
                }
            }
        )
    }

    if (showCreateCustomFileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCustomFileDialog = false },
            title = { Text("Compose Simulated File") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customFileNameInput,
                        onValueChange = { customFileNameInput = it },
                        label = { Text("File Name (e.g. secret.txt)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_custom_file_name")
                    )
                    OutlinedTextField(
                        value = customFileContentInput,
                        onValueChange = { customFileContentInput = it },
                        label = { Text("File Content Plaintext") },
                        modifier = Modifier.fillMaxWidth().testTag("input_custom_file_content")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customFileNameInput.isNotBlank() && customFileContentInput.isNotBlank()) {
                            val sizeInBytes = customFileContentInput.toByteArray().size
                            val sizeLabel = if (sizeInBytes < 1024) "${sizeInBytes} B" else String.format("%.1f KB", sizeInBytes / 1024.0)

                            droppedFileName = customFileNameInput
                            droppedFileSizeLabel = sizeLabel
                            droppedFileContent = customFileContentInput

                            customFileNameInput = ""
                            customFileContentInput = ""
                            showCreateCustomFileDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_custom_file_button")
                ) {
                    Text("Simulate Drop")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCustomFileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (viewingFileItem != null) {
        val fileItem = viewingFileItem!!
        AlertDialog(
            onDismissRequest = { viewingFileItem = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Decrypted File: ${fileItem.title}")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Size Label: ${fileItem.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text("Status: Decrypted dynamically with GCM master key session", fontSize = 11.sp, color = Color(0xFF00FF66))
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text("FILE BODY CONTENT:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF090D14))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = fileItem.decryptedValue,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFF00FF66),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(fileItem.decryptedValue))
                    },
                    modifier = Modifier.testTag("copy_file_content_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Content")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewingFileItem = null },
                    modifier = Modifier.testTag("close_file_viewer_button")
                ) {
                    Text("Close")
                }
            }
        )
    }
}

// Utility function to extract real filename from Content Uri
fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
    var name: String? = null
    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name ?: uri.path?.substringAfterLast('/')
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.components.InAppGboardKeyboard
import com.example.ui.theme.LocalParanoidPalette
import com.example.ui.viewmodel.MainViewModel

// [REAL] Этап 9/10: Universal Wallet MVP — Детерминированный identity/proof кошелёк из единого Master Seed.
// [SECURITY-CRITICAL] Приватный ключ не хранится на сервере и не покидает оперативную память;
// адрес и подпись детерминированно порождаются через HKDF-SHA512 ("ParanoidX/Wallet/EVM/v1") + Keccak-256.
// [NEEDS-WORK] Классическая secp256k1 ECDSA подпись для mainnet-транзакций пока не подключена (используется HMAC-SHA512 Proof MVP).
// [AIR-GAP] Ввод challenge-сообщений выполняется строго через встроенную клавиатуру InAppGboardKeyboard.

@Composable
fun WalletScreen(viewModel: MainViewModel) {
    val palette = LocalParanoidPalette.current
    val evmAddress by viewModel.walletEvmAddress.collectAsState()
    val evmFingerprint by viewModel.walletEvmFingerprint.collectAsState()
    val lastSignature by viewModel.walletLastSignature.collectAsState()
    val signatureVerified by viewModel.walletSignatureVerified.collectAsState()
    val activeOnion by viewModel.activeOnionAddress.collectAsState()

    val clipboardManager = LocalClipboardManager.current

    var challengeInput by remember { mutableStateOf("ParanoidX Node Proof #1") }
    var showInAppKeyboard by remember { mutableStateOf(false) }
    var copiedAddress by remember { mutableStateOf(false) }
    var copiedSignature by remember { mutableStateOf(false) }

    val presets = listOf(
        "ParanoidX Node Proof #1",
        "Fleet Attestation Node #0",
        "Zero-Trust Auth Challenge"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. UNIVERSAL EVM PASSPORT HERO CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
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
                                        .background(palette.accent.copy(alpha = 0.14f))
                                        .border(1.dp, palette.accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Universal Wallet",
                                        tint = palette.accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "UNIVERSAL WALLET (PROOF MVP)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = palette.textPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Non-Custodial • ParanoidX Proof Key",
                                        fontSize = 11.sp,
                                        color = palette.accent
                                    )
                                }
                            }

                            Surface(
                                color = palette.surfaceElevated,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "HMAC+EIP55",
                                    color = palette.secondaryAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "DETERMINISTIC PUBLIC ADDRESS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.headerBg)
                                .border(
                                    1.dp,
                                    if (evmAddress.isNotBlank()) palette.border else palette.warning.copy(alpha = 0.55f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = evmAddress.ifBlank { "Node locked — unlock with Master PIN to derive EVM Proof address" },
                                color = if (evmAddress.isNotBlank()) palette.accent else palette.warning,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("wallet_evm_address_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DOMAIN: ParanoidX/Wallet/EVM/v1",
                                    fontSize = 10.sp,
                                    color = palette.textSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "KEY FP: ${evmFingerprint.ifBlank { "----" }}",
                                    fontSize = 10.sp,
                                    color = palette.textMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Button(
                                onClick = {
                                    if (evmAddress.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(evmAddress))
                                        copiedAddress = true
                                        copiedSignature = false
                                    }
                                },
                                enabled = evmAddress.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_copy_wallet_address")
                            ) {
                                Icon(
                                    imageVector = if (copiedAddress) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy EVM address",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (copiedAddress) "Скопировано" else "Copy 0x",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Модель ключей: HKDF-SHA512 + EIP-191 HMAC Domain Proof (для идентификации ноды и флота, не является spend-ключом secp256k1 mainnet).",
                            fontSize = 10.sp,
                            color = palette.textSecondary
                        )
                    }
                }
            }

            // 2. CHALLENGE / MESSAGE SIGNING CARD (IN-APP KEYBOARD ONLY)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Sign Challenge",
                                    tint = palette.secondaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SIGN CHALLENGE / PROOF",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = palette.textPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "Air-Gap IME",
                                fontSize = 10.sp,
                                color = palette.accent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Подпись произвольного сообщения или challenge через EIP-191 Keccak-256 + HKDF-SHA512 без обращения к внешним серверам.",
                            fontSize = 11.sp,
                            color = palette.textSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preset Challenge Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEachIndexed { index, preset ->
                                FilterChip(
                                    selected = challengeInput == preset,
                                    onClick = { challengeInput = preset },
                                    label = { Text(preset, fontSize = 10.sp, maxLines = 1) },
                                    modifier = Modifier.testTag("wallet_challenge_preset_$index")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Read-only input wired strictly to InAppGboardKeyboard
                        OutlinedTextField(
                            value = challengeInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Challenge / Сообщение для подписи", fontSize = 11.sp) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showInAppKeyboard = !showInAppKeyboard },
                                    modifier = Modifier.testTag("btn_toggle_wallet_keyboard")
                                ) {
                                    Icon(
                                        imageVector = if (showInAppKeyboard) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                                        contentDescription = "Toggle In-App Keyboard",
                                        tint = palette.accent
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (it.isFocused) showInAppKeyboard = true }
                                .clickable { showInAppKeyboard = true }
                                .testTag("wallet_challenge_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                showInAppKeyboard = false
                                viewModel.signWalletChallenge(challengeInput)
                                copiedSignature = false
                            },
                            enabled = challengeInput.isNotBlank() && evmAddress.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.secondaryAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_sign_wallet_challenge")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (evmAddress.isNotBlank()) "Подписать Challenge (Offline)" else "Кошелёк заблокирован (Нужен PIN)",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Signed Proof Output
                        lastSignature?.let { sig ->
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = palette.border)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "CRYPTOGRAPHIC PROOF OUTPUT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.headerBg)
                                    .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "ALG: ${sig.algorithm}",
                                    fontSize = 9.sp,
                                    color = palette.textSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "EIP-191 DIGEST: ${sig.digestHex}",
                                    fontSize = 10.sp,
                                    color = palette.textPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "SIGNATURE: ${sig.signatureHex}",
                                    fontSize = 10.sp,
                                    color = palette.accent,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("wallet_signature_hex_text")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(sig.signatureHex))
                                        copiedSignature = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_copy_wallet_signature"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent)
                                ) {
                                    Icon(
                                        imageVector = if (copiedSignature) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "Copy Signature",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (copiedSignature) "Copied" else "Copy Sig", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { viewModel.verifyLastWalletChallenge() },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_verify_wallet_signature")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verify Signature",
                                        tint = palette.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Проверить", color = palette.textPrimary, fontSize = 12.sp)
                                }
                            }

                            signatureVerified?.let { isValid ->
                                Spacer(modifier = Modifier.height(8.dp))
                                val badgeColor = if (isValid) palette.accent else palette.danger
                                Surface(
                                    color = badgeColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                                        .testTag("wallet_verification_badge")
                                ) {
                                    Text(
                                        text = if (isValid) "SIGNATURE VALID • Seed Ownership Confirmed" else "SIGNATURE INVALID",
                                        color = badgeColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. LINKED ROOTKEYHUB DOMAINS CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ROOTKEYHUB • ACTIVE HKDF-SHA512 BRANCHES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        val domains = listOf(
                            "ParanoidX/Wallet/EVM/v1" to "Universal EVM Passport & Challenge Proof",
                            "ParanoidX/Vault/Key/v1" to "SQLCipher & AES-256-GCM Vault",
                            "ParanoidX/Tor/HiddenService/v3" to "Node Identity Label ($activeOnion)",
                            "ParanoidX/SimpleX/Identity/v1" to "SimpleX E2EE Double Ratchet Identity",
                            "ParanoidX/XFTP/ChunkKey/v1" to "XFTP 64KB Chunk Encryption",
                            "ParanoidX/P2P/SwarmKey/v1" to "P2P Container & Block Storage",
                            "ParanoidX/Fleet/Node/{index}" to "Fleet Command Sub-Nodes"
                        )
                        domains.forEach { (path, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.headerBg)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = path,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.secondaryAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 10.sp,
                                        color = palette.textSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Autonomous In-App Keyboard (Zero System IME)
        if (showInAppKeyboard) {
            InAppGboardKeyboard(
                text = challengeInput,
                onTextChange = { challengeInput = it },
                onSendOrDone = {
                    showInAppKeyboard = false
                    viewModel.signWalletChallenge(challengeInput)
                    copiedSignature = false
                },
                sendButtonLabel = "SIGN",
                onCloseKeyboard = { showInAppKeyboard = false },
                modifier = Modifier.testTag("wallet_inapp_keyboard")
            )
        }
    }
}

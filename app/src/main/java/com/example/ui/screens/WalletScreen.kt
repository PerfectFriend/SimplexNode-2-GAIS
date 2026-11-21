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

// [REAL] Этап 11 (P1): Dual-Mode Universal Wallet — строгое разделение режимов PROOF и SPEND.
// - PROOF ("ParanoidX/Wallet/EVM/v1"): детерминированная HMAC-SHA512 идентификация и challenge proof (не расходует средства mainnet).
// - SPEND ("ParanoidX/Wallet/Spend/secp256k1/v1"): настоящий криптографический ключ Ethereum secp256k1 (RFC 6979 + EIP-2 low-S + r,s,v).
// [AIR-GAP] Ввод challenge-сообщений выполняется строго через встроенную клавиатуру InAppGboardKeyboard.

private enum class WalletOperatingMode {
    PROOF,
    SPEND
}

@Composable
fun WalletScreen(viewModel: MainViewModel) {
    val palette = LocalParanoidPalette.current

    // PROOF mode state
    val proofAddress by viewModel.walletEvmAddress.collectAsState()
    val proofFingerprint by viewModel.walletEvmFingerprint.collectAsState()
    val proofLastSig by viewModel.walletLastSignature.collectAsState()
    val proofVerified by viewModel.walletSignatureVerified.collectAsState()

    // SPEND (secp256k1) mode state
    val spendAddress by viewModel.walletSpendAddress.collectAsState()
    val spendPubKeyHex by viewModel.walletSpendPublicKeyHex.collectAsState()
    val spendFingerprint by viewModel.walletSpendFingerprint.collectAsState()
    val spendLastSig by viewModel.walletSpendLastSignature.collectAsState()
    val spendVerified by viewModel.walletSpendSignatureVerified.collectAsState()

    val activeOnion by viewModel.activeOnionAddress.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var walletMode by remember { mutableStateOf(WalletOperatingMode.PROOF) }
    var challengeInput by remember { mutableStateOf("ParanoidX Node Proof #1") }
    var showInAppKeyboard by remember { mutableStateOf(false) }
    var copiedAddress by remember { mutableStateOf(false) }
    var copiedSignature by remember { mutableStateOf(false) }

    val isSpend = walletMode == WalletOperatingMode.SPEND
    val activeAddress = if (isSpend) spendAddress else proofAddress
    val activeFingerprint = if (isSpend) spendFingerprint else proofFingerprint
    val activeAccent = if (isSpend) palette.secondaryAccent else palette.accent

    val presets = listOf(
        "ParanoidX Node Proof #1",
        "Ethereum Spend Ownership Check",
        "Fleet Attestation Node #0"
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
            // 0. DUAL-MODE STRICT SWITCHER (PROOF vs SPEND)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, activeAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "WALLET MODE (STRICT KEY SEPARATION)",
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
                            WalletModeChip(
                                title = "PROOF (HMAC)",
                                subtitle = "Identity Proof Only",
                                selected = walletMode == WalletOperatingMode.PROOF,
                                accent = palette.accent,
                                onClick = {
                                    walletMode = WalletOperatingMode.PROOF
                                    copiedAddress = false
                                    copiedSignature = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wallet_mode_proof")
                            )
                            WalletModeChip(
                                title = "SPEND (secp256k1)",
                                subtitle = "True ECDSA r,s,v",
                                selected = walletMode == WalletOperatingMode.SPEND,
                                accent = palette.secondaryAccent,
                                onClick = {
                                    walletMode = WalletOperatingMode.SPEND
                                    copiedAddress = false
                                    copiedSignature = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wallet_mode_spend")
                            )
                        }
                    }
                }
            }

            // 1. UNIVERSAL EVM PASSPORT / SPEND ADDRESS CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, activeAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
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
                                        .background(activeAccent.copy(alpha = 0.14f))
                                        .border(1.dp, activeAccent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSpend) Icons.Default.Key else Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Universal Wallet",
                                        tint = activeAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isSpend) "SPEND WALLET (secp256k1)" else "PROOF PASSPORT (HMAC)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = palette.textPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (isSpend) "Ethereum Mainnet Compatible ECDSA Key" else "Non-Spendable Node Attestation Identity",
                                        fontSize = 11.sp,
                                        color = activeAccent
                                    )
                                }
                            }

                            Surface(
                                color = palette.surfaceElevated,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isSpend) "secp256k1" else "HMAC+EIP55",
                                    color = activeAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isSpend) "ETHEREUM SPEND ADDRESS (FROM PUBKEY)" else "PROOF IDENTITY ADDRESS (NON-SPEND)",
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
                                    if (activeAddress.isNotBlank()) palette.border else palette.warning.copy(alpha = 0.55f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = activeAddress.ifBlank { "Node locked — unlock with Master PIN to derive address" },
                                color = if (activeAddress.isNotBlank()) activeAccent else palette.warning,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("wallet_evm_address_text")
                            )
                        }

                        if (isSpend && spendPubKeyHex.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "PUBKEY (04||X||Y): ${spendPubKeyHex.take(26)}...${spendPubKeyHex.takeLast(12)}",
                                fontSize = 9.sp,
                                color = palette.textMuted,
                                fontFamily = FontFamily.Monospace
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
                                    text = if (isSpend) "DOMAIN: ParanoidX/Wallet/Spend/secp256k1/v1" else "DOMAIN: ParanoidX/Wallet/EVM/v1",
                                    fontSize = 9.sp,
                                    color = palette.textSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "KEY FP: ${activeFingerprint.ifBlank { "----" }}",
                                    fontSize = 10.sp,
                                    color = palette.textMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Button(
                                onClick = {
                                    if (activeAddress.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(activeAddress))
                                        copiedAddress = true
                                        copiedSignature = false
                                    }
                                },
                                enabled = activeAddress.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = activeAccent),
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
                            text = if (isSpend) {
                                "Режим SPEND: настоящий ECDSA secp256k1 ключ (RFC 6979 детерминированный nonce + EIP-2 low-S). Адрес 0x вычислен из Keccak-256 несжатого публичного ключа (отделён от PROOF)."
                            } else {
                                "Режим PROOF: HKDF-SHA512 + EIP-191 HMAC для аутентификации ноды и флота (намеренно НЕ расходует on-chain средства)."
                            },
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
                                    tint = activeAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSpend) "SIGN EIP-191 (secp256k1 ECDSA)" else "SIGN CHALLENGE (HMAC PROOF)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = palette.textPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "Air-Gap IME",
                                fontSize = 10.sp,
                                color = activeAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isSpend) {
                                "Создаёт стандартную 65-байтную подпись Ethereum (r || s || v) через secp256k1 без доступа к сети."
                            } else {
                                "Создаёт детерминированное HMAC-SHA512 доказательство владения Master Seed."
                            },
                            fontSize = 11.sp,
                            color = palette.textSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

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
                                        tint = activeAccent
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
                                if (isSpend) viewModel.signWalletSpendChallenge(challengeInput)
                                else viewModel.signWalletChallenge(challengeInput)
                                copiedSignature = false
                            },
                            enabled = challengeInput.isNotBlank() && activeAddress.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = activeAccent),
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
                                text = when {
                                    activeAddress.isBlank() -> "Кошелёк заблокирован (Нужен PIN)"
                                    isSpend -> "Подписать ECDSA secp256k1 (r,s,v)"
                                    else -> "Подписать Proof Challenge (Offline)"
                                },
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Output box strictly matched to active mode!
                        val activeAlg = if (isSpend) spendLastSig?.algorithm else proofLastSig?.algorithm
                        val activeDigest = if (isSpend) spendLastSig?.digestHex else proofLastSig?.digestHex
                        val activeSigHex = if (isSpend) spendLastSig?.signatureHex else proofLastSig?.signatureHex
                        val activeVerified = if (isSpend) spendVerified else proofVerified

                        if (activeSigHex != null && activeDigest != null && activeAlg != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = palette.border)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (isSpend) "ECDSA secp256k1 SIGNATURE (r,s,v)" else "CRYPTOGRAPHIC PROOF OUTPUT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccent,
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
                                    text = "ALG: $activeAlg",
                                    fontSize = 9.sp,
                                    color = palette.textSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "EIP-191 DIGEST: $activeDigest",
                                    fontSize = 10.sp,
                                    color = palette.textPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isSpend && spendLastSig != null) {
                                    Text(
                                        text = "r: ${spendLastSig!!.rHex.take(18)}... | v=${spendLastSig!!.v}",
                                        fontSize = 9.sp,
                                        color = palette.textSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "SIGNATURE: $activeSigHex",
                                    fontSize = 10.sp,
                                    color = activeAccent,
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
                                        clipboardManager.setText(AnnotatedString(activeSigHex))
                                        copiedSignature = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_copy_wallet_signature"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = activeAccent)
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
                                    onClick = {
                                        if (isSpend) viewModel.verifyLastWalletSpendChallenge()
                                        else viewModel.verifyLastWalletChallenge()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_verify_wallet_signature")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verify Signature",
                                        tint = activeAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Проверить", color = palette.textPrimary, fontSize = 12.sp)
                                }
                            }

                            activeVerified?.let { isValid ->
                                Spacer(modifier = Modifier.height(8.dp))
                                val badgeColor = if (isValid) activeAccent else palette.danger
                                Surface(
                                    color = badgeColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                                        .testTag("wallet_verification_badge")
                                ) {
                                    Text(
                                        text = if (isValid) {
                                            if (isSpend) "ECDSA secp256k1 VALID • PubKey Match Confirmed" else "HMAC PROOF VALID • Seed Ownership Confirmed"
                                        } else {
                                            "SIGNATURE INVALID"
                                        },
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
                            "ParanoidX/Wallet/Spend/secp256k1/v1" to "True Ethereum Spend Key (ECDSA + RFC 6979)",
                            "ParanoidX/Wallet/EVM/v1" to "Proof Identity Address & Challenge Proof (Non-Spend)",
                            "ParanoidX/Vault/Key/v1" to "SQLCipher & AES-256-GCM Vault",
                            "ParanoidX/Tor/HiddenService/v3" to "App Identity Label ($activeOnion)",
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
                    if (isSpend) viewModel.signWalletSpendChallenge(challengeInput)
                    else viewModel.signWalletChallenge(challengeInput)
                    copiedSignature = false
                },
                sendButtonLabel = "SIGN",
                onCloseKeyboard = { showInAppKeyboard = false },
                modifier = Modifier.testTag("wallet_inapp_keyboard")
            )
        }
    }
}

@Composable
private fun WalletModeChip(
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalParanoidPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.14f) else palette.headerBg)
            .border(1.5.dp, if (selected) accent else palette.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (selected) accent else palette.textPrimary
        )
        Text(
            text = subtitle,
            fontSize = 9.sp,
            color = if (selected) palette.textPrimary else palette.textMuted
        )
    }
}

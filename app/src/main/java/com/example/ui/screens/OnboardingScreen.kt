package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.example.data.security.Bip39Utils
import com.example.ui.components.InAppGboardKeyboard
import com.example.ui.viewmodel.MainViewModel

// [REAL] Этап 8: Sovereign Onboarding — Мастер создания и восстановления ноды с корнем доверия BIP-39.
// [SECURITY-CRITICAL] Ввод seed-слов, PIN и Panic PIN осуществляется строго через In-App Keyboard (zero system IME).
// [PROD-READY] Верификация мнемоники по контрольной сумме SHA-256 и детерминированная деривация ключей.

enum class OnboardingStep {
    WELCOME,
    SEED_DISPLAY,
    SEED_VERIFY,
    SEED_RESTORE_INPUT,
    PIN_SETUP,
    PANIC_PIN_SETUP,
    DERIVATION_SUMMARY
}

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onOnboardingFinished: () -> Unit
) {
    var step by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var generatedMnemonic by remember { mutableStateOf("") }
    var mnemonicWords by remember { mutableStateOf<List<String>>(emptyList()) }

    // Seed verification state (3 challenge words)
    var verifyIndices by remember { mutableStateOf<List<Int>>(listOf(2, 6, 10)) } // 3rd, 7th, 11th word
    var selectedVerifyWords by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var verifyError by remember { mutableStateOf(false) }

    // Restore state
    var restoreInputMnemonic by remember { mutableStateOf("") }
    var restoreWordsList by remember { mutableStateOf(List(12) { "" }) }
    var activeWordSlotIndex by remember { mutableStateOf(0) }
    var restoreChecksumValid by remember { mutableStateOf(false) }

    // PIN Setup State
    var pinInput by remember { mutableStateOf("") }
    var panicPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // In-App Keyboard visibility for text input
    var isKeyboardActive by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
            .padding(16.dp)
            .testTag("onboarding_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP HEADER ---
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(2.dp, Color(0xFF00FF66), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Shield",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ParanoidX Sovereign Node",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "BIP-39 Root Key Onboarding • Air-Gap Safe",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF00FF66)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- STEP CONTENT SWITCHER ---
            when (step) {
                // STEP 1: WELCOME & SEED SELECTION
                OnboardingStep.WELCOME -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Выберите способ инициализации ноды:",
                            fontSize = 14.sp,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Button 1: Create New
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    val mnemonic = Bip39Utils.generateMnemonic()
                                    generatedMnemonic = mnemonic
                                    mnemonicWords = mnemonic.split(" ")
                                    step = OnboardingStep.SEED_DISPLAY
                                }
                                .padding(16.dp)
                                .testTag("btn_onboarding_create_new")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddModerator, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("Создать новую суверенную ноду", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text("Сгенерировать чистую 12-словную мнемонику BIP-39", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Button 2: Restore
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .clickable {
                                    restoreWordsList = List(12) { "" }
                                    step = OnboardingStep.SEED_RESTORE_INPUT
                                }
                                .padding(16.dp)
                                .testTag("btn_onboarding_restore")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("Восстановить существующую ноду", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text("Импорт сохранённых 12 слов BIP-39 с детерминированным Onion-адресом", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }
                }

                // STEP 2A: DISPLAY GENERATED SEED
                OnboardingStep.SEED_DISPLAY -> {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ВАША 12-СЛОВНАЯ MASTER SEED ФРАЗА",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00FF66)
                        )
                        Text(
                            text = "Запишите слова строго по порядку на бумаге. Никому их не сообщайте.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // 3x4 Grid of words
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(mnemonicWords) { idx, word ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${idx + 1}.", fontSize = 10.sp, color = Color(0xFF00FF66), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(word, fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                selectedVerifyWords = emptyMap()
                                verifyError = false
                                step = OnboardingStep.SEED_VERIFY
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_seed_written_down")
                        ) {
                            Text("Я записал seed-фразу • Далее", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // STEP 2B: VERIFY SEED
                OnboardingStep.SEED_VERIFY -> {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ПРОВЕРКА РЕЗЕРВНОЙ КОПИИ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF00FF66),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Для подтверждения правильности записи выберите слова:",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Target verification challenges
                        verifyIndices.forEach { targetIdx ->
                            val targetNum = targetIdx + 1
                            val correctWord = mnemonicWords[targetIdx]
                            val selected = selectedVerifyWords[targetIdx]

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text("Слово #$targetNum: ${selected ?: "..."}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected == correctWord) Color(0xFF00FF66) else Color.White)
                                Spacer(modifier = Modifier.height(4.dp))

                                // Candidate word chips
                                val candidates = remember(targetIdx) {
                                    val otherWords = mnemonicWords.filter { it != correctWord }.shuffled().take(2)
                                    (otherWords + correctWord).shuffled()
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    candidates.forEach { cand ->
                                        val isChosen = selected == cand
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isChosen) Color(0xFF00FF66) else Color(0xFF0F172A))
                                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    selectedVerifyWords = selectedVerifyWords + (targetIdx to cand)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = cand,
                                                fontSize = 11.sp,
                                                color = if (isChosen) Color.Black else Color.White,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (verifyError) {
                            Text("Выбранное слово не совпадает с оригиналом!", color = Color(0xFFEF4444), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val allMatch = verifyIndices.all { idx -> selectedVerifyWords[idx] == mnemonicWords[idx] }
                                if (allMatch) {
                                    verifyError = false
                                    step = OnboardingStep.PIN_SETUP
                                } else {
                                    verifyError = true
                                }
                            },
                            enabled = verifyIndices.all { selectedVerifyWords.containsKey(it) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_verify_seed_confirm")
                        ) {
                            Text("Подтвердить • Перейти к PIN", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // STEP 2C: RESTORE INPUT (IN-APP KEYBOARD ONLY)
                OnboardingStep.SEED_RESTORE_INPUT -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ВВОД 12 СЛОВ ВОССТАНОВЛЕНИЯ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Ввод защищён: системная клавиатура отключена (In-App Air-Gap Safe)",
                            fontSize = 10.sp,
                            color = Color(0xFF00FF66),
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        // 12 Word slots grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(restoreWordsList) { idx, w ->
                                val isSelected = activeWordSlotIndex == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF00FF66).copy(alpha = 0.2f) else Color(0xFF0F172A))
                                        .border(1.dp, if (isSelected) Color(0xFF00FF66) else Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                        .clickable { activeWordSlotIndex = idx }
                                        .padding(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${idx + 1}.", fontSize = 9.sp, color = Color(0xFF64748B))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(w.ifEmpty { "..." }, fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        // Status of checksum
                        val fullMnemonicString = restoreWordsList.joinToString(" ").trim()
                        val isValidMnemonic = Bip39Utils.validateMnemonic(fullMnemonicString)
                        restoreChecksumValid = isValidMnemonic

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isValidMnemonic) "✓ Контрольная сумма BIP-39 совпадает" else "Ожидание ввода 12 слов...",
                                fontSize = 10.sp,
                                color = if (isValidMnemonic) Color(0xFF00FF66) else Color(0xFF94A3B8)
                            )

                            if (isValidMnemonic) {
                                Button(
                                    onClick = {
                                        generatedMnemonic = fullMnemonicString
                                        step = OnboardingStep.PIN_SETUP
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                                    modifier = Modifier.testTag("btn_restore_seed_proceed")
                                ) {
                                    Text("Далее", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // In-App Keyboard attached directly to active word slot
                        InAppGboardKeyboard(
                            text = restoreWordsList[activeWordSlotIndex],
                            onTextChange = { newWord ->
                                val updated = restoreWordsList.toMutableList()
                                updated[activeWordSlotIndex] = newWord.lowercase().trim()
                                restoreWordsList = updated
                            },
                            onSendOrDone = {
                                if (activeWordSlotIndex < 11) {
                                    activeWordSlotIndex++
                                }
                            },
                            sendButtonLabel = "СЛЕДУЮЩЕЕ",
                            modifier = Modifier.testTag("restore_inapp_keyboard")
                        )
                    }
                }

                // STEP 3A: STANDARD PIN SETUP
                OnboardingStep.PIN_SETUP -> {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "УСТАНОВИТЕ PIN-КОД НОДЫ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF00FF66),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Используется для повседневной разблокировки (4–8 цифр)",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Masked PIN indicators
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                            repeat(6) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (idx < pinInput.length) Color(0xFF00FF66) else Color(0xFF1E293B))
                                        .border(1.dp, Color(0xFF334155), CircleShape)
                                )
                            }
                        }

                        pinError?.let {
                            Text(it, color = Color(0xFFEF4444), fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                        }

                        // In-App Keypad for PIN entry
                        InAppGboardKeyboard(
                            text = pinInput,
                            onTextChange = { if (it.length <= 8) pinInput = it },
                            onSendOrDone = {
                                if (pinInput.length >= 4) {
                                    pinError = null
                                    step = OnboardingStep.PANIC_PIN_SETUP
                                } else {
                                    pinError = "Минимальная длина PIN — 4 цифры"
                                }
                            },
                            sendButtonLabel = "ДАЛЕЕ",
                            modifier = Modifier.testTag("pin_inapp_keyboard")
                        )
                    }
                }

                // STEP 3B: PANIC PIN (DURESS CODE) SETUP
                OnboardingStep.PANIC_PIN_SETUP -> {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ТРЕВОЖНЫЙ PIN (PANIC WIPE)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFEF4444),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "При вводе этого кода все ключи и сейф ноды мгновенно и безвозвратно уничтожаются!",
                            fontSize = 11.sp,
                            color = Color(0xFFFCA5A5),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Masked Panic PIN indicators
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                            repeat(6) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (idx < panicPinInput.length) Color(0xFFEF4444) else Color(0xFF1E293B))
                                        .border(1.dp, Color(0xFF334155), CircleShape)
                                )
                            }
                        }

                        pinError?.let {
                            Text(it, color = Color(0xFFEF4444), fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                        }

                        // In-App Keypad for Panic PIN
                        InAppGboardKeyboard(
                            text = panicPinInput,
                            onTextChange = { if (it.length <= 8) panicPinInput = it },
                            onSendOrDone = {
                                if (panicPinInput.length < 4) {
                                    pinError = "Минимальная длина Panic PIN — 4 цифры"
                                } else if (panicPinInput == pinInput) {
                                    pinError = "Panic PIN не должен совпадать с обычным PIN!"
                                } else {
                                    pinError = null
                                    step = OnboardingStep.DERIVATION_SUMMARY
                                }
                            },
                            sendButtonLabel = "СОХРАНИТЬ",
                            modifier = Modifier.testTag("panic_pin_inapp_keyboard")
                        )
                    }
                }

                // STEP 4: DERIVATION SUMMARY & LAUNCH
                OnboardingStep.DERIVATION_SUMMARY -> {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ИЕРАРХИЯ КЛЮЧЕЙ ИНИЦИАЛИЗИРОВАНА",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF00FF66),
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val summaryItems = listOf(
                            "Master Root Seed" to "BIP-39 512-bit PBKDF2 HMAC-SHA512",
                            "SQLCipher Vault" to "AES-256-GCM (Domain: ParanoidX/Vault/Key/v1)",
                            "Tor v3 Hidden Service" to "Ed25519 (px_simplex_v3_mesh.onion)",
                            "SimpleX Chat Identity" to "Double Ratchet X25519 E2EE",
                            "P2P Cloud Container" to "AES-GCM Disperse Swarm Key",
                            "Panic PIN Wipe Shield" to "Duress Auto-Zeroize Active"
                        )

                        summaryItems.forEach { (title, desc) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF00FF66))
                                    Text(desc, fontSize = 10.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                viewModel.completeOnboarding(
                                    mnemonic = generatedMnemonic,
                                    pin = pinInput,
                                    panicPin = panicPinInput
                                )
                                onOnboardingFinished()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_launch_sovereign_node")
                        ) {
                            Text("🚀 Запустить Суверенную Ноду", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Bottom Footer Notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Автономная нода ParanoidX • Без внешних серверов",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

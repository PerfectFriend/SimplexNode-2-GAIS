package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// [REAL] Этап 7-revised: Автономная встроенная клавиатура ноды (In-App Gboard-like Autonomous Keyboard).
// [SECURITY-CRITICAL] Системная клавиатура Android (Gboard, Samsung, AOSP) полностью отключена для предотвращения
// утечек через кейлоггеры, телеметрию вендоров ОС и облачные словари предиктивного ввода Google.
// [PROD-READY] Полная репликация эргономики Gboard: раскладки RU (ЙЦУКЕН) и EN (QWERTY), цифры/символы (?123 и =\<),
// Shift / Caps Lock, тактильный виброотклик (haptic feedback), кнопка скрытия и защищённый статус Air-Gap.

enum class KeyboardLayoutMode {
    LETTERS,
    SYMBOLS_1, // ?123
    SYMBOLS_2  // =\<
}

enum class ShiftState {
    OFF,
    ONCE,
    CAPS_LOCK
}

@Composable
fun InAppGboardKeyboard(
    text: String,
    onTextChange: (String) -> Unit,
    onSendOrDone: () -> Unit,
    modifier: Modifier = Modifier,
    isMatrixStyle: Boolean = false,
    sendButtonLabel: String = "SEND",
    onCloseKeyboard: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var currentLang by remember { mutableStateOf("RU") } // RU or EN
    var layoutMode by remember { mutableStateOf(KeyboardLayoutMode.LETTERS) }
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }

    fun triggerKeyHaptic() {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Exception) {}
    }

    fun handleKeyPress(char: String) {
        triggerKeyHaptic()
        onTextChange(text + char)
        if (shiftState == ShiftState.ONCE) {
            shiftState = ShiftState.OFF
        }
    }

    fun handleDelete() {
        triggerKeyHaptic()
        if (text.isNotEmpty()) {
            onTextChange(text.dropLast(1))
        }
    }

    fun handleSpace() {
        triggerKeyHaptic()
        onTextChange(text + " ")
    }

    fun toggleShift() {
        triggerKeyHaptic()
        shiftState = when (shiftState) {
            ShiftState.OFF -> ShiftState.ONCE
            ShiftState.ONCE -> ShiftState.CAPS_LOCK
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
    }

    val isUppercase = shiftState != ShiftState.OFF

    // Theme Palette (Gboard Dark Slate Style vs Matrix Green)
    val keyboardContainerBg = if (isMatrixStyle) Color(0xFF070B10) else Color(0xFF0E1522)
    val regularKeyBg = if (isMatrixStyle) Color(0xFF101721) else Color(0xFF1E293B)
    val regularKeyBorder = if (isMatrixStyle) Color(0xFF00FF66).copy(alpha = 0.2f) else Color(0xFF334155).copy(alpha = 0.35f)
    val specialKeyBg = if (isMatrixStyle) Color(0xFF16202D) else Color(0xFF141C2B)
    val specialKeyBorder = if (isMatrixStyle) Color(0xFF00FF66).copy(alpha = 0.3f) else Color(0xFF334155).copy(alpha = 0.5f)
    val letterTextColor = if (isMatrixStyle) Color(0xFF00FF66) else Color(0xFFF1F5F9)
    val sendButtonBg = if (isMatrixStyle) Color(0xFF00FF66) else Color(0xFF00E676)
    val sendButtonTextColor = Color.Black

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(keyboardContainerBg)
            .border(1.dp, regularKeyBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("in_app_gboard_keyboard")
    ) {
        // --- 1. TOP ACCESSORY / SECURITY STATUS BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Air-Gap Security Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FF66))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AIR-GAP SHIELD • ZERO TELEMETRY",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF00FF66)
                )
            }

            // Quick Toolbar Actions: Language & Hide Keyboard
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Language indicator pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(specialKeyBg)
                        .border(0.5.dp, specialKeyBorder, RoundedCornerShape(4.dp))
                        .clickable {
                            triggerKeyHaptic()
                            currentLang = if (currentLang == "RU") "EN" else "RU"
                            layoutMode = KeyboardLayoutMode.LETTERS
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (currentLang == "RU") "RU 🇷🇺" else "EN 🇺🇸",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (onCloseKeyboard != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            triggerKeyHaptic()
                            onCloseKeyboard()
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardHide,
                            contentDescription = "Hide Keyboard",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // --- 2. KEYBOARD ROWS CONFIGURATION ---
        when (layoutMode) {
            KeyboardLayoutMode.LETTERS -> {
                if (currentLang == "RU") {
                    // --- RU: ЙЦУКЕН ---
                    // Row 1 (12 keys): й ц у к е н г ш щ з х ъ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val row1 = listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ")
                        row1.forEach { k ->
                            val displayChar = if (isUppercase) k.uppercase() else k
                            GboardKey(
                                label = displayChar,
                                weight = 1f,
                                bg = regularKeyBg,
                                border = regularKeyBorder,
                                textColor = letterTextColor,
                                onClick = { handleKeyPress(displayChar) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 2 (11 keys): ф ы в а п р о л д ж э
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val row2 = listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э")
                        row2.forEach { k ->
                            val displayChar = if (isUppercase) k.uppercase() else k
                            GboardKey(
                                label = displayChar,
                                weight = 1f,
                                bg = regularKeyBg,
                                border = regularKeyBorder,
                                textColor = letterTextColor,
                                onClick = { handleKeyPress(displayChar) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 3: Shift + я ч с м и т ь б ю + Backspace
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Shift Key
                        GboardSpecialKey(
                            icon = Icons.Default.North,
                            weight = 1.35f,
                            bg = if (shiftState != ShiftState.OFF) Color(0xFF00FF66).copy(alpha = 0.25f) else specialKeyBg,
                            border = if (shiftState == ShiftState.CAPS_LOCK) Color(0xFF00FF66) else specialKeyBorder,
                            iconTint = if (shiftState != ShiftState.OFF) Color(0xFF00FF66) else Color(0xFF94A3B8),
                            onClick = { toggleShift() }
                        )

                        val row3 = listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю")
                        row3.forEach { k ->
                            val displayChar = if (isUppercase) k.uppercase() else k
                            GboardKey(
                                label = displayChar,
                                weight = 1f,
                                bg = regularKeyBg,
                                border = regularKeyBorder,
                                textColor = letterTextColor,
                                onClick = { handleKeyPress(displayChar) }
                            )
                        }

                        // Backspace Key
                        GboardSpecialKey(
                            icon = Icons.Default.Backspace,
                            weight = 1.35f,
                            bg = specialKeyBg,
                            border = specialKeyBorder,
                            iconTint = Color(0xFFEF4444),
                            onClick = { handleDelete() }
                        )
                    }
                } else {
                    // --- EN: QWERTY ---
                    // Row 1 (10 keys with top-hint numbers like Gboard): q w e r t y u i o p
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
                        val hints = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                        row1.forEachIndexed { i, k ->
                            val displayChar = if (isUppercase) k.uppercase() else k
                            GboardKeyWithHint(
                                label = displayChar,
                                hint = hints[i],
                                weight = 1f,
                                bg = regularKeyBg,
                                border = regularKeyBorder,
                                textColor = letterTextColor,
                                onClick = { handleKeyPress(displayChar) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 2 (9 keys): a s d f g h j k l
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
                        row2.forEach { k ->
                            val displayChar = if (isUppercase) k.uppercase() else k
                            GboardKey(
                                label = displayChar,
                                weight = 1f,
                                bg = regularKeyBg,
                                border = regularKeyBorder,
                                textColor = letterTextColor,
                                onClick = { handleKeyPress(displayChar) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 3: Shift + z x c v b n m + Backspace
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Shift Key
                        GboardSpecialKey(
                            icon = Icons.Default.North,
                            weight = 1.4f,
                            bg = if (shiftState != ShiftState.OFF) Color(0xFF00FF66).copy(alpha = 0.25f) else specialKeyBg,
                            border = if (shiftState == ShiftState.CAPS_LOCK) Color(0xFF00FF66) else specialKeyBorder,
                            iconTint = if (shiftState != ShiftState.OFF) Color(0xFF00FF66) else Color(0xFF94A3B8),
                            onClick = { toggleShift() }
                        )

                        val row3 = listOf("z", "x", "c", "v", "b", "n", "m")
                        row3.forEach { k ->
                            val displayChar = if (isUppercase) k.uppercase() else k
                            GboardKey(
                                label = displayChar,
                                weight = 1f,
                                bg = regularKeyBg,
                                border = regularKeyBorder,
                                textColor = letterTextColor,
                                onClick = { handleKeyPress(displayChar) }
                            )
                        }

                        // Backspace Key
                        GboardSpecialKey(
                            icon = Icons.Default.Backspace,
                            weight = 1.4f,
                            bg = specialKeyBg,
                            border = specialKeyBorder,
                            iconTint = Color(0xFFEF4444),
                            onClick = { handleDelete() }
                        )
                    }
                }
            }

            KeyboardLayoutMode.SYMBOLS_1 -> {
                // --- ?123 Symbols Mode ---
                // Row 1 (Digits 1-0)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                    row1.forEach { char ->
                        GboardKey(char, 1f, regularKeyBg, regularKeyBorder, letterTextColor) { handleKeyPress(char) }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Row 2: @ # $ % & * - + ( )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val row2 = listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")")
                    row2.forEach { char ->
                        GboardKey(char, 1f, regularKeyBg, regularKeyBorder, letterTextColor) { handleKeyPress(char) }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Row 3: Switch to Symbols 2 (=<) + ! " ' : ; / ? + Del
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    GboardKey("=\\<", 1.4f, specialKeyBg, specialKeyBorder, Color(0xFF38BDF8)) {
                        triggerKeyHaptic()
                        layoutMode = KeyboardLayoutMode.SYMBOLS_2
                    }

                    val row3 = listOf("!", "\"", "'", ":", ";", "/", "?")
                    row3.forEach { char ->
                        GboardKey(char, 1f, regularKeyBg, regularKeyBorder, letterTextColor) { handleKeyPress(char) }
                    }

                    GboardSpecialKey(
                        icon = Icons.Default.Backspace,
                        weight = 1.4f,
                        bg = specialKeyBg,
                        border = specialKeyBorder,
                        iconTint = Color(0xFFEF4444),
                        onClick = { handleDelete() }
                    )
                }
            }

            KeyboardLayoutMode.SYMBOLS_2 -> {
                // --- =\< Advanced Symbols Mode ---
                // Row 1: ~ ` | • √ π ÷ × ¶ ∆
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val row1 = listOf("~", "`", "|", "{", "}", "[", "]", "<", ">", "^")
                    row1.forEach { char ->
                        GboardKey(char, 1f, regularKeyBg, regularKeyBorder, letterTextColor) { handleKeyPress(char) }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Row 2: € £ ¥ ₽ © ® ™ ° \ =
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val row2 = listOf("€", "£", "¥", "₽", "©", "®", "™", "°", "\\", "=")
                    row2.forEach { char ->
                        GboardKey(char, 1f, regularKeyBg, regularKeyBorder, letterTextColor) { handleKeyPress(char) }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Row 3: Switch back to ?123 + _ § ¿ ¡ « » ± + Del
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    GboardKey("?123", 1.4f, specialKeyBg, specialKeyBorder, Color(0xFF38BDF8)) {
                        triggerKeyHaptic()
                        layoutMode = KeyboardLayoutMode.SYMBOLS_1
                    }

                    val row3 = listOf("_", "§", "¿", "¡", "«", "»", "±")
                    row3.forEach { char ->
                        GboardKey(char, 1f, regularKeyBg, regularKeyBorder, letterTextColor) { handleKeyPress(char) }
                    }

                    GboardSpecialKey(
                        icon = Icons.Default.Backspace,
                        weight = 1.4f,
                        bg = specialKeyBg,
                        border = specialKeyBorder,
                        iconTint = Color(0xFFEF4444),
                        onClick = { handleDelete() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // --- 3. BOTTOM ROW (GBOARD STYLE: ?123, GLOBE, COMMA, SPACEBAR, PERIOD, SEND) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Switcher (?123 / ABC)
            val isSymbols = layoutMode != KeyboardLayoutMode.LETTERS
            GboardKey(
                label = if (isSymbols) "ABC" else "?123",
                weight = 1.35f,
                bg = specialKeyBg,
                border = specialKeyBorder,
                textColor = Color(0xFF38BDF8),
                onClick = {
                    triggerKeyHaptic()
                    layoutMode = if (isSymbols) KeyboardLayoutMode.LETTERS else KeyboardLayoutMode.SYMBOLS_1
                }
            )

            // Language Toggle Key (Globe)
            GboardSpecialKey(
                icon = Icons.Default.Language,
                weight = 1f,
                bg = specialKeyBg,
                border = specialKeyBorder,
                iconTint = Color(0xFF94A3B8),
                onClick = {
                    triggerKeyHaptic()
                    currentLang = if (currentLang == "RU") "EN" else "RU"
                    layoutMode = KeyboardLayoutMode.LETTERS
                }
            )

            // Comma Key
            GboardKey(
                label = ",",
                weight = 0.9f,
                bg = regularKeyBg,
                border = regularKeyBorder,
                textColor = letterTextColor,
                onClick = { handleKeyPress(",") }
            )

            // Spacebar (wide, Gboard style with localized label)
            Card(
                colors = CardDefaults.cardColors(containerColor = regularKeyBg),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(3.4f)
                    .height(40.dp)
                    .border(0.5.dp, regularKeyBorder, RoundedCornerShape(6.dp))
                    .clickable { handleSpace() }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (currentLang == "RU") "Русский" else "English",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Default
                    )
                }
            }

            // Period Key
            GboardKey(
                label = ".",
                weight = 0.9f,
                bg = regularKeyBg,
                border = regularKeyBorder,
                textColor = letterTextColor,
                onClick = { handleKeyPress(".") }
            )

            // Action / Send / Done Key (Prominent Gboard Pill)
            Card(
                colors = CardDefaults.cardColors(containerColor = sendButtonBg),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1.6f)
                    .height(40.dp)
                    .border(0.5.dp, sendButtonBg, RoundedCornerShape(6.dp))
                    .clickable {
                        triggerKeyHaptic()
                        onSendOrDone()
                    }
                    .testTag("in_app_keyboard_send_button")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = sendButtonLabel,
                            tint = sendButtonTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.GboardKey(
    label: String,
    weight: Float,
    bg: Color,
    border: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .weight(weight)
            .height(38.dp)
            .border(0.5.dp, border, RoundedCornerShape(5.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                fontFamily = FontFamily.Default
            )
        }
    }
}

@Composable
private fun RowScope.GboardKeyWithHint(
    label: String,
    hint: String,
    weight: Float,
    bg: Color,
    border: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .weight(weight)
            .height(38.dp)
            .border(0.5.dp, border, RoundedCornerShape(5.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Hint digit in top right corner
            Text(
                text = hint,
                fontSize = 8.sp,
                color = Color(0xFF64748B),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 3.dp, top = 1.dp)
            )
            // Main letter
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                fontFamily = FontFamily.Default,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun RowScope.GboardSpecialKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    weight: Float,
    bg: Color,
    border: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .weight(weight)
            .height(38.dp)
            .border(0.5.dp, border, RoundedCornerShape(5.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

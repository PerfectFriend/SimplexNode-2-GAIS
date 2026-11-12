package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
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
import com.example.data.security.RootKeyHub
import com.example.ui.components.InAppGboardKeyboard
import com.example.ui.viewmodel.MainViewModel

// [REAL] Этап 8: Экран разблокировки ноды по PIN-коду с поддержкой Panic PIN.
// [SECURITY-CRITICAL] Ввод PIN осуществляется исключительно через внутрипроцессную клавиатуру.
// [SECURITY-CRITICAL] Ввод Panic PIN безмолвно запускает процедуру полного стирания (Panic Wipe).

@Composable
fun PinLockScreen(
    viewModel: MainViewModel,
    onUnlockSuccess: () -> Unit,
    onPanicWiped: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
            .padding(16.dp)
            .testTag("pin_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(2.dp, Color(0xFF00FF66), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Node",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Нода ParanoidX Заблокирована",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Введите суверенный PIN-код для расшифровки мастер-сида",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Masked PIN indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    repeat(6) { idx ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (idx < enteredPin.length) Color(0xFF00FF66) else Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), CircleShape)
                        )
                    }
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Autonomous In-App Keypad
            InAppGboardKeyboard(
                text = enteredPin,
                onTextChange = { if (it.length <= 8) enteredPin = it },
                onSendOrDone = {
                    if (enteredPin.isNotBlank()) {
                        val result = viewModel.verifyPinOrPanic(enteredPin)
                        when (result) {
                            RootKeyHub.PinResult.SUCCESS -> {
                                errorMessage = null
                                onUnlockSuccess()
                            }
                            RootKeyHub.PinResult.PANIC_WIPED -> {
                                errorMessage = "ТРЕВОГА: Данные ноды уничтожены (Panic Wipe)!"
                                onPanicWiped()
                            }
                            RootKeyHub.PinResult.INVALID -> {
                                errorMessage = "Неверный PIN-код ноды!"
                                enteredPin = ""
                            }
                        }
                    }
                },
                sendButtonLabel = "РАЗБЛОКИРОВАТЬ",
                modifier = Modifier.testTag("pin_lock_keyboard")
            )
        }
    }
}

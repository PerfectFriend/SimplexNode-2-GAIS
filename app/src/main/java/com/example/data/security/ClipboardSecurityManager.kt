package com.example.data.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// [REAL] Этап 13 (Security Pass): Безопасная работа с системным буфером обмена.
// Маркирует сиды, пароли и секреты флагом EXTRA_IS_SENSITIVE (скрывает превью в Android 13+)
// и автоматически затирает буфер по таймеру (по умолчанию 30с) или при lockNode / panicWipe.
object ClipboardSecurityManager {

    const val EXTRA_IS_SENSITIVE_KEY = "android.content.extra.IS_SENSITIVE"
    const val DEFAULT_AUTO_WIPE_MS = 30_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var pendingWipeJob: Job? = null

    @Volatile
    private var lastCopyTimestampMs: Long = 0L

    fun copySensitiveSecret(
        context: Context,
        label: String,
        secret: String,
        autoWipeMs: Long = DEFAULT_AUTO_WIPE_MS
    ): Boolean {
        if (secret.isEmpty()) return false
        val appCtx = context.applicationContext ?: context
        return try {
            val cm = appCtx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
            val clip = ClipData.newPlainText(label, secret).apply {
                val extras = PersistableBundle().apply {
                    putBoolean(EXTRA_IS_SENSITIVE_KEY, true)
                }
                description.extras = extras
            }
            cm.setPrimaryClip(clip)
            lastCopyTimestampMs = System.currentTimeMillis()
            scheduleAutoWipe(appCtx, autoWipeMs)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun scheduleAutoWipe(appCtx: Context, autoWipeMs: Long) {
        pendingWipeJob?.cancel()
        if (autoWipeMs <= 0L) return
        pendingWipeJob = scope.launch {
            delay(autoWipeMs)
            wipeClipboardNow(appCtx)
        }
    }

    fun wipeClipboardNow(context: Context): Boolean {
        pendingWipeJob?.cancel()
        pendingWipeJob = null
        val appCtx = context.applicationContext ?: context
        return try {
            val cm = appCtx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cm.clearPrimaryClip()
            } else {
                cm.setPrimaryClip(ClipData.newPlainText("", ""))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasPendingAutoWipe(): Boolean = pendingWipeJob?.isActive == true

    fun getLastCopyTimestampMs(): Long = lastCopyTimestampMs
}

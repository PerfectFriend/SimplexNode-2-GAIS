package com.example.network.tor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

// [REAL] Этап 11 (P2.1): Реальный TCP-клиент Tor ControlPort (Control-Spec v1: AUTHENTICATE + GETINFO + SIGNAL NEWNYM).
// [HONEST-TELEMETRY] Никаких случайных чисел: если ControlPort 9051 не слушает (например, FallbackSimulation без ELF),
// клиент честно возвращает reachable=false и rttMs=0 без маскировки.
object TorControlClient {

    data class TorControlTelemetry(
        val reachable: Boolean = false,
        val authenticated: Boolean = false,
        val networkLiveness: String = "unknown",
        val bootstrapPhase: String = "IDLE",
        val builtCircuits: Int = 0,
        val circuitSummaries: List<String> = emptyList(),
        val rttMs: Int = 0,
        val sourceLabel: String = "NO_CONTROLPORT",
        val error: String? = null,
        val queriedAtMs: Long = System.currentTimeMillis()
    )

    fun readControlCookieHex(cookieFile: File?): String? {
        if (cookieFile == null || !cookieFile.exists() || !cookieFile.isFile) return null
        return try {
            val bytes = cookieFile.readBytes()
            if (bytes.isEmpty()) null else bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun queryTelemetry(
        host: String = "127.0.0.1",
        port: Int = 9051,
        cookieFile: File? = null,
        timeoutMs: Int = 2000
    ): TorControlTelemetry = withContext(Dispatchers.IO) {
        val cookieHex = readControlCookieHex(cookieFile)
        val startNs = System.nanoTime()

        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                socket.soTimeout = timeoutMs

                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.US_ASCII))
                val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.US_ASCII)

                val authReply = authenticate(reader, writer, cookieHex)
                if (!authReply.first) {
                    val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceAtLeast(1)
                    sendCommandOnly(writer, "QUIT")
                    return@withContext TorControlTelemetry(
                        reachable = true,
                        authenticated = false,
                        rttMs = elapsedMs,
                        sourceLabel = "CONTROLPORT_AUTH_FAILED",
                        error = authReply.second
                    )
                }

                val livenessLines = executeCommand(reader, writer, "GETINFO network-liveness")
                val bootstrapLines = executeCommand(reader, writer, "GETINFO status/bootstrap-phase")
                val circuitLines = executeCommand(reader, writer, "GETINFO circuit-status")

                sendCommandOnly(writer, "QUIT")
                val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceAtLeast(1)

                val liveness = parseKeyValue(livenessLines, "network-liveness") ?: "unknown"
                val bootstrap = parseKeyValue(bootstrapLines, "status/bootstrap-phase") ?: "RUNNING"
                val builtCircuits = parseBuiltCircuits(circuitLines)

                TorControlTelemetry(
                    reachable = true,
                    authenticated = true,
                    networkLiveness = liveness,
                    bootstrapPhase = bootstrap,
                    builtCircuits = builtCircuits.size,
                    circuitSummaries = builtCircuits,
                    rttMs = elapsedMs,
                    sourceLabel = "CONTROLPORT_LIVE"
                )
            }
        } catch (e: Exception) {
            TorControlTelemetry(
                reachable = false,
                authenticated = false,
                networkLiveness = "offline",
                bootstrapPhase = "UNREACHABLE",
                builtCircuits = 0,
                circuitSummaries = emptyList(),
                rttMs = 0,
                sourceLabel = "NO_CONTROLPORT",
                error = e.message ?: e::class.java.simpleName
            )
        }
    }

    suspend fun sendSignalNewNym(
        host: String = "127.0.0.1",
        port: Int = 9051,
        cookieFile: File? = null,
        timeoutMs: Int = 2000
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cookieHex = readControlCookieHex(cookieFile)
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                socket.soTimeout = timeoutMs

                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.US_ASCII))
                val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.US_ASCII)

                val authReply = authenticate(reader, writer, cookieHex)
                if (!authReply.first) {
                    sendCommandOnly(writer, "QUIT")
                    return@withContext Pair(false, "ControlPort AUTH rejected: ${authReply.second}")
                }

                val newNymLines = executeCommand(reader, writer, "SIGNAL NEWNYM")
                sendCommandOnly(writer, "QUIT")

                val ok = newNymLines.any { it.startsWith("250") }
                val summary = newNymLines.lastOrNull() ?: "No reply"
                Pair(ok, summary)
            }
        } catch (e: Exception) {
            Pair(false, "ControlPort $host:$port unreachable: ${e.message}")
        }
    }

    private fun authenticate(
        reader: BufferedReader,
        writer: OutputStreamWriter,
        cookieHex: String?
    ): Pair<Boolean, String> {
        val cmd = if (!cookieHex.isNullOrBlank()) {
            "AUTHENTICATE $cookieHex"
        } else {
            "AUTHENTICATE \"\""
        }
        val lines = executeCommand(reader, writer, cmd)
        val ok = lines.any { it.startsWith("250") }
        return Pair(ok, lines.lastOrNull() ?: "Empty AUTH reply")
    }

    private fun sendCommandOnly(writer: OutputStreamWriter, command: String) {
        try {
            writer.write("$command\r\n")
            writer.flush()
        } catch (_: Exception) {
        }
    }

    private fun executeCommand(
        reader: BufferedReader,
        writer: OutputStreamWriter,
        command: String
    ): List<String> {
        writer.write("$command\r\n")
        writer.flush()

        val result = mutableListOf<String>()
        var inMultiLineData = false
        while (true) {
            val line = reader.readLine() ?: break
            if (inMultiLineData) {
                if (line == ".") {
                    inMultiLineData = false
                    continue
                }
                result.add(line)
                continue
            }

            result.add(line)
            if (line.length >= 4 && line[3] == '+') {
                inMultiLineData = true
            } else if (line.length >= 4 && line[3] == ' ') {
                break
            } else if (line.length < 4) {
                break
            }
        }
        return result
    }

    private fun parseKeyValue(lines: List<String>, key: String): String? {
        for (line in lines) {
            val cleaned = line.removePrefix("250-").removePrefix("250 ").trim()
            if (cleaned.startsWith("$key=")) {
                return cleaned.substringAfter("$key=").trim()
            }
        }
        return null
    }

    private fun parseBuiltCircuits(lines: List<String>): List<String> {
        val circuits = mutableListOf<String>()
        for (raw in lines) {
            val line = raw.removePrefix("250-").removePrefix("250 ").removePrefix("circuit-status=").trim()
            if (line.isBlank() || line == "OK") continue
            val parts = line.split(Regex("\\s+"))
            if (parts.size >= 2 && parts[1] == "BUILT") {
                val id = parts[0]
                val pathPart = parts.getOrNull(2)?.takeIf { !it.contains("=") } ?: "direct"
                circuits.add("Circ #$id ($pathPart)")
            }
        }
        return circuits
    }
}

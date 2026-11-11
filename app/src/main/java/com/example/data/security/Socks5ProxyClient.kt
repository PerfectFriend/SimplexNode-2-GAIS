package com.example.data.security

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

// [REAL] Низкоуровневая реализация RFC 1928 SOCKS5 клиента через сырые сокеты Java Socket.
// [REAL] Поддержка SOCKS5 туннелирования: метод openTunnel(...) открывает реальный поток данных через Tor proxy.
// [REAL] Поддержка проверочного HTTP-зонда sendProbeHttp(...) для сквозной валидации передачи данных через Tor.
// [SECURITY-CRITICAL] Защита от DNS-leak через ATYP 0x03 DOMAINNAME (резолвинг доменов и .onion адресов строго внутри Tor).
// [PROD-READY] Полный сетевой слой SOCKS5 клиента, готовый к интеграции с SMP/XFTP на следующих этапах.
object Socks5ProxyClient {
    private const val TAG = "Socks5ProxyClient"

    interface LogCallback {
        fun onLog(message: String)
    }

    private fun safeLog(msg: String) {
        try {
            Log.d(TAG, msg)
        } catch (_: Throwable) {
            // JVM unit test runner safe fallback
        }
    }

    /**
     * Opens a real SOCKS5 tunnel to [targetHost]:[targetPort] through [proxyHost]:[proxyPort].
     * If successful, returns the LIVE, connected [Socket] ready for bidirectional I/O.
     * The caller is responsible for closing the socket when done.
     */
    fun openTunnel(
        proxyHost: String = "127.0.0.1",
        proxyPort: Int = 9050,
        authMode: String = "NONE", // NONE, USER_PASS
        username: String = "",
        password: String = "",
        targetHost: String,
        targetPort: Int,
        dnsLeakProtection: Boolean = true,
        connectTimeoutMs: Int = 5000,
        readTimeoutMs: Int = 8000,
        callback: LogCallback? = null
    ): Socket {
        callback?.onLog("SOCKS5_CLIENT: Connecting to proxy at $proxyHost:$proxyPort...")
        val socket = Socket()
        socket.connect(InetSocketAddress(proxyHost, proxyPort), connectTimeoutMs)
        socket.soTimeout = readTimeoutMs
        callback?.onLog("SOCKS5_CLIENT: Connected to proxy. Starting RFC 1928 negotiation.")

        val outStream: OutputStream = socket.getOutputStream()
        val inStream: InputStream = socket.getInputStream()

        // 1. SOCKS5 greeting/methods negotiation
        val methods = if (authMode == "USER_PASS") {
            byteArrayOf(0x00, 0x02)
        } else {
            byteArrayOf(0x00)
        }

        val greeting = ByteArray(2 + methods.size)
        greeting[0] = 0x05 // SOCKS version 5
        greeting[1] = methods.size.toByte()
        System.arraycopy(methods, 0, greeting, 2, methods.size)

        callback?.onLog("SOCKS5_CLIENT: Sending greeting frame [VER=0x05, NMETHODS=${methods.size}]")
        outStream.write(greeting)
        outStream.flush()

        // 2. Read greeting response
        val response = ByteArray(2)
        val readBytes = inStream.read(response)
        if (readBytes < 2) {
            socket.close()
            throw IllegalStateException("SOCKS5 proxy terminated connection during method negotiation")
        }

        val version = response[0].toInt() and 0xFF
        val selectedMethod = response[1].toInt() and 0xFF
        if (version != 0x05) {
            socket.close()
            throw IllegalStateException("Unsupported SOCKS version: 0x%02X".format(version))
        }

        // 3. Negotiate Auth if requested
        if (selectedMethod == 0x02) {
            if (authMode != "USER_PASS") {
                socket.close()
                throw IllegalStateException("Proxy demands USER/PASS authentication")
            }
            callback?.onLog("SOCKS5_CLIENT: Sending credentials for user '$username'...")
            val userBytes = username.toByteArray(Charsets.UTF_8)
            val passBytes = password.toByteArray(Charsets.UTF_8)

            val authRequest = ByteArray(3 + userBytes.size + passBytes.size)
            authRequest[0] = 0x01
            authRequest[1] = userBytes.size.toByte()
            System.arraycopy(userBytes, 0, authRequest, 2, userBytes.size)
            authRequest[2 + userBytes.size] = passBytes.size.toByte()
            System.arraycopy(passBytes, 0, authRequest, 3 + userBytes.size, passBytes.size)

            outStream.write(authRequest)
            outStream.flush()

            val authResponse = ByteArray(2)
            val authRead = inStream.read(authResponse)
            if (authRead < 2 || (authResponse[1].toInt() and 0xFF) != 0x00) {
                socket.close()
                throw IllegalStateException("SOCKS5 authentication failed")
            }
            callback?.onLog("SOCKS5_CLIENT: Authentication accepted.")
        } else if (selectedMethod == 0xFF) {
            socket.close()
            throw IllegalStateException("No acceptable authentication methods offered")
        }

        // 4. Send Connect Command
        val targetBytes = targetHost.toByteArray(Charsets.UTF_8)
        val requestStream = ByteArrayOutputStream()
        requestStream.write(0x05) // VER
        requestStream.write(0x01) // CMD: CONNECT
        requestStream.write(0x00) // RSV

        if (dnsLeakProtection) {
            // ATYP 0x03: Domain name resolved on remote Tor exit node
            requestStream.write(0x03)
            requestStream.write(targetBytes.size)
            requestStream.write(targetBytes)
        } else {
            callback?.onLog("SOCKS5_CLIENT: [DNS LEAK WARNING] Resolving $targetHost locally...")
            val localAddress = java.net.InetAddress.getByName(targetHost)
            val addressBytes = localAddress.address
            if (addressBytes.size == 4) {
                requestStream.write(0x01) // IPv4
            } else {
                requestStream.write(0x04) // IPv6
            }
            requestStream.write(addressBytes)
        }

        requestStream.write((targetPort shr 8) and 0xFF)
        requestStream.write(targetPort and 0xFF)

        val connectRequest = requestStream.toByteArray()
        callback?.onLog("SOCKS5_CLIENT: Sending CONNECT to $targetHost:$targetPort [ATYP=${if (dnsLeakProtection) "0x03 DOMAIN" else "IP"}]")
        outStream.write(connectRequest)
        outStream.flush()

        // 5. Read response to CONNECT request
        val connectResponse = ByteArray(1024)
        val connRead = inStream.read(connectResponse)
        if (connRead < 4) {
            socket.close()
            throw IllegalStateException("Proxy disconnected before tunnel reply")
        }

        val replyCode = connectResponse[1].toInt() and 0xFF
        if (replyCode != 0x00) {
            socket.close()
            val reason = when (replyCode) {
                0x01 -> "General SOCKS server failure"
                0x02 -> "Connection not allowed by ruleset"
                0x03 -> "Network unreachable"
                0x04 -> "Host unreachable"
                0x05 -> "Connection refused"
                0x06 -> "TTL expired"
                0x07 -> "Command not supported"
                0x08 -> "Address type not supported"
                else -> "Error code 0x%02X".format(replyCode)
            }
            throw IllegalStateException("Tunnel establishment failed: $reason")
        }

        callback?.onLog("SOCKS5_CLIENT: SOCKS5 PRIVACY TUNNEL FULLY ESTABLISHED!")
        return socket
    }

    /**
     * Diagnostic handshake test: Opens the tunnel and safely closes the socket.
     */
    fun testConnection(
        proxyHost: String,
        proxyPort: Int,
        authMode: String,
        username: String = "",
        password: String = "",
        targetHost: String,
        targetPort: Int,
        dnsLeakProtection: Boolean,
        callback: LogCallback
    ): Boolean {
        return try {
            val socket = openTunnel(
                proxyHost = proxyHost,
                proxyPort = proxyPort,
                authMode = authMode,
                username = username,
                password = password,
                targetHost = targetHost,
                targetPort = targetPort,
                dnsLeakProtection = dnsLeakProtection,
                connectTimeoutMs = 3000,
                readTimeoutMs = 3000,
                callback = callback
            )
            socket.close()
            callback.onLog("SOCKS5_CLIENT: Test handshake completed successfully.")
            true
        } catch (e: Exception) {
            callback.onLog("SOCKS5_CLIENT: Connection error: ${e.message}")
            safeLog("SOCKS5 testConnection failed: ${e.message}")
            false
        }
    }

    /**
     * Sends a live HTTP GET probe through the established SOCKS5 tunnel to prove data transfer.
     * Returns Pair(success, responseSummary).
     */
    fun sendProbeHttp(
        proxyHost: String = "127.0.0.1",
        proxyPort: Int = 9050,
        targetHost: String = "check.torproject.org",
        targetPort: Int = 80,
        dnsLeakProtection: Boolean = true,
        callback: LogCallback
    ): Pair<Boolean, String> {
        var socket: Socket? = null
        return try {
            callback.onLog("SOCKS5_PROBE: Establishing tunnel for HTTP probe to $targetHost:$targetPort...")
            socket = openTunnel(
                proxyHost = proxyHost,
                proxyPort = proxyPort,
                targetHost = targetHost,
                targetPort = targetPort,
                dnsLeakProtection = dnsLeakProtection,
                connectTimeoutMs = 4000,
                readTimeoutMs = 6000,
                callback = callback
            )

            val out = socket.getOutputStream()
            val input = socket.getInputStream()

            val httpRequest = "GET / HTTP/1.1\r\n" +
                    "Host: $targetHost\r\n" +
                    "User-Agent: ParanoidX-Node/1.0\r\n" +
                    "Accept: */*\r\n" +
                    "Connection: close\r\n\r\n"

            callback.onLog("SOCKS5_PROBE: Transmitting HTTP GET request via Tor SOCKS5 tunnel...")
            out.write(httpRequest.toByteArray(Charsets.US_ASCII))
            out.flush()

            val buffer = ByteArray(2048)
            val bytesRead = input.read(buffer)
            if (bytesRead > 0) {
                val responseHeader = String(buffer, 0, bytesRead, Charsets.UTF_8).lines().firstOrNull() ?: "HTTP/1.1 Response"
                callback.onLog("SOCKS5_PROBE: [SUCCESS] Received HTTP response through Tor: $responseHeader ($bytesRead bytes read)")
                Pair(true, responseHeader)
            } else {
                callback.onLog("SOCKS5_PROBE: Empty response received from target.")
                Pair(false, "Empty response")
            }
        } catch (e: Exception) {
            callback.onLog("SOCKS5_PROBE: Probe failed: ${e.message}")
            Pair(false, e.message ?: "Probe error")
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }
}

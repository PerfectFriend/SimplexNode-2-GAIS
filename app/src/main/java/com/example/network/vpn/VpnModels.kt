package com.example.network.vpn

import android.util.Base64
import java.util.UUID

/**
 * VPN Protocol Type Enumeration covering all major privacy and anti-censorship VPN standards.
 */
enum class VpnProtocolType(val displayName: String, val defaultPort: Int, val description: String) {
    WIREGUARD("WireGuard", 51820, "Next-gen UDP crypto tunnel (Curve25519 & ChaCha20-Poly1305)"),
    OPENVPN("OpenVPN", 1194, "Battle-tested SSL/TLS VPN with AES-256-GCM and HMAC-SHA256"),
    SHADOWSOCKS("Shadowsocks (AEAD)", 8388, "Anti-censorship encrypted SOCKS5 tunnel (SIP008 AEAD)"),
    V2RAY_VMESS("V2Ray (VMess/VLESS)", 443, "Multi-hop routing protocol with TLS & WebSocket camouflage"),
    TAILSCALE("Tailscale Mesh", 41641, "Decentralized WireGuard mesh with DERP relay fallback")
}

data class VpnProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val protocol: VpnProtocolType,
    val serverAddress: String,
    val serverPort: Int,
    val rawConfig: String,
    val isEnabled: Boolean = true,
    val mtu: Int = 1420,
    val dnsServers: List<String> = listOf("1.1.1.1", "9.9.9.9"),
    val splitTunneling: Boolean = false,
    val killSwitchEnabled: Boolean = true,
    val lastPingMs: Int = 0
)

/**
 * Parsers and generators for popular VPN configuration formats.
 */
object VpnConfigParser {

    /**
     * Parses a WireGuard standard .conf configuration file:
     * [Interface]
     * PrivateKey = ...
     * Address = 10.0.0.2/24
     * DNS = 1.1.1.1
     * [Peer]
     * PublicKey = ...
     * Endpoint = 198.51.100.1:51820
     * AllowedIPs = 0.0.0.0/0
     */
    fun parseWireGuardConfig(configText: String, profileName: String = "WireGuard Tunnel"): VpnProfile {
        var endpoint = "127.0.0.1:51820"
        var dnsList = listOf("1.1.1.1", "9.9.9.9")
        var mtu = 1420

        configText.lines().forEach { line ->
            val clean = line.trim()
            if (clean.startsWith("Endpoint", ignoreCase = true)) {
                endpoint = clean.substringAfter("=").trim()
            } else if (clean.startsWith("DNS", ignoreCase = true)) {
                dnsList = clean.substringAfter("=").split(",").map { it.trim() }
            } else if (clean.startsWith("MTU", ignoreCase = true)) {
                mtu = clean.substringAfter("=").trim().toIntOrNull() ?: 1420
            }
        }

        val host = endpoint.substringBeforeLast(":")
        val port = endpoint.substringAfterLast(":").toIntOrNull() ?: 51820

        return VpnProfile(
            name = profileName,
            protocol = VpnProtocolType.WIREGUARD,
            serverAddress = host,
            serverPort = port,
            rawConfig = configText,
            mtu = mtu,
            dnsServers = dnsList
        )
    }

    /**
     * Parses an OpenVPN .ovpn configuration file:
     * client
     * dev tun
     * proto udp
     * remote 198.51.100.2 1194
     * cipher AES-256-GCM
     */
    fun parseOpenVpnConfig(configText: String, profileName: String = "OpenVPN Profile"): VpnProfile {
        var remoteHost = "vpn.paranoid-mesh.net"
        var remotePort = 1194

        configText.lines().forEach { line ->
            val tokens = line.trim().split("\\s+".toRegex())
            if (tokens.isNotEmpty() && tokens[0].equals("remote", ignoreCase = true)) {
                if (tokens.size >= 2) remoteHost = tokens[1]
                if (tokens.size >= 3) remotePort = tokens[2].toIntOrNull() ?: 1194
            }
        }

        return VpnProfile(
            name = profileName,
            protocol = VpnProtocolType.OPENVPN,
            serverAddress = remoteHost,
            serverPort = remotePort,
            rawConfig = configText,
            mtu = 1500,
            dnsServers = listOf("1.1.1.1", "8.8.8.8")
        )
    }

    /**
     * Parses a Shadowsocks URI (ss://BASE64@HOST:PORT?plugin=...)
     */
    fun parseShadowsocksUri(uriString: String, profileName: String = "Shadowsocks Node"): VpnProfile {
        return try {
            val withoutPrefix = uriString.removePrefix("ss://")
            val userInfoAndHost = withoutPrefix.substringBefore("#")
            
            val hostPort = userInfoAndHost.substringAfter("@")
            val host = hostPort.substringBeforeLast(":")
            val port = hostPort.substringAfterLast(":").substringBefore("?").toIntOrNull() ?: 8388

            VpnProfile(
                name = profileName,
                protocol = VpnProtocolType.SHADOWSOCKS,
                serverAddress = host,
                serverPort = port,
                rawConfig = uriString,
                mtu = 1400
            )
        } catch (e: Exception) {
            VpnProfile(
                name = profileName,
                protocol = VpnProtocolType.SHADOWSOCKS,
                serverAddress = "ss.paranoid-mesh.net",
                serverPort = 8388,
                rawConfig = uriString
            )
        }
    }

    /**
     * Generates a sample ready-to-use profile for each supported protocol.
     */
    fun getSampleProfiles(): List<VpnProfile> {
        return listOf(
            VpnProfile(
                name = "WireGuard Stealth Alpha",
                protocol = VpnProtocolType.WIREGUARD,
                serverAddress = "wg-de1.paranoid-mesh.net",
                serverPort = 51820,
                rawConfig = """
                    [Interface]
                    PrivateKey = aGVsbG9fd2lyZWd1YXJkX3ByaXZhdGVfa2V5XzMyYg==
                    Address = 10.14.0.2/32
                    DNS = 1.1.1.1, 9.9.9.9
                    MTU = 1420
                    
                    [Peer]
                    PublicKey = c2VydmVyX3B1YmxpY19rZXlfZm9yX2VuZHBvaW50XzMyYg==
                    Endpoint = wg-de1.paranoid-mesh.net:51820
                    AllowedIPs = 0.0.0.0/0, ::/0
                """.trimIndent(),
                lastPingMs = 38
            ),
            VpnProfile(
                name = "OpenVPN Secure Shield",
                protocol = VpnProtocolType.OPENVPN,
                serverAddress = "ovpn-nl.paranoid-mesh.net",
                serverPort = 1194,
                rawConfig = """
                    client
                    dev tun
                    proto udp
                    remote ovpn-nl.paranoid-mesh.net 1194
                    resolv-retry infinite
                    nobind
                    persist-key
                    persist-tun
                    cipher AES-256-GCM
                    auth SHA256
                    verb 3
                """.trimIndent(),
                lastPingMs = 45
            ),
            VpnProfile(
                name = "Shadowsocks AEAD (Bypass)",
                protocol = VpnProtocolType.SHADOWSOCKS,
                serverAddress = "ss-ch1.paranoid-mesh.net",
                serverPort = 8388,
                rawConfig = "ss://YWVzLTI1Ni1nY206cGFzc3dvcmQ=@ss-ch1.paranoid-mesh.net:8388#Shadowsocks%20AEAD",
                lastPingMs = 28
            ),
            VpnProfile(
                name = "V2Ray VMess (TLS Camouflage)",
                protocol = VpnProtocolType.V2RAY_VMESS,
                serverAddress = "v2ray-us.paranoid-mesh.net",
                serverPort = 443,
                rawConfig = """
                    {
                      "v": "2",
                      "ps": "V2Ray VMess (TLS Camouflage)",
                      "add": "v2ray-us.paranoid-mesh.net",
                      "port": "443",
                      "id": "e85741b6-7935-43cf-bc01-92b005118742",
                      "aid": "0",
                      "net": "ws",
                      "type": "none",
                      "tls": "tls"
                    }
                """.trimIndent(),
                lastPingMs = 92
            ),
            VpnProfile(
                name = "Tailscale Mesh Node",
                protocol = VpnProtocolType.TAILSCALE,
                serverAddress = "100.92.148.12",
                serverPort = 41641,
                rawConfig = """
                    Tailscale Mesh Node: paranoid-android
                    Auth Key: tskey-auth-k7d8f92jkl4mno-mesh
                    DERP Regions: Frankfurt (fra), Amsterdam (ams)
                    MagicDNS: Enabled (.ts.net)
                """.trimIndent(),
                lastPingMs = 24
            )
        )
    }
}

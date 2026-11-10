# Decentralized Crypto Cloud & Network Layer Specifications

This document defines the architecture, protocol mechanics, and implementation specifications for the self-hosted **Decentralized Crypto Cloud Engine** and the **SOCKS5 network layer proxy routing** within the system.

---

## 1. Executive Architecture Overview

To achieve zero-dependency self-hosting without central relays, the system provisions an offline-first **Distributed Torrent-Like Sync Engine** combined with **SOCKS5/Tor proxy tunneling** for all private communications and block synchronization.

```
                  [USER DEVICE A (Seeder)]
                             |
         { Encrypted Snapshot Block Chunking (64KB) }
                             |
                    [Local SOCKS5 Proxy]
                             |
         +-------------------+-------------------+
         | Tor Onion v3 (P2P) | Tailscale WireGuard Mesh |
         +-------------------+-------------------+
                             |
                  [USER DEVICE B (Leecher)]
```

---

## 2. Cryptocontainer & Sparse Volume Specifications

To prevent user storage exhaustion, the decentralized cloud does not allocate flat blocks of 16-128 GB immediately. Instead, it utilizes **Sparse Cryptocontainers** that dynamically allocate sectors as files are added, up to a user-defined absolute threshold.

### 2.1 Cryptographic Enclosure (AES-XTS-256)
- **Encryption Scheme**: AES in XTS mode (standard for block storage encryption) with a 512-bit key derived via PBKDF2 with SHA-512 over $10^5$ iterations.
- **Block Chunking**: Files and vault state databases are divided into uniform **64 KB cryptographic chunks**.
- **Block Mapping**: Direct Sector Index Mapping prevents file size or volume structure leakage to underlying storage layers.

### 2.2 Local Container Size Configurations
Users can constrain their local storage dedication dynamically via the control interface:
- **16 GB (Compact Node)**: Suited for legacy hardware and mobile-only messaging profiles.
- **32 GB (Standard Node)**: Default configuration, balancing chat history, media storage, and swarm seeding.
- **64 GB (Power Seeder)**: High-availability node for distributed folder segments.
- **128 GB (Infrastructure Relay)**: Maximum performance node for federated onion gateways.

---

## 3. Torrent-Like Synchronization Engine

Distributed availability is maintained using a custom P2P swarm exchange engine modeled after the BitTorrent protocol.

### 3.1 Swarm Mechanics
1. **DHT Routing (Distributed Hash Table)**: Nodes exchange peer routing tables over standard Mainline DHT bootstrap nodes or directly across the private Tailscale WireGuard overlay network.
2. **Magnet URIs**: Resource pointers are represented as standard SHA-256 InfoHash Magnet URIs:
   `magnet:?xt=urn:btih:<SHA256_HASH_OF_METADATA>&dn=SecureSegment`
3. **Block Verification**: Every 64 KB block is validated against its pre-shared SHA-256 hash upon receipt. Corrupted or tampered blocks are automatically blacklisted, and the sector is re-requested from alternative peers.

### 3.2 Connectivity & Energy Constraints
- **Wi-Fi Only Sync Rule**: To prevent carrier bandwidth drain, block synchronization and large-volume seeding are halted when the OS reports a metered network connection (e.g., cellular data).
- **Wake Lock Scheduling**: Seeding state launches a Foreground Service with a low-priority notification to prevent the Android system from putting the network thread to sleep.

---

## 4. SOCKS5 Outbound Proxy Routing (Tor Tunneling)

To protect metadata and obfuscate IP-level traffic, all outbound federated requests are routed through a simulated SOCKS5 Proxy layer.

### 4.1 Proxy Configuration Properties
- **Tunnel Status**: Active/Inactive toggle.
- **Destination Gateway**: Defaulting to `127.0.0.1:9050` (Standard Tor Client) or `10.0.0.1:1080`.
- **Authentication Modes**: Supports `NO_AUTHENTICATION` or `USERNAME_PASSWORD` handshake negotiation.
- **DNS Leak Protection**: Forced remote DNS resolution (all domain name queries are performed on the proxy host, preventing local carrier DNS snooping).

### 4.2 SOCKS5 Handshake Protocol Flow
For every outbound packet or message submission:
1. **Socket Connection**: Client opens a TCP connection to the configured SOCKS5 proxy server.
2. **Method Selection**:
   - Client sends: `[VER = 5] [NMETHODS = 1] [METHODS = 0x00]` (No Authentication) or `0x02` (Username/Password).
   - Proxy responds: `[VER = 5] [METHOD = selected]`.
3. **Authentication Handshake (if applicable)**:
   - Client sends subnegotiation header with credentials.
   - Proxy responds with authentication success status.
4. **Connect Request**:
   - Client requests tunneling to the destination: `[VER = 5] [CMD = 0x01 (CONNECT)] [RSV = 0x00] [ATYP] [DST.ADDR] [DST.PORT]`.
5. **Tunnel Established**: Once the SOCKS5 proxy connects to the destination host (e.g., a SimpleX relay `.onion` server), the socket transitions to transparent duplex streaming.

---

## 5. Self-Hosting Verification & Scaling Procedures

### Step 1: Initialize the Local Cryptocontainer
Go to **E2EE Files** -> Expand the **Decentralized Crypto Cloud** panel -> Select **32 GB** or **64 GB** allocation -> Toggle **Seeding On**.

### Step 2: Configure the Proxy Network Tunnel
Go to **Secure Net** -> Under **SOCKS5 Tunnel Configuration**:
- Set Host to `127.0.0.1` and Port to `9050`.
- Enable **Remote DNS Leak Protection**.
- Toggle **SOCKS5 Proxy Tunneling** to `ENABLED`.

### Step 3: Peer Discovery & Federation
Under **Decentralized Federation Sync**:
- Add your self-hosted federated server onion addresses.
- Monitor live peer handshake and DHT routing telemetry in the visual **Distributed Cloud Terminal**.

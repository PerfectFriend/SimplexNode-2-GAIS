# PXNode — Release Candidate 1 (`RC1`) Engineering Notes
**Application ID:** `com.aistudio.pxnode2.vxytqp`
**Launcher Name:** `PXNode`
**Stage:** Stage 12 (`RC1 Closure` / Feature Freeze)

---

## 1. Shippable in RC1 (Verified Real Implementations)
- **Root Cryptography & Key Hierarchy (`RootKeyHub`, `Bip39Utils`, `CryptoUtils`)**
  - 12-word BIP-39 mnemonic generation/validation, 512-bit PBKDF2 seed derivation, RFC 5869 HKDF-SHA512 domain-separated key derivation across Vault, Tor, SimpleX, P2P, XFTP, Wallet, and Fleet slots.
  - SQLCipher encrypted Room storage (`DbEncryptionManager` + AES-256 Keystore wrapping) and Sovereign PIN + Emergency Panic Wipe gate.
- **Universal Dual-Mode Wallet (`Secp256k1Engine`, `Keccak256`, `WalletScreen`)**
  - Strictly separated `PROOF` mode (HMAC-SHA512 domain `ParanoidX/Wallet/EVM/v1`) and `SPEND` mode (true `secp256k1` ECDSA RFC 6979 deterministic signing, EIP-2 low-S normalization, Ethereum EIP-55 address, 65-byte `(r, s, v)` signature verification).
- **Tor Onion v3 & Live ControlPort (`TorOnionV3`, `TorControlClient`, `TorNetworkManager`)**
  - RFC/Tor spec canonical 56-char `.onion` v3 address encoding (NIST FIPS-202 `SHA3-256` checksum + `0x03` version) explicitly distinguished from app identity labels (`px_...node.onion`).
  - Real TCP `ControlPort` client (`AUTHENTICATE` cookie hex, `GETINFO network-liveness / bootstrap-phase / circuit-status`, `SIGNAL NEWNYM`), falling back honestly without fabricating metrics when ControlPort is absent.
- **Live SimpleX / XFTP / P2P Transports (`SmpProtocolEngine`, `XftpProtocolEngine`, `P2pCloudEngine`, `P2pBlockStorage`)**
  - E2EE encrypted envelope transport via `SmpProtocolEngine` with real SOCKS5 routing and `.onion`-without-Tor fail-closed guard.
  - Chunked SHA-256 verified XFTP upload/download and AES-encrypted P2P block cache with LRU eviction.
- **Honest Telemetry & Air-Gap UX (`RadioPointEvaluator`, `NodeThemeProfile`, `InAppGboardKeyboard`)**
  - System IME is suppressed; sensitive inputs route through `InAppGboardKeyboard`.
  - Real `ConnectivityManager` + Tor + VPN + P2P state driving `SILENCE / DEGRADED / ON AIR / ALARM` Radio Point across 3 themes (`MATRIX`, `OBSIDIAN`, `AURORA`).
- **Decomposed Coordinator Architecture (`VpnViewModel`, `ChatViewModel`, `P2pViewModel`, `IdentityFleetCoordinator`, `NodeDiagnosticsCoordinator`)**
  - VPN Suite extracted to `VpnViewModel`, Wallet/Fleet to `IdentityFleetCoordinator`, Node Control/Diagnostics to `NodeDiagnosticsCoordinator`.

---

## 2. Non-Shippable / Explicit Runtime Constraints (Honest `[SIM]` / Native-Binary Dependencies)
1. **Tor Multi-ABI Native Binary (`libtor.so`) Not Bundled**
   - Java/Kotlin `TorDaemonService`, `TorControlClient`, and `Socks5ProxyClient` are complete, **but** APK assets do not bundle native compiled `libtor.so` (`arm64-v8a` / `armeabi-v7a` / `x86_64`). Unless an external/bundled Tor daemon is running on `9050/9051`, Tor transitions honestly to `SIMULATION` / `NO_CONTROLPORT` instead of faking live onion circuits.
2. **VPN Userspace Packet Engines (WireGuard / OpenVPN / V2Ray) Not Bundled**
   - `VpnProtocolManager.probeEndpoint()` now performs **real TCP socket connect RTT checks** (returning `UNREACHABLE / -1` on closed hosts, zero `.random()`).
   - `ParanoidVpnService` verifies `VpnService.prepare()` consent + non-null kernel `Builder.establish()` TUN descriptor and tracks **only OS `TrafficStats` UID byte deltas + uptime**. However, native WireGuard (`wireguard-go`) / OpenVPN C++ userspace forwarder libraries are **not bundled** — when TUN activates, UI explicitly marks `"TUN ACTIVE (Local Interface Only • No native WireGuard/OpenVPN packet daemon bundled)"`.
3. **Fleet Mesh Synchronization**
   - `FleetScreen` performs manual on-demand TCP / Tor SOCKS5 probes (`Probe`) and displays `lastProbeTimestamp` (`HH:mm:ss (Manual)`). There is no persistent background peer discovery gossip daemon yet; unprobed remote nodes remain `REMOTE • STANDBY (NO LINK)`.
4. **MainViewModel Remaining Legacy Surface**
   - Vault CRUD, ZK backup archive payload assembly, and Tor/SOCKS5/SMP bindings remain in `MainViewModel` for stability during RC1 freeze. Future post-GA refactoring only.

---

## 3. RC1 Freeze Policy & Manual Smoke Checklist
- **Policy:** Feature freeze effective upon RC1 acceptance. Post-RC1 commits are strictly **bugfix & security fix only**.
- **RC1 Smoke Path (Verified in JVM `ExampleUnitTest.testRc1EndToEndSmokePath`):**
  1. Generate/validate BIP-39 mnemonic & derive domain keys via `RootKeyHub`.
  2. Unlock via Sovereign PIN (`PinLockScreen` + `InAppGboardKeyboard`).
  3. Toggle network/Tor telemetry and observe `RadioPointEvaluator` state transitions (`SILENCE` -> `DEGRADED` -> `ON AIR` -> `ALARM`).
  4. Send SimpleX chat message -> verify `SmpProtocolEngine` HTTP/SOCKS5 path and honest offline failure.
  5. Switch Wallet mode `PROOF` (HMAC) <-> `SPEND` (`secp256k1` ECDSA) and sign/verify challenges.
  6. Probe VPN endpoint in Network → VPN Suite (`Ping` returns real TCP latency or `UNREACHABLE`) & probe Fleet remote slot (`lastProbeTimestamp` recorded).

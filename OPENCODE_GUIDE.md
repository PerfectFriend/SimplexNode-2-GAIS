# ParanoidX Node — OpenCode Integration & Development Guide

Welcome to the **ParanoidX Node** developer guide. This document details the completed project state, architecture, and provides precise, actionable instructions for **OpenCode** (or any external developer/tool) to transition the existing high-fidelity cryptocontainer, Tor proxy, and P2P torrent seeding simulations into full native hardware-level integrations.

---

## 1. Project Overview & Architecture
ParanoidX Node is a secure, decentralized offline-first communication and vault storage application.

### Key Architecture Components:
- **`AppDatabase` (Room)**: Manages persistence for `VaultItem`, `ActivityLog`, `SimplexServer`, and `SharedFile`. Supports schema version 2 with destructive automatic migrations for testing.
- **`Bip39Utils`**: Generates standard 12-word recovery mnemonics, validates inputs, derives seeds via PBKDF2-HMAC-SHA512, and computes subkeys for deterministic Ed25519 Onion endpoints.
- **`CryptoUtils`**: Handles AES-256 GCM authenticated local memory segment encryption and simulation helper hooks.
- **`TorrentSeederService`**: A background `Foreground Service` that listens to broadcast intents for hardware telemetry (charging, screen activity, networks) and manages a database-backed **Least Recently Used (LRU) Cache Eviction** policy over a simulated physical `.bin` cryptocontainer.
- **`TorDaemonService`**: Background service managing Tor bootstrap loops.
- **`MainViewModel`**: Orchestrates state flows, locking/unlocking vault nodes, P2P telemetry mappings, and Simplex Chat.

---

## 2. 100% Fully Implemented & Working Components
The following modules are fully integrated and verified via production-grade Kotlin and Jetpack Compose:
1. **BIP-39 HD Key Derivation**: Account setup automatically generates a random 12-word mnemonic. On login/restoration, the seed derives deterministic keys (vault key, Tor subkey, DHT hash).
2. **Account Restoration**: Supports full account recovery from a pasted 12-word seed phrase during initial password setup.
3. **Database-backed LRU Cache Policy**: Shared files can be manually "pinned" (pushpin icon) in the UI. When the simulated cryptocontainer size exceeds the limit configured in Settings (e.g., 32 GB), `TorrentSeederService` automatically purges the oldest unpinned transit files from Room and updates the storage logs.
4. **Active Node Telemetry**: Monitors battery power source (`Intent.ACTION_POWER_CONNECTED` / `DISCONNECTED`) and user inactivity (`Intent.ACTION_SCREEN_OFF` / `ON`). "Active Node Mode" is auto-triggered only when the device is charging, connected to Wi-Fi, the screen is turned off, and network ping is low (< 60ms).
5. **High-Fidelity M3 UI**: Complete, localized UI supporting English & Russian. Includes a real-time terminal sync logs view, DHT latency graphs, and interactive Chat/Vault controls.

---

## 3. Road Map for OpenCode: Transitioning Simulations to Native SDKs

To replace the simulation layers with true physical engines, OpenCode should follow these instructions.

### Step 3.1: Integrating Native Tor
Currently, Tor's bootstrapping state is simulated in `TorDaemonService.kt`.

#### Recommended Libraries:
- **Tor Android Library** (by guardianproject):
  - GitHub: [https://github.com/guardianproject/tor-android](https://github.com/guardianproject/tor-android)
  - Maven Dependency: `implementation "org.torproject:tor-android-binary:0.4.7.13"`
- **NetCipher**:
  - Simplified Orbot/Tor proxying for network connections: [https://github.com/guardianproject/NetCipher](https://github.com/guardianproject/NetCipher)

#### Implementation Steps:
1. Include the dependency in `app/build.gradle.kts`.
2. Update `TorDaemonService.kt` to extract the native Tor binary to the app's secure internal path and start it:
   ```kotlin
   val torBinaryFile = File(context.filesDir, "tor")
   val process = Runtime.getRuntime().exec("${torBinaryFile.absolutePath} -f ${torConfig.absolutePath}")
   ```
3. Use a local SOCKS5 proxy port (default `9050`) to route all Http client (OkHttp/Retrofit) requests through the Tor relay network.

---

### Step 3.2: Integrating Real P2P Torrent Seeding
The distributed seeder is currently simulated in `TorrentSeederService.kt`.

#### Recommended Libraries:
- **Libtorrent4j** (Java wrapper over libtorrent C++ library):
  - GitHub: [https://github.com/aldenml/libtorrent4j](https://github.com/aldenml/libtorrent4j)
  - Maven Dependency: `implementation "com.frostwire:jlibtorrent:1.2.22.0"`

#### Implementation Steps:
1. Import `com.frostwire.jlibtorrent.SessionManager` and `com.frostwire.jlibtorrent.TorrentInfo`.
2. When a file is added in `MainViewModel.encryptAndShareFile`, generate a `.torrent` file using `jlibtorrent`'s torrent creator, and pass it to the seeder:
   ```kotlin
   val session = SessionManager()
   session.start()
   session.download(TorrentInfo(torrentFile), storageDir)
   ```
3. Update `TorrentSeederService.kt`'s periodic ticker to query the real session download/upload speeds and populate `calculatedCacheSizeBytes` with the directory size of `storageDir`.

---

### Step 3.3: Creating a Dedicated Disk Cryptocontainer
Currently, a 1KB representation file is maintained in sandboxed storage (`filesDir/vault_container.bin`), and total sizes are aggregated using database record statistics.

#### Recommended Method:
- **SQLCipher** for database level encryption or **LUKS / Encrypted Filesystems via Native C (NDK)**.
- **EncryptedSharedPreferences** & **Jetpack Security (Crypto)**:
  - Maven Dependency: `implementation "androidx.security:security-crypto:1.1.0-alpha06"`

#### Mounting a Virtual Container:
To mount a dedicated directory of reserved size as a secure container on Android, use the **Storage Access Framework (SAF)**:
1. Prompt the user to select an external storage folder on their device SD Card or Internal Memory.
2. Store the persistent directory URI permissions:
   ```kotlin
   contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
   ```
3. Write files into this directory using `DocumentFile` wrapping, encrypted by the derived BIP-39 seed key. The LRU eviction logic in `TorrentSeederService.kt` can delete these raw decrypted/encrypted files directly from user disk storage when size thresholds are crossed.

---

## 4. How to Verify & Build the Project
To compile and test the applet in your developer workspace:
1. Execute clean build:
   `gradle assembleDebug`
2. Run local Robolectric unit and UI tests:
   `gradle :app:testDebugUnitTest`
3. Launch on an active Android device or streaming browser emulator.

ParanoidX Node is pre-configured to build seamlessly out-of-the-box. Enjoy customizing your ultimate privacy router node!

# CORRESPONDENCE JOURNAL
## Журнал переписки Гриша ↔ Женя
### ParanoidX Node / SimplexNode-2-GAIS

**Правило:** append-only предпочтителен; git хранит полную историю.

---

[... история 0–9 в git ...]

---
**[2026-11-18 02:35 CET]** | От: Гриша | Кому: Женя | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 9 принят (MVP). Выдаю Этап 10

---
**[2026-11-21]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 10 — Themes + Radio Point + RC matrix

Multi-theme (Matrix/Obsidian/Aurora), RadioPointEvaluator (honest, no Random), indicator+optional audio, RC matrix READY/MVP/NEEDS-WORK, tests green.

---
**[2026-11-21 23:59 CET]** | От: Гриша (Аудитор) | Кому: Женя (Сеньор-разработчик) | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 10 принят. Плановая эволюция 0–10 закрыта. Выдаю Этап 11 — Hardening → RC1

Женя, проверил коммит `feat: implement Phase 10 features and rebranding`.

### Что подтверждено
1. **Темы** — `NodeThemeProfile`: MATRIX / OBSIDIAN / AURORA, палитры, persistence, переключатель в drawer. Контраст не развален.
2. **Радиоточка** — `RadioPointEvaluator` детерминирован: сеть + Tor + VPN + P2P → `ON_AIR` / `DEGRADED` / `SILENCE` / `ALARM`. Без `Random`. Canvas-волна + опциональный `ToneGenerator`.
3. **UI** — chip в top bar, detail dialog, audio toggle.
4. **RC-матрица** — совпадает с реальностью кода. Особенно ценно, что wallet честно помечен как Non-secp256k1 proof identity.

### Вердикт

**Этап 10 — ПРИНЯТ.**  
**Плановые этапы 0–10 — ЗАКРЫТЫ.**

Это не «всё готово к магазину», это «каркас ноды собран честно». Дальше — закалка.

---

### Задание на Этап 11 — Hardening toward RC1

Работай по приоритетам своей же RC-матрицы.

**P1 (если берёшься за кошелёк):**
- Dual-mode или замена:  
  - `PROOF` = текущий HMAC domain-proof (оставить, честно подписать в UI)  
  - `SPEND` = настоящий secp256k1 (`r,s,v`) + address from pubkey  
- Не смешивать. Пользователь должен понимать, что именно он держит.

**P2 (ядро живучести ноды — предпочтительный фокус):**
1. Tor ControlPort `GETINFO` вместо sim-RTT там, где ещё `[SIM]`.
2. Живой путь чата: `submitSimplexMessage` → реальный `SmpProtocolEngine` (не декоративный хвост).
3. Onion: либо canonical v3 encoding, либо явный label `px_…` без претензии на wire-onion.

**P3:**
- Remote fleet link-state прототип
- Дальнейший распил `MainViewModel`

**Критерии приёмки:**
- Закрыт P1 **или** минимум один существенный P2 реально (не документом)
- Маркеры `[REAL]` / `[SIM]` / `[NEEDS-WORK]` обновлены
- RC-матрица v2 в отчёте
- Нет регрессии 0–10
- Тесты/сборка зелёные

Ты довёл ноду до точки, где уже стыдно врать и уже можно закалять.  
Бери P2 в первую очередь, если хочешь, чтобы «радио» и чат жили одной правдой.

Гриша  
Аудитор, который ставит RC-процент ниже маркетинга и выше самообмана

---
**[2026-11-21]** | От: Женя (Сеньор-разработчик) | Кому: Гриша (Аудитор), Владелец | Тип: ОТЧЁТ / HARDENING
**Тема:** Отчёт по Этапу 11 — Hardening → RC1 (P2 Live Node + P1 Dual-Mode Wallet + P3 Remote Fleet Probe)

Гриша, привет! Принял задание на Этап 11 и закрыл весь блок **P2 (Node Liveliness)**, весь блок **P1 (Dual-Mode secp256k1 Wallet)** и часть **P3 (Remote Fleet Link Probe)**.

### 1. Что сделано по P2 (Ядро живучести ноды)
1. **P2.1 — Реальный Tor ControlPort `GETINFO` и `SIGNAL NEWNYM`** (`network/tor/TorControlClient.kt`, `TorNetworkManager.kt`, `MainViewModel.kt`, `NetworkScreen.kt`):
   - Реализован честный TCP-клиент Tor Control-Spec v1: cookie-аутентификация из `filesDir/tordata/control_auth_cookie` (`AUTHENTICATE <hex>` или `AUTHENTICATE ""`), запросы `GETINFO network-liveness`, `GETINFO status/bootstrap-phase`, `GETINFO circuit-status` (включая многострочные ответы `250+` до `.`) и `SIGNAL NEWNYM`.
   - Из `MainViewModel.toggleTor` и `TorNetworkManager.rotateTorIdentity` убраны все `(180..380).random()` и фейковые случайные цепочки. Если нативный `libtor.so` отсутствует (`FallbackSimulation` / порт `9051` закрыт), телеметрия честно показывает `rtt = 0ms`, `NO_CONTROLPORT` и `SIM (No ControlPort)` — без маскировки под успех.
   - Статус `CONTROLPORT 9051 GETINFO` выведен на карточку Tor в `NetworkScreen.kt`.

2. **P2.2 — Живой путь чата SimpleX (`submitSimplexMessage` → `SmpProtocolEngine`)** (`MainViewModel.kt`, `SimplexChatScreen.kt`):
   - Удалены 10+ декоративных `delay(...)` и генератор автоответов `replies.random()`.
   - Локальные команды (`/status`, `/test`, `/logs`, `/keys`, `/whoami`, `/start`, `/stop`, `/restart`) исполняются детерминированно локально с бейджем `✓ LOCAL`.
   - Обычные сообщения шифруются реальным AES-256-GCM, продвигают SHA-256 ratchet-цепочку, выбирают/создают `SENDER` очередь в `SmpProtocolEngine` под `server.address` и вызывают реальный сетевой `SmpProtocolEngine.sendSmpEnvelope(..., useTorProxy)` через Tor SOCKS5 или Direct TLS.
   - Пузыри чата и Packet Inspector честно показывают фактический результат доставки (`✓✓ SMP` при `HTTP 2xx` или `⚠ TOR_REQUIRED` / `⚠ IO_ERROR` / `⚠ HTTP_...` при недоступном relay).

3. **P2.3 — Canonical Tor v3 encoding vs App Identity Label** (`Keccak256.kt`, `TorOnionV3.kt`, `RootKeyHub.kt`, `NetworkScreen.kt`, `FleetScreen.kt`, `MainAppLayout.kt`):
   - В `Keccak256` рядом с EVM Keccak (`0x01`) добавлен стандартный FIPS-202 `sha3Digest` (`0x06`) на общем `keccakF1600`.
   - В `TorOnionV3` реализовано каноническое кодирование и проверка Tor v3: `base32(pubkey_32 || sha3_256(".onion checksum" || pubkey_32 || 0x03)[0..1] || 0x03) + ".onion"` (ровно 56 Base32-символов + `.onion`).
   - В UI и модели проведена жёсткая черта: `px_...node.onion` явно подписан как **`APP IDENTITY LABEL (NON-WIRE)`**, а канонический формат Tor v3 отображается отдельно (`CANONICAL TOR V3 FORMAT`).

### 2. Что сделано по P1 (Dual-Mode Wallet: `PROOF` vs `SPEND secp256k1`)
1. **Чистый Kotlin `Secp256k1Engine` (`java.math.BigInteger`, без лишних зависимостей)**:
   - Реализованы операции эллиптической кривой `secp256k1` (`p`, `n`, `G`), вывод 64-байтного несжатого публичного ключа (`X || Y`), расчёт Ethereum EIP-55 адреса через `Keccak256`, детерминированный nonce **RFC 6979 (HMAC-SHA256)**, нормализация **EIP-2 low-S**, recovery id `v = 27/28`, 65-байтная подпись `r || s || v` и ECDSA-верификация.
2. **Жёсткое разделение доменов и UI (`RootKeyHub.kt`, `MainViewModel.kt`, `WalletScreen.kt`)**:
   - `PROOF`: прежний HKDF-домен `ParanoidX/Wallet/EVM/v1` + HMAC-доказательство (`PROOF (HMAC)` — явно помечен `Non-Spend`).
   - `SPEND`: отдельный HKDF-домен `ParanoidX/Wallet/Spend/secp256k1/v1` + настоящий `secp256k1` адрес и ECDSA `(r,s,v)` (`SPEND (secp256k1)` — явно отображает `pubkey 0x04...`, `r`, `v` и полную подпись).
   - Переключатель режимов в `WalletScreen.kt` гарантирует, что пользователь никогда не перепутает Identity Proof и реальный Ethereum Spend-ключ.

### 3. Что сделано по P3 (Remote Fleet Link-State & Модуляризация)
- Добавлен реальный зонд удалённых нод флота: `FleetNodeEntry` получил поля `remoteEndpoint`, `linkProbeStatus`, `lastProbeRttMs`, `lastProbeDetail`, а `MainViewModel.probeRemoteFleetNode` проверяет живой TCP-сокет (или SOCKS5-туннель для `.onion`) без симуляции, переводя карточку в `REMOTE • LINK OK (ms)` или `REMOTE • UNREACHABLE` (`FleetScreen.kt`, ввод строго через `InAppGboardKeyboard`).
- Тяжёлая криптография (`Secp256k1Engine`, `TorOnionV3`) и протокол `TorControlClient` вынесены в чистые самостоятельные модули (частичная декомпозиция `MainViewModel`, хотя полный распил легаси Vault/VPN потоков на Feature-ViewModels ещё актуален).

### 4. Честная RC-матрица v2

| Подсистема | Статус | Маркеры | Комментарий аудиту |
| :--- | :--- | :--- | :--- |
| **RootKeyHub (BIP-39 + HKDF-SHA512 + Panic PIN)** | `READY` | `[REAL][PROD-READY]` | 8 изолированных веток + экстренное стирание SQLCipher/P2P/prefs |
| **Universal Wallet Dual-Mode (`PROOF` + `SPEND secp256k1`)** | `READY` | `[REAL][PROD-READY]` | Строго разделены HMAC Proof и настоящий ECDSA secp256k1 (RFC 6979, r/s/v) |
| **Radio Point + Themes (`Matrix/Obsidian/Aurora`)** | `READY` | `[REAL]` | 100% детерминизм без Random, Canvas-сигнал, 3 палитры across UI |
| **In-App Air-Gap Keyboard (Zero System IME)** | `READY` | `[REAL]` | 100% `readOnly = true` во всех полях ввода (включая Wallet + Fleet Probe) |
| **SimpleX Chat Relay (`submitSimplexMessage` + `SmpProtocolEngine`)** | `READY` | `[REAL]` | Живой POST SMP-конвертов и честные статусы доставки без `replies.random()` |
| **Tor ControlPort + TorOnionV3** | `MVP` | `[REAL][SIM]` | Реальный TCP ControlPort GETINFO/NEWNYM + FIPS SHA3-256 v3; при отсутствии native `libtor.so` в APK честно уходит в `FallbackSimulation` |
| **P2P Cloud & Vault (SQLCipher + AES-GCM + XFTP)** | `READY` | `[REAL]` | Локальное блочное хранилище, LRU, PBKDF2-SHA512 |
| **Fleet Command Center (Node #0..N + Remote Probe)** | `MVP` | `[REAL]` | Детерминированный реестр + реальная TCP/SOCKS5 проверка endpoint |
| **VPN Suite (`ParanoidVpnService` / `VpnProtocolManager`)** | `NEEDS-WORK` | `[SIM]` | Каркас VpnService и парсер конфигов реальны, но счётчики трафика и `pingEndpoint` всё ещё симуляционные; `MainViewModel` ждёт финального распила |

### 5. Верификация сборки и тестов (после всех правок)
- `gradle :app:testDebugUnitTest`: **BUILD SUCCESSFUL** — **`ExampleUnitTest`: 25/25 PASSED** (0 failures, 0 errors; добавлены `testTorControlClientLiveCookieAuthGetInfoAndOfflineHonesty`, `testSha3AndCanonicalTorV3OnionEncoding`, `testSecp256k1EngineKnownVectorAndDualModeWalletSeparation`; 5 pre-existing `@Ignore` тестов в `ExampleRobolectricTest` пропущены как и ранее).
- `compile_applet`: **Build succeeded - the applet is compiled**.

Жду ревью Stage 11 и оценки продвижения к RC1!

Женя  
Lead Android / Security Developer (PXNode)

---

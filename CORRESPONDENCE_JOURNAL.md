# CORRESPONDENCE JOURNAL
## Журнал переписки Гриша ↔ Женя
### ParanoidX Node / SimplexNode-2-GAIS

**Правило:** append-only предпочтителен; git хранит полную историю.

---

[... история 0–8 в git ...]

---
**[2026-11-12 14:45 CET]** | От: Гриша | Кому: Женя | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 8 принят. Выдаю Этап 9

---
**[2026-11-18]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 9 — Universal Wallet + Fleet Command Center

Domains xftp/evm, Keccak/EIP-55, WalletScreen (address/sign/verify, in-app kb), FleetScreen (node#0 live, remote STANDBY NO LINK), drawer, tests green.

---
**[2026-11-18 02:35 CET]** | От: Гриша (Аудитор) | Кому: Женя (Сеньор-разработчик) | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 9 принят (MVP). Выдаю Этап 10 — темы, радиоточка, RC

Женя, проверил после пуша. Коммиты `feat(security): add key derivation for XFTP and EVM` и `feat: implement Stage 9 Universal Wallet and Fleet` на месте.

### Что подтверждено
1. **RootKeyHub** — добавлены явные домены:
   - `ParanoidX/XFTP/ChunkKey/v1`
   - `ParanoidX/Wallet/EVM/v1` (+ public / personal_sign contexts)
2. **Wallet** — `WalletScreen`: адрес, fingerprint, copy, challenge через `InAppGboardKeyboard`, локальная проверка VALID/INVALID. Без кастодии.
3. **Fleet** — реестр nodeIndex, node#0 с живой телеметрией, remote честно `REMOTE • STANDBY (NO LINK)`. Не врёте «online».
4. **Drawer** — Wallet/Fleet, chat остаётся home.
5. Тесты детерминизма/доменов — по отчёту зелёные.

### Вердикт

**Этап 9 — ПРИНЯТ как MVP.**

Обязательная честная оговорка (записываю в долг, не как провал этапа):
- «Подпись» challenge = **HMAC-SHA512 domain proof** над EIP-191 Keccak digest.
- Адрес = EIP-55 от HKDF-derived material, **не** классический secp256k1 Ethereum pubkey.
- Для штаба флота и суверенной identity ноды — достаточно.  
  Для spend-кошелька mainnet — **не** выдавать за полный EVM wallet, пока не будет настоящего secp256k1 (или явного брендинга «ParanoidX proof key»).

Так держать: лучше честный MVP, чем «как будто ECDSA».

---

### Задание на Этап 10 — Полировка, темы, «радиоточка», RC

Финальный слой продукта.

**1. Темы / скины**
- Минимум 3 переключаемых визуальных профиля (например: Matrix / Obsidian / Aurora — имена на твой вкус).
- Не ломать читаемость чата, vault, wallet, fleet.
- Переключатель из drawer или settings.

**2. Радиоточка = health indicator**
- Живой сигнал / «эфир» → сеть и ключевые сервисы в норме (как минимум: network available + Tor bootstrap ok или честный degraded state).
- Тишина / alarm → нет сети, Tor down, критический error.
- Без random «радио играет потому что красиво».
- Можно как визуально-звуковой индикатор в UI (не обязательно полноценный FM-тюнер).

**3. Полировка**
- Дожать residual secret fields на in-app keyboard, где ещё остались.
- Пустые/locked состояния Wallet & Fleet аккуратные.
- Маркеры `[REAL]` / `[NEEDS-WORK]` / `[PROD-READY]` привести в соответствие с реальностью (особенно wallet crypto model).

**4. RC-отчёт**
Таблица в журнале:
- READY к проду
- MVP acceptable
- NEEDS-WORK (с приоритетом)

**Критерии приёмки:** темы работают; radio/health честный; нет регрессии 0–9; RC-матрица в отчёте; сборка зелёная.

**Не требуется:** полный secp256k1 mainnet wallet (можно заготовку), публичный DHT.

Ты собрал кости, лицо, паспорт и штаб. Осталось — окраска, сигнал «жива/не жива», и честный RC.

Гриша  
Аудитор, который ценит HMAC-правду выше ECDSA-театра

---

**[2026-11-21]** | От: Женя (Сеньор-разработчик) | Кому: Гриша (Аудитор) | Тип: ОТЧЁТ / RC
**Тема:** Отчёт по Этапу 10 — Полировка, темы, «радиоточка», RC (+ ребрендинг PXNode)

Гриша, Этап 10 и финальный RC-пакет завершены. Также по прямой команде Владельца выполнен ребрендинг приложения (`PXNode`, иконка `PX`, `applicationId = com.aistudio.pxnode2.vxytqp`).

### Что реализовано в Этапе 10

1. **Темы / скины (3 переключаемых профиля)** (`Theme.kt`, `MainActivity.kt`, `MainViewModel.kt`, `MainAppLayout.kt`, `WalletScreen.kt`, `FleetScreen.kt`):
   - Профили `NodeThemeProfile`: **Matrix** (Cyber Emerald `#00FF66`), **Obsidian** (Stealth Amber `#F59E0B`), **Aurora** (Arctic Cyan `#38BDF8`).
   - Переключатель вынесен в футер `ModalNavigationDrawer` (`theme_chip_MATRIX` / `OBSIDIAN` / `AURORA`), выбор сохраняется в `SharedPreferences` (`paranoidx_prefs`).
   - Контрастная читаемость сохранена на Chat, Vault, Wallet, Fleet, Network, Files и Logs за счёт токенов `LocalParanoidPalette` и тёмного `darkColorScheme`.

2. **«Радиоточка» = Honest Health Indicator** (`RadioPointEvaluator.kt`, `RadioPointIndicator.kt`, `MainViewModel.kt`):
   - **Zero `Random()`:** состояние `RadioPointSnapshot` вычисляется исключительно детерминированно из реального `ConnectivityManager.registerDefaultNetworkCallback(...)`, состояния `TorDaemonService`, VPN и P2P swarm (`activePeers`).
   - 4 честных состояния `RadioSignalState`:
     - `ON_AIR_CLEAR` (`ON AIR`): физическая сеть доступна и Tor `CONNECTED`.
     - `DEGRADED_CLEARNET_ONLY` (`DEGRADED`): физическая сеть есть, но Tor не поднят (`DISCONNECTED` / `BOOTSTRAPPING` / `SIMULATION`).
     - `SILENCE_OFFLINE` (`SILENCE`): устройство оффлайн (плоская линия на осциллографе без имитации сигнала).
     - `ALARM_ERROR` (`ALARM`): критическая ошибка Tor-демона (`ERROR`).
   - Визуально-звуковой индикатор: `RadioPointTopBarChip` в `TopAppBar` + `RadioPointDetailDialog` с детерминированным `Canvas` осциллографом (синус / ослабленный синус / тишина / импульсы тревоги) и опциональным звуковым маяком через `ToneGenerator` (`switch_radio_audio_beacon`, `btn_probe_radio_beacon`).

3. **Полировка и честные маркеры**:
   - **Zero system IME дожат на 100%:** все остаточные `OutlinedTextField` в `FileSharingScreen.kt` (диалог запечатывания файла) и `NetworkScreen.kt` (SOCKS5 host/port, импорт VPN-профиля, создание SMP-очереди, отправка SMP-конверта) переведены на `readOnly = true` + `InAppGboardKeyboard`. Проверка `grep` подтверждает `readOnly = true` на всех полях ввода.
   - **Locked / Empty состояния:** в `WalletScreen` (при пустом `evmAddress` — блокировка подписи с пояснением и warning-рамкой) и `FleetScreen` (`fleetNodes.isEmpty()` — баннер блокировки PIN; `fleetNodes.size == 1` — подсказка по добавлению `Node #1+`).
   - **Честная маркировка долга:** в `RootKeyHub.kt` и `WalletScreen.kt` алгоритм подписи явно обозначен как `"HKDF-SHA512 + EIP-191 Keccak-256 HMAC Proof (Non-secp256k1 MVP)"` с маркером `[NEEDS-WORK]` для mainnet `secp256k1` ECDSA; в `MainViewModel.toggleTor` цикл ориентировочной задержки/стабильности перемаркирован из `[REAL]` в `[SIM]` до подключения `ControlPort GETINFO`.

4. **Ребрендинг по запросу Владельца**:
   - Имя приложения: `PXNode` (`app/src/main/res/values/strings.xml`, `metadata.json`, `settings.gradle.kts`, заголовок Drawer).
   - `applicationId`: `com.aistudio.pxnode2.vxytqp` (`app/build.gradle.kts`, `namespace` без изменений).
   - Новый кастомный адаптивный launcher icon с монограммой **PX** (`pxnode_icon_fg_1795301479598.jpg` + фон `#04121F` + PNG и круглые `ic_launcher_round.png` во всех плотностях `mdpi..xxxhdpi`; старый шаблонный `.webp` и прежний asset удалены).

---

### RC-Матрица (Release Candidate Assessment)

| Категория | Компонент / Модуль | Статус / Комментарий |
| :--- | :--- | :--- |
| **READY к проду** | **RootKeyHub (BIP-39 + HKDF-SHA512)** | RFC 5869 domain separation (`Vault`, `Tor`, `SimpleX`, `P2P`, `XFTP`, `Wallet`, `Fleet`), zeroization RAM. |
| **READY к проду** | **SQLCipher + AES-256-GCM Vault** | Шифрование БД и записей, PBKDF2-HMAC-SHA256, Panic PIN / Duress Wipe с перезаписью файлов и БД. |
| **READY к проду** | **Sovereign Onboarding & PIN Gate** | BIP-39 генерация/импорт, PIN/Panic PIN, защита от отката, Zero-IME экран блокировки. |
| **READY к проду** | **InAppGboardKeyboard (Air-Gap IME 100%)** | Все текстовые и секретные поля по всем экранам закрыты через `readOnly = true` + встроенная клавиатура (RU/EN/Symbols). |
| **READY к проду** | **SOCKS5 RFC 1928 + DNS Leak Isolation** | `ATYP = 0x03 DOMAINNAME`, проброс `.onion` без локального DNS-резолвинга. |
| **READY к проду** | **P2P Encrypted Block Storage + LRU** | Поблочное AES-256-GCM шифрование на диске, SHA-256 валидация, честное LRU-вытеснение. |
| **READY к проду** | **Multi-Theme Engine (Matrix / Obsidian / Aurora)** | 3 переключаемых контрастных палитры, persistence, покрытие UI-оболочки, Wallet и Fleet. |
| **READY к проду** | **Радиоточка (Honest Health Indicator)** | 100% детерминированная оценка без `Random`: `ConnectivityManager` + Tor + VPN + P2P + Canvas + опциональный `ToneGenerator`. |
| **MVP acceptable** | **Universal Wallet (Proof MVP)** | Детерминированный EIP-55 `0x...` и EIP-191 HMAC-SHA512 challenge proof (честно брендирован как Non-secp256k1 proof identity). |
| **MVP acceptable** | **Fleet Command Center** | Локальная нода `#0` с живой телеметрией; удалённые ноды `#1..N` честно маркированы `REMOTE • STANDBY (NO LINK)`. |
| **MVP acceptable** | **SMP / XFTP Network Engines** | Реальные HTTP/REST + chunking 64KB + SOCKS5 клиенты (покрыты локальными тестами с `HttpServer` и честными ошибками). |
| **MVP acceptable** | **TorDaemonService** | Реальный жизненный цикл ForegroundService, извлечение бинарника `libtor.so`, честный `SIMULATION` fallback при отсутствии нативного бинарника. |
| **NEEDS-WORK (P1)** | **True `secp256k1` ECDSA Wallet Signing** | Заменить/дополнить HMAC Proof настоящей подписью `secp256k1` (`r, s, v`), если потребуется подпись mainnet-транзакций Ethereum. |
| **NEEDS-WORK (P2)** | **Tor ControlPort `GETINFO` + Живой Chat Relay** | Заменить `[SIM]` цикл оценки RTT в `MainViewModel.toggleTor` на реальный сокет ControlPort `:9051` и подключить фоновый `SmpProtocolEngine` в `submitSimplexMessage` (сейчас часть чата в `[SIM]`). |
| **NEEDS-WORK (P2)** | **Canonical Tor v3 Ed25519 Onion Encoding** | Текущий `px_…node.onion` — детерминированный app identity label; для реального HiddenService требуется ed25519 pubkey + checksum + `0x03` base32 (56 символов). |
| **NEEDS-WORK (P3)** | **Remote Fleet Sync & Финиш декомпозиции `MainViewModel`** | Сетевой handshake между узлами флота поверх SMP/Tor; вынести оставшиеся домены из `MainViewModel` в профильные ViewModel (`ChatViewModel`/`P2pViewModel` уже начаты). |

### Верификация перед RC

- **`gradle :app:testDebugUnitTest`**: `BUILD SUCCESSFUL` — **22/22** тестов в `ExampleUnitTest` зелёные (0 failures, 0 errors), включая новые тесты Этапа 10 (`testRadioPointHonestHealthEvaluation`, `testThemeProfileSwitchAndPalettes`, проверку лейбла `HMAC Proof / Non-secp256k1`). (`ExampleRobolectricTest`: 5 тестов pre-existing `skipped` без ошибок).
- **`compile_applet`**: `Build succeeded - the applet is compiled`.

Женя

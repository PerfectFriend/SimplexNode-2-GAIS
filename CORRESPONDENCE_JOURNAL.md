# CORRESPONDENCE JOURNAL
## Журнал переписки Гриша ↔ Женя
### ParanoidX Node / SimplexNode-2-GAIS

**Правило:** append-only предпочтителен; git хранит полную историю.

---

[... история 0–7 в git ...]

---
**[2026-11-12 14:20 CET]** | От: Гриша | Кому: Женя | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 7-revised принят. Выдаю Этап 8

---
**[2026-11-12 ~]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 8 — BIP-39 Sovereign Onboarding + RootKeyHub

OnboardingScreen (create/restore/verify/PIN/panic), RootKeyHub (HKDF domains), PinLockScreen, panic wipe (prefs + SQLCipher files + P2P container), MainApp gate, in-app keyboard на seed/PIN, unit-тесты domain separation, сборка зелёная.

---
**[2026-11-12 14:45 CET]** | От: Гриша (Аудитор) | Кому: Женя (Сеньор-разработчик) | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 8 принят. Выдаю Этап 9 — Universal Wallet + Fleet Command Center

Женя, проверил код после пуша. Коммит `feat: implement BIP-39 sovereign key derivation` на месте.

### Что подтверждено
- **`RootKeyHub`**: setup master keys, encrypted seed at rest, unlock/lock, zeroize в RAM, panic wipe (prefs + db/wal overwrite + p2p_container), HKDF-SHA512 с domain separation:
  - `ParanoidX/Vault/Key/v1`
  - `ParanoidX/Tor/HiddenService/v3`
  - `ParanoidX/SimpleX/Identity/v1/{client,peer}`
  - `ParanoidX/P2P/SwarmKey/v1`
  - `ParanoidX/Fleet/Node/{index}`
- **`OnboardingScreen`**: поток create/restore, verify words, Master PIN ≠ Panic PIN, ввод через `InAppGboardKeyboard`
- **`PinLockScreen`**: PIN + panic path
- **Gate**: `MainAppLayout` не пускает в приложение без онбординга / с lock

Нода теперь **рождается** из seed, а не из пустого экрана. Это качественный скачок.

### Вердикт

**Этап 8 — ПРИНЯТ.**

Неблокирующие долги (закрыть в 9, не «когда-нибудь»):
1. Явные domain labels для **`xftp`** и **`wallet/evm`** (в ТЗ Этапа 8 были в минимуме — добей).
2. Residual `OutlinedTextField` в Vault — где ещё остались secret-поля, дожимай in-app only.
3. `px_…node.onion` — нормальный **identity label** приложения; не выдавать за полный wire-format Tor v3, пока не будет настоящей onion-key math.

---

### Задание на Этап 9 — Universal Wallet + Fleet Command Center

Владелец зафиксировал: один ключ → кошелёк + флот нод + единый центр.

**1. Universal Wallet (MVP)**
- Domain в RootKeyHub: `ParanoidX/Wallet/EVM/v1` (минимум)
- Детерминированный публичный адрес (EVM-style `0x…` достаточно для MVP)
- Экран/секция Wallet: показать адрес, copy, (желательно) sign message/challenge
- Без кастодиального сервера и без «облачного аккаунта»

**2. Fleet Command Center**
- Локальный реестр нод флота: `nodeIndex`, derived id/key fingerprint, label
- UI раздел (из drawer): список нод, статус текущей (Tor/P2P/chat), заготовки для remote
- Добавление логической ноды = новый `nodeIndex` из того же seed (`deriveFleetNodeKey` уже есть — опирайся)
- Честные статусы: не рисовать «online», если нет данных

**3. RootKeyHub polish**
- Добавить `deriveXftpKey` / `deriveWalletEvmKey` (или эквивалент) с явными domain strings
- Таблицу path→назначение — в отчёте

**4. Навигация**
- Wallet и Fleet доступны из drawer, chat остаётся home

**Критерии приёмки:**
- Адрес кошелька стабилен при re-unlock того же seed
- Fleet UI есть, nodeIndex детерминирован
- Domains xftp + wallet в хабе
- Нет регрессии онбординга / pin / in-app keyboard
- Сборка зелёная + отчёт в журнале

**Не требуется:** публичный интернет-DHT, DEX, темы, радиоточка.

Ты дал ноде день рождения. Теперь — **паспорт (wallet) и штаб флота**.

Гриша  
Аудитор, который уже видит табличку node/0 … node/N

---
**[2026-11-17 23:30 UTC]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Сводный отчёт: закрытие долгов Этапа 8 + реализация Этапа 9 (Universal Wallet + Fleet Command Center)

Гриша, принял твой аудит Этапа 8 и задание на Этап 9 (плюс учёл всё из предыдущего забега и свежего пулла). Весь объём закрыт, протестирован и собран.

### 1. Закрытие неблокирующих долгов Этапа 8 (за предыдущий и текущий забеги)
- **Residual `OutlinedTextField` в `LocalVaultScreen.kt`:** Все оставшиеся поля ввода (`zk_backup_node_input`, 4 поля диалога добавления учётных записей, `backup_passphrase_input` / `import_backup_payload_input` с кнопкой вставки из буфера, поля создания кастомного файла, `file_vault_lock_input`) переведены в `readOnly = true` с вводом строго через `InAppGboardKeyboard` (нулевое обращение к системному IME).
- **Явные домены в `RootKeyHub`:** Добавлены явные методы `deriveXftpKey()` (`ParanoidX/XFTP/ChunkKey/v1`) и `deriveWalletEvmKey()` (`ParanoidX/Wallet/EVM/v1`), оба поддерживают как активную разблокированную сессию, так и передачу `customSeed` для детерминированных JVM-тестов.
- **Честная маркировка `.onion`:** `deriveTorOnionAddress()` и `OnboardingScreen` (Step 4 `DERIVATION_SUMMARY`) явно зафиксированы и задокументированы как **детерминированный identity-label приложения** (`px_…node.onion`), без выдачи его за сырой wire-format Tor v3 base32.

### 2. Таблица иерархии ключей `RootKeyHub` (`path -> назначение`)

| Domain Path (HKDF-SHA512 info) | Метод в `RootKeyHub` | Назначение |
|---|---|---|
| `ParanoidX/Vault/Key/v1` | `deriveVaultKey(salt)` | AES-256-GCM / SQLCipher защита локального сейфа и секретов |
| `ParanoidX/Tor/HiddenService/v3` | `deriveTorOnionKey()` / `deriveTorOnionAddress()` | Детерминированный материал и application identity-label (`px_…node.onion`) |
| `ParanoidX/SimpleX/Identity/v1/{client,peer}` | `deriveSimpleXIdentityKey()` | Детерминированные ключевые идентификаторы SimpleX E2EE |
| `ParanoidX/XFTP/ChunkKey/v1` | `deriveXftpKey()` | Ключ шифрования 64KB чанков передачи XFTP (AES-256) |
| `ParanoidX/P2P/SwarmKey/v1` | `deriveP2pSwarmKey()` | Ключ криптоконтейнера и P2P-блоков роя (AES-256) |
| `ParanoidX/Wallet/EVM/v1` (+ `/public`, `/personal_sign`) | `deriveWalletEvmKey()` / `deriveWalletEvmAddress()` / `signWalletChallenge()` | Universal Wallet MVP: EIP-55 `0x…` адрес + EIP-191 Keccak-256 подпись challenge |
| `ParanoidX/Fleet/Node/{index}` | `deriveFleetNodeKey(index)` / `deriveFleetNodeInfo(index)` | Детерминированные sub-keys, `nodeId`, fingerprint и identity-label нод флота `#0..N` |

### 3. Этап 9 — Universal Wallet MVP (`WalletScreen.kt`, `Keccak256.kt`, `RootKeyHub.kt`)
- Реализован pure-Kotlin `Keccak256.kt` (padding `0x01`, стандарт EVM, `toEip55Address`, `ethereumMessageHash`).
- `RootKeyHub.deriveWalletEvmAddress()` стабильно выводит checksummed `0x…` адрес из Master Seed; адрес идентичен при повторной разблокировке ноды и очищается при `lockNode()` / `triggerPanicWipe()`.
- Экран `WalletScreen`: отображение EVM-адреса, `KEY FP`, копирование в буфер, оффлайн-подпись произвольного challenge (`InAppGboardKeyboard` + preset-чипы, без системной клавиатуры) и локальная криптографическая верификация подписи (`VALID / INVALID`). Никаких кастодиальных серверов.

### 4. Этап 9 — Fleet Command Center (`FleetScreen.kt`, `FleetNode.kt`, `MainViewModel.kt`)
- Локальный реестр флота (`paranoidx_fleet_registry` SharedPreferences, включён в `triggerPanicWipe`): хранит `nodeIndex`, `label`, `transportHint`, детерминированные `domainPath`, `nodeId`, `fingerprint`, `identityLabel`.
- Узел `#0` (текущее устройство) гарантируется всегда и отображает честную живую телеметрию (`Tor`, `P2P Swarm`, `SimpleX relays`).
- Добавление логической ноды вычисляет `nextIndex` из того же Master Seed (ввод имени и transport hint — через `InAppGboardKeyboard` в диалоге).
- **Честные статусы:** удалённые ноды `#1..N` без реального активного линка всегда отображаются как `REMOTE • STANDBY (NO LINK)` (жёлтый бейдж), без рисования фейкового «online».

### 5. Навигация и локализация (`MainAppLayout.kt`, `Localization.kt`)
- В `ModalNavigationDrawer` добавлены пункты `Wallet` (`nav_drawer_wallet`) и `Fleet` (`nav_drawer_fleet`), локализованные для `RU`, `EN`, `ES`, `FR`, `DE`, `ZH`.
- Главным экраном по умолчанию остался `chat`, кнопка назад и `BackHandler` корректно возвращают в чат.
- Регрессий онбординга, PIN-гейта, Panic Wipe и встроенной клавиатуры нет.

### 6. Проверки и сборка (`ExampleUnitTest.kt`)
- Добавлены unit-тесты:
  - `testKeccak256StandardVectorAndEip55` (эталонный вектор пустого входа EVM Keccak-256 + формат EIP-55)
  - `testUniversalWalletEvmAddressDeterminismAndSigning` (стабильность `0x…` между unlock, изоляция `xftp`/`evm`, подпись и верификация/отклонение подменённого challenge и чужого seed)
  - `testFleetNodeDeterministicDerivation` (детерминизм и уникальность `node#0..#2`)
  - В `testRootKeyHubHkdfDerivationAndDomainSeparation` добавлена проверка `ParanoidX/XFTP/ChunkKey/v1`, `ParanoidX/Wallet/EVM/v1`, `ParanoidX/Fleet/Node/0..1`.
- `gradle :app:testDebugUnitTest` — **BUILD SUCCESSFUL**
- `compile_applet` — **Build succeeded**

Жду ревью, Гриша!

Женя  
Lead Senior Dev


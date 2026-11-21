# CORRESPONDENCE JOURNAL
## Журнал переписки Гриша ↔ Женя
### ParanoidX Node / SimplexNode-2-GAIS

**Правило:** append-only предпочтителен; git хранит полную историю.

---

[... история 0–10 в git ...]

---
**[2026-11-21 23:59 CET]** | От: Гриша | Кому: Женя | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 10 принят. Выдаю Этап 11

---
**[2026-11-22]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 11 — Hardening (secp dual-mode, Tor ControlPort, live SMP, onion v3)

Secp256k1Engine + SPEND mode, TorControlClient GETINFO/NEWNYM, TorOnionV3, submitSimplexMessage→SmpProtocolEngine, fleet probe, RC matrix v2, tests 25/25.

---
**[2026-11-22 00:20 CET]** | От: Гриша (Аудитор) | Кому: Женя (Сеньор-разработчик) | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 11 принят. Выдаю Этап 12 — RC1 Closure

Женя, коммит `feat: implement Phase 11 hardening for node and wallet` проверен. Это был правильный этап.

### Что подтверждено
1. **P1 Wallet dual-mode** — `PROOF` (HMAC) и `SPEND` (secp256k1 ECDSA, отдельный domain `ParanoidX/Wallet/Spend/secp256k1/v1`) разведены в RootKeyHub и в UI. Не смешаны. Так и надо.
2. **Secp256k1Engine** — своя кривая на BigInteger, r/s/v, known-vector тесты по отчёту.
3. **TorControlClient** — реальный TCP ControlPort: AUTHENTICATE, GETINFO, NEWNYM; без порта — честный fail/fallback, не random success.
4. **TorOnionV3** — canonical encoding path (SHA3/checksum), отдельно от app-label `px_…`.
5. **Live chat path** — `submitSimplexMessage` идёт в `SmpProtocolEngine.sendSmpEnvelope`, не в `replies.random()`.
6. **Fleet probe** — manual TCP/SOCKS5 probe endpoint, remote без линка остаётся STANDBY.

### Вердикт

**Этап 11 — ПРИНЯТ.**  
RC1 из «надежды» стал «близко».

Остаточный долг (из твоей же матрицы v2) — это и есть Этап 12.

---

### Задание на Этап 12 — RC1 Closure

Не новые фичи. Закрытие дыр и freeze.

**1. VPN Suite honesty (обязательно)**  
Сейчас каркас VpnService есть, а счётчики/ping — sim.  
Либо реальные замеры, либо явный `[SIM]`/unavailable в UI без красивых фейковых Mbps.

**2. MainViewModel decomposition (обязательно хоть первый перенос)**  
Вынести VPN и/или остатки orchestration из «бога-объекта». Chat/P2P уже начаты — продолжай.

**3. RC1 smoke path (обязательно)**  
Зафиксируй и не сломай:  
onboard → PIN unlock → chat send (live SMP) → radio state меняется от сети/Tor → wallet switch PROOF/SPEND → fleet probe.

**4. RC-матрица v3 + короткий `RC1_NOTES.md`**  
Что shippable / что нет. Без поэзии.

**Критерии приёмки:** VPN не врёт; есть код распила VM; smoke path жив; матрица v3; тесты зелёные.

После приёмки 12 — **RC1**. Дальше bugfix / security fix only.

Ты закрыл главные P1/P2. Теперь — не дать честности расползтись на финише.

Гриша  
Аудитор, который уже почти готов выбить тег RC1

---

**[2026-11-22 01:05 CET]** | От: Женя (Сеньор-разработчик) | Кому: Гриша (Аудитор) | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 12 — RC1 Closure (VPN Honesty, VM Decomposition, Fleet Timestamp, Smoke Path, RC1 Notes)

Гриша, все 4 пункта задания на **Этап 12 (RC1 Closure)** + рекомендованный timestamp флота закрыты и проверены.

### Что выполнено
1. **VPN Suite Honesty (ноль выдуманной телеметрии)**
   - `VpnProtocolManager.kt`: удалены `delay(350)` и `(20..90).random()`. Метод `probeEndpoint()` / `pingEndpoint()` делает реальный TCP connect (`InetSocketAddress`) с замером nanosecond RTT и возвращает `VpnPingResult(reachable=false, rttMs=null)` / `-1` при недоступности. Профили в `VpnConfigParser.getSampleProfiles()` стартуют с `lastPingMs = -1` (`UNPROBED`), не подставляя фейковый пинг.
   - `ParanoidVpnService.kt`: полностью удалена `startTrafficSimulation()` с `(15000..85000).random()` и стартовыми фейковыми `14200L`/`8600L`. Добавлена обязательная проверка `val pfd = builder.establish()`: при `null` сервис **не** притворяется подключённым, ставит `isVpnConnected = false`, выставляет честный статус `"UNAVAILABLE • OS VpnService consent required or TUN rejected"` и останавливается. При активном TUN измеряются только реальные системные дельты `TrafficStats.getUidRxBytes/TxBytes` + `connectionDurationSeconds`, а статус прямо предупреждает о режиме без нативных userspace-демонов WireGuard/OpenVPN (`TUN_ONLY_NO_NATIVE_ENGINE`).
   - `NetworkScreen.kt`: интегрирован системный запрос согласия `VpnService.prepare(context)` через `ActivityResultContracts.StartActivityForResult()`, выводятся `vpnStatusDetail` и `vpnLastPingResult`.
   - Повторный `grep -rn "\.random(" app/src/main/java/`: во всём контуре сети/VPN/Tor/Wallet/Fleet/VM осталось **0** вызовов `.random()` (единственный `.random()` — визуальный splash delay в `CyberpunkMatrixSplashScreen.kt`).

2. **Декомпозиция `MainViewModel` (`2453` → `2045` строк, `-408` строк)**
   - Выделен `VpnViewModel.kt` (104 строки, `AndroidViewModel`), перенесён весь VPN Suite из `MainViewModel` и `NetworkScreen`.
   - Выделен `IdentityFleetCoordinator.kt` (349 строк): Wallet `PROOF` (HMAC) + `SPEND` (`secp256k1`), onion v3 / app-label derivation, локальный реестр флота и ручное зондирование.
   - Выделен `NodeDiagnosticsCoordinator.kt` (180 строк): `startNode`/`stopNode`/`restartNode`, диагностика, интерпретатор CLI-команд (`/status`, `/start`, `/stop`, `/restart`, `/test`, `/logs`, `/keys`, `/whoami`).

3. **Fleet Last-Probe Timestamp (Recommended)**
   - В `FleetNodeEntry` добавлено поле `lastProbeTimestamp: Long? = null`, сериализуется в `paranoidx_fleet_registry`, обновляется в `probeRemoteFleetNode` и отображается в `FleetNodeCard` как `HH:mm:ss (Manual)`.

4. **RC1 Smoke Path & `RC1_NOTES.md`**
   - В `ExampleUnitTest.kt` добавлены `testVpnProtocolManagerRealPingAndHonestFailure` и `testRc1EndToEndSmokePath` (BIP-39 seed → Canonical Tor v3 → `RadioPointEvaluator` (`SILENCE`→`DEGRADED`→`ON AIR`→`ALARM`) → Wallet `PROOF`/`SPEND secp256k1` → live `SmpProtocolEngine` local `HttpServer` → Fleet TCP probe + `lastProbeTimestamp`).
   - Создан честный `/RC1_NOTES.md` со списком shippable-модулей, ограничений (отсутствие нативных `libtor.so` и C/Go WG/OVPN packet-демонов) и правилом RC1 freeze (`bugfix / security fix only`).

### RC-Matrix v3 (Этап 12 — RC1 Candidate)
| Подсистема | Статус RC1 | Фактическое состояние на момент freeze |
|---|---|---|
| Root Cryptography & Vault (`RootKeyHub`, SQLCipher, PBKDF2, PIN/Panic) | **READY** | Реальный HKDF-SHA512, AES-GCM, SQLCipher, PIN/Panic Wipe |
| Air-Gap In-App Keyboard (`InAppGboardKeyboard` + системный IME suppressed) | **READY** | `readOnly = true`, RU/EN/NUM, zero system IME leaks |
| Wallet Dual-Mode (`PROOF` HMAC + `SPEND` `secp256k1` ECDSA RFC 6979) | **READY** | Независимые домены, EIP-2 low-S, `(r,s,v)` и EIP-55, known-vector тесты |
| Radio Point Health & 3 Themes (`RadioPointEvaluator`, `MATRIX`/`OBSIDIAN`/`AURORA`) | **READY** | Реальный `ConnectivityManager`, 4 честных состояния, zero fake green |
| SimpleX SMP / XFTP / P2P Storage & Engine | **READY** | Реальный HTTP/SOCKS5 транспорт, 64KB chunking SHA-256, LRU кэш, fail-closed `.onion` |
| Tor Identity (`TorOnionV3`) & ControlPort (`TorControlClient`) | **MVP** | Реальный RFC SHA3 `.onion` v3 и TCP ControlPort; без нативного `libtor.so` честно уходит в `SIMULATION` / `NO_CONTROLPORT` |
| VPN Suite (`VpnViewModel`, `VpnProtocolManager`, `ParanoidVpnService`) | **MVP** | Реальный TCP ping и системный TUN + UID `TrafficStats`, zero fake Mbps; нативные WG/OVPN движки не вшиты (explicit banner) |
| Fleet Command Center (`IdentityFleetCoordinator`, `FleetScreen`) | **MVP** | HKDF-деривация нод + ручной TCP/SOCKS5 probe с `lastProbeTimestamp`; нет фонового mesh-gossip |
| `MainViewModel` Architecture | **MVP** | Вынесены `ChatViewModel`, `P2pViewModel`, `VpnViewModel`, `IdentityFleetCoordinator`, `NodeDiagnosticsCoordinator`; Vault/ZK-backup в legacy-секции до post-GA |

### Прогон тестов (после финальных правок Этапа 12)
- `gradle :app:testDebugUnitTest`: **BUILD SUCCESSFUL** — `ExampleUnitTest`: **27/27 passed** (`0 failed, 0 errors, 0 skipped`); `ExampleRobolectricTest`: 5/5 pre-existing `@Ignore` (`0 failed`).

Жду вердикт на закрытие **RC1** и фиксацию freeze-ветки.

Женя
Lead Senior Developer
---

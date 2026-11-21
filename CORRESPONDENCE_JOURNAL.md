# CORRESPONDENCE JOURNAL
## Журнал переписки Гриша ↔ Женя
### ParanoidX Node / SimplexNode-2-GAIS

**Правило:** append-only предпочтителен; git хранит полную историю.

---

[... история 0–11 в git ...]

---
**[2026-11-22 00:20 CET]** | От: Гриша | Кому: Женя | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 11 принят. Выдаю Этап 12

---
**[2026-11-22]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 12 — RC1 Closure

VPN honest telemetry (TCP ping + TrafficStats), ViewModel/Coordinator split, fleet lastProbe persistence, RC1_NOTES.md, RC-matrix v3, smoke test, 27/27 unit tests.

---
**[2026-11-22 00:40 CET]** | От: Гриша (Аудитор) | Кому: Женя (Сеньор-разработчик) | Тип: АУДИТ / ПРИНЯТИЕ / FREEZE / ЗАДАНИЕ
**Тема:** Этап 12 принят. RC1 FEATURE FREEZE. Выдаю Этап 13 (post-RC1)

Женя, коммит `feat(vpn): implement honest telemetry and network probing` и `RC1_NOTES.md` проверены.

### Что подтверждено
1. **VPN honesty** — реальный TCP probe и системный `TrafficStats`; фейковые Mbps убраны; ограничения (нет вшитых native WG/OVPN packet engines) зафиксированы честно.
2. **Распил** — `VpnViewModel`, `ChatViewModel`, `P2pViewModel`, `IdentityFleetCoordinator`, `NodeDiagnosticsCoordinator` (Vault legacy ещё жив — ок для RC1).
3. **Fleet** — `lastProbeRttMs` / `lastProbeDetail` / `lastProbeTimestamp`.
4. **RC1_NOTES.md** + матрица v3 — shippable vs limits без самообмана.
5. **Smoke** — `testRc1EndToEndSmokePath` + 27/27 unit tests по отчёту.

### Вердикт

**Этап 12 — ПРИНЯТ.**  
**RC1 — FEATURE FREEZE.**

Нода достигла заявленного RC1 scope. Дальше не наращиваем «ещё одну фичу ради красивого коммита».

---

### Задание на Этап 13 — Post-RC1 Maintenance

Режим: **bugfix / security fix / снятие оставшегося SIM**, без feature creep.

Backlog (бери один крупный кусок за итерацию, не всё сразу):
1. Tor native packaging (`libtor.so` multi-ABI) — чтобы меньше жить в FallbackSimulation
2. VPN native engines **только** если реально, иначе не трогать баннер
3. Добить вынос Vault/ZK-backup из legacy MainViewModel
4. Security pass: panic edges, clipboard seed/PIN, backup flags
5. Стабилизация probe UX / мелочи RC1

**Критерии:** нет новых продуктовых экранов «с нуля»; тесты зелёные; короткий отчёт что починено; матрица/RC1_NOTES обновлены только по факту.

Если всё стабильно и чинить нечего — допустим отчёт «RC1 idle / no critical defects» без искусственной работы.

Ты довёл проект до freeze честно. Теперь охраняй периметр.

Гриша  
Аудитор, который наконец пишет слово FREEZE без кавычек

---

**[2026-11-22]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 13.1 — Post-RC1 Maintenance (Backlog #4: Security Pass)

Гриша, взял из бэклога Этапа 13 **пункт #4 (Security pass: panic edges, clipboard seed/PIN, backup flags)** строго в рамках feature freeze (новых экранов и фич не добавлялось).

### Что исправлено
1. **Clipboard seed/secret hygiene (`ClipboardSecurityManager` + `LocalVaultScreen`):**
   - Копирование расшифрованных секретов Vault, BIP-39 сид-фразы и расшифрованных файлов теперь помечается `android.content.extra.IS_SENSITIVE = true` (скрытие OS-превью на Android 13+) и ставит автоочистку буфера обмена через 30 секунд.
   - При блокировке/панике вызывается немедленная очистка буфера (`wipeClipboardNow`), а открытый диалог просмотра расшифрованного файла (`viewingFileItem`) сбрасывается при `!isUnlocked`.
   - `PinLockScreen` PIN-код в буфер не копирует.
2. **Panic edges & RAM/Storage purge (`RootKeyHub`, `DbEncryptionManager`, `MainViewModel`):**
   - `RootKeyHub.secureOverwriteAndDelete` теперь перезаписывает нулями с `fd.sync()` всю длину файла чанками по 64 KB (снято прежнее ограничение в 1 MB) перед удалением — применяется для `local_vault.db` (`-wal`, `-shm`, `-journal`), рекурсивно для `p2p_container` и `tordata`.
   - `triggerPanicWipe`, `verifyPinOrPanic(PANIC_WIPED)` и `deleteRecoveryAllData` синхронно вызывают `.commit()` clear на `paranoidx_root_hub`, `simplex_security`, `paranoidx_fleet_registry`, `paranoidx_prefs`, `simplex_db_crypto` (`DbEncryptionManager.purgePersistentState`), зануляют `masterSecretKey` / `cachedDbKey` / `_bip39Mnemonic` в памяти, очищают `identityFleet`, `P2pBlockStorage` и таблицы Room (`vaultDao.clearAllVault()`, `sharedFileDao`, `logDao.clearLogs()`).
3. **Backup & D2D extraction rules (`backup_rules.xml`, `data_extraction_rules.xml`):**
   - В дополнение к `android:allowBackup="false"` пустые шаблоны заменены явными `<exclude domain="sharedpref|database|file|root" path="." />` в `<full-backup-content>`, `<cloud-backup disableIfNoEncryptionCapabilities="true">` и `<device-transfer>` (блокировка Android 12+ D2D переноса).

### Проверки и документация
- Обновлён `RC1_NOTES.md` по факту выполненных изменений.
- Юнит-тесты `ExampleUnitTest`: **28/28 PASS** (добавлен `testStage13SecurityPassFileZeroizeLockAndClipboard`).
- Сборка APK (`compile_applet`): проверяется финальным шагом после отчёта.

### Что осталось в бэклоге Этапа 13
- #1 Tor native packaging (`libtor.so` multi-ABI)
- #2 VPN native engines (только при наличии реальных бинарей; баннер не трогался)
- #3 Вынос остатков Vault / ZK-backup из legacy `MainViewModel`
- #5 Мелкие UX-полировки probe / RC1

Жду вердикт по Этапу 13.1 и указание следующего куска (или подтверждение достаточности).
Женя
---

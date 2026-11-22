# CORRESPONDENCE JOURNAL
## Журнал переписки Гриша ↔ Женя
### ParanoidX Node / SimplexNode-2-GAIS

**Правило:** append-only предпочтителен; git хранит полную историю.

---

[... история 0–12 в git ...]

---
**[2026-11-22 00:40 CET]** | От: Гриша | Кому: Женя | Тип: FREEZE / ЗАДАНИЕ
**Тема:** RC1 freeze. Этап 13 post-RC1

---
**[2026-11-22]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Этап 13.1 — Security pass

ClipboardSecurityManager (IS_SENSITIVE + 30s wipe), panic full-file zeroize+sync, backup/data_extraction excludes, test 28/28, freeze не нарушен.

---
**[2026-11-22 00:55 CET]** | От: Гриша (Аудитор) | Кому: Женя (Сеньор-разработчик) | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** 13.1 принят. Выдаю 13.2 — Vault out of MainViewModel (рекомендация)

Женя, коммит `feat: implement post-RC1 security hardening` проверен. Это правильный post-RC1 кусок: безопасность, без фичекрепа.

### Что подтверждено
1. **ClipboardSecurityManager** — `android.content.extra.IS_SENSITIVE`, автоочистка 30с, wipe на lock/panic; vault copy path использует менеджер.
2. **Panic zeroize** — `secureOverwriteAndDelete` на всю длину файла, `fd.sync()`, не только первый 1MB; db/wal/shm/journal + p2p/tordata; sync `commit()` prefs.
3. **Backup/D2D** — `backup_rules.xml` + `data_extraction_rules.xml` с explicit `<exclude … path="." />`; manifest `allowBackup=false`.
4. Тест `testStage13SecurityPassFileZeroizeLockAndClipboard` на месте; 28/28 по отчёту.

### Вердикт

**Этап 13.1 — ПРИНЯТ.**  
Freeze соблюдён. Периметр стал плотнее.

---

### Задание на Этап 13.2

**Рекомендуемый кусок:** вынос Vault / ZK-backup orchestration из legacy `MainViewModel` → `VaultViewModel` или `VaultCoordinator` (как уже сделано для VPN/Chat/P2P/Fleet).

Почему не Tor packaging #1: без реальных multi-ABI `libtor.so` в артефактах это будет пустой жест. Распил Vault — измеримый архитектурный выигрыш без нарушения freeze.

Если у тебя **есть** готовые tor binaries для packaging — можно взять #1 вместо распила; тогда в отчёте укажи ABI-список и поведение при отсутствии бинаря.

**Не делать:** новые экраны, новые протоколы, «украшательства».

**Критерии:** RC1 flows unlock/lock/panic/vault list-add не сломаны; тесты зелёные; RC1_NOTES — только факт; короткий отчёт.

Можно и idle-отчёт, если распил сейчас рискованнее пользы — но 13.1 уже оправдал итерацию.

Гриша  
Аудитор, который любит zeroize с `fd.sync()` больше, чем обещания native VPN

---

**[2026-11-22]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 13.2 — Вынос Vault / ZK-backup orchestration из MainViewModel

Гриша, взял рекомендованный кусок бэклога **13.2**: вынес всю оркестрацию Vault / Master Key / PIN-Panic / Encrypted Files / Zero-Knowledge Backup из `MainViewModel` без нарушения RC1 feature freeze (новых экранов и фич не добавлялось).

### Что сделано
1. **Выделен `VaultCoordinator` (+ `VaultEnvironment` / `AndroidVaultEnvironment`):**
   - В `VaultCoordinator` перенесены: KDF setup/migration и `unlockVault` / `lockVault`, автоблокировка по 5-минутному таймауту неактивности, `completeOnboarding` / `verifyPinOrPanic` / `lockNode` / `deleteRecoveryAllData`, CRUD по записям Vault и `decryptedVaultItems` (с маскировкой при lock), пайплайн шифрования файлов `encryptAndSaveVaultFile`, шаринг `encryptAndShareFile`/`deleteSharedFile`/`toggleFilePin`, а также `generateZeroKnowledgeBackupPayload`, `restoreZeroKnowledgeBackup` и настройки/синк ZK Cloud Backup.
   - `MainViewModel` сокращён с **2072 до 1408 строк** (остались только делегирования к `VaultCoordinator` и связка сетевых транспортов Tor/SOCKS5/SMP/P2P, публичный API экранов `LocalVaultScreen`, `PinLockScreen`, `OnboardingScreen`, `MainAppLayout` сохранён).
2. **Побочный bugfix, пойманный тестом:**
   - В `VaultCoordinator` потоки `rawVaultItems` и `sharedFiles` переведены с `SharingStarted.WhileSubscribed(5000)` на `SharingStarted.Eagerly`, чтобы `generateZeroKnowledgeBackupPayload` / `triggerManualZkCloudSync` никогда не собирали пустой снимок при закрытом UI Vault.
3. **Тесты и документация:**
   - Добавлен `testStage13_2VaultCoordinatorLifecycleZkBackupAndPanic` в `ExampleUnitTest.kt`: проверяет setup, шифрование при записи в DAO, маскировку при lock и очистку RAM/буфера, unlock (отказ на неверном пароле, расшифровку на верном), экспорт/ошибку чужого пароля/импорт ZK-backup и `PANIC_WIPED` (очистка ключей, Vault/SharedFile DAO, буфера и колбэка ноды).
   - Юнит-тесты `ExampleUnitTest`: **29/29 PASS**.
   - Обновлён `RC1_NOTES.md` по факту выполненных изменений.

### Что остаётся в бэклоге
- #1 Tor native packaging (`libtor.so` multi-ABI — при наличии готовых бинарей)
- #2 VPN native engines (при наличии реальных движков; честный баннер не трогался)
- #5 Стабилизация probe UX / минорная полировка RC1 (или финализация Этапа 13 / idle при отсутствии дефектов)

Женя
---

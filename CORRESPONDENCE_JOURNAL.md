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

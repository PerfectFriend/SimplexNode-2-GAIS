# DEVELOPMENT JOURNAL
## ParanoidX Node / SimplexNode-2-GAIS

**Ведётся Гришей (Аудитор).**

---

## Статус на 2026-11-12

**Этапы 0–6 — ПРИНЯТЫ**  
**Этап 7-revised — ПРИНЯТ** (in-app keyboard only)  
**Этап 8 — В РАБОТЕ** (BIP-39 Onboarding + RootKeyHub)

### Аудит 7-revised
Проверено:
- `InAppGboardKeyboard.kt` (~28KB): RU/EN, symbols layers, shift/caps, haptic, AIR-GAP badge
- `MainActivity`: `SOFT_INPUT_STATE_ALWAYS_HIDDEN`
- Chat: in-app keyboard, без system TextField IME path
- Сборка/тесты по отчёту зелёные

Неблокирующий долг → Этап 8:
- `LocalVaultScreen` всё ещё содержит редактируемые `OutlinedTextField` (password/mnemonic/backup). Глобальный soft-input hide помогает, но secret-поля нужно явно перевести на in-app keyboard + readOnly display.

### Текущее задание
**Этап 8** — см. `CORRESPONDENCE_JOURNAL.md`.

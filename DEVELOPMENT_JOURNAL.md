# DEVELOPMENT JOURNAL
## ParanoidX Node / SimplexNode-2-GAIS

**Ведётся Гришей (Аудитор).**

---

## Статус на 2026-11-12

**Этапы 0–8 — ПРИНЯТЫ**  
**Этап 9 — В РАБОТЕ** (Universal Wallet + Fleet Command Center)

### Аудит Этапа 8
Коммит: `feat: implement BIP-39 sovereign key derivation`

Проверено:
- `RootKeyHub.kt` — setup/unlock/lock, panic wipe, HKDF domains (vault, tor, smp, p2p, fleet/node)
- `OnboardingScreen.kt` — create/restore, verify words, Master PIN, Panic PIN, InApp keyboard
- `PinLockScreen.kt` — in-app PIN, panic path
- `MainAppLayout` — hard gate `!isOnboarded` / `isPinLocked`
- Unit tests: domain separation + onion determinism (по отчёту)

Неблокирующие долги → Этап 9:
- domain labels `xftp`, `wallet/evm`
- Vault OutlinedTextField residual
- onion string = application identity label, не полный Tor v3 encoding

### Текущее задание
**Этап 9** — см. `CORRESPONDENCE_JOURNAL.md`.

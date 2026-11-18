# DEVELOPMENT JOURNAL
## ParanoidX Node / SimplexNode-2-GAIS

**Ведётся Гришей (Аудитор).**

---

## Статус на 2026-11-18

**Этапы 0–9 — ПРИНЯТЫ**  
**Этап 10 — В РАБОТЕ** (темы, радиоточка, RC)

### Аудит Этапа 9
Коммиты:
- `feat(security): add key derivation for XFTP and EVM`
- `feat: implement Stage 9 Universal Wallet and Fleet`

Проверено:
- RootKeyHub domains: + `ParanoidX/XFTP/ChunkKey/v1`, `ParanoidX/Wallet/EVM/v1`
- `Keccak256.kt` + EIP-55 helpers
- `WalletScreen` / `FleetScreen` / `FleetNode`
- Drawer entries; chat-home сохранён
- Remote fleet nodes: STANDBY (NO LINK) — честно
- Panic wipe включает fleet registry (по отчёту/коду)

Оговорка (зафиксирована):
- Wallet MVP использует HMAC domain-proof + HKDF-derived address material, не full secp256k1 spend key. Для MVP Этапа 9 допустимо; для «настоящего» EVM-кошелька — отдельный долг.

### Текущее задание
**Этап 10** — см. `CORRESPONDENCE_JOURNAL.md`.

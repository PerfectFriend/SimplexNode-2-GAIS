# DEVELOPMENT JOURNAL
## ParanoidX Node / SimplexNode-2-GAIS

**Ведётся Гришей (Аудитор).**

---

## Статус на 2026-11-22

**Этапы 0–11 — ПРИНЯТЫ**  
**Этап 12 — В РАБОТЕ** (RC1 Closure)

### Аудит Этапа 11
Коммит: `feat: implement Phase 11 hardening for node and wallet`

Проверено:
- `Secp256k1Engine` — pure BigInteger curve, RFC6979 path, r/s/v
- Wallet dual-mode UI: PROOF vs SPEND, разные domain paths
- `TorControlClient` — AUTHENTICATE + GETINFO + NEWNYM, honest offline
- `TorOnionV3` — canonical encoding helpers
- `submitSimplexMessage` → `SmpProtocolEngine.sendSmpEnvelope` live path
- Fleet remote probe via endpoint
- Unit tests expanded (25/25 per report)

### Release readiness
RC1 ≈ **84%**  
Full GA ≈ **72%**

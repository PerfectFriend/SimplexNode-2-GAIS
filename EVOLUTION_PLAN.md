# EVOLUTION PLAN
## ParanoidX Node / SimplexNode-2-GAIS

**Актуальная версия:** 2.5  
**Владелец:** Гриша (Аудитор)  
**Последнее обновление:** 2026-11-21

---

## Статус плановых этапов 0–10

**Все плановые этапы эволюции 0–10 — ПРИНЯТЫ.**

Проект переходит из «строительства каркаса» в **hardening → RC1**.

| Этап | Суть | Статус |
|------|------|--------|
| 0–5 | Честность, крипта, Tor, SMP, XFTP, P2P | ✅ |
| 6–7 | Telegram-shell + in-app keyboard only | ✅ |
| 8 | BIP-39 onboarding + RootKeyHub + Panic | ✅ |
| 9 | Wallet MVP + Fleet Command Center | ✅ MVP |
| 10 | Themes + Radio health + RC matrix | ✅ |

---

## Этап 11 — Hardening toward RC1
**Статус:** 🟡 В РАБОТЕ (выдан 2026-11-21)

Порядок приоритетов (из RC-матрицы Этапа 10):

### P1
1. **True secp256k1 path (опциональный spend-track)**  
   Либо настоящий ECDSA (`r,s,v`) + честный Ethereum address from pubkey,  
   либо явный dual-mode: `PROOF identity` (текущий HMAC) vs `SPEND key` (secp256k1).  
   Не смешивать и не врать в UI.

### P2
2. **Tor ControlPort `GETINFO`** вместо sim-RTT там, где ещё остался `[SIM]`.
3. **Живой chat relay** — `submitSimplexMessage` через реальный `SmpProtocolEngine` path, без декоративного sim-only хвоста.
4. **Canonical Tor v3 onion encoding** (если заявляем onion service; иначе оставить `px_…` как app identity label и не называть wire-onion).

### P3
5. Remote fleet handshake поверх SMP/Tor (хотя бы прототип link state).
6. Продолжить декомпозицию `MainViewModel`.

### Критерии приёмки Этапа 11
- UI/маркеры честно отражают PROOF vs SPEND (если secp добавлен)
- Хотя бы один из P2 (ControlPort **или** live chat path) закрыт реально
- Нет регрессии 0–10
- Обновлённая RC-матрица READY / MVP / NEEDS-WORK
- Сборка + тесты зелёные

---

## Оценка готовности к релизу (честная)

**~74% к RC1 / public beta**  
**~55–60% к full GA (mainnet-spend + multi-node mesh)**

Подробности — в журнале аудита.

# EVOLUTION PLAN
## ParanoidX Node / SimplexNode-2-GAIS

**Актуальная версия:** 2.6  
**Владелец:** Гриша (Аудитор)  
**Последнее обновление:** 2026-11-22

---

## Закрытые этапы

| Этап | Суть | Статус |
|------|------|--------|
| 0–10 | Каркас ноды (крипта → UI → radio/themes) | ✅ |
| 11 | Hardening: secp dual-mode, Tor ControlPort, live SMP, onion v3 | ✅ |

---

## Этап 12 — RC1 Closure
**Статус:** 🟡 В РАБОТЕ (выдан 2026-11-22)

Остаточный долг до честного RC1:

### Обязательный минимум
1. **VPN Suite honesty** — убрать/`[SIM]` явно отделить симулированные `pingEndpoint`/traffic counters; либо реальные замеры, либо честный disabled/unavailable.
2. **MainViewModel decomposition** — вынести хотя бы VPN + остатки Vault orchestration в профильные компоненты/VM (Chat/P2P уже начаты).
3. **RC1 freeze checklist** — freeze markers, no new feature creep, smoke path: onboard → pin → chat send → radio state → wallet PROOF/SPEND switch → fleet probe.

### Желательно
4. Fleet: после manual probe сохранять last-probe result с timestamp (не притворяться persistent mesh).
5. Документ `RC1_NOTES.md` коротко: что shippable, что нет.

### Критерии приёмки
- VPN больше не врёт цифрами без оговорки
- Есть ощутимый распил MainViewModel **или** явный план+первый перенос в коде
- Smoke path описан и не ломается
- RC-матрица v3
- Тесты/сборка зелёные

После Этапа 12 — **RC1 tag**, дальше только bugfix / security fix, без фич.

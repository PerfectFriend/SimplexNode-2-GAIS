# EVOLUTION PLAN
## ParanoidX Node / SimplexNode-2-GAIS

**Актуальная версия:** 2.7  
**Владелец:** Гриша (Аудитор)  
**Последнее обновление:** 2026-11-22

---

## Статус

| Диапазон | Содержание | Статус |
|----------|------------|--------|
| 0–10 | Каркас ноды | ✅ |
| 11 | Hardening (secp, Tor control, live SMP) | ✅ |
| 12 | RC1 Closure (VPN honesty, VM split, smoke, notes) | ✅ |

# RC1 FEATURE FREEZE

**RC1 объявлен.**  
Новые фичи в trunk не открываем. Только bugfix / security fix / документация.

См. `RC1_NOTES.md`.

---

## Этап 13 — Post-RC1 Maintenance & GA Backlog
**Статус:** 🟡 В РАБОТЕ (выдан 2026-11-22)

### Режим работы
- **Не** добавлять новые продуктовые поверхности (новые экраны «ради красоты», новые протоколы без необходимости).
- Можно: чинить баги, усиливать безопасность, убирать оставшийся `[SIM]`, доводить MVP→READY.

### Приоритетный backlog (на выбор, по одному крупному куску за итерацию)
1. **VPN native path** — честный баннер остаётся, но если появляется real WG/OVPN/engine integration — только с тестами и без fake throughput.
2. **Tor packaging** — native `libtor.so` multi-ABI в APK + путь без FallbackSimulation на поддерживаемых ABI.
3. **Добить распил** — Vault/ZK-backup orchestration из legacy `MainViewModel`.
4. **Fleet** — не mesh-gossip ради галочки; максимум: устойчивее probe history + clear UX lastProbe.
5. **Security pass** — panic wipe edges, clipboard policy для seed/PIN, backup flags review.

### Критерии приёмки итерации 13.x
- Нет feature creep вне backlog
- Обновлён RC1_NOTES / матрица только по факту
- Тесты зелёные
- Короткий отчёт: что починил / что осталось

### GA (далеко)
GA = RC1 + native Tor в релизной сборке + VPN real engines или явно «not in GA» + Vault VM split + operational docs. Не завтра.

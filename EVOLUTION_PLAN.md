# EVOLUTION PLAN
## ParanoidX Node / SimplexNode-2-GAIS

**Актуальная версия:** 2.8  
**Владелец:** Гриша (Аудитор)  
**Последнее обновление:** 2026-11-22

---

## Статус

| Этап | Суть | Статус |
|------|------|--------|
| 0–12 | Каркас → RC1 Closure | ✅ |
| RC1 | Feature freeze | ✅ ОБЪЯВЛЕН |
| 13.1 | Security pass (clipboard, panic zeroize, backup rules) | ✅ |
| 13.2 | Post-RC1 backlog (next) | 🟡 |

# RC1 FEATURE FREEZE — действует

Только bugfix / security / снятие SIM / распил legacy. Без новых продуктовых поверхностей.

---

## Этап 13.2 — Следующий кусок backlog

**Рекомендуемый фокус:** вынос остатков Vault / ZK-backup orchestration из legacy `MainViewModel` в `VaultCoordinator` / `VaultViewModel` (или эквивалент).

Почему не Tor packaging первым: multi-ABI `libtor.so` требует реальных артефактов в репо/CI; без бинарей это будет декоративный PR. Распил Vault — чистый код, снижает риск регрессий RC1.

Альтернативы (если бинари Tor реально есть):
1. Tor native packaging multi-ABI + честный статус без Fallback на поддерживаемых ABI
2. Security/bugfix по найденным дефектам
3. Idle report, если критичного нет

### Критерии 13.2
- Нет feature creep
- Если распил Vault: публичные RC1 flows (unlock/lock/panic/list/add) не сломаны
- Тесты зелёные + короткий отчёт
- RC1_NOTES обновлён только по факту

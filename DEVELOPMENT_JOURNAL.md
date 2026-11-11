# DEVELOPMENT JOURNAL
## ParanoidX Node / SimplexNode-2-GAIS

**Ведётся Гришей (Аудитор).**

---

## Статус проекта на 2026-11-11

### Общий вердикт
Сетевое ядро (0–5) и UI-каркас (6–7) приняты.  
Следующий стратегический блок: **BIP-39 как корень доверия + флот нод с одного ключа**.

**Этапы 0–7 — ПРИНЯТЫ**  
**Этап 8 — В РАБОТЕ** (BIP-39 Onboarding + Root Key Hierarchy)

### Матрица готовности

| Компонент | Статус | Маркер |
|-----------|--------|--------|
| Crypto / SQLCipher / BIP39 utils | Высокий | [REAL] [PROD-READY] |
| Tor + SOCKS5 | Высокий | [REAL] [PROD-READY] |
| SMP / XFTP / P2P MVP | Высокий | [REAL] [PROD-READY] |
| UI Telegram-shell + Drawer | Высокий | [REAL] |
| Gboard + Air-Gap input | Высокий | [REAL] |
| BIP-39 Onboarding UX | Низкий | [NEEDS-WORK] → Этап 8 |
| RootKeyHub / единая деривация | Низкий | [NEEDS-WORK] → Этап 8 |
| Panic PIN / duress wipe | Низкий | [NEEDS-WORK] → Этап 8 |
| Universal Wallet | Низкий | [NEEDS-WORK] → Этап 9 |
| Fleet Command Center | Низкий | [NEEDS-WORK] → Этап 9 |
| Themes / Radio health | — | Этап 10 |

---

## Записи аудитов (кратко)

- **Этап 6 ПРИНЯТ:** Chat как home, Drawer вместо bottom nav, BackHandler, начало распила ViewModel.
- **Этап 7 ПРИНЯТ:** системная Gboard, imePadding, privateImeOptions, Air-Gap toggle, Telegram bubbles/commands.
- **Продуктовое расширение (владелец):** интеграция идей из `PerfectFriend/BIP39-Onboarding-pipeline`; master seed → все ключи; один ключ → много нод; единый центр мониторинга флота.

---

## Текущее задание Жене

**Этап 8 — BIP-39 Sovereign Onboarding + Root Key Hierarchy**  
См. `CORRESPONDENCE_JOURNAL.md` и `EVOLUTION_PLAN.md` v2.0.

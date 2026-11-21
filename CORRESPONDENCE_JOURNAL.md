# CORRESPONDENCE JOURNAL
## Журнал переписки Гриша ↔ Женя
### ParanoidX Node / SimplexNode-2-GAIS

**Правило:** append-only предпочтителен; git хранит полную историю.

---

[... история 0–9 в git ...]

---
**[2026-11-18 02:35 CET]** | От: Гриша | Кому: Женя | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 9 принят (MVP). Выдаю Этап 10

---
**[2026-11-21]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 10 — Themes + Radio Point + RC matrix

Multi-theme (Matrix/Obsidian/Aurora), RadioPointEvaluator (honest, no Random), indicator+optional audio, RC matrix READY/MVP/NEEDS-WORK, tests green.

---
**[2026-11-21 23:59 CET]** | От: Гриша (Аудитор) | Кому: Женя (Сеньор-разработчик) | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 10 принят. Плановая эволюция 0–10 закрыта. Выдаю Этап 11 — Hardening → RC1

Женя, проверил коммит `feat: implement Phase 10 features and rebranding`.

### Что подтверждено
1. **Темы** — `NodeThemeProfile`: MATRIX / OBSIDIAN / AURORA, палитры, persistence, переключатель в drawer. Контраст не развален.
2. **Радиоточка** — `RadioPointEvaluator` детерминирован: сеть + Tor + VPN + P2P → `ON_AIR` / `DEGRADED` / `SILENCE` / `ALARM`. Без `Random`. Canvas-волна + опциональный `ToneGenerator`.
3. **UI** — chip в top bar, detail dialog, audio toggle.
4. **RC-матрица** — совпадает с реальностью кода. Особенно ценно, что wallet честно помечен как Non-secp256k1 proof identity.

### Вердикт

**Этап 10 — ПРИНЯТ.**  
**Плановые этапы 0–10 — ЗАКРЫТЫ.**

Это не «всё готово к магазину», это «каркас ноды собран честно». Дальше — закалка.

---

### Задание на Этап 11 — Hardening toward RC1

Работай по приоритетам своей же RC-матрицы.

**P1 (если берёшься за кошелёк):**
- Dual-mode или замена:  
  - `PROOF` = текущий HMAC domain-proof (оставить, честно подписать в UI)  
  - `SPEND` = настоящий secp256k1 (`r,s,v`) + address from pubkey  
- Не смешивать. Пользователь должен понимать, что именно он держит.

**P2 (ядро живучести ноды — предпочтительный фокус):**
1. Tor ControlPort `GETINFO` вместо sim-RTT там, где ещё `[SIM]`.
2. Живой путь чата: `submitSimplexMessage` → реальный `SmpProtocolEngine` (не декоративный хвост).
3. Onion: либо canonical v3 encoding, либо явный label `px_…` без претензии на wire-onion.

**P3:**
- Remote fleet link-state прототип
- Дальнейший распил `MainViewModel`

**Критерии приёмки:**
- Закрыт P1 **или** минимум один существенный P2 реально (не документом)
- Маркеры `[REAL]` / `[SIM]` / `[NEEDS-WORK]` обновлены
- RC-матрица v2 в отчёте
- Нет регрессии 0–10
- Тесты/сборка зелёные

Ты довёл ноду до точки, где уже стыдно врать и уже можно закалять.  
Бери P2 в первую очередь, если хочешь, чтобы «радио» и чат жили одной правдой.

Гриша  
Аудитор, который ставит RC-процент ниже маркетинга и выше самообмана

---

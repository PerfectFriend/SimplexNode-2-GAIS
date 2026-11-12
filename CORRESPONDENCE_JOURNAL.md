# CORRESPONDENCE JOURNAL
## Журнал переписки Гриша ↔ Женя
### ParanoidX Node / SimplexNode-2-GAIS

**Правило:** append-only предпочтителен; git хранит полную историю.

---

[... история 0–6 и коррекция клавиатуры в git ...]

---
**[2026-11-11 16:50 CET]** | От: Гриша | Кому: Женя | Тип: ЗАМЕЧАНИЕ / ВОЗВРАТ / ЗАДАНИЕ
**Тема:** СТОП. Коррекция владельца — Этап 7 на доработку (in-app only)

---
**[2026-11-12 17:15 CET]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 7-revised — In-App Keyboard Only

Системная IME отключена. `InAppGboardKeyboard` (RU/EN/symbols/shift), `SOFT_INPUT_STATE_ALWAYS_HIDDEN`, chat на in-app, vault частично, тесты зелёные.

---
**[2026-11-12 14:20 CET]** | От: Гриша (Аудитор) | Кому: Женя (Сеньор-разработчик) | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 7-revised принят. Выдаю Этап 8 — BIP-39 Onboarding + RootKeyHub

Женя, доброе утро муравейника. Проверил.

### Что подтверждено по коду
- `InAppGboardKeyboard.kt` — полноценная in-app клавиатура: RU/EN, два слоя символов, shift/caps, haptic, бейдж AIR-GAP.
- `MainActivity`: `window.setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN)` — системная IME глобально прижата.
- Chat переведён на in-app keyboard, Telegram-chrome сохранён.
- Коммит `feat: implement secure in-app keyboard` на месте.

Это ровно то, что потребовал владелец: **ввод не уходит в Google**.

### Вердикт

**Этап 7-revised — ПРИНЯТ.**

Неблокирующий долг (закрыть в Этапе 8, не откладывая «на потом»):
- В `LocalVaultScreen` ещё живы редактируемые `OutlinedTextField` (пароль, mnemonic restore, backup passphrase и т.д.).
- Глобальный `ALWAYS_HIDDEN` — хороший щит, но secret-поля должны быть **явно** на in-app keyboard (display readOnly / свой focus → наша панель), иначе на части прошивок IME всё ещё может дёрнуться.

---

### Задание на Этап 8 — BIP-39 Sovereign Onboarding + Root Key Hierarchy

Источник UX/идеи: https://github.com/PerfectFriend/BIP39-Onboarding-pipeline

**1. Онбординг (create + restore)**
- Welcome / disclaimer
- Показ мнемоники под `FLAG_SECURE`
- Верификация слов (challenge, нельзя пропустить)
- Master PIN PIN + **Panic PIN** (duress wipe)
- Restore с полной проверкой checksum
- **Весь ввод seed/PIN — только `InAppGboardKeyboard`**

**2. RootKeyHub**
- Единая точка деривации из master seed
- Domain separation, минимум:
  - `vault`
  - `smp` / `xftp` / `tor` / `p2p`
  - `wallet/evm` (заготовка)
  - `node/{index}` (заготовка флота)
- Новые ad-hoc `deriveKey("магическая_строка")` не плодим

**3. Хранение и panic**
- Seed не в plaintext
- Unlock через Master PIN
- Panic PIN → wipe чувствительного; границы wipe честно описать в отчёте

**4. Дожать IME на Vault/secret fields**
- Пароль, seed, backup secrets — in-app only

**Критерии приёмки:** create+restore E2E; verify обязателен; RootKeyHub + таблица path→назначение; panic есть; secret-поля без system IME; сборка зелёная; отчёт в журнале.

**Не требуется сейчас:** UI флота, полный multi-chain wallet, темы, радиоточка.

Ты закрыл рот Google на вводе. Теперь даём ноде **имя и корни ключей**.

Гриша  
Аудитор, который наконец может не бояться автоподстановки seed из облака Gboard

---

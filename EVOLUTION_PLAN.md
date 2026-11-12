# EVOLUTION PLAN
## ParanoidX Node / SimplexNode-2-GAIS

**Актуальная версия:** 2.2  
**Владелец:** Гриша (Аудитор)  
**Последнее обновление:** 2026-11-12

---

## Продуктовая рамка

1. **Главный экран = чат** (Telegram-style), единое боковое меню.
2. **Ввод = ТОЛЬКО встроенная клавиатура приложения** (эргономика ≈ Gboard). Системная IME запрещена.
3. **Корень доверия = BIP-39 master seed** → детерминированная деривация всех ключей.
4. **Флот нод:** один master seed → N нод + единый центр мониторинга.
5. **Онбординг UX-донор:** `PerfectFriend/BIP39-Onboarding-pipeline`.
6. **Украшательство / радиоточка** — после key/fleet ядра.

---

## Этапы 0–6
**Статус:** ✅ ПРИНЯТЫ

## Этап 7 / 7-revised — In-App Keyboard Only
**Статус:** ✅ ПРИНЯТ (2026-11-12)

- `InAppGboardKeyboard`: RU/EN, symbols, shift/caps, haptic
- `MainActivity`: `SOFT_INPUT_STATE_ALWAYS_HIDDEN`
- Chat на in-app keyboard
- Неблокирующий долг: часть `OutlinedTextField` в Vault ещё не переведена на readOnly + in-app (закрыть в Этапе 8 вместе с seed/PIN)

## Этап 8 — BIP-39 Sovereign Onboarding + Root Key Hierarchy
**Статус:** 🟡 В РАБОТЕ (выдан 2026-11-12)

### Обязательный объём
1. **Онбординг create/restore** (в духе BIP39-Onboarding-pipeline):
   - disclaimer → mnemonic display (`FLAG_SECURE`) → word verification → Master PIN → Panic PIN
   - restore: 12/24 + full checksum
   - **весь ввод seed/PIN только через InAppGboardKeyboard**
2. **RootKeyHub** — единая деривация:
   - labels/paths: `vault`, `smp`, `xftp`, `tor`, `p2p`, `wallet/evm`, `node/{index}`
   - запрет новых ad-hoc `deriveKey("string")` в обход хаба
3. **Хранение seed** не в plaintext; unlock Master PIN; Panic PIN → wipe
4. **Дожать IME-изоляцию** на всех secret-полях (Vault/PIN/seed): `readOnly` / нет system IME

### Критерии приёмки
- Create + Restore E2E
- Verification step нельзя обойти
- RootKeyHub + таблица деривации в отчёте
- Panic PIN существует и документирован
- Secret-поля без system IME
- Сборка зелёная + отчёт в журнале

### Не входит
- UI флота и полный multi-chain wallet (Этап 9)
- Темы / радиоточка (Этап 10)

## Этап 9 — Universal Wallet + Fleet Command Center
**Статус:** 🔒

## Этап 10 — Полировка, темы, радиоточка, RC
**Статус:** 🔒

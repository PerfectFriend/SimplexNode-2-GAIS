# CORRESPONDENCE JOURNAL
## Журнал переписки Гриша ↔ Женя
### ParanoidX Node / SimplexNode-2-GAIS

**Правило:** append-only предпочтителен; git хранит полную историю.

---

[... история 0–7 в git ...]

---
**[2026-11-12 14:20 CET]** | От: Гриша | Кому: Женя | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 7-revised принят. Выдаю Этап 8

---
**[2026-11-12 ~]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 8 — BIP-39 Sovereign Onboarding + RootKeyHub

OnboardingScreen (create/restore/verify/PIN/panic), RootKeyHub (HKDF domains), PinLockScreen, panic wipe (prefs + SQLCipher files + P2P container), MainApp gate, in-app keyboard на seed/PIN, unit-тесты domain separation, сборка зелёная.

---
**[2026-11-12 14:45 CET]** | От: Гриша (Аудитор) | Кому: Женя (Сеньор-разработчик) | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 8 принят. Выдаю Этап 9 — Universal Wallet + Fleet Command Center

Женя, проверил код после пуша. Коммит `feat: implement BIP-39 sovereign key derivation` на месте.

### Что подтверждено
- **`RootKeyHub`**: setup master keys, encrypted seed at rest, unlock/lock, zeroize в RAM, panic wipe (prefs + db/wal overwrite + p2p_container), HKDF-SHA512 с domain separation:
  - `ParanoidX/Vault/Key/v1`
  - `ParanoidX/Tor/HiddenService/v3`
  - `ParanoidX/SimpleX/Identity/v1/{client,peer}`
  - `ParanoidX/P2P/SwarmKey/v1`
  - `ParanoidX/Fleet/Node/{index}`
- **`OnboardingScreen`**: поток create/restore, verify words, Master PIN ≠ Panic PIN, ввод через `InAppGboardKeyboard`
- **`PinLockScreen`**: PIN + panic path
- **Gate**: `MainAppLayout` не пускает в приложение без онбординга / с lock

Нода теперь **рождается** из seed, а не из пустого экрана. Это качественный скачок.

### Вердикт

**Этап 8 — ПРИНЯТ.**

Неблокирующие долги (закрыть в 9, не «когда-нибудь»):
1. Явные domain labels для **`xftp`** и **`wallet/evm`** (в ТЗ Этапа 8 были в минимуме — добей).
2. Residual `OutlinedTextField` в Vault — где ещё остались secret-поля, дожимай in-app only.
3. `px_…node.onion` — нормальный **identity label** приложения; не выдавать за полный wire-format Tor v3, пока не будет настоящей onion-key math.

---

### Задание на Этап 9 — Universal Wallet + Fleet Command Center

Владелец зафиксировал: один ключ → кошелёк + флот нод + единый центр.

**1. Universal Wallet (MVP)**
- Domain в RootKeyHub: `ParanoidX/Wallet/EVM/v1` (минимум)
- Детерминированный публичный адрес (EVM-style `0x…` достаточно для MVP)
- Экран/секция Wallet: показать адрес, copy, (желательно) sign message/challenge
- Без кастодиального сервера и без «облачного аккаунта»

**2. Fleet Command Center**
- Локальный реестр нод флота: `nodeIndex`, derived id/key fingerprint, label
- UI раздел (из drawer): список нод, статус текущей (Tor/P2P/chat), заготовки для remote
- Добавление логической ноды = новый `nodeIndex` из того же seed (`deriveFleetNodeKey` уже есть — опирайся)
- Честные статусы: не рисовать «online», если нет данных

**3. RootKeyHub polish**
- Добавить `deriveXftpKey` / `deriveWalletEvmKey` (или эквивалент) с явными domain strings
- Таблицу path→назначение — в отчёте

**4. Навигация**
- Wallet и Fleet доступны из drawer, chat остаётся home

**Критерии приёмки:**
- Адрес кошелька стабилен при re-unlock того же seed
- Fleet UI есть, nodeIndex детерминирован
- Domains xftp + wallet в хабе
- Нет регрессии онбординга / pin / in-app keyboard
- Сборка зелёная + отчёт в журнале

**Не требуется:** публичный интернет-DHT, DEX, темы, радиоточка.

Ты дал ноде день рождения. Теперь — **паспорт (wallet) и штаб флота**.

Гриша  
Аудитор, который уже видит табличку node/0 … node/N

---

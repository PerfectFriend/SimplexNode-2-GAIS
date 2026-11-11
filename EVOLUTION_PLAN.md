# EVOLUTION PLAN
## ParanoidX Node / SimplexNode-2-GAIS

**Актуальная версия:** 2.0  
**Владелец:** Гриша (Аудитор)  
**Последнее обновление:** 2026-11-11

---

## Продуктовая рамка

1. **Главный экран = чат** (Telegram-style), единое боковое меню.
2. **Клавиатура** = системная Gboard по умолчанию + опциональный Air-Gap Shield.
3. **Корень доверия = BIP-39 master seed.**  
   Один ключ владельца → детерминированная деривация всего:
   - identity / wallet (EVM и др.)
   - vault keys
   - Tor onion seeds
   - SMP / XFTP / P2P keys
   - ключи отдельных нод флота
4. **Флот нод:** с одного master seed владелец поднимает N нод и управляет ими из единого центра (мониторинг, хранилища, чаты).
5. **Источник UX онбординга:** репозиторий `PerfectFriend/BIP39-Onboarding-pipeline` (disclaimer → mnemonic → verify → PIN → panic PIN → restore).
6. **Украшательство / радиоточка** — только после закрытия key/fleet ядра.

---

## Этапы 0–5 — Сетевое и крипто-ядро
**Статус:** ✅ ВСЕ ПРИНЯТЫ

| Этап | Суть |
|------|------|
| 0 | Честная маркировка |
| 1 | KDF + SQLCipher + BIP-39 checksum + zeroize |
| 2 | Реальный Tor + SOCKS5 |
| 3 | Реальный SMP |
| 4 | Реальный XFTP |
| 5 | P2P Crypto Cloud MVP |

---

## Этап 6 — UI Core Refactor (Telegram-style shell)
**Статус:** ✅ ПРИНЯТ (2026-11-11)

- Главный экран = Chat
- ModalNavigationDrawer вместо bottom nav
- BackHandler → чат
- Начало распила ViewModel (Chat / P2P)

---

## Этап 7 — Клавиатура и ввод (Gboard-like)
**Статус:** ✅ ПРИНЯТ (2026-11-11)

- Системная Gboard + imePadding + ImeAction.Send
- privateImeOptions для sensitive input
- Air-Gap Shield как опциональный режим
- Telegram-style bubbles, attach, commands, emoji

---

## Этап 8 — BIP-39 Sovereign Onboarding + Root Key Hierarchy
**Статус:** 🟡 В РАБОТЕ (выдан 2026-11-11)

**Источник идей и UX:** https://github.com/PerfectFriend/BIP39-Onboarding-pipeline

### Цели
1. **Полноценный онбординг** (create / restore):
   - Welcome + disclaimer
   - Генерация 12/24 слов (уже есть `Bip39Utils`)
   - Отображение мнемоники с защитой экрана (`FLAG_SECURE`)
   - Верификация слов (challenge, не «просто галочка»)
   - Master PIN + **Panic PIN** (duress wipe)
   - Restore flow с полной проверкой checksum
2. **Master Seed = единый корень доверия**
   - После онбординга seed не хранится в plaintext
   - Все рабочие ключи деривируются детерминированно (HKDF / BIP-32/44-подобные пути с domain separation):
     - `vault`
     - `smp`
     - `xftp`
     - `tor_onion`
     - `p2p`
     - `wallet/evm` (заготовка универсального кошелька)
     - `node/{nodeIndex}` — ключи конкретной ноды флота
3. **Универсальный источник ключей**
   - Один модуль `RootKeyHub` / `MasterSeedVault`, через который проходят все подсистемы
   - Запрет ad-hoc `deriveKey("random_string")` в новых местах
4. **Panic / Duress**
   - Panic PIN → мгновенный wipe чувствительных данных (prefs, in-memory keys, по возможности filesDir secrets)

### Критерии приёмки
- Первый запуск ведёт в онбординг, а не в «пустой чат без личности»
- Create + Restore работают end-to-end
- Checksum и verify-step обязательны
- Master PIN блокирует vault; Panic PIN уничтожает
- Есть явная иерархия деривации (документированные path/labels)
- Маркеры обновлены; отчёт в журнале

### Не входит в Этап 8
- Полноценный multi-chain wallet UI (можно заготовку адресов)
- UI командного центра флота (Это Этап 9)
- Темы / радиоточка

---

## Этап 9 — Universal Wallet + Fleet Command Center
**Статус:** 🔒 Заблокирован до Этапа 8

### Цели
1. **Универсальный крипто-кошелёк** на базе master seed:
   - детерминированные адреса (минимум EVM; расширяемо)
   - просмотр / экспорт публичных реквизитов
   - подпись сообщений (challenge-response) без утечки seed
2. **Флот нод с одного ключа:**
   - `nodeIndex = 0..N` → стабильные nodeId / onion seed / SMP identity
   - владелец поднимает любое количество нод на одном master seed
3. **Единый центр мониторинга и управления:**
   - список нод флота: online/offline, Tor, storage, chat queues
   - статусы хранилищ и облака
   - агрегированные логи / алерты
   - базовые команды (start/stop services, request status)

### Критерии приёмки
- Из одного seed можно зарегистрировать ≥2 логические ноды с разными derived identity
- Есть экран/раздел Fleet с честными статусами
- Wallet показывает хотя бы один детерминированный публичный адрес
- Нет регрессии онбординга и локального vault

---

## Этап 10 — Полировка, темы, «радиоточка», RC
**Статус:** 🔒 Заблокирован

- Темы / скины / обои
- Радиоточка как health-indicator ноды/флота
- Финальная полировка UX, батарея, память
- Release Candidate

---

## Примечания

- BIP39-Onboarding-pipeline — **референс UX/потока и security-модели**, не требование копировать TS/React один в один. На Android — нативная реализация в том же духе.
- Флот не требует «облачного аккаунта»: идентичность нод детерминирована seed + index; канал управления — через уже построенный безопасный транспорт (Tor/SMP), когда будет готов.
- Любые отклонения — только через запись Гриши в журнале.

# CORRESPONDENCE JOURNAL
## Журнал переписки Гриша ↔ Женя
### ParanoidX Node / SimplexNode-2-GAIS

**Статус:** Канонический канал связи  
**Правило:** Записи не удаляются. Предпочтительный режим — append-only; git хранит полную историю.

---

[... ранняя история этапов 0–5 и служебные записи сохранены в git ...]

---
**[2026-11-11 15:25 CET]** | От: Гриша (Аудитор) | Кому: Женя | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 5 принят. Выдаю Этап 6 — UI Core Refactor

---
**[2026-11-11 15:40 CET]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 6 — UI Core Refactor

Chat = home, ModalNavigationDrawer, BackHandler, без bottom nav, ChatViewModel/P2pViewModel, сборка зелёная.

---
**[2026-11-11 16:15 CET]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 7 — Клавиатура и ввод (Gboard-like)

Gboard TextField + imePadding + ImeAction.Send, privateImeOptions, Air-Gap Shield RU/EN, attach/commands/emoji, Telegram bubbles, тесты зелёные.

---
**[2026-11-11 16:45 CET]** | От: Гриша (Аудитор) | Кому: Женя (Сеньор-разработчик) | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этапы 6–7 приняты. Выдаю Этап 8 — BIP-39 Onboarding + Root Key Hierarchy (+ курс на флот)

Женя, отлично потрудился. Принимаю оба UI-этапа.

### Вердикт

**Этап 6 — ПРИНЯТ.**  
Чат как дом, drawer вместо жирной нижней панели, back в чат, начало распила ViewModel — это именно то, что просил владелец.

**Этап 7 — ПРИНЯТ.**  
Системная Gboard как основной ввод (без переучивания пальцев), imePadding, privateImeOptions для sensitive, Air-Gap как опция параноика — зрелое решение, не «кастом ради кастома».

Небольшие неблокирующие заметки:
- Продолжай распил MainViewModel (Vault/Network ещё можно вынести).
- Air-Gap раскладка — держи габариты близко к привычным, чтобы не ломать мышечную память.

---

### Новая продуктовая рамка от владельца (обязательна)

Изучен репозиторий-донор: **https://github.com/PerfectFriend/BIP39-Onboarding-pipeline**

Туда смотрим как на:
- UX-пайплайн онбординга (welcome → disclaimer → mnemonic → verify → PIN → panic PIN → restore);
- модель duress/panic wipe;
- идею **одного master seed как корня всего**.

Владелец зафиксировал:
1. **BIP-39 онбординг** должен стать полноценным входом в приложение.
2. **Универсальный источник ключей** — вся криптография ноды деривируется из master seed (не зоопарк ad-hoc строк).
3. **Универсальный крипто-кошелёк** — на том же seed (минимум детерминированные публичные адреса + заготовка подписи).
4. **Флот нод:** один ключ владельца → любое количество нод (`nodeIndex`), единый центр мониторинга и управления нодами, хранилищами и чатами.

Это отражено в `EVOLUTION_PLAN.md` v2.0:
- **Этап 8** — Onboarding + Root Key Hierarchy (сейчас)
- **Этап 9** — Wallet + Fleet Command Center
- **Этап 10** — темы, радиоточка, RC

---

### Задание на Этап 8 — BIP-39 Sovereign Onboarding + Root Key Hierarchy

**Обязательный объём:**

1. **Онбординг-поток (нативный Android, в духе BIP39-Onboarding-pipeline):**
   - create: disclaimer → show mnemonic (`FLAG_SECURE`) → word verification challenges → Master PIN → Panic PIN
   - restore: ввод 12/24 → full checksum validate → PIN setup
   - первый запуск без seed не должен «молча» пускать в чат как полноценную личность

2. **RootKeyHub (имя на твой вкус):**
   - единая точка деривации из master seed
   - domain-separated labels/paths: `vault`, `smp`, `xftp`, `tor`, `p2p`, `wallet/evm`, `node/{index}`
   - запрет новых `deriveKey("магическая_строка")` в обход хаба

3. **Хранение**
   - seed не в plaintext
   - unlock через Master PIN
   - Panic PIN → wipe чувствительного (in-memory + encrypted prefs / secrets; честно задокументируй границы wipe)

4. **Интеграция с уже принятым**
   - SQLCipher / vault / Tor seed / SMP identity по возможности перевести на ключи из хаба (миграция или clean path для новых установок)

5. **Отчёт**
   - что портировал по духу из BIP39-Onboarding-pipeline
   - схема деривации (таблица path/label → назначение)
   - что сделано с panic wipe
   - что осталось на Этап 9 (wallet UI / fleet)

**Критерии приёмки:**
- Create + Restore E2E
- Verification step нельзя обойти
- Есть RootKeyHub и документированная иерархия
- Panic PIN существует и делает что обещает (в рамках Android)
- Сборка зелёная + запись в журнале

**Не требуется в Этапе 8:**
- полноценный UI флота
- multi-chain wallet целиком
- темы и радиоточка

Ты собрал тело ноды и лицо чата. Теперь даём ей **день рождения** — онбординг и единый корень ключей.

Гриша  
Аудитор, который уже видит флот на горизонте

---

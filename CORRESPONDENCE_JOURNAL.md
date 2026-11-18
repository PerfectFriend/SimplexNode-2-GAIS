# CORRESPONDENCE JOURNAL
## Журнал переписки Гриша ↔ Женя
### ParanoidX Node / SimplexNode-2-GAIS

**Правило:** append-only предпочтителен; git хранит полную историю.

---

[... история 0–8 в git ...]

---
**[2026-11-12 14:45 CET]** | От: Гриша | Кому: Женя | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 8 принят. Выдаю Этап 9

---
**[2026-11-18]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 9 — Universal Wallet + Fleet Command Center

Domains xftp/evm, Keccak/EIP-55, WalletScreen (address/sign/verify, in-app kb), FleetScreen (node#0 live, remote STANDBY NO LINK), drawer, tests green.

---
**[2026-11-18 02:35 CET]** | От: Гриша (Аудитор) | Кому: Женя (Сеньор-разработчик) | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 9 принят (MVP). Выдаю Этап 10 — темы, радиоточка, RC

Женя, проверил после пуша. Коммиты `feat(security): add key derivation for XFTP and EVM` и `feat: implement Stage 9 Universal Wallet and Fleet` на месте.

### Что подтверждено
1. **RootKeyHub** — добавлены явные домены:
   - `ParanoidX/XFTP/ChunkKey/v1`
   - `ParanoidX/Wallet/EVM/v1` (+ public / personal_sign contexts)
2. **Wallet** — `WalletScreen`: адрес, fingerprint, copy, challenge через `InAppGboardKeyboard`, локальная проверка VALID/INVALID. Без кастодии.
3. **Fleet** — реестр nodeIndex, node#0 с живой телеметрией, remote честно `REMOTE • STANDBY (NO LINK)`. Не врёте «online».
4. **Drawer** — Wallet/Fleet, chat остаётся home.
5. Тесты детерминизма/доменов — по отчёту зелёные.

### Вердикт

**Этап 9 — ПРИНЯТ как MVP.**

Обязательная честная оговорка (записываю в долг, не как провал этапа):
- «Подпись» challenge = **HMAC-SHA512 domain proof** над EIP-191 Keccak digest.
- Адрес = EIP-55 от HKDF-derived material, **не** классический secp256k1 Ethereum pubkey.
- Для штаба флота и суверенной identity ноды — достаточно.  
  Для spend-кошелька mainnet — **не** выдавать за полный EVM wallet, пока не будет настоящего secp256k1 (или явного брендинга «ParanoidX proof key»).

Так держать: лучше честный MVP, чем «как будто ECDSA».

---

### Задание на Этап 10 — Полировка, темы, «радиоточка», RC

Финальный слой продукта.

**1. Темы / скины**
- Минимум 3 переключаемых визуальных профиля (например: Matrix / Obsidian / Aurora — имена на твой вкус).
- Не ломать читаемость чата, vault, wallet, fleet.
- Переключатель из drawer или settings.

**2. Радиоточка = health indicator**
- Живой сигнал / «эфир» → сеть и ключевые сервисы в норме (как минимум: network available + Tor bootstrap ok или честный degraded state).
- Тишина / alarm → нет сети, Tor down, критический error.
- Без random «радио играет потому что красиво».
- Можно как визуально-звуковой индикатор в UI (не обязательно полноценный FM-тюнер).

**3. Полировка**
- Дожать residual secret fields на in-app keyboard, где ещё остались.
- Пустые/locked состояния Wallet & Fleet аккуратные.
- Маркеры `[REAL]` / `[NEEDS-WORK]` / `[PROD-READY]` привести в соответствие с реальностью (особенно wallet crypto model).

**4. RC-отчёт**
Таблица в журнале:
- READY к проду
- MVP acceptable
- NEEDS-WORK (с приоритетом)

**Критерии приёмки:** темы работают; radio/health честный; нет регрессии 0–9; RC-матрица в отчёте; сборка зелёная.

**Не требуется:** полный secp256k1 mainnet wallet (можно заготовку), публичный DHT.

Ты собрал кости, лицо, паспорт и штаб. Осталось — окраска, сигнал «жива/не жива», и честный RC.

Гриша  
Аудитор, который ценит HMAC-правду выше ECDSA-театра

---

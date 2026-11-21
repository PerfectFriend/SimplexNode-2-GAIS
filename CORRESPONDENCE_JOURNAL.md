# CORRESPONDENCE JOURNAL
## Журнал переписки Гриша ↔ Женя
### ParanoidX Node / SimplexNode-2-GAIS

**Правило:** append-only предпочтителен; git хранит полную историю.

---

[... история 0–10 в git ...]

---
**[2026-11-21 23:59 CET]** | От: Гриша | Кому: Женя | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 10 принят. Выдаю Этап 11

---
**[2026-11-22]** | От: Женя | Кому: Гриша | Тип: ОТЧЁТ
**Тема:** Отчёт по Этапу 11 — Hardening (secp dual-mode, Tor ControlPort, live SMP, onion v3)

Secp256k1Engine + SPEND mode, TorControlClient GETINFO/NEWNYM, TorOnionV3, submitSimplexMessage→SmpProtocolEngine, fleet probe, RC matrix v2, tests 25/25.

---
**[2026-11-22 00:20 CET]** | От: Гриша (Аудитор) | Кому: Женя (Сеньор-разработчик) | Тип: АУДИТ / ПРИНЯТИЕ / ЗАДАНИЕ
**Тема:** Этап 11 принят. Выдаю Этап 12 — RC1 Closure

Женя, коммит `feat: implement Phase 11 hardening for node and wallet` проверен. Это был правильный этап.

### Что подтверждено
1. **P1 Wallet dual-mode** — `PROOF` (HMAC) и `SPEND` (secp256k1 ECDSA, отдельный domain `ParanoidX/Wallet/Spend/secp256k1/v1`) разведены в RootKeyHub и в UI. Не смешаны. Так и надо.
2. **Secp256k1Engine** — своя кривая на BigInteger, r/s/v, known-vector тесты по отчёту.
3. **TorControlClient** — реальный TCP ControlPort: AUTHENTICATE, GETINFO, NEWNYM; без порта — честный fail/fallback, не random success.
4. **TorOnionV3** — canonical encoding path (SHA3/checksum), отдельно от app-label `px_…`.
5. **Live chat path** — `submitSimplexMessage` идёт в `SmpProtocolEngine.sendSmpEnvelope`, не в `replies.random()`.
6. **Fleet probe** — manual TCP/SOCKS5 probe endpoint, remote без линка остаётся STANDBY.

### Вердикт

**Этап 11 — ПРИНЯТ.**  
RC1 из «надежды» стал «близко».

Остаточный долг (из твоей же матрицы v2) — это и есть Этап 12.

---

### Задание на Этап 12 — RC1 Closure

Не новые фичи. Закрытие дыр и freeze.

**1. VPN Suite honesty (обязательно)**  
Сейчас каркас VpnService есть, а счётчики/ping — sim.  
Либо реальные замеры, либо явный `[SIM]`/unavailable в UI без красивых фейковых Mbps.

**2. MainViewModel decomposition (обязательно хоть первый перенос)**  
Вынести VPN и/или остатки orchestration из «бога-объекта». Chat/P2P уже начаты — продолжай.

**3. RC1 smoke path (обязательно)**  
Зафиксируй и не сломай:  
onboard → PIN unlock → chat send (live SMP) → radio state меняется от сети/Tor → wallet switch PROOF/SPEND → fleet probe.

**4. RC-матрица v3 + короткий `RC1_NOTES.md`**  
Что shippable / что нет. Без поэзии.

**Критерии приёмки:** VPN не врёт; есть код распила VM; smoke path жив; матрица v3; тесты зелёные.

После приёмки 12 — **RC1**. Дальше bugfix / security fix only.

Ты закрыл главные P1/P2. Теперь — не дать честности расползтись на финише.

Гриша  
Аудитор, который уже почти готов выбить тег RC1

---

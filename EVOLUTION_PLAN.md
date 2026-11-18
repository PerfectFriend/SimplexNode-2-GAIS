# EVOLUTION PLAN
## ParanoidX Node / SimplexNode-2-GAIS

**Актуальная версия:** 2.4  
**Владелец:** Гриша (Аудитор)  
**Последнее обновление:** 2026-11-18

---

## Продуктовая рамка

1. Chat-home + drawer.
2. In-app keyboard only.
3. BIP-39 master seed → HKDF domains.
4. Wallet MVP + Fleet registry с одного seed.
5. Темы / радиоточка / RC — финальный слой.

---

## Этапы 0–9
**Статус:** ✅ ПРИНЯТЫ

| Этап | Суть |
|------|------|
| 0–5 | Честность, крипта, Tor, SMP, XFTP, P2P |
| 6–7 | Telegram-shell + in-app keyboard |
| 8 | Onboarding + RootKeyHub + Panic PIN |
| 9 | Wallet MVP + Fleet Command Center |

### Этап 9 — принято с оговорками
- Domains: vault, tor, smp, p2p, xftp, wallet/evm, fleet/node/{i}
- WalletScreen: адрес, copy, challenge sign/verify (in-app keyboard)
- FleetScreen: node#0 live telemetry; remote = STANDBY (NO LINK)
- Drawer: Wallet + Fleet; chat remains home

**Честный технический долг (не блокер приёмки MVP):**
- Подпись challenge сейчас = **HMAC-SHA512 domain proof** над EIP-191 digest, **не** secp256k1 ECDSA.
- EVM-адрес = EIP-55 над материалом из HKDF+Keccak, **не** классический pubkey secp256k1.
- Для «универсального кошелька» уровня mainnet это нужно будет заменить на настоящий secp256k1 (или явно держать как ParanoidX-identity proof, не как Ethereum spend key).

---

## Этап 10 — Полировка, темы, «радиоточка», RC
**Статус:** 🟡 В РАБОТЕ (выдан 2026-11-18)

### Цели
1. **Темы / скины / обои** — переключаемые визуальные профили (минимум 3), без ломки читаемости chat/vault.
2. **Радиоточка как health-indicator:**
   - «играет» / живой сигнал → нода видит сеть, ключевые сервисы в норме;
   - «замолчало» / alarm → проблема (Tor down / no network / critical error).
   - Это UX-сигнал стабильности, не развлекательный плеер ради плеера.
3. **Полировка:**
   - residual secret `OutlinedTextField` → in-app only где ещё осталось;
   - мелочи навигации, пустые состояния Fleet/Wallet;
   - маркеры `[REAL]` / `[NEEDS-WORK]` актуализировать (особенно wallet crypto model).
4. **RC checklist** в отчёте: что prod-ready, что сознательно MVP.

### Критерии приёмки
- ≥3 темы переключаются из UI
- Радио/health индикатор честно завязан на реальные статусы (Tor/network), не на random
- Нет регрессии онбординга, pin, keyboard, wallet, fleet
- В отчёте явная матрица READY vs NEEDS-WORK
- Сборка зелёная

### Опционально (если останутся силы)
- Заготовка под настоящий secp256k1 (библиотека + API), без обещания full mainnet wallet

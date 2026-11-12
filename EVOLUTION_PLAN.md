# EVOLUTION PLAN
## ParanoidX Node / SimplexNode-2-GAIS

**Актуальная версия:** 2.3  
**Владелец:** Гриша (Аудитор)  
**Последнее обновление:** 2026-11-12

---

## Продуктовая рамка

1. Главный экран = чат (Telegram-style), единое меню.
2. Ввод = **только** in-app keyboard (эргономика ≈ Gboard).
3. Корень доверия = BIP-39 master seed → HKDF domain separation.
4. Один seed → N нод флота + единый центр мониторинга.
5. Онбординг UX-донор: `PerfectFriend/BIP39-Onboarding-pipeline`.
6. Темы / радиоточка — после wallet/fleet.

---

## Этапы 0–7-revised
**Статус:** ✅ ПРИНЯТЫ

## Этап 8 — BIP-39 Onboarding + Root Key Hierarchy
**Статус:** ✅ ПРИНЯТ (2026-11-12)

Сделано:
- `OnboardingScreen` (create/restore, verify, Master PIN, Panic PIN)
- `RootKeyHub` (HKDF-SHA512, vault/tor/smp/p2p/fleet node)
- `PinLockScreen` + panic wipe
- Gate в `MainAppLayout`
- In-app keyboard на seed/PIN

Долг (не блокер):
- Явные domain labels для `xftp` и `wallet/evm` (добавить в Этапе 9)
- Полная зачистка всех `OutlinedTextField` в Vault (частично)
- Onion address сейчас упрощённый (`px_…node.onion`), не полный Tor v3 wire format — ок для identity-label

## Этап 9 — Universal Wallet + Fleet Command Center
**Статус:** 🟡 В РАБОТЕ (выдан 2026-11-12)

### Цели
1. **Universal Wallet (MVP)** на master seed:
   - domain `ParanoidX/Wallet/EVM/v1` (и при необходимости другие)
   - детерминированный публичный адрес (минимум EVM-style 0x…)
   - показ адреса / копирование; подпись challenge (опционально, желательно)
   - **без** кастодиальных бэкендов
2. **Fleet Command Center:**
   - список нод `nodeIndex = 0..N` с derived identity (через уже есть `deriveFleetNodeKey`)
   - статусы: online/unknown, Tor, storage/P2P, chat
   - возможность «добавить логическую ноду» в локальный реестр флота
   - агрегированный overview (хотя бы локальная нода + заготовки соседей)
3. **Добить RootKeyHub labels:** `xftp`, `wallet/evm` явно
4. UI доступен из drawer (не ломая chat-as-home)

### Критерии приёмки
- Wallet показывает стабильный адрес из seed
- Fleet screen/раздел существует, nodeIndex детерминирован
- RootKeyHub содержит wallet + xftp domains
- Нет регрессии онбординга / pin lock / in-app keyboard
- Сборка зелёная + отчёт

### Не входит
- Публичный DHT на весь интернет
- Полноценный multi-chain swap UI
- Темы, скины, радиоточка (Этап 10)

## Этап 10 — Полировка, темы, радиоточка, RC
**Статус:** 🔒

# DEVELOPMENT JOURNAL
## ParanoidX Node / SimplexNode-2-GAIS

**Ведётся Гришей (Аудитор).**  
Женя не редактирует этот файл напрямую.

---

## Статус проекта на 2026-11-11

### Общий вердикт
Функциональное сетевое ядро закрыто: крипта, Tor, SMP, XFTP, P2P Crypto Cloud. Дальше — интерфейс.

**Этап 0 — ПРИНЯТ**  
**Этап 1 — ПРИНЯТ**  
**Этап 2 — ПРИНЯТ**  
**Этап 3 — ПРИНЯТ**  
**Этап 4 — ПРИНЯТ**  
**Этап 5 — ПРИНЯТ**

### Матрица готовности (обновлено после аудита Этапа 5)

| Компонент | Статус | Маркер | Комментарий |
|-----------|--------|--------|-------------|
| Crypto / Vault / SQLCipher | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| Tor + SOCKS5 | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| SMP transport | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| XFTP transport | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| P2pBlockStorage | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | Диск, AES-GCM, SHA-256, LRU |
| P2pCloudEngine | Высокий | [REAL] [PROD-READY] | Micro-server, реальный latency/speed |
| TorrentSeederService | Высокий | [REAL] | Active Node по телеметрии |
| jlibtorrent / mainline DHT | Низкий | [NEEDS-WORK] | Честно отложено |
| MainViewModel | — | [NEEDS-WORK] | God-object → Этап 6 |
| UI shell | — | [NEEDS-WORK] | Telegram-style → Этап 6 |

---

## Записи аудитов

### Аудит 2026-11-11 — Этап 5 (P2P Crypto Cloud) — ПРИНЯТ

**Что проверено:**
- `P2pBlockStorage`: реальные файлы `blk_{hash}.bin`, AES-256-GCM, проверка SHA-256 на write/read, LRU-eviction, размер контейнера.
- `P2pCloudEngine`: embedded HttpServer (`/p2p/ping`, `/p2p/blocks`), реальный socket latency (не random), скорости из фактических байт, Tor SOCKS optional, статусы NO_NETWORK / ACTIVE_RELAY / SEEDING / DOWNLOADING.
- Симуляционные рандомы и фейковые кусочки из основного пути убраны (по отчёту и коду движка).
- Честное признание: jlibtorrent / global DHT — `[NEEDS-WORK]`, MVP закрыт.

**Незначительные замечания:**
- Ключ контейнера пока из `deriveKey(string)` — в будущем лучше SecureRandom + Keystore/HKDF.
- Micro-server слушает `127.0.0.1` — для локального MVP ок; для внешнего роя понадобится bind-политика и безопасность.
- `getActualPiecesMap` упрощённый (index < count) — для UI-индикатора достаточно, для настоящего bitfield позже уточнить.

**Вердикт:** Этап 5 принимается. Облако перестало врать.

---

## Текущее задание Жене

**Этап 6 — UI Core Refactor (Telegram-style shell)**  
См. `CORRESPONDENCE_JOURNAL.md`.

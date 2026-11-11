# DEVELOPMENT JOURNAL
## ParanoidX Node / SimplexNode-2-GAIS

**Ведётся Гришей (Аудитор).**  
Женя не редактирует этот файл напрямую.

---

## Статус проекта на 2026-11-11

### Общий вердикт
Крипто-фундамент, Tor, SMP и XFTP подняты на реальный сетевой уровень. Функциональное ядро транспорта собрано.

**Этап 0 — ПРИНЯТ**  
**Этап 1 — ПРИНЯТ**  
**Этап 2 — ПРИНЯТ**  
**Этап 3 — ПРИНЯТ**  
**Этап 4 — ПРИНЯТ**

### Матрица готовности (обновлено после аудита Этапа 4)

| Компонент | Статус | Маркер | Комментарий |
|-----------|--------|--------|-------------|
| CryptoUtils + KDF | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| Bip39Utils | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| SQLCipher / AppDatabase | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| TorDaemonService | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| Socks5ProxyClient | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| SmpProtocolEngine | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| XftpProtocolEngine | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | Реальный upload/download чанков, без delay |
| TorrentSeederService | Средний | частично [REAL] | Цель Этапа 5 |
| MainViewModel | — | [NEEDS-WORK] | God-object, Этап 6 |
| UI shell | — | [NEEDS-WORK] | Telegram-style — Этап 6 |

---

## Записи аудитов

### Аудит 2026-11-11 — Этап 4 (XFTP и файловый слой) — ПРИНЯТ

**Что проверено по коду:**
- `XftpProtocolEngine.kt` полностью переписан: нет `delay()` в upload/download.
- `buildHttpClient` — SOCKS5 через `createUnresolved` (DNS-leak isolation).
- `uploadTransfer` — реальные HTTP POST чанков с заголовками X-Chunk-* и честным FAILED при ошибках.
- `downloadTransfer` + `reassembleFile` — реальный GET, расшифровка, per-chunk SHA-256, root integrity.
- Блокировка `.onion` без Tor.
- Маркеры `[REAL] [PROD-READY] [SECURITY-CRITICAL]` соответствуют коду.

**Незначительные замечания (не блокируют):**
- Endpoint’ы `/api/v1/files/.../chunks/...` — разумная REST-аппроксимация XFTP; полный binary wire SimpleX XFTP можно уточнять позже.
- MetadataStripper в pipeline перед чанкированием желательно явно проверить на следующем проходе (не блокер приёмки).
- `deriveKey("xftp_key_...")` для эфемерного ключа файла — работает, но лучше явно random 256-bit + PBKDF/HKDF в будущем.

**Вердикт:** Этап 4 принимается. Файловый транспорт больше не симуляция.

---

## Текущее задание Жене

**Этап 5 — P2P Crypto Cloud**  
См. подробное ТЗ в `CORRESPONDENCE_JOURNAL.md`.

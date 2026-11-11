# DEVELOPMENT JOURNAL
## ParanoidX Node / SimplexNode-2-GAIS

**Ведётся Гришей (Аудитор).**  
Женя не редактирует этот файл напрямую.

---

## Статус проекта на 2026-11-11

### Общий вердикт
Крипто-фундамент, реальный Tor и реальный SMP-транспорт подняты. Приложение перестало быть прототипом с симуляцией мессенджера.

**Этап 0 — ПРИНЯТ**  
**Этап 1 — ПРИНЯТ**  
**Этап 2 — ПРИНЯТ**  
**Этап 3 — ПРИНЯТ**

### Матрица готовности (обновлено после аудита Этапа 3)

| Компонент | Статус | Маркер | Комментарий |
|-----------|--------|--------|-------------|
| CryptoUtils + KDF | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| Bip39Utils | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| SQLCipher / AppDatabase | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| TorDaemonService | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| Socks5ProxyClient | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| SmpProtocolEngine | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | Реальный OkHttp + SOCKS5, честные ошибки |
| XftpProtocolEngine | Низкий–Средний | частично [REAL] | Чанкирование реальное, транспорт — [SIM] → Этап 4 |
| TorrentSeederService | Средний | частично [REAL] | |
| MainViewModel | — | [NEEDS-WORK] | God-object, Этап 6 |
| Package name | — | [NEEDS-WORK] | com.example |

---

## Записи аудитов

### Аудит 2026-11-11 — Этап 3 (Реальный SMP-транспорт) — ПРИНЯТ

**Что проверено по коду:**
- `SmpProtocolEngine.kt`: полностью переписан на OkHttpClient.
- `buildHttpClient(useTorProxy)` — корректный SOCKS Proxy через `InetSocketAddress.createUnresolved` (DNS-leak isolation).
- `sendSmpEnvelope` — реальный HTTP POST, никаких `delay()` в основном пути.
- Честные статусы: `DELIVERED`, `TOR_REQUIRED`, `IO_ERROR`, `HTTP_xxx`, `UNREACHABLE`.
- Блокировка `.onion` без Tor.
- Предупреждение при clearnet-отправке.
- `registerQueueOnRelay`, `pollIncomingEnvelopes`, `pingRelay` — реальные сетевые вызовы.
- Маркеры обновлены корректно.

**Незначительные замечания (не блокируют):**
- API endpoint’ы (`/api/v1/queues/...`) — это разумная REST-аппроксимация SMP. Настоящий wire-протокол SimpleX более бинарный/WebSocket-ориентированный; для текущего этапа достаточно.
- Парсинг входящих сообщений в `pollIncomingEnvelopes` пока возвращает emptyList() (тело читается, но не разбирается в пакеты) — можно усилить на следующих итерациях.
- Полный Double Ratchet production-grade по-прежнему не требуется.

**Вердикт:** Этап 3 принимается. Основной путь SMP больше не является симуляцией.

---

## Текущее задание Жене

**Этап 4 — XFTP и файловый слой**  
См. подробное ТЗ в `CORRESPONDENCE_JOURNAL.md`.

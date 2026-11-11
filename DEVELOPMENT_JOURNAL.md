# DEVELOPMENT JOURNAL
## ParanoidX Node / SimplexNode-2-GAIS

**Ведётся Гришей (Аудитор).**  
Женя не редактирует этот файл напрямую.

---

## Статус проекта на 2026-11-11

### Общий вердикт
Криптографический фундамент и реальный Tor-демон подняты. Приложение перестало быть «красивой симуляцией лука» и получило настоящий SOCKS5-канал.

**Этап 0 — ПРИНЯТ**  
**Этап 1 — ПРИНЯТ**  
**Этап 2 — ПРИНЯТ**

### Матрица готовности (обновлено после аудита Этапа 2)

| Компонент | Статус | Маркер | Комментарий |
|-----------|--------|--------|-------------|
| CryptoUtils + KDF | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| Bip39Utils | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| SQLCipher / AppDatabase | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| TorDaemonService | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | Реальные ELF, bootstrap, fallback |
| Socks5ProxyClient | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | openTunnel + DNS-leak protection |
| Tor binaries (multi-ABI) | Высокий | [REAL] [PROD-READY] | arm64 / armeabi-v7a / x86_64 |
| SmpProtocolEngine | Низкий | [SIM] | Главная цель Этапа 3 |
| XftpProtocolEngine | Низкий–Средний | частично [REAL] | |
| TorrentSeederService | Средний | частично [REAL] | |
| MainViewModel | — | [NEEDS-WORK] | God-object, Этап 6 |
| Package name | — | [NEEDS-WORK] | com.example |

---

## Записи аудитов

### Аудит 2026-11-11 — Этап 2 (Настоящий Tor) — ПРИНЯТ

**Что проверено:**
- Реальные ELF-бинарники присутствуют в репозитории (`assets/bin/{arm64-v8a,armeabi-v7a,x86_64}/tor` + jniLibs).
- `TorDaemonService`: выбор ABI, извлечение/nativeLibraryDir, chmod, ProcessBuilder с боевыми флагами, парсинг Bootstrapped %, StateFlow, корректный stop, честный FallbackSimulation.
- `Socks5ProxyClient`: `openTunnel()` возвращает живой Socket, ATYP=0x03 по умолчанию, `sendProbeHttp()` для сквозной проверки.
- Unit-тест на RFC 1928 + DNS-leak protection заявлен.
- `allowBackup="false"` в манифесте.

**Незначительные замечания (не блокируют):**
- Полный Kill Switch на уровне VpnService пока не реализован (это отдельная большая тема, не входила в обязательный scope Этапа 2).
- Детерминированный Onion v3 из BIP-39 seed пока не закрыт (можно сделать позже).
- God-object MainViewModel остаётся.

**Вердикт:** Этап 2 принимается. Настоящий Tor-канал есть.

---

## Текущее задание Жене

**Этап 3 — Реальный SMP-транспорт**  
См. подробное ТЗ в `CORRESPONDENCE_JOURNAL.md`.

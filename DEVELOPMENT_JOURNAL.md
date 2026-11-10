# DEVELOPMENT JOURNAL
## ParanoidX Node / SimplexNode-2-GAIS

**Ведётся Гришей (Аудитор).**  
Женя не редактирует этот файл напрямую.

---

## Статус проекта на 2026-11-11

### Общий вердикт
Криптографический фундамент и шифрование локального хранилища приведены в порядок. Проект вышел из стадии «красивый прототип с дырявым KDF» и получил реальный security baseline.

**Этап 0 — ПРИНЯТ**  
**Этап 1 — ПРИНЯТ**

### Матрица готовности (обновлено после аудита Этапа 1)

| Компонент | Статус | Маркер | Комментарий |
|-----------|--------|--------|-------------|
| CryptoUtils (AES-GCM + PBKDF2) | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | 150k iter, 32-byte salt, zeroize, legacy migration |
| Bip39Utils | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | Полный checksum + zeroize |
| DbEncryptionManager | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | |
| AppDatabase + SQLCipher | Высокий | [REAL] [PROD-READY] [SECURITY-CRITICAL] | База зашифрована на диске |
| Socks5ProxyClient | Высокий | [REAL] [SECURITY-CRITICAL] | Нужна обвязка ProxyTunnel на Этапе 2 |
| TorDaemonService | Низкий–Средний | [REAL] каркас + [SIM] бинарник | Главная цель Этапа 2 |
| TorrentSeederService | Средний | частично [REAL] | |
| SmpProtocolEngine | Низкий | [SIM] | |
| XftpProtocolEngine | Низкий–Средний | частично [REAL] | |
| MainViewModel | — | [NEEDS-WORK] | God-object, Этап 6 |
| Package name | — | [NEEDS-WORK] | com.example |
| Тесты | Появились | [REAL] | Unit-тесты на KDF, BIP-39, zeroize |

---

## Записи аудитов

### Аудит 2026-11-10 — Инициализация Манифеста и Журнала
- Принят `DEVELOPMENT_MANIFESTO.md` v1.0

### Аудит 2026-11-11 — Этап 0 (Фундамент и Честность) — ПРИНЯТ
- Честная маркировка ключевых модулей

### Аудит 2026-11-11 — Этап 1 (Криптографический фундамент) — ПРИНЯТ

**Что проверено по коду:**
- `CryptoUtils.deriveKey` → PBKDF2WithHmacSHA512, 150_000 итераций, 32-байтная соль, `clearPassword()`, zeroize
- Legacy migration path сохранён
- `DbEncryptionManager` + `SupportFactory` + `SQLiteDatabase.loadLibs`
- `AppDatabase` открывается только через зашифрованный factory
- `Bip39Utils.validateMnemonic` — полная проверка checksum
- zeroize для ByteArray/CharArray присутствует и используется
- Unit-тесты добавлены и заявлены как зелёные

**Незначительные замечания (не блокируют приёмку):**
- При отсутствии пользовательского пароля БД всё равно шифруется через device enclave seed — это нормально для «always encrypted», но нужно чётко понимать threat model.
- `fallbackToDestructiveMigration()` всё ещё стоит — на следующих этапах лучше сделать нормальные миграции.
- Пакет `com.example` остаётся техническим долгом (Этап 6).

**Вердикт:** Этап 1 принимается. Крипто-фундамент можно считать `[PROD-READY]` в рамках текущего scope.

---

## Текущее задание Жене

**Этап 2 — Настоящий Tor**  
См. подробное ТЗ в `CORRESPONDENCE_JOURNAL.md`.

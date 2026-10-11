# Security Notes

## Room Database Encryption
The Room database for Classroom-LMS is encrypted at rest using **SQLCipher** (`net.zetetic:android-database-sqlcipher`). 

### Passphrase Management
- A strong, randomized 256-bit passphrase is generated automatically on the device during the first launch.
- The passphrase is securely stored in `EncryptedSharedPreferences`, protected by Android's hardware-backed Keystore (`MasterKey.KeyScheme.AES256_GCM`).
- The passphrase is never hardcoded, transmitted, or logged.

### Unencrypted to Encrypted Migration (v1.7.0+)
- In v1.7.0, an automated, safe one-time migration (`LegacyDatabaseMigrator`) is implemented to upgrade existing unencrypted databases (`classroom_lms_db`) to the SQLCipher encrypted database (`classroom_lms_db_encrypted`).
- **Safety & Verification**: All 14 entity tables are transferred within a single atomic database transaction. Row counts for critical tables (Classrooms and Students) are verified against the original database before the migration is finalized.
- **Fail-safe Backup**: Upon verified completion, the unencrypted database file (and its `-wal`/`-shm`/`-journal` auxiliary files) is safely renamed to `classroom_lms_db.bak` rather than immediately destroyed.
- **Error Blocking & Rescue Flow**: If migration fails for any reason (e.g. storage exhaustion, corrupt file), the app does NOT open an empty database. Instead, a blocking upgrade screen is presented with options to Retry or export the raw unencrypted database as a portable JSON backup file.
- **Auto-Backup Exclusions**: Auto-backup rules (`backup_rules.xml` and `data_extraction_rules.xml`) explicitly exclude `classroom_lms_db`, `classroom_lms_db_encrypted`, all `.bak` copies, and the Keystore-backed `db_secure_prefs.xml`.

## Sync 
- Sync operations are fully decoupled from the core `ClassroomViewModel` and are managed securely and explicitly via `SyncViewModel`.

## Network Security & Cleartext Traffic
- The application enforces a strict `network_security_config.xml` that disallows all cleartext HTTP traffic (`cleartextTrafficPermitted="false"`).
- All network interactions with Google Sheets and Firebase communicate solely over verified TLS.

## Logging & PII Redaction
- All network and synchronization logging routes through `SafeLogger`.
- Authentication tokens, API keys, email addresses, spreadsheet identifiers, and raw exception stacktraces are automatically redacted before any log emission to Logcat.
- Verbose debug logs are completely stripped in release builds.

## Target SDK 36 (Android 15) Compliance
- The application targets SDK 36 and complies with Android 15 background execution limitations.
- Classroom LMS is an offline-first educational tool and does not employ foreground services, background location tracking, or exact alarm scheduling, ensuring zero runtime permission regressions on Android 15 devices.

## Tests
- Database unit tests run against an unencrypted in-memory SQLite implementation to avoid dependency on native SQLCipher libraries inside Robolectric, ensuring stable and fast test executions without compromising real-world security.

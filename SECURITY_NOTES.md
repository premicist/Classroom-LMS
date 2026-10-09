# Security Notes

## Room Database Encryption
The Room database for Classroom-LMS is encrypted at rest using **SQLCipher** (`net.zetetic:android-database-sqlcipher`). 

### Passphrase Management
- A strong, randomized 256-bit passphrase is generated automatically on the device during the first launch.
- The passphrase is securely stored in `EncryptedSharedPreferences`, protected by Android's hardware-backed Keystore (`MasterKey.KeyScheme.AES256_GCM`).
- The passphrase is never hardcoded, transmitted, or logged.

### Unencrypted to Encrypted Migration
- Because safe cross-database exports using `sqlcipher_export` natively through Room migrations is highly risky, existing unencrypted installations will **lose their data** on upgrade to this version.
- When opening the database via SQLCipher, the system points to a new database file `classroom_lms_db_encrypted`. Old `classroom_lms_db` data is safely ignored, creating a clean slate for teachers. 
- **Important**: This must be explicitly communicated in the release notes so teachers can manually back up any important rosters *before* applying the app update.

## Sync 
- Sync operations are fully decoupled from the core `ClassroomViewModel` and are managed securely and explicitly via `SyncViewModel`.

## Tests
- Database unit tests run against an unencrypted in-memory SQLite implementation to avoid dependency on native SQLCipher libraries inside Robolectric, ensuring stable and fast test executions without compromising real-world security.

package com.aistudio.classroomlms.data.database

/**
 * Migration states for handling the transition from legacy unencrypted SQLite
 * to encrypted SQLCipher storage.
 */
enum class MigrationState {
    /** State A: Fresh install (neither legacy nor encrypted DB exists). */
    FIRST_INSTALL,

    /** State B: Encrypted DB exists and no migration is pending. */
    NORMAL_ENCRYPTED,

    /** State C: Legacy unencrypted DB exists, and encrypted DB is missing or empty. */
    MIGRATION_REQUIRED,

    /** State D: Both databases exist with data; prefer encrypted and preserve legacy as backup. */
    BOTH_EXIST_PREFER_ENCRYPTED
}

/**
 * Pure decision evaluator for database upgrade and encryption migration.
 * Decoupled from native Android / SQLCipher drivers to ensure deterministic unit testing in JVM.
 */
object DatabaseMigrationEvaluator {
    const val LEGACY_DB_NAME = "classroom_lms_db"
    const val ENCRYPTED_DB_NAME = "classroom_lms_db_encrypted"
    const val LEGACY_BACKUP_DB_NAME = "classroom_lms_db.bak"
    const val KEY_MIGRATION_V1_DONE = "migration_unencrypted_to_encrypted_v1_done"

    /**
     * Evaluates the migration state based on database existence flags and preference state.
     *
     * @param legacyDbExists True if the unencrypted database file exists on disk.
     * @param encryptedDbExists True if the encrypted database file exists on disk.
     * @param isMigrationDoneInPrefs True if secure preferences record that migration already finished.
     * @param encryptedDbHasData True if the encrypted database contains existing user data (not empty/new).
     */
    fun evaluate(
        legacyDbExists: Boolean,
        encryptedDbExists: Boolean,
        isMigrationDoneInPrefs: Boolean,
        encryptedDbHasData: Boolean = false
    ): MigrationState {
        // If migration was already recorded as successfully completed, always open encrypted DB
        if (isMigrationDoneInPrefs) {
            return MigrationState.NORMAL_ENCRYPTED
        }

        // State A: First install (neither DB file exists)
        if (!legacyDbExists && !encryptedDbExists) {
            return MigrationState.FIRST_INSTALL
        }

        // State B: Only encrypted DB exists (fresh post-encryption install)
        if (!legacyDbExists && encryptedDbExists) {
            return MigrationState.NORMAL_ENCRYPTED
        }

        // State C: Legacy DB exists, and encrypted DB is either missing or empty
        if (legacyDbExists && (!encryptedDbExists || !encryptedDbHasData)) {
            return MigrationState.MIGRATION_REQUIRED
        }

        // State D: Both databases exist and encrypted DB already has user data
        return MigrationState.BOTH_EXIST_PREFER_ENCRYPTED
    }
}

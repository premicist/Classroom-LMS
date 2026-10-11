package com.example.data.database

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import android.util.Base64

object DatabaseSecurityHelper {
    const val PREFS_NAME = "db_secure_prefs"
    const val KEY_PASSPHRASE = "db_passphrase"
    const val KEY_MIGRATION_V1_DONE = "migration_unencrypted_to_encrypted_v1_done"

    private fun getSecurePrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getOrGeneratePassphrase(context: Context): ByteArray {
        val sharedPreferences = getSecurePrefs(context)

        var passphraseBase64 = sharedPreferences.getString(KEY_PASSPHRASE, null)
        if (passphraseBase64 == null) {
            val random = SecureRandom()
            val newPassphrase = ByteArray(32)
            random.nextBytes(newPassphrase)
            passphraseBase64 = Base64.encodeToString(newPassphrase, Base64.DEFAULT)
            sharedPreferences.edit().putString(KEY_PASSPHRASE, passphraseBase64).apply()
        }

        return Base64.decode(passphraseBase64, Base64.DEFAULT)
    }

    fun isMigrationV1Completed(context: Context): Boolean {
        return try {
            getSecurePrefs(context).getBoolean(KEY_MIGRATION_V1_DONE, false)
        } catch (_: Exception) {
            false
        }
    }

    fun setMigrationV1Completed(context: Context, completed: Boolean = true) {
        try {
            getSecurePrefs(context).edit().putBoolean(KEY_MIGRATION_V1_DONE, completed).apply()
        } catch (_: Exception) {
            // Secure prefs update failure is non-fatal; legacy file rename serves as physical fallback
        }
    }
}

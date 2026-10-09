package com.example.data.database

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import android.util.Base64

object DatabaseSecurityHelper {
    private const val PREFS_NAME = "db_secure_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase"

    fun getOrGeneratePassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

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
}

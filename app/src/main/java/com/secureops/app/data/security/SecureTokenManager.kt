package com.secureops.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber

class SecureTokenManager(
    private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Encrypts and stores a token
     */
    fun storeToken(accountId: String, token: String) {
        try {
            encryptedPrefs.edit()
                .putString("token_$accountId", token)
                .apply()
            Timber.d("Token stored securely for account: $accountId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to store token")
            throw e
        }
    }

    /**
     * Retrieves and decrypts a token
     */
    fun getToken(accountId: String): String? {
        return try {
            encryptedPrefs.getString("token_$accountId", null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve token")
            null
        }
    }

    /**
     * Removes a stored token
     */
    fun removeToken(accountId: String) {
        try {
            encryptedPrefs.edit()
                .remove("token_$accountId")
                .apply()
            Timber.d("Token removed for account: $accountId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove token")
        }
    }

    /**
     * Clears all stored tokens
     */
    fun clearAllTokens() {
        try {
            encryptedPrefs.edit().clear().apply()
            Timber.d("All tokens cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear tokens")
        }
    }
}

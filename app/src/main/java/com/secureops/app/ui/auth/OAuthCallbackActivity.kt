package com.secureops.app.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.secureops.app.data.auth.OAuth2Manager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Activity to handle OAuth callback redirects
 */
class OAuthCallbackActivity : ComponentActivity() {

    private val oauth2Manager: OAuth2Manager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        if (uri != null) {
            handleOAuthCallback(uri)
        } else {
            Timber.e("OAuth callback received with no data")
            finish()
        }
    }

    private fun handleOAuthCallback(uri: Uri) {
        lifecycleScope.launch {
            try {
                val result = oauth2Manager.handleCallback(uri)
                
                result.fold(
                    onSuccess = { token ->
                        // Store token and return to AddAccountScreen
                        val resultIntent = Intent().apply {
                            putExtra("access_token", token.accessToken)
                            putExtra("refresh_token", token.refreshToken)
                            putExtra("expires_in", token.expiresIn)
                        }
                        setResult(RESULT_OK, resultIntent)
                    },
                    onFailure = { error ->
                        Timber.e(error, "OAuth callback failed")
                        val resultIntent = Intent().apply {
                            putExtra("error", error.message)
                        }
                        setResult(RESULT_CANCELED, resultIntent)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error handling OAuth callback")
                setResult(RESULT_CANCELED)
            } finally {
                finish()
            }
        }
    }
}

package com.secureops.app.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.secureops.app.domain.model.CIProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import org.json.JSONObject
import java.util.UUID

/**
 * OAuth2 Manager for CI/CD providers
 * Handles OAuth2 authentication flow
 */
class OAuth2Manager(
    private val context: Context
) {
    private val client = OkHttpClient()
    private val pendingAuthRequests = mutableMapOf<String, (Result<OAuthToken>) -> Unit>()

    companion object {
        // OAuth2 client IDs (you'll need to register apps with each provider)
        const val GITHUB_CLIENT_ID = "your_github_client_id"
        const val GITLAB_CLIENT_ID = "your_gitlab_client_id"

        // Redirect URI (must match in OAuth app settings)
        const val REDIRECT_URI = "secureops://oauth/callback"
    }

    /**
     * Start OAuth2 flow for a provider
     */
    suspend fun authenticate(provider: CIProvider): Result<OAuthToken> {
        return suspendCancellableCoroutine { continuation ->
            val state = UUID.randomUUID().toString()

            // Store callback
            pendingAuthRequests[state] = { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }

            // Build authorization URL
            val authUrl = buildAuthorizationUrl(provider, state)

            // Launch Chrome Custom Tabs
            try {
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()

                customTabsIntent.launchUrl(context, Uri.parse(authUrl))
            } catch (e: Exception) {
                Timber.e(e, "Failed to launch OAuth flow")
                pendingAuthRequests.remove(state)
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }

            // Handle cancellation
            continuation.invokeOnCancellation {
                pendingAuthRequests.remove(state)
            }
        }
    }

    /**
     * Handle OAuth callback with authorization code
     */
    suspend fun handleCallback(uri: Uri): Result<OAuthToken> {
        return try {
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")
            val error = uri.getQueryParameter("error")

            if (error != null) {
                return Result.failure(Exception("OAuth error: $error"))
            }

            if (code == null || state == null) {
                return Result.failure(Exception("Invalid OAuth callback"))
            }

            // Determine provider from state or URL
            val provider = determineProviderFromCallback(uri)

            // Exchange code for token
            exchangeCodeForToken(provider, code)
        } catch (e: Exception) {
            Timber.e(e, "OAuth callback error")
            Result.failure(e)
        }
    }

    /**
     * Build authorization URL for provider
     */
    private fun buildAuthorizationUrl(provider: CIProvider, state: String): String {
        return when (provider) {
            CIProvider.GITHUB_ACTIONS -> {
                "https://github.com/login/oauth/authorize" +
                        "?client_id=$GITHUB_CLIENT_ID" +
                        "&redirect_uri=$REDIRECT_URI" +
                        "&scope=repo,workflow,read:org" +
                        "&state=$state"
            }

            CIProvider.GITLAB_CI -> {
                "https://gitlab.com/oauth/authorize" +
                        "?client_id=$GITLAB_CLIENT_ID" +
                        "&redirect_uri=$REDIRECT_URI" +
                        "&response_type=code" +
                        "&scope=api,read_api,read_repository" +
                        "&state=$state"
            }

            CIProvider.AZURE_DEVOPS -> {
                "https://app.vssps.visualstudio.com/oauth2/authorize" +
                        "?client_id=$GITLAB_CLIENT_ID" +
                        "&response_type=Assertion" +
                        "&redirect_uri=$REDIRECT_URI" +
                        "&scope=vso.build" +
                        "&state=$state"
            }

            else -> {
                throw IllegalArgumentException("OAuth not supported for $provider")
            }
        }
    }

    /**
     * Exchange authorization code for access token
     */
    private suspend fun exchangeCodeForToken(
        provider: CIProvider,
        code: String
    ): Result<OAuthToken> {
        return try {
            val (tokenUrl, clientId, clientSecret) = getTokenEndpoint(provider)

            val requestBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .apply {
                    when (provider) {
                        CIProvider.GITHUB_ACTIONS -> add("grant_type", "authorization_code")
                        CIProvider.GITLAB_CI -> add("grant_type", "authorization_code")
                        else -> {}
                    }
                }
                .build()

            val request = Request.Builder()
                .url(tokenUrl)
                .post(requestBody)
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return Result.failure(Exception("Token exchange failed: ${response.code}"))
            }

            val json = JSONObject(responseBody)
            val accessToken = json.getString("access_token")
            val refreshToken = json.optString("refresh_token", null)
            val expiresIn = json.optLong("expires_in", 0)

            val token = OAuthToken(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = expiresIn,
                tokenType = json.optString("token_type", "Bearer"),
                scope = json.optString("scope", "")
            )

            Result.success(token)
        } catch (e: Exception) {
            Timber.e(e, "Token exchange error")
            Result.failure(e)
        }
    }

    /**
     * Get token endpoint and credentials for provider
     */
    private fun getTokenEndpoint(provider: CIProvider): Triple<String, String, String> {
        return when (provider) {
            CIProvider.GITHUB_ACTIONS -> Triple(
                "https://github.com/login/oauth/access_token",
                GITHUB_CLIENT_ID,
                "your_github_client_secret" // Store securely
            )

            CIProvider.GITLAB_CI -> Triple(
                "https://gitlab.com/oauth/token",
                GITLAB_CLIENT_ID,
                "your_gitlab_client_secret" // Store securely
            )

            CIProvider.AZURE_DEVOPS -> Triple(
                "https://app.vssps.visualstudio.com/oauth2/token",
                GITLAB_CLIENT_ID,
                "your_azure_client_secret" // Store securely
            )

            else -> throw IllegalArgumentException("OAuth not supported for $provider")
        }
    }

    /**
     * Determine provider from callback URL
     */
    private fun determineProviderFromCallback(uri: Uri): CIProvider {
        // You could pass provider info in the state parameter
        // For now, detect from URL patterns
        val state = uri.getQueryParameter("state") ?: ""

        return when {
            uri.toString().contains("github") -> CIProvider.GITHUB_ACTIONS
            uri.toString().contains("gitlab") -> CIProvider.GITLAB_CI
            uri.toString().contains("visualstudio") -> CIProvider.AZURE_DEVOPS
            else -> CIProvider.GITHUB_ACTIONS // Default
        }
    }

    /**
     * Refresh an expired OAuth token
     */
    suspend fun refreshToken(
        provider: CIProvider,
        refreshToken: String
    ): Result<OAuthToken> {
        return try {
            val (tokenUrl, clientId, clientSecret) = getTokenEndpoint(provider)

            val requestBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .build()

            val request = Request.Builder()
                .url(tokenUrl)
                .post(requestBody)
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return Result.failure(Exception("Token refresh failed: ${response.code}"))
            }

            val json = JSONObject(responseBody)
            val accessToken = json.getString("access_token")
            val newRefreshToken = json.optString("refresh_token", refreshToken)
            val expiresIn = json.optLong("expires_in", 0)

            val token = OAuthToken(
                accessToken = accessToken,
                refreshToken = newRefreshToken,
                expiresIn = expiresIn,
                tokenType = json.optString("token_type", "Bearer"),
                scope = json.optString("scope", "")
            )

            Result.success(token)
        } catch (e: Exception) {
            Timber.e(e, "Token refresh error")
            Result.failure(e)
        }
    }
}

/**
 * OAuth access token
 */
data class OAuthToken(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long, // seconds
    val tokenType: String = "Bearer",
    val scope: String = "",
    val issuedAt: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean {
        if (expiresIn == 0L) return false // Never expires
        val expiresAt = issuedAt + (expiresIn * 1000)
        return System.currentTimeMillis() >= expiresAt
    }
}

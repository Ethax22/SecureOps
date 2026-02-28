package com.secureops.app.data.offline

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import java.io.IOException

/**
 * Offline Interceptor
 * 
 * OkHttp interceptor that blocks network requests when offline mode is enabled
 * Simulates network unavailability for testing offline resilience
 */
class OfflineInterceptor(
    private val offlineSimulator: OfflineSimulator
) : Interceptor {
    
    companion object {
        private const val OFFLINE_ERROR_MESSAGE = "Network unavailable - Offline mode enabled"
        private const val OFFLINE_ERROR_CODE = 503 // Service Unavailable
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Check if offline mode is enabled
        if (offlineSimulator.isOffline()) {
            // Block the request
            offlineSimulator.recordBlockedRequest(request.url.toString())
            
            Timber.w("🚫 [OFFLINE] Blocking request: ${request.method} ${request.url}")
            
            // Return synthetic offline response
            return createOfflineResponse(chain)
        }
        
        // Proceed with normal request
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            Timber.e(e, "Network request failed: ${request.url}")
            throw e
        }
    }
    
    /**
     * Create a synthetic offline response
     */
    private fun createOfflineResponse(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Create error response body
        val errorBody = """
            {
                "error": "offline_mode_enabled",
                "message": "$OFFLINE_ERROR_MESSAGE",
                "url": "${request.url}",
                "method": "${request.method}",
                "timestamp": ${System.currentTimeMillis()}
            }
        """.trimIndent()
        
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(OFFLINE_ERROR_CODE)
            .message("Offline Mode - Service Unavailable")
            .body(
                errorBody.toResponseBody("application/json".toMediaType())
            )
            .build()
    }
}

/**
 * Offline Exception
 * 
 * Custom exception thrown when offline mode blocks a request
 */
class OfflineModeException(
    message: String = "Network request blocked - Offline mode is enabled"
) : IOException(message)

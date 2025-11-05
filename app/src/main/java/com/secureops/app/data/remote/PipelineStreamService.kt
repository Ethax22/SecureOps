package com.secureops.app.data.remote

import com.secureops.app.domain.model.CIProvider
import com.secureops.app.domain.model.Pipeline
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okio.ByteString
import timber.log.Timber
import java.util.concurrent.TimeUnit

class PipelineStreamService {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Stream build logs in real-time
     */
    fun streamBuildLogs(
        pipeline: Pipeline,
        authToken: String
    ): Flow<LogEntry> = callbackFlow {
        val url = getBuildLogStreamUrl(pipeline)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", getAuthHeader(pipeline.provider, authToken))
            .build()

        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocket opened for pipeline ${pipeline.id}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val logEntry = LogEntry(
                    timestamp = System.currentTimeMillis(),
                    message = text,
                    level = detectLogLevel(text)
                )
                trySend(logEntry)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val text = bytes.utf8()
                val logEntry = LogEntry(
                    timestamp = System.currentTimeMillis(),
                    message = text,
                    level = detectLogLevel(text)
                )
                trySend(logEntry)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "WebSocket failure")
                close(t)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closing: $code - $reason")
                webSocket.close(1000, null)
                close()
            }
        })

        awaitClose {
            webSocket.close(1000, "Client closed connection")
        }
    }

    /**
     * Stream build progress in real-time
     */
    fun streamBuildProgress(
        pipeline: Pipeline,
        authToken: String
    ): Flow<BuildProgress> = callbackFlow {
        val url = getBuildProgressUrl(pipeline)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", getAuthHeader(pipeline.provider, authToken))
            .header("Accept", "text/event-stream")
            .build()

        // For SSE (Server-Sent Events), we use regular HTTP call
        val call = client.newCall(request)

        try {
            val response = call.execute()
            val source = response.body?.source()

            if (source != null) {
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break

                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ")
                        val progress = parseBuildProgress(data, pipeline)
                        trySend(progress)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SSE stream error")
            close(e)
        }

        awaitClose {
            call.cancel()
        }
    }

    /**
     * Get WebSocket/SSE URL for build logs
     */
    private fun getBuildLogStreamUrl(pipeline: Pipeline): String {
        return when (pipeline.provider) {
            CIProvider.GITHUB_ACTIONS -> {
                val parts = pipeline.repositoryUrl.split("/")
                val owner = parts.getOrNull(parts.size - 2) ?: ""
                val repo = parts.getOrNull(parts.size - 1) ?: ""
                "wss://api.github.com/repos/$owner/$repo/actions/runs/${pipeline.id}/logs"
            }

            CIProvider.GITLAB_CI -> {
                val projectId = pipeline.repositoryUrl.substringAfterLast("/")
                "wss://gitlab.com/api/v4/projects/$projectId/jobs/${pipeline.id}/trace"
            }

            CIProvider.JENKINS -> {
                "ws://${pipeline.repositoryUrl}/job/${pipeline.repositoryName}/${pipeline.buildNumber}/logText/progressiveText"
            }

            CIProvider.CIRCLE_CI -> {
                "wss://circleci.com/api/v2/workflow/${pipeline.id}/logs"
            }

            CIProvider.AZURE_DEVOPS -> {
                val parts = pipeline.repositoryUrl.split("/")
                val org = parts.getOrNull(parts.size - 2) ?: ""
                val project = parts.getOrNull(parts.size - 1) ?: ""
                "wss://dev.azure.com/$org/$project/_apis/build/builds/${pipeline.id}/logs/stream"
            }
        }
    }

    /**
     * Get URL for build progress stream
     */
    private fun getBuildProgressUrl(pipeline: Pipeline): String {
        return when (pipeline.provider) {
            CIProvider.GITHUB_ACTIONS -> {
                val parts = pipeline.repositoryUrl.split("/")
                val owner = parts.getOrNull(parts.size - 2) ?: ""
                val repo = parts.getOrNull(parts.size - 1) ?: ""
                "https://api.github.com/repos/$owner/$repo/actions/runs/${pipeline.id}"
            }

            CIProvider.GITLAB_CI -> {
                val projectId = pipeline.repositoryUrl.substringAfterLast("/")
                "https://gitlab.com/api/v4/projects/$projectId/pipelines/${pipeline.id}"
            }

            else -> ""
        }
    }

    /**
     * Get authorization header based on provider
     */
    private fun getAuthHeader(provider: CIProvider, token: String): String {
        return when (provider) {
            CIProvider.GITHUB_ACTIONS -> "Bearer $token"
            CIProvider.GITLAB_CI -> "Bearer $token"
            CIProvider.JENKINS -> "Basic $token"
            CIProvider.CIRCLE_CI -> "Circle-Token $token"
            CIProvider.AZURE_DEVOPS -> "Bearer $token"
        }
    }

    /**
     * Detect log level from message
     */
    private fun detectLogLevel(message: String): LogLevel {
        val lowerMessage = message.lowercase()
        return when {
            lowerMessage.contains("error") || lowerMessage.contains("fatal") -> LogLevel.ERROR
            lowerMessage.contains("warn") || lowerMessage.contains("warning") -> LogLevel.WARNING
            lowerMessage.contains("info") -> LogLevel.INFO
            lowerMessage.contains("debug") -> LogLevel.DEBUG
            else -> LogLevel.INFO
        }
    }

    /**
     * Parse build progress from SSE data
     */
    private fun parseBuildProgress(data: String, pipeline: Pipeline): BuildProgress {
        // Simplified parsing - in production, use proper JSON parsing
        return BuildProgress(
            pipelineId = pipeline.id,
            currentStep = extractStep(data),
            totalSteps = extractTotalSteps(data),
            status = extractStatus(data),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun extractStep(data: String): Int {
        // Parse step number from data
        return 1 // Simplified
    }

    private fun extractTotalSteps(data: String): Int {
        // Parse total steps from data
        return 5 // Simplified
    }

    private fun extractStatus(data: String): String {
        // Parse status from data
        return "running" // Simplified
    }
}

/**
 * Log entry from build stream
 */
data class LogEntry(
    val timestamp: Long,
    val message: String,
    val level: LogLevel
)

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR
}

/**
 * Build progress information
 */
data class BuildProgress(
    val pipelineId: String,
    val currentStep: Int,
    val totalSteps: Int,
    val status: String,
    val timestamp: Long
)

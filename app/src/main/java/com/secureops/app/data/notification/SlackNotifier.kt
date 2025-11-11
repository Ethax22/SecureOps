package com.secureops.app.data.notification

import com.secureops.app.domain.model.Pipeline
import com.secureops.app.domain.model.BuildStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class SlackNotifier(
    private val client: OkHttpClient
) {
    /**
     * Send Slack notification via webhook
     */
    suspend fun sendNotification(
        webhookUrl: String,
        pipeline: Pipeline,
        message: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = buildSlackMessage(pipeline, message)

            val requestBody = json.toString().toRequestBody(
                "application/json".toMediaType()
            )

            val request = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Timber.d("Slack notification sent successfully")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Slack API error: ${response.code}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Slack notification failed")
            Result.failure(e)
        }
    }

    private fun buildSlackMessage(pipeline: Pipeline, message: String): JSONObject {
        return JSONObject().apply {
            put("text", message)
            put("blocks", JSONArray().apply {
                // Header
                put(JSONObject().apply {
                    put("type", "header")
                    put("text", JSONObject().apply {
                        put("type", "plain_text")
                        put("text", getStatusEmoji(pipeline.status) + " Build ${pipeline.status}")
                    })
                })

                // Details
                put(JSONObject().apply {
                    put("type", "section")
                    put("fields", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "mrkdwn")
                            put("text", "*Repository:*\n${pipeline.repositoryName}")
                        })
                        put(JSONObject().apply {
                            put("type", "mrkdwn")
                            put("text", "*Build:*\n#${pipeline.buildNumber}")
                        })
                        put(JSONObject().apply {
                            put("type", "mrkdwn")
                            put("text", "*Branch:*\n${pipeline.branch}")
                        })
                        put(JSONObject().apply {
                            put("type", "mrkdwn")
                            put("text", "*Status:*\n${pipeline.status}")
                        })
                    })
                })

                // Commit info if available
                if (pipeline.commitMessage.isNotEmpty()) {
                    put(JSONObject().apply {
                        put("type", "section")
                        put("text", JSONObject().apply {
                            put("type", "mrkdwn")
                            put("text", "*Commit:*\n${pipeline.commitMessage}")
                        })
                    })
                }

                // Actions
                if (pipeline.webUrl.isNotEmpty()) {
                    put(JSONObject().apply {
                        put("type", "actions")
                        put("elements", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "button")
                                put("text", JSONObject().apply {
                                    put("type", "plain_text")
                                    put("text", "View Build")
                                })
                                put("url", pipeline.webUrl)
                                put(
                                    "style", when (pipeline.status) {
                                        BuildStatus.SUCCESS -> "primary"
                                        BuildStatus.FAILURE -> "danger"
                                        else -> ""
                                    }
                                )
                            })
                        })
                    })
                }
            })
        }
    }

    private fun getStatusEmoji(status: BuildStatus): String {
        return when (status) {
            BuildStatus.SUCCESS -> "✅"
            BuildStatus.FAILURE -> "❌"
            BuildStatus.RUNNING -> "🔄"
            BuildStatus.PENDING -> "⏳"
            BuildStatus.CANCELED -> "🚫"
            BuildStatus.QUEUED -> "⏸️"
            BuildStatus.SKIPPED -> "⏭️"
            BuildStatus.UNKNOWN -> "❓"
        }
    }
}

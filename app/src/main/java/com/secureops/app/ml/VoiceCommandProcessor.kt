package com.secureops.app.ml

import com.secureops.app.domain.model.CommandIntent
import com.secureops.app.domain.model.VoiceCommand
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandProcessor @Inject constructor() {

    /**
     * Process voice input text and extract intent and parameters
     */
    fun processVoiceInput(text: String): VoiceCommand {
        val normalizedText = text.lowercase().trim()
        Timber.d("Processing voice command: $normalizedText")

        val intent = detectIntent(normalizedText)
        val parameters = extractParameters(normalizedText, intent)

        return VoiceCommand(
            rawText = text,
            intent = intent,
            parameters = parameters
        )
    }

    private fun detectIntent(text: String): CommandIntent {
        return when {
            // Query build status
            text.contains("status") && (text.contains("build") || text.contains("pipeline")) -> {
                CommandIntent.QUERY_BUILD_STATUS
            }

            text.contains("how") && text.contains("doing") -> {
                CommandIntent.QUERY_BUILD_STATUS
            }

            // Explain failure
            text.contains("why") && (text.contains("fail") || text.contains("broke")) -> {
                CommandIntent.EXPLAIN_FAILURE
            }

            text.contains("what") && text.contains("wrong") -> {
                CommandIntent.EXPLAIN_FAILURE
            }

            text.contains("what happened") -> {
                CommandIntent.EXPLAIN_FAILURE
            }

            // Check risky deployments
            text.contains("risky") || text.contains("risk") -> {
                CommandIntent.CHECK_RISKY_DEPLOYMENTS
            }

            text.contains("dangerous") || text.contains("unstable") -> {
                CommandIntent.CHECK_RISKY_DEPLOYMENTS
            }

            // Rerun build
            text.contains("rerun") || text.contains("re-run") || text.contains("retry") -> {
                CommandIntent.RERUN_BUILD
            }

            text.contains("run again") || text.contains("try again") -> {
                CommandIntent.RERUN_BUILD
            }

            // Rollback
            text.contains("rollback") || text.contains("roll back") -> {
                CommandIntent.ROLLBACK_DEPLOYMENT
            }

            text.contains("revert") || text.contains("undo") -> {
                CommandIntent.ROLLBACK_DEPLOYMENT
            }

            // Notify team
            text.contains("notify") || text.contains("alert") || text.contains("tell") -> {
                CommandIntent.NOTIFY_TEAM
            }

            text.contains("send") && (text.contains("team") || text.contains("slack")) -> {
                CommandIntent.NOTIFY_TEAM
            }

            else -> CommandIntent.UNKNOWN
        }
    }

    private fun extractParameters(text: String, intent: CommandIntent): Map<String, String> {
        val params = mutableMapOf<String, String>()

        // Extract build number
        val buildNumberPattern = Regex("build[\\s#]*(\\d+)", RegexOption.IGNORE_CASE)
        buildNumberPattern.find(text)?.let { match ->
            params["buildNumber"] = match.groupValues[1]
        }

        // Extract repository name
        val repoPattern = Regex("(?:repo|repository|project)\\s+([\\w-]+)", RegexOption.IGNORE_CASE)
        repoPattern.find(text)?.let { match ->
            params["repository"] = match.groupValues[1]
        }

        // Extract time references
        when {
            text.contains("today") -> params["timeRange"] = "today"
            text.contains("yesterday") -> params["timeRange"] = "yesterday"
            text.contains("this week") -> params["timeRange"] = "week"
            text.contains("last") && text.contains("hour") -> params["timeRange"] = "hour"
        }

        // Extract qualifiers
        when {
            text.contains("last") && text.contains("fail") -> params["target"] = "last_failed"
            text.contains("latest") -> params["target"] = "latest"
            text.contains("all") -> params["target"] = "all"
        }

        // For notification intent, extract channel
        if (intent == CommandIntent.NOTIFY_TEAM) {
            when {
                text.contains("slack") -> params["channel"] = "slack"
                text.contains("email") -> params["channel"] = "email"
                text.contains("both") -> params["channel"] = "both"
            }
        }

        return params
    }

    /**
     * Generate natural language response
     */
    fun generateResponse(
        intent: CommandIntent,
        data: Map<String, Any>,
        success: Boolean = true
    ): String {
        return when (intent) {
            CommandIntent.QUERY_BUILD_STATUS -> {
                val totalBuilds = data["totalBuilds"] as? Int ?: 0
                val failedBuilds = data["failedBuilds"] as? Int ?: 0
                val runningBuilds = data["runningBuilds"] as? Int ?: 0

                when {
                    failedBuilds > 0 -> {
                        "You have $failedBuilds failed build${if (failedBuilds > 1) "s" else ""} " +
                                "out of $totalBuilds total. ${if (runningBuilds > 0) "$runningBuilds currently running." else ""}"
                    }

                    runningBuilds > 0 -> {
                        "All builds are healthy! $runningBuilds build${if (runningBuilds > 1) "s are" else " is"} currently running."
                    }

                    else -> {
                        "All $totalBuilds builds are successful. Everything looks good!"
                    }
                }
            }

            CommandIntent.EXPLAIN_FAILURE -> {
                val buildNumber = data["buildNumber"] as? String
                val reason = data["reason"] as? String ?: "Unknown reason"

                "Build ${buildNumber ?: "the last one"} failed because: $reason"
            }

            CommandIntent.CHECK_RISKY_DEPLOYMENTS -> {
                val riskyCount = data["riskyCount"] as? Int ?: 0

                if (riskyCount > 0) {
                    "I found $riskyCount deployment${if (riskyCount > 1) "s" else ""} with high failure risk. " +
                            "You should review ${if (riskyCount > 1) "them" else "it"} before deploying."
                } else {
                    "Good news! No risky deployments detected. All systems look stable."
                }
            }

            CommandIntent.RERUN_BUILD -> {
                if (success) {
                    val buildNumber = data["buildNumber"] as? String
                    "I've restarted build ${buildNumber ?: ""}. You'll get a notification when it completes."
                } else {
                    "I couldn't rerun the build. ${data["error"] ?: "Please try again."}"
                }
            }

            CommandIntent.ROLLBACK_DEPLOYMENT -> {
                if (success) {
                    "Rollback initiated successfully. Reverting to the last stable version."
                } else {
                    "Unable to rollback. ${data["error"] ?: "Check your permissions."}"
                }
            }

            CommandIntent.NOTIFY_TEAM -> {
                if (success) {
                    val channel = data["channel"] as? String ?: "the team"
                    "Notification sent to $channel successfully."
                } else {
                    "Failed to send notification. ${data["error"] ?: ""}"
                }
            }

            CommandIntent.UNKNOWN -> {
                "I'm not sure what you want me to do. Try asking about build status, failures, or say 'rerun the last failed build'."
            }
        }
    }
}

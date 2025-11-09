package com.secureops.app.ml

import com.secureops.app.domain.model.CommandIntent
import com.secureops.app.domain.model.VoiceCommand
import timber.log.Timber

/**
 * Processes voice commands and extracts intent
 */
class VoiceCommandProcessor {

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
            // Query build status - Enhanced patterns
            text.contains("status") && (text.contains("build") || text.contains("pipeline")) -> {
                CommandIntent.QUERY_BUILD_STATUS
            }
            text.contains("how") && (text.contains("doing") || text.contains("is") || text.contains("are")) && 
                (text.contains("build") || text.contains("pipeline") || text.contains("project")) -> {
                CommandIntent.QUERY_BUILD_STATUS
            }
            text.matches(Regex(".*show.*builds?.*")) -> CommandIntent.QUERY_BUILD_STATUS
            text.matches(Regex(".*list.*pipelines?.*")) -> CommandIntent.QUERY_BUILD_STATUS
            text.contains("what's running") || text.contains("what is running") -> CommandIntent.QUERY_BUILD_STATUS
            text.contains("current") && (text.contains("build") || text.contains("pipeline")) -> CommandIntent.QUERY_BUILD_STATUS
            text.contains("latest") && (text.contains("build") || text.contains("pipeline")) -> CommandIntent.QUERY_BUILD_STATUS
            
            // Explain failure - Enhanced patterns
            text.contains("why") && (text.contains("fail") || text.contains("broke") || text.contains("error")) -> {
                CommandIntent.EXPLAIN_FAILURE
            }
            text.contains("what") && (text.contains("wrong") || text.contains("error") || text.contains("problem")) -> {
                CommandIntent.EXPLAIN_FAILURE
            }
            text.contains("what happened") -> CommandIntent.EXPLAIN_FAILURE
            text.contains("explain") && (text.contains("failure") || text.contains("error")) -> {
                CommandIntent.EXPLAIN_FAILURE
            }
            text.contains("tell me about") && (text.contains("failure") || text.contains("error")) -> {
                CommandIntent.EXPLAIN_FAILURE
            }
            text.matches(Regex(".*what caused.*fail.*")) -> CommandIntent.EXPLAIN_FAILURE
            text.contains("root cause") -> CommandIntent.EXPLAIN_FAILURE
            
            // Check risky deployments - Enhanced patterns
            text.contains("risky") || text.contains("risk") -> {
                CommandIntent.CHECK_RISKY_DEPLOYMENTS
            }
            text.contains("dangerous") || text.contains("unstable") -> {
                CommandIntent.CHECK_RISKY_DEPLOYMENTS
            }
            text.contains("high risk") || text.contains("critical") -> {
                CommandIntent.CHECK_RISKY_DEPLOYMENTS
            }
            text.matches(Regex(".*check.*risk.*")) -> CommandIntent.CHECK_RISKY_DEPLOYMENTS
            text.contains("failure rate") || text.contains("failure prone") -> {
                CommandIntent.CHECK_RISKY_DEPLOYMENTS
            }
            
            // Rerun build - Enhanced patterns
            text.contains("rerun") || text.contains("re-run") || text.contains("retry") -> {
                CommandIntent.RERUN_BUILD
            }
            text.contains("run again") || text.contains("try again") -> {
                CommandIntent.RERUN_BUILD
            }
            text.contains("restart") && (text.contains("build") || text.contains("pipeline")) -> {
                CommandIntent.RERUN_BUILD
            }
            
            // Rollback - Enhanced patterns
            text.contains("rollback") || text.contains("roll back") -> {
                CommandIntent.ROLLBACK_DEPLOYMENT
            }
            text.contains("revert") || text.contains("undo") -> {
                CommandIntent.ROLLBACK_DEPLOYMENT
            }
            text.contains("go back") && text.contains("previous") -> {
                CommandIntent.ROLLBACK_DEPLOYMENT
            }
            
            // Notify team
            text.contains("notify") || text.contains("alert") || text.contains("tell") -> {
                CommandIntent.NOTIFY_TEAM
            }
            text.contains("send") && (text.contains("team") || text.contains("slack")) -> {
                CommandIntent.NOTIFY_TEAM
            }
            
            // Analytics queries
            text.contains("analytics") || text.contains("statistics") || text.contains("stats") -> {
                CommandIntent.QUERY_ANALYTICS
            }
            text.contains("trend") || text.contains("history") -> {
                CommandIntent.QUERY_ANALYTICS
            }
            text.matches(Regex(".*failure rate.*")) && !text.contains("risky") -> {
                CommandIntent.QUERY_ANALYTICS
            }
            text.contains("time to fix") || text.contains("average") -> {
                CommandIntent.QUERY_ANALYTICS
            }
            text.matches(Regex(".*how many.*fail.*")) -> CommandIntent.QUERY_ANALYTICS
            text.matches(Regex(".*how many.*success.*")) -> CommandIntent.QUERY_ANALYTICS
            
            // Repository queries
            text.contains("repository") || text.contains("repo") -> {
                if (text.contains("list") || text.contains("show") || text.contains("all")) {
                    CommandIntent.LIST_REPOSITORIES
                } else {
                    CommandIntent.QUERY_REPOSITORY
                }
            }
            text.matches(Regex(".*which.*repos?.*")) -> CommandIntent.LIST_REPOSITORIES
            text.contains("projects") && !text.contains("about") -> CommandIntent.LIST_REPOSITORIES
            
            // Account queries
            text.contains("account") -> {
                if (text.contains("add") || text.contains("new") || text.contains("create")) {
                    CommandIntent.ADD_ACCOUNT
                } else if (text.contains("list") || text.contains("show")) {
                    CommandIntent.LIST_ACCOUNTS
                } else {
                    CommandIntent.QUERY_ACCOUNT
                }
            }
            text.contains("providers") || text.contains("ci/cd") -> CommandIntent.LIST_ACCOUNTS
            text.contains("github") || text.contains("gitlab") || text.contains("jenkins") || 
                text.contains("circleci") || text.contains("azure") -> CommandIntent.QUERY_ACCOUNT
            
            // CI/CD provider specific queries
            text.contains("jenkins") && (text.contains("job") || text.contains("build")) -> {
                CommandIntent.QUERY_BUILD_STATUS
            }
            text.contains("github actions") || text.contains("workflow") -> {
                CommandIntent.QUERY_BUILD_STATUS
            }
            
            // App information queries
            text.contains("help") || text.contains("what can you do") || text.contains("commands") -> {
                CommandIntent.SHOW_HELP
            }
            text.matches(Regex(".*how.*use.*")) -> CommandIntent.SHOW_HELP
            text.contains("features") || text.contains("capabilities") -> CommandIntent.SHOW_HELP
            
            // General greetings and conversation
            text.matches(Regex("^(hello|hi|hey|good morning|good afternoon|good evening).*")) -> {
                CommandIntent.GREETING
            }
            text.contains("thank") -> CommandIntent.GREETING
            
            // Deployment queries
            text.contains("deployment") -> {
                if (text.contains("last") || text.contains("recent") || text.contains("latest")) {
                    CommandIntent.QUERY_DEPLOYMENT
                } else if (text.contains("when") || text.contains("time")) {
                    CommandIntent.QUERY_DEPLOYMENT
                } else {
                    CommandIntent.CHECK_RISKY_DEPLOYMENTS
                }
            }
            
            // Branch queries
            text.contains("branch") -> CommandIntent.QUERY_BRANCH
            text.contains("main branch") || text.contains("master branch") -> CommandIntent.QUERY_BRANCH
            
            // Duration and time queries
            text.contains("how long") || text.contains("duration") -> {
                CommandIntent.QUERY_DURATION
            }
            text.contains("fastest") || text.contains("slowest") -> CommandIntent.QUERY_DURATION
            
            // Success rate queries
            text.matches(Regex(".*success rate.*")) -> CommandIntent.QUERY_SUCCESS_RATE
            text.matches(Regex(".*how successful.*")) -> CommandIntent.QUERY_SUCCESS_RATE
            
            // Commit queries
            text.contains("commit") && (text.contains("last") || text.contains("recent")) -> {
                CommandIntent.QUERY_COMMIT
            }
            text.contains("who") && text.contains("committed") -> CommandIntent.QUERY_COMMIT
            
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
            text.contains("this week") || text.contains("week") -> params["timeRange"] = "week"
            text.contains("this month") || text.contains("month") -> params["timeRange"] = "month"
            text.contains("last") && text.contains("hour") -> params["timeRange"] = "hour"
            text.contains("last") && text.contains("day") -> params["timeRange"] = "last_day"
        }

        // Extract qualifiers
        when {
            text.contains("last") && text.contains("fail") -> params["target"] = "last_failed"
            text.contains("latest") -> params["target"] = "latest"
            text.contains("all") -> params["target"] = "all"
            text.contains("recent") -> params["target"] = "recent"
        }

        // For notification intent, extract channel
        if (intent == CommandIntent.NOTIFY_TEAM) {
            when {
                text.contains("slack") -> params["channel"] = "slack"
                text.contains("email") -> params["channel"] = "email"
                text.contains("both") -> params["channel"] = "both"
            }
        }
        
        // Extract provider names
        when {
            text.contains("github") -> params["provider"] = "github"
            text.contains("gitlab") -> params["provider"] = "gitlab"
            text.contains("jenkins") -> params["provider"] = "jenkins"
            text.contains("circleci") || text.contains("circle") -> params["provider"] = "circleci"
            text.contains("azure") -> params["provider"] = "azure"
        }
        
        // Extract branch names
        val branchPattern = Regex("branch\\s+([\\w-/]+)", RegexOption.IGNORE_CASE)
        branchPattern.find(text)?.let { match ->
            params["branch"] = match.groupValues[1]
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
                val successBuilds = data["successBuilds"] as? Int ?: 0

                when {
                    totalBuilds == 0 -> "You don't have any builds configured yet. Add a CI/CD account to start monitoring your pipelines."
                    failedBuilds > 0 -> {
                        "You have $failedBuilds failed build${if (failedBuilds > 1) "s" else ""} " +
                                "out of $totalBuilds total. ${if (runningBuilds > 0) "$runningBuilds currently running." else ""} " +
                                "The rest are successful."
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
                val stepName = data["stepName"] as? String
                val repository = data["repository"] as? String

                buildString {
                    append("Build ${buildNumber ?: "the last one"} ")
                    repository?.let { append("in $it ") }
                    append("failed because: $reason")
                    stepName?.let { append(" The failure occurred in the $it step.") }
                }
            }

            CommandIntent.CHECK_RISKY_DEPLOYMENTS -> {
                val riskyCount = data["riskyCount"] as? Int ?: 0
                val repositories = data["repositories"] as? List<String>

                if (riskyCount > 0) {
                    buildString {
                        append("I found $riskyCount deployment${if (riskyCount > 1) "s" else ""} with high failure risk. ")
                        repositories?.let { 
                            if (it.isNotEmpty()) {
                                append("The risky repositories are: ${it.joinToString(", ")}. ")
                            }
                        }
                        append("You should review ${if (riskyCount > 1) "them" else "it"} before deploying.")
                    }
                } else {
                    "Good news! No risky deployments detected. All systems look stable."
                }
            }

            CommandIntent.RERUN_BUILD -> {
                if (success) {
                    val buildNumber = data["buildNumber"] as? String
                    val repository = data["repository"] as? String
                    buildString {
                        append("I've restarted build ${buildNumber ?: ""}")
                        repository?.let { append(" for $it") }
                        append(". You'll get a notification when it completes.")
                    }
                } else {
                    "I couldn't rerun the build. ${data["error"] ?: "Please try again."}"
                }
            }

            CommandIntent.ROLLBACK_DEPLOYMENT -> {
                if (success) {
                    val version = data["version"] as? String
                    "Rollback initiated successfully. Reverting to ${version?.let { "version $it" } ?: "the last stable version"}."
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
            
            CommandIntent.QUERY_ANALYTICS -> {
                val failureRate = data["failureRate"] as? Float
                val avgDuration = data["avgDuration"] as? Long
                val totalBuilds = data["totalBuilds"] as? Int ?: 0
                val timeRange = data["timeRange"] as? String ?: "all time"
                
                buildString {
                    append("Analytics for $timeRange: ")
                    append("You have $totalBuilds total builds. ")
                    failureRate?.let { 
                        append("The overall failure rate is ${String.format("%.1f", it)}%. ")
                    }
                    avgDuration?.let {
                        val minutes = it / 60000
                        append("Average build duration is $minutes minutes. ")
                    }
                }
            }
            
            CommandIntent.LIST_REPOSITORIES -> {
                val repositories = data["repositories"] as? List<String> ?: emptyList()
                val count = data["count"] as? Int ?: repositories.size
                
                if (repositories.isEmpty()) {
                    "You don't have any repositories configured yet. Add a CI/CD account to start monitoring."
                } else {
                    buildString {
                        append("You have $count repositor${if (count != 1) "ies" else "y"}: ")
                        append(repositories.take(5).joinToString(", "))
                        if (repositories.size > 5) {
                            append(", and ${repositories.size - 5} more")
                        }
                    }
                }
            }
            
            CommandIntent.QUERY_REPOSITORY -> {
                val repository = data["repository"] as? String ?: "this repository"
                val buildCount = data["buildCount"] as? Int
                val failureRate = data["failureRate"] as? Float
                
                buildString {
                    append("Repository $repository has $buildCount builds. ")
                    failureRate?.let {
                        append("Failure rate is ${String.format("%.1f", it)}%. ")
                    }
                }
            }
            
            CommandIntent.LIST_ACCOUNTS -> {
                val accounts = data["accounts"] as? List<String> ?: emptyList()
                val count = data["count"] as? Int ?: accounts.size
                
                if (accounts.isEmpty()) {
                    "You don't have any CI/CD accounts configured. Go to Settings to add your first account."
                } else {
                    "You have $count CI/CD account${if (count != 1) "s" else ""} connected: ${accounts.joinToString(", ")}"
                }
            }
            
            CommandIntent.QUERY_ACCOUNT -> {
                val account = data["account"] as? String ?: "this account"
                val provider = data["provider"] as? String
                val buildCount = data["buildCount"] as? Int
                
                buildString {
                    append("Account: $account")
                    provider?.let { append(" ($it)") }
                    buildCount?.let { append(". It has $it builds.") }
                }
            }
            
            CommandIntent.SHOW_HELP -> {
                """I can help you with:
                    • Check build status - "Show me my builds"
                    • Explain failures - "Why did build 123 fail?"
                    • Find risky deployments - "Any risky builds?"
                    • View analytics - "Show me statistics"
                    • List repositories - "What repositories do I have?"
                    • Manage accounts - "List my accounts"
                    • Rerun builds - "Rerun the last failed build"
                    • Rollback deployments - "Rollback the deployment"
                    Try asking me anything about your CI/CD pipelines!""".trimIndent()
            }
            
            CommandIntent.GREETING -> {
                val greetings = listOf(
                    "Hello! I'm your SecureOps assistant. How can I help you with your pipelines today?",
                    "Hi there! Ready to check on your builds?",
                    "Hey! What would you like to know about your CI/CD pipelines?",
                    "Hello! I'm here to help with your build monitoring. What do you need?"
                )
                greetings.random()
            }
            
            CommandIntent.QUERY_DEPLOYMENT -> {
                val lastDeployment = data["lastDeployment"] as? String
                val time = data["time"] as? String
                val status = data["status"] as? String
                
                buildString {
                    append("The last deployment ")
                    lastDeployment?.let { append("to $it ") }
                    append("was ")
                    time?.let { append("$it ") }
                    status?.let { append("and it $it") }
                }
            }
            
            CommandIntent.QUERY_BRANCH -> {
                val branch = data["branch"] as? String ?: "main"
                val buildCount = data["buildCount"] as? Int
                val status = data["status"] as? String
                
                buildString {
                    append("Branch $branch ")
                    buildCount?.let { append("has $it builds ") }
                    status?.let { append("with status: $it") }
                }
            }
            
            CommandIntent.QUERY_DURATION -> {
                val avgDuration = data["avgDuration"] as? Long
                val fastestDuration = data["fastestDuration"] as? Long
                val slowestDuration = data["slowestDuration"] as? Long
                
                buildString {
                    avgDuration?.let { 
                        val minutes = it / 60000
                        append("Average build time is $minutes minutes. ")
                    }
                    fastestDuration?.let {
                        val minutes = it / 60000
                        append("Fastest build was $minutes minutes. ")
                    }
                    slowestDuration?.let {
                        val minutes = it / 60000
                        append("Slowest build took $minutes minutes.")
                    }
                }
            }
            
            CommandIntent.QUERY_SUCCESS_RATE -> {
                val successRate = data["successRate"] as? Float ?: 0f
                val totalBuilds = data["totalBuilds"] as? Int ?: 0
                
                "Your overall success rate is ${String.format("%.1f", successRate)}% across $totalBuilds builds."
            }
            
            CommandIntent.QUERY_COMMIT -> {
                val commitHash = data["commitHash"] as? String
                val commitAuthor = data["commitAuthor"] as? String
                val commitMessage = data["commitMessage"] as? String
                
                buildString {
                    append("Last commit ")
                    commitHash?.let { append("(${it.take(8)}) ") }
                    commitAuthor?.let { append("by $it ") }
                    commitMessage?.let { append(": $it") }
                }
            }
            
            CommandIntent.ADD_ACCOUNT -> {
                "To add a new account, go to Settings and tap 'Add Account'. Then select your CI/CD provider and enter your credentials."
            }

            CommandIntent.UNKNOWN -> {
                """I'm not sure what you want me to do. Try asking about:
                    • Build status
                    • Pipeline failures
                    • Analytics and statistics
                    • Repositories and accounts
                    Or say "help" to see all commands.""".trimIndent()
            }
        }
    }
}

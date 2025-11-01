package com.secureops.app.domain.model

data class VoiceCommand(
    val rawText: String,
    val intent: CommandIntent,
    val parameters: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class CommandIntent {
    QUERY_BUILD_STATUS,
    EXPLAIN_FAILURE,
    CHECK_RISKY_DEPLOYMENTS,
    RERUN_BUILD,
    ROLLBACK_DEPLOYMENT,
    NOTIFY_TEAM,
    UNKNOWN
}

data class VoiceResponse(
    val textResponse: String,
    val spokenResponse: String,
    val actionPerformed: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

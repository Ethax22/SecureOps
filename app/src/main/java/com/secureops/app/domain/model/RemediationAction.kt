package com.secureops.app.domain.model

data class RemediationAction(
    val id: String,
    val type: ActionType,
    val pipeline: Pipeline,
    val description: String,
    val requiresConfirmation: Boolean = true,
    val parameters: Map<String, String> = emptyMap()
)

enum class ActionType {
    RERUN_PIPELINE,
    RERUN_FAILED_JOBS,
    ROLLBACK_DEPLOYMENT,
    NOTIFY_SLACK,
    NOTIFY_EMAIL,
    CANCEL_PIPELINE,
    RETRY_WITH_DEBUG
}

data class ActionResult(
    val success: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val details: Map<String, Any> = emptyMap()
)

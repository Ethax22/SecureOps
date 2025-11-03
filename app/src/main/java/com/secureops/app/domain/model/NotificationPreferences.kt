package com.secureops.app.domain.model

data class NotificationPreferences(
    val enabledChannels: Set<NotificationChannelType> = setOf(
        NotificationChannelType.FAILURES,
        NotificationChannelType.HIGH_RISK
    ),
    val riskThreshold: Int = 80,
    val alertOnCriticalOnly: Boolean = false,
    val quietHours: QuietHours? = null,
    val customRules: List<AlertRule> = emptyList(),
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val ledEnabled: Boolean = true
)

enum class NotificationChannelType {
    FAILURES,
    SUCCESS,
    WARNINGS,
    HIGH_RISK,
    BUILD_STARTED,
    BUILD_COMPLETED
}

data class QuietHours(
    val enabled: Boolean = false,
    val startHour: Int = 22, // 10 PM
    val startMinute: Int = 0,
    val endHour: Int = 8, // 8 AM
    val endMinute: Int = 0,
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5) // Mon-Fri
)

data class AlertRule(
    val id: String,
    val name: String,
    val condition: AlertCondition,
    val action: AlertAction,
    val enabled: Boolean = true
)

enum class AlertCondition {
    FAILURE_RATE_ABOVE_THRESHOLD,
    CONSECUTIVE_FAILURES,
    BUILD_DURATION_EXCEEDED,
    SPECIFIC_BRANCH,
    SPECIFIC_REPOSITORY,
    ALL_BUILDS
}

enum class AlertAction {
    NOTIFY,
    NOTIFY_WITH_SOUND,
    SILENT,
    DO_NOT_DISTURB
}

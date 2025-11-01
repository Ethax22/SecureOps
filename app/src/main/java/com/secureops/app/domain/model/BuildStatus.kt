package com.secureops.app.domain.model

enum class BuildStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILURE,
    CANCELED,
    SKIPPED,
    PENDING,
    UNKNOWN
}

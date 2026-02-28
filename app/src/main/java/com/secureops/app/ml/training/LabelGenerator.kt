package com.secureops.app.ml.training

import com.secureops.app.data.local.entity.PipelineEntity
import timber.log.Timber

/**
 * Generates binary labels for ML training
 * SUCCESS/SKIPPED = 0 (negative class)
 * FAILURE/CANCELED = 1 (positive class)
 */
class LabelGenerator {
    companion object {
        const val LABEL_SUCCESS = 0
        const val LABEL_FAILURE = 1
    }

    /**
     * Generate label for a pipeline based on its final status
     * @return 0 for success, 1 for failure, or null if status is indeterminate
     */
    fun generateLabel(pipeline: PipelineEntity): Int? {
        return when (pipeline.status) {
            "SUCCESS", "SKIPPED" -> LABEL_SUCCESS
            "FAILURE", "CANCELED" -> LABEL_FAILURE
            "QUEUED", "RUNNING", "PENDING", "UNKNOWN" -> null // Exclude incomplete builds
            else -> {
                Timber.w("Unknown pipeline status: ${pipeline.status} for pipeline ${pipeline.id}")
                null
            }
        }
    }

    /**
     * Check if pipeline is suitable for training (has completed status)
     */
    fun isValidForTraining(pipeline: PipelineEntity): Boolean {
        return pipeline.status in listOf("SUCCESS", "SKIPPED", "FAILURE", "CANCELED") &&
                pipeline.startedAt != null &&
                pipeline.finishedAt != null
    }

    /**
     * Get label name for display
     */
    fun getLabelName(label: Int): String = when (label) {
        LABEL_SUCCESS -> "SUCCESS"
        LABEL_FAILURE -> "FAILURE"
        else -> "UNKNOWN"
    }
}

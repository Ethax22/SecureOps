package com.secureops.app.ml.training

/**
 * Represents a single training sample with features and label
 */
data class TrainingSample(
    val features: FloatArray,
    val label: Int,
    val buildId: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrainingSample

        if (!features.contentEquals(other.features)) return false
        if (label != other.label) return false
        if (buildId != other.buildId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = features.contentHashCode()
        result = 31 * result + label
        result = 31 * result + buildId.hashCode()
        return result
    }
}

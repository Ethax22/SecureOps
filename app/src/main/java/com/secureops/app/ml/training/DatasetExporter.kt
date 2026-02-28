package com.secureops.app.ml.training

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Exports training dataset to CSV format for Python ML training
 */
class DatasetExporter(private val context: Context) {
    
    companion object {
        private const val DATASET_FILENAME = "training_dataset.csv"
        private const val EXPORT_DIR = "ml_training"
    }

    /**
     * Export training samples to CSV file
     * @return File path if successful, null if failed
     */
    suspend fun exportToCSV(
        samples: List<TrainingSample>,
        featureNames: List<String>
    ): String? {
        if (samples.isEmpty()) {
            Timber.w("Cannot export empty dataset")
            return null
        }

        try {
            val exportDir = File(context.getExternalFilesDir(null), EXPORT_DIR)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val csvFile = File(exportDir, DATASET_FILENAME)
            
            FileWriter(csvFile).use { writer ->
                // Write header
                val header = featureNames.joinToString(",") + ",label"
                writer.append(header)
                writer.append("\n")

                // Write data rows
                samples.forEach { sample ->
                    val row = sample.features.joinToString(",") + ",${sample.label}"
                    writer.append(row)
                    writer.append("\n")
                }

                writer.flush()
            }

            Timber.i("Dataset exported successfully: ${csvFile.absolutePath}")
            Timber.i("Total samples: ${samples.size}")
            Timber.i("Success samples: ${samples.count { it.label == LabelGenerator.LABEL_SUCCESS }}")
            Timber.i("Failure samples: ${samples.count { it.label == LabelGenerator.LABEL_FAILURE }}")

            return csvFile.absolutePath

        } catch (e: IOException) {
            Timber.e(e, "Failed to export dataset to CSV")
            return null
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception while exporting dataset")
            return null
        }
    }

    /**
     * Export dataset to JSON format (alternative format)
     */
    suspend fun exportToJSON(
        samples: List<TrainingSample>,
        featureNames: List<String>
    ): String? {
        if (samples.isEmpty()) {
            Timber.w("Cannot export empty dataset")
            return null
        }

        try {
            val exportDir = File(context.getExternalFilesDir(null), EXPORT_DIR)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val jsonFile = File(exportDir, "training_dataset.json")
            
            FileWriter(jsonFile).use { writer ->
                writer.append("{\n")
                writer.append("  \"feature_names\": [${featureNames.joinToString { "\"$it\"" }}],\n")
                writer.append("  \"samples\": [\n")

                samples.forEachIndexed { index, sample ->
                    writer.append("    {\n")
                    writer.append("      \"build_id\": \"${sample.buildId}\",\n")
                    writer.append("      \"features\": [${sample.features.joinToString()}],\n")
                    writer.append("      \"label\": ${sample.label}\n")
                    writer.append("    }")
                    
                    if (index < samples.size - 1) {
                        writer.append(",")
                    }
                    writer.append("\n")
                }

                writer.append("  ]\n")
                writer.append("}\n")
                writer.flush()
            }

            Timber.i("Dataset exported to JSON: ${jsonFile.absolutePath}")
            return jsonFile.absolutePath

        } catch (e: IOException) {
            Timber.e(e, "Failed to export dataset to JSON")
            return null
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception while exporting dataset")
            return null
        }
    }

    /**
     * Get statistics about the exported dataset
     */
    fun getDatasetStats(samples: List<TrainingSample>): DatasetStats {
        val successCount = samples.count { it.label == LabelGenerator.LABEL_SUCCESS }
        val failureCount = samples.count { it.label == LabelGenerator.LABEL_FAILURE }
        
        return DatasetStats(
            totalSamples = samples.size,
            successSamples = successCount,
            failureSamples = failureCount,
            balanceRatio = if (failureCount > 0) successCount.toFloat() / failureCount else 0f
        )
    }
}

/**
 * Dataset statistics
 */
data class DatasetStats(
    val totalSamples: Int,
    val successSamples: Int,
    val failureSamples: Int,
    val balanceRatio: Float
) {
    val isBalanced: Boolean
        get() = balanceRatio in 0.8f..1.2f
}

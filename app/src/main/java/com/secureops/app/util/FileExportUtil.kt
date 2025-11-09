package com.secureops.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.secureops.app.data.analytics.AnalyticsExport
import com.secureops.app.data.analytics.ExportFormat
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object FileExportUtil {

    /**
     * Export analytics data to a file and return the URI
     */
    fun exportAnalytics(
        context: Context,
        data: AnalyticsExport,
        format: ExportFormat
    ): FileExportResult {
        return try {
            val fileName = generateFileName(format)
            val mimeType = getMimeType(format)
            
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                saveToMediaStore(context, fileName, mimeType) { outputStream ->
                    writeDataToStream(outputStream, data, format)
                }
            } else {
                // Use legacy external storage for older Android versions
                saveToExternalStorage(context, fileName) { outputStream ->
                    writeDataToStream(outputStream, data, format)
                }
            }
            
            FileExportResult(
                success = true,
                uri = uri,
                fileName = fileName,
                message = "File exported successfully to Downloads"
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to export analytics")
            FileExportResult(
                success = false,
                uri = null,
                fileName = null,
                message = "Export failed: ${e.message}"
            )
        }
    }

    private fun saveToMediaStore(
        context: Context,
        fileName: String,
        mimeType: String,
        writeData: (OutputStream) -> Unit
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                writeData(outputStream)
            }
        }

        return uri
    }

    private fun saveToExternalStorage(
        context: Context,
        fileName: String,
        writeData: (OutputStream) -> Unit
    ): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { outputStream ->
            writeData(outputStream)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun writeDataToStream(
        outputStream: OutputStream,
        data: AnalyticsExport,
        format: ExportFormat
    ) {
        when (format) {
            ExportFormat.CSV -> exportToCSV(outputStream, data)
            ExportFormat.JSON -> exportToJSON(outputStream, data)
            ExportFormat.PDF -> exportToPDF(outputStream, data)
        }
    }

    private fun exportToCSV(outputStream: OutputStream, data: AnalyticsExport) {
        val csvBuilder = StringBuilder()
        
        // Header
        csvBuilder.append("SecureOps Analytics Report\n")
        csvBuilder.append("Generated: ${formatTimestamp(data.generatedAt)}\n\n")
        
        // Daily Trends
        csvBuilder.append("Daily Metrics\n")
        csvBuilder.append("Date,Total Builds,Failed Builds,Failure Rate\n")
        data.trends.dailyMetrics.forEach { metric ->
            csvBuilder.append("${metric.date},${metric.total},${metric.failed},${metric.failureRate}%\n")
        }
        csvBuilder.append("\n")
        
        // Failure Causes
        csvBuilder.append("Common Failure Causes\n")
        csvBuilder.append("Cause,Count\n")
        data.causes.forEach { (cause, count) ->
            csvBuilder.append("$cause,$count\n")
        }
        csvBuilder.append("\n")
        
        // Repository Metrics
        csvBuilder.append("Repository Metrics\n")
        csvBuilder.append("Repository,Total Builds,Failed,Successful,Failure Rate,Avg Duration (ms)\n")
        data.repositories.forEach { repo ->
            csvBuilder.append("${repo.repository},${repo.totalBuilds},${repo.failedBuilds},${repo.successfulBuilds},${repo.failureRate}%,${repo.averageDuration}\n")
        }
        
        outputStream.write(csvBuilder.toString().toByteArray())
    }

    private fun exportToJSON(outputStream: OutputStream, data: AnalyticsExport) {
        val json = JSONObject().apply {
            put("generatedAt", data.generatedAt)
            put("generatedAtFormatted", formatTimestamp(data.generatedAt))
            
            // Trends
            put("trends", JSONObject().apply {
                put("timeRange", data.trends.timeRange.name)
                put("overallFailureRate", data.trends.overallFailureRate)
                put("dailyMetrics", JSONArray().apply {
                    data.trends.dailyMetrics.forEach { metric ->
                        put(JSONObject().apply {
                            put("date", metric.date)
                            put("total", metric.total)
                            put("failed", metric.failed)
                            put("failureRate", metric.failureRate)
                        })
                    }
                })
            })
            
            // Causes
            put("failureCauses", JSONObject().apply {
                data.causes.forEach { (cause, count) ->
                    put(cause, count)
                }
            })
            
            // Repositories
            put("repositories", JSONArray().apply {
                data.repositories.forEach { repo ->
                    put(JSONObject().apply {
                        put("repository", repo.repository)
                        put("totalBuilds", repo.totalBuilds)
                        put("failedBuilds", repo.failedBuilds)
                        put("successfulBuilds", repo.successfulBuilds)
                        put("failureRate", repo.failureRate)
                        put("averageDuration", repo.averageDuration)
                    })
                }
            })
        }
        
        outputStream.write(json.toString(2).toByteArray())
    }

    private fun exportToPDF(outputStream: OutputStream, data: AnalyticsExport) {
        // For PDF, we'll create a simple text-based format
        // In a production app, you'd use a library like iText or Android's PdfDocument
        val pdfContent = buildString {
            append("=".repeat(50) + "\n")
            append("SecureOps Analytics Report\n")
            append("Generated: ${formatTimestamp(data.generatedAt)}\n")
            append("=".repeat(50) + "\n\n")
            
            append("DAILY METRICS (${data.trends.timeRange.name})\n")
            append("-".repeat(50) + "\n")
            append("Overall Failure Rate: ${String.format("%.2f", data.trends.overallFailureRate)}%\n\n")
            data.trends.dailyMetrics.take(10).forEach { metric ->
                append("${metric.date}: ${metric.total} builds, ${metric.failed} failed (${String.format("%.1f", metric.failureRate)}%)\n")
            }
            append("\n")
            
            append("COMMON FAILURE CAUSES\n")
            append("-".repeat(50) + "\n")
            data.causes.forEach { (cause, count) ->
                append("$cause: $count\n")
            }
            append("\n")
            
            append("REPOSITORY METRICS\n")
            append("-".repeat(50) + "\n")
            data.repositories.forEach { repo ->
                append("${repo.repository}\n")
                append("  Total: ${repo.totalBuilds}, Failed: ${repo.failedBuilds}, Success: ${repo.successfulBuilds}\n")
                append("  Failure Rate: ${String.format("%.1f", repo.failureRate)}%\n")
                append("  Avg Duration: ${repo.averageDuration}ms\n\n")
            }
        }
        
        outputStream.write(pdfContent.toByteArray())
    }

    private fun generateFileName(format: ExportFormat): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = when (format) {
            ExportFormat.CSV -> "csv"
            ExportFormat.JSON -> "json"
            ExportFormat.PDF -> "txt" // Using .txt instead of .pdf since we're not creating real PDFs
        }
        return "SecureOps_Analytics_$timestamp.$extension"
    }

    private fun getMimeType(format: ExportFormat): String {
        return when (format) {
            ExportFormat.CSV -> "text/csv"
            ExportFormat.JSON -> "application/json"
            ExportFormat.PDF -> "text/plain" // Using text/plain since we're not creating real PDFs
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
    }
}

data class FileExportResult(
    val success: Boolean,
    val uri: Uri?,
    val fileName: String?,
    val message: String
)

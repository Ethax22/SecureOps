package com.secureops.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.secureops.app.data.local.entity.BenchmarkResultEntity
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object BenchmarkReportGenerator {
    
    fun generatePdfReport(context: Context, result: BenchmarkResultEntity): Result<String> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            
            val canvas: Canvas = page.canvas
            val paint = Paint()
            paint.color = Color.BLACK
            paint.textSize = 24f
            paint.isFakeBoldText = true
            
            var yPos = 80f
            canvas.drawText("SecureOps Benchmark Report", 50f, yPos, paint)
            
            paint.textSize = 14f
            paint.isFakeBoldText = false
            yPos += 40f
            
            val lines = listOf(
                "Timestamp: \${java.util.Date(result.timestamp)}",
                "Device: \${result.deviceModel} (Android \${result.osVersion})",
                "",
                "--- Performance Metrics ---",
                "Avg Inference Time: \${String.format(\"%.2f\", result.inferenceTimeMsAvg)} ms",
                "P95 Inference Time: \${String.format(\"%.2f\", result.inferenceTimeMsP95)} ms",
                "Model Load Time: \${result.modelLoadTimeMs} ms",
                "App Startup Time: \${result.startupTimeMs} ms",
                "Avg Memory Usage: \${String.format(\"%.2f\", result.memoryUsageMbAvg)} MB",
                "Peak Memory Usage: \${String.format(\"%.2f\", result.memoryUsageMbPeak)} MB",
                "",
                "--- Validation Metrics ---",
                "Accuracy: \${String.format(\"%.2f\", result.accuracy * 100)}%",
                "Precision: \${String.format(\"%.2f\", result.precision * 100)}%",
                "Recall: \${String.format(\"%.2f\", result.recall * 100)}%",
                "F1 Score: \${String.format(\"%.2f\", result.f1Score * 100)}%"
            )
            
            for (line in lines) {
                canvas.drawText(line, 50f, yPos, paint)
                yPos += 25f
            }
            
            pdfDocument.finishPage(page)
            
            val fileName = "SecureOps_Benchmark_\${System.currentTimeMillis()}.pdf"
            val outputStream: OutputStream?
            val filePath: String
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
                filePath = "Downloads/\$fileName"
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
                filePath = file.absolutePath
            }
            
            outputStream?.use {
                pdfDocument.writeTo(it)
            }
            pdfDocument.close()
            
            Timber.i("PDF report generated at $filePath")
            Result.success(filePath)
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate PDF report")
            Result.failure(e)
        }
    }
}

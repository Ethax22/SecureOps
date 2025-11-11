package com.secureops.app.domain.model

data class BuildArtifact(
    val id: String,
    val name: String,
    val size: Long, // bytes
    val downloadUrl: String,
    val contentType: String = "application/octet-stream",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Format file size in human-readable format
 */
fun Long.formatFileSize(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> "%.2f GB".format(gb)
        mb >= 1 -> "%.2f MB".format(mb)
        kb >= 1 -> "%.2f KB".format(kb)
        else -> "$this bytes"
    }
}

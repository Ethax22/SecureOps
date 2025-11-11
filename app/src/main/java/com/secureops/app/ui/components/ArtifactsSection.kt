package com.secureops.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.secureops.app.domain.model.BuildArtifact
import com.secureops.app.domain.model.formatFileSize

@Composable
fun ArtifactsSection(
    artifacts: List<BuildArtifact>,
    onDownloadArtifact: (BuildArtifact) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Artifacts",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${artifacts.size} ${if (artifacts.size == 1) "item" else "items"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (artifacts.isEmpty()) {
                Text(
                    text = "No artifacts available for this build",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                artifacts.forEach { artifact ->
                    ArtifactItem(
                        artifact = artifact,
                        onDownload = { onDownloadArtifact(artifact) }
                    )
                    if (artifact != artifacts.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtifactItem(
    artifact: BuildArtifact,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getArtifactIcon(artifact.name),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = artifact.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = artifact.size.formatFileSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onDownload) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download artifact"
            )
        }
    }
}

private fun getArtifactIcon(filename: String) = when {
    filename.endsWith(".zip") || filename.endsWith(".tar.gz") || filename.endsWith(".tgz") -> Icons.Default.FolderZip
    filename.endsWith(".apk") || filename.endsWith(".aar") -> Icons.Default.Android
    filename.endsWith(".jar") -> Icons.Default.Category
    filename.endsWith(".log") || filename.endsWith(".txt") -> Icons.Default.Description
    filename.endsWith(".json") || filename.endsWith(".xml") -> Icons.Default.Code
    filename.endsWith(".pdf") -> Icons.Default.PictureAsPdf
    else -> Icons.Default.InsertDriveFile
}

package com.secureops.app.ui.screens.voice

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.secureops.app.ui.components.GlassCard
import com.secureops.app.ui.components.GradientBackground
import com.secureops.app.ui.components.NeonButton
import com.secureops.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun VoiceScreen() {
    val context = LocalContext.current
    val viewModel: VoiceViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Request microphone permission
    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    GradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Voice Assistant",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            floatingActionButton = {
                // Neon gradient FAB
                FloatingActionButton(
                    onClick = {
                        if (recordAudioPermission.status.isGranted) {
                            if (uiState.isListening) {
                                viewModel.stopListening()
                            } else {
                                viewModel.startListening()
                            }
                        } else {
                            recordAudioPermission.launchPermissionRequest()
                        }
                    },
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = if (uiState.isListening) {
                                    Brush.linearGradient(
                                        colors = listOf(ErrorRed, Color(0xFFFF6B6B))
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(GradientPurpleStart, GradientPurpleEnd)
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = if (uiState.isListening) "Stop listening" else "Start listening",
                            tint = Color.White
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Show permission rationale if needed
                if (!recordAudioPermission.status.isGranted && recordAudioPermission.status.shouldShowRationale) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentPadding = PaddingValues(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Microphone Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "This feature requires microphone access to listen to your voice commands. Please grant the permission to use voice assistant.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        NeonButton(
                            text = "Grant Permission",
                            onClick = { recordAudioPermission.launchPermissionRequest() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages) { message ->
                        VoiceMessageBubble(message)
                    }
                }

                // Error message
                uiState.errorMessage?.let { errorMsg ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "❌",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorRed
                            )
                        }
                    }
                }

                // Listening indicator
                if (uiState.isListening) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Animated pulsing circle
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                PrimaryPurple,
                                                PrimaryPurple.copy(alpha = 0.5f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = uiState.listeningText ?: "Listening...",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryPurple
                            )
                        }
                    }
                }

                // Suggestions
                if (!uiState.isListening) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                if (recordAudioPermission.status.isGranted) {
                                    viewModel.handleSuggestionClick("Check status")
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💼")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Check status")
                                }
                            }
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                if (recordAudioPermission.status.isGranted) {
                                    viewModel.handleSuggestionClick("Risky builds?")
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚠️")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Risky builds?")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceMessageBubble(message: VoiceMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        GlassCard(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isUser) 20.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 20.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Avatar/Icon indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (message.isUser) {
                                Brush.linearGradient(
                                    colors = listOf(AccentPink, PrimaryPurple)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(AccentCyan, AccentGreen)
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (message.isUser) "👤" else "🤖",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (message.isUser) AccentPink else AccentCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

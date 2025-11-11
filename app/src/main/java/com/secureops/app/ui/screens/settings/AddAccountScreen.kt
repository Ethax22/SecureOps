package com.secureops.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secureops.app.domain.model.CIProvider
import com.secureops.app.ui.components.*
import com.secureops.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddAccountViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var accountName by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf<CIProvider?>(null) }
    var baseUrl by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    var showProviderDialog by remember { mutableStateOf(false) }

    // Handle OAuth token when received
    LaunchedEffect(uiState.oauthToken) {
        uiState.oauthToken?.let { oauthToken ->
            token = oauthToken.accessToken
        }
    }

    // Handle success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    GradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Add Account") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            FadeInContent {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedCardEntrance {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "🔗 Connect a CI/CD Provider",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Enter your CI/CD provider credentials to start monitoring pipelines.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedCardEntrance(delayMillis = 100) {
                        // Provider Selection
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showProviderDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .then(
                                            Modifier.padding(end = 12.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CI/CD Provider",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = selectedProvider?.displayName
                                            ?: "Tap to select a provider",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // OAuth button for supported providers
                    if (selectedProvider != null && supportsOAuth(selectedProvider)) {
                        AnimatedCardEntrance(delayMillis = 200) {
                            Column {
                                NeonButton(
                                    text = "Sign in with OAuth",
                                    onClick = { viewModel.startOAuthFlow(selectedProvider!!) },
                                    icon = Icons.Default.Login,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isLoading
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = GlassBorderDark
                                    )
                                    Text(
                                        text = "OR",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = GlassBorderDark
                                    )
                                }
                            }
                        }
                    }

                    AnimatedCardEntrance(
                        delayMillis = if (selectedProvider != null && supportsOAuth(
                                selectedProvider
                            )
                        ) 300 else 200
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Account Name
                            GlassTextField(
                                value = accountName,
                                onValueChange = { accountName = it },
                                placeholder = "e.g., My GitHub Account",
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = Icons.Default.AccountCircle
                            )

                            // Base URL (for certain providers)
                            if (selectedProvider == CIProvider.JENKINS ||
                                selectedProvider == CIProvider.GITLAB_CI ||
                                selectedProvider == CIProvider.AZURE_DEVOPS
                            ) {
                                GlassTextField(
                                    value = baseUrl,
                                    onValueChange = { baseUrl = it },
                                    placeholder = when (selectedProvider) {
                                        CIProvider.JENKINS -> "https://jenkins.example.com"
                                        CIProvider.GITLAB_CI -> "https://gitlab.com"
                                        CIProvider.AZURE_DEVOPS -> "https://dev.azure.com"
                                        else -> "https://example.com"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = Icons.Default.Link
                                )
                            } else {
                                // Set default URLs for providers that don't need custom URLs
                                LaunchedEffect(selectedProvider) {
                                    baseUrl = when (selectedProvider) {
                                        CIProvider.GITHUB_ACTIONS -> "https://api.github.com"
                                        CIProvider.CIRCLE_CI -> "https://circleci.com/api"
                                        else -> ""
                                    }
                                }
                            }

                            // API Token
                            OutlinedTextField(
                                value = token,
                                onValueChange = { token = it },
                                label = { Text("API Token / Access Token") },
                                placeholder = { Text("Enter your access token") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (tokenVisible)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = PrimaryPurple
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                        Icon(
                                            if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (tokenVisible) "Hide token" else "Show token",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = GlassSurfaceDark,
                                    unfocusedContainerColor = GlassSurfaceDark,
                                    focusedBorderColor = PrimaryPurple,
                                    unfocusedBorderColor = GlassBorderDark,
                                    cursorColor = PrimaryPurple,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                singleLine = true
                            )
                        }
                    }

                    // Help text for token
                    AnimatedCardEntrance(
                        delayMillis = if (selectedProvider != null && supportsOAuth(
                                selectedProvider
                            )
                        ) 400 else 300
                    ) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "💡 ",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Column {
                                    Text(
                                        text = "How to get an API token?",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AccentCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = getTokenHelpText(selectedProvider),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Error message
                    if (uiState.error != null) {
                        AnimatedCardEntrance {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "❌ ",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = uiState.error ?: "An error occurred",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ErrorRed
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Add Account Button
                    AnimatedCardEntrance(
                        delayMillis = if (selectedProvider != null && supportsOAuth(
                                selectedProvider
                            )
                        ) 500 else 400
                    ) {
                        NeonButton(
                            text = if (uiState.isLoading) "Adding..." else "Add Account",
                            onClick = {
                                if (selectedProvider != null && accountName.isNotBlank() && token.isNotBlank()) {
                                    viewModel.addAccount(
                                        provider = selectedProvider!!,
                                        name = accountName,
                                        baseUrl = baseUrl.ifBlank {
                                            when (selectedProvider) {
                                                CIProvider.GITHUB_ACTIONS -> "https://api.github.com"
                                                CIProvider.CIRCLE_CI -> "https://circleci.com/api"
                                                else -> baseUrl
                                            }
                                        },
                                        token = token
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedProvider != null &&
                                    accountName.isNotBlank() &&
                                    token.isNotBlank() &&
                                    !uiState.isLoading
                        )
                    }
                }
            }
        }
    }

    // Provider Selection Dialog
    if (showProviderDialog) {
        AlertDialog(
            onDismissRequest = { showProviderDialog = false },
            title = {
                Text(
                    "Select CI/CD Provider",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CIProvider.values().forEach { provider ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedProvider = provider
                                showProviderDialog = false
                            }
                        ) {
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProviderDialog = false }) {
                    Text("Cancel", color = AccentCyan)
                }
            },
            containerColor = SurfaceDark,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Helper function to check if provider supports OAuth
private fun supportsOAuth(provider: CIProvider?): Boolean {
    return provider in listOf(
        CIProvider.GITHUB_ACTIONS,
        CIProvider.GITLAB_CI,
        CIProvider.AZURE_DEVOPS
    )
}

private fun getTokenHelpText(provider: CIProvider?): String {
    return when (provider) {
        CIProvider.GITHUB_ACTIONS -> 
            "Go to GitHub Settings → Developer settings → Personal access tokens → Generate new token. " +
            "Grant 'repo' and 'workflow' permissions."
        
        CIProvider.GITLAB_CI -> 
            "Go to GitLab Settings → Access Tokens → Add new token. " +
            "Select 'api' and 'read_api' scopes."
        
        CIProvider.JENKINS -> 
            "Go to Jenkins → User → Configure → API Token → Add new token."
        
        CIProvider.CIRCLE_CI -> 
            "Go to CircleCI User Settings → Personal API Tokens → Create New Token."
        
        CIProvider.AZURE_DEVOPS -> 
            "Go to Azure DevOps → User settings → Personal access tokens → New Token. " +
            "Grant 'Build (Read)' permissions."
        
        null -> "Select a provider to see instructions."
    }
}

package com.secureops.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.secureops.app.domain.model.CIProvider
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

    // Handle success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Connect a CI/CD Provider",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Enter your CI/CD provider credentials to start monitoring pipelines.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Provider Selection
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showProviderDialog = true }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "CI/CD Provider",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedProvider?.displayName ?: "Select a provider",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Account Name
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("Account Name") },
                placeholder = { Text("e.g., My GitHub Account") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Base URL (optional for some providers)
            if (selectedProvider == CIProvider.JENKINS || 
                selectedProvider == CIProvider.GITLAB_CI ||
                selectedProvider == CIProvider.AZURE_DEVOPS) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    placeholder = { 
                        Text(
                            when (selectedProvider) {
                                CIProvider.JENKINS -> "https://jenkins.example.com"
                                CIProvider.GITLAB_CI -> "https://gitlab.com"
                                CIProvider.AZURE_DEVOPS -> "https://dev.azure.com"
                                else -> "https://example.com"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                trailingIcon = {
                    IconButton(onClick = { tokenVisible = !tokenVisible }) {
                        Icon(
                            if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (tokenVisible) "Hide token" else "Show token"
                        )
                    }
                },
                singleLine = true
            )

            // Help text for token
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "How to get an API token?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getTokenHelpText(selectedProvider),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Error message
            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error ?: "An error occurred",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Add Account Button
            Button(
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
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Add Account")
                }
            }
        }
    }

    // Provider Selection Dialog
    if (showProviderDialog) {
        AlertDialog(
            onDismissRequest = { showProviderDialog = false },
            title = { Text("Select CI/CD Provider") },
            text = {
                Column {
                    CIProvider.values().forEach { provider ->
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = {
                                selectedProvider = provider
                                showProviderDialog = false
                            }
                        ) {
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProviderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
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

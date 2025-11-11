package com.secureops.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secureops.app.domain.model.Account
import com.secureops.app.ui.components.*
import com.secureops.app.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToEditAccount: (String) -> Unit = {},
    viewModel: ManageAccountsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }

    GradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Manage Accounts") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToAddAccount) {
                            Icon(Icons.Default.Add, "Add Account", tint = PrimaryPurple)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    uiState.isLoading && uiState.accounts.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedLoadingRings()
                        }
                    }

                    uiState.accounts.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassEmptyState(
                                icon = Icons.Default.AccountCircle,
                                title = "No Accounts Connected",
                                subtitle = "Connect your CI/CD provider to start monitoring pipelines",
                                actionText = "Add Account",
                                onAction = onNavigateToAddAccount,
                                iconColor = PrimaryPurple
                            )
                        }
                    }

                    else -> {
                        FadeInContent {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    AnimatedCardEntrance {
                                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "🔗 ",
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                                Column {
                                                    Text(
                                                        text = "Connected Accounts",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${uiState.accounts.size} account${if (uiState.accounts.size != 1) "s" else ""}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                itemsIndexed(uiState.accounts) { index, account ->
                                    AnimatedCardEntrance(delayMillis = (index + 1) * 100) {
                                        AccountCard(
                                            account = account,
                                            onToggleStatus = { viewModel.toggleAccountStatus(account) },
                                            onDelete = { accountToDelete = account },
                                            onEdit = { onNavigateToEditAccount(account.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed
                )
            },
            title = {
                Text(
                    "Delete Account?",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${accountToDelete?.name}\"? " +
                            "This action cannot be undone and will remove all associated data.",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        accountToDelete?.let { viewModel.deleteAccount(it.id) }
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel", color = AccentCyan)
                }
            },
            containerColor = SurfaceDark,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AccountCard(
    account: Account,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Provider icon with gradient background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    getProviderColor(account.provider.name).copy(alpha = 0.3f),
                                    getProviderColor(account.provider.name).copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getProviderIcon(account.provider.name),
                        contentDescription = null,
                        tint = getProviderColor(account.provider.name),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = account.provider.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onEdit()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = AccentCyan
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (account.isActive) "Disable" else "Enable")
                        },
                        onClick = {
                            onToggleStatus()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                if (account.isActive) Icons.Default.Close else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (account.isActive) WarningAmber else SuccessGreen
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = ErrorRed
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        GradientDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Account details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Base URL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = account.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status and sync info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (account.isActive) {
                    PulsingGlowBadge(
                        status = "Active",
                        color = SuccessGreen
                    )
                } else {
                    StatusBadge(
                        status = "Inactive",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (account.lastSyncedAt != null) {
                    NeonChip(
                        text = formatDate(account.lastSyncedAt),
                        icon = "🕐",
                        color = InfoBlue
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAccountsState(
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with gradient background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                PrimaryPurple.copy(alpha = 0.3f),
                                PrimaryPurple.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = PrimaryPurple
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "No Accounts Connected",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect your CI/CD provider to start monitoring pipelines",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            NeonButton(
                text = "Add Account",
                onClick = onAddAccount,
                icon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun getProviderIcon(providerName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (providerName) {
        "GITHUB_ACTIONS" -> Icons.Default.Code
        "GITLAB_CI" -> Icons.Default.Cloud
        "JENKINS" -> Icons.Default.Build
        "CIRCLE_CI" -> Icons.Default.Circle
        "AZURE_DEVOPS" -> Icons.Default.Business
        else -> Icons.Default.CloudQueue
    }
}

private fun getProviderColor(providerName: String): Color {
    return when (providerName) {
        "GITHUB_ACTIONS" -> AccentViolet
        "GITLAB_CI" -> AccentPink
        "JENKINS" -> AccentCyan
        "CIRCLE_CI" -> AccentGreen
        "AZURE_DEVOPS" -> InfoBlue
        else -> PrimaryPurple
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

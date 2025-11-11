# Quick Start Guide - New UI Components

## 🚀 Getting Started with the Purple Gradient UI

This guide will help you quickly adopt the new glassmorphism UI design in your SecureOps screens.

---

## Installation

The new UI components are already integrated into your project. No additional dependencies required!

**Key Files:**

- `ui/theme/Color.kt` - Color palette
- `ui/theme/Theme.kt` - Theme configuration
- `ui/components/GlassComponents.kt` - Reusable UI components

---

## Basic Setup

### 1. Wrap Your Screen with Gradient Background

Every screen should be wrapped with `GradientBackground` for the purple gradient effect:

```kotlin
@Composable
fun MyScreen() {
    GradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,  // IMPORTANT!
            topBar = { /* TopBar */ }
        ) { paddingValues ->
            // Your content here
        }
    }
}
```

**⚠️ Important:** Always set `containerColor = Color.Transparent` on Scaffold!

---

## Using Components

### Glass Card

Replace standard `Card` with `GlassCard`:

```kotlin
// ❌ Old Way
Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Hello")
    }
}

// ✅ New Way
GlassCard(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(16.dp)
) {
    Text("Hello")
}
```

**With Click Handler:**

```kotlin
GlassCard(
    onClick = { /* handle click */ },
    modifier = Modifier.fillMaxWidth()
) {
    Text("Clickable Card")
}
```

---

### Status Badge

Show status with colored badges:

```kotlin
StatusBadge(
    status = "Success",
    color = SuccessGreen
)

// Other status colors:
// - ErrorRed for failures
// - InfoBlue for running
// - WarningAmber for pending
```

---

### Neon Chip

Use for tags, labels, and categories:

```kotlin
NeonChip(
    text = "main",
    icon = "🌿",  // Optional emoji
    color = AccentGreen
)

// Different colors for different purposes:
NeonChip(text = "Jenkins", color = AccentCyan)
NeonChip(text = "v1.0.0", color = PrimaryPurple)
```

---

### Neon Button

Replace standard buttons with gradient buttons:

```kotlin
// ❌ Old Way
Button(onClick = { /* action */ }) {
    Text("Click Me")
}

// ✅ New Way
NeonButton(
    text = "Click Me",
    onClick = { /* action */ },
    modifier = Modifier.fillMaxWidth()
)

// With Icon:
NeonButton(
    text = "Get Started",
    icon = Icons.Default.ArrowForward,
    onClick = { /* action */ }
)

// Disabled State:
NeonButton(
    text = "Processing...",
    enabled = false,
    onClick = { /* action */ }
)
```

---

### Glass Text Field

For input fields with glassmorphism:

```kotlin
var text by remember { mutableStateOf("") }

GlassTextField(
    value = text,
    onValueChange = { text = it },
    placeholder = "Type something...",
    leadingIcon = Icons.Default.Search,  // Optional
    modifier = Modifier.fillMaxWidth()
)
```

---

### Gradient Divider

Separate sections with a purple gradient line:

```kotlin
Text("Section 1")
Spacer(modifier = Modifier.height(12.dp))
GradientDivider()
Spacer(modifier = Modifier.height(12.dp))
Text("Section 2")
```

---

## Color Palette Reference

### Quick Access

```kotlin
import com.secureops.app.ui.theme.*

// Primary Colors
PrimaryPurple        // Main brand color
PrimaryPurpleDark    // Darker variant
PrimaryPurpleLight   // Lighter variant

// Accent Colors
AccentViolet   // Secondary actions
AccentPink     // Call-to-action
AccentCyan     // Info elements
AccentGreen    // Success states

// Status Colors
SuccessGreen   // Success/passed
WarningAmber   // Warning/pending
ErrorRed       // Error/failed
InfoBlue       // Info/running

// Backgrounds (Dark Theme)
BackgroundDark          // Main background
SurfaceDark            // Card surfaces
GlassSurfaceDark       // Glass backgrounds
GlassBorderDark        // Glass borders

// Neon Effects
NeonPurple    // Glow effect
NeonPink      // Secondary glow
```

---

## Complete Screen Example

Here's a complete example showing how to build a screen with the new UI:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFeatureScreen(
    viewModel: MyViewModel = koinViewModel(),
    onNavigateToDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // 1. Wrap with GradientBackground
    GradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,  // Important!
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "My Feature",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section Header
                item {
                    Text(
                        text = "Recent Items",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }

                // Items List
                items(uiState.items) { item ->
                    GlassCard(
                        onClick = { onNavigateToDetails(item.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            StatusBadge(
                                status = item.status,
                                color = when(item.status) {
                                    "Active" -> SuccessGreen
                                    "Pending" -> WarningAmber
                                    else -> ErrorRed
                                }
                            )
                        }
                    }
                }

                // Action Button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    NeonButton(
                        text = "Add New Item",
                        icon = Icons.Default.Add,
                        onClick = { /* handle click */ },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
```

---

## Icon with Gradient Background

Create icon containers with gradient backgrounds:

```kotlin
Box(
    modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    AccentPink.copy(alpha = 0.3f),
                    AccentPink.copy(alpha = 0.1f)
                )
            )
        ),
    contentAlignment = Alignment.Center
) {
    Icon(
        imageVector = Icons.Default.Star,
        contentDescription = null,
        tint = AccentPink,
        modifier = Modifier.size(24.dp)
    )
}
```

---

## TopBar Configuration

Configure TopBar for transparent background:

```kotlin
TopAppBar(
    title = { 
        Text(
            "Screen Title",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    ),
    actions = {
        IconButton(onClick = { /* action */ }) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = PrimaryPurple
            )
        }
    }
)
```

---

## Common Patterns

### Loading State

```kotlin
if (uiState.isLoading) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PrimaryPurple)
    }
}
```

### Empty State

```kotlin
GlassCard(
    modifier = Modifier
        .fillMaxWidth()
        .padding(32.dp),
    contentPadding = PaddingValues(32.dp)
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            PrimaryPurple.copy(alpha = 0.3f),
                            AccentPink.copy(alpha = 0.2f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No items found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Add an item to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

### Error State

```kotlin
GlassCard(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    contentPadding = PaddingValues(16.dp)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ErrorRed
            )
            Text(
                text = uiState.errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

## Migration Checklist

When updating an existing screen:

- [ ] Wrap screen with `GradientBackground`
- [ ] Set `containerColor = Color.Transparent` on Scaffold
- [ ] Replace `Card` with `GlassCard`
- [ ] Replace `Button` with `NeonButton`
- [ ] Update TopBar colors to transparent
- [ ] Add `StatusBadge` for status indicators
- [ ] Use `NeonChip` for tags/labels
- [ ] Use `GradientDivider` between sections
- [ ] Add gradient backgrounds to icons
- [ ] Update color references to new palette
- [ ] Test in both light and dark modes

---

## Testing Your Changes

1. **Run the app**: `./gradlew assembleDebug`
2. **Check dark mode**: Toggle in Settings
3. **Verify transparency**: Glass effect should be visible
4. **Test interactions**: Cards should be clickable
5. **Check colors**: All elements should use new palette

---

## Tips & Best Practices

### ✅ Do This

- Always use `GradientBackground` as the outermost wrapper
- Use `GlassCard` for all content containers
- Apply gradient backgrounds to icon containers
- Use colored status badges consistently
- Add proper spacing between elements (16dp standard)
- Use `NeonButton` for primary actions

### ❌ Don't Do This

- Mix old and new components
- Forget to set `containerColor = Color.Transparent`
- Use colors outside the defined palette
- Reduce opacity below readable levels
- Use standard Material buttons
- Apply solid backgrounds to cards

---

## Troubleshooting

### Cards look solid instead of transparent

**Solution:** Make sure you wrapped the screen with `GradientBackground`

### Colors don't match the design

**Solution:** Import and use colors from `com.secureops.app.ui.theme.*`

### TopBar is blocking the gradient

**Solution:** Set `containerColor = Color.Transparent` in TopBar colors

### Text is hard to read

**Solution:** Check that you're using the correct text colors from MaterialTheme.colorScheme

---

## Next Steps

1. ✅ Read this Quick Start Guide
2. ✅ Check `UI_REDESIGN_SUMMARY.md` for detailed information
3. ✅ Review `UI_DESIGN_REFERENCE.md` for visual examples
4. ✅ Look at implemented screens for reference:
    - `DashboardScreen.kt`
    - `VoiceScreen.kt`
    - `SettingsScreen.kt`
5. ✅ Start migrating your screens one by one

---

## Support

Need help? Check these resources:

- **UI_REDESIGN_SUMMARY.md** - Complete implementation guide
- **UI_DESIGN_REFERENCE.md** - Visual design patterns
- **GlassComponents.kt** - Component source code
- **Implemented Screens** - Working examples

---

**Happy Coding! 🚀💜**

Transform your SecureOps screens with beautiful purple gradients and glassmorphism effects!

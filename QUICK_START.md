# SecureOps - Quick Start Guide

## Prerequisites

Before you begin, ensure you have:

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** or newer
- **Android SDK 34**
- **Git** installed

## Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/secureops.git
cd secureops
```

## Step 2: Open in Android Studio

1. Launch Android Studio
2. Select **File > Open**
3. Navigate to the SecureOps directory
4. Click **OK**

Android Studio will automatically:

- Sync Gradle dependencies
- Index the project
- Download required SDKs

## Step 3: Gradle Sync

Wait for Gradle sync to complete. This may take a few minutes on first run.

If sync fails:

```bash
./gradlew clean build
```

## Step 4: Firebase Configuration (Optional but Recommended)

For push notifications to work:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing
3. Add an Android app
4. Download `google-services.json`
5. Place it in `app/` directory

**Without Firebase:** The app will work but push notifications won't function.

## Step 5: Build the App

### Option A: Android Studio

1. Click **Build > Make Project** (Ctrl+F9 / Cmd+F9)
2. Wait for build to complete

### Option B: Command Line

```bash
./gradlew assembleDebug
```

## Step 6: Run on Device/Emulator

### Using Android Studio:

1. Connect device or start emulator
2. Click **Run** (Shift+F10 / Ctrl+R)
3. Select device
4. App will install and launch

### Using Command Line:

```bash
./gradlew installDebug
```

## Step 7: Explore the App

### Initial Setup

1. App opens to **Dashboard** (will be empty)
2. Navigate to **Settings** tab
3. Tap **Add Account**

### Add a CI/CD Account

**For GitHub:**

1. Select "GitHub Actions"
2. Enter name: "My GitHub"
3. Base URL: `https://api.github.com`
4. Token: Your GitHub Personal Access Token
    - Generate at: Settings > Developer settings > Personal access tokens
    - Required scopes: `repo`, `workflow`
5. Save

**For GitLab:**

1. Select "GitLab CI"
2. Enter name: "My GitLab"
3. Base URL: `https://gitlab.com` (or your instance)
4. Token: Your GitLab Personal Access Token
    - Generate at: User Settings > Access Tokens
    - Required scopes: `api`, `read_repository`
5. Save

### View Pipelines

1. Return to **Dashboard**
2. Pull down to refresh
3. Pipelines will load (if any exist in your repos)

### Test Voice Assistant

1. Navigate to **Voice** tab
2. Tap microphone button
3. Try commands:
    - "What's the status of my builds?"
    - "Any risky deployments today?"

### Check Analytics

1. Navigate to **Analytics** tab
2. View metrics and statistics

## Troubleshooting

### Build Errors

**Problem:** Gradle sync fails

```bash
# Solution: Clear Gradle cache
rm -rf ~/.gradle/caches
./gradlew clean build
```

**Problem:** Dependency resolution error

```bash
# Solution: Force dependency refresh
./gradlew build --refresh-dependencies
```

### Runtime Issues

**Problem:** App crashes on launch

- Check Logcat in Android Studio
- Ensure minimum SDK is 26 (Android 8.0)
- Verify all dependencies are installed

**Problem:** Database errors

- Uninstall and reinstall app
- Clear app data: Settings > Apps > SecureOps > Clear Data

**Problem:** Network errors

- Check internet connection
- Verify API tokens are correct
- Check API base URLs

### Testing Issues

**Problem:** Unit tests fail

```bash
./gradlew test --stacktrace
```

**Problem:** Instrumentation tests fail

```bash
./gradlew connectedAndroidTest --stacktrace
```

## Development Workflow

### Making Changes

1. Create a feature branch

```bash
git checkout -b feature/my-feature
```

2. Make your changes

3. Run tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

4. Build and verify

```bash
./gradlew build
```

5. Commit and push

```bash
git add .
git commit -m "Add my feature"
git push origin feature/my-feature
```

### Code Style

The project follows Kotlin coding conventions:

- 4 spaces for indentation
- camelCase for variables and functions
- PascalCase for classes
- Descriptive names
- Comments for complex logic

### Architecture Guidelines

When adding features:

1. **Domain Models** → Add to `domain/model/`
2. **API DTOs** → Add to `data/remote/dto/`
3. **Database Entities** → Add to `data/local/entity/`
4. **Repositories** → Add to `data/repository/`
5. **ViewModels** → Add to corresponding screen package
6. **UI Screens** → Add to `ui/screens/`
7. **Tests** → Add corresponding test files

### Running Specific Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests VoiceCommandProcessorTest

# Run with coverage
./gradlew testDebugUnitTest jacocoTestReport
```

## Project Structure Quick Reference

```
Key Directories:
├── app/src/main/java/com/secureops/app/
│   ├── data/          # Data layer (API, DB, repos)
│   ├── domain/        # Business logic & models
│   ├── ml/            # Machine learning
│   ├── di/            # Dependency injection
│   └── ui/            # User interface
├── app/src/test/      # Unit tests
└── app/src/androidTest/ # Instrumentation tests
```

## Useful Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (needs signing config)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# View all tasks
./gradlew tasks

# Generate APK and run tests
./gradlew build

# Check for dependency updates
./gradlew dependencyUpdates
```

## Next Steps

### For Developers:

1. Read `PROJECT_SUMMARY.md` for detailed architecture
2. Review `README.md` for feature documentation
3. Check existing code for patterns and style
4. Look at test files for testing examples

### For Testing:

1. Add test accounts from different CI/CD providers
2. Test all navigation flows
3. Verify offline functionality
4. Test voice commands
5. Check analytics display

### For Production:

1. Add signing configuration
2. Configure ProGuard rules
3. Set up Firebase properly
4. Add analytics tracking
5. Complete API implementations for all providers

## Getting Help

- **Issues:** Check GitHub Issues
- **Documentation:** See README.md and PROJECT_SUMMARY.md
- **Code Examples:** Browse existing implementations
- **Logs:** Use Logcat in Android Studio

## Common Development Tasks

### Adding a New Screen

1. Create package: `ui/screens/mynewscreen/`
2. Create ViewModel: `MyNewScreenViewModel.kt`
3. Create UI: `MyNewScreenScreen.kt`
4. Add to navigation: Update `NavGraph.kt`
5. Add tests: Create corresponding test file

### Adding a New API Endpoint

1. Add to service interface: `data/remote/api/MyService.kt`
2. Create/update DTOs: `data/remote/dto/`
3. Update mapper: `data/remote/mapper/`
4. Update repository: Use in repository class
5. Test: Create API test

### Adding a New Domain Model

1. Create model: `domain/model/MyModel.kt`
2. Create entity: `data/local/entity/MyModelEntity.kt`
3. Add converters if needed
4. Update DAO if needed
5. Add mapper functions

## Performance Tips

- Use `remember` for expensive calculations
- Minimize recompositions with `derivedStateOf`
- Use `LazyColumn` for lists
- Profile with Android Studio Profiler
- Monitor memory usage

## Security Reminders

- Never commit API keys or tokens
- Use EncryptedSharedPreferences for sensitive data
- Validate all user inputs
- Use HTTPS only
- Keep dependencies updated

---

**Happy Coding! 🚀**

For detailed information, see:

- [README.md](README.md) - Feature documentation
- [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) - Technical details
- [LICENSE](LICENSE) - License information

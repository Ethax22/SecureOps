# Gradle Setup Instructions

## Issue: Missing Gradle Wrapper JAR

The Gradle wrapper JAR file (`gradle/wrapper/gradle-wrapper.jar`) is not included in the repository
as it's a binary file.

## Solution: Generate Gradle Wrapper

You have two options to set up the Gradle wrapper:

### Option 1: Open in Android Studio (Recommended)

1. Open the project in Android Studio
2. Android Studio will automatically detect the missing wrapper
3. Click on "Sync Project with Gradle Files" or wait for automatic sync
4. Android Studio will download the Gradle wrapper automatically

### Option 2: Manual Setup with Local Gradle Installation

If you have Gradle installed on your system:

```bash
# Navigate to project directory
cd C:\Users\aravi\StudioProjects\Vibestate

# Generate wrapper with Gradle 8.2
gradle wrapper --gradle-version 8.2

# Or if using gradlew (on systems where it works):
gradle wrapper
```

### Option 3: Download Gradle Wrapper JAR Manually

1. Download the Gradle wrapper JAR from:
   https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar

2. Create directory structure:
   ```
   mkdir gradle\wrapper
   ```

3. Place the downloaded `gradle-wrapper.jar` in `gradle/wrapper/` directory

## After Setup

Once the wrapper is set up, you can run:

```bash
# Windows
gradlew.bat build

# Unix/Mac/Linux
./gradlew build
```

## Alternative: Use Android Studio

The easiest way is to simply:

1. Open this project in Android Studio
2. Let Android Studio handle everything automatically
3. Click "Sync Now" when prompted

Android Studio will:

- Download the Gradle wrapper
- Download all dependencies
- Configure the project
- Build successfully

## Verification

After setup, verify with:

```bash
# Windows
gradlew.bat --version

# Unix/Mac/Linux
./gradlew --version
```

You should see:

```
Gradle 8.2
```

## Note

This is a standard Android project setup requirement. The Gradle wrapper ensures all developers use
the same Gradle version regardless of their local Gradle installation.

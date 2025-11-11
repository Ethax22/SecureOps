package com.secureops.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = PrimaryPurpleDark,
    onPrimaryContainer = PrimaryPurpleLight,

    secondary = AccentCyan,
    onSecondary = Color.White,
    secondaryContainer = AccentViolet,
    onSecondaryContainer = AccentPink,

    tertiary = AccentPink,
    onTertiary = Color.White,
    tertiaryContainer = AccentGreen,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = TextSecondaryDark,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = ErrorRed,

    outline = GlassBorderDark,
    outlineVariant = Color.White.copy(alpha = 0.1f)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = PrimaryPurpleLight,
    onPrimaryContainer = PrimaryPurpleDark,

    secondary = AccentCyan,
    onSecondary = Color.White,
    secondaryContainer = AccentViolet.copy(alpha = 0.2f),
    onSecondaryContainer = AccentViolet,

    tertiary = AccentPink,
    onTertiary = Color.White,
    tertiaryContainer = AccentPink.copy(alpha = 0.2f),

    background = BackgroundLight,
    onBackground = TextPrimary,

    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLightElevated,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = ErrorRed,

    outline = GlassBorderLight,
    outlineVariant = PrimaryPurple.copy(alpha = 0.2f)
)

@Composable
fun SecureOpsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor =
                if (darkTheme) BackgroundDark.toArgb() else PrimaryPurple.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

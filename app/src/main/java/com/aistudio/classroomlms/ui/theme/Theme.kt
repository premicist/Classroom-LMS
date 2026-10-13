package com.aistudio.classroomlms.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = CalmPrimary,
    onPrimary = CalmOnPrimary,
    primaryContainer = CalmPrimaryContainer,
    onPrimaryContainer = CalmOnPrimaryContainer,
    secondary = CalmSecondary,
    onSecondary = CalmOnSecondary,
    secondaryContainer = CalmSecondaryContainer,
    onSecondaryContainer = CalmOnSecondaryContainer,
    tertiary = CalmTertiary,
    onTertiary = CalmOnTertiary,
    tertiaryContainer = CalmTertiaryContainer,
    onTertiaryContainer = CalmOnTertiaryContainer,
    background = CalmBackgroundLight,
    onBackground = CalmTextPrimaryLight,
    surface = CalmSurfaceLight,
    onSurface = CalmTextPrimaryLight,
    surfaceVariant = CalmSurfaceVariantLight,
    onSurfaceVariant = CalmTextSecondaryLight,
    outline = CalmBorderLight,
    outlineVariant = CalmBorderSubtleLight,
    error = StatusError,
    errorContainer = StatusErrorBg,
    onError = Color.White,
    onErrorContainer = StatusError
)

private val DarkColorScheme = darkColorScheme(
    primary = CalmPrimaryDark,
    onPrimary = CalmOnPrimaryDark,
    primaryContainer = CalmPrimaryContainerDark,
    onPrimaryContainer = CalmOnPrimaryContainerDark,
    secondary = CalmSecondaryDark,
    onSecondary = CalmOnSecondaryDark,
    secondaryContainer = CalmSecondaryContainerDark,
    onSecondaryContainer = CalmOnSecondaryContainerDark,
    tertiary = CalmTertiaryDark,
    onTertiary = CalmOnTertiaryDark,
    tertiaryContainer = CalmTertiaryContainerDark,
    onTertiaryContainer = CalmOnTertiaryContainerDark,
    background = CalmBackgroundDark,
    onBackground = CalmTextPrimaryDark,
    surface = CalmSurfaceDark,
    onSurface = CalmTextPrimaryDark,
    surfaceVariant = CalmSurfaceVariantDark,
    onSurfaceVariant = CalmTextSecondaryDark,
    outline = CalmBorderDark,
    outlineVariant = CalmBorderSubtleDark,
    error = StatusErrorDark,
    errorContainer = StatusErrorBgDark,
    onError = Color(0xFF3E0A08),
    onErrorContainer = StatusErrorDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branding colors
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

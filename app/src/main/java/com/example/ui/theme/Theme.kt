package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    onPrimary = OnPrimaryTeal,
    primaryContainer = PrimaryContainerTeal,
    onPrimaryContainer = OnPrimaryContainerTeal,
    secondary = PrimaryTeal,
    onSecondary = OnPrimaryTeal,
    background = NeutralDark,
    onBackground = OnPrimaryTeal,
    surface = NeutralDark,
    onSurface = OnPrimaryTeal,
    surfaceVariant = NeutralMedium,
    error = ErrorCrimson,
    errorContainer = ErrorContainerRed
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = OnPrimaryTeal,
    primaryContainer = PrimaryContainerTeal,
    onPrimaryContainer = OnPrimaryContainerTeal,
    secondary = PrimaryTeal,
    onSecondary = OnPrimaryTeal,
    background = NeutralLight,
    onBackground = NeutralDark,
    surface = NeutralLight,
    onSurface = NeutralDark,
    surfaceVariant = LightGrayContainer,
    error = ErrorCrimson,
    errorContainer = ErrorContainerRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Option to override dynamic colors to protect the brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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

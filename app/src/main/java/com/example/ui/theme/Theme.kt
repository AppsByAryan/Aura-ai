package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuraColorScheme = darkColorScheme(
    primary = AuraCyan,
    onPrimary = Color(0xFF002025),
    primaryContainer = AuraCyanContainer,
    onPrimaryContainer = Color(0xFFB2EBF2),
    secondary = AuraElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = AuraVioletContainer,
    onSecondaryContainer = Color(0xFFE8DDFF),
    tertiary = AuraNeonBlue,
    onTertiary = Color.White,
    background = AuraBlack,
    onBackground = AuraTextPrimary,
    surface = AuraDarkSurface,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraSurfaceElevated,
    onSurfaceVariant = AuraTextSecondary,
    outline = AuraBorderGlow,
    outlineVariant = Color(0xFF162540),
    error = AuraError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuraColorScheme,
        typography = Typography,
        content = content
    )
}

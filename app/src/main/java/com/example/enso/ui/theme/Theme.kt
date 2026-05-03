package com.example.enso.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EnsoColorScheme = lightColorScheme(
    primary = EnsoPrimary,
    onPrimary = EnsoSurface,
    primaryContainer = EnsoPrimaryContainer,
    onPrimaryContainer = EnsoOnBackground,
    secondary = EnsoSecondaryText,
    onSecondary = EnsoSurface,
    background = EnsoBackground,
    onBackground = EnsoOnBackground,
    surface = EnsoSurface,
    onSurface = EnsoOnBackground,
    surfaceVariant = EnsoSurfaceVariant,
    onSurfaceVariant = EnsoSecondaryText,
    outline = EnsoDivider,
    outlineVariant = EnsoDivider,
    error = EnsoRed,
    onError = EnsoSurface,
)

@Composable
fun EnsoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EnsoColorScheme,
        typography = EnsoTypography,
        content = content
    )
}

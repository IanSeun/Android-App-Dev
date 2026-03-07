package com.g.gradeapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GDarkColorScheme = darkColorScheme(
    primary              = Sapphire,
    onPrimary            = OnSapphire,
    primaryContainer     = SapphireContainer,
    onPrimaryContainer   = Sky,
    secondary            = Sky,
    onSecondary          = Black,
    secondaryContainer   = Navy700,
    onSecondaryContainer = Sky,
    tertiary             = GradeA,
    onTertiary           = Black,
    tertiaryContainer    = GradeAContainer,
    onTertiaryContainer  = GradeA,
    error                = Error,
    onError              = Black,
    errorContainer       = ErrorContainer,
    onErrorContainer     = Error,
    background           = Black,
    onBackground         = TextPrimary,
    surface              = Surf2,
    onSurface            = TextPrimary,
    surfaceVariant       = Surf3,
    onSurfaceVariant     = TextSecondary,
    outline              = NavyOutline,
    outlineVariant       = NavyDivider,
    scrim                = Black,
    inverseSurface       = TextPrimary,
    inverseOnSurface     = Surf2,
    inversePrimary       = SapphireDim,
    surfaceTint          = Sapphire,
)

@Composable
fun GTheme(content: @Composable () -> Unit) {
    // Note: Android-specific status bar/nav bar logic moved to androidMain
    MaterialTheme(
        colorScheme = GDarkColorScheme,
        typography  = GTypography,
        shapes      = GShapes,
        content     = content,
    )
}

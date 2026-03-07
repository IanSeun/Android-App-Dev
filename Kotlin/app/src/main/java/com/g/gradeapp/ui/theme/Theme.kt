// app/src/main/java/com/g/gradeapp/ui/theme/Theme.kt
package com.g.gradeapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor     = Black.toArgb()
            window.navigationBarColor = Navy900.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = GDarkColorScheme,
        typography  = GTypography,
        shapes      = GShapes,
        content     = content,
    )
}

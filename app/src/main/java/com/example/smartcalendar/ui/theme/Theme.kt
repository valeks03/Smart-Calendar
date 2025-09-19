package com.example.smartcalendar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Typography
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = OnPrimary,
    secondary = GreySurface,
    onSecondary = OnSurface,
    tertiary = GreyVariant,
    onTertiary = OnSurface,
    background = GreyBackground,
    onBackground = OnSurface,
    surface = GreySurface,
    onSurface = OnSurface,
    surfaceVariant = GreyVariant,
    onSurfaceVariant = DarkVariant
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,

    background = DarkBackground,
    onBackground = DarkOnSurface,

    surface = DarkSurface,
    onSurface = DarkOnSurface,

    surfaceVariant = DarkVariant,
    onSurfaceVariant = DarkOnSurface
)

/**
 * Единая тема приложения.
 * Dynamic color можно выключить, чтобы всегда были наши «серые».
 */
@Composable
fun SmartCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,     // ← оставим false, чтобы серые были стабильными
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else {
            if (darkTheme) DarkColors else LightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(), // см. ниже; можно оставить стандартный
        content = content
    )
}

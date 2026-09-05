package com.example.campusconnect.ui.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary80,
    onPrimary = BlueOnPrimaryDark,
    secondary = IndigoSecondary80,
    tertiary = AmberTertiary80,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnBackground,
    outline = DarkOutline,
    error = ErrorRed80
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary40,
    onPrimary = LightSurface,
    secondary = IndigoSecondary40,
    tertiary = AmberTertiary40,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnBackground,
    outline = LightOutline,
    error = ErrorRed40
)

/**
 * App-wide theme for CampusConnect.
 *
 * @param darkTheme whether dark mode is active. Defaults to the system setting but the
 *        Profile screen lets the user override it explicitly via [darkTheme]/[onToggleTheme]
 *        wiring in MainActivity.
 * @param dynamicColor when true (Android 12+), uses wallpaper-based Material You colors instead
 *        of the CampusConnect brand palette. Defaults to false so the app keeps a consistent
 *        look across devices.
 */
@Composable
fun CampusConnectTheme(
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
            val insetsController = WindowCompat.getInsetsController(window, view)
            // Status/navigation bar icons switch to dark icons in light mode and
            // light icons in dark mode, so they stay readable in both.
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

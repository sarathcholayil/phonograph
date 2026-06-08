package com.svcj91.naradavoicerecorder.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LightGrayBlue,
    onPrimary = DarkNavy,
    secondary = CoolGrayBlue,
    onSecondary = DarkNavy,
    tertiary = CoralRed,
    onTertiary = DarkNavy,
    background = DarkBg,
    onBackground = LightGrayBlue,
    surface = DarkSurface,
    onSurface = LightGrayBlue,
    error = DeepRed,
    onError = LightGrayBlue
)

private val LightColorScheme = lightColorScheme(
    primary = DarkNavy,
    onPrimary = LightGrayBlue,
    secondary = CoolGrayBlue,
    onSecondary = DarkNavy,
    tertiary = CoralRed,
    onTertiary = LightGrayBlue,
    background = LightGrayBlue,
    onBackground = DarkNavy,
    surface = LightGrayBlue,
    onSurface = DarkNavy,
    error = DeepRed,
    onError = LightGrayBlue
)

@Composable
fun NaradaVoiceRecorderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor to false by default to respect custom premium brand colors
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
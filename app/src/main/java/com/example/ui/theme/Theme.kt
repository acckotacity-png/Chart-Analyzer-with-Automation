package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = ElectricBlue,
    onPrimaryContainer = Color.White,
    secondary = BullishGreen,
    onSecondary = Color.Black,
    tertiary = IndigoAccent,
    background = ChartDarkBg,
    onBackground = TextPrimary,
    surface = ChartCardBg,
    onSurface = TextPrimary,
    surfaceVariant = ChartCardBgElevated,
    onSurfaceVariant = TextSecondary,
    outline = ChartBorder,
    error = BearishRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = ElectricBlue,
    secondary = BullishGreen,
    onSecondary = Color.White,
    tertiary = IndigoAccent,
    background = ChartLightBg,
    onBackground = TextLightPrimary,
    surface = ChartLightCard,
    onSurface = TextLightPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextLightSecondary,
    outline = ChartLightBorder,
    error = BearishRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek trader dark theme for optimal chart viewing
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

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}


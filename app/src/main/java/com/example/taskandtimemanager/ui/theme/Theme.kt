package com.example.taskandtimemanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    tertiary = Accent,
    background = SoftBackground,
    surface = SoftSurface,
    surfaceVariant = SoftSurfaceVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = OutlineSoft,
    error = Error,
    onError = OnError,
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    tertiary = DarkAccent,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    outline = DarkOutlineSoft,
    error = DarkError,
    onError = DarkOnError,
)

@Composable
fun TaskAndTimeManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+; keep but bias towards our custom scheme
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Slightly tint dynamic schemes towards our soft background for consistency
    val colorScheme = baseColorScheme.copy(
        background = if (darkTheme) DarkBackground else SoftBackground,
        surface = if (darkTheme) DarkSurface else SoftSurface,
        surfaceVariant = if (darkTheme) DarkSurfaceVariant else SoftSurfaceVariant,
        outline = if (darkTheme) DarkOutlineSoft else OutlineSoft,
        primary = if (darkTheme) DarkPrimary else Primary,
        secondary = if (darkTheme) DarkSecondary else Secondary,
        tertiary = if (darkTheme) DarkAccent else Accent,
        error = if (darkTheme) DarkError else Error,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

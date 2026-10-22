package ua.x44to.shiftplanner.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

private val LightFallback = lightColorScheme(
    primary = Color(0xFF4F5BD5),
    secondary = Color(0xFF5C5D72),
    tertiary = Color(0xFF7A5368),
)

private val DarkFallback = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    secondary = Color(0xFFC5C4DD),
    tertiary = Color(0xFFE9B9D3),
)

/** Material You: на Android 12+ кольори беруться зі шпалер телефона. */
@Composable
fun ShiftTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkFallback
        else -> LightFallback
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

fun shiftColor(argb: Long): Color = Color(argb.toInt())

/** Читабельний текст поверх кольору зміни. */
fun onShiftColor(c: Color): Color = if (c.luminance() > 0.5f) Color(0xFF1B1B1F) else Color.White

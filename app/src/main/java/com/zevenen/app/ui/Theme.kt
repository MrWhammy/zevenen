package com.zevenen.app.ui

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B34),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB2F2B0),
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF52634F),
    secondaryContainer = Color(0xFFD5E8CE),
    onSecondaryContainer = Color(0xFF101F10),
    tertiary = Color(0xFF39656E),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF97D797),
    onPrimary = Color(0xFF00390B),
    primaryContainer = Color(0xFF155223),
    onPrimaryContainer = Color(0xFFB2F2B0),
    secondary = Color(0xFFB9CCB3),
    secondaryContainer = Color(0xFF3A4B38),
    onSecondaryContainer = Color(0xFFD5E8CE),
    tertiary = Color(0xFFA1CED7),
    error = Color(0xFFFFB4AB),
)

@Composable
fun ZevenenTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

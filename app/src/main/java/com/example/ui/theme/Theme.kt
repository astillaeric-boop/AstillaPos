package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun getCustomColorScheme(themeIndex: Int, isDark: Boolean): androidx.compose.material3.ColorScheme {
    return when (themeIndex) {
        1 -> if (isDark) {
            darkColorScheme(
                primary = SapphirePrimaryDark,
                secondary = SapphireSecondaryDark,
                tertiary = SapphireTertiary,
                background = SlateDarkBackground,
                surface = SlateDarkSurface,
                surfaceVariant = SlateDarkSurfaceVariant,
                onPrimary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = SapphirePrimary,
                secondary = SapphireSecondary,
                tertiary = SapphireTertiary,
                background = LightBackground,
                surface = LightSurface,
                surfaceVariant = LightSurfaceVariant,
                onPrimary = Color.White,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            )
        }
        2 -> if (isDark) {
            darkColorScheme(
                primary = AmberPrimaryDark,
                secondary = AmberSecondaryDark,
                tertiary = AmberTertiary,
                background = Color(0xFF181512),
                surface = Color(0xFF26211C),
                surfaceVariant = Color(0xFF3B332B),
                onPrimary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = AmberPrimary,
                secondary = AmberSecondary,
                tertiary = AmberTertiary,
                background = Color(0xFFFFFDF7),
                surface = LightSurface,
                surfaceVariant = Color(0xFFFEF3C7),
                onPrimary = Color.White,
                onBackground = Color(0xFF1C1917),
                onSurface = Color(0xFF1C1917)
            )
        }
        3 -> if (isDark) {
            darkColorScheme(
                primary = CrimsonPrimaryDark,
                secondary = CrimsonSecondaryDark,
                tertiary = CrimsonTertiary,
                background = Color(0xFF181114),
                surface = Color(0xFF271A21),
                surfaceVariant = Color(0xFF3F2B36),
                onPrimary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = CrimsonPrimary,
                secondary = CrimsonSecondary,
                tertiary = CrimsonTertiary,
                background = Color(0xFFFFF1F2),
                surface = LightSurface,
                surfaceVariant = Color(0xFFFFE4E6),
                onPrimary = Color.White,
                onBackground = Color(0xFF1C1917),
                onSurface = Color(0xFF1C1917)
            )
        }
        else -> if (isDark) {
            // Emerald (Default)
            darkColorScheme(
                primary = EmeraldPrimaryDark,
                secondary = EmeraldSecondaryDark,
                tertiary = EmeraldTertiary,
                background = SlateDarkBackground,
                surface = SlateDarkSurface,
                surfaceVariant = SlateDarkSurfaceVariant,
                onPrimary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = EmeraldPrimary,
                secondary = EmeraldSecondary,
                tertiary = EmeraldTertiary,
                background = LightBackground,
                surface = LightSurface,
                surfaceVariant = LightSurfaceVariant,
                onPrimary = Color.White,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            )
        }
    }
}

@Composable
fun AstillaPosTheme(
    themeIndex: Int = 0,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = getCustomColorScheme(themeIndex, darkTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

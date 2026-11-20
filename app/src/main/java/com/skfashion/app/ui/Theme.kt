package com.skfashion.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** SK Fashion colour tokens. Light and dark values are listed in the README palette table. */
private val Light = lightColorScheme(primary = Color(0xFF4F46E5), onPrimary = Color.White, secondary = Color(0xFF0EA5A4), tertiary = Color(0xFFF59E0B),
    background = Color(0xFFF7F7FB), surface = Color.White, surfaceVariant = Color(0xFFEDEDF7), onBackground = Color(0xFF14142B),
    onSurface = Color(0xFF14142B), onSurfaceVariant = Color(0xFF6B6B85), outline = Color(0xFFD9D9E8), error = Color(0xFFDC2626))
private val Dark = darkColorScheme(primary = Color(0xFF8B83FF), onPrimary = Color(0xFF12102E), secondary = Color(0xFF2DD4BF), tertiary = Color(0xFFFBBF24),
    background = Color(0xFF0E0F1A), surface = Color(0xFF181A2B), surfaceVariant = Color(0xFF23263A), onBackground = Color(0xFFF1F1F8),
    onSurface = Color(0xFFF1F1F8), onSurfaceVariant = Color(0xFFA3A5BE), outline = Color(0xFF34374F), error = Color(0xFFF87171))
object Status { val success = Color(0xFF16A34A); val warning = Color(0xFFF59E0B); val info = Color(0xFF2563EB) }

@Composable fun SkTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)

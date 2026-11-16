package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UnknownDarkColorScheme = darkColorScheme(
  primary = CyberCyan,
  onPrimary = Color(0xFF001F26),
  primaryContainer = Color(0xFF003844),
  onPrimaryContainer = Color(0xFF9DF0FF),
  secondary = QuantumViolet,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF281D54),
  onSecondaryContainer = Color(0xFFE3D9FF),
  tertiary = AmberForge,
  onTertiary = Color(0xFF261600),
  tertiaryContainer = Color(0xFF422800),
  onTertiaryContainer = Color(0xFFFFDDB0),
  error = CrimsonPulse,
  onError = Color.White,
  background = ObsidianBg,
  onBackground = TextPrimary,
  surface = ObsidianSurface,
  onSurface = TextPrimary,
  surfaceVariant = ObsidianSurfaceHigh,
  onSurfaceVariant = TextSecondary,
  outline = ObsidianOutline,
)

private val UnknownLightColorScheme = lightColorScheme(
  primary = Color(0xFF00677D),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFB3EBFF),
  onPrimaryContainer = Color(0xFF001F27),
  secondary = Color(0xFF5A37C8),
  onSecondary = Color.White,
  tertiary = Color(0xFF995A00),
  onTertiary = Color.White,
  error = Color(0xFFB31B44),
  onError = Color.White,
  background = LightBg,
  onBackground = LightTextPrimary,
  surface = LightSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightTextSecondary,
  outline = Color(0xFFCBD5E1),
)

@Composable
fun UnknownUniversesTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) UnknownDarkColorScheme else UnknownLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    content = content,
  )
}

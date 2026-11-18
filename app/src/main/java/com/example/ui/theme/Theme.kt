package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.data.local.AppSettings
import com.example.data.local.ThemeVisualMode

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

private val HighContrastDarkColorScheme = darkColorScheme(
  primary = Color(0xFF40F4FF),
  onPrimary = Color.Black,
  primaryContainer = Color(0xFF004F5C),
  onPrimaryContainer = Color.White,
  secondary = Color(0xFFFFD54F),
  onSecondary = Color.Black,
  secondaryContainer = Color(0xFF3E2D00),
  onSecondaryContainer = Color.White,
  tertiary = Color(0xFF00FF88),
  onTertiary = Color.Black,
  tertiaryContainer = Color(0xFF004022),
  onTertiaryContainer = Color.White,
  error = Color(0xFFFF6E91),
  onError = Color.Black,
  background = Color(0xFF000000),
  onBackground = Color(0xFFFFFFFF),
  surface = Color(0xFF0A0D14),
  onSurface = Color(0xFFFFFFFF),
  surfaceVariant = Color(0xFF161C29),
  onSurfaceVariant = Color(0xFFE2E8F0),
  outline = Color(0xFFB8C7E0),
)

private val HighContrastLightColorScheme = lightColorScheme(
  primary = Color(0xFF004C66),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFBDEEFF),
  onPrimaryContainer = Color(0xFF00141C),
  secondary = Color(0xFF3E1FA8),
  onSecondary = Color.White,
  tertiary = Color(0xFF6B3C00),
  onTertiary = Color.White,
  error = Color(0xFF8C0025),
  onError = Color.White,
  background = Color(0xFFFFFFFF),
  onBackground = Color(0xFF05070B),
  surface = Color(0xFFF3F6FB),
  onSurface = Color(0xFF05070B),
  surfaceVariant = Color(0xFFE2E8F0),
  onSurfaceVariant = Color(0xFF1E293B),
  outline = Color(0xFF1E293B),
)

@Composable
fun UnknownUniversesTheme(
  darkTheme: Boolean = true,
  settings: AppSettings = AppSettings(),
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    settings.themeMode == ThemeVisualMode.HIGH_CONTRAST_LIGHT || !darkTheme -> HighContrastLightColorScheme
    settings.effectiveHighContrast -> HighContrastDarkColorScheme
    else -> UnknownDarkColorScheme
  }
  val typography = remember(settings.textScale) { scaledTypography(settings.textScale) }
  MaterialTheme(
    colorScheme = colorScheme,
    typography = typography,
    content = content,
  )
}

package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import com.example.data.model.RegionalServerCatalog
import com.example.data.model.RegionalServerSpec
import com.example.data.model.WorldRegion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ParticleDensity(val label: String) {
  OFF("Off"),
  REDUCED("Reduced"),
  HIGH("High"),
}

enum class GraphicsQuality(val label: String) {
  LOW("Low (Fast)"),
  MEDIUM("Medium"),
  ULTRA("Ultra"),
}

enum class ThemeVisualMode(val label: String) {
  DARK("Obsidian Dark"),
  HIGH_CONTRAST_DARK("High Contrast Dark"),
  HIGH_CONTRAST_LIGHT("High Contrast Light"),
}

enum class ColorBlindMode(val label: String) {
  NONE("Standard Colors"),
  DEUTERANOPIA("Deuteranopia (Red-Green)"),
  PROTANOPIA("Protanopia (Red-Weak)"),
  TRITANOPIA("Tritanopia (Blue-Yellow)"),
}

@Immutable
data class AppSettings(
  // 1. Controls
  val autoAimEnabled: Boolean = true,
  val autoFireEnabled: Boolean = true,
  val touchCanvasAimEnabled: Boolean = true,
  val leftHandedDeck: Boolean = false,
  val joystickScale: Float = 1.0f,
  val buttonScale: Float = 1.0f,
  val moveSensitivity: Float = 1.0f,
  val hapticFeedbackEnabled: Boolean = true,

  // 2. Performance
  val targetFpsCap: Int = 60,
  val particleDensity: ParticleDensity = ParticleDensity.HIGH,
  val showTacticalGrid: Boolean = true,
  val showMiniRadar: Boolean = true,
  val showFpsCounter: Boolean = false,

  // 3. Graphics & Visual
  val graphicsQuality: GraphicsQuality = GraphicsQuality.ULTRA,
  val themeMode: ThemeVisualMode = ThemeVisualMode.DARK,
  val dynamicGlowEffects: Boolean = true,
  val hudOpacity: Float = 0.92f,
  val screenMarginDp: Int = 0,

  // 4. Accessibility & Health Support
  val reduceFlashes: Boolean = false,
  val reduceMotion: Boolean = false,
  val colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,
  val highContrastMode: Boolean = false,
  val textScale: Float = 1.0f,
  val gameSpeedMultiplier: Float = 1.0f,
  val simplifiedHud: Boolean = false,
  val alwaysShowActionCues: Boolean = true,
  val assistShieldBoost: Boolean = false,

  // 5. Free App Protection & Privacy
  val screenCaptureShield: Boolean = false,
  val chatSafetyFilter: Boolean = true,

  // 6. Audio, Soundtrack & Fullscreen Display
  val musicEnabled: Boolean = true,
  val musicVolume: Float = 0.22f,
  val selectedMusicTrack: Int = 0, // 0 = Auto Cycle, 1 = My Feelings of Music, 2 = Destiny Legendary Stories 1
  val immersiveFullscreen: Boolean = true,

  // 7. World Region, Multi-Server & Region Default Auto-Tuning
  val selectedRegionId: String = WorldRegion.NA_EAST.id,
  val selectedServerId: String = "srv_na_east_1",
  val useRegionDefaultSettings: Boolean = true,

  // 8. Language & Localization
  val selectedLanguageCode: String = AppLanguage.ENGLISH.code,
) {
  val activeLanguage: AppLanguage
    get() = AppLanguage.fromCode(selectedLanguageCode)

  val activeRegion: WorldRegion
    get() = WorldRegion.fromId(selectedRegionId)

  val activeServer: RegionalServerSpec
    get() = RegionalServerCatalog.serverById(selectedServerId).let { srv ->
      if (srv.region == activeRegion) srv else RegionalServerCatalog.defaultServerFor(activeRegion)
    }

  val effectiveDarkTheme: Boolean
    get() = themeMode != ThemeVisualMode.HIGH_CONTRAST_LIGHT

  val effectiveHighContrast: Boolean
    get() = highContrastMode ||
      themeMode == ThemeVisualMode.HIGH_CONTRAST_DARK ||
      themeMode == ThemeVisualMode.HIGH_CONTRAST_LIGHT
}

object AppSettingsPresets {
  fun photosensitiveSafe(current: AppSettings): AppSettings = current.copy(
    reduceFlashes = true,
    reduceMotion = true,
    dynamicGlowEffects = false,
    particleDensity = ParticleDensity.REDUCED,
    graphicsQuality = GraphicsQuality.MEDIUM,
    simplifiedHud = true,
  )

  fun motorAssist(current: AppSettings): AppSettings = current.copy(
    autoAimEnabled = true,
    autoFireEnabled = true,
    touchCanvasAimEnabled = true,
    joystickScale = 1.3f,
    buttonScale = 1.3f,
    moveSensitivity = 1.15f,
    gameSpeedMultiplier = 0.75f,
    assistShieldBoost = true,
  )

  fun lowVisionContrast(current: AppSettings): AppSettings = current.copy(
    highContrastMode = true,
    themeMode = ThemeVisualMode.HIGH_CONTRAST_DARK,
    textScale = 1.3f,
    buttonScale = 1.2f,
    joystickScale = 1.2f,
    alwaysShowActionCues = true,
    dynamicGlowEffects = false,
    showTacticalGrid = false,
  )

  fun maxBatteryPerformance(): AppSettings = AppSettings(
    targetFpsCap = 30,
    particleDensity = ParticleDensity.OFF,
    graphicsQuality = GraphicsQuality.LOW,
    dynamicGlowEffects = false,
    showTacticalGrid = false,
    showMiniRadar = true,
    showFpsCounter = true,
  )

  fun forRegion(current: AppSettings, region: WorldRegion): AppSettings {
    val profile = region.defaultProfile
    val density = runCatching { ParticleDensity.valueOf(profile.particleDensityName) }
      .getOrDefault(current.particleDensity)
    return current.copy(
      targetFpsCap = profile.targetFpsCap,
      autoAimEnabled = profile.autoAimEnabled,
      particleDensity = density,
      showTacticalGrid = profile.showTacticalGrid,
      showMiniRadar = profile.showMiniRadar,
      gameSpeedMultiplier = profile.gameSpeedMultiplier,
      assistShieldBoost = profile.assistShieldBoost,
      reduceMotion = profile.reduceMotion,
    )
  }

  fun defaults(): AppSettings = AppSettings()
}

@Immutable
data class AccessibleArenaPalette(
  val playerAccent: Color,
  val playerSecondary: Color,
  val hostile: Color,
  val boss: Color,
  val dropBuff: Color,
  val dropHazard: Color,
  val enemyShot: Color,
  val safeExfil: Color,
  val radarPlayer: Color,
)

fun resolveArenaPalette(
  worldAccent: Color,
  worldSecondary: Color,
  settings: AppSettings,
): AccessibleArenaPalette {
  val palette = when (settings.colorBlindMode) {
    ColorBlindMode.NONE -> AccessibleArenaPalette(
      playerAccent = worldAccent,
      playerSecondary = worldSecondary,
      hostile = Color(0xFFFF3D71),
      boss = Color(0xFF00E5FF),
      dropBuff = Color(0xFFFFD54F),
      dropHazard = Color(0xFFFF3D71),
      enemyShot = Color(0xFFFF1744),
      safeExfil = Color(0xFF00E676),
      radarPlayer = Color(0xFF00E676),
    )
    ColorBlindMode.DEUTERANOPIA, ColorBlindMode.PROTANOPIA -> AccessibleArenaPalette(
      // Okabe-Ito inspired sky blue + orange-amber vs bright yellow/cyan contrast
      playerAccent = Color(0xFF56B4E9),
      playerSecondary = Color(0xFFF0E442),
      hostile = Color(0xFFE69F00),
      boss = Color(0xFFD55E00),
      dropBuff = Color(0xFFF0E442),
      dropHazard = Color(0xFFCC79A7),
      enemyShot = Color(0xFFD55E00),
      safeExfil = Color(0xFF56B4E9),
      radarPlayer = Color(0xFF56B4E9),
    )
    ColorBlindMode.TRITANOPIA -> AccessibleArenaPalette(
      // Crimson / Aqua / White contrast (avoids blue-yellow confusion)
      playerAccent = Color(0xFF00E5FF),
      playerSecondary = Color(0xFFFF4081),
      hostile = Color(0xFFFF3D71),
      boss = Color(0xFFFFFFFF),
      dropBuff = Color(0xFF00E5FF),
      dropHazard = Color(0xFFFF1744),
      enemyShot = Color(0xFFFF1744),
      safeExfil = Color(0xFF80D8FF),
      radarPlayer = Color(0xFF00E5FF),
    )
  }
  return if (settings.effectiveHighContrast) {
    palette.copy(
      playerAccent = if (settings.colorBlindMode == ColorBlindMode.NONE) Color(0xFF00F0FF) else palette.playerAccent,
      hostile = if (settings.colorBlindMode == ColorBlindMode.NONE) Color(0xFFFFCC00) else palette.hostile,
      enemyShot = Color(0xFFFFFFFF),
      safeExfil = Color(0xFF00FF88),
    )
  } else {
    palette
  }
}

class AppSettingsStore(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _settings = MutableStateFlow(loadFromPrefs())
  val settings: StateFlow<AppSettings> = _settings.asStateFlow()

  val current: AppSettings
    get() = _settings.value

  fun update(transform: (AppSettings) -> AppSettings) {
    val next = sanitize(transform(_settings.value))
    _settings.value = next
    saveToPrefs(next)
  }

  fun replace(nextSettings: AppSettings) {
    val clean = sanitize(nextSettings)
    _settings.value = clean
    saveToPrefs(clean)
  }

  fun resetDefaults() {
    replace(AppSettings())
  }

  fun selectRegion(regionId: String, serverId: String? = null) {
    val region = WorldRegion.fromId(regionId)
    val regionServers = RegionalServerCatalog.serversForRegion(region)
    val chosenServer = regionServers.firstOrNull { it.id == serverId } ?: regionServers.first()
    update { current ->
      val updatedBase = current.copy(
        selectedRegionId = region.id,
        selectedServerId = chosenServer.id,
      )
      if (updatedBase.useRegionDefaultSettings) {
        AppSettingsPresets.forRegion(updatedBase, region)
      } else {
        updatedBase
      }
    }
  }

  fun selectServer(serverId: String) {
    val srv = RegionalServerCatalog.serverById(serverId)
    selectRegion(srv.region.id, srv.id)
  }

  fun setUseRegionDefaultSettings(enabled: Boolean) {
    update { current ->
      val updated = current.copy(useRegionDefaultSettings = enabled)
      if (enabled) AppSettingsPresets.forRegion(updated, updated.activeRegion) else updated
    }
  }

  fun selectLanguage(code: String) {
    val resolved = AppLanguage.fromCode(code)
    update { current ->
      current.copy(selectedLanguageCode = resolved.code)
    }
  }

  private fun sanitize(s: AppSettings): AppSettings = s.copy(
    joystickScale = s.joystickScale.coerceIn(0.8f, 1.4f),
    buttonScale = s.buttonScale.coerceIn(1.0f, 1.35f),
    moveSensitivity = s.moveSensitivity.coerceIn(0.6f, 1.6f),
    targetFpsCap = when {
      s.targetFpsCap <= 30 -> 30
      s.targetFpsCap >= 120 -> 120
      else -> 60
    },
    hudOpacity = s.hudOpacity.coerceIn(0.6f, 1.0f),
    screenMarginDp = s.screenMarginDp.coerceIn(0, 16),
    textScale = s.textScale.coerceIn(1.0f, 1.4f),
    gameSpeedMultiplier = s.gameSpeedMultiplier.coerceIn(0.55f, 1.3f),
    musicVolume = s.musicVolume.coerceIn(0.0f, 0.70f),
    selectedMusicTrack = s.selectedMusicTrack.coerceIn(0, 2),
    selectedLanguageCode = AppLanguage.fromCode(s.selectedLanguageCode).code,
  )

  private fun loadFromPrefs(): AppSettings {
    val d = AppSettings()
    return sanitize(
      AppSettings(
        autoAimEnabled = prefs.getBoolean("auto_aim", d.autoAimEnabled),
        autoFireEnabled = prefs.getBoolean("auto_fire", d.autoFireEnabled),
        touchCanvasAimEnabled = prefs.getBoolean("touch_aim", d.touchCanvasAimEnabled),
        leftHandedDeck = prefs.getBoolean("left_handed", d.leftHandedDeck),
        joystickScale = prefs.getFloat("joystick_scale", d.joystickScale),
        buttonScale = prefs.getFloat("button_scale", d.buttonScale),
        moveSensitivity = prefs.getFloat("move_sens", d.moveSensitivity),
        hapticFeedbackEnabled = prefs.getBoolean("haptics", d.hapticFeedbackEnabled),
        targetFpsCap = prefs.getInt("fps_cap", d.targetFpsCap),
        particleDensity = runCatching {
          ParticleDensity.valueOf(prefs.getString("particle_density", d.particleDensity.name) ?: d.particleDensity.name)
        }.getOrDefault(d.particleDensity),
        showTacticalGrid = prefs.getBoolean("tactical_grid", d.showTacticalGrid),
        showMiniRadar = prefs.getBoolean("mini_radar", d.showMiniRadar),
        showFpsCounter = prefs.getBoolean("fps_counter", d.showFpsCounter),
        graphicsQuality = runCatching {
          GraphicsQuality.valueOf(prefs.getString("gfx_quality", d.graphicsQuality.name) ?: d.graphicsQuality.name)
        }.getOrDefault(d.graphicsQuality),
        themeMode = runCatching {
          ThemeVisualMode.valueOf(prefs.getString("theme_mode", d.themeMode.name) ?: d.themeMode.name)
        }.getOrDefault(d.themeMode),
        dynamicGlowEffects = prefs.getBoolean("dynamic_glow", d.dynamicGlowEffects),
        hudOpacity = prefs.getFloat("hud_opacity", d.hudOpacity),
        screenMarginDp = prefs.getInt("screen_margin", d.screenMarginDp),
        reduceFlashes = prefs.getBoolean("reduce_flashes", d.reduceFlashes),
        reduceMotion = prefs.getBoolean("reduce_motion", d.reduceMotion),
        colorBlindMode = runCatching {
          ColorBlindMode.valueOf(prefs.getString("color_blind", d.colorBlindMode.name) ?: d.colorBlindMode.name)
        }.getOrDefault(d.colorBlindMode),
        highContrastMode = prefs.getBoolean("high_contrast", d.highContrastMode),
        textScale = prefs.getFloat("text_scale", d.textScale),
        gameSpeedMultiplier = prefs.getFloat("game_speed", d.gameSpeedMultiplier),
        simplifiedHud = prefs.getBoolean("simplified_hud", d.simplifiedHud),
        alwaysShowActionCues = prefs.getBoolean("action_cues", d.alwaysShowActionCues),
        assistShieldBoost = prefs.getBoolean("assist_shield", d.assistShieldBoost),
        screenCaptureShield = prefs.getBoolean("screen_capture_shield", d.screenCaptureShield),
        chatSafetyFilter = prefs.getBoolean("chat_safety_filter", d.chatSafetyFilter),
        musicEnabled = prefs.getBoolean("music_enabled", d.musicEnabled),
        musicVolume = prefs.getFloat("music_volume", d.musicVolume),
        selectedMusicTrack = prefs.getInt("selected_music_track", d.selectedMusicTrack),
        immersiveFullscreen = prefs.getBoolean("immersive_fullscreen", d.immersiveFullscreen),
        selectedRegionId = prefs.getString("selected_region_id", d.selectedRegionId) ?: d.selectedRegionId,
        selectedServerId = prefs.getString("selected_server_id", d.selectedServerId) ?: d.selectedServerId,
        useRegionDefaultSettings = prefs.getBoolean("use_region_defaults", d.useRegionDefaultSettings),
        selectedLanguageCode = prefs.getString("selected_language_code", d.selectedLanguageCode) ?: d.selectedLanguageCode,
      )
    )
  }

  private fun saveToPrefs(s: AppSettings) {
    prefs.edit {
      putBoolean("auto_aim", s.autoAimEnabled)
      putBoolean("auto_fire", s.autoFireEnabled)
      putBoolean("touch_aim", s.touchCanvasAimEnabled)
      putBoolean("left_handed", s.leftHandedDeck)
      putFloat("joystick_scale", s.joystickScale)
      putFloat("button_scale", s.buttonScale)
      putFloat("move_sens", s.moveSensitivity)
      putBoolean("haptics", s.hapticFeedbackEnabled)
      putInt("fps_cap", s.targetFpsCap)
      putString("particle_density", s.particleDensity.name)
      putBoolean("tactical_grid", s.showTacticalGrid)
      putBoolean("mini_radar", s.showMiniRadar)
      putBoolean("fps_counter", s.showFpsCounter)
      putString("gfx_quality", s.graphicsQuality.name)
      putString("theme_mode", s.themeMode.name)
      putBoolean("dynamic_glow", s.dynamicGlowEffects)
      putFloat("hud_opacity", s.hudOpacity)
      putInt("screen_margin", s.screenMarginDp)
      putBoolean("reduce_flashes", s.reduceFlashes)
      putBoolean("reduce_motion", s.reduceMotion)
      putString("color_blind", s.colorBlindMode.name)
      putBoolean("high_contrast", s.highContrastMode)
      putFloat("text_scale", s.textScale)
      putFloat("game_speed", s.gameSpeedMultiplier)
      putBoolean("simplified_hud", s.simplifiedHud)
      putBoolean("action_cues", s.alwaysShowActionCues)
      putBoolean("assist_shield", s.assistShieldBoost)
      putBoolean("screen_capture_shield", s.screenCaptureShield)
      putBoolean("chat_safety_filter", s.chatSafetyFilter)
      putBoolean("music_enabled", s.musicEnabled)
      putFloat("music_volume", s.musicVolume)
      putInt("selected_music_track", s.selectedMusicTrack)
      putBoolean("immersive_fullscreen", s.immersiveFullscreen)
      putString("selected_region_id", s.selectedRegionId)
      putString("selected_server_id", s.selectedServerId)
      putBoolean("use_region_defaults", s.useRegionDefaultSettings)
      putString("selected_language_code", s.selectedLanguageCode)
    }
  }

  companion object {
    const val PREFS_NAME = "unknown_os_app_settings"
  }
}

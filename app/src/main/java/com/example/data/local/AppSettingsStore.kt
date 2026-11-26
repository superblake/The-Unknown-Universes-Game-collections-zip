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

enum class RamGraphicsMode(
  val id: String,
  val label: String,
  val defaultBudgetMb: Int,
  val detailSummary: String,
) {
  LOW_RAM_BOOST(
    id = "low_ram_boost",
    label = "Low-RAM Boost (Max FPS)",
    defaultBudgetMb = 128,
    detailSummary = "128 MB cap • Minimal brush cache, particles & towers for max FPS",
  ),
  BALANCED(
    id = "balanced",
    label = "Balanced RAM",
    defaultBudgetMb = 256,
    detailSummary = "256 MB cap • Standard LRU brush cache & crisp visuals",
  ),
  HIGH_RAM_DETAIL(
    id = "high_ram_detail",
    label = "High-RAM Detail",
    defaultBudgetMb = 512,
    detailSummary = "512 MB cap • Expanded shader cache, 18-tower skyline & rich arcs",
  ),
  MAX_RAM_ULTRA(
    id = "max_ram_ultra",
    label = "Max-RAM Ultra",
    defaultBudgetMb = 1024,
    detailSummary = "1024 MB cap • Full 16-slot LRU shader cache, 22 towers & ultra bloom",
  );

  companion object {
    fun fromId(id: String): RamGraphicsMode =
      entries.firstOrNull { it.id == id } ?: BALANCED
  }
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

enum class CollectionResolutionMode(
  val id: String,
  val label: String,
  val shortTag: String,
  val targetWidthPx: Int,
  val targetHeightPx: Int,
  val cardAspectRatio: Float,
  val bitmapMaxDim: Int,
  val arenaZoomScale: Float,
  val summary: String,
) {
  AUTO(
    id = "auto",
    label = "Auto (Device Native)",
    shortTag = "AUTO",
    targetWidthPx = 0,
    targetHeightPx = 0,
    cardAspectRatio = 16f / 9.5f,
    bitmapMaxDim = 960,
    arenaZoomScale = 1.0f,
    summary = "Adapts automatically to phone, foldable, or tablet displays (including Galaxy Tab A9+)",
  ),
  SD_480P(
    id = "sd_480",
    label = "480p SD (854×480)",
    shortTag = "480P",
    targetWidthPx = 854,
    targetHeightPx = 480,
    cardAspectRatio = 16f / 9f,
    bitmapMaxDim = 480,
    arenaZoomScale = 0.88f,
    summary = "Compact 16:9 low-overhead preset for older devices & maximum battery life",
  ),
  QHD_540P(
    id = "qhd_540",
    label = "540p qHD (960×540)",
    shortTag = "540P",
    targetWidthPx = 960,
    targetHeightPx = 540,
    cardAspectRatio = 16f / 9f,
    bitmapMaxDim = 540,
    arenaZoomScale = 0.91f,
    summary = "Balanced handheld qHD layout for slim screens and smooth frame pacing",
  ),
  HD_720(
    id = "hd_720",
    label = "720p HD (1280×720)",
    shortTag = "720P",
    targetWidthPx = 1280,
    targetHeightPx = 720,
    cardAspectRatio = 16f / 9f,
    bitmapMaxDim = 640,
    arenaZoomScale = 0.94f,
    summary = "Crisp 16:9 HD layout for phones and smooth battery performance",
  ),
  WXGA_800(
    id = "wxga_800",
    label = "800p Tablet (1280×800)",
    shortTag = "800P 16:10",
    targetWidthPx = 1280,
    targetHeightPx = 800,
    cardAspectRatio = 16f / 10f,
    bitmapMaxDim = 800,
    arenaZoomScale = 0.96f,
    summary = "Lightweight 16:10 tablet aspect ratio for 8–11\" Android tablets",
  ),
  HD_PLUS_900(
    id = "hd_plus_900",
    label = "900p HD+ (1600×900)",
    shortTag = "900P",
    targetWidthPx = 1600,
    targetHeightPx = 900,
    cardAspectRatio = 16f / 9f,
    bitmapMaxDim = 900,
    arenaZoomScale = 0.98f,
    summary = "Widescreen HD+ density bridging mobile and desktop showcase views",
  ),
  FHD_1080(
    id = "fhd_1080",
    label = "1080p FHD (1920×1080)",
    shortTag = "1080P",
    targetWidthPx = 1920,
    targetHeightPx = 1080,
    cardAspectRatio = 16f / 9f,
    bitmapMaxDim = 960,
    arenaZoomScale = 1.0f,
    summary = "Standard sharp 16:9 widescreen collection showcase & Arena framing",
  ),
  FHD_PLUS_20_9(
    id = "fhd_plus_2400",
    label = "1080p+ Tall (2400×1080)",
    shortTag = "20:9 FHD+",
    targetWidthPx = 2400,
    targetHeightPx = 1080,
    cardAspectRatio = 20f / 10.5f,
    bitmapMaxDim = 1080,
    arenaZoomScale = 1.02f,
    summary = "Modern 20:9 OLED smartphone cinema framing for wide banners",
  ),
  TABLET_WUXGA_1200(
    id = "wuxga_1200",
    label = "Tab A9+ 1200p (1920×1200)",
    shortTag = "1200P 16:10",
    targetWidthPx = 1920,
    targetHeightPx = 1200,
    cardAspectRatio = 16f / 10f,
    bitmapMaxDim = 1200,
    arenaZoomScale = 1.04f,
    summary = "Native 16:10 widescreen tuned specifically for Samsung Galaxy Tab A9+",
  ),
  QHD_1440(
    id = "qhd_1440",
    label = "1440p QHD (2560×1440)",
    shortTag = "1440P",
    targetWidthPx = 2560,
    targetHeightPx = 1440,
    cardAspectRatio = 16f / 9f,
    bitmapMaxDim = 1440,
    arenaZoomScale = 1.08f,
    summary = "Quad-HD high-density artwork sampling and crisper Arena perspective detail",
  ),
  WQXGA_1600(
    id = "wqxga_1600",
    label = "1600p Pro Tablet (2560×1600)",
    shortTag = "1600P 16:10",
    targetWidthPx = 2560,
    targetHeightPx = 1600,
    cardAspectRatio = 16f / 10f,
    bitmapMaxDim = 1600,
    arenaZoomScale = 1.10f,
    summary = "High-DPI 16:10 tablet widescreen for flagship large-screen slabs",
  ),
  ULTRAWIDE_1440(
    id = "uwqhd_1440",
    label = "Ultrawide 21:9 (3440×1440)",
    shortTag = "21:9 UWQHD",
    targetWidthPx = 3440,
    targetHeightPx = 1440,
    cardAspectRatio = 21f / 9.5f,
    bitmapMaxDim = 1720,
    arenaZoomScale = 1.12f,
    summary = "Panoramic 21:9 collection cards for Dex, ChromeOS, foldables & external displays",
  ),
  UHD_4K(
    id = "uhd_2160",
    label = "4K UHD (3840×2160)",
    shortTag = "4K UHD",
    targetWidthPx = 3840,
    targetHeightPx = 2160,
    cardAspectRatio = 16f / 9f,
    bitmapMaxDim = 1920,
    arenaZoomScale = 1.14f,
    summary = "Ultra-HD collection artwork decoding & multi-column wide viewport showcase",
  );

  companion object {
    fun fromId(id: String): CollectionResolutionMode =
      entries.firstOrNull { it.id == id } ?: AUTO
  }
}

enum class FullscreenMode(
  val id: String,
  val label: String,
  val summary: String,
) {
  APP_WIDE(
    id = "app_wide",
    label = "App-Wide Immersive",
    summary = "Hides Android status bar, nav pill, and Galaxy Tab taskbar across menus & Arena",
  ),
  ARENA_ONLY(
    id = "arena_only",
    label = "Arena Only",
    summary = "Shows OS system bars in menus and hides them during active Arena combat",
  ),
  OFF(
    id = "off",
    label = "Standard Window",
    summary = "Keeps system bars visible at all times while remaining edge-to-edge safe",
  );

  companion object {
    fun fromId(id: String): FullscreenMode =
      entries.firstOrNull { it.id == id } ?: APP_WIDE
  }
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
  val ramGraphicsMode: RamGraphicsMode = RamGraphicsMode.BALANCED,
  val ramGraphicsBudgetMb: Int = 256,
  val ramShaderCacheEnabled: Boolean = true,
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
  val fullscreenMode: FullscreenMode = FullscreenMode.APP_WIDE,
  val collectionResolutionMode: CollectionResolutionMode = CollectionResolutionMode.AUTO,
  val menuScale: Float = 1.0f,

  // 7. World Region, Multi-Server & Region Default Auto-Tuning
  val selectedRegionId: String = WorldRegion.NA_EAST.id,
  val selectedServerId: String = "srv_na_east_1",
  val useRegionDefaultSettings: Boolean = true,

  // 8. Language & Localization
  val selectedLanguageCode: String = AppLanguage.ENGLISH.code,

  // 9. Controller & Virtual Gamepad Customization
  val controllerStyle: String = "console_gamepad",
  val controllerOpacity: Float = 0.85f,
  val controllerScale: Float = 1.0f,
) {
  fun shouldHideSystemBars(inArena: Boolean): Boolean =
    immersiveFullscreen && when (fullscreenMode) {
      FullscreenMode.APP_WIDE -> true
      FullscreenMode.ARENA_ONLY -> inArena
      FullscreenMode.OFF -> false
    }

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
    ramGraphicsMode = RamGraphicsMode.LOW_RAM_BOOST,
    ramGraphicsBudgetMb = 128,
    ramShaderCacheEnabled = false,
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

  private fun sanitizeFloat(value: Float, min: Float, max: Float, fallback: Float): Float =
    if (value.isFinite()) value.coerceIn(min, max) else fallback

  private fun sanitize(s: AppSettings): AppSettings {
    val normalizedFullscreenMode = if (!s.immersiveFullscreen) {
      FullscreenMode.OFF
    } else {
      s.fullscreenMode
    }
    return s.copy(
      joystickScale = sanitizeFloat(s.joystickScale, 0.8f, 1.4f, 1.0f),
      buttonScale = sanitizeFloat(s.buttonScale, 1.0f, 1.35f, 1.0f),
      moveSensitivity = sanitizeFloat(s.moveSensitivity, 0.6f, 1.6f, 1.0f),
      targetFpsCap = when {
        s.targetFpsCap <= 30 -> 30
        s.targetFpsCap >= 120 -> 120
        else -> 60
      },
      ramGraphicsBudgetMb = s.ramGraphicsBudgetMb.coerceIn(64, 1024),
      hudOpacity = sanitizeFloat(s.hudOpacity, 0.6f, 1.0f, 0.92f),
      screenMarginDp = s.screenMarginDp.coerceIn(0, 16),
      textScale = sanitizeFloat(s.textScale, 1.0f, 1.4f, 1.0f),
      menuScale = sanitizeFloat(s.menuScale, 0.75f, 1.35f, 1.0f),
      gameSpeedMultiplier = sanitizeFloat(s.gameSpeedMultiplier, 0.55f, 1.3f, 1.0f),
      musicVolume = sanitizeFloat(s.musicVolume, 0.0f, 0.70f, 0.22f),
      selectedMusicTrack = s.selectedMusicTrack.coerceIn(0, 2),
      fullscreenMode = normalizedFullscreenMode,
      immersiveFullscreen = normalizedFullscreenMode != FullscreenMode.OFF,
      selectedLanguageCode = AppLanguage.fromCode(s.selectedLanguageCode).code,
    )
  }

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
        ramGraphicsMode = runCatching {
          RamGraphicsMode.fromId(prefs.getString("ram_gfx_mode", d.ramGraphicsMode.id) ?: d.ramGraphicsMode.id)
        }.getOrDefault(d.ramGraphicsMode),
        ramGraphicsBudgetMb = prefs.getInt("ram_gfx_budget_mb", d.ramGraphicsBudgetMb),
        ramShaderCacheEnabled = prefs.getBoolean("ram_shader_cache", d.ramShaderCacheEnabled),
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
        fullscreenMode = runCatching {
          FullscreenMode.fromId(prefs.getString("fullscreen_mode", d.fullscreenMode.id) ?: d.fullscreenMode.id)
        }.getOrDefault(d.fullscreenMode),
        collectionResolutionMode = runCatching {
          CollectionResolutionMode.fromId(
            prefs.getString("collection_resolution_mode", d.collectionResolutionMode.id)
              ?: d.collectionResolutionMode.id
          )
        }.getOrDefault(d.collectionResolutionMode),
        menuScale = prefs.getFloat("menu_scale", d.menuScale),
        selectedRegionId = prefs.getString("selected_region_id", d.selectedRegionId) ?: d.selectedRegionId,
        selectedServerId = prefs.getString("selected_server_id", d.selectedServerId) ?: d.selectedServerId,
        useRegionDefaultSettings = prefs.getBoolean("use_region_defaults", d.useRegionDefaultSettings),
        selectedLanguageCode = prefs.getString("selected_language_code", d.selectedLanguageCode) ?: d.selectedLanguageCode,
        controllerStyle = prefs.getString("controller_style", d.controllerStyle) ?: d.controllerStyle,
        controllerOpacity = prefs.getFloat("controller_opacity", d.controllerOpacity),
        controllerScale = prefs.getFloat("controller_scale", d.controllerScale),
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
      putString("ram_gfx_mode", s.ramGraphicsMode.id)
      putInt("ram_gfx_budget_mb", s.ramGraphicsBudgetMb)
      putBoolean("ram_shader_cache", s.ramShaderCacheEnabled)
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
      putString("fullscreen_mode", s.fullscreenMode.id)
      putString("collection_resolution_mode", s.collectionResolutionMode.id)
      putFloat("menu_scale", s.menuScale)
      putString("selected_region_id", s.selectedRegionId)
      putString("selected_server_id", s.selectedServerId)
      putBoolean("use_region_defaults", s.useRegionDefaultSettings)
      putString("selected_language_code", s.selectedLanguageCode)
      putString("controller_style", s.controllerStyle)
      putFloat("controller_opacity", s.controllerOpacity)
      putFloat("controller_scale", s.controllerScale)
    }
  }

  companion object {
    const val PREFS_NAME = "unknown_os_app_settings"
  }
}

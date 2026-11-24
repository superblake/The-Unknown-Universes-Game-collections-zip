package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettings
import com.example.data.local.AppSettingsPresets
import com.example.data.local.AppSettingsStore
import com.example.data.local.ColorBlindMode
import com.example.data.local.GraphicsQuality
import com.example.data.local.ParticleDensity
import com.example.data.local.ThemeVisualMode
import com.example.data.local.resolveArenaPalette
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.UnknownUniversesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class SettingsAndAccessibilityTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences(AppSettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  @Test
  fun appSettingsStore_persistsAndRestoresAllCategoriesAndPresets() {
    val store = AppSettingsStore(context)
    val modified = AppSettingsPresets.motorAssist(
      AppSettingsPresets.photosensitiveSafe(store.current)
    ).copy(
      leftHandedDeck = true,
      targetFpsCap = 30,
      colorBlindMode = ColorBlindMode.DEUTERANOPIA,
      themeMode = ThemeVisualMode.HIGH_CONTRAST_LIGHT,
      textScale = 1.3f,
      showFpsCounter = true,
      screenMarginDp = 12,
    )
    store.replace(modified)

    // Re-instantiate store from SharedPreferences to verify durability
    val reloaded = AppSettingsStore(context).current
    assertTrue(reloaded.reduceFlashes)
    assertTrue(reloaded.reduceMotion)
    assertTrue(reloaded.leftHandedDeck)
    assertTrue(reloaded.assistShieldBoost)
    assertEquals(30, reloaded.targetFpsCap)
    assertEquals(ColorBlindMode.DEUTERANOPIA, reloaded.colorBlindMode)
    assertEquals(ThemeVisualMode.HIGH_CONTRAST_LIGHT, reloaded.themeMode)
    assertFalse(reloaded.effectiveDarkTheme)
    assertTrue(reloaded.effectiveHighContrast)
    assertEquals(1.3f, reloaded.textScale, 0.01f)
    assertTrue(reloaded.showFpsCounter)
    assertEquals(12, reloaded.screenMarginDp)

    val battery = AppSettingsPresets.maxBatteryPerformance()
    assertEquals(30, battery.targetFpsCap)
    assertEquals(ParticleDensity.OFF, battery.particleDensity)
    assertEquals(GraphicsQuality.LOW, battery.graphicsQuality)

    store.resetDefaults()
    assertEquals(AppSettings(), store.current)
  }

  @Test
  fun resolveArenaPalette_producesDistinctColorBlindAndHighContrastPalettes() {
    val standard = resolveArenaPalette(Color.Cyan, Color.Magenta, AppSettings())
    val deut = resolveArenaPalette(
      Color.Cyan,
      Color.Magenta,
      AppSettings(colorBlindMode = ColorBlindMode.DEUTERANOPIA),
    )
    val trit = resolveArenaPalette(
      Color.Cyan,
      Color.Magenta,
      AppSettings(colorBlindMode = ColorBlindMode.TRITANOPIA),
    )
    val hiContrast = resolveArenaPalette(
      Color.Cyan,
      Color.Magenta,
      AppSettings(highContrastMode = true),
    )

    assertNotEquals(standard.hostile, deut.hostile)
    assertNotEquals(deut.hostile, trit.hostile)
    assertEquals(Color.White, hiContrast.enemyShot)
  }

  @Test
  fun arenaSimState_respectsReduceFlashesParticlesOffAndGameSpeed() {
    val world = UniverseCatalog.worldById("sector_zero")
    val loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan)
    val calmSettings = AppSettings(
      reduceFlashes = true,
      particleDensity = ParticleDensity.OFF,
      gameSpeedMultiplier = 0.6f,
      assistShieldBoost = true,
    )
    val sim = ArenaSimState(
      world = world,
      modeId = "zombies",
      loadout = loadout,
      hasConsumableEquipped = false,
      initialSettings = calmSettings,
    )

    // Trigger power near hostiles — particles must remain 0 because ParticleDensity.OFF
    sim.hostiles.first().apply {
      x = sim.playerX
      y = sim.playerY
    }
    sim.triggerPower()
    assertEquals(0, sim.sparks.size)

    // Damage flash must remain suppressed when reduceFlashes is ON
    val enemyShot = com.example.ui.screens.ArenaBolt(
      x = sim.playerX,
      y = sim.playerY,
      vx = 0f,
      vy = 0f,
      life = 10f,
      isEnemyShot = true,
    )
    sim.bolts.add(enemyShot)
    sim.stepFrame(0.016f)
    assertEquals(0f, sim.damageFlashTicks, 0.001f)
  }

  @Test
  fun settingsDialog_tabsAndPresetsUpdateSettingsLive() {
    var current by mutableStateOf(AppSettings())
    var dismissed = false

    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = current.effectiveDarkTheme, settings = current) {
        SettingsDialog(
          settings = current,
          onUpdateSettings = { fn -> current = fn(current) },
          onReplaceSettings = { current = it },
          onResetDefaults = { current = AppSettings() },
          onDismiss = { dismissed = true },
        )
      }
    }

    composeRule.onNodeWithTag("settings_dialog_root").assertIsDisplayed()
    composeRule.onNodeWithTag("preset_photosensitive_btn").performClick()
    assertTrue(current.reduceFlashes)
    assertTrue(current.reduceMotion)

    composeRule.onNodeWithTag("settings_tab_controls").performClick()
    composeRule.onNodeWithTag("switch_left_handed").performScrollTo().performClick()
    assertTrue(current.leftHandedDeck)

    composeRule.onNodeWithTag("settings_tab_performance").performClick()
    composeRule.onNodeWithTag("chip_fps_30").performScrollTo().performClick()
    assertEquals(30, current.targetFpsCap)

    composeRule.onNodeWithTag("done_settings_btn").performClick()
    assertTrue(dismissed)
  }

  @Test
  fun arenaScreen_showsFpsBadgeSettingsButtonAndLeftHandedControls() {
    val profile = OperatorProfile(
      userId = "u1",
      callsign = "BlakePrime",
      activeUniverseId = "sector_zero",
      activeMode = "zombies",
      activeRigId = "r1",
    )
    val rig = RigAsset(id = "r1", userId = "u1", name = "Vanguard", archetype = "operator")
    var openedSettings = false
    val customSettings = AppSettings(
      showFpsCounter = true,
      leftHandedDeck = true,
      highContrastMode = true,
      textScale = 1.2f,
    )

    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = customSettings.effectiveDarkTheme, settings = customSettings) {
        ArenaScreen(
          profile = profile,
          armory = emptyList(),
          rigs = listOf(rig),
          settings = customSettings,
          isPausedForSettings = false,
          onOpenSettings = { openedSettings = true },
          onSaveMatchAndExit = { _, _, _, _, _ -> },
        )
      }
    }
    composeRule.mainClock.advanceTimeByFrame()

    composeRule.onNodeWithTag("fps_counter_badge").assertIsDisplayed()
    composeRule.onNodeWithTag("arena_joystick").assertIsDisplayed()
    composeRule.onNodeWithTag("settings_arena_btn").assertIsDisplayed().performClick()
    composeRule.mainClock.advanceTimeByFrame()
    assertTrue(openedSettings)
  }
}

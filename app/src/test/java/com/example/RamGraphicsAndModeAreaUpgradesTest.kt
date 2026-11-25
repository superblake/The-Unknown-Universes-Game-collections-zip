package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettings
import com.example.data.local.AppSettingsPresets
import com.example.data.local.AppSettingsStore
import com.example.data.local.ParticleDensity
import com.example.data.local.ProgressionStore
import com.example.data.local.RamGraphicsMode
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.OperatorProfile
import com.example.data.model.OperatorProgressionState
import com.example.data.model.PowerTier
import com.example.data.model.StarRankMeta
import com.example.data.model.UniverseCatalog
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.OsDesktopScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.UnknownUniversesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class RamGraphicsAndModeAreaUpgradesTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences(AppSettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
      .edit().clear().commit()
    context.getSharedPreferences("uu_progression_v1_ram_upgrade_test", Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  @Test
  fun appSettingsStore_persistsRamGraphicsModesAndClampsBudget() {
    val store = AppSettingsStore(context)
    store.replace(
      AppSettings(
        ramGraphicsMode = RamGraphicsMode.MAX_RAM_ULTRA,
        ramGraphicsBudgetMb = 4096, // must clamp to 1024
        ramShaderCacheEnabled = true,
      )
    )

    val reloaded = AppSettingsStore(context).current
    assertEquals(RamGraphicsMode.MAX_RAM_ULTRA, reloaded.ramGraphicsMode)
    assertEquals(1024, reloaded.ramGraphicsBudgetMb)
    assertTrue(reloaded.ramShaderCacheEnabled)

    store.update { it.copy(ramGraphicsBudgetMb = 8) } // must clamp to 64
    assertEquals(64, AppSettingsStore(context).current.ramGraphicsBudgetMb)

    val battery = AppSettingsPresets.maxBatteryPerformance()
    assertEquals(RamGraphicsMode.LOW_RAM_BOOST, battery.ramGraphicsMode)
    assertEquals(128, battery.ramGraphicsBudgetMb)
    assertFalse(battery.ramShaderCacheEnabled)
  }

  @Test
  fun progressionStore_upgradesModesAndAreasAndPersistsAcrossReload() {
    val store = ProgressionStore(context, "ram_upgrade_test")
    val firstAreaId = MultiverseArsenalCatalog.customAreas.first().id

    // Mode tier & star upgrades with sufficient Blue Essence
    val modeTierRes = store.upgradeModeTier("zombies", 50_000L)
    assertTrue(modeTierRes.isSuccess)
    assertEquals(2, store.current.modeTier("zombies"))

    val modeStarRes = store.upgradeModeStar("zombies", 50_000L)
    assertTrue(modeStarRes.isSuccess)
    assertEquals(2, store.current.modeStar("zombies"))

    // Area feature & star upgrades
    val gridRes = store.upgradeAreaFeature(firstAreaId, "power_grid", 50_000L)
    assertTrue(gridRes.isSuccess)
    assertEquals(2, store.current.areaState(firstAreaId).powerGridTier)

    val starRes = store.upgradeAreaStar(firstAreaId, 50_000L)
    assertTrue(starRes.isSuccess)
    assertEquals(2, store.current.areaState(firstAreaId).starLevel)

    // Verify reload from SharedPreferences preserves mode and area upgrades
    val reloaded = ProgressionStore(context, "ram_upgrade_test").current
    assertEquals(2, reloaded.modeTier("zombies"))
    assertEquals(2, reloaded.modeStar("zombies"))
    assertEquals(2, reloaded.areaState(firstAreaId).powerGridTier)
    assertEquals(2, reloaded.areaState(firstAreaId).starLevel)

    // Bulk boost / max operations cover all playable areas and all modes
    store.setAllTiers(PowerTier.MAX_TIER)
    store.setAllStars(StarRankMeta.MAX_STARS)
    val maxed = ProgressionStore(context, "ram_upgrade_test").current
    UniverseCatalog.modes.forEach { m ->
      assertEquals(PowerTier.MAX_TIER, maxed.modeTier(m.id))
      assertEquals(StarRankMeta.MAX_STARS, maxed.modeStar(m.id))
    }
    MultiverseArsenalCatalog.customAreas.forEach { a ->
      val st = maxed.areaState(a.id)
      assertEquals(PowerTier.MAX_TIER, st.powerGridTier)
      assertEquals(StarRankMeta.MAX_STARS, st.starLevel)
    }
  }

  @Test
  fun arenaLoadoutAndSim_scaleWithModeTiersAndRespectLowRamParticleCaps() {
    val world = UniverseCatalog.worldById("sector_zero")
    val baseProg = OperatorProgressionState()
    val upgradedProg = baseProg.copy(
      modeTierMap = mapOf("zombies" to 12),
      modeStarMap = mapOf("zombies" to 5),
    )

    val baseLoadout = ArenaLoadoutStats.fromArmory(
      armory = emptyList(),
      fallbackAccent = Color.Cyan,
      progression = baseProg,
      activeModeId = "zombies",
    )
    val boostedLoadout = ArenaLoadoutStats.fromArmory(
      armory = emptyList(),
      fallbackAccent = Color.Cyan,
      progression = upgradedProg,
      activeModeId = "zombies",
    )
    assertTrue(boostedLoadout.maxShield > baseLoadout.maxShield)
    assertTrue(boostedLoadout.weaponBonusDamage > baseLoadout.weaponBonusDamage)

    val baseSim = ArenaSimState(
      world = world,
      modeId = "zombies",
      loadout = baseLoadout,
      progression = baseProg,
    )

    // Low-RAM Boost keeps particle, arc, and bolt pools strictly bounded
    val lowRamSim = ArenaSimState(
      world = world,
      modeId = "zombies",
      loadout = boostedLoadout,
      initialSettings = AppSettings(
        ramGraphicsMode = RamGraphicsMode.LOW_RAM_BOOST,
        ramGraphicsBudgetMb = 64,
        particleDensity = ParticleDensity.HIGH,
      ),
      progression = upgradedProg,
    )
    assertTrue(lowRamSim.gameplayRewardMultiplier > baseSim.gameplayRewardMultiplier)
    repeat(10) {
      lowRamSim.triggerLethal()
      lowRamSim.triggerPower()
      lowRamSim.stepFrame(0.016f)
    }
    assertTrue(lowRamSim.sparks.size <= 32)
    assertTrue(lowRamSim.arcs.size <= 16)
    assertTrue(lowRamSim.bolts.size <= 40)

    // ParticleDensity.OFF still ensures 0 sparks even when triggerLethal() runs
    val offParticleSim = ArenaSimState(
      world = world,
      modeId = "zombies",
      loadout = boostedLoadout,
      initialSettings = AppSettings(
        ramGraphicsMode = RamGraphicsMode.MAX_RAM_ULTRA,
        particleDensity = ParticleDensity.OFF,
      ),
      progression = upgradedProg,
    )
    offParticleSim.triggerLethal()
    assertEquals(0, offParticleSim.sparks.size)
  }

  @Test
  fun settingsDialog_updatesRamGraphicsModeAndShaderCache() {
    var current by mutableStateOf(AppSettings())

    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = current.effectiveDarkTheme, settings = current) {
        SettingsDialog(
          settings = current,
          onUpdateSettings = { fn -> current = fn(current) },
          onReplaceSettings = { current = it },
          onResetDefaults = { current = AppSettings() },
          onDismiss = {},
        )
      }
    }

    composeRule.onNodeWithTag("settings_tab_visual").performScrollTo().performClick()
    composeRule.onNodeWithTag("chip_ram_mode_max_ram_ultra").performScrollTo().performClick()
    assertEquals(RamGraphicsMode.MAX_RAM_ULTRA, current.ramGraphicsMode)
    assertEquals(1024, current.ramGraphicsBudgetMb)
    assertTrue(current.ramShaderCacheEnabled)

    composeRule.onNodeWithTag("chip_ram_mode_low_ram_boost").performScrollTo().performClick()
    assertEquals(RamGraphicsMode.LOW_RAM_BOOST, current.ramGraphicsMode)
    assertEquals(128, current.ramGraphicsBudgetMb)
    assertFalse(current.ramShaderCacheEnabled)

    composeRule.onNodeWithTag("switch_ram_shader_cache").performScrollTo().performClick()
    assertTrue(current.ramShaderCacheEnabled)
  }

  @Test
  fun osDesktopScreen_modeAndArenaUpgradeButtonsInvokeCallbacks() {
    val sectorZeroAreaId = MultiverseArsenalCatalog.customAreas.first { it.linkedWorldId == "sector_zero" }.id
    var upgradedModeTier = ""
    var upgradedModeStar = ""
    var upgradedAreaFeature = ""
    var upgradedAreaStar = ""

    composeRule.setContent {
      OsDesktopScreen(
        profile = OperatorProfile(
          userId = "u_upgrade",
          callsign = "Architect",
          activeUniverseId = "sector_zero",
          activeMode = "zombies",
          totalEssence = 100_000L,
        ),
        armory = emptyList(),
        rigs = emptyList(),
        matchLogs = emptyList(),
        linkedAccounts = emptyList(),
        activeWallpaperPath = null,
        onSelectUniverse = {},
        onSelectMode = {},
        onLaunchArena = {},
        onOpenForgeRig = {},
        onOpenMediaVault = {},
        onOpenExport = {},
        onCycleWallpaper = {},
        onUpdateCallsign = {},
        onUnlinkAccount = {},
        onStatusMessage = {},
        onUpgradeAreaFeature = { areaId, feat -> upgradedAreaFeature = "$areaId:$feat" },
        onUpgradeAreaStar = { upgradedAreaStar = it },
        onUpgradeModeTier = { upgradedModeTier = it },
        onUpgradeModeStar = { upgradedModeStar = it },
        onSignOut = {},
      )
    }

    composeRule.onNodeWithTag("os_desktop_list").performScrollToNode(hasTestTag("os_modes_row"))
    composeRule.onNodeWithTag("mode_up_tier_zombies").performScrollTo().assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("mode_up_star_zombies").performScrollTo().assertIsDisplayed().performClick()
    assertEquals("zombies", upgradedModeTier)
    assertEquals("zombies", upgradedModeStar)

    composeRule.onNodeWithTag("os_desktop_list").performScrollToNode(hasTestTag("os_arenas_row"))
    composeRule.onNodeWithTag("os_arena_up_tier_$sectorZeroAreaId").performScrollTo().assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("os_arena_up_star_$sectorZeroAreaId").performScrollTo().assertIsDisplayed().performClick()
    assertEquals("$sectorZeroAreaId:power_grid", upgradedAreaFeature)
    assertEquals(sectorZeroAreaId, upgradedAreaStar)
  }
}

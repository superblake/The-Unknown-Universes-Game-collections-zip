package com.example

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.data.local.AppLanguage
import com.example.data.local.AppSettings
import com.example.data.local.AppSettingsStore
import com.example.data.local.GameStringsCatalog
import com.example.data.local.LocalGameStrings
import com.example.data.local.ProgressionStore
import com.example.data.model.CharacterUniverseGroup
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.RegionalServerCatalog
import com.example.data.model.SocialCatalog
import com.example.data.model.UniverseCatalog
import com.example.data.model.WorldRegion
import com.example.ui.screens.ARENA_BORDER_HALF_GATE
import com.example.ui.screens.ARENA_SECTION_SIZE
import com.example.ui.screens.ARENA_WORLD_SIZE
import com.example.ui.screens.ArenaDamageType
import com.example.ui.screens.ArenaHostile
import com.example.ui.screens.ArenaLoadoutStats
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
class BorderedWorldAndRegionsTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("${ProgressionStore.PREFS_PREFIX}_bw_test", Context.MODE_PRIVATE)
      .edit().clear().commit()
    context.getSharedPreferences(AppSettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  private fun newSim(
    worldId: String = "mr_crossover_nexus",
    modeId: String = "hero_patrol",
    areaId: String = "area_hero_frontier",
  ): ArenaSimState {
    val store = ProgressionStore(context, "bw_test")
    store.selectArea(areaId)
    return ArenaSimState(
      world = UniverseCatalog.worldById(worldId),
      modeId = modeId,
      loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current, 25_000L),
      hasConsumableEquipped = true,
      initialSettings = AppSettings(),
      progression = store.current,
      initialBlueEssence = 25_000L,
    )
  }

  @Test
  fun borderedMultiSectionWorld_isLargerAndSegregatesSectionsWithGateways() {
    assertEquals(24.0f, ARENA_WORLD_SIZE, 0.001f)
    assertEquals(8.0f, ARENA_SECTION_SIZE, 0.001f)

    val sim = newSim()
    assertEquals(9, sim.mapSections.size)
    assertEquals(9, sim.mapSections.map { it.name }.distinct().size)
    assertEquals(9, sim.mapSections.map { it.biomeTag }.distinct().size)

    // Verify section lookup places corners and center in distinct segregated sections
    val nw = sim.sectionAt(2.0f, 2.0f)
    val center = sim.sectionAt(12.0f, 12.0f)
    val se = sim.sectionAt(21.0f, 21.0f)
    assertNotEquals(nw.id, center.id)
    assertNotEquals(center.id, se.id)
    assertNotEquals(nw.biomeTag, se.biomeTag)

    // Fortified outer perimeter border clamps movement
    sim.playerX = 23.95f
    sim.playerY = 12.0f
    sim.joyDx = 1.0f
    sim.joyDy = 0.0f
    repeat(10) { sim.stepFrame(0.016f) }
    assertTrue("Outer border wall should clamp X <= 23.82f", sim.playerX <= 23.85f)

    // Internal section border wall at x = 8.0f blocks non-gateway crossing (y = 1.5f)
    sim.playerX = 7.90f
    sim.playerY = 1.50f
    sim.joyDx = 1.0f
    sim.joyDy = 0.0f
    repeat(12) { sim.stepFrame(0.016f) }
    assertTrue("Solid border wall at y=1.5f should block crossing x=8.0f (was ${sim.playerX})", sim.playerX < 8.0f)

    // Gateway opening at y = 4.0f allows smooth crossing past x = 8.0f
    sim.playerX = 7.90f
    sim.playerY = 4.0f
    assertTrue(4.0f - ARENA_BORDER_HALF_GATE < 4.0f)
    sim.joyDx = 1.0f
    sim.joyDy = 0.0f
    repeat(30) { sim.stepFrame(0.016f) }
    assertTrue("Gateway at y=4.0f must allow crossing x=8.0f (was ${sim.playerX})", sim.playerX > 8.05f)
  }

  @Test
  fun enemyImmunityDecaysOverTime_andEnemyAttacksOnlyMirrorPlayerTypeRandomly() {
    val sim = newSim(modeId = "hero_patrol")
    sim.hostiles.clear()
    sim.bolts.clear()

    // 1. Immunity decay over time (high HP so auto-fire doesn't eliminate it prematurely)
    sim.hudAutoFire = false
    val armoredHostile = ArenaHostile(
      x = sim.playerX + 0.6f,
      y = sim.playerY,
      hp = 50_000,
      maxHp = 50_000,
      speed = 0.0001f,
      isElite = true,
      archetype = "dup_conduit",
      immuneTypes = setOf(ArenaDamageType.ENERGY, ArenaDamageType.CRYO),
      immunityDecayTicks = 40f,
      rangedCooldownTicks = 999f,
    )
    sim.hostiles.add(armoredHostile)

    // 60 steps at 0.016s => dt = 0.96 per step => ~57.6 ticks (> 40f decay threshold)
    repeat(60) { sim.stepFrame(0.016f) }
    assertEquals(
      "At least one immunity layer should decay over time",
      1,
      armoredHostile.immuneTypes.size,
    )

    // 2. Enemy ranged attacks must ONLY carry player's active damage type, mirroring at random times
    val store = ProgressionStore(context, "bw_test")
    val customWeaponSim = ArenaSimState(
      world = UniverseCatalog.worldById("gotham_arkham"),
      modeId = "hero_patrol",
      loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current, 10_000L),
      hasConsumableEquipped = true,
      initialSettings = AppSettings(),
      progression = store.current,
      initialBlueEssence = 10_000L,
    )
    customWeaponSim.hostiles.clear()
    customWeaponSim.bolts.clear()
    val expectedPlayerType = customWeaponSim.activeWeaponDamageType()

    repeat(30) { idx ->
      customWeaponSim.hostiles.add(
        ArenaHostile(
          x = customWeaponSim.playerX + 0.8f,
          y = customWeaponSim.playerY + (idx % 3 - 1) * 0.1f,
          hp = 80,
          maxHp = 80,
          speed = 0f,
          isElite = idx % 2 == 0,
          archetype = "blackgate_enforcer",
          rangedCooldownTicks = 1f,
        )
      )
    }

    repeat(15) { customWeaponSim.stepFrame(0.016f) }
    val enemyShots = customWeaponSim.bolts.filter { it.isEnemyShot }
    assertTrue("Frequent enemy attacks should produce multiple bolts", enemyShots.size >= 15)
    assertTrue(
      "Every enemy shot must match player's active damage type ($expectedPlayerType)",
      enemyShots.all { it.damageType == expectedPlayerType },
    )
    val mirroredCount = enemyShots.count { it.isMirroredElemental }
    val neutralCount = enemyShots.count { !it.isMirroredElemental }
    assertTrue("Enemies should mirror player's elemental type randomly (mirrored=$mirroredCount, neutral=$neutralCount)", mirroredCount > 0 && neutralCount > 0)
  }

  @Test
  fun codGtaInfamousPrototypeControls_andExpandedRosterWork() {
    val sim = newSim()
    sim.hostiles.clear()
    sim.bolts.clear()

    // Stance toggle (CoD Cover Crouch vs Standing)
    assertFalse(sim.hudCrouching)
    sim.toggleStance()
    assertTrue(sim.hudCrouching)

    // GTA Target Lock-On toggle
    val lockBefore = sim.gtaLockOnEnabled
    sim.toggleLockOn()
    assertNotEquals(lockBefore, sim.gtaLockOnEnabled)

    // Prototype / inFamous Melee Strike clears nearby enemy bolts & hits hostiles
    val nearbyEnemy = ArenaHostile(
      x = sim.playerX + 0.25f,
      y = sim.playerY,
      hp = 100,
      maxHp = 100,
      speed = 0f,
    )
    sim.hostiles.add(nearbyEnemy)
    sim.bolts.add(
      com.example.ui.screens.ArenaBolt(
        x = sim.playerX + 0.20f,
        y = sim.playerY,
        vx = -0.005f,
        vy = 0f,
        life = 60f,
        isEnemyShot = true,
      )
    )
    sim.triggerMeleeStrike()
    assertTrue("Melee cleave must damage nearby hostiles", nearbyEnemy.hp < 100)
    assertTrue("Melee cleave must deflect nearby enemy shots", sim.bolts.none { it.isEnemyShot })
    assertTrue("Melee cooldown must activate", sim.hudMeleeCdSec > 0)

    // Expanded Character Universe Groups & Outfits (66 outfits across 22 non-ALL groups)
    assertEquals(66, MultiverseArsenalCatalog.outfits.size)
    assertTrue(CharacterUniverseGroup.entries.contains(CharacterUniverseGroup.GTA4_LIBERTY))
    assertTrue(CharacterUniverseGroup.entries.contains(CharacterUniverseGroup.COD_PRIMIS))
    assertTrue(CharacterUniverseGroup.entries.contains(CharacterUniverseGroup.ARKHAM_KNIGHTS))
    assertTrue(CharacterUniverseGroup.entries.contains(CharacterUniverseGroup.BLACKLIGHT_HELLER))
    assertTrue(CharacterUniverseGroup.entries.contains(CharacterUniverseGroup.INFAMOUS_DELSIN))
    assertTrue(CharacterUniverseGroup.entries.contains(CharacterUniverseGroup.KRYPTON_APEX))
  }

  @Test
  fun globalRegionsAndServers_haveFullCoverageAndDistinctDefaults() {
    assertEquals(19, WorldRegion.entries.size)
    assertEquals(58, RegionalServerCatalog.servers.size)

    // Every region must have at least 3 servers & capacity >= 150
    WorldRegion.entries.forEach { region ->
      val regionServers = RegionalServerCatalog.serversForRegion(region)
      assertTrue("Region ${region.displayName} should have >= 3 servers", regionServers.size >= 3)
      assertTrue("All servers in ${region.displayName} should support >= 150 players", regionServers.all { it.maxCapacity >= 150 })
    }

    // All sampleMatches point to valid regions & servers
    SocialCatalog.sampleMatches.forEach { match ->
      val region = WorldRegion.entries.firstOrNull { it.id == match.regionId }
      assertTrue("Match ${match.id} has unknown regionId ${match.regionId}", region != null)
      val server = RegionalServerCatalog.servers.firstOrNull { it.id == match.serverId }
      assertTrue("Match ${match.id} has unknown serverId ${match.serverId}", server != null)
    }

    // Switching regions while useRegionDefaultSettings == true applies that region's profile
    val settingsStore = AppSettingsStore(context)
    assertTrue(settingsStore.current.useRegionDefaultSettings)
    settingsStore.selectRegion(WorldRegion.EAST_ASIA.id)
    assertEquals(WorldRegion.EAST_ASIA.id, settingsStore.current.selectedRegionId)
    assertEquals(120, settingsStore.current.targetFpsCap)
    assertFalse(settingsStore.current.autoAimEnabled)

    settingsStore.selectRegion(WorldRegion.SOUTH_ASIA.id)
    assertEquals(WorldRegion.SOUTH_ASIA.id, settingsStore.current.selectedRegionId)
    assertEquals(60, settingsStore.current.targetFpsCap)
    assertTrue(settingsStore.current.autoAimEnabled)
    assertTrue(settingsStore.current.assistShieldBoost)
  }

  @Test
  fun multiLanguageCatalog_persistenceAndSwitchUiWork() {
    // 1. All 12 languages resolve distinct localized bundles & Arabic marks RTL
    assertEquals(12, AppLanguage.entries.size)
    val deployLabels = AppLanguage.entries.map { lang ->
      val bundle = GameStringsCatalog.forLanguage(lang)
      assertEquals(lang.code, bundle.languageCode)
      assertTrue(bundle.deployArena.isNotBlank())
      assertTrue(bundle.languageSectionTitle.isNotBlank())
      bundle.deployArena
    }
    assertEquals(12, deployLabels.distinct().size)
    assertTrue(AppLanguage.ARABIC.isRtl)
    assertFalse(AppLanguage.ENGLISH.isRtl)

    // 2. Store persistence & independent retention alongside regional defaults
    val store = AppSettingsStore(context)
    store.selectRegion(WorldRegion.EAST_ASIA.id)
    store.selectLanguage(AppLanguage.JAPANESE.code)
    assertEquals(AppLanguage.JAPANESE, store.current.activeLanguage)
    assertEquals(WorldRegion.EAST_ASIA.id, store.current.selectedRegionId)

    val reloaded = AppSettingsStore(context)
    assertEquals(AppLanguage.JAPANESE, reloaded.current.activeLanguage)
    assertEquals(WorldRegion.EAST_ASIA.id, reloaded.current.selectedRegionId)

    // 3. Interactive UI switching inside SettingsDialog
    var liveSettings by mutableStateOf(reloaded.current)
    composeRule.setContent {
      val strings = GameStringsCatalog.forLanguage(liveSettings.activeLanguage)
      CompositionLocalProvider(LocalGameStrings provides strings) {
        UnknownUniversesTheme(darkTheme = liveSettings.effectiveDarkTheme, settings = liveSettings) {
          SettingsDialog(
            settings = liveSettings,
            onUpdateSettings = { transform -> liveSettings = transform(liveSettings) },
            onReplaceSettings = { liveSettings = it },
            onResetDefaults = { liveSettings = AppSettings() },
            onDismiss = {},
            onSelectLanguage = { code ->
              store.selectLanguage(code)
              liveSettings = store.current
            },
          )
        }
      }
    }

    composeRule.onNodeWithTag("settings_lang_es").performScrollTo().performClick()
    composeRule.waitForIdle()
    assertEquals(AppLanguage.SPANISH, liveSettings.activeLanguage)

    composeRule.onNodeWithTag("settings_lang_ar").performScrollTo().assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    assertEquals(AppLanguage.ARABIC, liveSettings.activeLanguage)
    assertEquals(AppLanguage.ARABIC, AppSettingsStore(context).current.activeLanguage)
  }
}

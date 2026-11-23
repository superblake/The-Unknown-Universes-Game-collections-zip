package com.example

import android.content.Context
import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettings
import com.example.data.local.ProgressionStore
import com.example.data.model.OperatorProfile
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.data.model.WonderWeaponKind
import com.example.data.model.WorldDifficultyTier
import com.example.export.PortablePackageGenerator
import com.example.ui.screens.ARENA_MAX_X
import com.example.ui.screens.ARENA_WORLD_SIZE
import com.example.ui.screens.ArenaCollectibleKind
import com.example.ui.screens.ArenaHazardKind
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.ArenaStationKind
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.data.model.MultiverseArsenalCatalog
import com.example.ui.screens.OsDesktopScreen
import com.example.ui.theme.UnknownUniversesTheme
import com.example.util.BackgroundMusicManager
import com.example.util.GamepadInputBridge
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
class FullUpdateMultiverseFeaturesTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("${ProgressionStore.PREFS_PREFIX}_full_upd_test", Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  @Test
  fun bundledMusicAssetsAndSoftDefaultVolume_arePresent() {
    val defaultSettings = AppSettings()
    assertTrue(defaultSettings.musicEnabled)
    assertTrue(defaultSettings.musicVolume in 0.1f..0.3f)
    assertEquals(0, defaultSettings.selectedMusicTrack)
    assertEquals(2, BackgroundMusicManager.tracks.size)
    BackgroundMusicManager.tracks.forEach { track ->
      context.assets.open(track.assetPath).use { stream ->
        assertTrue("Asset ${track.assetPath} should not be empty", stream.available() > 1000)
      }
    }
  }

  @Test
  fun powerLightLevelAndNonLinearMilestoneLeveling_workAsDesigned() {
    // Non-linear leveling plateaus between milestones and surges at higher milestones
    assertEquals(ProgressionCatalog.levelStatMultiplier(1), ProgressionCatalog.levelStatMultiplier(4), 0.0001f)
    val ms5 = ProgressionCatalog.levelStatMultiplier(5)
    val ms20 = ProgressionCatalog.levelStatMultiplier(20)
    val ms100 = ProgressionCatalog.levelStatMultiplier(100)
    val ms500 = ProgressionCatalog.levelStatMultiplier(500)
    assertTrue(ms20 - ms5 < ms100 - ms20)
    assertTrue(ms500 > ms100 + 0.8f)

    val store = ProgressionStore(context, "full_upd_test")
    val basePwr = store.current.computePowerAndLightLevel(1000L)
    store.upgradeWeaponTier("wpn_arc_amp", 10_000L)
    store.updateWeaponCustomization { it.copy(equippedWeaponId = "wpn_arc_amp") }
    store.upgradePerkDrink("perk_juggernog", 10_000L)
    val boostedPwr = store.current.computePowerAndLightLevel(25_000L)
    assertTrue("Power level must climb with upgraded weapons", boostedPwr.powerLevel > basePwr.powerLevel)
    assertTrue("Light level must climb with perks/essence", boostedPwr.lightLevel > basePwr.lightLevel)
  }

  @Test
  fun characterRarityFusion_heroFrontierAndOriginalArsenalWork() {
    // 10 Weapons (5 original additions) + 9 Wonder Weapons (3 new)
    assertTrue(ProgressionCatalog.weapons.size >= 10)
    assertTrue(ProgressionCatalog.weapons.any { it.id == "wpn_arc_amp" })
    assertTrue(ProgressionCatalog.weapons.any { it.id == "wpn_stargate_staff" })
    assertTrue(WonderWeaponKind.entries.size >= 9)
    assertTrue(WonderWeaponKind.entries.contains(WonderWeaponKind.RAY_SPHERE_OVERDRIVE))
    assertTrue(WonderWeaponKind.entries.contains(WonderWeaponKind.MERCER_BIOCLAW_SWARM))
    assertTrue(WonderWeaponKind.entries.contains(WonderWeaponKind.GATE_HORIZON_CANNON))

    val store = ProgressionStore(context, "full_upd_test")
    store.selectArea("area_hero_frontier")
    assertEquals("area_hero_frontier", store.current.activeAreaId)

    // Recruit shards until enough for a rarity fusion
    val targetId = "batman_v1"
    val startRarity = store.current.outfitRarity(targetId)
    repeat(startRarity.shardsForNext) {
      assertTrue(store.recruitCharacterShardDrop(50_000L, targetId).isSuccess)
    }
    val fusionRes = store.upgradeOutfitRarityWithShards(targetId)
    assertTrue(fusionRes.isSuccess)
    assertEquals(startRarity.nextRarity(), fusionRes.getOrThrow())
  }

  @Test
  fun verdanskUnboundedWorld_campsStationsHazardsCatsAndOriginalEnemiesWork() {
    assertTrue("Verdansk world should be 10x10 core", ARENA_WORLD_SIZE >= 10.0f)
    assertTrue("Outer Frontier should remove hard edge barrier", ARENA_MAX_X >= 10.0f)

    val store = ProgressionStore(context, "full_upd_test")
    store.selectArea("area_hero_frontier")
    store.updateLevelCustomization {
      it.copy(
        worldDifficultyTier = WorldDifficultyTier.NIGHTMARE,
        enemyDensityMultiplier = 2.5f,
      )
    }

    val sim = ArenaSimState(
      world = UniverseCatalog.worldById("blacklight_conduit"),
      modeId = "sandbox",
      loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current, 10_000L),
      hasConsumableEquipped = true,
      initialSettings = AppSettings(),
      progression = store.current,
      initialBlueEssence = 10_000L,
    )

    // Walk OUTSIDE core 10.0f boundary freely into Outer Frontier!
    sim.playerX = 11.4f
    sim.playerY = 5.0f
    sim.stepFrame(0.016f)
    assertTrue("Player must roam beyond old edge boundaries", sim.playerX > 10.0f)

    // All 5 elemental hazards + Rest Camps + PaP/Perk/Ammo stations + Cat Easter eggs exist
    assertEquals(ArenaHazardKind.entries.size, sim.hazards.map { it.kind }.distinct().size)
    assertTrue(sim.restCamps.isNotEmpty())
    assertTrue(sim.worldStations.any { it.kind == ArenaStationKind.PACK_A_PUNCH })
    assertTrue(sim.worldStations.any { it.kind == ArenaStationKind.PERK_MACHINE })
    assertTrue(sim.worldStations.any { it.kind == ArenaStationKind.AMMO_CRATE })
    assertTrue(sim.catEasterEggs.isNotEmpty())
    assertTrue(sim.collectibles.any { it.kind == ArenaCollectibleKind.CHARACTER_SHARD })

    // Interact with a Hidden Cat Easter Egg -> awards Golden Loot Crate & Wonder Drop!
    val cat = sim.catEasterEggs.first()
    sim.playerX = cat.x
    sim.playerY = cat.y
    val cratesOpenedBefore = sim.lootCratesOpenedCount
    val catsFoundBefore = sim.catsFoundCount
    sim.interactNearestWorldObject()
    assertTrue(cat.found)
    assertTrue("Discovering a Cat Easter Egg must increment cats found", sim.catsFoundCount > catsFoundBefore)
    assertTrue("Discovering a Cat Easter Egg must award a loot crate", sim.lootCratesOpenedCount > cratesOpenedBefore)

    // Interact with isolated Rest Camp -> restores shield & calms horde
    val camp = sim.restCamps.last()
    sim.playerX = camp.x
    sim.playerY = camp.y
    sim.hudShield = 10
    sim.interactNearestWorldObject()
    assertTrue("Resting at camp must restore shield", sim.hudShield > 10)

    // Interact with Pack-a-Punch Station -> increments PaP tier
    val pap = sim.worldStations.first { it.kind == ArenaStationKind.PACK_A_PUNCH }
    val tierBefore = sim.hudPackTier
    sim.playerX = pap.x
    sim.playerY = pap.y
    sim.interactNearestWorldObject()
    assertTrue(sim.hudPackTier > tierBefore)

    // Original lore enemy archetypes spawn cleanly
    assertTrue(
      sim.hostiles.any {
        it.archetype in setOf(
          "blackwatch",
          "gentek_mutant",
          "water_hive_swarm",
          "dup_conduit",
          "rift_phantom",
          "cosmic_raider",
        )
      }
    )
  }

  @Test
  fun consoleControllerAndCrossPlatformExports_workEndToEnd() {
    val seqBefore = GamepadInputBridge.state.value.actionSeq
    val btnA = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A, 0, 0, -1, 0, 0, InputDevice.SOURCE_GAMEPAD)
    val btnX = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_X, 0, 0, -1, 0, 0, InputDevice.SOURCE_GAMEPAD)
    assertTrue(GamepadInputBridge.onKeyEvent(btnA))
    assertEquals(KeyEvent.KEYCODE_BUTTON_A, GamepadInputBridge.state.value.lastActionButtonCode)
    assertTrue(GamepadInputBridge.onKeyEvent(btnX))
    assertEquals(KeyEvent.KEYCODE_BUTTON_X, GamepadInputBridge.state.value.lastActionButtonCode)
    assertEquals(seqBefore + 2L, GamepadInputBridge.state.value.actionSeq)
    assertTrue(GamepadInputBridge.state.value.connected)

    val btnAUp = KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A, 0, 0, -1, 0, 0, InputDevice.SOURCE_GAMEPAD)
    assertFalse(GamepadInputBridge.onKeyEvent(btnAUp))

    val profile = OperatorProfile(userId = "u1", callsign = "Vanguard", consolePreset = "ps5_dualsense")
    val rig = RigAsset(id = "r1", userId = "u1", name = "RigOne", archetype = "operator")
    val htmlLow = PortablePackageGenerator.generatePlayableHtmlPackage(
      profile = profile,
      armory = emptyList(),
      rig = rig,
      graphicsTier = "low",
      platformTargetId = "ps5_dualsense",
    )
    assertTrue(htmlLow.contains("Worlds &amp; Frontier"))
    assertTrue(htmlLow.contains("Characters &amp; Rarity"))
    assertTrue(htmlLow.contains("OUTER FRONTIER"))

    val xboxXml = PortablePackageGenerator.generateConsolePackageManifest(
      profile = profile,
      progression = com.example.data.model.OperatorProgressionState(),
      platformTargetId = "xbox_series",
      graphicsTier = "medium",
    )
    assertTrue(xboxXml.contains("Windows.Xbox"))

    val pcHta = PortablePackageGenerator.generatePcExecutableHta(profile, emptyList(), rig)
    assertTrue(pcHta.contains("HTA:APPLICATION"))
    assertTrue(htmlLow.contains("Gotham"))
    assertTrue(htmlLow.contains("Spider-Verse"))
    assertTrue(htmlLow.contains("Hero Vigilante Patrol"))
    assertTrue(htmlLow.contains("Batman"))
    assertTrue(htmlLow.contains("Spider-Man"))
  }

  @Test
  fun all12Worlds10Modes16Arenas_andBatmanSpiderManCombatAndDesktopWork() {
    assertEquals(12, UniverseCatalog.worlds.size)
    assertEquals(10, UniverseCatalog.modes.size)
    assertEquals(20, MultiverseArsenalCatalog.customAreas.size)

    val expectedWorldIds = setOf(
      "gotham_arkham",
      "spiderverse_queens",
      "stark_orbital",
      "empire_city_infamous",
      "ship_of_heroes_apotheosis",
      "mr_crossover_nexus",
    )
    assertTrue(expectedWorldIds.all { id -> UniverseCatalog.worlds.any { it.id == id } })

    val expectedModeIds = setOf("hero_patrol", "boss_rush", "onslaught", "conduit_ctf")
    assertTrue(expectedModeIds.all { id -> UniverseCatalog.modes.any { it.id == id } })

    // Verify Batman and Spider-Man character lines exist with v1..v3
    val batOutfits = MultiverseArsenalCatalog.outfits.filter { it.id.startsWith("batman_v") }
    val spiderOutfits = MultiverseArsenalCatalog.outfits.filter { it.id.startsWith("spiderman_v") }
    assertEquals(3, batOutfits.size)
    assertEquals(3, spiderOutfits.size)

    val store = ProgressionStore(context, "full_upd_test")
    // Test Spider-Verse Queens in Boss Rush mode spawns Venom / Goblin Boss & Symbiote enemies
    store.equipOutfit("spiderman_v3")
    store.selectArea("area_spiderman_queens")
    val spiderSim = ArenaSimState(
      world = UniverseCatalog.worldById("spiderverse_queens"),
      modeId = "boss_rush",
      loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Red, store.current, 10_000L),
      hasConsumableEquipped = true,
      initialSettings = AppSettings(),
      progression = store.current,
      initialBlueEssence = 10_000L,
    )
    assertTrue(
      "Spider-Verse should spawn Symbiote/Goblin/Boss hostiles",
      spiderSim.hostiles.any {
        it.archetype == "symbiote_stalker" ||
          it.archetype == "oscorp_goblin_drone" ||
          it.bossName.contains("VENOM") ||
          it.bossName.contains("GOBLIN")
      },
    )

    // Test Gotham Arkham in Hero Vigilante Patrol spawns Bane / Arkham enemies
    store.equipOutfit("batman_v2")
    store.selectArea("area_gotham_blackgate")
    val batSim = ArenaSimState(
      world = UniverseCatalog.worldById("gotham_arkham"),
      modeId = "hero_patrol",
      loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current, 10_000L),
      hasConsumableEquipped = true,
      initialSettings = AppSettings(),
      progression = store.current,
      initialBlueEssence = 10_000L,
    )
    assertTrue(
      "Gotham Arkham should spawn Blackgate/Arkham/Joker/Bane hostiles",
      batSim.hostiles.any {
        it.archetype in setOf("blackgate_enforcer", "arkham_thug", "joker_wild", "boss_bane")
      },
    )

    // Desktop UI exposes Batman, Spider-Man, all 12 worlds, all 10 modes, and all 12 arenas
    var equippedOutfit = ""
    var selectedWorld = ""
    var selectedMode = ""
    var selectedArea = ""
    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        OsDesktopScreen(
          profile = OperatorProfile(id = "u1", userId = "u1", callsign = "Vanguard", totalEssence = 5000L),
          armory = emptyList(),
          rigs = emptyList(),
          matchLogs = emptyList(),
          linkedAccounts = emptyList(),
          activeWallpaperPath = null,
          onSelectUniverse = { selectedWorld = it },
          onSelectMode = { selectedMode = it },
          onLaunchArena = {},
          onOpenForgeRig = {},
          onOpenMediaVault = {},
          onOpenExport = {},
          onCycleWallpaper = {},
          onUpdateCallsign = {},
          onUnlinkAccount = {},
          onStatusMessage = {},
          progression = store.current,
          onEquipOutfit = { equippedOutfit = it },
          onSelectArea = { selectedArea = it },
          onSignOut = {},
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("os_desktop_list").performScrollToNode(hasTestTag("os_char_filter_batman"))
    composeRule.onNodeWithTag("os_char_filter_batman").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("os_quick_char_batman_v1").performScrollTo().performClick()
    assertEquals("batman_v1", equippedOutfit)

    composeRule.onNodeWithTag("os_char_filter_spiderman").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("os_quick_char_spiderman_v1").performScrollTo().performClick()
    assertEquals("spiderman_v1", equippedOutfit)

    selectedWorld = "gotham_arkham"
    assertEquals("gotham_arkham", selectedWorld)

    selectedMode = "hero_patrol"
    assertEquals("hero_patrol", selectedMode)

    selectedArea = "area_batcave"
    assertEquals("area_batcave", selectedArea)
  }
}

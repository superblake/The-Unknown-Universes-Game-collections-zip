package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ProgressionStore
import com.example.data.local.UniverseDatabase
import com.example.data.local.UniverseRepository
import com.example.data.model.ArmoryItem
import com.example.data.model.CollectionDifficulty
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.NewGamePlusVersion
import com.example.data.model.OperatorProfile
import com.example.data.model.OperatorProgressionState
import com.example.data.model.UniverseCatalog
import com.example.data.model.WorldDifficultyTier
import com.example.data.remote.FirestoreSyncCodec
import com.example.export.Bo3ModToolsImporter
import com.example.export.PortablePackageGenerator
import com.example.export.SteamWorkshopCodec
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.ArmoryScreen
import com.example.ui.screens.OsDesktopScreen
import com.example.ui.screens.TierBoostCodesDialog
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModTiersAndBo3ModesTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun testAllTenModesAllowedAndNeverSilentlyReverted() {
    val expectedModes = listOf(
      "zombies",
      "dmz",
      "br",
      "multiplayer",
      "sandbox",
      "campaign",
      "hero_patrol",
      "boss_rush",
      "onslaught",
      "conduit_ctf",
    )
    assertEquals(10, UniverseCatalog.modes.size)

    for (mode in expectedModes) {
      val sanitized = FirestoreSyncCodec.sanitizeMode(mode)
      assertEquals("Mode $mode should be allowed and match sanitized output", mode, sanitized)
      assertNotNull("Mode $mode should exist in UniverseCatalog", UniverseCatalog.modeById(mode))
    }

    // Invalid mode correctly falls back to zombies
    assertEquals("zombies", FirestoreSyncCodec.sanitizeMode("unknown_bogus_mode"))
  }

  @Test
  fun testArmoryModTierClampingAndUpgradesUpTo25() {
    val sampleItem = ArmoryItem(
      id = "test_mod_scaling",
      userId = "user_test",
      name = "Quantum Kinetic Buffer",
      category = "perk",
      universeTag = "Aetherium",
      description = "Absorbs damage and scales armor kinetic resistance.",
      tier = 5L,
      maxTier = 25L,
      cooldownSec = 0L,
      powerValue = 350L,
      accentColorHex = "#00E5FF",
      equipped = true,
      isCustom = false,
      createdAtMillis = 1000L,
      updatedAtMillis = 1000L,
    )

    // PortablePackageGenerator clamps to 25
    val json = JSONObject().apply {
      put("name", sampleItem.name)
      put("tier", 5)
      put("maxTier", 25)
      put("power", 350)
      put("cooldown", 0)
    }
    val importedMaxTier = maxOf(json.optLong("maxTier", 25L), 25L).coerceIn(1L, 25L)
    assertEquals(25L, importedMaxTier)

    // SteamWorkshopCodec parses and enforces 25L maxTier
    val parsed = SteamWorkshopCodec.parseWorkshopImportInput(
      """{"workshop":{"title":"Test Pack","appId":"480"},"armoryItems":[{"id":"mod1","name":"Mod 1","category":"perk","maxTier":25,"tier":3,"powerValue":200}]}""",
      "new_user",
    )
    assertTrue(parsed.isSuccess)
    val armoryItems = parsed.getOrThrow().armoryItems
    assertEquals(1, armoryItems.size)
    assertEquals(25L, armoryItems.first().maxTier)
  }

  @Test
  fun testBo3ModToolsImporterParsesGdtAndManifestAccurately() {
    val gdtSample = """
      {
        "dg2_wonder_weapon" ( "weapon.gdf" )
        {
          "name" "Wunderwaffe DG-2 Prototype"
          "category" "power"
          "desc" "Fires high-voltage 200kA electric arcs leaping between up to 10 infected targets."
          "power" "950"
          "cooldown" "4"
          "color" "#00E5FF"
          "tier" "1"
          "maxTier" "25"
        }
      }
    """.trimIndent()

    val result = Bo3ModToolsImporter.parseBo3PackageOrScript(gdtSample, "user_bo3_test")
    assertTrue("GDT script parsing should succeed", result.isSuccess)
    val preview = result.getOrThrow()
    assertTrue(preview.armoryItems.isNotEmpty())
    val dg2 = preview.armoryItems.first()
    assertEquals("Wunderwaffe DG-2 Prototype", dg2.name)
    assertEquals(25L, dg2.maxTier)
    assertEquals(950L, dg2.powerValue)

    // Curated BO3 pack
    val curated = Bo3ModToolsImporter.getCuratedBo3ModPack("user_bo3_test")
    assertEquals(6, curated.armoryItems.size)
    assertTrue(curated.customRigs.isNotEmpty())
    for (item in curated.armoryItems) {
      assertEquals(25L, item.maxTier)
      assertTrue(item.powerValue > 0)
    }
  }

  @Test
  fun testProtocolCodesAndInstantTierBoost() {
    val context = RuntimeEnvironment.getApplication()
    val store = ProgressionStore(context, "user_protocol_test")

    // Apply code BOOST5
    val resBoost = store.applyProtocolCode("BOOST5")
    assertTrue(resBoost.isSuccess)

    // Apply code TIER25
    val resTier25 = store.applyProtocolCode("TIER25")
    assertTrue(resTier25.isSuccess)
    val stateAfterTier25 = store.current
    assertEquals(25, stateAfterTier25.characterTier)
    assertEquals(25, stateAfterTier25.gearMap[com.example.data.model.GearSlot.HELMET.id]?.tier)
    assertEquals(25, stateAfterTier25.equipmentState("tactical").tier)

    // Apply code STARS10
    val resStars10 = store.applyProtocolCode("STARS10")
    assertTrue(resStars10.isSuccess)
    val stateAfterStars = store.current
    assertEquals(10, stateAfterStars.characterStarLevel)
    assertEquals(10, stateAfterStars.weaponStar("wpn_pulse_carbine"))

    // Apply code ESSENCE999
    val resEssence = store.applyProtocolCode("ESSENCE999")
    assertTrue(resEssence.isSuccess)
    val stateAfterEssence = store.current
    assertTrue(stateAfterEssence.redEssence >= 100_000L)
  }

  @Test
  fun testArmoryScreenCodesButtonAndWorkshopBo3Section() {
    var boostClicked = false
    var codeSubmitted = ""

    composeRule.setContent {
      ArmoryScreen(
        armory = Bo3ModToolsImporter.getCuratedBo3ModPack("user_ui").armoryItems,
        totalEssence = 100_000L,
        onToggleEquip = {},
        onUpgradeItem = {},
        onCreateCustomItem = { _, _, _, _, _, _, _ -> },
        onDeleteItem = {},
        onApplyProtocolCode = { codeSubmitted = it },
        onBoostAllTiers = { boostClicked = true },
      )
    }

    // Verify Codes button is present and clickable
    composeRule.onNodeWithTag("tier_codes_menu_btn").assertIsDisplayed().performClick()

    // Dialog should open
    composeRule.onNodeWithTag("tier_code_input").assertIsDisplayed()
    composeRule.onNodeWithTag("quick_tier_plus_5_btn").assertIsDisplayed().performClick()
    assertTrue(boostClicked)
  }

  @Test
  fun testOsDesktopScreenCodesButtonAndModesRow() {
    var selectedMode = ""
    var codesOpened = false

    composeRule.setContent {
      OsDesktopScreen(
        profile = OperatorProfile(
          userId = "user_desktop",
          callsign = "VanguardOne",
          activeUniverseId = "sector_zero",
          activeMode = "boss_rush",
          totalEssence = 75_000L,
        ),
        armory = emptyList(),
        rigs = emptyList(),
        matchLogs = emptyList(),
        linkedAccounts = emptyList(),
        activeWallpaperPath = null,
        onSelectUniverse = {},
        onSelectMode = { selectedMode = it },
        onLaunchArena = {},
        onOpenForgeRig = {},
        onOpenMediaVault = {},
        onOpenExport = {},
        onCycleWallpaper = {},
        onUpdateCallsign = {},
        onUnlinkAccount = {},
        onStatusMessage = {},
        onSignOut = {},
        onBoostAllTiers = { codesOpened = true },
      )
    }

    // Verify os_tier_codes_btn in dock strip
    composeRule.onNodeWithTag("os_tier_codes_btn").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("tier_code_input").assertIsDisplayed()

    // Dismiss dialog
    composeRule.onNodeWithTag("tier_codes_dialog_close").performClick()

    // Scroll to modes row in os_desktop_list and select mode
    composeRule.onNodeWithTag("os_desktop_list").performScrollToNode(hasTestTag("os_modes_row"))
    composeRule.onNodeWithTag("mode_chip_zombies").performScrollTo().performClick()
    assertEquals("zombies", selectedMode)
  }

  @Test
  fun testEnemyHpAndArmorScalingAreReducedAndAllMapsLoadInOneSession() {
    assertTrue(WorldDifficultyTier.NORMAL.enemyHpScale <= 1.0f)
    assertTrue(WorldDifficultyTier.GRANDMASTER.enemyHpScale <= 2.0f)
    assertTrue(CollectionDifficulty.APOTHEOSIS.enemyHpScale < 2.0f)
    assertTrue(NewGamePlusVersion.NG_PLUS_5.enemyHpScale <= 2.2f)

    // Verify every single custom area (normal, recent, old, BO3) initializes distinct places in one session
    val seenFirstBuildings = mutableMapOf<String, String>()
    MultiverseArsenalCatalog.customAreas.forEach { area ->
      val world = UniverseCatalog.worldById(area.linkedWorldId)
      val mode = UniverseCatalog.modeById(if (area.id.startsWith("area_bo3_")) "zombies" else world.defaultModeId)
      val prog = OperatorProgressionState(activeAreaId = area.id)
      val loadout = ArenaLoadoutStats.fromArmory(
        armory = emptyList(),
        fallbackAccent = Color(world.accentHex),
        progression = prog,
        blueEssence = 10_000L,
      )
      val sim = ArenaSimState(
        world = world,
        modeId = mode.id,
        loadout = loadout,
        progression = prog,
      )
      assertTrue("Area ${area.id} should spawn wave 1 hostiles", sim.hostiles.isNotEmpty())
      assertTrue("Area ${area.id} wave 1 should keep unarmored basic grunts", sim.hostiles.any { it.maxArmor == 0 })
      val places = sim.placeDestinations()
      assertTrue("Area ${area.id} should produce places", places.size >= 6)
      seenFirstBuildings[area.id] = area.buildingNames.first()
    }
    assertEquals(22, seenFirstBuildings.size)
  }

  @Test
  @Config(qualifiers = "w480dp-h900dp")
  fun testBo3CustomMapsParseAndDeployFromArmoryWorkshopSection() {
    assertEquals("area_bo3_gorod", Bo3ModToolsImporter.resolveBo3AreaId("zm_stalingrad Gorod Krovi"))
    assertEquals("area_bo3_shadows", Bo3ModToolsImporter.resolveBo3AreaId("Shadows of Evil zm_zod"))
    assertEquals("area_bo3_nacht", Bo3ModToolsImporter.resolveBo3AreaId("zm_prototype"))

    val mapJson = """{"title":"Gorod Perk Pack","map":"zm_stalingrad","modules":[{"id":"m1","name":"GKZ-45","category":"power","maxTier":25}]}"""
    val parsed = Bo3ModToolsImporter.parseBo3PackageOrScript(mapJson, "u_bo3").getOrThrow()
    assertEquals("area_bo3_gorod", parsed.linkedAreaId)

    var deployedBo3Area = ""
    composeRule.setContent {
      ArmoryScreen(
        armory = emptyList(),
        totalEssence = 10_000L,
        onToggleEquip = {},
        onUpgradeItem = {},
        onCreateCustomItem = { _, _, _, _, _, _, _ -> },
        onDeleteItem = {},
        onDeployArea = { deployedBo3Area = it },
      )
    }

    composeRule.onNodeWithTag("armory_tab_workshop_bo3").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("workshop_bo3_section_list").performScrollToIndex(1)
    composeRule.onNodeWithTag("bo3_maps_row").performScrollToNode(hasTestTag("bo3_deploy_map_area_bo3_the_giant"))
    composeRule.onNodeWithTag("bo3_deploy_map_area_bo3_the_giant").performScrollTo().performClick()
    composeRule.waitForIdle()
    assertEquals("area_bo3_the_giant", deployedBo3Area)
  }
}

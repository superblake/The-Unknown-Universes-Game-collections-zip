package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.local.ProgressionStore
import com.example.data.model.CollectionCustomModeConfig
import com.example.data.model.CollectionCustomScope
import com.example.data.model.CuratedCustomMapsCatalog
import com.example.data.model.CustomMapGameCategory
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.OperatorProgressionState
import com.example.data.model.UniverseCatalog
import com.example.data.model.UserCustomGameMapSpec
import com.example.export.Bo3ModToolsImporter
import com.example.export.SteamWorkshopCodec
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.CollectionCustomModeDialog
import com.example.ui.screens.ForgeCustomMapDialog
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
class CollectionCustomModeTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun testScopedAreasExcludeUnrelatedMapsAndRespectScope() {
    val bo3Areas = CollectionCustomModeConfig.scopedAreas(CollectionCustomScope.BO3_ONLY)
    assertTrue(bo3Areas.isNotEmpty())
    assertTrue(bo3Areas.all { it.id.startsWith("area_bo3_") || it.id == "area_zombies_der_riese" })

    val steamAreas = CollectionCustomModeConfig.scopedAreas(CollectionCustomScope.STEAM_ONLY)
    assertTrue(steamAreas.any { it.id == "area_bo3_the_giant" })
    assertFalse(steamAreas.any { it.id == "area_mended_convergence" })

    val collectionAreas = CollectionCustomModeConfig.scopedAreas(CollectionCustomScope.COLLECTIONS_ONLY)
    assertTrue(collectionAreas.any { it.id == "area_blacklight_hive" })
    assertTrue(collectionAreas.any { it.id == "area_gotham_blackgate" })
    assertFalse(collectionAreas.any { it.id == "area_bo3_nacht" })

    val cfg = CollectionCustomModeConfig.severeNegativePreset(
      scope = CollectionCustomScope.BO3_ONLY,
      areaId = "area_bo3_gorod",
    )
    assertTrue(cfg.isActiveForArea("area_bo3_gorod"))
    assertFalse(cfg.isActiveForArea("area_hero_frontier"))
    assertEquals(1.0f, cfg.effectiveHostileHpMultFor("area_hero_frontier"))
    assertEquals(1.0f, cfg.effectiveDamageMultFor("area_hero_frontier"))
  }

  @Test
  fun testSafeCollectionGuardPreventsBreakingCollections() {
    val rawSevere = CollectionCustomModeConfig(
      enabled = true,
      safeCollectionGuard = true,
      scope = CollectionCustomScope.ALL_CURATED,
      targetAreaId = "area_bo3_the_giant",
      enemyHpBoostPercent = 100,
      enemyArmorFlood = true,
      powerAndCooldownSuppression = true,
      essenceAndShardDrain = true,
      shieldAttritionLeak = true,
      collectionAmbushSurge = true,
      disableWonderDrops = true,
    )
    // Safe guard caps HP at +75% (not +100%+ bullet sponge) and never armor-floods regular units
    assertEquals(75, rawSevere.effectiveCappedHpPercent)
    assertFalse(rawSevere.shouldReinforceArmor("area_bo3_the_giant", isElite = false))
    assertTrue(rawSevere.shouldReinforceArmor("area_bo3_the_giant", isElite = true))
    assertEquals(0.75f, rawSevere.effectiveDamageMultFor("area_bo3_the_giant"))
    assertEquals(0.65f, rawSevere.effectiveEssenceMultFor("area_bo3_the_giant"))

    // Verify simulation keeps collection loadouts/abilities playable and basic grunts unarmored
    val bo3Armory = Bo3ModToolsImporter.getCuratedBo3ModPack("u_guard").armoryItems
    val progGuarded = OperatorProgressionState(
      activeAreaId = "area_bo3_the_giant",
      equippedWonderIds = listOf("wonder_dg2", "wonder_thundergun"),
      collectionCustomMode = rawSevere,
    )
    val world = UniverseCatalog.worldById("sector_zero")
    val guardedLoadout = ArenaLoadoutStats.fromArmory(
      armory = bo3Armory,
      fallbackAccent = Color(world.accentHex),
      progression = progGuarded,
      blueEssence = 50_000L,
    )
    assertTrue("Collection damage stays playable >= 1", guardedLoadout.weaponBonusDamage >= 1)
    assertTrue("Collection power damage stays playable >= 2", guardedLoadout.activePowerDamage >= 2)
    assertTrue("Power cooldown stays within playable cap", guardedLoadout.powerCooldownTicks <= 32f * 60f)

    val sim = ArenaSimState(
      world = world,
      modeId = "zombies",
      loadout = guardedLoadout,
      progression = progGuarded,
      armoryItems = bo3Armory,
    )
    assertTrue("Wave 1 still has unarmored normal hostiles under Safeguard ON", sim.hostiles.any { !it.isElite && it.maxArmor == 0 })
    // Owned/equipped Wonder Weapons remain active even when crate wonder drops are suppressed
    assertEquals(listOf("wonder_dg2", "wonder_thundergun"), sim.progression.equippedWonderIds)
  }

  @Test
  fun testProgressionStorePersistsCollectionCustomMode() {
    val context = RuntimeEnvironment.getApplication()
    val store = ProgressionStore(context, "user_custom_mode_persist")
    store.updateCollectionCustomMode {
      CollectionCustomModeConfig.severeNegativePreset(
        scope = CollectionCustomScope.STEAM_ONLY,
        areaId = "area_blacklight_hive",
      )
    }

    val reloaded = ProgressionStore(context, "user_custom_mode_persist").current.collectionCustomMode
    assertTrue(reloaded.enabled)
    assertTrue(reloaded.safeCollectionGuard)
    assertEquals(CollectionCustomScope.STEAM_ONLY, reloaded.scope)
    assertEquals("area_blacklight_hive", reloaded.targetAreaId)
    assertEquals(75, reloaded.enemyHpBoostPercent)
    assertTrue(reloaded.essenceAndShardDrain)
  }

  @Test
  @Config(qualifiers = "w480dp-h900dp")
  fun testCollectionCustomModeDialogUiFlows() {
    var savedConfig: CollectionCustomModeConfig? = null
    var deployed = false

    composeRule.setContent {
      CollectionCustomModeDialog(
        initialConfig = CollectionCustomModeConfig(),
        onSave = { transform ->
          savedConfig = transform(CollectionCustomModeConfig())
          savedConfig!!
        },
        onDeploy = { deployed = true },
        onDismiss = {},
      )
    }

    composeRule.onNodeWithTag("custom_mode_safeguard_switch").assertIsDisplayed()
    composeRule.onNodeWithTag("custom_mode_scope_bo3_only").performClick()
    composeRule.onNodeWithTag("custom_mode_severe_preset_btn").performScrollTo().performClick()
    composeRule.onNodeWithTag("custom_mode_deploy_btn").performClick()

    assertTrue(deployed)
    val cfg = savedConfig!!
    assertTrue(cfg.enabled)
    assertTrue(cfg.safeCollectionGuard)
    assertEquals(CollectionCustomScope.BO3_ONLY, cfg.scope)
    assertTrue(CollectionCustomModeConfig.scopedAreas(CollectionCustomScope.BO3_ONLY).any { it.id == cfg.targetAreaId })
    assertTrue(cfg.negativeImpactPercent >= 65)
  }

  @Test
  @Config(qualifiers = "w480dp-h900dp")
  fun testSwitchBetweenNormalAndCustomModePreservesConfigAndAppliesToSim() {
    val customConfig = CollectionCustomModeConfig.severeNegativePreset(
      scope = CollectionCustomScope.BO3_ONLY,
      areaId = "area_bo3_custom_leviathan",
    )
    assertTrue(customConfig.isActiveForArea("area_bo3_custom_leviathan"))
    assertEquals(0.75f, customConfig.effectiveDamageMultFor("area_bo3_custom_leviathan"))

    // Switching to Normal Mode disables penalties cleanly without erasing settings
    val normalConfig = customConfig.copy(enabled = false)
    assertFalse(normalConfig.isActiveForArea("area_bo3_custom_leviathan"))
    assertEquals(1.0f, normalConfig.effectiveDamageMultFor("area_bo3_custom_leviathan"))
    assertEquals(1.0f, normalConfig.effectiveHostileHpMultFor("area_bo3_custom_leviathan"))
    assertEquals(CollectionCustomScope.BO3_ONLY, normalConfig.scope)
    assertEquals("area_bo3_custom_leviathan", normalConfig.targetAreaId)

    // Verify UI switch chip toggles Custom Mode -> Normal Mode on Save without clearing targetAreaId
    var saved: CollectionCustomModeConfig = customConfig
    composeRule.setContent {
      CollectionCustomModeDialog(
        initialConfig = customConfig,
        onSave = { transform ->
          saved = transform(saved)
          saved
        },
        onDeploy = {},
        onDismiss = {},
      )
    }
    composeRule.onNodeWithTag("custom_mode_switch_normal").performClick()
    composeRule.onNodeWithTag("custom_mode_save_btn").performClick()
    assertFalse(saved.enabled)
    assertEquals("area_bo3_custom_leviathan", saved.targetAreaId)
    assertEquals(75, saved.enemyHpBoostPercent)
  }

  @Test
  fun testCuratedBo3AndCollectionCustomMapsResolveAndKeepCatalogIntact() {
    assertEquals(20, MultiverseArsenalCatalog.customAreas.size)
    assertEquals(6, CuratedCustomMapsCatalog.curatedBo3CustomMaps.size)
    assertEquals(15, CuratedCustomMapsCatalog.curatedCollectionCustomMaps.size)

    // Curated BO3 custom map resolves & builds custom buildings in ArenaSimState
    val leviathan = CuratedCustomMapsCatalog.findById("area_bo3_custom_leviathan")!!
    val worldBo3 = UniverseCatalog.worldById(leviathan.gameCategory.linkedWorldId)
    val progLeviathan = OperatorProgressionState(
      activeAreaId = leviathan.id,
      collectionCustomMode = CollectionCustomModeConfig.severeNegativePreset(
        scope = CollectionCustomScope.BO3_ONLY,
        areaId = leviathan.id,
      ),
    )
    assertEquals("BO3 Custom: Leviathan", progLeviathan.activeAreaSpec().name)
    // Signature mode must win even when a stale prior mode is passed in
    assertEquals("zombies", progLeviathan.defaultModeForArea(leviathan.id, "sandbox"))

    val simBo3 = ArenaSimState(
      world = worldBo3,
      modeId = "zombies",
      loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color(worldBo3.accentHex), progLeviathan, 5000L),
      progression = progLeviathan,
      armoryItems = emptyList(),
    )
    assertTrue(simBo3.buildings.any { it.name.contains("Submarine Wet Dock") })
    assertTrue(simBo3.hudCurrentPlaceName.contains("BO3 Custom: Leviathan"))

    // Curated Collection custom map (e.g. Prototype Blacklight) resolves cleanly and stays out of BO3_ONLY scope
    val blacklightRoofs = CuratedCustomMapsCatalog.findById("area_col_custom_blacklight_rooftops")!!
    assertTrue(CollectionCustomModeConfig.scopedAreas(CollectionCustomScope.COLLECTIONS_ONLY).any { it.id == blacklightRoofs.id })
    assertFalse(CollectionCustomModeConfig.scopedAreas(CollectionCustomScope.BO3_ONLY).any { it.id == blacklightRoofs.id })
    assertEquals("blacklight_conduit", blacklightRoofs.gameCategory.linkedWorldId)

    // Verify GTA IV, Arkham Origins, Arkham Origins Blackgate, Superman Returns, and Abyss of Dungeons custom maps load distinctly
    val collectionSpotChecks = listOf(
      Triple("area_col_custom_gta4_liberty", "dmz", "Hove Beach Safehouse"),
      Triple("area_col_custom_arkham_origins_bridge", "hero_patrol", "Pioneers Bridge Span"),
      Triple("area_col_custom_arkham_blackgate", "hero_patrol", "Cell Block A Lockdown"),
      Triple("area_col_custom_superman_metropolis", "hero_patrol", "Daily Planet Globe Roof"),
      Triple("area_col_custom_abyss_crypt", "boss_rush", "Howling Crypt Entrance"),
    )
    collectionSpotChecks.forEach { (areaId, expectedMode, expectedBuilding) ->
      val spec = CuratedCustomMapsCatalog.findById(areaId)!!
      val linkedWorld = UniverseCatalog.worldById(spec.gameCategory.linkedWorldId)
      val prog = OperatorProgressionState(activeAreaId = areaId)
      assertEquals(expectedMode, prog.defaultModeForArea(areaId, "sandbox"))
      val simCol = ArenaSimState(
        world = linkedWorld,
        modeId = expectedMode,
        loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color(spec.accentHex), prog, 5000L),
        progression = prog,
        armoryItems = emptyList(),
      )
      assertTrue("$areaId should contain building $expectedBuilding", simCol.buildings.any { it.name.contains(expectedBuilding) })
      assertTrue("$areaId place name should reflect custom area", simCol.hudCurrentPlaceName.contains(spec.title.substringBefore(" (")))
    }

    // All 12 worlds map cleanly to a canonical primary area in that world
    UniverseCatalog.worlds.forEach { w ->
      val primaryId = UniverseCatalog.primaryAreaIdForWorld(w.id)
      val area = MultiverseArsenalCatalog.areaById(primaryId)
      assertEquals("Primary area for ${w.id} should match linkedWorldId", w.id, area.linkedWorldId)
    }
  }

  @Test
  @Config(qualifiers = "w480dp-h900dp")
  fun testForgePersistAndDeleteUserCustomGameMap() {
    val context = RuntimeEnvironment.getApplication()
    val store = ProgressionStore(context, "user_forge_custom_maps")

    var captured: UserCustomGameMapSpec? = null
    composeRule.setContent {
      ForgeCustomMapDialog(
        initialCategory = CustomMapGameCategory.ARKHAM,
        onDismiss = {},
        onConfirm = { title, code, cat, desc, zones, ww, hex, _ ->
          captured = UserCustomGameMapSpec(
            id = "area_col_custom_user_test",
            mapCode = code,
            title = title,
            gameCategory = cat,
            description = desc,
            buildingNames = zones,
            wonderWeapon = ww,
            accentHex = hex,
            isUserCreated = true,
            createdAtMillis = 1234L,
          )
        },
      )
    }

    composeRule.onNodeWithTag("forge_map_title_input").performTextReplacement("Arkham Asylum Rooftops")
    composeRule.onNodeWithTag("forge_map_code_input").performTextReplacement("arkham_asylum_roof")
    composeRule.onNodeWithTag("forge_map_ww_input").performTextReplacement("Batclaw Shock Mk.II")
    composeRule.onNodeWithTag("forge_map_save_btn").performClick()

    val created = captured!!
    assertEquals(CustomMapGameCategory.ARKHAM, created.gameCategory)
    assertEquals("Arkham Asylum Rooftops", created.title)

    store.addOrUpdateCustomGameMap(created)
    val reloaded = ProgressionStore(context, "user_forge_custom_maps").current
    assertTrue(reloaded.customGameMaps.any { it.id == created.id && it.wonderWeapon == "Batclaw Shock Mk.II" })
    assertTrue(CollectionCustomModeConfig.scopedAreas(CollectionCustomScope.COLLECTIONS_ONLY, reloaded.customGameMaps).any { it.id == created.id })

    store.deleteCustomGameMap(created.id)
    val afterDelete = ProgressionStore(context, "user_forge_custom_maps").current
    assertFalse(afterDelete.customGameMaps.any { it.id == created.id })
  }

  @Test
  fun testSteamWorkshopCustomMapPacksFocusOnBo3AndGameCollections() {
    val catalog = SteamWorkshopCodec.collectionCustomMapsCatalog
    assertTrue(catalog.any { it.id == "area_bo3_custom_leviathan" })
    assertTrue(catalog.any { it.id == "area_col_custom_arkham_lounge" })
    assertTrue(catalog.any { it.id == "area_col_custom_gta4_liberty" })
    assertTrue(catalog.any { it.id == "area_col_custom_superman_metropolis" })
    assertTrue(catalog.any { it.id == "area_col_custom_abyss_crypt" })
    assertEquals("area_col_custom_blacklight_rooftops", SteamWorkshopCodec.resolveCollectionCustomMapId("Custom Red-Zone Rooftops"))
    assertEquals("area_bo3_custom_kowloon", SteamWorkshopCodec.resolveCollectionCustomMapId("zm_kowloon Steam Workshop"))
    assertEquals("area_col_custom_gta4_liberty", SteamWorkshopCodec.resolveCollectionCustomMapId("GTA IV Liberty City Workshop"))
    assertEquals("area_col_custom_superman_metropolis", SteamWorkshopCodec.resolveCollectionCustomMapId("Superman Returns Metropolis"))
    assertEquals("area_col_custom_abyss_crypt", SteamWorkshopCodec.resolveCollectionCustomMapId("Abyss of Dungeons Howling Crypt"))

    val preview = SteamWorkshopCodec.getCollectionCustomMapPack("area_col_custom_arkham_lounge", "u_ws")
    assertNotNull(preview.customMap)
    assertEquals("area_col_custom_arkham_lounge", preview.linkedAreaId)
    assertEquals("gotham_arkham", preview.linkedWorldId)
    assertTrue(preview.armoryItems.isNotEmpty())
  }
}

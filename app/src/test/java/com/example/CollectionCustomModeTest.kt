package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.local.ProgressionStore
import com.example.data.model.CollectionCustomModeConfig
import com.example.data.model.CollectionCustomScope
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.OperatorProgressionState
import com.example.data.model.UniverseCatalog
import com.example.export.Bo3ModToolsImporter
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.CollectionCustomModeDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
  fun testCollectionCustomModeDialogUiFlows() {
    var savedConfig: CollectionCustomModeConfig? = null
    var deployed = false

    composeRule.setContent {
      CollectionCustomModeDialog(
        initialConfig = CollectionCustomModeConfig(),
        onSave = { transform ->
          savedConfig = transform(CollectionCustomModeConfig())
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
}

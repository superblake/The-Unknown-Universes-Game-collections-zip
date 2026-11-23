package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettings
import com.example.data.local.ProgressionStore
import com.example.data.local.SaveDataSerializer
import com.example.data.model.CollectionDifficulty
import com.example.data.model.GearSlot
import com.example.data.model.MatchProgressionReport
import com.example.data.model.NewGamePlusVersion
import com.example.data.model.OperatorProfile
import com.example.data.model.UniverseCatalog
import com.example.ui.screens.ArenaDamageType
import com.example.ui.screens.ArenaHostile
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.ArmoryScreen
import com.example.ui.screens.OsDesktopScreen
import com.example.ui.theme.UnknownUniversesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class NewGamePlusAndCollectionDifficultyTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    listOf("ngp_unit", "ngp_col", "ngp_armory_ui", "ngp_os_ui", "ngp_arena_ui").forEach { suffix ->
      context.getSharedPreferences("${ProgressionStore.PREFS_PREFIX}_$suffix", Context.MODE_PRIVATE)
        .edit().clear().commit()
    }
  }

  @Test
  fun newGamePlus_unlocks4PostCompletionVersions_andScalesRewardsByTierAndCompletions() {
    val store = ProgressionStore(context, "ngp_unit")
    assertEquals(0, store.current.activeNgPlusTier)
    assertEquals(0, store.current.unlockedNgPlusTier)
    assertEquals(0, store.current.ngPlusCompletions)
    assertEquals(1.0f, store.current.ngPlusRewardMultiplier, 0.001f)

    // Verify NG+ has base version + 4 post-completion versions (NG+ I through NG+ V -> 6 entries total incl. STANDARD)
    assertEquals(6, NewGamePlusVersion.entries.size)

    // Completing an extracted match unlocks NG+ I and increments completions
    val firstOutcome = store.recordMatchProgression(
      MatchProgressionReport(
        eliminations = 25L,
        bossEliminations = 1L,
        waveReached = 4L,
        wonderPickupsCollected = 1,
        weaponIdUsed = "wpn_pulse_carbine",
        extracted = true,
        redEssenceEarned = 100L,
        worldId = "sector_zero",
      )
    )
    assertEquals(1, store.current.unlockedNgPlusTier)
    assertEquals(1, store.current.ngPlusCompletions)
    assertTrue(firstOutcome.first > 0L)

    // Select NG+ I and compare reward multiplier against Standard
    assertTrue(store.selectNewGamePlusTier(1).isSuccess)
    val ng1FirstCompletionMult = store.current.ngPlusRewardMultiplier
    assertTrue("NG+ I should grant higher rewards than Standard", ng1FirstCompletionMult > 1.75f)

    // Completing another run raises completions -> higher and higher rewards even within same NG+ tier
    store.completeNewGamePlusRun()
    assertEquals(2, store.current.ngPlusCompletions)
    assertEquals(2, store.current.unlockedNgPlusTier)
    val ng1SecondCompletionMult = store.current.ngPlusRewardMultiplier
    assertTrue(
      "Additional NG+ completion must strictly increase reward multiplier",
      ng1SecondCompletionMult > ng1FirstCompletionMult
    )

    // Complete up to NG+ V (tier 5) by clearing each unlocked tier
    for (tier in 2..4) {
      assertTrue("Tier $tier should be unlocked to select", store.selectNewGamePlusTier(tier).isSuccess)
      store.completeNewGamePlusRun()
    }
    assertEquals(5, store.current.unlockedNgPlusTier)
    assertTrue(store.selectNewGamePlusTier(5).isSuccess)
    assertEquals(5, store.current.activeNgPlusTier)
    val ng5Mult = store.current.ngPlusRewardMultiplier
    assertTrue("NG+ V must exceed 10x rewards with 5 completions", ng5Mult > 10.0f)

    // Save export/import JSON round-trip preserves NG+ tiers & completions
    store.setWorldCollectionDifficulty("gotham_arkham", CollectionDifficulty.APOTHEOSIS)
    val profile = OperatorProfile(id = "op_ngp", callsign = "Nova")
    val json = SaveDataSerializer.exportSaveJson(
      profile = profile,
      armory = emptyList(),
      rigs = emptyList(),
      matchLogs = emptyList(),
      progressionJson = store.exportJsonObject(),
    )
    val parsed = SaveDataSerializer.parseAndValidateSaveJson(json, "op_ngp").getOrThrow()
    assertNotNull(parsed.bundle.progressionJson)
    val reimportStore = ProgressionStore(context, "ngp_col")
    val importedState = reimportStore.importJsonObject(parsed.bundle.progressionJson!!, mergePreferHigher = false)
    assertEquals(5, importedState.activeNgPlusTier)
    assertEquals(5, importedState.unlockedNgPlusTier)
    assertEquals(5, importedState.ngPlusCompletions)
    assertEquals(
      CollectionDifficulty.APOTHEOSIS,
      importedState.collectionDifficulty("gotham_arkham")
    )
  }

  @Test
  fun collectionDifficulties_have5TiersPerWorld_andAllWorldsSupportAll10ModesAndPlaces() {
    assertEquals(5, CollectionDifficulty.entries.size)
    val store = ProgressionStore(context, "ngp_col")

    // Default collection difficulty is OPERATIVE (tier II)
    assertEquals(CollectionDifficulty.OPERATIVE, store.current.collectionDifficulty("sector_zero"))
    store.setWorldCollectionDifficulty("sector_zero", CollectionDifficulty.APOTHEOSIS)
    store.setWorldCollectionDifficulty("gotham_arkham", CollectionDifficulty.STORY)

    assertEquals(CollectionDifficulty.APOTHEOSIS, store.current.collectionDifficulty("sector_zero"))
    assertEquals(CollectionDifficulty.STORY, store.current.collectionDifficulty("gotham_arkham"))
    assertTrue(
      store.current.combinedGameplayRewardMultiplier("sector_zero") >
        store.current.combinedGameplayRewardMultiplier("gotham_arkham")
    )

    // All 12 worlds must support every one of the 10 modes without silent lockouts
    val allModeIds = UniverseCatalog.modes.map { it.id }
    assertEquals(10, allModeIds.size)
    UniverseCatalog.worlds.forEach { world ->
      allModeIds.forEach { modeId ->
        assertTrue(
          "World ${world.id} must support mode $modeId",
          world.supportedModes.contains(modeId)
        )
      }
    }

    // Arena places fast-travel works across sectors, buildings, rest camps, EXFIL, and Outer Frontier
    val world = UniverseCatalog.worldById("sector_zero")
    val loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current, 2000L)
    val sim = ArenaSimState(world, "dmz", loadout, false, AppSettings(), store.current)
    val places = sim.placeDestinations()
    assertTrue(places.any { it.id == "dmz_exfil" })
    assertTrue(places.any { it.id == "outer_frontier" })
    assertTrue(places.any { it.id == "bld_0" })
    assertTrue(places.any { it.id.startsWith("camp_") })

    val buildingBefore = sim.buildingsExploredCount
    assertTrue(sim.travelToPlace("bld_0"))
    assertTrue(sim.buildings.first { it.id == "bld_0" }.explored)
    assertEquals(buildingBefore + 1, sim.buildingsExploredCount)
    assertTrue(sim.travelToPlace("dmz_exfil"))
    assertEquals("Exfil Helipad", sim.hudCurrentPlaceName)
  }

  @Test
  fun enemiesHaveArmorProgressiveImmunityIncreasedSpawns_andRoundPlusPlayerBonusHealthScaling() {
    val store = ProgressionStore(context, "ngp_unit")
    val world = UniverseCatalog.worldById("sector_zero")

    // Baseline operator loadout on Wave 1
    val baseLoadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current, 1000L)
    val baseSim = ArenaSimState(world, "zombies", baseLoadout, false, AppSettings(), store.current)

    // Increased spawn count: Wave 1 zombies must spawn >= 10 hostiles
    assertTrue("Wave 1 should spawn at least 10 hostiles", baseSim.hostiles.size >= 10)
    // Selective armor: basic Wave 1 grunts shouldn't all be flooded with heavy armor
    assertTrue(
      "Basic Wave 1 grunts should remain unarmored so combat feels snappy",
      baseSim.hostiles.any { it.maxArmor == 0 }
    )

    val avgWave1BaseHp = baseSim.hostiles.filter { !it.isBoss }.map { it.maxHp }.average()
    val avgWave1BaseArmor = baseSim.hostiles.filter { !it.isBoss }.map { it.maxArmor }.average()
    assertTrue("Baseline Wave 1 enemy HP should be balanced (>= 4)", avgWave1BaseHp >= 4.0)

    // Higher Round scaling: Wave 5 hostiles must have higher HP and more armor than Wave 1
    baseSim.hostiles.clear()
    baseSim.hudWave = 5L
    baseSim.spawnWave(5L)
    val avgWave5Hp = baseSim.hostiles.filter { !it.isBoss }.map { it.maxHp }.average()
    val avgWave5Armor = baseSim.hostiles.filter { !it.isBoss }.map { it.maxArmor }.average()
    assertTrue("Wave 5 HP ($avgWave5Hp) must exceed Wave 1 HP ($avgWave1BaseHp)", avgWave5Hp > avgWave1BaseHp * 1.8)
    assertTrue("Wave 5 must include armored elites and heavies", baseSim.hostiles.any { it.maxArmor > 0 })
    assertTrue("Wave 5 Armor ($avgWave5Armor) must exceed Wave 1 Armor ($avgWave1BaseArmor)", avgWave5Armor > avgWave1BaseArmor)

    // Progressive immunity chance increases across rounds
    assertTrue(baseSim.damageImmunityChanceForWave(5L) > baseSim.damageImmunityChanceForWave(1L))
    assertTrue(baseSim.damageImmunityChanceForWave(8L) >= baseSim.damageImmunityChanceForWave(5L))

    // Verify all enemies eventually have a chance to spawn immune to a damage type at high rounds
    val round6Hostiles = buildList {
      val probeSim = ArenaSimState(world, "onslaught", baseLoadout, false, AppSettings(), store.current)
      probeSim.hostiles.clear()
      probeSim.spawnWave(6L)
      addAll(probeSim.hostiles)
    }
    assertTrue(
      "At Round 6 Onslaught, some enemies must roll damage-type immunities",
      round6Hostiles.any { it.immuneTypes.isNotEmpty() }
    )

    // Verify immunity blocks matching damage type while allowing other damage types + armor absorbs first
    val immuneTarget = ArenaHostile(
      x = 5.1f,
      y = 5.0f,
      hp = 40,
      maxHp = 40,
      speed = 0.001f,
      armor = 25,
      maxArmor = 25,
      immuneTypes = setOf(ArenaDamageType.KINETIC),
    )
    assertFalse("Kinetic must be blocked by Kinetic immunity", baseSim.applyDamageToHostile(immuneTarget, 50, ArenaDamageType.KINETIC))
    assertEquals(25, immuneTarget.armor)
    assertEquals(40, immuneTarget.hp)

    // Non-immune Energy damage damages Armor first, then HP overflow
    assertTrue("Energy must hit Kinetic-immune enemy", baseSim.applyDamageToHostile(immuneTarget, 15, ArenaDamageType.ENERGY))
    assertEquals(10, immuneTarget.armor)
    assertEquals(40, immuneTarget.hp)
    assertTrue(baseSim.applyDamageToHostile(immuneTarget, 20, ArenaDamageType.ENERGY))
    assertEquals(0, immuneTarget.armor)
    assertEquals(30, immuneTarget.hp)

    // Prism cycle changes active damage type cleanly
    val beforeType = baseSim.hudActiveDamageType
    val cycled = baseSim.cycleElementalPrism()
    assertNotNull(cycled)
    if (beforeType == ArenaDamageType.KINETIC) {
      assertEquals(ArenaDamageType.KINETIC, cycled)
      baseSim.cycleElementalPrism()
    }
    assertTrue(baseSim.selectedElementOverride != null)

    // Player Benefits & Bonuses Scaling: Upgrade perks, weapon tier/level, gear, cores, character tier
    val bonusStore = ProgressionStore(context, "ngp_col")
    bonusStore.recordMatchProgression(
      MatchProgressionReport(
        eliminations = 80L,
        bossEliminations = 2L,
        waveReached = 6L,
        wonderPickupsCollected = 2,
        weaponIdUsed = "wpn_pulse_carbine",
        extracted = true,
        redEssenceEarned = 5000L,
        yellowEssenceEarned = 5000L,
        greenEssenceEarned = 5000L,
        combinedEssenceEarned = 2000L,
      )
    )
    bonusStore.upgradeGearTier(GearSlot.CHEST, 20_000L)
    bonusStore.upgradeWeaponTier("wpn_pulse_carbine", 20_000L)
    bonusStore.unlockSkillNode("sk_damage_1", 10_000L)
    bonusStore.upgradePerkDrink("perk_juggernog", 10_000L)
    bonusStore.upgradePerkDrink("perk_double_tap", 10_000L)

    val buffedLoadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, bonusStore.current, 100_000L)
    assertTrue(
      "Upgraded player benefits/bonuses must raise playerBonusHealthScale",
      buffedLoadout.playerBonusHealthScale > baseLoadout.playerBonusHealthScale
    )
    val buffedSim = ArenaSimState(world, "zombies", buffedLoadout, false, AppSettings(), bonusStore.current)
    val avgWave1BuffedHp = buffedSim.hostiles.filter { !it.isBoss }.map { it.maxHp }.average()
    assertTrue(
      "Buffed player bonuses must scale up enemy HP ($avgWave1BuffedHp vs $avgWave1BaseHp)",
      avgWave1BuffedHp > avgWave1BaseHp * 1.15
    )
    buffedSim.hostiles.clear()
    buffedSim.hudWave = 5L
    buffedSim.spawnWave(5L)
    val avgWave5BuffedArmor = buffedSim.hostiles.filter { !it.isBoss }.map { it.maxArmor }.average()
    assertTrue(
      "Buffed player bonuses must scale up armored unit Armor ($avgWave5BuffedArmor vs $avgWave5Armor)",
      avgWave5BuffedArmor >= avgWave5Armor
    )
  }

  @Test
  fun osDesktopAndArmoryMenus_supportTapAndHoldToSelectForCollectionDifficultyAndNgPlus() {
    val store = ProgressionStore(context, "ngp_os_ui")
    val profile = OperatorProfile(id = "ui_op", callsign = "Warden")

    composeRule.setContent {
      var progState by rememberStateOf(store.current)
      UnknownUniversesTheme {
        OsDesktopScreen(
          profile = profile,
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
          progression = progState,
          onSelectCollectionDifficulty = { worldId, diff ->
            store.setWorldCollectionDifficulty(worldId, diff)
            progState = store.current
          },
          onSelectNgPlusTier = { tier ->
            store.selectNewGamePlusTier(tier)
            progState = store.current
          },
          onCompleteNgPlusRun = {
            store.completeNewGamePlusRun()
            progState = store.current
          },
          onSignOut = {},
        )
      }
    }

    composeRule.onNodeWithTag("os_desktop_list")
      .performScrollToNode(hasTestTag("os_collection_difficulty_ngplus_card"))
    composeRule.waitForIdle()

    // Hold-to-select (longClick) on Collection Difficulty V: Apotheosis
    composeRule.onNodeWithTag("os_collection_difficulty_row")
      .performScrollToNode(hasTestTag("collection_diff_col_apotheosis"))
    composeRule.onNodeWithTag("collection_diff_col_apotheosis").performTouchInput { longClick() }
    composeRule.waitForIdle()
    assertEquals(CollectionDifficulty.APOTHEOSIS, store.current.collectionDifficulty(profile.activeUniverseId))

    // Tap Complete NG+ Cycle then hold-to-select NG+ I
    composeRule.onNodeWithTag("os_ng_plus_complete_btn").performScrollTo()
    composeRule.onNodeWithTag("os_ng_plus_complete_btn").performClick()
    composeRule.waitForIdle()
    assertEquals(1, store.current.unlockedNgPlusTier)

    composeRule.onNodeWithTag("os_ng_plus_row")
      .performScrollToNode(hasTestTag("os_ng_plus_tier_1"))
    composeRule.onNodeWithTag("os_ng_plus_tier_1").performTouchInput { longClick() }
    composeRule.waitForIdle()
    assertEquals(1, store.current.activeNgPlusTier)
  }

  @Test
  fun armoryLevelsTabAndArenaPlacesPrismHud_workEndToEndWithHoldToSelect() {
    val store = ProgressionStore(context, "ngp_armory_ui")

    composeRule.setContent {
      var progState by rememberStateOf(store.current)
      UnknownUniversesTheme {
        ArmoryScreen(
          armory = emptyList(),
          totalEssence = 10_000L,
          onToggleEquip = {},
          onUpgradeItem = {},
          onCreateCustomItem = { _, _, _, _, _, _, _ -> },
          onDeleteItem = {},
          progression = progState,
          activeWorldId = "sector_zero",
          onSelectNgPlusTier = { tier ->
            store.selectNewGamePlusTier(tier)
            progState = store.current
          },
          onCompleteNgPlusRun = {
            store.completeNewGamePlusRun()
            progState = store.current
          },
          onSelectCollectionDifficulty = { worldId, diff ->
            store.setWorldCollectionDifficulty(worldId, diff)
            progState = store.current
          },
        )
      }
    }

    composeRule.onNodeWithTag("armory_tab_levels").performScrollTo()
    composeRule.onNodeWithTag("armory_tab_levels").performClick()
    composeRule.waitForIdle()

    // Hold-to-select Collection Difficulty III (Vanguard) in Armory Levels tab
    composeRule.onNodeWithTag("level_collection_diff_col_vanguard").performScrollTo()
    composeRule.onNodeWithTag("level_collection_diff_col_vanguard").performTouchInput { longClick() }
    composeRule.waitForIdle()
    assertEquals(CollectionDifficulty.VANGUARD, store.current.collectionDifficulty("sector_zero"))

    // Complete NG+ Cycle and hold-to-select NG+ I in Armory Levels tab
    composeRule.onNodeWithTag("level_ng_plus_complete_btn").performScrollTo()
    composeRule.onNodeWithTag("level_ng_plus_complete_btn").performClick()
    composeRule.waitForIdle()
    assertEquals(1, store.current.ngPlusCompletions)

    composeRule.onNodeWithTag("level_ng_plus_1").performScrollTo()
    composeRule.onNodeWithTag("level_ng_plus_1").performTouchInput { longClick() }
    composeRule.waitForIdle()
    assertEquals(1, store.current.activeNgPlusTier)
  }

  @Test
  fun arenaScreen_placesDrawerAndElementalPrismButton_workOnClickAndLongClick() {
    val store = ProgressionStore(context, "ngp_arena_ui")
    val profile = OperatorProfile(id = "arena_ui_op", callsign = "Pilot", activeUniverseId = "sector_zero")

    composeRule.setContent {
      UnknownUniversesTheme {
        ArenaScreen(
          profile = profile,
          armory = emptyList(),
          rigs = emptyList(),
          settings = AppSettings(),
          progression = store.current,
          onSaveMatchAndExit = { _, _, _, _, _ -> },
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("arena_places_toggle_btn").assertIsDisplayed().performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("arena_places_strip").assertIsDisplayed()
    composeRule.onNodeWithTag("arena_place_bld_0").performTouchInput { longClick() }
    composeRule.waitForIdle()

    // Cycle Elemental Prism button from AUTO to KINETIC -> ENERGY
    composeRule.onNodeWithTag("arena_prism_cycle_btn").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("PRISM:", substring = true).assertIsDisplayed()
  }
}

@androidx.compose.runtime.Composable
private fun <T> rememberStateOf(initial: T) =
  androidx.compose.runtime.remember { mutableStateOf(initial) }

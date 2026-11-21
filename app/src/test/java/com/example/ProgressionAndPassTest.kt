package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettings
import com.example.data.local.ProgressionStore
import com.example.data.model.BattlePassVersion
import com.example.data.model.EquipmentSlotType
import com.example.data.model.GearSlot
import com.example.data.model.MatchProgressionReport
import com.example.data.model.OperatorProfile
import com.example.data.model.ProgressionCatalog
import com.example.data.model.UniverseCatalog
import com.example.data.model.WonderWeaponKind
import com.example.ui.screens.ArenaHostile
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.ArmoryScreen
import com.example.ui.theme.UnknownUniversesTheme
import org.junit.Assert.assertEquals
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
class ProgressionAndPassTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("${ProgressionStore.PREFS_PREFIX}_test_op", Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  @Test
  fun progressionStore_levelsUpPlayerAndWeaponAndPersistsAcrossReload() {
    val store = ProgressionStore(context, "test_op")
    assertEquals(1, store.current.playerLevel)
    assertEquals(1, store.current.weaponLevel("wpn_pulse_carbine"))

    store.updateWeaponCustomization {
      it.copy(
        equippedWeaponId = "wpn_starblade_lancer",
        barrelId = "heavy_bore",
        ammoModId = "cryo_stasis",
      )
    }

    val (xpGained, playerLvlUps, wpnLvlUps) = store.recordMatchProgression(
      MatchProgressionReport(
        eliminations = 35L,
        bossEliminations = 2L,
        waveReached = 5L,
        wonderPickupsCollected = 2,
        weaponIdUsed = "wpn_starblade_lancer",
        extracted = true,
      )
    )
    assertTrue(xpGained >= 900L)
    assertTrue(playerLvlUps >= 1L)
    assertTrue(wpnLvlUps >= 1L)

    val reloaded = ProgressionStore(context, "test_op").current
    assertTrue(reloaded.playerLevel >= 2)
    assertTrue(reloaded.weaponLevel("wpn_starblade_lancer") >= 2)
    assertEquals("heavy_bore", reloaded.weaponCustom.barrelId)
    assertEquals("cryo_stasis", reloaded.weaponCustom.ammoModId)
    assertEquals(2L, reloaded.wonderWeaponsPickedTotal)
  }

  @Test
  fun battlePassEditions_allFourVersionsUnlockAndAlterArenaStats() {
    val store = ProgressionStore(context, "test_op")
    val statsStd = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current)

    // Unlock V2 Blackcell (costs 600)
    val blackcellRes = store.activateOrUnlockPassVersion(BattlePassVersion.BLACKCELL_OVERDRIVE, 1500L)
    assertTrue(blackcellRes.isSuccess)
    assertEquals(600L, blackcellRes.getOrThrow())
    val statsBlackcell = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current)
    assertTrue(statsBlackcell.fireCadenceTicks < statsStd.fireCadenceTicks)
    assertTrue(statsBlackcell.maxShield > statsStd.maxShield)

    // Unlock V3 Chrono-Vault (costs 750)
    val chronoRes = store.activateOrUnlockPassVersion(BattlePassVersion.CHRONO_MULTIVERSE, 1500L)
    assertTrue(chronoRes.isSuccess)
    val statsChrono = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current)
    assertTrue(statsChrono.powerCooldownTicks < statsStd.powerCooldownTicks)
    assertTrue(statsChrono.maxShield >= statsBlackcell.maxShield)

    // Unlock V4 Hellbreaker Supreme (costs 900)
    val hellRes = store.activateOrUnlockPassVersion(BattlePassVersion.HELLBREAKER_SUPREME, 1500L)
    assertTrue(hellRes.isSuccess)
    val statsHell = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current)
    assertTrue(statsHell.maxShield > statsChrono.maxShield)
    assertTrue(statsHell.moveBoost > statsStd.moveBoost)
    assertEquals(4, BattlePassVersion.HELLBREAKER_SUPREME.glorySiphonPerBossHit)

    // Claim Tier 1 reward on Hellbreaker Supreme (requires 0 BP XP)
    val tier1Res = store.claimPassTier(BattlePassVersion.HELLBREAKER_SUPREME, 1)
    assertTrue(tier1Res.isSuccess)
    assertEquals(350L, tier1Res.getOrThrow())
  }

  @Test
  fun wonderWeapons_spawnCollectAndFireAllSixKinds() {
    val store = ProgressionStore(context, "test_op")
    store.activateOrUnlockPassVersion(BattlePassVersion.CHRONO_MULTIVERSE, 1000L)
    val world = UniverseCatalog.worldById("sector_zero")
    val loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current)
    val sim = ArenaSimState(world, "zombies", loadout, false, AppSettings(), store.current)

    // Test random wonder crate spawn + pickup collection
    val droppedKind = sim.spawnRandomWonderWeapon(x = sim.playerX, y = sim.playerY)
    assertEquals(1, sim.wonderPickups.size)
    sim.stepFrame(0.016f)
    assertEquals(droppedKind, sim.hudActiveWonder)
    assertEquals(1, sim.hudWonderPickedCount)

    // Verify all 6 Wonder Weapon variants fire cleanly and apply mechanics
    WonderWeaponKind.entries.forEach { kind ->
      sim.bolts.clear()
      sim.arcs.clear()
      sim.collectWonderPickup(kind)
      sim.hostiles.add(ArenaHostile(x = sim.playerX + 0.08f, y = sim.playerY, hp = 20, maxHp = 20, speed = 0.001f))
      sim.fireVolley(forceManual = true)
      assertTrue("${kind.name} should spawn bolts", sim.bolts.isNotEmpty())
      if (kind == WonderWeaponKind.STARBLADE_BOOMERANG) {
        assertEquals(6, sim.bolts.size)
      }
      sim.stepFrame(0.016f)
    }

    val report = sim.buildProgressionReport()
    assertEquals(1 + WonderWeaponKind.entries.size, report.wonderPickupsCollected)
  }

  @Test
  fun challenges_trackClaimAndResetForReplay() {
    val store = ProgressionStore(context, "test_op")
    store.recordMatchProgression(
      MatchProgressionReport(
        eliminations = 45L,
        bossEliminations = 3L,
        waveReached = 4L,
        wonderPickupsCollected = 3,
        weaponIdUsed = "wpn_pulse_carbine",
        extracted = true,
      )
    )

    val list = store.challengeStates()
    assertTrue(list.first { it.spec.id == "ch_hostiles_40" }.completed)
    assertTrue(list.first { it.spec.id == "ch_bosses_2" }.completed)
    assertTrue(list.first { it.spec.id == "ch_wonder_2" }.completed)
    assertTrue(list.first { it.spec.id == "ch_wave_4" }.completed)

    val claimRes = store.claimChallengeReward("ch_hostiles_40")
    assertTrue(claimRes.isSuccess)
    val (essence, pxp, bpxp) = claimRes.getOrThrow()
    assertEquals(350L, essence)
    assertEquals(450L, pxp)
    assertEquals(350L, bpxp)

    store.resetCompletedChallengesForReplay()
    val resetList = store.challengeStates()
    assertEquals(0L, resetList.first { it.spec.id == "ch_hostiles_40" }.progress)
    assertTrue(resetList.none { it.claimed })
  }

  @Test
  fun armoryScreen_allCustomizationAndPassTabsRenderAndUpdate() {
    val store = ProgressionStore(context, "test_op")
    composeRule.setContent {
      var prog by rememberStateOf(store.current)
      UnknownUniversesTheme(darkTheme = true) {
        ArmoryScreen(
          armory = emptyList(),
          totalEssence = 2500L,
          onToggleEquip = {},
          onUpgradeItem = {},
          onCreateCustomItem = { _, _, _, _, _, _, _ -> },
          onDeleteItem = {},
          progression = prog,
          challenges = store.challengeStates(prog),
          onUpdateWeaponCustom = { t ->
            store.updateWeaponCustomization(t)
            prog = store.current
          },
          onUpdateCharacterCustom = { t ->
            store.updateCharacterCustomization(t)
            prog = store.current
          },
          onUpdateLevelCustom = { t ->
            store.updateLevelCustomization(t)
            prog = store.current
          },
          onSelectOrUnlockPass = { v ->
            store.activateOrUnlockPassVersion(v, 2500L)
            prog = store.current
          },
          onClaimPassTier = { v, tier ->
            store.claimPassTier(v, tier)
            prog = store.current
          },
          onClaimChallenge = {},
          onRefreshChallenges = {},
        )
      }
    }

    composeRule.onNodeWithTag("armory_tab_weapons").performClick()
    composeRule.onNodeWithTag("weapon_level_badge").assertIsDisplayed()
    composeRule.onNodeWithTag("select_weapon_wpn_crucible_cannon").performClick()
    assertEquals("wpn_crucible_cannon", store.current.weaponCustom.equippedWeaponId)

    composeRule.onNodeWithTag("armory_tab_character").performClick()
    composeRule.onNodeWithTag("player_level_rank_text").assertIsDisplayed()
    composeRule.onNodeWithTag("suit_chip_blackcell_gold").performClick()
    assertEquals("blackcell_gold", store.current.characterCustom.suitThemeId)

    composeRule.onNodeWithTag("armory_tab_levels").performClick()
    composeRule.onNodeWithTag("mutator_boss_rush").performClick()
    assertTrue(store.current.levelCustom.bossRushMutator)

    composeRule.onNodeWithTag("armory_tab_pass").performClick()
    composeRule.onNodeWithTag("bp_edition_${BattlePassVersion.HELLBREAKER_SUPREME.id}").performClick()
    composeRule.onNodeWithTag("bp_activate_unlock_btn").performClick()
    assertEquals(BattlePassVersion.HELLBREAKER_SUPREME, store.current.activePassVersion)
  }

  @Test
  fun arenaScreen_displaysPlayerWeaponLevelPassAndWonderHud() {
    val store = ProgressionStore(context, "test_op")
    store.activateOrUnlockPassVersion(BattlePassVersion.BLACKCELL_OVERDRIVE, 1000L)

    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        ArenaScreen(
          profile = OperatorProfile(
            id = "test_op",
            userId = "test_op",
            activeUniverseId = "sector_zero",
            activeMode = "sandbox",
          ),
          armory = emptyList(),
          rigs = emptyList(),
          settings = AppSettings(reduceMotion = true),
          progression = store.current,
          onSaveMatchAndExit = { _, _, _, _, _ -> },
        )
      }
    }
    composeRule.mainClock.advanceTimeByFrame()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("arena_level_weapon_hud").assertIsDisplayed()
    composeRule.onNodeWithTag("arena_level_weapon_hud").assertTextContains("LV 1", substring = true)
    composeRule.onNodeWithTag("arena_level_weapon_hud").assertTextContains("V2", substring = true)
    composeRule.onNodeWithTag("sandbox_spawn_wonder_btn").assertIsDisplayed()
  }

  @Test
  fun equipmentAndGear_upgradesLevelsAndPrestiges() {
    val store = ProgressionStore(context, "test_op")
    assertEquals(1, store.current.equipmentState("lethal").level)
    assertEquals(1, store.current.equipmentState("tactical").level)

    // Upgrade lethal equipment
    store.upgradeEquipment("lethal")
    assertEquals(2, store.current.equipmentState("lethal").level)

    // Equip different lethal item
    store.equipEquipment("lethal", "lethal_c4")
    assertEquals("lethal_c4", store.current.equipmentState("lethal").equippedItemId)

    // Gear upgrade and prestige
    val helmet = store.current.gearMap[GearSlot.HELMET.id]
    assertNotNull(helmet)
    assertEquals(1, helmet!!.level)
    store.upgradeGear(GearSlot.HELMET)
    assertEquals(2, store.current.gearMap[GearSlot.HELMET.id]?.level)

    // Skill tree unlock
    val skillRes = store.unlockSkillNode("sk_shield_1", 1000L)
    assertTrue(skillRes.isSuccess)
    assertTrue(store.current.skillNodes["sk_shield_1"]?.unlocked == true)

    // Verify persistence across reload
    val reloaded = ProgressionStore(context, "test_op").current
    assertEquals(2, reloaded.equipmentState("lethal").level)
    assertEquals("lethal_c4", reloaded.equipmentState("lethal").equippedItemId)
    assertEquals(2, reloaded.gearMap[GearSlot.HELMET.id]?.level)
    assertTrue(reloaded.skillNodes["sk_shield_1"]?.unlocked == true)
  }

  @Test
  fun packAPunchAndEquipment_inArenaSim() {
    val store = ProgressionStore(context, "test_op")
    val world = UniverseCatalog.worldById("sector_zero")
    val loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current)
    val sim = ArenaSimState(world, "zombies", loadout, false, AppSettings(), store.current)

    assertEquals(1, sim.hudPackTier)
    sim.upgradePackAPunch()
    assertEquals(2, sim.hudPackTier)
    sim.upgradePackAPunch()
    assertEquals(3, sim.hudPackTier)

    // Trigger lethal equipment in arena
    sim.triggerLethal()
    assertTrue(sim.lethalCd > 0f)

    // Trigger tactical equipment in arena
    sim.triggerTactical()
    assertTrue(sim.tacticalCd > 0f)
  }

  @Test
  fun armoryScreen_equipmentGearAndSkillTabsRender() {
    val store = ProgressionStore(context, "test_op")
    composeRule.setContent {
      var prog by rememberStateOf(store.current)
      UnknownUniversesTheme(darkTheme = true) {
        ArmoryScreen(
          armory = emptyList(),
          totalEssence = 2500L,
          onToggleEquip = {},
          onUpgradeItem = {},
          onCreateCustomItem = { _, _, _, _, _, _, _ -> },
          onDeleteItem = {},
          progression = prog,
          onUpgradeEquipment = { slot ->
            store.upgradeEquipment(slot)
            prog = store.current
          },
          onUpgradeGear = { slot ->
            store.upgradeGear(slot)
            prog = store.current
          },
          onUnlockSkill = { id ->
            store.unlockSkillNode(id, 2500L)
            prog = store.current
          },
        )
      }
    }

    composeRule.onNodeWithTag("armory_tab_equipment").performClick()
    composeRule.onNodeWithTag("eq_level_lethal").assertIsDisplayed()

    composeRule.onNodeWithTag("armory_tab_gear").performClick()
    composeRule.onNodeWithTag("gear_prestige_badge").assertIsDisplayed()

    composeRule.onNodeWithTag("armory_tab_skills").performClick()
    composeRule.onNodeWithTag("skills_unlocked_badge").assertIsDisplayed()
  }
}

@androidx.compose.runtime.Composable
private fun <T> rememberStateOf(initial: T) = androidx.compose.runtime.remember { mutableStateOf(initial) }

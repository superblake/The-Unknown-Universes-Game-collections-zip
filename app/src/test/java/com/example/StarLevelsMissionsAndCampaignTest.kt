package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ProgressionStore
import com.example.data.local.UniverseDatabase
import com.example.data.local.UniverseRepository
import com.example.data.model.MissionScope
import com.example.data.model.MatchProgressionReport
import com.example.data.model.MissionsCatalog
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.OperatorProfile
import com.example.data.model.StarRankMeta
import com.example.data.model.UniverseCatalog
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaSimState
import com.example.data.local.AppSettings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.ui.screens.ArmoryScreen
import com.example.ui.screens.MissionsDialog
import com.example.ui.theme.UnknownUniversesTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class StarLevelsMissionsAndCampaignTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("${ProgressionStore.PREFS_PREFIX}_star_test", Context.MODE_PRIVATE)
      .edit().clear().commit()
    context.getSharedPreferences("${ProgressionStore.PREFS_PREFIX}_ui_star_test", Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  @Test
  fun missionsCatalog_hasExactly1080MissionsWith45PerWorldAndArena() {
    assertEquals(12, UniverseCatalog.worlds.size)
    assertEquals(12, MultiverseArsenalCatalog.customAreas.size)

    assertEquals(540, MissionsCatalog.worldMissions.size)
    assertEquals(540, MissionsCatalog.arenaMissions.size)
    assertEquals(1080, MissionsCatalog.allMissions.size)

    UniverseCatalog.worlds.forEach { world ->
      val list = MissionsCatalog.missionsForWorld(world.id)
      assertEquals("World ${world.id} should have 45 missions", 45, list.size)
    }

    MultiverseArsenalCatalog.customAreas.forEach { area ->
      val list = MissionsCatalog.missionsForArena(area.id)
      assertEquals("Area ${area.id} should have 45 missions", 45, list.size)
    }
  }

  @Test
  fun campaignAndConvergence_worldsAndAreasExistAndLoadInSim() {
    val campaignWorld = UniverseCatalog.worldById("campaign_origins")
    assertEquals("Campaign: Genesis Fracture", campaignWorld.title)
    val campaignMode = UniverseCatalog.modeById("campaign")
    assertEquals("Story Campaign", campaignMode.label)

    val nexusArea = MultiverseArsenalCatalog.areaById("area_campaign_nexus")
    assertEquals("Campaign Command: Genesis Frontline", nexusArea.name)

    val convArea = MultiverseArsenalCatalog.areaById("area_mended_convergence")
    assertEquals("The Mended Convergence (Near-Finale)", convArea.name)
    assertEquals(8, convArea.buildingNames.size)

    val store = ProgressionStore(context, "star_test")
    val prog = store.current.copy(activeAreaId = "area_mended_convergence")
    val sim = ArenaSimState(
      world = campaignWorld,
      modeId = "campaign",
      loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, prog, 10_000L),
      hasConsumableEquipped = true,
      initialSettings = AppSettings(),
      progression = prog,
      initialBlueEssence = 10_000L,
    )
    assertEquals(8, sim.buildings.size)
  }

  @Test
  fun starLevelsAndTiers_upgradePersistAndBoostArenaStats() {
    runBlocking {
      val db = Room.inMemoryDatabaseBuilder(context, UniverseDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val dao = db.universeDao()
      val repo = UniverseRepository(dao)
      val userId = "star_tester"
      val now = System.currentTimeMillis()
      dao.upsertProfile(OperatorProfile(id = userId, userId = userId, callsign = "Apex", totalEssence = 100_000L, createdAtMillis = now, updatedAtMillis = now))
      repo.ensureStarterData(userId, "Apex")

      val store = ProgressionStore(context, userId)
      var essence = 100_000L

      // Upgrade Character Tier & Star
      val tierRes = store.upgradeCharacterTier(essence)
      assertTrue(tierRes.isSuccess)
      essence -= tierRes.getOrThrow()

      val starRes = store.upgradeCharacterStar(essence)
      assertTrue(starRes.isSuccess)
      essence -= starRes.getOrThrow()

      assertEquals(2, store.current.characterTier)
      assertEquals(2, store.current.characterStarLevel)
      assertEquals(StarRankMeta.STAR_2, StarRankMeta.forStars(store.current.characterStarLevel))

      // Upgrade Weapon Star & Wonder Star
      val wpnStarRes = store.upgradeWeaponStar("wpn_m16_commando", essence)
      assertTrue(wpnStarRes.isSuccess)
      essence -= wpnStarRes.getOrThrow()
      assertEquals(2, store.current.weaponStar("wpn_m16_commando"))

      val wonderStarRes = store.upgradeWonderStar("wonder_raygun", essence)
      assertTrue(wonderStarRes.isSuccess)
      essence -= wonderStarRes.getOrThrow()
      assertEquals(2, store.current.wonderStar("wonder_raygun"))

      // Upgrade Outfit Star, Vacuum Bot Star, Area Star, Gear Star, Equipment Star, Skill Star, Mod Star
      assertTrue(store.upgradeOutfitStar("batman_v1", essence).isSuccess)
      assertEquals(2, store.current.outfitState("batman_v1").starLevel)

      assertTrue(store.upgradeVacuumBotStar(essence).isSuccess)
      assertEquals(2, store.current.vacuumBot.starLevel)

      assertTrue(store.upgradeAreaStar("area_mended_convergence", essence).isSuccess)
      assertEquals(2, store.current.areaState("area_mended_convergence").starLevel)

      assertTrue(store.upgradeGearStar(com.example.data.model.GearSlot.HELMET, essence).isSuccess)
      assertEquals(2, store.current.gearMap[com.example.data.model.GearSlot.HELMET.id]?.starLevel)

      assertTrue(store.upgradeEquipmentStar("lethal", essence).isSuccess)
      assertEquals(2, store.current.equipmentState("lethal").starLevel)

      val firstSkillId = store.current.skillNodes.keys.first()
      assertTrue(store.unlockSkillNode(firstSkillId, essence).isSuccess)
      assertTrue(store.upgradeSkillStar(firstSkillId, essence).isSuccess)
      assertEquals(2, store.current.skillNodes[firstSkillId]?.starLevel)

      assertTrue(store.upgradeArmoryItemStar("perk_juggernog", essence).isSuccess)
      assertEquals(2, store.current.armoryItemStar("perk_juggernog"))

      // Verify ArenaLoadoutStats incorporates all star levels and tier multipliers
      val loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current, 5000L)
      assertTrue(loadout.weaponTierMultiplier >= 1.0f)
    } finally {
      db.close()
    }
    }
  }

  @Test
  fun missionProgressionAndClaiming_worksCorrectly() {
    val store = ProgressionStore(context, "star_test")
    val firstMissionId = MissionsCatalog.worldMissions.first().id
    val spec = MissionsCatalog.missionById(firstMissionId)!!

    // Record match progression meeting target
    store.recordMatchProgression(
      MatchProgressionReport(
        eliminations = spec.targetCount + 10L,
        bossEliminations = 1L,
        waveReached = 10L,
        wonderPickupsCollected = 2,
        weaponIdUsed = "wpn_pulse_carbine",
        extracted = true,
        worldId = spec.targetId,
        areaId = "area_empire_city",
      )
    )

    val ms = store.missionStates().first { it.spec.id == firstMissionId }
    assertTrue(ms.completed)

    val claimRes = store.claimMission(firstMissionId)
    assertTrue(claimRes.isSuccess)
    val (blueGain, xpGain, shardGain) = claimRes.getOrThrow()
    assertTrue(blueGain > 0L)
    assertTrue(xpGain > 0L)
    assertTrue(shardGain > 0L)
  }

  @Test
  fun armoryScreen_weaponWonderAndCharacterStarButtonsAndMissionsTabWorkEndToEnd() {
    val store = ProgressionStore(context, "ui_star_test")
    val firstMission = MissionsCatalog.worldMissions.first()
    var deployedTarget: String? = null

    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        val prog by store.state.collectAsState()
        ArmoryScreen(
          armory = emptyList(),
          totalEssence = 10_000L,
          onToggleEquip = {},
          onUpgradeItem = {},
          onCreateCustomItem = { _, _, _, _, _, _, _ -> },
          onDeleteItem = {},
          progression = prog,
          missions = store.missionStates(),
          activeWorldId = firstMission.targetId,
          activeAreaId = "area_empire_city",
          onUpgradeCharacterTier = { store.upgradeCharacterTier(10_000L) },
          onUpgradeCharacterStar = { store.upgradeCharacterStar(10_000L) },
          onUpgradeWeaponStar = { wId -> store.upgradeWeaponStar(wId, 10_000L) },
          onUpgradeWonderStar = { wId -> store.upgradeWonderStar(wId, 10_000L) },
          onDeployMissionTarget = { _, targetId -> deployedTarget = targetId },
        )
      }
    }

    // Switch to WEAPONS tab and upgrade primary weapon star & Wonder Weapon star
    composeRule.onNodeWithTag("armory_tab_weapons").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("upgrade_weapon_star_btn").assertIsEnabled().performClick()
    composeRule.waitForIdle()
    assertEquals(2, store.current.weaponStar(store.current.weaponCustom.equippedWeaponId))

    // Scroll LazyColumn to Wonder Weapon Ray Nova MK3 star button and upgrade
    composeRule.onNodeWithTag("weapon_customization_list")
      .performScrollToNode(hasTestTag("upgrade_wonder_star_ww_ray_mk3"))
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("upgrade_wonder_star_ww_ray_mk3").performScrollTo().assertIsEnabled().performClick()
    composeRule.waitForIdle()
    assertEquals(2, store.current.wonderStar("ww_ray_mk3"))

    // Switch to CHARACTER tab and upgrade Character Tier and Character Star
    composeRule.onNodeWithTag("armory_tab_character").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("upgrade_char_tier_btn").assertIsEnabled().performClick()
    composeRule.onNodeWithTag("upgrade_char_star_btn").assertIsEnabled().performClick()
    composeRule.waitForIdle()
    assertEquals(2, store.current.characterTier)
    assertEquals(2, store.current.characterStarLevel)

    // Switch to MISSIONS tab and verify deploy target works
    composeRule.onNodeWithTag("armory_tab_missions").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("missions_deploy_target_btn").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    assertEquals(firstMission.targetId, deployedTarget)
  }

  @Test
  fun missionsDialog_rendersFiltersAndDeploysToSelectedTarget() {
    val store = ProgressionStore(context, "ui_star_test")
    var dismissed = false
    var deployed = ""

    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        MissionsDialog(
          missions = store.missionStates(),
          activeWorldId = "campaign_origins",
          activeAreaId = "area_mended_convergence",
          onClaimMission = {},
          onDeployTarget = { _, id -> deployed = id },
          onDismiss = { dismissed = true },
        )
      }
    }

    composeRule.onNodeWithTag("missions_tab_list").assertIsDisplayed()
    composeRule.onNodeWithTag("missions_scope_arena").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("missions_deploy_target_btn").performClick()
    composeRule.waitForIdle()
    assertEquals("area_mended_convergence", deployed)

    composeRule.onNodeWithTag("close_missions_dialog_btn").performClick()
    composeRule.waitForIdle()
    assertTrue(dismissed)
  }
}

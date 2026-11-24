package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettings
import com.example.data.local.ProgressionStore
import com.example.data.local.SaveDataSerializer
import com.example.data.local.UniverseDatabase
import com.example.data.local.UniverseRepository
import com.example.data.model.AreaAgeEra
import com.example.data.model.ArmoryItem
import com.example.data.model.BlastCoreColor
import com.example.data.model.CharacterUniverseGroup
import com.example.data.model.EquipmentSlotType
import com.example.data.model.EssenceResonanceBonuses
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.OperatorProfile
import com.example.data.model.UniverseCatalog
import com.example.data.remote.FirestoreSyncCodec
import com.example.ui.screens.ARENA_MAX_X
import com.example.ui.screens.ARENA_WORLD_SIZE
import com.example.ui.screens.ArenaCollectible
import com.example.ui.screens.ArenaCollectibleKind
import com.example.ui.screens.ArenaHostile
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.ArmoryScreen
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
class MultiverseExpansionTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("${ProgressionStore.PREFS_PREFIX}_exp_test", Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  @Test
  fun modTiers_unlockUpToTier25AcrossRoomCodecSaveAndArenaLoadout() = runBlocking {
    val db = Room.inMemoryDatabaseBuilder(context, UniverseDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val dao = db.universeDao()
      val repo = UniverseRepository(dao)
      val userId = "tier25_tester"
      val now = System.currentTimeMillis()
      dao.upsertProfile(OperatorProfile(id = userId, userId = userId, callsign = "Vanguard", totalEssence = 150_000L, createdAtMillis = now, updatedAtMillis = now))
      // Legacy item stuck at tier 5 / maxTier 5
      val legacyPack = ArmoryItem(
        id = "upg_pack_a_punch",
        userId = userId,
        name = "Pack-a-Punch Core",
        category = "upgrade",
        universeTag = "All Universes",
        description = "Enhances weapon fire cadence.",
        cooldownSec = 0L,
        powerValue = 250L,
        tier = 5L,
        maxTier = 5L,
        equipped = true,
        accentColorHex = "#FFAB40",
        isCustom = false,
      )
      dao.upsertArmoryItem(legacyPack)

      // ensureStarterData migrates legacy maxTier < 25 -> 25
      repo.ensureStarterData(userId, "Vanguard")
      val migrated = dao.getArmoryItemsOnce(userId).first { it.id == "upg_pack_a_punch" }
      assertEquals(25L, migrated.maxTier)

      // Upgrade all the way to Tier 25
      var currentItem = migrated
      var essence = 150_000L
      while (currentItem.tier < 25L) {
        val cost = currentItem.tier * 300L
        val res = repo.upgradeArmoryItem(userId, currentItem, cost, essence)
        assertTrue("Upgrade from tier ${currentItem.tier} failed: ${res.exceptionOrNull()}", res.isSuccess)
        essence -= cost
        currentItem = dao.getArmoryItemsOnce(userId).first { it.id == "upg_pack_a_punch" }
      }
      assertEquals(25L, currentItem.tier)
      assertEquals(25L, currentItem.maxTier)

      // Verify ArenaLoadoutStats respects Tier 25 Pack-a-Punch
      val loadout = ArenaLoadoutStats.fromArmory(listOf(currentItem), Color.Cyan)
      assertEquals(25, loadout.initialPackTier)

      // Verify FirestoreSyncCodec & SaveDataSerializer preserve Tier 25 (not clamped to 5, 6, or 10)
      val fsDoc = with(FirestoreSyncCodec) { currentItem.toFirestoreCreateMap(userId) }
      assertEquals(25L, fsDoc["tier"])
      assertEquals(25L, fsDoc["maxTier"])
      val exportJson = SaveDataSerializer.exportSaveJson(
        profile = dao.getProfileOnce(userId)!!,
        armory = listOf(currentItem),
        rigs = emptyList<com.example.data.model.RigAsset>(),
        matchLogs = emptyList<com.example.data.model.MatchRecord>(),
      )
      val parsed = SaveDataSerializer.parseAndValidateSaveJson(exportJson, userId).getOrThrow()
      assertEquals(25L, parsed.bundle.armoryItems.first().tier)
      assertEquals(25L, parsed.bundle.armoryItems.first().maxTier)
    } finally {
      db.close()
    }
  }

  @Test
  fun heroOutfits_coverAll36VersionsWithIndependentSkillsGearEquipment() {
    val outfits = MultiverseArsenalCatalog.outfits
    assertEquals(CharacterUniverseGroup.entries.count { it != CharacterUniverseGroup.ALL } * 3, outfits.size)
    // Ensure every requested character group has v1/v2/v3
    for (group in CharacterUniverseGroup.entries.filter { it != CharacterUniverseGroup.ALL }) {
      val groupOutfits = outfits.filter { it.characterGroup == group }
      assertEquals("Group ${group.label} should have 3 versions", 3, groupOutfits.size)
      assertTrue(groupOutfits.any { it.nameWithVersion.contains("v1") })
      assertTrue(groupOutfits.any { it.nameWithVersion.contains("v2") })
      assertTrue(groupOutfits.any { it.nameWithVersion.contains("v3") })
    }

    val store = ProgressionStore(context, "exp_test")
    val eqRes = store.equipOutfit("batman_v2")
    assertEquals("Batman v2", eqRes.nameWithVersion)
    assertEquals("batman_v2", store.current.equippedOutfitId)

    // Upgrade Batman v2 skills, gear, equipment & level independently from Cole v3
    assertTrue(store.upgradeOutfit("batman_v2", 5000L).isSuccess)
    assertTrue(store.upgradeOutfitSubSystem("batman_v2", "skill", 5000L).isSuccess)
    assertTrue(store.upgradeOutfitSubSystem("batman_v2", "gear", 5000L).isSuccess)
    assertTrue(store.upgradeOutfitSubSystem("batman_v2", "equipment", 5000L).isSuccess)

    val batState = store.current.outfitState("batman_v2")
    val coleState = store.current.outfitState("cole_v3")
    assertEquals(6, batState.level)
    assertEquals(2, batState.skillRank)
    assertEquals(2, batState.gearRank)
    assertEquals(2, batState.equipmentRank)
    assertEquals(1, coleState.level)
    assertEquals(1, coleState.skillRank)
  }

  @Test
  fun multiColorEssenceScaling_andTradingPostFusionAndBlastCoresWork() {
    // Low essence vs high essence scaling
    val low = EssenceResonanceBonuses.compute(blueEssence = 100L, redEssence = 0L, yellowEssence = 0L, greenEssence = 0L, combinedEssence = 0L, blastShards = 0L)
    val highBlue = EssenceResonanceBonuses.compute(blueEssence = 500_000L, redEssence = 0L, yellowEssence = 0L, greenEssence = 0L, combinedEssence = 0L, blastShards = 0L)
    val highRed = EssenceResonanceBonuses.compute(blueEssence = 500_000L, redEssence = 500_000L, yellowEssence = 0L, greenEssence = 0L, combinedEssence = 0L, blastShards = 0L)
    val withCombined = EssenceResonanceBonuses.compute(blueEssence = 500_000L, redEssence = 500_000L, yellowEssence = 500_000L, greenEssence = 500_000L, combinedEssence = 500_000L, blastShards = 50L)

    assertTrue(highBlue.damageMult > low.damageMult)
    assertTrue("Red Essence must exceed Blue-only damage cap", highRed.damageMult > highBlue.damageMult + 1.5f)
    assertTrue("Combined Quad-Essence must exceed single-color caps", withCombined.damageMult > highRed.damageMult + 2.0f)
    assertTrue(withCombined.shieldMult > highRed.shieldMult + 2.0f)

    // Trading Post & Shards/Perks/Cores
    val store = ProgressionStore(context, "exp_test")
    var blue = 5000L
    // Swap Blue -> Red, Yellow, Green
    listOf("trade_blue_to_red", "trade_blue_to_yellow", "trade_blue_to_green").forEach { offerId ->
      val d = store.executeTradeOffer(offerId, blue).getOrThrow()
      blue += d
    }
    assertTrue(store.current.redEssence >= 400L)
    assertTrue(store.current.yellowEssence >= 400L)
    assertTrue(store.current.greenEssence >= 400L)

    val beforeCombined = store.current.combinedEssence
    val fuseDelta = store.executeTradeOffer("trade_fuse_combined", blue).getOrThrow()
    blue += fuseDelta
    assertEquals(beforeCombined + 150L, store.current.combinedEssence)

    // Forge & fuse a Purple Blast Core + Upgrade a Perk Drink
    val beforeCores = store.current.blastCoreCount(BlastCoreColor.PURPLE)
    blue += store.executeTradeOffer("trade_essence_to_purple_core", blue).getOrThrow()
    assertEquals(beforeCores + 2L, store.current.blastCoreCount(BlastCoreColor.PURPLE))
    assertTrue(store.upgradeBlastCore(BlastCoreColor.PURPLE).isSuccess)
    assertEquals(2, store.current.blastCoreLevel(BlastCoreColor.PURPLE))

    assertTrue(store.upgradePerkDrink("perk_juggernog", blue).isSuccess)
    assertEquals(1, store.current.perkRank("perk_juggernog"))
  }

  @Test
  fun areasAndAging_transitionsYoungMatureAncientAndRejuvenatesToYoung() {
    val store = ProgressionStore(context, "exp_test")
    assertEquals(22, MultiverseArsenalCatalog.customAreas.size)
    store.selectArea("area_batcave")
    assertEquals("area_batcave", store.current.activeAreaId)

    // Initial age is 12 -> YOUNG
    assertEquals(AreaAgeEra.YOUNG, store.current.areaState("area_batcave").currentEra)
    // +30 -> 42 -> MATURE
    val mature = store.advanceAreaAge("area_batcave", 30)
    assertEquals(42, mature.ageYears)
    assertEquals(AreaAgeEra.MATURE, mature.currentEra)
    // +40 -> 82 -> ANCIENT
    val ancient = store.advanceAreaAge("area_batcave", 40)
    assertEquals(82, ancient.ageYears)
    assertEquals(AreaAgeEra.ANCIENT, ancient.currentEra)
    // +25 -> 107 -> wraps back to 7 (YOUNG) and increments rebirthCycles to 1
    val reborn = store.advanceAreaAge("area_batcave", 25)
    assertEquals(7, reborn.ageYears)
    assertEquals(AreaAgeEra.YOUNG, reborn.currentEra)
    assertEquals(1, reborn.rebirthCycles)
    assertTrue(reborn.cycleBonusMult > 1.0f)

    assertTrue(store.upgradeAreaFeature("area_batcave", "turrets", 2000L).isSuccess)
    assertEquals(2, store.current.areaState("area_batcave").defenseTurretTier)
  }

  @Test
  fun multiEquipAndExpandedArena_buildingsTrapsVacuumAndControlsWork() {
    val store = ProgressionStore(context, "exp_test")
    // Equip up to 3 weapons, 3 wonder weapons, 2 lethals, 2 tacticials
    store.toggleMultiWeaponEquip("wpn_plasma_scatter")
    store.toggleMultiWeaponEquip("wpn_starblade_lancer")
    assertEquals(3, store.current.effectiveWeaponList().size)

    store.toggleMultiWonderEquip("wonder_wunderwaffe")
    store.toggleMultiWonderEquip("wonder_thundergun")
    assertEquals(3, store.current.effectiveWonderList().size)

    store.toggleMultiEquipmentEquip(EquipmentSlotType.LETHAL, "lethal_molotov")
    store.toggleMultiEquipmentEquip(EquipmentSlotType.TACTICAL, "tactical_emp")
    assertEquals(2, store.current.equippedLethalIds.size)
    assertEquals(2, store.current.equippedTacticalIds.size)

    val sim = ArenaSimState(
      world = UniverseCatalog.worlds.first(),
      modeId = "zombies",
      loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current, 2500L),
      hasConsumableEquipped = true,
      initialSettings = AppSettings(),
      progression = store.current,
      initialBlueEssence = 2500L,
    )

    // Verify expanded world, 6 buildings, Electric & Fire traps
    assertTrue(ARENA_WORLD_SIZE >= 2.0f)
    assertTrue(ARENA_MAX_X > 2.0f)
    assertEquals(6, sim.buildings.size)
    assertTrue(sim.traps.any { it.kind.name == "ELECTRIC" })
    assertTrue(sim.traps.any { it.kind.name == "FIRE" })

    // Verify multi-weapon swap & wonder cycling
    val firstWeapon = sim.hudActiveWeaponName
    sim.cycleActiveWeapon()
    assertTrue(sim.hudActiveWeaponName != firstWeapon)
    sim.activateEquippedWonderSlot()
    assertTrue(sim.hudActiveWonder != null)

    // Verify triggers for all electric & fire traps deal damage
    sim.hostiles.clear()
    val trap = sim.traps.first()
    sim.hostiles.add(ArenaHostile(x = trap.x + 0.01f, y = trap.y + 0.01f, hp = 3, maxHp = 3, speed = 0.001f))
    sim.triggerAllTraps()
    assertTrue(sim.trapKillsCount >= 1)

    // Verify Vacuum Bot pulls & collects collectibles and fills charge toward auto-upgrade
    sim.collectibles.clear()
    sim.collectibles.add(ArenaCollectible(sim.playerX + 0.08f, sim.playerY, ArenaCollectibleKind.BLAST_SHARD, amount = 5L))
    sim.collectibles.add(ArenaCollectible(sim.playerX + 0.09f, sim.playerY, ArenaCollectibleKind.BLAST_CORE, subId = BlastCoreColor.BLUE.id, amount = 1L))
    // Use RED_ORB too to test colored essence collection
    sim.collectibles.add(ArenaCollectible(sim.playerX + 0.02f, sim.playerY, ArenaCollectibleKind.RED_ORB, amount = 40L))
    for (t in 0 until 45) {
      sim.stepFrame(dtSec = 1f / 60f)
    }
    assertTrue("Blast shards should be vacuum-collected", sim.blastShardsEarned >= 5L)
    assertTrue("Red essence orb should be vacuum-collected", sim.redEssenceEarned >= 40L)
    assertTrue("Vacuum charge should advance", sim.hudVacuumChargePct > 0 || sim.hudPackTier > 1)
  }

  @Test
  fun armoryUi_navigatesOutfitsShardsAreasAndTradingPost() {
    val store = ProgressionStore(context, "exp_test")
    composeRule.setContent {
      var prog by remember { mutableStateOf(store.current) }
      var blue by mutableLongStateOf(5000L)
      UnknownUniversesTheme(darkTheme = true) {
        ArmoryScreen(
          armory = emptyList(),
          totalEssence = blue,
          onToggleEquip = {},
          onUpgradeItem = {},
          onCreateCustomItem = { _, _, _, _, _, _, _ -> },
          onDeleteItem = {},
          progression = prog,
          onEquipOutfit = { id ->
            store.equipOutfit(id)
            prog = store.current
          },
          onAdvanceAreaAge = { id, yrs ->
            store.advanceAreaAge(id, yrs)
            prog = store.current
          },
          onExecuteTradeOffer = { offerId ->
            store.executeTradeOffer(offerId, blue).onSuccess { d ->
              blue += d
              prog = store.current
            }
          },
        )
      }
    }

    // Use easy-to-move Next > button to step to OUTFITS tab
    composeRule.onNodeWithTag("armory_next_section_btn").performClick()
    composeRule.onNodeWithTag("outfits_menu_list").assertIsDisplayed()
    composeRule.onNodeWithTag("outfit_group_ironman").performScrollTo().performClick()
    composeRule.onNodeWithTag("outfits_menu_list").performScrollToNode(hasTestTag("equip_outfit_ironman_v2"))
    composeRule.onNodeWithTag("equip_outfit_ironman_v2").performClick()
    assertEquals("ironman_v2", store.current.equippedOutfitId)

    // Navigate directly to Areas & Aging tab and advance area age
    composeRule.onNodeWithTag("armory_tab_areas").performScrollTo().performClick()
    composeRule.onNodeWithTag("areas_aging_list").assertIsDisplayed()
    val ageBefore = store.current.areaState("area_empire_city").ageYears
    composeRule.onNodeWithTag("areas_aging_list").performScrollToNode(hasTestTag("age_area_area_empire_city"))
    composeRule.onNodeWithTag("age_area_area_empire_city").performClick()
    assertEquals((ageBefore + 18) % 100, store.current.areaState("area_empire_city").ageYears)

    // Navigate to Shards & Cores tab
    composeRule.onNodeWithTag("armory_tab_shards_cores").performScrollTo().performClick()
    composeRule.onNodeWithTag("shards_perks_cores_list").assertIsDisplayed()
    composeRule.onNodeWithTag("essence_resonance_summary").assertIsDisplayed()

    // Navigate to Store -> Trading Post and swap Blue -> Red Essence
    composeRule.onNodeWithTag("armory_tab_store").performScrollTo().performClick()
    composeRule.onNodeWithTag("store_mode_trade").performClick()
    composeRule.onNodeWithTag("blue_essence_store_list").performScrollToNode(hasTestTag("trade_buy_trade_blue_to_red"))
    composeRule.onNodeWithTag("trade_buy_trade_blue_to_red").performClick()
    assertTrue(store.current.redEssence >= 150L)
  }
}

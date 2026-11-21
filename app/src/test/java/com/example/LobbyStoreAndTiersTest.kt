package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettings
import com.example.data.local.ProgressionStore
import com.example.data.model.BotDifficulty
import com.example.data.model.GearSlot
import com.example.data.model.LobbyPartyMode
import com.example.data.model.MatchProgressionReport
import com.example.data.model.OperatorProfile
import com.example.data.model.PowerTier
import com.example.data.model.ProgressionCatalog
import com.example.data.model.UniverseCatalog
import com.example.data.model.WonderWeaponKind
import com.example.ui.screens.ArenaHostile
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.ArmoryScreen
import com.example.ui.screens.OsDesktopScreen
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
class LobbyStoreAndTiersTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("${ProgressionStore.PREFS_PREFIX}_test_mega", Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  @Test
  fun progressionAndTiers_syncXpAndUpgradeAllCategoriesUpToTier25() {
    val store = ProgressionStore(context, "test_mega")

    // Verify manual upgrades sync XP cleanly
    store.upgradeGear(GearSlot.HELMET)
    val helmetAfter = store.current.gearMap[GearSlot.HELMET.id]!!
    assertEquals(2, helmetAfter.level)
    assertTrue(helmetAfter.xp >= ProgressionCatalog.totalXpForGearLevel(2))

    store.upgradeEquipment("lethal")
    val eqAfter = store.current.equipmentState("lethal")
    assertEquals(2, eqAfter.level)
    assertTrue(eqAfter.xp >= ProgressionCatalog.totalXpForEquipmentLevel(2))

    // Verify match progression levels up Player, Weapon, Equipment, and Gear simultaneously
    val (xpGained, _, _) = store.recordMatchProgression(
      MatchProgressionReport(
        eliminations = 50L,
        bossEliminations = 3L,
        waveReached = 6L,
        wonderPickupsCollected = 4,
        weaponIdUsed = "wpn_pulse_carbine",
        extracted = true,
        highestPackTierReached = 25,
        lobbySizePlayed = 32,
      )
    )
    assertTrue(xpGained > 0L)
    assertTrue(store.current.playerLevel > 1)
    assertTrue(store.current.weaponLevel("wpn_pulse_carbine") > 1)
    assertTrue(store.current.equipmentState("tactical").xp > 0L)
    assertTrue(store.current.gearMap[GearSlot.CHEST.id]!!.xp > 0L)
    assertEquals(25, store.current.highestPackTierReached)
    assertEquals(32, store.current.largestLobbyPlayed)

    // Verify PowerTier 1..25 & upgrades on Weapons, Wonder Weapons, Gear, Equipment, Skills
    assertEquals(PowerTier.OMEGA, ProgressionCatalog.tierMeta(10))
    assertEquals(PowerTier.MULTIVERSAL, ProgressionCatalog.tierMeta(25))
    assertTrue(ProgressionCatalog.tierMultiplier(25) > ProgressionCatalog.tierMultiplier(10))
    assertTrue(ProgressionCatalog.tierMultiplier(10) > ProgressionCatalog.tierMultiplier(5))

    var blueEssence = 500_000L
    for (t in 2..25) {
      val wRes = store.upgradeWeaponTier("wpn_pulse_carbine", blueEssence)
      assertTrue("Weapon tier $t should succeed", wRes.isSuccess)
      blueEssence -= wRes.getOrThrow()
    }
    assertEquals(25, store.current.weaponTier("wpn_pulse_carbine"))
    assertTrue(store.upgradeWeaponTier("wpn_pulse_carbine", blueEssence).isFailure)

    assertTrue(store.upgradeWonderTier(WonderWeaponKind.RAY_NOVA_MK3.id, blueEssence).isSuccess)
    assertEquals(2, store.current.wonderTier(WonderWeaponKind.RAY_NOVA_MK3.id))

    assertTrue(store.upgradeGearTier(GearSlot.CHEST, blueEssence).isSuccess)
    assertEquals(2, store.current.gearMap[GearSlot.CHEST.id]!!.tier)

    assertTrue(store.upgradeEquipmentTier("lethal", blueEssence).isSuccess)
    assertEquals(2, store.current.equipmentState("lethal").tier)

    // Unlock + upgrade skill node up to Tier 25
    val skUnlock = store.unlockSkillNode("sk_damage_1", blueEssence)
    assertTrue(skUnlock.isSuccess)
    assertEquals(1, store.current.skillNodes["sk_damage_1"]!!.tier)
    for (st in 2..25) {
      val skUpgrade = store.unlockSkillNode("sk_damage_1", blueEssence)
      assertTrue("Skill tier $st should succeed", skUpgrade.isSuccess)
      blueEssence -= skUpgrade.getOrThrow()
    }
    assertEquals(25, store.current.skillNodes["sk_damage_1"]!!.tier)
    assertTrue(store.unlockSkillNode("sk_damage_1", blueEssence).isFailure)

    // Verify ArmoryItem default maxTier supports Tier 25 Multiversal
    assertEquals(
      25L,
      com.example.data.model.ArmoryItem(
        id = "test",
        userId = "test_mega",
        name = "Test",
        category = "perk",
        universeTag = "Test",
        description = "Test",
      ).maxTier
    )
  }

  @Test
  fun lobbyModesAndBots_supportSoloDuosSquadsOver8PlayersAndArenaCombat() {
    val store = ProgressionStore(context, "test_mega")

    // Default or switched to Squads 32-player Mega Lobby with Apex bots
    store.updateLobbyConfig {
      it.copy(
        partyMode = LobbyPartyMode.SQUADS,
        targetPlayers = 32,
        autoFillBots = true,
        botDifficulty = BotDifficulty.APEX,
        humanPlayersOnline = 1,
      )
    }
    val roster32 = ProgressionCatalog.generateLobbyRoster(
      config = store.current.lobbyConfig,
      playerCallsign = "Vanguard",
      playerLevel = 50,
      playerPrestige = 2,
    )
    assertEquals(32, roster32.size)
    assertEquals(1, roster32.count { it.isHumanPlayer })
    assertEquals(31, roster32.count { !it.isHumanPlayer })
    // Squads of 4: player + 3 allied AI bots in Squad 1
    assertEquals(4, roster32.count { it.squadIndex == 1 && it.isAlliedSquadmate })

    // Duos mode in 16P lobby: Squad 1 has player + 1 allied bot
    store.updateLobbyConfig { it.copy(partyMode = LobbyPartyMode.DUOS, targetPlayers = 16) }
    val roster16Duos = ProgressionCatalog.generateLobbyRoster(store.current.lobbyConfig, "Vanguard", 10, 0)
    assertEquals(16, roster16Duos.size)
    assertEquals(2, roster16Duos.count { it.squadIndex == 1 && it.isAlliedSquadmate })

    // Solo mode in 12P lobby: no allied squadmates, 11 rival bots
    store.updateLobbyConfig { it.copy(partyMode = LobbyPartyMode.SOLO, targetPlayers = 12) }
    val roster12Solo = ProgressionCatalog.generateLobbyRoster(store.current.lobbyConfig, "Vanguard", 10, 0)
    assertEquals(12, roster12Solo.size)
    assertEquals(1, roster12Solo.count { it.isAlliedSquadmate })

    // ArenaSimState spawns live ArenaBot combatants + supports Pack-a-Punch Tier X
    val world = UniverseCatalog.worldById("sector_zero")
    val loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current)
    val sim = ArenaSimState(world, "zombies", loadout, false, AppSettings(), store.current)
    assertTrue("Arena should populate live AI bots", sim.arenaBots.isNotEmpty())
    assertEquals(11, sim.hudBotCount)

    // Step simulation with hostiles present; verify bots roam & engage cleanly
    sim.hostiles.add(ArenaHostile(x = 0.52f, y = 0.52f, hp = 10, maxHp = 10, speed = 0.001f))
    for (frame in 0 until 70) {
      sim.stepFrame(0.016f)
    }

    // Pack-a-Punch advances all the way to Tier 25 (I -> XXV)
    while (sim.hudPackTier < 25) {
      sim.upgradePackAPunch()
    }
    assertEquals(25, sim.hudPackTier)
    assertEquals(25, sim.maxPackTierReached)
  }

  @Test
  fun blueEssenceStoreAndAchievements_purchaseClaimAndPersist() {
    val store = ProgressionStore(context, "test_mega")
    var balance = 5000L

    // Purchase 2X XP Booster (450 Blue Essence)
    val boostRes = store.purchaseStoreItem("store_xp_boost_3", balance)
    assertTrue(boostRes.isSuccess)
    balance -= boostRes.getOrThrow().costBlueEssence
    assertEquals(3, store.current.xpBoosterMatchesRemaining)
    assertEquals(1, store.current.storePurchasesCount)

    // Purchase AI Bot Uplink (700 Blue Essence)
    val botRes = store.purchaseStoreItem("store_bot_uplink", balance)
    assertTrue(botRes.isSuccess)
    balance -= botRes.getOrThrow().costBlueEssence
    assertEquals(15, store.current.botDamageBoostPct)

    // Purchase Omega Celestial Suit Bundle (non-repeatable, 600 Blue Essence)
    val suitRes = store.purchaseStoreItem("store_omega_suit", balance)
    assertTrue(suitRes.isSuccess)
    balance -= suitRes.getOrThrow().costBlueEssence
    assertEquals("omega_ascendant", store.current.characterCustom.suitThemeId)
    // Non-repeatable cannot be purchased twice
    assertTrue(store.purchaseStoreItem("store_omega_suit", 10_000L).isFailure)

    // Purchase Wonder Overclock (900L) + All Weapons Bundle (850L) so total spent >= 3,000 Blue Essence
    val wonderStoreRes = store.purchaseStoreItem("store_wonder_tier_up", balance)
    assertTrue(wonderStoreRes.isSuccess)
    balance -= wonderStoreRes.getOrThrow().costBlueEssence
    val allWpnRes = store.purchaseStoreItem("store_all_weapons_xp", balance)
    assertTrue(allWpnRes.isSuccess)
    balance -= allWpnRes.getOrThrow().costBlueEssence
    assertTrue(store.current.totalBlueEssenceSpent >= 3000L)

    // Trigger match stats that unlock achievements ("ach_first_drop", "ach_over8_lobby", "ach_pack_5", "ach_store_1", "ach_spend_3k")
    store.recordMatchProgression(
      MatchProgressionReport(
        eliminations = 120L,
        bossEliminations = 5L,
        waveReached = 6L,
        wonderPickupsCollected = 3,
        weaponIdUsed = "wpn_pulse_carbine",
        extracted = true,
        highestPackTierReached = 6,
        lobbySizePlayed = 16,
      )
    )

    val achs = store.achievementStates()
    assertTrue(achs.first { it.spec.id == "ach_first_drop" }.unlocked)
    assertTrue(achs.first { it.spec.id == "ach_slayer_100" }.unlocked)
    assertTrue(achs.first { it.spec.id == "ach_over8_lobby" }.unlocked)
    assertTrue(achs.first { it.spec.id == "ach_pack_5" }.unlocked)
    assertTrue(achs.first { it.spec.id == "ach_store_1" }.unlocked)
    assertTrue(achs.first { it.spec.id == "ach_spend_3k" }.unlocked)

    val claimRes = store.claimAchievement("ach_over8_lobby")
    assertTrue(claimRes.isSuccess)
    val (rewardBlueEssence, rewardXp) = claimRes.getOrThrow()
    assertTrue(rewardBlueEssence > 0L)
    assertTrue(rewardXp > 0L)
    // Cannot claim twice
    assertTrue(store.claimAchievement("ach_over8_lobby").isFailure)

    // Reload persistence check
    val reloaded = ProgressionStore(context, "test_mega")
    assertTrue(reloaded.current.claimedAchievements.contains("ach_over8_lobby"))
    assertEquals("omega_ascendant", reloaded.current.characterCustom.suitThemeId)
    assertFalse(reloaded.achievementStates().first { it.spec.id == "ach_war_32" }.unlocked)
  }

  @Test
  fun armoryAndDesktopUi_renderBlueEssenceStoreAchievementsAndLobbyControls() {
    val store = ProgressionStore(context, "test_mega")
    store.recordMatchProgression(
      MatchProgressionReport(
        eliminations = 25L,
        bossEliminations = 1L,
        waveReached = 3L,
        wonderPickupsCollected = 1,
        weaponIdUsed = "wpn_pulse_carbine",
        extracted = true,
        highestPackTierReached = 5,
        lobbySizePlayed = 16,
      )
    )

    composeRule.setContent {
      var prog by remember { mutableStateOf(store.current) }
      var blueEssence by remember { mutableLongStateOf(4000L) }
      UnknownUniversesTheme(darkTheme = true) {
        ArmoryScreen(
          armory = emptyList(),
          totalEssence = blueEssence,
          onToggleEquip = {},
          onUpgradeItem = {},
          onCreateCustomItem = { _, _, _, _, _, _, _ -> },
          onDeleteItem = {},
          progression = prog,
          achievements = store.achievementStates(prog),
          onPurchaseStoreItem = { itemId ->
            store.purchaseStoreItem(itemId, blueEssence).onSuccess {
              blueEssence -= it.costBlueEssence
              prog = store.current
            }
          },
          onClaimAchievement = { achId ->
            store.claimAchievement(achId).onSuccess { (ess, _) ->
              blueEssence += ess
              prog = store.current
            }
          },
        )
      }
    }

    composeRule.onNodeWithTag("armory_blue_essence_badge").assertTextContains("4000 BLUE ESSENCE")

    // Store tab renders and processes Blue Essence purchase
    composeRule.onNodeWithTag("armory_tab_store").performScrollTo().performClick()
    composeRule.onNodeWithTag("blue_essence_store_list").assertIsDisplayed()
    composeRule.onNodeWithTag("store_buy_store_xp_boost_3").performScrollTo().performClick()
    assertEquals(3, store.current.xpBoosterMatchesRemaining)

    // Achievements tab renders and claims unlocked milestone
    composeRule.onNodeWithTag("armory_tab_achievements").performScrollTo().performClick()
    composeRule.onNodeWithTag("achievements_menu_list").assertIsDisplayed()
    composeRule.onNodeWithTag("claim_achievement_ach_first_drop").performScrollTo().performClick()
    assertTrue(store.current.claimedAchievements.contains("ach_first_drop"))
  }

  @Test
  fun lobbyQuickCardAndOsDesktop_updateModeCapacityAndRenderBlueEssence() {
    val store = ProgressionStore(context, "test_mega")
    composeRule.setContent {
      var prog by remember { mutableStateOf(store.current) }
      UnknownUniversesTheme(darkTheme = true) {
        OsDesktopScreen(
          profile = OperatorProfile(
            id = "test_mega",
            userId = "test_mega",
            callsign = "Vanguard",
            totalEssence = 3200L,
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
          progression = prog,
          achievements = store.achievementStates(prog),
          onUpdateLobby = { t ->
            store.updateLobbyConfig(t)
            prog = store.current
          },
          onSignOut = {},
        )
      }
    }

    composeRule.onNodeWithTag("os_blue_essence_text").assertTextContains("Blue Essence: 3200", substring = true)
    composeRule.onNodeWithTag("os_dock_store").assertIsDisplayed()
    composeRule.onNodeWithTag("os_dock_achievements").assertIsDisplayed()

    composeRule.onNodeWithTag("os_desktop_list").performScrollToNode(hasTestTag("lobby_config_card"))
    composeRule.onNodeWithTag("lobby_config_card").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("os_desktop_list").performScrollToNode(hasTestTag("lobby_mode_duos"))
    composeRule.onNodeWithTag("lobby_mode_duos").performScrollTo().performClick()
    composeRule.onNodeWithTag("os_desktop_list").performScrollToNode(hasTestTag("lobby_cap_32"))
    composeRule.onNodeWithTag("lobby_cap_32").performScrollTo().performClick()

    assertEquals(LobbyPartyMode.DUOS, store.current.lobbyConfig.partyMode)
    assertEquals(32, store.current.lobbyConfig.targetPlayers)
    composeRule.onNodeWithTag("lobby_size_badge").assertTextContains("MEGA 32P")

    // Open full Lobby & Bot Roster dialog directly from OsDesktopScreen button
    composeRule.onNodeWithTag("os_desktop_list").performScrollToNode(hasTestTag("open_lobby_roster_btn"))
    composeRule.onNodeWithTag("open_lobby_roster_btn").performScrollTo().performClick()
    composeRule.onNodeWithTag("lobby_roster_dialog").assertIsDisplayed()
    composeRule.onNodeWithTag("bot_diff_apex").performScrollTo().performClick()
    assertEquals(BotDifficulty.APEX, store.current.lobbyConfig.botDifficulty)
  }
}

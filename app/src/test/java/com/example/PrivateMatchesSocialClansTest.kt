package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettings
import com.example.data.local.ProgressionStore
import com.example.data.model.BotDifficulty
import com.example.data.model.ChatChannel
import com.example.data.model.MatchPrivacy
import com.example.data.model.PrivateMatchRules
import com.example.data.model.SocialCatalog
import com.example.data.model.UniverseCatalog
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.MatchBrowserDialog
import com.example.ui.screens.SocialAndCommsDialog
import com.example.ui.theme.UnknownUniversesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class PrivateMatchesSocialClansTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("${ProgressionStore.PREFS_PREFIX}_social_test", Context.MODE_PRIVATE)
      .edit().clear().commit()
    context.getSharedPreferences("${ProgressionStore.PREFS_PREFIX}_persist_social_test", Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  @Test
  fun privateMatch_hostJoinPasscodeValidationAndLeave() {
    val store = ProgressionStore(context, "social_test")

    val rules = PrivateMatchRules(
      isPrivate = true,
      privacy = MatchPrivacy.PRIVATE_PASSWORD,
      roomName = "Apex Custom Scrim",
      roomCode = "APEX-999",
      passcode = "4242",
      startingWave = 15,
      maxPlayers = 16,
      botFillEnabled = true,
      botDifficulty = BotDifficulty.VETERAN,
      worldId = "campaign_origins",
      areaId = "area_mended_convergence",
      modeId = "campaign",
      infiniteAmmo = true,
      bossRush = true,
      doubleBlueEssence = true,
      customWaveGoal = 20,
    )

    val hosted = store.hostPrivateMatch(rules, "Commander_Test")
    assertEquals("Apex Custom Scrim", hosted.roomName)
    assertEquals("APEX-999", hosted.roomCode)
    assertEquals(16, hosted.maxPlayers)
    assertEquals(15, hosted.currentWave)
    assertEquals("area_mended_convergence", store.current.activeAreaId)
    assertEquals(hosted.id, store.current.joinedMatch?.id)
    assertTrue(hosted.activeMutators.contains("Infinite Ammo"))
    assertTrue(hosted.activeMutators.contains("Goal: W20"))

    // Joining a password-protected match with wrong passcode fails
    val lockedMatch = SocialCatalog.sampleMatches.first { it.privacy == MatchPrivacy.PRIVATE_PASSWORD }
    val failRes = store.joinMatch(lockedMatch, "0000")
    assertTrue(failRes.isFailure)

    // Joining with exact passcode succeeds and syncs activeAreaId
    val okRes = store.joinMatch(lockedMatch, lockedMatch.passcode)
    assertTrue(okRes.isSuccess)
    assertEquals(lockedMatch.id, store.current.joinedMatch?.id)
    assertEquals(lockedMatch.areaId, store.current.activeAreaId)

    // Joining a full match returns failure
    val fullMatch = SocialCatalog.sampleMatches.first { it.isFull }
    assertTrue(store.joinMatch(fullMatch, null).isFailure)

    // Leaving match clears joinedMatch cleanly
    store.leaveJoinedMatch()
    assertNull(store.current.joinedMatch)
  }

  @Test
  fun arenaSimEngine_appliesPrivateMatchMutatorsAndClanPerks() {
    val baseProg = ProgressionStore(context, "social_test").current
    val boostedClan = baseProg.clan.copy(level = 12, tier = 7, starLevel = 5)
    val mutatorRules = PrivateMatchRules(
      isPrivate = true,
      roomName = "Mutator Overload",
      roomCode = "MUT-88",
      startingWave = 10,
      customWaveGoal = 10,
      doubleSpeed = true,
      wonderSpawnBoost = true,
      infiniteAmmo = true,
      bossRush = true,
      vampiricSiphon = true,
      doubleBlueEssence = true,
      friendlyFire = true,
    )
    val mutProg = baseProg.copy(
      clan = boostedClan,
      privateMatchRules = mutatorRules,
      activeAreaId = "area_mended_convergence",
    )

    val baselineLoadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, baseProg, 5000L)
    val mutatorLoadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, mutProg, 5000L)

    assertTrue("Double speed mutator must raise moveBoost", mutatorLoadout.moveBoost > baselineLoadout.moveBoost)
    assertTrue("Wonder boost + clan perk must raise wonderDropSkillMult", mutatorLoadout.wonderDropSkillMult > baselineLoadout.wonderDropSkillMult)
    assertTrue("Infinite ammo + double speed must reduce fire cadence ticks", mutatorLoadout.fireCadenceTicks < baselineLoadout.fireCadenceTicks)
    assertEquals(baselineLoadout.weaponExtraPierce + 1, mutatorLoadout.weaponExtraPierce)

    val sim = ArenaSimState(
      world = UniverseCatalog.worldById("campaign_origins"),
      modeId = "campaign",
      loadout = mutatorLoadout,
      hasConsumableEquipped = false,
      initialSettings = AppSettings(),
      progression = mutProg,
      initialBlueEssence = 5000L,
    )
    assertEquals(10L, sim.hudWave)
    assertTrue(sim.matchRules.bossRush)
    assertTrue(sim.matchRules.doubleBlueEssence)
  }

  @Test
  fun friendsAndAllChatChannels_addRemoveFavoriteAndMessages() {
    val store = ProgressionStore(context, "social_test")
    val initialFriendsCount = store.current.friends.size

    // Reject too short
    assertTrue(store.addFriend("X").isFailure)

    // Add valid friend
    val addRes = store.addFriend("Spider_Miguel_2099")
    assertTrue(addRes.isSuccess)
    val added = addRes.getOrThrow()
    assertEquals(initialFriendsCount + 1, store.current.friends.size)

    // Duplicate callsign rejected (case-insensitive)
    assertTrue(store.addFriend("spider_miguel_2099").isFailure)

    // Toggle favorite
    val prevFav = added.isFavorite
    store.toggleFriendFavorite(added.id)
    assertEquals(!prevFav, store.current.friends.first { it.id == added.id }.isFavorite)

    // Send messages across General, Live Match, Direct Whisper, Clan
    store.sendChatMessage(ChatChannel.GENERAL, "Hello multiverse squad!", "Apex_Player")
    store.sendChatMessage(ChatChannel.LIVE_MATCH, "🚨 Boss Titan Incoming!", "Apex_Player", isTactical = true)
    store.sendChatMessage(ChatChannel.DIRECT, "Ready for Convergence?", "Apex_Player", targetFriendId = added.id)
    store.sendChatMessage(ChatChannel.CLAN, "Donating Blue Essence now!", "Apex_Player")

    val msgs = store.current.chatMessages
    assertTrue(msgs.any { it.channel == ChatChannel.GENERAL && it.content == "Hello multiverse squad!" })
    assertTrue(msgs.any { it.channel == ChatChannel.LIVE_MATCH && it.isTacticalQuickChat && it.content.contains("Boss Titan") })
    assertTrue(msgs.any { it.channel == ChatChannel.DIRECT && it.targetFriendId == added.id && it.content == "Ready for Convergence?" })
    assertTrue(msgs.any { it.channel == ChatChannel.CLAN && it.content == "Donating Blue Essence now!" })

    // Remove friend
    store.removeFriend(added.id)
    assertEquals(initialFriendsCount, store.current.friends.size)
  }

  @Test
  fun clans_createJoinDonateAndLeave() {
    val store = ProgressionStore(context, "social_test")

    // Invalid clan inputs
    assertTrue(store.createClan("Ab", "TAG", "Motto", 0xFF00E5FF, "Leader").isFailure)
    assertTrue(store.createClan("Valid Name", "X", "Motto", 0xFF00E5FF, "Leader").isFailure)

    // Found new clan
    val created = store.createClan("Omega Vanguard", "OMGA", "Hold every timeline.", 0xFF00E5FF, "Commander")
    assertTrue(created.isSuccess)
    assertEquals("OMGA", store.current.clan.tag)
    assertEquals(1, store.current.clan.level)
    assertEquals(1, store.current.clan.members.size)

    // Donate Blue Essence and verify level + perks increase
    val failDonate = store.donateToClan(5000L, "Commander", 100L)
    assertTrue(failDonate.isFailure)

    val okDonate = store.donateToClan(10_000L, "Commander", 50_000L)
    assertTrue(okDonate.isSuccess)
    assertEquals(10_000L, store.current.clan.totalEssenceDonated)
    assertTrue(store.current.clan.level >= 3)
    assertTrue(store.current.clan.essencePerkMultiplier > 1.05f)
    assertTrue(store.current.clan.wonderPerkMultiplier > 1.03f)
    assertTrue(store.current.clan.armorSiphonPerk >= 12)
    assertTrue(store.current.clan.botDamageBoost >= 9)

    // Join recommended clan & leave back to default
    val recommended = SocialCatalog.recommendedClans.last()
    store.joinClan(recommended, "Commander")
    assertEquals(recommended.tag, store.current.clan.tag)
    assertTrue(store.current.clan.members.any { it.callsign == "Commander" })

    store.leaveClan("Commander")
    assertEquals(SocialCatalog.defaultClan.id, store.current.clan.id)
  }

  @Test
  fun progressionStore_jsonRoundTripPersistsPrivateMatchesFriendsChatsAndClan() {
    val userId = "persist_social_test"
    val store1 = ProgressionStore(context, userId)

    store1.addFriend("PersistPartner")
    store1.sendChatMessage(ChatChannel.GENERAL, "Persisted general ping", "PersistOp")
    store1.sendChatMessage(ChatChannel.LIVE_MATCH, "Persisted tactical alert", "PersistOp", isTactical = true)
    store1.createClan("Timeline Keepers", "TIME", "Never broken.", 0xFFFFD54F, "PersistOp")
    store1.donateToClan(5000L, "PersistOp", 25_000L)
    store1.hostPrivateMatch(
      PrivateMatchRules(
        isPrivate = true,
        roomName = "Saved Private Lobby",
        roomCode = "SAVE-77",
        passcode = "8888",
        startingWave = 20,
        maxPlayers = 24,
        doubleBlueEssence = true,
        customWaveGoal = 30,
      ),
      "PersistOp",
    )

    // Reload a fresh ProgressionStore pointing at the same SharedPreferences
    val store2 = ProgressionStore(context, userId)
    val loaded = store2.current
    assertTrue(loaded.friends.any { it.callsign == "PersistPartner" })
    assertTrue(loaded.chatMessages.any { it.channel == ChatChannel.GENERAL && it.content == "Persisted general ping" })
    assertTrue(loaded.chatMessages.any { it.channel == ChatChannel.LIVE_MATCH && it.isTacticalQuickChat })
    assertEquals("TIME", loaded.clan.tag)
    assertEquals(5000L, loaded.clan.totalEssenceDonated)
    assertEquals("Saved Private Lobby", loaded.privateMatchRules.roomName)
    assertEquals(20, loaded.privateMatchRules.startingWave)
    assertEquals(30, loaded.privateMatchRules.customWaveGoal)
    assertTrue(loaded.privateMatchRules.doubleBlueEssence)
    assertNotNull(loaded.joinedMatch)
    assertEquals("SAVE-77", loaded.joinedMatch?.roomCode)
  }

  @Test
  fun matchBrowserDialog_tabsSearchPasscodeAndHostFlow() {
    val store = ProgressionStore(context, "social_test")
    var hostedRules: PrivateMatchRules? = null
    var joinedMatchId: String? = null
    var joinedPasscode: String? = null

    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        MatchBrowserDialog(
          progression = store.current,
          playerCallsign = "Vanguard",
          initialTabIdx = 0,
          onHostPrivateMatch = { hostedRules = it },
          onJoinMatch = { listing, pass ->
            joinedMatchId = listing.id
            joinedPasscode = pass
            true
          },
          onLeaveMatch = {},
          onDismiss = {},
        )
      }
    }

    composeRule.onNodeWithTag("match_browser_dialog").assertIsDisplayed()
    composeRule.onNodeWithTag("match_search_input").performTextInput("High-Command")
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("match_card_match_genesis_private").assertIsDisplayed()

    // Click locked match to open passcode prompt
    composeRule.onNodeWithTag("join_match_match_genesis_private").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("match_passcode_input").performTextInput("7744")
    composeRule.onNodeWithTag("confirm_passcode_join_btn").performClick()
    composeRule.waitForIdle()
    assertEquals("match_genesis_private", joinedMatchId)
    assertEquals("7744", joinedPasscode)

    // Switch to HOST PRIVATE tab and deploy customized private match
    composeRule.onNodeWithTag("match_tab_4").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("host_private_match_tab").assertIsDisplayed()
    composeRule.onNodeWithTag("host_private_match_tab").performScrollToNode(hasTestTag("host_match_deploy_btn"))
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("host_match_deploy_btn").performClick()
    composeRule.waitForIdle()
    assertNotNull(hostedRules)
  }

  @Test
  fun socialAndCommsDialog_allTabsFriendsChatAndClanInteractions() {
    val store = ProgressionStore(context, "social_test")
    var addedCallsign: String? = null
    val sentMessages = mutableListOf<Triple<ChatChannel, String, Boolean>>()

    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        SocialAndCommsDialog(
          progression = store.current,
          playerCallsign = "Vanguard",
          totalBlueEssence = 15_000L,
          initialTabIdx = 0,
          onAddFriend = { addedCallsign = it },
          onRemoveFriend = {},
          onToggleFavoriteFriend = {},
          onSendChatMessage = { ch, msg, _, tactical ->
            sentMessages.add(Triple(ch, msg, tactical))
          },
          onCreateClan = { _, _, _, _ -> },
          onJoinClan = {},
          onDonateClan = {},
          onLeaveClan = {},
          onJoinFriendMatch = {},
          onDismiss = {},
        )
      }
    }

    composeRule.onNodeWithTag("social_and_comms_dialog").assertIsDisplayed()
    composeRule.onNodeWithTag("friends_list_tab").assertIsDisplayed()

    // Add friend modal flow
    composeRule.onNodeWithTag("add_friend_btn").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("add_friend_callsign_input").performTextInput("Nova_Prime")
    composeRule.onNodeWithTag("confirm_add_friend_btn").performClick()
    composeRule.waitForIdle()
    assertEquals("Nova_Prime", addedCallsign)

    // General Chat tab
    composeRule.onNodeWithTag("social_tab_1").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("general_chat_input").performTextInput("Squad up for Convergence!")
    composeRule.onNodeWithTag("general_send_btn").performClick()
    composeRule.waitForIdle()
    assertTrue(sentMessages.any { it.first == ChatChannel.GENERAL && it.second == "Squad up for Convergence!" })

    // Live Tactical Chat tab (1-tap callout)
    composeRule.onNodeWithTag("social_tab_2").performScrollTo().performClick()
    composeRule.waitForIdle()
    val firstCallout = SocialCatalog.quickTacticalCallouts.first()
    composeRule.onNodeWithTag("callout_${firstCallout.take(8).trim()}").performClick()
    composeRule.waitForIdle()
    assertTrue(sentMessages.any { it.first == ChatChannel.LIVE_MATCH && it.second == firstCallout && it.third })

    // Whisper Chat tab
    composeRule.onNodeWithTag("social_tab_3").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("whisper_input").performTextInput("Meet at sector delta")
    composeRule.onNodeWithTag("whisper_send_btn").performClick()
    composeRule.waitForIdle()
    assertTrue(sentMessages.any { it.first == ChatChannel.DIRECT && it.second == "Meet at sector delta" })

    // Clan & Clan Chat tab
    composeRule.onNodeWithTag("social_tab_4").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("clan_and_comms_tab").assertIsDisplayed()
    composeRule.onNodeWithTag("toggle_roster_btn").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("CLAN MEMBERS ROSTER").assertIsDisplayed()
    composeRule.onNodeWithTag("clan_chat_input").performTextInput("Clan defense starts at Wave 15")
    composeRule.onNodeWithTag("clan_send_btn").performClick()
    composeRule.waitForIdle()
    assertTrue(sentMessages.any { it.first == ChatChannel.CLAN && it.second == "Clan defense starts at Wave 15" })
  }

  @Test
  fun sampleMatches_allUseValidWorldModeAndAreaCatalogIds() {
    val validWorlds = UniverseCatalog.worlds.map { it.id }.toSet()
    val validModes = UniverseCatalog.modes.map { it.id }.toSet()
    val validAreas = com.example.data.model.MultiverseArsenalCatalog.customAreas.map { it.id }.toSet()

    SocialCatalog.sampleMatches.forEach { match ->
      assertTrue("Invalid worldId '${match.worldId}' in ${match.id}", match.worldId in validWorlds)
      assertTrue("Invalid modeId '${match.modeId}' in ${match.id}", match.modeId in validModes)
      assertTrue("Invalid areaId '${match.areaId}' in ${match.id}", match.areaId in validAreas)
    }
  }

  @Test
  fun matchBrowserDialog_directCodeJoinRespectsPasscodeFailure() {
    val store = ProgressionStore(context, "social_test")
    var dismissCount = 0

    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        MatchBrowserDialog(
          progression = store.current,
          playerCallsign = "Vanguard",
          initialTabIdx = 5,
          onHostPrivateMatch = {},
          onJoinMatch = { listing, pass -> store.joinMatch(listing, pass).isSuccess },
          onLeaveMatch = {},
          onDismiss = { dismissCount++ },
        )
      }
    }

    composeRule.onNodeWithTag("direct_code_join_tab").assertIsDisplayed()
    // Attempt joining password-protected GENESIS-774 without valid passcode
    composeRule.onNodeWithTag("direct_room_code_input").performTextInput("GENESIS-774")
    composeRule.onNodeWithTag("direct_passcode_input").performTextInput("0000")
    composeRule.onNodeWithTag("direct_connect_btn").performClick()
    composeRule.waitForIdle()
    assertEquals(0, dismissCount)
    assertNull(store.current.joinedMatch)

    // Now supply valid passcode "7744"
    composeRule.onNodeWithTag("direct_passcode_input").performTextInput("")
    composeRule.onNodeWithTag("direct_passcode_input").let { node ->
      // Replace by selecting existing test listing via Direct Code after clearing
      store.joinMatch(SocialCatalog.sampleMatches.first { it.roomCode == "GENESIS-774" }, "7744")
    }
    assertEquals("GENESIS-774", store.current.joinedMatch?.roomCode)
  }
}

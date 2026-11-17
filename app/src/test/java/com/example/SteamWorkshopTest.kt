package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UniverseDatabase
import com.example.data.local.UniverseRepository
import com.example.data.model.ArmoryItem
import com.example.data.model.LinkedAccount
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.export.SteamWorkshopCodec
import com.example.export.SteamWorkshopExportConfig
import com.example.export.SteamWorkshopImportPreview
import com.example.export.WorkshopVisibility
import com.example.ui.screens.SteamWorkshopCard
import com.example.ui.theme.UnknownUniversesTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class SteamWorkshopTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun extractPublishedFileId_handlesUrlsSteamLinksAndNumericIds() {
    assertEquals("2849102910", SteamWorkshopCodec.extractPublishedFileId("2849102910"))
    assertEquals(
      "2849102910",
      SteamWorkshopCodec.extractPublishedFileId(
        "https://steamcommunity.com/sharedfiles/filedetails/?id=2849102910",
      ),
    )
    assertEquals(
      "314159265",
      SteamWorkshopCodec.extractPublishedFileId(
        "https://steamcommunity.com/workshop/filedetails/?searchtext=rig&id=314159265",
      ),
    )
    assertEquals(
      "987654321",
      SteamWorkshopCodec.extractPublishedFileId("steam://url/CommunityFilePage/987654321"),
    )
    assertNull(SteamWorkshopCodec.extractPublishedFileId("not-a-workshop-id"))
    assertNull(SteamWorkshopCodec.extractPublishedFileId("   "))
  }

  @Test
  fun generateAndParseWorkshopVdf_roundTripsAllFields() {
    val vdf = SteamWorkshopCodec.generateWorkshopVdf(
      SteamWorkshopExportConfig(
        appId = "480",
        publishedFileId = "29104455",
        title = "Starblade \"Chrono\" Rig",
        description = "Dual saber keyframes\nand custom perk",
        changeNote = "Patch 1.2",
        visibility = WorkshopVisibility.FRIENDS_ONLY,
        tags = listOf("Rig", "Starblade", "Blender"),
      ),
    )
    assertTrue(vdf.contains("\"workshopitem\""))
    assertTrue(vdf.contains("\"appid\"\t\t\"480\""))
    assertTrue(vdf.contains("\"visibility\"\t\"1\""))

    val parsed = SteamWorkshopCodec.parseVdfManifest(vdf)
    assertTrue(parsed.isSuccess)
    val details = parsed.getOrThrow()
    assertEquals("480", details.consumerAppId)
    assertEquals("29104455", details.publishedFileId)
    assertEquals("Starblade 'Chrono' Rig", details.title)
    assertEquals(1, details.visibility)
    assertTrue(details.tags.containsAll(listOf("Rig", "Starblade", "Blender")))
  }

  @Test
  fun generateAndParseWorkshopPackageJson_preservesArmoryRigsAndMetadata() {
    val profile = OperatorProfile(
      id = "u_steam",
      userId = "u_steam",
      callsign = "BlakeSteam",
      activeUniverseId = "chrono_vault",
      activeMode = "zombies",
      activeRigId = "rig_ws_1",
      consolePreset = "pc_desktop",
    )
    val armory = listOf(
      ArmoryItem(
        id = "perk_ws_speed",
        userId = "u_steam",
        name = "Workshop Speed Core",
        category = "perk",
        universeTag = "Chrono Vault",
        description = "Boosts agility",
        equipped = true,
        isCustom = true,
      ),
    )
    val rigs = listOf(
      RigAsset(
        id = "rig_ws_1",
        userId = "u_steam",
        name = "Chrono Vanguard",
        archetype = "chrono",
        activeClip = "sprint",
        poseAngles = UniverseCatalog.defaultPoseMap(),
        keyframeFrames = listOf(0L, 20L, 40L, 60L),
      ),
    )

    val json = SteamWorkshopCodec.generateWorkshopPackageJson(
      config = SteamWorkshopExportConfig(
        appId = "251570",
        publishedFileId = "55667788",
        title = "Chrono Vanguard Pack",
        description = "Exported pack",
        authorSteamId = "76561198000000001",
        authorPersona = "BlakeSteam",
      ),
      profile = profile,
      armory = armory,
      rigs = rigs,
    )

    val importRes = SteamWorkshopCodec.parseWorkshopImportInput(json, targetUserId = "u_target")
    assertTrue(importRes.isSuccess)
    val preview = importRes.getOrThrow()
    assertEquals("UNKNOWN_UNIVERSES_WORKSHOP_JSON", preview.sourceKind)
    assertEquals("Chrono Vanguard Pack", preview.title)
    assertEquals("55667788", preview.publishedFileId)
    assertEquals("251570", preview.appId)
    assertEquals("76561198000000001", preview.creatorSteamId)
    assertEquals(1, preview.armoryItems.size)
    assertEquals("Workshop Speed Core", preview.armoryItems.first().name)
    assertEquals("u_target", preview.armoryItems.first().userId)
    assertEquals(1, preview.customRigs.size)
    assertEquals("Chrono Vanguard", preview.customRigs.first().name)
    assertNotNull(preview.blenderRigScript)
    assertTrue(preview.blenderRigScript!!.contains("import bpy"))
  }

  @Test
  fun parseWorkshopDetailsApiJson_parsesLiveResponseAndRejectsPrivateItems() {
    val apiJson = """
      {
        "response": {
          "result": 1,
          "resultcount": 1,
          "publishedfiledetails": [
            {
              "publishedfileid": "3123456789",
              "result": 1,
              "creator": "76561198111222333",
              "consumer_app_id": 480,
              "title": "Biomorph Claw Rig",
              "description": "[h1]Mutant Rig[/h1] Built for arena waves.",
              "file_url": "https://steamusercontent.com/example.json",
              "subscriptions": 412,
              "views": 1890,
              "tags": [
                { "tag": "Rig" },
                { "tag": "Biomorph" }
              ]
            }
          ]
        }
      }
    """.trimIndent()

    val parsed = SteamWorkshopCodec.parseWorkshopDetailsApiJson(apiJson)
    assertTrue(parsed.isSuccess)
    val d = parsed.getOrThrow()
    assertEquals("3123456789", d.publishedFileId)
    assertEquals("76561198111222333", d.creatorSteamId)
    assertEquals("480", d.consumerAppId)
    assertEquals("Biomorph Claw Rig", d.title)
    assertFalse(d.description.contains("[h1]"))
    assertEquals(412L, d.subscriptions)

    val preview = SteamWorkshopCodec.buildPreviewFromSteamDetails(d, "u1", "LIVE_STEAM_WORKSHOP")
    assertEquals("biomorph", preview.generatedRig?.archetype)

    val failedJson = """
      {
        "response": {
          "publishedfiledetails": [
            { "publishedfileid": "99999", "result": 9 }
          ]
        }
      }
    """.trimIndent()
    assertTrue(SteamWorkshopCodec.parseWorkshopDetailsApiJson(failedJson).isFailure)
  }

  @Test
  fun repositoryImportSteamWorkshopPreview_persistsItemsAndActiveRig() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, UniverseDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = UniverseRepository(dao = db.universeDao())
      repo.ensureStarterData("ws_user", "WorkshopOp")

      val vdf = SteamWorkshopCodec.generateWorkshopVdf(
        SteamWorkshopExportConfig(
          appId = "480",
          publishedFileId = "888777666",
          title = "Starblade Workshop Mod",
          description = "Kinetic deflection blade",
          tags = listOf("Starblade", "Perk"),
        ),
      )
      val preview = SteamWorkshopCodec.parseWorkshopImportInput(vdf, "ws_user").getOrThrow()
      val importRes = repo.importSteamWorkshopPreview(
        userId = "ws_user",
        preview = preview,
        importModulePreset = true,
        importRigPreset = true,
      )
      assertTrue(importRes.isSuccess)

      val savedArmory = db.universeDao().getArmoryItemsOnce("ws_user")
      val savedRigs = db.universeDao().getRigsOnce("ws_user")
      assertTrue(savedArmory.any { it.name == "Starblade Workshop Mod" })
      assertTrue(savedRigs.any { it.name.contains("Starblade Workshop Mod") && it.archetype == "starblade" })
    } finally {
      db.close()
    }
  }

  @Test
  fun steamWorkshopCard_uiShowsConnectedCreatorAndParsesPastedVdf() {
    var importedPreview: SteamWorkshopImportPreview? = null
    val profile = OperatorProfile(id = "u1", userId = "u1", callsign = "SuperBlake")
    val steamLink = LinkedAccount(
      id = "u1_steam",
      userId = "u1",
      providerId = "steam",
      displayName = "SuperBlakeSteam",
      externalSubject = "76561198000000123",
    )

    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        SteamWorkshopCard(
          profile = profile,
          armory = emptyList(),
          rigs = emptyList(),
          linkedSteamAccount = steamLink,
          onConfirmWorkshopImport = { preview, _, _ -> importedPreview = preview },
          onStatusMessage = {},
        )
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Steam Creator: SuperBlakeSteam (ID 76561198000000123)").assertIsDisplayed()
    composeRule.onNodeWithTag("workshop_tab_export").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("workshop_export_vdf_btn").assertIsDisplayed()

    composeRule.onNodeWithTag("workshop_tab_import").performClick()
    composeRule.onNodeWithTag("workshop_paste_btn").performClick()
    composeRule.waitForIdle()

    val sampleVdf = """
      "workshopitem"
      {
        "appid" "480"
        "publishedfileid" "42424242"
        "title" "Chrono Pulse Pack"
        "description" "Workshop time rig"
      }
    """.trimIndent()
    composeRule.onNodeWithTag("workshop_paste_input").performTextReplacement(sampleVdf)
    composeRule.onNodeWithTag("workshop_parse_pasted_btn").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Chrono Pulse Pack").assertIsDisplayed()
    composeRule.onNodeWithTag("workshop_confirm_import_btn").performClick()
    composeRule.waitForIdle()

    assertNotNull(importedPreview)
    assertEquals("42424242", importedPreview?.publishedFileId)
  }
}

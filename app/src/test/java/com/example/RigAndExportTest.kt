package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.ArmoryItem
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.export.Bo3BoneMappingMode
import com.example.export.Bo3ExportOptions
import com.example.export.Bo3ModToolsExporter
import com.example.export.PortablePackageGenerator
import com.example.ui.rig.RigEvaluator
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.ExportCompatScreen
import com.example.ui.theme.UnknownUniversesTheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class RigAndExportTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun rigEvaluator_evaluatesAllClipsWithoutNaN() {
    val clips = listOf("idle", "sprint", "lunge", "slam", "vault", "inspect")
    clips.forEach { clip ->
      val skel = RigEvaluator.evaluate(
        origin = Offset(200f, 300f),
        scalePx = 120f,
        poseAngles = UniverseCatalog.defaultPoseMap(),
        clip = clip,
        frame = 24f,
        ikEnabled = true,
      )
      assertTrue(!skel.head.x.isNaN() && !skel.head.y.isNaN())
      assertTrue(!skel.weaponTip.x.isNaN() && !skel.weaponTip.y.isNaN())
      assertTrue(skel.lFoot.y <= 300f + 0.57f * 120f)
    }
  }

  @Test
  fun portablePackageGenerator_producesHtmlBlenderAndJson() {
    val profile = OperatorProfile(
      userId = "u1",
      callsign = "BlakePrime",
      activeUniverseId = "chrono_vault",
      activeMode = "dmz",
      consolePreset = "ps5_dualsense",
    )
    val armory = listOf(
      ArmoryItem(
        id = "p1",
        userId = "u1",
        name = "Aegis Core",
        category = "perk",
        universeTag = "Sector Zero",
        equipped = true,
      )
    )
    val rig = RigAsset(
      id = "r1",
      userId = "u1",
      name = "Starblade Sentinel",
      archetype = "starblade",
      activeClip = "slam",
      poseAngles = UniverseCatalog.defaultPoseMap(),
      keyframeFrames = listOf(0L, 15L, 30L, 60L),
    )

    val html = PortablePackageGenerator.generatePlayableHtmlPackage(profile, armory, rig)
    assertTrue(html.contains("<!DOCTYPE html>"))
    assertTrue(html.contains("navigator.getGamepads"))
    assertTrue(html.contains("BlakePrime"))

    val py = PortablePackageGenerator.generateBlenderRigScript(rig)
    assertTrue(py.contains("import bpy"))
    assertTrue(py.contains("Starblade Sentinel"))

    val json = PortablePackageGenerator.generatePortableJsonBundle(profile, armory, listOf(rig))
    assertTrue(json.contains("The Unknown Universes"))
    assertTrue(json.contains(BuildConfig.APPLICATION_ID))
    assertTrue(json.contains("Aegis Core"))
  }

  @Test
  fun bo3ModToolsExporter_producesValidXModelXAnimGdtAndBlenderScripts() {
    val profile = OperatorProfile(
      userId = "u1",
      callsign = "BlakePrime",
      activeUniverseId = "sector_zero",
      activeMode = "zombies",
      totalEssence = 4200L,
    )
    val armory = listOf(
      ArmoryItem(
        id = "p1",
        userId = "u1",
        name = "Chrono Jugger",
        category = "perk",
        universeTag = "Sector Zero",
        tier = 3L,
        equipped = true,
      )
    )
    val rig = RigAsset(
      id = "r1",
      userId = "u1",
      name = "Vanguard Operative",
      archetype = "operator",
      activeClip = "slam",
      rigScale = 1.0,
      playbackSpeed = 1.0,
      poseAngles = UniverseCatalog.defaultPoseMap(),
      keyframeFrames = listOf(0L, 15L, 30L, 45L, 60L),
    )

    val charXModel = Bo3ModToolsExporter.generateXModelExport(
      rig,
      Bo3ExportOptions(boneMode = Bo3BoneMappingMode.TREYARCH_CHARACTER, useBo3Inches = true),
    )
    assertTrue(charXModel.contains("MODEL\nVERSION 6"))
    assertTrue(charXModel.contains("\"j_mainroot\""))
    assertTrue(charXModel.contains("\"tag_weapon_right\""))
    assertTrue(charXModel.contains("NUMVERTS "))
    assertTrue(charXModel.contains("NUMFACES "))
    assertTrue(charXModel.contains("MATERIAL 0"))
    assertFalse(charXModel.contains("NaN"))

    val vmXModel = Bo3ModToolsExporter.generateXModelExport(
      rig,
      Bo3ExportOptions(boneMode = Bo3BoneMappingMode.TREYARCH_VIEWMODEL, useBo3Inches = false),
    )
    assertTrue(vmXModel.contains("\"tag_view\""))
    assertTrue(vmXModel.contains("\"tag_flash\""))
    assertFalse(vmXModel.contains("NaN"))

    val xAnim = Bo3ModToolsExporter.generateXAnimExport(
      rig,
      Bo3ExportOptions(boneMode = Bo3BoneMappingMode.TREYARCH_CHARACTER, fps = 60, includeNotetracks = true),
    )
    assertTrue(xAnim.contains("ANIMATION\nVERSION 3"))
    assertTrue(xAnim.contains("FRAMERATE 60"))
    assertTrue(xAnim.contains("NUMKEYS 61"))
    assertTrue(xAnim.contains("NOTETRACKS"))
    assertTrue(xAnim.contains("slam_impact"))
    assertFalse(xAnim.contains("NaN"))

    val xAnimNoNotes = Bo3ModToolsExporter.generateXAnimExport(
      rig,
      Bo3ExportOptions(boneMode = Bo3BoneMappingMode.UNKNOWN_NATIVE, fps = 30, includeNotetracks = false),
    )
    assertTrue(xAnimNoNotes.contains("\"weaponAngle\""))
    assertTrue(xAnimNoNotes.contains("FRAMERATE 30"))
    assertFalse(xAnimNoNotes.contains("NOTETRACKS"))

    val gdt = Bo3ModToolsExporter.generateGdtPackage(profile, armory, rig)
    assertTrue(gdt.contains("\"xmodel.gdf\""))
    assertTrue(gdt.contains("\"xanim.gdf\""))
    assertTrue(gdt.contains("vanguard_operative_rig.xmodel_export"))
    assertTrue(gdt.contains("Chrono Jugger"))

    val codPy = Bo3ModToolsExporter.generateCoDBlenderBo3Script(rig)
    assertTrue(codPy.contains("import bpy"))
    assertTrue(codPy.contains("bpy.ops.export_scene, \"xmodel\""))
    assertTrue(codPy.contains("bpy.ops.export_scene, \"xanim\""))
  }

  @Test
  fun exportCompatScreen_rendersBo3ModToolsOptionsAndPreviewTabs() {
    val profile = OperatorProfile(userId = "u1", callsign = "BlakePrime", activeRigId = "r1")
    val rig = RigAsset(
      id = "r1",
      userId = "u1",
      name = "Starblade Sentinel",
      archetype = "starblade",
      activeClip = "sprint",
    )

    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        ExportCompatScreen(
          profile = profile,
          armory = emptyList(),
          rigs = listOf(rig),
          onSelectConsolePreset = {},
          onStatusMessage = {},
        )
      }
    }

    composeRule.onNodeWithTag("bo3_mod_tools_card").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("download_bo3_xmodel_btn").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("download_bo3_xanim_btn").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("download_bo3_gdt_btn").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("download_bo3_blender_btn").performScrollTo().assertIsDisplayed()

    composeRule.onNodeWithTag("preview_tab_xmodel").performScrollTo().performClick()
    composeRule.onNodeWithTag("preview_code_card").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("preview_tab_xanim").performScrollTo().performClick()
    composeRule.onNodeWithTag("preview_tab_gdt").performScrollTo().performClick()
  }

  @Test
  fun arenaLoadoutAndSimState_applyArmoryPowersClawsAndLoreBosses() {
    val fullArmory = listOf(
      ArmoryItem(id = "perk_juggernaut", userId = "u1", name = "Aegis Juggernaut Core", category = "perk", tier = 2L, equipped = true),
      ArmoryItem(id = "perk_overdrive", userId = "u1", name = "Stamina Speed-Cola", category = "perk", tier = 2L, equipped = true),
      ArmoryItem(id = "power_repulse", userId = "u1", name = "Starblade Force Repulse", category = "power", tier = 2L, cooldownSec = 8L, powerValue = 250L, equipped = true),
      ArmoryItem(id = "upgrade_packtier", userId = "u1", name = "Pack-a-Tier III Matrix", category = "upgrade", tier = 4L, equipped = true),
      ArmoryItem(id = "upgrade_claws", userId = "u1", name = "Adamant Claw Edge", category = "upgrade", tier = 2L, equipped = true),
      ArmoryItem(id = "consumable_maxcharge", userId = "u1", name = "Omni-Surge Cell", category = "consumable", tier = 2L, powerValue = 300L, equipped = true),
    )

    val loadout = ArenaLoadoutStats.fromArmory(fullArmory, Color.Cyan)
    assertEquals(220, loadout.maxShield)
    assertEquals(4, loadout.initialPackTier)
    assertTrue(loadout.hasClawEdge)
    assertTrue(loadout.hasRepulsePower)
    assertTrue(loadout.multiShot >= 3)

    val sim = ArenaSimState(
      world = UniverseCatalog.worldById("blacklight_conduit"),
      modeId = "sandbox",
      loadout = loadout,
      hasConsumableEquipped = true,
    )
    assertTrue(sim.hostiles.isNotEmpty())
    sim.spawnBossForWorld(3L)
    assertTrue(sim.hostiles.any { it.isBoss && it.bossName.contains("WATER HYDRA") })

    val beforeBolts = sim.bolts.size
    sim.triggerPower()
    assertTrue(sim.bolts.size > beforeBolts)
    assertTrue(sim.hudCallout?.contains("REPULSE") == true)

    sim.stepFrame(0.016f)
    assertFalse(sim.playerX.isNaN() || sim.playerY.isNaN())
    assertTrue(sim.renderNonce > 0L)
  }

  @Test
  fun arenaScreen_rendersControlsAndRespondsToActions() {
    val profile = OperatorProfile(
      userId = "u1",
      callsign = "BlakePrime",
      activeUniverseId = "sector_zero",
      activeMode = "zombies",
      activeRigId = "r1",
    )
    val rig = RigAsset(id = "r1", userId = "u1", name = "Vanguard", archetype = "operator")
    var exited = false

    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        ArenaScreen(
          profile = profile,
          armory = emptyList(),
          rigs = listOf(rig),
          onSaveMatchAndExit = { _, _, _, _, _ -> exited = true },
        )
      }
    }
    composeRule.mainClock.advanceTimeByFrame()

    composeRule.onNodeWithText("RND I").assertIsDisplayed()
    composeRule.onNodeWithTag("arena_fire_btn").assertIsDisplayed().performClick()
    composeRule.mainClock.advanceTimeByFrame()
    composeRule.onNodeWithTag("arena_power_btn").assertIsDisplayed().performClick()
    composeRule.mainClock.advanceTimeByFrame()
    composeRule.onNodeWithTag("arena_interact_btn").assertIsDisplayed().performClick()
    composeRule.mainClock.advanceTimeByFrame()
    composeRule.onNodeWithTag("arena_dash_btn").assertIsDisplayed().performClick()
    composeRule.mainClock.advanceTimeByFrame()
    composeRule.onNodeWithTag("extract_exit_arena_btn").performClick()
    composeRule.mainClock.advanceTimeByFrame()
    assertTrue(exited)
  }
}

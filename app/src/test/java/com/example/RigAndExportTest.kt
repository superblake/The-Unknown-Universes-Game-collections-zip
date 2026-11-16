package com.example

import androidx.compose.ui.geometry.Offset
import com.example.data.model.ArmoryItem
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.export.PortablePackageGenerator
import com.example.ui.rig.RigEvaluator
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RigAndExportTest {

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
}

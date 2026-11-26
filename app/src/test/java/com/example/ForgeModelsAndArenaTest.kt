package com.example

import androidx.compose.ui.geometry.Offset
import com.example.data.model.CustomMapGameCategory
import com.example.data.model.EquipmentSlotType
import com.example.data.model.GearSlot
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.UserCustomEquipmentSpec
import com.example.data.model.UserCustomGameMapSpec
import com.example.data.model.UserCustomGearSpec
import com.example.data.model.UserCustomPerkSpec
import com.example.data.model.UserCustomWeaponSpec
import com.example.export.Bo3BoneMappingMode
import com.example.export.Bo3ExportOptions
import com.example.export.Bo3ModToolsExporter
import com.example.export.Prisma3dExportOptions
import com.example.export.Prisma3dExporter
import com.example.ui.rig.RigEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeModelsAndArenaTest {

  @Test
  fun testCustomEquipmentDeduplicationAndResolution() {
    val customLethal = UserCustomEquipmentSpec(
      id = "ceq_lethal_custom_1",
      slotType = EquipmentSlotType.LETHAL,
      name = "Arc Surge Charge",
      description = "High voltage charge",
      damageTypeId = "energy",
      baseDamageOrEffect = 340f,
      cooldownSeconds = 4.5f,
    )
    val duplicateCustomLethal = customLethal.copy(name = "Duplicate Overwrite")
    val customTac = UserCustomEquipmentSpec(
      id = "ceq_tactical_custom_2",
      slotType = EquipmentSlotType.TACTICAL,
      name = "ISAC Nano Drone",
      description = "Regen pulse",
      damageTypeId = "energy",
      baseDamageOrEffect = 140f,
      cooldownSeconds = 6.0f,
    )

    val all = ProgressionCatalog.allEquipmentItems(listOf(customLethal, duplicateCustomLethal, customTac))
    assertEquals(
      "Duplicate IDs must be deduplicated for Compose LazyRow keys",
      all.size,
      all.distinctBy { it.id }.size,
    )

    val resolvedLethal = ProgressionCatalog.resolveEquipmentItem("ceq_lethal_custom_1", listOf(customLethal))
    assertEquals("Arc Surge Charge", resolvedLethal.name)
    assertEquals(EquipmentSlotType.LETHAL, resolvedLethal.slotType)

    val resolvedTac = ProgressionCatalog.resolveEquipmentItem("ceq_tactical_custom_2", listOf(customTac))
    assertEquals("ISAC Nano Drone", resolvedTac.name)
    assertEquals(EquipmentSlotType.TACTICAL, resolvedTac.slotType)

    // Unknown ID safely falls back to a valid built-in item instead of throwing
    val fallback = ProgressionCatalog.resolveEquipmentItem("nonexistent_id", listOf(customLethal))
    assertTrue(fallback.id.isNotBlank())
  }

  @Test
  fun testCustomForgeModelsAndPlayableAreas() {
    val customMap = UserCustomGameMapSpec(
      id = "area_bo3_custom_test_1",
      mapCode = "zm_forged_bunker",
      title = "BO3 Forged Bunker",
      gameCategory = CustomMapGameCategory.BO3,
      description = "Forged custom map",
      buildingNames = listOf("Spawn Room", "Power Grid", "Pack-a-Punch Vault"),
      wonderWeapon = "Ray Gun Mk.III",
    )
    val customWeapon = UserCustomWeaponSpec(
      id = "wpn_bo3_forged_1",
      weaponCode = "bo3_ray_canon",
      name = "Ray Canon Mk.IV",
      gameCategory = CustomMapGameCategory.BO3,
      description = "Forged energy weapon",
      damageTypeId = "energy",
      baseDamage = 14,
      baseCadenceTicks = 6f,
      baseSpreadCount = 2,
      basePierce = 3,
    )
    val customPerk = UserCustomPerkSpec(
      id = "cperk_surge_1",
      name = "Surge Elixir",
      description = "Boosts shield and damage",
      shieldBonusPerRank = 30,
      damagePctPerRank = 8,
    )
    val customGear = UserCustomGearSpec(
      id = "cgear_chest_1",
      slot = GearSlot.CHEST,
      name = "Denim Tactical Rig",
      bonusShield = 45,
      bonusDamagePct = 10,
    )

    val progression = OperatorProgressionState(
      customGameMaps = listOf(customMap, customMap),
      customWeapons = listOf(customWeapon, customWeapon),
      customPerks = listOf(customPerk),
      customGear = listOf(customGear),
    )

    val playable = progression.allPlayableAreas()
    assertTrue(playable.any { it.id == customMap.id })
    assertEquals("Custom playable areas must deduplicate by id", playable.size, playable.distinctBy { it.id }.size)

    val weapons = ProgressionCatalog.allWeapons(progression.customWeapons)
    assertTrue(weapons.any { it.id == customWeapon.id })
    assertEquals("Custom weapons must deduplicate by id", weapons.size, weapons.distinctBy { it.id }.size)
    assertEquals("Custom perk should keep configured shield bonus", 30, customPerk.shieldBonusPerRank)
    assertEquals(GearSlot.CHEST, customGear.slot)
  }

  @Test
  fun testRigEvaluatorResilientToNaNAndInfiniteInputs() {
    val badPose = mapOf(
      "torso" to Double.NaN,
      "head" to Double.POSITIVE_INFINITY,
      "rUpperArm" to Double.NEGATIVE_INFINITY,
      "weaponAngle" to Double.NaN,
    )
    val skel = RigEvaluator.evaluate(
      origin = Offset(Float.NaN, Float.POSITIVE_INFINITY),
      scalePx = Float.NaN,
      poseAngles = badPose,
      clip = "sprint",
      frame = Float.NaN,
      ikEnabled = true,
      animationSpeedMultiplier = Float.POSITIVE_INFINITY,
    )

    val points = listOf(
      skel.pelvis, skel.chest, skel.head,
      skel.lShoulder, skel.lElbow, skel.lHand,
      skel.rShoulder, skel.rElbow, skel.rHand,
      skel.weaponTip, skel.lHip, skel.lKnee, skel.lFoot,
      skel.rHip, skel.rKnee, skel.rFoot,
    )
    points.forEach { pt ->
      assertTrue("Evaluated joint X must be finite: ${pt.x}", pt.x.isFinite())
      assertTrue("Evaluated joint Y must be finite: ${pt.y}", pt.y.isFinite())
    }
  }

  @Test
  fun testExportersResilientToBlankNamesEmptyKeyframesAndNaNValues() {
    val edgeRig = RigAsset(
      id = "rig_edge",
      userId = "u1",
      name = "   ",
      archetype = "operator",
      activeClip = "slam",
      ikEnabled = false,
      rigScale = Double.NaN,
      playbackSpeed = Double.POSITIVE_INFINITY,
      poseAngles = mapOf("torso" to Double.NaN, "rUpperArm" to Double.NEGATIVE_INFINITY),
      keyframeFrames = emptyList(),
      isCompleted = true,
    )

    val prismaJson = Prisma3dExporter.generatePrismaSceneJson(edgeRig, Prisma3dExportOptions())
    assertTrue(prismaJson.startsWith("{"))
    assertFalse("Prisma JSON must never contain literal NaN", prismaJson.contains("NaN"))
    assertFalse("Prisma JSON must never contain Infinity", prismaJson.contains("Infinity"))

    val bo3Opts = Bo3ExportOptions(boneMode = Bo3BoneMappingMode.TREYARCH_CHARACTER)
    val xmodel = Bo3ModToolsExporter.generateXModelExport(edgeRig, bo3Opts)
    val xanim = Bo3ModToolsExporter.generateXAnimExport(edgeRig, bo3Opts)
    val codPy = Bo3ModToolsExporter.generateCoDBlenderBo3Script(edgeRig, bo3Opts)

    assertTrue(xmodel.contains("VERSION 6"))
    assertFalse(xmodel.contains("NaN"))
    assertTrue(xanim.contains("VERSION 3"))
    assertFalse(xanim.contains("NaN"))
    assertTrue(codPy.contains("import bpy"))
    assertFalse(codPy.contains("NaN"))
  }
}

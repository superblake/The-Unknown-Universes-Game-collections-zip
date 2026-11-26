package com.example

import com.example.data.model.CharacterUniverseGroup
import com.example.data.model.CustomMapGameCategory
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.export.Prisma3dExportOptions
import com.example.export.Prisma3dExporter
import com.example.export.Prisma3dImporter
import com.example.util.AppCrashShield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class Prisma3dExportAndImportTest {

  private fun createSampleRig(): RigAsset {
    return RigAsset(
      id = "blake_real_life_rig",
      userId = "blake_creator",
      name = "Blake Casual Creator Rig",
      archetype = "operator",
      activeClip = "sprint",
      ikEnabled = true,
      rigScale = 1.05,
      playbackSpeed = 1.0,
      poseAngles = UniverseCatalog.defaultPoseMap() + mapOf(
        "torso" to -8.0,
        "lUpperArm" to -28.0,
        "rUpperArm" to 34.0,
        "weaponAngle" to -12.0,
      ),
      keyframeFrames = listOf(0L, 12L, 24L, 38L, 60L),
      isCompleted = true,
    )
  }

  @Test
  fun testPrisma3dExporterSceneJson() {
    val rig = createSampleRig()
    val options = Prisma3dExportOptions(fps = 60, includeMeshes = true, includeKeyframes = true)
    val jsonStr = Prisma3dExporter.generatePrismaSceneJson(rig, options)

    assertNotNull(jsonStr)
    assertTrue("JSON must not be empty", jsonStr.isNotBlank())
    assertTrue(jsonStr.startsWith("{") && jsonStr.endsWith("}"))
    assertTrue(jsonStr.contains("\"version\": \"3.0\""))
    assertTrue(jsonStr.contains("\"format\": \"prisma3d_scene\""))
    assertTrue(jsonStr.contains("\"projectName\": \"Blake Casual Creator Rig\""))
    assertTrue(jsonStr.contains("\"archetype\": \"operator\""))
    assertTrue(jsonStr.contains("\"fps\": 60"))
    assertTrue(jsonStr.contains("\"bones\": ["))
    assertTrue(jsonStr.contains("\"objects\": ["))
    assertTrue(jsonStr.contains("\"materials\": ["))
    assertTrue(jsonStr.contains("\"Left_Forearm_Wristwatch\""))
    assertTrue(jsonStr.contains("\"Visor_Glasses\""))
  }

  @Test
  fun testPrisma3dExporterWavefrontObjAndMtl() {
    val rig = createSampleRig()
    val objText = Prisma3dExporter.generateWavefrontObj(rig)
    val mtlText = Prisma3dExporter.generateWavefrontMtl(rig)

    assertTrue("OBJ should have file header", objText.contains("# The Unknown Universes — Prisma3D"))
    assertTrue("OBJ should reference material library", objText.contains("mtllib"))
    assertTrue("OBJ should contain 3D vertices", objText.contains("\nv "))
    assertTrue("OBJ should contain faces", objText.contains("\nf "))
    assertTrue("OBJ should contain named body parts", objText.contains("o Torso_Armor"))
    assertTrue("OBJ should contain left arm with wristwatch", objText.contains("o Left_Forearm_Wristwatch"))

    assertTrue("MTL should define Material_Armor", mtlText.contains("newmtl Material_Armor"))
    assertTrue("MTL should define Material_Visor", mtlText.contains("newmtl Material_Visor"))
  }

  @Test
  fun testPrisma3dExporterGlTF() {
    val rig = createSampleRig()
    val gltfJson = Prisma3dExporter.generateGlTFSceneJson(rig)

    assertTrue(gltfJson.startsWith("{") && gltfJson.endsWith("}"))
    assertTrue(gltfJson.contains("\"version\": \"2.0\""))
    assertTrue(gltfJson.contains("\"generator\": \"UnknownOS Prisma3D Exporter\""))
    assertTrue(gltfJson.contains("\"nodes\": ["))
    assertTrue(gltfJson.contains("\"Root_Blake Casual Creator Rig\""))
  }

  @Test
  fun testPrisma3dExporterZipBundle() {
    val rig = createSampleRig()
    val zipBytes = Prisma3dExporter.generatePrisma3dZipBundle(rig)

    assertTrue("Zip bytes should not be empty", zipBytes.isNotEmpty())

    val entries = mutableListOf<String>()
    ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        entries.add(entry.name)
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }

    assertTrue("Zip must contain project.prisma", entries.contains("project.prisma"))
    assertTrue("Zip must contain .obj mesh", entries.any { it.endsWith(".obj") })
    assertTrue("Zip must contain .mtl material", entries.any { it.endsWith(".mtl") })
    assertTrue("Zip must contain .gltf scene", entries.any { it.endsWith(".gltf") })
    assertTrue("Zip must contain README_PRISMA3D.txt", entries.contains("README_PRISMA3D.txt"))
  }

  @Test
  fun testPrisma3dExportObjImportRoundTripAndPresets() {
    val rig = createSampleRig()
    val objText = Prisma3dExporter.generateWavefrontObj(rig)
    assertTrue(Prisma3dImporter.looksLikePrisma3dFile("blake_rig.obj", objText.toByteArray()))

    val importResult = Prisma3dImporter.importFromText(objText, "blake_rig.obj", "user_test")
    assertTrue("Import from generated Wavefront OBJ should succeed", importResult.isSuccess)

    val imported = importResult.getOrThrow()
    assertTrue("Imported stats must count 3D vertices", imported.stats.vertexCount > 0)
    assertTrue("Imported stats must count 3D faces", imported.stats.faceCount > 0)
    assertTrue("Imported stats must count body part objects", imported.stats.objectCount >= 10)

    val zipBytes = Prisma3dExporter.generatePrisma3dZipBundle(rig)
    assertTrue(
      "looksLikePrisma3dFile should recognize .p3d bundle",
      Prisma3dImporter.looksLikePrisma3dFile("bundle.p3d", zipBytes),
    )

    val preset = Prisma3dImporter.getSamplePreset("real_life_blake", "user_test")
    assertEquals("Blake Prisma3D Casual Rig", preset.rig.name)
    assertTrue(preset.stats.boneCount >= 12)
  }

  @Test
  fun testRealLifeCreatorOutfitsGrounding() {
    val outfits = MultiverseArsenalCatalog.outfits
    assertEquals(66, outfits.size)

    val originalOutfits = outfits.filter { it.isOriginalCharacter }
    assertEquals(21, originalOutfits.size)

    for (outfit in originalOutfits) {
      assertTrue("Original outfit ${outfit.id} must be flagged as isRealLifeCreatorVersion", outfit.isRealLifeCreatorVersion)
      assertTrue("Outfit ${outfit.id} must have realLifeInspiration", outfit.realLifeInspiration.isNotBlank())
      assertTrue("Outfit ${outfit.id} must have realLifeApparel", outfit.realLifeApparel.isNotBlank())
      assertTrue("Outfit ${outfit.id} must have realLifeDailyGear", outfit.realLifeDailyGear.isNotBlank())
    }

    val originalGroups = originalOutfits.map { it.characterGroup }.distinct()
    assertEquals(7, originalGroups.size)

    val groups = CharacterUniverseGroup.entries
    assertEquals(23, groups.size)
    assertTrue("Ship of Heroes group should exist", groups.contains(CharacterUniverseGroup.SHIP_OF_HEROES))
    assertTrue("Superman group should exist", groups.contains(CharacterUniverseGroup.SUPERMAN))
  }

  @Test
  fun testExpandedSkillsPerksAndEquipment() {
    val skillNodes = ProgressionCatalog.defaultSkillNodes()
    assertEquals("defaultSkillNodes must have 36 skills", 36, skillNodes.size)

    val categories = skillNodes.values.map { it.category }.distinct()
    assertTrue("Must cover at least 6 categories", categories.size >= 6)
    assertTrue("Must have REAL_LIFE skill category", categories.contains("REAL_LIFE"))
    assertTrue("Must have PROTECTION skill category", categories.contains("PROTECTION"))

    val perks = MultiverseArsenalCatalog.perkDrinks
    assertEquals("MultiverseArsenalCatalog must have 14 perk drinks", 14, perks.size)

    val protections = ProgressionCatalog.protectionUpgrades
    assertEquals("ProgressionCatalog must have 10 protection upgrades", 10, protections.size)

    val equipment = ProgressionCatalog.equipmentItems
    assertEquals("ProgressionCatalog must have 16 equipment items", 16, equipment.size)
  }

  @Test
  fun testForgeMapAndWeaponPresetEnumMappings() {
    val cats = CustomMapGameCategory.entries
    assertTrue(cats.contains(CustomMapGameCategory.INFAMOUS))
    assertTrue(cats.contains(CustomMapGameCategory.ARKHAM))
    assertTrue(cats.contains(CustomMapGameCategory.GODOFWAR))
    assertTrue(cats.contains(CustomMapGameCategory.BO3))
    assertTrue(cats.contains(CustomMapGameCategory.ABYSS_DUNGEONS))
  }

  @Test
  fun testDrawnAppIconAndMenuBannersWired() {
    assertTrue(R.drawable.tuu_drawn_app_icon_1795681684222 != 0)
    assertTrue(R.drawable.tuu_hero_banner_drawn_1795681686173 != 0)
    assertTrue(R.drawable.tuu_sector_zero_drawn_1795681683332 != 0)
    assertTrue(R.drawable.tuu_conduit_drawn_1795681682431 != 0)
    assertTrue(R.drawable.tuu_hellbreaker_drawn_1795681702472 != 0)
    assertTrue(R.drawable.tuu_chrono_aegis_drawn_1795681705351 != 0)
    assertTrue(R.drawable.tuu_aegis_starblade_drawn_1795681704068 != 0)
    assertTrue(
      "All worlds must reference valid banner drawables",
      UniverseCatalog.worlds.all { (it.bannerRes ?: 0) != 0 },
    )
  }

  @Test
  fun testAppCrashShieldStatus() {
    val initialCrash = AppCrashShield.lastCrashReport
    assertTrue(initialCrash == null || initialCrash.isNotBlank())
  }
}

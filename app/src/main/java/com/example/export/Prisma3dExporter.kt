package com.example.export

import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Supported file export formats for Prisma3D on Android & iOS.
 */
enum class Prisma3dExportFormat(
  val id: String,
  val fileExtension: String,
  val label: String,
  val description: String,
) {
  SCENE_JSON(
    id = "prisma_scene",
    fileExtension = ".prisma",
    label = "Prisma3D Scene (.prisma)",
    description = "Full Prisma3D scene project with bone hierarchy, keyframes, materials & meshes",
  ),
  ZIP_PACKAGE(
    id = "prisma_bundle",
    fileExtension = ".p3d",
    label = "Prisma3D Archive (.p3d)",
    description = "Self-contained bundle with scene JSON, 3D Wavefront .OBJ mesh, .MTL and import guide",
  ),
  WAVEFRONT_OBJ(
    id = "prisma_obj",
    fileExtension = ".obj",
    label = "3D Wavefront Mesh (.obj)",
    description = "Posed 3D humanoid mesh with named body parts & materials for Prisma3D model importer",
  ),
  GLTF_SCENE(
    id = "prisma_gltf",
    fileExtension = ".gltf",
    label = "glTF 2.0 Scene (.gltf)",
    description = "Standard glTF 2.0 format compatible with Prisma3D and all modern 3D viewports",
  ),
}

data class Prisma3dExportOptions(
  val format: Prisma3dExportFormat = Prisma3dExportFormat.SCENE_JSON,
  val fps: Int = 30,
  val includeMeshes: Boolean = true,
  val includeKeyframes: Boolean = true,
  val scaleMultiplier: Double = 1.0,
  val author: String = "Blake",
  val appName: String = "UnknownOS ForgeRig",
)

object Prisma3dExporter {

  fun sanitizeFileName(raw: String, fallback: String = "unknown_rig"): String =
    raw.lowercase(Locale.US)
      .replace(Regex("[^a-z0-9]+"), "_")
      .trim('_')
      .ifBlank { fallback }
      .take(48)

  /**
   * Generates a complete Prisma3D project JSON scene (.prisma / .prisma3d).
   * Fully compatible with Prisma3D's scene graph:
   * - Hierarchical bones (torso -> head, arms, legs, weapon)
   * - Local rotation angles mapped from poseAngles
   * - Animation tracks with keyframe timings
   * - Geometric meshes for 3D preview & rendering
   * - Material palette
   */
  fun generatePrismaSceneJson(
    rig: RigAsset,
    options: Prisma3dExportOptions = Prisma3dExportOptions(),
  ): String {
    val now = System.currentTimeMillis()
    val fps = options.fps.coerceIn(24, 60)
    val effectiveScale = (rig.rigScale * options.scaleMultiplier).coerceIn(0.5, 2.5)
    val (primaryHex, secondaryHex, visorHex) = getPaletteForArchetype(rig.archetype)
    val boneSpecs = UniverseCatalog.rigBones
    val angles = rig.poseAngles
    val frames = if (rig.keyframeFrames.isEmpty()) listOf(0L, 15L, 30L, 45L, 60L) else rig.keyframeFrames

    val sb = StringBuilder()
    sb.appendLine("{")
    sb.appendLine("  \"version\": \"3.0\",")
    sb.appendLine("  \"format\": \"prisma3d_scene\",")
    sb.appendLine("  \"appName\": ${jsonStr(options.appName)},")
    sb.appendLine("  \"projectName\": ${jsonStr(rig.name)},")
    sb.appendLine("  \"author\": ${jsonStr(options.author)},")
    sb.appendLine("  \"createdAtMillis\": $now,")
    sb.appendLine("  \"archetype\": ${jsonStr(rig.archetype)},")
    sb.appendLine("  \"activeClip\": ${jsonStr(rig.activeClip)},")
    sb.appendLine("  \"fps\": $fps,")
    sb.appendLine("  \"frameCount\": 60,")
    sb.appendLine("  \"ikEnabled\": ${rig.ikEnabled},")
    sb.appendLine("  \"scale\": ${jsonNum(effectiveScale)},")

    // Materials definition
    val materials = listOf(
      createMaterialJson("Material_Armor", primaryHex, 0.4, 0.2),
      createMaterialJson("Material_Accent", secondaryHex, 0.3, 0.5),
      createMaterialJson("Material_Visor", visorHex, 0.1, 0.9, isEmissive = true),
      createMaterialJson("Material_Weapon", "#CBD5E1", 0.2, 0.8),
    )
    sb.appendLine("  \"materials\": [")
    materials.forEachIndexed { idx, matJson ->
      val comma = if (idx < materials.lastIndex) "," else ""
      sb.appendLine("    $matJson$comma")
    }
    sb.appendLine("  ],")

    // Bones hierarchy
    sb.appendLine("  \"bones\": [")
    boneSpecs.forEachIndexed { idx, spec ->
      val deg = angles[spec.id] ?: spec.defaultAngle.toDouble()
      val pos = getBoneBasePosition(spec.id, effectiveScale)
      val parentJson = spec.parentId?.let { jsonStr(it) } ?: "null"
      sb.appendLine("    {")
      sb.appendLine("      \"id\": ${jsonStr(spec.id)},")
      sb.appendLine("      \"name\": ${jsonStr(spec.label)},")
      sb.appendLine("      \"type\": \"bone\",")
      sb.appendLine("      \"parent\": $parentJson,")
      sb.appendLine("      \"rotation\": {\"x\": 0.0, \"y\": 0.0, \"z\": ${jsonNum(deg)}},")
      sb.appendLine("      \"position\": {\"x\": ${jsonNum(pos.first)}, \"y\": ${jsonNum(pos.second)}, \"z\": ${jsonNum(pos.third)}},")
      val kfTrailing = if (options.includeKeyframes) "," else ""
      sb.appendLine("      \"scale\": {\"x\": 1.0, \"y\": 1.0, \"z\": 1.0}$kfTrailing")
      if (options.includeKeyframes) {
        sb.appendLine("      \"keyframes\": [")
        frames.forEachIndexed { fIdx, f ->
          val frameFraction = (f.toDouble() / 60.0).coerceIn(0.0, 1.0)
          val waveFactor = sin(frameFraction * 2.0 * PI)
          val animatedDeg = deg + (waveFactor * 8.0)
          val kComma = if (fIdx < frames.lastIndex) "," else ""
          sb.appendLine(
            "        {\"frame\": $f, \"timeSec\": ${jsonNum(f.toDouble() / fps)}, \"rotation\": {\"x\": 0.0, \"y\": 0.0, \"z\": ${jsonNum(animatedDeg)}}}$kComma"
          )
        }
        sb.appendLine("      ]")
      }
      val bComma = if (idx < boneSpecs.lastIndex) "," else ""
      sb.appendLine("    }$bComma")
    }
    sb.appendLine("  ],")

    // 3D Objects / Meshes (Low-Poly Humanoid Body Segments)
    if (options.includeMeshes) {
      val meshes = buildHumanoidMeshes(rig, effectiveScale)
      sb.appendLine("  \"objects\": [")
      meshes.forEachIndexed { idx, m ->
        val vertsJoined = m.vertices.joinToString(", ") { jsonNum(it) }
        val facesJoined = m.faces.joinToString(", ")
        val mComma = if (idx < meshes.lastIndex) "," else ""
        sb.appendLine("    {")
        sb.appendLine("      \"name\": ${jsonStr(m.name)},")
        sb.appendLine("      \"type\": \"mesh\",")
        sb.appendLine("      \"boneId\": ${jsonStr(m.boneId)},")
        sb.appendLine("      \"material\": ${jsonStr(m.materialName)},")
        sb.appendLine("      \"vertices\": [$vertsJoined],")
        sb.appendLine("      \"faces\": [$facesJoined]")
        sb.appendLine("    }$mComma")
      }
      sb.appendLine("  ],")
    }

    // Animation Clips
    sb.appendLine("  \"animations\": [")
    sb.appendLine(
      "    {\"name\": ${jsonStr(rig.activeClip)}, \"fps\": $fps, \"length\": 60, \"loop\": true, \"playbackSpeed\": ${jsonNum(rig.playbackSpeed)}}"
    )
    sb.appendLine("  ]")
    sb.append("}")

    return sb.toString()
  }

  /**
   * Generates a standard Wavefront .OBJ file with the humanoid mesh
   * posed according to the rig's current joint angles and archetype.
   */
  fun generateWavefrontObj(
    rig: RigAsset,
    options: Prisma3dExportOptions = Prisma3dExportOptions(),
  ): String {
    val sb = StringBuilder()
    val slug = sanitizeFileName(rig.name)
    val effectiveScale = (rig.rigScale * options.scaleMultiplier).coerceIn(0.5, 2.5)

    sb.appendLine("# The Unknown Universes — Prisma3D 3D Wavefront Mesh (.obj)")
    sb.appendLine("# Rig Name: ${rig.name}")
    sb.appendLine("# Archetype: ${rig.archetype} • Active Clip: ${rig.activeClip}")
    sb.appendLine("# Compatible with Prisma3D Mobile Model Importer & Blender")
    sb.appendLine("mtllib ${slug}.mtl")
    sb.appendLine()

    val meshes = buildHumanoidMeshes(rig, effectiveScale)
    var globalVertexOffset = 1

    for (m in meshes) {
      sb.appendLine("o ${m.name}")
      sb.appendLine("usemtl ${m.materialName}")
      sb.appendLine("s 1")

      // Vertices
      for (i in 0 until m.vertices.size step 3) {
        val x = m.vertices[i]
        val y = m.vertices[i + 1]
        val z = m.vertices[i + 2]
        sb.appendLine("v ${fmt(x)} ${fmt(y)} ${fmt(z)}")
      }

      // Faces (triangles, 1-indexed)
      val localVertCount = m.vertices.size / 3
      for (f in 0 until m.faces.size step 3) {
        val i0 = m.faces[f] + globalVertexOffset
        val i1 = m.faces[f + 1] + globalVertexOffset
        val i2 = m.faces[f + 2] + globalVertexOffset
        sb.appendLine("f $i0 $i1 $i2")
      }
      globalVertexOffset += localVertCount
      sb.appendLine()
    }

    return sb.toString()
  }

  /**
   * Generates a companion Wavefront .MTL material definition.
   */
  fun generateWavefrontMtl(rig: RigAsset): String {
    val (primaryHex, secondaryHex, visorHex) = getPaletteForArchetype(rig.archetype)
    val pRgb = parseHexRgb(primaryHex)
    val sRgb = parseHexRgb(secondaryHex)
    val vRgb = parseHexRgb(visorHex)

    return """# The Unknown Universes — Material Definitions for Prisma3D
# Rig: ${rig.name} (${rig.archetype})

newmtl Material_Armor
Ka 0.1 0.1 0.1
Kd ${fmt(pRgb.first)} ${fmt(pRgb.second)} ${fmt(pRgb.third)}
Ks 0.3 0.3 0.3
Ns 32.0
d 1.0
illum 2

newmtl Material_Accent
Ka 0.1 0.1 0.1
Kd ${fmt(sRgb.first)} ${fmt(sRgb.second)} ${fmt(sRgb.third)}
Ks 0.5 0.5 0.5
Ns 64.0
d 1.0
illum 2

newmtl Material_Visor
Ka 0.3 0.3 0.3
Kd ${fmt(vRgb.first)} ${fmt(vRgb.second)} ${fmt(vRgb.third)}
Ks 0.9 0.9 0.9
Ke ${fmt(vRgb.first * 0.7)} ${fmt(vRgb.second * 0.7)} ${fmt(vRgb.third * 0.7)}
Ns 128.0
d 1.0
illum 2

newmtl Material_Weapon
Ka 0.15 0.15 0.15
Kd 0.82 0.85 0.90
Ks 0.75 0.75 0.75
Ns 96.0
d 1.0
illum 2
"""
  }

  /**
   * Generates a standard glTF 2.0 JSON scene file (.gltf).
   */
  fun generateGlTFSceneJson(
    rig: RigAsset,
    options: Prisma3dExportOptions = Prisma3dExportOptions(),
  ): String {
    val boneSpecs = UniverseCatalog.rigBones
    val childIndices = (1..boneSpecs.size).joinToString(", ")
    val scaleVal = jsonNum(options.scaleMultiplier)

    val sb = StringBuilder()
    sb.appendLine("{")
    sb.appendLine("  \"asset\": {")
    sb.appendLine("    \"version\": \"2.0\",")
    sb.appendLine("    \"generator\": \"UnknownOS Prisma3D Exporter\"")
    sb.appendLine("  },")
    sb.appendLine("  \"scene\": 0,")
    sb.appendLine("  \"scenes\": [")
    sb.appendLine("    {\"name\": ${jsonStr(rig.name)}, \"nodes\": [0]}")
    sb.appendLine("  ],")
    sb.appendLine("  \"nodes\": [")
    val rootComma = if (boneSpecs.isNotEmpty()) "," else ""
    sb.appendLine("    {\"name\": ${jsonStr("Root_${rig.name}")}, \"scale\": [$scaleVal, $scaleVal, $scaleVal], \"children\": [$childIndices]}$rootComma")
    boneSpecs.forEachIndexed { idx, spec ->
      val deg = rig.poseAngles[spec.id] ?: spec.defaultAngle.toDouble()
      val pos = getBoneBasePosition(spec.id, options.scaleMultiplier)
      val rad = Math.toRadians(deg)
      val qz = sin(rad / 2.0)
      val qw = cos(rad / 2.0)
      val comma = if (idx < boneSpecs.lastIndex) "," else ""
      sb.appendLine(
        "    {\"name\": ${jsonStr(spec.label)}, \"translation\": [${jsonNum(pos.first)}, ${jsonNum(pos.second)}, ${jsonNum(pos.third)}], \"rotation\": [0.0, 0.0, ${jsonNum(qz)}, ${jsonNum(qw)}]}$comma"
      )
    }
    sb.appendLine("  ]")
    sb.append("}")
    return sb.toString()
  }

  /**
   * Packages all Prisma3D export assets into a single zip archive (.p3d).
   */
  fun generatePrisma3dZipBundle(
    rig: RigAsset,
    options: Prisma3dExportOptions = Prisma3dExportOptions(),
  ): ByteArray {
    val slug = sanitizeFileName(rig.name)
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zos ->
      // 1. Prisma3D Project Scene JSON
      val sceneJson = generatePrismaSceneJson(rig, options)
      zos.putNextEntry(ZipEntry("project.prisma"))
      zos.write(sceneJson.toByteArray(Charsets.UTF_8))
      zos.closeEntry()

      // 2. 3D Wavefront Mesh (.obj)
      val objMesh = generateWavefrontObj(rig, options)
      zos.putNextEntry(ZipEntry("${slug}.obj"))
      zos.write(objMesh.toByteArray(Charsets.UTF_8))
      zos.closeEntry()

      // 3. Material file (.mtl)
      val mtl = generateWavefrontMtl(rig)
      zos.putNextEntry(ZipEntry("${slug}.mtl"))
      zos.write(mtl.toByteArray(Charsets.UTF_8))
      zos.closeEntry()

      // 4. glTF 2.0 Scene
      val gltf = generateGlTFSceneJson(rig, options)
      zos.putNextEntry(ZipEntry("${slug}.gltf"))
      zos.write(gltf.toByteArray(Charsets.UTF_8))
      zos.closeEntry()

      // 5. Prisma3D Instructions
      val readme = """=====================================================
THE UNKNOWN UNIVERSES — PRISMA3D EXPORT PACKAGE
=====================================================
Rig: ${rig.name}
Archetype: ${rig.archetype}
Active Clip: ${rig.activeClip}
Exported At: ${System.currentTimeMillis()}

HOW TO OPEN IN PRISMA3D:
-----------------------------------------------------
Option A (3D Model Import):
1. Open Prisma3D on your Android or iOS device.
2. Tap the '+' menu or 3D Import icon.
3. Select '${slug}.obj'.
4. Prisma3D will load the full humanoid character mesh
   with materials (${slug}.mtl) automatically!

Option B (Project Scene Import):
1. In Prisma3D, choose 'Open Project' or import 'project.prisma'.
2. The full bone skeleton and animation keyframes will load!

=====================================================
Built with UnknownOS • Ready for 3D Animation!
"""
      zos.putNextEntry(ZipEntry("README_PRISMA3D.txt"))
      zos.write(readme.toByteArray(Charsets.UTF_8))
      zos.closeEntry()
    }
    return out.toByteArray()
  }

  /**
   * Returns a concise human-readable text descriptor.
   */
  fun generatePrisma3dDescriptor(rig: RigAsset): String {
    val sb = StringBuilder()
    sb.appendLine("PRISMA3D RIG SPECIFICATION")
    sb.appendLine("Name: ${rig.name} • Archetype: ${rig.archetype} • Clip: ${rig.activeClip}")
    sb.appendLine("Rig Scale: ${rig.rigScale} • IK Enabled: ${rig.ikEnabled}")
    sb.appendLine("Keyframes (${rig.keyframeFrames.size}): ${rig.keyframeFrames.joinToString(", ")}")
    sb.appendLine()
    sb.appendLine("POSE ANGLE MAPPINGS (Prisma3D Bone Local Rotations):")
    for (spec in UniverseCatalog.rigBones) {
      val deg = rig.poseAngles[spec.id] ?: spec.defaultAngle.toDouble()
      sb.appendLine("  • ${spec.label} [${spec.id}]: ${fmt(deg)}° (Range: ${spec.minAngle}° .. ${spec.maxAngle}°)")
    }
    return sb.toString()
  }

  // --- Internal Mesh & Math Helpers ---

  private data class HumanoidMeshPart(
    val name: String,
    val boneId: String,
    val materialName: String,
    val vertices: List<Double>,
    val faces: List<Int>,
  )

  private fun buildHumanoidMeshes(rig: RigAsset, scale: Double): List<HumanoidMeshPart> {
    val list = mutableListOf<HumanoidMeshPart>()
    val angles = rig.poseAngles

    // 1. Torso / Body Armor
    val torsoDeg = angles["torso"] ?: 0.0
    val torsoRad = Math.toRadians(torsoDeg)
    val torsoBox = generateBoxMesh(
      cx = 0.0,
      cy = 0.0,
      cz = 1.15 * scale,
      dx = 0.38 * scale,
      dy = 0.22 * scale,
      dz = 0.46 * scale,
      rotYRad = torsoRad,
    )
    list.add(HumanoidMeshPart("Torso_Armor", "torso", "Material_Armor", torsoBox.first, torsoBox.second))

    // 2. Head & Visor
    val headDeg = angles["head"] ?: 0.0
    val headRad = Math.toRadians(headDeg + torsoDeg)
    val headBox = generateBoxMesh(
      cx = 0.0,
      cy = 0.0,
      cz = 1.62 * scale,
      dx = 0.22 * scale,
      dy = 0.22 * scale,
      dz = 0.24 * scale,
      rotYRad = headRad,
    )
    list.add(HumanoidMeshPart("Head_Helmet", "head", "Material_Armor", headBox.first, headBox.second))

    // Visor strip (or Blake eyeglasses frame)
    val visorBox = generateBoxMesh(
      cx = 0.0,
      cy = 0.12 * scale,
      cz = 1.62 * scale,
      dx = 0.18 * scale,
      dy = 0.04 * scale,
      dz = 0.08 * scale,
      rotYRad = headRad,
    )
    list.add(HumanoidMeshPart("Visor_Glasses", "head", "Material_Visor", visorBox.first, visorBox.second))

    // 3. Left Arm (Upper arm + Forearm + Wristwatch)
    val lUpperDeg = angles["lUpperArm"] ?: -24.0
    val lUpperRad = Math.toRadians(lUpperDeg + torsoDeg)
    val lShoulderX = -0.26 * scale
    val lShoulderZ = 1.38 * scale
    val lUpperBox = generateBoxMesh(
      cx = lShoulderX,
      cy = 0.0,
      cz = lShoulderZ - 0.16 * scale,
      dx = 0.14 * scale,
      dy = 0.14 * scale,
      dz = 0.32 * scale,
      rotYRad = lUpperRad,
    )
    list.add(HumanoidMeshPart("Left_Upper_Arm", "lUpperArm", "Material_Armor", lUpperBox.first, lUpperBox.second))

    val lForeDeg = angles["lForearm"] ?: -35.0
    val lForeRad = Math.toRadians(lForeDeg + lUpperDeg)
    val lForeBox = generateBoxMesh(
      cx = lShoulderX,
      cy = 0.0,
      cz = lShoulderZ - 0.44 * scale,
      dx = 0.12 * scale,
      dy = 0.12 * scale,
      dz = 0.28 * scale,
      rotYRad = lForeRad,
    )
    list.add(HumanoidMeshPart("Left_Forearm_Wristwatch", "lForearm", "Material_Accent", lForeBox.first, lForeBox.second))

    // 4. Right Arm (Upper arm + Forearm)
    val rUpperDeg = angles["rUpperArm"] ?: 28.0
    val rUpperRad = Math.toRadians(rUpperDeg + torsoDeg)
    val rShoulderX = 0.26 * scale
    val rShoulderZ = 1.38 * scale
    val rUpperBox = generateBoxMesh(
      cx = rShoulderX,
      cy = 0.0,
      cz = rShoulderZ - 0.16 * scale,
      dx = 0.14 * scale,
      dy = 0.14 * scale,
      dz = 0.32 * scale,
      rotYRad = rUpperRad,
    )
    list.add(HumanoidMeshPart("Right_Upper_Arm", "rUpperArm", "Material_Armor", rUpperBox.first, rUpperBox.second))

    val rForeDeg = angles["rForearm"] ?: 40.0
    val rForeRad = Math.toRadians(rForeDeg + rUpperDeg)
    val rForeBox = generateBoxMesh(
      cx = rShoulderX,
      cy = 0.0,
      cz = rShoulderZ - 0.44 * scale,
      dx = 0.12 * scale,
      dy = 0.12 * scale,
      dz = 0.28 * scale,
      rotYRad = rForeRad,
    )
    list.add(HumanoidMeshPart("Right_Forearm_Gauntlet", "rForearm", "Material_Accent", rForeBox.first, rForeBox.second))

    // 5. Tactical Weapon Socket
    val wDeg = angles["weaponAngle"] ?: -15.0
    val wRad = Math.toRadians(wDeg + rForeDeg)
    val weaponBox = generateBoxMesh(
      cx = rShoulderX + 0.10 * scale,
      cy = 0.12 * scale,
      cz = rShoulderZ - 0.52 * scale,
      dx = 0.08 * scale,
      dy = 0.55 * scale,
      dz = 0.10 * scale,
      rotYRad = wRad,
    )
    list.add(HumanoidMeshPart("Tactical_Weapon", "weaponAngle", "Material_Weapon", weaponBox.first, weaponBox.second))

    // 6. Left Leg (Thigh + Shin)
    val lThighDeg = angles["lThigh"] ?: -14.0
    val lThighRad = Math.toRadians(lThighDeg + torsoDeg)
    val lHipX = -0.14 * scale
    val lHipZ = 0.88 * scale
    val lThighBox = generateBoxMesh(
      cx = lHipX,
      cy = 0.0,
      cz = lHipZ - 0.22 * scale,
      dx = 0.16 * scale,
      dy = 0.16 * scale,
      dz = 0.42 * scale,
      rotYRad = lThighRad,
    )
    list.add(HumanoidMeshPart("Left_Thigh", "lThigh", "Material_Armor", lThighBox.first, lThighBox.second))

    val lShinDeg = angles["lShin"] ?: 16.0
    val lShinRad = Math.toRadians(lShinDeg + lThighDeg)
    val lShinBox = generateBoxMesh(
      cx = lHipX,
      cy = 0.0,
      cz = lHipZ - 0.62 * scale,
      dx = 0.14 * scale,
      dy = 0.15 * scale,
      dz = 0.40 * scale,
      rotYRad = lShinRad,
    )
    list.add(HumanoidMeshPart("Left_Shin_Boot", "lShin", "Material_Accent", lShinBox.first, lShinBox.second))

    // 7. Right Leg (Thigh + Shin)
    val rThighDeg = angles["rThigh"] ?: 16.0
    val rThighRad = Math.toRadians(rThighDeg + torsoDeg)
    val rHipX = 0.14 * scale
    val rHipZ = 0.88 * scale
    val rThighBox = generateBoxMesh(
      cx = rHipX,
      cy = 0.0,
      cz = rHipZ - 0.22 * scale,
      dx = 0.16 * scale,
      dy = 0.16 * scale,
      dz = 0.42 * scale,
      rotYRad = rThighRad,
    )
    list.add(HumanoidMeshPart("Right_Thigh", "rThigh", "Material_Armor", rThighBox.first, rThighBox.second))

    val rShinDeg = angles["rShin"] ?: -12.0
    val rShinRad = Math.toRadians(rShinDeg + rThighDeg)
    val rShinBox = generateBoxMesh(
      cx = rHipX,
      cy = 0.0,
      cz = rHipZ - 0.62 * scale,
      dx = 0.14 * scale,
      dy = 0.15 * scale,
      dz = 0.40 * scale,
      rotYRad = rShinRad,
    )
    list.add(HumanoidMeshPart("Right_Shin_Boot", "rShin", "Material_Accent", rShinBox.first, rShinBox.second))

    return list
  }

  /**
   * Generates 8 vertices and 12 triangular faces for a 3D box oriented with rotYRad.
   */
  private fun generateBoxMesh(
    cx: Double,
    cy: Double,
    cz: Double,
    dx: Double,
    dy: Double,
    dz: Double,
    rotYRad: Double,
  ): Pair<List<Double>, List<Int>> {
    val hx = dx / 2.0
    val hy = dy / 2.0
    val hz = dz / 2.0
    val cY = cos(rotYRad)
    val sY = sin(rotYRad)

    // Local 8 corners
    val localCorners = listOf(
      Triple(-hx, -hy, -hz),
      Triple(hx, -hy, -hz),
      Triple(hx, hy, -hz),
      Triple(-hx, hy, -hz),
      Triple(-hx, -hy, hz),
      Triple(hx, -hy, hz),
      Triple(hx, hy, hz),
      Triple(-hx, hy, hz),
    )

    val verts = mutableListOf<Double>()
    for (c in localCorners) {
      // Rotate around Y axis
      val rx = c.first * cY - c.third * sY
      val rz = c.first * sY + c.third * cY
      verts.add(cx + rx)
      verts.add(cy + c.second)
      verts.add(cz + rz)
    }

    // 12 triangles (indices 0..7)
    val faces = listOf(
      // Front
      4, 5, 6, 4, 6, 7,
      // Back
      1, 0, 3, 1, 3, 2,
      // Left
      0, 4, 7, 0, 7, 3,
      // Right
      5, 1, 2, 5, 2, 6,
      // Top
      7, 6, 2, 7, 2, 3,
      // Bottom
      0, 1, 5, 0, 5, 4,
    )

    return Pair(verts, faces)
  }

  private fun getBoneBasePosition(boneId: String, scale: Double): Triple<Double, Double, Double> {
    return when (boneId) {
      "torso" -> Triple(0.0, 0.0, 0.90 * scale)
      "head" -> Triple(0.0, 0.0, 1.48 * scale)
      "lUpperArm" -> Triple(-0.24 * scale, 0.0, 1.40 * scale)
      "lForearm" -> Triple(-0.48 * scale, 0.0, 1.15 * scale)
      "rUpperArm" -> Triple(0.24 * scale, 0.0, 1.40 * scale)
      "rForearm" -> Triple(0.48 * scale, 0.0, 1.15 * scale)
      "weaponAngle" -> Triple(0.65 * scale, 0.15 * scale, 0.90 * scale)
      "lThigh" -> Triple(-0.14 * scale, 0.0, 0.88 * scale)
      "lShin" -> Triple(-0.18 * scale, 0.0, 0.46 * scale)
      "rThigh" -> Triple(0.14 * scale, 0.0, 0.88 * scale)
      "rShin" -> Triple(0.18 * scale, 0.0, 0.46 * scale)
      else -> Triple(0.0, 0.0, 1.0 * scale)
    }
  }

  private fun getPaletteForArchetype(archetype: String): Triple<String, String, String> {
    return when (archetype) {
      "biomorph" -> Triple("#991B1B", "#DC2626", "#F43F5E")
      "starblade" -> Triple("#1E1B4B", "#38BDF8", "#FDE047")
      "infernal" -> Triple("#7F1D1D", "#EA580C", "#FBBF24")
      "chrono" -> Triple("#0F172A", "#06B6D4", "#A855F7")
      else -> Triple("#1E293B", "#38BDF8", "#00E5FF") // Real-Life Blake / Operator
    }
  }

  private fun createMaterialJson(
    name: String,
    hex: String,
    roughness: Double,
    metallic: Double,
    isEmissive: Boolean = false,
  ): String {
    val emissionFields = if (isEmissive) {
      ", \"emission\": ${jsonStr(hex)}, \"emissionIntensity\": 1.8"
    } else {
      ""
    }
    return "{\"name\": ${jsonStr(name)}, \"color\": ${jsonStr(hex)}, \"roughness\": ${jsonNum(roughness)}, \"metallic\": ${jsonNum(metallic)}$emissionFields}"
  }

  private fun jsonStr(value: String): String {
    val escaped = buildString(value.length + 4) {
      for (ch in value) {
        when (ch) {
          '\\' -> append("\\\\")
          '"' -> append("\\\"")
          '\n' -> append("\\n")
          '\r' -> append("\\r")
          '\t' -> append("\\t")
          else -> if (ch < ' ') append(String.format(Locale.US, "\\u%04x", ch.code)) else append(ch)
        }
      }
    }
    return "\"$escaped\""
  }

  private fun jsonNum(value: Double): String {
    val safe = if (value.isFinite()) value else 0.0
    return String.format(Locale.US, "%.4f", safe)
  }

  private fun parseHexRgb(hex: String): Triple<Double, Double, Double> {
    val clean = hex.removePrefix("#").removePrefix("0x")
    val rgb = clean.takeLast(6).toLongOrNull(16) ?: 0x1E293BL
    val r = ((rgb shr 16) and 0xFF) / 255.0
    val g = ((rgb shr 8) and 0xFF) / 255.0
    val b = (rgb and 0xFF) / 255.0
    return Triple(r, g, b)
  }

  private fun fmt(v: Double): String = String.format(Locale.US, "%.4f", v)
}

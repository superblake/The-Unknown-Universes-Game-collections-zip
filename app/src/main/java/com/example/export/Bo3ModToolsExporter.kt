package com.example.export

import com.example.data.model.ArmoryItem
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.ui.rig.RigEvaluator
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class Bo3BoneMappingMode(
  val id: String,
  val label: String,
  val description: String,
) {
  TREYARCH_CHARACTER(
    id = "treyarch_character",
    label = "BO3 Humanoid (j_*)",
    description = "Full Treyarch AI/Player skeleton (j_mainroot, j_spine4, tag_weapon_right)",
  ),
  TREYARCH_VIEWMODEL(
    id = "treyarch_viewmodel",
    label = "BO3 Viewmodel (tag_view)",
    description = "First-person arms + tag_weapon & tag_flash sockets for weapon mods",
  ),
  UNKNOWN_NATIVE(
    id = "unknown_native",
    label = "UnknownBlend Native",
    description = "Preserves ForgeRig bone IDs (torso, lUpperArm, weaponAngle)",
  ),
}

data class Bo3ExportOptions(
  val boneMode: Bo3BoneMappingMode = Bo3BoneMappingMode.TREYARCH_CHARACTER,
  val useBo3Inches: Boolean = true,
  val fps: Int = 30,
  val includeNotetracks: Boolean = true,
) {
  val unitScale: Double get() = if (useBo3Inches) 39.37 else 1.0
}

private data class Bo3JointSpec(
  val id: String,
  val exportName: String,
  val parentIndex: Int,
  val basePosMeters: Triple<Double, Double, Double>,
  val defaultPitchDeg: Double = 0.0,
)

object Bo3ModToolsExporter {

  fun sanitizeAssetSlug(raw: String, fallback: String = "unknown_rig"): String =
    raw.lowercase(Locale.US)
      .replace(Regex("[^a-z0-9]+"), "_")
      .trim('_')
      .ifBlank { fallback }
      .take(48)

  private fun buildJointHierarchy(mode: Bo3BoneMappingMode): List<Bo3JointSpec> =
    when (mode) {
      Bo3BoneMappingMode.TREYARCH_CHARACTER -> listOf(
        Bo3JointSpec("pelvis", "j_mainroot", -1, Triple(0.0, 0.0, 0.90)),
        Bo3JointSpec("torso", "j_spine4", 0, Triple(0.0, 0.0, 0.45)),
        Bo3JointSpec("neck", "j_neck", 1, Triple(0.0, 0.0, 0.12)),
        Bo3JointSpec("head", "j_head", 2, Triple(0.0, 0.0, 0.16)),
        Bo3JointSpec("lUpperArm", "j_shoulder_le", 1, Triple(0.0, 0.19, -0.02)),
        Bo3JointSpec("lForearm", "j_elbow_le", 4, Triple(0.0, 0.24, -0.18)),
        Bo3JointSpec("lHand", "j_wrist_le", 5, Triple(0.0, 0.16, -0.20)),
        Bo3JointSpec("rUpperArm", "j_shoulder_ri", 1, Triple(0.0, -0.19, -0.02)),
        Bo3JointSpec("rForearm", "j_elbow_ri", 7, Triple(0.0, -0.24, -0.18)),
        Bo3JointSpec("rHand", "j_wrist_ri", 8, Triple(0.0, -0.16, -0.20)),
        Bo3JointSpec("weaponAngle", "tag_weapon_right", 9, Triple(0.18, 0.0, 0.02)),
        Bo3JointSpec("lThigh", "j_hip_le", 0, Triple(0.0, 0.11, -0.04)),
        Bo3JointSpec("lShin", "j_knee_le", 11, Triple(0.0, 0.03, -0.40)),
        Bo3JointSpec("lFoot", "j_ankle_le", 12, Triple(0.0, 0.01, -0.42)),
        Bo3JointSpec("rThigh", "j_hip_ri", 0, Triple(0.0, -0.11, -0.04)),
        Bo3JointSpec("rShin", "j_knee_ri", 14, Triple(0.0, -0.03, -0.40)),
        Bo3JointSpec("rFoot", "j_ankle_ri", 15, Triple(0.0, -0.01, -0.42)),
      )

      Bo3BoneMappingMode.TREYARCH_VIEWMODEL -> listOf(
        Bo3JointSpec("torso", "tag_view", -1, Triple(0.0, 0.0, 1.45)),
        Bo3JointSpec("head", "tag_torso", 0, Triple(0.0, 0.0, 0.0)),
        Bo3JointSpec("lUpperArm", "j_shoulder_le", 1, Triple(0.08, 0.22, -0.10)),
        Bo3JointSpec("lForearm", "j_elbow_le", 2, Triple(0.20, 0.10, -0.14)),
        Bo3JointSpec("lHand", "j_wrist_le", 3, Triple(0.24, -0.08, 0.05)),
        Bo3JointSpec("rUpperArm", "j_shoulder_ri", 1, Triple(0.08, -0.22, -0.10)),
        Bo3JointSpec("rForearm", "j_elbow_ri", 5, Triple(0.20, -0.10, -0.14)),
        Bo3JointSpec("rHand", "j_wrist_ri", 6, Triple(0.24, 0.02, 0.04)),
        Bo3JointSpec("weaponAngle", "tag_weapon", 7, Triple(0.16, 0.0, 0.05)),
        Bo3JointSpec("tagFlash", "tag_flash", 8, Triple(0.42, 0.0, 0.04)),
      )

      Bo3BoneMappingMode.UNKNOWN_NATIVE -> listOf(
        Bo3JointSpec("torso", "torso", -1, Triple(0.0, 0.0, 0.90)),
        Bo3JointSpec("head", "head", 0, Triple(0.0, 0.0, 0.58)),
        Bo3JointSpec("lUpperArm", "lUpperArm", 0, Triple(0.0, 0.22, 0.45)),
        Bo3JointSpec("lForearm", "lForearm", 2, Triple(0.0, 0.22, -0.24)),
        Bo3JointSpec("rUpperArm", "rUpperArm", 0, Triple(0.0, -0.22, 0.45)),
        Bo3JointSpec("rForearm", "rForearm", 4, Triple(0.0, -0.22, -0.24)),
        Bo3JointSpec("weaponAngle", "weaponAngle", 5, Triple(0.24, 0.0, 0.0)),
        Bo3JointSpec("lThigh", "lThigh", 0, Triple(0.0, 0.12, -0.05)),
        Bo3JointSpec("lShin", "lShin", 7, Triple(0.0, 0.02, -0.44)),
        Bo3JointSpec("rThigh", "rThigh", 0, Triple(0.0, -0.12, -0.05)),
        Bo3JointSpec("rShin", "rShin", 9, Triple(0.0, -0.02, -0.44)),
      )
    }

  /**
   * Generates a Call of Duty: Black Ops 3 Mod Tools compatible `.xmodel_export` (VERSION 6)
   * with full bone hierarchy, Z-up transforms, bound prism mesh geometry, normals, UVs, and materials.
   */
  fun generateXModelExport(
    rig: RigAsset,
    options: Bo3ExportOptions = Bo3ExportOptions(),
  ): String {
    val joints = buildJointHierarchy(options.boneMode)
    val safeScale = if (rig.rigScale.isFinite()) rig.rigScale.coerceIn(0.4, 3.0) else 1.0
    val scale = safeScale * options.unitScale
    val worldPositions = computeRestWorldPositions(joints, scale)
    val slug = sanitizeAssetSlug(rig.name)
    val matName = "mtl_${sanitizeAssetSlug(rig.archetype)}_$slug"

    val sb = StringBuilder(8192)
    sb.appendLine("// Generated by The Unknown Universes — UnknownBlend BO3 Mod Tools Exporter")
    sb.appendLine("// Rig: ${rig.name} (${rig.archetype}) | Skeleton: ${options.boneMode.label} | Scale: ${fmt(options.unitScale)}")
    sb.appendLine("MODEL")
    sb.appendLine("VERSION 6")
    sb.appendLine()
    sb.appendLine("NUMBONES ${joints.size}")
    joints.forEachIndexed { idx, joint ->
      sb.appendLine("BONE $idx ${joint.parentIndex} \"${joint.exportName}\"")
    }
    sb.appendLine()

    joints.forEachIndexed { idx, joint ->
      val pos = worldPositions[idx]
      val deg = rig.poseAngles[joint.id] ?: joint.defaultPitchDeg
      val m = pitchRotationMatrix(deg)
      sb.appendLine("BONE $idx")
      sb.appendLine("OFFSET ${fmt(pos.first)}, ${fmt(pos.second)}, ${fmt(pos.third)}")
      sb.appendLine("SCALE 1.000000, 1.000000, 1.000000")
      sb.appendLine("X ${fmt(m[0])}, ${fmt(m[1])}, ${fmt(m[2])}")
      sb.appendLine("Y ${fmt(m[3])}, ${fmt(m[4])}, ${fmt(m[5])}")
      sb.appendLine("Z ${fmt(m[6])}, ${fmt(m[7])}, ${fmt(m[8])}")
      sb.appendLine()
    }

    // Generate clean articulated box/limb proxy mesh bound to each joint so BO3 APE imports usable 3D geo
    data class MeshVertex(val x: Double, val y: Double, val z: Double, val boneIdx: Int)
    data class MeshTri(val v0: Int, val v1: Int, val v2: Int, val nx: Double, val ny: Double, val nz: Double)

    val vertices = mutableListOf<MeshVertex>()
    val tris = mutableListOf<MeshTri>()
    val halfSize = 0.055 * scale

    joints.forEachIndexed { boneIdx, joint ->
      val (cx, cy, cz) = worldPositions[boneIdx]
      val thickness = when {
        joint.id == "torso" || joint.id == "pelvis" -> halfSize * 1.9
        joint.id == "head" -> halfSize * 1.45
        joint.id == "weaponAngle" || joint.id == "tagFlash" -> halfSize * 0.75
        else -> halfSize
      }
      val baseV = vertices.size
      // 8 cube vertices around joint
      val corners = listOf(
        Triple(-thickness, -thickness, -thickness),
        Triple(thickness, -thickness, -thickness),
        Triple(thickness, thickness, -thickness),
        Triple(-thickness, thickness, -thickness),
        Triple(-thickness, -thickness, thickness),
        Triple(thickness, -thickness, thickness),
        Triple(thickness, thickness, thickness),
        Triple(-thickness, thickness, thickness),
      )
      corners.forEach { (ox, oy, oz) ->
        vertices.add(MeshVertex(cx + ox, cy + oy, cz + oz, boneIdx))
      }

      val boxFaces = listOf(
        Pair(listOf(0, 2, 1, 0, 3, 2), Triple(0.0, 0.0, -1.0)), // bottom
        Pair(listOf(4, 5, 6, 4, 6, 7), Triple(0.0, 0.0, 1.0)),  // top
        Pair(listOf(0, 1, 5, 0, 5, 4), Triple(0.0, -1.0, 0.0)), // front
        Pair(listOf(2, 3, 7, 2, 7, 6), Triple(0.0, 1.0, 0.0)),  // back
        Pair(listOf(1, 2, 6, 1, 6, 5), Triple(1.0, 0.0, 0.0)),  // right
        Pair(listOf(3, 0, 4, 3, 4, 7), Triple(-1.0, 0.0, 0.0)), // left
      )
      boxFaces.forEach { (indices, norm) ->
        tris.add(MeshTri(baseV + indices[0], baseV + indices[1], baseV + indices[2], norm.first, norm.second, norm.third))
        tris.add(MeshTri(baseV + indices[3], baseV + indices[4], baseV + indices[5], norm.first, norm.second, norm.third))
      }
    }

    sb.appendLine("NUMVERTS ${vertices.size}")
    vertices.forEachIndexed { idx, v ->
      sb.appendLine("VERT $idx")
      sb.appendLine("OFFSET ${fmt(v.x)}, ${fmt(v.y)}, ${fmt(v.z)}")
      sb.appendLine("BONES 1")
      sb.appendLine("BONE ${v.boneIdx} 1.000000")
    }
    sb.appendLine()

    sb.appendLine("NUMFACES ${tris.size}")
    tris.forEach { tri ->
      sb.appendLine("TRI 0 0 0 0")
      listOf(tri.v0 to Pair(0.0, 0.0), tri.v1 to Pair(1.0, 0.0), tri.v2 to Pair(0.5, 1.0)).forEach { (vIdx, uv) ->
        sb.appendLine("VERT $vIdx")
        sb.appendLine("NORMAL ${fmt(tri.nx)} ${fmt(tri.ny)} ${fmt(tri.nz)}")
        sb.appendLine("COLOR 1.000000 1.000000 1.000000 1.000000")
        sb.appendLine("UV 1 ${fmt(uv.first)} ${fmt(uv.second)}")
      }
    }
    sb.appendLine()

    sb.appendLine("NUMOBJECTS 1")
    sb.appendLine("OBJECT 0 \"${slug}_mesh\"")
    sb.appendLine()
    sb.appendLine("NUMMATERIALS 1")
    sb.appendLine("MATERIAL 0 \"$matName\" \"Phong\" \"unknown_${sanitizeAssetSlug(rig.archetype)}_c.tga\"")
    sb.appendLine("COLOR 0.000000 0.000000 0.000000 1.000000")
    sb.appendLine("TRANSPARENCY 0.000000 0.000000 0.000000 1.000000")
    sb.appendLine("AMBIENTCOLOR 1.000000 1.000000 1.000000 1.000000")
    sb.appendLine("INCANDESCENCE 0.000000 0.000000 0.000000 1.000000")
    sb.appendLine("COEFFS 0.800000 0.000000")
    sb.appendLine("GLOW 0.000000 0")
    sb.appendLine("REFRACTIVE 6 1.000000")
    sb.appendLine("SPECULARCOLOR 0.500000 0.500000 0.500000 1.000000")
    sb.appendLine("REFLECTIVECOLOR 0.000000 0.000000 0.000000 1.000000")
    sb.appendLine("REFLECTIVE 1 -1.000000")
    sb.appendLine("BLINN -1.000000 -1.000000")
    sb.appendLine("PHONG 20.000000")

    return sb.toString()
  }

  /**
   * Generates a Call of Duty: Black Ops 3 Mod Tools compatible `.xanim_export` (VERSION 3)
   * driven by real `RigEvaluator` skeleton frame evaluation + notetracks.
   */
  fun generateXAnimExport(
    rig: RigAsset,
    options: Bo3ExportOptions = Bo3ExportOptions(),
  ): String {
    val joints = buildJointHierarchy(options.boneMode)
    val safeScale = if (rig.rigScale.isFinite()) rig.rigScale.coerceIn(0.4, 3.0) else 1.0
    val safeSpeed = if (rig.playbackSpeed.isFinite()) rig.playbackSpeed.coerceIn(0.25, 3.0) else 1.0
    val scale = safeScale * options.unitScale
    val frameList = (0..60 step if (options.fps >= 60) 1 else 2).toList()
    val effectiveFps = (options.fps * safeSpeed).toInt().coerceIn(15, 120)
    val baseRest = computeRestWorldPositions(joints, scale)

    val sb = StringBuilder(12288)
    sb.appendLine("// Generated by The Unknown Universes — UnknownBlend BO3 Mod Tools Exporter")
    sb.appendLine("// Clip: ${rig.activeClip} | Rig: ${rig.name} (${rig.archetype}) | IK: ${rig.ikEnabled}")
    sb.appendLine("ANIMATION")
    sb.appendLine("VERSION 3")
    sb.appendLine()
    sb.appendLine("NUMPARTS ${joints.size}")
    joints.forEachIndexed { idx, joint ->
      sb.appendLine("PART $idx \"${joint.exportName}\"")
    }
    sb.appendLine()
    sb.appendLine("NUMKEYS ${frameList.size}")
    sb.appendLine("FRAMERATE $effectiveFps")
    sb.appendLine()

    frameList.forEachIndexed { keyIdx, simFrame ->
      val skel = RigEvaluator.evaluate(
        origin = androidx.compose.ui.geometry.Offset(0f, 0f),
        scalePx = 1.0f,
        poseAngles = rig.poseAngles,
        clip = rig.activeClip,
        frame = simFrame.toFloat(),
        ikEnabled = rig.ikEnabled,
      )
      val tRad = (simFrame / 60.0) * (2.0 * PI)
      val skelOffsets = mapSkeletonOffsetsToBo3(skel, scale)

      sb.appendLine("FRAME $keyIdx")
      joints.forEachIndexed { partIdx, joint ->
        val rest = baseRest[partIdx]
        val delta = skelOffsets[joint.id] ?: Triple(0.0, 0.0, 0.0)
        val px = rest.first + delta.first
        val py = rest.second + delta.second
        val pz = rest.third + delta.third
        val deg = (rig.poseAngles[joint.id] ?: joint.defaultPitchDeg) +
          clipDeltaDeg(rig.activeClip, joint.id, tRad)
        val m = pitchRotationMatrix(deg)

        sb.appendLine("PART $partIdx")
        sb.appendLine("OFFSET ${fmt(px)} ${fmt(py)} ${fmt(pz)}")
        sb.appendLine("SCALE 1.000000 1.000000 1.000000")
        sb.appendLine("X ${fmt(m[0])} ${fmt(m[1])} ${fmt(m[2])}")
        sb.appendLine("Y ${fmt(m[3])} ${fmt(m[4])} ${fmt(m[5])}")
        sb.appendLine("Z ${fmt(m[6])} ${fmt(m[7])} ${fmt(m[8])}")
      }
      sb.appendLine()
    }

    if (options.includeNotetracks) {
      val notetracks = buildNotetracks(rig.activeClip, frameList.size)
      sb.appendLine("NOTETRACKS")
      sb.appendLine()
      joints.forEachIndexed { partIdx, _ ->
        val partNotes = if (partIdx == 0) notetracks else emptyList()
        sb.appendLine("PART $partIdx")
        sb.appendLine("NUMTRACKS ${partNotes.size}")
        partNotes.forEachIndexed { noteIdx, (trackName, frameNum) ->
          sb.appendLine("NOTETRACK $noteIdx")
          sb.appendLine("NAME \"$trackName\"")
          sb.appendLine("FRAME $frameNum")
        }
      }
    }

    return sb.toString()
  }

  /**
   * Generates a Black Ops 3 Asset Property Editor (.GDT) definition registering the rig's
   * `xmodel`, `xanim`, and equipped Unknown Universes Armory perks/weapons + GSC hook stub.
   */
  fun generateGdtPackage(
    profile: OperatorProfile,
    armory: List<ArmoryItem>,
    rig: RigAsset,
    options: Bo3ExportOptions = Bo3ExportOptions(),
  ): String {
    val slug = sanitizeAssetSlug(rig.name)
    val modelAsset = "unknown_char_$slug"
    val animAsset = "unknown_anim_${slug}_${sanitizeAssetSlug(rig.activeClip)}"
    val isLooping = rig.activeClip in setOf("idle", "sprint")
    val equipped = armory.filter { it.equipped }

    val sb = StringBuilder(2048)
    sb.appendLine("// ============================================================================")
    sb.appendLine("// The Unknown Universes -> Call of Duty: Black Ops 3 Mod Tools (.GDT)")
    sb.appendLine("// Place in: <BO3_ROOT>/source_data/$modelAsset.gdt and rebuild in APE")
    sb.appendLine("// Operator: ${profile.callsign} | World: ${profile.activeUniverseId}")
    sb.appendLine("// ============================================================================")
    sb.appendLine("{")
    sb.appendLine("\t\"$modelAsset\" ( \"xmodel.gdf\" )")
    sb.appendLine("\t{")
    sb.appendLine("\t\t\"filename\" \"model_export\\\\unknown_universes\\\\${slug}_rig.xmodel_export\"")
    sb.appendLine("\t\t\"type\" \"animated\"")
    sb.appendLine("\t\t\"highLodDist\" \"0\"")
    sb.appendLine("\t\t\"scale\" \"1\"")
    sb.appendLine("\t\t\"collisionLOD\" \"High\"")
    sb.appendLine("\t\t\"skinOverride\" \"\"")
    sb.appendLine("\t\t\"noCastShadow\" \"0\"")
    sb.appendLine("\t}")
    sb.appendLine("\t\"$animAsset\" ( \"xanim.gdf\" )")
    sb.appendLine("\t{")
    sb.appendLine("\t\t\"filename\" \"anim_export\\\\unknown_universes\\\\${slug}_${sanitizeAssetSlug(rig.activeClip)}.xanim_export\"")
    sb.appendLine("\t\t\"model\" \"$modelAsset\"")
    sb.appendLine("\t\t\"type\" \"${if (options.boneMode == Bo3BoneMappingMode.TREYARCH_VIEWMODEL) "viewmodel" else "relative"}\"")
    sb.appendLine("\t\t\"looping\" \"${if (isLooping) "1" else "0"}\"")
    sb.appendLine("\t\t\"angleError\" \"0.05\"")
    sb.appendLine("\t\t\"translationError\" \"0.05\"")
    sb.appendLine("\t\t\"useBones\" \"1\"")
    sb.appendLine("\t}")

    equipped.take(6).forEach { item ->
      val itemSlug = sanitizeAssetSlug(item.name, "unknown_item")
      sb.appendLine("\t\"unknown_item_${itemSlug}\" ( \"xmodel.gdf\" )")
      sb.appendLine("\t{")
      sb.appendLine("\t\t\"filename\" \"model_export\\\\unknown_universes\\\\${slug}_rig.xmodel_export\"")
      sb.appendLine("\t\t\"type\" \"rigid\"")
      sb.appendLine("\t\t\"highLodDist\" \"1024\"")
      sb.appendLine("\t}")
    }
    sb.appendLine("}")
    sb.appendLine()
    sb.appendLine("/*")
    sb.appendLine("// Optional BO3 GSC Snippet (scripts/zm/_zm_unknown_loadout.gsc)")
    sb.appendLine("#precache( \"xmodel\", \"$modelAsset\" );")
    sb.appendLine("#precache( \"xanim\", \"$animAsset\" );")
    sb.appendLine("function init_unknown_operator()")
    sb.appendLine("{")
    sb.appendLine("\t// Operator: ${profile.callsign} | Essence: ${profile.totalEssence}")
    equipped.forEach { item ->
      sb.appendLine("\t// Equipped ${item.category.uppercase(Locale.US)}: ${item.name} (Tier ${item.tier}, Power ${item.powerValue}, CD ${item.cooldownSec}s)")
    }
    sb.appendLine("}")
    sb.appendLine("*/")
    return sb.toString()
  }

  /**
   * Generates a Blender (`bpy`) + `io_scene_cod` (SE2Dev CoD-Blender Addon) Python script
   * targeting Black Ops 3 Mod Tools bone hierarchy & automated `.xmodel_export`/`.xanim_export` output.
   */
  fun generateCoDBlenderBo3Script(
    rig: RigAsset,
    options: Bo3ExportOptions = Bo3ExportOptions(),
  ): String {
    val joints = buildJointHierarchy(options.boneMode)
    val safeScale = if (rig.rigScale.isFinite()) rig.rigScale.coerceIn(0.4, 3.0) else 1.0
    val worldPositions = computeRestWorldPositions(joints, safeScale)
    val slug = sanitizeAssetSlug(rig.name)
    val clipSlug = sanitizeAssetSlug(rig.activeClip)
    val boneDefs = joints.mapIndexed { idx, joint ->
      val pos = worldPositions[idx]
      val parentName = if (joint.parentIndex >= 0) "\"${joints[joint.parentIndex].exportName}\"" else "None"
      "    (\"${joint.exportName}\", $parentName, (${fmt(pos.first)}, ${fmt(pos.second)}, ${fmt(pos.third)}), \"${joint.id}\"),"
    }.joinToString("\n")
    val anglesJson = rig.poseAngles.entries.joinToString(",\n    ") { (k, v) -> "\"$k\": ${fmt(v)}" }
    val keyframes = rig.keyframeFrames.filter { it in 0L..60L }.joinToString(", ")

    return """# The Unknown Universes — CoD: Black Ops 3 Mod Tools Blender (bpy) Pipeline
# Works standalone in Blender 3.6+/4.x and supports io_scene_cod (.xmodel_export / .xanim_export)
import bpy
import math
import os

RIG_NAME = "${rig.name.replace("\"", "'")}"
BO3_SLUG = "$slug"
CLIP_NAME = "${rig.activeClip}"
SKELETON_MODE = "${options.boneMode.id}"
UNIT_SCALE = ${options.unitScale}
FPS = ${options.fps}
KEYFRAMES = [$keyframes]

BO3_JOINTS = [
$boneDefs
]

POSE_ANGLES_DEG = {
    $anglesJson
}

def build_bo3_rig():
    bpy.ops.object.armature_add(enter_editmode=True, location=(0, 0, 0))
    arm_obj = bpy.context.active_object
    arm_obj.name = f"bo3_{BO3_SLUG}"
    arm = arm_obj.data
    eb = arm.edit_bones
    if len(eb) > 0:
        eb.remove(eb[0])

    created = {}
    for b_name, parent_name, (bx, by, bz), _ in BO3_JOINTS:
        bone = eb.new(b_name)
        bone.head = (bx, by, bz)
        bone.tail = (bx, by, bz + 0.08)
        if parent_name and parent_name in created:
            bone.parent = created[parent_name]
        created[b_name] = bone

    bpy.context.scene.render.fps = FPS
    bpy.ops.object.mode_set(mode='POSE')
    for b_name, _, _, src_id in BO3_JOINTS:
        pb = arm_obj.pose.bones.get(b_name)
        if pb:
            pb.rotation_mode = 'XYZ'
            deg = POSE_ANGLES_DEG.get(src_id, 0.0)
            pb.rotation_euler = (0.0, math.radians(deg), 0.0)
            for kf in KEYFRAMES:
                pb.keyframe_insert(data_path="rotation_euler", frame=int(kf))

    bpy.ops.object.mode_set(mode='OBJECT')
    print(f"Built BO3 skeleton {arm_obj.name} ({SKELETON_MODE}, scale={UNIT_SCALE})")

    # Auto-export if CoD Blender Tools (io_scene_cod) is installed
    out_dir = bpy.path.abspath("//bo3_export")
    os.makedirs(out_dir, exist_ok=True)
    if hasattr(bpy.ops, "export_scene") and hasattr(bpy.ops.export_scene, "xmodel"):
        bpy.ops.export_scene.xmodel(
            filepath=os.path.join(out_dir, f"{BO3_SLUG}_rig.xmodel_export"),
            use_selection=True,
            global_scale=UNIT_SCALE,
        )
    if hasattr(bpy.ops, "export_scene") and hasattr(bpy.ops.export_scene, "xanim"):
        bpy.ops.export_scene.xanim(
            filepath=os.path.join(out_dir, f"{BO3_SLUG}_$clipSlug.xanim_export"),
            global_scale=UNIT_SCALE,
        )

if __name__ == "__main__":
    build_bo3_rig()
"""
  }

  private fun computeRestWorldPositions(
    joints: List<Bo3JointSpec>,
    scale: Double,
  ): List<Triple<Double, Double, Double>> {
    val result = MutableList(joints.size) { Triple(0.0, 0.0, 0.0) }
    joints.forEachIndexed { idx, joint ->
      val lx = joint.basePosMeters.first * scale
      val ly = joint.basePosMeters.second * scale
      val lz = joint.basePosMeters.third * scale
      result[idx] = if (joint.parentIndex in 0 until idx) {
        val p = result[joint.parentIndex]
        Triple(p.first + lx, p.second + ly, p.third + lz)
      } else {
        Triple(lx, ly, lz)
      }
    }
    return result
  }

  private fun mapSkeletonOffsetsToBo3(
    skel: com.example.ui.rig.EvaluatedSkeleton,
    scale: Double,
  ): Map<String, Triple<Double, Double, Double>> {
    val s = scale * 0.08
    fun delta(pt: androidx.compose.ui.geometry.Offset, ref: androidx.compose.ui.geometry.Offset): Triple<Double, Double, Double> =
      Triple(0.0, (pt.x - ref.x).toDouble() * s, -(pt.y - ref.y).toDouble() * s)

    return mapOf(
      "pelvis" to Triple(0.0, skel.pelvis.x.toDouble() * s, -skel.pelvis.y.toDouble() * s),
      "torso" to delta(skel.chest, skel.pelvis),
      "head" to delta(skel.head, skel.chest),
      "lUpperArm" to delta(skel.lElbow, skel.lShoulder),
      "lForearm" to delta(skel.lHand, skel.lElbow),
      "rUpperArm" to delta(skel.rElbow, skel.rShoulder),
      "rForearm" to delta(skel.rHand, skel.rElbow),
      "weaponAngle" to delta(skel.weaponTip, skel.rHand),
      "lThigh" to delta(skel.lKnee, skel.lHip),
      "lShin" to delta(skel.lFoot, skel.lKnee),
      "rThigh" to delta(skel.rKnee, skel.rHip),
      "rShin" to delta(skel.rFoot, skel.rKnee),
    )
  }

  private fun clipDeltaDeg(clip: String, boneId: String, tRad: Double): Double =
    when (clip) {
      "sprint" -> when (boneId) {
        "torso" -> 12.0 + sin(tRad * 2.0) * 4.0
        "lUpperArm" -> sin(tRad) * 38.0
        "rUpperArm" -> -sin(tRad) * 38.0
        "lThigh" -> -sin(tRad) * 34.0
        "rThigh" -> sin(tRad) * 34.0
        else -> 0.0
      }
      "lunge" -> when (boneId) {
        "torso" -> 18.0 + sin(tRad) * 10.0
        "rUpperArm" -> -45.0 + sin(tRad) * 55.0
        "weaponAngle" -> sin(tRad) * 28.0
        else -> 0.0
      }
      "slam" -> when (boneId) {
        "torso" -> -10.0 + cos(tRad) * 20.0
        "rUpperArm" -> -95.0 + cos(tRad) * 65.0
        "lUpperArm" -> -70.0 + cos(tRad) * 45.0
        "weaponAngle" -> cos(tRad) * 40.0
        else -> 0.0
      }
      "vault" -> when (boneId) {
        "torso" -> 24.0
        "lThigh" -> -40.0 * maxOf(0.0, sin(tRad))
        "rThigh" -> -32.0 * maxOf(0.0, sin(tRad))
        else -> 0.0
      }
      "inspect" -> when (boneId) {
        "head" -> sin(tRad) * 12.0
        "rUpperArm" -> -15.0 + sin(tRad) * 14.0
        "weaponAngle" -> sin(tRad) * 32.0
        else -> 0.0
      }
      else -> when (boneId) {
        "torso" -> sin(tRad) * 2.5
        "head" -> cos(tRad * 0.5) * 3.0
        else -> 0.0
      }
    }

  private fun buildNotetracks(clip: String, numKeys: Int): List<Pair<String, Int>> {
    val mid = (numKeys / 2).coerceAtLeast(1)
    return when (clip) {
      "sprint" -> listOf("footstep_left" to 1, "footstep_right" to mid)
      "lunge" -> listOf("melee_swing" to (mid / 2).coerceAtLeast(1), "melee_hit" to mid)
      "slam" -> listOf("jump_takeoff" to 1, "slam_impact" to mid)
      "vault" -> listOf("mantle_grab" to 1, "mantle_land" to (numKeys - 2).coerceAtLeast(2))
      "inspect" -> listOf("inspect_start" to 0, "inspect_flip" to mid)
      else -> listOf("idle_breath" to 0)
    }
  }

  private fun pitchRotationMatrix(deg: Double): DoubleArray {
    val rad = (if (deg.isNaN() || deg.isInfinite()) 0.0 else deg) * (PI / 180.0)
    val c = cos(rad)
    val s = sin(rad)
    val len = hypot(c, s).takeIf { it > 1e-9 } ?: 1.0
    val nc = c / len
    val ns = s / len
    // Clean orthonormal Y-pitch 3x3 matrix (X, Y, Z basis rows)
    return doubleArrayOf(
      nc, 0.0, -ns,
      0.0, 1.0, 0.0,
      ns, 0.0, nc,
    )
  }

  private fun fmt(v: Double): String {
    val safe = if (v.isNaN() || v.isInfinite()) 0.0 else v
    return String.format(Locale.US, "%.6f", safe)
  }
}

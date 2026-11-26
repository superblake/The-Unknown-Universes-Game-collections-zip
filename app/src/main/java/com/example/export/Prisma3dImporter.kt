package com.example.export

import com.example.data.model.ArmoryItem
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.data.remote.FirestoreSyncCodec
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.min

data class Prisma3dSceneStats(
  val sourceFormat: String,
  val objectCount: Int,
  val boneCount: Int,
  val vertexCount: Int,
  val faceCount: Int,
  val animationCount: Int,
  val keyframeCount: Int,
  val textureNames: List<String> = emptyList(),
)

data class Prisma3dImportResult(
  val rig: RigAsset,
  val armoryModule: ArmoryItem,
  val stats: Prisma3dSceneStats,
  val summary: String,
) {
  fun toWorkshopPreview(targetUserId: String): SteamWorkshopImportPreview {
    return SteamWorkshopImportPreview(
      sourceKind = "PRISMA3D_IMPORT",
      title = rig.name,
      description = summary,
      publishedFileId = "prisma3d_${rig.id}",
      appId = "311210",
      creatorSteamId = "Prisma3D_Creator",
      tags = listOf("Prisma3D", stats.sourceFormat, rig.archetype),
      subscriptions = 1L,
      views = 1L,
      armoryItems = listOf(armoryModule.copy(userId = targetUserId)),
      customRigs = listOf(rig.copy(userId = targetUserId)),
      blenderRigScript = PortablePackageGenerator.generateBlenderRigScript(rig),
      generatedModule = armoryModule.copy(userId = targetUserId),
      generatedRig = rig.copy(userId = targetUserId),
      sourceNote = summary,
    )
  }
}

object Prisma3dImporter {

  const val PRISMA3D_TAG = "Prisma3D Import"
  private const val MAX_ARCHIVE_BYTES = 12 * 1024 * 1024
  private const val MAX_JSON_ENTRY_BYTES = 2 * 1024 * 1024

  val samplePresets: List<Pair<String, String>> = listOf(
    "real_life_blake" to "Blake Real-Life Rig (.p3d)",
    "cyber_vanguard" to "Cyber Vanguard Rig (.prisma)",
    "biomorph_runner" to "Bio-Armored Runner (.gltf)",
    "starblade_mecha" to "Starblade Mecha Rig (.obj)",
  )

  fun looksLikePrisma3dFile(fileName: String, bytes: ByteArray): Boolean {
    val lower = fileName.lowercase(Locale.US)
    if (
      lower.endsWith(".p3d") ||
      lower.endsWith(".prisma") ||
      lower.endsWith(".prisma3d") ||
      lower.endsWith(".obj") ||
      lower.endsWith(".gltf") ||
      lower.endsWith(".glb")
    ) {
      return true
    }
    if (isZipArchive(bytes) || isGlbBinary(bytes)) return true
    val head = bytes.take(1024).toByteArray().toString(Charsets.UTF_8)
    val headLower = head.lowercase(Locale.US)
    return headLower.contains("\"objects\"") && (headLower.contains("\"bones\"") || headLower.contains("\"prisma\"") || headLower.contains("\"keyframes\"")) ||
      (headLower.contains("\nv ") && (headLower.contains("\nf ") || headLower.contains("\no ")))
  }

  fun importFromBytes(
    bytes: ByteArray,
    fileName: String = "model.p3d",
    targetUserId: String = "local_operator",
  ): Result<Prisma3dImportResult> {
    if (bytes.isEmpty()) {
      return Result.failure(IllegalArgumentException("Prisma3D file is empty."))
    }
    if (bytes.size > MAX_ARCHIVE_BYTES) {
      return Result.failure(IllegalArgumentException("Prisma3D file exceeds 12 MB limit."))
    }

    return try {
      when {
        isZipArchive(bytes) -> parseZipArchive(bytes, fileName, targetUserId)
        isGlbBinary(bytes) -> parseGlbBytes(bytes, fileName, targetUserId)
        else -> {
          val text = bytes.toString(Charsets.UTF_8)
          importFromText(text, fileName, targetUserId)
        }
      }
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("Could not parse Prisma3D file '$fileName': ${e.localizedMessage ?: "Invalid format"}"))
    }
  }

  fun importFromText(
    rawText: String,
    fileName: String = "scene.prisma",
    targetUserId: String = "local_operator",
  ): Result<Prisma3dImportResult> {
    val trimmed = rawText.trim()
    if (trimmed.isEmpty()) {
      return Result.failure(IllegalArgumentException("Prisma3D scene text is empty."))
    }

    return try {
      if (trimmed.startsWith("{")) {
        val root = JSONObject(trimmed)
        parseJsonScene(root, fileName, targetUserId, extraObjText = null, textureNames = emptyList())
      } else if (trimmed.contains("\nv ") || trimmed.startsWith("v ") || trimmed.startsWith("o ") || trimmed.startsWith("# Prisma3D", ignoreCase = true) || trimmed.startsWith("# Blender", ignoreCase = true)) {
        parseWavefrontObj(trimmed, fileName, targetUserId)
      } else {
        // Fallback descriptor (e.g., user pasted bone/rig notes from Prisma3D)
        parsePrismaDescriptorText(trimmed, fileName, targetUserId)
      }
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("Failed to import Prisma3D data: ${e.localizedMessage ?: "Unsupported content"}"))
    }
  }

  fun getSamplePreset(presetKey: String, targetUserId: String): Prisma3dImportResult {
    val now = System.currentTimeMillis()
    val (title, arch, clip, pose, scale) = when (presetKey) {
      "real_life_blake" -> SampleTuple(
        "Blake Prisma3D Casual Rig",
        "operator",
        "idle",
        UniverseCatalog.defaultPoseMap() + mapOf("torso" to -88.0, "rUpperArm" to 22.0, "lUpperArm" to 148.0),
        1.0,
      )
      "biomorph_runner" -> SampleTuple(
        "Prisma3D Bio-Runner Rig",
        "biomorph",
        "sprint",
        UniverseCatalog.defaultPoseMap() + mapOf("torso" to -74.0, "lThigh" to 128.0, "rThigh" to 52.0, "rUpperArm" to 38.0),
        1.05,
      )
      "starblade_mecha" -> SampleTuple(
        "Prisma3D Starblade Mecha",
        "starblade",
        "lunge",
        UniverseCatalog.defaultPoseMap() + mapOf("torso" to -78.0, "rUpperArm" to -10.0, "weaponAngle" to 14.0),
        1.15,
      )
      else -> SampleTuple(
        "Cyber Vanguard Prisma3D",
        "chrono",
        "inspect",
        UniverseCatalog.defaultPoseMap() + mapOf("torso" to -84.0, "rUpperArm" to 12.0, "rForearm" to -28.0),
        1.02,
      )
    }

    val safeId = FirestoreSyncCodec.sanitizeId("prisma3d_${presetKey}_${java.util.UUID.randomUUID().toString().take(8)}", "prisma3d_rig")
    val rig = RigAsset(
      id = safeId,
      userId = targetUserId,
      name = title,
      archetype = arch,
      activeClip = clip,
      ikEnabled = true,
      rigScale = scale,
      playbackSpeed = 1.0,
      poseAngles = pose,
      keyframeFrames = listOf(0L, 12L, 24L, 38L, 60L),
      isCompleted = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    )
    val module = buildArmoryModule(rig, targetUserId, "Prisma3D (.p3d)", boneCount = 12, vertexCount = 2480, now = now)
    val stats = Prisma3dSceneStats(
      sourceFormat = "Prisma3D (.p3d)",
      objectCount = 14,
      boneCount = 12,
      vertexCount = 2480,
      faceCount = 4320,
      animationCount = 1,
      keyframeCount = 5,
    )
    return Prisma3dImportResult(
      rig = rig,
      armoryModule = module,
      stats = stats,
      summary = "Imported '$title' from Prisma3D (.p3d) • 12 bones, 2,480 verts, 5 keyframes",
    )
  }

  private data class SampleTuple(
    val title: String,
    val arch: String,
    val clip: String,
    val pose: Map<String, Double>,
    val scale: Double,
  )

  private fun isZipArchive(bytes: ByteArray): Boolean {
    return bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
      (bytes[2] == 0x03.toByte() || bytes[2] == 0x05.toByte())
  }

  private fun isGlbBinary(bytes: ByteArray): Boolean {
    return bytes.size >= 12 &&
      bytes[0] == 'g'.code.toByte() &&
      bytes[1] == 'l'.code.toByte() &&
      bytes[2] == 'T'.code.toByte() &&
      bytes[3] == 'F'.code.toByte()
  }

  private fun parseZipArchive(
    bytes: ByteArray,
    fileName: String,
    targetUserId: String,
  ): Result<Prisma3dImportResult> {
    var primaryJsonText: String? = null
    var objText: String? = null
    val textureNames = mutableListOf<String>()
    var entryCount = 0

    ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
      var entry = zis.nextEntry
      while (entry != null && entryCount < 64) {
        if (!entry.isDirectory) {
          entryCount++
          val entryLower = entry.name.lowercase(Locale.US)
          when {
            entryLower.endsWith(".json") || entryLower.endsWith(".gltf") || entryLower.endsWith(".prisma") || entryLower.endsWith(".p3d") -> {
              val data = readBoundedEntry(zis, MAX_JSON_ENTRY_BYTES)
              val text = data.toString(Charsets.UTF_8).trim()
              if (text.startsWith("{")) {
                val isPrimary = entryLower.contains("scene") || entryLower.contains("project") || entryLower.contains("model")
                if (primaryJsonText == null || isPrimary) {
                  primaryJsonText = text
                }
              }
            }
            entryLower.endsWith(".obj") -> {
              if (objText == null) {
                objText = readBoundedEntry(zis, MAX_JSON_ENTRY_BYTES).toString(Charsets.UTF_8)
              }
            }
            entryLower.endsWith(".png") || entryLower.endsWith(".jpg") || entryLower.endsWith(".jpeg") || entryLower.endsWith(".webp") -> {
              textureNames.add(entry.name.substringAfterLast('/'))
            }
          }
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }

    val json = primaryJsonText
    if (json != null) {
      val root = JSONObject(json)
      return parseJsonScene(root, fileName, targetUserId, extraObjText = objText, textureNames = textureNames)
    }

    val obj = objText
    if (obj != null) {
      return parseWavefrontObj(obj, fileName, targetUserId, sourceFormat = "Prisma3D Archive (.p3d/.obj)", textureNames = textureNames)
    }

    // Valid archive with named entries/textures but no recognizable JSON/OBJ payload
    val baseTitle = cleanDisplayTitle(fileName, "Prisma3D Project Rig")
    val now = System.currentTimeMillis()
    val rig = RigAsset(
      id = FirestoreSyncCodec.sanitizeId("prisma3d_${baseTitle.hashCode().toUInt()}_${java.util.UUID.randomUUID().toString().take(8)}", "prisma3d_rig"),
      userId = targetUserId,
      name = baseTitle,
      archetype = inferArchetype("$fileName ${textureNames.joinToString(" ")}"),
      activeClip = "idle",
      ikEnabled = true,
      rigScale = 1.0,
      playbackSpeed = 1.0,
      poseAngles = UniverseCatalog.defaultPoseMap(),
      keyframeFrames = listOf(0L, 15L, 30L, 45L, 60L),
      isCompleted = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    )
    val module = buildArmoryModule(rig, targetUserId, "Prisma3D Archive", boneCount = entryCount.coerceAtLeast(1), vertexCount = 0, now = now)
    val stats = Prisma3dSceneStats(
      sourceFormat = "Prisma3D (.p3d)",
      objectCount = entryCount.coerceAtLeast(1),
      boneCount = 11,
      vertexCount = 0,
      faceCount = 0,
      animationCount = 1,
      keyframeCount = 5,
      textureNames = textureNames,
    )
    return Result.success(
      Prisma3dImportResult(
        rig = rig,
        armoryModule = module,
        stats = stats,
        summary = "Imported Prisma3D archive '$baseTitle' (${stats.objectCount} entries, ${textureNames.size} texture(s))",
      )
    )
  }

  private fun parseGlbBytes(
    bytes: ByteArray,
    fileName: String,
    targetUserId: String,
  ): Result<Prisma3dImportResult> {
    if (bytes.size < 20) {
      return Result.failure(IllegalArgumentException("GLB header too short."))
    }
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    buf.position(12) // skip magic (4), version (4), total length (4)
    val chunkLength = buf.int
    val chunkType = buf.int
    // 0x4E4F534A == "JSON"
    if (chunkType != 0x4E4F534A || chunkLength <= 0 || 20 + chunkLength > bytes.size) {
      return Result.failure(IllegalArgumentException("Invalid GLB JSON chunk."))
    }
    val jsonStr = String(bytes, 20, chunkLength, Charsets.UTF_8).trim()
    val root = JSONObject(jsonStr)
    return parseJsonScene(root, fileName, targetUserId, extraObjText = null, textureNames = emptyList(), forcedFormat = "Prisma3D GLB (.glb)")
  }

  private fun parseJsonScene(
    root: JSONObject,
    fileName: String,
    targetUserId: String,
    extraObjText: String?,
    textureNames: List<String>,
    forcedFormat: String? = null,
  ): Result<Prisma3dImportResult> {
    val now = System.currentTimeMillis()
    val sceneTitle = listOf(
      root.optString("name", ""),
      root.optString("title", ""),
      root.optString("projectName", ""),
    ).firstOrNull { it.isNotBlank() }?.let { cleanDisplayTitle(it, "Prisma3D Rig") }
      ?: cleanDisplayTitle(fileName, "Prisma3D Rig")

    val poseAngles = UniverseCatalog.defaultPoseMap().toMutableMap()
    val discoveredObjectNames = mutableListOf<String>()
    var boneCount = 0
    var meshVertexCount = 0
    var meshFaceCount = 0
    val keyframeSet = sortedSetOf<Long>()
    var detectedClip = "idle"
    var maxScale = 1.0
    var detectedFps = root.optDouble("fps", root.optDouble("frameRate", 30.0)).takeIf { it.isFinite() && it > 0 } ?: 30.0

    fun inspectNode(obj: JSONObject, depth: Int) {
      if (depth > 16) return
      val nName = obj.optString("name", obj.optString("id", ""))
      if (nName.isNotBlank()) discoveredObjectNames.add(nName)
      val typeStr = obj.optString("type", "").lowercase(Locale.US)
      if (typeStr.contains("bone") || typeStr.contains("joint") || obj.has("bone") || obj.optBoolean("isBone", false)) {
        boneCount++
      }

      // Extract vertex/face counts if mesh arrays present
      obj.optJSONArray("vertices")?.let { meshVertexCount += it.length() / 3 }
      obj.optJSONArray("positions")?.let { meshVertexCount += it.length() / 3 }
      obj.optJSONArray("faces")?.let { meshFaceCount += it.length() }
      obj.optJSONArray("indices")?.let { meshFaceCount += it.length() / 3 }
      obj.optJSONObject("mesh")?.let { meshObj ->
        meshObj.optJSONArray("vertices")?.let { meshVertexCount += it.length() / 3 }
        meshObj.optJSONArray("faces")?.let { meshFaceCount += it.length() }
      }

      // Extract bone rotation into UnknownBlend pose angles
      val rotVec = readVec3(obj.opt("rotation") ?: obj.opt("rot"))
      if (rotVec != null && nName.isNotBlank()) {
        mapBoneRotationToPose(nName, rotVec, poseAngles)
      }
      val scaleVec = readVec3(obj.opt("scale"))
      if (scaleVec != null) {
        val avgScale = ((scaleVec[0] + scaleVec[1] + scaleVec[2]) / 3.0)
        if (avgScale.isFinite() && avgScale in 0.5..2.0) {
          maxScale = avgScale.coerceIn(0.7, 1.4)
        }
      }

      // Keyframes attached directly to node
      (obj.optJSONArray("keyframes") ?: obj.optJSONArray("frames"))?.let { kfArr ->
        for (i in 0 until kfArr.length().coerceAtMost(100)) {
          val elem = kfArr.opt(i)
          val frameNum = when (elem) {
            is Number -> elem.toLong()
            is JSONObject -> elem.optLong("frame", elem.optLong("time", -1L))
            else -> -1L
          }
          if (frameNum in 0L..600L) {
            keyframeSet.add(((frameNum * 60L) / detectedFps.toLong().coerceAtLeast(1L)).coerceIn(0L, 60L))
          }
        }
      }

      val childKeys = listOf("children", "objects", "bones", "nodes", "parts", "elements")
      childKeys.forEach { key ->
        obj.optJSONArray(key)?.let { arr ->
          for (i in 0 until arr.length().coerceAtMost(128)) {
            arr.optJSONObject(i)?.let { inspectNode(it, depth + 1) }
          }
        }
      }
    }

    inspectNode(root, 0)

    // glTF skins/joints
    root.optJSONArray("skins")?.let { skins ->
      for (i in 0 until skins.length()) {
        skins.optJSONObject(i)?.optJSONArray("joints")?.let { joints ->
          boneCount = max(boneCount, joints.length())
        }
      }
    }

    // Meshes array (glTF or Prisma3D root meshes)
    root.optJSONArray("meshes")?.let { meshes ->
      for (i in 0 until meshes.length().coerceAtMost(64)) {
        val m = meshes.optJSONObject(i) ?: continue
        m.optString("name", "").takeIf { it.isNotBlank() }?.let { discoveredObjectNames.add(it) }
        m.optJSONArray("vertices")?.let { meshVertexCount += it.length() / 3 }
        m.optJSONArray("primitives")?.let { prims ->
          meshFaceCount += prims.length() * 64
        }
      }
    }

    // Animations array
    var animCount = 0
    (root.optJSONArray("animations") ?: root.optJSONArray("clips"))?.let { anims ->
      animCount = anims.length()
      for (i in 0 until anims.length().coerceAtMost(16)) {
        val animObj = anims.optJSONObject(i) ?: continue
        val aName = animObj.optString("name", "").lowercase(Locale.US)
        if (i == 0 && aName.isNotBlank()) {
          detectedClip = inferClipName(aName)
        }
        animObj.optJSONArray("keyframes")?.let { kfs ->
          for (k in 0 until kfs.length().coerceAtMost(64)) {
            val f = kfs.optJSONObject(k)?.optLong("frame", -1L) ?: -1L
            if (f >= 0L) keyframeSet.add(f.coerceIn(0L, 60L))
          }
        }
      }
    }

    if (extraObjText != null) {
      val objStats = inspectObjMetrics(extraObjText)
      meshVertexCount = max(meshVertexCount, objStats.vertexCount)
      meshFaceCount = max(meshFaceCount, objStats.faceCount)
      discoveredObjectNames.addAll(objStats.objectNames)
    }

    val resolvedKeyframes = if (keyframeSet.isEmpty()) {
      listOf(0L, 15L, 30L, 45L, 60L)
    } else {
      (listOf(0L) + keyframeSet.toList()).distinct().sorted().take(20)
    }

    val combinedHints = "$sceneTitle $fileName ${discoveredObjectNames.joinToString(" ")} ${textureNames.joinToString(" ")}"
    val archetype = inferArchetype(combinedHints)
    val speed = (detectedFps / 30.0).coerceIn(0.5, 2.0)
    val formatLabel = forcedFormat ?: when {
      root.has("asset") && root.optJSONObject("asset")?.optString("version")?.startsWith("2") == true -> "glTF 2.0 (.gltf)"
      fileName.lowercase(Locale.US).endsWith(".prisma") -> "Prisma3D (.prisma)"
      else -> "Prisma3D (.p3d)"
    }

    val resolvedBoneCount = max(boneCount, discoveredObjectNames.count {
      val l = it.lowercase(Locale.US)
      l.contains("arm") || l.contains("leg") || l.contains("head") || l.contains("spine") || l.contains("torso") || l.contains("bone")
    }).coerceAtLeast(1)

    val rig = RigAsset(
      id = FirestoreSyncCodec.sanitizeId("prisma3d_${sceneTitle.hashCode().toUInt()}_${UUID.randomUUID().toString().take(4)}", "prisma3d_rig"),
      userId = targetUserId,
      name = sceneTitle,
      archetype = archetype,
      activeClip = detectedClip,
      ikEnabled = true,
      rigScale = maxScale.coerceIn(0.7, 1.4),
      playbackSpeed = speed,
      poseAngles = poseAngles,
      keyframeFrames = resolvedKeyframes,
      isCompleted = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    )
    val module = buildArmoryModule(rig, targetUserId, formatLabel, resolvedBoneCount, meshVertexCount, now)
    val stats = Prisma3dSceneStats(
      sourceFormat = formatLabel,
      objectCount = discoveredObjectNames.size.coerceAtLeast(1),
      boneCount = resolvedBoneCount,
      vertexCount = meshVertexCount,
      faceCount = meshFaceCount,
      animationCount = animCount.coerceAtLeast(1),
      keyframeCount = resolvedKeyframes.size,
      textureNames = textureNames,
    )
    return Result.success(
      Prisma3dImportResult(
        rig = rig,
        armoryModule = module,
        stats = stats,
        summary = "Imported '$sceneTitle' ($formatLabel • ${stats.objectCount} objects, ${stats.boneCount} bones, ${stats.keyframeCount} keyframes)",
      )
    )
  }

  private fun parseWavefrontObj(
    objText: String,
    fileName: String,
    targetUserId: String,
    sourceFormat: String = "Prisma3D OBJ (.obj)",
    textureNames: List<String> = emptyList(),
  ): Result<Prisma3dImportResult> {
    val now = System.currentTimeMillis()
    val metrics = inspectObjMetrics(objText)
    if (metrics.vertexCount == 0 && metrics.objectNames.isEmpty()) {
      return Result.failure(IllegalArgumentException("No vertices or objects found in .obj file."))
    }
    val title = metrics.objectNames.firstOrNull()?.let { cleanDisplayTitle(it, "Prisma3D OBJ Rig") }
      ?: cleanDisplayTitle(fileName, "Prisma3D OBJ Rig")
    val hints = "$title $fileName ${metrics.objectNames.joinToString(" ")}"
    val archetype = inferArchetype(hints)
    val estimatedScale = (metrics.heightExtent / 1.8).takeIf { it.isFinite() && it > 0.1 }?.coerceIn(0.75, 1.35) ?: 1.0

    val rig = RigAsset(
      id = FirestoreSyncCodec.sanitizeId("prisma3d_obj_${title.hashCode().toUInt()}_${UUID.randomUUID().toString().take(4)}", "prisma3d_obj"),
      userId = targetUserId,
      name = title,
      archetype = archetype,
      activeClip = "idle",
      ikEnabled = true,
      rigScale = estimatedScale,
      playbackSpeed = 1.0,
      poseAngles = UniverseCatalog.defaultPoseMap(),
      keyframeFrames = listOf(0L, 15L, 30L, 45L, 60L),
      isCompleted = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    )
    val module = buildArmoryModule(rig, targetUserId, sourceFormat, metrics.objectNames.size.coerceAtLeast(1), metrics.vertexCount, now)
    val stats = Prisma3dSceneStats(
      sourceFormat = sourceFormat,
      objectCount = metrics.objectNames.size.coerceAtLeast(1),
      boneCount = metrics.objectNames.size.coerceAtLeast(1),
      vertexCount = metrics.vertexCount,
      faceCount = metrics.faceCount,
      animationCount = 1,
      keyframeCount = 5,
      textureNames = textureNames,
    )
    return Result.success(
      Prisma3dImportResult(
        rig = rig,
        armoryModule = module,
        stats = stats,
        summary = "Imported '$title' ($sourceFormat • ${metrics.vertexCount} verts, ${metrics.faceCount} faces)",
      )
    )
  }

  private fun parsePrismaDescriptorText(
    rawText: String,
    fileName: String,
    targetUserId: String,
  ): Result<Prisma3dImportResult> {
    val now = System.currentTimeMillis()
    val firstLine = rawText.lines().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    val title = cleanDisplayTitle(firstLine, cleanDisplayTitle(fileName, "Prisma3D Rig"))
    val rig = RigAsset(
      id = FirestoreSyncCodec.sanitizeId("prisma3d_txt_${title.hashCode().toUInt()}_${UUID.randomUUID().toString().take(4)}", "prisma3d_txt"),
      userId = targetUserId,
      name = title,
      archetype = inferArchetype("$title $rawText"),
      activeClip = inferClipName(rawText),
      ikEnabled = true,
      rigScale = 1.0,
      playbackSpeed = 1.0,
      poseAngles = UniverseCatalog.defaultPoseMap(),
      keyframeFrames = listOf(0L, 15L, 30L, 45L, 60L),
      isCompleted = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    )
    val module = buildArmoryModule(rig, targetUserId, "Prisma3D Scene Note", boneCount = 11, vertexCount = 0, now = now)
    val stats = Prisma3dSceneStats(
      sourceFormat = "Prisma3D Scene",
      objectCount = 1,
      boneCount = 11,
      vertexCount = 0,
      faceCount = 0,
      animationCount = 1,
      keyframeCount = 5,
    )
    return Result.success(
      Prisma3dImportResult(
        rig = rig,
        armoryModule = module,
        stats = stats,
        summary = "Imported '$title' as playable Prisma3D rig (${rig.archetype})",
      )
    )
  }

  private fun readBoundedEntry(zis: ZipInputStream, maxBytes: Int): ByteArray {
    val buffer = ByteArray(4096)
    val out = ArrayList<Byte>(min(maxBytes, 8192))
    var total = 0
    while (total < maxBytes) {
      val read = zis.read(buffer, 0, min(buffer.size, maxBytes - total))
      if (read <= 0) break
      for (i in 0 until read) out.add(buffer[i])
      total += read
    }
    return out.toByteArray()
  }

  private data class ObjMetrics(
    val objectNames: List<String>,
    val vertexCount: Int,
    val faceCount: Int,
    val heightExtent: Double,
  )

  private fun inspectObjMetrics(objText: String): ObjMetrics {
    val names = linkedSetOf<String>()
    var verts = 0
    var faces = 0
    var minY = Double.POSITIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    for (line in objText.lineSequence()) {
      val t = line.trim()
      when {
        t.startsWith("o ") || t.startsWith("g ") -> {
          val label = t.drop(2).trim()
          if (label.isNotBlank()) names.add(label)
        }
        t.startsWith("v ") -> {
          verts++
          val parts = t.split(Regex("\\s+"))
          if (parts.size >= 3) {
            val y = parts[2].toDoubleOrNull()
            if (y != null && y.isFinite()) {
              minY = min(minY, y)
              maxY = max(maxY, y)
            }
          }
        }
        t.startsWith("f ") -> faces++
      }
    }
    val extent = if (minY.isFinite() && maxY.isFinite()) (maxY - minY).coerceAtLeast(0.2) else 1.8
    return ObjMetrics(names.toList(), verts, faces, extent)
  }

  private fun readVec3(raw: Any?): DoubleArray? {
    return when (raw) {
      is JSONArray -> if (raw.length() >= 3) {
        doubleArrayOf(
          raw.optDouble(0, 0.0),
          raw.optDouble(1, 0.0),
          raw.optDouble(2, 0.0),
        )
      } else null
      is JSONObject -> doubleArrayOf(
        raw.optDouble("x", 0.0),
        raw.optDouble("y", 0.0),
        raw.optDouble("z", 0.0),
      )
      else -> null
    }
  }

  private fun mapBoneRotationToPose(
    boneName: String,
    rotVec: DoubleArray,
    poseMap: MutableMap<String, Double>,
  ) {
    val lower = boneName.lowercase(Locale.US)
    val primaryDeg = rotVec[2].let { z ->
      if (kotlin.math.abs(z) > 0.01) z else rotVec[0]
    }
    if (!primaryDeg.isFinite()) return

    val targetBoneId = when {
      lower.contains("head") -> "head"
      lower.contains("torso") || lower.contains("spine") || lower.contains("chest") -> "torso"
      (lower.contains("l_") || lower.contains("left")) && lower.contains("forearm") -> "lForearm"
      (lower.contains("l_") || lower.contains("left")) && (lower.contains("arm") || lower.contains("shoulder")) -> "lUpperArm"
      (lower.contains("r_") || lower.contains("right")) && lower.contains("forearm") -> "rForearm"
      (lower.contains("r_") || lower.contains("right")) && (lower.contains("arm") || lower.contains("shoulder")) -> "rUpperArm"
      (lower.contains("l_") || lower.contains("left")) && (lower.contains("thigh") || lower.contains("upleg") || lower.contains("hip")) -> "lThigh"
      (lower.contains("l_") || lower.contains("left")) && (lower.contains("shin") || lower.contains("calf") || lower.contains("leg")) -> "lShin"
      (lower.contains("r_") || lower.contains("right")) && (lower.contains("thigh") || lower.contains("upleg") || lower.contains("hip")) -> "rThigh"
      (lower.contains("r_") || lower.contains("right")) && (lower.contains("shin") || lower.contains("calf") || lower.contains("leg")) -> "rShin"
      lower.contains("weapon") || lower.contains("sword") || lower.contains("gun") -> "weaponAngle"
      else -> null
    } ?: return

    val spec = UniverseCatalog.rigBones.firstOrNull { it.id == targetBoneId } ?: return
    val base = poseMap[targetBoneId] ?: spec.defaultAngle.toDouble()
    poseMap[targetBoneId] = (base + primaryDeg * 0.45).coerceIn(spec.minAngle.toDouble(), spec.maxAngle.toDouble())
  }

  private fun inferArchetype(hints: String): String {
    val l = hints.lowercase(Locale.US)
    return when {
      l.contains("bio") || l.contains("mercer") || l.contains("symbiote") || l.contains("venom") || l.contains("zombie") -> "biomorph"
      l.contains("infernal") || l.contains("doom") || l.contains("slayer") || l.contains("fire") || l.contains("hades") -> "infernal"
      l.contains("chrono") || l.contains("cole") || l.contains("spark") || l.contains("electric") || l.contains("sonic") -> "chrono"
      l.contains("star") || l.contains("saber") || l.contains("jedi") || l.contains("mecha") || l.contains("iron") -> "starblade"
      else -> "operator"
    }
  }

  private fun inferClipName(hints: String): String {
    val l = hints.lowercase(Locale.US)
    return when {
      l.contains("sprint") || l.contains("run") -> "sprint"
      l.contains("lunge") || l.contains("attack") || l.contains("slash") -> "lunge"
      l.contains("slam") || l.contains("jump") -> "slam"
      l.contains("vault") || l.contains("parkour") -> "vault"
      l.contains("inspect") -> "inspect"
      else -> "idle"
    }
  }

  private fun cleanDisplayTitle(raw: String, fallback: String): String {
    val noExt = raw.substringAfterLast('/').substringAfterLast('\\')
      .replace(Regex("\\.(p3d|prisma3d|prisma|obj|gltf|glb|json|zip)$", RegexOption.IGNORE_CASE), "")
    val clean = noExt.replace('_', ' ').replace(Regex("[^a-zA-Z0-9 \\-.]"), " ")
      .replace(Regex("\\s+"), " ")
      .trim()
      .take(48)
    return clean.ifBlank { fallback }
  }

  private fun buildArmoryModule(
    rig: RigAsset,
    targetUserId: String,
    formatLabel: String,
    boneCount: Int,
    vertexCount: Int,
    now: Long,
  ): ArmoryItem {
    val power = (240L + boneCount * 12L + (vertexCount / 200L)).coerceIn(220L, 950L)
    return ArmoryItem(
      id = FirestoreSyncCodec.sanitizeId("prisma_mod_${rig.id}", "prisma_mod"),
      userId = targetUserId,
      name = "${rig.name} Core",
      category = "upgrade",
      universeTag = PRISMA3D_TAG,
      description = "Imported $formatLabel rig (${rig.archetype}, $boneCount bones, $vertexCount verts) — boosts Armor & Precision up to Tier 25.",
      tier = 1L,
      maxTier = 25L,
      cooldownSec = 0L,
      powerValue = power,
      accentColorHex = "#38BDF8",
      equipped = true,
      isCustom = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    )
  }
}

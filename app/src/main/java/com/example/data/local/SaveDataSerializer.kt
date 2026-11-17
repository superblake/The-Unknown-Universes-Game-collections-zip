package com.example.data.local

import com.example.BuildConfig
import com.example.data.model.ArmoryItem
import com.example.data.model.MatchRecord
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.data.remote.FirestoreSyncCodec
import org.json.JSONArray
import org.json.JSONObject

data class SaveDataBundle(
  val schemaVersion: Int,
  val exportedAtMillis: Long,
  val sourceAppVersion: String,
  val profile: OperatorProfile,
  val armoryItems: List<ArmoryItem>,
  val customRigs: List<RigAsset>,
  val matchLogs: List<MatchRecord>,
)

data class SaveBundlePreview(
  val callsign: String,
  val sourceAppVersion: String,
  val totalEssence: Long,
  val highScore: Long,
  val highestWave: Long,
  val armoryCount: Int,
  val rigsCount: Int,
  val matchLogsCount: Int,
  val exportedAtMillis: Long,
  val bundle: SaveDataBundle,
)

object SaveDataSerializer {

  const val FORMAT_MARKER = "unknown_universes_save_v1"
  const val SCHEMA_VERSION = 1

  fun exportSaveJson(
    profile: OperatorProfile,
    armory: List<ArmoryItem>,
    rigs: List<RigAsset>,
    matchLogs: List<MatchRecord>,
    nowMillis: Long = System.currentTimeMillis(),
  ): String {
    val root = JSONObject()
    root.put("format", FORMAT_MARKER)
    root.put("schemaVersion", SCHEMA_VERSION)
    root.put("packageName", BuildConfig.APPLICATION_ID)
    root.put("sourceAppVersion", BuildConfig.VERSION_NAME)
    root.put("exportedAtMillis", nowMillis)

    // Privacy-safe OperatorProfile snapshot (omits userId and linked OAuth accounts)
    root.put(
      "profile",
      JSONObject().apply {
        put("callsign", profile.callsign)
        put("activeUniverseId", profile.activeUniverseId)
        put("activeMode", profile.activeMode)
        put("activeRigId", profile.activeRigId)
        put("consolePreset", profile.consolePreset)
        put("totalEssence", profile.totalEssence)
        put("highScore", profile.highScore)
        put("highestWave", profile.highestWave)
        put("osWallpaperIndex", profile.osWallpaperIndex)
      },
    )

    val armoryArr = JSONArray()
    armory.forEach { item ->
      armoryArr.put(
        JSONObject().apply {
          put("id", item.id)
          put("name", item.name)
          put("category", item.category)
          put("universeTag", item.universeTag)
          put("description", item.description)
          put("tier", item.tier)
          put("maxTier", item.maxTier)
          put("cooldownSec", item.cooldownSec)
          put("powerValue", item.powerValue)
          put("accentColorHex", item.accentColorHex)
          put("equipped", item.equipped)
          put("isCustom", item.isCustom)
          put("createdAtMillis", item.createdAtMillis)
          put("updatedAtMillis", item.updatedAtMillis)
        },
      )
    }
    root.put("armoryItems", armoryArr)

    val rigsArr = JSONArray()
    rigs.forEach { rig ->
      rigsArr.put(
        JSONObject().apply {
          put("id", rig.id)
          put("name", rig.name)
          put("archetype", rig.archetype)
          put("activeClip", rig.activeClip)
          put("ikEnabled", rig.ikEnabled)
          put("rigScale", rig.rigScale)
          put("playbackSpeed", rig.playbackSpeed)
          put("poseAngles", JSONObject(rig.poseAngles))
          put("keyframeFrames", JSONArray(rig.keyframeFrames))
          put("isCompleted", rig.isCompleted)
          put("createdAtMillis", rig.createdAtMillis)
          put("updatedAtMillis", rig.updatedAtMillis)
        },
      )
    }
    root.put("customRigs", rigsArr)

    val logsArr = JSONArray()
    matchLogs.take(50).forEach { log ->
      logsArr.put(
        JSONObject().apply {
          put("id", log.id)
          put("universeId", log.universeId)
          put("mode", log.mode)
          put("score", log.score)
          put("waveReached", log.waveReached)
          put("eliminations", log.eliminations)
          put("extracted", log.extracted)
          put("durationSec", log.durationSec)
          put("createdAtMillis", log.createdAtMillis)
        },
      )
    }
    root.put("matchLogs", logsArr)

    return root.toString(2)
  }

  /**
   * Parses either a full SaveData (.tuusave / unknown_universes_save_v1) OR a PortableBundle JSON
   * and re-targets all records cleanly to [targetUserId] within Firestore/Room safety bounds.
   */
  fun parseAndValidateSaveJson(
    rawJson: String,
    targetUserId: String,
  ): Result<SaveBundlePreview> {
    return try {
      val trimmed = rawJson.trim()
      require(trimmed.startsWith("{")) { "Selected file is not valid save data JSON." }
      val root = JSONObject(trimmed)
      val now = System.currentTimeMillis()
      val exportedAt = root.optLong("exportedAtMillis", now).coerceAtLeast(0L)
      val sourceVersion = root.optString("sourceAppVersion", BuildConfig.VERSION_NAME)
        .ifBlank { root.optString("osEnvironment", "Portable") }
        .take(32)

      val profileObj = root.optJSONObject("profile")
      val callsign = FirestoreSyncCodec.sanitizeCallsign(
        profileObj?.optString("callsign")
          ?: root.optString("callsign").takeIf { it.isNotBlank() }
      )
      val activeUniverseId = (
        profileObj?.optString("activeUniverseId")
          ?: root.optString("activeUniverse").takeIf { it.isNotBlank() }
          ?: "sector_zero"
        ).trim().take(64).ifEmpty { "sector_zero" }
      val activeMode = FirestoreSyncCodec.sanitizeMode(
        profileObj?.optString("activeMode")
          ?: root.optString("activeMode").takeIf { it.isNotBlank() }
          ?: "zombies"
      )
      val activeRigId = (
        profileObj?.optString("activeRigId")?.takeIf { it.isNotBlank() }
          ?: "rig_operator"
        ).trim().take(64)
      val consolePreset = FirestoreSyncCodec.sanitizePreset(
        profileObj?.optString("consolePreset")
          ?: root.optString("consolePreset").takeIf { it.isNotBlank() }
          ?: "universal_web"
      )
      val totalEssence = (
        profileObj?.optLong("totalEssence", -1L)?.takeIf { it >= 0L }
          ?: root.optLong("totalEssence", 2500L)
        ).coerceIn(0L, 999_999_999L)
      val highScore = (profileObj?.optLong("highScore", 0L) ?: 0L).coerceIn(0L, 999_999_999L)
      val highestWave = (profileObj?.optLong("highestWave", 0L) ?: 0L).coerceIn(0L, 9999L)
      val wallpaperIndex = (profileObj?.optLong("osWallpaperIndex", 0L) ?: 0L).coerceIn(0L, 10L)

      val profile = OperatorProfile(
        id = targetUserId,
        userId = targetUserId,
        callsign = callsign,
        activeUniverseId = activeUniverseId,
        activeMode = activeMode,
        activeRigId = activeRigId,
        consolePreset = consolePreset,
        totalEssence = totalEssence,
        highScore = highScore,
        highestWave = highestWave,
        osWallpaperIndex = wallpaperIndex,
        createdAtMillis = now,
        updatedAtMillis = now,
      )

      val armoryArr = root.optJSONArray("armoryItems") ?: root.optJSONArray("armory") ?: JSONArray()
      val armoryItems = buildList {
        for (i in 0 until armoryArr.length().coerceAtMost(80)) {
          val obj = armoryArr.optJSONObject(i) ?: continue
          val name = obj.optString("name").trim().take(50)
          if (name.isEmpty()) continue
          val rawId = obj.optString("id").ifBlank { "imp_arm_${i}_${name.hashCode().toUInt()}" }
          val maxTier = obj.optLong("maxTier", 5L).coerceIn(1L, 5L)
          val tier = obj.optLong("tier", 1L).coerceIn(1L, maxTier)
          add(
            ArmoryItem(
              id = FirestoreSyncCodec.sanitizeId(rawId, "arm_$i"),
              userId = targetUserId,
              name = name,
              category = FirestoreSyncCodec.sanitizeArmoryCategory(obj.optString("category", "perk")),
              universeTag = obj.optString("universeTag", "UnknownOS").trim().take(40).ifEmpty { "UnknownOS" },
              description = obj.optString("description", "").trim().take(240),
              tier = tier,
              maxTier = maxTier,
              cooldownSec = obj.optLong("cooldownSec", 0L).coerceIn(0L, 120L),
              powerValue = obj.optLong("powerValue", 120L).coerceIn(1L, 500L),
              accentColorHex = obj.optString("accentColorHex", "#00E5FF").trim().take(10).ifEmpty { "#00E5FF" },
              equipped = obj.optBoolean("equipped", false),
              isCustom = obj.optBoolean("isCustom", rawId.startsWith("custom_") || rawId.startsWith("imp_")),
              createdAtMillis = obj.optLong("createdAtMillis", now).coerceAtLeast(0L),
              updatedAtMillis = obj.optLong("updatedAtMillis", now).coerceAtLeast(0L),
            ),
          )
        }
      }

      val rigsArr = root.optJSONArray("customRigs") ?: root.optJSONArray("rigs") ?: JSONArray()
      val rigs = buildList {
        for (i in 0 until rigsArr.length().coerceAtMost(40)) {
          val obj = rigsArr.optJSONObject(i) ?: continue
          val name = obj.optString("name").trim().take(50)
          if (name.isEmpty()) continue
          val rawId = obj.optString("id").ifBlank { "imp_rig_${i}_${name.hashCode().toUInt()}" }
          val poseObj = obj.optJSONObject("poseAngles")
          val poseMap = if (poseObj != null) {
            buildMap {
              val keys = poseObj.keys()
              var count = 0
              while (keys.hasNext() && count < 20) {
                val k = keys.next()
                val v = poseObj.optDouble(k, Double.NaN)
                if (!v.isNaN()) {
                  put(k.take(32), v.coerceIn(-180.0, 180.0))
                  count++
                }
              }
            }.ifEmpty { UniverseCatalog.defaultPoseMap() }
          } else {
            UniverseCatalog.defaultPoseMap()
          }
          val framesArr = obj.optJSONArray("keyframeFrames")
          val frames = if (framesArr != null) {
            buildList {
              for (f in 0 until framesArr.length().coerceAtMost(20)) {
                add(framesArr.optLong(f, 0L).coerceIn(0L, 600L))
              }
            }
          } else {
            listOf(0L, 15L, 30L, 45L, 60L)
          }

          add(
            RigAsset(
              id = FirestoreSyncCodec.sanitizeId(rawId, "rig_$i"),
              userId = targetUserId,
              name = name,
              archetype = FirestoreSyncCodec.sanitizeArchetype(obj.optString("archetype", "operator")),
              activeClip = FirestoreSyncCodec.sanitizeClip(obj.optString("activeClip", "idle")),
              ikEnabled = obj.optBoolean("ikEnabled", true),
              rigScale = obj.optDouble("rigScale", 1.0).let { if (it.isNaN()) 1.0 else it.coerceIn(0.5, 2.0) },
              playbackSpeed = obj.optDouble("playbackSpeed", 1.0).let { if (it.isNaN()) 1.0 else it.coerceIn(0.25, 3.0) },
              poseAngles = poseMap,
              keyframeFrames = frames,
              isCompleted = obj.optBoolean("isCompleted", true),
              createdAtMillis = obj.optLong("createdAtMillis", now).coerceAtLeast(0L),
              updatedAtMillis = obj.optLong("updatedAtMillis", now).coerceAtLeast(0L),
            ),
          )
        }
      }

      val logsArr = root.optJSONArray("matchLogs") ?: JSONArray()
      val matchLogs = buildList {
        for (i in 0 until logsArr.length().coerceAtMost(50)) {
          val obj = logsArr.optJSONObject(i) ?: continue
          val rawId = obj.optString("id").ifBlank { "imp_match_${i}_$now" }
          add(
            MatchRecord(
              id = FirestoreSyncCodec.sanitizeId(rawId, "match_$i"),
              userId = targetUserId,
              universeId = obj.optString("universeId", "sector_zero").trim().take(64).ifEmpty { "sector_zero" },
              mode = FirestoreSyncCodec.sanitizeMode(obj.optString("mode", "zombies")),
              score = obj.optLong("score", 0L).coerceIn(0L, 999_999_999L),
              waveReached = obj.optLong("waveReached", 0L).coerceIn(0L, 9999L),
              eliminations = obj.optLong("eliminations", 0L).coerceIn(0L, 99999L),
              extracted = obj.optBoolean("extracted", false),
              durationSec = obj.optLong("durationSec", 0L).coerceIn(0L, 86400L),
              createdAtMillis = obj.optLong("createdAtMillis", now).coerceAtLeast(0L),
            ),
          )
        }
      }

      val hasRecognizedStructure = profileObj != null || root.has("format") || armoryItems.isNotEmpty() || rigs.isNotEmpty()
      require(hasRecognizedStructure) { "JSON does not contain Unknown Universes save fields." }

      val bundle = SaveDataBundle(
        schemaVersion = root.optInt("schemaVersion", SCHEMA_VERSION),
        exportedAtMillis = exportedAt,
        sourceAppVersion = sourceVersion,
        profile = profile,
        armoryItems = armoryItems,
        customRigs = rigs,
        matchLogs = matchLogs,
      )
      Result.success(
        SaveBundlePreview(
          callsign = callsign,
          sourceAppVersion = sourceVersion,
          totalEssence = totalEssence,
          highScore = highScore,
          highestWave = highestWave,
          armoryCount = armoryItems.size,
          rigsCount = rigs.size,
          matchLogsCount = matchLogs.size,
          exportedAtMillis = exportedAt,
          bundle = bundle,
        ),
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}

package com.example.data.remote

import com.example.data.model.ArmoryItem
import com.example.data.model.LinkedAccount
import com.example.data.model.MatchRecord
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

object FirestoreSyncCodec {

  private val validDocIdRegex = Regex("^[a-zA-Z0-9_\\-]+$")
  private val allowedModes = setOf("zombies", "dmz", "br", "multiplayer", "sandbox")
  private val allowedPresets = setOf(
    "universal_web",
    "ps5_dualsense",
    "xbox_series",
    "switch_handheld",
    "ios_safari",
    "pc_desktop",
  )
  private val allowedArmoryCategories = setOf("perk", "power", "upgrade", "consumable")
  private val allowedArchetypes = setOf("operator", "biomorph", "infernal", "chrono", "starblade")
  private val allowedClips = setOf("idle", "sprint", "lunge", "slam", "vault", "inspect")
  private val allowedProviders = setOf(
    "epic", "steam", "gog", "ea_origin", "amazon",
    "galaxy_store", "psn", "nintendo", "ubisoft", "xbox", "apple",
  )

  fun sanitizeId(raw: String, fallbackPrefix: String): String {
    val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
    return if (cleaned.isNotEmpty() && validDocIdRegex.matches(cleaned)) cleaned else "${fallbackPrefix}_item"
  }

  fun sanitizeCallsign(raw: String?): String {
    val trimmed = raw?.trim().orEmpty().take(30)
    return if (trimmed.length >= 2) trimmed else "VanguardPrime"
  }

  fun sanitizeMode(raw: String): String = if (raw in allowedModes) raw else "zombies"

  fun sanitizePreset(raw: String): String = if (raw in allowedPresets) raw else "universal_web"

  fun sanitizeArmoryCategory(raw: String): String =
    if (raw in allowedArmoryCategories) raw else "perk"

  fun sanitizeArchetype(raw: String): String =
    if (raw in allowedArchetypes) raw else "operator"

  fun sanitizeClip(raw: String): String =
    if (raw in allowedClips) raw else "idle"

  fun isSupportedProviderId(raw: String): Boolean = raw in allowedProviders

  private fun DocumentSnapshot.readTimestampMillis(field: String, fallbackMillis: Long): Long {
    val ts: Timestamp? = getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    return ts?.toDate()?.time ?: fallbackMillis
  }

  // --- OperatorProfile (/users/{userId}) ---

  fun OperatorProfile.toFirestoreCreateMap(uid: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "callsign" to sanitizeCallsign(callsign),
    "activeUniverseId" to activeUniverseId.trim().take(64).ifEmpty { "sector_zero" },
    "activeMode" to sanitizeMode(activeMode),
    "activeRigId" to activeRigId.trim().take(64).ifEmpty { "rig_operator" },
    "consolePreset" to sanitizePreset(consolePreset),
    "totalEssence" to totalEssence.coerceIn(0L, 999_999_999L),
    "highScore" to highScore.coerceIn(0L, 999_999_999L),
    "highestWave" to highestWave.coerceIn(0L, 9999L),
    "osWallpaperIndex" to osWallpaperIndex.coerceIn(0L, 10L),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
  )

  fun OperatorProfile.toFirestoreUpdateMap(): Map<String, Any> = mapOf(
    "callsign" to sanitizeCallsign(callsign),
    "activeUniverseId" to activeUniverseId.trim().take(64).ifEmpty { "sector_zero" },
    "activeMode" to sanitizeMode(activeMode),
    "activeRigId" to activeRigId.trim().take(64).ifEmpty { "rig_operator" },
    "consolePreset" to sanitizePreset(consolePreset),
    "totalEssence" to totalEssence.coerceIn(0L, 999_999_999L),
    "highScore" to highScore.coerceIn(0L, 999_999_999L),
    "highestWave" to highestWave.coerceIn(0L, 9999L),
    "osWallpaperIndex" to osWallpaperIndex.coerceIn(0L, 10L),
    "updatedAt" to FieldValue.serverTimestamp(),
  )

  fun DocumentSnapshot.toOperatorProfileOrNull(): OperatorProfile? {
    val data = data ?: return null
    val uid = (data["userId"] as? String)?.takeIf { it.isNotBlank() } ?: id
    val now = System.currentTimeMillis()
    return OperatorProfile(
      id = uid,
      userId = uid,
      callsign = sanitizeCallsign(data["callsign"] as? String),
      activeUniverseId = (data["activeUniverseId"] as? String)?.takeIf { it.isNotBlank() } ?: "sector_zero",
      activeMode = sanitizeMode((data["activeMode"] as? String).orEmpty()),
      activeRigId = (data["activeRigId"] as? String)?.takeIf { it.isNotBlank() } ?: "rig_operator",
      consolePreset = sanitizePreset((data["consolePreset"] as? String).orEmpty()),
      totalEssence = ((data["totalEssence"] as? Number)?.toLong() ?: 2500L).coerceIn(0L, 999_999_999L),
      highScore = ((data["highScore"] as? Number)?.toLong() ?: 0L).coerceIn(0L, 999_999_999L),
      highestWave = ((data["highestWave"] as? Number)?.toLong() ?: 0L).coerceIn(0L, 9999L),
      osWallpaperIndex = ((data["osWallpaperIndex"] as? Number)?.toLong() ?: 0L).coerceIn(0L, 10L),
      createdAtMillis = readTimestampMillis("createdAt", now),
      updatedAtMillis = readTimestampMillis("updatedAt", now),
    )
  }

  // --- ArmoryItem (/users/{userId}/armoryItems/{itemId}) ---

  fun ArmoryItem.toFirestoreCreateMap(uid: String): Map<String, Any> {
    val safeMaxTier = maxTier.coerceIn(1L, 5L)
    val safeTier = tier.coerceIn(1L, safeMaxTier)
    return mapOf(
      "userId" to uid,
      "name" to name.trim().take(50).ifEmpty { "Module" },
      "category" to sanitizeArmoryCategory(category),
      "universeTag" to universeTag.trim().take(40).ifEmpty { "UnknownOS" },
      "description" to description.trim().take(240),
      "tier" to safeTier,
      "maxTier" to safeMaxTier,
      "cooldownSec" to cooldownSec.coerceIn(0L, 120L),
      "powerValue" to powerValue.coerceIn(1L, 500L),
      "accentColorHex" to accentColorHex.trim().take(10).ifEmpty { "#00E5FF" },
      "equipped" to equipped,
      "isCustom" to isCustom,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
  }

  fun ArmoryItem.toFirestoreUpdateMap(): Map<String, Any> {
    val safeMaxTier = maxTier.coerceIn(1L, 5L)
    val safeTier = tier.coerceIn(1L, safeMaxTier)
    return mapOf(
      "name" to name.trim().take(50).ifEmpty { "Module" },
      "category" to sanitizeArmoryCategory(category),
      "universeTag" to universeTag.trim().take(40).ifEmpty { "UnknownOS" },
      "description" to description.trim().take(240),
      "tier" to safeTier,
      "maxTier" to safeMaxTier,
      "cooldownSec" to cooldownSec.coerceIn(0L, 120L),
      "powerValue" to powerValue.coerceIn(1L, 500L),
      "accentColorHex" to accentColorHex.trim().take(10).ifEmpty { "#00E5FF" },
      "equipped" to equipped,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
  }

  fun DocumentSnapshot.toArmoryItemOrNull(fallbackUserId: String): ArmoryItem? {
    val data = data ?: return null
    val now = System.currentTimeMillis()
    val maxTier = ((data["maxTier"] as? Number)?.toLong() ?: 5L).coerceIn(1L, 5L)
    val tier = ((data["tier"] as? Number)?.toLong() ?: 1L).coerceIn(1L, maxTier)
    return ArmoryItem(
      id = id,
      userId = (data["userId"] as? String)?.takeIf { it.isNotBlank() } ?: fallbackUserId,
      name = (data["name"] as? String)?.take(50).orEmpty().ifBlank { "Module" },
      category = sanitizeArmoryCategory((data["category"] as? String).orEmpty()),
      universeTag = (data["universeTag"] as? String)?.take(40).orEmpty().ifBlank { "UnknownOS" },
      description = (data["description"] as? String)?.take(240).orEmpty(),
      tier = tier,
      maxTier = maxTier,
      cooldownSec = ((data["cooldownSec"] as? Number)?.toLong() ?: 0L).coerceIn(0L, 120L),
      powerValue = ((data["powerValue"] as? Number)?.toLong() ?: 100L).coerceIn(1L, 500L),
      accentColorHex = (data["accentColorHex"] as? String)?.take(10).orEmpty().ifBlank { "#00E5FF" },
      equipped = (data["equipped"] as? Boolean) ?: false,
      isCustom = (data["isCustom"] as? Boolean) ?: false,
      createdAtMillis = readTimestampMillis("createdAt", now),
      updatedAtMillis = readTimestampMillis("updatedAt", now),
    )
  }

  // --- RigAsset (/users/{userId}/customRigs/{rigId}) ---

  fun RigAsset.toFirestoreCreateMap(uid: String): Map<String, Any> {
    val safePose = poseAngles.entries.take(20).associate { (k, v) -> k.take(32) to v }
    val safeFrames = keyframeFrames.take(20).map { it.coerceIn(0L, 600L) }
    return mapOf(
      "userId" to uid,
      "name" to name.trim().take(50).ifEmpty { "Custom Rig" },
      "archetype" to sanitizeArchetype(archetype),
      "activeClip" to sanitizeClip(activeClip),
      "ikEnabled" to ikEnabled,
      "rigScale" to rigScale.coerceIn(0.5, 2.0),
      "playbackSpeed" to playbackSpeed.coerceIn(0.25, 3.0),
      "poseAngles" to safePose,
      "keyframeFrames" to safeFrames,
      "isCompleted" to isCompleted,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
  }

  fun RigAsset.toFirestoreUpdateMap(): Map<String, Any> {
    val safePose = poseAngles.entries.take(20).associate { (k, v) -> k.take(32) to v }
    val safeFrames = keyframeFrames.take(20).map { it.coerceIn(0L, 600L) }
    return mapOf(
      "name" to name.trim().take(50).ifEmpty { "Custom Rig" },
      "archetype" to sanitizeArchetype(archetype),
      "activeClip" to sanitizeClip(activeClip),
      "ikEnabled" to ikEnabled,
      "rigScale" to rigScale.coerceIn(0.5, 2.0),
      "playbackSpeed" to playbackSpeed.coerceIn(0.25, 3.0),
      "poseAngles" to safePose,
      "keyframeFrames" to safeFrames,
      "isCompleted" to isCompleted,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
  }

  fun DocumentSnapshot.toRigAssetOrNull(fallbackUserId: String): RigAsset? {
    val data = data ?: return null
    val now = System.currentTimeMillis()
    val rawPose = data["poseAngles"] as? Map<*, *>
    val poseMap = rawPose?.entries
      ?.mapNotNull { (k, v) ->
        val key = k as? String ?: return@mapNotNull null
        val num = (v as? Number)?.toDouble() ?: return@mapNotNull null
        key to num
      }
      ?.toMap()
      ?: UniverseCatalog.defaultPoseMap()
    val rawFrames = (data["keyframeFrames"] as? List<*>)
      ?.mapNotNull { (it as? Number)?.toLong() }
      ?.take(20)
      ?: listOf(0L, 15L, 30L, 45L, 60L)
    return RigAsset(
      id = id,
      userId = (data["userId"] as? String)?.takeIf { it.isNotBlank() } ?: fallbackUserId,
      name = (data["name"] as? String)?.take(50).orEmpty().ifBlank { "Custom Rig" },
      archetype = sanitizeArchetype((data["archetype"] as? String).orEmpty()),
      activeClip = sanitizeClip((data["activeClip"] as? String).orEmpty()),
      ikEnabled = (data["ikEnabled"] as? Boolean) ?: true,
      rigScale = ((data["rigScale"] as? Number)?.toDouble() ?: 1.0).coerceIn(0.5, 2.0),
      playbackSpeed = ((data["playbackSpeed"] as? Number)?.toDouble() ?: 1.0).coerceIn(0.25, 3.0),
      poseAngles = poseMap,
      keyframeFrames = rawFrames,
      isCompleted = (data["isCompleted"] as? Boolean) ?: true,
      createdAtMillis = readTimestampMillis("createdAt", now),
      updatedAtMillis = readTimestampMillis("updatedAt", now),
    )
  }

  // --- MatchRecord (/users/{userId}/matchLogs/{matchId}) ---

  fun MatchRecord.toFirestoreCreateMap(uid: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "universeId" to universeId.trim().take(64).ifEmpty { "sector_zero" },
    "mode" to sanitizeMode(mode),
    "score" to score.coerceIn(0L, 999_999_999L),
    "waveReached" to waveReached.coerceIn(0L, 9999L),
    "eliminations" to eliminations.coerceIn(0L, 99999L),
    "extracted" to extracted,
    "durationSec" to durationSec.coerceIn(0L, 86400L),
    "createdAt" to FieldValue.serverTimestamp(),
  )

  fun DocumentSnapshot.toMatchRecordOrNull(fallbackUserId: String): MatchRecord? {
    val data = data ?: return null
    val now = System.currentTimeMillis()
    return MatchRecord(
      id = id,
      userId = (data["userId"] as? String)?.takeIf { it.isNotBlank() } ?: fallbackUserId,
      universeId = (data["universeId"] as? String)?.take(64).orEmpty().ifBlank { "sector_zero" },
      mode = sanitizeMode((data["mode"] as? String).orEmpty()),
      score = ((data["score"] as? Number)?.toLong() ?: 0L).coerceIn(0L, 999_999_999L),
      waveReached = ((data["waveReached"] as? Number)?.toLong() ?: 0L).coerceIn(0L, 9999L),
      eliminations = ((data["eliminations"] as? Number)?.toLong() ?: 0L).coerceIn(0L, 99999L),
      extracted = (data["extracted"] as? Boolean) ?: false,
      durationSec = ((data["durationSec"] as? Number)?.toLong() ?: 0L).coerceIn(0L, 86400L),
      createdAtMillis = readTimestampMillis("createdAt", now),
    )
  }

  // --- LinkedAccount (/users/{userId}/linkedAccounts/{providerId}) ---

  fun LinkedAccount.toFirestoreMap(uid: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "providerId" to providerId,
    "displayName" to displayName.trim().take(64).ifEmpty { providerId },
    "externalSubject" to externalSubject.trim().take(128).ifEmpty { providerId },
    "linkedAt" to FieldValue.serverTimestamp(),
  )

  fun DocumentSnapshot.toLinkedAccountOrNull(fallbackUserId: String): LinkedAccount? {
    val data = data ?: return null
    val providerId = (data["providerId"] as? String)?.takeIf { isSupportedProviderId(it) } ?: return null
    val uid = (data["userId"] as? String)?.takeIf { it.isNotBlank() } ?: fallbackUserId
    val now = System.currentTimeMillis()
    return LinkedAccount(
      id = "${uid}_$providerId",
      userId = uid,
      providerId = providerId,
      displayName = (data["displayName"] as? String)?.take(64).orEmpty().ifBlank { providerId },
      externalSubject = (data["externalSubject"] as? String)?.take(128).orEmpty().ifBlank { providerId },
      linkedAtMillis = readTimestampMillis("linkedAt", now),
    )
  }
}

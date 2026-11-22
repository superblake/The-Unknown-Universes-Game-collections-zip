package com.example.data.local

import android.content.Context
import com.example.R
import com.example.data.model.ArmoryItem
import com.example.data.model.LinkedAccount
import com.example.data.model.MatchRecord
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.data.model.UpgradeCosts
import com.example.data.remote.FirestoreSyncCodec
import com.example.data.remote.UniverseCloudSync
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import org.json.JSONObject
import java.util.UUID

sealed interface CloudSyncState {
  data object LocalOnly : CloudSyncState
  data object Syncing : CloudSyncState
  data class Synced(val lastSyncedMillis: Long) : CloudSyncState
  data class Error(val message: String) : CloudSyncState
}

class UniverseRepository(
  private val dao: UniverseDao,
  firestore: FirebaseFirestore? = null,
  private val authProvider: () -> FirebaseAuth? = { null },
) {

  constructor(context: Context) : this(
    dao = UniverseDatabase.getInstance(context).universeDao(),
    firestore = resolveFirestore(context),
    authProvider = createAuthProvider(context.applicationContext),
  )

  private val cloudSync: UniverseCloudSync? = firestore?.let { db ->
    UniverseCloudSync(db = db, dao = dao, authProvider = authProvider)
  }

  private val _cloudSyncState = MutableStateFlow<CloudSyncState>(CloudSyncState.LocalOnly)
  val cloudSyncState: StateFlow<CloudSyncState> = _cloudSyncState.asStateFlow()

  fun isCloudSyncAvailableFor(userId: String): Boolean =
    cloudSync?.isAuthenticatedFor(userId) == true

  fun observeProfile(userId: String): Flow<OperatorProfile?> = dao.observeProfile(userId)

  fun observeArmoryItems(userId: String): Flow<List<ArmoryItem>> = dao.observeArmoryItems(userId)

  fun observeCustomRigs(userId: String): Flow<List<RigAsset>> = dao.observeCustomRigs(userId)

  fun observeMatchLogs(userId: String): Flow<List<MatchRecord>> = dao.observeMatchLogs(userId)

  fun observeLinkedAccounts(userId: String): Flow<List<LinkedAccount>> = dao.observeLinkedAccounts(userId)

  fun observeCloudChanges(userId: String): Flow<Long> {
    val sync = cloudSync ?: return emptyFlow()
    if (!sync.isAuthenticatedFor(userId)) {
      _cloudSyncState.value = CloudSyncState.LocalOnly
      return emptyFlow()
    }
    return sync.observeAndMirrorCloud(userId).onEach { timestamp ->
      _cloudSyncState.value = CloudSyncState.Synced(timestamp)
    }
  }

  suspend fun syncWithCloud(userId: String, displayName: String?): Result<String> {
    val sync = cloudSync
    if (sync == null || !sync.isAuthenticatedFor(userId)) {
      _cloudSyncState.value = CloudSyncState.LocalOnly
      return Result.failure(IllegalStateException("Sign in with Google to enable Cloud Sync."))
    }
    _cloudSyncState.value = CloudSyncState.Syncing
    val res = sync.pullMergeAndPush(
      userId = userId,
      displayName = displayName,
      starterArmoryProvider = ::starterArmoryItems,
      starterRigsProvider = ::starterRigs,
    )
    return if (res.isSuccess) {
      val summary = res.getOrThrow()
      _cloudSyncState.value = CloudSyncState.Synced(summary.syncedAtMillis)
      Result.success("Cloud synced (${summary.downloadedCount} pulled, ${summary.uploadedCount} pushed)")
    } else {
      val msg = res.exceptionOrNull()?.localizedMessage ?: "Cloud sync failed"
      _cloudSyncState.value = CloudSyncState.Error(msg)
      Result.failure(res.exceptionOrNull() ?: IllegalStateException(msg))
    }
  }

  suspend fun ensureStarterData(userId: String, displayName: String?): Result<Unit> {
    return try {
      val existing = dao.getProfileOnce(userId)
      if (existing == null) {
        val safeCallsign = FirestoreSyncCodec.sanitizeCallsign(displayName)
        val now = System.currentTimeMillis()
        dao.upsertProfile(
          OperatorProfile(
            id = userId,
            userId = userId,
            callsign = safeCallsign,
            activeUniverseId = "sector_zero",
            activeMode = "zombies",
            activeRigId = "rig_operator",
            consolePreset = "universal_web",
            totalEssence = 2500L,
            highScore = 0L,
            highestWave = 0L,
            osWallpaperIndex = 0L,
            createdAtMillis = now,
            updatedAtMillis = now,
          )
        )
        dao.upsertArmoryItems(starterArmoryItems(userId, now))
        dao.upsertRigs(starterRigs(userId, now))
      } else {
        if (!displayName.isNullOrBlank() && displayName.trim().length >= 2 && existing.callsign != displayName.trim().take(30)) {
          val updated = existing.copy(
            callsign = displayName.trim().take(30),
            updatedAtMillis = System.currentTimeMillis(),
          )
          dao.upsertProfile(updated)
        }
        val existingArmory = dao.getArmoryItemsOnce(userId)
        val outdated = existingArmory.filter { it.maxTier < 25L }
        if (outdated.isNotEmpty()) {
          dao.upsertArmoryItems(outdated.map { it.copy(maxTier = 25L) })
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateProfileSelection(
    userId: String,
    activeUniverseId: String? = null,
    activeMode: String? = null,
    activeRigId: String? = null,
    consolePreset: String? = null,
    callsign: String? = null,
    osWallpaperIndex: Long? = null,
  ): Result<Unit> {
    return try {
      val existing = dao.getProfileOnce(userId)
        ?: return Result.failure(IllegalStateException("Operator profile not found"))
      val updated = existing.copy(
        activeUniverseId = activeUniverseId?.trim()?.take(64)?.ifEmpty { existing.activeUniverseId } ?: existing.activeUniverseId,
        activeMode = activeMode?.let { FirestoreSyncCodec.sanitizeMode(it) } ?: existing.activeMode,
        activeRigId = activeRigId?.trim()?.take(64)?.ifEmpty { existing.activeRigId } ?: existing.activeRigId,
        consolePreset = consolePreset?.let { FirestoreSyncCodec.sanitizePreset(it) } ?: existing.consolePreset,
        callsign = callsign?.let { FirestoreSyncCodec.sanitizeCallsign(it) } ?: existing.callsign,
        osWallpaperIndex = osWallpaperIndex?.coerceIn(0L, 10L) ?: existing.osWallpaperIndex,
        updatedAtMillis = System.currentTimeMillis(),
      )
      dao.upsertProfile(updated)
      mirrorCloud(userId) { it.pushProfile(updated) }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun addArmoryItem(
    userId: String,
    name: String,
    category: String,
    universeTag: String,
    description: String,
    cooldownSec: Long,
    powerValue: Long,
    accentColorHex: String,
    equipped: Boolean = true,
    isCustom: Boolean = true,
  ): Result<String> {
    return try {
      val itemId = "custom_${UUID.randomUUID().toString().take(8)}"
      val now = System.currentTimeMillis()
      val item = ArmoryItem(
        id = itemId,
        userId = userId,
        name = name.trim().take(50).ifEmpty { "Custom Module" },
        category = FirestoreSyncCodec.sanitizeArmoryCategory(category),
        universeTag = universeTag.trim().take(40).ifEmpty { "UnknownOS" },
        description = description.trim().take(240),
        tier = 1L,
        maxTier = 25L,
        cooldownSec = cooldownSec.coerceIn(0L, 120L),
        powerValue = powerValue.coerceIn(1L, 1500L),
        accentColorHex = accentColorHex.trim().take(10).ifEmpty { "#00E5FF" },
        equipped = equipped,
        isCustom = isCustom,
        createdAtMillis = now,
        updatedAtMillis = now,
      )
      dao.upsertArmoryItem(item)
      mirrorCloud(userId) { it.pushArmoryItem(item) }
      Result.success(itemId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun toggleArmoryEquipped(userId: String, item: ArmoryItem): Result<Unit> {
    require(item.id.isNotBlank()) { "ArmoryItem id required" }
    return try {
      val updated = item.copy(
        userId = userId,
        equipped = !item.equipped,
        updatedAtMillis = System.currentTimeMillis(),
      )
      dao.upsertArmoryItem(updated)
      mirrorCloud(userId) { it.pushArmoryItem(updated) }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getArmoryItem(userId: String, itemId: String): ArmoryItem? =
    dao.getArmoryItemsOnce(userId).firstOrNull { it.id == itemId }

  suspend fun upgradeArmoryItem(
    userId: String,
    item: ArmoryItem,
    upgradeCost: Long,
    currentEssence: Long,
  ): Result<Unit> {
    require(item.id.isNotBlank()) { "ArmoryItem id required" }
    return try {
      val now = System.currentTimeMillis()
      val liveItem = dao.getArmoryItemsOnce(userId).firstOrNull { it.id == item.id } ?: item
      val effectiveMaxTier = maxOf(liveItem.maxTier, 25L)
      if (liveItem.tier >= effectiveMaxTier) {
        return Result.failure(IllegalStateException("Already at max Tier $effectiveMaxTier"))
      }
      val cost = if (liveItem.tier == item.tier && upgradeCost >= 0L) upgradeCost else UpgradeCosts.armoryModTier(liveItem.tier.toInt())
      val profile = dao.getProfileOnce(userId)
        ?: return Result.failure(IllegalStateException("Operator profile not found"))
      val availableEssence = if (currentEssence > 0L) minOf(profile.totalEssence, currentEssence) else profile.totalEssence
      if (availableEssence < cost) {
        return Result.failure(IllegalStateException("Need $cost Blue Essence to upgrade"))
      }
      val nextTier = liveItem.tier + 1L
      val updatedItem = liveItem.copy(
        userId = userId,
        tier = nextTier,
        maxTier = effectiveMaxTier,
        powerValue = (liveItem.powerValue + 75L).coerceAtMost(1500L),
        updatedAtMillis = now,
      )
      val updatedProfile = profile.copy(
        totalEssence = (profile.totalEssence - cost).coerceAtLeast(0L),
        updatedAtMillis = now,
      )
      dao.upsertArmoryItem(updatedItem)
      dao.upsertProfile(updatedProfile)
      mirrorCloud(userId) {
        it.pushArmoryItem(updatedItem)
        it.pushProfile(updatedProfile)
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun boostAllArmoryTiers(
    userId: String,
    delta: Int = 1,
    targetTier: Int? = null,
  ): Result<Int> {
    return try {
      val now = System.currentTimeMillis()
      val items = dao.getArmoryItemsOnce(userId)
      if (items.isEmpty()) return Result.success(0)
      val updatedList = items.map { item ->
        val effectiveMax = maxOf(item.maxTier, 25L)
        val newTier = (targetTier?.toLong() ?: (item.tier + delta)).coerceIn(1L, effectiveMax)
        item.copy(
          tier = newTier,
          maxTier = effectiveMax,
          powerValue = (100L + newTier * 50L).coerceAtMost(1500L),
          updatedAtMillis = now,
        )
      }
      dao.upsertArmoryItems(updatedList)
      mirrorCloud(userId) { sync ->
        updatedList.forEach { sync.pushArmoryItem(it) }
      }
      Result.success(updatedList.size)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun adjustOperatorEssence(userId: String, delta: Long): Result<Long> {
    return try {
      val now = System.currentTimeMillis()
      val profile = dao.getProfileOnce(userId)
        ?: return Result.failure(IllegalStateException("Operator profile not found"))
      val nextEssence = (profile.totalEssence + delta).coerceIn(0L, 999_999_999L)
      val updatedProfile = profile.copy(
        totalEssence = nextEssence,
        updatedAtMillis = now,
      )
      dao.upsertProfile(updatedProfile)
      mirrorCloud(userId) { it.pushProfile(updatedProfile) }
      Result.success(nextEssence)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteArmoryItem(userId: String, itemId: String): Result<Unit> {
    return try {
      dao.deleteArmoryItem(userId, itemId)
      mirrorCloud(userId) { it.deleteArmoryItem(userId, itemId) }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun saveRigAsset(
    userId: String,
    rigId: String?,
    name: String,
    archetype: String,
    activeClip: String,
    ikEnabled: Boolean,
    rigScale: Double,
    playbackSpeed: Double,
    poseAngles: Map<String, Double>,
    keyframeFrames: List<Long>,
    isCompleted: Boolean,
  ): Result<String> {
    val safeName = name.trim().take(50).ifEmpty { "Custom Rig" }
    val safeFrames = keyframeFrames.take(20)
    val now = System.currentTimeMillis()
    return try {
      val resolvedId = if (rigId.isNullOrBlank()) "rig_${UUID.randomUUID().toString().take(8)}" else rigId
      val existing = if (rigId.isNullOrBlank()) null else dao.getRigOnce(userId, rigId)
      val rig = RigAsset(
        id = resolvedId,
        userId = userId,
        name = safeName,
        archetype = FirestoreSyncCodec.sanitizeArchetype(archetype),
        activeClip = FirestoreSyncCodec.sanitizeClip(activeClip),
        ikEnabled = ikEnabled,
        rigScale = rigScale.coerceIn(0.5, 2.0),
        playbackSpeed = playbackSpeed.coerceIn(0.25, 3.0),
        poseAngles = poseAngles,
        keyframeFrames = safeFrames,
        isCompleted = isCompleted,
        createdAtMillis = existing?.createdAtMillis ?: now,
        updatedAtMillis = now,
      )
      dao.upsertRig(rig)
      mirrorCloud(userId) { it.pushRigAsset(rig) }
      Result.success(resolvedId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun recordMatchResult(
    userId: String,
    profile: OperatorProfile,
    universeId: String,
    mode: String,
    score: Long,
    waveReached: Long,
    eliminations: Long,
    extracted: Boolean,
    durationSec: Long,
    essenceEarned: Long,
  ): Result<String> {
    return try {
      val matchId = "match_${UUID.randomUUID().toString().take(8)}"
      val now = System.currentTimeMillis()
      val record = MatchRecord(
        id = matchId,
        userId = userId,
        universeId = universeId.trim().take(64).ifEmpty { "sector_zero" },
        mode = FirestoreSyncCodec.sanitizeMode(mode),
        score = score.coerceIn(0L, 999_999_999L),
        waveReached = waveReached.coerceIn(0L, 9999L),
        eliminations = eliminations.coerceIn(0L, 99999L),
        extracted = extracted,
        durationSec = durationSec.coerceIn(0L, 86400L),
        createdAtMillis = now,
      )
      dao.insertMatchLog(record)
      val latestProfile = dao.getProfileOnce(userId) ?: profile
      val updatedProfile = latestProfile.copy(
        totalEssence = (latestProfile.totalEssence + essenceEarned).coerceAtMost(999_999_999L),
        highScore = maxOf(latestProfile.highScore, score),
        highestWave = maxOf(latestProfile.highestWave, waveReached),
        updatedAtMillis = now,
      )
      dao.upsertProfile(updatedProfile)
      mirrorCloud(userId) {
        it.pushMatchRecord(record)
        it.pushProfile(updatedProfile)
      }
      Result.success(matchId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun saveLinkedAccount(
    userId: String,
    providerId: String,
    displayName: String,
    externalSubject: String,
  ): Result<Unit> {
    return try {
      val account = LinkedAccount(
        id = "${userId}_$providerId",
        userId = userId,
        providerId = providerId,
        displayName = displayName.trim().take(64).ifEmpty { providerId },
        externalSubject = externalSubject.trim().take(128).ifEmpty { providerId },
        linkedAtMillis = System.currentTimeMillis(),
      )
      dao.deleteLinkedAccount(userId, providerId)
      dao.upsertLinkedAccount(account)
      mirrorCloud(userId) { it.pushLinkedAccount(account) }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun unlinkAccount(userId: String, providerId: String): Result<Unit> {
    return try {
      dao.deleteLinkedAccount(userId, providerId)
      mirrorCloud(userId) { it.deleteLinkedAccount(userId, providerId) }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun exportSaveDataJson(
    userId: String,
    progressionJson: JSONObject? = null,
  ): Result<String> {
    return try {
      val profile = dao.getProfileOnce(userId)
        ?: return Result.failure(IllegalStateException("No save profile loaded yet"))
      val armory = dao.getArmoryItemsOnce(userId)
      val rigs = dao.getRigsOnce(userId)
      val logs = dao.getMatchLogsOnce(userId)
      Result.success(SaveDataSerializer.exportSaveJson(profile, armory, rigs, logs, progressionJson = progressionJson))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun importSaveBundle(
    userId: String,
    bundle: SaveDataBundle,
    replaceExisting: Boolean,
  ): Result<String> {
    return try {
      val now = System.currentTimeMillis()
      val existingProfile = dao.getProfileOnce(userId)
      val importedProfile = bundle.profile.copy(
        id = userId,
        userId = userId,
        createdAtMillis = existingProfile?.createdAtMillis ?: now,
        updatedAtMillis = now,
      )

      val targetProfile = if (replaceExisting || existingProfile == null) {
        importedProfile
      } else {
        existingProfile.copy(
          totalEssence = maxOf(existingProfile.totalEssence, importedProfile.totalEssence),
          highScore = maxOf(existingProfile.highScore, importedProfile.highScore),
          highestWave = maxOf(existingProfile.highestWave, importedProfile.highestWave),
          activeUniverseId = importedProfile.activeUniverseId,
          activeMode = importedProfile.activeMode,
          activeRigId = importedProfile.activeRigId,
          updatedAtMillis = now,
        )
      }

      val resolvedArmory = bundle.armoryItems.map {
        it.copy(userId = userId, maxTier = maxOf(it.maxTier, 25L), updatedAtMillis = now)
      }.ifEmpty { starterArmoryItems(userId, now) }
      val resolvedRigs = bundle.customRigs.map { it.copy(userId = userId, updatedAtMillis = now) }
        .ifEmpty { starterRigs(userId, now) }
      val resolvedLogs = bundle.matchLogs.map { it.copy(userId = userId) }

      if (replaceExisting) {
        dao.deleteAllArmoryItemsForUser(userId)
        dao.deleteAllRigsForUser(userId)
        dao.deleteAllMatchLogsForUser(userId)
      }

      dao.upsertProfile(targetProfile)
      dao.upsertArmoryItems(resolvedArmory)
      dao.upsertRigs(resolvedRigs)
      if (resolvedLogs.isNotEmpty()) {
        dao.insertMatchLogs(resolvedLogs)
      }

      mirrorCloud(userId) { sync ->
        sync.pushProfile(targetProfile)
        resolvedArmory.forEach { sync.pushArmoryItem(it) }
        resolvedRigs.forEach { sync.pushRigAsset(it) }
        resolvedLogs.forEach { sync.pushMatchRecord(it) }
      }

      val modeLabel = if (replaceExisting) "Loaded" else "Merged"
      val cloudNote = if (isCloudSyncAvailableFor(userId)) " & synced to Cloud" else ""
      Result.success(
        "$modeLabel save (${targetProfile.callsign}: ${resolvedArmory.size} modules, ${resolvedRigs.size} rigs)$cloudNote",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun importSteamWorkshopPreview(
    userId: String,
    preview: com.example.export.SteamWorkshopImportPreview,
    importModulePreset: Boolean = true,
    importRigPreset: Boolean = true,
  ): Result<String> {
    return try {
      val now = System.currentTimeMillis()
      val itemsToSave = buildList {
        addAll(preview.armoryItems.map { it.copy(userId = userId, isCustom = true, maxTier = maxOf(it.maxTier, 25L), updatedAtMillis = now) })
        if (preview.armoryItems.isEmpty() && importModulePreset) {
          preview.generatedModule?.let {
            add(it.copy(userId = userId, isCustom = true, maxTier = maxOf(it.maxTier, 25L), updatedAtMillis = now))
          }
        }
      }
      val rigsToSave = buildList {
        addAll(preview.customRigs.map { it.copy(userId = userId, updatedAtMillis = now) })
        if (preview.customRigs.isEmpty() && importRigPreset) {
          preview.generatedRig?.let {
            add(it.copy(userId = userId, updatedAtMillis = now))
          }
        }
      }

      if (itemsToSave.isEmpty() && rigsToSave.isEmpty()) {
        return Result.failure(IllegalStateException("Select at least one Armory Module or Rig to import."))
      }

      if (itemsToSave.isNotEmpty()) {
        dao.upsertArmoryItems(itemsToSave)
      }
      if (rigsToSave.isNotEmpty()) {
        dao.upsertRigs(rigsToSave)
        val firstRig = rigsToSave.first()
        val profile = dao.getProfileOnce(userId)
        if (profile != null) {
          val updatedProfile = profile.copy(activeRigId = firstRig.id, updatedAtMillis = now)
          dao.upsertProfile(updatedProfile)
          mirrorCloud(userId) { it.pushProfile(updatedProfile) }
        }
      }

      mirrorCloud(userId) { sync ->
        itemsToSave.forEach { sync.pushArmoryItem(it) }
        rigsToSave.forEach { sync.pushRigAsset(it) }
      }

      val cloudNote = if (isCloudSyncAvailableFor(userId)) " & synced to Cloud" else ""
      Result.success(
        "Steam Workshop imported '${preview.title}': ${itemsToSave.size} module(s), ${rigsToSave.size} rig(s)$cloudNote",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private suspend inline fun mirrorCloud(userId: String, block: (UniverseCloudSync) -> Unit) {
    val sync = cloudSync ?: return
    if (!sync.isAuthenticatedFor(userId)) return
    _cloudSyncState.value = CloudSyncState.Syncing
    try {
      block(sync)
      _cloudSyncState.value = CloudSyncState.Synced(System.currentTimeMillis())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      _cloudSyncState.value = CloudSyncState.Error(e.localizedMessage ?: "Cloud sync warning")
    }
  }

  companion object {
    private fun createAuthProvider(appCtx: Context): () -> FirebaseAuth? = {
      if (runCatching { FirebaseApp.getApps(appCtx).isNotEmpty() }.getOrDefault(false)) {
        runCatching { Firebase.auth }.getOrNull()
      } else {
        null
      }
    }

    private fun resolveFirestore(context: Context): FirebaseFirestore? {
      val appCtx = context.applicationContext
      if (!runCatching { FirebaseApp.getApps(appCtx).isNotEmpty() }.getOrDefault(false)) {
        return null
      }
      val dbId = appCtx.getString(R.string.firestore_database_id)
      return runCatching { FirebaseFirestore.getInstance(dbId) }.getOrNull()
    }
  }

  private fun starterArmoryItems(userId: String, now: Long): List<ArmoryItem> = listOf(
    ArmoryItem(
      id = "perk_juggernaut",
      userId = userId,
      name = "Aegis Juggernaut Core",
      category = "perk",
      universeTag = "Sector Zero",
      description = "Reinforces kinetic armor shielding (+40 Max Shield per tier).",
      tier = 1L,
      maxTier = 25L,
      cooldownSec = 0L,
      powerValue = 140L,
      accentColorHex = "#FF3D71",
      equipped = true,
      isCustom = false,
      createdAtMillis = now,
      updatedAtMillis = now,
    ),
    ArmoryItem(
      id = "perk_overdrive",
      userId = userId,
      name = "Stamina Speed-Cola",
      category = "perk",
      universeTag = "Sector Zero",
      description = "Accelerates dash cadence, reload speed, and movement agility.",
      tier = 1L,
      maxTier = 25L,
      cooldownSec = 0L,
      powerValue = 125L,
      accentColorHex = "#00E676",
      equipped = true,
      isCustom = false,
      createdAtMillis = now + 1,
      updatedAtMillis = now + 1,
    ),
    ArmoryItem(
      id = "power_tendril",
      userId = userId,
      name = "Bio-Tendril Shockwave",
      category = "power",
      universeTag = "Blacklight Conduit",
      description = "Unleashes radial bio-electric tendrils that stun and clear nearby hostiles.",
      tier = 2L,
      maxTier = 25L,
      cooldownSec = 6L,
      powerValue = 220L,
      accentColorHex = "#7C4DFF",
      equipped = true,
      isCustom = false,
      createdAtMillis = now + 2,
      updatedAtMillis = now + 2,
    ),
    ArmoryItem(
      id = "power_repulse",
      userId = userId,
      name = "Starblade Force Repulse",
      category = "power",
      universeTag = "Aegis & Starblade",
      description = "Telekinetic blast wave paired with twin-core saber deflection.",
      tier = 1L,
      maxTier = 25L,
      cooldownSec = 8L,
      powerValue = 250L,
      accentColorHex = "#00E5FF",
      equipped = false,
      isCustom = false,
      createdAtMillis = now + 3,
      updatedAtMillis = now + 3,
    ),
    ArmoryItem(
      id = "power_chevron",
      userId = userId,
      name = "Chevron Chrono-Stasis",
      category = "power",
      universeTag = "Chrono-Vault",
      description = "Locks hostile waves inside a golden time-dilation field for 4 seconds.",
      tier = 1L,
      maxTier = 25L,
      cooldownSec = 12L,
      powerValue = 190L,
      accentColorHex = "#FFD54F",
      equipped = false,
      isCustom = false,
      createdAtMillis = now + 4,
      updatedAtMillis = now + 4,
    ),
    ArmoryItem(
      id = "upgrade_packtier",
      userId = userId,
      name = "Pack-a-Tier III Matrix",
      category = "upgrade",
      universeTag = "Sector Zero",
      description = "Supercharges primary energy blaster fire rate and multishot spread.",
      tier = 2L,
      maxTier = 25L,
      cooldownSec = 0L,
      powerValue = 210L,
      accentColorHex = "#FFAB40",
      equipped = true,
      isCustom = false,
      createdAtMillis = now + 5,
      updatedAtMillis = now + 5,
    ),
    ArmoryItem(
      id = "upgrade_claws",
      userId = userId,
      name = "Adamant Claw Edge",
      category = "upgrade",
      universeTag = "Hellbreaker",
      description = "Converts close-range dashes into armor-piercing fury strikes.",
      tier = 1L,
      maxTier = 25L,
      cooldownSec = 0L,
      powerValue = 180L,
      accentColorHex = "#FF5252",
      equipped = false,
      isCustom = false,
      createdAtMillis = now + 6,
      updatedAtMillis = now + 6,
    ),
    ArmoryItem(
      id = "consumable_maxcharge",
      userId = userId,
      name = "Omni-Surge Cell",
      category = "consumable",
      universeTag = "UnknownOS",
      description = "Instant tactical consumable: restores shields and resets all Power cooldowns.",
      tier = 1L,
      maxTier = 25L,
      cooldownSec = 15L,
      powerValue = 300L,
      accentColorHex = "#00E5FF",
      equipped = true,
      isCustom = false,
      createdAtMillis = now + 7,
      updatedAtMillis = now + 7,
    ),
  )

  private fun starterRigs(userId: String, now: Long): List<RigAsset> = listOf(
    RigAsset(
      id = "rig_operator",
      userId = userId,
      name = "Vanguard Tactical Rig",
      archetype = "operator",
      activeClip = "idle",
      ikEnabled = true,
      rigScale = 1.0,
      playbackSpeed = 1.0,
      poseAngles = UniverseCatalog.defaultPoseMap(),
      keyframeFrames = listOf(0L, 15L, 30L, 45L, 60L),
      isCompleted = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    ),
    RigAsset(
      id = "rig_biomorph",
      userId = userId,
      name = "Blacklight Conduit Rig",
      archetype = "biomorph",
      activeClip = "lunge",
      ikEnabled = true,
      rigScale = 1.05,
      playbackSpeed = 1.25,
      poseAngles = mapOf(
        "torso" to 14.0,
        "head" to -8.0,
        "lUpperArm" to -55.0,
        "lForearm" to -40.0,
        "rUpperArm" to 68.0,
        "rForearm" to 35.0,
        "lThigh" to -28.0,
        "lShin" to 34.0,
        "rThigh" to 24.0,
        "rShin" to -18.0,
        "weaponAngle" to 22.0,
      ),
      keyframeFrames = listOf(0L, 12L, 24L, 40L, 60L),
      isCompleted = true,
      createdAtMillis = now + 1,
      updatedAtMillis = now + 1,
    ),
    RigAsset(
      id = "rig_starblade",
      userId = userId,
      name = "Starblade Sentinel Rig",
      archetype = "starblade",
      activeClip = "slam",
      ikEnabled = true,
      rigScale = 1.0,
      playbackSpeed = 1.0,
      poseAngles = mapOf(
        "torso" to -8.0,
        "head" to 4.0,
        "lUpperArm" to -42.0,
        "lForearm" to -25.0,
        "rUpperArm" to 105.0,
        "rForearm" to 22.0,
        "lThigh" to -22.0,
        "lShin" to 24.0,
        "rThigh" to 26.0,
        "rShin" to -14.0,
        "weaponAngle" to -38.0,
      ),
      keyframeFrames = listOf(0L, 10L, 25L, 45L, 60L),
      isCompleted = true,
      createdAtMillis = now + 2,
      updatedAtMillis = now + 2,
    ),
    RigAsset(
      id = "rig_infernal",
      userId = userId,
      name = "Hellbreaker Claw Rig",
      archetype = "infernal",
      activeClip = "sprint",
      ikEnabled = true,
      rigScale = 1.12,
      playbackSpeed = 1.35,
      poseAngles = mapOf(
        "torso" to 20.0,
        "head" to -12.0,
        "lUpperArm" to 52.0,
        "lForearm" to -58.0,
        "rUpperArm" to -48.0,
        "rForearm" to 62.0,
        "lThigh" to -36.0,
        "lShin" to 45.0,
        "rThigh" to 34.0,
        "rShin" to -28.0,
        "weaponAngle" to 12.0,
      ),
      keyframeFrames = listOf(0L, 15L, 30L, 45L, 60L),
      isCompleted = true,
      createdAtMillis = now + 3,
      updatedAtMillis = now + 3,
    ),
  )
}

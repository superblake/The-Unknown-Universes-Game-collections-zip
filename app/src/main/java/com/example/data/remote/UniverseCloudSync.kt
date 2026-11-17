package com.example.data.remote

import com.example.data.local.UniverseDao
import com.example.data.model.ArmoryItem
import com.example.data.model.LinkedAccount
import com.example.data.model.MatchRecord
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.remote.FirestoreSyncCodec.isSupportedProviderId
import com.example.data.remote.FirestoreSyncCodec.toArmoryItemOrNull
import com.example.data.remote.FirestoreSyncCodec.toFirestoreCreateMap
import com.example.data.remote.FirestoreSyncCodec.toFirestoreMap
import com.example.data.remote.FirestoreSyncCodec.toFirestoreUpdateMap
import com.example.data.remote.FirestoreSyncCodec.toLinkedAccountOrNull
import com.example.data.remote.FirestoreSyncCodec.toMatchRecordOrNull
import com.example.data.remote.FirestoreSyncCodec.toOperatorProfileOrNull
import com.example.data.remote.FirestoreSyncCodec.toRigAssetOrNull
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.tasks.await

data class CloudSyncSummary(
  val syncedAtMillis: Long,
  val uploadedCount: Int,
  val downloadedCount: Int,
)

class UniverseCloudSync(
  private val db: FirebaseFirestore,
  private val dao: UniverseDao,
  private val authProvider: () -> FirebaseAuth?,
) {

  fun isAuthenticatedFor(userId: String): Boolean {
    val auth = authProvider() ?: return false
    val currentUid = auth.currentUser?.uid ?: return false
    return userId.isNotBlank() && currentUid == userId
  }

  private fun userDocRef(userId: String) = db.collection("users").document(userId)

  suspend fun pullMergeAndPush(
    userId: String,
    displayName: String?,
    starterArmoryProvider: (String, Long) -> List<ArmoryItem>,
    starterRigsProvider: (String, Long) -> List<RigAsset>,
    localGuestId: String = "local_operator",
  ): Result<CloudSyncSummary> {
    if (!isAuthenticatedFor(userId)) {
      return Result.failure(IllegalStateException("Google / Firebase session required for Cloud Sync."))
    }
    val userRef = userDocRef(userId)
    return try {
      val remoteProfileSnap = userRef.get().await()
      val remoteArmorySnap = userRef.collection("armoryItems").get().await()
      val remoteRigsSnap = userRef.collection("customRigs").get().await()
      val remoteLogsSnap = userRef.collection("matchLogs").get().await()
      val remoteLinksSnap = userRef.collection("linkedAccounts").get().await()

      val remoteProfile = remoteProfileSnap.toOperatorProfileOrNull()
      val remoteArmory = remoteArmorySnap.documents.mapNotNull { it.toArmoryItemOrNull(userId) }.associateBy { it.id }
      val remoteRigs = remoteRigsSnap.documents.mapNotNull { it.toRigAssetOrNull(userId) }.associateBy { it.id }
      val remoteLogs = remoteLogsSnap.documents.mapNotNull { it.toMatchRecordOrNull(userId) }.associateBy { it.id }
      val remoteLinks = remoteLinksSnap.documents.mapNotNull { it.toLinkedAccountOrNull(userId) }.associateBy { it.providerId }

      val localProfile = dao.getProfileOnce(userId)
      val localArmory = dao.getArmoryItemsOnce(userId).associateBy { it.id }
      val localRigs = dao.getRigsOnce(userId).associateBy { it.id }
      val localLogs = dao.getMatchLogsOnce(userId).associateBy { it.id }
      val localLinks = dao.getLinkedAccountsOnce(userId).associateBy { it.providerId }

      // Optional migration from local_operator when a user signs in after playing offline
      val guestProfile = if (userId != localGuestId) dao.getProfileOnce(localGuestId) else null
      val guestArmory = if (userId != localGuestId) {
        dao.getArmoryItemsOnce(localGuestId).map { it.copy(userId = userId) }
      } else {
        emptyList()
      }
      val guestRigs = if (userId != localGuestId) {
        dao.getRigsOnce(localGuestId).map { it.copy(userId = userId) }
      } else {
        emptyList()
      }
      val guestLogs = if (userId != localGuestId) {
        dao.getMatchLogsOnce(localGuestId).map { it.copy(userId = userId) }
      } else {
        emptyList()
      }
      val guestLinks = if (userId != localGuestId) {
        dao.getLinkedAccountsOnce(localGuestId).map {
          it.copy(id = "${userId}_${it.providerId}", userId = userId)
        }
      } else {
        emptyList()
      }

      val now = System.currentTimeMillis()
      val defaultCallsign = FirestoreSyncCodec.sanitizeCallsign(
        displayName ?: guestProfile?.callsign ?: localProfile?.callsign,
      )

      val mergedProfile = when {
        remoteProfile != null && localProfile != null -> {
          val localNewer = localProfile.updatedAtMillis > remoteProfile.updatedAtMillis
          remoteProfile.copy(
            id = userId,
            userId = userId,
            callsign = if (localNewer) FirestoreSyncCodec.sanitizeCallsign(localProfile.callsign) else remoteProfile.callsign,
            activeUniverseId = if (localNewer) localProfile.activeUniverseId else remoteProfile.activeUniverseId,
            activeMode = if (localNewer) localProfile.activeMode else remoteProfile.activeMode,
            activeRigId = if (localNewer) localProfile.activeRigId else remoteProfile.activeRigId,
            consolePreset = if (localNewer) localProfile.consolePreset else remoteProfile.consolePreset,
            totalEssence = if (localNewer) localProfile.totalEssence else remoteProfile.totalEssence,
            highScore = maxOf(remoteProfile.highScore, localProfile.highScore, guestProfile?.highScore ?: 0L),
            highestWave = maxOf(remoteProfile.highestWave, localProfile.highestWave, guestProfile?.highestWave ?: 0L),
            osWallpaperIndex = if (localNewer) localProfile.osWallpaperIndex else remoteProfile.osWallpaperIndex,
            updatedAtMillis = maxOf(remoteProfile.updatedAtMillis, localProfile.updatedAtMillis),
          )
        }
        remoteProfile != null -> {
          remoteProfile.copy(
            id = userId,
            userId = userId,
            highScore = maxOf(remoteProfile.highScore, guestProfile?.highScore ?: 0L),
            highestWave = maxOf(remoteProfile.highestWave, guestProfile?.highestWave ?: 0L),
          )
        }
        localProfile != null -> {
          localProfile.copy(
            id = userId,
            userId = userId,
            highScore = maxOf(localProfile.highScore, guestProfile?.highScore ?: 0L),
            highestWave = maxOf(localProfile.highestWave, guestProfile?.highestWave ?: 0L),
          )
        }
        guestProfile != null -> {
          guestProfile.copy(
            id = userId,
            userId = userId,
            callsign = FirestoreSyncCodec.sanitizeCallsign(guestProfile.callsign.ifBlank { defaultCallsign }),
            updatedAtMillis = now,
          )
        }
        else -> {
          OperatorProfile(
            id = userId,
            userId = userId,
            callsign = defaultCallsign,
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
        }
      }

      // Merge Armory (Remote + Local + Custom/Upgraded Guest + Starter fallback)
      val starterArmory = starterArmoryProvider(userId, now).associateBy { it.id }
      val mergedArmoryMap = linkedMapOf<String, ArmoryItem>()
      starterArmory.forEach { (id, item) -> mergedArmoryMap[id] = item }
      guestArmory.filter { it.isCustom || it.tier > 1L }.forEach { item ->
        val current = mergedArmoryMap[item.id]
        if (current == null || item.tier > current.tier) mergedArmoryMap[item.id] = item
      }
      localArmory.values.forEach { item ->
        val current = mergedArmoryMap[item.id]
        if (current == null || item.updatedAtMillis >= current.updatedAtMillis || item.tier > current.tier) {
          mergedArmoryMap[item.id] = item
        }
      }
      remoteArmory.values.forEach { item ->
        val current = mergedArmoryMap[item.id]
        if (current == null || item.updatedAtMillis > current.updatedAtMillis || item.tier > current.tier) {
          mergedArmoryMap[item.id] = item
        }
      }

      // Merge Rigs
      val starterRigs = starterRigsProvider(userId, now).associateBy { it.id }
      val mergedRigsMap = linkedMapOf<String, RigAsset>()
      starterRigs.forEach { (id, rig) -> mergedRigsMap[id] = rig }
      guestRigs.filter { it.id !in starterRigs }.forEach { rig ->
        mergedRigsMap.putIfAbsent(rig.id, rig)
      }
      localRigs.values.forEach { rig ->
        val current = mergedRigsMap[rig.id]
        if (current == null || rig.updatedAtMillis >= current.updatedAtMillis) {
          mergedRigsMap[rig.id] = rig
        }
      }
      remoteRigs.values.forEach { rig ->
        val current = mergedRigsMap[rig.id]
        if (current == null || rig.updatedAtMillis > current.updatedAtMillis) {
          mergedRigsMap[rig.id] = rig
        }
      }

      // Merge Match Logs (immutable records union)
      val mergedLogsMap = linkedMapOf<String, MatchRecord>()
      guestLogs.forEach { mergedLogsMap[it.id] = it }
      localLogs.values.forEach { mergedLogsMap[it.id] = it }
      remoteLogs.values.forEach { mergedLogsMap[it.id] = it }

      // Merge Linked Accounts
      val mergedLinksMap = linkedMapOf<String, LinkedAccount>()
      guestLinks.filter { isSupportedProviderId(it.providerId) }.forEach {
        mergedLinksMap[it.providerId] = it
      }
      localLinks.values.filter { isSupportedProviderId(it.providerId) }.forEach {
        mergedLinksMap[it.providerId] = it.copy(id = "${userId}_${it.providerId}", userId = userId)
      }
      remoteLinks.values.filter { isSupportedProviderId(it.providerId) }.forEach {
        val current = mergedLinksMap[it.providerId]
        if (current == null || it.linkedAtMillis >= current.linkedAtMillis) {
          mergedLinksMap[it.providerId] = it.copy(id = "${userId}_${it.providerId}", userId = userId)
        }
      }

      // Persist merged snapshot locally to Room first
      dao.upsertProfile(mergedProfile)
      dao.upsertArmoryItems(mergedArmoryMap.values.toList())
      dao.upsertRigs(mergedRigsMap.values.toList())
      if (mergedLogsMap.isNotEmpty()) dao.insertMatchLogs(mergedLogsMap.values.toList())
      if (mergedLinksMap.isNotEmpty()) dao.upsertLinkedAccounts(mergedLinksMap.values.toList())

      var uploaded = 0
      val downloaded = remoteArmory.size + remoteRigs.size + remoteLogs.size + remoteLinks.size +
        if (remoteProfile != null) 1 else 0

      // Push missing or newer profile state to Firestore
      if (remoteProfile == null) {
        userRef.set(mergedProfile.toFirestoreCreateMap(userId)).await()
        uploaded++
      } else if (mergedProfile.hasDifferenceFrom(remoteProfile)) {
        userRef.update(mergedProfile.toFirestoreUpdateMap()).await()
        uploaded++
      }

      // Push missing or newer Armory items
      mergedArmoryMap.values.forEach { item ->
        val remote = remoteArmory[item.id]
        val doc = userRef.collection("armoryItems").document(item.id)
        if (remote == null) {
          doc.set(item.toFirestoreCreateMap(userId)).await()
          uploaded++
        } else if (item.updatedAtMillis > remote.updatedAtMillis || item.tier > remote.tier || item.equipped != remote.equipped) {
          doc.update(item.toFirestoreUpdateMap()).await()
          uploaded++
        }
      }

      // Push missing or newer Rigs
      mergedRigsMap.values.forEach { rig ->
        val remote = remoteRigs[rig.id]
        val doc = userRef.collection("customRigs").document(rig.id)
        if (remote == null) {
          doc.set(rig.toFirestoreCreateMap(userId)).await()
          uploaded++
        } else if (rig.updatedAtMillis > remote.updatedAtMillis) {
          doc.update(rig.toFirestoreUpdateMap()).await()
          uploaded++
        }
      }

      // Push missing Match Logs (create-only)
      mergedLogsMap.values.forEach { log ->
        if (remoteLogs[log.id] == null) {
          userRef.collection("matchLogs").document(log.id).set(log.toFirestoreCreateMap(userId)).await()
          uploaded++
        }
      }

      // Push missing or newer Linked Accounts
      mergedLinksMap.values.forEach { link ->
        val remote = remoteLinks[link.providerId]
        if (remote == null || link.linkedAtMillis > remote.linkedAtMillis) {
          userRef.collection("linkedAccounts").document(link.providerId).set(link.toFirestoreMap(userId)).await()
          uploaded++
        }
      }

      Result.success(
        CloudSyncSummary(
          syncedAtMillis = System.currentTimeMillis(),
          uploadedCount = uploaded,
          downloadedCount = downloaded,
        ),
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userRef.path, authProvider())
      Result.failure(extractFirestoreRootError(e))
    }
  }

  /**
   * Real-time Firestore listener stream mirroring server changes into Room.
   */
  fun observeAndMirrorCloud(userId: String): Flow<Long> {
    val userRef = userDocRef(userId)
    val profileFlow = userRef.snapshots(MetadataChanges.EXCLUDE).map { snap ->
      if (!snap.metadata.hasPendingWrites() && snap.exists()) {
        val remote = snap.toOperatorProfileOrNull()
        if (remote != null) {
          val local = dao.getProfileOnce(userId)
          if (local == null || remote.updatedAtMillis >= local.updatedAtMillis ||
            remote.highScore > local.highScore || remote.highestWave > local.highestWave
          ) {
            dao.upsertProfile(
              remote.copy(
                highScore = maxOf(remote.highScore, local?.highScore ?: 0L),
                highestWave = maxOf(remote.highestWave, local?.highestWave ?: 0L),
              ),
            )
          }
        }
      }
      System.currentTimeMillis()
    }

    val armoryFlow = userRef.collection("armoryItems").snapshots(MetadataChanges.EXCLUDE).map { snap ->
      if (!snap.metadata.hasPendingWrites()) {
        val items = snap.documents.mapNotNull { it.toArmoryItemOrNull(userId) }
        if (items.isNotEmpty()) dao.upsertArmoryItems(items)
      }
      System.currentTimeMillis()
    }

    val rigsFlow = userRef.collection("customRigs").snapshots(MetadataChanges.EXCLUDE).map { snap ->
      if (!snap.metadata.hasPendingWrites()) {
        val rigs = snap.documents.mapNotNull { it.toRigAssetOrNull(userId) }
        if (rigs.isNotEmpty()) dao.upsertRigs(rigs)
      }
      System.currentTimeMillis()
    }

    val logsFlow = userRef.collection("matchLogs").snapshots(MetadataChanges.EXCLUDE).map { snap ->
      if (!snap.metadata.hasPendingWrites()) {
        val logs = snap.documents.mapNotNull { it.toMatchRecordOrNull(userId) }
        if (logs.isNotEmpty()) dao.insertMatchLogs(logs)
      }
      System.currentTimeMillis()
    }

    val linksFlow = userRef.collection("linkedAccounts").snapshots(MetadataChanges.EXCLUDE).map { snap ->
      if (!snap.metadata.hasPendingWrites()) {
        val links = snap.documents.mapNotNull { it.toLinkedAccountOrNull(userId) }
        if (links.isNotEmpty()) dao.upsertLinkedAccounts(links)
      }
      System.currentTimeMillis()
    }

    return merge(profileFlow, armoryFlow, rigsFlow, logsFlow, linksFlow)
  }

  suspend fun pushProfile(profile: OperatorProfile): Result<Unit> {
    if (!isAuthenticatedFor(profile.userId)) return Result.success(Unit)
    val ref = userDocRef(profile.userId)
    return try {
      try {
        ref.update(profile.toFirestoreUpdateMap()).await()
      } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
          ref.set(profile.toFirestoreCreateMap(profile.userId)).await()
        } else {
          throw e
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, ref.path, authProvider())
      Result.failure(extractFirestoreRootError(e))
    }
  }

  suspend fun pushArmoryItem(item: ArmoryItem): Result<Unit> {
    if (!isAuthenticatedFor(item.userId)) return Result.success(Unit)
    val ref = userDocRef(item.userId).collection("armoryItems").document(item.id)
    return try {
      try {
        ref.update(item.toFirestoreUpdateMap()).await()
      } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
          ref.set(item.toFirestoreCreateMap(item.userId)).await()
        } else {
          throw e
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, ref.path, authProvider())
      Result.failure(extractFirestoreRootError(e))
    }
  }

  suspend fun deleteArmoryItem(userId: String, itemId: String): Result<Unit> {
    if (!isAuthenticatedFor(userId)) return Result.success(Unit)
    val ref = userDocRef(userId).collection("armoryItems").document(itemId)
    return try {
      ref.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, ref.path, authProvider())
      Result.failure(extractFirestoreRootError(e))
    }
  }

  suspend fun pushRigAsset(rig: RigAsset): Result<Unit> {
    if (!isAuthenticatedFor(rig.userId)) return Result.success(Unit)
    val ref = userDocRef(rig.userId).collection("customRigs").document(rig.id)
    return try {
      try {
        ref.update(rig.toFirestoreUpdateMap()).await()
      } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
          ref.set(rig.toFirestoreCreateMap(rig.userId)).await()
        } else {
          throw e
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, ref.path, authProvider())
      Result.failure(extractFirestoreRootError(e))
    }
  }

  suspend fun pushMatchRecord(record: MatchRecord): Result<Unit> {
    if (!isAuthenticatedFor(record.userId)) return Result.success(Unit)
    val ref = userDocRef(record.userId).collection("matchLogs").document(record.id)
    return try {
      ref.set(record.toFirestoreCreateMap(record.userId)).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, ref.path, authProvider())
      Result.failure(extractFirestoreRootError(e))
    }
  }

  suspend fun pushLinkedAccount(account: LinkedAccount): Result<Unit> {
    if (!isAuthenticatedFor(account.userId)) return Result.success(Unit)
    if (!isSupportedProviderId(account.providerId)) return Result.success(Unit)
    val ref = userDocRef(account.userId).collection("linkedAccounts").document(account.providerId)
    return try {
      ref.set(account.toFirestoreMap(account.userId)).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, ref.path, authProvider())
      Result.failure(extractFirestoreRootError(e))
    }
  }

  suspend fun deleteLinkedAccount(userId: String, providerId: String): Result<Unit> {
    if (!isAuthenticatedFor(userId)) return Result.success(Unit)
    val ref = userDocRef(userId).collection("linkedAccounts").document(providerId)
    return try {
      ref.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, ref.path, authProvider())
      Result.failure(extractFirestoreRootError(e))
    }
  }

  private fun OperatorProfile.hasDifferenceFrom(other: OperatorProfile): Boolean {
    return callsign != other.callsign ||
      activeUniverseId != other.activeUniverseId ||
      activeMode != other.activeMode ||
      activeRigId != other.activeRigId ||
      consolePreset != other.consolePreset ||
      totalEssence != other.totalEssence ||
      highScore != other.highScore ||
      highestWave != other.highestWave ||
      osWallpaperIndex != other.osWallpaperIndex
  }
}

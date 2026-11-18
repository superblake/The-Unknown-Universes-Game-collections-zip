package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CloudSyncState
import com.example.data.local.ProgressionStore
import com.example.data.local.SaveBundlePreview
import com.example.data.local.SaveDataSerializer
import com.example.data.local.UniverseRepository
import com.example.data.model.ArmoryItem
import com.example.data.model.BattlePassVersion
import com.example.data.model.ChallengeState
import com.example.data.model.CharacterCustomization
import com.example.data.model.LevelCustomization
import com.example.data.model.LinkedAccount
import com.example.data.model.MatchProgressionReport
import com.example.data.model.MatchRecord
import com.example.data.model.OperatorProfile
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.data.model.WeaponCustomization
import com.example.data.remote.extractFirestoreRootError
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OsWorkspaceTab(val label: String, val routeTag: String) {
  OS_DESKTOP("OS", "os_desktop"),
  ARENA_PLAY("Arena", "arena_play"),
  FORGE_RIG("Forge", "forge_rig"),
  ARMORY("Armory", "armory"),
  MEDIA_VAULT("Vault", "media_vault"),
  EXPORT_COMPAT("Export", "export_compat"),
}

sealed interface UniverseUiState {
  data object Loading : UniverseUiState
  data class Ready(
    val profile: OperatorProfile,
    val armory: List<ArmoryItem>,
    val rigs: List<RigAsset>,
    val matchLogs: List<MatchRecord>,
    val linkedAccounts: List<LinkedAccount> = emptyList(),
  ) : UniverseUiState
  data class Error(val message: String) : UniverseUiState
}

class UniverseViewModel(
  private val repository: UniverseRepository,
  private val currentUserId: String,
  private val defaultDisplayName: String?,
  private val progressionStore: ProgressionStore? = null,
) : ViewModel() {

  private val _activeTab = MutableStateFlow(OsWorkspaceTab.OS_DESKTOP)
  val activeTab: StateFlow<OsWorkspaceTab> = _activeTab.asStateFlow()

  private val _statusBanner = MutableStateFlow<String?>(null)
  val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

  val cloudSyncState: StateFlow<CloudSyncState> = repository.cloudSyncState

  val progressionState: StateFlow<OperatorProgressionState> =
    progressionStore?.state ?: MutableStateFlow(OperatorProgressionState()).asStateFlow()

  val challengeStates: StateFlow<List<ChallengeState>> =
    progressionState
      .map { state ->
        progressionStore?.challengeStates(state)
          ?: ProgressionCatalog.challenges.map { ChallengeState(it, 0L, false) }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = progressionStore?.challengeStates()
          ?: ProgressionCatalog.challenges.map { ChallengeState(it, 0L, false) },
      )

  private var cloudListenerJob: Job? = null

  init {
    viewModelScope.launch {
      val initRes = repository.ensureStarterData(currentUserId, defaultDisplayName)
      if (initRes.isFailure) {
        _statusBanner.value = initRes.exceptionOrNull()?.localizedMessage ?: "Local storage warning"
      }
      if (repository.isCloudSyncAvailableFor(currentUserId)) {
        val syncRes = repository.syncWithCloud(currentUserId, defaultDisplayName)
        if (syncRes.isFailure) {
          Log.w(TAG, "Initial cloud sync failed", syncRes.exceptionOrNull())
        }
        startCloudListener()
      }
    }
  }

  private fun startCloudListener() {
    if (cloudListenerJob?.isActive == true) return
    cloudListenerJob = viewModelScope.launch {
      repository.observeCloudChanges(currentUserId)
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val rootError = extractFirestoreRootError(error)
          Log.w(TAG, "Cloud snapshot listener warning", rootError)
        }
        .collect()
    }
  }

  val uiState: StateFlow<UniverseUiState> =
    combine(
      repository.observeProfile(currentUserId),
      repository.observeArmoryItems(currentUserId),
      repository.observeCustomRigs(currentUserId),
      repository.observeMatchLogs(currentUserId),
      repository.observeLinkedAccounts(currentUserId),
    ) { profile, armory, rigs, logs, linked ->
      if (profile == null) {
        UniverseUiState.Loading
      } else {
        UniverseUiState.Ready(
          profile = profile,
          armory = armory,
          rigs = rigs,
          matchLogs = logs,
          linkedAccounts = linked,
        )
      }
    }
      .catch { error ->
        if (error is CancellationException) throw error
        Log.w(TAG, "Error observing Universe state", error)
        emit(UniverseUiState.Error(error.localizedMessage ?: "Failed to load UnknownOS data"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UniverseUiState.Loading,
      )

  fun selectTab(tab: OsWorkspaceTab) {
    _activeTab.value = tab
  }

  fun dismissBanner() {
    _statusBanner.value = null
  }

  fun showBanner(message: String) {
    _statusBanner.value = message
  }

  fun syncNow() {
    viewModelScope.launch {
      val res = repository.syncWithCloud(currentUserId, defaultDisplayName)
      if (res.isSuccess) {
        startCloudListener()
        _statusBanner.value = res.getOrThrow()
      } else {
        _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Cloud Sync unavailable"
      }
    }
  }

  fun exportSaveJson(onReady: (String) -> Unit) {
    viewModelScope.launch {
      val res = repository.exportSaveDataJson(currentUserId)
      if (res.isSuccess) {
        onReady(res.getOrThrow())
      } else {
        _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Could not export save data"
      }
    }
  }

  fun parseSavePreview(rawJson: String): Result<SaveBundlePreview> =
    SaveDataSerializer.parseAndValidateSaveJson(rawJson, currentUserId)

  fun importSavePreview(preview: SaveBundlePreview, replaceExisting: Boolean) {
    viewModelScope.launch {
      val res = repository.importSaveBundle(
        userId = currentUserId,
        bundle = preview.bundle,
        replaceExisting = replaceExisting,
      )
      _statusBanner.value = if (res.isSuccess) {
        res.getOrThrow()
      } else {
        res.exceptionOrNull()?.localizedMessage ?: "Save import failed"
      }
    }
  }

  fun importSteamWorkshopPreview(
    preview: com.example.export.SteamWorkshopImportPreview,
    importModulePreset: Boolean = true,
    importRigPreset: Boolean = true,
  ) {
    viewModelScope.launch {
      val res = repository.importSteamWorkshopPreview(
        userId = currentUserId,
        preview = preview,
        importModulePreset = importModulePreset,
        importRigPreset = importRigPreset,
      )
      _statusBanner.value = if (res.isSuccess) {
        res.getOrThrow()
      } else {
        res.exceptionOrNull()?.localizedMessage ?: "Steam Workshop import failed"
      }
    }
  }

  fun selectUniverse(universeId: String) {
    viewModelScope.launch {
      val world = UniverseCatalog.worldById(universeId)
      repository.updateProfileSelection(
        userId = currentUserId,
        activeUniverseId = world.id,
        activeMode = world.defaultModeId,
      )
      _statusBanner.value = "Mounted ${world.title}"
    }
  }

  fun mountAndDeploySeries(universeId: String, modeId: String) {
    viewModelScope.launch {
      val world = UniverseCatalog.worldById(universeId)
      val mode = UniverseCatalog.modeById(modeId)
      repository.updateProfileSelection(
        userId = currentUserId,
        activeUniverseId = world.id,
        activeMode = mode.id,
      )
      _statusBanner.value = "Mounted ${world.title} (${mode.label})"
      _activeTab.value = OsWorkspaceTab.ARENA_PLAY
    }
  }

  fun selectMode(modeId: String) {
    viewModelScope.launch {
      repository.updateProfileSelection(userId = currentUserId, activeMode = modeId)
    }
  }

  fun selectConsolePreset(presetId: String) {
    viewModelScope.launch {
      repository.updateProfileSelection(userId = currentUserId, consolePreset = presetId)
      val target = UniverseCatalog.consoleById(presetId)
      _statusBanner.value = "Controller & HUD profile: ${target.label}"
    }
  }

  fun selectActiveRig(rigId: String) {
    viewModelScope.launch {
      repository.updateProfileSelection(userId = currentUserId, activeRigId = rigId)
      _statusBanner.value = "Active Rig mounted to Arena & OS Avatar"
    }
  }

  fun cycleWallpaper(currentIndex: Long) {
    viewModelScope.launch {
      val next = (currentIndex + 1L) % 4L
      repository.updateProfileSelection(userId = currentUserId, osWallpaperIndex = next)
    }
  }

  fun updateCallsign(callsign: String) {
    viewModelScope.launch {
      val res = repository.updateProfileSelection(userId = currentUserId, callsign = callsign)
      if (res.isSuccess) _statusBanner.value = "Callsign updated"
    }
  }

  fun toggleArmoryEquip(item: ArmoryItem) {
    viewModelScope.launch {
      val res = repository.toggleArmoryEquipped(currentUserId, item)
      if (res.isFailure) {
        _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Equip failed"
      }
    }
  }

  fun upgradeArmoryItem(item: ArmoryItem, currentEssence: Long) {
    val cost = item.tier * 300L
    viewModelScope.launch {
      val res = repository.upgradeArmoryItem(currentUserId, item, cost, currentEssence)
      _statusBanner.value =
        if (res.isSuccess) "${item.name} upgraded to Tier ${item.tier + 1}"
        else res.exceptionOrNull()?.message ?: "Upgrade failed"
    }
  }

  fun createCustomArmoryItem(
    name: String,
    category: String,
    universeTag: String,
    description: String,
    cooldownSec: Long,
    powerValue: Long,
    accentColorHex: String,
  ) {
    viewModelScope.launch {
      val res =
        repository.addArmoryItem(
          userId = currentUserId,
          name = name,
          category = category,
          universeTag = universeTag,
          description = description,
          cooldownSec = cooldownSec,
          powerValue = powerValue,
          accentColorHex = accentColorHex,
          equipped = true,
          isCustom = true,
        )
      _statusBanner.value =
        if (res.isSuccess) "Forged custom $category: $name"
        else res.exceptionOrNull()?.localizedMessage ?: "Failed to forge item"
    }
  }

  fun deleteCustomArmoryItem(itemId: String) {
    viewModelScope.launch {
      repository.deleteArmoryItem(currentUserId, itemId)
    }
  }

  fun saveRig(
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
  ) {
    viewModelScope.launch {
      val res =
        repository.saveRigAsset(
          userId = currentUserId,
          rigId = rigId,
          name = name,
          archetype = archetype,
          activeClip = activeClip,
          ikEnabled = ikEnabled,
          rigScale = rigScale,
          playbackSpeed = playbackSpeed,
          poseAngles = poseAngles,
          keyframeFrames = keyframeFrames,
          isCompleted = isCompleted,
        )
      if (res.isSuccess) {
        val savedId = res.getOrThrow()
        repository.updateProfileSelection(userId = currentUserId, activeRigId = savedId)
        val cloudSuffix = if (repository.isCloudSyncAvailableFor(currentUserId)) " & synced to Cloud" else " locally"
        _statusBanner.value = "Saved rig '$name' ($activeClip)$cloudSuffix"
      } else {
        _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Rig save failed"
      }
    }
  }

  fun updateWeaponCustomization(transform: (WeaponCustomization) -> WeaponCustomization) {
    progressionStore?.updateWeaponCustomization(transform)
  }

  fun updateCharacterCustomization(transform: (CharacterCustomization) -> CharacterCustomization) {
    progressionStore?.updateCharacterCustomization(transform)
  }

  fun updateLevelCustomization(transform: (LevelCustomization) -> LevelCustomization) {
    progressionStore?.updateLevelCustomization(transform)
  }

  fun activateOrUnlockPass(version: BattlePassVersion, currentEssence: Long) {
    val store = progressionStore ?: return
    val res = store.activateOrUnlockPassVersion(version, currentEssence)
    if (res.isFailure) {
      _statusBanner.value = res.exceptionOrNull()?.message ?: "Could not unlock Battle Pass"
      return
    }
    val spent = res.getOrThrow()
    viewModelScope.launch {
      if (spent > 0L) {
        repository.adjustOperatorEssence(currentUserId, -spent)
        _statusBanner.value = "Unlocked & Mounted ${version.title}!"
      } else {
        _statusBanner.value = "Mounted ${version.title}"
      }
    }
  }

  fun claimPassTier(version: BattlePassVersion, tier: Int) {
    val store = progressionStore ?: return
    val res = store.claimPassTier(version, tier)
    if (res.isFailure) {
      _statusBanner.value = res.exceptionOrNull()?.message ?: "Cannot claim tier"
      return
    }
    val rewardEssence = res.getOrThrow()
    viewModelScope.launch {
      if (rewardEssence > 0L) {
        repository.adjustOperatorEssence(currentUserId, rewardEssence)
      }
      _statusBanner.value = "Claimed Tier $tier (+$rewardEssence Essence)"
    }
  }

  fun claimChallengeReward(challengeId: String) {
    val store = progressionStore ?: return
    val res = store.claimChallengeReward(challengeId)
    if (res.isFailure) {
      _statusBanner.value = res.exceptionOrNull()?.message ?: "Cannot claim challenge"
      return
    }
    val (essenceGain, playerXpGain, bpXpGain) = res.getOrThrow()
    viewModelScope.launch {
      if (essenceGain > 0L) {
        repository.adjustOperatorEssence(currentUserId, essenceGain)
      }
      _statusBanner.value = "Challenge Complete! +$essenceGain Essence • +$playerXpGain XP • +$bpXpGain BP XP"
    }
  }

  fun refreshCompletedChallenges() {
    progressionStore?.resetCompletedChallengesForReplay()
    _statusBanner.value = "Completed challenges reset for replay!"
  }

  fun recordArenaProgression(report: MatchProgressionReport) {
    val store = progressionStore ?: return
    val (xpGained, playerLvlUps, wpnLvlUps) = store.recordMatchProgression(report)
    val notes = mutableListOf("+$xpGained XP")
    if (playerLvlUps > 0L) notes.add("PLAYER LV UP (+$playerLvlUps)")
    if (wpnLvlUps > 0L) notes.add("WEAPON LV UP (+$wpnLvlUps)")
    _statusBanner.value = notes.joinToString(" • ")
  }

  fun submitMatchResult(
    profile: OperatorProfile,
    universeId: String,
    mode: String,
    score: Long,
    waveReached: Long,
    eliminations: Long,
    extracted: Boolean,
    durationSec: Long,
  ) {
    val passMult = progressionState.value.activePassVersion.essenceMultiplier
    val baseEssence = (score / 10L).coerceAtLeast(150L) + if (extracted) 350L else 0L
    val essenceEarned = (baseEssence * passMult).toLong()
    viewModelScope.launch {
      val res =
        repository.recordMatchResult(
          userId = currentUserId,
          profile = profile,
          universeId = universeId,
          mode = mode,
          score = score,
          waveReached = waveReached,
          eliminations = eliminations,
          extracted = extracted,
          durationSec = durationSec,
          essenceEarned = essenceEarned,
        )
      _statusBanner.value =
        if (res.isSuccess) {
          val suffix = if (repository.isCloudSyncAvailableFor(currentUserId)) " & synced to Cloud" else ""
          "Match saved! +$essenceEarned Essence earned$suffix"
        } else {
          res.exceptionOrNull()?.localizedMessage ?: "Failed to save match"
        }
    }
  }

  fun saveLinkedAccount(
    providerId: String,
    displayName: String,
    externalSubject: String,
    bridgedAccounts: List<com.example.ui.auth.BridgedExternalAccount> = emptyList(),
  ) {
    viewModelScope.launch {
      val res = repository.saveLinkedAccount(currentUserId, providerId, displayName, externalSubject)
      bridgedAccounts.forEach { bridged ->
        repository.saveLinkedAccount(
          userId = currentUserId,
          providerId = bridged.providerId,
          displayName = bridged.displayName,
          externalSubject = bridged.externalSubject,
        )
      }
      _statusBanner.value =
        if (res.isSuccess) {
          if (bridgedAccounts.isEmpty()) "Linked $displayName"
          else "Linked $displayName (+${bridgedAccounts.size} bridged accounts)"
        } else {
          res.exceptionOrNull()?.localizedMessage ?: "Failed to save linked account"
        }
    }
  }

  fun unlinkExternalAccount(providerId: String) {
    viewModelScope.launch {
      val res = repository.unlinkAccount(currentUserId, providerId)
      if (res.isSuccess) _statusBanner.value = "Unlinked $providerId"
    }
  }

  private companion object {
    const val TAG = "UniverseVM"
  }
}

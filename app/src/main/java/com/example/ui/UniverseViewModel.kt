package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CloudSyncState
import com.example.data.local.ProgressionStore
import com.example.data.local.SaveBundlePreview
import com.example.data.local.SaveDataSerializer
import com.example.data.local.UniverseRepository
import com.example.data.model.AchievementState
import com.example.data.model.ArmoryItem
import com.example.data.model.BattlePassVersion
import com.example.data.model.BlastCoreColor
import com.example.data.model.ChallengeState
import com.example.data.model.CharacterCustomization
import com.example.data.model.CollectionCustomModeConfig
import com.example.data.model.CollectionCustomScope
import com.example.data.model.CollectionDifficulty
import com.example.data.model.CustomMapGameCategory
import com.example.data.model.EquipmentSlotType
import com.example.data.model.GearSlot
import com.example.data.model.LevelCustomization
import com.example.data.model.ChatChannel
import com.example.data.model.ChatMessage
import com.example.data.model.ClanData
import com.example.data.model.FriendEntry
import com.example.data.model.LinkedAccount
import com.example.data.model.LobbyConfig
import com.example.data.model.MatchProgressionReport
import com.example.data.model.MatchRecord
import com.example.data.model.MissionScope
import com.example.data.model.MissionState
import com.example.data.model.MissionsCatalog
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.MultiverseMatchListing
import com.example.data.model.NewGamePlusVersion
import com.example.data.model.OperatorProfile
import com.example.data.model.OperatorProgressionState
import com.example.data.model.PowerTier
import com.example.data.model.PrivateMatchRules
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.SocialCatalog
import com.example.data.model.UpgradeCosts
import com.example.data.model.StarRankMeta
import com.example.data.model.UniverseCatalog
import com.example.data.model.UserCustomGameMapSpec
import com.example.data.model.UserCustomWeaponSpec
import com.example.data.model.UserCustomPerkSpec
import com.example.data.model.UserCustomGearSpec
import com.example.data.model.UserCustomEquipmentSpec
import com.example.data.model.GearSlotCustomization
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

  private val _selectedArmorySection = MutableStateFlow("armory_items")
  val selectedArmorySection: StateFlow<String> = _selectedArmorySection.asStateFlow()

  private val _selectedArmoryCategory = MutableStateFlow("ALL")
  val selectedArmoryCategory: StateFlow<String> = _selectedArmoryCategory.asStateFlow()

  private val _optimisticSelection = MutableStateFlow<Pair<String?, String?>?>(null)

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

  val achievementStates: StateFlow<List<AchievementState>> =
    progressionState
      .map { state ->
        progressionStore?.achievementStates(state)
          ?: ProgressionCatalog.achievements.map { AchievementState(it, 0L, false) }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = progressionStore?.achievementStates()
          ?: ProgressionCatalog.achievements.map { AchievementState(it, 0L, false) },
      )

  val missionStates: StateFlow<List<MissionState>> =
    progressionState
      .map { state ->
        progressionStore?.missionStates(state = state)
          ?: MissionsCatalog.allMissions.map { MissionState(it, 0L, false) }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = progressionStore?.missionStates()
          ?: MissionsCatalog.allMissions.map { MissionState(it, 0L, false) },
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
      combine(
        repository.observeProfile(currentUserId),
        repository.observeArmoryItems(currentUserId),
        repository.observeCustomRigs(currentUserId),
      ) { profile, armory, rigs -> Triple(profile, armory, rigs) },
      repository.observeMatchLogs(currentUserId),
      repository.observeLinkedAccounts(currentUserId),
      _optimisticSelection,
    ) { (profile, armory, rigs), logs, linked, opt ->
      if (profile == null) {
        UniverseUiState.Loading
      } else {
        val effectiveProfile = if (opt != null) {
          profile.copy(
            activeUniverseId = opt.first ?: profile.activeUniverseId,
            activeMode = opt.second ?: profile.activeMode,
          )
        } else {
          profile
        }
        UniverseUiState.Ready(
          profile = effectiveProfile,
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

  fun selectArmorySection(sectionId: String) {
    _selectedArmorySection.value = sectionId
  }

  fun selectArmoryCategory(categoryId: String) {
    _selectedArmoryCategory.value = categoryId
  }

  fun dismissBanner() {
    _statusBanner.value = null
  }

  fun showBanner(message: String) {
    _statusBanner.value = message
  }

  fun verifyProgressionIntegrity(): Boolean =
    progressionStore?.verifyStoredIntegrity() ?: true

  private inline fun runEssenceUpgrade(
    res: Result<Long>,
    fallbackError: String = "Upgrade failed",
    crossinline bannerOnSuccess: (cost: Long) -> String,
  ) {
    if (res.isFailure) {
      _statusBanner.value = res.exceptionOrNull()?.message ?: fallbackError
      return
    }
    val cost = res.getOrThrow()
    viewModelScope.launch {
      if (cost > 0L) repository.adjustOperatorEssence(currentUserId, -cost)
      _statusBanner.value = bannerOnSuccess(cost)
    }
  }

  private inline fun runEssenceReward(
    res: Result<Long>,
    fallbackError: String = "Action failed",
    crossinline bannerOnSuccess: (reward: Long) -> String,
  ) {
    if (res.isFailure) {
      _statusBanner.value = res.exceptionOrNull()?.message ?: fallbackError
      return
    }
    val reward = res.getOrThrow()
    viewModelScope.launch {
      if (reward > 0L) repository.adjustOperatorEssence(currentUserId, reward)
      _statusBanner.value = bannerOnSuccess(reward)
    }
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
      val res = repository.exportSaveDataJson(
        userId = currentUserId,
        progressionJson = progressionStore?.exportJsonObject(),
      )
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
      if (res.isSuccess) {
        _optimisticSelection.value = null
        preview.bundle.progressionJson?.let { progJson ->
          progressionStore?.importJsonObject(progJson, mergePreferHigher = !replaceExisting)
        }
      }
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
      preview.customMap?.let { customMap ->
        progressionStore?.addOrUpdateCustomGameMap(customMap)
      }
      val res = repository.importSteamWorkshopPreview(
        userId = currentUserId,
        preview = preview,
        importModulePreset = importModulePreset,
        importRigPreset = importRigPreset,
      )
      _statusBanner.value = if (res.isSuccess) {
        val mapSuffix = preview.customMap?.let { " • Custom Map '${it.title}' ready!" }.orEmpty()
        "${res.getOrThrow()}$mapSuffix"
      } else {
        res.exceptionOrNull()?.localizedMessage ?: "Steam Workshop import failed"
      }
    }
  }

  fun selectUniverse(universeId: String) {
    val world = UniverseCatalog.worldById(universeId)
    val curJoined = progressionState.value.joinedMatch
    if (curJoined != null && curJoined.worldId != world.id) {
      progressionStore?.leaveJoinedMatch()
    }
    val primaryAreaId = UniverseCatalog.primaryAreaIdForWorld(world.id)
    val curArea = progressionState.value.resolveAreaSpec(progressionState.value.activeAreaId)
    val shouldResetToPrimary = curArea.linkedWorldId != world.id
    if (shouldResetToPrimary) {
      progressionStore?.selectArea(primaryAreaId)
    }
    val resolvedMode = world.defaultModeId
    _optimisticSelection.value = Pair(world.id, resolvedMode)
    _statusBanner.value = "Mounted ${world.title} (${UniverseCatalog.modeById(resolvedMode).shortTag})"
    viewModelScope.launch {
      repository.updateProfileSelection(
        userId = currentUserId,
        activeUniverseId = world.id,
        activeMode = resolvedMode,
      )
    }
  }

  fun mountAndDeploySeries(universeId: String, modeId: String) {
    val world = UniverseCatalog.worldById(universeId)
    val mode = UniverseCatalog.modeById(modeId)
    val curJoined = progressionState.value.joinedMatch
    if (curJoined != null && (curJoined.worldId != world.id || curJoined.modeId != mode.id)) {
      progressionStore?.leaveJoinedMatch()
    }
    val primaryAreaId = UniverseCatalog.primaryAreaIdForWorld(world.id)
    val curArea = progressionState.value.resolveAreaSpec(progressionState.value.activeAreaId)
    val shouldResetToPrimary = curArea.linkedWorldId != world.id
    if (shouldResetToPrimary) {
      progressionStore?.selectArea(primaryAreaId)
    }
    _optimisticSelection.value = Pair(world.id, mode.id)
    _statusBanner.value = "Mounted ${world.title} (${mode.label})"
    _activeTab.value = OsWorkspaceTab.ARENA_PLAY
    viewModelScope.launch {
      repository.updateProfileSelection(
        userId = currentUserId,
        activeUniverseId = world.id,
        activeMode = mode.id,
      )
    }
  }

  fun selectMode(modeId: String) {
    val mode = UniverseCatalog.modeById(modeId)
    val curUniverse = _optimisticSelection.value?.first
      ?: (uiState.value as? UniverseUiState.Ready)?.profile?.activeUniverseId
    _optimisticSelection.value = Pair(curUniverse, mode.id)
    _statusBanner.value = "Mode active: ${mode.label} [${mode.shortTag}]"
    viewModelScope.launch {
      repository.updateProfileSelection(userId = currentUserId, activeMode = mode.id)
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
    viewModelScope.launch {
      val liveItem = repository.getArmoryItem(currentUserId, item.id) ?: item
      val cost = UpgradeCosts.armoryModTier(liveItem.tier.toInt())
      val res = repository.upgradeArmoryItem(currentUserId, liveItem, cost, currentEssence)
      if (res.isSuccess) {
        val updated = repository.getArmoryItem(currentUserId, item.id) ?: liveItem
        _statusBanner.value = "${updated.name} upgraded to Tier ${updated.tier}/25 (${ProgressionCatalog.tierMeta(updated.tier.toInt()).label})!"
      } else {
        _statusBanner.value = res.exceptionOrNull()?.message ?: "Upgrade failed"
      }
    }
  }

  fun applyTierProtocolCode(code: String) {
    val store = progressionStore
    if (store == null) {
      _statusBanner.value = "Progression store offline"
      return
    }
    val res = store.applyProtocolCode(code)
    if (res.isSuccess) {
      viewModelScope.launch {
        val upper = code.trim().uppercase(java.util.Locale.US)
        when {
          upper.contains("25") || upper.contains("MAX") || upper.contains("GOD") -> {
            repository.boostAllArmoryTiers(currentUserId, targetTier = 25)
          }
          upper.contains("20") -> repository.boostAllArmoryTiers(currentUserId, targetTier = 20)
          upper.contains("15") -> repository.boostAllArmoryTiers(currentUserId, targetTier = 15)
          upper.contains("10") -> repository.boostAllArmoryTiers(currentUserId, targetTier = 10)
          upper.contains("BOOST") || upper.contains("+5") -> repository.boostAllArmoryTiers(currentUserId, delta = 5)
        }
        if (upper == "ESSENCE999" || upper == "RICH") {
          repository.adjustOperatorEssence(currentUserId, 500_000L)
        }
      }
      _statusBanner.value = res.getOrThrow()
    } else {
      _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Invalid code"
    }
  }

  fun boostAllTiers(delta: Int = 1) {
    progressionStore?.boostAllTiers(delta)
    viewModelScope.launch {
      repository.boostAllArmoryTiers(currentUserId, delta = delta)
    }
    _statusBanner.value = "All systems boosted +$delta Tiers!"
  }

  fun maxAllTiers() {
    progressionStore?.setAllTiers(25)
    viewModelScope.launch {
      repository.boostAllArmoryTiers(currentUserId, targetTier = 25)
    }
    _statusBanner.value = "All systems maxed to TIER 25 MULTIVERSAL!"
  }

  fun maxAllStars() {
    progressionStore?.setAllStars(10)
    _statusBanner.value = "All systems maxed to 10★ Omniverse Nova!"
  }

  fun grantEssenceBonus(amount: Long = 50_000L) {
    viewModelScope.launch {
      repository.adjustOperatorEssence(currentUserId, amount)
      _statusBanner.value = "+$amount Blue Essence granted!"
    }
  }

  fun importBo3ModToolsCuratedPack() {
    viewModelScope.launch {
      val preview = com.example.export.Bo3ModToolsImporter.getCuratedBo3ModPack(currentUserId)
      val res = repository.importSteamWorkshopPreview(
        userId = currentUserId,
        preview = preview,
        importModulePreset = true,
        importRigPreset = true,
      )
      _statusBanner.value = if (res.isSuccess) {
        "BO3 Mod Tools Pack Imported: Juggernog, Speed Cola, Double Tap, Widow's Wine & Wonder Weapons active!"
      } else {
        res.exceptionOrNull()?.localizedMessage ?: "BO3 import failed"
      }
    }
  }

  fun importBo3TextScript(rawText: String) {
    val parseRes = com.example.export.Bo3ModToolsImporter.parseBo3PackageOrScript(rawText, currentUserId)
    if (parseRes.isFailure) {
      _statusBanner.value = parseRes.exceptionOrNull()?.localizedMessage ?: "Failed to parse BO3 input"
      return
    }
    val preview = parseRes.getOrThrow()
    viewModelScope.launch {
      val res = repository.importSteamWorkshopPreview(
        userId = currentUserId,
        preview = preview,
        importModulePreset = true,
        importRigPreset = true,
      )
      _statusBanner.value = if (res.isSuccess) {
        "BO3 Mod Tools imported '${preview.title}' accurately!"
      } else {
        res.exceptionOrNull()?.localizedMessage ?: "Import failed"
      }
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

  fun updateWonderWeaponCustomization(transform: (com.example.data.model.WonderWeaponCustomization) -> com.example.data.model.WonderWeaponCustomization) {
    progressionStore?.updateWonderWeaponCustomization(transform)
  }

  fun updateCharacterCustomization(transform: (CharacterCustomization) -> CharacterCustomization) {
    progressionStore?.updateCharacterCustomization(transform)
  }

  fun updateLevelCustomization(transform: (LevelCustomization) -> LevelCustomization) {
    progressionStore?.updateLevelCustomization(transform)
  }

  fun updateLobbyConfig(transform: (LobbyConfig) -> LobbyConfig) {
    progressionStore?.updateLobbyConfig(transform)
  }

  fun updatePrivateMatchRules(transform: (PrivateMatchRules) -> PrivateMatchRules) {
    progressionStore?.updatePrivateMatchRules(transform)
  }

  fun updateCollectionCustomMode(transform: (CollectionCustomModeConfig) -> CollectionCustomModeConfig) {
    val store = progressionStore ?: return
    val prevEnabled = store.current.collectionCustomMode.enabled
    store.updateCollectionCustomMode(transform)
    val nextCfg = store.current.collectionCustomMode
    if (prevEnabled != nextCfg.enabled) {
      _statusBanner.value = if (nextCfg.enabled) {
        "Switched to CUSTOM MODE (${nextCfg.scope.label} • ${nextCfg.impactLabel})"
      } else {
        "Switched to NORMAL MODE (Custom Modifiers OFF • Collections Untouched)"
      }
    }
  }

  fun switchNormalOrCustomMode(enableCustomMode: Boolean) {
    val store = progressionStore ?: return
    store.updateCollectionCustomMode { cfg ->
      cfg.copy(enabled = enableCustomMode)
    }
    val nextState = progressionState.value
    val nextCfg = nextState.collectionCustomMode
    _statusBanner.value = if (!nextCfg.enabled) {
      "Normal Mode Active • Standard Collection Rules (${nextState.activeAreaSpec().name})"
    } else {
      val areaInScope = nextCfg.isAreaInScope(nextState.activeAreaId, nextState.customGameMaps)
      if (areaInScope) {
        "Custom Mode Active (${nextCfg.impactLabel}) on ${nextState.activeAreaSpec().name}"
      } else {
        "Custom Mode Enabled (${nextCfg.scope.label}) • Select or Deploy a scoped BO3/Steam/Collection map"
      }
    }
  }

  fun deployCollectionCustomMode() {
    val store = progressionStore ?: return
    val currentState = progressionState.value
    val currentCfg = currentState.collectionCustomMode
    val scoped = CollectionCustomModeConfig.scopedAreas(currentCfg.scope, currentState.customGameMaps)
    val safeArea = scoped.firstOrNull { it.id == currentCfg.targetAreaId } ?: scoped.firstOrNull()
    val areaId = safeArea?.id ?: currentState.activeAreaId
    store.updateCollectionCustomMode { cfg ->
      cfg.copy(enabled = true, targetAreaId = areaId)
    }
    val curJoined = progressionState.value.joinedMatch
    if (curJoined != null && curJoined.areaId != areaId) {
      store.leaveJoinedMatch()
    }
    store.selectArea(areaId)
    val areaSpec = progressionState.value.resolveAreaSpec(areaId)
    val linkedWorld = UniverseCatalog.worldById(areaSpec.linkedWorldId)
    val resolvedMode = progressionState.value.defaultModeForArea(areaId, null)
    val activeCfg = progressionState.value.collectionCustomMode
    _optimisticSelection.value = Pair(linkedWorld.id, resolvedMode)
    _statusBanner.value = "Custom Mode (${activeCfg.scope.label}): ${areaSpec.name} • ${activeCfg.impactLabel}"
    _activeTab.value = OsWorkspaceTab.ARENA_PLAY
    viewModelScope.launch {
      repository.updateProfileSelection(
        userId = currentUserId,
        activeUniverseId = linkedWorld.id,
        activeMode = resolvedMode,
      )
    }
  }

  fun createCustomGameMap(
    title: String,
    mapCode: String,
    category: CustomMapGameCategory,
    description: String,
    zones: List<String>,
    wonderWeapon: String,
    accentHex: Long,
    deployNow: Boolean = true,
  ) {
    val store = progressionStore ?: return
    val slug = mapCode.lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').ifEmpty {
      title.lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').ifEmpty { "custom" }
    }
    val prefix = if (category == CustomMapGameCategory.BO3) "area_bo3_custom_" else "area_col_custom_"
    val mapId = "${prefix}${slug.take(24)}_${java.util.UUID.randomUUID().toString().take(8)}"
    val spec = store.addOrUpdateCustomGameMap(
      UserCustomGameMapSpec(
        id = mapId,
        mapCode = mapCode.ifBlank { if (category == CustomMapGameCategory.BO3) "zm_custom_${slug.take(16)}" else "${category.id}_${slug.take(16)}" },
        title = title,
        gameCategory = category,
        description = description,
        buildingNames = zones,
        wonderWeapon = wonderWeapon,
        accentHex = accentHex,
        isUserCreated = true,
        createdAtMillis = System.currentTimeMillis(),
      ),
    )
    if (deployNow) {
      deployToAreaAndPlay(spec.id)
    } else {
      _statusBanner.value = "Saved Custom Map: ${spec.title} (${spec.mapCode})"
    }
  }

  fun deleteCustomGameMap(mapId: String) {
    progressionStore?.deleteCustomGameMap(mapId)
    _statusBanner.value = "Removed custom map."
  }

  fun createCustomWeapon(
    name: String,
    weaponCode: String,
    category: CustomMapGameCategory,
    description: String,
    damageTypeId: String,
    baseDamage: Int,
    baseCadenceTicks: Float,
    baseSpreadCount: Int,
    basePierce: Int,
    tracerHex: Long,
    equipNow: Boolean = true,
  ) {
    val store = progressionStore ?: return
    val slug = weaponCode.lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').ifEmpty {
      name.lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').ifEmpty { "custom" }
    }
    val wpnId = "wpn_${category.id}_${slug.take(18)}_${java.util.UUID.randomUUID().toString().take(8)}"
    val spec = store.addOrUpdateCustomWeapon(
      UserCustomWeaponSpec(
        id = wpnId,
        weaponCode = weaponCode.ifBlank { "${category.id}_${slug.take(14)}" },
        name = name,
        gameCategory = category,
        description = description,
        damageTypeId = damageTypeId,
        baseDamage = baseDamage,
        baseCadenceTicks = baseCadenceTicks,
        baseProjectileSpeed = 0.026f,
        baseSpreadCount = baseSpreadCount,
        basePierce = basePierce,
        defaultTracerHex = tracerHex,
        isUserCreated = true,
        createdAtMillis = System.currentTimeMillis(),
      )
    )
    if (equipNow) {
      store.updateWeaponCustomization { wc ->
        val nextMulti = (listOf(spec.id) + wc.equippedWeaponIds).distinct().take(3)
        wc.copy(
          equippedWeaponId = spec.id,
          equippedWeaponIds = nextMulti,
          tracerColorHex = spec.defaultTracerHex,
        )
      }
      _statusBanner.value = "Forged & Equipped Custom Weapon: ${spec.name} (${spec.damageTypeId.uppercase()})"
    } else {
      _statusBanner.value = "Saved Custom Weapon: ${spec.name}"
    }
  }

  fun deleteCustomWeapon(weaponId: String) {
    progressionStore?.deleteCustomWeapon(weaponId)
    _statusBanner.value = "Removed custom weapon."
  }

  fun hostPrivateMatch(rules: PrivateMatchRules, hostCallsign: String): MultiverseMatchListing? {
    val store = progressionStore ?: return null
    val match = store.hostPrivateMatch(rules, hostCallsign)
    if (match.areaId.isNotBlank()) store.selectArea(match.areaId)
    _optimisticSelection.value = Pair(match.worldId, match.modeId)
    viewModelScope.launch {
      repository.updateProfileSelection(
        userId = currentUserId,
        activeUniverseId = match.worldId,
        activeMode = match.modeId,
      )
    }
    _statusBanner.value = "Hosted Private Match: ${match.roomName} (Code: ${match.roomCode})"
    return match
  }

  fun joinMatch(match: MultiverseMatchListing, enteredPasscode: String? = null): Boolean {
    val store = progressionStore ?: return false
    val res = store.joinMatch(match, enteredPasscode)
    return if (res.isSuccess) {
      if (match.areaId.isNotBlank()) store.selectArea(match.areaId)
      _optimisticSelection.value = Pair(match.worldId, match.modeId)
      viewModelScope.launch {
        repository.updateProfileSelection(
          userId = currentUserId,
          activeUniverseId = match.worldId,
          activeMode = match.modeId,
        )
      }
      _statusBanner.value = "Joined Match: ${match.roomName} [${match.modeLabel}]"
      true
    } else {
      _statusBanner.value = res.exceptionOrNull()?.message ?: "Failed to join match"
      false
    }
  }

  fun joinMatchById(matchId: String): Boolean {
    val target = progressionState.value.joinedMatch?.takeIf { it.id == matchId }
      ?: SocialCatalog.sampleMatches.firstOrNull { it.id == matchId }
    if (target == null) {
      _statusBanner.value = "Match session [$matchId] is no longer active"
      return false
    }
    return joinMatch(target, target.passcode)
  }

  fun leaveJoinedMatch() {
    progressionStore?.leaveJoinedMatch()
    _statusBanner.value = "Left Match Session"
  }

  fun addFriend(callsign: String): Boolean {
    val store = progressionStore ?: return false
    val res = store.addFriend(callsign)
    return if (res.isSuccess) {
      _statusBanner.value = "Added $callsign to Friends List!"
      true
    } else {
      _statusBanner.value = res.exceptionOrNull()?.message ?: "Failed to add friend"
      false
    }
  }

  fun removeFriend(friendId: String) {
    progressionStore?.removeFriend(friendId)
    _statusBanner.value = "Removed from Friends"
  }

  fun toggleFriendFavorite(friendId: String) {
    progressionStore?.toggleFriendFavorite(friendId)
  }

  fun sendChatMessage(
    channel: ChatChannel,
    content: String,
    senderCallsign: String,
    targetFriendId: String? = null,
    isTactical: Boolean = false,
    safetyFilterEnabled: Boolean = true,
  ) {
    progressionStore?.sendChatMessage(
      channel = channel,
      content = content,
      senderCallsign = senderCallsign,
      targetFriendId = targetFriendId,
      isTactical = isTactical,
      safetyFilterEnabled = safetyFilterEnabled,
    )
  }

  fun createClan(name: String, tag: String, motto: String, colorHex: Long, creatorCallsign: String): Boolean {
    val store = progressionStore ?: return false
    val res = store.createClan(name, tag, motto, colorHex, creatorCallsign)
    return if (res.isSuccess) {
      _statusBanner.value = "Created Clan [$tag] $name!"
      true
    } else {
      _statusBanner.value = res.exceptionOrNull()?.message ?: "Failed to create clan"
      false
    }
  }

  fun joinClan(clan: ClanData, memberCallsign: String) {
    progressionStore?.joinClan(clan, memberCallsign)
    _statusBanner.value = "Joined Clan [${clan.tag}] ${clan.name}!"
  }

  fun donateToClan(essenceAmount: Long, memberCallsign: String, totalBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.donateToClan(essenceAmount, memberCallsign, totalBlueEssence), "Could not donate to clan") { amount ->
      "Contributed $amount Blue Essence to Clan [${store.current.clan.tag}]!"
    }
  }

  fun leaveClan(memberCallsign: String) {
    progressionStore?.leaveClan(memberCallsign)
    _statusBanner.value = "Reset to Default Clan"
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
    runEssenceReward(store.claimPassTier(version, tier), "Cannot claim tier") { rewardEssence ->
      "Claimed Tier $tier (+$rewardEssence Blue Essence)"
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
      _statusBanner.value = "Challenge Complete! +$essenceGain Blue Essence • +$playerXpGain XP • +$bpXpGain BP XP"
    }
  }

  fun refreshCompletedChallenges() {
    progressionStore?.resetCompletedChallengesForReplay()
    _statusBanner.value = "Completed challenges reset for replay!"
  }

  fun upgradeEquipment(slotId: String) {
    progressionStore?.upgradeEquipment(slotId)
    _statusBanner.value = "Upgraded Equipment in $slotId slot!"
  }

  fun upgradeEquipmentTier(slotId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeEquipmentTier(slotId, currentBlueEssence), "Cannot upgrade Equipment Tier") {
      val nextTier = progressionState.value.equipmentState(slotId).tier
      "Equipment Tier ${ProgressionCatalog.tierMeta(nextTier).label} (T$nextTier/25)!"
    }
  }

  fun prestigeEquipment(slotId: String) {
    val store = progressionStore ?: return
    runEssenceReward(store.prestigeEquipment(slotId), "Cannot prestige equipment") { reward ->
      "Equipment Prestiged! (+$reward Blue Essence)"
    }
  }

  fun equipEquipment(slotId: String, itemId: String) {
    progressionStore?.equipEquipment(slotId, itemId)
    _statusBanner.value = "Equipped $itemId in $slotId slot!"
  }

  fun upgradeGear(slot: GearSlot) {
    progressionStore?.upgradeGear(slot)
    _statusBanner.value = "Upgraded ${slot.displayName}!"
  }

  fun upgradeGearTier(slot: GearSlot, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeGearTier(slot, currentBlueEssence), "Cannot upgrade Gear Tier") {
      val nextTier = progressionState.value.gearMap[slot.id]?.tier ?: 1
      "${slot.displayName} Tier ${ProgressionCatalog.tierMeta(nextTier).label} (T$nextTier/25)!"
    }
  }

  fun prestigeGear() {
    progressionStore?.prestigeGear()
    _statusBanner.value = "All Gear Prestiged!"
  }

  fun unlockSkill(nodeId: String, currentEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.unlockSkillNode(nodeId, currentEssence), "Cannot upgrade skill") { cost ->
      val tier = progressionState.value.skillNodes[nodeId]?.tier ?: 1
      "Skill Tier $tier/25 Active! (-$cost Blue Essence)"
    }
  }

  fun upgradeWeaponTier(weaponId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeWeaponTier(weaponId, currentBlueEssence), "Cannot upgrade Weapon Tier") {
      val nextTier = progressionState.value.weaponTier(weaponId)
      "${ProgressionCatalog.weaponById(weaponId, progressionState.value.customWeapons).name} Tier $nextTier/25 (${ProgressionCatalog.tierMeta(nextTier).label})!"
    }
  }

  fun upgradeWonderTier(wonderId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeWonderTier(wonderId, currentBlueEssence), "Cannot upgrade Wonder Tier") {
      val nextTier = progressionState.value.wonderTier(wonderId)
      "Wonder Weapon upgraded to Tier $nextTier/25 (${ProgressionCatalog.tierMeta(nextTier).label})!"
    }
  }

  fun prestigePlayer() {
    val store = progressionStore ?: return
    runEssenceReward(store.prestigePlayer(), "Cannot prestige operator") { reward ->
      "Operator Prestiged to P${progressionState.value.playerPrestige}! (+$reward Blue Essence)"
    }
  }

  fun prestigeWeapon(weaponId: String) {
    val store = progressionStore ?: return
    runEssenceReward(store.prestigeWeapon(weaponId), "Cannot prestige weapon") { reward ->
      "Weapon Prestiged to P${progressionState.value.weaponPrestige(weaponId)}! (+$reward Blue Essence)"
    }
  }

  fun claimAchievement(achievementId: String) {
    val store = progressionStore ?: return
    val res = store.claimAchievement(achievementId)
    if (res.isFailure) {
      _statusBanner.value = res.exceptionOrNull()?.message ?: "Cannot claim achievement"
      return
    }
    val (blueEssenceReward, xpReward) = res.getOrThrow()
    viewModelScope.launch {
      if (blueEssenceReward > 0L) repository.adjustOperatorEssence(currentUserId, blueEssenceReward)
      _statusBanner.value = "Achievement Unlocked! +$blueEssenceReward Blue Essence • +$xpReward XP"
    }
  }

  fun upgradeCharacterTier(currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeCharacterTier(currentBlueEssence), "Cannot upgrade Character Tier") {
      val nextTier = progressionState.value.characterTier
      "Operator Character Tier $nextTier/25 (${ProgressionCatalog.tierMeta(nextTier).label})!"
    }
  }

  fun upgradeCharacterStar(currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeCharacterStar(currentBlueEssence), "Cannot upgrade Character Star") {
      val stars = progressionState.value.characterStarLevel
      "Operator Character ${StarRankMeta.forStars(stars).label} (${stars}★/10★)!"
    }
  }

  fun upgradeOutfitStar(outfitId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeOutfitStar(outfitId, currentBlueEssence), "Cannot upgrade Outfit Star") {
      val stars = progressionState.value.outfitState(outfitId).starLevel
      val name = MultiverseArsenalCatalog.outfitById(outfitId).nameWithVersion
      "$name elevated to $stars★/10★ (${StarRankMeta.forStars(stars).label})!"
    }
  }

  fun upgradeArmoryItemStar(itemId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeArmoryItemStar(itemId, currentBlueEssence), "Cannot upgrade Mod Star") {
      val stars = progressionState.value.armoryItemStar(itemId)
      "Armory Mod elevated to $stars★/10★ (${StarRankMeta.forStars(stars).label})!"
    }
  }

  fun upgradeWeaponStar(weaponId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeWeaponStar(weaponId, currentBlueEssence), "Cannot upgrade Weapon Star") {
      val stars = progressionState.value.weaponStar(weaponId)
      "${ProgressionCatalog.weaponById(weaponId, progressionState.value.customWeapons).name} elevated to $stars★/10★!"
    }
  }

  fun upgradeWonderStar(wonderId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeWonderStar(wonderId, currentBlueEssence), "Cannot upgrade Wonder Star") {
      val stars = progressionState.value.wonderStar(wonderId)
      "Wonder Weapon elevated to $stars★/10★!"
    }
  }

  fun upgradeGearStar(slot: GearSlot, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeGearStar(slot, currentBlueEssence), "Cannot upgrade Gear Star") {
      val stars = progressionState.value.gearMap[slot.id]?.starLevel ?: 1
      "${slot.displayName} elevated to $stars★/10★!"
    }
  }

  fun upgradeEquipmentStar(slotId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeEquipmentStar(slotId, currentBlueEssence), "Cannot upgrade Equipment Star") {
      val stars = progressionState.value.equipmentState(slotId).starLevel
      "Equipment elevated to $stars★/10★!"
    }
  }

  fun upgradeSkillStar(nodeId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeSkillStar(nodeId, currentBlueEssence), "Cannot upgrade Skill Star") {
      val stars = progressionState.value.skillNodes[nodeId]?.starLevel ?: 1
      "Skill Node elevated to $stars★/10★!"
    }
  }

  fun upgradeVacuumBotStar(currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeVacuumBotStar(currentBlueEssence), "Cannot upgrade Vacuum Bot Star") {
      val stars = progressionState.value.vacuumBot.starLevel
      "Vacuum Bot elevated to $stars★/10★!"
    }
  }

  fun upgradeAreaStar(areaId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeAreaStar(areaId, currentBlueEssence), "Cannot upgrade Area Star") {
      val stars = progressionState.value.areaState(areaId).starLevel
      "${progressionState.value.resolveAreaSpec(areaId).name} elevated to $stars★/${StarRankMeta.MAX_STARS}★!"
    }
  }

  fun upgradeModeTier(modeId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeModeTier(modeId, currentBlueEssence), "Cannot upgrade Mode Tier") {
      val nextTier = progressionState.value.modeTier(modeId)
      val modeLabel = UniverseCatalog.modeById(modeId).label
      "$modeLabel Mode Tier $nextTier/${PowerTier.MAX_TIER} (${ProgressionCatalog.tierMeta(nextTier).label})!"
    }
  }

  fun upgradeModeStar(modeId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeModeStar(modeId, currentBlueEssence), "Cannot upgrade Mode Star") {
      val stars = progressionState.value.modeStar(modeId)
      val modeLabel = UniverseCatalog.modeById(modeId).label
      "$modeLabel Mode elevated to $stars★/${StarRankMeta.MAX_STARS}★ (${StarRankMeta.forStars(stars).label})!"
    }
  }

  fun claimMission(missionId: String) {
    val store = progressionStore ?: return
    val res = store.claimMission(missionId)
    if (res.isFailure) {
      _statusBanner.value = res.exceptionOrNull()?.message ?: "Cannot claim mission"
      return
    }
    val (blueEssenceReward, xpReward, shards) = res.getOrThrow()
    viewModelScope.launch {
      if (blueEssenceReward > 0L) repository.adjustOperatorEssence(currentUserId, blueEssenceReward)
      _statusBanner.value = "Mission Complete! +$blueEssenceReward Blue Essence • +$xpReward XP • +$shards Shards"
    }
  }

  fun selectNewGamePlusTier(tier: Int) {
    val store = progressionStore ?: return
    val res = store.selectNewGamePlusTier(tier)
    _statusBanner.value = if (res.isSuccess) {
      val ver = res.getOrThrow()
      "${ver.label} Active! (Reward Boost x${String.format(java.util.Locale.US, "%.2f", store.current.ngPlusRewardMultiplier)})"
    } else {
      res.exceptionOrNull()?.message ?: "New Game+ tier locked"
    }
  }

  fun completeNewGamePlusRun() {
    val store = progressionStore ?: return
    val nextUnlocked = store.completeNewGamePlusRun()
    _statusBanner.value = "Timeline Completed (${store.current.ngPlusCompletions}x)! Unlocked ${nextUnlocked.label} • Rewards x${String.format(java.util.Locale.US, "%.2f", store.current.ngPlusRewardMultiplier)}"
  }

  fun selectWorldCollectionDifficulty(worldId: String, difficulty: CollectionDifficulty) {
    val store = progressionStore ?: return
    store.setWorldCollectionDifficulty(worldId, difficulty)
    val world = UniverseCatalog.worldById(worldId)
    _statusBanner.value = "${world.title.substringBefore(":")} Difficulty: ${difficulty.label} (x${String.format(java.util.Locale.US, "%.2f", difficulty.rewardMultiplier)} Rewards)"
  }

  fun deployMissionTarget(scope: MissionScope, targetId: String) {
    when (scope) {
      MissionScope.WORLD -> {
        val world = UniverseCatalog.worldById(targetId)
        val primaryAreaId = UniverseCatalog.primaryAreaIdForWorld(world.id)
        val curArea = progressionState.value.resolveAreaSpec(progressionState.value.activeAreaId)
        val shouldReset = curArea.linkedWorldId != world.id
        if (shouldReset) {
          progressionStore?.selectArea(primaryAreaId)
        }
        val resolvedMode = world.defaultModeId
        _optimisticSelection.value = Pair(world.id, resolvedMode)
        _statusBanner.value = "Deploying to ${world.title} (${resolvedMode.uppercase()}) for World Missions"
        _activeTab.value = OsWorkspaceTab.ARENA_PLAY
        viewModelScope.launch {
          repository.updateProfileSelection(
            userId = currentUserId,
            activeUniverseId = world.id,
            activeMode = resolvedMode,
          )
        }
      }
      MissionScope.ARENA -> {
        val store = progressionStore
        store?.selectArea(targetId)
        val area = progressionState.value.resolveAreaSpec(targetId)
        val linkedWorld = UniverseCatalog.worldById(area.linkedWorldId)
        val targetMode = progressionState.value.defaultModeForArea(targetId, null)
        _optimisticSelection.value = Pair(linkedWorld.id, targetMode)
        _statusBanner.value = "Deploying to ${area.name} (${linkedWorld.title}) for Arena Missions"
        _activeTab.value = OsWorkspaceTab.ARENA_PLAY
        viewModelScope.launch {
          repository.updateProfileSelection(
            userId = currentUserId,
            activeUniverseId = linkedWorld.id,
            activeMode = targetMode,
          )
        }
      }
    }
  }

  fun purchaseStoreItem(itemId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    val res = store.purchaseStoreItem(itemId, currentBlueEssence)
    if (res.isFailure) {
      _statusBanner.value = res.exceptionOrNull()?.message ?: "Store purchase failed"
      return
    }
    val purchase = res.getOrThrow()
    viewModelScope.launch {
      if (purchase.costBlueEssence > 0L) {
        repository.adjustOperatorEssence(currentUserId, -purchase.costBlueEssence)
      }
      _statusBanner.value = "Store: ${purchase.summary} (-${purchase.costBlueEssence} Blue Essence)"
    }
  }

  fun recordArenaProgression(report: MatchProgressionReport) {
    val store = progressionStore ?: return
    val (xpGained, playerLvlUps, wpnLvlUps) = store.recordMatchProgression(report)
    val notes = mutableListOf("+$xpGained XP")
    if (playerLvlUps > 0L) notes.add("PLAYER LV UP (+$playerLvlUps)")
    if (wpnLvlUps > 0L) notes.add("WEAPON LV UP (+$wpnLvlUps)")
    if (report.blastShardsEarned > 0L) notes.add("+${report.blastShardsEarned} Shards")
    _statusBanner.value = notes.joinToString(" • ")
  }

  fun toggleMultiWeaponEquip(weaponId: String) {
    val list = progressionStore?.toggleMultiWeaponEquip(weaponId) ?: return
    val custom = progressionState.value.customWeapons
    _statusBanner.value = "Equipped Weapons (${list.size}/3): ${list.joinToString { ProgressionCatalog.weaponById(it, custom).name }}"
  }

  fun toggleMultiWonderEquip(wonderId: String) {
    val list = progressionStore?.toggleMultiWonderEquip(wonderId) ?: return
    _statusBanner.value = "Equipped Wonder Loadout (${list.size}/3)"
  }

  fun toggleMultiEquipmentEquip(slotType: EquipmentSlotType, itemId: String) {
    val list = progressionStore?.toggleMultiEquipmentEquip(slotType, itemId) ?: return
    val name = ProgressionCatalog.resolveEquipmentItem(itemId, progressionState.value.customEquipment).name
    _statusBanner.value = "${slotType.displayName} Loadout (${list.size}/2) updated: $name"
  }

  fun equipOutfit(outfitId: String) {
    val res = progressionStore?.equipOutfit(outfitId, syncSignatureEquipment = true) ?: return
    val spec = MultiverseArsenalCatalog.outfitById(outfitId)
    val originSuffix = if (spec.isOriginalCharacter) " • Original Hero +25% Buff" else ""
    _statusBanner.value = "Equipped ${res.nameWithVersion} (Lv.${res.progress.level} T${res.progress.tier}/25)$originSuffix"
  }

  fun upgradeOutfit(outfitId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeOutfit(outfitId, currentBlueEssence), "Outfit upgrade failed") {
      val st = progressionState.value.outfitState(outfitId)
      val name = MultiverseArsenalCatalog.outfitById(outfitId).nameWithVersion
      "$name -> Lv.${st.level} Tier ${st.tier}/25!"
    }
  }

  fun upgradeOutfitSubSystem(outfitId: String, part: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeOutfitSubSystem(outfitId, part, currentBlueEssence), "Upgrade failed") {
      val st = progressionState.value.outfitState(outfitId)
      val rank = when (part) {
        "skill" -> st.skillRank
        "gear" -> st.gearRank
        else -> st.equipmentRank
      }
      "Outfit $part upgraded to Rank $rank/20!"
    }
  }

  fun upgradeOutfitRarity(outfitId: String) {
    val store = progressionStore ?: return
    val res = store.upgradeOutfitRarityWithShards(outfitId)
    val name = MultiverseArsenalCatalog.outfitById(outfitId).nameWithVersion
    _statusBanner.value = if (res.isSuccess) {
      "$name ascended to ${res.getOrThrow().label.uppercase()} Rarity!"
    } else {
      res.exceptionOrNull()?.message ?: "Need more Character Shards"
    }
  }

  fun recruitCharacterShard(currentBlueEssence: Long, targetOutfitId: String? = null) {
    val store = progressionStore ?: return
    val res = store.recruitCharacterShardDrop(currentBlueEssence, targetOutfitId)
    if (res.isFailure) {
      _statusBanner.value = res.exceptionOrNull()?.message ?: "Cannot recruit Character Shards"
      return
    }
    val (name, shards, cost) = res.getOrThrow()
    viewModelScope.launch {
      if (cost > 0L) repository.adjustOperatorEssence(currentUserId, -cost)
      _statusBanner.value = "Character Beacon Loot: +$shards Shards for $name (-$cost B)"
    }
  }

  fun selectCustomArea(areaId: String) {
    val store = progressionStore ?: return
    val curJoined = progressionState.value.joinedMatch
    if (curJoined != null && curJoined.areaId != areaId) {
      store.leaveJoinedMatch()
    }
    store.selectArea(areaId)
    val spec = progressionState.value.resolveAreaSpec(areaId)
    val linkedWorld = UniverseCatalog.worldById(spec.linkedWorldId)
    val resolvedMode = progressionState.value.defaultModeForArea(areaId, null)
    _optimisticSelection.value = Pair(linkedWorld.id, resolvedMode)
    val st = progressionState.value.areaState(areaId)
    _statusBanner.value = "Area Selected: ${spec.name} • ${linkedWorld.title} [${UniverseCatalog.modeById(resolvedMode).shortTag}] (${st.ageYears} yrs • ${st.currentEra.label})"
    viewModelScope.launch {
      repository.updateProfileSelection(
        userId = currentUserId,
        activeUniverseId = linkedWorld.id,
        activeMode = resolvedMode,
      )
    }
  }

  fun deployToAreaAndPlay(areaId: String) {
    val store = progressionStore ?: return
    val curJoined = progressionState.value.joinedMatch
    if (curJoined != null && curJoined.areaId != areaId) {
      store.leaveJoinedMatch()
    }
    store.selectArea(areaId)
    val spec = progressionState.value.resolveAreaSpec(areaId)
    val linkedWorld = UniverseCatalog.worldById(spec.linkedWorldId)
    val resolvedMode = progressionState.value.defaultModeForArea(areaId, null)
    _optimisticSelection.value = Pair(linkedWorld.id, resolvedMode)
    _statusBanner.value = "Deployed: ${spec.name} • ${linkedWorld.title} [${UniverseCatalog.modeById(resolvedMode).shortTag}]"
    _activeTab.value = OsWorkspaceTab.ARENA_PLAY
    viewModelScope.launch {
      repository.updateProfileSelection(
        userId = currentUserId,
        activeUniverseId = linkedWorld.id,
        activeMode = resolvedMode,
      )
    }
  }

  fun upgradeAreaFeature(areaId: String, feature: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeAreaFeature(areaId, feature, currentBlueEssence), "Area upgrade failed") { cost ->
      "Area $feature upgraded! (-$cost Blue Essence)"
    }
  }

  fun advanceAreaAge(areaId: String, yearsDelta: Int = 18) {
    val store = progressionStore ?: return
    val prevCycles = progressionState.value.areaState(areaId).rebirthCycles
    val next = store.advanceAreaAge(areaId, yearsDelta)
    val spec = progressionState.value.resolveAreaSpec(areaId)
    _statusBanner.value = if (next.rebirthCycles > prevCycles) {
      "${spec.name} REJUVENATED to Young Era (${next.ageYears} yrs • Rebirth Cycle #${next.rebirthCycles})!"
    } else {
      "${spec.name} aged to ${next.ageYears} yrs (${next.currentEra.label})"
    }
  }

  fun spendBlastShardsUpgrade() {
    val store = progressionStore ?: return
    val res = store.spendBlastShardsUpgrade(10L)
    _statusBanner.value = if (res.isSuccess) {
      "10 Blast Shards channeled: +60 Red/Yellow/Green Essence & Vacuum Bot Lv.${progressionState.value.vacuumBot.level}!"
    } else {
      res.exceptionOrNull()?.message ?: "Need 10 Blast Shards"
    }
  }

  fun upgradePerkDrink(perkId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradePerkDrink(perkId, currentBlueEssence), "Perk upgrade failed") {
      val rank = progressionState.value.perkRank(perkId)
      val name = MultiverseArsenalCatalog.perkDrinks.firstOrNull { it.id == perkId }?.name ?: perkId
      "$name upgraded to Rank $rank/25!"
    }
  }

  fun upgradeBlastCore(color: BlastCoreColor) {
    val store = progressionStore ?: return
    val res = store.upgradeBlastCore(color)
    _statusBanner.value = if (res.isSuccess) {
      "${color.displayName} Level ${res.getOrThrow()}/20 Activated!"
    } else {
      res.exceptionOrNull()?.message ?: "Blast Core upgrade failed"
    }
  }

  fun upgradePerkStar(perkId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradePerkStar(perkId, currentBlueEssence), "Perk star upgrade failed") {
      val star = progressionState.value.perkStar(perkId)
      val name = MultiverseArsenalCatalog.perkDrinks.firstOrNull { it.id == perkId }?.name ?: perkId
      "$name Star Level upgraded to $star/10★!"
    }
  }

  fun upgradeAllPerks(currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeAllPerks(currentBlueEssence), "All-Perk upgrade failed") { cost ->
      "All Perks upgraded! (-$cost Blue Essence)"
    }
  }

  fun upgradeProtection(protId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeProtection(protId, currentBlueEssence), "Protection upgrade failed") {
      val rank = progressionState.value.protectionRank(protId)
      val spec = ProgressionCatalog.protectionUpgrades.firstOrNull { it.id == protId }
      "${spec?.title ?: protId} upgraded to Rank $rank/25!"
    }
  }

  fun upgradeProtectionStar(protId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeProtectionStar(protId, currentBlueEssence), "Protection star upgrade failed") {
      val star = progressionState.value.protectionStar(protId)
      val spec = ProgressionCatalog.protectionUpgrades.firstOrNull { it.id == protId }
      "${spec?.title ?: protId} Star Level upgraded to $star/10★!"
    }
  }

  fun upgradeBlastShardTier(currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeBlastShardTier(currentBlueEssence), "Blast Shard tier upgrade failed") {
      val tier = progressionState.value.blastShardTier
      "Blast Shard Channeling Tier upgraded to T$tier/25!"
    }
  }

  fun upgradeCompanionBuffTier(currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeCompanionBuffTier(currentBlueEssence), "Companion buff tier upgrade failed") {
      val tier = progressionState.value.companionBuffTier
      "Companion Squad Buff Tier upgraded to T$tier/25!"
    }
  }

  fun upgradeBlastCoreWithEssence(color: BlastCoreColor, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeBlastCoreWithEssence(color, currentBlueEssence), "Blast Core essence upgrade failed") {
      val lvl = progressionState.value.blastCoreLevel(color)
      "${color.displayName} Core upgraded to Level $lvl/20 using Blue Essence!"
    }
  }

  fun addOrUpdateCustomPerk(spec: UserCustomPerkSpec) {
    progressionStore?.addOrUpdateCustomPerk(spec)
    _statusBanner.value = "Custom Perk '${spec.name}' saved!"
  }

  fun toggleEquipCustomPerk(perkId: String) {
    progressionStore?.toggleEquipCustomPerk(perkId)
    val equipped = progressionState.value.equippedCustomPerkIds.contains(perkId)
    _statusBanner.value = "Custom Perk ${if (equipped) "equipped" else "unequipped"}!"
  }

  fun upgradeCustomPerkTier(perkId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeCustomPerkTier(perkId, currentBlueEssence), "Custom perk tier upgrade failed") {
      val perk = progressionState.value.customPerks.firstOrNull { it.id == perkId }
      "${perk?.name ?: "Custom Perk"} upgraded to Tier ${perk?.tier ?: 1}/25!"
    }
  }

  fun upgradeCustomPerkStar(perkId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeCustomPerkStar(perkId, currentBlueEssence), "Custom perk star upgrade failed") {
      val perk = progressionState.value.customPerks.firstOrNull { it.id == perkId }
      "${perk?.name ?: "Custom Perk"} upgraded to Star ${perk?.starLevel ?: 0}/10★!"
    }
  }

  fun deleteCustomPerk(perkId: String) {
    progressionStore?.deleteCustomPerk(perkId)
    _statusBanner.value = "Custom Perk deleted."
  }

  fun customizeGearSlot(slot: GearSlot, customization: GearSlotCustomization) {
    progressionStore?.customizeGearSlot(slot, customization)
    _statusBanner.value = "${slot.displayName} customization updated!"
  }

  fun addOrUpdateCustomGear(spec: UserCustomGearSpec) {
    progressionStore?.addOrUpdateCustomGear(spec)
    _statusBanner.value = "Custom Gear '${spec.name}' (${spec.slot.displayName}) saved & equipped!"
  }

  fun equipCustomGear(slot: GearSlot, gearId: String) {
    val store = progressionStore ?: return
    val spec = store.state.value.customGear.firstOrNull { it.id == gearId }
    if (spec != null) {
      store.equipCustomGear(slot, gearId)
      _statusBanner.value = "Equipped ${spec.name} in ${slot.displayName}!"
    }
  }

  fun upgradeCustomGearTier(gearId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeCustomGearTier(gearId, currentBlueEssence), "Custom gear tier upgrade failed") {
      val gear = progressionState.value.customGear.firstOrNull { it.id == gearId }
      "${gear?.name ?: "Gear"} upgraded to Tier ${gear?.tier ?: 1}/25!"
    }
  }

  fun upgradeCustomGearStar(gearId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeCustomGearStar(gearId, currentBlueEssence), "Custom gear star upgrade failed") {
      val gear = progressionState.value.customGear.firstOrNull { it.id == gearId }
      "${gear?.name ?: "Gear"} upgraded to Star ${gear?.starLevel ?: 0}/10★!"
    }
  }

  fun deleteCustomGear(gearId: String) {
    progressionStore?.deleteCustomGear(gearId)
    _statusBanner.value = "Custom Gear deleted."
  }

  fun setEquipmentVariant(slotId: String, variantId: String) {
    progressionStore?.setEquipmentVariant(slotId, variantId)
    val variant = ProgressionCatalog.equipmentVariants.firstOrNull { it.first == variantId }
    _statusBanner.value = "$slotId variant set to ${variant?.second ?: variantId}!"
  }

  fun addOrUpdateCustomEquipment(spec: UserCustomEquipmentSpec) {
    progressionStore?.addOrUpdateCustomEquipment(spec)
    _statusBanner.value = "Custom Equipment '${spec.name}' saved & equipped!"
  }

  fun upgradeCustomEquipmentTier(itemId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeCustomEquipmentTier(itemId, currentBlueEssence), "Equipment tier upgrade failed") {
      val eq = progressionState.value.customEquipment.firstOrNull { it.id == itemId }
      "${eq?.name ?: "Equipment"} upgraded to Tier ${eq?.tier ?: 1}/25!"
    }
  }

  fun upgradeCustomEquipmentStar(itemId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeCustomEquipmentStar(itemId, currentBlueEssence), "Equipment star upgrade failed") {
      val eq = progressionState.value.customEquipment.firstOrNull { it.id == itemId }
      "${eq?.name ?: "Equipment"} upgraded to Star ${eq?.starLevel ?: 0}/10★!"
    }
  }

  fun deleteCustomEquipment(itemId: String) {
    progressionStore?.deleteCustomEquipment(itemId)
    _statusBanner.value = "Custom Equipment deleted."
  }

  fun upgradeVacuumBot(currentBlueEssence: Long) {
    val store = progressionStore ?: return
    runEssenceUpgrade(store.upgradeVacuumBot(currentBlueEssence), "Vacuum Bot upgrade failed") { cost ->
      val bot = progressionState.value.vacuumBot
      "Vacuum Bot -> Lv.${bot.level} • Tier ${bot.tier}/25 (-$cost Blue Essence)"
    }
  }

  fun toggleVacuumAutoUpgrade() {
    progressionStore?.toggleVacuumAutoUpgrade()
    val on = progressionState.value.vacuumBot.autoUpgradeEnabled
    _statusBanner.value = "Vacuum Bot Auto-Upgrade ${if (on) "ENABLED" else "PAUSED"}"
  }

  fun executeTradeOffer(offerId: String, currentBlueEssence: Long) {
    val store = progressionStore ?: return
    val res = store.executeTradeOffer(offerId, currentBlueEssence)
    if (res.isFailure) {
      _statusBanner.value = res.exceptionOrNull()?.message ?: "Trade failed"
      return
    }
    val blueDelta = res.getOrThrow()
    val offer = MultiverseArsenalCatalog.tradeOffers.firstOrNull { it.id == offerId }
    viewModelScope.launch {
      if (blueDelta != 0L) repository.adjustOperatorEssence(currentUserId, blueDelta)
      _statusBanner.value = "Traded: ${offer?.title ?: offerId} (+${offer?.outputAmount ?: 0} ${offer?.outputKind?.label ?: ""})"
    }
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
    val prog = progressionState.value
    val passMult = prog.activePassVersion.essenceMultiplier
    val clanMult = prog.clan.essencePerkMultiplier
    val ngAndColMult = prog.combinedGameplayRewardMultiplier(universeId)
    val rules = prog.effectiveMatchRules()
    val mutatorEssenceMult = (if (rules.doubleBlueEssence) 2.0f else 1.0f) * (if (rules.friendlyFire) 1.25f else 1.0f)
    val customEssenceMult = prog.collectionCustomMode.effectiveEssenceMultFor(prog.activeAreaId, prog.customGameMaps)
    val baseEssence = (score / 10L).coerceAtLeast(150L) + if (extracted) 350L else 0L
    val essenceEarned = (baseEssence * passMult * clanMult * mutatorEssenceMult * ngAndColMult * customEssenceMult)
      .toLong()
      .coerceAtLeast(100L)
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
          "Match saved! +$essenceEarned Blue Essence earned$suffix"
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

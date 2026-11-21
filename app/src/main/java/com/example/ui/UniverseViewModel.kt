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
import com.example.data.model.OperatorProfile
import com.example.data.model.OperatorProgressionState
import com.example.data.model.PrivateMatchRules
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.SocialCatalog
import com.example.data.model.StarRankMeta
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

  fun updateLobbyConfig(transform: (LobbyConfig) -> LobbyConfig) {
    progressionStore?.updateLobbyConfig(transform)
  }

  fun updatePrivateMatchRules(transform: (PrivateMatchRules) -> PrivateMatchRules) {
    progressionStore?.updatePrivateMatchRules(transform)
  }

  fun hostPrivateMatch(rules: PrivateMatchRules, hostCallsign: String): MultiverseMatchListing? {
    val store = progressionStore ?: return null
    val match = store.hostPrivateMatch(rules, hostCallsign)
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
      "${ProgressionCatalog.weaponById(weaponId).name} Tier $nextTier/25 (${ProgressionCatalog.tierMeta(nextTier).label})!"
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
      "${ProgressionCatalog.weaponById(weaponId).name} elevated to $stars★/10★!"
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
      "${MultiverseArsenalCatalog.areaById(areaId).name} elevated to $stars★/10★!"
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

  fun deployMissionTarget(scope: MissionScope, targetId: String) {
    when (scope) {
      MissionScope.WORLD -> {
        val world = UniverseCatalog.worldById(targetId)
        viewModelScope.launch {
          repository.updateProfileSelection(
            userId = currentUserId,
            activeUniverseId = world.id,
            activeMode = world.defaultModeId,
          )
          _statusBanner.value = "Deploying to ${world.title} (${world.defaultModeId.uppercase()}) for World Missions"
          _activeTab.value = OsWorkspaceTab.ARENA_PLAY
        }
      }
      MissionScope.ARENA -> {
        val store = progressionStore
        store?.selectArea(targetId)
        val area = MultiverseArsenalCatalog.areaById(targetId)
        viewModelScope.launch {
          if (targetId == "area_campaign_nexus" || targetId == "area_mended_convergence") {
            repository.updateProfileSelection(
              userId = currentUserId,
              activeUniverseId = "campaign_origins",
              activeMode = "campaign",
            )
          }
          _statusBanner.value = "Deploying to ${area.name} for Arena Missions"
          _activeTab.value = OsWorkspaceTab.ARENA_PLAY
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
    _statusBanner.value = "Equipped Weapons (${list.size}/3): ${list.joinToString { ProgressionCatalog.weaponById(it).name }}"
  }

  fun toggleMultiWonderEquip(wonderId: String) {
    val list = progressionStore?.toggleMultiWonderEquip(wonderId) ?: return
    _statusBanner.value = "Equipped Wonder Loadout (${list.size}/3)"
  }

  fun toggleMultiEquipmentEquip(slotType: EquipmentSlotType, itemId: String) {
    val list = progressionStore?.toggleMultiEquipmentEquip(slotType, itemId) ?: return
    val name = ProgressionCatalog.equipmentItems.firstOrNull { it.id == itemId }?.name ?: itemId
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
    store.selectArea(areaId)
    val spec = MultiverseArsenalCatalog.areaById(areaId)
    val st = progressionState.value.areaState(areaId)
    _statusBanner.value = "Area Selected: ${spec.name} (${st.ageYears} yrs • ${st.currentEra.label})"
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
    val spec = MultiverseArsenalCatalog.areaById(areaId)
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
    val rules = prog.effectiveMatchRules()
    val mutatorEssenceMult = (if (rules.doubleBlueEssence) 2.0f else 1.0f) * (if (rules.friendlyFire) 1.25f else 1.0f)
    val baseEssence = (score / 10L).coerceAtLeast(150L) + if (extracted) 350L else 0L
    val essenceEarned = (baseEssence * passMult * clanMult * mutatorEssenceMult).toLong()
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

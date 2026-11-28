package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Locale
import com.example.data.model.AchievementState
import com.example.data.model.BattlePassVersion
import com.example.data.model.BlastCoreColor
import com.example.data.model.BotDifficulty
import com.example.data.model.ChallengeState
import com.example.data.model.CharacterCustomization
import com.example.data.model.CharacterRarity
import com.example.data.model.CollectionCustomModeConfig
import com.example.data.model.CollectionCustomScope
import com.example.data.model.CollectionDifficulty
import com.example.data.model.CustomAreaState
import com.example.data.model.CuratedCustomMapsCatalog
import com.example.data.model.CustomMapGameCategory
import com.example.data.model.EquipmentSlotType
import com.example.data.model.EquipmentState
import com.example.data.model.GearSlot
import com.example.data.model.GearState
import com.example.data.model.LevelCustomization
import com.example.data.model.LobbyConfig
import com.example.data.model.LobbyPartyMode
import com.example.data.model.MatchProgressionReport
import com.example.data.model.MissionScope
import com.example.data.model.MissionSpec
import com.example.data.model.MissionStatKind
import com.example.data.model.MissionState
import com.example.data.model.MissionsCatalog
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.NewGamePlusVersion
import com.example.data.model.OperatorProgressionState
import com.example.data.model.OutfitProgressState
import com.example.data.model.PowerTier
import com.example.data.model.ProgressionCatalog
import com.example.data.model.SkillNode
import com.example.data.model.StarRankMeta
import com.example.data.model.TradeCurrencyKind
import com.example.data.model.VacuumBotState
import com.example.data.model.WeaponCustomization
import com.example.data.model.UserCustomWeaponSpec
import com.example.data.model.WorldRegion
import com.example.data.model.ChatChannel
import com.example.data.model.ChatMessage
import com.example.data.model.ClanData
import com.example.data.model.ClanMember
import com.example.data.model.ClanRank
import com.example.data.model.FriendEntry
import com.example.data.model.FriendOnlineStatus
import com.example.data.model.MatchPrivacy
import com.example.data.model.MultiverseMatchListing
import com.example.data.model.PrivateMatchRules
import com.example.data.model.SocialCatalog
import com.example.data.model.UniverseCatalog
import com.example.data.model.UpgradeCosts
import com.example.data.model.UserCustomEquipmentSpec
import com.example.data.model.UserCustomGameMapSpec
import com.example.data.model.UserCustomGearSpec
import com.example.data.model.UserCustomPerkSpec
import com.example.data.model.GearSlotCustomization
import com.example.data.model.WonderWeaponKind
import com.example.data.model.WorldDifficultyTier
import com.example.data.model.CrystalItem
import com.example.data.model.MagicSystemState
import com.example.data.model.MultiverseSystemsCatalog
import com.example.data.model.PlayerBaseState
import com.example.data.model.defaultCrystals
import com.example.data.model.defaultEnvironmentalResources
import com.example.data.model.defaultSystemShards
import com.example.security.AppSecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class StorePurchaseResult(
  val itemName: String,
  val costBlueEssence: Long,
  val summary: String,
)

class ProgressionStore(
  context: Context,
  userId: String,
) {
  private val appContext: Context = context.applicationContext
  private val safeUserId = userId.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "local_operator" }
  private val prefs: SharedPreferences = appContext.getSharedPreferences(
    "${PREFS_PREFIX}_$safeUserId",
    Context.MODE_PRIVATE,
  )

  var lastLoadedIntegrityValid: Boolean = true
    private set

  private val _state = MutableStateFlow(load())
  val state: StateFlow<OperatorProgressionState> = _state.asStateFlow()

  val current: OperatorProgressionState
    get() = _state.value

  fun verifyStoredIntegrity(): Boolean {
    val raw = prefs.getString(KEY_STATE_JSON, null) ?: return true
    val storedSeal = prefs.getString(KEY_STATE_HASH, null)
    val valid = if (storedSeal == null) true else AppSecurityManager.verifyPayload(raw, storedSeal)
    lastLoadedIntegrityValid = valid
    return valid
  }

  fun updateWeaponCustomization(transform: (WeaponCustomization) -> WeaponCustomization) {
    val updated = transform(current.weaponCustom)
    val ensuredList = if (updated.equippedWeaponIds.contains(updated.equippedWeaponId)) {
      updated.equippedWeaponIds
    } else {
      (listOf(updated.equippedWeaponId) + updated.equippedWeaponIds).distinct().take(3)
    }
    val next = current.copy(
      weaponCustom = updated.copy(equippedWeaponIds = ensuredList),
      challengeProgress = incrementChallenge(current.challengeProgress, "ch_armory_custom", 1L),
    )
    save(next)
  }

  fun updateWonderWeaponCustomization(transform: (com.example.data.model.WonderWeaponCustomization) -> com.example.data.model.WonderWeaponCustomization) {
    val updated = transform(current.wonderCustom)
    val next = current.copy(wonderCustom = updated)
    save(next)
  }

  fun toggleMultiWeaponEquip(weaponId: String): List<String> {
    val cur = current
    val curList = cur.effectiveWeaponList().toMutableList()
    if (curList.contains(weaponId)) {
      if (curList.size > 1) {
        curList.remove(weaponId)
      }
    } else {
      if (curList.size >= 3) {
        curList.removeAt(0)
      }
      curList.add(weaponId)
    }
    val nextPrimary = if (curList.contains(cur.weaponCustom.equippedWeaponId)) {
      cur.weaponCustom.equippedWeaponId
    } else {
      curList.first()
    }
    val spec = ProgressionCatalog.weaponById(nextPrimary, cur.customWeapons)
    save(
      cur.copy(
        weaponCustom = cur.weaponCustom.copy(
          equippedWeaponId = nextPrimary,
          equippedWeaponIds = curList,
          tracerColorHex = spec.defaultTracerHex,
        ),
        challengeProgress = incrementChallenge(cur.challengeProgress, "ch_armory_custom", 1L),
      )
    )
    return curList
  }

  fun toggleMultiWonderEquip(wonderId: String): List<String> {
    val cur = current
    val curList = cur.effectiveWonderList().toMutableList()
    if (curList.contains(wonderId)) {
      if (curList.size > 1) curList.remove(wonderId)
    } else {
      if (curList.size >= 3) curList.removeAt(0)
      curList.add(wonderId)
    }
    save(cur.copy(equippedWonderIds = curList))
    return curList
  }

  fun toggleMultiEquipmentEquip(slotType: EquipmentSlotType, itemId: String): List<String> {
    val cur = current
    val slotId = slotType.id
    val curList = (if (slotType == EquipmentSlotType.LETHAL) cur.equippedLethalIds else cur.equippedTacticalIds)
      .toMutableList()
    if (curList.contains(itemId)) {
      if (curList.size > 1) curList.remove(itemId)
    } else {
      if (curList.size >= 2) curList.removeAt(0)
      curList.add(itemId)
    }
    val eq = cur.equipmentState(slotId)
    val primaryItem = if (curList.contains(eq.equippedItemId)) eq.equippedItemId else curList.first()
    val nextEqMap = cur.equipmentMap + (slotId to eq.copy(equippedItemId = primaryItem))
    val nextState = if (slotType == EquipmentSlotType.LETHAL) {
      cur.copy(equippedLethalIds = curList, equipmentMap = nextEqMap)
    } else {
      cur.copy(equippedTacticalIds = curList, equipmentMap = nextEqMap)
    }
    save(nextState)
    return curList
  }

  fun updateCharacterCustomization(transform: (CharacterCustomization) -> CharacterCustomization) {
    val next = current.copy(
      characterCustom = transform(current.characterCustom),
      challengeProgress = incrementChallenge(current.challengeProgress, "ch_armory_custom", 1L),
    )
    save(next)
  }

  fun updateLevelCustomization(transform: (LevelCustomization) -> LevelCustomization) {
    val next = current.copy(levelCustom = transform(current.levelCustom))
    save(next)
  }

  fun updateLobbyConfig(transform: (LobbyConfig) -> LobbyConfig) {
    val nextConfig = transform(current.lobbyConfig)
    val clampedTarget = nextConfig.targetPlayers.coerceIn(1, 250)
    val clamped = nextConfig.copy(
      targetPlayers = clampedTarget,
      humanPlayersOnline = nextConfig.humanPlayersOnline.coerceIn(1, clampedTarget),
    )
    save(current.copy(lobbyConfig = clamped))
  }

  fun updatePrivateMatchRules(transform: (PrivateMatchRules) -> PrivateMatchRules) {
    val nextRules = transform(current.privateMatchRules)
    save(current.copy(privateMatchRules = nextRules))
  }

  fun updateCollectionCustomMode(transform: (CollectionCustomModeConfig) -> CollectionCustomModeConfig) {
    val updated = transform(current.collectionCustomMode)
    val allowedIds = CollectionCustomModeConfig.scopedAreas(updated.scope, current.customGameMaps).map { it.id }.toSet()
    val safeAreaId = if (updated.targetAreaId in allowedIds) {
      updated.targetAreaId
    } else {
      allowedIds.firstOrNull() ?: updated.targetAreaId
    }
    save(
      current.copy(
        collectionCustomMode = updated.copy(
          targetAreaId = safeAreaId,
          enemyHpBoostPercent = updated.enemyHpBoostPercent.coerceIn(0, 100),
        ),
      ),
    )
  }

  fun addOrUpdateCustomGameMap(mapSpec: UserCustomGameMapSpec): UserCustomGameMapSpec {
    val cur = current
    val sanitizedZones = mapSpec.buildingNames.map { it.trim() }.filter { it.isNotEmpty() }
      .ifEmpty { mapSpec.gameCategory.defaultZones }
    val normalized = mapSpec.copy(
      buildingNames = sanitizedZones.take(8),
      title = mapSpec.title.trim().take(54).ifBlank { "Custom ${mapSpec.gameCategory.label} Map" },
      mapCode = mapSpec.mapCode.trim().take(40).ifBlank { "custom_map" },
      description = mapSpec.description.trim().take(240),
      wonderWeapon = mapSpec.wonderWeapon.trim().take(48).ifBlank { mapSpec.gameCategory.defaultWonderWeapon },
    )
    val nextMaps = (cur.customGameMaps.filterNot { it.id == normalized.id } + normalized).take(40)
    val nextAreas = if (cur.areaStates.containsKey(normalized.id)) {
      cur.areaStates
    } else {
      cur.areaStates + (normalized.id to CustomAreaState(normalized.id))
    }
    save(cur.copy(customGameMaps = nextMaps, areaStates = nextAreas))
    return normalized
  }

  fun deleteCustomGameMap(mapId: String) {
    val cur = current
    val nextMaps = cur.customGameMaps.filterNot { it.id == mapId }
    val nextActive = if (cur.activeAreaId == mapId) "area_bo3_the_giant" else cur.activeAreaId
    val nextCustomModeArea = if (cur.collectionCustomMode.targetAreaId == mapId) {
      "area_bo3_the_giant"
    } else {
      cur.collectionCustomMode.targetAreaId
    }
    save(
      cur.copy(
        customGameMaps = nextMaps,
        activeAreaId = nextActive,
        collectionCustomMode = cur.collectionCustomMode.copy(targetAreaId = nextCustomModeArea),
      ),
    )
  }

  fun addOrUpdateCustomWeapon(weaponSpec: UserCustomWeaponSpec): UserCustomWeaponSpec {
    val cur = current
    val normalized = weaponSpec.copy(
      name = weaponSpec.name.trim().take(48).ifBlank { "Custom ${weaponSpec.gameCategory.label} Armament" },
      weaponCode = weaponSpec.weaponCode.trim().take(36).ifBlank { "wpn_custom" },
      description = weaponSpec.description.trim().take(200),
      baseDamage = weaponSpec.baseDamage.coerceIn(1, 30),
      baseCadenceTicks = weaponSpec.baseCadenceTicks.coerceIn(3.5f, 18.0f),
      baseProjectileSpeed = weaponSpec.baseProjectileSpeed.coerceIn(0.015f, 0.045f),
      baseSpreadCount = weaponSpec.baseSpreadCount.coerceIn(1, 7),
      basePierce = weaponSpec.basePierce.coerceIn(0, 8),
    )
    val nextWeapons = (cur.customWeapons.filterNot { it.id == normalized.id } + normalized).take(40)
    val nextTierMap = if (cur.weaponTierMap.containsKey(normalized.id)) {
      cur.weaponTierMap
    } else {
      cur.weaponTierMap + (normalized.id to 1)
    }
    val nextStarMap = if (cur.weaponStarMap.containsKey(normalized.id)) {
      cur.weaponStarMap
    } else {
      cur.weaponStarMap + (normalized.id to 1)
    }
    save(
      cur.copy(
        customWeapons = nextWeapons,
        weaponTierMap = nextTierMap,
        weaponStarMap = nextStarMap,
      ),
    )
    return normalized
  }

  fun deleteCustomWeapon(weaponId: String) {
    val cur = current
    val nextWeapons = cur.customWeapons.filterNot { it.id == weaponId }
    val nextPrimary = if (cur.weaponCustom.equippedWeaponId == weaponId) "wpn_pulse_carbine" else cur.weaponCustom.equippedWeaponId
    val nextMulti = cur.weaponCustom.equippedWeaponIds.filterNot { it == weaponId }.ifEmpty { listOf(nextPrimary) }
    save(
      cur.copy(
        customWeapons = nextWeapons,
        weaponCustom = cur.weaponCustom.copy(
          equippedWeaponId = nextPrimary,
          equippedWeaponIds = nextMulti,
        ),
      ),
    )
  }

  fun hostPrivateMatch(rules: PrivateMatchRules, hostCallsign: String): MultiverseMatchListing {
    val activeWorld = UniverseCatalog.worldById(rules.worldId)
    val activeMode = UniverseCatalog.modeById(rules.modeId)
    val activeArea = current.resolveAreaSpec(rules.areaId)
    val listing = MultiverseMatchListing(
      id = "match_${rules.roomCode.lowercase()}_${System.currentTimeMillis() % 10000}",
      roomName = rules.roomName,
      hostCallsign = hostCallsign,
      hostLevel = current.playerLevel,
      hostStars = current.characterStarLevel,
      worldId = rules.worldId,
      worldTitle = activeWorld.title,
      modeId = rules.modeId,
      modeLabel = activeMode.label,
      areaId = rules.areaId,
      areaName = activeArea.name,
      currentPlayers = 1,
      maxPlayers = rules.maxPlayers,
      privacy = rules.privacy,
      roomCode = rules.roomCode,
      passcode = if (rules.privacy == MatchPrivacy.PRIVATE_PASSWORD) rules.passcode.ifEmpty { "0000" } else null,
      pingMs = 15,
      currentWave = rules.startingWave,
      activeMutators = rules.activeMutatorLabels(),
      clanTag = current.clan.tag,
    )
    val updatedLobby = current.lobbyConfig.copy(
      targetPlayers = rules.maxPlayers,
      autoFillBots = rules.botFillEnabled,
      botDifficulty = rules.botDifficulty,
      humanPlayersOnline = 1,
    )
    save(
      current.copy(
        activeAreaId = activeArea.id,
        privateMatchRules = rules,
        joinedMatch = listing,
        lobbyConfig = updatedLobby,
      )
    )
    return listing
  }

  fun joinMatch(match: MultiverseMatchListing, enteredPasscode: String? = null): Result<MultiverseMatchListing> {
    if (match.isFull) {
      return Result.failure(IllegalStateException("Match ${match.roomName} is already full (${match.currentPlayers}/${match.maxPlayers})"))
    }
    if (match.privacy == MatchPrivacy.PRIVATE_PASSWORD) {
      val expected = match.passcode.orEmpty()
      if (expected.isNotEmpty() && enteredPasscode != expected) {
        return Result.failure(IllegalStateException("Incorrect passcode for private match ${match.roomName}"))
      }
    }
    val updatedLobby = current.lobbyConfig.copy(
      targetPlayers = match.maxPlayers,
      humanPlayersOnline = (match.currentPlayers + 1).coerceAtMost(match.maxPlayers),
    )
    val joinedListing = match.copy(currentPlayers = match.currentPlayers + 1)
    save(
      current.copy(
        activeAreaId = match.areaId,
        joinedMatch = joinedListing,
        lobbyConfig = updatedLobby,
      )
    )
    return Result.success(joinedListing)
  }

  fun leaveJoinedMatch() {
    save(current.copy(joinedMatch = null))
  }

  fun addFriend(callsign: String): Result<FriendEntry> {
    val clean = AppSecurityManager.sanitizeIdentifierLabel(callsign, maxLength = 24)
    if (clean.length < 2) {
      return Result.failure(IllegalArgumentException("Callsign too short"))
    }
    if (current.friends.any { it.callsign.equals(clean, ignoreCase = true) }) {
      return Result.failure(IllegalStateException("$clean is already on your friends list"))
    }
    val newFriend = FriendEntry(
      id = "friend_${clean.lowercase()}_${System.currentTimeMillis() % 10000}",
      callsign = clean,
      level = (15..55).random(),
      stars = (3..9).random(),
      tier = (3..8).random(),
      status = FriendOnlineStatus.ONLINE_LOBBY,
      statusDetail = "In Lobby",
      clanTag = if ((0..1).random() == 1) current.clan.tag else null,
    )
    save(current.copy(friends = current.friends + newFriend))
    return Result.success(newFriend)
  }

  fun removeFriend(friendId: String) {
    save(current.copy(friends = current.friends.filterNot { it.id == friendId }))
  }

  fun toggleFriendFavorite(friendId: String) {
    val nextFriends = current.friends.map {
      if (it.id == friendId) it.copy(isFavorite = !it.isFavorite) else it
    }
    save(current.copy(friends = nextFriends))
  }

  fun sendChatMessage(
    channel: ChatChannel,
    content: String,
    senderCallsign: String,
    targetFriendId: String? = null,
    isTactical: Boolean = false,
    safetyFilterEnabled: Boolean = true,
  ) {
    val clean = AppSecurityManager.sanitizeChatText(content, safetyFilterEnabled = safetyFilterEnabled)
    if (clean.isEmpty()) return
    val msg = ChatMessage(
      id = "msg_${System.currentTimeMillis()}_${(100..999).random()}",
      channel = channel,
      senderCallsign = AppSecurityManager.sanitizeIdentifierLabel(senderCallsign, 30),
      senderClanTag = current.clan.tag,
      senderStars = current.characterStarLevel,
      targetFriendId = targetFriendId,
      content = clean,
      timestampMillis = System.currentTimeMillis(),
      isTacticalQuickChat = isTactical,
    )
    val nextMessages = (current.chatMessages + msg).takeLast(250)
    save(current.copy(chatMessages = nextMessages))
  }

  fun createClan(
    name: String,
    tag: String,
    motto: String,
    colorHex: Long,
    creatorCallsign: String,
  ): Result<ClanData> {
    val cName = AppSecurityManager.sanitizeIdentifierLabel(name, 32)
    val cTag = AppSecurityManager.sanitizeIdentifierLabel(tag, 5).uppercase()
    val cMotto = AppSecurityManager.sanitizeChatText(motto, safetyFilterEnabled = true, maxLength = 80)
    if (cName.length < 3) return Result.failure(IllegalArgumentException("Clan name too short"))
    if (cTag.length !in 2..5) return Result.failure(IllegalArgumentException("Clan tag must be 2-5 characters"))
    val newClan = ClanData(
      id = "clan_${cTag.lowercase()}_${System.currentTimeMillis() % 10000}",
      name = cName,
      tag = cTag,
      motto = cMotto.ifEmpty { "United we conquer the multiverse." },
      level = 1,
      tier = 1,
      starLevel = 1,
      totalEssenceDonated = 0L,
      leaderCallsign = creatorCallsign,
      colorHex = colorHex,
      members = listOf(
        ClanMember("cm_1", creatorCallsign, ClanRank.LEADER, current.playerLevel, current.characterStarLevel, 0L, true)
      ),
    )
    val announce = ChatMessage(
      id = "msg_clan_create_${System.currentTimeMillis()}",
      channel = ChatChannel.CLAN,
      senderCallsign = creatorCallsign,
      senderClanTag = cTag,
      content = "Clan [$cTag] $cName founded! Welcome all operatives.",
      isSystemMessage = true,
    )
    save(current.copy(clan = newClan, chatMessages = (current.chatMessages + announce).takeLast(250)))
    return Result.success(newClan)
  }

  fun joinClan(targetClan: ClanData, memberCallsign: String) {
    val existingMembers = targetClan.members.filterNot { it.callsign.equals(memberCallsign, ignoreCase = true) }
    val newMember = ClanMember(
      id = "cm_${System.currentTimeMillis() % 10000}",
      callsign = memberCallsign,
      rank = ClanRank.RECRUIT,
      level = current.playerLevel,
      stars = current.characterStarLevel,
      donatedEssence = 0L,
      online = true,
    )
    val updatedClan = targetClan.copy(members = existingMembers + newMember)
    val announce = ChatMessage(
      id = "msg_clan_join_${System.currentTimeMillis()}",
      channel = ChatChannel.CLAN,
      senderCallsign = memberCallsign,
      senderClanTag = updatedClan.tag,
      content = "$memberCallsign has joined the clan!",
      isSystemMessage = true,
    )
    save(current.copy(clan = updatedClan, chatMessages = (current.chatMessages + announce).takeLast(250)))
  }

  fun donateToClan(essenceAmount: Long, memberCallsign: String, totalBlueEssence: Long): Result<Long> {
    if (essenceAmount <= 0L) return Result.failure(IllegalArgumentException("Amount must be positive"))
    if (totalBlueEssence < essenceAmount) return Result.failure(IllegalStateException("Need $essenceAmount Blue Essence"))
    val curClan = current.clan
    val nextDonated = curClan.totalEssenceDonated + essenceAmount
    val computedLevel = (1 + (nextDonated / 5000L).toInt()).coerceIn(1, ProgressionCatalog.MAX_CLAN_LEVEL)
    val newLevel = maxOf(curClan.level, computedLevel)
    val newTier = maxOf(curClan.tier, (1 + (newLevel / 2)).coerceIn(1, PowerTier.MAX_TIER))
    val newStar = maxOf(curClan.starLevel, (1 + (newLevel / 3)).coerceIn(1, StarRankMeta.MAX_STARS))
    val leveledUp = newLevel > curClan.level
    val updatedMembers = curClan.members.map { m ->
      if (m.callsign.equals(memberCallsign, ignoreCase = true)) {
        m.copy(donatedEssence = m.donatedEssence + essenceAmount)
      } else m
    }
    val updatedClan = curClan.copy(
      totalEssenceDonated = nextDonated,
      level = newLevel,
      tier = newTier,
      starLevel = newStar,
      members = updatedMembers,
    )
    val sysMsg = ChatMessage(
      id = "msg_clan_don_${System.currentTimeMillis()}",
      channel = ChatChannel.CLAN,
      senderCallsign = memberCallsign,
      senderClanTag = updatedClan.tag,
      content = "$memberCallsign donated $essenceAmount Blue Essence! Clan Bank: ${nextDonated} B" +
        if (leveledUp) " • CLAN LEVELED UP TO LV.$newLevel (Tier $newTier, ${newStar}★)!" else "",
      isSystemMessage = true,
    )
    save(
      current.copy(
        clan = updatedClan,
        totalBlueEssenceSpent = current.totalBlueEssenceSpent + essenceAmount,
        chatMessages = (current.chatMessages + sysMsg).takeLast(250),
      )
    )
    return Result.success(essenceAmount)
  }

  fun leaveClan(memberCallsign: String) {
    val defaultClan = SocialCatalog.defaultClan
    val announce = ChatMessage(
      id = "msg_clan_leave_${System.currentTimeMillis()}",
      channel = ChatChannel.CLAN,
      senderCallsign = memberCallsign,
      senderClanTag = defaultClan.tag,
      content = "$memberCallsign returned to Clan [${defaultClan.tag}] ${defaultClan.name}.",
      isSystemMessage = true,
    )
    save(current.copy(clan = defaultClan, chatMessages = (current.chatMessages + announce).takeLast(250)))
  }

  fun upgradeGear(slot: GearSlot) {
    val cur = current
    val gear = cur.gearMap[slot.id] ?: GearState(slot)
    if (gear.level >= ProgressionCatalog.MAX_GEAR_LEVEL) return
    val nextLvl = gear.level + 1
    val minXpForNext = ProgressionCatalog.totalXpForGearLevel(nextLvl)
    val nextGear = gear.copy(
      level = nextLvl,
      xp = maxOf(gear.xp, minXpForNext),
    )
    save(cur.copy(gearMap = cur.gearMap + (slot.id to nextGear)))
  }

  fun upgradeGearTier(slot: GearSlot, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val gear = cur.gearMap[slot.id] ?: GearState(slot)
    if (gear.tier >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("${slot.displayName} already at max Tier ${PowerTier.MAX_TIER} Multiversal"))
    }
    val cost = UpgradeCosts.gearTier(gear.tier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextGear = gear.copy(tier = gear.tier + 1)
    save(
      cur.copy(
        gearMap = cur.gearMap + (slot.id to nextGear),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeGearStar(slot: GearSlot, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val gear = cur.gearMap[slot.id] ?: GearState(slot)
    if (gear.starLevel >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("${slot.displayName} already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.gearStar(gear.starLevel)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextGear = gear.copy(starLevel = gear.starLevel + 1)
    save(
      cur.copy(
        gearMap = cur.gearMap + (slot.id to nextGear),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun prestigeGear() {
    val cur = current
    if (cur.gearPrestige >= ProgressionCatalog.MAX_PRESTIGE) return
    val resetGear = cur.gearMap.mapValues { (_, g) ->
      g.copy(level = 1, xp = 0L, prestige = (g.prestige + 1).coerceAtMost(ProgressionCatalog.MAX_PRESTIGE))
    }
    save(cur.copy(gearMap = resetGear, gearPrestige = cur.gearPrestige + 1))
  }

  fun upgradeEquipment(slotId: String) {
    val cur = current
    val eq = cur.equipmentState(slotId)
    if (eq.level >= ProgressionCatalog.MAX_EQUIPMENT_LEVEL) return
    val nextLvl = eq.level + 1
    val minXpForNext = ProgressionCatalog.totalXpForEquipmentLevel(nextLvl)
    val nextEq = eq.copy(
      level = nextLvl,
      xp = maxOf(eq.xp, minXpForNext),
    )
    save(cur.copy(equipmentMap = cur.equipmentMap + (slotId to nextEq)))
  }

  fun upgradeEquipmentTier(slotId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val eq = cur.equipmentState(slotId)
    if (eq.tier >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("Equipment already at max Tier ${PowerTier.MAX_TIER} Multiversal"))
    }
    val cost = UpgradeCosts.equipmentTier(eq.tier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextEq = eq.copy(tier = eq.tier + 1)
    save(
      cur.copy(
        equipmentMap = cur.equipmentMap + (slotId to nextEq),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeEquipmentStar(slotId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val eq = cur.equipmentState(slotId)
    if (eq.starLevel >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("Equipment already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.equipmentStar(eq.starLevel)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextEq = eq.copy(starLevel = eq.starLevel + 1)
    save(
      cur.copy(
        equipmentMap = cur.equipmentMap + (slotId to nextEq),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun prestigeEquipment(slotId: String): Result<Long> {
    val cur = current
    val eq = cur.equipmentMap[slotId] ?: return Result.failure(IllegalStateException("Equipment not found"))
    if (eq.level < ProgressionCatalog.MAX_EQUIPMENT_LEVEL || eq.prestige >= ProgressionCatalog.MAX_PRESTIGE) {
      return Result.failure(
        IllegalStateException(
          "Must reach Level ${ProgressionCatalog.MAX_EQUIPMENT_LEVEL} to Prestige Equipment (Max ${ProgressionCatalog.MAX_PRESTIGE})"
        )
      )
    }
    val nextEq = eq.copy(level = 1, prestige = eq.prestige + 1, xp = 0L)
    save(cur.copy(equipmentMap = cur.equipmentMap + (slotId to nextEq)))
    return Result.success(500L)
  }

  fun equipEquipment(slotId: String, itemId: String) {
    val cur = current
    val eq = cur.equipmentState(slotId)
    val nextEq = eq.copy(equippedItemId = itemId)
    val nextLethals = if (slotId == "lethal") {
      (listOf(itemId) + cur.equippedLethalIds).distinct().take(2)
    } else cur.equippedLethalIds
    val nextTacticals = if (slotId == "tactical") {
      (listOf(itemId) + cur.equippedTacticalIds).distinct().take(2)
    } else cur.equippedTacticalIds
    save(
      cur.copy(
        equipmentMap = cur.equipmentMap + (slotId to nextEq),
        equippedLethalIds = nextLethals,
        equippedTacticalIds = nextTacticals,
      )
    )
  }

  // CHARACTER & OUTFITS SYSTEM (Character + Version + own Skill/Gear/Equipment)
  fun equipOutfit(
    outfitId: String,
    syncSignatureEquipment: Boolean = true,
  ): HeroOutfitResult {
    val cur = current
    val spec = MultiverseArsenalCatalog.outfitById(outfitId)
    val nextCharCustom = cur.characterCustom.copy(
      primaryArmorHex = spec.primaryHex,
      accentArmorHex = spec.secondaryHex,
      cloakEnabled = spec.cloakEnabled,
      auraTrailId = spec.auraId,
      titleBadge = spec.nameWithVersion,
    )
    var nextEqMap = cur.equipmentMap
    var nextLethals = cur.equippedLethalIds
    var nextTacticals = cur.equippedTacticalIds
    if (syncSignatureEquipment) {
      val lethalEq = cur.equipmentState("lethal").copy(equippedItemId = spec.signatureLethalId)
      val tacticalEq = cur.equipmentState("tactical").copy(equippedItemId = spec.signatureTacticalId)
      nextEqMap = cur.equipmentMap + ("lethal" to lethalEq) + ("tactical" to tacticalEq)
      nextLethals = (listOf(spec.signatureLethalId) + cur.equippedLethalIds).distinct().take(2)
      nextTacticals = (listOf(spec.signatureTacticalId) + cur.equippedTacticalIds).distinct().take(2)
    }
    save(
      cur.copy(
        equippedOutfitId = spec.id,
        characterCustom = nextCharCustom,
        equipmentMap = nextEqMap,
        equippedLethalIds = nextLethals,
        equippedTacticalIds = nextTacticals,
      )
    )
    return HeroOutfitResult(spec.nameWithVersion, cur.outfitState(spec.id))
  }

  fun upgradeOutfit(outfitId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val st = cur.outfitState(outfitId)
    val cost = UpgradeCosts.outfitLevelUp(st.tier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence to upgrade outfit"))
    }
    val nextTier = if (st.level % 10 == 0 && st.tier < PowerTier.MAX_TIER) st.tier + 1 else st.tier
    val nextSt = st.copy(
      level = (st.level + 5).coerceAtMost(ProgressionCatalog.MAX_OUTFIT_LEVEL),
      tier = nextTier.coerceIn(1, PowerTier.MAX_TIER),
    )
    save(
      cur.copy(
        outfitStates = cur.outfitStates + (outfitId to nextSt),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeCharacterTier(currentBlueEssence: Long): Result<Long> {
    val cur = current
    if (cur.characterTier >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("Character already at max Tier ${PowerTier.MAX_TIER} Multiversal"))
    }
    val cost = UpgradeCosts.characterTier(cur.characterTier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        characterTier = cur.characterTier + 1,
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeCharacterStar(currentBlueEssence: Long): Result<Long> {
    val cur = current
    if (cur.characterStarLevel >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("Character already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.characterStar(cur.characterStarLevel)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        characterStarLevel = cur.characterStarLevel + 1,
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeOutfitStar(outfitId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val st = cur.outfitState(outfitId)
    if (st.starLevel >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("Outfit already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.outfitStar(st.starLevel)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextSt = st.copy(starLevel = st.starLevel + 1)
    save(
      cur.copy(
        outfitStates = cur.outfitStates + (outfitId to nextSt),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeOutfitRarityWithShards(outfitId: String): Result<CharacterRarity> {
    val cur = current
    val curRarity = cur.outfitRarity(outfitId)
    val nextRarity = curRarity.nextRarity()
      ?: return Result.failure(IllegalStateException("Already at max ${CharacterRarity.OMNIVERSE.label} rarity!"))
    val curShards = cur.outfitShards(outfitId)
    val req = curRarity.shardsForNext.toLong()
    if (curShards < req) {
      return Result.failure(IllegalStateException("Need $req Character Shards (have $curShards). Hunt Shards in Hero Frontier or Crates!"))
    }
    save(
      cur.copy(
        outfitRarityMap = cur.outfitRarityMap + (outfitId to nextRarity.rank),
        outfitShardMap = cur.outfitShardMap + (outfitId to (curShards - req)),
      )
    )
    return Result.success(nextRarity)
  }

  fun recruitCharacterShardDrop(currentBlueEssence: Long, targetOutfitId: String? = null): Result<Triple<String, Long, Long>> {
    val cur = current
    val cost = 350L
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence to deploy Character Beacon"))
    }
    val targetSpec = targetOutfitId?.let { id -> MultiverseArsenalCatalog.outfits.firstOrNull { it.id == id } }
      ?: MultiverseArsenalCatalog.outfits.filter { it.isOriginalCharacter }.randomOrNull()
      ?: MultiverseArsenalCatalog.outfits.random()
    val shardsGained = 4L
    val prevShards = cur.outfitShards(targetSpec.id)
    save(
      cur.copy(
        outfitShardMap = cur.outfitShardMap + (targetSpec.id to (prevShards + shardsGained)),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(Triple(targetSpec.nameWithVersion, shardsGained, cost))
  }

  fun upgradeArmoryItemStar(itemId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val curStar = cur.armoryItemStar(itemId)
    if (curStar >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("Mod already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.armoryModStar(curStar)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        armoryItemStarMap = cur.armoryItemStarMap + (itemId to (curStar + 1)),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeOutfitSubSystem(outfitId: String, part: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val st = cur.outfitState(outfitId)
    val curRank = when (part) {
      "skill" -> st.skillRank
      "gear" -> st.gearRank
      else -> st.equipmentRank
    }
    if (curRank >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("Outfit $part already at max Rank ${PowerTier.MAX_TIER}"))
    }
    val cost = UpgradeCosts.outfitSubSystem(curRank)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextSt = when (part) {
      "skill" -> st.copy(skillRank = curRank + 1, tier = (st.tier + if ((curRank + 1) % 2 == 0) 1 else 0).coerceAtMost(PowerTier.MAX_TIER))
      "gear" -> st.copy(gearRank = curRank + 1)
      else -> st.copy(equipmentRank = curRank + 1)
    }
    save(
      cur.copy(
        outfitStates = cur.outfitStates + (outfitId to nextSt),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  // AREAS & TEMPORAL AGING SYSTEM
  fun selectArea(areaId: String) {
    val cur = current
    val spec = cur.resolveAreaSpec(areaId)
    save(
      cur.copy(
        activeAreaId = spec.id,
        levelCustom = cur.levelCustom.copy(floorAccentHex = spec.accentHex),
      )
    )
  }

  fun upgradeAreaFeature(areaId: String, feature: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val area = cur.areaState(areaId)
    val curTier = when (feature) {
      "power_grid" -> area.powerGridTier
      "turrets" -> area.defenseTurretTier
      "traps" -> area.trapDensityTier
      else -> area.lootRichnessTier
    }
    if (curTier >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("$feature already at max Tier ${PowerTier.MAX_TIER}"))
    }
    val cost = UpgradeCosts.areaFeatureTier(curTier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextArea = when (feature) {
      "power_grid" -> area.copy(powerGridTier = curTier + 1)
      "turrets" -> area.copy(defenseTurretTier = curTier + 1)
      "traps" -> area.copy(trapDensityTier = curTier + 1)
      else -> area.copy(lootRichnessTier = curTier + 1)
    }
    save(
      cur.copy(
        areaStates = cur.areaStates + (areaId to nextArea),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeAreaStar(areaId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val area = cur.areaState(areaId)
    if (area.starLevel >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("Area already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.areaStar(area.starLevel)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextArea = area.copy(starLevel = area.starLevel + 1)
    save(
      cur.copy(
        areaStates = cur.areaStates + (areaId to nextArea),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeModeTier(modeId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val curTier = cur.modeTier(modeId)
    if (curTier >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("Mode already at max Tier ${PowerTier.MAX_TIER}"))
    }
    val cost = UpgradeCosts.modeTier(curTier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        modeTierMap = cur.modeTierMap + (modeId to (curTier + 1)),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeModeStar(modeId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val curStars = cur.modeStar(modeId)
    if (curStars >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("Mode already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.modeStar(curStars)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        modeStarMap = cur.modeStarMap + (modeId to (curStars + 1)),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun advanceAreaAge(areaId: String, yearsDelta: Int = 18): CustomAreaState {
    val cur = current
    val area = cur.areaState(areaId)
    val rawAge = area.ageYears + yearsDelta
    val cyclesGained = rawAge / 100
    val wrappedAge = rawAge % 100
    val nextArea = area.copy(
      ageYears = wrappedAge,
      rebirthCycles = area.rebirthCycles + cyclesGained,
    )
    save(cur.copy(areaStates = cur.areaStates + (areaId to nextArea)))
    return nextArea
  }

  // NEW GAME+ (BASE NG+ & 4 POST-COMPLETION VERSIONS) + 5 COLLECTION DIFFICULTIES
  fun selectNewGamePlusTier(tier: Int): Result<NewGamePlusVersion> {
    val cur = current
    val clamped = tier.coerceIn(0, NewGamePlusVersion.MAX_NG_PLUS_TIER)
    if (clamped > cur.unlockedNgPlusTier) {
      return Result.failure(
        IllegalStateException("Complete NG+ Tier ${cur.unlockedNgPlusTier} to unlock NG+ Tier $clamped")
      )
    }
    val version = NewGamePlusVersion.fromTier(clamped)
    save(cur.copy(activeNgPlusTier = clamped))
    return Result.success(version)
  }

  fun completeNewGamePlusRun(): NewGamePlusVersion {
    val cur = current
    val nextCompletions = cur.ngPlusCompletions + 1
    val nextUnlocked = maxOf(
      cur.unlockedNgPlusTier,
      (cur.activeNgPlusTier + 1).coerceAtMost(NewGamePlusVersion.MAX_NG_PLUS_TIER),
    )
    save(
      cur.copy(
        ngPlusCompletions = nextCompletions,
        unlockedNgPlusTier = nextUnlocked,
      )
    )
    return NewGamePlusVersion.fromTier(nextUnlocked)
  }

  fun setWorldCollectionDifficulty(worldId: String, difficulty: CollectionDifficulty) {
    val cur = current
    save(
      cur.copy(
        worldCollectionDifficultyMap = cur.worldCollectionDifficultyMap + (worldId to difficulty.id)
      )
    )
  }

  // BLAST SHARDS, PERK CANS/BOTTLES, COLORED BLAST CORES & VACUUM BOT
  fun spendBlastShardsUpgrade(shardCost: Long = 10L): Result<Unit> {
    val cur = current
    if (cur.blastShards < shardCost) {
      return Result.failure(IllegalStateException("Need $shardCost Blast Shards"))
    }
    val nextBot = cur.vacuumBot.copy(
      level = (cur.vacuumBot.level + 1).coerceAtMost(ProgressionCatalog.MAX_BOT_LEVEL),
    )
    save(
      cur.copy(
        blastShards = cur.blastShards - shardCost,
        redEssence = cur.redEssence + 60L,
        yellowEssence = cur.yellowEssence + 60L,
        greenEssence = cur.greenEssence + 60L,
        vacuumBot = nextBot,
      )
    )
    return Result.success(Unit)
  }

  fun upgradePerkDrink(perkId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val spec = MultiverseArsenalCatalog.perkDrinks.firstOrNull { it.id == perkId }
      ?: return Result.failure(IllegalStateException("Perk drink not found"))
    val curRank = cur.perkRank(perkId)
    if (curRank >= spec.maxRank) {
      return Result.failure(IllegalStateException("${spec.name} is already Max Rank ${spec.maxRank}"))
    }
    val cost = UpgradeCosts.perkDrink(curRank)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        perkRanks = cur.perkRanks + (perkId to (curRank + 1)),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradePerkStar(perkId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val curStar = cur.perkStar(perkId)
    if (curStar >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("Perk already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.perkStar(curStar)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        perkStarMap = cur.perkStarMap + (perkId to (curStar + 1)),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeAllPerks(currentBlueEssence: Long): Result<Long> {
    val cur = current
    val upgradable = MultiverseArsenalCatalog.perkDrinks.filter { cur.perkRank(it.id) < it.maxRank }
    if (upgradable.isEmpty()) {
      return Result.failure(IllegalStateException("All Perk Drinks are already Max Rank"))
    }
    var remaining = currentBlueEssence
    var spent = 0L
    val nextRanks = cur.perkRanks.toMutableMap()
    for (spec in upgradable) {
      val r = cur.perkRank(spec.id)
      val cost = UpgradeCosts.perkDrink(r)
      if (remaining >= cost) {
        remaining -= cost
        spent += cost
        nextRanks[spec.id] = r + 1
      }
    }
    if (spent == 0L) {
      val minCost = upgradable.minOf { UpgradeCosts.perkDrink(cur.perkRank(it.id)) }
      return Result.failure(IllegalStateException("Need at least $minCost Blue Essence"))
    }
    save(
      cur.copy(
        perkRanks = nextRanks,
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + spent,
      )
    )
    return Result.success(spent)
  }

  fun upgradeProtection(protId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val spec = ProgressionCatalog.protectionUpgrades.firstOrNull { it.id == protId }
      ?: return Result.failure(IllegalStateException("Protection upgrade not found"))
    val curRank = cur.protectionRank(protId)
    if (curRank >= spec.maxRank) {
      return Result.failure(IllegalStateException("${spec.title} already at max Rank ${spec.maxRank}"))
    }
    val cost = spec.nextCost(curRank)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        protectionRanks = cur.protectionRanks + (protId to (curRank + 1)),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeProtectionStar(protId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val curStar = cur.protectionStar(protId)
    if (curStar >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("Protection already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.protectionStar(curStar)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        protectionStarMap = cur.protectionStarMap + (protId to (curStar + 1)),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeBlastShardTier(currentBlueEssence: Long): Result<Long> {
    val cur = current
    if (cur.blastShardTier >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("Blast Shard Channeling already at max Tier ${PowerTier.MAX_TIER}"))
    }
    val cost = UpgradeCosts.blastShardTier(cur.blastShardTier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        blastShardTier = cur.blastShardTier + 1,
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeCompanionBuffTier(currentBlueEssence: Long): Result<Long> {
    val cur = current
    if (cur.companionBuffTier >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("Companion Squad Buff already at max Tier ${PowerTier.MAX_TIER}"))
    }
    val cost = UpgradeCosts.companionBuffTier(cur.companionBuffTier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        companionBuffTier = cur.companionBuffTier + 1,
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun addOrUpdateCustomPerk(spec: UserCustomPerkSpec) {
    val cur = current
    val existing = cur.customPerks.firstOrNull { it.id == spec.id }
    val preserved = if (existing != null) {
      spec.copy(
        tier = maxOf(existing.tier, spec.tier).coerceIn(1, PowerTier.MAX_TIER),
        maxTier = maxOf(existing.maxTier, spec.maxTier).coerceIn(1, PowerTier.MAX_TIER),
        starLevel = maxOf(existing.starLevel, spec.starLevel).coerceIn(1, StarRankMeta.MAX_STARS),
      )
    } else spec
    val updatedList = (cur.customPerks.filterNot { it.id == spec.id } + preserved)
    save(
      cur.copy(
        customPerks = updatedList,
        equippedCustomPerkIds = cur.equippedCustomPerkIds + preserved.id,
      )
    )
  }

  fun toggleEquipCustomPerk(perkId: String) {
    val cur = current
    val nextSet = if (perkId in cur.equippedCustomPerkIds) {
      cur.equippedCustomPerkIds - perkId
    } else {
      cur.equippedCustomPerkIds + perkId
    }
    save(cur.copy(equippedCustomPerkIds = nextSet))
  }

  fun upgradeCustomPerkTier(perkId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val existing = cur.customPerks.firstOrNull { it.id == perkId }
      ?: return Result.failure(IllegalStateException("Custom Perk not found"))
    if (existing.tier >= existing.maxTier) {
      return Result.failure(IllegalStateException("${existing.name} already at max Tier ${existing.maxTier}"))
    }
    val cost = UpgradeCosts.customBuffTier(existing.tier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextItem = existing.copy(tier = existing.tier + 1)
    save(
      cur.copy(
        customPerks = cur.customPerks.map { if (it.id == perkId) nextItem else it },
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeCustomPerkStar(perkId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val existing = cur.customPerks.firstOrNull { it.id == perkId }
      ?: return Result.failure(IllegalStateException("Custom Perk not found"))
    if (existing.starLevel >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("${existing.name} already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.customBuffStar(existing.starLevel)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextItem = existing.copy(starLevel = existing.starLevel + 1)
    save(
      cur.copy(
        customPerks = cur.customPerks.map { if (it.id == perkId) nextItem else it },
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun deleteCustomPerk(perkId: String) {
    val cur = current
    save(
      cur.copy(
        customPerks = cur.customPerks.filterNot { it.id == perkId },
        equippedCustomPerkIds = cur.equippedCustomPerkIds - perkId,
      )
    )
  }

  fun customizeGearSlot(slot: GearSlot, customization: GearSlotCustomization) {
    val cur = current
    save(
      cur.copy(
        gearCustomizations = cur.gearCustomizations + (slot.id to customization.copy(slotId = slot.id)),
      )
    )
  }

  fun addOrUpdateCustomGear(spec: UserCustomGearSpec) {
    val cur = current
    val existing = cur.customGear.firstOrNull { it.id == spec.id }
    val preserved = if (existing != null) {
      spec.copy(
        tier = maxOf(existing.tier, spec.tier).coerceIn(1, PowerTier.MAX_TIER),
        maxTier = maxOf(existing.maxTier, spec.maxTier).coerceIn(1, PowerTier.MAX_TIER),
        starLevel = maxOf(existing.starLevel, spec.starLevel).coerceIn(1, StarRankMeta.MAX_STARS),
      )
    } else spec
    val nextList = cur.customGear.filterNot { it.id == spec.id } + preserved
    save(
      cur.copy(
        customGear = nextList,
        equippedCustomGearMap = cur.equippedCustomGearMap + (preserved.slot.id to preserved.id),
      )
    )
  }

  fun equipCustomGear(slot: GearSlot, gearId: String?) {
    val cur = current
    val nextMap = if (gearId.isNullOrBlank() || cur.equippedCustomGearMap[slot.id] == gearId) {
      cur.equippedCustomGearMap - slot.id
    } else {
      cur.equippedCustomGearMap + (slot.id to gearId)
    }
    save(cur.copy(equippedCustomGearMap = nextMap))
  }

  fun upgradeCustomGearTier(gearId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val existing = cur.customGear.firstOrNull { it.id == gearId }
      ?: return Result.failure(IllegalStateException("Custom Gear not found"))
    if (existing.tier >= existing.maxTier) {
      return Result.failure(IllegalStateException("${existing.name} already at max Tier ${existing.maxTier}"))
    }
    val cost = UpgradeCosts.customBuffTier(existing.tier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextItem = existing.copy(tier = existing.tier + 1)
    save(
      cur.copy(
        customGear = cur.customGear.map { if (it.id == gearId) nextItem else it },
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeCustomGearStar(gearId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val existing = cur.customGear.firstOrNull { it.id == gearId }
      ?: return Result.failure(IllegalStateException("Custom Gear not found"))
    if (existing.starLevel >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("${existing.name} already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.customBuffStar(existing.starLevel)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextItem = existing.copy(starLevel = existing.starLevel + 1)
    save(
      cur.copy(
        customGear = cur.customGear.map { if (it.id == gearId) nextItem else it },
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun deleteCustomGear(gearId: String) {
    val cur = current
    save(
      cur.copy(
        customGear = cur.customGear.filterNot { it.id == gearId },
        equippedCustomGearMap = cur.equippedCustomGearMap.filterValues { it != gearId },
      )
    )
  }

  fun setEquipmentVariant(slotId: String, variantId: String) {
    val cur = current
    val st = cur.equipmentState(slotId)
    save(
      cur.copy(
        equipmentMap = cur.equipmentMap + (slotId to st.copy(variantId = variantId)),
      )
    )
  }

  fun addOrUpdateCustomEquipment(spec: UserCustomEquipmentSpec) {
    val cur = current
    val existing = cur.customEquipment.firstOrNull { it.id == spec.id }
    val preserved = if (existing != null) {
      spec.copy(
        tier = maxOf(existing.tier, spec.tier).coerceIn(1, PowerTier.MAX_TIER),
        maxTier = maxOf(existing.maxTier, spec.maxTier).coerceIn(1, PowerTier.MAX_TIER),
        starLevel = maxOf(existing.starLevel, spec.starLevel).coerceIn(1, StarRankMeta.MAX_STARS),
      )
    } else spec
    val nextList = cur.customEquipment.filterNot { it.id == spec.id } + preserved
    val slotId = if (preserved.slotType == EquipmentSlotType.LETHAL) "lethal" else "tactical"
    val eqSt = cur.equipmentState(slotId)
    save(
      cur.copy(
        customEquipment = nextList,
        equipmentMap = cur.equipmentMap + (slotId to eqSt.copy(equippedItemId = preserved.id)),
        equippedLethalIds = if (preserved.slotType == EquipmentSlotType.LETHAL) {
          (listOf(preserved.id) + cur.equippedLethalIds).distinct().take(2)
        } else cur.equippedLethalIds,
        equippedTacticalIds = if (preserved.slotType == EquipmentSlotType.TACTICAL) {
          (listOf(preserved.id) + cur.equippedTacticalIds).distinct().take(2)
        } else cur.equippedTacticalIds,
      )
    )
  }

  fun upgradeCustomEquipmentTier(itemId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val existing = cur.customEquipment.firstOrNull { it.id == itemId }
      ?: return Result.failure(IllegalStateException("Custom Equipment not found"))
    if (existing.tier >= existing.maxTier) {
      return Result.failure(IllegalStateException("${existing.name} already at max Tier ${existing.maxTier}"))
    }
    val cost = UpgradeCosts.customBuffTier(existing.tier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextItem = existing.copy(tier = existing.tier + 1)
    save(
      cur.copy(
        customEquipment = cur.customEquipment.map { if (it.id == itemId) nextItem else it },
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeCustomEquipmentStar(itemId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val existing = cur.customEquipment.firstOrNull { it.id == itemId }
      ?: return Result.failure(IllegalStateException("Custom Equipment not found"))
    if (existing.starLevel >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("${existing.name} already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.customBuffStar(existing.starLevel)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextItem = existing.copy(starLevel = existing.starLevel + 1)
    save(
      cur.copy(
        customEquipment = cur.customEquipment.map { if (it.id == itemId) nextItem else it },
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun deleteCustomEquipment(itemId: String) {
    val cur = current
    val fallbackLethal = cur.equippedLethalIds.filterNot { it == itemId }.ifEmpty { listOf("lethal_frag") }
    val fallbackTactical = cur.equippedTacticalIds.filterNot { it == itemId }.ifEmpty { listOf("tactical_stim") }
    val nextEqMap = cur.equipmentMap.mapValues { (slot, eq) ->
      if (eq.equippedItemId == itemId) {
        eq.copy(equippedItemId = if (slot == "lethal") fallbackLethal.first() else fallbackTactical.first())
      } else eq
    }
    save(
      cur.copy(
        customEquipment = cur.customEquipment.filterNot { it.id == itemId },
        equippedLethalIds = fallbackLethal,
        equippedTacticalIds = fallbackTactical,
        equipmentMap = nextEqMap,
      )
    )
  }

  fun upgradeBlastCore(color: BlastCoreColor): Result<Int> {
    val cur = current
    val count = cur.blastCoreCount(color)
    val curLvl = cur.blastCoreLevel(color)
    if (curLvl >= ProgressionCatalog.MAX_CORE_LEVEL) {
      return Result.failure(IllegalStateException("${color.displayName} is already Max Level ${ProgressionCatalog.MAX_CORE_LEVEL}"))
    }
    if (count < 1L) {
      return Result.failure(IllegalStateException("Need 1 ${color.displayName} (collect in Arena or forge in Trading Post)"))
    }
    val nextInv = cur.blastCoreInventory + (color.id to (count - 1L))
    val nextLvls = cur.blastCoreLevels + (color.id to (curLvl + 1))
    save(cur.copy(blastCoreInventory = nextInv, blastCoreLevels = nextLvls))
    return Result.success(curLvl + 1)
  }

  fun upgradeBlastCoreWithEssence(color: BlastCoreColor, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val curLvl = cur.blastCoreLevel(color)
    if (curLvl >= ProgressionCatalog.MAX_CORE_LEVEL) {
      return Result.failure(IllegalStateException("${color.displayName} is already Max Level ${ProgressionCatalog.MAX_CORE_LEVEL}"))
    }
    val cost = UpgradeCosts.blastCoreBlueEssence(curLvl)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextLvls = cur.blastCoreLevels + (color.id to (curLvl + 1))
    save(
      cur.copy(
        blastCoreLevels = nextLvls,
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeVacuumBot(currentBlueEssence: Long): Result<Long> {
    val cur = current
    val bot = cur.vacuumBot
    if (bot.tier >= PowerTier.MAX_TIER && bot.level >= ProgressionCatalog.MAX_BOT_LEVEL) {
      return Result.failure(IllegalStateException("Vacuum Bot is already Tier ${PowerTier.MAX_TIER} Multiversal Max"))
    }
    val cost = UpgradeCosts.vacuumBot(bot.tier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextLvl = (bot.level + 3).coerceAtMost(ProgressionCatalog.MAX_BOT_LEVEL)
    val nextTier = (bot.tier + 1).coerceAtMost(PowerTier.MAX_TIER)
    save(
      cur.copy(
        vacuumBot = bot.copy(level = nextLvl, tier = nextTier),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeVacuumBotStar(currentBlueEssence: Long): Result<Long> {
    val cur = current
    val bot = cur.vacuumBot
    if (bot.starLevel >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("Vacuum Bot already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.vacuumBotStar(bot.starLevel)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        vacuumBot = bot.copy(starLevel = bot.starLevel + 1),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun toggleVacuumAutoUpgrade() {
    val cur = current
    save(cur.copy(vacuumBot = cur.vacuumBot.copy(autoUpgradeEnabled = !cur.vacuumBot.autoUpgradeEnabled)))
  }

  /**
   * Executes a Trade Post transaction. Returns net Blue Essence change (negative = spent, positive = gained).
   */
  fun executeTradeOffer(offerId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val offer = MultiverseArsenalCatalog.tradeOffers.firstOrNull { it.id == offerId }
      ?: return Result.failure(IllegalStateException("Trade offer not found"))

    // Validate balances
    for ((kind, req) in offer.inputCosts) {
      val have = when (kind) {
        TradeCurrencyKind.BLUE_ESSENCE -> currentBlueEssence
        TradeCurrencyKind.RED_ESSENCE -> cur.redEssence
        TradeCurrencyKind.YELLOW_ESSENCE -> cur.yellowEssence
        TradeCurrencyKind.GREEN_ESSENCE -> cur.greenEssence
        TradeCurrencyKind.COMBINED_ESSENCE -> cur.combinedEssence
        TradeCurrencyKind.BLAST_SHARDS -> cur.blastShards
        TradeCurrencyKind.BLUE_CORE -> cur.blastCoreCount(BlastCoreColor.BLUE)
        TradeCurrencyKind.PURPLE_CORE -> cur.blastCoreCount(BlastCoreColor.PURPLE)
        TradeCurrencyKind.GREEN_CORE -> cur.blastCoreCount(BlastCoreColor.GREEN)
        TradeCurrencyKind.YELLOW_CORE -> cur.blastCoreCount(BlastCoreColor.YELLOW)
        TradeCurrencyKind.RED_CORE -> cur.blastCoreCount(BlastCoreColor.RED)
      }
      if (have < req) {
        return Result.failure(IllegalStateException("Need $req ${kind.label} (have $have)"))
      }
    }

    var nextRed = cur.redEssence
    var nextYellow = cur.yellowEssence
    var nextGreen = cur.greenEssence
    var nextCombined = cur.combinedEssence
    var nextShards = cur.blastShards
    val nextCores = cur.blastCoreInventory.toMutableMap()
    var blueDelta = 0L
    var blueSpent = 0L

    for ((kind, cost) in offer.inputCosts) {
      when (kind) {
        TradeCurrencyKind.BLUE_ESSENCE -> {
          blueDelta -= cost
          blueSpent += cost
        }
        TradeCurrencyKind.RED_ESSENCE -> nextRed -= cost
        TradeCurrencyKind.YELLOW_ESSENCE -> nextYellow -= cost
        TradeCurrencyKind.GREEN_ESSENCE -> nextGreen -= cost
        TradeCurrencyKind.COMBINED_ESSENCE -> nextCombined -= cost
        TradeCurrencyKind.BLAST_SHARDS -> nextShards -= cost
        TradeCurrencyKind.BLUE_CORE -> nextCores[BlastCoreColor.BLUE.id] = (nextCores[BlastCoreColor.BLUE.id] ?: 0L) - cost
        TradeCurrencyKind.PURPLE_CORE -> nextCores[BlastCoreColor.PURPLE.id] = (nextCores[BlastCoreColor.PURPLE.id] ?: 0L) - cost
        TradeCurrencyKind.GREEN_CORE -> nextCores[BlastCoreColor.GREEN.id] = (nextCores[BlastCoreColor.GREEN.id] ?: 0L) - cost
        TradeCurrencyKind.YELLOW_CORE -> nextCores[BlastCoreColor.YELLOW.id] = (nextCores[BlastCoreColor.YELLOW.id] ?: 0L) - cost
        TradeCurrencyKind.RED_CORE -> nextCores[BlastCoreColor.RED.id] = (nextCores[BlastCoreColor.RED.id] ?: 0L) - cost
      }
    }

    when (offer.outputKind) {
      TradeCurrencyKind.BLUE_ESSENCE -> blueDelta += offer.outputAmount
      TradeCurrencyKind.RED_ESSENCE -> nextRed += offer.outputAmount
      TradeCurrencyKind.YELLOW_ESSENCE -> nextYellow += offer.outputAmount
      TradeCurrencyKind.GREEN_ESSENCE -> nextGreen += offer.outputAmount
      TradeCurrencyKind.COMBINED_ESSENCE -> nextCombined += offer.outputAmount
      TradeCurrencyKind.BLAST_SHARDS -> nextShards += offer.outputAmount
      TradeCurrencyKind.BLUE_CORE -> nextCores[BlastCoreColor.BLUE.id] = (nextCores[BlastCoreColor.BLUE.id] ?: 0L) + offer.outputAmount
      TradeCurrencyKind.PURPLE_CORE -> nextCores[BlastCoreColor.PURPLE.id] = (nextCores[BlastCoreColor.PURPLE.id] ?: 0L) + offer.outputAmount
      TradeCurrencyKind.GREEN_CORE -> nextCores[BlastCoreColor.GREEN.id] = (nextCores[BlastCoreColor.GREEN.id] ?: 0L) + offer.outputAmount
      TradeCurrencyKind.YELLOW_CORE -> nextCores[BlastCoreColor.YELLOW.id] = (nextCores[BlastCoreColor.YELLOW.id] ?: 0L) + offer.outputAmount
      TradeCurrencyKind.RED_CORE -> nextCores[BlastCoreColor.RED.id] = (nextCores[BlastCoreColor.RED.id] ?: 0L) + offer.outputAmount
    }

    save(
      cur.copy(
        redEssence = nextRed.coerceAtLeast(0L),
        yellowEssence = nextYellow.coerceAtLeast(0L),
        greenEssence = nextGreen.coerceAtLeast(0L),
        combinedEssence = nextCombined.coerceAtLeast(0L),
        blastShards = nextShards.coerceAtLeast(0L),
        blastCoreInventory = nextCores,
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + blueSpent,
        storePurchasesCount = cur.storePurchasesCount + 1,
      )
    )
    return Result.success(blueDelta)
  }

  fun unlockSkillNode(nodeId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val node = cur.skillNodes[nodeId] ?: return Result.failure(IllegalStateException("Skill node not found"))
    if (!node.unlocked) {
      if (currentBlueEssence < node.costEssence) {
        return Result.failure(IllegalStateException("Need ${node.costEssence} Blue Essence"))
      }
      val nextNode = node.copy(unlocked = true, tier = 1)
      save(
        cur.copy(
          skillNodes = cur.skillNodes + (nodeId to nextNode),
          totalBlueEssenceSpent = cur.totalBlueEssenceSpent + node.costEssence,
        )
      )
      return Result.success(node.costEssence)
    }
    if (node.tier >= node.maxTier) {
      return Result.failure(IllegalStateException("Already at max Tier ${node.maxTier}"))
    }
    val upgradeCost = node.nextUpgradeCost()
    if (currentBlueEssence < upgradeCost) {
      return Result.failure(IllegalStateException("Need $upgradeCost Blue Essence"))
    }
    val nextNode = node.copy(tier = node.tier + 1)
    save(
      cur.copy(
        skillNodes = cur.skillNodes + (nodeId to nextNode),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + upgradeCost,
      )
    )
    return Result.success(upgradeCost)
  }

  fun upgradeSkillStar(nodeId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val node = cur.skillNodes[nodeId] ?: return Result.failure(IllegalStateException("Skill node not found"))
    if (!node.unlocked) {
      return Result.failure(IllegalStateException("Unlock ${node.title} before raising its Star Level"))
    }
    if (node.starLevel >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("${node.title} already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.skillStar(node.starLevel)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextNode = node.copy(starLevel = node.starLevel + 1)
    save(
      cur.copy(
        skillNodes = cur.skillNodes + (nodeId to nextNode),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeWeaponTier(weaponId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val curTier = cur.weaponTier(weaponId)
    if (curTier >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("Weapon already at max Tier ${PowerTier.MAX_TIER} Transcendent"))
    }
    val cost = UpgradeCosts.weaponTier(curTier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextMap = cur.weaponTierMap + (weaponId to (curTier + 1))
    save(
      cur.copy(
        weaponTierMap = nextMap,
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeWeaponStar(weaponId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val curStar = cur.weaponStar(weaponId)
    if (curStar >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("Weapon already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.weaponStar(curStar)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        weaponStarMap = cur.weaponStarMap + (weaponId to (curStar + 1)),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeWonderTier(wonderId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val curTier = cur.wonderTier(wonderId)
    if (curTier >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("Wonder Weapon already at max Tier ${PowerTier.MAX_TIER} Transcendent"))
    }
    val cost = UpgradeCosts.wonderTier(curTier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextMap = cur.wonderTierMap + (wonderId to (curTier + 1))
    save(
      cur.copy(
        wonderTierMap = nextMap,
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun upgradeWonderStar(wonderId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val curStar = cur.wonderStar(wonderId)
    if (curStar >= StarRankMeta.MAX_STARS) {
      return Result.failure(IllegalStateException("Wonder Weapon already at max ${StarRankMeta.MAX_STARS}★"))
    }
    val cost = UpgradeCosts.wonderStar(curStar)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    save(
      cur.copy(
        wonderStarMap = cur.wonderStarMap + (wonderId to (curStar + 1)),
        totalBlueEssenceSpent = cur.totalBlueEssenceSpent + cost,
      )
    )
    return Result.success(cost)
  }

  fun prestigePlayer(): Result<Long> {
    val cur = current
    if (cur.playerLevel < ProgressionCatalog.MAX_PLAYER_LEVEL || cur.playerPrestige >= ProgressionCatalog.MAX_PRESTIGE) {
      return Result.failure(
        IllegalStateException(
          "Must reach Level ${ProgressionCatalog.MAX_PLAYER_LEVEL} to Prestige (Max ${ProgressionCatalog.MAX_PRESTIGE})"
        )
      )
    }
    val rewardBlueEssence = (cur.playerPrestige + 1) * 2500L
    val next = cur.copy(
      playerXp = 0L,
      playerPrestige = cur.playerPrestige + 1,
    )
    save(next)
    return Result.success(rewardBlueEssence)
  }

  fun prestigeWeapon(weaponId: String): Result<Long> {
    val cur = current
    val wpnLvl = cur.weaponLevel(weaponId)
    val wpnPrestige = cur.weaponPrestigeMap[weaponId] ?: 0
    if (wpnLvl < ProgressionCatalog.MAX_WEAPON_LEVEL || wpnPrestige >= ProgressionCatalog.MAX_PRESTIGE) {
      return Result.failure(
        IllegalStateException(
          "Must reach Weapon Level ${ProgressionCatalog.MAX_WEAPON_LEVEL} to Prestige (Max ${ProgressionCatalog.MAX_PRESTIGE})"
        )
      )
    }
    val nextMap = cur.weaponXpMap + (weaponId to 0L)
    val nextPrestigeMap = cur.weaponPrestigeMap + (weaponId to wpnPrestige + 1)
    val rewardBlueEssence = 1000L
    save(cur.copy(weaponXpMap = nextMap, weaponPrestigeMap = nextPrestigeMap))
    return Result.success(rewardBlueEssence)
  }

  private fun allKnownArmoryStarKeys(cur: OperatorProgressionState, extraIds: Collection<String>): List<String> =
    (listOf(
      "perk_juggernaut", "perk_overdrive", "power_tendril", "power_repulse",
      "power_chevron", "upgrade_packtier", "upgrade_claws", "consumable_maxcharge",
    ) + cur.armoryItemStarMap.keys + extraIds).distinct()

  fun boostAllTiers(delta: Int = 1): Int {
    val cur = current
    val targetCharTier = (cur.characterTier + delta).coerceIn(1, PowerTier.MAX_TIER)
    val nextWeapons = ProgressionCatalog.allWeapons(cur.customWeapons).associate { w ->
      w.id to (cur.weaponTier(w.id) + delta).coerceIn(1, PowerTier.MAX_TIER)
    }
    val nextWonders = WonderWeaponKind.entries.associate { w ->
      w.id to (cur.wonderTier(w.id) + delta).coerceIn(1, PowerTier.MAX_TIER)
    }
    val nextGear = cur.gearMap.mapValues { (_, g) -> g.copy(tier = (g.tier + delta).coerceIn(1, PowerTier.MAX_TIER)) }
    val nextEq = cur.equipmentMap.mapValues { (_, e) -> e.copy(tier = (e.tier + delta).coerceIn(1, PowerTier.MAX_TIER)) }
    val nextSkills = cur.skillNodes.mapValues { (_, s) -> s.copy(tier = (s.tier + delta).coerceIn(1, s.maxTier), unlocked = true) }
    val nextPerks = MultiverseArsenalCatalog.perkDrinks.associate { it.id to (cur.perkRank(it.id) + delta).coerceIn(1, it.maxRank) }
    val nextProt = ProgressionCatalog.protectionUpgrades.associate { it.id to (cur.protectionRank(it.id) + delta).coerceIn(1, it.maxRank) }
    val nextCustomPerks = cur.customPerks.map { it.copy(tier = (it.tier + delta).coerceIn(1, it.maxTier)) }
    val nextCustomGear = cur.customGear.map { it.copy(tier = (it.tier + delta).coerceIn(1, it.maxTier)) }
    val nextCustomEq = cur.customEquipment.map { it.copy(tier = (it.tier + delta).coerceIn(1, it.maxTier)) }
    val nextCores = cur.blastCoreLevels.mapValues { (_, l) -> (l + delta).coerceIn(1, ProgressionCatalog.MAX_CORE_LEVEL) }
    val allAreaIds = (cur.allPlayableAreas().map { it.id } + cur.areaStates.keys).distinct()
    val nextAreas = allAreaIds.associateWith { areaId ->
      val a = cur.areaState(areaId)
      a.copy(
        powerGridTier = (a.powerGridTier + delta).coerceIn(1, PowerTier.MAX_TIER),
        defenseTurretTier = (a.defenseTurretTier + delta).coerceIn(1, PowerTier.MAX_TIER),
        trapDensityTier = (a.trapDensityTier + delta).coerceIn(1, PowerTier.MAX_TIER),
        lootRichnessTier = (a.lootRichnessTier + delta).coerceIn(1, PowerTier.MAX_TIER),
      )
    }
    val allModeIds = (UniverseCatalog.modes.map { it.id } + cur.modeTierMap.keys + cur.modeStarMap.keys).distinct()
    val nextModeTiers = allModeIds.associateWith { modeId ->
      (cur.modeTier(modeId) + delta).coerceIn(1, PowerTier.MAX_TIER)
    }
    val nextOutfits = (MultiverseArsenalCatalog.outfits.map { it.id } + cur.outfitStates.keys).distinct().associateWith { oid ->
      val o = cur.outfitState(oid)
      o.copy(
        tier = (o.tier + delta).coerceIn(1, PowerTier.MAX_TIER),
        skillRank = (o.skillRank + delta).coerceIn(1, PowerTier.MAX_TIER),
        gearRank = (o.gearRank + delta).coerceIn(1, PowerTier.MAX_TIER),
        equipmentRank = (o.equipmentRank + delta).coerceIn(1, PowerTier.MAX_TIER),
      )
    }
    val nextMagic = cur.magicState.copy(
      magicTier = (cur.magicState.magicTier + delta).coerceIn(1, PowerTier.MAX_TIER),
      spellTiers = MultiverseSystemsCatalog.magicSpells.associate { s ->
        s.id to (cur.magicState.spellTier(s.id) + delta).coerceIn(1, PowerTier.MAX_TIER)
      },
    )
    val nextBase = cur.playerBaseState.copy(
      wallTier = (cur.playerBaseState.wallTier + delta).coerceIn(1, PowerTier.MAX_TIER),
      turretTier = (cur.playerBaseState.turretTier + delta).coerceIn(1, PowerTier.MAX_TIER),
      shieldGeneratorTier = (cur.playerBaseState.shieldGeneratorTier + delta).coerceIn(1, PowerTier.MAX_TIER),
      resourceVaultTier = (cur.playerBaseState.resourceVaultTier + delta).coerceIn(1, PowerTier.MAX_TIER),
    ).let { it.copy(baseHpMax = it.maxIntegrity(), baseHpCurrent = it.maxIntegrity()) }
    val nextBot = cur.vacuumBot.copy(
      tier = (cur.vacuumBot.tier + delta).coerceIn(1, PowerTier.MAX_TIER),
      level = (cur.vacuumBot.level + delta).coerceIn(1, ProgressionCatalog.MAX_BOT_LEVEL),
    )
    val nextClan = cur.clan.copy(
      tier = (cur.clan.tier + delta).coerceIn(1, PowerTier.MAX_TIER),
      level = (cur.clan.level + delta).coerceIn(1, ProgressionCatalog.MAX_CLAN_LEVEL),
    )
    save(
      cur.copy(
        characterTier = targetCharTier,
        weaponTierMap = nextWeapons,
        wonderTierMap = nextWonders,
        gearMap = nextGear,
        equipmentMap = nextEq,
        skillNodes = nextSkills,
        perkRanks = nextPerks,
        protectionRanks = nextProt,
        customPerks = nextCustomPerks,
        customGear = nextCustomGear,
        customEquipment = nextCustomEq,
        blastShardTier = (cur.blastShardTier + delta).coerceIn(1, PowerTier.MAX_TIER),
        companionBuffTier = (cur.companionBuffTier + delta).coerceIn(1, PowerTier.MAX_TIER),
        blastCoreLevels = nextCores,
        areaStates = nextAreas,
        modeTierMap = nextModeTiers,
        outfitStates = nextOutfits,
        magicState = nextMagic,
        playerBaseState = nextBase,
        vacuumBot = nextBot,
        clan = nextClan,
      )
    )
    return delta
  }

  fun setAllTiers(targetTier: Int = PowerTier.MAX_TIER): Int {
    val clamped = targetTier.coerceIn(1, PowerTier.MAX_TIER)
    val cur = current
    val nextWeapons = ProgressionCatalog.allWeapons(cur.customWeapons).associate { it.id to clamped }
    val nextWonders = WonderWeaponKind.entries.associate { it.id to clamped }
    val nextGear = cur.gearMap.mapValues { (_, g) -> g.copy(tier = clamped) }
    val nextEq = cur.equipmentMap.mapValues { (_, e) -> e.copy(tier = clamped) }
    val nextSkills = cur.skillNodes.mapValues { (_, s) -> s.copy(tier = minOf(clamped, s.maxTier), unlocked = true) }
    val nextPerks = MultiverseArsenalCatalog.perkDrinks.associate { it.id to minOf(clamped, it.maxRank) }
    val nextProt = ProgressionCatalog.protectionUpgrades.associate { it.id to minOf(clamped, it.maxRank) }
    val nextCustomPerks = cur.customPerks.map { it.copy(tier = minOf(clamped, it.maxTier)) }
    val nextCustomGear = cur.customGear.map { it.copy(tier = minOf(clamped, it.maxTier)) }
    val nextCustomEq = cur.customEquipment.map { it.copy(tier = minOf(clamped, it.maxTier)) }
    val nextCores = cur.blastCoreLevels.mapValues { _ -> minOf(clamped, ProgressionCatalog.MAX_CORE_LEVEL) }
    val allAreaIds = (cur.allPlayableAreas().map { it.id } + cur.areaStates.keys).distinct()
    val nextAreas = allAreaIds.associateWith { areaId ->
      cur.areaState(areaId).copy(
        powerGridTier = clamped,
        defenseTurretTier = clamped,
        trapDensityTier = clamped,
        lootRichnessTier = clamped,
      )
    }
    val allModeIds = (UniverseCatalog.modes.map { it.id } + cur.modeTierMap.keys + cur.modeStarMap.keys).distinct()
    val nextModeTiers = allModeIds.associateWith { clamped }
    val nextOutfits = (MultiverseArsenalCatalog.outfits.map { it.id } + cur.outfitStates.keys).distinct().associateWith { oid ->
      cur.outfitState(oid).copy(
        tier = clamped,
        skillRank = clamped,
        gearRank = clamped,
        equipmentRank = clamped,
      )
    }
    val nextMagic = cur.magicState.copy(
      magicTier = clamped,
      spellTiers = MultiverseSystemsCatalog.magicSpells.associate { it.id to clamped },
    )
    val nextBase = cur.playerBaseState.copy(
      wallTier = clamped,
      turretTier = clamped,
      shieldGeneratorTier = clamped,
      resourceVaultTier = clamped,
    ).let { it.copy(baseHpMax = it.maxIntegrity(), baseHpCurrent = it.maxIntegrity()) }
    save(
      cur.copy(
        characterTier = clamped,
        weaponTierMap = nextWeapons,
        wonderTierMap = nextWonders,
        gearMap = nextGear,
        equipmentMap = nextEq,
        skillNodes = nextSkills,
        perkRanks = nextPerks,
        protectionRanks = nextProt,
        customPerks = nextCustomPerks,
        customGear = nextCustomGear,
        customEquipment = nextCustomEq,
        blastShardTier = clamped,
        companionBuffTier = clamped,
        blastCoreLevels = nextCores,
        areaStates = nextAreas,
        modeTierMap = nextModeTiers,
        outfitStates = nextOutfits,
        magicState = nextMagic,
        playerBaseState = nextBase,
        vacuumBot = cur.vacuumBot.copy(tier = clamped),
        clan = cur.clan.copy(tier = clamped),
      )
    )
    return clamped
  }

  fun boostAllStars(delta: Int = 10, armoryItemIds: Collection<String> = emptyList()): Int {
    val cur = current
    val nextWeapons = ProgressionCatalog.allWeapons(cur.customWeapons).associate { w ->
      w.id to (cur.weaponStar(w.id) + delta).coerceIn(1, StarRankMeta.MAX_STARS)
    }
    val nextWonders = WonderWeaponKind.entries.associate { w ->
      w.id to (cur.wonderStar(w.id) + delta).coerceIn(1, StarRankMeta.MAX_STARS)
    }
    val nextArmoryStars = allKnownArmoryStarKeys(cur, armoryItemIds).associateWith { id ->
      (cur.armoryItemStar(id) + delta).coerceIn(1, StarRankMeta.MAX_STARS)
    }
    val nextGear = cur.gearMap.mapValues { (_, g) -> g.copy(starLevel = (g.starLevel + delta).coerceIn(1, StarRankMeta.MAX_STARS)) }
    val nextEq = cur.equipmentMap.mapValues { (_, e) -> e.copy(starLevel = (e.starLevel + delta).coerceIn(1, StarRankMeta.MAX_STARS)) }
    val nextOutfits = (MultiverseArsenalCatalog.outfits.map { it.id } + cur.outfitStates.keys).distinct().associateWith { oid ->
      val o = cur.outfitState(oid)
      o.copy(starLevel = (o.starLevel + delta).coerceIn(1, StarRankMeta.MAX_STARS))
    }
    val nextSkills = cur.skillNodes.mapValues { (_, s) -> s.copy(starLevel = (s.starLevel + delta).coerceIn(1, StarRankMeta.MAX_STARS)) }
    val nextPerkStars = MultiverseArsenalCatalog.perkDrinks.associate { it.id to (cur.perkStar(it.id) + delta).coerceIn(1, StarRankMeta.MAX_STARS) }
    val nextProtStars = ProgressionCatalog.protectionUpgrades.associate { it.id to (cur.protectionStar(it.id) + delta).coerceIn(1, StarRankMeta.MAX_STARS) }
    val nextCustomPerks = cur.customPerks.map { it.copy(starLevel = (it.starLevel + delta).coerceIn(1, StarRankMeta.MAX_STARS)) }
    val nextCustomGear = cur.customGear.map { it.copy(starLevel = (it.starLevel + delta).coerceIn(1, StarRankMeta.MAX_STARS)) }
    val nextCustomEq = cur.customEquipment.map { it.copy(starLevel = (it.starLevel + delta).coerceIn(1, StarRankMeta.MAX_STARS)) }
    val allAreaIds = (cur.allPlayableAreas().map { it.id } + cur.areaStates.keys).distinct()
    val nextAreas = allAreaIds.associateWith { areaId ->
      val a = cur.areaState(areaId)
      a.copy(starLevel = (a.starLevel + delta).coerceIn(1, StarRankMeta.MAX_STARS))
    }
    val allModeIds = (UniverseCatalog.modes.map { it.id } + cur.modeTierMap.keys + cur.modeStarMap.keys).distinct()
    val nextModeStars = allModeIds.associateWith { modeId ->
      (cur.modeStar(modeId) + delta).coerceIn(1, StarRankMeta.MAX_STARS)
    }
    save(
      cur.copy(
        characterStarLevel = (cur.characterStarLevel + delta).coerceIn(1, StarRankMeta.MAX_STARS),
        weaponStarMap = nextWeapons,
        wonderStarMap = nextWonders,
        armoryItemStarMap = nextArmoryStars,
        gearMap = nextGear,
        equipmentMap = nextEq,
        outfitStates = nextOutfits,
        skillNodes = nextSkills,
        perkStarMap = nextPerkStars,
        protectionStarMap = nextProtStars,
        customPerks = nextCustomPerks,
        customGear = nextCustomGear,
        customEquipment = nextCustomEq,
        areaStates = nextAreas,
        modeStarMap = nextModeStars,
        vacuumBot = cur.vacuumBot.copy(starLevel = (cur.vacuumBot.starLevel + delta).coerceIn(1, StarRankMeta.MAX_STARS)),
        clan = cur.clan.copy(starLevel = (cur.clan.starLevel + delta).coerceIn(1, StarRankMeta.MAX_STARS)),
      )
    )
    return delta
  }

  fun boostAllLevels(delta: Int = 10): Int {
    val cur = current
    val nextPlayerLvl = (cur.playerLevel + delta).coerceIn(1, ProgressionCatalog.MAX_PLAYER_LEVEL)
    val nextPlayerXp = maxOf(cur.playerXp, ProgressionCatalog.totalXpForLevel(nextPlayerLvl))
    val nextWpnXp = ProgressionCatalog.allWeapons(cur.customWeapons).associate { w ->
      val targetLvl = (cur.weaponLevel(w.id) + delta).coerceIn(1, ProgressionCatalog.MAX_WEAPON_LEVEL)
      w.id to maxOf(cur.weaponXp(w.id), ProgressionCatalog.totalXpForWeaponLevel(targetLvl))
    }
    val nextGear = cur.gearMap.mapValues { (_, g) ->
      val lvl = (g.level + delta).coerceIn(1, ProgressionCatalog.MAX_GEAR_LEVEL)
      g.copy(level = lvl, xp = maxOf(g.xp, ProgressionCatalog.totalXpForGearLevel(lvl)))
    }
    val nextEq = cur.equipmentMap.mapValues { (_, e) ->
      val lvl = (e.level + delta).coerceIn(1, ProgressionCatalog.MAX_EQUIPMENT_LEVEL)
      e.copy(level = lvl, xp = maxOf(e.xp, ProgressionCatalog.totalXpForEquipmentLevel(lvl)))
    }
    val nextOutfits = (MultiverseArsenalCatalog.outfits.map { it.id } + cur.outfitStates.keys).distinct().associateWith { oid ->
      val o = cur.outfitState(oid)
      o.copy(level = (o.level + delta).coerceIn(1, ProgressionCatalog.MAX_OUTFIT_LEVEL))
    }
    val nextCrystals = cur.crystals.map { c -> c.copy(level = (c.level + delta).coerceIn(1, PowerTier.MAX_TIER)) }
    val nextCores = cur.blastCoreLevels.mapValues { (_, l) -> (l + delta).coerceIn(1, ProgressionCatalog.MAX_CORE_LEVEL) }
    val nextNg = (cur.unlockedNgPlusTier + delta).coerceIn(0, NewGamePlusVersion.MAX_NG_PLUS_TIER)
    save(
      cur.copy(
        playerXp = nextPlayerXp,
        weaponXpMap = nextWpnXp,
        gearMap = nextGear,
        equipmentMap = nextEq,
        outfitStates = nextOutfits,
        crystals = nextCrystals,
        blastCoreLevels = nextCores,
        vacuumBot = cur.vacuumBot.copy(level = (cur.vacuumBot.level + delta).coerceIn(1, ProgressionCatalog.MAX_BOT_LEVEL)),
        clan = cur.clan.copy(level = (cur.clan.level + delta).coerceIn(1, ProgressionCatalog.MAX_CLAN_LEVEL)),
        unlockedNgPlusTier = nextNg,
        activeNgPlusTier = maxOf(cur.activeNgPlusTier, nextNg).coerceIn(0, NewGamePlusVersion.MAX_NG_PLUS_TIER),
      )
    )
    return delta
  }

  fun grantDiamondBonus(amount: Long = 50_000L) {
    val cur = current
    save(cur.copy(diamonds = cur.diamonds + amount, gold = cur.gold + amount, tuuCash = cur.tuuCash + (amount * 2)))
  }

  fun setAllStars(
    targetStars: Int = StarRankMeta.MAX_STARS,
    armoryItemIds: Collection<String> = emptyList(),
  ): Int {
    val clamped = targetStars.coerceIn(1, StarRankMeta.MAX_STARS)
    val cur = current
    val nextWeapons = ProgressionCatalog.allWeapons(cur.customWeapons).associate { it.id to clamped }
    val nextWonders = WonderWeaponKind.entries.associate { it.id to clamped }
    val nextArmoryStars = allKnownArmoryStarKeys(cur, armoryItemIds).associateWith { clamped }
    val nextGear = cur.gearMap.mapValues { (_, g) -> g.copy(starLevel = clamped) }
    val nextEq = cur.equipmentMap.mapValues { (_, e) -> e.copy(starLevel = clamped) }
    val nextOutfits = (MultiverseArsenalCatalog.outfits.map { it.id } + cur.outfitStates.keys).distinct().associateWith { oid ->
      cur.outfitState(oid).copy(starLevel = clamped)
    }
    val nextSkills = cur.skillNodes.mapValues { (_, s) -> s.copy(starLevel = clamped) }
    val nextPerkStars = MultiverseArsenalCatalog.perkDrinks.associate { it.id to clamped }
    val nextProtStars = ProgressionCatalog.protectionUpgrades.associate { it.id to clamped }
    val nextCustomPerks = cur.customPerks.map { it.copy(starLevel = clamped) }
    val nextCustomGear = cur.customGear.map { it.copy(starLevel = clamped) }
    val nextCustomEq = cur.customEquipment.map { it.copy(starLevel = clamped) }
    val allAreaIds = (cur.allPlayableAreas().map { it.id } + cur.areaStates.keys).distinct()
    val nextAreas = allAreaIds.associateWith { areaId ->
      cur.areaState(areaId).copy(starLevel = clamped)
    }
    val allModeIds = (UniverseCatalog.modes.map { it.id } + cur.modeTierMap.keys + cur.modeStarMap.keys).distinct()
    val nextModeStars = allModeIds.associateWith { clamped }
    save(
      cur.copy(
        characterStarLevel = clamped,
        weaponStarMap = nextWeapons,
        wonderStarMap = nextWonders,
        armoryItemStarMap = nextArmoryStars,
        gearMap = nextGear,
        equipmentMap = nextEq,
        outfitStates = nextOutfits,
        skillNodes = nextSkills,
        perkStarMap = nextPerkStars,
        protectionStarMap = nextProtStars,
        customPerks = nextCustomPerks,
        customGear = nextCustomGear,
        customEquipment = nextCustomEq,
        areaStates = nextAreas,
        modeStarMap = nextModeStars,
        vacuumBot = cur.vacuumBot.copy(starLevel = clamped),
        clan = cur.clan.copy(starLevel = clamped),
      )
    )
    return clamped
  }

  fun applyProtocolCode(
    rawCode: String,
    armoryItemIds: Collection<String> = emptyList(),
  ): Result<String> {
    val code = rawCode.trim().uppercase(Locale.US)
    return when {
      code.isEmpty() -> Result.failure(IllegalArgumentException("Please enter a protocol code."))
      code == "TIER60" || code == "T60" || code == "MAXTIERS" || code == "MAXTIER" || code == "TRANSCENDENT" -> {
        setAllTiers(PowerTier.MAX_TIER)
        Result.success("PROTOCOL ACCEPTED: All systems elevated to TIER ${PowerTier.MAX_TIER} ETERNIUM APEX!")
      }
      code == "TIER50" || code == "T50" -> {
        setAllTiers(50)
        Result.success("PROTOCOL ACCEPTED: All systems set to TIER 50 (Omniversal)!")
      }
      code == "BOOST10" || code == "+10ALL" || code == "+10TIERS" || code == "TIER+10" || code == "STARS+10" -> {
        boostAllTiers(10)
        boostAllStars(10, armoryItemIds)
        boostAllLevels(10)
        Result.success("PROTOCOL ACCEPTED: +10 Levels, +10 Tiers & +10★ applied across ALL systems!")
      }
      code == "TIER25" || code == "MULTIVERSAL" -> {
        setAllTiers(25)
        Result.success("PROTOCOL ACCEPTED: All systems elevated to TIER 25 MULTIVERSAL!")
      }
      code == "TIER20" || code == "T20" -> {
        setAllTiers(20)
        Result.success("PROTOCOL ACCEPTED: All systems set to TIER 20 (Infinity)!")
      }
      code == "TIER15" || code == "T15" -> {
        setAllTiers(15)
        Result.success("PROTOCOL ACCEPTED: All systems set to TIER 15 (Apex)!")
      }
      code == "TIER10" || code == "T10" -> {
        setAllTiers(10)
        Result.success("PROTOCOL ACCEPTED: All systems set to TIER 10 (Omega)!")
      }
      code == "BOOST5" || code == "+5TIERS" || code == "TIER+5" -> {
        boostAllTiers(5)
        Result.success("PROTOCOL ACCEPTED: +5 Tiers applied to all weapons, gear, character & systems!")
      }
      code == "STARS25" || code == "MAXSTARS" || code == "25STARS" -> {
        setAllStars(StarRankMeta.MAX_STARS, armoryItemIds)
        Result.success("PROTOCOL ACCEPTED: ${StarRankMeta.MAX_STARS}★ Eternium Sovereign star ranks unlocked across all equipment!")
      }
      code == "STARS15" || code == "15STARS" -> {
        setAllStars(15, armoryItemIds)
        Result.success("PROTOCOL ACCEPTED: 15★ Omniverse Sovereign star ranks unlocked across all equipment!")
      }
      code == "STARS10" || code == "10STARS" -> {
        setAllStars(10, armoryItemIds)
        Result.success("PROTOCOL ACCEPTED: 10★ Omniverse Nova star ranks unlocked across all equipment!")
      }
      code == "ESSENCE999" || code == "MAXESSENCE" || code == "RICH" -> {
        val cur = current
        val nextShards = cur.systemShards.toMutableMap().apply {
          MultiverseSystemsCatalog.systemShardDomains.forEach { (id, _) ->
            this[id] = (this[id] ?: 0L) + 120L
          }
        }
        val nextRes = cur.environmentalResources.toMutableMap().apply {
          MultiverseSystemsCatalog.environmentalResourceSpecs.forEach { (id, _) ->
            this[id] = (this[id] ?: 0L) + 250L
          }
        }
        save(
          cur.copy(
            redEssence = cur.redEssence + 100_000L,
            yellowEssence = cur.yellowEssence + 100_000L,
            greenEssence = cur.greenEssence + 100_000L,
            combinedEssence = cur.combinedEssence + 50_000L,
            blastShards = cur.blastShards + 500L,
            gold = cur.gold + 50_000L,
            diamonds = cur.diamonds + 5_000L,
            tuuCash = cur.tuuCash + 150_000L,
            systemShards = nextShards,
            environmentalResources = nextRes,
          )
        )
        Result.success("PROTOCOL ACCEPTED: +100k Essence, +50k Gold, +5k Diamonds, +150k TUUCash, +120 All Shards & Resources!")
      }
      code == "UNLOCKALL" || code == "ALLUNLOCKED" || code == "GODMODE" -> {
        setAllTiers(PowerTier.MAX_TIER)
        setAllStars(StarRankMeta.MAX_STARS, armoryItemIds)
        boostAllLevels(10)
        val cur = current
        val allSpells = MultiverseSystemsCatalog.magicSpells.map { it.id }.toSet()
        save(
          cur.copy(
            unlockedNgPlusTier = NewGamePlusVersion.MAX_NG_PLUS_TIER,
            activeNgPlusTier = NewGamePlusVersion.MAX_NG_PLUS_TIER,
            magicState = cur.magicState.copy(unlockedSpells = allSpells, manaMax = 240f, manaCurrent = 240f),
          )
        )
        Result.success("PROTOCOL ACCEPTED: Full Universe Unlocked (T${PowerTier.MAX_TIER}, ${StarRankMeta.MAX_STARS}★, NG+${NewGamePlusVersion.MAX_NG_PLUS_TIER}, All Spells & Skills)!")
      }
      code == "BO3PERKS" || code == "TREYARCH" || code == "ZOMBIES" -> {
        boostAllTiers(10)
        Result.success("BO3 MOD TOOLS OVERDRIVE: +10 Tiers applied & Treyarch perks supercharged!")
      }
      else -> Result.failure(IllegalArgumentException("Unknown protocol code '$code'. Try TIER60, STARS25, BOOST10, ESSENCE999, or UNLOCKALL."))
    }
  }

  fun activateOrUnlockPassVersion(version: BattlePassVersion, currentBlueEssence: Long): Result<Long> {
    val cur = current
    if (cur.unlockedPassVersions.contains(version)) {
      save(cur.copy(activePassVersion = version))
      return Result.success(0L)
    }
    if (currentBlueEssence < version.unlockCostEssence) {
      return Result.failure(IllegalStateException("Need ${version.unlockCostEssence} Blue Essence to unlock ${version.title}"))
    }
    val next = cur.copy(
      activePassVersion = version,
      unlockedPassVersions = cur.unlockedPassVersions + version,
      totalBlueEssenceSpent = cur.totalBlueEssenceSpent + version.unlockCostEssence,
    )
    save(next)
    return Result.success(version.unlockCostEssence)
  }

  fun claimPassTier(version: BattlePassVersion, tier: Int): Result<Long> {
    val cur = current
    if (!cur.unlockedPassVersions.contains(version)) {
      return Result.failure(IllegalStateException("Unlock ${version.title} first"))
    }
    val cycle = cur.battlePassPrestige
    val tiers = ProgressionCatalog.tiersForVersion(version, cycle)
    val rewardSpec = tiers.firstOrNull { it.tier == tier }
      ?: return Result.failure(IllegalStateException("Tier not found"))
    if (cur.battlePassXp < rewardSpec.bpXpRequired) {
      return Result.failure(IllegalStateException("Need ${rewardSpec.bpXpRequired} Battle Pass XP"))
    }
    val key = "${version.id}_c${cycle}_t$tier"
    if (cur.claimedPassTiers.contains(key)) {
      return Result.failure(IllegalStateException("Tier already claimed"))
    }
    val bonusWpnXp = if (rewardSpec.unlockTag == "WPN XP" || rewardSpec.unlockTag == "BLUEPRINT") 800L else 0L
    val equippedWpn = cur.weaponCustom.equippedWeaponId
    val nextWpnMap = if (bonusWpnXp > 0L) {
      cur.weaponXpMap + (equippedWpn to ((cur.weaponXpMap[equippedWpn] ?: 0L) + bonusWpnXp))
    } else cur.weaponXpMap

    val nextClaimed = cur.claimedPassTiers + key
    val nextCycle = if (tier >= 6 + cycle * 6 && cur.battlePassPrestige < ProgressionCatalog.MAX_PRESTIGE) {
      cur.copy(battlePassPrestige = cur.battlePassPrestige + 1, bpCycleCount = cur.bpCycleCount + 1)
    } else cur

    save(
      nextCycle.copy(
        claimedPassTiers = nextClaimed,
        weaponXpMap = nextWpnMap,
      )
    )
    return Result.success(rewardSpec.rewardEssence)
  }

  fun claimChallengeReward(challengeId: String): Result<Triple<Long, Long, Long>> {
    val cur = current
    val spec = ProgressionCatalog.challenges.firstOrNull { it.id == challengeId }
      ?: return Result.failure(IllegalStateException("Unknown challenge"))
    val progress = cur.challengeProgress[challengeId] ?: 0L
    if (progress < spec.target) {
      return Result.failure(IllegalStateException("Challenge not yet completed"))
    }
    val key = "$challengeId:${cur.challengePrestige}"
    if (cur.claimedChallenges.contains(key)) {
      return Result.failure(IllegalStateException("Reward already claimed"))
    }
    val bpMult = if (cur.activePassVersion == BattlePassVersion.BLACKCELL_OVERDRIVE) 1.30f else 1.0f
    val prestigeMultiplier = 1f + (cur.challengePrestige * 0.2f)
    val bpGain = (spec.rewardBpXp * bpMult * prestigeMultiplier).toLong()
    val essenceGain = (spec.rewardEssence * prestigeMultiplier).toLong()
    val pxpGain = (spec.rewardPlayerXp * prestigeMultiplier).toLong()

    val nextProgress = cur.challengeProgress - challengeId
    val nextClaimed = cur.claimedChallenges + key
    val nextPrestige = if (cur.claimedChallenges.size >= 15 && cur.challengePrestige < ProgressionCatalog.MAX_PRESTIGE) cur.challengePrestige + 1 else cur.challengePrestige

    save(
      cur.copy(
        playerXp = cur.playerXp + pxpGain,
        battlePassXp = cur.battlePassXp + bpGain,
        challengeProgress = nextProgress,
        claimedChallenges = nextClaimed,
        challengePrestige = nextPrestige,
      )
    )
    return Result.success(Triple(essenceGain, pxpGain, bpGain))
  }

  fun resetCompletedChallengesForReplay() {
    val cur = current
    save(
      cur.copy(
        challengeProgress = emptyMap(),
        claimedChallenges = emptySet(),
      )
    )
  }

  // MULTIVERSE SYSTEMS: CRYSTALS, MAGIC, PLAYER BASE, TRANSMUTER & MULTI-CURRENCY UPGRADES

  fun selectMagicSpell(spellId: String) {
    val cur = current
    if (spellId !in cur.magicState.unlockedSpells) return
    save(cur.copy(magicState = cur.magicState.copy(activeSpellId = spellId)))
  }

  fun unlockOrUpgradeMagicSpell(spellId: String, currency: String = "gold"): Result<String> {
    val cur = current
    val spec = MultiverseSystemsCatalog.magicSpells.firstOrNull { it.id == spellId }
      ?: return Result.failure(IllegalStateException("Spell not found"))
    val magic = cur.magicState
    if (spellId !in magic.unlockedSpells) {
      return when (currency) {
        "diamonds" -> {
          val cost = spec.unlockDiamonds.coerceAtLeast(40L)
          if (cur.diamonds < cost) return Result.failure(IllegalStateException("Need $cost Diamonds"))
          save(
            cur.copy(
              diamonds = cur.diamonds - cost,
              magicState = magic.copy(
                unlockedSpells = magic.unlockedSpells + spellId,
                activeSpellId = spellId,
                spellTiers = magic.spellTiers + (spellId to 1),
              ),
            )
          )
          Result.success("Unlocked ${spec.name} (-$cost Diamonds)")
        }
        "shards" -> {
          val cost = 10L
          val have = cur.systemShardCount("magic")
          if (have < cost) return Result.failure(IllegalStateException("Need $cost Magic Shards"))
          save(
            cur.copy(
              systemShards = cur.systemShards + ("magic" to (have - cost)),
              magicState = magic.copy(
                unlockedSpells = magic.unlockedSpells + spellId,
                activeSpellId = spellId,
                spellTiers = magic.spellTiers + (spellId to 1),
              ),
            )
          )
          Result.success("Unlocked ${spec.name} (-$cost Magic Shards)")
        }
        else -> {
          val cost = spec.unlockGold.coerceAtLeast(300L)
          if (cur.gold < cost) return Result.failure(IllegalStateException("Need $cost Gold"))
          save(
            cur.copy(
              gold = cur.gold - cost,
              magicState = magic.copy(
                unlockedSpells = magic.unlockedSpells + spellId,
                activeSpellId = spellId,
                spellTiers = magic.spellTiers + (spellId to 1),
              ),
            )
          )
          Result.success("Unlocked ${spec.name} (-$cost Gold)")
        }
      }
    }
    val curTier = magic.spellTier(spellId)
    if (curTier >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("${spec.name} already at Max Tier ${PowerTier.MAX_TIER}"))
    }
    return upgradeMagicSubTarget(
      curTier = curTier,
      currency = currency,
      label = spec.name,
    ) { updatedCur ->
      updatedCur.copy(
        magicState = updatedCur.magicState.copy(
          spellTiers = updatedCur.magicState.spellTiers + (spellId to (curTier + 1)),
          activeSpellId = spellId,
        )
      )
    }
  }

  fun upgradeMagicTier(currency: String = "gold"): Result<String> {
    val cur = current
    val curTier = cur.magicState.magicTier.coerceIn(1, PowerTier.MAX_TIER)
    if (curTier >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("Arcane Conduit already at Max Tier ${PowerTier.MAX_TIER}"))
    }
    return upgradeMagicSubTarget(curTier, currency, "Arcane Conduit") { updatedCur ->
      val nextTier = (curTier + 1).coerceAtMost(PowerTier.MAX_TIER)
      val nextManaMax = (100f + nextTier * 4f).coerceAtMost(320f)
      updatedCur.copy(
        magicState = updatedCur.magicState.copy(
          magicTier = nextTier,
          manaMax = nextManaMax,
          manaCurrent = nextManaMax,
        )
      )
    }
  }

  private fun upgradeMagicSubTarget(
    curTier: Int,
    currency: String,
    label: String,
    mutate: (OperatorProgressionState) -> OperatorProgressionState,
  ): Result<String> {
    val cur = current
    return when (currency) {
      "diamonds" -> {
        val cost = UpgradeCosts.magicTierCostDiamonds(curTier)
        if (cur.diamonds < cost) return Result.failure(IllegalStateException("Need $cost Diamonds"))
        save(mutate(cur.copy(diamonds = cur.diamonds - cost)))
        Result.success("$label upgraded to T${curTier + 1}/${PowerTier.MAX_TIER} (-$cost Diamonds)")
      }
      "shards" -> {
        val cost = UpgradeCosts.systemShardCost(curTier)
        val have = cur.systemShardCount("magic")
        if (have < cost) return Result.failure(IllegalStateException("Need $cost Arcane Magic Shards"))
        save(mutate(cur.copy(systemShards = cur.systemShards + ("magic" to (have - cost)))))
        Result.success("$label upgraded to T${curTier + 1}/${PowerTier.MAX_TIER} (-$cost Magic Shards)")
      }
      "energy_cell" -> {
        val cost = 6L + curTier
        val have = cur.resourceCount("energy_cell")
        if (have < cost) return Result.failure(IllegalStateException("Need $cost Fusion Energy Cells"))
        save(mutate(cur.copy(environmentalResources = cur.environmentalResources + ("energy_cell" to (have - cost)))))
        Result.success("$label upgraded to T${curTier + 1}/${PowerTier.MAX_TIER} (-$cost Energy Cells)")
      }
      else -> {
        val cost = UpgradeCosts.magicTierCostGold(curTier)
        if (cur.gold < cost) return Result.failure(IllegalStateException("Need $cost Gold"))
        save(mutate(cur.copy(gold = cur.gold - cost)))
        Result.success("$label upgraded to T${curTier + 1}/${PowerTier.MAX_TIER} (-$cost Gold)")
      }
    }
  }

  fun adjustMagicDifficulty(delta: Float): Float {
    val cur = current
    val nextScale = (cur.magicState.difficultyScalingFactor + delta).coerceIn(1.0f, 3.5f)
    save(cur.copy(magicState = cur.magicState.copy(difficultyScalingFactor = nextScale)))
    return nextScale
  }

  fun toggleMagicOvercharge(): Boolean {
    val cur = current
    val nextOn = !cur.magicState.overchargeDifficultyEnabled
    save(cur.copy(magicState = cur.magicState.copy(overchargeDifficultyEnabled = nextOn)))
    return nextOn
  }

  fun craftCrystal(templateId: String): Result<CrystalItem> {
    val cur = current
    val tpl = MultiverseSystemsCatalog.craftableCrystalTemplates.firstOrNull { it.id == templateId }
      ?: return Result.failure(IllegalStateException("Crystal template not found"))
    val fragCost = 20L
    val kyberCost = 6L
    val goldCost = 500L
    if (cur.resourceCount("crystal_shard") < fragCost) {
      return Result.failure(IllegalStateException("Need $fragCost Prism Crystal Frags"))
    }
    if (cur.resourceCount("kyber_fragment") < kyberCost) {
      return Result.failure(IllegalStateException("Need $kyberCost Kyber Force Fragments"))
    }
    if (cur.gold < goldCost) {
      return Result.failure(IllegalStateException("Need $goldCost Gold"))
    }
    val existingCount = cur.crystals.count { it.id.startsWith(tpl.id) }
    val crafted = tpl.copy(
      id = if (existingCount == 0 && cur.crystals.none { it.id == tpl.id }) tpl.id else "${tpl.id}_${System.currentTimeMillis() % 10000}",
      level = 1,
      socketedSlot = null,
    )
    save(
      cur.copy(
        gold = cur.gold - goldCost,
        environmentalResources = cur.environmentalResources +
          ("crystal_shard" to (cur.resourceCount("crystal_shard") - fragCost)) +
          ("kyber_fragment" to (cur.resourceCount("kyber_fragment") - kyberCost)),
        crystals = cur.crystals + crafted,
      )
    )
    return Result.success(crafted)
  }

  fun upgradeCrystal(crystalId: String, currency: String = "gold"): Result<CrystalItem> {
    val cur = current
    val item = cur.crystals.firstOrNull { it.id == crystalId }
      ?: return Result.failure(IllegalStateException("Crystal not found"))
    if (item.level >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("${item.name} is already Max Level ${PowerTier.MAX_TIER}"))
    }
    val nextLevel = item.level + 1
    val updatedCrystal = item.copy(level = nextLevel)
    when (currency) {
      "diamonds" -> {
        val cost = UpgradeCosts.crystalUpgradeDiamonds(item.level)
        if (cur.diamonds < cost) return Result.failure(IllegalStateException("Need $cost Diamonds"))
        save(cur.copy(diamonds = cur.diamonds - cost, crystals = cur.crystals.map { if (it.id == crystalId) updatedCrystal else it }))
      }
      "shards" -> {
        val cost = UpgradeCosts.systemShardCost(item.level)
        val have = cur.systemShardCount("crystal")
        if (have < cost) return Result.failure(IllegalStateException("Need $cost Crystal Shards"))
        save(
          cur.copy(
            systemShards = cur.systemShards + ("crystal" to (have - cost)),
            crystals = cur.crystals.map { if (it.id == crystalId) updatedCrystal else it },
          )
        )
      }
      "crystal_shard" -> {
        val cost = 10L + item.level * 2L
        val have = cur.resourceCount("crystal_shard")
        if (have < cost) return Result.failure(IllegalStateException("Need $cost Prism Crystal Frags"))
        save(
          cur.copy(
            environmentalResources = cur.environmentalResources + ("crystal_shard" to (have - cost)),
            crystals = cur.crystals.map { if (it.id == crystalId) updatedCrystal else it },
          )
        )
      }
      else -> {
        val cost = UpgradeCosts.crystalUpgradeGold(item.level)
        if (cur.gold < cost) return Result.failure(IllegalStateException("Need $cost Gold"))
        save(cur.copy(gold = cur.gold - cost, crystals = cur.crystals.map { if (it.id == crystalId) updatedCrystal else it }))
      }
    }
    return Result.success(updatedCrystal)
  }

  fun socketCrystal(crystalId: String, slot: String?): CrystalItem? {
    val cur = current
    val target = cur.crystals.firstOrNull { it.id == crystalId } ?: return null
    val nextCrystals = cur.crystals.map { c ->
      when {
        c.id == crystalId -> c.copy(socketedSlot = slot)
        slot != null && c.socketedSlot == slot -> c.copy(socketedSlot = null)
        else -> c
      }
    }
    save(cur.copy(crystals = nextCrystals))
    return nextCrystals.firstOrNull { it.id == crystalId }
  }

  fun customizePlayerBase(
    baseName: String? = null,
    layoutTheme: String? = null,
    primaryHex: Long? = null,
  ): PlayerBaseState {
    val cur = current
    val b = cur.playerBaseState
    val next = b.copy(
      baseName = baseName?.trim()?.take(40)?.ifBlank { b.baseName } ?: b.baseName,
      layoutTheme = layoutTheme ?: b.layoutTheme,
      primaryHex = primaryHex ?: b.primaryHex,
    )
    save(cur.copy(playerBaseState = next))
    return next
  }

  fun upgradePlayerBaseModule(module: String, currency: String = "gold"): Result<String> {
    val cur = current
    val b = cur.playerBaseState
    val curTier = when (module) {
      "wall" -> b.wallTier
      "turret" -> b.turretTier
      "shield" -> b.shieldGeneratorTier
      else -> b.resourceVaultTier
    }
    if (curTier >= PowerTier.MAX_TIER) {
      return Result.failure(IllegalStateException("Base $module already at Max Tier ${PowerTier.MAX_TIER}"))
    }
    val nextTier = curTier + 1
    fun applyBase(s: OperatorProgressionState): OperatorProgressionState {
      val updatedBase = when (module) {
        "wall" -> b.copy(wallTier = nextTier)
        "turret" -> b.copy(turretTier = nextTier)
        "shield" -> b.copy(shieldGeneratorTier = nextTier)
        else -> b.copy(resourceVaultTier = nextTier)
      }.let { it.copy(baseHpMax = it.maxIntegrity(), baseHpCurrent = it.maxIntegrity()) }
      return s.copy(playerBaseState = updatedBase)
    }
    return when (currency) {
      "tuu_cash" -> {
        val cost = UpgradeCosts.baseModuleTuuCash(curTier)
        if (cur.tuuCash < cost) return Result.failure(IllegalStateException("Need $cost TUUCash"))
        save(applyBase(cur.copy(tuuCash = cur.tuuCash - cost)))
        Result.success("Base $module upgraded to Tier $nextTier/${PowerTier.MAX_TIER} (-$cost TUUCash)")
      }
      "shards" -> {
        val cost = UpgradeCosts.systemShardCost(curTier)
        val have = cur.systemShardCount("base")
        if (have < cost) return Result.failure(IllegalStateException("Need $cost Citadel Base Shards"))
        save(applyBase(cur.copy(systemShards = cur.systemShards + ("base" to (have - cost)))))
        Result.success("Base $module upgraded to Tier $nextTier/${PowerTier.MAX_TIER} (-$cost Base Shards)")
      }
      "metal_scrap" -> {
        val cost = 18L + (curTier * 5L)
        val have = cur.resourceCount("metal_scrap")
        if (have < cost) return Result.failure(IllegalStateException("Need $cost Beskar / Alloy Scrap"))
        save(applyBase(cur.copy(environmentalResources = cur.environmentalResources + ("metal_scrap" to (have - cost)))))
        Result.success("Base $module upgraded to Tier $nextTier/${PowerTier.MAX_TIER} (-$cost Alloy Scrap)")
      }
      else -> {
        val cost = UpgradeCosts.baseModuleGold(curTier)
        if (cur.gold < cost) return Result.failure(IllegalStateException("Need $cost Gold"))
        save(applyBase(cur.copy(gold = cur.gold - cost)))
        Result.success("Base $module upgraded to Tier $nextTier/${PowerTier.MAX_TIER} (-$cost Gold)")
      }
    }
  }

  fun collectBaseVault(): Result<Triple<Long, Long, Long>> {
    val cur = current
    val b = cur.playerBaseState
    if (b.storedGold <= 0L && b.storedDiamonds <= 0L && b.storedTuuCash <= 0L) {
      return Result.failure(IllegalStateException("Base Vault is currently empty — play matches to generate income!"))
    }
    val g = b.storedGold
    val d = b.storedDiamonds
    val c = b.storedTuuCash
    save(
      cur.copy(
        gold = cur.gold + g,
        diamonds = cur.diamonds + d,
        tuuCash = cur.tuuCash + c,
        playerBaseState = b.copy(storedGold = 0L, storedDiamonds = 0L, storedTuuCash = 0L),
      )
    )
    return Result.success(Triple(g, d, c))
  }

  fun triggerOrResolveBaseAttack(defendNow: Boolean): Result<String> {
    val cur = current
    val b = cur.playerBaseState
    if (!b.underAttack && !defendNow) {
      save(cur.copy(playerBaseState = b.copy(underAttack = true, attackWaveCountdownSec = 0)))
      return Result.success("ALERT: Hostile strike force is sieging ${b.baseName}!")
    }
    val defensePower = (b.wallTier * 22) + (b.turretTier * 30) + (b.shieldGeneratorTier * 25) + (cur.characterTier * 8)
    val rewardGold = 350L + (b.resourceVaultTier * 90L)
    val rewardDiamonds = 45L + (b.shieldGeneratorTier * 8L)
    val rewardCash = 900L + (b.turretTier * 180L)
    val baseShardsGain = 8L
    val maxHp = b.maxIntegrity()
    save(
      cur.copy(
        gold = cur.gold + rewardGold,
        diamonds = cur.diamonds + rewardDiamonds,
        tuuCash = cur.tuuCash + rewardCash,
        systemShards = cur.systemShards + ("base" to (cur.systemShardCount("base") + baseShardsGain)),
        playerBaseState = b.copy(
          underAttack = false,
          attackWaveCountdownSec = 240,
          defenseSuccessCount = b.defenseSuccessCount + 1,
          baseHpMax = maxHp,
          baseHpCurrent = maxHp,
        ),
      )
    )
    return Result.success(
      "Base Defended (Power $defensePower)! +$rewardGold Gold, +$rewardDiamonds Diamonds, +$rewardCash TUUCash & +$baseShardsGain Base Shards!"
    )
  }

  fun convertCurrencyOrResource(actionId: String): Result<String> {
    val cur = current
    return when (actionId) {
      "tuucash_to_gold" -> {
        if (cur.tuuCash < 1500L) return Result.failure(IllegalStateException("Need 1,500 TUUCash"))
        save(cur.copy(tuuCash = cur.tuuCash - 1500L, gold = cur.gold + 600L))
        Result.success("Exchanged 1,500 TUUCash -> +600 Gold")
      }
      "gold_to_diamonds" -> {
        if (cur.gold < 800L) return Result.failure(IllegalStateException("Need 800 Gold"))
        save(cur.copy(gold = cur.gold - 800L, diamonds = cur.diamonds + 120L))
        Result.success("Transmuted 800 Gold -> +120 Diamonds")
      }
      "diamonds_to_all_shards" -> {
        if (cur.diamonds < 100L) return Result.failure(IllegalStateException("Need 100 Diamonds"))
        val nextShards = cur.systemShards.toMutableMap()
        MultiverseSystemsCatalog.systemShardDomains.forEach { (id, _) ->
          nextShards[id] = (nextShards[id] ?: 0L) + 12L
        }
        save(cur.copy(diamonds = cur.diamonds - 100L, systemShards = nextShards))
        Result.success("Refined 100 Diamonds -> +12 Shards across ALL 15 systems!")
      }
      "resources_to_combined_essence" -> {
        val frag = cur.resourceCount("crystal_shard")
        val scrap = cur.resourceCount("metal_scrap")
        val cell = cur.resourceCount("energy_cell")
        if (frag < 15L || scrap < 20L || cell < 10L) {
          return Result.failure(IllegalStateException("Need 15 Crystal Frags, 20 Alloy Scrap & 10 Energy Cells"))
        }
        save(
          cur.copy(
            environmentalResources = cur.environmentalResources +
              ("crystal_shard" to (frag - 15L)) +
              ("metal_scrap" to (scrap - 20L)) +
              ("energy_cell" to (cell - 10L)),
            combinedEssence = cur.combinedEssence + 150L,
            blastShards = cur.blastShards + 25L,
          )
        )
        Result.success("Synthesized Env Resources -> +150 Combined Essence & +25 Blast Shards!")
      }
      "tuucash_to_resources" -> {
        if (cur.tuuCash < 1200L) return Result.failure(IllegalStateException("Need 1,200 TUUCash"))
        val nextRes = cur.environmentalResources.toMutableMap()
        MultiverseSystemsCatalog.environmentalResourceSpecs.forEach { (id, _) ->
          nextRes[id] = (nextRes[id] ?: 0L) + 20L
        }
        save(cur.copy(tuuCash = cur.tuuCash - 1200L, environmentalResources = nextRes))
        Result.success("Procured Supply Crate (-1,200 TUUCash) -> +20 of EVERY Environmental Resource!")
      }
      "tuucash_bank_exchange" -> {
        if (cur.tuuCash < 2500L) return Result.failure(IllegalStateException("Need 2,500 TUUCash"))
        save(
          cur.copy(
            tuuCash = cur.tuuCash - 2500L,
            gold = cur.gold + 1500L,
            diamonds = cur.diamonds + 150L,
          )
        )
        Result.success("Bank Exchange (-2,500 TUUCash) -> +1,500 Gold & +150 Diamonds!")
      }
      "diamonds_to_tuucash_shards" -> {
        if (cur.diamonds < 300L) return Result.failure(IllegalStateException("Need 300 Diamonds"))
        save(
          cur.copy(
            diamonds = cur.diamonds - 300L,
            tuuCash = cur.tuuCash + 12_000L,
            blastShards = cur.blastShards + 40L,
          )
        )
        Result.success("Vault Liquidation (-300 Diamonds) -> +12,000 TUUCash & +40 Blast Shards!")
      }
      "diamonds_magic_overclock" -> {
        if (cur.diamonds < 180L) return Result.failure(IllegalStateException("Need 180 Diamonds"))
        val m = cur.magicState
        val nextTier = (m.magicTier + 1).coerceAtMost(PowerTier.MAX_TIER)
        val nextManaMax = (m.manaMax + 20f).coerceAtMost(450f)
        val boostedSpells = m.spellTiers.toMutableMap()
        m.unlockedSpells.forEach { spId ->
          val curSp = boostedSpells[spId] ?: 1
          boostedSpells[spId] = (curSp + 1).coerceAtMost(PowerTier.MAX_TIER)
        }
        save(
          cur.copy(
            diamonds = cur.diamonds - 180L,
            magicState = m.copy(
              magicTier = nextTier,
              manaMax = nextManaMax,
              manaCurrent = nextManaMax,
              spellTiers = boostedSpells,
            ),
          )
        )
        Result.success("Diamond Magic Overclock (-180 D) -> Magic T$nextTier, Max Mana ${nextManaMax.toInt()}, All Unlocked Spells +1 Tier!")
      }
      "diamonds_fortify_turrets" -> {
        if (cur.diamonds < 200L) return Result.failure(IllegalStateException("Need 200 Diamonds"))
        val b = cur.playerBaseState
        val nextTurret = (b.turretTier + 2).coerceAtMost(PowerTier.MAX_TIER)
        val nextWall = (b.wallTier + 2).coerceAtMost(PowerTier.MAX_TIER)
        val nextShield = (b.shieldGeneratorTier + 1).coerceAtMost(PowerTier.MAX_TIER)
        val updatedBase = b.copy(
          turretTier = nextTurret,
          wallTier = nextWall,
          shieldGeneratorTier = nextShield,
        ).let { it.copy(baseHpMax = it.maxIntegrity(), baseHpCurrent = it.maxIntegrity()) }
        save(cur.copy(diamonds = cur.diamonds - 200L, playerBaseState = updatedBase))
        Result.success("Fortified Citadel (-200 Diamonds) -> Turrets T$nextTurret, Walls T$nextWall, Shield Gen T$nextShield!")
      }
      "tuucash_deploy_turret_fleet" -> {
        if (cur.tuuCash < 3000L) return Result.failure(IllegalStateException("Need 3,000 TUUCash"))
        val b = cur.playerBaseState
        val nextTurret = (b.turretTier + 2).coerceAtMost(PowerTier.MAX_TIER)
        val nextVault = (b.resourceVaultTier + 2).coerceAtMost(PowerTier.MAX_TIER)
        val updatedBase = b.copy(
          turretTier = nextTurret,
          resourceVaultTier = nextVault,
          storedGold = b.storedGold + 300L,
        ).let { it.copy(baseHpMax = it.maxIntegrity(), baseHpCurrent = it.maxIntegrity()) }
        save(cur.copy(tuuCash = cur.tuuCash - 3000L, playerBaseState = updatedBase))
        Result.success("Deployed TUUCash Turret Fleet (-3,000 TUUCash) -> Turrets T$nextTurret, Vault T$nextVault & +300 Vault Gold!")
      }
      "magic_arcane_infusion" -> {
        if (cur.tuuCash < 1000L || cur.diamonds < 100L) {
          return Result.failure(IllegalStateException("Need 1,000 TUUCash & 100 Diamonds"))
        }
        val nextCells = cur.resourceCount("energy_cell") + 25L
        val nextMagicShards = cur.systemShardCount("magic") + 15L
        save(
          cur.copy(
            tuuCash = cur.tuuCash - 1000L,
            diamonds = cur.diamonds - 100L,
            combinedEssence = cur.combinedEssence + 50L,
            systemShards = cur.systemShards + ("magic" to nextMagicShards),
            environmentalResources = cur.environmentalResources + ("energy_cell" to nextCells),
          )
        )
        Result.success("Arcane Infusion Complete -> +50 Combined Essence, +15 Magic Shards & +25 Energy Cells!")
      }
      else -> Result.failure(IllegalArgumentException("Unknown exchange action"))
    }
  }

  /**
   * Alternate currency upgrade path across every system using Shards, Gold, Diamonds, TUUCash, or Colored Essence.
   */
  fun upgradeTargetWithCurrency(
    targetType: String,
    targetId: String,
    currency: String,
  ): Result<String> {
    val cur = current
    val (currentLevel, maxLevel, shardDomain) = when (targetType) {
      "character" -> Triple(cur.characterTier, PowerTier.MAX_TIER, "outfit")
      "weapon" -> Triple(cur.weaponTier(targetId), PowerTier.MAX_TIER, "weapon")
      "wonder" -> Triple(cur.wonderTier(targetId), PowerTier.MAX_TIER, "wonder")
      "gear" -> Triple((cur.gearMap[targetId]?.tier ?: 1), PowerTier.MAX_TIER, "gear")
      "equipment" -> Triple(cur.equipmentState(targetId).tier, PowerTier.MAX_TIER, "equipment")
      "perk" -> Triple(cur.perkRank(targetId), ProgressionCatalog.MAX_PERK_RANK, "perk")
      "protection" -> Triple(cur.protectionRank(targetId), ProgressionCatalog.MAX_PROT_RANK, "protection")
      "skill" -> Triple((cur.skillNodes[targetId]?.tier ?: 0), PowerTier.MAX_TIER, "skill")
      "bot" -> Triple(cur.vacuumBot.tier, PowerTier.MAX_TIER, "bot")
      "blast_shard" -> Triple(cur.blastShardTier, PowerTier.MAX_TIER, "core")
      "companion" -> Triple(cur.companionBuffTier, PowerTier.MAX_TIER, "outfit")
      "mode" -> Triple(cur.modeTier(targetId), PowerTier.MAX_TIER, "mode")
      "area" -> Triple(cur.areaState(targetId).powerGridTier, PowerTier.MAX_TIER, "area")
      else -> return Result.failure(IllegalArgumentException("Unknown upgrade target $targetType"))
    }
    if (currentLevel >= maxLevel) {
      return Result.failure(IllegalStateException("$targetType already at max Tier $maxLevel"))
    }

    val nextStateBase: OperatorProgressionState
    val costLabel: String
    when (currency) {
      "shards" -> {
        val cost = UpgradeCosts.systemShardCost(currentLevel)
        val have = cur.systemShardCount(shardDomain)
        if (have < cost) return Result.failure(IllegalStateException("Need $cost $shardDomain Shards (have $have)"))
        nextStateBase = cur.copy(systemShards = cur.systemShards + (shardDomain to (have - cost)))
        costLabel = "-$cost $shardDomain Shards"
      }
      "gold" -> {
        val cost = UpgradeCosts.goldCostForTierUpgrade(currentLevel)
        if (cur.gold < cost) return Result.failure(IllegalStateException("Need $cost Gold"))
        nextStateBase = cur.copy(gold = cur.gold - cost)
        costLabel = "-$cost Gold"
      }
      "diamonds" -> {
        val cost = UpgradeCosts.diamondCostForTierUpgrade(currentLevel)
        if (cur.diamonds < cost) return Result.failure(IllegalStateException("Need $cost Diamonds"))
        nextStateBase = cur.copy(diamonds = cur.diamonds - cost)
        costLabel = "-$cost Diamonds"
      }
      "tuu_cash" -> {
        val cost = UpgradeCosts.tuuCashCostForTierUpgrade(currentLevel)
        if (cur.tuuCash < cost) return Result.failure(IllegalStateException("Need $cost TUUCash"))
        nextStateBase = cur.copy(tuuCash = cur.tuuCash - cost)
        costLabel = "-$cost TUUCash"
      }
      "combined_essence" -> {
        val cost = 20L + currentLevel * 12L
        if (cur.combinedEssence < cost) return Result.failure(IllegalStateException("Need $cost Combined Essence"))
        nextStateBase = cur.copy(combinedEssence = cur.combinedEssence - cost)
        costLabel = "-$cost Combined Essence"
      }
      else -> return Result.failure(IllegalArgumentException("Unsupported payment currency: $currency"))
    }

    val nextTier = (currentLevel + 1).coerceAtMost(maxLevel)
    val finalState = when (targetType) {
      "character" -> nextStateBase.copy(characterTier = nextTier)
      "weapon" -> nextStateBase.copy(weaponTierMap = nextStateBase.weaponTierMap + (targetId to nextTier))
      "wonder" -> nextStateBase.copy(wonderTierMap = nextStateBase.wonderTierMap + (targetId to nextTier))
      "gear" -> {
        val slot = GearSlot.entries.firstOrNull { it.id == targetId } ?: GearSlot.HELMET
        val g = nextStateBase.gearMap[targetId] ?: GearState(slot)
        nextStateBase.copy(gearMap = nextStateBase.gearMap + (targetId to g.copy(tier = nextTier)))
      }
      "equipment" -> {
        val eq = nextStateBase.equipmentState(targetId)
        nextStateBase.copy(equipmentMap = nextStateBase.equipmentMap + (targetId to eq.copy(tier = nextTier)))
      }
      "perk" -> nextStateBase.copy(perkRanks = nextStateBase.perkRanks + (targetId to nextTier))
      "protection" -> nextStateBase.copy(protectionRanks = nextStateBase.protectionRanks + (targetId to nextTier))
      "skill" -> {
        val node = nextStateBase.skillNodes[targetId]
          ?: return Result.failure(IllegalStateException("Skill node not found"))
        nextStateBase.copy(
          skillNodes = nextStateBase.skillNodes + (targetId to node.copy(unlocked = true, tier = nextTier.coerceAtMost(node.maxTier)))
        )
      }
      "bot" -> nextStateBase.copy(
        vacuumBot = nextStateBase.vacuumBot.copy(
          tier = nextTier,
          level = (nextStateBase.vacuumBot.level + 2).coerceAtMost(ProgressionCatalog.MAX_BOT_LEVEL),
        )
      )
      "blast_shard" -> nextStateBase.copy(blastShardTier = nextTier)
      "companion" -> nextStateBase.copy(companionBuffTier = nextTier)
      "mode" -> nextStateBase.copy(modeTierMap = nextStateBase.modeTierMap + (targetId to nextTier))
      "area" -> {
        val a = nextStateBase.areaState(targetId)
        nextStateBase.copy(areaStates = nextStateBase.areaStates + (targetId to a.copy(powerGridTier = nextTier)))
      }
      else -> nextStateBase
    }
    save(finalState)
    return Result.success("Upgraded $targetType ($targetId) to T$nextTier/$maxLevel ($costLabel)!")
  }

  fun achievementStates(state: OperatorProgressionState = current): List<AchievementState> {
    val maxWeaponLvl = ProgressionCatalog.weapons.maxOf { state.weaponLevel(it.id).toLong() }
    return ProgressionCatalog.achievements.map { spec ->
      val progress = when (spec.id) {
        "ach_first_drop" -> state.totalMatchesPlayed
        "ach_slayer_100", "ach_slayer_500" -> state.totalElimsLifetime
        "ach_boss_5", "ach_boss_20" -> state.totalBossesLifetime
        "ach_lvl_25", "ach_lvl_100", "ach_lvl_500" -> state.playerLevel.toLong()
        "ach_wpn_30", "ach_wpn_60" -> maxWeaponLvl
        "ach_over8_lobby", "ach_war_32", "ach_war_128" -> state.largestLobbyPlayed.toLong()
        "ach_pack_5", "ach_pack_10", "ach_pack_15", "ach_pack_20", "ach_pack_25" -> state.highestPackTierReached.toLong()
        "ach_wonder_10" -> state.wonderWeaponsPickedTotal
        "ach_store_1", "ach_store_5" -> state.storePurchasesCount.toLong()
        "ach_spend_3k" -> state.totalBlueEssenceSpent
        else -> 0L
      }.coerceAtMost(spec.target)
      AchievementState(
        spec = spec,
        progress = progress,
        claimed = state.claimedAchievements.contains(spec.id),
      )
    }
  }

  fun claimAchievement(achievementId: String): Result<Pair<Long, Long>> {
    val cur = current
    val st = achievementStates(cur).firstOrNull { it.spec.id == achievementId }
      ?: return Result.failure(IllegalStateException("Achievement not found"))
    if (!st.unlocked) {
      return Result.failure(IllegalStateException("Achievement not yet unlocked"))
    }
    if (st.claimed) {
      return Result.failure(IllegalStateException("Achievement already claimed"))
    }
    val next = cur.copy(
      claimedAchievements = cur.claimedAchievements + achievementId,
      playerXp = cur.playerXp + st.spec.rewardPlayerXp,
    )
    save(next)
    return Result.success(st.spec.rewardBlueEssence to st.spec.rewardPlayerXp)
  }

  fun purchaseStoreItem(itemId: String, currentBlueEssence: Long): Result<StorePurchaseResult> {
    val cur = current
    val spec = ProgressionCatalog.storeItems.firstOrNull { it.id == itemId }
      ?: return Result.failure(IllegalStateException("Store item not found"))
    if (!spec.repeatable && cur.purchasedStoreItems.contains(itemId)) {
      return Result.failure(IllegalStateException("${spec.name} is already owned"))
    }
    if (currentBlueEssence < spec.costBlueEssence) {
      return Result.failure(IllegalStateException("Need ${spec.costBlueEssence} Blue Essence"))
    }

    var next = cur.copy(
      totalBlueEssenceSpent = cur.totalBlueEssenceSpent + spec.costBlueEssence,
      storePurchasesCount = cur.storePurchasesCount + 1,
      purchasedStoreItems = cur.purchasedStoreItems + itemId,
    )
    val summary: String = when (itemId) {
      "store_xp_boost_3" -> {
        next = next.copy(xpBoosterMatchesRemaining = cur.xpBoosterMatchesRemaining + 3)
        "Activated 2X XP Booster (+3 matches, total ${next.xpBoosterMatchesRemaining})"
      }
      "store_player_xp_5k" -> {
        next = next.copy(playerXp = cur.playerXp + 6000L)
        "Operator Lv.${next.playerLevel} (+6,000 Player XP)"
      }
      "store_wpn_xp_3k" -> {
        val wpnId = cur.weaponCustom.equippedWeaponId
        val nextWpnXp = (cur.weaponXp(wpnId) + 3000L)
        next = next.copy(weaponXpMap = cur.weaponXpMap + (wpnId to nextWpnXp))
        "${ProgressionCatalog.weaponById(wpnId, cur.customWeapons).name} reached W.Lv.${next.weaponLevel(wpnId)}"
      }
      "store_bp_skip_1500" -> {
        next = next.copy(battlePassXp = cur.battlePassXp + 1800L)
        "Battle Pass advanced to ${next.battlePassXp} BP XP"
      }
      "store_weapon_tier_up" -> {
        val wpnId = cur.weaponCustom.equippedWeaponId
        val curTier = cur.weaponTier(wpnId)
        if (curTier >= PowerTier.MAX_TIER) return Result.failure(IllegalStateException("Equipped weapon is already Tier ${PowerTier.MAX_TIER} Transcendent"))
        val newTier = curTier + 1
        next = next.copy(weaponTierMap = cur.weaponTierMap + (wpnId to newTier))
        "Elevated weapon to Tier $newTier (${ProgressionCatalog.tierMeta(newTier).label})"
      }
      "store_wonder_tier_up" -> {
        val nextWonderTiers = WonderWeaponKind.entries.associate {
          it.id to (cur.wonderTier(it.id) + 1).coerceAtMost(PowerTier.MAX_TIER)
        }
        next = next.copy(wonderTierMap = nextWonderTiers)
        "All 6 Wonder Weapons upgraded +1 Tier!"
      }
      "store_gear_eq_pack" -> {
        val nextGear = cur.gearMap.mapValues { (_, g) ->
          val nextLvl = (g.level + 5).coerceAtMost(ProgressionCatalog.MAX_GEAR_LEVEL)
          val minXp = ProgressionCatalog.totalXpForGearLevel(nextLvl)
          val nextTier = if (g.level >= ProgressionCatalog.MAX_GEAR_LEVEL) (g.tier + 1).coerceAtMost(PowerTier.MAX_TIER) else g.tier
          g.copy(level = nextLvl, xp = maxOf(g.xp, minXp), tier = nextTier)
        }
        val nextEq = cur.equipmentMap.mapValues { (_, e) ->
          val nextLvl = (e.level + 5).coerceAtMost(ProgressionCatalog.MAX_EQUIPMENT_LEVEL)
          val minXp = ProgressionCatalog.totalXpForEquipmentLevel(nextLvl)
          val nextTier = if (e.level >= ProgressionCatalog.MAX_EQUIPMENT_LEVEL) (e.tier + 1).coerceAtMost(PowerTier.MAX_TIER) else e.tier
          e.copy(level = nextLvl, xp = maxOf(e.xp, minXp), tier = nextTier)
        }
        next = next.copy(gearMap = nextGear, equipmentMap = nextEq)
        "All Gear & Equipment boosted +5 Levels!"
      }
      "store_all_weapons_xp" -> {
        val boosted = ProgressionCatalog.allWeapons(cur.customWeapons).associate { w ->
          w.id to ((cur.weaponXpMap[w.id] ?: 0L) + 2000L)
        }
        next = next.copy(weaponXpMap = cur.weaponXpMap + boosted)
        "+2,000 Weapon XP granted to all ${boosted.size} weapons!"
      }
      "store_bot_uplink" -> {
        val nextBoost = (cur.botDamageBoostPct + 15).coerceAtMost(150)
        next = next.copy(botDamageBoostPct = nextBoost)
        "AI Bots tactical damage boosted to +$nextBoost%!"
      }
      "store_omega_suit" -> {
        next = next.copy(
          playerXp = cur.playerXp + 1200L,
          characterCustom = cur.characterCustom.copy(
            suitThemeId = "omega_ascendant",
            primaryArmorHex = 0xFFFFFFFF,
            accentArmorHex = 0xFF00E5FF,
          ),
          weaponCustom = cur.weaponCustom.copy(camoId = "omega_prismatic"),
        )
        "Equipped Omega Celestial Suit + Camo & +1,200 XP!"
      }
      else -> "Purchased ${spec.name}"
    }

    save(next)
    return Result.success(StorePurchaseResult(spec.name, spec.costBlueEssence, summary))
  }

  fun recordMatchProgression(report: MatchProgressionReport): Triple<Long, Long, Long> {
    val cur = current
    val prevPlayerLvl = cur.playerLevel
    val prevWpnLvl = cur.weaponLevel(report.weaponIdUsed)

    val boosterMult = if (cur.xpBoosterMatchesRemaining > 0) 2.0f else 1.0f
    val diffRewardMult = cur.levelCustom.worldDifficultyTier.rewardScale
    val ngPlusAndCollectionMult = cur.combinedGameplayRewardMultiplier(report.worldId)
    val lobbyBonusMult = 1.0f + ((report.lobbySizePlayed.coerceAtLeast(1) - 1) * 0.025f).coerceAtMost(0.75f)
    val baseXp = (report.eliminations * 26L + report.bossEliminations * 140L + report.waveReached * 65L + if (report.extracted) 240L else 75L)
      .coerceAtLeast(60L)
    val xpGained = (baseXp * boosterMult * lobbyBonusMult * diffRewardMult * ngPlusAndCollectionMult).toLong()

    val baseWpnXp = (report.eliminations * 22L + report.bossEliminations * 95L + report.waveReached * 40L + report.wonderPickupsCollected * 35L)
      .coerceAtLeast(50L)
    val wpnXpGained = (baseWpnXp * boosterMult * ngPlusAndCollectionMult).toLong()

    val equipXpGained = ((report.eliminations * 14L + report.bossEliminations * 60L + report.waveReached * 30L) * boosterMult * ngPlusAndCollectionMult)
      .toLong().coerceAtLeast(45L)
    val gearXpGained = ((report.eliminations * 12L + report.bossEliminations * 55L + report.waveReached * 28L) * boosterMult * ngPlusAndCollectionMult)
      .toLong().coerceAtLeast(40L)

    val bpMult = if (cur.activePassVersion == BattlePassVersion.BLACKCELL_OVERDRIVE) 1.30f else 1.0f
    val bpXpGained = ((xpGained * 0.9f) * bpMult).toLong().coerceAtLeast(55L)

    var nextChallenges = cur.challengeProgress
    nextChallenges = incrementChallenge(nextChallenges, "ch_hostiles_40", report.eliminations)
    nextChallenges = incrementChallenge(nextChallenges, "ch_bosses_2", report.bossEliminations)
    nextChallenges = incrementChallenge(nextChallenges, "ch_wonder_2", report.wonderPickupsCollected.toLong())
    if (report.waveReached >= 4L || report.extracted) {
      nextChallenges = incrementChallenge(nextChallenges, "ch_wave_4", 1L)
    }

    val nextWpnMap = cur.weaponXpMap + (report.weaponIdUsed to (cur.weaponXp(report.weaponIdUsed) + wpnXpGained))
    val nextEquipmentMap = cur.equipmentMap.mapValues { (_, eq) ->
      val nextXp = eq.xp + equipXpGained
      val derivedLvl = ProgressionCatalog.equipmentLevelForXp(nextXp)
      eq.copy(
        xp = nextXp,
        level = maxOf(eq.level, derivedLvl).coerceAtMost(ProgressionCatalog.MAX_EQUIPMENT_LEVEL),
      )
    }
    val nextGearMap = cur.gearMap.mapValues { (_, g) ->
      val nextXp = g.xp + gearXpGained
      val derivedLvl = ProgressionCatalog.gearLevelForXp(nextXp)
      g.copy(
        xp = nextXp,
        level = maxOf(g.level, derivedLvl).coerceAtMost(ProgressionCatalog.MAX_GEAR_LEVEL),
      )
    }

    val nextCores = cur.blastCoreInventory.toMutableMap()
    report.blastCoresEarned.forEach { (coreId, count) ->
      if (count > 0L) nextCores[coreId] = (nextCores[coreId] ?: 0L) + count
    }

    // Age active area after each match, cycling back to Young at 100 years
    val curArea = cur.areaState(cur.activeAreaId)
    val rawAge = curArea.ageYears + 8
    val nextArea = curArea.copy(
      ageYears = rawAge % 100,
      rebirthCycles = curArea.rebirthCycles + (rawAge / 100),
    )

    // Level active outfit slightly on every match
    val curOutfit = cur.outfitState(cur.equippedOutfitId)
    val nextOutfit = curOutfit.copy(
      level = (curOutfit.level + 1).coerceAtMost(ProgressionCatalog.MAX_OUTFIT_LEVEL),
    )

    val redGain = (report.redEssenceEarned.coerceAtLeast(report.eliminations * 4L + 20L) * ngPlusAndCollectionMult).toLong()
    val yellowGain = (report.yellowEssenceEarned.coerceAtLeast(report.waveReached * 18L + 20L) * ngPlusAndCollectionMult).toLong()
    val greenGain = (report.greenEssenceEarned.coerceAtLeast(report.eliminations * 3L + 20L) * ngPlusAndCollectionMult).toLong()
    val combinedGain = ((report.combinedEssenceEarned + if (report.bossEliminations > 0) report.bossEliminations * 8L else 0L) * ngPlusAndCollectionMult).toLong()
    val shardGain = ((report.blastShardsEarned + (report.buildingsExplored * 2L)) * ngPlusAndCollectionMult).toLong()
    val nextMissions = applyMatchToMissions(cur.missionProgress, report, shardGain)

    val nextOutfitShards = cur.outfitShardMap.toMutableMap()
    val equippedShards = (report.characterShardsCollected / 2L).coerceAtLeast(if (report.catsFound > 0) 2L else 0L)
    if (equippedShards > 0L) {
      nextOutfitShards[cur.equippedOutfitId] = (nextOutfitShards[cur.equippedOutfitId] ?: 0L) + equippedShards
    }
    report.recruitedOutfitIds.forEach { recId ->
      nextOutfitShards[recId] = (nextOutfitShards[recId] ?: 0L) + 2L
    }
    val remainingShards = report.characterShardsCollected - equippedShards
    if (remainingShards > 0L) {
      val fallbackOriginal = MultiverseArsenalCatalog.outfits.firstOrNull { it.isOriginalCharacter }?.id ?: cur.equippedOutfitId
      nextOutfitShards[fallbackOriginal] = (nextOutfitShards[fallbackOriginal] ?: 0L) + remainingShards
    }
    val nextCatWorlds = if (report.catsFound > 0) cur.discoveredCatWorlds + report.areaId else cur.discoveredCatWorlds
    val runCompletedForNgPlus = report.extracted || report.waveReached >= 5L
    val ngPlusCompletionsNext = if (runCompletedForNgPlus) cur.ngPlusCompletions + 1 else cur.ngPlusCompletions

    val overchargeMult = if (cur.magicState.overchargeDifficultyEnabled) {
      cur.magicState.difficultyScalingFactor.coerceIn(1f, 3.5f)
    } else 1.0f
    val totalRewardScale = ngPlusAndCollectionMult * overchargeMult

    val goldGain = (report.goldEarned.coerceAtLeast(
      (report.eliminations * 14L + report.bossEliminations * 120L + report.waveReached * 35L + if (report.extracted) 200L else 60L)
    ) * totalRewardScale).toLong().coerceAtLeast(80L)
    val diamondsGain = (report.diamondsEarned.coerceAtLeast(
      (report.bossEliminations * 18L + report.waveReached * 4L + if (report.extracted) 25L else 8L)
    ) * totalRewardScale).toLong().coerceAtLeast(8L)
    val tuuCashGain = (report.tuuCashEarned.coerceAtLeast(
      (report.eliminations * 45L + report.bossEliminations * 350L + report.waveReached * 110L + if (report.extracted) 600L else 180L)
    ) * totalRewardScale).toLong().coerceAtLeast(250L)

    val nextEnvRes = cur.environmentalResources.toMutableMap()
    val defaultDrops = mapOf(
      "crystal_shard" to (2L + report.waveReached + report.wonderPickupsCollected * 2L),
      "metal_scrap" to (4L + report.eliminations / 3L + report.buildingsExplored * 2L),
      "energy_cell" to (2L + report.magicCastsCount.toLong() + report.waveReached),
      "aether_herb" to (2L + report.perkDrinksCollected.toLong() + if (report.extracted) 3L else 1L),
      "kyber_fragment" to (1L + report.bossEliminations * 2L + if (report.worldId.contains("star_wars") || report.areaId.contains("star_wars")) 6L else 1L),
    )
    defaultDrops.forEach { (id, baseAmt) ->
      val reported = report.environmentalResourcesEarned[id] ?: 0L
      val gain = ((maxOf(baseAmt, reported)) * ngPlusAndCollectionMult).toLong().coerceAtLeast(1L)
      nextEnvRes[id] = (nextEnvRes[id] ?: 0L) + gain
    }
    report.environmentalResourcesEarned.forEach { (id, amt) ->
      if (id !in defaultDrops && amt > 0L) {
        nextEnvRes[id] = (nextEnvRes[id] ?: 0L) + amt
      }
    }

    val nextSystemShards = cur.systemShards.toMutableMap()
    MultiverseSystemsCatalog.systemShardDomains.forEach { (domainId, _) ->
      val reported = report.systemShardsEarned[domainId] ?: 0L
      val passiveDomainGain = when (domainId) {
        "weapon", "wonder" -> 2L + report.eliminations / 10L
        "crystal", "magic" -> 2L + report.magicCastsCount.toLong()
        "base" -> if (report.baseDefenseWon) 10L else 3L
        "area", "mode" -> 2L + report.buildingsExplored.toLong()
        else -> 2L
      }
      val totalDomainGain = (maxOf(reported, passiveDomainGain) * totalRewardScale).toLong().coerceAtLeast(2L)
      nextSystemShards[domainId] = (nextSystemShards[domainId] ?: 0L) + totalDomainGain
    }

    val vaultTickGold = 60L + (cur.playerBaseState.resourceVaultTier * 35L)
    val vaultTickDiamonds = 12L + (cur.playerBaseState.resourceVaultTier * 6L)
    val vaultTickCash = 240L + (cur.playerBaseState.resourceVaultTier * 140L)
    val prevBase = cur.playerBaseState
    val nextCountdown = (prevBase.attackWaveCountdownSec - 85).let { if (it <= 0) 240 else it }
    val nextUnderAttack = if (report.baseDefenseWon) false else (prevBase.underAttack || nextCountdown == 240)
    val nextBase = prevBase.copy(
      storedGold = prevBase.storedGold + vaultTickGold,
      storedDiamonds = prevBase.storedDiamonds + vaultTickDiamonds,
      storedTuuCash = prevBase.storedTuuCash + vaultTickCash,
      underAttack = nextUnderAttack,
      attackWaveCountdownSec = nextCountdown,
      defenseSuccessCount = prevBase.defenseSuccessCount + if (report.baseDefenseWon) 1 else 0,
      baseHpMax = prevBase.maxIntegrity(),
      baseHpCurrent = if (report.baseDefenseWon) prevBase.maxIntegrity() else prevBase.baseHpCurrent.coerceIn(1, prevBase.maxIntegrity()),
    )

    val nextUnlockedNgPlusTier = if (report.extracted && cur.activeNgPlusTier >= cur.unlockedNgPlusTier) {
      (cur.unlockedNgPlusTier + 1).coerceAtMost(NewGamePlusVersion.MAX_NG_PLUS_TIER)
    } else {
      cur.unlockedNgPlusTier
    }
    val ngPlusCompletionsReport = if (report.extracted) cur.ngPlusCompletions + 1 else cur.ngPlusCompletions

    val nextState = cur.copy(
      playerXp = cur.playerXp + xpGained,
      weaponXpMap = nextWpnMap,
      equipmentMap = nextEquipmentMap,
      gearMap = nextGearMap,
      battlePassXp = cur.battlePassXp + bpXpGained,
      challengeProgress = nextChallenges,
      wonderWeaponsPickedTotal = cur.wonderWeaponsPickedTotal + report.wonderPickupsCollected,
      claimedMissions = cur.claimedMissions,
      missionProgress = nextMissions,
      totalElimsLifetime = cur.totalElimsLifetime + report.eliminations,
      totalBossesLifetime = cur.totalBossesLifetime + report.bossEliminations,
      totalMatchesPlayed = cur.totalMatchesPlayed + 1L,
      highestPackTierReached = maxOf(cur.highestPackTierReached, report.highestPackTierReached),
      largestLobbyPlayed = maxOf(cur.largestLobbyPlayed, report.lobbySizePlayed),
      xpBoosterMatchesRemaining = (cur.xpBoosterMatchesRemaining - 1).coerceAtLeast(0),
      redEssence = cur.redEssence + redGain,
      yellowEssence = cur.yellowEssence + yellowGain,
      greenEssence = cur.greenEssence + greenGain,
      combinedEssence = cur.combinedEssence + combinedGain,
      blastShards = cur.blastShards + shardGain,
      blastCoreInventory = nextCores,
      areaStates = cur.areaStates + (cur.activeAreaId to nextArea),
      outfitStates = cur.outfitStates + (cur.equippedOutfitId to nextOutfit),
      outfitShardMap = nextOutfitShards,
      discoveredCatWorlds = nextCatWorlds,
      lootCratesOpenedTotal = cur.lootCratesOpenedTotal + report.lootCratesOpened.toLong(),
      unlockedNgPlusTier = nextUnlockedNgPlusTier,
      ngPlusCompletions = ngPlusCompletionsReport,
      gold = cur.gold + goldGain,
      diamonds = (cur.diamonds + diamondsGain - report.diamondsSpent).coerceAtLeast(0L),
      tuuCash = (cur.tuuCash + tuuCashGain - report.tuuCashSpent).coerceAtLeast(0L),
      environmentalResources = nextEnvRes,
      systemShards = nextSystemShards,
      playerBaseState = nextBase,
    )
    save(nextState)

    val newPlayerLvl = nextState.playerLevel
    val newWpnLvl = nextState.weaponLevel(report.weaponIdUsed)
    return Triple(
      xpGained,
      (newPlayerLvl - prevPlayerLvl).toLong(),
      (newWpnLvl - prevWpnLvl).toLong(),
    )
  }

  fun missionStates(
    scope: MissionScope? = null,
    targetId: String? = null,
    state: OperatorProgressionState = current,
  ): List<MissionState> {
    val specs: List<MissionSpec> = when {
      scope == MissionScope.WORLD && targetId != null -> MissionsCatalog.missionsForWorld(targetId)
      scope == MissionScope.ARENA && targetId != null -> MissionsCatalog.missionsForArena(targetId)
      scope == MissionScope.WORLD -> MissionsCatalog.worldMissions
      scope == MissionScope.ARENA -> MissionsCatalog.arenaMissions
      else -> MissionsCatalog.allMissions
    }
    return specs.map { spec ->
      val prog = (state.missionProgress[spec.id] ?: 0L).coerceAtMost(spec.targetCount)
      MissionState(
        spec = spec,
        progress = prog,
        claimed = state.claimedMissions.contains(spec.id),
      )
    }
  }

  fun claimMission(missionId: String): Result<Triple<Long, Long, Long>> {
    val cur = current
    val spec = MissionsCatalog.missionById(missionId)
      ?: return Result.failure(IllegalStateException("Mission not found"))
    val prog = cur.missionProgress[missionId] ?: 0L
    if (prog < spec.targetCount) {
      return Result.failure(IllegalStateException("Mission incomplete ($prog/${spec.targetCount})"))
    }
    if (cur.claimedMissions.contains(missionId)) {
      return Result.failure(IllegalStateException("Mission already claimed"))
    }
    val next = cur.copy(
      claimedMissions = cur.claimedMissions + missionId,
      playerXp = cur.playerXp + spec.rewardPlayerXp,
      blastShards = cur.blastShards + spec.rewardShards,
    )
    save(next)
    return Result.success(Triple(spec.rewardBlueEssence, spec.rewardPlayerXp, spec.rewardShards))
  }

  private fun applyMatchToMissions(
    existing: Map<String, Long>,
    report: MatchProgressionReport,
    shardGain: Long,
  ): Map<String, Long> {
    val relevant = MissionsCatalog.missionsForWorld(report.worldId) + MissionsCatalog.missionsForArena(report.areaId)
    if (relevant.isEmpty()) return existing
    val essenceGain = report.redEssenceEarned + report.yellowEssenceEarned + report.greenEssenceEarned + report.combinedEssenceEarned
    val mutable = existing.toMutableMap()
    relevant.forEach { spec ->
      val curVal = mutable[spec.id] ?: 0L
      val updated = when (spec.statKind) {
        MissionStatKind.ELIMINATIONS -> curVal + report.eliminations
        MissionStatKind.BOSS_ELIMS -> curVal + report.bossEliminations
        MissionStatKind.WAVES -> curVal + report.waveReached
        MissionStatKind.WONDER_PICKUPS -> curVal + report.wonderPickupsCollected.toLong()
        MissionStatKind.EXTRACTIONS -> curVal + if (report.extracted) 1L else 0L
        MissionStatKind.TRAP_KILLS -> curVal + report.trapKills.toLong()
        MissionStatKind.BUILDINGS_EXPLORED -> curVal + report.buildingsExplored.toLong()
        MissionStatKind.PACK_TIER -> maxOf(curVal, report.highestPackTierReached.toLong())
        MissionStatKind.ESSENCE_EARNED -> curVal + essenceGain.coerceAtLeast(report.eliminations * 6L + 30L)
        MissionStatKind.BLAST_SHARDS -> curVal + shardGain
      }.coerceAtMost(spec.targetCount)
      if (updated > 0L) mutable[spec.id] = updated
    }
    return mutable
  }

  fun challengeStates(state: OperatorProgressionState = current): List<ChallengeState> =
    ProgressionCatalog.challenges.map { spec ->
      val key = "${spec.id}:${state.challengePrestige}"
      ChallengeState(
        spec = spec,
        progress = (state.challengeProgress[spec.id] ?: 0L).coerceAtMost(spec.target),
        claimed = state.claimedChallenges.contains(key),
      )
    }

  private fun incrementChallenge(
    map: Map<String, Long>,
    id: String,
    delta: Long,
  ): Map<String, Long> {
    if (delta <= 0L) return map
    val spec = ProgressionCatalog.challenges.firstOrNull { it.id == id } ?: return map
    val curVal = map[id] ?: 0L
    val nextVal = (curVal + delta).coerceAtMost(spec.target)
    return map + (id to nextVal)
  }

  private fun save(next: OperatorProgressionState) {
    _state.value = next
    val root = buildRootJson(next)
    val jsonString = root.toString()
    val integritySeal = AppSecurityManager.signPayload(jsonString)
    lastLoadedIntegrityValid = true
    prefs.edit(commit = true) {
      putString(KEY_STATE_JSON, jsonString)
      putString(KEY_STATE_JSON_LEGACY, jsonString)
      putString(KEY_STATE_HASH, integritySeal)
    }
  }

  fun exportJsonObject(): JSONObject = buildRootJson(current)

  fun importJsonObject(root: JSONObject, mergePreferHigher: Boolean): OperatorProgressionState {
    val parsed = parseProgressionJson(root, fallback = current)
    val finalState = if (mergePreferHigher) mergeStatesPreferHigher(current, parsed) else parsed
    save(finalState)
    return finalState
  }

  private fun buildRootJson(next: OperatorProgressionState): JSONObject {
    return JSONObject().apply {
      put("playerXp", next.playerXp)
      put("playerPrestige", next.playerPrestige)
      put("characterTier", next.characterTier)
      put("characterStarLevel", next.characterStarLevel)
      put("battlePassXp", next.battlePassXp)
      put("battlePassPrestige", next.battlePassPrestige)
      put("bpCycleCount", next.bpCycleCount)
      put("challengePrestige", next.challengePrestige)
      put("gearPrestige", next.gearPrestige)
      put("activePassVersion", next.activePassVersion.id)
      put("wonderPicked", next.wonderWeaponsPickedTotal)
      put("totalElimsLifetime", next.totalElimsLifetime)
      put("totalBossesLifetime", next.totalBossesLifetime)
      put("totalMatchesPlayed", next.totalMatchesPlayed)
      put("totalBlueEssenceSpent", next.totalBlueEssenceSpent)
      put("highestPackTierReached", next.highestPackTierReached)
      put("largestLobbyPlayed", next.largestLobbyPlayed)
      put("storePurchasesCount", next.storePurchasesCount)
      put("xpBoosterMatchesRemaining", next.xpBoosterMatchesRemaining)
      put("botDamageBoostPct", next.botDamageBoostPct)
      put("redEssence", next.redEssence)
      put("yellowEssence", next.yellowEssence)
      put("greenEssence", next.greenEssence)
      put("combinedEssence", next.combinedEssence)
      put("blastShards", next.blastShards)
      put("equippedOutfitId", next.equippedOutfitId)
      put("activeAreaId", next.activeAreaId)
      put("equippedWonderIds", JSONArray(next.equippedWonderIds))
      put("equippedLethalIds", JSONArray(next.equippedLethalIds))
      put("equippedTacticalIds", JSONArray(next.equippedTacticalIds))
      put("unlockedPasses", JSONArray(next.unlockedPassVersions.map { it.id }))
      put("claimedTiers", JSONArray(next.claimedPassTiers.toList()))
      put("claimedChallenges", JSONArray(next.claimedChallenges.toList()))
      put("claimedAchievements", JSONArray(next.claimedAchievements.toList()))
      put("claimedMissions", JSONArray(next.claimedMissions.toList()))
      put("missionProgress", JSONObject().apply {
        next.missionProgress.forEach { (k, v) -> put(k, v) }
      })
      put("purchasedStoreItems", JSONArray(next.purchasedStoreItems.toList()))

      put("perkRanks", JSONObject().apply {
        next.perkRanks.forEach { (k, v) -> put(k, v) }
      })
      put("blastCoreInventory", JSONObject().apply {
        next.blastCoreInventory.forEach { (k, v) -> put(k, v) }
      })
      put("blastCoreLevels", JSONObject().apply {
        next.blastCoreLevels.forEach { (k, v) -> put(k, v) }
      })
      put("outfitStates", JSONObject().apply {
        next.outfitStates.forEach { (k, o) ->
          put(k, JSONObject().apply {
            put("level", o.level)
            put("tier", o.tier)
            put("starLevel", o.starLevel)
            put("skillRank", o.skillRank)
            put("gearRank", o.gearRank)
            put("equipmentRank", o.equipmentRank)
          })
        }
      })
      put("areaStates", JSONObject().apply {
        next.areaStates.forEach { (k, a) ->
          put(k, JSONObject().apply {
            put("ageYears", a.ageYears)
            put("rebirthCycles", a.rebirthCycles)
            put("powerGridTier", a.powerGridTier)
            put("defenseTurretTier", a.defenseTurretTier)
            put("trapDensityTier", a.trapDensityTier)
            put("lootRichnessTier", a.lootRichnessTier)
            put("starLevel", a.starLevel)
          })
        }
      })
      put("vacuumBot", JSONObject().apply {
        put("level", next.vacuumBot.level)
        put("tier", next.vacuumBot.tier)
        put("starLevel", next.vacuumBot.starLevel)
        put("autoUpgradeEnabled", next.vacuumBot.autoUpgradeEnabled)
        put("autoCollectEnabled", next.vacuumBot.autoCollectEnabled)
      })

      put("weaponXpMap", JSONObject().apply {
        next.weaponXpMap.forEach { (k, v) -> put(k, v) }
      })
      put("weaponPrestigeMap", JSONObject().apply {
        next.weaponPrestigeMap.forEach { (k, v) -> put(k, v) }
      })
      put("weaponTierMap", JSONObject().apply {
        next.weaponTierMap.forEach { (k, v) -> put(k, v) }
      })
      put("weaponStarMap", JSONObject().apply {
        next.weaponStarMap.forEach { (k, v) -> put(k, v) }
      })
      put("wonderTierMap", JSONObject().apply {
        next.wonderTierMap.forEach { (k, v) -> put(k, v) }
      })
      put("wonderStarMap", JSONObject().apply {
        next.wonderStarMap.forEach { (k, v) -> put(k, v) }
      })
      put("armoryItemStarMap", JSONObject().apply {
        next.armoryItemStarMap.forEach { (k, v) -> put(k, v) }
      })
      put("modeTierMap", JSONObject().apply {
        next.modeTierMap.forEach { (k, v) -> put(k, v) }
      })
      put("modeStarMap", JSONObject().apply {
        next.modeStarMap.forEach { (k, v) -> put(k, v) }
      })
      put("challengeProgress", JSONObject().apply {
        next.challengeProgress.forEach { (k, v) -> put(k, v) }
      })
      put("gearMap", JSONObject().apply {
        next.gearMap.forEach { (k, g) ->
          put(k, JSONObject().apply {
            put("level", g.level)
            put("prestige", g.prestige)
            put("xp", g.xp)
            put("tier", g.tier)
            put("colorHex", g.colorHex)
            put("starLevel", g.starLevel)
          })
        }
      })
      put("equipmentMap", JSONObject().apply {
        next.equipmentMap.forEach { (k, eq) ->
          put(k, JSONObject().apply {
            put("equippedItemId", eq.equippedItemId)
            put("level", eq.level)
            put("prestige", eq.prestige)
            put("xp", eq.xp)
            put("variantId", eq.variantId)
            put("tier", eq.tier)
            put("starLevel", eq.starLevel)
          })
        }
      })
      put("skillNodes", JSONObject().apply {
        next.skillNodes.forEach { (k, node) ->
          put(k, JSONObject().apply {
            put("unlocked", node.unlocked)
            put("tier", node.tier)
            put("starLevel", node.starLevel)
          })
        }
      })
      put("lobbyConfig", JSONObject().apply {
        put("partyMode", next.lobbyConfig.partyMode.id)
        put("targetPlayers", next.lobbyConfig.targetPlayers)
        put("autoFillBots", next.lobbyConfig.autoFillBots)
        put("botDifficulty", next.lobbyConfig.botDifficulty.id)
        put("humanPlayersOnline", next.lobbyConfig.humanPlayersOnline)
      })
      put("weaponCustom", JSONObject().apply {
        put("equippedWeaponId", next.weaponCustom.equippedWeaponId)
        put("equippedWeaponIds", JSONArray(next.weaponCustom.equippedWeaponIds))
        put("camoId", next.weaponCustom.camoId)
        put("opticId", next.weaponCustom.opticId)
        put("barrelId", next.weaponCustom.barrelId)
        put("muzzleId", next.weaponCustom.muzzleId)
        put("underbarrelId", next.weaponCustom.underbarrelId)
        put("magazineId", next.weaponCustom.magazineId)
        put("stockId", next.weaponCustom.stockId)
        put("rearGripId", next.weaponCustom.rearGripId)
        put("laserId", next.weaponCustom.laserId)
        put("ammoModId", next.weaponCustom.ammoModId)
        put("tracerColorHex", next.weaponCustom.tracerColorHex)
      })
      put("wonderCustom", JSONObject().apply {
        put("equippedWonderId", next.wonderCustom.equippedWonderId)
        put("coreId", next.wonderCustom.coreId)
        put("lensId", next.wonderCustom.lensId)
        put("amplifierId", next.wonderCustom.amplifierId)
      })
      put("characterCustom", JSONObject().apply {
        put("suitThemeId", next.characterCustom.suitThemeId)
        put("visorStyleId", next.characterCustom.visorStyleId)
        put("auraTrailId", next.characterCustom.auraTrailId)
        put("animationSpeedMultiplier", next.characterCustom.animationSpeedMultiplier.toDouble())
        put("cloakEnabled", next.characterCustom.cloakEnabled)
        put("titleBadge", next.characterCustom.titleBadge)
        put("primaryArmorHex", next.characterCustom.primaryArmorHex)
        put("accentArmorHex", next.characterCustom.accentArmorHex)
      })
      put("levelCustom", JSONObject().apply {
        put("skyThemeId", next.levelCustom.skyThemeId)
        put("gridStyleId", next.levelCustom.gridStyleId)
        put("enemyDensityMultiplier", next.levelCustom.enemyDensityMultiplier.toDouble())
        put("worldDifficultyTier", next.levelCustom.worldDifficultyTier.id)
        put("wonderDropBoost", next.levelCustom.wonderDropBoost)
        put("critSurgeMutator", next.levelCustom.critSurgeMutator)
        put("instagibMutator", next.levelCustom.instagibMutator)
        put("lowGravityDash", next.levelCustom.lowGravityDash)
        put("bossRushMutator", next.levelCustom.bossRushMutator)
        put("floorAccentHex", next.levelCustom.floorAccentHex)
      })
      put("outfitRarityMap", JSONObject().apply {
        next.outfitRarityMap.forEach { (k, v) -> put(k, v) }
      })
      put("outfitShardMap", JSONObject().apply {
        next.outfitShardMap.forEach { (k, v) -> put(k, v) }
      })
      put("discoveredCatWorlds", JSONArray(next.discoveredCatWorlds.toList()))
      put("lootCratesOpenedTotal", next.lootCratesOpenedTotal)
      put("privateMatchRules", JSONObject().apply {
        put("isPrivate", next.privateMatchRules.isPrivate)
        put("privacy", next.privateMatchRules.privacy.id)
        put("roomName", next.privateMatchRules.roomName)
        put("roomCode", next.privateMatchRules.roomCode)
        put("passcode", next.privateMatchRules.passcode)
        put("startingWave", next.privateMatchRules.startingWave)
        put("maxPlayers", next.privateMatchRules.maxPlayers)
        put("botFillEnabled", next.privateMatchRules.botFillEnabled)
        put("botDifficulty", next.privateMatchRules.botDifficulty.id)
        put("worldId", next.privateMatchRules.worldId)
        put("areaId", next.privateMatchRules.areaId)
        put("modeId", next.privateMatchRules.modeId)
        put("infiniteAmmo", next.privateMatchRules.infiniteAmmo)
        put("headshotsOnly", next.privateMatchRules.headshotsOnly)
        put("bossRush", next.privateMatchRules.bossRush)
        put("wonderSpawnBoost", next.privateMatchRules.wonderSpawnBoost)
        put("lowGravity", next.privateMatchRules.lowGravity)
        put("doubleSpeed", next.privateMatchRules.doubleSpeed)
        put("vampiricSiphon", next.privateMatchRules.vampiricSiphon)
        put("doubleBlueEssence", next.privateMatchRules.doubleBlueEssence)
        put("friendlyFire", next.privateMatchRules.friendlyFire)
        put("customWaveGoal", next.privateMatchRules.customWaveGoal)
      })
      put("clan", JSONObject().apply {
        put("id", next.clan.id)
        put("name", next.clan.name)
        put("tag", next.clan.tag)
        put("motto", next.clan.motto)
        put("level", next.clan.level)
        put("tier", next.clan.tier)
        put("starLevel", next.clan.starLevel)
        put("totalEssenceDonated", next.clan.totalEssenceDonated)
        put("leaderCallsign", next.clan.leaderCallsign)
        put("colorHex", next.clan.colorHex)
        put("members", JSONArray().apply {
          next.clan.members.forEach { m ->
            put(JSONObject().apply {
              put("id", m.id)
              put("callsign", m.callsign)
              put("rank", m.rank.id)
              put("level", m.level)
              put("stars", m.stars)
              put("donatedEssence", m.donatedEssence)
              put("online", m.online)
            })
          }
        })
      })
      put("friends", JSONArray().apply {
        next.friends.forEach { f ->
          put(JSONObject().apply {
            put("id", f.id)
            put("callsign", f.callsign)
            put("level", f.level)
            put("stars", f.stars)
            put("tier", f.tier)
            put("avatarThemeId", f.avatarThemeId)
            put("status", f.status.id)
            put("statusDetail", f.statusDetail)
            put("currentMatchId", f.currentMatchId ?: "")
            put("clanTag", f.clanTag ?: "")
            put("isFavorite", f.isFavorite)
          })
        }
      })
      put("chatMessages", JSONArray().apply {
        next.chatMessages.takeLast(150).forEach { msg ->
          put(JSONObject().apply {
            put("id", msg.id)
            put("channel", msg.channel.id)
            put("senderCallsign", msg.senderCallsign)
            put("senderClanTag", msg.senderClanTag ?: "")
            put("senderStars", msg.senderStars)
            put("targetFriendId", msg.targetFriendId ?: "")
            put("content", msg.content)
            put("timestampMillis", msg.timestampMillis)
            put("isTacticalQuickChat", msg.isTacticalQuickChat)
            put("isSystemMessage", msg.isSystemMessage)
          })
        }
      })
      next.joinedMatch?.let { jm ->
        put("joinedMatch", JSONObject().apply {
          put("id", jm.id)
          put("roomName", jm.roomName)
          put("hostCallsign", jm.hostCallsign)
          put("hostLevel", jm.hostLevel)
          put("hostStars", jm.hostStars)
          put("worldId", jm.worldId)
          put("worldTitle", jm.worldTitle)
          put("modeId", jm.modeId)
          put("modeLabel", jm.modeLabel)
          put("areaId", jm.areaId)
          put("areaName", jm.areaName)
          put("currentPlayers", jm.currentPlayers)
          put("maxPlayers", jm.maxPlayers)
          put("privacy", jm.privacy.id)
          put("roomCode", jm.roomCode)
          put("passcode", jm.passcode ?: "")
          put("pingMs", jm.pingMs)
          put("currentWave", jm.currentWave)
          put("activeMutators", JSONArray(jm.activeMutators))
          put("clanTag", jm.clanTag ?: "")
          put("regionId", jm.regionId)
          put("serverId", jm.serverId)
        })
      }
      put("activeNgPlusTier", next.activeNgPlusTier)
      put("unlockedNgPlusTier", next.unlockedNgPlusTier)
      put("ngPlusCompletions", next.ngPlusCompletions)
      put("worldCollectionDifficultyMap", JSONObject().apply {
        next.worldCollectionDifficultyMap.forEach { (k, v) -> put(k, v) }
      })
      put("collectionCustomMode", JSONObject().apply {
        val cm = next.collectionCustomMode
        put("enabled", cm.enabled)
        put("safeCollectionGuard", cm.safeCollectionGuard)
        put("scope", cm.scope.id)
        put("targetAreaId", cm.targetAreaId)
        put("enemyHpBoostPercent", cm.enemyHpBoostPercent)
        put("enemyArmorFlood", cm.enemyArmorFlood)
        put("powerAndCooldownSuppression", cm.powerAndCooldownSuppression)
        put("essenceAndShardDrain", cm.essenceAndShardDrain)
        put("shieldAttritionLeak", cm.shieldAttritionLeak)
        put("collectionAmbushSurge", cm.collectionAmbushSurge)
        put("disableWonderDrops", cm.disableWonderDrops)
      })
      put("customGameMaps", JSONArray().apply {
        next.customGameMaps.forEach { m ->
          put(JSONObject().apply {
            put("id", m.id)
            put("mapCode", m.mapCode)
            put("title", m.title)
            put("gameCategory", m.gameCategory.id)
            put("description", m.description)
            put("buildingNames", JSONArray(m.buildingNames))
            put("wonderWeapon", m.wonderWeapon)
            put("accentHex", m.accentHex)
            put("secondaryHex", m.secondaryHex)
            put("isUserCreated", m.isUserCreated)
            put("createdAtMillis", m.createdAtMillis)
          })
        }
      })
      put("customWeapons", JSONArray().apply {
        next.customWeapons.forEach { w ->
          put(JSONObject().apply {
            put("id", w.id)
            put("weaponCode", w.weaponCode)
            put("name", w.name)
            put("gameCategory", w.gameCategory.id)
            put("description", w.description)
            put("damageTypeId", w.damageTypeId)
            put("baseDamage", w.baseDamage)
            put("baseCadenceTicks", w.baseCadenceTicks.toDouble())
            put("baseProjectileSpeed", w.baseProjectileSpeed.toDouble())
            put("baseSpreadCount", w.baseSpreadCount)
            put("basePierce", w.basePierce)
            put("defaultTracerHex", w.defaultTracerHex)
            put("isUserCreated", w.isUserCreated)
            put("createdAtMillis", w.createdAtMillis)
          })
        }
      })
      put("protectionRanks", JSONObject().apply {
        next.protectionRanks.forEach { (k, v) -> put(k, v) }
      })
      put("protectionStarMap", JSONObject().apply {
        next.protectionStarMap.forEach { (k, v) -> put(k, v) }
      })
      put("perkStarMap", JSONObject().apply {
        next.perkStarMap.forEach { (k, v) -> put(k, v) }
      })
      put("blastShardTier", next.blastShardTier)
      put("companionBuffTier", next.companionBuffTier)
      put("customPerks", JSONArray().apply {
        next.customPerks.forEach { p ->
          put(JSONObject().apply {
            put("id", p.id)
            put("name", p.name)
            put("description", p.description)
            put("accentHex", p.accentHex)
            put("shieldBonusPerRank", p.shieldBonusPerRank)
            put("damagePctPerRank", p.damagePctPerRank)
            put("fireRatePctPerRank", p.fireRatePctPerRank)
            put("cooldownHastePctPerRank", p.cooldownHastePctPerRank)
            put("moveSpeedPctPerRank", p.moveSpeedPctPerRank)
            put("regenPerSecPerRank", p.regenPerSecPerRank.toDouble())
            put("siphonPerKillPerRank", p.siphonPerKillPerRank)
            put("tier", p.tier)
            put("maxTier", p.maxTier)
            put("starLevel", p.starLevel)
          })
        }
      })
      put("equippedCustomPerkIds", JSONArray(next.equippedCustomPerkIds.toList()))
      put("gearCustomizations", JSONObject().apply {
        next.gearCustomizations.forEach { (k, gc) ->
          put(k, JSONObject().apply {
            put("slotId", gc.slotId)
            put("customTitle", gc.customTitle)
            put("alloyId", gc.alloyId)
            put("focusId", gc.focusId)
            put("elementAffinityId", gc.elementAffinityId)
            put("accentHex", gc.accentHex)
          })
        }
      })
      put("customGear", JSONArray().apply {
        next.customGear.forEach { g ->
          put(JSONObject().apply {
            put("id", g.id)
            put("slot", g.slot.id)
            put("name", g.name)
            put("description", g.description)
            put("elementAffinityId", g.elementAffinityId)
            put("accentHex", g.accentHex)
            put("bonusShield", g.bonusShield)
            put("bonusDamagePct", g.bonusDamagePct)
            put("bonusRatePct", g.bonusRatePct)
            put("bonusCooldownHastePct", g.bonusCooldownHastePct)
            put("bonusProtectionPct", g.bonusProtectionPct)
            put("tier", g.tier)
            put("maxTier", g.maxTier)
            put("starLevel", g.starLevel)
          })
        }
      })
      put("equippedCustomGearMap", JSONObject().apply {
        next.equippedCustomGearMap.forEach { (k, v) -> put(k, v) }
      })
      put("customEquipment", JSONArray().apply {
        next.customEquipment.forEach { eq ->
          put(JSONObject().apply {
            put("id", eq.id)
            put("slotType", eq.slotType.id)
            put("name", eq.name)
            put("description", eq.description)
            put("damageTypeId", eq.damageTypeId)
            put("baseDamageOrEffect", eq.baseDamageOrEffect.toDouble())
            put("cooldownSeconds", eq.cooldownSeconds.toDouble())
            put("cooldownHastePct", eq.cooldownHastePct)
            put("accentHex", eq.accentHex)
            put("tier", eq.tier)
            put("maxTier", eq.maxTier)
            put("starLevel", eq.starLevel)
          })
        }
      })
      put("gold", next.gold)
      put("diamonds", next.diamonds)
      put("tuuCash", next.tuuCash)
      put("environmentalResources", JSONObject().apply {
        next.environmentalResources.forEach { (k, v) -> put(k, v) }
      })
      put("systemShards", JSONObject().apply {
        next.systemShards.forEach { (k, v) -> put(k, v) }
      })
      put("magicState", JSONObject().apply {
        val m = next.magicState
        put("unlocked", m.unlocked)
        put("activeSpellId", m.activeSpellId)
        put("manaCurrent", m.manaCurrent.toDouble())
        put("manaMax", m.manaMax.toDouble())
        put("magicTier", m.magicTier)
        put("difficultyScalingFactor", m.difficultyScalingFactor.toDouble())
        put("overchargeDifficultyEnabled", m.overchargeDifficultyEnabled)
        put("unlockedSpells", JSONArray(m.unlockedSpells.toList()))
        put("spellTiers", JSONObject().apply {
          m.spellTiers.forEach { (k, v) -> put(k, v) }
        })
      })
      put("playerBaseState", JSONObject().apply {
        val b = next.playerBaseState
        put("baseName", b.baseName)
        put("layoutTheme", b.layoutTheme)
        put("primaryHex", b.primaryHex)
        put("wallTier", b.wallTier)
        put("turretTier", b.turretTier)
        put("shieldGeneratorTier", b.shieldGeneratorTier)
        put("resourceVaultTier", b.resourceVaultTier)
        put("storedGold", b.storedGold)
        put("storedDiamonds", b.storedDiamonds)
        put("storedTuuCash", b.storedTuuCash)
        put("baseHpCurrent", b.baseHpCurrent)
        put("baseHpMax", b.baseHpMax)
        put("underAttack", b.underAttack)
        put("attackWaveCountdownSec", b.attackWaveCountdownSec)
        put("defenseSuccessCount", b.defenseSuccessCount)
      })
      put("crystals", JSONArray().apply {
        next.crystals.forEach { c ->
          put(JSONObject().apply {
            put("id", c.id)
            put("name", c.name)
            put("rarity", c.rarity)
            put("element", c.element)
            put("statBonusSummary", c.statBonusSummary)
            put("damageBoostPct", c.damageBoostPct)
            put("shieldBoostPct", c.shieldBoostPct)
            put("luckBoostPct", c.luckBoostPct)
            put("socketedSlot", c.socketedSlot ?: "")
            put("level", c.level)
          })
        }
      })
    }
  }

  private inline fun <T> safeParse(defaultValue: T, block: () -> T): T =
    runCatching(block).getOrElse { defaultValue }

  private fun parseProgressionJson(
    root: JSONObject,
    fallback: OperatorProgressionState,
  ): OperatorProgressionState {
    val unlockedPasses = safeParse(fallback.unlockedPassVersions) {
      val unlockedArr = root.optJSONArray("unlockedPasses")
      val set = mutableSetOf(BattlePassVersion.STANDARD_SECTOR_OPS)
      if (unlockedArr != null) {
        for (i in 0 until unlockedArr.length()) {
          val id = unlockedArr.optString(i)
          BattlePassVersion.entries.firstOrNull { it.id == id }?.let { set.add(it) }
        }
      }
      set
    }
    val activePass = safeParse(fallback.activePassVersion) {
      val activePassId = root.optString("activePassVersion", fallback.activePassVersion.id)
      BattlePassVersion.entries.firstOrNull { it.id == activePassId } ?: fallback.activePassVersion
    }

    val claimedTiers = safeParse(fallback.claimedPassTiers) {
      val set = mutableSetOf<String>()
      root.optJSONArray("claimedTiers")?.let { arr ->
        for (i in 0 until arr.length()) set.add(arr.optString(i))
        set
      } ?: fallback.claimedPassTiers
    }
    val claimedChallenges = safeParse(fallback.claimedChallenges) {
      val set = mutableSetOf<String>()
      root.optJSONArray("claimedChallenges")?.let { arr ->
        for (i in 0 until arr.length()) set.add(arr.optString(i))
        set
      } ?: fallback.claimedChallenges
    }
    val claimedAchievements = safeParse(fallback.claimedAchievements) {
      val set = mutableSetOf<String>()
      root.optJSONArray("claimedAchievements")?.let { arr ->
        for (i in 0 until arr.length()) set.add(arr.optString(i))
        set
      } ?: fallback.claimedAchievements
    }
    val claimedMissions = safeParse(fallback.claimedMissions) {
      val set = mutableSetOf<String>()
      root.optJSONArray("claimedMissions")?.let { arr ->
        for (i in 0 until arr.length()) set.add(arr.optString(i))
        set
      } ?: fallback.claimedMissions
    }
    val missionProgress = safeParse(fallback.missionProgress) {
      val map = mutableMapOf<String, Long>()
      root.optJSONObject("missionProgress")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optLong(k, 0L).coerceAtLeast(0L) }
        map
      } ?: fallback.missionProgress
    }
    val purchasedStoreItems = safeParse(fallback.purchasedStoreItems) {
      val set = mutableSetOf<String>()
      root.optJSONArray("purchasedStoreItems")?.let { arr ->
        for (i in 0 until arr.length()) set.add(arr.optString(i))
        set
      } ?: fallback.purchasedStoreItems
    }

    val wpnXp = safeParse(fallback.weaponXpMap) {
      val map = mutableMapOf<String, Long>()
      root.optJSONObject("weaponXpMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optLong(k, 0L) }
        map
      } ?: fallback.weaponXpMap
    }
    val wpnPrestige = safeParse(fallback.weaponPrestigeMap) {
      val map = mutableMapOf<String, Int>()
      root.optJSONObject("weaponPrestigeMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 0) }
        map
      } ?: fallback.weaponPrestigeMap
    }
    val wpnTierMap = safeParse(fallback.weaponTierMap) {
      val map = ProgressionCatalog.weapons.associate { it.id to 1 }.toMutableMap()
      root.optJSONObject("weaponTierMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, PowerTier.MAX_TIER) }
      }
      map
    }
    val wpnStarMap = safeParse(fallback.weaponStarMap) {
      val map = ProgressionCatalog.weapons.associate { it.id to 1 }.toMutableMap()
      root.optJSONObject("weaponStarMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, StarRankMeta.MAX_STARS) }
      }
      map
    }
    val wonderTierMap = safeParse(fallback.wonderTierMap) {
      val map = WonderWeaponKind.entries.associate { it.id to 1 }.toMutableMap()
      root.optJSONObject("wonderTierMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, PowerTier.MAX_TIER) }
      }
      map
    }
    val wonderStarMap = safeParse(fallback.wonderStarMap) {
      val map = WonderWeaponKind.entries.associate { it.id to 1 }.toMutableMap()
      root.optJSONObject("wonderStarMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, StarRankMeta.MAX_STARS) }
      }
      map
    }
    val armoryStarMap = safeParse(fallback.armoryItemStarMap) {
      val map = mutableMapOf<String, Int>()
      root.optJSONObject("armoryItemStarMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, StarRankMeta.MAX_STARS) }
        map
      } ?: fallback.armoryItemStarMap
    }
    val chProg = safeParse(fallback.challengeProgress) {
      val map = mutableMapOf<String, Long>()
      root.optJSONObject("challengeProgress")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optLong(k, 0L) }
        map
      } ?: fallback.challengeProgress
    }

    val gearMap = safeParse(fallback.gearMap) {
      val map = GearSlot.entries.associate { it.id to GearState(it) }.toMutableMap()
      root.optJSONObject("gearMap")?.let { obj ->
        obj.keys().forEach { k ->
          val gObj = obj.optJSONObject(k)
          if (gObj != null) {
            val slot = GearSlot.entries.firstOrNull { it.id == k } ?: GearSlot.HELMET
            val lvl = gObj.optInt("level", 1).coerceIn(1, ProgressionCatalog.MAX_GEAR_LEVEL)
            val xp = gObj.optLong("xp", ProgressionCatalog.totalXpForGearLevel(lvl))
            map[k] = GearState(
              slot = slot,
              level = maxOf(lvl, ProgressionCatalog.gearLevelForXp(xp)),
              prestige = gObj.optInt("prestige", 0),
              xp = xp,
              tier = gObj.optInt("tier", 1).coerceIn(1, PowerTier.MAX_TIER),
              colorHex = gObj.optLong("colorHex", 0xFF00E5FF),
              starLevel = gObj.optInt("starLevel", 1).coerceIn(1, StarRankMeta.MAX_STARS),
            )
          }
        }
      }
      map
    }

    val equipmentMap = safeParse(fallback.equipmentMap) {
      val map = mapOf(
        "lethal" to EquipmentState("lethal", "lethal_frag", 1, 0, 0L, "standard", 1, 1),
        "tactical" to EquipmentState("tactical", "tactical_stim", 1, 0, 0L, "standard", 1, 1),
      ).toMutableMap()
      root.optJSONObject("equipmentMap")?.let { obj ->
        obj.keys().forEach { k ->
          val eObj = obj.optJSONObject(k)
          if (eObj != null) {
            val lvl = eObj.optInt("level", 1).coerceIn(1, ProgressionCatalog.MAX_EQUIPMENT_LEVEL)
            val xp = eObj.optLong("xp", ProgressionCatalog.totalXpForEquipmentLevel(lvl))
            map[k] = EquipmentState(
              slotId = k,
              equippedItemId = eObj.optString("equippedItemId", if (k == "lethal") "lethal_frag" else "tactical_stim"),
              level = maxOf(lvl, ProgressionCatalog.equipmentLevelForXp(xp)),
              prestige = eObj.optInt("prestige", 0),
              xp = xp,
              variantId = eObj.optString("variantId", "standard"),
              tier = eObj.optInt("tier", 1).coerceIn(1, PowerTier.MAX_TIER),
              starLevel = eObj.optInt("starLevel", 1).coerceIn(1, StarRankMeta.MAX_STARS),
            )
          }
        }
      }
      map
    }

    val skillNodes = safeParse(fallback.skillNodes) {
      val map = ProgressionCatalog.defaultSkillNodes().toMutableMap()
      root.optJSONObject("skillNodes")?.let { obj ->
        obj.keys().forEach { k ->
          val sObj = obj.optJSONObject(k)
          val existing = map[k]
          if (sObj != null && existing != null) {
            map[k] = existing.copy(
              unlocked = sObj.optBoolean("unlocked", false),
              tier = sObj.optInt("tier", 1).coerceIn(1, existing.maxTier),
              starLevel = sObj.optInt("starLevel", 1).coerceIn(1, StarRankMeta.MAX_STARS),
            )
          }
        }
      }
      map
    }

    val lobbyCfg = safeParse(fallback.lobbyConfig) {
      val lobbyObj = root.optJSONObject("lobbyConfig")
      if (lobbyObj != null) {
        val modeId = lobbyObj.optString("partyMode", LobbyPartyMode.SQUADS.id)
        val pMode = LobbyPartyMode.entries.firstOrNull { it.id == modeId } ?: LobbyPartyMode.SQUADS
        val diffId = lobbyObj.optString("botDifficulty", BotDifficulty.OPERATIVE.id)
        val diff = BotDifficulty.entries.firstOrNull { it.id == diffId } ?: BotDifficulty.OPERATIVE
        LobbyConfig(
          partyMode = pMode,
          targetPlayers = lobbyObj.optInt("targetPlayers", 12).coerceIn(1, 250),
          autoFillBots = lobbyObj.optBoolean("autoFillBots", true),
          botDifficulty = diff,
          humanPlayersOnline = lobbyObj.optInt("humanPlayersOnline", 1).coerceIn(1, 250),
        )
      } else fallback.lobbyConfig
    }

    val wc = safeParse(fallback.weaponCustom) {
      val wcObj = root.optJSONObject("weaponCustom")
      if (wcObj != null) {
        val mainId = wcObj.optString("equippedWeaponId", "wpn_pulse_carbine")
        val eqList = mutableListOf<String>()
        wcObj.optJSONArray("equippedWeaponIds")?.let { arr ->
          for (i in 0 until arr.length()) {
            val s = arr.optString(i)
            if (s.isNotBlank()) eqList.add(s)
          }
        }
        if (eqList.isEmpty()) eqList.add(mainId)
        WeaponCustomization(
          equippedWeaponId = mainId,
          equippedWeaponIds = eqList.distinct(),
          camoId = wcObj.optString("camoId", "cyber_matte"),
          opticId = wcObj.optString("opticId", "holo_cyan"),
          barrelId = wcObj.optString("barrelId", "standard_barrel"),
          muzzleId = wcObj.optString("muzzleId", "standard_muzzle"),
          underbarrelId = wcObj.optString("underbarrelId", "none_underbarrel"),
          magazineId = wcObj.optString("magazineId", "extended_mag_1"),
          stockId = wcObj.optString("stockId", "tactical_stock"),
          rearGripId = wcObj.optString("rearGripId", "stippled_grip"),
          laserId = wcObj.optString("laserId", "none_laser"),
          ammoModId = wcObj.optString("ammoModId", "standard_core"),
          tracerColorHex = wcObj.optLong("tracerColorHex", 0xFF00E5FF),
        )
      } else fallback.weaponCustom
    }

    val wonC = safeParse(fallback.wonderCustom) {
      val wonObj = root.optJSONObject("wonderCustom")
      if (wonObj != null) {
        com.example.data.model.WonderWeaponCustomization(
          equippedWonderId = wonObj.optString("equippedWonderId", fallback.wonderCustom.equippedWonderId),
          coreId = wonObj.optString("coreId", fallback.wonderCustom.coreId),
          lensId = wonObj.optString("lensId", fallback.wonderCustom.lensId),
          amplifierId = wonObj.optString("amplifierId", fallback.wonderCustom.amplifierId),
        )
      } else fallback.wonderCustom
    }

    val cc = safeParse(fallback.characterCustom) {
      val ccObj = root.optJSONObject("characterCustom")
      if (ccObj != null) {
        CharacterCustomization(
          suitThemeId = ccObj.optString("suitThemeId", "vanguard_cyber"),
          visorStyleId = ccObj.optString("visorStyleId", "cyan_visor"),
          auraTrailId = ccObj.optString("auraTrailId", "none"),
          animationSpeedMultiplier = ccObj.optDouble("animationSpeedMultiplier", 1.0).toFloat().coerceIn(0.5f, 2.5f),
          cloakEnabled = ccObj.optBoolean("cloakEnabled", true),
          titleBadge = ccObj.optString("titleBadge", "Multiverse Operative"),
          primaryArmorHex = ccObj.optLong("primaryArmorHex", 0xFF00E5FF),
          accentArmorHex = ccObj.optLong("accentArmorHex", 0xFFFFAB40),
        )
      } else fallback.characterCustom
    }

    val lc = safeParse(fallback.levelCustom) {
      val lcObj = root.optJSONObject("levelCustom")
      if (lcObj != null) {
        LevelCustomization(
          skyThemeId = lcObj.optString("skyThemeId", "default_sector"),
          gridStyleId = lcObj.optString("gridStyleId", "tactical_square"),
          enemyDensityMultiplier = lcObj.optDouble("enemyDensityMultiplier", 1.0).toFloat().coerceIn(0.75f, 2.5f),
          worldDifficultyTier = WorldDifficultyTier.fromId(lcObj.optString("worldDifficultyTier", WorldDifficultyTier.NORMAL.id)),
          wonderDropBoost = lcObj.optBoolean("wonderDropBoost", false),
          critSurgeMutator = lcObj.optBoolean("critSurgeMutator", false),
          instagibMutator = lcObj.optBoolean("instagibMutator", false),
          lowGravityDash = lcObj.optBoolean("lowGravityDash", false),
          bossRushMutator = lcObj.optBoolean("bossRushMutator", false),
          floorAccentHex = lcObj.optLong("floorAccentHex", 0xFF00E5FF),
        )
      } else fallback.levelCustom
    }

    val outfitRarityMap = safeParse(fallback.outfitRarityMap) {
      val map = mutableMapOf<String, Int>()
      root.optJSONObject("outfitRarityMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, CharacterRarity.entries.size) }
        map
      } ?: fallback.outfitRarityMap
    }
    val outfitShardMap = safeParse(fallback.outfitShardMap) {
      val map = mutableMapOf<String, Long>()
      root.optJSONObject("outfitShardMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optLong(k, 0L).coerceAtLeast(0L) }
        map
      } ?: fallback.outfitShardMap
    }
    val discoveredCatWorlds = safeParse(fallback.discoveredCatWorlds) {
      val set = mutableSetOf<String>()
      root.optJSONArray("discoveredCatWorlds")?.let { arr ->
        for (i in 0 until arr.length()) set.add(arr.optString(i))
        set
      } ?: fallback.discoveredCatWorlds
    }

    val wonderList = safeParse(fallback.equippedWonderIds) {
      val list = mutableListOf<String>()
      root.optJSONArray("equippedWonderIds")?.let { arr ->
        for (i in 0 until arr.length()) {
          val s = arr.optString(i)
          if (s.isNotBlank()) list.add(s)
        }
      }
      if (list.isEmpty()) list.add(WonderWeaponKind.RAY_NOVA_MK3.id)
      list.distinct()
    }

    val lethalList = safeParse(fallback.equippedLethalIds) {
      val list = mutableListOf<String>()
      root.optJSONArray("equippedLethalIds")?.let { arr ->
        for (i in 0 until arr.length()) {
          val s = arr.optString(i)
          if (s.isNotBlank()) list.add(s)
        }
      }
      if (list.isEmpty()) list.add(equipmentMap["lethal"]?.equippedItemId ?: "lethal_frag")
      list.distinct()
    }

    val tacticalList = safeParse(fallback.equippedTacticalIds) {
      val list = mutableListOf<String>()
      root.optJSONArray("equippedTacticalIds")?.let { arr ->
        for (i in 0 until arr.length()) {
          val s = arr.optString(i)
          if (s.isNotBlank()) list.add(s)
        }
      }
      if (list.isEmpty()) list.add(equipmentMap["tactical"]?.equippedItemId ?: "tactical_stim")
      list.distinct()
    }

    val perkRanks = safeParse(fallback.perkRanks) {
      val map = mutableMapOf<String, Int>()
      root.optJSONObject("perkRanks")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 0).coerceIn(0, ProgressionCatalog.MAX_PERK_RANK) }
        map
      } ?: fallback.perkRanks
    }

    val coreInv = safeParse(fallback.blastCoreInventory) {
      val map = BlastCoreColor.entries.associate { it.id to 2L }.toMutableMap()
      root.optJSONObject("blastCoreInventory")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optLong(k, 2L).coerceAtLeast(0L) }
      }
      map
    }

    val coreLvls = safeParse(fallback.blastCoreLevels) {
      val map = BlastCoreColor.entries.associate { it.id to 1 }.toMutableMap()
      root.optJSONObject("blastCoreLevels")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, ProgressionCatalog.MAX_CORE_LEVEL) }
      }
      map
    }

    val outfitStates = safeParse(fallback.outfitStates) {
      val map = MultiverseArsenalCatalog.outfits.associate {
        it.id to OutfitProgressState(it.id)
      }.toMutableMap()
      root.optJSONObject("outfitStates")?.let { obj ->
        obj.keys().forEach { k ->
          val oObj = obj.optJSONObject(k)
          if (oObj != null) {
            map[k] = OutfitProgressState(
              outfitId = k,
              level = oObj.optInt("level", 1).coerceIn(1, ProgressionCatalog.MAX_OUTFIT_LEVEL),
              tier = oObj.optInt("tier", 1).coerceIn(1, PowerTier.MAX_TIER),
              skillRank = oObj.optInt("skillRank", 1).coerceIn(1, PowerTier.MAX_TIER),
              gearRank = oObj.optInt("gearRank", 1).coerceIn(1, PowerTier.MAX_TIER),
              equipmentRank = oObj.optInt("equipmentRank", 1).coerceIn(1, PowerTier.MAX_TIER),
              starLevel = oObj.optInt("starLevel", 1).coerceIn(1, StarRankMeta.MAX_STARS),
            )
          }
        }
      }
      map
    }

    val areaStates = safeParse(fallback.areaStates) {
      val seedAreaIds = (CuratedCustomMapsCatalog.allPlayableAreas().map { it.id } +
        MultiverseArsenalCatalog.customAreas.map { it.id }).distinct()
      val map = seedAreaIds.associateWith { id -> CustomAreaState(id) }.toMutableMap()
      root.optJSONObject("areaStates")?.let { obj ->
        obj.keys().forEach { k ->
          val aObj = obj.optJSONObject(k)
          if (aObj != null) {
            map[k] = CustomAreaState(
              areaId = k,
              ageYears = aObj.optInt("ageYears", 12).coerceIn(0, 99),
              rebirthCycles = aObj.optInt("rebirthCycles", 0).coerceAtLeast(0),
              powerGridTier = aObj.optInt("powerGridTier", 1).coerceIn(1, PowerTier.MAX_TIER),
              defenseTurretTier = aObj.optInt("defenseTurretTier", 1).coerceIn(1, PowerTier.MAX_TIER),
              trapDensityTier = aObj.optInt("trapDensityTier", 1).coerceIn(1, PowerTier.MAX_TIER),
              lootRichnessTier = aObj.optInt("lootRichnessTier", 1).coerceIn(1, PowerTier.MAX_TIER),
              starLevel = aObj.optInt("starLevel", 1).coerceIn(1, StarRankMeta.MAX_STARS),
            )
          }
        }
      }
      map
    }

    val vacBot = safeParse(fallback.vacuumBot) {
      val vacObj = root.optJSONObject("vacuumBot")
      if (vacObj != null) {
        VacuumBotState(
          level = vacObj.optInt("level", 1).coerceIn(1, ProgressionCatalog.MAX_BOT_LEVEL),
          tier = vacObj.optInt("tier", 1).coerceIn(1, PowerTier.MAX_TIER),
          autoUpgradeEnabled = vacObj.optBoolean("autoUpgradeEnabled", true),
          autoCollectEnabled = vacObj.optBoolean("autoCollectEnabled", true),
          starLevel = vacObj.optInt("starLevel", 1).coerceIn(1, StarRankMeta.MAX_STARS),
        )
      } else fallback.vacuumBot
    }

    val pmRules = safeParse(fallback.privateMatchRules) {
      val pmObj = root.optJSONObject("privateMatchRules")
      if (pmObj != null) {
        val privId = pmObj.optString("privacy", MatchPrivacy.PRIVATE_PASSWORD.id)
        val priv = MatchPrivacy.entries.firstOrNull { it.id == privId } ?: MatchPrivacy.PRIVATE_PASSWORD
        val diffId = pmObj.optString("botDifficulty", BotDifficulty.OPERATIVE.id)
        val diff = BotDifficulty.entries.firstOrNull { it.id == diffId } ?: BotDifficulty.OPERATIVE
        PrivateMatchRules(
          isPrivate = pmObj.optBoolean("isPrivate", true),
          privacy = priv,
          roomName = pmObj.optString("roomName", "Omega Convergence Strike"),
          roomCode = pmObj.optString("roomCode", "GENESIS-774"),
          passcode = pmObj.optString("passcode", "7744"),
          startingWave = pmObj.optInt("startingWave", 1),
          maxPlayers = pmObj.optInt("maxPlayers", 8),
          botFillEnabled = pmObj.optBoolean("botFillEnabled", true),
          botDifficulty = diff,
          worldId = pmObj.optString("worldId", "campaign_origins"),
          areaId = pmObj.optString("areaId", "area_mended_convergence"),
          modeId = pmObj.optString("modeId", "campaign"),
          infiniteAmmo = pmObj.optBoolean("infiniteAmmo", false),
          headshotsOnly = pmObj.optBoolean("headshotsOnly", false),
          bossRush = pmObj.optBoolean("bossRush", false),
          wonderSpawnBoost = pmObj.optBoolean("wonderSpawnBoost", false),
          lowGravity = pmObj.optBoolean("lowGravity", false),
          doubleSpeed = pmObj.optBoolean("doubleSpeed", false),
          vampiricSiphon = pmObj.optBoolean("vampiricSiphon", false),
          doubleBlueEssence = pmObj.optBoolean("doubleBlueEssence", false),
          friendlyFire = pmObj.optBoolean("friendlyFire", false),
          customWaveGoal = pmObj.optInt("customWaveGoal", 0),
        )
      } else fallback.privateMatchRules
    }

    val clanData = safeParse(fallback.clan) {
      val clanObj = root.optJSONObject("clan")
      if (clanObj != null) {
        val membersArr = clanObj.optJSONArray("members")
        val loadedMembers = if (membersArr != null) {
          val mList = ArrayList<ClanMember>(membersArr.length())
          for (i in 0 until membersArr.length()) {
            val mObj = membersArr.optJSONObject(i) ?: continue
            val rId = mObj.optString("rank", ClanRank.RECRUIT.id)
            val rank = ClanRank.entries.firstOrNull { it.id == rId } ?: ClanRank.RECRUIT
            mList.add(
              ClanMember(
                id = mObj.optString("id", "cm_$i"),
                callsign = mObj.optString("callsign", "Operative"),
                rank = rank,
                level = mObj.optInt("level", 1),
                stars = mObj.optInt("stars", 1),
                donatedEssence = mObj.optLong("donatedEssence", 0L),
                online = mObj.optBoolean("online", true),
              )
            )
          }
          mList.ifEmpty { SocialCatalog.defaultClan.members }
        } else SocialCatalog.defaultClan.members
        SocialCatalog.defaultClan.copy(
          id = clanObj.optString("id", SocialCatalog.defaultClan.id),
          name = clanObj.optString("name", SocialCatalog.defaultClan.name),
          tag = clanObj.optString("tag", SocialCatalog.defaultClan.tag),
          motto = clanObj.optString("motto", SocialCatalog.defaultClan.motto),
          level = clanObj.optInt("level", 4).coerceIn(1, ProgressionCatalog.MAX_CLAN_LEVEL),
          tier = clanObj.optInt("tier", 4).coerceIn(1, PowerTier.MAX_TIER),
          starLevel = clanObj.optInt("starLevel", 3).coerceIn(1, StarRankMeta.MAX_STARS),
          totalEssenceDonated = clanObj.optLong("totalEssenceDonated", 18500L),
          leaderCallsign = clanObj.optString("leaderCallsign", SocialCatalog.defaultClan.leaderCallsign),
          colorHex = clanObj.optLong("colorHex", SocialCatalog.defaultClan.colorHex),
          members = loadedMembers,
        )
      } else fallback.clan
    }

    val friendsList = safeParse(fallback.friends) {
      val friendsArray = root.optJSONArray("friends")
      if (friendsArray != null) {
        val list = ArrayList<FriendEntry>(friendsArray.length())
        for (i in 0 until friendsArray.length()) {
          val fObj = friendsArray.optJSONObject(i) ?: continue
          val statusId = fObj.optString("status", FriendOnlineStatus.ONLINE_LOBBY.id)
          val status = FriendOnlineStatus.entries.firstOrNull { it.id == statusId } ?: FriendOnlineStatus.ONLINE_LOBBY
          val tag = fObj.optString("clanTag", "").ifEmpty { null }
          val matchId = fObj.optString("currentMatchId", "").ifEmpty { null }
          list.add(
            FriendEntry(
              id = fObj.optString("id", "f_$i"),
              callsign = fObj.optString("callsign", "Operator_$i"),
              level = fObj.optInt("level", 25),
              stars = fObj.optInt("stars", 5),
              tier = fObj.optInt("tier", 4),
              avatarThemeId = fObj.optString("avatarThemeId", "vanguard_cyber"),
              status = status,
              statusDetail = fObj.optString("statusDetail", "In Lobby"),
              currentMatchId = matchId,
              clanTag = tag,
              isFavorite = fObj.optBoolean("isFavorite", false),
            )
          )
        }
        list
      } else fallback.friends
    }

    val loadedChats = safeParse(fallback.chatMessages) {
      val chatArray = root.optJSONArray("chatMessages")
      if (chatArray != null) {
        val list = ArrayList<ChatMessage>(chatArray.length())
        for (i in 0 until chatArray.length()) {
          val mObj = chatArray.optJSONObject(i) ?: continue
          val chId = mObj.optString("channel", ChatChannel.GENERAL.id)
          val ch = ChatChannel.entries.firstOrNull { it.id == chId } ?: ChatChannel.GENERAL
          list.add(
            ChatMessage(
              id = mObj.optString("id", "msg_$i"),
              channel = ch,
              senderCallsign = mObj.optString("senderCallsign", "Operator"),
              senderClanTag = mObj.optString("senderClanTag", "").ifEmpty { null },
              senderStars = mObj.optInt("senderStars", 1),
              targetFriendId = mObj.optString("targetFriendId", "").ifEmpty { null },
              content = mObj.optString("content", ""),
              timestampMillis = mObj.optLong("timestampMillis", System.currentTimeMillis()),
              isTacticalQuickChat = mObj.optBoolean("isTacticalQuickChat", false),
              isSystemMessage = mObj.optBoolean("isSystemMessage", false),
            )
          )
        }
        list
      } else fallback.chatMessages
    }

    val loadedJoinedMatch = safeParse(fallback.joinedMatch) {
      val jmObj = root.optJSONObject("joinedMatch")
      if (jmObj != null) {
        val privId = jmObj.optString("privacy", MatchPrivacy.PUBLIC.id)
        val priv = MatchPrivacy.entries.firstOrNull { it.id == privId } ?: MatchPrivacy.PUBLIC
        val muts = mutableListOf<String>()
        jmObj.optJSONArray("activeMutators")?.let { arr ->
          for (i in 0 until arr.length()) muts.add(arr.optString(i))
        }
        MultiverseMatchListing(
          id = jmObj.optString("id", ""),
          roomName = jmObj.optString("roomName", "Match"),
          hostCallsign = jmObj.optString("hostCallsign", "Host"),
          hostLevel = jmObj.optInt("hostLevel", 1),
          hostStars = jmObj.optInt("hostStars", 1),
          worldId = jmObj.optString("worldId", "campaign_origins"),
          worldTitle = jmObj.optString("worldTitle", "Genesis Fracture"),
          modeId = jmObj.optString("modeId", "campaign"),
          modeLabel = jmObj.optString("modeLabel", "Story Campaign"),
          areaId = jmObj.optString("areaId", "area_mended_convergence"),
          areaName = jmObj.optString("areaName", "The Mended Convergence"),
          currentPlayers = jmObj.optInt("currentPlayers", 1),
          maxPlayers = jmObj.optInt("maxPlayers", 8),
          privacy = priv,
          roomCode = jmObj.optString("roomCode", "CONV-101"),
          passcode = jmObj.optString("passcode", "").ifEmpty { null },
          pingMs = jmObj.optInt("pingMs", 20),
          currentWave = jmObj.optInt("currentWave", 1),
          activeMutators = muts,
          clanTag = jmObj.optString("clanTag", "").ifEmpty { null },
          regionId = jmObj.optString("regionId", WorldRegion.NA_EAST.id),
          serverId = jmObj.optString("serverId", "srv_na_east_1"),
        )
      } else null
    }

    val worldColDiffMap = safeParse(fallback.worldCollectionDifficultyMap) {
      val map = mutableMapOf<String, String>()
      root.optJSONObject("worldCollectionDifficultyMap")?.let { obj ->
        obj.keys().forEach { k ->
          map[k] = CollectionDifficulty.fromId(obj.optString(k, CollectionDifficulty.OPERATIVE.id)).id
        }
        map
      } ?: fallback.worldCollectionDifficultyMap
    }

    val customMapsList = safeParse(fallback.customGameMaps) {
      val arr = root.optJSONArray("customGameMaps")
      if (arr != null) {
        val list = ArrayList<UserCustomGameMapSpec>(arr.length())
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val cat = CustomMapGameCategory.fromId(obj.optString("gameCategory", CustomMapGameCategory.BO3.id))
          val zones = mutableListOf<String>()
          obj.optJSONArray("buildingNames")?.let { zArr ->
            for (z in 0 until zArr.length()) {
              val s = zArr.optString(z).trim()
              if (s.isNotEmpty()) zones.add(s)
            }
          }
          list.add(
            UserCustomGameMapSpec(
              id = obj.optString("id", "area_bo3_custom_$i").ifBlank { "area_bo3_custom_$i" },
              mapCode = obj.optString("mapCode", "zm_custom_$i"),
              title = obj.optString("title", "Custom Map $i"),
              gameCategory = cat,
              description = obj.optString("description", "${cat.label} custom map."),
              buildingNames = zones.ifEmpty { cat.defaultZones },
              wonderWeapon = obj.optString("wonderWeapon", cat.defaultWonderWeapon),
              accentHex = obj.optLong("accentHex", cat.defaultAccentHex),
              secondaryHex = obj.optLong("secondaryHex", 0xFF00E5FF),
              isUserCreated = obj.optBoolean("isUserCreated", true),
              createdAtMillis = obj.optLong("createdAtMillis", 0L),
            )
          )
        }
        list.distinctBy { it.id }
      } else fallback.customGameMaps
    }

    val customWeaponsList = safeParse(fallback.customWeapons) {
      val arr = root.optJSONArray("customWeapons")
      if (arr != null) {
        val list = ArrayList<UserCustomWeaponSpec>(arr.length())
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val cat = CustomMapGameCategory.fromId(obj.optString("gameCategory", CustomMapGameCategory.INFAMOUS.id))
          list.add(
            UserCustomWeaponSpec(
              id = obj.optString("id", "wpn_custom_$i").ifBlank { "wpn_custom_$i" },
              weaponCode = obj.optString("weaponCode", "wpn_custom_$i"),
              name = obj.optString("name", "Custom Armament $i"),
              gameCategory = cat,
              description = obj.optString("description", "${cat.label} forged custom weapon."),
              damageTypeId = obj.optString("damageTypeId", "arc"),
              baseDamage = obj.optInt("baseDamage", 4).coerceIn(1, 30),
              baseCadenceTicks = obj.optDouble("baseCadenceTicks", 8.5).toFloat().coerceIn(3.5f, 18.0f),
              baseProjectileSpeed = obj.optDouble("baseProjectileSpeed", 0.025).toFloat().coerceIn(0.015f, 0.045f),
              baseSpreadCount = obj.optInt("baseSpreadCount", 2).coerceIn(1, 7),
              basePierce = obj.optInt("basePierce", 2).coerceIn(0, 8),
              defaultTracerHex = obj.optLong("defaultTracerHex", cat.defaultAccentHex),
              isUserCreated = obj.optBoolean("isUserCreated", true),
              createdAtMillis = obj.optLong("createdAtMillis", 0L),
            )
          )
        }
        list.distinctBy { it.id }
      } else fallback.customWeapons
    }

    val customModeCfg = safeParse(fallback.collectionCustomMode) {
      root.optJSONObject("collectionCustomMode")?.let { obj ->
        val parsedScope = CollectionCustomScope.fromId(obj.optString("scope", fallback.collectionCustomMode.scope.id))
        val rawAreaId = obj.optString("targetAreaId", fallback.collectionCustomMode.targetAreaId)
        val allowedAreaIds = CollectionCustomModeConfig.scopedAreas(parsedScope, customMapsList).map { it.id }.toSet()
        val safeAreaId = if (rawAreaId in allowedAreaIds) rawAreaId else (allowedAreaIds.firstOrNull() ?: "area_bo3_the_giant")
        CollectionCustomModeConfig(
          enabled = obj.optBoolean("enabled", fallback.collectionCustomMode.enabled),
          safeCollectionGuard = obj.optBoolean("safeCollectionGuard", fallback.collectionCustomMode.safeCollectionGuard),
          scope = parsedScope,
          targetAreaId = safeAreaId,
          enemyHpBoostPercent = obj.optInt("enemyHpBoostPercent", fallback.collectionCustomMode.enemyHpBoostPercent).coerceIn(0, 100),
          enemyArmorFlood = obj.optBoolean("enemyArmorFlood", fallback.collectionCustomMode.enemyArmorFlood),
          powerAndCooldownSuppression = obj.optBoolean("powerAndCooldownSuppression", fallback.collectionCustomMode.powerAndCooldownSuppression),
          essenceAndShardDrain = obj.optBoolean("essenceAndShardDrain", fallback.collectionCustomMode.essenceAndShardDrain),
          shieldAttritionLeak = obj.optBoolean("shieldAttritionLeak", fallback.collectionCustomMode.shieldAttritionLeak),
          collectionAmbushSurge = obj.optBoolean("collectionAmbushSurge", fallback.collectionCustomMode.collectionAmbushSurge),
          disableWonderDrops = obj.optBoolean("disableWonderDrops", fallback.collectionCustomMode.disableWonderDrops),
        )
      } ?: fallback.collectionCustomMode
    }

    val protRanks = safeParse(fallback.protectionRanks) {
      val map = mutableMapOf<String, Int>()
      root.optJSONObject("protectionRanks")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 0).coerceIn(0, ProgressionCatalog.MAX_PROT_RANK) }
        map
      } ?: fallback.protectionRanks
    }
    val protStars = safeParse(fallback.protectionStarMap) {
      val map = mutableMapOf<String, Int>()
      root.optJSONObject("protectionStarMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, StarRankMeta.MAX_STARS) }
        map
      } ?: fallback.protectionStarMap
    }
    val perkStars = safeParse(fallback.perkStarMap) {
      val map = mutableMapOf<String, Int>()
      root.optJSONObject("perkStarMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, StarRankMeta.MAX_STARS) }
        map
      } ?: fallback.perkStarMap
    }
    val customPerksList = safeParse(fallback.customPerks) {
      val arr = root.optJSONArray("customPerks")
      if (arr != null) {
        val list = ArrayList<UserCustomPerkSpec>(arr.length())
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          list.add(
            UserCustomPerkSpec(
              id = obj.optString("id", "cperk_$i").ifBlank { "cperk_$i" },
              name = obj.optString("name", "Custom Perk $i"),
              description = obj.optString("description", ""),
              accentHex = obj.optLong("accentHex", 0xFF00E676L),
              shieldBonusPerRank = obj.optInt("shieldBonusPerRank", 25).coerceIn(0, 200),
              damagePctPerRank = obj.optInt("damagePctPerRank", 6).coerceIn(0, 50),
              fireRatePctPerRank = obj.optInt("fireRatePctPerRank", 6).coerceIn(0, 50),
              cooldownHastePctPerRank = obj.optInt("cooldownHastePctPerRank", 8).coerceIn(0, 60),
              moveSpeedPctPerRank = obj.optInt("moveSpeedPctPerRank", 3).coerceIn(0, 30),
              regenPerSecPerRank = obj.optDouble("regenPerSecPerRank", 1.0).toFloat().coerceIn(0f, 15f),
              siphonPerKillPerRank = obj.optInt("siphonPerKillPerRank", 1).coerceIn(0, 20),
              tier = obj.optInt("tier", 1).coerceIn(1, PowerTier.MAX_TIER),
              maxTier = obj.optInt("maxTier", PowerTier.MAX_TIER).coerceIn(1, PowerTier.MAX_TIER),
              starLevel = obj.optInt("starLevel", 1).coerceIn(1, StarRankMeta.MAX_STARS),
            )
          )
        }
        list.distinctBy { it.id }
      } else fallback.customPerks
    }
    val equippedCustomPerkSet = safeParse(fallback.equippedCustomPerkIds) {
      val set = mutableSetOf<String>()
      root.optJSONArray("equippedCustomPerkIds")?.let { arr ->
        for (i in 0 until arr.length()) {
          val id = arr.optString(i)
          if (id.isNotBlank()) set.add(id)
        }
        set
      } ?: fallback.equippedCustomPerkIds
    }
    val gearCustomMap = safeParse(fallback.gearCustomizations) {
      val map = mutableMapOf<String, GearSlotCustomization>()
      root.optJSONObject("gearCustomizations")?.let { obj ->
        obj.keys().forEach { k ->
          val gObj = obj.optJSONObject(k)
          if (gObj != null) {
            map[k] = GearSlotCustomization(
              slotId = gObj.optString("slotId", k),
              customTitle = gObj.optString("customTitle", ""),
              alloyId = gObj.optString("alloyId", "titanium"),
              focusId = gObj.optString("focusId", "balanced"),
              elementAffinityId = gObj.optString("elementAffinityId", "kinetic"),
              accentHex = gObj.optLong("accentHex", 0xFF00E5FFL),
            )
          }
        }
        map
      } ?: fallback.gearCustomizations
    }
    val customGearList = safeParse(fallback.customGear) {
      val arr = root.optJSONArray("customGear")
      val parsed = if (arr != null) {
        val list = ArrayList<UserCustomGearSpec>(arr.length())
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val slotId = obj.optString("slot", GearSlot.HELMET.id)
          val slot = GearSlot.entries.firstOrNull { it.id == slotId } ?: GearSlot.HELMET
          list.add(
            UserCustomGearSpec(
              id = obj.optString("id", "cgear_$i").ifBlank { "cgear_$i" },
              slot = slot,
              name = obj.optString("name", "Custom ${slot.displayName}"),
              description = obj.optString("description", ""),
              elementAffinityId = obj.optString("elementAffinityId", "kinetic"),
              accentHex = obj.optLong("accentHex", 0xFF00E5FFL),
              bonusShield = obj.optInt("bonusShield", 30).coerceIn(0, 300),
              bonusDamagePct = obj.optInt("bonusDamagePct", 8).coerceIn(0, 60),
              bonusRatePct = obj.optInt("bonusRatePct", 6).coerceIn(0, 60),
              bonusCooldownHastePct = obj.optInt("bonusCooldownHastePct", 10).coerceIn(0, 80),
              bonusProtectionPct = obj.optInt("bonusProtectionPct", 4).coerceIn(0, 40),
              tier = obj.optInt("tier", 1).coerceIn(1, PowerTier.MAX_TIER),
              maxTier = obj.optInt("maxTier", PowerTier.MAX_TIER).coerceIn(1, PowerTier.MAX_TIER),
              starLevel = obj.optInt("starLevel", 1).coerceIn(1, StarRankMeta.MAX_STARS),
            )
          )
        }
        list.distinctBy { it.id }
      } else fallback.customGear
      (parsed + ProgressionCatalog.creatorZombieAetherGearPresets).distinctBy { it.id }
    }
    val equippedCustomGear = safeParse(fallback.equippedCustomGearMap) {
      val map = mutableMapOf<String, String>()
      root.optJSONObject("equippedCustomGearMap")?.let { obj ->
        obj.keys().forEach { k ->
          val v = obj.optString(k)
          if (v.isNotBlank()) map[k] = v
        }
        map
      } ?: fallback.equippedCustomGearMap
    }.ifEmpty { ProgressionCatalog.defaultEquippedCreatorGearMap() }
    val customEquipList = safeParse(fallback.customEquipment) {
      val arr = root.optJSONArray("customEquipment")
      if (arr != null) {
        val list = ArrayList<UserCustomEquipmentSpec>(arr.length())
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val stId = obj.optString("slotType", EquipmentSlotType.LETHAL.id)
          val st = EquipmentSlotType.entries.firstOrNull { it.id == stId } ?: EquipmentSlotType.LETHAL
          list.add(
            UserCustomEquipmentSpec(
              id = obj.optString("id", "ceq_$i").ifBlank { "ceq_$i" },
              slotType = st,
              name = obj.optString("name", "Custom ${st.displayName}"),
              description = obj.optString("description", ""),
              damageTypeId = obj.optString("damageTypeId", "explosive"),
              baseDamageOrEffect = obj.optDouble("baseDamageOrEffect", 200.0).toFloat().coerceIn(0f, 1500f),
              cooldownSeconds = obj.optDouble("cooldownSeconds", 5.5).toFloat().coerceIn(1.5f, 25f),
              cooldownHastePct = obj.optInt("cooldownHastePct", 6).coerceIn(0, 60),
              accentHex = obj.optLong("accentHex", 0xFFFF6D00L),
              tier = obj.optInt("tier", 1).coerceIn(1, PowerTier.MAX_TIER),
              maxTier = obj.optInt("maxTier", PowerTier.MAX_TIER).coerceIn(1, PowerTier.MAX_TIER),
              starLevel = obj.optInt("starLevel", 1).coerceIn(1, StarRankMeta.MAX_STARS),
            )
          )
        }
        list.distinctBy { it.id }
      } else fallback.customEquipment
    }

    val parsedModeTierMap = safeParse(fallback.modeTierMap) {
      val map = mutableMapOf<String, Int>()
      root.optJSONObject("modeTierMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, PowerTier.MAX_TIER) }
      }
      map
    }
    val parsedModeStarMap = safeParse(fallback.modeStarMap) {
      val map = mutableMapOf<String, Int>()
      root.optJSONObject("modeStarMap")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, StarRankMeta.MAX_STARS) }
      }
      map
    }

    val parsedEnvRes = safeParse(fallback.environmentalResources) {
      val map = defaultEnvironmentalResources().toMutableMap()
      root.optJSONObject("environmentalResources")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optLong(k, map[k] ?: 0L).coerceAtLeast(0L) }
      }
      map
    }

    val parsedSystemShards = safeParse(fallback.systemShards) {
      val map = defaultSystemShards().toMutableMap()
      root.optJSONObject("systemShards")?.let { obj ->
        obj.keys().forEach { k -> map[k] = obj.optLong(k, map[k] ?: 0L).coerceAtLeast(0L) }
      }
      map
    }

    val parsedMagicState = safeParse(fallback.magicState) {
      val mObj = root.optJSONObject("magicState")
      if (mObj != null) {
        val unlockedSpells = mutableSetOf<String>().apply {
          mObj.optJSONArray("unlockedSpells")?.let { arr ->
            for (i in 0 until arr.length()) {
              val s = arr.optString(i)
              if (s.isNotBlank()) add(s)
            }
          }
          if (isEmpty()) addAll(fallback.magicState.unlockedSpells)
        }
        val spellTiers = mutableMapOf<String, Int>().apply {
          unlockedSpells.forEach { put(it, 1) }
          mObj.optJSONObject("spellTiers")?.let { stObj ->
            stObj.keys().forEach { k -> put(k, stObj.optInt(k, 1).coerceIn(1, PowerTier.MAX_TIER)) }
          }
        }
        MagicSystemState(
          unlocked = mObj.optBoolean("unlocked", true),
          activeSpellId = mObj.optString("activeSpellId", fallback.magicState.activeSpellId).ifBlank { "spell_abyssal_nova" },
          manaCurrent = mObj.optDouble("manaCurrent", fallback.magicState.manaCurrent.toDouble()).toFloat().coerceIn(0f, 350f),
          manaMax = mObj.optDouble("manaMax", fallback.magicState.manaMax.toDouble()).toFloat().coerceIn(50f, 350f),
          magicTier = mObj.optInt("magicTier", fallback.magicState.magicTier).coerceIn(1, PowerTier.MAX_TIER),
          difficultyScalingFactor = mObj.optDouble("difficultyScalingFactor", fallback.magicState.difficultyScalingFactor.toDouble()).toFloat().coerceIn(1f, 3.5f),
          overchargeDifficultyEnabled = mObj.optBoolean("overchargeDifficultyEnabled", fallback.magicState.overchargeDifficultyEnabled),
          unlockedSpells = unlockedSpells,
          spellTiers = spellTiers,
        )
      } else fallback.magicState
    }

    val parsedBaseState = safeParse(fallback.playerBaseState) {
      val bObj = root.optJSONObject("playerBaseState")
      if (bObj != null) {
        val baseObj = PlayerBaseState(
          baseName = bObj.optString("baseName", fallback.playerBaseState.baseName).ifBlank { "Nexus Command Citadel" },
          layoutTheme = bObj.optString("layoutTheme", fallback.playerBaseState.layoutTheme).ifBlank { "citadel" },
          primaryHex = bObj.optLong("primaryHex", fallback.playerBaseState.primaryHex),
          wallTier = bObj.optInt("wallTier", 1).coerceIn(1, PowerTier.MAX_TIER),
          turretTier = bObj.optInt("turretTier", 1).coerceIn(1, PowerTier.MAX_TIER),
          shieldGeneratorTier = bObj.optInt("shieldGeneratorTier", 1).coerceIn(1, PowerTier.MAX_TIER),
          resourceVaultTier = bObj.optInt("resourceVaultTier", 1).coerceIn(1, PowerTier.MAX_TIER),
          storedGold = bObj.optLong("storedGold", fallback.playerBaseState.storedGold).coerceAtLeast(0L),
          storedDiamonds = bObj.optLong("storedDiamonds", fallback.playerBaseState.storedDiamonds).coerceAtLeast(0L),
          storedTuuCash = bObj.optLong("storedTuuCash", fallback.playerBaseState.storedTuuCash).coerceAtLeast(0L),
          baseHpCurrent = bObj.optInt("baseHpCurrent", 1000).coerceAtLeast(1),
          baseHpMax = bObj.optInt("baseHpMax", 1000).coerceAtLeast(500),
          underAttack = bObj.optBoolean("underAttack", false),
          attackWaveCountdownSec = bObj.optInt("attackWaveCountdownSec", 240).coerceAtLeast(0),
          defenseSuccessCount = bObj.optInt("defenseSuccessCount", 0).coerceAtLeast(0),
        )
        baseObj.copy(baseHpMax = maxOf(baseObj.baseHpMax, baseObj.maxIntegrity()))
      } else fallback.playerBaseState
    }

    val parsedCrystals = safeParse(fallback.crystals) {
      val arr = root.optJSONArray("crystals")
      if (arr != null) {
        val list = ArrayList<CrystalItem>(arr.length())
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          list.add(
            CrystalItem(
              id = obj.optString("id", "crystal_$i").ifBlank { "crystal_$i" },
              name = obj.optString("name", "Resonance Crystal $i"),
              rarity = obj.optString("rarity", "Epic"),
              element = obj.optString("element", "energy"),
              statBonusSummary = obj.optString("statBonusSummary", "+10% DMG & Shield"),
              damageBoostPct = obj.optInt("damageBoostPct", 10).coerceIn(0, 100),
              shieldBoostPct = obj.optInt("shieldBoostPct", 10).coerceIn(0, 100),
              luckBoostPct = obj.optInt("luckBoostPct", 5).coerceIn(0, 100),
              socketedSlot = obj.optString("socketedSlot", "").ifBlank { null },
              level = obj.optInt("level", 1).coerceIn(1, PowerTier.MAX_TIER),
            )
          )
        }
        list.ifEmpty { defaultCrystals() }.distinctBy { it.id }
      } else fallback.crystals
    }

    return OperatorProgressionState(
      playerXp = root.optLong("playerXp", fallback.playerXp).coerceAtLeast(0L),
      playerPrestige = root.optInt("playerPrestige", fallback.playerPrestige).coerceAtLeast(0),
      characterTier = root.optInt("characterTier", fallback.characterTier).coerceIn(1, PowerTier.MAX_TIER),
      characterStarLevel = root.optInt("characterStarLevel", fallback.characterStarLevel).coerceIn(1, StarRankMeta.MAX_STARS),
      weaponXpMap = wpnXp.ifEmpty { mapOf("wpn_pulse_carbine" to 0L) },
      weaponPrestigeMap = wpnPrestige,
      weaponTierMap = wpnTierMap,
      weaponStarMap = wpnStarMap,
      wonderTierMap = wonderTierMap,
      wonderStarMap = wonderStarMap,
      armoryItemStarMap = armoryStarMap,
      modeTierMap = parsedModeTierMap,
      modeStarMap = parsedModeStarMap,
      gearMap = gearMap,
      gearPrestige = root.optInt("gearPrestige", fallback.gearPrestige).coerceAtLeast(0),
      equipmentMap = equipmentMap,
      equippedWonderIds = wonderList,
      equippedLethalIds = lethalList,
      equippedTacticalIds = tacticalList,
      redEssence = root.optLong("redEssence", fallback.redEssence).coerceAtLeast(0L),
      yellowEssence = root.optLong("yellowEssence", fallback.yellowEssence).coerceAtLeast(0L),
      greenEssence = root.optLong("greenEssence", fallback.greenEssence).coerceAtLeast(0L),
      combinedEssence = root.optLong("combinedEssence", fallback.combinedEssence).coerceAtLeast(0L),
      blastShards = root.optLong("blastShards", fallback.blastShards).coerceAtLeast(0L),
      perkRanks = perkRanks,
      blastCoreInventory = coreInv,
      blastCoreLevels = coreLvls,
      equippedOutfitId = root.optString("equippedOutfitId", fallback.equippedOutfitId),
      outfitStates = outfitStates,
      activeAreaId = root.optString("activeAreaId", fallback.activeAreaId),
      areaStates = areaStates,
      vacuumBot = vacBot,
      weaponCustom = wc,
      wonderCustom = wonC,
      characterCustom = cc,
      levelCustom = lc,
      lobbyConfig = lobbyCfg,
      activePassVersion = activePass,
      unlockedPassVersions = unlockedPasses,
      battlePassXp = root.optLong("battlePassXp", fallback.battlePassXp).coerceAtLeast(0L),
      battlePassPrestige = root.optInt("battlePassPrestige", fallback.battlePassPrestige).coerceAtLeast(0),
      bpCycleCount = root.optInt("bpCycleCount", fallback.bpCycleCount).coerceAtLeast(0),
      claimedPassTiers = claimedTiers,
      challengeProgress = chProg,
      claimedChallenges = claimedChallenges,
      challengePrestige = root.optInt("challengePrestige", fallback.challengePrestige).coerceAtLeast(0),
      skillNodes = skillNodes,
      wonderWeaponsPickedTotal = root.optLong("wonderPicked", fallback.wonderWeaponsPickedTotal).coerceAtLeast(0L),
      claimedAchievements = claimedAchievements,
      missionProgress = missionProgress,
      claimedMissions = claimedMissions,
      totalElimsLifetime = root.optLong("totalElimsLifetime", fallback.totalElimsLifetime).coerceAtLeast(0L),
      totalBossesLifetime = root.optLong("totalBossesLifetime", fallback.totalBossesLifetime).coerceAtLeast(0L),
      totalMatchesPlayed = root.optLong("totalMatchesPlayed", fallback.totalMatchesPlayed).coerceAtLeast(0L),
      totalBlueEssenceSpent = root.optLong("totalBlueEssenceSpent", fallback.totalBlueEssenceSpent).coerceAtLeast(0L),
      highestPackTierReached = root.optInt("highestPackTierReached", fallback.highestPackTierReached).coerceIn(1, PowerTier.MAX_TIER),
      largestLobbyPlayed = root.optInt("largestLobbyPlayed", fallback.largestLobbyPlayed).coerceIn(1, 250),
      storePurchasesCount = root.optInt("storePurchasesCount", fallback.storePurchasesCount).coerceAtLeast(0),
      purchasedStoreItems = purchasedStoreItems,
      xpBoosterMatchesRemaining = root.optInt("xpBoosterMatchesRemaining", fallback.xpBoosterMatchesRemaining).coerceAtLeast(0),
      botDamageBoostPct = root.optInt("botDamageBoostPct", fallback.botDamageBoostPct).coerceAtLeast(0),
      privateMatchRules = pmRules,
      friends = friendsList,
      chatMessages = loadedChats,
      clan = clanData,
      joinedMatch = loadedJoinedMatch,
      outfitRarityMap = outfitRarityMap,
      outfitShardMap = outfitShardMap,
      discoveredCatWorlds = discoveredCatWorlds,
      lootCratesOpenedTotal = root.optLong("lootCratesOpenedTotal", fallback.lootCratesOpenedTotal).coerceAtLeast(0L),
      activeNgPlusTier = root.optInt("activeNgPlusTier", fallback.activeNgPlusTier).coerceIn(0, NewGamePlusVersion.MAX_NG_PLUS_TIER),
      unlockedNgPlusTier = root.optInt("unlockedNgPlusTier", fallback.unlockedNgPlusTier).coerceIn(0, NewGamePlusVersion.MAX_NG_PLUS_TIER),
      ngPlusCompletions = root.optInt("ngPlusCompletions", fallback.ngPlusCompletions).coerceAtLeast(0),
      worldCollectionDifficultyMap = worldColDiffMap,
      collectionCustomMode = customModeCfg,
      customGameMaps = customMapsList,
      customWeapons = customWeaponsList,
      protectionRanks = protRanks,
      protectionStarMap = protStars,
      perkStarMap = perkStars,
      customPerks = customPerksList,
      equippedCustomPerkIds = equippedCustomPerkSet,
      gearCustomizations = gearCustomMap,
      customGear = customGearList,
      equippedCustomGearMap = equippedCustomGear,
      customEquipment = customEquipList,
      blastShardTier = root.optInt("blastShardTier", fallback.blastShardTier).coerceIn(1, PowerTier.MAX_TIER),
      companionBuffTier = root.optInt("companionBuffTier", fallback.companionBuffTier).coerceIn(1, PowerTier.MAX_TIER),
      gold = root.optLong("gold", fallback.gold).coerceAtLeast(0L),
      diamonds = root.optLong("diamonds", fallback.diamonds).coerceAtLeast(0L),
      tuuCash = root.optLong("tuuCash", fallback.tuuCash).coerceAtLeast(0L),
      environmentalResources = parsedEnvRes,
      systemShards = parsedSystemShards,
      magicState = parsedMagicState,
      playerBaseState = parsedBaseState,
      crystals = parsedCrystals,
    )
  }

  private fun mergeStatesPreferHigher(
    base: OperatorProgressionState,
    incoming: OperatorProgressionState,
  ): OperatorProgressionState {
    fun mergeLongMap(a: Map<String, Long>, b: Map<String, Long>): Map<String, Long> =
      (a.keys + b.keys).associateWith { k -> maxOf(a[k] ?: 0L, b[k] ?: 0L) }

    fun mergeIntMap(a: Map<String, Int>, b: Map<String, Int>, floor: Int = 1): Map<String, Int> =
      (a.keys + b.keys).associateWith { k -> maxOf(a[k] ?: floor, b[k] ?: floor) }

    val mergedGear = (base.gearMap.keys + incoming.gearMap.keys).associateWith { k ->
      val a = base.gearMap[k]
      val b = incoming.gearMap[k]
      when {
        a == null && b != null -> b
        b == null && a != null -> a
        a != null && b != null -> a.copy(
          level = maxOf(a.level, b.level),
          prestige = maxOf(a.prestige, b.prestige),
          xp = maxOf(a.xp, b.xp),
          tier = maxOf(a.tier, b.tier),
          starLevel = maxOf(a.starLevel, b.starLevel),
        )
        else -> a ?: b ?: GearState(slot = GearSlot.entries.firstOrNull { it.id == k } ?: GearSlot.HELMET)
      }
    }

    val mergedEquip = (base.equipmentMap.keys + incoming.equipmentMap.keys).associateWith { k ->
      val a = base.equipmentMap[k]
      val b = incoming.equipmentMap[k]
      when {
        a == null && b != null -> b
        b == null && a != null -> a
        a != null && b != null -> a.copy(
          level = maxOf(a.level, b.level),
          prestige = maxOf(a.prestige, b.prestige),
          xp = maxOf(a.xp, b.xp),
          tier = maxOf(a.tier, b.tier),
          starLevel = maxOf(a.starLevel, b.starLevel),
        )
        else -> a ?: b ?: EquipmentState(slotId = k, equippedItemId = "frag")
      }
    }

    val mergedSkills = (base.skillNodes.keys + incoming.skillNodes.keys).associateWith { k ->
      val a = base.skillNodes[k]
      val b = incoming.skillNodes[k]
      when {
        a == null && b != null -> b
        b == null && a != null -> a
        a != null && b != null -> a.copy(
          unlocked = a.unlocked || b.unlocked,
          tier = maxOf(a.tier, b.tier),
          starLevel = maxOf(a.starLevel, b.starLevel),
        )
        else -> a ?: b ?: SkillNode(id = k, title = k, description = "", category = "COMBAT", costEssence = 100L)
      }
    }

    val mergedOutfits = (base.outfitStates.keys + incoming.outfitStates.keys).associateWith { k ->
      val a = base.outfitStates[k]
      val b = incoming.outfitStates[k]
      when {
        a == null && b != null -> b
        b == null && a != null -> a
        a != null && b != null -> a.copy(
          level = maxOf(a.level, b.level),
          tier = maxOf(a.tier, b.tier),
          skillRank = maxOf(a.skillRank, b.skillRank),
          gearRank = maxOf(a.gearRank, b.gearRank),
          equipmentRank = maxOf(a.equipmentRank, b.equipmentRank),
          starLevel = maxOf(a.starLevel, b.starLevel),
        )
        else -> a ?: b ?: OutfitProgressState(k)
      }
    }

    val mergedAreas = (base.areaStates.keys + incoming.areaStates.keys).associateWith { k ->
      val a = base.areaStates[k]
      val b = incoming.areaStates[k]
      when {
        a == null && b != null -> b
        b == null && a != null -> a
        a != null && b != null -> a.copy(
          ageYears = maxOf(a.ageYears, b.ageYears),
          rebirthCycles = maxOf(a.rebirthCycles, b.rebirthCycles),
          powerGridTier = maxOf(a.powerGridTier, b.powerGridTier),
          defenseTurretTier = maxOf(a.defenseTurretTier, b.defenseTurretTier),
          trapDensityTier = maxOf(a.trapDensityTier, b.trapDensityTier),
          lootRichnessTier = maxOf(a.lootRichnessTier, b.lootRichnessTier),
          starLevel = maxOf(a.starLevel, b.starLevel),
        )
        else -> a ?: b ?: CustomAreaState(k)
      }
    }

    return base.copy(
      playerXp = maxOf(base.playerXp, incoming.playerXp),
      playerPrestige = maxOf(base.playerPrestige, incoming.playerPrestige),
      characterTier = maxOf(base.characterTier, incoming.characterTier),
      characterStarLevel = maxOf(base.characterStarLevel, incoming.characterStarLevel),
      weaponXpMap = mergeLongMap(base.weaponXpMap, incoming.weaponXpMap),
      weaponPrestigeMap = mergeIntMap(base.weaponPrestigeMap, incoming.weaponPrestigeMap, 0),
      weaponTierMap = mergeIntMap(base.weaponTierMap, incoming.weaponTierMap, 1),
      weaponStarMap = mergeIntMap(base.weaponStarMap, incoming.weaponStarMap, 1),
      wonderTierMap = mergeIntMap(base.wonderTierMap, incoming.wonderTierMap, 1),
      wonderStarMap = mergeIntMap(base.wonderStarMap, incoming.wonderStarMap, 1),
      armoryItemStarMap = mergeIntMap(base.armoryItemStarMap, incoming.armoryItemStarMap, 1),
      modeTierMap = mergeIntMap(base.modeTierMap, incoming.modeTierMap, 1),
      modeStarMap = mergeIntMap(base.modeStarMap, incoming.modeStarMap, 1),
      gearMap = mergedGear,
      gearPrestige = maxOf(base.gearPrestige, incoming.gearPrestige),
      equipmentMap = mergedEquip,
      redEssence = maxOf(base.redEssence, incoming.redEssence),
      yellowEssence = maxOf(base.yellowEssence, incoming.yellowEssence),
      greenEssence = maxOf(base.greenEssence, incoming.greenEssence),
      combinedEssence = maxOf(base.combinedEssence, incoming.combinedEssence),
      blastShards = maxOf(base.blastShards, incoming.blastShards),
      perkRanks = mergeIntMap(base.perkRanks, incoming.perkRanks, 0),
      blastCoreInventory = mergeLongMap(base.blastCoreInventory, incoming.blastCoreInventory),
      blastCoreLevels = mergeIntMap(base.blastCoreLevels, incoming.blastCoreLevels, 1),
      outfitStates = mergedOutfits,
      areaStates = mergedAreas,
      vacuumBot = base.vacuumBot.copy(
        level = maxOf(base.vacuumBot.level, incoming.vacuumBot.level),
        tier = maxOf(base.vacuumBot.tier, incoming.vacuumBot.tier),
        starLevel = maxOf(base.vacuumBot.starLevel, incoming.vacuumBot.starLevel),
      ),
      unlockedPassVersions = base.unlockedPassVersions + incoming.unlockedPassVersions,
      battlePassXp = maxOf(base.battlePassXp, incoming.battlePassXp),
      battlePassPrestige = maxOf(base.battlePassPrestige, incoming.battlePassPrestige),
      bpCycleCount = maxOf(base.bpCycleCount, incoming.bpCycleCount),
      claimedPassTiers = base.claimedPassTiers + incoming.claimedPassTiers,
      challengeProgress = mergeLongMap(base.challengeProgress, incoming.challengeProgress),
      claimedChallenges = base.claimedChallenges + incoming.claimedChallenges,
      challengePrestige = maxOf(base.challengePrestige, incoming.challengePrestige),
      skillNodes = mergedSkills,
      wonderWeaponsPickedTotal = maxOf(base.wonderWeaponsPickedTotal, incoming.wonderWeaponsPickedTotal),
      claimedAchievements = base.claimedAchievements + incoming.claimedAchievements,
      missionProgress = mergeLongMap(base.missionProgress, incoming.missionProgress),
      claimedMissions = base.claimedMissions + incoming.claimedMissions,
      totalElimsLifetime = maxOf(base.totalElimsLifetime, incoming.totalElimsLifetime),
      totalBossesLifetime = maxOf(base.totalBossesLifetime, incoming.totalBossesLifetime),
      totalMatchesPlayed = maxOf(base.totalMatchesPlayed, incoming.totalMatchesPlayed),
      totalBlueEssenceSpent = maxOf(base.totalBlueEssenceSpent, incoming.totalBlueEssenceSpent),
      highestPackTierReached = maxOf(base.highestPackTierReached, incoming.highestPackTierReached),
      largestLobbyPlayed = maxOf(base.largestLobbyPlayed, incoming.largestLobbyPlayed),
      storePurchasesCount = maxOf(base.storePurchasesCount, incoming.storePurchasesCount),
      purchasedStoreItems = base.purchasedStoreItems + incoming.purchasedStoreItems,
      xpBoosterMatchesRemaining = maxOf(base.xpBoosterMatchesRemaining, incoming.xpBoosterMatchesRemaining),
      botDamageBoostPct = maxOf(base.botDamageBoostPct, incoming.botDamageBoostPct),
      outfitRarityMap = mergeIntMap(base.outfitRarityMap, incoming.outfitRarityMap, 1),
      outfitShardMap = mergeLongMap(base.outfitShardMap, incoming.outfitShardMap),
      discoveredCatWorlds = base.discoveredCatWorlds + incoming.discoveredCatWorlds,
      lootCratesOpenedTotal = maxOf(base.lootCratesOpenedTotal, incoming.lootCratesOpenedTotal),
      unlockedNgPlusTier = maxOf(base.unlockedNgPlusTier, incoming.unlockedNgPlusTier),
      activeNgPlusTier = maxOf(base.activeNgPlusTier, incoming.activeNgPlusTier).coerceAtMost(maxOf(base.unlockedNgPlusTier, incoming.unlockedNgPlusTier)),
      ngPlusCompletions = maxOf(base.ngPlusCompletions, incoming.ngPlusCompletions),
      worldCollectionDifficultyMap = base.worldCollectionDifficultyMap + incoming.worldCollectionDifficultyMap,
      customGameMaps = (base.customGameMaps + incoming.customGameMaps).distinctBy { it.id },
      customWeapons = (base.customWeapons + incoming.customWeapons).distinctBy { it.id },
      protectionRanks = mergeIntMap(base.protectionRanks, incoming.protectionRanks, 0),
      protectionStarMap = mergeIntMap(base.protectionStarMap, incoming.protectionStarMap, 1),
      perkStarMap = mergeIntMap(base.perkStarMap, incoming.perkStarMap, 1),
      customPerks = (base.customPerks + incoming.customPerks).distinctBy { it.id },
      equippedCustomPerkIds = base.equippedCustomPerkIds + incoming.equippedCustomPerkIds,
      gearCustomizations = base.gearCustomizations + incoming.gearCustomizations,
      customGear = (base.customGear + incoming.customGear).distinctBy { it.id },
      equippedCustomGearMap = base.equippedCustomGearMap + incoming.equippedCustomGearMap,
      customEquipment = (base.customEquipment + incoming.customEquipment).distinctBy { it.id },
      blastShardTier = maxOf(base.blastShardTier, incoming.blastShardTier),
      companionBuffTier = maxOf(base.companionBuffTier, incoming.companionBuffTier),
      gold = maxOf(base.gold, incoming.gold),
      diamonds = maxOf(base.diamonds, incoming.diamonds),
      tuuCash = maxOf(base.tuuCash, incoming.tuuCash),
      environmentalResources = mergeLongMap(base.environmentalResources, incoming.environmentalResources),
      systemShards = mergeLongMap(base.systemShards, incoming.systemShards),
      magicState = base.magicState.copy(
        magicTier = maxOf(base.magicState.magicTier, incoming.magicState.magicTier),
        manaMax = maxOf(base.magicState.manaMax, incoming.magicState.manaMax),
        manaCurrent = maxOf(base.magicState.manaCurrent, incoming.magicState.manaCurrent),
        unlockedSpells = base.magicState.unlockedSpells + incoming.magicState.unlockedSpells,
        spellTiers = mergeIntMap(base.magicState.spellTiers, incoming.magicState.spellTiers, 1),
      ),
      playerBaseState = base.playerBaseState.copy(
        wallTier = maxOf(base.playerBaseState.wallTier, incoming.playerBaseState.wallTier),
        turretTier = maxOf(base.playerBaseState.turretTier, incoming.playerBaseState.turretTier),
        shieldGeneratorTier = maxOf(base.playerBaseState.shieldGeneratorTier, incoming.playerBaseState.shieldGeneratorTier),
        resourceVaultTier = maxOf(base.playerBaseState.resourceVaultTier, incoming.playerBaseState.resourceVaultTier),
        storedGold = maxOf(base.playerBaseState.storedGold, incoming.playerBaseState.storedGold),
        storedDiamonds = maxOf(base.playerBaseState.storedDiamonds, incoming.playerBaseState.storedDiamonds),
        storedTuuCash = maxOf(base.playerBaseState.storedTuuCash, incoming.playerBaseState.storedTuuCash),
        defenseSuccessCount = maxOf(base.playerBaseState.defenseSuccessCount, incoming.playerBaseState.defenseSuccessCount),
      ),
      crystals = (base.crystals + incoming.crystals)
        .groupBy { it.id }
        .values
        .map { list -> list.maxBy { it.level } },
    )
  }

  private fun load(): OperatorProgressionState {
    var raw = prefs.getString(KEY_STATE_JSON, null)
      ?: prefs.getString(KEY_STATE_JSON_LEGACY, null)
      ?: prefs.getString("progression_state", null)

    if (raw.isNullOrBlank()) {
      val candidates = listOf(
        "${PREFS_PREFIX}_local_operator",
        "${PREFS_PREFIX}_local",
        "${PREFS_PREFIX}_default",
        PREFS_PREFIX,
      )
      for (c in candidates) {
        val p = appContext.getSharedPreferences(c, Context.MODE_PRIVATE)
        val s = p.getString(KEY_STATE_JSON, null)
          ?: p.getString(KEY_STATE_JSON_LEGACY, null)
          ?: p.getString("progression_state", null)
        if (!s.isNullOrBlank()) {
          raw = s
          prefs.edit(commit = true) {
            putString(KEY_STATE_JSON, s)
            putString(KEY_STATE_JSON_LEGACY, s)
          }
          break
        }
      }
    }
    if (raw.isNullOrBlank()) {
      lastLoadedIntegrityValid = true
      return OperatorProgressionState()
    }
    val storedSeal = prefs.getString(KEY_STATE_HASH, null)
    lastLoadedIntegrityValid = storedSeal == null || AppSecurityManager.verifyPayload(raw, storedSeal)
    val parsedPrimary = runCatching {
      parseProgressionJson(JSONObject(raw), OperatorProgressionState())
    }.getOrDefault(OperatorProgressionState())

    if (safeUserId != "local_operator") {
      val localRaw = appContext.getSharedPreferences("${PREFS_PREFIX}_local_operator", Context.MODE_PRIVATE)
        .getString(KEY_STATE_JSON, null)
      if (!localRaw.isNullOrBlank() && localRaw != raw) {
        val localParsed = runCatching {
          parseProgressionJson(JSONObject(localRaw), OperatorProgressionState())
        }.getOrNull()
        if (localParsed != null) {
          return mergeStatesPreferHigher(parsedPrimary, localParsed)
        }
      }
    }
    return parsedPrimary
  }

  data class HeroOutfitResult(
    val nameWithVersion: String,
    val progress: OutfitProgressState,
  )

  companion object {
    const val PREFS_PREFIX = "unknown_os_progression"
    private const val KEY_STATE_JSON = "progression_json_v2"
    private const val KEY_STATE_JSON_LEGACY = "progression_json"
    private const val KEY_STATE_HASH = "progression_sha256_v1"
  }
}

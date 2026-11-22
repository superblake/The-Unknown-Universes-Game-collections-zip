package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.data.model.AchievementState
import com.example.data.model.BattlePassVersion
import com.example.data.model.BlastCoreColor
import com.example.data.model.BotDifficulty
import com.example.data.model.ChallengeState
import com.example.data.model.CharacterCustomization
import com.example.data.model.CharacterRarity
import com.example.data.model.CollectionDifficulty
import com.example.data.model.CustomAreaState
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
import com.example.data.model.StarRankMeta
import com.example.data.model.TradeCurrencyKind
import com.example.data.model.VacuumBotState
import com.example.data.model.WeaponCustomization
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
import com.example.data.model.WonderWeaponKind
import com.example.data.model.WorldDifficultyTier
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
    val spec = ProgressionCatalog.weaponById(nextPrimary)
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
    val clamped = nextConfig.copy(
      targetPlayers = nextConfig.targetPlayers.coerceIn(1, 32),
      humanPlayersOnline = nextConfig.humanPlayersOnline.coerceIn(1, nextConfig.targetPlayers.coerceIn(1, 32)),
    )
    save(current.copy(lobbyConfig = clamped))
  }

  fun updatePrivateMatchRules(transform: (PrivateMatchRules) -> PrivateMatchRules) {
    val nextRules = transform(current.privateMatchRules)
    save(current.copy(privateMatchRules = nextRules))
  }

  fun hostPrivateMatch(rules: PrivateMatchRules, hostCallsign: String): MultiverseMatchListing {
    val activeWorld = UniverseCatalog.worldById(rules.worldId)
    val activeMode = UniverseCatalog.modeById(rules.modeId)
    val activeArea = MultiverseArsenalCatalog.areaById(rules.areaId)
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
    val newLevel = (1 + (nextDonated / 5000L).toInt()).coerceIn(1, 25)
    val newTier = (1 + (newLevel / 2)).coerceIn(1, 25)
    val newStar = (1 + (newLevel / 3)).coerceIn(1, 10)
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
    if (gear.level >= 50) return
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
    if (cur.gearPrestige >= 30) return
    val resetGear = cur.gearMap.mapValues { (_, g) ->
      g.copy(level = 1, xp = 0L, prestige = (g.prestige + 1).coerceAtMost(30))
    }
    save(cur.copy(gearMap = resetGear, gearPrestige = cur.gearPrestige + 1))
  }

  fun upgradeEquipment(slotId: String) {
    val cur = current
    val eq = cur.equipmentState(slotId)
    if (eq.level >= 60) return
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
    if (eq.level < 60 || eq.prestige >= 30) {
      return Result.failure(IllegalStateException("Must reach Level 60 to Prestige Equipment (Max 30)"))
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
    val nextTier = if (st.level % 10 == 0 && st.tier < 25) st.tier + 1 else st.tier
    val nextSt = st.copy(
      level = (st.level + 5).coerceAtMost(1000),
      tier = nextTier.coerceIn(1, 25),
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
      ?: return Result.failure(IllegalStateException("Already at max ${CharacterRarity.MYTHIC.label} rarity!"))
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
    if (curRank >= 20) {
      return Result.failure(IllegalStateException("Outfit $part already at max Rank 20"))
    }
    val cost = UpgradeCosts.outfitSubSystem(curRank)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextSt = when (part) {
      "skill" -> st.copy(skillRank = curRank + 1, tier = (st.tier + if ((curRank + 1) % 2 == 0) 1 else 0).coerceAtMost(25))
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
    val spec = MultiverseArsenalCatalog.areaById(areaId)
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
    if (curTier >= 25) {
      return Result.failure(IllegalStateException("$feature already at max Tier 25"))
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
      level = (cur.vacuumBot.level + 1).coerceAtMost(50),
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

  fun upgradeBlastCore(color: BlastCoreColor): Result<Int> {
    val cur = current
    val count = cur.blastCoreCount(color)
    val curLvl = cur.blastCoreLevel(color)
    if (curLvl >= 20) {
      return Result.failure(IllegalStateException("${color.displayName} is already Max Level 20"))
    }
    if (count < 1L) {
      return Result.failure(IllegalStateException("Need 1 ${color.displayName} (collect in Arena or forge in Trading Post)"))
    }
    val nextInv = cur.blastCoreInventory + (color.id to (count - 1L))
    val nextLvls = cur.blastCoreLevels + (color.id to (curLvl + 1))
    save(cur.copy(blastCoreInventory = nextInv, blastCoreLevels = nextLvls))
    return Result.success(curLvl + 1)
  }

  fun upgradeVacuumBot(currentBlueEssence: Long): Result<Long> {
    val cur = current
    val bot = cur.vacuumBot
    if (bot.tier >= 25 && bot.level >= 50) {
      return Result.failure(IllegalStateException("Vacuum Bot is already Tier 25 Multiversal Max"))
    }
    val cost = UpgradeCosts.vacuumBot(bot.tier)
    if (currentBlueEssence < cost) {
      return Result.failure(IllegalStateException("Need $cost Blue Essence"))
    }
    val nextLvl = (bot.level + 3).coerceAtMost(50)
    val nextTier = (bot.tier + 1).coerceAtMost(25)
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
    if (curTier >= 25) {
      return Result.failure(IllegalStateException("Weapon already at max Tier 25 Multiversal"))
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
    if (curTier >= 25) {
      return Result.failure(IllegalStateException("Wonder Weapon already at max Tier 25 Multiversal"))
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
    if (cur.playerLevel < 1000 || cur.playerPrestige >= 30) {
      return Result.failure(IllegalStateException("Must reach Level 1000 to Prestige (Max 30)"))
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
    if (wpnLvl < 60 || wpnPrestige >= 30) {
      return Result.failure(IllegalStateException("Must reach Weapon Level 60 to Prestige (Max 30)"))
    }
    val nextMap = cur.weaponXpMap + (weaponId to 0L)
    val nextPrestigeMap = cur.weaponPrestigeMap + (weaponId to wpnPrestige + 1)
    val rewardBlueEssence = 1000L
    save(cur.copy(weaponXpMap = nextMap, weaponPrestigeMap = nextPrestigeMap))
    return Result.success(rewardBlueEssence)
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
    val nextCycle = if (tier >= 6 + cycle * 6 && cur.battlePassPrestige < 30) {
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
    val nextPrestige = if (cur.claimedChallenges.size >= 15 && cur.challengePrestige < 30) cur.challengePrestige + 1 else cur.challengePrestige

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

  fun achievementStates(state: OperatorProgressionState = current): List<AchievementState> {
    val maxWeaponLvl = ProgressionCatalog.weapons.maxOf { state.weaponLevel(it.id).toLong() }
    return ProgressionCatalog.achievements.map { spec ->
      val progress = when (spec.id) {
        "ach_first_drop" -> state.totalMatchesPlayed
        "ach_slayer_100", "ach_slayer_500" -> state.totalElimsLifetime
        "ach_boss_5", "ach_boss_20" -> state.totalBossesLifetime
        "ach_lvl_25", "ach_lvl_100", "ach_lvl_500" -> state.playerLevel.toLong()
        "ach_wpn_30", "ach_wpn_60" -> maxWeaponLvl
        "ach_over8_lobby", "ach_war_32" -> state.largestLobbyPlayed.toLong()
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
        "${ProgressionCatalog.weaponById(wpnId).name} reached W.Lv.${next.weaponLevel(wpnId)}"
      }
      "store_bp_skip_1500" -> {
        next = next.copy(battlePassXp = cur.battlePassXp + 1800L)
        "Battle Pass advanced to ${next.battlePassXp} BP XP"
      }
      "store_weapon_tier_up" -> {
        val wpnId = cur.weaponCustom.equippedWeaponId
        val curTier = cur.weaponTier(wpnId)
        if (curTier >= 25) return Result.failure(IllegalStateException("Equipped weapon is already Tier 25 Multiversal"))
        val newTier = curTier + 1
        next = next.copy(weaponTierMap = cur.weaponTierMap + (wpnId to newTier))
        "Elevated weapon to Tier $newTier (${ProgressionCatalog.tierMeta(newTier).label})"
      }
      "store_wonder_tier_up" -> {
        val nextWonderTiers = WonderWeaponKind.entries.associate {
          it.id to (cur.wonderTier(it.id) + 1).coerceAtMost(25)
        }
        next = next.copy(wonderTierMap = nextWonderTiers)
        "All 6 Wonder Weapons upgraded +1 Tier!"
      }
      "store_gear_eq_pack" -> {
        val nextGear = cur.gearMap.mapValues { (_, g) ->
          val nextLvl = (g.level + 5).coerceAtMost(50)
          val minXp = ProgressionCatalog.totalXpForGearLevel(nextLvl)
          val nextTier = if (g.level >= 50) (g.tier + 1).coerceAtMost(25) else g.tier
          g.copy(level = nextLvl, xp = maxOf(g.xp, minXp), tier = nextTier)
        }
        val nextEq = cur.equipmentMap.mapValues { (_, e) ->
          val nextLvl = (e.level + 5).coerceAtMost(60)
          val minXp = ProgressionCatalog.totalXpForEquipmentLevel(nextLvl)
          val nextTier = if (e.level >= 60) (e.tier + 1).coerceAtMost(25) else e.tier
          e.copy(level = nextLvl, xp = maxOf(e.xp, minXp), tier = nextTier)
        }
        next = next.copy(gearMap = nextGear, equipmentMap = nextEq)
        "All Gear & Equipment boosted +5 Levels!"
      }
      "store_all_weapons_xp" -> {
        val boosted = ProgressionCatalog.weapons.associate { w ->
          w.id to ((cur.weaponXpMap[w.id] ?: 0L) + 2000L)
        }
        next = next.copy(weaponXpMap = cur.weaponXpMap + boosted)
        "+2,000 Weapon XP granted to all 5 primary weapons!"
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
        level = maxOf(eq.level, derivedLvl).coerceAtMost(60),
      )
    }
    val nextGearMap = cur.gearMap.mapValues { (_, g) ->
      val nextXp = g.xp + gearXpGained
      val derivedLvl = ProgressionCatalog.gearLevelForXp(nextXp)
      g.copy(
        xp = nextXp,
        level = maxOf(g.level, derivedLvl).coerceAtMost(50),
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
      level = (curOutfit.level + 1).coerceAtMost(1000),
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
    val nextNgPlusCompletions = if (runCompletedForNgPlus) cur.ngPlusCompletions + 1 else cur.ngPlusCompletions
    val nextUnlockedNgPlusTier = if (runCompletedForNgPlus) {
      maxOf(
        cur.unlockedNgPlusTier,
        (cur.activeNgPlusTier + 1).coerceAtMost(NewGamePlusVersion.MAX_NG_PLUS_TIER),
      )
    } else {
      cur.unlockedNgPlusTier
    }

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
      ngPlusCompletions = nextNgPlusCompletions,
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
    prefs.edit {
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
        })
      }
      put("activeNgPlusTier", next.activeNgPlusTier)
      put("unlockedNgPlusTier", next.unlockedNgPlusTier)
      put("ngPlusCompletions", next.ngPlusCompletions)
      put("worldCollectionDifficultyMap", JSONObject().apply {
        next.worldCollectionDifficultyMap.forEach { (k, v) -> put(k, v) }
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
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, 25) }
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
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, 25) }
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
            val lvl = gObj.optInt("level", 1).coerceIn(1, 50)
            val xp = gObj.optLong("xp", ProgressionCatalog.totalXpForGearLevel(lvl))
            map[k] = GearState(
              slot = slot,
              level = maxOf(lvl, ProgressionCatalog.gearLevelForXp(xp)),
              prestige = gObj.optInt("prestige", 0),
              xp = xp,
              tier = gObj.optInt("tier", 1).coerceIn(1, 25),
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
            val lvl = eObj.optInt("level", 1).coerceIn(1, 60)
            val xp = eObj.optLong("xp", ProgressionCatalog.totalXpForEquipmentLevel(lvl))
            map[k] = EquipmentState(
              slotId = k,
              equippedItemId = eObj.optString("equippedItemId", if (k == "lethal") "lethal_frag" else "tactical_stim"),
              level = maxOf(lvl, ProgressionCatalog.equipmentLevelForXp(xp)),
              prestige = eObj.optInt("prestige", 0),
              xp = xp,
              variantId = eObj.optString("variantId", "standard"),
              tier = eObj.optInt("tier", 1).coerceIn(1, 25),
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
          if (sObj != null && map.containsKey(k)) {
            val existing = map[k]!!
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
          targetPlayers = lobbyObj.optInt("targetPlayers", 12).coerceIn(1, 32),
          autoFillBots = lobbyObj.optBoolean("autoFillBots", true),
          botDifficulty = diff,
          humanPlayersOnline = lobbyObj.optInt("humanPlayersOnline", 1).coerceIn(1, 32),
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
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 0).coerceIn(0, 25) }
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
        obj.keys().forEach { k -> map[k] = obj.optInt(k, 1).coerceIn(1, 20) }
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
              level = oObj.optInt("level", 1).coerceIn(1, 1000),
              tier = oObj.optInt("tier", 1).coerceIn(1, 25),
              skillRank = oObj.optInt("skillRank", 1).coerceIn(1, 20),
              gearRank = oObj.optInt("gearRank", 1).coerceIn(1, 20),
              equipmentRank = oObj.optInt("equipmentRank", 1).coerceIn(1, 20),
              starLevel = oObj.optInt("starLevel", 1).coerceIn(1, StarRankMeta.MAX_STARS),
            )
          }
        }
      }
      map
    }

    val areaStates = safeParse(fallback.areaStates) {
      val map = MultiverseArsenalCatalog.customAreas.associate {
        it.id to CustomAreaState(it.id)
      }.toMutableMap()
      root.optJSONObject("areaStates")?.let { obj ->
        obj.keys().forEach { k ->
          val aObj = obj.optJSONObject(k)
          if (aObj != null) {
            map[k] = CustomAreaState(
              areaId = k,
              ageYears = aObj.optInt("ageYears", 12).coerceIn(0, 99),
              rebirthCycles = aObj.optInt("rebirthCycles", 0).coerceAtLeast(0),
              powerGridTier = aObj.optInt("powerGridTier", 1).coerceIn(1, 25),
              defenseTurretTier = aObj.optInt("defenseTurretTier", 1).coerceIn(1, 25),
              trapDensityTier = aObj.optInt("trapDensityTier", 1).coerceIn(1, 25),
              lootRichnessTier = aObj.optInt("lootRichnessTier", 1).coerceIn(1, 25),
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
          level = vacObj.optInt("level", 1).coerceIn(1, 50),
          tier = vacObj.optInt("tier", 1).coerceIn(1, 25),
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
          level = clanObj.optInt("level", 4).coerceIn(1, 25),
          tier = clanObj.optInt("tier", 4).coerceIn(1, 25),
          starLevel = clanObj.optInt("starLevel", 3).coerceIn(1, 10),
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
      highestPackTierReached = root.optInt("highestPackTierReached", fallback.highestPackTierReached).coerceIn(1, 25),
      largestLobbyPlayed = root.optInt("largestLobbyPlayed", fallback.largestLobbyPlayed).coerceIn(1, 32),
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
        a == null -> b!!
        b == null -> a
        else -> a.copy(
          level = maxOf(a.level, b.level),
          prestige = maxOf(a.prestige, b.prestige),
          xp = maxOf(a.xp, b.xp),
          tier = maxOf(a.tier, b.tier),
          starLevel = maxOf(a.starLevel, b.starLevel),
        )
      }
    }

    val mergedEquip = (base.equipmentMap.keys + incoming.equipmentMap.keys).associateWith { k ->
      val a = base.equipmentMap[k]
      val b = incoming.equipmentMap[k]
      when {
        a == null -> b!!
        b == null -> a
        else -> a.copy(
          level = maxOf(a.level, b.level),
          prestige = maxOf(a.prestige, b.prestige),
          xp = maxOf(a.xp, b.xp),
          tier = maxOf(a.tier, b.tier),
          starLevel = maxOf(a.starLevel, b.starLevel),
        )
      }
    }

    val mergedSkills = (base.skillNodes.keys + incoming.skillNodes.keys).associateWith { k ->
      val a = base.skillNodes[k]
      val b = incoming.skillNodes[k]
      when {
        a == null -> b!!
        b == null -> a
        else -> a.copy(
          unlocked = a.unlocked || b.unlocked,
          tier = maxOf(a.tier, b.tier),
          starLevel = maxOf(a.starLevel, b.starLevel),
        )
      }
    }

    val mergedOutfits = (base.outfitStates.keys + incoming.outfitStates.keys).associateWith { k ->
      val a = base.outfitStates[k]
      val b = incoming.outfitStates[k]
      when {
        a == null -> b!!
        b == null -> a
        else -> a.copy(
          level = maxOf(a.level, b.level),
          tier = maxOf(a.tier, b.tier),
          skillRank = maxOf(a.skillRank, b.skillRank),
          gearRank = maxOf(a.gearRank, b.gearRank),
          equipmentRank = maxOf(a.equipmentRank, b.equipmentRank),
          starLevel = maxOf(a.starLevel, b.starLevel),
        )
      }
    }

    val mergedAreas = (base.areaStates.keys + incoming.areaStates.keys).associateWith { k ->
      val a = base.areaStates[k]
      val b = incoming.areaStates[k]
      when {
        a == null -> b!!
        b == null -> a
        else -> a.copy(
          ageYears = maxOf(a.ageYears, b.ageYears),
          rebirthCycles = maxOf(a.rebirthCycles, b.rebirthCycles),
          powerGridTier = maxOf(a.powerGridTier, b.powerGridTier),
          defenseTurretTier = maxOf(a.defenseTurretTier, b.defenseTurretTier),
          trapDensityTier = maxOf(a.trapDensityTier, b.trapDensityTier),
          lootRichnessTier = maxOf(a.lootRichnessTier, b.lootRichnessTier),
          starLevel = maxOf(a.starLevel, b.starLevel),
        )
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
          prefs.edit {
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
    return runCatching {
      parseProgressionJson(JSONObject(raw), OperatorProgressionState())
    }.getOrDefault(OperatorProgressionState())
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

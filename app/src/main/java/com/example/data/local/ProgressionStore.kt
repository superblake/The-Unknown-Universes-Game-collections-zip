package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.data.model.AchievementState
import com.example.data.model.BattlePassVersion
import com.example.data.model.BotDifficulty
import com.example.data.model.ChallengeState
import com.example.data.model.CharacterCustomization
import com.example.data.model.EquipmentState
import com.example.data.model.GearSlot
import com.example.data.model.GearState
import com.example.data.model.LevelCustomization
import com.example.data.model.LobbyConfig
import com.example.data.model.LobbyPartyMode
import com.example.data.model.MatchProgressionReport
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.WeaponCustomization
import com.example.data.model.WonderWeaponKind
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

  private val _state = MutableStateFlow(load())
  val state: StateFlow<OperatorProgressionState> = _state.asStateFlow()

  val current: OperatorProgressionState
    get() = _state.value

  fun updateWeaponCustomization(transform: (WeaponCustomization) -> WeaponCustomization) {
    val next = current.copy(
      weaponCustom = transform(current.weaponCustom),
      challengeProgress = incrementChallenge(current.challengeProgress, "ch_armory_custom", 1L),
    )
    save(next)
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
    if (gear.tier >= 10) {
      return Result.failure(IllegalStateException("${slot.displayName} already at max Tier 10 Omega"))
    }
    val cost = gear.tier * 400L
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
    if (eq.tier >= 10) {
      return Result.failure(IllegalStateException("Equipment already at max Tier 10 Omega"))
    }
    val cost = eq.tier * 350L
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
    save(cur.copy(equipmentMap = cur.equipmentMap + (slotId to nextEq)))
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

  fun upgradeWeaponTier(weaponId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val curTier = cur.weaponTier(weaponId)
    if (curTier >= 10) {
      return Result.failure(IllegalStateException("Weapon already at max Tier 10 Omega"))
    }
    val cost = curTier * 450L
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

  fun upgradeWonderTier(wonderId: String, currentBlueEssence: Long): Result<Long> {
    val cur = current
    val curTier = cur.wonderTier(wonderId)
    if (curTier >= 10) {
      return Result.failure(IllegalStateException("Wonder Weapon already at max Tier 10 Omega"))
    }
    val cost = curTier * 500L
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
        "ach_pack_5", "ach_pack_10" -> state.highestPackTierReached.toLong()
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
        if (curTier >= 10) return Result.failure(IllegalStateException("Equipped weapon is already Tier 10 Omega"))
        val newTier = curTier + 1
        next = next.copy(weaponTierMap = cur.weaponTierMap + (wpnId to newTier))
        "Elevated weapon to Tier $newTier (${ProgressionCatalog.tierMeta(newTier).label})"
      }
      "store_wonder_tier_up" -> {
        val nextWonderTiers = WonderWeaponKind.entries.associate {
          it.id to (cur.wonderTier(it.id) + 1).coerceAtMost(10)
        }
        next = next.copy(wonderTierMap = nextWonderTiers)
        "All 6 Wonder Weapons upgraded +1 Tier!"
      }
      "store_gear_eq_pack" -> {
        val nextGear = cur.gearMap.mapValues { (_, g) ->
          val nextLvl = (g.level + 5).coerceAtMost(50)
          val minXp = ProgressionCatalog.totalXpForGearLevel(nextLvl)
          val nextTier = if (g.level >= 50) (g.tier + 1).coerceAtMost(10) else g.tier
          g.copy(level = nextLvl, xp = maxOf(g.xp, minXp), tier = nextTier)
        }
        val nextEq = cur.equipmentMap.mapValues { (_, e) ->
          val nextLvl = (e.level + 5).coerceAtMost(60)
          val minXp = ProgressionCatalog.totalXpForEquipmentLevel(nextLvl)
          val nextTier = if (e.level >= 60) (e.tier + 1).coerceAtMost(10) else e.tier
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
    val lobbyBonusMult = 1.0f + ((report.lobbySizePlayed.coerceAtLeast(1) - 1) * 0.025f).coerceAtMost(0.75f)
    val baseXp = (report.eliminations * 26L + report.bossEliminations * 140L + report.waveReached * 65L + if (report.extracted) 240L else 75L)
      .coerceAtLeast(60L)
    val xpGained = (baseXp * boosterMult * lobbyBonusMult).toLong()

    val baseWpnXp = (report.eliminations * 22L + report.bossEliminations * 95L + report.waveReached * 40L + report.wonderPickupsCollected * 35L)
      .coerceAtLeast(50L)
    val wpnXpGained = (baseWpnXp * boosterMult).toLong()

    val equipXpGained = ((report.eliminations * 14L + report.bossEliminations * 60L + report.waveReached * 30L) * boosterMult)
      .toLong().coerceAtLeast(45L)
    val gearXpGained = ((report.eliminations * 12L + report.bossEliminations * 55L + report.waveReached * 28L) * boosterMult)
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

    val nextState = cur.copy(
      playerXp = cur.playerXp + xpGained,
      weaponXpMap = nextWpnMap,
      equipmentMap = nextEquipmentMap,
      gearMap = nextGearMap,
      battlePassXp = cur.battlePassXp + bpXpGained,
      challengeProgress = nextChallenges,
      wonderWeaponsPickedTotal = cur.wonderWeaponsPickedTotal + report.wonderPickupsCollected,
      totalElimsLifetime = cur.totalElimsLifetime + report.eliminations,
      totalBossesLifetime = cur.totalBossesLifetime + report.bossEliminations,
      totalMatchesPlayed = cur.totalMatchesPlayed + 1L,
      highestPackTierReached = maxOf(cur.highestPackTierReached, report.highestPackTierReached),
      largestLobbyPlayed = maxOf(cur.largestLobbyPlayed, report.lobbySizePlayed),
      xpBoosterMatchesRemaining = (cur.xpBoosterMatchesRemaining - 1).coerceAtLeast(0),
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
    val root = JSONObject().apply {
      put("playerXp", next.playerXp)
      put("playerPrestige", next.playerPrestige)
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
      put("unlockedPasses", JSONArray(next.unlockedPassVersions.map { it.id }))
      put("claimedTiers", JSONArray(next.claimedPassTiers.toList()))
      put("claimedChallenges", JSONArray(next.claimedChallenges.toList()))
      put("claimedAchievements", JSONArray(next.claimedAchievements.toList()))
      put("purchasedStoreItems", JSONArray(next.purchasedStoreItems.toList()))

      put("weaponXpMap", JSONObject().apply {
        next.weaponXpMap.forEach { (k, v) -> put(k, v) }
      })
      put("weaponPrestigeMap", JSONObject().apply {
        next.weaponPrestigeMap.forEach { (k, v) -> put(k, v) }
      })
      put("weaponTierMap", JSONObject().apply {
        next.weaponTierMap.forEach { (k, v) -> put(k, v) }
      })
      put("wonderTierMap", JSONObject().apply {
        next.wonderTierMap.forEach { (k, v) -> put(k, v) }
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
          })
        }
      })
      put("skillNodes", JSONObject().apply {
        next.skillNodes.forEach { (k, node) ->
          put(k, JSONObject().apply {
            put("unlocked", node.unlocked)
            put("tier", node.tier)
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
        put("wonderDropBoost", next.levelCustom.wonderDropBoost)
        put("critSurgeMutator", next.levelCustom.critSurgeMutator)
        put("instagibMutator", next.levelCustom.instagibMutator)
        put("lowGravityDash", next.levelCustom.lowGravityDash)
        put("bossRushMutator", next.levelCustom.bossRushMutator)
        put("floorAccentHex", next.levelCustom.floorAccentHex)
      })
    }
    val jsonString = root.toString()
    prefs.edit {
      putString(KEY_STATE_JSON, jsonString)
      putString(KEY_STATE_JSON_LEGACY, jsonString)
    }
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
    if (raw.isNullOrBlank()) return OperatorProgressionState()
    return runCatching {
      val root = JSONObject(raw)
      val unlockedArr = root.optJSONArray("unlockedPasses")
      val unlockedPasses = mutableSetOf(BattlePassVersion.STANDARD_SECTOR_OPS)
      if (unlockedArr != null) {
        for (i in 0 until unlockedArr.length()) {
          val id = unlockedArr.optString(i)
          BattlePassVersion.entries.firstOrNull { it.id == id }?.let { unlockedPasses.add(it) }
        }
      }
      val activePassId = root.optString("activePassVersion", BattlePassVersion.STANDARD_SECTOR_OPS.id)
      val activePass = BattlePassVersion.entries.firstOrNull { it.id == activePassId } ?: BattlePassVersion.STANDARD_SECTOR_OPS

      val claimedTiers = mutableSetOf<String>()
      root.optJSONArray("claimedTiers")?.let { arr ->
        for (i in 0 until arr.length()) claimedTiers.add(arr.optString(i))
      }
      val claimedChallenges = mutableSetOf<String>()
      root.optJSONArray("claimedChallenges")?.let { arr ->
        for (i in 0 until arr.length()) claimedChallenges.add(arr.optString(i))
      }
      val claimedAchievements = mutableSetOf<String>()
      root.optJSONArray("claimedAchievements")?.let { arr ->
        for (i in 0 until arr.length()) claimedAchievements.add(arr.optString(i))
      }
      val purchasedStoreItems = mutableSetOf<String>()
      root.optJSONArray("purchasedStoreItems")?.let { arr ->
        for (i in 0 until arr.length()) purchasedStoreItems.add(arr.optString(i))
      }

      val wpnXp = mutableMapOf<String, Long>()
      root.optJSONObject("weaponXpMap")?.let { obj ->
        obj.keys().forEach { k -> wpnXp[k] = obj.optLong(k, 0L) }
      }
      val wpnPrestige = mutableMapOf<String, Int>()
      root.optJSONObject("weaponPrestigeMap")?.let { obj ->
        obj.keys().forEach { k -> wpnPrestige[k] = obj.optInt(k, 0) }
      }
      val wpnTierMap = ProgressionCatalog.weapons.associate { it.id to 1 }.toMutableMap()
      root.optJSONObject("weaponTierMap")?.let { obj ->
        obj.keys().forEach { k -> wpnTierMap[k] = obj.optInt(k, 1).coerceIn(1, 10) }
      }
      val wonderTierMap = WonderWeaponKind.entries.associate { it.id to 1 }.toMutableMap()
      root.optJSONObject("wonderTierMap")?.let { obj ->
        obj.keys().forEach { k -> wonderTierMap[k] = obj.optInt(k, 1).coerceIn(1, 10) }
      }
      val chProg = mutableMapOf<String, Long>()
      root.optJSONObject("challengeProgress")?.let { obj ->
        obj.keys().forEach { k -> chProg[k] = obj.optLong(k, 0L) }
      }

      val gearMap = GearSlot.entries.associate { it.id to GearState(it) }.toMutableMap()
      root.optJSONObject("gearMap")?.let { obj ->
        obj.keys().forEach { k ->
          val gObj = obj.optJSONObject(k)
          if (gObj != null) {
            val slot = GearSlot.entries.firstOrNull { it.id == k } ?: GearSlot.HELMET
            val lvl = gObj.optInt("level", 1).coerceIn(1, 50)
            val xp = gObj.optLong("xp", ProgressionCatalog.totalXpForGearLevel(lvl))
            gearMap[k] = GearState(
              slot = slot,
              level = maxOf(lvl, ProgressionCatalog.gearLevelForXp(xp)),
              prestige = gObj.optInt("prestige", 0),
              xp = xp,
              tier = gObj.optInt("tier", 1).coerceIn(1, 10),
              colorHex = gObj.optLong("colorHex", 0xFF00E5FF),
            )
          }
        }
      }

      val equipmentMap = mapOf(
        "lethal" to EquipmentState("lethal", "lethal_frag", 1, 0, 0L, "standard", 1),
        "tactical" to EquipmentState("tactical", "tactical_stim", 1, 0, 0L, "standard", 1),
      ).toMutableMap()
      root.optJSONObject("equipmentMap")?.let { obj ->
        obj.keys().forEach { k ->
          val eObj = obj.optJSONObject(k)
          if (eObj != null) {
            val lvl = eObj.optInt("level", 1).coerceIn(1, 60)
            val xp = eObj.optLong("xp", ProgressionCatalog.totalXpForEquipmentLevel(lvl))
            equipmentMap[k] = EquipmentState(
              slotId = k,
              equippedItemId = eObj.optString("equippedItemId", if (k == "lethal") "lethal_frag" else "tactical_stim"),
              level = maxOf(lvl, ProgressionCatalog.equipmentLevelForXp(xp)),
              prestige = eObj.optInt("prestige", 0),
              xp = xp,
              variantId = eObj.optString("variantId", "standard"),
              tier = eObj.optInt("tier", 1).coerceIn(1, 10),
            )
          }
        }
      }

      val skillNodes = ProgressionCatalog.defaultSkillNodes().toMutableMap()
      root.optJSONObject("skillNodes")?.let { obj ->
        obj.keys().forEach { k ->
          val sObj = obj.optJSONObject(k)
          if (sObj != null && skillNodes.containsKey(k)) {
            val existing = skillNodes[k]!!
            skillNodes[k] = existing.copy(
              unlocked = sObj.optBoolean("unlocked", false),
              tier = sObj.optInt("tier", 1).coerceIn(1, existing.maxTier),
            )
          }
        }
      }

      val lobbyObj = root.optJSONObject("lobbyConfig")
      val lobbyCfg = if (lobbyObj != null) {
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
      } else LobbyConfig()

      val wcObj = root.optJSONObject("weaponCustom")
      val wc = if (wcObj != null) {
        WeaponCustomization(
          equippedWeaponId = wcObj.optString("equippedWeaponId", "wpn_pulse_carbine"),
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
      } else WeaponCustomization()

      val ccObj = root.optJSONObject("characterCustom")
      val cc = if (ccObj != null) {
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
      } else CharacterCustomization()

      val lcObj = root.optJSONObject("levelCustom")
      val lc = if (lcObj != null) {
        LevelCustomization(
          skyThemeId = lcObj.optString("skyThemeId", "default_sector"),
          gridStyleId = lcObj.optString("gridStyleId", "tactical_square"),
          enemyDensityMultiplier = lcObj.optDouble("enemyDensityMultiplier", 1.0).toFloat().coerceIn(0.75f, 1.5f),
          wonderDropBoost = lcObj.optBoolean("wonderDropBoost", false),
          critSurgeMutator = lcObj.optBoolean("critSurgeMutator", false),
          instagibMutator = lcObj.optBoolean("instagibMutator", false),
          lowGravityDash = lcObj.optBoolean("lowGravityDash", false),
          bossRushMutator = lcObj.optBoolean("bossRushMutator", false),
          floorAccentHex = lcObj.optLong("floorAccentHex", 0xFF00E5FF),
        )
      } else LevelCustomization()

      OperatorProgressionState(
        playerXp = root.optLong("playerXp", 0L),
        playerPrestige = root.optInt("playerPrestige", 0),
        weaponXpMap = wpnXp.ifEmpty { mapOf("wpn_pulse_carbine" to 0L) },
        weaponPrestigeMap = wpnPrestige,
        weaponTierMap = wpnTierMap,
        wonderTierMap = wonderTierMap,
        gearMap = gearMap,
        gearPrestige = root.optInt("gearPrestige", 0),
        equipmentMap = equipmentMap,
        weaponCustom = wc,
        characterCustom = cc,
        levelCustom = lc,
        lobbyConfig = lobbyCfg,
        activePassVersion = activePass,
        unlockedPassVersions = unlockedPasses,
        battlePassXp = root.optLong("battlePassXp", 0L),
        battlePassPrestige = root.optInt("battlePassPrestige", 0),
        bpCycleCount = root.optInt("bpCycleCount", 0),
        claimedPassTiers = claimedTiers,
        challengeProgress = chProg,
        claimedChallenges = claimedChallenges,
        challengePrestige = root.optInt("challengePrestige", 0),
        skillNodes = skillNodes,
        wonderWeaponsPickedTotal = root.optLong("wonderPicked", 0L),
        claimedAchievements = claimedAchievements,
        totalElimsLifetime = root.optLong("totalElimsLifetime", 0L),
        totalBossesLifetime = root.optLong("totalBossesLifetime", 0L),
        totalMatchesPlayed = root.optLong("totalMatchesPlayed", 0L),
        totalBlueEssenceSpent = root.optLong("totalBlueEssenceSpent", 0L),
        highestPackTierReached = root.optInt("highestPackTierReached", 1),
        largestLobbyPlayed = root.optInt("largestLobbyPlayed", 1),
        storePurchasesCount = root.optInt("storePurchasesCount", 0),
        purchasedStoreItems = purchasedStoreItems,
        xpBoosterMatchesRemaining = root.optInt("xpBoosterMatchesRemaining", 0),
        botDamageBoostPct = root.optInt("botDamageBoostPct", 0),
      )
    }.getOrDefault(OperatorProgressionState())
  }

  companion object {
    const val PREFS_PREFIX = "unknown_os_progression"
    private const val KEY_STATE_JSON = "progression_json_v2"
    private const val KEY_STATE_JSON_LEGACY = "progression_json"
  }
}

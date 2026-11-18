package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.data.model.BattlePassVersion
import com.example.data.model.ChallengeState
import com.example.data.model.CharacterCustomization
import com.example.data.model.GearSlot
import com.example.data.model.GearState
import com.example.data.model.LevelCustomization
import com.example.data.model.MatchProgressionReport
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.SkillNode
import com.example.data.model.WeaponCustomization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

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

  fun upgradeGear(slot: GearSlot) {
    val cur = current
    val gear = cur.gearMap[slot.id] ?: GearState(slot)
    if (gear.level >= 50) return
    val nextGear = gear.copy(level = gear.level + 1)
    val nextMap = cur.gearMap + (slot.id to nextGear)
    save(cur.copy(gearMap = nextMap))
  }

  fun prestigeGear() {
    val cur = current
    if (cur.gearPrestige >= 30) return
    val resetGear = cur.gearMap.mapValues { (_, g) -> g.copy(level = 1, prestige = g.prestige + 1) }
    save(cur.copy(gearMap = resetGear, gearPrestige = cur.gearPrestige + 1))
  }

  fun upgradeEquipment(slotId: String) {
    val cur = current
    val eq = cur.equipmentMap[slotId] ?: com.example.data.model.EquipmentState(slotId, if (slotId == "lethal") "lethal_frag" else "tactical_stim", 1, 0, 0L, "standard")
    if (eq.level >= 60) return
    val nextEq = eq.copy(level = eq.level + 1)
    save(cur.copy(equipmentMap = cur.equipmentMap + (slotId to nextEq)))
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
    val eq = cur.equipmentMap[slotId] ?: com.example.data.model.EquipmentState(slotId, itemId, 1, 0, 0L, "standard")
    val nextEq = eq.copy(equippedItemId = itemId)
    save(cur.copy(equipmentMap = cur.equipmentMap + (slotId to nextEq)))
  }

  fun unlockSkillNode(nodeId: String, currentEssence: Long): Result<Long> {
    val cur = current
    val node = cur.skillNodes[nodeId] ?: return Result.failure(IllegalStateException("Skill node not found"))
    if (node.unlocked) return Result.failure(IllegalStateException("Already unlocked"))
    if (currentEssence < node.costEssence) {
      return Result.failure(IllegalStateException("Need ${node.costEssence} Essence"))
    }
    val nextNode = node.copy(unlocked = true)
    val nextNodes = cur.skillNodes + (nodeId to nextNode)
    save(cur.copy(skillNodes = nextNodes))
    return Result.success(node.costEssence)
  }

  fun prestigePlayer(): Result<Long> {
    val cur = current
    if (cur.playerLevel < 1000 || cur.playerPrestige >= 30) {
      return Result.failure(IllegalStateException("Must reach Level 1000 to Prestige (Max 30)"))
    }
    val rewardEssence = (cur.playerPrestige + 1) * 2500L
    val next = cur.copy(
      playerXp = 0L,
      playerPrestige = cur.playerPrestige + 1,
    )
    save(next)
    return Result.success(rewardEssence)
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
    val rewardEssence = 1000L
    save(cur.copy(weaponXpMap = nextMap, weaponPrestigeMap = nextPrestigeMap))
    return Result.success(rewardEssence)
  }

  fun activateOrUnlockPassVersion(version: BattlePassVersion, currentEssence: Long): Result<Long> {
    val cur = current
    if (cur.unlockedPassVersions.contains(version)) {
      save(cur.copy(activePassVersion = version))
      return Result.success(0L)
    }
    if (currentEssence < version.unlockCostEssence) {
      return Result.failure(IllegalStateException("Need ${version.unlockCostEssence} Essence to unlock ${version.title}"))
    }
    val next = cur.copy(
      activePassVersion = version,
      unlockedPassVersions = cur.unlockedPassVersions + version,
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

  fun recordMatchProgression(report: MatchProgressionReport): Triple<Long, Long, Long> {
    val cur = current
    val prevPlayerLvl = cur.playerLevel
    val prevWpnLvl = cur.weaponLevel(report.weaponIdUsed)

    val xpGained = (report.eliminations * 22L + report.bossEliminations * 120L + report.waveReached * 50L + if (report.extracted) 200L else 50L)
      .coerceAtLeast(40L)
    val wpnXpGained = (report.eliminations * 18L + report.bossEliminations * 80L + report.waveReached * 30L + report.wonderPickupsCollected * 30L)
      .coerceAtLeast(35L)
    val bpMult = if (cur.activePassVersion == BattlePassVersion.BLACKCELL_OVERDRIVE) 1.30f else 1.0f
    val bpXpGained = ((xpGained * 0.9f) * bpMult).toLong().coerceAtLeast(45L)

    var nextChallenges = cur.challengeProgress
    nextChallenges = incrementChallenge(nextChallenges, "ch_hostiles_40", report.eliminations)
    nextChallenges = incrementChallenge(nextChallenges, "ch_bosses_2", report.bossEliminations)
    nextChallenges = incrementChallenge(nextChallenges, "ch_wonder_2", report.wonderPickupsCollected.toLong())
    if (report.waveReached >= 4L || report.extracted) {
      nextChallenges = incrementChallenge(nextChallenges, "ch_wave_4", 1L)
    }

    val nextWpnMap = cur.weaponXpMap + (report.weaponIdUsed to (cur.weaponXp(report.weaponIdUsed) + wpnXpGained))
    val nextState = cur.copy(
      playerXp = cur.playerXp + xpGained,
      weaponXpMap = nextWpnMap,
      battlePassXp = cur.battlePassXp + bpXpGained,
      challengeProgress = nextChallenges,
      wonderWeaponsPickedTotal = cur.wonderWeaponsPickedTotal + report.wonderPickupsCollected,
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
      put("unlockedPasses", JSONArray(next.unlockedPassVersions.map { it.id }))
      put("claimedTiers", JSONArray(next.claimedPassTiers.toList()))
      put("claimedChallenges", JSONArray(next.claimedChallenges.toList()))

      put("weaponXpMap", JSONObject().apply {
        next.weaponXpMap.forEach { (k, v) -> put(k, v) }
      })
      put("weaponPrestigeMap", JSONObject().apply {
        next.weaponPrestigeMap.forEach { (k, v) -> put(k, v) }
      })
      put("challengeProgress", JSONObject().apply {
        next.challengeProgress.forEach { (k, v) -> put(k, v) }
      })
      put("gearMap", JSONObject().apply {
        next.gearMap.forEach { (k, g) ->
          put(k, JSONObject().apply {
            put("level", g.level)
            put("prestige", g.prestige)
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

      val wpnXp = mutableMapOf<String, Long>()
      root.optJSONObject("weaponXpMap")?.let { obj ->
        obj.keys().forEach { k -> wpnXp[k] = obj.optLong(k, 0L) }
      }
      val wpnPrestige = mutableMapOf<String, Int>()
      root.optJSONObject("weaponPrestigeMap")?.let { obj ->
        obj.keys().forEach { k -> wpnPrestige[k] = obj.optInt(k, 0) }
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
            gearMap[k] = GearState(
              slot = slot,
              level = gObj.optInt("level", 1),
              prestige = gObj.optInt("prestige", 0),
              colorHex = gObj.optLong("colorHex", 0xFF00E5FF),
            )
          }
        }
      }

      val equipmentMap = mapOf(
        "lethal" to com.example.data.model.EquipmentState("lethal", "lethal_frag", 1, 0, 0L, "standard"),
        "tactical" to com.example.data.model.EquipmentState("tactical", "tactical_stim", 1, 0, 0L, "standard"),
      ).toMutableMap()
      root.optJSONObject("equipmentMap")?.let { obj ->
        obj.keys().forEach { k ->
          val eObj = obj.optJSONObject(k)
          if (eObj != null) {
            equipmentMap[k] = com.example.data.model.EquipmentState(
              slotId = k,
              equippedItemId = eObj.optString("equippedItemId", if (k == "lethal") "lethal_frag" else "tactical_stim"),
              level = eObj.optInt("level", 1),
              prestige = eObj.optInt("prestige", 0),
              xp = eObj.optLong("xp", 0L),
              variantId = eObj.optString("variantId", "standard"),
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
              tier = sObj.optInt("tier", 1),
            )
          }
        }
      }

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
          animationSpeedMultiplier = ccObj.optDouble("animationSpeedMultiplier", 1.0).toFloat().coerceIn(0.5f, 2.0f),
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
        gearMap = gearMap,
        gearPrestige = root.optInt("gearPrestige", 0),
        equipmentMap = equipmentMap,
        weaponCustom = wc,
        characterCustom = cc,
        levelCustom = lc,
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
      )
    }.getOrDefault(OperatorProgressionState())
  }

  companion object {
    const val PREFS_PREFIX = "unknown_os_progression"
    private const val KEY_STATE_JSON = "progression_json_v2"
    private const val KEY_STATE_JSON_LEGACY = "progression_json"
  }
}

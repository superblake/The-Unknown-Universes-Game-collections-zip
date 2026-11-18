package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.data.model.BattlePassVersion
import com.example.data.model.ChallengeState
import com.example.data.model.CharacterCustomization
import com.example.data.model.LevelCustomization
import com.example.data.model.MatchProgressionReport
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
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
  private val safeUserId = userId.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "local_operator" }
  private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
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
    val rewardSpec = ProgressionCatalog.tiersForVersion(version).firstOrNull { it.tier == tier }
      ?: return Result.failure(IllegalStateException("Tier not found"))
    if (cur.battlePassXp < rewardSpec.bpXpRequired) {
      return Result.failure(IllegalStateException("Need ${rewardSpec.bpXpRequired} Battle Pass XP"))
    }
    val key = "${version.id}_$tier"
    if (cur.claimedPassTiers.contains(key)) {
      return Result.failure(IllegalStateException("Tier already claimed"))
    }
    val bonusWpnXp = if (rewardSpec.unlockTag == "WPN XP" || rewardSpec.unlockTag == "BLUEPRINT") 450L else 0L
    val equippedWpn = cur.weaponCustom.equippedWeaponId
    val nextWpnMap = if (bonusWpnXp > 0L) {
      cur.weaponXpMap + (equippedWpn to ((cur.weaponXpMap[equippedWpn] ?: 0L) + bonusWpnXp))
    } else cur.weaponXpMap

    save(
      cur.copy(
        claimedPassTiers = cur.claimedPassTiers + key,
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
    if (cur.claimedChallenges.contains(challengeId)) {
      return Result.failure(IllegalStateException("Reward already claimed"))
    }
    val bpMult = if (cur.activePassVersion == BattlePassVersion.BLACKCELL_OVERDRIVE) 1.25f else 1.0f
    val bpGain = (spec.rewardBpXp * bpMult).toLong()
    save(
      cur.copy(
        playerXp = cur.playerXp + spec.rewardPlayerXp,
        battlePassXp = cur.battlePassXp + bpGain,
        claimedChallenges = cur.claimedChallenges + challengeId,
      )
    )
    return Result.success(Triple(spec.rewardEssence, spec.rewardPlayerXp, bpGain))
  }

  fun resetCompletedChallengesForReplay() {
    val cur = current
    val remainingProgress = cur.challengeProgress.filterKeys { !cur.claimedChallenges.contains(it) }
    save(
      cur.copy(
        challengeProgress = remainingProgress,
        claimedChallenges = emptySet(),
      )
    )
  }

  fun recordMatchProgression(report: MatchProgressionReport): Triple<Long, Long, Long> {
    val cur = current
    val prevPlayerLvl = cur.playerLevel
    val prevWpnLvl = cur.weaponLevel(report.weaponIdUsed)

    val xpGained = (report.eliminations * 18L + report.bossEliminations * 95L + report.waveReached * 40L + if (report.extracted) 160L else 40L)
      .coerceAtLeast(30L)
    val wpnXpGained = (report.eliminations * 14L + report.bossEliminations * 65L + report.waveReached * 25L + report.wonderPickupsCollected * 20L)
      .coerceAtLeast(25L)
    val bpMult = if (cur.activePassVersion == BattlePassVersion.BLACKCELL_OVERDRIVE) 1.25f else 1.0f
    val bpXpGained = ((xpGained * 0.85f) * bpMult).toLong().coerceAtLeast(35L)

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
      ChallengeState(
        spec = spec,
        progress = (state.challengeProgress[spec.id] ?: 0L).coerceAtMost(spec.target),
        claimed = state.claimedChallenges.contains(spec.id),
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
      put("battlePassXp", next.battlePassXp)
      put("activePassVersion", next.activePassVersion.id)
      put("wonderPicked", next.wonderWeaponsPickedTotal)
      put("unlockedPasses", JSONArray(next.unlockedPassVersions.map { it.id }))
      put("claimedTiers", JSONArray(next.claimedPassTiers.toList()))
      put("claimedChallenges", JSONArray(next.claimedChallenges.toList()))

      put("weaponXpMap", JSONObject().apply {
        next.weaponXpMap.forEach { (k, v) -> put(k, v) }
      })
      put("challengeProgress", JSONObject().apply {
        next.challengeProgress.forEach { (k, v) -> put(k, v) }
      })
      put("weaponCustom", JSONObject().apply {
        put("equippedWeaponId", next.weaponCustom.equippedWeaponId)
        put("camoId", next.weaponCustom.camoId)
        put("opticId", next.weaponCustom.opticId)
        put("barrelId", next.weaponCustom.barrelId)
        put("ammoModId", next.weaponCustom.ammoModId)
        put("tracerColorHex", next.weaponCustom.tracerColorHex)
      })
      put("characterCustom", JSONObject().apply {
        put("suitThemeId", next.characterCustom.suitThemeId)
        put("visorStyleId", next.characterCustom.visorStyleId)
        put("auraTrailId", next.characterCustom.auraTrailId)
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
    prefs.edit { putString(KEY_STATE_JSON, root.toString()) }
  }

  private fun load(): OperatorProgressionState {
    val raw = prefs.getString(KEY_STATE_JSON, null) ?: return OperatorProgressionState()
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
      val chProg = mutableMapOf<String, Long>()
      root.optJSONObject("challengeProgress")?.let { obj ->
        obj.keys().forEach { k -> chProg[k] = obj.optLong(k, 0L) }
      }

      val wcObj = root.optJSONObject("weaponCustom")
      val wc = if (wcObj != null) {
        WeaponCustomization(
          equippedWeaponId = wcObj.optString("equippedWeaponId", "wpn_pulse_carbine"),
          camoId = wcObj.optString("camoId", "cyber_matte"),
          opticId = wcObj.optString("opticId", "holo_cyan"),
          barrelId = wcObj.optString("barrelId", "standard_barrel"),
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
        weaponXpMap = wpnXp.ifEmpty { mapOf("wpn_pulse_carbine" to 0L) },
        weaponCustom = wc,
        characterCustom = cc,
        levelCustom = lc,
        activePassVersion = activePass,
        unlockedPassVersions = unlockedPasses,
        battlePassXp = root.optLong("battlePassXp", 0L),
        claimedPassTiers = claimedTiers,
        challengeProgress = chProg,
        claimedChallenges = claimedChallenges,
        wonderWeaponsPickedTotal = root.optLong("wonderPicked", 0L),
      )
    }.getOrDefault(OperatorProgressionState())
  }

  companion object {
    const val PREFS_PREFIX = "unknown_os_progression"
    private const val KEY_STATE_JSON = "progression_json_v1"
  }
}

package com.example.data.local

import android.content.Context
import com.example.data.model.ArmoryItem
import com.example.data.model.LinkedAccount
import com.example.data.model.MatchRecord
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class UniverseRepository(private val dao: UniverseDao) {

  constructor(context: Context) : this(
    UniverseDatabase.getInstance(context).universeDao()
  )

  fun observeProfile(userId: String): Flow<OperatorProfile?> = dao.observeProfile(userId)

  fun observeArmoryItems(userId: String): Flow<List<ArmoryItem>> = dao.observeArmoryItems(userId)

  fun observeCustomRigs(userId: String): Flow<List<RigAsset>> = dao.observeCustomRigs(userId)

  fun observeMatchLogs(userId: String): Flow<List<MatchRecord>> = dao.observeMatchLogs(userId)

  fun observeLinkedAccounts(userId: String): Flow<List<LinkedAccount>> = dao.observeLinkedAccounts(userId)

  suspend fun ensureStarterData(userId: String, displayName: String?): Result<Unit> {
    return try {
      val existing = dao.getProfileOnce(userId)
      if (existing == null) {
        val safeCallsign =
          displayName?.trim()?.takeIf { it.length >= 2 }?.take(30) ?: "VanguardPrime"
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
      } else if (!displayName.isNullOrBlank() && displayName.trim().length >= 2 && existing.callsign != displayName.trim().take(30)) {
        dao.upsertProfile(
          existing.copy(
            callsign = displayName.trim().take(30),
            updatedAtMillis = System.currentTimeMillis(),
          )
        )
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
        activeUniverseId = activeUniverseId ?: existing.activeUniverseId,
        activeMode = activeMode ?: existing.activeMode,
        activeRigId = activeRigId ?: existing.activeRigId,
        consolePreset = consolePreset ?: existing.consolePreset,
        callsign = callsign?.let { it.trim().take(30).padEnd(2, '_') } ?: existing.callsign,
        osWallpaperIndex = osWallpaperIndex?.coerceIn(0L, 10L) ?: existing.osWallpaperIndex,
        updatedAtMillis = System.currentTimeMillis(),
      )
      dao.upsertProfile(updated)
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
      dao.upsertArmoryItem(
        ArmoryItem(
          id = itemId,
          userId = userId,
          name = name.trim().take(50).ifEmpty { "Custom Module" },
          category = category,
          universeTag = universeTag.trim().take(40).ifEmpty { "UnknownOS" },
          description = description.trim().take(240),
          tier = 1L,
          maxTier = 5L,
          cooldownSec = cooldownSec.coerceIn(0L, 120L),
          powerValue = powerValue.coerceIn(1L, 500L),
          accentColorHex = accentColorHex,
          equipped = equipped,
          isCustom = isCustom,
          createdAtMillis = now,
          updatedAtMillis = now,
        )
      )
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
      dao.upsertArmoryItem(
        item.copy(
          userId = userId,
          equipped = !item.equipped,
          updatedAtMillis = System.currentTimeMillis(),
        )
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun upgradeArmoryItem(
    userId: String,
    item: ArmoryItem,
    upgradeCost: Long,
    currentEssence: Long,
  ): Result<Unit> {
    require(item.id.isNotBlank()) { "ArmoryItem id required" }
    if (item.tier >= item.maxTier) {
      return Result.failure(IllegalStateException("Already at max Tier ${item.maxTier}"))
    }
    if (currentEssence < upgradeCost) {
      return Result.failure(IllegalStateException("Need $upgradeCost Essence to upgrade"))
    }
    return try {
      val now = System.currentTimeMillis()
      val profile = dao.getProfileOnce(userId)
        ?: return Result.failure(IllegalStateException("Operator profile not found"))
      dao.upsertArmoryItem(
        item.copy(
          userId = userId,
          tier = item.tier + 1L,
          powerValue = (item.powerValue + 35L).coerceAtMost(500L),
          updatedAtMillis = now,
        )
      )
      dao.upsertProfile(
        profile.copy(
          totalEssence = (currentEssence - upgradeCost).coerceAtLeast(0L),
          updatedAtMillis = now,
        )
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteArmoryItem(userId: String, itemId: String): Result<Unit> {
    return try {
      dao.deleteArmoryItem(userId, itemId)
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
      dao.upsertRig(
        RigAsset(
          id = resolvedId,
          userId = userId,
          name = safeName,
          archetype = archetype,
          activeClip = activeClip,
          ikEnabled = ikEnabled,
          rigScale = rigScale.coerceIn(0.5, 2.0),
          playbackSpeed = playbackSpeed.coerceIn(0.25, 3.0),
          poseAngles = poseAngles,
          keyframeFrames = safeFrames,
          isCompleted = isCompleted,
          createdAtMillis = existing?.createdAtMillis ?: now,
          updatedAtMillis = now,
        )
      )
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
      dao.insertMatchLog(
        MatchRecord(
          id = matchId,
          userId = userId,
          universeId = universeId,
          mode = mode,
          score = score.coerceIn(0L, 999_999_999L),
          waveReached = waveReached.coerceIn(0L, 9999L),
          eliminations = eliminations.coerceIn(0L, 99999L),
          extracted = extracted,
          durationSec = durationSec.coerceIn(0L, 86400L),
          createdAtMillis = now,
        )
      )
      val latestProfile = dao.getProfileOnce(userId) ?: profile
      dao.upsertProfile(
        latestProfile.copy(
          totalEssence = (latestProfile.totalEssence + essenceEarned).coerceAtMost(999_999_999L),
          highScore = maxOf(latestProfile.highScore, score),
          highestWave = maxOf(latestProfile.highestWave, waveReached),
          updatedAtMillis = now,
        )
      )
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
      dao.upsertLinkedAccount(
        LinkedAccount(
          id = "${userId}_$providerId",
          userId = userId,
          providerId = providerId,
          displayName = displayName.trim().take(64).ifEmpty { providerId },
          externalSubject = externalSubject.trim().take(128).ifEmpty { providerId },
          linkedAtMillis = System.currentTimeMillis(),
        )
      )
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
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
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
      maxTier = 5L,
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
      maxTier = 5L,
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
      maxTier = 5L,
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
      maxTier = 5L,
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
      maxTier = 5L,
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
      maxTier = 5L,
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
      maxTier = 5L,
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
      maxTier = 5L,
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

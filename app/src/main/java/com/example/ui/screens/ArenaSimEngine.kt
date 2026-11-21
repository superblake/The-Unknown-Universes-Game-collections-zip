package com.example.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.data.local.AppSettings
import com.example.data.local.ParticleDensity
import com.example.data.model.ArmoryItem
import com.example.data.model.BattlePassVersion
import com.example.data.model.BlastCoreColor
import com.example.data.model.EssenceColor
import com.example.data.model.EssenceResonanceBonuses
import com.example.data.model.GearSlot
import com.example.data.model.MatchProgressionReport
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.OperatorProgressionState
import com.example.data.model.PowerTier
import com.example.data.model.ProgressionCatalog
import com.example.data.model.StarRankMeta
import com.example.data.model.UniverseWorld
import com.example.data.model.WeaponSpec
import com.example.data.model.WonderWeaponKind
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

const val ARENA_WORLD_SIZE = 2.2f
const val ARENA_MIN_X = 0.06f
const val ARENA_MAX_X = 2.14f
const val ARENA_MIN_Y = 0.08f
const val ARENA_MAX_Y = 2.12f
const val ARENA_CENTER = 1.1f

enum class ArenaDropKind(val badge: String, val label: String, val color: Color) {
  DOUBLE_ESSENCE("2X", "DOUBLE BLUE ESSENCE (2X)", Color(0xFFFFD54F)),
  INSTA_KILL("IK", "INSTA-KILL ACTIVE!", Color(0xFFFF3D71)),
  NUKE("NK", "TACTICAL NUKE DETONATED!", Color(0xFF00E5FF)),
  MAX_ARMOR("MX", "MAX ARMOR REFILLED!", Color(0xFF00E676)),
}

enum class ArenaTrapKind(val label: String, val color: Color) {
  ELECTRIC("ARC TRAP", Color(0xFF00E5FF)),
  FIRE("INFERNO TRAP", Color(0xFFFF5722)),
}

enum class ArenaCollectibleKind(val label: String, val color: Color) {
  BLUE_ORB("Blue Essence", Color(0xFF00E5FF)),
  RED_ORB("Red Essence", Color(0xFFFF3D71)),
  YELLOW_ORB("Yellow Essence", Color(0xFFFFD54F)),
  GREEN_ORB("Green Essence", Color(0xFF00E676)),
  COMBINED_ORB("Combined Quad-Essence", Color(0xFFE040FB)),
  BLAST_SHARD("Blast Shard", Color(0xFF40C4FF)),
  PERK_DRINK("Perk Can/Bottle", Color(0xFFFFAB40)),
  BLAST_CORE("Blast Core", Color(0xFFE040FB)),
}

data class ArenaHostile(
  var x: Float,
  var y: Float,
  var hp: Int,
  val maxHp: Int,
  val speed: Float,
  val isElite: Boolean = false,
  val isBoss: Boolean = false,
  val archetype: String = "standard",
  val bossName: String = "",
  var rangedCooldownTicks: Float = 90f,
  var cryoSlowTicks: Float = 0f,
)

data class ArenaBolt(
  var x: Float,
  var y: Float,
  val vx: Float,
  val vy: Float,
  var life: Float,
  val isSupercharged: Boolean = false,
  val isEnemyShot: Boolean = false,
  var pierceLeft: Int = 0,
  val damage: Int = 1,
  val color: Color = Color(0xFF00E5FF),
)

data class ArenaIntel(
  val x: Float,
  val y: Float,
  var collected: Boolean = false,
)

data class ArenaDrop(
  var x: Float,
  var y: Float,
  val kind: ArenaDropKind,
  var life: Float = 720f,
)

data class ArenaCollectible(
  var x: Float,
  var y: Float,
  val kind: ArenaCollectibleKind,
  val subId: String = "",
  val amount: Long = 15L,
  val color: Color = kind.color,
  var life: Float = 1020f,
)

data class ArenaBuilding(
  val id: String,
  val name: String,
  val x: Float,
  val y: Float,
  val width: Float,
  val height: Float,
  val accentColor: Color,
  var explored: Boolean = false,
  var turretCooldownTicks: Float = 60f,
)

data class ArenaTrap(
  val id: String,
  val x: Float,
  val y: Float,
  val kind: ArenaTrapKind,
  val radius: Float = 0.14f,
  var activeTicks: Float = 0f,
  var cooldownTicks: Float = 0f,
)

data class HitSpark(
  val x: Float,
  val y: Float,
  val color: Color,
  var life: Float,
  val maxLife: Float = 18f,
)

data class ArenaArcLine(
  val x1: Float,
  val y1: Float,
  val x2: Float,
  val y2: Float,
  val color: Color,
  var life: Float = 14f,
)

data class ArenaWonderPickup(
  var x: Float,
  var y: Float,
  val kind: WonderWeaponKind,
  var life: Float = 960f,
)

data class ArenaBot(
  val id: String,
  val callsign: String,
  val isAllied: Boolean,
  val archetype: String,
  val color: Color,
  var x: Float,
  var y: Float,
  var hp: Int,
  val maxHp: Int,
  var fireCdTicks: Float,
  var roamAngle: Float,
)

data class ArenaLoadoutStats(
  val maxShield: Int,
  val moveBoost: Float,
  val multiShot: Int,
  val initialPackTier: Int,
  val hasClawEdge: Boolean,
  val clawTier: Int,
  val powerCooldownTicks: Float,
  val activePowerId: String,
  val activePowerName: String,
  val activePowerColor: Color,
  val activePowerRadius: Float,
  val activePowerDamage: Int,
  val hasTendrilPower: Boolean,
  val hasRepulsePower: Boolean,
  val hasChevronPower: Boolean,
  val passiveRegenPerSec: Float,
  val consumableHeal: Int,
  val consumableCooldownTicks: Float,
  val fireCadenceTicks: Float,
  val activePerksSummary: String,
  val weaponSpec: WeaponSpec = ProgressionCatalog.weapons.first(),
  val weaponLevel: Int = 1,
  val weaponTier: Int = 1,
  val weaponTierMultiplier: Float = 1.0f,
  val weaponBonusDamage: Int = 0,
  val weaponExtraSpread: Int = 0,
  val weaponExtraPierce: Int = 0,
  val projectileSpeed: Float = 0.020f,
  val tracerColor: Color = Color(0xFF00E5FF),
  val ammoModId: String = "standard_core",
  val playerLevel: Int = 1,
  val activePassVersion: BattlePassVersion = BattlePassVersion.STANDARD_SECTOR_OPS,
  val wonderDropSkillMult: Float = 1.0f,
  val wonderDurSkillMult: Float = 1.0f,
  val critDamageGearBonus: Float = 1.0f,
  val skillDashCdMult: Float = 1.0f,
  val resonance: EssenceResonanceBonuses = EssenceResonanceBonuses.compute(0L, 0L, 0L, 0L, 0L, 0L),
  val trapDamageBonus: Int = 0,
  val vacuumRadius: Float = 0.22f,
) {
  companion object {
    fun fromArmory(
      armory: List<ArmoryItem>,
      fallbackAccent: Color,
      progression: OperatorProgressionState = OperatorProgressionState(),
      blueEssence: Long? = null,
      weaponOverrideId: String? = null,
    ): ArenaLoadoutStats {
      val equipped = armory.filter { it.equipped }
      val perks = equipped.filter { it.category == "perk" }
      val powers = equipped.filter { it.category == "power" }
      val upgrades = equipped.filter { it.category == "upgrade" }
      val consumables = equipped.filter { it.category == "consumable" }

      val pass = progression.activePassVersion
      val targetWpnId = weaponOverrideId ?: progression.weaponCustom.equippedWeaponId
      val weapon = ProgressionCatalog.weaponById(targetWpnId)
      val wpnLevel = progression.weaponLevel(weapon.id)
      val wpnPrestige = progression.weaponPrestige(weapon.id)
      val wpnTier = progression.weaponTier(weapon.id)
      val wpnTierMult = ProgressionCatalog.tierMultiplier(wpnTier)
      val playerLvl = progression.playerLevel
      val playerPrestige = progression.playerPrestige
      val wc = progression.weaponCustom

      // Multi-Color Essence & Blast Shard Dynamic Resonance
      val resonance = if (blueEssence != null) {
        progression.essenceResonance(blueEssence)
      } else {
        EssenceResonanceBonuses.compute(0L, 0L, 0L, 0L, 0L, 0L)
      }

      // Active Character Outfit + its own Skills / Gear / Equipment + Star Level
      val outfitSpec = progression.activeOutfitSpec()
      val outfitSt = progression.outfitState(outfitSpec.id)
      val outfitStarAbove = (outfitSt.starLevel - 1).coerceAtLeast(0)
      val originPowerMult = if (outfitSpec.isOriginalCharacter) 1.25f else 1.0f
      val originMobilityBonus = if (outfitSpec.isOriginalCharacter) 0.08f else 0f
      val outfitShieldBonus = outfitSpec.baseShieldBonus +
        ((outfitSt.tier - 1) * 12) + ((outfitSt.gearRank - 1) * 8) + (outfitStarAbove * 14)
      val outfitDmgBonus = ((outfitSpec.baseDamageBonus + (outfitSt.skillRank - 1) + (outfitSt.tier - 1) + (outfitStarAbove * 2)) * originPowerMult).roundToInt()
      val outfitMoveBonus = outfitSpec.baseMoveBonus + originMobilityBonus + ((outfitSt.equipmentRank - 1) * 0.01f) + (outfitStarAbove * 0.015f)
      val outfitRateBonus = outfitSpec.baseFireRateBonus + originMobilityBonus + ((outfitSt.skillRank - 1) * 0.015f) + (outfitStarAbove * 0.02f)

      // Operator Character Tier (1..25) & Star Level (1..10★)
      val charTierAbove = (progression.characterTier - 1).coerceAtLeast(0)
      val charStarAbove = (progression.characterStarLevel - 1).coerceAtLeast(0)
      val charStarMult = StarRankMeta.forStars(progression.characterStarLevel).multiplier
      val charTierShield = charTierAbove * 18
      val charStarShield = charStarAbove * 24
      val charTierDmg = charTierAbove * 2

      // Active Area + Temporal Aging Era + Star Level + Story Area Benefits
      val areaSpec = progression.activeAreaSpec()
      val areaSt = progression.areaState()
      val era = areaSt.currentEra
      val areaCycleMult = areaSt.cycleBonusMult
      val areaStarMult = 1f + ((areaSt.starLevel - 1).coerceAtLeast(0) * 0.10f)
      val isMendedConvergence = areaSpec.id == "area_mended_convergence"
      val isCampaignNexus = areaSpec.id == "area_campaign_nexus"
      val storyAreaShield = when {
        isMendedConvergence -> 45
        isCampaignNexus -> 25
        else -> 0
      }
      val storyAreaDmgMult = when {
        isMendedConvergence -> 1.15f
        isCampaignNexus -> 1.10f
        else -> 1.0f
      }
      val storyAreaRegen = when {
        isMendedConvergence -> 4.0f
        isCampaignNexus -> 2.0f
        else -> 0f
      }
      val storyAreaMove = when {
        isMendedConvergence -> 0.10f
        isCampaignNexus -> 0.05f
        else -> 0f
      }

      // Weapon Star Multiplier
      val wpnStarMult = StarRankMeta.forStars(progression.weaponStar(weapon.id)).multiplier

      // Perk Cans & Bottles Ranks
      val juggPerkRank = progression.perkRank("perk_juggernog")
      val speedColaRank = progression.perkRank("perk_speed_cola")
      val doubleTapRank = progression.perkRank("perk_double_tap")
      val quickReviveRank = progression.perkRank("perk_quick_revive")
      val staminUpRank = progression.perkRank("perk_stamin_up")
      val electricCherryRank = progression.perkRank("perk_electric_cherry")
      val phdRank = progression.perkRank("perk_phd_flopper")

      // Colored Blast Core Levels (Blue, Purple, Green, Yellow, Red) — Lv.1 is baseline (+0)
      val blueCoreAbove = (progression.blastCoreLevel(BlastCoreColor.BLUE) - 1).coerceAtLeast(0)
      val purpleCoreAbove = (progression.blastCoreLevel(BlastCoreColor.PURPLE) - 1).coerceAtLeast(0)
      val greenCoreAbove = (progression.blastCoreLevel(BlastCoreColor.GREEN) - 1).coerceAtLeast(0)
      val yellowCoreAbove = (progression.blastCoreLevel(BlastCoreColor.YELLOW) - 1).coerceAtLeast(0)
      val redCoreAbove = (progression.blastCoreLevel(BlastCoreColor.RED) - 1).coerceAtLeast(0)

      // Skill Tree Nodes (Tier 1..25 + Star Level 1..10★)
      val skShield1Node = progression.skillNodes["sk_shield_1"]?.takeIf { it.unlocked }
      val skShield2Node = progression.skillNodes["sk_shield_2"]?.takeIf { it.unlocked }
      val skDmg1Node = progression.skillNodes["sk_damage_1"]?.takeIf { it.unlocked }
      val skDmg2Node = progression.skillNodes["sk_damage_2"]?.takeIf { it.unlocked }
      val skCdNode = progression.skillNodes["sk_cooldown_1"]?.takeIf { it.unlocked }
      val skWonderNode = progression.skillNodes["sk_wonder_1"]?.takeIf { it.unlocked }

      val skShield1 = ((skShield1Node?.tier ?: 0) * StarRankMeta.forStars(skShield1Node?.starLevel ?: 1).multiplier).roundToInt()
      val skShield2 = ((skShield2Node?.tier ?: 0) * StarRankMeta.forStars(skShield2Node?.starLevel ?: 1).multiplier).roundToInt()
      val skDmg1 = ((skDmg1Node?.tier ?: 0) * StarRankMeta.forStars(skDmg1Node?.starLevel ?: 1).multiplier).roundToInt()
      val skDmg2 = ((skDmg2Node?.tier ?: 0) * StarRankMeta.forStars(skDmg2Node?.starLevel ?: 1).multiplier).roundToInt()
      val skCd = ((skCdNode?.tier ?: 0) * StarRankMeta.forStars(skCdNode?.starLevel ?: 1).multiplier).roundToInt()
      val skWonder = ((skWonderNode?.tier ?: 0) * StarRankMeta.forStars(skWonderNode?.starLevel ?: 1).multiplier).roundToInt()

      // Gear Bonuses (Level 1..50, Tier 1..25, Star 1..10★, Prestige 0..30)
      val helmet = progression.gearMap[GearSlot.HELMET.id]
      val chest = progression.gearMap[GearSlot.CHEST.id]
      val gloves = progression.gearMap[GearSlot.GLOVES.id]
      val boots = progression.gearMap[GearSlot.BOOTS.id]
      val backRig = progression.gearMap[GearSlot.BACK_RIG.id]

      val chestStarAbove = ((chest?.starLevel ?: 1) - 1).coerceAtLeast(0)
      val bootsStarAbove = ((boots?.starLevel ?: 1) - 1).coerceAtLeast(0)
      val glovesStarAbove = ((gloves?.starLevel ?: 1) - 1).coerceAtLeast(0)
      val helmetStarAbove = ((helmet?.starLevel ?: 1) - 1).coerceAtLeast(0)
      val backRigStarAbove = ((backRig?.starLevel ?: 1) - 1).coerceAtLeast(0)

      val chestShield = ((chest?.level ?: 1) - 1) * 3 + (((chest?.tier ?: 1) - 1) * 18) + (chestStarAbove * 22) + progression.gearPrestige * 15
      val skillShield = skShield1 * 25 + skShield2 * 35

      val juggBonus = perks.sumOf { p ->
        val starExtra = (progression.armoryItemStar(p.id) - 1) * 15
        if (p.id == "perk_juggernaut" || p.name.contains("Juggernaut", ignoreCase = true) || p.name.contains("Aegis", ignoreCase = true)) {
          (p.tier * 40).toInt() + starExtra
        } else {
          (p.tier * 20).toInt() + starExtra
        }
      } + (juggPerkRank * 45) + (blueCoreAbove * 15) + ((areaSt.powerGridTier - 1) * 15)

      val levelShieldBonus = ((playerLvl - 1) * 2).coerceAtMost(600) + playerPrestige * 25
      val rawShield = 100 + juggBonus + levelShieldBonus + pass.bonusShield + chestShield + skillShield +
        outfitShieldBonus + charTierShield + charStarShield + storyAreaShield
      val maxShield = (rawShield * originPowerMult * resonance.shieldMult * areaStarMult).roundToInt().coerceAtLeast(100)

      val speedPerkCount = perks.sumOf { p ->
        if (p.id == "perk_overdrive" || p.name.contains("Speed", ignoreCase = true) || p.name.contains("Stamina", ignoreCase = true)) {
          p.tier.toInt()
        } else 0
      }
      val matchRules = progression.effectiveMatchRules()
      val bootsMoveBonus = (((boots?.level ?: 1) - 1) * 0.0035f) + ((boots?.tier ?: 1) - 1) * 0.02f + (bootsStarAbove * 0.025f)
      val stockMoveBonus = if (wc.stockId == "lightweight_skeleton") 0.08f else 0f
      val speedMutatorMult = if (matchRules.doubleSpeed) 1.45f else 1.0f
      val moveBoost = ((1f + speedPerkCount * 0.14f + (perks.size * 0.05f) + pass.moveSpeedBonus +
        skCd * 0.03f + bootsMoveBonus + stockMoveBonus + outfitMoveBonus + storyAreaMove +
        (staminUpRank * 0.10f) + (yellowCoreAbove * 0.04f)) * resonance.moveSpeedMult * era.speedMod * speedMutatorMult)
        .coerceIn(0.85f, 4.2f)

      val magExtraSpread = if (wc.magazineId == "extended_mag_2") 1 else 0
      val multiShot = (1 + upgrades.sumOf { (it.tier / 2).toInt().coerceAtLeast(1) } +
        (weapon.baseSpreadCount - 1) + magExtraSpread + (doubleTapRank / 3)).coerceAtMost(8)

      // Armory Pack Mod Tier 1..25 directly unlocks Pack Tier 1..25!
      val packItem = upgrades.firstOrNull { it.id == "upgrade_packtier" || it.name.contains("Pack", ignoreCase = true) }
      val initialPackTier = when {
        packItem == null -> 1
        else -> packItem.tier.toInt().coerceIn(1, 25)
      }

      val clawItem = upgrades.firstOrNull { it.id == "upgrade_claws" || it.name.contains("Claw", ignoreCase = true) || it.name.contains("Melee", ignoreCase = true) }
      val primaryPower = powers.firstOrNull()
      val skillCdMult = ((1f - skCd * 0.06f) * (1f - resonance.cooldownReductionRatio * 0.45f)).coerceIn(0.22f, 1.0f)
      val powerCdSec = ((primaryPower?.cooldownSec?.toFloat() ?: 6f) * pass.cooldownMultiplier * skillCdMult).coerceIn(0.9f, 30f)
      val customBonusDmg = ((primaryPower?.powerValue ?: 180L) / 70L).toInt().coerceIn(2, 24)
      val powerTierBonus = ((primaryPower?.tier ?: 1L) - 1L).toInt()
      val powerStarBonus = primaryPower?.let { (progression.armoryItemStar(it.id) - 1) * 2 } ?: 0
      val powerColor = primaryPower?.accentColorHex?.let { parseHexSafe(it, fallbackAccent) } ?: fallbackAccent

      val primaryConsumable = consumables.firstOrNull()
      val consumableStarBonus = primaryConsumable?.let { (progression.armoryItemStar(it.id) - 1) * 12 } ?: 0
      val cHeal = (45 + ((primaryConsumable?.tier ?: 1L) * 18L) + ((primaryConsumable?.powerValue ?: 100L) / 15L) + consumableStarBonus).toInt().coerceAtMost(550)
      val cCdSec = ((primaryConsumable?.cooldownSec?.toFloat() ?: 12f) * pass.cooldownMultiplier * skillCdMult).coerceIn(2.0f, 35f)

      val barrelBonusDmg = if (wc.barrelId == "heavy_bore") 2 else 0
      val muzzleBonusDmg = if (wc.muzzleId == "heavy_compensator" || wc.muzzleId == "monolithic_suppressor") 1 else 0
      val barrelExtraSpread = if (wc.barrelId == "spread_choke") 1 else 0
      val barrelSpeedMult = if (wc.barrelId == "overclocked_rifling") 1.22f else 1.0f

      val levelDmgBonus = ((wpnLevel - 1) / 3) + (wpnPrestige * 2)
      val skillDmgBonus = skDmg1 * 2 + skDmg2 * 3 + outfitDmgBonus + charTierDmg + (doubleTapRank * 2) + (redCoreAbove * 2)
      val rawTotalWpnDmg = ((weapon.baseDamage - 1).coerceAtLeast(0) + barrelBonusDmg + muzzleBonusDmg +
        levelDmgBonus + skillDmgBonus) * originPowerMult * wpnTierMult * wpnStarMult * charStarMult * resonance.damageMult * era.damageMod * areaCycleMult * areaStarMult * storyAreaDmgMult

      val glovesRateBonus = ((gloves?.level ?: 1) - 1) * 0.04f + ((gloves?.tier ?: 1) - 1) * 0.25f + (glovesStarAbove * 0.25f)
      val levelCadenceReduction = (((wpnLevel - 1) * 0.08f) + glovesRateBonus).coerceAtMost(4.5f)
      val mutatorRateMult = (if (matchRules.doubleSpeed) 1.75f else 1.0f) * (if (matchRules.infiniteAmmo) 1.25f else 1.0f)
      val totalRateMult = (pass.fireRateMultiplier + outfitRateBonus + (speedColaRank * 0.12f) + (yellowCoreAbove * 0.06f)) * resonance.fireRateMult * mutatorRateMult
      val rawCadence = (weapon.baseCadenceTicks - speedPerkCount * 0.9f - levelCadenceReduction) / totalRateMult.coerceAtLeast(1f)
      val fireCadence = rawCadence.coerceIn(1.8f, 15.0f)

      val wonderDropSkillMult = (1f + skWonder * 0.30f + ((areaSt.lootRichnessTier - 1) * 0.15f) + resonance.lootLuckBonus) *
        progression.clan.wonderPerkMultiplier * (if (matchRules.wonderSpawnBoost) 2.2f else 1.0f)
      val wonderDurSkillMult = 1f + skWonder * 0.15f + (((backRig?.level ?: 1) - 1) * 0.01f) + ((backRig?.tier ?: 1) - 1) * 0.08f + (backRigStarAbove * 0.10f) + (purpleCoreAbove * 0.18f)
      val critDmgGearBonus = 1f + (((helmet?.level ?: 1) - 1) * 0.02f) + ((helmet?.tier ?: 1) - 1) * 0.12f + (helmetStarAbove * 0.14f) + (redCoreAbove * 0.12f)
      val vacRadius = (progression.vacuumBot.vacuumRadius + greenCoreAbove * 0.018f).coerceAtMost(0.95f)
      val trapBonus = (electricCherryRank * 3) + (phdRank * 3) + (blueCoreAbove * 2) + ((areaSt.trapDensityTier - 1) * 2)

      val weaponBulletColor = if (wc.equippedWeaponId == weapon.id && wc.tracerColorHex != 0L) {
        Color(wc.tracerColorHex)
      } else {
        Color(weapon.defaultTracerHex)
      }

      val perkRegenStarBonus = perks.sumOf { (progression.armoryItemStar(it.id) - 1) * 0.5 }.toFloat()
      return ArenaLoadoutStats(
        maxShield = maxShield,
        moveBoost = moveBoost,
        multiShot = multiShot,
        initialPackTier = initialPackTier,
        hasClawEdge = clawItem != null || outfitSpec.id.startsWith("prototype"),
        clawTier = (clawItem?.tier?.toInt() ?: 1) + ((gloves?.level ?: 1) / 10) + (clawItem?.let { progression.armoryItemStar(it.id) - 1 } ?: 0),
        powerCooldownTicks = powerCdSec * 60f,
        activePowerId = primaryPower?.id ?: "surge",
        activePowerName = primaryPower?.name?.uppercase() ?: "MULTIVERSE SURGE",
        activePowerColor = powerColor,
        activePowerRadius = (0.28f + powerTierBonus * 0.03f).coerceAtMost(0.55f),
        activePowerDamage = ((customBonusDmg + powerTierBonus + powerStarBonus + outfitDmgBonus + charTierDmg) * charStarMult * resonance.damageMult).roundToInt(),
        hasTendrilPower = powers.any { it.id == "power_tendril" || it.name.contains("Tendril", ignoreCase = true) || it.name.contains("Bio", ignoreCase = true) },
        hasRepulsePower = powers.any { it.id == "power_repulse" || it.name.contains("Repulse", ignoreCase = true) || it.name.contains("Force", ignoreCase = true) },
        hasChevronPower = powers.any { it.id == "power_chevron" || it.name.contains("Chevron", ignoreCase = true) || it.name.contains("Chrono", ignoreCase = true) || it.name.contains("Stasis", ignoreCase = true) },
        passiveRegenPerSec = 1.5f + perks.size * 1.0f + perkRegenStarBonus + skShield2 * 1.0f + ((chest?.tier ?: 1) - 1) * 0.4f +
          (quickReviveRank * 2.0f) + (greenCoreAbove * 1.2f) + resonance.regenBonusPerSec + storyAreaRegen,
        consumableHeal = cHeal,
        consumableCooldownTicks = cCdSec * 60f,
        fireCadenceTicks = fireCadence,
        activePerksSummary = perks.take(2).joinToString(" • ") { it.name.substringBefore(" ") }
          .ifBlank { outfitSpec.nameWithVersion },
        weaponSpec = weapon,
        weaponLevel = wpnLevel,
        weaponTier = wpnTier,
        weaponTierMultiplier = wpnTierMult,
        weaponBonusDamage = rawTotalWpnDmg.roundToInt().coerceAtLeast(1),
        weaponExtraSpread = barrelExtraSpread,
        weaponExtraPierce = weapon.basePierce + ((wpnLevel - 1) / 6) + (skDmg2 / 2) + ((wpnTier - 1) / 3) + (if (matchRules.infiniteAmmo) 1 else 0),
        projectileSpeed = weapon.baseProjectileSpeed * barrelSpeedMult * (1f + ((wpnLevel - 1) * 0.008f).coerceAtMost(0.35f)),
        tracerColor = weaponBulletColor,
        ammoModId = wc.ammoModId,
        playerLevel = playerLvl,
        activePassVersion = pass,
        wonderDropSkillMult = wonderDropSkillMult,
        wonderDurSkillMult = wonderDurSkillMult,
        critDamageGearBonus = critDmgGearBonus,
        skillDashCdMult = skillCdMult,
        resonance = resonance,
        trapDamageBonus = trapBonus,
        vacuumRadius = vacRadius,
      )
    }
  }
}

@Stable
class ArenaSimState(
  val world: UniverseWorld,
  val modeId: String,
  loadout: ArenaLoadoutStats,
  val hasConsumableEquipped: Boolean = false,
  initialSettings: AppSettings = AppSettings(),
  val progression: OperatorProgressionState = OperatorProgressionState(),
  val armoryItems: List<ArmoryItem> = emptyList(),
  val initialBlueEssence: Long = 1000L,
) {
  var loadout: ArenaLoadoutStats = loadout
  var liveSettings: AppSettings = initialSettings

  // High-frequency Canvas state (read ONLY inside Canvas drawScope / simulation loop)
  var renderNonce by mutableLongStateOf(0L)
  var playerX = ARENA_CENTER
  var playerY = ARENA_CENTER
  var aimX = 1f
  var aimY = 0f
  var joyDx = 0f
  var joyDy = 0f
  var touchAimActive = false
  var touchAimX = ARENA_CENTER
  var touchAimY = ARENA_CENTER

  // Vacuum Bot live drone coordinates & charge
  var vacuumBotX = ARENA_CENTER + 0.055f
  var vacuumBotY = ARENA_CENTER - 0.055f
  var vacuumChargeFloat = 0f

  var elapsedTicks = 0L
  var frameTick = 0f
  var fireAutoTicker = 0f
  var powerCd = 0f
  var consumableCd = 0f
  var interactCd = 0f
  var lethalCd = 0f
  var tacticalCd = 0f
  var dashCd = 0f
  var dashInvulnTicks = 0f
  var overdriveTicks = 0f
  var stasisTicks = 0f
  var doubleEssenceTicks = 0f
  var instaKillTicks = 0f
  var damageFlashTicks = 0f
  var pulseRingRadius = 0f
  var brRingRadius = 0.92f
  var exfilHoldTicks = 0f
  var calloutTicks = 0f
  var regenAccum = 0f
  var brDamageAccum = 0f
  var meleeDamageAccum = 0f
  var activeWonderWeapon: WonderWeaponKind? = null
  var wonderWeaponTicks = 0f
  var playerShotCounter = 0

  val hostiles = ArrayList<ArenaHostile>(32)
  val bolts = ArrayList<ArenaBolt>(80)
  val intels = ArrayList<ArenaIntel>(4)
  val drops = ArrayList<ArenaDrop>(10)
  val collectibles = ArrayList<ArenaCollectible>(24)
  val buildings = ArrayList<ArenaBuilding>(8)
  val traps = ArrayList<ArenaTrap>(16)
  val wonderPickups = ArrayList<ArenaWonderPickup>(6)
  val sparks = ArrayList<HitSpark>(36)
  val arcs = ArrayList<ArenaArcLine>(20)
  val arenaBots = ArrayList<ArenaBot>(16)
  var maxPackTierReached = loadout.initialPackTier

  // Multi-Equip cycling
  val equippedWeaponIds: List<String> = progression.effectiveWeaponList()
  var activeWeaponIndex by mutableIntStateOf(0)
  val equippedWonderIds: List<String> = progression.effectiveWonderList()
  var activeWonderSlotIndex by mutableIntStateOf(0)

  // Collectible tallies earned in this Arena session
  var redEssenceEarned by mutableLongStateOf(0L)
  var yellowEssenceEarned by mutableLongStateOf(0L)
  var greenEssenceEarned by mutableLongStateOf(0L)
  var combinedEssenceEarned by mutableLongStateOf(0L)
  var blastShardsEarned by mutableLongStateOf(0L)
  var perkDrinksCollected by mutableIntStateOf(0)
  val blastCoresEarned = mutableMapOf<String, Long>()
  var buildingsExploredCount by mutableIntStateOf(0)
  var trapKillsCount by mutableIntStateOf(0)

  // Low-frequency HUD Snapshot States
  var hudShield by mutableIntStateOf(loadout.maxShield)
  var hudScore by mutableLongStateOf(0L)
  var hudWave by mutableLongStateOf(1L)
  var hudElims by mutableLongStateOf(0L)
  var hudBossElims by mutableLongStateOf(0L)
  var hudWonderPickedCount by mutableIntStateOf(0)
  var hudActiveWonder by mutableStateOf<WonderWeaponKind?>(null)
  var hudWonderSecLeft by mutableIntStateOf(0)
  var hudPackTier by mutableIntStateOf(loadout.initialPackTier)
  var hudBotCount by mutableIntStateOf(progression.lobbyConfig.activeBotCount)
  var hudIntelCollected by mutableIntStateOf(0)
  var hudChevronsLocked by mutableIntStateOf(1)
  var hudPowerCdSec by mutableIntStateOf(0)
  var hudConsumableCdSec by mutableIntStateOf(0)
  var hudInteractCdSec by mutableIntStateOf(0)
  var hudLethalCdSec by mutableIntStateOf(0)
  var hudTacticalCdSec by mutableIntStateOf(0)
  var hudVacuumChargePct by mutableIntStateOf(0)
  var hudActiveBuildingName by mutableStateOf<String?>(null)
  var hudActiveWeaponName by mutableStateOf(loadout.weaponSpec.name)

  val equippedLethalItem: com.example.data.model.EquipmentItemSpec
    get() = ProgressionCatalog.equipmentItems.firstOrNull { it.id == progression.equippedLethalIds.firstOrNull() }
      ?: ProgressionCatalog.equipmentItems.firstOrNull { it.id == progression.equipmentState("lethal").equippedItemId }
      ?: ProgressionCatalog.equipmentItems[0]
  val equippedTacticalItem: com.example.data.model.EquipmentItemSpec
    get() = ProgressionCatalog.equipmentItems.firstOrNull { it.id == progression.equippedTacticalIds.firstOrNull() }
      ?: ProgressionCatalog.equipmentItems.firstOrNull { it.id == progression.equipmentState("tactical").equippedItemId }
      ?: ProgressionCatalog.equipmentItems[4]
  val hudLethalName: String
    get() = if (progression.equippedLethalIds.size > 1) "${equippedLethalItem.name.substringBefore(" ").uppercase()} +${progression.equippedLethalIds.size - 1}"
    else equippedLethalItem.name.substringBefore(" ").uppercase()
  val hudTacticalName: String
    get() = if (progression.equippedTacticalIds.size > 1) "${equippedTacticalItem.name.substringBefore(" ").uppercase()} +${progression.equippedTacticalIds.size - 1}"
    else equippedTacticalItem.name.substringBefore(" ").uppercase()
  var hudDashReady by mutableStateOf(true)
  var hudOverdriveActive by mutableStateOf(false)
  var hudActiveDropText by mutableStateOf<String?>(null)
  var hudCallout by mutableStateOf<String?>(null)
  var hudBossName by mutableStateOf<String?>(null)
  var hudBossHpRatio by mutableFloatStateOf(0f)
  var hudExfilRatio by mutableFloatStateOf(0f)
  var hudAutoFire by mutableStateOf(initialSettings.autoFireEnabled)
  var hudRunEnded by mutableStateOf(false)
  var hudExtractedSuccess by mutableStateOf(false)

  private val accentColor = Color(world.accentHex)
  val matchRules = progression.effectiveMatchRules()

  init {
    setupBuildingsAndTraps()
    spawnLobbyBots()
    spawnAreaCollectibles()
    val initialWave = matchRules.startingWave.toLong().coerceAtLeast(1L)
    hudWave = initialWave
    spawnWave(initialWave)
  }

  fun cameraOriginX(): Float = (playerX - 0.5f).coerceIn(0f, ARENA_WORLD_SIZE - 1.0f)
  fun cameraOriginY(): Float = (playerY - 0.5f).coerceIn(0f, ARENA_WORLD_SIZE - 1.0f)

  private fun setupBuildingsAndTraps() {
    buildings.clear()
    traps.clear()
    val areaSpec = progression.activeAreaSpec()
    val areaState = progression.areaState()
    val bNames = areaSpec.buildingNames
    val positions = listOf(
      Pair(0.28f, 0.30f),
      Pair(1.52f, 0.28f),
      Pair(0.26f, 1.54f),
      Pair(1.54f, 1.54f),
      Pair(0.92f, 0.26f),
      Pair(0.92f, 1.62f),
      Pair(0.28f, 0.94f),
      Pair(1.56f, 0.94f),
    )
    val countToSpawn = bNames.size.coerceAtMost(positions.size)
    for (idx in 0 until countToSpawn) {
      val (bx, by) = positions[idx]
      val name = bNames[idx]
      val bColor = if (idx % 2 == 0) Color(areaSpec.accentHex) else Color(areaSpec.secondaryHex)
      buildings.add(
        ArenaBuilding(
          id = "bld_$idx",
          name = name,
          x = bx,
          y = by,
          width = 0.28f,
          height = 0.22f,
          accentColor = bColor,
        )
      )
      // Place an Electric or Fire Trap near each building entrance
      traps.add(
        ArenaTrap(
          id = "trap_b_$idx",
          x = (bx + 0.14f).coerceIn(ARENA_MIN_X, ARENA_MAX_X),
          y = (by + 0.26f).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y),
          kind = if (idx % 2 == 0) ArenaTrapKind.ELECTRIC else ArenaTrapKind.FIRE,
          radius = 0.14f + areaState.trapDensityTier * 0.008f,
        )
      )
    }
    // Central quadrant Electric & Fire Traps surrounding spawn courtyard
    val ringOffsets = listOf(
      Triple(ARENA_CENTER - 0.34f, ARENA_CENTER, ArenaTrapKind.ELECTRIC),
      Triple(ARENA_CENTER + 0.34f, ARENA_CENTER, ArenaTrapKind.FIRE),
      Triple(ARENA_CENTER, ARENA_CENTER - 0.34f, ArenaTrapKind.ELECTRIC),
      Triple(ARENA_CENTER, ARENA_CENTER + 0.34f, ArenaTrapKind.FIRE),
    )
    ringOffsets.forEachIndexed { idx, (tx, ty, kind) ->
      traps.add(ArenaTrap("trap_mid_$idx", tx, ty, kind, radius = 0.13f + areaState.trapDensityTier * 0.008f))
    }
  }

  private fun spawnAreaCollectibles() {
    // Spawn initial Blast Shards, Perk Cans/Bottles, Colored Essence Orbs, and Blast Cores inside buildings
    buildings.forEachIndexed { idx, b ->
      val cx = b.x + b.width * 0.5f
      val cy = b.y + b.height * 0.5f
      collectibles.add(ArenaCollectible(cx - 0.05f, cy, ArenaCollectibleKind.BLAST_SHARD, amount = 4L))
      val perk = MultiverseArsenalCatalog.perkDrinks[idx % MultiverseArsenalCatalog.perkDrinks.size]
      collectibles.add(ArenaCollectible(cx + 0.05f, cy, ArenaCollectibleKind.PERK_DRINK, subId = perk.id, amount = 1L, color = Color(perk.colorHex)))
      val core = BlastCoreColor.entries[idx % BlastCoreColor.entries.size]
      collectibles.add(ArenaCollectible(cx, cy + 0.04f, ArenaCollectibleKind.BLAST_CORE, subId = core.id, amount = 1L, color = Color(core.colorHex)))
    }
  }

  fun cycleActiveWeapon() {
    if (equippedWeaponIds.size <= 1) {
      postCallout("${loadout.weaponSpec.name} (Equip up to 3 in Armory)")
      return
    }
    activeWeaponIndex = (activeWeaponIndex + 1) % equippedWeaponIds.size
    val nextId = equippedWeaponIds[activeWeaponIndex]
    loadout = ArenaLoadoutStats.fromArmory(
      armory = armoryItems,
      fallbackAccent = accentColor,
      progression = progression,
      blueEssence = initialBlueEssence,
      weaponOverrideId = nextId,
    )
    hudActiveWeaponName = loadout.weaponSpec.name
    postCallout("SWAPPED WEAPON: ${loadout.weaponSpec.name.uppercase()}")
  }

  fun activateEquippedWonderSlot() {
    if (equippedWonderIds.isEmpty()) return
    activeWonderSlotIndex = (activeWonderSlotIndex + 1) % equippedWonderIds.size
    val wId = equippedWonderIds[activeWonderSlotIndex]
    val kind = WonderWeaponKind.entries.firstOrNull { it.id == wId } ?: WonderWeaponKind.RAY_NOVA_MK3
    collectWonderPickup(kind)
  }

  fun triggerAllTraps() {
    if (hudRunEnded) return
    val bonus = loadout.trapDamageBonus
    val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L
    var totalHits = 0
    for (ti in 0 until traps.size) {
      val t = traps[ti]
      if (t.cooldownTicks <= 0f) {
        t.activeTicks = 240f
        t.cooldownTicks = 300f
        val rSq = (t.radius * 1.4f) * (t.radius * 1.4f)
        val iter = hostiles.iterator()
        while (iter.hasNext()) {
          val h = iter.next()
          val dx = h.x - t.x
          val dy = h.y - t.y
          if (dx * dx + dy * dy <= rSq) {
            h.hp -= (6 + bonus)
            arcs.add(ArenaArcLine(t.x, t.y, h.x, h.y, t.kind.color, 20f))
            totalHits++
            if (h.hp <= 0) {
              iter.remove()
              trapKillsCount++
              onHostileDefeated(h, scoreMult)
            }
          }
        }
      }
    }
    pulseRingRadius = 0.05f
    postCallout("ALL ELECTRIC & FIRE TRAPS ARMED! ($totalHits hits)")
  }

  private fun spawnLobbyBots() {
    arenaBots.clear()
    val roster = ProgressionCatalog.generateLobbyRoster(
      config = progression.lobbyConfig,
      playerCallsign = "Vanguard",
      playerLevel = progression.playerLevel,
      playerPrestige = progression.playerPrestige,
    )
    val bots = roster.filter { !it.isHumanPlayer }.take(16)
    hudBotCount = progression.lobbyConfig.activeBotCount
    bots.forEachIndexed { idx, member ->
      val angle = idx * (2f * PI.toFloat() / bots.size.coerceAtLeast(1))
      val dist = if (member.isAlliedSquadmate) 0.16f else 0.42f
      arenaBots.add(
        ArenaBot(
          id = member.id,
          callsign = member.callsign,
          isAllied = member.isAlliedSquadmate,
          archetype = member.archetype,
          color = Color(member.accentHex),
          x = (playerX + cos(angle) * dist).coerceIn(ARENA_MIN_X, ARENA_MAX_X),
          y = (playerY + sin(angle) * dist).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y),
          hp = 120 + member.level / 2,
          maxHp = 120 + member.level / 2,
          fireCdTicks = 28f + (idx * 7f) % 24f,
          roamAngle = angle,
        )
      )
    }
  }

  fun postCallout(text: String) {
    hudCallout = text
    calloutTicks = 120f
  }

  fun buildProgressionReport(): MatchProgressionReport = MatchProgressionReport(
    eliminations = hudElims,
    bossEliminations = hudBossElims,
    waveReached = hudWave,
    wonderPickupsCollected = hudWonderPickedCount,
    weaponIdUsed = loadout.weaponSpec.id,
    extracted = hudExtractedSuccess || modeId == "sandbox",
    highestPackTierReached = maxOf(maxPackTierReached, hudPackTier),
    lobbySizePlayed = progression.lobbyConfig.targetPlayers,
    redEssenceEarned = redEssenceEarned,
    yellowEssenceEarned = yellowEssenceEarned,
    greenEssenceEarned = greenEssenceEarned,
    combinedEssenceEarned = combinedEssenceEarned,
    blastShardsEarned = blastShardsEarned,
    perkDrinksCollected = perkDrinksCollected,
    blastCoresEarned = blastCoresEarned.toMap(),
    buildingsExplored = buildingsExploredCount,
    trapKills = trapKillsCount,
    worldId = world.id,
    areaId = progression.activeAreaId,
  )

  private fun addSparkIfAllowed(x: Float, y: Float, color: Color, life: Float) {
    val cap = when {
      liveSettings.particleDensity == ParticleDensity.OFF -> 0
      liveSettings.reduceMotion || liveSettings.particleDensity == ParticleDensity.REDUCED -> 8
      else -> 36
    }
    if (cap <= 0 || sparks.size >= cap) return
    sparks.add(HitSpark(x, y, color, life))
  }

  fun spawnWave(w: Long) {
    val lc = progression.levelCustom
    val rawCount = if (modeId == "sandbox") 5 else (4 + w * 2).toInt().coerceAtMost(20)
    val count = (rawCount * lc.enemyDensityMultiplier).roundToInt().coerceIn(2, 26)
    for (i in 0 until count) {
      val angle = Random.nextFloat() * 2f * PI.toFloat()
      val r = 0.55f + (Random.nextFloat() * 0.25f)
      val isElite = (i == 0 && w >= 2)
      val archetype = chooseArchetype(world.id, w, i)
      val rawHp = 1 + (w.toInt() / 2) + if (archetype == "mech") 2 else if (isElite) 2 else 0
      val baseHp = if (lc.instagibMutator) 1 else rawHp
      val spd = when (archetype) {
        "rusher" -> (0.0018f + w * 0.00018f).coerceAtMost(0.0040f)
        "mech" -> (0.0008f + w * 0.00011f).coerceAtMost(0.0021f)
        else -> (0.0011f + w * 0.00015f).coerceAtMost(0.0032f)
      }
      hostiles.add(
        ArenaHostile(
          x = (playerX + cos(angle) * r).coerceIn(ARENA_MIN_X, ARENA_MAX_X),
          y = (playerY + sin(angle) * r).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y),
          hp = baseHp,
          maxHp = baseHp,
          speed = spd,
          isElite = isElite,
          archetype = archetype,
          rangedCooldownTicks = 85f + Random.nextFloat() * 80f,
        )
      )
    }

    if (lc.bossRushMutator || matchRules.bossRush || w % 3L == 0L || (modeId == "br" && w == 4L)) {
      spawnBossForWorld(w)
    }

    if (modeId == "dmz" && intels.isEmpty()) {
      intels.add(ArenaIntel(0.36f, 0.36f))
      intels.add(ArenaIntel(1.66f, 0.42f))
      intels.add(ArenaIntel(1.64f, 1.66f))
    }
  }

  fun spawnRandomWonderWeapon(
    x: Float = (playerX - 0.25f + Random.nextFloat() * 0.50f),
    y: Float = (playerY - 0.25f + Random.nextFloat() * 0.50f),
    fixedKind: WonderWeaponKind? = null,
  ): WonderWeaponKind {
    val chosen = fixedKind ?: pickWeightedWonderKind()
    if (wonderPickups.size >= 6) {
      wonderPickups.removeAt(0)
    }
    wonderPickups.add(ArenaWonderPickup(x.coerceIn(ARENA_MIN_X + 0.04f, ARENA_MAX_X - 0.04f), y.coerceIn(ARENA_MIN_Y + 0.04f, ARENA_MAX_Y - 0.04f), chosen))
    postCallout("WONDER CRATE: ${chosen.displayName.uppercase()}!")
    return chosen
  }

  private fun pickWeightedWonderKind(): WonderWeaponKind {
    val pass = progression.activePassVersion
    if (pass == BattlePassVersion.CHRONO_MULTIVERSE && Random.nextFloat() < 0.40f) {
      return WonderWeaponKind.CHRONO_ZAT
    }
    if (pass == BattlePassVersion.HELLBREAKER_SUPREME && Random.nextFloat() < 0.40f) {
      return WonderWeaponKind.CRUCIBLE_BFG
    }
    return WonderWeaponKind.entries[Random.nextInt(WonderWeaponKind.entries.size)]
  }

  fun spawnBossForWorld(w: Long) {
    val (arch, name) = if (progression.activeAreaId == "area_mended_convergence") {
      if (w % 2L == 0L) "boss_hydra" to "MENDED CONVERGENCE LOOM" else "boss_mech" to "FRACTURE REMNANT SOVEREIGN"
    } else {
      when (world.id) {
        "blacklight_conduit" -> "boss_hydra" to "WATER HYDRA ALPHA"
        "hellbreaker_citadel" -> if (w % 2L == 0L) "boss_mech" to "ASSAULT MECH TITAN" else "boss_croc" to "KILLER CROC JUGGERNAUT"
        "aegis_starblade" -> "boss_croc" to "BLACK GATE KILLER CROC"
        "chrono_vault" -> "boss_hydra" to "CHRONO RIFT WARDEN"
        "campaign_origins" -> if (w % 2L == 0L) "boss_mech" to "CONVERGENCE LOOM TITAN" else "boss_hydra" to "RIFT SOVEREIGN PRIME"
        else -> "boss_mech" to "GENTEK CONTAINMENT MECH"
      }
    }
    val hp = 12 + w.toInt() * 4
    hostiles.add(
      ArenaHostile(
        x = playerX,
        y = (playerY - 0.42f).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y),
        hp = hp,
        maxHp = hp,
        speed = 0.0010f,
        isElite = true,
        isBoss = true,
        archetype = arch,
        bossName = name,
        rangedCooldownTicks = 65f,
      )
    )
    postCallout("BOSS INBOUND: $name!")
  }

  private fun chooseArchetype(worldId: String, wave: Long, index: Int): String =
    when (worldId) {
      "blacklight_conduit" -> when {
        index % 4 == 1 -> "trooper"
        index % 4 == 2 -> "slasher"
        index % 4 == 3 && wave >= 2 -> "rusher"
        else -> "standard"
      }
      "hellbreaker_citadel" -> when {
        index % 3 == 1 -> "slasher"
        index % 4 == 2 -> "mech"
        else -> "standard"
      }
      "chrono_vault" -> when {
        index % 3 == 1 -> "trooper"
        index % 4 == 2 -> "slasher"
        else -> "standard"
      }
      "aegis_starblade" -> when {
        index % 3 == 1 -> "trooper"
        index % 4 == 3 -> "mech"
        else -> "rusher"
      }
      "campaign_origins" -> when {
        index % 4 == 1 -> "trooper"
        index % 4 == 2 -> "slasher"
        index % 4 == 3 -> "mech"
        else -> "rusher"
      }
      else -> when {
        wave >= 2 && index % 4 == 1 -> "rusher"
        index % 4 == 2 -> "trooper"
        else -> "standard"
      }
    }

  private fun autoAimTowardNearestIfNeeded() {
    if (!liveSettings.autoAimEnabled || touchAimActive || hostiles.isEmpty()) return
    var best: ArenaHostile? = null
    var bestDistSq = Float.MAX_VALUE
    for (i in 0 until hostiles.size) {
      val h = hostiles[i]
      val dx = h.x - playerX
      val dy = h.y - playerY
      val dSq = dx * dx + dy * dy
      if (dSq < bestDistSq) {
        bestDistSq = dSq
        best = h
      }
    }
    if (best != null) {
      val dx = best.x - playerX
      val dy = best.y - playerY
      val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
      aimX = dx / d
      aimY = dy / d
    }
  }

  fun setTouchAim(worldNormX: Float, worldNormY: Float, active: Boolean) {
    touchAimActive = active
    touchAimX = worldNormX.coerceIn(0f, ARENA_WORLD_SIZE)
    touchAimY = worldNormY.coerceIn(0f, ARENA_WORLD_SIZE)
    if (active) {
      val dx = touchAimX - playerX
      val dy = touchAimY - playerY
      val d = sqrt(dx * dx + dy * dy)
      if (d > 0.01f) {
        aimX = dx / d
        aimY = dy / d
      }
    }
  }

  private fun packDamageMultiplier(tier: Int): Int = when (tier.coerceIn(1, 25)) {
    1 -> 1
    2 -> 2
    3 -> 4
    4 -> 6
    5 -> 9
    6 -> 12
    7 -> 16
    8 -> 20
    9 -> 25
    10 -> 32
    11 -> 40
    12 -> 50
    13 -> 62
    14 -> 76
    15 -> 92
    16 -> 112
    17 -> 135
    18 -> 162
    19 -> 195
    20 -> 232
    21 -> 275
    22 -> 325
    23 -> 380
    24 -> 445
    else -> 520
  }

  fun fireVolley(forceManual: Boolean = false) {
    if (hudRunEnded) return
    if (!touchAimActive) {
      autoAimTowardNearestIfNeeded()
    }
    playerShotCounter++
    val baseAngle = atan2(aimY, aimX)
    val packLevel = hudPackTier.coerceIn(1, 25)
    val wonder = activeWonderWeapon
    val critBoost = if (progression.levelCustom.critSurgeMutator && playerShotCounter % 3 == 0) 2f else 1f
    val instagibBoost = if (progression.levelCustom.instagibMutator || matchRules.headshotsOnly) 3 else 1

    if (wonder != null) {
      val wonderCol = Color(wonder.accentHex)
      val wonderTierMult = ProgressionCatalog.tierMultiplier(progression.wonderTier(wonder.id))
      val wonderStarMult = StarRankMeta.forStars(progression.wonderStar(wonder.id)).multiplier
      val purpleBonus = 1f + (progression.blastCoreLevel(BlastCoreColor.PURPLE) * 0.10f)
      val wonderDmg = ((wonder.baseDamage + packLevel * 2) * wonderTierMult * wonderStarMult * purpleBonus * critBoost * loadout.critDamageGearBonus * instagibBoost).roundToInt().coerceAtLeast(1)
      when (wonder) {
        WonderWeaponKind.STARBLADE_BOOMERANG -> {
          for (i in 0 until 6) {
            val a = baseAngle + i * (2f * PI.toFloat() / 6f)
            bolts.add(
              ArenaBolt(
                x = playerX,
                y = playerY,
                vx = cos(a) * 0.025f,
                vy = sin(a) * 0.025f,
                life = 56f,
                isSupercharged = true,
                pierceLeft = 3,
                damage = wonderDmg,
                color = wonderCol,
              )
            )
          }
        }
        WonderWeaponKind.RAY_NOVA_MK3 -> {
          for (offset in listOf(-0.08f, 0.08f)) {
            val a = baseAngle + offset
            bolts.add(
              ArenaBolt(
                x = playerX,
                y = playerY,
                vx = cos(a) * 0.027f,
                vy = sin(a) * 0.027f,
                life = 64f,
                isSupercharged = true,
                pierceLeft = 2,
                damage = wonderDmg,
                color = wonderCol,
              )
            )
          }
        }
        WonderWeaponKind.CRUCIBLE_BFG -> {
          bolts.add(
            ArenaBolt(
              x = playerX,
              y = playerY,
              vx = cos(baseAngle) * 0.018f,
              vy = sin(baseAngle) * 0.018f,
              life = 72f,
              isSupercharged = true,
              pierceLeft = 5,
              damage = wonderDmg + 2,
              color = wonderCol,
            )
          )
        }
        WonderWeaponKind.BLACKLIGHT_WHIP -> {
          bolts.add(
            ArenaBolt(
              x = playerX,
              y = playerY,
              vx = cos(baseAngle) * 0.030f,
              vy = sin(baseAngle) * 0.030f,
              life = 48f,
              isSupercharged = true,
              pierceLeft = 99,
              damage = wonderDmg,
              color = wonderCol,
            )
          )
        }
        WonderWeaponKind.LIBERTY_WUNDERWAFFE, WonderWeaponKind.CHRONO_ZAT -> {
          bolts.add(
            ArenaBolt(
              x = playerX,
              y = playerY,
              vx = cos(baseAngle) * 0.028f,
              vy = sin(baseAngle) * 0.028f,
              life = 58f,
              isSupercharged = true,
              pierceLeft = 1,
              damage = wonderDmg,
              color = wonderCol,
            )
          )
        }
      }
      if (forceManual) {
        fireAutoTicker = loadout.fireCadenceTicks * 0.55f
      }
      return
    }

    val shots = (loadout.multiShot + loadout.weaponExtraSpread + ((packLevel - 1) / 2).coerceAtMost(4) + if (overdriveTicks > 0f) 1 else 0).coerceAtMost(8)
    val spread = if (shots > 4) 0.12f else 0.16f
    val packDamageMult = packDamageMultiplier(packLevel) + (progression.blastCoreLevel(BlastCoreColor.RED) - 1)
    val baseDmg = ((packLevel + loadout.weaponBonusDamage) * packDamageMult + if (overdriveTicks > 0f) 2 else 0)
    val dmg = (baseDmg * critBoost * loadout.critDamageGearBonus * instagibBoost).roundToInt().coerceAtLeast(1)
    val pierce = (packLevel - 1 + loadout.weaponExtraPierce).coerceAtLeast(0)
    val weaponDefaultColor = when (loadout.weaponSpec.id) {
      "wpn_pulse_carbine" -> Color(0xFF00E5FF)
      "wpn_plasma_scatter" -> Color(0xFF7C4DFF)
      "wpn_starblade_lancer" -> Color(0xFFE040FB)
      "wpn_chrono_repeater" -> Color(0xFFFFD54F)
      "wpn_crucible_cannon", "wpn_hellbreaker_cannon" -> Color(0xFFFF3D71)
      else -> loadout.tracerColor
    }
    val boltColor = when {
      instaKillTicks > 0f -> Color(0xFFFF1744)
      critBoost > 1f -> Color(0xFFFFD700)
      packLevel >= 9 -> Color(0xFFFFFFFF)
      packLevel >= 7 -> Color(0xFF00E676)
      packLevel >= 5 -> Color(0xFFFF3D71)
      packLevel >= 3 -> Color(0xFFFFAB00)
      packLevel == 2 -> Color(0xFFE040FB)
      overdriveTicks > 0f -> Color(0xFFFF6D00)
      else -> weaponDefaultColor
    }
    val speed = loadout.projectileSpeed + (packLevel * 0.0018f).coerceAtMost(0.015f)
    for (i in 0 until shots) {
      val offset = (i - (shots - 1) / 2f) * spread
      val a = baseAngle + offset
      bolts.add(
        ArenaBolt(
          x = playerX,
          y = playerY,
          vx = cos(a) * speed,
          vy = sin(a) * speed,
          life = 64f,
          isSupercharged = overdriveTicks > 0f || packLevel >= 2 || critBoost > 1,
          pierceLeft = pierce,
          damage = dmg,
          color = boltColor,
        )
      )
    }
    if (forceManual) {
      fireAutoTicker = loadout.fireCadenceTicks * 0.6f
    }
  }

  fun triggerPower() {
    if (hudRunEnded || powerCd > 0f) return
    powerCd = loadout.powerCooldownTicks
    pulseRingRadius = 0.04f

    val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L
    val killRadiusSq = loadout.activePowerRadius * loadout.activePowerRadius
    val baseDmg = loadout.activePowerDamage

    if (loadout.hasChevronPower || world.id == "chrono_vault") {
      stasisTicks = 210f
    }

    if (loadout.hasRepulsePower || world.id == "aegis_starblade") {
      bolts.removeAll { it.isEnemyShot }
      for (i in 0 until 8) {
        val a = i * (2f * PI.toFloat() / 8f)
        bolts.add(
          ArenaBolt(
            x = playerX,
            y = playerY,
            vx = cos(a) * 0.022f,
            vy = sin(a) * 0.022f,
            life = 48f,
            isSupercharged = true,
            pierceLeft = 1,
            damage = 2,
            color = Color(0xFF00E5FF),
          )
        )
      }
      for (i in 0 until hostiles.size) {
        val h = hostiles[i]
        val dx = h.x - playerX
        val dy = h.y - playerY
        val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
        if (d < 0.42f) {
          h.x = (h.x + (dx / d) * 0.16f).coerceIn(ARENA_MIN_X, ARENA_MAX_X)
          h.y = (h.y + (dy / d) * 0.16f).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y)
        }
      }
    }

    if (loadout.hasTendrilPower || world.id == "blacklight_conduit") {
      var chained = 0
      for (i in 0 until hostiles.size) {
        if (chained >= 8) break
        val h = hostiles[i]
        val dx = h.x - playerX
        val dy = h.y - playerY
        if (dx * dx + dy * dy <= 0.46f * 0.46f) {
          arcs.add(ArenaArcLine(playerX, playerY, h.x, h.y, loadout.activePowerColor, 18f))
          chained++
        }
      }
    }

    val iter = hostiles.iterator()
    while (iter.hasNext()) {
      val h = iter.next()
      val dx = h.x - playerX
      val dy = h.y - playerY
      if (dx * dx + dy * dy <= killRadiusSq) {
        h.hp -= baseDmg
        addSparkIfAllowed(h.x, h.y, loadout.activePowerColor, 18f)
        if (h.hp <= 0) {
          iter.remove()
          onHostileDefeated(h, scoreMult)
        }
      }
    }
    postCallout("${loadout.activePowerName}!")
  }

  fun triggerWorldInteract() {
    if (hudRunEnded || interactCd > 0f) return
    interactCd = 360f
    overdriveTicks = 300f
    pulseRingRadius = 0.05f
    val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L

    if (modeId == "zombies" || world.id == "sector_zero") {
      upgradePackAPunch()
      return
    }

    when (world.id) {
      "chrono_vault" -> {
        val nextChevron = (hudChevronsLocked % 7) + 1
        hudChevronsLocked = nextChevron
        stasisTicks = 150f
        if (nextChevron == 7) {
          val iter = hostiles.iterator()
          while (iter.hasNext()) {
            val h = iter.next()
            h.hp -= if (h.isBoss) 6 else 99
            arcs.add(ArenaArcLine(playerX, playerY, h.x, h.y, Color(0xFF00E5FF), 22f))
            if (h.hp <= 0) {
              iter.remove()
              onHostileDefeated(h, scoreMult)
            }
          }
          postCallout("CHEVRON 7 LOCKED! EVENT HORIZON OPEN")
        } else {
          postCallout("CHEVRON $nextChevron/7 LOCKED • STASIS FIELD")
        }
      }
      "blacklight_conduit" -> {
        val limit = minOf(8, hostiles.size)
        for (i in 0 until limit) {
          val h = hostiles[i]
          h.hp -= 4
          arcs.add(ArenaArcLine(playerX, playerY, h.x, h.y, Color(0xFF00E5FF), 20f))
          addSparkIfAllowed(h.x, h.y, Color(0xFF00E5FF), 22f)
        }
        val iter = hostiles.iterator()
        while (iter.hasNext()) {
          val h = iter.next()
          if (h.hp <= 0) {
            iter.remove()
            onHostileDefeated(h, scoreMult)
          }
        }
        postCallout("LIBERTY ARC PYLON DISCHARGE!")
      }
      "hellbreaker_citadel" -> {
        hudShield = (hudShield + 45).coerceAtMost(loadout.maxShield)
        instaKillTicks = maxOf(instaKillTicks, 180f)
        postCallout("CRUCIBLE FURY: GLORY SIPHON!")
      }
      else -> {
        bolts.removeAll { it.isEnemyShot }
        for (i in 0 until hostiles.size) {
          val h = hostiles[i]
          val dx = h.x - playerX
          val dy = h.y - playerY
          val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
          h.x = (h.x + (dx / d) * 0.16f).coerceIn(ARENA_MIN_X, ARENA_MAX_X)
          h.y = (h.y + (dy / d) * 0.16f).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y)
        }
        postCallout("SHIP OF HEROES FORCE REPULSE!")
      }
    }
  }

  fun triggerConsumable() {
    if (hudRunEnded || (!hasConsumableEquipped && modeId != "sandbox") || consumableCd > 0f) return
    consumableCd = loadout.consumableCooldownTicks
    hudShield = (hudShield + loadout.consumableHeal).coerceAtMost(loadout.maxShield)
    powerCd = 0f
    overdriveTicks = maxOf(overdriveTicks, 180f)
    pulseRingRadius = 0.04f
    postCallout("OMNI-SURGE: +${loadout.consumableHeal} ARMOR & CD RESET")
  }

  fun upgradePackAPunch() {
    if (hudRunEnded) return
    if (hudPackTier < 25) {
      hudPackTier++
      maxPackTierReached = maxOf(maxPackTierReached, hudPackTier)
      val roman = formatRoundRoman(hudPackTier.toLong())
      overdriveTicks = 240f
      pulseRingRadius = 0.05f
      postCallout("PACK-A-PUNCH TIER $roman! (x${packDamageMultiplier(hudPackTier)} DAMAGE)")
    } else {
      hudShield = (hudShield + 75).coerceAtMost(loadout.maxShield)
      postCallout("MULTIVERSAL PACK TIER XXV OVERDRIVE! (+75 ARMOR)")
    }
  }

  fun triggerLethal() {
    if (hudRunEnded || lethalCd > 0f) return
    val eqState = progression.equipmentState("lethal")
    val eqTierMult = ProgressionCatalog.tierMultiplier(eqState.tier)
    val eqStarMult = StarRankMeta.forStars(eqState.starLevel).multiplier
    val levelBonus = eqState.level * 2
    val prestigeBonus = eqState.prestige * 5
    val equippedIds = progression.equippedLethalIds.ifEmpty { listOf(equippedLethalItem.id) }
    val baseCd = equippedLethalItem.cooldownSeconds * 60f * loadout.skillDashCdMult
    lethalCd = baseCd
    pulseRingRadius = 0.05f

    val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L
    var totalDmg = 0
    equippedIds.forEachIndexed { idx, itemId ->
      val spec = ProgressionCatalog.equipmentItems.firstOrNull { it.id == itemId } ?: equippedLethalItem
      val spreadAngle = atan2(aimY, aimX) + (idx * 0.25f)
      val blastX = (playerX + cos(spreadAngle) * 0.20f).coerceIn(ARENA_MIN_X, ARENA_MAX_X)
      val blastY = (playerY + sin(spreadAngle) * 0.20f).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y)
      val blastRadiusSq = 0.22f * 0.22f
      val lethalDmg = (((spec.baseDamageOrEffect / 30f).toInt() + levelBonus / 4 + prestigeBonus) * eqTierMult * eqStarMult).roundToInt().coerceAtLeast(2)
      totalDmg += lethalDmg

      arcs.add(ArenaArcLine(playerX, playerY, blastX, blastY, Color(spec.defaultColorHex), 20f))
      for (i in 0 until 10) {
        val a = (i / 10f) * 2f * PI.toFloat()
        sparks.add(HitSpark(blastX + cos(a) * 0.04f, blastY + sin(a) * 0.04f, Color(spec.defaultColorHex), 25f))
      }

      val iter = hostiles.iterator()
      while (iter.hasNext()) {
        val h = iter.next()
        val dx = h.x - blastX
        val dy = h.y - blastY
        if (dx * dx + dy * dy <= blastRadiusSq) {
          h.hp -= lethalDmg
          addSparkIfAllowed(h.x, h.y, Color(spec.defaultColorHex), 18f)
          if (h.hp <= 0) {
            iter.remove()
            onHostileDefeated(h, scoreMult)
          }
        }
      }
    }
    postCallout("${equippedLethalItem.name.uppercase()} x${equippedIds.size} DETONATED! ($totalDmg DMG)")
  }

  fun triggerTactical() {
    if (hudRunEnded || tacticalCd > 0f) return
    val eqState = progression.equipmentState("tactical")
    val eqStarMult = StarRankMeta.forStars(eqState.starLevel).multiplier
    val equippedIds = progression.equippedTacticalIds.ifEmpty { listOf(equippedTacticalItem.id) }
    val baseCd = equippedTacticalItem.cooldownSeconds * 60f * loadout.skillDashCdMult
    tacticalCd = baseCd
    var totalHeal = 0
    equippedIds.forEach { itemId ->
      val spec = ProgressionCatalog.equipmentItems.firstOrNull { it.id == itemId } ?: equippedTacticalItem
      totalHeal += (((spec.baseDamageOrEffect.toInt() + eqState.level * 2).coerceAtLeast(35)) * eqStarMult).roundToInt()
      if (spec.id == "tactical_emp" || spec.id == "tactical_decoy") {
        stasisTicks = maxOf(stasisTicks, 140f)
      }
    }
    hudShield = (hudShield + totalHeal).coerceAtMost(loadout.maxShield)
    overdriveTicks = maxOf(overdriveTicks, 180f)
    pulseRingRadius = 0.04f
    postCallout("${equippedTacticalItem.name.uppercase()} x${equippedIds.size}: +$totalHeal SHIELD")
  }

  fun triggerDash() {
    if (hudRunEnded || dashCd > 0f) return
    val baseDashCd = if (loadout.hasClawEdge) 40f else 50f
    val lowGrav = progression.levelCustom.lowGravityDash || matchRules.lowGravity
    dashCd = (if (lowGrav) baseDashCd * 0.6f else baseDashCd) * loadout.skillDashCdMult
    dashInvulnTicks = if (lowGrav) 32f else 24f
    val dashDist = if (lowGrav) 0.22f else 0.16f
    val oldX = playerX
    val oldY = playerY
    playerX = (playerX + aimX * dashDist).coerceIn(ARENA_MIN_X, ARENA_MAX_X)
    playerY = (playerY + aimY * dashDist).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y)
    arcs.add(ArenaArcLine(oldX, oldY, playerX, playerY, accentColor, 14f))

    if (loadout.hasClawEdge) {
      val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L
      val clawDmg = 2 + loadout.clawTier
      val iter = hostiles.iterator()
      while (iter.hasNext()) {
        val h = iter.next()
        val dx = h.x - playerX
        val dy = h.y - playerY
        if (dx * dx + dy * dy <= 0.16f * 0.16f) {
          h.hp -= clawDmg
          addSparkIfAllowed(h.x, h.y, Color(0xFFFF5252), 18f)
          if (h.hp <= 0) {
            iter.remove()
            onHostileDefeated(h, scoreMult)
          }
        }
      }
    }
  }

  private fun onHostileDefeated(h: ArenaHostile, scoreMult: Long) {
    hudElims++
    if (h.isBoss) hudBossElims++
    if (loadout.ammoModId == "vampiric_bio") {
      hudShield = (hudShield + 2).coerceAtMost(loadout.maxShield)
    }
    if (matchRules.vampiricSiphon) {
      hudShield = (hudShield + 20).coerceAtMost(loadout.maxShield)
    }
    if (h.isBoss || h.isElite) {
      val clanSiphon = (progression.clan.armorSiphonPerk / 4).coerceAtLeast(1)
      hudShield = (hudShield + clanSiphon).coerceAtMost(loadout.maxShield)
    }
    val base = when {
      h.isBoss -> 500L
      h.isElite -> 160L
      else -> 70L
    }
    val effectiveMult = scoreMult * (if (matchRules.doubleBlueEssence) 2L else 1L)
    val hardcoreBonus = if (matchRules.friendlyFire) base / 4L else 0L
    hudScore += (base + hardcoreBonus) * effectiveMult

    if (modeId == "multiplayer") {
      if (hudElims > 0L && hudElims % 7L == 0L) {
        overdriveTicks = 240f
        postCallout("STREAK OVERDRIVE ($hudElims ELIMS)!")
      }
      if (hudElims >= 25L && !hudExtractedSuccess) {
        hudExtractedSuccess = true
        hudRunEnded = true
        return
      }
    }

    // Power-up Drops
    if (h.isBoss || (h.isElite && Random.nextFloat() < 0.65f) || Random.nextFloat() < 0.18f) {
      val kind = ArenaDropKind.entries[Random.nextInt(ArenaDropKind.entries.size)]
      drops.add(ArenaDrop(h.x, h.y, kind))
    }

    // Spawn Multi-Color Essence Orbs, Blast Shards, Perk Drinks & Blast Cores for Vacuum Bot to scoop!
    val orbRoll = Random.nextFloat()
    when {
      h.isBoss -> {
        collectibles.add(ArenaCollectible(h.x, h.y, ArenaCollectibleKind.COMBINED_ORB, amount = 25L))
        val core = BlastCoreColor.entries[Random.nextInt(BlastCoreColor.entries.size)]
        collectibles.add(ArenaCollectible(h.x + 0.04f, h.y, ArenaCollectibleKind.BLAST_CORE, subId = core.id, amount = 1L, color = Color(core.colorHex)))
        collectibles.add(ArenaCollectible(h.x - 0.04f, h.y, ArenaCollectibleKind.BLAST_SHARD, amount = 5L))
      }
      orbRoll < 0.28f -> collectibles.add(ArenaCollectible(h.x, h.y, ArenaCollectibleKind.RED_ORB, amount = 18L))
      orbRoll < 0.56f -> collectibles.add(ArenaCollectible(h.x, h.y, ArenaCollectibleKind.YELLOW_ORB, amount = 18L))
      orbRoll < 0.84f -> collectibles.add(ArenaCollectible(h.x, h.y, ArenaCollectibleKind.GREEN_ORB, amount = 18L))
      else -> collectibles.add(ArenaCollectible(h.x, h.y, ArenaCollectibleKind.BLAST_SHARD, amount = 3L))
    }

    // Random Wonder Weapon Pickups
    val wonderBoost = progression.activePassVersion.wonderDropBonusMultiplier *
      (if (progression.levelCustom.wonderDropBoost) 2.2f else 1.0f) * loadout.wonderDropSkillMult
    val wonderChance = when {
      h.isBoss -> 1.0f
      h.isElite -> (0.38f * wonderBoost).coerceAtMost(0.95f)
      else -> (0.08f * wonderBoost).coerceAtMost(0.45f)
    }
    if (Random.nextFloat() < wonderChance) {
      spawnRandomWonderWeapon(h.x, h.y)
    }
  }

  private fun applyCollectible(c: ArenaCollectible) {
    when (c.kind) {
      ArenaCollectibleKind.BLUE_ORB -> {
        hudScore += c.amount * 5L
      }
      ArenaCollectibleKind.RED_ORB -> redEssenceEarned += c.amount
      ArenaCollectibleKind.YELLOW_ORB -> yellowEssenceEarned += c.amount
      ArenaCollectibleKind.GREEN_ORB -> greenEssenceEarned += c.amount
      ArenaCollectibleKind.COMBINED_ORB -> combinedEssenceEarned += c.amount
      ArenaCollectibleKind.BLAST_SHARD -> blastShardsEarned += c.amount
      ArenaCollectibleKind.PERK_DRINK -> {
        perkDrinksCollected++
        hudShield = (hudShield + 35).coerceAtMost(loadout.maxShield)
        overdriveTicks = maxOf(overdriveTicks, 180f)
        postCallout("PERK DRINK SECURED: ${c.subId.removePrefix("perk_").uppercase()}!")
      }
      ArenaCollectibleKind.BLAST_CORE -> {
        blastCoresEarned[c.subId] = (blastCoresEarned[c.subId] ?: 0L) + c.amount
        postCallout("BLAST CORE COLLECTED: ${c.subId.uppercase()}!")
      }
    }
    // Feed Vacuum Bot auto-upgrade capacitor!
    if (progression.vacuumBot.autoCollectEnabled) {
      vacuumChargeFloat += 20f * progression.vacuumBot.upgradeChargeRate
      if (vacuumChargeFloat >= 100f) {
        vacuumChargeFloat -= 100f
        if (progression.vacuumBot.autoUpgradeEnabled) {
          if (hudPackTier < 25) {
            upgradePackAPunch()
          } else {
            hudShield = (hudShield + 40).coerceAtMost(loadout.maxShield)
            postCallout("VACUUM BOT AUTO-UPGRADED ARMOR (+40)")
          }
        }
      }
      hudVacuumChargePct = vacuumChargeFloat.roundToInt().coerceIn(0, 100)
    }
  }

  fun collectWonderPickup(kind: WonderWeaponKind) {
    val durMult = (if (progression.activePassVersion == BattlePassVersion.CHRONO_MULTIVERSE) 1.30f else 1.0f) * loadout.wonderDurSkillMult
    activeWonderWeapon = kind
    wonderWeaponTicks = kind.durationTicks * durMult
    hudActiveWonder = kind
    hudWonderSecLeft = (wonderWeaponTicks / 60f).roundToInt()
    hudWonderPickedCount++
    pulseRingRadius = 0.05f
    postCallout("EQUIPPED ${kind.displayName.uppercase()} (T${progression.wonderTier(kind.id)})!")
  }

  private fun applyDrop(drop: ArenaDrop) {
    when (drop.kind) {
      ArenaDropKind.DOUBLE_ESSENCE -> {
        doubleEssenceTicks = 600f
        postCallout("2X BLUE ESSENCE ACTIVE (10s)")
      }
      ArenaDropKind.INSTA_KILL -> {
        instaKillTicks = 480f
        postCallout("INSTA-KILL ACTIVE (8s)")
      }
      ArenaDropKind.MAX_ARMOR -> {
        hudShield = loadout.maxShield
        dashCd = 0f
        postCallout("MAX ARMOR RESTORED!")
      }
      ArenaDropKind.NUKE -> {
        pulseRingRadius = 0.05f
        val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L
        val iter = hostiles.iterator()
        while (iter.hasNext()) {
          val h = iter.next()
          if (h.isBoss) {
            h.hp -= 8
            addSparkIfAllowed(h.x, h.y, Color(0xFF00E5FF), 20f)
          } else {
            addSparkIfAllowed(h.x, h.y, Color(0xFF00E5FF), 20f)
            iter.remove()
            hudElims++
          }
        }
        hudScore += 400L * scoreMult
        postCallout("TACTICAL NUKE • +${400L * scoreMult}")
      }
    }
  }

  fun stepFrame(dtSec: Float) {
    if (hudRunEnded) return
    val dt = (dtSec * 60f).coerceIn(0.1f, 2.5f)
    val pace = liveSettings.gameSpeedMultiplier.coerceIn(0.55f, 1.3f)
    val paceDt = dt * pace
    elapsedTicks++

    val moving = (joyDx * joyDx + joyDy * joyDy) > 0.0064f
    frameTick = (frameTick + (if (moving) 1.35f else 0.7f) * dt) % 60f

    if (powerCd > 0f) powerCd = (powerCd - dt).coerceAtLeast(0f)
    if (consumableCd > 0f) consumableCd = (consumableCd - dt).coerceAtLeast(0f)
    if (interactCd > 0f) interactCd = (interactCd - dt).coerceAtLeast(0f)
    if (lethalCd > 0f) {
      lethalCd = (lethalCd - dt).coerceAtLeast(0f)
      val sec = (lethalCd / 60f).toInt() + 1
      if (hudLethalCdSec != (if (lethalCd <= 0f) 0 else sec)) hudLethalCdSec = if (lethalCd <= 0f) 0 else sec
    }
    if (tacticalCd > 0f) {
      tacticalCd = (tacticalCd - dt).coerceAtLeast(0f)
      val sec = (tacticalCd / 60f).toInt() + 1
      if (hudTacticalCdSec != (if (tacticalCd <= 0f) 0 else sec)) hudTacticalCdSec = if (tacticalCd <= 0f) 0 else sec
    }
    if (dashCd > 0f) dashCd = (dashCd - dt).coerceAtLeast(0f)
    if (dashInvulnTicks > 0f) dashInvulnTicks = (dashInvulnTicks - dt).coerceAtLeast(0f)
    if (overdriveTicks > 0f) overdriveTicks = (overdriveTicks - dt).coerceAtLeast(0f)
    if (stasisTicks > 0f) stasisTicks = (stasisTicks - dt).coerceAtLeast(0f)
    if (doubleEssenceTicks > 0f) doubleEssenceTicks = (doubleEssenceTicks - dt).coerceAtLeast(0f)
    if (instaKillTicks > 0f) instaKillTicks = (instaKillTicks - dt).coerceAtLeast(0f)
    if (damageFlashTicks > 0f) damageFlashTicks = (damageFlashTicks - dt).coerceAtLeast(0f)
    if (wonderWeaponTicks > 0f) {
      wonderWeaponTicks = (wonderWeaponTicks - dt).coerceAtLeast(0f)
      if (wonderWeaponTicks <= 0f) {
        activeWonderWeapon = null
        hudActiveWonder = null
        hudWonderSecLeft = 0
      } else {
        val sec = (wonderWeaponTicks / 60f).toInt() + 1
        if (hudWonderSecLeft != sec) hudWonderSecLeft = sec
      }
    }

    if (calloutTicks > 0f) {
      calloutTicks = (calloutTicks - dt).coerceAtLeast(0f)
      if (calloutTicks <= 0f) hudCallout = null
    }
    if (pulseRingRadius > 0f) {
      pulseRingRadius += 0.018f * dt
      if (pulseRingRadius > 0.36f) pulseRingRadius = 0f
    }

    // Passive perk armor regeneration
    if (hudShield < loadout.maxShield && damageFlashTicks <= 0f) {
      val assistMult = if (liveSettings.assistShieldBoost) 1.55f else 1.0f
      regenAccum += loadout.passiveRegenPerSec * assistMult * dtSec
      if (regenAccum >= 1f) {
        val add = regenAccum.toInt()
        regenAccum -= add
        hudShield = (hudShield + add).coerceAtMost(loadout.maxShield)
      }
    }

    // Player movement scaled by sensitivity setting inside the larger 2.2f x 2.2f arena
    if (moving) {
      val len = sqrt(joyDx * joyDx + joyDy * joyDy).coerceAtLeast(0.001f)
      if (!touchAimActive) {
        aimX = joyDx / len
        aimY = joyDy / len
      }
      val spd = 0.0058f * loadout.moveBoost * liveSettings.moveSensitivity.coerceIn(0.6f, 1.6f) * dt
      playerX = (playerX + (joyDx / len) * spd * len.coerceAtMost(1f)).coerceIn(ARENA_MIN_X, ARENA_MAX_X)
      playerY = (playerY + (joyDy / len) * spd * len.coerceAtMost(1f)).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y)
    }

    // Vacuum Bot companion orbit + auto suction of nearby pickups/drops/orbs
    val vacAngle = elapsedTicks * 0.055f
    vacuumBotX = (playerX + cos(vacAngle) * 0.068f).coerceIn(ARENA_MIN_X, ARENA_MAX_X)
    vacuumBotY = (playerY + sin(vacAngle) * 0.068f).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y)

    // Check explorable Buildings + defense turrets
    var insideBuilding: String? = null
    val areaTurretLvl = progression.areaState().defenseTurretTier
    for (bi in 0 until buildings.size) {
      val b = buildings[bi]
      if (playerX in b.x..(b.x + b.width) && playerY in b.y..(b.y + b.height)) {
        insideBuilding = b.name
        if (!b.explored) {
          b.explored = true
          buildingsExploredCount++
          blastShardsEarned += 4L
          combinedEssenceEarned += 10L
          postCallout("EXPLORED ${b.name.uppercase()} (+4 BLAST SHARDS!)")
        }
      }
      // Building defense turret fires when hostiles approach
      b.turretCooldownTicks -= dt
      if (b.turretCooldownTicks <= 0f && hostiles.isNotEmpty()) {
        b.turretCooldownTicks = (90f - areaTurretLvl * 5f).coerceAtLeast(35f)
        val cx = b.x + b.width * 0.5f
        val cy = b.y + b.height * 0.5f
        val target = hostiles.firstOrNull { (it.x - cx) * (it.x - cx) + (it.y - cy) * (it.y - cy) < 0.45f * 0.45f }
        if (target != null && bolts.size < 78) {
          val dx = target.x - cx
          val dy = target.y - cy
          val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
          bolts.add(ArenaBolt(cx, cy, (dx / d) * 0.022f, (dy / d) * 0.022f, 45f, true, false, 0, 2 + areaTurretLvl, b.accentColor))
        }
      }
    }
    if (hudActiveBuildingName != insideBuilding) hudActiveBuildingName = insideBuilding

    // Electric & Fire Traps automatic proximity trigger & shock/burn damage
    val scoreMultForTraps = if (doubleEssenceTicks > 0f) 2L else 1L
    for (ti in 0 until traps.size) {
      val trap = traps[ti]
      if (trap.cooldownTicks > 0f) trap.cooldownTicks = (trap.cooldownTicks - dt).coerceAtLeast(0f)
      if (trap.activeTicks > 0f) trap.activeTicks = (trap.activeTicks - dt).coerceAtLeast(0f)

      val rSq = trap.radius * trap.radius
      val hIterT = hostiles.iterator()
      var triggeredNow = false
      while (hIterT.hasNext()) {
        val h = hIterT.next()
        val dx = h.x - trap.x
        val dy = h.y - trap.y
        if (dx * dx + dy * dy <= rSq && (trap.activeTicks > 0f || trap.cooldownTicks <= 0f)) {
          triggeredNow = true
          if (elapsedTicks % 12L == 0L) {
            h.hp -= (2 + loadout.trapDamageBonus / 2)
            arcs.add(ArenaArcLine(trap.x, trap.y, h.x, h.y, trap.kind.color, 12f))
            if (h.hp <= 0) {
              hIterT.remove()
              trapKillsCount++
              onHostileDefeated(h, scoreMultForTraps)
            }
          }
        }
      }
      if (triggeredNow && trap.cooldownTicks <= 0f) {
        trap.activeTicks = 150f
        trap.cooldownTicks = 240f
      }
    }

    // Auto-fire / Touch-drag fire
    if ((hudAutoFire && hostiles.isNotEmpty()) || touchAimActive) {
      fireAutoTicker -= dt
      if (fireAutoTicker <= 0f) {
        fireVolley()
        fireAutoTicker = if (overdriveTicks > 0f) loadout.fireCadenceTicks * 0.65f else loadout.fireCadenceTicks
      }
    }

    // AI Squad & Lobby Bots Movement and Tactical Fire
    if (arenaBots.isNotEmpty()) {
      val botDiff = progression.lobbyConfig.botDifficulty
      val hardcoreBotScale = if (matchRules.friendlyFire) 1.35f else 1.0f
      val botDmgMult = botDiff.damageScale * (1f + (progression.botDamageBoostPct + progression.clan.botDamageBoost) / 100f) * hardcoreBotScale
      for (bi in 0 until arenaBots.size) {
        val bot = arenaBots[bi]
        bot.roamAngle = (bot.roamAngle + 0.015f * dt) % (2f * PI.toFloat())
        val targetX: Float
        val targetY: Float
        var nearestHostile: ArenaHostile? = null
        var bestDistSq = Float.MAX_VALUE
        for (hi in 0 until hostiles.size) {
          val h = hostiles[hi]
          val dx = h.x - bot.x
          val dy = h.y - bot.y
          val dSq = dx * dx + dy * dy
          if (dSq < bestDistSq) {
            bestDistSq = dSq
            nearestHostile = h
          }
        }
        if (bot.isAllied) {
          targetX = (playerX + cos(bot.roamAngle + bi) * 0.16f).coerceIn(ARENA_MIN_X, ARENA_MAX_X)
          targetY = (playerY + sin(bot.roamAngle + bi) * 0.16f).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y)
        } else {
          targetX = (playerX + cos(bot.roamAngle + bi) * 0.38f).coerceIn(ARENA_MIN_X, ARENA_MAX_X)
          targetY = (playerY + sin(bot.roamAngle + bi) * 0.38f).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y)
        }
        val moveDx = targetX - bot.x
        val moveDy = targetY - bot.y
        bot.x = (bot.x + moveDx * 0.03f * dt).coerceIn(ARENA_MIN_X, ARENA_MAX_X)
        bot.y = (bot.y + moveDy * 0.03f * dt).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y)

        bot.fireCdTicks -= dt * botDiff.fireRateScale
        if (bot.fireCdTicks <= 0f && nearestHostile != null && bolts.size < 75) {
          bot.fireCdTicks = 34f
          val aimDx = nearestHostile.x - bot.x
          val aimDy = nearestHostile.y - bot.y
          val dist = sqrt(aimDx * aimDx + aimDy * aimDy).coerceAtLeast(0.001f)
          bolts.add(
            ArenaBolt(
              x = bot.x,
              y = bot.y,
              vx = (aimDx / dist) * 0.022f,
              vy = (aimDy / dist) * 0.022f,
              life = 52f,
              isSupercharged = bot.isAllied,
              pierceLeft = 0,
              damage = (2f * botDmgMult).roundToInt().coerceAtLeast(1),
              color = bot.color,
            )
          )
        }
      }
    }

    // Battle Royale shrinking zone
    if (modeId == "br") {
      brRingRadius = (brRingRadius - 0.00010f * paceDt).coerceAtLeast(0.24f)
      val dx = playerX - ARENA_CENTER
      val dy = playerY - ARENA_CENTER
      if (dx * dx + dy * dy > brRingRadius * brRingRadius) {
        brDamageAccum += paceDt
        if (brDamageAccum >= 22f) {
          brDamageAccum = 0f
          hudShield = (hudShield - 3).coerceAtLeast(0)
          damageFlashTicks = if (liveSettings.reduceFlashes) 0f else 10f
          if (hudShield <= 0) hudRunEnded = true
        }
      }
    }

    // Sparks & Arcs update
    val sIter = sparks.iterator()
    while (sIter.hasNext()) {
      val sp = sIter.next()
      sp.life -= dt
      if (sp.life <= 0f) sIter.remove()
    }
    val aIter = arcs.iterator()
    while (aIter.hasNext()) {
      val a = aIter.next()
      a.life -= dt
      if (a.life <= 0f) aIter.remove()
    }

    val vacRadSq = loadout.vacuumRadius * loadout.vacuumRadius

    // Collectibles (Blast Shards, Perk Drinks, Essence Orbs, Blast Cores) + Vacuum Suction
    val cIter = collectibles.iterator()
    while (cIter.hasNext()) {
      val c = cIter.next()
      c.life -= dt
      val dx = playerX - c.x
      val dy = playerY - c.y
      val dSq = dx * dx + dy * dy
      if (dSq <= 0.055f * 0.055f) {
        cIter.remove()
        applyCollectible(c)
      } else {
        if (progression.vacuumBot.autoCollectEnabled && dSq <= vacRadSq) {
          val d = sqrt(dSq).coerceAtLeast(0.001f)
          c.x += (dx / d) * 0.012f * dt
          c.y += (dy / d) * 0.012f * dt
        }
        if (c.life <= 0f) cIter.remove()
      }
    }

    // Field Drops update & Vacuum Bot Pull + pickup check
    val dIter = drops.iterator()
    while (dIter.hasNext()) {
      val drop = dIter.next()
      drop.life -= dt
      val dx = playerX - drop.x
      val dy = playerY - drop.y
      val dSq = dx * dx + dy * dy
      if (dSq <= 0.055f * 0.055f) {
        dIter.remove()
        applyDrop(drop)
      } else {
        if (progression.vacuumBot.autoCollectEnabled && dSq <= vacRadSq) {
          val d = sqrt(dSq).coerceAtLeast(0.001f)
          drop.x += (dx / d) * 0.011f * dt
          drop.y += (dy / d) * 0.011f * dt
        }
        if (drop.life <= 0f) dIter.remove()
      }
    }

    // Wonder Weapon Pickups update & Vacuum Pull + collection
    val wIter = wonderPickups.iterator()
    while (wIter.hasNext()) {
      val wp = wIter.next()
      wp.life -= dt
      val dx = playerX - wp.x
      val dy = playerY - wp.y
      val dSq = dx * dx + dy * dy
      if (dSq <= 0.06f * 0.06f) {
        wIter.remove()
        collectWonderPickup(wp.kind)
      } else {
        if (progression.vacuumBot.autoCollectEnabled && dSq <= vacRadSq) {
          val d = sqrt(dSq).coerceAtLeast(0.001f)
          wp.x += (dx / d) * 0.010f * dt
          wp.y += (dy / d) * 0.010f * dt
        }
        if (wp.life <= 0f) wIter.remove()
      }
    }

    // Bolts update + enemy shot hits player
    val bIter = bolts.iterator()
    while (bIter.hasNext()) {
      val b = bIter.next()
      val stepB = if (b.isEnemyShot) paceDt else dt
      b.x += b.vx * stepB
      b.y += b.vy * stepB
      b.life -= stepB
      if (b.isEnemyShot && dashInvulnTicks <= 0f && modeId != "sandbox") {
        val dx = b.x - playerX
        val dy = b.y - playerY
        if (dx * dx + dy * dy <= 0.036f * 0.036f) {
          b.life = 0f
          val rawShot = if (liveSettings.assistShieldBoost) 5 else 8
          val shotDmg = if (matchRules.friendlyFire) (rawShot * 1.35f).roundToInt() else rawShot
          hudShield = (hudShield - shotDmg).coerceAtLeast(0)
          damageFlashTicks = if (liveSettings.reduceFlashes) 0f else 12f
          if (hudShield <= 0) hudRunEnded = true
        }
      }
      if (b.life <= 0f || b.x < 0f || b.x > ARENA_WORLD_SIZE || b.y < 0f || b.y > ARENA_WORLD_SIZE) {
        bIter.remove()
      }
    }

    // DMZ Intel & Active Exfil Chopper Timer
    if (modeId == "dmz") {
      for (i in 0 until intels.size) {
        val intel = intels[i]
        if (!intel.collected) {
          val dx = intel.x - playerX
          val dy = intel.y - playerY
          if (dx * dx + dy * dy < 0.065f * 0.065f) {
            intel.collected = true
            hudIntelCollected++
            hudScore += 250L
            postCallout(
              if (hudIntelCollected >= 3) "ALL INTEL SECURED! REACH EXFIL HELIPAD!"
              else "INTEL SECURED ($hudIntelCollected / 3)"
            )
          }
        }
      }
      if (hudIntelCollected >= 3) {
        val dx = playerX - ARENA_CENTER
        val dy = playerY - 0.30f
        if (dx * dx + dy * dy < 0.11f * 0.11f) {
          exfilHoldTicks += dt
          hudExfilRatio = (exfilHoldTicks / 150f).coerceIn(0f, 1f)
          if (exfilHoldTicks >= 150f) {
            hudExtractedSuccess = true
            hudRunEnded = true
          }
        } else {
          exfilHoldTicks = (exfilHoldTicks - dt * 0.5f).coerceAtLeast(0f)
          hudExfilRatio = (exfilHoldTicks / 150f).coerceIn(0f, 1f)
        }
      }
    }

    // Hostiles movement, ranged fire, and player-bolt collisions
    val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L
    var activeBoss: ArenaHostile? = null
    val hIter = hostiles.iterator()
    while (hIter.hasNext()) {
      val h = hIter.next()
      if (stasisTicks <= 0f) {
        if (h.cryoSlowTicks > 0f) h.cryoSlowTicks = (h.cryoSlowTicks - dt).coerceAtLeast(0f)
        val slowMult = if (h.cryoSlowTicks > 0f) 0.42f else 1.0f
        val dx = playerX - h.x
        val dy = playerY - h.y
        val dSq = dx * dx + dy * dy
        val d = sqrt(dSq).coerceAtLeast(0.001f)
        h.x = (h.x + (dx / d) * h.speed * slowMult * paceDt).coerceIn(ARENA_MIN_X, ARENA_MAX_X)
        h.y = (h.y + (dy / d) * h.speed * slowMult * paceDt).coerceIn(ARENA_MIN_Y, ARENA_MAX_Y)

        if ((h.archetype == "trooper" || h.archetype == "mech" || h.isBoss) && d > 0.15f && d < 0.95f) {
          h.rangedCooldownTicks -= paceDt
          if (h.rangedCooldownTicks <= 0f) {
            h.rangedCooldownTicks = if (h.isBoss) 118f else 185f
            bolts.add(
              ArenaBolt(
                x = h.x,
                y = h.y,
                vx = (dx / d) * 0.0072f,
                vy = (dy / d) * 0.0072f,
                life = 115f,
                isEnemyShot = true,
                color = Color(0xFFFF5252),
              )
            )
          }
        }

        val contactThreshold = if (liveSettings.assistShieldBoost) 42f else 28f
        if (dSq < 0.045f * 0.045f && dashInvulnTicks <= 0f && modeId != "sandbox") {
          meleeDamageAccum += paceDt
          if (meleeDamageAccum >= contactThreshold) {
            meleeDamageAccum = 0f
            val baseDmg = if (h.isBoss) 14 else 9
            val dmg = if (liveSettings.assistShieldBoost) (baseDmg * 2 / 3).coerceAtLeast(4) else baseDmg
            hudShield = (hudShield - dmg).coerceAtLeast(0)
            damageFlashTicks = if (liveSettings.reduceFlashes) 0f else 12f
            if (hudShield <= 0) {
              hudRunEnded = true
            }
          }
        }
      }

      val hitR = if (h.isBoss) 0.068f else 0.050f
      val hitRSq = hitR * hitR
      var died = false
      for (i in 0 until bolts.size) {
        val b = bolts[i]
        if (b.isEnemyShot || b.life <= 0f) continue
        val bdx = b.x - h.x
        val bdy = b.y - h.y
        if (bdx * bdx + bdy * bdy < hitRSq) {
          val dmg = if (instaKillTicks > 0f && !h.isBoss) 999 else b.damage
          h.hp -= dmg
          addSparkIfAllowed(h.x, h.y, b.color, 14f)

          val siphon = loadout.activePassVersion.glorySiphonPerBossHit
          if (siphon > 0 && (h.isBoss || h.isElite)) {
            hudShield = (hudShield + siphon).coerceAtMost(loadout.maxShield)
          }

          if (loadout.ammoModId == "cryo_stasis" || activeWonderWeapon == WonderWeaponKind.CHRONO_ZAT) {
            h.cryoSlowTicks = if (activeWonderWeapon == WonderWeaponKind.CHRONO_ZAT) 150f else 90f
          }
          if (activeWonderWeapon == WonderWeaponKind.BLACKLIGHT_WHIP) {
            hudShield = (hudShield + 2).coerceAtMost(loadout.maxShield)
          }
          if (activeWonderWeapon == WonderWeaponKind.LIBERTY_WUNDERWAFFE ||
            (loadout.ammoModId == "arc_chain" && playerShotCounter % 3 == 0)
          ) {
            var chains = 0
            val maxChains = if (activeWonderWeapon == WonderWeaponKind.LIBERTY_WUNDERWAFFE) 4 else 2
            for (j in 0 until hostiles.size) {
              if (chains >= maxChains) break
              val other = hostiles[j]
              if (other === h || other.hp <= 0) continue
              val ox = other.x - h.x
              val oy = other.y - h.y
              if (ox * ox + oy * oy <= 0.24f * 0.24f) {
                other.hp -= 2
                arcs.add(ArenaArcLine(h.x, h.y, other.x, other.y, Color(0xFF00E5FF), 14f))
                chains++
              }
            }
          }
          if (activeWonderWeapon == WonderWeaponKind.CRUCIBLE_BFG || activeWonderWeapon == WonderWeaponKind.RAY_NOVA_MK3) {
            val splashRadiusSq = if (activeWonderWeapon == WonderWeaponKind.CRUCIBLE_BFG) 0.22f * 0.22f else 0.14f * 0.14f
            val splashDmg = if (activeWonderWeapon == WonderWeaponKind.CRUCIBLE_BFG) 3 else 2
            for (j in 0 until hostiles.size) {
              val other = hostiles[j]
              if (other === h || other.hp <= 0) continue
              val ox = other.x - h.x
              val oy = other.y - h.y
              if (ox * ox + oy * oy <= splashRadiusSq) {
                other.hp -= splashDmg
                addSparkIfAllowed(other.x, other.y, Color(activeWonderWeapon!!.accentHex), 12f)
              }
            }
          }

          if (b.pierceLeft > 0) {
            b.pierceLeft--
          } else {
            b.life = 0f
          }
          if (h.hp <= 0) {
            died = true
            break
          }
        }
      }
      if (died) {
        hIter.remove()
        onHostileDefeated(h, scoreMult)
      } else if (h.isBoss) {
        activeBoss = h
      }
    }

    // Update discrete HUD state ONLY when changed
    if (activeBoss != null) {
      if (hudBossName != activeBoss.bossName) hudBossName = activeBoss.bossName
      hudBossHpRatio = (activeBoss.hp.toFloat() / activeBoss.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
    } else if (hudBossName != null) {
      hudBossName = null
      hudBossHpRatio = 0f
    }

    val pSec = if (powerCd <= 0f) 0 else (powerCd / 60f).toInt() + 1
    if (hudPowerCdSec != pSec) hudPowerCdSec = pSec
    val cSec = if (consumableCd <= 0f) 0 else (consumableCd / 60f).toInt() + 1
    if (hudConsumableCdSec != cSec) hudConsumableCdSec = cSec
    val iSec = if (interactCd <= 0f) 0 else (interactCd / 60f).toInt() + 1
    if (hudInteractCdSec != iSec) hudInteractCdSec = iSec
    val dReady = dashCd <= 0f
    if (hudDashReady != dReady) hudDashReady = dReady
    val ov = overdriveTicks > 0f
    if (hudOverdriveActive != ov) hudOverdriveActive = ov

    val dropTag = when {
      instaKillTicks > 0f && doubleEssenceTicks > 0f -> "2X BLUE ESSENCE • INSTA-KILL"
      instaKillTicks > 0f -> "INSTA-KILL (${(instaKillTicks / 60f).toInt() + 1}s)"
      doubleEssenceTicks > 0f -> "2X BLUE ESSENCE (${(doubleEssenceTicks / 60f).toInt() + 1}s)"
      else -> null
    }
    if (hudActiveDropText != dropTag) hudActiveDropText = dropTag

    // Wave transition
    if (hostiles.isEmpty() && !hudRunEnded) {
      if ((modeId == "br" && hudWave >= 4L) || (matchRules.customWaveGoal > 0 && hudWave >= matchRules.customWaveGoal.toLong())) {
        hudExtractedSuccess = true
        hudRunEnded = true
        if (matchRules.customWaveGoal > 0) {
          postCallout("MATCH GOAL WAVE ${matchRules.customWaveGoal} CLEARED! VICTORY EXFIL!")
        }
      } else {
        hudWave++
        val waveEffMult = scoreMult * (if (matchRules.doubleBlueEssence) 2L else 1L)
        hudScore += hudWave * 100L * waveEffMult
        if (world.id == "chrono_vault") {
          hudChevronsLocked = ((hudWave.toInt() - 1) % 7) + 1
        }
        postCallout("ROUND ${formatRoundRoman(hudWave)} INCOMING")
        spawnWave(hudWave)
      }
    }

    renderNonce++
  }
}

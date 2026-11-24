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

const val ARENA_WORLD_SIZE = 10.0f
const val ARENA_MIN_X = -2.0f
const val ARENA_MAX_X = 12.0f
const val ARENA_MIN_Y = -2.0f
const val ARENA_MAX_Y = 12.0f
const val ARENA_CENTER = 5.0f

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

enum class ArenaHazardKind(val label: String, val color: Color, val summary: String) {
  ICE("CRYO ICE ZONE", Color(0xFF80D8FF), "Slows movement • Boosts Rare/Epic Crates"),
  FIRE("INFERNO ZONE", Color(0xFFFF5722), "Thermal DoT • Boosts Legendary Crates"),
  TOXIC("BIO-TOXIC MIASMA", Color(0xFF76FF03), "Corrosive DoT • Boosts Character Shards"),
  ELECTRIC("ARC STORM FIELD", Color(0xFF40C4FF), "Static shocks • Boosts Exotic Crates"),
  WINDY("GALE RIFT SQUALL", Color(0xFFB2DFDB), "Wind drift push • Boosts Mythic Crates"),
}

enum class ArenaStationKind(val label: String, val color: Color) {
  PACK_A_PUNCH("PACK-A-PUNCH", Color(0xFFE040FB)),
  PERK_MACHINE("PERK MACHINE", Color(0xFF00E676)),
  AMMO_CRATE("AMMO CRATE", Color(0xFFFFD54F)),
}

data class ArenaHazardZone(
  val id: String,
  val x: Float,
  val y: Float,
  val radius: Float,
  val kind: ArenaHazardKind,
  val windDx: Float = 0f,
  val windDy: Float = 0f,
)

data class ArenaRestCamp(
  val id: String,
  val name: String,
  val x: Float,
  val y: Float,
  val radius: Float = 0.22f,
  var cooldownTicks: Float = 0f,
)

data class ArenaWorldStation(
  val id: String,
  val x: Float,
  val y: Float,
  val kind: ArenaStationKind,
  val subLabel: String,
  var cooldownTicks: Float = 0f,
)

data class ArenaLootCrate(
  val id: String,
  val x: Float,
  val y: Float,
  val rarity: com.example.data.model.CharacterRarity,
  val fromHazard: Boolean = false,
  var opened: Boolean = false,
)

data class ArenaCatEasterEgg(
  val id: String,
  val x: Float,
  val y: Float,
  val name: String,
  var found: Boolean = false,
)

enum class ArenaCollectibleKind(val label: String, val color: Color) {
  BLUE_ORB("Blue Essence", Color(0xFF00E5FF)),
  RED_ORB("Red Essence", Color(0xFFFF3D71)),
  YELLOW_ORB("Yellow Essence", Color(0xFFFFD54F)),
  GREEN_ORB("Green Essence", Color(0xFF00E676)),
  COMBINED_ORB("Combined Quad-Essence", Color(0xFFE040FB)),
  BLAST_SHARD("Blast Shard", Color(0xFF40C4FF)),
  PERK_DRINK("Perk Can/Bottle", Color(0xFFFFAB40)),
  BLAST_CORE("Blast Core", Color(0xFFE040FB)),
  CAT_EASTER_EGG("Golden Cat Easter Egg", Color(0xFFFFD54F)),
  CHARACTER_SHARD("Character Collection Shard", Color(0xFF00E5FF)),
}

enum class ArenaDamageType(
  val id: String,
  val label: String,
  val shortTag: String,
  val color: Color,
) {
  KINETIC("kinetic", "Kinetic", "KIN", Color(0xFFFFD54F)),
  ENERGY("energy", "Energy", "ENG", Color(0xFF00E5FF)),
  THERMAL("thermal", "Thermal", "FIR", Color(0xFFFF5722)),
  CRYO("cryo", "Cryo", "CRY", Color(0xFF80D8FF)),
  BIO("bio", "Bio-Corrosive", "BIO", Color(0xFF76FF03)),
  SAND("sand", "Sand / Earth", "SND", Color(0xFFD7CCC8)),
  VOID("void", "Void / Cosmic", "VOD", Color(0xFFE040FB)),
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
  var hitStaggerTicks: Float = 0f,
  var knockVelX: Float = 0f,
  var knockVelY: Float = 0f,
  var armor: Int = 0,
  val maxArmor: Int = 0,
  val immuneTypes: Set<ArenaDamageType> = emptySet(),
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
  val damageType: ArenaDamageType = ArenaDamageType.KINETIC,
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

data class ArenaPlaceDestination(
  val id: String,
  val label: String,
  val kind: String,
  val x: Float,
  val y: Float,
  val accent: Color,
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
  val powerLevel: Int = 300,
  val lightLevel: Int = 540,
  val powerLightLabel: String = "RISEN LIGHT",
  val difficultyTier: com.example.data.model.WorldDifficultyTier = com.example.data.model.WorldDifficultyTier.NORMAL,
  val characterRarity: com.example.data.model.CharacterRarity = com.example.data.model.CharacterRarity.COMMON,
  val levelMilestoneMult: Float = 1.0f,
  val playerBonusHealthScale: Float = 1.0f,
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
      val powerLight = progression.computePowerAndLightLevel(blueEssence ?: 0L)
      val levelMilestoneMult = ProgressionCatalog.levelStatMultiplier(playerLvl)
      val difficultyTier = progression.levelCustom.worldDifficultyTier

      // Multi-Color Essence & Blast Shard Dynamic Resonance
      val resonance = if (blueEssence != null) {
        progression.essenceResonance(blueEssence)
      } else {
        EssenceResonanceBonuses.compute(0L, 0L, 0L, 0L, 0L, 0L)
      }

      // Active Character Outfit + Rarity + its own Skills / Gear / Equipment + Star Level
      val outfitSpec = progression.activeOutfitSpec()
      val outfitSt = progression.outfitState(outfitSpec.id)
      val outfitRarity = progression.outfitRarity(outfitSpec.id)
      val outfitStarAbove = (outfitSt.starLevel - 1).coerceAtLeast(0)
      val originPowerMult = (if (outfitSpec.isOriginalCharacter) 1.25f else 1.0f) * outfitRarity.statMultiplier
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

      val barrelBonusDmg = when (wc.barrelId) {
        "heavy_bore" -> 2
        "gta4_sawed_heavy", "prototype_blade_barrel", "superman_solar_bore" -> 3
        "cod_taskforce_barrel", "abyss_nether_barrel" -> 2
        else -> 0
      }
      val muzzleBonusDmg = when (wc.muzzleId) {
        "heavy_compensator", "monolithic_suppressor", "infamous_shock_choke", "prototype_tendril_muzzle" -> 1
        "cod_colossus_suppressor" -> 2
        else -> 0
      }
      val opticBonusDmg = when (wc.opticId) {
        "arkham_detective_optic" -> 2
        "cod_thermal_hybrid" -> 1
        else -> 0
      }
      val underbarrelBonusDmg = when (wc.underbarrelId) {
        "arkham_grapnel_launcher" -> 2
        "superman_krypton_grip" -> 1
        else -> 0
      }
      val laserBonusDmg = when (wc.laserId) {
        "arkham_sonar_laser", "superman_heat_beam_guide" -> 2
        else -> 0
      }
      val stockBonusDmg = if (wc.stockId == "arkham_glide_stock") 1 else 0
      val rearGripBonusDmg = if (wc.rearGripId == "superman_steel_grip") 1 else 0

      val attachmentTotalBonusDmg = barrelBonusDmg + muzzleBonusDmg + opticBonusDmg + underbarrelBonusDmg + laserBonusDmg + stockBonusDmg + rearGripBonusDmg

      val attachmentExtraSpread = (if (wc.barrelId == "spread_choke") 1 else 0) +
        (if (wc.barrelId == "gta4_sawed_heavy") 2 else 0) +
        (if (wc.underbarrelId == "cod_masterkey_shotgun") 2 else 0) +
        (if (wc.magazineId == "extended_mag_2" || wc.magazineId == "cod_60_drum") 1 else 0) +
        (if (wc.magazineId == "gta_drum_extended") 2 else 0)

      val attachmentExtraPierce = (if (wc.barrelId == "prototype_blade_barrel") 2 else 0) +
        (if (wc.barrelId == "arkham_disruptor_barrel" || wc.opticId == "dungeon_abyss_lens" || wc.underbarrelId == "prototype_whip_spool" || wc.rearGripId == "arkham_batarang_grip") 1 else 0)

      val attachmentSpeedMult = 1.0f +
        (if (wc.barrelId == "overclocked_rifling") 0.22f else 0f) +
        (if (wc.barrelId == "cod_taskforce_barrel") 0.45f else 0f) +
        (if (wc.barrelId == "superman_solar_bore") 0.30f else 0f) +
        (if (wc.opticId == "superman_telescopic") 0.40f else 0f) +
        (if (wc.laserId == "cod_tac_laser") 0.15f else 0f) +
        (if (wc.muzzleId == "cod_colossus_suppressor") 0.10f else 0f)

      val attachmentCadenceReduction = (if (wc.magazineId == "superman_crystal_cell") 1.2f else 0f) +
        (if (wc.magazineId == "cod_60_drum" || wc.stockId == "cod_no_stock") 0.6f else 0f) +
        (if (wc.magazineId == "gta_drum_extended") 0.8f else 0f) +
        (if (wc.underbarrelId == "superman_krypton_grip") 0.5f else 0f)

      val attachmentMoveBonus = (if (wc.stockId == "cod_no_stock") 0.08f else 0f) +
        (if (wc.stockId == "superman_redcape_stock") 0.06f else 0f)

      val attachmentShieldBonus = (if (wc.stockId == "prototype_armored_stock") 30 else 0) +
        (if (wc.magazineId == "prototype_biomass_sack") 20 else 0)

      val attachmentCritBonus = (if (wc.opticId == "arkham_detective_optic") 0.35f else 0f) +
        (if (wc.opticId == "cod_thermal_hybrid") 0.25f else 0f) +
        (if (wc.underbarrelId == "abyss_skull_lantern") 0.10f else 0f) +
        (if (wc.laserId == "cod_tac_laser") 0.10f else 0f)

      val wonderCustomDurBonus = if (progression.wonderCustom.amplifierId == "amp_chronos_overdrive") 0.60f else 0f

      val levelShieldBonus = ((playerLvl - 1) / 3).coerceAtMost(240) + playerPrestige * 25
      val rawShield = 100 + juggBonus + levelShieldBonus + pass.bonusShield + chestShield + skillShield +
        outfitShieldBonus + charTierShield + charStarShield + storyAreaShield + attachmentShieldBonus
      val maxShield = (rawShield * originPowerMult * resonance.shieldMult * areaStarMult * levelMilestoneMult).roundToInt().coerceAtLeast(100)

      val speedPerkCount = perks.sumOf { p ->
        if (p.id == "perk_overdrive" || p.name.contains("Speed", ignoreCase = true) || p.name.contains("Stamina", ignoreCase = true)) {
          p.tier.toInt()
        } else 0
      }
      val matchRules = progression.effectiveMatchRules()
      val bootsMoveBonus = (((boots?.level ?: 1) - 1) * 0.0035f) + ((boots?.tier ?: 1) - 1) * 0.02f + (bootsStarAbove * 0.025f)
      val stockMoveBonus = (if (wc.stockId == "lightweight_skeleton") 0.08f else 0f) + attachmentMoveBonus
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

      val cm = progression.collectionCustomMode
      val customModeCdMult = cm.effectiveCooldownMultFor(progression.activeAreaId, progression.customGameMaps)
      val customModeDmgMult = cm.effectiveDamageMultFor(progression.activeAreaId, progression.customGameMaps)
      val clawItem = upgrades.firstOrNull { it.id == "upgrade_claws" || it.name.contains("Claw", ignoreCase = true) || it.name.contains("Melee", ignoreCase = true) }
      val primaryPower = powers.firstOrNull()
      val skillCdMult = ((1f - skCd * 0.06f) * (1f - resonance.cooldownReductionRatio * 0.45f)).coerceIn(0.22f, 1.0f)
      val powerCdSec = ((primaryPower?.cooldownSec?.toFloat() ?: 6f) * pass.cooldownMultiplier * skillCdMult * customModeCdMult).coerceIn(0.9f, 32f)
      val customBonusDmg = ((primaryPower?.powerValue ?: 180L) / 70L).toInt().coerceIn(2, 24)
      val powerTierBonus = ((primaryPower?.tier ?: 1L) - 1L).toInt()
      val powerStarBonus = primaryPower?.let { (progression.armoryItemStar(it.id) - 1) * 2 } ?: 0
      val powerColor = primaryPower?.accentColorHex?.let { parseHexSafe(it, fallbackAccent) } ?: fallbackAccent

      val primaryConsumable = consumables.firstOrNull()
      val consumableStarBonus = primaryConsumable?.let { (progression.armoryItemStar(it.id) - 1) * 12 } ?: 0
      val cHeal = (45 + ((primaryConsumable?.tier ?: 1L) * 18L) + ((primaryConsumable?.powerValue ?: 100L) / 15L) + consumableStarBonus).toInt().coerceAtMost(550)
      val cCdSec = ((primaryConsumable?.cooldownSec?.toFloat() ?: 12f) * pass.cooldownMultiplier * skillCdMult * customModeCdMult).coerceIn(2.0f, 35f)

      val levelDmgBonus = ((wpnLevel - 1) / 3) + (wpnPrestige * 2)
      val skillDmgBonus = skDmg1 * 2 + skDmg2 * 3 + outfitDmgBonus + charTierDmg + (doubleTapRank * 2) + (redCoreAbove * 2)
      val rawTotalWpnDmg = ((weapon.baseDamage - 1).coerceAtLeast(0) + attachmentTotalBonusDmg +
        levelDmgBonus + skillDmgBonus) * originPowerMult * wpnTierMult * wpnStarMult * charStarMult * resonance.damageMult * era.damageMod * areaCycleMult * areaStarMult * storyAreaDmgMult * levelMilestoneMult * customModeDmgMult

      val glovesRateBonus = ((gloves?.level ?: 1) - 1) * 0.04f + ((gloves?.tier ?: 1) - 1) * 0.25f + (glovesStarAbove * 0.25f)
      val levelCadenceReduction = (((wpnLevel - 1) * 0.08f) + glovesRateBonus).coerceAtMost(4.5f)
      val mutatorRateMult = (if (matchRules.doubleSpeed) 1.75f else 1.0f) * (if (matchRules.infiniteAmmo) 1.25f else 1.0f)
      val totalRateMult = (pass.fireRateMultiplier + outfitRateBonus + (speedColaRank * 0.12f) + (yellowCoreAbove * 0.06f)) * resonance.fireRateMult * mutatorRateMult
      val rawCadence = (weapon.baseCadenceTicks - speedPerkCount * 0.9f - levelCadenceReduction - attachmentCadenceReduction) / totalRateMult.coerceAtLeast(1f)
      val fireCadence = rawCadence.coerceIn(1.8f, 15.0f)

      val wonderDropSkillMult = (1f + skWonder * 0.30f + ((areaSt.lootRichnessTier - 1) * 0.15f) + resonance.lootLuckBonus) *
        progression.clan.wonderPerkMultiplier * (if (matchRules.wonderSpawnBoost) 2.2f else 1.0f)
      val wonderDurSkillMult = 1f + skWonder * 0.15f + (((backRig?.level ?: 1) - 1) * 0.01f) + ((backRig?.tier ?: 1) - 1) * 0.08f + (backRigStarAbove * 0.10f) + (purpleCoreAbove * 0.18f) + wonderCustomDurBonus
      val critDmgGearBonus = 1f + (((helmet?.level ?: 1) - 1) * 0.02f) + ((helmet?.tier ?: 1) - 1) * 0.12f + (helmetStarAbove * 0.14f) + (redCoreAbove * 0.12f) + attachmentCritBonus
      val vacRadius = (progression.vacuumBot.vacuumRadius + greenCoreAbove * 0.018f).coerceAtMost(0.95f)
      val trapBonus = (electricCherryRank * 3) + (phdRank * 3) + (blueCoreAbove * 2) + ((areaSt.trapDensityTier - 1) * 2)

      val weaponBulletColor = if (wc.equippedWeaponId == weapon.id && wc.tracerColorHex != 0L) {
        Color(wc.tracerColorHex)
      } else {
        Color(weapon.defaultTracerHex)
      }

      val weaponBonusDmg = rawTotalWpnDmg.roundToInt().coerceAtLeast(1)
      val bonusDamageFactor = ((weaponBonusDmg - 1).coerceAtLeast(0) * 0.16f)
      val bonusPackFactor = ((initialPackTier - 1).coerceAtLeast(0) * 0.25f)
      val bonusMultiShotFactor = ((multiShot - 1).coerceAtLeast(0) * 0.16f)
      val bonusShieldFactor = (((maxShield - 100).coerceAtLeast(0)) / 90f) * 0.14f
      val bonusResonanceFactor = ((resonance.damageMult - 1f).coerceAtLeast(0f) + (resonance.shieldMult - 1f).coerceAtLeast(0f) + (resonance.fireRateMult - 1f).coerceAtLeast(0f)) * 0.40f
      val bonusPowerLightFactor = ((powerLight.powerLevel - 300).coerceAtLeast(0) / 140f) * 0.18f
      val bonusRarityMilestoneFactor = ((outfitRarity.statMultiplier - 1f).coerceAtLeast(0f) * 0.35f) + ((levelMilestoneMult - 1f).coerceAtLeast(0f) * 0.45f) + ((critDmgGearBonus - 1f).coerceAtLeast(0f) * 0.35f) + ((wonderDropSkillMult - 1f).coerceAtLeast(0f) * 0.15f)
      val computedBonusHealthScale = (1.0f + bonusDamageFactor + bonusPackFactor + bonusMultiShotFactor + bonusShieldFactor + bonusResonanceFactor + bonusPowerLightFactor + bonusRarityMilestoneFactor)
        .coerceIn(1.0f, 32.0f)

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
        activePowerDamage = ((customBonusDmg + powerTierBonus + powerStarBonus + outfitDmgBonus + charTierDmg) * charStarMult * resonance.damageMult * levelMilestoneMult * customModeDmgMult).roundToInt().coerceAtLeast(2),
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
        weaponBonusDamage = weaponBonusDmg,
        weaponExtraSpread = attachmentExtraSpread,
        weaponExtraPierce = weapon.basePierce + attachmentExtraPierce + ((wpnLevel - 1) / 6) + (skDmg2 / 2) + ((wpnTier - 1) / 3) + (if (matchRules.infiniteAmmo) 1 else 0),
        projectileSpeed = weapon.baseProjectileSpeed * attachmentSpeedMult * (1f + ((wpnLevel - 1) * 0.008f).coerceAtMost(0.35f)),
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
        powerLevel = powerLight.powerLevel,
        lightLevel = powerLight.lightLevel,
        powerLightLabel = powerLight.tierLabel,
        difficultyTier = difficultyTier,
        characterRarity = outfitRarity,
        levelMilestoneMult = levelMilestoneMult,
        playerBonusHealthScale = computedBonusHealthScale,
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
  var playerVelX = 0f
  var playerVelY = 0f
  var dashVelX = 0f
  var dashVelY = 0f
  var camX = ARENA_CENTER - 0.5f
  var camY = ARENA_CENTER - 0.5f
  var aimX = 1f
  var aimY = 0f
  var joyDx = 0f
  var joyDy = 0f
  var touchAimActive = false
  var touchAimX = ARENA_CENTER
  var touchAimY = ARENA_CENTER
  var progressionReported = false
    private set

  fun markProgressionReported(): Boolean {
    if (progressionReported) return false
    progressionReported = true
    return true
  }

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
  var brRingRadius = 1.45f
  var exfilHoldTicks = 0f
  var calloutTicks = 0f
  var regenAccum = 0f
  var brDamageAccum = 0f
  var meleeDamageAccum = 0f
  var hazardTickAccum = 0f
  var frontierSpawnAccum = 0f
  var enteredOuterFrontierOnce = false
  var activeWonderWeapon: WonderWeaponKind? = null
  var wonderWeaponTicks = 0f
  var playerShotCounter = 0

  val hostiles = ArrayList<ArenaHostile>(48)
  val bolts = ArrayList<ArenaBolt>(96)
  val intels = ArrayList<ArenaIntel>(4)
  val drops = ArrayList<ArenaDrop>(12)
  val collectibles = ArrayList<ArenaCollectible>(36)
  val buildings = ArrayList<ArenaBuilding>(8)
  val traps = ArrayList<ArenaTrap>(16)
  val hazards = ArrayList<ArenaHazardZone>(8)
  val restCamps = ArrayList<ArenaRestCamp>(4)
  val worldStations = ArrayList<ArenaWorldStation>(12)
  val lootCrates = ArrayList<ArenaLootCrate>(12)
  val catEasterEggs = ArrayList<ArenaCatEasterEgg>(4)
  val wonderPickups = ArrayList<ArenaWonderPickup>(8)
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
  var characterShardsCollected by mutableLongStateOf(0L)
  var catsFoundCount by mutableIntStateOf(0)
  var lootCratesOpenedCount by mutableIntStateOf(0)
  val recruitedOutfitIdsList = mutableListOf<String>()

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
  var hudInRestCamp by mutableStateOf(false)
  var hudActiveHazardLabel by mutableStateOf<String?>(null)
  var hudNearbyPrompt by mutableStateOf<String?>(null)
  var hudInOuterFrontier by mutableStateOf(false)

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

  private val areaSpec = progression.activeAreaSpec()
  private val customAreaSpec = com.example.data.model.CuratedCustomMapsCatalog.findById(progression.activeAreaId, progression.customGameMaps)
  private val customAreaCategory = customAreaSpec?.gameCategory
  private val isBo3Map = progression.activeAreaId.startsWith("area_bo3_") ||
    progression.activeAreaId == "area_zombies_der_riese" ||
    customAreaCategory == com.example.data.model.CustomMapGameCategory.BO3

  val collectionDifficulty = progression.collectionDifficulty(world.id)
  val ngPlusSpec = progression.ngPlusSpec
  val gameplayRewardMultiplier = progression.combinedGameplayRewardMultiplier(world.id)
  var hudCurrentPlaceName by mutableStateOf(
    "${areaSpec.name.substringBefore(" (")} • ${areaSpec.buildingNames.firstOrNull() ?: world.mapSectors.firstOrNull() ?: "Central Sector"}"
  )
  var selectedElementOverride by mutableStateOf<ArenaDamageType?>(null)
  var hudBossArmorRatio by mutableFloatStateOf(0f)
  var hudBossImmuneSummary by mutableStateOf<String?>(null)
  var hudLastImmunityAlert by mutableStateOf<String?>(null)
  val hudActiveDamageType: ArenaDamageType
    get() = activeWeaponDamageType()

  private val accentColor = Color(areaSpec.accentHex)
  val matchRules = progression.effectiveMatchRules()

  init {
    setupBuildingsAndTraps()
    setupVerdanskInteractiveWorld()
    spawnLobbyBots()
    spawnAreaCollectibles()
    val initialWave = matchRules.startingWave.toLong().coerceAtLeast(1L)
    hudWave = initialWave
    spawnWave(initialWave)
  }

  fun activeWeaponDamageType(): ArenaDamageType {
    selectedElementOverride?.let { return it }
    val wonder = activeWonderWeapon
    if (wonder != null) {
      return when (wonder) {
        WonderWeaponKind.STAFF_SAND -> ArenaDamageType.SAND
        WonderWeaponKind.STAFF_CRYO -> ArenaDamageType.CRYO
        WonderWeaponKind.STAFF_THERMAL -> ArenaDamageType.THERMAL
        WonderWeaponKind.STAFF_BIO -> ArenaDamageType.BIO
        WonderWeaponKind.STAFF_ARC -> ArenaDamageType.ENERGY
        WonderWeaponKind.STAFF_KINETIC -> ArenaDamageType.KINETIC
        WonderWeaponKind.STAFF_VOID -> ArenaDamageType.VOID
        WonderWeaponKind.CRUCIBLE_BFG -> ArenaDamageType.THERMAL
        WonderWeaponKind.CHRONO_ZAT, WonderWeaponKind.GATE_HORIZON_CANNON -> ArenaDamageType.CRYO
        WonderWeaponKind.BLACKLIGHT_WHIP, WonderWeaponKind.MERCER_BIOCLAW_SWARM -> ArenaDamageType.BIO
        WonderWeaponKind.STARBLADE_BOOMERANG -> ArenaDamageType.KINETIC
        else -> ArenaDamageType.ENERGY
      }
    }
    return when {
      loadout.ammoModId in setOf("cryo_stasis", "nitrogen_subzero", "staff_cryo") || loadout.ammoModId.contains("cryo") || loadout.ammoModId.contains("nitrogen") -> ArenaDamageType.CRYO
      loadout.ammoModId in setOf("arc_chain", "tesla_hv", "suckerpunch_infamous_conduit", "staff_arc") || loadout.ammoModId.contains("arc") || loadout.ammoModId.contains("tesla") || loadout.ammoModId.contains("conduit") || loadout.ammoModId.contains("insomniac") -> ArenaDamageType.ENERGY
      loadout.ammoModId in setOf("vampiric_bio", "acid_venom", "staff_bio") || loadout.ammoModId.contains("bio") || loadout.ammoModId.contains("acid") || loadout.ammoModId.contains("venom") -> ArenaDamageType.BIO
      loadout.ammoModId in setOf("incendiary_core", "plasma_scorched", "bioware_ea_mass_plasma", "staff_thermal") || loadout.ammoModId.contains("incendiary") || loadout.ammoModId.contains("plasma") || loadout.ammoModId.contains("thermal") || loadout.ammoModId.contains("bioware") -> ArenaDamageType.THERMAL
      loadout.ammoModId in setOf("sand_abrasive", "desert_glass", "staff_sand") || loadout.ammoModId.contains("sand") || loadout.ammoModId.contains("glass") -> ArenaDamageType.SAND
      loadout.ammoModId in setOf("void_singularity", "remedy_valve_abyss_sand", "staff_void") || loadout.ammoModId.contains("void") || loadout.ammoModId.contains("singularity") || loadout.ammoModId.contains("remedy") -> ArenaDamageType.VOID
      loadout.ammoModId in setOf("standard_core", "ap_sabot", "rocksteady_wb_arkham", "rock_slug", "staff_kinetic") || loadout.ammoModId.contains("kinetic") || loadout.ammoModId.contains("sabot") || loadout.ammoModId.contains("arkham") || loadout.ammoModId.contains("rock") || loadout.ammoModId.contains("rocksteady") -> ArenaDamageType.KINETIC
      loadout.ammoModId.contains("activision") || loadout.ammoModId.contains("omni") -> ArenaDamageType.entries.random()
      loadout.weaponSpec.id in setOf("wpn_crucible_cannon", "wpn_hellbreaker_cannon", "wpn_olympian_spear") -> ArenaDamageType.THERMAL
      loadout.weaponSpec.id == "wpn_blacklight_carbine" -> ArenaDamageType.BIO
      loadout.weaponSpec.id == "wpn_chrono_repeater" -> ArenaDamageType.CRYO
      loadout.weaponSpec.id == "wpn_starblade_lancer" -> ArenaDamageType.KINETIC
      loadout.weaponSpec.id in setOf("wpn_pulse_carbine", "wpn_plasma_scatter", "wpn_arc_amp", "wpn_ship_phaser") -> ArenaDamageType.ENERGY
      else -> ArenaDamageType.KINETIC
    }
  }

  fun cycleElementalPrism(): ArenaDamageType {
    val cycle = listOf<ArenaDamageType?>(null) + ArenaDamageType.entries
    val curIdx = cycle.indexOf(selectedElementOverride).coerceAtLeast(0)
    selectedElementOverride = cycle[(curIdx + 1) % cycle.size]
    val active = activeWeaponDamageType()
    val label = if (selectedElementOverride == null) "AUTO (${active.label.uppercase()})" else active.label.uppercase()
    postCallout("PRISM FREQUENCY: $label")
    return active
  }

  fun damageImmunityChanceForWave(wave: Long): Float {
    val w = wave.coerceAtLeast(1L).toInt()
    val base = if (w <= 2) 0.02f else (0.05f + (w - 2) * 0.04f)
    val diffBonus = (collectionDifficulty.rank - 2).coerceAtLeast(0) * 0.03f
    val ngBonus = ngPlusSpec.tier * 0.03f
    return (base + diffBonus + ngBonus).coerceIn(0f, 0.35f)
  }

  fun rollImmunitiesForWave(wave: Long, isElite: Boolean, isBoss: Boolean, indexSeed: Int = 0): Set<ArenaDamageType> {
    if (progression.levelCustom.instagibMutator) return emptySet()
    val baseChance = damageImmunityChanceForWave(wave)
    val effectiveChance = when {
      isBoss -> (baseChance + 0.35f).coerceIn(0.35f, 0.96f)
      isElite -> (baseChance + 0.18f).coerceIn(0.18f, 0.92f)
      else -> baseChance
    }
    if (Random.nextFloat() >= effectiveChance) return emptySet()
    val allTypes = ArenaDamageType.entries
    val first = allTypes[(Random.nextInt(allTypes.size) + indexSeed) % allTypes.size]
    val dualChance = ((wave - 3L).coerceAtLeast(0L) * 0.07f + ngPlusSpec.tier * 0.08f).coerceIn(0f, 0.45f)
    return if ((isBoss || wave >= 4L) && Random.nextFloat() < dualChance) {
      val second = allTypes[(first.ordinal + 1 + Random.nextInt(allTypes.size - 1)) % allTypes.size]
      setOf(first, second)
    } else {
      setOf(first)
    }
  }

  fun applyDamageToHostile(h: ArenaHostile, rawDamage: Int, damageType: ArenaDamageType): Boolean {
    if (rawDamage <= 0) return false
    if (damageType in h.immuneTypes) {
      addSparkIfAllowed(h.x, h.y, damageType.color, 10f)
      val msg = "IMMUNE TO ${damageType.shortTag}!"
      if (hudLastImmunityAlert != msg) {
        hudLastImmunityAlert = msg
        postCallout("${h.bossName.ifBlank { h.archetype.uppercase() }} $msg (SWAP WPN / PRISM!)")
      }
      return false
    }
    var remaining = rawDamage
    if (h.armor > 0) {
      val absorbed = minOf(h.armor, remaining)
      h.armor -= absorbed
      remaining -= absorbed
      if (h.armor == 0) {
        addSparkIfAllowed(h.x, h.y, Color(0xFFB0BEC5), 18f)
      }
    }
    if (remaining > 0) {
      h.hp -= remaining
    }
    return true
  }

  fun placeDestinations(): List<ArenaPlaceDestination> {
    val out = mutableListOf<ArenaPlaceDestination>()
    val sectorCoords = listOf(
      Pair(ARENA_CENTER, ARENA_CENTER),
      Pair(ARENA_CENTER, 2.20f),
      Pair(7.55f, 4.95f),
      Pair(2.45f, 7.25f),
    )
    world.mapSectors.forEachIndexed { idx, sectorName ->
      val (sx, sy) = sectorCoords[idx % sectorCoords.size]
      out.add(
        ArenaPlaceDestination(
          id = "sector_$idx",
          label = sectorName,
          kind = "SECTOR",
          x = sx,
          y = sy,
          accent = accentColor,
        )
      )
    }
    buildings.forEach { b ->
      out.add(
        ArenaPlaceDestination(
          id = b.id,
          label = b.name,
          kind = "LANDMARK",
          x = b.x + b.width * 0.5f,
          y = b.y + b.height * 0.5f,
          accent = b.accentColor,
        )
      )
    }
    restCamps.forEach { camp ->
      out.add(
        ArenaPlaceDestination(
          id = camp.id,
          label = camp.name,
          kind = "CAMP",
          x = camp.x,
          y = camp.y,
          accent = Color(0xFF00E676),
        )
      )
    }
    if (modeId == "dmz") {
      out.add(
        ArenaPlaceDestination(
          id = "dmz_exfil",
          label = "Exfil Helipad",
          kind = "EXFIL",
          x = ARENA_CENTER,
          y = ARENA_CENTER - 1.25f,
          accent = Color(0xFF00E676),
        )
      )
    }
    out.add(
      ArenaPlaceDestination(
        id = "outer_frontier",
        label = "Outer Rift Frontier",
        kind = "FRONTIER",
        x = 5.0f,
        y = -0.30f,
        accent = Color(0xFFE040FB),
      )
    )
    return out
  }

  fun travelToPlace(destId: String): Boolean {
    if (hudRunEnded) return false
    val dest = placeDestinations().firstOrNull { it.id == destId } ?: return false
    playerX = dest.x
    playerY = dest.y
    playerVelX = 0f
    playerVelY = 0f
    dashVelX = 0f
    dashVelY = 0f
    camX = playerX - 0.5f
    camY = playerY - 0.5f
    dashInvulnTicks = maxOf(dashInvulnTicks, 30f)
    for (bi in 0 until arenaBots.size) {
      val bot = arenaBots[bi]
      if (bot.isAllied) {
        val ang = bi * 1.1f
        bot.x = playerX + cos(ang) * 0.16f
        bot.y = playerY + sin(ang) * 0.16f
      }
    }
    val enteredBuilding = buildings.firstOrNull { it.id == dest.id }
    if (enteredBuilding != null) {
      hudActiveBuildingName = enteredBuilding.name
      if (!enteredBuilding.explored) {
        enteredBuilding.explored = true
        buildingsExploredCount++
        blastShardsEarned += 4L
        combinedEssenceEarned += 10L
      }
    }
    hudCurrentPlaceName = dest.label
    pulseRingRadius = 0.05f
    postCallout("ARRIVED AT ${dest.kind}: ${dest.label.uppercase()}!")
    return true
  }

  // Damped Open-World Camera: follows player smoothly with zero liquid wobble inside 10x10 and Outer Frontier
  fun cameraOriginX(): Float = camX
  fun cameraOriginY(): Float = camY

  private fun setupBuildingsAndTraps() {
    buildings.clear()
    traps.clear()
    val areaSpec = progression.activeAreaSpec()
    val areaState = progression.areaState()
    val bNames = areaSpec.buildingNames
    // Spread across Verdansk-scale 10.0 x 10.0 map with central courtyard structures near ARENA_CENTER (5.0, 5.0)
    val positions = listOf(
      Pair(4.34f, 4.30f),
      Pair(5.40f, 4.28f),
      Pair(2.35f, 2.55f),
      Pair(7.45f, 2.55f),
      Pair(2.40f, 7.35f),
      Pair(7.40f, 7.35f),
      Pair(4.85f, 1.45f),
      Pair(4.85f, 8.20f),
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
          width = 0.34f,
          height = 0.26f,
          accentColor = bColor,
        )
      )
      // Place an Electric or Fire Trap near each building entrance
      traps.add(
        ArenaTrap(
          id = "trap_b_$idx",
          x = bx + 0.17f,
          y = by + 0.31f,
          kind = if (idx % 2 == 0) ArenaTrapKind.ELECTRIC else ArenaTrapKind.FIRE,
          radius = 0.15f + areaState.trapDensityTier * 0.008f,
        )
      )
    }
    // Central quadrant Electric & Fire Traps surrounding spawn courtyard (ARENA_CENTER ± 0.34f)
    val ringOffsets = listOf(
      Triple(ARENA_CENTER - 0.34f, ARENA_CENTER, ArenaTrapKind.ELECTRIC),
      Triple(ARENA_CENTER + 0.34f, ARENA_CENTER, ArenaTrapKind.FIRE),
      Triple(ARENA_CENTER, ARENA_CENTER - 0.34f, ArenaTrapKind.ELECTRIC),
      Triple(ARENA_CENTER, ARENA_CENTER + 0.34f, ArenaTrapKind.FIRE),
    )
    ringOffsets.forEachIndexed { idx, (tx, ty, kind) ->
      traps.add(ArenaTrap("trap_mid_$idx", tx, ty, kind, radius = 0.14f + areaState.trapDensityTier * 0.008f))
    }
  }

  private fun setupVerdanskInteractiveWorld() {
    hazards.clear()
    restCamps.clear()
    worldStations.clear()
    lootCrates.clear()
    catEasterEggs.clear()

    val areaTitle = areaSpec.name.substringBefore(" (").take(22)
    val (campCentral, campNorth, campWest) = when {
      customAreaCategory == com.example.data.model.CustomMapGameCategory.GTA4 ->
        Triple("Hove Beach Safehouse", "Algonquin High-Rise Loft", "Alderney Docks Cache")
      customAreaCategory == com.example.data.model.CustomMapGameCategory.SUPERMAN ->
        Triple("Daily Planet Roof Sanctuary", "Fortress Solar Chamber", "Centennial Harbor Pad")
      customAreaCategory == com.example.data.model.CustomMapGameCategory.ABYSS_DUNGEONS ->
        Triple("Lit Torch Safe Shrine", "Runic Campfire Alcove", "Blue Stone Extraction Altar")
      isBo3Map ->
        Triple("BO3 Teleporter Staging Pad", "$areaTitle Quick-Revive Camp", "Mystery Box Courtyard")
      world.id == "gotham_arkham" ->
        Triple("Batcomputer Safe Alcove", "$areaTitle Gargoyle Perch", "Oracle Overwatch Niche")
      world.id == "spiderverse_queens" ->
        Triple("F.E.A.S.T. Rooftop Haven", "$areaTitle Web-Anchor Loft", "Queens Subway Refuge")
      world.id == "stark_orbital" ->
        Triple("J.A.R.V.I.S. Shield Pod", "$areaTitle Maintenance Bay", "Reactor Recovery Deck")
      world.id == "empire_city_infamous" ->
        Triple("Zeke's Stash Sanctuary", "$areaTitle Substation Shelter", "Neon Rooftop Camp")
      world.id == "chrono_vault" ->
        Triple("Library Resonance Study", "$areaTitle TARDIS Alcove", "Dialer Safe Chamber")
      world.id == "aegis_starblade" ->
        Triple("Kyber Meditation Alcove", "$areaTitle Hangar Refuge", "Clone Command Pod")
      world.id == "ship_of_heroes_apotheosis" ->
        Triple("Justice Medical Bay", "$areaTitle Cadet Sanctuary", "Promenade Safe Zone")
      world.id == "hellbreaker_citadel" ->
        Triple("Spartan Forge Sanctuary", "$areaTitle Hearth Refuge", "Crucible Rest Alcove")
      world.id == "blacklight_conduit" ->
        Triple("Gentek Decontam Pod", "$areaTitle Safehouse Vault", "Liberty Tunnel Shelter")
      else -> Triple("$areaTitle Forward Camp", "North Sector Safe Camp", "Frontier Ridge Bivouac")
    }
    val stationNames = when {
      customAreaCategory == com.example.data.model.CustomMapGameCategory.GTA4 ->
        listOf("Roman's Ammu-Nation Pack", "Sprunk & Burger Shot", "LCPD Evidence Stash", "Algonquin Arms Dealer", "Alderney Munitions Van", "Liberty Armor Rack")
      customAreaCategory == com.example.data.model.CustomMapGameCategory.SUPERMAN ->
        listOf("Fortress Solar Forge", "S.T.A.R. Labs Conduit", "Metropolis Defense Crate", "Warworld Gladiator Anvil", "Daily Planet Drop Box", "Kryptonian Sun Font")
      customAreaCategory == com.example.data.model.CustomMapGameCategory.ABYSS_DUNGEONS ->
        listOf("Runic Forge Anvil", "Cleric Crimson Altar", "Mimic Quiver Chest", "Abyssal Enchant Pedestal", "Goblin Supply Sack", "Soul Tonic Fountain")
      isBo3Map ->
        listOf("BO3 Pack-a-Punch Machine", "Der Wunderfizz Perks", "Max Ammo Wall Crate", "Aether Pack Station", "Mystery Box Resupply", "Juggernog & Speed Cola")
      world.id == "gotham_arkham" ->
        listOf("Wayne Tech Workbench", "Arkham Detective Rig", "Utility Belt Resupply", "Batcave Outer Forge", "Grapnel Ammo Cache", "Batsuit Upgrade Rack")
      world.id == "spiderverse_queens" ->
        listOf("Horizon Lab Bench", "Parker Bio-Enhancer", "Web-Shooter Cartridge", "Oscorp Tier Bench", "Midtown Supply Drop", "Symbiote Stabilizer")
      world.id == "stark_orbital" ->
        listOf("Mark-X Armor Gantry", "Arc Reactor Overclock", "Munitions Pod Bay", "Outer Orbital Gantry", "Repulsor Reserve Crate", "Nano-Suit Dispensary")
      world.id == "empire_city_infamous" ->
        listOf("Conduit Amplifier", "Static Surge Dispenser", "Blast Shard Cache", "Warren Grid Booster", "Neon Conduit Crate", "Ray-Field Stabilizer")
      world.id == "chrono_vault" ->
        listOf("Chevron Upgrade Pedestal", "Chrono Elixir Fountain", "Artifact Supply Chest", "Outer Gate Amplifier", "TARDIS Supply Locker", "Sonic Resonator Stand")
      else -> listOf("Pack-a-Punch Machine", "Juggernog & Speed Cola", "Tactical Ammo Crate", "Outer Sector Pack Station", "Conduit Ammo Cache", "Quick-Revive & Double-Tap")
    }
    val papMain = stationNames[0]
    val perkMain = stationNames[1]
    val ammoMain = stationNames[2]
    val papOuter = stationNames[3]
    val ammoOuter = stationNames[4]
    val perkOuter = stationNames[5]

    // 1. Safe Rest Camps (take a break, shield regen, calm aggro)
    restCamps.add(ArenaRestCamp("camp_central", campCentral, ARENA_CENTER - 0.26f, ARENA_CENTER + 0.14f, 0.18f))
    restCamps.add(ArenaRestCamp("camp_north", campNorth, 5.0f, 2.4f, 0.22f))
    restCamps.add(ArenaRestCamp("camp_west", campWest, 2.9f, 5.1f, 0.22f))

    // 2. Interactive Pack-a-Punch, Perk Machines & Ammo Crates
    worldStations.add(ArenaWorldStation("st_pap_main", ARENA_CENTER + 0.28f, ARENA_CENTER + 0.12f, ArenaStationKind.PACK_A_PUNCH, papMain))
    worldStations.add(ArenaWorldStation("st_perk_jugg", ARENA_CENTER - 0.28f, ARENA_CENTER - 0.24f, ArenaStationKind.PERK_MACHINE, perkMain))
    worldStations.add(ArenaWorldStation("st_ammo_1", ARENA_CENTER + 0.24f, ARENA_CENTER - 0.26f, ArenaStationKind.AMMO_CRATE, ammoMain))
    worldStations.add(ArenaWorldStation("st_pap_east", 7.1f, 5.0f, ArenaStationKind.PACK_A_PUNCH, papOuter))
    worldStations.add(ArenaWorldStation("st_ammo_south", 5.1f, 7.2f, ArenaStationKind.AMMO_CRATE, ammoOuter))
    worldStations.add(ArenaWorldStation("st_perk_north", 5.0f, 2.0f, ArenaStationKind.PERK_MACHINE, perkOuter))

    // 3. All 5 Elemental Hazard Zones (Ice, Fire, Toxic, Electric, Windy) with Higher-Rarity Loot Crates inside!
    val hazardSpecs = listOf(
      Triple(ArenaHazardZone("haz_ice", ARENA_CENTER - 0.92f, ARENA_CENTER - 0.42f, 0.34f, ArenaHazardKind.ICE), com.example.data.model.CharacterRarity.RARE, true),
      Triple(ArenaHazardZone("haz_fire", ARENA_CENTER + 0.92f, ARENA_CENTER - 0.42f, 0.34f, ArenaHazardKind.FIRE), com.example.data.model.CharacterRarity.LEGENDARY, true),
      Triple(ArenaHazardZone("haz_toxic", ARENA_CENTER - 0.92f, ARENA_CENTER + 0.65f, 0.34f, ArenaHazardKind.TOXIC), com.example.data.model.CharacterRarity.EPIC, true),
      Triple(ArenaHazardZone("haz_electric", ARENA_CENTER + 0.92f, ARENA_CENTER + 0.65f, 0.34f, ArenaHazardKind.ELECTRIC), com.example.data.model.CharacterRarity.EXOTIC, true),
      Triple(ArenaHazardZone("haz_windy", ARENA_CENTER, ARENA_CENTER + 1.05f, 0.36f, ArenaHazardKind.WINDY, windDx = 0.0022f, windDy = -0.0011f), com.example.data.model.CharacterRarity.MYTHIC, true),
    )
    hazardSpecs.forEachIndexed { idx, (hz, crateRarity, fromHaz) ->
      hazards.add(hz)
      lootCrates.add(ArenaLootCrate("crate_haz_$idx", hz.x + 0.04f, hz.y, crateRarity, fromHazard = fromHaz))
    }

    // Standard sector loot crates
    lootCrates.add(ArenaLootCrate("crate_courtyard", ARENA_CENTER - 0.14f, ARENA_CENTER - 0.36f, com.example.data.model.CharacterRarity.UNCOMMON, fromHazard = false))
    lootCrates.add(ArenaLootCrate("crate_frontier_n", 5.0f, -0.35f, com.example.data.model.CharacterRarity.MYTHIC, fromHazard = true))
    lootCrates.add(ArenaLootCrate("crate_frontier_e", 10.35f, 5.0f, com.example.data.model.CharacterRarity.EXOTIC, fromHazard = true))

    // 4. Hidden Cat Easter Egg per world/area (unlocks Golden Cat Loot Crate!)
    val catOffsetAngle = (world.id.hashCode() and 0x7) * 0.78f
    catEasterEggs.add(
      ArenaCatEasterEgg(
        id = "cat_${world.id}_${progression.activeAreaId}",
        x = ARENA_CENTER + cos(catOffsetAngle) * 0.56f,
        y = ARENA_CENTER + sin(catOffsetAngle) * 0.56f,
        name = "${world.title.substringBefore(" ")} Lucky Cat",
      )
    )
    catEasterEggs.add(
      ArenaCatEasterEgg(
        id = "cat_verdansk_outer_${world.id}",
        x = 7.65f,
        y = 2.72f,
        name = "Golden Skyline Cat",
      )
    )
  }

  private fun spawnAreaCollectibles() {
    // Spawn initial Blast Shards, Perk Cans/Bottles, Colored Essence Orbs, and Blast Cores inside buildings
    val allOutfits = MultiverseArsenalCatalog.outfits
    val isHeroFrontier = progression.activeAreaId == "area_hero_frontier"
    buildings.forEachIndexed { idx, b ->
      val cx = b.x + b.width * 0.5f
      val cy = b.y + b.height * 0.5f
      collectibles.add(ArenaCollectible(cx - 0.05f, cy, ArenaCollectibleKind.BLAST_SHARD, amount = 4L))
      val perk = MultiverseArsenalCatalog.perkDrinks[idx % MultiverseArsenalCatalog.perkDrinks.size]
      collectibles.add(ArenaCollectible(cx + 0.05f, cy, ArenaCollectibleKind.PERK_DRINK, subId = perk.id, amount = 1L, color = Color(perk.colorHex)))
      val core = BlastCoreColor.entries[idx % BlastCoreColor.entries.size]
      collectibles.add(ArenaCollectible(cx, cy + 0.04f, ArenaCollectibleKind.BLAST_CORE, subId = core.id, amount = 1L, color = Color(core.colorHex)))
      val outfit = allOutfits[(idx * 5) % allOutfits.size]
      collectibles.add(ArenaCollectible(cx, cy - 0.05f, ArenaCollectibleKind.CHARACTER_SHARD, subId = outfit.id, amount = if (isHeroFrontier) 2L else 1L, color = Color(outfit.primaryHex)))
    }
    // Courtyard, Hero Patrol & Hero Frontier bonus character hunt shards
    val shardCount = when {
      modeId == "hero_patrol" -> 6
      isHeroFrontier -> 6
      else -> 2
    }
    for (s in 0 until shardCount) {
      val ang = s * (2f * PI.toFloat() / shardCount)
      val outfit = if (modeId == "hero_patrol" && s % 2 == 0) {
        allOutfits.firstOrNull { it.characterGroup == com.example.data.model.CharacterUniverseGroup.SPIDERMAN || it.characterGroup == com.example.data.model.CharacterUniverseGroup.BATMAN }
          ?: allOutfits[Random.nextInt(allOutfits.size)]
      } else {
        allOutfits[Random.nextInt(allOutfits.size)]
      }
      collectibles.add(
        ArenaCollectible(
          x = playerX + cos(ang) * 0.44f,
          y = playerY + sin(ang) * 0.44f,
          kind = ArenaCollectibleKind.CHARACTER_SHARD,
          subId = outfit.id,
          amount = if (isHeroFrontier || modeId == "hero_patrol") 2L else 1L,
          color = Color(outfit.secondaryHex),
          life = 1400f,
        )
      )
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

  fun openLootCrate(crate: ArenaLootCrate) {
    if (crate.opened) return
    crate.opened = true
    lootCratesOpenedCount++
    val shardBonus = crate.rarity.rank.toLong()
    blastShardsEarned += shardBonus * 2L
    characterShardsCollected += shardBonus
    hudScore += crate.rarity.rank * 400L
    val foundOutfit = MultiverseArsenalCatalog.outfits[Random.nextInt(MultiverseArsenalCatalog.outfits.size)]
    recruitedOutfitIdsList.add(foundOutfit.id)
    collectibles.add(
      ArenaCollectible(
        x = crate.x,
        y = crate.y,
        kind = ArenaCollectibleKind.CHARACTER_SHARD,
        subId = foundOutfit.id,
        amount = shardBonus,
        color = Color(crate.rarity.colorHex),
      )
    )
    if ((crate.fromHazard || crate.rarity.rank >= 4) && !progression.collectionCustomMode.shouldSuppressCrateWonderDrops(progression.activeAreaId, progression.customGameMaps)) {
      spawnRandomWonderWeapon(crate.x, crate.y)
    }
    pulseRingRadius = 0.05f
    postCallout("${crate.rarity.label.uppercase()} CRATE! +$shardBonus SHARDS (${foundOutfit.nameWithVersion})")
  }

  fun discoverCat(cat: ArenaCatEasterEgg) {
    if (cat.found) return
    cat.found = true
    catsFoundCount++
    lootCratesOpenedCount++
    hudScore += 2500L
    blastShardsEarned += 15L
    characterShardsCollected += 3L
    val foundOutfit = MultiverseArsenalCatalog.outfits.firstOrNull { it.isOriginalCharacter } ?: MultiverseArsenalCatalog.outfits.first()
    recruitedOutfitIdsList.add(foundOutfit.id)
    collectibles.add(ArenaCollectible(cat.x, cat.y, ArenaCollectibleKind.CAT_EASTER_EGG, subId = cat.id, amount = 1L))
    spawnRandomWonderWeapon(cat.x + 0.04f, cat.y)
    pulseRingRadius = 0.06f
    postCallout("CAT EASTER EGG FOUND (${cat.name})! GOLDEN LOOT CRATE + WONDER DROP!")
  }

  fun interactNearestWorldObject(): Boolean {
    if (hudRunEnded) return false
    for (ci in 0 until catEasterEggs.size) {
      val cat = catEasterEggs[ci]
      if (!cat.found) {
        val dx = cat.x - playerX
        val dy = cat.y - playerY
        if (dx * dx + dy * dy <= 0.16f * 0.16f) {
          discoverCat(cat)
          return true
        }
      }
    }
    for (li in 0 until lootCrates.size) {
      val crate = lootCrates[li]
      if (!crate.opened) {
        val dx = crate.x - playerX
        val dy = crate.y - playerY
        if (dx * dx + dy * dy <= 0.16f * 0.16f) {
          openLootCrate(crate)
          return true
        }
      }
    }
    for (si in 0 until worldStations.size) {
      val st = worldStations[si]
      val dx = st.x - playerX
      val dy = st.y - playerY
      if (dx * dx + dy * dy <= 0.17f * 0.17f) {
        when (st.kind) {
          ArenaStationKind.PACK_A_PUNCH -> upgradePackAPunch()
          ArenaStationKind.PERK_MACHINE -> {
            if (st.cooldownTicks <= 0f) {
              st.cooldownTicks = 300f
              perkDrinksCollected++
              hudShield = (hudShield + 50).coerceAtMost(loadout.maxShield)
              overdriveTicks = maxOf(overdriveTicks, 240f)
              postCallout("DRANK ${st.subLabel.uppercase()}! (+50 SHIELD & OVERDRIVE)")
            } else {
              postCallout("PERK MACHINE RECHARGING (${(st.cooldownTicks / 60f).toInt() + 1}s)")
            }
          }
          ArenaStationKind.AMMO_CRATE -> {
            if (st.cooldownTicks <= 0f) {
              st.cooldownTicks = 240f
              powerCd = 0f
              consumableCd = 0f
              lethalCd = 0f
              tacticalCd = 0f
              hudShield = (hudShield + 25).coerceAtMost(loadout.maxShield)
              postCallout("AMMO CRATE OPENED! ALL ABILITIES & AMMO RESUPPLIED!")
            } else {
              postCallout("AMMO CRATE RESTOCKING (${(st.cooldownTicks / 60f).toInt() + 1}s)")
            }
          }
        }
        return true
      }
    }
    for (ri in 0 until restCamps.size) {
      val camp = restCamps[ri]
      val dx = camp.x - playerX
      val dy = camp.y - playerY
      if (dx * dx + dy * dy <= camp.radius * camp.radius) {
        hudShield = (hudShield + 60).coerceAtMost(loadout.maxShield)
        stasisTicks = maxOf(stasisTicks, 150f)
        postCallout("RESTING AT ${camp.name.uppercase()}: +60 SHIELD & HORDE CALMED")
        return true
      }
    }
    triggerWorldInteract()
    return true
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
            val trapDmgType = if (t.kind == ArenaTrapKind.ELECTRIC) ArenaDamageType.ENERGY else ArenaDamageType.THERMAL
            applyDamageToHostile(h, 6 + bonus, trapDmgType)
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
          x = playerX + cos(angle) * dist,
          y = playerY + sin(angle) * dist,
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
    blastShardsEarned = if (blastShardsEarned > 0L) {
      (blastShardsEarned * progression.collectionCustomMode.effectiveEssenceMultFor(progression.activeAreaId, progression.customGameMaps)).toLong().coerceAtLeast(1L)
    } else 0L,
    perkDrinksCollected = perkDrinksCollected,
    blastCoresEarned = blastCoresEarned.toMap(),
    buildingsExplored = buildingsExploredCount,
    trapKills = trapKillsCount,
    worldId = world.id,
    areaId = progression.activeAreaId,
    characterShardsCollected = characterShardsCollected,
    catsFound = catsFoundCount,
    lootCratesOpened = lootCratesOpenedCount,
    recruitedOutfitIds = recruitedOutfitIdsList.toList(),
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
    val diffTier = lc.worldDifficultyTier
    val rawCount = when (modeId) {
      "sandbox" -> 10
      "onslaught" -> (14 + w * 4).toInt().coerceAtMost(58)
      "hero_patrol" -> (12 + w * 3).toInt().coerceAtMost(48)
      else -> (10 + w * 3).toInt().coerceAtMost(44)
    }
    val cm = progression.collectionCustomMode
    val activeArea = progression.activeAreaId
    val customMaps = progression.customGameMaps
    val ngPlusSpawnBoost = 1f + ngPlusSpec.tier * 0.12f
    val customSpawnBoost = cm.effectiveSpawnCountMultFor(activeArea, customMaps)
    val count = (rawCount * lc.enemyDensityMultiplier.coerceIn(0.75f, 2.5f) * ngPlusSpawnBoost * customSpawnBoost).roundToInt().coerceIn(8, 72)
    val roundHpMult = 1.0f + (w - 1L).coerceAtLeast(0L) * 0.22f + (w * w * 0.02f)
    val roundArmorMult = 1.0f + (w - 1L).coerceAtLeast(0L) * 0.10f
    val customHpScale = cm.effectiveHostileHpMultFor(activeArea, customMaps)
    val customSpeedScale = cm.effectiveEnemySpeedMultFor(activeArea, customMaps)
    val totalHpScale = diffTier.enemyHpScale * collectionDifficulty.enemyHpScale * ngPlusSpec.enemyHpScale * customHpScale
    val totalSpeedScale = diffTier.enemySpeedScale * collectionDifficulty.enemySpeedScale * ngPlusSpec.enemySpeedScale * customSpeedScale
    val playerBonusScale = loadout.playerBonusHealthScale.coerceAtLeast(1.0f)
    val armorBonusScale = 1f + (playerBonusScale - 1f) * 0.35f

    for (i in 0 until count) {
      val angle = Random.nextFloat() * 2f * PI.toFloat()
      val r = 0.52f + (Random.nextFloat() * 0.34f)
      val isElite = (i == 0 && w >= 2) || (i % 7 == 0 && w >= 3) || (modeId == "onslaught" && i % 5 == 0)
      val archetype = chooseArchetype(world.id, w, i)
      val archHpBonus = when (archetype) {
        "mech", "dup_conduit", "ultron_sentry" -> 2
        "blackwatch", "necromorph", "symbiote_stalker", "blackgate_enforcer" -> 1
        else -> 0
      }
      val rawHp = ((4 + w.toInt() * 2 + archHpBonus * 2 + if (isElite) 6 else 0) * roundHpMult * totalHpScale * playerBonusScale)
        .roundToInt()
        .coerceAtLeast(4)
      val baseHp = if (lc.instagibMutator) 1 else rawHp
      val hasArmorUnit = isElite ||
        archetype in setOf("mech", "dup_conduit", "ultron_sentry", "blackgate_enforcer") ||
        (w >= 3L && i % 4 == 0) ||
        cm.shouldReinforceArmor(activeArea, isElite = isElite, userCustomMaps = customMaps)
      val rawArmor = if (lc.instagibMutator || !hasArmorUnit) 0 else {
        val armorCustomBump = if (cm.shouldReinforceArmor(activeArea, isElite = isElite, userCustomMaps = customMaps)) 2 else 0
        (((1 + w.toInt() / 2 + if (isElite) 4 else 0) + armorCustomBump) * roundArmorMult * totalHpScale * armorBonusScale)
          .roundToInt()
          .coerceAtLeast(1)
      }
      val baseSpd = when (archetype) {
        "rusher", "water_hive_swarm", "rift_phantom", "symbiote_stalker", "oscorp_goblin_drone" -> (0.0017f + w * 0.00016f).coerceAtMost(0.0038f)
        "mech", "dup_conduit", "clockwork_automaton", "blackgate_enforcer" -> (0.0008f + w * 0.00010f).coerceAtMost(0.0020f)
        else -> (0.00105f + w * 0.00014f).coerceAtMost(0.0030f)
      }
      val spd = baseSpd * totalSpeedScale
      val immunities = rollImmunitiesForWave(wave = w, isElite = isElite, isBoss = false, indexSeed = i)
      hostiles.add(
        ArenaHostile(
          x = playerX + cos(angle) * r,
          y = playerY + sin(angle) * r,
          hp = baseHp,
          maxHp = baseHp,
          speed = spd,
          isElite = isElite,
          archetype = archetype,
          rangedCooldownTicks = 85f + Random.nextFloat() * 80f,
          armor = rawArmor,
          maxArmor = rawArmor,
          immuneTypes = immunities,
        )
      )
    }

    val scheduledBoss = lc.bossRushMutator || matchRules.bossRush || modeId == "boss_rush" || w % 3L == 0L || (modeId == "br" && w == 4L)
    if (scheduledBoss) {
      spawnBossForWorld(w)
    } else if (cm.isActiveForArea(activeArea, customMaps) && cm.collectionAmbushSurge && w >= 2L && w % 2L == 0L) {
      spawnBossForWorld(w)
    }

    if (modeId == "dmz" && intels.isEmpty()) {
      intels.add(ArenaIntel(ARENA_CENTER - 0.55f, ARENA_CENTER - 0.50f))
      intels.add(ArenaIntel(6.85f, 3.80f))
      intels.add(ArenaIntel(3.25f, 6.95f))
    }
    if (modeId == "conduit_ctf") {
      traps.forEach { it.cooldownTicks = (it.cooldownTicks * 0.5f).coerceAtLeast(0f) }
    }
  }

  fun spawnRandomWonderWeapon(
    x: Float = (playerX - 0.25f + Random.nextFloat() * 0.50f),
    y: Float = (playerY - 0.25f + Random.nextFloat() * 0.50f),
    fixedKind: WonderWeaponKind? = null,
  ): WonderWeaponKind {
    val chosen = fixedKind ?: pickWeightedWonderKind()
    if (wonderPickups.size >= 8) {
      wonderPickups.removeAt(0)
    }
    wonderPickups.add(ArenaWonderPickup(x, y, chosen))
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
    val activeId = progression.activeAreaId
    val (arch, name) = when {
      activeId == "area_mended_convergence" ->
        if (w % 2L == 0L) "boss_hydra" to "MENDED CONVERGENCE LOOM" else "boss_mech" to "FRACTURE REMNANT SOVEREIGN"
      activeId == "area_hero_frontier" ->
        "boss_hydra" to "HERO FRONTIER SOVEREIGN"
      customAreaCategory == com.example.data.model.CustomMapGameCategory.GTA4 ->
        if (w % 2L != 0L) "boss_mech" to "NOOSE ANNIHILATOR CHOPPER" else "boss_bane" to "DIMITRI & LCPD ENFORCER"
      customAreaCategory == com.example.data.model.CustomMapGameCategory.SUPERMAN ->
        if (w % 2L != 0L) "boss_mech" to "METALLO KRYPTONITE MECH" else "boss_bane" to "MONGUL WARWORLD WARLORD"
      customAreaCategory == com.example.data.model.CustomMapGameCategory.ABYSS_DUNGEONS ->
        if (w % 2L != 0L) "boss_hydra" to "ABYSSAL LICH KING" else "boss_croc" to "CAVE TROLL JUGGERNAUT"
      activeId == "area_col_custom_arkham_origins_bridge" ->
        if (w % 2L != 0L) "boss_bane" to "DEATHSTROKE & FIREFLY SQUAD" else "boss_croc" to "TN-1 BANE ARKHAM ORIGINS"
      activeId == "area_col_custom_arkham_blackgate" ->
        if (w % 2L != 0L) "boss_croc" to "SOLOMON GRUNDY BLACKGATE" else "boss_bane" to "BLACK MASK CELLBLOCK JUGGERNAUT"
      activeId == "area_col_custom_arkham_asylum_city" ->
        if (w % 2L != 0L) "boss_bane" to "TITAN JOKER ASYLUM PRIME" else "boss_mech" to "MR. FREEZE CRYO-SUIT"
      isBo3Map ->
        if (w % 2L != 0L) "boss_mech" to "PANZERSOLDAT MK.II" else "boss_hydra" to "MARGWA AETHER TITAN"
      else -> when (world.id) {
        "blacklight_conduit" -> if (w % 2L != 0L) "boss_hydra" to "WATER HYDRA ALPHA" else "boss_mech" to "BLACKWATCH SUPER-HUNTER"
        "hellbreaker_citadel" -> if (w % 2L == 0L) "boss_mech" to "ASSAULT MECH TITAN" else "boss_croc" to "KILLER CROC JUGGERNAUT"
        "aegis_starblade" -> "boss_croc" to "COSMIC STARBLADE DREADNOUGHT"
        "chrono_vault" -> "boss_hydra" to "CHRONO RIFT WARDEN"
        "gotham_arkham" -> if (w % 2L != 0L) "boss_bane" to "BANE ARKHAM TITAN" else "boss_croc" to "KILLER CROC JUGGERNAUT"
        "spiderverse_queens" -> if (w % 2L != 0L) "boss_venom" to "VENOM SYMBIOTE PRIME" else "boss_goblin" to "GREEN GOBLIN SKY-OVERLORD"
        "stark_orbital" -> "boss_mech" to "IRON MONGER APEX MECH"
        "empire_city_infamous" -> "boss_hydra" to "KESSEL RAY-SPHERE CONDUIT"
        "ship_of_heroes_apotheosis" -> "boss_mech" to "APOTHEOSIS DREADNOUGHT"
        "mr_crossover_nexus" -> if (w % 2L != 0L) "boss_venom" to "OMNIVERSE SYMBIOTE TITAN" else "boss_bane" to "CROSSOVER RIFT SOVEREIGN"
        "campaign_origins" -> if (w % 2L == 0L) "boss_mech" to "CONVERGENCE LOOM TITAN" else "boss_hydra" to "RIFT SOVEREIGN PRIME"
        else -> "boss_mech" to "GENTEK CONTAINMENT MECH"
      }
    }
    val lc = progression.levelCustom
    val cm = progression.collectionCustomMode
    val activeArea = progression.activeAreaId
    val totalHpScale = lc.worldDifficultyTier.enemyHpScale * collectionDifficulty.enemyHpScale * ngPlusSpec.enemyHpScale * cm.effectiveHostileHpMultFor(activeArea, progression.customGameMaps)
    val totalSpeedScale = lc.worldDifficultyTier.enemySpeedScale * collectionDifficulty.enemySpeedScale * ngPlusSpec.enemySpeedScale * cm.effectiveEnemySpeedMultFor(activeArea, progression.customGameMaps)
    val roundHpMult = 1.0f + (w - 1L).coerceAtLeast(0L) * 0.28f + (w * w * 0.03f)
    val roundArmorMult = 1.0f + (w - 1L).coerceAtLeast(0L) * 0.16f
    val playerBonusScale = loadout.playerBonusHealthScale.coerceAtLeast(1.0f)
    val armorBonusScale = 1f + (playerBonusScale - 1f) * 0.40f

    val hp = if (lc.instagibMutator) 6 else {
      ((24 + w.toInt() * 8) * roundHpMult * totalHpScale * playerBonusScale).roundToInt().coerceAtLeast(24)
    }
    val bossArmor = if (lc.instagibMutator) 0 else {
      ((12 + w.toInt() * 5) * roundArmorMult * totalHpScale * armorBonusScale).roundToInt().coerceAtLeast(10)
    }
    val immunities = rollImmunitiesForWave(wave = w, isElite = true, isBoss = true, indexSeed = w.toInt())
    hostiles.add(
      ArenaHostile(
        x = playerX,
        y = playerY - 0.42f,
        hp = hp,
        maxHp = hp,
        speed = 0.00095f * totalSpeedScale,
        isElite = true,
        isBoss = true,
        archetype = arch,
        bossName = name,
        rangedCooldownTicks = 65f,
        armor = bossArmor,
        maxArmor = bossArmor,
        immuneTypes = immunities,
      )
    )
    postCallout("BOSS INBOUND: $name!")
  }

  private fun chooseArchetype(worldId: String, wave: Long, index: Int): String {
    if (progression.activeAreaId == "area_hero_frontier") {
      return when (index % 7) {
        0 -> "rift_phantom"
        1 -> "blackwatch"
        2 -> "gentek_mutant"
        3 -> "cosmic_raider"
        4 -> "dup_conduit"
        5 -> "symbiote_stalker"
        else -> "blackgate_enforcer"
      }
    }
    when (customAreaCategory) {
      com.example.data.model.CustomMapGameCategory.GTA4 -> return when (index % 4) {
        0 -> "blackgate_enforcer"
        1 -> "trooper"
        2 -> "arkham_thug"
        else -> "rusher"
      }
      com.example.data.model.CustomMapGameCategory.SUPERMAN -> return when (index % 4) {
        0 -> "mech"
        1 -> "ultron_sentry"
        2 -> "cosmic_raider"
        else -> "dup_conduit"
      }
      com.example.data.model.CustomMapGameCategory.ABYSS_DUNGEONS -> return when (index % 4) {
        0 -> "necromorph"
        1 -> "slasher"
        2 -> "rift_phantom"
        else -> "clockwork_automaton"
      }
      com.example.data.model.CustomMapGameCategory.BO3 -> return when (index % 4) {
        0 -> "necromorph"
        1 -> "slasher"
        2 -> if (wave >= 2) "mech" else "gentek_mutant"
        else -> "rusher"
      }
      else -> Unit
    }
    return when (worldId) {
      "blacklight_conduit" -> when (index % 5) {
        0 -> "water_hive_swarm"
        1 -> "blackwatch"
        2 -> "gentek_mutant"
        3 -> "dup_conduit"
        else -> if (wave >= 2) "rusher" else "trooper"
      }
      "hellbreaker_citadel" -> when (index % 4) {
        0 -> "necromorph"
        1 -> "slasher"
        2 -> "mech"
        else -> "gentek_mutant"
      }
      "chrono_vault" -> when (index % 4) {
        0 -> "clockwork_automaton"
        1 -> "trooper"
        2 -> "slasher"
        else -> "rift_phantom"
      }
      "aegis_starblade" -> when (index % 4) {
        0 -> "cosmic_raider"
        1 -> "trooper"
        2 -> "mech"
        else -> "rusher"
      }
      "gotham_arkham" -> when (index % 4) {
        0 -> "blackgate_enforcer"
        1 -> "joker_wild"
        2 -> "arkham_thug"
        else -> "trooper"
      }
      "spiderverse_queens" -> when (index % 4) {
        0 -> "symbiote_stalker"
        1 -> "oscorp_goblin_drone"
        2 -> "reaper_conduit"
        else -> "rusher"
      }
      "stark_orbital" -> when (index % 4) {
        0 -> "ultron_sentry"
        1 -> "mech"
        2 -> "trooper"
        else -> "clockwork_automaton"
      }
      "empire_city_infamous" -> when (index % 4) {
        0 -> "reaper_conduit"
        1 -> "dup_conduit"
        2 -> "blackwatch"
        else -> "trooper"
      }
      "ship_of_heroes_apotheosis" -> when (index % 4) {
        0 -> "cosmic_raider"
        1 -> "rift_phantom"
        2 -> "ultron_sentry"
        else -> "trooper"
      }
      "mr_crossover_nexus", "campaign_origins" -> when (index % 7) {
        0 -> "rift_phantom"
        1 -> "symbiote_stalker"
        2 -> "blackgate_enforcer"
        3 -> "clockwork_automaton"
        4 -> "necromorph"
        5 -> "mech"
        else -> "cosmic_raider"
      }
      else -> when (index % 5) {
        0 -> "blackwatch"
        1 -> if (wave >= 2) "gentek_mutant" else "standard"
        2 -> "trooper"
        3 -> "dup_conduit"
        else -> "necromorph"
      }
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
    touchAimX = worldNormX
    touchAimY = worldNormY
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
    val shotDmgType = activeWeaponDamageType()
    val critBoost = if (progression.levelCustom.critSurgeMutator && playerShotCounter % 3 == 0) 2f else 1f
    val instagibBoost = if (progression.levelCustom.instagibMutator || matchRules.headshotsOnly) 3 else 1

    if (wonder != null) {
      val wonderCol = selectedElementOverride?.color ?: Color(wonder.accentHex)
      val wonderTierMult = ProgressionCatalog.tierMultiplier(progression.wonderTier(wonder.id))
      val wonderStarMult = StarRankMeta.forStars(progression.wonderStar(wonder.id)).multiplier
      val purpleBonus = 1f + (progression.blastCoreLevel(BlastCoreColor.PURPLE) * 0.10f)
      val coreDmgMult = if (progression.wonderCustom.coreId == "core_pap_mk2") 1.50f else if (progression.wonderCustom.coreId == "core_stargate_naquadah") 1.30f else 1.0f
      val wonderDmg = (((wonder.baseDamage + packLevel * 2) * coreDmgMult * wonderTierMult * wonderStarMult * purpleBonus * critBoost * loadout.critDamageGearBonus * instagibBoost)).roundToInt().coerceAtLeast(1)

      if (progression.wonderCustom.amplifierId == "amp_superman_fortress") {
        hudShield = (hudShield + 2).coerceAtMost(loadout.maxShield)
      }
      if (progression.wonderCustom.lensId == "lens_diffraction_fan") {
        for (fanOffset in listOf(-0.25f, 0.25f)) {
          val fa = baseAngle + fanOffset
          bolts.add(
            ArenaBolt(
              x = playerX,
              y = playerY,
              vx = cos(fa) * 0.026f,
              vy = sin(fa) * 0.026f,
              life = 60f,
              isSupercharged = true,
              pierceLeft = 2,
              damage = wonderDmg,
              color = wonderCol,
              damageType = shotDmgType,
            )
          )
        }
      }
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
                damageType = shotDmgType,
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
                damageType = shotDmgType,
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
              damageType = shotDmgType,
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
              damageType = shotDmgType,
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
              damageType = shotDmgType,
            )
          )
        }
        WonderWeaponKind.RAY_SPHERE_OVERDRIVE -> {
          for (offset in listOf(-0.14f, 0f, 0.14f)) {
            val a = baseAngle + offset
            bolts.add(
              ArenaBolt(
                x = playerX,
                y = playerY,
                vx = cos(a) * 0.029f,
                vy = sin(a) * 0.029f,
                life = 62f,
                isSupercharged = true,
                pierceLeft = 3,
                damage = wonderDmg,
                color = wonderCol,
                damageType = shotDmgType,
              )
            )
          }
        }
        WonderWeaponKind.MERCER_BIOCLAW_SWARM -> {
          for (offset in listOf(-0.22f, 0f, 0.22f)) {
            val a = baseAngle + offset
            bolts.add(
              ArenaBolt(
                x = playerX,
                y = playerY,
                vx = cos(a) * 0.031f,
                vy = sin(a) * 0.031f,
                life = 52f,
                isSupercharged = true,
                pierceLeft = 4,
                damage = wonderDmg,
                color = wonderCol,
                damageType = shotDmgType,
              )
            )
          }
        }
        WonderWeaponKind.GATE_HORIZON_CANNON -> {
          bolts.add(
            ArenaBolt(
              x = playerX,
              y = playerY,
              vx = cos(baseAngle) * 0.022f,
              vy = sin(baseAngle) * 0.022f,
              life = 74f,
              isSupercharged = true,
              pierceLeft = 6,
              damage = wonderDmg + 2,
              color = wonderCol,
              damageType = shotDmgType,
            )
          )
        }
        else -> {
          bolts.add(
            ArenaBolt(
              x = playerX,
              y = playerY,
              vx = cos(baseAngle) * 0.026f,
              vy = sin(baseAngle) * 0.026f,
              life = 64f,
              isSupercharged = true,
              pierceLeft = 3,
              damage = wonderDmg,
              color = wonderCol,
              damageType = shotDmgType,
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
      "wpn_arc_amp" -> Color(0xFF00E5FF)
      "wpn_blacklight_carbine" -> Color(0xFFFF1744)
      "wpn_stargate_staff" -> Color(0xFFFFD54F)
      "wpn_olympian_spear" -> Color(0xFFFF6D00)
      "wpn_ship_phaser" -> Color(0xFFE040FB)
      else -> loadout.tracerColor
    }
    val boltColor = when {
      instaKillTicks > 0f -> Color(0xFFFF1744)
      selectedElementOverride != null -> selectedElementOverride!!.color
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
          damageType = shotDmgType,
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
          h.x += (dx / d) * 0.16f
          h.y += (dy / d) * 0.16f
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

    val powerDmgType = when {
      loadout.hasChevronPower || world.id == "chrono_vault" -> ArenaDamageType.CRYO
      loadout.hasTendrilPower || world.id == "blacklight_conduit" -> ArenaDamageType.BIO
      loadout.hasRepulsePower || world.id == "aegis_starblade" -> ArenaDamageType.ENERGY
      else -> ArenaDamageType.ENERGY
    }
    val iter = hostiles.iterator()
    while (iter.hasNext()) {
      val h = iter.next()
      val dx = h.x - playerX
      val dy = h.y - playerY
      if (dx * dx + dy * dy <= killRadiusSq) {
        applyDamageToHostile(h, baseDmg, powerDmgType)
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

    if (customAreaCategory == com.example.data.model.CustomMapGameCategory.GTA4) {
      hudShield = (hudShield + 45).coerceAtMost(loadout.maxShield)
      val angle = atan2(aimY, aimX)
      for (offset in listOf(-0.18f, 0f, 0.18f)) {
        val a = angle + offset
        bolts.add(
          ArenaBolt(
            x = playerX,
            y = playerY,
            vx = cos(a) * 0.025f,
            vy = sin(a) * 0.025f,
            life = 60f,
            isSupercharged = true,
            pierceLeft = 3,
            damage = 7,
            color = Color(0xFFFFAB40),
            damageType = ArenaDamageType.THERMAL,
          )
        )
      }
      postCallout("GTA IV SAFEHOUSE RPG BARRAGE! +45 ARMOR")
      return
    }
    if (customAreaCategory == com.example.data.model.CustomMapGameCategory.SUPERMAN) {
      val iter = hostiles.iterator()
      while (iter.hasNext()) {
        val h = iter.next()
        val dx = h.x - playerX
        val dy = h.y - playerY
        if (dx * dx + dy * dy <= 0.65f * 0.65f) {
          applyDamageToHostile(h, 6, ArenaDamageType.THERMAL)
          h.cryoSlowTicks = maxOf(h.cryoSlowTicks, 150f)
          arcs.add(ArenaArcLine(playerX, playerY, h.x, h.y, Color(0xFFFF3D71), 20f))
          if (h.hp <= 0) {
            iter.remove()
            onHostileDefeated(h, scoreMult)
          }
        }
      }
      hudShield = (hudShield + 40).coerceAtMost(loadout.maxShield)
      postCallout("SOLAR HEAT-VISION & ARCTIC BREATH SURGE!")
      return
    }
    if (customAreaCategory == com.example.data.model.CustomMapGameCategory.ABYSS_DUNGEONS) {
      combinedEssenceEarned += 20L
      blastShardsEarned += 4L
      hudShield = (hudShield + 40).coerceAtMost(loadout.maxShield)
      stasisTicks = maxOf(stasisTicks, 140f)
      postCallout("ABYSSAL TORCH SHRINE LIT! +20 COMBINED ESSENCE & STASIS")
      return
    }
    if (isBo3Map || world.id == "sector_zero") {
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
          h.hitStaggerTicks = 12f
          arcs.add(ArenaArcLine(playerX, playerY, h.x, h.y, Color(0xFFFF3D71), 20f))
          addSparkIfAllowed(h.x, h.y, Color(0xFF7C4DFF), 22f)
        }
        val iter = hostiles.iterator()
        while (iter.hasNext()) {
          val h = iter.next()
          if (h.hp <= 0) {
            iter.remove()
            onHostileDefeated(h, scoreMult)
          }
        }
        postCallout("BLACKLIGHT WATER HIVE BIO-SURGE!")
      }
      "hellbreaker_citadel" -> {
        hudShield = (hudShield + 45).coerceAtMost(loadout.maxShield)
        instaKillTicks = maxOf(instaKillTicks, 180f)
        postCallout("CRUCIBLE FURY: GLORY SIPHON!")
      }
      "gotham_arkham" -> {
        val iter = hostiles.iterator()
        while (iter.hasNext()) {
          val h = iter.next()
          val dx = h.x - playerX
          val dy = h.y - playerY
          val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
          if (d <= 0.55f) {
            h.hp -= 4
            h.hitStaggerTicks = 40f
            h.knockVelX += (dx / d) * 0.012f
            h.knockVelY += (dy / d) * 0.012f
            arcs.add(ArenaArcLine(playerX, playerY, h.x, h.y, Color(0xFFFFD54F), 18f))
            if (h.hp <= 0) {
              iter.remove()
              onHostileDefeated(h, scoreMult)
            }
          }
        }
        hudShield = (hudShield + 30).coerceAtMost(loadout.maxShield)
        postCallout("BAT-SIGNAL RECON PULSE: HOSTILES STAGGERED!")
      }
      "spiderverse_queens" -> {
        val limit = minOf(10, hostiles.size)
        for (i in 0 until limit) {
          val h = hostiles[i]
          h.cryoSlowTicks = maxOf(h.cryoSlowTicks, 180f)
          h.hitStaggerTicks = 18f
          h.hp -= 3
          arcs.add(ArenaArcLine(playerX, playerY, h.x, h.y, Color(0xFFE53935), 20f))
          addSparkIfAllowed(h.x, h.y, Color(0xFF2979FF), 18f)
        }
        val iter = hostiles.iterator()
        while (iter.hasNext()) {
          val h = iter.next()
          if (h.hp <= 0) {
            iter.remove()
            onHostileDefeated(h, scoreMult)
          }
        }
        dashCd = 0f
        postCallout("WEB-ZIP SNARE SURGE! FOES WEBBED & DASH READY")
      }
      "stark_orbital" -> {
        val angle = atan2(aimY, aimX)
        for (offset in listOf(-0.07f, 0f, 0.07f)) {
          val a = angle + offset
          bolts.add(
            ArenaBolt(
              x = playerX,
              y = playerY,
              vx = cos(a) * 0.032f,
              vy = sin(a) * 0.032f,
              life = 68f,
              isSupercharged = true,
              pierceLeft = 6,
              damage = 7,
              color = Color(0xFF00E5FF),
            )
          )
        }
        hudShield = (hudShield + 40).coerceAtMost(loadout.maxShield)
        postCallout("STARK UNIBEAM OVERDRIVE! +40 ARMOR")
      }
      "empire_city_infamous" -> {
        val iter = hostiles.iterator()
        var drained = 0
        while (iter.hasNext() && drained < 8) {
          val h = iter.next()
          val dx = h.x - playerX
          val dy = h.y - playerY
          if (dx * dx + dy * dy <= 0.58f * 0.58f) {
            h.hp -= 5
            h.hitStaggerTicks = 15f
            arcs.add(ArenaArcLine(playerX, playerY, h.x, h.y, Color(0xFF00E5FF), 20f))
            drained++
            if (h.hp <= 0) {
              iter.remove()
              onHostileDefeated(h, scoreMult)
            }
          }
        }
        hudShield = (hudShield + drained * 5).coerceAtMost(loadout.maxShield)
        postCallout("RAY-SPHERE ARC DRAIN! +${drained * 5} BIO-CHARGE")
      }
      "aegis_starblade" -> {
        bolts.removeAll { it.isEnemyShot }
        for (i in 0 until hostiles.size) {
          val h = hostiles[i]
          val dx = h.x - playerX
          val dy = h.y - playerY
          val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
          if (d < 0.65f) {
            h.knockVelX += (dx / d) * 0.022f
            h.knockVelY += (dy / d) * 0.022f
            h.hitStaggerTicks = 25f
            h.hp -= 4
          }
        }
        val iter = hostiles.iterator()
        while (iter.hasNext()) {
          val h = iter.next()
          if (h.hp <= 0) {
            iter.remove()
            onHostileDefeated(h, scoreMult)
          }
        }
        postCallout("STARBLADE FORCE REPULSE!")
      }
      "ship_of_heroes_apotheosis" -> {
        bolts.removeAll { it.isEnemyShot }
        hudShield = (hudShield + 50).coerceAtMost(loadout.maxShield)
        for (i in 0 until hostiles.size) {
          val h = hostiles[i]
          val dx = h.x - playerX
          val dy = h.y - playerY
          val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
          h.knockVelX += (dx / d) * 0.014f
          h.knockVelY += (dy / d) * 0.014f
        }
        postCallout("APOTHEOSIS NOVA STRIKE & SQUAD SHIELD!")
      }
      "mr_crossover_nexus" -> {
        redEssenceEarned += 15L
        yellowEssenceEarned += 15L
        greenEssenceEarned += 15L
        combinedEssenceEarned += 15L
        val iter = hostiles.iterator()
        while (iter.hasNext()) {
          val h = iter.next()
          val dx = h.x - playerX
          val dy = h.y - playerY
          if (dx * dx + dy * dy <= 0.50f * 0.50f) {
            h.hp -= 5
            arcs.add(ArenaArcLine(playerX, playerY, h.x, h.y, Color(0xFFE040FB), 18f))
            if (h.hp <= 0) {
              iter.remove()
              onHostileDefeated(h, scoreMult)
            }
          }
        }
        postCallout("QUAD-ESSENCE PRISM NOVA! +15 ALL ESSENCE")
      }
      else -> {
        hudShield = (hudShield + 40).coerceAtMost(loadout.maxShield)
        stasisTicks = maxOf(stasisTicks, 120f)
        bolts.removeAll { it.isEnemyShot }
        postCallout("REALITY ANCHOR STABILIZED! (+40 ARMOR)")
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
      val blastX = playerX + cos(spreadAngle) * 0.20f
      val blastY = playerY + sin(spreadAngle) * 0.20f
      val blastRadiusSq = 0.22f * 0.22f
      val lethalDmg = (((spec.baseDamageOrEffect / 30f).toInt() + levelBonus / 4 + prestigeBonus) * eqTierMult * eqStarMult).roundToInt().coerceAtLeast(2)
      totalDmg += lethalDmg

      arcs.add(ArenaArcLine(playerX, playerY, blastX, blastY, Color(spec.defaultColorHex), 20f))
      for (i in 0 until 10) {
        val a = (i / 10f) * 2f * PI.toFloat()
        sparks.add(HitSpark(blastX + cos(a) * 0.04f, blastY + sin(a) * 0.04f, Color(spec.defaultColorHex), 25f))
      }

      val lethalDmgType = when {
        spec.id.contains("thermite", ignoreCase = true) || spec.id.contains("frag", ignoreCase = true) || spec.id.contains("c4", ignoreCase = true) -> ArenaDamageType.THERMAL
        spec.id.contains("cryo", ignoreCase = true) -> ArenaDamageType.CRYO
        spec.id.contains("bio", ignoreCase = true) || spec.id.contains("toxic", ignoreCase = true) -> ArenaDamageType.BIO
        else -> ArenaDamageType.THERMAL
      }
      val iter = hostiles.iterator()
      while (iter.hasNext()) {
        val h = iter.next()
        val dx = h.x - blastX
        val dy = h.y - blastY
        if (dx * dx + dy * dy <= blastRadiusSq) {
          applyDamageToHostile(h, lethalDmg, lethalDmgType)
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
    // Crisp 45% initial evade snap + smooth decaying kinetic dash velocity (never rigid teleport or watery drift)
    val snapDist = dashDist * 0.45f
    playerX += aimX * snapDist
    playerY += aimY * snapDist
    val impulse = dashDist * 0.135f
    dashVelX = aimX * impulse
    dashVelY = aimY * impulse
    arcs.add(ArenaArcLine(oldX, oldY, playerX, playerY, accentColor, 14f))

    val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L
    val outfitId = progression.equippedOutfitId
    if (loadout.hasClawEdge) {
      val clawDmg = 2 + loadout.clawTier
      val iter = hostiles.iterator()
      while (iter.hasNext()) {
        val h = iter.next()
        val dx = h.x - playerX
        val dy = h.y - playerY
        if (dx * dx + dy * dy <= 0.17f * 0.17f) {
          applyDamageToHostile(h, clawDmg, ArenaDamageType.KINETIC)
          h.hitStaggerTicks = 10f
          addSparkIfAllowed(h.x, h.y, Color(0xFFFF5252), 18f)
          if (h.hp <= 0) {
            iter.remove()
            onHostileDefeated(h, scoreMult)
          }
        }
      }
    } else if (outfitId.startsWith("batman") || world.id == "gotham_arkham") {
      // Original Grapnel Concussion Shock
      for (i in 0 until hostiles.size) {
        val h = hostiles[i]
        val dx = h.x - playerX
        val dy = h.y - playerY
        val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
        if (d <= 0.22f) {
          h.hitStaggerTicks = maxOf(h.hitStaggerTicks, 18f)
          h.knockVelX += (dx / d) * 0.008f
          h.knockVelY += (dy / d) * 0.008f
          addSparkIfAllowed(h.x, h.y, Color(0xFFFFD54F), 14f)
        }
      }
    } else if (outfitId.startsWith("spiderman") || world.id == "spiderverse_queens") {
      // Original Web-Snare Dodge
      for (i in 0 until hostiles.size) {
        val h = hostiles[i]
        val dx = h.x - playerX
        val dy = h.y - playerY
        if (dx * dx + dy * dy <= 0.22f * 0.22f) {
          h.cryoSlowTicks = maxOf(h.cryoSlowTicks, 75f)
          addSparkIfAllowed(h.x, h.y, Color(0xFFE53935), 14f)
        }
      }
    } else if (outfitId.startsWith("cole") || world.id == "empire_city_infamous") {
      // Original Static Step
      var arcHits = 0
      for (i in 0 until hostiles.size) {
        if (arcHits >= 2) break
        val h = hostiles[i]
        val dx = h.x - playerX
        val dy = h.y - playerY
        if (dx * dx + dy * dy <= 0.24f * 0.24f) {
          h.hp -= 1
          h.hitStaggerTicks = maxOf(h.hitStaggerTicks, 12f)
          arcs.add(ArenaArcLine(playerX, playerY, h.x, h.y, Color(0xFF00E5FF), 14f))
          arcHits++
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
    val scaledReward = (((base + hardcoreBonus) * effectiveMult) * gameplayRewardMultiplier).roundToInt().toLong().coerceAtLeast(1L)
    hudScore += scaledReward

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

    // Spawn Multi-Color Essence Orbs, Blast Shards, Perk Drinks & Blast Cores scaled by NG+ & Collection Difficulty!
    fun scaledDrop(raw: Long): Long = (raw * gameplayRewardMultiplier).roundToInt().toLong().coerceAtLeast(1L)
    val orbRoll = Random.nextFloat()
    when {
      h.isBoss -> {
        collectibles.add(ArenaCollectible(h.x, h.y, ArenaCollectibleKind.COMBINED_ORB, amount = scaledDrop(25L)))
        val core = BlastCoreColor.entries[Random.nextInt(BlastCoreColor.entries.size)]
        collectibles.add(ArenaCollectible(h.x + 0.04f, h.y, ArenaCollectibleKind.BLAST_CORE, subId = core.id, amount = 1L, color = Color(core.colorHex)))
        collectibles.add(ArenaCollectible(h.x - 0.04f, h.y, ArenaCollectibleKind.BLAST_SHARD, amount = scaledDrop(5L)))
      }
      orbRoll < 0.28f -> collectibles.add(ArenaCollectible(h.x, h.y, ArenaCollectibleKind.RED_ORB, amount = scaledDrop(18L)))
      orbRoll < 0.56f -> collectibles.add(ArenaCollectible(h.x, h.y, ArenaCollectibleKind.YELLOW_ORB, amount = scaledDrop(18L)))
      orbRoll < 0.84f -> collectibles.add(ArenaCollectible(h.x, h.y, ArenaCollectibleKind.GREEN_ORB, amount = scaledDrop(18L)))
      else -> collectibles.add(ArenaCollectible(h.x, h.y, ArenaCollectibleKind.BLAST_SHARD, amount = scaledDrop(3L)))
    }

    // Random Wonder Weapon Pickups
    val wonderBoost = progression.activePassVersion.wonderDropBonusMultiplier *
      (if (progression.levelCustom.wonderDropBoost) 2.2f else 1.0f) * loadout.wonderDropSkillMult
    val wonderChance = when {
      h.isBoss -> 1.0f
      h.isElite -> (0.38f * wonderBoost).coerceAtMost(0.95f)
      else -> (0.08f * wonderBoost).coerceAtMost(0.45f)
    }
    if (Random.nextFloat() < wonderChance && (!progression.collectionCustomMode.shouldSuppressCrateWonderDrops(progression.activeAreaId, progression.customGameMaps) || h.isBoss)) {
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
      ArenaCollectibleKind.CAT_EASTER_EGG -> {
        hudScore += 2500L
        postCallout("EASTER EGG FOUND: GOLDEN CAT LOOT CRATE UNLOCKED!")
      }
      ArenaCollectibleKind.CHARACTER_SHARD -> {
        val addShards = c.amount.coerceAtLeast(1L)
        characterShardsCollected += addShards
        blastShardsEarned += addShards * 4L
        if (c.subId.isNotBlank()) {
          recruitedOutfitIdsList.add(c.subId)
          val charName = MultiverseArsenalCatalog.outfitById(c.subId).nameWithVersion
          postCallout("CHARACTER SHARD +$addShards: ${charName.uppercase()}!")
        } else {
          postCallout("CHARACTER COLLECTION SHARD +$addShards ACQUIRED!")
        }
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
    if (progression.wonderCustom.amplifierId == "amp_prototype_tendril_harness") {
      hudShield = loadout.maxShield
    }
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
    // Slightly slower calibrated tactical pacing while preserving user speed multiplier ordering
    val pace = liveSettings.gameSpeedMultiplier.coerceIn(0.55f, 1.3f) * 0.88f
    val paceDt = dt * pace
    elapsedTicks++

    val moving = (joyDx * joyDx + joyDy * joyDy) > 0.0064f
    frameTick = (frameTick + (if (moving) 1.35f else 0.7f) * dt) % 60f

    val cdRate = if (modeId == "onslaught") dt * 1.35f else dt
    if (powerCd > 0f) powerCd = (powerCd - cdRate).coerceAtLeast(0f)
    if (consumableCd > 0f) consumableCd = (consumableCd - cdRate).coerceAtLeast(0f)
    if (interactCd > 0f) interactCd = (interactCd - cdRate).coerceAtLeast(0f)
    for (si in 0 until worldStations.size) {
      val st = worldStations[si]
      if (st.cooldownTicks > 0f) st.cooldownTicks = (st.cooldownTicks - dt).coerceAtLeast(0f)
    }
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

    // Active Elemental Hazards (Ice, Fire, Toxic, Electric, Windy) + Safe Rest Camps
    var insideCamp = false
    for (ri in 0 until restCamps.size) {
      val camp = restCamps[ri]
      val dx = playerX - camp.x
      val dy = playerY - camp.y
      if (dx * dx + dy * dy <= camp.radius * camp.radius) {
        insideCamp = true
        break
      }
    }
    if (hudInRestCamp != insideCamp) hudInRestCamp = insideCamp

    var activeHaz: ArenaHazardZone? = null
    for (hi in 0 until hazards.size) {
      val hz = hazards[hi]
      val dx = playerX - hz.x
      val dy = playerY - hz.y
      if (dx * dx + dy * dy <= hz.radius * hz.radius) {
        activeHaz = hz
        break
      }
    }
    val hazLabel = activeHaz?.kind?.label
    if (hudActiveHazardLabel != hazLabel) hudActiveHazardLabel = hazLabel

    if (activeHaz != null) {
      if (activeHaz.kind == ArenaHazardKind.WINDY) {
        playerX += activeHaz.windDx * dt
        playerY += activeHaz.windDy * dt
      }
      if (modeId != "sandbox" && dashInvulnTicks <= 0f && !insideCamp &&
        (activeHaz.kind == ArenaHazardKind.FIRE || activeHaz.kind == ArenaHazardKind.TOXIC || activeHaz.kind == ArenaHazardKind.ELECTRIC)
      ) {
        hazardTickAccum += dt
        if (hazardTickAccum >= 48f) {
          hazardTickAccum = 0f
          val hazDmg = if (activeHaz.kind == ArenaHazardKind.ELECTRIC) 4 else 3
          hudShield = (hudShield - hazDmg).coerceAtLeast(1)
          if (activeHaz.kind == ArenaHazardKind.ELECTRIC) {
            arcs.add(ArenaArcLine(activeHaz.x, activeHaz.y, playerX, playerY, activeHaz.kind.color, 12f))
          }
        }
      }
    }

    // Passive perk armor regeneration (x3 accelerated while resting in a Safe Camp!)
    if (hudShield < loadout.maxShield && (damageFlashTicks <= 0f || insideCamp)) {
      val assistMult = (if (liveSettings.assistShieldBoost) 1.55f else 1.0f) * (if (insideCamp) 3.2f else 1.0f)
      val toxicMult = if (activeHaz?.kind == ArenaHazardKind.TOXIC) 0.45f else 1.0f
      val customRegenMult = progression.collectionCustomMode.effectiveRegenMultFor(progression.activeAreaId, progression.customGameMaps)
      regenAccum += (loadout.passiveRegenPerSec + if (insideCamp) 8f else 0f) * assistMult * toxicMult * customRegenMult * dtSec
      if (regenAccum >= 1f) {
        val add = regenAccum.toInt()
        regenAccum -= add
        hudShield = (hudShield + add).coerceAtMost(loadout.maxShield)
      }
    }

    // Crisp Arcade Velocity & Friction Movement (grounded, snappy acceleration and fast stop—no liquid drift)
    val cryoMoveFactor = if (activeHaz?.kind == ArenaHazardKind.ICE) 0.78f else 1.0f
    val maxSpd = 0.0058f * loadout.moveBoost * cryoMoveFactor * liveSettings.moveSensitivity.coerceIn(0.6f, 1.6f)
    if (moving) {
      val len = sqrt(joyDx * joyDx + joyDy * joyDy).coerceAtLeast(0.001f)
      val nx = joyDx / len
      val ny = joyDy / len
      if (!touchAimActive) {
        aimX = nx
        aimY = ny
      }
      val targetVx = nx * maxSpd * len.coerceAtMost(1f)
      val targetVy = ny * maxSpd * len.coerceAtMost(1f)
      val accelLerp = (1f - kotlin.math.exp(-0.55f * dt)).coerceIn(0.25f, 0.85f)
      playerVelX += (targetVx - playerVelX) * accelLerp
      playerVelY += (targetVy - playerVelY) * accelLerp
    } else {
      val stopDamp = (1f - 0.36f * dt).coerceIn(0.15f, 0.92f)
      playerVelX *= stopDamp
      playerVelY *= stopDamp
      if (kotlin.math.abs(playerVelX) < 0.00005f) playerVelX = 0f
      if (kotlin.math.abs(playerVelY) < 0.00005f) playerVelY = 0f
    }

    // Integrate movement + smooth decaying dash kinetic impulse
    playerX += (playerVelX + dashVelX) * dt
    playerY += (playerVelY + dashVelY) * dt
    val dashDamp = (1f - 0.24f * dt).coerceIn(0.30f, 0.90f)
    dashVelX *= dashDamp
    dashVelY *= dashDamp
    if (kotlin.math.abs(dashVelX) < 0.00004f) dashVelX = 0f
    if (kotlin.math.abs(dashVelY) < 0.00004f) dashVelY = 0f

    // Tight damped camera tracking (crisp lock-on with subtle follow damping)
    val targetCamX = playerX - 0.5f
    val targetCamY = playerY - 0.5f
    val camFollow = (1f - kotlin.math.exp(-0.42f * dt)).coerceIn(0.22f, 0.65f)
    camX += (targetCamX - camX) * camFollow
    camY += (targetCamY - camY) * camFollow
    camX = camX.coerceIn(targetCamX - 0.14f, targetCamX + 0.14f)
    camY = camY.coerceIn(targetCamY - 0.14f, targetCamY + 0.14f)

    // Check Outer Rift Frontier roaming (beyond 0..10f boundaries) -> procedural high-tier crates!
    val outer = playerX < 0f || playerX > ARENA_WORLD_SIZE || playerY < 0f || playerY > ARENA_WORLD_SIZE
    if (hudInOuterFrontier != outer) hudInOuterFrontier = outer
    if (outer) {
      if (!enteredOuterFrontierOnce) {
        enteredOuterFrontierOnce = true
        postCallout("OUTER RIFT FRONTIER! BOUNDARIES REMOVED — HIGH-TIER LOOT ACTIVE")
      }
      frontierSpawnAccum += dt
      if (frontierSpawnAccum >= 300f) {
        frontierSpawnAccum = 0f
        if (lootCrates.count { !it.opened } < 10) {
          val ang = Random.nextFloat() * 2f * PI.toFloat()
          val rarityRoll = Random.nextFloat()
          val rarity = when {
            rarityRoll < 0.25f -> com.example.data.model.CharacterRarity.MYTHIC
            rarityRoll < 0.60f -> com.example.data.model.CharacterRarity.EXOTIC
            else -> com.example.data.model.CharacterRarity.LEGENDARY
          }
          lootCrates.add(
            ArenaLootCrate(
              id = "crate_dyn_${elapsedTicks}",
              x = playerX + cos(ang) * 0.45f,
              y = playerY + sin(ang) * 0.45f,
              rarity = rarity,
              fromHazard = true,
            )
          )
        }
      }
    }

    // Auto-collect closely touched Cat Easter Eggs & Loot Crates + compute HUD Interact Prompt
    for (ci in 0 until catEasterEggs.size) {
      val cat = catEasterEggs[ci]
      if (!cat.found) {
        val dx = cat.x - playerX
        val dy = cat.y - playerY
        if (dx * dx + dy * dy <= 0.075f * 0.075f) discoverCat(cat)
      }
    }
    for (li in 0 until lootCrates.size) {
      val crate = lootCrates[li]
      if (!crate.opened) {
        val dx = crate.x - playerX
        val dy = crate.y - playerY
        if (dx * dx + dy * dy <= 0.075f * 0.075f) openLootCrate(crate)
      }
    }
    var nearPrompt: String? = null
    if (catEasterEggs.any { !it.found && (it.x - playerX) * (it.x - playerX) + (it.y - playerY) * (it.y - playerY) <= 0.16f * 0.16f }) {
      nearPrompt = "PET CAT"
    } else if (lootCrates.any { !it.opened && (it.x - playerX) * (it.x - playerX) + (it.y - playerY) * (it.y - playerY) <= 0.16f * 0.16f }) {
      nearPrompt = "OPEN CRATE"
    } else {
      val st = worldStations.firstOrNull { (it.x - playerX) * (it.x - playerX) + (it.y - playerY) * (it.y - playerY) <= 0.17f * 0.17f }
      if (st != null) {
        nearPrompt = when (st.kind) {
          ArenaStationKind.PACK_A_PUNCH -> "PACK MACHINE"
          ArenaStationKind.PERK_MACHINE -> "DRINK PERK"
          ArenaStationKind.AMMO_CRATE -> "AMMO CRATE"
        }
      } else if (insideCamp) {
        nearPrompt = "REST CAMP"
      }
    }
    if (hudNearbyPrompt != nearPrompt) hudNearbyPrompt = nearPrompt

    // Vacuum Bot companion orbit + auto suction of nearby pickups/drops/orbs
    val vacAngle = elapsedTicks * 0.055f
    vacuumBotX = playerX + cos(vacAngle) * 0.068f
    vacuumBotY = playerY + sin(vacAngle) * 0.068f

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
            val tDmgType = if (trap.kind == ArenaTrapKind.ELECTRIC) ArenaDamageType.ENERGY else ArenaDamageType.THERMAL
            applyDamageToHostile(h, 2 + loadout.trapDamageBonus / 2, tDmgType)
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
          targetX = playerX + cos(bot.roamAngle + bi) * 0.16f
          targetY = playerY + sin(bot.roamAngle + bi) * 0.16f
        } else {
          targetX = playerX + cos(bot.roamAngle + bi) * 0.38f
          targetY = playerY + sin(bot.roamAngle + bi) * 0.38f
        }
        val moveDx = targetX - bot.x
        val moveDy = targetY - bot.y
        bot.x += moveDx * 0.03f * dt
        bot.y += moveDy * 0.03f * dt

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
              damageType = if (bot.isAllied) ArenaDamageType.ENERGY else ArenaDamageType.KINETIC,
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
          val customIncomingMult = progression.collectionCustomMode.effectiveIncomingDamageMultFor(progression.activeAreaId, progression.customGameMaps)
          val shotDmg = ((if (matchRules.friendlyFire) rawShot * 1.35f else rawShot.toFloat()) * customIncomingMult).roundToInt().coerceAtLeast(1)
          hudShield = (hudShield - shotDmg).coerceAtLeast(0)
          damageFlashTicks = if (liveSettings.reduceFlashes) 0f else 12f
          if (hudShield <= 0) hudRunEnded = true
        }
      }
      if (b.life <= 0f || kotlin.math.abs(b.x - playerX) > 2.2f || kotlin.math.abs(b.y - playerY) > 2.2f) {
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
        val dy = playerY - (ARENA_CENTER - 1.25f)
        if (dx * dx + dy * dy < 0.12f * 0.12f) {
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
        if (h.hitStaggerTicks > 0f) h.hitStaggerTicks = (h.hitStaggerTicks - dt).coerceAtLeast(0f)
        h.x += h.knockVelX * dt
        h.y += h.knockVelY * dt
        h.knockVelX *= (1f - 0.28f * dt).coerceIn(0.2f, 0.9f)
        h.knockVelY *= (1f - 0.28f * dt).coerceIn(0.2f, 0.9f)
        val slowMult = (if (h.cryoSlowTicks > 0f) 0.42f else 1.0f) *
          (if (insideCamp) 0.45f else 1.0f) *
          (if (h.hitStaggerTicks > 0f) 0.25f else 1.0f)
        val dx = playerX - h.x
        val dy = playerY - h.y
        val dSq = dx * dx + dy * dy
        val d = sqrt(dSq).coerceAtLeast(0.001f)
        h.x += (dx / d) * h.speed * slowMult * paceDt
        h.y += (dy / d) * h.speed * slowMult * paceDt

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
            val customIncomingMult = progression.collectionCustomMode.effectiveIncomingDamageMultFor(progression.activeAreaId, progression.customGameMaps)
            val preGuardDmg = if (liveSettings.assistShieldBoost) (baseDmg * 2 / 3).coerceAtLeast(4) else baseDmg
            val dmg = (preGuardDmg * customIncomingMult).roundToInt().coerceAtLeast(1)
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
          val hitLanded = applyDamageToHostile(h, dmg, b.damageType)
          if (hitLanded) {
            val bSpd = sqrt(b.vx * b.vx + b.vy * b.vy).coerceAtLeast(0.001f)
            val push = if (h.isBoss) 0.0015f else 0.0042f
            h.knockVelX += (b.vx / bSpd) * push
            h.knockVelY += (b.vy / bSpd) * push
            h.hitStaggerTicks = maxOf(h.hitStaggerTicks, if (h.isBoss) 2f else 5f)
            addSparkIfAllowed(h.x, h.y, b.color, 14f)

            val siphon = loadout.activePassVersion.glorySiphonPerBossHit
            if (siphon > 0 && (h.isBoss || h.isElite)) {
              hudShield = (hudShield + siphon).coerceAtMost(loadout.maxShield)
            }

            // Authentic Weapon Attachments
            val wc = progression.weaponCustom
            if (wc.underbarrelId == "arkham_grapnel_launcher") {
              h.hitStaggerTicks = maxOf(h.hitStaggerTicks, 35f)
            }
            if (wc.underbarrelId == "infamous_kinetic_pulse") {
              h.knockVelX += (b.vx / bSpd) * 0.010f
              h.knockVelY += (b.vy / bSpd) * 0.010f
            }
            if (wc.underbarrelId == "prototype_whip_spool" || wc.rearGripId == "prototype_tendril_wrap" || wc.muzzleId == "prototype_tendril_muzzle" || wc.opticId == "prototype_viral_sight") {
              hudShield = (hudShield + 1).coerceAtMost(loadout.maxShield)
            }
            if (wc.barrelId == "arkham_disruptor_barrel" || wc.muzzleId == "abyss_eldritch_muzzle") {
              if (h.armor > 0) h.armor = (h.armor - 2).coerceAtLeast(0)
            }

            // Authentic Wonder Weapon Attachments
            val wonC = progression.wonderCustom
            if (wonC.coreId == "core_abyss_eldritch") {
              hudShield = (hudShield + 3).coerceAtMost(loadout.maxShield)
            }
            if (wonC.coreId == "core_arkham_freeze" || wonC.lensId == "lens_cryo_subzero") {
              h.cryoSlowTicks = 160f
            }
            if (wonC.coreId == "core_infamous_ray_core" || wonC.amplifierId == "amp_cod_wunder_grid") {
              var extraChains = 0
              for (j in 0 until hostiles.size) {
                if (extraChains >= 4) break
                val other = hostiles[j]
                if (other === h || other.hp <= 0) continue
                val ox = other.x - h.x
                val oy = other.y - h.y
                if (ox * ox + oy * oy <= 0.28f * 0.28f) {
                  applyDamageToHostile(other, 3, ArenaDamageType.ENERGY)
                  arcs.add(ArenaArcLine(h.x, h.y, other.x, other.y, Color(0xFF00E5FF), 14f))
                  extraChains++
                }
              }
            }

            if (loadout.ammoModId == "cryo_stasis" || activeWonderWeapon == WonderWeaponKind.CHRONO_ZAT || activeWonderWeapon == WonderWeaponKind.GATE_HORIZON_CANNON) {
              h.cryoSlowTicks = if (activeWonderWeapon == WonderWeaponKind.CHRONO_ZAT || activeWonderWeapon == WonderWeaponKind.GATE_HORIZON_CANNON) 150f else 90f
            }
            if (activeWonderWeapon == WonderWeaponKind.BLACKLIGHT_WHIP || activeWonderWeapon == WonderWeaponKind.MERCER_BIOCLAW_SWARM) {
              hudShield = (hudShield + if (activeWonderWeapon == WonderWeaponKind.MERCER_BIOCLAW_SWARM) 3 else 2).coerceAtMost(loadout.maxShield)
            }
            if (activeWonderWeapon == WonderWeaponKind.LIBERTY_WUNDERWAFFE ||
              activeWonderWeapon == WonderWeaponKind.RAY_SPHERE_OVERDRIVE ||
              (loadout.ammoModId == "arc_chain" && playerShotCounter % 3 == 0)
            ) {
              var chains = 0
              val maxChains = when (activeWonderWeapon) {
                WonderWeaponKind.RAY_SPHERE_OVERDRIVE -> 5
                WonderWeaponKind.LIBERTY_WUNDERWAFFE -> 4
                else -> 2
              }
              for (j in 0 until hostiles.size) {
                if (chains >= maxChains) break
                val other = hostiles[j]
                if (other === h || other.hp <= 0) continue
                val ox = other.x - h.x
                val oy = other.y - h.y
                if (ox * ox + oy * oy <= 0.25f * 0.25f) {
                  applyDamageToHostile(other, 2, ArenaDamageType.ENERGY)
                  arcs.add(ArenaArcLine(h.x, h.y, other.x, other.y, Color(activeWonderWeapon?.accentHex ?: 0xFF00E5FFL), 14f))
                  chains++
                }
              }
            }
            if (activeWonderWeapon == WonderWeaponKind.CRUCIBLE_BFG ||
              activeWonderWeapon == WonderWeaponKind.RAY_NOVA_MK3 ||
              activeWonderWeapon == WonderWeaponKind.GATE_HORIZON_CANNON
            ) {
              val splashRadiusSq = if (activeWonderWeapon == WonderWeaponKind.RAY_NOVA_MK3) 0.14f * 0.14f else 0.22f * 0.22f
              val splashDmg = if (activeWonderWeapon == WonderWeaponKind.RAY_NOVA_MK3) 2 else 3
              val splashType = if (activeWonderWeapon == WonderWeaponKind.CRUCIBLE_BFG) ArenaDamageType.THERMAL else b.damageType
              for (j in 0 until hostiles.size) {
                val other = hostiles[j]
                if (other === h || other.hp <= 0) continue
                val ox = other.x - h.x
                val oy = other.y - h.y
                if (ox * ox + oy * oy <= splashRadiusSq) {
                  applyDamageToHostile(other, splashDmg, splashType)
                  addSparkIfAllowed(other.x, other.y, Color(activeWonderWeapon!!.accentHex), 12f)
                }
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
      hudBossArmorRatio = if (activeBoss.maxArmor > 0) (activeBoss.armor.toFloat() / activeBoss.maxArmor).coerceIn(0f, 1f) else 0f
      hudBossImmuneSummary = if (activeBoss.immuneTypes.isEmpty()) null else activeBoss.immuneTypes.joinToString("/") { it.shortTag }
    } else if (hudBossName != null) {
      hudBossName = null
      hudBossHpRatio = 0f
      hudBossArmorRatio = 0f
      hudBossImmuneSummary = null
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
        when (modeId) {
          "campaign" -> {
            combinedEssenceEarned += 15L
            postCallout("CAMPAIGN ANCHOR SECURED! ROUND ${formatRoundRoman(hudWave)} INCOMING (+15 COMB ESS)")
          }
          "conduit_ctf" -> {
            blastShardsEarned += 4L
            traps.forEach { it.cooldownTicks = 0f }
            postCallout("CONDUIT RELAY CHARGED! TRAPS ARMED (+4 SHARDS)")
          }
          "hero_patrol" -> {
            characterShardsCollected += 1L
            overdriveTicks = maxOf(overdriveTicks, 180f)
            postCallout("SECTOR PATROL SECURED! +1 HERO SHARD • ROUND ${formatRoundRoman(hudWave)}")
          }
          else -> {
            postCallout("ROUND ${formatRoundRoman(hudWave)} INCOMING")
          }
        }
        spawnWave(hudWave)
      }
    }

    renderNonce++
  }
}

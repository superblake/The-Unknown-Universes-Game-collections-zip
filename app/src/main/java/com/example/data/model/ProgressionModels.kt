package com.example.data.model

import androidx.compose.runtime.Immutable

enum class WonderWeaponKind(
  val id: String,
  val displayName: String,
  val shortTag: String,
  val description: String,
  val accentHex: Long,
  val durationTicks: Float,
  val baseDamage: Int,
) {
  RAY_NOVA_MK3(
    id = "ww_ray_mk3",
    displayName = "Ray-Nova Mk. III",
    shortTag = "RAY-MK3",
    description = "Twin piercing crimson-plasma orbs with explosive splash impact.",
    accentHex = 0xFFFF3D71,
    durationTicks = 1320f,
    baseDamage = 4,
  ),
  LIBERTY_WUNDERWAFFE(
    id = "ww_wunderwaffe",
    displayName = "Liberty Wunder-Arc",
    shortTag = "DG-ARC",
    description = "High-voltage bolts chain arc lightning to up to 4 nearby hostiles.",
    accentHex = 0xFF00E5FF,
    durationTicks = 1200f,
    baseDamage = 3,
  ),
  BLACKLIGHT_WHIP(
    id = "ww_blacklight_whip",
    displayName = "Blacklight Tendril Whip",
    shortTag = "WHIP",
    description = "Infinitely piercing crimson bio-lance that siphons +2 Shield per hit.",
    accentHex = 0xFFE040FB,
    durationTicks = 1200f,
    baseDamage = 3,
  ),
  CRUCIBLE_BFG(
    id = "ww_crucible_bfg",
    displayName = "Crucible BFG-9000",
    shortTag = "BFG-9K",
    description = "Superheated molten core that melts bosses and vaporizes surrounding waves.",
    accentHex = 0xFF00E676,
    durationTicks = 1080f,
    baseDamage = 6,
  ),
  CHRONO_ZAT(
    id = "ww_zat_stasis",
    displayName = "Chrono Zat'nik'tel",
    shortTag = "ZAT-SG",
    description = "Stargate energy arcs freeze struck enemies in temporal stasis.",
    accentHex = 0xFFFFD54F,
    durationTicks = 1260f,
    baseDamage = 3,
  ),
  STARBLADE_BOOMERANG(
    id = "ww_starblade_saber",
    displayName = "Twin Starblade Throw",
    shortTag = "SABER",
    description = "360-degree radial cosmic saber fan that shreds incoming rushes.",
    accentHex = 0xFF448AFF,
    durationTicks = 1200f,
    baseDamage = 4,
  ),
}

@Immutable
data class WeaponSpec(
  val id: String,
  val name: String,
  val universeOrigin: String,
  val summary: String,
  val baseDamage: Int,
  val baseCadenceTicks: Float,
  val baseProjectileSpeed: Float,
  val baseSpreadCount: Int,
  val basePierce: Int,
  val defaultTracerHex: Long,
)

@Immutable
data class WeaponCustomization(
  val equippedWeaponId: String = "wpn_pulse_carbine",
  val camoId: String = "cyber_matte",
  val opticId: String = "holo_cyan",
  val barrelId: String = "standard_barrel",
  val ammoModId: String = "standard_core",
  val tracerColorHex: Long = 0xFF00E5FF,
)

@Immutable
data class CharacterCustomization(
  val suitThemeId: String = "vanguard_cyber",
  val visorStyleId: String = "cyan_visor",
  val auraTrailId: String = "none",
  val cloakEnabled: Boolean = true,
  val titleBadge: String = "Multiverse Operative",
  val primaryArmorHex: Long = 0xFF00E5FF,
  val accentArmorHex: Long = 0xFFFFAB40,
)

@Immutable
data class LevelCustomization(
  val skyThemeId: String = "default_sector",
  val gridStyleId: String = "tactical_square",
  val enemyDensityMultiplier: Float = 1.0f,
  val wonderDropBoost: Boolean = false,
  val critSurgeMutator: Boolean = false,
  val instagibMutator: Boolean = false,
  val lowGravityDash: Boolean = false,
  val bossRushMutator: Boolean = false,
  val floorAccentHex: Long = 0xFF00E5FF,
)

enum class BattlePassVersion(
  val id: String,
  val title: String,
  val subtitle: String,
  val unlockCostEssence: Long,
  val accentHex: Long,
  val gameplayEffectSummary: String,
  val bonusShield: Int,
  val fireRateMultiplier: Float,
  val cooldownMultiplier: Float,
  val moveSpeedBonus: Float,
  val essenceMultiplier: Float,
  val wonderDropBonusMultiplier: Float,
  val glorySiphonPerBossHit: Int,
) {
  STANDARD_SECTOR_OPS(
    id = "bp_standard",
    title = "V1: Sector Ops Free Pass",
    subtitle = "Balanced SHD containment track — Free for all Operators",
    unlockCostEssence = 0L,
    accentHex = 0xFF00E5FF,
    gameplayEffectSummary = "+15% Bonus Match Essence & Standard XP Progression",
    bonusShield = 0,
    fireRateMultiplier = 1.0f,
    cooldownMultiplier = 1.0f,
    moveSpeedBonus = 0.0f,
    essenceMultiplier = 1.15f,
    wonderDropBonusMultiplier = 1.0f,
    glorySiphonPerBossHit = 0,
  ),
  BLACKCELL_OVERDRIVE(
    id = "bp_blackcell",
    title = "V2: Blackcell Overdrive",
    subtitle = "Elite Gold & Obsidian Syndicate Edition",
    unlockCostEssence = 600L,
    accentHex = 0xFFFFD54F,
    gameplayEffectSummary = "+18% Fire Rate, +35% Wonder Weapon Drop Chance & +25% BP XP",
    bonusShield = 15,
    fireRateMultiplier = 1.18f,
    cooldownMultiplier = 0.95f,
    moveSpeedBonus = 0.05f,
    essenceMultiplier = 1.25f,
    wonderDropBonusMultiplier = 1.35f,
    glorySiphonPerBossHit = 0,
  ),
  CHRONO_MULTIVERSE(
    id = "bp_chrono",
    title = "V3: Chrono-Vault Pass",
    subtitle = "Space & Time 7-Chevron Temporal Edition",
    unlockCostEssence = 750L,
    accentHex = 0xFF7C4DFF,
    gameplayEffectSummary = "-25% Power/Stim Cooldowns & Extended Wonder Weapon Duration",
    bonusShield = 20,
    fireRateMultiplier = 1.08f,
    cooldownMultiplier = 0.75f,
    moveSpeedBonus = 0.08f,
    essenceMultiplier = 1.20f,
    wonderDropBonusMultiplier = 1.20f,
    glorySiphonPerBossHit = 0,
  ),
  HELLBREAKER_SUPREME(
    id = "bp_hellbreaker",
    title = "V4: Hellbreaker Supreme",
    subtitle = "Mythic Crucible & Bio-Claw Ultimate Edition",
    unlockCostEssence = 900L,
    accentHex = 0xFFFF3D71,
    gameplayEffectSummary = "+40 Max Shield, +15% Move Speed & +3 Armor Siphon vs Elites/Bosses",
    bonusShield = 40,
    fireRateMultiplier = 1.10f,
    cooldownMultiplier = 0.90f,
    moveSpeedBonus = 0.15f,
    essenceMultiplier = 1.30f,
    wonderDropBonusMultiplier = 1.25f,
    glorySiphonPerBossHit = 3,
  ),
}

@Immutable
data class BattlePassTierReward(
  val tier: Int,
  val bpXpRequired: Long,
  val title: String,
  val detail: String,
  val rewardEssence: Long,
  val unlockTag: String,
)

@Immutable
data class ChallengeSpec(
  val id: String,
  val title: String,
  val description: String,
  val category: String,
  val target: Long,
  val rewardEssence: Long,
  val rewardPlayerXp: Long,
  val rewardBpXp: Long,
)

@Immutable
data class ChallengeState(
  val spec: ChallengeSpec,
  val progress: Long,
  val claimed: Boolean,
) {
  val completed: Boolean get() = progress >= spec.target
  val ratio: Float get() = (progress.toFloat() / spec.target.coerceAtLeast(1L)).coerceIn(0f, 1f)
}

@Immutable
data class MatchProgressionReport(
  val eliminations: Long,
  val bossEliminations: Long,
  val waveReached: Long,
  val wonderPickupsCollected: Int,
  val weaponIdUsed: String,
  val extracted: Boolean,
  val weaponLevelUps: Int = 0,
)

@Immutable
data class OperatorProgressionState(
  val playerXp: Long = 0L,
  val weaponXpMap: Map<String, Long> = mapOf("wpn_pulse_carbine" to 0L),
  val weaponCustom: WeaponCustomization = WeaponCustomization(),
  val characterCustom: CharacterCustomization = CharacterCustomization(),
  val levelCustom: LevelCustomization = LevelCustomization(),
  val activePassVersion: BattlePassVersion = BattlePassVersion.STANDARD_SECTOR_OPS,
  val unlockedPassVersions: Set<BattlePassVersion> = setOf(BattlePassVersion.STANDARD_SECTOR_OPS),
  val battlePassXp: Long = 0L,
  val claimedPassTiers: Set<String> = emptySet(), // key = "${version.id}_$tier"
  val challengeProgress: Map<String, Long> = emptyMap(),
  val claimedChallenges: Set<String> = emptySet(),
  val wonderWeaponsPickedTotal: Long = 0L,
) {
  val playerLevel: Int
    get() = ProgressionCatalog.levelForXp(playerXp)

  val playerRankTitle: String
    get() = ProgressionCatalog.rankTitleForLevel(playerLevel)

  fun weaponXp(weaponId: String): Long = weaponXpMap[weaponId] ?: 0L

  fun weaponLevel(weaponId: String): Int = ProgressionCatalog.weaponLevelForXp(weaponXp(weaponId))
}

object ProgressionCatalog {

  val weapons: List<WeaponSpec> = listOf(
    WeaponSpec(
      id = "wpn_pulse_carbine",
      name = "SHD Pulse Carbine",
      universeOrigin = "Sector Zero",
      summary = "Balanced high-velocity energy carbine with crisp handling.",
      baseDamage = 1,
      baseCadenceTicks = 10.5f,
      baseProjectileSpeed = 0.020f,
      baseSpreadCount = 1,
      basePierce = 0,
      defaultTracerHex = 0xFF00E5FF,
    ),
    WeaponSpec(
      id = "wpn_plasma_scatter",
      name = "Blacklight Scattergun",
      universeOrigin = "Blacklight Conduit",
      summary = "Wide 3-bolt plasma spread for crowd control & close encounters.",
      baseDamage = 1,
      baseCadenceTicks = 13.5f,
      baseProjectileSpeed = 0.018f,
      baseSpreadCount = 3,
      basePierce = 0,
      defaultTracerHex = 0xFF7C4DFF,
    ),
    WeaponSpec(
      id = "wpn_starblade_lancer",
      name = "Starblade Nova Lancer",
      universeOrigin = "Aegis & Starblade",
      summary = "Piercing twin-core rail lance that slices through lined hostiles.",
      baseDamage = 2,
      baseCadenceTicks = 12.0f,
      baseProjectileSpeed = 0.026f,
      baseSpreadCount = 1,
      basePierce = 2,
      defaultTracerHex = 0xFFE040FB,
    ),
    WeaponSpec(
      id = "wpn_crucible_cannon",
      name = "Hellbreaker Heavy Cannon",
      universeOrigin = "Hellbreaker Citadel",
      summary = "Heavy molten slugs engineered to crack Mechs & Hydra bosses.",
      baseDamage = 3,
      baseCadenceTicks = 15.0f,
      baseProjectileSpeed = 0.019f,
      baseSpreadCount = 1,
      basePierce = 1,
      defaultTracerHex = 0xFFFF5252,
    ),
    WeaponSpec(
      id = "wpn_chrono_repeater",
      name = "Chrono-Vault Repeater",
      universeOrigin = "Chrono-Vault",
      summary = "Rapid-cycle temporal repeater firing twin golden bolts at high cadence.",
      baseDamage = 1,
      baseCadenceTicks = 8.0f,
      baseProjectileSpeed = 0.023f,
      baseSpreadCount = 2,
      basePierce = 0,
      defaultTracerHex = 0xFFFFD54F,
    ),
  )

  fun weaponById(id: String): WeaponSpec =
    weapons.firstOrNull { it.id == id } ?: weapons.first()

  val camos: List<Pair<String, String>> = listOf(
    "cyber_matte" to "Cyber Matte",
    "molten_damascus" to "Molten Damascus",
    "pack_tier_gold" to "Pack-a-Tier Gold",
    "blacklight_neon" to "Blacklight Neon",
    "chrono_chrome" to "Chrono Chrome",
  )

  val optics: List<Pair<String, String>> = listOf(
    "iron_sight" to "Precision Ring",
    "holo_cyan" to "Holo Diamond",
    "isac_thermal" to "ISAC Crosshair",
    "starblade_reflex" to "Twin-Core Reflex",
  )

  val barrels: List<Pair<String, String>> = listOf(
    "standard_barrel" to "Standard Barrel",
    "overclocked_rifling" to "Overclocked (+18% Bolt Speed)",
    "spread_choke" to "Spread Choke (+1 Extra Bolt)",
    "heavy_bore" to "Heavy Bore (+1 Base Damage)",
  )

  val ammoMods: List<Pair<String, String>> = listOf(
    "standard_core" to "Standard Kinetic",
    "cryo_stasis" to "Cryo-Freeze (Slows Enemies)",
    "arc_chain" to "Arc Wire (Chain Lightning)",
    "vampiric_bio" to "Blacklight Leech (+Shield on Kill)",
  )

  val suitThemes: List<Triple<String, String, Pair<Long, Long>>> = listOf(
    Triple("vanguard_cyber", "Vanguard Cyber", 0xFF00E5FF to 0xFFFFAB40),
    Triple("blackcell_gold", "Blackcell Gold", 0xFFFFD54F to 0xFFFFFFFF),
    Triple("blacklight_red", "Blacklight Crimson", 0xFFFF3D71 to 0xFF7C4DFF),
    Triple("chrono_emerald", "Chrono Emerald", 0xFF00E676 to 0xFF448AFF),
    Triple("starblade_royal", "Starblade Void", 0xFFE040FB to 0xFF00E5FF),
  )

  val visorStyles: List<Pair<String, Long>> = listOf(
    "cyan_visor" to 0xFF00E5FF,
    "amber_isac" to 0xFFFF8F00,
    "crimson_fury" to 0xFFFF3D71,
    "gold_prime" to 0xFFFFD54F,
    "emerald_optic" to 0xFF00E676,
  )

  val auraTrails: List<Pair<String, String>> = listOf(
    "none" to "Clean Silhouette",
    "pulse_ring" to "Overdrive Ring",
    "chrono_spark" to "Chrono Stasis Halo",
    "infernal_flare" to "Crucible Ember Ring",
  )

  val skyThemes: List<Pair<String, String>> = listOf(
    "default_sector" to "Sector Default",
    "blood_moon" to "Blood Moon Red-Zone",
    "cryo_tundra" to "Sub-Zero Tundra",
    "golden_nexus" to "Golden Gate Nexus",
    "neon_void" to "Cyber Neon Void",
  )

  val gridStyles: List<Pair<String, String>> = listOf(
    "tactical_square" to "Tactical Grid",
    "hex_vault" to "Wide Vault Grid",
    "minimal_clean" to "Clean Floor",
  )

  val challenges: List<ChallengeSpec> = listOf(
    ChallengeSpec(
      id = "ch_hostiles_40",
      title = "Containment Breaker",
      description = "Eliminate 40 hostiles across any Multiverse Arena deployment.",
      category = "COMBAT",
      target = 40L,
      rewardEssence = 350L,
      rewardPlayerXp = 450L,
      rewardBpXp = 350L,
    ),
    ChallengeSpec(
      id = "ch_bosses_2",
      title = "Hydra & Mech Slayer",
      description = "Defeat 2 Lore Bosses (Water Hydra, Killer Croc, or Assault Mech).",
      category = "BOSS",
      target = 2L,
      rewardEssence = 500L,
      rewardPlayerXp = 600L,
      rewardBpXp = 500L,
    ),
    ChallengeSpec(
      id = "ch_wonder_2",
      title = "Mystery Wonder Hunter",
      description = "Pick up 2 Random Wonder Weapon crates during Arena matches.",
      category = "LOOT",
      target = 2L,
      rewardEssence = 400L,
      rewardPlayerXp = 400L,
      rewardBpXp = 400L,
    ),
    ChallengeSpec(
      id = "ch_wave_4",
      title = "Deep Sector Survivor",
      description = "Reach Wave 4+ or complete a DMZ Exfiltration.",
      category = "SURVIVAL",
      target = 1L,
      rewardEssence = 450L,
      rewardPlayerXp = 500L,
      rewardBpXp = 450L,
    ),
    ChallengeSpec(
      id = "ch_armory_custom",
      title = "Master Gunsmith",
      description = "Customize a Weapon or Character loadout inside the Armory.",
      category = "ARMORY",
      target = 1L,
      rewardEssence = 300L,
      rewardPlayerXp = 350L,
      rewardBpXp = 300L,
    ),
  )

  fun tiersForVersion(version: BattlePassVersion): List<BattlePassTierReward> = when (version) {
    BattlePassVersion.STANDARD_SECTOR_OPS -> listOf(
      BattlePassTierReward(1, 0L, "SHD Matte Finish", "Unlocks Sector Zero standard operator trim & +15% Essence", 200L, "CAMO"),
      BattlePassTierReward(2, 300L, "Pulse Sight Reticle", "Precision Holo crosshair & bonus Essence cache", 250L, "OPTIC"),
      BattlePassTierReward(3, 700L, "Quarantine XP Surge", "+400 Weapon XP injection for your active primary", 300L, "WPN XP"),
      BattlePassTierReward(4, 1200L, "Aegis Emerald Tracer", "Unlocks bright emerald tracer bolts & badge", 350L, "TRACER"),
      BattlePassTierReward(5, 1800L, "Division Tech Cache", "Boosts Mystery Wonder Weapon salvage rewards", 450L, "CACHE"),
      BattlePassTierReward(6, 2500L, "Vanguard Sentinel Master", "Prestige Operator Emblem + 700 Bonus Essence", 700L, "PRESTIGE"),
    )
    BattlePassVersion.BLACKCELL_OVERDRIVE -> listOf(
      BattlePassTierReward(1, 0L, "Blackcell Obsidian Suit", "Instantly grants Obsidian & Gold armor trim + +18% Fire Rate", 250L, "SUIT"),
      BattlePassTierReward(2, 350L, "Molten Damascus Camo", "Blackcell animated gold-vein finish & tracer color", 350L, "CAMO"),
      BattlePassTierReward(3, 800L, "Overclocked Syndicate", "Boosts Wonder Crate spawn rate +35% in all modes", 450L, "PERK"),
      BattlePassTierReward(4, 1300L, "Gilded BFG Munitions", "+500 Weapon XP + Blackcell Heavy Bore tuning", 500L, "BLUEPRINT"),
      BattlePassTierReward(5, 1900L, "Syndicate Golden Crown", "Unlocks Gold Prime visor & aura ring flare", 650L, "VISOR"),
      BattlePassTierReward(6, 2600L, "Blackcell Overlord Tier", "Ultimate Syndicate Title + 900 Bonus Essence", 900L, "ULTIMATE"),
    )
    BattlePassVersion.CHRONO_MULTIVERSE -> listOf(
      BattlePassTierReward(1, 0L, "Time-Warden Regalia", "Activates -25% Power/Stim Cooldowns in Arena", 250L, "PASSIVE"),
      BattlePassTierReward(2, 350L, "Cryo-Stasis Munitions", "Unlocks Chrono Freeze bolt coloration & stasis aura", 350L, "AMMO"),
      BattlePassTierReward(3, 800L, "7-Chevron Gate Floor", "Unlocks Cosmic Nexus Arena floor & grid styling", 450L, "LEVEL"),
      BattlePassTierReward(4, 1300L, "Librarian Artifact Surge", "+500 Weapon XP + extended Wonder Weapon duration", 550L, "WPN XP"),
      BattlePassTierReward(5, 1900L, "Chrono Zat Priority", "Higher chance to pull Chrono Zat'nik'tel pickups", 650L, "WONDER"),
      BattlePassTierReward(6, 2600L, "Space & Time Sovereign", "Sovereign Temporal Banner + 950 Bonus Essence", 950L, "PRESTIGE"),
    )
    BattlePassVersion.HELLBREAKER_SUPREME -> listOf(
      BattlePassTierReward(1, 0L, "Hellbreaker Plate", "Activates +40 Max Shield & +15% Move Speed immediately", 300L, "ARMOR"),
      BattlePassTierReward(2, 400L, "Blood Moon Arena Kit", "Unlocks Crimson Crucible sky/floor theme & tracers", 400L, "LEVEL"),
      BattlePassTierReward(3, 900L, "Vampiric Glory Siphon", "Hits vs Bosses/Elites siphon +3 Shield live in Arena", 500L, "SIPHON"),
      BattlePassTierReward(4, 1450L, "Adamant Claw Temper", "+600 Weapon XP + Crucible Ember armor glow", 600L, "AURA"),
      BattlePassTierReward(5, 2050L, "BFG-9000 Arsenal Lock", "Increases Crucible BFG-9000 Wonder drop priority", 750L, "WONDER"),
      BattlePassTierReward(6, 2800L, "Apex Warlord Ascension", "Hellbreaker Supreme Emblem + 1100 Bonus Essence", 1100L, "SUPREME"),
    )
  }

  fun levelForXp(xp: Long): Int {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 100) {
      val need = xpRequiredForPlayerLevel(lvl)
      if (remaining < need) break
      remaining -= need
      lvl++
    }
    return lvl
  }

  fun xpRequiredForPlayerLevel(level: Int): Long = 400L + (level - 1).coerceAtLeast(0) * 120L

  fun xpProgressInCurrentPlayerLevel(xp: Long): Pair<Long, Long> {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 100) {
      val need = xpRequiredForPlayerLevel(lvl)
      if (remaining < need) return remaining to need
      remaining -= need
      lvl++
    }
    return 1L to 1L
  }

  fun rankTitleForLevel(level: Int): String = when {
    level >= 50 -> "Multiverse Sovereign"
    level >= 35 -> "Apex Continuum"
    level >= 22 -> "Crucible Warlord"
    level >= 12 -> "Conduit Prime"
    level >= 6 -> "Vanguard Operative"
    level >= 3 -> "SHD Field Agent"
    else -> "UnknownOS Recruit"
  }

  fun weaponLevelForXp(xp: Long): Int {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 30) {
      val need = 180L + (lvl - 1) * 80L
      if (remaining < need) break
      remaining -= need
      lvl++
    }
    return lvl
  }

  fun xpProgressInCurrentWeaponLevel(xp: Long): Pair<Long, Long> {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 30) {
      val need = 180L + (lvl - 1) * 80L
      if (remaining < need) return remaining to need
      remaining -= need
      lvl++
    }
    return 1L to 1L
  }
}

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
    baseDamage = 5,
  ),
  LIBERTY_WUNDERWAFFE(
    id = "ww_wunderwaffe",
    displayName = "Liberty Wunder-Arc",
    shortTag = "DG-ARC",
    description = "High-voltage bolts chain arc lightning to up to 4 nearby hostiles.",
    accentHex = 0xFF00E5FF,
    durationTicks = 1200f,
    baseDamage = 4,
  ),
  BLACKLIGHT_WHIP(
    id = "ww_blacklight_whip",
    displayName = "Blacklight Tendril Whip",
    shortTag = "WHIP",
    description = "Infinitely piercing crimson bio-lance that siphons +2 Shield per hit.",
    accentHex = 0xFFE040FB,
    durationTicks = 1200f,
    baseDamage = 4,
  ),
  CRUCIBLE_BFG(
    id = "ww_crucible_bfg",
    displayName = "Crucible BFG-9000",
    shortTag = "BFG-9K",
    description = "Superheated molten core that melts bosses and vaporizes surrounding waves.",
    accentHex = 0xFF00E676,
    durationTicks = 1080f,
    baseDamage = 8,
  ),
  CHRONO_ZAT(
    id = "ww_zat_stasis",
    displayName = "Chrono Zat'nik'tel",
    shortTag = "ZAT-SG",
    description = "Stargate energy arcs freeze struck enemies in temporal stasis.",
    accentHex = 0xFFFFD54F,
    durationTicks = 1260f,
    baseDamage = 4,
  ),
  STARBLADE_BOOMERANG(
    id = "ww_starblade_saber",
    displayName = "Twin Starblade Throw",
    shortTag = "SABER",
    description = "360-degree radial cosmic saber fan that shreds incoming rushes.",
    accentHex = 0xFF448AFF,
    durationTicks = 1200f,
    baseDamage = 5,
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
  val muzzleId: String = "standard_muzzle",
  val underbarrelId: String = "none_underbarrel",
  val magazineId: String = "extended_mag_1",
  val stockId: String = "tactical_stock",
  val rearGripId: String = "stippled_grip",
  val laserId: String = "none_laser",
  val ammoModId: String = "standard_core",
  val tracerColorHex: Long = 0xFF00E5FF,
)

@Immutable
data class CharacterCustomization(
  val suitThemeId: String = "vanguard_cyber",
  val visorStyleId: String = "cyan_visor",
  val auraTrailId: String = "none",
  val animationSpeedMultiplier: Float = 1.0f,
  val cloakEnabled: Boolean = true,
  val titleBadge: String = "Multiverse Operative",
  val primaryArmorHex: Long = 0xFF00E5FF,
  val accentArmorHex: Long = 0xFFFFAB40,
)

enum class GearSlot(val id: String, val displayName: String) {
  HELMET("helmet", "Tactical Helmet"),
  CHEST("chest", "Chassis Plate"),
  GLOVES("gloves", "Gauntlet Gloves"),
  BOOTS("boots", "Exo Boots"),
  BACK_RIG("back_rig", "Back-Rig Jump-Pack"),
}

@Immutable
data class GearState(
  val slot: GearSlot,
  val level: Int = 1,
  val prestige: Int = 0,
  val colorHex: Long = 0xFF00E5FF,
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

@Immutable
data class SkillNode(
  val id: String,
  val title: String,
  val description: String,
  val category: String, // COMBAT, SURVIVAL, ENGINEERING, WONDER
  val costEssence: Long,
  val unlocked: Boolean = false,
  val tier: Int = 1,
  val maxTier: Int = 5,
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
    gameplayEffectSummary = "+22% Fire Rate, +40% Wonder Weapon Drop Chance & +30% BP XP",
    bonusShield = 25,
    fireRateMultiplier = 1.22f,
    cooldownMultiplier = 0.90f,
    moveSpeedBonus = 0.08f,
    essenceMultiplier = 1.30f,
    wonderDropBonusMultiplier = 1.40f,
    glorySiphonPerBossHit = 1,
  ),
  CHRONO_MULTIVERSE(
    id = "bp_chrono",
    title = "V3: Chrono-Vault Pass",
    subtitle = "Space & Time 7-Chevron Temporal Edition",
    unlockCostEssence = 750L,
    accentHex = 0xFF7C4DFF,
    gameplayEffectSummary = "-30% Power/Stim Cooldowns & Extended Wonder Duration",
    bonusShield = 35,
    fireRateMultiplier = 1.12f,
    cooldownMultiplier = 0.70f,
    moveSpeedBonus = 0.12f,
    essenceMultiplier = 1.25f,
    wonderDropBonusMultiplier = 1.30f,
    glorySiphonPerBossHit = 2,
  ),
  HELLBREAKER_SUPREME(
    id = "bp_hellbreaker",
    title = "V4: Hellbreaker Supreme",
    subtitle = "Mythic Crucible & Bio-Claw Ultimate Edition",
    unlockCostEssence = 900L,
    accentHex = 0xFFFF3D71,
    gameplayEffectSummary = "+60 Max Shield, +20% Move Speed & +4 Armor Siphon vs Bosses",
    bonusShield = 60,
    fireRateMultiplier = 1.15f,
    cooldownMultiplier = 0.85f,
    moveSpeedBonus = 0.20f,
    essenceMultiplier = 1.40f,
    wonderDropBonusMultiplier = 1.35f,
    glorySiphonPerBossHit = 4,
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
  val playerPrestige: Int = 0,
  val weaponXpMap: Map<String, Long> = mapOf("wpn_pulse_carbine" to 0L),
  val weaponPrestigeMap: Map<String, Int> = emptyMap(),
  val gearMap: Map<String, GearState> = GearSlot.entries.associate { it.id to GearState(it) },
  val gearPrestige: Int = 0,
  val weaponCustom: WeaponCustomization = WeaponCustomization(),
  val characterCustom: CharacterCustomization = CharacterCustomization(),
  val levelCustom: LevelCustomization = LevelCustomization(),
  val activePassVersion: BattlePassVersion = BattlePassVersion.STANDARD_SECTOR_OPS,
  val unlockedPassVersions: Set<BattlePassVersion> = setOf(BattlePassVersion.STANDARD_SECTOR_OPS),
  val battlePassXp: Long = 0L,
  val battlePassPrestige: Int = 0,
  val bpCycleCount: Int = 0,
  val claimedPassTiers: Set<String> = emptySet(),
  val challengeProgress: Map<String, Long> = emptyMap(),
  val claimedChallenges: Set<String> = emptySet(),
  val challengePrestige: Int = 0,
  val skillNodes: Map<String, SkillNode> = ProgressionCatalog.defaultSkillNodes(),
  val wonderWeaponsPickedTotal: Long = 0L,
) {
  val playerLevel: Int
    get() = ProgressionCatalog.levelForXp(playerXp)

  val playerRankTitle: String
    get() = ProgressionCatalog.rankTitleForLevel(playerLevel, playerPrestige)

  fun weaponXp(weaponId: String): Long = weaponXpMap[weaponId] ?: 0L

  fun weaponLevel(weaponId: String): Int = ProgressionCatalog.weaponLevelForXp(weaponXp(weaponId))

  fun weaponPrestige(weaponId: String): Int = weaponPrestigeMap[weaponId] ?: 0
}

object ProgressionCatalog {

  val weapons: List<WeaponSpec> = listOf(
    WeaponSpec(
      id = "wpn_pulse_carbine",
      name = "SHD Pulse Carbine",
      universeOrigin = "Sector Zero",
      summary = "Balanced high-velocity energy carbine with crisp handling.",
      baseDamage = 2,
      baseCadenceTicks = 10.0f,
      baseProjectileSpeed = 0.022f,
      baseSpreadCount = 1,
      basePierce = 1,
      defaultTracerHex = 0xFF00E5FF,
    ),
    WeaponSpec(
      id = "wpn_plasma_scatter",
      name = "Blacklight Scattergun",
      universeOrigin = "Blacklight Conduit",
      summary = "Wide 3-bolt plasma spread for crowd control & close encounters.",
      baseDamage = 2,
      baseCadenceTicks = 13.0f,
      baseProjectileSpeed = 0.019f,
      baseSpreadCount = 3,
      basePierce = 0,
      defaultTracerHex = 0xFF7C4DFF,
    ),
    WeaponSpec(
      id = "wpn_starblade_lancer",
      name = "Starblade Nova Lancer",
      universeOrigin = "Aegis & Starblade",
      summary = "Piercing twin-core rail lance that slices through lined hostiles.",
      baseDamage = 3,
      baseCadenceTicks = 11.5f,
      baseProjectileSpeed = 0.028f,
      baseSpreadCount = 1,
      basePierce = 3,
      defaultTracerHex = 0xFFE040FB,
    ),
    WeaponSpec(
      id = "wpn_crucible_cannon",
      name = "Hellbreaker Heavy Cannon",
      universeOrigin = "Hellbreaker Citadel",
      summary = "Heavy molten slugs engineered to crack Mechs & Hydra bosses.",
      baseDamage = 4,
      baseCadenceTicks = 14.5f,
      baseProjectileSpeed = 0.020f,
      baseSpreadCount = 1,
      basePierce = 2,
      defaultTracerHex = 0xFFFF5252,
    ),
    WeaponSpec(
      id = "wpn_chrono_repeater",
      name = "Chrono-Vault Repeater",
      universeOrigin = "Chrono-Vault",
      summary = "Rapid-cycle temporal repeater firing twin golden bolts at high cadence.",
      baseDamage = 2,
      baseCadenceTicks = 7.5f,
      baseProjectileSpeed = 0.025f,
      baseSpreadCount = 2,
      basePierce = 1,
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
    "overclocked_rifling" to "Overclocked (+22% Bolt Speed)",
    "spread_choke" to "Spread Choke (+1 Extra Bolt)",
    "heavy_bore" to "Heavy Bore (+2 Base Damage)",
  )

  val muzzles: List<Pair<String, String>> = listOf(
    "standard_muzzle" to "Standard Flash Hider",
    "tactical_brake" to "Tactical Muzzle Brake",
    "monolithic_suppressor" to "Monolithic Suppressor",
    "heavy_compensator" to "Heavy Recoil Compensator",
  )

  val underbarrels: List<Pair<String, String>> = listOf(
    "none_underbarrel" to "No Attachment",
    "foregrip_vertical" to "Vertical Foregrip",
    "ranger_grip" to "Ranger Stabilizer",
    "bipod_mount" to "Heavy Bipod Mount",
  )

  val magazines: List<Pair<String, String>> = listOf(
    "extended_mag_1" to "Standard Mag",
    "extended_mag_2" to "Extended Drum Mag (+Spread)",
    "fast_mag" to "Speed Reload Mag",
    "composite_mag" to "Composite Energy Cell",
  )

  val stocks: List<Pair<String, String>> = listOf(
    "tactical_stock" to "Tactical Stock",
    "heavy_padded_stock" to "Heavy Padded Buffer",
    "lightweight_skeleton" to "Skeletonized Dash Stock",
  )

  val rearGrips: List<Pair<String, String>> = listOf(
    "stippled_grip" to "Stippled Combat Grip",
    "rubberized_grip" to "Rubberized Recoil Grip",
    "tactical_tape" to "Tactical Friction Tape",
  )

  val lasers: List<Pair<String, String>> = listOf(
    "none_laser" to "No Laser",
    "tactical_laser_5mw" to "5mW Tactical Laser",
    "infrared_designator" to "IR Target Designator",
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

  fun defaultSkillNodes(): Map<String, SkillNode> = listOf(
    SkillNode("sk_shield_1", "Reinforced Chassis I", "Increases max operator shield by +25.", "SURVIVAL", 400L, false, 1, 5),
    SkillNode("sk_shield_2", "Reinforced Chassis II", "Increases max operator shield by another +35.", "SURVIVAL", 900L, false, 1, 5),
    SkillNode("sk_damage_1", "Ballistic Lethality I", "Boosts weapon projectile base damage by +1.", "COMBAT", 500L, false, 1, 5),
    SkillNode("sk_damage_2", "Ballistic Lethality II", "Boosts weapon projectile base damage by another +2.", "COMBAT", 1100L, false, 1, 5),
    SkillNode("sk_cooldown_1", "Overdrive Capacitor I", "-10% cooldowns for powers and stims.", "ENGINEERING", 600L, false, 1, 5),
    SkillNode("sk_wonder_1", "Mystery Crate Attractor", "+35% higher random Wonder Weapon drop chance.", "WONDER", 800L, false, 1, 5),
  ).associateBy { it.id }

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

  fun tiersForVersion(version: BattlePassVersion, cycle: Int = 0): List<BattlePassTierReward> {
    val baseTiers = when (version) {
      BattlePassVersion.STANDARD_SECTOR_OPS -> listOf(
        BattlePassTierReward(1, 0L, "SHD Matte Finish", "Unlocks Sector Zero standard operator trim & +15% Essence", 200L, "CAMO"),
        BattlePassTierReward(2, 400L, "Pulse Sight Reticle", "Precision Holo crosshair & bonus Essence cache", 280L, "OPTIC"),
        BattlePassTierReward(3, 950L, "Quarantine XP Surge", "+600 Weapon XP injection for your active primary", 350L, "WPN XP"),
        BattlePassTierReward(4, 1600L, "Aegis Emerald Tracer", "Unlocks bright emerald tracer bolts & badge", 420L, "TRACER"),
        BattlePassTierReward(5, 2400L, "Division Tech Cache", "Boosts Mystery Wonder Weapon salvage rewards", 550L, "CACHE"),
        BattlePassTierReward(6, 3400L, "Vanguard Sentinel Master", "Prestige Operator Emblem + 900 Bonus Essence", 900L, "PRESTIGE"),
      )
      BattlePassVersion.BLACKCELL_OVERDRIVE -> listOf(
        BattlePassTierReward(1, 0L, "Blackcell Obsidian Suit", "Instantly grants Obsidian & Gold armor trim + +22% Fire Rate", 300L, "SUIT"),
        BattlePassTierReward(2, 450L, "Molten Damascus Camo", "Blackcell animated gold-vein finish & tracer color", 400L, "CAMO"),
        BattlePassTierReward(3, 1050L, "Overclocked Syndicate", "Boosts Wonder Crate spawn rate +40% in all modes", 520L, "PERK"),
        BattlePassTierReward(4, 1750L, "Gilded BFG Munitions", "+800 Weapon XP + Blackcell Heavy Bore tuning", 650L, "BLUEPRINT"),
        BattlePassTierReward(5, 2600L, "Syndicate Golden Crown", "Unlocks Gold Prime visor & aura ring flare", 800L, "VISOR"),
        BattlePassTierReward(6, 3600L, "Blackcell Overlord Tier", "Ultimate Syndicate Title + 1200 Bonus Essence", 1200L, "ULTIMATE"),
      )
      BattlePassVersion.CHRONO_MULTIVERSE -> listOf(
        BattlePassTierReward(1, 0L, "Time-Warden Regalia", "Activates -30% Power/Stim Cooldowns in Arena", 300L, "PASSIVE"),
        BattlePassTierReward(2, 450L, "Cryo-Stasis Munitions", "Unlocks Chrono Freeze bolt coloration & stasis aura", 400L, "AMMO"),
        BattlePassTierReward(3, 1050L, "7-Chevron Gate Floor", "Unlocks Cosmic Nexus Arena floor & grid styling", 520L, "LEVEL"),
        BattlePassTierReward(4, 1750L, "Librarian Artifact Surge", "+800 Weapon XP + extended Wonder Weapon duration", 650L, "WPN XP"),
        BattlePassTierReward(5, 2600L, "Chrono Zat Priority", "Higher chance to pull Chrono Zat'nik'tel pickups", 800L, "WONDER"),
        BattlePassTierReward(6, 3600L, "Space & Time Sovereign", "Sovereign Temporal Banner + 1250 Bonus Essence", 1250L, "PRESTIGE"),
      )
      BattlePassVersion.HELLBREAKER_SUPREME -> listOf(
        BattlePassTierReward(1, 0L, "Hellbreaker Plate", "Activates +60 Max Shield & +20% Move Speed immediately", 350L, "ARMOR"),
        BattlePassTierReward(2, 500L, "Blood Moon Arena Kit", "Unlocks Crimson Crucible sky/floor theme & tracers", 480L, "LEVEL"),
        BattlePassTierReward(3, 1150L, "Vampiric Glory Siphon", "Hits vs Bosses/Elites siphon +4 Shield live in Arena", 600L, "SIPHON"),
        BattlePassTierReward(4, 1900L, "Adamant Claw Temper", "+900 Weapon XP + Crucible Ember armor glow", 750L, "AURA"),
        BattlePassTierReward(5, 2800L, "BFG-9000 Arsenal Lock", "Increases Crucible BFG-9000 Wonder drop priority", 900L, "WONDER"),
        BattlePassTierReward(6, 3800L, "Apex Warlord Ascension", "Hellbreaker Supreme Emblem + 1400 Bonus Essence", 1400L, "SUPREME"),
      )
    }
    if (cycle <= 0) return baseTiers
    return baseTiers.map { t ->
      t.copy(
        tier = t.tier + cycle * 6,
        bpXpRequired = t.bpXpRequired + (cycle * 3800L),
        title = "${t.title} [Prestige C$cycle]",
        rewardEssence = t.rewardEssence + cycle * 300L,
      )
    }
  }

  fun levelForXp(xp: Long): Int {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 1000) {
      val need = xpRequiredForPlayerLevel(lvl)
      if (remaining < need) break
      remaining -= need
      lvl++
    }
    return lvl
  }

  fun xpRequiredForPlayerLevel(level: Int): Long = 350L + (level - 1).coerceAtLeast(0) * 85L

  fun xpProgressInCurrentPlayerLevel(xp: Long): Pair<Long, Long> {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 1000) {
      val need = xpRequiredForPlayerLevel(lvl)
      if (remaining < need) return remaining to need
      remaining -= need
      lvl++
    }
    return 1L to 1L
  }

  fun rankTitleForLevel(level: Int, prestige: Int): String {
    val suffix = if (prestige > 0) " [P$prestige]" else ""
    val title = when {
      level >= 500 -> "Multiverse Sovereign"
      level >= 350 -> "Apex Continuum"
      level >= 200 -> "Crucible Warlord"
      level >= 100 -> "Conduit Prime"
      level >= 50 -> "Vanguard Commander"
      level >= 20 -> "SHD Elite Operative"
      level >= 5 -> "Field Agent"
      else -> "UnknownOS Recruit"
    }
    return "$title$suffix"
  }

  fun weaponLevelForXp(xp: Long): Int {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 60) {
      val need = 150L + (lvl - 1) * 55L
      if (remaining < need) break
      remaining -= need
      lvl++
    }
    return lvl
  }

  fun xpProgressInCurrentWeaponLevel(xp: Long): Pair<Long, Long> {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 60) {
      val need = 150L + (lvl - 1) * 55L
      if (remaining < need) return remaining to need
      remaining -= need
      lvl++
    }
    return 1L to 1L
  }
}

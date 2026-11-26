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
  RAY_SPHERE_OVERDRIVE(
    id = "ww_ray_sphere",
    displayName = "Ray-Sphere Conduit Core",
    shortTag = "RAY-SPH",
    description = "Cole McGrath & Blacklight hybrid orb unleashing triple lightning conduits.",
    accentHex = 0xFF00E5FF,
    durationTicks = 1260f,
    baseDamage = 6,
  ),
  MERCER_BIOCLAW_SWARM(
    id = "ww_mercer_swarm",
    displayName = "Mercer Bio-Claw Devastator",
    shortTag = "BIOCLAW",
    description = "3-fang Prototype biomass fan that slices hordes and siphons +3 Shield.",
    accentHex = 0xFFFF1744,
    durationTicks = 1200f,
    baseDamage = 6,
  ),
  GATE_HORIZON_CANNON(
    id = "ww_gate_horizon",
    displayName = "7-Chevron Horizon Cannon",
    shortTag = "HORIZON",
    description = "Stargate event-horizon launcher triggering heavy shockwaves & stasis.",
    accentHex = 0xFFFFD54F,
    durationTicks = 1320f,
    baseDamage = 7,
  ),
  STAFF_SAND(
    id = "staff_sand",
    displayName = "Sand Staff",
    shortTag = "SAND-STF",
    description = "Summon desert dunes and sandstorm vortexes dealing Sand damage.",
    accentHex = 0xFFD7CCC8,
    durationTicks = 1260f,
    baseDamage = 6,
  ),
  STAFF_CRYO(
    id = "staff_cryo",
    displayName = "Ice Staff",
    shortTag = "ICE-STF",
    description = "Hurl glacial shards that freeze and shatter enemy armor with Cryo damage.",
    accentHex = 0xFF80D8FF,
    durationTicks = 1260f,
    baseDamage = 6,
  ),
  STAFF_THERMAL(
    id = "staff_thermal",
    displayName = "Fire Staff",
    shortTag = "FIRE-STF",
    description = "Channel thermal magma fireballs that melt and burn enemy hordes with Thermal damage.",
    accentHex = 0xFFFF5722,
    durationTicks = 1260f,
    baseDamage = 7,
  ),
  STAFF_BIO(
    id = "staff_bio",
    displayName = "Toxic Staff",
    shortTag = "TOXIC-STF",
    description = "Unleash corrosive bio-venom that melts armor and siphons Shield on hit with Bio damage.",
    accentHex = 0xFF76FF03,
    durationTicks = 1260f,
    baseDamage = 6,
  ),
  STAFF_ARC(
    id = "staff_arc",
    displayName = "Electric Staff",
    shortTag = "ELEC-STF",
    description = "Discharge high-voltage lightning bolts that chain across groups with Energy damage.",
    accentHex = 0xFF00E5FF,
    durationTicks = 1260f,
    baseDamage = 6,
  ),
  STAFF_KINETIC(
    id = "staff_kinetic",
    displayName = "Rock Staff",
    shortTag = "ROCK-STF",
    description = "Launch heavy tectonic rock boulders that crush armor and stagger hostiles with Rock damage.",
    accentHex = 0xFFFFD54F,
    durationTicks = 1260f,
    baseDamage = 6,
  ),
  STAFF_VOID(
    id = "staff_void",
    displayName = "Void Staff",
    shortTag = "VOID-STF",
    description = "Project dark matter void tears that pull in and vaporize enemies with Void damage.",
    accentHex = 0xFFE040FB,
    durationTicks = 1320f,
    baseDamage = 8,
  ),
}

enum class CharacterRarity(
  val rank: Int,
  val label: String,
  val colorHex: Long,
  val statMultiplier: Float,
  val shardsForNext: Int,
) {
  COMMON(1, "Common", 0xFF90A4AE, 1.00f, 3),
  UNCOMMON(2, "Uncommon", 0xFF00E676, 1.18f, 6),
  RARE(3, "Rare", 0xFF448AFF, 1.40f, 10),
  EPIC(4, "Epic", 0xFF7C4DFF, 1.70f, 15),
  LEGENDARY(5, "Legendary", 0xFFFFAB40, 2.10f, 22),
  EXOTIC(6, "Exotic", 0xFFFFD54F, 2.55f, 30),
  MYTHIC(7, "Mythic", 0xFFFF3D71, 3.10f, 40),
  TRANSCENDENT(8, "Transcendent", 0xFF00E5FF, 3.80f, 55),
  COSMIC(9, "Cosmic", 0xFFE040FB, 4.60f, 75),
  OMNIVERSE(10, "Omniverse Sovereign", 0xFFFFFFFF, 5.50f, 0);

  fun nextRarity(): CharacterRarity? = entries.firstOrNull { it.rank == rank + 1 }

  companion object {
    fun fromRank(rank: Int): CharacterRarity =
      entries.firstOrNull { it.rank == rank.coerceIn(1, entries.size) } ?: COMMON
  }
}

enum class WorldDifficultyTier(
  val id: String,
  val label: String,
  val enemyHpScale: Float,
  val enemySpeedScale: Float,
  val rewardScale: Float,
  val accentHex: Long,
) {
  NORMAL("normal", "Normal", 0.78f, 1.0f, 1.0f, 0xFF00E5FF),
  HARDENED("hardened", "Hardened", 0.95f, 1.12f, 1.35f, 0xFF00E676),
  VETERAN("veteran", "Veteran", 1.20f, 1.24f, 1.80f, 0xFFFFAB40),
  NIGHTMARE("nightmare", "Nightmare", 1.50f, 1.36f, 2.45f, 0xFFFF3D71),
  GRANDMASTER("grandmaster", "Grandmaster", 1.85f, 1.48f, 3.25f, 0xFFE040FB);

  companion object {
    fun fromId(id: String): WorldDifficultyTier =
      entries.firstOrNull { it.id == id } ?: NORMAL
  }
}

enum class CollectionDifficulty(
  val id: String,
  val rank: Int,
  val label: String,
  val shortTag: String,
  val summary: String,
  val enemyHpScale: Float,
  val enemySpeedScale: Float,
  val rewardMultiplier: Float,
  val accentHex: Long,
) {
  STORY(
    id = "col_story",
    rank = 1,
    label = "I: Story Explorer",
    shortTag = "I",
    summary = "Story Collection Balance • 0.65x Enemy HP • 0.95x Baseline Rewards",
    enemyHpScale = 0.65f,
    enemySpeedScale = 0.92f,
    rewardMultiplier = 0.95f,
    accentHex = 0xFF40C4FF,
  ),
  OPERATIVE(
    id = "col_operative",
    rank = 2,
    label = "II: Tactical Operative",
    shortTag = "II",
    summary = "Standard Collection Balance • 0.85x Enemy HP • 1.00x Rewards",
    enemyHpScale = 0.85f,
    enemySpeedScale = 1.00f,
    rewardMultiplier = 1.00f,
    accentHex = 0xFF00E676,
  ),
  VANGUARD(
    id = "col_vanguard",
    rank = 3,
    label = "III: Vanguard Hunter",
    shortTag = "III",
    summary = "Aggressive Collection Foes • 1.10x HP • 1.50x Rewards",
    enemyHpScale = 1.10f,
    enemySpeedScale = 1.12f,
    rewardMultiplier = 1.50f,
    accentHex = 0xFFFFD54F,
  ),
  LEGEND(
    id = "col_legend",
    rank = 4,
    label = "IV: Multiverse Legend",
    shortTag = "IV",
    summary = "Elite Collection Assault • 1.40x HP • 2.25x Rewards",
    enemyHpScale = 1.40f,
    enemySpeedScale = 1.24f,
    rewardMultiplier = 2.25f,
    accentHex = 0xFFFF9100,
  ),
  APOTHEOSIS(
    id = "col_apotheosis",
    rank = 5,
    label = "V: Apotheosis Sovereign",
    shortTag = "V",
    summary = "Peak Collection Crucible • 1.75x HP • 3.25x Rewards",
    enemyHpScale = 1.75f,
    enemySpeedScale = 1.38f,
    rewardMultiplier = 3.25f,
    accentHex = 0xFFFF3D71,
  );

  companion object {
    fun fromId(id: String): CollectionDifficulty =
      entries.firstOrNull { it.id == id } ?: OPERATIVE
  }
}

enum class NewGamePlusVersion(
  val tier: Int,
  val label: String,
  val shortTag: String,
  val subtitle: String,
  val rewardMultiplier: Float,
  val enemyHpScale: Float,
  val enemySpeedScale: Float,
  val accentHex: Long,
) {
  STANDARD(
    tier = 0,
    label = "Standard Run",
    shortTag = "STD",
    subtitle = "Initial timeline run • Complete a run to unlock New Game+",
    rewardMultiplier = 1.00f,
    enemyHpScale = 0.85f,
    enemySpeedScale = 1.00f,
    accentHex = 0xFF00E5FF,
  ),
  NG_PLUS_1(
    tier = 1,
    label = "NG+ I: Shattered Echo",
    shortTag = "NG+1",
    subtitle = "New Game+ Base • +75% base rewards & higher Essence drops",
    rewardMultiplier = 1.75f,
    enemyHpScale = 1.05f,
    enemySpeedScale = 1.08f,
    accentHex = 0xFF00E676,
  ),
  NG_PLUS_2(
    tier = 2,
    label = "NG+ II: Ascendant Surge",
    shortTag = "NG+2",
    subtitle = "Post-Completion Version #1 • 2.60x rewards & elite surges",
    rewardMultiplier = 2.60f,
    enemyHpScale = 1.28f,
    enemySpeedScale = 1.16f,
    accentHex = 0xFF7C4DFF,
  ),
  NG_PLUS_3(
    tier = 3,
    label = "NG+ III: Paradox Rift",
    shortTag = "NG+3",
    subtitle = "Post-Completion Version #2 • 3.60x rewards & dense bosses",
    rewardMultiplier = 3.60f,
    enemyHpScale = 1.55f,
    enemySpeedScale = 1.24f,
    accentHex = 0xFFFFAB40,
  ),
  NG_PLUS_4(
    tier = 4,
    label = "NG+ IV: Omega Protocol",
    shortTag = "NG+4",
    subtitle = "Post-Completion Version #3 • 4.85x rewards & apex hunters",
    rewardMultiplier = 4.85f,
    enemyHpScale = 1.85f,
    enemySpeedScale = 1.32f,
    accentHex = 0xFFFF3D71,
  ),
  NG_PLUS_5(
    tier = 5,
    label = "NG+ V: Eternal Sovereign",
    shortTag = "NG+5",
    subtitle = "Post-Completion Version #4 • 6.50x rewards & mythic mayhem",
    rewardMultiplier = 6.50f,
    enemyHpScale = 2.15f,
    enemySpeedScale = 1.40f,
    accentHex = 0xFFFFD54F,
  );

  companion object {
    const val MAX_NG_PLUS_TIER: Int = 5

    fun fromTier(tier: Int): NewGamePlusVersion =
      entries.firstOrNull { it.tier == tier.coerceIn(0, MAX_NG_PLUS_TIER) } ?: STANDARD
  }
}

@Immutable
data class PowerLightSummary(
  val powerLevel: Int,
  val lightLevel: Int,
  val tierLabel: String,
)

enum class PowerTier(
  val level: Int,
  val label: String,
  val colorHex: Long,
  val multiplier: Float,
) {
  STANDARD(1, "Standard", 0xFF90A4AE, 1.0f),
  TACTICAL(2, "Tactical", 0xFF00E5FF, 1.25f),
  RARE(3, "Rare", 0xFF448AFF, 1.55f),
  EPIC(4, "Epic", 0xFF7C4DFF, 1.9f),
  LEGENDARY(5, "Legendary", 0xFFFFAB40, 2.35f),
  MYTHIC(6, "Mythic", 0xFFFF3D71, 2.9f),
  ASCENDANT(7, "Ascendant", 0xFF00E676, 3.6f),
  COSMIC(8, "Cosmic", 0xFFE040FB, 4.4f),
  CELESTIAL(9, "Celestial", 0xFFFFD54F, 5.4f),
  OMEGA(10, "Omega", 0xFFFFFFFF, 6.8f),
  TRANSCENDENT(11, "Transcendent", 0xFF00BFA5, 8.4f),
  ASTRAL(12, "Astral", 0xFF40C4FF, 10.3f),
  HYPERION(13, "Hyperion", 0xFF536DFE, 12.6f),
  RADIANT(14, "Radiant", 0xFFFFEA00, 15.4f),
  APEX(15, "Apex", 0xFFFF9100, 18.8f),
  SOVEREIGN(16, "Sovereign", 0xFFFF1744, 23.0f),
  QUANTUM(17, "Quantum", 0xFF1DE9B6, 28.0f),
  VOIDBORN(18, "Voidborn", 0xFFAA00FF, 34.0f),
  CHRONOS(19, "Chronos", 0xFFFFD180, 41.5f),
  INFINITY(20, "Infinity", 0xFF00E5FF, 50.5f),
  ABYSSAL(21, "Abyssal", 0xFFD500F9, 61.5f),
  SERAPHIC(22, "Seraphic", 0xFFFFC400, 75.0f),
  PRIMORDIAL(23, "Primordial", 0xFF00E676, 92.0f),
  SINGULARITY(24, "Singularity", 0xFFFF3D71, 112.0f),
  MULTIVERSAL(25, "Multiversal", 0xFFFFFFFF, 140.0f);

  companion object {
    const val MAX_TIER = 25

    fun fromLevel(tier: Int): PowerTier =
      entries.firstOrNull { it.level == tier.coerceIn(1, MAX_TIER) } ?: MULTIVERSAL
  }
}

enum class StarRankMeta(
  val stars: Int,
  val label: String,
  val multiplier: Float,
  val colorHex: Long,
) {
  STAR_1(1, "1★ Spark", 1.00f, 0xFF90A4AE),
  STAR_2(2, "2★ Flare", 1.25f, 0xFF40C4FF),
  STAR_3(3, "3★ Nimbus", 1.50f, 0xFF00E676),
  STAR_4(4, "4★ Supernova", 1.75f, 0xFF7C4DFF),
  STAR_5(5, "5★ Pulsar", 2.00f, 0xFFFFD54F),
  STAR_6(6, "6★ Celestial", 2.25f, 0xFFFFAB40),
  STAR_7(7, "7★ Astral Crown", 2.50f, 0xFFFF5252),
  STAR_8(8, "8★ Cosmic Apex", 2.75f, 0xFFE040FB),
  STAR_9(9, "9★ Reality Loom", 3.00f, 0xFF00E5FF),
  STAR_10(10, "10★ Omniverse Nova", 3.25f, 0xFFFFFFFF),
  STAR_11(11, "11★ Stellar Genesis", 3.55f, 0xFF00E5FF),
  STAR_12(12, "12★ Singularity King", 3.85f, 0xFFFF3D71),
  STAR_13(13, "13★ Infinite Cosmos", 4.15f, 0xFFE040FB),
  STAR_14(14, "14★ Transcendent Apex", 4.45f, 0xFFFFD54F),
  STAR_15(15, "15★ Omniverse Sovereign", 4.80f, 0xFFFFFFFF);

  companion object {
    const val MAX_STARS: Int = 15

    fun forStars(stars: Int): StarRankMeta =
      entries.firstOrNull { it.stars == stars.coerceIn(1, MAX_STARS) } ?: STAR_15
  }
}

enum class LobbyPartyMode(
  val id: String,
  val displayName: String,
  val shortLabel: String,
  val squadSize: Int,
  val defaultLobbySize: Int,
  val summary: String,
) {
  SOLO(
    id = "solo",
    displayName = "Solo Lobby",
    shortLabel = "SOLO",
    squadSize = 1,
    defaultLobbySize = 1,
    summary = "Lone wolf 1-player squad (supports 1P solo or over-8 FFA lobbies)",
  ),
  DUOS(
    id = "duos",
    displayName = "Duos Lobby",
    shortLabel = "DUOS",
    squadSize = 2,
    defaultLobbySize = 2,
    summary = "2-operator strike team paired with an AI tactical partner",
  ),
  SQUADS(
    id = "squads",
    displayName = "Squad Lobby",
    shortLabel = "SQUADS",
    squadSize = 4,
    defaultLobbySize = 4,
    summary = "4-operator fireteam auto-filled by AI Squad Bots when needed",
  ),
}

enum class BotDifficulty(
  val id: String,
  val label: String,
  val damageScale: Float,
  val fireRateScale: Float,
) {
  RECRUIT("recruit", "Recruit AI", 0.85f, 0.80f),
  OPERATIVE("operative", "Operative AI", 1.15f, 1.0f),
  VETERAN("veteran", "Veteran AI", 1.50f, 1.25f),
  APEX("apex", "Apex Omega AI", 2.0f, 1.55f),
}

@Immutable
data class LobbyBotMember(
  val id: String,
  val callsign: String,
  val squadIndex: Int,
  val isAlliedSquadmate: Boolean,
  val isHumanPlayer: Boolean,
  val archetype: String,
  val accentHex: Long,
  val level: Int,
  val prestige: Int,
)

@Immutable
data class LobbyConfig(
  val partyMode: LobbyPartyMode = LobbyPartyMode.SQUADS,
  val targetPlayers: Int = 12,
  val autoFillBots: Boolean = true,
  val botDifficulty: BotDifficulty = BotDifficulty.OPERATIVE,
  val humanPlayersOnline: Int = 1,
) {
  val isOverEightLobby: Boolean get() = targetPlayers > 8
  val isOver100Lobby: Boolean get() = targetPlayers >= 100
  val activeBotCount: Int
    get() = if (autoFillBots) (targetPlayers - humanPlayersOnline).coerceAtLeast(0) else 0
}

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
  val xp: Long = 0L,
  val tier: Int = 1,
  val colorHex: Long = 0xFF00E5FF,
  val starLevel: Int = 1,
)

enum class EquipmentSlotType(val id: String, val displayName: String) {
  LETHAL("lethal", "Lethal Equipment"),
  TACTICAL("tactical", "Tactical Equipment"),
}

@Immutable
data class EquipmentItemSpec(
  val id: String,
  val name: String,
  val slotType: EquipmentSlotType,
  val description: String,
  val baseDamageOrEffect: Float,
  val cooldownSeconds: Float,
  val defaultColorHex: Long,
)

@Immutable
data class EquipmentState(
  val slotId: String,
  val equippedItemId: String,
  val level: Int = 1,
  val prestige: Int = 0,
  val xp: Long = 0L,
  val variantId: String = "standard",
  val tier: Int = 1,
  val starLevel: Int = 1,
)

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
  val damageTypeId: String = "kinetic",
  val isCustomWeapon: Boolean = false,
)

@Immutable
data class UserCustomWeaponSpec(
  val id: String,
  val weaponCode: String,
  val name: String,
  val gameCategory: CustomMapGameCategory = CustomMapGameCategory.INFAMOUS,
  val description: String = "",
  val damageTypeId: String = "arc",
  val baseDamage: Int = 4,
  val baseCadenceTicks: Float = 8.5f,
  val baseProjectileSpeed: Float = 0.025f,
  val baseSpreadCount: Int = 2,
  val basePierce: Int = 2,
  val defaultTracerHex: Long = gameCategory.defaultAccentHex,
  val isUserCreated: Boolean = true,
  val createdAtMillis: Long = 0L,
) {
  fun toWeaponSpec(): WeaponSpec = WeaponSpec(
    id = id,
    name = name,
    universeOrigin = "${gameCategory.label} Custom",
    summary = description.ifBlank { "${gameCategory.label} forged custom weapon ($weaponCode) • Element: ${damageTypeId.uppercase()}" },
    baseDamage = baseDamage.coerceIn(1, 30),
    baseCadenceTicks = baseCadenceTicks.coerceIn(3.5f, 18.0f),
    baseProjectileSpeed = baseProjectileSpeed.coerceIn(0.015f, 0.045f),
    baseSpreadCount = baseSpreadCount.coerceIn(1, 7),
    basePierce = basePierce.coerceIn(0, 8),
    defaultTracerHex = defaultTracerHex,
    damageTypeId = damageTypeId,
    isCustomWeapon = true,
  )
}

@Immutable
data class WeaponCustomization(
  val equippedWeaponId: String = "wpn_pulse_carbine",
  val equippedWeaponIds: List<String> = listOf("wpn_pulse_carbine"),
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
data class WonderWeaponCustomization(
  val equippedWonderId: String = "ww_ray_mk3",
  val coreId: String = "core_standard_wonder",
  val lensId: String = "lens_standard_wonder",
  val amplifierId: String = "amp_standard_wonder",
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

@Immutable
data class LevelCustomization(
  val skyThemeId: String = "default_sector",
  val gridStyleId: String = "tactical_square",
  val enemyDensityMultiplier: Float = 1.0f,
  val worldDifficultyTier: WorldDifficultyTier = WorldDifficultyTier.NORMAL,
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
  val category: String, // COMBAT, SURVIVAL, ENGINEERING, PROTECTION, WONDER, REAL_LIFE
  val costEssence: Long,
  val unlocked: Boolean = false,
  val tier: Int = 1,
  val maxTier: Int = 25,
  val starLevel: Int = 1,
  val inspirationTag: String = "Core",
) {
  fun nextUpgradeCost(): Long = costEssence + (tier * 250L)
}

@Immutable
data class ProtectionUpgradeSpec(
  val id: String,
  val title: String,
  val subtitle: String,
  val effectTag: String,
  val baseCostEssence: Long,
  val maxRank: Int = 25,
  val colorHex: Long,
) {
  fun nextCost(currentRank: Int): Long = baseCostEssence + (currentRank.coerceAtLeast(0) * 180L)
}

@Immutable
data class UserCustomPerkSpec(
  val id: String,
  val name: String,
  val description: String,
  val accentHex: Long = 0xFF00E676L,
  val shieldBonusPerRank: Int = 25,
  val damagePctPerRank: Int = 6,
  val fireRatePctPerRank: Int = 6,
  val cooldownHastePctPerRank: Int = 8,
  val moveSpeedPctPerRank: Int = 3,
  val regenPerSecPerRank: Float = 1.0f,
  val siphonPerKillPerRank: Int = 1,
  val tier: Int = 1,
  val maxTier: Int = 25,
  val starLevel: Int = 1,
)

@Immutable
data class GearSlotCustomization(
  val slotId: String,
  val customTitle: String = "",
  val alloyId: String = "titanium",
  val focusId: String = "balanced",
  val elementAffinityId: String = "kinetic",
  val accentHex: Long = 0xFF00E5FFL,
)

@Immutable
data class UserCustomGearSpec(
  val id: String,
  val slot: GearSlot,
  val name: String,
  val description: String = "",
  val elementAffinityId: String = "kinetic",
  val accentHex: Long = 0xFF00E5FFL,
  val bonusShield: Int = 30,
  val bonusDamagePct: Int = 8,
  val bonusRatePct: Int = 6,
  val bonusCooldownHastePct: Int = 10,
  val bonusProtectionPct: Int = 4,
  val tier: Int = 1,
  val maxTier: Int = 25,
  val starLevel: Int = 1,
)

@Immutable
data class UserCustomEquipmentSpec(
  val id: String,
  val slotType: EquipmentSlotType,
  val name: String,
  val description: String = "",
  val damageTypeId: String = "explosive",
  val baseDamageOrEffect: Float = 200f,
  val cooldownSeconds: Float = 5.5f,
  val cooldownHastePct: Int = 6,
  val accentHex: Long = 0xFFFF6D00L,
  val tier: Int = 1,
  val maxTier: Int = 25,
  val starLevel: Int = 1,
) {
  fun toEquipmentItemSpec(): EquipmentItemSpec = EquipmentItemSpec(
    id = id,
    name = name,
    slotType = slotType,
    description = description.ifBlank { "${damageTypeId.uppercase()} Custom ${slotType.displayName} • +${cooldownHastePct * tier}% Haste" },
    baseDamageOrEffect = baseDamageOrEffect,
    cooldownSeconds = cooldownSeconds.coerceIn(1.5f, 25f),
    defaultColorHex = accentHex,
  )
}

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
    gameplayEffectSummary = "+15% Bonus Blue Essence & Standard XP Progression",
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
data class AchievementSpec(
  val id: String,
  val title: String,
  val description: String,
  val category: String, // COMBAT, PROGRESSION, LOBBY, ARMORY, STORE
  val target: Long,
  val rewardBlueEssence: Long,
  val rewardPlayerXp: Long,
  val tierRank: Int = 1,
)

@Immutable
data class AchievementState(
  val spec: AchievementSpec,
  val progress: Long,
  val claimed: Boolean,
) {
  val unlocked: Boolean get() = progress >= spec.target
  val ratio: Float get() = (progress.toFloat() / spec.target.coerceAtLeast(1L)).coerceIn(0f, 1f)
}

enum class StoreCategory(val id: String, val label: String) {
  ALL("all", "All"),
  BOOSTERS("boosters", "XP Boosters"),
  TIERS("tiers", "Stronger Tiers"),
  ARSENAL("arsenal", "Weapons & Wonder"),
  COSMETICS("cosmetics", "Suits & Bots"),
}

@Immutable
data class StoreItemSpec(
  val id: String,
  val name: String,
  val category: StoreCategory,
  val description: String,
  val costBlueEssence: Long,
  val tier: Int,
  val accentHex: Long,
  val repeatable: Boolean = true,
)

@Immutable
data class MatchProgressionReport(
  val eliminations: Long,
  val bossEliminations: Long,
  val waveReached: Long,
  val wonderPickupsCollected: Int,
  val weaponIdUsed: String,
  val extracted: Boolean,
  val weaponLevelUps: Int = 0,
  val highestPackTierReached: Int = 1,
  val lobbySizePlayed: Int = 1,
  val redEssenceEarned: Long = 0L,
  val yellowEssenceEarned: Long = 0L,
  val greenEssenceEarned: Long = 0L,
  val combinedEssenceEarned: Long = 0L,
  val blastShardsEarned: Long = 0L,
  val perkDrinksCollected: Int = 0,
  val blastCoresEarned: Map<String, Long> = emptyMap(),
  val buildingsExplored: Int = 0,
  val trapKills: Int = 0,
  val worldId: String = "sector_zero",
  val areaId: String = "area_empire_city",
  val characterShardsCollected: Long = 0L,
  val catsFound: Int = 0,
  val lootCratesOpened: Int = 0,
  val recruitedOutfitIds: List<String> = emptyList(),
)

@Immutable
data class OperatorProgressionState(
  val playerXp: Long = 0L,
  val playerPrestige: Int = 0,
  val characterTier: Int = 1,
  val characterStarLevel: Int = 1,
  val weaponXpMap: Map<String, Long> = mapOf("wpn_pulse_carbine" to 0L),
  val weaponPrestigeMap: Map<String, Int> = emptyMap(),
  val weaponTierMap: Map<String, Int> = ProgressionCatalog.weapons.associate { it.id to 1 },
  val weaponStarMap: Map<String, Int> = ProgressionCatalog.weapons.associate { it.id to 1 },
  val wonderTierMap: Map<String, Int> = WonderWeaponKind.entries.associate { it.id to 1 },
  val wonderStarMap: Map<String, Int> = WonderWeaponKind.entries.associate { it.id to 1 },
  val armoryItemStarMap: Map<String, Int> = emptyMap(),
  val gearMap: Map<String, GearState> = GearSlot.entries.associate { it.id to GearState(it) },
  val gearPrestige: Int = 0,
  val weaponCustom: WeaponCustomization = WeaponCustomization(),
  val wonderCustom: WonderWeaponCustomization = WonderWeaponCustomization(),
  val characterCustom: CharacterCustomization = CharacterCustomization(),
  val levelCustom: LevelCustomization = LevelCustomization(),
  val lobbyConfig: LobbyConfig = LobbyConfig(),
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
  val equipmentMap: Map<String, EquipmentState> = mapOf(
    "lethal" to EquipmentState("lethal", "lethal_frag", 1, 0, 0L, "standard", 1, 1),
    "tactical" to EquipmentState("tactical", "tactical_stim", 1, 0, 0L, "standard", 1, 1),
  ),
  val equippedWonderIds: List<String> = listOf(WonderWeaponKind.RAY_NOVA_MK3.id),
  val equippedLethalIds: List<String> = listOf("lethal_frag"),
  val equippedTacticalIds: List<String> = listOf("tactical_stim"),
  val redEssence: Long = 300L,
  val yellowEssence: Long = 300L,
  val greenEssence: Long = 300L,
  val combinedEssence: Long = 50L,
  val blastShards: Long = 20L,
  val perkRanks: Map<String, Int> = emptyMap(),
  val blastCoreInventory: Map<String, Long> = BlastCoreColor.entries.associate { it.id to 2L },
  val blastCoreLevels: Map<String, Int> = BlastCoreColor.entries.associate { it.id to 1 },
  val equippedOutfitId: String = "mrcrossover_v1",
  val outfitStates: Map<String, OutfitProgressState> = MultiverseArsenalCatalog.outfits.associate {
    it.id to OutfitProgressState(it.id)
  },
  val activeAreaId: String = "area_empire_city",
  val areaStates: Map<String, CustomAreaState> = MultiverseArsenalCatalog.customAreas.associate {
    it.id to CustomAreaState(it.id)
  },
  val vacuumBot: VacuumBotState = VacuumBotState(),
  val wonderWeaponsPickedTotal: Long = 0L,
  val claimedAchievements: Set<String> = emptySet(),
  val missionProgress: Map<String, Long> = emptyMap(),
  val claimedMissions: Set<String> = emptySet(),
  val totalElimsLifetime: Long = 0L,
  val totalBossesLifetime: Long = 0L,
  val totalMatchesPlayed: Long = 0L,
  val totalBlueEssenceSpent: Long = 0L,
  val highestPackTierReached: Int = 1,
  val largestLobbyPlayed: Int = 1,
  val storePurchasesCount: Int = 0,
  val purchasedStoreItems: Set<String> = emptySet(),
  val xpBoosterMatchesRemaining: Int = 0,
  val botDamageBoostPct: Int = 0,
  val privateMatchRules: PrivateMatchRules = PrivateMatchRules(),
  val friends: List<FriendEntry> = SocialCatalog.defaultFriends,
  val chatMessages: List<ChatMessage> = SocialCatalog.defaultChatMessages,
  val clan: ClanData = SocialCatalog.defaultClan,
  val joinedMatch: MultiverseMatchListing? = null,
  val outfitRarityMap: Map<String, Int> = emptyMap(),
  val outfitShardMap: Map<String, Long> = emptyMap(),
  val discoveredCatWorlds: Set<String> = emptySet(),
  val lootCratesOpenedTotal: Long = 0L,
  val activeNgPlusTier: Int = 0,
  val unlockedNgPlusTier: Int = 0,
  val ngPlusCompletions: Int = 0,
  val worldCollectionDifficultyMap: Map<String, String> = emptyMap(),
  val collectionCustomMode: CollectionCustomModeConfig = CollectionCustomModeConfig(),
  val customGameMaps: List<UserCustomGameMapSpec> = emptyList(),
  val customWeapons: List<UserCustomWeaponSpec> = emptyList(),
  val protectionRanks: Map<String, Int> = emptyMap(),
  val protectionStarMap: Map<String, Int> = emptyMap(),
  val perkStarMap: Map<String, Int> = emptyMap(),
  val customPerks: List<UserCustomPerkSpec> = emptyList(),
  val equippedCustomPerkIds: Set<String> = emptySet(),
  val gearCustomizations: Map<String, GearSlotCustomization> = emptyMap(),
  val customGear: List<UserCustomGearSpec> = emptyList(),
  val equippedCustomGearMap: Map<String, String> = emptyMap(),
  val customEquipment: List<UserCustomEquipmentSpec> = emptyList(),
  val blastShardTier: Int = 1,
  val companionBuffTier: Int = 1,
  val modeTierMap: Map<String, Int> = emptyMap(),
  val modeStarMap: Map<String, Int> = emptyMap(),
  val gold: Long = 1500L,
  val diamonds: Long = 350L,
  val tuuCash: Long = 10000L,
  val environmentalResources: Map<String, Long> = mapOf("crystal_shard" to 50L, "metal_scrap" to 120L, "energy_cell" to 40L),
  val magicState: MagicSystemState = MagicSystemState(),
  val playerBaseState: PlayerBaseState = PlayerBaseState(),
  val crystals: List<CrystalItem> = defaultCrystals(),
) {
  val playerLevel: Int
    get() = ProgressionCatalog.levelForXp(playerXp)

  fun canHealWithThoughtAndSpirit(): Boolean {
    val summary = computePowerAndLightLevel()
    return summary.powerLevel >= 300 || playerLevel >= 2
  }

  val playerRankTitle: String
    get() = ProgressionCatalog.rankTitleForLevel(playerLevel, playerPrestige)

  val ngPlusSpec: NewGamePlusVersion
    get() = NewGamePlusVersion.fromTier(activeNgPlusTier)

  val ngPlusRewardMultiplier: Float
    get() = if (activeNgPlusTier <= 0 && ngPlusCompletions <= 0) 1.0f
    else ngPlusSpec.rewardMultiplier * (1f + ngPlusCompletions * 0.25f)

  fun collectionDifficulty(worldId: String): CollectionDifficulty =
    CollectionDifficulty.fromId(worldCollectionDifficultyMap[worldId] ?: CollectionDifficulty.OPERATIVE.id)

  fun combinedGameplayRewardMultiplier(worldId: String): Float =
    ngPlusRewardMultiplier * collectionDifficulty(worldId).rewardMultiplier

  fun weaponXp(weaponId: String): Long = weaponXpMap[weaponId] ?: 0L

  fun weaponLevel(weaponId: String): Int = ProgressionCatalog.weaponLevelForXp(weaponXp(weaponId))

  fun weaponPrestige(weaponId: String): Int = weaponPrestigeMap[weaponId] ?: 0

  fun weaponTier(weaponId: String): Int = (weaponTierMap[weaponId] ?: 1).coerceIn(1, 25)

  fun weaponStar(weaponId: String): Int = (weaponStarMap[weaponId] ?: 1).coerceIn(1, StarRankMeta.MAX_STARS)

  fun wonderTier(wonderId: String): Int = (wonderTierMap[wonderId] ?: 1).coerceIn(1, 25)

  fun wonderStar(wonderId: String): Int = (wonderStarMap[wonderId] ?: 1).coerceIn(1, StarRankMeta.MAX_STARS)

  fun armoryItemStar(itemId: String): Int = (armoryItemStarMap[itemId] ?: 1).coerceIn(1, StarRankMeta.MAX_STARS)

  fun modeTier(modeId: String): Int = (modeTierMap[modeId] ?: 1).coerceIn(1, PowerTier.MAX_TIER)

  fun modeStar(modeId: String): Int = (modeStarMap[modeId] ?: 1).coerceIn(1, StarRankMeta.MAX_STARS)

  fun equipmentState(slotId: String): EquipmentState =
    equipmentMap[slotId] ?: EquipmentState(slotId, if (slotId == "lethal") "lethal_frag" else "tactical_stim", 1, 0, 0L, "standard", 1, 1)

  fun outfitState(id: String = equippedOutfitId): OutfitProgressState =
    outfitStates[id] ?: OutfitProgressState(id)

  fun activeOutfitSpec(): HeroOutfitSpec =
    MultiverseArsenalCatalog.outfitById(equippedOutfitId)

  fun areaState(id: String = activeAreaId): CustomAreaState =
    areaStates[id] ?: CustomAreaState(id)

  fun activeAreaSpec(): CustomAreaSpec =
    resolveAreaSpec(activeAreaId)

  fun resolveAreaSpec(areaId: String): CustomAreaSpec =
    CuratedCustomMapsCatalog.resolveAreaSpec(areaId, customGameMaps)

  fun allPlayableAreas(): List<CustomAreaSpec> =
    CuratedCustomMapsCatalog.allPlayableAreas(customGameMaps)

  fun allWeapons(): List<WeaponSpec> =
    ProgressionCatalog.allWeapons(customWeapons)

  fun resolveWeaponSpec(weaponId: String): WeaponSpec =
    ProgressionCatalog.weaponById(weaponId, customWeapons)

  fun customWeaponById(weaponId: String): UserCustomWeaponSpec? =
    customWeapons.firstOrNull { it.id == weaponId }

  fun upgradedAreaCount(): Int {
    val upgradedIds = areaStates.values.count { st ->
      st.powerGridTier > 1 ||
        st.defenseTurretTier > 1 ||
        st.trapDensityTier > 1 ||
        st.lootRichnessTier > 1 ||
        st.starLevel > 1 ||
        st.rebirthCycles > 0 ||
        st.ageYears != 12
    }
    return upgradedIds.coerceIn(0, EssenceResonanceBonuses.MAX_SYNERGY_MAPS)
  }

  fun totalAreaTierPoints(): Int =
    areaStates.values.sumOf { st ->
      (st.powerGridTier - 1) +
        (st.defenseTurretTier - 1) +
        (st.trapDensityTier - 1) +
        (st.lootRichnessTier - 1) +
        (st.starLevel - 1) * 2 +
        (st.rebirthCycles * 3)
    }

  fun defaultModeForArea(areaId: String, currentMode: String? = null): String {
    val customSpec = CuratedCustomMapsCatalog.findById(areaId, customGameMaps)
    if (customSpec != null) {
      return customSpec.gameCategory.defaultModeId
    }
    val spec = MultiverseArsenalCatalog.areaById(areaId)
    val world = UniverseCatalog.worldById(spec.linkedWorldId)
    return when {
      areaId.startsWith("area_bo3_") || areaId == "area_zombies_der_riese" -> "zombies"
      areaId == "area_campaign_nexus" || areaId == "area_mended_convergence" -> "campaign"
      world.defaultModeId.isNotBlank() -> world.defaultModeId
      currentMode != null && currentMode in world.supportedModes -> currentMode
      else -> "zombies"
    }
  }

  fun outfitRarity(id: String = equippedOutfitId): CharacterRarity {
    val defaultRank = if (MultiverseArsenalCatalog.outfitById(id).isOriginalCharacter) CharacterRarity.RARE.rank else CharacterRarity.COMMON.rank
    return CharacterRarity.fromRank(outfitRarityMap[id] ?: defaultRank)
  }

  fun outfitShards(id: String = equippedOutfitId): Long =
    (outfitShardMap[id] ?: 0L).coerceAtLeast(0L)

  fun computePowerAndLightLevel(totalBlueEssence: Long = 0L): PowerLightSummary {
    val wpnId = weaponCustom.equippedWeaponId
    val wpnScore = (weaponLevel(wpnId) * 4) + (weaponTier(wpnId) * 18) + (weaponStar(wpnId) * 15) + (weaponPrestige(wpnId) * 22)
    val wonderScore = effectiveWonderList().sumOf { wId -> (wonderTier(wId) * 16) + (wonderStar(wId) * 12) } /
      effectiveWonderList().size.coerceAtLeast(1)
    val gearScore = gearMap.values.sumOf { g -> (g.level * 2) + (g.tier * 8) + (g.starLevel * 8) + (g.prestige * 12) } /
      gearMap.size.coerceAtLeast(1)
    val outfit = outfitState(equippedOutfitId)
    val rarity = outfitRarity(equippedOutfitId)
    val outfitScore = (outfit.level / 4) + (outfit.tier * 14) + (outfit.starLevel * 14) + (rarity.rank * 35)
    val charScore = (ProgressionCatalog.milestoneTierCount(playerLevel) * 24) + (characterTier * 15) + (characterStarLevel * 14) + (playerPrestige * 20)
    val powerLevel = (300 + wpnScore + wonderScore + gearScore + outfitScore + charScore).coerceAtLeast(300)
    val lightBonus = (perkRanks.values.sum() * 4) + (blastCoreLevels.values.sum() * 3) +
      ((totalBlueEssence / 500L).toInt().coerceAtMost(80)) + (rarity.rank * 28) + (discoveredCatWorlds.size * 15)
    val lightLevel = (powerLevel + 240 + lightBonus).coerceAtLeast(540)
    val label = when {
      lightLevel >= 2200 -> "OMEGA LIGHT"
      lightLevel >= 1700 -> "PINNACLE CAP"
      lightLevel >= 1300 -> "ASCENDANT LIGHT"
      lightLevel >= 950 -> "VANGUARD LIGHT"
      else -> "RISEN LIGHT"
    }
    return PowerLightSummary(powerLevel = powerLevel, lightLevel = lightLevel, tierLabel = label)
  }

  fun perkRank(perkId: String): Int = (perkRanks[perkId] ?: 0).coerceIn(0, 25)

  fun perkStar(perkId: String): Int = (perkStarMap[perkId] ?: 1).coerceIn(1, StarRankMeta.MAX_STARS)

  fun protectionRank(protId: String): Int = (protectionRanks[protId] ?: 0).coerceIn(0, 25)

  fun protectionStar(protId: String): Int = (protectionStarMap[protId] ?: 1).coerceIn(1, StarRankMeta.MAX_STARS)

  fun gearCustomization(slot: GearSlot): GearSlotCustomization =
    gearCustomizations[slot.id] ?: GearSlotCustomization(slot.id)

  fun equippedCustomGear(slot: GearSlot): UserCustomGearSpec? {
    val id = equippedCustomGearMap[slot.id] ?: return null
    return customGear.firstOrNull { it.id == id }
  }

  fun resolveEquipmentSpec(itemId: String): EquipmentItemSpec =
    ProgressionCatalog.resolveEquipmentItem(itemId, customEquipment)

  fun customEquipmentById(itemId: String): UserCustomEquipmentSpec? =
    customEquipment.firstOrNull { it.id == itemId }

  fun blastCoreCount(color: BlastCoreColor): Long = (blastCoreInventory[color.id] ?: 0L).coerceAtLeast(0L)

  fun blastCoreLevel(color: BlastCoreColor): Int = (blastCoreLevels[color.id] ?: 1).coerceIn(1, 20)

  fun essenceResonance(blueEssence: Long): EssenceResonanceBonuses =
    comprehensiveBonuses(blueEssence = blueEssence)

  fun comprehensiveBonuses(
    blueEssence: Long,
    armoryItems: List<ArmoryItem> = emptyList(),
    region: WorldRegion = WorldRegion.NA_EAST,
    server: RegionalServerSpec? = null,
  ): EssenceResonanceBonuses {
    val activeRegionBonus = region.defaultProfile
    val srvDmg = activeRegionBonus.damageBonusMult * (server?.bonusDamageMult ?: 1.0f)
    val srvShd = activeRegionBonus.shieldBonusMult * (server?.bonusShieldMult ?: 1.0f)
    val srvRate = activeRegionBonus.fireRateBonusMult * (server?.bonusRateMult ?: 1.0f)
    val activeOutfit = outfitState(equippedOutfitId)
    val activeRarity = outfitRarity(equippedOutfitId)
    val customBuffTiers = customPerks.sumOf { (it.tier - 1) + (it.starLevel - 1) } +
      customGear.sumOf { (it.tier - 1) + (it.starLevel - 1) } +
      customEquipment.sumOf { (it.tier - 1) + (it.starLevel - 1) } +
      gearCustomizations.size
    return EssenceResonanceBonuses.computeComprehensive(
      blueEssence = blueEssence,
      blueEssenceSpent = totalBlueEssenceSpent,
      redEssence = redEssence,
      yellowEssence = yellowEssence,
      greenEssence = greenEssence,
      combinedEssence = combinedEssence,
      blastShards = blastShards,
      upgradedMapCount = upgradedAreaCount(),
      totalMapTierPoints = totalAreaTierPoints(),
      armoryModCount = armoryItems.size,
      armoryModTotalTiers = armoryItems.sumOf { it.tier },
      armoryModTotalPower = armoryItems.sumOf { it.powerValue },
      weaponTierSum = weaponTierMap.values.sumOf { (it - 1).coerceAtLeast(0) },
      weaponStarSum = weaponStarMap.values.sumOf { (it - 1).coerceAtLeast(0) },
      customWeaponCount = customWeapons.size,
      wonderTierSum = wonderTierMap.values.sumOf { (it - 1).coerceAtLeast(0) },
      gearTierSum = gearMap.values.sumOf { (it.tier - 1).coerceAtLeast(0) },
      gearLevelSum = gearMap.values.sumOf { (it.level - 1).coerceAtLeast(0) },
      equipmentTierSum = equipmentMap.values.sumOf { (it.tier - 1).coerceAtLeast(0) },
      skillTierSum = skillNodes.values.sumOf { (if (it.unlocked) it.tier else 0) },
      outfitTierSum = (activeOutfit.tier - 1) + (activeOutfit.starLevel - 1) + (activeOutfit.gearRank - 1),
      outfitRarityRank = (activeRarity.rank - 1).coerceAtLeast(0),
      perkRankSum = perkRanks.values.sum(),
      blastCoreLevelSum = blastCoreLevels.values.sumOf { (it - 1).coerceAtLeast(0) },
      clanLevel = clan.level,
      ngPlusTier = activeNgPlusTier,
      protectionRankSum = protectionRanks.values.sum() + protectionStarMap.values.sumOf { (it - 1).coerceAtLeast(0) },
      perkStarSum = perkStarMap.values.sumOf { (it - 1).coerceAtLeast(0) },
      customBuffTierSum = customBuffTiers,
      blastShardTier = blastShardTier,
      companionBuffTier = companionBuffTier,
      serverDamageMult = srvDmg,
      serverShieldMult = srvShd,
      serverFireRateMult = srvRate,
    )
  }

  fun effectiveWeaponList(): List<String> =
    weaponCustom.equippedWeaponIds.ifEmpty { listOf(weaponCustom.equippedWeaponId) }.distinct()

  fun effectiveWonderList(): List<String> =
    equippedWonderIds.ifEmpty { listOf(WonderWeaponKind.RAY_NOVA_MK3.id) }.distinct()

  fun effectiveMatchRules(): PrivateMatchRules {
    val jm = joinedMatch
    if (jm != null) {
      val muts = jm.activeMutators
      return PrivateMatchRules(
        isPrivate = jm.isPrivate,
        privacy = jm.privacy,
        roomName = jm.roomName,
        roomCode = jm.roomCode,
        passcode = jm.passcode ?: "",
        startingWave = jm.currentWave.coerceAtLeast(1),
        maxPlayers = jm.maxPlayers,
        botFillEnabled = lobbyConfig.autoFillBots,
        botDifficulty = lobbyConfig.botDifficulty,
        worldId = jm.worldId,
        areaId = jm.areaId,
        modeId = jm.modeId,
        infiniteAmmo = muts.any { it.contains("Infinite", ignoreCase = true) } || (jm.roomCode == privateMatchRules.roomCode && privateMatchRules.infiniteAmmo),
        headshotsOnly = muts.any { it.contains("Instagib", ignoreCase = true) || it.contains("Headshot", ignoreCase = true) } || (jm.roomCode == privateMatchRules.roomCode && privateMatchRules.headshotsOnly),
        bossRush = muts.any { it.contains("Boss Rush", ignoreCase = true) } || (jm.roomCode == privateMatchRules.roomCode && privateMatchRules.bossRush),
        wonderSpawnBoost = muts.any { it.contains("Wonder Boost", ignoreCase = true) } || (jm.roomCode == privateMatchRules.roomCode && privateMatchRules.wonderSpawnBoost),
        lowGravity = muts.any { it.contains("Low Gravity", ignoreCase = true) } || (jm.roomCode == privateMatchRules.roomCode && privateMatchRules.lowGravity),
        doubleSpeed = muts.any { it.contains("2x Speed", ignoreCase = true) } || (jm.roomCode == privateMatchRules.roomCode && privateMatchRules.doubleSpeed),
        vampiricSiphon = muts.any { it.contains("Vampiric", ignoreCase = true) } || (jm.roomCode == privateMatchRules.roomCode && privateMatchRules.vampiricSiphon),
        doubleBlueEssence = muts.any { it.contains("2x Blue Essence", ignoreCase = true) } || (jm.roomCode == privateMatchRules.roomCode && privateMatchRules.doubleBlueEssence),
        friendlyFire = muts.any { it.contains("Hardcore", ignoreCase = true) } || (jm.roomCode == privateMatchRules.roomCode && privateMatchRules.friendlyFire),
        customWaveGoal = if (jm.roomCode == privateMatchRules.roomCode) privateMatchRules.customWaveGoal else 0,
      )
    }
    return if (privateMatchRules.isPrivate) privateMatchRules else PrivateMatchRules(isPrivate = false)
  }
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
    WeaponSpec(
      id = "wpn_arc_amp",
      name = "Empire Conduit Amp",
      universeOrigin = "Cole McGrath",
      summary = "Cole's electric Amp blade launcher firing high-voltage cyan shock arcs.",
      baseDamage = 3,
      baseCadenceTicks = 9.0f,
      baseProjectileSpeed = 0.026f,
      baseSpreadCount = 2,
      basePierce = 2,
      defaultTracerHex = 0xFF00E5FF,
    ),
    WeaponSpec(
      id = "wpn_blacklight_carbine",
      name = "Prototype Bio-Carbine",
      universeOrigin = "Prototype",
      summary = "Mutated BlacklightTendril Shard rifle tearing through bio-armored swarms.",
      baseDamage = 3,
      baseCadenceTicks = 9.5f,
      baseProjectileSpeed = 0.023f,
      baseSpreadCount = 2,
      basePierce = 2,
      defaultTracerHex = 0xFFFF1744,
    ),
    WeaponSpec(
      id = "wpn_stargate_staff",
      name = "Space & Time Staff Weapon",
      universeOrigin = "Space & Time",
      summary = "Original Stargate Ma'Tok plasma staff launching piercing golden bolts.",
      baseDamage = 4,
      baseCadenceTicks = 12.0f,
      baseProjectileSpeed = 0.024f,
      baseSpreadCount = 1,
      basePierce = 3,
      defaultTracerHex = 0xFFFFD54F,
    ),
    WeaponSpec(
      id = "wpn_olympian_spear",
      name = "Crucible Spartan Spear",
      universeOrigin = "TUU God of War",
      summary = "Draupnir-forged throwing spears blasting molten rings across bosses.",
      baseDamage = 4,
      baseCadenceTicks = 13.0f,
      baseProjectileSpeed = 0.022f,
      baseSpreadCount = 2,
      basePierce = 2,
      defaultTracerHex = 0xFFFF6D00,
    ),
    WeaponSpec(
      id = "wpn_ship_phaser",
      name = "Ship of Heroes Twin Phaser",
      universeOrigin = "Ship of Heroes",
      summary = "High-rate Apotheosis orbital pulse phaser with wide triple-beam spread.",
      baseDamage = 2,
      baseCadenceTicks = 7.8f,
      baseProjectileSpeed = 0.027f,
      baseSpreadCount = 3,
      basePierce = 1,
      defaultTracerHex = 0xFFE040FB,
    ),
  )

  fun allWeapons(customWeapons: List<UserCustomWeaponSpec> = emptyList()): List<WeaponSpec> =
    (weapons + customWeapons.map { it.toWeaponSpec() }).distinctBy { it.id }

  fun weaponById(id: String, customWeapons: List<UserCustomWeaponSpec> = emptyList()): WeaponSpec =
    customWeapons.firstOrNull { it.id == id }?.toWeaponSpec()
      ?: weapons.firstOrNull { it.id == id }
      ?: weapons.first()

  val camos: List<Pair<String, String>> = listOf(
    "cyber_matte" to "Cyber Matte",
    "molten_damascus" to "Molten Damascus",
    "pack_tier_gold" to "Pack-a-Tier Gold",
    "blacklight_neon" to "Blacklight Neon",
    "chrono_chrome" to "Chrono Chrome",
    "omega_prismatic" to "Omega Celestial",
  )

  val optics: List<Pair<String, String>> = listOf(
    "iron_sight" to "Precision Ring Sight",
    "holo_cyan" to "Holo Cyan Diamond",
    "isac_thermal" to "ISAC Thermal Targeting Optic",
    "starblade_reflex" to "Twin-Core Reflex Sight",
    "arkham_detective_optic" to "Batman Arkham Detective Sonar Optic (+35% Crit & Tracking)",
    "cod_thermal_hybrid" to "Call of Duty Thermal Hybrid Scope (+25% Precision Zoom)",
    "infamous_amp_lens" to "inFamous Conduit Amp Focus Lens (+Electric Arc Flash)",
    "prototype_viral_sight" to "Prototype Biomass Thermal Eye (+Vampiric Bio Leech)",
    "superman_telescopic" to "Superman Kryptonian Telescopic Heat Visor (+40% Velocity)",
    "dungeon_abyss_lens" to "Abyss of Dungeons Nether Sight (Penetrates Armor)",
  )

  val barrels: List<Pair<String, String>> = listOf(
    "standard_barrel" to "Standard Barrel",
    "overclocked_rifling" to "Overclocked (+22% Bolt Speed)",
    "spread_choke" to "Spread Choke (+1 Extra Bolt)",
    "heavy_bore" to "Heavy Bore (+2 Base Damage)",
    "gta4_sawed_heavy" to "GTA IV South Bohan Sawed-Off Barrel (+2 Spread, +3 Close Damage)",
    "cod_taskforce_barrel" to "Call of Duty Task Force Heavy Barrel (+45% Velocity, +2 Dmg)",
    "arkham_disruptor_barrel" to "Arkham Origins Batclaw Disruptor Barrel (+Armor Shred)",
    "infamous_conduit_barrel" to "Empire City Conduit Rail Barrel (+Chain Voltage Stagger)",
    "prototype_blade_barrel" to "Prototype Biomass Arm-Blade Barrel (+3 Dmg, +2 Pierce)",
    "superman_solar_bore" to "Superman Solar Charged Core Barrel (+Solar Plasma Beam)",
    "abyss_nether_barrel" to "Abyss of Dungeons Netherflame Barrel (+Void Burn & Pierce)",
  )

  val muzzles: List<Pair<String, String>> = listOf(
    "standard_muzzle" to "Standard Flash Hider",
    "tactical_brake" to "Tactical Muzzle Brake",
    "monolithic_suppressor" to "Monolithic Suppressor (+1 Dmg, Stealth)",
    "heavy_compensator" to "Heavy Recoil Compensator (+1 Dmg)",
    "cod_colossus_suppressor" to "Call of Duty Colossus Silencer (+Suppression, +2 Dmg)",
    "gta_street_brake" to "GTA IV Street Combat Muzzle Brake (+Recoil Control)",
    "arkham_baffled_choke" to "Batman WayneTech Baffled Acoustic Choke (+Stagger)",
    "infamous_shock_choke" to "inFamous Conduit Lightning Choke (+Concussion Shock)",
    "prototype_tendril_muzzle" to "Prototype Viral Tendril Muzzle (+Shield Siphon on Hit)",
    "abyss_eldritch_muzzle" to "Abyss of Dungeons Eldritch Void Muzzle (+Dissolves Armor)",
  )

  val underbarrels: List<Pair<String, String>> = listOf(
    "none_underbarrel" to "No Attachment",
    "foregrip_vertical" to "Vertical Foregrip",
    "ranger_grip" to "Ranger Stabilizer",
    "bipod_mount" to "Heavy Bipod Mount",
    "arkham_grapnel_launcher" to "Batman Arkham Grapnel Underbarrel (+Tethers Hostiles)",
    "cod_masterkey_shotgun" to "Call of Duty Underbarrel Masterkey (+Shotgun Secondary Spread)",
    "infamous_kinetic_pulse" to "inFamous Cole McGrath Kinetic Concussion Pad (+60% Knockback)",
    "prototype_whip_spool" to "Prototype Alex Mercer Biomass Whip Spool (+Shield Siphon)",
    "superman_krypton_grip" to "Superman Sunstone Ergonomic Foregrip (+Stable Recoil)",
    "abyss_skull_lantern" to "Abyss of Dungeons Soul Lantern (+Reveals Boss Weakspots)",
  )

  val magazines: List<Pair<String, String>> = listOf(
    "extended_mag_1" to "Standard Mag",
    "extended_mag_2" to "Extended Drum Mag (+Spread)",
    "fast_mag" to "Speed Reload Mag",
    "composite_mag" to "Composite Energy Cell",
    "cod_60_drum" to "Call of Duty 60-Round Heavy Drum (+Spread & High Fire Rate)",
    "gta_drum_extended" to "GTA IV Liberty City High-Capacity Drum (+2 Spread)",
    "arkham_shock_mag" to "Batman Batarang Electric Battery (+Arc Voltage Discharge)",
    "prototype_biomass_sack" to "Prototype Biomass Storage Reserve (+Max Shield Capacity)",
    "superman_crystal_cell" to "Fortress of Solitude Sunstone Battery (+Rapid Cadence)",
    "abyss_soul_well" to "Abyss of Dungeons Soul Well (+Shield Restoration on Kill)",
  )

  val stocks: List<Pair<String, String>> = listOf(
    "tactical_stock" to "Tactical Stock",
    "heavy_padded_stock" to "Heavy Padded Buffer",
    "lightweight_skeleton" to "Skeletonized Dash Stock",
    "cod_no_stock" to "Call of Duty No Stock (+25% Sprint & Movement Velocity)",
    "arkham_glide_stock" to "Batman Arkham Cape-Stabilized Tactical Stock (Zero Recoil)",
    "infamous_conduit_pad" to "inFamous Conduit Grounding Buffer (Extra Electric Resist)",
    "prototype_armored_stock" to "Prototype Biomass Armored Shield Stock (+30 Max Shield)",
    "superman_redcape_stock" to "Superman Kryptonian Aerodynamic Stock (+High Speed)",
    "abyss_crypt_stock" to "Abyss of Dungeons Crypt Bone Stock (+Dark Magic Stability)",
  )

  val rearGrips: List<Pair<String, String>> = listOf(
    "stippled_grip" to "Stippled Combat Grip",
    "rubberized_grip" to "Rubberized Recoil Grip",
    "tactical_tape" to "Tactical Friction Tape",
    "cod_granulated_grip" to "Call of Duty Granulated Stippled Grip Tape",
    "arkham_batarang_grip" to "Batman WayneTech Carbon Fiber Batarang Grip (+1 Pierce)",
    "prototype_tendril_wrap" to "Prototype Biomass Tendril Wrap (+Shield Siphon)",
    "infamous_grounding_grip" to "inFamous Conduit Rubberized Insulator Grip",
    "superman_steel_grip" to "Superman Man of Steel Reinforced Grip (+Unshakable Aim)",
    "abyss_nether_wrap" to "Abyss of Dungeons Nether-Cloth Wrap (+Cursed Damage)",
  )

  val lasers: List<Pair<String, String>> = listOf(
    "none_laser" to "No Laser",
    "tactical_laser_5mw" to "5mW Tactical Laser",
    "infrared_designator" to "IR Target Designator",
    "cod_tac_laser" to "Call of Duty 5mW Green Tac Laser (+Spread Focus & Velocity)",
    "arkham_sonar_laser" to "Batman Arkham Detective Sonar Laser (Reveals Hostiles)",
    "infamous_arc_laser" to "inFamous Conduit Arc Tracer Laser (+Shock Arc)",
    "prototype_viral_laser" to "Prototype Blacklight Crimson Tracer Laser (+Armor Pen)",
    "superman_heat_beam_guide" to "Superman Kryptonian Heat-Vision Guide Laser (+2 Dmg)",
    "abyss_spectral_laser" to "Abyss of Dungeons Spectral Nether Guide (+Pierces Walls)",
  )

  val wonderCores: List<Pair<String, String>> = listOf(
    "core_standard_wonder" to "Standard Quantum Reactor",
    "core_pap_mk2" to "Call of Duty Pack-a-Punch Infusion Core (+100% Wonder Splash & 2x Dmg)",
    "core_elemental_prism" to "Origins Elemental Prism Matrix (Infuses Wonder with All Elements)",
    "core_apotheicon_eye" to "Shadows of Evil Apothicon Rift Singularity (Gravitational Black Hole)",
    "core_stargate_naquadah" to "Stargate Naquadah Reactor (Heavy Piercing Shockwave)",
    "core_abyss_eldritch" to "Abyss of Dungeons Eldritch Heart (Siphons 15% Shield & +3 Pierce)",
    "core_kryptonian_matrix" to "Superman Kryptonian Sunstone (Blinding Solar Flare & Plasma)",
    "core_arkham_freeze" to "Batman Arkham Mr. Freeze Cryo Core (Instant Flash Freeze)",
    "core_infamous_ray_core" to "inFamous Ray Sphere Core (+5 Chain Lightning Arcs)",
    "core_prototype_hivemind" to "Prototype Hivemind Core (Vaporizes Waves into Biomass Bombs)",
  )

  val wonderLenses: List<Pair<String, String>> = listOf(
    "lens_standard_wonder" to "Standard Focusing Ring",
    "lens_diffraction_fan" to "Origins 4-Way Splitter Fan (+4 Projectiles in Wide Arc)",
    "lens_collimated_beam" to "Treyarch Collimated Ray Beam (+50% Velocity & Infinite Pierce)",
    "lens_vortex_chamber" to "Apothicon Singularity Vortex Emitter (Draws Enemies into Zone)",
    "lens_cryo_subzero" to "Origins Glacial Sub-Zero Focusing Lens (Freezes All Surrounding)",
    "lens_superman_heat_aperture" to "Superman Kryptonian Heat Aperture (Supercharged Solar Beam)",
    "lens_abyss_void_choke" to "Abyss of Dungeons Nether Choke (Expands Void Radius by 80%)",
  )

  val wonderAmplifiers: List<Pair<String, String>> = listOf(
    "amp_standard_wonder" to "Standard Battery Buffer",
    "amp_chronos_overdrive" to "Chrono Multiverse Overdrive (+60% Wonder Active Duration)",
    "amp_void_resonance" to "Abyss Void Resonator (Chain Lightning & Fire Explosions on Kill)",
    "amp_superman_fortress" to "Superman Fortress Aegis (Invulnerability Shielding during Volley)",
    "amp_arkham_disruptor" to "Batman Arkham Disruptor Amplifier (Staggers Boss Special Attacks)",
    "amp_cod_wunder_grid" to "Call of Duty Wunderwaffe Power Grid (+4 Additional Arc Chains)",
    "amp_prototype_tendril_harness" to "Prototype Tendril Harness (Restores Shield on Wonder Pickup)",
  )

  val ammoMods: List<Pair<String, String>> = listOf(
    "activision_treyarch_omni" to "Activision & Treyarch Omnistrike Core (Deals All Ammo & Damage Types)",
    "rocksteady_wb_arkham" to "Rocksteady & WB Arkham Heavy Kinetic Slugs (Kinetic)",
    "insomniac_spider_quantum" to "Insomniac Spider-Verse Quantum Web Ammo (Energy)",
    "suckerpunch_infamous_conduit" to "Sucker Punch Infamous & Prototype Conduction Slugs (Electric)",
    "bioware_ea_mass_plasma" to "BioWare & EA Mass Effect Thermal Plasma Core (Thermal/Fire)",
    "remedy_valve_abyss_sand" to "Remedy & Valve Abyss Sand-Storm Desolator (Void & Sand)",
    "standard_core" to "Standard Kinetic Hard-Cast (Kinetic)",
    "ap_sabot" to "AP Tungsten Sabot Round (Kinetic)",
    "rock_slug" to "Tectonic Rock Crushing Sabot (Kinetic/Rock)",
    "cryo_stasis" to "Cryo-Freeze Stasis Sabot (Cryo/Ice)",
    "nitrogen_subzero" to "Liquid Nitrogen Sub-Zero Subslug (Cryo/Ice)",
    "arc_chain" to "Arc Wire Chain Lightning (Energy/Electric)",
    "tesla_hv" to "Tesla High-Frequency Ion Cell (Electric)",
    "vampiric_bio" to "Blacklight Leech Bio-Capsule (Bio/Toxic)",
    "acid_venom" to "Acidic Venom Corrosive Slime (Toxic)",
    "incendiary_core" to "Thermal Napalm Burn Core (Thermal/Fire)",
    "plasma_scorched" to "Scorched Plasma Incendiary (Thermal/Fire)",
    "sand_abrasive" to "Quicksand Abrasive Dust Rounds (Sand)",
    "desert_glass" to "Desert Glass Tungsten-Sand Composite (Sand)",
    "explosive_he" to "High-Explosive Frag Core (Explosive AoE)",
    "cod_explosive_shells" to "CoD Explosive Breacher Rounds (Explosive AoE)",
    "tectonic_rock" to "Tectonic Boulder Rounds (Rock Armor Shatter)",
    "void_singularity" to "Dark Matter Singularity Shards (Void)",
  )

  val suitThemes: List<Triple<String, String, Pair<Long, Long>>> = listOf(
    Triple("vanguard_cyber", "Vanguard Cyber", 0xFF00E5FF to 0xFFFFAB40),
    Triple("blackcell_gold", "Blackcell Gold", 0xFFFFD54F to 0xFFFFFFFF),
    Triple("blacklight_red", "Blacklight Crimson", 0xFFFF3D71 to 0xFF7C4DFF),
    Triple("chrono_emerald", "Chrono Emerald", 0xFF00E676 to 0xFF448AFF),
    Triple("starblade_royal", "Starblade Void", 0xFFE040FB to 0xFF00E5FF),
    Triple("omega_ascendant", "Omega Celestia", 0xFFFFFFFF to 0xFF00E5FF),
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
    // Core + Existing 14 IDs preserved
    SkillNode("sk_shield_1", "Reinforced Chassis I", "+25 Max Shield per Tier (up to T25: +625 Shield).", "SURVIVAL", 300L, false, 1, 25, inspirationTag = "The Division SHD"),
    SkillNode("sk_shield_2", "Reinforced Chassis II", "+35 Max Shield & +1 Regen/sec per Tier (up to T25).", "SURVIVAL", 600L, false, 1, 25, inspirationTag = "Warframe / Destiny"),
    SkillNode("sk_damage_1", "Ballistic Lethality I", "+2 Bolt Damage per Tier (up to T25: +50 DMG).", "COMBAT", 350L, false, 1, 25, inspirationTag = "Call of Duty Zombies"),
    SkillNode("sk_damage_2", "Ballistic Lethality II", "+3 Bolt Damage & +1 Pierce every 2 Tiers (T1-T25).", "COMBAT", 750L, false, 1, 25, inspirationTag = "Doom Eternal"),
    SkillNode("sk_cooldown_1", "Overdrive Capacitor", "+12% Cooldown Haste & +3% Move Speed per Tier.", "ENGINEERING", 450L, false, 1, 25, inspirationTag = "inFAMOUS / Control"),
    SkillNode("sk_cooldown_2", "Temporal Flux Relay", "+14% Cooldown Haste & +5% Fire Rate per Tier.", "ENGINEERING", 650L, false, 1, 25, inspirationTag = "Chrono / Force Unleashed"),
    SkillNode("sk_wonder_1", "Mystery Crate Attractor", "+30% Wonder Drop Rate & +15% Wonder Duration per Tier.", "WONDER", 550L, false, 1, 25, inspirationTag = "BO3 / MW Zombies"),
    SkillNode("sk_protection_1", "Titanium Reactive Plate", "+3.5% Incoming Damage Protection & +20 Shield per Tier.", "PROTECTION", 400L, false, 1, 25, inspirationTag = "Batman Arkham"),
    SkillNode("sk_protection_2", "Multiverse Aegis Ward", "+4% Elemental/Explosive/Rock/Sand DR & +1.5 Siphon/Tier.", "PROTECTION", 650L, false, 1, 25, inspirationTag = "God of War / Hades"),
    SkillNode("sk_explosive_1", "Ordnance & Seismic Mastery", "+12% Explosive, Rock & Sand Damage + Blast Splash per Tier.", "COMBAT", 500L, false, 1, 25, inspirationTag = "GTA IV / Prototype"),
    SkillNode("sk_perk_amp_1", "Global Perk Overcharger", "+8% Effectiveness to ALL Perks & Custom Perks per Tier.", "SURVIVAL", 550L, false, 1, 25, inspirationTag = "BO3 GobbleGum / Sunset"),
    SkillNode("sk_buff_mastery_1", "Omniverse Buff Resonance", "+6% Power to ALL Buffs, Cores, Shards & Companions per Tier.", "WONDER", 600L, false, 1, 25, inspirationTag = "Control / Warframe"),
    SkillNode("sk_sand_rock_1", "Tectonic Dune Shredder", "+3 Armor Shred & +8% Stagger/Knockback per Tier.", "COMBAT", 480L, false, 1, 25, inspirationTag = "Abyss / Dark & Darker"),
    SkillNode("sk_siphon_1", "Blacklight Shield Siphon", "+3 Kill Shield Siphon & +1.2/sec Regen per Tier.", "PROTECTION", 520L, false, 1, 25, inspirationTag = "Prototype / Bloodborne"),
    // Expanded Inspired Collection & Spiritual Successor Skills (22 new nodes -> 36 total)
    SkillNode("sk_zombies_gobble", "Aether GobbleGum Alchemy", "+5% Essence Gain & +18% Wonder Drop Chance per Tier.", "WONDER", 420L, false, 1, 25, inspirationTag = "BO3 Zombies"),
    SkillNode("sk_zombies_deadbolt", "Deadbolt Exfil Mastery", "+12 Max Shield & +4% Fire Rate per Tier during Waves.", "SURVIVAL", 440L, false, 1, 25, inspirationTag = "MW Zombies"),
    SkillNode("sk_division_isac", "ISAC Smart Tech Support", "-5% All Cooldowns & +0.8/sec Armor Regen per Tier.", "ENGINEERING", 460L, false, 1, 25, inspirationTag = "The Division"),
    SkillNode("sk_infamous_karmic", "Karmic Conduit Overload", "+2 Chain Voltage DMG & +3% Sprint Speed per Tier.", "COMBAT", 430L, false, 1, 25, inspirationTag = "inFAMOUS / Sunset Overdrive"),
    SkillNode("sk_prototype_biomass", "Blacklight Tendril Devour", "+20 Max Shield & +2 Shield Siphon on Kill per Tier.", "SURVIVAL", 480L, false, 1, 25, inspirationTag = "Prototype / Crackdown"),
    SkillNode("sk_arkham_freeflow", "Freeflow Critical Combo", "+8% Crit Damage & +2.5% Evade Speed per Tier.", "COMBAT", 410L, false, 1, 25, inspirationTag = "Batman Arkham / Shadow of War"),
    SkillNode("sk_arkham_predator", "Detective Sonar Disruptor", "+1 Bolt Pierce every 4 Tiers & +14% Trap Bonus per Tier.", "ENGINEERING", 450L, false, 1, 25, inspirationTag = "Arkham Origins"),
    SkillNode("sk_gow_spartan", "Spartan Runic Rage", "+3 Heavy DMG & +15 Max Shield per Tier.", "COMBAT", 510L, false, 1, 25, inspirationTag = "God of War / DMC"),
    SkillNode("sk_gow_valkyrie", "Valkyrie Parry Ward", "+3% Incoming DR & +1.0/sec Regen per Tier.", "PROTECTION", 540L, false, 1, 25, inspirationTag = "God of War Ragnarök"),
    SkillNode("sk_force_telekinesis", "Force Repulse Mastery", "-6% Power Cooldown & +10% Knockback/Splash per Tier.", "ENGINEERING", 490L, false, 1, 25, inspirationTag = "Force Unleashed / Control"),
    SkillNode("sk_spider_acuity", "Spider-Sense Dodge Flow", "+4% Move Speed & +2.5% Incoming DR per Tier.", "PROTECTION", 400L, false, 1, 25, inspirationTag = "Insomniac Spider-Man"),
    SkillNode("sk_gta4_grit", "Liberty Street Grit", "+18 Max Shield & +2 Close-Range DMG per Tier.", "SURVIVAL", 360L, false, 1, 25, inspirationTag = "GTA IV / Max Payne"),
    SkillNode("sk_superman_solar", "Fortress Solar Overcharge", "+14% Projectile Velocity & +2 Solar DMG per Tier.", "COMBAT", 530L, false, 1, 25, inspirationTag = "Superman Returns / Saints Row IV"),
    SkillNode("sk_abyss_dungeon", "Abyssal Torch Extraction", "+6% Loot Luck & +1.5/sec Dungeon Regen per Tier.", "WONDER", 470L, false, 1, 25, inspirationTag = "Abyss / Dark & Darker"),
    SkillNode("sk_abyss_boon", "Chthonic Death Defiance", "+25 Max Shield & +3% Boss Impact DR per Tier.", "PROTECTION", 580L, false, 1, 25, inspirationTag = "Hades / Abyss"),
    SkillNode("sk_doom_glory", "Rip & Tear Armor Belch", "+2.5 Kill Shield Siphon & +4% Fire Rate per Tier.", "COMBAT", 560L, false, 1, 25, inspirationTag = "Doom Eternal"),
    SkillNode("sk_dmc_style", "SSS Stylish Cadence", "+6% Fire Rate & +5% Crit Multiplier per Tier.", "COMBAT", 520L, false, 1, 25, inspirationTag = "Devil May Cry"),
    SkillNode("sk_control_astral", "Astral Object of Power", "-6% Power & Dash Cooldown + 12% Wonder Duration per Tier.", "WONDER", 550L, false, 1, 25, inspirationTag = "Control / Remedy"),
    // Real-Life Creator Skills (Blake's real-life habits & creative craft)
    SkillNode("sk_rl_sketchbook", "Sep 2025 Sketchbook Focus", "+2 Bolt DMG & +5% Essence Gain per Tier from hand-drawn planning.", "REAL_LIFE", 320L, false, 1, 25, inspirationTag = "Real-Life Origins Map"),
    SkillNode("sk_rl_krita_layers", "Krita & PENUP Layer Flow", "+5% Crit & -5% Cooldowns per Tier from digital art precision.", "REAL_LIFE", 360L, false, 1, 25, inspirationTag = "Real-Life Art Tablet"),
    SkillNode("sk_rl_gym_cardio", "Everyday Gym & Cardio Grit", "+22 Max Shield & +4% Sprint Speed per Tier from real conditioning.", "REAL_LIFE", 340L, false, 1, 25, inspirationTag = "Real-Life Conditioning"),
    SkillNode("sk_rl_modder_tinker", "Android & Blender Tinker", "+5% Fire Rate & +12% Trap/Vacuum Radius per Tier.", "REAL_LIFE", 390L, false, 1, 25, inspirationTag = "Real-Life Modding"),
  ).associateBy { it.id }

  val protectionUpgrades: List<ProtectionUpgradeSpec> = listOf(
    ProtectionUpgradeSpec(
      id = "prot_armor_weave",
      title = "Ballistic Armor Weave",
      subtitle = "+3% All Incoming Damage Reduction & +18 Max Shield per Rank (R0-R25)",
      effectTag = "ARMOR DR",
      baseCostEssence = 300L,
      colorHex = 0xFF00E5FFL,
    ),
    ProtectionUpgradeSpec(
      id = "prot_elemental_ward",
      title = "Elemental & Explosive Ward",
      subtitle = "+4% Protection vs Explosive, Rock, Sand, Fire, Void & Cryo per Rank",
      effectTag = "ELEM WARD",
      baseCostEssence = 350L,
      colorHex = 0xFFFFAB40L,
    ),
    ProtectionUpgradeSpec(
      id = "prot_aegis_siphon",
      title = "Aegis Shield Siphon",
      subtitle = "+2 Shield on Kill, +25 Max Shield & +0.8/s Shield Regen per Rank",
      effectTag = "SIPHON",
      baseCostEssence = 400L,
      colorHex = 0xFF00E676L,
    ),
    ProtectionUpgradeSpec(
      id = "prot_cover_fortress",
      title = "Parkour Bastion Plate",
      subtitle = "+2.5% Damage Reduction & +3% Sprint/Dodge Speed per Rank",
      effectTag = "MOBILITY DR",
      baseCostEssence = 320L,
      colorHex = 0xFF448AFFL,
    ),
    ProtectionUpgradeSpec(
      id = "prot_emergency_dome",
      title = "Chrono Overclock Dome",
      subtitle = "+8% Cooldown Haste & +1.5/sec Emergency Regen per Rank",
      effectTag = "HASTE + REGEN",
      baseCostEssence = 450L,
      colorHex = 0xFFE040FBL,
    ),
    ProtectionUpgradeSpec(
      id = "prot_titan_guard",
      title = "Titan Boss Bulwark",
      subtitle = "+40 Max Shield & +2.5% Boss/Elite Impact Protection per Rank",
      effectTag = "BOSS GUARD",
      baseCostEssence = 480L,
      colorHex = 0xFFFF3D71L,
    ),
    ProtectionUpgradeSpec(
      id = "prot_arkham_freeflow",
      title = "Arkham Counter-Weave Vest",
      subtitle = "Batman Arkham / Shadow of War: +20 Max Shield & +3% Counter Evade DR per Rank",
      effectTag = "COUNTER DR",
      baseCostEssence = 360L,
      colorHex = 0xFFFFD54FL,
    ),
    ProtectionUpgradeSpec(
      id = "prot_control_resonance",
      title = "HRA Astral Resonance Shield",
      subtitle = "Control / Force Unleashed: +28 Max Shield & +5% Power Haste per Rank",
      effectTag = "ASTRAL WARD",
      baseCostEssence = 420L,
      colorHex = 0xFF7C4DFFL,
    ),
    ProtectionUpgradeSpec(
      id = "prot_hades_defiance",
      title = "Stygian Death-Defiance Boon",
      subtitle = "Hades / Abyss of Dungeons: +22 Max Shield & +1.2/s Regen per Rank",
      effectTag = "DEFIANCE",
      baseCostEssence = 460L,
      colorHex = 0xFFB388FFL,
    ),
    ProtectionUpgradeSpec(
      id = "prot_doom_armor_belch",
      title = "Praetor Flame-Belch Plating",
      subtitle = "Doom Eternal / God of War: +30 Max Shield & +2 Shield Siphon/Kill per Rank",
      effectTag = "ARMOR BELCH",
      baseCostEssence = 490L,
      colorHex = 0xFFFF5722L,
    ),
  )

  val gearAlloys: List<Pair<String, String>> = listOf(
    "titanium" to "Titanium Weave (+Armor & Shield)",
    "promethium" to "Promethium Core (+Fire/Explosive DMG)",
    "blacklight" to "Blacklight Alloy (+Siphon & Rate)",
    "chronosteel" to "Chrono-Steel (+Cooldown Haste)",
    "tectonic" to "Tectonic Obsidian (+Rock & Sand)",
  )

  val gearFocuses: List<Pair<String, String>> = listOf(
    "balanced" to "Balanced Tactical",
    "haste" to "Cooldown Haste (+12% CDR)",
    "fortress" to "Protection Bulwark (+8% DR)",
    "lethality" to "Damage Overclock (+12% DMG)",
    "velocity" to "Conduit Mobility (+10% Move & Rate)",
  )

  val equipmentVariants: List<Pair<String, String>> = listOf(
    "standard" to "Standard Calibration",
    "overclocked" to "Overclocked (+25% Cooldown Haste)",
    "explosive" to "Explosive Breacher Payload",
    "rock" to "Rock Tectonic Crush Payload",
    "sand" to "Sand Abrasive Storm Payload",
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

  val achievements: List<AchievementSpec> = listOf(
    AchievementSpec("ach_first_drop", "Multiverse Boot", "Complete your first Arena deployment.", "COMBAT", 1L, 300L, 400L, 1),
    AchievementSpec("ach_slayer_100", "Containment Sweep", "Eliminate 100 hostiles across all runs.", "COMBAT", 100L, 650L, 900L, 3),
    AchievementSpec("ach_slayer_500", "Extinction Protocol", "Eliminate 500 hostiles across the Multiverse.", "COMBAT", 500L, 1600L, 2500L, 6),
    AchievementSpec("ach_boss_5", "Titan Breaker", "Defeat 5 Lore Bosses (Hydra, Croc, or Mech).", "COMBAT", 5L, 900L, 1400L, 4),
    AchievementSpec("ach_boss_20", "Crucible Sovereign", "Defeat 20 Lore Bosses across any universe.", "COMBAT", 20L, 2400L, 4000L, 8),
    AchievementSpec("ach_lvl_25", "Field Veteran", "Reach Operator Level 25.", "PROGRESSION", 25L, 500L, 800L, 2),
    AchievementSpec("ach_lvl_100", "Centurion Prime", "Reach Operator Level 100.", "PROGRESSION", 100L, 1500L, 2500L, 5),
    AchievementSpec("ach_lvl_500", "Halfway to Omega", "Reach Operator Level 500.", "PROGRESSION", 500L, 4000L, 6000L, 9),
    AchievementSpec("ach_wpn_30", "MasterMarksman", "Level any primary weapon to Level 30.", "PROGRESSION", 30L, 750L, 1000L, 4),
    AchievementSpec("ach_wpn_60", "Weapon Perfection", "Reach max Level 60 on any weapon.", "PROGRESSION", 60L, 1800L, 2200L, 7),
    AchievementSpec("ach_over8_lobby", "Over-Capacity Ops", "Deploy into a 12+ player Mega Lobby with AI Bots.", "LOBBY", 12L, 700L, 1100L, 5),
    AchievementSpec("ach_war_32", "Total Multiverse War", "Deploy into a 32-Player Mega Lobby.", "LOBBY", 32L, 1500L, 2200L, 8),
    AchievementSpec("ach_war_128", "Omniverse 100+ Crusade", "Deploy into a 100+ to 200-Player Grand Lobby.", "LOBBY", 128L, 3500L, 5000L, 10),
    AchievementSpec("ach_pack_5", "Mythic Pacifier", "Upgrade Pack-a-Punch to Tier V+ inside an Arena.", "ARMORY", 5L, 850L, 1200L, 5),
    AchievementSpec("ach_pack_10", "Omega Transcendence", "Upgrade Pack-a-Punch to Tier X+ in combat.", "ARMORY", 10L, 2500L, 3500L, 10),
    AchievementSpec("ach_pack_15", "Apex Conduit", "Upgrade Pack-a-Punch to Tier XV+ in combat.", "ARMORY", 15L, 4500L, 6500L, 15),
    AchievementSpec("ach_pack_20", "Infinity Overclock", "Upgrade Pack-a-Punch to Tier XX+ in combat.", "ARMORY", 20L, 7500L, 10000L, 20),
    AchievementSpec("ach_pack_25", "Multiversal Ascendancy", "Reach maximum Pack-a-Punch Tier XXV in combat.", "ARMORY", 25L, 12000L, 16000L, 25),
    AchievementSpec("ach_wonder_10", "Artifact Collector", "Pick up 10 Wonder Weapon crates in Arena.", "ARMORY", 10L, 900L, 1300L, 5),
    AchievementSpec("ach_store_1", "Quartermaster Regular", "Make your first purchase in the Blue Essence Store.", "STORE", 1L, 350L, 500L, 2),
    AchievementSpec("ach_store_5", "Syndicate Patron", "Purchase 5 upgrades from the Blue Essence Store.", "STORE", 5L, 1100L, 1600L, 6),
    AchievementSpec("ach_spend_3k", "Blue Essence Baron", "Spend 3,000 Blue Essence on upgrades, passes, or store.", "STORE", 3000L, 1400L, 2000L, 7),
  )

  val storeItems: List<StoreItemSpec> = listOf(
    StoreItemSpec(
      id = "store_xp_boost_3",
      name = "2X Hyper XP Booster (3 Matches)",
      category = StoreCategory.BOOSTERS,
      description = "Doubles Player, Weapon, Gear, Equipment & Battle Pass XP for your next 3 Arena runs.",
      costBlueEssence = 350L,
      tier = 4,
      accentHex = 0xFF00E5FF,
      repeatable = true,
    ),
    StoreItemSpec(
      id = "store_player_xp_5k",
      name = "Operator XP Core (+6,000 XP)",
      category = StoreCategory.BOOSTERS,
      description = "Instant +6,000 Operator XP injection to accelerate toward Level 1,000.",
      costBlueEssence = 400L,
      tier = 5,
      accentHex = 0xFF448AFF,
      repeatable = true,
    ),
    StoreItemSpec(
      id = "store_wpn_xp_3k",
      name = "Weapon Calibration Core (+3,000 WPN XP)",
      category = StoreCategory.BOOSTERS,
      description = "Injects +3,000 Weapon XP directly into your currently equipped primary firearm.",
      costBlueEssence = 350L,
      tier = 5,
      accentHex = 0xFF7C4DFF,
      repeatable = true,
    ),
    StoreItemSpec(
      id = "store_bp_skip_1500",
      name = "Battle Pass Tier Skip (+1,800 BP XP)",
      category = StoreCategory.BOOSTERS,
      description = "Instantly advances your Battle Pass track by +1,800 BP XP.",
      costBlueEssence = 450L,
      tier = 5,
      accentHex = 0xFFFFD54F,
      repeatable = true,
    ),
    StoreItemSpec(
      id = "store_weapon_tier_up",
      name = "Multiversal Weapon Tier Catalyst (+1 Tier)",
      category = StoreCategory.TIERS,
      description = "Elevates your equipped Weapon Rarity Tier by +1 (up to Tier 25 Multiversal: massive DMG & Pierce).",
      costBlueEssence = 750L,
      tier = 15,
      accentHex = 0xFFFF3D71,
      repeatable = true,
    ),
    StoreItemSpec(
      id = "store_wonder_tier_up",
      name = "Wonder Arsenal Overclock (+1 All Wonder)",
      category = StoreCategory.TIERS,
      description = "Upgrades ALL 6 Mystery Wonder Weapons by +1 Tier (up to Tier 25 Multiversal) for massive Arena power.",
      costBlueEssence = 900L,
      tier = 25,
      accentHex = 0xFFE040FB,
      repeatable = true,
    ),
    StoreItemSpec(
      id = "store_gear_eq_pack",
      name = "Gear & Equipment +5 Level Kit",
      category = StoreCategory.ARSENAL,
      description = "Adds +5 Levels (or Tier +1 if maxed) to ALL 5 Gear slots and Lethal/Tactical Equipment.",
      costBlueEssence = 700L,
      tier = 6,
      accentHex = 0xFF00E676,
      repeatable = true,
    ),
    StoreItemSpec(
      id = "store_all_weapons_xp",
      name = "Multiverse Armory Bundle (+2,000 ALL WPN)",
      category = StoreCategory.ARSENAL,
      description = "Grants +2,000 Weapon XP to EVERY primary weapon in your arsenal simultaneously.",
      costBlueEssence = 850L,
      tier = 7,
      accentHex = 0xFFFFAB40,
      repeatable = true,
    ),
    StoreItemSpec(
      id = "store_bot_uplink",
      name = "AI Bot Tactical Uplink (+15% Bot Power)",
      category = StoreCategory.COSMETICS,
      description = "Upgrades all allied Squad & Lobby AI Bots with +15% cumulative combat damage (stacks up to +150%).",
      costBlueEssence = 500L,
      tier = 6,
      accentHex = 0xFF00E5FF,
      repeatable = true,
    ),
    StoreItemSpec(
      id = "store_omega_suit",
      name = "Omega Celestial Suit & Camo Bundle",
      category = StoreCategory.COSMETICS,
      description = "Equips the Radiant Omega Celestial Armor Theme, Prismatic Camo & +1,200 Bonus Player XP.",
      costBlueEssence = 600L,
      tier = 10,
      accentHex = 0xFFFFFFFF,
      repeatable = false,
    ),
  )

  fun tiersForVersion(version: BattlePassVersion, cycle: Int = 0): List<BattlePassTierReward> {
    val baseTiers = when (version) {
      BattlePassVersion.STANDARD_SECTOR_OPS -> listOf(
        BattlePassTierReward(1, 0L, "SHD Matte Finish", "Unlocks Sector Zero standard operator trim & +15% Blue Essence", 200L, "CAMO"),
        BattlePassTierReward(2, 400L, "Pulse Sight Reticle", "Precision Holo crosshair & bonus Blue Essence cache", 280L, "OPTIC"),
        BattlePassTierReward(3, 950L, "Quarantine XP Surge", "+600 Weapon XP injection for your active primary", 350L, "WPN XP"),
        BattlePassTierReward(4, 1600L, "Aegis Emerald Tracer", "Unlocks bright emerald tracer bolts & badge", 420L, "TRACER"),
        BattlePassTierReward(5, 2400L, "Division Tech Cache", "Boosts Mystery Wonder Weapon salvage rewards", 550L, "CACHE"),
        BattlePassTierReward(6, 3400L, "Vanguard Sentinel Master", "Prestige Operator Emblem + 900 Bonus Blue Essence", 900L, "PRESTIGE"),
      )
      BattlePassVersion.BLACKCELL_OVERDRIVE -> listOf(
        BattlePassTierReward(1, 0L, "Blackcell Obsidian Suit", "Instantly grants Obsidian & Gold armor trim + +22% Fire Rate", 300L, "SUIT"),
        BattlePassTierReward(2, 450L, "Molten Damascus Camo", "Blackcell animated gold-vein finish & tracer color", 400L, "CAMO"),
        BattlePassTierReward(3, 1050L, "Overclocked Syndicate", "Boosts Wonder Crate spawn rate +40% in all modes", 520L, "PERK"),
        BattlePassTierReward(4, 1750L, "Gilded BFG Munitions", "+800 Weapon XP + Blackcell Heavy Bore tuning", 650L, "BLUEPRINT"),
        BattlePassTierReward(5, 2600L, "Syndicate Golden Crown", "Unlocks Gold Prime visor & aura ring flare", 800L, "VISOR"),
        BattlePassTierReward(6, 3600L, "Blackcell Overlord Tier", "Ultimate Syndicate Title + 1,200 Bonus Blue Essence", 1200L, "ULTIMATE"),
      )
      BattlePassVersion.CHRONO_MULTIVERSE -> listOf(
        BattlePassTierReward(1, 0L, "Time-Warden Regalia", "Activates -30% Power/Stim Cooldowns in Arena", 300L, "PASSIVE"),
        BattlePassTierReward(2, 450L, "Cryo-Stasis Munitions", "Unlocks Chrono Freeze bolt coloration & stasis aura", 400L, "AMMO"),
        BattlePassTierReward(3, 1050L, "7-Chevron Gate Floor", "Unlocks Cosmic Nexus Arena floor & grid styling", 520L, "LEVEL"),
        BattlePassTierReward(4, 1750L, "Librarian Artifact Surge", "+800 Weapon XP + extended Wonder Weapon duration", 650L, "WPN XP"),
        BattlePassTierReward(5, 2600L, "Chrono Zat Priority", "Higher chance to pull Chrono Zat'nik'tel pickups", 800L, "WONDER"),
        BattlePassTierReward(6, 3600L, "Space & Time Sovereign", "Sovereign Temporal Banner + 1,250 Bonus Blue Essence", 1250L, "PRESTIGE"),
      )
      BattlePassVersion.HELLBREAKER_SUPREME -> listOf(
        BattlePassTierReward(1, 0L, "Hellbreaker Plate", "Activates +60 Max Shield & +20% Move Speed immediately", 350L, "ARMOR"),
        BattlePassTierReward(2, 500L, "Blood Moon Arena Kit", "Unlocks Crimson Crucible sky/floor theme & tracers", 480L, "LEVEL"),
        BattlePassTierReward(3, 1150L, "Vampiric Glory Siphon", "Hits vs Bosses/Elites siphon +4 Shield live in Arena", 600L, "SIPHON"),
        BattlePassTierReward(4, 1900L, "Adamant Claw Temper", "+900 Weapon XP + Crucible Ember armor glow", 750L, "AURA"),
        BattlePassTierReward(5, 2800L, "BFG-9000 Arsenal Lock", "Increases Crucible BFG-9000 Wonder drop priority", 900L, "WONDER"),
        BattlePassTierReward(6, 3800L, "Apex Warlord Ascension", "Hellbreaker Supreme Emblem + 1,400 Bonus Blue Essence", 1400L, "SUPREME"),
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

  private val botCallsigns = listOf(
    "SPECTRE-01", "VALKYRIE-X", "SHD-GHOST", "AEGIS-ZERO", "MERCER-BL", "CHRONO-SG1",
    "STARBLADE-7", "HELLFIRE-9", "NOVA-PRIME", "VIPER-ECHO", "TITAN-K9", "PHOENIX-R4",
    "ORION-SYNC", "STRIKER-Z", "SOLARIS-15", "APEX-WRAITH", "CYBER-FANG", "PULSE-V2",
    "HYDRA-BANE", "OMEGA-20", "KESTREL-X", "REAPER-22", "DRIFT-23", "NEXUS-24",
    "GUARDIAN-25", "VANGUARD-26", "QUANTUM-27", "RAZOR-28", "COBALT-29", "ZENITH-30", "ASTRAL-31",
  )

  private val botArchetypes = listOf("operator", "biomorph", "starblade", "infernal")
  private val botAccents = listOf(0xFF00E676L, 0xFF00E5FFL, 0xFFFFAB40L, 0xFFE040FBL, 0xFFFF5252L, 0xFFFFD54FL)

  fun generateLobbyRoster(config: LobbyConfig, playerCallsign: String, playerLevel: Int, playerPrestige: Int): List<LobbyBotMember> {
    val totalSlots = config.targetPlayers.coerceIn(1, 250)
    val members = ArrayList<LobbyBotMember>(totalSlots)
    val squadSize = config.partyMode.squadSize.coerceAtLeast(1)

    members.add(
      LobbyBotMember(
        id = "local_player",
        callsign = playerCallsign.ifBlank { "Vanguard" },
        squadIndex = 1,
        isAlliedSquadmate = true,
        isHumanPlayer = true,
        archetype = "operator",
        accentHex = 0xFF00E5FFL,
        level = playerLevel,
        prestige = playerPrestige,
      )
    )

    if (config.autoFillBots && totalSlots > 1) {
      for (slotIdx in 1 until totalSlots) {
        val squadIdx = (slotIdx / squadSize) + 1
        val isAllied = squadIdx == 1
        val baseName = botCallsigns[(slotIdx - 1) % botCallsigns.size]
        val suffixCycle = (slotIdx - 1) / botCallsigns.size
        val botName = if (suffixCycle == 0) baseName else "$baseName-${suffixCycle + 1}"
        val botLvl = (playerLevel + ((slotIdx * 7) % 25) - 5).coerceIn(1, 1000)
        val botPrestige = if (config.botDifficulty == BotDifficulty.APEX) (slotIdx % 10) + 1 else (slotIdx % 4)
        members.add(
          LobbyBotMember(
            id = "ai_bot_$slotIdx",
            callsign = botName,
            squadIndex = squadIdx,
            isAlliedSquadmate = isAllied,
            isHumanPlayer = false,
            archetype = botArchetypes[slotIdx % botArchetypes.size],
            accentHex = if (isAllied) 0xFF00E676L else botAccents[slotIdx % botAccents.size],
            level = botLvl,
            prestige = botPrestige,
          )
        )
      }
    }
    return members
  }

  fun tierMeta(tier: Int): PowerTier = PowerTier.fromLevel(tier)

  fun tierMultiplier(tier: Int): Float = PowerTier.fromLevel(tier).multiplier

  fun xpRequiredForPlayerLevel(level: Int): Long = 180L + (level - 1).coerceAtLeast(0) * 18L

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

  fun xpProgressInCurrentPlayerLevel(xp: Long): Pair<Long, Long> {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 1000) {
      val need = xpRequiredForPlayerLevel(lvl)
      if (remaining < need) return remaining to need
      remaining -= need
      lvl++
    }
    val maxNeed = xpRequiredForPlayerLevel(999)
    return maxNeed to maxNeed
  }

  val milestoneLevels: List<Int> = listOf(5, 10, 20, 35, 50, 75, 100, 150, 200, 350, 500, 750, 1000)

  fun isMilestoneLevel(level: Int): Boolean = milestoneLevels.contains(level)

  fun nextMilestoneLevel(level: Int): Int =
    milestoneLevels.firstOrNull { it > level } ?: 1000

  fun milestoneTierCount(level: Int): Int =
    milestoneLevels.count { level >= it }

  /**
   * Non-linear milestone leveling: intermediate levels plateau with minimal passive change,
   * while milestone breakthroughs (5, 10, 20, 35, 50, 75, 100, 150, 200, 350, 500, 750, 1000)
   * grant dramatically larger cumulative stat jumps over time.
   */
  fun levelStatMultiplier(level: Int): Float {
    val count = milestoneTierCount(level)
    return 1.0f + (count * 0.14f) + ((count * count) * 0.022f)
  }

  fun milestoneBonusSummary(level: Int): String {
    val unlocked = milestoneTierCount(level)
    val nextMs = nextMilestoneLevel(level)
    return if (isMilestoneLevel(level)) {
      "MILESTONE LV.$level BREAKTHROUGH ACTIVE (x${String.format(java.util.Locale.US, "%.2f", levelStatMultiplier(level))} Stats)"
    } else if (level >= 1000) {
      "MAX OMEGA MILESTONE (x${String.format(java.util.Locale.US, "%.2f", levelStatMultiplier(level))} Stats)"
    } else {
      "Milestone $unlocked/${milestoneLevels.size} • Next Stat Jump at Lv.$nextMs"
    }
  }

  fun rankTitleForLevel(level: Int, prestige: Int): String {
    val suffix = if (prestige > 0) " [P$prestige]" else ""
    val title = when {
      level >= 800 -> "Omega Omniverse God"
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

  fun xpRequiredForWeaponLevel(level: Int): Long = 100L + (level - 1).coerceAtLeast(0) * 26L

  fun weaponLevelForXp(xp: Long): Int {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 60) {
      val need = xpRequiredForWeaponLevel(lvl)
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
      val need = xpRequiredForWeaponLevel(lvl)
      if (remaining < need) return remaining to need
      remaining -= need
      lvl++
    }
    val maxNeed = xpRequiredForWeaponLevel(59)
    return maxNeed to maxNeed
  }

  val equipmentItems: List<EquipmentItemSpec> = listOf(
    EquipmentItemSpec("lethal_frag", "Frag Grenade", EquipmentSlotType.LETHAL, "High explosive shrapnel grenade with wide blast radius (Explosive).", 160f, 6.0f, 0xFFFF5722),
    EquipmentItemSpec("lethal_molotov", "Napalm Molotov", EquipmentSlotType.LETHAL, "Deploys a fiery hazard zone burning hordes over time (Thermal).", 120f, 8.0f, 0xFFFF9800),
    EquipmentItemSpec("lethal_c4", "Remote C4 Satchel", EquipmentSlotType.LETHAL, "Detonated manually for heavy explosive breaching damage (Explosive).", 240f, 12.0f, 0xFFE53935),
    EquipmentItemSpec("lethal_cluster", "Arc Cluster Mine", EquipmentSlotType.LETHAL, "Deploys homing arc sub-munitions seeking elite targets (Energy).", 190f, 10.0f, 0xFF7C4DFF),
    EquipmentItemSpec("lethal_rock_tremor", "Tectonic Quake Charge", EquipmentSlotType.LETHAL, "Detonates seismic rock spikes shattering enemy armor (Rock).", 220f, 7.5f, 0xFFFFD54F),
    EquipmentItemSpec("lethal_sandstorm_jar", "Abyssal Sandstorm Flask", EquipmentSlotType.LETHAL, "Unleashes an abrasive desert vortex eroding armor & shields (Sand).", 185f, 7.0f, 0xFFD7CCC8),
    EquipmentItemSpec("lethal_web_snare_bomb", "Spider-Tech Web Bomb", EquipmentSlotType.LETHAL, "Spider-Man / Arkham: Tethers & slows swarms in electrified shock webbing (Energy).", 210f, 6.5f, 0xFFE53935),
    EquipmentItemSpec("lethal_force_repulse_emitter", "Force & Astral Repulse Pod", EquipmentSlotType.LETHAL, "Force Unleashed / Control: Blasts a kinetic shockwave knocking back elites (Kinetic).", 255f, 8.5f, 0xFF38BDF8),
    EquipmentItemSpec("tactical_stim", "Nano-Stim Injector", EquipmentSlotType.TACTICAL, "Restores 60 shield instantly and accelerates movement & cooldowns.", 60f, 5.0f, 0xFF00E5FF),
    EquipmentItemSpec("tactical_emp", "EMP Shockwave", EquipmentSlotType.TACTICAL, "Disables enemy energy barriers and stuns nearby targets (Energy).", 90f, 9.0f, 0xFF00B0FF),
    EquipmentItemSpec("tactical_decoy", "Holographic Decoy", EquipmentSlotType.TACTICAL, "Projects a decoy emitter that draws hostile aggro.", 0f, 14.0f, 0xFF1DE9B6),
    EquipmentItemSpec("tactical_insertion", "Chrono Beacon", EquipmentSlotType.TACTICAL, "Spawns a revival checkpoint, continuous shield aura & cooldown surge.", 110f, 20.0f, 0xFF651FFF),
    EquipmentItemSpec("tactical_rock_aegis", "Stonebreaker Bulwark Surge", EquipmentSlotType.TACTICAL, "Coats armor in tectonic stone reducing damage & staggering hostiles (Rock).", 95f, 8.5f, 0xFFFFCA28),
    EquipmentItemSpec("tactical_sand_veil", "Desert Mirage Cloak", EquipmentSlotType.TACTICAL, "Summons a shifting sand veil restoring shield & boosting evade speed (Sand).", 80f, 7.5f, 0xFFE0C097),
    EquipmentItemSpec("tactical_isac_seeker_heal", "ISAC Mender Seeker", EquipmentSlotType.TACTICAL, "The Division: Launches a nano-repair drone restoring 85 shield & pulsing regen.", 85f, 6.5f, 0xFFFF8F00),
    EquipmentItemSpec("tactical_runic_frost_cleaver", "Leviathan Frost & Parry Pulse", EquipmentSlotType.TACTICAL, "God of War / Hades: Chills surrounding enemies & grants brief counter armor (Cryo).", 105f, 8.0f, 0xFF448AFF),
  )

  fun allEquipmentItems(customEquipment: List<UserCustomEquipmentSpec> = emptyList()): List<EquipmentItemSpec> =
    (equipmentItems + customEquipment.map { it.toEquipmentItemSpec() }).distinctBy { it.id }

  fun resolveEquipmentItem(id: String, customEquipment: List<UserCustomEquipmentSpec> = emptyList()): EquipmentItemSpec =
    customEquipment.firstOrNull { it.id == id }?.toEquipmentItemSpec()
      ?: equipmentItems.firstOrNull { it.id == id }
      ?: equipmentItems.first()

  fun xpRequiredForEquipmentLevel(level: Int): Long = 90L + (level - 1).coerceAtLeast(0) * 22L

  fun totalXpForEquipmentLevel(targetLevel: Int): Long {
    var sum = 0L
    for (lvl in 1 until targetLevel.coerceIn(1, 60)) {
      sum += xpRequiredForEquipmentLevel(lvl)
    }
    return sum
  }

  fun equipmentLevelForXp(xp: Long): Int {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 60) {
      val need = xpRequiredForEquipmentLevel(lvl)
      if (remaining < need) break
      remaining -= need
      lvl++
    }
    return lvl
  }

  fun xpProgressInCurrentEquipmentLevel(xp: Long): Pair<Long, Long> {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 60) {
      val need = xpRequiredForEquipmentLevel(lvl)
      if (remaining < need) return remaining to need
      remaining -= need
      lvl++
    }
    val maxNeed = xpRequiredForEquipmentLevel(59)
    return maxNeed to maxNeed
  }

  fun xpRequiredForGearLevel(level: Int): Long = 85L + (level - 1).coerceAtLeast(0) * 20L

  fun totalXpForGearLevel(targetLevel: Int): Long {
    var sum = 0L
    for (lvl in 1 until targetLevel.coerceIn(1, 50)) {
      sum += xpRequiredForGearLevel(lvl)
    }
    return sum
  }

  fun gearLevelForXp(xp: Long): Int {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 50) {
      val need = xpRequiredForGearLevel(lvl)
      if (remaining < need) break
      remaining -= need
      lvl++
    }
    return lvl
  }

  fun xpProgressInCurrentGearLevel(xp: Long): Pair<Long, Long> {
    var lvl = 1
    var remaining = xp.coerceAtLeast(0L)
    while (lvl < 50) {
      val need = xpRequiredForGearLevel(lvl)
      if (remaining < need) return remaining to need
      remaining -= need
      lvl++
    }
    val maxNeed = xpRequiredForGearLevel(49)
    return maxNeed to maxNeed
  }

  fun starMeta(stars: Int): StarRankMeta = StarRankMeta.forStars(stars)

  fun starMultiplier(stars: Int): Float = StarRankMeta.forStars(stars).multiplier
}

/**
 * Single source of truth for all Blue Essence upgrade costs across UI tabs and ProgressionStore.
 */
object UpgradeCosts {
  fun characterTier(tier: Int): Long = tier * 500L
  fun characterStar(stars: Int): Long = stars * 600L
  fun outfitLevelUp(tier: Int): Long = 250L + (tier * 180L)
  fun outfitStar(stars: Int): Long = stars * 500L
  fun outfitSubSystem(rank: Int): Long = 180L + (rank * 120L)
  fun armoryModTier(tier: Int): Long = tier * 300L
  fun armoryModStar(stars: Int): Long = stars * 450L
  fun gearTier(tier: Int): Long = tier * 400L
  fun gearStar(stars: Int): Long = stars * 450L
  fun equipmentTier(tier: Int): Long = tier * 350L
  fun equipmentStar(stars: Int): Long = stars * 400L
  fun weaponTier(tier: Int): Long = tier * 450L
  fun weaponStar(stars: Int): Long = stars * 500L
  fun wonderTier(tier: Int): Long = tier * 500L
  fun wonderStar(stars: Int): Long = stars * 550L
  fun skillStar(stars: Int): Long = stars * 400L
  fun areaFeatureTier(tier: Int): Long = tier * 300L
  fun areaStar(stars: Int): Long = stars * 450L
  fun modeTier(tier: Int): Long = tier * 350L
  fun modeStar(stars: Int): Long = stars * 450L
  fun perkDrink(rank: Int): Long = 250L + (rank * 200L)
  fun perkStar(stars: Int): Long = stars * 380L
  fun protectionRank(rank: Int): Long = 260L + (rank * 200L)
  fun protectionStar(stars: Int): Long = stars * 400L
  fun customBuffTier(tier: Int): Long = tier * 320L
  fun customBuffStar(stars: Int): Long = stars * 400L
  fun blastShardTier(tier: Int): Long = 280L + (tier * 220L)
  fun companionBuffTier(tier: Int): Long = 280L + (tier * 240L)
  fun blastCoreBlueEssence(level: Int): Long = 240L + (level * 190L)
  fun vacuumBot(tier: Int): Long = 220L + (tier * 160L)
  fun vacuumBotStar(stars: Int): Long = stars * 400L

  /**
   * Embedded No-APK Play Guide for running exported collections without an Android APK.
   */
  const val NO_APK_PLAY_GUIDE: String = UniverseCatalog.NO_APK_PLAY_GUIDE
}

enum class CollectionCustomScope(
  val id: String,
  val label: String,
  val shortNote: String,
) {
  ALL_CURATED(
    id = "all_curated",
    label = "BO3 + Steam + Collections",
    shortNote = "Scoped strictly to BO3 Mod Tools, Steam Workshop & your Game Collections.",
  ),
  BO3_ONLY(
    id = "bo3_only",
    label = "BO3 Mod Tools Only",
    shortNote = "Only Treyarch BO3 Zombies maps (Nacht, The Giant, Gorod, Shadows, Der Riese) & BO3 mods.",
  ),
  STEAM_ONLY(
    id = "steam_only",
    label = "Steam Workshop Only",
    shortNote = "Only Steam Workshop game collection presets, .VDF/.JSON packs & imported Rigs.",
  ),
  COLLECTIONS_ONLY(
    id = "collections_only",
    label = "Game Collections Only",
    shortNote = "Only your Game Collections (Prototype, inFAMOUS, Arkham, Spider-Man, GoW, Division, Starkiller, Ship of Heroes).",
  );

  companion object {
    fun fromId(id: String): CollectionCustomScope =
      entries.firstOrNull { it.id == id } ?: ALL_CURATED
  }
}

@Immutable
data class CollectionCustomModeConfig(
  val enabled: Boolean = false,
  val safeCollectionGuard: Boolean = true,
  val scope: CollectionCustomScope = CollectionCustomScope.ALL_CURATED,
  val targetAreaId: String = "area_bo3_the_giant",
  val enemyHpBoostPercent: Int = 30,
  val enemyArmorFlood: Boolean = false,
  val powerAndCooldownSuppression: Boolean = true,
  val essenceAndShardDrain: Boolean = true,
  val shieldAttritionLeak: Boolean = false,
  val collectionAmbushSurge: Boolean = false,
  val disableWonderDrops: Boolean = false,
) {
  val effectiveCappedHpPercent: Int
    get() = if (safeCollectionGuard) enemyHpBoostPercent.coerceIn(0, 75) else enemyHpBoostPercent.coerceIn(0, 100)

  val activeNegativeCount: Int
    get() = listOf(
      effectiveCappedHpPercent > 0,
      enemyArmorFlood,
      powerAndCooldownSuppression,
      essenceAndShardDrain,
      shieldAttritionLeak,
      collectionAmbushSurge,
      disableWonderDrops,
    ).count { it }

  val negativeImpactPercent: Int
    get() {
      val hpScore = (effectiveCappedHpPercent / 4).coerceAtMost(25)
      val toggleScore =
        (if (enemyArmorFlood) (if (safeCollectionGuard) 8 else 14) else 0) +
          (if (powerAndCooldownSuppression) (if (safeCollectionGuard) 12 else 16) else 0) +
          (if (essenceAndShardDrain) (if (safeCollectionGuard) 12 else 18) else 0) +
          (if (shieldAttritionLeak) (if (safeCollectionGuard) 10 else 14) else 0) +
          (if (collectionAmbushSurge) (if (safeCollectionGuard) 9 else 12) else 0) +
          (if (disableWonderDrops) (if (safeCollectionGuard) 6 else 10) else 0)
      return (hpScore + toggleScore).coerceIn(0, if (safeCollectionGuard) 82 else 100)
    }

  val impactLabel: String
    get() = when {
      !enabled -> "Custom Mode OFF"
      negativeImpactPercent >= 65 -> "High Strain (${negativeImpactPercent}%)${if (safeCollectionGuard) " • Safe" else ""}"
      negativeImpactPercent >= 35 -> "Heavy Collection Impact (${negativeImpactPercent}%)"
      negativeImpactPercent > 0 -> "Moderate Collection Strain (${negativeImpactPercent}%)"
      else -> "Clean Custom Rules (0% Negative Impact)"
    }

  val collectionSafetyNote: String
    get() = if (safeCollectionGuard) {
      "Collection Safeguard ON: BO3/Steam/Collection unlocks, Tiers (1-25), Stars, Bank & Wonder Weapons stay 100% intact; enemy HP/armor caps prevent softlocks."
    } else {
      "Safeguard Relaxed: Stronger negative penalties active in scoped runs (saved Collection items & Bank still remain safe)."
    }

  fun isAreaInScope(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): Boolean =
    scopedAreas(scope, userCustomMaps).any { it.id == areaId }

  fun isActiveForArea(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): Boolean =
    enabled && isAreaInScope(areaId, userCustomMaps)

  fun effectiveHostileHpMultFor(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): Float =
    if (!isActiveForArea(areaId, userCustomMaps)) 1.0f else 1.0f + (effectiveCappedHpPercent / 100f)

  fun effectiveDamageMultFor(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): Float =
    if (!isActiveForArea(areaId, userCustomMaps) || !powerAndCooldownSuppression) 1.0f
    else if (safeCollectionGuard) 0.75f else 0.65f

  fun effectiveCooldownMultFor(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): Float =
    if (!isActiveForArea(areaId, userCustomMaps) || !powerAndCooldownSuppression) 1.0f
    else if (safeCollectionGuard) 1.35f else 1.55f

  fun effectiveEssenceMultFor(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): Float =
    if (!isActiveForArea(areaId, userCustomMaps) || !essenceAndShardDrain) 1.0f
    else if (safeCollectionGuard) 0.65f else 0.55f

  fun effectiveIncomingDamageMultFor(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): Float =
    if (!isActiveForArea(areaId, userCustomMaps) || !shieldAttritionLeak) 1.0f
    else if (safeCollectionGuard) 1.25f else 1.40f

  fun effectiveRegenMultFor(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): Float =
    if (!isActiveForArea(areaId, userCustomMaps) || !shieldAttritionLeak) 1.0f
    else if (safeCollectionGuard) 0.55f else 0.35f

  fun effectiveSpawnCountMultFor(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): Float =
    if (!isActiveForArea(areaId, userCustomMaps) || !collectionAmbushSurge) 1.0f
    else if (safeCollectionGuard) 1.25f else 1.35f

  fun effectiveEnemySpeedMultFor(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): Float =
    if (!isActiveForArea(areaId, userCustomMaps) || !collectionAmbushSurge) 1.0f
    else if (safeCollectionGuard) 1.25f else 1.40f

  fun shouldSuppressCrateWonderDrops(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): Boolean =
    isActiveForArea(areaId, userCustomMaps) && disableWonderDrops

  fun shouldReinforceArmor(
    areaId: String,
    isElite: Boolean,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): Boolean {
    if (!isActiveForArea(areaId, userCustomMaps) || !enemyArmorFlood) return false
    return if (safeCollectionGuard) isElite else true
  }

  val effectiveHostileHpMult: Float
    get() = if (!enabled) 1.0f else 1.0f + (effectiveCappedHpPercent / 100f)

  val effectiveDamageMult: Float
    get() = if (!enabled || !powerAndCooldownSuppression) 1.0f else if (safeCollectionGuard) 0.75f else 0.65f

  val effectiveCooldownMult: Float
    get() = if (!enabled || !powerAndCooldownSuppression) 1.0f else if (safeCollectionGuard) 1.35f else 1.55f

  val effectiveEssenceMult: Float
    get() = if (!enabled || !essenceAndShardDrain) 1.0f else if (safeCollectionGuard) 0.65f else 0.55f

  val effectiveIncomingDamageMult: Float
    get() = if (!enabled || !shieldAttritionLeak) 1.0f else if (safeCollectionGuard) 1.25f else 1.40f

  val effectiveRegenMult: Float
    get() = if (!enabled || !shieldAttritionLeak) 1.0f else if (safeCollectionGuard) 0.55f else 0.35f

  companion object {
    private val bo3AreaIds = setOf(
      "area_bo3_nacht",
      "area_bo3_the_giant",
      "area_bo3_gorod",
      "area_bo3_shadows",
      "area_zombies_der_riese",
    )

    private val steamLinkedAreaIds = setOf(
      "area_bo3_the_giant",
      "area_bo3_nacht",
      "area_bo3_gorod",
      "area_bo3_shadows",
      "area_blacklight_hive",
      "area_gotham_blackgate",
      "area_spiderman_queens",
      "area_hellbreaker_crucible",
      "area_sector_zero",
      "area_aegis_orbital",
      "area_ironman_stark",
      "area_ship_of_heroes_deck",
      "area_empire_city",
    )

    private val collectionAreaIds = setOf(
      "area_empire_city",
      "area_batcave",
      "area_gotham_blackgate",
      "area_ironman_stark",
      "area_spiderman_queens",
      "area_spider_bugle_subway",
      "area_blacklight_hive",
      "area_hellbreaker_crucible",
      "area_chrono_gate",
      "area_aegis_orbital",
      "area_sector_zero",
      "area_ship_of_heroes_deck",
      "area_mended_convergence",
      "area_hero_frontier",
    )

    fun scopedAreas(
      scope: CollectionCustomScope,
      userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
    ): List<CustomAreaSpec> {
      val customBo3Specs = (CuratedCustomMapsCatalog.curatedBo3CustomMaps + userCustomMaps.filter { it.gameCategory == CustomMapGameCategory.BO3 })
        .distinctBy { it.id }
        .map { it.toCustomAreaSpec() }
      val customCollectionSpecs = (CuratedCustomMapsCatalog.curatedCollectionCustomMaps + userCustomMaps.filter { it.gameCategory != CustomMapGameCategory.BO3 })
        .distinctBy { it.id }
        .map { it.toCustomAreaSpec() }

      val combined = when (scope) {
        CollectionCustomScope.BO3_ONLY ->
          MultiverseArsenalCatalog.customAreas.filter { it.id in bo3AreaIds } + customBo3Specs
        CollectionCustomScope.STEAM_ONLY ->
          MultiverseArsenalCatalog.customAreas.filter { it.id in steamLinkedAreaIds } + customBo3Specs + customCollectionSpecs
        CollectionCustomScope.COLLECTIONS_ONLY ->
          MultiverseArsenalCatalog.customAreas.filter { it.id in collectionAreaIds } + customCollectionSpecs
        CollectionCustomScope.ALL_CURATED ->
          MultiverseArsenalCatalog.customAreas.filter {
            it.id in bo3AreaIds || it.id in steamLinkedAreaIds || it.id in collectionAreaIds
          } + customBo3Specs + customCollectionSpecs
      }
      return combined.distinctBy { it.id }
    }

    fun isArmoryItemEligible(item: ArmoryItem, scope: CollectionCustomScope): Boolean {
      val tag = item.universeTag.lowercase()
      val id = item.id.lowercase()
      val isBo3 = tag.contains("bo3") || id.startsWith("bo3_")
      val isSteam = tag.contains("workshop") || id.startsWith("ws_")
      val isCollection = tag.contains("tuu") ||
        tag.contains("prototype") ||
        tag.contains("infamous") ||
        tag.contains("arkham") ||
        tag.contains("origins") ||
        tag.contains("blackgate") ||
        tag.contains("spider") ||
        tag.contains("stark") ||
        tag.contains("god of war") ||
        tag.contains("sector zero") ||
        tag.contains("aegis") ||
        tag.contains("ship of heroes") ||
        tag.contains("crossover") ||
        tag.contains("chrono") ||
        tag.contains("gta") ||
        tag.contains("liberty") ||
        tag.contains("superman") ||
        tag.contains("metropolis") ||
        tag.contains("abyss") ||
        tag.contains("dungeon")
      return when (scope) {
        CollectionCustomScope.BO3_ONLY -> isBo3
        CollectionCustomScope.STEAM_ONLY -> isSteam || isBo3
        CollectionCustomScope.COLLECTIONS_ONLY -> isCollection
        CollectionCustomScope.ALL_CURATED -> isBo3 || isSteam || isCollection
      }
    }

    fun severeNegativePreset(
      scope: CollectionCustomScope = CollectionCustomScope.ALL_CURATED,
      areaId: String = scopedAreas(scope).firstOrNull()?.id ?: "area_bo3_the_giant",
    ): CollectionCustomModeConfig = CollectionCustomModeConfig(
      enabled = true,
      safeCollectionGuard = true,
      scope = scope,
      targetAreaId = areaId,
      enemyHpBoostPercent = 75,
      enemyArmorFlood = true,
      powerAndCooldownSuppression = true,
      essenceAndShardDrain = true,
      shieldAttritionLeak = true,
      collectionAmbushSurge = true,
      disableWonderDrops = true,
    )

    fun mildStrainPreset(
      scope: CollectionCustomScope = CollectionCustomScope.ALL_CURATED,
      areaId: String = scopedAreas(scope).firstOrNull()?.id ?: "area_bo3_the_giant",
    ): CollectionCustomModeConfig = CollectionCustomModeConfig(
      enabled = true,
      safeCollectionGuard = true,
      scope = scope,
      targetAreaId = areaId,
      enemyHpBoostPercent = 30,
      enemyArmorFlood = false,
      powerAndCooldownSuppression = true,
      essenceAndShardDrain = false,
      shieldAttritionLeak = false,
      collectionAmbushSurge = false,
      disableWonderDrops = false,
    )

    fun cleanPreset(
      scope: CollectionCustomScope = CollectionCustomScope.ALL_CURATED,
      areaId: String = scopedAreas(scope).firstOrNull()?.id ?: "area_bo3_the_giant",
    ): CollectionCustomModeConfig = CollectionCustomModeConfig(
      enabled = true,
      safeCollectionGuard = true,
      scope = scope,
      targetAreaId = areaId,
      enemyHpBoostPercent = 0,
      enemyArmorFlood = false,
      powerAndCooldownSuppression = false,
      essenceAndShardDrain = false,
      shieldAttritionLeak = false,
      collectionAmbushSurge = false,
      disableWonderDrops = false,
    )
  }
}

enum class CustomMapGameCategory(
  val id: String,
  val label: String,
  val linkedWorldId: String,
  val defaultModeId: String,
  val accentColorHex: String,
  val defaultAccentHex: Long,
  val defaultWonderWeapon: String,
  val defaultZones: List<String>,
) {
  BO3(
    id = "bo3",
    label = "BO3 Mod Tools",
    linkedWorldId = "sector_zero",
    defaultModeId = "zombies",
    accentColorHex = "#00E676",
    defaultAccentHex = 0xFF00E676,
    defaultWonderWeapon = "Ray Gun Mk.III",
    defaultZones = listOf("Spawn Courtyard", "Mystery Box Room", "Power Generator", "Pack-a-Punch Vault", "Juggernog Alcove", "Escape Teleporter"),
  ),
  PROTOTYPE(
    id = "prototype",
    label = "Prototype / Blacklight",
    linkedWorldId = "blacklight_conduit",
    defaultModeId = "zombies",
    accentColorHex = "#FF3D71",
    defaultAccentHex = 0xFFFF3D71,
    defaultWonderWeapon = "Blacklight Tendril Whip",
    defaultZones = listOf("Gentek Containment Plaza", "Red-Zone Tower Roof", "Blackwatch Fuel Depot", "Bio-Hive Chamber", "Strike Helipad", "Viral Vault"),
  ),
  INFAMOUS(
    id = "infamous",
    label = "inFAMOUS Conduit",
    linkedWorldId = "empire_city_infamous",
    defaultModeId = "hero_patrol",
    accentColorHex = "#00E5FF",
    defaultAccentHex = 0xFF00E5FF,
    defaultWonderWeapon = "Ray-Sphere Conduit Core",
    defaultZones = listOf("Stasis Pier Substation", "Neon Skylink Bridge", "Zeke's Roof Deck", "Conduit Relay Core", "Blast Shard Yard", "Warren Railhead"),
  ),
  ARKHAM(
    id = "arkham",
    label = "Batman Arkham (Origins/Blackgate)",
    linkedWorldId = "gotham_arkham",
    defaultModeId = "hero_patrol",
    accentColorHex = "#FFD54F",
    defaultAccentHex = 0xFFFFD54F,
    defaultWonderWeapon = "Shock-Gloves Detective Core",
    defaultZones = listOf("Pioneers Bridge", "Blackgate Cellblock A", "Royal Hotel Ballroom", "Wonder Tower Spire", "GCPD Roof Beacon", "Batcave Sub-Vault"),
  ),
  GTA4(
    id = "gta4",
    label = "GTA IV Liberty City",
    linkedWorldId = "sector_zero",
    defaultModeId = "dmz",
    accentColorHex = "#FFAB40",
    defaultAccentHex = 0xFFFFAB40,
    defaultWonderWeapon = "Roman's Combat RPG & M4",
    defaultZones = listOf("Hove Beach Safehouse", "Broker-Dukes Bridge", "Star Junction Plaza", "Bank of Liberty Vault", "Alderney Industrial Docks", "Happiness Island Statue"),
  ),
  SUPERMAN(
    id = "superman",
    label = "Superman Returns Metropolis",
    linkedWorldId = "mr_crossover_nexus",
    defaultModeId = "hero_patrol",
    accentColorHex = "#2979FF",
    defaultAccentHex = 0xFF2979FF,
    defaultWonderWeapon = "Solar Heat-Vision Overdrive",
    defaultZones = listOf("Daily Planet Globe", "LexCorp Tower Spire", "Centennial Park Harbor", "Warworld Gladiator Pit", "Hob's Bay Power Plant", "Fortress Solitude Conduit"),
  ),
  ABYSS_DUNGEONS(
    id = "abyss_dungeons",
    label = "Abyss of Dungeons",
    linkedWorldId = "hellbreaker_citadel",
    defaultModeId = "boss_rush",
    accentColorHex = "#B388FF",
    defaultAccentHex = 0xFFB388FF,
    defaultWonderWeapon = "Abyssal Lich Staff / Greatsword",
    defaultZones = listOf("Howling Crypt Entrance", "Torch Shrine Crossing", "Runic Forge Anvil", "Mimic Treasury Vault", "Goblin Cave Bridge", "Abyssal Lich Sanctum"),
  ),
  SPIDERMAN(
    id = "spiderman",
    label = "Spider-Man",
    linkedWorldId = "spiderverse_queens",
    defaultModeId = "hero_patrol",
    accentColorHex = "#E53935",
    defaultAccentHex = 0xFFE53935,
    defaultWonderWeapon = "Iron-Spider Web Conduit",
    defaultZones = listOf("Oscorp Helipad", "Queens Bridge Tower", "Horizon Collider Deck", "Bugle Skybridge", "F.E.A.S.T. Rooftop", "Midtown Pylon"),
  ),
  GODOFWAR(
    id = "godofwar",
    label = "God of War Crucible",
    linkedWorldId = "hellbreaker_citadel",
    defaultModeId = "boss_rush",
    accentColorHex = "#FF5252",
    defaultAccentHex = 0xFFFF5252,
    defaultWonderWeapon = "Crucible BFG-9000",
    defaultZones = listOf("Tartarus Abyss Gate", "Leviathan Forge Anvil", "Valkyrie Trial Ring", "Blade Siphon Altar", "Hydra Maw Overlook", "Odin's Vault"),
  ),
  DIVISION(
    id = "division",
    label = "The Division SHD",
    linkedWorldId = "sector_zero",
    defaultModeId = "dmz",
    accentColorHex = "#FFAB40",
    defaultAccentHex = 0xFFFFAB40,
    defaultWonderWeapon = "Liberty Wunder-Arc",
    defaultZones = listOf("DZ-09 Mall Atrium", "SHD Safe House", "Extraction Helipad", "ISAC Relay Tower", "Contaminated Metro", "Cleaners Foundry"),
  );

  companion object {
    fun fromId(id: String): CustomMapGameCategory =
      entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: BO3
  }
}

@Immutable
data class UserCustomGameMapSpec(
  val id: String,
  val mapCode: String,
  val title: String,
  val gameCategory: CustomMapGameCategory = CustomMapGameCategory.BO3,
  val description: String,
  val buildingNames: List<String>,
  val wonderWeapon: String = gameCategory.defaultWonderWeapon,
  val accentHex: Long = gameCategory.defaultAccentHex,
  val secondaryHex: Long = 0xFF00E5FF,
  val isUserCreated: Boolean = true,
  val createdAtMillis: Long = 0L,
) {
  fun toCustomAreaSpec(): CustomAreaSpec {
    val zones = buildingNames.map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty {
      gameCategory.defaultZones
    }
    return CustomAreaSpec(
      id = id,
      name = title,
      linkedWorldId = gameCategory.linkedWorldId,
      description = description.ifBlank { "${gameCategory.label} custom playable map ($mapCode)." },
      accentHex = accentHex,
      secondaryHex = secondaryHex,
      buildingNames = zones,
      storyEraNote = "${gameCategory.label} • Custom Map ($mapCode)",
      uniqueTraits = "WW: $wonderWeapon • ${zones.size} Custom Zones • Safe Collection Custom Mode Ready",
    )
  }
}

object CuratedCustomMapsCatalog {
  val curatedBo3CustomMaps: List<UserCustomGameMapSpec> = listOf(
    UserCustomGameMapSpec(
      id = "area_bo3_custom_leviathan",
      mapCode = "zm_leviathan",
      title = "BO3 Custom: Leviathan",
      gameCategory = CustomMapGameCategory.BO3,
      description = "Iconic BO3 Mod Tools underwater submarine base with Moon-style PES airlocks, Pack-a-Punch Dock & Wave Gun.",
      buildingNames = listOf(
        "Submarine Wet Dock",
        "PES Airlock Atrium",
        "Reactor Dome Beta",
        "Pack-a-Punch Torpedo Bay",
        "Flooded Biodome",
        "Emergency Dive Escape",
      ),
      wonderWeapon = "Wave Gun / Zap Gun",
      accentHex = 0xFF00E5FF,
      secondaryHex = 0xFF00E676,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_bo3_custom_kowloon",
      mapCode = "zm_kowloon",
      title = "BO3 Custom: Kowloon",
      gameCategory = CustomMapGameCategory.BO3,
      description = "Custom BO3 Mod Tools rain-soaked Kowloon Walled City rooftops, neon ziplines, Dr. Monty Factory & Wunderfizz alleys.",
      buildingNames = listOf(
        "Neon Zipline Roof",
        "Kowloon Market Alley",
        "Walled City Courtyard",
        "Power Switch Substation",
        "Wunderfizz Noodle Hall",
        "Dragon Extraction Helipad",
      ),
      wonderWeapon = "Thundergun + Ray Gun Mk.III",
      accentHex = 0xFFFFAB40,
      secondaryHex = 0xFFFF3D71,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_bo3_custom_town",
      mapCode = "zm_town_reimagined",
      title = "BO3 Custom: Town Reimagined",
      gameCategory = CustomMapGameCategory.BO3,
      description = "BO3 Mod Tools lava-fissured Green Run Town remaster with Stamin-Up Bar, Bank Vault Pack & Tombstone Alley.",
      buildingNames = listOf(
        "Lava Courtyard",
        "Town Bank Vault",
        "Jugger-Nog Bar",
        "Speed Cola Diner",
        "Double Tap Garage",
        "Pack-a-Punch Crack",
      ),
      wonderWeapon = "Ray Gun Mk.II",
      accentHex = 0xFFFF5252,
      secondaryHex = 0xFFFFD54F,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_bo3_custom_herrenhaus",
      mapCode = "zm_herrenhaus",
      title = "BO3 Custom: Das Herrenhaus",
      gameCategory = CustomMapGameCategory.BO3,
      description = "Gothic BO3 Mod Tools haunted manor with Crypt Teleporters, Library Mystery Box & Element 115 Observatory.",
      buildingNames = listOf(
        "Manor Foyer Stairs",
        "Aether Conservatory",
        "Clockwork Library",
        "Catacomb Crypts",
        "Observatory Pack Tower",
        "Courtyard Fountain",
      ),
      wonderWeapon = "Wunderwaffe DG-2",
      accentHex = 0xFF00E676,
      secondaryHex = 0xFFE040FB,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_bo3_custom_cotd",
      mapCode = "zm_cotd_custom",
      title = "BO3 Custom: Call of the Dead",
      gameCategory = CustomMapGameCategory.BO3,
      description = "BO3 Mod Tools Siberian lighthouse & frozen tanker with Scavenger explosive sniper & V-R11 zipline routes.",
      buildingNames = listOf(
        "Frozen Coastline Spawn",
        "Lighthouse Spiral",
        "Aether Freighter Stern",
        "George Gorge Bridge",
        "Power Switch Cabin",
        "Scavenger Ice Cave",
      ),
      wonderWeapon = "Scavenger + V-R11",
      accentHex = 0xFF40C4FF,
      secondaryHex = 0xFFFFFFFF,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_bo3_custom_mob",
      mapCode = "zm_alcatraz_bo3",
      title = "BO3 Custom: Mob Remastered",
      gameCategory = CustomMapGameCategory.BO3,
      description = "BO3 Mod Tools Alcatraz Cellblock custom remaster with Blundergat Acidgat bench, Afterlife boxes & Golden Gate Pack.",
      buildingNames = listOf(
        "D-Block Cellblock",
        "Michigan Avenue",
        "Wardens Office Armory",
        "Infirmary Roof Dock",
        "Gondola Tower",
        "Golden Gate Pack Bridge",
      ),
      wonderWeapon = "Acidgat Blundergat",
      accentHex = 0xFFFF3D71,
      secondaryHex = 0xFFFFAB40,
      isUserCreated = false,
    ),
  )

  val curatedCollectionCustomMaps: List<UserCustomGameMapSpec> = listOf(
    UserCustomGameMapSpec(
      id = "area_col_custom_blacklight_rooftops",
      mapCode = "prot_redzone_rooftops",
      title = "Custom Prototype: Red-Zone Roofs",
      gameCategory = CustomMapGameCategory.PROTOTYPE,
      description = "Custom Blacklight Collection map across viral Times Square skyscrapers, Blackwatch drop pads & biomass hive vents.",
      buildingNames = listOf(
        "Red-Zone Antenna",
        "Gentek Skybridge",
        "Blackwatch Munitions",
        "Tendril Biomass Vat",
        "Mercer Extraction Pad",
        "Viral Containment Dome",
      ),
      wonderWeapon = "Mercer Bio-Claw Devastator",
      accentHex = 0xFFFF3D71,
      secondaryHex = 0xFF7C4DFF,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_infamous_pier",
      mapCode = "inf_stasis_pier",
      title = "Custom inFAMOUS: Stasis Pier",
      gameCategory = CustomMapGameCategory.INFAMOUS,
      description = "Custom Empire City conduit shipping yard with high-voltage crane pylons, Ray Sphere docks & Blast Shard barges.",
      buildingNames = listOf(
        "Conduit Crane Pylon",
        "Stasis Freighter Pier",
        "Neon Relay Warehouse",
        "Zeke's Dockside Rig",
        "First Sons Vault",
        "Ray-Sphere Lighthouse",
      ),
      wonderWeapon = "Ray-Sphere Conduit Core",
      accentHex = 0xFF00E5FF,
      secondaryHex = 0xFFFFD54F,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_arkham_lounge",
      mapCode = "arkham_iceberg_lounge",
      title = "Custom Arkham: Iceberg Lounge",
      gameCategory = CustomMapGameCategory.ARKHAM,
      description = "Custom Batman Arkham combat & stealth arena in Penguin's Iceberg Lounge, Bat-Grapple rafters & sewer vaults.",
      buildingNames = listOf(
        "Iceberg VIP Floor",
        "Glacial Shark Pit",
        "Gargoyle Roof Rafters",
        "Cobblepot Armory",
        "GCPD Sewer Breach",
        "Wayne Recon Pod",
      ),
      wonderWeapon = "Shock-Gloves Detective Core",
      accentHex = 0xFFFFD54F,
      secondaryHex = 0xFF448AFF,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_spider_helipad",
      mapCode = "spidey_oscorp_helipad",
      title = "Custom Spider-Man: Oscorp Spire",
      gameCategory = CustomMapGameCategory.SPIDERMAN,
      description = "Custom Spider-Verse skyline map spanning Oscorp's upper research helipad, web-zip cranes & Symbiote containment wings.",
      buildingNames = listOf(
        "Oscorp Upper Helipad",
        "Symbiote Containment",
        "Queens Sky-Zip Crane",
        "Horizon Collider Core",
        "Daily Bugle Relay",
        "Otto Lab Catwalk",
      ),
      wonderWeapon = "Iron-Spider Web Conduit",
      accentHex = 0xFFE53935,
      secondaryHex = 0xFFE040FB,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_gow_tartarus",
      mapCode = "gow_tartarus_forge",
      title = "Custom GoW: Tartarus Forge",
      gameCategory = CustomMapGameCategory.GODOFWAR,
      description = "Custom Crucible God of War map in the molten depths of Tartarus with chain-anchored trial islands & Hydra rifts.",
      buildingNames = listOf(
        "Tartarus Gatehouse",
        "Hephaestus Lava Anvil",
        "Chaos Chain Bridge",
        "Valkyrie Crucible",
        "Water Hydra Rift",
        "Spartan Glory Shrine",
      ),
      wonderWeapon = "Crucible BFG-9000",
      accentHex = 0xFFFF5252,
      secondaryHex = 0xFFFFAB40,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_division_mall",
      mapCode = "div_dz09_mall",
      title = "Custom Division: DZ-09 Mall",
      gameCategory = CustomMapGameCategory.DIVISION,
      description = "Custom SHD Dark Zone snowbound atrium & contaminated mall extraction zone with SHD supply boxes & Hunter turrets.",
      buildingNames = listOf(
        "DZ-09 Mall Atrium",
        "SHD Safe Cache",
        "Cleaners Incinerator",
        "Roof Helipad Extract",
        "ISAC Comms Balcony",
        "Subway Escalator Lock",
      ),
      wonderWeapon = "Liberty Wunder-Arc",
      accentHex = 0xFFFFAB40,
      secondaryHex = 0xFF00E5FF,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_gta4_liberty",
      mapCode = "gta4_liberty_city",
      title = "GTA IV: Liberty City (Broker & Algonquin)",
      gameCategory = CustomMapGameCategory.GTA4,
      description = "GTA IV Liberty City sandbox/DMZ street war from Niko's Hove Beach safehouse across Broker Bridge into Star Junction & Bank of Liberty.",
      buildingNames = listOf(
        "Hove Beach Safehouse",
        "Broker-Dukes Bridge",
        "Star Junction Plaza",
        "Bank of Liberty Vault",
        "NOOSE Tactical Precinct",
        "Bohan Industrial Depot",
      ),
      wonderWeapon = "Roman's Combat RPG & M4",
      accentHex = 0xFFFFAB40,
      secondaryHex = 0xFF00E676,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_gta4_alderney",
      mapCode = "gta4_alderney_statue",
      title = "GTA IV: Alderney & Happiness Island",
      gameCategory = CustomMapGameCategory.GTA4,
      description = "GTA IV West River Alderney Prison, Acter Industrial docks, Honkers strip, and Happiness Island 6-star NOOSE chopper standoff.",
      buildingNames = listOf(
        "Happiness Island Statue",
        "Alderney Correctional Pen",
        "Acter Industrial Docks",
        "Sprunk Bottling Plant",
        "Tudor Westdyke Safehouse",
        "Middle Park Helipad",
      ),
      wonderWeapon = "Annihilator Explosive Minigun",
      accentHex = 0xFFFF8F00,
      secondaryHex = 0xFF00E5FF,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_arkham_origins_bridge",
      mapCode = "arkham_origins_bridge",
      title = "Batman: Arkham Origins (Pioneers Bridge & Royal Hotel)",
      gameCategory = CustomMapGameCategory.ARKHAM,
      description = "Christmas Eve Arkham Origins assault across Pioneers Bridge cable towers, Royal Hotel penthouse, and Final Offer cargo deck against Deathstroke, Firefly & Bane.",
      buildingNames = listOf(
        "Pioneers Bridge Span",
        "Gotham Royal Hotel",
        "Final Offer Casino Ship",
        "Lacey Towers Penthouse",
        "GCPD Basement Precinct",
        "Coventry Comm Tower",
      ),
      wonderWeapon = "Electrocutioner Shock-Gloves",
      accentHex = 0xFFFFD54F,
      secondaryHex = 0xFF40C4FF,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_arkham_blackgate",
      mapCode = "arkham_origins_blackgate",
      title = "Batman: Arkham Origins Blackgate (3-Wing Prison)",
      gameCategory = CustomMapGameCategory.ARKHAM,
      description = "Sidescroller-inspired 3-wing prison riot spanning Penguin's Cell Blocks, Black Mask's Industrial Wing, and Joker's Administration Arcade.",
      buildingNames = listOf(
        "Cell Block A Lockdown",
        "Black Mask Industrial",
        "Joker Admin Wing",
        "Blackgate Lighthouse",
        "Sewer Smuggler Tunnels",
        "Warden Armory Hub",
      ),
      wonderWeapon = "Gel-Launcher Detective Visor",
      accentHex = 0xFF448AFF,
      secondaryHex = 0xFFFFD54F,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_arkham_asylum_city",
      mapCode = "arkham_asylum_city_knight",
      title = "Arkham Asylum, Arkham City & Knight (Panessa)",
      gameCategory = CustomMapGameCategory.ARKHAM,
      description = "Classic Arkham trilogy gauntlet uniting Arkham Asylum Botanical Gardens, Arkham City Wonder Tower, and Arkham Knight Panessa Studios.",
      buildingNames = listOf(
        "Asylum Intensive Care",
        "Botanical Titan Hall",
        "Wonder Tower Observation",
        "Sionis Steel Mill",
        "Panessa Movie Studios",
        "Elliott Memorial Hospital",
      ),
      wonderWeapon = "Batmobile Disruptor & Freeze Blast",
      accentHex = 0xFFFFAB40,
      secondaryHex = 0xFF7C4DFF,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_superman_metropolis",
      mapCode = "superman_returns_metropolis",
      title = "Superman Returns: Metropolis Skyline",
      gameCategory = CustomMapGameCategory.SUPERMAN,
      description = "Soar across Metropolis Daily Planet, LexCorp Tower, Centennial Park, and Hob's Bay shutting down tornadoes, Metallo bots & Kryptonite asteroids.",
      buildingNames = listOf(
        "Daily Planet Globe Roof",
        "LexCorp Kryptonite Spire",
        "Centennial Park Harbor",
        "Hob's Bay Power Station",
        "Suicide Slums Overpass",
        "Metropolis Spaceport Pad",
      ),
      wonderWeapon = "Solar Heat-Vision Overdrive",
      accentHex = 0xFF2979FF,
      secondaryHex = 0xFFFF3D71,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_superman_warworld",
      mapCode = "superman_warworld_fortress",
      title = "Superman Returns: Warworld & Fortress",
      gameCategory = CustomMapGameCategory.SUPERMAN,
      description = "Arena combat shifting between Mongul's subterranean Warworld Gladiator coliseum and crystalline Fortress of Solitude solar chambers.",
      buildingNames = listOf(
        "Warworld Arena Pit",
        "Mongul Throne Balcony",
        "Fortress Crystal Dais",
        "Phantom Zone Projector",
        "Solar Recharge Chamber",
        "New Krypton Shard Core",
      ),
      wonderWeapon = "Fortress Solar Super-Breath",
      accentHex = 0xFF00E5FF,
      secondaryHex = 0xFFFF5252,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_abyss_crypt",
      mapCode = "abyss_howling_crypts",
      title = "Abyss of Dungeons: Howling Crypts",
      gameCategory = CustomMapGameCategory.ABYSS_DUNGEONS,
      description = "Dark fantasy dungeon extraction crawl through torch-lit Howling Crypts, Runic Anvils, Mimic Treasury vaults, and Goblin Cave shrines.",
      buildingNames = listOf(
        "Howling Crypt Entrance",
        "Torch Shrine Crossing",
        "Mimic Treasury Vault",
        "Runic Forge Anvil",
        "Goblin Cave Bridge",
        "Blue Stone Extraction",
      ),
      wonderWeapon = "Abyssal Lich Staff / Greatsword",
      accentHex = 0xFFB388FF,
      secondaryHex = 0xFFFFAB40,
      isUserCreated = false,
    ),
    UserCustomGameMapSpec(
      id = "area_col_custom_abyss_lich_sanctum",
      mapCode = "abyss_lich_sanctum",
      title = "Abyss of Dungeons: Inferno Lich Sanctum",
      gameCategory = CustomMapGameCategory.ABYSS_DUNGEONS,
      description = "High-roller Abyssal descent into Inferno blood altars, Hellhound kennels, Cursed Tombstones, and the Ghost King & Lich Lord boss sanctum.",
      buildingNames = listOf(
        "Inferno Blood Altar",
        "Skeleton Champion Keep",
        "Hellhound Kennel Ring",
        "Soul Relic Extractor",
        "Spectral Knight Gallery",
        "Lich King Boss Throne",
      ),
      wonderWeapon = "Fulgor Runic Longsword",
      accentHex = 0xFFFF3D71,
      secondaryHex = 0xFFB388FF,
      isUserCreated = false,
    ),
  )

  val allCuratedCustomMaps: List<UserCustomGameMapSpec> =
    curatedBo3CustomMaps + curatedCollectionCustomMaps

  fun findById(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): UserCustomGameMapSpec? =
    userCustomMaps.firstOrNull { it.id == areaId }
      ?: allCuratedCustomMaps.firstOrNull { it.id == areaId }

  fun resolveAreaSpec(
    areaId: String,
    userCustomMaps: List<UserCustomGameMapSpec> = emptyList(),
  ): CustomAreaSpec {
    findById(areaId, userCustomMaps)?.let { return it.toCustomAreaSpec() }
    return MultiverseArsenalCatalog.areaById(areaId)
  }

  fun allPlayableAreas(userCustomMaps: List<UserCustomGameMapSpec> = emptyList()): List<CustomAreaSpec> {
    val customSpecs = (userCustomMaps + allCuratedCustomMaps)
      .distinctBy { it.id }
      .map { it.toCustomAreaSpec() }
    return (MultiverseArsenalCatalog.customAreas + customSpecs).distinctBy { it.id }
  }
}

@Immutable
data class MagicSystemState(
  val unlocked: Boolean = true,
  val activeSpellId: String = "spell_abyssal_nova",
  val manaCurrent: Float = 100f,
  val manaMax: Float = 100f,
  val magicTier: Int = 1,
  val difficultyScalingFactor: Float = 1.20f,
  val unlockedSpells: Set<String> = setOf("spell_abyssal_nova", "spell_chrono_stasis", "spell_soul_heal"),
)

@Immutable
data class PlayerBaseState(
  val baseName: String = "Nexus Command Citadel",
  val wallTier: Int = 1,
  val turretTier: Int = 1,
  val shieldGeneratorTier: Int = 1,
  val resourceVaultTier: Int = 1,
  val storedGold: Long = 850L,
  val storedDiamonds: Long = 200L,
  val underAttack: Boolean = false,
  val attackWaveCountdownSec: Int = 300,
  val defenseSuccessCount: Int = 0,
)

@Immutable
data class CrystalItem(
  val id: String,
  val name: String,
  val rarity: String,
  val statBonusSummary: String,
  val damageBoostPct: Int,
  val shieldBoostPct: Int,
  val luckBoostPct: Int,
  val socketedSlot: String? = null,
  val level: Int = 1,
)

fun defaultCrystals(): List<CrystalItem> = listOf(
  CrystalItem("crystal_kyber", "Kyber Resonance Crystal", "Mythic", "+15% Energy DMG & +20% Shield", 15, 20, 5, "weapon", 1),
  CrystalItem("crystal_aether", "Aether 115 Shard Crystal", "Legendary", "+12% Fire Rate & +10% Loot Luck", 12, 10, 10, "rig", 1),
  CrystalItem("crystal_vibranium", "Vibranium Plating Crystal", "Epic", "+25% Max Shield & Armor Regen", 0, 25, 0, "chest", 1),
  CrystalItem("crystal_chrono", "Chrono-Stasis Temporal Crystal", "Cosmic", "-20% Cooldowns & +18% Move Speed", 10, 15, 12, "helmet", 1),
)






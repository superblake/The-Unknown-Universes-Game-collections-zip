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
  STAR_10(10, "10★ Omniverse Nova", 3.25f, 0xFFFFFFFF);

  companion object {
    const val MAX_STARS: Int = 10

    fun forStars(stars: Int): StarRankMeta =
      entries.firstOrNull { it.stars == stars.coerceIn(1, MAX_STARS) } ?: STAR_10
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
)

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
  val maxTier: Int = 25,
  val starLevel: Int = 1,
) {
  fun nextUpgradeCost(): Long = costEssence + (tier * 250L)
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
) {
  val playerLevel: Int
    get() = ProgressionCatalog.levelForXp(playerXp)

  val playerRankTitle: String
    get() = ProgressionCatalog.rankTitleForLevel(playerLevel, playerPrestige)

  fun weaponXp(weaponId: String): Long = weaponXpMap[weaponId] ?: 0L

  fun weaponLevel(weaponId: String): Int = ProgressionCatalog.weaponLevelForXp(weaponXp(weaponId))

  fun weaponPrestige(weaponId: String): Int = weaponPrestigeMap[weaponId] ?: 0

  fun weaponTier(weaponId: String): Int = (weaponTierMap[weaponId] ?: 1).coerceIn(1, 25)

  fun weaponStar(weaponId: String): Int = (weaponStarMap[weaponId] ?: 1).coerceIn(1, StarRankMeta.MAX_STARS)

  fun wonderTier(wonderId: String): Int = (wonderTierMap[wonderId] ?: 1).coerceIn(1, 25)

  fun wonderStar(wonderId: String): Int = (wonderStarMap[wonderId] ?: 1).coerceIn(1, StarRankMeta.MAX_STARS)

  fun armoryItemStar(itemId: String): Int = (armoryItemStarMap[itemId] ?: 1).coerceIn(1, StarRankMeta.MAX_STARS)

  fun equipmentState(slotId: String): EquipmentState =
    equipmentMap[slotId] ?: EquipmentState(slotId, if (slotId == "lethal") "lethal_frag" else "tactical_stim", 1, 0, 0L, "standard", 1, 1)

  fun outfitState(id: String = equippedOutfitId): OutfitProgressState =
    outfitStates[id] ?: OutfitProgressState(id)

  fun activeOutfitSpec(): HeroOutfitSpec =
    MultiverseArsenalCatalog.outfitById(equippedOutfitId)

  fun areaState(id: String = activeAreaId): CustomAreaState =
    areaStates[id] ?: CustomAreaState(id)

  fun activeAreaSpec(): CustomAreaSpec =
    MultiverseArsenalCatalog.areaById(activeAreaId)

  fun perkRank(perkId: String): Int = (perkRanks[perkId] ?: 0).coerceIn(0, 25)

  fun blastCoreCount(color: BlastCoreColor): Long = (blastCoreInventory[color.id] ?: 0L).coerceAtLeast(0L)

  fun blastCoreLevel(color: BlastCoreColor): Int = (blastCoreLevels[color.id] ?: 1).coerceIn(1, 20)

  fun essenceResonance(blueEssence: Long): EssenceResonanceBonuses =
    EssenceResonanceBonuses.compute(
      blueEssence = blueEssence,
      redEssence = redEssence,
      yellowEssence = yellowEssence,
      greenEssence = greenEssence,
      combinedEssence = combinedEssence,
      blastShards = blastShards,
    )

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
  )

  fun weaponById(id: String): WeaponSpec =
    weapons.firstOrNull { it.id == id } ?: weapons.first()

  val camos: List<Pair<String, String>> = listOf(
    "cyber_matte" to "Cyber Matte",
    "molten_damascus" to "Molten Damascus",
    "pack_tier_gold" to "Pack-a-Tier Gold",
    "blacklight_neon" to "Blacklight Neon",
    "chrono_chrome" to "Chrono Chrome",
    "omega_prismatic" to "Omega Celestial",
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
    SkillNode("sk_shield_1", "Reinforced Chassis I", "+25 Max Shield per Tier (up to T25: +625 Shield).", "SURVIVAL", 300L, false, 1, 25),
    SkillNode("sk_shield_2", "Reinforced Chassis II", "+35 Max Shield & +1 Regen/sec per Tier (up to T25).", "SURVIVAL", 600L, false, 1, 25),
    SkillNode("sk_damage_1", "Ballistic Lethality I", "+2 Bolt Damage per Tier (up to T25: +50 DMG).", "COMBAT", 350L, false, 1, 25),
    SkillNode("sk_damage_2", "Ballistic Lethality II", "+3 Bolt Damage & +1 Pierce every 2 Tiers (T1-T25).", "COMBAT", 750L, false, 1, 25),
    SkillNode("sk_cooldown_1", "Overdrive Capacitor", "-6% Cooldowns & +3% Move Speed per Tier (up to T25).", "ENGINEERING", 450L, false, 1, 25),
    SkillNode("sk_wonder_1", "Mystery Crate Attractor", "+30% Wonder Drop Rate & +15% Wonder Duration per Tier (T1-T25).", "WONDER", 550L, false, 1, 25),
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
    AchievementSpec("ach_war_32", "Total Multiverse War", "Deploy into a maximum 32-Player Lobby.", "LOBBY", 32L, 1500L, 2200L, 8),
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
    val totalSlots = config.targetPlayers.coerceIn(1, 32)
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
        val botName = botCallsigns[(slotIdx - 1) % botCallsigns.size]
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
    EquipmentItemSpec("lethal_frag", "Frag Grenade", EquipmentSlotType.LETHAL, "High explosive shrapnel grenade with wide blast radius.", 160f, 6.0f, 0xFFFF5722),
    EquipmentItemSpec("lethal_molotov", "Napalm Molotov", EquipmentSlotType.LETHAL, "Deploys a fiery hazard zone burning hordes over time.", 120f, 8.0f, 0xFFFF9800),
    EquipmentItemSpec("lethal_c4", "Remote C4 Satchel", EquipmentSlotType.LETHAL, "Detonated manually for heavy explosive breaching damage.", 240f, 12.0f, 0xFFE53935),
    EquipmentItemSpec("lethal_cluster", "Arc Cluster Mine", EquipmentSlotType.LETHAL, "Deploys homing arc sub-munitions seeking elite targets.", 190f, 10.0f, 0xFF7C4DFF),
    EquipmentItemSpec("tactical_stim", "Nano-Stim Injector", EquipmentSlotType.TACTICAL, "Restores 60 shield instantly and boosts movement speed.", 60f, 5.0f, 0xFF00E5FF),
    EquipmentItemSpec("tactical_emp", "EMP Shockwave", EquipmentSlotType.TACTICAL, "Disables enemy energy barriers and stuns nearby targets.", 90f, 9.0f, 0xFF00B0FF),
    EquipmentItemSpec("tactical_decoy", "Holographic Decoy", EquipmentSlotType.TACTICAL, "Projects a decoy emitter that draws hostile aggro.", 0f, 14.0f, 0xFF1DE9B6),
    EquipmentItemSpec("tactical_insertion", "Chrono Beacon", EquipmentSlotType.TACTICAL, "Spawns a revival checkpoint and continuous shield aura.", 110f, 20.0f, 0xFF651FFF),
  )

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
  fun perkDrink(rank: Int): Long = 250L + (rank * 200L)
  fun vacuumBot(tier: Int): Long = 220L + (tier * 160L)
  fun vacuumBotStar(stars: Int): Long = stars * 400L
}



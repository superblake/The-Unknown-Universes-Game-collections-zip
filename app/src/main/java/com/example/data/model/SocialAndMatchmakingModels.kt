package com.example.data.model

import androidx.compose.runtime.Immutable

enum class MatchPrivacy(
  val id: String,
  val displayName: String,
  val subtitle: String,
) {
  PUBLIC("public", "Public Match", "Open to all operators across the multiverse"),
  PRIVATE_PASSWORD("private_pass", "Password Protected", "Private match requiring an entry passcode"),
  INVITE_ONLY("invite_only", "Invite / Code Only", "Accessible only via unique room code"),
  FRIENDS_ONLY("friends_only", "Friends Only", "Open only to operators on your friends list"),
  CLAN_ONLY("clan_only", "Clan Only", "Restricted to members of your active clan"),
}

enum class FriendOnlineStatus(
  val id: String,
  val label: String,
  val colorHex: Long,
) {
  ONLINE_LOBBY("online_lobby", "In Lobby", 0xFF00E676),
  IN_ARENA("in_arena", "In Combat", 0xFFFF9100),
  ONLINE_IDLE("online_idle", "Idle at OS", 0xFF00E5FF),
  OFFLINE("offline", "Offline", 0xFF9E9E9E),
}

enum class ChatChannel(
  val id: String,
  val title: String,
  val description: String,
) {
  GENERAL("general", "General Chat", "Global Multiverse operator communications"),
  LIVE_MATCH("live_match", "Live Tactical", "Real-time combat callouts & match banter"),
  DIRECT("direct", "Direct Whisper", "1-on-1 private messaging with friends"),
  CLAN("clan", "Clan Comms", "Internal syndicate & clan battle operations"),
}

enum class ClanRank(
  val id: String,
  val title: String,
  val priority: Int,
) {
  LEADER("leader", "Clan Leader", 4),
  OFFICER("officer", "Officer", 3),
  VETERAN("veteran", "Veteran", 2),
  RECRUIT("recruit", "Recruit", 1),
}

@Immutable
data class PrivateMatchRules(
  val isPrivate: Boolean = true,
  val privacy: MatchPrivacy = MatchPrivacy.PRIVATE_PASSWORD,
  val roomName: String = "Omega Convergence Strike",
  val roomCode: String = "GENESIS-774",
  val passcode: String = "7744",
  val startingWave: Int = 1,
  val maxPlayers: Int = 8,
  val botFillEnabled: Boolean = true,
  val botDifficulty: BotDifficulty = BotDifficulty.OPERATIVE,
  val worldId: String = "campaign_origins",
  val areaId: String = "area_mended_convergence",
  val modeId: String = "campaign",
  val infiniteAmmo: Boolean = false,
  val headshotsOnly: Boolean = false,
  val bossRush: Boolean = false,
  val wonderSpawnBoost: Boolean = false,
  val lowGravity: Boolean = false,
  val doubleSpeed: Boolean = false,
  val vampiricSiphon: Boolean = false,
  val doubleBlueEssence: Boolean = false,
  val friendlyFire: Boolean = false,
  val customWaveGoal: Int = 0,
) {
  val activeMutatorCount: Int
    get() = listOf(
      infiniteAmmo, headshotsOnly, bossRush, wonderSpawnBoost,
      lowGravity, doubleSpeed, vampiricSiphon, doubleBlueEssence, friendlyFire
    ).count { it }

  fun activeMutatorLabels(): List<String> {
    val list = mutableListOf<String>()
    if (infiniteAmmo) list.add("Infinite Ammo")
    if (headshotsOnly) list.add("Instagib / Headshots")
    if (bossRush) list.add("Boss Rush")
    if (wonderSpawnBoost) list.add("Wonder Boost")
    if (lowGravity) list.add("Low Gravity")
    if (doubleSpeed) list.add("2x Speed")
    if (vampiricSiphon) list.add("Vampiric Siphon")
    if (doubleBlueEssence) list.add("2x Blue Essence")
    if (friendlyFire) list.add("Hardcore Fire")
    if (customWaveGoal > 0) list.add("Goal: W$customWaveGoal")
    return list
  }
}

@Immutable
data class MultiverseMatchListing(
  val id: String,
  val roomName: String,
  val hostCallsign: String,
  val hostLevel: Int,
  val hostStars: Int,
  val worldId: String,
  val worldTitle: String,
  val modeId: String,
  val modeLabel: String,
  val areaId: String,
  val areaName: String,
  val currentPlayers: Int,
  val maxPlayers: Int,
  val privacy: MatchPrivacy,
  val roomCode: String,
  val passcode: String? = null,
  val pingMs: Int,
  val currentWave: Int,
  val activeMutators: List<String>,
  val clanTag: String? = null,
) {
  val isFull: Boolean get() = currentPlayers >= maxPlayers
  val isPrivate: Boolean get() = privacy != MatchPrivacy.PUBLIC
}

@Immutable
data class FriendEntry(
  val id: String,
  val callsign: String,
  val level: Int,
  val stars: Int,
  val tier: Int,
  val avatarThemeId: String = "vanguard_cyber",
  val status: FriendOnlineStatus = FriendOnlineStatus.ONLINE_LOBBY,
  val statusDetail: String = "In Lobby",
  val currentMatchId: String? = null,
  val clanTag: String? = null,
  val isFavorite: Boolean = false,
)

@Immutable
data class ChatMessage(
  val id: String,
  val channel: ChatChannel,
  val senderCallsign: String,
  val senderClanTag: String? = null,
  val senderStars: Int = 1,
  val targetFriendId: String? = null,
  val content: String,
  val timestampMillis: Long = System.currentTimeMillis(),
  val isTacticalQuickChat: Boolean = false,
  val isSystemMessage: Boolean = false,
)

@Immutable
data class ClanMember(
  val id: String,
  val callsign: String,
  val rank: ClanRank,
  val level: Int,
  val stars: Int,
  val donatedEssence: Long,
  val online: Boolean = true,
)

@Immutable
data class ClanData(
  val id: String = "clan_sentinels",
  val name: String = "Sentinels of Convergence",
  val tag: String = "CONV",
  val motto: String = "Reality mended, multiverse defended.",
  val level: Int = 4,
  val tier: Int = 4,
  val starLevel: Int = 3,
  val totalEssenceDonated: Long = 18_500L,
  val leaderCallsign: String = "Apex_Conduit",
  val colorHex: Long = 0xFF00E5FF,
  val members: List<ClanMember> = emptyList(),
) {
  val essencePerkMultiplier: Float
    get() = 1.0f + (level * 0.05f).coerceAtMost(0.75f)

  val wonderPerkMultiplier: Float
    get() = 1.0f + (level * 0.03f).coerceAtMost(0.50f)

  val armorSiphonPerk: Int
    get() = level * 4

  val botDamageBoost: Int
    get() = level * 3
}

object SocialCatalog {
  val quickTacticalCallouts = listOf(
    "📢 Need Ammo & Pack-a-Punch!",
    "🚨 Boss Titan Incoming!",
    "⚡ Activate Electric Trap!",
    "🔥 Trap West Corridor!",
    "🌟 Wonder Weapon Ready!",
    "🛡️ Fall Back & Regroup!",
    "💉 Cover Me, Reviving!",
    "💎 Blue Essence Stash Here!",
  )

  val defaultClan = ClanData(
    id = "clan_sentinels",
    name = "Sentinels of Convergence",
    tag = "CONV",
    motto = "Reality mended, multiverse defended.",
    level = 4,
    tier = 4,
    starLevel = 3,
    totalEssenceDonated = 18_500L,
    leaderCallsign = "Apex_Conduit",
    colorHex = 0xFF00E5FF,
    members = listOf(
      ClanMember("m_1", "Apex_Conduit", ClanRank.LEADER, 50, 10, 8_000L, true),
      ClanMember("m_2", "Nightwing_Bludhaven", ClanRank.OFFICER, 42, 8, 4_500L, true),
      ClanMember("m_3", "Miles_1610", ClanRank.OFFICER, 38, 7, 3_200L, true),
      ClanMember("m_4", "Zeke_Empire", ClanRank.VETERAN, 31, 5, 1_800L, false),
      ClanMember("m_5", "Galen_Starkiller", ClanRank.VETERAN, 45, 9, 1_000L, true),
    ),
  )

  val recommendedClans = listOf(
    defaultClan,
    ClanData(
      id = "clan_gotham",
      name = "Wayne Shadow Syndicate",
      tag = "GOTH",
      motto = "In the dark we stand vigil.",
      level = 6,
      tier = 5,
      starLevel = 4,
      totalEssenceDonated = 32_000L,
      leaderCallsign = "DarkKnight_Prime",
      colorHex = 0xFFFFD54F,
      members = listOf(
        ClanMember("g_1", "DarkKnight_Prime", ClanRank.LEADER, 60, 10, 15_000L, true),
        ClanMember("g_2", "RedHood_Arsenal", ClanRank.OFFICER, 48, 8, 9_000L, true),
      ),
    ),
    ClanData(
      id = "clan_blacklight",
      name = "Blacklight Conduits",
      tag = "BLCK",
      motto = "Evolve or disintegrate.",
      level = 5,
      tier = 5,
      starLevel = 3,
      totalEssenceDonated = 25_000L,
      leaderCallsign = "Cole_Kessler",
      colorHex = 0xFF00E5FF,
      members = listOf(
        ClanMember("b_1", "Cole_Kessler", ClanRank.LEADER, 55, 9, 12_000L, true),
        ClanMember("b_2", "Mercer_Apex", ClanRank.OFFICER, 52, 9, 10_000L, true),
      ),
    ),
    ClanData(
      id = "clan_stark",
      name = "Stark Defense Vanguard",
      tag = "STARK",
      motto = "Bleeding-edge multiverse security.",
      level = 7,
      tier = 6,
      starLevel = 5,
      totalEssenceDonated = 45_000L,
      leaderCallsign = "IronCommander",
      colorHex = 0xFFFF3D71,
      members = listOf(
        ClanMember("s_1", "IronCommander", ClanRank.LEADER, 60, 10, 20_000L, true),
        ClanMember("s_2", "WarMachine_Rhodey", ClanRank.OFFICER, 44, 7, 12_000L, true),
      ),
    ),
    ClanData(
      id = "clan_requiem",
      name = "Requiem Aether Division",
      tag = "AETH",
      motto = "Survive the outbreak across all dimensions.",
      level = 8,
      tier = 7,
      starLevel = 6,
      totalEssenceDonated = 60_000L,
      leaderCallsign = "Tank_Dempsey",
      colorHex = 0xFF00E676,
      members = listOf(
        ClanMember("r_1", "Tank_Dempsey", ClanRank.LEADER, 60, 10, 30_000L, true),
        ClanMember("r_2", "Nikolai_Heavy", ClanRank.OFFICER, 54, 9, 18_000L, true),
      ),
    ),
  )

  val defaultFriends = listOf(
    FriendEntry(
      id = "friend_nightwing",
      callsign = "Nightwing_Bludhaven",
      level = 42,
      stars = 8,
      tier = 7,
      status = FriendOnlineStatus.IN_ARENA,
      statusDetail = "In Combat: Batcave Sanctuary (Wave 16)",
      currentMatchId = "match_gotham_batcave",
      clanTag = "GOTH",
      isFavorite = true,
    ),
    FriendEntry(
      id = "friend_miles",
      callsign = "Miles_1610",
      level = 38,
      stars = 7,
      tier = 6,
      status = FriendOnlineStatus.ONLINE_LOBBY,
      statusDetail = "In Lobby (Duos)",
      clanTag = "CONV",
      isFavorite = true,
    ),
    FriendEntry(
      id = "friend_cole",
      callsign = "Cole_Kessler",
      level = 55,
      stars = 9,
      tier = 9,
      status = FriendOnlineStatus.IN_ARENA,
      statusDetail = "In Combat: Empire City Core (Wave 24)",
      currentMatchId = "match_empire_public",
      clanTag = "BLCK",
      isFavorite = true,
    ),
    FriendEntry(
      id = "friend_galen",
      callsign = "Galen_Starkiller",
      level = 45,
      stars = 9,
      tier = 8,
      status = FriendOnlineStatus.ONLINE_LOBBY,
      statusDetail = "In Lobby (Mega Squad 12P)",
      clanTag = "CONV",
      isFavorite = false,
    ),
    FriendEntry(
      id = "friend_rhodey",
      callsign = "WarMachine_Rhodey",
      level = 44,
      stars = 7,
      tier = 7,
      status = FriendOnlineStatus.ONLINE_IDLE,
      statusDetail = "Idle at OS Armory",
      clanTag = "STARK",
      isFavorite = false,
    ),
    FriendEntry(
      id = "friend_dempsey",
      callsign = "Tank_Dempsey",
      level = 60,
      stars = 10,
      tier = 10,
      status = FriendOnlineStatus.IN_ARENA,
      statusDetail = "In Combat: Genesis Fracture (Wave 35)",
      currentMatchId = "match_genesis_campaign",
      clanTag = "AETH",
      isFavorite = true,
    ),
    FriendEntry(
      id = "friend_mercer",
      callsign = "Mercer_Apex",
      level = 52,
      stars = 9,
      tier = 8,
      status = FriendOnlineStatus.ONLINE_LOBBY,
      statusDetail = "In Lobby (Squad)",
      clanTag = "BLCK",
      isFavorite = false,
    ),
    FriendEntry(
      id = "friend_zeke",
      callsign = "Zeke_Empire",
      level = 31,
      stars = 5,
      tier = 5,
      status = FriendOnlineStatus.OFFLINE,
      statusDetail = "Offline (2 hours ago)",
      clanTag = "CONV",
      isFavorite = false,
    ),
  )

  val defaultChatMessages = listOf(
    ChatMessage(
      id = "chat_gen_1",
      channel = ChatChannel.GENERAL,
      senderCallsign = "Nightwing_Bludhaven",
      senderClanTag = "GOTH",
      senderStars = 8,
      content = "Just upgraded the Batarang launcher to Tier 25 Multiversal. The bounce velocity in the Convergence is insane!",
      timestampMillis = System.currentTimeMillis() - 600_000L,
    ),
    ChatMessage(
      id = "chat_gen_2",
      channel = ChatChannel.GENERAL,
      senderCallsign = "Tank_Dempsey",
      senderClanTag = "AETH",
      senderStars = 10,
      content = "Ray Nova MK3 Pack-a-Punch Tier 25 cuts through Hellbreaker bosses like butter. Who's ready for a 16P Mega run?",
      timestampMillis = System.currentTimeMillis() - 480_000L,
    ),
    ChatMessage(
      id = "chat_gen_3",
      channel = ChatChannel.GENERAL,
      senderCallsign = "Miles_1610",
      senderClanTag = "CONV",
      senderStars = 7,
      content = "Donated 3,200 Blue Essence to Clan Sentinels. Our essence boost is now +20% for everyone!",
      timestampMillis = System.currentTimeMillis() - 300_000L,
    ),
    ChatMessage(
      id = "chat_live_1",
      channel = ChatChannel.LIVE_MATCH,
      senderCallsign = "Cole_Kessler",
      senderClanTag = "BLCK",
      senderStars = 9,
      content = "🚨 Boss Titan Incoming! Activate Electric Traps on East Gate!",
      timestampMillis = System.currentTimeMillis() - 120_000L,
      isTacticalQuickChat = true,
    ),
    ChatMessage(
      id = "chat_live_2",
      channel = ChatChannel.LIVE_MATCH,
      senderCallsign = "Galen_Starkiller",
      senderClanTag = "CONV",
      senderStars = 9,
      content = "🌟 Wonder Weapon Ready! Overdrive channeled, pushing into courtyard!",
      timestampMillis = System.currentTimeMillis() - 60_000L,
      isTacticalQuickChat = true,
    ),
    ChatMessage(
      id = "chat_clan_1",
      channel = ChatChannel.CLAN,
      senderCallsign = "Apex_Conduit",
      senderClanTag = "CONV",
      senderStars = 10,
      content = "Welcome to Sentinels of Convergence [CONV]. Keep leveling your hero outfits and donating to unlock the +50% Blue Essence clan perk!",
      timestampMillis = System.currentTimeMillis() - 900_000L,
      isSystemMessage = true,
    ),
    ChatMessage(
      id = "chat_clan_2",
      channel = ChatChannel.CLAN,
      senderCallsign = "Nightwing_Bludhaven",
      senderClanTag = "CONV",
      senderStars = 8,
      content = "Running private match 'GENESIS-774' with 2x Blue Essence and Boss Rush mutators. Room code is in clan lobby!",
      timestampMillis = System.currentTimeMillis() - 240_000L,
    ),
  )

  val sampleMatches = listOf(
    MultiverseMatchListing(
      id = "match_genesis_campaign",
      roomName = "Campaign: Mended Convergence Finale",
      hostCallsign = "Apex_Conduit",
      hostLevel = 50,
      hostStars = 10,
      worldId = "campaign_origins",
      worldTitle = "Campaign: Genesis Fracture",
      modeId = "campaign",
      modeLabel = "Story Campaign",
      areaId = "area_mended_convergence",
      areaName = "The Mended Convergence",
      currentPlayers = 6,
      maxPlayers = 8,
      privacy = MatchPrivacy.PUBLIC,
      roomCode = "CONV-101",
      pingMs = 24,
      currentWave = 14,
      activeMutators = listOf("Wonder Boost", "2x Blue Essence"),
      clanTag = "CONV",
    ),
    MultiverseMatchListing(
      id = "match_genesis_private",
      roomName = "🔒 High-Command Tactical Sim",
      hostCallsign = "Galen_Starkiller",
      hostLevel = 45,
      hostStars = 9,
      worldId = "campaign_origins",
      worldTitle = "Campaign: Genesis Fracture",
      modeId = "campaign",
      modeLabel = "Story Campaign",
      areaId = "area_campaign_nexus",
      areaName = "Campaign Command",
      currentPlayers = 3,
      maxPlayers = 4,
      privacy = MatchPrivacy.PRIVATE_PASSWORD,
      roomCode = "GENESIS-774",
      passcode = "7744",
      pingMs = 19,
      currentWave = 8,
      activeMutators = listOf("Infinite Ammo", "Boss Rush", "Hardcore Fire"),
      clanTag = "CONV",
    ),
    MultiverseMatchListing(
      id = "match_empire_public",
      roomName = "Empire City Blacklight Defense",
      hostCallsign = "Cole_Kessler",
      hostLevel = 55,
      hostStars = 9,
      worldId = "blacklight_conduit",
      worldTitle = "Blacklight Conduit: Metro",
      modeId = "zombies",
      modeLabel = "Zombies Wave",
      areaId = "area_empire_city",
      areaName = "Empire City",
      currentPlayers = 4,
      maxPlayers = 4,
      privacy = MatchPrivacy.PUBLIC,
      roomCode = "EMPIRE-202",
      pingMs = 32,
      currentWave = 24,
      activeMutators = listOf("Instagib / Headshots"),
      clanTag = "BLCK",
    ),
    MultiverseMatchListing(
      id = "match_gotham_batcave",
      roomName = "🔒 Batcave Lockdown Trial",
      hostCallsign = "Nightwing_Bludhaven",
      hostLevel = 42,
      hostStars = 8,
      worldId = "aegis_starblade",
      worldTitle = "Aegis & Starblade: Arkham & Heroes",
      modeId = "zombies",
      modeLabel = "Zombies Wave",
      areaId = "area_batcave",
      areaName = "Batcave: Dark Sanctum",
      currentPlayers = 2,
      maxPlayers = 2,
      privacy = MatchPrivacy.FRIENDS_ONLY,
      roomCode = "GOTH-550",
      pingMs = 28,
      currentWave = 16,
      activeMutators = listOf("Low Gravity", "2x Speed"),
      clanTag = "GOTH",
    ),
    MultiverseMatchListing(
      id = "match_stark_mega",
      roomName = "Stark Tower 16-Player Mega Defense",
      hostCallsign = "IronCommander",
      hostLevel = 60,
      hostStars = 10,
      worldId = "hellbreaker_citadel",
      worldTitle = "Hellbreaker: Mythic Crucible",
      modeId = "multiplayer",
      modeLabel = "Arena Skirmish",
      areaId = "area_ironman_stark",
      areaName = "Stark Tower: Hall of Armor",
      currentPlayers = 11,
      maxPlayers = 16,
      privacy = MatchPrivacy.PUBLIC,
      roomCode = "STARK-900",
      pingMs = 45,
      currentWave = 5,
      activeMutators = listOf("Boss Rush", "Wonder Boost", "2x Blue Essence"),
      clanTag = "STARK",
    ),
    MultiverseMatchListing(
      id = "match_total_war_32",
      roomName = "Total Multiverse War (32 Operators)",
      hostCallsign = "Tank_Dempsey",
      hostLevel = 60,
      hostStars = 10,
      worldId = "campaign_origins",
      worldTitle = "Campaign: Genesis Fracture",
      modeId = "campaign",
      modeLabel = "Story Campaign",
      areaId = "area_mended_convergence",
      areaName = "The Mended Convergence",
      currentPlayers = 27,
      maxPlayers = 32,
      privacy = MatchPrivacy.PUBLIC,
      roomCode = "WAR-3200",
      pingMs = 52,
      currentWave = 9,
      activeMutators = listOf("Wonder Boost", "2x Blue Essence", "2x Speed"),
      clanTag = "AETH",
    ),
    MultiverseMatchListing(
      id = "match_queens_private",
      roomName = "🔒 Web-Slinger Stealth Course",
      hostCallsign = "Miles_1610",
      hostLevel = 38,
      hostStars = 7,
      worldId = "aegis_starblade",
      worldTitle = "Aegis & Starblade: Arkham & Heroes",
      modeId = "br",
      modeLabel = "Multiverse BR",
      areaId = "area_spiderman_queens",
      areaName = "Queens: Oscorp Rooftops",
      currentPlayers = 1,
      maxPlayers = 2,
      privacy = MatchPrivacy.INVITE_ONLY,
      roomCode = "SPIDER-161",
      pingMs = 18,
      currentWave = 6,
      activeMutators = listOf("Low Gravity", "Vampiric Siphon"),
      clanTag = "CONV",
    ),
    MultiverseMatchListing(
      id = "match_clan_scrim",
      roomName = "🔒 Sentinels Clan Tactical Scrim",
      hostCallsign = "Apex_Conduit",
      hostLevel = 50,
      hostStars = 10,
      worldId = "campaign_origins",
      worldTitle = "Campaign: Genesis Fracture",
      modeId = "campaign",
      modeLabel = "Story Campaign",
      areaId = "area_mended_convergence",
      areaName = "The Mended Convergence",
      currentPlayers = 4,
      maxPlayers = 8,
      privacy = MatchPrivacy.CLAN_ONLY,
      roomCode = "CONV-CLAN",
      pingMs = 21,
      currentWave = 12,
      activeMutators = listOf("Wonder Boost", "2x Blue Essence", "Boss Rush"),
      clanTag = "CONV",
    ),
  )
}

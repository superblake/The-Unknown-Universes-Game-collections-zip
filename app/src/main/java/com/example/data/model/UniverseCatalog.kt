package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R

data class UniverseWorld(
  val id: String,
  val title: String,
  val subtitle: String,
  val inspirations: String,
  val description: String,
  val signatureMechanic: String,
  val defaultModeId: String,
  val accentHex: Long,
  val secondaryHex: Long,
  @param:DrawableRes val bannerRes: Int,
  val supportedModes: List<String>,
  val mapSectors: List<String>,
  val interactActionLabel: String = "CACHE",
  val platformBadges: String = "PS5/4/3 • XBOX • SWITCH • PC",
)

data class GameModeSpec(
  val id: String,
  val label: String,
  val shortTag: String,
  val summary: String,
  val winCondition: String,
  val accentHex: Long,
)

data class ConsoleCompatTarget(
  val id: String,
  val label: String,
  val platformFamily: String,
  val buttonLegend: List<String>,
  val browserEngineNote: String,
  val accentHex: Long,
)

data class RigBoneSpec(
  val id: String,
  val label: String,
  val parentId: String?,
  val minAngle: Float,
  val maxAngle: Float,
  val defaultAngle: Float,
)

object UniverseCatalog {

  private val allModeIds: List<String> = listOf(
    "zombies", "dmz", "br", "multiplayer", "sandbox", "campaign",
    "hero_patrol", "boss_rush", "onslaught", "conduit_ctf",
  )

  val worlds: List<UniverseWorld> = listOf(
    UniverseWorld(
      id = "sector_zero",
      title = "Sector Zero: Undead Ops",
      subtitle = "TUU Call of Duty Stories • Quarantine & Exfil",
      inspirations = "TUU Call of Duty Stories • Zombies & DMZ • The Division SHD",
      description = "Hold containment lines against escalating spectral-undead hordes, dial Pack-a-Tier weapon stations, or drop into contested DMZ sectors with ISAC scan rings for dark-tech extraction.",
      signatureMechanic = "Mystery Cache + Pack-a-Tier + Exfil Chopper",
      defaultModeId = "zombies",
      accentHex = 0xFFFF8F00,
      secondaryHex = 0xFF00E5FF,
      bannerRes = R.drawable.img_sector_zero_banner_1794591189085,
      supportedModes = allModeIds,
      mapSectors = listOf("Quarantine Yard", "Sub-Zero Bunker", "Helipad Exfil", "ISAC Relay"),
      interactActionLabel = "PACK-A-TIER",
      platformBadges = "PS5/PS4/PS3 • XBOX 360/X|S • PC",
    ),
    UniverseWorld(
      id = "blacklight_conduit",
      title = "Blacklight Conduit: Metro",
      subtitle = "TUU Prototype & Infamous Journey • Mr Crossover",
      inspirations = "TUU Prototype (Gentek/Water Hive) • TUU Infamous (DUP) • Mr Crossover",
      description = "Traverse Red-Zone Gentek sectors and Liberty Metro relays with mutated crimson tendril blades, chain-lightning arcs, and Water Hive bio-storms against DUP and Blackwatch.",
      signatureMechanic = "Bio-Tendril Slash + Arc Pylon Overload",
      defaultModeId = "sandbox",
      accentHex = 0xFF7C4DFF,
      secondaryHex = 0xFFFF3D71,
      bannerRes = R.drawable.img_conduit_citadel_banner_1794591189064,
      supportedModes = allModeIds,
      mapSectors = listOf("Gentek Red-Zone", "Water Hive Lab", "DUP Conduit Spire", "Liberty Viaduct"),
      interactActionLabel = "ARC PYLON",
      platformBadges = "PS3/PS4/PS5 • XBOX • SWITCH • PC",
    ),
    UniverseWorld(
      id = "hellbreaker_citadel",
      title = "Hellbreaker: Mythic Crucible",
      subtitle = "TUU God of War & Necromorph Crucible",
      inspirations = "TUU God of War • Doom Crucible • Wolverine & Necromorph Clash",
      description = "Chain glory-dashes, mythic blades, and twin adamantium-molten claws through cyber-fiends, Killer Croc juggernauts, and assault mechs in zero-cooldown crucible fury.",
      signatureMechanic = "Twin-Claw Berserk + Glory Siphon Dash",
      defaultModeId = "zombies",
      accentHex = 0xFFFF5252,
      secondaryHex = 0xFFFFAB40,
      bannerRes = R.drawable.img_hellbreaker_banner_1794596418917,
      supportedModes = allModeIds,
      mapSectors = listOf("Crucible Core", "Water Hydra Rift", "Molten Forge", "Mech Arena"),
      interactActionLabel = "BERSERK",
      platformBadges = "PS5/PS4 • XBOX X|S • SWITCH • PC",
    ),
    UniverseWorld(
      id = "chrono_vault",
      title = "Chrono-Vault: Space & Time",
      subtitle = "Space and Time • 7-Chevron Gate Archive",
      inspirations = "Space and Time • Stargate SG • Doctor Who • The Librarians",
      description = "Lock all 7 ancient chevrons inside the Space and Time Continuation Hub, wielding sonic resonance tools and Librarian artifacts to unite all 8 radial sectors.",
      signatureMechanic = "7-Chevron Gate Dialer + Time-Freeze Stasis",
      defaultModeId = "dmz",
      accentHex = 0xFFFFD54F,
      secondaryHex = 0xFF448AFF,
      bannerRes = R.drawable.img_chrono_aegis_banner_1794591190129,
      supportedModes = allModeIds,
      mapSectors = listOf("Annex Prime", "Chevron Vault", "Space-Time Nexus", "Origins Ring"),
      interactActionLabel = "DIAL GATE",
      platformBadges = "PS5/4/3 • XBOX • NINTENDO • PC • iOS",
    ),
    UniverseWorld(
      id = "aegis_starblade",
      title = "Aegis & Starblade: Force Unleashed",
      subtitle = "Starkiller • Orbital Sith & Jedi Crucible",
      inspirations = "Starkiller • Force Unleashed • Twin Kyber Blades",
      description = "Unleash Force-repulse shockwaves, chain-lightning sabers, and Kamino clone dreadnought duels high above orbital citadels.",
      signatureMechanic = "Force Repulse + Twin-Core Saber Throw",
      defaultModeId = "multiplayer",
      accentHex = 0xFF00E5FF,
      secondaryHex = 0xFFE040FB,
      bannerRes = R.drawable.img_aegis_starblade_banner_1794596418262,
      supportedModes = allModeIds,
      mapSectors = listOf("Star Destroyer Hull", "Kamino Clone Spire", "Kyber Sanctum", "Orbital Citadel"),
      interactActionLabel = "REPULSE",
      platformBadges = "PS5/PS4/PS3 • XBOX • SWITCH • PC",
    ),
    UniverseWorld(
      id = "gotham_arkham",
      title = "Gotham: Arkham Knightfall",
      subtitle = "Batman v1–v3 • Batcave • Black Gate & DC",
      inspirations = "Batman • TUU Arkham Origins • TUU DC Heroes • Black Gate",
      description = "Glide above Gotham rooftops, Black Gate cellblocks, and the Batcave sub-vault as Batman v1/Hellbat/Beyond battling Joker enforcers, Killer Croc, and Bane Titans.",
      signatureMechanic = "Bat-Signal Recon + Grapnel Detective Pulse",
      defaultModeId = "hero_patrol",
      accentHex = 0xFFFFD54F,
      secondaryHex = 0xFF448AFF,
      bannerRes = R.drawable.img_aegis_starblade_banner_1794596418262,
      supportedModes = allModeIds,
      mapSectors = listOf("Batcave Nexus", "Black Gate Yard", "Ace Chemicals", "GCPD Bat-Signal Roof"),
      interactActionLabel = "BAT-SIGNAL",
      platformBadges = "PS5/PS4/PS3 • XBOX • SWITCH • PC • iOS",
    ),
    UniverseWorld(
      id = "spiderverse_queens",
      title = "Spider-Verse: Queens & Oscorp",
      subtitle = "Spider-Man v1–v3 • Symbiote & Iron Spider",
      inspirations = "Spider-Man • Queens • Oscorp Bio-Spire • Daily Bugle • Venom",
      description = "Web-zip across Queens skyline bridges, Oscorp Bio-Spires, and Symbiote subways as Classic, Symbiote, or Iron Spider fighting Venom Stalkers and Goblin Sky-Drones.",
      signatureMechanic = "Spider-Sense Dodge + Web-Zip Arc Burst",
      defaultModeId = "hero_patrol",
      accentHex = 0xFFE53935,
      secondaryHex = 0xFF2979FF,
      bannerRes = R.drawable.img_conduit_citadel_banner_1794591189064,
      supportedModes = allModeIds,
      mapSectors = listOf("Queens Skyline", "Oscorp Bio-Spire", "Daily Bugle Plaza", "Symbiote Subway"),
      interactActionLabel = "WEB-ZIP",
      platformBadges = "PS5/PS4/PS3 • XBOX • SWITCH • PC • iOS",
    ),
    UniverseWorld(
      id = "stark_orbital",
      title = "Stark Tower: Hall of Armor",
      subtitle = "Iron Man v1–v3 • Arc Reactor & Hulkbuster",
      inspirations = "Iron Man • Mark III • Bleeding Edge • Veronica Hulkbuster",
      description = "Defend the palladium Arc Reactor Core and J.A.R.V.I.S. orbital flight deck with twin repulsor barrages, micro-missiles, and Hulkbuster orbital drops.",
      signatureMechanic = "Unibeam Reactor Overdrive + Repulsor Matrix",
      defaultModeId = "multiplayer",
      accentHex = 0xFFFF3D71,
      secondaryHex = 0xFFFFD54F,
      bannerRes = R.drawable.img_hellbreaker_banner_1794596418917,
      supportedModes = allModeIds,
      mapSectors = listOf("Arc Reactor Core", "Hall of Armor", "Jarvis Flight Deck", "Hulkbuster Drop Pad"),
      interactActionLabel = "UNIBEAM",
      platformBadges = "PS5/4 • XBOX X|S • SWITCH • PC • iOS",
    ),
    UniverseWorld(
      id = "empire_city_infamous",
      title = "Empire City: Infamous Journey",
      subtitle = "TUU Infamous Journey • Cole McGrath v1–v3",
      inspirations = "TUU Infamous Journey • Ray Sphere • Blast Shards • DUP",
      description = "Channel high-voltage lightning across Neon District rooftops, Warren Substations, and Alden's Spire while draining Blast Shards to shatter Reaper & DUP blockades.",
      signatureMechanic = "Ray-Sphere Conduit Storm + Rail Grinder",
      defaultModeId = "sandbox",
      accentHex = 0xFF00E5FF,
      secondaryHex = 0xFFFFD54F,
      bannerRes = R.drawable.img_conduit_citadel_banner_1794591189064,
      supportedModes = allModeIds,
      mapSectors = listOf("Neon Substation", "Warren Power Grid", "Alden's Spire", "Historic Blast Plaza"),
      interactActionLabel = "ARC DRAIN",
      platformBadges = "PS3/PS4/PS5 • XBOX • SWITCH • PC",
    ),
    UniverseWorld(
      id = "ship_of_heroes_apotheosis",
      title = "Ship of Heroes: Apotheosis",
      subtitle = "Orbital Starship Command • Original Hero Fleet",
      inspirations = "Ship of Heroes • Apotheosis Justice Bridge • Cosmic Armada",
      description = "Rally original starship cadets and Celestial Admirals aboard the FHS Apotheosis orbital city—repelling Void Corsairs and Cosmic Raiders across multi-deck hangars.",
      signatureMechanic = "Apotheosis Nova Strike + Squad Shield Bubble",
      defaultModeId = "multiplayer",
      accentHex = 0xFFE040FB,
      secondaryHex = 0xFF00E5FF,
      bannerRes = R.drawable.img_aegis_starblade_banner_1794596418262,
      supportedModes = allModeIds,
      mapSectors = listOf("Justice Bridge", "Nebula Hangar Bay", "Photon Core", "Starport Promenade"),
      interactActionLabel = "NOVA STRIKE",
      platformBadges = "PS5/4/3 • XBOX • SWITCH • PC • iOS",
    ),
    UniverseWorld(
      id = "mr_crossover_nexus",
      title = "Mr. Crossover: Infinite Nexus",
      subtitle = "All Universes Converged • Original Creator Hub",
      inspirations = "Mr Crossover • Batman • Spider-Man • Cole • Mercer • CoD",
      description = "The ultimate zero-boundary convergence where Batman, Spider-Man, Cole McGrath, Alex Mercer, Starkiller, Iron Man, and TUU Originals fight side by side across reality.",
      signatureMechanic = "Quad-Essence Prism Nova + Cross-Universe Portal",
      defaultModeId = "onslaught",
      accentHex = 0xFF00E676,
      secondaryHex = 0xFFE040FB,
      bannerRes = R.drawable.img_chrono_aegis_banner_1794591190129,
      supportedModes = allModeIds,
      mapSectors = listOf("Convergence Ring", "Crossover Bastion", "All-Hero Plaza", "Outer Rift Horizon"),
      interactActionLabel = "PRISM NOVA",
      platformBadges = "ALL PLATFORMS • HTML5 • CONSOLES • PC",
    ),
    UniverseWorld(
      id = "campaign_origins",
      title = "Campaign: Genesis Fracture",
      subtitle = "TUU Main Story • Worlds & Areas Awakening",
      inspirations = "UnknownOS Story Campaign • Gotham • Empire City • Stark • Der Riese",
      description = "Play the central Multiverse Story Campaign from the first Rift Breach to uniting all heroes, worlds, and healing areas right up to The Mended Convergence before reality is fully healed.",
      signatureMechanic = "Story Chapter Relays + Reality Anchor Surge",
      defaultModeId = "campaign",
      accentHex = 0xFF40C4FF,
      secondaryHex = 0xFFFFD54F,
      bannerRes = R.drawable.img_chrono_aegis_banner_1794591190129,
      supportedModes = allModeIds,
      mapSectors = listOf("Prologue Drop Deck", "Coalition Frontline", "Shattered Nexus Bridge", "Reality Mend Spire"),
      interactActionLabel = "ANCHOR",
      platformBadges = "PS5/4/3 • XBOX • SWITCH • PC • MOBILE",
    ),
  )

  val modes: List<GameModeSpec> = listOf(
    GameModeSpec(
      id = "zombies",
      label = "Zombies Wave",
      shortTag = "ZMB",
      summary = "Round-based survival with Mystery Cache, Perk Cores, and Pack-a-Tier upgrades.",
      winCondition = "Survive escalating waves & stack Blue Essence multipliers",
      accentHex = 0xFF00E676,
    ),
    GameModeSpec(
      id = "dmz",
      label = "DMZ Extraction",
      shortTag = "DMZ",
      summary = "High-stakes sector infiltration—secure Intel Cores and exfil before radiation collapse.",
      winCondition = "Collect 3+ Intel Cores & reach the Exfil Beacon",
      accentHex = 0xFFFFAB40,
    ),
    GameModeSpec(
      id = "br",
      label = "Multiverse BR",
      shortTag = "BR",
      summary = "Shrinking dimensional ring with contested supply drops and rival squad bots.",
      winCondition = "Outlast all rival squads inside the shrinking rift",
      accentHex = 0xFFFF5252,
    ),
    GameModeSpec(
      id = "multiplayer",
      label = "Arena Skirmish",
      shortTag = "MP",
      summary = "Fast-tempo tactical combat with scorestreaks, powers, and rig animations.",
      winCondition = "Reach 25 eliminations with active loadout streaks",
      accentHex = 0xFF00E5FF,
    ),
    GameModeSpec(
      id = "sandbox",
      label = "UnknownOS Sandbox",
      shortTag = "SBX",
      summary = "Open testing ground for custom Blender-rigged avatars, powers, and physics.",
      winCondition = "Experiment freely with custom rigs & consumables",
      accentHex = 0xFFB388FF,
    ),
    GameModeSpec(
      id = "campaign",
      label = "Story Campaign",
      shortTag = "CMP",
      summary = "Chapter-based Multiverse campaign advancing missions and reality healing.",
      winCondition = "Complete story waves, stabilize anchors & defeat Rift Sovereigns",
      accentHex = 0xFF40C4FF,
    ),
    GameModeSpec(
      id = "hero_patrol",
      label = "Hero Vigilante Patrol",
      shortTag = "HERO",
      summary = "Batman & Spider-Man rooftop patrol: rescue distress beacons, stop villain squads & harvest hero shards.",
      winCondition = "Protect sectors, intercept ambushes & recruit hero shards",
      accentHex = 0xFFFFD54F,
    ),
    GameModeSpec(
      id = "boss_rush",
      label = "Apex Boss Rush",
      shortTag = "BOSS",
      summary = "Every single wave summons a Multiverse Boss (Venom, Killer Croc, Hydra, Bane, Starkiller) with Wonder drops.",
      winCondition = "Chain boss eliminations for mythic shard & tier explosions",
      accentHex = 0xFFFF3D71,
    ),
    GameModeSpec(
      id = "onslaught",
      label = "Horde Onslaught Blitz",
      shortTag = "HORD",
      summary = "Dense 1.5x enemy horde blitz with accelerated cooldowns and +50% Colored Essence payouts.",
      winCondition = "Clear surging multiverse swarms under nonstop pressure",
      accentHex = 0xFFE040FB,
    ),
    GameModeSpec(
      id = "conduit_ctf",
      label = "Conduit Relay Holdout",
      shortTag = "RELIC",
      summary = "Charge Verdansk sector relays with faster Electric & Fire Trap cycles and bonus Shard drops.",
      winCondition = "Hold sector relays & overwhelm waves with trap resonance",
      accentHex = 0xFF00B0FF,
    ),
  )

  val consoleTargets: List<ConsoleCompatTarget> = listOf(
    ConsoleCompatTarget(
      id = "universal_web",
      label = "Universal HTML5 Build",
      platformFamily = "All Browsers • Zero Store Lock-in",
      buttonLegend = listOf("Gamepad API", "Touch Stick", "WASD / Space"),
      browserEngineNote = "Self-contained single-file HTML5 Canvas build runnable offline on iOS, Consoles & PC.",
      accentHex = 0xFF00E5FF,
    ),
    ConsoleCompatTarget(
      id = "ps5_dualsense",
      label = "PS5 / PS4 / PS3 Bridge",
      platformFamily = "PlayStation Web / Gamepad Profile",
      buttonLegend = listOf("✕ Fire", "○ Dash", "□ Reload", "△ Power", "L1/R1 Perks"),
      browserEngineNote = "Maps standard W3C Gamepad axes & face buttons to PlayStation DualSense / DualShock indices.",
      accentHex = 0xFF448AFF,
    ),
    ConsoleCompatTarget(
      id = "xbox_series",
      label = "Xbox Series / One / 360",
      platformFamily = "Xbox Edge Browser & Gamepad API",
      buttonLegend = listOf("A Fire", "B Dash", "X Reload", "Y Power", "LB/RB Perks"),
      browserEngineNote = "Ready for Xbox Edge browser full-screen execution with native XInput-to-Gamepad mapping.",
      accentHex = 0xFF00E676,
    ),
    ConsoleCompatTarget(
      id = "switch_handheld",
      label = "Nintendo Switch / Handheld",
      platformFamily = "Compact 720p/1080p Console Layout",
      buttonLegend = listOf("B Fire", "A Dash", "Y Reload", "X Power", "ZL/ZR Surge"),
      browserEngineNote = "High-contrast 16:9 handheld HUD with Nintendo face-button swap and D-pad focus rings.",
      accentHex = 0xFFFF5252,
    ),
    ConsoleCompatTarget(
      id = "ios_safari",
      label = "iOS Safari / iPadOS PWA",
      platformFamily = "WebKit Touch + MFi Controller",
      buttonLegend = listOf("Virtual Stick", "Tap Power", "MFi Ready", "Home-Screen PWA"),
      browserEngineNote = "Includes iOS apple-mobile-web-app viewport tags and touch-safe canvas controls—no App Store needed.",
      accentHex = 0xFFFFD54F,
    ),
    ConsoleCompatTarget(
      id = "pc_desktop",
      label = "PC / Steam Deck / Linux",
      platformFamily = "Desktop & Handheld PC",
      buttonLegend = listOf("WASD Move", "Click/J Fire", "E Power", "Q Consumable", "1-4 Perks"),
      browserEngineNote = "Supports simultaneous keyboard + mouse + Steam Deck controller input at 60 FPS.",
      accentHex = 0xFFE040FB,
    ),
  )

  val rigBones: List<RigBoneSpec> = listOf(
    RigBoneSpec("torso", "Spine / Torso", null, -45f, 45f, 0f),
    RigBoneSpec("head", "Head / Visor", "torso", -40f, 40f, 0f),
    RigBoneSpec("lUpperArm", "L. Upper Arm", "torso", -135f, 135f, -24f),
    RigBoneSpec("lForearm", "L. Forearm", "lUpperArm", -130f, 30f, -35f),
    RigBoneSpec("rUpperArm", "R. Upper Arm", "torso", -135f, 135f, 28f),
    RigBoneSpec("rForearm", "R. Forearm", "rUpperArm", -30f, 130f, 40f),
    RigBoneSpec("lThigh", "L. Thigh", "torso", -85f, 85f, -14f),
    RigBoneSpec("lShin", "L. Shin", "lThigh", -10f, 120f, 16f),
    RigBoneSpec("rThigh", "R. Thigh", "torso", -85f, 85f, 16f),
    RigBoneSpec("rShin", "R. Shin", "rThigh", -120f, 10f, -12f),
    RigBoneSpec("weaponAngle", "Weapon / Blade Socket", "rForearm", -90f, 90f, -15f),
  )

  fun defaultPoseMap(): Map<String, Double> =
    rigBones.associate { it.id to it.defaultAngle.toDouble() }

  fun worldById(id: String): UniverseWorld =
    worlds.firstOrNull { it.id == id } ?: worlds.first()

  fun modeById(id: String): GameModeSpec =
    modes.firstOrNull { it.id == id } ?: modes.first()

  fun consoleById(id: String): ConsoleCompatTarget =
    consoleTargets.firstOrNull { it.id == id } ?: consoleTargets.first()

  /**
   * Embedded No-APK Play Guide bundled directly inside the Game Collection Catalog.
   * Explains how to launch exported collections in browsers, GitHub Pages, .hta, PWA, and console controllers.
   */
  const val NO_APK_PLAY_GUIDE: String = """
# The Unknown Universes — Game Collections No-APK Play Guide

This guide explains how to easily run, share, and play **The Unknown Universes** game collections on any device (PC, Mac, Linux, iOS, Android, and consoles) **without needing an Android APK**.

1. Instant Browser Play (No Installation Required):
   - Export a portable game package from the Export & Compat menu and save as index.html.
   - Double-click index.html or drop it into Chrome, Safari, Edge, or Firefox for full offline canvas rendering, touch controls, audio, and gamepad support.

2. Free Cloud Hosting (GitHub Pages / Vercel):
   - Upload your exported index.html to a public GitHub repo root, enable Settings -> Pages on main/root, and share the live link.

3. Windows Desktop App (.hta) or PWA:
   - Rename index.html to game.hta to run in a standalone desktop window on Windows, or install via Chrome/Edge as a Progressive Web App.

4. Console & Controller Support:
   - Connect any Xbox, PlayStation DualSense, or Switch Pro controller; the W3C Gamepad API automatically maps axes and face buttons.
"""
}

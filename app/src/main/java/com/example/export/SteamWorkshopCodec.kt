package com.example.export

import com.example.BuildConfig
import com.example.data.local.SaveDataSerializer
import com.example.data.model.ArmoryItem
import com.example.data.model.CuratedCustomMapsCatalog
import com.example.data.model.CustomMapGameCategory
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.data.model.UserCustomGameMapSpec
import com.example.data.remote.FirestoreSyncCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class WorkshopVisibility(val code: Int, val label: String) {
  PUBLIC(0, "Public"),
  FRIENDS_ONLY(1, "Friends Only"),
  HIDDEN(2, "Private / Hidden"),
  UNLISTED(3, "Unlisted");

  companion object {
    fun fromCode(code: Int): WorkshopVisibility =
      entries.firstOrNull { it.code == code } ?: PUBLIC
  }
}

data class SteamWorkshopExportConfig(
  val appId: String = "480",
  val publishedFileId: String = "0",
  val title: String,
  val description: String,
  val changeNote: String = "Uploaded from UnknownOS v${BuildConfig.VERSION_NAME}",
  val visibility: WorkshopVisibility = WorkshopVisibility.PUBLIC,
  val tags: List<String> = listOf("UnknownOS", "Rig", "Armory"),
  val includeRigs: Boolean = true,
  val includeArmory: Boolean = true,
  val includeBlenderScript: Boolean = true,
  val authorSteamId: String = "",
  val authorPersona: String = "",
)

data class SteamWorkshopPublishedDetails(
  val publishedFileId: String,
  val title: String,
  val description: String,
  val creatorSteamId: String,
  val consumerAppId: String,
  val fileName: String = "",
  val fileSize: Long = 0L,
  val fileUrl: String? = null,
  val previewUrl: String? = null,
  val tags: List<String> = emptyList(),
  val subscriptions: Long = 0L,
  val favorited: Long = 0L,
  val views: Long = 0L,
  val timeUpdatedEpochSec: Long = 0L,
  val visibility: Int = 0,
)

data class SteamWorkshopImportPreview(
  val sourceKind: String,
  val title: String,
  val description: String,
  val publishedFileId: String?,
  val appId: String?,
  val creatorSteamId: String?,
  val tags: List<String>,
  val subscriptions: Long = 0L,
  val views: Long = 0L,
  val armoryItems: List<ArmoryItem>,
  val customRigs: List<RigAsset>,
  val blenderRigScript: String? = null,
  val generatedModule: ArmoryItem? = null,
  val generatedRig: RigAsset? = null,
  val sourceNote: String = "",
  val linkedWorldId: String? = null,
  val linkedAreaId: String? = null,
  val customMap: UserCustomGameMapSpec? = null,
)

data class GameCollectionWorkshopPreset(
  val key: String,
  val appId: String,
  val label: String,
  val gameTitle: String,
  val universeTag: String,
  val linkedWorldId: String,
  val linkedAreaId: String,
  val archetype: String,
  val accentHex: String,
  val sampleWorkshopId: String,
  val modNames: List<Pair<String, String>>,
  val description: String,
)

object SteamWorkshopCodec {

  const val WORKSHOP_FORMAT_MARKER = "unknown_universes_workshop_v1"
  const val SCHEMA_VERSION = 1
  private const val STEAM_PUBLISHED_FILE_API =
    "https://api.steampowered.com/ISteamRemoteStorage/GetPublishedFileDetails/v1/"
  private const val MAX_REMOTE_PAYLOAD_BYTES = 1_500_000

  val collectionGamePresets: List<GameCollectionWorkshopPreset> = listOf(
    GameCollectionWorkshopPreset(
      key = "bo3",
      appId = "311210",
      label = "BO3 Mod Tools (311210)",
      gameTitle = "Call of Duty: Black Ops III Mod Tools",
      universeTag = "BO3 Mod Tools",
      linkedWorldId = "sector_zero",
      linkedAreaId = "area_bo3_the_giant",
      archetype = "operator",
      accentHex = "#00E5FF",
      sampleWorkshopId = "311210001",
      modNames = listOf(
        "BO3 Pack-a-Punch Mk.III" to "upgrade",
        "BO3 Wunderfizz Overload" to "perk",
      ),
      description = "BO3 Mod Tools .XMODEL/.XANIM/.GDT perks, Wonder Weapons & custom Zombies maps (Nacht, The Giant, Gorod Krovi, Shadows).",
    ),
    GameCollectionWorkshopPreset(
      key = "prototype",
      appId = "115320",
      label = "Prototype (115320)",
      gameTitle = "Prototype / Blacklight Bio-Hive",
      universeTag = "TUU Prototype",
      linkedWorldId = "blacklight_conduit",
      linkedAreaId = "area_blacklight_hive",
      archetype = "biomorph",
      accentHex = "#FF3D71",
      sampleWorkshopId = "115320001",
      modNames = listOf(
        "Blacklight Tendril Cleaver" to "power",
        "Mercer Devastator Mass" to "perk",
      ),
      description = "Alex Mercer bio-blade tendrils, Water Hive armor plating, and Gentek viral surge modules.",
    ),
    GameCollectionWorkshopPreset(
      key = "arkham",
      appId = "209000",
      label = "Batman Arkham (209000)",
      gameTitle = "Batman: Arkham Origins, Blackgate & City",
      universeTag = "TUU Arkham",
      linkedWorldId = "gotham_arkham",
      linkedAreaId = "area_gotham_blackgate",
      archetype = "operator",
      accentHex = "#FFD54F",
      sampleWorkshopId = "209000001",
      modNames = listOf(
        "Shock-Gloves Detective Pulse" to "power",
        "Hellbat Kevlar Weave" to "perk",
      ),
      description = "Arkham Origins Pioneers Bridge, Origins Blackgate 3-wing prison, Asylum/City/Knight stealth & Shock Gloves.",
    ),
    GameCollectionWorkshopPreset(
      key = "gta4",
      appId = "12210",
      label = "GTA IV Liberty City (12210)",
      gameTitle = "GTA IV: Liberty City & Episodes",
      universeTag = "GTA IV Liberty",
      linkedWorldId = "sector_zero",
      linkedAreaId = "area_col_custom_gta4_liberty",
      archetype = "operator",
      accentHex = "#FFAB40",
      sampleWorkshopId = "12210001",
      modNames = listOf(
        "Roman's RPG & Combat M4" to "power",
        "Liberty Kevlar Safehouse Rig" to "perk",
      ),
      description = "Broker Hove Beach, Algonquin Star Junction, Bank of Liberty heists, and Alderney NOOSE 6-star defense.",
    ),
    GameCollectionWorkshopPreset(
      key = "superman",
      appId = "208650",
      label = "Superman Returns (208650)",
      gameTitle = "Superman Returns: Metropolis & Warworld",
      universeTag = "Superman Metropolis",
      linkedWorldId = "mr_crossover_nexus",
      linkedAreaId = "area_col_custom_superman_metropolis",
      archetype = "chrono",
      accentHex = "#2979FF",
      sampleWorkshopId = "208650001",
      modNames = listOf(
        "Solar Heat-Vision Super-Flare" to "power",
        "Kryptonian Fortress Shield" to "perk",
      ),
      description = "Daily Planet rooftop rescues, LexCorp Kryptonite disruption, and Mongul Warworld arena duels.",
    ),
    GameCollectionWorkshopPreset(
      key = "abyss_dungeons",
      appId = "2016590",
      label = "Abyss of Dungeons (2016590)",
      gameTitle = "Abyss of Dungeons: Howling Crypts",
      universeTag = "Abyss of Dungeons",
      linkedWorldId = "hellbreaker_citadel",
      linkedAreaId = "area_col_custom_abyss_crypt",
      archetype = "infernal",
      accentHex = "#B388FF",
      sampleWorkshopId = "201659001",
      modNames = listOf(
        "Fulgor Abyssal Lich Blade" to "power",
        "Runic Torch Relic Protection" to "perk",
      ),
      description = "Dark fantasy crypt crawls, Mimic vault extractions, Runic Forge upgrades, and Lich Sanctum raids.",
    ),
    GameCollectionWorkshopPreset(
      key = "spiderman",
      appId = "1817070",
      label = "Spider-Man (1817070)",
      gameTitle = "Spider-Verse: Queens & Oscorp",
      universeTag = "Spider-Verse",
      linkedWorldId = "spiderverse_queens",
      linkedAreaId = "area_spiderman_queens",
      archetype = "biomorph",
      accentHex = "#E53935",
      sampleWorkshopId = "181707001",
      modNames = listOf(
        "Iron-Spider Waldoes Volley" to "power",
        "Symbiote Reflex Surge" to "perk",
      ),
      description = "Queens rooftop web-zip accelerators, Symbiote tendril combos, and Oscorp suit modules.",
    ),
    GameCollectionWorkshopPreset(
      key = "godofwar",
      appId = "1593500",
      label = "God of War (1593500)",
      gameTitle = "TUU God of War: Mythic Crucible",
      universeTag = "TUU God of War",
      linkedWorldId = "hellbreaker_citadel",
      linkedAreaId = "area_hellbreaker_crucible",
      archetype = "infernal",
      accentHex = "#FF5252",
      sampleWorkshopId = "159350001",
      modNames = listOf(
        "Crucible Chaos Chain Slam" to "power",
        "Spartan Rage Siphon" to "perk",
      ),
      description = "Leviathan frost cleaves, Chaos blade shockwaves, and Crucible zero-cooldown fury.",
    ),
    GameCollectionWorkshopPreset(
      key = "division",
      appId = "365590",
      label = "The Division (365590)",
      gameTitle = "Sector Zero: SHD Dark Zone",
      universeTag = "Sector Zero SHD",
      linkedWorldId = "sector_zero",
      linkedAreaId = "area_sector_zero",
      archetype = "operator",
      accentHex = "#FFAB40",
      sampleWorkshopId = "365590001",
      modNames = listOf(
        "ISAC Seeker Cluster Mine" to "power",
        "SHD Striker Battlegear" to "perk",
      ),
      description = "SHD ISAC tactical pulses, Dark Zone extraction turrets, and Striker armor calibration.",
    ),
    GameCollectionWorkshopPreset(
      key = "force_unleashed",
      appId = "32430",
      label = "Force Unleashed (32430)",
      gameTitle = "Aegis & Starblade: Force Unleashed",
      universeTag = "Aegis Starblade",
      linkedWorldId = "aegis_starblade",
      linkedAreaId = "area_aegis_orbital",
      archetype = "starblade",
      accentHex = "#00E5FF",
      sampleWorkshopId = "32430001",
      modNames = listOf(
        "Starkiller Force Repulse" to "power",
        "Twin Kyber Saber Crystal" to "upgrade",
      ),
      description = "Starkiller orbital Force shockwaves, twin Kyber saber throws, and Kamino clone bay rigs.",
    ),
    GameCollectionWorkshopPreset(
      key = "ironman",
      appId = "997070",
      label = "Iron Man / Stark (997070)",
      gameTitle = "Stark Tower: Hall of Armor",
      universeTag = "Stark Orbital",
      linkedWorldId = "stark_orbital",
      linkedAreaId = "area_ironman_stark",
      archetype = "chrono",
      accentHex = "#FF3D71",
      sampleWorkshopId = "997070001",
      modNames = listOf(
        "Bleeding Edge Unibeam Core" to "power",
        "Veronica Arc Shield Matrix" to "perk",
      ),
      description = "Palladium Arc Reactor repulsor salvos, J.A.R.V.I.S. flight stabilization, and Hulkbuster plating.",
    ),
    GameCollectionWorkshopPreset(
      key = "ship_of_heroes",
      appId = "1884640",
      label = "Ship of Heroes (1884640)",
      gameTitle = "Ship of Heroes: Apotheosis Fleet",
      universeTag = "Ship of Heroes",
      linkedWorldId = "ship_of_heroes_apotheosis",
      linkedAreaId = "area_ship_of_heroes_deck",
      archetype = "chrono",
      accentHex = "#E040FB",
      sampleWorkshopId = "188464001",
      modNames = listOf(
        "Apotheosis Photon Nova" to "power",
        "Starship Admiral Aura" to "perk",
      ),
      description = "FHS Apotheosis orbital hero powers, squad shield bubbles, and crossover gateway rigs.",
    ),
    GameCollectionWorkshopPreset(
      key = "generic",
      appId = "480",
      label = "Generic / Empire City (480)",
      gameTitle = "inFAMOUS Empire City & Universal Workshop",
      universeTag = "TUU Infamous",
      linkedWorldId = "empire_city_infamous",
      linkedAreaId = "area_empire_city",
      archetype = "operator",
      accentHex = "#66C0F4",
      sampleWorkshopId = "48000001",
      modNames = listOf(
        "Ray-Sphere Arc Storm" to "power",
        "Blast Shard Conduit Core" to "perk",
      ),
      description = "Universal SteamCMD Workshop App 480 pack pre-tuned for Empire City Conduit lightning & crossover loadouts.",
    ),
  )

  fun findCollectionPreset(appIdOrKeyOrMeta: String?): GameCollectionWorkshopPreset? {
    val token = appIdOrKeyOrMeta?.trim()?.lowercase().orEmpty()
    if (token.isEmpty()) return null
    return collectionGamePresets.firstOrNull {
      it.key == token || it.appId == token
    } ?: collectionGamePresets.firstOrNull { preset ->
      when (preset.key) {
        "bo3" -> token.contains("bo3") || token.contains("black ops") || token.contains("zombies") || token.contains("treyarch")
        "prototype" -> token.contains("prototype") || token.contains("mercer") || token.contains("blacklight")
        "arkham" -> token.contains("batman") || token.contains("arkham") || token.contains("gotham") || token.contains("blackgate") || token.contains("black gate")
        "gta4" -> token.contains("gta") || token.contains("liberty city") || token.contains("hove beach") || token.contains("alderney") || token.contains("niko")
        "superman" -> token.contains("superman") || token.contains("metropolis") || token.contains("warworld") || token.contains("lexcorp") || token.contains("daily planet")
        "abyss_dungeons" -> token.contains("abyss") || token.contains("dungeon") || token.contains("crypt") || token.contains("lich")
        "spiderman" -> token.contains("spider") || token.contains("symbiote") || token.contains("oscorp")
        "godofwar" -> token.contains("god of war") || token.contains("kratos") || token.contains("crucible")
        "division" -> token.contains("division") || token.contains("shd") || token.contains("isac") || token.contains("sector zero")
        "force_unleashed" -> token.contains("starkiller") || token.contains("force unleashed") || token.contains("kyber") || token.contains("star wars")
        "ironman" -> token.contains("iron man") || token.contains("stark") || token.contains("hulkbuster") || token.contains("arc reactor")
        "ship_of_heroes" -> token.contains("ship of heroes") || token.contains("apotheosis") || token.contains("crossover")
        else -> token.contains("infamous") || token.contains("conduit") || token.contains("empire city")
      }
    }
  }

  fun getGameCollectionWorkshopPack(
    appIdOrKey: String,
    targetUserId: String,
  ): SteamWorkshopImportPreview {
    if (appIdOrKey.equals("bo3", ignoreCase = true) || appIdOrKey == "311210") {
      return Bo3ModToolsImporter.getCuratedBo3ModPack(targetUserId)
    }
    val preset = findCollectionPreset(appIdOrKey) ?: collectionGamePresets.last()
    val now = System.currentTimeMillis()
    val items = preset.modNames.mapIndexed { idx, (modName, cat) ->
      ArmoryItem(
        id = FirestoreSyncCodec.sanitizeId("ws_${preset.key}_$idx", "ws_col"),
        userId = targetUserId,
        name = modName,
        category = cat,
        universeTag = preset.universeTag,
        description = "${preset.gameTitle} curated Workshop module (Tier 1-25 scaling).",
        tier = 1L,
        maxTier = 25L,
        cooldownSec = if (cat == "perk") 0L else 6L,
        powerValue = 260L + idx * 70L,
        accentColorHex = preset.accentHex,
        equipped = true,
        isCustom = true,
        createdAtMillis = now + idx,
        updatedAtMillis = now + idx,
      )
    }
    val rig = RigAsset(
      id = FirestoreSyncCodec.sanitizeId("ws_${preset.key}_rig", "ws_col_rig"),
      userId = targetUserId,
      name = "${preset.gameTitle.substringBefore(':')} Workshop Rig",
      archetype = preset.archetype,
      activeClip = "idle",
      ikEnabled = true,
      rigScale = 1.0,
      playbackSpeed = 1.0,
      poseAngles = UniverseCatalog.defaultPoseMap(),
      keyframeFrames = listOf(0L, 15L, 30L, 45L, 60L),
      isCompleted = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    )
    return SteamWorkshopImportPreview(
      sourceKind = "COLLECTION_WORKSHOP_PACK",
      title = "${preset.gameTitle} Workshop Pack",
      description = preset.description,
      publishedFileId = preset.sampleWorkshopId,
      appId = preset.appId,
      creatorSteamId = "UnknownOS_Collection",
      tags = listOf(preset.universeTag, "Workshop", "Tier25"),
      subscriptions = 4200L,
      views = 15800L,
      armoryItems = items,
      customRigs = listOf(rig),
      blenderRigScript = null,
      generatedModule = null,
      generatedRig = null,
      sourceNote = "${items.size} ${preset.universeTag} modules, 1 ForgeRig & linked map ready to deploy.",
      linkedWorldId = preset.linkedWorldId,
      linkedAreaId = preset.linkedAreaId,
    )
  }

  val collectionCustomMapsCatalog: List<UserCustomGameMapSpec>
    get() = CuratedCustomMapsCatalog.allCuratedCustomMaps

  fun resolveCollectionCustomMapId(rawMeta: String): String? {
    val lower = rawMeta.lowercase()
    return when {
      lower.contains("red-zone") || lower.contains("redzone") || lower.contains("prot_redzone") -> "area_col_custom_blacklight_rooftops"
      lower.contains("stasis pier") || lower.contains("inf_stasis_pier") -> "area_col_custom_infamous_pier"
      lower.contains("iceberg lounge") || lower.contains("arkham_iceberg_lounge") -> "area_col_custom_arkham_lounge"
      lower.contains("pioneers bridge") || lower.contains("royal hotel") || lower.contains("arkham_origins_bridge") -> "area_col_custom_arkham_origins_bridge"
      lower.contains("origins blackgate") || lower.contains("3-wing") || lower.contains("arkham_origins_blackgate") -> "area_col_custom_arkham_blackgate"
      lower.contains("wonder tower") || lower.contains("asylum") || lower.contains("arkham_asylum_city") -> "area_col_custom_arkham_asylum_city"
      lower.contains("gta4_alderney") || lower.contains("alderney") || lower.contains("happiness island") -> "area_col_custom_gta4_alderney"
      lower.contains("gta4") || lower.contains("liberty city") || lower.contains("hove beach") || lower.contains("star junction") -> "area_col_custom_gta4_liberty"
      lower.contains("warworld") || lower.contains("fortress") || lower.contains("superman_warworld") -> "area_col_custom_superman_warworld"
      lower.contains("superman") || lower.contains("metropolis") || lower.contains("daily planet") -> "area_col_custom_superman_metropolis"
      lower.contains("lich sanctum") || lower.contains("inferno blood altar") || lower.contains("abyss_lich") -> "area_col_custom_abyss_lich_sanctum"
      lower.contains("abyss") || lower.contains("howling crypt") || lower.contains("dungeons") -> "area_col_custom_abyss_crypt"
      lower.contains("oscorp") && lower.contains("helipad") || lower.contains("spidey_oscorp") -> "area_col_custom_spider_helipad"
      lower.contains("tartarus") || lower.contains("gow_tartarus") -> "area_col_custom_gow_tartarus"
      lower.contains("dz-09 mall") || lower.contains("dz09_mall") -> "area_col_custom_division_mall"
      else -> Bo3ModToolsImporter.resolveBo3AreaId(lower)
    }
  }

  fun getCollectionCustomMapPack(
    mapId: String,
    targetUserId: String,
  ): SteamWorkshopImportPreview {
    val spec = CuratedCustomMapsCatalog.findById(mapId)
      ?: CuratedCustomMapsCatalog.curatedBo3CustomMaps.first()
    val now = System.currentTimeMillis()
    val isBo3 = spec.gameCategory == CustomMapGameCategory.BO3
    val matchedPreset = findCollectionPreset(spec.gameCategory.id)
    val modItem = ArmoryItem(
      id = FirestoreSyncCodec.sanitizeId("ws_map_mod_${spec.mapCode}", "ws_map"),
      userId = targetUserId,
      name = "${spec.title.substringAfter(": ").trim()} Core",
      category = "power",
      universeTag = if (isBo3) "BO3 Mod Tools" else (matchedPreset?.universeTag ?: spec.gameCategory.label),
      description = "Workshop custom map addon for ${spec.title} (${spec.wonderWeapon}).",
      tier = 1L,
      maxTier = 25L,
      cooldownSec = 5L,
      powerValue = 320L,
      accentColorHex = spec.gameCategory.accentColorHex,
      equipped = true,
      isCustom = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    )
    return SteamWorkshopImportPreview(
      sourceKind = if (isBo3) "BO3_CUSTOM_WORKSHOP_MAP" else "COLLECTION_WORKSHOP_CUSTOM_MAP",
      title = spec.title,
      description = spec.description,
      publishedFileId = matchedPreset?.sampleWorkshopId ?: "311210999",
      appId = matchedPreset?.appId ?: "311210",
      creatorSteamId = "SteamWorkshop_Curated",
      tags = listOf(spec.gameCategory.label, "CustomMap", spec.mapCode),
      subscriptions = 9400L,
      views = 32000L,
      armoryItems = listOf(modItem),
      customRigs = emptyList(),
      blenderRigScript = null,
      generatedModule = null,
      generatedRig = null,
      sourceNote = "Workshop Custom Map '${spec.title}' (${spec.buildingNames.size} zones, WW: ${spec.wonderWeapon}) ready to play!",
      linkedWorldId = spec.gameCategory.linkedWorldId,
      linkedAreaId = spec.id,
      customMap = spec.copy(isUserCreated = true, createdAtMillis = now),
    )
  }

  fun sanitizeNumericId(raw: String, fallback: String): String {
    val digits = raw.trim().filter(Char::isDigit).take(20)
    return digits.ifEmpty { fallback }
  }

  /**
   * Extracts a Steam Workshop PublishedFileId from a raw ID, steamcommunity.com URL,
   * or steam://url/CommunityFilePage/<id> link.
   */
  fun extractPublishedFileId(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    if (trimmed.all(Char::isDigit) && trimmed.length in 3..20) {
      return trimmed
    }

    val queryIdMatch = Regex("""(?:[?&]id=)(\d{3,20})""").find(trimmed)
    if (queryIdMatch != null) {
      return queryIdMatch.groupValues[1]
    }

    val pathIdMatch = Regex("""CommunityFilePage/(\d{3,20})""", RegexOption.IGNORE_CASE).find(trimmed)
    if (pathIdMatch != null) {
      return pathIdMatch.groupValues[1]
    }

    return null
  }

  fun workshopItemWebUrl(publishedFileId: String): String =
    "https://steamcommunity.com/sharedfiles/filedetails/?id=${sanitizeNumericId(publishedFileId, "0")}"

  fun buildWorkshopPortalUrl(appId: String, steamId64: String?): String {
    val cleanSteamId = steamId64?.trim()?.filter(Char::isDigit).orEmpty()
    if (cleanSteamId.length >= 15) {
      return "https://steamcommunity.com/profiles/$cleanSteamId/myworkshopfiles/"
    }
    val cleanAppId = sanitizeNumericId(appId, "480")
    return "https://steamcommunity.com/app/$cleanAppId/workshop/"
  }

  /**
   * Generates a real Valve KeyValues (.vdf) build manifest compatible with
   * `steamcmd +workshop_build_item workshop_item.vdf`.
   */
  fun generateWorkshopVdf(config: SteamWorkshopExportConfig): String {
    val safeAppId = sanitizeNumericId(config.appId, "480")
    val safePubId = sanitizeNumericId(config.publishedFileId, "0")
    val safeTitle = escapeVdfString(config.title.ifBlank { "The Unknown Universes Pack" }.take(128))
    val safeDesc = escapeVdfString(config.description.ifBlank { "Exported from UnknownOS" }.take(4000))
    val safeChange = escapeVdfString(config.changeNote.ifBlank { "Updated from UnknownOS" }.take(500))

    val sb = StringBuilder()
    sb.appendLine("\"workshopitem\"")
    sb.appendLine("{")
    sb.appendLine("\t\"appid\"\t\t\"$safeAppId\"")
    sb.appendLine("\t\"publishedfileid\"\t\"$safePubId\"")
    sb.appendLine("\t\"contentfolder\"\t\"./workshop_content\"")
    sb.appendLine("\t\"previewfile\"\t\"./workshop_preview.png\"")
    sb.appendLine("\t\"visibility\"\t\"${config.visibility.code}\"")
    sb.appendLine("\t\"title\"\t\t\"$safeTitle\"")
    sb.appendLine("\t\"description\"\t\"$safeDesc\"")
    sb.appendLine("\t\"changenote\"\t\"$safeChange\"")
    val cleanTags = config.tags.map { escapeVdfString(it.take(32)) }.filter { it.isNotBlank() }.distinct()
    if (cleanTags.isNotEmpty()) {
      sb.appendLine("\t\"tags\"")
      sb.appendLine("\t{")
      cleanTags.forEachIndexed { idx, tag ->
        sb.appendLine("\t\t\"$idx\"\t\t\"$tag\"")
      }
      sb.appendLine("\t}")
    }
    sb.append("}")
    return sb.toString()
  }

  /**
   * Generates a structured Steam Workshop JSON package (`unknown_universes_workshop_v1`) containing
   * Steam Workshop metadata alongside Rigs, Armory Modules, and optional Blender (.py) script.
   */
  fun generateWorkshopPackageJson(
    config: SteamWorkshopExportConfig,
    profile: OperatorProfile,
    armory: List<ArmoryItem>,
    rigs: List<RigAsset>,
    nowMillis: Long = System.currentTimeMillis(),
  ): String {
    val safeAppId = sanitizeNumericId(config.appId, "480")
    val safePubId = sanitizeNumericId(config.publishedFileId, "0")
    val activeRig = rigs.firstOrNull { it.id == profile.activeRigId } ?: rigs.firstOrNull()

    val exportArmory = if (config.includeArmory) {
      val customOrEquipped = armory.filter { it.isCustom || it.equipped }
      customOrEquipped.ifEmpty { armory }
    } else {
      emptyList()
    }
    val exportRigs = if (config.includeRigs) rigs else emptyList()

    val root = JSONObject()
    root.put("format", WORKSHOP_FORMAT_MARKER)
    root.put("schemaVersion", SCHEMA_VERSION)
    root.put("packageName", BuildConfig.APPLICATION_ID)
    root.put("sourceAppVersion", BuildConfig.VERSION_NAME)
    root.put("exportedAtMillis", nowMillis)

    val wsObj = JSONObject().apply {
      put("appId", safeAppId)
      put("publishedFileId", safePubId)
      put("title", config.title.ifBlank { "${profile.callsign} UnknownOS Pack" }.take(128))
      put("description", config.description.take(4000))
      put("changeNote", config.changeNote.take(500))
      put("visibility", config.visibility.code)
      put("authorCallsign", profile.callsign)
      put("authorSteamId", config.authorSteamId.trim().take(32))
      put("authorPersona", config.authorPersona.trim().take(64))
      if (safePubId != "0") {
        put("workshopUrl", workshopItemWebUrl(safePubId))
      }
      put("tags", JSONArray(config.tags.map { it.trim().take(32) }.filter { it.isNotBlank() }.distinct()))
    }
    root.put("workshop", wsObj)

    val armoryArr = JSONArray()
    exportArmory.forEach { item ->
      armoryArr.put(
        JSONObject().apply {
          put("id", item.id)
          put("name", item.name)
          put("category", item.category)
          put("universeTag", item.universeTag)
          put("description", item.description)
          put("tier", item.tier)
          put("maxTier", item.maxTier)
          put("cooldownSec", item.cooldownSec)
          put("powerValue", item.powerValue)
          put("accentColorHex", item.accentColorHex)
          put("equipped", item.equipped)
          put("isCustom", true)
          put("createdAtMillis", item.createdAtMillis)
          put("updatedAtMillis", item.updatedAtMillis)
        },
      )
    }
    root.put("armoryItems", armoryArr)

    val rigsArr = JSONArray()
    exportRigs.forEach { rig ->
      rigsArr.put(
        JSONObject().apply {
          put("id", rig.id)
          put("name", rig.name)
          put("archetype", rig.archetype)
          put("activeClip", rig.activeClip)
          put("ikEnabled", rig.ikEnabled)
          put("rigScale", rig.rigScale)
          put("playbackSpeed", rig.playbackSpeed)
          put("poseAngles", JSONObject(rig.poseAngles))
          put("keyframeFrames", JSONArray(rig.keyframeFrames))
          put("isCompleted", rig.isCompleted)
          put("createdAtMillis", rig.createdAtMillis)
          put("updatedAtMillis", rig.updatedAtMillis)
        },
      )
    }
    root.put("customRigs", rigsArr)

    if (config.includeBlenderScript && activeRig != null) {
      root.put("blenderRigScript", PortablePackageGenerator.generateBlenderRigScript(activeRig))
    }

    return root.toString(2)
  }

  /**
   * Parses JSON returned by `ISteamRemoteStorage/GetPublishedFileDetails/v1/`.
   */
  fun parseWorkshopDetailsApiJson(jsonText: String): Result<SteamWorkshopPublishedDetails> {
    return try {
      val root = JSONObject(jsonText)
      val response = root.optJSONObject("response")
        ?: return Result.failure(IllegalArgumentException("Missing 'response' field in Steam API reply."))
      val detailsArr = response.optJSONArray("publishedfiledetails")
        ?: return Result.failure(IllegalArgumentException("Missing 'publishedfiledetails' array."))
      val item = detailsArr.optJSONObject(0)
        ?: return Result.failure(IllegalArgumentException("Steam Workshop item was not found."))

      val resultCode = item.optInt("result", 0)
      if (resultCode != 1) {
        return Result.failure(
          IllegalStateException("Steam Workshop item unavailable or private (Steam result=$resultCode)."),
        )
      }

      val pubId = item.optString("publishedfileid").trim()
      if (pubId.isEmpty()) {
        return Result.failure(IllegalArgumentException("PublishedFileId missing in Steam API response."))
      }

      val tagsList = mutableListOf<String>()
      val tagsArr = item.optJSONArray("tags")
      if (tagsArr != null) {
        for (i in 0 until tagsArr.length()) {
          val tagVal = tagsArr.optJSONObject(i)?.optString("tag")?.trim().orEmpty()
          if (tagVal.isNotEmpty()) tagsList.add(tagVal)
        }
      }

      val rawDesc = item.optString("description", "")
      val cleanDesc = stripSteamBbCode(rawDesc).take(4000)

      Result.success(
        SteamWorkshopPublishedDetails(
          publishedFileId = pubId,
          title = item.optString("title").trim().ifEmpty { "Workshop Item #$pubId" }.take(128),
          description = cleanDesc,
          creatorSteamId = item.optString("creator").trim(),
          consumerAppId = item.optLong("consumer_app_id", 0L).takeIf { it > 0L }?.toString()
            ?: item.optLong("creator_app_id", 0L).takeIf { it > 0L }?.toString().orEmpty(),
          fileName = item.optString("filename").trim(),
          fileSize = item.optLong("file_size", 0L),
          fileUrl = item.optString("file_url").trim().takeIf { it.startsWith("http") },
          previewUrl = item.optString("preview_url").trim().takeIf { it.startsWith("http") },
          tags = tagsList.distinct(),
          subscriptions = item.optLong("subscriptions", 0L),
          favorited = item.optLong("favorited", 0L),
          views = item.optLong("views", 0L),
          timeUpdatedEpochSec = item.optLong("time_updated", 0L),
          visibility = item.optInt("visibility", 0),
        ),
      )
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("Invalid Steam Workshop API JSON: ${e.localizedMessage}"))
    }
  }

  /**
   * Parses Valve KeyValues `.vdf` / `.acf` Workshop manifest files.
   */
  fun parseVdfManifest(vdfText: String): Result<SteamWorkshopPublishedDetails> {
    return try {
      val trimmed = vdfText.trim()
      if (trimmed.isEmpty()) {
        return Result.failure(IllegalArgumentException("Empty VDF manifest."))
      }
      val pairs = linkedMapOf<String, String>()
      val numericTagValues = mutableListOf<String>()
      val pairRegex = Regex("\"((?:\\\\.|[^\"\\\\])*)\"\\s+\"((?:\\\\.|[^\"\\\\])*)\"")

      trimmed.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.startsWith("//")) return@forEach
        val match = pairRegex.find(line) ?: return@forEach
        val k = unescapeVdfString(match.groupValues[1]).lowercase()
        val v = unescapeVdfString(match.groupValues[2])
        if (k.all(Char::isDigit) && v.isNotBlank()) {
          numericTagValues.add(v)
        } else if (!pairs.containsKey(k)) {
          pairs[k] = v
        }
      }

      val hasWorkshopKeys = pairs.containsKey("appid") ||
        pairs.containsKey("publishedfileid") ||
        pairs.containsKey("title") ||
        trimmed.contains("\"workshopitem\"", ignoreCase = true)
      if (!hasWorkshopKeys) {
        return Result.failure(IllegalArgumentException("File does not look like a Steam Workshop VDF manifest."))
      }

      val pubId = pairs["publishedfileid"]?.filter(Char::isDigit).orEmpty().ifEmpty { "0" }
      val appId = pairs["appid"]?.filter(Char::isDigit).orEmpty()
      val title = pairs["title"].orEmpty().ifBlank {
        if (pubId != "0") "Workshop Item #$pubId" else "Steam Workshop VDF Pack"
      }
      val description = pairs["description"].orEmpty().ifBlank { pairs["changenote"].orEmpty() }
      val tagStringList = pairs["tags"].orEmpty()
        .split(',', ';')
        .map { it.trim() }
        .filter { it.isNotBlank() }

      Result.success(
        SteamWorkshopPublishedDetails(
          publishedFileId = pubId,
          title = title.take(128),
          description = stripSteamBbCode(description).take(4000),
          creatorSteamId = pairs["creator"].orEmpty(),
          consumerAppId = appId,
          fileName = pairs["contentfolder"].orEmpty(),
          tags = (numericTagValues + tagStringList).distinct(),
          visibility = pairs["visibility"]?.toIntOrNull() ?: 0,
        ),
      )
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("Failed to parse VDF manifest: ${e.localizedMessage}"))
    }
  }

  /**
   * Parses any local or remote Workshop import payload:
   * - `unknown_universes_workshop_v1` JSON
   * - `unknown_universes_save_v1` / portable collection JSON
   * - Steam Web API `GetPublishedFileDetails` JSON
   * - Valve KeyValues `.vdf` manifest
   */
  fun parseWorkshopImportInput(
    rawText: String,
    targetUserId: String,
    fallbackPublishedDetails: SteamWorkshopPublishedDetails? = null,
  ): Result<SteamWorkshopImportPreview> {
    val trimmed = rawText.trim()
    if (trimmed.isEmpty()) {
      return Result.failure(IllegalArgumentException("Workshop import input is empty."))
    }

    if (trimmed.startsWith("{")) {
      return try {
        val root = JSONObject(trimmed)
        when {
          root.optString("format") == WORKSHOP_FORMAT_MARKER || root.has("workshop") -> {
            parseUnknownUniversesWorkshopJson(root, targetUserId)
          }
          root.optJSONObject("response")?.has("publishedfiledetails") == true -> {
            val details = parseWorkshopDetailsApiJson(trimmed).getOrThrow()
            Result.success(buildPreviewFromSteamDetails(details, targetUserId, "STEAM_API_JSON"))
          }
          else -> {
            val savePreview = SaveDataSerializer.parseAndValidateSaveJson(trimmed, targetUserId).getOrThrow()
            val bundle = savePreview.bundle
            val title = fallbackPublishedDetails?.title ?: "${savePreview.callsign} Workshop Collection"
            Result.success(
              SteamWorkshopImportPreview(
                sourceKind = if (fallbackPublishedDetails != null) "LIVE_STEAM_WORKSHOP_BUNDLE" else "WORKSHOP_SAVE_JSON",
                title = title,
                description = fallbackPublishedDetails?.description
                  ?.ifBlank { "Imported from Steam Workshop portable collection JSON" }
                  ?: "Portable save bundle (${bundle.armoryItems.size} modules, ${bundle.customRigs.size} rigs)",
                publishedFileId = fallbackPublishedDetails?.publishedFileId,
                appId = fallbackPublishedDetails?.consumerAppId,
                creatorSteamId = fallbackPublishedDetails?.creatorSteamId,
                tags = fallbackPublishedDetails?.tags ?: listOf("UnknownOS", "Workshop"),
                subscriptions = fallbackPublishedDetails?.subscriptions ?: 0L,
                views = fallbackPublishedDetails?.views ?: 0L,
                armoryItems = retargetWorkshopArmory(bundle.armoryItems, targetUserId, fallbackPublishedDetails?.publishedFileId),
                customRigs = retargetWorkshopRigs(bundle.customRigs, targetUserId, fallbackPublishedDetails?.publishedFileId),
                blenderRigScript = null,
                generatedModule = null,
                generatedRig = null,
                sourceNote = "Parsed ${bundle.armoryItems.size} Armory modules and ${bundle.customRigs.size} Rigs from Workshop JSON.",
              ),
            )
          }
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

    val vdfResult = parseVdfManifest(trimmed)
    if (vdfResult.isSuccess) {
      return vdfResult.map { details ->
        buildPreviewFromSteamDetails(details, targetUserId, "VDF_MANIFEST")
      }
    }

    val bo3Result = Bo3ModToolsImporter.parseBo3PackageOrScript(trimmed, targetUserId)
    if (bo3Result.isSuccess) {
      return bo3Result
    }

    return vdfResult.map { details ->
      buildPreviewFromSteamDetails(details, targetUserId, "VDF_MANIFEST")
    }
  }

  /**
   * Fetches live item metadata from Steam Workshop's public Web API (`GetPublishedFileDetails/v1/`)
   * and, when a legacy direct `file_url` points to JSON/VDF content, downloads & parses it as well.
   */
  suspend fun fetchSteamWorkshopItem(
    inputOrUrl: String,
    targetUserId: String,
  ): Result<SteamWorkshopImportPreview> = withContext(Dispatchers.IO) {
    val publishedId = extractPublishedFileId(inputOrUrl)
      ?: return@withContext Result.failure(
        IllegalArgumentException("Enter a valid Steam Workshop URL or numeric item ID (e.g. 2849102910)."),
      )

    try {
      val apiBody = postForm(
        endpoint = STEAM_PUBLISHED_FILE_API,
        params = mapOf(
          "itemcount" to "1",
          "publishedfileids[0]" to publishedId,
        ),
      )
      val details = parseWorkshopDetailsApiJson(apiBody).getOrElse { err ->
        return@withContext Result.failure(err)
      }

      val fileUrl = details.fileUrl
      if (!fileUrl.isNullOrBlank()) {
        val remoteText = fetchSmallTextPayload(fileUrl)
        if (remoteText != null) {
          val parsedRemote = parseWorkshopImportInput(remoteText, targetUserId, details)
          if (parsedRemote.isSuccess) {
            return@withContext parsedRemote
          }
        }
      }

      Result.success(buildPreviewFromSteamDetails(details, targetUserId, "LIVE_STEAM_WORKSHOP"))
    } catch (e: Exception) {
      Result.failure(
        IllegalStateException("Could not reach Steam Workshop API: ${e.localizedMessage}"),
      )
    }
  }

  private fun parseUnknownUniversesWorkshopJson(
    root: JSONObject,
    targetUserId: String,
  ): Result<SteamWorkshopImportPreview> {
    val ws = root.optJSONObject("workshop") ?: JSONObject()
    val now = System.currentTimeMillis()
    val pubId = ws.optString("publishedFileId").trim().ifEmpty { null }
    val appId = ws.optString("appId").trim().ifEmpty { null }
    val title = ws.optString("title").trim().ifEmpty { "UnknownOS Workshop Pack" }.take(128)
    val description = ws.optString("description").trim().take(4000)
    val creatorSteamId = ws.optString("authorSteamId").trim().ifEmpty { null }
    val tags = mutableListOf<String>()
    val tagsArr = ws.optJSONArray("tags")
    if (tagsArr != null) {
      for (i in 0 until tagsArr.length()) {
        val t = tagsArr.optString(i).trim()
        if (t.isNotEmpty()) tags.add(t)
      }
    }

    val saveCompatRes = SaveDataSerializer.parseAndValidateSaveJson(root.toString(), targetUserId)
    val armory = saveCompatRes.getOrNull()?.bundle?.armoryItems.orEmpty()
    val rigs = saveCompatRes.getOrNull()?.bundle?.customRigs.orEmpty()

    val retargetedArmory = retargetWorkshopArmory(armory, targetUserId, pubId, now)
    val retargetedRigs = retargetWorkshopRigs(rigs, targetUserId, pubId, now)
    val blenderScript = root.optString("blenderRigScript").trim().takeIf { it.isNotEmpty() }

    if (retargetedArmory.isEmpty() && retargetedRigs.isEmpty()) {
      val details = SteamWorkshopPublishedDetails(
        publishedFileId = pubId ?: "0",
        title = title,
        description = description,
        creatorSteamId = creatorSteamId.orEmpty(),
        consumerAppId = appId.orEmpty(),
        tags = tags,
      )
      return Result.success(buildPreviewFromSteamDetails(details, targetUserId, "WORKSHOP_JSON_MANIFEST"))
    }

    return Result.success(
      SteamWorkshopImportPreview(
        sourceKind = "UNKNOWN_UNIVERSES_WORKSHOP_JSON",
        title = title,
        description = description,
        publishedFileId = pubId,
        appId = appId,
        creatorSteamId = creatorSteamId,
        tags = tags,
        armoryItems = retargetedArmory,
        customRigs = retargetedRigs,
        blenderRigScript = blenderScript,
        generatedModule = null,
        generatedRig = null,
        sourceNote = "Ready to import ${retargetedArmory.size} Armory module(s) and ${retargetedRigs.size} custom Rig(s).",
      ),
    )
  }

  internal fun buildPreviewFromSteamDetails(
    details: SteamWorkshopPublishedDetails,
    targetUserId: String,
    sourceKind: String,
  ): SteamWorkshopImportPreview {
    val now = System.currentTimeMillis()
    val idToken = details.publishedFileId.filter(Char::isDigit).ifEmpty {
      (details.title.hashCode().toUInt()).toString()
    }.take(16)
    val tagSummary = details.tags.joinToString(", ").take(36).ifBlank {
      if (details.consumerAppId.isNotBlank()) "Steam App ${details.consumerAppId}" else "Steam Workshop"
    }
    val lowerMeta = "${details.title} ${details.description} ${details.tags.joinToString(" ")}".lowercase()
    val matchedPreset = findCollectionPreset(details.consumerAppId.takeIf { it != "480" })
      ?: findCollectionPreset(lowerMeta)
    val archetype = when {
      lowerMeta.contains("star") || lowerMeta.contains("saber") || lowerMeta.contains("jedi") -> "starblade"
      lowerMeta.contains("bio") || lowerMeta.contains("zombie") || lowerMeta.contains("infected") -> "biomorph"
      lowerMeta.contains("fire") || lowerMeta.contains("hell") || lowerMeta.contains("infernal") -> "infernal"
      lowerMeta.contains("time") || lowerMeta.contains("chrono") || lowerMeta.contains("cyber") -> "chrono"
      matchedPreset != null -> matchedPreset.archetype
      else -> "operator"
    }
    val category = if (lowerMeta.contains("perk") || lowerMeta.contains("passive") || lowerMeta.contains("armor")) "perk" else "power"
    val cleanDesc = details.description
      .ifBlank { "Imported from Steam Workshop #${details.publishedFileId} ($tagSummary)" }
      .take(240)
    val resolvedCustomMapId = resolveCollectionCustomMapId(lowerMeta)
    val explicitBo3Map = Bo3ModToolsImporter.resolveBo3AreaId(lowerMeta)
    val isCustomMapItem = lowerMeta.contains("map") || details.tags.any { it.equals("Map", ignoreCase = true) || it.equals("CustomMap", ignoreCase = true) || it.equals("Zombies", ignoreCase = true) }
    val synthesizedCustomMap = if (resolvedCustomMapId == null && isCustomMapItem && (matchedPreset != null || lowerMeta.contains("zm_"))) {
      val cat = when (matchedPreset?.key) {
        "prototype" -> CustomMapGameCategory.PROTOTYPE
        "arkham" -> CustomMapGameCategory.ARKHAM
        "gta4" -> CustomMapGameCategory.GTA4
        "superman" -> CustomMapGameCategory.SUPERMAN
        "abyss_dungeons" -> CustomMapGameCategory.ABYSS_DUNGEONS
        "spiderman" -> CustomMapGameCategory.SPIDERMAN
        "godofwar" -> CustomMapGameCategory.GODOFWAR
        "division" -> CustomMapGameCategory.DIVISION
        "bo3" -> CustomMapGameCategory.BO3
        "generic" -> CustomMapGameCategory.INFAMOUS
        else -> if (lowerMeta.contains("zm_") || lowerMeta.contains("bo3")) CustomMapGameCategory.BO3 else CustomMapGameCategory.INFAMOUS
      }
      val mapCodeMatch = Regex("""zm_[a-z0-9_]{2,28}""", RegexOption.IGNORE_CASE).find(lowerMeta)?.value
        ?: "${cat.id}_ws_${idToken.take(10)}"
      val areaPrefix = if (cat == CustomMapGameCategory.BO3) "area_bo3_custom_" else "area_col_custom_"
      val mapId = FirestoreSyncCodec.sanitizeId("${areaPrefix}${mapCodeMatch.removePrefix("zm_")}", areaPrefix.trimEnd('_'))
      UserCustomGameMapSpec(
        id = mapId,
        mapCode = mapCodeMatch,
        title = details.title.trim().take(48).ifEmpty { "Workshop Custom Map" },
        gameCategory = cat,
        description = cleanDesc,
        buildingNames = cat.defaultZones,
        wonderWeapon = cat.defaultWonderWeapon,
        accentHex = cat.defaultAccentHex,
        isUserCreated = true,
        createdAtMillis = now,
      )
    } else null

    val resolvedAreaId = synthesizedCustomMap?.id ?: resolvedCustomMapId ?: explicitBo3Map ?: matchedPreset?.linkedAreaId
    val resolvedWorldId = when {
      synthesizedCustomMap != null -> synthesizedCustomMap.gameCategory.linkedWorldId
      resolvedAreaId?.startsWith("area_bo3_") == true -> "sector_zero"
      resolvedAreaId != null -> CuratedCustomMapsCatalog.findById(resolvedAreaId)?.gameCategory?.linkedWorldId ?: matchedPreset?.linkedWorldId
      else -> matchedPreset?.linkedWorldId
    }

    val addonModule = ArmoryItem(
      id = FirestoreSyncCodec.sanitizeId("ws_mod_$idToken", "ws_mod"),
      userId = targetUserId,
      name = details.title.trim().take(50).ifEmpty { "Workshop Module" },
      category = category,
      universeTag = matchedPreset?.universeTag ?: "Workshop #$idToken".take(40),
      description = cleanDesc,
      tier = 1L,
      maxTier = 25L,
      cooldownSec = if (category == "perk") 0L else 6L,
      powerValue = if (matchedPreset != null) 220L else 175L,
      accentColorHex = matchedPreset?.accentHex ?: "#66C0F4",
      equipped = true,
      isCustom = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    )

    val addonRig = RigAsset(
      id = FirestoreSyncCodec.sanitizeId("ws_rig_$idToken", "ws_rig"),
      userId = targetUserId,
      name = "${details.title.trim().take(38)} Rig".trim(),
      archetype = archetype,
      activeClip = "inspect",
      ikEnabled = true,
      rigScale = 1.0,
      playbackSpeed = 1.0,
      poseAngles = UniverseCatalog.defaultPoseMap(),
      keyframeFrames = listOf(0L, 15L, 30L, 45L, 60L),
      isCompleted = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    )

    return SteamWorkshopImportPreview(
      sourceKind = sourceKind,
      title = details.title,
      description = details.description,
      publishedFileId = details.publishedFileId.takeIf { it.isNotBlank() && it != "0" },
      appId = details.consumerAppId.takeIf { it.isNotBlank() } ?: matchedPreset?.appId,
      creatorSteamId = details.creatorSteamId.takeIf { it.isNotBlank() },
      tags = details.tags,
      subscriptions = details.subscriptions,
      views = details.views,
      armoryItems = emptyList(),
      customRigs = emptyList(),
      blenderRigScript = null,
      generatedModule = addonModule,
      generatedRig = addonRig,
      sourceNote = buildString {
        append("Workshop metadata resolved. Converts into an equipped Armory Module & ForgeRig preset.")
        if (matchedPreset != null) {
          append(" Matched collection: ${matchedPreset.gameTitle}.")
        }
        if (synthesizedCustomMap != null) {
          append(" Created custom playable map: ${synthesizedCustomMap.title}.")
        }
      },
      linkedWorldId = resolvedWorldId,
      linkedAreaId = resolvedAreaId,
      customMap = synthesizedCustomMap,
    )
  }

  private fun retargetWorkshopArmory(
    items: List<ArmoryItem>,
    targetUserId: String,
    pubId: String?,
    now: Long = System.currentTimeMillis(),
  ): List<ArmoryItem> {
    val prefix = pubId?.filter(Char::isDigit)?.take(8)?.let { "ws_${it}_" } ?: "ws_"
    return items.map { item ->
      val newId = if (item.id.startsWith("ws_")) item.id else "$prefix${item.id}"
      item.copy(
        id = FirestoreSyncCodec.sanitizeId(newId, "ws_arm"),
        userId = targetUserId,
        isCustom = true,
        maxTier = maxOf(item.maxTier, 25L),
        updatedAtMillis = now,
      )
    }
  }

  private fun retargetWorkshopRigs(
    rigs: List<RigAsset>,
    targetUserId: String,
    pubId: String?,
    now: Long = System.currentTimeMillis(),
  ): List<RigAsset> {
    val prefix = pubId?.filter(Char::isDigit)?.take(8)?.let { "ws_${it}_" } ?: "ws_"
    return rigs.map { rig ->
      val newId = if (rig.id.startsWith("ws_")) rig.id else "$prefix${rig.id}"
      rig.copy(
        id = FirestoreSyncCodec.sanitizeId(newId, "ws_rig"),
        userId = targetUserId,
        updatedAtMillis = now,
      )
    }
  }

  private fun escapeVdfString(raw: String): String =
    raw.replace("\\", "\\\\").replace("\"", "'").replace("\r", "").replace("\n", "\\n")

  private fun unescapeVdfString(raw: String): String =
    raw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")

  private fun stripSteamBbCode(raw: String): String =
    raw
      .replace(Regex("""\[/?[a-zA-Z0-9*=._:/\-]+]"""), "")
      .replace(Regex("""\n{3,}"""), "\n\n")
      .trim()

  private fun postForm(endpoint: String, params: Map<String, String>): String {
    val encoded = params.entries.joinToString("&") { (k, v) ->
      "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
    }
    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      doOutput = true
      connectTimeout = 10_000
      readTimeout = 10_000
      setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
      setRequestProperty("Accept", "application/json")
    }
    conn.outputStream.use { it.write(encoded.toByteArray(Charsets.UTF_8)) }
    val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
    val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    if (conn.responseCode !in 200..299) {
      throw IllegalStateException("HTTP ${conn.responseCode}: ${text.take(140)}")
    }
    return text
  }

  private fun fetchSmallTextPayload(urlString: String): String? {
    return try {
      val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8_000
        readTimeout = 8_000
        instanceFollowRedirects = true
      }
      if (conn.responseCode !in 200..299) return null
      val contentLen = conn.contentLengthLong
      if (contentLen > MAX_REMOTE_PAYLOAD_BYTES) return null
      val bytes = conn.inputStream.use { input ->
        val buffer = ByteArray(MAX_REMOTE_PAYLOAD_BYTES + 1)
        var total = 0
        while (total < buffer.size) {
          val read = input.read(buffer, total, buffer.size - total)
          if (read <= 0) break
          total += read
        }
        if (total > MAX_REMOTE_PAYLOAD_BYTES) return null
        buffer.copyOf(total)
      }
      val text = bytes.toString(Charsets.UTF_8).trimStart()
      if (text.startsWith("{") || text.contains("\"workshopitem\"", ignoreCase = true)) text else null
    } catch (_: Exception) {
      null
    }
  }
}

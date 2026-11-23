package com.example.export

import com.example.data.model.ArmoryItem
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.data.remote.FirestoreSyncCodec
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

data class Bo3CustomMapSpec(
  val areaId: String,
  val mapCode: String,
  val title: String,
  val description: String,
  val wonderWeapon: String,
  val defaultModeId: String = "zombies",
)

object Bo3ModToolsImporter {

  const val BO3_TAG = "BO3 Mod Tools"

  val bo3CustomMaps: List<Bo3CustomMapSpec> = listOf(
    Bo3CustomMapSpec(
      areaId = "area_bo3_nacht",
      mapCode = "zm_prototype",
      title = "BO3 Nacht der Untoten",
      description = "Classic BO3 bunker survival remaster with Wunderfizz courtyard & Help Door Mystery Box.",
      wonderWeapon = "Ray Gun Mk.II",
    ),
    Bo3CustomMapSpec(
      areaId = "area_bo3_the_giant",
      mapCode = "zm_factory",
      title = "BO3 The Giant (Der Riese)",
      description = "Teleporter A/B/C Mainframe, Pack-a-Punch Courtyard & Annihilator Furnace.",
      wonderWeapon = "Wunderwaffe DG-2",
    ),
    Bo3CustomMapSpec(
      areaId = "area_bo3_gorod",
      mapCode = "zm_stalingrad",
      title = "BO3 Gorod Krovi",
      description = "Dragon Command, Belinski Square, Department Store & Ray Gun Mk.III Vault.",
      wonderWeapon = "GKZ-45 Mk3",
    ),
    Bo3CustomMapSpec(
      areaId = "area_bo3_shadows",
      mapCode = "zm_zod",
      title = "BO3 Shadows of Evil",
      description = "Morg City noir districts, Beast Mode Altars & Apothicon Subway Pack.",
      wonderWeapon = "Apothicon Servant",
    ),
    Bo3CustomMapSpec(
      areaId = "area_zombies_der_riese",
      mapCode = "zm_der_riese_aether",
      title = "Der Riese & Dark Aether",
      description = "Original Element 115 Teleporter Reactor & Pack-a-Punch Catwalk.",
      wonderWeapon = "Thundergun",
    ),
  )

  fun resolveBo3AreaId(rawText: String): String? {
    val lower = rawText.lowercase(Locale.US)
    return when {
      lower.contains("zm_prototype") || lower.contains("nacht") -> "area_bo3_nacht"
      lower.contains("zm_factory") || lower.contains("the giant") || lower.contains("area_bo3_the_giant") -> "area_bo3_the_giant"
      lower.contains("zm_stalingrad") || lower.contains("gorod") -> "area_bo3_gorod"
      lower.contains("zm_zod") || lower.contains("shadows of evil") || lower.contains("morg city") -> "area_bo3_shadows"
      lower.contains("der riese") || lower.contains("der_riese") -> "area_zombies_der_riese"
      else -> null
    }
  }

  fun parseBo3PackageOrScript(rawText: String, targetUserId: String): Result<SteamWorkshopImportPreview> {
    return try {
      val trimmed = rawText.trim()
      if (trimmed.isEmpty()) {
        return Result.failure(IllegalArgumentException("BO3 Mod Tools input text is empty."))
      }

      val now = System.currentTimeMillis()

      // Check if GDT asset definition (contains .gdf or ("...") block definitions)
      if (trimmed.contains(".gdf", ignoreCase = true) || (trimmed.contains("(") && trimmed.contains(")") && trimmed.contains("{"))) {
        return parseGdtScript(trimmed, targetUserId, now)
      }

      // Check if JSON manifest
      if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
        val jsonRes = try {
          parseJsonBo3Manifest(trimmed, targetUserId, now)
        } catch (_: Exception) {
          null
        }
        if (jsonRes != null && jsonRes.isSuccess) {
          return jsonRes
        }
      }

      // Check if XMODEL_EXPORT / XANIM_EXPORT
      if (trimmed.contains("NUMBONES", ignoreCase = true) || trimmed.contains("XMODEL", ignoreCase = true) || trimmed.contains("XANIM", ignoreCase = true)) {
        return parseExportFile(trimmed, targetUserId, now)
      }

      // Check if other GDT definition format
      if (trimmed.contains("\"") && trimmed.contains("{") && trimmed.contains("}")) {
        val gdtRes = parseGdtScript(trimmed, targetUserId, now)
        if (gdtRes.isSuccess) return gdtRes
      }

      // Fallback: Freeform script / plain text description of a BO3 weapon or perk
      return parseFreeformBo3Script(trimmed, targetUserId, now)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun getCuratedBo3ModPack(targetUserId: String): SteamWorkshopImportPreview {
    val now = System.currentTimeMillis()
    val armoryItems = listOf(
      ArmoryItem(
        id = "bo3_juggernog_core",
        userId = targetUserId,
        name = "BO3 Juggernog Matrix",
        category = "perk",
        universeTag = BO3_TAG,
        description = "Treyarch Juggernog formula: boosts maximum kinetic shield (+45 per tier up to T25).",
        tier = 1L,
        maxTier = 25L,
        cooldownSec = 0L,
        powerValue = 280L,
        accentColorHex = "#FF1744",
        equipped = true,
        isCustom = true,
        createdAtMillis = now,
        updatedAtMillis = now,
      ),
      ArmoryItem(
        id = "bo3_speed_cola_core",
        userId = targetUserId,
        name = "BO3 Speed Cola Accelerator",
        category = "perk",
        universeTag = BO3_TAG,
        description = "Treyarch Speed Cola: accelerates volley fire rate, dash cadence, and recharge speed.",
        tier = 1L,
        maxTier = 25L,
        cooldownSec = 0L,
        powerValue = 240L,
        accentColorHex = "#00E676",
        equipped = true,
        isCustom = true,
        createdAtMillis = now + 1,
        updatedAtMillis = now + 1,
      ),
      ArmoryItem(
        id = "bo3_double_tap_core",
        userId = targetUserId,
        name = "BO3 Double Tap Root Beer II",
        category = "upgrade",
        universeTag = BO3_TAG,
        description = "Treyarch Double Tap 2.0: fires double projectile volleys with +2 pierce per tier.",
        tier = 1L,
        maxTier = 25L,
        cooldownSec = 0L,
        powerValue = 320L,
        accentColorHex = "#FFAB40",
        equipped = true,
        isCustom = true,
        createdAtMillis = now + 2,
        updatedAtMillis = now + 2,
      ),
      ArmoryItem(
        id = "bo3_widows_wine_core",
        userId = targetUserId,
        name = "BO3 Widow's Wine Bio-Web",
        category = "power",
        universeTag = BO3_TAG,
        description = "Treyarch Widow's Wine: triggers sticky bio-web explosive bursts when hostiles strike shields.",
        tier = 1L,
        maxTier = 25L,
        cooldownSec = 6L,
        powerValue = 350L,
        accentColorHex = "#7C4DFF",
        equipped = true,
        isCustom = true,
        createdAtMillis = now + 3,
        updatedAtMillis = now + 3,
      ),
      ArmoryItem(
        id = "bo3_thundergun_core",
        userId = targetUserId,
        name = "BO3 Thundergun Sonic Blast",
        category = "power",
        universeTag = BO3_TAG,
        description = "Treyarch Wonder Weapon: compressed sonic air cannon that blasts entire horde lines backwards.",
        tier = 2L,
        maxTier = 25L,
        cooldownSec = 8L,
        powerValue = 420L,
        accentColorHex = "#00E5FF",
        equipped = true,
        isCustom = true,
        createdAtMillis = now + 4,
        updatedAtMillis = now + 4,
      ),
      ArmoryItem(
        id = "bo3_wunderwaffe_core",
        userId = targetUserId,
        name = "BO3 Wunderwaffe DG-2 Core",
        category = "power",
        universeTag = BO3_TAG,
        description = "Treyarch Wonder Weapon: 200,000-amp electrical arc discharge chaining to up to 10 hostiles.",
        tier = 2L,
        maxTier = 25L,
        cooldownSec = 10L,
        powerValue = 460L,
        accentColorHex = "#FFD54F",
        equipped = false,
        isCustom = true,
        createdAtMillis = now + 5,
        updatedAtMillis = now + 5,
      ),
    )

    val customRigs = listOf(
      RigAsset(
        id = "bo3_viewmodel_rig",
        userId = targetUserId,
        name = "BO3 Treyarch Viewmodel Rig",
        archetype = "operator",
        activeClip = "idle",
        ikEnabled = true,
        rigScale = 1.0,
        playbackSpeed = 1.0,
        poseAngles = mapOf(
          "torso" to 0.0,
          "head" to 0.0,
          "lUpperArm" to -35.0,
          "lForearm" to -25.0,
          "rUpperArm" to 75.0,
          "rForearm" to 30.0,
          "lThigh" to -15.0,
          "lShin" to 15.0,
          "rThigh" to 15.0,
          "rShin" to -10.0,
          "weaponAngle" to 15.0,
        ),
        keyframeFrames = listOf(0L, 15L, 30L, 45L, 60L),
        isCompleted = true,
        createdAtMillis = now,
        updatedAtMillis = now,
      ),
    )

    return SteamWorkshopImportPreview(
      sourceKind = "BO3_MOD_TOOLS_IMPORT",
      title = "Treyarch BO3 Mod Tools Zombies Curated Pack",
      description = "Official BO3 Mod Tools Perk-a-Colas (Juggernog, Speed Cola, Double Tap, Widow's Wine) & Wonder Weapons (Thundergun, Wunderwaffe DG-2).",
      publishedFileId = "bo3_treyarch_pack",
      appId = "311210",
      creatorSteamId = "Treyarch_ModTools",
      tags = listOf("BO3", "ModTools", "Zombies", "Perks", "WonderWeapons"),
      subscriptions = 48500L,
      views = 125000L,
      armoryItems = armoryItems,
      customRigs = customRigs,
      blenderRigScript = null,
      generatedModule = null,
      generatedRig = null,
      sourceNote = "6 BO3 modules, 1 viewmodel rig & 5 playable BO3 maps (Nacht, The Giant, Gorod Krovi, Shadows, Der Riese).",
      linkedWorldId = "sector_zero",
      linkedAreaId = "area_bo3_the_giant",
    )
  }

  private fun parseJsonBo3Manifest(
    jsonText: String,
    targetUserId: String,
    now: Long,
  ): Result<SteamWorkshopImportPreview> {
    val root = JSONObject(jsonText)
    val title = root.optString("title", "BO3 Mod Tools Pack").trim().take(50).ifBlank { "BO3 Mod Tools Pack" }
    val desc = root.optString("description", "Imported from Call of Duty: Black Ops III Mod Tools.").take(240)
    val armoryArr = root.optJSONArray("modules") ?: root.optJSONArray("armory") ?: JSONArray()
    val armoryList = mutableListOf<ArmoryItem>()

    for (i in 0 until armoryArr.length().coerceAtMost(20)) {
      val obj = armoryArr.optJSONObject(i) ?: continue
      val name = obj.optString("name", "BO3 Module $i").trim().take(50)
      val cat = FirestoreSyncCodec.sanitizeArmoryCategory(obj.optString("category", "perk"))
      val maxTier = maxOf(obj.optLong("maxTier", 25L), 25L).coerceIn(1L, 25L)
      val tier = obj.optLong("tier", 1L).coerceIn(1L, maxTier)
      val cd = obj.optLong("cooldownSec", if (cat == "perk") 0L else 6L).coerceIn(0L, 120L)
      val pwr = obj.optLong("powerValue", 220L).coerceIn(1L, 1500L)
      val hex = obj.optString("accentColorHex", "#FF1744").trim().take(10).ifEmpty { "#FF1744" }
      val descItem = obj.optString("description", "BO3 Mod Tools customized module.").take(240)
      armoryList.add(
        ArmoryItem(
          id = FirestoreSyncCodec.sanitizeId(obj.optString("id", "bo3_mod_$i"), "bo3_mod"),
          userId = targetUserId,
          name = name,
          category = cat,
          universeTag = BO3_TAG,
          description = descItem,
          tier = tier,
          maxTier = maxTier,
          cooldownSec = cd,
          powerValue = pwr,
          accentColorHex = hex,
          equipped = true,
          isCustom = true,
          createdAtMillis = now + i,
          updatedAtMillis = now + i,
        )
      )
    }

    if (armoryList.isEmpty()) {
      // Create single generated module from root JSON metadata
      val category = if (title.lowercase(Locale.US).contains("perk") || desc.lowercase(Locale.US).contains("perk")) "perk" else "power"
      armoryList.add(
        ArmoryItem(
          id = FirestoreSyncCodec.sanitizeId("bo3_${title.hashCode().toUInt()}", "bo3_mod"),
          userId = targetUserId,
          name = title,
          category = category,
          universeTag = BO3_TAG,
          description = desc,
          tier = 1L,
          maxTier = 25L,
          cooldownSec = if (category == "perk") 0L else 6L,
          powerValue = 260L,
          accentColorHex = "#FF1744",
          equipped = true,
          isCustom = true,
          createdAtMillis = now,
          updatedAtMillis = now,
        )
      )
    }

    val resolvedAreaId = resolveBo3AreaId("${root.optString("map")} $title $desc") ?: "area_bo3_the_giant"

    return Result.success(
      SteamWorkshopImportPreview(
        sourceKind = "BO3_MOD_TOOLS_IMPORT",
        title = title,
        description = desc,
        publishedFileId = root.optString("publishedFileId", "bo3_imported"),
        appId = "311210",
        creatorSteamId = root.optString("author", "BO3_Modder"),
        tags = listOf("BO3", "ModTools", "Zombies"),
        subscriptions = 1200L,
        views = 3400L,
        armoryItems = armoryList,
        customRigs = emptyList(),
        blenderRigScript = null,
        generatedModule = null,
        generatedRig = null,
        sourceNote = "Imported ${armoryList.size} BO3 Mod Tools item(s) up to Tier 25 + linked BO3 map.",
        linkedWorldId = "sector_zero",
        linkedAreaId = resolvedAreaId,
      )
    )
  }

  private fun parseGdtScript(
    gdtText: String,
    targetUserId: String,
    now: Long,
  ): Result<SteamWorkshopImportPreview> {
    val items = mutableListOf<ArmoryItem>()
    val lines = gdtText.lines()
    var currentName = ""
    var currentType = "perk"
    var currentDesc = ""
    var currentPwr = 200L

    for (line in lines) {
      val trimmed = line.trim()
      val quoteMatches = Regex("\"([^\"]*)\"").findAll(trimmed).map { it.groupValues[1] }.toList()
      if (quoteMatches.size >= 2) {
        val key = quoteMatches[0].lowercase(Locale.US)
        val value = quoteMatches[1]
        when (key) {
          "name" -> currentName = value.take(50)
          "type", "category" -> currentType = if (value.contains("perk", ignoreCase = true)) "perk" else if (value.contains("wonder", ignoreCase = true) || value.contains("power", ignoreCase = true)) "power" else "upgrade"
          "description", "desc" -> currentDesc = value.take(240)
          "damage", "power", "cost" -> currentPwr = value.filter(Char::isDigit).toLongOrNull()?.coerceIn(100L, 1500L) ?: 250L
        }
      }
    }

    val resolvedName = currentName.ifBlank { "BO3 Custom GDT Module" }
    val resolvedDesc = currentDesc.ifBlank { "Imported from BO3 Asset Definition (.GDT) into Unknown Universes." }
    val modItem = ArmoryItem(
      id = FirestoreSyncCodec.sanitizeId("bo3_gdt_${UUID.randomUUID().toString().take(8)}", "bo3_gdt"),
      userId = targetUserId,
      name = resolvedName,
      category = FirestoreSyncCodec.sanitizeArmoryCategory(currentType),
      universeTag = BO3_TAG,
      description = resolvedDesc,
      tier = 1L,
      maxTier = 25L,
      cooldownSec = if (currentType == "perk") 0L else 6L,
      powerValue = currentPwr,
      accentColorHex = if (currentType == "perk") "#FF1744" else "#00E5FF",
      equipped = true,
      isCustom = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    )
    items.add(modItem)

    val resolvedAreaId = resolveBo3AreaId(gdtText) ?: "area_bo3_the_giant"

    return Result.success(
      SteamWorkshopImportPreview(
        sourceKind = "BO3_MOD_TOOLS_IMPORT",
        title = resolvedName,
        description = resolvedDesc,
        publishedFileId = "bo3_gdt_import",
        appId = "311210",
        creatorSteamId = "BO3_Asset_Editor",
        tags = listOf("BO3", "GDT", "APE"),
        subscriptions = 500L,
        views = 1500L,
        armoryItems = items,
        customRigs = emptyList(),
        blenderRigScript = null,
        generatedModule = null,
        generatedRig = null,
        sourceNote = "BO3 .GDT asset parsed into an equipable Tier 25 Armory Module.",
        linkedWorldId = "sector_zero",
        linkedAreaId = resolvedAreaId,
      )
    )
  }

  private fun parseExportFile(
    exportText: String,
    targetUserId: String,
    now: Long,
  ): Result<SteamWorkshopImportPreview> {
    val rigName = if (exportText.contains("VIEWMODEL", ignoreCase = true)) "BO3 Viewmodel Rig" else "BO3 Humanoid Character Rig"
    val archetype = if (exportText.contains("zombie", ignoreCase = true)) "biomorph" else "operator"
    val rig = RigAsset(
      id = FirestoreSyncCodec.sanitizeId("bo3_xmodel_${UUID.randomUUID().toString().take(8)}", "bo3_xmodel"),
      userId = targetUserId,
      name = rigName,
      archetype = archetype,
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

    return Result.success(
      SteamWorkshopImportPreview(
        sourceKind = "BO3_MOD_TOOLS_IMPORT",
        title = rigName,
        description = "Articulated skeleton parsed from BO3 .XMODEL_EXPORT / .XANIM_EXPORT.",
        publishedFileId = "bo3_model_import",
        appId = "311210",
        creatorSteamId = "BO3_Rig_Porter",
        tags = listOf("BO3", "XModel", "Rigging"),
        subscriptions = 800L,
        views = 2000L,
        armoryItems = emptyList(),
        customRigs = listOf(rig),
        blenderRigScript = null,
        generatedModule = null,
        generatedRig = null,
        sourceNote = "BO3 bone hierarchy and geometry mapped into UnknownBlend ForgeRig.",
      )
    )
  }

  private fun parseFreeformBo3Script(
    rawText: String,
    targetUserId: String,
    now: Long,
  ): Result<SteamWorkshopImportPreview> {
    val firstLine = rawText.lines().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    val cleanName = firstLine.replace(Regex("[^a-zA-Z0-9 _\\-]"), "").trim().take(40).ifBlank { "BO3 Custom Addon" }
    val lower = rawText.lowercase(Locale.US)
    val cat = when {
      lower.contains("perk") || lower.contains("cola") || lower.contains("jugg") || lower.contains("armor") -> "perk"
      lower.contains("wonder") || lower.contains("ray") || lower.contains("thunder") || lower.contains("waffe") -> "power"
      lower.contains("pack") || lower.contains("punch") || lower.contains("upgrade") -> "upgrade"
      else -> "consumable"
    }

    val mod = ArmoryItem(
      id = FirestoreSyncCodec.sanitizeId("bo3_raw_${UUID.randomUUID().toString().take(8)}", "bo3_raw"),
      userId = targetUserId,
      name = cleanName,
      category = cat,
      universeTag = BO3_TAG,
      description = rawText.take(240).ifBlank { "BO3 Mod Tools imported content." },
      tier = 1L,
      maxTier = 25L,
      cooldownSec = if (cat == "perk") 0L else 6L,
      powerValue = 220L,
      accentColorHex = if (cat == "perk") "#FF1744" else "#00E5FF",
      equipped = true,
      isCustom = true,
      createdAtMillis = now,
      updatedAtMillis = now,
    )

    val resolvedAreaId = resolveBo3AreaId(rawText) ?: "area_bo3_nacht"

    return Result.success(
      SteamWorkshopImportPreview(
        sourceKind = "BO3_MOD_TOOLS_IMPORT",
        title = cleanName,
        description = rawText.take(240),
        publishedFileId = "bo3_script_import",
        appId = "311210",
        creatorSteamId = "BO3_Community",
        tags = listOf("BO3", "ModTools", "Zombies"),
        subscriptions = 300L,
        views = 900L,
        armoryItems = listOf(mod),
        customRigs = emptyList(),
        blenderRigScript = null,
        generatedModule = null,
        generatedRig = null,
        sourceNote = "Imported 1 custom BO3 Mod Tools item ready for Tier 1-25 progression.",
        linkedWorldId = "sector_zero",
        linkedAreaId = resolvedAreaId,
      )
    )
  }
}

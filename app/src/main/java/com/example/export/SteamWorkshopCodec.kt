package com.example.export

import com.example.BuildConfig
import com.example.data.local.SaveDataSerializer
import com.example.data.model.ArmoryItem
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
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
)

object SteamWorkshopCodec {

  const val WORKSHOP_FORMAT_MARKER = "unknown_universes_workshop_v1"
  const val SCHEMA_VERSION = 1
  private const val STEAM_PUBLISHED_FILE_API =
    "https://api.steampowered.com/ISteamRemoteStorage/GetPublishedFileDetails/v1/"
  private const val MAX_REMOTE_PAYLOAD_BYTES = 1_500_000

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
    val archetype = when {
      lowerMeta.contains("star") || lowerMeta.contains("saber") || lowerMeta.contains("jedi") -> "starblade"
      lowerMeta.contains("bio") || lowerMeta.contains("zombie") || lowerMeta.contains("infected") -> "biomorph"
      lowerMeta.contains("fire") || lowerMeta.contains("hell") || lowerMeta.contains("infernal") -> "infernal"
      lowerMeta.contains("time") || lowerMeta.contains("chrono") || lowerMeta.contains("cyber") -> "chrono"
      else -> "operator"
    }
    val category = if (lowerMeta.contains("perk") || lowerMeta.contains("passive") || lowerMeta.contains("armor")) "perk" else "power"
    val cleanDesc = details.description
      .ifBlank { "Imported from Steam Workshop #${details.publishedFileId} ($tagSummary)" }
      .take(240)

    val addonModule = ArmoryItem(
      id = FirestoreSyncCodec.sanitizeId("ws_mod_$idToken", "ws_mod"),
      userId = targetUserId,
      name = details.title.trim().take(50).ifEmpty { "Workshop Module" },
      category = category,
      universeTag = "Workshop #$idToken".take(40),
      description = cleanDesc,
      tier = 1L,
      maxTier = 25L,
      cooldownSec = if (category == "perk") 0L else 6L,
      powerValue = 175L,
      accentColorHex = "#66C0F4",
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
      appId = details.consumerAppId.takeIf { it.isNotBlank() },
      creatorSteamId = details.creatorSteamId.takeIf { it.isNotBlank() },
      tags = details.tags,
      subscriptions = details.subscriptions,
      views = details.views,
      armoryItems = emptyList(),
      customRigs = emptyList(),
      blenderRigScript = null,
      generatedModule = addonModule,
      generatedRig = addonRig,
      sourceNote = "Workshop metadata resolved. Import converts it into an equipped Armory Module and matching ForgeRig preset.",
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

package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.R
import com.example.data.model.ArmoryItem
import com.example.data.model.LinkedAccount
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.export.Bo3ModToolsImporter
import com.example.export.SteamWorkshopCodec
import com.example.export.SteamWorkshopExportConfig
import com.example.export.SteamWorkshopImportPreview
import com.example.export.WorkshopVisibility
import com.example.ui.theme.AppIcons
import kotlinx.coroutines.launch

private val SteamAccent = Color(0xFF66C0F4)

@Composable
fun SteamWorkshopCard(
  profile: OperatorProfile,
  armory: List<ArmoryItem>,
  rigs: List<RigAsset>,
  linkedSteamAccount: LinkedAccount?,
  onConfirmWorkshopImport: (SteamWorkshopImportPreview, Boolean, Boolean) -> Unit,
  onStatusMessage: (String) -> Unit,
  onOpenBo3Export: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val activeRig = rigs.firstOrNull { it.id == profile.activeRigId } ?: rigs.firstOrNull()

  var activeTab by remember { mutableStateOf("import") }
  var workshopUrlOrId by remember { mutableStateOf("") }
  var isFetching by remember { mutableStateOf(false) }

  var exportAppId by remember { mutableStateOf("480") }
  var exportPubId by remember { mutableStateOf("0") }
  var exportTitle by remember(profile.callsign, activeRig?.name) {
    mutableStateOf("${profile.callsign} — ${activeRig?.name ?: "UnknownOS Loadout"}".take(80))
  }
  var exportDesc by remember(profile.activeUniverseId, activeRig?.archetype) {
    mutableStateOf(
      "UnknownOS Workshop Pack built by ${profile.callsign}. Includes custom ForgeRig poses, Armory perks/powers, and SteamCMD .VDF manifest.",
    )
  }
  var exportVisibility by remember { mutableStateOf(WorkshopVisibility.PUBLIC) }
  var includeRigs by remember { mutableStateOf(true) }
  var includeArmory by remember { mutableStateOf(true) }
  var includeBlenderScript by remember { mutableStateOf(true) }

  var pendingFileContent by remember { mutableStateOf("") }
  var pendingImportPreview by remember { mutableStateOf<SteamWorkshopImportPreview?>(null) }
  var showPasteDialog by remember { mutableStateOf(false) }
  var pasteDraft by remember { mutableStateOf("") }

  var importModulePreset by remember { mutableStateOf(true) }
  var importRigPreset by remember { mutableStateOf(true) }

  val saveWorkshopDocLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
      if (uri != null) {
        try {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(pendingFileContent.toByteArray(Charsets.UTF_8))
          }
          onStatusMessage("Steam Workshop package exported successfully!")
        } catch (e: Exception) {
          onStatusMessage("Workshop export failed: ${e.localizedMessage}")
        }
      }
    }

  val openWorkshopDocLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        try {
          val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
            it.readText()
          }.orEmpty()
          val parsed = SteamWorkshopCodec.parseWorkshopImportInput(raw, profile.userId)
          if (parsed.isSuccess) {
            importModulePreset = true
            importRigPreset = true
            pendingImportPreview = parsed.getOrThrow()
          } else {
            onStatusMessage(parsed.exceptionOrNull()?.localizedMessage ?: "Invalid Steam Workshop file")
          }
        } catch (e: Exception) {
          onStatusMessage("Could not read Workshop file: ${e.localizedMessage}")
        }
      }
    }

  fun buildExportConfig(): SteamWorkshopExportConfig = SteamWorkshopExportConfig(
    appId = exportAppId,
    publishedFileId = exportPubId,
    title = exportTitle,
    description = exportDesc,
    visibility = exportVisibility,
    includeRigs = includeRigs,
    includeArmory = includeArmory,
    includeBlenderScript = includeBlenderScript,
    authorSteamId = linkedSteamAccount?.externalSubject.orEmpty(),
    authorPersona = linkedSteamAccount?.displayName.orEmpty(),
  )

  fun openExternalUrl(url: String) {
    runCatching {
      context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }.onFailure {
      onStatusMessage("No web browser available to open Steam Workshop.")
    }
  }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier
      .fillMaxWidth()
      .border(1.5.dp, SteamAccent.copy(alpha = 0.58f), RoundedCornerShape(18.dp))
      .testTag("steam_workshop_card"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(if (linkedSteamAccount != null) Color(0xFF00E676) else SteamAccent),
          )
          Column {
            Text(
              text = stringResource(R.string.steam_workshop_title),
              style = MaterialTheme.typography.titleMedium,
              color = SteamAccent,
            )
            Text(
              text = if (linkedSteamAccount != null) {
                "Steam Creator: ${linkedSteamAccount.displayName} (ID ${linkedSteamAccount.externalSubject})"
              } else {
                "Fetch any Workshop URL/ID, import .VDF/.JSON, or export SteamCMD packs"
              },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
          onClick = {
            openExternalUrl(
              SteamWorkshopCodec.buildWorkshopPortalUrl(
                appId = exportAppId,
                steamId64 = linkedSteamAccount?.externalSubject,
              ),
            )
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.heightIn(min = 48.dp).testTag("steam_workshop_open_portal_btn"),
        ) {
          Icon(AppIcons.Web, contentDescription = null, modifier = Modifier.size(16.dp), tint = SteamAccent)
          Spacer(Modifier.width(4.dp))
          Text("Workshop", style = MaterialTheme.typography.labelSmall, color = SteamAccent)
        }
      }

      // Segmented Mode Toggle (Import vs Export)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf("import" to "Import Workshop", "export" to "Export Workshop").forEach { (key, label) ->
          val selected = activeTab == key
          Surface(
            onClick = { activeTab = key },
            shape = RoundedCornerShape(10.dp),
            color = if (selected) SteamAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) SteamAccent else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(10.dp),
              )
              .testTag("workshop_tab_$key"),
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) SteamAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
              )
            }
          }
        }
      }

      AnimatedVisibility(visible = activeTab == "import") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = workshopUrlOrId,
            onValueChange = { workshopUrlOrId = it },
            label = { Text("Steam Workshop URL or PublishedFileId") },
            placeholder = { Text("https://steamcommunity.com/sharedfiles/filedetails/?id=...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("workshop_url_input"),
          )

          Button(
            onClick = {
              if (!isFetching) {
                isFetching = true
                scope.launch {
                  val res = SteamWorkshopCodec.fetchSteamWorkshopItem(workshopUrlOrId, profile.userId)
                  isFetching = false
                  if (res.isSuccess) {
                    importModulePreset = true
                    importRigPreset = true
                    pendingImportPreview = res.getOrThrow()
                  } else {
                    onStatusMessage(res.exceptionOrNull()?.localizedMessage ?: "Workshop lookup failed")
                  }
                }
              }
            },
            enabled = !isFetching && workshopUrlOrId.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
              containerColor = SteamAccent,
              contentColor = Color(0xFF06131F),
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("workshop_fetch_btn"),
          ) {
            if (isFetching) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF06131F),
              )
              Spacer(Modifier.width(8.dp))
              Text("Fetching Steam Workshop...", style = MaterialTheme.typography.labelLarge)
            } else {
              Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text(stringResource(R.string.steam_workshop_fetch_btn), style = MaterialTheme.typography.labelLarge)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = { openWorkshopDocLauncher.launch(arrayOf("*/*")) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("workshop_import_file_btn"),
            ) {
              Icon(AppIcons.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(stringResource(R.string.steam_workshop_import_file_btn), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }

            OutlinedButton(
              onClick = {
                pasteDraft = ""
                showPasteDialog = true
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("workshop_paste_btn"),
            ) {
              Icon(AppIcons.Code, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(stringResource(R.string.steam_workshop_paste_btn), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
          }

          Button(
            onClick = {
              val bo3Pack = Bo3ModToolsImporter.getCuratedBo3ModPack(profile.userId)
              importModulePreset = true
              importRigPreset = true
              pendingImportPreview = bo3Pack
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("workshop_import_bo3_preset_btn"),
          ) {
            Icon(AppIcons.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Import Curated BO3 Mod Tools Pack", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
          }
        }
      }

      AnimatedVisibility(visible = activeTab == "export") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = exportAppId == "311210",
              onClick = {
                exportAppId = "311210"
                exportTitle = "BO3 Mod Tools — ${activeRig?.name ?: "UnknownRig"}".take(80)
                exportDesc = "Call of Duty: Black Ops 3 Mod Tools Workshop item by ${profile.callsign}. Includes custom .xmodel_export and .xanim_export assets."
              },
              label = { Text("BO3 Mod Tools (311210)") },
              modifier = Modifier.testTag("workshop_preset_bo3"),
            )
            FilterChip(
              selected = exportAppId == "480",
              onClick = {
                exportAppId = "480"
                exportTitle = "${profile.callsign} — ${activeRig?.name ?: "UnknownOS Loadout"}".take(80)
                exportDesc = "UnknownOS Workshop Pack built by ${profile.callsign}."
              },
              label = { Text("Generic / Spacewar (480)") },
              modifier = Modifier.testTag("workshop_preset_generic"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = exportAppId,
              onValueChange = { exportAppId = it.filter(Char::isDigit).take(12) },
              label = { Text("Steam App ID") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("workshop_appid_input"),
            )
            OutlinedTextField(
              value = exportPubId,
              onValueChange = { exportPubId = it.filter(Char::isDigit).take(18) },
              label = { Text("PublishedFileId (0 = new)") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("workshop_pubid_input"),
            )
          }

          OutlinedTextField(
            value = exportTitle,
            onValueChange = { exportTitle = it.take(128) },
            label = { Text("Workshop Item Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("workshop_export_title_input"),
          )

          OutlinedTextField(
            value = exportDesc,
            onValueChange = { exportDesc = it.take(1000) },
            label = { Text("Workshop Description & Changenote") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth().testTag("workshop_export_desc_input"),
          )

          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            WorkshopVisibility.entries.forEach { vis ->
              FilterChip(
                selected = exportVisibility == vis,
                onClick = { exportVisibility = vis },
                label = { Text(vis.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("workshop_vis_${vis.name.lowercase()}"),
              )
            }
          }

          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = includeRigs,
              onClick = { includeRigs = !includeRigs },
              label = { Text("Rigs (${rigs.size})") },
              modifier = Modifier.testTag("workshop_toggle_rigs"),
            )
            FilterChip(
              selected = includeArmory,
              onClick = { includeArmory = !includeArmory },
              label = { Text("Armory (${armory.size})") },
              modifier = Modifier.testTag("workshop_toggle_armory"),
            )
            FilterChip(
              selected = includeBlenderScript,
              onClick = { includeBlenderScript = !includeBlenderScript },
              label = { Text("Blender .PY") },
              modifier = Modifier.testTag("workshop_toggle_blender"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = {
                pendingFileContent = SteamWorkshopCodec.generateWorkshopVdf(buildExportConfig())
                val appToken = SteamWorkshopCodec.sanitizeNumericId(exportAppId, "480")
                saveWorkshopDocLauncher.launch("workshop_${appToken}_item.vdf")
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = SteamAccent,
                contentColor = Color(0xFF06131F),
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("workshop_export_vdf_btn"),
            ) {
              Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(stringResource(R.string.steam_workshop_export_vdf_btn), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }

            OutlinedButton(
              onClick = {
                pendingFileContent = SteamWorkshopCodec.generateWorkshopPackageJson(
                  config = buildExportConfig(),
                  profile = profile,
                  armory = armory,
                  rigs = rigs,
                )
                saveWorkshopDocLauncher.launch("unknown_universes_workshop.json")
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("workshop_export_json_btn"),
            ) {
              Icon(AppIcons.Code, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(stringResource(R.string.steam_workshop_export_json_btn), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
          }

          OutlinedButton(
            onClick = {
              val cfg = buildExportConfig()
              val vdf = SteamWorkshopCodec.generateWorkshopVdf(cfg)
              val wsJson = SteamWorkshopCodec.generateWorkshopPackageJson(cfg, profile, armory, rigs)
              val combined = "=== workshop_item.vdf (SteamCMD) ===\n$vdf\n\n=== unknown_universes_workshop.json ===\n$wsJson"
              val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Steam Workshop Pack: ${cfg.title}")
                putExtra(Intent.EXTRA_TEXT, combined)
              }
              context.startActivity(Intent.createChooser(sendIntent, "Share Steam Workshop Pack"))
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("workshop_share_btn"),
          ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.steam_workshop_share_btn), style = MaterialTheme.typography.labelMedium)
          }

          if (onOpenBo3Export != null) {
            OutlinedButton(
              onClick = onOpenBo3Export,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("workshop_open_bo3_btn"),
            ) {
              Icon(AppIcons.Animation, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
              Spacer(Modifier.width(6.dp))
              Text("Open BO3 Mod Tools Exporter (.XMODEL / .XANIM / .GDT)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            }
          }
        }
      }
    }
  }

  if (showPasteDialog) {
    AlertDialog(
      onDismissRequest = { showPasteDialog = false },
      title = { Text("Paste Steam Workshop .VDF or .JSON") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Paste a Valve KeyValues workshop_item.vdf manifest, GetPublishedFileDetails API JSON, or Unknown Universes Workshop JSON.",
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = pasteDraft,
            onValueChange = { pasteDraft = it },
            label = { Text("Workshop VDF or JSON") },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth().testTag("workshop_paste_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val parsed = SteamWorkshopCodec.parseWorkshopImportInput(pasteDraft, profile.userId)
            if (parsed.isSuccess) {
              showPasteDialog = false
              importModulePreset = true
              importRigPreset = true
              pendingImportPreview = parsed.getOrThrow()
            } else {
              onStatusMessage(parsed.exceptionOrNull()?.localizedMessage ?: "Unrecognized Workshop format")
            }
          },
          modifier = Modifier.testTag("workshop_parse_pasted_btn"),
        ) { Text("Preview Item") }
      },
      dismissButton = {
        TextButton(onClick = { showPasteDialog = false }) { Text("Cancel") }
      },
    )
  }

  pendingImportPreview?.let { preview ->
    val isSynthesizedFallback = preview.armoryItems.isEmpty() && preview.customRigs.isEmpty()
    AlertDialog(
      onDismissRequest = { pendingImportPreview = null },
      title = { Text(preview.title) },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = buildString {
              append("Source: ${preview.sourceKind}")
              preview.publishedFileId?.let { append(" • ID #$it") }
              preview.appId?.let { append(" • App $it") }
            },
            style = MaterialTheme.typography.labelSmall,
            color = SteamAccent,
          )
          if (!preview.creatorSteamId.isNullOrBlank()) {
            Text(
              text = "Creator SteamID: ${preview.creatorSteamId}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (preview.subscriptions > 0L || preview.views > 0L) {
            Text(
              text = "Subscriptions: ${preview.subscriptions} • Views: ${preview.views}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.tertiary,
            )
          }
          if (preview.tags.isNotEmpty()) {
            Text(
              text = "Tags: ${preview.tags.joinToString(", ")}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (preview.description.isNotBlank()) {
            Text(
              text = preview.description.take(360),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          if (preview.sourceNote.isNotBlank()) {
            Text(
              text = preview.sourceNote,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          if (!isSynthesizedFallback) {
            Text(
              text = "Direct payload: ${preview.armoryItems.size} Armory Module(s), ${preview.customRigs.size} ForgeRig(s).",
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFF00E676),
            )
          } else {
            preview.generatedModule?.let { mod ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                  checked = importModulePreset,
                  onCheckedChange = { importModulePreset = it },
                )
                Text(
                  text = "Import as Armory ${mod.category}: ${mod.name}",
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
            preview.generatedRig?.let { rig ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                  checked = importRigPreset,
                  onCheckedChange = { importRigPreset = it },
                )
                Text(
                  text = "Import as ${rig.archetype} ForgeRig: ${rig.name}",
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
          }

          preview.publishedFileId?.let { pubId ->
            TextButton(
              onClick = { openExternalUrl(SteamWorkshopCodec.workshopItemWebUrl(pubId)) },
              modifier = Modifier.testTag("workshop_view_on_steam_btn"),
            ) {
              Text("Open Item Page on Steam Community")
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onConfirmWorkshopImport(preview, importModulePreset, importRigPreset)
            pendingImportPreview = null
          },
          modifier = Modifier.testTag("workshop_confirm_import_btn"),
        ) { Text("Import to Armory & Rigs") }
      },
      dismissButton = {
        TextButton(onClick = { pendingImportPreview = null }) { Text("Cancel") }
      },
    )
  }
}

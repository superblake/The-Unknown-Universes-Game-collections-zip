package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CloudSyncState
import com.example.data.local.SaveBundlePreview
import com.example.security.SaveIntegrityStatus
import com.example.ui.theme.AppIcons

@Composable
fun CloudSyncAndSaveCard(
  callsign: String,
  cloudSyncState: CloudSyncState,
  isCloudSignedIn: Boolean,
  onSyncNow: () -> Unit,
  onRequestSaveExportJson: (onReady: (String) -> Unit) -> Unit,
  onParseSaveJson: (String) -> Result<SaveBundlePreview>,
  onConfirmSaveImport: (SaveBundlePreview, Boolean) -> Unit,
  onStatusMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var pendingSaveJson by remember { mutableStateOf("") }
  var pendingImportPreview by remember { mutableStateOf<SaveBundlePreview?>(null) }
  var showPasteDialog by remember { mutableStateOf(false) }
  var pasteDraft by remember { mutableStateOf("") }

  val saveFileLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) {
        try {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(pendingSaveJson.toByteArray(Charsets.UTF_8))
          }
          onStatusMessage("Save data exported (.json) — ready to share!")
        } catch (e: Exception) {
          onStatusMessage("Save export failed: ${e.localizedMessage}")
        }
      }
    }

  val openFileLauncher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.OpenDocument(),
    ) { uri ->
      if (uri != null) {
        try {
          val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
            it.readText()
          }.orEmpty()
          val parsed = onParseSaveJson(raw)
          if (parsed.isSuccess) {
            pendingImportPreview = parsed.getOrThrow()
          } else {
            onStatusMessage(parsed.exceptionOrNull()?.localizedMessage ?: "Invalid save file")
          }
        } catch (e: Exception) {
          onStatusMessage("Could not read save file: ${e.localizedMessage}")
        }
      }
    }

  val (statusDotColor, statusLabel, statusDetail) = when (cloudSyncState) {
    is CloudSyncState.Synced -> Triple(
      Color(0xFF00E676),
      "CLOUD SYNCED",
      "Profile, Armory, Rigs & Match Logs are backed up to Firebase Cloud.",
    )
    is CloudSyncState.Syncing -> Triple(
      Color(0xFF00E5FF),
      "SYNCING CLOUD...",
      "Synchronizing UnknownOS progression across devices...",
    )
    is CloudSyncState.Error -> Triple(
      Color(0xFFFF5252),
      "SYNC RETRY NEEDED",
      cloudSyncState.message,
    )
    is CloudSyncState.LocalOnly -> Triple(
      Color(0xFFFFAB40),
      if (isCloudSignedIn) "CLOUD STANDBY" else "LOCAL SAVE ONLY",
      if (isCloudSignedIn) {
        "Tap Sync Now to merge local and cloud progression."
      } else {
        "Saved locally on this device. Sign in with Google to sync Cloud Data, or export/share your save file below."
      },
    )
  }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier
      .fillMaxWidth()
      .border(1.5.dp, statusDotColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
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
          Icon(
            imageVector = AppIcons.CloudDone,
            contentDescription = null,
            tint = statusDotColor,
            modifier = Modifier.size(20.dp),
          )
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusDotColor))
              Text(
                text = statusLabel,
                style = MaterialTheme.typography.titleMedium,
                color = statusDotColor,
              )
            }
            Text(
              text = statusDetail,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
          onClick = onSyncNow,
          enabled = cloudSyncState !is CloudSyncState.Syncing,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.heightIn(min = 48.dp).testTag("cloud_sync_now_btn"),
        ) {
          Icon(AppIcons.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.sync_now_btn), style = MaterialTheme.typography.labelSmall)
        }
      }

      Text(
        text = "PORTABLE SAVE DATA (BACKUP & SHARE WITH FRIENDS)",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = {
            onRequestSaveExportJson { json ->
              pendingSaveJson = json
              val safeTag = callsign.lowercase().replace(Regex("[^a-z0-9_\\-]"), "_").ifBlank { "operator" }
              saveFileLauncher.launch("unknown_universes_${safeTag}_save.json")
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color(0xFF041016),
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("export_save_file_btn"),
        ) {
          Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(stringResource(R.string.export_save_btn), style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
          onClick = {
            onRequestSaveExportJson { json ->
              val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "The Unknown Universes — $callsign Save Data")
                putExtra(Intent.EXTRA_TEXT, json)
              }
              context.startActivity(Intent.createChooser(sendIntent, "Share Save Data"))
            }
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("share_save_text_btn"),
        ) {
          Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(stringResource(R.string.share_save_btn), style = MaterialTheme.typography.labelMedium)
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = {
            openFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("import_save_file_btn"),
        ) {
          Icon(AppIcons.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(stringResource(R.string.import_save_btn), style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
          onClick = {
            pasteDraft = ""
            showPasteDialog = true
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("paste_save_json_btn"),
        ) {
          Icon(AppIcons.Code, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(stringResource(R.string.paste_save_btn), style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }

  if (showPasteDialog) {
    AlertDialog(
      onDismissRequest = { showPasteDialog = false },
      title = { Text("Paste Shared Save Data JSON") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Paste save JSON exported from The Unknown Universes to preview and load it.",
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = pasteDraft,
            onValueChange = { pasteDraft = it },
            label = { Text("Save JSON") },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth().testTag("paste_save_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val parsed = onParseSaveJson(pasteDraft)
            if (parsed.isSuccess) {
              showPasteDialog = false
              pendingImportPreview = parsed.getOrThrow()
            } else {
              onStatusMessage(parsed.exceptionOrNull()?.localizedMessage ?: "Invalid save JSON")
            }
          },
          modifier = Modifier.testTag("parse_pasted_save_btn"),
        ) { Text("Preview Save") }
      },
      dismissButton = {
        TextButton(onClick = { showPasteDialog = false }) { Text("Cancel") }
      },
    )
  }

  pendingImportPreview?.let { preview ->
    AlertDialog(
      onDismissRequest = { pendingImportPreview = null },
      title = { Text("Import Save: ${preview.callsign}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Source v${preview.sourceAppVersion} • Blue Essence: ${preview.totalEssence} • High Score: ${preview.highScore} • Best Wave: ${preview.highestWave}",
            style = MaterialTheme.typography.bodyMedium,
          )
          val (sealColor, sealText) = when (preview.integrityStatus) {
            SaveIntegrityStatus.VERIFIED -> Color(0xFF00E676) to "SHA-256 INTEGRITY VERIFIED"
            SaveIntegrityStatus.UNSEALED_LEGACY -> Color(0xFFFFAB40) to "LEGACY PORTABLE SAVE (UNSEALED)"
            SaveIntegrityStatus.TAMPERED -> Color(0xFFFF5252) to "WARNING: MODIFIED SAVE SEAL"
          }
          Text(
            text = sealText,
            style = MaterialTheme.typography.labelMedium,
            color = sealColor,
            modifier = Modifier.testTag("save_integrity_badge"),
          )
          Text(
            text = "Includes ${preview.armoryCount} Armory modules, ${preview.rigsCount} Blender Rigs, and ${preview.matchLogsCount} Match Logs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Choose Load & Replace to adopt this exact save, or Merge to keep your highest scores and combine custom Rigs/Modules.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              onConfirmSaveImport(preview, false)
              pendingImportPreview = null
            },
            modifier = Modifier.testTag("confirm_merge_save_btn"),
          ) { Text("Merge") }
          Button(
            onClick = {
              onConfirmSaveImport(preview, true)
              pendingImportPreview = null
            },
            modifier = Modifier.testTag("confirm_replace_save_btn"),
          ) { Text("Load & Replace") }
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingImportPreview = null }) { Text("Cancel") }
      },
    )
  }
}

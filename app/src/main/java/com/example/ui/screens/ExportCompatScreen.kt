package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import com.example.ui.theme.AppIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.CloudSyncState
import com.example.data.local.SaveBundlePreview
import com.example.data.model.ArmoryItem
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.export.PortablePackageGenerator

@Composable
fun ExportCompatScreen(
  profile: OperatorProfile,
  armory: List<ArmoryItem>,
  rigs: List<RigAsset>,
  cloudSyncState: CloudSyncState = CloudSyncState.LocalOnly,
  isCloudSignedIn: Boolean = false,
  onSyncNow: () -> Unit = {},
  onRequestSaveExportJson: ((String) -> Unit) -> Unit = {},
  onParseSaveJson: (String) -> Result<SaveBundlePreview> = { Result.failure(IllegalStateException("Unavailable")) },
  onConfirmSaveImport: (SaveBundlePreview, Boolean) -> Unit = { _, _ -> },
  onSelectConsolePreset: (String) -> Unit,
  onStatusMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  val activeRig = rigs.firstOrNull { it.id == profile.activeRigId } ?: rigs.firstOrNull()
  val activeConsole = UniverseCatalog.consoleById(profile.consolePreset)

  var previewKind by remember { mutableStateOf("html") }
  val previewText = remember(profile, armory, activeRig, previewKind) {
    when (previewKind) {
      "py" ->
        if (activeRig != null) PortablePackageGenerator.generateBlenderRigScript(activeRig)
        else "# Create a rig in ForgeRig Studio first"
      "json" -> PortablePackageGenerator.generatePortableJsonBundle(profile, armory, rigs)
      else -> PortablePackageGenerator.generatePlayableHtmlPackage(profile, armory, activeRig)
    }
  }

  var pendingFileContent by remember { mutableStateOf("") }
  val createDocumentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
      if (uri != null) {
        try {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(pendingFileContent.toByteArray(Charsets.UTF_8))
          }
          onStatusMessage("Exported package saved successfully!")
        } catch (e: Exception) {
          onStatusMessage("Export failed: ${e.localizedMessage}")
        }
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("CROSS-PLATFORM PORTABLE EXPORTER", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
          Text(
            text = "Export self-contained HTML5 playable builds (works in iOS Safari, Xbox Edge, PlayStation, Switch & PC browsers without store accounts or devkits), plus Blender .py rig scripts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Android Package ID: ${BuildConfig.APPLICATION_ID} (v${BuildConfig.VERSION_NAME})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
          )
        }
      }
    }

    item {
      CloudSyncAndSaveCard(
        callsign = profile.callsign,
        cloudSyncState = cloudSyncState,
        isCloudSignedIn = isCloudSignedIn,
        onSyncNow = onSyncNow,
        onRequestSaveExportJson = onRequestSaveExportJson,
        onParseSaveJson = onParseSaveJson,
        onConfirmSaveImport = onConfirmSaveImport,
        onStatusMessage = onStatusMessage,
      )
    }

    // Console / iOS / PC Target Presets
    item {
      Text("TARGET CONTROLLER & BROWSER PROFILE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
      Spacer(Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UniverseCatalog.consoleTargets.forEach { target ->
          val selected = target.id == profile.consolePreset
          Surface(
            onClick = { onSelectConsolePreset(target.id) },
            shape = RoundedCornerShape(14.dp),
            color = if (selected) Color(target.accentHex).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
              .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Color(target.accentHex) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
              )
              .testTag("console_preset_${target.id}"),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(AppIcons.Gamepad, contentDescription = null, tint = Color(target.accentHex), modifier = Modifier.size(18.dp))
                  Text(target.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(target.platformFamily, style = MaterialTheme.typography.labelSmall, color = Color(target.accentHex))
              }
              Text(target.browserEngineNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                target.buttonLegend.forEach { key ->
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                  ) {
                    Text(
                      text = key,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Download Actions
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "DOWNLOAD PORTABLE PACKAGES (${activeConsole.label})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Button(
            onClick = {
              pendingFileContent = PortablePackageGenerator.generatePlayableHtmlPackage(profile, armory, activeRig)
              createDocumentLauncher.launch("unknown_universes_portable.html")
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("download_html_btn"),
          ) {
            Icon(AppIcons.Web, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.export_html_btn), style = MaterialTheme.typography.labelLarge)
          }

          OutlinedButton(
            onClick = {
              if (activeRig != null) {
                pendingFileContent = PortablePackageGenerator.generateBlenderRigScript(activeRig)
                createDocumentLauncher.launch("${activeRig.archetype}_rig_blender.py")
              } else {
                onStatusMessage("Open ForgeRig Studio first to select a rig.")
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("download_blender_py_btn"),
          ) {
            Icon(AppIcons.Code, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.export_blender_btn), style = MaterialTheme.typography.labelLarge)
          }

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = {
                pendingFileContent = PortablePackageGenerator.generatePortableJsonBundle(profile, armory, rigs)
                createDocumentLauncher.launch("unknown_universes_bundle.json")
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("download_json_btn"),
            ) {
              Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Save .JSON", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
              onClick = {
                val shareText = PortablePackageGenerator.generatePortableJsonBundle(profile, armory, rigs)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_SUBJECT, "The Unknown Universes — Portable Bundle")
                  putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Send to iOS / Console / PC"))
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("share_bundle_btn"),
            ) {
              Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Share Bundle", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    }

    // Live Source Code Preview
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF05070D),
        modifier = Modifier.fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("html" to "HTML5 Build", "py" to "Blender .PY", "json" to "Manifest .JSON").forEach { (k, label) ->
              OutlinedButton(
                onClick = { previewKind = k },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = if (previewKind == k) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                ),
              ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
          Text(
            text = previewText.take(1400) + if (previewText.length > 1400) "\n... (${previewText.length} chars total)" else "",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8BE9FD),
          )
        }
      }
    }
  }
}

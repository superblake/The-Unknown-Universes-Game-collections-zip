package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import com.example.ui.theme.AppIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.export.Bo3BoneMappingMode
import com.example.export.Bo3ExportOptions
import com.example.export.Bo3ModToolsExporter
import com.example.export.PortablePackageGenerator
import com.example.export.Prisma3dExportFormat
import com.example.export.Prisma3dExportOptions
import com.example.export.Prisma3dExporter

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
  progression: com.example.data.model.OperatorProgressionState = com.example.data.model.OperatorProgressionState(),
  onConfirmWorkshopImport: (com.example.export.SteamWorkshopImportPreview, Boolean, Boolean) -> Unit = { _, _, _ -> },
) {
  val context = LocalContext.current
  val activeRig = rigs.firstOrNull { it.id == profile.activeRigId } ?: rigs.firstOrNull()
  val activeConsole = UniverseCatalog.consoleById(profile.consolePreset)

  var graphicsTier by remember { mutableStateOf("medium") }
  var bo3BoneMode by remember { mutableStateOf(Bo3BoneMappingMode.TREYARCH_CHARACTER) }
  var bo3UseInches by remember { mutableStateOf(true) }
  var bo3Fps by remember { mutableStateOf(30) }
  var bo3IncludeNotetracks by remember { mutableStateOf(true) }
  val bo3Options = remember(bo3BoneMode, bo3UseInches, bo3Fps, bo3IncludeNotetracks) {
    Bo3ExportOptions(
      boneMode = bo3BoneMode,
      useBo3Inches = bo3UseInches,
      fps = bo3Fps,
      includeNotetracks = bo3IncludeNotetracks,
    )
  }

  var previewKind by remember { mutableStateOf("html") }
  val previewText = remember(profile, armory, activeRig, progression, graphicsTier, previewKind, bo3Options) {
    when (previewKind) {
      "py" ->
        if (activeRig != null) PortablePackageGenerator.generateBlenderRigScript(activeRig)
        else "# Create a rig in ForgeRig Studio first"
      "xmodel" ->
        if (activeRig != null) Bo3ModToolsExporter.generateXModelExport(activeRig, bo3Options)
        else "// Create a rig in ForgeRig Studio first"
      "xanim" ->
        if (activeRig != null) Bo3ModToolsExporter.generateXAnimExport(activeRig, bo3Options)
        else "// Create a rig in ForgeRig Studio first"
      "gdt" ->
        if (activeRig != null) Bo3ModToolsExporter.generateGdtPackage(profile, armory, activeRig, bo3Options)
        else "// Create a rig in ForgeRig Studio first"
      "bo3_py" ->
        if (activeRig != null) Bo3ModToolsExporter.generateCoDBlenderBo3Script(activeRig, bo3Options)
        else "# Create a rig in ForgeRig Studio first"
      "prisma" ->
        if (activeRig != null) Prisma3dExporter.generatePrismaSceneJson(activeRig)
        else "// Create a rig in ForgeRig Studio first"
      "prisma_obj" ->
        if (activeRig != null) Prisma3dExporter.generateWavefrontObj(activeRig)
        else "// Create a rig in ForgeRig Studio first"
      "console" -> PortablePackageGenerator.generateConsolePackageManifest(profile, progression, profile.consolePreset, graphicsTier)
      "xbox_xml" -> PortablePackageGenerator.generateConsolePackageManifest(profile, progression, "xbox_series", graphicsTier)
      "pc_hta" -> PortablePackageGenerator.generatePcExecutableHta(profile, armory, activeRig, progression, graphicsTier)
      "json" -> PortablePackageGenerator.generatePortableJsonBundle(profile, armory, rigs)
      else -> PortablePackageGenerator.generatePlayableHtmlPackage(profile, armory, activeRig, progression, graphicsTier, profile.consolePreset)
    }
  }

  var pendingFileContent by remember { mutableStateOf("") }
  var pendingFileBytes by remember { mutableStateOf<ByteArray?>(null) }
  val launchCreateDoc = com.example.util.rememberSafeCreateDocumentLauncher(
    mimeType = "*/*",
    onUnavailable = {
      pendingFileBytes = null
      onStatusMessage("File saver unavailable on this device.")
    },
  ) { uri ->
    if (uri != null) {
      try {
        val data = pendingFileBytes ?: pendingFileContent.toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(data)
        }
        pendingFileBytes = null
        onStatusMessage("Exported package saved successfully!")
      } catch (t: Throwable) {
        pendingFileBytes = null
        onStatusMessage("Export failed: ${t.localizedMessage ?: "Write error"}")
      }
    } else {
      pendingFileBytes = null
    }
  }

  val importPrisma3dDocLauncher = com.example.util.rememberSafeOpenDocumentLauncher(
    onUnavailable = {
      onStatusMessage("File picker unavailable on this device.")
    },
  ) { uri ->
    if (uri != null) {
      try {
        val fileName = com.example.util.resolveUriDisplayName(context, uri, "imported.p3d")
        val maxBytes = 12 * 1024 * 1024 + 1
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
          val buffer = ByteArray(16 * 1024)
          val baos = java.io.ByteArrayOutputStream()
          var total = 0
          while (total < maxBytes) {
            val read = input.read(buffer, 0, minOf(buffer.size, maxBytes - total))
            if (read <= 0) break
            baos.write(buffer, 0, read)
            total += read
          }
          baos.toByteArray()
        } ?: ByteArray(0)
        val res = com.example.export.Prisma3dImporter.importFromBytes(
          bytes = bytes,
          fileName = fileName,
          targetUserId = profile.userId,
        )
        if (res.isSuccess) {
          val imported = res.getOrThrow()
          onConfirmWorkshopImport(imported.toWorkshopPreview(profile.userId), true, true)
          onStatusMessage(imported.summary)
        } else {
          onStatusMessage(res.exceptionOrNull()?.localizedMessage ?: "Invalid Prisma3D file")
        }
      } catch (t: Throwable) {
        onStatusMessage("Prisma3D import failed: ${t.localizedMessage ?: "Read error"}")
      }
    }
  }

  fun launchImportDoc() {
    importPrisma3dDocLauncher(arrayOf("*/*"))
  }

  fun shareBundleSafe(subject: String, chooserTitle: String, text: String) {
    try {
      if (text.length > 200_000) {
        onStatusMessage("Bundle too large to share inline (${text.length / 1024} KB). Use Save .JSON instead.")
        return
      }
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
      }
      context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
    } catch (t: Throwable) {
      onStatusMessage("Share unavailable: ${t.localizedMessage ?: "Unsupported"}")
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
      .testTag("export_compat_list"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
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

    // Console / iOS / PC Target Presets
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

    // Download Actions
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "DOWNLOAD PORTABLE & CONSOLE PACKAGES (${activeConsole.label})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("low" to "GFX: Low (60fps)", "medium" to "GFX: Balanced", "high" to "GFX: High Shadows").forEach { (tier, lbl) ->
              FilterChip(
                selected = graphicsTier == tier,
                onClick = { graphicsTier = tier },
                label = { Text(lbl) },
                modifier = Modifier.testTag("export_gfx_$tier"),
              )
            }
          }
          Button(
            onClick = {
              pendingFileBytes = null
              pendingFileContent = PortablePackageGenerator.generatePlayableHtmlPackage(profile, armory, activeRig, progression, graphicsTier, profile.consolePreset)
              launchCreateDoc("unknown_universes_portable.html")
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("download_html_btn"),
          ) {
            Icon(AppIcons.Web, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.export_html_btn), style = MaterialTheme.typography.labelLarge)
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = {
                pendingFileBytes = null
                pendingFileContent = PortablePackageGenerator.generatePcExecutableHta(profile, armory, activeRig, progression, graphicsTier)
                previewKind = "pc_hta"
                launchCreateDoc("UnknownUniverses_PC_Launcher.hta")
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(44.dp).testTag("download_pc_hta_btn"),
            ) {
              Text("PC App (.HTA)", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = {
                pendingFileBytes = null
                pendingFileContent = PortablePackageGenerator.generateConsolePackageManifest(profile, progression, "xbox_series", graphicsTier)
                previewKind = "xbox_xml"
                launchCreateDoc("AppxManifest_Xbox.xml")
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(44.dp).testTag("download_xbox_xml_btn"),
            ) {
              Text("Xbox (.XML)", style = MaterialTheme.typography.labelSmall)
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = {
                pendingFileBytes = null
                pendingFileContent = PortablePackageGenerator.generateConsolePackageManifest(profile, progression, "ps5_dualsense", graphicsTier)
                previewKind = "console"
                launchCreateDoc("UnknownUniverses_PlayStation.pspkg.json")
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(44.dp).testTag("download_ps_pkg_btn"),
            ) {
              Text("PlayStation", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = {
                pendingFileBytes = null
                pendingFileContent = PortablePackageGenerator.generateConsolePackageManifest(profile, progression, "switch_handheld", graphicsTier)
                previewKind = "console"
                launchCreateDoc("UnknownUniverses_Nintendo.nsp.json")
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(44.dp).testTag("download_switch_pkg_btn"),
            ) {
              Text("Nintendo", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = {
                pendingFileBytes = null
                pendingFileContent = PortablePackageGenerator.generatePlayableHtmlPackage(profile, armory, activeRig, progression, graphicsTier, "ios_safari")
                previewKind = "html"
                launchCreateDoc("UnknownUniverses_iOS_PWA.html")
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(44.dp).testTag("download_ios_pwa_btn"),
            ) {
              Text("iOS PWA", style = MaterialTheme.typography.labelSmall)
            }
          }

          OutlinedButton(
            onClick = {
              if (activeRig != null) {
                pendingFileBytes = null
                pendingFileContent = PortablePackageGenerator.generateBlenderRigScript(activeRig)
                launchCreateDoc("${activeRig.archetype}_rig_blender.py")
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

          OutlinedButton(
            onClick = { launchImportDoc() },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_tab_import_prisma3d_btn"),
          ) {
            Icon(AppIcons.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Import Prisma3D (.p3d / .prisma / .obj / .glb)", style = MaterialTheme.typography.labelLarge)
          }

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = {
                pendingFileBytes = null
                pendingFileContent = PortablePackageGenerator.generatePortableJsonBundle(profile, armory, rigs)
                launchCreateDoc("unknown_universes_bundle.json")
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
                shareBundleSafe(
                  subject = "The Unknown Universes — Portable Bundle",
                  chooserTitle = "Send to iOS / Console / PC",
                  text = shareText,
                )
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

    // Prisma3D 3D Scene & Rig Exporter (Android / iOS / Desktop)
      val prismaSlug = activeRig?.let { Prisma3dExporter.sanitizeFileName(it.name) } ?: "prisma3d_rig"
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.55f), RoundedCornerShape(18.dp))
          .testTag("prisma3d_export_card"),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "PRISMA3D 3D SCENE & RIG EXPORTER",
              style = MaterialTheme.typography.titleMedium,
              color = Color(0xFF00E5FF),
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFF00E5FF).copy(alpha = 0.16f),
            ) {
              Text(
                text = ".PRISMA / .P3D / .OBJ / .GLTF",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00E5FF),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          Text(
            text = "Export ${activeRig?.name ?: "your active ForgeRig"} to Prisma3D on Android & iOS — native scene projects (.prisma), complete 3D model packages (.p3d), 3D Wavefront meshes with materials (.obj + .mtl), or glTF 2.0.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Button(
            onClick = {
              if (activeRig != null) {
                pendingFileBytes = null
                pendingFileContent = Prisma3dExporter.generatePrismaSceneJson(activeRig)
                previewKind = "prisma"
                launchCreateDoc("${prismaSlug}.prisma")
              } else {
                onStatusMessage("Open ForgeRig Studio first to select a rig.")
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("download_prisma_scene_btn"),
          ) {
            Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF00363A))
            Spacer(Modifier.width(8.dp))
            Text("Export Prisma3D Scene (.PRISMA)", style = MaterialTheme.typography.labelLarge, color = Color(0xFF00363A))
          }

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = {
                if (activeRig != null) {
                  pendingFileBytes = Prisma3dExporter.generatePrisma3dZipBundle(activeRig)
                  launchCreateDoc("${prismaSlug}_bundle.p3d")
                } else {
                  onStatusMessage("Open ForgeRig Studio first to select a rig.")
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("download_prisma_bundle_btn"),
            ) {
              Icon(AppIcons.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Package (.P3D)", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
              onClick = {
                if (activeRig != null) {
                  pendingFileBytes = null
                  pendingFileContent = Prisma3dExporter.generateWavefrontObj(activeRig)
                  previewKind = "prisma_obj"
                  launchCreateDoc("${prismaSlug}_mesh.obj")
                } else {
                  onStatusMessage("Open ForgeRig Studio first to select a rig.")
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("download_prisma_obj_btn"),
            ) {
              Icon(AppIcons.Code, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("3D Mesh (.OBJ)", style = MaterialTheme.typography.labelLarge)
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = {
                if (activeRig != null) {
                  pendingFileBytes = null
                  pendingFileContent = Prisma3dExporter.generateGlTFSceneJson(activeRig)
                  launchCreateDoc("${prismaSlug}_scene.gltf")
                } else {
                  onStatusMessage("Open ForgeRig Studio first to select a rig.")
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("download_prisma_gltf_btn"),
            ) {
              Icon(AppIcons.Animation, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("glTF 2.0 (.GLTF)", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
              onClick = { launchImportDoc() },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("export_tab_import_prisma3d_card_btn"),
            ) {
              Icon(AppIcons.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Import P3D", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }

    // Call of Duty: Black Ops 3 Mod Tools Exporter
      val rigSlug = activeRig?.let { Bo3ModToolsExporter.sanitizeAssetSlug(it.name) } ?: "unknown_rig"
      val clipSlug = activeRig?.let { Bo3ModToolsExporter.sanitizeAssetSlug(it.activeClip, "idle") } ?: "idle"
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
          .testTag("bo3_mod_tools_card"),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "BO3 MOD TOOLS EXPORTER (APE / RADIANT)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
          )
          Text(
            text = "Export real Treyarch Black Ops 3 Mod Tools assets from ${activeRig?.name ?: "your active ForgeRig"} — ASCII .XMODEL_EXPORT (v6), .XANIM_EXPORT (v3), APE .GDT asset entries, and CoD-Blender scripts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Text("SKELETON BONE MAPPING", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Bo3BoneMappingMode.entries.forEach { mode ->
              FilterChip(
                selected = bo3BoneMode == mode,
                onClick = { bo3BoneMode = mode },
                label = { Text(mode.label) },
                modifier = Modifier.testTag("bo3_bone_mode_${mode.id}"),
              )
            }
          }
          Text(
            text = bo3BoneMode.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = bo3UseInches,
              onClick = { bo3UseInches = true },
              label = { Text("BO3 Inches (39.37x)") },
              modifier = Modifier.testTag("bo3_scale_inches_chip"),
            )
            FilterChip(
              selected = !bo3UseInches,
              onClick = { bo3UseInches = false },
              label = { Text("Meters (1.0x)") },
              modifier = Modifier.testTag("bo3_scale_meters_chip"),
            )
            FilterChip(
              selected = bo3Fps == 30,
              onClick = { bo3Fps = 30 },
              label = { Text("30 FPS") },
              modifier = Modifier.testTag("bo3_fps_30_chip"),
            )
            FilterChip(
              selected = bo3Fps == 60,
              onClick = { bo3Fps = 60 },
              label = { Text("60 FPS") },
              modifier = Modifier.testTag("bo3_fps_60_chip"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Include XANIM Notetracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Switch(
              checked = bo3IncludeNotetracks,
              onCheckedChange = { bo3IncludeNotetracks = it },
              modifier = Modifier.testTag("bo3_notetracks_switch"),
            )
          }

          Button(
            onClick = {
              if (activeRig != null) {
                pendingFileBytes = null
                pendingFileContent = Bo3ModToolsExporter.generateXModelExport(activeRig, bo3Options)
                previewKind = "xmodel"
                launchCreateDoc("${rigSlug}_rig.xmodel_export")
              } else {
                onStatusMessage("Open ForgeRig Studio first to select a rig.")
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("download_bo3_xmodel_btn"),
          ) {
            Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.export_bo3_xmodel_btn), style = MaterialTheme.typography.labelLarge)
          }

          OutlinedButton(
            onClick = {
              if (activeRig != null) {
                pendingFileBytes = null
                pendingFileContent = Bo3ModToolsExporter.generateXAnimExport(activeRig, bo3Options)
                previewKind = "xanim"
                launchCreateDoc("${rigSlug}_${clipSlug}.xanim_export")
              } else {
                onStatusMessage("Open ForgeRig Studio first to select a rig.")
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("download_bo3_xanim_btn"),
          ) {
            Icon(AppIcons.Animation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.export_bo3_xanim_btn), style = MaterialTheme.typography.labelLarge)
          }

          OutlinedButton(
            onClick = {
              if (activeRig != null) {
                pendingFileBytes = null
                pendingFileContent = Bo3ModToolsExporter.generateGdtPackage(profile, armory, activeRig, bo3Options)
                previewKind = "gdt"
                launchCreateDoc("${rigSlug}_bo3_assets.gdt")
              } else {
                onStatusMessage("Open ForgeRig Studio first to select a rig.")
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("download_bo3_gdt_btn"),
          ) {
            Icon(AppIcons.Code, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.export_bo3_gdt_btn), style = MaterialTheme.typography.labelLarge)
          }

          OutlinedButton(
            onClick = {
              if (activeRig != null) {
                pendingFileBytes = null
                pendingFileContent = Bo3ModToolsExporter.generateCoDBlenderBo3Script(activeRig, bo3Options)
                previewKind = "bo3_py"
                launchCreateDoc("${rigSlug}_bo3_blender.py")
              } else {
                onStatusMessage("Open ForgeRig Studio first to select a rig.")
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("download_bo3_blender_btn"),
          ) {
            Icon(AppIcons.Code, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.export_bo3_blender_btn), style = MaterialTheme.typography.labelLarge)
          }
        }
      }

    // Live Source Code Preview
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF05070D),
        modifier = Modifier.fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
          .testTag("preview_code_card"),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf(
              "html" to "HTML5 Build",
              "console" to "Console Spec",
              "xbox_xml" to "Xbox AppX XML",
              "pc_hta" to "PC .HTA App",
              "py" to "Blender .PY",
              "prisma" to "Prisma3D .PRISMA",
              "prisma_obj" to "Prisma3D .OBJ",
              "xmodel" to "BO3 .XMODEL",
              "xanim" to "BO3 .XANIM",
              "gdt" to "BO3 .GDT",
              "bo3_py" to "CoD-Blender .PY",
              "json" to "Manifest .JSON",
            ).forEach { (k, label) ->
              OutlinedButton(
                onClick = { previewKind = k },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = if (previewKind == k) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                ),
                modifier = Modifier.testTag("preview_tab_$k"),
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

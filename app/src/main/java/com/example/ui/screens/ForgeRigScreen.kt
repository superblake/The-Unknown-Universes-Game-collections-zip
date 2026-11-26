package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import com.example.ui.theme.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AppSettings
import com.example.data.local.GraphicsQuality
import com.example.data.local.RamGraphicsMode
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.export.Bo3BoneMappingMode
import com.example.export.Bo3ExportOptions
import com.example.export.Bo3ModToolsExporter
import com.example.export.PortablePackageGenerator
import com.example.export.Prisma3dExportFormat
import com.example.export.Prisma3dExportOptions
import com.example.export.Prisma3dExporter
import com.example.export.Prisma3dImporter
import com.example.ui.rig.RigEvaluator
import com.example.ui.rig.drawRigCharacter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun ForgeRigScreen(
  rigs: List<RigAsset>,
  activeRigId: String,
  onSelectRig: (String) -> Unit,
  onSaveRig: (
    rigId: String?,
    name: String,
    archetype: String,
    activeClip: String,
    ikEnabled: Boolean,
    rigScale: Double,
    playbackSpeed: Double,
    poseAngles: Map<String, Double>,
    keyframeFrames: List<Long>,
    isCompleted: Boolean,
  ) -> Unit,
  settings: AppSettings = AppSettings(),
  progression: OperatorProgressionState = OperatorProgressionState(),
  onStatusMessage: (String) -> Unit = {},
  onOpenForgeWeapon: (() -> Unit)? = null,
  onOpenForgeMap: (() -> Unit)? = null,
  onOpenForgeModule: (() -> Unit)? = null,
  onOpenForgePerk: (() -> Unit)? = null,
  onOpenForgeGear: (() -> Unit)? = null,
  onOpenForgeEquipment: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val currentRig = rigs.firstOrNull { it.id == activeRigId } ?: rigs.firstOrNull()

  var rigName by remember(currentRig?.id) { mutableStateOf(currentRig?.name ?: "Custom Rig") }
  var archetype by remember(currentRig?.id) { mutableStateOf(currentRig?.archetype ?: "operator") }
  var activeClip by remember(currentRig?.id) { mutableStateOf(currentRig?.activeClip ?: "idle") }
  var ikEnabled by remember(currentRig?.id) { mutableStateOf(currentRig?.ikEnabled ?: true) }
  var rigScale by remember(currentRig?.id) {
    val s = (currentRig?.rigScale ?: 1.0).toFloat()
    mutableFloatStateOf(if (s.isFinite()) s.coerceIn(0.7f, 1.4f) else 1.0f)
  }
  var playbackSpeed by remember(currentRig?.id) {
    val sp = (currentRig?.playbackSpeed ?: 1.0).toFloat()
    mutableFloatStateOf(if (sp.isFinite()) sp.coerceIn(0.3f, 2.5f) else 1.0f)
  }
  var keyframes by remember(currentRig?.id) {
    mutableStateOf(currentRig?.keyframeFrames?.filter { it in 0L..60L }?.ifEmpty { null } ?: listOf(0L, 15L, 30L, 45L, 60L))
  }
  val poseMap = remember(currentRig?.id) {
    mutableStateMapOf<String, Double>().apply {
      putAll(UniverseCatalog.defaultPoseMap())
      currentRig?.poseAngles?.forEach { (k, v) ->
        if (v.isFinite()) {
          put(k, v)
        }
      }
    }
  }

  var selectedBoneId by remember { mutableStateOf("rUpperArm") }
  var isPlaying by remember(settings.reduceMotion) { mutableStateOf(!settings.reduceMotion) }
  var currentFrame by remember { mutableFloatStateOf(0f) }
  var showBonesOverlay by remember { mutableStateOf(true) }
  var showNewRigDialog by remember { mutableStateOf(false) }
  var showRigExportDialog by remember { mutableStateOf(false) }
  var showPrisma3dExportDialog by remember { mutableStateOf(false) }
  var showPrisma3dImportDialog by remember { mutableStateOf(false) }
  var bo3ExportMode by remember { mutableStateOf(Bo3BoneMappingMode.TREYARCH_CHARACTER) }
  var pendingRigExportText by remember { mutableStateOf("") }
  var pendingRigExportBytes by remember { mutableStateOf<ByteArray?>(null) }

  fun shareRigTextSafe(subject: String, chooserTitle: String, text: String) {
    try {
      if (text.length > 200_000) {
        onStatusMessage("File is too large for quick share (${text.length / 1024} KB). Use Export to save directly.")
        return
      }
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
      }
      context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    } catch (t: Throwable) {
      onStatusMessage("Share not available: ${t.localizedMessage ?: "Unsupported"}")
    }
  }

  val launchRigSaveDoc = com.example.util.rememberSafeCreateDocumentLauncher(
    mimeType = "*/*",
    onUnavailable = {
      pendingRigExportBytes = null
      onStatusMessage("File picker unavailable on this device.")
    },
  ) { uri ->
    if (uri != null) {
      try {
        val data = pendingRigExportBytes ?: pendingRigExportText.toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(data)
        }
        pendingRigExportBytes = null
        onStatusMessage("Rig export saved successfully!")
      } catch (t: Throwable) {
        pendingRigExportBytes = null
        onStatusMessage("Rig export failed: ${t.localizedMessage ?: "Write error"}")
      }
    } else {
      pendingRigExportBytes = null
    }
  }

  val prisma3dImportLauncher = com.example.util.rememberSafeOpenDocumentLauncher(
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
        val res = Prisma3dImporter.importFromBytes(
          bytes = bytes,
          fileName = fileName,
          targetUserId = currentRig?.userId ?: "local_operator",
        )
        if (res.isSuccess) {
          val imported = res.getOrThrow()
          val rig = imported.rig
          onSaveRig(
            null,
            rig.name,
            rig.archetype,
            rig.activeClip,
            rig.ikEnabled,
            rig.rigScale,
            rig.playbackSpeed,
            rig.poseAngles,
            rig.keyframeFrames,
            true,
          )
          showPrisma3dImportDialog = false
          onStatusMessage(imported.summary)
        } else {
          onStatusMessage(res.exceptionOrNull()?.localizedMessage ?: "Invalid Prisma3D file")
        }
      } catch (t: Throwable) {
        onStatusMessage("Prisma3D import error: ${t.localizedMessage ?: "Read error"}")
      }
    }
  }

  val capePathScratch = remember { Path() }
  val torsoPathScratch = remember { Path() }

  LaunchedEffect(isPlaying, playbackSpeed, settings.targetFpsCap) {
    if (!isPlaying) return@LaunchedEffect
    try {
      var lastNanos = withFrameNanos { it }
      var accumNanos = 0L
      val minStepNanos = if (settings.targetFpsCap <= 30) 30_000_000L else 14_000_000L
      val safeSpeed = if (playbackSpeed.isFinite() && playbackSpeed > 0f) playbackSpeed else 1.0f
      while (isPlaying) {
        val now = withFrameNanos { it }
        accumNanos += (now - lastNanos).coerceAtLeast(1_000_000L)
        lastNanos = now
        if (accumNanos >= minStepNanos) {
          val dtSec = (accumNanos / 1_000_000_000f).coerceIn(0.001f, 0.06f)
          accumNanos = 0L
          val nextFrame = (currentFrame + safeSpeed * 54f * dtSec) % 60f
          currentFrame = if (nextFrame.isFinite()) nextFrame else 0f
        }
      }
    } catch (e: Throwable) {
      android.util.Log.e("ForgeRig", "Animation loop interrupted", e)
    }
  }

  val clips = listOf("idle", "sprint", "lunge", "slam", "vault", "inspect")
  val archetypes = listOf("operator", "biomorph", "infernal", "chrono", "starblade")

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("UNKNOWNBLEND // FORGE STUDIO", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          Text("1-Tap Custom Forge Hub & Blender 3D Rig Posing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = { showPrisma3dImportDialog = true },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(48.dp).testTag("import_prisma3d_btn"),
          ) {
            Icon(AppIcons.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Prisma3D", style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = { showNewRigDialog = true },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(48.dp).testTag("new_rig_btn"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("New Rig", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    if (onOpenForgeWeapon != null || onOpenForgeMap != null || onOpenForgePerk != null || onOpenForgeGear != null || onOpenForgeEquipment != null || onOpenForgeModule != null) {
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("QUICK FORGE ANY ITEM (1 TAP)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              OutlinedButton(
                onClick = { showPrisma3dImportDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp).testTag("forge_hub_prisma3d_btn"),
              ) {
                Text("+ Prisma3D", style = MaterialTheme.typography.labelSmall)
              }
              OutlinedButton(
                onClick = { showPrisma3dExportDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp).testTag("forge_hub_export_p3d_btn"),
              ) {
                Text("+ Export P3D", style = MaterialTheme.typography.labelSmall)
              }
              if (onOpenForgeWeapon != null) {
                Button(onClick = onOpenForgeWeapon, shape = RoundedCornerShape(8.dp), modifier = Modifier.height(48.dp).testTag("forge_hub_weapon_btn")) {
                  Text("+ Weapon", style = MaterialTheme.typography.labelSmall)
                }
              }
              if (onOpenForgeMap != null) {
                Button(onClick = onOpenForgeMap, shape = RoundedCornerShape(8.dp), modifier = Modifier.height(48.dp).testTag("forge_hub_map_btn")) {
                  Text("+ Map", style = MaterialTheme.typography.labelSmall)
                }
              }
              if (onOpenForgePerk != null) {
                OutlinedButton(onClick = onOpenForgePerk, shape = RoundedCornerShape(8.dp), modifier = Modifier.height(48.dp).testTag("forge_hub_perk_btn")) {
                  Text("+ Perk", style = MaterialTheme.typography.labelSmall)
                }
              }
              if (onOpenForgeGear != null) {
                OutlinedButton(onClick = onOpenForgeGear, shape = RoundedCornerShape(8.dp), modifier = Modifier.height(48.dp).testTag("forge_hub_gear_btn")) {
                  Text("+ Gear", style = MaterialTheme.typography.labelSmall)
                }
              }
              if (onOpenForgeEquipment != null) {
                OutlinedButton(onClick = onOpenForgeEquipment, shape = RoundedCornerShape(8.dp), modifier = Modifier.height(48.dp).testTag("forge_hub_equipment_btn")) {
                  Text("+ Equipment", style = MaterialTheme.typography.labelSmall)
                }
              }
              if (onOpenForgeModule != null) {
                OutlinedButton(onClick = onOpenForgeModule, shape = RoundedCornerShape(8.dp), modifier = Modifier.height(48.dp).testTag("forge_hub_module_btn")) {
                  Text("+ Module", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }
    }

    // Rig Library Switcher
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rigs.distinctBy { it.id }, key = { it.id }) { rig ->
          FilterChip(
            selected = rig.id == currentRig?.id,
            onClick = { onSelectRig(rig.id) },
            label = { Text(rig.name) },
            modifier = Modifier.height(48.dp).testTag("rig_chip_${rig.id}"),
          )
        }
      }
    }

    // Interactive Blender-style Viewport Canvas
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1A1D24),
        modifier = Modifier.fillMaxWidth()
          .height(310.dp)
          .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(currentRig?.id, rigScale, ikEnabled, activeClip) {
              detectDragGestures(
                onDragStart = { start ->
                  try {
                    if (size.width > 0 && size.height > 0 && start.x.isFinite() && start.y.isFinite()) {
                      val origin = Offset(size.width * 0.5f, size.height * 0.56f)
                      val safeScale = (size.height * 0.42f * (if (rigScale.isFinite()) rigScale else 1f)).takeIf { it.isFinite() && it > 0f } ?: 120f
                      val safeCurFrame = if (currentFrame.isFinite()) currentFrame else 0f
                      val skel = RigEvaluator.evaluate(
                        origin = origin,
                        scalePx = safeScale,
                        poseAngles = poseMap,
                        clip = activeClip,
                        frame = safeCurFrame,
                        ikEnabled = ikEnabled,
                      )
                      val nearest = skel.handleMap().minByOrNull { (_, pt) ->
                        if (pt.x.isFinite() && pt.y.isFinite()) hypot(pt.x - start.x, pt.y - start.y) else Float.MAX_VALUE
                      }
                      if (nearest != null) {
                        selectedBoneId = nearest.key
                        isPlaying = false
                      }
                    }
                  } catch (t: Throwable) {
                    android.util.Log.e("ForgeRig", "onDragStart error", t)
                  }
                },
                onDrag = { change, _ ->
                  try {
                    change.consume()
                    if (size.width > 0 && size.height > 0 && change.position.x.isFinite() && change.position.y.isFinite()) {
                      val origin = Offset(size.width * 0.5f, size.height * 0.56f)
                      val safeScale = (size.height * 0.42f * (if (rigScale.isFinite()) rigScale else 1f)).takeIf { it.isFinite() && it > 0f } ?: 120f
                      val safeCurFrame = if (currentFrame.isFinite()) currentFrame else 0f
                      val skel = RigEvaluator.evaluate(
                        origin = origin,
                        scalePx = safeScale,
                        poseAngles = poseMap,
                        clip = activeClip,
                        frame = safeCurFrame,
                        ikEnabled = ikEnabled,
                      )
                      val pivot = when (selectedBoneId) {
                        "torso" -> skel.pelvis
                        "head" -> skel.chest
                        "lUpperArm" -> skel.lShoulder
                        "lForearm" -> skel.lElbow
                        "rUpperArm" -> skel.rShoulder
                        "rForearm" -> skel.rElbow
                        "lThigh" -> skel.lHip
                        "lShin" -> skel.lKnee
                        "rThigh" -> skel.rHip
                        "rShin" -> skel.rKnee
                        else -> skel.rHand
                      }
                      if (pivot.x.isFinite() && pivot.y.isFinite()) {
                        val dx = change.position.x - pivot.x
                        val dy = change.position.y - pivot.y
                        if (dx != 0f || dy != 0f) {
                          val rawDeg = atan2(dy, dx) * (180f / PI.toFloat())
                          if (rawDeg.isFinite()) {
                            val spec = UniverseCatalog.rigBones.firstOrNull { it.id == selectedBoneId }
                            val clamped = if (spec != null) {
                              rawDeg.coerceIn(spec.minAngle, spec.maxAngle)
                            } else {
                              rawDeg.coerceIn(-120f, 120f)
                            }
                            if (clamped.isFinite()) {
                              poseMap[selectedBoneId] = clamped.toDouble()
                            }
                          }
                        }
                      }
                    }
                  } catch (t: Throwable) {
                    android.util.Log.e("ForgeRig", "onDrag error", t)
                  }
                },
              )
            }
          ) {
            try {
              val w = size.width
              val h = size.height
              if (w <= 0f || h <= 0f || !w.isFinite() || !h.isFinite()) return@Canvas
              val horizonY = h * 0.34f
              val vanishX = w * 0.5f
              val safeRigScale = if (rigScale.isFinite()) rigScale.coerceIn(0.7f, 1.4f) else 1.0f
              val groundY = h * 0.56f + (h * 0.42f * safeRigScale) * 0.56f

              val ramRange = when (settings.ramGraphicsMode) {
                RamGraphicsMode.LOW_RAM_BOOST -> 4
                RamGraphicsMode.BALANCED -> 6
                RamGraphicsMode.HIGH_RAM_DETAIL -> 8
                RamGraphicsMode.MAX_RAM_ULTRA -> 10
              }
              val ramRows = when (settings.ramGraphicsMode) {
                RamGraphicsMode.LOW_RAM_BOOST -> 4
                RamGraphicsMode.BALANCED -> 6
                RamGraphicsMode.HIGH_RAM_DETAIL -> 8
                RamGraphicsMode.MAX_RAM_ULTRA -> 10
              }
              // Blender 3D perspective floor grid (scaled by RAM graphics mode)
              for (i in -ramRange..ramRange) {
                val bx = vanishX + i * (w * 0.085f)
                val tx = vanishX + i * (w * 0.025f)
                if (bx.isFinite() && tx.isFinite() && horizonY.isFinite()) {
                  drawLine(Color(0xFF282E3A), Offset(tx, horizonY), Offset(bx, h), strokeWidth = 1f)
                }
              }
              for (r in 0..ramRows) {
                val t = r / ramRows.toFloat()
                val ry = horizonY + (h - horizonY) * (t * t)
                if (ry.isFinite()) {
                  drawLine(Color(0xFF282E3A), Offset(0f, ry), Offset(w, ry), strokeWidth = 1f)
                }
              }
              // Blender World Axes (X Crimson, Y Green, Z Blue)
              if (groundY.isFinite() && vanishX.isFinite() && horizonY.isFinite()) {
                drawLine(Color(0xAAFF4060), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 2f)
                drawLine(Color(0x8838E54D), Offset(vanishX, horizonY), Offset(vanishX, h), strokeWidth = 1.6f)
                drawLine(Color(0x6640A9FF), Offset(vanishX, 16f), Offset(vanishX, groundY), strokeWidth = 1.4f)
              }

              val origin = Offset(vanishX, h * 0.56f)
              val safeCurFrame = if (currentFrame.isFinite()) currentFrame else 0f
              val skel = RigEvaluator.evaluate(
                origin = origin,
                scalePx = h * 0.42f * safeRigScale,
                poseAngles = poseMap,
                clip = activeClip,
                frame = safeCurFrame,
                ikEnabled = ikEnabled,
              )
              val visorHex = ProgressionCatalog.visorStyles
                .firstOrNull { it.first == progression.characterCustom.visorStyleId }
                ?.second ?: progression.characterCustom.accentArmorHex
              drawRigCharacter(
                skeleton = skel,
                archetype = archetype,
                primaryColor = Color(progression.characterCustom.primaryArmorHex),
                secondaryColor = Color(progression.characterCustom.accentArmorHex),
                showBonesOverlay = showBonesOverlay,
                selectedBoneId = selectedBoneId,
                capePathScratch = capePathScratch,
                torsoPathScratch = torsoPathScratch,
                dynamicGlow = settings.dynamicGlowEffects &&
                  settings.graphicsQuality != GraphicsQuality.LOW &&
                  settings.ramGraphicsMode != RamGraphicsMode.LOW_RAM_BOOST,
                highContrast = settings.effectiveHighContrast,
                cloakOverride = progression.characterCustom.cloakEnabled,
                visorColorOverride = Color(visorHex),
                auraTrailId = progression.characterCustom.auraTrailId,
                outfitId = progression.equippedOutfitId,
              )

              // Blender-style Top-Right XYZ Orientation Gizmo
              if (w >= 72f) {
                val gCenter = Offset(w - 36f, 36f)
                drawCircle(Color(0x6610141D), radius = 22f, center = gCenter)
                drawLine(Color(0xFFFF4060), gCenter, Offset(gCenter.x + 16f, gCenter.y + 4f), strokeWidth = 2.5f)
                drawLine(Color(0xFF38E54D), gCenter, Offset(gCenter.x - 12f, gCenter.y + 9f), strokeWidth = 2.5f)
                drawLine(Color(0xFF40A9FF), gCenter, Offset(gCenter.x, gCenter.y - 16f), strokeWidth = 2.5f)
              }

              // Subtle Dope-Sheet Keyframe Strip at bottom edge
              val barY = h - 10f
              if (w >= 140f && barY.isFinite()) {
                drawLine(Color(0xFF343B4A), Offset(16f, barY), Offset(w - 110f, barY), strokeWidth = 2f)
                keyframes.forEach { kf ->
                  val kfRatio = (kf.toFloat() / 60f).coerceIn(0f, 1f)
                  if (kfRatio.isFinite()) {
                    val kx = 16f + (w - 126f) * kfRatio
                    if (kx.isFinite()) {
                      drawCircle(Color(0xFFFFB300), radius = 4.5f, center = Offset(kx, barY))
                    }
                  }
                }
                val curRatio = (safeCurFrame / 60f).coerceIn(0f, 1f)
                if (curRatio.isFinite()) {
                  val curX = 16f + (w - 126f) * curRatio
                  if (curX.isFinite()) {
                    drawLine(Color(0xFF00E5FF), Offset(curX, barY - 8f), Offset(curX, barY + 6f), strokeWidth = 2.5f)
                  }
                }
              }
            } catch (t: Throwable) {
              android.util.Log.e("ForgeRig", "Canvas draw error", t)
            }
          }

          // Viewport HUD Overlay (isolated recomposition scope via integer frame derivedStateOf)
          Row(
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            val frameInt by remember { derivedStateOf { currentFrame.toInt() } }
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xCC111622)) {
              Text(
                text = "POSE MODE • F$frameInt/60 • ${activeClip.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
            val boneDeg = poseMap[selectedBoneId]?.takeIf { it.isFinite() }?.toInt() ?: 0
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xCC111622)) {
              Text(
                text = "BONE: $selectedBoneId ($boneDeg°)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }

          Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xCC11182A)) {
              IconButton(onClick = { isPlaying = !isPlaying }, modifier = Modifier.testTag("rig_play_pause_btn")) {
                Icon(
                  imageVector = if (isPlaying) AppIcons.Pause else Icons.Filled.PlayArrow,
                  contentDescription = if (isPlaying) "Pause animation" else "Play animation",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xCC11182A)) {
              IconButton(
                onClick = {
                  poseMap.clear()
                  poseMap.putAll(UniverseCatalog.defaultPoseMap())
                },
              ) {
                Icon(AppIcons.RestartAlt, contentDescription = "Reset pose", tint = MaterialTheme.colorScheme.onSurface)
              }
            }
          }
        }
      }
    }

    // Timeline Scrubber & Keyframes
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("ANIMATION TIMELINE & KEYFRAMES", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            OutlinedButton(
              onClick = {
                val f = currentFrame.toInt().toLong()
                if (!keyframes.contains(f)) {
                  keyframes = (keyframes + f).sorted().take(20)
                }
              },
              shape = RoundedCornerShape(8.dp),
            ) {
              Icon(AppIcons.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Key Frame ${currentFrame.toInt()}")
            }
          }
          Slider(
            value = if (currentFrame.isFinite()) currentFrame.coerceIn(0f, 60f) else 0f,
            onValueChange = {
              if (it.isFinite()) {
                isPlaying = false
                currentFrame = it.coerceIn(0f, 60f)
              }
            },
            valueRange = 0f..60f,
            modifier = Modifier.fillMaxWidth().testTag("rig_timeline_slider"),
          )
          Text(
            text = "Keyframes: ${keyframes.joinToString(" • ") { "F$it" }}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
          )
        }
      }
    }

    // Animation Clip & Archetype Controls
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = rigName,
            onValueChange = { rigName = it.take(50) },
            label = { Text("Rig Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("forge_rig_name_input"),
          )

          Text("1-TAP POSE PRESETS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          val posePresets = listOf(
            "Hero Stance" to mapOf("torso" to -84.0, "head" to -90.0, "rUpperArm" to 18.0, "rForearm" to -20.0, "lUpperArm" to 125.0),
            "Combat Aim" to mapOf("torso" to -78.0, "rUpperArm" to -8.0, "rForearm" to -4.0, "lUpperArm" to 22.0, "lForearm" to -6.0),
            "Parkour Sprint" to mapOf("torso" to -68.0, "lThigh" to 132.0, "rThigh" to 52.0, "lUpperArm" to -145.0, "rUpperArm" to 42.0),
            "Real-Life Casual" to UniverseCatalog.defaultPoseMap(),
          )
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            posePresets.forEach { (pLabel, pAngles) ->
              FilterChip(
                selected = false,
                onClick = {
                  poseMap.putAll(UniverseCatalog.defaultPoseMap())
                  poseMap.putAll(pAngles)
                  onStatusMessage("Applied '$pLabel' pose preset")
                },
                label = { Text(pLabel, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(48.dp),
              )
            }
          }

          Text("ANIMATION CLIPS", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            clips.forEach { clip ->
              FilterChip(
                selected = activeClip == clip,
                onClick = { activeClip = clip },
                label = { Text(clip.uppercase()) },
                modifier = Modifier.height(48.dp).testTag("clip_chip_$clip"),
              )
            }
          }

          Text("RIG ARCHETYPE", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            archetypes.forEach { arch ->
              FilterChip(
                selected = archetype == arch,
                onClick = { archetype = arch },
                label = { Text(arch.replaceFirstChar { it.uppercase() }) },
                modifier = Modifier.height(48.dp),
              )
            }
          }

          // Selected Bone Angle Slider
          val boneSpec = UniverseCatalog.rigBones.firstOrNull { it.id == selectedBoneId }
          if (boneSpec != null) {
            val rawVal = poseMap[selectedBoneId]
            val valDeg = (if (rawVal != null && rawVal.isFinite()) rawVal.toFloat() else boneSpec.defaultAngle)
              .coerceIn(boneSpec.minAngle, boneSpec.maxAngle)
            Text(
              text = "${boneSpec.label}: ${valDeg.toInt()}°",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Slider(
              value = valDeg,
              onValueChange = {
                if (it.isFinite()) {
                  poseMap[selectedBoneId] = it.coerceIn(boneSpec.minAngle, boneSpec.maxAngle).toDouble()
                }
              },
              valueRange = boneSpec.minAngle..boneSpec.maxAngle,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("IK Ground Constraint Lock", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = ikEnabled, onCheckedChange = { ikEnabled = it })
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Show Blender Bone Gizmos", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = showBonesOverlay, onCheckedChange = { showBonesOverlay = it })
          }

          val safeSpeedDisp = if (playbackSpeed.isFinite()) playbackSpeed.coerceIn(0.3f, 2.5f) else 1.0f
          Text("Playback Speed: ${String.format(Locale.US, "%.2fx", safeSpeedDisp)}", style = MaterialTheme.typography.labelMedium)
          Slider(
            value = safeSpeedDisp,
            onValueChange = { if (it.isFinite()) playbackSpeed = it.coerceIn(0.3f, 2.5f) },
            valueRange = 0.3f..2.5f,
          )

          val safeScaleDisp = if (rigScale.isFinite()) rigScale.coerceIn(0.7f, 1.4f) else 1.0f
          Text("Rig Scale: ${String.format(Locale.US, "%.2fx", safeScaleDisp)}", style = MaterialTheme.typography.labelMedium)
          Slider(
            value = safeScaleDisp,
            onValueChange = { if (it.isFinite()) rigScale = it.coerceIn(0.7f, 1.4f) },
            valueRange = 0.7f..1.4f,
          )

          Button(
            onClick = {
              try {
                val safeAngles = poseMap.filter { it.value.isFinite() }.toMap()
                val safeFrames = keyframes.filter { it in 0L..60L }.distinct().sorted()
                val safeSc = if (rigScale.isFinite()) rigScale.coerceIn(0.7f, 1.4f).toDouble() else 1.0
                val safeSp = if (playbackSpeed.isFinite()) playbackSpeed.coerceIn(0.3f, 2.5f).toDouble() else 1.0
                onSaveRig(
                  currentRig?.id,
                  rigName.trim().ifBlank { "Custom Rig" },
                  archetype,
                  activeClip,
                  ikEnabled,
                  safeSc,
                  safeSp,
                  safeAngles,
                  safeFrames,
                  true,
                )
              } catch (e: Throwable) {
                onStatusMessage("Failed to save rig: ${e.localizedMessage}")
              }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_rig_button"),
          ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.save_rig_cloud), style = MaterialTheme.typography.labelLarge)
          }

          OutlinedButton(
            onClick = { showRigExportDialog = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("forge_export_bo3_btn"),
          ) {
            Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Export Rig (BO3 Mod Tools / Blender)", style = MaterialTheme.typography.labelLarge)
          }

          OutlinedButton(
            onClick = { showPrisma3dExportDialog = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("forge_export_prisma3d_btn"),
          ) {
            Icon(AppIcons.Animation, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF00E5FF))
            Spacer(Modifier.width(8.dp))
            Text("Export Rig to Prisma3D (.prisma / .p3d / .obj)", style = MaterialTheme.typography.labelLarge, color = Color(0xFF00E5FF))
          }
        }
      }
    }
  }

  if (showRigExportDialog) {
    val snapshotRig = RigAsset(
      id = currentRig?.id ?: "draft_rig",
      userId = currentRig?.userId.orEmpty(),
      name = rigName.trim().ifBlank { "Custom Rig" },
      archetype = archetype,
      activeClip = activeClip,
      ikEnabled = ikEnabled,
      rigScale = if (rigScale.isFinite()) rigScale.coerceIn(0.7f, 1.4f).toDouble() else 1.0,
      playbackSpeed = if (playbackSpeed.isFinite()) playbackSpeed.coerceIn(0.3f, 2.5f).toDouble() else 1.0,
      poseAngles = poseMap.filter { it.value.isFinite() }.toMap(),
      keyframeFrames = keyframes.filter { it in 0L..60L }.distinct().sorted(),
      isCompleted = true,
    )
    val slug = Bo3ModToolsExporter.sanitizeAssetSlug(snapshotRig.name)
    val clipSlug = Bo3ModToolsExporter.sanitizeAssetSlug(snapshotRig.activeClip, "idle")
    val bo3Opts = Bo3ExportOptions(boneMode = bo3ExportMode)
    AlertDialog(
      onDismissRequest = { showRigExportDialog = false },
      title = { Text("Export ${snapshotRig.name}") },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Choose a BO3 Mod Tools skeleton target and export format:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Bo3BoneMappingMode.entries.forEach { mode ->
              FilterChip(
                selected = bo3ExportMode == mode,
                onClick = { bo3ExportMode = mode },
                label = { Text(mode.label) },
              )
            }
          }
          Button(
            onClick = {
              try {
                val outFile = java.io.File(context.filesDir, "${slug}_bo3_blender.py")
                outFile.writeText(Bo3ModToolsExporter.generateCoDBlenderBo3Script(snapshotRig, bo3Opts), Charsets.UTF_8)
                showRigExportDialog = false
                onStatusMessage("Saved ${outFile.name} to UnknownOS local exports!")
              } catch (t: Throwable) {
                onStatusMessage("Local save error: ${t.localizedMessage ?: "Write error"}")
              }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_save_local_rig_btn"),
          ) { Text("Save Quick Copy (.PY to Device)") }
          OutlinedButton(
            onClick = {
              try {
                pendingRigExportBytes = null
                pendingRigExportText = Bo3ModToolsExporter.generateXModelExport(snapshotRig, bo3Opts)
                showRigExportDialog = false
                launchRigSaveDoc("${slug}_rig.xmodel_export")
              } catch (t: Throwable) {
                onStatusMessage("Export error: ${t.localizedMessage ?: "Failed"}")
              }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_export_xmodel_btn"),
          ) { Text(".XMODEL_EXPORT (BO3 Rig & Mesh)") }
          OutlinedButton(
            onClick = {
              try {
                pendingRigExportBytes = null
                pendingRigExportText = Bo3ModToolsExporter.generateXAnimExport(snapshotRig, bo3Opts)
                showRigExportDialog = false
                launchRigSaveDoc("${slug}_${clipSlug}.xanim_export")
              } catch (t: Throwable) {
                onStatusMessage("Export error: ${t.localizedMessage ?: "Failed"}")
              }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_export_xanim_btn"),
          ) { Text(".XANIM_EXPORT (BO3 Animation)") }
          OutlinedButton(
            onClick = {
              try {
                pendingRigExportBytes = null
                pendingRigExportText = Bo3ModToolsExporter.generateCoDBlenderBo3Script(snapshotRig, bo3Opts)
                showRigExportDialog = false
                launchRigSaveDoc("${slug}_bo3_blender.py")
              } catch (t: Throwable) {
                onStatusMessage("Export error: ${t.localizedMessage ?: "Failed"}")
              }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_export_cod_blender_btn"),
          ) { Text("CoD-Blender BO3 Script (.PY)") }
          OutlinedButton(
            onClick = {
              val text = runCatching { PortablePackageGenerator.generateBlenderRigScript(snapshotRig) }.getOrElse {
                onStatusMessage("Export error: ${it.localizedMessage ?: "Failed"}")
                return@OutlinedButton
              }
              showRigExportDialog = false
              shareRigTextSafe("${snapshotRig.name} — UnknownBlend Rig", "Share Blender .PY Script", text)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_share_blender_py_btn"),
          ) { Text("Share Standard Blender .PY") }
          OutlinedButton(
            onClick = {
              showRigExportDialog = false
              showPrisma3dExportDialog = true
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_switch_prisma3d_btn"),
          ) { Text("Switch to Prisma3D Exporter (.prisma / .p3d)") }
        }
      },
      confirmButton = {
        TextButton(onClick = { showRigExportDialog = false }) { Text("Close") }
      },
    )
  }

  if (showPrisma3dExportDialog) {
    val snapshotRig = RigAsset(
      id = currentRig?.id ?: "draft_rig",
      userId = currentRig?.userId.orEmpty(),
      name = rigName.trim().ifBlank { "Custom Rig" },
      archetype = archetype,
      activeClip = activeClip,
      ikEnabled = ikEnabled,
      rigScale = if (rigScale.isFinite()) rigScale.coerceIn(0.7f, 1.4f).toDouble() else 1.0,
      playbackSpeed = if (playbackSpeed.isFinite()) playbackSpeed.coerceIn(0.3f, 2.5f).toDouble() else 1.0,
      poseAngles = poseMap.filter { it.value.isFinite() }.toMap(),
      keyframeFrames = keyframes.filter { it in 0L..60L }.distinct().sorted(),
      isCompleted = true,
    )
    val slug = Prisma3dExporter.sanitizeFileName(snapshotRig.name)
    var p3dFps by remember { mutableStateOf(30) }
    var p3dIncludeKeyframes by remember { mutableStateOf(true) }
    val p3dOptions = remember(p3dFps, p3dIncludeKeyframes) {
      Prisma3dExportOptions(fps = p3dFps, includeKeyframes = p3dIncludeKeyframes)
    }

    AlertDialog(
      onDismissRequest = { showPrisma3dExportDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(AppIcons.Animation, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
          Text("Prisma3D Export: ${snapshotRig.name}")
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Export ${snapshotRig.name} for 3D animation, rigging, and rendering in Prisma3D on mobile & tablet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = p3dFps == 30,
              onClick = { p3dFps = 30 },
              label = { Text("30 FPS") },
              modifier = Modifier.testTag("p3d_fps_30_chip"),
            )
            FilterChip(
              selected = p3dFps == 60,
              onClick = { p3dFps = 60 },
              label = { Text("60 FPS") },
              modifier = Modifier.testTag("p3d_fps_60_chip"),
            )
            FilterChip(
              selected = p3dIncludeKeyframes,
              onClick = { p3dIncludeKeyframes = !p3dIncludeKeyframes },
              label = { Text(if (p3dIncludeKeyframes) "Keyframes: ON" else "Keyframes: OFF") },
              modifier = Modifier.testTag("p3d_keyframes_toggle_chip"),
            )
          }

          Button(
            onClick = {
              try {
                pendingRigExportBytes = null
                pendingRigExportText = Prisma3dExporter.generatePrismaSceneJson(snapshotRig, p3dOptions)
                showPrisma3dExportDialog = false
                launchRigSaveDoc("${slug}.prisma")
              } catch (t: Throwable) {
                onStatusMessage("Export error: ${t.localizedMessage ?: "Failed"}")
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_export_prisma_scene_btn"),
          ) {
            Text("Export Prisma3D Scene (.PRISMA)", color = Color(0xFF00363A), fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = {
              try {
                pendingRigExportBytes = Prisma3dExporter.generatePrisma3dZipBundle(snapshotRig, p3dOptions)
                showPrisma3dExportDialog = false
                launchRigSaveDoc("${slug}_bundle.p3d")
              } catch (t: Throwable) {
                onStatusMessage("Package error: ${t.localizedMessage ?: "Failed"}")
              }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_export_prisma_bundle_btn"),
          ) {
            Text("Export Complete Package (.P3D Zip)")
          }

          OutlinedButton(
            onClick = {
              try {
                pendingRigExportBytes = null
                pendingRigExportText = Prisma3dExporter.generateWavefrontObj(snapshotRig, p3dOptions)
                showPrisma3dExportDialog = false
                launchRigSaveDoc("${slug}_mesh.obj")
              } catch (t: Throwable) {
                onStatusMessage("OBJ export error: ${t.localizedMessage ?: "Failed"}")
              }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_export_prisma_obj_btn"),
          ) {
            Text("Export 3D Wavefront Mesh (.OBJ)")
          }

          OutlinedButton(
            onClick = {
              try {
                pendingRigExportBytes = null
                pendingRigExportText = Prisma3dExporter.generateGlTFSceneJson(snapshotRig, p3dOptions)
                showPrisma3dExportDialog = false
                launchRigSaveDoc("${slug}_scene.gltf")
              } catch (t: Throwable) {
                onStatusMessage("glTF error: ${t.localizedMessage ?: "Failed"}")
              }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_export_prisma_gltf_btn"),
          ) {
            Text("Export glTF 2.0 Scene (.GLTF)")
          }

          OutlinedButton(
            onClick = {
              try {
                val outFile = java.io.File(context.filesDir, "${slug}.prisma")
                outFile.writeText(Prisma3dExporter.generatePrismaSceneJson(snapshotRig, p3dOptions), Charsets.UTF_8)
                showPrisma3dExportDialog = false
                onStatusMessage("Saved ${outFile.name} to UnknownOS local files!")
              } catch (t: Throwable) {
                onStatusMessage("Local save error: ${t.localizedMessage ?: "Write error"}")
              }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_save_local_prisma_btn"),
          ) {
            Text("Save Quick Copy (.PRISMA to App)")
          }

          OutlinedButton(
            onClick = {
              val json = runCatching { Prisma3dExporter.generatePrismaSceneJson(snapshotRig, p3dOptions) }.getOrElse {
                onStatusMessage("Share error: ${it.localizedMessage ?: "Failed"}")
                return@OutlinedButton
              }
              showPrisma3dExportDialog = false
              shareRigTextSafe("${snapshotRig.name} — Prisma3D Scene", "Share Prisma3D Scene", json)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_share_prisma_scene_btn"),
          ) {
            Text("Share Scene JSON")
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showPrisma3dExportDialog = false }, modifier = Modifier.height(48.dp)) {
          Text("Close")
        }
      },
    )
  }

  if (showNewRigDialog) {
    var newName by remember { mutableStateOf("") }
    var newArch by remember { mutableStateOf("starblade") }
    AlertDialog(
      onDismissRequest = { showNewRigDialog = false },
      title = { Text("Create New Character Rig") },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it.take(50) },
            label = { Text("Rig Name (Optional)") },
            placeholder = { Text("e.g. Stargate Sentinel") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("new_rig_name_input"),
          )
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            archetypes.forEach { arch ->
              FilterChip(
                selected = newArch == arch,
                onClick = { newArch = arch },
                label = { Text(arch) },
                modifier = Modifier.height(48.dp),
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val resolvedName = newName.trim().ifBlank { "${newArch.replaceFirstChar { it.uppercase() }} Rig" }
            onSaveRig(
              null,
              resolvedName,
              newArch,
              "idle",
              true,
              1.0,
              1.0,
              UniverseCatalog.defaultPoseMap(),
              listOf(0L, 15L, 30L, 45L, 60L),
              true,
            )
            showNewRigDialog = false
          },
          modifier = Modifier.height(48.dp).testTag("confirm_new_rig_btn"),
        ) { Text("Create") }
      },
      dismissButton = {
        TextButton(onClick = { showNewRigDialog = false }, modifier = Modifier.height(48.dp)) { Text("Cancel") }
      },
    )
  }

  if (showPrisma3dImportDialog) {
    var prismaSceneDraft by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showPrisma3dImportDialog = false },
      title = { Text("Import Prisma3D Rig / Model") },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Pick any Prisma3D file (.p3d, .prisma, .prisma3d, .obj, .gltf, .glb, .zip) or tap a ready-to-rig template below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = { prisma3dImportLauncher(arrayOf("*/*")) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("prisma3d_pick_file_btn"),
          ) {
            Icon(AppIcons.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Choose Prisma3D File (.p3d / .obj / .glb)")
          }

          Text("1-TAP PRISMA3D RIG TEMPLATES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Prisma3dImporter.samplePresets.forEach { (presetKey, label) ->
              FilterChip(
                selected = false,
                onClick = {
                  val imported = Prisma3dImporter.getSamplePreset(presetKey, currentRig?.userId ?: "local_operator")
                  val rig = imported.rig
                  onSaveRig(
                    null,
                    rig.name,
                    rig.archetype,
                    rig.activeClip,
                    rig.ikEnabled,
                    rig.rigScale,
                    rig.playbackSpeed,
                    rig.poseAngles,
                    rig.keyframeFrames,
                    true,
                  )
                  showPrisma3dImportDialog = false
                  onStatusMessage(imported.summary)
                },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(48.dp).testTag("prisma3d_preset_$presetKey"),
              )
            }
          }

          OutlinedTextField(
            value = prismaSceneDraft,
            onValueChange = { prismaSceneDraft = it },
            label = { Text("Or Paste Prisma3D JSON / .OBJ") },
            placeholder = { Text("{\"name\":\"MyHero\",\"bones\":[...]} or Wavefront .obj") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth().testTag("prisma3d_paste_input"),
          )
        }
      },
      confirmButton = {
        Button(
          enabled = prismaSceneDraft.isNotBlank(),
          onClick = {
            val res = Prisma3dImporter.importFromText(
              rawText = prismaSceneDraft,
              fileName = "pasted_scene.prisma",
              targetUserId = currentRig?.userId ?: "local_operator",
            )
            if (res.isSuccess) {
              val imported = res.getOrThrow()
              val rig = imported.rig
              onSaveRig(
                null,
                rig.name,
                rig.archetype,
                rig.activeClip,
                rig.ikEnabled,
                rig.rigScale,
                rig.playbackSpeed,
                rig.poseAngles,
                rig.keyframeFrames,
                true,
              )
              showPrisma3dImportDialog = false
              onStatusMessage(imported.summary)
            } else {
              onStatusMessage(res.exceptionOrNull()?.localizedMessage ?: "Invalid Prisma3D text")
            }
          },
          modifier = Modifier.height(48.dp).testTag("prisma3d_confirm_paste_btn"),
        ) { Text("Import Scene") }
      },
      dismissButton = {
        TextButton(onClick = { showPrisma3dImportDialog = false }, modifier = Modifier.height(48.dp)) { Text("Close") }
      },
    )
  }
}

package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import com.example.ui.theme.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.export.Bo3BoneMappingMode
import com.example.export.Bo3ExportOptions
import com.example.export.Bo3ModToolsExporter
import com.example.export.PortablePackageGenerator
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
) {
  val context = LocalContext.current
  val currentRig = rigs.firstOrNull { it.id == activeRigId } ?: rigs.firstOrNull()

  var rigName by remember(currentRig?.id) { mutableStateOf(currentRig?.name ?: "Custom Rig") }
  var archetype by remember(currentRig?.id) { mutableStateOf(currentRig?.archetype ?: "operator") }
  var activeClip by remember(currentRig?.id) { mutableStateOf(currentRig?.activeClip ?: "idle") }
  var ikEnabled by remember(currentRig?.id) { mutableStateOf(currentRig?.ikEnabled ?: true) }
  var rigScale by remember(currentRig?.id) { mutableFloatStateOf((currentRig?.rigScale ?: 1.0).toFloat()) }
  var playbackSpeed by remember(currentRig?.id) { mutableFloatStateOf((currentRig?.playbackSpeed ?: 1.0).toFloat()) }
  var keyframes by remember(currentRig?.id) {
    mutableStateOf(currentRig?.keyframeFrames ?: listOf(0L, 15L, 30L, 45L, 60L))
  }
  val poseMap = remember(currentRig?.id) {
    mutableStateMapOf<String, Double>().apply {
      putAll(UniverseCatalog.defaultPoseMap())
      currentRig?.poseAngles?.let { putAll(it) }
    }
  }

  var selectedBoneId by remember { mutableStateOf("rUpperArm") }
  var isPlaying by remember(settings.reduceMotion) { mutableStateOf(!settings.reduceMotion) }
  var currentFrame by remember { mutableFloatStateOf(0f) }
  var showBonesOverlay by remember { mutableStateOf(true) }
  var showNewRigDialog by remember { mutableStateOf(false) }
  var showRigExportDialog by remember { mutableStateOf(false) }
  var bo3ExportMode by remember { mutableStateOf(Bo3BoneMappingMode.TREYARCH_CHARACTER) }
  var pendingRigExportText by remember { mutableStateOf("") }

  val rigExportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
      if (uri != null) {
        try {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(pendingRigExportText.toByteArray(Charsets.UTF_8))
          }
          onStatusMessage("Rig export saved successfully!")
        } catch (e: Exception) {
          onStatusMessage("Rig export failed: ${e.localizedMessage}")
        }
      }
    }

  val capePathScratch = remember { Path() }
  val torsoPathScratch = remember { Path() }

  LaunchedEffect(isPlaying, playbackSpeed, settings.targetFpsCap) {
    if (!isPlaying) return@LaunchedEffect
    var lastNanos = withFrameNanos { it }
    var accumNanos = 0L
    val minStepNanos = if (settings.targetFpsCap <= 30) 30_000_000L else 14_000_000L
    while (isPlaying) {
      val now = withFrameNanos { it }
      accumNanos += (now - lastNanos).coerceAtLeast(1_000_000L)
      lastNanos = now
      if (accumNanos >= minStepNanos) {
        val dtSec = (accumNanos / 1_000_000_000f).coerceIn(0.001f, 0.06f)
        accumNanos = 0L
        currentFrame = (currentFrame + playbackSpeed * 54f * dtSec) % 60f
      }
    }
  }

  val clips = listOf("idle", "sprint", "lunge", "slam", "vault", "inspect")
  val archetypes = listOf("operator", "biomorph", "infernal", "chrono", "starblade")

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("UNKNOWNBLEND // FORGERIG 3D", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
          Text("Custom Blender rig viewport — drag bone handles to pose & finish animations", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(
          onClick = { showNewRigDialog = true },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("new_rig_btn"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("New Rig")
        }
      }
    }

    // Rig Library Switcher
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rigs, key = { it.id }) { rig ->
          FilterChip(
            selected = rig.id == currentRig?.id,
            onClick = { onSelectRig(rig.id) },
            label = { Text(rig.name) },
            modifier = Modifier.testTag("rig_chip_${rig.id}"),
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
                  val origin = Offset(size.width * 0.5f, size.height * 0.56f)
                  val skel = RigEvaluator.evaluate(
                    origin = origin,
                    scalePx = size.height * 0.42f * rigScale,
                    poseAngles = poseMap,
                    clip = activeClip,
                    frame = currentFrame,
                    ikEnabled = ikEnabled,
                  )
                  val nearest = skel.handleMap().minByOrNull { (_, pt) ->
                    hypot(pt.x - start.x, pt.y - start.y)
                  }
                  if (nearest != null) {
                    selectedBoneId = nearest.key
                    isPlaying = false
                  }
                },
                onDrag = { change, _ ->
                  change.consume()
                  val origin = Offset(size.width * 0.5f, size.height * 0.56f)
                  val skel = RigEvaluator.evaluate(
                    origin = origin,
                    scalePx = size.height * 0.42f * rigScale,
                    poseAngles = poseMap,
                    clip = activeClip,
                    frame = currentFrame,
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
                  val dx = change.position.x - pivot.x
                  val dy = change.position.y - pivot.y
                  val rawDeg = atan2(dy, dx) * (180f / PI.toFloat())
                  val spec = UniverseCatalog.rigBones.firstOrNull { it.id == selectedBoneId }
                  val clamped = if (spec != null) {
                    rawDeg.coerceIn(spec.minAngle, spec.maxAngle)
                  } else {
                    rawDeg.coerceIn(-120f, 120f)
                  }
                  poseMap[selectedBoneId] = clamped.toDouble()
                },
              )
            }
          ) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.34f
            val vanishX = w * 0.5f
            val groundY = h * 0.56f + (h * 0.42f * rigScale) * 0.56f

            // Blender 3D perspective floor grid
            for (i in -8..8) {
              val bx = vanishX + i * (w * 0.085f)
              val tx = vanishX + i * (w * 0.025f)
              drawLine(Color(0xFF282E3A), Offset(tx, horizonY), Offset(bx, h), strokeWidth = 1f)
            }
            for (r in 0..7) {
              val t = r / 7f
              val ry = horizonY + (h - horizonY) * (t * t)
              drawLine(Color(0xFF282E3A), Offset(0f, ry), Offset(w, ry), strokeWidth = 1f)
            }
            // Blender World Axes (X Crimson, Y Green, Z Blue)
            drawLine(Color(0xAAFF4060), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 2f)
            drawLine(Color(0x8838E54D), Offset(vanishX, horizonY), Offset(vanishX, h), strokeWidth = 1.6f)
            drawLine(Color(0x6640A9FF), Offset(vanishX, 16f), Offset(vanishX, groundY), strokeWidth = 1.4f)

            val origin = Offset(vanishX, h * 0.56f)
            val skel = RigEvaluator.evaluate(
              origin = origin,
              scalePx = h * 0.42f * rigScale,
              poseAngles = poseMap,
              clip = activeClip,
              frame = currentFrame,
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
              dynamicGlow = settings.dynamicGlowEffects && settings.graphicsQuality != GraphicsQuality.LOW,
              highContrast = settings.effectiveHighContrast,
              cloakOverride = progression.characterCustom.cloakEnabled,
              visorColorOverride = Color(visorHex),
              auraTrailId = progression.characterCustom.auraTrailId,
            )

            // Blender-style Top-Right XYZ Orientation Gizmo
            val gCenter = Offset(w - 36f, 36f)
            drawCircle(Color(0x6610141D), radius = 22f, center = gCenter)
            drawLine(Color(0xFFFF4060), gCenter, Offset(gCenter.x + 16f, gCenter.y + 4f), strokeWidth = 2.5f)
            drawLine(Color(0xFF38E54D), gCenter, Offset(gCenter.x - 12f, gCenter.y + 9f), strokeWidth = 2.5f)
            drawLine(Color(0xFF40A9FF), gCenter, Offset(gCenter.x, gCenter.y - 16f), strokeWidth = 2.5f)

            // Subtle Dope-Sheet Keyframe Strip at bottom edge
            val barY = h - 10f
            drawLine(Color(0xFF343B4A), Offset(16f, barY), Offset(w - 110f, barY), strokeWidth = 2f)
            keyframes.forEach { kf ->
              val kx = 16f + (w - 126f) * (kf.toFloat() / 60f).coerceIn(0f, 1f)
              drawCircle(Color(0xFFFFB300), radius = 4.5f, center = Offset(kx, barY))
            }
            val curX = 16f + (w - 126f) * (currentFrame / 60f).coerceIn(0f, 1f)
            drawLine(Color(0xFF00E5FF), Offset(curX, barY - 8f), Offset(curX, barY + 6f), strokeWidth = 2.5f)
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
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xCC111622)) {
              Text(
                text = "BONE: $selectedBoneId (${poseMap[selectedBoneId]?.toInt() ?: 0}°)",
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
            value = currentFrame,
            onValueChange = {
              isPlaying = false
              currentFrame = it
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
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("ANIMATION CLIPS", style = MaterialTheme.typography.titleMedium)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            clips.forEach { clip ->
              FilterChip(
                selected = activeClip == clip,
                onClick = { activeClip = clip },
                label = { Text(clip.uppercase()) },
                modifier = Modifier.testTag("clip_chip_$clip"),
              )
            }
          }

          Text("RIG ARCHETYPE", style = MaterialTheme.typography.titleMedium)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            archetypes.forEach { arch ->
              FilterChip(
                selected = archetype == arch,
                onClick = { archetype = arch },
                label = { Text(arch.replaceFirstChar { it.uppercase() }) },
              )
            }
          }

          // Selected Bone Angle Slider
          val boneSpec = UniverseCatalog.rigBones.firstOrNull { it.id == selectedBoneId }
          if (boneSpec != null) {
            val valDeg = (poseMap[selectedBoneId] ?: boneSpec.defaultAngle.toDouble()).toFloat()
            Text(
              text = "${boneSpec.label}: ${valDeg.toInt()}°",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Slider(
              value = valDeg.coerceIn(boneSpec.minAngle, boneSpec.maxAngle),
              onValueChange = { poseMap[selectedBoneId] = it.toDouble() },
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

          Text("Playback Speed: ${String.format(Locale.US, "%.2fx", playbackSpeed)}", style = MaterialTheme.typography.labelMedium)
          Slider(value = playbackSpeed, onValueChange = { playbackSpeed = it }, valueRange = 0.3f..2.5f)

          Text("Rig Scale: ${String.format(Locale.US, "%.2fx", rigScale)}", style = MaterialTheme.typography.labelMedium)
          Slider(value = rigScale, onValueChange = { rigScale = it }, valueRange = 0.7f..1.4f)

          Button(
            onClick = {
              onSaveRig(
                currentRig?.id,
                rigName,
                archetype,
                activeClip,
                ikEnabled,
                rigScale.toDouble(),
                playbackSpeed.toDouble(),
                poseMap.toMap(),
                keyframes,
                true,
              )
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
        }
      }
    }
  }

  if (showRigExportDialog) {
    val snapshotRig = RigAsset(
      id = currentRig?.id ?: "draft_rig",
      userId = currentRig?.userId.orEmpty(),
      name = rigName,
      archetype = archetype,
      activeClip = activeClip,
      ikEnabled = ikEnabled,
      rigScale = rigScale.toDouble(),
      playbackSpeed = playbackSpeed.toDouble(),
      poseAngles = poseMap.toMap(),
      keyframeFrames = keyframes,
      isCompleted = true,
    )
    val slug = Bo3ModToolsExporter.sanitizeAssetSlug(snapshotRig.name)
    val clipSlug = Bo3ModToolsExporter.sanitizeAssetSlug(snapshotRig.activeClip, "idle")
    val bo3Opts = Bo3ExportOptions(boneMode = bo3ExportMode)
    AlertDialog(
      onDismissRequest = { showRigExportDialog = false },
      title = { Text("Export ${snapshotRig.name}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
              pendingRigExportText = Bo3ModToolsExporter.generateXModelExport(snapshotRig, bo3Opts)
              showRigExportDialog = false
              rigExportLauncher.launch("${slug}_rig.xmodel_export")
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_export_xmodel_btn"),
          ) { Text(".XMODEL_EXPORT (BO3 Rig & Mesh)") }
          OutlinedButton(
            onClick = {
              pendingRigExportText = Bo3ModToolsExporter.generateXAnimExport(snapshotRig, bo3Opts)
              showRigExportDialog = false
              rigExportLauncher.launch("${slug}_${clipSlug}.xanim_export")
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_export_xanim_btn"),
          ) { Text(".XANIM_EXPORT (BO3 Animation)") }
          OutlinedButton(
            onClick = {
              pendingRigExportText = Bo3ModToolsExporter.generateCoDBlenderBo3Script(snapshotRig, bo3Opts)
              showRigExportDialog = false
              rigExportLauncher.launch("${slug}_bo3_blender.py")
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_export_cod_blender_btn"),
          ) { Text("CoD-Blender BO3 Script (.PY)") }
          OutlinedButton(
            onClick = {
              val text = PortablePackageGenerator.generateBlenderRigScript(snapshotRig)
              val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "${snapshotRig.name} — UnknownBlend Rig")
                putExtra(Intent.EXTRA_TEXT, text)
              }
              showRigExportDialog = false
              context.startActivity(Intent.createChooser(shareIntent, "Share Blender .PY Script"))
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_share_blender_py_btn"),
          ) { Text("Share Standard Blender .PY") }
        }
      },
      confirmButton = {
        TextButton(onClick = { showRigExportDialog = false }) { Text("Close") }
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it.take(50) },
            label = { Text("Rig Name") },
            placeholder = { Text("e.g. Stargate Sentinel") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            archetypes.forEach { arch ->
              FilterChip(selected = newArch == arch, onClick = { newArch = arch }, label = { Text(arch) })
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newName.trim().isNotEmpty()) {
              onSaveRig(
                null,
                newName.trim(),
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
            }
          }
        ) { Text("Create") }
      },
      dismissButton = {
        TextButton(onClick = { showNewRigDialog = false }) { Text("Cancel") }
      },
    )
  }
}

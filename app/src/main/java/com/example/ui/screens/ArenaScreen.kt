package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettings
import com.example.data.local.ColorBlindMode
import com.example.data.local.GraphicsQuality
import com.example.data.local.ParticleDensity
import com.example.data.local.resolveArenaPalette
import com.example.data.model.ArmoryItem
import com.example.data.model.MatchProgressionReport
import com.example.data.model.OperatorProfile
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.ui.rig.RigEvaluator
import com.example.ui.rig.drawArenaHostile
import com.example.ui.rig.drawRigCharacter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import com.example.data.model.ChatChannel
import com.example.data.model.SocialCatalog
import com.example.ui.theme.AppIcons
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ArenaScreen(
  profile: OperatorProfile,
  armory: List<ArmoryItem>,
  rigs: List<RigAsset>,
  settings: AppSettings = AppSettings(),
  progression: OperatorProgressionState = OperatorProgressionState(),
  isPausedForSettings: Boolean = false,
  onOpenSettings: () -> Unit = {},
  onRecordProgression: (MatchProgressionReport) -> Unit = {},
  onSendChatMessage: (ChatChannel, String, String?, Boolean) -> Unit = { _, _, _, _ -> },
  onSaveMatchAndExit: (score: Long, wave: Long, eliminations: Long, extracted: Boolean, durationSec: Long) -> Unit,
) {
  val world = remember(profile.activeUniverseId) { UniverseCatalog.worldById(profile.activeUniverseId) }
  val mode = remember(profile.activeMode) { UniverseCatalog.modeById(profile.activeMode) }
  val console = remember(profile.consolePreset) { UniverseCatalog.consoleById(profile.consolePreset) }
  val activeRig = remember(rigs, profile.activeRigId) {
    rigs.firstOrNull { it.id == profile.activeRigId } ?: rigs.firstOrNull()
  }
  val activeOutfit = remember(progression.equippedOutfitId) { progression.activeOutfitSpec() }
  val activeAreaSpec = remember(progression.activeAreaId) { progression.activeAreaSpec() }
  val activeAreaState = remember(progression.activeAreaId, progression.areaStates) { progression.areaState() }

  val loadout = remember(armory, world.accentHex, progression, profile.totalEssence) {
    ArenaLoadoutStats.fromArmory(
      armory = armory,
      fallbackAccent = Color(world.accentHex),
      progression = progression,
      blueEssence = profile.totalEssence,
    )
  }
  val hasConsumable = remember(armory) { armory.any { it.equipped && it.category == "consumable" } }

  val sim = remember(world.id, mode.id, loadout, hasConsumable) {
    ArenaSimState(
      world = world,
      modeId = mode.id,
      loadout = loadout,
      hasConsumableEquipped = hasConsumable,
      initialSettings = settings,
      progression = progression,
      armoryItems = armory,
      initialBlueEssence = profile.totalEssence,
    )
  }

  LaunchedEffect(settings) {
    sim.liveSettings = settings
  }
  LaunchedEffect(settings.autoFireEnabled) {
    sim.hudAutoFire = settings.autoFireEnabled
  }

  val palette = remember(world.accentHex, world.secondaryHex, settings.colorBlindMode, settings.highContrastMode, settings.themeMode) {
    resolveArenaPalette(Color(world.accentHex), Color(world.secondaryHex), settings)
  }

  val haptic = LocalHapticFeedback.current
  val triggerHaptic: () -> Unit = {
    if (settings.hapticFeedbackEnabled) {
      runCatching { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
  }

  val backdropCache = remember { ArenaBackdropCache() }
  val capePathScratch = remember { Path() }
  val torsoPathScratch = remember { Path() }
  val focusRequester = remember { FocusRequester() }
  val heldKeys = remember { mutableSetOf<Int>() }
  var thumbOffsetX by remember { mutableFloatStateOf(0f) }
  var thumbOffsetY by remember { mutableFloatStateOf(0f) }
  var hudFps by remember { mutableIntStateOf(60) }
  var showTacticalLiveChatDialog by remember { mutableStateOf(false) }

  fun syncKeyboardAxes() {
    var dx = 0f
    var dy = 0f
    if (heldKeys.contains(KeyEvent.KEYCODE_A) || heldKeys.contains(KeyEvent.KEYCODE_DPAD_LEFT)) dx -= 1f
    if (heldKeys.contains(KeyEvent.KEYCODE_D) || heldKeys.contains(KeyEvent.KEYCODE_DPAD_RIGHT)) dx += 1f
    if (heldKeys.contains(KeyEvent.KEYCODE_W) || heldKeys.contains(KeyEvent.KEYCODE_DPAD_UP)) dy -= 1f
    if (heldKeys.contains(KeyEvent.KEYCODE_S) || heldKeys.contains(KeyEvent.KEYCODE_DPAD_DOWN)) dy += 1f
    sim.joyDx = dx
    sim.joyDy = dy
    thumbOffsetX = dx * 22f * settings.joystickScale
    thumbOffsetY = dy * 22f * settings.joystickScale
  }

  LaunchedEffect(sim, isPausedForSettings, settings.targetFpsCap, settings.showFpsCounter) {
    runCatching { focusRequester.requestFocus() }
    if (isPausedForSettings) return@LaunchedEffect

    var lastNanos = withFrameNanos { it }
    var accumNanos = 0L
    var fpsAccumNanos = 0L
    var fpsFrames = 0
    val minStepNanos = when {
      settings.targetFpsCap <= 30 -> 30_000_000L
      settings.targetFpsCap >= 120 -> 4_000_000L
      else -> 14_000_000L
    }

    while (!sim.hudRunEnded && !isPausedForSettings) {
      val frameNanos = withFrameNanos { it }
      val rawDeltaNanos = (frameNanos - lastNanos).coerceAtLeast(1_000_000L)
      lastNanos = frameNanos
      accumNanos += rawDeltaNanos

      if (accumNanos >= minStepNanos) {
        val dtSec = (accumNanos / 1_000_000_000f).coerceIn(0.001f, 0.06f)
        accumNanos = 0L
        sim.stepFrame(dtSec)
        if (settings.showFpsCounter) {
          fpsAccumNanos += rawDeltaNanos
          fpsFrames++
          if (fpsAccumNanos >= 500_000_000L) {
            hudFps = ((fpsFrames * 1_000_000_000.0) / fpsAccumNanos.coerceAtLeast(1L)).roundToInt()
            fpsAccumNanos = 0L
            fpsFrames = 0
          }
        }
      }
    }
  }

  val outerMargin = settings.screenMarginDp.dp
  val hudSurfaceBg = if (settings.effectiveHighContrast) {
    Color.Black.copy(alpha = settings.hudOpacity.coerceAtLeast(0.92f))
  } else {
    Color(0xFF0A101E).copy(alpha = settings.hudOpacity)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(if (settings.effectiveHighContrast) Color.Black else Color(0xFF060911))
      .padding(outerMargin)
      .focusRequester(focusRequester)
      .focusable()
      .onPreviewKeyEvent { keyEvent ->
        val code = keyEvent.nativeKeyEvent.keyCode
        if (keyEvent.type == KeyEventType.KeyDown) {
          when (code) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W,
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
              heldKeys.add(code)
              syncKeyboardAxes()
              true
            }
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_BUTTON_A -> { sim.fireVolley(forceManual = true); true }
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_BUTTON_X -> { sim.triggerPower(); true }
            KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_BUTTON_Y -> { sim.triggerConsumable(); true }
            KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_BUTTON_L1 -> { sim.triggerWorldInteract(); true }
            KeyEvent.KEYCODE_R -> { sim.cycleActiveWeapon(); true }
            KeyEvent.KEYCODE_T -> { sim.triggerAllTraps(); true }
            KeyEvent.KEYCODE_Z -> { sim.activateEquippedWonderSlot(); true }
            KeyEvent.KEYCODE_C -> { sim.triggerLethal(); true }
            KeyEvent.KEYCODE_V -> { sim.triggerTactical(); true }
            KeyEvent.KEYCODE_P -> { sim.upgradePackAPunch(); true }
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_BUTTON_B -> { sim.triggerDash(); true }
            else -> false
          }
        } else if (keyEvent.type == KeyEventType.KeyUp) {
          if (heldKeys.remove(code)) {
            syncKeyboardAxes()
            true
          } else false
        } else false
      }
  ) {
    // Low-Frequency Tactical HUD Top Bar
    Surface(
      color = hudSurfaceBg,
      modifier = Modifier.fillMaxWidth().border(1.dp, palette.playerAccent.copy(alpha = 0.6f)),
    ) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = palette.hostile.copy(alpha = 0.22f),
              modifier = Modifier.border(1.5.dp, palette.hostile, RoundedCornerShape(8.dp)),
            ) {
              Text(
                text = "RND ${formatRoundRoman(sim.hudWave)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.hostile,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
              )
            }
            Column {
              val elimTarget = if (mode.id == "multiplayer") "${sim.hudElims}/25" else "${sim.hudElims}"
              Text(
                text = "${activeAreaSpec.name.substringBefore(" (")} (${activeAreaState.ageYears}y) • ${mode.shortTag} • PACK ${formatRoundRoman(sim.hudPackTier.toLong())}",
                style = MaterialTheme.typography.titleSmall,
                color = palette.playerAccent,
                maxLines = 1,
              )
              Text(
                text = "SCORE ${sim.hudScore} • ELIMS $elimTarget • SHD ${sim.hudShield}/${sim.loadout.maxShield} • VAC ${sim.hudVacuumChargePct}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
              )
              Text(
                text = "${activeOutfit.nameWithVersion} • LV ${sim.loadout.playerLevel} • ${sim.loadout.activePassVersion.title.substringBefore(":")} • ${sim.hudActiveWeaponName} (${sim.activeWeaponIndex + 1}/${sim.equippedWeaponIds.size}) • ${progression.lobbyConfig.targetPlayers}P (${sim.hudBotCount} BOTS)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(sim.loadout.activePassVersion.accentHex),
                modifier = Modifier.testTag("arena_level_weapon_hud"),
                maxLines = 1,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (progression.joinedMatch?.isPrivate == true || progression.privateMatchRules.isPrivate) {
              val pCode = progression.joinedMatch?.roomCode ?: progression.privateMatchRules.roomCode
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFF9100).copy(alpha = 0.22f),
                modifier = Modifier.border(1.dp, Color(0xFFFF9100), RoundedCornerShape(6.dp)).testTag("private_match_hud_badge"),
              ) {
                Text(
                  text = "🔒 $pCode",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFFF9100),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
              }
            }
            if (settings.showFpsCounter) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF102033),
                modifier = Modifier.border(1.dp, Color(0xFF00E676).copy(alpha = 0.6f), RoundedCornerShape(6.dp)).testTag("fps_counter_badge"),
              ) {
                Text(
                  text = "$hudFps FPS",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF00E676),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
              }
            }
            IconButton(
              onClick = { showTacticalLiveChatDialog = true },
              modifier = Modifier.size(48.dp).testTag("live_chat_arena_btn"),
            ) {
              Icon(
                imageVector = AppIcons.Chat,
                contentDescription = "Live Combat Tactical Comms",
                tint = Color(0xFF00E5FF),
              )
            }
            IconButton(
              onClick = onOpenSettings,
              modifier = Modifier.size(48.dp).testTag("settings_arena_btn"),
            ) {
              Icon(
                imageVector = AppIcons.Settings,
                contentDescription = "Arena Settings & Accessibility",
                tint = palette.playerAccent,
              )
            }
            OutlinedButton(
              onClick = {
                sim.hudRunEnded = true
                onRecordProgression(sim.buildProgressionReport())
                onSaveMatchAndExit(
                  sim.hudScore,
                  sim.hudWave,
                  sim.hudElims,
                  sim.hudExtractedSuccess || mode.id == "sandbox",
                  sim.elapsedTicks / 60L,
                )
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("extract_exit_arena_btn"),
            ) {
              Icon(AppIcons.Logout, contentDescription = "Exfil", modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Exfil")
            }
          }
        }

        // Segmented Shield Bar + Building / Wonder / Resonance / Multi-Essence Strip
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier = Modifier
              .weight(1f)
              .height(if (settings.effectiveHighContrast) 10.dp else 7.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xFF1C2742))
          ) {
            val pct = (sim.hudShield.toFloat() / sim.loadout.maxShield.coerceAtLeast(1)).coerceIn(0f, 1f)
            Box(
              modifier = Modifier
                .fillMaxWidth(pct)
                .height(if (settings.effectiveHighContrast) 10.dp else 7.dp)
                .background(if (sim.hudOverdriveActive) palette.dropBuff else palette.playerAccent)
            )
          }
          val activeWonder = sim.hudActiveWonder
          val dropText = sim.hudActiveDropText
          val bld = sim.hudActiveBuildingName
          when {
            activeWonder != null -> {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(activeWonder.accentHex).copy(alpha = 0.22f),
                modifier = Modifier.border(1.dp, Color(activeWonder.accentHex), RoundedCornerShape(6.dp)).testTag("arena_wonder_badge"),
              ) {
                Text(
                  text = "WONDER: ${activeWonder.shortTag} (${sim.hudWonderSecLeft}s)",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(activeWonder.accentHex),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
            bld != null -> {
              Text(
                text = "INSIDE: $bld",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E676),
              )
            }
            dropText != null -> {
              Text(
                text = dropText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = palette.dropBuff,
              )
            }
            !settings.simplifiedHud -> {
              Text(
                text = "SHARDS:${progression.blastShards + sim.blastShardsEarned} • ${sim.loadout.resonance.summary}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.safeExfil,
              )
            }
          }
        }
      }
    }

    // Playable 2.2x2.2 Scrolling Multiverse Arena Canvas + Touch-to-Aim
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(sim, settings.touchCanvasAimEnabled) {
            if (!settings.touchCanvasAimEnabled) return@pointerInput
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              val w = size.width.toFloat().coerceAtLeast(1f)
              val h = size.height.toFloat().coerceAtLeast(1f)
              val cx0 = sim.cameraOriginX()
              val cy0 = sim.cameraOriginY()
              sim.setTouchAim(cx0 + (down.position.x / w), cy0 + (down.position.y / h), active = true)
              sim.fireVolley(forceManual = true)
              while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                if (change == null || !change.pressed) {
                  sim.setTouchAim(0f, 0f, active = false)
                  break
                }
                change.consume()
                val cx = sim.cameraOriginX()
                val cy = sim.cameraOriginY()
                sim.setTouchAim(cx + (change.position.x / w), cy + (change.position.y / h), active = true)
              }
            }
          }
      ) {
        @Suppress("UNUSED_VARIABLE")
        val tick = sim.renderNonce
        val w = size.width
        val h = size.height
        val camX = sim.cameraOriginX()
        val camY = sim.cameraOriginY()
        val glowOn = settings.dynamicGlowEffects && settings.graphicsQuality != GraphicsQuality.LOW
        val highContrast = settings.effectiveHighContrast
        val pulse = if (settings.reduceMotion || settings.reduceFlashes) 0.3f else (sin(sim.elapsedTicks * 0.12f) + 1f) * 0.5f

        drawWorldArenaBackdrop(
          worldId = world.id,
          accentColor = palette.playerAccent,
          secondaryColor = palette.playerSecondary,
          elapsedTicks = sim.elapsedTicks,
          chevronsLocked = sim.hudChevronsLocked,
          overdriveActive = sim.hudOverdriveActive,
          backdropCache = backdropCache,
          stationReady = sim.hudInteractCdSec == 0,
          showTacticalGrid = settings.showTacticalGrid,
          reduceMotion = settings.reduceMotion,
          reduceFlashes = settings.reduceFlashes,
          highContrast = highContrast,
          lowGraphics = settings.graphicsQuality == GraphicsQuality.LOW,
          skyThemeId = progression.levelCustom.skyThemeId,
          gridStyleId = progression.levelCustom.gridStyleId,
          floorAccentOverride = if (settings.colorBlindMode == ColorBlindMode.NONE && !highContrast) {
            Color(progression.levelCustom.floorAccentHex)
          } else null,
          camX = camX,
          camY = camY,
          areaAgeEra = activeAreaState.currentEra,
        )

        drawBuildingsAndTraps(
          sim = sim,
          camX = camX,
          camY = camY,
          glowOn = glowOn,
          pulse = pulse,
        )

        if (mode.id == "br") {
          drawCircle(
            color = palette.dropHazard,
            radius = sim.brRingRadius * minOf(w, h),
            center = Offset((ARENA_CENTER - camX) * w, (ARENA_CENTER - camY) * h),
            style = Stroke(width = if (highContrast) 6f else 4f),
          )
        }

        if (mode.id == "dmz") {
          val exfilReady = sim.hudIntelCollected >= 3
          val exfilCenter = Offset((ARENA_CENTER - camX) * w, (0.30f - camY) * h)
          drawCircle(
            color = if (exfilReady) palette.safeExfil else palette.dropBuff.copy(alpha = 0.65f),
            radius = 32f,
            center = exfilCenter,
            style = Stroke(width = if (highContrast) 6f else 4f),
          )
          if (sim.hudExfilRatio > 0f) {
            drawArc(
              color = palette.playerAccent,
              startAngle = -90f,
              sweepAngle = sim.hudExfilRatio * 360f,
              useCenter = false,
              topLeft = Offset(exfilCenter.x - 40f, exfilCenter.y - 40f),
              size = Size(80f, 80f),
              style = Stroke(width = 5f, cap = StrokeCap.Round),
            )
          }
          for (i in 0 until sim.intels.size) {
            val intel = sim.intels[i]
            if (!intel.collected) {
              val ip = Offset((intel.x - camX) * w, (intel.y - camY) * h)
              if (glowOn) drawCircle(palette.dropBuff.copy(alpha = 0.25f), radius = 20f, center = ip)
              drawCircle(palette.dropBuff, radius = 11f, center = ip)
            }
          }
        }

        drawVacuumAndCollectibles(
          sim = sim,
          camX = camX,
          camY = camY,
          glowOn = glowOn,
          pulse = pulse,
        )

        // Power-up Field Drops
        for (i in 0 until sim.drops.size) {
          val d = sim.drops[i]
          val pos = Offset((d.x - camX) * w, (d.y - camY) * h)
          if (pos.x < -30f || pos.x > w + 30f || pos.y < -30f || pos.y > h + 30f) continue
          val dCol = when (d.kind) {
            ArenaDropKind.NUKE -> palette.playerAccent
            ArenaDropKind.INSTA_KILL -> palette.dropHazard
            ArenaDropKind.MAX_ARMOR -> palette.safeExfil
            ArenaDropKind.DOUBLE_ESSENCE -> palette.dropBuff
          }
          if (glowOn) drawCircle(dCol.copy(alpha = 0.24f + pulse * 0.18f), radius = 18f, center = pos)
          drawCircle(dCol, radius = if (highContrast) 12f else 10f, center = pos, style = Stroke(width = 3f))
          drawCircle(Color.White, radius = 4f, center = pos)
        }

        // Random Mystery Wonder Weapon Pickups
        for (i in 0 until sim.wonderPickups.size) {
          val wp = sim.wonderPickups[i]
          val pos = Offset((wp.x - camX) * w, (wp.y - camY) * h)
          if (pos.x < -30f || pos.x > w + 30f || pos.y < -30f || pos.y > h + 30f) continue
          val wCol = Color(wp.kind.accentHex)
          if (glowOn) drawCircle(wCol.copy(alpha = 0.28f + pulse * 0.22f), radius = 22f, center = pos)
          drawRect(color = Color(0xFF0A1222), topLeft = Offset(pos.x - 11f, pos.y - 11f), size = Size(22f, 22f))
          drawRect(color = wCol, topLeft = Offset(pos.x - 11f, pos.y - 11f), size = Size(22f, 22f), style = Stroke(width = 3f))
          drawCircle(Color.White, radius = 4.5f, center = pos)
        }

        if (sim.pulseRingRadius > 0f && !settings.reduceFlashes) {
          drawCircle(
            color = palette.playerAccent.copy(alpha = 0.65f),
            radius = sim.pulseRingRadius * minOf(w, h),
            center = Offset((sim.playerX - camX) * w, (sim.playerY - camY) * h),
            style = Stroke(width = 7f),
          )
        }

        // Arcs
        for (i in 0 until sim.arcs.size) {
          val arc = sim.arcs[i]
          val a = (arc.life / 18f).coerceIn(0f, 1f)
          val p1 = Offset((arc.x1 - camX) * w, (arc.y1 - camY) * h)
          val p2 = Offset((arc.x2 - camX) * w, (arc.y2 - camY) * h)
          if (glowOn) {
            drawLine(color = arc.color.copy(alpha = a * 0.78f), start = p1, end = p2, strokeWidth = 6f * a, cap = StrokeCap.Round)
          }
          drawLine(color = Color.White.copy(alpha = a), start = p1, end = p2, strokeWidth = 2.5f, cap = StrokeCap.Round)
        }

        // Projectiles
        for (i in 0 until sim.bolts.size) {
          val b = sim.bolts[i]
          val head = Offset((b.x - camX) * w, (b.y - camY) * h)
          if (head.x < -30f || head.x > w + 30f || head.y < -30f || head.y > h + 30f) continue
          if (b.isEnemyShot) {
            if (glowOn) drawCircle(palette.enemyShot.copy(alpha = 0.45f), radius = 10f, center = head)
            drawCircle(palette.enemyShot, radius = if (highContrast) 7f else 5f, center = head)
          } else {
            val tail = Offset((b.x - b.vx * 2.2f - camX) * w, (b.y - b.vy * 2.2f - camY) * h)
            val boltCol = if (settings.colorBlindMode == ColorBlindMode.NONE) b.color else palette.playerAccent
            if (glowOn) {
              drawLine(boltCol.copy(alpha = 0.58f), tail, head, strokeWidth = if (b.isSupercharged) 10f else 6.5f, cap = StrokeCap.Round)
            }
            drawLine(Color.White, tail, head, strokeWidth = 3f, cap = StrokeCap.Round)
          }
        }

        // Impact Sparks
        if (settings.particleDensity != ParticleDensity.OFF) {
          for (i in 0 until sim.sparks.size) {
            val sp = sim.sparks[i]
            val sx = (sp.x - camX) * w
            val sy = (sp.y - camY) * h
            val r = (sp.maxLife - sp.life).coerceAtLeast(3f) * 1.8f
            drawCircle(
              color = sp.color.copy(alpha = (sp.life / sp.maxLife).coerceIn(0f, 1f)),
              radius = r,
              center = Offset(sx, sy),
              style = Stroke(width = 2.5f),
            )
          }
        }

        // Animated Lore Hostiles & Bosses
        val overrideEnemyCol = if (settings.colorBlindMode != ColorBlindMode.NONE || highContrast) palette.hostile else null
        for (i in 0 until sim.hostiles.size) {
          val e = sim.hostiles[i]
          val ex = (e.x - camX) * w
          val ey = (e.y - camY) * h
          if (ex < -60f || ex > w + 60f || ey < -60f || ey > h + 60f) continue
          drawArenaHostile(
            center = Offset(ex, ey),
            worldId = world.id,
            isElite = e.isElite,
            stasisActive = sim.stasisTicks > 0f,
            hp = e.hp,
            maxHp = e.maxHp,
            animPhase = sim.elapsedTicks * 0.14f + i,
            archetype = e.archetype,
            isBoss = e.isBoss,
            reduceMotion = settings.reduceMotion,
            highContrast = highContrast,
            dynamicGlow = glowOn,
            overrideColor = if (e.isBoss && overrideEnemyCol != null) palette.boss else overrideEnemyCol,
          )
        }

        // AI Lobby Bot Combatants
        for (i in 0 until sim.arenaBots.size) {
          val bot = sim.arenaBots[i]
          if (bot.hp <= 0) continue
          val bx = (bot.x - camX) * w
          val by = (bot.y - camY) * h
          if (bx < -50f || bx > w + 50f || by < -50f || by > h + 50f) continue
          val botOrigin = Offset(bx, by)
          val botRingCol = if (bot.isAllied) palette.safeExfil else palette.dropBuff
          drawCircle(color = botRingCol.copy(alpha = 0.72f), radius = 20f, center = botOrigin, style = Stroke(width = 2.4f))
          val botSkel = RigEvaluator.evaluate(
            origin = botOrigin,
            scalePx = 56f,
            poseAngles = UniverseCatalog.defaultPoseMap(),
            clip = if (settings.reduceMotion) "idle" else "sprint",
            frame = if (settings.reduceMotion) 0f else (sim.frameTick + i * 11f),
            ikEnabled = true,
            animationSpeedMultiplier = progression.characterCustom.animationSpeedMultiplier,
          )
          drawRigCharacter(
            skeleton = botSkel,
            archetype = bot.archetype,
            primaryColor = bot.color,
            secondaryColor = botRingCol,
            showBonesOverlay = false,
            capePathScratch = capePathScratch,
            torsoPathScratch = torsoPathScratch,
            packTier = 1,
            isDashing = false,
            dynamicGlow = glowOn,
            highContrast = highContrast,
            cloakOverride = false,
          )
        }

        // Aim Reticle
        val retPos = if (sim.touchAimActive) {
          Offset((sim.touchAimX - camX) * w, (sim.touchAimY - camY) * h)
        } else {
          Offset((sim.playerX + sim.aimX * 0.085f - camX) * w, (sim.playerY + sim.aimY * 0.085f - camY) * h)
        }
        val opticCol = if (settings.colorBlindMode == ColorBlindMode.NONE && !highContrast) {
          sim.loadout.tracerColor
        } else palette.playerAccent
        drawCircle(opticCol.copy(alpha = 0.85f), radius = 10f, center = retPos, style = Stroke(width = 2.4f))
        drawCircle(Color.White.copy(alpha = 0.9f), radius = 2.5f, center = retPos)

        // Player Character with Active Outfit & Rig
        val isMoving = (sim.joyDx * sim.joyDx + sim.joyDy * sim.joyDy) > 0.0064f
        val playerClip = when {
          settings.reduceMotion -> "idle"
          isMoving -> "sprint"
          else -> activeRig?.activeClip ?: "idle"
        }
        val skel = RigEvaluator.evaluate(
          origin = Offset((sim.playerX - camX) * w, (sim.playerY - camY) * h),
          scalePx = 70f * (activeRig?.rigScale?.toFloat() ?: 1f),
          poseAngles = activeRig?.poseAngles ?: UniverseCatalog.defaultPoseMap(),
          clip = playerClip,
          frame = if (settings.reduceMotion) 0f else sim.frameTick,
          ikEnabled = activeRig?.ikEnabled ?: true,
          animationSpeedMultiplier = progression.characterCustom.animationSpeedMultiplier,
        )
        val visorHex = ProgressionCatalog.visorStyles
          .firstOrNull { it.first == progression.characterCustom.visorStyleId }
          ?.second ?: activeOutfit.visorHex
        val customPrimary = if (settings.colorBlindMode == ColorBlindMode.NONE && !highContrast) {
          Color(activeOutfit.primaryHex)
        } else palette.playerAccent
        val customSecondary = if (settings.colorBlindMode == ColorBlindMode.NONE && !highContrast) {
          Color(activeOutfit.secondaryHex)
        } else palette.playerSecondary
        drawRigCharacter(
          skeleton = skel,
          archetype = activeOutfit.rigArchetype.ifBlank { activeRig?.archetype ?: "operator" },
          primaryColor = customPrimary,
          secondaryColor = customSecondary,
          showBonesOverlay = false,
          capePathScratch = capePathScratch,
          torsoPathScratch = torsoPathScratch,
          packTier = sim.hudPackTier,
          isDashing = sim.dashInvulnTicks > 0f,
          dynamicGlow = glowOn,
          highContrast = highContrast,
          cloakOverride = activeOutfit.cloakEnabled,
          visorColorOverride = if (settings.colorBlindMode == ColorBlindMode.NONE && !highContrast) Color(visorHex) else null,
          auraTrailId = activeOutfit.auraId,
        )

        // Full-Arena 2.2x2.2 Mini-Radar showing buildings, traps, hostiles, bots & player
        if (settings.showMiniRadar && !settings.simplifiedHud) {
          val radarR = 38f
          val radarCenter = Offset(w - radarR - 14f, radarR + 14f)
          drawCircle(Color(0xCC080D1A), radius = radarR, center = radarCenter)
          drawCircle(palette.playerAccent.copy(alpha = 0.75f), radius = radarR, center = radarCenter, style = Stroke(width = 2f))
          for (bi in 0 until sim.buildings.size) {
            val b = sim.buildings[bi]
            val rx = radarCenter.x + (((b.x + b.width * 0.5f) / ARENA_WORLD_SIZE) - 0.5f) * radarR * 1.7f
            val ry = radarCenter.y + (((b.y + b.height * 0.5f) / ARENA_WORLD_SIZE) - 0.5f) * radarR * 1.7f
            drawCircle(if (b.explored) Color(0xFF00E676) else b.accentColor, radius = 2.2f, center = Offset(rx, ry))
          }
          drawCircle(
            palette.radarPlayer,
            radius = 3.8f,
            center = Offset(
              radarCenter.x + ((sim.playerX / ARENA_WORLD_SIZE) - 0.5f) * radarR * 1.7f,
              radarCenter.y + ((sim.playerY / ARENA_WORLD_SIZE) - 0.5f) * radarR * 1.7f,
            ),
          )
          for (i in 0 until sim.hostiles.size) {
            val e = sim.hostiles[i]
            drawCircle(
              color = if (e.isBoss) palette.boss else palette.hostile,
              radius = if (e.isBoss) 3.8f else 2.2f,
              center = Offset(
                radarCenter.x + ((e.x / ARENA_WORLD_SIZE) - 0.5f) * radarR * 1.7f,
                radarCenter.y + ((e.y / ARENA_WORLD_SIZE) - 0.5f) * radarR * 1.7f,
              ),
            )
          }
        }

        if (sim.damageFlashTicks > 0f && !settings.reduceFlashes) {
          drawRect(color = palette.enemyShot.copy(alpha = 0.35f), topLeft = Offset.Zero, size = Size(w, h), style = Stroke(width = 22f))
        }
      }

      // Boss Health Banner & Action Callouts
      Column(
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        val bossName = sim.hudBossName
        if (bossName != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xE81A0812),
            modifier = Modifier.border(1.5.dp, palette.hostile, RoundedCornerShape(10.dp)),
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(bossName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.hostile)
              Spacer(Modifier.height(3.dp))
              LinearProgressIndicator(
                progress = { sim.hudBossHpRatio },
                color = palette.hostile,
                trackColor = Color(0xFF2B121B),
                modifier = Modifier.width(190.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
              )
            }
          }
        }

        if (settings.alwaysShowActionCues && (sim.hudCallout != null || mode.id == "dmz" || sim.hudShield < sim.loadout.maxShield / 3)) {
          val captionText = sim.hudCallout ?: when {
            sim.hudShield < sim.loadout.maxShield / 3 -> "LOW SHIELD — TAP SURGE / TACTICAL / DASH!"
            mode.id == "dmz" && sim.hudIntelCollected >= 3 -> "EXFIL READY — Reach Northern Beacon!"
            mode.id == "dmz" -> "DMZ Intel Cores: ${sim.hudIntelCollected} / 3"
            else -> ""
          }
          if (captionText.isNotEmpty()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = hudSurfaceBg,
              modifier = Modifier.border(1.dp, palette.playerAccent.copy(alpha = 0.65f), RoundedCornerShape(10.dp)),
            ) {
              Text(
                text = captionText,
                style = MaterialTheme.typography.labelMedium,
                color = if (sim.hudOverdriveActive || sim.hudIntelCollected >= 3) palette.safeExfil else palette.playerAccent,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }
        }
      }

      if (mode.id == "sandbox" && !sim.hudRunEnded && !settings.simplifiedHud) {
        Row(
          modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(shape = RoundedCornerShape(10.dp), color = hudSurfaceBg) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedButton(
                onClick = { sim.spawnBossForWorld(sim.hudWave + 1L) },
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(8.dp),
              ) { Text("+Boss", style = MaterialTheme.typography.labelSmall) }
              OutlinedButton(
                onClick = {
                  sim.hudPackTier = 25
                  sim.maxPackTierReached = 25
                  sim.postCallout("SANDBOX PACK-A-TIER XXV (MULTIVERSAL) MAXED!")
                },
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(8.dp),
              ) { Text("Tier XXV", style = MaterialTheme.typography.labelSmall) }
              OutlinedButton(
                onClick = { sim.spawnRandomWonderWeapon() },
                modifier = Modifier.height(36.dp).testTag("sandbox_spawn_wonder_btn"),
                shape = RoundedCornerShape(8.dp),
              ) { Text("+Wonder", style = MaterialTheme.typography.labelSmall) }
              OutlinedButton(
                onClick = {
                  sim.powerCd = 0f
                  sim.interactCd = 0f
                  sim.consumableCd = 0f
                  sim.lethalCd = 0f
                  sim.tacticalCd = 0f
                  sim.overdriveTicks = 300f
                  sim.postCallout("ALL COOLDOWNS RESET!")
                },
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(8.dp),
              ) { Text("Reset CD", style = MaterialTheme.typography.labelSmall) }
            }
          }
        }
      }

      if (sim.hudRunEnded) {
        Surface(
          color = Color(0xE6070A12),
          modifier = Modifier.fillMaxSize(),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(
              text = if (sim.hudExtractedSuccess) "EXFILTRATION COMPLETE" else "DEPLOYMENT ENDED",
              style = MaterialTheme.typography.displaySmall,
              color = if (sim.hudExtractedSuccess) palette.safeExfil else palette.playerAccent,
            )
            Spacer(Modifier.height(8.dp))
            Text(
              text = "Score: ${sim.hudScore} • Round ${sim.hudWave} • ${sim.hudElims} Elims • ${sim.buildingsExploredCount}/${sim.buildings.size} Buildings • +${sim.blastShardsEarned} Shards",
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
            )
            Spacer(Modifier.height(18.dp))
            Button(
              onClick = {
                onRecordProgression(sim.buildProgressionReport())
                onSaveMatchAndExit(
                  sim.hudScore,
                  sim.hudWave,
                  sim.hudElims,
                  sim.hudExtractedSuccess,
                  sim.elapsedTicks / 60L,
                )
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.height(52.dp).testTag("save_match_result_btn"),
            ) {
              Text("Claim Essence & Save Run", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    }

    // Bottom Responsive Gamepad & Multi-Touch Deck (zero-slop joystick + scrollable full action rows)
    Surface(
      color = hudSurfaceBg,
      modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      val stickDiameter = (92f * settings.joystickScale).dp
      val puckDiameter = (34f * settings.joystickScale).dp
      val btnHeight = (44f * settings.buttonScale).dp

      val stickWidget: @Composable () -> Unit = {
        Box(
          modifier = Modifier
            .size(stickDiameter)
            .clip(CircleShape)
            .background(Color(0xFF141E36))
            .border(1.5.dp, palette.playerAccent.copy(alpha = 0.75f), CircleShape)
            .testTag("arena_joystick")
            .pointerInput(sim, settings.joystickScale) {
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                fun updateStick(posX: Float, posY: Float) {
                  val cx = size.width / 2f
                  val cy = size.height / 2f
                  val nx = ((posX - cx) / cx.coerceAtLeast(1f)).coerceIn(-1f, 1f)
                  val ny = ((posY - cy) / cy.coerceAtLeast(1f)).coerceIn(-1f, 1f)
                  sim.joyDx = nx
                  sim.joyDy = ny
                  thumbOffsetX = nx * 24f * settings.joystickScale
                  thumbOffsetY = ny * 24f * settings.joystickScale
                }
                down.consume()
                updateStick(down.position.x, down.position.y)
                while (true) {
                  val event = awaitPointerEvent()
                  val ch = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                  if (ch == null || !ch.pressed) {
                    sim.joyDx = 0f
                    sim.joyDy = 0f
                    thumbOffsetX = 0f
                    thumbOffsetY = 0f
                    break
                  }
                  ch.consume()
                  updateStick(ch.position.x, ch.position.y)
                }
              }
            },
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier
              .offset { IntOffset(thumbOffsetX.roundToInt(), thumbOffsetY.roundToInt()) }
              .size(puckDiameter)
              .clip(CircleShape)
              .background(palette.playerAccent)
          )
        }
      }

      val buttonsAlign = if (settings.leftHandedDeck) Alignment.Start else Alignment.End
      val actionsWidget: @Composable () -> Unit = {
        Column(
          horizontalAlignment = buttonsAlign,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!settings.simplifiedHud) {
              Text(
                text = console.buttonLegend.take(2).joinToString(" • ") + if (sim.hudOverdriveActive) " • OVERDRIVE" else "",
                style = MaterialTheme.typography.labelSmall,
                color = if (sim.hudOverdriveActive) palette.dropBuff else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            FilterChip(
              selected = sim.hudAutoFire,
              onClick = {
                triggerHaptic()
                sim.hudAutoFire = !sim.hudAutoFire
              },
              label = { Text("AUTO", style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.height(32.dp).testTag("arena_autofire_chip"),
            )
          }
          Spacer(Modifier.height(5.dp))
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Button(
              onClick = { triggerHaptic(); sim.fireVolley(forceManual = true) },
              colors = ButtonDefaults.buttonColors(containerColor = palette.playerAccent, contentColor = Color.Black),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(btnHeight).testTag("arena_fire_btn"),
            ) { Text("FIRE") }
            Button(
              onClick = { triggerHaptic(); sim.triggerPower() },
              enabled = sim.hudPowerCdSec == 0,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(btnHeight).testTag("arena_power_btn"),
            ) { Text(if (sim.hudPowerCdSec == 0) "POWER" else "${sim.hudPowerCdSec}s") }
            Button(
              onClick = { triggerHaptic(); sim.upgradePackAPunch() },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (sim.hudPackTier >= 25) Color(0xFF00E5FF) else Color(0xFFE040FB),
                contentColor = Color.Black,
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(btnHeight).testTag("arena_pack_btn"),
            ) { Text("PACK ${formatRoundRoman(sim.hudPackTier.toLong())}") }
            OutlinedButton(
              onClick = { triggerHaptic(); sim.cycleActiveWeapon() },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(btnHeight).testTag("arena_swap_wpn_btn"),
            ) { Text("WPN ${sim.activeWeaponIndex + 1}/${sim.equippedWeaponIds.size}") }
            OutlinedButton(
              onClick = { triggerHaptic(); sim.activateEquippedWonderSlot() },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(btnHeight).testTag("arena_wonder_swap_btn"),
            ) { Text("WNDR") }
          }
          Spacer(Modifier.height(5.dp))
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            OutlinedButton(
              onClick = { triggerHaptic(); sim.triggerAllTraps() },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(btnHeight).testTag("arena_trap_btn"),
            ) { Text("TRAPS", color = Color(0xFFFFAB40), style = MaterialTheme.typography.labelSmall) }
            OutlinedButton(
              onClick = { triggerHaptic(); sim.triggerConsumable() },
              enabled = sim.hudConsumableCdSec == 0,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(btnHeight).testTag("arena_surge_btn"),
            ) { Text(if (sim.hudConsumableCdSec == 0) "SURGE" else "${sim.hudConsumableCdSec}s", style = MaterialTheme.typography.labelSmall) }
            OutlinedButton(
              onClick = { triggerHaptic(); sim.triggerWorldInteract() },
              enabled = sim.hudInteractCdSec == 0,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(btnHeight).testTag("arena_interact_btn"),
            ) { Text(if (sim.hudInteractCdSec == 0) world.interactActionLabel else "${sim.hudInteractCdSec}s", style = MaterialTheme.typography.labelSmall) }
            OutlinedButton(
              onClick = { triggerHaptic(); sim.triggerLethal() },
              enabled = sim.hudLethalCdSec == 0,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(btnHeight).testTag("arena_lethal_btn"),
            ) { Text(if (sim.hudLethalCdSec == 0) sim.hudLethalName else "${sim.hudLethalCdSec}s", style = MaterialTheme.typography.labelSmall) }
            OutlinedButton(
              onClick = { triggerHaptic(); sim.triggerTactical() },
              enabled = sim.hudTacticalCdSec == 0,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(btnHeight).testTag("arena_tactical_btn"),
            ) { Text(if (sim.hudTacticalCdSec == 0) sim.hudTacticalName else "${sim.hudTacticalCdSec}s", style = MaterialTheme.typography.labelSmall) }
            OutlinedButton(
              onClick = { triggerHaptic(); sim.triggerDash() },
              enabled = sim.hudDashReady,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(btnHeight).testTag("arena_dash_btn"),
            ) { Text(if (sim.loadout.hasClawEdge) "CLAW" else "DASH", style = MaterialTheme.typography.labelSmall) }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp).testTag("arena_controls_deck"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (settings.leftHandedDeck) {
          Box(modifier = Modifier.weight(1f)) { actionsWidget() }
          stickWidget()
        } else {
          stickWidget()
          Box(modifier = Modifier.weight(1f)) { actionsWidget() }
        }
      }
    }

    if (showTacticalLiveChatDialog) {
      val liveMessages = progression.chatMessages.filter { it.channel == ChatChannel.LIVE_MATCH }
      var liveInput by remember { mutableStateOf("") }
      AlertDialog(
        onDismissRequest = { showTacticalLiveChatDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(AppIcons.Chat, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
            Text("LIVE COMBAT TACTICAL COMMS", fontWeight = FontWeight.Bold)
          }
        },
        text = {
          Column(modifier = Modifier.fillMaxWidth().height(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("1-TAP QUICK CALLOUTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(SocialCatalog.quickTacticalCallouts) { callout ->
                Button(
                  onClick = {
                    onSendChatMessage(ChatChannel.LIVE_MATCH, callout, null, true)
                  },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  modifier = Modifier.height(34.dp).testTag("arena_callout_${callout.take(8).trim()}"),
                ) {
                  Text(callout, style = MaterialTheme.typography.labelSmall)
                }
              }
            }

            LazyColumn(
              modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              items(liveMessages.takeLast(25), key = { it.id }) { msg ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(
                    text = "${msg.senderCallsign}: ${msg.content}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (msg.isTacticalQuickChat) Color(0xFFFF9100) else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (msg.isTacticalQuickChat) FontWeight.Bold else FontWeight.Normal,
                  )
                }
              }
            }

            OutlinedTextField(
              value = liveInput,
              onValueChange = { liveInput = it },
              placeholder = { Text("Send combat message...") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("arena_live_chat_input"),
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              if (liveInput.isNotBlank()) {
                onSendChatMessage(ChatChannel.LIVE_MATCH, liveInput, null, false)
                liveInput = ""
              }
            },
            modifier = Modifier.testTag("arena_send_live_chat_btn"),
          ) {
            Text("Send")
          }
        },
        dismissButton = {
          TextButton(onClick = { showTacticalLiveChatDialog = false }) {
            Text("Close")
          }
        },
      )
    }
  }
}

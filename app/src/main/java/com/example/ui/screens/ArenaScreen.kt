package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.runtime.mutableStateOf
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
import com.example.util.GamepadInputBridge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

import com.example.data.model.MultiverseArsenalCatalog

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
  onSelectArea: ((String) -> Unit)? = null,
  onSelectMode: ((String) -> Unit)? = null,
  onUpdateCollectionCustomMode: (((com.example.data.model.CollectionCustomModeConfig) -> com.example.data.model.CollectionCustomModeConfig) -> Unit)? = null,
  onDeployCollectionCustomMode: (() -> Unit)? = null,
  externalSimProvider: ((String, () -> ArenaSimState) -> ArenaSimState)? = null,
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

  val cm = progression.collectionCustomMode
  val customKeySuffix = if (cm.isActiveForArea(progression.activeAreaId)) {
    "cm_${cm.scope.id}_${cm.safeCollectionGuard}_${cm.negativeImpactPercent}_${cm.enemyArmorFlood}_${cm.collectionAmbushSurge}"
  } else {
    "cm_off"
  }
  val simSessionKey = "${world.id}|${mode.id}|${progression.activeAreaId}|${progression.joinedMatch?.id ?: ""}|$customKeySuffix"
  val sim = remember(simSessionKey) {
    val factory = {
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
    externalSimProvider?.invoke(simSessionKey, factory) ?: factory()
  }

  LaunchedEffect(loadout) {
    val curWeaponId = sim.equippedWeaponIds.getOrElse(sim.activeWeaponIndex) { loadout.weaponSpec.id }
    sim.loadout = if (curWeaponId == loadout.weaponSpec.id) {
      loadout
    } else {
      ArenaLoadoutStats.fromArmory(armory, Color(world.accentHex), progression, profile.totalEssence, curWeaponId)
    }
    sim.hudActiveWeaponName = sim.loadout.weaponSpec.name
  }

  LaunchedEffect(sim, sim.hudRunEnded) {
    if (sim.hudRunEnded && sim.markProgressionReported()) {
      onRecordProgression(sim.buildProgressionReport())
    }
  }

  DisposableEffect(sim) {
    onDispose {
      if ((sim.hudElims > 0 || sim.hudScore > 0 || sim.buildingsExploredCount > 0 || sim.elapsedTicks > 180L) &&
        sim.markProgressionReported()
      ) {
        onRecordProgression(sim.buildProgressionReport())
      }
    }
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
  var showMapSwitcherDialog by remember { mutableStateOf(false) }
  var showCustomModeDialog by remember { mutableStateOf(false) }
  val gamepad by GamepadInputBridge.state.collectAsStateWithLifecycle()

  LaunchedEffect(
    gamepad.connected,
    gamepad.leftX,
    gamepad.leftY,
    gamepad.rightX,
    gamepad.rightY,
    gamepad.r2Trigger,
    gamepad.l2Trigger,
  ) {
    if (!gamepad.connected || isPausedForSettings || sim.hudRunEnded) return@LaunchedEffect
    sim.joyDx = gamepad.leftX
    sim.joyDy = gamepad.leftY
    thumbOffsetX = gamepad.leftX * 22f * settings.joystickScale
    thumbOffsetY = gamepad.leftY * 22f * settings.joystickScale

    val rx = gamepad.rightX
    val ry = gamepad.rightY
    val rMagSq = rx * rx + ry * ry
    if (rMagSq > 0.04f) {
      val len = sqrt(rMagSq).coerceAtLeast(0.01f)
      sim.aimX = rx / len
      sim.aimY = ry / len
      sim.fireVolley()
    } else if (gamepad.r2Trigger > 0.35f) {
      sim.fireVolley()
    }
    if (gamepad.l2Trigger > 0.40f) {
      sim.triggerPower()
    }
  }

  LaunchedEffect(gamepad.actionSeq) {
    if (gamepad.actionSeq <= 0L || isPausedForSettings || sim.hudRunEnded) return@LaunchedEffect
    when (gamepad.lastActionButtonCode) {
      KeyEvent.KEYCODE_BUTTON_A -> sim.triggerDash()
      KeyEvent.KEYCODE_BUTTON_X -> sim.interactNearestWorldObject()
      KeyEvent.KEYCODE_BUTTON_Y -> sim.activateEquippedWonderSlot()
      KeyEvent.KEYCODE_BUTTON_B -> sim.triggerConsumable()
      KeyEvent.KEYCODE_BUTTON_L1 -> sim.triggerLethal()
      KeyEvent.KEYCODE_BUTTON_R1 -> sim.cycleActiveWeapon()
      KeyEvent.KEYCODE_BUTTON_THUMBR -> sim.triggerAllTraps()
    }
  }

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

  LaunchedEffect(sim, isPausedForSettings, showMapSwitcherDialog, settings.targetFpsCap, settings.showFpsCounter) {
    runCatching { focusRequester.requestFocus() }
    if (isPausedForSettings || showMapSwitcherDialog) return@LaunchedEffect

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
      .windowInsetsPadding(WindowInsets.displayCutout)
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
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_BUTTON_X -> { sim.interactNearestWorldObject(); true }
            KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_BUTTON_Y -> { sim.triggerConsumable(); true }
            KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_BUTTON_L1 -> { sim.interactNearestWorldObject(); true }
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
        var showPlacesBar by remember { mutableStateOf(false) }
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
              val densStr = String.format(Locale.US, "%.2fx", progression.levelCustom.enemyDensityMultiplier)
              Text(
                text = "${activeAreaSpec.name.substringBefore(" (")} • ${mode.shortTag} • PACK ${formatRoundRoman(sim.hudPackTier.toLong())} • COL-${sim.collectionDifficulty.shortTag} • ${sim.ngPlusSpec.shortTag} $densStr",
                style = MaterialTheme.typography.titleSmall,
                color = palette.playerAccent,
                maxLines = 1,
              )
              Text(
                text = "PLACE: ${sim.hudCurrentPlaceName} • SCORE ${sim.hudScore} • ELIMS $elimTarget • SHD ${sim.hudShield}/${sim.loadout.maxShield}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
              )
              Text(
                text = "${activeOutfit.nameWithVersion} (${sim.loadout.characterRarity.label}) • ${sim.loadout.activePassVersion.title.substringBefore(":")} • LV ${sim.loadout.playerLevel} (RWD x${String.format(Locale.US, "%.2f", sim.gameplayRewardMultiplier)}) • ${sim.hudActiveWeaponName}",
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
            if (progression.collectionCustomMode.isActiveForArea(progression.activeAreaId)) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFF9100).copy(alpha = 0.22f),
                modifier = Modifier.border(1.dp, Color(0xFFFF9100), RoundedCornerShape(6.dp)).testTag("arena_custom_mode_badge"),
              ) {
                Text(
                  text = "CUSTOM -${progression.collectionCustomMode.negativeImpactPercent}%",
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
            if (onSelectArea != null) {
              OutlinedButton(
                onClick = { showMapSwitcherDialog = true },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(48.dp).testTag("arena_map_switcher_btn"),
              ) {
                Text("Maps", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }
            OutlinedButton(
              onClick = { showPlacesBar = !showPlacesBar },
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(48.dp).testTag("arena_places_toggle_btn"),
            ) {
              Text(if (showPlacesBar) "Hide Places" else "Places", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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

        if (showPlacesBar) {
          val places = remember(sim) { sim.placeDestinations() }
          @OptIn(ExperimentalFoundationApi::class)
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().testTag("arena_places_strip"),
          ) {
            items(places, key = { it.id }) { dest ->
              val isHere = sim.hudCurrentPlaceName == dest.label
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isHere) dest.accent.copy(alpha = 0.25f) else Color(0xFF10192D),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .combinedClickable(
                    onClick = { triggerHaptic(); sim.travelToPlace(dest.id) },
                    onLongClick = { triggerHaptic(); sim.travelToPlace(dest.id) },
                  )
                  .border(
                    width = if (isHere) 1.5.dp else 1.dp,
                    color = if (isHere) dest.accent else dest.accent.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                  )
                  .testTag("arena_place_${dest.id}"),
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                  Text(
                    text = "${dest.kind}: ${dest.label}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isHere) dest.accent else Color.White,
                    maxLines = 1,
                  )
                }
              }
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
            sim.hudInRestCamp -> {
              Text(
                text = "CAMP SAFE ZONE: x3 SHIELD REGEN",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E676),
              )
            }
            sim.hudActiveHazardLabel != null -> {
              Text(
                text = "HAZARD: ${sim.hudActiveHazardLabel} (HIGH LOOT)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFAB40),
              )
            }
            sim.hudInOuterFrontier -> {
              Text(
                text = "OUTER RIFT FRONTIER • NO BOUNDARIES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE040FB),
              )
            }
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
                text = "VAC ${sim.hudVacuumChargePct}% • SHARDS:${progression.blastShards + sim.blastShardsEarned}",
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
              val (wx0, wy0) = screenToWorldCoords(down.position.x, down.position.y, cx0, cy0, w, h)
              sim.setTouchAim(wx0, wy0, active = true)
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
                val (wx, wy) = screenToWorldCoords(change.position.x, change.position.y, cx, cy, w, h)
                sim.setTouchAim(wx, wy, active = true)
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
          areaId = activeAreaSpec.id,
        )

        drawBuildingsAndTraps(
          sim = sim,
          camX = camX,
          camY = camY,
          glowOn = glowOn,
          pulse = pulse,
        )

        if (mode.id == "br") {
          val brC = worldToPerspectiveScreen(ARENA_CENTER, ARENA_CENTER, camX, camY, w, h)
          val brScale = perspectiveScaleForRelY(ARENA_CENTER - camY)
          val brR = sim.brRingRadius * minOf(w, h) * brScale
          drawOval(
            color = palette.dropHazard,
            topLeft = Offset(brC.x - brR, brC.y - brR * 0.72f),
            size = Size(brR * 2f, brR * 1.44f),
            style = Stroke(width = if (highContrast) 6f else 4f),
          )
        }

        if (mode.id == "dmz") {
          val exfilReady = sim.hudIntelCollected >= 3
          val exfilCenter = worldToPerspectiveScreen(ARENA_CENTER, ARENA_CENTER - 1.25f, camX, camY, w, h)
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
              val ip = worldToPerspectiveScreen(intel.x, intel.y, camX, camY, w, h)
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
          val pos = worldToPerspectiveScreen(d.x, d.y, camX, camY, w, h)
          val dScale = perspectiveScaleForRelY(d.y - camY)
          if (pos.x < -30f || pos.x > w + 30f || pos.y < -30f || pos.y > h + 30f) continue
          val dCol = when (d.kind) {
            ArenaDropKind.NUKE -> palette.playerAccent
            ArenaDropKind.INSTA_KILL -> palette.dropHazard
            ArenaDropKind.MAX_ARMOR -> palette.safeExfil
            ArenaDropKind.DOUBLE_ESSENCE -> palette.dropBuff
          }
          if (glowOn) drawCircle(dCol.copy(alpha = 0.24f + pulse * 0.18f), radius = 18f * dScale, center = pos)
          drawCircle(dCol, radius = (if (highContrast) 12f else 10f) * dScale, center = pos, style = Stroke(width = 3f))
          drawCircle(Color.White, radius = 4f * dScale, center = pos)
        }

        // Random Mystery Wonder Weapon Pickups
        for (i in 0 until sim.wonderPickups.size) {
          val wp = sim.wonderPickups[i]
          val pos = worldToPerspectiveScreen(wp.x, wp.y, camX, camY, w, h)
          val wpScale = perspectiveScaleForRelY(wp.y - camY)
          if (pos.x < -30f || pos.x > w + 30f || pos.y < -30f || pos.y > h + 30f) continue
          val wCol = Color(wp.kind.accentHex)
          val boxHalf = 11f * wpScale
          if (glowOn) drawCircle(wCol.copy(alpha = 0.28f + pulse * 0.22f), radius = 22f * wpScale, center = pos)
          drawRect(color = Color(0xFF0A1222), topLeft = Offset(pos.x - boxHalf, pos.y - boxHalf), size = Size(boxHalf * 2f, boxHalf * 2f))
          drawRect(color = wCol, topLeft = Offset(pos.x - boxHalf, pos.y - boxHalf), size = Size(boxHalf * 2f, boxHalf * 2f), style = Stroke(width = 3f))
          drawCircle(Color.White, radius = 4.5f * wpScale, center = pos)
        }

        if (sim.pulseRingRadius > 0f && !settings.reduceFlashes) {
          val pCenter = worldToPerspectiveScreen(sim.playerX, sim.playerY, camX, camY, w, h)
          val pR = sim.pulseRingRadius * minOf(w, h) * 0.8f
          drawOval(
            color = palette.playerAccent.copy(alpha = 0.65f),
            topLeft = Offset(pCenter.x - pR, pCenter.y - pR * 0.68f),
            size = Size(pR * 2f, pR * 1.36f),
            style = Stroke(width = 7f),
          )
        }

        // Arcs
        for (i in 0 until sim.arcs.size) {
          val arc = sim.arcs[i]
          val a = (arc.life / 18f).coerceIn(0f, 1f)
          val p1 = worldToPerspectiveScreen(arc.x1, arc.y1, camX, camY, w, h)
          val p2 = worldToPerspectiveScreen(arc.x2, arc.y2, camX, camY, w, h)
          if (glowOn) {
            drawLine(color = arc.color.copy(alpha = a * 0.78f), start = p1, end = p2, strokeWidth = 6f * a, cap = StrokeCap.Round)
          }
          drawLine(color = Color.White.copy(alpha = a), start = p1, end = p2, strokeWidth = 2.5f, cap = StrokeCap.Round)
        }

        // Projectiles
        for (i in 0 until sim.bolts.size) {
          val b = sim.bolts[i]
          val head = worldToPerspectiveScreen(b.x, b.y, camX, camY, w, h)
          val bScale = perspectiveScaleForRelY(b.y - camY)
          if (head.x < -30f || head.x > w + 30f || head.y < -30f || head.y > h + 30f) continue
          if (b.isEnemyShot) {
            if (glowOn) drawCircle(palette.enemyShot.copy(alpha = 0.45f), radius = 10f * bScale, center = head)
            drawCircle(palette.enemyShot, radius = (if (highContrast) 7f else 5f) * bScale, center = head)
          } else {
            val tail = worldToPerspectiveScreen(b.x - b.vx * 2.2f, b.y - b.vy * 2.2f, camX, camY, w, h)
            val boltCol = if (settings.colorBlindMode == ColorBlindMode.NONE) b.color else palette.playerAccent
            if (glowOn) {
              drawLine(boltCol.copy(alpha = 0.58f), tail, head, strokeWidth = (if (b.isSupercharged) 10f else 6.5f) * bScale, cap = StrokeCap.Round)
            }
            drawLine(Color.White, tail, head, strokeWidth = 3f * bScale, cap = StrokeCap.Round)
          }
        }

        // Impact Sparks
        if (settings.particleDensity != ParticleDensity.OFF) {
          for (i in 0 until sim.sparks.size) {
            val sp = sim.sparks[i]
            val spPos = worldToPerspectiveScreen(sp.x, sp.y, camX, camY, w, h)
            val spScale = perspectiveScaleForRelY(sp.y - camY)
            val r = (sp.maxLife - sp.life).coerceAtLeast(3f) * 1.8f * spScale
            drawCircle(
              color = sp.color.copy(alpha = (sp.life / sp.maxLife).coerceIn(0f, 1f)),
              radius = r,
              center = spPos,
              style = Stroke(width = 2.5f),
            )
          }
        }

        // Animated Lore & Original Hostiles & Bosses with 2.5D depth scaling
        val overrideEnemyCol = if (settings.colorBlindMode != ColorBlindMode.NONE || highContrast) palette.hostile else null
        for (i in 0 until sim.hostiles.size) {
          val e = sim.hostiles[i]
          val ePos = worldToPerspectiveScreen(e.x, e.y, camX, camY, w, h)
          if (ePos.x < -70f || ePos.x > w + 70f || ePos.y < -70f || ePos.y > h + 70f) continue
          val eScale = perspectiveScaleForRelY(e.y - camY)
          drawArenaHostile(
            center = ePos,
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
            scale = eScale,
            armor = e.armor,
            maxArmor = e.maxArmor,
            immuneColors = e.immuneTypes.map { it.color },
          )
        }

        // AI Lobby Bot Combatants
        for (i in 0 until sim.arenaBots.size) {
          val bot = sim.arenaBots[i]
          if (bot.hp <= 0) continue
          val botOrigin = worldToPerspectiveScreen(bot.x, bot.y, camX, camY, w, h)
          if (botOrigin.x < -50f || botOrigin.x > w + 50f || botOrigin.y < -50f || botOrigin.y > h + 50f) continue
          val botScale = perspectiveScaleForRelY(bot.y - camY)
          val botRingCol = if (bot.isAllied) palette.safeExfil else palette.dropBuff
          drawOval(
            color = botRingCol.copy(alpha = 0.72f),
            topLeft = Offset(botOrigin.x - 20f * botScale, botOrigin.y - 12f * botScale),
            size = Size(40f * botScale, 24f * botScale),
            style = Stroke(width = 2.4f),
          )
          val botSkel = RigEvaluator.evaluate(
            origin = botOrigin,
            scalePx = 56f * botScale,
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
          worldToPerspectiveScreen(sim.touchAimX, sim.touchAimY, camX, camY, w, h)
        } else {
          worldToPerspectiveScreen(sim.playerX + sim.aimX * 0.11f, sim.playerY + sim.aimY * 0.11f, camX, camY, w, h)
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
        val playerScreen = worldToPerspectiveScreen(sim.playerX, sim.playerY, camX, camY, w, h)
        val playerPerspScale = perspectiveScaleForRelY(sim.playerY - camY)
        val skel = RigEvaluator.evaluate(
          origin = playerScreen,
          scalePx = 74f * (activeRig?.rigScale?.toFloat() ?: 1f) * playerPerspScale,
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
          outfitId = activeOutfit.id,
        )

        // Player-Centered Open-World Mini-Radar (±2.6f local radius around player across Verdansk & Outer Frontier)
        if (settings.showMiniRadar && !settings.simplifiedHud) {
          val radarR = 42f
          val radarCenter = Offset(w - radarR - 14f, radarR + 14f)
          val radarScope = 2.6f
          drawCircle(Color(0xD2080D1A), radius = radarR, center = radarCenter)
          drawCircle(
            color = (if (sim.hudInOuterFrontier) Color(0xFFE040FB) else palette.playerAccent).copy(alpha = 0.80f),
            radius = radarR,
            center = radarCenter,
            style = Stroke(width = 2f),
          )
          fun plotRadar(wx: Float, wy: Float): Offset? {
            val dx = (wx - sim.playerX) / radarScope
            val dy = (wy - sim.playerY) / radarScope
            if (dx * dx + dy * dy > 0.96f) return null
            return Offset(radarCenter.x + dx * radarR, radarCenter.y + dy * radarR)
          }
          for (bi in 0 until sim.buildings.size) {
            val b = sim.buildings[bi]
            plotRadar(b.x + b.width * 0.5f, b.y + b.height * 0.5f)?.let { pt ->
              drawCircle(if (b.explored) Color(0xFF00E676) else b.accentColor, radius = 2.5f, center = pt)
            }
          }
          for (ci in 0 until sim.restCamps.size) {
            val camp = sim.restCamps[ci]
            plotRadar(camp.x, camp.y)?.let { pt ->
              drawCircle(Color(0xFF00E676), radius = 3f, center = pt, style = Stroke(width = 1.6f))
            }
          }
          for (si in 0 until sim.worldStations.size) {
            val st = sim.worldStations[si]
            plotRadar(st.x, st.y)?.let { pt ->
              drawCircle(st.kind.color, radius = 2.4f, center = pt)
            }
          }
          for (li in 0 until sim.lootCrates.size) {
            val cr = sim.lootCrates[li]
            if (!cr.opened) {
              plotRadar(cr.x, cr.y)?.let { pt ->
                drawCircle(Color(cr.rarity.colorHex), radius = 2.2f, center = pt)
              }
            }
          }
          for (ei in 0 until sim.catEasterEggs.size) {
            val cat = sim.catEasterEggs[ei]
            if (!cat.found) {
              plotRadar(cat.x, cat.y)?.let { pt ->
                drawCircle(Color(0xFFFFD54F), radius = 2.4f, center = pt)
              }
            }
          }
          for (i in 0 until sim.hostiles.size) {
            val e = sim.hostiles[i]
            plotRadar(e.x, e.y)?.let { pt ->
              drawCircle(
                color = if (e.isBoss) palette.boss else palette.hostile,
                radius = if (e.isBoss) 3.8f else 2.2f,
                center = pt,
              )
            }
          }
          drawCircle(palette.radarPlayer, radius = 3.8f, center = radarCenter)
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
            modifier = Modifier.border(1.5.dp, palette.hostile, RoundedCornerShape(10.dp)).testTag("arena_boss_banner"),
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(bossName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.hostile)
                sim.hudBossImmuneSummary?.let { imm ->
                  Text(
                    text = "IMMUNE:$imm",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F),
                    modifier = Modifier.testTag("arena_boss_immune_badge"),
                  )
                }
              }
              if (sim.hudBossArmorRatio > 0f) {
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(
                  progress = { sim.hudBossArmorRatio },
                  color = Color(0xFFB0BEC5),
                  trackColor = Color(0xFF1A2538),
                  modifier = Modifier.width(190.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                )
              }
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
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(8.dp),
              ) { Text("+Boss", style = MaterialTheme.typography.labelSmall) }
              OutlinedButton(
                onClick = {
                  sim.hudPackTier = 25
                  sim.maxPackTierReached = 25
                  sim.postCallout("SANDBOX PACK-A-TIER XXV (MULTIVERSAL) MAXED!")
                },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(8.dp),
              ) { Text("Tier XXV", style = MaterialTheme.typography.labelSmall) }
              OutlinedButton(
                onClick = { sim.spawnRandomWonderWeapon() },
                modifier = Modifier.height(48.dp).testTag("sandbox_spawn_wonder_btn"),
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
                modifier = Modifier.height(48.dp),
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
                if (sim.markProgressionReported()) {
                  onRecordProgression(sim.buildProgressionReport())
                }
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
      val btnHeight = (48f * settings.buttonScale).coerceAtLeast(48f).dp

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
            OutlinedButton(
              onClick = {
                triggerHaptic()
                sim.cycleElementalPrism()
              },
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.heightIn(min = 48.dp).testTag("arena_prism_cycle_btn"),
            ) {
              Text(
                text = if (sim.selectedElementOverride == null) "DMG:${sim.hudActiveDamageType.shortTag}" else "PRISM:${sim.hudActiveDamageType.shortTag}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = sim.hudActiveDamageType.color,
              )
            }
            FilterChip(
              selected = sim.hudAutoFire,
              onClick = {
                triggerHaptic()
                sim.hudAutoFire = !sim.hudAutoFire
              },
              label = { Text("AUTO", style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.heightIn(min = 48.dp).testTag("arena_autofire_chip"),
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
              onClick = { triggerHaptic(); sim.interactNearestWorldObject() },
              enabled = sim.hudInteractCdSec == 0 || sim.hudNearbyPrompt != null,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(btnHeight).testTag("arena_interact_btn"),
            ) {
              Text(
                text = sim.hudNearbyPrompt ?: if (sim.hudInteractCdSec == 0) world.interactActionLabel else "${sim.hudInteractCdSec}s",
                style = MaterialTheme.typography.labelSmall,
              )
            }
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
      var activeArenaChannel by remember { mutableStateOf(ChatChannel.LIVE_MATCH) }
      val channelMessages = progression.chatMessages.filter { it.channel == activeArenaChannel }
      var liveInput by remember { mutableStateOf("") }
      AlertDialog(
        onDismissRequest = { showTacticalLiveChatDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(AppIcons.Chat, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
            Text("LIVE COMBAT & CLAN COMMS", fontWeight = FontWeight.Bold)
          }
        },
        text = {
          Column(modifier = Modifier.fillMaxWidth().height(380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(listOf(ChatChannel.LIVE_MATCH, ChatChannel.CLAN, ChatChannel.GENERAL), key = { it.id }) { ch ->
                FilterChip(
                  selected = activeArenaChannel == ch,
                  onClick = { activeArenaChannel = ch },
                  label = {
                    Text(
                      when (ch) {
                        ChatChannel.LIVE_MATCH -> "LIVE TACTICAL"
                        ChatChannel.CLAN -> "CLAN [${progression.clan.tag}]"
                        else -> "GENERAL"
                      },
                      style = MaterialTheme.typography.labelSmall,
                    )
                  },
                  modifier = Modifier.testTag("arena_chat_channel_${ch.id}"),
                )
              }
            }
            if (activeArenaChannel == ChatChannel.LIVE_MATCH) {
              Text("1-TAP QUICK CALLOUTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SocialCatalog.quickTacticalCallouts) { callout ->
                  Button(
                    onClick = {
                      onSendChatMessage(ChatChannel.LIVE_MATCH, callout, null, true)
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("arena_callout_${callout.take(8).trim()}"),
                  ) {
                    Text(callout, style = MaterialTheme.typography.labelSmall)
                  }
                }
              }
            }

            LazyColumn(
              modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              items(channelMessages.takeLast(25), key = { it.id }) { msg ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(
                    text = "${msg.senderClanTag?.let { "[$it] " } ?: ""}${msg.senderCallsign}: ${msg.content}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (msg.isTacticalQuickChat) Color(0xFFFF9100) else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (msg.isTacticalQuickChat || msg.isSystemMessage) FontWeight.Bold else FontWeight.Normal,
                  )
                }
              }
            }

            OutlinedTextField(
              value = liveInput,
              onValueChange = { liveInput = it },
              placeholder = { Text("Message ${activeArenaChannel.title}...") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("arena_live_chat_input"),
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              if (liveInput.isNotBlank()) {
                onSendChatMessage(activeArenaChannel, liveInput, null, false)
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

    if (showMapSwitcherDialog && onSelectArea != null) {
      AlertDialog(
        onDismissRequest = { showMapSwitcherDialog = false },
        title = {
          Text("SWITCH ARENA MAP & MODE (${MultiverseArsenalCatalog.customAreas.size} MAPS)", fontWeight = FontWeight.Bold)
        },
        text = {
          Column(modifier = Modifier.fillMaxWidth().height(420.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onSelectMode != null) {
              Text("DEPLOYMENT MODE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(UniverseCatalog.modes, key = { it.id }) { m ->
                  FilterChip(
                    selected = mode.id == m.id,
                    onClick = { onSelectMode(m.id) },
                    label = { Text("${m.shortTag} • ${m.label}", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("arena_quick_mode_${m.id}"),
                  )
                }
              }
            }
            if (onUpdateCollectionCustomMode != null) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = {
                    onUpdateCollectionCustomMode { cfg -> cfg.copy(enabled = !cfg.enabled) }
                  },
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("arena_custom_mode_toggle"),
                ) {
                  Text(
                    text = if (progression.collectionCustomMode.enabled) "Custom Mode: ON" else "Custom Mode: OFF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                  )
                }
                OutlinedButton(
                  onClick = {
                    showMapSwitcherDialog = false
                    showCustomModeDialog = true
                  },
                  modifier = Modifier.heightIn(min = 48.dp).testTag("arena_open_custom_mode_btn"),
                ) {
                  Text("Configure", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
              }
            }
            Text("ALL MAPS & BO3 CUSTOM ARENAS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            LazyColumn(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              items(MultiverseArsenalCatalog.customAreas, key = { it.id }) { area ->
                val selected = area.id == progression.activeAreaId
                val accent = Color(area.accentHex)
                Surface(
                  onClick = {
                    onSelectArea(area.id)
                    showMapSwitcherDialog = false
                  },
                  shape = RoundedCornerShape(12.dp),
                  color = if (selected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .border(if (selected) 2.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .testTag("arena_quick_map_${area.id}"),
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(area.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
                    Text("${area.storyEraNote} • ${area.buildingNames.take(3).joinToString(" / ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showMapSwitcherDialog = false }) {
            Text("Done")
          }
        },
      )
    }

    if (showCustomModeDialog && onUpdateCollectionCustomMode != null) {
      CollectionCustomModeDialog(
        initialConfig = progression.collectionCustomMode,
        onSave = onUpdateCollectionCustomMode,
        onDeploy = onDeployCollectionCustomMode ?: { showCustomModeDialog = false },
        onDismiss = { showCustomModeDialog = false },
      )
    }
  }
}

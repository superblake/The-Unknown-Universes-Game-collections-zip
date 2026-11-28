package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import com.example.data.local.CollectionResolutionMode
import com.example.data.local.ColorBlindMode
import com.example.data.local.GraphicsQuality
import com.example.data.local.LocalGameStrings
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
  onUpdateSettings: (((AppSettings) -> AppSettings) -> Unit)? = null,
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
  val activeAreaSpec = remember(progression.activeAreaId, progression.customGameMaps) { progression.activeAreaSpec() }
  val activeAreaState = remember(progression.activeAreaId, progression.areaStates) { progression.areaState() }

  val loadout = remember(armory, activeAreaSpec.accentHex, progression, profile.totalEssence, mode.id) {
    ArenaLoadoutStats.fromArmory(
      armory = armory,
      fallbackAccent = Color(activeAreaSpec.accentHex),
      progression = progression,
      blueEssence = profile.totalEssence,
      activeModeId = mode.id,
    )
  }
  val hasConsumable = remember(armory) { armory.any { it.equipped && it.category == "consumable" } }

  val cm = progression.collectionCustomMode
  val customKeySuffix = if (cm.isActiveForArea(progression.activeAreaId, progression.customGameMaps)) {
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

  LaunchedEffect(loadout, mode.id) {
    val curWeaponId = sim.equippedWeaponIds.getOrElse(sim.activeWeaponIndex) { loadout.weaponSpec.id }
    sim.loadout = if (curWeaponId == loadout.weaponSpec.id) {
      loadout
    } else {
      ArenaLoadoutStats.fromArmory(armory, Color(activeAreaSpec.accentHex), progression, profile.totalEssence, curWeaponId, activeModeId = mode.id)
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

  val palette = remember(activeAreaSpec.accentHex, activeAreaSpec.secondaryHex, settings.colorBlindMode, settings.highContrastMode, settings.themeMode) {
    resolveArenaPalette(Color(activeAreaSpec.accentHex), Color(activeAreaSpec.secondaryHex), settings)
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
  var showQuickControllerDialog by remember { mutableStateOf(false) }
  var showFieldUpgradesDialog by remember { mutableStateOf(false) }
  val gamepad by GamepadInputBridge.state.collectAsStateWithLifecycle()

  fun executeControllerAction(actionId: String) {
    when (actionId) {
      "fire" -> sim.fireVolley(forceManual = true)
      "melee" -> sim.triggerMeleeStrike()
      "power" -> sim.triggerPower()
      "magic" -> sim.triggerMagicSpell()
      "spirit_heal" -> sim.triggerSpiritThoughtHeal()
      "aegis_ward" -> sim.triggerAegisBarrier()
      "dash" -> sim.triggerDash()
      "interact" -> sim.interactNearestWorldObject()
      "surge" -> sim.triggerConsumable()
      "lethal" -> sim.triggerLethal()
      "tactical" -> sim.triggerTactical()
      "wonder" -> sim.activateEquippedWonderSlot()
      "traps" -> sim.triggerAllTraps()
      "swap_wpn" -> sim.cycleActiveWeapon()
      "pack" -> sim.upgradePackAPunch()
      "base_raid" -> sim.triggerBaseDefenseRaid()
      "spell_cycle" -> sim.cycleMagicSpell()
      else -> sim.fireVolley(forceManual = true)
    }
  }

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
      executeControllerAction(settings.controllerActionR2)
    }
    if (gamepad.l2Trigger > 0.40f) {
      executeControllerAction(settings.controllerActionL2)
    }
  }

  LaunchedEffect(gamepad.actionSeq) {
    if (gamepad.actionSeq <= 0L || isPausedForSettings || sim.hudRunEnded) return@LaunchedEffect
    when (gamepad.lastActionButtonCode) {
      KeyEvent.KEYCODE_BUTTON_A -> executeControllerAction(settings.controllerActionA)
      KeyEvent.KEYCODE_BUTTON_X -> executeControllerAction(settings.controllerActionX)
      KeyEvent.KEYCODE_BUTTON_Y -> executeControllerAction(settings.controllerActionY)
      KeyEvent.KEYCODE_BUTTON_B -> executeControllerAction(settings.controllerActionB)
      KeyEvent.KEYCODE_BUTTON_L1 -> executeControllerAction(settings.controllerActionL1)
      KeyEvent.KEYCODE_BUTTON_R1 -> executeControllerAction(settings.controllerActionR1)
      KeyEvent.KEYCODE_BUTTON_THUMBR -> sim.triggerMeleeStrike()
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
        try {
          sim.stepFrame(dtSec)
        } catch (e: Throwable) {
          android.util.Log.e("ArenaScreen", "Suppressed simulation step error", e)
        }
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

  val resMode = settings.collectionResolutionMode
  activeArenaZoomScale = resMode.arenaZoomScale
  activeArenaResolutionScale = when (resMode) {
    CollectionResolutionMode.SD_480P -> 0.86f
    CollectionResolutionMode.QHD_540P -> 0.90f
    CollectionResolutionMode.HD_720 -> 0.94f
    CollectionResolutionMode.WXGA_800 -> 0.96f
    CollectionResolutionMode.HD_PLUS_900 -> 0.98f
    CollectionResolutionMode.AUTO, CollectionResolutionMode.FHD_1080 -> 1.00f
    CollectionResolutionMode.FHD_PLUS_20_9 -> 1.02f
    CollectionResolutionMode.TABLET_WUXGA_1200 -> 1.06f
    CollectionResolutionMode.QHD_1440 -> 1.12f
    CollectionResolutionMode.WQXGA_1600 -> 1.14f
    CollectionResolutionMode.ULTRAWIDE_1440 -> 1.16f
    CollectionResolutionMode.UHD_4K -> 1.20f
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(if (settings.effectiveHighContrast) Color.Black else Color(0xFF060911)),
  ) {
    val rightActionMaxWidth = (maxWidth * 0.58f).coerceAtLeast(180.dp)
    val isCompactHeight = maxHeight < 580.dp
  Column(
    modifier = Modifier
      .fillMaxSize()
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
            KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_BUTTON_THUMBR -> { sim.triggerMeleeStrike(); true }
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> { sim.toggleStance(); true }
            KeyEvent.KEYCODE_L -> { sim.toggleLockOn(); true }
            KeyEvent.KEYCODE_P -> { sim.upgradePackAPunch(); true }
            KeyEvent.KEYCODE_M -> { sim.triggerMagicSpell(); true }
            KeyEvent.KEYCODE_H -> { sim.triggerSpiritThoughtHeal(); true }
            KeyEvent.KEYCODE_K -> { sim.triggerAegisBarrier(); true }
            KeyEvent.KEYCODE_U -> { showFieldUpgradesDialog = !showFieldUpgradesDialog; true }
            KeyEvent.KEYCODE_B -> { sim.triggerBaseDefenseRaid(); true }
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
      Column(
        modifier = Modifier
          .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
          .padding(horizontal = 10.dp, vertical = if (isCompactHeight) 4.dp else 6.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 3.dp else 4.dp),
      ) {
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
                text = "SEC: ${sim.hudCurrentSectionName} • SCORE ${sim.hudScore} • ELIMS $elimTarget • SHD ${sim.hudShield}/${sim.effectiveMaxShield()} • MP ${sim.hudMana.toInt()}/${sim.hudManaMax.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
              )
              Text(
                text = "DOA x${String.format(Locale.US, "%.1f", sim.hudDoaMultiplier)} • ${sim.hudEpoch.shortTag} • LV ${sim.hudMatchLevel} • PLATES ${sim.hudArmorPlates}/${sim.hudMaxArmorPlates} • TURRETS ${sim.hudTurretCount} • BASE ${sim.hudBaseHp}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(sim.hudEpoch.accentHex),
                modifier = Modifier.testTag("arena_survival_protection_hud"),
                maxLines = 1,
              )
              Text(
                text = "${activeOutfit.nameWithVersion} • 🚗 ${sim.activeVehicleSpec?.name ?: "Escort"} • ${sim.hudActiveWeaponName} • G:${sim.goldEarned} D:${sim.availableDiamondsInMatch()} $:${sim.availableTuuCashInMatch()}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(sim.loadout.activePassVersion.accentHex),
                modifier = Modifier.testTag("arena_level_weapon_hud"),
                maxLines = 1,
              )
            }
          }

          Spacer(Modifier.width(6.dp))
          Row(
            modifier = Modifier.widthIn(max = rightActionMaxWidth).horizontalScroll(rememberScrollState()),
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
            val customActiveHere = progression.collectionCustomMode.isActiveForArea(progression.activeAreaId, progression.customGameMaps)
            if (customActiveHere || onUpdateCollectionCustomMode != null) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (customActiveHere) Color(0xFFFF9100).copy(alpha = 0.22f) else Color(0xFF00E5FF).copy(alpha = 0.16f),
                modifier = Modifier
                  .border(
                    1.dp,
                    if (customActiveHere) Color(0xFFFF9100) else Color(0xFF00E5FF).copy(alpha = 0.7f),
                    RoundedCornerShape(8.dp),
                  )
                  .clip(RoundedCornerShape(8.dp))
                  .let { mod ->
                    if (onUpdateCollectionCustomMode != null) {
                      mod.clickable {
                        onUpdateCollectionCustomMode { cfg -> cfg.copy(enabled = !cfg.enabled) }
                      }
                    } else mod
                  }
                  .testTag(if (customActiveHere) "arena_custom_mode_badge" else "arena_normal_mode_badge"),
              ) {
                Text(
                  text = if (customActiveHere) {
                    "CUSTOM -${progression.collectionCustomMode.negativeImpactPercent}%"
                  } else if (progression.collectionCustomMode.enabled) {
                    "NORMAL (SCOPE)"
                  } else {
                    "NORMAL"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (customActiveHere) Color(0xFFFF9100) else Color(0xFF00E5FF),
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
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
              onClick = { showFieldUpgradesDialog = true },
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(48.dp).testTag("arena_field_upgrades_btn"),
            ) {
              Text(
                text = if (sim.hudPendingDraftCount > 0) "UPG (${sim.hudPendingDraftCount})" else "UPG/AGE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (sim.hudPendingDraftCount > 0) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary,
              )
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
            items(places.distinctBy { it.id }, key = { it.id }) { dest ->
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

        // Segmented Shield Bar + Survival XP Bar + Building / Wonder / Resonance / Multi-Essence Strip
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(if (settings.effectiveHighContrast) 10.dp else 7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1C2742))
            ) {
              val pct = (sim.hudShield.toFloat() / sim.effectiveMaxShield().coerceAtLeast(1)).let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0f }
              Box(
                modifier = Modifier
                  .fillMaxWidth(pct)
                  .height(if (settings.effectiveHighContrast) 10.dp else 7.dp)
                  .background(if (sim.hudAegisDomeActive) Color(0xFF38BDF8) else if (sim.hudOverdriveActive) palette.dropBuff else palette.playerAccent)
              )
            }
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF0E1B24))
            ) {
              val xpPct = (sim.hudMatchXp.toFloat() / sim.hudMatchXpNext.coerceAtLeast(1L)).let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0f }
              Box(
                modifier = Modifier
                  .fillMaxWidth(xpPct)
                  .height(4.dp)
                  .background(Color(0xFF69F0AE))
              )
            }
          }
          val activeWonder = sim.hudActiveWonder
          val dropText = sim.hudActiveDropText
          val bld = sim.hudActiveBuildingName
          when {
            sim.hudBaseRaidActive -> {
              Text(
                text = "CITADEL SIEGE: ${sim.hudBaseHp}/${sim.hudBaseMaxHp} HP (${sim.hudBaseRaidSecLeft}s)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF3D71),
              )
            }
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
                text = "PERIMETER BORDER • GATEWAYS ACTIVE",
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
        if (!w.isFinite() || !h.isFinite() || w <= 1f || h <= 1f) return@Canvas
        val camX = sim.cameraOriginX()
        val camY = sim.cameraOriginY()
        if (!camX.isFinite() || !camY.isFinite()) return@Canvas
        val resScale = canvasDensityScale(w, h)
        val worldAxis = worldAxisPx(w, h) * currentWorldViewZoom()
        val glowOn = settings.dynamicGlowEffects &&
          settings.graphicsQuality != GraphicsQuality.LOW &&
          settings.ramGraphicsMode != com.example.data.local.RamGraphicsMode.LOW_RAM_BOOST
        val highContrast = settings.effectiveHighContrast
        val pulse = if (settings.reduceMotion || settings.reduceFlashes) 0.3f else (sin(sim.elapsedTicks * 0.12f) + 1f) * 0.5f

        try {
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
          ramGraphicsMode = settings.ramGraphicsMode,
          ramGraphicsBudgetMb = settings.ramGraphicsBudgetMb,
          ramShaderCacheEnabled = settings.ramShaderCacheEnabled,
        )

        drawSectionBiomesAndBorders(
          sim = sim,
          camX = camX,
          camY = camY,
          highContrast = highContrast,
          lowGraphics = settings.graphicsQuality == GraphicsQuality.LOW,
          glowOn = glowOn,
          pulse = pulse,
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
          val brDepth = perspectiveScaleForRelY(ARENA_CENTER - camY)
          val brR = sim.brRingRadius * worldAxis * brDepth
          if (brR.isFinite() && brR > 0f && brC.x.isFinite() && brC.y.isFinite()) {
            drawOval(
              color = palette.dropHazard,
              topLeft = Offset(brC.x - brR, brC.y - brR * 0.78f),
              size = Size(brR * 2f, brR * 1.56f),
              style = Stroke(width = (if (highContrast) 6f else 4f) * resScale),
            )
          }
        }

        if (mode.id == "dmz") {
          val exfilReady = sim.hudIntelCollected >= 3
          val exfilCenter = worldToPerspectiveScreen(ARENA_CENTER, ARENA_CENTER - 2.4f, camX, camY, w, h)
          val exScale = perspectiveScaleForRelY((ARENA_CENTER - 2.4f) - camY, w, h)
          drawCircle(
            color = if (exfilReady) palette.safeExfil else palette.dropBuff.copy(alpha = 0.65f),
            radius = 32f * exScale,
            center = exfilCenter,
            style = Stroke(width = (if (highContrast) 6f else 4f) * exScale),
          )
          if (sim.hudExfilRatio > 0f) {
            val arcR = 40f * exScale
            drawArc(
              color = palette.playerAccent,
              startAngle = -90f,
              sweepAngle = sim.hudExfilRatio * 360f,
              useCenter = false,
              topLeft = Offset(exfilCenter.x - arcR, exfilCenter.y - arcR),
              size = Size(arcR * 2f, arcR * 2f),
              style = Stroke(width = 5f * exScale, cap = StrokeCap.Round),
            )
          }
          for (i in 0 until sim.intels.size) {
            val intel = sim.intels.getOrNull(i) ?: continue
            if (!intel.collected) {
              val ip = worldToPerspectiveScreen(intel.x, intel.y, camX, camY, w, h)
              val iScale = perspectiveScaleForRelY(intel.y - camY, w, h)
              if (glowOn) drawCircle(palette.dropBuff.copy(alpha = 0.25f), radius = 20f * iScale, center = ip)
              drawCircle(palette.dropBuff, radius = 11f * iScale, center = ip)
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
          val d = sim.drops.getOrNull(i) ?: continue
          val pos = worldToPerspectiveScreen(d.x, d.y, camX, camY, w, h)
          val dScale = perspectiveScaleForRelY(d.y - camY, w, h)
          if (pos.x < -30f * resScale || pos.x > w + 30f * resScale || pos.y < -30f * resScale || pos.y > h + 30f * resScale) continue
          val dCol = when (d.kind) {
            ArenaDropKind.NUKE -> palette.playerAccent
            ArenaDropKind.INSTA_KILL -> palette.dropHazard
            ArenaDropKind.MAX_ARMOR -> palette.safeExfil
            ArenaDropKind.DOUBLE_ESSENCE -> palette.dropBuff
            ArenaDropKind.AEGIS_RING -> Color(0xFF38BDF8)
            ArenaDropKind.FATE_BLESSING -> Color(0xFFFFAB40)
            ArenaDropKind.EPOCH_SURGE -> Color(0xFF69F0AE)
          }
          if (glowOn) drawCircle(dCol.copy(alpha = 0.24f + pulse * 0.18f), radius = 18f * dScale, center = pos)
          drawCircle(dCol, radius = (if (highContrast) 12f else 10f) * dScale, center = pos, style = Stroke(width = 3f * resScale))
          drawCircle(Color.White, radius = 4f * dScale, center = pos)
        }

        // Random Mystery Wonder Weapon Pickups
        for (i in 0 until sim.wonderPickups.size) {
          val wp = sim.wonderPickups.getOrNull(i) ?: continue
          val pos = worldToPerspectiveScreen(wp.x, wp.y, camX, camY, w, h)
          val wpScale = perspectiveScaleForRelY(wp.y - camY, w, h)
          if (pos.x < -30f * resScale || pos.x > w + 30f * resScale || pos.y < -30f * resScale || pos.y > h + 30f * resScale) continue
          val wCol = Color(wp.kind.accentHex)
          val boxHalf = 11f * wpScale
          if (glowOn) drawCircle(wCol.copy(alpha = 0.28f + pulse * 0.22f), radius = 22f * wpScale, center = pos)
          drawRect(color = Color(0xFF0A1222), topLeft = Offset(pos.x - boxHalf, pos.y - boxHalf), size = Size(boxHalf * 2f, boxHalf * 2f))
          drawRect(color = wCol, topLeft = Offset(pos.x - boxHalf, pos.y - boxHalf), size = Size(boxHalf * 2f, boxHalf * 2f), style = Stroke(width = 3f * resScale))
          drawCircle(Color.White, radius = 4.5f * wpScale, center = pos)
        }

        // Random Non-Wonder Weapon Pickups
        for (i in 0 until sim.weaponPickups.size) {
          val wpnPk = sim.weaponPickups.getOrNull(i) ?: continue
          val pos = worldToPerspectiveScreen(wpnPk.x, wpnPk.y, camX, camY, w, h)
          val wpScale = perspectiveScaleForRelY(wpnPk.y - camY, w, h)
          if (pos.x < -30f * resScale || pos.x > w + 30f * resScale || pos.y < -30f * resScale || pos.y > h + 30f * resScale) continue
          val wCol = Color(0xFF00E676)
          val boxHalf = 10f * wpScale
          if (glowOn) drawCircle(wCol.copy(alpha = 0.28f + pulse * 0.22f), radius = 20f * wpScale, center = pos)
          drawRect(color = Color(0xFF0F172A), topLeft = Offset(pos.x - boxHalf, pos.y - boxHalf), size = Size(boxHalf * 2f, boxHalf * 2f))
          drawRect(color = wCol, topLeft = Offset(pos.x - boxHalf, pos.y - boxHalf), size = Size(boxHalf * 2f, boxHalf * 2f), style = Stroke(width = 2.5f * resScale))
          drawCircle(Color.White, radius = 3.5f * wpScale, center = pos)
        }

        if (sim.pulseRingRadius > 0f && !settings.reduceFlashes) {
          val pCenter = worldToPerspectiveScreen(sim.playerX, sim.playerY, camX, camY, w, h)
          val pR = sim.pulseRingRadius * worldAxis
          if (pR.isFinite() && pR > 0f && pCenter.x.isFinite() && pCenter.y.isFinite()) {
            drawOval(
              color = palette.playerAccent.copy(alpha = 0.65f),
              topLeft = Offset(pCenter.x - pR, pCenter.y - pR * 0.78f),
              size = Size(pR * 2f, pR * 1.56f),
              style = Stroke(width = 7f * resScale),
            )
          }
        }

        // Arcs
        for (i in 0 until sim.arcs.size) {
          val arc = sim.arcs.getOrNull(i) ?: continue
          val a = (arc.life / 18f).coerceIn(0f, 1f)
          val p1 = worldToPerspectiveScreen(arc.x1, arc.y1, camX, camY, w, h)
          val p2 = worldToPerspectiveScreen(arc.x2, arc.y2, camX, camY, w, h)
          if (glowOn) {
            drawLine(color = arc.color.copy(alpha = a * 0.78f), start = p1, end = p2, strokeWidth = 6f * a * resScale, cap = StrokeCap.Round)
          }
          drawLine(color = Color.White.copy(alpha = a), start = p1, end = p2, strokeWidth = 2.5f * resScale, cap = StrokeCap.Round)
        }

        // Projectiles
        for (i in 0 until sim.bolts.size) {
          val b = sim.bolts.getOrNull(i) ?: continue
          val head = worldToPerspectiveScreen(b.x, b.y, camX, camY, w, h)
          val bScale = perspectiveScaleForRelY(b.y - camY, w, h)
          if (head.x < -30f * resScale || head.x > w + 30f * resScale || head.y < -30f * resScale || head.y > h + 30f * resScale) continue
          if (b.isEnemyShot) {
            val eShotCol = if (b.isMirroredElemental) b.damageType.color else palette.enemyShot
            if (glowOn) drawCircle(eShotCol.copy(alpha = 0.50f), radius = 11.5f * bScale, center = head)
            drawCircle(eShotCol, radius = (if (highContrast) 7.5f else 5.5f) * bScale, center = head)
            if (b.isMirroredElemental) {
              drawCircle(Color.White.copy(alpha = 0.9f), radius = 2.4f * bScale, center = head)
            }
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
            val sp = sim.sparks.getOrNull(i) ?: continue
            val spPos = worldToPerspectiveScreen(sp.x, sp.y, camX, camY, w, h)
            val spScale = perspectiveScaleForRelY(sp.y - camY, w, h)
            val r = (sp.maxLife - sp.life).coerceAtLeast(3f) * 1.8f * spScale
            if (!r.isFinite() || r <= 0f || !spPos.x.isFinite() || !spPos.y.isFinite()) continue
            drawCircle(
              color = sp.color.copy(alpha = (sp.life / sp.maxLife).coerceIn(0f, 1f)),
              radius = r,
              center = spPos,
              style = Stroke(width = 2.5f * resScale),
            )
          }
        }

        // Animated Lore & Original Hostiles & Bosses with 2.5D depth scaling
        val overrideEnemyCol = if (settings.colorBlindMode != ColorBlindMode.NONE || highContrast) palette.hostile else null
        for (i in 0 until sim.hostiles.size) {
          val e = sim.hostiles.getOrNull(i) ?: continue
          val ePos = worldToPerspectiveScreen(e.x, e.y, camX, camY, w, h)
          if (ePos.x < -130f * resScale || ePos.x > w + 130f * resScale || ePos.y < -130f * resScale || ePos.y > h + 130f * resScale) continue
          val eScale = perspectiveScaleForRelY(e.y - camY, w, h)
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
          val bot = sim.arenaBots.getOrNull(i) ?: continue
          if (bot.hp <= 0) continue
          val botOrigin = worldToPerspectiveScreen(bot.x, bot.y, camX, camY, w, h)
          if (botOrigin.x < -50f * resScale || botOrigin.x > w + 50f * resScale || botOrigin.y < -50f * resScale || botOrigin.y > h + 50f * resScale) continue
          val botScale = perspectiveScaleForRelY(bot.y - camY, w, h)
          val botRingCol = if (bot.isAllied) palette.safeExfil else palette.dropBuff
          drawOval(
            color = botRingCol.copy(alpha = 0.72f),
            topLeft = Offset(botOrigin.x - 20f * botScale, botOrigin.y - 12f * botScale),
            size = Size(40f * botScale, 24f * botScale),
            style = Stroke(width = 2.4f * resScale),
          )
          val botSkel = RigEvaluator.evaluate(
            origin = botOrigin,
            scalePx = 68f * botScale,
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
        drawCircle(opticCol.copy(alpha = 0.85f), radius = 10f * resScale, center = retPos, style = Stroke(width = 2.4f * resScale))
        drawCircle(Color.White.copy(alpha = 0.9f), radius = 2.5f * resScale, center = retPos)

        // Draw protection aura, orbitals, wingman, and escort vehicle behind player rig so full hero shows unobstructed
        drawPlayerProtectionAndCompanions(
          sim = sim,
          camX = camX,
          camY = camY,
          glowOn = glowOn,
          pulse = pulse,
        )

        // Player Character with Active Outfit & Rig
        val isMoving = (sim.joyDx * sim.joyDx + sim.joyDy * sim.joyDy) > 0.0064f
        val playerClip = when {
          settings.reduceMotion -> "idle"
          isMoving -> "sprint"
          else -> activeRig?.activeClip ?: "idle"
        }
        val playerScreen = worldToPerspectiveScreen(sim.playerX, sim.playerY, camX, camY, w, h)
        val playerPerspScale = perspectiveScaleForRelY(sim.playerY - camY, w, h)
        val skel = RigEvaluator.evaluate(
          origin = playerScreen,
          scalePx = 88f * (activeRig?.rigScale?.toFloat() ?: 1f) * playerPerspScale,
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

        // Player-Centered 24x24 Bordered Multi-Section Mini-Radar
        if (settings.showMiniRadar && !settings.simplifiedHud) {
          val radarR = (44f * resScale).coerceIn(34f, 88f)
          val radarCenter = Offset(w - radarR - 12f * resScale, radarR + 12f * resScale)
          val radarScope = 8.5f
          drawCircle(Color(0xD2080D1A), radius = radarR, center = radarCenter)
          drawCircle(
            color = (if (sim.hudInOuterFrontier) Color(0xFFE040FB) else palette.playerAccent).copy(alpha = 0.85f),
            radius = radarR,
            center = radarCenter,
            style = Stroke(width = 2.2f * resScale),
          )
          fun plotRadarUnbounded(wx: Float, wy: Float): Offset =
            Offset(
              radarCenter.x + ((wx - sim.playerX) / radarScope) * radarR,
              radarCenter.y + ((wy - sim.playerY) / radarScope) * radarR,
            )
          fun plotRadar(wx: Float, wy: Float): Offset? {
            val dx = (wx - sim.playerX) / radarScope
            val dy = (wy - sim.playerY) / radarScope
            if (dx * dx + dy * dy > 0.94f) return null
            return Offset(radarCenter.x + dx * radarR, radarCenter.y + dy * radarR)
          }
          // Draw section borders on radar
          for (div in floatArrayOf(0f, 8.0f, 16.0f, ARENA_WORLD_SIZE)) {
            val vx = radarCenter.x + ((div - sim.playerX) / radarScope) * radarR
            if (kotlin.math.abs(vx - radarCenter.x) < radarR * 0.92f) {
              val yTop = (radarCenter.y + ((0f - sim.playerY) / radarScope) * radarR).coerceIn(radarCenter.y - radarR * 0.88f, radarCenter.y + radarR * 0.88f)
              val yBot = (radarCenter.y + ((ARENA_WORLD_SIZE - sim.playerY) / radarScope) * radarR).coerceIn(radarCenter.y - radarR * 0.88f, radarCenter.y + radarR * 0.88f)
              val isOuter = div == 0f || div == ARENA_WORLD_SIZE
              drawLine(
                color = if (isOuter) Color(0xFFE040FB).copy(alpha = 0.7f) else Color(0xFF40C4FF).copy(alpha = 0.45f),
                start = Offset(vx, yTop),
                end = Offset(vx, yBot),
                strokeWidth = if (isOuter) 1.8f else 1f,
              )
            }
            val hy = radarCenter.y + ((div - sim.playerY) / radarScope) * radarR
            if (kotlin.math.abs(hy - radarCenter.y) < radarR * 0.92f) {
              val xL = (radarCenter.x + ((0f - sim.playerX) / radarScope) * radarR).coerceIn(radarCenter.x - radarR * 0.88f, radarCenter.x + radarR * 0.88f)
              val xR = (radarCenter.x + ((ARENA_WORLD_SIZE - sim.playerX) / radarScope) * radarR).coerceIn(radarCenter.x - radarR * 0.88f, radarCenter.x + radarR * 0.88f)
              val isOuter = div == 0f || div == ARENA_WORLD_SIZE
              drawLine(
                color = if (isOuter) Color(0xFFE040FB).copy(alpha = 0.7f) else Color(0xFF40C4FF).copy(alpha = 0.45f),
                start = Offset(xL, hy),
                end = Offset(xR, hy),
                strokeWidth = (if (isOuter) 1.8f else 1f) * resScale,
              )
            }
          }
          val rScale = radarR / 44f
          for (bi in 0 until sim.buildings.size) {
            val b = sim.buildings.getOrNull(bi) ?: continue
            plotRadar(b.x + b.width * 0.5f, b.y + b.height * 0.5f)?.let { pt ->
              drawCircle(if (b.explored) Color(0xFF00E676) else b.accentColor, radius = 2.5f * rScale, center = pt)
            }
          }
          plotRadar(sim.baseX, sim.baseY)?.let { bPt ->
            drawRect(color = Color(0xFF38BDF8), topLeft = Offset(bPt.x - 4f * rScale, bPt.y - 4f * rScale), size = Size(8f * rScale, 8f * rScale))
          }
          for (ti in 0 until sim.defenseTurrets.size) {
            val t = sim.defenseTurrets.getOrNull(ti) ?: continue
            plotRadar(t.x, t.y)?.let { tPt ->
              drawCircle(t.accentColor, radius = 1.9f * rScale, center = tPt)
            }
          }
          for (ci in 0 until sim.restCamps.size) {
            val camp = sim.restCamps.getOrNull(ci) ?: continue
            plotRadar(camp.x, camp.y)?.let { pt ->
              drawCircle(Color(0xFF00E676), radius = 3f * rScale, center = pt, style = Stroke(width = 1.6f * rScale))
            }
          }
          for (si in 0 until sim.worldStations.size) {
            val st = sim.worldStations.getOrNull(si) ?: continue
            plotRadar(st.x, st.y)?.let { pt ->
              drawCircle(st.kind.color, radius = 2.4f * rScale, center = pt)
            }
          }
          for (li in 0 until sim.lootCrates.size) {
            val cr = sim.lootCrates.getOrNull(li) ?: continue
            if (!cr.opened) {
              plotRadar(cr.x, cr.y)?.let { pt ->
                drawCircle(Color(cr.rarity.colorHex), radius = 2.2f * rScale, center = pt)
              }
            }
          }
          for (ei in 0 until sim.catEasterEggs.size) {
            val cat = sim.catEasterEggs.getOrNull(ei) ?: continue
            if (!cat.found) {
              plotRadar(cat.x, cat.y)?.let { pt ->
                drawCircle(Color(0xFFFFD54F), radius = 2.4f * rScale, center = pt)
              }
            }
          }
          for (i in 0 until sim.hostiles.size) {
            val e = sim.hostiles.getOrNull(i) ?: continue
            plotRadar(e.x, e.y)?.let { pt ->
              drawCircle(
                color = if (e.isBoss) palette.boss else palette.hostile,
                radius = (if (e.isBoss) 3.8f else 2.2f) * rScale,
                center = pt,
              )
            }
          }
          drawCircle(palette.radarPlayer, radius = 3.8f * rScale, center = radarCenter)
        }

        if (sim.damageFlashTicks > 0f && !settings.reduceFlashes) {
          drawRect(color = palette.enemyShot.copy(alpha = 0.35f), topLeft = Offset.Zero, size = Size(w, h), style = Stroke(width = 22f * resScale))
        }
        } catch (t: Throwable) {
          android.util.Log.e("ArenaCanvas", "draw error", t)
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
              val bossArmor = if (sim.hudBossArmorRatio.isFinite()) sim.hudBossArmorRatio.coerceIn(0f, 1f) else 0f
              val bossHp = if (sim.hudBossHpRatio.isFinite()) sim.hudBossHpRatio.coerceIn(0f, 1f) else 0f
              if (bossArmor > 0f) {
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(
                  progress = { bossArmor },
                  color = Color(0xFFB0BEC5),
                  trackColor = Color(0xFF1A2538),
                  modifier = Modifier.width(190.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                )
              }
              Spacer(Modifier.height(3.dp))
              LinearProgressIndicator(
                progress = { bossHp },
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
                  sim.hudPackTier = com.example.data.model.PowerTier.MAX_TIER
                  sim.maxPackTierReached = com.example.data.model.PowerTier.MAX_TIER
                  sim.manaCurrent = sim.manaMax
                  sim.hudMana = sim.manaMax
                  sim.postCallout("SANDBOX PACK-A-TIER LX (${com.example.data.model.PowerTier.MAX_TIER}) & FULL MANA!")
                },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(8.dp),
              ) { Text("Tier LX (60)", style = MaterialTheme.typography.labelSmall) }
              OutlinedButton(
                onClick = { sim.triggerBaseDefenseRaid() },
                modifier = Modifier.height(48.dp).testTag("sandbox_base_raid_btn"),
                shape = RoundedCornerShape(8.dp),
              ) { Text("+Base Raid", style = MaterialTheme.typography.labelSmall) }
              OutlinedButton(
                onClick = { sim.spawnRandomWonderWeapon() },
                modifier = Modifier.height(48.dp).testTag("sandbox_spawn_wonder_btn"),
                shape = RoundedCornerShape(8.dp),
              ) { Text("+Wonder", style = MaterialTheme.typography.labelSmall) }
              OutlinedButton(
                onClick = {
                  sim.hudPendingDraftCount++
                  sim.rollInGameUpgradeDraft()
                  sim.postCallout("SANDBOX IN-GAME DRAFT TRIGGERED!")
                },
                modifier = Modifier.height(48.dp).testTag("sandbox_draft_btn"),
                shape = RoundedCornerShape(8.dp),
              ) { Text("+Draft", style = MaterialTheme.typography.labelSmall) }
              OutlinedButton(
                onClick = {
                  sim.powerCd = 0f
                  sim.interactCd = 0f
                  sim.consumableCd = 0f
                  sim.lethalCd = 0f
                  sim.tacticalCd = 0f
                  sim.magicCd = 0f
                  sim.spiritHealCd = 0f
                  sim.manaCurrent = sim.manaMax
                  sim.hudMana = sim.manaMax
                  sim.overdriveTicks = 300f
                  sim.postCallout("ALL COOLDOWNS & MANA RESET!")
                },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(8.dp),
              ) { Text("Reset CD", style = MaterialTheme.typography.labelSmall) }
            }
          }
        }
      }

      if (sim.hudPendingDraftChoices.isNotEmpty() && !sim.hudRunEnded && !settings.simplifiedHud) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xEC08101E),
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 10.dp, vertical = if (mode.id == "sandbox") 64.dp else 8.dp)
            .border(1.5.dp, Color(0xFFFFD54F), RoundedCornerShape(12.dp))
            .testTag("arena_upgrade_draft_panel"),
        ) {
          Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "SURVIVAL LV.${sim.hudMatchLevel} DRAFT — PICK 1 BUFF (${sim.hudPendingDraftCount} READY)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFFD54F),
            )
            Row(
              modifier = Modifier.horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              sim.hudPendingDraftChoices.forEachIndexed { idx, spec ->
                val curRank = sim.inGameUpgradeRanks[spec.id] ?: 0
                val accent = Color(spec.accentHex)
                Surface(
                  onClick = {
                    triggerHaptic()
                    sim.selectInGameUpgrade(spec.id)
                  },
                  shape = RoundedCornerShape(10.dp),
                  color = accent.copy(alpha = 0.16f),
                  modifier = Modifier
                    .widthIn(min = 148.dp, max = 185.dp)
                    .heightIn(min = 48.dp)
                    .border(1.2.dp, accent, RoundedCornerShape(10.dp))
                    .testTag("arena_draft_pick_$idx"),
                ) {
                  Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${spec.category} • R${curRank + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent, maxLines = 1)
                    Text(spec.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                    Text(spec.summary, style = MaterialTheme.typography.labelSmall, color = Color(0xFFD0DCF0), maxLines = 2)
                  }
                }
              }
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

    // Bottom Compact Console/PC Multi-Action Controller Bar
    val controllerDeckBg = hudSurfaceBg.copy(
      alpha = (settings.hudOpacity * settings.controllerOpacity * 0.78f).coerceIn(0.24f, 0.88f)
    )
    Surface(
      color = controllerDeckBg,
      modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
      val cScale = settings.controllerScale.coerceIn(0.65f, 1.35f)
      val stickDiameter = (58f * settings.joystickScale * cScale).coerceIn(50f, 92f).dp
      val aimStickDiameter = (52f * settings.joystickScale * cScale).coerceIn(48f, 84f).dp
      val puckDiameter = (22f * settings.joystickScale * cScale).coerceIn(18f, 34f).dp
      val btnHeight = (48f * settings.buttonScale * cScale).coerceIn(48f, 56f).dp
      val btnSpacing = settings.controllerSpacingDp.coerceIn(2, 10).dp
      val actionShape = when (settings.controllerButtonShape) {
        "gamepad_circle" -> CircleShape
        "tactical_hex" -> RoundedCornerShape(6.dp)
        else -> RoundedCornerShape(10.dp)
      }
      var aimThumbOffsetX by remember { mutableFloatStateOf(0f) }
      var aimThumbOffsetY by remember { mutableFloatStateOf(0f) }

      val stickWidget: @Composable () -> Unit = {
        Box(
          modifier = Modifier
            .size(stickDiameter)
            .clip(CircleShape)
            .background(Color(0xFF141E36).copy(alpha = settings.controllerOpacity.coerceIn(0.35f, 0.95f)))
            .border(
              width = if (sim.hudSprinting) 2.dp else 1.4.dp,
              color = if (sim.hudSprinting) Color(0xFF00E676) else palette.playerAccent.copy(alpha = 0.75f),
              shape = CircleShape,
            )
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
                  thumbOffsetX = nx * 16f * settings.joystickScale
                  thumbOffsetY = ny * 16f * settings.joystickScale
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
              .background(if (sim.hudSprinting) Color(0xFF00E676) else palette.playerAccent)
          )
        }
      }

      val aimStickWidget: @Composable () -> Unit = {
        Box(
          modifier = Modifier
            .size(aimStickDiameter)
            .clip(CircleShape)
            .background(Color(0xFF1A1228).copy(alpha = settings.controllerOpacity.coerceIn(0.35f, 0.95f)))
            .border(1.4.dp, Color(0xFFFF3D71).copy(alpha = 0.85f), CircleShape)
            .testTag("arena_aim_joystick")
            .pointerInput(sim, settings.joystickScale) {
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                fun updateAim(posX: Float, posY: Float) {
                  val cx = size.width / 2f
                  val cy = size.height / 2f
                  val nx = ((posX - cx) / cx.coerceAtLeast(1f)).coerceIn(-1f, 1f)
                  val ny = ((posY - cy) / cy.coerceAtLeast(1f)).coerceIn(-1f, 1f)
                  val magSq = nx * nx + ny * ny
                  if (magSq > 0.04f) {
                    val len = sqrt(magSq).coerceAtLeast(0.01f)
                    sim.setTouchAim(sim.playerX + (nx / len) * 0.45f, sim.playerY + (ny / len) * 0.45f, active = true)
                  }
                  aimThumbOffsetX = nx * 14f * settings.joystickScale
                  aimThumbOffsetY = ny * 14f * settings.joystickScale
                }
                down.consume()
                updateAim(down.position.x, down.position.y)
                sim.fireVolley(forceManual = true)
                while (true) {
                  val event = awaitPointerEvent()
                  val ch = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                  if (ch == null || !ch.pressed) {
                    sim.setTouchAim(0f, 0f, active = false)
                    aimThumbOffsetX = 0f
                    aimThumbOffsetY = 0f
                    break
                  }
                  ch.consume()
                  updateAim(ch.position.x, ch.position.y)
                }
              }
            },
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier
              .offset { IntOffset(aimThumbOffsetX.roundToInt(), aimThumbOffsetY.roundToInt()) }
              .size((puckDiameter * 0.9f))
              .clip(CircleShape)
              .background(Color(0xFFFF3D71))
          )
        }
      }

      val dpadWidget: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedButton(
            onClick = { triggerHaptic(); sim.cycleElementalPrism() },
            shape = actionShape,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.height(48.dp).testTag("arena_dpad_left"),
          ) { Text("◀", style = MaterialTheme.typography.labelSmall) }
          OutlinedButton(
            onClick = { triggerHaptic(); sim.cycleMagicSpell() },
            shape = actionShape,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.height(48.dp).testTag("arena_dpad_up"),
          ) { Text("▲", style = MaterialTheme.typography.labelSmall) }
          OutlinedButton(
            onClick = { triggerHaptic(); sim.toggleStance() },
            shape = actionShape,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.height(48.dp).testTag("arena_dpad_down"),
          ) { Text("▼", style = MaterialTheme.typography.labelSmall) }
          OutlinedButton(
            onClick = { triggerHaptic(); sim.cycleActiveWeapon() },
            shape = actionShape,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.height(48.dp).testTag("arena_dpad_right"),
          ) { Text("▶", style = MaterialTheme.typography.labelSmall) }
        }
      }

      val isConsolePad = settings.controllerStyle == "console_gamepad"
      val actionsWidget: @Composable () -> Unit = {
        val activeSpellSpec = com.example.data.model.MultiverseSystemsCatalog.spellById(sim.hudActiveSpellId)
        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun ConsoleMultiButton(
          primary: String,
          secondary: String,
          bgColor: Color,
          fgColor: Color = Color.Black,
          enabled: Boolean = true,
          tag: String,
          onTap: () -> Unit,
          onHold: () -> Unit,
        ) {
          Surface(
            shape = actionShape,
            color = if (enabled) bgColor else bgColor.copy(alpha = 0.35f),
            modifier = Modifier
              .height(btnHeight.coerceAtLeast(48.dp))
              .widthIn(min = 54.dp)
              .clip(actionShape)
              .border(1.dp, fgColor.copy(alpha = 0.28f), actionShape)
              .combinedClickable(
                enabled = enabled,
                onClick = { triggerHaptic(); onTap() },
                onLongClick = { triggerHaptic(); onHold() },
              )
              .testTag(tag),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              Text(primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = fgColor, maxLines = 1)
              Text(secondary, style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.72f), maxLines = 1)
            }
          }
        }

        // Single Slim Row of Console/PC Multi-Action Buttons (Tap = Primary, Hold = Secondary)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(btnSpacing),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ConsoleMultiButton(
            primary = if (isConsolePad) "A:ATK" else "ATK",
            secondary = if (sim.hudMeleeCdSec == 0) "MELEE" else "${sim.hudMeleeCdSec}s",
            bgColor = palette.playerAccent,
            fgColor = Color.Black,
            tag = "arena_fire_btn",
            onTap = { sim.fireVolley(forceManual = true) },
            onHold = { sim.triggerMeleeStrike() },
          )
          ConsoleMultiButton(
            primary = if (sim.hudMagicCdSec == 0) (if (isConsolePad) "RB:${activeSpellSpec.shortTag.take(3)}" else activeSpellSpec.shortTag.take(4)) else "${sim.hudMagicCdSec}s",
            secondary = if (sim.hudCanSpiritHeal && sim.hudSpiritHealCdSec == 0) "HEAL" else "SPELL",
            bgColor = Color(activeSpellSpec.colorHex),
            fgColor = Color.Black,
            tag = "arena_magic_btn",
            onTap = {
              if (sim.hudMana >= activeSpellSpec.manaCost && sim.hudMagicCdSec == 0) {
                sim.triggerMagicSpell()
              } else {
                sim.cycleMagicSpell()
              }
            },
            onHold = {
              if (sim.hudCanSpiritHeal && sim.hudShield < sim.effectiveMaxShield() * 0.75f && sim.hudSpiritHealCdSec == 0) {
                sim.triggerSpiritThoughtHeal()
              } else {
                sim.cycleMagicSpell()
              }
            },
          )
          ConsoleMultiButton(
            primary = if (sim.hudPowerCdSec == 0) (if (isConsolePad) "X:PWR" else "PWR") else "${sim.hudPowerCdSec}s",
            secondary = if (sim.hudLethalCdSec == 0 || sim.hudConsumableCdSec == 0) "SRG+LTH" else "CD",
            bgColor = Color(0xFF7C4DFF),
            fgColor = Color.White,
            tag = "arena_power_btn",
            onTap = { sim.triggerPower() },
            onHold = {
              sim.triggerConsumable()
              if (sim.hudLethalCdSec == 0) sim.triggerLethal()
              if (sim.hudTacticalCdSec == 0) sim.triggerTactical()
            },
          )
          ConsoleMultiButton(
            primary = if (sim.hudNearbyPrompt != null) (if (isConsolePad) "Y:USE" else "USE") else "PK ${formatRoundRoman(sim.hudPackTier.toLong())}",
            secondary = "WPN ${sim.activeWeaponIndex + 1}/${sim.equippedWeaponIds.size}",
            bgColor = if (sim.hudPackTier >= com.example.data.model.PowerTier.MAX_TIER) Color(0xFF00E5FF) else Color(0xFFE040FB),
            fgColor = Color.Black,
            tag = "arena_interact_btn",
            onTap = {
              if (sim.hudNearbyPrompt != null) {
                sim.interactNearestWorldObject()
              } else {
                sim.upgradePackAPunch()
              }
            },
            onHold = {
              sim.cycleActiveWeapon()
              sim.triggerAllTraps()
            },
          )
          ConsoleMultiButton(
            primary = if (sim.hudDashReady) (if (isConsolePad) "B:DSH" else LocalGameStrings.current.dashLabel.take(4)) else "CD",
            secondary = if (sim.hudBaseRaidActive) "RAID" else "WNDR",
            bgColor = Color(0xFF00E676),
            fgColor = Color.Black,
            tag = "arena_dash_btn",
            onTap = { sim.triggerDash() },
            onHold = {
              if (!sim.hudBaseRaidActive) sim.triggerBaseDefenseRaid() else sim.activateEquippedWonderSlot()
            },
          )
          ConsoleMultiButton(
            primary = if (sim.hudAegisDomeActive) "DOME!" else if (sim.hudAegisDomeCdSec == 0) "AEGIS" else "${sim.hudAegisDomeCdSec}s",
            secondary = "UPG",
            bgColor = Color(0xFF38BDF8),
            fgColor = Color.Black,
            tag = "arena_aegis_btn",
            onTap = { sim.triggerAegisBarrier() },
            onHold = { showFieldUpgradesDialog = true },
          )
          ConsoleMultiButton(
            primary = sim.hudActiveDamageType.shortTag,
            secondary = if (sim.hudAutoFire) "AUTO" else "MAN",
            bgColor = Color(0xFF172036),
            fgColor = sim.hudActiveDamageType.color,
            tag = "arena_prism_cycle_btn",
            onTap = { sim.cycleElementalPrism() },
            onHold = { sim.hudAutoFire = !sim.hudAutoFire },
          )
          ConsoleMultiButton(
            primary = "PAD",
            secondary = if (sim.hudCrouching) "CRCH" else if (sim.gtaLockOnEnabled) "LOCK" else "FREE",
            bgColor = Color(0xFF141C2E),
            fgColor = Color(0xFFE2E8F0),
            tag = "arena_pad_tune_btn",
            onTap = {
              if (onUpdateSettings != null) showQuickControllerDialog = true else onOpenSettings()
            },
            onHold = {
              sim.toggleLockOn()
              sim.toggleStance()
            },
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp).testTag("arena_controls_deck"),
        horizontalArrangement = Arrangement.spacedBy(btnSpacing),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (settings.leftHandedDeck) {
          aimStickWidget()
          Box(modifier = Modifier.weight(1f)) { actionsWidget() }
          if (settings.controllerShowDpad) dpadWidget()
          stickWidget()
        } else {
          stickWidget()
          if (settings.controllerShowDpad) dpadWidget()
          Box(modifier = Modifier.weight(1f)) { actionsWidget() }
          aimStickWidget()
        }
      }
    }

    if (showQuickControllerDialog && onUpdateSettings != null) {
      val styleChoices = listOf("console_gamepad" to "Console Gamepad", "codm_tactical" to "CoDM Tactical", "floating_classic" to "Floating Classic")
      val shapeChoices = listOf("gamepad_circle" to "Circle", "tactical_hex" to "Hex", "pill" to "Pill")
      val actionChoices = listOf("fire", "melee", "power", "aegis_ward", "magic", "spirit_heal", "dash", "interact", "surge", "lethal", "tactical", "wonder", "traps", "swap_wpn", "pack", "base_raid")
      AlertDialog(
        onDismissRequest = { showQuickControllerDialog = false },
        title = { Text("CUSTOMIZE CONTROLLER DECK", fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CONTROLLER LAYOUT STYLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              styleChoices.forEach { (id, label) ->
                FilterChip(
                  selected = settings.controllerStyle == id,
                  onClick = { onUpdateSettings { s -> s.copy(controllerStyle = id) } },
                  label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.heightIn(min = 48.dp),
                )
              }
            }
            Text("BUTTON SHAPE & D-PAD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              shapeChoices.forEach { (id, label) ->
                FilterChip(
                  selected = settings.controllerButtonShape == id,
                  onClick = { onUpdateSettings { s -> s.copy(controllerButtonShape = id) } },
                  label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.heightIn(min = 48.dp),
                )
              }
              FilterChip(
                selected = settings.controllerShowDpad,
                onClick = { onUpdateSettings { s -> s.copy(controllerShowDpad = !s.controllerShowDpad) } },
                label = { Text("Show D-Pad", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.heightIn(min = 48.dp),
              )
            }
            Text("SCALE (${String.format(Locale.US, "%.2fx", settings.controllerScale)}) & OPACITY (${(settings.controllerOpacity * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedButton(onClick = { onUpdateSettings { s -> s.copy(controllerScale = (s.controllerScale - 0.1f).coerceIn(0.65f, 1.35f)) } }, modifier = Modifier.height(48.dp)) { Text("-Scale") }
              OutlinedButton(onClick = { onUpdateSettings { s -> s.copy(controllerScale = (s.controllerScale + 0.1f).coerceIn(0.65f, 1.35f)) } }, modifier = Modifier.height(48.dp)) { Text("+Scale") }
              OutlinedButton(onClick = { onUpdateSettings { s -> s.copy(controllerOpacity = (s.controllerOpacity - 0.15f).coerceIn(0.30f, 1.0f)) } }, modifier = Modifier.height(48.dp)) { Text("-Opac") }
              OutlinedButton(onClick = { onUpdateSettings { s -> s.copy(controllerOpacity = (s.controllerOpacity + 0.15f).coerceIn(0.30f, 1.0f)) } }, modifier = Modifier.height(48.dp)) { Text("+Opac") }
            }
            Text("TAP BUTTON TO CYCLE BINDING (A/B/X/Y/L1/R1)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              val cycle: (String) -> String = { cur -> actionChoices[(actionChoices.indexOf(cur) + 1) % actionChoices.size] }
              OutlinedButton(onClick = { onUpdateSettings { s -> s.copy(controllerActionA = cycle(s.controllerActionA)) } }, modifier = Modifier.height(48.dp)) { Text("A:${settings.controllerActionA.take(5)}") }
              OutlinedButton(onClick = { onUpdateSettings { s -> s.copy(controllerActionB = cycle(s.controllerActionB)) } }, modifier = Modifier.height(48.dp)) { Text("B:${settings.controllerActionB.take(5)}") }
              OutlinedButton(onClick = { onUpdateSettings { s -> s.copy(controllerActionX = cycle(s.controllerActionX)) } }, modifier = Modifier.height(48.dp)) { Text("X:${settings.controllerActionX.take(5)}") }
              OutlinedButton(onClick = { onUpdateSettings { s -> s.copy(controllerActionY = cycle(s.controllerActionY)) } }, modifier = Modifier.height(48.dp)) { Text("Y:${settings.controllerActionY.take(5)}") }
              OutlinedButton(onClick = { onUpdateSettings { s -> s.copy(controllerActionL1 = cycle(s.controllerActionL1)) } }, modifier = Modifier.height(48.dp)) { Text("L1:${settings.controllerActionL1.take(5)}") }
              OutlinedButton(onClick = { onUpdateSettings { s -> s.copy(controllerActionR1 = cycle(s.controllerActionR1)) } }, modifier = Modifier.height(48.dp)) { Text("R1:${settings.controllerActionR1.take(5)}") }
            }
          }
        },
        confirmButton = { TextButton(onClick = { showQuickControllerDialog = false }) { Text("Done") } },
      )
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
          Column(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              listOf(ChatChannel.LIVE_MATCH, ChatChannel.CLAN, ChatChannel.GENERAL).forEach { ch ->
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
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                SocialCatalog.quickTacticalCallouts.forEach { callout ->
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
              items(channelMessages.takeLast(25).distinctBy { it.id }, key = { it.id }) { msg ->
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
      val allAreas = remember(progression.customGameMaps) { progression.allPlayableAreas() }
      var mapCategoryFilter by remember { mutableStateOf("all") }
      val visibleAreas = remember(allAreas, mapCategoryFilter) {
        when (mapCategoryFilter) {
          "bank_vault" -> allAreas.filter { it.id.contains("bank", true) || it.id.contains("vault", true) || it.name.contains("bank", true) || it.name.contains("vault", true) }
          "bo3" -> allAreas.filter { it.id.startsWith("area_bo3_") || it.id == "area_zombies_der_riese" }
          "collections" -> allAreas.filter { it.id.startsWith("area_col_custom_") || it.storyEraNote.contains("Custom Map", ignoreCase = true) }
          else -> allAreas
        }
      }
      AlertDialog(
        onDismissRequest = { showMapSwitcherDialog = false },
        title = {
          Text("SWITCH ARENA MAP & MODE (${allAreas.size} MAPS)", fontWeight = FontWeight.Bold)
        },
        text = {
          Column(modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 440.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onSelectMode != null) {
              Text("DEPLOYMENT MODE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                UniverseCatalog.modes.distinctBy { it.id }.forEach { m ->
                  val mTier = progression.modeTier(m.id)
                  val mStar = progression.modeStar(m.id)
                  FilterChip(
                    selected = mode.id == m.id,
                    onClick = { onSelectMode(m.id) },
                    label = { Text("${m.shortTag} • ${m.label} (T$mTier/$mStar★)", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("arena_quick_mode_${m.id}"),
                  )
                }
              }
            }
            if (onUpdateCollectionCustomMode != null) {
              Text("RULESET: NORMAL MODE vs CUSTOM MODE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                FilterChip(
                  selected = !progression.collectionCustomMode.enabled,
                  onClick = { onUpdateCollectionCustomMode { cfg -> cfg.copy(enabled = false) } },
                  label = { Text("Normal Mode", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("arena_switch_normal_mode"),
                )
                FilterChip(
                  selected = progression.collectionCustomMode.enabled,
                  onClick = { onUpdateCollectionCustomMode { cfg -> cfg.copy(enabled = true) } },
                  label = { Text("Custom Mode", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("arena_switch_custom_mode"),
                )
                OutlinedButton(
                  onClick = {
                    showMapSwitcherDialog = false
                    showCustomModeDialog = true
                  },
                  modifier = Modifier.heightIn(min = 48.dp).testTag("arena_open_custom_mode_btn"),
                ) {
                  Text("Setup", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
              }
            }
            Text("ALL MAPS & CUSTOM ARENAS (${visibleAreas.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf("all" to "All Maps", "bank_vault" to "Bank & Vault", "bo3" to "BO3 Custom", "collections" to "Collection Custom").forEach { (k, lbl) ->
                FilterChip(
                  selected = mapCategoryFilter == k,
                  onClick = { mapCategoryFilter = k },
                  label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.heightIn(min = 48.dp).testTag("arena_map_filter_$k"),
                )
              }
            }
            LazyColumn(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              items(visibleAreas.distinctBy { it.id }, key = { it.id }) { area ->
                val selected = area.id == progression.activeAreaId
                val aSt = progression.areaState(area.id)
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
                    Text("${area.name} • Grid T${aSt.powerGridTier}/${com.example.data.model.PowerTier.MAX_TIER} • ${aSt.starLevel}★", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
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
        customGameMaps = progression.customGameMaps,
      )
    }

    if (showFieldUpgradesDialog) {
      val nextEpoch = sim.hudEpoch.next()
      val epochCost = 350L + (sim.hudEpoch.tier * 150L)
      AlertDialog(
        onDismissRequest = { showFieldUpgradesDialog = false },
        title = { Text("SURVIVAL EPOCH, BARRIERS & IN-GAME BUFFS", fontWeight = FontWeight.Bold) },
        text = {
          Column(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "Score: ${sim.hudScore} • Diamonds: ${sim.availableDiamondsInMatch()} • TUUCash: ${sim.availableTuuCashInMatch()} • Turrets: ${sim.hudTurretCount}",
              style = MaterialTheme.typography.labelSmall,
              color = Color(sim.hudEpoch.accentHex),
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "DOA x${String.format(Locale.US, "%.1f", sim.hudDoaMultiplier)} • ${sim.hudEpoch.label} • Plates ${sim.hudArmorPlates}/${sim.hudMaxArmorPlates} • Sentinels ${sim.hudOrbitalBladesCount}",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF80D8FF),
            )
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Button(
                onClick = { sim.buyFieldUpgrade("plate_refill") },
                modifier = Modifier.height(48.dp).testTag("field_upg_plates_btn"),
              ) { Text("Plates (150 Sc)", style = MaterialTheme.typography.labelSmall) }
              Button(
                onClick = { sim.buyFieldUpgrade("orbital_blade") },
                modifier = Modifier.height(48.dp).testTag("field_upg_blade_btn"),
              ) { Text("+Sentinel (300 Sc)", style = MaterialTheme.typography.labelSmall) }
              Button(
                onClick = { sim.buyFieldUpgrade("draft_roll") },
                modifier = Modifier.height(48.dp).testTag("field_upg_draft_btn"),
              ) { Text("+Buff Draft (250 Sc)", style = MaterialTheme.typography.labelSmall) }
              Button(
                onClick = { sim.buyFieldUpgrade("magic_surge") },
                modifier = Modifier.height(48.dp).testTag("field_upg_magic_btn"),
              ) { Text("Magic Surge (200 Sc)", style = MaterialTheme.typography.labelSmall) }
              Button(
                onClick = { sim.buyFieldUpgrade("deploy_turret_cash") },
                modifier = Modifier.height(48.dp).testTag("field_upg_turret_cash_btn"),
              ) { Text("+Turret (1,200 $)", style = MaterialTheme.typography.labelSmall) }
              Button(
                onClick = { sim.buyFieldUpgrade("tuu_airstrike_call") },
                modifier = Modifier.height(48.dp).testTag("field_upg_airstrike_cash_btn"),
              ) { Text("Gunship (1,800 $)", style = MaterialTheme.typography.labelSmall) }
              Button(
                onClick = { sim.buyFieldUpgrade("diamond_overclock") },
                modifier = Modifier.height(48.dp).testTag("field_upg_diamond_overclock_btn"),
              ) { Text("Overclock (40 D)", style = MaterialTheme.typography.labelSmall) }
              Button(
                onClick = { sim.buyFieldUpgrade("diamond_laser_overload") },
                modifier = Modifier.height(48.dp).testTag("field_upg_diamond_laser_btn"),
              ) { Text("Prism Beam (50 D)", style = MaterialTheme.typography.labelSmall) }
              Button(
                onClick = { sim.buyFieldUpgrade("diamond_harvest") },
                modifier = Modifier.height(48.dp).testTag("field_upg_diamond_drop_btn"),
              ) { Text("+Diamonds (300 Sc)", style = MaterialTheme.typography.labelSmall) }
              Button(
                onClick = { sim.buyFieldUpgrade("tuu_bounty") },
                modifier = Modifier.height(48.dp).testTag("field_upg_cash_drop_btn"),
              ) { Text("+TUUCash (220 Sc)", style = MaterialTheme.typography.labelSmall) }
              if (nextEpoch != null) {
                Button(
                  onClick = { sim.buyFieldUpgrade("advance_epoch") },
                  modifier = Modifier.height(48.dp).testTag("field_upg_epoch_btn"),
                ) { Text("-> ${nextEpoch.shortTag} ($epochCost Sc)", style = MaterialTheme.typography.labelSmall) }
              }
            }
            if (sim.hudPendingDraftChoices.isNotEmpty()) {
              Text("PENDING DRAFT PICKS (${sim.hudPendingDraftCount})", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
              sim.hudPendingDraftChoices.forEach { spec ->
                OutlinedButton(
                  onClick = { sim.selectInGameUpgrade(spec.id) },
                  modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                  Text("${spec.title}: ${spec.summary}", style = MaterialTheme.typography.labelSmall, maxLines = 2)
                }
              }
            }
            Text("ACTIVE IN-MATCH UPGRADES (${sim.inGameUpgradeRanks.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              items(InGameUpgradeCatalog.upgrades, key = { it.id }) { spec ->
                val rank = sim.inGameUpgradeRanks[spec.id] ?: 0
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (rank > 0) Color(spec.accentHex).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier.fillMaxWidth().border(1.dp, if (rank > 0) Color(spec.accentHex) else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                ) {
                  Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("${spec.title} (${spec.category})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                      Text(spec.summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Lv.$rank/${spec.maxStacks}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(spec.accentHex))
                  }
                }
              }
            }
          }
        },
        confirmButton = { TextButton(onClick = { showFieldUpgradesDialog = false }) { Text("Close") } },
      )
    }
  }
  }
}

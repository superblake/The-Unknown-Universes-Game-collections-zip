package com.example.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.AppSettings
import com.example.data.local.AppSettingsPresets
import com.example.data.local.CollectionResolutionMode
import com.example.data.local.ColorBlindMode
import com.example.data.local.FullscreenMode
import com.example.data.local.GameStrings
import com.example.data.local.GraphicsQuality
import com.example.data.local.LocalGameStrings
import com.example.data.local.ParticleDensity
import com.example.data.local.ThemeVisualMode
import com.example.util.SyncDialogFullscreen
import com.example.data.model.RegionalServerCatalog
import com.example.data.model.WorldRegion
import com.example.security.AppSecurityManager
import com.example.security.SecurityAuditReport
import com.example.ui.theme.AppIcons
import java.util.Locale
import kotlin.math.roundToInt

private enum class SettingsCategory(val id: String, val label: String) {
  ALL("all", "All"),
  AUDIO("audio", "Music & Audio"),
  CONTROLS("controls", "Controls"),
  PERFORMANCE("performance", "Performance"),
  VISUAL("visual", "Visuals"),
  ACCESSIBILITY("accessibility", "Health & Access"),
  PROTECTION("protection", "Security");

  fun localizedLabel(s: GameStrings): String = when (this) {
    ALL -> s.settingsAll
    AUDIO -> s.settingsAudio
    CONTROLS -> s.settingsControls
    PERFORMANCE -> s.settingsPerformance
    VISUAL -> s.settingsVisual
    ACCESSIBILITY -> s.settingsAccess
    PROTECTION -> s.settingsSecurity
  }
}

@Composable
fun SettingsDialog(
  settings: AppSettings,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
  onReplaceSettings: (AppSettings) -> Unit,
  onResetDefaults: () -> Unit,
  onDismiss: () -> Unit,
  progressionIntegrityValid: Boolean = true,
  onVerifyIntegrity: () -> Boolean = { true },
  onSelectRegion: ((String) -> Unit)? = null,
  onSelectServer: ((String) -> Unit)? = null,
  onToggleRegionDefaultSettings: ((Boolean) -> Unit)? = null,
  onSelectLanguage: ((String) -> Unit)? = null,
) {
  val context = LocalContext.current
  val gameStrings = LocalGameStrings.current
  var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }
  var pinEnabled by remember(context) { mutableStateOf(AppSecurityManager.isPinEnabled(context)) }
  var showPinDialog by remember { mutableStateOf(false) }
  var pinDraft by remember { mutableStateOf("") }
  var pinError by remember { mutableStateOf<String?>(null) }
  var lastAuditValid by remember(progressionIntegrityValid) { mutableStateOf(progressionIntegrityValid) }
  val auditReport = remember(pinEnabled, settings.screenCaptureShield, settings.chatSafetyFilter, lastAuditValid) {
    SecurityAuditReport(
      pinLockEnabled = pinEnabled,
      screenShieldEnabled = settings.screenCaptureShield,
      chatFilterEnabled = settings.chatSafetyFilter,
      progressionSealValid = lastAuditValid,
      backupDisabled = true,
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false,
    ),
  ) {
    SyncDialogFullscreen(hideSystemBars = settings.shouldHideSystemBars(inArena = false))
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().testTag("settings_dialog_root"),
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Header Bar
        Surface(
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = AppIcons.Accessibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
              )
              Column {
                Text(
                  text = stringResource(R.string.settings_title),
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.semantics { heading() },
                )
                Text(
                  text = stringResource(R.string.settings_subtitle),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(48.dp).testTag("close_settings_btn"),
            ) {
              Icon(AppIcons.Close, contentDescription = "Close Settings", tint = MaterialTheme.colorScheme.onSurface)
            }
          }
        }

        // Category Filter Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SettingsCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat.localizedLabel(gameStrings)) },
              modifier = Modifier.height(48.dp).testTag("settings_tab_${cat.id}"),
            )
          }
        }

        // Scrollable Settings Cards
        LazyColumn(
          modifier = Modifier.weight(1f).widthIn(max = 720.dp).fillMaxWidth(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          if (selectedCategory == SettingsCategory.ALL) {
            // Game Language & Localization Switcher (12 Global Languages)
            item {
              val activeLang = settings.activeLanguage
              SettingsSectionCard(
                title = gameStrings.languageSectionTitle,
                subtitle = "${gameStrings.languageSectionSubtitle} (${activeLang.nativeName})",
                icon = AppIcons.Public,
                accent = MaterialTheme.colorScheme.primary,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  AppLanguage.entries.forEach { lang ->
                    FilterChip(
                      selected = activeLang == lang,
                      onClick = {
                        if (onSelectLanguage != null) {
                          onSelectLanguage(lang.code)
                        } else {
                          onUpdateSettings { it.copy(selectedLanguageCode = lang.code) }
                        }
                      },
                      label = { Text("${lang.flagTag} • ${lang.nativeName}") },
                      modifier = Modifier.height(48.dp).testTag("settings_lang_${lang.code}"),
                    )
                  }
                }
              }
            }

            // One-Tap Health, Disability & Comfort Presets
            item {
              SettingsSectionCard(
                title = gameStrings.presetsTitle,
                subtitle = "Instant profiles tailored for epilepsy/migraine protection, motor dexterity, low vision, or battery life.",
                icon = AppIcons.AutoAwesome,
                accent = MaterialTheme.colorScheme.tertiary,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  OutlinedButton(
                    onClick = { onReplaceSettings(AppSettingsPresets.photosensitiveSafe(settings)) },
                    modifier = Modifier.height(48.dp).testTag("preset_photosensitive_btn"),
                  ) { Text("Photosensitive Safe") }
                  OutlinedButton(
                    onClick = { onReplaceSettings(AppSettingsPresets.motorAssist(settings)) },
                    modifier = Modifier.height(48.dp).testTag("preset_motor_btn"),
                  ) { Text("Motor / One-Hand Assist") }
                  OutlinedButton(
                    onClick = { onReplaceSettings(AppSettingsPresets.lowVisionContrast(settings)) },
                    modifier = Modifier.height(48.dp).testTag("preset_lowvision_btn"),
                  ) { Text("Low-Vision Contrast") }
                  OutlinedButton(
                    onClick = { onReplaceSettings(AppSettingsPresets.maxBatteryPerformance()) },
                    modifier = Modifier.height(48.dp).testTag("preset_battery_btn"),
                  ) { Text("Max Battery 30 FPS") }
                }
                OutlinedButton(
                  onClick = onResetDefaults,
                  modifier = Modifier.fillMaxWidth().height(48.dp).testTag("preset_reset_btn"),
                ) { Text(gameStrings.resetAllSettings) }
              }
            }
          }

          if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.AUDIO) {
            item {
              SettingsSectionCard(
                title = "SOUNDTRACK & AMBIENT MUSIC",
                subtitle = "Soft background music using your bundled tracks (defaults to 22% so combat SFX and dialogue stay comfortable).",
                icon = AppIcons.AutoAwesome,
                accent = Color(0xFF00E5FF),
              ) {
                SettingSwitchRow(
                  title = "Enable Background Music",
                  description = "Plays your soundtrack gently across menus and Arena combat.",
                  checked = settings.musicEnabled,
                  tag = "switch_music_enabled",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(musicEnabled = v) } },
                )
                SettingSliderRow(
                  title = "Music Volume (${(settings.musicVolume * 100).roundToInt()}%)",
                  description = "Capped softly (up to 70%) so tracks never overpower gameplay.",
                  value = settings.musicVolume,
                  range = 0.0f..0.70f,
                  steps = 13,
                  tag = "slider_music_volume",
                  onValueChange = { v -> onUpdateSettings { it.copy(musicVolume = v) } },
                )
                Text("Soundtrack Selection", style = MaterialTheme.typography.titleMedium)
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  listOf(
                    0 to "Auto Cycle Both",
                    1 to "My Feelings of Music",
                    2 to "Destiny Legendary Stories 1",
                  ).forEach { (idx, label) ->
                    FilterChip(
                      selected = settings.selectedMusicTrack == idx,
                      onClick = { onUpdateSettings { it.copy(selectedMusicTrack = idx) } },
                      label = { Text(label) },
                      modifier = Modifier.height(48.dp).testTag("chip_music_track_$idx"),
                    )
                  }
                }
              }
            }
          }

          if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.ACCESSIBILITY) {
            item {
              SettingsSectionCard(
                title = "ACCESSIBILITY, DISABILITIES & HEALTH SUPPORT",
                subtitle = "Designed for photosensitive epilepsy, vestibular motion sensitivity, colorblindness, low vision, hearing loss, and motor or cognitive fatigue.",
                icon = AppIcons.Accessibility,
                accent = Color(0xFF00E676),
              ) {
                SettingSwitchRow(
                  title = "Reduce Flashes (Photosensitivity & Migraine Safe)",
                  description = "Disables crimson damage screen-edge flashes, overdrive strobing, and bright impact pulses.",
                  checked = settings.reduceFlashes,
                  tag = "switch_reduce_flashes",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(reduceFlashes = v) } },
                )
                SettingSwitchRow(
                  title = "Reduce Motion (Vestibular & Nausea Relief)",
                  description = "Minimizes character bobbing, rain streaks, rapid pulsing rings, and screen particle bursts.",
                  checked = settings.reduceMotion,
                  tag = "switch_reduce_motion",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(reduceMotion = v) } },
                )
                SettingSwitchRow(
                  title = "High-Contrast Outlines & Clarity",
                  description = "Boosts UI contrast ratios and outlines player, enemies, drops, and Exfil zones clearly.",
                  checked = settings.highContrastMode,
                  tag = "switch_high_contrast",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(highContrastMode = v) } },
                )
                SettingSwitchRow(
                  title = "Visual Event Captions (Deaf / Hard-of-Hearing)",
                  description = "Keeps high-contrast text captions visible for bosses, power-ups, shield alerts, and events.",
                  checked = settings.alwaysShowActionCues,
                  tag = "switch_action_cues",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(alwaysShowActionCues = v) } },
                )
                SettingSwitchRow(
                  title = "Simplified HUD (Sensory & Cognitive Focus)",
                  description = "Hides secondary HUD badges and corner clutter so you can focus strictly on core action.",
                  checked = settings.simplifiedHud,
                  tag = "switch_simplified_hud",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(simplifiedHud = v) } },
                )
                SettingSwitchRow(
                  title = "Aegis Resilience Assist (Motor / Fatigue Support)",
                  description = "Increases passive shield regen and softens contact damage frequency for relaxed play.",
                  checked = settings.assistShieldBoost,
                  tag = "switch_assist_shield",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(assistShieldBoost = v) } },
                )

                Text("Color Vision Deficiency Filter", style = MaterialTheme.typography.titleMedium)
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  ColorBlindMode.entries.forEach { mode ->
                    FilterChip(
                      selected = settings.colorBlindMode == mode,
                      onClick = { onUpdateSettings { it.copy(colorBlindMode = mode) } },
                      label = { Text(mode.label) },
                      modifier = Modifier.height(48.dp).testTag("chip_cb_${mode.name.lowercase()}"),
                    )
                  }
                }

                SettingSliderRow(
                  title = "Low-Vision Text Scale (${(settings.textScale * 100).roundToInt()}%)",
                  description = "Scales all app menus, buttons, and Arena captions up to 140%.",
                  value = settings.textScale,
                  range = 1.0f..1.4f,
                  steps = 3,
                  tag = "slider_text_scale",
                  onValueChange = { v -> onUpdateSettings { it.copy(textScale = (v * 10f).roundToInt() / 10f) } },
                )

                Text("Combat Pace / Game Speed (${String.format(Locale.US, "%.2fx", settings.gameSpeedMultiplier)})", style = MaterialTheme.typography.titleMedium)
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  listOf(0.6f to "0.6x Relaxed", 0.8f to "0.8x Steady", 1.0f to "1.0x Normal", 1.25f to "1.25x Turbo").forEach { (spd, lbl) ->
                    FilterChip(
                      selected = kotlin.math.abs(settings.gameSpeedMultiplier - spd) < 0.05f,
                      onClick = { onUpdateSettings { it.copy(gameSpeedMultiplier = spd) } },
                      label = { Text(lbl) },
                      modifier = Modifier.height(48.dp).testTag("chip_speed_${(spd * 100).toInt()}"),
                    )
                  }
                }
              }
            }
          }

          if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.CONTROLS) {
            item {
              SettingsSectionCard(
                title = "CONTROLS & ERGONOMICS",
                subtitle = "One-handed aim/fire assist, left-handed mirrored layout, oversized touch targets, and haptics.",
                icon = AppIcons.Gamepad,
                accent = MaterialTheme.colorScheme.primary,
              ) {
                SettingSwitchRow(
                  title = "Left-Handed Mirrored Controls",
                  description = "Swaps the analog stick to the right side and combat action buttons to the left.",
                  checked = settings.leftHandedDeck,
                  tag = "switch_left_handed",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(leftHandedDeck = v) } },
                )
                SettingSwitchRow(
                  title = "Smart Auto-Aim Target Lock",
                  description = "Automatically locks onto the nearest hostile when not manually dragging aim.",
                  checked = settings.autoAimEnabled,
                  tag = "switch_auto_aim",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(autoAimEnabled = v) } },
                )
                SettingSwitchRow(
                  title = "Auto-Fire Default",
                  description = "Starts Arena runs with automatic weapon cadence active (reduces thumb strain).",
                  checked = settings.autoFireEnabled,
                  tag = "switch_auto_fire",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(autoFireEnabled = v) } },
                )
                SettingSwitchRow(
                  title = "Tap/Drag Anywhere on Arena to Aim",
                  description = "Allows pointing directly at targets on the battlefield with one finger or stylus.",
                  checked = settings.touchCanvasAimEnabled,
                  tag = "switch_touch_aim",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(touchCanvasAimEnabled = v) } },
                )
                SettingSwitchRow(
                  title = "Tactile Haptic Feedback",
                  description = "Subtle vibration confirmation on manual fire, powers, dashes, and stim activations.",
                  checked = settings.hapticFeedbackEnabled,
                  tag = "switch_haptics",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(hapticFeedbackEnabled = v) } },
                )
                SettingSliderRow(
                  title = "Joystick Size (${(settings.joystickScale * 100).roundToInt()}%)",
                  description = "Enlarge thumb stick target area for easier grip and tremor tolerance.",
                  value = settings.joystickScale,
                  range = 0.8f..1.4f,
                  steps = 5,
                  tag = "slider_joystick_scale",
                  onValueChange = { v -> onUpdateSettings { it.copy(joystickScale = v) } },
                )
                SettingSliderRow(
                  title = "Action Button Size (${(settings.buttonScale * 100).roundToInt()}%)",
                  description = "Increases Arena FIRE, POWER, STIM, and DASH button heights for dexterity.",
                  value = settings.buttonScale,
                  range = 1.0f..1.35f,
                  steps = 6,
                  tag = "slider_button_scale",
                  onValueChange = { v -> onUpdateSettings { it.copy(buttonScale = v) } },
                )
                Spacer(Modifier.height(8.dp))
                Text("Virtual Controller Layout Style", style = MaterialTheme.typography.titleMedium)
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  listOf(
                    "console_gamepad" to "Console Gamepad (ABXY + D-Pad)",
                    "codm_tactical" to "CODM Tactical HUD",
                    "floating_classic" to "Floating Classic",
                  ).forEach { (id, lbl) ->
                    FilterChip(
                      selected = settings.controllerStyle == id,
                      onClick = { onUpdateSettings { it.copy(controllerStyle = id) } },
                      label = { Text(lbl) },
                      modifier = Modifier.height(48.dp).testTag("chip_controller_$id"),
                    )
                  }
                }
                SettingSliderRow(
                  title = "Controller Opacity (${(settings.controllerOpacity * 100).roundToInt()}%)",
                  description = "Transparency of virtual controller buttons and sticks on screen.",
                  value = settings.controllerOpacity,
                  range = 0.3f..1.0f,
                  steps = 6,
                  tag = "slider_controller_opacity",
                  onValueChange = { v -> onUpdateSettings { it.copy(controllerOpacity = v) } },
                )
                SettingSliderRow(
                  title = "Controller Scale (${(settings.controllerScale * 100).roundToInt()}%)",
                  description = "Overall scaling factor for the compact virtual controller layout.",
                  value = settings.controllerScale.coerceIn(0.65f, 1.4f),
                  range = 0.65f..1.4f,
                  steps = 5,
                  tag = "slider_controller_scale",
                  onValueChange = { v -> onUpdateSettings { it.copy(controllerScale = v) } },
                )
                SettingSwitchRow(
                  title = "Show 4-Way Tactical D-Pad",
                  description = "Adds an interactive directional D-Pad (Weapon Swap, Interact, Spirit Heal, Traps) in Console Gamepad mode.",
                  checked = settings.controllerShowDpad,
                  tag = "switch_controller_dpad",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(controllerShowDpad = v) } },
                )
                Text("Controller Button Shape", style = MaterialTheme.typography.titleMedium)
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  listOf(
                    "gamepad_circle" to "Gamepad Circle",
                    "tactical_hex" to "Tactical Square",
                    "pill" to "Classic Pill",
                  ).forEach { (id, lbl) ->
                    FilterChip(
                      selected = settings.controllerButtonShape == id,
                      onClick = { onUpdateSettings { it.copy(controllerButtonShape = id) } },
                      label = { Text(lbl) },
                      modifier = Modifier.height(48.dp).testTag("chip_shape_$id"),
                    )
                  }
                }
                SettingSliderRow(
                  title = "Button Spacing (${settings.controllerSpacingDp} dp)",
                  description = "Adjust gap between gamepad face buttons, shoulder triggers, and thumbsticks.",
                  value = settings.controllerSpacingDp.toFloat(),
                  range = 2f..14f,
                  steps = 5,
                  tag = "slider_controller_spacing",
                  onValueChange = { v -> onUpdateSettings { it.copy(controllerSpacingDp = v.roundToInt()) } },
                )
                Spacer(Modifier.height(6.dp))
                Text("Remappable Face & Shoulder Bindings (Tap to Cycle)", style = MaterialTheme.typography.titleMedium)
                val actionCycle = listOf(
                  "fire", "melee", "power", "magic", "spirit_heal",
                  "dash", "interact", "surge", "lethal", "tactical",
                  "wonder", "traps", "swap_wpn", "pack"
                )
                fun nextBinding(cur: String): String {
                  val idx = actionCycle.indexOf(cur)
                  return actionCycle[(idx + 1) % actionCycle.size]
                }
                val slots = listOf(
                  Triple("A Button", settings.controllerActionA) { next: String -> onUpdateSettings { it.copy(controllerActionA = next) } },
                  Triple("B Button", settings.controllerActionB) { next: String -> onUpdateSettings { it.copy(controllerActionB = next) } },
                  Triple("X Button", settings.controllerActionX) { next: String -> onUpdateSettings { it.copy(controllerActionX = next) } },
                  Triple("Y Button", settings.controllerActionY) { next: String -> onUpdateSettings { it.copy(controllerActionY = next) } },
                  Triple("L1 Bumper", settings.controllerActionL1) { next: String -> onUpdateSettings { it.copy(controllerActionL1 = next) } },
                  Triple("R1 Bumper", settings.controllerActionR1) { next: String -> onUpdateSettings { it.copy(controllerActionR1 = next) } },
                  Triple("L2 Trigger", settings.controllerActionL2) { next: String -> onUpdateSettings { it.copy(controllerActionL2 = next) } },
                  Triple("R2 Trigger", settings.controllerActionR2) { next: String -> onUpdateSettings { it.copy(controllerActionR2 = next) } },
                )
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  slots.forEach { (slotLabel, currentAct, onSet) ->
                    FilterChip(
                      selected = true,
                      onClick = { onSet(nextBinding(currentAct)) },
                      label = { Text("$slotLabel: ${currentAct.uppercase()}") },
                      modifier = Modifier.height(48.dp).testTag("remap_${slotLabel.lowercase().replace(" ", "_")}"),
                    )
                  }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                  onClick = {
                    onUpdateSettings {
                      it.copy(
                        controllerStyle = "console_gamepad",
                        controllerOpacity = 0.85f,
                        controllerScale = 1.0f,
                        controllerShowDpad = true,
                        controllerSpacingDp = 6,
                        controllerButtonShape = "gamepad_circle",
                        controllerActionA = "fire",
                        controllerActionB = "melee",
                        controllerActionX = "interact",
                        controllerActionY = "power",
                        controllerActionL1 = "dash",
                        controllerActionR1 = "magic",
                        controllerActionL2 = "tactical",
                        controllerActionR2 = "lethal",
                      )
                    }
                  },
                  modifier = Modifier.height(48.dp).testTag("reset_controller_bindings_btn"),
                ) {
                  Text("Reset Controller Defaults")
                }
                SettingSliderRow(
                  title = "Movement Sensitivity (${String.format(Locale.US, "%.2fx", settings.moveSensitivity)})",
                  description = "Adjust how quickly operator movement responds to thumbstick deflection.",
                  value = settings.moveSensitivity,
                  range = 0.6f..1.6f,
                  steps = 9,
                  tag = "slider_move_sens",
                  onValueChange = { v -> onUpdateSettings { it.copy(moveSensitivity = v) } },
                )
              }
            }
          }

          if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.PERFORMANCE) {
            item {
              SettingsSectionCard(
                title = "PERFORMANCE & FRAME PACING",
                subtitle = "Tune frame rate cap, particle budget, radar, and live FPS telemetry.",
                icon = AppIcons.Speed,
                accent = Color(0xFFFFAB40),
              ) {
                Text("Target Frame Rate Cap (${settings.targetFpsCap} FPS)", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  listOf(30, 60, 120).forEach { fps ->
                    FilterChip(
                      selected = settings.targetFpsCap == fps,
                      onClick = { onUpdateSettings { it.copy(targetFpsCap = fps) } },
                      label = { Text("$fps FPS") },
                      modifier = Modifier.height(48.dp).testTag("chip_fps_$fps"),
                    )
                  }
                }

                Text("Particle & Impact Effects Density", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  ParticleDensity.entries.forEach { density ->
                    FilterChip(
                      selected = settings.particleDensity == density,
                      onClick = { onUpdateSettings { it.copy(particleDensity = density) } },
                      label = { Text(density.label) },
                      modifier = Modifier.height(48.dp).testTag("chip_particles_${density.name.lowercase()}"),
                    )
                  }
                }

                SettingSwitchRow(
                  title = "Show Live FPS Telemetry Counter",
                  description = "Displays real-time frames-per-second inside the Arena top bar.",
                  checked = settings.showFpsCounter,
                  tag = "switch_fps_counter",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(showFpsCounter = v) } },
                )
                SettingSwitchRow(
                  title = "Tactical Sector Grid Lines",
                  description = "Disable for lower GPU fill-rate and cleaner low-distraction arena floors.",
                  checked = settings.showTacticalGrid,
                  tag = "switch_tactical_grid",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(showTacticalGrid = v) } },
                )
                SettingSwitchRow(
                  title = "Corner Mini-Radar",
                  description = "Show enemy & boss positions on the top-right radar.",
                  checked = settings.showMiniRadar,
                  tag = "switch_mini_radar",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(showMiniRadar = v) } },
                )
              }
            }

            item {
              val activeRegion = settings.activeRegion
              val activeServer = settings.activeServer
              val regionServers = RegionalServerCatalog.serversForRegion(activeRegion)
              SettingsSectionCard(
                title = gameStrings.regionSectionTitle,
                subtitle = "${RegionalServerCatalog.servers.size} global servers across ${WorldRegion.entries.size} regions. Regional optimization settings are ON by default and automatically adjust when switching regions.",
                icon = AppIcons.Public,
                accent = Color(activeRegion.accentHex),
              ) {
                SettingSwitchRow(
                  title = "Auto-Apply Region Default Settings",
                  description = "Current profile (${activeRegion.shortTag}): ${activeRegion.defaultProfile.summary}",
                  checked = settings.useRegionDefaultSettings,
                  tag = "switch_region_auto_settings",
                  onCheckedChange = { enabled ->
                    if (onToggleRegionDefaultSettings != null) {
                      onToggleRegionDefaultSettings(enabled)
                    } else {
                      onUpdateSettings { cur ->
                        val next = cur.copy(useRegionDefaultSettings = enabled)
                        if (enabled) AppSettingsPresets.forRegion(next, next.activeRegion) else next
                      }
                    }
                  },
                )

                Text(
                  text = "Select World Region (${WorldRegion.entries.size} Regions)",
                  style = MaterialTheme.typography.titleMedium,
                )
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  WorldRegion.entries.forEach { region ->
                    FilterChip(
                      selected = activeRegion == region,
                      onClick = {
                        if (onSelectRegion != null) {
                          onSelectRegion(region.id)
                        } else {
                          val defaultSrv = RegionalServerCatalog.defaultServerFor(region)
                          onUpdateSettings { cur ->
                            val base = cur.copy(selectedRegionId = region.id, selectedServerId = defaultSrv.id)
                            if (base.useRegionDefaultSettings) AppSettingsPresets.forRegion(base, region) else base
                          }
                        }
                      },
                      label = { Text("${region.shortTag} • ${region.displayName}") },
                      modifier = Modifier.height(48.dp).testTag("settings_region_${region.id}"),
                    )
                  }
                }

                Text(
                  text = "${activeRegion.displayName} Servers (${regionServers.size} Nodes)",
                  style = MaterialTheme.typography.titleMedium,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  regionServers.forEach { srv ->
                    val isSrvSelected = activeServer.id == srv.id
                    Surface(
                      onClick = {
                        if (onSelectServer != null) {
                          onSelectServer(srv.id)
                        } else {
                          onUpdateSettings { cur ->
                            val base = cur.copy(selectedRegionId = srv.region.id, selectedServerId = srv.id)
                            if (base.useRegionDefaultSettings) AppSettingsPresets.forRegion(base, srv.region) else base
                          }
                        }
                      },
                      shape = RoundedCornerShape(12.dp),
                      color = if (isSrvSelected) Color(activeRegion.accentHex).copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                      modifier = Modifier
                        .fillMaxWidth()
                        .border(
                          width = if (isSrvSelected) 1.5.dp else 1.dp,
                          color = if (isSrvSelected) Color(activeRegion.accentHex) else MaterialTheme.colorScheme.outline,
                          shape = RoundedCornerShape(12.dp),
                        )
                        .testTag("settings_server_${srv.id}"),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = "${srv.name} (${srv.code})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSrvSelected) Color(activeRegion.accentHex) else MaterialTheme.colorScheme.onSurface,
                          )
                          Text(
                            text = "${srv.basePingMs}ms • ${srv.tickRateHz}Hz • ${srv.activeOperators}/${srv.maxCapacity}P • ${srv.bonusTag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                        if (isSrvSelected) {
                          Text(
                            text = "CONNECTED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.VISUAL) {
            item {
              SettingsSectionCard(
                title = "GRAPHICS & VISUAL DISPLAY",
                subtitle = "Theme contrast mode, graphics detail tier, glow aura, HUD opacity, and safe margins.",
                icon = AppIcons.Visibility,
                accent = MaterialTheme.colorScheme.secondary,
              ) {
                Text("Color Theme Mode", style = MaterialTheme.typography.titleMedium)
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  ThemeVisualMode.entries.forEach { mode ->
                    FilterChip(
                      selected = settings.themeMode == mode,
                      onClick = { onUpdateSettings { it.copy(themeMode = mode) } },
                      label = { Text(mode.label) },
                      modifier = Modifier.height(48.dp).testTag("chip_theme_${mode.name.lowercase()}"),
                    )
                  }
                }

                Text("Graphics Quality Tier", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  GraphicsQuality.entries.forEach { quality ->
                    FilterChip(
                      selected = settings.graphicsQuality == quality,
                      onClick = { onUpdateSettings { it.copy(graphicsQuality = quality) } },
                      label = { Text(quality.label) },
                      modifier = Modifier.height(48.dp).testTag("chip_gfx_${quality.name.lowercase()}"),
                    )
                  }
                }

                Text("RAM & Graphics Memory Profile", style = MaterialTheme.typography.titleMedium)
                Text(
                  text = settings.ramGraphicsMode.detailSummary,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  com.example.data.local.RamGraphicsMode.entries.forEach { ramMode ->
                    FilterChip(
                      selected = settings.ramGraphicsMode == ramMode,
                      onClick = {
                        onUpdateSettings {
                          it.copy(
                            ramGraphicsMode = ramMode,
                            ramGraphicsBudgetMb = ramMode.defaultBudgetMb,
                            ramShaderCacheEnabled = ramMode != com.example.data.local.RamGraphicsMode.LOW_RAM_BOOST,
                          )
                        }
                      },
                      label = { Text(ramMode.label) },
                      modifier = Modifier.height(48.dp).testTag("chip_ram_mode_${ramMode.id}"),
                    )
                  }
                }

                SettingSliderRow(
                  title = "RAM Graphics Budget (${settings.ramGraphicsBudgetMb} MB)",
                  description = "Controls arena particle/arc/bolt pool size and cached gradient slots. Lower = lighter RAM & steadier FPS; higher = maximum visual richness.",
                  value = settings.ramGraphicsBudgetMb.toFloat(),
                  range = 64f..1024f,
                  steps = 14,
                  tag = "slider_ram_budget",
                  onValueChange = { v -> onUpdateSettings { it.copy(ramGraphicsBudgetMb = v.roundToInt().coerceIn(64, 1024)) } },
                )

                SettingSwitchRow(
                  title = "RAM Shader & Backdrop LRU Cache",
                  description = "Caches compiled arena sky & floor gradient brushes in RAM (up to 16 slots) to avoid per-frame shader allocations.",
                  checked = settings.ramShaderCacheEnabled,
                  tag = "switch_ram_shader_cache",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(ramShaderCacheEnabled = v) } },
                )

                SettingSwitchRow(
                  title = "Full-Screen Immersive Display",
                  description = "Hides Android status bar, navigation bar, and Samsung Galaxy Tab taskbar across selected app scopes.",
                  checked = settings.immersiveFullscreen,
                  tag = "switch_immersive_fullscreen",
                  onCheckedChange = { v ->
                    onUpdateSettings {
                      it.copy(
                        immersiveFullscreen = v,
                        fullscreenMode = if (v) {
                          if (it.fullscreenMode == FullscreenMode.OFF) FullscreenMode.APP_WIDE else it.fullscreenMode
                        } else {
                          FullscreenMode.OFF
                        },
                      )
                    }
                  },
                )

                Text("Fullscreen Coverage Mode", style = MaterialTheme.typography.titleMedium)
                Text(
                  text = settings.fullscreenMode.summary,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  FullscreenMode.entries.forEach { fsMode ->
                    FilterChip(
                      selected = settings.fullscreenMode == fsMode,
                      onClick = {
                        onUpdateSettings {
                          it.copy(
                            fullscreenMode = fsMode,
                            immersiveFullscreen = fsMode != FullscreenMode.OFF,
                          )
                        }
                      },
                      label = { Text(fsMode.label) },
                      modifier = Modifier.height(48.dp).testTag("chip_fullscreen_mode_${fsMode.id}"),
                    )
                  }
                }

                Text("Game Collections & Viewport Resolution", style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "${settings.collectionResolutionMode.label} — ${settings.collectionResolutionMode.summary}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  CollectionResolutionMode.entries.forEach { resMode ->
                    FilterChip(
                      selected = settings.collectionResolutionMode == resMode,
                      onClick = {
                        onUpdateSettings { it.copy(collectionResolutionMode = resMode) }
                      },
                      label = { Text(resMode.label) },
                      modifier = Modifier.height(48.dp).testTag("chip_col_res_${resMode.id}"),
                    )
                  }
                }

                val safeMenuScalePct = (
                  (if (settings.menuScale.isFinite()) settings.menuScale else 1.0f) * 100f
                ).roundToInt()
                SettingSliderRow(
                  title = "Menu & UI Scale ($safeMenuScalePct%)",
                  description = "Scales menus, cards, tabs, and dialogs cleanly for compact phones or large tablets (75%–135%).",
                  value = if (settings.menuScale.isFinite()) settings.menuScale else 1.0f,
                  range = 0.75f..1.35f,
                  steps = 11,
                  tag = "slider_menu_scale",
                  onValueChange = { v ->
                    val clean = if (v.isFinite()) v.coerceIn(0.75f, 1.35f) else 1.0f
                    onUpdateSettings { it.copy(menuScale = clean) }
                  },
                )
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  listOf(
                    0.85f to "85% Compact",
                    1.0f to "100% Standard",
                    1.15f to "115% Comfort",
                    1.30f to "130% Tablet",
                  ).forEach { (presetScale, label) ->
                    val activeScale = kotlin.math.abs((if (settings.menuScale.isFinite()) settings.menuScale else 1.0f) - presetScale) < 0.03f
                    FilterChip(
                      selected = activeScale,
                      onClick = { onUpdateSettings { it.copy(menuScale = presetScale) } },
                      label = { Text(label) },
                      modifier = Modifier.height(48.dp).testTag("chip_menu_scale_${(presetScale * 100).roundToInt()}"),
                    )
                  }
                }

                SettingSwitchRow(
                  title = "Neon Glow & Aura Rings",
                  description = "Outer energy bloom around bolts, capes, weapons, and Overdrive rings.",
                  checked = settings.dynamicGlowEffects,
                  tag = "switch_dynamic_glow",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(dynamicGlowEffects = v) } },
                )

                SettingSliderRow(
                  title = "HUD Backdrop Opacity (${(settings.hudOpacity * 100).roundToInt()}%)",
                  description = "Make HUD panels more solid for legibility or more transparent for wider view.",
                  value = settings.hudOpacity,
                  range = 0.6f..1.0f,
                  steps = 7,
                  tag = "slider_hud_opacity",
                  onValueChange = { v -> onUpdateSettings { it.copy(hudOpacity = v) } },
                )

                SettingSliderRow(
                  title = "Edge Grip Margin (${settings.screenMarginDp} dp)",
                  description = "Adds protective inner padding away from curved edges, cases, or palms.",
                  value = settings.screenMarginDp.toFloat(),
                  range = 0f..16f,
                  steps = 3,
                  tag = "slider_screen_margin",
                  onValueChange = { v -> onUpdateSettings { it.copy(screenMarginDp = v.roundToInt()) } },
                )
              }
            }
          }

          if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.PROTECTION) {
            item {
              SettingsSectionCard(
                title = "FREE APP PROTECTION & PRIVACY SHIELD",
                subtitle = "On-device SHA-256 save integrity seals, optional salted PIN lock, screen-capture protection, and automated chat safety filtering.",
                icon = AppIcons.Shield,
                accent = Color(0xFF40C4FF),
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Text(
                      text = auditReport.summaryLine,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                      color = if (auditReport.progressionSealValid) Color(0xFF00E676) else MaterialTheme.colorScheme.error,
                      modifier = Modifier.testTag("integrity_audit_result"),
                    )
                    Text(
                      text = "Backup Disabled (allowBackup=false) • Local Salted PIN: ${if (pinEnabled) "ACTIVE" else "OFF"}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

                SettingSwitchRow(
                  title = "Screen Capture & Overlay Shield (FLAG_SECURE)",
                  description = "Blocks screenshots and screen recording while UnknownOS is open to protect private rooms and account info.",
                  checked = settings.screenCaptureShield,
                  tag = "switch_screen_shield",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(screenCaptureShield = v) } },
                )
                SettingSwitchRow(
                  title = "Chat & Room Input Safety Filter",
                  description = "Automatically strips HTML/control payloads and filters abusive terms in General, Clan, Friend & Live chats.",
                  checked = settings.chatSafetyFilter,
                  tag = "switch_chat_filter",
                  onCheckedChange = { v -> onUpdateSettings { it.copy(chatSafetyFilter = v) } },
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  OutlinedButton(
                    onClick = {
                      pinDraft = ""
                      pinError = null
                      showPinDialog = true
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("btn_manage_app_pin"),
                  ) {
                    Text(if (pinEnabled) "Change / Remove PIN" else "Set 4–6 Digit App Lock PIN")
                  }
                  OutlinedButton(
                    onClick = {
                      pinEnabled = AppSecurityManager.isPinEnabled(context)
                      lastAuditValid = onVerifyIntegrity()
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("btn_run_integrity_check"),
                  ) {
                    Text("Verify SHA-256 Seal")
                  }
                }
              }
            }
          }

        }

        // Sticky Bottom Action Bar
        Surface(
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().height(52.dp).testTag("done_settings_btn"),
            ) {
              Text(gameStrings.saveAndReturn, style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    }

    if (showPinDialog) {
      AlertDialog(
        onDismissRequest = { showPinDialog = false },
        title = { Text(if (pinEnabled) "Update App Lock PIN" else "Set Free App Lock PIN") },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Enter a 4 to 6 digit numeric PIN. Stored locally using salted 1000-round SHA-256.",
              style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
              value = pinDraft,
              onValueChange = { raw ->
                pinDraft = raw.filter { it.isDigit() }.take(6)
                pinError = null
              },
              label = { Text("4–6 Digit PIN") },
              singleLine = true,
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
              modifier = Modifier.fillMaxWidth().testTag("pin_setup_input"),
            )
            pinError?.let { err ->
              Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val result = AppSecurityManager.setAppPin(context, pinDraft)
              if (result.isSuccess) {
                pinEnabled = true
                showPinDialog = false
              } else {
                pinError = result.exceptionOrNull()?.message ?: "Enter 4 to 6 digits."
              }
            },
            modifier = Modifier.height(48.dp).testTag("confirm_app_pin_btn"),
          ) {
            Text("Save PIN")
          }
        },
        dismissButton = {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pinEnabled) {
              TextButton(
                onClick = {
                  AppSecurityManager.clearAppPin(context)
                  pinEnabled = false
                  showPinDialog = false
                },
                modifier = Modifier.height(48.dp).testTag("remove_app_pin_btn"),
              ) {
                Text("Remove PIN", color = MaterialTheme.colorScheme.error)
              }
            }
            TextButton(
              onClick = { showPinDialog = false },
              modifier = Modifier.height(48.dp).testTag("cancel_app_pin_btn"),
            ) {
              Text("Cancel")
            }
          }
        },
      )
    }
  }
}

@Composable
private fun SettingsSectionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accent: Color,
  content: @Composable () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.5.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = accent,
          modifier = Modifier.semantics { heading() },
        )
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      content()
    }
  }
}

@Composable
private fun SettingSwitchRow(
  title: String,
  description: String,
  checked: Boolean,
  tag: String,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
      Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(tag),
    )
  }
}

@Composable
private fun SettingSliderRow(
  title: String,
  description: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  steps: Int,
  tag: String,
  onValueChange: (Float) -> Unit,
) {
  val safeVal = if (value.isFinite()) value.coerceIn(range.start, range.endInclusive) else range.start
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Slider(
      value = safeVal,
      onValueChange = { next ->
        if (next.isFinite()) {
          onValueChange(next.coerceIn(range.start, range.endInclusive))
        }
      },
      valueRange = range,
      steps = steps,
      modifier = Modifier.fillMaxWidth().testTag(tag),
    )
  }
}

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.AppSettings
import com.example.data.local.AppSettingsPresets
import com.example.data.local.ColorBlindMode
import com.example.data.local.GraphicsQuality
import com.example.data.local.ParticleDensity
import com.example.data.local.ThemeVisualMode
import com.example.ui.theme.AppIcons
import java.util.Locale
import kotlin.math.roundToInt

private enum class SettingsCategory(val id: String, val label: String) {
  ALL("all", "All"),
  CONTROLS("controls", "Controls"),
  PERFORMANCE("performance", "Performance"),
  VISUAL("visual", "Visuals"),
  ACCESSIBILITY("accessibility", "Health & Access"),
}

@Composable
fun SettingsDialog(
  settings: AppSettings,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
  onReplaceSettings: (AppSettings) -> Unit,
  onResetDefaults: () -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxSize().testTag("settings_dialog_root"),
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
              label = { Text(cat.label) },
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
          // One-Tap Health, Disability & Comfort Presets
          item {
            SettingsSectionCard(
              title = "ONE-TAP COMFORT & HEALTH PRESETS",
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
              ) { Text("Reset All Settings to Defaults") }
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

          item {
            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().height(52.dp).testTag("done_settings_btn"),
            ) {
              Text("Save & Return", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(16.dp))
          }
        }
      }
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
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Slider(
      value = value.coerceIn(range.start, range.endInclusive),
      onValueChange = onValueChange,
      valueRange = range,
      steps = steps,
      modifier = Modifier.fillMaxWidth().testTag(tag),
    )
  }
}

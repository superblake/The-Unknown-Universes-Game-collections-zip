package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CollectionCustomModeConfig
import com.example.data.model.CollectionCustomScope
import com.example.data.model.CustomMapGameCategory
import com.example.data.model.UserCustomGameMapSpec
import com.example.data.model.UserCustomWeaponSpec
import kotlin.math.roundToInt

@Composable
fun CollectionCustomModeDialog(
  initialConfig: CollectionCustomModeConfig,
  onSave: ((CollectionCustomModeConfig) -> CollectionCustomModeConfig) -> Unit,
  onDeploy: () -> Unit,
  onDismiss: () -> Unit,
  customGameMaps: List<UserCustomGameMapSpec> = emptyList(),
  customWeapons: List<UserCustomWeaponSpec> = emptyList(),
  onOpenForgeMap: ((CustomMapGameCategory) -> Unit)? = null,
  onOpenForgeWeapon: ((CustomMapGameCategory) -> Unit)? = null,
) {
  var draft by remember(initialConfig) { mutableStateOf(initialConfig) }
  val scopedAreas = remember(draft.scope, customGameMaps) {
    CollectionCustomModeConfig.scopedAreas(draft.scope, customGameMaps)
  }
  val resolvedArea = remember(draft.scope, draft.targetAreaId, customGameMaps) {
    scopedAreas.firstOrNull { it.id == draft.targetAreaId } ?: scopedAreas.firstOrNull()
  }

  val accentColor = when {
    !draft.enabled -> Color(0xFF90A4AE)
    draft.negativeImpactPercent >= 65 -> Color(0xFFFF5252)
    draft.negativeImpactPercent >= 35 -> Color(0xFFFF9100)
    else -> Color(0xFF00E5FF)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "BO3 • STEAM • COLLECTIONS CUSTOM MODE",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = accentColor,
        )
        Text(
          text = if (draft.enabled) draft.impactLabel else "Ready (Safe Guarded • Scoped to BO3, Steam & Collections)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 460.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Normal Mode <-> Custom Mode Switcher
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = !draft.enabled,
            onClick = { draft = draft.copy(enabled = false) },
            label = { Text("Normal Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("custom_mode_switch_normal"),
          )
          FilterChip(
            selected = draft.enabled,
            onClick = { draft = draft.copy(enabled = true) },
            label = { Text("Custom Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("custom_mode_switch_custom"),
          )
        }

        // Collection Safeguard Card
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (draft.safeCollectionGuard) {
            Color(0xFF00E676).copy(alpha = 0.14f)
          } else {
            Color(0xFFFF9100).copy(alpha = 0.14f)
          },
          modifier = Modifier
            .fillMaxWidth()
            .border(
              1.dp,
              if (draft.safeCollectionGuard) Color(0xFF00E676).copy(alpha = 0.55f) else Color(0xFFFF9100).copy(alpha = 0.55f),
              RoundedCornerShape(12.dp),
            ),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Collection Safeguard (Recommended)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (draft.safeCollectionGuard) Color(0xFF00E676) else Color(0xFFFF9100),
              )
              Text(
                text = draft.collectionSafetyNote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
              checked = draft.safeCollectionGuard,
              onCheckedChange = { guardOn ->
                draft = draft.copy(safeCollectionGuard = guardOn)
              },
              modifier = Modifier.testTag("custom_mode_safeguard_switch"),
            )
          }
        }

        // Scope Selection (strictly BO3 / Steam / Collections)
        Text(
          text = "CUSTOM SCOPE (STRICTLY BO3 / STEAM / COLLECTIONS)",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(CollectionCustomScope.entries, key = { it.id }) { sc ->
            FilterChip(
              selected = draft.scope == sc,
              onClick = {
                val nextAreas = CollectionCustomModeConfig.scopedAreas(sc, customGameMaps)
                val nextAreaId = if (nextAreas.any { it.id == draft.targetAreaId }) {
                  draft.targetAreaId
                } else {
                  nextAreas.firstOrNull()?.id ?: draft.targetAreaId
                }
                draft = draft.copy(scope = sc, targetAreaId = nextAreaId)
              },
              label = { Text(sc.label, style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.heightIn(min = 48.dp).testTag("custom_mode_scope_${sc.id}"),
            )
          }
        }
        Text(
          text = draft.scope.shortNote,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Presets
        Text(
          text = "QUICK IMPACT PRESETS",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          OutlinedButton(
            onClick = {
              draft = CollectionCustomModeConfig.severeNegativePreset(
                scope = draft.scope,
                areaId = resolvedArea?.id ?: draft.targetAreaId,
              ).copy(safeCollectionGuard = draft.safeCollectionGuard)
            },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("custom_mode_severe_preset_btn"),
          ) {
            Text("High Risk", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = {
              draft = CollectionCustomModeConfig.mildStrainPreset(
                scope = draft.scope,
                areaId = resolvedArea?.id ?: draft.targetAreaId,
              ).copy(safeCollectionGuard = draft.safeCollectionGuard)
            },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("custom_mode_mild_preset_btn"),
          ) {
            Text("Balanced", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = {
              draft = CollectionCustomModeConfig.cleanPreset(
                scope = draft.scope,
                areaId = resolvedArea?.id ?: draft.targetAreaId,
              ).copy(safeCollectionGuard = draft.safeCollectionGuard)
            },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("custom_mode_clear_btn"),
          ) {
            Text("Clean", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }
        }

        // Scoped Target Map
        Text(
          text = "TARGET MAP (${scopedAreas.size} SCOPED)",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(scopedAreas, key = { it.id }) { area ->
            FilterChip(
              selected = resolvedArea?.id == area.id,
              onClick = { draft = draft.copy(targetAreaId = area.id) },
              label = {
                Text(
                  text = area.name,
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              modifier = Modifier.heightIn(min = 48.dp).testTag("custom_mode_area_${area.id}"),
            )
          }
        }

        if (onOpenForgeMap != null || onOpenForgeWeapon != null) {
          val suggestedCategory = when (draft.scope) {
            CollectionCustomScope.BO3_ONLY -> CustomMapGameCategory.BO3
            CollectionCustomScope.COLLECTIONS_ONLY -> CustomMapGameCategory.ARKHAM
            else -> CustomMapGameCategory.BO3
          }
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF00E5FF).copy(alpha = 0.1f),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "IN-COLLECTION FORGE (${customGameMaps.size} Maps • ${customWeapons.size} Custom Weapons)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                if (onOpenForgeMap != null) {
                  OutlinedButton(
                    onClick = { onOpenForgeMap(suggestedCategory) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("collection_forge_map_btn"),
                  ) {
                    Text("+ Forge Map", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  }
                }
                if (onOpenForgeWeapon != null) {
                  OutlinedButton(
                    onClick = { onOpenForgeWeapon(suggestedCategory) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("collection_forge_weapon_btn"),
                  ) {
                    Text("+ Forge Weapon", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        // Enemy HP Boost Slider
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          val maxUiHp = if (draft.safeCollectionGuard) 75 else 100
          Text(
            text = "Hostile Health Boost: +${draft.effectiveCappedHpPercent}% (Cap +$maxUiHp% so enemies never become broken sponges)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Slider(
            value = draft.enemyHpBoostPercent.toFloat().coerceIn(0f, maxUiHp.toFloat()),
            onValueChange = { v ->
              draft = draft.copy(enabled = true, enemyHpBoostPercent = (v.roundToInt() / 5) * 5)
            },
            valueRange = 0f..maxUiHp.toFloat(),
            colors = SliderDefaults.colors(
              thumbColor = accentColor,
              activeTrackColor = accentColor,
            ),
            modifier = Modifier.fillMaxWidth().testTag("custom_mode_hp_slider"),
          )
        }

        // Negative Modifiers (Guarded)
        CustomModifierSwitchRow(
          title = "Elite Armor Surge",
          subtitle = if (draft.safeCollectionGuard) {
            "Adds extra armor ONLY to Elites & Heavies — normal enemies stay unarmored."
          } else {
            "Reinforces armor across hostile waves (wave 3+ normal + elites)."
          },
          checked = draft.enemyArmorFlood,
          onCheckedChange = { draft = draft.copy(enabled = true, enemyArmorFlood = it) },
          testTag = "custom_mode_armor_switch",
        )

        CustomModifierSwitchRow(
          title = "Power & Cooldown Suppression",
          subtitle = if (draft.safeCollectionGuard) {
            "-25% ability/weapon bonus damage & +35% cooldowns (all collection powers stay usable)."
          } else {
            "-35% power scaling & +55% cooldown delay."
          },
          checked = draft.powerAndCooldownSuppression,
          onCheckedChange = { draft = draft.copy(enabled = true, powerAndCooldownSuppression = it) },
          testTag = "custom_mode_power_cd_switch",
        )

        CustomModifierSwitchRow(
          title = "Essence & Shard Run Drain",
          subtitle = if (draft.safeCollectionGuard) {
            "-35% match bonus yield only; your saved bank, tiers & stars are never reduced."
          } else {
            "-45% match yield during scoped runs (bank still untouched)."
          },
          checked = draft.essenceAndShardDrain,
          onCheckedChange = { draft = draft.copy(enabled = true, essenceAndShardDrain = it) },
          testTag = "custom_mode_essence_drain_switch",
        )

        CustomModifierSwitchRow(
          title = "Shield Attrition Leak",
          subtitle = if (draft.safeCollectionGuard) {
            "+25% incoming damage & -45% passive shield regen."
          } else {
            "+40% incoming damage & -65% passive shield regen."
          },
          checked = draft.shieldAttritionLeak,
          onCheckedChange = { draft = draft.copy(enabled = true, shieldAttritionLeak = it) },
          testTag = "custom_mode_shield_leak_switch",
        )

        CustomModifierSwitchRow(
          title = "Collection Ambush Surge",
          subtitle = if (draft.safeCollectionGuard) {
            "+25% hostile speed, 1.25x spawn density & ambush boss on non-boss waves."
          } else {
            "+40% hostile speed & 1.35x spawn pressure."
          },
          checked = draft.collectionAmbushSurge,
          onCheckedChange = { draft = draft.copy(enabled = true, collectionAmbushSurge = it) },
          testTag = "custom_mode_ambush_switch",
        )

        CustomModifierSwitchRow(
          title = "Suppress Random Crate Wonder Drops",
          subtitle = "Stops random crate/mob Wonder drops while keeping your equipped Collection Wonder Weapons 100% active.",
          checked = draft.disableWonderDrops,
          onCheckedChange = { draft = draft.copy(enabled = true, disableWonderDrops = it) },
          testTag = "custom_mode_no_wonder_switch",
        )
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = {
            val savedTarget = resolvedArea?.id ?: draft.targetAreaId
            onSave { draft.copy(targetAreaId = savedTarget) }
            onDismiss()
          },
          modifier = Modifier.heightIn(min = 48.dp).testTag("custom_mode_save_btn"),
        ) {
          Text(if (draft.enabled) "Save" else "Save Normal")
        }
        Button(
          onClick = {
            val enabledDraft = draft.copy(
              enabled = true,
              targetAreaId = resolvedArea?.id ?: draft.targetAreaId,
            )
            onSave { enabledDraft }
            onDismiss()
            onDeploy()
          },
          colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
          modifier = Modifier.heightIn(min = 48.dp).testTag("custom_mode_deploy_btn"),
        ) {
          Text("Deploy Custom", fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (draft.enabled) {
          TextButton(
            onClick = {
              onSave { draft.copy(enabled = false) }
              onDismiss()
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("custom_mode_disable_btn"),
          ) {
            Text("Turn Off")
          }
        }
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.heightIn(min = 48.dp).testTag("custom_mode_cancel_btn"),
        ) {
          Text("Cancel")
        }
      }
    },
  )
}

@Composable
private fun CustomModifierSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(Modifier.width(8.dp))
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.testTag(testTag),
      )
    }
  }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomMapGameCategory
import java.util.Locale
import kotlin.math.roundToInt

private val customWeaponElements: List<Pair<String, String>> = listOf(
  "kinetic" to "Kinetic",
  "energy" to "Energy",
  "thermal" to "Thermal",
  "cryo" to "Cryo",
  "bio" to "Bio / Blacklight",
  "sand" to "Sand / Earth",
  "rock" to "Rock / Tectonic",
  "explosive" to "Explosive / Ordnance",
  "void" to "Void / Cosmic",
)

private val customWeaponTracerPalette: List<Pair<String, Long>> = listOf(
  "Cyan" to 0xFF00E5FF,
  "Emerald" to 0xFF00E676,
  "Crimson" to 0xFFFF3D71,
  "Gold" to 0xFFFFD54F,
  "Violet" to 0xFFE040FB,
  "Kryptonian" to 0xFFFF5252,
)

@Composable
fun ForgeCustomWeaponDialog(
  initialCategory: CustomMapGameCategory = CustomMapGameCategory.BO3,
  onDismiss: () -> Unit,
  onConfirm: (
    name: String,
    weaponCode: String,
    category: CustomMapGameCategory,
    description: String,
    damageTypeId: String,
    baseDamage: Int,
    baseCadenceTicks: Float,
    baseSpreadCount: Int,
    basePierce: Int,
    tracerHex: Long,
    equipNow: Boolean,
  ) -> Unit,
) {
  var category by remember(initialCategory) { mutableStateOf(initialCategory) }
  var name by remember { mutableStateOf("") }
  var weaponCode by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var damageTypeId by remember { mutableStateOf("energy") }
  var baseDamage by remember { mutableIntStateOf(8) }
  var baseCadenceTicks by remember { mutableFloatStateOf(7f) }
  var baseSpreadCount by remember { mutableIntStateOf(2) }
  var basePierce by remember { mutableIntStateOf(1) }
  var tracerHex by remember(initialCategory) { mutableLongStateOf(initialCategory.defaultAccentHex) }

  val accentColor = Color(tracerHex)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    com.example.util.SyncDialogFullscreen()
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).testTag("forge_custom_weapon_dialog"),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        ) {
          Text(
            text = "FORGE CUSTOM WEAPON",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
          )
          Text(
            text = "Build custom BO3 & Game Collection weapons with element, spread, pierce & tracers",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(12.dp))

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 440.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "1-TAP QUICK FORGE PRESETS",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = accentColor,
            )
            val weaponPresets = listOf(
              Triple("Ray Cannon Mk.IV", CustomMapGameCategory.BO3, "energy"),
              Triple("Conduit Amp Rifle", CustomMapGameCategory.INFAMOUS, "energy"),
              Triple("Arkham Shock Blaster", CustomMapGameCategory.ARKHAM, "kinetic"),
              Triple("Spartan Solar Spear", CustomMapGameCategory.GODOFWAR, "thermal"),
              Triple("Abyss Void Edge", CustomMapGameCategory.ABYSS_DUNGEONS, "void"),
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              weaponPresets.forEach { (pName, pCat, pElem) ->
                FilterChip(
                  selected = name == pName,
                  onClick = {
                    name = pName
                    category = pCat
                    damageTypeId = pElem
                    tracerHex = pCat.defaultAccentHex
                    baseDamage = 12
                    baseCadenceTicks = 6f
                    baseSpreadCount = 2
                    basePierce = 2
                  },
                  label = { Text(pName, style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.heightIn(min = 48.dp),
                )
              }
            }
            Text(
              text = "GAME COLLECTION ORIGIN",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              CustomMapGameCategory.entries.forEach { cat ->
                FilterChip(
                  selected = category == cat,
                  onClick = {
                    val prevDefaultHex = category.defaultAccentHex
                    category = cat
                    if (tracerHex == prevDefaultHex) {
                      tracerHex = cat.defaultAccentHex
                    }
                  },
                  label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.heightIn(min = 48.dp).testTag("forge_wpn_cat_${cat.id}"),
                )
              }
            }

            OutlinedTextField(
              value = name,
              onValueChange = { name = it.take(44) },
              label = { Text("Weapon Name") },
              placeholder = {
                Text(
                  when (category) {
                    CustomMapGameCategory.BO3 -> "Ray Cannon Mk.IV"
                    else -> "${category.label.substringBefore(' ')} Prototype Rifle"
                  }
                )
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("forge_wpn_name_input"),
            )

            OutlinedTextField(
              value = weaponCode,
              onValueChange = { weaponCode = it.take(28) },
              label = { Text("Weapon Code / Blueprint ID") },
              placeholder = { Text("${category.id}_wpn_mk1") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("forge_wpn_code_input"),
            )

            OutlinedTextField(
              value = description,
              onValueChange = { description = it.take(160) },
              label = { Text("Weapon Notes (Optional)") },
              placeholder = { Text("Custom-tuned bolt matrix & tactical core") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("forge_wpn_desc_input"),
            )

            Text(
              text = "DAMAGE ELEMENT TYPE",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              customWeaponElements.forEach { (elemId, elemLabel) ->
                FilterChip(
                  selected = damageTypeId == elemId,
                  onClick = { damageTypeId = elemId },
                  label = { Text(elemLabel, style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.heightIn(min = 48.dp).testTag("forge_wpn_elem_$elemId"),
                )
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Base Damage: $baseDamage (3 – 25)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
              )
              Slider(
                value = baseDamage.toFloat(),
                onValueChange = { baseDamage = it.roundToInt().coerceIn(3, 25) },
                valueRange = 3f..25f,
                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                modifier = Modifier.fillMaxWidth().testTag("forge_wpn_dmg_slider"),
              )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Fire Cadence Interval: ${String.format(Locale.US, "%.1f", baseCadenceTicks)} ticks (${if (baseCadenceTicks <= 5f) "Rapid" else if (baseCadenceTicks <= 9f) "Balanced" else "Heavy"})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
              )
              Slider(
                value = baseCadenceTicks,
                onValueChange = { baseCadenceTicks = it.coerceIn(3f, 16f) },
                valueRange = 3f..16f,
                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                modifier = Modifier.fillMaxWidth().testTag("forge_wpn_cadence_slider"),
              )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Multi-Shot Spread: $baseSpreadCount bolts (1 – 5)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
              )
              Slider(
                value = baseSpreadCount.toFloat(),
                onValueChange = { baseSpreadCount = it.roundToInt().coerceIn(1, 5) },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                modifier = Modifier.fillMaxWidth().testTag("forge_wpn_spread_slider"),
              )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Target Pierce Depth: $basePierce (0 – 6)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
              )
              Slider(
                value = basePierce.toFloat(),
                onValueChange = { basePierce = it.roundToInt().coerceIn(0, 6) },
                valueRange = 0f..6f,
                steps = 5,
                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                modifier = Modifier.fillMaxWidth().testTag("forge_wpn_pierce_slider"),
              )
            }

            Text(
              text = "TRACER BOLT COLOR",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              customWeaponTracerPalette.forEachIndexed { idx, (_, hex) ->
                val col = Color(hex)
                val selected = tracerHex == hex
                Box(
                  modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(col.copy(alpha = 0.28f))
                    .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else col, CircleShape)
                    .clickable { tracerHex = hex }
                    .padding(8.dp)
                    .testTag("forge_wpn_tracer_$idx"),
                ) {
                  Box(
                    modifier = Modifier
                      .size(32.dp)
                      .clip(CircleShape)
                      .background(col),
                  )
                }
              }
            }
          }

          Spacer(Modifier.height(14.dp))

          val parsedName = name.trim().ifBlank {
            "${category.label.substringBefore(' ')} Forge Cannon"
          }
          val parsedCode = weaponCode.trim().ifBlank { "${category.id}_custom_wpn" }
          val parsedDesc = description.trim().ifBlank {
            "User-forged ${category.label} ${damageTypeId.uppercase()} weapon (${baseDamage} DMG • ${baseSpreadCount}x spread • P$basePierce)."
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(
              onClick = onDismiss,
              modifier = Modifier.heightIn(min = 48.dp).testTag("forge_wpn_cancel_btn"),
            ) {
              Text("Cancel")
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(
              onClick = {
                onConfirm(
                  parsedName,
                  parsedCode,
                  category,
                  parsedDesc,
                  damageTypeId,
                  baseDamage,
                  baseCadenceTicks,
                  baseSpreadCount,
                  basePierce,
                  tracerHex,
                  false,
                )
              },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.heightIn(min = 48.dp).testTag("forge_wpn_save_btn"),
            ) {
              Text("Save", fontWeight = FontWeight.SemiBold)
            }
            Button(
              onClick = {
                onConfirm(
                  parsedName,
                  parsedCode,
                  category,
                  parsedDesc,
                  damageTypeId,
                  baseDamage,
                  baseCadenceTicks,
                  baseSpreadCount,
                  basePierce,
                  tracerHex,
                  true,
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.heightIn(min = 48.dp).testTag("forge_wpn_equip_btn"),
            ) {
              Text("Forge & Equip", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

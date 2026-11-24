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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

private val customMapAccentPalette: List<Pair<String, Long>> = listOf(
  "Element 115 Green" to 0xFF00E676,
  "Conduit Cyan" to 0xFF00E5FF,
  "Blacklight Rose" to 0xFFFF3D71,
  "Arkham Gold" to 0xFFFFD54F,
  "Spartan Crimson" to 0xFFFF5252,
  "Chrono Violet" to 0xFFE040FB,
)

@Composable
fun ForgeCustomMapDialog(
  initialCategory: CustomMapGameCategory = CustomMapGameCategory.BO3,
  onDismiss: () -> Unit,
  onConfirm: (
    title: String,
    mapCode: String,
    category: CustomMapGameCategory,
    description: String,
    zones: List<String>,
    wonderWeapon: String,
    accentHex: Long,
    deployNow: Boolean,
  ) -> Unit,
) {
  var category by remember(initialCategory) { mutableStateOf(initialCategory) }
  var title by remember { mutableStateOf("") }
  var mapCode by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var wonderWeapon by remember(initialCategory) { mutableStateOf(initialCategory.defaultWonderWeapon) }
  var zonesText by remember(initialCategory) {
    mutableStateOf(initialCategory.defaultZones.joinToString(", "))
  }
  var accentHex by remember(initialCategory) { mutableLongStateOf(initialCategory.defaultAccentHex) }

  val accentColor = Color(accentHex)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
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
        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
      ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
      ) {
        Text(
          text = "FORGE CUSTOM GAME MAP",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = accentColor,
        )
        Text(
          text = "Create custom BO3 Mod Tools or Game Collection playable maps",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "GAME / COLLECTION CATEGORY",
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
                  val prevDefaultWw = category.defaultWonderWeapon
                  val prevDefaultZones = category.defaultZones.joinToString(", ")
                  val prevDefaultHex = category.defaultAccentHex
                  category = cat
                  if (wonderWeapon == prevDefaultWw || wonderWeapon.isBlank()) {
                    wonderWeapon = cat.defaultWonderWeapon
                  }
                  if (zonesText == prevDefaultZones || zonesText.isBlank()) {
                    zonesText = cat.defaultZones.joinToString(", ")
                  }
                  if (accentHex == prevDefaultHex) {
                    accentHex = cat.defaultAccentHex
                  }
                },
                label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("forge_map_cat_${cat.id}"),
              )
            }
          }

          OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(48) },
            label = { Text("Map Title") },
            placeholder = {
              Text(
                if (category == CustomMapGameCategory.BO3) "BO3 Custom: Der Eisendrache Remixed"
                else "Custom ${category.label.substringBefore(' ')}: Sector Alpha"
              )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("forge_map_title_input"),
          )

          OutlinedTextField(
            value = mapCode,
            onValueChange = { mapCode = it.take(32) },
            label = { Text("Map Code / Mod ID") },
            placeholder = {
              Text(if (category == CustomMapGameCategory.BO3) "zm_custom_bunker" else "${category.id}_custom_arena")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("forge_map_code_input"),
          )

          OutlinedTextField(
            value = wonderWeapon,
            onValueChange = { wonderWeapon = it.take(48) },
            label = { Text("Featured Wonder Weapon") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("forge_map_ww_input"),
          )

          OutlinedTextField(
            value = zonesText,
            onValueChange = { zonesText = it.take(240) },
            label = { Text("Custom Zones / Buildings (comma-separated)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("forge_map_zones_input"),
          )

          OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(180) },
            label = { Text("Map Notes (Optional)") },
            placeholder = { Text("Custom Mystery Box, Pack-a-Punch & safe routes") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("forge_map_desc_input"),
          )

          Text(
            text = "ARENA ACCENT SIGNATURE",
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
            customMapAccentPalette.forEachIndexed { idx, (_, hex) ->
              val col = Color(hex)
              val selected = accentHex == hex
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(col.copy(alpha = 0.28f))
                  .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else col, CircleShape)
                  .clickable { accentHex = hex }
                  .padding(8.dp)
                  .testTag("forge_map_accent_$idx"),
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

        val parsedTitle = title.trim().ifBlank {
          if (category == CustomMapGameCategory.BO3) "BO3 Custom: Sector Forge"
          else "Custom ${category.label.substringBefore(' ')} Arena"
        }
        val parsedCode = mapCode.trim().ifBlank {
          if (category == CustomMapGameCategory.BO3) "zm_forge_arena" else "${category.id}_forge_arena"
        }
        val parsedZones = zonesText.split(',').map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty {
          category.defaultZones
        }
        val parsedWw = wonderWeapon.trim().ifBlank { category.defaultWonderWeapon }
        val parsedDesc = description.trim().ifBlank {
          "User-forged ${category.label} custom playable map featuring $parsedWw."
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.heightIn(min = 48.dp).testTag("forge_map_cancel_btn"),
          ) {
            Text("Cancel")
          }
          Spacer(Modifier.weight(1f))
          OutlinedButton(
            onClick = {
              onConfirm(parsedTitle, parsedCode, category, parsedDesc, parsedZones, parsedWw, accentHex, false)
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.heightIn(min = 48.dp).testTag("forge_map_save_btn"),
          ) {
            Text("Save Map", fontWeight = FontWeight.SemiBold)
          }
          Button(
            onClick = {
              onConfirm(parsedTitle, parsedCode, category, parsedDesc, parsedZones, parsedWw, accentHex, true)
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.heightIn(min = 48.dp).testTag("forge_map_play_btn"),
          ) {
            Text("Forge & Play", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
}

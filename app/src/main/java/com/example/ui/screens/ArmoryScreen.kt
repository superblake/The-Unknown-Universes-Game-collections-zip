package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import com.example.ui.theme.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.R
import com.example.data.model.ArmoryItem

@Composable
fun ArmoryScreen(
  armory: List<ArmoryItem>,
  totalEssence: Long,
  onToggleEquip: (ArmoryItem) -> Unit,
  onUpgradeItem: (ArmoryItem) -> Unit,
  onCreateCustomItem: (
    name: String,
    category: String,
    universeTag: String,
    description: String,
    cooldownSec: Long,
    powerValue: Long,
    accentHex: String,
  ) -> Unit,
  onDeleteItem: (String) -> Unit,
) {
  val categories = listOf("all", "perk", "power", "upgrade", "consumable")
  var selectedCategory by remember { mutableStateOf("all") }
  var showCreateDialog by remember { mutableStateOf(false) }

  val filtered =
    if (selectedCategory == "all") armory else armory.filter { it.category == selectedCategory }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("MULTIVERSE ARMORY", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
              Text("Equip & Pack-a-Tier Perks, Powers, Upgrades & Consumables", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
            ) {
              Text(
                text = "$totalEssence ESSENCE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
      }

      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(categories) { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = {
                Text(
                  text = when (cat) {
                    "all" -> "All (${armory.size})"
                    "perk" -> "Perks"
                    "power" -> "Powers"
                    "upgrade" -> "Upgrades"
                    else -> "Consumables"
                  }
                )
              },
              modifier = Modifier.testTag("armory_filter_$cat"),
            )
          }
        }
      }

      items(filtered, key = { it.id }) { item ->
        ArmoryModuleCard(
          item = item,
          totalEssence = totalEssence,
          onToggleEquip = { onToggleEquip(item) },
          onUpgrade = { onUpgradeItem(item) },
          onDelete = if (item.isCustom) ({ onDeleteItem(item.id) }) else null,
        )
      }
    }

    FloatingActionButton(
      onClick = { showCreateDialog = true },
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
        .testTag("forge_custom_module_fab"),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.forge_custom_module))
        Spacer(Modifier.width(6.dp))
        Text("Forge Module", style = MaterialTheme.typography.labelLarge)
      }
    }
  }

  if (showCreateDialog) {
    ForgeArmoryItemDialog(
      onDismiss = { showCreateDialog = false },
      onConfirm = { name, cat, tag, desc, cd, power, hex ->
        onCreateCustomItem(name, cat, tag, desc, cd, power, hex)
        showCreateDialog = false
      },
    )
  }
}

@Composable
private fun ArmoryModuleCard(
  item: ArmoryItem,
  totalEssence: Long,
  onToggleEquip: () -> Unit,
  onUpgrade: () -> Unit,
  onDelete: (() -> Unit)?,
) {
  val accent = parseHexSafe(item.accentColorHex, MaterialTheme.colorScheme.primary)
  val upgradeCost = item.tier * 300L
  val canUpgrade = item.tier < item.maxTier && totalEssence >= upgradeCost

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxWidth()
      .border(
        width = if (item.equipped) 1.5.dp else 1.dp,
        color = if (item.equipped) accent else MaterialTheme.colorScheme.outline,
        shape = RoundedCornerShape(16.dp),
      ),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = accent.copy(alpha = 0.18f),
            ) {
              Text(
                text = item.category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
            Text(
              text = item.universeTag,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(Modifier.height(2.dp))
          Text(
            text = item.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onDelete != null) {
            IconButton(onClick = onDelete) {
              Icon(Icons.Filled.Delete, contentDescription = "Delete custom module", tint = MaterialTheme.colorScheme.error)
            }
          }
          IconButton(
            onClick = onToggleEquip,
            modifier = Modifier.testTag("equip_btn_${item.id}"),
          ) {
            Icon(
              imageVector = if (item.equipped) Icons.Filled.CheckCircle else AppIcons.RadioButtonUnchecked,
              contentDescription = if (item.equipped) "Equipped" else "Equip module",
              tint = if (item.equipped) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Text(
        text = item.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Tier ${item.tier}/${item.maxTier} • Power ${item.powerValue}" +
              if (item.cooldownSec > 0) " • CD ${item.cooldownSec}s" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { item.tier.toFloat() / item.maxTier.toFloat().coerceAtLeast(1f) },
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(6.dp),
          )
        }

        OutlinedButton(
          onClick = onUpgrade,
          enabled = canUpgrade,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("upgrade_btn_${item.id}"),
        ) {
          Icon(AppIcons.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(if (item.tier >= item.maxTier) "MAX" else "$upgradeCost")
        }
      }
    }
  }
}

@Composable
private fun ForgeArmoryItemDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, String, Long, Long, String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("power") }
  var universeTag by remember { mutableStateOf("UnknownOS Custom") }
  var description by remember { mutableStateOf("Custom forged multiverse module.") }
  var powerStr by remember { mutableStateOf("200") }
  var cooldownStr by remember { mutableStateOf("6") }

  val categoryOptions = listOf("perk", "power", "upgrade", "consumable")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Forge Custom Armory Module") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it.take(50) },
          label = { Text("Module Name") },
          placeholder = { Text("e.g. Rift Nova Surge") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("custom_module_name_input"),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(categoryOptions) { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat.replaceFirstChar { it.uppercase() }) },
            )
          }
        }
        OutlinedTextField(
          value = universeTag,
          onValueChange = { universeTag = it.take(40) },
          label = { Text("Universe Tag") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it.take(240) },
          label = { Text("Effect Description") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = powerStr,
            onValueChange = { powerStr = it.filter { c -> c.isDigit() }.take(3) },
            label = { Text("Power (1-500)") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = cooldownStr,
            onValueChange = { cooldownStr = it.filter { c -> c.isDigit() }.take(3) },
            label = { Text("Cooldown (s)") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.trim().isNotEmpty()) {
            val hex = when (category) {
              "perk" -> "#00E676"
              "power" -> "#7C4DFF"
              "upgrade" -> "#FFAB40"
              else -> "#00E5FF"
            }
            onConfirm(
              name.trim(),
              category,
              universeTag.trim(),
              description.trim(),
              cooldownStr.toLongOrNull() ?: 6L,
              powerStr.toLongOrNull() ?: 200L,
              hex,
            )
          }
        },
        modifier = Modifier.testTag("confirm_forge_module_btn"),
      ) { Text("Forge") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

fun parseHexSafe(hex: String, fallback: Color): Color {
  return try {
    Color(hex.toColorInt())
  } catch (_: Exception) {
    fallback
  }
}

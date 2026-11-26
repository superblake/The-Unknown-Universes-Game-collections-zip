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
import com.example.data.model.EquipmentSlotType
import com.example.data.model.GearSlot
import com.example.data.model.UserCustomEquipmentSpec
import com.example.data.model.UserCustomGearSpec
import com.example.data.model.UserCustomPerkSpec
import kotlin.math.roundToInt

private val forgeSwatches = listOf(
  0xFF00E676L,
  0xFF00E5FFL,
  0xFFFFD54FL,
  0xFFFF3D71L,
  0xFFE040FBL,
  0xFFFF8F00L,
)

@Composable
fun ForgeCustomPerkDialog(
  onDismiss: () -> Unit,
  onConfirm: (UserCustomPerkSpec) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var shieldPerRank by remember { mutableIntStateOf(30) }
  var dmgPctPerRank by remember { mutableIntStateOf(8) }
  var ratePctPerRank by remember { mutableIntStateOf(8) }
  var hastePctPerRank by remember { mutableIntStateOf(10) }
  var accentHex by remember { mutableLongStateOf(0xFF00E676L) }

  val presets = listOf(
    "Conduit Fizz" to Triple(30, 10, 12),
    "Gym Grit Tonic" to Triple(55, 8, 6),
    "Freeflow Brew" to Triple(25, 12, 10),
    "Deadbolt Surge" to Triple(40, 14, 14),
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    com.example.util.SyncDialogFullscreen()
    Box(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp).testTag("forge_custom_perk_dialog"),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("FORGE CUSTOM PERK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(accentHex))
          Text("1-tap preset or tune stats — instantly equipped & saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

          Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 390.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              presets.forEach { (presetName, stats) ->
                FilterChip(
                  selected = name == presetName,
                  onClick = {
                    name = presetName
                    shieldPerRank = stats.first
                    dmgPctPerRank = stats.second
                    ratePctPerRank = stats.third
                  },
                  label = { Text(presetName, style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.heightIn(min = 48.dp),
                )
              }
            }
            OutlinedTextField(
              value = name,
              onValueChange = { name = it.take(40) },
              label = { Text("Perk Name (Optional)") },
              placeholder = { Text("e.g. Conduit Fizz") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("forge_perk_name_input"),
            )
            OutlinedTextField(
              value = description,
              onValueChange = { description = it.take(120) },
              label = { Text("Effect Notes (Optional)") },
              placeholder = { Text("Boosts shield, weapon damage, cadence & regen") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            Text("Shield Bonus: +$shieldPerRank • DMG: +$dmgPctPerRank% • Fire Rate: +$ratePctPerRank%", style = MaterialTheme.typography.labelSmall)
            Slider(
              value = shieldPerRank.toFloat(),
              onValueChange = { shieldPerRank = it.roundToInt().coerceIn(10, 80) },
              valueRange = 10f..80f,
              colors = SliderDefaults.colors(thumbColor = Color(accentHex), activeTrackColor = Color(accentHex)),
            )
            Slider(
              value = dmgPctPerRank.toFloat(),
              onValueChange = { dmgPctPerRank = it.roundToInt().coerceIn(2, 20) },
              valueRange = 2f..20f,
              colors = SliderDefaults.colors(thumbColor = Color(accentHex), activeTrackColor = Color(accentHex)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              forgeSwatches.forEach { hex ->
                Box(
                  modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(hex).copy(alpha = 0.25f))
                    .border(if (accentHex == hex) 2.5.dp else 1.dp, if (accentHex == hex) Color.White else Color(hex), CircleShape)
                    .clickable { accentHex = hex }
                    .padding(10.dp),
                ) {
                  Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(hex)))
                }
              }
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
            Spacer(Modifier.weight(1f))
            Button(
              onClick = {
                val safeName = name.trim().ifBlank { "Custom Surge Drink" }
                val id = "cperk_${safeName.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(16)}_${java.util.UUID.randomUUID().toString().take(8)}"
                onConfirm(
                  UserCustomPerkSpec(
                    id = id,
                    name = safeName,
                    description = description.trim().ifBlank { "+$shieldPerRank SHD • +$dmgPctPerRank% DMG • +$ratePctPerRank% Rate" },
                    accentHex = accentHex,
                    shieldBonusPerRank = shieldPerRank,
                    damagePctPerRank = dmgPctPerRank,
                    fireRatePctPerRank = ratePctPerRank,
                    cooldownHastePctPerRank = hastePctPerRank,
                  )
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(accentHex), contentColor = Color.Black),
              modifier = Modifier.heightIn(min = 48.dp).testTag("forge_perk_confirm_btn"),
            ) {
              Text("Forge & Equip", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
fun ForgeCustomGearDialog(
  onDismiss: () -> Unit,
  onConfirm: (UserCustomGearSpec) -> Unit,
) {
  var slot by remember { mutableStateOf(GearSlot.CHEST) }
  var name by remember { mutableStateOf("") }
  var elementAffinityId by remember { mutableStateOf("kinetic") }
  var bonusShield by remember { mutableIntStateOf(40) }
  var bonusDmgPct by remember { mutableIntStateOf(10) }
  var accentHex by remember { mutableLongStateOf(0xFF00E5FFL) }

  val presets = listOf(
    "Real-Life Denim Jacket" to GearSlot.CHEST,
    "Everyday Glasses HUD" to GearSlot.HELMET,
    "Gym Lifting Gloves" to GearSlot.GLOVES,
    "Parkour Trainers" to GearSlot.BOOTS,
    "Sketchbook & Laptop Pack" to GearSlot.BACK_RIG,
  )
  val elements = listOf("kinetic", "energy", "thermal", "cryo", "bio", "rock", "sand", "void")

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    com.example.util.SyncDialogFullscreen()
    Box(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp).testTag("forge_custom_gear_dialog"),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("FORGE CUSTOM GEAR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(accentHex))
          Text("Pick a slot or 1-tap Real-Life preset — auto-equips immediately", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

          Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 390.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              presets.forEach { (pName, pSlot) ->
                FilterChip(
                  selected = name == pName,
                  onClick = {
                    name = pName
                    slot = pSlot
                  },
                  label = { Text(pName, style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.heightIn(min = 48.dp),
                )
              }
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              GearSlot.entries.forEach { s ->
                FilterChip(
                  selected = slot == s,
                  onClick = { slot = s },
                  label = { Text(s.displayName, style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.heightIn(min = 48.dp),
                )
              }
            }
            OutlinedTextField(
              value = name,
              onValueChange = { name = it.take(40) },
              label = { Text("Gear Piece Name (Optional)") },
              placeholder = { Text("e.g. Forged ${slot.displayName}") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("forge_gear_name_input"),
            )
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              elements.forEach { elem ->
                FilterChip(
                  selected = elementAffinityId == elem,
                  onClick = { elementAffinityId = elem },
                  label = { Text(elem.uppercase(), style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.heightIn(min = 48.dp),
                )
              }
            }
            Text("Shield: +$bonusShield • Damage: +$bonusDmgPct%", style = MaterialTheme.typography.labelSmall)
            Slider(
              value = bonusShield.toFloat(),
              onValueChange = { bonusShield = it.roundToInt().coerceIn(15, 100) },
              valueRange = 15f..100f,
              colors = SliderDefaults.colors(thumbColor = Color(accentHex), activeTrackColor = Color(accentHex)),
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
            Spacer(Modifier.weight(1f))
            Button(
              onClick = {
                val safeName = name.trim().ifBlank { "Forged ${slot.displayName}" }
                val id = "cgear_${slot.id}_${java.util.UUID.randomUUID().toString().take(8)}"
                onConfirm(
                  UserCustomGearSpec(
                    id = id,
                    slot = slot,
                    name = safeName,
                    description = "${elementAffinityId.uppercase()} Forged ${slot.displayName} (+$bonusShield SHD, +$bonusDmgPct% DMG)",
                    elementAffinityId = elementAffinityId,
                    accentHex = accentHex,
                    bonusShield = bonusShield,
                    bonusDamagePct = bonusDmgPct,
                  )
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(accentHex), contentColor = Color.Black),
              modifier = Modifier.heightIn(min = 48.dp).testTag("forge_gear_confirm_btn"),
            ) {
              Text("Forge & Equip", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
fun ForgeCustomEquipmentDialog(
  onDismiss: () -> Unit,
  onConfirm: (UserCustomEquipmentSpec) -> Unit,
) {
  var slotType by remember { mutableStateOf(EquipmentSlotType.LETHAL) }
  var name by remember { mutableStateOf("") }
  var damageTypeId by remember { mutableStateOf("explosive") }
  var basePower by remember { mutableFloatStateOf(220f) }
  var cooldownSec by remember { mutableFloatStateOf(5.5f) }
  var accentHex by remember { mutableLongStateOf(0xFFFF6D00L) }

  val presets = listOf(
    Triple("Arkham Shock Batarang", EquipmentSlotType.LETHAL, "energy"),
    Triple("Conduit Arc Charge", EquipmentSlotType.LETHAL, "explosive"),
    Triple("ISAC Repair Drone", EquipmentSlotType.TACTICAL, "energy"),
    Triple("Real-Life Pocket Flash", EquipmentSlotType.TACTICAL, "cryo"),
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    com.example.util.SyncDialogFullscreen()
    Box(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp).testTag("forge_custom_equipment_dialog"),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("FORGE CUSTOM EQUIPMENT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(accentHex))
          Text("Create custom Lethal or Tactical gear — instantly equipped", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

          Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 390.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              presets.forEach { (pName, pSlot, pElem) ->
                FilterChip(
                  selected = name == pName,
                  onClick = {
                    name = pName
                    slotType = pSlot
                    damageTypeId = pElem
                  },
                  label = { Text(pName, style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.heightIn(min = 48.dp),
                )
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              EquipmentSlotType.entries.forEach { st ->
                FilterChip(
                  selected = slotType == st,
                  onClick = { slotType = st },
                  label = { Text(st.displayName) },
                  modifier = Modifier.heightIn(min = 48.dp),
                )
              }
            }
            OutlinedTextField(
              value = name,
              onValueChange = { name = it.take(40) },
              label = { Text("Equipment Name (Optional)") },
              placeholder = { Text("e.g. Custom ${slotType.displayName}") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("forge_eq_name_input"),
            )
            Text("Power: ${basePower.roundToInt()} • Cooldown: ${String.format(java.util.Locale.US, "%.1fs", cooldownSec)}", style = MaterialTheme.typography.labelSmall)
            Slider(
              value = basePower,
              onValueChange = { basePower = it.coerceIn(60f, 500f) },
              valueRange = 60f..500f,
              colors = SliderDefaults.colors(thumbColor = Color(accentHex), activeTrackColor = Color(accentHex)),
            )
            Slider(
              value = cooldownSec,
              onValueChange = { cooldownSec = it.coerceIn(2.0f, 15.0f) },
              valueRange = 2.0f..15.0f,
              colors = SliderDefaults.colors(thumbColor = Color(accentHex), activeTrackColor = Color(accentHex)),
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
            Spacer(Modifier.weight(1f))
            Button(
              onClick = {
                val safeName = name.trim().ifBlank { "Forged ${slotType.displayName}" }
                val id = "ceq_${slotType.id}_${java.util.UUID.randomUUID().toString().take(8)}"
                onConfirm(
                  UserCustomEquipmentSpec(
                    id = id,
                    slotType = slotType,
                    name = safeName,
                    description = "${damageTypeId.uppercase()} Custom ${slotType.displayName} (${basePower.roundToInt()} PWR)",
                    damageTypeId = damageTypeId,
                    baseDamageOrEffect = basePower,
                    cooldownSeconds = cooldownSec,
                    accentHex = accentHex,
                  )
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(accentHex), contentColor = Color.Black),
              modifier = Modifier.heightIn(min = 48.dp).testTag("forge_eq_confirm_btn"),
            ) {
              Text("Forge & Equip", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

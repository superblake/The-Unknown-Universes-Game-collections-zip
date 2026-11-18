package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CharacterCustomization
import com.example.data.model.GearSlot
import com.example.data.model.GearState
import com.example.data.model.LevelCustomization
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.SkillNode
import com.example.data.model.UniverseCatalog
import com.example.data.model.WeaponCustomization
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.example.ui.rig.RigEvaluator
import com.example.ui.rig.drawRigCharacter
import java.util.Locale

private val tracerSwatches: List<Pair<String, Long>> = listOf(
  "Cyan" to 0xFF00E5FF,
  "Gold" to 0xFFFFD54F,
  "Crimson" to 0xFFFF3D71,
  "Emerald" to 0xFF00E676,
  "Violet" to 0xFFE040FB,
)

private val titleBadges: List<String> = listOf(
  "Multiverse Operative",
  "SHD Containment Lead",
  "Blacklight Conduit",
  "Gatewalker Prime",
  "Hellbreaker Slayer",
)

@Composable
fun WeaponCustomizationTab(
  progression: OperatorProgressionState,
  onUpdateWeaponCustom: ((WeaponCustomization) -> WeaponCustomization) -> Unit,
) {
  val wc = progression.weaponCustom
  val equippedWeapon = ProgressionCatalog.weaponById(wc.equippedWeaponId)
  val wpnXp = progression.weaponXp(equippedWeapon.id)
  val wpnLevel = progression.weaponLevel(equippedWeapon.id)
  val (wpnCur, wpnNeed) = ProgressionCatalog.xpProgressInCurrentWeaponLevel(wpnXp)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, Color(wc.tracerColorHex).copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(equippedWeapon.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
              Text("${equippedWeapon.universeOrigin} • ${equippedWeapon.summary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(wc.tracerColorHex).copy(alpha = 0.2f),
            ) {
              Text(
                text = "W.LV $wpnLevel/30",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(wc.tracerColorHex),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("weapon_level_badge"),
              )
            }
          }
          LinearProgressIndicator(
            progress = { (wpnCur.toFloat() / wpnNeed.coerceAtLeast(1L)).coerceIn(0f, 1f) },
            color = Color(wc.tracerColorHex),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
          )
          Text(
            text = "Weapon XP: $wpnCur / $wpnNeed to W.Lv.${(wpnLevel + 1).coerceAtMost(30)} • +${((wpnLevel - 1) / 5)} Level Damage Bonus",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    item {
      Text("PRIMARY WEAPON PLATFORM", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(ProgressionCatalog.weapons, key = { it.id }) { weapon ->
          val selected = weapon.id == wc.equippedWeaponId
          val lvl = progression.weaponLevel(weapon.id)
          Surface(
            onClick = {
              onUpdateWeaponCustom { it.copy(equippedWeaponId = weapon.id, tracerColorHex = weapon.defaultTracerHex) }
            },
            shape = RoundedCornerShape(14.dp),
            color = if (selected) Color(weapon.defaultTracerHex).copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .width(210.dp)
              .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Color(weapon.defaultTracerHex) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
              )
              .testTag("select_weapon_${weapon.id}"),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(weapon.universeOrigin, style = MaterialTheme.typography.labelSmall, color = Color(weapon.defaultTracerHex))
                Text("Lv.$lvl", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
              Text(weapon.name, style = MaterialTheme.typography.titleMedium)
              Text(
                text = "DMG ${weapon.baseDamage} • Spread ${weapon.baseSpreadCount} • Pierce ${weapon.basePierce}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    item {
      CustomizationChipGroup(
        title = "CAMO / FINISH",
        options = ProgressionCatalog.camos,
        selectedId = wc.camoId,
        tagPrefix = "camo",
        onSelect = { id -> onUpdateWeaponCustom { it.copy(camoId = id) } },
      )
    }

    item {
      CustomizationChipGroup(
        title = "OPTIC / RETICLE (LIVE IN ARENA)",
        options = ProgressionCatalog.optics,
        selectedId = wc.opticId,
        tagPrefix = "optic",
        onSelect = { id -> onUpdateWeaponCustom { it.copy(opticId = id) } },
      )
    }

    item {
      CustomizationChipGroup(
        title = "BARREL TUNING",
        options = ProgressionCatalog.barrels,
        selectedId = wc.barrelId,
        tagPrefix = "barrel",
        onSelect = { id -> onUpdateWeaponCustom { it.copy(barrelId = id) } },
      )
    }

    item {
      CustomizationChipGroup(
        title = "AMMO MOD CORE",
        options = ProgressionCatalog.ammoMods,
        selectedId = wc.ammoModId,
        tagPrefix = "ammo",
        onSelect = { id -> onUpdateWeaponCustom { it.copy(ammoModId = id) } },
      )
    }

    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("TRACER BOLT COLOR", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            tracerSwatches.forEach { (label, hex) ->
              val selected = wc.tracerColorHex == hex
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onUpdateWeaponCustom { it.copy(tracerColorHex = hex) } }
                    .testTag("tracer_swatch_$label"),
                )
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun CharacterCustomizationTab(
  progression: OperatorProgressionState,
  activeRig: RigAsset?,
  onUpdateCharacterCustom: ((CharacterCustomization) -> CharacterCustomization) -> Unit,
) {
  val cc = progression.characterCustom
  val (xpCur, xpNeed) = ProgressionCatalog.xpProgressInCurrentPlayerLevel(progression.playerXp)
  val visorHex = ProgressionCatalog.visorStyles.firstOrNull { it.first == cc.visorStyleId }?.second ?: cc.accentArmorHex
  val capePath = remember { Path() }
  val torsoPath = remember { Path() }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(cc.primaryArmorHex).copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(width = 112.dp, height = 140.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF0A101E))
              .border(1.dp, Color(cc.primaryArmorHex).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val skel = RigEvaluator.evaluate(
                origin = Offset(size.width * 0.5f, size.height * 0.56f),
                scalePx = size.height * 0.52f,
                poseAngles = activeRig?.poseAngles ?: UniverseCatalog.defaultPoseMap(),
                clip = "idle",
                frame = 12f,
                ikEnabled = true,
              )
              drawRigCharacter(
                skeleton = skel,
                archetype = activeRig?.archetype ?: "operator",
                primaryColor = Color(cc.primaryArmorHex),
                secondaryColor = Color(cc.accentArmorHex),
                showBonesOverlay = false,
                capePathScratch = capePath,
                torsoPathScratch = torsoPath,
                cloakOverride = cc.cloakEnabled,
                visorColorOverride = Color(visorHex),
                auraTrailId = cc.auraTrailId,
              )
            }
          }

          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("LEVEL ${progression.playerLevel} • ${progression.playerRankTitle}", style = MaterialTheme.typography.titleMedium, color = Color(cc.primaryArmorHex), modifier = Modifier.testTag("player_level_rank_text"))
            Text("Title Badge: ${cc.titleBadge}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
              progress = { (xpCur.toFloat() / xpNeed.coerceAtLeast(1L)).coerceIn(0f, 1f) },
              color = Color(cc.primaryArmorHex),
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            )
            Text(
              text = "XP: $xpCur / $xpNeed • Passive Shield: +${((progression.playerLevel - 1) * 3).coerceAtMost(90)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    item {
      Text("ARMOR SUIT THEME", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ProgressionCatalog.suitThemes, key = { it.first }) { (id, label, colors) ->
          val selected = cc.suitThemeId == id
          FilterChip(
            selected = selected,
            onClick = {
              onUpdateCharacterCustom {
                it.copy(suitThemeId = id, primaryArmorHex = colors.first, accentArmorHex = colors.second)
              }
            },
            label = { Text(label) },
            modifier = Modifier.testTag("suit_chip_$id"),
          )
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("HELMET VISOR COLOR", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressionCatalog.visorStyles.forEach { (id, hex) ->
              val selected = cc.visorStyleId == id
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(Color(hex))
                  .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else MaterialTheme.colorScheme.outline, CircleShape)
                  .clickable { onUpdateCharacterCustom { it.copy(visorStyleId = id) } }
                  .testTag("visor_swatch_$id"),
              )
            }
          }
        }
      }
    }

    item {
      CustomizationChipGroup(
        title = "AURA TRAIL EFFECT",
        options = ProgressionCatalog.auraTrails,
        selectedId = cc.auraTrailId,
        tagPrefix = "aura",
        onSelect = { id -> onUpdateCharacterCustom { it.copy(auraTrailId = id) } },
      )
    }

    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Combat Cloak / Trenchcoat", style = MaterialTheme.typography.titleSmall)
              Text("Render flowing tactical cape on Arena & Rig viewports", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = cc.cloakEnabled,
              onCheckedChange = { enabled -> onUpdateCharacterCustom { it.copy(cloakEnabled = enabled) } },
              modifier = Modifier.testTag("character_cloak_switch"),
            )
          }
          Text("OPERATOR TITLE BADGE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(titleBadges) { badge ->
              FilterChip(
                selected = cc.titleBadge == badge,
                onClick = { onUpdateCharacterCustom { it.copy(titleBadge = badge) } },
                label = { Text(badge) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun LevelCustomizationTab(
  progression: OperatorProgressionState,
  onUpdateLevelCustom: ((LevelCustomization) -> LevelCustomization) -> Unit,
) {
  val lc = progression.levelCustom
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      CustomizationChipGroup(
        title = "ARENA SKY & ATMOSPHERE THEME",
        options = ProgressionCatalog.skyThemes,
        selectedId = lc.skyThemeId,
        tagPrefix = "sky",
        onSelect = { id -> onUpdateLevelCustom { it.copy(skyThemeId = id) } },
      )
    }

    item {
      CustomizationChipGroup(
        title = "TACTICAL FLOOR GRID STYLE",
        options = ProgressionCatalog.gridStyles,
        selectedId = lc.gridStyleId,
        tagPrefix = "grid",
        onSelect = { id -> onUpdateLevelCustom { it.copy(gridStyleId = id) } },
      )
    }

    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Enemy Wave Density: ${String.format(Locale.US, "%.2fx", lc.enemyDensityMultiplier)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Slider(
            value = lc.enemyDensityMultiplier,
            onValueChange = { v -> onUpdateLevelCustom { it.copy(enemyDensityMultiplier = v.coerceIn(0.75f, 1.5f)) } },
            valueRange = 0.75f..1.5f,
            modifier = Modifier.testTag("level_density_slider"),
          )
          Text("FLOOR GRID ACCENT COLOR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            tracerSwatches.forEach { (label, hex) ->
              val selected = lc.floorAccentHex == hex
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(Color(hex))
                  .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else MaterialTheme.colorScheme.outline, CircleShape)
                  .clickable { onUpdateLevelCustom { it.copy(floorAccentHex = hex) } }
                  .testTag("floor_accent_$label"),
              )
            }
          }
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("CUSTOM ARENA MUTATORS", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          MutatorToggleRow(
            title = "Mystery Wonder Boost",
            subtitle = "Doubles random Wonder Weapon pickup drop chances",
            checked = lc.wonderDropBoost,
            tag = "mutator_wonder_boost",
            onCheckedChange = { c -> onUpdateLevelCustom { it.copy(wonderDropBoost = c) } },
          )
          MutatorToggleRow(
            title = "Crit Surge Mode",
            subtitle = "Every 3rd volley deals 2x Critical Gold Damage",
            checked = lc.critSurgeMutator,
            tag = "mutator_crit_surge",
            onCheckedChange = { c -> onUpdateLevelCustom { it.copy(critSurgeMutator = c) } },
          )
          MutatorToggleRow(
            title = "Low-Gravity Dash",
            subtitle = "-40% Dash cooldown & extended dash range",
            checked = lc.lowGravityDash,
            tag = "mutator_low_grav",
            onCheckedChange = { c -> onUpdateLevelCustom { it.copy(lowGravityDash = c) } },
          )
          MutatorToggleRow(
            title = "Instagib Arcade",
            subtitle = "Standard hostiles drop in 1 hit & 2x bolt damage",
            checked = lc.instagibMutator,
            tag = "mutator_instagib",
            onCheckedChange = { c -> onUpdateLevelCustom { it.copy(instagibMutator = c) } },
          )
          MutatorToggleRow(
            title = "Boss Rush Wave Protocol",
            subtitle = "Spawns a Lore Boss every single Arena wave",
            checked = lc.bossRushMutator,
            tag = "mutator_boss_rush",
            onCheckedChange = { c -> onUpdateLevelCustom { it.copy(bossRushMutator = c) } },
          )
        }
      }
    }
  }
}

@Composable
fun EquipmentCustomizationTab(
  progression: OperatorProgressionState,
  onUpgradeEquipment: (String) -> Unit,
  onPrestigeEquipment: (String) -> Unit,
  onEquipEquipment: (String, String) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text("TACTICAL & LETHAL EQUIPMENT LOADOUT", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
      Text("Equip, upgrade to level 60, and prestige up to P30 for tactical combat dominance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    items(com.example.data.model.EquipmentSlotType.entries, key = { it.id }) { slotType ->
      val eqState = progression.equipmentState(slotType.id)
      val itemsForSlot = ProgressionCatalog.equipmentItems.filter { it.slotType == slotType }
      val equippedItem = itemsForSlot.firstOrNull { it.id == eqState.equippedItemId } ?: itemsForSlot.first()

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(equippedItem.defaultColorHex).copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(slotType.displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
              Text(equippedItem.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(equippedItem.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(equippedItem.defaultColorHex).copy(alpha = 0.2f),
            ) {
              Text(
                text = "LVL ${eqState.level}/60 ${if (eqState.prestige > 0) "• P${eqState.prestige}" else ""}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(equippedItem.defaultColorHex),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("eq_level_${slotType.id}"),
              )
            }
          }

          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(itemsForSlot, key = { it.id }) { item ->
              val isSelected = item.id == eqState.equippedItemId
              FilterChip(
                selected = isSelected,
                onClick = { onEquipEquipment(slotType.id, item.id) },
                label = { Text(item.name) },
                modifier = Modifier.testTag("equip_item_${item.id}"),
              )
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
              onClick = { onUpgradeEquipment(slotType.id) },
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.weight(1f).testTag("upgrade_eq_${slotType.id}"),
            ) {
              Text(
                text = "Upgrade Level (${eqState.level}/60)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              )
            }
            if (eqState.level >= 60 && eqState.prestige < 30) {
              Surface(
                onClick = { onPrestigeEquipment(slotType.id) },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f).testTag("prestige_eq_${slotType.id}"),
              ) {
                Text(
                  text = "Prestige (${eqState.prestige}/30)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                  modifier = Modifier.padding(vertical = 10.dp),
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun GearCustomizationTab(
  progression: OperatorProgressionState,
  onUpgradeGear: (GearSlot) -> Unit,
  onPrestigeGear: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("OPERATOR GEAR SUITE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          Text(
            text = "Upgrade 5 gear modules up to Level 50 & Prestige up to P30.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
        ) {
          Text(
            text = "GEAR PRESTIGE: P${progression.gearPrestige}/30",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).testTag("gear_prestige_badge"),
          )
        }
      }
    }

    if (progression.gearPrestige < 30) {
      item {
        val allMaxed = GearSlot.entries.all { (progression.gearMap[it.id]?.level ?: 1) >= 50 }
        Surface(
          onClick = { if (allMaxed) onPrestigeGear() },
          shape = RoundedCornerShape(12.dp),
          color = if (allMaxed) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth().testTag("gear_prestige_action_btn"),
        ) {
          Text(
            text = if (allMaxed) "PRESTIGE ALL GEAR (Ascend to P${progression.gearPrestige + 1})" else "Reach Level 50 on all 5 Gear Slots to Prestige",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (allMaxed) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          )
        }
      }
    }

    items(GearSlot.entries, key = { it.id }) { slot ->
      val gear = progression.gearMap[slot.id] ?: GearState(slot)
      val slotDesc = when (slot) {
        GearSlot.HELMET -> "+Target HUD tracking & +${gear.level * 2}% Precision headshot critical damage"
        GearSlot.CHEST -> "+${gear.level * 3} Max Shield armor & +${gear.level / 10} Armor siphon"
        GearSlot.GLOVES -> "+${(gear.level * 0.4f)}% Volley fire rate & +${gear.level / 8} Melee claw strike"
        GearSlot.BOOTS -> "+${(gear.level * 0.35f)}% Sprint speed & -${(gear.level * 0.4f)}% Dash recovery cooldown"
        GearSlot.BACK_RIG -> "+Jump apex hangtime & +${(gear.level * 0.5f)}s Wonder Weapon duration"
      }

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(gear.colorHex).copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(slot.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(slotDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(gear.colorHex).copy(alpha = 0.2f),
            ) {
              Text(
                text = "LVL ${gear.level}/50 ${if (gear.prestige > 0) "• P${gear.prestige}" else ""}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(gear.colorHex),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("gear_level_${slot.id}"),
              )
            }
          }

          LinearProgressIndicator(
            progress = { (gear.level.toFloat() / 50f).coerceIn(0f, 1f) },
            color = Color(gear.colorHex),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
          )

          Surface(
            onClick = { onUpgradeGear(slot) },
            enabled = gear.level < 50,
            shape = RoundedCornerShape(10.dp),
            color = if (gear.level < 50) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().testTag("upgrade_gear_${slot.id}"),
          ) {
            Text(
              text = if (gear.level < 50) "Upgrade to Level ${gear.level + 1}" else "MAX LEVEL (50/50)",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (gear.level < 50) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 10.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
          }
        }
      }
    }
  }
}

@Composable
fun SkillTreeTab(
  progression: OperatorProgressionState,
  totalEssence: Long,
  onUnlockSkill: (String) -> Unit,
) {
  var selectedCategory by remember { mutableStateOf("ALL") }
  val categories = listOf("ALL", "COMBAT", "SURVIVAL", "ENGINEERING", "WONDER")
  val nodes = progression.skillNodes.values.toList()
  val filtered = if (selectedCategory == "ALL") nodes else nodes.filter { it.category == selectedCategory }
  val unlockedCount = nodes.count { it.unlocked }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("OPERATOR SKILL TREE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          Text(
            text = "Unlock permanent combat, survival, and engineering traits with Essence.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        ) {
          Text(
            text = "$unlockedCount / ${nodes.size} UNLOCKED",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).testTag("skills_unlocked_badge"),
          )
        }
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories) { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            label = { Text(cat) },
            modifier = Modifier.testTag("skill_cat_$cat"),
          )
        }
      }
    }

    items(filtered, key = { it.id }) { node ->
      val accent = when (node.category) {
        "COMBAT" -> Color(0xFFFF5252)
        "SURVIVAL" -> Color(0xFF00E5FF)
        "ENGINEERING" -> Color(0xFFFFD54F)
        else -> Color(0xFFE040FB)
      }
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = if (node.unlocked) 1.5.dp else 1.dp,
            color = if (node.unlocked) accent else MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(14.dp),
          ),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(node.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(node.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (node.unlocked) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Text(
                text = if (node.unlocked) "ACTIVE" else "${node.costEssence} ESS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (node.unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }

          if (!node.unlocked) {
            val canAfford = totalEssence >= node.costEssence
            Surface(
              onClick = { onUnlockSkill(node.id) },
              enabled = canAfford,
              shape = RoundedCornerShape(10.dp),
              color = if (canAfford) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth().testTag("unlock_skill_${node.id}"),
            ) {
              Text(
                text = if (canAfford) "Unlock Skill (${node.costEssence} Essence)" else "Need ${node.costEssence - totalEssence} More Essence",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (canAfford) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CustomizationChipGroup(
  title: String,
  options: List<Pair<String, String>>,
  selectedId: String,
  tagPrefix: String,
  onSelect: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(options, key = { it.first }) { (id, label) ->
        FilterChip(
          selected = selectedId == id,
          onClick = { onSelect(id) },
          label = { Text(label) },
          modifier = Modifier.testTag("${tagPrefix}_$id"),
        )
      }
    }
  }
}

@Composable
private fun MutatorToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  tag: String,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
      Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(tag),
    )
  }
}

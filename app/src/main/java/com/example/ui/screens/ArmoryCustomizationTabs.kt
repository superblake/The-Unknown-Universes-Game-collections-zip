package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CharacterCustomization
import com.example.data.model.CollectionDifficulty
import com.example.data.model.GearSlot
import com.example.data.model.GearState
import com.example.data.model.LevelCustomization
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.NewGamePlusVersion
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.StarRankMeta
import com.example.data.model.UniverseCatalog
import com.example.data.model.UpgradeCosts
import com.example.data.model.WeaponCustomization
import com.example.data.model.WonderWeaponKind
import com.example.data.model.WorldDifficultyTier
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeaponCustomizationTab(
  progression: OperatorProgressionState,
  onUpdateWeaponCustom: ((WeaponCustomization) -> WeaponCustomization) -> Unit,
  onUpdateWonderCustom: ((com.example.data.model.WonderWeaponCustomization) -> com.example.data.model.WonderWeaponCustomization) -> Unit = {},
  totalBlueEssence: Long = 0L,
  onUpgradeWeaponTier: (String) -> Unit = {},
  onUpgradeWeaponStar: (String) -> Unit = {},
  onUpgradeWonderTier: (String) -> Unit = {},
  onUpgradeWonderStar: (String) -> Unit = {},
  onPrestigeWeapon: (String) -> Unit = {},
  onToggleMultiWeaponEquip: (String) -> Unit = {},
  onToggleMultiWonderEquip: (String) -> Unit = {},
  onOpenForgeWeapon: () -> Unit = {},
  onDeleteCustomWeapon: (String) -> Unit = {},
) {
  val haptic = LocalHapticFeedback.current
  val wc = progression.weaponCustom
  val multiWeapons = progression.effectiveWeaponList()
  val multiWonders = progression.effectiveWonderList()
  val allWeapons = remember(progression.customWeapons) { ProgressionCatalog.allWeapons(progression.customWeapons) }
  val equippedWeapon = ProgressionCatalog.weaponById(wc.equippedWeaponId, progression.customWeapons)
  val wpnXp = progression.weaponXp(equippedWeapon.id)
  val wpnLevel = progression.weaponLevel(equippedWeapon.id)
  val wpnPrestige = progression.weaponPrestige(equippedWeapon.id)
  val wpnTier = progression.weaponTier(equippedWeapon.id)
  val wpnTierMeta = ProgressionCatalog.tierMeta(wpnTier)
  val wpnStar = progression.weaponStar(equippedWeapon.id)
  val wpnStarMeta = ProgressionCatalog.starMeta(wpnStar)
  val tierCost = UpgradeCosts.weaponTier(wpnTier)
  val wpnStarCost = UpgradeCosts.weaponStar(wpnStar)
  val (wpnCur, wpnNeed) = ProgressionCatalog.xpProgressInCurrentWeaponLevel(wpnXp)

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("weapon_customization_list"),
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
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(wc.tracerColorHex).copy(alpha = 0.2f),
              ) {
                Text(
                  text = "W.LV $wpnLevel/60 • P$wpnPrestige/30",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = Color(wc.tracerColorHex),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("weapon_level_badge"),
                )
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(wpnTierMeta.colorHex).copy(alpha = 0.18f),
              ) {
                Text(
                  text = "T$wpnTier/25 • ${wpnStar}★/10★ (${wpnStarMeta.label})",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(wpnTierMeta.colorHex),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp).testTag("weapon_tier_badge"),
                )
              }
            }
          }
          LinearProgressIndicator(
            progress = { (wpnCur.toFloat() / wpnNeed.coerceAtLeast(1L)).coerceIn(0f, 1f) },
            color = Color(wc.tracerColorHex),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
          )
          Text(
            text = "Weapon XP: $wpnCur / $wpnNeed to W.Lv.${(wpnLevel + 1).coerceAtMost(60)} • Tier x${String.format(Locale.US, "%.2f", wpnTierMeta.multiplier)} • Star x${String.format(Locale.US, "%.2f", wpnStarMeta.multiplier)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { onUpgradeWeaponTier(equippedWeapon.id) },
              enabled = wpnTier < 25 && totalBlueEssence >= tierCost,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_weapon_tier_btn"),
            ) {
              Text(
                text = if (wpnTier >= 25) "MAX TIER 25" else "Tier +1 ($tierCost B)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
            }
            OutlinedButton(
              onClick = { onUpgradeWeaponStar(equippedWeapon.id) },
              enabled = wpnStar < StarRankMeta.MAX_STARS && totalBlueEssence >= wpnStarCost,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_weapon_star_btn"),
            ) {
              Text(
                text = if (wpnStar >= StarRankMeta.MAX_STARS) "10★ MAX" else "Star +1 ($wpnStarCost B)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
            }
            if (wpnLevel >= 60 && wpnPrestige < 30) {
              OutlinedButton(
                onClick = { onPrestigeWeapon(equippedWeapon.id) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp).testTag("prestige_weapon_btn"),
              ) {
                Text("Prestige (+1000 B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "PRIMARY WEAPONS (${multiWeapons.size}/3 EQUIPPED • ${allWeapons.size} TOTAL)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Loadout: ${multiWeapons.joinToString(" • ") { ProgressionCatalog.weaponById(it, progression.customWeapons).name }}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF00E5FF),
            modifier = Modifier.testTag("multi_weapon_loadout_summary"),
          )
        }
        Button(
          onClick = onOpenForgeWeapon,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.heightIn(min = 48.dp).testTag("open_forge_custom_weapon_btn"),
        ) {
          Text("+ Forge Weapon", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
      }
      Spacer(Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(allWeapons, key = { it.id }) { weapon ->
          val selected = weapon.id == wc.equippedWeaponId
          val inMulti = multiWeapons.contains(weapon.id)
          val lvl = progression.weaponLevel(weapon.id)
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (selected) Color(weapon.defaultTracerHex).copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .width(232.dp)
              .clip(RoundedCornerShape(14.dp))
              .combinedClickable(
                onClick = {
                  onUpdateWeaponCustom { it.copy(equippedWeaponId = weapon.id, tracerColorHex = weapon.defaultTracerHex) }
                },
                onLongClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  onUpdateWeaponCustom { it.copy(equippedWeaponId = weapon.id, tracerColorHex = weapon.defaultTracerHex) }
                },
              )
              .border(
                width = if (selected || inMulti) 1.5.dp else 1.dp,
                color = if (selected) Color(weapon.defaultTracerHex) else if (inMulti) Color(0xFF00E676) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
              )
              .testTag("select_weapon_${weapon.id}"),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = if (weapon.isCustomWeapon) "${weapon.universeOrigin} • CUSTOM" else weapon.universeOrigin,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(weapon.defaultTracerHex),
                  modifier = Modifier.weight(1f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text("Lv.$lvl", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                if (weapon.isCustomWeapon) {
                  IconButton(
                    onClick = { onDeleteCustomWeapon(weapon.id) },
                    modifier = Modifier.size(48.dp).testTag("custom_wpn_delete_${weapon.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Delete,
                      contentDescription = "Delete custom weapon",
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                }
              }
              Text(weapon.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(
                text = "${weapon.damageTypeId.uppercase()} • DMG ${weapon.baseDamage} • Spread ${weapon.baseSpreadCount} • Pierce ${weapon.basePierce}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              OutlinedButton(
                onClick = { onToggleMultiWeaponEquip(weapon.id) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("multi_equip_weapon_${weapon.id}"),
              ) {
                Text(
                  text = if (inMulti) "In Loadout (${multiWeapons.size}/3)" else "+ Add to Loadout",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (inMulti) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                )
              }
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
        title = "MUZZLE ATTACHMENT",
        options = ProgressionCatalog.muzzles,
        selectedId = wc.muzzleId,
        tagPrefix = "muzzle",
        onSelect = { id -> onUpdateWeaponCustom { it.copy(muzzleId = id) } },
      )
    }

    item {
      CustomizationChipGroup(
        title = "UNDERBARREL MOUNT",
        options = ProgressionCatalog.underbarrels,
        selectedId = wc.underbarrelId,
        tagPrefix = "underbarrel",
        onSelect = { id -> onUpdateWeaponCustom { it.copy(underbarrelId = id) } },
      )
    }

    item {
      CustomizationChipGroup(
        title = "MAGAZINE CAPACITY & ENERGY CELLS",
        options = ProgressionCatalog.magazines,
        selectedId = wc.magazineId,
        tagPrefix = "mag",
        onSelect = { id -> onUpdateWeaponCustom { it.copy(magazineId = id) } },
      )
    }

    item {
      CustomizationChipGroup(
        title = "STOCK BUFFER & MANEUVER",
        options = ProgressionCatalog.stocks,
        selectedId = wc.stockId,
        tagPrefix = "stock",
        onSelect = { id -> onUpdateWeaponCustom { it.copy(stockId = id) } },
      )
    }

    item {
      CustomizationChipGroup(
        title = "REAR GRIP TAPE & INSULATION",
        options = ProgressionCatalog.rearGrips,
        selectedId = wc.rearGripId,
        tagPrefix = "reargrip",
        onSelect = { id -> onUpdateWeaponCustom { it.copy(rearGripId = id) } },
      )
    }

    item {
      CustomizationChipGroup(
        title = "LASER DESIGNATOR",
        options = ProgressionCatalog.lasers,
        selectedId = wc.laserId,
        tagPrefix = "laser",
        onSelect = { id -> onUpdateWeaponCustom { it.copy(laserId = id) } },
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
      Text(
        text = "WONDER WEAPONS (${multiWonders.size}/3 EQUIPPED • TIERS T1–T25 MULTIVERSAL)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(WonderWeaponKind.entries, key = { it.id }) { kind ->
          val wt = progression.wonderTier(kind.id)
          val wStar = progression.wonderStar(kind.id)
          val wMeta = ProgressionCatalog.tierMeta(wt)
          val wCost = UpgradeCosts.wonderTier(wt)
          val wStarCost = UpgradeCosts.wonderStar(wStar)
          val inWonderLoadout = multiWonders.contains(kind.id)
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (inWonderLoadout) Color(kind.accentHex).copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .width(240.dp)
              .clip(RoundedCornerShape(14.dp))
              .combinedClickable(
                onClick = { onToggleMultiWonderEquip(kind.id) },
                onLongClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  onToggleMultiWonderEquip(kind.id)
                },
              )
              .border(
                width = if (inWonderLoadout) 2.dp else 1.dp,
                color = Color(kind.accentHex).copy(alpha = if (inWonderLoadout) 0.95f else 0.55f),
                shape = RoundedCornerShape(14.dp),
              ),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(kind.shortTag, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(kind.accentHex))
                Text("T$wt/25 • ${wStar}★ • ${wMeta.label}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(wMeta.colorHex))
              }
              Text(kind.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text(kind.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
              OutlinedButton(
                onClick = { onToggleMultiWonderEquip(kind.id) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("multi_equip_wonder_${kind.id}"),
              ) {
                Text(
                  text = if (inWonderLoadout) "EQUIPPED (${multiWonders.size}/3)" else "+ Equip Wonder Slot",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (inWonderLoadout) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                )
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                  onClick = { onUpgradeWonderTier(kind.id) },
                  enabled = wt < 25 && totalBlueEssence >= wCost,
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_wonder_${kind.id}"),
                ) {
                  Text(
                    text = if (wt >= 25) "MAX T25" else "T+1 (${wCost}B)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                  )
                }
                OutlinedButton(
                  onClick = { onUpgradeWonderStar(kind.id) },
                  enabled = wStar < StarRankMeta.MAX_STARS && totalBlueEssence >= wStarCost,
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_wonder_star_${kind.id}"),
                ) {
                  Text(
                    text = if (wStar >= StarRankMeta.MAX_STARS) "15★" else "★+1 (${wStarCost}B)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
      }
    }

    item {
      CustomizationChipGroup(
        title = "WONDER WEAPON ATTACHMENT CORE",
        options = ProgressionCatalog.wonderCores,
        selectedId = progression.wonderCustom.coreId,
        tagPrefix = "wonder_core",
        onSelect = { id -> onUpdateWonderCustom { it.copy(coreId = id) } },
      )
    }

    item {
      CustomizationChipGroup(
        title = "WONDER WEAPON FOCUS LENS / APERTURE",
        options = ProgressionCatalog.wonderLenses,
        selectedId = progression.wonderCustom.lensId,
        tagPrefix = "wonder_lens",
        onSelect = { id -> onUpdateWonderCustom { it.copy(lensId = id) } },
      )
    }

    item {
      CustomizationChipGroup(
        title = "WONDER WEAPON AMPLIFIER & POWER GRID",
        options = ProgressionCatalog.wonderAmplifiers,
        selectedId = progression.wonderCustom.amplifierId,
        tagPrefix = "wonder_amp",
        onSelect = { id -> onUpdateWonderCustom { it.copy(amplifierId = id) } },
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else MaterialTheme.colorScheme.outline, CircleShape)
                    .combinedClickable(
                      onClick = { onUpdateWeaponCustom { it.copy(tracerColorHex = hex) } },
                      onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUpdateWeaponCustom { it.copy(tracerColorHex = hex) }
                      },
                    )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CharacterCustomizationTab(
  progression: OperatorProgressionState,
  activeRig: RigAsset?,
  onUpdateCharacterCustom: ((CharacterCustomization) -> CharacterCustomization) -> Unit,
  onPrestigePlayer: () -> Unit = {},
  totalBlueEssence: Long = 0L,
  onUpgradeCharacterTier: () -> Unit = {},
  onUpgradeCharacterStar: () -> Unit = {},
  onEquipOutfit: (String) -> Unit = {},
) {
  val haptic = LocalHapticFeedback.current
  val cc = progression.characterCustom
  val activeSpec = progression.activeOutfitSpec()
  val (xpCur, xpNeed) = ProgressionCatalog.xpProgressInCurrentPlayerLevel(progression.playerXp)
  val visorHex = ProgressionCatalog.visorStyles.firstOrNull { it.first == cc.visorStyleId }?.second ?: cc.accentArmorHex
  val charTierMeta = ProgressionCatalog.tierMeta(progression.characterTier)
  val charStarMeta = ProgressionCatalog.starMeta(progression.characterStarLevel)
  val charTierCost = UpgradeCosts.characterTier(progression.characterTier)
  val charStarCost = UpgradeCosts.characterStar(progression.characterStarLevel)
  val powerLight = progression.computePowerAndLightLevel(totalBlueEssence)
  val milestoneSummary = ProgressionCatalog.milestoneBonusSummary(progression.playerLevel)
  var originalsOnlyFilter by remember { mutableStateOf(false) }
  val characterRoster = remember(originalsOnlyFilter) {
    if (originalsOnlyFilter) {
      MultiverseArsenalCatalog.outfits.filter { it.isOriginalCharacter }
    } else {
      MultiverseArsenalCatalog.outfits
    }
  }
  val capePath = remember { Path() }
  val torsoPath = remember { Path() }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("character_customization_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(cc.primaryArmorHex).copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
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
                  animationSpeedMultiplier = cc.animationSpeedMultiplier,
                )
                drawRigCharacter(
                  skeleton = skel,
                  archetype = activeSpec.rigArchetype.ifBlank { activeRig?.archetype ?: "operator" },
                  primaryColor = Color(cc.primaryArmorHex),
                  secondaryColor = Color(cc.accentArmorHex),
                  showBonesOverlay = false,
                  capePathScratch = capePath,
                  torsoPathScratch = torsoPath,
                  cloakOverride = cc.cloakEnabled,
                  visorColorOverride = Color(visorHex),
                  auraTrailId = cc.auraTrailId,
                  outfitId = activeSpec.id,
                )
              }
            }

            Column(modifier = Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "${activeSpec.nameWithVersion} (${activeSpec.originTag})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(activeSpec.secondaryHex),
                modifier = Modifier.testTag("character_active_hero_name"),
              )
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFFD54F).copy(alpha = 0.16f),
              ) {
                Text(
                  text = "PWR ${powerLight.powerLevel} • LIGHT ${powerLight.lightLevel} (${powerLight.tierLabel})",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFFFD54F),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).testTag("character_power_light_badge"),
                )
              }
              if (activeSpec.isOriginalCharacter) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFF00E676).copy(alpha = 0.18f),
                ) {
                  Text(
                    text = "ORIGINAL HERO • +25% POWER & SHIELD RESONANCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).testTag("character_original_badge"),
                  )
                }
              }
              Text(
                text = "LV ${progression.playerLevel}/1000 • P${progression.playerPrestige}/30 • ${progression.playerRankTitle}",
                style = MaterialTheme.typography.titleSmall,
                color = Color(cc.primaryArmorHex),
                modifier = Modifier.testTag("player_level_rank_text"),
              )
              Text(
                text = milestoneSummary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE040FB),
                modifier = Modifier.testTag("character_milestone_banner"),
              )
              Text(
                text = "Tier ${progression.characterTier}/25 (${charTierMeta.label}) • ${progression.characterStarLevel}★/10★ (${charStarMeta.label})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                modifier = Modifier.testTag("character_tier_star_badge"),
              )
              LinearProgressIndicator(
                progress = { (xpCur.toFloat() / xpNeed.coerceAtLeast(1L)).coerceIn(0f, 1f) },
                color = Color(cc.primaryArmorHex),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
              )
              val shieldBonus = ((progression.playerLevel - 1) * 2).coerceAtMost(600) + progression.playerPrestige * 25
              Text(
                text = "Hero Bonus: +${activeSpec.baseShieldBonus} SHD / +${activeSpec.baseDamageBonus} DMG • Milestone x${String.format(Locale.US, "%.2f", ProgressionCatalog.levelStatMultiplier(progression.playerLevel))} • +$shieldBonus SHD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onUpgradeCharacterTier,
              enabled = progression.characterTier < 25 && totalBlueEssence >= charTierCost,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_char_tier_btn"),
            ) {
              Text(if (progression.characterTier >= 25) "MAX TIER" else "Tier +1 ($charTierCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = onUpgradeCharacterStar,
              enabled = progression.characterStarLevel < 10 && totalBlueEssence >= charStarCost,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_char_star_btn"),
            ) {
              Text(if (progression.characterStarLevel >= 10) "MAX ★" else "Star +1 ($charStarCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }

          if (progression.playerLevel >= 1000 && progression.playerPrestige < 30) {
            Button(
              onClick = onPrestigePlayer,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("prestige_player_btn"),
            ) {
              Text("Prestige Operator (+${(progression.playerPrestige + 1) * 2500L} Blue Essence)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "PLAYABLE CHARACTER ROSTER (${characterRoster.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = !originalsOnlyFilter,
              onClick = { originalsOnlyFilter = false },
              label = { Text("All (${MultiverseArsenalCatalog.outfits.size})") },
              modifier = Modifier.heightIn(min = 48.dp).testTag("char_filter_all"),
            )
            FilterChip(
              selected = originalsOnlyFilter,
              onClick = { originalsOnlyFilter = true },
              label = { Text("Originals +25%") },
              modifier = Modifier.heightIn(min = 48.dp).testTag("char_filter_originals"),
            )
          }
        }
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.testTag("char_roster_row"),
        ) {
          items(characterRoster, key = { it.id }) { spec ->
            val selected = progression.equippedOutfitId == spec.id
            val accent = Color(spec.secondaryHex)
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
              modifier = Modifier
                .width(210.dp)
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                  onClick = { onEquipOutfit(spec.id) },
                  onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEquipOutfit(spec.id)
                  },
                )
                .border(
                  width = if (selected) 2.dp else 1.dp,
                  color = if (selected) accent else MaterialTheme.colorScheme.outline,
                  shape = RoundedCornerShape(14.dp),
                )
                .testTag("char_select_${spec.id}"),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = spec.originTag.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                  )
                  if (spec.isOriginalCharacter) {
                    Text(
                      text = "ORIG +25%",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color(0xFF00E676),
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
                Text(spec.nameWithVersion, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                  text = "+${spec.baseShieldBonus} SHD • +${spec.baseDamageBonus} DMG • +${(spec.baseMoveBonus * 100).toInt()}% SPD",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = if (selected) "ACTIVE CHARACTER" else "Tap to Play",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (selected) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                )
              }
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
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "RIGGING MOVEMENT SPEED: ${String.format(Locale.US, "%.2fx", cc.animationSpeedMultiplier)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Slider(
            value = cc.animationSpeedMultiplier.coerceIn(0.5f, 2.5f),
            onValueChange = { spd -> onUpdateCharacterCustom { it.copy(animationSpeedMultiplier = spd.coerceIn(0.5f, 2.5f)) } },
            valueRange = 0.5f..2.5f,
            modifier = Modifier.testTag("character_rig_speed_slider"),
          )
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
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(Color(hex))
                  .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else MaterialTheme.colorScheme.outline, CircleShape)
                  .combinedClickable(
                    onClick = { onUpdateCharacterCustom { it.copy(visorStyleId = id) } },
                    onLongClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onUpdateCharacterCustom { it.copy(visorStyleId = id) }
                    },
                  )
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
                modifier = Modifier.heightIn(min = 48.dp),
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LevelCustomizationTab(
  progression: OperatorProgressionState,
  onUpdateLevelCustom: ((LevelCustomization) -> LevelCustomization) -> Unit,
  activeWorldId: String = "sector_zero",
  onSelectNgPlusTier: (Int) -> Unit = {},
  onCompleteNgPlusRun: () -> Unit = {},
  onSelectCollectionDifficulty: (String, CollectionDifficulty) -> Unit = { _, _ -> },
) {
  val haptic = LocalHapticFeedback.current
  val lc = progression.levelCustom
  val activeWorld = UniverseCatalog.worldById(activeWorldId)
  val activeColDiff = progression.collectionDifficulty(activeWorldId)
  val activeNg = progression.ngPlusSpec
  val combinedRewardMult = progression.combinedGameplayRewardMultiplier(activeWorldId)

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("level_customization_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(activeColDiff.accentHex).copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "COLLECTION DIFFICULTY (${activeWorld.title.substringBefore(":")})",
            style = MaterialTheme.typography.titleSmall,
            color = Color(activeColDiff.accentHex),
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${activeColDiff.label} • ${activeColDiff.summary}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CollectionDifficulty.entries, key = { it.id }) { diff ->
              val selected = activeColDiff == diff
              val accent = Color(diff.accentHex)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .combinedClickable(
                    onClick = { onSelectCollectionDifficulty(activeWorldId, diff) },
                    onLongClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onSelectCollectionDifficulty(activeWorldId, diff)
                    },
                  )
                  .border(if (selected) 2.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                  .testTag("level_collection_diff_${diff.id}"),
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalArrangement = Arrangement.Center,
                ) {
                  Text(
                    text = diff.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = "HP x${String.format(Locale.US, "%.2f", diff.enemyHpScale)} • RWD x${String.format(Locale.US, "%.2f", diff.rewardMultiplier)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD54F),
                  )
                }
              }
            }
          }
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(activeNg.accentHex).copy(alpha = 0.72f), RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "NEW GAME+ PROTOCOL (${activeNg.label.uppercase()})",
                style = MaterialTheme.typography.titleSmall,
                color = Color(activeNg.accentHex),
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Completions: ${progression.ngPlusCompletions} • NG+ Mult x${String.format(Locale.US, "%.2f", progression.ngPlusRewardMultiplier)} • Combined x${String.format(Locale.US, "%.2f", combinedRewardMult)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFD54F),
              )
            }
            OutlinedButton(
              onClick = onCompleteNgPlusRun,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.heightIn(min = 48.dp).testTag("level_ng_plus_complete_btn"),
            ) {
              Text("Log Run", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(NewGamePlusVersion.entries, key = { it.tier }) { ver ->
              val unlocked = ver.tier <= progression.unlockedNgPlusTier
              val selected = progression.activeNgPlusTier == ver.tier
              val accent = Color(ver.accentHex)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .combinedClickable(
                    enabled = unlocked,
                    onClick = { onSelectNgPlusTier(ver.tier) },
                    onLongClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onSelectNgPlusTier(ver.tier)
                    },
                  )
                  .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) accent else if (unlocked) accent.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                  )
                  .testTag("level_ng_plus_${ver.tier}"),
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalArrangement = Arrangement.Center,
                ) {
                  Text(
                    text = if (unlocked) ver.shortTag else "LOCKED ${ver.shortTag}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = "RWD x${String.format(Locale.US, "%.2f", ver.rewardMultiplier)} • HP x${String.format(Locale.US, "%.2f", ver.enemyHpScale)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unlocked) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(lc.worldDifficultyTier.accentHex).copy(alpha = 0.65f), RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "WORLD DIFFICULTY TIER (${lc.worldDifficultyTier.label.uppercase()})",
            style = MaterialTheme.typography.titleSmall,
            color = Color(lc.worldDifficultyTier.accentHex),
          )
          Text(
            text = "Enemy HP x${String.format(Locale.US, "%.2f", lc.worldDifficultyTier.enemyHpScale)} • Speed x${String.format(Locale.US, "%.2f", lc.worldDifficultyTier.enemySpeedScale)} • Rewards x${String.format(Locale.US, "%.2f", lc.worldDifficultyTier.rewardScale)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFFD54F),
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WorldDifficultyTier.entries, key = { it.id }) { tier ->
              FilterChip(
                selected = lc.worldDifficultyTier == tier,
                onClick = { onUpdateLevelCustom { it.copy(worldDifficultyTier = tier) } },
                label = { Text(tier.label) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("difficulty_tier_${tier.id}"),
              )
            }
          }
        }
      }
    }

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
            text = "Enemy Wave Density: ${String.format(Locale.US, "%.2fx", lc.enemyDensityMultiplier)} (up to 2.5x Swarm)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Slider(
            value = lc.enemyDensityMultiplier.coerceIn(0.75f, 2.5f),
            onValueChange = { v -> onUpdateLevelCustom { it.copy(enemyDensityMultiplier = v.coerceIn(0.75f, 2.5f)) } },
            valueRange = 0.75f..2.5f,
            modifier = Modifier.testTag("level_density_slider"),
          )
          Text("FLOOR GRID ACCENT COLOR (TAP OR HOLD)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          FloorAccentRow(
            selectedHex = lc.floorAccentHex,
            onSelect = { hex -> onUpdateLevelCustom { it.copy(floorAccentHex = hex) } },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FloorAccentRow(
  selectedHex: Long,
  onSelect: (Long) -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    tracerSwatches.forEach { (label, hex) ->
      val selected = selectedHex == hex
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(Color(hex))
          .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else MaterialTheme.colorScheme.outline, CircleShape)
          .combinedClickable(
            onClick = { onSelect(hex) },
            onLongClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onSelect(hex)
            },
          )
          .testTag("floor_accent_$label"),
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EquipmentCustomizationTab(
  progression: OperatorProgressionState,
  onUpgradeEquipment: (String) -> Unit,
  onPrestigeEquipment: (String) -> Unit,
  onEquipEquipment: (String, String) -> Unit,
  totalBlueEssence: Long = 0L,
  onUpgradeEquipmentTier: (String) -> Unit = {},
  onToggleMultiEquipmentEquip: (com.example.data.model.EquipmentSlotType, String) -> Unit = { _, _ -> },
  onUpgradeEquipmentStar: (String) -> Unit = {},
  onUpgradeCustomEquipmentTier: (String) -> Unit = {},
  onUpgradeCustomEquipmentStar: (String) -> Unit = {},
) {
  val haptic = LocalHapticFeedback.current
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text("MULTI-EQUIP LETHAL & TACTICAL LOADOUT (UP TO 2 PER SLOT)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
      Text("Tap or Hold any equipment card to equip/upgrade • Level to 60, Tier 1–25, Prestige P30.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    items(com.example.data.model.EquipmentSlotType.entries, key = { it.id }) { slotType ->
      val eqState = progression.equipmentState(slotType.id)
      val itemsForSlot = ProgressionCatalog.allEquipmentItems(progression.customEquipment).filter { it.slotType == slotType }
      val equippedItem = itemsForSlot.firstOrNull { it.id == eqState.equippedItemId }
        ?: itemsForSlot.firstOrNull()
        ?: ProgressionCatalog.equipmentItems.first { it.slotType == slotType }
      val multiList = if (slotType == com.example.data.model.EquipmentSlotType.LETHAL) {
        progression.equippedLethalIds.ifEmpty { listOf(eqState.equippedItemId) }
      } else {
        progression.equippedTacticalIds.ifEmpty { listOf(eqState.equippedItemId) }
      }
      val (eqXpCur, eqXpNeed) = ProgressionCatalog.xpProgressInCurrentEquipmentLevel(eqState.xp)
      val eqTierMeta = ProgressionCatalog.tierMeta(eqState.tier)
      val eqTierCost = UpgradeCosts.equipmentTier(eqState.tier)
      val eqStarCost = UpgradeCosts.equipmentStar(eqState.starLevel)

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
              Text("${slotType.displayName} (${multiList.size}/2 Active) • T${eqState.tier}/25 • ${eqState.starLevel}★", style = MaterialTheme.typography.labelMedium, color = Color(eqTierMeta.colorHex))
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

          LinearProgressIndicator(
            progress = { (eqXpCur.toFloat() / eqXpNeed.coerceAtLeast(1L)).coerceIn(0f, 1f) },
            color = Color(equippedItem.defaultColorHex),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
          )
          Text(
            text = "Active Multi-Loadout (${multiList.size}/2): ${multiList.joinToString(" + ") { id -> itemsForSlot.firstOrNull { it.id == id }?.name ?: id }}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF00E5FF),
          )

          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(itemsForSlot, key = { it.id }) { item ->
              val isPrimary = item.id == eqState.equippedItemId
              val inMulti = multiList.contains(item.id)
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                  selected = isPrimary,
                  onClick = { onEquipEquipment(slotType.id, item.id) },
                  label = { Text(item.name) },
                  modifier = Modifier.heightIn(min = 48.dp).testTag("equip_item_${item.id}"),
                )
                OutlinedButton(
                  onClick = { onToggleMultiEquipmentEquip(slotType, item.id) },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.height(48.dp).testTag("multi_equip_${item.id}"),
                ) {
                  Text(
                    text = if (inMulti) "Loaded (${multiList.size}/2)" else "+ Dual-Load",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (inMulti) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                  onClick = { onUpgradeEquipment(slotType.id) },
                  onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUpgradeEquipment(slotType.id)
                  },
                )
                .testTag("upgrade_eq_${slotType.id}"),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
                Text(
                  text = "Level +1 (${eqState.level}/60)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  textAlign = TextAlign.Center,
                )
              }
            }
            OutlinedButton(
              onClick = { onUpgradeEquipmentTier(slotType.id) },
              enabled = eqState.tier < 25 && totalBlueEssence >= eqTierCost,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_eq_tier_${slotType.id}"),
            ) {
              Text(
                text = if (eqState.tier >= 25) "T25 MAX" else "Tier +1 ($eqTierCost B)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
            }
            OutlinedButton(
              onClick = { onUpgradeEquipmentStar(slotType.id) },
              enabled = eqState.starLevel < 10 && totalBlueEssence >= eqStarCost,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_eq_star_${slotType.id}"),
            ) {
              Text(
                text = if (eqState.starLevel >= 10) "10★ MAX" else "Star +1 ($eqStarCost B)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
            }
            if (eqState.level >= 60 && eqState.prestige < 30) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .combinedClickable(
                    onClick = { onPrestigeEquipment(slotType.id) },
                    onLongClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onPrestigeEquipment(slotType.id)
                    },
                  )
                  .testTag("prestige_eq_${slotType.id}"),
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
                  Text(
                    text = "Prestige (${eqState.prestige}/30)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center,
                  )
                }
              }
            }
          }
        }
      }
    }

    if (progression.customEquipment.isNotEmpty()) {
      item {
        Text("FORGED CUSTOM EQUIPMENT (${progression.customEquipment.size})", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF6D00))
      }
      items(progression.customEquipment, key = { it.id }) { cEq ->
        val accent = Color(cEq.accentHex)
        val tierCost = UpgradeCosts.equipmentTier(cEq.tier)
        val starCost = UpgradeCosts.equipmentStar(cEq.starLevel)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().border(1.2.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${cEq.slotType.displayName}: ${cEq.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
                Text(cEq.description.ifBlank { "Forged ${cEq.slotType.displayName} • ${cEq.damageTypeId.uppercase()}" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Surface(shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = 0.2f)) {
                Text(
                  text = "T${cEq.tier}/${cEq.maxTier} • ${cEq.starLevel}★",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = accent,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = { onEquipEquipment(cEq.slotType.id, cEq.id) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("equip_ceq_${cEq.id}"),
              ) {
                Text("Equip", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
              OutlinedButton(
                onClick = { onUpgradeCustomEquipmentTier(cEq.id) },
                enabled = cEq.tier < cEq.maxTier && totalBlueEssence >= tierCost,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_ceq_tier_${cEq.id}"),
              ) {
                Text(if (cEq.tier >= cEq.maxTier) "T${cEq.maxTier} MAX" else "Tier +1 ($tierCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
              OutlinedButton(
                onClick = { onUpgradeCustomEquipmentStar(cEq.id) },
                enabled = cEq.starLevel < 10 && totalBlueEssence >= starCost,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_ceq_star_${cEq.id}"),
              ) {
                Text(if (cEq.starLevel >= 10) "10★ MAX" else "Star +1 ($starCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GearCustomizationTab(
  progression: OperatorProgressionState,
  onUpgradeGear: (GearSlot) -> Unit,
  onPrestigeGear: () -> Unit,
  totalBlueEssence: Long = 0L,
  onUpgradeGearTier: (GearSlot) -> Unit = {},
  onUpgradeGearStar: (GearSlot) -> Unit = {},
  onUpgradeCustomGearTier: (String) -> Unit = {},
  onUpgradeCustomGearStar: (String) -> Unit = {},
  onEquipCustomGear: (GearSlot, String) -> Unit = { _, _ -> },
) {
  val haptic = LocalHapticFeedback.current
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
            text = "Tap or Hold to upgrade 5 gear modules up to Level 50, Tiers 1–25, and Prestige P30.",
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
          shape = RoundedCornerShape(12.dp),
          color = if (allMaxed) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
              enabled = allMaxed,
              onClick = { if (allMaxed) onPrestigeGear() },
              onLongClick = {
                if (allMaxed) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  onPrestigeGear()
                }
              },
            )
            .testTag("gear_prestige_action_btn"),
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)) {
            Text(
              text = if (allMaxed) "PRESTIGE ALL GEAR (Ascend to P${progression.gearPrestige + 1})" else "Reach Level 50 on all 5 Gear Slots to Prestige",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = if (allMaxed) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }

    items(GearSlot.entries, key = { it.id }) { slot ->
      val gear = progression.gearMap[slot.id] ?: GearState(slot)
      val gearTierMeta = ProgressionCatalog.tierMeta(gear.tier)
      val gearTierCost = UpgradeCosts.gearTier(gear.tier)
      val gearStarCost = UpgradeCosts.gearStar(gear.starLevel)
      val (gXpCur, gXpNeed) = ProgressionCatalog.xpProgressInCurrentGearLevel(gear.xp)
      val slotDesc = when (slot) {
        GearSlot.HELMET -> "+Target HUD tracking & +${gear.level * 2}% Precision critical damage"
        GearSlot.CHEST -> "+${gear.level * 3} Max Shield armor & +${gear.level / 8} Armor siphon"
        GearSlot.GLOVES -> "+${(gear.level * 0.4f)}% Volley fire rate & +${gear.level / 8} Melee claw strike"
        GearSlot.BOOTS -> "+${(gear.level * 0.35f)}% Sprint speed & -${(gear.level * 0.4f)}% Dash cooldown"
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
              Text("${slot.displayName} • Tier ${gear.tier}/25 • ${gear.starLevel}★", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            progress = { (gXpCur.toFloat() / gXpNeed.coerceAtLeast(1L)).coerceIn(0f, 1f) },
            color = Color(gear.colorHex),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
          )
          Text(
            text = "Gear XP: $gXpCur / $gXpNeed • Tier Bonus x${String.format(Locale.US, "%.2f", gearTierMeta.multiplier)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (gear.level < 50) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                  enabled = gear.level < 50,
                  onClick = { if (gear.level < 50) onUpgradeGear(slot) },
                  onLongClick = {
                    if (gear.level < 50) {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onUpgradeGear(slot)
                    }
                  },
                )
                .testTag("upgrade_gear_${slot.id}"),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
                Text(
                  text = if (gear.level < 50) "Level +1 (${gear.level}/50)" else "MAX LVL (50/50)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (gear.level < 50) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                )
              }
            }
            OutlinedButton(
              onClick = { onUpgradeGearTier(slot) },
              enabled = gear.tier < 25 && totalBlueEssence >= gearTierCost,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_gear_tier_${slot.id}"),
            ) {
              Text(
                text = if (gear.tier >= 25) "T25 MAX" else "Tier +1 ($gearTierCost B)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
            }
            OutlinedButton(
              onClick = { onUpgradeGearStar(slot) },
              enabled = gear.starLevel < 10 && totalBlueEssence >= gearStarCost,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_gear_star_${slot.id}"),
            ) {
              Text(
                text = if (gear.starLevel >= 10) "10★ MAX" else "Star +1 ($gearStarCost B)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }

    if (progression.customGear.isNotEmpty()) {
      item {
        Text("FORGED CUSTOM GEAR MODULES (${progression.customGear.size})", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00E5FF))
      }
      items(progression.customGear, key = { it.id }) { cGear ->
        val accent = Color(cGear.accentHex)
        val tierCost = UpgradeCosts.gearTier(cGear.tier)
        val starCost = UpgradeCosts.gearStar(cGear.starLevel)
        val isEquipped = progression.gearMap[cGear.slot.id]?.colorHex == cGear.accentHex
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().border(1.2.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${cGear.slot.displayName}: ${cGear.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
                Text(cGear.description.ifBlank { "Forged ${cGear.slot.displayName} • +${cGear.bonusShield} Shield • +${cGear.bonusDamagePct}% DMG" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Surface(shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = 0.2f)) {
                Text(
                  text = "T${cGear.tier}/${cGear.maxTier} • ${cGear.starLevel}★",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = accent,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = { onEquipCustomGear(cGear.slot, cGear.id) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("equip_cgear_${cGear.id}"),
              ) {
                Text("Equip", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
              OutlinedButton(
                onClick = { onUpgradeCustomGearTier(cGear.id) },
                enabled = cGear.tier < cGear.maxTier && totalBlueEssence >= tierCost,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_cgear_tier_${cGear.id}"),
              ) {
                Text(if (cGear.tier >= cGear.maxTier) "T${cGear.maxTier} MAX" else "Tier +1 ($tierCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
              OutlinedButton(
                onClick = { onUpgradeCustomGearStar(cGear.id) },
                enabled = cGear.starLevel < 10 && totalBlueEssence >= starCost,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_cgear_star_${cGear.id}"),
              ) {
                Text(if (cGear.starLevel >= 10) "10★ MAX" else "Star +1 ($starCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SkillTreeTab(
  progression: OperatorProgressionState,
  totalEssence: Long,
  onUnlockSkill: (String) -> Unit,
  onUpgradeSkillStar: (String) -> Unit = {},
) {
  val haptic = LocalHapticFeedback.current
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
          Text("OPERATOR SKILL TREE (TIERS 1–25)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          Text(
            text = "Tap or Hold skill nodes to unlock and ascend from Tier 1 to Tier 25 Multiversal with Blue Essence.",
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
            modifier = Modifier.heightIn(min = 48.dp).testTag("skill_cat_$cat"),
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
      val tierMeta = ProgressionCatalog.tierMeta(node.tier)
      val nextCost = if (!node.unlocked) node.costEssence else node.nextUpgradeCost()
      val skillStarCost = UpgradeCosts.skillStar(node.starLevel)
      val isMaxTier = node.unlocked && node.tier >= node.maxTier

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
              color = if (node.unlocked) Color(tierMeta.colorHex).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Text(
                text = if (node.unlocked) "T${node.tier}/${node.maxTier} • ${node.starLevel}★" else "${node.costEssence} B",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (node.unlocked) Color(tierMeta.colorHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }

          if (!isMaxTier) {
            val canAfford = totalEssence >= nextCost
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (canAfford) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                  enabled = canAfford,
                  onClick = { onUnlockSkill(node.id) },
                  onLongClick = {
                    if (canAfford) {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onUnlockSkill(node.id)
                    }
                  },
                )
                .testTag("unlock_skill_${node.id}"),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp)) {
                Text(
                  text = when {
                    !node.unlocked && canAfford -> "Unlock Skill (${nextCost} Blue Essence)"
                    node.unlocked && canAfford -> "Upgrade to Tier ${node.tier + 1} (${nextCost} Blue Essence)"
                    else -> "Need ${nextCost - totalEssence} More Blue Essence"
                  },
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (canAfford) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                  textAlign = TextAlign.Center,
                )
              }
            }
          } else {
            Text(
              text = "MAX TIER ${node.maxTier} MULTIVERSAL ACTIVE",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF00E676),
            )
          }

          if (node.unlocked) {
            OutlinedButton(
              onClick = { onUpgradeSkillStar(node.id) },
              enabled = node.starLevel < 10 && totalEssence >= skillStarCost,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("upgrade_skill_star_${node.id}"),
            ) {
              Text(
                text = if (node.starLevel >= 10) "10★ MAX" else "Skill Star +1 (${node.starLevel}★ -> ${node.starLevel + 1}★, $skillStarCost B)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomizationChipGroup(
  title: String,
  options: List<Pair<String, String>>,
  selectedId: String,
  tagPrefix: String,
  onSelect: (String) -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(options, key = { it.first }) { (id, label) ->
        val selected = selectedId == id
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
              onClick = { onSelect(id) },
              onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSelect(id)
              },
            )
            .border(
              width = if (selected) 1.5.dp else 1.dp,
              color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
              shape = RoundedCornerShape(10.dp),
            )
            .testTag("${tagPrefix}_$id"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MutatorToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  tag: String,
  onCheckedChange: (Boolean) -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 52.dp)
      .clip(RoundedCornerShape(10.dp))
      .combinedClickable(
        onClick = { onCheckedChange(!checked) },
        onLongClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onCheckedChange(!checked)
        },
      )
      .padding(horizontal = 4.dp, vertical = 4.dp),
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

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ArmoryItem
import com.example.data.model.BlastCoreColor
import com.example.data.model.CharacterRarity
import com.example.data.model.CharacterUniverseGroup
import com.example.data.model.EssenceColor
import com.example.data.model.EssenceResonanceBonuses
import com.example.data.model.HeroOutfitSpec
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RegionalServerSpec
import com.example.data.model.StarRankMeta
import com.example.data.model.UniverseCatalog
import com.example.data.model.UpgradeCosts
import com.example.data.model.WorldRegion
import com.example.ui.rig.RigEvaluator
import com.example.ui.rig.drawRigCharacter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutfitsMenuTab(
  progression: OperatorProgressionState,
  totalBlueEssence: Long,
  onEquipOutfit: (String) -> Unit,
  onUpgradeOutfit: (String) -> Unit,
  onUpgradeOutfitSubSystem: (String, String) -> Unit,
  onUpgradeOutfitStar: (String) -> Unit = {},
  onUpgradeOutfitRarity: (String) -> Unit = {},
  onRecruitCharacterShard: (String?) -> Unit = {},
  onHuntHeroFrontier: () -> Unit = {},
) {
  val haptic = LocalHapticFeedback.current
  var selectedGroup by remember { mutableStateOf(CharacterUniverseGroup.ALL) }
  val outfits = remember(selectedGroup) {
    if (selectedGroup == CharacterUniverseGroup.ALL) {
      MultiverseArsenalCatalog.outfits
    } else {
      MultiverseArsenalCatalog.outfits.filter { it.characterGroup == selectedGroup }
    }
  }
  val activeSpec = progression.activeOutfitSpec()
  val activeState = progression.outfitState(activeSpec.id)
  val activeRarity = progression.outfitRarity(activeSpec.id)
  val activeShards = progression.outfitShards(activeSpec.id)
  val capePath = remember { Path() }
  val torsoPath = remember { Path() }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("outfits_menu_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, Color(activeSpec.secondaryHex).copy(alpha = 0.75f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(width = 96.dp, height = 118.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A101E))
                .border(1.dp, Color(activeSpec.secondaryHex).copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val skel = RigEvaluator.evaluate(
                  origin = Offset(size.width * 0.5f, size.height * 0.56f),
                  scalePx = size.height * 0.52f,
                  poseAngles = UniverseCatalog.defaultPoseMap(),
                  clip = "idle",
                  frame = 10f,
                  ikEnabled = true,
                  animationSpeedMultiplier = progression.characterCustom.animationSpeedMultiplier,
                )
                drawRigCharacter(
                  skeleton = skel,
                  archetype = activeSpec.rigArchetype,
                  primaryColor = Color(activeSpec.primaryHex),
                  secondaryColor = Color(activeSpec.secondaryHex),
                  showBonesOverlay = false,
                  capePathScratch = capePath,
                  torsoPathScratch = torsoPath,
                  cloakOverride = activeSpec.cloakEnabled,
                  visorColorOverride = Color(activeSpec.visorHex),
                  auraTrailId = activeSpec.auraId,
                  outfitId = activeSpec.id,
                )
              }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "ACTIVE CHARACTER: ${activeSpec.nameWithVersion}",
                style = MaterialTheme.typography.titleMedium,
                color = Color(activeSpec.secondaryHex),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("active_outfit_banner"),
              )
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(activeRarity.colorHex).copy(alpha = 0.2f),
                ) {
                  Text(
                    text = "${activeRarity.label.uppercase()} (x${String.format(Locale.US, "%.2f", activeRarity.statMultiplier)}) • $activeShards SHARDS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(activeRarity.colorHex),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
              if (activeSpec.isOriginalCharacter) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFF00E676).copy(alpha = 0.18f),
                ) {
                  Text(
                    text = "${activeSpec.originTag.uppercase()} • ORIGINAL HERO +25% POWER BUFF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
              Text(activeSpec.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
              Text(
                text = "Base: +${activeSpec.baseShieldBonus} SHD • +${activeSpec.baseDamageBonus} DMG • +${(activeSpec.baseMoveBonus * 100).toInt()}% SPD",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFD54F),
              )
              Text(
                text = "Lv.${activeState.level}/1000 • Tier ${activeState.tier}/25 • ${activeState.starLevel}★ (${StarRankMeta.forStars(activeState.starLevel).label})",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Own Skill R${activeState.skillRank} • Gear R${activeState.gearRank} • Equip R${activeState.equipmentRank}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00E676),
              )
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onHuntHeroFrontier,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("hunt_frontier_btn"),
            ) {
              Text("Hunt Hero Frontier", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = { onRecruitCharacterShard(null) },
              enabled = totalBlueEssence >= 200L,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("recruit_shard_beacon_btn"),
            ) {
              Text("Beacon Loot (200 B)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    item {
      Text("PLAYABLE CHARACTERS (${MultiverseArsenalCatalog.outfits.size} VERSIONS • SHARD RARITY & +25% ORIGINALS)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CharacterUniverseGroup.entries, key = { it.id }) { group ->
          FilterChip(
            selected = selectedGroup == group,
            onClick = { selectedGroup = group },
            label = { Text(group.label) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("outfit_group_${group.id}"),
          )
        }
      }
    }

    items(outfits, key = { it.id }) { spec ->
      val st = progression.outfitState(spec.id)
      val isEquipped = progression.equippedOutfitId == spec.id
      val tierMeta = ProgressionCatalog.tierMeta(st.tier)
      val starMeta = StarRankMeta.forStars(st.starLevel)
      val rarity = progression.outfitRarity(spec.id)
      val nextRarity = rarity.nextRarity()
      val shards = progression.outfitShards(spec.id)
      val upgCost = UpgradeCosts.outfitLevelUp(st.tier)
      val starCost = UpgradeCosts.outfitStar(st.starLevel)
      val skillCost = UpgradeCosts.outfitSubSystem(st.skillRank)
      val gearCost = UpgradeCosts.outfitSubSystem(st.gearRank)
      val equipCost = UpgradeCosts.outfitSubSystem(st.equipmentRank)
      val accent = Color(spec.secondaryHex)

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .combinedClickable(
            onClick = { onEquipOutfit(spec.id) },
            onLongClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onEquipOutfit(spec.id)
            },
          )
          .border(
            width = if (isEquipped) 2.dp else 1.dp,
            color = if (isEquipped) accent else MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(16.dp),
          ),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                  modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(spec.primaryHex))
                    .border(1.5.dp, accent, RoundedCornerShape(6.dp)),
                )
                Box(
                  modifier = Modifier
                    .size(width = 26.dp, height = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(spec.visorHex)),
                )
              }
              Column {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = spec.nameWithVersion,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEquipped) accent else MaterialTheme.colorScheme.onSurface,
                  )
                  Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = Color(rarity.colorHex).copy(alpha = 0.22f),
                  ) {
                    Text(
                      text = rarity.label.uppercase(),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color(rarity.colorHex),
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                  }
                  if (spec.isOriginalCharacter) {
                    Surface(
                      shape = RoundedCornerShape(5.dp),
                      color = Color(0xFF00E676).copy(alpha = 0.2f),
                    ) {
                      Text(
                        text = "ORIGINAL +25%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                      )
                    }
                  }
                }
                Text(
                  text = "${spec.originTag} • ${spec.subtitle} • Rig: ${spec.rigArchetype.uppercase()}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(tierMeta.colorHex).copy(alpha = 0.2f),
            ) {
              Text(
                text = "T${st.tier}/25 • ${st.starLevel}★",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(tierMeta.colorHex),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }

          Text(
            text = "Stats: +${spec.baseShieldBonus} SHD • +${spec.baseDamageBonus} DMG • +${(spec.baseMoveBonus * 100).toInt()}% SPD • Rarity x${String.format(Locale.US, "%.2f", rarity.statMultiplier)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF00E5FF),
          )
          Text(
            text = "Shards: $shards${if (nextRarity != null) " / ${rarity.shardsForNext} -> ${nextRarity.label}" else " (MAX MYTHIC)"} • ${spec.signatureGearSummary}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFFD54F),
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { onEquipOutfit(spec.id) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isEquipped) Color(0xFF00E676) else accent,
                contentColor = Color.Black,
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("equip_outfit_${spec.id}"),
            ) {
              Text(if (isEquipped) "EQUIPPED" else "Select Character", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = { onUpgradeOutfit(spec.id) },
              enabled = totalBlueEssence >= upgCost && st.level < 1000,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_outfit_${spec.id}"),
            ) {
              Text(if (st.level >= 1000) "MAX LV" else "Lv+5 ($upgCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = { onUpgradeOutfitStar(spec.id) },
              enabled = st.starLevel < StarRankMeta.MAX_STARS && totalBlueEssence >= starCost,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_outfit_star_${spec.id}"),
            ) {
              Text(if (st.starLevel >= StarRankMeta.MAX_STARS) "10★" else "★+1 ($starCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = { onUpgradeOutfitRarity(spec.id) },
              enabled = nextRarity != null && shards >= rarity.shardsForNext,
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_rarity_${spec.id}"),
            ) {
              Text(
                text = if (nextRarity == null) "MYTHIC MAX" else "Fuse ${nextRarity.label} ($shards/${rarity.shardsForNext})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            OutlinedButton(
              onClick = { onRecruitCharacterShard(spec.id) },
              enabled = totalBlueEssence >= 200L,
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
              modifier = Modifier.weight(0.8f).height(48.dp).testTag("recruit_shard_${spec.id}"),
            ) {
              Text("+Shard (200 B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = { onUpgradeOutfitSubSystem(spec.id, "skill") },
              enabled = st.skillRank < 20 && totalBlueEssence >= skillCost,
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("outfit_skill_${spec.id}"),
            ) {
              Text("Skill R${st.skillRank}/20\n$skillCost B", style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
            OutlinedButton(
              onClick = { onUpgradeOutfitSubSystem(spec.id, "gear") },
              enabled = st.gearRank < 20 && totalBlueEssence >= gearCost,
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("outfit_gear_${spec.id}"),
            ) {
              Text("Gear R${st.gearRank}/20\n$gearCost B", style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
            OutlinedButton(
              onClick = { onUpgradeOutfitSubSystem(spec.id, "equipment") },
              enabled = st.equipmentRank < 20 && totalBlueEssence >= equipCost,
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("outfit_equip_${spec.id}"),
            ) {
              Text("Equip R${st.equipmentRank}/20\n$equipCost B", style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShardsPerksAndCoresTab(
  progression: OperatorProgressionState,
  totalBlueEssence: Long,
  onSpendBlastShards: () -> Unit,
  onUpgradeVacuumBot: () -> Unit,
  onToggleVacuumAutoUpgrade: () -> Unit,
  onUpgradePerkDrink: (String) -> Unit,
  onUpgradeBlastCore: (BlastCoreColor) -> Unit,
  onUpgradeVacuumBotStar: () -> Unit = {},
  onUpgradeCustomPerkTier: (String) -> Unit = {},
  onUpgradeCustomPerkStar: (String) -> Unit = {},
  armoryItems: List<ArmoryItem> = emptyList(),
  region: WorldRegion = WorldRegion.NA_EAST,
  server: RegionalServerSpec? = null,
) {
  val haptic = LocalHapticFeedback.current
  val resonance = remember(
    totalBlueEssence,
    progression,
    armoryItems,
    region,
    server,
  ) {
    progression.comprehensiveBonuses(
      blueEssence = totalBlueEssence,
      armoryItems = armoryItems,
      region = region,
      server = server,
    )
  }
  val bot = progression.vacuumBot
  val botCost = UpgradeCosts.vacuumBot(bot.tier)
  val botStarCost = UpgradeCosts.vacuumBotStar(bot.starLevel)

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("shards_perks_cores_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(0xFFE040FB).copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("OMNI-SOURCE RESONANCE & BONUSES", style = MaterialTheme.typography.titleMedium, color = Color(0xFFE040FB), fontWeight = FontWeight.Bold)
          Text(
            text = "All bonuses scale uncapped from held/spent Essence, Armory Mods, 30 Upgraded Maps, Weapons, Gear, Skills, Perks, Blast Cores & ${region.shortTag} Server.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0A1220),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "LIVE BONUS: ${resonance.summary} • CDR ${(resonance.cooldownReductionRatio * 100).toInt()}% • Regen +${String.format(Locale.US, "%.1f", resonance.regenBonusPerSec)}/s",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("essence_resonance_summary"),
              )
              if (resonance.extendedSourcesSummary.isNotBlank()) {
                Text(
                  text = "Sources: ${resonance.extendedSourcesSummary}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF00E676),
                  modifier = Modifier.testTag("essence_extended_sources_summary"),
                )
              }
            }
          }
          EssenceColor.entries.forEach { ec ->
            val bal = when (ec) {
              EssenceColor.BLUE -> totalBlueEssence
              EssenceColor.RED -> progression.redEssence
              EssenceColor.YELLOW -> progression.yellowEssence
              EssenceColor.GREEN -> progression.greenEssence
              EssenceColor.COMBINED -> progression.combinedEssence
            }
            val col = Color(ec.colorHex)
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = col.copy(alpha = 0.1f),
              modifier = Modifier.fillMaxWidth().border(1.dp, col.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            ) {
              Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(ec.displayName, style = MaterialTheme.typography.labelLarge, color = col, fontWeight = FontWeight.Bold)
                  Text("$bal HELD", style = MaterialTheme.typography.labelLarge, color = col, fontWeight = FontWeight.Bold)
                }
                Text(ec.specialization, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(ec.beyondCapNote, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(0xFF40C4FF).copy(alpha = 0.75f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text("VACUUM BOT & BLAST SHARDS", style = MaterialTheme.typography.titleMedium, color = Color(0xFF40C4FF), fontWeight = FontWeight.Bold)
              Text(
                text = "Vacuum Bot Lv.${bot.level}/50 • T${bot.tier}/25 • ${bot.starLevel}★ • Reach ${(bot.vacuumRadius * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF40C4FF).copy(alpha = 0.2f)) {
              Text(
                text = "${progression.blastShards} SHARDS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF40C4FF),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp).testTag("blast_shards_balance"),
              )
            }
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Auto-Upgrade in Arena", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              Text("When Vacuum Bot charges to 100%, auto-upgrades Pack-a-Punch or restores Armor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = bot.autoUpgradeEnabled,
              onCheckedChange = { onToggleVacuumAutoUpgrade() },
              modifier = Modifier.testTag("toggle_vac_auto_btn"),
            )
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onSpendBlastShards,
              enabled = progression.blastShards >= 10L,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("spend_blast_shards_btn"),
            ) {
              Text("Channel Shards", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = onUpgradeVacuumBot,
              enabled = totalBlueEssence >= botCost && (bot.tier < 25 || bot.level < 50),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_vac_bot_btn"),
            ) {
              Text("Bot +Lv3 ($botCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = onUpgradeVacuumBotStar,
              enabled = bot.starLevel < StarRankMeta.MAX_STARS && totalBlueEssence >= botStarCost,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_vac_bot_star_btn"),
            ) {
              Text(if (bot.starLevel >= StarRankMeta.MAX_STARS) "10★" else "★+1 ($botStarCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    item {
      Text("PERK CANS & BOTTLES (8 CLASSICS)", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFD54F))
    }

    items(MultiverseArsenalCatalog.perkDrinks, key = { it.id }) { perk ->
      val rank = progression.perkRank(perk.id)
      val cost = UpgradeCosts.perkDrink(rank)
      val accent = Color(perk.colorHex)
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .combinedClickable(
            enabled = rank < perk.maxRank && totalBlueEssence >= cost,
            onClick = { onUpgradePerkDrink(perk.id) },
            onLongClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onUpgradePerkDrink(perk.id)
            },
          )
          .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
              text = "${perk.container.label.uppercase()} • RANK $rank/${perk.maxRank}",
              style = MaterialTheme.typography.labelSmall,
              color = accent,
              fontWeight = FontWeight.Bold,
            )
            Text(perk.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(perk.summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Button(
            onClick = { onUpgradePerkDrink(perk.id) },
            enabled = rank < perk.maxRank && totalBlueEssence >= cost,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(48.dp).testTag("upgrade_perk_${perk.id}"),
          ) {
            Text(if (rank >= perk.maxRank) "MAX" else "$cost B", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    if (progression.customPerks.isNotEmpty()) {
      item {
        Text("FORGED CUSTOM PERKS (${progression.customPerks.size})", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00E676))
      }
      items(progression.customPerks, key = { it.id }) { cPerk ->
        val accent = Color(cPerk.accentHex)
        val tierCost = UpgradeCosts.gearTier(cPerk.tier)
        val starCost = UpgradeCosts.gearStar(cPerk.starLevel)
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
                Text(cPerk.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
                Text(cPerk.description.ifBlank { "Forged Perk • Tier ${cPerk.tier}/${cPerk.maxTier} • ${cPerk.starLevel}★" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Surface(shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = 0.2f)) {
                Text(
                  text = "T${cPerk.tier}/${cPerk.maxTier} • ${cPerk.starLevel}★",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = accent,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { onUpgradeCustomPerkTier(cPerk.id) },
                enabled = cPerk.tier < cPerk.maxTier && totalBlueEssence >= tierCost,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_cperk_tier_${cPerk.id}"),
              ) {
                Text(if (cPerk.tier >= cPerk.maxTier) "T${cPerk.maxTier} MAX" else "Tier +1 ($tierCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
              OutlinedButton(
                onClick = { onUpgradeCustomPerkStar(cPerk.id) },
                enabled = cPerk.starLevel < 10 && totalBlueEssence >= starCost,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_cperk_star_${cPerk.id}"),
              ) {
                Text(if (cPerk.starLevel >= 10) "10★ MAX" else "Star +1 ($starCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    item {
      Text("COLORED BLAST CORES (BLUE, PURPLE, GREEN, YELLOW, RED)", style = MaterialTheme.typography.titleMedium, color = Color(0xFFE040FB))
    }

    items(BlastCoreColor.entries, key = { it.id }) { core ->
      val count = progression.blastCoreCount(core)
      val lvl = progression.blastCoreLevel(core)
      val accent = Color(core.colorHex)
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .combinedClickable(
            enabled = count >= 1L && lvl < 20,
            onClick = { onUpgradeBlastCore(core) },
            onLongClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onUpgradeBlastCore(core)
            },
          )
          .border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
              text = "${core.displayName.uppercase()} • LV $lvl/20 • OWNED: $count",
              style = MaterialTheme.typography.labelSmall,
              color = accent,
              fontWeight = FontWeight.Bold,
            )
            Text(core.upgradedDomain, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(core.effectPerLevel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          OutlinedButton(
            onClick = { onUpgradeBlastCore(core) },
            enabled = count >= 1L && lvl < 20,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(48.dp).testTag("upgrade_core_${core.id}"),
          ) {
            Text(if (lvl >= 20) "MAX" else "Fuse 1 Core", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AreasAndAgingTab(
  progression: OperatorProgressionState,
  totalBlueEssence: Long,
  onSelectArea: (String) -> Unit,
  onUpgradeAreaFeature: (String, String) -> Unit,
  onAdvanceAreaAge: (String, Int) -> Unit,
  onUpgradeAreaStar: (String) -> Unit = {},
  onOpenForgeMap: () -> Unit = {},
  onOpenForgeWeapon: () -> Unit = {},
  onDeployArea: (String) -> Unit = {},
) {
  val haptic = LocalHapticFeedback.current
  val activeSpec = progression.activeAreaSpec()
  val activeArea = progression.areaState(activeSpec.id)
  val upgradedMaps = progression.upgradedAreaCount()
  val mapTierPts = progression.totalAreaTierPoints()

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("areas_aging_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(activeArea.currentEra.colorHex).copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "ACTIVE AREA: ${activeSpec.name}",
            style = MaterialTheme.typography.titleMedium,
            color = Color(activeSpec.accentHex),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("active_area_banner"),
          )
          Text(
            text = "Age: ${activeArea.ageYears}/99 yrs • ${activeArea.currentEra.label} • ${activeArea.starLevel}★ • Rebirths: ${activeArea.rebirthCycles}",
            style = MaterialTheme.typography.labelMedium,
            color = Color(activeArea.currentEra.colorHex),
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "30-Map Mod Synergy: $upgradedMaps/${EssenceResonanceBonuses.MAX_SYNERGY_MAPS} Upgraded Maps ($mapTierPts Tier Pts) • ${progression.customWeapons.size} Custom Weapons",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF00E676),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("areas_30_map_synergy_badge"),
          )
          LinearProgressIndicator(
            progress = { (upgradedMaps.toFloat() / EssenceResonanceBonuses.MAX_SYNERGY_MAPS.toFloat()).coerceIn(0f, 1f) },
            color = Color(0xFF00E676),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
          )
          Text(
            text = activeSpec.storyEraNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onOpenForgeMap,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAB40), contentColor = Color.Black),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("areas_forge_map_btn"),
            ) {
              Text("+ Forge Map", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = onOpenForgeWeapon,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("areas_forge_weapon_btn"),
            ) {
              Text("+ Forge Weapon", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    items(progression.allPlayableAreas().distinctBy { it.id }, key = { it.id }) { spec ->
      val area = progression.areaState(spec.id)
      val isSelected = progression.activeAreaId == spec.id
      val accent = Color(spec.accentHex)
      val eraColor = Color(area.currentEra.colorHex)
      val starCost = UpgradeCosts.areaStar(area.starLevel)

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .combinedClickable(
            onClick = { onSelectArea(spec.id) },
            onLongClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onSelectArea(spec.id)
            },
          )
          .border(if (isSelected) 2.dp else 1.dp, if (isSelected) accent else MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text(spec.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
              Text(spec.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(8.dp), color = eraColor.copy(alpha = 0.2f)) {
              Text(
                text = "${area.ageYears}y • ${area.starLevel}★ • C${area.rebirthCycles}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = eraColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("area_age_${spec.id}"),
              )
            }
          }

          Text(
            text = "Story Note: ${spec.storyEraNote} • Traits: ${spec.uniqueTraits}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF00E5FF),
          )

          LinearProgressIndicator(
            progress = { (area.ageYears / 100f).coerceIn(0f, 1f) },
            color = eraColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { onSelectArea(spec.id) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) Color(0xFF00E676) else accent,
                contentColor = Color.Black,
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("select_area_${spec.id}"),
            ) {
              Text(if (isSelected) "ACTIVE AREA" else "Mount Area", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = { onAdvanceAreaAge(spec.id, 18) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("age_area_${spec.id}"),
            ) {
              Text("Age +18 yrs", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = { onUpgradeAreaStar(spec.id) },
              enabled = area.starLevel < StarRankMeta.MAX_STARS,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_area_star_${spec.id}"),
            ) {
              Text(if (area.starLevel >= StarRankMeta.MAX_STARS) "${StarRankMeta.MAX_STARS}★ MAX" else "★+1 ($starCost B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AreaUpgradeButton("Grid T${area.powerGridTier}/25", area.powerGridTier, totalBlueEssence, "area_up_grid_${spec.id}") {
              onUpgradeAreaFeature(spec.id, "power_grid")
            }
            AreaUpgradeButton("Turret T${area.defenseTurretTier}/25", area.defenseTurretTier, totalBlueEssence, "area_up_turret_${spec.id}") {
              onUpgradeAreaFeature(spec.id, "turrets")
            }
            AreaUpgradeButton("Traps T${area.trapDensityTier}/25", area.trapDensityTier, totalBlueEssence, "area_up_traps_${spec.id}") {
              onUpgradeAreaFeature(spec.id, "traps")
            }
            AreaUpgradeButton("Loot T${area.lootRichnessTier}/25", area.lootRichnessTier, totalBlueEssence, "area_up_loot_${spec.id}") {
              onUpgradeAreaFeature(spec.id, "loot")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.AreaUpgradeButton(
  label: String,
  tier: Int,
  totalBlueEssence: Long,
  tag: String,
  onClick: () -> Unit,
) {
  val cost = UpgradeCosts.areaFeatureTier(tier)
  OutlinedButton(
    onClick = onClick,
    enabled = tier < 25,
    shape = RoundedCornerShape(8.dp),
    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    modifier = Modifier.weight(1f).height(48.dp).testTag(tag),
  ) {
    Text(
      text = if (tier >= 25) "$label\nMAX" else "$label\n${cost}B",
      style = MaterialTheme.typography.labelSmall,
      maxLines = 2,
    )
  }
}

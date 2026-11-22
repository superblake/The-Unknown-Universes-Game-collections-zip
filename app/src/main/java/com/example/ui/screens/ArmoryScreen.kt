package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.R
import com.example.data.model.AchievementState
import com.example.data.model.ArmoryItem
import com.example.data.model.BattlePassVersion
import com.example.data.model.BlastCoreColor
import com.example.data.model.ChallengeState
import com.example.data.model.CharacterCustomization
import com.example.data.model.CollectionDifficulty
import com.example.data.model.EquipmentSlotType
import com.example.data.model.GearSlot
import com.example.data.model.LevelCustomization
import com.example.data.model.MissionScope
import com.example.data.model.MissionState
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.StarRankMeta
import com.example.data.model.UpgradeCosts
import com.example.data.model.WeaponCustomization

enum class ArmorySection(val label: String, val tag: String) {
  MODS("Mods", "mods"),
  OUTFITS("Characters", "outfits"),
  WEAPONS("Weapons", "weapons"),
  GEAR("Gear", "gear"),
  EQUIPMENT("Equipment", "equipment"),
  SHARDS_CORES("Shards & Cores", "shards_cores"),
  AREAS("Areas & Aging", "areas"),
  SKILLS("Skill Tree", "skills"),
  CHARACTER("Character", "character"),
  LEVELS("Levels", "levels"),
  MISSIONS("Missions", "missions"),
  PASS_CHALLENGES("Pass & Challenges", "pass"),
  STORE("Store", "store"),
  ACHIEVEMENTS("Achievements", "achievements"),
}

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
  progression: OperatorProgressionState = OperatorProgressionState(),
  challenges: List<ChallengeState> = ProgressionCatalog.challenges.map { ChallengeState(it, 0L, false) },
  achievements: List<AchievementState> = emptyList(),
  missions: List<MissionState> = emptyList(),
  activeWorldId: String = "sector_zero",
  activeAreaId: String = "area_empire_city",
  activeRig: RigAsset? = null,
  onUpdateWeaponCustom: ((WeaponCustomization) -> WeaponCustomization) -> Unit = {},
  onUpdateCharacterCustom: ((CharacterCustomization) -> CharacterCustomization) -> Unit = {},
  onUpdateLevelCustom: ((LevelCustomization) -> LevelCustomization) -> Unit = {},
  onSelectOrUnlockPass: (BattlePassVersion) -> Unit = {},
  onClaimPassTier: (BattlePassVersion, Int) -> Unit = { _, _ -> },
  onClaimChallenge: (String) -> Unit = {},
  onRefreshChallenges: () -> Unit = {},
  onUpgradeEquipment: (String) -> Unit = {},
  onUpgradeEquipmentTier: (String) -> Unit = {},
  onPrestigeEquipment: (String) -> Unit = {},
  onEquipEquipment: (String, String) -> Unit = { _, _ -> },
  onUpgradeGear: (GearSlot) -> Unit = {},
  onUpgradeGearTier: (GearSlot) -> Unit = {},
  onPrestigeGear: () -> Unit = {},
  onUnlockSkill: (String) -> Unit = {},
  onUpgradeWeaponTier: (String) -> Unit = {},
  onUpgradeWonderTier: (String) -> Unit = {},
  onPrestigePlayer: () -> Unit = {},
  onPrestigeWeapon: (String) -> Unit = {},
  onPurchaseStoreItem: (String) -> Unit = {},
  onClaimAchievement: (String) -> Unit = {},
  onToggleMultiWeaponEquip: (String) -> Unit = {},
  onToggleMultiWonderEquip: (String) -> Unit = {},
  onToggleMultiEquipmentEquip: (EquipmentSlotType, String) -> Unit = { _, _ -> },
  onEquipOutfit: (String) -> Unit = {},
  onUpgradeOutfit: (String) -> Unit = {},
  onUpgradeOutfitSubSystem: (String, String) -> Unit = { _, _ -> },
  onSelectArea: (String) -> Unit = {},
  onUpgradeAreaFeature: (String, String) -> Unit = { _, _ -> },
  onAdvanceAreaAge: (String, Int) -> Unit = { _, _ -> },
  onSpendBlastShards: () -> Unit = {},
  onUpgradeVacuumBot: () -> Unit = {},
  onToggleVacuumAutoUpgrade: () -> Unit = {},
  onUpgradePerkDrink: (String) -> Unit = {},
  onUpgradeBlastCore: (BlastCoreColor) -> Unit = {},
  onExecuteTradeOffer: (String) -> Unit = {},
  onClaimMission: (String) -> Unit = {},
  onDeployMissionTarget: (MissionScope, String) -> Unit = { _, _ -> },
  onUpgradeArmoryItemStar: (String) -> Unit = {},
  onUpgradeCharacterTier: () -> Unit = {},
  onUpgradeCharacterStar: () -> Unit = {},
  onUpgradeOutfitStar: (String) -> Unit = {},
  onUpgradeWeaponStar: (String) -> Unit = {},
  onUpgradeWonderStar: (String) -> Unit = {},
  onUpgradeGearStar: (GearSlot) -> Unit = {},
  onUpgradeEquipmentStar: (String) -> Unit = {},
  onUpgradeSkillStar: (String) -> Unit = {},
  onUpgradeVacuumBotStar: () -> Unit = {},
  onUpgradeAreaStar: (String) -> Unit = {},
  onUpgradeOutfitRarity: (String) -> Unit = {},
  onRecruitCharacterShard: (String?) -> Unit = {},
  onHuntHeroFrontier: () -> Unit = {},
  onSelectNgPlusTier: (Int) -> Unit = {},
  onCompleteNgPlusRun: () -> Unit = {},
  onSelectCollectionDifficulty: (String, CollectionDifficulty) -> Unit = { _, _ -> },
) {
  val categories = listOf("all", "perk", "power", "upgrade", "consumable")
  var selectedSection by remember { mutableStateOf(ArmorySection.MODS) }
  var selectedCategory by remember { mutableStateOf("all") }
  var showCreateDialog by remember { mutableStateOf(false) }

  val filtered =
    if (selectedCategory == "all") armory else armory.filter { it.category == selectedCategory }
  val sections = ArmorySection.entries
  val curIdx = sections.indexOf(selectedSection).coerceAtLeast(0)
  val resonance = remember(
    totalEssence,
    progression.redEssence,
    progression.yellowEssence,
    progression.greenEssence,
    progression.combinedEssence,
    progression.blastShards,
  ) { progression.essenceResonance(totalEssence) }
  val powerLight = progression.computePowerAndLightLevel(totalEssence)

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp, top = 12.dp)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("MULTIVERSE ARMORY", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
              text = "PWR ${powerLight.powerLevel} • LT ${powerLight.lightLevel} • Lv.${progression.playerLevel} • ${progression.activeOutfitSpec().nameWithVersion}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF00E5FF).copy(alpha = 0.16f),
          ) {
            Text(
              text = "$totalEssence BLUE ESSENCE",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF00E5FF),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).testTag("armory_blue_essence_badge"),
            )
          }
        }

        Text(
          text = "Red ${progression.redEssence} • Yellow ${progression.yellowEssence} • Green ${progression.greenEssence} • Combined ${progression.combinedEssence} • ${resonance.summary}",
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFE040FB),
          fontWeight = FontWeight.SemiBold,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = { selectedSection = sections[(curIdx - 1 + sections.size) % sections.size] },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(48.dp).testTag("armory_prev_section_btn"),
          ) {
            Text("< Prev", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          }
          Text(
            text = "${curIdx + 1}/${sections.size}: ${selectedSection.label.uppercase()}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF),
          )
          OutlinedButton(
            onClick = { selectedSection = sections[(curIdx + 1) % sections.size] },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(48.dp).testTag("armory_next_section_btn"),
          ) {
            Text("Next >", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          }
        }

        val topRowSections = remember(sections) {
          listOf(
            ArmorySection.MODS,
            ArmorySection.OUTFITS,
            ArmorySection.WEAPONS,
            ArmorySection.CHARACTER,
            ArmorySection.LEVELS,
            ArmorySection.MISSIONS,
            ArmorySection.PASS_CHALLENGES,
          )
        }
        val bottomRowSections = remember(sections) {
          listOf(
            ArmorySection.GEAR,
            ArmorySection.EQUIPMENT,
            ArmorySection.SKILLS,
            ArmorySection.SHARDS_CORES,
            ArmorySection.AREAS,
            ArmorySection.STORE,
            ArmorySection.ACHIEVEMENTS,
          )
        }
        Column(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            topRowSections.forEach { sec ->
              FilterChip(
                selected = selectedSection == sec,
                onClick = { selectedSection = sec },
                label = { Text(sec.label, fontWeight = if (selectedSection == sec) FontWeight.Bold else FontWeight.Medium) },
                modifier = Modifier.height(48.dp).testTag("armory_tab_${sec.tag}"),
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bottomRowSections.forEach { sec ->
              FilterChip(
                selected = selectedSection == sec,
                onClick = { selectedSection = sec },
                label = { Text(sec.label, fontWeight = if (selectedSection == sec) FontWeight.Bold else FontWeight.Medium) },
                modifier = Modifier.height(48.dp).testTag("armory_tab_${sec.tag}"),
              )
            }
          }
        }
      }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (selectedSection) {
        ArmorySection.MODS -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
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
                    modifier = Modifier.heightIn(min = 48.dp).testTag("armory_filter_$cat"),
                  )
                }
              }
            }

            items(filtered, key = { it.id }) { item ->
              ArmoryModuleCard(
                item = item,
                totalEssence = totalEssence,
                starLevel = progression.armoryItemStar(item.id),
                onToggleEquip = { onToggleEquip(item) },
                onUpgrade = { onUpgradeItem(item) },
                onUpgradeStar = { onUpgradeArmoryItemStar(item.id) },
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

        ArmorySection.OUTFITS -> OutfitsMenuTab(
          progression = progression,
          totalBlueEssence = totalEssence,
          onEquipOutfit = onEquipOutfit,
          onUpgradeOutfit = onUpgradeOutfit,
          onUpgradeOutfitSubSystem = onUpgradeOutfitSubSystem,
          onUpgradeOutfitStar = onUpgradeOutfitStar,
          onUpgradeOutfitRarity = onUpgradeOutfitRarity,
          onRecruitCharacterShard = onRecruitCharacterShard,
          onHuntHeroFrontier = onHuntHeroFrontier,
        )

        ArmorySection.WEAPONS -> WeaponCustomizationTab(
          progression = progression,
          onUpdateWeaponCustom = onUpdateWeaponCustom,
          totalBlueEssence = totalEssence,
          onUpgradeWeaponTier = onUpgradeWeaponTier,
          onUpgradeWeaponStar = onUpgradeWeaponStar,
          onUpgradeWonderTier = onUpgradeWonderTier,
          onUpgradeWonderStar = onUpgradeWonderStar,
          onPrestigeWeapon = onPrestigeWeapon,
          onToggleMultiWeaponEquip = onToggleMultiWeaponEquip,
          onToggleMultiWonderEquip = onToggleMultiWonderEquip,
        )

        ArmorySection.SHARDS_CORES -> ShardsPerksAndCoresTab(
          progression = progression,
          totalBlueEssence = totalEssence,
          onSpendBlastShards = onSpendBlastShards,
          onUpgradeVacuumBot = onUpgradeVacuumBot,
          onToggleVacuumAutoUpgrade = onToggleVacuumAutoUpgrade,
          onUpgradePerkDrink = onUpgradePerkDrink,
          onUpgradeBlastCore = onUpgradeBlastCore,
          onUpgradeVacuumBotStar = onUpgradeVacuumBotStar,
        )

        ArmorySection.AREAS -> AreasAndAgingTab(
          progression = progression,
          totalBlueEssence = totalEssence,
          onSelectArea = onSelectArea,
          onUpgradeAreaFeature = onUpgradeAreaFeature,
          onAdvanceAreaAge = onAdvanceAreaAge,
          onUpgradeAreaStar = onUpgradeAreaStar,
        )

        ArmorySection.CHARACTER -> CharacterCustomizationTab(
          progression = progression,
          activeRig = activeRig,
          onUpdateCharacterCustom = onUpdateCharacterCustom,
          onPrestigePlayer = onPrestigePlayer,
          totalBlueEssence = totalEssence,
          onUpgradeCharacterTier = onUpgradeCharacterTier,
          onUpgradeCharacterStar = onUpgradeCharacterStar,
          onEquipOutfit = onEquipOutfit,
        )

        ArmorySection.LEVELS -> LevelCustomizationTab(
          progression = progression,
          onUpdateLevelCustom = onUpdateLevelCustom,
          activeWorldId = activeWorldId,
          onSelectNgPlusTier = onSelectNgPlusTier,
          onCompleteNgPlusRun = onCompleteNgPlusRun,
          onSelectCollectionDifficulty = onSelectCollectionDifficulty,
        )

        ArmorySection.MISSIONS -> MissionsTab(
          missions = missions,
          activeWorldId = activeWorldId,
          activeAreaId = activeAreaId,
          onClaimMission = onClaimMission,
          onDeployTarget = onDeployMissionTarget,
        )

        ArmorySection.PASS_CHALLENGES -> PassAndChallengesTab(
          progression = progression,
          challenges = challenges,
          totalEssence = totalEssence,
          onSelectOrUnlockPass = onSelectOrUnlockPass,
          onClaimPassTier = onClaimPassTier,
          onClaimChallenge = onClaimChallenge,
          onRefreshChallenges = onRefreshChallenges,
        )

        ArmorySection.GEAR -> GearCustomizationTab(
          progression = progression,
          onUpgradeGear = onUpgradeGear,
          onPrestigeGear = onPrestigeGear,
          totalBlueEssence = totalEssence,
          onUpgradeGearTier = onUpgradeGearTier,
          onUpgradeGearStar = onUpgradeGearStar,
        )

        ArmorySection.EQUIPMENT -> EquipmentCustomizationTab(
          progression = progression,
          onUpgradeEquipment = onUpgradeEquipment,
          onPrestigeEquipment = onPrestigeEquipment,
          onEquipEquipment = onEquipEquipment,
          totalBlueEssence = totalEssence,
          onUpgradeEquipmentTier = onUpgradeEquipmentTier,
          onToggleMultiEquipmentEquip = onToggleMultiEquipmentEquip,
          onUpgradeEquipmentStar = onUpgradeEquipmentStar,
        )

        ArmorySection.SKILLS -> SkillTreeTab(
          progression = progression,
          totalEssence = totalEssence,
          onUnlockSkill = onUnlockSkill,
          onUpgradeSkillStar = onUpgradeSkillStar,
        )

        ArmorySection.STORE -> BlueEssenceStoreTab(
          progression = progression,
          totalBlueEssence = totalEssence,
          onPurchaseItem = onPurchaseStoreItem,
          onExecuteTradeOffer = onExecuteTradeOffer,
        )

        ArmorySection.ACHIEVEMENTS -> AchievementsMenuTab(
          achievements = achievements,
          onClaimAchievement = onClaimAchievement,
        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArmoryModuleCard(
  item: ArmoryItem,
  totalEssence: Long,
  starLevel: Int,
  onToggleEquip: () -> Unit,
  onUpgrade: () -> Unit,
  onUpgradeStar: () -> Unit,
  onDelete: (() -> Unit)?,
) {
  val haptic = LocalHapticFeedback.current
  val accent = parseHexSafe(item.accentColorHex, MaterialTheme.colorScheme.primary)
  val effectiveMaxTier = maxOf(item.maxTier, 25L)
  val upgradeCost = UpgradeCosts.armoryModTier(item.tier.toInt())
  val starCost = UpgradeCosts.armoryModStar(starLevel)
  val canUpgrade = item.tier < effectiveMaxTier && totalEssence >= upgradeCost
  val canUpgradeStar = starLevel < StarRankMeta.MAX_STARS && totalEssence >= starCost
  val tierMeta = ProgressionCatalog.tierMeta(item.tier.toInt())
  val starMeta = ProgressionCatalog.starMeta(starLevel)

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .combinedClickable(
        onClick = onToggleEquip,
        onLongClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onToggleEquip()
        },
      )
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
              text = "${item.universeTag} • $starLevel★ (${starMeta.label})",
              style = MaterialTheme.typography.labelSmall,
              color = Color(starMeta.colorHex),
              fontWeight = FontWeight.Bold,
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
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
              Icon(Icons.Filled.Delete, contentDescription = "Delete custom module", tint = MaterialTheme.colorScheme.error)
            }
          }
          IconButton(
            onClick = onToggleEquip,
            modifier = Modifier.size(48.dp).testTag("equip_btn_${item.id}"),
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
            text = "Tier ${item.tier}/$effectiveMaxTier (${tierMeta.label}) • Power ${item.powerValue}" +
              if (item.cooldownSec > 0) " • CD ${item.cooldownSec}s" else "",
            style = MaterialTheme.typography.labelSmall,
            color = Color(tierMeta.colorHex),
          )
          Spacer(Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { item.tier.toFloat() / effectiveMaxTier.toFloat().coerceAtLeast(1f) },
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(6.dp),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = onUpgrade,
            enabled = canUpgrade,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.heightIn(min = 48.dp).testTag("upgrade_btn_${item.id}"),
          ) {
            Text(if (item.tier >= effectiveMaxTier) "MAX" else "$upgradeCost B")
          }
          OutlinedButton(
            onClick = onUpgradeStar,
            enabled = canUpgradeStar,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.heightIn(min = 48.dp).testTag("upgrade_star_btn_${item.id}"),
          ) {
            Text(if (starLevel >= StarRankMeta.MAX_STARS) "10★" else "★+1 ($starCost B)")
          }
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
            onValueChange = { powerStr = it.filter { c -> c.isDigit() }.take(4) },
            label = { Text("Power (1-1500)") },
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

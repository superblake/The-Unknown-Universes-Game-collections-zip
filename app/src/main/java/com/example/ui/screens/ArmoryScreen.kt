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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import java.util.Locale
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
import com.example.data.local.AppSettings
import com.example.data.local.GameStrings
import com.example.data.local.LocalGameStrings
import com.example.data.model.AchievementState
import com.example.data.model.ArmoryItem
import com.example.data.model.BattlePassVersion
import com.example.data.model.BlastCoreColor
import com.example.data.model.ChallengeState
import com.example.data.model.CharacterCustomization
import com.example.data.model.CollectionDifficulty
import com.example.data.model.CuratedCustomMapsCatalog
import com.example.data.model.CustomMapGameCategory
import com.example.data.model.EquipmentSlotType
import com.example.data.model.EssenceResonanceBonuses
import com.example.data.model.GearSlot
import com.example.data.model.LevelCustomization
import com.example.data.model.MissionScope
import com.example.data.model.MissionState
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.StarRankMeta
import com.example.data.model.UpgradeCosts
import com.example.data.model.UserCustomGameMapSpec
import com.example.data.model.UserCustomWeaponSpec
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
  WORKSHOP_BO3("BO3 & Workshop", "workshop_bo3");

  fun localizedLabel(s: GameStrings): String = when (this) {
    MODS -> s.armoryMods
    OUTFITS -> s.armoryCharacters
    WEAPONS -> s.armoryWeapons
    GEAR -> s.armoryGear
    EQUIPMENT -> s.armoryEquipment
    SHARDS_CORES -> s.armoryShardsCores
    AREAS -> s.armoryAreas
    SKILLS -> s.armorySkills
    CHARACTER -> s.armoryCharacter
    LEVELS -> s.armoryLevels
    MISSIONS -> s.armoryMissions
    PASS_CHALLENGES -> s.armoryPass
    STORE -> s.armoryStore
    ACHIEVEMENTS -> s.armoryAchievements
    WORKSHOP_BO3 -> s.armoryWorkshop
  }
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
  onUpdateWonderCustom: ((com.example.data.model.WonderWeaponCustomization) -> com.example.data.model.WonderWeaponCustomization) -> Unit = {},
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
  onApplyProtocolCode: (String) -> Unit = {},
  onBoostAllTiers: (Int) -> Unit = {},
  onMaxAllTiers: () -> Unit = {},
  onMaxAllStars: () -> Unit = {},
  onGrantEssenceBonus: () -> Unit = {},
  onImportBo3Preset: () -> Unit = {},
  onDeployArea: (String) -> Unit = {},
  onUpdateCollectionCustomMode: ((com.example.data.model.CollectionCustomModeConfig) -> com.example.data.model.CollectionCustomModeConfig) -> Unit = {},
  onDeployCollectionCustomMode: () -> Unit = {},
  onForgeCustomMap: (
    title: String,
    mapCode: String,
    category: CustomMapGameCategory,
    description: String,
    zones: List<String>,
    wonderWeapon: String,
    accentHex: Long,
    deployNow: Boolean,
  ) -> Unit = { _, _, _, _, _, _, _, _ -> },
  onDeleteCustomMap: (String) -> Unit = {},
  onForgeCustomWeapon: (
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
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
  onDeleteCustomWeapon: (String) -> Unit = {},
  onUpgradeCustomPerkTier: (String) -> Unit = {},
  onUpgradeCustomPerkStar: (String) -> Unit = {},
  onUpgradeCustomGearTier: (String) -> Unit = {},
  onUpgradeCustomGearStar: (String) -> Unit = {},
  onEquipCustomGear: (GearSlot, String) -> Unit = { _, _ -> },
  onUpgradeCustomEquipmentTier: (String) -> Unit = {},
  onUpgradeCustomEquipmentStar: (String) -> Unit = {},
  settings: AppSettings = AppSettings(),
  persistedSectionTag: String? = null,
  onPersistSectionTag: ((String) -> Unit)? = null,
  persistedCategory: String? = null,
  onPersistCategory: ((String) -> Unit)? = null,
) {
  val gameStrings = LocalGameStrings.current
  val categories = listOf("all", "perk", "power", "upgrade", "consumable", "workshop_bo3")
  var localSection by remember { mutableStateOf(ArmorySection.MODS) }
  var localCategory by remember { mutableStateOf("all") }
  val selectedSection = if (onPersistSectionTag != null && persistedSectionTag != null) {
    ArmorySection.entries.firstOrNull { it.tag == persistedSectionTag } ?: ArmorySection.MODS
  } else {
    localSection
  }
  val setSelectedSection: (ArmorySection) -> Unit = { next ->
    localSection = next
    onPersistSectionTag?.invoke(next.tag)
  }
  val selectedCategory = if (onPersistCategory != null && persistedCategory != null) {
    persistedCategory.lowercase().let { if (it in categories) it else "all" }
  } else {
    localCategory
  }
  val setSelectedCategory: (String) -> Unit = { next ->
    localCategory = next
    onPersistCategory?.invoke(next)
  }
  var showCreateDialog by remember { mutableStateOf(false) }
  var showCodesDialog by remember { mutableStateOf(false) }
  var showCustomModeDialog by remember { mutableStateOf(false) }
  var showForgeCustomMapDialog by remember { mutableStateOf(false) }
  var forgeMapInitialCategory by remember { mutableStateOf(CustomMapGameCategory.BO3) }
  var showForgeCustomWeaponDialog by remember { mutableStateOf(false) }
  var forgeWeaponInitialCategory by remember { mutableStateOf(CustomMapGameCategory.BO3) }
  var customMapFilter by remember { mutableStateOf("all") }

  val filtered = when (selectedCategory) {
    "all" -> armory
    "workshop_bo3" -> armory.filter { it.isCustom || it.universeTag.contains("BO3", ignoreCase = true) || it.universeTag.contains("Workshop", ignoreCase = true) }
    else -> armory.filter { it.category == selectedCategory }
  }
  val sections = ArmorySection.entries
  val curIdx = sections.indexOf(selectedSection).coerceAtLeast(0)
  val resonance = remember(
    totalEssence,
    progression,
    armory,
    settings.selectedRegionId,
    settings.selectedServerId,
  ) {
    progression.comprehensiveBonuses(
      blueEssence = totalEssence,
      armoryItems = armory,
      region = settings.activeRegion,
      server = settings.activeServer,
    )
  }
  val powerLight = progression.computePowerAndLightLevel(totalEssence)
  val upgradedMaps = progression.upgradedAreaCount()

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
            Text(gameStrings.armoryTitle, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
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
          text = "R:${progression.redEssence} Y:${progression.yellowEssence} G:${progression.greenEssence} • ${resonance.summary} • $upgradedMaps/${EssenceResonanceBonuses.MAX_SYNERGY_MAPS} Maps • ${settings.activeRegion.shortTag}/${settings.activeServer.code}",
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
            onClick = { setSelectedSection(sections[(curIdx - 1 + sections.size) % sections.size]) },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(48.dp).testTag("armory_prev_section_btn"),
          ) {
            Text("< Prev", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          }
          Button(
            onClick = { showCodesDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE), contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(48.dp).testTag("tier_codes_menu_btn"),
          ) {
            Text("⚡ Codes & Boost", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = { setSelectedSection(sections[(curIdx + 1) % sections.size]) },
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
            ArmorySection.WORKSHOP_BO3,
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
                onClick = { setSelectedSection(sec) },
                label = { Text(sec.localizedLabel(gameStrings), fontWeight = if (selectedSection == sec) FontWeight.Bold else FontWeight.Medium) },
                modifier = Modifier.height(48.dp).testTag("armory_tab_${sec.tag}"),
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bottomRowSections.forEach { sec ->
              FilterChip(
                selected = selectedSection == sec,
                onClick = { setSelectedSection(sec) },
                label = { Text(sec.localizedLabel(gameStrings), fontWeight = if (selectedSection == sec) FontWeight.Bold else FontWeight.Medium) },
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
                    onClick = { setSelectedCategory(cat) },
                    label = {
                      Text(
                        text = when (cat) {
                          "all" -> "All (${armory.size})"
                          "perk" -> "Perks"
                          "power" -> "Powers"
                          "upgrade" -> "Upgrades"
                          "workshop_bo3" -> "BO3 / Workshop"
                          else -> "Consumables"
                        }
                      )
                    },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("armory_filter_$cat"),
                  )
                }
              }
            }
            item {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF00E676).copy(alpha = 0.1f),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, Color(0xFF00E676).copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
              ) {
                Text(
                  text = "Mod 30-Map Synergy: $upgradedMaps/${EssenceResonanceBonuses.MAX_SYNERGY_MAPS} Maps Upgraded • ${resonance.extendedSourcesSummary.ifBlank { resonance.summary }}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF00E676),
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).testTag("mods_30_map_synergy_banner"),
                )
              }
            }

            items(filtered.distinctBy { it.id }, key = { it.id }) { item ->
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
          onUpdateWonderCustom = onUpdateWonderCustom,
          totalBlueEssence = totalEssence,
          onUpgradeWeaponTier = onUpgradeWeaponTier,
          onUpgradeWeaponStar = onUpgradeWeaponStar,
          onUpgradeWonderTier = onUpgradeWonderTier,
          onUpgradeWonderStar = onUpgradeWonderStar,
          onPrestigeWeapon = onPrestigeWeapon,
          onToggleMultiWeaponEquip = onToggleMultiWeaponEquip,
          onToggleMultiWonderEquip = onToggleMultiWonderEquip,
          onOpenForgeWeapon = {
            forgeWeaponInitialCategory = CustomMapGameCategory.BO3
            showForgeCustomWeaponDialog = true
          },
          onDeleteCustomWeapon = onDeleteCustomWeapon,
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
          onUpgradeCustomPerkTier = onUpgradeCustomPerkTier,
          onUpgradeCustomPerkStar = onUpgradeCustomPerkStar,
          armoryItems = armory,
          region = settings.activeRegion,
          server = settings.activeServer,
        )

        ArmorySection.AREAS -> AreasAndAgingTab(
          progression = progression,
          totalBlueEssence = totalEssence,
          onSelectArea = onSelectArea,
          onUpgradeAreaFeature = onUpgradeAreaFeature,
          onAdvanceAreaAge = onAdvanceAreaAge,
          onUpgradeAreaStar = onUpgradeAreaStar,
          onOpenForgeMap = {
            forgeMapInitialCategory = CustomMapGameCategory.ARKHAM
            showForgeCustomMapDialog = true
          },
          onOpenForgeWeapon = {
            forgeWeaponInitialCategory = CustomMapGameCategory.ARKHAM
            showForgeCustomWeaponDialog = true
          },
          onDeployArea = onDeployArea,
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
          onUpgradeCustomGearTier = onUpgradeCustomGearTier,
          onUpgradeCustomGearStar = onUpgradeCustomGearStar,
          onEquipCustomGear = onEquipCustomGear,
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
          onUpgradeCustomEquipmentTier = onUpgradeCustomEquipmentTier,
          onUpgradeCustomEquipmentStar = onUpgradeCustomEquipmentStar,
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

        ArmorySection.WORKSHOP_BO3 -> {
          val workshopItems = remember(armory) {
            armory.filter { it.isCustom || it.universeTag.contains("BO3", ignoreCase = true) || it.universeTag.contains("Workshop", ignoreCase = true) }
          }
          LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("workshop_bo3_section_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                      text = "BO3 MOD TOOLS & WORKSHOP PIPELINE",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF00E5FF),
                    )
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF6200EE).copy(alpha = 0.25f)) {
                      Text("TIER 25 READY", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBB86FC), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                  }
                  Text(
                    text = "Imports Treyarch Black Ops 3 Mod Tools assets (.gdt, .xmodel_export, .xanim_export, .gsc scripts) and Steam Workshop mods with 1:1 weapon models, audio cues, perks, and custom maps up to Tier 25.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                      onClick = onImportBo3Preset,
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.weight(1f).height(48.dp).testTag("import_bo3_curated_btn"),
                    ) {
                      Text("Import BO3 Pack", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                      onClick = { showCodesDialog = true },
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.weight(1f).height(48.dp).testTag("bo3_section_boost_codes_btn"),
                    ) {
                      Text("⚡ Boost & Codes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                  }
                  Button(
                    onClick = { showCustomModeDialog = true },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (progression.collectionCustomMode.enabled) Color(0xFFFF9100) else Color(0xFF6200EE),
                      contentColor = if (progression.collectionCustomMode.enabled) Color.Black else Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("bo3_open_custom_mode_btn"),
                  ) {
                    Text(
                      text = "BO3 / Steam / Collections Custom Mode • ${progression.collectionCustomMode.impactLabel}",
                      fontWeight = FontWeight.Bold,
                      style = MaterialTheme.typography.labelSmall,
                    )
                  }
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    FilterChip(
                      selected = !progression.collectionCustomMode.enabled,
                      onClick = { onUpdateCollectionCustomMode { it.copy(enabled = false) } },
                      label = {
                        Text(
                          text = "Normal Mode",
                          fontWeight = FontWeight.Bold,
                          style = MaterialTheme.typography.labelSmall,
                        )
                      },
                      modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("armory_switch_normal_mode"),
                    )
                    FilterChip(
                      selected = progression.collectionCustomMode.enabled,
                      onClick = { onUpdateCollectionCustomMode { it.copy(enabled = true) } },
                      label = {
                        Text(
                          text = "Custom Mode",
                          fontWeight = FontWeight.Bold,
                          style = MaterialTheme.typography.labelSmall,
                        )
                      },
                      modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("armory_switch_custom_mode"),
                    )
                  }
                }
              }
            }

            item {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00E676).copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = "BO3 MOD TOOLS PLAYABLE MAPS (${com.example.export.Bo3ModToolsImporter.bo3CustomMaps.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676),
                  )
                  LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("bo3_maps_row"),
                  ) {
                    items(com.example.export.Bo3ModToolsImporter.bo3CustomMaps.distinctBy { it.areaId }, key = { it.areaId }) { mapSpec ->
                      val isSelected = activeAreaId == mapSpec.areaId
                      Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF00E676).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                          .width(210.dp)
                          .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF00E676) else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp),
                          ),
                      ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                          Text(
                            text = "${mapSpec.title} (${mapSpec.mapCode})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                          )
                          Text(
                            text = "WW: ${mapSpec.wonderWeapon} • ${mapSpec.description}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                          )
                          Button(
                            onClick = {
                              onSelectArea(mapSpec.areaId)
                              onDeployArea(mapSpec.areaId)
                            },
                            colors = ButtonDefaults.buttonColors(
                              containerColor = Color(0xFF00E676),
                              contentColor = Color(0xFF04140A),
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("bo3_deploy_map_${mapSpec.areaId}"),
                          ) {
                            Text(if (isSelected) "Play Active Map" else "Deploy BO3 Map", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            item {
              val allCustomMaps = remember(progression.customGameMaps, customMapFilter) {
                val merged = (progression.customGameMaps + CuratedCustomMapsCatalog.allCuratedCustomMaps)
                  .distinctBy { it.id }
                when (customMapFilter) {
                  "bo3" -> merged.filter { it.gameCategory == CustomMapGameCategory.BO3 }
                  "collections" -> merged.filter { it.gameCategory != CustomMapGameCategory.BO3 }
                  else -> merged
                }
              }
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, Color(0xFFFFAB40).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "CUSTOM MAPS FORGER (BO3 & GAME COLLECTIONS)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFAB40),
                      )
                      Text(
                        text = "Play curated or user-forged custom maps scoped strictly to BO3 & your Game Collections.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Button(
                      onClick = {
                        forgeMapInitialCategory = CustomMapGameCategory.BO3
                        showForgeCustomMapDialog = true
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFAB40),
                        contentColor = Color.Black,
                      ),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.heightIn(min = 48.dp).testTag("open_forge_custom_map_btn"),
                    ) {
                      Text("+ Forge Map", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                  }

                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                      selected = customMapFilter == "all",
                      onClick = { customMapFilter = "all" },
                      label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                      modifier = Modifier.heightIn(min = 48.dp).testTag("custom_map_filter_all"),
                    )
                    FilterChip(
                      selected = customMapFilter == "bo3",
                      onClick = { customMapFilter = "bo3" },
                      label = { Text("BO3 Custom", style = MaterialTheme.typography.labelSmall) },
                      modifier = Modifier.heightIn(min = 48.dp).testTag("custom_map_filter_bo3"),
                    )
                    FilterChip(
                      selected = customMapFilter == "collections",
                      onClick = { customMapFilter = "collections" },
                      label = { Text("Collections", style = MaterialTheme.typography.labelSmall) },
                      modifier = Modifier.heightIn(min = 48.dp).testTag("custom_map_filter_collections"),
                    )
                  }

                  LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("custom_game_maps_row"),
                  ) {
                    items(allCustomMaps, key = { it.id }) { mapSpec: UserCustomGameMapSpec ->
                      val isActive = activeAreaId == mapSpec.id
                      val mapAccent = Color(mapSpec.accentHex)
                      Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive) mapAccent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                        modifier = Modifier
                          .width(236.dp)
                          .border(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) mapAccent else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp),
                          ),
                      ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                          ) {
                            Surface(
                              shape = RoundedCornerShape(6.dp),
                              color = mapAccent.copy(alpha = 0.2f),
                            ) {
                              Text(
                                text = mapSpec.gameCategory.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = mapAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                              )
                            }
                            if (mapSpec.isUserCreated) {
                              IconButton(
                                onClick = { onDeleteCustomMap(mapSpec.id) },
                                modifier = Modifier.size(48.dp).testTag("custom_map_delete_${mapSpec.id}"),
                              ) {
                                Icon(
                                  imageVector = Icons.Filled.Delete,
                                  contentDescription = "Delete custom map",
                                  tint = MaterialTheme.colorScheme.error,
                                )
                              }
                            }
                          }
                          Text(
                            text = "${mapSpec.title} (${mapSpec.mapCode})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                          )
                          Text(
                            text = "WW: ${mapSpec.wonderWeapon} • ${mapSpec.buildingNames.size} Zones",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                          )
                          Button(
                            onClick = {
                              onSelectArea(mapSpec.id)
                              onDeployArea(mapSpec.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                              containerColor = mapAccent,
                              contentColor = Color.Black,
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("custom_map_deploy_${mapSpec.id}"),
                          ) {
                            Text(
                              text = if (isActive) "Active Custom Map" else "Deploy Custom Map",
                              fontWeight = FontWeight.Bold,
                              style = MaterialTheme.typography.labelSmall,
                            )
                          }
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
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "CUSTOM WEAPONS FORGER (BO3 & GAME COLLECTIONS)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                      )
                      Text(
                        text = "${progression.customWeapons.size} custom-forged weapon(s) ready across Collections & Arena.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Button(
                      onClick = {
                        forgeWeaponInitialCategory = CustomMapGameCategory.BO3
                        showForgeCustomWeaponDialog = true
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black,
                      ),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.heightIn(min = 48.dp).testTag("bo3_open_forge_weapon_btn"),
                    ) {
                      Text("+ Forge Weapon", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                  }
                  if (progression.customWeapons.isNotEmpty()) {
                    LazyRow(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.testTag("custom_game_weapons_row"),
                    ) {
                      items(progression.customWeapons.distinctBy { it.id }, key = { it.id }) { wpn: UserCustomWeaponSpec ->
                        val isEquippedWpn = progression.weaponCustom.equippedWeaponId == wpn.id
                        val wAccent = Color(wpn.defaultTracerHex)
                        Surface(
                          shape = RoundedCornerShape(12.dp),
                          color = if (isEquippedWpn) wAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                          modifier = Modifier
                            .width(232.dp)
                            .border(
                              width = if (isEquippedWpn) 1.5.dp else 1.dp,
                              color = if (isEquippedWpn) wAccent else MaterialTheme.colorScheme.outline,
                              shape = RoundedCornerShape(12.dp),
                            ),
                        ) {
                          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                              modifier = Modifier.fillMaxWidth(),
                              horizontalArrangement = Arrangement.SpaceBetween,
                              verticalAlignment = Alignment.CenterVertically,
                            ) {
                              Text(
                                text = wpn.gameCategory.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = wAccent,
                              )
                              IconButton(
                                onClick = { onDeleteCustomWeapon(wpn.id) },
                                modifier = Modifier.size(48.dp).testTag("bo3_delete_wpn_${wpn.id}"),
                              ) {
                                Icon(
                                  imageVector = Icons.Filled.Delete,
                                  contentDescription = "Delete custom weapon",
                                  tint = MaterialTheme.colorScheme.error,
                                )
                              }
                            }
                            Text(wpn.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(
                              text = "${wpn.damageTypeId.uppercase()} • DMG ${wpn.baseDamage} • Spread ${wpn.baseSpreadCount} • P${wpn.basePierce}",
                              style = MaterialTheme.typography.labelSmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              maxLines = 1,
                            )
                            Button(
                              onClick = {
                                onUpdateWeaponCustom { wc ->
                                  val nextMulti = (listOf(wpn.id) + wc.equippedWeaponIds).distinct().take(3)
                                  wc.copy(equippedWeaponId = wpn.id, equippedWeaponIds = nextMulti, tracerColorHex = wpn.defaultTracerHex)
                                }
                              },
                              colors = ButtonDefaults.buttonColors(containerColor = wAccent, contentColor = Color.Black),
                              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                              shape = RoundedCornerShape(8.dp),
                              modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("bo3_equip_wpn_${wpn.id}"),
                            ) {
                              Text(if (isEquippedWpn) "Equipped Weapon" else "Equip Custom Weapon", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            if (workshopItems.isEmpty()) {
              item {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surface,
                  modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                ) {
                  Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text("No BO3 Mod Tools / Workshop Items Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tap 'Import BO3 Pack' above to load Treyarch Juggernog, Speed Cola, Double Tap, Widow's Wine, Thundergun, and Wunderwaffe DG-2 with full Tier 25 scaling.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
            } else {
              items(workshopItems.distinctBy { it.id }, key = { it.id }) { item ->
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
          }
        }
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

  if (showCodesDialog) {
    TierBoostCodesDialog(
      onDismiss = { showCodesDialog = false },
      onApplyCode = onApplyProtocolCode,
      onBoostAllTiers = onBoostAllTiers,
      onMaxAllTiers = onMaxAllTiers,
      onMaxAllStars = onMaxAllStars,
      onGrantEssence = onGrantEssenceBonus,
      onImportBo3Pack = onImportBo3Preset,
    )
  }

  if (showCustomModeDialog) {
    CollectionCustomModeDialog(
      initialConfig = progression.collectionCustomMode,
      customGameMaps = progression.customGameMaps,
      customWeapons = progression.customWeapons,
      onOpenForgeMap = { cat ->
        showCustomModeDialog = false
        forgeMapInitialCategory = cat
        showForgeCustomMapDialog = true
      },
      onOpenForgeWeapon = { cat ->
        showCustomModeDialog = false
        forgeWeaponInitialCategory = cat
        showForgeCustomWeaponDialog = true
      },
      onSave = onUpdateCollectionCustomMode,
      onDeploy = onDeployCollectionCustomMode,
      onDismiss = { showCustomModeDialog = false },
    )
  }

  if (showForgeCustomMapDialog) {
    ForgeCustomMapDialog(
      initialCategory = forgeMapInitialCategory,
      onDismiss = { showForgeCustomMapDialog = false },
      onConfirm = { title, code, category, desc, zones, ww, hex, deployNow ->
        onForgeCustomMap(title, code, category, desc, zones, ww, hex, deployNow)
        showForgeCustomMapDialog = false
      },
    )
  }

  if (showForgeCustomWeaponDialog) {
    ForgeCustomWeaponDialog(
      initialCategory = forgeWeaponInitialCategory,
      onDismiss = { showForgeCustomWeaponDialog = false },
      onConfirm = { name, code, category, desc, dmgType, dmg, cadence, spread, pierce, hex, equipNow ->
        onForgeCustomWeapon(name, code, category, desc, dmgType, dmg, cadence, spread, pierce, hex, equipNow)
        showForgeCustomWeaponDialog = false
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

      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Tier ${item.tier}/$effectiveMaxTier (${tierMeta.label}) • Power ${item.powerValue}" +
              if (item.cooldownSec > 0) " • CD ${item.cooldownSec}s" else "",
            style = MaterialTheme.typography.labelSmall,
            color = Color(tierMeta.colorHex),
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "${(item.tier.toFloat() / effectiveMaxTier.toFloat() * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Bold,
          )
        }
        LinearProgressIndicator(
          progress = { item.tier.toFloat() / effectiveMaxTier.toFloat().coerceAtLeast(1f) },
          color = accent,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onUpgrade()
          },
          enabled = canUpgrade,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("upgrade_btn_${item.id}"),
        ) {
          Text(
            text = if (item.tier >= effectiveMaxTier) "TIER MAX ($effectiveMaxTier)" else "Tier ${item.tier + 1} ($upgradeCost B)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
        }

        OutlinedButton(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onUpgradeStar()
          },
          enabled = canUpgradeStar,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("upgrade_star_btn_${item.id}"),
        ) {
          Text(
            text = if (starLevel >= StarRankMeta.MAX_STARS) "STAR MAX (10★)" else "Star ${starLevel + 1}★ ($starCost B)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
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
            onValueChange = { powerStr = it.filter { c -> c.isDigit() }.take(5) },
            label = { Text("Power (1-50000)") },
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

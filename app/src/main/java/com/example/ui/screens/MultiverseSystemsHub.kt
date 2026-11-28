package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MultiverseSystemsCatalog
import com.example.data.model.OperatorProgressionState
import com.example.data.model.PowerTier
import com.example.data.model.ProgressionCatalog
import com.example.data.model.StarRankMeta
import com.example.data.model.NewGamePlusVersion
import com.example.ui.theme.AppIcons
import java.util.Locale

@Composable
fun MultiverseSystemsHubDialog(
  progression: OperatorProgressionState,
  onSelectMagicSpell: (String) -> Unit,
  onUnlockOrUpgradeMagicSpell: (String, String) -> Unit,
  onUpgradeMagicTier: (String) -> Unit,
  onAdjustMagicDifficulty: (Float) -> Unit,
  onToggleMagicOvercharge: () -> Unit,
  onCraftCrystal: (String) -> Unit,
  onUpgradeCrystal: (String, String) -> Unit,
  onSocketCrystal: (String, String?) -> Unit,
  onCustomizePlayerBase: (String?, String?, Long?) -> Unit,
  onUpgradePlayerBaseModule: (String, String) -> Unit,
  onCollectBaseVault: () -> Unit,
  onTriggerOrResolveBaseAttack: (Boolean) -> Unit,
  onConvertCurrencyOrResource: (String) -> Unit,
  onUpgradeTargetWithCurrency: (String, String, String) -> Unit,
  onBoostAllTiers: (Int) -> Unit,
  onBoostAllStars: (Int) -> Unit,
  onBoostEverythingPlusTen: () -> Unit,
  onMaxAllTiers: () -> Unit,
  onMaxAllStars: () -> Unit,
  onGrantDiamondBonus: (Long) -> Unit,
  onDeployArea: (String) -> Unit,
  onDismiss: () -> Unit,
  initialTab: Int = 0,
) {
  var activeTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 5)) }
  val tabs = listOf(
    "Wallet & +10",
    "Systems Upgrader",
    "Crystal Forge",
    "Magic & Difficulty",
    "Base Citadel",
    "Bank & Vault",
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(10.dp)
        .testTag("multiverse_systems_hub_dialog"),
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "MULTIVERSE SYSTEMS HUB",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Gold ${progression.gold} • Diamonds ${progression.diamonds} • TUUCash ${progression.tuuCash} • Caps: T${PowerTier.MAX_TIER} / ${StarRankMeta.MAX_STARS}★ / NG+${NewGamePlusVersion.MAX_NG_PLUS_TIER}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.height(48.dp).testTag("hub_close_btn"),
          ) {
            Text("Close")
          }
        }

        // Top Navigation Tabs
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          tabs.forEachIndexed { idx, label ->
            FilterChip(
              selected = activeTab == idx,
              onClick = { activeTab = idx },
              label = { Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
              modifier = Modifier.heightIn(min = 48.dp).testTag("hub_tab_$idx"),
            )
          }
        }

        // Body Content
        when (activeTab) {
          0 -> HubWalletAndPlusTenTab(
            progression = progression,
            onConvertCurrencyOrResource = onConvertCurrencyOrResource,
            onBoostAllTiers = onBoostAllTiers,
            onBoostAllStars = onBoostAllStars,
            onBoostEverythingPlusTen = onBoostEverythingPlusTen,
            onMaxAllTiers = onMaxAllTiers,
            onMaxAllStars = onMaxAllStars,
            onGrantDiamondBonus = onGrantDiamondBonus,
          )
          1 -> HubSystemsUpgraderTab(
            progression = progression,
            onUpgradeTargetWithCurrency = onUpgradeTargetWithCurrency,
          )
          2 -> HubCrystalForgeTab(
            progression = progression,
            onCraftCrystal = onCraftCrystal,
            onUpgradeCrystal = onUpgradeCrystal,
            onSocketCrystal = onSocketCrystal,
          )
          3 -> HubMagicAndDifficultyTab(
            progression = progression,
            onSelectMagicSpell = onSelectMagicSpell,
            onUnlockOrUpgradeMagicSpell = onUnlockOrUpgradeMagicSpell,
            onUpgradeMagicTier = onUpgradeMagicTier,
            onAdjustMagicDifficulty = onAdjustMagicDifficulty,
            onToggleMagicOvercharge = onToggleMagicOvercharge,
            onConvertCurrencyOrResource = onConvertCurrencyOrResource,
          )
          4 -> HubPlayerBaseCitadelTab(
            progression = progression,
            onCustomizePlayerBase = onCustomizePlayerBase,
            onUpgradePlayerBaseModule = onUpgradePlayerBaseModule,
            onCollectBaseVault = onCollectBaseVault,
            onTriggerOrResolveBaseAttack = onTriggerOrResolveBaseAttack,
            onConvertCurrencyOrResource = onConvertCurrencyOrResource,
          )
          else -> HubBankAndVaultTab(
            progression = progression,
            onGrantDiamondBonus = onGrantDiamondBonus,
            onConvertCurrencyOrResource = onConvertCurrencyOrResource,
            onDeployArea = onDeployArea,
          )
        }
      }
    }
  }
}

@Composable
private fun HubWalletAndPlusTenTab(
  progression: OperatorProgressionState,
  onConvertCurrencyOrResource: (String) -> Unit,
  onBoostAllTiers: (Int) -> Unit,
  onBoostAllStars: (Int) -> Unit,
  onBoostEverythingPlusTen: () -> Unit,
  onMaxAllTiers: () -> Unit,
  onMaxAllStars: () -> Unit,
  onGrantDiamondBonus: (Long) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(bottom = 12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F1E34),
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("+10 PROGRESSION BOOST PROTOCOLS (STARS FIXED — NEVER RESET)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
          Text(
            text = "Caps increased by +10: Player/Outfits Lv.${ProgressionCatalog.MAX_PLAYER_LEVEL}, Tiers T${PowerTier.MAX_TIER}, Stars ${StarRankMeta.MAX_STARS}★, Weapons Lv.${ProgressionCatalog.MAX_WEAPON_LEVEL}, Gear/Bots/Clan Lv.${ProgressionCatalog.MAX_GEAR_LEVEL}, Prestige P${ProgressionCatalog.MAX_PRESTIGE}, NG+${NewGamePlusVersion.MAX_NG_PLUS_TIER}.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFD6E4FF),
          )
          Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { onGrantDiamondBonus(50_000L) },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
              modifier = Modifier.height(48.dp).testTag("hub_grant_diamonds_btn"),
            ) {
              Text("💎 +50k Diamonds", fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = onBoostEverythingPlusTen,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
              modifier = Modifier.height(48.dp).testTag("hub_boost_plus10_btn"),
            ) {
              Text("+10 EVERYTHING", fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = { onBoostAllStars(10) },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F), contentColor = Color.Black),
              modifier = Modifier.height(48.dp).testTag("hub_boost_stars10_btn"),
            ) {
              Text("+10★ All Stars", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = { onBoostAllTiers(10) },
              modifier = Modifier.height(48.dp).testTag("hub_boost_tiers10_btn"),
            ) {
              Text("+10 All Tiers", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = onMaxAllTiers,
              modifier = Modifier.height(48.dp).testTag("hub_max_tiers_btn"),
            ) {
              Text("Max T${PowerTier.MAX_TIER}")
            }
            OutlinedButton(
              onClick = onMaxAllStars,
              modifier = Modifier.height(48.dp).testTag("hub_max_stars_btn"),
            ) {
              Text("Max ${StarRankMeta.MAX_STARS}★")
            }
          }
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("MULTI-CURRENCY & ENVIRONMENTAL RESOURCE WALLET", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
          Text(
            text = "Gold: ${progression.gold} • Diamonds: ${progression.diamonds} • TUUCash: ${progression.tuuCash} • Combined Essence: ${progression.combinedEssence} • Blast Shards: ${progression.blastShards}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = MultiverseSystemsCatalog.environmentalResourceSpecs.joinToString(" • ") { (id, name) ->
              "$name: ${progression.resourceCount(id)}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF80CBC4),
          )
        }
      }
    }

    item {
      Text("CURRENCY & RESOURCE TRANSMUTER", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }

    val conversions = listOf(
      Triple("tuucash_to_gold", "1,500 TUUCash -> +600 Gold", "Convert TUUCash notes into Gold."),
      Triple("gold_to_diamonds", "800 Gold -> +120 Diamonds", "Refine Gold bars into Diamonds."),
      Triple("diamonds_to_all_shards", "100 Diamonds -> +12 Shards (All 15 Domains)", "Crystallize Diamonds into Shards for every system."),
      Triple("resources_to_combined_essence", "15 Crystal + 20 Scrap + 10 Cells -> +150 Combined Essence & +25 Shards", "Synthesize field resources into Omega Essence."),
      Triple("tuucash_to_resources", "1,200 TUUCash -> +20 ALL Env Resources", "Acquire resource supply drop across all 5 materials."),
    )
    items(conversions, key = { it.first }) { (actionId, title, desc) ->
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Spacer(Modifier.size(8.dp))
          Button(
            onClick = { onConvertCurrencyOrResource(actionId) },
            modifier = Modifier.height(48.dp).testTag("hub_convert_$actionId"),
          ) {
            Text("Transmute")
          }
        }
      }
    }
  }
}

@Composable
private fun HubSystemsUpgraderTab(
  progression: OperatorProgressionState,
  onUpgradeTargetWithCurrency: (String, String, String) -> Unit,
) {
  var selectedCurrency by remember { mutableStateOf("shards") }
  val currencies = listOf(
    "shards" to "System Shards",
    "gold" to "Gold",
    "diamonds" to "Diamonds",
    "tuu_cash" to "TUUCash",
    "combined_essence" to "Combined Essence",
  )

  val targets = listOf(
    Triple("character", progression.equippedOutfitId, "Operator Character Tier (T${progression.characterTier}/${PowerTier.MAX_TIER}) • Shard Domain: outfit"),
    Triple("weapon", progression.weaponCustom.equippedWeaponId, "Active Weapon Tier (T${progression.weaponTier(progression.weaponCustom.equippedWeaponId)}/${PowerTier.MAX_TIER}) • Shard Domain: weapon"),
    Triple("wonder", progression.equippedWonderIds.firstOrNull() ?: "ww_ray_mk3", "Active Wonder Weapon Tier (T${progression.wonderTier(progression.equippedWonderIds.firstOrNull() ?: "ww_ray_mk3")}/${PowerTier.MAX_TIER}) • Shard Domain: wonder"),
    Triple("gear", "helmet", "Helmet Gear Tier (T${progression.gearMap["helmet"]?.tier ?: 1}/${PowerTier.MAX_TIER}) • Shard Domain: gear"),
    Triple("equipment", "lethal", "Lethal Equipment Tier (T${progression.equipmentState("lethal").tier}/${PowerTier.MAX_TIER}) • Shard Domain: equipment"),
    Triple("perk", "perk_juggernog", "Juggernog Perk Rank (R${progression.perkRank("perk_juggernog")}/${ProgressionCatalog.MAX_PERK_RANK}) • Shard Domain: perk"),
    Triple("protection", "prot_armor_weave", "Reactive Exo-Weave Plating Rank (R${progression.protectionRank("prot_armor_weave")}/${ProgressionCatalog.MAX_PROT_RANK}) • Shard Domain: protection"),
    Triple("skill", "sk_shield_1", "Shield Conduit Skill Tier (T${progression.skillNodes["sk_shield_1"]?.tier ?: 0}/${PowerTier.MAX_TIER}) • Shard Domain: skill"),
    Triple("bot", "vacuum", "Vacuum Bot Tier (T${progression.vacuumBot.tier}/${PowerTier.MAX_TIER}) • Shard Domain: bot"),
    Triple("blast_shard", "blast_shard", "Blast Shard Resonance Tier (T${progression.blastShardTier}/${PowerTier.MAX_TIER}) • Shard Domain: core"),
    Triple("companion", "companion", "Companion Buff Tier (T${progression.companionBuffTier}/${PowerTier.MAX_TIER}) • Shard Domain: outfit"),
    Triple("mode", "survival", "Survival Mode Tier (T${progression.modeTier("survival")}/${PowerTier.MAX_TIER}) • Shard Domain: mode"),
    Triple("area", progression.activeAreaId, "Active Area Power Grid (T${progression.areaState().powerGridTier}/${PowerTier.MAX_TIER}) • Shard Domain: area"),
  )

  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("SELECT PAYMENT CURRENCY FOR UNIVERSAL UPGRADES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      currencies.forEach { (id, label) ->
        FilterChip(
          selected = selectedCurrency == id,
          onClick = { selectedCurrency = id },
          label = { Text(label, style = MaterialTheme.typography.labelSmall) },
          modifier = Modifier.heightIn(min = 48.dp).testTag("hub_currency_$id"),
        )
      }
    }

    // 15 System Shards stock bar
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      MultiverseSystemsCatalog.systemShardDomains.forEach { (id, label) ->
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF141E34),
          modifier = Modifier.border(1.dp, Color(0xFFE040FB).copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
        ) {
          Text(
            text = "${label.substringBefore(" Shards")}: ${progression.systemShardCount(id)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE040FB),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(bottom = 12.dp),
    ) {
      items(targets, key = { it.first }) { (targetType, targetId, label) ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(targetType.replace('_', ' ').uppercase(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
              Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            Button(
              onClick = { onUpgradeTargetWithCurrency(targetType, targetId, selectedCurrency) },
              modifier = Modifier.height(48.dp).testTag("hub_upgrade_${targetType}_btn"),
            ) {
              Text("+1 Tier")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HubCrystalForgeTab(
  progression: OperatorProgressionState,
  onCraftCrystal: (String) -> Unit,
  onUpgradeCrystal: (String, String) -> Unit,
  onSocketCrystal: (String, String?) -> Unit,
) {
  val slots = listOf("weapon", "rig", "chest", "helmet")
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(bottom = 12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A102B),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFFF4081), RoundedCornerShape(12.dp)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("ACTIVE SOCKETED CRYSTAL BONUSES", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFF4081))
          Text(
            text = "+${progression.crystalTotalDamagePct()}% Damage • +${progression.crystalTotalShieldPct()}% Max Shield • +${progression.crystalTotalLuckPct()}% Crystal Drop Luck",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
          Text(
            text = "Craftable Crystal Frags: ${progression.resourceCount("crystal_shard")} • Kyber Frags: ${progression.resourceCount("kyber_fragment")} • Crystal Shards: ${progression.systemShardCount("crystal")}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF80CBC4),
          )
        }
      }
    }

    item {
      Text("OWNED RESONANCE CRYSTALS (${progression.crystals.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }

    items(progression.crystals, key = { it.id }) { c ->
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text("${c.name} • Lv.${c.level}/${PowerTier.MAX_TIER} (${c.rarity})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text("+${c.scaledDamageBoostPct()}% DMG • +${c.scaledShieldBoostPct()}% SHD • +${c.scaledLuckBoostPct()}% LUCK", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E5FF))
            }
            Text(
              text = c.socketedSlot?.uppercase() ?: "UNSOCKETED",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (c.socketedSlot != null) Color(0xFF00E676) else Color(0xFFFFAB40),
            )
          }
          Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
              onClick = { onUpgradeCrystal(c.id, "gold") },
              modifier = Modifier.height(48.dp).testTag("hub_crystal_upg_gold_${c.id}"),
            ) { Text("Upgrade (Gold)") }
            OutlinedButton(
              onClick = { onUpgradeCrystal(c.id, "diamonds") },
              modifier = Modifier.height(48.dp),
            ) { Text("Upgrade (Diamonds)") }
            slots.forEach { slot ->
              FilterChip(
                selected = c.socketedSlot == slot,
                onClick = { onSocketCrystal(c.id, if (c.socketedSlot == slot) null else slot) },
                label = { Text(slot.uppercase(), style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.heightIn(min = 48.dp),
              )
            }
          }
        }
      }
    }

    item {
      Text("FORGE NEW KYBER & MULTIVERSE CRYSTALS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
    }
    items(MultiverseSystemsCatalog.craftableCrystalTemplates, key = { it.id }) { tpl ->
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
      ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(1f)) {
            Text("${tpl.name} (${tpl.rarity})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(tpl.statBonusSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Button(
            onClick = { onCraftCrystal(tpl.id) },
            modifier = Modifier.height(48.dp).testTag("hub_craft_${tpl.id}"),
          ) { Text("Craft Crystal") }
        }
      }
    }
  }
}

@Composable
private fun HubMagicAndDifficultyTab(
  progression: OperatorProgressionState,
  onSelectMagicSpell: (String) -> Unit,
  onUnlockOrUpgradeMagicSpell: (String, String) -> Unit,
  onUpgradeMagicTier: (String) -> Unit,
  onAdjustMagicDifficulty: (Float) -> Unit,
  onToggleMagicOvercharge: () -> Unit,
  onConvertCurrencyOrResource: (String) -> Unit,
) {
  val ms = progression.magicState
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(bottom = 12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF141028),
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(0xFFE040FB), RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("ARCANE MAGIC CORE & OVERCHARGE DIFFICULTY", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE040FB))
          Text(
            text = "Magic Tier ${ms.magicTier}/${PowerTier.MAX_TIER} • Max Mana ${ms.manaMax.toInt()} • Difficulty Overcharge x${String.format(Locale.US, "%.2f", ms.difficultyScalingFactor)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Enable Overcharge Scaling (+Enemy Threat & +Magic/Loot)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Switch(checked = ms.overchargeDifficultyEnabled, onCheckedChange = { onToggleMagicOvercharge() })
          }
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onUpgradeMagicTier("gold") }, modifier = Modifier.height(48.dp).testTag("hub_magic_tier_gold_btn")) {
              Text("Magic Tier (Gold)")
            }
            OutlinedButton(onClick = { onUpgradeMagicTier("diamonds") }, modifier = Modifier.height(48.dp)) {
              Text("Magic Tier (Diamonds)")
            }
            Button(
              onClick = { onConvertCurrencyOrResource("diamonds_magic_overclock") },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
              modifier = Modifier.height(48.dp).testTag("hub_magic_diamond_overclock_btn"),
            ) {
              Text("💎 Overclock Magic + All Spells (180 D)", fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = { onConvertCurrencyOrResource("magic_arcane_infusion") },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB), contentColor = Color.Black),
              modifier = Modifier.height(48.dp).testTag("hub_magic_arcane_infusion_btn"),
            ) {
              Text("✨ Arcane Infusion (1k Cash + 100 D)", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = { onAdjustMagicDifficulty(-0.25f) }, modifier = Modifier.height(48.dp)) {
              Text("-0.25x Threat")
            }
            OutlinedButton(onClick = { onAdjustMagicDifficulty(0.25f) }, modifier = Modifier.height(48.dp)) {
              Text("+0.25x Threat")
            }
          }
        }
      }
    }

    items(MultiverseSystemsCatalog.magicSpells, key = { it.id }) { spell ->
      val unlocked = spell.id in ms.unlockedSpells
      val active = ms.activeSpellId == spell.id
      val tier = ms.spellTier(spell.id)
      val col = Color(spell.colorHex)
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (active) col.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().border(if (active) 2.dp else 1.dp, if (active) col else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("${spell.name} (${spell.shortTag}) • T$tier/${PowerTier.MAX_TIER} • ${spell.manaCost.toInt()} MP", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = col)
          Text(spell.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (unlocked) {
              Button(
                onClick = { onSelectMagicSpell(spell.id) },
                modifier = Modifier.height(48.dp).testTag("hub_select_spell_${spell.id}"),
              ) { Text(if (active) "Attuned" else "Equip Spell") }
            }
            OutlinedButton(
              onClick = { onUnlockOrUpgradeMagicSpell(spell.id, "gold") },
              modifier = Modifier.height(48.dp).testTag("hub_upg_spell_${spell.id}"),
            ) { Text(if (unlocked) "+1 Tier (Gold)" else "Unlock (${spell.unlockGold}g)") }
            OutlinedButton(
              onClick = { onUnlockOrUpgradeMagicSpell(spell.id, "diamonds") },
              modifier = Modifier.height(48.dp),
            ) { Text(if (unlocked) "+1 Tier (Diamonds)" else "Unlock (${spell.unlockDiamonds}d)") }
          }
        }
      }
    }
  }
}

@Composable
private fun HubPlayerBaseCitadelTab(
  progression: OperatorProgressionState,
  onCustomizePlayerBase: (String?, String?, Long?) -> Unit,
  onUpgradePlayerBaseModule: (String, String) -> Unit,
  onCollectBaseVault: () -> Unit,
  onTriggerOrResolveBaseAttack: (Boolean) -> Unit,
  onConvertCurrencyOrResource: (String) -> Unit,
) {
  val base = progression.playerBaseState
  var draftBaseName by remember(base.baseName) { mutableStateOf(base.baseName) }
  val modules = listOf(
    Triple("wall", "Reinforced Perimeter Walls", base.wallTier),
    Triple("turret", "Automated Pulse Turrets", base.turretTier),
    Triple("shield", "Barrier Shield Generators", base.shieldGeneratorTier),
    Triple("vault", "Gold & TUUCash Resource Vault", base.resourceVaultTier),
  )

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(bottom = 12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0C182B),
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(base.primaryHex), RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("${base.baseName.uppercase()} • HP ${base.baseHpCurrent}/${base.maxIntegrity()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(base.primaryHex))
          Text(
            text = "Vault Stored: ${base.storedGold} Gold • ${base.storedDiamonds} Diamonds • ${base.storedTuuCash} TUUCash • Defenses Won: ${base.defenseSuccessCount}",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF00E676),
          )
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onCollectBaseVault,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F), contentColor = Color.Black),
              modifier = Modifier.height(48.dp).testTag("hub_collect_vault_btn"),
            ) { Text("Collect Vault") }
            Button(
              onClick = { onTriggerOrResolveBaseAttack(!base.underAttack) },
              colors = ButtonDefaults.buttonColors(containerColor = if (base.underAttack) Color(0xFF00E676) else Color(0xFFFF3D71), contentColor = Color.Black),
              modifier = Modifier.height(48.dp).testTag("hub_base_siege_btn"),
            ) {
              Text(if (base.underAttack) "Resolve & Defend Raid" else "Trigger Citadel Raid")
            }
            Button(
              onClick = { onConvertCurrencyOrResource("diamonds_fortify_turrets") },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
              modifier = Modifier.height(48.dp).testTag("hub_base_diamond_fortify_btn"),
            ) { Text("💎 Fortify Turrets +2 (200 D)", fontWeight = FontWeight.Bold) }
            Button(
              onClick = { onConvertCurrencyOrResource("tuucash_deploy_turret_fleet") },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
              modifier = Modifier.height(48.dp).testTag("hub_base_tuucash_turret_btn"),
            ) { Text("💵 TUUCash Turret Fleet (3k)", fontWeight = FontWeight.Bold) }
          }
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = draftBaseName,
              onValueChange = { draftBaseName = it },
              label = { Text("Citadel Name") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            Button(
              onClick = { onCustomizePlayerBase(draftBaseName, null, null) },
              modifier = Modifier.height(48.dp),
            ) { Text("Rename") }
          }
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MultiverseSystemsCatalog.baseThemes.forEach { theme ->
              FilterChip(
                selected = base.layoutTheme == theme.id,
                onClick = { onCustomizePlayerBase(null, theme.id, theme.accentHex) },
                label = { Text(theme.name, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.heightIn(min = 48.dp),
              )
            }
          }
        }
      }
    }

    items(modules, key = { it.first }) { (moduleId, title, tier) ->
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("$title (Tier $tier/${PowerTier.MAX_TIER})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { onUpgradePlayerBaseModule(moduleId, "gold") }, modifier = Modifier.height(48.dp).testTag("hub_base_upg_${moduleId}_gold")) { Text("Gold") }
            OutlinedButton(onClick = { onUpgradePlayerBaseModule(moduleId, "tuu_cash") }, modifier = Modifier.height(48.dp)) { Text("TUUCash") }
            OutlinedButton(onClick = { onUpgradePlayerBaseModule(moduleId, "shards") }, modifier = Modifier.height(48.dp)) { Text("Base Shards") }
            OutlinedButton(onClick = { onUpgradePlayerBaseModule(moduleId, "metal_scrap") }, modifier = Modifier.height(48.dp)) { Text("Alloy Scrap") }
          }
        }
      }
    }
  }
}

@Composable
private fun HubBankAndVaultTab(
  progression: OperatorProgressionState,
  onGrantDiamondBonus: (Long) -> Unit,
  onConvertCurrencyOrResource: (String) -> Unit,
  onDeployArea: (String) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(bottom = 12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF141A12),
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(0xFFFFD54F), RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("🏛️ CENTRAL NATIONAL BANK & SECURE VAULT TREASURY", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
          Text(
            text = "Current Bank Reserves: 🪙 ${progression.gold} Gold • 💎 ${progression.diamonds} Diamonds • 💵 ${progression.tuuCash} TUUCash",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFE2E8F0),
          )
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { onGrantDiamondBonus(10_000L) },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F), contentColor = Color.Black),
              modifier = Modifier.height(48.dp).testTag("hub_bank_collect_interest_btn"),
            ) {
              Text("🏦 Collect Bank Interest (+10k Gold / +1k Diamonds)", fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = { onGrantDiamondBonus(50_000L) },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
              modifier = Modifier.height(48.dp).testTag("hub_bank_diamond_stash_btn"),
            ) {
              Text("💎 Open Diamond Vault Stash (+50k Diamonds)", fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = { onConvertCurrencyOrResource("tuucash_bank_exchange") },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
              modifier = Modifier.height(48.dp).testTag("hub_bank_tuucash_exchange_btn"),
            ) {
              Text("💵 2,500 TUUCash -> +1.5k Gold & +150 D", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = { onConvertCurrencyOrResource("diamonds_to_tuucash_shards") },
              modifier = Modifier.height(48.dp).testTag("hub_bank_diamond_tuucash_btn"),
            ) {
              Text("💎 300 Diamonds -> +12k Cash & +40 Shards", fontWeight = FontWeight.Bold)
            }
          }
          Button(
            onClick = { onDeployArea("area_bank_vault") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("hub_deploy_bank_heist_btn"),
          ) {
            Text("🚀 DEPLOY TO FEDERAL BANK & VAULT HEIST ARENA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

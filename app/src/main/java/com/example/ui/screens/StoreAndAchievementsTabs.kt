package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.AchievementState
import com.example.data.model.BlastCoreColor
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog
import com.example.data.model.StoreCategory
import com.example.data.model.TradeCurrencyKind
import com.example.ui.theme.AppIcons

@Composable
fun BlueEssenceStoreTab(
  progression: OperatorProgressionState,
  totalBlueEssence: Long,
  onPurchaseItem: (String) -> Unit,
  onExecuteTradeOffer: (String) -> Unit = {},
) {
  var isTradeMode by remember { mutableStateOf(false) }
  var selectedCategory by remember { mutableStateOf(StoreCategory.ALL) }
  val items = remember(selectedCategory) {
    if (selectedCategory == StoreCategory.ALL) ProgressionCatalog.storeItems
    else ProgressionCatalog.storeItems.filter { it.category == selectedCategory }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("blue_essence_store_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("BLUE ESSENCE STORE", style = MaterialTheme.typography.titleLarge, color = Color(0xFF00E5FF))
              Text(
                text = "Spend Blue Essence on XP Boosters, Tier Catalysts, Armor Bundles & AI Bot Uplinks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFF00E5FF).copy(alpha = 0.16f),
            ) {
              Text(
                text = "$totalBlueEssence BLUE ESSENCE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).testTag("store_blue_essence_balance"),
              )
            }
          }
          if (progression.xpBoosterMatchesRemaining > 0 || progression.botDamageBoostPct > 0) {
            Text(
              text = "Active Perks: 2X XP Matches=${progression.xpBoosterMatchesRemaining} • Bot DMG Boost=+${progression.botDamageBoostPct}%",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF00E676),
            )
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = !isTradeMode,
              onClick = { isTradeMode = false },
              label = { Text("Buy Upgrades") },
              modifier = Modifier.weight(1f).testTag("store_mode_buy"),
            )
            FilterChip(
              selected = isTradeMode,
              onClick = { isTradeMode = true },
              label = { Text("Trading Post (${MultiverseArsenalCatalog.tradeOffers.size})") },
              modifier = Modifier.weight(1f).testTag("store_mode_trade"),
            )
          }
          Text(
            text = "Blue $totalBlueEssence • Red ${progression.redEssence} • Yellow ${progression.yellowEssence} • Green ${progression.greenEssence} • Combined ${progression.combinedEssence} • Shards ${progression.blastShards}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE040FB),
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }

    if (!isTradeMode) {
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(StoreCategory.entries, key = { it.id }) { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat.label) },
              modifier = Modifier.testTag("store_cat_${cat.id}"),
            )
          }
        }
      }

      items(items, key = { it.id }) { item ->
        val tierMeta = ProgressionCatalog.tierMeta(item.tier)
        val ownedNonRepeatable = !item.repeatable && progression.purchasedStoreItems.contains(item.id)
        val canAfford = totalBlueEssence >= item.costBlueEssence && !ownedNonRepeatable
        val accent = Color(item.accentHex)

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${item.category.label.uppercase()} • TIER ${item.tier} ${tierMeta.label.uppercase()}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(tierMeta.colorHex),
                  fontWeight = FontWeight.Bold,
                )
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF00E5FF).copy(alpha = 0.14f),
              ) {
                Text(
                  text = "${item.costBlueEssence} BLUE ESSENCE",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF00E5FF),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
              onClick = { onPurchaseItem(item.id) },
              enabled = canAfford,
              colors = ButtonDefaults.buttonColors(
                containerColor = if (ownedNonRepeatable) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF00E5FF),
                contentColor = Color.Black,
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("store_buy_${item.id}"),
            ) {
              Text(
                text = when {
                  ownedNonRepeatable -> "Owned"
                  canAfford -> "Purchase (${item.costBlueEssence} Blue Essence)"
                  else -> "Need ${item.costBlueEssence - totalBlueEssence} More Blue Essence"
                },
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    } else {
      item {
        Text("MULTIVERSE TRADING POST (ESSENCE SWAPS, QUAD-FUSION & CORE FORGE)", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE040FB), fontWeight = FontWeight.Bold)
      }
      items(MultiverseArsenalCatalog.tradeOffers, key = { it.id }) { offer ->
        val canAffordTrade = offer.inputCosts.entries.all { (kind, req) ->
          val have = when (kind) {
            TradeCurrencyKind.BLUE_ESSENCE -> totalBlueEssence
            TradeCurrencyKind.RED_ESSENCE -> progression.redEssence
            TradeCurrencyKind.YELLOW_ESSENCE -> progression.yellowEssence
            TradeCurrencyKind.GREEN_ESSENCE -> progression.greenEssence
            TradeCurrencyKind.COMBINED_ESSENCE -> progression.combinedEssence
            TradeCurrencyKind.BLAST_SHARDS -> progression.blastShards
            TradeCurrencyKind.BLUE_CORE -> progression.blastCoreCount(BlastCoreColor.BLUE)
            TradeCurrencyKind.PURPLE_CORE -> progression.blastCoreCount(BlastCoreColor.PURPLE)
            TradeCurrencyKind.GREEN_CORE -> progression.blastCoreCount(BlastCoreColor.GREEN)
            TradeCurrencyKind.YELLOW_CORE -> progression.blastCoreCount(BlastCoreColor.YELLOW)
            TradeCurrencyKind.RED_CORE -> progression.blastCoreCount(BlastCoreColor.RED)
          }
          have >= req
        }
        val accent = Color(offer.outputKind.colorHex)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().border(1.5.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(offer.title, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold)
                Text(offer.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Surface(shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = 0.16f)) {
                Text(
                  text = "+${offer.outputAmount} ${offer.outputKind.label}",
                  style = MaterialTheme.typography.labelSmall,
                  color = accent,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
            Button(
              onClick = { onExecuteTradeOffer(offer.id) },
              enabled = canAffordTrade,
              colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("trade_buy_${offer.id}"),
            ) {
              Text(
                text = if (canAffordTrade) "Execute Trade" else "Insufficient Input Resources",
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AchievementsMenuTab(
  achievements: List<AchievementState>,
  onClaimAchievement: (String) -> Unit,
) {
  var selectedCategory by remember { mutableStateOf("ALL") }
  val categories = listOf("ALL", "COMBAT", "PROGRESSION", "LOBBY", "ARMORY", "STORE")
  val filtered = remember(achievements, selectedCategory) {
    if (selectedCategory == "ALL") achievements else achievements.filter { it.spec.category == selectedCategory }
  }
  val unlockedCount = achievements.count { it.unlocked }
  val claimableCount = achievements.count { it.unlocked && !it.claimed }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("achievements_menu_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(0xFFFFD54F).copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("MULTIVERSE ACHIEVEMENTS", style = MaterialTheme.typography.titleLarge, color = Color(0xFFFFD54F))
              Text(
                text = "Unlock milestones across Combat, Levels, Mega Lobbies & Store to earn Blue Essence.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFFFD54F).copy(alpha = 0.18f),
            ) {
              Text(
                text = "$unlockedCount/${achievements.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).testTag("achievements_count_badge"),
              )
            }
          }
          LinearProgressIndicator(
            progress = { unlockedCount.toFloat() / achievements.size.coerceAtLeast(1) },
            color = Color(0xFFFFD54F),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
          )
          if (claimableCount > 0) {
            Text(
              text = "$claimableCount Achievement reward(s) ready to claim!",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF00E676),
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
            label = { Text(cat) },
            modifier = Modifier.testTag("ach_cat_$cat"),
          )
        }
      }
    }

    items(filtered, key = { it.spec.id }) { ach ->
      val tierMeta = ProgressionCatalog.tierMeta(ach.spec.tierRank)
      val borderCol = when {
        ach.claimed -> Color(0xFF00E676)
        ach.unlocked -> Color(0xFFFFD54F)
        else -> MaterialTheme.colorScheme.outline
      }

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(if (ach.unlocked) 1.5.dp else 1.dp, borderCol, RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "[${ach.spec.category}] • ${tierMeta.label.uppercase()} TIER ${ach.spec.tierRank}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(tierMeta.colorHex),
                fontWeight = FontWeight.Bold,
              )
              Text(ach.spec.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(ach.spec.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.size(8.dp))
            Button(
              onClick = { onClaimAchievement(ach.spec.id) },
              enabled = ach.unlocked && !ach.claimed,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("claim_achievement_${ach.spec.id}"),
            ) {
              Text(
                when {
                  ach.claimed -> "Claimed"
                  ach.unlocked -> "Claim"
                  else -> "${ach.progress}/${ach.spec.target}"
                }
              )
            }
          }

          LinearProgressIndicator(
            progress = { ach.ratio },
            color = if (ach.unlocked) Color(0xFF00E676) else Color(0xFF00E5FF),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
          )
          Text(
            text = "Reward: +${ach.spec.rewardBlueEssence} Blue Essence • +${ach.spec.rewardPlayerXp} Operator XP",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF00E5FF),
          )
        }
      }
    }
  }
}

@Composable
fun BlueEssenceStoreDialog(
  progression: OperatorProgressionState,
  totalBlueEssence: Long,
  onPurchaseItem: (String) -> Unit,
  onDismiss: () -> Unit,
  onExecuteTradeOffer: (String) -> Unit = {},
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize().testTag("blue_essence_store_dialog")) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("BLUE ESSENCE STORE & TRADING POST", style = MaterialTheme.typography.titleLarge, color = Color(0xFF00E5FF))
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).testTag("close_store_dialog_btn")) {
            Icon(AppIcons.Close, contentDescription = "Close Blue Essence Store")
          }
        }
        BlueEssenceStoreTab(
          progression = progression,
          totalBlueEssence = totalBlueEssence,
          onPurchaseItem = onPurchaseItem,
          onExecuteTradeOffer = onExecuteTradeOffer,
        )
      }
    }
  }
}

@Composable
fun AchievementsDialog(
  achievements: List<AchievementState>,
  onClaimAchievement: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize().testTag("achievements_dialog")) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("ACHIEVEMENTS MENU", style = MaterialTheme.typography.titleLarge, color = Color(0xFFFFD54F))
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).testTag("close_achievements_dialog_btn")) {
            Icon(AppIcons.Close, contentDescription = "Close Achievements Menu")
          }
        }
        AchievementsMenuTab(
          achievements = achievements,
          onClaimAchievement = onClaimAchievement,
        )
      }
    }
  }
}

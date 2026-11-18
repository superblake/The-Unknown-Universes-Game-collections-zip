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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.BattlePassVersion
import com.example.data.model.ChallengeState
import com.example.data.model.OperatorProgressionState
import com.example.data.model.ProgressionCatalog

@Composable
fun PassAndChallengesTab(
  progression: OperatorProgressionState,
  challenges: List<ChallengeState>,
  totalEssence: Long,
  onSelectOrUnlockPass: (BattlePassVersion) -> Unit,
  onClaimPassTier: (BattlePassVersion, Int) -> Unit,
  onClaimChallenge: (String) -> Unit,
  onRefreshChallenges: () -> Unit,
) {
  var previewPass by remember(progression.activePassVersion) {
    mutableStateOf(progression.activePassVersion)
  }
  val isPreviewUnlocked = progression.unlockedPassVersions.contains(previewPass)
  val isPreviewActive = progression.activePassVersion == previewPass
  val passCycle = progression.battlePassPrestige
  val passTiers = ProgressionCatalog.tiersForVersion(previewPass, passCycle)
  val maxTierXp = passTiers.lastOrNull()?.bpXpRequired?.coerceAtLeast(1L) ?: 3400L
  val previewAccent = Color(previewPass.accentHex)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(
        text = "4 BATTLE PASS EDITIONS (EACH CHANGES ARENA GAMEPLAY)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(BattlePassVersion.entries, key = { it.id }) { version ->
          val selected = previewPass == version
          val active = progression.activePassVersion == version
          val unlocked = progression.unlockedPassVersions.contains(version)
          val accent = Color(version.accentHex)
          Surface(
            onClick = { previewPass = version },
            shape = RoundedCornerShape(14.dp),
            color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .width(226.dp)
              .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
              )
              .testTag("bp_edition_${version.id}"),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                  text = when {
                    active -> "ACTIVE"
                    unlocked -> "OWNED"
                    else -> "${version.unlockCostEssence} BLUE ESSENCE"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = accent,
                )
                Text(version.id.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text(version.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
              Text(version.gameplayEffectSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, previewAccent.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(previewPass.title, style = MaterialTheme.typography.titleLarge, color = previewAccent)
              Text(previewPass.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
              onClick = { onSelectOrUnlockPass(previewPass) },
              enabled = !isPreviewActive && (isPreviewUnlocked || totalEssence >= previewPass.unlockCostEssence),
              colors = ButtonDefaults.buttonColors(containerColor = previewAccent, contentColor = Color.Black),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("bp_activate_unlock_btn"),
            ) {
              Text(
                text = when {
                  isPreviewActive -> "Mounted"
                  isPreviewUnlocked -> "Activate"
                  else -> "Unlock (${previewPass.unlockCostEssence})"
                },
                fontWeight = FontWeight.Bold,
              )
            }
          }
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = previewAccent.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "GAMEPLAY PERK: ${previewPass.gameplayEffectSummary}",
              style = MaterialTheme.typography.labelMedium,
              color = previewAccent,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
          LinearProgressIndicator(
            progress = { (progression.battlePassXp.toFloat() / maxTierXp).coerceIn(0f, 1f) },
            color = previewAccent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
          )
          Text(
            text = "Infinite Battle Pass XP: ${progression.battlePassXp} / $maxTierXp • BP Prestige P${progression.battlePassPrestige}/30 (Cycle #${progression.bpCycleCount + 1})",
            style = MaterialTheme.typography.labelSmall,
            color = previewAccent,
          )
        }
      }
    }

    items(passTiers, key = { "${previewPass.id}_c${passCycle}_t${it.tier}" }) { tierSpec ->
      val key = "${previewPass.id}_c${passCycle}_t${tierSpec.tier}"
      val claimed = progression.claimedPassTiers.contains(key)
      val unlocked = isPreviewUnlocked && progression.battlePassXp >= tierSpec.bpXpRequired
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, if (claimed) previewAccent else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
              text = "TIER ${tierSpec.tier} [${tierSpec.unlockTag}] • ${tierSpec.title}",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(tierSpec.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = "Req: ${tierSpec.bpXpRequired} BP XP • Reward: +${tierSpec.rewardEssence} Blue Essence",
              style = MaterialTheme.typography.labelSmall,
              color = previewAccent,
            )
          }
          Spacer(Modifier.width(8.dp))
          OutlinedButton(
            onClick = { onClaimPassTier(previewPass, tierSpec.tier) },
            enabled = unlocked && !claimed,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(48.dp).testTag("claim_tier_${previewPass.id}_${tierSpec.tier}"),
          ) {
            Text(
              when {
                claimed -> "Claimed"
                !isPreviewUnlocked -> "Locked"
                progression.battlePassXp < tierSpec.bpXpRequired -> "Need XP"
                else -> "Claim"
              }
            )
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "INFINITE CHALLENGES • P${progression.challengePrestige}/30",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        OutlinedButton(
          onClick = onRefreshChallenges,
          modifier = Modifier.height(40.dp).testTag("refresh_challenges_btn"),
        ) {
          Text("Replay Completed", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    items(challenges, key = { it.spec.id }) { ch ->
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, if (ch.completed) Color(0xFF00E676) else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("[${ch.spec.category}] ${ch.spec.title}", style = MaterialTheme.typography.titleSmall)
              Text(ch.spec.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Button(
              onClick = { onClaimChallenge(ch.spec.id) },
              enabled = ch.completed && !ch.claimed,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("claim_challenge_${ch.spec.id}"),
            ) {
              Text(
                when {
                  ch.claimed -> "Claimed"
                  ch.completed -> "Claim"
                  else -> "${ch.progress}/${ch.spec.target}"
                }
              )
            }
          }
          LinearProgressIndicator(
            progress = { ch.ratio },
            color = Color(0xFF00E676),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
          )
          Text(
            text = "Rewards: +${ch.spec.rewardEssence} Blue Essence • +${ch.spec.rewardPlayerXp} Player XP • +${ch.spec.rewardBpXp} BP XP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
          )
        }
      }
    }
  }
}

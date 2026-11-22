package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MissionScope
import com.example.data.model.MissionState
import com.example.data.model.MissionsCatalog
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.UniverseCatalog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MissionsTab(
  missions: List<MissionState>,
  activeWorldId: String,
  activeAreaId: String,
  onClaimMission: (String) -> Unit,
  onDeployTarget: (MissionScope, String) -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  var selectedScope by remember { mutableStateOf(MissionScope.WORLD) }
  var selectedWorldTarget by remember(activeWorldId) { mutableStateOf(activeWorldId) }
  var selectedAreaTarget by remember(activeAreaId) { mutableStateOf(activeAreaId) }

  val targetList = if (selectedScope == MissionScope.WORLD) {
    UniverseCatalog.worlds.map { it.id to it.title }
  } else {
    MultiverseArsenalCatalog.customAreas.map { it.id to it.name }
  }

  val activeTargetId = if (selectedScope == MissionScope.WORLD) selectedWorldTarget else selectedAreaTarget
  val filteredMissions = missions.filter { it.spec.scope == selectedScope && it.spec.targetId == activeTargetId }

  val completedCount = filteredMissions.count { it.completed }
  val totalCount = filteredMissions.size.coerceAtLeast(1)

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("missions_tab_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "${MissionsCatalog.allMissions.size} MULTIVERSE & CAMPAIGN MISSIONS (45 / WORLD & ARENA)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Complete objectives across all ${UniverseCatalog.worlds.size} worlds, ${MultiverseArsenalCatalog.customAreas.size} arenas, campaign, and the Mended Convergence!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = selectedScope == MissionScope.WORLD,
              onClick = { selectedScope = MissionScope.WORLD },
              label = { Text("World Missions (${MissionsCatalog.worldMissions.size})") },
              modifier = Modifier.heightIn(min = 48.dp).testTag("missions_scope_world"),
            )
            FilterChip(
              selected = selectedScope == MissionScope.ARENA,
              onClick = { selectedScope = MissionScope.ARENA },
              label = { Text("Arena / Area Missions (${MissionsCatalog.arenaMissions.size})") },
              modifier = Modifier.heightIn(min = 48.dp).testTag("missions_scope_arena"),
            )
          }
        }
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(targetList, key = { it.first }) { (id, title) ->
          val selected = activeTargetId == id
          FilterChip(
            selected = selected,
            onClick = {
              if (selectedScope == MissionScope.WORLD) selectedWorldTarget = id
              else selectedAreaTarget = id
            },
            label = { Text(title) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("missions_target_$id"),
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
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Target Status: $completedCount / $totalCount Completed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(
              onClick = { onDeployTarget(selectedScope, activeTargetId) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("missions_deploy_target_btn"),
            ) {
              Text("Deploy to Target", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }
          LinearProgressIndicator(
            progress = { (completedCount.toFloat() / totalCount).coerceIn(0f, 1f) },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
          )
        }
      }
    }

    items(filteredMissions, key = { it.spec.id }) { ms ->
      val accent = Color(ms.spec.accentHex)
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .combinedClickable(
            enabled = ms.completed && !ms.claimed,
            onClick = { onClaimMission(ms.spec.id) },
            onLongClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onClaimMission(ms.spec.id)
            },
          )
          .border(if (ms.completed) 1.5.dp else 1.dp, if (ms.completed) Color(0xFF00E676) else accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text(ms.spec.chapterLabel, style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
              Text(ms.spec.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (ms.completed) Color(0xFF00E676).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Text(
                text = if (ms.claimed) "CLAIMED" else if (ms.completed) "READY TO CLAIM" else "${ms.progress} / ${ms.spec.targetCount}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (ms.completed) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }

          Text(ms.spec.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

          LinearProgressIndicator(
            progress = { ms.ratio },
            color = if (ms.completed) Color(0xFF00E676) else accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Rewards: +${ms.spec.rewardBlueEssence} Blue Essence • +${ms.spec.rewardPlayerXp} XP • +${ms.spec.rewardShards} Shards",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF00E5FF),
            )
            if (ms.completed && !ms.claimed) {
              Button(
                onClick = { onClaimMission(ms.spec.id) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp).testTag("claim_mission_${ms.spec.id}"),
              ) {
                Text("Claim", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun MissionsDialog(
  missions: List<MissionState>,
  activeWorldId: String,
  activeAreaId: String,
  onClaimMission: (String) -> Unit,
  onDeployTarget: (MissionScope, String) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Multiverse & Campaign Missions") },
    text = {
      Column(modifier = Modifier.fillMaxWidth().height(500.dp)) {
        MissionsTab(
          missions = missions,
          activeWorldId = activeWorldId,
          activeAreaId = activeAreaId,
          onClaimMission = onClaimMission,
          onDeployTarget = onDeployTarget,
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag("close_missions_dialog_btn")) {
        Text("Close")
      }
    },
  )
}

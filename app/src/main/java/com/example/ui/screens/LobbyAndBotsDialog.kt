package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BotDifficulty
import com.example.data.model.LobbyConfig
import com.example.data.model.LobbyPartyMode
import com.example.data.model.ProgressionCatalog
import com.example.ui.theme.AppIcons

private val lobbyCapacityOptions = listOf(1, 2, 4, 8, 12, 16, 24, 32)

@Composable
fun LobbyQuickCard(
  lobbyConfig: LobbyConfig,
  playerCallsign: String,
  playerLevel: Int,
  playerPrestige: Int,
  accentColor: Color,
  onUpdateLobby: ((LobbyConfig) -> LobbyConfig) -> Unit,
  onOpenFullRoster: () -> Unit,
) {
  val roster = ProgressionCatalog.generateLobbyRoster(lobbyConfig, playerCallsign, playerLevel, playerPrestige)
  val botCount = roster.count { !it.isHumanPlayer }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.5.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
      .testTag("lobby_config_card"),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "LOBBY & AI SQUAD BOTS",
            style = MaterialTheme.typography.titleMedium,
            color = accentColor,
          )
          Text(
            text = "${lobbyConfig.partyMode.displayName} • ${lobbyConfig.targetPlayers}P Lobby (${if (botCount > 0) "$botCount AI Bots" else "Solo Human"})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (lobbyConfig.isOverEightLobby) Color(0xFFE040FB).copy(alpha = 0.2f) else accentColor.copy(alpha = 0.16f),
        ) {
          Text(
            text = if (lobbyConfig.isOverEightLobby) "MEGA ${lobbyConfig.targetPlayers}P" else "${lobbyConfig.targetPlayers}P LOBBY",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (lobbyConfig.isOverEightLobby) Color(0xFFE040FB) else accentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("lobby_size_badge"),
          )
        }
      }

      // Solo / Duos / Squads Selector
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(LobbyPartyMode.entries, key = { it.id }) { mode ->
          val selected = lobbyConfig.partyMode == mode
          FilterChip(
            selected = selected,
            onClick = {
              onUpdateLobby { cur ->
                val suggestedSize = when {
                  mode == LobbyPartyMode.SOLO && cur.targetPlayers <= 4 -> 1
                  mode == LobbyPartyMode.DUOS && cur.targetPlayers < 2 -> 2
                  mode == LobbyPartyMode.SQUADS && cur.targetPlayers < 4 -> 4
                  else -> cur.targetPlayers
                }
                cur.copy(partyMode = mode, targetPlayers = suggestedSize)
              }
            },
            label = { Text(mode.displayName) },
            modifier = Modifier.testTag("lobby_mode_${mode.id}"),
          )
        }
      }

      // Lobby Player Count (including >8 player lobbies: 12, 16, 24, 32)
      Text("LOBBY CAPACITY (SUPPORTS >8 PLAYERS)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(lobbyCapacityOptions) { cap ->
          val selected = lobbyConfig.targetPlayers == cap
          FilterChip(
            selected = selected,
            onClick = { onUpdateLobby { it.copy(targetPlayers = cap) } },
            label = { Text("${cap}P${if (cap > 8) "+" else ""}") },
            modifier = Modifier.testTag("lobby_cap_$cap"),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Auto-Fill AI Bots", style = MaterialTheme.typography.labelLarge)
          Text(
            text = "Populates empty teammate & rival slots when players are offline",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = lobbyConfig.autoFillBots,
          onCheckedChange = { enabled -> onUpdateLobby { it.copy(autoFillBots = enabled) } },
          modifier = Modifier.testTag("lobby_autofill_bots_switch"),
        )
      }

      OutlinedButton(
        onClick = onOpenFullRoster,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_lobby_roster_btn"),
      ) {
        Icon(AppIcons.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Manage Squad, Bot AI (${lobbyConfig.botDifficulty.label}) & Roster (${roster.size}/${lobbyConfig.targetPlayers})")
      }
    }
  }
}

@Composable
fun LobbyAndBotsDialog(
  lobbyConfig: LobbyConfig,
  playerCallsign: String,
  playerLevel: Int,
  playerPrestige: Int,
  onUpdateLobby: ((LobbyConfig) -> LobbyConfig) -> Unit,
  onDismiss: () -> Unit,
) {
  val roster = ProgressionCatalog.generateLobbyRoster(lobbyConfig, playerCallsign, playerLevel, playerPrestige)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxSize().testTag("lobby_roster_dialog"),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("MULTIVERSE LOBBY & BOT COMMAND", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
              Text(
                text = "${lobbyConfig.partyMode.displayName} • ${roster.size}/${lobbyConfig.targetPlayers} Operators (${lobbyConfig.activeBotCount} AI Bots)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(48.dp).testTag("close_lobby_dialog_btn"),
            ) {
              Icon(AppIcons.Close, contentDescription = "Close Lobby Roster")
            }
          }
        }

        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("AI BOT DIFFICULTY", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  items(BotDifficulty.entries, key = { it.id }) { diff ->
                    FilterChip(
                      selected = lobbyConfig.botDifficulty == diff,
                      onClick = { onUpdateLobby { it.copy(botDifficulty = diff) } },
                      label = { Text(diff.label) },
                      modifier = Modifier.testTag("bot_diff_${diff.id}"),
                    )
                  }
                }
                Text("LOBBY SIZE PRESETS (1 TO 32 PLAYERS)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  items(lobbyCapacityOptions) { cap ->
                    FilterChip(
                      selected = lobbyConfig.targetPlayers == cap,
                      onClick = { onUpdateLobby { it.copy(targetPlayers = cap) } },
                      label = { Text("${cap} Players") },
                      modifier = Modifier.testTag("dialog_lobby_cap_$cap"),
                    )
                  }
                }
              }
            }
          }

          item {
            Text(
              text = "ACTIVE LOBBY ROSTER (${roster.size} SLOTS)",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          items(roster, key = { it.id }) { member ->
            val badgeCol = Color(member.accentHex)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth().border(
                width = if (member.isHumanPlayer) 1.5.dp else 1.dp,
                color = if (member.isHumanPlayer) Color(0xFF00E5FF) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp),
              ),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                  Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(badgeCol))
                  Column {
                    Text(
                      text = "${member.callsign} • ${if (member.isHumanPlayer) "YOU" else if (member.isAlliedSquadmate) "ALLY BOT" else "RIVAL BOT"}",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "Squad #${member.squadIndex} • Rig: ${member.archetype.uppercase()} • Lv.${member.level}${if (member.prestige > 0) " P${member.prestige}" else ""}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = badgeCol.copy(alpha = 0.16f),
                ) {
                  Text(
                    text = if (member.isAlliedSquadmate) "SQUAD 1" else "SQD ${member.squadIndex}",
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeCol,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            }
          }
        }

        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
          ) {
            Text("Done — Save Lobby Settings", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

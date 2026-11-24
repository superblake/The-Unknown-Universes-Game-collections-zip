package com.example.ui.screens

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BotDifficulty
import com.example.data.model.MatchPrivacy
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.MultiverseMatchListing
import com.example.data.model.OperatorProgressionState
import com.example.data.model.PrivateMatchRules
import com.example.data.model.RegionalServerCatalog
import com.example.data.model.RegionalServerSpec
import com.example.data.model.SocialCatalog
import com.example.data.model.UniverseCatalog
import com.example.data.model.WorldRegion
import com.example.ui.theme.AppIcons

private val matchBrowserTabs = listOf("ALL", "PUBLIC", "PRIVATE", "FRIENDS", "HOST PRIVATE", "DIRECT CODE")
private val lobbySizes = listOf(1, 2, 4, 8, 12, 16, 24, 32, 64, 100, 128, 150, 200)
private val startingWaveOptions = listOf(1, 5, 10, 15, 20, 25, 30)
private val customWaveGoalOptions = listOf(0, 5, 10, 15, 20, 30)

@Composable
fun MatchBrowserDialog(
  progression: OperatorProgressionState,
  playerCallsign: String,
  initialTabIdx: Int = 0,
  activeRegion: WorldRegion = WorldRegion.NA_EAST,
  activeServer: RegionalServerSpec? = null,
  onSelectRegionServer: ((String, String) -> Unit)? = null,
  onHostPrivateMatch: (PrivateMatchRules) -> Unit,
  onJoinMatch: (MultiverseMatchListing, String?) -> Boolean,
  onLeaveMatch: () -> Unit = {},
  onDismiss: () -> Unit,
) {
  var selectedTabIdx by remember(initialTabIdx) { mutableIntStateOf(initialTabIdx.coerceIn(0, matchBrowserTabs.lastIndex)) }
  var searchQuery by remember { mutableStateOf("") }
  var filterOnlyAvailable by remember { mutableStateOf(false) }
  var filterLowPing by remember { mutableStateOf(false) }
  var filterMegaLobbies by remember { mutableStateOf(false) }
  var selectedRegionFilter by remember { mutableStateOf<String?>(null) }

  var matchPendingPasscode by remember { mutableStateOf<MultiverseMatchListing?>(null) }
  var passcodeDraft by remember { mutableStateOf("") }
  var directCodeInput by remember { mutableStateOf("") }

  // State for Host Private Match customizer
  var customRules by remember(progression.privateMatchRules) { mutableStateOf(progression.privateMatchRules) }

  val allMatches = remember(progression.joinedMatch) {
    val base = SocialCatalog.sampleMatches
    val joined = progression.joinedMatch
    if (joined != null && base.none { it.id == joined.id }) listOf(joined) + base else base
  }

  val filteredMatches = remember(allMatches, selectedTabIdx, searchQuery, filterOnlyAvailable, filterLowPing, filterMegaLobbies, selectedRegionFilter, progression.friends) {
    allMatches.filter { match ->
      // Tab filter
      val matchesTab = when (selectedTabIdx) {
        0 -> true // ALL
        1 -> match.privacy == MatchPrivacy.PUBLIC // PUBLIC
        2 -> match.privacy != MatchPrivacy.PUBLIC // PRIVATE
        3 -> progression.friends.any { it.currentMatchId == match.id || it.callsign.equals(match.hostCallsign, ignoreCase = true) } // FRIENDS
        else -> true
      }
      if (!matchesTab) return@filter false

      // Search query
      if (searchQuery.isNotBlank()) {
        val q = searchQuery.trim().lowercase()
        val inName = match.roomName.lowercase().contains(q)
        val inHost = match.hostCallsign.lowercase().contains(q)
        val inWorld = match.worldTitle.lowercase().contains(q)
        val inMode = match.modeLabel.lowercase().contains(q)
        val inCode = match.roomCode.lowercase().contains(q)
        val inReg = WorldRegion.fromId(match.regionId).shortTag.lowercase().contains(q)
        if (!inName && !inHost && !inWorld && !inMode && !inCode && !inReg) return@filter false
      }

      // Filter chips
      if (filterOnlyAvailable && match.isFull) return@filter false
      if (filterLowPing && match.pingMs > 40) return@filter false
      if (filterMegaLobbies && match.maxPlayers <= 8) return@filter false
      if (selectedRegionFilter != null && match.regionId != selectedRegionFilter) return@filter false

      true
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxSize().testTag("match_browser_dialog"),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
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
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(AppIcons.Public, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                Text(
                  text = "MULTIVERSE MATCH BROWSER",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Text(
                text = "Search, join, or host customizable lobbies across ${UniverseCatalog.worlds.size} worlds & ${MultiverseArsenalCatalog.customAreas.size} arenas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(48.dp).testTag("close_match_browser_btn"),
            ) {
              Icon(AppIcons.Close, contentDescription = "Close Match Browser")
            }
          }
        }

        // Active Session Banner (when connected to a public/private room)
        val activeJoined = progression.joinedMatch
        if (activeJoined != null) {
          Surface(
            color = Color(0xFF00E676).copy(alpha = 0.14f),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00E676).copy(alpha = 0.45f)),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "CONNECTED: ${activeJoined.roomName} [${activeJoined.roomCode}]",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF00E676),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${activeJoined.worldTitle} • ${activeJoined.modeLabel} • ${activeJoined.currentPlayers}/${activeJoined.maxPlayers}P",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              OutlinedButton(
                onClick = onLeaveMatch,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(48.dp).testTag("leave_joined_match_btn"),
              ) {
                Text("Leave", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
              }
            }
          }
        }

        // Multi-Tab Selector Bar (Scrollable so all 6 tabs are never clipped on narrow screens)
        ScrollableTabRow(
          selectedTabIndex = selectedTabIdx,
          edgePadding = 8.dp,
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          contentColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.fillMaxWidth().testTag("match_browser_tab_row"),
        ) {
          matchBrowserTabs.forEachIndexed { index, label ->
            Tab(
              selected = selectedTabIdx == index,
              onClick = { selectedTabIdx = index },
              text = { Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
              modifier = Modifier.testTag("match_tab_$index"),
            )
          }
        }

        // Body content based on tab
        when (selectedTabIdx) {
          0, 1, 2, 3 -> {
            // Browse Matches list with Search and Filter bar
            Column(modifier = Modifier.fillMaxSize()) {
              // Search & Filter header
              Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by room name, host, world, mode, or code...") },
                    leadingIcon = { Icon(AppIcons.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                      if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                          Icon(AppIcons.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                      }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("match_search_input"),
                  )
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                      FilterChip(
                        selected = filterOnlyAvailable,
                        onClick = { filterOnlyAvailable = !filterOnlyAvailable },
                        label = { Text("Open Slots Only") },
                        modifier = Modifier.testTag("filter_available_chip"),
                      )
                    }
                    item {
                      FilterChip(
                        selected = filterLowPing,
                        onClick = { filterLowPing = !filterLowPing },
                        label = { Text("Low Ping (<40ms)") },
                        modifier = Modifier.testTag("filter_low_ping_chip"),
                      )
                    }
                    item {
                      FilterChip(
                        selected = filterMegaLobbies,
                        onClick = { filterMegaLobbies = !filterMegaLobbies },
                        label = { Text("Mega Lobbies (>8P / 100+)") },
                        modifier = Modifier.testTag("filter_mega_chip"),
                      )
                    }
                    item {
                      FilterChip(
                        selected = selectedRegionFilter == null,
                        onClick = { selectedRegionFilter = null },
                        label = { Text("All Regions") },
                        modifier = Modifier.testTag("filter_region_all"),
                      )
                    }
                    items(WorldRegion.entries, key = { it.id }) { region ->
                      FilterChip(
                        selected = selectedRegionFilter == region.id,
                        onClick = {
                          selectedRegionFilter = if (selectedRegionFilter == region.id) null else region.id
                        },
                        label = { Text(region.shortTag) },
                        modifier = Modifier.testTag("filter_region_${region.id}"),
                      )
                    }
                  }
                }
              }

              // Matches list
              if (filteredMatches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(AppIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Text("No matching multiverse lobbies found", style = MaterialTheme.typography.titleMedium)
                    Text("Try adjusting your search query or host your own customized private match!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                      onClick = { selectedTabIdx = 4 },
                      modifier = Modifier.padding(top = 8.dp).testTag("go_to_host_btn"),
                    ) {
                      Text("Host Custom Private Match")
                    }
                  }
                }
              } else {
                LazyColumn(
                  modifier = Modifier.fillMaxSize(),
                  contentPadding = PaddingValues(16.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  items(filteredMatches, key = { it.id }) { match ->
                    MatchListingCard(
                      match = match,
                      isJoined = progression.joinedMatch?.id == match.id,
                      onJoinClicked = {
                        if (match.privacy == MatchPrivacy.PRIVATE_PASSWORD) {
                          matchPendingPasscode = match
                          passcodeDraft = ""
                        } else {
                          onJoinMatch(match, null)
                        }
                      },
                    )
                  }
                }
              }
            }
          }

          4 -> {
            // HOST PRIVATE MATCH: High customization tab
            HostPrivateMatchTab(
              customRules = customRules,
              progression = progression,
              activeRegion = activeRegion,
              activeServer = activeServer ?: RegionalServerCatalog.defaultServerFor(activeRegion),
              onSelectRegionServer = onSelectRegionServer,
              onRulesChanged = { customRules = it },
              onDeploy = {
                onHostPrivateMatch(customRules)
                onDismiss()
              },
            )
          }

          5 -> {
            // DIRECT CODE JOIN TAB
            DirectCodeJoinTab(
              directCodeInput = directCodeInput,
              onCodeChanged = { directCodeInput = it },
              onDirectJoin = { code, pass ->
                val match = allMatches.firstOrNull { it.roomCode.equals(code.trim(), ignoreCase = true) }
                val ok = if (match != null) {
                  onJoinMatch(match, pass)
                } else {
                  val selectedWorld = UniverseCatalog.worldById(customRules.worldId)
                  val selectedMode = UniverseCatalog.modeById(customRules.modeId)
                  val selectedArea = progression.resolveAreaSpec(customRules.areaId)
                  val srv = activeServer ?: RegionalServerCatalog.defaultServerFor(activeRegion)
                  val generated = MultiverseMatchListing(
                    id = "match_${code.lowercase().trim()}",
                    roomName = "Direct Session [$code]",
                    hostCallsign = "CommandOperator",
                    hostLevel = 35,
                    hostStars = 7,
                    worldId = selectedWorld.id,
                    worldTitle = selectedWorld.title,
                    modeId = selectedMode.id,
                    modeLabel = selectedMode.label,
                    areaId = selectedArea.id,
                    areaName = selectedArea.name,
                    currentPlayers = 1,
                    maxPlayers = customRules.maxPlayers,
                    privacy = MatchPrivacy.INVITE_ONLY,
                    roomCode = code.trim().uppercase(),
                    pingMs = srv.basePingMs,
                    currentWave = customRules.startingWave,
                    activeMutators = listOf("Custom Direct Join"),
                    regionId = activeRegion.id,
                    serverId = srv.id,
                  )
                  onJoinMatch(generated, pass)
                }
                if (ok) onDismiss()
              },
            )
          }
        }
      }
    }
  }

  // Passcode entry dialog for locked matches
  if (matchPendingPasscode != null) {
    val pending = matchPendingPasscode!!
    AlertDialog(
      onDismissRequest = { matchPendingPasscode = null },
      title = { Text("Private Match Passcode") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Enter the security passcode to join '${pending.roomName}' (Host: ${pending.hostCallsign})")
          OutlinedTextField(
            value = passcodeDraft,
            onValueChange = { passcodeDraft = it },
            label = { Text("Passcode") },
            placeholder = { Text("e.g. 7744") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("match_passcode_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onJoinMatch(pending, passcodeDraft.trim())
            matchPendingPasscode = null
          },
          modifier = Modifier.testTag("confirm_passcode_join_btn"),
        ) {
          Text("Unlock & Join")
        }
      },
      dismissButton = {
        TextButton(onClick = { matchPendingPasscode = null }) {
          Text("Cancel")
        }
      },
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MatchListingCard(
  match: MultiverseMatchListing,
  isJoined: Boolean,
  onJoinClicked: () -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  val privacyColor = when (match.privacy) {
    MatchPrivacy.PUBLIC -> Color(0xFF00E676)
    MatchPrivacy.PRIVATE_PASSWORD -> Color(0xFFFF9100)
    MatchPrivacy.INVITE_ONLY -> Color(0xFF40C4FF)
    MatchPrivacy.FRIENDS_ONLY -> Color(0xFFE040FB)
    MatchPrivacy.CLAN_ONLY -> Color(0xFFFF5252)
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .combinedClickable(
        enabled = !match.isFull || isJoined,
        onClick = onJoinClicked,
        onLongClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onJoinClicked()
        },
      )
      .border(
        width = if (isJoined) 2.dp else 1.dp,
        color = if (isJoined) Color(0xFF00E676) else MaterialTheme.colorScheme.outline,
        shape = RoundedCornerShape(16.dp),
      )
      .testTag("match_card_${match.id}"),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(match.roomName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (match.clanTag != null) {
              Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF00E5FF).copy(alpha = 0.2f)) {
                Text(
                  text = "[${match.clanTag}]",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF00E5FF),
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
              }
            }
          }
          Text(
            text = "Host: ${match.hostCallsign} (Lv.${match.hostLevel} • ${match.hostStars}★) • Wave ${match.currentWave}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = privacyColor.copy(alpha = 0.18f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = if (match.privacy == MatchPrivacy.PUBLIC) AppIcons.Public else AppIcons.Lock,
              contentDescription = null,
              tint = privacyColor,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = match.privacy.displayName.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = privacyColor,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val reg = WorldRegion.fromId(match.regionId)
        val srv = RegionalServerCatalog.serverById(match.serverId)
        Text(
          text = "${match.worldTitle} • ${match.areaName} • ${match.modeLabel}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${reg.shortTag}/${srv.code} • ${match.pingMs}ms",
          style = MaterialTheme.typography.labelSmall,
          color = if (match.pingMs < 35) Color(0xFF00E676) else Color(0xFFFFB74D),
          fontWeight = FontWeight.Bold,
        )
      }

      // Mutators pills
      if (match.activeMutators.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(match.activeMutators) { mut ->
            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
              Text(
                text = mut,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Code: ${match.roomCode}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Players: ${match.currentPlayers}/${match.maxPlayers}${if (match.maxPlayers > 8) " [MEGA]" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = if (match.isFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = onJoinClicked,
          enabled = !match.isFull || isJoined,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isJoined) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
          ),
          modifier = Modifier.height(48.dp).testTag("join_match_${match.id}"),
        ) {
          Text(
            text = when {
              isJoined -> "CONNECTED"
              match.isFull -> "LOBBY FULL"
              match.privacy == MatchPrivacy.PRIVATE_PASSWORD -> "ENTER PASSCODE"
              else -> "JOIN MATCH"
            },
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
    }
  }
}

@Composable
private fun HostPrivateMatchTab(
  customRules: PrivateMatchRules,
  progression: OperatorProgressionState,
  activeRegion: WorldRegion,
  activeServer: RegionalServerSpec,
  onSelectRegionServer: ((String, String) -> Unit)?,
  onRulesChanged: (PrivateMatchRules) -> Unit,
  onDeploy: () -> Unit,
) {
  val playableAreas = remember(progression.customGameMaps) { progression.allPlayableAreas() }
  val regionServers = remember(activeRegion) { RegionalServerCatalog.serversForRegion(activeRegion) }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("host_private_match_tab"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("HIGH-CUSTOMIZATION PRIVATE MATCH", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
          Text(
            text = "Configure custom rules, mutators, region servers, waves, lobby size (up to 200 players), AI bots, and passcodes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    item {
      Text("WORLD REGION & SERVER (${activeRegion.shortTag} • ${activeServer.code})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(WorldRegion.entries, key = { it.id }) { region ->
          FilterChip(
            selected = activeRegion == region,
            onClick = {
              val defSrv = RegionalServerCatalog.defaultServerFor(region)
              onSelectRegionServer?.invoke(region.id, defSrv.id)
            },
            label = { Text("${region.shortTag} (${region.displayName})") },
            modifier = Modifier.heightIn(min = 48.dp).testTag("host_region_chip_${region.id}"),
          )
        }
      }
      Spacer(Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(regionServers, key = { it.id }) { srv ->
          FilterChip(
            selected = activeServer.id == srv.id,
            onClick = { onSelectRegionServer?.invoke(srv.region.id, srv.id) },
            label = { Text("${srv.code} • ${srv.basePingMs}ms (${srv.bonusTag})") },
            modifier = Modifier.heightIn(min = 48.dp).testTag("host_server_chip_${srv.id}"),
          )
        }
      }
    }

    // Room Name & Code
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = customRules.roomName,
          onValueChange = { onRulesChanged(customRules.copy(roomName = it)) },
          label = { Text("Match Room Title") },
          modifier = Modifier.weight(1.5f).testTag("custom_room_name_input"),
        )
        OutlinedTextField(
          value = customRules.roomCode,
          onValueChange = { onRulesChanged(customRules.copy(roomCode = it.uppercase())) },
          label = { Text("Room Code") },
          modifier = Modifier.weight(1f).testTag("custom_room_code_input"),
        )
      }
    }

    // Privacy Setting
    item {
      Text("PRIVACY LEVEL", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(MatchPrivacy.entries, key = { it.id }) { priv ->
          FilterChip(
            selected = customRules.privacy == priv,
            onClick = { onRulesChanged(customRules.copy(privacy = priv)) },
            label = { Text(priv.displayName) },
            modifier = Modifier.testTag("privacy_chip_${priv.id}"),
          )
        }
      }
      if (customRules.privacy == MatchPrivacy.PRIVATE_PASSWORD) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
          value = customRules.passcode,
          onValueChange = { onRulesChanged(customRules.copy(passcode = it)) },
          label = { Text("Security Passcode") },
          placeholder = { Text("e.g. 7744") },
          modifier = Modifier.fillMaxWidth().testTag("custom_passcode_input"),
        )
      }
    }

    // World, Mode & Area
    item {
      Text("WORLD SELECTION", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(UniverseCatalog.worlds, key = { it.id }) { world ->
          FilterChip(
            selected = customRules.worldId == world.id,
            onClick = {
              val matchingArea = playableAreas.firstOrNull { it.linkedWorldId == world.id }
              val keepArea = progression.resolveAreaSpec(customRules.areaId).linkedWorldId == world.id
              onRulesChanged(
                customRules.copy(
                  worldId = world.id,
                  modeId = world.defaultModeId,
                  areaId = if (keepArea) customRules.areaId else (matchingArea?.id ?: customRules.areaId),
                )
              )
            },
            label = { Text(world.title) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("world_chip_${world.id}"),
          )
        }
      }
      Spacer(Modifier.height(8.dp))
      Text("GAME MODE", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(UniverseCatalog.modes, key = { it.id }) { mode ->
          FilterChip(
            selected = customRules.modeId == mode.id,
            onClick = { onRulesChanged(customRules.copy(modeId = mode.id)) },
            label = { Text("${mode.shortTag} • ${mode.label}") },
            modifier = Modifier.heightIn(min = 48.dp).testTag("mode_chip_${mode.id}"),
          )
        }
      }
      Spacer(Modifier.height(8.dp))
      Text("CUSTOM AREA LANDMARK (${playableAreas.size} MAPS)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(playableAreas, key = { it.id }) { area ->
          FilterChip(
            selected = customRules.areaId == area.id,
            onClick = {
              val linkedWorld = UniverseCatalog.worldById(area.linkedWorldId)
              val syncedMode = progression.defaultModeForArea(area.id, linkedWorld.defaultModeId)
              onRulesChanged(
                customRules.copy(
                  areaId = area.id,
                  worldId = linkedWorld.id,
                  modeId = syncedMode,
                )
              )
            },
            label = { Text(area.name) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("area_chip_${area.id}"),
          )
        }
      }
    }

    // Starting Wave, Custom Wave Goal & Max Players
    item {
      Text("STARTING WAVE", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(startingWaveOptions) { wave ->
          FilterChip(
            selected = customRules.startingWave == wave,
            onClick = { onRulesChanged(customRules.copy(startingWave = wave)) },
            label = { Text("Wave $wave") },
            modifier = Modifier.testTag("start_wave_chip_$wave"),
          )
        }
      }
      Spacer(Modifier.height(8.dp))
      Text("VICTORY WAVE GOAL", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(customWaveGoalOptions) { goal ->
          FilterChip(
            selected = customRules.customWaveGoal == goal,
            onClick = { onRulesChanged(customRules.copy(customWaveGoal = goal)) },
            label = { Text(if (goal == 0) "Endless" else "Goal W$goal") },
            modifier = Modifier.testTag("wave_goal_chip_$goal"),
          )
        }
      }
      Spacer(Modifier.height(8.dp))
      Text("MAX CAPACITY (UP TO 200 PLAYERS)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(lobbySizes) { size ->
          FilterChip(
            selected = customRules.maxPlayers == size,
            onClick = { onRulesChanged(customRules.copy(maxPlayers = size)) },
            label = { Text("${size}P${if (size > 8) " (Mega)" else ""}") },
            modifier = Modifier.testTag("max_players_chip_$size"),
          )
        }
      }
    }

    // Bot Fill & Difficulty
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Auto-Fill AI Bots", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Text("Fills vacant lobby slots with intelligent AI bots", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
          checked = customRules.botFillEnabled,
          onCheckedChange = { onRulesChanged(customRules.copy(botFillEnabled = it)) },
          modifier = Modifier.testTag("bot_fill_switch"),
        )
      }
      if (customRules.botFillEnabled) {
        Spacer(Modifier.height(6.dp))
        Text("BOT DIFFICULTY", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(BotDifficulty.entries, key = { it.id }) { diff ->
            FilterChip(
              selected = customRules.botDifficulty == diff,
              onClick = { onRulesChanged(customRules.copy(botDifficulty = diff)) },
              label = { Text(diff.label) },
              modifier = Modifier.testTag("bot_diff_chip_${diff.id}"),
            )
          }
        }
      }
    }

    // Mutators Toggles (Customizable Game Rules)
    item {
      Text("CUSTOM MATCH MUTATORS", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MutatorToggle("Infinite Ammo", "Weapons fire without consuming reserve magazines", customRules.infiniteAmmo) {
          onRulesChanged(customRules.copy(infiniteAmmo = it))
        }
        MutatorToggle("Headshots Only / Instagib", "All hits inflict maximum precision critical damage", customRules.headshotsOnly) {
          onRulesChanged(customRules.copy(headshotsOnly = it))
        }
        MutatorToggle("Boss Rush", "Spawns Hellbreaker and Overlord Bosses every wave", customRules.bossRush) {
          onRulesChanged(customRules.copy(bossRush = it))
        }
        MutatorToggle("Wonder Weapon Boost", "Multiplies Ray Nova MK3 and Wonder Weapon crate drops", customRules.wonderSpawnBoost) {
          onRulesChanged(customRules.copy(wonderSpawnBoost = it))
        }
        MutatorToggle("Low Gravity Float", "Doubles jump hangtime and tactical dash distance", customRules.lowGravity) {
          onRulesChanged(customRules.copy(lowGravity = it))
        }
        MutatorToggle("2x Speed Velocity", "Increases operator sprint and fire rate by 100%", customRules.doubleSpeed) {
          onRulesChanged(customRules.copy(doubleSpeed = it))
        }
        MutatorToggle("Vampiric Siphon", "Instantly restores +20 Shield upon every enemy elimination", customRules.vampiricSiphon) {
          onRulesChanged(customRules.copy(vampiricSiphon = it))
        }
        MutatorToggle("2x Blue Essence Multiplier", "Doubles Blue Essence earned from waves and kills", customRules.doubleBlueEssence) {
          onRulesChanged(customRules.copy(doubleBlueEssence = it))
        }
        MutatorToggle("Hardcore Fire (+25% Essence)", "Hostiles hit +35% harder while bots & essence reward surge", customRules.friendlyFire) {
          onRulesChanged(customRules.copy(friendlyFire = it))
        }
      }
    }

    // Host Button
    item {
      Button(
        onClick = onDeploy,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("host_match_deploy_btn"),
      ) {
        Icon(AppIcons.Gamepad, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          text = "HOST PRIVATE MATCH (${customRules.roomCode})",
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleMedium,
        )
      }
    }
  }
}

@Composable
private fun DirectCodeJoinTab(
  directCodeInput: String,
  onCodeChanged: (String) -> Unit,
  onDirectJoin: (String, String?) -> Unit,
) {
  var passcode by remember { mutableStateOf("") }

  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp).testTag("direct_code_join_tab"),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(AppIcons.Key, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(56.dp))
    Text("DIRECT ROOM CODE ENTRY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(
      text = "Enter a direct Room Code shared by a friend, clan member, or private match host to connect instantly.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    OutlinedTextField(
      value = directCodeInput,
      onValueChange = onCodeChanged,
      label = { Text("Room Code (e.g. GENESIS-774, GOTH-550)") },
      placeholder = { Text("ENTER CODE") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("direct_room_code_input"),
    )

    OutlinedTextField(
      value = passcode,
      onValueChange = { passcode = it },
      label = { Text("Passcode (if required)") },
      placeholder = { Text("Optional Passcode") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("direct_passcode_input"),
    )

    Button(
      onClick = { onDirectJoin(directCodeInput, passcode.ifBlank { null }) },
      enabled = directCodeInput.trim().length >= 3,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().height(50.dp).testTag("direct_connect_btn"),
    ) {
      Icon(AppIcons.Public, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(Modifier.width(8.dp))
      Text("DIRECT CONNECT & JOIN", fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun MutatorToggle(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
      modifier = Modifier.testTag("mutator_switch_${title.replace(" ", "_").lowercase()}"),
    )
  }
}

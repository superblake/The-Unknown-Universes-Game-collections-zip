package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatChannel
import com.example.data.model.ChatMessage
import com.example.data.model.ClanData
import com.example.data.model.ClanRank
import com.example.data.model.FriendEntry
import com.example.data.model.FriendOnlineStatus
import com.example.data.model.MultiverseMatchListing
import com.example.data.model.OperatorProgressionState
import com.example.data.model.SocialCatalog
import com.example.ui.theme.AppIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val socialTabs = listOf("FRIENDS", "GENERAL CHAT", "LIVE TACTICAL", "WHISPER", "CLAN & CLAN CHAT")

@Composable
fun SocialAndCommsDialog(
  progression: OperatorProgressionState,
  playerCallsign: String,
  totalBlueEssence: Long,
  initialTabIdx: Int = 0,
  onAddFriend: (String) -> Unit,
  onRemoveFriend: (String) -> Unit,
  onToggleFavoriteFriend: (String) -> Unit,
  onSendChatMessage: (ChatChannel, String, String?, Boolean) -> Unit,
  onCreateClan: (String, String, String, Long) -> Unit,
  onJoinClan: (ClanData) -> Unit,
  onDonateClan: (Long) -> Unit,
  onLeaveClan: () -> Unit,
  onJoinFriendMatch: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedTabIdx by remember(initialTabIdx) { mutableIntStateOf(initialTabIdx.coerceIn(0, socialTabs.lastIndex)) }
  var selectedFriendForWhisper by remember {
    mutableStateOf(progression.friends.firstOrNull())
  }

  // Dialog state for adding friend
  var showAddFriendDialog by remember { mutableStateOf(false) }
  var addFriendCallsignDraft by remember { mutableStateOf("") }

  // Dialog state for clan creation
  var showCreateClanDialog by remember { mutableStateOf(false) }
  var clanNameDraft by remember { mutableStateOf("") }
  var clanTagDraft by remember { mutableStateOf("") }
  var clanMottoDraft by remember { mutableStateOf("") }

  // Dialog state for clan donation
  var showDonateDialog by remember { mutableStateOf(false) }

  // Text inputs for chats
  var generalChatInput by remember { mutableStateOf("") }
  var liveChatInput by remember { mutableStateOf("") }
  var whisperChatInput by remember { mutableStateOf("") }
  var clanChatInput by remember { mutableStateOf("") }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    com.example.util.SyncDialogFullscreen()
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxSize().testTag("social_and_comms_dialog"),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Header
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
                Icon(AppIcons.Group, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                Text(
                  text = "MULTIVERSE COMMUNICATIONS & SOCIAL",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Text(
                text = "Friends List (${progression.friends.size}) • Clan [${progression.clan.tag}] • Live, General & Whisper Comms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(48.dp).testTag("close_social_dialog_btn"),
            ) {
              Icon(AppIcons.Close, contentDescription = "Close Social Hub")
            }
          }
        }

        // Multi-Tab Row (Scrollable so all 5 tabs fit cleanly on compact phones)
        ScrollableTabRow(
          selectedTabIndex = selectedTabIdx,
          edgePadding = 8.dp,
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          contentColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.fillMaxWidth().testTag("social_tab_row"),
        ) {
          socialTabs.forEachIndexed { index, label ->
            Tab(
              selected = selectedTabIdx == index,
              onClick = { selectedTabIdx = index },
              text = { Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
              modifier = Modifier.testTag("social_tab_$index"),
            )
          }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (selectedTabIdx) {
            0 -> {
              // FRIENDS LIST TAB
              FriendsListTab(
                friends = progression.friends,
                onOpenAddFriend = { showAddFriendDialog = true },
                onRemoveFriend = onRemoveFriend,
                onToggleFavorite = onToggleFavoriteFriend,
                onWhisperClicked = { friend ->
                  selectedFriendForWhisper = friend
                  selectedTabIdx = 3 // Jump to Whisper tab
                },
                onJoinFriendMatch = onJoinFriendMatch,
              )
            }

            1 -> {
              // GENERAL CHAT TAB
              val generalMessages = progression.chatMessages.filter { it.channel == ChatChannel.GENERAL }
              ChatStreamTab(
                messages = generalMessages,
                channelTitle = "MULTIVERSE GENERAL CHANNEL",
                channelSubtitle = "Global communications across all universes and sectors",
                inputText = generalChatInput,
                onInputChanged = { generalChatInput = it },
                onSend = {
                  onSendChatMessage(ChatChannel.GENERAL, generalChatInput, null, false)
                  generalChatInput = ""
                },
                testTagPrefix = "general",
              )
            }

            2 -> {
              // LIVE TACTICAL CHAT TAB
              val liveMessages = progression.chatMessages.filter { it.channel == ChatChannel.LIVE_MATCH }
              LiveTacticalChatTab(
                messages = liveMessages,
                inputText = liveChatInput,
                onInputChanged = { liveChatInput = it },
                onSend = {
                  onSendChatMessage(ChatChannel.LIVE_MATCH, liveChatInput, null, false)
                  liveChatInput = ""
                },
                onQuickCallout = { callout ->
                  onSendChatMessage(ChatChannel.LIVE_MATCH, callout, null, true)
                },
              )
            }

            3 -> {
              // FRIEND DIRECT WHISPER CHAT TAB
              val currentFriend = selectedFriendForWhisper ?: progression.friends.firstOrNull()
              val whisperMessages = progression.chatMessages.filter {
                it.channel == ChatChannel.DIRECT && (it.targetFriendId == currentFriend?.id || it.senderCallsign == currentFriend?.callsign)
              }
              WhisperChatTab(
                friends = progression.friends,
                selectedFriend = currentFriend,
                onSelectFriend = { selectedFriendForWhisper = it },
                messages = whisperMessages,
                inputText = whisperChatInput,
                onInputChanged = { whisperChatInput = it },
                onSend = {
                  if (currentFriend != null) {
                    onSendChatMessage(ChatChannel.DIRECT, whisperChatInput, currentFriend.id, false)
                    whisperChatInput = ""
                  }
                },
              )
            }

            4 -> {
              // CLAN & CLAN CHAT TAB
              val clanMessages = progression.chatMessages.filter { it.channel == ChatChannel.CLAN }
              ClanAndCommsTab(
                clan = progression.clan,
                playerCallsign = playerCallsign,
                totalBlueEssence = totalBlueEssence,
                clanMessages = clanMessages,
                clanChatInput = clanChatInput,
                onClanChatInputChanged = { clanChatInput = it },
                onSendClanChat = {
                  onSendChatMessage(ChatChannel.CLAN, clanChatInput, null, false)
                  clanChatInput = ""
                },
                onOpenDonate = { showDonateDialog = true },
                onOpenCreateClan = { showCreateClanDialog = true },
                onJoinClan = onJoinClan,
                onLeaveClan = onLeaveClan,
              )
            }
          }
        }
      }
    }
  }

  // ADD FRIEND DIALOG
  if (showAddFriendDialog) {
    AlertDialog(
      onDismissRequest = { showAddFriendDialog = false },
      title = { Text("Add Multiverse Friend") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Enter an operator's Callsign or User ID to add them to your friends list.")
          OutlinedTextField(
            value = addFriendCallsignDraft,
            onValueChange = { addFriendCallsignDraft = it },
            label = { Text("Operator Callsign") },
            placeholder = { Text("e.g. Cole_Kessler, Miles_1610") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("add_friend_callsign_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (addFriendCallsignDraft.isNotBlank()) {
              onAddFriend(addFriendCallsignDraft.trim())
              addFriendCallsignDraft = ""
              showAddFriendDialog = false
            }
          },
          modifier = Modifier.testTag("confirm_add_friend_btn"),
        ) {
          Text("Send Friend Request")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddFriendDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }

  // CREATE CLAN DIALOG
  if (showCreateClanDialog) {
    AlertDialog(
      onDismissRequest = { showCreateClanDialog = false },
      title = { Text("Found New Multiverse Clan") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = clanNameDraft,
            onValueChange = { clanNameDraft = it },
            label = { Text("Clan Name") },
            placeholder = { Text("e.g. Avengers Syndicate") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("create_clan_name_input"),
          )
          OutlinedTextField(
            value = clanTagDraft,
            onValueChange = { clanTagDraft = it.uppercase() },
            label = { Text("Clan Tag (2-5 letters)") },
            placeholder = { Text("e.g. AVNG") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("create_clan_tag_input"),
          )
          OutlinedTextField(
            value = clanMottoDraft,
            onValueChange = { clanMottoDraft = it },
            label = { Text("Clan Motto") },
            placeholder = { Text("e.g. United across all realities.") },
            modifier = Modifier.fillMaxWidth().testTag("create_clan_motto_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (clanNameDraft.isNotBlank() && clanTagDraft.isNotBlank()) {
              onCreateClan(clanNameDraft.trim(), clanTagDraft.trim(), clanMottoDraft.trim(), 0xFF00E5FF)
              showCreateClanDialog = false
            }
          },
          modifier = Modifier.testTag("confirm_create_clan_btn"),
        ) {
          Text("Found Clan")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateClanDialog = false }) { Text("Cancel") }
      },
    )
  }

  // DONATE TO CLAN DIALOG
  if (showDonateDialog) {
    val donationOptions = listOf(500L, 1000L, 2500L, 5000L)
    AlertDialog(
      onDismissRequest = { showDonateDialog = false },
      title = { Text("Contribute to Clan Bank") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Donate Blue Essence to level up Clan [${progression.clan.tag}] and increase perks for all members!")
          Text("Current Clan Bank: ${progression.clan.totalEssenceDonated} Blue Essence • Level ${progression.clan.level}/25")
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            donationOptions.forEach { amount ->
              OutlinedButton(
                onClick = {
                  onDonateClan(amount)
                  showDonateDialog = false
                },
                enabled = totalBlueEssence >= amount,
                modifier = Modifier.testTag("donate_chip_$amount"),
              ) {
                Text("$amount B.Ess")
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDonateDialog = false }) { Text("Close") }
      },
    )
  }
}

@Composable
private fun FriendsListTab(
  friends: List<FriendEntry>,
  onOpenAddFriend: () -> Unit,
  onRemoveFriend: (String) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onWhisperClicked: (FriendEntry) -> Unit,
  onJoinFriendMatch: (String) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("friends_list_tab"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("OPERATOR FRIENDS (${friends.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text("Track comrade status, invite to lobbies, direct whisper, and jump into their matches.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
          onClick = onOpenAddFriend,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("add_friend_btn"),
        ) {
          Icon(AppIcons.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Add Friend")
        }
      }
    }

    if (friends.isEmpty()) {
      item {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
          Text("No friends on list yet. Click 'Add Friend' to connect with operators!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    } else {
      items(friends.distinctBy { it.id }, key = { it.id }) { friend ->
        FriendItemCard(
          friend = friend,
          onRemove = { onRemoveFriend(friend.id) },
          onToggleFavorite = { onToggleFavorite(friend.id) },
          onWhisper = { onWhisperClicked(friend) },
          onJoinMatch = {
            if (friend.currentMatchId != null) onJoinFriendMatch(friend.currentMatchId)
          },
        )
      }
    }
  }
}

@Composable
private fun FriendItemCard(
  friend: FriendEntry,
  onRemove: () -> Unit,
  onToggleFavorite: () -> Unit,
  onWhisper: () -> Unit,
  onJoinMatch: () -> Unit,
) {
  val statusColor = Color(friend.status.colorHex)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)).testTag("friend_card_${friend.id}"),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor)
          )
          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(friend.callsign, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              if (friend.clanTag != null) {
                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF00E5FF).copy(alpha = 0.2f)) {
                  Text(
                    text = "[${friend.clanTag}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                  )
                }
              }
            }
            Text(
              text = "Lv.${friend.level} • ${friend.stars}★ • Tier ${friend.tier} • ${friend.statusDetail}",
              style = MaterialTheme.typography.bodySmall,
              color = statusColor,
            )
          }
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp).testTag("fav_friend_${friend.id}")) {
          Text(text = if (friend.isFavorite) "⭐" else "☆", style = MaterialTheme.typography.titleMedium)
        }
      }

      // Action buttons
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onWhisper,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.weight(1f).height(36.dp).testTag("whisper_btn_${friend.id}"),
        ) {
          Icon(AppIcons.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(Modifier.width(4.dp))
          Text("Whisper", style = MaterialTheme.typography.labelSmall)
        }

        if (friend.currentMatchId != null) {
          Button(
            onClick = onJoinMatch,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).height(36.dp).testTag("join_friend_btn_${friend.id}"),
          ) {
            Icon(AppIcons.Public, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Join Match", style = MaterialTheme.typography.labelSmall)
          }
        }

        TextButton(
          onClick = onRemove,
          modifier = Modifier.height(36.dp).testTag("remove_friend_btn_${friend.id}"),
        ) {
          Text("Remove", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
      }
    }
  }
}

@Composable
private fun ChatStreamTab(
  messages: List<ChatMessage>,
  channelTitle: String,
  channelSubtitle: String,
  inputText: String,
  onInputChanged: (String) -> Unit,
  onSend: () -> Unit,
  testTagPrefix: String,
) {
  Column(modifier = Modifier.fillMaxSize().testTag("${testTagPrefix}_chat_tab")) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
      Column {
        Text(channelTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(channelSubtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
      reverseLayout = false,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(messages.distinctBy { it.id }, key = { it.id }) { msg ->
        ChatMessageBubble(msg = msg)
      }
    }

    // Input row
    Surface(
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = onInputChanged,
          placeholder = { Text("Send a message...") },
          modifier = Modifier.weight(1f).testTag("${testTagPrefix}_chat_input"),
          singleLine = true,
        )
        Button(
          onClick = onSend,
          enabled = inputText.isNotBlank(),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(52.dp).testTag("${testTagPrefix}_send_btn"),
        ) {
          Icon(AppIcons.Send, contentDescription = "Send Message", modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
private fun LiveTacticalChatTab(
  messages: List<ChatMessage>,
  inputText: String,
  onInputChanged: (String) -> Unit,
  onSend: () -> Unit,
  onQuickCallout: (String) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize().testTag("live_tactical_chat_tab")) {
    // Quick Tactical Callouts Bar
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      shape = RoundedCornerShape(10.dp),
    ) {
      Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("TACTICAL 1-TAP CALLOUTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(SocialCatalog.quickTacticalCallouts) { callout ->
            Button(
              onClick = { onQuickCallout(callout) },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              modifier = Modifier.height(34.dp).testTag("callout_${callout.take(8).trim()}"),
            ) {
              Text(callout, style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(messages.distinctBy { it.id }, key = { it.id }) { msg ->
        ChatMessageBubble(msg = msg)
      }
    }

    // Input row
    Surface(
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = onInputChanged,
          placeholder = { Text("Tactical live combat message...") },
          modifier = Modifier.weight(1f).testTag("live_chat_input"),
          singleLine = true,
        )
        Button(
          onClick = onSend,
          enabled = inputText.isNotBlank(),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(52.dp).testTag("live_send_btn"),
        ) {
          Icon(AppIcons.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
private fun WhisperChatTab(
  friends: List<FriendEntry>,
  selectedFriend: FriendEntry?,
  onSelectFriend: (FriendEntry) -> Unit,
  messages: List<ChatMessage>,
  inputText: String,
  onInputChanged: (String) -> Unit,
  onSend: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize().testTag("whisper_chat_tab")) {
    // Select friend row
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      shape = RoundedCornerShape(10.dp),
    ) {
      Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("SELECT FRIEND TO WHISPER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(friends.distinctBy { it.id }, key = { it.id }) { friend ->
            FilterChip(
              selected = selectedFriend?.id == friend.id,
              onClick = { onSelectFriend(friend) },
              label = { Text(friend.callsign) },
              modifier = Modifier.testTag("select_whisper_${friend.id}"),
            )
          }
        }
      }
    }

    if (selectedFriend == null) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text("Select a friend above to begin 1-on-1 private messaging.", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(messages.distinctBy { it.id }, key = { it.id }) { msg ->
          ChatMessageBubble(msg = msg)
        }
      }
    }

    // Input row
    Surface(
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = onInputChanged,
          placeholder = { Text(if (selectedFriend != null) "Whisper to ${selectedFriend.callsign}..." else "Select a friend first") },
          enabled = selectedFriend != null,
          modifier = Modifier.weight(1f).testTag("whisper_input"),
          singleLine = true,
        )
        Button(
          onClick = onSend,
          enabled = selectedFriend != null && inputText.isNotBlank(),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(52.dp).testTag("whisper_send_btn"),
        ) {
          Icon(AppIcons.Send, contentDescription = "Send Whisper", modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
private fun ClanAndCommsTab(
  clan: ClanData,
  playerCallsign: String,
  totalBlueEssence: Long,
  clanMessages: List<ChatMessage>,
  clanChatInput: String,
  onClanChatInputChanged: (String) -> Unit,
  onSendClanChat: () -> Unit,
  onOpenDonate: () -> Unit,
  onOpenCreateClan: () -> Unit,
  onJoinClan: (ClanData) -> Unit,
  onLeaveClan: () -> Unit,
) {
  var showClanRoster by remember { mutableStateOf(false) }
  var showRecommendedClans by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().testTag("clan_and_comms_tab")) {
    // Clan Profile Banner
    Surface(
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().border(1.dp, Color(clan.colorHex).copy(alpha = 0.5f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(shape = RoundedCornerShape(6.dp), color = Color(clan.colorHex).copy(alpha = 0.2f)) {
                Text(
                  text = "[${clan.tag}]",
                  style = MaterialTheme.typography.titleMedium,
                  color = Color(clan.colorHex),
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
              Text(clan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("\"${clan.motto}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          Surface(shape = RoundedCornerShape(8.dp), color = Color(clan.colorHex).copy(alpha = 0.18f)) {
            Text(
              text = "LVL ${clan.level}/25 • T${clan.tier} • ${clan.starLevel}★",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = Color(clan.colorHex),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }

        // Active Perks row
        Text(
          text = "Clan Perks: +${(clan.level * 5)}% Blue Essence • +${(clan.level * 3)}% Wonder Drops • +${clan.armorSiphonPerk} Shield Siphon • +${clan.botDamageBoost}% Bot DMG",
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFF00E676),
          fontWeight = FontWeight.Bold,
        )

        // Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onOpenDonate,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).height(36.dp).testTag("donate_clan_btn"),
          ) {
            Text("Contribute Essence", style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = { showClanRoster = !showClanRoster },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).height(36.dp).testTag("toggle_roster_btn"),
          ) {
            Text(if (showClanRoster) "Hide Roster" else "Roster (${clan.members.size})", style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = { showRecommendedClans = !showRecommendedClans },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).height(36.dp).testTag("browse_clans_btn"),
          ) {
            Text("Browse / Join", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    if (showClanRoster) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
      ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("CLAN MEMBERS ROSTER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          clan.members.forEach { m ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("${m.callsign} (${m.rank.title})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
              Text("Lv.${m.level} • ${m.stars}★ • ${m.donatedEssence} B.Donated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }

    if (showRecommendedClans) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
      ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("MULTIVERSE CLANS DIRECTORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              if (clan.id != SocialCatalog.defaultClan.id) {
                TextButton(onClick = onLeaveClan, modifier = Modifier.height(28.dp).testTag("leave_clan_btn")) {
                  Text("Leave Clan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
              }
              TextButton(onClick = onOpenCreateClan, modifier = Modifier.height(28.dp).testTag("open_create_clan_btn")) {
                Text("+ Create Clan", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
          SocialCatalog.recommendedClans.forEach { recClan ->
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text("[${recClan.tag}] ${recClan.name}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text("Lv.${recClan.level} • ${recClan.members.size} members", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              if (recClan.id != clan.id) {
                OutlinedButton(
                  onClick = { onJoinClan(recClan) },
                  shape = RoundedCornerShape(6.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp).testTag("join_clan_${recClan.tag}"),
                ) {
                  Text("Join", style = MaterialTheme.typography.labelSmall)
                }
              } else {
                Text("Current Clan", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E676))
              }
            }
          }
        }
      }
    }

    // Clan Chat Stream
    Text(
      text = "CLAN COMMS & SYSTEM ANNOUNCEMENTS",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(clanMessages.distinctBy { it.id }, key = { it.id }) { msg ->
        ChatMessageBubble(msg = msg)
      }
    }

    // Clan chat input
    Surface(
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = clanChatInput,
          onValueChange = onClanChatInputChanged,
          placeholder = { Text("Message clan members...") },
          modifier = Modifier.weight(1f).testTag("clan_chat_input"),
          singleLine = true,
        )
        Button(
          onClick = onSendClanChat,
          enabled = clanChatInput.isNotBlank(),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(52.dp).testTag("clan_send_btn"),
        ) {
          Icon(AppIcons.Send, contentDescription = "Send Clan Comms", modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
private fun ChatMessageBubble(msg: ChatMessage) {
  val isSystem = msg.isSystemMessage
  val timeFormatted = remember(msg.timestampMillis) {
    SimpleDateFormat("HH:mm", Locale.US).format(Date(msg.timestampMillis))
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (isSystem) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else if (msg.isTacticalQuickChat) Color(0xFFFF9100).copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surfaceVariant,
    modifier = Modifier.fillMaxWidth().testTag("chat_bubble_${msg.id}"),
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (msg.senderClanTag != null) {
            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF00E5FF).copy(alpha = 0.2f)) {
              Text(
                text = "[${msg.senderClanTag}]",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
              )
            }
          }
          Text(msg.senderCallsign, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          if (msg.senderStars > 1) {
            Text("${msg.senderStars}★", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD54F))
          }
        }
        Text(timeFormatted, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Text(
        text = msg.content,
        style = MaterialTheme.typography.bodyMedium,
        color = if (msg.isTacticalQuickChat) Color(0xFFFF9100) else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (msg.isTacticalQuickChat || isSystem) FontWeight.SemiBold else FontWeight.Normal,
      )
    }
  }
}

package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import com.example.export.SteamWorkshopImportPreview
import com.example.ui.theme.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.CloudSyncState
import com.example.data.local.SaveBundlePreview
import com.example.data.model.ArmoryItem
import com.example.data.model.LinkedAccount
import com.example.data.model.MatchRecord
import com.example.data.model.OperatorProfile
import com.example.data.model.OriginalVaultCatalog
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.data.model.UniverseWorld
import com.example.ui.auth.CrossPlatformAccountsCard

@Composable
fun OsDesktopScreen(
  profile: OperatorProfile,
  armory: List<ArmoryItem>,
  rigs: List<RigAsset>,
  matchLogs: List<MatchRecord>,
  linkedAccounts: List<LinkedAccount>,
  cloudSyncState: CloudSyncState = CloudSyncState.LocalOnly,
  isCloudSignedIn: Boolean = false,
  googleAccountLabel: String? = null,
  isGoogleSigningIn: Boolean = false,
  onSignInGoogle: (() -> Unit)? = null,
  onSyncNow: () -> Unit = {},
  onRequestSaveExportJson: ((String) -> Unit) -> Unit = {},
  onParseSaveJson: (String) -> Result<SaveBundlePreview> = { Result.failure(IllegalStateException("Unavailable")) },
  onConfirmSaveImport: (SaveBundlePreview, Boolean) -> Unit = { _, _ -> },
  onConfirmWorkshopImport: (SteamWorkshopImportPreview, Boolean, Boolean) -> Unit = { _, _, _ -> },
  activeWallpaperPath: String?,
  onSelectUniverse: (String) -> Unit,
  onSelectMode: (String) -> Unit,
  onLaunchArena: () -> Unit,
  onOpenForgeRig: () -> Unit,
  onOpenMediaVault: () -> Unit,
  onOpenExport: () -> Unit,
  onCycleWallpaper: () -> Unit,
  onUpdateCallsign: (String) -> Unit,
  onLinkAccount: (String, String, String) -> Unit = { _, _, _ -> },
  onUnlinkAccount: (String) -> Unit,
  onStatusMessage: (String) -> Unit,
  onOpenSettings: () -> Unit = {},
  onSignOut: () -> Unit,
) {
  val activeWorld = UniverseCatalog.worldById(profile.activeUniverseId)
  val activeMode = UniverseCatalog.modeById(profile.activeMode)
  val activeConsole = UniverseCatalog.consoleById(profile.consolePreset)
  val activeRig = rigs.firstOrNull { it.id == profile.activeRigId } ?: rigs.firstOrNull()
  val equippedCount = armory.count { it.equipped }

  var showCallsignDialog by remember { mutableStateOf(false) }
  var callsignDraft by remember(profile.callsign) { mutableStateOf(profile.callsign) }

  val wallpaperBitmap = rememberAssetImageBitmap(activeWallpaperPath, maxDim = 960)
  val wallpaperGradient = when (profile.osWallpaperIndex.toInt() % 4) {
    1 -> listOf(Color(0xFF0A0718), Color(0xFF1A0F36), Color(0xFF070A12))
    2 -> listOf(Color(0xFF14070D), Color(0xFF280D1B), Color(0xFF070A12))
    3 -> listOf(Color(0xFF071418), Color(0xFF0E2933), Color(0xFF070A12))
    else -> listOf(Color(0xFF070A12), Color(0xFF0F1A33), Color(0xFF070A12))
  }

  Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(wallpaperGradient))) {
    if (wallpaperBitmap != null) {
      Image(
        bitmap = wallpaperBitmap,
        contentDescription = "UnknownOS custom wallpaper",
        contentScale = ContentScale.Crop,
        alpha = 0.24f,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Brush.verticalGradient(listOf(Color(0x70070A12), Color(0xC8070A12)))),
      )
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // App Title Header & UnknownOS Kernel Status
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        modifier = Modifier.fillMaxWidth()
          .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "THE UNKNOWN UNIVERSES",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                  modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00E676))
                )
                Text(
                  text = "UNKNOWNOS v2.4 // ${profile.callsign.uppercase()}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                  onClick = { showCallsignDialog = true },
                  modifier = Modifier.size(28.dp).testTag("edit_callsign_btn"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit callsign",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
              Text(
                text = "ID: ${BuildConfig.APPLICATION_ID} • Essence: ${profile.totalEssence} • Best: ${profile.highScore} • ${activeConsole.label}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(48.dp).testTag("open_settings_button"),
              ) {
                Icon(
                  imageVector = AppIcons.Settings,
                  contentDescription = stringResource(R.string.cd_open_settings),
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
              IconButton(
                onClick = onCycleWallpaper,
                modifier = Modifier.size(48.dp).testTag("cycle_wallpaper_btn"),
              ) {
                Icon(
                  imageVector = AppIcons.Palette,
                  contentDescription = "Cycle OS wallpaper",
                  tint = MaterialTheme.colorScheme.tertiary,
                )
              }
              IconButton(
                onClick = onSignOut,
                modifier = Modifier.size(48.dp).testTag("sign_out_button"),
              ) {
                Icon(
                  imageVector = AppIcons.Logout,
                  contentDescription = stringResource(R.string.cd_sign_out),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          // UnknownOS Window Dock Strip
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            OsDockChip(
              modifier = Modifier.weight(1f),
              label = "ARENA",
              accent = Color(activeWorld.accentHex),
              onClick = onLaunchArena,
            )
            OsDockChip(
              modifier = Modifier.weight(1f),
              label = "BLENDER",
              accent = Color(0xFFFF8F00),
              onClick = onOpenForgeRig,
            )
            OsDockChip(
              modifier = Modifier.weight(1f),
              label = "VAULT",
              accent = Color(0xFF00E5FF),
              onClick = onOpenMediaVault,
            )
            OsDockChip(
              modifier = Modifier.weight(1f),
              label = "EXPORT",
              accent = Color(0xFF00E676),
              onClick = onOpenExport,
            )
          }
        }
      }
    }

    // Active Universe Hero Box-Art Card
    item {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
          .border(1.5.dp, Color(activeWorld.accentHex).copy(alpha = 0.68f), RoundedCornerShape(22.dp)),
      ) {
        Column {
          Box(modifier = Modifier.fillMaxWidth().height(196.dp)) {
            Image(
              painter = painterResource(id = activeWorld.bannerRes),
              contentDescription = stringResource(R.string.cd_world_banner),
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
            )
            Box(
              modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0x28070A12), Color(0xEE070A12))))
            )
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = Color(0xCC0A0F1A),
              modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                .border(1.dp, Color(activeWorld.accentHex).copy(alpha = 0.7f), RoundedCornerShape(999.dp)),
            ) {
              Text(
                text = activeWorld.platformBadges,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
              )
            }
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = Color(activeMode.accentHex).copy(alpha = 0.24f),
              modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                .border(1.dp, Color(activeMode.accentHex), RoundedCornerShape(999.dp)),
            ) {
              Text(
                text = "${activeMode.shortTag} • ${activeMode.label}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(activeMode.accentHex),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
              Text(
                text = activeWorld.inspirations.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(activeWorld.accentHex),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = activeWorld.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
              )
            }
          }

          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = activeWorld.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(activeWorld.accentHex).copy(alpha = 0.12f),
              modifier = Modifier.fillMaxWidth()
                .border(1.dp, Color(activeWorld.accentHex).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "SIGNATURE: ${activeWorld.signatureMechanic}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = "[${activeWorld.interactActionLabel}]",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(activeWorld.accentHex),
                )
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = onLaunchArena,
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(activeWorld.accentHex),
                  contentColor = Color(0xFF041016),
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(50.dp).testTag("launch_arena_button"),
              ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.launch_arena), style = MaterialTheme.typography.labelLarge)
              }
              OutlinedButton(
                onClick = onOpenForgeRig,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(50.dp).testTag("open_forgerig_button"),
              ) {
                Icon(AppIcons.Animation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("UnknownBlend", style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }
      }
    }

    // Quick OS Telemetry Tiles
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OsStatTile(
          modifier = Modifier.weight(1f),
          icon = AppIcons.Shield,
          label = "Equipped",
          value = "$equippedCount Modules",
          accent = MaterialTheme.colorScheme.primary,
        )
        OsStatTile(
          modifier = Modifier.weight(1f).clickable { onOpenForgeRig() },
          icon = AppIcons.Animation,
          label = "Blender Rig",
          value = activeRig?.archetype?.uppercase() ?: "OPERATOR",
          accent = Color(0xFFFF8F00),
        )
        OsStatTile(
          modifier = Modifier.weight(1f).clickable { onOpenExport() },
          icon = AppIcons.Gamepad,
          label = "Compat Build",
          value = activeConsole.id.substringBefore("_").uppercase(),
          accent = Color(0xFF00E676),
        )
      }
    }

    // Settings, Performance, Controls & Accessibility Support Card
    item {
      Surface(
        onClick = onOpenSettings,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, Color(0xFF00E676).copy(alpha = 0.55f), RoundedCornerShape(18.dp))
          .testTag("settings_accessibility_card"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = AppIcons.Accessibility,
            contentDescription = stringResource(R.string.cd_open_settings),
            tint = Color(0xFF00E676),
            modifier = Modifier.size(28.dp),
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.settings_title),
              style = MaterialTheme.typography.titleMedium,
              color = Color(0xFF00E676),
            )
            Text(
              text = "Tune controls, FPS, visuals, epilepsy flash safety, colorblind palettes, large text & motor assist.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Icon(
            imageVector = AppIcons.Tune,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }

    // Original Media Vault Showcase Card
    item {
      Surface(
        onClick = onOpenMediaVault,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.55f), RoundedCornerShape(18.dp))
          .testTag("open_media_vault_card"),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "ORIGINAL MEDIA VAULT & TRAILERS",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF00E5FF),
              )
              Text(
                text = "${OriginalVaultCatalog.artworks.size} Hand-Drawn, Krita & PENUP Artworks • ${OriginalVaultCatalog.videos.size} Original Videos • 8 Series",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Icon(
              imageVector = AppIcons.VideoLibrary,
              contentDescription = null,
              tint = Color(0xFF00E5FF),
              modifier = Modifier.size(22.dp),
            )
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(OriginalVaultCatalog.artworks.take(6), key = { it.id }) { art ->
              val thumb = rememberAssetImageBitmap(art.assetPath, maxDim = 220)
              Box(
                modifier = Modifier
                  .size(width = 86.dp, height = 62.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color(0xFF090E1B))
                  .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
              ) {
                if (thumb != null) {
                  Image(
                    bitmap = thumb,
                    contentDescription = art.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              }
            }
          }
        }
      }
    }

    // Multiverse Worlds Collection
    item {
      Text(
        text = "THE UNKNOWN UNIVERSES COLLECTION (${UniverseCatalog.worlds.size} WORLDS)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }

    item {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(UniverseCatalog.worlds, key = { it.id }) { world ->
          UniverseCollectionCard(
            world = world,
            selected = world.id == profile.activeUniverseId,
            onClick = { onSelectUniverse(world.id) },
          )
        }
      }
    }

    // Game Modes Switcher
    item {
      Text(
        text = "DEPLOYMENT MODES (ZOMBIES • DMZ • BR • MP • SANDBOX)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(UniverseCatalog.modes, key = { it.id }) { mode ->
          val isSelected = mode.id == profile.activeMode
          Surface(
            onClick = { onSelectMode(mode.id) },
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) Color(mode.accentHex).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(mode.accentHex) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
              )
              .testTag("mode_chip_${mode.id}"),
          ) {
            Column(modifier = Modifier.width(176.dp).padding(12.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = AppIcons.Bolt,
                  contentDescription = null,
                  tint = Color(mode.accentHex),
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = mode.label,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Spacer(Modifier.height(4.dp))
              Text(
                text = mode.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }

    // UnknownOS Kernel Console & Match History
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = AppIcons.Terminal,
              contentDescription = null,
              tint = Color(0xFF00E676),
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "UNKNOWNOS KERNEL & RIG DIAGNOSTICS",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          val completedRigs = rigs.count { it.isCompleted }
          Text(
            text = "> Package: ${BuildConfig.APPLICATION_ID} (v${BuildConfig.VERSION_NAME}) | Rigs: $completedRigs/${rigs.size} | IK: ONLINE",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF00E676),
          )
          Text(
            text = "> Active Sectors: ${activeWorld.mapSectors.joinToString(" • ")}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (matchLogs.isEmpty()) {
            Text(
              text = "> No arena deployments recorded yet. Tap 'Deploy to Arena' to play!",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.tertiary,
            )
          } else {
            matchLogs.take(3).forEach { log ->
              val w = UniverseCatalog.worldById(log.universeId)
              val status = if (log.extracted) "EXTRACTED" else "WAVE ${log.waveReached}"
              Text(
                text = "> [${log.mode.uppercase()}] ${w.title.substringBefore(":")} • Score ${log.score} • ${log.eliminations} Elims ($status)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }

    // Cloud Sync & Shareable Save Data Card
    item {
      CloudSyncAndSaveCard(
        callsign = profile.callsign,
        cloudSyncState = cloudSyncState,
        isCloudSignedIn = isCloudSignedIn,
        onSyncNow = onSyncNow,
        onRequestSaveExportJson = onRequestSaveExportJson,
        onParseSaveJson = onParseSaveJson,
        onConfirmSaveImport = onConfirmSaveImport,
        onStatusMessage = onStatusMessage,
      )
    }

    // Cross-Platform Store & Console Accounts Card
    item {
      CrossPlatformAccountsCard(
        linkedAccounts = linkedAccounts,
        googleAccountLabel = googleAccountLabel,
        isGoogleSigningIn = isGoogleSigningIn,
        onSignInGoogle = onSignInGoogle,
        onSignOutGoogle = onSignOut,
        onLinkExternal = onLinkAccount,
        onUnlinkExternal = onUnlinkAccount,
        onStatusMessage = onStatusMessage,
      )
    }

    // Steam Workshop Export & Import Card
    item {
      SteamWorkshopCard(
        profile = profile,
        armory = armory,
        rigs = rigs,
        linkedSteamAccount = linkedAccounts.firstOrNull { it.providerId == "steam" },
        onConfirmWorkshopImport = onConfirmWorkshopImport,
        onStatusMessage = onStatusMessage,
        onOpenBo3Export = onOpenExport,
      )
    }
  }
  }

  if (showCallsignDialog) {
    AlertDialog(
      onDismissRequest = { showCallsignDialog = false },
      title = { Text("Update Operator Callsign") },
      text = {
        OutlinedTextField(
          value = callsignDraft,
          onValueChange = { callsignDraft = it.take(30) },
          label = { Text("Callsign") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (callsignDraft.trim().length >= 2) {
              onUpdateCallsign(callsignDraft)
              showCallsignDialog = false
            }
          }
        ) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = { showCallsignDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun OsDockChip(
  modifier: Modifier,
  label: String,
  accent: Color,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(10.dp),
    color = accent.copy(alpha = 0.14f),
    modifier = modifier.border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun OsStatTile(
  modifier: Modifier,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
  accent: Color,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Composable
private fun UniverseCollectionCard(
  world: UniverseWorld,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .width(248.dp)
      .border(
        width = if (selected) 2.dp else 1.dp,
        color = if (selected) Color(world.accentHex) else MaterialTheme.colorScheme.outline,
        shape = RoundedCornerShape(18.dp),
      )
      .testTag("world_card_${world.id}"),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().height(108.dp)) {
        Image(
          painter = painterResource(id = world.bannerRes),
          contentDescription = world.title,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
        Box(
          modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0x28070A12), Color(0xBB070A12)))
          )
        )
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = Color(0xCC0A0F1A),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            .border(1.dp, Color(world.accentHex).copy(alpha = 0.65f), RoundedCornerShape(999.dp)),
        ) {
          Text(
            text = world.interactActionLabel,
            style = MaterialTheme.typography.labelSmall,
            color = Color(world.accentHex),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }
        if (selected) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color(world.accentHex),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(AppIcons.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
              Text("MOUNTED", style = MaterialTheme.typography.labelSmall, color = Color.Black)
            }
          }
        }
        Text(
          text = world.platformBadges,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFE2E8F0),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 6.dp),
        )
      }
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = world.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = world.inspirations,
          style = MaterialTheme.typography.labelSmall,
          color = Color(world.accentHex),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = world.signatureMechanic,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

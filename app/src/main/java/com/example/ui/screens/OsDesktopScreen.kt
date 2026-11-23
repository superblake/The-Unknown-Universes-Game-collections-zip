package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.CloudSyncState
import com.example.data.local.SaveBundlePreview
import com.example.data.model.AchievementState
import com.example.data.model.ArmoryItem
import com.example.data.model.CollectionCustomModeConfig
import com.example.data.model.CollectionDifficulty
import com.example.data.model.FriendOnlineStatus
import com.example.data.model.LinkedAccount
import com.example.data.model.LobbyConfig
import com.example.data.model.MatchRecord
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.NewGamePlusVersion
import com.example.data.model.OperatorProfile
import com.example.data.model.OperatorProgressionState
import com.example.data.model.OriginalVaultCatalog
import com.example.data.model.ProgressionCatalog
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.data.model.UniverseWorld
import com.example.ui.auth.CrossPlatformAccountsCard
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
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
  progression: OperatorProgressionState = OperatorProgressionState(),
  achievements: List<AchievementState> = emptyList(),
  onUpdateLobby: ((LobbyConfig) -> LobbyConfig) -> Unit = {},
  onOpenStore: () -> Unit = {},
  onOpenAchievements: () -> Unit = {},
  onOpenMissions: () -> Unit = {},
  onOpenMatchBrowser: (Int) -> Unit = {},
  onOpenSocialHub: (Int) -> Unit = {},
  onEquipOutfit: (String) -> Unit = {},
  onSelectArea: (String) -> Unit = {},
  onHuntHeroFrontier: () -> Unit = {},
  onSelectCollectionDifficulty: (String, CollectionDifficulty) -> Unit = { _, _ -> },
  onSelectNgPlusTier: (Int) -> Unit = {},
  onCompleteNgPlusRun: () -> Unit = {},
  onApplyProtocolCode: (String) -> Unit = {},
  onBoostAllTiers: (Int) -> Unit = {},
  onMaxAllTiers: () -> Unit = {},
  onMaxAllStars: () -> Unit = {},
  onGrantEssenceBonus: () -> Unit = {},
  onImportBo3Preset: () -> Unit = {},
  onDeployArea: (String) -> Unit = { areaId -> onSelectArea(areaId); onLaunchArena() },
  onUpdateCollectionCustomMode: ((CollectionCustomModeConfig) -> CollectionCustomModeConfig) -> Unit = {},
  onDeployCollectionCustomMode: () -> Unit = {},
  onSignOut: () -> Unit,
) {
  val activeWorld = UniverseCatalog.worldById(profile.activeUniverseId)
  val activeMode = UniverseCatalog.modeById(profile.activeMode)
  val activeConsole = UniverseCatalog.consoleById(profile.consolePreset)
  val activeRig = rigs.firstOrNull { it.id == profile.activeRigId } ?: rigs.firstOrNull()
  val equippedCount = armory.count { it.equipped }

  var showCallsignDialog by remember { mutableStateOf(false) }
  var showLobbyRosterDialog by remember { mutableStateOf(false) }
  var showCodesDialog by remember { mutableStateOf(false) }
  var showCustomModeDialog by remember { mutableStateOf(false) }
  var callsignDraft by remember(profile.callsign) { mutableStateOf(profile.callsign) }
  val claimableAchievements = achievements.count { it.unlocked && !it.claimed }
  val haptic = LocalHapticFeedback.current

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
    modifier = Modifier.fillMaxSize().testTag("os_desktop_list"),
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
                  modifier = Modifier.size(48.dp).testTag("edit_callsign_btn"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit callsign",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
              val activeWeapon = ProgressionCatalog.weaponById(progression.weaponCustom.equippedWeaponId)
              val activeOutfit = progression.activeOutfitSpec()
              val activeAreaSpec = progression.activeAreaSpec()
              val activeAreaState = progression.areaState(activeAreaSpec.id)
              val resonance = progression.essenceResonance(profile.totalEssence)
              val powerLight = progression.computePowerAndLightLevel(profile.totalEssence)
              val activeRarity = progression.outfitRarity(activeOutfit.id)
              Text(
                text = "PWR ${powerLight.powerLevel} • LT ${powerLight.lightLevel} (${powerLight.tierLabel}) • ${progression.levelCustom.worldDifficultyTier.label.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F),
                modifier = Modifier.testTag("os_power_light_badge"),
              )
              Text(
                text = "LV ${progression.playerLevel} (${progression.playerRankTitle}) • ${activeOutfit.nameWithVersion} [${activeRarity.label}] • ${activeWeapon.name}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(progression.activePassVersion.accentHex),
                modifier = Modifier.testTag("os_level_rank_badge"),
              )
              Text(
                text = "Blue Essence: ${profile.totalEssence} • ${progression.activePassVersion.title.substringBefore(":")} • Best: ${profile.highScore} • ${activeConsole.label}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00E5FF),
                modifier = Modifier.testTag("os_blue_essence_text"),
              )
              Text(
                text = "R:${progression.redEssence} Y:${progression.yellowEssence} G:${progression.greenEssence} Prism:${progression.combinedEssence} • Shards:${progression.blastShards} • ${activeAreaSpec.name.substringBefore(" (")} (${activeAreaState.ageYears}y ${activeAreaState.currentEra.name})",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE040FB),
              )
              Text(
                text = "Resonance ${resonance.summary} • Vacuum Lv.${progression.vacuumBot.level}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00E676),
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
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp),
              label = "ARENA",
              accent = Color(activeWorld.accentHex),
              onClick = onLaunchArena,
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 78.dp).testTag("os_tier_codes_btn"),
              label = "⚡ CODES",
              accent = Color(0xFF6200EE),
              onClick = { showCodesDialog = true },
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp).testTag("os_dock_store"),
              label = "STORE",
              accent = Color(0xFF00E5FF),
              onClick = onOpenStore,
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp).testTag("os_dock_achievements"),
              label = if (claimableAchievements > 0) "ACH ($claimableAchievements)" else "ACHIEVE",
              accent = Color(0xFFFFD54F),
              onClick = onOpenAchievements,
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp).testTag("os_dock_missions"),
              label = "MISSIONS",
              accent = Color(0xFF00E676),
              onClick = onOpenMissions,
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp),
              label = "BLENDER",
              accent = Color(0xFFFF8F00),
              onClick = onOpenForgeRig,
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp),
              label = "VAULT",
              accent = Color(0xFFE040FB),
              onClick = onOpenMediaVault,
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp),
              label = "EXPORT",
              accent = Color(0xFF00E676),
              onClick = onOpenExport,
            )
          }
        }
      }
    }

    item {
      @OptIn(ExperimentalFoundationApi::class)
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(16.dp))
          .combinedClickable(
            onClick = onOpenMissions,
            onLongClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onOpenMissions()
            },
          )
          .border(1.5.dp, Color(0xFF00E676).copy(alpha = 0.65f), RoundedCornerShape(16.dp))
          .testTag("os_missions_card"),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("${com.example.data.model.MissionsCatalog.allMissions.size} MULTIVERSE & CAMPAIGN MISSIONS", style = MaterialTheme.typography.titleSmall, color = Color(0xFF00E676))
          Text(
            text = "45 Missions per ${UniverseCatalog.worlds.size} Worlds & ${MultiverseArsenalCatalog.customAreas.size} Arenas • Gotham, Spider-Verse & Frontier Ready",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
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
      @OptIn(ExperimentalFoundationApi::class)
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
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
              onClick = onOpenForgeRig,
              onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onOpenForgeRig()
              },
            ),
          icon = AppIcons.Animation,
          label = "Blender Rig",
          value = activeRig?.archetype?.uppercase() ?: "OPERATOR",
          accent = Color(0xFFFF8F00),
        )
        OsStatTile(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
              onClick = onOpenExport,
              onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onOpenExport()
              },
            ),
          icon = AppIcons.Gamepad,
          label = "Compat Build",
          value = activeConsole.id.substringBefore("_").uppercase(),
          accent = Color(0xFF00E676),
        )
      }
    }

    // Playable Original, Batman, Spider-Man & Crossover Characters Quick-Swap Strip
    item {
      var selectedDesktopGroup by remember { mutableStateOf(com.example.data.model.CharacterUniverseGroup.ALL) }
      var originalsOnlyFilter by remember { mutableStateOf(false) }
      val heroList = remember(selectedDesktopGroup, originalsOnlyFilter) {
        when {
          originalsOnlyFilter -> MultiverseArsenalCatalog.outfits.filter { it.isOriginalCharacter }
          selectedDesktopGroup == com.example.data.model.CharacterUniverseGroup.ALL -> MultiverseArsenalCatalog.outfits
          else -> MultiverseArsenalCatalog.outfits.filter { it.characterGroup == selectedDesktopGroup }
        }
      }
      val activeSpec = progression.activeOutfitSpec()
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, Color(activeSpec.secondaryHex).copy(alpha = 0.65f), RoundedCornerShape(18.dp))
          .testTag("os_character_quick_swap_card"),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "PLAYABLE CHARACTERS • ${activeSpec.nameWithVersion}",
                style = MaterialTheme.typography.titleSmall,
                color = Color(activeSpec.secondaryHex),
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = if (activeSpec.isOriginalCharacter) {
                  "Original Hero Resonance ACTIVE (+25% Power & Shield) • ${activeSpec.originTag}"
                } else {
                  "${activeSpec.originTag} • +${activeSpec.baseShieldBonus} SHD / +${activeSpec.baseDamageBonus} DMG"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (activeSpec.isOriginalCharacter) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedButton(
                onClick = onHuntHeroFrontier,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(48.dp).testTag("os_hero_frontier_btn"),
              ) {
                Text("Hero Frontier", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E5FF))
              }
              OutlinedButton(
                onClick = { originalsOnlyFilter = !originalsOnlyFilter },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(48.dp).testTag("os_toggle_all_characters_btn"),
              ) {
                Text(if (originalsOnlyFilter) "All ${MultiverseArsenalCatalog.outfits.size}" else "Originals", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.testTag("os_char_filter_row"),
          ) {
            items(com.example.data.model.CharacterUniverseGroup.entries, key = { it.id }) { grp ->
              val chipSelected = !originalsOnlyFilter && selectedDesktopGroup == grp
              androidx.compose.material3.FilterChip(
                selected = chipSelected,
                onClick = {
                  originalsOnlyFilter = false
                  selectedDesktopGroup = grp
                },
                label = { Text(grp.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("os_char_filter_${grp.id}"),
              )
            }
          }
          @OptIn(ExperimentalFoundationApi::class)
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("os_chars_row"),
          ) {
            items(heroList, key = { it.id }) { spec ->
              val isSelected = progression.equippedOutfitId == spec.id
              val rarity = progression.outfitRarity(spec.id)
              val accent = Color(spec.secondaryHex)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                  .width(178.dp)
                  .heightIn(min = 68.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .combinedClickable(
                    onClick = {
                      onEquipOutfit(spec.id)
                      onStatusMessage("Selected Hero: ${spec.nameWithVersion}")
                    },
                    onLongClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onEquipOutfit(spec.id)
                      onStatusMessage("Held & Equipped: ${spec.nameWithVersion}")
                    },
                  )
                  .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp),
                  )
                  .testTag("os_quick_char_${spec.id}"),
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "${spec.originTag.uppercase()} • ${rarity.label}",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color(rarity.colorHex),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f),
                    )
                    if (spec.isOriginalCharacter) {
                      Text("+25%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                    }
                  }
                  Text(
                    text = spec.nameWithVersion,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = if (isSelected) "EQUIPPED (${progression.outfitShards(spec.id)} Shards)" else "+${spec.baseShieldBonus} SHD • +${spec.baseDamageBonus} DMG",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }

    // Solo / Duos / Squads + Over-8-Player Lobby & AI Bots Card
    item {
      LobbyQuickCard(
        lobbyConfig = progression.lobbyConfig,
        playerCallsign = profile.callsign,
        playerLevel = progression.playerLevel,
        playerPrestige = progression.playerPrestige,
        accentColor = Color(activeWorld.accentHex),
        onUpdateLobby = onUpdateLobby,
        onOpenFullRoster = { showLobbyRosterDialog = true },
        onOpenMatchBrowser = { onOpenMatchBrowser(0) },
        onOpenHostPrivate = { onOpenMatchBrowser(4) },
      )
    }

    // Multiverse Communications & Social Hub Card
    item {
      val onlineFriends = progression.friends.count { it.status != FriendOnlineStatus.OFFLINE }
      val latestMsg = progression.chatMessages.lastOrNull()
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.65f), RoundedCornerShape(18.dp))
          .testTag("os_social_card"),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(AppIcons.Group, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                Text("COMMS, FRIENDS & CLANS", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
              }
              Text(
                text = "${progression.friends.size} Friends ($onlineFriends Online) • Clan [${progression.clan.tag}] ${progression.clan.name} (Lv.${progression.clan.level})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF00E5FF).copy(alpha = 0.2f)) {
              Text(
                text = "[${progression.clan.tag}] LV.${progression.clan.level}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("os_clan_badge"),
              )
            }
          }

          if (latestMsg != null) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text("💬", style = MaterialTheme.typography.bodySmall)
                Text(
                  text = "${latestMsg.senderCallsign}: ${latestMsg.content}",
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { onOpenSocialHub(0) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("os_open_friends_btn"),
            ) {
              Icon(AppIcons.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Friends (${progression.friends.size})", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = { onOpenSocialHub(1) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("os_open_chat_btn"),
            ) {
              Icon(AppIcons.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Live Chat", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = { onOpenSocialHub(4) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("os_open_clan_btn"),
            ) {
              Icon(AppIcons.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Clan [${progression.clan.tag}]", style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }

    // Blue Essence Store & Achievements Menu Quick Row
    item {
      @OptIn(ExperimentalFoundationApi::class)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
              onClick = onOpenStore,
              onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onOpenStore()
              },
            )
            .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.65f), RoundedCornerShape(16.dp))
            .testTag("os_store_card"),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("BLUE ESSENCE STORE", style = MaterialTheme.typography.titleSmall, color = Color(0xFF00E5FF))
            Text(
              text = "${profile.totalEssence} Blue Essence • XP Boosters, Tier Catalysts & Bot Uplinks",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
              onClick = onOpenAchievements,
              onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onOpenAchievements()
              },
            )
            .border(1.5.dp, Color(0xFFFFD54F).copy(alpha = 0.65f), RoundedCornerShape(16.dp))
            .testTag("os_achievements_card"),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val unlockedAch = achievements.count { it.unlocked }
            Text("ACHIEVEMENTS ($unlockedAch/${achievements.size.coerceAtLeast(18)})", style = MaterialTheme.typography.titleSmall, color = Color(0xFFFFD54F))
            Text(
              text = if (claimableAchievements > 0) "$claimableAchievements reward(s) ready to claim!" else "Earn Blue Essence & Operator XP milestones",
              style = MaterialTheme.typography.bodySmall,
              color = if (claimableAchievements > 0) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // Settings, Performance, Controls & Accessibility Support Card
    item {
      @OptIn(ExperimentalFoundationApi::class)
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .combinedClickable(
            onClick = onOpenSettings,
            onLongClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onOpenSettings()
            },
          )
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
      @OptIn(ExperimentalFoundationApi::class)
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .combinedClickable(
            onClick = onOpenMediaVault,
            onLongClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onOpenMediaVault()
            },
          )
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
        modifier = Modifier.testTag("os_worlds_row"),
      ) {
        items(UniverseCatalog.worlds, key = { it.id }) { world ->
          UniverseCollectionCard(
            world = world,
            selected = world.id == profile.activeUniverseId,
            onClick = { onSelectUniverse(world.id) },
            onLongClick = {
              onSelectUniverse(world.id)
              onStatusMessage("Held & Mounted World: ${world.title}")
            },
          )
        }
      }
    }

    // 5 Difficulties for Game Collections + New Game+ & 4 Post-Completion Versions
    item {
      @OptIn(ExperimentalFoundationApi::class)
      val activeColDiff = progression.collectionDifficulty(activeWorld.id)
      val activeNg = progression.ngPlusSpec
      val combinedMult = progression.combinedGameplayRewardMultiplier(activeWorld.id)
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, Color(activeColDiff.accentHex).copy(alpha = 0.65f), RoundedCornerShape(18.dp))
          .testTag("os_collection_difficulty_ngplus_card"),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "COLLECTION DIFFICULTY (${activeWorld.title.substringBefore(":").uppercase()}) — ${activeColDiff.label}",
            style = MaterialTheme.typography.titleSmall,
            color = Color(activeColDiff.accentHex),
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${activeColDiff.summary} • Tap or Hold any difficulty or NG+ tier",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("os_collection_difficulty_row"),
          ) {
            items(CollectionDifficulty.entries, key = { it.id }) { diff ->
              val selected = activeColDiff == diff
              val accent = Color(diff.accentHex)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .combinedClickable(
                    onClick = {
                      onSelectCollectionDifficulty(activeWorld.id, diff)
                      onStatusMessage("${activeWorld.title.substringBefore(":")}: ${diff.label} (RWD x${String.format(Locale.US, "%.2f", diff.rewardMultiplier)})")
                    },
                    onLongClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onSelectCollectionDifficulty(activeWorld.id, diff)
                      onStatusMessage("Held & Selected Difficulty: ${diff.label}")
                    },
                  )
                  .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) accent else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp),
                  )
                  .testTag("collection_diff_${diff.id}"),
              ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                  Text(
                    text = diff.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = "HP x${String.format(Locale.US, "%.2f", diff.enemyHpScale)} • RWD x${String.format(Locale.US, "%.2f", diff.rewardMultiplier)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD54F),
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
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "NEW GAME+ PROTOCOL (${activeNg.label.uppercase()})",
                style = MaterialTheme.typography.titleSmall,
                color = Color(activeNg.accentHex),
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Completions: ${progression.ngPlusCompletions} • NG+ Mult x${String.format(Locale.US, "%.2f", progression.ngPlusRewardMultiplier)} • Combined x${String.format(Locale.US, "%.2f", combinedMult)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFD54F),
              )
            }
            OutlinedButton(
              onClick = {
                onCompleteNgPlusRun()
                onStatusMessage("Run completed! NG+ completions & rewards scaled higher.")
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.heightIn(min = 48.dp).testTag("os_ng_plus_complete_btn"),
            ) {
              Text("Log Run", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("os_ng_plus_row"),
          ) {
            items(NewGamePlusVersion.entries, key = { it.tier }) { ver ->
              val unlocked = ver.tier <= progression.unlockedNgPlusTier
              val selected = progression.activeNgPlusTier == ver.tier
              val accent = Color(ver.accentHex)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .combinedClickable(
                    enabled = unlocked,
                    onClick = {
                      onSelectNgPlusTier(ver.tier)
                      onStatusMessage("Activated ${ver.label}")
                    },
                    onLongClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onSelectNgPlusTier(ver.tier)
                      onStatusMessage("Held & Activated ${ver.label}")
                    },
                  )
                  .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) accent else if (unlocked) accent.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                  )
                  .testTag("os_ng_plus_tier_${ver.tier}"),
              ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                  Text(
                    text = if (unlocked) ver.shortTag else "LOCKED ${ver.shortTag}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = "RWD x${String.format(Locale.US, "%.2f", ver.rewardMultiplier)} • HP x${String.format(Locale.US, "%.2f", ver.enemyHpScale)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unlocked) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }

          val cm = progression.collectionCustomMode
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (cm.enabled) Color(0xFFFF9100).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
              .fillMaxWidth()
              .border(
                1.dp,
                if (cm.enabled) Color(0xFFFF9100).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp),
              ),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "BO3 • STEAM • COLLECTIONS CUSTOM MODE",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (cm.enabled) Color(0xFFFF9100) else Color(0xFF00E5FF),
                )
                Text(
                  text = "${cm.impactLabel} • ${if (cm.safeCollectionGuard) "Safeguard ON" else "Unguarded"}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Spacer(Modifier.width(8.dp))
              Button(
                onClick = { showCustomModeDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (cm.enabled) Color(0xFFFF9100) else Color(0xFF00E5FF),
                  contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 48.dp).testTag("os_custom_mode_btn"),
              ) {
                Text("Custom Mode", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // Game Modes Switcher
    item {
      Text(
        text = "DEPLOYMENT MODES (${UniverseCatalog.modes.size} MODES • HERO PATROL • BOSS RUSH • ONSLAUGHT)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }

    item {
      @OptIn(ExperimentalFoundationApi::class)
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.testTag("os_modes_row"),
      ) {
        items(UniverseCatalog.modes, key = { it.id }) { mode ->
          val isSelected = mode.id == profile.activeMode
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) Color(mode.accentHex).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .heightIn(min = 56.dp)
              .clip(RoundedCornerShape(14.dp))
              .combinedClickable(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  onSelectMode(mode.id)
                  onStatusMessage("Selected Mode: ${mode.label}")
                },
                onLongClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  onSelectMode(mode.id)
                  onStatusMessage("Held & Locked Mode: ${mode.label}")
                },
              )
              .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(mode.accentHex) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
              )
              .testTag("mode_chip_${mode.id}"),
          ) {
            Column(modifier = Modifier.width(196.dp).padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.weight(1f),
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
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                if (isSelected) {
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(mode.accentHex),
                  ) {
                    Text(
                      text = "ACTIVE",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color.Black,
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                  }
                }
              }
              Spacer(Modifier.height(4.dp))
              Text(
                text = "${mode.shortTag} • ${mode.summary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }

    // Playable Arenas & Custom Locations Switcher
    item {
      Text(
        text = "PLAYABLE ARENAS & LOCATIONS (${MultiverseArsenalCatalog.customAreas.size} ARENAS • BATCAVE • QUEENS • STARK • CONVERGENCE)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }

    item {
      @OptIn(ExperimentalFoundationApi::class)
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.testTag("os_arenas_row"),
      ) {
        items(MultiverseArsenalCatalog.customAreas, key = { it.id }) { area ->
          val isSelected = area.id == progression.activeAreaId
          val areaSt = progression.areaState(area.id)
          val accent = Color(area.accentHex)
          val linkedWorld = UniverseCatalog.worldById(area.linkedWorldId)
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isSelected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .width(232.dp)
              .heightIn(min = 64.dp)
              .clip(RoundedCornerShape(16.dp))
              .combinedClickable(
                onClick = { onSelectArea(area.id) },
                onLongClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  onSelectArea(area.id)
                  onStatusMessage("Held & Deployed Area: ${area.name}")
                },
              )
              .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accent else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp),
              )
              .testTag("os_arena_card_${area.id}"),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "${linkedWorld.title.substringBefore(":")} • ${areaSt.ageYears}y",
                  style = MaterialTheme.typography.labelSmall,
                  color = accent,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                if (isSelected) {
                  Text(
                    text = "ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00E676),
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
              Text(
                text = area.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = area.description,
                style = MaterialTheme.typography.bodySmall,
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
        onDeployWorkshopArea = onDeployArea,
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

  if (showLobbyRosterDialog) {
    LobbyAndBotsDialog(
      lobbyConfig = progression.lobbyConfig,
      playerCallsign = profile.callsign,
      playerLevel = progression.playerLevel,
      playerPrestige = progression.playerPrestige,
      onUpdateLobby = onUpdateLobby,
      onDismiss = { showLobbyRosterDialog = false },
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
      onSave = onUpdateCollectionCustomMode,
      onDeploy = onDeployCollectionCustomMode,
      onDismiss = { showCustomModeDialog = false },
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OsDockChip(
  modifier: Modifier,
  label: String,
  accent: Color,
  onClick: () -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = accent.copy(alpha = 0.14f),
    modifier = modifier
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onClick()
        },
      )
      .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
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
    modifier = modifier.heightIn(min = 56.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UniverseCollectionCard(
  world: UniverseWorld,
  selected: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit = onClick,
) {
  val haptic = LocalHapticFeedback.current
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .width(248.dp)
      .clip(RoundedCornerShape(18.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onLongClick()
        },
      )
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

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.AppSettings
import com.example.data.local.CloudSyncState
import com.example.data.local.CollectionResolutionMode
import com.example.data.local.LocalGameStrings
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
import kotlin.math.roundToInt

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
  onOpenVehiclesArmory: () -> Unit = {},
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
  onOpenMultiverseHub: (Int) -> Unit = {},
  onGrantEssenceBonus: () -> Unit = {},
  onImportBo3Preset: () -> Unit = {},
  onDeployArea: (String) -> Unit = { areaId -> onSelectArea(areaId); onLaunchArena() },
  onUpgradeAreaFeature: (String, String) -> Unit = { _, _ -> },
  onUpgradeAreaStar: (String) -> Unit = {},
  onUpgradeModeTier: (String) -> Unit = {},
  onUpgradeModeStar: (String) -> Unit = {},
  onUpdateCollectionCustomMode: ((CollectionCustomModeConfig) -> CollectionCustomModeConfig) -> Unit = {},
  onDeployCollectionCustomMode: () -> Unit = {},
  activeLanguage: AppLanguage = AppLanguage.ENGLISH,
  appSettings: AppSettings = AppSettings(),
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit = {},
  onSelectLanguage: (String) -> Unit = {},
  onSignOut: () -> Unit,
) {
  val gameStrings = LocalGameStrings.current
  val activeWorld = UniverseCatalog.worldById(profile.activeUniverseId)
  val activeMode = UniverseCatalog.modeById(profile.activeMode)
  val activeConsole = UniverseCatalog.consoleById(profile.consolePreset)
  val activeRig = rigs.firstOrNull { it.id == profile.activeRigId } ?: rigs.firstOrNull()
  val equippedCount = armory.count { it.equipped }

  var showCallsignDialog by remember { mutableStateOf(false) }
  var showLobbyRosterDialog by remember { mutableStateOf(false) }
  var showCodesDialog by remember { mutableStateOf(false) }
  var showCustomModeDialog by remember { mutableStateOf(false) }
  var showLanguageDialog by remember { mutableStateOf(false) }
  var callsignDraft by remember(profile.callsign) { mutableStateOf(profile.callsign) }
  val claimableAchievements = achievements.count { it.unlocked && !it.claimed }
  val haptic = LocalHapticFeedback.current
  val activeResMode = appSettings.collectionResolutionMode

  val wallpaperBitmap = rememberAssetImageBitmap(
    activeWallpaperPath,
    maxDim = activeResMode.bitmapMaxDim.coerceIn(720, 1440),
  )
  val wallpaperGradient = when (profile.osWallpaperIndex.toInt() % 4) {
    1 -> listOf(Color(0xFF0A0718), Color(0xFF1A0F36), Color(0xFF070A12))
    2 -> listOf(Color(0xFF14070D), Color(0xFF280D1B), Color(0xFF070A12))
    3 -> listOf(Color(0xFF071418), Color(0xFF0E2933), Color(0xFF070A12))
    else -> listOf(Color(0xFF070A12), Color(0xFF0F1A33), Color(0xFF070A12))
  }

  BoxWithConstraints(
    modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(wallpaperGradient)),
  ) {
    val viewportWidthDp = maxWidth
    val viewportHeightDp = maxHeight
    val isMediumScreen = viewportWidthDp >= 600.dp
    val isExpandedScreen = viewportWidthDp >= 840.dp
    val collectionColumns = when {
      activeResMode == CollectionResolutionMode.UHD_4K || activeResMode == CollectionResolutionMode.ULTRAWIDE_1440 ->
        if (viewportWidthDp >= 840.dp) 3 else if (isMediumScreen) 2 else 1
      viewportWidthDp >= 1040.dp -> 4
      isExpandedScreen -> 3
      isMediumScreen -> 2
      else -> 1
    }
    val heroBannerHeight = when {
      isExpandedScreen -> 196.dp
      isMediumScreen -> 168.dp
      else -> 136.dp
    }

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
    horizontalAlignment = Alignment.CenterHorizontally,
    contentPadding = PaddingValues(
      horizontal = if (isExpandedScreen) 20.dp else 12.dp,
      vertical = 12.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // App Title Header & UnknownOS Kernel Status
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        modifier = Modifier.fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
      ) {
        Column(
          modifier = Modifier.padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "THE UNKNOWN UNIVERSES",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                  modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF00E676))
                )
                Text(
                  text = "UNKNOWNOS // ${profile.callsign.uppercase()}",
                  style = MaterialTheme.typography.labelSmall,
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
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
              val activeWeapon = ProgressionCatalog.weaponById(progression.weaponCustom.equippedWeaponId, progression.customWeapons)
              val activeOutfit = progression.activeOutfitSpec()
              val activeAreaSpec = progression.activeAreaSpec()
              val powerLight = progression.computePowerAndLightLevel(profile.totalEssence)
              val activeRarity = progression.outfitRarity(activeOutfit.id)
              Text(
                text = "PWR ${powerLight.powerLevel} • LT ${powerLight.lightLevel} • ${progression.levelCustom.worldDifficultyTier.label.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("os_power_light_badge"),
              )
              Text(
                text = "LV ${progression.playerLevel} (${progression.playerRankTitle}) • ${activeOutfit.nameWithVersion} [${activeRarity.label}] • ${activeWeapon.name}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(progression.activePassVersion.accentHex),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("os_level_rank_badge"),
              )
              Text(
                text = "Blue Essence: ${profile.totalEssence} • R:${progression.redEssence} Y:${progression.yellowEssence} G:${progression.greenEssence} • ${activeAreaSpec.name.substringBefore(" (")}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00E5FF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("os_blue_essence_text"),
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              OutlinedButton(
                onClick = { showLanguageDialog = true },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(48.dp).testTag("os_language_btn"),
              ) {
                Text(
                  text = activeLanguage.flagTag,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
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

          // UnknownOS Window Dock Strip — compact game-relation icon links
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp),
              icon = AppIcons.MyLocation,
              label = "ARENA",
              subtitle = "Combat Sim",
              accent = Color(activeWorld.accentHex),
              onClick = onLaunchArena,
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 74.dp).testTag("os_tier_codes_btn"),
              icon = AppIcons.Bolt,
              label = "CODES",
              subtitle = "T25 / 15★",
              accent = Color(0xFF8C52FF),
              onClick = { showCodesDialog = true },
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp).testTag("os_dock_store"),
              icon = AppIcons.StorefrontBag,
              label = "STORE",
              subtitle = "Conduit Shop",
              accent = Color(0xFF00E5FF),
              onClick = onOpenStore,
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp).testTag("os_dock_achievements"),
              icon = AppIcons.TrophyCup,
              label = if (claimableAchievements > 0) "ACH ($claimableAchievements)" else "ACHIEVE",
              subtitle = "Arkham Cups",
              accent = Color(0xFFFFD54F),
              onClick = onOpenAchievements,
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp).testTag("os_dock_missions"),
              icon = AppIcons.CompassOps,
              label = "MISSIONS",
              subtitle = "Stargate Ops",
              accent = Color(0xFF00E676),
              onClick = onOpenMissions,
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp),
              icon = AppIcons.Animation,
              label = "BLENDER",
              subtitle = "3D Rig Lab",
              accent = Color(0xFFFF8F00),
              onClick = onOpenForgeRig,
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp),
              icon = AppIcons.VideoLibrary,
              label = "VAULT",
              subtitle = "Krita Media",
              accent = Color(0xFFE040FB),
              onClick = onOpenMediaVault,
            )
            OsDockChip(
              modifier = Modifier.widthIn(min = 72.dp),
              icon = AppIcons.Gamepad,
              label = "EXPORT",
              subtitle = "Mod Bridge",
              accent = Color(0xFF00E676),
              onClick = onOpenExport,
            )
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GameIconLinkTile(
          modifier = Modifier.weight(1f).testTag("os_missions_card"),
          icon = AppIcons.CompassOps,
          title = "Missions",
          subtitle = "Stargate ${com.example.data.model.MissionsCatalog.allMissions.size} Ops",
          accent = Color(0xFF00E676),
          onClick = onOpenMissions,
        )
        GameIconLinkTile(
          modifier = Modifier.weight(1f),
          icon = AppIcons.Public,
          title = "Multiverse",
          subtitle = "${UniverseCatalog.worlds.size} Worlds",
          accent = Color(activeWorld.accentHex),
          onClick = { onOpenMultiverseHub(0) },
        )
        GameIconLinkTile(
          modifier = Modifier.weight(1f),
          icon = AppIcons.BuildForge,
          title = "3D Forge",
          subtitle = "Blender Rigs",
          accent = Color(0xFFFF8F00),
          onClick = onOpenForgeRig,
        )
      }
    }

    item {
      val activeVeh = armory.firstOrNull { it.category == "vehicle" && it.equipped } ?: armory.firstOrNull { it.category == "vehicle" }
      val activeVehLabel = activeVeh?.let { "${it.name} (T${it.tier})" } ?: "8 Collection Vehicles Ready"
      MultiverseSystemsControlCard(
        progression = progression,
        activeVehicleLabel = activeVehLabel,
        onMaxAllTiers = onMaxAllTiers,
        onMaxAllStars = onMaxAllStars,
        onDeployStarWarsNexus = {
          onSelectUniverse("sector_star_wars_nexus")
          onDeployArea("area_star_wars_nexus")
        },
        onDeployBankVault = {
          onDeployArea("area_bank_vault")
        },
        onOpenVehiclesArmory = onOpenVehiclesArmory,
        onOpenMultiverseHub = onOpenMultiverseHub,
      )
    }

    // Active Universe Hero Box-Art Card
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
          .border(1.2.dp, Color(activeWorld.accentHex).copy(alpha = 0.65f), RoundedCornerShape(16.dp)),
      ) {
        Column {
          Box(modifier = Modifier.fillMaxWidth().height(heroBannerHeight)) {
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
              modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                .border(1.dp, Color(activeWorld.accentHex).copy(alpha = 0.7f), RoundedCornerShape(999.dp)),
            ) {
              Text(
                text = activeWorld.platformBadges,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = Color(activeMode.accentHex).copy(alpha = 0.24f),
              modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                .border(1.dp, Color(activeMode.accentHex), RoundedCornerShape(999.dp)),
            ) {
              Text(
                text = "${activeMode.shortTag} • ${activeMode.label}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(activeMode.accentHex),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
              Text(
                text = activeWorld.inspirations.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(activeWorld.accentHex),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = activeWorld.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = activeWorld.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
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
            items(heroList.distinctBy { it.id }, key = { it.id }) { spec ->
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

    // Store, Achievements & Batcomputer Settings — Compact Game-Relation Icon Links
    item {
      val unlockedAch = achievements.count { it.unlocked }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GameIconLinkTile(
          modifier = Modifier.weight(1f).testTag("os_store_card"),
          icon = AppIcons.StorefrontBag,
          title = "Store",
          subtitle = "${profile.totalEssence} Conduit",
          accent = Color(0xFF00E5FF),
          onClick = onOpenStore,
        )
        GameIconLinkTile(
          modifier = Modifier.weight(1f).testTag("os_achievements_card"),
          icon = AppIcons.TrophyCup,
          title = "Trophies",
          subtitle = if (claimableAchievements > 0) "$claimableAchievements Ready!" else "$unlockedAch/${achievements.size.coerceAtLeast(18)} Arkham",
          accent = Color(0xFFFFD54F),
          onClick = onOpenAchievements,
        )
        GameIconLinkTile(
          modifier = Modifier.weight(1f).testTag("settings_accessibility_card"),
          icon = AppIcons.Accessibility,
          title = "Batcomputer",
          subtitle = "Controls & FPS",
          accent = Color(0xFF00E676),
          contentDescription = stringResource(R.string.cd_open_settings),
          onClick = onOpenSettings,
        )
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

    // Multiverse Worlds Collection (Multi-Resolution Showcase)
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "THE UNKNOWN UNIVERSES COLLECTION (${UniverseCatalog.worlds.size} WORLDS)",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            val vpW = viewportWidthDp.value.roundToInt()
            val vpH = viewportHeightDp.value.roundToInt()
            val safeMenuPct = ((if (appSettings.menuScale.isFinite()) appSettings.menuScale else 1.0f) * 100f).roundToInt()
            Text(
              text = "Viewport ${vpW}×${vpH}dp • ${activeResMode.label} • ${collectionColumns}x Grid • Menu $safeMenuPct%",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF00E5FF),
              modifier = Modifier.testTag("os_collection_resolution_badge"),
            )
          }
          OutlinedButton(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.heightIn(min = 48.dp).testTag("os_collection_res_settings_btn"),
          ) {
            Text("Display & Scale", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }
        }

        // Quick Multi-Resolution Preset Bar + Menu Scale Pills
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CollectionResolutionMode.entries.forEach { resMode ->
            FilterChip(
              selected = activeResMode == resMode,
              onClick = {
                onUpdateSettings { it.copy(collectionResolutionMode = resMode) }
                onStatusMessage("Collection Resolution: ${resMode.label}")
              },
              label = { Text(resMode.shortTag, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
              modifier = Modifier.heightIn(min = 48.dp).testTag("os_col_res_${resMode.id}"),
            )
          }
          listOf(0.85f to "UI 85%", 1.0f to "UI 100%", 1.15f to "UI 115%", 1.30f to "UI 130%").forEach { (scaleVal, scaleLabel) ->
            val currentScale = if (appSettings.menuScale.isFinite()) appSettings.menuScale else 1.0f
            val activeScale = kotlin.math.abs(currentScale - scaleVal) < 0.03f
            FilterChip(
              selected = activeScale,
              onClick = {
                onUpdateSettings { it.copy(menuScale = scaleVal) }
                onStatusMessage("Menu Scale set to $scaleLabel")
              },
              label = { Text(scaleLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
              modifier = Modifier.heightIn(min = 48.dp).testTag("os_menu_scale_${(scaleVal * 100).roundToInt()}"),
            )
          }
        }
      }
    }

    item {
      if (collectionColumns <= 1) {
        val compactCardWidth = (viewportWidthDp * 0.74f).coerceIn(228.dp, 310.dp)
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.testTag("os_worlds_row"),
        ) {
          items(UniverseCatalog.worlds, key = { it.id }) { world ->
            UniverseCollectionCard(
              world = world,
              selected = world.id == profile.activeUniverseId,
              cardAspectRatio = activeResMode.cardAspectRatio,
              modifier = Modifier.width(compactCardWidth),
              onClick = { onSelectUniverse(world.id) },
              onLongClick = {
                onSelectUniverse(world.id)
                onStatusMessage("Held & Mounted World: ${world.title}")
              },
            )
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth().testTag("os_worlds_row"),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          UniverseCatalog.worlds.chunked(collectionColumns).forEach { rowWorlds ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              rowWorlds.forEach { world ->
                UniverseCollectionCard(
                  world = world,
                  selected = world.id == profile.activeUniverseId,
                  cardAspectRatio = activeResMode.cardAspectRatio,
                  modifier = Modifier.weight(1f),
                  onClick = { onSelectUniverse(world.id) },
                  onLongClick = {
                    onSelectUniverse(world.id)
                    onStatusMessage("Held & Mounted World: ${world.title}")
                  },
                )
              }
              repeat(collectionColumns - rowWorlds.size) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
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
                    onClick = {
                      onSelectNgPlusTier(ver.tier)
                    },
                    onLongClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onSelectNgPlusTier(ver.tier)
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
            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
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
                    text = if (cm.enabled) {
                      "${cm.impactLabel} • ${if (cm.safeCollectionGuard) "Safeguard ON" else "Unguarded"}"
                    } else {
                      "Normal Mode Active • Switch to Custom Mode anytime"
                    },
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
                  Text("Configure", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FilterChip(
                  selected = !cm.enabled,
                  onClick = { onUpdateCollectionCustomMode { it.copy(enabled = false) } },
                  label = { Text("Normal Mode", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("os_switch_normal_mode_btn"),
                )
                FilterChip(
                  selected = cm.enabled,
                  onClick = { onUpdateCollectionCustomMode { it.copy(enabled = true) } },
                  label = { Text("Custom Mode", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("os_switch_custom_mode_btn"),
                )
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
        items(UniverseCatalog.modes.distinctBy { it.id }, key = { it.id }) { mode ->
          val isSelected = mode.id == profile.activeMode
          val modeTier = progression.modeTier(mode.id)
          val modeStar = progression.modeStar(mode.id)
          val modeTierCost = com.example.data.model.UpgradeCosts.modeTier(modeTier)
          val modeStarCost = com.example.data.model.UpgradeCosts.modeStar(modeStar)
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
            Column(modifier = Modifier.width(196.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    imageVector = AppIcons.Bolt,
                    contentDescription = null,
                    tint = Color(mode.accentHex),
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = mode.label,
                    style = MaterialTheme.typography.titleSmall,
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
              Text(
                text = "T$modeTier/25 • $modeStar★ • ${mode.shortTag}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(mode.accentHex),
                maxLines = 1,
              )
              Text(
                text = mode.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                OutlinedButton(
                  onClick = { onUpgradeModeTier(mode.id) },
                  enabled = modeTier < 25,
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("mode_up_tier_${mode.id}"),
                ) {
                  Text(
                    text = if (modeTier >= 25) "T25" else "+Tier (${modeTierCost}B)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                  )
                }
                OutlinedButton(
                  onClick = { onUpgradeModeStar(mode.id) },
                  enabled = modeStar < com.example.data.model.StarRankMeta.MAX_STARS,
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("mode_up_star_${mode.id}"),
                ) {
                  Text(
                    text = if (modeStar >= com.example.data.model.StarRankMeta.MAX_STARS) "${com.example.data.model.StarRankMeta.MAX_STARS}★" else "+★ (${modeStarCost}B)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                  )
                }
              }
            }
          }
        }
      }
    }

    // Playable Arenas & Custom Locations Switcher (Segregated by Current World vs All Worlds)
    item {
      val playableAreas = remember(progression.customGameMaps) { progression.allPlayableAreas() }
      var showAllWorldArenas by remember { mutableStateOf(false) }
      val worldScopedAreas = remember(playableAreas, activeWorld.id, progression.activeAreaId, showAllWorldArenas) {
        if (showAllWorldArenas) {
          playableAreas
        } else {
          playableAreas.filter {
            it.linkedWorldId == activeWorld.id ||
              it.id == progression.activeAreaId ||
              it.id == "area_bank_vault"
          }.ifEmpty { playableAreas }
        }
      }
      val thisWorldCount = remember(playableAreas, activeWorld.id, progression.activeAreaId) {
        playableAreas.count {
          it.linkedWorldId == activeWorld.id ||
            it.id == progression.activeAreaId ||
            it.id == "area_bank_vault"
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "${gameStrings.playableArenas} (${worldScopedAreas.size}/${playableAreas.size} MAPS)",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = !showAllWorldArenas,
            onClick = { showAllWorldArenas = false },
            label = { Text("${gameStrings.thisWorld} ($thisWorldCount)") },
            modifier = Modifier.heightIn(min = 48.dp).testTag("os_arenas_filter_world"),
          )
          FilterChip(
            selected = showAllWorldArenas,
            onClick = { showAllWorldArenas = true },
            label = { Text("${gameStrings.allWorlds} (${playableAreas.size})") },
            modifier = Modifier.heightIn(min = 48.dp).testTag("os_arenas_filter_all"),
          )
        }
        @OptIn(ExperimentalFoundationApi::class)
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.testTag("os_arenas_row"),
        ) {
          items(worldScopedAreas.distinctBy { it.id }, key = { it.id }) { area ->
          val isSelected = area.id == progression.activeAreaId
          val areaSt = progression.areaState(area.id)
          val accent = Color(area.accentHex)
          val linkedWorld = UniverseCatalog.worldById(area.linkedWorldId)
          val gridCost = com.example.data.model.UpgradeCosts.areaFeatureTier(areaSt.powerGridTier)
          val starCost = com.example.data.model.UpgradeCosts.areaStar(areaSt.starLevel)
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .width(214.dp)
              .heightIn(min = 56.dp)
              .clip(RoundedCornerShape(14.dp))
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
                shape = RoundedCornerShape(14.dp),
              )
              .testTag("os_arena_card_${area.id}"),
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
                    text = gameStrings.activeBadge,
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
                text = "Grid T${areaSt.powerGridTier}/25 • ${areaSt.starLevel}★",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                maxLines = 1,
              )
              Text(
                text = area.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                OutlinedButton(
                  onClick = { onUpgradeAreaFeature(area.id, "power_grid") },
                  enabled = areaSt.powerGridTier < 25,
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("os_arena_up_tier_${area.id}"),
                ) {
                  Text(
                    text = if (areaSt.powerGridTier >= 25) "T25" else "+Grid (${gridCost}B)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                  )
                }
                OutlinedButton(
                  onClick = { onUpgradeAreaStar(area.id) },
                  enabled = areaSt.starLevel < com.example.data.model.StarRankMeta.MAX_STARS,
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("os_arena_up_star_${area.id}"),
                ) {
                  Text(
                    text = if (areaSt.starLevel >= com.example.data.model.StarRankMeta.MAX_STARS) "${com.example.data.model.StarRankMeta.MAX_STARS}★" else "+★ (${starCost}B)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                  )
                }
              }
            }
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
        onOpenPrisma3dExport = onOpenExport,
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
      customGameMaps = progression.customGameMaps,
    )
  }

  if (showLanguageDialog) {
    AlertDialog(
      onDismissRequest = { showLanguageDialog = false },
      title = { Text(gameStrings.chooseLanguageDialogTitle, fontWeight = FontWeight.Bold) },
      text = {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(AppLanguage.entries, key = { it.code }) { lang ->
            val selected = lang == activeLanguage
            Surface(
              onClick = {
                onSelectLanguage(lang.code)
                onStatusMessage("${gameStrings.languageSectionTitle}: ${lang.nativeName}")
                showLanguageDialog = false
              },
              shape = RoundedCornerShape(12.dp),
              color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .border(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                  shape = RoundedCornerShape(12.dp),
                )
                .testTag("os_lang_option_${lang.code}"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = "${lang.flagTag} • ${lang.nativeName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = lang.englishName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (selected) {
                  Text(
                    text = gameStrings.activeBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676),
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showLanguageDialog = false }) { Text("OK") }
      },
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OsDockChip(
  modifier: Modifier,
  icon: ImageVector,
  label: String,
  subtitle: String,
  accent: Color,
  onClick: () -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = accent.copy(alpha = 0.12f),
    modifier = modifier
      .heightIn(min = 58.dp)
      .clip(RoundedCornerShape(12.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onClick()
        },
      )
      .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Box(
        modifier = Modifier
          .size(26.dp)
          .clip(CircleShape)
          .background(accent.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = accent,
          modifier = Modifier.size(15.dp),
        )
      }
      Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
      Text(
        text = subtitle,
        fontSize = 8.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameIconLinkTile(
  modifier: Modifier = Modifier,
  icon: ImageVector,
  title: String,
  subtitle: String,
  accent: Color,
  contentDescription: String = "$title — $subtitle",
  onClick: () -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = accent.copy(alpha = 0.10f),
    modifier = modifier
      .heightIn(min = 66.dp)
      .clip(RoundedCornerShape(14.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onClick()
        },
      )
      .border(1.dp, accent.copy(alpha = 0.50f), RoundedCornerShape(14.dp)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(accent.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = contentDescription,
          tint = accent,
          modifier = Modifier.size(16.dp),
        )
      }
      Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
      Text(
        text = subtitle,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
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
  cardAspectRatio: Float = 16f / 9.5f,
  modifier: Modifier = Modifier.width(248.dp),
  onClick: () -> Unit,
  onLongClick: () -> Unit = onClick,
) {
  val haptic = LocalHapticFeedback.current
  val safeAspect = cardAspectRatio.coerceIn(1.35f, 2.35f)
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier
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
      Box(modifier = Modifier.fillMaxWidth().aspectRatio(safeAspect)) {
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

@Composable
fun MultiverseSystemsControlCard(
  progression: OperatorProgressionState,
  activeVehicleLabel: String = "8 Collection Vehicles Ready",
  onMaxAllTiers: () -> Unit,
  onMaxAllStars: () -> Unit,
  onDeployStarWarsNexus: () -> Unit,
  onDeployBankVault: () -> Unit = {},
  onOpenVehiclesArmory: () -> Unit = {},
  onOpenMultiverseHub: (Int) -> Unit = {},
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.5.dp, Color(0xFFFFD54F).copy(alpha = 0.75f), RoundedCornerShape(16.dp)),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "🌟 MULTIVERSE SYSTEMS & HIGHER UPGRADES",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFFFD54F),
        )
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color(0xFFFFD54F).copy(alpha = 0.2f),
        ) {
          Text(
            text = "TIER 25+ / 15★",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD54F),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        CurrencyBadge("🪙 Gold", "${progression.gold}", Color(0xFFFFD54F))
        CurrencyBadge("💎 Diamonds", "${progression.diamonds}", Color(0xFF00E5FF))
        CurrencyBadge("💵 TUUCash", "${progression.tuuCash}", Color(0xFF00E676))
        CurrencyBadge("🔮 Crystals", "${progression.crystals.size}", Color(0xFFE040FB))
      }

      Text(
        text = "🔮 Magic: ${progression.magicState.activeSpellId.uppercase()} (Mana: ${progression.magicState.manaCurrent.toInt()}/${progression.magicState.manaMax.toInt()}) • Difficulty: ${String.format(Locale.US, "%.2f", progression.magicState.difficultyScalingFactor)}x",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFE040FB),
      )
      Text(
        text = "🏰 Player Base: ${progression.playerBaseState.baseName} (Wall T${progression.playerBaseState.wallTier} • Turret T${progression.playerBaseState.turretTier}) • Defended: ${progression.playerBaseState.defenseSuccessCount}",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF38BDF8),
      )
      Text(
        text = "🚗 Active Vehicle: $activeVehicleLabel • 16 Perimeter Turrets Armed",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF00E5FF),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        GameIconLinkTile(
          modifier = Modifier.weight(1f).testTag("os_open_base_citadel_btn"),
          icon = AppIcons.CitadelKeep,
          title = "Citadel Base",
          subtitle = "Krypton Turrets",
          accent = Color(0xFF38BDF8),
          onClick = { onOpenMultiverseHub(4) },
        )
        GameIconLinkTile(
          modifier = Modifier.weight(1f).testTag("os_open_vehicles_btn"),
          icon = AppIcons.VehicleFleet,
          title = "Vehicles",
          subtitle = "Wayne & Stark",
          accent = Color(0xFF00E5FF),
          onClick = onOpenVehiclesArmory,
        )
        GameIconLinkTile(
          modifier = Modifier.weight(1f).testTag("os_open_bank_vault_btn"),
          icon = AppIcons.BankVault,
          title = "Bank & Vault",
          subtitle = "TUUCash & Gem",
          accent = Color(0xFFFFD54F),
          onClick = { onOpenMultiverseHub(5) },
        )
        GameIconLinkTile(
          modifier = Modifier.weight(1f).testTag("os_deploy_bank_vault_btn"),
          icon = AppIcons.DiamondShard,
          title = "Vault Arena",
          subtitle = "Deploy Sector",
          accent = Color(0xFF00E676),
          onClick = onDeployBankVault,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        GameIconLinkTile(
          modifier = Modifier.weight(1f).testTag("os_max_tiers_btn"),
          icon = AppIcons.Bolt,
          title = "Max T25",
          subtitle = "BO3 Gear Tier",
          accent = Color(0xFFFFD54F),
          onClick = onMaxAllTiers,
        )
        GameIconLinkTile(
          modifier = Modifier.weight(1f).testTag("os_max_stars_btn"),
          icon = AppIcons.AutoAwesome,
          title = "Max 15★",
          subtitle = "Starfield Mastery",
          accent = Color(0xFF00E5FF),
          onClick = onMaxAllStars,
        )
        GameIconLinkTile(
          modifier = Modifier.weight(1f).testTag("os_star_wars_nexus_btn"),
          icon = AppIcons.Public,
          title = "SW Nexus",
          subtitle = "Galactic Deploy",
          accent = Color(0xFFFF3D71),
          onClick = onDeployStarWarsNexus,
        )
      }
    }
  }
}

@Composable
fun CurrencyBadge(label: String, value: String, color: Color) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = color.copy(alpha = 0.15f),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
      Text(text = value, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
  }
}


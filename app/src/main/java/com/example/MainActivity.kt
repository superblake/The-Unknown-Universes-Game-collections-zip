package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.AppSettings
import com.example.data.local.AppSettingsStore
import com.example.data.local.ProgressionStore
import com.example.data.local.UniverseRepository
import com.example.security.AppSecurityManager
import com.example.ui.OsWorkspaceTab
import com.example.ui.UniverseUiState
import com.example.ui.UniverseViewModel
import com.example.ui.auth.AppPinUnlockScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.GoogleSignInHelper
import com.example.ui.auth.OAuthCallbackResult
import com.example.ui.auth.PlatformOAuthManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.example.ui.screens.AchievementsDialog
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.ArmoryScreen
import com.example.ui.screens.BlueEssenceStoreDialog
import com.example.ui.screens.ExportCompatScreen
import com.example.ui.screens.ForgeRigScreen
import com.example.ui.screens.MatchBrowserDialog
import com.example.ui.screens.MediaVaultScreen
import com.example.ui.screens.MissionsDialog
import com.example.ui.screens.OsDesktopScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.SocialAndCommsDialog
import com.example.data.model.SocialCatalog
import com.example.ui.theme.AppIcons
import com.example.ui.theme.UnknownUniversesTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
  private val pendingOAuthRedirect = MutableStateFlow<Uri?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pendingOAuthRedirect.value = intent?.data
    enableEdgeToEdge()
    com.example.util.BackgroundMusicManager.startAmbientMusic(this)
    setContent {
      AppNavigation(
        pendingOAuthRedirect = pendingOAuthRedirect.asStateFlow(),
        onConsumeRedirect = { pendingOAuthRedirect.value = null },
      )
    }
  }

  override fun onPause() {
    super.onPause()
    com.example.util.BackgroundMusicManager.pauseMusic()
  }

  override fun onResume() {
    super.onResume()
    com.example.util.BackgroundMusicManager.resumeMusic()
  }

  override fun onDestroy() {
    super.onDestroy()
    com.example.util.BackgroundMusicManager.stopAmbientMusic()
  }

  override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
    if (com.example.util.GamepadInputBridge.onGenericMotionEvent(ev)) {
      return true
    }
    return super.dispatchGenericMotionEvent(ev)
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (com.example.util.GamepadInputBridge.onKeyEvent(event)) {
      return true
    }
    return super.dispatchKeyEvent(event)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    intent.data?.let { pendingOAuthRedirect.value = it }
  }
}

private const val SESSION_PREFS = "unknown_os_local_session"
private const val KEY_SESSION_ACTIVE = "session_active"
private const val KEY_LAST_CALLSIGN = "last_callsign"
private const val KEY_VAULT_WALLPAPER = "vault_wallpaper_path"
private const val LOCAL_OPERATOR_ID = "local_operator"

@Composable
fun AppNavigation(
  pendingOAuthRedirect: StateFlow<Uri?> = MutableStateFlow(null),
  onConsumeRedirect: () -> Unit = {},
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val prefs = remember(context) {
    context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
  }
  val settingsStore = remember(context) { AppSettingsStore(context) }
  val appSettings by settingsStore.settings.collectAsStateWithLifecycle()
  var showSettingsDialog by remember { mutableStateOf(false) }

  val googleSessionUser by GoogleSignInHelper.sessionUser.collectAsStateWithLifecycle()
  LaunchedEffect(Unit) {
    GoogleSignInHelper.restoreSavedSession(context)
  }

  var firebaseUser by remember {
    mutableStateOf(runCatching { Firebase.auth.currentUser }.getOrNull())
  }
  DisposableEffect(Unit) {
    val auth = runCatching { Firebase.auth }.getOrNull()
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      firebaseUser = firebaseAuth.currentUser
    }
    auth?.addAuthStateListener(listener)
    onDispose {
      auth?.removeAuthStateListener(listener)
    }
  }

  val signedInGoogleUid = firebaseUser?.uid ?: googleSessionUser?.uid
  val googleAccountLabel = firebaseUser?.let { user ->
    user.displayName?.takeIf { it.isNotBlank() }
      ?: user.email?.takeIf { it.isNotBlank() }
      ?: "Connected"
  } ?: googleSessionUser?.let { user ->
    user.displayName?.takeIf { it.isNotBlank() }
      ?: user.email?.takeIf { it.isNotBlank() }
      ?: "Connected"
  }

  var isSessionActive by remember(signedInGoogleUid) {
    mutableStateOf(prefs.getBoolean(KEY_SESSION_ACTIVE, false) || signedInGoogleUid != null)
  }
  var activeCallsign by remember(signedInGoogleUid) {
    mutableStateOf(
      firebaseUser?.displayName?.takeIf { it.length >= 2 }?.take(30)
        ?: googleSessionUser?.displayName?.takeIf { it.length >= 2 }?.take(30)
        ?: prefs.getString(KEY_LAST_CALLSIGN, "VanguardPrime")
        ?: "VanguardPrime"
    )
  }
  var activeWallpaperPath by remember {
    mutableStateOf(prefs.getString(KEY_VAULT_WALLPAPER, null))
  }

  var isPinUnlocked by remember(context) {
    mutableStateOf(!AppSecurityManager.isPinEnabled(context))
  }
  var progressionIntegrityValid by remember { mutableStateOf(true) }
  var verifyIntegrityFn by remember { mutableStateOf<() -> Boolean>({ true }) }

  LaunchedEffect(
    appSettings.musicEnabled,
    appSettings.musicVolume,
    appSettings.selectedMusicTrack,
  ) {
    com.example.util.BackgroundMusicManager.applySettings(
      context = context,
      enabled = appSettings.musicEnabled,
      volume = appSettings.musicVolume,
      trackMode = appSettings.selectedMusicTrack,
    )
  }

  DisposableEffect(appSettings.screenCaptureShield, context) {
    val window = (context as? Activity)?.window
    if (appSettings.screenCaptureShield) {
      window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
      window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    onDispose {}
  }

  val currentUserId = signedInGoogleUid ?: LOCAL_OPERATOR_ID

  UnknownUniversesTheme(
    darkTheme = appSettings.effectiveDarkTheme,
    settings = appSettings,
  ) {
    if (!isSessionActive) {
      AuthScreen(
        initialCallsign = activeCallsign,
        onBootLocalOperator = { callsign ->
          activeCallsign = callsign
          prefs.edit {
            putBoolean(KEY_SESSION_ACTIVE, true)
            putString(KEY_LAST_CALLSIGN, callsign)
          }
          isPinUnlocked = !AppSecurityManager.isPinEnabled(context)
          isSessionActive = true
        },
      )
    } else if (!isPinUnlocked && AppSecurityManager.isPinEnabled(context)) {
      AppPinUnlockScreen(
        callsign = activeCallsign,
        onVerifyPin = { pin -> AppSecurityManager.verifyAppPin(context, pin) },
        onUnlocked = { isPinUnlocked = true },
        onResetSession = {
          prefs.edit { putBoolean(KEY_SESSION_ACTIVE, false) }
          GoogleSignInHelper.signOut(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onSignOutComplete = { isSessionActive = false },
          )
        },
      )
    } else {
      key(currentUserId) {
        UnknownOsShell(
          currentUserId = currentUserId,
          displayName = activeCallsign,
          googleAccountLabel = googleAccountLabel,
          activeWallpaperPath = activeWallpaperPath,
          appSettings = appSettings,
          isSettingsOpen = showSettingsDialog,
          onOpenSettings = { showSettingsDialog = true },
          onSetWallpaperPath = { path ->
            activeWallpaperPath = path
            prefs.edit {
              if (path == null) remove(KEY_VAULT_WALLPAPER)
              else putString(KEY_VAULT_WALLPAPER, path)
            }
          },
          pendingOAuthRedirect = pendingOAuthRedirect,
          onConsumeRedirect = onConsumeRedirect,
          onRegisterIntegrityCheck = { valid, fn ->
            progressionIntegrityValid = valid
            verifyIntegrityFn = fn
          },
          onLockSession = {
            prefs.edit { putBoolean(KEY_SESSION_ACTIVE, false) }
            isPinUnlocked = !AppSecurityManager.isPinEnabled(context)
            GoogleSignInHelper.signOut(
              context = context,
              credentialManager = credentialManager,
              scope = scope,
              onSignOutComplete = { isSessionActive = false },
            )
          },
        )
      }
    }

    if (showSettingsDialog) {
      SettingsDialog(
        settings = appSettings,
        onUpdateSettings = { transform -> settingsStore.update(transform) },
        onReplaceSettings = { next -> settingsStore.replace(next) },
        onResetDefaults = { settingsStore.resetDefaults() },
        onDismiss = { showSettingsDialog = false },
        progressionIntegrityValid = progressionIntegrityValid,
        onVerifyIntegrity = {
          val valid = verifyIntegrityFn()
          progressionIntegrityValid = valid
          valid
        },
      )
    }
  }
}

@Composable
fun UnknownOsShell(
  currentUserId: String,
  displayName: String?,
  googleAccountLabel: String?,
  activeWallpaperPath: String?,
  appSettings: AppSettings = AppSettings(),
  isSettingsOpen: Boolean = false,
  onOpenSettings: () -> Unit = {},
  onSetWallpaperPath: (String?) -> Unit,
  pendingOAuthRedirect: StateFlow<Uri?>,
  onConsumeRedirect: () -> Unit,
  onRegisterIntegrityCheck: (Boolean, () -> Boolean) -> Unit = { _, _ -> },
  onLockSession: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isGoogleSigningIn by remember { mutableStateOf(false) }

  val viewModel: UniverseViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY])
        UniverseViewModel(
          repository = UniverseRepository(app),
          currentUserId = currentUserId,
          defaultDisplayName = displayName,
          progressionStore = ProgressionStore(app, currentUserId),
        )
      }
    },
  )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()
  val progression by viewModel.progressionState.collectAsStateWithLifecycle()
  val challenges by viewModel.challengeStates.collectAsStateWithLifecycle()
  val achievements by viewModel.achievementStates.collectAsStateWithLifecycle()
  val missions by viewModel.missionStates.collectAsStateWithLifecycle()
  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
  val oauthRedirectUri by pendingOAuthRedirect.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val isCloudSignedIn = runCatching { Firebase.auth.currentUser?.uid == currentUserId }.getOrDefault(false)
  var showStoreDialog by remember { mutableStateOf(false) }
  var showAchievementsDialog by remember { mutableStateOf(false) }
  var showMissionsDialog by remember { mutableStateOf(false) }
  var showMatchBrowserDialog by remember { mutableStateOf(false) }
  var matchBrowserInitialTab by remember { mutableIntStateOf(0) }
  var showSocialDialog by remember { mutableStateOf(false) }
  var socialDialogInitialTab by remember { mutableIntStateOf(0) }

  LaunchedEffect(viewModel) {
    onRegisterIntegrityCheck(
      viewModel.verifyProgressionIntegrity(),
      viewModel::verifyProgressionIntegrity,
    )
  }

  val handleDesktopGoogleSuccess: (String?) -> Unit = { name ->
    isGoogleSigningIn = false
    viewModel.showBanner("Signed in with Google${name?.let { " as $it" } ?: ""}")
  }
  val handleDesktopGoogleError: (String) -> Unit = { err ->
    isGoogleSigningIn = false
    viewModel.showBanner(err)
  }
  val handleDesktopGoogleCancelled: () -> Unit = {
    isGoogleSigningIn = false
    viewModel.showBanner("Google Sign-In was cancelled.")
  }

  LaunchedEffect(oauthRedirectUri) {
    val uri = oauthRedirectUri ?: return@LaunchedEffect
    onConsumeRedirect()
    when (val outcome = PlatformOAuthManager.handleRedirectUri(context, uri)) {
      is OAuthCallbackResult.Linked -> {
        viewModel.saveLinkedAccount(
          outcome.providerId,
          outcome.displayName,
          outcome.externalSubject,
          outcome.bridgedAccounts,
        )
      }
      is OAuthCallbackResult.Failed -> {
        viewModel.showBanner(outcome.message)
      }
      null -> Unit
    }
  }

  LaunchedEffect(statusBanner) {
    val msg = statusBanner
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.dismissBanner()
    }
  }

  if (activeTab != OsWorkspaceTab.OS_DESKTOP) {
    BackHandler {
      viewModel.selectTab(OsWorkspaceTab.OS_DESKTOP)
    }
  }

  val hideBarsForArena = appSettings.immersiveFullscreen && activeTab == OsWorkspaceTab.ARENA_PLAY
  DisposableEffect(hideBarsForArena, context) {
    val window = (context as? Activity)?.window
    if (window != null) {
      val controller = WindowCompat.getInsetsController(window, window.decorView)
      if (hideBarsForArena) {
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
      } else {
        controller.show(WindowInsetsCompat.Type.systemBars())
      }
    }
    onDispose {}
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = if (hideBarsForArena) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
      snackbarHost = {
        SnackbarHost(hostState = snackbarHostState) { data ->
          Snackbar(snackbarData = data)
        }
      },
      bottomBar = {
        if (!useNavRail && !hideBarsForArena) {
          NavigationBar(
            windowInsets = WindowInsets.navigationBars,
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            OsWorkspaceTab.entries.forEach { tab ->
              NavigationBarItem(
                selected = activeTab == tab,
                onClick = { viewModel.selectTab(tab) },
                icon = {
                  Icon(
                    imageVector = tabIcon(tab),
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label, maxLines = 1) },
                modifier = Modifier.testTag("nav_${tab.routeTag}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier.fillMaxSize().padding(innerPadding)
      ) {
        if (useNavRail && !hideBarsForArena) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(),
          ) {
            OsWorkspaceTab.entries.forEach { tab ->
              NavigationRailItem(
                selected = activeTab == tab,
                onClick = { viewModel.selectTab(tab) },
                icon = {
                  Icon(
                    imageVector = tabIcon(tab),
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("rail_${tab.routeTag}"),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
          when (val state = uiState) {
            is UniverseUiState.Loading -> {
              Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                Text(
                  text = "Booting UnknownOS Kernel...",
                  style = MaterialTheme.typography.titleMedium,
                  modifier = Modifier.padding(top = 12.dp),
                )
              }
            }

            is UniverseUiState.Error -> {
              Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                Text(
                  text = "Storage Error",
                  style = MaterialTheme.typography.headlineSmall,
                  color = MaterialTheme.colorScheme.error,
                )
                Text(
                  text = state.message,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(top = 8.dp),
                )
              }
            }

            is UniverseUiState.Ready -> {
              when (activeTab) {
                OsWorkspaceTab.OS_DESKTOP -> OsDesktopScreen(
                  profile = state.profile,
                  armory = state.armory,
                  rigs = state.rigs,
                  matchLogs = state.matchLogs,
                  linkedAccounts = state.linkedAccounts,
                  cloudSyncState = cloudSyncState,
                  isCloudSignedIn = isCloudSignedIn,
                  googleAccountLabel = googleAccountLabel,
                  isGoogleSigningIn = isGoogleSigningIn,
                  onSignInGoogle = {
                    isGoogleSigningIn = true
                    GoogleSignInHelper.onGoogleSignInClicked(
                      context = context,
                      credentialManager = credentialManager,
                      onAuthSuccess = handleDesktopGoogleSuccess,
                      onAuthError = handleDesktopGoogleError,
                      scope = scope,
                      onAuthCancelled = handleDesktopGoogleCancelled,
                    )
                  },
                  onSyncNow = viewModel::syncNow,
                  onRequestSaveExportJson = viewModel::exportSaveJson,
                  onParseSaveJson = viewModel::parseSavePreview,
                  onConfirmSaveImport = viewModel::importSavePreview,
                  onConfirmWorkshopImport = viewModel::importSteamWorkshopPreview,
                  activeWallpaperPath = activeWallpaperPath,
                  onSelectUniverse = viewModel::selectUniverse,
                  onSelectMode = viewModel::selectMode,
                  onLaunchArena = { viewModel.selectTab(OsWorkspaceTab.ARENA_PLAY) },
                  onOpenForgeRig = { viewModel.selectTab(OsWorkspaceTab.FORGE_RIG) },
                  onOpenMediaVault = { viewModel.selectTab(OsWorkspaceTab.MEDIA_VAULT) },
                  onOpenExport = { viewModel.selectTab(OsWorkspaceTab.EXPORT_COMPAT) },
                  onCycleWallpaper = { viewModel.cycleWallpaper(state.profile.osWallpaperIndex) },
                  onUpdateCallsign = viewModel::updateCallsign,
                  onLinkAccount = { providerId, displayName, externalSubject ->
                    viewModel.saveLinkedAccount(providerId, displayName, externalSubject)
                  },
                  onUnlinkAccount = viewModel::unlinkExternalAccount,
                  onStatusMessage = viewModel::showBanner,
                  onOpenSettings = onOpenSettings,
                  progression = progression,
                  achievements = achievements,
                  onUpdateLobby = viewModel::updateLobbyConfig,
                  onOpenStore = { showStoreDialog = true },
                  onOpenAchievements = { showAchievementsDialog = true },
                  onOpenMissions = { showMissionsDialog = true },
                  onOpenMatchBrowser = { tabIdx ->
                    matchBrowserInitialTab = tabIdx
                    showMatchBrowserDialog = true
                  },
                  onOpenSocialHub = { tabIdx ->
                    socialDialogInitialTab = tabIdx
                    showSocialDialog = true
                  },
                  onEquipOutfit = viewModel::equipOutfit,
                  onSelectArea = viewModel::selectCustomArea,
                  onHuntHeroFrontier = {
                    viewModel.selectCustomArea("area_hero_frontier")
                    viewModel.selectTab(OsWorkspaceTab.ARENA_PLAY)
                  },
                  onSelectCollectionDifficulty = viewModel::selectWorldCollectionDifficulty,
                  onSelectNgPlusTier = viewModel::selectNewGamePlusTier,
                  onCompleteNgPlusRun = viewModel::completeNewGamePlusRun,
                  onApplyProtocolCode = viewModel::applyTierProtocolCode,
                  onBoostAllTiers = viewModel::boostAllTiers,
                  onMaxAllTiers = viewModel::maxAllTiers,
                  onMaxAllStars = viewModel::maxAllStars,
                  onGrantEssenceBonus = { viewModel.grantEssenceBonus(50_000L) },
                  onImportBo3Preset = viewModel::importBo3ModToolsCuratedPack,
                  onSignOut = onLockSession,
                )

                OsWorkspaceTab.ARENA_PLAY -> ArenaScreen(
                  profile = state.profile,
                  armory = state.armory,
                  rigs = state.rigs,
                  settings = appSettings,
                  progression = progression,
                  isPausedForSettings = isSettingsOpen,
                  onOpenSettings = onOpenSettings,
                  onRecordProgression = viewModel::recordArenaProgression,
                  onSendChatMessage = { ch, content, targetId, isTac ->
                    viewModel.sendChatMessage(
                      channel = ch,
                      content = content,
                      senderCallsign = state.profile.callsign,
                      targetFriendId = targetId,
                      isTactical = isTac,
                      safetyFilterEnabled = appSettings.chatSafetyFilter,
                    )
                  },
                  onSaveMatchAndExit = { score, wave, elims, extracted, durationSec ->
                    viewModel.submitMatchResult(
                      profile = state.profile,
                      universeId = state.profile.activeUniverseId,
                      mode = state.profile.activeMode,
                      score = score,
                      waveReached = wave,
                      eliminations = elims,
                      extracted = extracted,
                      durationSec = durationSec,
                    )
                    viewModel.selectTab(OsWorkspaceTab.OS_DESKTOP)
                  },
                )

                OsWorkspaceTab.FORGE_RIG -> ForgeRigScreen(
                  rigs = state.rigs,
                  activeRigId = state.profile.activeRigId,
                  onSelectRig = viewModel::selectActiveRig,
                  onSaveRig = viewModel::saveRig,
                  settings = appSettings,
                  progression = progression,
                  onStatusMessage = viewModel::showBanner,
                )

                OsWorkspaceTab.ARMORY -> ArmoryScreen(
                  armory = state.armory,
                  totalEssence = state.profile.totalEssence,
                  onToggleEquip = viewModel::toggleArmoryEquip,
                  onUpgradeItem = { item ->
                    viewModel.upgradeArmoryItem(item, state.profile.totalEssence)
                  },
                  onCreateCustomItem = viewModel::createCustomArmoryItem,
                  onDeleteItem = viewModel::deleteCustomArmoryItem,
                  progression = progression,
                  challenges = challenges,
                  achievements = achievements,
                  missions = missions,
                  activeWorldId = state.profile.activeUniverseId,
                  activeAreaId = progression.activeAreaId,
                  activeRig = state.rigs.firstOrNull { it.id == state.profile.activeRigId } ?: state.rigs.firstOrNull(),
                  onUpdateWeaponCustom = viewModel::updateWeaponCustomization,
                  onUpdateCharacterCustom = viewModel::updateCharacterCustomization,
                  onUpdateLevelCustom = viewModel::updateLevelCustomization,
                  onSelectOrUnlockPass = { version ->
                    viewModel.activateOrUnlockPass(version, state.profile.totalEssence)
                  },
                  onClaimPassTier = viewModel::claimPassTier,
                  onClaimChallenge = viewModel::claimChallengeReward,
                  onRefreshChallenges = viewModel::refreshCompletedChallenges,
                  onUpgradeEquipment = viewModel::upgradeEquipment,
                  onUpgradeEquipmentTier = { slot -> viewModel.upgradeEquipmentTier(slot, state.profile.totalEssence) },
                  onPrestigeEquipment = viewModel::prestigeEquipment,
                  onEquipEquipment = viewModel::equipEquipment,
                  onUpgradeGear = viewModel::upgradeGear,
                  onUpgradeGearTier = { slot -> viewModel.upgradeGearTier(slot, state.profile.totalEssence) },
                  onPrestigeGear = viewModel::prestigeGear,
                  onUnlockSkill = { nodeId -> viewModel.unlockSkill(nodeId, state.profile.totalEssence) },
                  onUpgradeWeaponTier = { wId -> viewModel.upgradeWeaponTier(wId, state.profile.totalEssence) },
                  onUpgradeWonderTier = { wId -> viewModel.upgradeWonderTier(wId, state.profile.totalEssence) },
                  onPrestigePlayer = viewModel::prestigePlayer,
                  onPrestigeWeapon = viewModel::prestigeWeapon,
                  onPurchaseStoreItem = { itemId -> viewModel.purchaseStoreItem(itemId, state.profile.totalEssence) },
                  onClaimAchievement = viewModel::claimAchievement,
                  onToggleMultiWeaponEquip = viewModel::toggleMultiWeaponEquip,
                  onToggleMultiWonderEquip = viewModel::toggleMultiWonderEquip,
                  onToggleMultiEquipmentEquip = viewModel::toggleMultiEquipmentEquip,
                  onEquipOutfit = viewModel::equipOutfit,
                  onUpgradeOutfit = { id -> viewModel.upgradeOutfit(id, state.profile.totalEssence) },
                  onUpgradeOutfitSubSystem = { id, part -> viewModel.upgradeOutfitSubSystem(id, part, state.profile.totalEssence) },
                  onSelectArea = viewModel::selectCustomArea,
                  onUpgradeAreaFeature = { id, feat -> viewModel.upgradeAreaFeature(id, feat, state.profile.totalEssence) },
                  onAdvanceAreaAge = viewModel::advanceAreaAge,
                  onSpendBlastShards = viewModel::spendBlastShardsUpgrade,
                  onUpgradeVacuumBot = { viewModel.upgradeVacuumBot(state.profile.totalEssence) },
                  onToggleVacuumAutoUpgrade = viewModel::toggleVacuumAutoUpgrade,
                  onUpgradePerkDrink = { perkId -> viewModel.upgradePerkDrink(perkId, state.profile.totalEssence) },
                  onUpgradeBlastCore = viewModel::upgradeBlastCore,
                  onExecuteTradeOffer = { offerId -> viewModel.executeTradeOffer(offerId, state.profile.totalEssence) },
                  onClaimMission = viewModel::claimMission,
                  onDeployMissionTarget = viewModel::deployMissionTarget,
                  onUpgradeArmoryItemStar = { itemId -> viewModel.upgradeArmoryItemStar(itemId, state.profile.totalEssence) },
                  onUpgradeCharacterTier = { viewModel.upgradeCharacterTier(state.profile.totalEssence) },
                  onUpgradeCharacterStar = { viewModel.upgradeCharacterStar(state.profile.totalEssence) },
                  onUpgradeOutfitStar = { id -> viewModel.upgradeOutfitStar(id, state.profile.totalEssence) },
                  onUpgradeWeaponStar = { wId -> viewModel.upgradeWeaponStar(wId, state.profile.totalEssence) },
                  onUpgradeWonderStar = { wId -> viewModel.upgradeWonderStar(wId, state.profile.totalEssence) },
                  onUpgradeGearStar = { slot -> viewModel.upgradeGearStar(slot, state.profile.totalEssence) },
                  onUpgradeEquipmentStar = { slot -> viewModel.upgradeEquipmentStar(slot, state.profile.totalEssence) },
                  onUpgradeSkillStar = { nodeId -> viewModel.upgradeSkillStar(nodeId, state.profile.totalEssence) },
                  onUpgradeVacuumBotStar = { viewModel.upgradeVacuumBotStar(state.profile.totalEssence) },
                  onUpgradeAreaStar = { areaId -> viewModel.upgradeAreaStar(areaId, state.profile.totalEssence) },
                  onUpgradeOutfitRarity = viewModel::upgradeOutfitRarity,
                  onRecruitCharacterShard = { outfitId -> viewModel.recruitCharacterShard(state.profile.totalEssence, outfitId) },
                  onHuntHeroFrontier = {
                    viewModel.selectCustomArea("area_hero_frontier")
                    viewModel.selectTab(OsWorkspaceTab.ARENA_PLAY)
                  },
                  onSelectNgPlusTier = viewModel::selectNewGamePlusTier,
                  onCompleteNgPlusRun = viewModel::completeNewGamePlusRun,
                  onSelectCollectionDifficulty = viewModel::selectWorldCollectionDifficulty,
                  onApplyProtocolCode = viewModel::applyTierProtocolCode,
                  onBoostAllTiers = viewModel::boostAllTiers,
                  onMaxAllTiers = viewModel::maxAllTiers,
                  onMaxAllStars = viewModel::maxAllStars,
                  onGrantEssenceBonus = { viewModel.grantEssenceBonus(50_000L) },
                  onImportBo3Preset = viewModel::importBo3ModToolsCuratedPack,
                )

                OsWorkspaceTab.MEDIA_VAULT -> MediaVaultScreen(
                  activeWallpaperPath = activeWallpaperPath,
                  onSetWallpaper = onSetWallpaperPath,
                  onDeploySeries = viewModel::mountAndDeploySeries,
                  onStatusMessage = viewModel::showBanner,
                )

                OsWorkspaceTab.EXPORT_COMPAT -> ExportCompatScreen(
                  profile = state.profile,
                  armory = state.armory,
                  rigs = state.rigs,
                  cloudSyncState = cloudSyncState,
                  isCloudSignedIn = isCloudSignedIn,
                  onSyncNow = viewModel::syncNow,
                  onRequestSaveExportJson = viewModel::exportSaveJson,
                  onParseSaveJson = viewModel::parseSavePreview,
                  onConfirmSaveImport = viewModel::importSavePreview,
                  onSelectConsolePreset = viewModel::selectConsolePreset,
                  onStatusMessage = viewModel::showBanner,
                  progression = progression,
                )
              }
            }
          }
        }
      }
    }

    val readyProfile = (uiState as? UniverseUiState.Ready)?.profile
    if (showStoreDialog && readyProfile != null) {
      BlueEssenceStoreDialog(
        progression = progression,
        totalBlueEssence = readyProfile.totalEssence,
        onPurchaseItem = { itemId -> viewModel.purchaseStoreItem(itemId, readyProfile.totalEssence) },
        onDismiss = { showStoreDialog = false },
        onExecuteTradeOffer = { offerId -> viewModel.executeTradeOffer(offerId, readyProfile.totalEssence) },
      )
    }

    if (showAchievementsDialog) {
      AchievementsDialog(
        achievements = achievements,
        onClaimAchievement = viewModel::claimAchievement,
        onDismiss = { showAchievementsDialog = false },
      )
    }

    if (showMissionsDialog && readyProfile != null) {
      MissionsDialog(
        missions = missions,
        activeWorldId = readyProfile.activeUniverseId,
        activeAreaId = progression.activeAreaId,
        onClaimMission = viewModel::claimMission,
        onDeployTarget = { scopeType, targetId ->
          viewModel.deployMissionTarget(scopeType, targetId)
          showMissionsDialog = false
        },
        onDismiss = { showMissionsDialog = false },
      )
    }

    if (showMatchBrowserDialog && readyProfile != null) {
      MatchBrowserDialog(
        progression = progression,
        playerCallsign = readyProfile.callsign,
        initialTabIdx = matchBrowserInitialTab,
        onHostPrivateMatch = { rules ->
          viewModel.hostPrivateMatch(rules, readyProfile.callsign)
          showMatchBrowserDialog = false
        },
        onJoinMatch = { match, passcode ->
          val ok = viewModel.joinMatch(match, passcode)
          if (ok) showMatchBrowserDialog = false
          ok
        },
        onLeaveMatch = viewModel::leaveJoinedMatch,
        onDismiss = { showMatchBrowserDialog = false },
      )
    }

    if (showSocialDialog && readyProfile != null) {
      SocialAndCommsDialog(
        progression = progression,
        playerCallsign = readyProfile.callsign,
        totalBlueEssence = readyProfile.totalEssence,
        initialTabIdx = socialDialogInitialTab,
        onAddFriend = { callsign -> viewModel.addFriend(callsign) },
        onRemoveFriend = viewModel::removeFriend,
        onToggleFavoriteFriend = viewModel::toggleFriendFavorite,
        onSendChatMessage = { ch, content, targetId, isTac ->
          viewModel.sendChatMessage(
            channel = ch,
            content = content,
            senderCallsign = readyProfile.callsign,
            targetFriendId = targetId,
            isTactical = isTac,
            safetyFilterEnabled = appSettings.chatSafetyFilter,
          )
        },
        onCreateClan = { name, tag, motto, color ->
          viewModel.createClan(name, tag, motto, color, readyProfile.callsign)
        },
        onJoinClan = { clan -> viewModel.joinClan(clan, readyProfile.callsign) },
        onDonateClan = { amount -> viewModel.donateToClan(amount, readyProfile.callsign, readyProfile.totalEssence) },
        onLeaveClan = { viewModel.leaveClan(readyProfile.callsign) },
        onJoinFriendMatch = { matchId ->
          val ok = viewModel.joinMatchById(matchId)
          if (ok) showSocialDialog = false
        },
        onDismiss = { showSocialDialog = false },
      )
    }
  }
}

private fun tabIcon(tab: OsWorkspaceTab) = when (tab) {
  OsWorkspaceTab.OS_DESKTOP -> AppIcons.DesktopWindows
  OsWorkspaceTab.ARENA_PLAY -> AppIcons.Gamepad
  OsWorkspaceTab.FORGE_RIG -> AppIcons.Animation
  OsWorkspaceTab.ARMORY -> AppIcons.Shield
  OsWorkspaceTab.MEDIA_VAULT -> AppIcons.PhotoLibrary
  OsWorkspaceTab.EXPORT_COMPAT -> AppIcons.Download
}

package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.example.data.local.UniverseRepository
import com.example.ui.OsWorkspaceTab
import com.example.ui.UniverseUiState
import com.example.ui.UniverseViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.GoogleSignInHelper
import com.example.ui.auth.OAuthCallbackResult
import com.example.ui.auth.PlatformOAuthManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.ArmoryScreen
import com.example.ui.screens.ExportCompatScreen
import com.example.ui.screens.ForgeRigScreen
import com.example.ui.screens.MediaVaultScreen
import com.example.ui.screens.OsDesktopScreen
import com.example.ui.theme.AppIcons
import com.example.ui.theme.UnknownUniversesTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val pendingOAuthRedirect = MutableStateFlow<Uri?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pendingOAuthRedirect.value = intent?.data
    enableEdgeToEdge()
    setContent {
      UnknownUniversesTheme(darkTheme = true) {
        AppNavigation(
          pendingOAuthRedirect = pendingOAuthRedirect.asStateFlow(),
          onConsumeRedirect = { pendingOAuthRedirect.value = null },
        )
      }
    }
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

  val currentUserId = signedInGoogleUid ?: LOCAL_OPERATOR_ID
  val repository = remember(context) { UniverseRepository(context.applicationContext) }
  val detectedBootLinks by remember(currentUserId, repository) {
    repository.observeLinkedAccounts(currentUserId)
  }.collectAsStateWithLifecycle(initialValue = emptyList())
  var authExternalNotice by remember { mutableStateOf<String?>(null) }
  var authExternalError by remember { mutableStateOf<String?>(null) }
  val bootOAuthRedirectUri by pendingOAuthRedirect.collectAsStateWithLifecycle()

  LaunchedEffect(bootOAuthRedirectUri, isSessionActive, currentUserId) {
    if (!isSessionActive) {
      val uri = bootOAuthRedirectUri ?: return@LaunchedEffect
      onConsumeRedirect()
      when (val outcome = PlatformOAuthManager.handleRedirectUri(context, uri)) {
        is OAuthCallbackResult.Linked -> {
          repository.saveLinkedAccount(currentUserId, outcome.providerId, outcome.displayName, outcome.externalSubject)
          outcome.bridgedAccounts.forEach { bridged ->
            repository.saveLinkedAccount(currentUserId, bridged.providerId, bridged.displayName, bridged.externalSubject)
          }
          authExternalError = null
          authExternalNotice = if (outcome.bridgedAccounts.isEmpty()) {
            "Signed in with ${outcome.displayName}. Tap Boot UnknownOS to enter."
          } else {
            "Signed in with ${outcome.displayName} (+${outcome.bridgedAccounts.size} bridged accounts)."
          }
        }
        is OAuthCallbackResult.Failed -> {
          authExternalError = outcome.message
        }
        null -> Unit
      }
    }
  }

  if (!isSessionActive) {
    AuthScreen(
      initialCallsign = activeCallsign,
      linkedAccounts = detectedBootLinks,
      externalAuthNotice = authExternalNotice,
      externalAuthError = authExternalError,
      onLinkExternalAccount = { providerId, displayName, externalSubject ->
        scope.launch {
          repository.ensureStarterData(currentUserId, activeCallsign)
          repository.saveLinkedAccount(currentUserId, providerId, displayName, externalSubject)
        }
      },
      onUnlinkExternalAccount = { providerId ->
        scope.launch {
          repository.unlinkAccount(currentUserId, providerId)
          authExternalError = null
          authExternalNotice = "Signed out of $providerId."
        }
      },
      onBootLocalOperator = { callsign ->
        activeCallsign = callsign
        prefs.edit {
          putBoolean(KEY_SESSION_ACTIVE, true)
          putString(KEY_LAST_CALLSIGN, callsign)
        }
        isSessionActive = true
      },
    )
  } else {
    key(currentUserId) {
      UnknownOsShell(
        currentUserId = currentUserId,
        displayName = activeCallsign,
        googleAccountLabel = googleAccountLabel,
        activeWallpaperPath = activeWallpaperPath,
        onSetWallpaperPath = { path ->
          activeWallpaperPath = path
          prefs.edit {
            if (path == null) remove(KEY_VAULT_WALLPAPER)
            else putString(KEY_VAULT_WALLPAPER, path)
          }
        },
        pendingOAuthRedirect = pendingOAuthRedirect,
        onConsumeRedirect = onConsumeRedirect,
        onLockSession = {
          prefs.edit { putBoolean(KEY_SESSION_ACTIVE, false) }
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
}

@Composable
fun UnknownOsShell(
  currentUserId: String,
  displayName: String?,
  googleAccountLabel: String?,
  activeWallpaperPath: String?,
  onSetWallpaperPath: (String?) -> Unit,
  pendingOAuthRedirect: StateFlow<Uri?>,
  onConsumeRedirect: () -> Unit,
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
        )
      }
    },
  )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()
  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
  val oauthRedirectUri by pendingOAuthRedirect.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val isCloudSignedIn = runCatching { Firebase.auth.currentUser?.uid == currentUserId }.getOrDefault(false)

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

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = {
        SnackbarHost(hostState = snackbarHostState) { data ->
          Snackbar(snackbarData = data)
        }
      },
      bottomBar = {
        if (!useNavRail) {
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
        if (useNavRail) {
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
                  onSignOut = onLockSession,
                )

                OsWorkspaceTab.ARENA_PLAY -> ArenaScreen(
                  profile = state.profile,
                  armory = state.armory,
                  rigs = state.rigs,
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
                )
              }
            }
          }
        }
      }
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

package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.AppLanguage
import com.example.data.local.AppSettings
import com.example.data.local.AppSettingsStore
import com.example.data.local.GameStrings
import com.example.data.local.GameStringsCatalog
import com.example.data.local.LocalGameStrings
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
import com.example.ui.screens.MultiverseSystemsHubDialog
import com.example.data.model.CustomMapGameCategory
import com.example.data.model.SocialCatalog
import com.example.ui.screens.ForgeArmoryItemDialog
import com.example.ui.screens.ForgeCustomEquipmentDialog
import com.example.ui.screens.ForgeCustomGearDialog
import com.example.ui.screens.ForgeCustomMapDialog
import com.example.ui.screens.ForgeCustomPerkDialog
import com.example.ui.screens.ForgeCustomWeaponDialog
import com.example.ui.theme.AppIcons
import com.example.ui.theme.UnknownUniversesTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
  private val pendingOAuthRedirect = MutableStateFlow<Uri?>(null)
  @Volatile private var shouldHideSystemBars: Boolean = true

  fun updateSystemBarsVisibility(hide: Boolean) {
    shouldHideSystemBars = hide
    com.example.util.applyWindowFullscreen(window, hide)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    com.example.util.AppCrashShield.install(application)
    pendingOAuthRedirect.value = intent?.data
    enableEdgeToEdge()
    com.example.util.applyWindowFullscreen(window, shouldHideSystemBars)
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
    com.example.util.applyWindowFullscreen(window, shouldHideSystemBars)
    com.example.util.BackgroundMusicManager.resumeMusic()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      com.example.util.applyWindowFullscreen(window, shouldHideSystemBars)
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    com.example.util.applyWindowFullscreen(window, shouldHideSystemBars)
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

private tailrec fun Context.findActivity(): Activity? =
  this as? Activity ?: (this as? ContextWrapper)?.baseContext?.findActivity()

private fun OsWorkspaceTab.localizedLabel(s: GameStrings): String = when (this) {
  OsWorkspaceTab.OS_DESKTOP -> s.tabOs
  OsWorkspaceTab.ARENA_PLAY -> s.tabArena
  OsWorkspaceTab.FORGE_RIG -> s.tabForge
  OsWorkspaceTab.ARMORY -> s.tabArmory
  OsWorkspaceTab.MEDIA_VAULT -> s.tabVault
  OsWorkspaceTab.EXPORT_COMPAT -> s.tabExport
}

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
    val window = context.findActivity()?.window
    if (appSettings.screenCaptureShield) {
      window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
      window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    onDispose {}
  }

  val hostActivity = remember(context) { context.findActivity() }
  val activityResultOwner =
    LocalActivityResultRegistryOwner.current ?: (hostActivity as? ActivityResultRegistryOwner)
  val backPressedOwner =
    LocalOnBackPressedDispatcherOwner.current ?: (hostActivity as? OnBackPressedDispatcherOwner)

  val activeLanguage = appSettings.activeLanguage
  val activeLocale = remember(activeLanguage) { activeLanguage.toLocale() }
  val localizedResources = remember(context, activeLanguage) {
    val config = Configuration(context.resources.configuration)
    config.setLocale(activeLocale)
    config.setLayoutDirection(activeLocale)
    context.createConfigurationContext(config).resources
  }
  val localizedContext = remember(context, localizedResources) {
    object : ContextWrapper(context) {
      override fun getResources(): Resources = localizedResources
    }
  }
  val gameStrings = remember(activeLanguage) { GameStringsCatalog.forLanguage(activeLanguage) }
  val layoutDirection = if (activeLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

  val currentUserId = signedInGoogleUid ?: LOCAL_OPERATOR_ID

  val providedLocals = buildList {
    add(LocalContext provides localizedContext)
    add(LocalConfiguration provides localizedResources.configuration)
    add(LocalLayoutDirection provides layoutDirection)
    add(LocalGameStrings provides gameStrings)
    add(com.example.util.LocalHideSystemBars provides appSettings.shouldHideSystemBars(inArena = false))
    if (activityResultOwner != null) {
      add(LocalActivityResultRegistryOwner provides activityResultOwner)
    }
    if (backPressedOwner != null) {
      add(LocalOnBackPressedDispatcherOwner provides backPressedOwner)
    }
  }.toTypedArray()

  CompositionLocalProvider(*providedLocals) {
  UnknownUniversesTheme(
    darkTheme = appSettings.effectiveDarkTheme,
    settings = appSettings,
  ) {
    if (!isSessionActive) {
      AuthScreen(
        initialCallsign = activeCallsign,
        selectedLanguageCode = activeLanguage.code,
        onSelectLanguage = { code -> settingsStore.selectLanguage(code) },
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
          onUpdateSettings = { transform -> settingsStore.update(transform) },
          onSetWallpaperPath = { path ->
            activeWallpaperPath = path
            prefs.edit {
              if (path == null) remove(KEY_VAULT_WALLPAPER)
              else putString(KEY_VAULT_WALLPAPER, path)
            }
          },
          onSelectLanguage = { code -> settingsStore.selectLanguage(code) },
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
        onSelectRegion = { id -> settingsStore.selectRegion(id) },
        onSelectServer = { id -> settingsStore.selectServer(id) },
        onToggleRegionDefaultSettings = { enabled -> settingsStore.setUseRegionDefaultSettings(enabled) },
        onSelectLanguage = { code -> settingsStore.selectLanguage(code) },
      )
    }
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
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit = {},
  onSetWallpaperPath: (String?) -> Unit,
  onSelectLanguage: (String) -> Unit = {},
  pendingOAuthRedirect: StateFlow<Uri?>,
  onConsumeRedirect: () -> Unit,
  onRegisterIntegrityCheck: (Boolean, () -> Boolean) -> Unit = { _, _ -> },
  onLockSession: () -> Unit,
) {
  val context = LocalContext.current
  val gameStrings = LocalGameStrings.current
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
  val selectedArmorySection by viewModel.selectedArmorySection.collectAsStateWithLifecycle()
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
  var showHubForgeWeapon by remember { mutableStateOf(false) }
  var showHubForgeMap by remember { mutableStateOf(false) }
  var showHubForgeModule by remember { mutableStateOf(false) }
  var showHubForgePerk by remember { mutableStateOf(false) }
  var showHubForgeGear by remember { mutableStateOf(false) }
  var showHubForgeEquipment by remember { mutableStateOf(false) }
  var showMultiverseHubDialog by remember { mutableStateOf(false) }
  var multiverseHubInitialTab by remember { mutableIntStateOf(0) }
  var cachedArenaSimKey by remember { mutableStateOf<String?>(null) }
  var cachedArenaSim by remember { mutableStateOf<com.example.ui.screens.ArenaSimState?>(null) }

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

  val inArena = activeTab == OsWorkspaceTab.ARENA_PLAY
  val hideSystemBars = appSettings.shouldHideSystemBars(inArena = inArena)
  val hideChromeBarsForArena = appSettings.immersiveFullscreen && inArena
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(hideSystemBars, context, lifecycleOwner) {
    val activity = context.findActivity()
    (activity as? MainActivity)?.updateSystemBarsVisibility(hideSystemBars)
      ?: com.example.util.applyWindowFullscreen(activity?.window, hideSystemBars)
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        (activity as? MainActivity)?.updateSystemBarsVisibility(hideSystemBars)
          ?: com.example.util.applyWindowFullscreen(activity?.window, hideSystemBars)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val baseDensity = LocalDensity.current
  val safeMenuScale = if (appSettings.menuScale.isFinite()) appSettings.menuScale.coerceIn(0.75f, 1.35f) else 1.0f
  val scaledMenuDensity = remember(baseDensity, safeMenuScale, inArena) {
    if (inArena || safeMenuScale == 1.0f) {
      baseDensity
    } else {
      Density(
        density = (baseDensity.density * safeMenuScale).coerceAtLeast(0.75f),
        fontScale = baseDensity.fontScale,
      )
    }
  }

  CompositionLocalProvider(
    LocalDensity provides scaledMenuDensity,
    com.example.util.LocalHideSystemBars provides hideSystemBars,
  ) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = if (hideSystemBars) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
      snackbarHost = {
        SnackbarHost(hostState = snackbarHostState) { data ->
          Snackbar(snackbarData = data)
        }
      },
      bottomBar = {
        if (!useNavRail && !hideChromeBarsForArena) {
          NavigationBar(
            windowInsets = if (hideSystemBars) WindowInsets(0, 0, 0, 0) else WindowInsets.navigationBars,
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            OsWorkspaceTab.entries.forEach { tab ->
              val tabText = tab.localizedLabel(gameStrings)
              NavigationBarItem(
                selected = activeTab == tab,
                onClick = { viewModel.selectTab(tab) },
                icon = {
                  Icon(
                    imageVector = tabIcon(tab),
                    contentDescription = tabText,
                  )
                },
                label = { Text(tabText, maxLines = 1) },
                modifier = Modifier.testTag("nav_${tab.routeTag}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      val cutoutModifier = if (!inArena) {
        Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
      } else {
        Modifier
      }
      Row(
        modifier = Modifier.fillMaxSize().padding(innerPadding).then(cutoutModifier)
      ) {
        if (useNavRail && !hideChromeBarsForArena) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(),
          ) {
            OsWorkspaceTab.entries.forEach { tab ->
              val tabText = tab.localizedLabel(gameStrings)
              NavigationRailItem(
                selected = activeTab == tab,
                onClick = { viewModel.selectTab(tab) },
                icon = {
                  Icon(
                    imageVector = tabIcon(tab),
                    contentDescription = tabText,
                  )
                },
                label = { Text(tabText) },
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
                  onOpenVehiclesArmory = {
                    viewModel.selectArmorySection("vehicles")
                    viewModel.selectTab(OsWorkspaceTab.ARMORY)
                  },
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
                    viewModel.deployToAreaAndPlay("area_hero_frontier")
                  },
                  onSelectCollectionDifficulty = viewModel::selectWorldCollectionDifficulty,
                  onSelectNgPlusTier = viewModel::selectNewGamePlusTier,
                  onCompleteNgPlusRun = viewModel::completeNewGamePlusRun,
                  onApplyProtocolCode = viewModel::applyTierProtocolCode,
                  onBoostAllTiers = viewModel::boostAllTiers,
                  onMaxAllTiers = viewModel::maxAllTiers,
                  onMaxAllStars = viewModel::maxAllStars,
                  onOpenMultiverseHub = { tabIdx ->
                    multiverseHubInitialTab = tabIdx
                    showMultiverseHubDialog = true
                  },
                  onGrantEssenceBonus = { viewModel.grantEssenceBonus(50_000L) },
                  onImportBo3Preset = viewModel::importBo3ModToolsCuratedPack,
                  onDeployArea = viewModel::deployToAreaAndPlay,
                  onUpgradeAreaFeature = { id, feat -> viewModel.upgradeAreaFeature(id, feat, state.profile.totalEssence) },
                  onUpgradeAreaStar = { areaId -> viewModel.upgradeAreaStar(areaId, state.profile.totalEssence) },
                  onUpgradeModeTier = { modeId -> viewModel.upgradeModeTier(modeId, state.profile.totalEssence) },
                  onUpgradeModeStar = { modeId -> viewModel.upgradeModeStar(modeId, state.profile.totalEssence) },
                  onUpdateCollectionCustomMode = viewModel::updateCollectionCustomMode,
                  onDeployCollectionCustomMode = viewModel::deployCollectionCustomMode,
                  activeLanguage = appSettings.activeLanguage,
                  appSettings = appSettings,
                  onUpdateSettings = onUpdateSettings,
                  onSelectLanguage = onSelectLanguage,
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
                  onSelectArea = viewModel::selectCustomArea,
                  onSelectMode = viewModel::selectMode,
                  onUpdateCollectionCustomMode = viewModel::updateCollectionCustomMode,
                  onDeployCollectionCustomMode = viewModel::deployCollectionCustomMode,
                  externalSimProvider = { simKey, factory ->
                    val existing = cachedArenaSim
                    if (existing != null && cachedArenaSimKey == simKey && !existing.hudRunEnded) {
                      existing
                    } else {
                      val created = factory()
                      cachedArenaSimKey = simKey
                      cachedArenaSim = created
                      created
                    }
                  },
                  onSaveMatchAndExit = { score, wave, elims, extracted, durationSec ->
                    cachedArenaSimKey = null
                    cachedArenaSim = null
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
                  onImportPrisma3dResult = viewModel::importPrisma3dAsset,
                  onOpenForgeWeapon = { showHubForgeWeapon = true },
                  onOpenForgeMap = { showHubForgeMap = true },
                  onOpenForgeModule = { showHubForgeModule = true },
                  onOpenForgePerk = { showHubForgePerk = true },
                  onOpenForgeGear = { showHubForgeGear = true },
                  onOpenForgeEquipment = { showHubForgeEquipment = true },
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
                  onUpdateWonderCustom = viewModel::updateWonderWeaponCustomization,
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
                  onDeployArea = viewModel::deployToAreaAndPlay,
                  onUpdateCollectionCustomMode = viewModel::updateCollectionCustomMode,
                  onDeployCollectionCustomMode = viewModel::deployCollectionCustomMode,
                  onForgeCustomMap = viewModel::createCustomGameMap,
                  onDeleteCustomMap = viewModel::deleteCustomGameMap,
                  onForgeCustomWeapon = viewModel::createCustomWeapon,
                  onDeleteCustomWeapon = viewModel::deleteCustomWeapon,
                  onUpgradeCustomPerkTier = { perkId -> viewModel.upgradeCustomPerkTier(perkId, state.profile.totalEssence) },
                  onUpgradeCustomPerkStar = { perkId -> viewModel.upgradeCustomPerkStar(perkId, state.profile.totalEssence) },
                  onUpgradeCustomGearTier = { gearId -> viewModel.upgradeCustomGearTier(gearId, state.profile.totalEssence) },
                  onUpgradeCustomGearStar = { gearId -> viewModel.upgradeCustomGearStar(gearId, state.profile.totalEssence) },
                  onEquipCustomGear = viewModel::equipCustomGear,
                  onUpgradeCustomEquipmentTier = { itemId -> viewModel.upgradeCustomEquipmentTier(itemId, state.profile.totalEssence) },
                  onUpgradeCustomEquipmentStar = { itemId -> viewModel.upgradeCustomEquipmentStar(itemId, state.profile.totalEssence) },
                  onForgeCustomPerk = viewModel::addOrUpdateCustomPerk,
                  onForgeCustomGear = viewModel::addOrUpdateCustomGear,
                  onForgeCustomEquipment = viewModel::addOrUpdateCustomEquipment,
                  persistedSectionTag = if (selectedArmorySection == "armory_items") "mods" else selectedArmorySection,
                  onPersistSectionTag = viewModel::selectArmorySection,
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
                  onConfirmWorkshopImport = viewModel::importSteamWorkshopPreview,
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

    if (showMultiverseHubDialog) {
      MultiverseSystemsHubDialog(
        progression = progression,
        onSelectMagicSpell = viewModel::selectMagicSpell,
        onUnlockOrUpgradeMagicSpell = viewModel::unlockOrUpgradeMagicSpell,
        onUpgradeMagicTier = viewModel::upgradeMagicTier,
        onAdjustMagicDifficulty = viewModel::adjustMagicDifficulty,
        onToggleMagicOvercharge = viewModel::toggleMagicOvercharge,
        onCraftCrystal = viewModel::craftCrystal,
        onUpgradeCrystal = viewModel::upgradeCrystal,
        onSocketCrystal = viewModel::socketCrystal,
        onCustomizePlayerBase = viewModel::customizePlayerBase,
        onUpgradePlayerBaseModule = viewModel::upgradePlayerBaseModule,
        onCollectBaseVault = viewModel::collectBaseVault,
        onTriggerOrResolveBaseAttack = viewModel::triggerOrResolveBaseAttack,
        onConvertCurrencyOrResource = viewModel::convertCurrencyOrResource,
        onUpgradeTargetWithCurrency = viewModel::upgradeTargetWithCurrency,
        onBoostAllTiers = viewModel::boostAllTiers,
        onBoostAllStars = viewModel::boostAllStars,
        onBoostEverythingPlusTen = viewModel::boostEverythingPlusTen,
        onMaxAllTiers = viewModel::maxAllTiers,
        onMaxAllStars = viewModel::maxAllStars,
        onGrantDiamondBonus = viewModel::grantDiamondBonus,
        onDeployArea = { areaId ->
          showMultiverseHubDialog = false
          viewModel.deployToAreaAndPlay(areaId)
        },
        onDismiss = { showMultiverseHubDialog = false },
        initialTab = multiverseHubInitialTab,
      )
    }

    if (showHubForgeWeapon) {
      ForgeCustomWeaponDialog(
        initialCategory = CustomMapGameCategory.BO3,
        onDismiss = { showHubForgeWeapon = false },
        onConfirm = { name, code, category, desc, dmgType, dmg, cadence, spread, pierce, hex, equipNow ->
          viewModel.createCustomWeapon(name, code, category, desc, dmgType, dmg, cadence, spread, pierce, hex, equipNow)
          showHubForgeWeapon = false
        },
      )
    }

    if (showHubForgeMap) {
      ForgeCustomMapDialog(
        initialCategory = CustomMapGameCategory.BO3,
        onDismiss = { showHubForgeMap = false },
        onConfirm = { title, code, category, desc, zones, ww, hex, deployNow ->
          viewModel.createCustomGameMap(title, code, category, desc, zones, ww, hex, deployNow)
          showHubForgeMap = false
        },
      )
    }

    if (showHubForgeModule) {
      ForgeArmoryItemDialog(
        onDismiss = { showHubForgeModule = false },
        onConfirm = { name, cat, tag, desc, cd, power, hex ->
          viewModel.createCustomArmoryItem(name, cat, tag, desc, cd, power, hex)
          showHubForgeModule = false
        },
      )
    }

    if (showHubForgePerk) {
      ForgeCustomPerkDialog(
        onDismiss = { showHubForgePerk = false },
        onConfirm = { spec ->
          viewModel.addOrUpdateCustomPerk(spec)
          showHubForgePerk = false
        },
      )
    }

    if (showHubForgeGear) {
      ForgeCustomGearDialog(
        onDismiss = { showHubForgeGear = false },
        onConfirm = { spec ->
          viewModel.addOrUpdateCustomGear(spec)
          showHubForgeGear = false
        },
      )
    }

    if (showHubForgeEquipment) {
      ForgeCustomEquipmentDialog(
        onDismiss = { showHubForgeEquipment = false },
        onConfirm = { spec ->
          viewModel.addOrUpdateCustomEquipment(spec)
          showHubForgeEquipment = false
        },
      )
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

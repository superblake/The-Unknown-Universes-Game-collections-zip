package com.example.ui.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

enum class OAuthProtocol {
  OAUTH2_PKCE,
  STEAM_OPENID_2,
}

data class ExternalOAuthProviderSpec(
  val id: String,
  val label: String,
  val platformGroup: String,
  val protocol: OAuthProtocol,
  val authEndpoint: String,
  val tokenEndpoint: String,
  val scope: String,
  val secretKeyName: String,
  val portalUrl: String = "",
  val handleHint: String = "",
  val accentHex: Long,
  private val configValueProvider: () -> String,
) {
  fun configuredValue(context: Context? = null): String {
    val runtimeVal = context?.let { PlatformOAuthManager.getRuntimeClientId(it, id) }.orEmpty()
    return if (runtimeVal.isNotBlank()) runtimeVal.trim() else configValueProvider().trim()
  }

  fun isConfigured(context: Context? = null): Boolean {
    val v = configuredValue(context)
    return v.isNotEmpty() && !v.startsWith("MY_")
  }

  fun redirectUri(): String = "unknownuniverses://auth/$id"
}

data class PlatformPortalSpec(
  val providerId: String,
  val label: String,
  val platformNote: String,
  val portalUrl: String,
  val handleHint: String,
  val accentHex: Long,
)

data class UnifiedPlatformMeta(
  val providerId: String,
  val label: String,
  val subtitle: String,
  val portalUrl: String,
  val handleHint: String,
  val accentHex: Long,
  val oauthSpec: ExternalOAuthProviderSpec? = null,
)

data class PendingPlatformAuth(
  val providerId: String,
  val label: String,
  val isOAuthFlow: Boolean,
  val portalUrl: String,
  val handleHint: String,
  val startedAtMillis: Long,
)

sealed interface OAuthCallbackResult {
  data class Linked(
    val providerId: String,
    val displayName: String,
    val externalSubject: String,
    val bridgedAccounts: List<BridgedExternalAccount> = emptyList(),
  ) : OAuthCallbackResult

  data class Failed(val message: String) : OAuthCallbackResult
}

object PlatformOAuthManager {
  private const val PREFS_NAME = "unknown_os_oauth_pending"
  private const val CLIENT_OVERRIDES_PREFS = "unknown_os_oauth_clients"
  private const val KEY_STATE = "pending_state"
  private const val KEY_VERIFIER = "pending_verifier"
  private const val KEY_PROVIDER = "pending_provider"
  private const val KEY_PENDING_LABEL = "pending_label"
  private const val KEY_PENDING_IS_OAUTH = "pending_is_oauth"
  private const val KEY_PENDING_PORTAL_URL = "pending_portal_url"
  private const val KEY_PENDING_HINT = "pending_hint"
  private const val KEY_PENDING_STARTED_AT = "pending_started_at"
  private const val PENDING_TIMEOUT_MILLIS = 15L * 60L * 1000L
  private const val EPIC_CONNECTIONS_URL = "https://www.epicgames.com/account/connections"

  private val _pendingAuth = MutableStateFlow<PendingPlatformAuth?>(null)
  val pendingAuth: StateFlow<PendingPlatformAuth?> = _pendingAuth.asStateFlow()

  val platformPortalProviders: List<PlatformPortalSpec> = listOf(
    PlatformPortalSpec(
      providerId = "apple",
      label = "Apple / iOS",
      platformNote = "Sign in via Apple Account portal for iOS Safari & Apple bindings",
      portalUrl = "https://account.apple.com/",
      handleHint = "Apple ID / Game Center nickname",
      accentHex = 0xFFE2E8F0,
    ),
    PlatformPortalSpec(
      providerId = "xbox",
      label = "Xbox / Microsoft",
      platformNote = "Sign in via Xbox Live / Microsoft account portal",
      portalUrl = "https://account.xbox.com/",
      handleHint = "Xbox Gamertag (e.g. Operator#1234)",
      accentHex = 0xFF107C10,
    ),
  )

  val externalOAuthProviders: List<ExternalOAuthProviderSpec> = listOf(
    ExternalOAuthProviderSpec(
      id = "epic",
      label = "Epic Games",
      platformGroup = "PC / Cross-Platform Bridge",
      protocol = OAuthProtocol.OAUTH2_PKCE,
      authEndpoint = "https://www.epicgames.com/id/authorize",
      tokenEndpoint = "https://api.epicgames.dev/epic/oauth/v2/token",
      scope = "basic_profile",
      secretKeyName = "EPIC_CLIENT_ID",
      portalUrl = "https://www.epicgames.com/account/connections",
      handleHint = "Epic Games Display Name",
      accentHex = 0xFFB0BEC5,
      configValueProvider = { BuildConfig.EPIC_CLIENT_ID },
    ),
    ExternalOAuthProviderSpec(
      id = "steam",
      label = "Steam",
      platformGroup = "PC / SteamOS",
      protocol = OAuthProtocol.STEAM_OPENID_2,
      authEndpoint = "https://steamcommunity.com/openid/login",
      tokenEndpoint = "https://steamcommunity.com/openid/login",
      scope = "",
      secretKeyName = "STEAM_OPENID_RETURN_URL",
      portalUrl = "https://store.steampowered.com/login/",
      handleHint = "Steam Vanity ID, 17-digit SteamID64, or profile URL",
      accentHex = 0xFF66C0F4,
      configValueProvider = { BuildConfig.STEAM_OPENID_RETURN_URL },
    ),
    ExternalOAuthProviderSpec(
      id = "gog",
      label = "GOG Galaxy",
      platformGroup = "PC DRM-Free",
      protocol = OAuthProtocol.OAUTH2_PKCE,
      authEndpoint = "https://auth.gog.com/auth",
      tokenEndpoint = "https://auth.gog.com/token",
      scope = "",
      secretKeyName = "GOG_CLIENT_ID",
      portalUrl = "https://www.gog.com/account",
      handleHint = "GOG Username",
      accentHex = 0xFFBA68C8,
      configValueProvider = { BuildConfig.GOG_CLIENT_ID },
    ),
    ExternalOAuthProviderSpec(
      id = "ea_origin",
      label = "EA App / Origin",
      platformGroup = "PC / EA",
      protocol = OAuthProtocol.OAUTH2_PKCE,
      authEndpoint = "https://accounts.ea.com/connect/auth",
      tokenEndpoint = "https://accounts.ea.com/connect/token",
      scope = "basic.identity",
      secretKeyName = "EA_CLIENT_ID",
      portalUrl = "https://myaccount.ea.com/cp-ui/connectaccounts/index",
      handleHint = "EA ID / Origin Handle",
      accentHex = 0xFFFF5252,
      configValueProvider = { BuildConfig.EA_CLIENT_ID },
    ),
    ExternalOAuthProviderSpec(
      id = "amazon",
      label = "Amazon / Appstore",
      platformGroup = "Amazon / Fire",
      protocol = OAuthProtocol.OAUTH2_PKCE,
      authEndpoint = "https://www.amazon.com/ap/oa",
      tokenEndpoint = "https://api.amazon.com/auth/o2/token",
      scope = "profile",
      secretKeyName = "AMAZON_CLIENT_ID",
      portalUrl = "https://www.amazon.com/ap/signin",
      handleHint = "Amazon Gamer / Account Name",
      accentHex = 0xFFFFAB40,
      configValueProvider = { BuildConfig.AMAZON_CLIENT_ID },
    ),
    ExternalOAuthProviderSpec(
      id = "galaxy_store",
      label = "Samsung Galaxy Store",
      platformGroup = "Galaxy Android",
      protocol = OAuthProtocol.OAUTH2_PKCE,
      authEndpoint = "https://account.samsung.com/accounts/v1/SA/authorize",
      tokenEndpoint = "https://auth.samsung.com/auth/oauth2/v2/token",
      scope = "openid profile",
      secretKeyName = "SAMSUNG_CLIENT_ID",
      portalUrl = "https://account.samsung.com/",
      handleHint = "Samsung Account ID or Nickname",
      accentHex = 0xFF5C6BC0,
      configValueProvider = { BuildConfig.SAMSUNG_CLIENT_ID },
    ),
  )

  val epicBridgedPlatforms: List<PlatformPortalSpec> = listOf(
    PlatformPortalSpec(
      providerId = "psn",
      label = "PlayStation Network (PS5 / PS4 / PS3)",
      platformNote = "Link via PlayStation sign-in portal or Epic Connections",
      portalUrl = "https://www.playstation.com/acct/management",
      handleHint = "PSN Online ID",
      accentHex = 0xFF2979FF,
    ),
    PlatformPortalSpec(
      providerId = "nintendo",
      label = "Nintendo Account (Switch / Legacy)",
      platformNote = "Link via Nintendo Account portal or Epic Connections",
      portalUrl = "https://accounts.nintendo.com/",
      handleHint = "Nintendo Nickname or SW-Friend Code",
      accentHex = 0xFFFF3D71,
    ),
    PlatformPortalSpec(
      providerId = "ubisoft",
      label = "Ubisoft Connect",
      platformNote = "Link via Ubisoft Account portal or Epic Connections",
      portalUrl = "https://account.ubisoft.com/",
      handleHint = "Ubisoft Username",
      accentHex = 0xFF7C4DFF,
    ),
  )

  fun allUnifiedPlatforms(): List<UnifiedPlatformMeta> {
    val portals = platformPortalProviders.map {
      UnifiedPlatformMeta(
        providerId = it.providerId,
        label = it.label,
        subtitle = it.platformNote,
        portalUrl = it.portalUrl,
        handleHint = it.handleHint,
        accentHex = it.accentHex,
      )
    }
    val oauths = externalOAuthProviders.map {
      UnifiedPlatformMeta(
        providerId = it.id,
        label = it.label,
        subtitle = it.platformGroup,
        portalUrl = it.portalUrl,
        handleHint = it.handleHint.ifBlank { "${it.label} handle" },
        accentHex = it.accentHex,
        oauthSpec = it,
      )
    }
    val bridged = epicBridgedPlatforms.map {
      UnifiedPlatformMeta(
        providerId = it.providerId,
        label = it.label,
        subtitle = it.platformNote,
        portalUrl = it.portalUrl,
        handleHint = it.handleHint,
        accentHex = it.accentHex,
      )
    }
    return portals + oauths + bridged
  }

  fun findPlatformMeta(providerId: String): UnifiedPlatformMeta? =
    allUnifiedPlatforms().firstOrNull { it.providerId == providerId }

  fun getRuntimeClientId(context: Context, providerId: String): String =
    context.getSharedPreferences(CLIENT_OVERRIDES_PREFS, Context.MODE_PRIVATE)
      .getString("client_$providerId", null)
      .orEmpty()

  fun saveRuntimeClientId(context: Context, providerId: String, clientId: String) {
    context.getSharedPreferences(CLIENT_OVERRIDES_PREFS, Context.MODE_PRIVATE).edit {
      val trimmed = clientId.trim()
      if (trimmed.isEmpty()) remove("client_$providerId")
      else putString("client_$providerId", trimmed)
    }
  }

  fun restorePendingAuth(context: Context): PendingPlatformAuth? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val providerId = prefs.getString(KEY_PROVIDER, null)?.takeIf { it.isNotBlank() } ?: run {
      _pendingAuth.value = null
      return null
    }
    val startedAt = prefs.getLong(KEY_PENDING_STARTED_AT, 0L)
    if (startedAt > 0L && System.currentTimeMillis() - startedAt > PENDING_TIMEOUT_MILLIS) {
      clearPendingAuth(context)
      return null
    }
    val meta = findPlatformMeta(providerId)
    val restored = PendingPlatformAuth(
      providerId = providerId,
      label = prefs.getString(KEY_PENDING_LABEL, null) ?: meta?.label ?: providerId,
      isOAuthFlow = prefs.getBoolean(KEY_PENDING_IS_OAUTH, false),
      portalUrl = prefs.getString(KEY_PENDING_PORTAL_URL, null) ?: meta?.portalUrl.orEmpty(),
      handleHint = prefs.getString(KEY_PENDING_HINT, null) ?: meta?.handleHint.orEmpty(),
      startedAtMillis = startedAt,
    )
    _pendingAuth.value = restored
    return restored
  }

  fun clearPendingAuth(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { clear() }
    _pendingAuth.value = null
  }

  private fun savePendingAuth(
    context: Context,
    pending: PendingPlatformAuth,
    state: String? = null,
    verifier: String? = null,
  ) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
      putString(KEY_PROVIDER, pending.providerId)
      putString(KEY_PENDING_LABEL, pending.label)
      putBoolean(KEY_PENDING_IS_OAUTH, pending.isOAuthFlow)
      putString(KEY_PENDING_PORTAL_URL, pending.portalUrl)
      putString(KEY_PENDING_HINT, pending.handleHint)
      putLong(KEY_PENDING_STARTED_AT, pending.startedAtMillis)
      if (state != null) putString(KEY_STATE, state) else remove(KEY_STATE)
      if (verifier != null) putString(KEY_VERIFIER, verifier) else remove(KEY_VERIFIER)
    }
    _pendingAuth.value = pending
  }

  fun openEpicConnectionsPortal(context: Context): Result<Unit> =
    openPortalUrl(context, EPIC_CONNECTIONS_URL)

  fun openPortalUrl(context: Context, url: String): Result<Unit> {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
      addCategory(Intent.CATEGORY_BROWSABLE)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
      context.startActivity(intent)
      Result.success(Unit)
    } catch (_: ActivityNotFoundException) {
      Result.failure(IllegalStateException("No web browser installed on this device to open $url"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun beginPortalSignIn(context: Context, providerId: String): Result<PendingPlatformAuth> {
    val meta = findPlatformMeta(providerId)
      ?: return Result.failure(IllegalArgumentException("Unsupported platform: $providerId"))
    val oauthSpec = meta.oauthSpec
    if (oauthSpec != null && oauthSpec.isConfigured(context)) {
      val launchRes = launchExternalOAuth(context, oauthSpec)
      return launchRes.map {
        _pendingAuth.value ?: PendingPlatformAuth(
          providerId = meta.providerId,
          label = meta.label,
          isOAuthFlow = true,
          portalUrl = meta.portalUrl,
          handleHint = meta.handleHint,
          startedAtMillis = System.currentTimeMillis(),
        )
      }
    }

    val pending = PendingPlatformAuth(
      providerId = meta.providerId,
      label = meta.label,
      isOAuthFlow = false,
      portalUrl = meta.portalUrl,
      handleHint = meta.handleHint,
      startedAtMillis = System.currentTimeMillis(),
    )
    savePendingAuth(context, pending)
    val openRes = openPortalUrl(context, meta.portalUrl)
    if (openRes.isFailure) {
      clearPendingAuth(context)
      return Result.failure(openRes.exceptionOrNull() ?: IllegalStateException("Unable to open ${meta.label} sign-in"))
    }
    return Result.success(pending)
  }

  fun launchExternalOAuth(
    context: Context,
    spec: ExternalOAuthProviderSpec,
  ): Result<Unit> {
    if (!spec.isConfigured(context)) {
      return beginPortalSignIn(context, spec.id).map { }
    }
    val state = generateRandomUrlSafeToken(24)
    val verifier = generateRandomUrlSafeToken(48)
    val challenge = computePkceS256Challenge(verifier)
    val pending = PendingPlatformAuth(
      providerId = spec.id,
      label = spec.label,
      isOAuthFlow = true,
      portalUrl = spec.portalUrl,
      handleHint = spec.handleHint.ifBlank { "${spec.label} handle" },
      startedAtMillis = System.currentTimeMillis(),
    )
    savePendingAuth(context, pending, state = state, verifier = verifier)

    val uri = buildAuthorizationUri(spec, state, challenge, context)
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
      addCategory(Intent.CATEGORY_BROWSABLE)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
      context.startActivity(intent)
      Result.success(Unit)
    } catch (_: ActivityNotFoundException) {
      clearPendingAuth(context)
      Result.failure(IllegalStateException("No web browser installed on this device to open ${spec.label} sign-in."))
    } catch (e: Exception) {
      clearPendingAuth(context)
      Result.failure(e)
    }
  }

  fun buildAuthorizationUri(
    spec: ExternalOAuthProviderSpec,
    state: String,
    codeChallenge: String,
    context: Context? = null,
  ): Uri {
    val configVal = spec.configuredValue(context)
    if (spec.protocol == OAuthProtocol.STEAM_OPENID_2) {
      val parsedReturn = configVal.toUri()
      val realm = "${parsedReturn.scheme}://${parsedReturn.authority}"
      return spec.authEndpoint.toUri().buildUpon()
        .appendQueryParameter("openid.ns", "http://specs.openid.net/auth/2.0")
        .appendQueryParameter("openid.mode", "checkid_setup")
        .appendQueryParameter("openid.return_to", configVal)
        .appendQueryParameter("openid.realm", realm)
        .appendQueryParameter("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select")
        .appendQueryParameter("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select")
        .build()
    }

    val builder = spec.authEndpoint.toUri().buildUpon()
      .appendQueryParameter("response_type", "code")
      .appendQueryParameter("client_id", configVal)
      .appendQueryParameter("redirect_uri", spec.redirectUri())
      .appendQueryParameter("state", state)
      .appendQueryParameter("code_challenge", codeChallenge)
      .appendQueryParameter("code_challenge_method", "S256")
    if (spec.scope.isNotBlank()) {
      builder.appendQueryParameter("scope", spec.scope)
    }
    return builder.build()
  }

  suspend fun handleRedirectUri(context: Context, uri: Uri): OAuthCallbackResult? {
    if (uri.scheme != "unknownuniverses" || uri.host != "auth") return null
    val providerId = uri.pathSegments.firstOrNull()
      ?: run {
        clearPendingAuth(context)
        return OAuthCallbackResult.Failed("Missing provider in OAuth callback")
      }
    val spec = externalOAuthProviders.firstOrNull { it.id == providerId }
      ?: run {
        clearPendingAuth(context)
        return OAuthCallbackResult.Failed("Unknown provider: $providerId")
      }

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val expectedState = prefs.getString(KEY_STATE, null)
    val verifier = prefs.getString(KEY_VERIFIER, null)
    val expectedProvider = prefs.getString(KEY_PROVIDER, null)

    if (spec.protocol == OAuthProtocol.STEAM_OPENID_2) {
      clearPendingAuth(context)
      return verifySteamOpenIdCallback(spec, uri)
    }

    val error = uri.getQueryParameter("error")
    if (!error.isNullOrBlank()) {
      clearPendingAuth(context)
      val desc = uri.getQueryParameter("error_description") ?: error
      return OAuthCallbackResult.Failed("${spec.label} sign-in cancelled or denied: $desc")
    }

    val returnedState = uri.getQueryParameter("state")
    if (expectedProvider != providerId || expectedState.isNullOrBlank() || returnedState != expectedState || verifier.isNullOrBlank()) {
      clearPendingAuth(context)
      return OAuthCallbackResult.Failed("OAuth state verification failed for ${spec.label}.")
    }

    val code = uri.getQueryParameter("code")
      ?: run {
        clearPendingAuth(context)
        return OAuthCallbackResult.Failed("Missing authorization code from ${spec.label}.")
      }

    clearPendingAuth(context)
    return exchangePkceCode(context, spec, code, verifier)
  }

  suspend fun resolveSteamProfile(input: String): Result<SteamResolvedProfile> = withContext(Dispatchers.IO) {
    val xmlUrl = PlatformProfileParsers.buildSteamXmlUrl(input)
      ?: return@withContext Result.failure(
        IllegalArgumentException("Enter a Steam Vanity ID, 17-digit SteamID64, or steamcommunity.com URL.")
      )
    try {
      val conn = (URL(xmlUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 10_000
        setRequestProperty("Accept", "application/xml, text/xml")
      }
      val code = conn.responseCode
      val xml = (if (code in 200..299) conn.inputStream else conn.errorStream)
        ?.bufferedReader()?.use { it.readText() }.orEmpty()
      if (code !in 200..299 || xml.contains("<error>")) {
        return@withContext Result.failure(
          IllegalStateException("Steam profile not found or unreachable (HTTP $code).")
        )
      }
      val steamId64 = PlatformProfileParsers.extractXmlTag(xml, "steamID64")
        ?.takeIf { it.all(Char::isDigit) && it.length >= 15 }
        ?: return@withContext Result.failure(IllegalStateException("Steam profile did not return a valid SteamID64."))
      val persona = PlatformProfileParsers.extractXmlTag(xml, "steamID")
        ?.ifBlank { "Steam #$steamId64" }
        ?: "Steam #$steamId64"
      Result.success(SteamResolvedProfile(steamId64 = steamId64, personaName = persona.take(64)))
    } catch (e: Exception) {
      Result.failure(IllegalStateException("Unable to verify Steam profile: ${e.localizedMessage}"))
    }
  }

  fun parseEpicExternalAuthsJson(accountsJsonArray: String): List<BridgedExternalAccount> =
    PlatformProfileParsers.parseEpicExternalAuthsJson(accountsJsonArray)

  private suspend fun verifySteamOpenIdCallback(
    spec: ExternalOAuthProviderSpec,
    uri: Uri,
  ): OAuthCallbackResult = withContext(Dispatchers.IO) {
    val mode = uri.getQueryParameter("openid.mode")
    if (mode != "id_res") {
      return@withContext OAuthCallbackResult.Failed("Steam login was cancelled or invalid.")
    }
    val claimedId = uri.getQueryParameter("openid.claimed_id").orEmpty()
    val steamId = claimedId.substringAfterLast("/").takeIf { it.all(Char::isDigit) && it.length >= 15 }
      ?: return@withContext OAuthCallbackResult.Failed("Invalid SteamID returned.")

    val formParams = linkedMapOf<String, String>()
    uri.queryParameterNames.forEach { key ->
      val v = uri.getQueryParameter(key)
      if (key.startsWith("openid.") && v != null) {
        formParams[key] = if (key == "openid.mode") "check_authentication" else v
      }
    }

    try {
      val body = postForm(spec.tokenEndpoint, formParams)
      if (!body.contains("is_valid:true")) {
        return@withContext OAuthCallbackResult.Failed("Steam OpenID signature verification failed.")
      }
      val resolvedName = resolveSteamProfile(steamId).getOrNull()?.personaName ?: "Steam #$steamId"
      OAuthCallbackResult.Linked(
        providerId = spec.id,
        displayName = resolvedName,
        externalSubject = steamId,
      )
    } catch (e: Exception) {
      OAuthCallbackResult.Failed("Steam verification error: ${e.localizedMessage}")
    }
  }

  private suspend fun exchangePkceCode(
    context: Context,
    spec: ExternalOAuthProviderSpec,
    code: String,
    verifier: String,
  ): OAuthCallbackResult = withContext(Dispatchers.IO) {
    try {
      val responseText = postForm(
        endpoint = spec.tokenEndpoint,
        params = mapOf(
          "grant_type" to "authorization_code",
          "code" to code,
          "client_id" to spec.configuredValue(context),
          "redirect_uri" to spec.redirectUri(),
          "code_verifier" to verifier,
        ),
      )
      val json = JSONObject(responseText)
      val accessToken = json.optString("access_token")
      val idClaims = PlatformProfileParsers.decodeJwtClaims(json.optString("id_token"))

      var subject = json.optString("account_id")
        .ifBlank { json.optString("user_id") }
        .ifBlank { json.optString("sub") }
        .ifBlank { idClaims?.optString("sub").orEmpty() }
        .ifBlank { idClaims?.optString("pid").orEmpty() }
      var display = json.optString("displayName")
        .ifBlank { json.optString("username") }
        .ifBlank { idClaims?.optString("preferred_username").orEmpty() }
        .ifBlank { idClaims?.optString("nickname").orEmpty() }
        .ifBlank { idClaims?.optString("persona").orEmpty() }
        .ifBlank { idClaims?.optString("name").orEmpty() }
        .ifBlank { idClaims?.optString("email").orEmpty().substringBefore("@") }

      var bridged: List<BridgedExternalAccount> = emptyList()

      if (accessToken.isNotBlank()) {
        when (spec.id) {
          "epic" -> if (subject.isNotBlank()) {
            val rawEpic = getJsonWithBearer(
              "https://api.epicgames.dev/epic/id/v2/accounts?accountId=${URLEncoder.encode(subject, "UTF-8")}",
              accessToken,
            )
            if (rawEpic != null) {
              if (display.isBlank()) {
                display = PlatformProfileParsers.parseEpicAccountDisplayNameJson(rawEpic).orEmpty()
              }
              bridged = PlatformProfileParsers.parseEpicExternalAuthsJson(rawEpic)
            }
          }
          "amazon" -> {
            val rawAmazon = getJsonWithBearer("https://api.amazon.com/user/profile", accessToken)
            val parsed = rawAmazon?.let { PlatformProfileParsers.parseAmazonProfileJson(it) }
            if (parsed != null) {
              if (display.isBlank()) display = parsed.displayName
              if (subject.isBlank()) subject = parsed.subject
            }
          }
          "gog" -> {
            val rawGog = getJsonWithBearer("https://embed.gog.com/userData.json", accessToken)
            val parsed = rawGog?.let { PlatformProfileParsers.parseGogUserDataJson(it) }
            if (parsed != null) {
              if (display.isBlank()) display = parsed.displayName
              if (subject.isBlank()) subject = parsed.subject
            }
          }
          "ea_origin" -> if (display.isBlank() || subject.isBlank()) {
            val rawEa = getJsonWithBearer("https://gateway.ea.com/proxy/identity/pids/me", accessToken)
            val parsed = rawEa?.let { PlatformProfileParsers.parseEaPidMeJson(it) }
            if (parsed != null) {
              if (display.isBlank()) display = parsed.displayName
              if (subject.isBlank()) subject = parsed.subject
            }
          }
        }
      }

      val resolvedSubject = subject.ifBlank { accessToken.take(16).let { "token_$it" } }
      val resolvedDisplay = display.ifBlank { "${spec.label} ($resolvedSubject)" }

      OAuthCallbackResult.Linked(
        providerId = spec.id,
        displayName = resolvedDisplay.take(64),
        externalSubject = resolvedSubject.take(128),
        bridgedAccounts = bridged,
      )
    } catch (e: Exception) {
      OAuthCallbackResult.Failed("${spec.label} token exchange failed: ${e.localizedMessage}")
    }
  }

  private fun getJsonWithBearer(urlString: String, accessToken: String): String? {
    return try {
      val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8_000
        readTimeout = 8_000
        setRequestProperty("Authorization", "Bearer $accessToken")
        setRequestProperty("Accept", "application/json")
      }
      if (conn.responseCode in 200..299) {
        conn.inputStream.bufferedReader().use { it.readText() }
      } else {
        null
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun postForm(endpoint: String, params: Map<String, String>): String {
    val encoded = params.entries.joinToString("&") { (k, v) ->
      "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
    }
    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      doOutput = true
      connectTimeout = 10_000
      readTimeout = 10_000
      setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
      setRequestProperty("Accept", "application/json, text/plain")
    }
    conn.outputStream.use { it.write(encoded.toByteArray(Charsets.UTF_8)) }
    val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
    val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    if (conn.responseCode !in 200..299) {
      throw IllegalStateException("HTTP ${conn.responseCode}: ${text.take(140)}")
    }
    return text
  }

  fun generateRandomUrlSafeToken(byteLen: Int): String {
    val bytes = ByteArray(byteLen)
    SecureRandom().nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
  }

  fun computePkceS256Challenge(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
  }
}

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
import kotlinx.coroutines.withContext
import org.json.JSONArray
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

data class BridgedExternalAccount(
  val providerId: String,
  val displayName: String,
  val externalSubject: String,
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

data class SteamResolvedProfile(
  val steamId64: String,
  val personaName: String,
)

object PlatformOAuthManager {
  private const val PREFS_NAME = "unknown_os_oauth_pending"
  private const val CLIENT_OVERRIDES_PREFS = "unknown_os_oauth_clients"
  private const val KEY_STATE = "pending_state"
  private const val KEY_VERIFIER = "pending_verifier"
  private const val KEY_PROVIDER = "pending_provider"
  private const val EPIC_CONNECTIONS_URL = "https://www.epicgames.com/account/connections"

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
      accentHex = 0xFF5C6BC0,
      configValueProvider = { BuildConfig.SAMSUNG_CLIENT_ID },
    ),
  )

  /**
   * Console & publisher accounts linkable both via Epic Connections AND directly via their official web portals.
   */
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

  fun launchExternalOAuth(
    context: Context,
    spec: ExternalOAuthProviderSpec,
  ): Result<Unit> {
    if (!spec.isConfigured(context)) {
      return openPortalUrl(context, spec.portalUrl)
    }
    val state = generateRandomUrlSafeToken(24)
    val verifier = generateRandomUrlSafeToken(48)
    val challenge = computePkceS256Challenge(verifier)

    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
      putString(KEY_STATE, state)
      putString(KEY_VERIFIER, verifier)
      putString(KEY_PROVIDER, spec.id)
    }

    val uri = buildAuthorizationUri(spec, state, challenge, context)
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
      addCategory(Intent.CATEGORY_BROWSABLE)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
      context.startActivity(intent)
      Result.success(Unit)
    } catch (_: ActivityNotFoundException) {
      Result.failure(IllegalStateException("No web browser installed on this device to open ${spec.label} sign-in."))
    } catch (e: Exception) {
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
      ?: return OAuthCallbackResult.Failed("Missing provider in OAuth callback")
    val spec = externalOAuthProviders.firstOrNull { it.id == providerId }
      ?: return OAuthCallbackResult.Failed("Unknown provider: $providerId")

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val expectedState = prefs.getString(KEY_STATE, null)
    val verifier = prefs.getString(KEY_VERIFIER, null)
    val expectedProvider = prefs.getString(KEY_PROVIDER, null)

    if (spec.protocol == OAuthProtocol.STEAM_OPENID_2) {
      prefs.edit { clear() }
      return verifySteamOpenIdCallback(spec, uri)
    }

    val error = uri.getQueryParameter("error")
    if (!error.isNullOrBlank()) {
      prefs.edit { clear() }
      val desc = uri.getQueryParameter("error_description") ?: error
      return OAuthCallbackResult.Failed("${spec.label} sign-in cancelled or denied: $desc")
    }

    val returnedState = uri.getQueryParameter("state")
    if (expectedProvider != providerId || expectedState.isNullOrBlank() || returnedState != expectedState || verifier.isNullOrBlank()) {
      prefs.edit { clear() }
      return OAuthCallbackResult.Failed("OAuth state verification failed for ${spec.label}.")
    }

    val code = uri.getQueryParameter("code")
      ?: run {
        prefs.edit { clear() }
        return OAuthCallbackResult.Failed("Missing authorization code from ${spec.label}.")
      }

    prefs.edit { clear() }
    return exchangePkceCode(context, spec, code, verifier)
  }

  /**
   * Resolves a real Steam account directly from Steam Community XML (`?xml=1`) using a Steam URL,
   * Steam Vanity ID, or 17-digit SteamID64, requiring no Steam Web API key.
   */
  suspend fun resolveSteamProfile(input: String): Result<SteamResolvedProfile> = withContext(Dispatchers.IO) {
    val xmlUrl = buildSteamXmlUrl(input)
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
      val steamId64 = extractXmlTag(xml, "steamID64")
        ?.takeIf { it.all(Char::isDigit) && it.length >= 15 }
        ?: return@withContext Result.failure(IllegalStateException("Steam profile did not return a valid SteamID64."))
      val persona = extractXmlTag(xml, "steamID")
        ?.ifBlank { "Steam #$steamId64" }
        ?: "Steam #$steamId64"
      Result.success(SteamResolvedProfile(steamId64 = steamId64, personaName = persona.take(64)))
    } catch (e: Exception) {
      Result.failure(IllegalStateException("Unable to verify Steam profile: ${e.localizedMessage}"))
    }
  }

  internal fun buildSteamXmlUrl(rawInput: String): String? {
    val trimmed = rawInput.trim().trimEnd('/')
    if (trimmed.isEmpty()) return null
    val cleanPath = trimmed
      .removePrefix("https://")
      .removePrefix("http://")
    return when {
      cleanPath.startsWith("steamcommunity.com/profiles/") -> {
        val id = cleanPath.removePrefix("steamcommunity.com/profiles/").substringBefore("/").substringBefore("?")
        if (id.isNotBlank()) "https://steamcommunity.com/profiles/$id/?xml=1" else null
      }
      cleanPath.startsWith("steamcommunity.com/id/") -> {
        val vanity = cleanPath.removePrefix("steamcommunity.com/id/").substringBefore("/").substringBefore("?")
        if (vanity.isNotBlank()) "https://steamcommunity.com/id/${URLEncoder.encode(vanity, "UTF-8")}/?xml=1" else null
      }
      trimmed.all(Char::isDigit) && trimmed.length >= 15 ->
        "https://steamcommunity.com/profiles/$trimmed/?xml=1"
      trimmed.matches(Regex("^[a-zA-Z0-9_\\-\\.]{2,64}$")) ->
        "https://steamcommunity.com/id/${URLEncoder.encode(trimmed, "UTF-8")}/?xml=1"
      else -> null
    }
  }

  internal fun extractXmlTag(xml: String, tag: String): String? {
    val regex = Regex("<$tag>(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?<\\/$tag>", RegexOption.DOT_MATCHES_ALL)
    return regex.find(xml)?.groupValues?.getOrNull(1)?.trim()
  }

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
      val subject =
        json.optString("account_id")
          .ifBlank { json.optString("user_id") }
          .ifBlank { json.optString("sub") }
          .ifBlank { accessToken.take(16).let { "token_$it" } }
      val display =
        json.optString("displayName")
          .ifBlank { json.optString("username") }
          .ifBlank { "${spec.label} ($subject)" }

      val bridged =
        if (spec.id == "epic" && accessToken.isNotBlank() && subject.isNotBlank()) {
          fetchEpicExternalAuths(accessToken, subject)
        } else {
          emptyList()
        }

      OAuthCallbackResult.Linked(
        providerId = spec.id,
        displayName = display.take(64),
        externalSubject = subject.take(128),
        bridgedAccounts = bridged,
      )
    } catch (e: Exception) {
      OAuthCallbackResult.Failed("${spec.label} token exchange failed: ${e.localizedMessage}")
    }
  }

  fun parseEpicExternalAuthsJson(accountsJsonArray: String): List<BridgedExternalAccount> {
    val results = mutableListOf<BridgedExternalAccount>()
    val arr = JSONArray(accountsJsonArray)
    if (arr.length() == 0) return results
    val accountObj = arr.optJSONObject(0) ?: return results
    val externalAuths = accountObj.optJSONObject("externalAuths") ?: return results

    val keyMapping = mapOf(
      "psn" to "psn",
      "nintendo" to "nintendo",
      "ubisoft" to "ubisoft",
      "xbl" to "xbox",
      "steam" to "steam",
      "apple" to "apple",
    )

    keyMapping.forEach { (epicKey, mappedProviderId) ->
      val extObj = externalAuths.optJSONObject(epicKey)
      if (extObj != null) {
        val extId = extObj.optString("externalAuthId").ifBlank { extObj.optString("id") }
        val extDisplay = extObj.optString("externalDisplayName").ifBlank { extId }
        if (extId.isNotBlank() || extDisplay.isNotBlank()) {
          results.add(
            BridgedExternalAccount(
              providerId = mappedProviderId,
              displayName = "$extDisplay (via Epic)".take(64),
              externalSubject = extId.ifBlank { extDisplay }.take(128),
            )
          )
        }
      }
    }
    return results
  }

  private fun fetchEpicExternalAuths(accessToken: String, accountId: String): List<BridgedExternalAccount> {
    return try {
      val url = URL("https://api.epicgames.dev/epic/id/v2/accounts?accountId=${URLEncoder.encode(accountId, "UTF-8")}")
      val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8_000
        readTimeout = 8_000
        setRequestProperty("Authorization", "Bearer $accessToken")
        setRequestProperty("Accept", "application/json")
      }
      if (conn.responseCode in 200..299) {
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        parseEpicExternalAuthsJson(text)
      } else {
        emptyList()
      }
    } catch (_: Exception) {
      emptyList()
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

package com.example

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.example.ui.auth.ExternalOAuthProviderSpec
import com.example.ui.auth.OAuthProtocol
import com.example.ui.auth.PlatformOAuthManager
import com.example.ui.auth.PlatformProfileParsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlatformOAuthTest {

  @Test
  fun pkceChallenge_isUrlSafeWithoutPadding() {
    val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
    val challenge = PlatformOAuthManager.computePkceS256Challenge(verifier)
    // RFC 7636 Appendix B known test vector
    assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", challenge)
    assertFalse(challenge.contains("="))
    assertFalse(challenge.contains("+"))
    assertFalse(challenge.contains("/"))
  }

  @Test
  fun buildAuthorizationUri_generatesValidOAuth2AndSteamUrls() {
    val epicSpec = ExternalOAuthProviderSpec(
      id = "epic",
      label = "Epic Games",
      platformGroup = "PC",
      protocol = OAuthProtocol.OAUTH2_PKCE,
      authEndpoint = "https://www.epicgames.com/id/authorize",
      tokenEndpoint = "https://api.epicgames.dev/epic/oauth/v2/token",
      scope = "basic_profile",
      secretKeyName = "EPIC_CLIENT_ID",
      accentHex = 0xFFFFFFFF,
      configValueProvider = { "client_12345" },
    )
    val uri = PlatformOAuthManager.buildAuthorizationUri(epicSpec, "state_abc", "chal_xyz")
    assertEquals("code", uri.getQueryParameter("response_type"))
    assertEquals("client_12345", uri.getQueryParameter("client_id"))
    assertEquals("unknownuniverses://auth/epic", uri.getQueryParameter("redirect_uri"))
    assertEquals("S256", uri.getQueryParameter("code_challenge_method"))
    assertEquals("chal_xyz", uri.getQueryParameter("code_challenge"))

    val steamSpec = ExternalOAuthProviderSpec(
      id = "steam",
      label = "Steam",
      platformGroup = "PC",
      protocol = OAuthProtocol.STEAM_OPENID_2,
      authEndpoint = "https://steamcommunity.com/openid/login",
      tokenEndpoint = "https://steamcommunity.com/openid/login",
      scope = "",
      secretKeyName = "STEAM_OPENID_RETURN_URL",
      accentHex = 0xFFFFFFFF,
      configValueProvider = { "https://example.com/steam-callback" },
    )
    val steamUri = PlatformOAuthManager.buildAuthorizationUri(steamSpec, "s1", "c1")
    assertEquals("checkid_setup", steamUri.getQueryParameter("openid.mode"))
    assertEquals("https://example.com/steam-callback", steamUri.getQueryParameter("openid.return_to"))
    assertEquals("https://example.com", steamUri.getQueryParameter("openid.realm"))
  }

  @Test
  fun epicExternalAuthsParser_extractsPsnNintendoUbisoftAndXbox() {
    val sampleJson = """
      [
        {
          "id": "epic_acc_001",
          "displayName": "BlakePrime",
          "externalAuths": {
            "psn": { "externalAuthId": "psn_123", "externalDisplayName": "BlakePS5" },
            "nintendo": { "externalAuthId": "nsw_456", "externalDisplayName": "BlakeSwitch" },
            "ubisoft": { "externalAuthId": "ubi_789", "externalDisplayName": "BlakeUbi" },
            "xbl": { "externalAuthId": "xbl_321", "externalDisplayName": "BlakeXbox" }
          }
        }
      ]
    """.trimIndent()
    val parsed = PlatformOAuthManager.parseEpicExternalAuthsJson(sampleJson)
    assertEquals(4, parsed.size)
    assertTrue(parsed.any { it.providerId == "psn" && it.displayName.contains("BlakePS5") })
    assertTrue(parsed.any { it.providerId == "nintendo" && it.displayName.contains("BlakeSwitch") })
    assertTrue(parsed.any { it.providerId == "ubisoft" && it.displayName.contains("BlakeUbi") })
    assertTrue(parsed.any { it.providerId == "xbox" && it.displayName.contains("BlakeXbox") })
    assertEquals("BlakePrime", PlatformProfileParsers.parseEpicAccountDisplayNameJson(sampleJson))
  }

  @Test
  fun unifiedPlatforms_coversAll11ProvidersAndResolvesMetadata() {
    val expected = setOf(
      "apple", "xbox", "epic", "steam", "gog", "ea_origin",
      "amazon", "galaxy_store", "psn", "nintendo", "ubisoft",
    )
    val actual = PlatformOAuthManager.allUnifiedPlatforms().map { it.providerId }.toSet()
    assertEquals(expected, actual)
    expected.forEach { id ->
      val meta = PlatformOAuthManager.findPlatformMeta(id)
      assertNotNull("Expected metadata for $id", meta)
      assertTrue(meta!!.portalUrl.startsWith("https://"))
    }
  }

  @Test
  fun profileParsers_decodeJwtAndAmazonGogEaProfiles() {
    val claimsPayload = Base64.encodeToString(
      """{"sub":"sam_99","preferred_username":"GalaxyBlake"}""".toByteArray(),
      Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
    val claims = PlatformProfileParsers.decodeJwtClaims("hdr.$claimsPayload.sig")
    assertEquals("sam_99", claims?.optString("sub"))
    assertEquals("GalaxyBlake", claims?.optString("preferred_username"))

    val amazon = PlatformProfileParsers.parseAmazonProfileJson(
      """{"user_id":"amzn1.account.ABC","name":"BlakeAmazon","email":"b@example.com"}""",
    )
    assertEquals("BlakeAmazon", amazon?.displayName)
    assertEquals("amzn1.account.ABC", amazon?.subject)

    val gog = PlatformProfileParsers.parseGogUserDataJson(
      """{"isLoggedIn":true,"userId":"445566","username":"BlakeGOG"}""",
    )
    assertEquals("BlakeGOG", gog?.displayName)
    assertEquals("445566", gog?.subject)

    val ea = PlatformProfileParsers.parseEaPidMeJson(
      """{"pid":{"pidId":"100200300","displayName":"BlakeEA"}}""",
    )
    assertEquals("BlakeEA", ea?.displayName)
    assertEquals("100200300", ea?.subject)
  }

  @Test
  fun runtimeClientIdOverride_persistsAndEnablesOAuthConfig() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val placeholderEpicSpec = ExternalOAuthProviderSpec(
      id = "epic",
      label = "Epic Games",
      platformGroup = "PC",
      protocol = OAuthProtocol.OAUTH2_PKCE,
      authEndpoint = "https://www.epicgames.com/id/authorize",
      tokenEndpoint = "https://api.epicgames.dev/epic/oauth/v2/token",
      scope = "basic_profile",
      secretKeyName = "EPIC_CLIENT_ID",
      accentHex = 0xFFFFFFFF,
      configValueProvider = { "Neargooglelogin" },
    )
    PlatformOAuthManager.saveRuntimeClientId(context, "epic", "")
    assertFalse(placeholderEpicSpec.isConfigured(context))

    val invalidSteamSpec = ExternalOAuthProviderSpec(
      id = "steam",
      label = "Steam",
      platformGroup = "PC",
      protocol = OAuthProtocol.STEAM_OPENID_2,
      authEndpoint = "https://steamcommunity.com/openid/login",
      tokenEndpoint = "https://steamcommunity.com/openid/login",
      scope = "",
      secretKeyName = "STEAM_OPENID_RETURN_URL",
      accentHex = 0xFFFFFFFF,
      configValueProvider = { "Neargooglelogin" },
    )
    assertFalse(invalidSteamSpec.isConfigured(context))

    PlatformOAuthManager.saveRuntimeClientId(context, "epic", "epic_custom_client_xyz")
    assertTrue(placeholderEpicSpec.isConfigured(context))
    assertEquals("epic_custom_client_xyz", placeholderEpicSpec.configuredValue(context))

    PlatformOAuthManager.saveRuntimeClientId(context, "epic", "")
    assertFalse(placeholderEpicSpec.isConfigured(context))
    PlatformOAuthManager.clearPendingAuth(context)
    assertNull(PlatformOAuthManager.restorePendingAuth(context))
  }

  @Test
  fun epicBridgedPlatforms_coverPsnNintendoAndUbisoft() {
    val ids = PlatformOAuthManager.epicBridgedPlatforms.map { it.providerId }
    assertTrue(ids.contains("psn"))
    assertTrue(ids.contains("nintendo"))
    assertTrue(ids.contains("ubisoft"))
  }
}

package com.example

import android.content.Context
import android.util.Base64
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.core.app.ApplicationProvider
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.GoogleSignInHelper
import com.example.ui.theme.UnknownUniversesTheme
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class ExampleRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `read app name and package name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("The Unknown Universes", appName)
    assertEquals("com.unknownuniverses.os", BuildConfig.APPLICATION_ID)
    assertEquals("com.unknownuniverses.os", context.packageName)
  }

  @Test
  fun `auth screen renders Google Sign-In and Boot UnknownOS controls`() {
    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        AuthScreen(onBootLocalOperator = {})
      }
    }
    composeRule.onNodeWithText("App Package ID: com.unknownuniverses.os").assertIsDisplayed()
    composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
    composeRule.onNodeWithTag("boot_unknownos_button").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `auth screen shows error banner next to buttons when callsign is too short`() {
    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        AuthScreen(onBootLocalOperator = {})
      }
    }
    composeRule.onNodeWithTag("operator_callsign_input").performScrollTo().performTextReplacement("X")
    composeRule.onNodeWithTag("boot_unknownos_button").performScrollTo().performClick()
    composeRule.onNodeWithTag("auth_error_banner").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `google sign in error formatter gives actionable guidance for common failures`() {
    val noCred = GoogleSignInHelper.formatAuthError(NoCredentialException("No credential"))
    assertTrue(noCred.contains("Boot UnknownOS"))

    val providerErr = GoogleSignInHelper.formatAuthError(GetCredentialProviderConfigurationException("Provider missing"))
    assertTrue(providerErr.contains("Play Services"))

    val unsupported = GoogleSignInHelper.formatAuthError(GetCredentialUnsupportedException("Unsupported"))
    assertTrue(unsupported.contains("Boot UnknownOS"))

    val shaMismatch = GoogleSignInHelper.formatAuthError(RuntimeException("[28444] Developer console is not set up correctly."))
    assertTrue(shaMismatch.contains("SHA-1"))

    val apiDeveloperError = GoogleSignInHelper.formatAuthError(ApiException(Status(10, "DEVELOPER_ERROR")))
    assertTrue(apiDeveloperError.contains("10/28444"))

    val apiNetworkError = GoogleSignInHelper.formatAuthError(ApiException(Status(7, "NETWORK_ERROR")))
    assertTrue(apiNetworkError.contains("code 7"))
  }

  @Test
  fun `google session extractor decodes JWT sub name and email cleanly`() {
    val payload = Base64.encodeToString(
      """{"sub":"99887766","email":"superblake@example.com","name":"Super Blake"}""".toByteArray(),
      Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
    val fakeJwt = "eyJhbGciOiJSUzI1NiJ9.$payload.sig"
    val session = GoogleSignInHelper.extractGoogleSessionFromIdToken(
      idToken = fakeJwt,
      fallbackId = "superblake@example.com",
      fallbackDisplayName = null,
    )
    assertEquals("google_99887766", session.uid)
    assertEquals("Super Blake", session.displayName)
    assertEquals("superblake@example.com", session.email)
  }

  @Test
  fun `cloud sync and save transfer card renders sync and export import actions`() {
    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        com.example.ui.screens.CloudSyncAndSaveCard(
          callsign = "SuperBlake",
          cloudSyncState = com.example.data.local.CloudSyncState.Synced(1234L),
          isCloudSignedIn = true,
          onSyncNow = {},
          onRequestSaveExportJson = {},
          onParseSaveJson = { Result.failure(IllegalStateException("none")) },
          onConfirmSaveImport = { _, _ -> },
          onStatusMessage = {},
        )
      }
    }
    composeRule.onNodeWithText("CLOUD SYNCED").assertIsDisplayed()
    composeRule.onNodeWithTag("cloud_sync_now_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("export_save_file_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("share_save_text_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("import_save_file_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("paste_save_json_btn").assertIsDisplayed()
  }

  @Test
  fun `cross platform accounts card detects signed-in apple xbox and steam accounts`() {
    val links = listOf(
      com.example.data.model.LinkedAccount(
        id = "u1_apple",
        userId = "u1",
        providerId = "apple",
        displayName = "BlakeApple",
        externalSubject = "apple:blakeapple",
      ),
      com.example.data.model.LinkedAccount(
        id = "u1_xbox",
        userId = "u1",
        providerId = "xbox",
        displayName = "BlakeXbox",
        externalSubject = "xbox:blakexbox",
      ),
      com.example.data.model.LinkedAccount(
        id = "u1_psn",
        userId = "u1",
        providerId = "psn",
        displayName = "BlakePSN",
        externalSubject = "psn:blakepsn",
      ),
    )
    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        com.example.ui.auth.CrossPlatformAccountsCard(
          linkedAccounts = links,
          onUnlinkExternal = {},
          onStatusMessage = {},
        )
      }
    }
    composeRule.onNodeWithText("Signed in: BlakeApple").assertIsDisplayed()
    composeRule.onNodeWithText("Signed in: BlakeXbox").assertIsDisplayed()
    composeRule.onNodeWithText("Signed in: BlakePSN").assertIsDisplayed()
    composeRule.onNodeWithTag("sign_out_apple").assertIsDisplayed()
    composeRule.onNodeWithTag("sign_out_xbox").assertIsDisplayed()
    composeRule.onNodeWithTag("sign_out_psn").assertIsDisplayed()
  }

  @Test
  fun `auth screen detects linked platform sessions and expands other logins`() {
    val links = listOf(
      com.example.data.model.LinkedAccount(
        id = "local_steam",
        userId = "local_operator",
        providerId = "steam",
        displayName = "SteamBlake",
        externalSubject = "76561198000000000",
      ),
    )
    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        AuthScreen(
          linkedAccounts = links,
          onBootLocalOperator = {},
        )
      }
    }
    composeRule.onNodeWithTag("auth_other_logins_card").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("1 platform login(s) detected on device").assertIsDisplayed()
    composeRule.onNodeWithText("Steam: SteamBlake").assertIsDisplayed()
    composeRule.onNodeWithTag("toggle_other_logins_btn").performClick()
    composeRule.onNodeWithTag("auth_sign_out_steam").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("auth_login_xbox").performScrollTo().assertIsDisplayed()
  }
}

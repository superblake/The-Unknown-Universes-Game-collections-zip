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
}

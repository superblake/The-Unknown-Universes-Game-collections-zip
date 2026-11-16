package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ui.auth.AuthScreen
import com.example.ui.theme.UnknownUniversesTheme
import org.junit.Assert.assertEquals
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
}

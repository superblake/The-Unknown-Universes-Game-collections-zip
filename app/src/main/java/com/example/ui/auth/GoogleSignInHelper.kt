package com.example.ui.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GoogleSignInHelper {

  private tailrec fun Context.findActivity(): Activity? =
    (this as? Activity) ?: (this as? ContextWrapper)?.baseContext?.findActivity()

  @SuppressLint("DiscouragedApi")
  private fun resolveDefaultWebClientId(context: Context): String? =
    try {
      val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
      if (resId != 0) context.getString(resId).takeIf { it.isNotBlank() } else null
    } catch (_: Exception) {
      null
    }

  private fun isFirebaseReady(context: Context): Boolean =
    runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (displayName: String?) -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope,
  ) {
    if (!isFirebaseReady(context)) {
      onUnauthenticated()
      return
    }
    val existing = runCatching { Firebase.auth.currentUser }.getOrNull()
    if (existing != null) {
      onAuthSuccess(existing.displayName)
      return
    }
    val clientId = resolveDefaultWebClientId(context) ?: run {
      onUnauthenticated()
      return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    val hostContext = context.findActivity() ?: context

    scope.launch {
      try {
        val result = credentialManager.getCredential(hostContext, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          val name = Firebase.auth.currentUser?.displayName
          onAuthSuccess(name)
        } else {
          onUnauthenticated()
        }
      } catch (e: NoCredentialException) {
        Log.d("Auth", "No saved Google credential for auto sign-in: ${e.message}")
        onUnauthenticated()
      } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "Silent Google Sign-In cancelled: ${e.message}", e)
        onUnauthenticated()
      } catch (e: Exception) {
        Log.w("Auth", "Silent Google Sign-In unavailable: ${e.message}")
        onUnauthenticated()
      }
    }
  }

  fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (displayName: String?) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
  ) {
    val clientId = resolveDefaultWebClientId(context) ?: run {
      onAuthError(
        "Google Sign-In configuration missing: rebuild with app/google-services.json, or tap Boot UnknownOS to play locally."
      )
      return
    }
    if (!isFirebaseReady(context)) {
      onAuthError(
        "Firebase Authentication is not initialized in this build. Rebuild with app/google-services.json, or tap Boot UnknownOS."
      )
      return
    }

    val hostContext = context.findActivity() ?: context
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val primaryRequest = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
      try {
        val result = requestInteractiveCredential(
          hostContext = hostContext,
          credentialManager = credentialManager,
          primaryRequest = primaryRequest,
          clientId = clientId,
        )
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          val user = Firebase.auth.currentUser
          val name = user?.displayName?.takeIf { it.isNotBlank() } ?: user?.email?.substringBefore("@")
          onAuthSuccess(name)
        } else {
          onAuthError("Unexpected credential type returned by Google Sign-In.")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
        if (isConfigurationMismatchMessage(e.message)) {
          onAuthError(formatAuthError(e))
        } else {
          onAuthCancelled()
        }
      } catch (e: Exception) {
        Log.e("Auth", "Google Sign-In failed", e)
        onAuthError(formatAuthError(e))
      }
    }
  }

  private suspend fun requestInteractiveCredential(
    hostContext: Context,
    credentialManager: CredentialManager,
    primaryRequest: GetCredentialRequest,
    clientId: String,
  ): GetCredentialResponse {
    return try {
      credentialManager.getCredential(hostContext, primaryRequest)
    } catch (primaryError: GetCredentialException) {
      val shouldRetryWithGoogleIdOption =
        primaryError is NoCredentialException ||
          primaryError is GetCredentialProviderConfigurationException ||
          primaryError is GetCredentialUnsupportedException
      if (!shouldRetryWithGoogleIdOption) {
        throw primaryError
      }
      Log.w(
        "Auth",
        "Primary GetSignInWithGoogleOption failed (${primaryError::class.simpleName}: ${primaryError.message}); retrying with GetGoogleIdOption",
      )
      val fallbackOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(clientId)
        .build()
      val fallbackRequest = GetCredentialRequest.Builder()
        .addCredentialOption(fallbackOption)
        .build()
      credentialManager.getCredential(hostContext, fallbackRequest)
    }
  }

  private fun isConfigurationMismatchMessage(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    val lower = message.lowercase()
    return lower.contains("28444") ||
      lower.contains("developer console") ||
      lower.contains("developer_error") ||
      lower.contains("sha-1") ||
      lower.contains("caller") ||
      lower.contains("not configured")
  }

  @VisibleForTesting
  internal fun formatAuthError(throwable: Throwable): String {
    val msg = throwable.message.orEmpty()
    if (isConfigurationMismatchMessage(msg) || (throwable is ApiException && throwable.statusCode == 10)) {
      return "Google Sign-In configuration or app signature (SHA-1) mismatch. Install an APK signed with the project keystore, or tap Boot UnknownOS."
    }
    return when (throwable) {
      is NoCredentialException ->
        "No Google account found on this device or emulator. Add a Google account in Settings, or tap Boot UnknownOS below to play locally."
      is GetCredentialProviderConfigurationException ->
        "Google Play Services Credential Manager is unavailable or outdated on this device. Update Google Play Services or tap Boot UnknownOS."
      is GetCredentialUnsupportedException ->
        "Credential Manager Google Sign-In is unsupported on this device/emulator. Tap Boot UnknownOS below to play locally."
      is GetCredentialInterruptedException ->
        "Google Sign-In was interrupted. Please tap Sign in with Google again."
      is FirebaseNetworkException ->
        "Network error while signing in with Google. Check your internet connection and try again."
      is FirebaseAuthException ->
        "Firebase Auth error (${throwable.errorCode}): ${throwable.localizedMessage ?: "Sign in failed"}"
      else -> throwable.localizedMessage?.takeIf { it.isNotBlank() } ?: "Google Sign-In failed. Tap Boot UnknownOS to play locally."
    }
  }

  fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {},
  ) {
    scope.launch {
      withContext(NonCancellable) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w("Auth", "Failed to clear credential state: ${e.message}")
        } finally {
          runCatching { Firebase.auth.signOut() }
          onSignOutComplete()
        }
      }
    }
  }
}

package com.example.ui.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (displayName: String?) -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope,
  ) {
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

    scope.launch(Dispatchers.IO) {
      try {
        val result = credentialManager.getCredential(hostContext, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          val name = Firebase.auth.currentUser?.displayName
          withContext(Dispatchers.Main) { onAuthSuccess(name) }
        } else {
          withContext(Dispatchers.Main) { onUnauthenticated() }
        }
      } catch (e: NoCredentialException) {
        Log.d("Auth", "No saved Google credential for auto sign-in")
        withContext(Dispatchers.Main) { onUnauthenticated() }
      } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "Silent Google Sign-In cancelled: ${e.message}", e)
        withContext(Dispatchers.Main) { onUnauthenticated() }
      } catch (e: Exception) {
        Log.w("Auth", "Silent Google Sign-In unavailable: ${e.message}")
        withContext(Dispatchers.Main) { onUnauthenticated() }
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
      onAuthError("Google Sign-In configuration missing: add app/google-services.json to enable Google Sign-In, or tap Boot UnknownOS.")
      return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
    val hostContext = context.findActivity() ?: context

    scope.launch(Dispatchers.IO) {
      try {
        val result = credentialManager.getCredential(hostContext, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          val name = Firebase.auth.currentUser?.displayName
          withContext(Dispatchers.Main) { onAuthSuccess(name) }
        } else {
          withContext(Dispatchers.Main) { onAuthError("Unexpected credential type") }
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
        withContext(Dispatchers.Main) { onAuthCancelled() }
      } catch (e: NoCredentialException) {
        Log.w("Auth", "No Google account available on device: ${e.message}", e)
        withContext(Dispatchers.Main) {
          onAuthError("No Google account found on this device. Add a Google account in Settings or boot locally.")
        }
      } catch (e: Exception) {
        Log.e("Auth", "Google Sign-In failed", e)
        withContext(Dispatchers.Main) { onAuthError(e.localizedMessage ?: "Sign in failed") }
      }
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
          withContext(Dispatchers.Main) { onSignOutComplete() }
        }
      }
    }
  }
}

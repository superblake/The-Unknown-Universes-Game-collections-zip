package com.example.ui.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Base64
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class GoogleSessionUser(
  val uid: String,
  val displayName: String?,
  val email: String?,
)

object GoogleSignInHelper {

  // Public OAuth 2.0 Web Client ID for com.unknownuniverses.os (not a secret/API key).
  // Used automatically if a build environment (such as GitHub export) strips google-services.json.
  private const val FALLBACK_WEB_CLIENT_ID =
    "531913103030-5fglsponek4ll9urhcvkpnrq23finq4g.apps.googleusercontent.com"

  private const val GOOGLE_PREFS = "unknown_os_google_session"
  private const val KEY_UID = "google_uid"
  private const val KEY_NAME = "google_name"
  private const val KEY_EMAIL = "google_email"

  private val _sessionUser = MutableStateFlow<GoogleSessionUser?>(null)
  val sessionUser: StateFlow<GoogleSessionUser?> = _sessionUser.asStateFlow()

  private tailrec fun Context.findActivity(): Activity? =
    (this as? Activity) ?: (this as? ContextWrapper)?.baseContext?.findActivity()

  @SuppressLint("DiscouragedApi")
  private fun resolveDefaultWebClientId(context: Context): String {
    val fromResources = try {
      val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
      if (resId != 0) context.getString(resId).takeIf { it.isNotBlank() } else null
    } catch (_: Exception) {
      null
    }
    return fromResources ?: FALLBACK_WEB_CLIENT_ID
  }

  private fun isFirebaseReady(context: Context): Boolean =
    runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

  fun restoreSavedSession(context: Context): GoogleSessionUser? {
    val firebaseCurrent = if (isFirebaseReady(context)) {
      runCatching { Firebase.auth.currentUser }.getOrNull()
    } else {
      null
    }
    if (firebaseCurrent != null) {
      val user = GoogleSessionUser(
        uid = firebaseCurrent.uid,
        displayName = firebaseCurrent.displayName,
        email = firebaseCurrent.email,
      )
      _sessionUser.value = user
      return user
    }
    val prefs = context.getSharedPreferences(GOOGLE_PREFS, Context.MODE_PRIVATE)
    val uid = prefs.getString(KEY_UID, null)?.takeIf { it.isNotBlank() } ?: return null
    val saved = GoogleSessionUser(
      uid = uid,
      displayName = prefs.getString(KEY_NAME, null),
      email = prefs.getString(KEY_EMAIL, null),
    )
    _sessionUser.value = saved
    return saved
  }

  private fun persistSession(context: Context, user: GoogleSessionUser) {
    context.getSharedPreferences(GOOGLE_PREFS, Context.MODE_PRIVATE).edit {
      putString(KEY_UID, user.uid)
      if (user.displayName != null) putString(KEY_NAME, user.displayName) else remove(KEY_NAME)
      if (user.email != null) putString(KEY_EMAIL, user.email) else remove(KEY_EMAIL)
    }
    _sessionUser.value = user
  }

  private fun clearSavedSession(context: Context) {
    context.getSharedPreferences(GOOGLE_PREFS, Context.MODE_PRIVATE).edit { clear() }
    _sessionUser.value = null
  }

  @VisibleForTesting
  internal fun extractGoogleSessionFromIdToken(
    idToken: String,
    fallbackId: String,
    fallbackDisplayName: String?,
  ): GoogleSessionUser {
    val payloadJson = runCatching {
      val parts = idToken.split(".")
      if (parts.size >= 2) {
        val decoded = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        JSONObject(String(decoded, Charsets.UTF_8))
      } else {
        null
      }
    }.getOrNull()

    val sub = payloadJson?.optString("sub")?.takeIf { it.isNotBlank() }
    val email = fallbackId.takeIf { it.contains("@") }
      ?: payloadJson?.optString("email")?.takeIf { it.isNotBlank() }
    val displayName = fallbackDisplayName?.takeIf { it.isNotBlank() }
      ?: payloadJson?.optString("name")?.takeIf { it.isNotBlank() }
      ?: email?.substringBefore("@")
      ?: fallbackId.takeIf { it.isNotBlank() }

    val uid = if (sub != null) "google_$sub" else "google_${fallbackId.hashCode().toUInt()}"
    return GoogleSessionUser(uid = uid, displayName = displayName, email = email)
  }

  private suspend fun completeSignInWithCredential(
    context: Context,
    googleCred: GoogleIdTokenCredential,
  ): String? {
    if (isFirebaseReady(context)) {
      val authCredential = GoogleAuthProvider.getCredential(googleCred.idToken, null)
      Firebase.auth.signInWithCredential(authCredential).await()
      val fbUser = Firebase.auth.currentUser
      val session = GoogleSessionUser(
        uid = fbUser?.uid ?: "google_${googleCred.id.hashCode().toUInt()}",
        displayName = fbUser?.displayName?.takeIf { it.isNotBlank() }
          ?: googleCred.displayName?.takeIf { it.isNotBlank() }
          ?: fbUser?.email?.substringBefore("@"),
        email = fbUser?.email ?: googleCred.id.takeIf { it.contains("@") },
      )
      persistSession(context, session)
      return session.displayName
    }

    val directSession = extractGoogleSessionFromIdToken(
      idToken = googleCred.idToken,
      fallbackId = googleCred.id,
      fallbackDisplayName = googleCred.displayName,
    )
    persistSession(context, directSession)
    return directSession.displayName
  }

  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (displayName: String?) -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope,
  ) {
    val existing = restoreSavedSession(context)
    if (existing != null) {
      onAuthSuccess(existing.displayName ?: existing.email?.substringBefore("@"))
      return
    }
    val clientId = resolveDefaultWebClientId(context)

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
          val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
          val name = completeSignInWithCredential(context, googleCred)
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
    val clientId = resolveDefaultWebClientId(context)
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
          val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
          val name = completeSignInWithCredential(context, googleCred)
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
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {},
  ) {
    clearSavedSession(context)
    scope.launch {
      withContext(NonCancellable) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w("Auth", "Failed to clear credential state: ${e.message}")
        } finally {
          if (isFirebaseReady(context)) {
            runCatching { Firebase.auth.signOut() }
          }
          onSignOutComplete()
        }
      }
    }
  }
}

package com.example.ui.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlatformSignInController(
  val pendingAuth: PendingPlatformAuth?,
  private val onStartLogin: (String) -> Unit,
  private val onOpenDialogDirect: (String) -> Unit,
) {
  fun startSignIn(providerId: String) = onStartLogin(providerId)
  fun openVerificationDialog(providerId: String) = onOpenDialogDirect(providerId)
}

@Composable
fun rememberPlatformSignInController(
  onAccountLinked: (providerId: String, displayName: String, externalSubject: String) -> Unit,
  onStatusMessage: (String) -> Unit,
): PlatformSignInController {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val pendingAuth by PlatformOAuthManager.pendingAuth.collectAsStateWithLifecycle()

  var activeDialogMeta by remember { mutableStateOf<UnifiedPlatformMeta?>(null) }
  var returnedFromBrowser by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    PlatformOAuthManager.restorePendingAuth(context)
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        scope.launch {
          // Give onNewIntent / deep-link OAuth redirect a brief window to consume first
          delay(320L)
          val pending = PlatformOAuthManager.restorePendingAuth(context)
          if (pending != null && activeDialogMeta?.providerId != pending.providerId) {
            val meta = PlatformOAuthManager.findPlatformMeta(pending.providerId)
            if (meta != null) {
              returnedFromBrowser = true
              activeDialogMeta = meta
            } else {
              PlatformOAuthManager.clearPendingAuth(context)
            }
          }
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val startLogin: (String) -> Unit = { providerId ->
    val meta = PlatformOAuthManager.findPlatformMeta(providerId)
    if (meta == null) {
      onStatusMessage("Unsupported platform: $providerId")
    } else if (providerId == "steam" && meta.oauthSpec?.isConfigured(context) != true) {
      // Steam supports instant live verification by SteamID / Vanity URL without needing browser return
      returnedFromBrowser = false
      activeDialogMeta = meta
    } else {
      val res = PlatformOAuthManager.beginPortalSignIn(context, providerId)
      if (res.isSuccess) {
        val oauthConfigured = meta.oauthSpec?.isConfigured(context) == true
        onStatusMessage(
          if (oauthConfigured) {
            "Opened ${meta.label} OAuth sign-in in browser..."
          } else {
            "Opened ${meta.label} portal — return to UnknownOS after signing in to finish linking."
          },
        )
      } else {
        onStatusMessage(res.exceptionOrNull()?.localizedMessage ?: "Unable to launch ${meta.label} login")
      }
    }
  }

  val openDialogDirect: (String) -> Unit = { providerId ->
    val meta = PlatformOAuthManager.findPlatformMeta(providerId)
    if (meta != null) {
      returnedFromBrowser = false
      activeDialogMeta = meta
    }
  }

  activeDialogMeta?.let { meta ->
    PlatformSignInDialog(
      meta = meta,
      returnedFromBrowser = returnedFromBrowser,
      onDismiss = {
        PlatformOAuthManager.clearPendingAuth(context)
        returnedFromBrowser = false
        activeDialogMeta = null
      },
      onVerified = { displayName, externalSubject ->
        PlatformOAuthManager.clearPendingAuth(context)
        returnedFromBrowser = false
        activeDialogMeta = null
        onAccountLinked(meta.providerId, displayName, externalSubject)
      },
      onStatusMessage = onStatusMessage,
    )
  }

  return remember(pendingAuth, startLogin, openDialogDirect) {
    PlatformSignInController(
      pendingAuth = pendingAuth,
      onStartLogin = startLogin,
      onOpenDialogDirect = openDialogDirect,
    )
  }
}

@Composable
fun PlatformSignInDialog(
  meta: UnifiedPlatformMeta,
  returnedFromBrowser: Boolean,
  onDismiss: () -> Unit,
  onVerified: (displayName: String, externalSubject: String) -> Unit,
  onStatusMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isSteam = meta.providerId == "steam"

  var handleInput by remember(meta.providerId) { mutableStateOf("") }
  var isVerifying by remember(meta.providerId) { mutableStateOf(false) }
  var errorText by remember(meta.providerId) { mutableStateOf<String?>(null) }
  var showClientConfig by remember(meta.providerId) { mutableStateOf(false) }
  var clientIdDraft by remember(meta.providerId) {
    mutableStateOf(PlatformOAuthManager.getRuntimeClientId(context, meta.providerId))
  }

  AlertDialog(
    onDismissRequest = { if (!isVerifying) onDismiss() },
    title = {
      Text(
        text = if (returnedFromBrowser) "Complete ${meta.label} Sign-In" else "Sign In: ${meta.label}",
        color = Color(meta.accentHex),
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (returnedFromBrowser) {
          Surface(
            color = Color(0xFF00E676).copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, Color(0xFF00E676).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
          ) {
            Text(
              text = if (meta.oauthSpec?.isConfigured(context) == true) {
                "Returned from ${meta.label} browser tab without an OAuth callback. Confirm your account below or retry OAuth."
              } else {
                "Returned from ${meta.label} login portal. Confirm your signed-in account below to bind it to UnknownOS."
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(10.dp),
            )
          }
        } else {
          Text(
            text = if (isSteam) {
              "Verify your live Steam Community profile directly (no API key needed), or open Steam sign-in in your browser first."
            } else {
              meta.subtitle
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        OutlinedTextField(
          value = handleInput,
          onValueChange = {
            handleInput = it.take(120)
            errorText = null
          },
          label = { Text(meta.handleHint.ifBlank { "${meta.label} Account ID" }) },
          singleLine = true,
          enabled = !isVerifying,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_platform_handle_input"),
        )

        OutlinedButton(
          onClick = {
            val res = PlatformOAuthManager.openPortalUrl(context, meta.portalUrl)
            if (res.isFailure) {
              errorText = res.exceptionOrNull()?.localizedMessage ?: "Unable to open browser"
            }
          },
          enabled = !isVerifying && meta.portalUrl.isNotBlank(),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_reopen_portal_btn"),
        ) {
          Text(if (returnedFromBrowser) "Re-open ${meta.label} Portal" else "Open ${meta.label} Sign-In Portal")
        }

        if (meta.oauthSpec != null) {
          TextButton(
            onClick = { showClientConfig = !showClientConfig },
            enabled = !isVerifying,
            modifier = Modifier.testTag("dialog_toggle_oauth_client_btn"),
          ) {
            Text(
              if (showClientConfig) "Hide OAuth Client Config"
              else "Advanced: Direct OAuth Client ID (${meta.oauthSpec.secretKeyName})",
              style = MaterialTheme.typography.labelSmall,
            )
          }
          if (showClientConfig) {
            OutlinedTextField(
              value = clientIdDraft,
              onValueChange = { clientIdDraft = it.take(160) },
              label = { Text(meta.oauthSpec.secretKeyName) },
              singleLine = true,
              enabled = !isVerifying,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
              onClick = {
                PlatformOAuthManager.saveRuntimeClientId(context, meta.providerId, clientIdDraft)
                val saved = clientIdDraft.trim()
                if (saved.isNotEmpty()) {
                  val res = PlatformOAuthManager.launchExternalOAuth(context, meta.oauthSpec)
                  if (res.isFailure) {
                    errorText = res.exceptionOrNull()?.localizedMessage ?: "OAuth launch failed"
                  } else {
                    onStatusMessage("Saved ${meta.oauthSpec.secretKeyName} & launched direct OAuth...")
                  }
                } else {
                  onStatusMessage("Cleared custom ${meta.oauthSpec.secretKeyName}")
                }
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Save Client ID & Launch Direct OAuth")
            }
          }
        }

        if (errorText != null) {
          Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("dialog_platform_error_banner"),
          ) {
            Text(
              text = errorText!!,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(10.dp),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val trimmed = handleInput.trim()
          if (isSteam) {
            isVerifying = true
            errorText = null
            scope.launch {
              val res = PlatformOAuthManager.resolveSteamProfile(trimmed)
              isVerifying = false
              if (res.isSuccess) {
                val steam = res.getOrThrow()
                onVerified(steam.personaName, steam.steamId64)
              } else {
                errorText = res.exceptionOrNull()?.localizedMessage ?: "Steam verification failed"
              }
            }
          } else {
            if (trimmed.length < 2) {
              errorText = "Enter a valid ${meta.label} account name or ID (at least 2 characters)."
            } else {
              val subject = "${meta.providerId}:${trimmed.lowercase()}"
              onVerified(trimmed.take(64), subject.take(128))
            }
          }
        },
        enabled = !isVerifying,
        modifier = Modifier.testTag("dialog_confirm_platform_signin"),
      ) {
        if (isVerifying) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Text("Verifying...")
          }
        } else {
          Text(if (isSteam) "Verify Steam Profile" else "Complete Sign-In")
        }
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isVerifying,
        modifier = Modifier.testTag("dialog_cancel_platform_signin"),
      ) {
        Text("Cancel")
      }
    },
  )
}

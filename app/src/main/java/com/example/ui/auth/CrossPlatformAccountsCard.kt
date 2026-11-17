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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.LinkedAccount
import com.example.ui.theme.AppIcons

@Composable
fun CrossPlatformAccountsCard(
  linkedAccounts: List<LinkedAccount>,
  googleAccountLabel: String? = null,
  isGoogleSigningIn: Boolean = false,
  onSignInGoogle: (() -> Unit)? = null,
  onSignOutGoogle: (() -> Unit)? = null,
  onLinkExternal: (providerId: String, displayName: String, externalSubject: String) -> Unit = { _, _, _ -> },
  onUnlinkExternal: (String) -> Unit,
  onStatusMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  val externalLinkedMap = remember(linkedAccounts) {
    linkedAccounts.associateBy { it.providerId }
  }
  val platformController = rememberPlatformSignInController(
    onAccountLinked = onLinkExternal,
    onStatusMessage = onStatusMessage,
  )
  val pendingProviderId = platformController.pendingAuth?.providerId

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = AppIcons.Devices,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "CROSS-PLATFORM & STORE ACCOUNTS",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Text(
        text = buildString {
          if (googleAccountLabel != null) {
            append("Google Account active ($googleAccountLabel). ")
          } else {
            append("Local UnknownOS Profile Active. ")
          }
          append("${linkedAccounts.size} external platform(s) linked. Sign in below — returns from browser are detected automatically.")
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // Primary Google Account row
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (googleAccountLabel != null) {
          Color(0xFF00E676).copy(alpha = 0.12f)
        } else {
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        modifier = Modifier
          .fillMaxWidth()
          .border(
            1.dp,
            if (googleAccountLabel != null) Color(0xFF00E676).copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            RoundedCornerShape(12.dp),
          ),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Google Account",
              style = MaterialTheme.typography.titleMedium,
              color = if (googleAccountLabel != null) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = googleAccountLabel?.let { "Signed in as $it" }
                ?: "Not signed in — local device profile active",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(Modifier.width(8.dp))
          if (googleAccountLabel != null && onSignOutGoogle != null) {
            TextButton(
              onClick = onSignOutGoogle,
              colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.testTag("desktop_google_sign_out_btn"),
            ) {
              Text("Sign Out")
            }
          } else if (onSignInGoogle != null) {
            OutlinedButton(
              onClick = onSignInGoogle,
              enabled = !isGoogleSigningIn,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("desktop_google_sign_in_btn"),
            ) {
              Text(if (isGoogleSigningIn) "Signing in..." else "Sign In")
            }
          }
        }
      }

      // Console & Mobile Account Portals: Apple (iOS), Xbox (Microsoft), PlayStation, Nintendo, Ubisoft
      PlatformOAuthManager.platformPortalProviders.forEach { spec ->
        val linked = externalLinkedMap[spec.providerId]
        PlatformAccountRow(
          title = spec.label,
          accentColor = Color(spec.accentHex),
          subtitle = when {
            linked != null -> "Signed in: ${linked.displayName}"
            pendingProviderId == spec.providerId -> "Waiting for browser sign-in return..."
            else -> spec.platformNote
          },
          isLinked = linked != null,
          isPending = pendingProviderId == spec.providerId,
          signInLabel = "Sign In",
          signInTestTag = "portal_${spec.providerId}",
          signOutTestTag = "sign_out_${spec.providerId}",
          onSignIn = { platformController.startSignIn(spec.providerId) },
          onFinishPending = { platformController.openVerificationDialog(spec.providerId) },
          onSignOut = { onUnlinkExternal(spec.providerId) },
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

      // OAuth 2.0 PKCE & Steam OpenID 2.0 Storefronts (Epic, Steam, GOG, EA/Origin, Amazon, Galaxy Store)
      PlatformOAuthManager.externalOAuthProviders.forEach { spec ->
        val linked = externalLinkedMap[spec.id]
        val configured = spec.isConfigured(context)
        val isPending = pendingProviderId == spec.id
        PlatformAccountRow(
          title = "${spec.label} • ${spec.platformGroup}",
          accentColor = Color(spec.accentHex),
          subtitle = when {
            linked != null -> "Signed in: ${linked.displayName}"
            isPending -> "Waiting for ${spec.label} login return..."
            spec.id == "steam" -> "Live Steam Community check or OpenID sign-in"
            spec.id == "epic" -> "PC & Cross-Platform Bridge (auto-syncs PSN, Nintendo & Ubisoft via OAuth)"
            configured -> "Direct in-app OAuth ready"
            else -> "Sign in via official portal (auto-detected on return)"
          },
          isLinked = linked != null,
          isPending = isPending,
          signInLabel = if (configured) "Connect" else "Sign In",
          signInTestTag = if (configured) "connect_oauth_${spec.id}" else "portal_oauth_${spec.id}",
          signOutTestTag = "sign_out_${spec.id}",
          onSignIn = { platformController.startSignIn(spec.id) },
          onFinishPending = { platformController.openVerificationDialog(spec.id) },
          onSignOut = { onUnlinkExternal(spec.id) },
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

      PlatformOAuthManager.epicBridgedPlatforms.forEach { bridgedSpec ->
        val synced = externalLinkedMap[bridgedSpec.providerId]
        val isPending = pendingProviderId == bridgedSpec.providerId
        PlatformAccountRow(
          title = bridgedSpec.label,
          accentColor = Color(bridgedSpec.accentHex),
          subtitle = when {
            synced != null -> "Signed in: ${synced.displayName}"
            isPending -> "Waiting for ${bridgedSpec.label} login return..."
            else -> bridgedSpec.platformNote
          },
          isLinked = synced != null,
          isPending = isPending,
          signInLabel = "Sign In",
          signInTestTag = "connect_bridged_${bridgedSpec.providerId}",
          signOutTestTag = "sign_out_${bridgedSpec.providerId}",
          onSignIn = { platformController.startSignIn(bridgedSpec.providerId) },
          onFinishPending = { platformController.openVerificationDialog(bridgedSpec.providerId) },
          onSignOut = { onUnlinkExternal(bridgedSpec.providerId) },
        )
      }
    }
  }
}

@Composable
private fun PlatformAccountRow(
  title: String,
  accentColor: Color,
  subtitle: String,
  isLinked: Boolean,
  isPending: Boolean,
  signInLabel: String,
  signInTestTag: String,
  signOutTestTag: String,
  onSignIn: () -> Unit,
  onFinishPending: () -> Unit,
  onSignOut: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (isLinked) {
      Color(0xFF00E676).copy(alpha = 0.12f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    },
    modifier = Modifier
      .fillMaxWidth()
      .border(
        1.dp,
        if (isLinked) Color(0xFF00E676).copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        RoundedCornerShape(12.dp),
      ),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = if (isLinked) Color(0xFF00E676) else accentColor,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = if (isLinked) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(Modifier.width(8.dp))
      when {
        isLinked -> {
          TextButton(
            onClick = onSignOut,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.testTag(signOutTestTag),
          ) {
            Text("Sign Out")
          }
        }
        isPending -> {
          OutlinedButton(
            onClick = onFinishPending,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("finish_$signInTestTag"),
          ) {
            Text("Finish")
          }
        }
        else -> {
          OutlinedButton(
            onClick = onSignIn,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag(signInTestTag),
          ) {
            Text(signInLabel)
          }
        }
      }
    }
  }
}

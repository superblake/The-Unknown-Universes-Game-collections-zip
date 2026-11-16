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
  onUnlinkExternal: (String) -> Unit,
  onStatusMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  val externalLinkedMap = remember(linkedAccounts) {
    linkedAccounts.associateBy { it.providerId }
  }

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
        text = "Local UnknownOS Profile Active. Open official iOS/Apple & Xbox portals, connect storefronts via OAuth PKCE / Steam OpenID, or manage Epic Connections.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // Official Console & Mobile Account Portals: Apple (iOS) & Xbox (Microsoft)
      PlatformOAuthManager.platformPortalProviders.forEach { spec ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = spec.label,
                style = MaterialTheme.typography.titleMedium,
                color = Color(spec.accentHex),
              )
              Text(
                text = spec.platformNote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
              onClick = {
                val res = PlatformOAuthManager.openPortalUrl(context, spec.portalUrl)
                if (res.isFailure) {
                  onStatusMessage(res.exceptionOrNull()?.localizedMessage ?: "Unable to open browser")
                }
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("portal_${spec.providerId}"),
            ) {
              Text("Portal")
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

      // OAuth 2.0 PKCE & Steam OpenID 2.0 Storefronts (Epic, Steam, GOG, EA/Origin, Amazon, Galaxy Store)
      PlatformOAuthManager.externalOAuthProviders.forEach { spec ->
        val linked = externalLinkedMap[spec.id]
        val configured = spec.isConfigured()
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${spec.label} • ${spec.platformGroup}",
                style = MaterialTheme.typography.titleMedium,
                color = Color(spec.accentHex),
              )
              Text(
                text = when {
                  linked != null -> "Linked: ${linked.displayName}"
                  configured -> "Direct in-app OAuth ready"
                  else -> "Sign in via official account portal"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (linked != null) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(Modifier.width(8.dp))
            when {
              linked != null -> {
                TextButton(
                  onClick = { onUnlinkExternal(spec.id) },
                  colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                  Text("Unlink")
                }
              }
              configured -> {
                OutlinedButton(
                  onClick = {
                    val res = PlatformOAuthManager.launchExternalOAuth(context, spec)
                    if (res.isFailure) {
                      onStatusMessage(res.exceptionOrNull()?.localizedMessage ?: "OAuth launch failed")
                    }
                  },
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.testTag("connect_oauth_${spec.id}"),
                ) {
                  Text("Connect")
                }
              }
              else -> {
                OutlinedButton(
                  onClick = {
                    val res = PlatformOAuthManager.openPortalUrl(context, spec.portalUrl)
                    if (res.isFailure) {
                      onStatusMessage(res.exceptionOrNull()?.localizedMessage ?: "Unable to open browser")
                    }
                  },
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.testTag("portal_oauth_${spec.id}"),
                ) {
                  Text("Sign In")
                }
              }
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "EPIC ACCOUNT BRIDGE",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.tertiary,
        )
        OutlinedButton(
          onClick = {
            val res = PlatformOAuthManager.openEpicConnectionsPortal(context)
            if (res.isFailure) {
              onStatusMessage(res.exceptionOrNull()?.localizedMessage ?: "Unable to open browser")
            }
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("open_epic_connections_btn"),
        ) {
          Text("Epic Connections", style = MaterialTheme.typography.labelSmall)
        }
      }
      Text(
        text = "Epic Games links PlayStation, Nintendo, Xbox, Steam, Apple & Ubisoft under one account—no console developer portals required.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      PlatformOAuthManager.epicBridgedPlatforms.forEach { bridgedSpec ->
        val synced = externalLinkedMap[bridgedSpec.providerId]
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = bridgedSpec.label,
                style = MaterialTheme.typography.labelLarge,
                color = Color(bridgedSpec.accentHex),
              )
              Text(
                text = synced?.displayName ?: "Synced automatically via Epic Account Services",
                style = MaterialTheme.typography.labelSmall,
                color = if (synced != null) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (synced != null) {
              TextButton(
                onClick = { onUnlinkExternal(bridgedSpec.providerId) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
              ) {
                Text("Unlink")
              }
            }
          }
        }
      }
    }
  }
}

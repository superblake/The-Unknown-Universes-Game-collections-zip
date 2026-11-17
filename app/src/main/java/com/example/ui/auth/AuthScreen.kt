package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.BuildConfig
import com.example.R
import com.example.data.model.LinkedAccount
import com.example.ui.theme.AppIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
  initialCallsign: String = "VanguardPrime",
  linkedAccounts: List<LinkedAccount> = emptyList(),
  externalAuthNotice: String? = null,
  externalAuthError: String? = null,
  onLinkExternalAccount: (providerId: String, displayName: String, externalSubject: String) -> Unit = { _, _, _ -> },
  onUnlinkExternalAccount: (providerId: String) -> Unit = {},
  onBootLocalOperator: (callsign: String) -> Unit,
) {
  var callsign by remember(initialCallsign) { mutableStateOf(initialCallsign) }
  var validationError by remember { mutableStateOf<String?>(null) }
  var authNotice by remember { mutableStateOf<String?>(null) }
  var isGoogleSigningIn by remember { mutableStateOf(false) }
  var showOtherLogins by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val linkedByProvider = remember(linkedAccounts) { linkedAccounts.associateBy { it.providerId } }

  LaunchedEffect(externalAuthNotice) {
    if (!externalAuthNotice.isNullOrBlank()) {
      authNotice = externalAuthNotice
      validationError = null
    }
  }
  LaunchedEffect(externalAuthError) {
    if (!externalAuthError.isNullOrBlank()) {
      validationError = externalAuthError
    }
  }

  val platformController = rememberPlatformSignInController(
    onAccountLinked = { providerId, displayName, externalSubject ->
      onLinkExternalAccount(providerId, displayName, externalSubject)
      val metaLabel = PlatformOAuthManager.findPlatformMeta(providerId)?.label ?: providerId
      validationError = null
      authNotice = "Signed in with $metaLabel ($displayName). Tap Boot UnknownOS to enter."
      if (callsign.trim() == "VanguardPrime" || callsign.trim().length < 2) {
        val candidate = displayName.substringBefore(" (via ").trim().take(30)
        if (candidate.length >= 2) callsign = candidate
      }
    },
    onStatusMessage = { msg ->
      validationError = null
      authNotice = msg
    },
  )

  val handleGoogleSignInSuccess: (String?) -> Unit = { googleName ->
    isGoogleSigningIn = false
    val resolved = googleName?.trim()?.takeIf { it.length >= 2 }?.take(30)
      ?: callsign.trim().takeIf { it.length >= 2 }
      ?: "VanguardPrime"
    onBootLocalOperator(resolved)
  }

  val handleGoogleSignInError: (String) -> Unit = { err ->
    isGoogleSigningIn = false
    validationError = err
  }

  val handleGoogleSignInCancelled: () -> Unit = {
    isGoogleSigningIn = false
    authNotice =
      "Google Sign-In was cancelled. Tap Sign in with Google to retry, or tap Boot UnknownOS to play locally."
  }

  LaunchedEffect(Unit) {
    GoogleSignInHelper.attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = { googleName ->
        val resolved = googleName?.trim()?.takeIf { it.length >= 2 }?.take(30)
          ?: callsign.trim().takeIf { it.length >= 2 }
          ?: "VanguardPrime"
        onBootLocalOperator(resolved)
      },
      onUnauthenticated = {},
      scope = coroutineScope,
    )
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              MaterialTheme.colorScheme.background,
              Color(0xFF0C152B),
              MaterialTheme.colorScheme.background,
            )
          )
        )
        .safeDrawingPadding()
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(190.dp)
              .clip(RoundedCornerShape(20.dp))
              .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                RoundedCornerShape(20.dp),
              )
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_universe_hero_banner_1794591188619),
            contentDescription = stringResource(R.string.cd_hero_banner),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xCC070A12))
                  )
                )
          )
          Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
          ) {
            Text(
              text = stringResource(R.string.os_boot_tag),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = stringResource(R.string.app_name).uppercase(),
              style = MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "App Package ID: ${BuildConfig.APPLICATION_ID}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 4.dp,
          modifier =
            Modifier.fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = stringResource(R.string.auth_title),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.auth_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FeatureHighlightRow(
              icon = AppIcons.Gamepad,
              title = "5 Crossover Worlds & 5 Modes",
              subtitle = "Zombies, DMZ Extraction, Multiverse BR, Skirmish & Sandbox",
            )
            FeatureHighlightRow(
              icon = AppIcons.Animation,
              title = "ForgeRig Skeletal & Blender Studio",
              subtitle = "Interactive joint posing, completed animation clips & bpy script export",
            )
            FeatureHighlightRow(
              icon = AppIcons.Devices,
              title = "Cross-Platform HTML5 Package Export",
              subtitle = "Playable builds for Console, iOS Safari & PC browsers without store lock-in",
            )
            FeatureHighlightRow(
              icon = AppIcons.Shield,
              title = "Google Account + On-Device Persistence",
              subtitle = "Sign in with Google or boot locally with saved Perks, Powers, Rigs & Vault",
            )

            OutlinedTextField(
              value = callsign,
              onValueChange = {
                callsign = it.take(30)
                validationError = null
                authNotice = null
              },
              label = { Text("Operator Callsign") },
              placeholder = { Text("VanguardPrime") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("operator_callsign_input"),
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
              onClick = {
                validationError = null
                authNotice = null
                isGoogleSigningIn = true
                GoogleSignInHelper.onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = handleGoogleSignInSuccess,
                  onAuthError = handleGoogleSignInError,
                  scope = coroutineScope,
                  onAuthCancelled = handleGoogleSignInCancelled,
                )
              },
              enabled = !isGoogleSigningIn,
              shape = RoundedCornerShape(14.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .height(52.dp)
                  .testTag("google_sign_in_button"),
            ) {
              if (isGoogleSigningIn) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Signing in with Google...",
                  style = MaterialTheme.typography.labelLarge,
                )
              } else {
                Icon(
                  imageVector = AppIcons.Shield,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Sign in with Google",
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }

            // Other Platform Logins & Detected Linked Sessions
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              modifier = Modifier
                .fillMaxWidth()
                .border(
                  1.dp,
                  if (linkedAccounts.isNotEmpty()) Color(0xFF00E676).copy(alpha = 0.45f)
                  else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                  RoundedCornerShape(14.dp),
                )
                .testTag("auth_other_logins_card"),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOtherLogins = !showOtherLogins }
                    .testTag("toggle_other_logins_btn"),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Other Platform Logins",
                      style = MaterialTheme.typography.titleSmall,
                      color = if (linkedAccounts.isNotEmpty()) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = if (linkedAccounts.isNotEmpty()) {
                        "${linkedAccounts.size} platform login(s) detected on device"
                      } else {
                        "Apple, Xbox, Steam, Epic, PSN, Nintendo, Ubisoft, GOG, EA & more"
                      },
                      style = MaterialTheme.typography.labelSmall,
                      color = if (linkedAccounts.isNotEmpty()) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  TextButton(onClick = { showOtherLogins = !showOtherLogins }) {
                    Text(if (showOtherLogins) "Hide" else "Manage")
                  }
                }

                if (linkedAccounts.isNotEmpty()) {
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    linkedAccounts.forEach { acc ->
                      val label = PlatformOAuthManager.findPlatformMeta(acc.providerId)?.label
                        ?.substringBefore(" (")
                        ?: acc.providerId
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF00E676).copy(alpha = 0.14f),
                        modifier = Modifier.border(
                          1.dp,
                          Color(0xFF00E676).copy(alpha = 0.4f),
                          RoundedCornerShape(8.dp),
                        ),
                      ) {
                        Text(
                          text = "$label: ${acc.displayName}",
                          style = MaterialTheme.typography.labelSmall,
                          color = Color(0xFF00E676),
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                      }
                    }
                  }
                }

                AnimatedVisibility(visible = showOtherLogins) {
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlatformOAuthManager.allUnifiedPlatforms().forEach { meta ->
                      val linked = linkedByProvider[meta.providerId]
                      val isPending = platformController.pendingAuth?.providerId == meta.providerId
                      Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (linked != null) {
                          Color(0xFF00E676).copy(alpha = 0.12f)
                        } else {
                          MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                          Column(modifier = Modifier.weight(1f)) {
                            Text(
                              text = meta.label,
                              style = MaterialTheme.typography.labelLarge,
                              color = if (linked != null) Color(0xFF00E676) else Color(meta.accentHex),
                            )
                            Text(
                              text = when {
                                linked != null -> "Signed in: ${linked.displayName}"
                                isPending -> "Waiting for browser return..."
                                else -> meta.subtitle
                              },
                              style = MaterialTheme.typography.labelSmall,
                              color = if (linked != null) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                          }
                          Spacer(Modifier.width(8.dp))
                          when {
                            linked != null -> {
                              TextButton(
                                onClick = { onUnlinkExternalAccount(meta.providerId) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("auth_sign_out_${meta.providerId}"),
                              ) {
                                Text("Sign Out")
                              }
                            }
                            isPending -> {
                              OutlinedButton(
                                onClick = { platformController.openVerificationDialog(meta.providerId) },
                                shape = RoundedCornerShape(8.dp),
                              ) {
                                Text("Finish")
                              }
                            }
                            else -> {
                              OutlinedButton(
                                onClick = { platformController.startSignIn(meta.providerId) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("auth_login_${meta.providerId}"),
                              ) {
                                Text("Sign In")
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            if (validationError != null) {
              Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                  .testTag("auth_error_banner"),
              ) {
                Text(
                  text = validationError!!,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.error,
                  modifier = Modifier.padding(12.dp),
                )
              }
            } else if (authNotice != null) {
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                  .testTag("auth_notice_banner"),
              ) {
                Text(
                  text = authNotice!!,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }

            Button(
              onClick = {
                val trimmed = callsign.trim()
                if (trimmed.length < 2) {
                  authNotice = null
                  validationError = "Enter a callsign with at least 2 characters."
                } else {
                  onBootLocalOperator(trimmed)
                }
              },
              enabled = !isGoogleSigningIn,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
              shape = RoundedCornerShape(14.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .height(54.dp)
                  .testTag("boot_unknownos_button"),
            ) {
              Text(
                text = stringResource(R.string.boot_unknownos),
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
      modifier = Modifier.size(38.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

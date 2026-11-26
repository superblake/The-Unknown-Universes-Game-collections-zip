package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

tailrec fun Context.findActivityResultOwner(): ActivityResultRegistryOwner? =
  this as? ActivityResultRegistryOwner ?: (this as? ContextWrapper)?.baseContext?.findActivityResultOwner()

/**
 * Resolves the human-readable filename (preserving .p3d/.prisma/.obj/.gltf/.json extensions)
 * across SAF providers such as Samsung My Files and Google Drive where lastPathSegment can be
 * an opaque document ID.
 */
fun resolveUriDisplayName(
  context: Context,
  uri: Uri,
  fallback: String = "imported_asset",
): String {
  runCatching {
    context.contentResolver.query(
      uri,
      arrayOf(OpenableColumns.DISPLAY_NAME),
      null,
      null,
      null,
    )?.use { cursor ->
      if (cursor.moveToFirst()) {
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0) {
          val name = cursor.getString(idx)?.trim()
          if (!name.isNullOrEmpty()) return name
        }
      }
    }
  }
  return uri.lastPathSegment
    ?.substringAfterLast('/')
    ?.substringAfterLast(':')
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: fallback
}

@Composable
fun rememberSafeCreateDocumentLauncher(
  mimeType: String,
  onUnavailable: () -> Unit = {},
  onResult: (Uri?) -> Unit,
): (String) -> Unit {
  val context = LocalContext.current
  val owner = LocalActivityResultRegistryOwner.current ?: context.findActivityResultOwner()
  if (owner == null) {
    return { _ -> onUnavailable() }
  }
  var launchBlock: ((String) -> Unit)? = null
  CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
    val launcher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument(mimeType),
      onResult = onResult,
    )
    launchBlock = { fileName ->
      runCatching { launcher.launch(fileName) }
        .onFailure { onUnavailable() }
    }
  }
  return launchBlock ?: { _ -> onUnavailable() }
}

@Composable
fun rememberSafeOpenDocumentLauncher(
  onUnavailable: () -> Unit = {},
  onResult: (Uri?) -> Unit,
): (Array<String>) -> Unit {
  val context = LocalContext.current
  val owner = LocalActivityResultRegistryOwner.current ?: context.findActivityResultOwner()
  if (owner == null) {
    return { _ -> onUnavailable() }
  }
  var launchBlock: ((Array<String>) -> Unit)? = null
  CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
    val launcher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument(),
      onResult = onResult,
    )
    launchBlock = { mimeTypes ->
      runCatching { launcher.launch(mimeTypes) }
        .onFailure { onUnavailable() }
    }
  }
  return launchBlock ?: { _ -> onUnavailable() }
}

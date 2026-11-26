package com.example.util

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

val LocalHideSystemBars = staticCompositionLocalOf { true }

/**
 * Applies consistent edge-to-edge display cutout behavior and transient system bar hiding across
 * standard Android phones, foldables, and OEM tablets such as Samsung Galaxy Tab A9+ (One UI).
 */
@Suppress("DEPRECATION")
fun applyWindowFullscreen(
  window: Window?,
  hideSystemBars: Boolean,
) {
  if (window == null) return
  runCatching {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isStatusBarContrastEnforced = false
      window.isNavigationBarContrastEnforced = false
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val lp = window.attributes
      val targetCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
      } else {
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      }
      if (lp.layoutInDisplayCutoutMode != targetCutoutMode) {
        lp.layoutInDisplayCutoutMode = targetCutoutMode
        window.attributes = lp
      }
    }

    val decor = window.decorView
    val controller = WindowCompat.getInsetsController(window, decor)
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (hideSystemBars) {
      controller.hide(WindowInsetsCompat.Type.systemBars())
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        decor.systemUiVisibility = (
          View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
          )
      }
    } else {
      controller.show(WindowInsetsCompat.Type.systemBars())
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        decor.systemUiVisibility = (
          View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
          )
      }
    }
  }
}

/**
 * Ensures full-screen Jetpack Compose Dialog windows retain the same edge-to-edge & hidden bar state
 * so opening dialogs does not force One UI tablet taskbars or navigation bars back onto screen.
 */
@Composable
fun SyncDialogFullscreen(hideSystemBars: Boolean = LocalHideSystemBars.current) {
  val view = LocalView.current
  DisposableEffect(view, hideSystemBars) {
    val window = (view.parent as? DialogWindowProvider)?.window
    applyWindowFullscreen(window, hideSystemBars)
    val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
      applyWindowFullscreen(window, hideSystemBars)
    }
    view.addOnLayoutChangeListener(listener)
    onDispose {
      view.removeOnLayoutChangeListener(listener)
    }
  }
}

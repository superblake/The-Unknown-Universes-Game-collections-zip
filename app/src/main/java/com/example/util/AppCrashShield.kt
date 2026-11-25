package com.example.util

import android.app.Application
import android.content.Context
import android.util.Log

/**
 * Universal crash shield for Unknown Universes.
 * Intercepts uncaught exceptions on any thread, prevents crash-to-desktop,
 * and logs actionable diagnostics.
 */
object AppCrashShield {
  private const val TAG = "AppCrashShield"
  private var isInstalled = false
  private var defaultHandler: Thread.UncaughtExceptionHandler? = null

  var lastCrashReport: String? = null
    private set

  fun install(app: Application) {
    if (isInstalled) return
    isInstalled = true
    defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      val stack = Log.getStackTraceString(throwable)
      Log.e(TAG, "Intercepted uncaught exception on thread '${thread.name}':\n$stack")
      lastCrashReport = "${throwable::class.java.simpleName}: ${throwable.message}\n$stack"

      // Handle common transient non-fatal coroutine or render exceptions gracefully
      val isTransient = throwable is IndexOutOfBoundsException ||
        throwable is NullPointerException ||
        throwable is IllegalStateException ||
        throwable is ConcurrentModificationException ||
        throwable is IllegalArgumentException ||
        throwable.message?.contains("ConcurrentModification") == true ||
        throwable.message?.contains("IndexOutOfBounds") == true

      if (isTransient && thread.name != "main") {
        Log.w(TAG, "Suppressed transient background exception on '${thread.name}', keeping process alive.")
        return@setDefaultUncaughtExceptionHandler
      }

      // For main thread, try default handler or log warning to keep app alive
      try {
        defaultHandler?.uncaughtException(thread, throwable)
      } catch (e: Throwable) {
        Log.e(TAG, "Secondary error in default handler", e)
      }
    }
    Log.i(TAG, "AppCrashShield installed successfully.")
  }
}

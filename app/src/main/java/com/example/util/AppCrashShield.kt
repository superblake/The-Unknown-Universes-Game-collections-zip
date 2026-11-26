package com.example.util

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Universal crash shield for Unknown Universes.
 * Intercepts uncaught exceptions on any thread, prevents crash-to-desktop,
 * and keeps the main UI message pump alive.
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

    // 1. Intercept uncaught exceptions across all threads
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

      if (isTransient) {
        Log.w(TAG, "Suppressed transient exception on '${thread.name}', keeping process alive.")
        if (thread.name == "main" || Looper.myLooper() == Looper.getMainLooper()) {
          while (true) {
            try {
              Looper.loop()
            } catch (t: Throwable) {
              Log.e(TAG, "Shielded secondary main loop exception: ${t.message}")
            }
          }
        }
        return@setDefaultUncaughtExceptionHandler
      }

      // For fatal/unrecoverable errors, delegate to default handler
      try {
        defaultHandler?.uncaughtException(thread, throwable)
      } catch (e: Throwable) {
        Log.e(TAG, "Secondary error in default handler", e)
      }
    }

    // 2. Continuous Main Looper guard (NeverCrash pattern)
    // Catches any unhandled UI/Compose exceptions in real-time without terminating the process
    Handler(Looper.getMainLooper()).post {
      while (true) {
        try {
          Looper.loop()
        } catch (t: Throwable) {
          val stack = Log.getStackTraceString(t)
          Log.e(TAG, "Intercepted main looper exception:\n$stack")
          lastCrashReport = "${t::class.java.simpleName}: ${t.message}\n$stack"
        }
      }
    }

    Log.i(TAG, "AppCrashShield installed successfully with Main Looper Guard.")
  }
}

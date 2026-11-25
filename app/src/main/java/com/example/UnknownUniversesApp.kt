package com.example

import android.app.Application
import com.example.util.AppCrashShield

class UnknownUniversesApp : Application() {
  override fun onCreate() {
    super.onCreate()
    AppCrashShield.install(this)
  }
}

package com.vkauth.vkid

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.vkauth.vkid.jsinput.App
import com.vkauth.vkid.jsinput.VKID

class InitDelegate(
  @Suppress("unused") private val context: ReactApplicationContext,
) {
  fun initialize(app: App, vkid: VKID) {
    // Конфигурация клиента — через manifest placeholders (VKIDClientID и т.д.) и VKID.init в Application.
    Log.d(TAG, "initialize mode=${app.mode} appName=${vkid.appName}")
  }

  private companion object {
    private const val TAG = "VkAuth"
  }
}

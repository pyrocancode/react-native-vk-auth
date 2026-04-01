package com.vkauth.vkid

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.vkauth.vkid.jsinput.App
import com.vkauth.vkid.jsinput.AuthFlow
import com.vkauth.vkid.jsinput.VKID

class InitDelegate(
  @Suppress("unused") private val context: ReactApplicationContext,
) {
  fun initialize(app: App, vkid: VKID) {
    VkAuthConfig.useAuthorizationCodeFlow = app.authFlow == AuthFlow.AUTHORIZATION_CODE
    VkAuthConfig.scopes = app.scopes
    Log.d(
      TAG,
      "initialize mode=${app.mode} appName=${vkid.appName} authFlow=${app.authFlow} scopes=${app.scopes.size}",
    )
  }

  private companion object {
    private const val TAG = "VkAuth"
  }
}

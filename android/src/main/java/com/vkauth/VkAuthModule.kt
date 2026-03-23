package com.vkauth

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.vkauth.vkid.AuthDelegate
import com.vkauth.vkid.InitDelegate
import com.vkauth.vkid.jsinput.App
import com.vkauth.vkid.jsinput.VKID as VkIdInput

class VkAuthModule(
  reactContext: ReactApplicationContext,
  private val initDelegate: InitDelegate,
  private val authDelegate: AuthDelegate,
) : ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String = "VkAuth"

  @ReactMethod
  fun initialize(app: ReadableMap, vkid: ReadableMap) {
    initDelegate.initialize(App.fromMap(app), VkIdInput.fromMap(vkid))
  }

  @ReactMethod
  fun startAuth() {
    authDelegate.startAuth()
  }

  @ReactMethod
  fun closeAuth() {
    authDelegate.closeAuth()
  }

  @ReactMethod
  fun addListener(@Suppress("UNUSED_PARAMETER") eventName: String) {}

  @ReactMethod
  fun removeAllListeners() {}

  @ReactMethod
  fun removeListeners(@Suppress("UNUSED_PARAMETER") type: Int?) {}

  @ReactMethod
  fun openURL(@Suppress("UNUSED_PARAMETER") url: String) {
    // Редирект обрабатывает VK ID SDK / система; при необходимости дополните.
  }

  @ReactMethod
  fun accessTokenChangedSuccess(token: String, userId: Int) {
    authDelegate.accessTokenChangedSuccess(token, userId)
  }

  @ReactMethod
  fun accessTokenChangedFailed(@Suppress("UNUSED_PARAMETER") error: ReadableMap) {
    authDelegate.accessTokenChangedFailed()
  }

  @ReactMethod
  fun logout() {
    authDelegate.logout()
  }

  @ReactMethod
  fun getUserSessions(promise: Promise) {
    authDelegate.getUserSessions(promise)
  }

  @ReactMethod
  fun getUserProfile(promise: Promise) {
    authDelegate.getUserProfile(promise)
  }
}

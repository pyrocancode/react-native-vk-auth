package com.vkauth.vkid

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.AuthCodeData
import com.vk.id.auth.VKIDAuthCallback
import com.vk.id.auth.VKIDAuthParams
import com.vk.id.logout.VKIDLogoutCallback
import com.vk.id.logout.VKIDLogoutFail

class AuthDelegate(private val reactContext: ReactApplicationContext) {

  companion object {
    private const val TAG = "VkAuth"
  }

  init {
    VKID.logsEnabled = true
  }

  fun startAuth() {
    val activity = reactContext.currentActivity as? FragmentActivity
    if (activity == null) {
      Log.e(TAG, "startAuth: no FragmentActivity")
      emitAuthFail("No activity")
      return
    }
    VKID.instance.authorize(
      activity,
      object : VKIDAuthCallback {
        override fun onAuth(accessToken: com.vk.id.AccessToken) {
          Log.d(
            TAG,
            "VKID onAuth: userID=${accessToken.userID} expire=${accessToken.expireTime}",
          )
          emitAuthSuccess(accessToken)
        }

        override fun onAuthCode(data: AuthCodeData, isCompletion: Boolean) {
          Log.d(TAG, "VKID onAuthCode: isCompletion=$isCompletion code=${data.code} deviceId=${data.deviceId}")
        }

        override fun onFail(fail: VKIDAuthFail) {
          Log.e(TAG, "VKID onFail: $fail")
          emitAuthFail(fail.toString())
        }
      },
      VKIDAuthParams.Builder().build(),
    )
  }

  fun closeAuth() {
    // VK ID SDK не требует явного закрытия веб-view из нативного модуля.
  }

  fun logout() {
    val activity = reactContext.currentActivity as? FragmentActivity
    if (activity == null) {
      Log.e(TAG, "logout: no FragmentActivity")
      return
    }
    VKID.instance.logout(
      object : VKIDLogoutCallback {
        override fun onSuccess() {
          Log.d(TAG, "VKID logout success")
          sendEvent("onLogout", null)
        }

        override fun onFail(fail: VKIDLogoutFail) {
          Log.e(TAG, "VKID logout fail: $fail")
        }
      },
      activity,
    )
  }

  fun accessTokenChangedSuccess(token: String, userId: Int) {
    // Старый поток silent token; оставлено для совместимости с JS до миграции приложения.
    Log.d(TAG, "accessTokenChangedSuccess (legacy): userId=$userId")
  }

  fun accessTokenChangedFailed() {
    Log.d(TAG, "accessTokenChangedFailed (legacy)")
  }

  fun getUserSessions(promise: Promise) {
    val token = VKID.instance.accessToken
    val arr = com.facebook.react.bridge.Arguments.createArray()
    if (token != null) {
      val m = com.facebook.react.bridge.Arguments.createMap()
      m.putString("type", "authorized")
      arr.pushMap(m)
    }
    promise.resolve(arr)
  }

  fun getUserProfile(promise: Promise) {
    val token = VKID.instance.accessToken
    if (token == null) {
      promise.reject("E_VK_NOT_AUTHORIZED", "No VK ID access token")
      return
    }
    promise.resolve(VkAuthPayload.profileForJs(token.userID, token.userData))
  }

  fun emitAuthSuccess(accessToken: com.vk.id.AccessToken) {
    val map = VkAuthPayload.fromAccessToken(accessToken)
    map.putMap("profile", VkAuthPayload.profileForJs(accessToken.userID, accessToken.userData))
    sendEvent("onAuth", map)
  }

  fun emitAuthFail(message: String) {
    val map = Arguments.createMap()
    map.putString("type", "error")
    map.putString("error", message)
    sendEvent("onAuth", map)
  }

  private fun sendEvent(eventName: String, params: WritableMap?) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }
}

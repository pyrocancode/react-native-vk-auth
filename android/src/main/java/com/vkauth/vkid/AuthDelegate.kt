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
import com.vk.id.auth.VKIDAuthUiParams
import com.vk.id.logout.VKIDLogoutCallback
import com.vk.id.logout.VKIDLogoutFail
import java.util.UUID

class AuthDelegate(private val reactContext: ReactApplicationContext) {

  companion object {
    private const val TAG = "VkAuth"
  }

  /** Для потока authorization code: переданы в authorize() и нужны бэкенду вместе с [AuthCodeData]. */
  private var pendingCodeVerifier: String? = null
  private var pendingState: String? = null
  private var authorizationCodeEmitted: Boolean = false

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

    authorizationCodeEmitted = false
    pendingCodeVerifier = null
    pendingState = null

    val params =
      VKIDAuthParams.Builder().apply {
        if (VkAuthConfig.useAuthorizationCodeFlow) {
          val verifier = Pkce.generateVerifier()
          val challenge = Pkce.challengeS256(verifier)
          val state = UUID.randomUUID().toString()
          pendingCodeVerifier = verifier
          pendingState = state
          codeChallenge = challenge
          this.state = state
        }
        if (VkAuthConfig.scopes.isNotEmpty()) {
          scopes = VkAuthConfig.scopes
        }
      }.build()

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
          dispatchAuthCode(data, isCompletion)
        }

        override fun onFail(fail: VKIDAuthFail) {
          Log.e(TAG, "VKID onFail: $fail")
          pendingCodeVerifier = null
          pendingState = null
          authorizationCodeEmitted = false
          emitAuthFail(fail.toString())
        }
      },
      params,
    )
  }

  /**
   * Параметры One Tap / UI при потоке authorization code: PKCE (code_challenge) и state,
   * как в [startAuth] с [VKIDAuthParams].
   */
  fun buildAuthorizationCodeUiParams(): VKIDAuthUiParams? {
    if (!VkAuthConfig.useAuthorizationCodeFlow) {
      return null
    }
    authorizationCodeEmitted = false
    pendingCodeVerifier = null
    pendingState = null
    val verifier = Pkce.generateVerifier()
    val challenge = Pkce.challengeS256(verifier)
    val state = UUID.randomUUID().toString()
    pendingCodeVerifier = verifier
    pendingState = state
    return VKIDAuthUiParams.Builder().apply {
      codeChallenge = challenge
      this.state = state
      scopes = VkAuthConfig.scopes
    }.build()
  }

  /**
   * [VKIDAuthUiParams] для One Tap: PKCE при authorization code, иначе только [scopes] (как в
   * [документации One Tap](https://id.vk.com/about/business/go/docs/ru/vkid/latest/vk-id/connection/elements/onetap-button/onetap-android)).
   */
  fun buildOneTapAuthUiParams(): VKIDAuthUiParams? {
    if (VkAuthConfig.useAuthorizationCodeFlow) {
      return buildAuthorizationCodeUiParams()
    }
    if (VkAuthConfig.scopes.isEmpty()) {
      return null
    }
    return VKIDAuthUiParams.Builder().apply {
      scopes = VkAuthConfig.scopes
    }.build()
  }

  /** Общая обработка [AuthCodeData] для [startAuth] и One Tap. */
  fun dispatchAuthCode(data: AuthCodeData, isCompletion: Boolean) {
    Log.d(
      TAG,
      "VKID onAuthCode: isCompletion=$isCompletion code=${data.code} deviceId=${data.deviceId}",
    )
    if (!VkAuthConfig.useAuthorizationCodeFlow) {
      return
    }
    // SDK может сначала вызвать с isCompletion=false (промежуточный код), затем с true — обмен на бэкенде нужен только по финальному.
    if (!isCompletion) {
      Log.d(TAG, "onAuthCode: skip non-final code, wait for isCompletion=true")
      return
    }
    if (authorizationCodeEmitted) {
      return
    }
    val verifier = pendingCodeVerifier
    val state = pendingState
    if (verifier == null || state == null) {
      Log.e(TAG, "onAuthCode: missing PKCE state")
      emitAuthFail("PKCE state lost")
      return
    }
    emitAuthAuthorizationCode(
      code = data.code,
      deviceId = data.deviceId,
      state = state,
      codeVerifier = verifier,
      isCompletion = isCompletion,
    )
    authorizationCodeEmitted = true
    pendingCodeVerifier = null
    pendingState = null
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

  private fun emitAuthAuthorizationCode(
    code: String,
    deviceId: String,
    state: String,
    codeVerifier: String,
    isCompletion: Boolean,
  ) {
    val map = VkAuthPayload.fromAuthorizationCode(
      code = code,
      deviceId = deviceId,
      state = state,
      codeVerifier = codeVerifier,
      isCompletion = isCompletion,
    )
    sendEvent("onAuth", map)
  }

  fun emitAuthSuccess(accessToken: com.vk.id.AccessToken) {
    if (VkAuthConfig.useAuthorizationCodeFlow && authorizationCodeEmitted) {
      Log.d(TAG, "emitAuthSuccess: skipped, authorization code already emitted")
      return
    }
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

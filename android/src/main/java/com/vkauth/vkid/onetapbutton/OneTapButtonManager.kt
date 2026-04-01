package com.vkauth.vkid.onetapbutton

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.vk.id.onetap.xml.OneTap
import com.vkauth.vkid.VkAuthConfig
import com.vkauth.vkid.VkAuthServiceHolder

/**
 * VK ID One Tap (XML). См. [OneTap.setCallbacks](https://vkcom.github.io/vkid-android-sdk/onetap-xml/com.vk.id.onetap.xml/-one-tap/set-callbacks.html).
 */
class OneTabButtonManager : SimpleViewManager<OneTap>() {

  override fun getName(): String = COMPONENT_NAME

  override fun createViewInstance(context: ThemedReactContext): OneTap {
    val view = OneTap(context)
    val delegate = VkAuthServiceHolder.authDelegate
    delegate?.buildOneTapAuthUiParams()?.let { view.authParams = it }
    view.setCallbacks(
      onAuth = { _, accessToken ->
        delegate?.emitAuthSuccess(accessToken)
      },
      onFail = { _, fail ->
        Log.e(TAG, "OneTap onFail: $fail")
        delegate?.emitAuthFail(fail.toString())
        delegate?.buildOneTapAuthUiParams()?.let { view.authParams = it }
      },
      onAuthCode = { data, isCompletion ->
        delegate?.dispatchAuthCode(data, isCompletion)
      },
    )
    return view
  }

  @ReactProp(name = "backgroundStyle")
  fun setBackgroundStyle(@Suppress("UNUSED_PARAMETER") view: OneTap, @Suppress("UNUSED_PARAMETER") style: ReadableMap) {
    // Стили One Tap задаются через [OneTap.style]; при необходимости сопоставьте с [com.vk.id.onetap.common.OneTapStyle].
  }

  @ReactProp(name = "iconGravity")
  fun setIconGravity(@Suppress("UNUSED_PARAMETER") view: OneTap, @Suppress("UNUSED_PARAMETER") gravity: String) {}

  @ReactProp(name = "firstLineFieldType")
  fun setFirstLineField(@Suppress("UNUSED_PARAMETER") view: OneTap, @Suppress("UNUSED_PARAMETER") type: String) {}

  @ReactProp(name = "secondLineFieldType")
  fun setSecondLineField(@Suppress("UNUSED_PARAMETER") view: OneTap, @Suppress("UNUSED_PARAMETER") type: String) {}

  @ReactProp(name = "oneLineTextSize")
  fun setOneLineTextSize(@Suppress("UNUSED_PARAMETER") view: OneTap, @Suppress("UNUSED_PARAMETER") size: Float) {}

  @ReactProp(name = "firstLineTextSize")
  fun setFirstLineTextSize(@Suppress("UNUSED_PARAMETER") view: OneTap, @Suppress("UNUSED_PARAMETER") size: Float) {}

  @ReactProp(name = "secondLineTextSize")
  fun setSecondLineTextSize(@Suppress("UNUSED_PARAMETER") view: OneTap, @Suppress("UNUSED_PARAMETER") size: Float) {}

  @ReactProp(name = "avatarSize")
  fun setAvatarSize(@Suppress("UNUSED_PARAMETER") view: OneTap, @Suppress("UNUSED_PARAMETER") size: Float) {}

  @ReactProp(name = "iconSize")
  fun setVkIconSize(@Suppress("UNUSED_PARAMETER") view: OneTap, @Suppress("UNUSED_PARAMETER") size: Float) {}

  @ReactProp(name = "progressSize")
  fun setProgressSize(@Suppress("UNUSED_PARAMETER") view: OneTap, @Suppress("UNUSED_PARAMETER") size: Float) {}

  @ReactProp(name = "texts")
  fun setTexts(@Suppress("UNUSED_PARAMETER") view: OneTap, @Suppress("UNUSED_PARAMETER") texts: ReadableMap) {}

  private companion object {
    private const val COMPONENT_NAME = "RTCVkOneTapButton"
    private const val TAG = "OneTabButtonManager"
  }
}

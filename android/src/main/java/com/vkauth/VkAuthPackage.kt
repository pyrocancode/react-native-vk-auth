package com.vkauth

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import com.vkauth.vkid.AuthDelegate
import com.vkauth.vkid.InitDelegate
import com.vkauth.vkid.VkAuthServiceHolder
import com.vkauth.vkid.onetapbutton.OneTabButtonManager

class VkAuthPackage : ReactPackage {
  override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
    val authDelegate = AuthDelegate(reactContext)
    VkAuthServiceHolder.authDelegate = authDelegate
    return listOf(
      VkAuthModule(
        reactContext,
        InitDelegate(reactContext),
        authDelegate,
      ),
    )
  }

  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
    return listOf(OneTabButtonManager())
  }
}

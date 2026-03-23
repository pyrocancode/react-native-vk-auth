package com.vkauth.vkid

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.vk.id.AccessToken
import com.vk.id.VKIDUser

/**
 * Сериализация ответа VK ID SDK в WritableMap для JS (событие onAuth и отладка).
 * Документация: https://vkcom.github.io/vkid-android-sdk/
 */
internal object VkAuthPayload {

  fun fromAccessToken(token: AccessToken): WritableMap {
    val map = Arguments.createMap()
    map.putString("type", "authorized")
    map.putMap("vkid", buildVkidDebugMap(token))
    return map
  }

  fun buildVkidDebugMap(token: AccessToken): WritableMap {
    val vkid = Arguments.createMap()
    vkid.putString("accessToken", token.token)
    vkid.putDouble("userID", token.userID.toDouble())
    vkid.putDouble("expireTime", token.expireTime.toDouble())
    token.idToken?.let { vkid.putString("idToken", it) }
    token.scopes?.let { scopes ->
      val arr = Arguments.createArray()
      scopes.forEach { arr.pushString(it) }
      vkid.putArray("scopes", arr)
    }
    vkid.putMap("userData", fromVKIDUser(token.userData))
    return vkid
  }

  fun fromVKIDUser(user: VKIDUser): WritableMap {
    val m = Arguments.createMap()
    m.putString("firstName", user.firstName)
    m.putString("lastName", user.lastName)
    user.phone?.let { m.putString("phone", it) }
    user.email?.let { m.putString("email", it) }
    user.photo50?.let { m.putString("photo50", it) }
    user.photo100?.let { m.putString("photo100", it) }
    user.photo200?.let { m.putString("photo200", it) }
    return m
  }

  fun profileForJs(userId: Long, user: VKIDUser): WritableMap {
    val m = Arguments.createMap()
    val userIdMap = Arguments.createMap()
    userIdMap.putString("value", userId.toString())
    m.putMap("userID", userIdMap)
    m.putString("firstName", user.firstName)
    m.putString("lastName", user.lastName)
    m.putString("phone", user.phone)
    m.putString("photo200", user.photo200)
    m.putString("email", user.email)
    m.putNull("userHash")
    return m
  }
}

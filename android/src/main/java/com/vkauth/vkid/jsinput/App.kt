package com.vkauth.vkid.jsinput

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap

enum class AuthFlow {
  /** Токен на устройстве (по умолчанию). */
  ACCESS_TOKEN,

  /** PKCE + код → обмен на бэкенде; см. VKIDAuthParams.codeChallenge. */
  AUTHORIZATION_CODE,
}

data class App(
  val credentials: Credentials,
  val mode: Mode,
  val authFlow: AuthFlow = AuthFlow.ACCESS_TOKEN,
  val scopes: Set<String> = emptySet(),
) {
  companion object {
    fun fromMap(map: ReadableMap): App {
      val flowStr = if (map.hasKey("authFlow")) map.getString("authFlow") else null
      val authFlow =
        when (flowStr) {
          "authorizationCode" -> AuthFlow.AUTHORIZATION_CODE
          else -> AuthFlow.ACCESS_TOKEN
        }
      return App(
        credentials = Credentials.fromMap(map.getMap("credentials")!!),
        mode = Mode.fromString(map.getString("mode")!!),
        authFlow = authFlow,
        scopes = parseScopes(map),
      )
    }

    private fun parseScopes(map: ReadableMap): Set<String> {
      if (!map.hasKey("scopes")) {
        return emptySet()
      }
      val arr: ReadableArray = map.getArray("scopes") ?: return emptySet()
      val out = LinkedHashSet<String>()
      for (i in 0 until arr.size()) {
        arr.getString(i)?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
      }
      return out
    }
  }
}

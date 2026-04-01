package com.vkauth.vkid

/** Задаётся в [InitDelegate] из JS (`authFlow`, `scopes`). */
object VkAuthConfig {
  @JvmField
  var useAuthorizationCodeFlow: Boolean = false

  @JvmField
  var scopes: Set<String> = emptySet()
}

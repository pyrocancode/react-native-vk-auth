package com.vkauth.vkid

/** Задаётся в [InitDelegate] из JS (`authFlow`). */
object VkAuthConfig {
  @JvmField
  var useAuthorizationCodeFlow: Boolean = false
}

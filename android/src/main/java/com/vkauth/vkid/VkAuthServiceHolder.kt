package com.vkauth.vkid

/**
 * Общая ссылка на [AuthDelegate] для One Tap view и [com.vkauth.VkAuthModule].
 */
internal object VkAuthServiceHolder {
  var authDelegate: AuthDelegate? = null
}

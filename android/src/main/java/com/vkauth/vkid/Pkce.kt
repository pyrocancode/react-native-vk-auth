package com.vkauth.vkid

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/** RFC 7636 PKCE для обмена кода на бэкенде (см. VKIDAuthParams.codeChallenge). */
internal object Pkce {
  fun generateVerifier(): String {
    val random = SecureRandom()
    val bytes = ByteArray(32)
    random.nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  }

  fun challengeS256(verifier: String): String {
    val digest =
      MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  }
}

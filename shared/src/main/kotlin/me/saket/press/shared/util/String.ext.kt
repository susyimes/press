@file:Suppress("NOTHING_TO_INLINE")

package me.saket.press.shared.util

import kotlin.text.isLetterOrDigit as kotlinIsLetterOrDigit

actual inline fun String.toLowerCase(locale: Locale): String {
  return toLowerCase(when (locale) {
    Locale.US -> java.util.Locale.US
  })
}

actual inline fun Char.isLetterOrDigit(): Boolean {
  return this.kotlinIsLetterOrDigit()
}

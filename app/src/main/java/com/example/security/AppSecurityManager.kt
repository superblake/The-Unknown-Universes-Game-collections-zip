package com.example.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom

enum class SaveIntegrityStatus(val label: String) {
  VERIFIED("SHA-256 Integrity Verified"),
  UNSEALED_LEGACY("Legacy Save (No Integrity Seal)"),
  TAMPERED("Modified / Invalid Signature"),
}

data class SecurityAuditReport(
  val pinLockEnabled: Boolean,
  val screenShieldEnabled: Boolean,
  val chatFilterEnabled: Boolean,
  val progressionSealValid: Boolean,
  val backupDisabled: Boolean = true,
) {
  val protectionScore: Int
    get() = listOf(
      true, // SHA-256 engine always active
      progressionSealValid,
      chatFilterEnabled,
      screenShieldEnabled,
      pinLockEnabled,
    ).count { it }

  val summaryLine: String
    get() = "Shields Active: $protectionScore/5 • Progression Seal: ${if (progressionSealValid) "VALID" else "WARN"}"
}

/**
 * Zero-cost, on-device app protection suite (Play Creator Track compliant):
 * - SHA-256 save & progression integrity sealing/verification
 * - Salted iterative SHA-256 App Lock PIN (4-6 digits)
 * - Chat & user-input sanitization
 */
object AppSecurityManager {

  private const val PREFS_NAME = "unknown_os_security_prefs"
  private const val KEY_PIN_HASH = "app_pin_hash"
  private const val KEY_PIN_SALT = "app_pin_salt"
  private const val PIN_HASH_ITERATIONS = 1000
  private const val SEAL_DOMAIN = "TUU_UNKNOWN_UNIVERSES_FREE_SHIELD_V1"

  private val blockedTermsRegex = Regex(
    pattern = "\\b(kill\\s*yourself|kys|nigger|faggot|retard|cunt)\\b",
    options = setOf(RegexOption.IGNORE_CASE),
  )

  private fun prefs(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun sha256Hex(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
      .digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  /**
   * Computes a deterministic integrity seal over any payload string.
   */
  fun signPayload(payload: String): String =
    sha256Hex("$SEAL_DOMAIN::$payload")

  fun verifyPayload(payload: String, expectedSeal: String?): Boolean {
    if (expectedSeal.isNullOrBlank()) return false
    return constantTimeEquals(signPayload(payload), expectedSeal.trim().lowercase())
  }

  /**
   * Computes a tamper-evident canonical seal for exported save JSON bundles.
   */
  fun computeSaveBundleSeal(
    callsign: String,
    totalEssence: Long,
    highScore: Long,
    highestWave: Long,
    armoryCount: Int,
    rigsCount: Int,
    matchLogsCount: Int,
    exportedAtMillis: Long,
  ): String {
    val canonical = listOf(
      SEAL_DOMAIN,
      callsign,
      totalEssence.toString(),
      highScore.toString(),
      highestWave.toString(),
      armoryCount.toString(),
      rigsCount.toString(),
      matchLogsCount.toString(),
      exportedAtMillis.toString(),
    ).joinToString("|")
    return sha256Hex(canonical)
  }

  fun verifySaveBundleSeal(
    seal: String?,
    callsign: String,
    totalEssence: Long,
    highScore: Long,
    highestWave: Long,
    armoryCount: Int,
    rigsCount: Int,
    matchLogsCount: Int,
    exportedAtMillis: Long,
  ): SaveIntegrityStatus {
    if (seal.isNullOrBlank()) return SaveIntegrityStatus.UNSEALED_LEGACY
    val expected = computeSaveBundleSeal(
      callsign = callsign,
      totalEssence = totalEssence,
      highScore = highScore,
      highestWave = highestWave,
      armoryCount = armoryCount,
      rigsCount = rigsCount,
      matchLogsCount = matchLogsCount,
      exportedAtMillis = exportedAtMillis,
    )
    return if (constantTimeEquals(expected, seal.trim().lowercase())) {
      SaveIntegrityStatus.VERIFIED
    } else {
      SaveIntegrityStatus.TAMPERED
    }
  }

  fun isPinEnabled(context: Context): Boolean {
    val p = prefs(context)
    return !p.getString(KEY_PIN_HASH, null).isNullOrBlank() &&
      !p.getString(KEY_PIN_SALT, null).isNullOrBlank()
  }

  fun setAppPin(context: Context, rawPin: String): Result<Unit> {
    val pin = rawPin.trim()
    if (pin.length !in 4..6 || !pin.all { it.isDigit() }) {
      return Result.failure(IllegalArgumentException("PIN must be 4 to 6 digits."))
    }
    val saltBytes = ByteArray(16)
    SecureRandom().nextBytes(saltBytes)
    val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
    val hash = derivePinHash(pin, saltHex)
    prefs(context).edit {
      putString(KEY_PIN_SALT, saltHex)
      putString(KEY_PIN_HASH, hash)
    }
    return Result.success(Unit)
  }

  fun verifyAppPin(context: Context, enteredPin: String): Boolean {
    val p = prefs(context)
    val salt = p.getString(KEY_PIN_SALT, null) ?: return true
    val storedHash = p.getString(KEY_PIN_HASH, null) ?: return true
    val candidate = enteredPin.trim()
    if (candidate.isEmpty()) return false
    return constantTimeEquals(derivePinHash(candidate, salt), storedHash)
  }

  fun clearAppPin(context: Context) {
    prefs(context).edit {
      remove(KEY_PIN_SALT)
      remove(KEY_PIN_HASH)
    }
  }

  /**
   * Strips control characters, HTML/script tags, and profanity/abuse tokens from chat & room inputs.
   */
  fun sanitizeChatText(
    raw: String,
    safetyFilterEnabled: Boolean = true,
    maxLength: Int = 240,
  ): String {
    val noControl = raw
      .replace(Regex("[\\p{Cntrl}&&[^\n\t]]"), "")
      .replace(Regex("<[^>]{1,64}>"), "")
      .replace(Regex("\\s+"), " ")
      .trim()
      .take(maxLength)
    if (!safetyFilterEnabled || noControl.isEmpty()) return noControl
    return blockedTermsRegex.replace(noControl) { m -> "*".repeat(m.value.length) }
  }

  fun sanitizeIdentifierLabel(raw: String, maxLength: Int = 36): String =
    sanitizeChatText(raw, safetyFilterEnabled = true, maxLength = maxLength)

  private fun derivePinHash(pin: String, saltHex: String): String {
    var acc = "$saltHex:$pin:$SEAL_DOMAIN"
    repeat(PIN_HASH_ITERATIONS) {
      acc = sha256Hex(acc)
    }
    return acc
  }

  private fun constantTimeEquals(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    var diff = 0
    for (i in a.indices) {
      diff = diff or (a[i].code xor b[i].code)
    }
    return diff == 0
  }
}

package com.example.ui.auth

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

data class BridgedExternalAccount(
  val providerId: String,
  val displayName: String,
  val externalSubject: String,
)

data class SteamResolvedProfile(
  val steamId64: String,
  val personaName: String,
)

data class ResolvedOAuthIdentity(
  val displayName: String,
  val subject: String,
)

object PlatformProfileParsers {

  fun decodeJwtClaims(jwt: String?): JSONObject? {
    if (jwt.isNullOrBlank()) return null
    return runCatching {
      val parts = jwt.split(".")
      if (parts.size < 2) return null
      val decoded = Base64.decode(
        parts[1],
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
      )
      JSONObject(String(decoded, Charsets.UTF_8))
    }.getOrNull()
  }

  fun parseEpicAccountDisplayNameJson(accountsJsonArray: String): String? {
    return runCatching {
      val arr = JSONArray(accountsJsonArray)
      if (arr.length() == 0) return null
      val obj = arr.optJSONObject(0) ?: return null
      obj.optString("displayName").takeIf { it.isNotBlank() }
    }.getOrNull()
  }

  fun parseEpicExternalAuthsJson(accountsJsonArray: String): List<BridgedExternalAccount> {
    val results = mutableListOf<BridgedExternalAccount>()
    val arr = JSONArray(accountsJsonArray)
    if (arr.length() == 0) return results
    val accountObj = arr.optJSONObject(0) ?: return results
    val externalAuths = accountObj.optJSONObject("externalAuths") ?: return results

    val keyMapping = mapOf(
      "psn" to "psn",
      "nintendo" to "nintendo",
      "ubisoft" to "ubisoft",
      "xbl" to "xbox",
      "steam" to "steam",
      "apple" to "apple",
    )

    keyMapping.forEach { (epicKey, mappedProviderId) ->
      val extObj = externalAuths.optJSONObject(epicKey)
      if (extObj != null) {
        val extId = extObj.optString("externalAuthId").ifBlank { extObj.optString("id") }
        val extDisplay = extObj.optString("externalDisplayName").ifBlank { extId }
        if (extId.isNotBlank() || extDisplay.isNotBlank()) {
          results.add(
            BridgedExternalAccount(
              providerId = mappedProviderId,
              displayName = "$extDisplay (via Epic)".take(64),
              externalSubject = extId.ifBlank { extDisplay }.take(128),
            )
          )
        }
      }
    }
    return results
  }

  fun parseAmazonProfileJson(rawJson: String): ResolvedOAuthIdentity? {
    return runCatching {
      val obj = JSONObject(rawJson)
      val subject = obj.optString("user_id").trim()
      val display = obj.optString("name")
        .ifBlank { obj.optString("email").substringBefore("@") }
        .ifBlank { subject }
        .trim()
      if (display.isBlank() && subject.isBlank()) null
      else ResolvedOAuthIdentity(
        displayName = display.take(64),
        subject = subject.ifBlank { display }.take(128),
      )
    }.getOrNull()
  }

  fun parseGogUserDataJson(rawJson: String): ResolvedOAuthIdentity? {
    return runCatching {
      val obj = JSONObject(rawJson)
      val isLoggedIn = obj.optBoolean("isLoggedIn", true)
      if (!isLoggedIn) return null
      val userId = obj.optString("userId").trim()
      val username = obj.optString("username")
        .ifBlank { obj.optString("email").substringBefore("@") }
        .ifBlank { userId }
        .trim()
      if (username.isBlank() && userId.isBlank()) null
      else ResolvedOAuthIdentity(
        displayName = username.take(64),
        subject = userId.ifBlank { username }.take(128),
      )
    }.getOrNull()
  }

  fun parseEaPidMeJson(rawJson: String): ResolvedOAuthIdentity? {
    return runCatching {
      val root = JSONObject(rawJson)
      val pidObj = root.optJSONObject("pid") ?: root
      val pidId = pidObj.optString("pidId").ifBlank { pidObj.optString("id") }.trim()
      val display = pidObj.optString("displayName")
        .ifBlank { pidObj.optString("email").substringBefore("@") }
        .ifBlank { pidId }
        .trim()
      if (display.isBlank() && pidId.isBlank()) null
      else ResolvedOAuthIdentity(
        displayName = display.take(64),
        subject = pidId.ifBlank { display }.take(128),
      )
    }.getOrNull()
  }

  fun buildSteamXmlUrl(rawInput: String): String? {
    val trimmed = rawInput.trim().trimEnd('/')
    if (trimmed.isEmpty()) return null
    val cleanPath = trimmed
      .removePrefix("https://")
      .removePrefix("http://")
    return when {
      cleanPath.startsWith("steamcommunity.com/profiles/") -> {
        val id = cleanPath.removePrefix("steamcommunity.com/profiles/")
          .substringBefore("/")
          .substringBefore("?")
        if (id.isNotBlank()) "https://steamcommunity.com/profiles/$id/?xml=1" else null
      }
      cleanPath.startsWith("steamcommunity.com/id/") -> {
        val vanity = cleanPath.removePrefix("steamcommunity.com/id/")
          .substringBefore("/")
          .substringBefore("?")
        if (vanity.isNotBlank()) {
          "https://steamcommunity.com/id/${URLEncoder.encode(vanity, "UTF-8")}/?xml=1"
        } else null
      }
      trimmed.all(Char::isDigit) && trimmed.length >= 15 ->
        "https://steamcommunity.com/profiles/$trimmed/?xml=1"
      trimmed.matches(Regex("^[a-zA-Z0-9_\\-\\.]{2,64}$")) ->
        "https://steamcommunity.com/id/${URLEncoder.encode(trimmed, "UTF-8")}/?xml=1"
      else -> null
    }
  }

  fun extractXmlTag(xml: String, tag: String): String? {
    val regex = Regex("<$tag>(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?<\\/$tag>", RegexOption.DOT_MATCHES_ALL)
    return regex.find(xml)?.groupValues?.getOrNull(1)?.trim()
  }
}

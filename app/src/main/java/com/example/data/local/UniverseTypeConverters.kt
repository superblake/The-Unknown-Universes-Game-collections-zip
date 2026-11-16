package com.example.data.local

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

class UniverseTypeConverters {

  @TypeConverter
  fun fromPoseAnglesMap(value: Map<String, Double>): String {
    val obj = JSONObject()
    value.forEach { (k, v) -> obj.put(k, v) }
    return obj.toString()
  }

  @TypeConverter
  fun toPoseAnglesMap(raw: String): Map<String, Double> {
    if (raw.isBlank()) return emptyMap()
    return try {
      val obj = JSONObject(raw)
      val out = linkedMapOf<String, Double>()
      val keys = obj.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        out[k] = obj.optDouble(k, 0.0)
      }
      out
    } catch (_: Exception) {
      emptyMap()
    }
  }

  @TypeConverter
  fun fromLongList(value: List<Long>): String {
    val arr = JSONArray()
    value.forEach { arr.put(it) }
    return arr.toString()
  }

  @TypeConverter
  fun toLongList(raw: String): List<Long> {
    if (raw.isBlank()) return emptyList()
    return try {
      val arr = JSONArray(raw)
      List(arr.length()) { idx -> arr.optLong(idx) }
    } catch (_: Exception) {
      emptyList()
    }
  }
}

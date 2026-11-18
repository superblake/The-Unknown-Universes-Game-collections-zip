package com.example.ui.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ArenaBackdropCache {
  private var cachedWorldId: String = ""
  private var cachedW: Float = -1f
  private var cachedH: Float = -1f
  private var cachedBrush: Brush? = null

  fun brushFor(worldId: String, skyThemeId: String, w: Float, h: Float): Brush {
    val key = "$worldId|$skyThemeId"
    val existing = cachedBrush
    if (existing != null && cachedWorldId == key && cachedW == w && cachedH == h) {
      return existing
    }
    val bgTop = when (skyThemeId) {
      "blood_moon" -> Color(0xFF2B0712)
      "cryo_tundra" -> Color(0xFF061E2C)
      "golden_nexus" -> Color(0xFF261D07)
      "neon_void" -> Color(0xFF17082E)
      else -> when (worldId) {
        "sector_zero" -> Color(0xFF0A121C)
        "blacklight_conduit" -> Color(0xFF100822)
        "hellbreaker_citadel" -> Color(0xFF1E0809)
        "chrono_vault" -> Color(0xFF081229)
        else -> Color(0xFF060C1D)
      }
    }
    val created = Brush.radialGradient(
      colors = listOf(bgTop, Color(0xFF05070E)),
      center = Offset(w * 0.5f, h * 0.5f),
      radius = maxOf(w, h).coerceAtLeast(1f) * 0.78f,
    )
    cachedWorldId = key
    cachedW = w
    cachedH = h
    cachedBrush = created
    return created
  }
}

fun DrawScope.drawWorldArenaBackdrop(
  worldId: String,
  accentColor: Color,
  secondaryColor: Color,
  elapsedTicks: Long,
  chevronsLocked: Int,
  overdriveActive: Boolean,
  backdropCache: ArenaBackdropCache? = null,
  stationReady: Boolean = false,
  showTacticalGrid: Boolean = true,
  reduceMotion: Boolean = false,
  reduceFlashes: Boolean = false,
  highContrast: Boolean = false,
  lowGraphics: Boolean = false,
  skyThemeId: String = "default_sector",
  gridStyleId: String = "tactical_square",
  floorAccentOverride: Color? = null,
) {
  val w = size.width
  val h = size.height
  val pulse = if (reduceMotion || reduceFlashes) 0.35f else (sin(elapsedTicks * 0.06f) + 1f) * 0.5f

  // Cached radial gradient brush (0 allocations per frame after first draw)
  if (highContrast) {
    drawRect(color = Color(0xFF020307))
  } else {
    val bgBrush = backdropCache?.brushFor(worldId, skyThemeId, w, h) ?: run {
      val bgTop = when (worldId) {
        "sector_zero" -> Color(0xFF0A121C)
        "blacklight_conduit" -> Color(0xFF100822)
        "hellbreaker_citadel" -> Color(0xFF1E0809)
        "chrono_vault" -> Color(0xFF081229)
        else -> Color(0xFF060C1D)
      }
      Brush.radialGradient(
        colors = listOf(bgTop, Color(0xFF05070E)),
        center = Offset(w * 0.5f, h * 0.5f),
        radius = maxOf(w, h).coerceAtLeast(1f) * 0.78f,
      )
    }
    drawRect(brush = bgBrush)
  }

  // Tactical sector grid (respects Level Customization gridStyleId)
  if (showTacticalGrid && gridStyleId != "minimal_clean") {
    val step = when {
      lowGraphics || gridStyleId == "hex_vault" -> 86f
      else -> 56f
    }
    val gridAlpha = if (overdriveActive && !reduceFlashes) 0.16f else 0.09f
    val gridColor = (floorAccentOverride ?: accentColor).copy(alpha = gridAlpha)
    var gx = 0f
    while (gx < w) {
      drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
      gx += step
    }
    var gy = 0f
    while (gy < h) {
      drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
      gy += step
    }
  }

  when (worldId) {
    "sector_zero" -> {
      if (!reduceMotion && !reduceFlashes) {
        val maxScan = minOf(w, h) * 0.45f
        val isacRadius = ((elapsedTicks % 180L) / 180f) * maxScan
        drawCircle(
          color = Color(0xFFFF8F00).copy(alpha = (1f - isacRadius / maxScan.coerceAtLeast(1f)) * 0.35f),
          radius = isacRadius.coerceAtLeast(10f),
          center = Offset(w * 0.5f, h * 0.5f),
          style = Stroke(width = 3f),
        )
      }
      drawRoundRect(
        color = Color(0xFF1A2433),
        topLeft = Offset(w * 0.10f, h * 0.22f),
        size = Size(w * 0.14f, h * 0.08f),
        cornerRadius = CornerRadius(8f, 8f),
      )
      drawRoundRect(
        color = Color(0xFFFFAB40).copy(alpha = 0.55f),
        topLeft = Offset(w * 0.10f, h * 0.22f),
        size = Size(w * 0.14f, h * 0.08f),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 2f),
      )
      val cachePos = Offset(w * 0.20f, h * 0.76f)
      drawCircle(Color(0xFF00E5FF).copy(alpha = (if (stationReady) 0.34f else 0.16f) + pulse * 0.2f), radius = 28f, center = cachePos)
      drawCircle(Color(0xFFFFD54F), radius = 17f, center = cachePos, style = Stroke(width = 3f))
      drawCircle(Color(0xFF00E5FF), radius = 8f, center = cachePos)
    }

    "blacklight_conduit" -> {
      val b1Pos = Offset(w * 0.14f, h * 0.24f)
      val b1Sz = Size(w * 0.16f, h * 0.14f)
      drawRoundRect(Color(0xFF161129), topLeft = b1Pos, size = b1Sz, cornerRadius = CornerRadius(10f, 10f))
      drawRoundRect(Color(0xFFFF3D71).copy(alpha = 0.55f), topLeft = b1Pos, size = b1Sz, cornerRadius = CornerRadius(10f, 10f), style = Stroke(width = 2f))

      val b2Pos = Offset(w * 0.70f, h * 0.24f)
      val b2Sz = Size(w * 0.16f, h * 0.14f)
      drawRoundRect(Color(0xFF0E1B2E), topLeft = b2Pos, size = b2Sz, cornerRadius = CornerRadius(10f, 10f))
      drawRoundRect(Color(0xFF00E5FF).copy(alpha = 0.55f), topLeft = b2Pos, size = b2Sz, cornerRadius = CornerRadius(10f, 10f), style = Stroke(width = 2f))

      if (!reduceMotion && !lowGraphics) {
        val wInt = w.toInt().coerceAtLeast(1)
        val hInt = h.toInt().coerceAtLeast(1)
        for (i in 0 until 14) {
          val rx = ((i * 97 + elapsedTicks * 4) % wInt).toFloat()
          val ry = ((i * 73 + elapsedTicks * 9) % hInt).toFloat()
          drawLine(Color(0x337C4DFF), Offset(rx, ry), Offset(rx - 8f, ry + 18f), strokeWidth = 1.5f)
        }
      }
      val pylon = Offset(w * 0.80f, h * 0.76f)
      drawCircle(Color(0xFF7C4DFF).copy(alpha = (if (stationReady) 0.36f else 0.22f) + pulse * 0.2f), radius = 28f, center = pylon)
      drawCircle(Color(0xFF00E5FF), radius = 15f, center = pylon, style = Stroke(width = 3f))
    }

    "hellbreaker_citadel" -> {
      val center = Offset(w * 0.5f, h * 0.5f)
      val r = minOf(w, h) * 0.30f
      drawCircle(Color(0xFFFF3D00).copy(alpha = 0.16f + pulse * 0.12f), radius = r, center = center, style = Stroke(width = 8f))
      drawCircle(Color(0xFFFFAB40).copy(alpha = 0.45f), radius = r * 0.74f, center = center, style = Stroke(width = 2.5f))
      for (k in 0 until 6) {
        val a = k * (PI.toFloat() / 3f) + 0.25f
        drawLine(
          color = Color(0x66FF5252),
          start = Offset(center.x + cos(a) * r * 0.74f, center.y + sin(a) * r * 0.74f),
          end = Offset(center.x + cos(a) * r * 1.45f, center.y + sin(a) * r * 1.45f),
          strokeWidth = 3f,
        )
      }
      val hydraVent = Offset(w * 0.20f, h * 0.76f)
      drawCircle(Color(0xFF00E5FF).copy(alpha = 0.22f + pulse * 0.15f), radius = 22f, center = hydraVent)
      drawCircle(Color(0xFFFF5252), radius = 12f, center = hydraVent, style = Stroke(width = 2.5f))
    }

    "chrono_vault" -> {
      val gateCenter = Offset(w * 0.5f, h * 0.30f)
      val gateRadius = minOf(w, h) * 0.18f
      val kawooshAlpha = if (chevronsLocked >= 7) 0.42f + pulse * 0.25f else 0.14f + pulse * 0.10f
      drawCircle(Color(0xFF448AFF).copy(alpha = kawooshAlpha), radius = gateRadius * 0.85f, center = gateCenter)
      if (chevronsLocked >= 7) {
        drawCircle(Color(0xFF00E5FF).copy(alpha = 0.55f), radius = gateRadius * (0.45f + pulse * 0.25f), center = gateCenter, style = Stroke(width = 3f))
      }
      drawCircle(Color(0xFF90A4AE), radius = gateRadius, center = gateCenter, style = Stroke(width = 7f))
      drawCircle(Color(0xFFFFD54F).copy(alpha = 0.5f), radius = gateRadius * 0.86f, center = gateCenter, style = Stroke(width = 2f))
      for (c in 0 until 7) {
        val angle = -PI.toFloat() * 0.5f + c * (2f * PI.toFloat() / 7f)
        val cp = Offset(gateCenter.x + cos(angle) * gateRadius, gateCenter.y + sin(angle) * gateRadius)
        val lit = c < chevronsLocked
        drawCircle(
          color = if (lit) Color(0xFFFFAB40) else Color(0xFF37474F),
          radius = if (lit) 7.5f else 5f,
          center = cp,
        )
      }
    }

    else -> {
      if (!lowGraphics) {
        val wInt = w.toInt().coerceAtLeast(1)
        val hInt = h.toInt().coerceAtLeast(1)
        for (s in 0 until 24) {
          val sx = ((s * 131 + 40) % wInt).toFloat()
          val sy = ((s * 89 + 25) % hInt).toFloat()
          drawCircle(Color.White.copy(alpha = 0.25f + (s % 3) * 0.18f), radius = 1.8f, center = Offset(sx, sy))
        }
      }
      val center = Offset(w * 0.5f, h * 0.5f)
      drawCircle(accentColor.copy(alpha = 0.22f + pulse * 0.14f), radius = minOf(w, h) * 0.28f, center = center, style = Stroke(width = 4f))
      drawCircle(secondaryColor.copy(alpha = 0.28f), radius = minOf(w, h) * 0.16f, center = center, style = Stroke(width = 2.5f))
      drawRoundRect(
        color = Color(0xFF141B2D),
        topLeft = Offset(w * 0.10f, h * 0.22f),
        size = Size(w * 0.14f, h * 0.08f),
        cornerRadius = CornerRadius(8f, 8f),
      )
      drawRoundRect(
        color = Color(0xFF00E5FF).copy(alpha = 0.5f),
        topLeft = Offset(w * 0.10f, h * 0.22f),
        size = Size(w * 0.14f, h * 0.08f),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 2f),
      )
    }
  }
}

fun formatRoundRoman(round: Long): String = when (round) {
  1L -> "I"
  2L -> "II"
  3L -> "III"
  4L -> "IV"
  5L -> "V"
  6L -> "VI"
  7L -> "VII"
  8L -> "VIII"
  9L -> "IX"
  10L -> "X"
  else -> round.toString()
}

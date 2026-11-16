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

fun DrawScope.drawWorldArenaBackdrop(
  worldId: String,
  accentColor: Color,
  secondaryColor: Color,
  elapsedTicks: Long,
  chevronsLocked: Int,
  overdriveActive: Boolean,
) {
  val w = size.width
  val h = size.height
  val pulse = (sin(elapsedTicks * 0.06f) + 1f) * 0.5f

  // Deep atmospheric floor gradient per universe
  val bgTop = when (worldId) {
    "sector_zero" -> Color(0xFF0A1018)
    "blacklight_conduit" -> Color(0xFF0D081C)
    "hellbreaker_citadel" -> Color(0xFF1A0708)
    "chrono_vault" -> Color(0xFF081024)
    else -> Color(0xFF060B1A)
  }
  drawRect(
    brush = Brush.radialGradient(
      colors = listOf(bgTop, Color(0xFF05070E)),
      center = Offset(w * 0.5f, h * 0.5f),
      radius = maxOf(w, h) * 0.75f,
    )
  )

  // Tactical sector grid
  val step = 56f
  val gridColor = accentColor.copy(alpha = if (overdriveActive) 0.16f else 0.08f)
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

  when (worldId) {
    "sector_zero" -> {
      // Division SHD orange scan ring + CoD Zombies quarantine bunker + Pack-a-Tier station
      val isacRadius = ((elapsedTicks % 180L) / 180f) * minOf(w, h) * 0.45f
      drawCircle(
        color = Color(0xFFFF8F00).copy(alpha = (1f - isacRadius / (minOf(w, h) * 0.45f)) * 0.35f),
        radius = isacRadius.coerceAtLeast(10f),
        center = Offset(w * 0.5f, h * 0.5f),
        style = Stroke(width = 3f),
      )
      // Quarantine bunker blocks
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
      // Pack-a-Tier / Mystery Cache terminal
      val cachePos = Offset(w * 0.20f, h * 0.76f)
      drawCircle(Color(0xFF00E5FF).copy(alpha = 0.2f + pulse * 0.2f), radius = 26f, center = cachePos)
      drawCircle(Color(0xFF00E5FF), radius = 16f, center = cachePos, style = Stroke(width = 3f))
    }

    "blacklight_conduit" -> {
      // Metro city rooftop blocks + Liberty rain streaks + Arc Relay Pylon
      val blocks = listOf(
        Offset(w * 0.14f, h * 0.24f) to Size(w * 0.16f, h * 0.14f),
        Offset(w * 0.70f, h * 0.24f) to Size(w * 0.16f, h * 0.14f),
      )
      blocks.forEach { (pos, sz) ->
        drawRoundRect(Color(0xFF161129), topLeft = pos, size = sz, cornerRadius = CornerRadius(10f, 10f))
        drawRoundRect(accentColor.copy(alpha = 0.45f), topLeft = pos, size = sz, cornerRadius = CornerRadius(10f, 10f), style = Stroke(width = 2f))
      }
      // Subtle angled rain streaks
      for (i in 0 until 14) {
        val rx = ((i * 97 + elapsedTicks * 4) % w.toInt().coerceAtLeast(1)).toFloat()
        val ry = ((i * 73 + elapsedTicks * 9) % h.toInt().coerceAtLeast(1)).toFloat()
        drawLine(Color(0x337C4DFF), Offset(rx, ry), Offset(rx - 8f, ry + 18f), strokeWidth = 1.5f)
      }
      val pylon = Offset(w * 0.80f, h * 0.76f)
      drawCircle(Color(0xFF7C4DFF).copy(alpha = 0.28f + pulse * 0.2f), radius = 26f, center = pylon)
      drawCircle(Color(0xFF00E5FF), radius = 14f, center = pylon, style = Stroke(width = 3f))
    }

    "hellbreaker_citadel" -> {
      // Crucible molten lava fissures + obsidian forge rune circle
      val center = Offset(w * 0.5f, h * 0.5f)
      val r = minOf(w, h) * 0.30f
      drawCircle(Color(0xFFFF3D00).copy(alpha = 0.16f + pulse * 0.12f), radius = r, center = center, style = Stroke(width = 8f))
      drawCircle(Color(0xFFFFAB40).copy(alpha = 0.45f), radius = r * 0.74f, center = center, style = Stroke(width = 2.5f))
      // Magma fissure lines radiating from center
      for (k in 0 until 6) {
        val a = k * (PI.toFloat() / 3f) + 0.25f
        drawLine(
          color = Color(0x66FF5252),
          start = Offset(center.x + cos(a) * r * 0.74f, center.y + sin(a) * r * 0.74f),
          end = Offset(center.x + cos(a) * r * 1.45f, center.y + sin(a) * r * 1.45f),
          strokeWidth = 3f,
        )
      }
    }

    "chrono_vault" -> {
      // Stargate 7-Chevron Ring + Temporal Event Horizon
      val gateCenter = Offset(w * 0.5f, h * 0.30f)
      val gateRadius = minOf(w, h) * 0.18f
      drawCircle(Color(0xFF448AFF).copy(alpha = 0.16f + pulse * 0.12f), radius = gateRadius * 0.85f, center = gateCenter)
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
      // Starforge Orbital Deck + Cosmic Starfield + Force Nexus Ring
      for (s in 0 until 24) {
        val sx = ((s * 131 + 40) % w.toInt().coerceAtLeast(1)).toFloat()
        val sy = ((s * 89 + 25) % h.toInt().coerceAtLeast(1)).toFloat()
        drawCircle(Color.White.copy(alpha = 0.25f + (s % 3) * 0.18f), radius = 1.8f, center = Offset(sx, sy))
      }
      val center = Offset(w * 0.5f, h * 0.5f)
      drawCircle(accentColor.copy(alpha = 0.22f + pulse * 0.14f), radius = minOf(w, h) * 0.28f, center = center, style = Stroke(width = 4f))
      drawCircle(secondaryColor.copy(alpha = 0.28f), radius = minOf(w, h) * 0.16f, center = center, style = Stroke(width = 2.5f))
    }
  }
}

fun formatRoundRoman(round: Long): String = when (round) {
  1L -> "I"
  2L -> "II"
  3L -> "III"
  4L -> "IV"
  5L -> "V"
  else -> round.toString()
}

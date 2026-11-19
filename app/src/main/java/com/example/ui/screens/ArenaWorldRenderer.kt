package com.example.ui.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.AreaAgeEra
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ArenaBackdropCache {
  private var cachedWorldId: String = ""
  private var cachedW: Float = -1f
  private var cachedH: Float = -1f
  private var cachedBrush: Brush? = null

  fun brushFor(worldId: String, skyThemeId: String, areaAgeEra: AreaAgeEra, w: Float, h: Float): Brush {
    val key = "$worldId|$skyThemeId|${areaAgeEra.name}"
    val existing = cachedBrush
    if (existing != null && cachedWorldId == key && cachedW == w && cachedH == h) {
      return existing
    }
    val bgTop = when (skyThemeId) {
      "blood_moon" -> Color(0xFF2B0712)
      "cryo_tundra" -> Color(0xFF061E2C)
      "golden_nexus" -> Color(0xFF261D07)
      "neon_void" -> Color(0xFF17082E)
      else -> when (areaAgeEra) {
        AreaAgeEra.MATURE -> Color(0xFF1B160B)
        AreaAgeEra.ANCIENT -> Color(0xFF071D15)
        AreaAgeEra.YOUNG -> when (worldId) {
          "sector_zero" -> Color(0xFF0A121C)
          "blacklight_conduit" -> Color(0xFF100822)
          "hellbreaker_citadel" -> Color(0xFF1E0809)
          "chrono_vault" -> Color(0xFF081229)
          "campaign_origins" -> Color(0xFF161329)
          else -> Color(0xFF060C1D)
        }
      }
    }
    val created = Brush.radialGradient(
      colors = listOf(bgTop, Color(0xFF05070E)),
      center = Offset(w * 0.5f, h * 0.5f),
      radius = maxOf(w, h).coerceAtLeast(1f) * 0.82f,
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
  camX: Float = 0.6f,
  camY: Float = 0.6f,
  areaAgeEra: AreaAgeEra = AreaAgeEra.YOUNG,
) {
  val w = size.width
  val h = size.height
  val pulse = if (reduceMotion || reduceFlashes) 0.35f else (sin(elapsedTicks * 0.06f) + 1f) * 0.5f

  if (highContrast) {
    drawRect(color = Color(0xFF020307))
  } else {
    val bgBrush = backdropCache?.brushFor(worldId, skyThemeId, areaAgeEra, w, h) ?: Brush.radialGradient(
      colors = listOf(Color(0xFF0A121C), Color(0xFF05070E)),
      center = Offset(w * 0.5f, h * 0.5f),
      radius = maxOf(w, h).coerceAtLeast(1f) * 0.8f,
    )
    drawRect(brush = bgBrush)
  }

  // Scrolling World-Locked Tactical Sector Grid (so player feels movement speed across larger 2.2x2.2 arena!)
  if (showTacticalGrid && gridStyleId != "minimal_clean") {
    val stepWorld = if (lowGraphics || gridStyleId == "hex_vault") 0.14f else 0.09f
    val stepPxX = stepWorld * w
    val stepPxY = stepWorld * h
    val offsetPxX = -((camX % stepWorld) * w)
    val offsetPxY = -((camY % stepWorld) * h)
    val gridAlpha = if (overdriveActive && !reduceFlashes) 0.18f else 0.10f
    val gridColor = (floorAccentOverride ?: accentColor).copy(alpha = gridAlpha)
    var gx = offsetPxX
    while (gx < w) {
      if (gx >= 0f) drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
      gx += stepPxX
    }
    var gy = offsetPxY
    while (gy < h) {
      if (gy >= 0f) drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
      gy += stepPxY
    }
  }

  // World Boundary Wall Frame
  val bLeft = (0.04f - camX) * w
  val bTop = (0.06f - camY) * h
  val bWidth = (ARENA_WORLD_SIZE - 0.08f) * w
  val bHeight = (ARENA_WORLD_SIZE - 0.12f) * h
  drawRect(
    color = (floorAccentOverride ?: accentColor).copy(alpha = 0.45f),
    topLeft = Offset(bLeft, bTop),
    size = Size(bWidth, bHeight),
    style = Stroke(width = 4f),
  )

  // Center World Landmark (Stargate / Pylon / Crucible / ISAC)
  val cx = (ARENA_CENTER - camX) * w
  val cy = (ARENA_CENTER - camY) * h
  when (worldId) {
    "chrono_vault" -> {
      val gateCenter = Offset(cx, cy - h * 0.16f)
      val gateRadius = minOf(w, h) * 0.16f
      val kawooshAlpha = if (chevronsLocked >= 7) 0.42f + pulse * 0.25f else 0.16f + pulse * 0.10f
      drawCircle(Color(0xFF448AFF).copy(alpha = kawooshAlpha), radius = gateRadius * 0.85f, center = gateCenter)
      drawCircle(Color(0xFF90A4AE), radius = gateRadius, center = gateCenter, style = Stroke(width = 7f))
      for (c in 0 until 7) {
        val angle = -PI.toFloat() * 0.5f + c * (2f * PI.toFloat() / 7f)
        val cp = Offset(gateCenter.x + cos(angle) * gateRadius, gateCenter.y + sin(angle) * gateRadius)
        drawCircle(
          color = if (c < chevronsLocked) Color(0xFFFFAB40) else Color(0xFF37474F),
          radius = if (c < chevronsLocked) 7f else 4.5f,
          center = cp,
        )
      }
    }
    "hellbreaker_citadel" -> {
      val center = Offset(cx, cy)
      val r = minOf(w, h) * 0.24f
      drawCircle(Color(0xFFFF3D00).copy(alpha = 0.16f + pulse * 0.12f), radius = r, center = center, style = Stroke(width = 6f))
      drawCircle(Color(0xFFFFAB40).copy(alpha = 0.45f), radius = r * 0.72f, center = center, style = Stroke(width = 2.5f))
    }
    "campaign_origins" -> {
      val center = Offset(cx, cy)
      val r = minOf(w, h) * 0.26f
      drawCircle(Color(0xFFFFD54F).copy(alpha = 0.24f + pulse * 0.16f), radius = r, center = center, style = Stroke(width = 5f))
      drawCircle(Color(0xFF00E5FF).copy(alpha = 0.42f), radius = r * 0.72f, center = center, style = Stroke(width = 3f))
      drawCircle(Color(0xFFE040FB).copy(alpha = 0.35f + pulse * 0.2f), radius = r * 0.44f, center = center, style = Stroke(width = 2.5f))
      drawCircle(Color(0xFF00E676).copy(alpha = if (stationReady) 0.55f else 0.28f), radius = 16f, center = center)
    }
    else -> {
      val center = Offset(cx, cy)
      drawCircle(accentColor.copy(alpha = 0.18f + pulse * 0.12f), radius = minOf(w, h) * 0.20f, center = center, style = Stroke(width = 3.5f))
      drawCircle(secondaryColor.copy(alpha = (if (stationReady) 0.36f else 0.2f)), radius = 14f, center = center)
    }
  }
}

fun DrawScope.drawBuildingsAndTraps(
  sim: ArenaSimState,
  camX: Float,
  camY: Float,
  glowOn: Boolean,
  pulse: Float,
) {
  val w = size.width
  val h = size.height

  // 1. Explorable Buildings & Structures with Doors and Corner Turrets
  for (i in 0 until sim.buildings.size) {
    val b = sim.buildings[i]
    val sx = (b.x - camX) * w
    val sy = (b.y - camY) * h
    val sw = b.width * w
    val sh = b.height * h
    if (sx + sw < -40f || sx > w + 40f || sy + sh < -40f || sy > h + 40f) continue

    val fillCol = if (b.explored) Color(0xFF10242D).copy(alpha = 0.85f) else Color(0xFF131A2C).copy(alpha = 0.90f)
    val strokeCol = if (b.explored) Color(0xFF00E676) else b.accentColor
    drawRoundRect(
      color = fillCol,
      topLeft = Offset(sx, sy),
      size = Size(sw, sh),
      cornerRadius = CornerRadius(12f, 12f),
    )
    drawRoundRect(
      color = strokeCol.copy(alpha = 0.8f),
      topLeft = Offset(sx, sy),
      size = Size(sw, sh),
      cornerRadius = CornerRadius(12f, 12f),
      style = Stroke(width = if (b.explored) 3f else 2.2f),
    )
    // Open entrance doorway at bottom edge of building
    drawRect(
      color = strokeCol,
      topLeft = Offset(sx + sw * 0.36f, sy + sh - 4f),
      size = Size(sw * 0.28f, 7f),
    )
    // Interior Reactor/Turret Node
    val center = Offset(sx + sw * 0.5f, sy + sh * 0.5f)
    if (glowOn) drawCircle(strokeCol.copy(alpha = 0.18f + pulse * 0.12f), radius = 18f, center = center)
    drawCircle(strokeCol, radius = 6.5f, center = center)
  }

  // 2. Electric & Fire Traps placed around arenas
  for (i in 0 until sim.traps.size) {
    val trap = sim.traps[i]
    val tx = (trap.x - camX) * w
    val ty = (trap.y - camY) * h
    val radPx = trap.radius * minOf(w, h)
    if (tx + radPx < -30f || tx - radPx > w + 30f || ty + radPx < -30f || ty - radPx > h + 30f) continue

    val baseCol = trap.kind.color
    val activeAlpha = if (trap.activeTicks > 0f) 0.36f else if (trap.cooldownTicks > 0f) 0.10f else 0.22f
    drawCircle(baseCol.copy(alpha = activeAlpha), radius = radPx * 0.72f, center = Offset(tx, ty))
    drawCircle(baseCol.copy(alpha = 0.85f), radius = radPx * 0.72f, center = Offset(tx, ty), style = Stroke(width = 2.2f))
    if (trap.kind == ArenaTrapKind.ELECTRIC) {
      // Lightning zigzag coil
      drawLine(Color.White, Offset(tx - 9f, ty - 7f), Offset(tx + 4f, ty), strokeWidth = 2.5f)
      drawLine(baseCol, Offset(tx + 4f, ty), Offset(tx - 5f, ty + 9f), strokeWidth = 2.5f)
    } else {
      // Fire flame emblem
      drawCircle(Color(0xFFFFD54F), radius = 7f + pulse * 3f, center = Offset(tx, ty))
      drawCircle(baseCol, radius = 13f, center = Offset(tx, ty), style = Stroke(width = 2.2f))
    }
  }
}

fun DrawScope.drawVacuumAndCollectibles(
  sim: ArenaSimState,
  camX: Float,
  camY: Float,
  glowOn: Boolean,
  pulse: Float,
) {
  val w = size.width
  val h = size.height

  // Collectibles (Blue/Red/Yellow/Green/Combined Orbs, Blast Shards, Perk Drinks, Colored Blast Cores)
  for (i in 0 until sim.collectibles.size) {
    val c = sim.collectibles[i]
    val pos = Offset((c.x - camX) * w, (c.y - camY) * h)
    if (pos.x < -25f || pos.x > w + 25f || pos.y < -25f || pos.y > h + 25f) continue
    if (glowOn) drawCircle(c.color.copy(alpha = 0.25f + pulse * 0.2f), radius = 14f, center = pos)
    when (c.kind) {
      ArenaCollectibleKind.BLAST_SHARD -> {
        drawRect(c.color, topLeft = Offset(pos.x - 5f, pos.y - 8f), size = Size(10f, 16f))
        drawRect(Color.White, topLeft = Offset(pos.x - 5f, pos.y - 8f), size = Size(10f, 16f), style = Stroke(width = 1.6f))
      }
      ArenaCollectibleKind.PERK_DRINK -> {
        drawRoundRect(c.color, topLeft = Offset(pos.x - 6f, pos.y - 9f), size = Size(12f, 18f), cornerRadius = CornerRadius(4f, 4f))
        drawCircle(Color.White, radius = 3f, center = pos)
      }
      ArenaCollectibleKind.BLAST_CORE -> {
        drawCircle(c.color, radius = 9f, center = pos, style = Stroke(width = 3f))
        drawCircle(Color.White, radius = 4f, center = pos)
      }
      else -> {
        drawCircle(c.color, radius = 7f, center = pos)
        drawCircle(Color.White.copy(alpha = 0.9f), radius = 2.6f, center = pos)
      }
    }
  }

  // Vacuum Bot Drone Orbiting Player + Magnetic Suction Ring
  val vx = (sim.vacuumBotX - camX) * w
  val vy = (sim.vacuumBotY - camY) * h
  val botPos = Offset(vx, vy)
  val vacCol = Color(0xFF00E676)
  if (glowOn) drawCircle(vacCol.copy(alpha = 0.22f + pulse * 0.14f), radius = 16f, center = botPos)
  drawCircle(Color(0xFF0C1A24), radius = 9.5f, center = botPos)
  drawCircle(vacCol, radius = 9.5f, center = botPos, style = Stroke(width = 2.5f))
  drawCircle(Color(0xFF00E5FF), radius = 3.5f, center = botPos)
}

fun formatRoundRoman(round: Long): String {
  if (round <= 0L) return "0"
  if (round > 399L) return round.toString()
  val values = intArrayOf(100, 90, 50, 40, 10, 9, 5, 4, 1)
  val symbols = arrayOf("C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
  var rem = round.toInt()
  val sb = StringBuilder()
  for (i in values.indices) {
    while (rem >= values[i]) {
      rem -= values[i]
      sb.append(symbols[i])
    }
  }
  return sb.toString()
}

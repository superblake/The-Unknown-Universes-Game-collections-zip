package com.example.ui.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.AreaAgeEra
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val HORIZON_RATIO = 0.14f
private const val PERSP_TILT = 0.24f

fun perspectiveScaleForRelY(relY: Float): Float =
  (0.78f + (relY - 0.5f) * PERSP_TILT).coerceIn(0.46f, 1.32f)

fun worldToPerspectiveScreen(
  wx: Float,
  wy: Float,
  camX: Float,
  camY: Float,
  w: Float,
  h: Float,
): Offset {
  val relX = wx - camX
  val relY = wy - camY
  val depthScale = perspectiveScaleForRelY(relY)
  val horizonPx = h * HORIZON_RATIO
  val groundH = h - horizonPx
  val sx = w * 0.5f + (relX - 0.5f) * w * depthScale
  val normalizedY = ((relY - 0.5f) * depthScale + 0.5f)
  val sy = horizonPx + normalizedY * groundH
  return Offset(sx, sy)
}

fun screenToWorldCoords(
  px: Float,
  py: Float,
  camX: Float,
  camY: Float,
  w: Float,
  h: Float,
): Pair<Float, Float> {
  val horizonPx = h * HORIZON_RATIO
  val groundH = (h - horizonPx).coerceAtLeast(1f)
  val normY = (py - horizonPx) / groundH
  var relY = normY
  for (it in 0..2) {
    val ds = perspectiveScaleForRelY(relY)
    relY = 0.5f + (normY - 0.5f) / ds.coerceAtLeast(0.25f)
  }
  val ds = perspectiveScaleForRelY(relY).coerceAtLeast(0.25f)
  val relX = 0.5f + (px - w * 0.5f) / (w.coerceAtLeast(1f) * ds)
  return Pair(camX + relX, camY + relY)
}

class ArenaBackdropCache {
  private var cachedWorldId: String = ""
  private var cachedW: Float = -1f
  private var cachedH: Float = -1f
  private var cachedBrush: Brush? = null
  private var cachedSkyBrush: Brush? = null

  fun brushFor(
    worldId: String,
    skyThemeId: String,
    areaAgeEra: AreaAgeEra,
    w: Float,
    h: Float,
    areaId: String = "",
    floorAccentHex: Long = 0L,
  ): Brush {
    val key = "$worldId|$areaId|$skyThemeId|${areaAgeEra.name}|$floorAccentHex"
    val existing = cachedBrush
    if (existing != null && cachedWorldId == key && cachedW == w && cachedH == h) {
      return existing
    }
    val bgTop = topTintFor(worldId, skyThemeId, areaAgeEra)
    val created = Brush.radialGradient(
      colors = listOf(bgTop, Color(0xFF04060D)),
      center = Offset(w * 0.5f, h * 0.54f),
      radius = maxOf(w, h).coerceAtLeast(1f) * 0.86f,
    )
    val horizonCol = bgTop.copy(alpha = 0.92f)
    cachedSkyBrush = Brush.verticalGradient(
      colors = listOf(Color(0xFF02040A), horizonCol, Color(0xFF0C1426)),
      startY = 0f,
      endY = (h * HORIZON_RATIO).coerceAtLeast(10f),
    )
    cachedWorldId = key
    cachedW = w
    cachedH = h
    cachedBrush = created
    return created
  }

  fun skyBrushFor(
    worldId: String,
    skyThemeId: String,
    areaAgeEra: AreaAgeEra,
    w: Float,
    h: Float,
    areaId: String = "",
    floorAccentHex: Long = 0L,
  ): Brush {
    brushFor(worldId, skyThemeId, areaAgeEra, w, h, areaId, floorAccentHex)
    return cachedSkyBrush ?: Brush.verticalGradient(listOf(Color(0xFF02040A), Color(0xFF0C1426)))
  }

  private fun topTintFor(worldId: String, skyThemeId: String, areaAgeEra: AreaAgeEra): Color = when (skyThemeId) {
    "blood_moon" -> Color(0xFF2B0712)
    "cryo_tundra" -> Color(0xFF061E2C)
    "golden_nexus" -> Color(0xFF261D07)
    "neon_void" -> Color(0xFF17082E)
    else -> when (areaAgeEra) {
      AreaAgeEra.MATURE -> Color(0xFF1B160B)
      AreaAgeEra.ANCIENT -> Color(0xFF071D15)
      AreaAgeEra.YOUNG -> when (worldId) {
        "sector_zero" -> Color(0xFF0A141E)
        "blacklight_conduit" -> Color(0xFF130826)
        "hellbreaker_citadel" -> Color(0xFF22080A)
        "chrono_vault" -> Color(0xFF091632)
        "gotham_arkham" -> Color(0xFF0D111E)
        "spiderverse_queens" -> Color(0xFF1E0A16)
        "stark_orbital" -> Color(0xFF210912)
        "empire_city_infamous" -> Color(0xFF071B2A)
        "ship_of_heroes_apotheosis" -> Color(0xFF160B2E)
        "mr_crossover_nexus" -> Color(0xFF081F1A)
        "campaign_origins" -> Color(0xFF181430)
        else -> Color(0xFF081024)
      }
    }
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
  camX: Float = ARENA_CENTER - 0.5f,
  camY: Float = ARENA_CENTER - 0.5f,
  areaAgeEra: AreaAgeEra = AreaAgeEra.YOUNG,
  areaId: String = "",
) {
  val w = size.width
  val h = size.height
  val horizonY = h * HORIZON_RATIO
  val pulse = if (reduceMotion || reduceFlashes) 0.35f else (sin(elapsedTicks * 0.06f) + 1f) * 0.5f
  val effAccent = floorAccentOverride ?: accentColor
  val outerFrontier = (camX + 0.5f) < 0f || (camX + 0.5f) > ARENA_WORLD_SIZE || (camY + 0.5f) < 0f || (camY + 0.5f) > ARENA_WORLD_SIZE
  val floorHexLong = floorAccentOverride?.value?.toLong() ?: 0L

  if (highContrast) {
    drawRect(color = Color(0xFF020307))
  } else {
    val bgBrush = backdropCache?.brushFor(worldId, skyThemeId, areaAgeEra, w, h, areaId, floorHexLong) ?: Brush.radialGradient(
      colors = listOf(Color(0xFF0A121C), Color(0xFF04060D)),
      center = Offset(w * 0.5f, h * 0.54f),
      radius = maxOf(w, h).coerceAtLeast(1f) * 0.86f,
    )
    drawRect(brush = bgBrush)

    // 2.5D Atmospheric Horizon & Parallax Skyline Silhouette
    val skyBrush = backdropCache?.skyBrushFor(worldId, skyThemeId, areaAgeEra, w, h, areaId, floorHexLong)
      ?: Brush.verticalGradient(listOf(Color(0xFF02040A), effAccent.copy(alpha = 0.25f)))
    drawRect(brush = skyBrush, topLeft = Offset.Zero, size = Size(w, horizonY))

    if (!lowGraphics) {
      val parallaxShift = ((camX * 28f) % w)
      val silColor = Color(0xFF050812)
      val rimColor = (if (outerFrontier) Color(0xFFE040FB) else effAccent).copy(alpha = 0.38f)
      for (tIdx in 0 until 14) {
        val bx = ((tIdx * (w / 10f) - parallaxShift + w * 2f) % (w * 1.4f)) - w * 0.2f
        val towerW = (w * 0.06f) + ((tIdx % 3) * w * 0.025f)
        val towerH = horizonY * (0.28f + ((tIdx * 37) % 55) / 100f)
        drawRect(silColor, topLeft = Offset(bx, horizonY - towerH), size = Size(towerW, towerH))
        drawRect(rimColor, topLeft = Offset(bx, horizonY - towerH), size = Size(towerW, 2.5f))
      }
      // Horizon moon/rift glow
      val sunX = (w * 0.72f - (camX * 8f)) .coerceIn(w * 0.15f, w * 0.85f)
      drawCircle(effAccent.copy(alpha = 0.18f + pulse * 0.08f), radius = horizonY * 0.42f, center = Offset(sunX, horizonY * 0.52f))
    }
    drawLine(effAccent.copy(alpha = 0.55f), Offset(0f, horizonY), Offset(w, horizonY), strokeWidth = 2.5f)
  }

  // Domain-Specific Ground Arteries projected into 2.5D Perspective (Highways only on urban worlds; Bulkheads/Causeways elsewhere)
  if (!lowGraphics) {
    val isUrbanRoadWorld = worldId in setOf(
      "sector_zero",
      "spiderverse_queens",
      "empire_city_infamous",
      "blacklight_conduit",
      "campaign_origins",
      "mr_crossover_nexus",
    )
    val isOrbitalDeckWorld = worldId in setOf(
      "stark_orbital",
      "aegis_starblade",
      "ship_of_heroes_apotheosis",
    )
    val roadCol = when {
      isOrbitalDeckWorld -> Color(0xFF142238).copy(alpha = 0.72f)
      isUrbanRoadWorld -> Color(0xFF0F1624).copy(alpha = 0.78f)
      else -> Color(0xFF1A1624).copy(alpha = 0.74f)
    }
    val dashCol = when {
      outerFrontier -> Color(0xFFE040FB).copy(alpha = 0.34f)
      isOrbitalDeckWorld -> Color(0xFF00E5FF).copy(alpha = 0.36f)
      isUrbanRoadWorld -> Color(0xFFFFD54F).copy(alpha = 0.30f)
      else -> effAccent.copy(alpha = 0.32f)
    }
    val vRoads = floatArrayOf(2.4f, ARENA_CENTER, 7.6f)
    for (rx in vRoads) {
      val topP = worldToPerspectiveScreen(rx, camY - 0.15f, camX, camY, w, h)
      val botP = worldToPerspectiveScreen(rx, camY + 1.15f, camX, camY, w, h)
      if (maxOf(topP.x, botP.x) > -80f && minOf(topP.x, botP.x) < w + 80f) {
        drawLine(roadCol, topP, botP, strokeWidth = if (isOrbitalDeckWorld) 28f else 34f)
        if (isUrbanRoadWorld) {
          drawLine(dashCol, topP, botP, strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))
        } else {
          drawLine(dashCol, topP, botP, strokeWidth = 2.2f)
        }
      }
    }
    val hRoads = floatArrayOf(2.4f, ARENA_CENTER, 7.6f)
    for (ry in hRoads) {
      val leftP = worldToPerspectiveScreen(camX - 0.25f, ry, camX, camY, w, h)
      val rightP = worldToPerspectiveScreen(camX + 1.25f, ry, camX, camY, w, h)
      if (maxOf(leftP.y, rightP.y) > horizonY - 20f && minOf(leftP.y, rightP.y) < h + 40f) {
        val roadW = (if (isOrbitalDeckWorld) 24f else 28f) * perspectiveScaleForRelY(ry - camY)
        drawLine(roadCol, leftP, rightP, strokeWidth = roadW)
        if (isUrbanRoadWorld) {
          drawLine(dashCol, leftP, rightP, strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))
        } else {
          drawLine(dashCol, leftP, rightP, strokeWidth = 2f)
        }
      }
    }
  }

  // Foreshortened Perspective Tactical Sector Grid
  if (showTacticalGrid && gridStyleId != "minimal_clean") {
    val stepWorld = if (lowGraphics || gridStyleId == "hex_vault") 0.22f else 0.14f
    val gridAlpha = if (overdriveActive && !reduceFlashes) 0.22f else 0.12f
    val gridColor = (if (outerFrontier) Color(0xFFE040FB) else effAccent).copy(alpha = gridAlpha)
    val startWX = ((camX - 0.35f) / stepWorld).toInt() * stepWorld
    val endWX = camX + 1.35f
    var wx = startWX
    while (wx <= endWX) {
      val pTop = worldToPerspectiveScreen(wx, camY - 0.10f, camX, camY, w, h)
      val pBot = worldToPerspectiveScreen(wx, camY + 1.15f, camX, camY, w, h)
      drawLine(gridColor, pTop, pBot, strokeWidth = 1f)
      wx += stepWorld
    }
    val startWY = ((camY - 0.10f) / stepWorld).toInt() * stepWorld
    val endWY = camY + 1.15f
    var wy = startWY
    while (wy <= endWY) {
      val pLeft = worldToPerspectiveScreen(camX - 0.35f, wy, camX, camY, w, h)
      val pRight = worldToPerspectiveScreen(camX + 1.35f, wy, camX, camY, w, h)
      if (pLeft.y >= horizonY - 12f) {
        drawLine(gridColor, pLeft, pRight, strokeWidth = 1f)
      }
      wy += stepWorld
    }
  }

  // Verdansk 10x10 Frontier Threshold (Dashed Passable Threshold — NOT a closed barrier!)
  val fTL = worldToPerspectiveScreen(0f, 0f, camX, camY, w, h)
  val fTR = worldToPerspectiveScreen(ARENA_WORLD_SIZE, 0f, camX, camY, w, h)
  val fBR = worldToPerspectiveScreen(ARENA_WORLD_SIZE, ARENA_WORLD_SIZE, camX, camY, w, h)
  val fBL = worldToPerspectiveScreen(0f, ARENA_WORLD_SIZE, camX, camY, w, h)
  val frontierStroke = Color(0xFFE040FB).copy(alpha = 0.55f)
  val dash = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))
  if (fTL.y in -40f..h + 40f || fTR.y in -40f..h + 40f) {
    drawLine(frontierStroke, fTL, fTR, strokeWidth = 3f, pathEffect = dash)
  }
  if (fBL.y in -40f..h + 40f || fBR.y in -40f..h + 40f) {
    drawLine(frontierStroke, fBL, fBR, strokeWidth = 3f, pathEffect = dash)
  }
  if (fTL.x in -40f..w + 40f || fBL.x in -40f..w + 40f) {
    drawLine(frontierStroke, fTL, fBL, strokeWidth = 3f, pathEffect = dash)
  }
  if (fTR.x in -40f..w + 40f || fBR.x in -40f..w + 40f) {
    drawLine(frontierStroke, fTR, fBR, strokeWidth = 3f, pathEffect = dash)
  }

  // Center World Landmark projected in 2.5D
  val cCenter = worldToPerspectiveScreen(ARENA_CENTER, ARENA_CENTER, camX, camY, w, h)
  val cScale = perspectiveScaleForRelY(ARENA_CENTER - camY)
  if (cCenter.x in -240f..w + 240f && cCenter.y in -240f..h + 240f) {
    when (worldId) {
      "chrono_vault" -> {
        val gateCenter = Offset(cCenter.x, cCenter.y - h * 0.12f * cScale)
        val gateRadius = minOf(w, h) * 0.16f * cScale
        val kawooshAlpha = if (chevronsLocked >= 7) 0.42f + pulse * 0.25f else 0.16f + pulse * 0.10f
        drawCircle(Color(0xFF448AFF).copy(alpha = kawooshAlpha), radius = gateRadius * 0.85f, center = gateCenter)
        drawCircle(Color(0xFF90A4AE), radius = gateRadius, center = gateCenter, style = Stroke(width = 7f * cScale))
        for (c in 0 until 7) {
          val angle = -PI.toFloat() * 0.5f + c * (2f * PI.toFloat() / 7f)
          val cp = Offset(gateCenter.x + cos(angle) * gateRadius, gateCenter.y + sin(angle) * gateRadius)
          drawCircle(
            color = if (c < chevronsLocked) Color(0xFFFFAB40) else Color(0xFF37474F),
            radius = (if (c < chevronsLocked) 7f else 4.5f) * cScale,
            center = cp,
          )
        }
      }
      "hellbreaker_citadel" -> {
        val r = minOf(w, h) * 0.22f * cScale
        drawOval(Color(0xFFFF3D00).copy(alpha = 0.20f + pulse * 0.12f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.65f), size = Size(r * 2f, r * 1.3f), style = Stroke(width = 6f * cScale))
        drawOval(Color(0xFFFFAB40).copy(alpha = 0.45f), topLeft = Offset(cCenter.x - r * 0.7f, cCenter.y - r * 0.45f), size = Size(r * 1.4f, r * 0.9f), style = Stroke(width = 2.5f))
      }
      "gotham_arkham" -> {
        val r = minOf(w, h) * 0.21f * cScale
        drawOval(Color(0xFFFFD54F).copy(alpha = 0.20f + pulse * 0.14f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.62f), size = Size(r * 2f, r * 1.24f))
        drawOval(Color(0xFFFFD54F).copy(alpha = 0.80f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.62f), size = Size(r * 2f, r * 1.24f), style = Stroke(width = 3.5f * cScale))
        drawOval(Color(0xFF080A12), topLeft = Offset(cCenter.x - r * 0.55f, cCenter.y - r * 0.24f), size = Size(r * 1.1f, r * 0.48f))
      }
      "spiderverse_queens" -> {
        val r = minOf(w, h) * 0.22f * cScale
        for (sp in 0 until 8) {
          val a = sp * (PI.toFloat() / 4f)
          drawLine(Color(0xFFE53935).copy(alpha = 0.55f), cCenter, Offset(cCenter.x + cos(a) * r, cCenter.y + sin(a) * r * 0.65f), strokeWidth = 2.4f * cScale)
        }
        drawOval(Color(0xFF2979FF).copy(alpha = 0.65f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.65f), size = Size(r * 2f, r * 1.3f), style = Stroke(width = 3f * cScale))
        drawOval(Color(0xFFE53935).copy(alpha = 0.72f), topLeft = Offset(cCenter.x - r * 0.5f, cCenter.y - r * 0.32f), size = Size(r, r * 0.64f), style = Stroke(width = 2.5f * cScale))
      }
      "stark_orbital" -> {
        val r = minOf(w, h) * 0.20f * cScale
        drawCircle(Color(0xFF00E5FF).copy(alpha = 0.24f + pulse * 0.16f), radius = r * 0.85f, center = cCenter)
        drawCircle(Color(0xFFFFD54F), radius = r, center = cCenter, style = Stroke(width = 4f * cScale))
        drawCircle(Color(0xFF00E5FF), radius = r * 0.55f, center = cCenter, style = Stroke(width = 3f * cScale))
      }
      "sector_zero" -> {
        val r = minOf(w, h) * 0.20f * cScale
        drawOval(Color(0xFFFF8F00).copy(alpha = 0.22f + pulse * 0.12f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.64f), size = Size(r * 2f, r * 1.28f), style = Stroke(width = 4f * cScale))
        drawLine(Color(0xFF00E5FF).copy(alpha = 0.65f), Offset(cCenter.x - r * 0.45f, cCenter.y - r * 0.28f), Offset(cCenter.x - r * 0.45f, cCenter.y + r * 0.28f), strokeWidth = 3f * cScale)
        drawLine(Color(0xFF00E5FF).copy(alpha = 0.65f), Offset(cCenter.x + r * 0.45f, cCenter.y - r * 0.28f), Offset(cCenter.x + r * 0.45f, cCenter.y + r * 0.28f), strokeWidth = 3f * cScale)
        drawLine(Color(0xFF00E5FF).copy(alpha = 0.65f), Offset(cCenter.x - r * 0.45f, cCenter.y), Offset(cCenter.x + r * 0.45f, cCenter.y), strokeWidth = 3f * cScale)
      }
      "blacklight_conduit" -> {
        val r = minOf(w, h) * 0.21f * cScale
        drawOval(Color(0xFFFF3D71).copy(alpha = 0.20f + pulse * 0.14f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.62f), size = Size(r * 2f, r * 1.24f), style = Stroke(width = 4f * cScale))
        drawOval(Color(0xFF7C4DFF).copy(alpha = 0.65f), topLeft = Offset(cCenter.x - r * 0.55f, cCenter.y - r * 0.35f), size = Size(r * 1.1f, r * 0.7f), style = Stroke(width = 3f * cScale))
      }
      "aegis_starblade" -> {
        val r = minOf(w, h) * 0.20f * cScale
        drawOval(Color(0xFF00E5FF).copy(alpha = 0.30f + pulse * 0.14f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.60f), size = Size(r * 2f, r * 1.20f), style = Stroke(width = 3.5f * cScale))
        drawLine(Color(0xFFE040FB).copy(alpha = 0.75f), Offset(cCenter.x - r * 0.65f, cCenter.y - r * 0.3f), Offset(cCenter.x + r * 0.65f, cCenter.y + r * 0.3f), strokeWidth = 3f * cScale)
        drawLine(Color(0xFF00E5FF).copy(alpha = 0.75f), Offset(cCenter.x + r * 0.65f, cCenter.y - r * 0.3f), Offset(cCenter.x - r * 0.65f, cCenter.y + r * 0.3f), strokeWidth = 3f * cScale)
      }
      "empire_city_infamous" -> {
        val r = minOf(w, h) * 0.21f * cScale
        drawCircle(Color(0xFF00E5FF).copy(alpha = 0.20f + pulse * 0.16f), radius = r * 0.75f, center = cCenter)
        drawOval(Color(0xFFFFD54F).copy(alpha = 0.78f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.62f), size = Size(r * 2f, r * 1.24f), style = Stroke(width = 3.5f * cScale))
      }
      "ship_of_heroes_apotheosis" -> {
        val r = minOf(w, h) * 0.22f * cScale
        drawOval(Color(0xFFE040FB).copy(alpha = 0.22f + pulse * 0.14f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.65f), size = Size(r * 2f, r * 1.3f), style = Stroke(width = 4f * cScale))
        drawCircle(Color(0xFF00E5FF).copy(alpha = 0.55f), radius = r * 0.35f, center = cCenter, style = Stroke(width = 3f * cScale))
      }
      "campaign_origins", "mr_crossover_nexus" -> {
        val r = minOf(w, h) * 0.24f * cScale
        drawOval(Color(0xFFFFD54F).copy(alpha = 0.24f + pulse * 0.16f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.65f), size = Size(r * 2f, r * 1.3f), style = Stroke(width = 5f * cScale))
        drawOval(Color(0xFF00E5FF).copy(alpha = 0.42f), topLeft = Offset(cCenter.x - r * 0.72f, cCenter.y - r * 0.46f), size = Size(r * 1.44f, r * 0.92f), style = Stroke(width = 3f))
        drawCircle(Color(0xFF00E676).copy(alpha = if (stationReady) 0.55f else 0.28f), radius = 16f * cScale, center = cCenter)
      }
      else -> {
        val r = minOf(w, h) * 0.18f * cScale
        drawOval(effAccent.copy(alpha = 0.22f + pulse * 0.12f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.65f), size = Size(r * 2f, r * 1.3f), style = Stroke(width = 3.5f * cScale))
        drawCircle(secondaryColor.copy(alpha = if (stationReady) 0.36f else 0.2f), radius = 14f * cScale, center = cCenter)
      }
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

  // 1. Elemental Hazard Zones (Ice, Fire, Toxic, Electric, Windy)
  for (i in 0 until sim.hazards.size) {
    val hz = sim.hazards[i]
    val p = worldToPerspectiveScreen(hz.x, hz.y, camX, camY, w, h)
    val scale = perspectiveScaleForRelY(hz.y - camY)
    val rx = hz.radius * w * 0.46f * scale
    val ry = rx * 0.62f
    if (p.x + rx < -40f || p.x - rx > w + 40f || p.y + ry < -40f || p.y - ry > h + 40f) continue

    val col = hz.kind.color
    drawOval(col.copy(alpha = 0.16f + pulse * 0.08f), topLeft = Offset(p.x - rx, p.y - ry), size = Size(rx * 2f, ry * 2f))
    drawOval(col.copy(alpha = 0.85f), topLeft = Offset(p.x - rx, p.y - ry), size = Size(rx * 2f, ry * 2f), style = Stroke(width = 2.6f * scale))
    when (hz.kind) {
      ArenaHazardKind.ICE -> {
        drawLine(Color.White, Offset(p.x - 14f * scale, p.y), Offset(p.x + 14f * scale, p.y), strokeWidth = 2.2f)
        drawLine(Color.White, Offset(p.x, p.y - 10f * scale), Offset(p.x, p.y + 10f * scale), strokeWidth = 2.2f)
      }
      ArenaHazardKind.WINDY -> {
        for (l in -1..1) {
          drawLine(col, Offset(p.x - rx * 0.5f, p.y + l * 10f * scale), Offset(p.x + rx * 0.5f, p.y + l * 10f * scale - 6f * scale), strokeWidth = 2.4f, cap = StrokeCap.Round)
        }
      }
      else -> {
        if (glowOn) drawCircle(col.copy(alpha = 0.25f + pulse * 0.15f), radius = 14f * scale, center = p)
      }
    }
  }

  // 2. Safe Rest Camps (Take a break + x3 Shield Regeneration)
  for (i in 0 until sim.restCamps.size) {
    val camp = sim.restCamps[i]
    val p = worldToPerspectiveScreen(camp.x, camp.y, camX, camY, w, h)
    val scale = perspectiveScaleForRelY(camp.y - camY)
    val r = camp.radius * w * 0.52f * scale
    if (p.x + r < -40f || p.x - r > w + 40f || p.y + r < -40f || p.y - r > h + 40f) continue
    drawOval(Color(0xFF00E676).copy(alpha = 0.14f + pulse * 0.08f), topLeft = Offset(p.x - r, p.y - r * 0.65f), size = Size(r * 2f, r * 1.3f))
    drawOval(Color(0xFF00E676).copy(alpha = 0.85f), topLeft = Offset(p.x - r, p.y - r * 0.65f), size = Size(r * 2f, r * 1.3f), style = Stroke(width = 2.4f * scale))
    // Tent triangle + Warm Campfire
    val tentPath = Path().apply {
      moveTo(p.x - 16f * scale, p.y + 4f * scale)
      lineTo(p.x, p.y - 20f * scale)
      lineTo(p.x + 16f * scale, p.y + 4f * scale)
      close()
    }
    drawPath(tentPath, color = Color(0xFF1A3A2E))
    drawPath(tentPath, color = Color(0xFF00E676), style = Stroke(width = 2.2f * scale))
    drawCircle(Color(0xFFFF9100), radius = (5.5f + pulse * 2f) * scale, center = Offset(p.x + 18f * scale, p.y + 6f * scale))
  }

  // 3. 3D Extruded Buildings with Rooftop Bevel, Windows & Interior Nodes
  for (i in 0 until sim.buildings.size) {
    val b = sim.buildings[i]
    val tl = worldToPerspectiveScreen(b.x, b.y, camX, camY, w, h)
    val br = worldToPerspectiveScreen(b.x + b.width, b.y + b.height, camX, camY, w, h)
    val sw = (br.x - tl.x).coerceAtLeast(18f)
    val sh = (br.y - tl.y).coerceAtLeast(14f)
    if (tl.x + sw < -60f || tl.x > w + 60f || tl.y + sh < -60f || tl.y > h + 60f) continue

    val midScale = perspectiveScaleForRelY((b.y + b.height * 0.5f) - camY)
    val extrudeH = (26f * midScale).coerceAtLeast(12f)
    val strokeCol = if (b.explored) Color(0xFF00E676) else b.accentColor

    // Drop Shadow
    drawRoundRect(
      color = Color.Black.copy(alpha = 0.45f),
      topLeft = Offset(tl.x + 8f, tl.y + 8f),
      size = Size(sw, sh),
      cornerRadius = CornerRadius(10f, 10f),
    )
    // Lower Wall Facade
    drawRoundRect(
      color = Color(0xFF0D1322),
      topLeft = Offset(tl.x, tl.y),
      size = Size(sw, sh),
      cornerRadius = CornerRadius(8f, 8f),
    )
    // Illuminated architectural windows on front wall
    val winY = tl.y + sh - 10f
    for (win in 0..2) {
      drawRect(
        color = strokeCol.copy(alpha = 0.55f),
        topLeft = Offset(tl.x + sw * (0.14f + win * 0.30f), winY),
        size = Size(sw * 0.12f, 5f),
      )
    }
    // Extruded 3D Roof Deck shifted up by extrudeH
    val roofTL = Offset(tl.x + 2f, tl.y - extrudeH)
    drawRoundRect(
      color = if (b.explored) Color(0xFF132D36).copy(alpha = 0.92f) else Color(0xFF17223B).copy(alpha = 0.92f),
      topLeft = roofTL,
      size = Size(sw - 4f, sh * 0.85f),
      cornerRadius = CornerRadius(10f, 10f),
    )
    drawRoundRect(
      color = strokeCol.copy(alpha = 0.88f),
      topLeft = roofTL,
      size = Size(sw - 4f, sh * 0.85f),
      cornerRadius = CornerRadius(10f, 10f),
      style = Stroke(width = if (b.explored) 3f else 2.2f),
    )
    // Vertical front pillars connecting roof to foundation
    drawLine(strokeCol.copy(alpha = 0.65f), Offset(tl.x, tl.y + sh), Offset(roofTL.x, roofTL.y + sh * 0.85f), strokeWidth = 2f)
    drawLine(strokeCol.copy(alpha = 0.65f), Offset(tl.x + sw, tl.y + sh), Offset(roofTL.x + sw - 4f, roofTL.y + sh * 0.85f), strokeWidth = 2f)

    // Doorway & Rooftop Reactor Node
    drawRect(color = strokeCol, topLeft = Offset(tl.x + sw * 0.38f, tl.y + sh - 4f), size = Size(sw * 0.24f, 6f))
    val center = Offset(roofTL.x + (sw - 4f) * 0.5f, roofTL.y + sh * 0.42f)
    if (glowOn) drawCircle(strokeCol.copy(alpha = 0.20f + pulse * 0.12f), radius = 16f * midScale, center = center)
    drawCircle(strokeCol, radius = 6f * midScale, center = center)
  }

  // 4. Electric & Fire Traps projected in 2.5D
  for (i in 0 until sim.traps.size) {
    val trap = sim.traps[i]
    val p = worldToPerspectiveScreen(trap.x, trap.y, camX, camY, w, h)
    val scale = perspectiveScaleForRelY(trap.y - camY)
    val radPx = trap.radius * minOf(w, h) * scale * 0.72f
    if (p.x + radPx < -30f || p.x - radPx > w + 30f || p.y + radPx < -30f || p.y - radPx > h + 30f) continue

    val baseCol = trap.kind.color
    val activeAlpha = if (trap.activeTicks > 0f) 0.36f else if (trap.cooldownTicks > 0f) 0.10f else 0.22f
    drawOval(baseCol.copy(alpha = activeAlpha), topLeft = Offset(p.x - radPx, p.y - radPx * 0.68f), size = Size(radPx * 2f, radPx * 1.36f))
    drawOval(baseCol.copy(alpha = 0.85f), topLeft = Offset(p.x - radPx, p.y - radPx * 0.68f), size = Size(radPx * 2f, radPx * 1.36f), style = Stroke(width = 2.2f * scale))
    if (trap.kind == ArenaTrapKind.ELECTRIC) {
      drawLine(Color.White, Offset(p.x - 9f * scale, p.y - 7f * scale), Offset(p.x + 4f * scale, p.y), strokeWidth = 2.5f * scale)
      drawLine(baseCol, Offset(p.x + 4f * scale, p.y), Offset(p.x - 5f * scale, p.y + 9f * scale), strokeWidth = 2.5f * scale)
    } else {
      drawCircle(Color(0xFFFFD54F), radius = (6.5f + pulse * 3f) * scale, center = p)
      drawCircle(baseCol, radius = 12f * scale, center = p, style = Stroke(width = 2.2f * scale))
    }
  }

  // 5. Interactive Pack-a-Punch, Perk Machines & Ammo Crates
  for (i in 0 until sim.worldStations.size) {
    val st = sim.worldStations[i]
    val p = worldToPerspectiveScreen(st.x, st.y, camX, camY, w, h)
    val scale = perspectiveScaleForRelY(st.y - camY)
    if (p.x < -50f || p.x > w + 50f || p.y < -50f || p.y > h + 50f) continue
    val stCol = st.kind.color
    val ready = st.cooldownTicks <= 0f
    if (glowOn && ready) drawCircle(stCol.copy(alpha = 0.25f + pulse * 0.18f), radius = 20f * scale, center = p)
    val boxW = 24f * scale
    val boxH = when (st.kind) {
      ArenaStationKind.PACK_A_PUNCH -> 30f * scale
      ArenaStationKind.PERK_MACHINE -> 32f * scale
      ArenaStationKind.AMMO_CRATE -> 16f * scale
    }
    drawRoundRect(
      color = Color(0xFF101829),
      topLeft = Offset(p.x - boxW * 0.5f, p.y - boxH),
      size = Size(boxW, boxH),
      cornerRadius = CornerRadius(5f, 5f),
    )
    drawRoundRect(
      color = if (ready) stCol else stCol.copy(alpha = 0.35f),
      topLeft = Offset(p.x - boxW * 0.5f, p.y - boxH),
      size = Size(boxW, boxH),
      cornerRadius = CornerRadius(5f, 5f),
      style = Stroke(width = 2.6f * scale),
    )
    drawCircle(Color.White, radius = 4f * scale, center = Offset(p.x, p.y - boxH * 0.5f))
  }

  // 6. Rarity-Colored 3D Loot Crates (higher rarity inside Hazards & Outer Frontier!)
  for (i in 0 until sim.lootCrates.size) {
    val crate = sim.lootCrates[i]
    if (crate.opened) continue
    val p = worldToPerspectiveScreen(crate.x, crate.y, camX, camY, w, h)
    val scale = perspectiveScaleForRelY(crate.y - camY)
    if (p.x < -40f || p.x > w + 40f || p.y < -40f || p.y > h + 40f) continue
    val cCol = Color(crate.rarity.colorHex)
    if (crate.fromHazard || crate.rarity.rank >= 3) {
      drawLine(cCol.copy(alpha = 0.38f + pulse * 0.22f), Offset(p.x, p.y - 6f * scale), Offset(p.x, p.y - 54f * scale), strokeWidth = 5f * scale, cap = StrokeCap.Round)
    }
    drawRoundRect(
      color = Color(0xFF151C2C),
      topLeft = Offset(p.x - 13f * scale, p.y - 12f * scale),
      size = Size(26f * scale, 16f * scale),
      cornerRadius = CornerRadius(4f, 4f),
    )
    drawRoundRect(
      color = cCol,
      topLeft = Offset(p.x - 13f * scale, p.y - 12f * scale),
      size = Size(26f * scale, 16f * scale),
      cornerRadius = CornerRadius(4f, 4f),
      style = Stroke(width = 2.5f * scale),
    )
    drawLine(Color(0xFFFFD54F), Offset(p.x - 13f * scale, p.y - 5f * scale), Offset(p.x + 13f * scale, p.y - 5f * scale), strokeWidth = 2f * scale)
  }

  // 7. Hidden Cat Easter Eggs
  for (i in 0 until sim.catEasterEggs.size) {
    val cat = sim.catEasterEggs[i]
    if (cat.found) continue
    val p = worldToPerspectiveScreen(cat.x, cat.y, camX, camY, w, h)
    val scale = perspectiveScaleForRelY(cat.y - camY)
    if (p.x < -30f || p.x > w + 30f || p.y < -30f || p.y > h + 30f) continue
    val gold = Color(0xFFFFD54F)
    if (glowOn) drawCircle(gold.copy(alpha = 0.22f + pulse * 0.16f), radius = 15f * scale, center = p)
    // Cat ears
    val earL = Path().apply {
      moveTo(p.x - 7f * scale, p.y - 2f * scale)
      lineTo(p.x - 9f * scale, p.y - 12f * scale)
      lineTo(p.x - 2f * scale, p.y - 6f * scale)
      close()
    }
    val earR = Path().apply {
      moveTo(p.x + 7f * scale, p.y - 2f * scale)
      lineTo(p.x + 9f * scale, p.y - 12f * scale)
      lineTo(p.x + 2f * scale, p.y - 6f * scale)
      close()
    }
    drawPath(earL, color = gold)
    drawPath(earR, color = gold)
    drawCircle(gold, radius = 7.5f * scale, center = p)
    drawCircle(Color(0xFF0A1220), radius = 1.6f * scale, center = Offset(p.x - 2.6f * scale, p.y - 1f * scale))
    drawCircle(Color(0xFF0A1220), radius = 1.6f * scale, center = Offset(p.x + 2.6f * scale, p.y - 1f * scale))
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

  // Collectibles (Orbs, Blast Shards, Perk Drinks, Blast Cores, Character Shards, Cat Easter Eggs)
  for (i in 0 until sim.collectibles.size) {
    val c = sim.collectibles[i]
    val pos = worldToPerspectiveScreen(c.x, c.y, camX, camY, w, h)
    val scale = perspectiveScaleForRelY(c.y - camY)
    if (pos.x < -30f || pos.x > w + 30f || pos.y < -30f || pos.y > h + 30f) continue
    if (glowOn) drawCircle(c.color.copy(alpha = 0.25f + pulse * 0.2f), radius = 14f * scale, center = pos)
    when (c.kind) {
      ArenaCollectibleKind.BLAST_SHARD -> {
        drawRect(c.color, topLeft = Offset(pos.x - 5f * scale, pos.y - 8f * scale), size = Size(10f * scale, 16f * scale))
        drawRect(Color.White, topLeft = Offset(pos.x - 5f * scale, pos.y - 8f * scale), size = Size(10f * scale, 16f * scale), style = Stroke(width = 1.6f * scale))
      }
      ArenaCollectibleKind.CHARACTER_SHARD -> {
        val diamond = Path().apply {
          moveTo(pos.x, pos.y - 11f * scale)
          lineTo(pos.x + 8f * scale, pos.y)
          lineTo(pos.x, pos.y + 11f * scale)
          lineTo(pos.x - 8f * scale, pos.y)
          close()
        }
        drawPath(diamond, color = c.color)
        drawPath(diamond, color = Color.White, style = Stroke(width = 1.8f * scale))
      }
      ArenaCollectibleKind.PERK_DRINK -> {
        drawRoundRect(c.color, topLeft = Offset(pos.x - 6f * scale, pos.y - 9f * scale), size = Size(12f * scale, 18f * scale), cornerRadius = CornerRadius(4f, 4f))
        drawCircle(Color.White, radius = 3f * scale, center = pos)
      }
      ArenaCollectibleKind.BLAST_CORE, ArenaCollectibleKind.CAT_EASTER_EGG -> {
        drawCircle(c.color, radius = 9f * scale, center = pos, style = Stroke(width = 3f * scale))
        drawCircle(Color.White, radius = 4f * scale, center = pos)
      }
      else -> {
        drawCircle(c.color, radius = 7f * scale, center = pos)
        drawCircle(Color.White.copy(alpha = 0.9f), radius = 2.6f * scale, center = pos)
      }
    }
  }

  // Vacuum Bot Drone Orbiting Player
  val botPos = worldToPerspectiveScreen(sim.vacuumBotX, sim.vacuumBotY, camX, camY, w, h)
  val botScale = perspectiveScaleForRelY(sim.vacuumBotY - camY)
  val vacCol = Color(0xFF00E676)
  if (glowOn) drawCircle(vacCol.copy(alpha = 0.22f + pulse * 0.14f), radius = 16f * botScale, center = botPos)
  drawCircle(Color(0xFF0C1A24), radius = 9.5f * botScale, center = botPos)
  drawCircle(vacCol, radius = 9.5f * botScale, center = botPos, style = Stroke(width = 2.5f * botScale))
  drawCircle(Color(0xFF00E5FF), radius = 3.5f * botScale, center = botPos)
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

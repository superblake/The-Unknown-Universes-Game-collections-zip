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
import com.example.data.local.RamGraphicsMode
import com.example.data.model.AreaAgeEra
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val HORIZON_RATIO = 0.14f
private const val PERSP_TILT = 0.24f
private const val BASE_WORLD_VIEW_ZOOM = 0.64f
private const val PERSP_Y_FORESHORTEN = 0.78f

@Volatile
var activeArenaZoomScale: Float = 1.0f

@Volatile
var activeArenaResolutionScale: Float = 1.0f

fun currentWorldViewZoom(): Float =
  (BASE_WORLD_VIEW_ZOOM * (if (activeArenaZoomScale.isFinite()) activeArenaZoomScale else 1.0f))
    .coerceIn(0.48f, 0.85f)

fun canvasDensityScale(w: Float, h: Float): Float {
  val shortSide = minOf(w, h).coerceAtLeast(1f)
  val viewportScale = (shortSide / 720f).coerceIn(0.68f, 2.40f)
  val modeScale = if (activeArenaResolutionScale.isFinite()) activeArenaResolutionScale.coerceIn(0.75f, 1.35f) else 1.0f
  return (viewportScale * modeScale).coerceIn(0.65f, 2.60f)
}

fun worldAxisPx(w: Float, h: Float): Float {
  val groundH = (h * (1f - HORIZON_RATIO)).coerceAtLeast(1f)
  return minOf(w * 1.02f, groundH * 1.24f).coerceAtLeast(1f)
}

fun perspectiveScaleForRelY(relY: Float): Float =
  (0.78f + (relY - 0.5f) * PERSP_TILT).coerceIn(0.46f, 1.32f)

fun perspectiveScaleForRelY(relY: Float, w: Float, h: Float): Float =
  perspectiveScaleForRelY(relY) * canvasDensityScale(w, h)

fun worldToPerspectiveScreen(
  wx: Float,
  wy: Float,
  camX: Float,
  camY: Float,
  w: Float,
  h: Float,
): Offset {
  val zoom = currentWorldViewZoom()
  val relX = wx - camX
  val relY = wy - camY
  val depthScale = perspectiveScaleForRelY(relY)
  val horizonPx = h * HORIZON_RATIO
  val groundH = (h - horizonPx).coerceAtLeast(1f)
  val axis = worldAxisPx(w, h)
  val sx = w * 0.5f + (relX - 0.5f) * axis * depthScale * zoom
  val sy = horizonPx + groundH * 0.5f + (relY - 0.5f) * (axis * PERSP_Y_FORESHORTEN) * depthScale * zoom
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
  val zoom = currentWorldViewZoom()
  val horizonPx = h * HORIZON_RATIO
  val groundH = (h - horizonPx).coerceAtLeast(1f)
  val axis = worldAxisPx(w, h)
  val normY = (py - (horizonPx + groundH * 0.5f)) / (axis * PERSP_Y_FORESHORTEN).coerceAtLeast(1f)
  var relY = 0.5f + normY / zoom
  for (it in 0..2) {
    val ds = perspectiveScaleForRelY(relY)
    relY = 0.5f + normY / (ds * zoom).coerceAtLeast(0.20f)
  }
  val ds = perspectiveScaleForRelY(relY).coerceAtLeast(0.25f)
  val relX = 0.5f + (px - w * 0.5f) / (axis * ds * zoom).coerceAtLeast(1f)
  return Pair(camX + relX, camY + relY)
}

class ArenaBackdropCache {
  private data class CachedEntry(
    val bgBrush: Brush,
    val skyBrush: Brush,
  )

  private val lruCache = object : LinkedHashMap<String, CachedEntry>(16, 0.75f, true) {}
  private var maxCacheEntries: Int = 4

  fun configure(ramShaderCacheEnabled: Boolean, ramBudgetMb: Int) {
    val target = when {
      !ramShaderCacheEnabled -> 0
      ramBudgetMb >= 768 -> 16
      ramBudgetMb >= 512 -> 8
      ramBudgetMb >= 256 -> 4
      else -> 2
    }
    maxCacheEntries = target
    while (lruCache.size > maxCacheEntries && lruCache.isNotEmpty()) {
      val firstKey = lruCache.keys.firstOrNull() ?: break
      lruCache.remove(firstKey)
    }
  }

  private fun getOrBuild(
    worldId: String,
    skyThemeId: String,
    areaAgeEra: AreaAgeEra,
    w: Float,
    h: Float,
    areaId: String,
    floorAccentHex: Long,
  ): CachedEntry {
    val safeW = if (w.isFinite()) w.coerceAtLeast(1f) else 1f
    val safeH = if (h.isFinite()) h.coerceAtLeast(1f) else 1f
    val key = "$worldId|$areaId|$skyThemeId|${areaAgeEra.name}|$floorAccentHex|${safeW.toInt()}x${safeH.toInt()}"
    if (maxCacheEntries > 0) {
      lruCache[key]?.let { return it }
    }
    val bgTop = topTintFor(worldId, skyThemeId, areaAgeEra)
    val bgBrush = Brush.radialGradient(
      colors = listOf(bgTop, Color(0xFF04060D)),
      center = Offset(safeW * 0.5f, safeH * 0.54f),
      radius = maxOf(safeW, safeH).coerceAtLeast(1f) * 0.86f,
    )
    val horizonCol = bgTop.copy(alpha = 0.92f)
    val skyBrush = Brush.verticalGradient(
      colors = listOf(Color(0xFF02040A), horizonCol, Color(0xFF0C1426)),
      startY = 0f,
      endY = (safeH * HORIZON_RATIO).coerceAtLeast(10f),
    )
    val entry = CachedEntry(bgBrush, skyBrush)
    if (maxCacheEntries > 0) {
      lruCache[key] = entry
      while (lruCache.size > maxCacheEntries && lruCache.isNotEmpty()) {
        val firstKey = lruCache.keys.firstOrNull() ?: break
        lruCache.remove(firstKey)
      }
    }
    return entry
  }

  fun brushFor(
    worldId: String,
    skyThemeId: String,
    areaAgeEra: AreaAgeEra,
    w: Float,
    h: Float,
    areaId: String = "",
    floorAccentHex: Long = 0L,
  ): Brush = getOrBuild(worldId, skyThemeId, areaAgeEra, w, h, areaId, floorAccentHex).bgBrush

  fun skyBrushFor(
    worldId: String,
    skyThemeId: String,
    areaAgeEra: AreaAgeEra,
    w: Float,
    h: Float,
    areaId: String = "",
    floorAccentHex: Long = 0L,
  ): Brush = getOrBuild(worldId, skyThemeId, areaAgeEra, w, h, areaId, floorAccentHex).skyBrush

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
        "sector_star_wars_nexus" -> Color(0xFF1F1608)
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
  ramGraphicsMode: RamGraphicsMode = RamGraphicsMode.BALANCED,
  ramGraphicsBudgetMb: Int = 256,
  ramShaderCacheEnabled: Boolean = true,
) {
  val w = size.width
  val h = size.height
  if (!w.isFinite() || !h.isFinite() || w <= 1f || h <= 1f) return
  backdropCache?.configure(ramShaderCacheEnabled, ramGraphicsBudgetMb)
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
      val towerCount = when (ramGraphicsMode) {
        RamGraphicsMode.LOW_RAM_BOOST -> 6
        RamGraphicsMode.BALANCED -> 14
        RamGraphicsMode.HIGH_RAM_DETAIL -> 18
        RamGraphicsMode.MAX_RAM_ULTRA -> 22
      }
      for (tIdx in 0 until towerCount) {
        val bx = ((tIdx * (w / 10f) - parallaxShift + w * 2f) % (w * 1.4f)) - w * 0.2f
        val towerW = (w * 0.06f) + ((tIdx % 3) * w * 0.025f)
        val towerH = horizonY * (0.28f + ((tIdx * 37) % 55) / 100f)
        drawRect(silColor, topLeft = Offset(bx, horizonY - towerH), size = Size(towerW, towerH))
        drawRect(rimColor, topLeft = Offset(bx, horizonY - towerH), size = Size(towerW, 2.5f))
      }
      // Horizon moon/rift glow
      val sunX = (w * 0.72f - (camX * 8f)).coerceIn(w * 0.15f, w * 0.85f)
      if (ramGraphicsMode == RamGraphicsMode.HIGH_RAM_DETAIL || ramGraphicsMode == RamGraphicsMode.MAX_RAM_ULTRA) {
        drawCircle(effAccent.copy(alpha = 0.10f + pulse * 0.06f), radius = horizonY * 0.65f, center = Offset(sunX, horizonY * 0.52f))
      }
      drawCircle(effAccent.copy(alpha = 0.18f + pulse * 0.08f), radius = horizonY * 0.42f, center = Offset(sunX, horizonY * 0.52f))
    }
    drawLine(effAccent.copy(alpha = 0.55f), Offset(0f, horizonY), Offset(w, horizonY), strokeWidth = 2.5f)
  }

  // Domain-Specific Ground Arteries projected into 2.5D Perspective (Highways only on urban worlds; Bulkheads/Causeways elsewhere)
  if (!lowGraphics) {
    val isAbyssMap = areaId.contains("abyss")
    val isUrbanRoadWorld = !isAbyssMap && (worldId in setOf(
      "sector_zero",
      "spiderverse_queens",
      "empire_city_infamous",
      "blacklight_conduit",
      "campaign_origins",
      "mr_crossover_nexus",
    ) || areaId.contains("gta4") || areaId.contains("superman") || areaId.contains("arkham"))
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
    val vRoads = floatArrayOf(4.0f, ARENA_CENTER, 20.0f)
    for (rx in vRoads) {
      val topP = worldToPerspectiveScreen(rx, camY - 0.25f, camX, camY, w, h)
      val botP = worldToPerspectiveScreen(rx, camY + 1.35f, camX, camY, w, h)
      if (maxOf(topP.x, botP.x) > -80f && minOf(topP.x, botP.x) < w + 80f) {
        drawLine(roadCol, topP, botP, strokeWidth = if (isOrbitalDeckWorld) 26f else 32f)
        if (isUrbanRoadWorld) {
          drawLine(dashCol, topP, botP, strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))
        } else {
          drawLine(dashCol, topP, botP, strokeWidth = 2.2f)
        }
      }
    }
    val hRoads = floatArrayOf(4.0f, ARENA_CENTER, 20.0f)
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
    val stepWorld = when {
      lowGraphics || ramGraphicsMode == RamGraphicsMode.LOW_RAM_BOOST -> 0.28f
      gridStyleId == "hex_vault" -> 0.22f
      ramGraphicsMode == RamGraphicsMode.MAX_RAM_ULTRA -> 0.12f
      else -> 0.14f
    }
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

  // Fortified 24x24 Perimeter Border Wall, Bastion Buttresses, Searchlight Watchtowers & Shield Pylons
  val fTL = worldToPerspectiveScreen(0f, 0f, camX, camY, w, h)
  val fTR = worldToPerspectiveScreen(ARENA_WORLD_SIZE, 0f, camX, camY, w, h)
  val fBR = worldToPerspectiveScreen(ARENA_WORLD_SIZE, ARENA_WORLD_SIZE, camX, camY, w, h)
  val fBL = worldToPerspectiveScreen(0f, ARENA_WORLD_SIZE, camX, camY, w, h)
  val outerWallGlow = (if (highContrast) Color.White else Color(0xFFFF3D71)).copy(alpha = 0.34f + pulse * 0.16f)
  val outerWallCore = if (highContrast) Color(0xFFFFD54F) else Color(0xFFE040FB)
  fun drawPerimeterWallWithStructures(wx1: Float, wy1: Float, wx2: Float, wy2: Float, isHorizontal: Boolean) {
    val a = worldToPerspectiveScreen(wx1, wy1, camX, camY, w, h)
    val b = worldToPerspectiveScreen(wx2, wy2, camX, camY, w, h)
    if ((a.x < -140f && b.x < -140f) || (a.x > w + 140f && b.x > w + 140f)) return
    if ((a.y < -140f && b.y < -140f) || (a.y > h + 140f && b.y > h + 140f)) return
    // Armored lower parapet shadow + barrier core
    drawLine(Color(0xFF090D1A), a, b, strokeWidth = 22f, cap = StrokeCap.Square)
    drawLine(outerWallGlow, a, b, strokeWidth = 12f, cap = StrokeCap.Square)
    drawLine(outerWallCore, a, b, strokeWidth = 3.5f, cap = StrokeCap.Square)
    if (!lowGraphics) {
      // Build architectural buttress bunkers & energy shield pylons along the boundary every 3.0 world units
      var step = 3.0f
      while (step < ARENA_WORLD_SIZE - 0.5f) {
        val bx = if (isHorizontal) step else wx1
        val by = if (isHorizontal) wy1 else step
        if (kotlin.math.abs(bx - (camX + 0.5f)) < 6.5f && kotlin.math.abs(by - (camY + 0.5f)) < 6.5f) {
          val p = worldToPerspectiveScreen(bx, by, camX, camY, w, h)
          val pScale = perspectiveScaleForRelY(by - camY, w, h)
          if (p.x in -60f..w + 60f && p.y in horizonY - 40f..h + 60f) {
            val bw = 20f * pScale
            val bh = 16f * pScale
            drawRoundRect(
              color = Color(0xFF141B2E),
              topLeft = Offset(p.x - bw * 0.5f, p.y - bh * 0.65f),
              size = Size(bw, bh),
              cornerRadius = CornerRadius(4f, 4f),
            )
            drawRoundRect(
              color = outerWallCore.copy(alpha = 0.85f),
              topLeft = Offset(p.x - bw * 0.5f, p.y - bh * 0.65f),
              size = Size(bw, bh),
              cornerRadius = CornerRadius(4f, 4f),
              style = Stroke(width = 2.2f * pScale),
            )
            // Vertical shield pylon mast + glowing beacon tip
            drawLine(
              color = outerWallCore,
              start = Offset(p.x, p.y - bh * 0.5f),
              end = Offset(p.x, p.y - bh * 1.45f),
              strokeWidth = 3f * pScale,
            )
            drawCircle(
              color = Color(0xFFFFD54F).copy(alpha = 0.72f + pulse * 0.25f),
              radius = 3.8f * pScale,
              center = Offset(p.x, p.y - bh * 1.45f),
            )
          }
        }
        step += 3.0f
      }
    }
  }
  drawPerimeterWallWithStructures(0f, 0f, ARENA_WORLD_SIZE, 0f, isHorizontal = true)
  drawPerimeterWallWithStructures(0f, ARENA_WORLD_SIZE, ARENA_WORLD_SIZE, ARENA_WORLD_SIZE, isHorizontal = true)
  drawPerimeterWallWithStructures(0f, 0f, 0f, ARENA_WORLD_SIZE, isHorizontal = false)
  drawPerimeterWallWithStructures(ARENA_WORLD_SIZE, 0f, ARENA_WORLD_SIZE, ARENA_WORLD_SIZE, isHorizontal = false)
  val cornerWorlds = arrayOf(
    Pair(0f, 0f),
    Pair(ARENA_WORLD_SIZE, 0f),
    Pair(ARENA_WORLD_SIZE, ARENA_WORLD_SIZE),
    Pair(0f, ARENA_WORLD_SIZE),
  )
  cornerWorlds.forEachIndexed { cIdx, (cwx, cwy) ->
    val corner = worldToPerspectiveScreen(cwx, cwy, camX, camY, w, h)
    if (corner.x in -100f..w + 100f && corner.y in -100f..h + 100f) {
      val cScale = perspectiveScaleForRelY(cwy - camY, w, h)
      // Corner Citadel Watchtower + Rotating Searchlight Cone
      drawCircle(Color(0xFF101628), radius = 26f * cScale, center = corner)
      drawCircle(outerWallGlow, radius = 26f * cScale, center = corner, style = Stroke(width = 4f * cScale))
      drawCircle(outerWallCore, radius = 11f * cScale, center = corner)
      if (!lowGraphics) {
        val beamAng = elapsedTicks * 0.025f + cIdx * 1.57f
        val beamLen = 95f * cScale
        val beamPath = Path().apply {
          moveTo(corner.x, corner.y)
          lineTo(corner.x + cos(beamAng - 0.22f) * beamLen, corner.y + sin(beamAng - 0.22f) * beamLen * 0.72f)
          lineTo(corner.x + cos(beamAng + 0.22f) * beamLen, corner.y + sin(beamAng + 0.22f) * beamLen * 0.72f)
          close()
        }
        drawPath(beamPath, color = outerWallCore.copy(alpha = 0.14f + pulse * 0.08f))
      }
    }
  }

  // Center World Landmark projected in 2.5D
  val cCenter = worldToPerspectiveScreen(ARENA_CENTER, ARENA_CENTER, camX, camY, w, h)
  val cScale = perspectiveScaleForRelY(ARENA_CENTER - camY, w, h)
  val worldAxis = worldAxisPx(w, h) * currentWorldViewZoom()
  if (cCenter.x in -240f..w + 240f && cCenter.y in -240f..h + 240f) {
    when {
      areaId.contains("bank") || areaId.contains("vault") -> {
        val r = minOf(w, h) * 0.24f * cScale
        drawCircle(Color(0xFF0F172A), radius = r * 0.95f, center = cCenter)
        drawCircle(Color(0xFFFFD54F), radius = r * 0.95f, center = cCenter, style = Stroke(width = 6f * cScale))
        drawCircle(Color(0xFF00E5FF).copy(alpha = 0.5f + pulse * 0.3f), radius = r * 0.65f, center = cCenter, style = Stroke(width = 4f * cScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f))))
        for (sp in 0..7) {
          val ang = sp * (PI.toFloat() / 4f) + (elapsedTicks * 0.02f)
          val spX = cCenter.x + cos(ang) * r * 0.8f
          val spY = cCenter.y + sin(ang) * r * 0.58f
          drawLine(Color(0xFFFFD54F), cCenter, Offset(spX, spY), strokeWidth = 3.5f * cScale)
          drawCircle(Color(0xFF00E5FF), radius = 6f * cScale, center = Offset(spX, spY))
        }
        drawCircle(Color(0xFF00E5FF).copy(alpha = 0.85f), radius = 18f * cScale, center = cCenter)
        drawCircle(Color.White, radius = 9f * cScale, center = cCenter)
        drawRect(Color(0xFFFFD54F), topLeft = Offset(cCenter.x - r * 0.7f, cCenter.y + r * 0.35f), size = Size(24f * cScale, 14f * cScale))
        drawRect(Color(0xFFFFD54F), topLeft = Offset(cCenter.x + r * 0.45f, cCenter.y + r * 0.35f), size = Size(24f * cScale, 14f * cScale))
      }
      areaId.contains("abyss") -> {
        val r = minOf(w, h) * 0.22f * cScale
        drawOval(Color(0xFFB388FF).copy(alpha = 0.24f + pulse * 0.16f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.65f), size = Size(r * 2f, r * 1.3f), style = Stroke(width = 5f * cScale))
        drawCircle(Color(0xFFFFAB40).copy(alpha = 0.72f), radius = r * 0.34f, center = cCenter, style = Stroke(width = 3f * cScale))
      }
      areaId.contains("gta4") -> {
        val r = minOf(w, h) * 0.21f * cScale
        drawOval(Color(0xFFFFAB40).copy(alpha = 0.24f + pulse * 0.14f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.62f), size = Size(r * 2f, r * 1.24f), style = Stroke(width = 4f * cScale))
        drawOval(Color(0xFF00E676).copy(alpha = 0.62f), topLeft = Offset(cCenter.x - r * 0.58f, cCenter.y - r * 0.36f), size = Size(r * 1.16f, r * 0.72f), style = Stroke(width = 3f * cScale))
      }
      areaId.contains("superman") -> {
        val r = minOf(w, h) * 0.22f * cScale
        drawOval(Color(0xFF2979FF).copy(alpha = 0.26f + pulse * 0.14f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.64f), size = Size(r * 2f, r * 1.28f), style = Stroke(width = 4.5f * cScale))
        drawOval(Color(0xFFFF3D71).copy(alpha = 0.72f), topLeft = Offset(cCenter.x - r * 0.52f, cCenter.y - r * 0.32f), size = Size(r * 1.04f, r * 0.64f), style = Stroke(width = 3.2f * cScale))
      }
      else -> when (worldId) {
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
      "sector_star_wars_nexus" -> {
        val r = minOf(w, h) * 0.23f * cScale
        // Death Star Kyber Trench Ring + Twin Orbiting Suns
        drawOval(Color(0xFF40C4FF).copy(alpha = 0.28f + pulse * 0.14f), topLeft = Offset(cCenter.x - r, cCenter.y - r * 0.64f), size = Size(r * 2f, r * 1.28f), style = Stroke(width = 4.5f * cScale))
        drawCircle(Color(0xFFFFD54F).copy(alpha = 0.75f), radius = 11f * cScale, center = Offset(cCenter.x - r * 0.45f, cCenter.y - r * 0.20f))
        drawCircle(Color(0xFFFF7043).copy(alpha = 0.75f), radius = 8f * cScale, center = Offset(cCenter.x + r * 0.48f, cCenter.y - r * 0.12f))
        drawCircle(Color(0xFF00E676).copy(alpha = 0.65f), radius = 14f * cScale, center = cCenter, style = Stroke(width = 3f * cScale))
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
    if (areaId.startsWith("area_bo3_")) {
      val rBo3 = minOf(w, h) * 0.27f * cScale
      drawOval(
        color = Color(0xFF00E676).copy(alpha = 0.36f + pulse * 0.18f),
        topLeft = Offset(cCenter.x - rBo3, cCenter.y - rBo3 * 0.64f),
        size = Size(rBo3 * 2f, rBo3 * 1.28f),
        style = Stroke(width = 3f * cScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
      )
    } else if (areaId.startsWith("area_col_custom_")) {
      val rCol = minOf(w, h) * 0.27f * cScale
      drawOval(
        color = effAccent.copy(alpha = 0.42f + pulse * 0.18f),
        topLeft = Offset(cCenter.x - rCol, cCenter.y - rCol * 0.64f),
        size = Size(rCol * 2f, rCol * 1.28f),
        style = Stroke(width = 3f * cScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 8f))),
      )
    }
  }
}

fun DrawScope.drawSectionBiomesAndBorders(
  sim: ArenaSimState,
  camX: Float,
  camY: Float,
  highContrast: Boolean,
  lowGraphics: Boolean,
  glowOn: Boolean,
  pulse: Float,
) {
  val w = size.width
  val h = size.height
  if (!w.isFinite() || !h.isFinite() || w <= 1f || h <= 1f) return
  val horizonY = h * HORIZON_RATIO

  // 1. Render distinct 8x8 Section Biomes & Unique Floor Geometry
  for (i in 0 until sim.mapSections.size) {
    val sec = sim.mapSections.getOrNull(i) ?: continue
    val pCenter = worldToPerspectiveScreen(sec.centerX, sec.centerY, camX, camY, w, h)
    // Skip sections completely far outside camera view
    if (kotlin.math.abs(sec.centerX - camX) > 10.5f || kotlin.math.abs(sec.centerY - camY) > 10.5f) continue

    val tl = worldToPerspectiveScreen(sec.minX, sec.minY, camX, camY, w, h)
    val tr = worldToPerspectiveScreen(sec.maxX, sec.minY, camX, camY, w, h)
    val br = worldToPerspectiveScreen(sec.maxX, sec.maxY, camX, camY, w, h)
    val bl = worldToPerspectiveScreen(sec.minX, sec.maxY, camX, camY, w, h)
    val quad = Path().apply {
      moveTo(tl.x, tl.y.coerceAtLeast(horizonY))
      lineTo(tr.x, tr.y.coerceAtLeast(horizonY))
      lineTo(br.x, br.y.coerceAtLeast(horizonY))
      lineTo(bl.x, bl.y.coerceAtLeast(horizonY))
      close()
    }
    val groundTint = if (highContrast) Color(0xFF060812) else Color(sec.groundTintHex).copy(alpha = 0.56f)
    drawPath(quad, color = groundTint)

    val secAccent = Color(sec.accentHex)
    if (!lowGraphics && pCenter.x in -220f..w + 220f && pCenter.y in horizonY - 120f..h + 220f) {
      val sScale = perspectiveScaleForRelY(sec.centerY - camY, w, h)
      val r = minOf(w, h) * 0.19f * perspectiveScaleForRelY(sec.centerY - camY)
      when (sec.floorStyle) {
        SectionFloorStyle.CRYO -> {
          val tipOffset = Offset(pCenter.x, pCenter.y - r * 1.8f)
          val baseRect = Size(r * 0.9f, r * 0.7f)
          drawRect(Color(0xFF06283D), topLeft = Offset(pCenter.x - baseRect.width * 0.5f, pCenter.y - baseRect.height * 0.5f), size = baseRect)
          val iceSpire = Path().apply {
            moveTo(pCenter.x - baseRect.width * 0.5f, pCenter.y)
            lineTo(pCenter.x + baseRect.width * 0.5f, pCenter.y)
            lineTo(tipOffset.x, tipOffset.y)
            close()
          }
          drawPath(iceSpire, color = Color(0xFF00E5FF).copy(alpha = 0.45f + pulse * 0.15f))
          drawPath(iceSpire, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 3f * sScale))
          drawCircle(Color(0xFFFFD54F).copy(alpha = 0.8f + pulse * 0.2f), radius = 5f * sScale, center = tipOffset)
        }
        SectionFloorStyle.FOUNDRY -> {
          val towerRect = Size(r * 1.1f, r * 1.4f)
          val topCenter = Offset(pCenter.x, pCenter.y - r * 1.3f)
          drawRect(Color(0xFF160A06), topLeft = Offset(pCenter.x - towerRect.width * 0.5f, pCenter.y - towerRect.height * 0.6f), size = towerRect)
          drawRect(Color(0xFFFF5722).copy(alpha = 0.85f), topLeft = Offset(pCenter.x - towerRect.width * 0.5f, pCenter.y - towerRect.height * 0.6f), size = towerRect, style = Stroke(width = 3f * sScale))
          drawOval(Color(0xFFFFAB40).copy(alpha = 0.75f + pulse * 0.25f), topLeft = Offset(pCenter.x - r * 0.5f, pCenter.y - r * 0.2f), size = Size(r, r * 0.4f))
          drawLine(Color(0xFF37474F), topCenter, Offset(topCenter.x, topCenter.y - r * 0.5f), strokeWidth = 8f * sScale)
          drawCircle(Color(0xFFFFD54F).copy(alpha = 0.9f), radius = 6f * sScale, center = Offset(topCenter.x, topCenter.y - r * 0.5f))
        }
        SectionFloorStyle.BIO -> {
          val bulbCenter = Offset(pCenter.x, pCenter.y - r * 0.8f)
          drawCircle(Color(0xFF1B2E05).copy(alpha = 0.85f), radius = r * 0.55f, center = bulbCenter)
          drawCircle(Color(0xFF76FF03).copy(alpha = 0.75f + pulse * 0.25f), radius = r * 0.55f, center = bulbCenter, style = Stroke(width = 3.5f * sScale))
          for (tendril in 0..4) {
            val ang = tendril * 1.2f
            val endPoint = Offset(pCenter.x + cos(ang) * r * 1.1f, pCenter.y + sin(ang) * r * 0.6f)
            drawLine(Color(0xFF00E676).copy(alpha = 0.8f), bulbCenter, endPoint, strokeWidth = 4f * sScale, cap = StrokeCap.Round)
            drawCircle(Color(0xFFCCFF90), radius = 4f * sScale, center = endPoint)
          }
        }
        SectionFloorStyle.SOLAR -> {
          drawCircle(Color(0xFFFF8F00).copy(alpha = 0.25f + pulse * 0.15f), radius = r * 0.9f, center = pCenter)
          drawCircle(Color(0xFFFFD54F).copy(alpha = 0.9f), radius = r * 0.85f, center = pCenter, style = Stroke(width = 4f * sScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f))))
          drawCircle(Color(0xFF00E5FF).copy(alpha = 0.85f), radius = r * 0.4f, center = pCenter, style = Stroke(width = 3f * sScale))
          drawLine(Color(0xFFFFD54F), Offset(pCenter.x, pCenter.y - r * 0.7f), Offset(pCenter.x, pCenter.y - r * 1.6f), strokeWidth = 4f * sScale)
          drawCircle(Color.White, radius = 5.5f * sScale, center = Offset(pCenter.x, pCenter.y - r * 1.6f))
        }
        SectionFloorStyle.CRYPT -> {
          val tombRect = Size(r * 1.2f, r * 0.8f)
          drawRect(Color(0xFF0A120E), topLeft = Offset(pCenter.x - tombRect.width * 0.5f, pCenter.y - tombRect.height * 0.6f), size = tombRect)
          drawRect(Color(0xFF00E676).copy(alpha = 0.8f), topLeft = Offset(pCenter.x - tombRect.width * 0.5f, pCenter.y - tombRect.height * 0.6f), size = tombRect, style = Stroke(width = 3f * sScale))
          drawLine(Color(0xFF00E676), Offset(pCenter.x, pCenter.y - r * 1.1f), Offset(pCenter.x, pCenter.y), strokeWidth = 5f * sScale)
          drawLine(Color(0xFF00E676), Offset(pCenter.x - r * 0.35f, pCenter.y - r * 0.7f), Offset(pCenter.x + r * 0.35f, pCenter.y - r * 0.7f), strokeWidth = 4f * sScale)
          drawCircle(Color(0xFFFF3D71).copy(alpha = 0.85f + pulse * 0.2f), radius = 6f * sScale, center = Offset(pCenter.x, pCenter.y - r * 1.1f))
        }
        SectionFloorStyle.VOID -> {
          val ringR = r * 0.75f
          drawCircle(Color(0xFF14082B), radius = ringR, center = pCenter)
          drawCircle(Color(0xFF9C27B0).copy(alpha = 0.85f + pulse * 0.2f), radius = ringR, center = pCenter, style = Stroke(width = 4.5f * sScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f))))
          drawCircle(Color(0xFF00E5FF).copy(alpha = 0.9f), radius = ringR * 0.45f, center = pCenter)
          for (shard in 0..3) {
            val a = shard * 1.57f + sim.elapsedTicks * 0.03f
            val sp = Offset(pCenter.x + cos(a) * ringR * 1.25f, pCenter.y + sin(a) * ringR * 0.75f)
            drawRect(Color(0xFFE040FB), topLeft = Offset(sp.x - 4f, sp.y - 4f), size = Size(8f, 8f))
          }
        }
        SectionFloorStyle.URBAN -> {
          val bldgRect = Size(r * 1.3f, r * 1.6f)
          drawRect(Color(0xFF0B1329), topLeft = Offset(pCenter.x - bldgRect.width * 0.5f, pCenter.y - bldgRect.height * 0.7f), size = bldgRect)
          drawRect(Color(0xFFFFD54F).copy(alpha = 0.85f), topLeft = Offset(pCenter.x - bldgRect.width * 0.5f, pCenter.y - bldgRect.height * 0.7f), size = bldgRect, style = Stroke(width = 3f * sScale))
          for (wx in -1..1) {
            for (wy in -2..1) {
              drawRect(
                Color(0xFFFFD54F).copy(alpha = if ((wx + wy + sim.elapsedTicks.toInt()) % 2 == 0) 0.85f else 0.3f),
                topLeft = Offset(pCenter.x + wx * 12f * sScale - 4f, pCenter.y + wy * 14f * sScale - 6f),
                size = Size(8f * sScale, 10f * sScale),
              )
            }
          }
        }
        SectionFloorStyle.NEON -> {
          val pyrPath = Path().apply {
            moveTo(pCenter.x, pCenter.y - r * 1.5f)
            lineTo(pCenter.x + r * 0.75f, pCenter.y)
            lineTo(pCenter.x - r * 0.75f, pCenter.y)
            close()
          }
          drawPath(pyrPath, color = Color(0xFF240A38))
          drawPath(pyrPath, color = Color(0xFFFF3D71).copy(alpha = 0.9f), style = Stroke(width = 3.5f * sScale))
          drawLine(Color(0xFFFF3D71), Offset(pCenter.x, pCenter.y - r * 1.5f), Offset(pCenter.x, pCenter.y - r * 2.5f), strokeWidth = 5f * sScale)
          drawCircle(Color.White, radius = 6f * sScale, center = Offset(pCenter.x, pCenter.y - r * 2.5f))
        }
        else -> {
          val colRect = Size(r * 0.9f, r * 1.3f)
          drawRect(Color(0xFF0A192F), topLeft = Offset(pCenter.x - colRect.width * 0.5f, pCenter.y - colRect.height * 0.6f), size = colRect)
          drawRect(Color(0xFF00E5FF).copy(alpha = 0.85f), topLeft = Offset(pCenter.x - colRect.width * 0.5f, pCenter.y - colRect.height * 0.6f), size = colRect, style = Stroke(width = 3f * sScale))
          drawCircle(Color(0xFF00E5FF).copy(alpha = 0.9f), radius = 8f * sScale, center = Offset(pCenter.x, pCenter.y - colRect.height * 0.6f))
        }
      }
    }
  }

  // 2. Architectural Internal Section Border Walls along x=8,16 and y=8,16 with Parapet Towers & Gateway Archways
  val wallCore = if (highContrast) Color(0xFF00F0FF) else Color(0xFF40C4FF)
  val gateCol = Color(0xFF00E676)
  val gateHalf = ARENA_BORDER_HALF_GATE
  val gateCenters = floatArrayOf(4.0f, 12.0f, 20.0f)

  fun drawWallSegmentWithStructures(x1: Float, y1: Float, x2: Float, y2: Float) {
    val a = worldToPerspectiveScreen(x1, y1, camX, camY, w, h)
    val b = worldToPerspectiveScreen(x2, y2, camX, camY, w, h)
    if ((a.x < -120f && b.x < -120f) || (a.x > w + 120f && b.x > w + 120f)) return
    if ((a.y < horizonY - 60f && b.y < horizonY - 60f) || (a.y > h + 120f && b.y > h + 120f)) return
    drawLine(Color(0xFF090F1E), a, b, strokeWidth = 18f, cap = StrokeCap.Square)
    if (glowOn) drawLine(wallCore.copy(alpha = 0.28f + pulse * 0.14f), a, b, strokeWidth = 10f, cap = StrokeCap.Square)
    drawLine(wallCore.copy(alpha = 0.88f), a, b, strokeWidth = 2.8f, cap = StrokeCap.Square)
    if (!lowGraphics) {
      // Midpoint & quarter-span border buttress structures (biome-tinted colonnade / conduit nodes)
      val midX = (x1 + x2) * 0.5f
      val midY = (y1 + y2) * 0.5f
      if (kotlin.math.abs(midX - (camX + 0.5f)) < 6.0f && kotlin.math.abs(midY - (camY + 0.5f)) < 6.0f) {
        val mp = worldToPerspectiveScreen(midX, midY, camX, camY, w, h)
        val sc = perspectiveScaleForRelY(midY - camY, w, h)
        val secAccentCol = Color(sim.sectionAt(midX, midY).accentHex)
        val pw = 16f * sc
        val ph = 20f * sc
        drawRoundRect(
          color = Color(0xFF10192D),
          topLeft = Offset(mp.x - pw * 0.5f, mp.y - ph),
          size = Size(pw, ph),
          cornerRadius = CornerRadius(4f, 4f),
        )
        drawRoundRect(
          color = secAccentCol.copy(alpha = 0.9f),
          topLeft = Offset(mp.x - pw * 0.5f, mp.y - ph),
          size = Size(pw, ph),
          cornerRadius = CornerRadius(4f, 4f),
          style = Stroke(width = 2.2f * sc),
        )
        drawCircle(secAccentCol.copy(alpha = 0.75f + pulse * 0.2f), radius = 4f * sc, center = Offset(mp.x, mp.y - ph * 0.65f))
      }
    }
  }

  fun drawGatewayArchway(gx1: Float, gy1: Float, gx2: Float, gy2: Float) {
    val p1 = worldToPerspectiveScreen(gx1, gy1, camX, camY, w, h)
    val p2 = worldToPerspectiveScreen(gx2, gy2, camX, camY, w, h)
    val s1 = perspectiveScaleForRelY(gy1 - camY, w, h)
    val s2 = perspectiveScaleForRelY(gy2 - camY, w, h)
    val visible1 = p1.x in -100f..w + 100f && p1.y in horizonY - 60f..h + 100f
    val visible2 = p2.x in -100f..w + 100f && p2.y in horizonY - 60f..h + 100f
    if (visible1 || visible2) {
      // Overhead Checkpoint Archway span + threshold pad
      val top1 = Offset(p1.x, p1.y - 26f * s1)
      val top2 = Offset(p2.x, p2.y - 26f * s2)
      drawLine(gateCol.copy(alpha = 0.42f + pulse * 0.22f), p1, p2, strokeWidth = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
      drawLine(gateCol.copy(alpha = 0.78f), top1, top2, strokeWidth = 3.2f)
    }
    if (visible1) {
      drawRect(Color(0xFF0D1E22), topLeft = Offset(p1.x - 6f * s1, p1.y - 26f * s1), size = Size(12f * s1, 28f * s1))
      drawRect(gateCol, topLeft = Offset(p1.x - 6f * s1, p1.y - 26f * s1), size = Size(12f * s1, 28f * s1), style = Stroke(width = 2f * s1))
      drawCircle(gateCol, radius = 4.5f * s1, center = Offset(p1.x, p1.y - 28f * s1))
    }
    if (visible2) {
      drawRect(Color(0xFF0D1E22), topLeft = Offset(p2.x - 6f * s2, p2.y - 26f * s2), size = Size(12f * s2, 28f * s2))
      drawRect(gateCol, topLeft = Offset(p2.x - 6f * s2, p2.y - 26f * s2), size = Size(12f * s2, 28f * s2), style = Stroke(width = 2f * s2))
      drawCircle(gateCol, radius = 4.5f * s2, center = Offset(p2.x, p2.y - 28f * s2))
    }
  }

  for (wallX in floatArrayOf(8.0f, 16.0f)) {
    var cursor = 0.0f
    for (gc in gateCenters) {
      val gStart = (gc - gateHalf).coerceAtLeast(0f)
      val gEnd = (gc + gateHalf).coerceAtMost(ARENA_WORLD_SIZE)
      if (gStart > cursor) drawWallSegmentWithStructures(wallX, cursor, wallX, gStart)
      drawGatewayArchway(wallX, gStart, wallX, gEnd)
      cursor = gEnd
    }
    if (cursor < ARENA_WORLD_SIZE) drawWallSegmentWithStructures(wallX, cursor, wallX, ARENA_WORLD_SIZE)
  }

  for (wallY in floatArrayOf(8.0f, 16.0f)) {
    var cursor = 0.0f
    for (gc in gateCenters) {
      val gStart = (gc - gateHalf).coerceAtLeast(0f)
      val gEnd = (gc + gateHalf).coerceAtMost(ARENA_WORLD_SIZE)
      if (gStart > cursor) drawWallSegmentWithStructures(cursor, wallY, gStart, wallY)
      drawGatewayArchway(gStart, wallY, gEnd, wallY)
      cursor = gEnd
    }
    if (cursor < ARENA_WORLD_SIZE) drawWallSegmentWithStructures(cursor, wallY, ARENA_WORLD_SIZE, wallY)
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
  if (!w.isFinite() || !h.isFinite() || w <= 1f || h <= 1f) return

  val worldAxis = worldAxisPx(w, h) * currentWorldViewZoom()
  val resScale = canvasDensityScale(w, h)

  // 1. Elemental Hazard Zones (Ice, Fire, Toxic, Electric, Windy)
  for (i in 0 until sim.hazards.size) {
    val hz = sim.hazards.getOrNull(i) ?: continue
    val p = worldToPerspectiveScreen(hz.x, hz.y, camX, camY, w, h)
    val depthOnly = perspectiveScaleForRelY(hz.y - camY)
    val scale = depthOnly * resScale
    val rx = hz.radius * worldAxis * depthOnly
    val ry = rx * PERSP_Y_FORESHORTEN
    if (p.x + rx < -40f || p.x - rx > w + 40f || p.y + ry < -40f || p.y - ry > h + 40f) continue

    val col = hz.kind.color
    drawOval(col.copy(alpha = 0.16f + pulse * 0.08f), topLeft = Offset(p.x - rx, p.y - ry), size = Size(rx * 2f, ry * 2f))
    drawOval(col.copy(alpha = 0.85f), topLeft = Offset(p.x - rx, p.y - ry), size = Size(rx * 2f, ry * 2f), style = Stroke(width = 2.6f * scale))
    when (hz.kind) {
      ArenaHazardKind.ICE -> {
        drawLine(Color.White, Offset(p.x - 14f * scale, p.y), Offset(p.x + 14f * scale, p.y), strokeWidth = 2.2f * resScale)
        drawLine(Color.White, Offset(p.x, p.y - 10f * scale), Offset(p.x, p.y + 10f * scale), strokeWidth = 2.2f * resScale)
      }
      ArenaHazardKind.WINDY -> {
        for (l in -1..1) {
          drawLine(col, Offset(p.x - rx * 0.5f, p.y + l * 10f * scale), Offset(p.x + rx * 0.5f, p.y + l * 10f * scale - 6f * scale), strokeWidth = 2.4f * resScale, cap = StrokeCap.Round)
        }
      }
      else -> {
        if (glowOn) drawCircle(col.copy(alpha = 0.25f + pulse * 0.15f), radius = 14f * scale, center = p)
      }
    }
  }

  // 2. Safe Rest Camps (Take a break + x3 Shield Regeneration)
  for (i in 0 until sim.restCamps.size) {
    val camp = sim.restCamps.getOrNull(i) ?: continue
    val p = worldToPerspectiveScreen(camp.x, camp.y, camX, camY, w, h)
    val depthOnly = perspectiveScaleForRelY(camp.y - camY)
    val scale = depthOnly * resScale
    val r = camp.radius * worldAxis * depthOnly
    if (p.x + r < -40f || p.x - r > w + 40f || p.y + r < -40f || p.y - r > h + 40f) continue
    drawOval(Color(0xFF00E676).copy(alpha = 0.14f + pulse * 0.08f), topLeft = Offset(p.x - r, p.y - r * PERSP_Y_FORESHORTEN), size = Size(r * 2f, r * 2f * PERSP_Y_FORESHORTEN))
    drawOval(Color(0xFF00E676).copy(alpha = 0.85f), topLeft = Offset(p.x - r, p.y - r * PERSP_Y_FORESHORTEN), size = Size(r * 2f, r * 2f * PERSP_Y_FORESHORTEN), style = Stroke(width = 2.4f * scale))
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
    val b = sim.buildings.getOrNull(i) ?: continue
    val tl = worldToPerspectiveScreen(b.x, b.y, camX, camY, w, h)
    val br = worldToPerspectiveScreen(b.x + b.width, b.y + b.height, camX, camY, w, h)
    val sw = (br.x - tl.x).coerceAtLeast(18f * resScale)
    val sh = (br.y - tl.y).coerceAtLeast(14f * resScale)
    if (tl.x + sw < -60f || tl.x > w + 60f || tl.y + sh < -60f || tl.y > h + 60f) continue

    val midScale = perspectiveScaleForRelY((b.y + b.height * 0.5f) - camY, w, h)
    val extrudeH = (26f * midScale).coerceAtLeast(12f * resScale)
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

    // Doorway & Rooftop Reactor Node / Bank & Vault Landmarks
    drawRect(color = strokeCol, topLeft = Offset(tl.x + sw * 0.38f, tl.y + sh - 4f), size = Size(sw * 0.24f, 6f))
    val center = Offset(roofTL.x + (sw - 4f) * 0.5f, roofTL.y + sh * 0.42f)
    if (glowOn) drawCircle(strokeCol.copy(alpha = 0.22f + pulse * 0.14f), radius = 18f * midScale, center = center)
    val isVaultBld = b.name.contains("Vault", ignoreCase = true) || b.name.contains("Diamond", ignoreCase = true) || b.name.contains("Depository", ignoreCase = true)
    val isBankBld = b.name.contains("Bank", ignoreCase = true) || b.name.contains("Teller", ignoreCase = true) || b.name.contains("Bullion", ignoreCase = true) || b.name.contains("Mint", ignoreCase = true) || b.name.contains("Reserve", ignoreCase = true)
    when {
      isVaultBld -> {
        val dSz = 11f * midScale
        val dPath = Path().apply {
          moveTo(center.x, center.y - dSz)
          lineTo(center.x + dSz * 0.8f, center.y)
          lineTo(center.x, center.y + dSz)
          lineTo(center.x - dSz * 0.8f, center.y)
          close()
        }
        drawPath(dPath, color = Color(0xFF40C4FF))
        drawPath(dPath, color = Color(0xFFFFD54F), style = Stroke(width = 2.2f * midScale))
      }
      isBankBld -> {
        // Golden Bullion Vault Bars + Emerald Bank Crest
        for (colIdx in -1..1) {
          drawRect(
            color = Color(0xFFFFD54F),
            topLeft = Offset(center.x + colIdx * 6.5f * midScale - 2f * midScale, center.y - 7f * midScale),
            size = Size(4f * midScale, 14f * midScale),
          )
        }
        drawCircle(Color(0xFF00E676), radius = 12f * midScale, center = center, style = Stroke(width = 2.2f * midScale))
      }
      else -> drawCircle(strokeCol, radius = 6f * midScale, center = center)
    }
  }

  // 4. Electric & Fire Traps projected in 2.5D
  for (i in 0 until sim.traps.size) {
    val trap = sim.traps.getOrNull(i) ?: continue
    val p = worldToPerspectiveScreen(trap.x, trap.y, camX, camY, w, h)
    val depthOnly = perspectiveScaleForRelY(trap.y - camY)
    val scale = depthOnly * resScale
    val radPx = trap.radius * worldAxis * depthOnly
    if (p.x + radPx < -30f || p.x - radPx > w + 30f || p.y + radPx < -30f || p.y - radPx > h + 30f) continue

    val baseCol = trap.kind.color
    val activeAlpha = if (trap.activeTicks > 0f) 0.36f else if (trap.cooldownTicks > 0f) 0.10f else 0.22f
    drawOval(baseCol.copy(alpha = activeAlpha), topLeft = Offset(p.x - radPx, p.y - radPx * PERSP_Y_FORESHORTEN), size = Size(radPx * 2f, radPx * 2f * PERSP_Y_FORESHORTEN))
    drawOval(baseCol.copy(alpha = 0.85f), topLeft = Offset(p.x - radPx, p.y - radPx * PERSP_Y_FORESHORTEN), size = Size(radPx * 2f, radPx * 2f * PERSP_Y_FORESHORTEN), style = Stroke(width = 2.2f * scale))
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
    val st = sim.worldStations.getOrNull(i) ?: continue
    val p = worldToPerspectiveScreen(st.x, st.y, camX, camY, w, h)
    val scale = perspectiveScaleForRelY(st.y - camY, w, h)
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
    val crate = sim.lootCrates.getOrNull(i) ?: continue
    if (crate.opened) continue
    val p = worldToPerspectiveScreen(crate.x, crate.y, camX, camY, w, h)
    val scale = perspectiveScaleForRelY(crate.y - camY, w, h)
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
    val cat = sim.catEasterEggs.getOrNull(i) ?: continue
    if (cat.found) continue
    val p = worldToPerspectiveScreen(cat.x, cat.y, camX, camY, w, h)
    val scale = perspectiveScaleForRelY(cat.y - camY, w, h)
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

  // 8. Customizable Player Base Citadel Keep, Energy Shield Dome & Automated Turrets
  val basePt = worldToPerspectiveScreen(sim.baseX, sim.baseY, camX, camY, w, h)
  if (basePt.x in -180f * resScale..w + 180f * resScale && basePt.y in -180f * resScale..h + 180f * resScale) {
    val bScale = perspectiveScaleForRelY(sim.baseY - camY, w, h)
    val baseSt = sim.progression.playerBaseState
    val bCol = if (sim.hudBaseRaidActive) Color(0xFFFF3D71) else Color(baseSt.primaryHex)
    val domeR = 62f * bScale
    if (glowOn) {
      drawCircle(bCol.copy(alpha = 0.16f + pulse * 0.12f), radius = domeR, center = basePt)
    }
    drawOval(
      color = bCol.copy(alpha = 0.90f),
      topLeft = Offset(basePt.x - domeR, basePt.y - domeR * 0.65f),
      size = Size(domeR * 2f, domeR * 1.3f),
      style = Stroke(width = if (sim.hudBaseRaidActive) 3.8f * bScale else 2.8f * bScale),
    )
    // Citadel Fortress Core + Battlements
    val kw = 50f * bScale
    val kh = 38f * bScale
    drawRoundRect(
      color = Color(0xFF0D1528),
      topLeft = Offset(basePt.x - kw * 0.5f, basePt.y - kh * 0.65f),
      size = Size(kw, kh),
      cornerRadius = CornerRadius(8f, 8f),
    )
    drawRoundRect(
      color = bCol,
      topLeft = Offset(basePt.x - kw * 0.5f, basePt.y - kh * 0.65f),
      size = Size(kw, kh),
      cornerRadius = CornerRadius(8f, 8f),
      style = Stroke(width = 3f * bScale),
    )
    // Castle Crenellations along top edge + Central Fortress Core Emblem
    for (merlon in -2..2) {
      drawRect(
        color = bCol,
        topLeft = Offset(basePt.x + merlon * (kw * 0.19f) - 3.5f * bScale, basePt.y - kh * 0.78f),
        size = Size(7f * bScale, 6f * bScale),
      )
    }
    drawCircle(Color(0xFFFFD54F), radius = 6.5f * bScale, center = Offset(basePt.x, basePt.y - kh * 0.15f))
    drawCircle(bCol, radius = 10f * bScale, center = Offset(basePt.x, basePt.y - kh * 0.15f), style = Stroke(width = 2f * bScale))
    // Twin Automated Defense Turret Pylons
    for (txOff in listOf(-kw * 0.44f, kw * 0.44f)) {
      val tp = Offset(basePt.x + txOff, basePt.y - kh * 0.52f)
      drawCircle(Color(0xFF1A2540), radius = 7f * bScale, center = tp)
      drawCircle(Color(0xFFFFD54F), radius = 4f * bScale, center = tp)
    }
    // Base Integrity Bar
    val hpPct = (sim.hudBaseHp.toFloat() / sim.hudBaseMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
    drawRect(Color(0xFF1B2336), topLeft = Offset(basePt.x - 26f * bScale, basePt.y - kh * 0.98f), size = Size(52f * bScale, 5f * bScale))
    drawRect(Color(0xFF00E676), topLeft = Offset(basePt.x - 26f * bScale, basePt.y - kh * 0.98f), size = Size(52f * bScale * hpPct, 5f * bScale))
  }

  // 9. Automated Area Defense Turrets (perimeter bastions, gateways, and section sentinels)
  for (i in 0 until sim.defenseTurrets.size) {
    val turret = sim.defenseTurrets.getOrNull(i) ?: continue
    val p = worldToPerspectiveScreen(turret.x, turret.y, camX, camY, w, h)
    val tScale = perspectiveScaleForRelY(turret.y - camY, w, h)
    if (p.x < -80f || p.x > w + 80f || p.y < -80f || p.y > h + 80f) continue
    val tCol = turret.accentColor
    // Subtle detection range ring
    val rngPx = 28f * tScale
    drawOval(
      color = tCol.copy(alpha = 0.22f + pulse * 0.12f),
      topLeft = Offset(p.x - rngPx, p.y - rngPx * 0.65f),
      size = Size(rngPx * 2f, rngPx * 1.3f),
      style = Stroke(width = 1.6f * tScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
    )
    // Armored Pedestal
    drawCircle(Color(0xFF0F172A), radius = 12f * tScale, center = p)
    drawCircle(tCol, radius = 12f * tScale, center = p, style = Stroke(width = 2.6f * tScale))
    // Rotating Gun Barrel toward current target angle
    val bLen = 18f * tScale
    val tip = Offset(p.x + cos(turret.aimAngle) * bLen, p.y + sin(turret.aimAngle) * bLen * 0.72f)
    drawLine(Color(0xFFCFD8DC), p, tip, strokeWidth = 4.2f * tScale, cap = StrokeCap.Round)
    drawLine(tCol, p, tip, strokeWidth = 2f * tScale, cap = StrokeCap.Round)
    drawCircle(
      color = if (turret.muzzleFlashTicks > 0f) Color.White else tCol,
      radius = (if (turret.muzzleFlashTicks > 0f) 5.5f else 3.2f) * tScale,
      center = tip,
    )
    drawCircle(Color(0xFFFFD54F), radius = 4f * tScale, center = p)
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
  if (!w.isFinite() || !h.isFinite() || w <= 1f || h <= 1f) return

  // Expanding Magic Spell Nova & Thought/Spirit Healing Wave
  if (sim.magicWaveRadius > 0f) {
    val mp = worldToPerspectiveScreen(sim.playerX, sim.playerY, camX, camY, w, h)
    val mr = sim.magicWaveRadius * minOf(w, h) * 0.95f
    if (mr.isFinite() && mr > 0f) {
      drawOval(
        color = sim.magicWaveColor.copy(alpha = 0.72f),
        topLeft = Offset(mp.x - mr, mp.y - mr * 0.66f),
        size = Size(mr * 2f, mr * 1.32f),
        style = Stroke(width = 6f),
      )
    }
  }
  if (sim.spiritWaveRadius > 0f) {
    val sp = worldToPerspectiveScreen(sim.playerX, sim.playerY, camX, camY, w, h)
    val sr = sim.spiritWaveRadius * minOf(w, h) * 0.90f
    if (sr.isFinite() && sr > 0f) {
      drawOval(
        color = Color(0xFF00E676).copy(alpha = 0.78f),
        topLeft = Offset(sp.x - sr, sp.y - sr * 0.66f),
        size = Size(sr * 2f, sr * 1.32f),
        style = Stroke(width = 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))),
      )
    }
  }

  // Collectibles (Orbs, Gold, Diamonds, TUUCash, Resources, System Shards, Crystals, Blast Shards, Perks)
  for (i in 0 until sim.collectibles.size) {
    val c = sim.collectibles.getOrNull(i) ?: continue
    val pos = worldToPerspectiveScreen(c.x, c.y, camX, camY, w, h)
    val scale = perspectiveScaleForRelY(c.y - camY, w, h)
    if (pos.x < -30f || pos.x > w + 30f || pos.y < -30f || pos.y > h + 30f) continue
    if (glowOn) drawCircle(c.color.copy(alpha = 0.25f + pulse * 0.2f), radius = 14f * scale, center = pos)
    when (c.kind) {
      ArenaCollectibleKind.BLAST_SHARD -> {
        drawRect(c.color, topLeft = Offset(pos.x - 5f * scale, pos.y - 8f * scale), size = Size(10f * scale, 16f * scale))
        drawRect(Color.White, topLeft = Offset(pos.x - 5f * scale, pos.y - 8f * scale), size = Size(10f * scale, 16f * scale), style = Stroke(width = 1.6f * scale))
      }
      ArenaCollectibleKind.CHARACTER_SHARD, ArenaCollectibleKind.DIAMOND_GEM, ArenaCollectibleKind.CRYSTAL_NODE, ArenaCollectibleKind.XP_GEM -> {
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
      ArenaCollectibleKind.GOLD_COIN -> {
        drawCircle(Color(0xFFFFD54F), radius = 8.5f * scale, center = pos)
        drawCircle(Color(0xFFFF8F00), radius = 5f * scale, center = pos, style = Stroke(width = 1.6f * scale))
      }
      ArenaCollectibleKind.TUU_CASH, ArenaCollectibleKind.ENV_RESOURCE -> {
        drawRoundRect(c.color, topLeft = Offset(pos.x - 8f * scale, pos.y - 5f * scale), size = Size(16f * scale, 10f * scale), cornerRadius = CornerRadius(3f, 3f))
        drawRoundRect(Color.White, topLeft = Offset(pos.x - 8f * scale, pos.y - 5f * scale), size = Size(16f * scale, 10f * scale), cornerRadius = CornerRadius(3f, 3f), style = Stroke(width = 1.4f * scale))
      }
      ArenaCollectibleKind.PERK_DRINK -> {
        drawRoundRect(c.color, topLeft = Offset(pos.x - 6f * scale, pos.y - 9f * scale), size = Size(12f * scale, 18f * scale), cornerRadius = CornerRadius(4f, 4f))
        drawCircle(Color.White, radius = 3f * scale, center = pos)
      }
      ArenaCollectibleKind.BLAST_CORE, ArenaCollectibleKind.CAT_EASTER_EGG, ArenaCollectibleKind.SYSTEM_SHARD -> {
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
  val botScale = perspectiveScaleForRelY(sim.vacuumBotY - camY, w, h)
  val vacCol = Color(0xFF00E676)
  if (glowOn) drawCircle(vacCol.copy(alpha = 0.22f + pulse * 0.14f), radius = 16f * botScale, center = botPos)
  drawCircle(Color(0xFF0C1A24), radius = 9.5f * botScale, center = botPos)
  drawCircle(vacCol, radius = 9.5f * botScale, center = botPos, style = Stroke(width = 2.5f * botScale))
  drawCircle(Color(0xFF00E5FF), radius = 3.5f * botScale, center = botPos)
}

fun DrawScope.drawPlayerProtectionAndCompanions(
  sim: ArenaSimState,
  camX: Float,
  camY: Float,
  glowOn: Boolean,
  pulse: Float,
) {
  val w = size.width
  val h = size.height
  if (!w.isFinite() || !h.isFinite() || w <= 1f || h <= 1f) return
  val pCenter = worldToPerspectiveScreen(sim.playerX, sim.playerY, camX, camY, w, h)
  val pScale = perspectiveScaleForRelY(sim.playerY - camY, w, h)

  // Fortress Aegis Bubble Dome
  if (sim.hudAegisDomeActive) {
    val domeRx = 46f * pScale
    val domeRy = 34f * pScale
    if (glowOn) {
      drawOval(
        color = Color(0xFF38BDF8).copy(alpha = 0.18f + pulse * 0.12f),
        topLeft = Offset(pCenter.x - domeRx, pCenter.y - domeRy),
        size = Size(domeRx * 2f, domeRy * 2f),
      )
    }
    drawOval(
      color = Color(0xFF00E5FF),
      topLeft = Offset(pCenter.x - domeRx, pCenter.y - domeRy),
      size = Size(domeRx * 2f, domeRy * 2f),
      style = Stroke(width = 3.2f * pScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 6f))),
    )
  }

  // Orbiting Aegis Blades / Guardian Orbs
  val bladeCount = sim.hudOrbitalBladesCount
  if (bladeCount > 0) {
    val orbRad = 0.145f
    val baseAng = sim.elapsedTicks * 0.11f
    val stepAng = (2f * PI.toFloat()) / bladeCount.coerceAtLeast(1)
    for (k in 0 until bladeCount) {
      val ang = baseAng + k * stepAng
      val ox = sim.playerX + cos(ang) * orbRad
      val oy = sim.playerY + sin(ang) * orbRad
      val oPt = worldToPerspectiveScreen(ox, oy, camX, camY, w, h)
      val oScale = perspectiveScaleForRelY(oy - camY, w, h)
      val orbCol = if (k % 2 == 0) Color(0xFF38BDF8) else Color(0xFFFFD54F)
      if (glowOn) drawCircle(orbCol.copy(alpha = 0.28f + pulse * 0.16f), radius = 13f * oScale, center = oPt)
      drawCircle(Color(0xFF0B1626), radius = 6.5f * oScale, center = oPt)
      drawCircle(orbCol, radius = 6.5f * oScale, center = oPt, style = Stroke(width = 2.4f * oScale))
      drawCircle(Color.White, radius = 2.4f * oScale, center = oPt)
    }
  }

  // Dead Ops Arcade Wingman Companion Drone
  if (sim.wingmanLevel > 0) {
    val wPt = worldToPerspectiveScreen(sim.wingmanX, sim.wingmanY, camX, camY, w, h)
    val wScale = perspectiveScaleForRelY(sim.wingmanY - camY, w, h)
    val wCol = Color(0xFFE040FB)
    if (glowOn) drawCircle(wCol.copy(alpha = 0.25f + pulse * 0.18f), radius = 15f * wScale, center = wPt)
    drawRoundRect(
      color = Color(0xFF190E2B),
      topLeft = Offset(wPt.x - 8f * wScale, wPt.y - 6f * wScale),
      size = Size(16f * wScale, 12f * wScale),
      cornerRadius = CornerRadius(4f, 4f),
    )
    drawRoundRect(
      color = wCol,
      topLeft = Offset(wPt.x - 8f * wScale, wPt.y - 6f * wScale),
      size = Size(16f * wScale, 12f * wScale),
      cornerRadius = CornerRadius(4f, 4f),
      style = Stroke(width = 2.4f * wScale),
    )
    drawCircle(Color(0xFFFFD54F), radius = 3f * wScale, center = wPt)
  }

  // Equipped Collection Vehicle: Underglow Harness + 2.5D Deployed Escort/Mount Craft
  sim.activeVehicleSpec?.let { veh ->
    val vCol = Color(veh.accentHex)
    val vRx = 30f * pScale
    val vRy = 18f * pScale
    drawOval(
      color = vCol.copy(alpha = 0.52f + pulse * 0.18f),
      topLeft = Offset(pCenter.x - vRx, pCenter.y + 4f * pScale - vRy * 0.5f),
      size = Size(vRx * 2f, vRy),
      style = Stroke(width = 2.6f * pScale),
    )
    // Draw detailed 2.5D vehicle hull positioned cleanly beside the operator
    val vc = Offset(pCenter.x - 42f * pScale, pCenter.y + 6f * pScale)
    val aimLen = kotlin.math.hypot(sim.aimX, sim.aimY).coerceAtLeast(0.001f)
    val dirX = sim.aimX / aimLen
    val dirY = sim.aimY / aimLen
    if (glowOn) {
      drawCircle(vCol.copy(alpha = 0.22f + pulse * 0.14f), radius = 24f * pScale, center = vc)
    }
    when {
      veh.vehicleType.contains("Tank", ignoreCase = true) || veh.vehicleType.contains("Carrier", ignoreCase = true) -> {
        val hullW = 34f * pScale
        val hullH = 20f * pScale
        // Armored tracks / heavy wheels
        drawRoundRect(
          color = Color(0xFF1E293B),
          topLeft = Offset(vc.x - hullW * 0.55f, vc.y - hullH * 0.65f),
          size = Size(hullW * 1.1f, 6f * pScale),
          cornerRadius = CornerRadius(3f, 3f),
        )
        drawRoundRect(
          color = Color(0xFF1E293B),
          topLeft = Offset(vc.x - hullW * 0.55f, vc.y + hullH * 0.35f),
          size = Size(hullW * 1.1f, 6f * pScale),
          cornerRadius = CornerRadius(3f, 3f),
        )
        // Main Armored Hull
        drawRoundRect(
          color = Color(0xFF0F172A),
          topLeft = Offset(vc.x - hullW * 0.5f, vc.y - hullH * 0.5f),
          size = Size(hullW, hullH),
          cornerRadius = CornerRadius(5f, 5f),
        )
        drawRoundRect(
          color = vCol,
          topLeft = Offset(vc.x - hullW * 0.5f, vc.y - hullH * 0.5f),
          size = Size(hullW, hullH),
          cornerRadius = CornerRadius(5f, 5f),
          style = Stroke(width = 2.4f * pScale),
        )
        // Rotating Turret + Cannon Barrel
        drawCircle(vCol.copy(alpha = 0.85f), radius = 7.5f * pScale, center = vc)
        drawLine(
          color = Color.White,
          start = vc,
          end = Offset(vc.x + dirX * 22f * pScale, vc.y + dirY * 18f * pScale),
          strokeWidth = 4.2f * pScale,
          cap = StrokeCap.Round,
        )
      }
      veh.vehicleType.contains("Jet", ignoreCase = true) || veh.vehicleType.contains("Dropship", ignoreCase = true) -> {
        // Delta-wing aerial craft floating above shadow
        val hoverVc = Offset(vc.x, vc.y - 12f * pScale)
        drawOval(
          color = Color.Black.copy(alpha = 0.35f),
          topLeft = Offset(vc.x - 16f * pScale, vc.y + 4f * pScale),
          size = Size(32f * pScale, 10f * pScale),
        )
        val perpX = -dirY
        val perpY = dirX
        val nose = Offset(hoverVc.x + dirX * 20f * pScale, hoverVc.y + dirY * 14f * pScale)
        val wingL = Offset(hoverVc.x - dirX * 12f * pScale + perpX * 18f * pScale, hoverVc.y - dirY * 9f * pScale + perpY * 14f * pScale)
        val wingR = Offset(hoverVc.x - dirX * 12f * pScale - perpX * 18f * pScale, hoverVc.y - dirY * 9f * pScale - perpY * 14f * pScale)
        val jetPath = Path().apply {
          moveTo(nose.x, nose.y)
          lineTo(wingL.x, wingL.y)
          lineTo(hoverVc.x - dirX * 7f * pScale, hoverVc.y - dirY * 5f * pScale)
          lineTo(wingR.x, wingR.y)
          close()
        }
        drawPath(jetPath, color = Color(0xFF0F172A))
        drawPath(jetPath, color = vCol, style = Stroke(width = 2.5f * pScale))
        drawCircle(Color(0xFF00E5FF), radius = 4f * pScale, center = hoverVc)
        // Twin afterburners
        drawCircle(Color(0xFFFF9100), radius = (3.5f + pulse * 1.5f) * pScale, center = Offset(hoverVc.x - dirX * 11f * pScale, hoverVc.y - dirY * 8f * pScale))
      }
      else -> {
        // Hovercraft / Speed Bike sleek turbo body
        val bw = 32f * pScale
        val bh = 14f * pScale
        drawRoundRect(
          color = Color(0xFF0B1324),
          topLeft = Offset(vc.x - bw * 0.5f, vc.y - bh * 0.5f),
          size = Size(bw, bh),
          cornerRadius = CornerRadius(9f, 9f),
        )
        drawRoundRect(
          color = vCol,
          topLeft = Offset(vc.x - bw * 0.5f, vc.y - bh * 0.5f),
          size = Size(bw, bh),
          cornerRadius = CornerRadius(9f, 9f),
          style = Stroke(width = 2.6f * pScale),
        )
        // Glowing twin side wheels/thrusters + cockpit visor
        drawCircle(Color(0xFF00E5FF), radius = 4.5f * pScale, center = Offset(vc.x - bw * 0.32f, vc.y + bh * 0.45f))
        drawCircle(Color(0xFF00E5FF), radius = 4.5f * pScale, center = Offset(vc.x + bw * 0.32f, vc.y + bh * 0.45f))
        drawCircle(Color.White, radius = 3.2f * pScale, center = Offset(vc.x + dirX * 8f * pScale, vc.y))
      }
    }
  }
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

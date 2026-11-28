package com.example.ui.rig

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class EvaluatedSkeleton(
  val pelvis: Offset,
  val chest: Offset,
  val head: Offset,
  val lShoulder: Offset,
  val lElbow: Offset,
  val lHand: Offset,
  val rShoulder: Offset,
  val rElbow: Offset,
  val rHand: Offset,
  val weaponTip: Offset,
  val lHip: Offset,
  val lKnee: Offset,
  val lFoot: Offset,
  val rHip: Offset,
  val rKnee: Offset,
  val rFoot: Offset,
) {
  fun handleMap(): Map<String, Offset> = mapOf(
    "torso" to chest,
    "head" to head,
    "lUpperArm" to lElbow,
    "lForearm" to lHand,
    "rUpperArm" to rElbow,
    "rForearm" to rHand,
    "lThigh" to lKnee,
    "lShin" to lFoot,
    "rThigh" to rKnee,
    "rShin" to rFoot,
    "weaponAngle" to weaponTip,
  )
}

object RigEvaluator {

  fun evaluate(
    origin: Offset,
    scalePx: Float,
    poseAngles: Map<String, Double>,
    clip: String,
    frame: Float,
    ikEnabled: Boolean,
    animationSpeedMultiplier: Float = 1.0f,
  ): EvaluatedSkeleton {
    val safeOrigin = if (origin.x.isFinite() && origin.y.isFinite()) origin else Offset(160f, 160f)
    val safeScale = if (scalePx.isFinite() && scalePx > 10f) scalePx else 120f
    val safeFrame = if (frame.isFinite()) (frame % 60f + 60f) % 60f else 0f
    val safeSpeed = if (animationSpeedMultiplier.isFinite()) animationSpeedMultiplier.coerceIn(0.2f, 3.0f) else 1.0f
    val t = (safeFrame / 60f) * (2f * PI.toFloat()) * safeSpeed

    var bobYOff = 0f
    var torsoOff = 0f
    var headOff = 0f
    var lUpperOff = 0f
    var lForeOff = 0f
    var rUpperOff = 0f
    var rForeOff = 0f
    var lThighOff = 0f
    var lShinOff = 0f
    var rThighOff = 0f
    var rShinOff = 0f
    var weaponOff = 0f

    when (clip) {
      "sprint" -> {
        val st = sin(t)
        val ct = cos(t)
        val st2 = sin(t * 2f)
        bobYOff = -kotlin.math.abs(st2) * 0.04f
        torsoOff = 14f + st2 * 4f
        lUpperOff = st * 42f
        lForeOff = -35f + ct * 18f
        rUpperOff = -st * 42f
        rForeOff = 35f - ct * 18f
        lThighOff = -st * 38f
        lShinOff = 25f + maxOf(0f, st) * 35f
        rThighOff = st * 38f
        rShinOff = -25f - maxOf(0f, -st) * 35f
      }
      "lunge" -> {
        val st = sin(t)
        val ct = cos(t)
        bobYOff = st * 0.03f
        torsoOff = 20f + st * 12f
        rUpperOff = -55f + st * 65f
        rForeOff = 20f + ct * 28f
        lUpperOff = 30f - st * 25f
        lThighOff = -26f + st * 18f
        rThighOff = 28f - st * 18f
        weaponOff = st * 32f
      }
      "slam" -> {
        val st = sin(t)
        val ct = cos(t)
        val jump = maxOf(0f, st)
        bobYOff = -jump * 0.16f
        torsoOff = -12f + ct * 24f
        rUpperOff = -110f + ct * 75f
        lUpperOff = -80f + ct * 55f
        lThighOff = -jump * 32f
        rThighOff = jump * 28f
        weaponOff = ct * 45f
      }
      "vault" -> {
        val st = sin(t)
        val jump = maxOf(0f, st)
        bobYOff = -jump * 0.18f
        torsoOff = 26f
        lUpperOff = -40f + st * 30f
        rUpperOff = 35f - st * 30f
        lThighOff = -48f * jump
        lShinOff = 55f * jump
        rThighOff = -38f * jump
      }
      "inspect" -> {
        val st = sin(t)
        val ct = cos(t)
        bobYOff = st * 0.015f
        headOff = st * 14f
        rUpperOff = -18f + st * 16f
        rForeOff = -28f + ct * 15f
        weaponOff = st * 36f
      }
      else -> {
        val st = sin(t)
        bobYOff = st * 0.018f
        torsoOff = st * 3f
        lUpperOff = st * 5f
        rUpperOff = -st * 5f
        headOff = cos(t * 0.5f) * 4f
      }
    }

    fun baseAngle(id: String): Float {
      val v = poseAngles[id]
      return if (v != null && v.isFinite()) v.toFloat() else 0f
    }

    val pelvis = Offset(safeOrigin.x, safeOrigin.y + (if (bobYOff.isFinite()) bobYOff else 0f) * safeScale)

    val torsoDeg = baseAngle("torso") + torsoOff
    val chest = step(pelvis, -90f + torsoDeg, 0.34f * safeScale)
    val head = step(chest, -90f + torsoDeg + (baseAngle("head") + headOff) * 0.6f, 0.19f * safeScale)

    val lShoulder = Offset(chest.x - 0.14f * safeScale, chest.y + 0.03f * safeScale)
    val rShoulder = Offset(chest.x + 0.14f * safeScale, chest.y + 0.03f * safeScale)

    val lUpperDeg = 110f + torsoDeg * 0.4f + baseAngle("lUpperArm") + lUpperOff
    val lElbow = step(lShoulder, lUpperDeg, 0.22f * safeScale)
    val lForeDeg = lUpperDeg + (baseAngle("lForearm") + lForeOff) * 0.65f
    val lHand = step(lElbow, lForeDeg, 0.21f * safeScale)

    val rUpperDeg = 70f + torsoDeg * 0.4f + baseAngle("rUpperArm") + rUpperOff
    val rElbow = step(rShoulder, rUpperDeg, 0.22f * safeScale)
    val rForeDeg = rUpperDeg + (baseAngle("rForearm") + rForeOff) * 0.65f
    val rHand = step(rElbow, rForeDeg, 0.21f * safeScale)
    val weaponTip = step(rHand, rForeDeg - 25f + baseAngle("weaponAngle") + weaponOff, 0.28f * safeScale)

    val lHip = Offset(pelvis.x - 0.09f * safeScale, pelvis.y + 0.03f * safeScale)
    val rHip = Offset(pelvis.x + 0.09f * safeScale, pelvis.y + 0.03f * safeScale)

    val lThighDeg = 95f + baseAngle("lThigh") + lThighOff
    val lKnee = step(lHip, lThighDeg, 0.26f * safeScale)
    val lShinDeg = lThighDeg + (baseAngle("lShin") + lShinOff) * 0.6f
    var lFoot = step(lKnee, lShinDeg, 0.26f * safeScale)

    val rThighDeg = 85f + baseAngle("rThigh") + rThighOff
    val rKnee = step(rHip, rThighDeg, 0.26f * safeScale)
    val rShinDeg = rThighDeg + (baseAngle("rShin") + rShinOff) * 0.6f
    var rFoot = step(rKnee, rShinDeg, 0.26f * safeScale)

    if (ikEnabled) {
      val groundY = safeOrigin.y + 0.56f * safeScale
      if (groundY.isFinite()) {
        if (lFoot.y > groundY) lFoot = Offset(lFoot.x, groundY)
        if (rFoot.y > groundY) rFoot = Offset(rFoot.x, groundY)
      }
    }

    fun safePt(pt: Offset): Offset = if (pt.x.isFinite() && pt.y.isFinite()) pt else safeOrigin

    return EvaluatedSkeleton(
      pelvis = safePt(pelvis),
      chest = safePt(chest),
      head = safePt(head),
      lShoulder = safePt(lShoulder),
      lElbow = safePt(lElbow),
      lHand = safePt(lHand),
      rShoulder = safePt(rShoulder),
      rElbow = safePt(rElbow),
      rHand = safePt(rHand),
      weaponTip = safePt(weaponTip),
      lHip = safePt(lHip),
      lKnee = safePt(lKnee),
      lFoot = safePt(lFoot),
      rHip = safePt(rHip),
      rKnee = safePt(rKnee),
      rFoot = safePt(rFoot),
    )
  }

  private fun step(from: Offset, angleDeg: Float, length: Float): Offset {
    val safeDeg = if (angleDeg.isFinite()) angleDeg else 0f
    val safeLen = if (length.isFinite()) length else 0f
    val safeFromX = if (from.x.isFinite()) from.x else 0f
    val safeFromY = if (from.y.isFinite()) from.y else 0f
    val rad = safeDeg * (PI.toFloat() / 180f)
    val nx = safeFromX + cos(rad) * safeLen
    val ny = safeFromY + sin(rad) * safeLen
    return Offset(
      if (nx.isFinite()) nx else safeFromX,
      if (ny.isFinite()) ny else safeFromY,
    )
  }
}

fun DrawScope.drawRigCharacter(
  skeleton: EvaluatedSkeleton,
  archetype: String,
  primaryColor: Color,
  secondaryColor: Color,
  showBonesOverlay: Boolean,
  selectedBoneId: String? = null,
  capePathScratch: Path? = null,
  torsoPathScratch: Path? = null,
  packTier: Int = 1,
  isDashing: Boolean = false,
  dynamicGlow: Boolean = true,
  highContrast: Boolean = false,
  cloakOverride: Boolean? = null,
  visorColorOverride: Color? = null,
  auraTrailId: String = "none",
  outfitId: String = "",
) {
  try {
    if (!skeleton.pelvis.x.isFinite() || !skeleton.pelvis.y.isFinite() ||
        !skeleton.chest.x.isFinite() || !skeleton.chest.y.isFinite() ||
        !skeleton.head.x.isFinite() || !skeleton.head.y.isFinite()) {
      return
    }
    val isBatman = outfitId.startsWith("batman") || outfitId == "nightwing_v3" || archetype == "batman"
    val isNightwing = outfitId == "nightwing_v1" || outfitId == "nightwing_v2"
    val isSpiderMan = outfitId.startsWith("spiderman") || archetype == "spiderman"
    val isIronMan = outfitId.startsWith("ironman")
    val isIronSpider = outfitId == "spiderman_v3"
    val isSuperman = outfitId.startsWith("superman") || outfitId.startsWith("krypton")
    val isFlash = outfitId.startsWith("flash")
    val isGreenLantern = outfitId.startsWith("green_lantern")
    val isAbyss = outfitId.startsWith("abyss")
    val isCodGhost = outfitId.startsWith("cod_ghost")
    val isNiko = outfitId.startsWith("niko")
    val isHeller = outfitId.startsWith("heller")
    val isDelsin = outfitId.startsWith("delsin")
    val isRealLifeMe = outfitId.startsWith("mrcrossover") ||
      outfitId.startsWith("origin_sentinel") ||
      outfitId.startsWith("cole") ||
      outfitId.startsWith("prototype") ||
      outfitId.startsWith("spacetime") ||
      outfitId.startsWith("crucible") ||
      outfitId.startsWith("shipheroes")
    val torsoDist = kotlin.math.hypot(
      (skeleton.chest.x - skeleton.pelvis.x).toDouble(),
      (skeleton.chest.y - skeleton.pelvis.y).toDouble(),
    ).toFloat()
    val u = (torsoDist / 34f).let { if (it.isFinite()) it.coerceIn(0.45f, 3.5f) else 1f }
    val bodyStroke = (if (highContrast) 10.5f else 8.8f) * u
    val armorColor =
      if (visorColorOverride != null || cloakOverride != null) primaryColor
      else when (archetype) {
        "biomorph" -> Color(0xFF9C27B0)
        "infernal" -> Color(0xFFFF5252)
        "chrono" -> Color(0xFFFFD54F)
        "starblade" -> Color(0xFF00E5FF)
        else -> primaryColor
      }
    val underSuit = if (highContrast) Color(0xFF050810) else Color(0xFF131B2E)

    // Ground shadow + aura ring + Character Aura Trail
    val footY = (if (skeleton.lFoot.y.isFinite() && skeleton.rFoot.y.isFinite()) maxOf(skeleton.lFoot.y, skeleton.rFoot.y) else skeleton.pelvis.y + 54f * u)
    drawOval(
      color = Color.Black.copy(alpha = 0.52f),
      topLeft = Offset(skeleton.pelvis.x - 36f * u, footY - 5.5f * u),
      size = androidx.compose.ui.geometry.Size(72f * u, 14f * u),
    )
    if (auraTrailId != "none") {
      val auraCol = when (auraTrailId) {
        "chrono_spark" -> Color(0xFF448AFF)
        "infernal_flare" -> Color(0xFFFF3D71)
        else -> secondaryColor
      }
      drawOval(
        color = auraCol.copy(alpha = 0.35f),
        topLeft = Offset(skeleton.pelvis.x - 34f * u, footY - 7f * u),
        size = androidx.compose.ui.geometry.Size(68f * u, 16f * u),
        style = Stroke(width = 3f * u),
      )
    }
    if (dynamicGlow || highContrast || isDashing) {
      drawOval(
        color = (if (highContrast) Color.White else armorColor).copy(alpha = if (highContrast) 0.85f else if (isDashing) 0.55f else 0.22f),
        topLeft = Offset(skeleton.pelvis.x - 28f * u, footY - 3.5f * u),
        size = androidx.compose.ui.geometry.Size(56f * u, 10f * u),
        style = Stroke(width = (if (isDashing || highContrast) 3f else 1.8f) * u),
      )
    }

    // Flowing trenchcoat / hero cape behind torso + Iron Spider golden waldoes
    val showCloak = cloakOverride ?: (isBatman || isSuperman || isAbyss || archetype == "chrono" || archetype == "starblade" || archetype == "biomorph")
    if (isIronSpider) {
      val waldoGold = Color(0xFFFFD54F)
      val armOffsets = listOf(
        Pair(Offset(-30f * u, -22f * u), Offset(-40f * u, 8f * u)),
        Pair(Offset(30f * u, -22f * u), Offset(40f * u, 8f * u)),
        Pair(Offset(-25f * u, -36f * u), Offset(-36f * u, -10f * u)),
        Pair(Offset(25f * u, -36f * u), Offset(36f * u, -10f * u)),
      )
      armOffsets.forEach { (elbowOff, tipOff) ->
        val elbow = Offset(skeleton.chest.x + elbowOff.x, skeleton.chest.y + elbowOff.y)
        val tip = Offset(skeleton.chest.x + tipOff.x, skeleton.chest.y + tipOff.y)
        drawLine(waldoGold, skeleton.chest, elbow, strokeWidth = 4.2f * u, cap = StrokeCap.Round)
        drawLine(Color(0xFF00E5FF), elbow, tip, strokeWidth = 3f * u, cap = StrokeCap.Round)
        drawCircle(waldoGold, radius = 3f * u, center = elbow)
      }
    }
    if (isFlash && !highContrast) {
      // Speed-force lightning trail arcs behind Flash
      val boltCol = if (outfitId == "flash_v3") Color(0xFF00E5FF) else Color(0xFFFFD54F)
      drawLine(boltCol.copy(alpha = 0.72f), skeleton.chest, Offset(skeleton.pelvis.x - 20f * u, skeleton.pelvis.y + 7f * u), strokeWidth = 2.4f * u, cap = StrokeCap.Round)
      drawLine(boltCol.copy(alpha = 0.72f), skeleton.rElbow, Offset(skeleton.lFoot.x - 22f * u, skeleton.lFoot.y - 7f * u), strokeWidth = 2.2f * u, cap = StrokeCap.Round)
    }
    if (showCloak) {
      val capeColor = when {
        isBatman -> Color(0xFF0B0E17)
        isSuperman -> Color(0xFFD50000)
        isAbyss -> Color(0xFF2B0E3B)
        archetype == "biomorph" -> Color(0xFF3A0E24)
        archetype == "chrono" -> Color(0xFF1D2C4D)
        else -> armorColor.copy(alpha = 0.28f)
      }
      val capePath = (capePathScratch ?: Path()).apply {
        reset()
        moveTo(skeleton.lShoulder.x - 4f * u, skeleton.lShoulder.y)
        lineTo(skeleton.rShoulder.x + 4f * u, skeleton.rShoulder.y)
        lineTo(skeleton.rFoot.x + (if (isBatman) 16f else 11f) * u, footY - 4f * u)
        if (isBatman) {
          lineTo(skeleton.pelvis.x + 8f * u, footY - 11f * u)
          lineTo(skeleton.pelvis.x, footY - 4f * u)
          lineTo(skeleton.pelvis.x - 8f * u, footY - 11f * u)
        }
        lineTo(skeleton.lFoot.x - (if (isBatman) 16f else 11f) * u, footY - 4f * u)
        close()
      }
      drawPath(capePath, color = capeColor.copy(alpha = if (isBatman) 0.90f else 0.78f))
      drawPath(capePath, color = armorColor.copy(alpha = 0.55f), style = Stroke(width = 1.8f * u))
    }

    // Legs + Armored Greaves/Boots
    drawLine(underSuit, skeleton.lHip, skeleton.lKnee, strokeWidth = bodyStroke + 1.6f * u, cap = StrokeCap.Round)
    drawLine(armorColor.copy(alpha = 0.85f), skeleton.lHip, skeleton.lKnee, strokeWidth = bodyStroke * 0.78f, cap = StrokeCap.Round)
    drawLine(underSuit, skeleton.lKnee, skeleton.lFoot, strokeWidth = bodyStroke, cap = StrokeCap.Round)
    drawLine(armorColor, skeleton.lKnee, skeleton.lFoot, strokeWidth = bodyStroke * 0.68f, cap = StrokeCap.Round)
    drawCircle(secondaryColor, radius = 4.2f * u, center = skeleton.lKnee)
    drawOval(
      color = underSuit,
      topLeft = Offset(skeleton.lFoot.x - 6f * u, skeleton.lFoot.y - 3f * u),
      size = androidx.compose.ui.geometry.Size(12f * u, 7f * u),
    )
    drawOval(
      color = armorColor,
      topLeft = Offset(skeleton.lFoot.x - 6f * u, skeleton.lFoot.y - 3f * u),
      size = androidx.compose.ui.geometry.Size(12f * u, 7f * u),
      style = Stroke(width = 1.6f * u),
    )

    drawLine(underSuit, skeleton.rHip, skeleton.rKnee, strokeWidth = bodyStroke + 1.6f * u, cap = StrokeCap.Round)
    drawLine(armorColor.copy(alpha = 0.85f), skeleton.rHip, skeleton.rKnee, strokeWidth = bodyStroke * 0.78f, cap = StrokeCap.Round)
    drawLine(underSuit, skeleton.rKnee, skeleton.rFoot, strokeWidth = bodyStroke, cap = StrokeCap.Round)
    drawLine(armorColor, skeleton.rKnee, skeleton.rFoot, strokeWidth = bodyStroke * 0.68f, cap = StrokeCap.Round)
    drawCircle(secondaryColor, radius = 4.2f * u, center = skeleton.rKnee)
    drawOval(
      color = underSuit,
      topLeft = Offset(skeleton.rFoot.x - 5f * u, skeleton.rFoot.y - 3f * u),
      size = androidx.compose.ui.geometry.Size(12f * u, 7f * u),
    )
    drawOval(
      color = armorColor,
      topLeft = Offset(skeleton.rFoot.x - 5f * u, skeleton.rFoot.y - 3f * u),
      size = androidx.compose.ui.geometry.Size(12f * u, 7f * u),
      style = Stroke(width = 1.6f * u),
    )

    // Torso Armor Chassis
    val torsoPath = (torsoPathScratch ?: Path()).apply {
      reset()
      moveTo(skeleton.lShoulder.x - 3.5f * u, skeleton.lShoulder.y)
      lineTo(skeleton.rShoulder.x + 3.5f * u, skeleton.rShoulder.y)
      lineTo(skeleton.rHip.x + 2f * u, skeleton.rHip.y)
      lineTo(skeleton.lHip.x - 2f * u, skeleton.lHip.y)
      close()
    }
  drawPath(torsoPath, color = underSuit)
  drawPath(torsoPath, color = armorColor.copy(alpha = 0.42f))
  drawPath(torsoPath, color = if (highContrast) Color.White else armorColor, style = Stroke(width = (if (highContrast) 3.8f else 3.0f) * u))

  // Chest Core / Bat Emblem / Spider Emblem / Iron Man Arc Reactor
  val chestMid = Offset((skeleton.chest.x + skeleton.pelvis.x) * 0.5f, (skeleton.chest.y + skeleton.pelvis.y) * 0.5f)
  if (dynamicGlow) {
    drawCircle(color = secondaryColor.copy(alpha = 0.32f), radius = 9f * u, center = chestMid)
  }
  when {
    isBatman -> {
      drawOval(color = secondaryColor, topLeft = Offset(chestMid.x - 8.5f * u, chestMid.y - 4.5f * u), size = androidx.compose.ui.geometry.Size(17f * u, 9f * u))
      drawLine(Color(0xFF0A0D14), Offset(chestMid.x - 7f * u, chestMid.y), Offset(chestMid.x + 7f * u, chestMid.y), strokeWidth = 3f * u, cap = StrokeCap.Round)
    }
    isSpiderMan -> {
      val spCol = if (outfitId == "spiderman_v2") Color.White else if (isIronSpider) Color(0xFFFFD54F) else Color(0xFF0A0D14)
      drawCircle(color = spCol, radius = 4f * u, center = chestMid)
      for (leg in -1..1 step 2) {
        drawLine(spCol, chestMid, Offset(chestMid.x + leg * 8f * u, chestMid.y - 5f * u), strokeWidth = 1.6f * u)
        drawLine(spCol, chestMid, Offset(chestMid.x + leg * 8.5f * u, chestMid.y + 4.5f * u), strokeWidth = 1.6f * u)
      }
    }
    isIronMan -> {
      drawCircle(color = Color(0xFF00E5FF), radius = 5.5f * u, center = chestMid)
      drawCircle(color = Color.White, radius = 2.8f * u, center = chestMid)
    }
    isSuperman -> {
      val shield = Path().apply {
        moveTo(chestMid.x - 7f * u, chestMid.y - 4.5f * u)
        lineTo(chestMid.x + 7f * u, chestMid.y - 4.5f * u)
        lineTo(chestMid.x + 8.5f * u, chestMid.y - 1f * u)
        lineTo(chestMid.x, chestMid.y + 7f * u)
        lineTo(chestMid.x - 8.5f * u, chestMid.y - 1f * u)
        close()
      }
      drawPath(shield, color = Color(0xFFFFD54F))
      drawPath(shield, color = Color(0xFFD50000), style = Stroke(width = 2.0f * u))
      drawLine(Color(0xFFD50000), Offset(chestMid.x - 4.5f * u, chestMid.y - 1.8f * u), Offset(chestMid.x + 4.5f * u, chestMid.y + 1.8f * u), strokeWidth = 2.2f * u, cap = StrokeCap.Round)
    }
    isFlash -> {
      val crestCol = if (outfitId == "flash_v3") Color(0xFF00E5FF) else Color(0xFFFFD54F)
      drawCircle(color = Color.White, radius = 6f * u, center = chestMid)
      drawCircle(color = crestCol, radius = 6f * u, center = chestMid, style = Stroke(width = 1.8f * u))
      drawLine(crestCol, Offset(chestMid.x + 1.8f * u, chestMid.y - 5f * u), Offset(chestMid.x - 2.6f * u, chestMid.y), strokeWidth = 2.2f * u, cap = StrokeCap.Round)
      drawLine(crestCol, Offset(chestMid.x - 2.6f * u, chestMid.y), Offset(chestMid.x + 2.6f * u, chestMid.y + 1f * u), strokeWidth = 2.2f * u, cap = StrokeCap.Round)
      drawLine(crestCol, Offset(chestMid.x + 2.6f * u, chestMid.y + 1f * u), Offset(chestMid.x - 1.8f * u, chestMid.y + 6f * u), strokeWidth = 2.2f * u, cap = StrokeCap.Round)
    }
    isGreenLantern -> {
      val lanCol = Color(0xFF00E676)
      drawCircle(color = Color.White, radius = 6f * u, center = chestMid)
      drawCircle(color = lanCol, radius = 3.6f * u, center = chestMid, style = Stroke(width = 2.2f * u))
      drawLine(lanCol, Offset(chestMid.x - 5f * u, chestMid.y - 4.5f * u), Offset(chestMid.x + 5f * u, chestMid.y - 4.5f * u), strokeWidth = 2.0f * u)
      drawLine(lanCol, Offset(chestMid.x - 5f * u, chestMid.y + 4.5f * u), Offset(chestMid.x + 5f * u, chestMid.y + 4.5f * u), strokeWidth = 2.0f * u)
      drawCircle(color = lanCol.copy(alpha = 0.5f), radius = 7.5f * u, center = skeleton.rHand)
      drawCircle(color = Color.White, radius = 3f * u, center = skeleton.rHand)
    }
    isAbyss -> {
      drawCircle(color = Color(0xFFFFAB40).copy(alpha = 0.35f), radius = 7f * u, center = chestMid)
      drawLine(secondaryColor, Offset(chestMid.x, chestMid.y - 6f * u), Offset(chestMid.x, chestMid.y + 6f * u), strokeWidth = 2.6f * u, cap = StrokeCap.Round)
      drawLine(secondaryColor, Offset(chestMid.x - 5f * u, chestMid.y), Offset(chestMid.x + 5f * u, chestMid.y), strokeWidth = 2.2f * u, cap = StrokeCap.Round)
    }
    isNightwing -> {
      drawLine(secondaryColor, Offset(chestMid.x - 8.5f * u, chestMid.y - 3.5f * u), Offset(chestMid.x, chestMid.y + 3.5f * u), strokeWidth = 2.8f * u, cap = StrokeCap.Round)
      drawLine(secondaryColor, Offset(chestMid.x + 8.5f * u, chestMid.y - 3.5f * u), Offset(chestMid.x, chestMid.y + 3.5f * u), strokeWidth = 2.8f * u, cap = StrokeCap.Round)
    }
    isCodGhost -> {
      drawCircle(color = Color.White, radius = 5.2f * u, center = chestMid)
      drawCircle(color = Color(0xFF0A0D14), radius = 1.4f * u, center = Offset(chestMid.x - 2f * u, chestMid.y - 1f * u))
      drawCircle(color = Color(0xFF0A0D14), radius = 1.4f * u, center = Offset(chestMid.x + 2f * u, chestMid.y - 1f * u))
    }
    isHeller -> {
      drawCircle(color = Color(0xFFFF3D00), radius = 5.5f * u, center = chestMid, style = Stroke(width = 2.2f * u))
      drawLine(Color(0xFFFFAB40), Offset(chestMid.x - 5f * u, chestMid.y - 4.5f * u), Offset(chestMid.x + 5f * u, chestMid.y + 4.5f * u), strokeWidth = 2.2f * u)
    }
    isDelsin -> {
      drawLine(secondaryColor, Offset(chestMid.x - 6f * u, chestMid.y - 4.5f * u), Offset(chestMid.x + 6f * u, chestMid.y - 4.5f * u), strokeWidth = 2.2f * u)
      drawLine(secondaryColor, Offset(chestMid.x - 4.5f * u, chestMid.y + 3.5f * u), Offset(chestMid.x + 4.5f * u, chestMid.y + 3.5f * u), strokeWidth = 2.2f * u)
    }
    isNiko -> {
      drawRect(color = secondaryColor, topLeft = Offset(chestMid.x - 5f * u, chestMid.y - 3.5f * u), size = androidx.compose.ui.geometry.Size(10f * u, 7f * u), style = Stroke(width = 2f * u))
    }
    isRealLifeMe && !highContrast -> {
      val collarMid = Offset(skeleton.chest.x, skeleton.chest.y - 1.5f * u)
      drawLine(Color(0xFFE2E8F0).copy(alpha = 0.72f), collarMid, Offset(skeleton.pelvis.x, skeleton.pelvis.y - 1.5f * u), strokeWidth = 2.1f * u, cap = StrokeCap.Round)
      drawLine(secondaryColor.copy(alpha = 0.9f), skeleton.lShoulder, collarMid, strokeWidth = 2.8f * u, cap = StrokeCap.Round)
      drawLine(secondaryColor.copy(alpha = 0.9f), skeleton.rShoulder, collarMid, strokeWidth = 2.8f * u, cap = StrokeCap.Round)
      drawCircle(color = secondaryColor, radius = 3.2f * u, center = chestMid)
    }
    else -> drawCircle(color = secondaryColor, radius = 4.4f * u, center = chestMid)
  }
  // Tactical / Utility belt
  drawLine(
    color = if (isBatman) Color(0xFFFFD54F) else if (isRealLifeMe) Color(0xFF334155) else secondaryColor.copy(alpha = 0.8f),
    start = Offset(skeleton.lHip.x - 3f * u, skeleton.pelvis.y),
    end = Offset(skeleton.rHip.x + 3f * u, skeleton.pelvis.y),
    strokeWidth = (if (isBatman) 4.2f else 3.4f) * u,
    cap = StrokeCap.Round,
  )

  // Shoulder Pauldrons & Division Orange ISAC Beacon
  drawCircle(color = armorColor, radius = (if (isRealLifeMe) 5.8f else 7.0f) * u, center = skeleton.lShoulder)
  drawCircle(color = armorColor, radius = (if (isRealLifeMe) 5.8f else 7.0f) * u, center = skeleton.rShoulder)
  if (archetype == "operator") {
    drawCircle(color = Color(0xFFFF8F00), radius = 3.4f * u, center = Offset(skeleton.lShoulder.x, skeleton.lShoulder.y - 3.5f * u))
  }

  // Arms & Gauntlet Bracers
  drawLine(armorColor.copy(alpha = 0.9f), skeleton.lShoulder, skeleton.lElbow, strokeWidth = bodyStroke * 0.85f, cap = StrokeCap.Round)
  drawLine(secondaryColor, skeleton.lElbow, skeleton.lHand, strokeWidth = bodyStroke * 0.78f, cap = StrokeCap.Round)
  if (isRealLifeMe && !highContrast) {
    val watchPos = Offset((skeleton.lElbow.x + skeleton.lHand.x) * 0.52f, (skeleton.lElbow.y + skeleton.lHand.y) * 0.52f)
    drawCircle(color = Color(0xFF0F172A), radius = 4.6f * u, center = watchPos)
    drawCircle(color = Color(0xFF38BDF8), radius = 2.6f * u, center = watchPos)
  }
  drawLine(armorColor.copy(alpha = 0.9f), skeleton.rShoulder, skeleton.rElbow, strokeWidth = bodyStroke * 0.85f, cap = StrokeCap.Round)
  drawLine(secondaryColor, skeleton.rElbow, skeleton.rHand, strokeWidth = bodyStroke * 0.78f, cap = StrokeCap.Round)

  // Pack-a-Tier aura around active weapon
  if (packTier >= 2 && dynamicGlow) {
    val tierCol = if (packTier >= 3) Color(0xFFFFD54F) else Color(0xFFE040FB)
    drawLine(tierCol.copy(alpha = 0.38f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 14f * u, cap = StrokeCap.Round)
  }

  // Archetype Signature Weapons
  when (archetype) {
    "biomorph" -> {
      if (dynamicGlow) {
        drawLine(Color(0xFFFF1744).copy(alpha = 0.35f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 13f * u, cap = StrokeCap.Round)
      }
      drawLine(Color(0xFFFF3D71), skeleton.rHand, skeleton.weaponTip, strokeWidth = 7f * u, cap = StrokeCap.Round)
      drawCircle(Color(0xFF00E5FF).copy(alpha = 0.45f), radius = 10f * u, center = skeleton.lHand, style = Stroke(width = 2.5f * u))
      drawLine(Color(0xFF00E5FF), skeleton.lHand, Offset(skeleton.lHand.x - 13f * u, skeleton.lHand.y + 11f * u), strokeWidth = 3.8f * u, cap = StrokeCap.Round)
    }
    "infernal" -> {
      for (c in -1..1) {
        val ox = c * 4.2f * u
        val oy = c * 3.0f * u
        drawLine(Color(0xFFFFAB40), Offset(skeleton.rHand.x + ox, skeleton.rHand.y + oy), Offset(skeleton.weaponTip.x + ox, skeleton.weaponTip.y + oy), strokeWidth = 2.8f * u, cap = StrokeCap.Round)
        drawLine(Color(0xFFFFD54F), Offset(skeleton.lHand.x + ox * 0.7f, skeleton.lHand.y + oy * 0.7f), Offset(skeleton.lHand.x - 15f * u + ox, skeleton.lHand.y + 12f * u + oy), strokeWidth = 2.4f * u, cap = StrokeCap.Round)
      }
    }
    "starblade" -> {
      if (dynamicGlow) {
        drawLine(Color(0xFF00E5FF).copy(alpha = 0.32f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 14f * u, cap = StrokeCap.Round)
      }
      drawLine(Color(0xFFE040FB).copy(alpha = 0.55f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 7.5f * u, cap = StrokeCap.Round)
      drawLine(Color.White, skeleton.rHand, skeleton.weaponTip, strokeWidth = 3.4f * u, cap = StrokeCap.Round)
    }
    "chrono" -> {
      drawLine(Color(0xFFB0BEC5), skeleton.rHand, skeleton.weaponTip, strokeWidth = 4.2f * u, cap = StrokeCap.Round)
      if (dynamicGlow) {
        drawCircle(Color(0xFF448AFF).copy(alpha = 0.4f), radius = 9.5f * u, center = skeleton.weaponTip)
      }
      drawCircle(Color(0xFFFFD54F), radius = 5f * u, center = skeleton.weaponTip)
    }
    else -> {
      drawLine(Color(0xFF607D8B), skeleton.rElbow, skeleton.weaponTip, strokeWidth = 5.4f * u, cap = StrokeCap.Round)
      drawLine(Color(0xFFCFD8DC), skeleton.rHand, skeleton.weaponTip, strokeWidth = 3.8f * u, cap = StrokeCap.Round)
      drawCircle(secondaryColor, radius = 3.4f * u, center = skeleton.weaponTip)
    }
  }

  // Helmet / Head + Archetype / Batman Cowl / Spider-Man Lenses
  val headR = 13.5f * u
  val headFill = when {
    isSpiderMan && outfitId != "spiderman_v2" -> Color(0xFFB71C1C)
    isIronMan -> Color(0xFFC62828)
    isFlash -> Color(0xFFD50000)
    isSuperman -> Color(0xFF1A237E)
    isGreenLantern -> Color(0xFF0A2416)
    isAbyss -> Color(0xFF1D0C2B)
    else -> Color(0xFF0E1629)
  }
  if (isFlash) {
    val wingCol = if (outfitId == "flash_v3") Color(0xFF00E5FF) else Color(0xFFFFD54F)
    drawLine(wingCol, Offset(skeleton.head.x - 13f * u, skeleton.head.y), Offset(skeleton.head.x - 20f * u, skeleton.head.y - 10f * u), strokeWidth = 3.0f * u, cap = StrokeCap.Round)
    drawLine(wingCol, Offset(skeleton.head.x + 13f * u, skeleton.head.y), Offset(skeleton.head.x + 20f * u, skeleton.head.y - 10f * u), strokeWidth = 3.0f * u, cap = StrokeCap.Round)
  }
  if (isBatman) {
    val cowlCol = Color(0xFF0E1629)
    val rimCol = if (highContrast) Color.White else armorColor
    val lEar = Path().apply {
      moveTo(skeleton.head.x - 10.5f * u, skeleton.head.y - 6.5f * u)
      lineTo(skeleton.head.x - 8f * u, skeleton.head.y - 22f * u)
      lineTo(skeleton.head.x - 3f * u, skeleton.head.y - 11f * u)
      close()
    }
    val rEar = Path().apply {
      moveTo(skeleton.head.x + 10.5f * u, skeleton.head.y - 6.5f * u)
      lineTo(skeleton.head.x + 8f * u, skeleton.head.y - 22f * u)
      lineTo(skeleton.head.x + 3f * u, skeleton.head.y - 11f * u)
      close()
    }
    drawPath(lEar, color = cowlCol)
    drawPath(rEar, color = cowlCol)
    drawPath(lEar, color = rimCol, style = Stroke(width = 1.8f * u))
    drawPath(rEar, color = rimCol, style = Stroke(width = 1.8f * u))
  }
  drawCircle(color = headFill, radius = headR, center = skeleton.head)
  drawCircle(color = if (highContrast) Color.White else armorColor, radius = headR, center = skeleton.head, style = Stroke(width = (if (highContrast) 3.4f else 2.6f) * u))
  if (archetype == "infernal" && !isBatman && !isIronMan && !isAbyss) {
    drawLine(Color(0xFFFFAB40), Offset(skeleton.head.x - 7.5f * u, skeleton.head.y - 11f * u), Offset(skeleton.head.x + 7.5f * u, skeleton.head.y - 11f * u), strokeWidth = 3f * u, cap = StrokeCap.Round)
  }
  if (isSpiderMan) {
    val lensCol = visorColorOverride ?: Color.White
    val lEye = Path().apply {
      moveTo(skeleton.head.x - 9f * u, skeleton.head.y - 5f * u)
      lineTo(skeleton.head.x - 1.8f * u, skeleton.head.y - 1f * u)
      lineTo(skeleton.head.x - 7.5f * u, skeleton.head.y + 3.2f * u)
      close()
    }
    val rEye = Path().apply {
      moveTo(skeleton.head.x + 9.5f * u, skeleton.head.y - 5f * u)
      lineTo(skeleton.head.x + 2.5f * u, skeleton.head.y - 1f * u)
      lineTo(skeleton.head.x + 8f * u, skeleton.head.y + 3.2f * u)
      close()
    }
    drawPath(lEye, color = lensCol)
    drawPath(rEye, color = lensCol)
    drawPath(lEye, color = Color.Black, style = Stroke(width = 1.4f * u))
    drawPath(rEye, color = Color.Black, style = Stroke(width = 1.4f * u))
  } else if (isBatman) {
    val cowlEye = visorColorOverride ?: Color.White
    drawLine(cowlEye, Offset(skeleton.head.x - 7.5f * u, skeleton.head.y - 2.4f * u), Offset(skeleton.head.x - 1.8f * u, skeleton.head.y - 0.8f * u), strokeWidth = 2.8f * u, cap = StrokeCap.Round)
    drawLine(cowlEye, Offset(skeleton.head.x + 2.4f * u, skeleton.head.y - 0.8f * u), Offset(skeleton.head.x + 8.2f * u, skeleton.head.y - 2.4f * u), strokeWidth = 2.8f * u, cap = StrokeCap.Round)
  } else if (isGreenLantern) {
    val maskCol = Color(0xFF00E676)
    drawLine(maskCol, Offset(skeleton.head.x - 8.2f * u, skeleton.head.y - 1.8f * u), Offset(skeleton.head.x + 8.2f * u, skeleton.head.y - 1.8f * u), strokeWidth = 4.8f * u, cap = StrokeCap.Round)
    drawCircle(Color.White, radius = 2.0f * u, center = Offset(skeleton.head.x - 4.2f * u, skeleton.head.y - 1.8f * u))
    drawCircle(Color.White, radius = 2.0f * u, center = Offset(skeleton.head.x + 4.2f * u, skeleton.head.y - 1.8f * u))
  } else if (isRealLifeMe) {
    val frameCol = visorColorOverride ?: Color(0xFFE2E8F0)
    drawLine(Color(0xFF1E293B), Offset(skeleton.head.x - 8.2f * u, skeleton.head.y - 11f * u), Offset(skeleton.head.x + 8.2f * u, skeleton.head.y - 11f * u), strokeWidth = 3.6f * u, cap = StrokeCap.Round)
    drawRect(color = frameCol, topLeft = Offset(skeleton.head.x - 8.2f * u, skeleton.head.y - 5f * u), size = androidx.compose.ui.geometry.Size(6.6f * u, 5f * u), style = Stroke(width = 1.6f * u))
    drawRect(color = frameCol, topLeft = Offset(skeleton.head.x + 1.6f * u, skeleton.head.y - 5f * u), size = androidx.compose.ui.geometry.Size(6.6f * u, 5f * u), style = Stroke(width = 1.6f * u))
    drawLine(frameCol, Offset(skeleton.head.x - 1.6f * u, skeleton.head.y - 2.4f * u), Offset(skeleton.head.x + 1.6f * u, skeleton.head.y - 2.4f * u), strokeWidth = 1.6f * u)
  } else {
    drawLine(
      color = visorColorOverride ?: secondaryColor,
      start = Offset(skeleton.head.x - 5f * u, skeleton.head.y - 1.6f * u),
      end = Offset(skeleton.head.x + 9f * u, skeleton.head.y - 1.6f * u),
      strokeWidth = 4.2f * u,
      cap = StrokeCap.Round,
    )
  }

  if (showBonesOverlay) {
    val hs = u.coerceIn(0.8f, 1.8f)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.pelvis, skeleton.chest, strokeWidth = 2f * hs)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.chest, skeleton.head, strokeWidth = 2f * hs)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.lShoulder, skeleton.lElbow, strokeWidth = 2f * hs)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.lElbow, skeleton.lHand, strokeWidth = 2f * hs)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.rShoulder, skeleton.rElbow, strokeWidth = 2f * hs)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.rElbow, skeleton.rHand, strokeWidth = 2f * hs)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 2f * hs)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.lHip, skeleton.lKnee, strokeWidth = 2f * hs)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.lKnee, skeleton.lFoot, strokeWidth = 2f * hs)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.rHip, skeleton.rKnee, strokeWidth = 2f * hs)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.rKnee, skeleton.rFoot, strokeWidth = 2f * hs)

    fun drawHandle(id: String, pt: Offset) {
      val isSel = id == selectedBoneId
      drawCircle(
        color = if (isSel) Color(0xFFFFAB40) else Color(0xFF00E5FF),
        radius = (if (isSel) 9.5f else 6.2f) * hs,
        center = pt,
      )
      drawCircle(
        color = Color(0xFF070A12),
        radius = (if (isSel) 3.8f else 2.4f) * hs,
        center = pt,
      )
    }
    drawHandle("torso", skeleton.chest)
    drawHandle("head", skeleton.head)
    drawHandle("lUpperArm", skeleton.lElbow)
    drawHandle("lForearm", skeleton.lHand)
    drawHandle("rUpperArm", skeleton.rElbow)
    drawHandle("rForearm", skeleton.rHand)
    drawHandle("lThigh", skeleton.lKnee)
    drawHandle("lShin", skeleton.lFoot)
    drawHandle("rThigh", skeleton.rKnee)
    drawHandle("rShin", skeleton.rFoot)
    drawHandle("weaponAngle", skeleton.weaponTip)
  }
  } catch (t: Throwable) {
    android.util.Log.e("RigRenderer", "Error drawing rig character", t)
  }
}

fun DrawScope.drawArenaHostile(
  center: Offset,
  worldId: String,
  isElite: Boolean,
  stasisActive: Boolean,
  hp: Int,
  maxHp: Int,
  animPhase: Float,
  archetype: String = "standard",
  isBoss: Boolean = false,
  reduceMotion: Boolean = false,
  highContrast: Boolean = false,
  dynamicGlow: Boolean = true,
  overrideColor: Color? = null,
  scale: Float = 1.0f,
  armor: Int = 0,
  maxArmor: Int = 0,
  immuneColors: List<Color> = emptyList(),
) {
  try {
    if (!center.x.isFinite() || !center.y.isFinite()) return
    val effScale = if (scale.isFinite()) scale.coerceIn(0.55f, 1.85f) else 1.0f
    val baseRadius = when {
      isBoss -> 46f
      archetype == "mech" || archetype == "clockwork_automaton" || archetype == "ultron_sentry" || archetype == "blackgate_enforcer" || archetype == "b2_super_droid" || archetype == "dark_trooper" -> 34f
      isElite -> 31f
      archetype == "rusher" || archetype == "water_hive_swarm" -> 20f
      else -> 24f
    } * effScale
  val primary = when {
    stasisActive -> Color(0xFFFFD54F)
    overrideColor != null -> overrideColor
    archetype == "boss_hydra" -> Color(0xFF00E5FF)
    archetype == "boss_croc" -> Color(0xFF00E676)
    archetype == "boss_venom" -> Color(0xFFE040FB)
    archetype == "boss_goblin" -> Color(0xFFFF6D00)
    archetype == "boss_bane" -> Color(0xFF00E676)
    archetype == "boss_vader" -> Color(0xFFFF1744)
    archetype == "boss_starkiller" -> Color(0xFF40C4FF)
    archetype == "boss_atst" -> Color(0xFFB0BEC5)
    archetype == "stormtrooper" -> Color(0xFFECEFF1)
    archetype == "sith_acolyte" -> Color(0xFFFF3D71)
    archetype == "b2_super_droid" -> Color(0xFF90A4AE)
    archetype == "dark_trooper" -> Color(0xFFFF5252)
    archetype == "mandalorian" -> Color(0xFFFFD54F)
    archetype == "mech" || archetype == "boss_mech" || archetype == "ultron_sentry" -> Color(0xFFFF8F00)
    archetype == "blackwatch" -> Color(0xFFD50000)
    archetype == "gentek_mutant" -> Color(0xFFFF1744)
    archetype == "symbiote_stalker" -> Color(0xFFE040FB)
    archetype == "oscorp_goblin_drone" -> Color(0xFF76FF03)
    archetype == "arkham_thug" || archetype == "blackgate_enforcer" -> Color(0xFF448AFF)
    archetype == "joker_wild" -> Color(0xFFAA00FF)
    archetype == "reaper_conduit" -> Color(0xFF00E5FF)
    archetype == "water_hive_swarm" -> Color(0xFF00E676)
    archetype == "dup_conduit" -> Color(0xFFFF9100)
    archetype == "necromorph" -> Color(0xFFFF6D00)
    archetype == "clockwork_automaton" -> Color(0xFFFFD54F)
    archetype == "cosmic_raider" -> Color(0xFFAA00FF)
    archetype == "rift_phantom" -> Color(0xFF00E5FF)
    archetype == "slasher" -> Color(0xFFFF3D71)
    archetype == "trooper" -> Color(0xFF7C4DFF)
    isElite -> Color(0xFFFF1744)
    worldId == "sector_star_wars_nexus" -> Color(0xFFFFD54F)
    worldId == "gotham_arkham" -> Color(0xFFFFD54F)
    worldId == "spiderverse_queens" -> Color(0xFFE53935)
    worldId == "stark_orbital" -> Color(0xFFFF3D71)
    worldId == "empire_city_infamous" -> Color(0xFF00E5FF)
    worldId == "ship_of_heroes_apotheosis" -> Color(0xFFE040FB)
    worldId == "mr_crossover_nexus" -> Color(0xFF00E676)
    worldId == "sector_zero" -> Color(0xFF00E676)
    worldId == "blacklight_conduit" -> Color(0xFFE040FB)
    worldId == "hellbreaker_citadel" -> Color(0xFFFF6D00)
    worldId == "chrono_vault" -> Color(0xFF448AFF)
    else -> Color(0xFFFF3D71)
  }

  // 2.5D grounded contact shadow
  drawOval(
    color = Color.Black.copy(alpha = 0.45f),
    topLeft = Offset(center.x - baseRadius * 1.12f, center.y + baseRadius * 0.56f),
    size = androidx.compose.ui.geometry.Size(baseRadius * 2.24f, baseRadius * 0.64f),
  )

  // Aura shadow (skips soft bloom when dynamicGlow is disabled)
  if (dynamicGlow) {
    drawCircle(primary.copy(alpha = if (isBoss) 0.30f else 0.20f), radius = baseRadius * 1.45f, center = center)
  }
  if (isBoss || highContrast || armor > 0) {
    drawCircle(
      color = (if (highContrast) Color.White else if (armor > 0 && !isBoss) Color(0xFF90CAF9) else primary)
        .copy(alpha = if (highContrast) 0.85f else if (armor > 0) 0.55f else 0.65f),
      radius = baseRadius * 1.25f,
      center = center,
      style = Stroke(width = if (highContrast) 3f else if (armor > 0) 2.2f else 2.5f),
    )
  }

  val wobble = if (reduceMotion) 0f else sin(animPhase) * 3.5f * effScale

  // Shoulders / Silhouette body
  if (archetype == "mech" || archetype == "boss_mech" || archetype == "clockwork_automaton" || archetype == "b2_super_droid" || archetype == "boss_atst") {
    val chassisFill = when (archetype) {
      "clockwork_automaton" -> Color(0xFF281F10)
      "boss_atst", "b2_super_droid" -> Color(0xFF232C3B)
      else -> Color(0xFF1A2234)
    }
    drawRect(
      color = chassisFill,
      topLeft = Offset(center.x - baseRadius, center.y - baseRadius * 0.78f),
      size = androidx.compose.ui.geometry.Size(baseRadius * 2f, baseRadius * 1.55f),
    )
    drawRect(
      color = primary,
      topLeft = Offset(center.x - baseRadius, center.y - baseRadius * 0.78f),
      size = androidx.compose.ui.geometry.Size(baseRadius * 2f, baseRadius * 1.55f),
      style = Stroke(width = if (highContrast) 4f else 3f),
    )
    val pylonColor = if (archetype == "clockwork_automaton") Color(0xFFFFD54F) else if (archetype == "boss_atst") Color(0xFFFF3D71) else Color(0xFFFFAB40)
    drawLine(pylonColor, Offset(center.x - baseRadius * 0.8f, center.y - baseRadius * 0.6f), Offset(center.x - baseRadius * 0.8f, center.y - baseRadius * 1.2f), strokeWidth = 5f * effScale, cap = StrokeCap.Round)
    drawLine(pylonColor, Offset(center.x + baseRadius * 0.8f, center.y - baseRadius * 0.6f), Offset(center.x + baseRadius * 0.8f, center.y - baseRadius * 1.2f), strokeWidth = 5f * effScale, cap = StrokeCap.Round)
  } else {
    val fillTint = when (archetype) {
      "rift_phantom" -> Color(0x99062436)
      "water_hive_swarm" -> Color(0xFF0A241B)
      "cosmic_raider" -> Color(0xFF1E0B2E)
      "stormtrooper" -> Color(0xFF222A38)
      "boss_vader", "dark_trooper" -> Color(0xFF090B12)
      else -> Color(0xFF101728)
    }
    drawOval(
      color = fillTint,
      topLeft = Offset(center.x - baseRadius, center.y - baseRadius * 0.72f),
      size = androidx.compose.ui.geometry.Size(baseRadius * 2f, baseRadius * 1.45f),
    )
    drawOval(
      color = primary,
      topLeft = Offset(center.x - baseRadius, center.y - baseRadius * 0.72f),
      size = androidx.compose.ui.geometry.Size(baseRadius * 2f, baseRadius * 1.45f),
      style = Stroke(width = if (highContrast) 4f else if (isElite || isBoss) 3.2f else 2.2f),
    )
  }

  // Distinct archetype limbs & weapons
  when (archetype) {
    "sith_acolyte", "boss_vader" -> {
      val saberLen = if (archetype == "boss_vader") baseRadius * 2.1f else baseRadius * 1.65f
      drawLine(Color(0xFFFF1744), Offset(center.x + baseRadius * 0.7f, center.y), Offset(center.x + saberLen, center.y - 22f * effScale + wobble * 0.5f), strokeWidth = 5.5f * effScale, cap = StrokeCap.Round)
      drawLine(Color.White, Offset(center.x + baseRadius * 0.7f, center.y), Offset(center.x + saberLen, center.y - 22f * effScale + wobble * 0.5f), strokeWidth = 2.0f * effScale, cap = StrokeCap.Round)
      if (archetype == "boss_vader") {
        drawLine(Color(0xFF2A0C16), Offset(center.x - baseRadius * 0.8f, center.y), Offset(center.x - baseRadius * 1.25f, center.y + baseRadius * 0.9f), strokeWidth = 9f * effScale, cap = StrokeCap.Round)
      }
    }
    "boss_starkiller" -> {
      drawLine(Color(0xFF00E5FF), Offset(center.x - baseRadius * 0.7f, center.y + 4f), Offset(center.x - baseRadius * 1.9f, center.y - 16f * effScale + wobble), strokeWidth = 5f * effScale, cap = StrokeCap.Round)
      drawLine(Color.White, Offset(center.x - baseRadius * 0.7f, center.y + 4f), Offset(center.x - baseRadius * 1.9f, center.y - 16f * effScale + wobble), strokeWidth = 1.8f * effScale, cap = StrokeCap.Round)
      drawLine(Color(0xFFFF1744), Offset(center.x + baseRadius * 0.7f, center.y + 4f), Offset(center.x + baseRadius * 1.9f, center.y - 16f * effScale - wobble), strokeWidth = 5f * effScale, cap = StrokeCap.Round)
      drawLine(Color.White, Offset(center.x + baseRadius * 0.7f, center.y + 4f), Offset(center.x + baseRadius * 1.9f, center.y - 16f * effScale - wobble), strokeWidth = 1.8f * effScale, cap = StrokeCap.Round)
    }
    "stormtrooper", "dark_trooper", "mandalorian" -> {
      drawLine(Color(0xFF90A4AE), Offset(center.x + baseRadius * 0.5f, center.y + 2f * effScale), Offset(center.x + baseRadius * 1.45f, center.y - 2f * effScale), strokeWidth = 4.2f * effScale, cap = StrokeCap.Round)
      drawCircle(if (archetype == "mandalorian") Color(0xFFFFD54F) else Color(0xFFFF1744), radius = 3.5f * effScale, center = Offset(center.x + baseRadius * 1.45f, center.y - 2f * effScale))
      if (archetype == "dark_trooper" || archetype == "mandalorian") {
        drawCircle(Color(0xFFFF6D00), radius = 4f * effScale, center = Offset(center.x - baseRadius * 0.7f, center.y + baseRadius * 0.55f))
      }
    }
    "slasher", "necromorph" -> {
      val bladeCol = if (archetype == "necromorph") Color(0xFFFFD180) else Color(0xFFFFAB40)
      drawLine(bladeCol, Offset(center.x - baseRadius * 0.7f, center.y - 6f * effScale), Offset(center.x - baseRadius * 1.55f, center.y - 18f * effScale + wobble), strokeWidth = 4.2f * effScale, cap = StrokeCap.Round)
      drawLine(primary, Offset(center.x - baseRadius * 1.55f, center.y - 18f * effScale + wobble), Offset(center.x - baseRadius * 1.15f, center.y + 12f * effScale), strokeWidth = 3f * effScale, cap = StrokeCap.Round)
      drawLine(bladeCol, Offset(center.x + baseRadius * 0.7f, center.y - 6f * effScale), Offset(center.x + baseRadius * 1.55f, center.y - 18f * effScale - wobble), strokeWidth = 4.2f * effScale, cap = StrokeCap.Round)
      drawLine(primary, Offset(center.x + baseRadius * 1.55f, center.y - 18f * effScale - wobble), Offset(center.x + baseRadius * 1.15f, center.y + 12f * effScale), strokeWidth = 3f * effScale, cap = StrokeCap.Round)
    }
    "gentek_mutant" -> {
      drawLine(Color(0xFFE040FB), Offset(center.x - baseRadius * 0.8f, center.y), Offset(center.x - baseRadius * 1.65f, center.y - 8f * effScale + wobble), strokeWidth = 6f * effScale, cap = StrokeCap.Round)
      drawCircle(Color(0xFFFF1744), radius = 5.5f * effScale, center = Offset(center.x - baseRadius * 1.65f, center.y - 8f * effScale + wobble))
      drawLine(primary, Offset(center.x + baseRadius * 0.8f, center.y), Offset(center.x + baseRadius * 1.25f, center.y + 10f * effScale - wobble), strokeWidth = 3.5f * effScale, cap = StrokeCap.Round)
    }
    "water_hive_swarm" -> {
      for (sp in -1..1) {
        drawLine(Color(0xFF00E5FF), Offset(center.x + sp * 5f * effScale, center.y - baseRadius * 0.7f), Offset(center.x + sp * 9f * effScale, center.y - baseRadius * 1.35f + wobble * 0.4f), strokeWidth = 2.6f * effScale, cap = StrokeCap.Round)
      }
    }
    "dup_conduit" -> {
      val orbitX = cos(animPhase) * baseRadius * 1.35f
      val orbitY = sin(animPhase) * baseRadius * 0.65f
      drawRect(Color(0xFFFFAB40), topLeft = Offset(center.x + orbitX - 3.5f * effScale, center.y + orbitY - 3.5f * effScale), size = androidx.compose.ui.geometry.Size(7f * effScale, 7f * effScale))
      drawRect(Color(0xFF90A4AE), topLeft = Offset(center.x - orbitX - 3.5f * effScale, center.y - orbitY - 3.5f * effScale), size = androidx.compose.ui.geometry.Size(7f * effScale, 7f * effScale))
    }
    "cosmic_raider" -> {
      drawLine(Color(0xFF40C4FF), Offset(center.x + baseRadius * 0.6f, center.y + 4f * effScale), Offset(center.x + baseRadius * 1.55f, center.y - 12f * effScale), strokeWidth = 3.5f * effScale, cap = StrokeCap.Round)
      drawCircle(Color(0xFFE040FB), radius = 4f * effScale, center = Offset(center.x + baseRadius * 1.55f, center.y - 12f * effScale))
    }
    "rift_phantom" -> {
      drawLine(Color(0xFF00E5FF).copy(alpha = 0.6f), Offset(center.x - baseRadius * 0.5f, center.y + baseRadius * 0.5f), Offset(center.x - baseRadius * 0.8f, center.y + baseRadius * 1.1f + wobble), strokeWidth = 3f * effScale, cap = StrokeCap.Round)
      drawLine(Color(0xFF00E5FF).copy(alpha = 0.6f), Offset(center.x + baseRadius * 0.5f, center.y + baseRadius * 0.5f), Offset(center.x + baseRadius * 0.8f, center.y + baseRadius * 1.1f - wobble), strokeWidth = 3f * effScale, cap = StrokeCap.Round)
    }
    "boss_hydra" -> {
      for (h in -1..1) {
        val hx = center.x + h * 13f * effScale
        val hy = center.y - baseRadius * 0.9f + kotlin.math.abs(h) * 5f * effScale + wobble * 0.6f
        drawLine(primary, center, Offset(hx, hy), strokeWidth = 5f * effScale, cap = StrokeCap.Round)
        drawCircle(Color.White, radius = 6f * effScale, center = Offset(hx, hy))
      }
    }
    "symbiote_stalker", "boss_venom" -> {
      drawLine(Color(0xFFE040FB), Offset(center.x - baseRadius * 0.7f, center.y - 4f), Offset(center.x - baseRadius * 1.75f, center.y - 16f * effScale + wobble), strokeWidth = 5f * effScale, cap = StrokeCap.Round)
      drawLine(Color(0xFFE040FB), Offset(center.x + baseRadius * 0.7f, center.y - 4f), Offset(center.x + baseRadius * 1.75f, center.y - 16f * effScale - wobble), strokeWidth = 5f * effScale, cap = StrokeCap.Round)
      drawCircle(Color.White, radius = 4f * effScale, center = Offset(center.x - 5f * effScale, center.y - 6f * effScale))
      drawCircle(Color.White, radius = 4f * effScale, center = Offset(center.x + 5f * effScale, center.y - 6f * effScale))
    }
    "oscorp_goblin_drone", "boss_goblin" -> {
      // Sky-Glider wings & Pumpkin orb
      drawLine(Color(0xFF90A4AE), Offset(center.x - baseRadius * 1.55f, center.y + 8f * effScale), Offset(center.x + baseRadius * 1.55f, center.y + 8f * effScale), strokeWidth = 4.5f * effScale, cap = StrokeCap.Round)
      drawCircle(Color(0xFFFF6D00), radius = 5f * effScale, center = Offset(center.x + baseRadius * 1.2f, center.y - 8f * effScale + wobble))
    }
    "trooper", "blackwatch", "blackgate_enforcer", "ultron_sentry" -> {
      drawLine(Color(0xFF90A4AE), Offset(center.x + baseRadius * 0.6f, center.y + 2f * effScale), Offset(center.x + baseRadius * 1.38f, center.y + 4f * effScale), strokeWidth = 4.5f * effScale, cap = StrokeCap.Round)
      drawCircle(primary, radius = 3.5f * effScale, center = Offset(center.x + baseRadius * 1.38f, center.y + 4f * effScale))
    }
    else -> {
      drawLine(primary, Offset(center.x - baseRadius * 0.85f, center.y - 2f * effScale), Offset(center.x - baseRadius * 1.15f, center.y + 8f * effScale + wobble), strokeWidth = 3.5f * effScale, cap = StrokeCap.Round)
      drawLine(primary, Offset(center.x + baseRadius * 0.85f, center.y - 2f * effScale), Offset(center.x + baseRadius * 1.15f, center.y + 8f * effScale - wobble), strokeWidth = 3.5f * effScale, cap = StrokeCap.Round)
    }
  }

  // Head + Glowing Eyes / Visor
  val headCenter = Offset(center.x, center.y - 4f * effScale)
  drawCircle(color = Color(0xFF0B101C), radius = baseRadius * 0.52f, center = headCenter)
  drawCircle(color = primary, radius = baseRadius * 0.52f, center = headCenter, style = Stroke(width = 2f * effScale))
  val eyeColor = if (stasisActive) Color(0xFFFFD54F) else if (isElite || isBoss || highContrast) Color.White else primary
  drawCircle(eyeColor, radius = (if (isBoss) 3.5f else 2.5f) * effScale, center = Offset(headCenter.x - 4f * effScale, headCenter.y - 1f * effScale))
  drawCircle(eyeColor, radius = (if (isBoss) 3.5f else 2.5f) * effScale, center = Offset(headCenter.x + 4f * effScale, headCenter.y - 1f * effScale))

  // Boss / Elite / Armored / Damaged HP & Armor pip bars
  val barW = baseRadius * 2f
  val barY = center.y - baseRadius - 8f * effScale
  if (isBoss || isElite || hp < maxHp || armor > 0) {
    val ratio = (hp.toFloat() / maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
    drawLine(Color(0xAA000000), Offset(center.x - barW * 0.5f, barY), Offset(center.x + barW * 0.5f, barY), strokeWidth = 5f * effScale, cap = StrokeCap.Round)
    drawLine(primary, Offset(center.x - barW * 0.5f, barY), Offset(center.x - barW * 0.5f + barW * ratio, barY), strokeWidth = 3.5f * effScale, cap = StrokeCap.Round)
  }
  if (maxArmor > 0 && armor > 0) {
    val armRatio = (armor.toFloat() / maxArmor.coerceAtLeast(1)).coerceIn(0f, 1f)
    val armY = barY - 5.5f * effScale
    drawLine(Color(0xBB0A192F), Offset(center.x - barW * 0.5f, armY), Offset(center.x + barW * 0.5f, armY), strokeWidth = 4.2f * effScale, cap = StrokeCap.Round)
    drawLine(Color(0xFFB0BEC5), Offset(center.x - barW * 0.5f, armY), Offset(center.x - barW * 0.5f + barW * armRatio, armY), strokeWidth = 3f * effScale, cap = StrokeCap.Round)
  }
  if (immuneColors.isNotEmpty()) {
    drawCircle(
      color = immuneColors.first().copy(alpha = 0.38f),
      radius = baseRadius * 1.38f,
      center = center,
      style = Stroke(width = 2.2f * effScale),
    )
    val pipY = barY - (if (maxArmor > 0 && armor > 0) 11.5f else 6.5f) * effScale
    immuneColors.take(2).forEachIndexed { idx, immCol ->
      val pipX = center.x + ((idx - (immuneColors.size.coerceAtMost(2) - 1) / 2f) * 11f * effScale)
      drawCircle(Color.Black, radius = 4.6f * effScale, center = Offset(pipX, pipY))
      drawCircle(immCol, radius = 3.2f * effScale, center = Offset(pipX, pipY))
    }
  }
  } catch (t: Throwable) {
    android.util.Log.e("ArenaHostile", "Error drawing hostile", t)
  }
}

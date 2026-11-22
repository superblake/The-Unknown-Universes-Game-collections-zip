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
    val t = (frame / 60f) * (2f * PI.toFloat()) * animationSpeedMultiplier.coerceIn(0.5f, 2.0f)

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

    fun baseAngle(id: String): Float = poseAngles[id]?.toFloat() ?: 0f

    val pelvis = Offset(origin.x, origin.y + bobYOff * scalePx)

    val torsoDeg = baseAngle("torso") + torsoOff
    val chest = step(pelvis, -90f + torsoDeg, 0.34f * scalePx)
    val head = step(chest, -90f + torsoDeg + (baseAngle("head") + headOff) * 0.6f, 0.19f * scalePx)

    val lShoulder = Offset(chest.x - 0.14f * scalePx, chest.y + 0.03f * scalePx)
    val rShoulder = Offset(chest.x + 0.14f * scalePx, chest.y + 0.03f * scalePx)

    val lUpperDeg = 110f + torsoDeg * 0.4f + baseAngle("lUpperArm") + lUpperOff
    val lElbow = step(lShoulder, lUpperDeg, 0.22f * scalePx)
    val lForeDeg = lUpperDeg + (baseAngle("lForearm") + lForeOff) * 0.65f
    val lHand = step(lElbow, lForeDeg, 0.21f * scalePx)

    val rUpperDeg = 70f + torsoDeg * 0.4f + baseAngle("rUpperArm") + rUpperOff
    val rElbow = step(rShoulder, rUpperDeg, 0.22f * scalePx)
    val rForeDeg = rUpperDeg + (baseAngle("rForearm") + rForeOff) * 0.65f
    val rHand = step(rElbow, rForeDeg, 0.21f * scalePx)
    val weaponTip = step(rHand, rForeDeg - 25f + baseAngle("weaponAngle") + weaponOff, 0.28f * scalePx)

    val lHip = Offset(pelvis.x - 0.09f * scalePx, pelvis.y + 0.03f * scalePx)
    val rHip = Offset(pelvis.x + 0.09f * scalePx, pelvis.y + 0.03f * scalePx)

    val lThighDeg = 95f + baseAngle("lThigh") + lThighOff
    val lKnee = step(lHip, lThighDeg, 0.26f * scalePx)
    val lShinDeg = lThighDeg + (baseAngle("lShin") + lShinOff) * 0.6f
    var lFoot = step(lKnee, lShinDeg, 0.26f * scalePx)

    val rThighDeg = 85f + baseAngle("rThigh") + rThighOff
    val rKnee = step(rHip, rThighDeg, 0.26f * scalePx)
    val rShinDeg = rThighDeg + (baseAngle("rShin") + rShinOff) * 0.6f
    var rFoot = step(rKnee, rShinDeg, 0.26f * scalePx)

    if (ikEnabled) {
      val groundY = origin.y + 0.56f * scalePx
      if (lFoot.y > groundY) lFoot = Offset(lFoot.x, groundY)
      if (rFoot.y > groundY) rFoot = Offset(rFoot.x, groundY)
    }

    return EvaluatedSkeleton(
      pelvis = pelvis,
      chest = chest,
      head = head,
      lShoulder = lShoulder,
      lElbow = lElbow,
      lHand = lHand,
      rShoulder = rShoulder,
      rElbow = rElbow,
      rHand = rHand,
      weaponTip = weaponTip,
      lHip = lHip,
      lKnee = lKnee,
      lFoot = lFoot,
      rHip = rHip,
      rKnee = rKnee,
      rFoot = rFoot,
    )
  }

  private fun step(from: Offset, angleDeg: Float, length: Float): Offset {
    val rad = angleDeg * (PI.toFloat() / 180f)
    return Offset(from.x + cos(rad) * length, from.y + sin(rad) * length)
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
  val isBatman = outfitId.startsWith("batman") || archetype == "batman"
  val isSpiderMan = outfitId.startsWith("spiderman") || archetype == "spiderman"
  val isIronMan = outfitId.startsWith("ironman")
  val isIronSpider = outfitId == "spiderman_v3"
  val bodyStroke = if (highContrast) 13f else 11f
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
  val footY = maxOf(skeleton.lFoot.y, skeleton.rFoot.y)
  drawOval(
    color = Color.Black.copy(alpha = 0.52f),
    topLeft = Offset(skeleton.pelvis.x - 46f, footY - 7f),
    size = androidx.compose.ui.geometry.Size(92f, 18f),
  )
  if (auraTrailId != "none") {
    val auraCol = when (auraTrailId) {
      "chrono_spark" -> Color(0xFF448AFF)
      "infernal_flare" -> Color(0xFFFF3D71)
      else -> secondaryColor
    }
    drawOval(
      color = auraCol.copy(alpha = 0.35f),
      topLeft = Offset(skeleton.pelvis.x - 44f, footY - 9f),
      size = androidx.compose.ui.geometry.Size(88f, 20f),
      style = Stroke(width = 3.5f),
    )
  }
  if (dynamicGlow || highContrast || isDashing) {
    drawOval(
      color = (if (highContrast) Color.White else armorColor).copy(alpha = if (highContrast) 0.85f else if (isDashing) 0.55f else 0.22f),
      topLeft = Offset(skeleton.pelvis.x - 36f, footY - 4f),
      size = androidx.compose.ui.geometry.Size(72f, 12f),
      style = Stroke(width = if (isDashing || highContrast) 3.5f else 2f),
    )
  }

  // Flowing trenchcoat / hero cape behind torso + Iron Spider golden waldoes
  val showCloak = cloakOverride ?: (isBatman || archetype == "chrono" || archetype == "starblade" || archetype == "biomorph")
  if (isIronSpider) {
    val waldoGold = Color(0xFFFFD54F)
    val armOffsets = listOf(
      Pair(Offset(-36f, -26f), Offset(-48f, 10f)),
      Pair(Offset(36f, -26f), Offset(48f, 10f)),
      Pair(Offset(-30f, -44f), Offset(-44f, -12f)),
      Pair(Offset(30f, -44f), Offset(44f, -12f)),
    )
    armOffsets.forEach { (elbowOff, tipOff) ->
      val elbow = Offset(skeleton.chest.x + elbowOff.x, skeleton.chest.y + elbowOff.y)
      val tip = Offset(skeleton.chest.x + tipOff.x, skeleton.chest.y + tipOff.y)
      drawLine(waldoGold, skeleton.chest, elbow, strokeWidth = 5f, cap = StrokeCap.Round)
      drawLine(Color(0xFF00E5FF), elbow, tip, strokeWidth = 3.5f, cap = StrokeCap.Round)
      drawCircle(waldoGold, radius = 3.5f, center = elbow)
    }
  }
  if (showCloak) {
    val capeColor = when {
      isBatman -> Color(0xFF0B0E17)
      archetype == "biomorph" -> Color(0xFF3A0E24)
      archetype == "chrono" -> Color(0xFF1D2C4D)
      else -> armorColor.copy(alpha = 0.28f)
    }
    val capePath = (capePathScratch ?: Path()).apply {
      reset()
      moveTo(skeleton.lShoulder.x - 5f, skeleton.lShoulder.y)
      lineTo(skeleton.rShoulder.x + 5f, skeleton.rShoulder.y)
      lineTo(skeleton.rFoot.x + if (isBatman) 20f else 14f, footY - 6f)
      if (isBatman) {
        lineTo(skeleton.pelvis.x + 10f, footY - 14f)
        lineTo(skeleton.pelvis.x, footY - 5f)
        lineTo(skeleton.pelvis.x - 10f, footY - 14f)
      }
      lineTo(skeleton.lFoot.x - if (isBatman) 20f else 14f, footY - 6f)
      close()
    }
    drawPath(capePath, color = capeColor.copy(alpha = if (isBatman) 0.90f else 0.78f))
    drawPath(capePath, color = armorColor.copy(alpha = 0.55f), style = Stroke(width = 2f))
  }

  // Legs + Armored Greaves/Boots
  drawLine(underSuit, skeleton.lHip, skeleton.lKnee, strokeWidth = bodyStroke + 2f, cap = StrokeCap.Round)
  drawLine(armorColor.copy(alpha = 0.85f), skeleton.lHip, skeleton.lKnee, strokeWidth = bodyStroke * 0.78f, cap = StrokeCap.Round)
  drawLine(underSuit, skeleton.lKnee, skeleton.lFoot, strokeWidth = bodyStroke, cap = StrokeCap.Round)
  drawLine(armorColor, skeleton.lKnee, skeleton.lFoot, strokeWidth = bodyStroke * 0.68f, cap = StrokeCap.Round)
  drawCircle(secondaryColor, radius = 5f, center = skeleton.lKnee)

  drawLine(underSuit, skeleton.rHip, skeleton.rKnee, strokeWidth = bodyStroke + 2f, cap = StrokeCap.Round)
  drawLine(armorColor.copy(alpha = 0.85f), skeleton.rHip, skeleton.rKnee, strokeWidth = bodyStroke * 0.78f, cap = StrokeCap.Round)
  drawLine(underSuit, skeleton.rKnee, skeleton.rFoot, strokeWidth = bodyStroke, cap = StrokeCap.Round)
  drawLine(armorColor, skeleton.rKnee, skeleton.rFoot, strokeWidth = bodyStroke * 0.68f, cap = StrokeCap.Round)
  drawCircle(secondaryColor, radius = 5f, center = skeleton.rKnee)

  // Torso Armor Chassis
  val torsoPath = (torsoPathScratch ?: Path()).apply {
    reset()
    moveTo(skeleton.lShoulder.x - 4f, skeleton.lShoulder.y)
    lineTo(skeleton.rShoulder.x + 4f, skeleton.rShoulder.y)
    lineTo(skeleton.rHip.x + 2f, skeleton.rHip.y)
    lineTo(skeleton.lHip.x - 2f, skeleton.lHip.y)
    close()
  }
  drawPath(torsoPath, color = underSuit)
  drawPath(torsoPath, color = armorColor.copy(alpha = 0.42f))
  drawPath(torsoPath, color = if (highContrast) Color.White else armorColor, style = Stroke(width = if (highContrast) 4.2f else 3.5f))

  // Chest Core / Bat Emblem / Spider Emblem / Iron Man Arc Reactor
  val chestMid = Offset((skeleton.chest.x + skeleton.pelvis.x) * 0.5f, (skeleton.chest.y + skeleton.pelvis.y) * 0.5f)
  if (dynamicGlow) {
    drawCircle(color = secondaryColor.copy(alpha = 0.32f), radius = 10f, center = chestMid)
  }
  when {
    isBatman -> {
      // Bat-emblem wings silhouette
      drawOval(color = secondaryColor, topLeft = Offset(chestMid.x - 10f, chestMid.y - 5f), size = androidx.compose.ui.geometry.Size(20f, 10f))
      drawLine(Color(0xFF0A0D14), Offset(chestMid.x - 8f, chestMid.y), Offset(chestMid.x + 8f, chestMid.y), strokeWidth = 3.5f, cap = StrokeCap.Round)
    }
    isSpiderMan -> {
      val spCol = if (outfitId == "spiderman_v2") Color.White else if (isIronSpider) Color(0xFFFFD54F) else Color(0xFF0A0D14)
      drawCircle(color = spCol, radius = 4.5f, center = chestMid)
      for (leg in -1..1 step 2) {
        drawLine(spCol, chestMid, Offset(chestMid.x + leg * 9f, chestMid.y - 6f), strokeWidth = 1.8f)
        drawLine(spCol, chestMid, Offset(chestMid.x + leg * 10f, chestMid.y + 5f), strokeWidth = 1.8f)
      }
    }
    isIronMan -> {
      drawCircle(color = Color(0xFF00E5FF), radius = 6.5f, center = chestMid)
      drawCircle(color = Color.White, radius = 3.2f, center = chestMid)
    }
    else -> drawCircle(color = secondaryColor, radius = 5f, center = chestMid)
  }
  // Tactical / Utility belt
  drawLine(
    color = if (isBatman) Color(0xFFFFD54F) else secondaryColor.copy(alpha = 0.8f),
    start = Offset(skeleton.lHip.x - 4f, skeleton.pelvis.y),
    end = Offset(skeleton.rHip.x + 4f, skeleton.pelvis.y),
    strokeWidth = if (isBatman) 5f else 4f,
    cap = StrokeCap.Round,
  )

  // Shoulder Pauldrons & Division Orange ISAC Beacon
  drawCircle(color = armorColor, radius = 8.5f, center = skeleton.lShoulder)
  drawCircle(color = armorColor, radius = 8.5f, center = skeleton.rShoulder)
  if (archetype == "operator") {
    drawCircle(color = Color(0xFFFF8F00), radius = 4f, center = Offset(skeleton.lShoulder.x, skeleton.lShoulder.y - 4f))
  }

  // Arms & Gauntlet Bracers
  drawLine(armorColor.copy(alpha = 0.9f), skeleton.lShoulder, skeleton.lElbow, strokeWidth = bodyStroke * 0.85f, cap = StrokeCap.Round)
  drawLine(secondaryColor, skeleton.lElbow, skeleton.lHand, strokeWidth = bodyStroke * 0.78f, cap = StrokeCap.Round)
  drawLine(armorColor.copy(alpha = 0.9f), skeleton.rShoulder, skeleton.rElbow, strokeWidth = bodyStroke * 0.85f, cap = StrokeCap.Round)
  drawLine(secondaryColor, skeleton.rElbow, skeleton.rHand, strokeWidth = bodyStroke * 0.78f, cap = StrokeCap.Round)

  // Pack-a-Tier aura around active weapon when upgraded in Sector Zero / Armory
  if (packTier >= 2 && dynamicGlow) {
    val tierCol = if (packTier >= 3) Color(0xFFFFD54F) else Color(0xFFE040FB)
    drawLine(tierCol.copy(alpha = 0.38f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 18f, cap = StrokeCap.Round)
  }

  // Archetype Signature Weapons
  when (archetype) {
    "biomorph" -> {
      if (dynamicGlow) {
        drawLine(Color(0xFFFF1744).copy(alpha = 0.35f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 16f, cap = StrokeCap.Round)
      }
      drawLine(Color(0xFFFF3D71), skeleton.rHand, skeleton.weaponTip, strokeWidth = 8.5f, cap = StrokeCap.Round)
      drawCircle(Color(0xFF00E5FF).copy(alpha = 0.45f), radius = 12f, center = skeleton.lHand, style = Stroke(width = 3f))
      drawLine(Color(0xFF00E5FF), skeleton.lHand, Offset(skeleton.lHand.x - 16f, skeleton.lHand.y + 14f), strokeWidth = 4.5f, cap = StrokeCap.Round)
    }
    "infernal" -> {
      for (c in -1..1) {
        val ox = c * 5f
        val oy = c * 3.5f
        drawLine(Color(0xFFFFAB40), Offset(skeleton.rHand.x + ox, skeleton.rHand.y + oy), Offset(skeleton.weaponTip.x + ox, skeleton.weaponTip.y + oy), strokeWidth = 3.2f, cap = StrokeCap.Round)
        drawLine(Color(0xFFFFD54F), Offset(skeleton.lHand.x + ox * 0.7f, skeleton.lHand.y + oy * 0.7f), Offset(skeleton.lHand.x - 18f + ox, skeleton.lHand.y + 15f + oy), strokeWidth = 2.8f, cap = StrokeCap.Round)
      }
    }
    "starblade" -> {
      if (dynamicGlow) {
        drawLine(Color(0xFF00E5FF).copy(alpha = 0.32f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 18f, cap = StrokeCap.Round)
      }
      drawLine(Color(0xFFE040FB).copy(alpha = 0.55f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 9f, cap = StrokeCap.Round)
      drawLine(Color.White, skeleton.rHand, skeleton.weaponTip, strokeWidth = 4f, cap = StrokeCap.Round)
    }
    "chrono" -> {
      drawLine(Color(0xFFB0BEC5), skeleton.rHand, skeleton.weaponTip, strokeWidth = 5f, cap = StrokeCap.Round)
      if (dynamicGlow) {
        drawCircle(Color(0xFF448AFF).copy(alpha = 0.4f), radius = 11f, center = skeleton.weaponTip)
      }
      drawCircle(Color(0xFFFFD54F), radius = 6f, center = skeleton.weaponTip)
    }
    else -> {
      drawLine(Color(0xFF607D8B), skeleton.rElbow, skeleton.weaponTip, strokeWidth = 6.5f, cap = StrokeCap.Round)
      drawLine(Color(0xFFCFD8DC), skeleton.rHand, skeleton.weaponTip, strokeWidth = 4.5f, cap = StrokeCap.Round)
      drawCircle(secondaryColor, radius = 4f, center = skeleton.weaponTip)
    }
  }

  // Helmet / Head + Archetype / Batman Cowl / Spider-Man Lenses
  val headFill = when {
    isSpiderMan && outfitId != "spiderman_v2" -> Color(0xFFB71C1C)
    isIronMan -> Color(0xFFC62828)
    else -> Color(0xFF0E1629)
  }
  if (isBatman) {
    // Pointed Bat-Cowl Ears!
    val cowlCol = Color(0xFF0E1629)
    val rimCol = if (highContrast) Color.White else armorColor
    val lEar = Path().apply {
      moveTo(skeleton.head.x - 13f, skeleton.head.y - 8f)
      lineTo(skeleton.head.x - 10f, skeleton.head.y - 28f)
      lineTo(skeleton.head.x - 4f, skeleton.head.y - 14f)
      close()
    }
    val rEar = Path().apply {
      moveTo(skeleton.head.x + 13f, skeleton.head.y - 8f)
      lineTo(skeleton.head.x + 10f, skeleton.head.y - 28f)
      lineTo(skeleton.head.x + 4f, skeleton.head.y - 14f)
      close()
    }
    drawPath(lEar, color = cowlCol)
    drawPath(rEar, color = cowlCol)
    drawPath(lEar, color = rimCol, style = Stroke(width = 2.2f))
    drawPath(rEar, color = rimCol, style = Stroke(width = 2.2f))
  }
  drawCircle(color = headFill, radius = 17f, center = skeleton.head)
  drawCircle(color = if (highContrast) Color.White else armorColor, radius = 17f, center = skeleton.head, style = Stroke(width = if (highContrast) 4f else 3.2f))
  if (archetype == "infernal" && !isBatman && !isIronMan) {
    drawLine(Color(0xFFFFAB40), Offset(skeleton.head.x - 9f, skeleton.head.y - 14f), Offset(skeleton.head.x + 9f, skeleton.head.y - 14f), strokeWidth = 3.5f, cap = StrokeCap.Round)
  }
  if (isSpiderMan) {
    // Expressive angular white Spider-Man mask eye lenses!
    val lensCol = visorColorOverride ?: Color.White
    val lEye = Path().apply {
      moveTo(skeleton.head.x - 11f, skeleton.head.y - 6f)
      lineTo(skeleton.head.x - 2f, skeleton.head.y - 1f)
      lineTo(skeleton.head.x - 9f, skeleton.head.y + 4f)
      close()
    }
    val rEye = Path().apply {
      moveTo(skeleton.head.x + 12f, skeleton.head.y - 6f)
      lineTo(skeleton.head.x + 3f, skeleton.head.y - 1f)
      lineTo(skeleton.head.x + 10f, skeleton.head.y + 4f)
      close()
    }
    drawPath(lEye, color = lensCol)
    drawPath(rEye, color = lensCol)
    drawPath(lEye, color = Color.Black, style = Stroke(width = 1.6f))
    drawPath(rEye, color = Color.Black, style = Stroke(width = 1.6f))
  } else if (isBatman) {
    // White cowl eye slits
    val cowlEye = visorColorOverride ?: Color.White
    drawLine(cowlEye, Offset(skeleton.head.x - 9f, skeleton.head.y - 3f), Offset(skeleton.head.x - 2f, skeleton.head.y - 1f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    drawLine(cowlEye, Offset(skeleton.head.x + 3f, skeleton.head.y - 1f), Offset(skeleton.head.x + 10f, skeleton.head.y - 3f), strokeWidth = 3.5f, cap = StrokeCap.Round)
  } else {
    drawLine(
      color = visorColorOverride ?: secondaryColor,
      start = Offset(skeleton.head.x - 6f, skeleton.head.y - 2f),
      end = Offset(skeleton.head.x + 11f, skeleton.head.y - 2f),
      strokeWidth = 5f,
      cap = StrokeCap.Round,
    )
  }

  if (showBonesOverlay) {
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.pelvis, skeleton.chest, strokeWidth = 2f)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.chest, skeleton.head, strokeWidth = 2f)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.lShoulder, skeleton.lElbow, strokeWidth = 2f)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.lElbow, skeleton.lHand, strokeWidth = 2f)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.rShoulder, skeleton.rElbow, strokeWidth = 2f)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.rElbow, skeleton.rHand, strokeWidth = 2f)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 2f)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.lHip, skeleton.lKnee, strokeWidth = 2f)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.lKnee, skeleton.lFoot, strokeWidth = 2f)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.rHip, skeleton.rKnee, strokeWidth = 2f)
    drawLine(Color.White.copy(alpha = 0.75f), skeleton.rKnee, skeleton.rFoot, strokeWidth = 2f)

    fun drawHandle(id: String, pt: Offset) {
      val isSel = id == selectedBoneId
      drawCircle(
        color = if (isSel) Color(0xFFFFAB40) else Color(0xFF00E5FF),
        radius = if (isSel) 10f else 6.5f,
        center = pt,
      )
      drawCircle(
        color = Color(0xFF070A12),
        radius = if (isSel) 4f else 2.5f,
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
  val effScale = scale.coerceIn(0.45f, 1.55f)
  val baseRadius = when {
    isBoss -> 28f
    archetype == "mech" || archetype == "clockwork_automaton" -> 21f
    isElite -> 19f
    archetype == "rusher" || archetype == "water_hive_swarm" -> 12.5f
    else -> 15f
  } * effScale
  val primary = when {
    stasisActive -> Color(0xFFFFD54F)
    overrideColor != null -> overrideColor
    archetype == "boss_hydra" -> Color(0xFF00E5FF)
    archetype == "boss_croc" -> Color(0xFF00E676)
    archetype == "boss_venom" -> Color(0xFFE040FB)
    archetype == "boss_goblin" -> Color(0xFFFF6D00)
    archetype == "boss_bane" -> Color(0xFF00E676)
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
  if (archetype == "mech" || archetype == "boss_mech" || archetype == "clockwork_automaton") {
    val chassisFill = if (archetype == "clockwork_automaton") Color(0xFF281F10) else Color(0xFF1A2234)
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
    val pylonColor = if (archetype == "clockwork_automaton") Color(0xFFFFD54F) else Color(0xFFFFAB40)
    drawLine(pylonColor, Offset(center.x - baseRadius * 0.8f, center.y - baseRadius * 0.6f), Offset(center.x - baseRadius * 0.8f, center.y - baseRadius * 1.2f), strokeWidth = 5f * effScale, cap = StrokeCap.Round)
    drawLine(pylonColor, Offset(center.x + baseRadius * 0.8f, center.y - baseRadius * 0.6f), Offset(center.x + baseRadius * 0.8f, center.y - baseRadius * 1.2f), strokeWidth = 5f * effScale, cap = StrokeCap.Round)
  } else {
    val fillTint = when (archetype) {
      "rift_phantom" -> Color(0x99062436)
      "water_hive_swarm" -> Color(0xFF0A241B)
      "cosmic_raider" -> Color(0xFF1E0B2E)
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
    val pipY = barY - (if (maxArmor > 0 && armor > 0) 11.5f else 6.5f) * effScale
    immuneColors.take(2).forEachIndexed { idx, immCol ->
      val pipX = center.x + ((idx - (immuneColors.size.coerceAtMost(2) - 1) / 2f) * 9f * effScale)
      drawCircle(Color.Black, radius = 3.8f * effScale, center = Offset(pipX, pipY))
      drawCircle(immCol, radius = 2.6f * effScale, center = Offset(pipX, pipY))
    }
  }
}

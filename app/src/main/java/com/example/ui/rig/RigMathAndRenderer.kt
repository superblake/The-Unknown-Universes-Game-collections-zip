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
  ): EvaluatedSkeleton {
    val t = (frame / 60f) * (2f * PI.toFloat())
    val animOffsets = clipOffsets(clip, t)

    fun angle(id: String): Float {
      val base = poseAngles[id]?.toFloat() ?: 0f
      val delta = animOffsets[id] ?: 0f
      return base + delta
    }

    val bobY = (animOffsets["bobY"] ?: 0f) * scalePx
    val pelvis = Offset(origin.x, origin.y + bobY)

    val torsoDeg = angle("torso")
    val chest = step(pelvis, -90f + torsoDeg, 0.34f * scalePx)
    val head = step(chest, -90f + torsoDeg + angle("head") * 0.6f, 0.19f * scalePx)

    val lShoulder = Offset(chest.x - 0.14f * scalePx, chest.y + 0.03f * scalePx)
    val rShoulder = Offset(chest.x + 0.14f * scalePx, chest.y + 0.03f * scalePx)

    val lUpperDeg = 110f + torsoDeg * 0.4f + angle("lUpperArm")
    val lElbow = step(lShoulder, lUpperDeg, 0.22f * scalePx)
    val lForeDeg = lUpperDeg + angle("lForearm") * 0.65f
    val lHand = step(lElbow, lForeDeg, 0.21f * scalePx)

    val rUpperDeg = 70f + torsoDeg * 0.4f + angle("rUpperArm")
    val rElbow = step(rShoulder, rUpperDeg, 0.22f * scalePx)
    val rForeDeg = rUpperDeg + angle("rForearm") * 0.65f
    val rHand = step(rElbow, rForeDeg, 0.21f * scalePx)
    val weaponTip = step(rHand, rForeDeg - 25f + angle("weaponAngle"), 0.28f * scalePx)

    val lHip = Offset(pelvis.x - 0.09f * scalePx, pelvis.y + 0.03f * scalePx)
    val rHip = Offset(pelvis.x + 0.09f * scalePx, pelvis.y + 0.03f * scalePx)

    val lThighDeg = 95f + angle("lThigh")
    val lKnee = step(lHip, lThighDeg, 0.26f * scalePx)
    val lShinDeg = lThighDeg + angle("lShin") * 0.6f
    var lFoot = step(lKnee, lShinDeg, 0.26f * scalePx)

    val rThighDeg = 85f + angle("rThigh")
    val rKnee = step(rHip, rThighDeg, 0.26f * scalePx)
    val rShinDeg = rThighDeg + angle("rShin") * 0.6f
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

  private fun clipOffsets(clip: String, t: Float): Map<String, Float> =
    when (clip) {
      "sprint" -> mapOf(
        "bobY" to -kotlin.math.abs(sin(t * 2f)) * 0.04f,
        "torso" to 14f + sin(t * 2f) * 4f,
        "lUpperArm" to sin(t) * 42f,
        "lForearm" to -35f + cos(t) * 18f,
        "rUpperArm" to -sin(t) * 42f,
        "rForearm" to 35f - cos(t) * 18f,
        "lThigh" to -sin(t) * 38f,
        "lShin" to 25f + maxOf(0f, sin(t)) * 35f,
        "rThigh" to sin(t) * 38f,
        "rShin" to -25f - maxOf(0f, -sin(t)) * 35f,
      )
      "lunge" -> mapOf(
        "bobY" to sin(t) * 0.03f,
        "torso" to 20f + sin(t) * 12f,
        "rUpperArm" to -55f + sin(t) * 65f,
        "rForearm" to 20f + cos(t) * 28f,
        "lUpperArm" to 30f - sin(t) * 25f,
        "lThigh" to -26f + sin(t) * 18f,
        "rThigh" to 28f - sin(t) * 18f,
        "weaponAngle" to sin(t) * 32f,
      )
      "slam" -> {
        val jump = maxOf(0f, sin(t))
        mapOf(
          "bobY" to -jump * 0.16f,
          "torso" to -12f + cos(t) * 24f,
          "rUpperArm" to -110f + cos(t) * 75f,
          "lUpperArm" to -80f + cos(t) * 55f,
          "lThigh" to -jump * 32f,
          "rThigh" to jump * 28f,
          "weaponAngle" to cos(t) * 45f,
        )
      }
      "vault" -> mapOf(
        "bobY" to -maxOf(0f, sin(t)) * 0.18f,
        "torso" to 26f,
        "lUpperArm" to -40f + sin(t) * 30f,
        "rUpperArm" to 35f - sin(t) * 30f,
        "lThigh" to -48f * maxOf(0f, sin(t)),
        "lShin" to 55f * maxOf(0f, sin(t)),
        "rThigh" to -38f * maxOf(0f, sin(t)),
      )
      "inspect" -> mapOf(
        "bobY" to sin(t) * 0.015f,
        "head" to sin(t) * 14f,
        "rUpperArm" to -18f + sin(t) * 16f,
        "rForearm" to -28f + cos(t) * 15f,
        "weaponAngle" to sin(t) * 36f,
      )
      else -> mapOf( // idle
        "bobY" to sin(t) * 0.018f,
        "torso" to sin(t) * 3f,
        "lUpperArm" to sin(t) * 5f,
        "rUpperArm" to -sin(t) * 5f,
        "head" to cos(t * 0.5f) * 4f,
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
) {
  val bodyStroke = 11f
  val armorColor =
    when (archetype) {
      "biomorph" -> Color(0xFF9C27B0)
      "infernal" -> Color(0xFFFF5252)
      "chrono" -> Color(0xFFFFD54F)
      "starblade" -> Color(0xFF00E5FF)
      else -> primaryColor
    }
  val underSuit = Color(0xFF131B2E)

  // Ground shadow + aura ring
  val footY = maxOf(skeleton.lFoot.y, skeleton.rFoot.y)
  drawOval(
    color = Color.Black.copy(alpha = 0.52f),
    topLeft = Offset(skeleton.pelvis.x - 46f, footY - 7f),
    size = androidx.compose.ui.geometry.Size(92f, 18f),
  )
  drawOval(
    color = armorColor.copy(alpha = 0.22f),
    topLeft = Offset(skeleton.pelvis.x - 36f, footY - 4f),
    size = androidx.compose.ui.geometry.Size(72f, 12f),
    style = Stroke(width = 2f),
  )

  // Flowing trenchcoat / hero cape behind torso for biomorph, chrono, and starblade
  if (archetype == "chrono" || archetype == "starblade" || archetype == "biomorph") {
    val capeColor = when (archetype) {
      "biomorph" -> Color(0xFF3A0E24)
      "chrono" -> Color(0xFF1D2C4D)
      else -> Color(0xFF102A43)
    }
    val capePath = Path().apply {
      moveTo(skeleton.lShoulder.x - 4f, skeleton.lShoulder.y)
      lineTo(skeleton.rShoulder.x + 4f, skeleton.rShoulder.y)
      lineTo(skeleton.rFoot.x + 14f, footY - 8f)
      lineTo(skeleton.lFoot.x - 14f, footY - 8f)
      close()
    }
    drawPath(capePath, color = capeColor.copy(alpha = 0.78f))
    drawPath(capePath, color = armorColor.copy(alpha = 0.45f), style = Stroke(width = 2f))
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
  val torsoPath = Path().apply {
    moveTo(skeleton.lShoulder.x - 4f, skeleton.lShoulder.y)
    lineTo(skeleton.rShoulder.x + 4f, skeleton.rShoulder.y)
    lineTo(skeleton.rHip.x + 2f, skeleton.rHip.y)
    lineTo(skeleton.lHip.x - 2f, skeleton.lHip.y)
    close()
  }
  drawPath(torsoPath, color = underSuit)
  drawPath(torsoPath, color = armorColor.copy(alpha = 0.42f))
  drawPath(torsoPath, color = armorColor, style = Stroke(width = 3.5f))

  // Chest Core / Division SHD Beacon / Stargate Crest
  val chestMid = Offset((skeleton.chest.x + skeleton.pelvis.x) * 0.5f, (skeleton.chest.y + skeleton.pelvis.y) * 0.5f)
  drawCircle(color = secondaryColor.copy(alpha = 0.32f), radius = 10f, center = chestMid)
  drawCircle(color = secondaryColor, radius = 5f, center = chestMid)
  // Tactical belt
  drawLine(
    color = secondaryColor.copy(alpha = 0.8f),
    start = Offset(skeleton.lHip.x - 4f, skeleton.pelvis.y),
    end = Offset(skeleton.rHip.x + 4f, skeleton.pelvis.y),
    strokeWidth = 4f,
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

  // Archetype Signature Weapons (Prototype Blade, InFamous Arc, Doom/Wolverine Claws, Sonic/Stargate Staff, Force Saber, Tactical Carbine)
  when (archetype) {
    "biomorph" -> {
      // Crimson Prototype bio-scythe on right arm + InFamous electric wrist halo on left hand
      drawLine(Color(0xFFFF1744).copy(alpha = 0.35f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 16f, cap = StrokeCap.Round)
      drawLine(Color(0xFFFF3D71), skeleton.rHand, skeleton.weaponTip, strokeWidth = 8.5f, cap = StrokeCap.Round)
      drawCircle(Color(0xFF00E5FF).copy(alpha = 0.45f), radius = 12f, center = skeleton.lHand, style = Stroke(width = 3f))
      drawLine(Color(0xFF00E5FF), skeleton.lHand, Offset(skeleton.lHand.x - 16f, skeleton.lHand.y + 14f), strokeWidth = 4.5f, cap = StrokeCap.Round)
    }
    "infernal" -> {
      // Triple Wolverine / Doom Crucible claws on both hands
      for (c in -1..1) {
        val ox = c * 5f
        val oy = c * 3.5f
        drawLine(Color(0xFFFFAB40), Offset(skeleton.rHand.x + ox, skeleton.rHand.y + oy), Offset(skeleton.weaponTip.x + ox, skeleton.weaponTip.y + oy), strokeWidth = 3.2f, cap = StrokeCap.Round)
        drawLine(Color(0xFFFFD54F), Offset(skeleton.lHand.x + ox * 0.7f, skeleton.lHand.y + oy * 0.7f), Offset(skeleton.lHand.x - 18f + ox, skeleton.lHand.y + 15f + oy), strokeWidth = 2.8f, cap = StrokeCap.Round)
      }
    }
    "starblade" -> {
      // Force Unleashed backhand/dual-core plasma saber with outer glow bloom
      drawLine(Color(0xFF00E5FF).copy(alpha = 0.32f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 18f, cap = StrokeCap.Round)
      drawLine(Color(0xFFE040FB).copy(alpha = 0.55f), skeleton.rHand, skeleton.weaponTip, strokeWidth = 9f, cap = StrokeCap.Round)
      drawLine(Color.White, skeleton.rHand, skeleton.weaponTip, strokeWidth = 4f, cap = StrokeCap.Round)
    }
    "chrono" -> {
      // Stargate / Librarian / Doctor Who Sonic Relic Staff
      drawLine(Color(0xFFB0BEC5), skeleton.rHand, skeleton.weaponTip, strokeWidth = 5f, cap = StrokeCap.Round)
      drawCircle(Color(0xFF448AFF).copy(alpha = 0.4f), radius = 11f, center = skeleton.weaponTip)
      drawCircle(Color(0xFFFFD54F), radius = 6f, center = skeleton.weaponTip)
    }
    else -> {
      // Tactical CoD / Division Carbine with holographic sight & muzzle point
      drawLine(Color(0xFF607D8B), skeleton.rElbow, skeleton.weaponTip, strokeWidth = 6.5f, cap = StrokeCap.Round)
      drawLine(Color(0xFFCFD8DC), skeleton.rHand, skeleton.weaponTip, strokeWidth = 4.5f, cap = StrokeCap.Round)
      drawCircle(Color(0xFFFF8F00), radius = 4f, center = skeleton.weaponTip)
    }
  }

  // Helmet / Head + Archetype Visor
  drawCircle(color = Color(0xFF0E1629), radius = 17f, center = skeleton.head)
  drawCircle(color = armorColor, radius = 17f, center = skeleton.head, style = Stroke(width = 3.2f))
  if (archetype == "infernal") {
    // Praetor crown notch
    drawLine(Color(0xFFFFAB40), Offset(skeleton.head.x - 9f, skeleton.head.y - 14f), Offset(skeleton.head.x + 9f, skeleton.head.y - 14f), strokeWidth = 3.5f, cap = StrokeCap.Round)
  }
  drawLine(
    color = secondaryColor,
    start = Offset(skeleton.head.x - 6f, skeleton.head.y - 2f),
    end = Offset(skeleton.head.x + 11f, skeleton.head.y - 2f),
    strokeWidth = 5f,
    cap = StrokeCap.Round,
  )

  if (showBonesOverlay) {
    val boneSegments = listOf(
      skeleton.pelvis to skeleton.chest,
      skeleton.chest to skeleton.head,
      skeleton.lShoulder to skeleton.lElbow,
      skeleton.lElbow to skeleton.lHand,
      skeleton.rShoulder to skeleton.rElbow,
      skeleton.rElbow to skeleton.rHand,
      skeleton.rHand to skeleton.weaponTip,
      skeleton.lHip to skeleton.lKnee,
      skeleton.lKnee to skeleton.lFoot,
      skeleton.rHip to skeleton.rKnee,
      skeleton.rKnee to skeleton.rFoot,
    )
    boneSegments.forEach { (a, b) ->
      drawLine(Color.White.copy(alpha = 0.75f), a, b, strokeWidth = 2f)
    }
    skeleton.handleMap().forEach { (id, pt) ->
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
) {
  val baseRadius = if (isElite) 20f else 14f
  val primary = when {
    stasisActive -> Color(0xFFFFD54F)
    isElite -> Color(0xFFFF1744)
    worldId == "sector_zero" -> Color(0xFF00E676)
    worldId == "blacklight_conduit" -> Color(0xFFE040FB)
    worldId == "hellbreaker_citadel" -> Color(0xFFFF6D00)
    worldId == "chrono_vault" -> Color(0xFF448AFF)
    else -> Color(0xFFFF3D71)
  }

  // Aura shadow
  drawCircle(primary.copy(alpha = 0.22f), radius = baseRadius * 1.45f, center = center)

  // Shoulders / Silhouette body
  val wobble = sin(animPhase) * 3f
  drawOval(
    color = Color(0xFF101728),
    topLeft = Offset(center.x - baseRadius, center.y - baseRadius * 0.72f),
    size = androidx.compose.ui.geometry.Size(baseRadius * 2f, baseRadius * 1.45f),
  )
  drawOval(
    color = primary,
    topLeft = Offset(center.x - baseRadius, center.y - baseRadius * 0.72f),
    size = androidx.compose.ui.geometry.Size(baseRadius * 2f, baseRadius * 1.45f),
    style = Stroke(width = if (isElite) 3.2f else 2.2f),
  )

  // Outstretched zombie / stalker claws or rifle arms
  drawLine(
    color = primary,
    start = Offset(center.x - baseRadius * 0.85f, center.y - 2f),
    end = Offset(center.x - baseRadius * 1.15f, center.y + 8f + wobble),
    strokeWidth = 3.5f,
    cap = StrokeCap.Round,
  )
  drawLine(
    color = primary,
    start = Offset(center.x + baseRadius * 0.85f, center.y - 2f),
    end = Offset(center.x + baseRadius * 1.15f, center.y + 8f - wobble),
    strokeWidth = 3.5f,
    cap = StrokeCap.Round,
  )

  // Head + Glowing Eyes / Visor
  drawCircle(color = Color(0xFF0B101C), radius = baseRadius * 0.55f, center = Offset(center.x, center.y - 4f))
  drawCircle(color = primary, radius = baseRadius * 0.55f, center = Offset(center.x, center.y - 4f), style = Stroke(width = 2f))
  val eyeColor = if (stasisActive) Color(0xFFFFD54F) else if (isElite) Color.White else primary
  drawCircle(eyeColor, radius = 2.5f, center = Offset(center.x - 3.5f, center.y - 5f))
  drawCircle(eyeColor, radius = 2.5f, center = Offset(center.x + 3.5f, center.y - 5f))

  // Elite crown or HP pip bar
  if (isElite || hp < maxHp) {
    val barW = baseRadius * 2f
    val ratio = (hp.toFloat() / maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
    drawLine(Color(0xAA000000), Offset(center.x - barW * 0.5f, center.y - baseRadius - 8f), Offset(center.x + barW * 0.5f, center.y - baseRadius - 8f), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(primary, Offset(center.x - barW * 0.5f, center.y - baseRadius - 8f), Offset(center.x - barW * 0.5f + barW * ratio, center.y - baseRadius - 8f), strokeWidth = 3.5f, cap = StrokeCap.Round)
  }
}

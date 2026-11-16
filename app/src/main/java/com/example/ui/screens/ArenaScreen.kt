package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ArmoryItem
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import com.example.ui.rig.RigEvaluator
import com.example.ui.rig.drawArenaHostile
import com.example.ui.rig.drawRigCharacter
import com.example.ui.theme.AppIcons
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private data class ArenaHostile(
  var x: Float,
  var y: Float,
  var hp: Int,
  val maxHp: Int,
  val speed: Float,
  val isElite: Boolean = false,
)

private data class ArenaBolt(
  var x: Float,
  var y: Float,
  val vx: Float,
  val vy: Float,
  var life: Int,
  val isSupercharged: Boolean = false,
)

private data class ArenaIntel(
  val x: Float,
  val y: Float,
  var collected: Boolean = false,
)

private data class HitSpark(
  val x: Float,
  val y: Float,
  val color: Color,
  var life: Int,
)

@Composable
fun ArenaScreen(
  profile: OperatorProfile,
  armory: List<ArmoryItem>,
  rigs: List<RigAsset>,
  onSaveMatchAndExit: (score: Long, wave: Long, eliminations: Long, extracted: Boolean, durationSec: Long) -> Unit,
) {
  val world = UniverseCatalog.worldById(profile.activeUniverseId)
  val mode = UniverseCatalog.modeById(profile.activeMode)
  val console = UniverseCatalog.consoleById(profile.consolePreset)
  val activeRig = rigs.firstOrNull { it.id == profile.activeRigId } ?: rigs.firstOrNull()

  val equippedPerks = armory.filter { it.equipped && it.category == "perk" }
  val equippedPowers = armory.filter { it.equipped && it.category == "power" }
  val equippedUpgrades = armory.filter { it.equipped && it.category == "upgrade" }
  val equippedConsumables = armory.filter { it.equipped && it.category == "consumable" }

  val maxShield = 100 + equippedPerks.sumOf { (it.tier * 20).toInt() }
  val moveBoost = 1f + equippedPerks.count { it.name.contains("Speed", ignoreCase = true) } * 0.25f
  val multiShot = 1 + equippedUpgrades.sumOf { (it.tier / 2).toInt() }.coerceAtMost(2)
  val powerCooldownMax = (equippedPowers.firstOrNull()?.cooldownSec?.toInt() ?: 6) * 60

  var playerX by remember { mutableFloatStateOf(0.5f) }
  var playerY by remember { mutableFloatStateOf(0.56f) }
  var aimX by remember { mutableFloatStateOf(1f) }
  var aimY by remember { mutableFloatStateOf(0f) }
  var joyDx by remember { mutableFloatStateOf(0f) }
  var joyDy by remember { mutableFloatStateOf(0f) }

  var shield by remember(maxShield) { mutableIntStateOf(maxShield) }
  var score by remember { mutableLongStateOf(0L) }
  var wave by remember { mutableLongStateOf(1L) }
  var eliminations by remember { mutableLongStateOf(0L) }
  var powerCd by remember { mutableIntStateOf(0) }
  var consumableCd by remember { mutableIntStateOf(0) }
  var interactCd by remember { mutableIntStateOf(0) }
  var overdriveTicks by remember { mutableIntStateOf(0) }
  var stasisTicks by remember { mutableIntStateOf(0) }
  var damageFlashTicks by remember { mutableIntStateOf(0) }
  var pulseRingRadius by remember { mutableFloatStateOf(0f) }
  var brRingRadius by remember { mutableFloatStateOf(0.46f) }
  var intelCollected by remember { mutableIntStateOf(0) }
  var chevronsLocked by remember { mutableIntStateOf(1) }
  var statusCallout by remember { mutableStateOf<String?>(null) }
  var statusCalloutTicks by remember { mutableIntStateOf(0) }
  var frameTick by remember { mutableFloatStateOf(0f) }
  var elapsedTicks by remember { mutableLongStateOf(0L) }
  var runEnded by remember { mutableStateOf(false) }
  var extractedSuccess by remember { mutableStateOf(false) }

  val hostiles = remember { mutableStateListOf<ArenaHostile>() }
  val bolts = remember { mutableStateListOf<ArenaBolt>() }
  val intels = remember { mutableStateListOf<ArenaIntel>() }
  val sparks = remember { mutableStateListOf<HitSpark>() }

  val focusRequester = remember { FocusRequester() }

  fun postCallout(text: String) {
    statusCallout = text
    statusCalloutTicks = 110
  }

  fun spawnWave(w: Long) {
    val count = if (mode.id == "sandbox") 3 else (3 + w * 2).toInt().coerceAtMost(18)
    for (i in 0 until count) {
      val angle = Random.nextFloat() * 2f * PI.toFloat()
      val r = 0.42f
      val hp = 1 + (w.toInt() / 2)
      hostiles.add(
        ArenaHostile(
          x = (0.5f + cos(angle) * r).coerceIn(0.08f, 0.92f),
          y = (0.5f + sin(angle) * r).coerceIn(0.12f, 0.88f),
          hp = hp,
          maxHp = hp,
          speed = (0.0014f + w * 0.00022f).coerceAtMost(0.0042f),
          isElite = (i == 0 && w >= 2),
        )
      )
    }
    if (mode.id == "dmz" && intels.isEmpty()) {
      intels.add(ArenaIntel(0.22f, 0.28f))
      intels.add(ArenaIntel(0.78f, 0.30f))
      intels.add(ArenaIntel(0.75f, 0.78f))
    }
  }

  fun fireVolley() {
    if (runEnded) return
    val baseAngle = kotlin.math.atan2(aimY, aimX)
    val spread = 0.16f
    val shots = multiShot + if (overdriveTicks > 0) 2 else 0
    for (i in 0 until shots) {
      val offset = (i - (shots - 1) / 2f) * spread
      val a = baseAngle + offset
      bolts.add(
        ArenaBolt(
          x = playerX,
          y = playerY,
          vx = cos(a) * 0.019f,
          vy = sin(a) * 0.019f,
          life = 55,
          isSupercharged = overdriveTicks > 0,
        )
      )
    }
  }

  fun triggerPower() {
    if (runEnded || powerCd > 0) return
    powerCd = powerCooldownMax
    pulseRingRadius = 0.04f
    stasisTicks = if (world.id == "chrono_vault") 150 else 0
    val killRadius = 0.28f
    val iter = hostiles.iterator()
    while (iter.hasNext()) {
      val h = iter.next()
      if (hypot(h.x - playerX, h.y - playerY) <= killRadius) {
        h.hp -= 3
        sparks.add(HitSpark(h.x, h.y, Color(world.accentHex), 18))
        if (h.hp <= 0) {
          iter.remove()
          eliminations++
          score += 110L
        }
      }
    }
    postCallout(equippedPowers.firstOrNull()?.name?.uppercase() ?: "MULTIVERSE SURGE!")
  }

  fun triggerWorldInteract() {
    if (runEnded || interactCd > 0) return
    interactCd = 540
    overdriveTicks = 300
    pulseRingRadius = 0.05f
    when (world.id) {
      "chrono_vault" -> {
        chevronsLocked = (chevronsLocked % 7) + 1
        stasisTicks = 120
        postCallout("CHEVRON $chevronsLocked LOCKED • STASIS FIELD")
      }
      "sector_zero" -> {
        shield = (shield + 25).coerceAtMost(maxShield)
        postCallout("PACK-A-TIER WEAPON OVERDRIVE!")
      }
      "blacklight_conduit" -> {
        val targets = hostiles.take(4)
        targets.forEach { h ->
          h.hp -= 2
          sparks.add(HitSpark(h.x, h.y, Color(0xFF00E5FF), 22))
        }
        hostiles.removeAll { h ->
          if (h.hp <= 0) {
            eliminations++
            score += 90L
            true
          } else false
        }
        postCallout("LIBERTY ARC PYLON DISCHARGE!")
      }
      "hellbreaker_citadel" -> {
        shield = (shield + 35).coerceAtMost(maxShield)
        postCallout("CRUCIBLE CLAW BERSERK!")
      }
      else -> {
        postCallout("FORCE REPULSE + TWIN-SABER OVERDRIVE!")
      }
    }
  }

  fun triggerConsumable() {
    if (runEnded || consumableCd > 0) return
    consumableCd = 420
    shield = (shield + 55).coerceAtMost(maxShield)
    powerCd = 0
    postCallout("STIM CORE APPLIED (+55 SHIELD)")
  }

  fun triggerDash() {
    if (runEnded) return
    playerX = (playerX + aimX * 0.12f).coerceIn(0.06f, 0.94f)
    playerY = (playerY + aimY * 0.12f).coerceIn(0.10f, 0.90f)
  }

  LaunchedEffect(Unit) {
    spawnWave(1L)
    runCatching { focusRequester.requestFocus() }
    while (!runEnded) {
      delay(16L)
      elapsedTicks++
      val isMoving = hypot(joyDx, joyDy) > 0.08f
      frameTick = (frameTick + (if (isMoving) 1.35f else 0.7f)) % 60f
      if (powerCd > 0) powerCd--
      if (consumableCd > 0) consumableCd--
      if (interactCd > 0) interactCd--
      if (overdriveTicks > 0) overdriveTicks--
      if (stasisTicks > 0) stasisTicks--
      if (damageFlashTicks > 0) damageFlashTicks--
      if (statusCalloutTicks > 0) {
        statusCalloutTicks--
        if (statusCalloutTicks == 0) statusCallout = null
      }
      if (pulseRingRadius > 0f) {
        pulseRingRadius += 0.02f
        if (pulseRingRadius > 0.34f) pulseRingRadius = 0f
      }

      val sIter = sparks.iterator()
      while (sIter.hasNext()) {
        val sp = sIter.next()
        sp.life--
        if (sp.life <= 0) sIter.remove()
      }

      if (isMoving) {
        val len = hypot(joyDx, joyDy).coerceAtLeast(0.001f)
        aimX = joyDx / len
        aimY = joyDy / len
        playerX = (playerX + joyDx * 0.0064f * moveBoost).coerceIn(0.06f, 0.94f)
        playerY = (playerY + joyDy * 0.0064f * moveBoost).coerceIn(0.10f, 0.90f)
      }

      if (mode.id == "br" && elapsedTicks % 18L == 0L) {
        brRingRadius = (brRingRadius - 0.0015f).coerceAtLeast(0.16f)
        if (hypot(playerX - 0.5f, playerY - 0.5f) > brRingRadius) {
          shield = (shield - 2).coerceAtLeast(0)
          damageFlashTicks = 8
        }
      }

      val bIter = bolts.iterator()
      while (bIter.hasNext()) {
        val b = bIter.next()
        b.x += b.vx
        b.y += b.vy
        b.life--
        if (b.life <= 0 || b.x < 0f || b.x > 1f || b.y < 0f || b.y > 1f) {
          bIter.remove()
        }
      }

      if (mode.id == "dmz") {
        intels.forEach { intel ->
          if (!intel.collected && hypot(intel.x - playerX, intel.y - playerY) < 0.06f) {
            intel.collected = true
            intelCollected++
            score += 250L
            postCallout("INTEL SECURED ($intelCollected / 3)")
          }
        }
        if (intelCollected >= 3 && hypot(playerX - 0.5f, playerY - 0.16f) < 0.08f) {
          extractedSuccess = true
          runEnded = true
        }
      }

      val hIter = hostiles.iterator()
      while (hIter.hasNext()) {
        val h = hIter.next()
        if (stasisTicks == 0) {
          val dx = playerX - h.x
          val dy = playerY - h.y
          val d = hypot(dx, dy).coerceAtLeast(0.001f)
          h.x += (dx / d) * h.speed
          h.y += (dy / d) * h.speed
          if (d < 0.045f && elapsedTicks % 24L == 0L && mode.id != "sandbox") {
            shield = (shield - 10).coerceAtLeast(0)
            damageFlashTicks = 12
            if (shield <= 0) {
              runEnded = true
            }
          }
        }

        val hitBolt = bolts.firstOrNull { hypot(it.x - h.x, it.y - h.y) < 0.048f }
        if (hitBolt != null) {
          val dmg = if (hitBolt.isSupercharged) 2 else 1
          hitBolt.life = 0
          h.hp -= dmg
          sparks.add(HitSpark(h.x, h.y, Color(world.secondaryHex), 14))
          if (h.hp <= 0) {
            hIter.remove()
            eliminations++
            score += if (h.isElite) 150L else 60L
          }
        }
      }

      if (hostiles.isEmpty() && !runEnded) {
        wave++
        score += wave * 100L
        if (world.id == "chrono_vault") chevronsLocked = ((wave.toInt() - 1) % 7) + 1
        postCallout("ROUND ${formatRoundRoman(wave)} INCOMING")
        spawnWave(wave)
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF060911))
      .focusRequester(focusRequester)
      .onPreviewKeyEvent { keyEvent ->
        if (keyEvent.type == KeyEventType.KeyDown) {
          when (keyEvent.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> { joyDy = -1f; true }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> { joyDy = 1f; true }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> { joyDx = -1f; true }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> { joyDx = 1f; true }
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_BUTTON_A -> { fireVolley(); true }
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_BUTTON_X -> { triggerPower(); true }
            KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_BUTTON_Y -> { triggerConsumable(); true }
            KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_BUTTON_L1 -> { triggerWorldInteract(); true }
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_BUTTON_B -> { triggerDash(); true }
            else -> false
          }
        } else if (keyEvent.type == KeyEventType.KeyUp) {
          when (keyEvent.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_S -> { joyDy = 0f; true }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_D -> { joyDx = 0f; true }
            else -> false
          }
        } else false
      }
  ) {
    // Tactical Zombies / Division / Console HUD Top Bar
    Surface(
      color = Color(0xFF0A101E),
      modifier = Modifier.fillMaxWidth().border(1.dp, Color(world.accentHex).copy(alpha = 0.45f)),
    ) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Classic glowing Round / Wave Tally Badge
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0x33FF1744),
              modifier = Modifier.border(1.5.dp, Color(0xFFFF3D71), RoundedCornerShape(8.dp)),
            ) {
              Text(
                text = "RND ${formatRoundRoman(wave)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5252),
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
              )
            }
            Column {
              Text(
                text = "${world.title.substringBefore(":")} • ${mode.shortTag}",
                style = MaterialTheme.typography.titleMedium,
                color = Color(world.accentHex),
              )
              Text(
                text = "SCORE $score • ELIMS $eliminations • SHD ARMOR $shield/$maxShield",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
              )
            }
          }
          OutlinedButton(
            onClick = {
              runEnded = true
              onSaveMatchAndExit(score, wave, eliminations, extractedSuccess || mode.id == "sandbox", elapsedTicks / 60L)
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("extract_exit_arena_btn"),
          ) {
            Icon(AppIcons.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Exfil")
          }
        }

        // Segmented Shield Bar + Active Perks Strip
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier = Modifier
              .weight(1f)
              .height(7.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xFF1C2742))
          ) {
            val pct = (shield.toFloat() / maxShield.coerceAtLeast(1)).coerceIn(0f, 1f)
            Box(
              modifier = Modifier
                .fillMaxWidth(pct)
                .height(7.dp)
                .background(if (overdriveTicks > 0) Color(0xFFFFD54F) else Color(world.accentHex))
            )
          }
          if (equippedPerks.isNotEmpty()) {
            Text(
              text = equippedPerks.take(2).joinToString(" • ") { it.name.substringBefore(" ") },
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF00E676),
            )
          }
        }
      }
    }

    // Playable Multiverse Arena Canvas
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawWorldArenaBackdrop(
          worldId = world.id,
          accentColor = Color(world.accentHex),
          secondaryColor = Color(world.secondaryHex),
          elapsedTicks = elapsedTicks,
          chevronsLocked = chevronsLocked,
          overdriveActive = overdriveTicks > 0,
        )

        if (mode.id == "br") {
          drawCircle(
            color = Color(0xFFFF3D71),
            radius = brRingRadius * minOf(w, h),
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = 4f),
          )
        }

        if (mode.id == "dmz") {
          val exfilReady = intelCollected >= 3
          drawCircle(
            color = if (exfilReady) Color(0xFF00E676) else Color(0xFFFFAB40).copy(alpha = 0.45f),
            radius = 30f,
            center = Offset(w * 0.5f, h * 0.16f),
            style = Stroke(width = 4f),
          )
          intels.filter { !it.collected }.forEach { intel ->
            drawCircle(Color(0xFFFFD54F).copy(alpha = 0.25f), radius = 20f, center = Offset(intel.x * w, intel.y * h))
            drawCircle(Color(0xFFFFD54F), radius = 11f, center = Offset(intel.x * w, intel.y * h))
          }
        }

        if (pulseRingRadius > 0f) {
          drawCircle(
            color = Color(world.accentHex).copy(alpha = 0.65f),
            radius = pulseRingRadius * minOf(w, h),
            center = Offset(playerX * w, playerY * h),
            style = Stroke(width = 7f),
          )
        }

        // Laser / Plasma Tracer Bolts
        bolts.forEach { b ->
          val head = Offset(b.x * w, b.y * h)
          val tail = Offset((b.x - b.vx * 2.2f) * w, (b.y - b.vy * 2.2f) * h)
          val tracerCol = if (b.isSupercharged) Color(0xFFFF4081) else Color(world.accentHex)
          drawLine(tracerCol.copy(alpha = 0.55f), tail, head, strokeWidth = if (b.isSupercharged) 10f else 6f, cap = StrokeCap.Round)
          drawLine(Color.White, tail, head, strokeWidth = 2.6f, cap = StrokeCap.Round)
        }

        // Impact Sparks
        sparks.forEach { sp ->
          val r = (18 - sp.life).coerceAtLeast(3) * 1.8f
          drawCircle(sp.color.copy(alpha = (sp.life / 18f).coerceIn(0f, 1f)), radius = r, center = Offset(sp.x * w, sp.y * h), style = Stroke(width = 2.5f))
        }

        // Animated Enemy Silhouettes
        hostiles.forEachIndexed { idx, e ->
          drawArenaHostile(
            center = Offset(e.x * w, e.y * h),
            worldId = world.id,
            isElite = e.isElite,
            stasisActive = stasisTicks > 0,
            hp = e.hp,
            maxHp = e.maxHp,
            animPhase = elapsedTicks * 0.14f + idx,
          )
        }

        // Player Custom Rig Character rendered live!
        val playerClip = if (hypot(joyDx, joyDy) > 0.08f) "sprint" else (activeRig?.activeClip ?: "idle")
        val skel = RigEvaluator.evaluate(
          origin = Offset(playerX * w, playerY * h),
          scalePx = 70f * (activeRig?.rigScale?.toFloat() ?: 1f),
          poseAngles = activeRig?.poseAngles ?: UniverseCatalog.defaultPoseMap(),
          clip = playerClip,
          frame = frameTick,
          ikEnabled = activeRig?.ikEnabled ?: true,
        )
        drawRigCharacter(
          skeleton = skel,
          archetype = activeRig?.archetype ?: "operator",
          primaryColor = Color(world.accentHex),
          secondaryColor = Color(world.secondaryHex),
          showBonesOverlay = false,
        )

        // Corner Tactical Mini-Radar
        val radarR = 38f
        val radarCenter = Offset(w - radarR - 14f, radarR + 14f)
        drawCircle(Color(0xBB080D1A), radius = radarR, center = radarCenter)
        drawCircle(Color(world.accentHex).copy(alpha = 0.65f), radius = radarR, center = radarCenter, style = Stroke(width = 2f))
        drawCircle(Color(0xFF00E676), radius = 3.5f, center = Offset(radarCenter.x + (playerX - 0.5f) * radarR * 1.6f, radarCenter.y + (playerY - 0.5f) * radarR * 1.6f))
        hostiles.forEach { e ->
          drawCircle(
            color = if (e.isElite) Color(0xFFFF1744) else Color(0xFFFFAB40),
            radius = 2.5f,
            center = Offset(radarCenter.x + (e.x - 0.5f) * radarR * 1.6f, radarCenter.y + (e.y - 0.5f) * radarR * 1.6f),
          )
        }

        // Damage Screen-Edge Flash
        if (damageFlashTicks > 0) {
          drawRect(
            color = Color(0x55FF1744),
            topLeft = Offset.Zero,
            size = Size(w, h),
            style = Stroke(width = 22f),
          )
        }
      }

      if (statusCallout != null || mode.id == "dmz") {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xD80A101E),
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 8.dp)
            .border(1.dp, Color(world.accentHex).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
        ) {
          Text(
            text = statusCallout
              ?: if (intelCollected >= 3) "EXFIL READY — Move to Top Beacon!" else "DMZ Intel Cores: $intelCollected / 3",
            style = MaterialTheme.typography.labelMedium,
            color = if (overdriveTicks > 0 || intelCollected >= 3) Color(0xFF00E676) else Color(world.accentHex),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }

      if (runEnded) {
        Surface(
          color = Color(0xE6070A12),
          modifier = Modifier.fillMaxSize(),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(
              text = if (extractedSuccess) "EXFILTRATION COMPLETE" else "DEPLOYMENT ENDED",
              style = MaterialTheme.typography.displaySmall,
              color = if (extractedSuccess) Color(0xFF00E676) else Color(world.accentHex),
            )
            Spacer(Modifier.height(8.dp))
            Text(
              text = "Score: $score • Round $wave • $eliminations Eliminations",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
            )
            Spacer(Modifier.height(18.dp))
            Button(
              onClick = {
                onSaveMatchAndExit(score, wave, eliminations, extractedSuccess, elapsedTicks / 60L)
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.height(52.dp).testTag("save_match_result_btn"),
            ) {
              Text("Claim Essence & Save Run", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    }

    // Bottom Gamepad / Touch Controls Deck
    Surface(
      color = Color(0xFF0A101E),
      modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .background(Color(0xFF141E36))
            .border(1.5.dp, Color(world.accentHex).copy(alpha = 0.65f), CircleShape)
            .pointerInput(Unit) {
              detectDragGestures(
                onDragEnd = { joyDx = 0f; joyDy = 0f },
                onDragCancel = { joyDx = 0f; joyDy = 0f },
                onDrag = { change, _ ->
                  change.consume()
                  val cx = size.width / 2f
                  val cy = size.height / 2f
                  joyDx = ((change.position.x - cx) / cx).coerceIn(-1f, 1f)
                  joyDy = ((change.position.y - cy) / cy).coerceIn(-1f, 1f)
                },
              )
            },
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(Color(world.accentHex))
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${console.buttonLegend.take(2).joinToString(" • ")}" + if (overdriveTicks > 0) " • OVERDRIVE" else "",
            style = MaterialTheme.typography.labelSmall,
            color = if (overdriveTicks > 0) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { fireVolley() },
              colors = ButtonDefaults.buttonColors(containerColor = Color(world.accentHex), contentColor = Color.Black),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("arena_fire_btn"),
            ) {
              Icon(AppIcons.MyLocation, contentDescription = "Fire", modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("FIRE")
            }
            Button(
              onClick = { triggerPower() },
              enabled = powerCd == 0,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("arena_power_btn"),
            ) {
              Icon(AppIcons.FlashOn, contentDescription = "Power", modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text(if (powerCd == 0) "POWER" else "${(powerCd / 60) + 1}s")
            }
          }
          Spacer(Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = { triggerWorldInteract() },
              enabled = interactCd == 0,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(42.dp).testTag("arena_interact_btn"),
            ) {
              Text(if (interactCd == 0) world.interactActionLabel else "${(interactCd / 60) + 1}s", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = { triggerConsumable() },
              enabled = consumableCd == 0 && equippedConsumables.isNotEmpty(),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(42.dp).testTag("arena_consumable_btn"),
            ) {
              Icon(AppIcons.MedicalServices, contentDescription = "Consumable", modifier = Modifier.size(15.dp))
              Spacer(Modifier.width(3.dp))
              Text(if (consumableCd == 0) "STIM" else "${(consumableCd / 60) + 1}s", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = { triggerDash() },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(42.dp).testTag("arena_dash_btn"),
            ) {
              Icon(AppIcons.Bolt, contentDescription = "Dash", modifier = Modifier.size(15.dp))
              Spacer(Modifier.width(3.dp))
              Text("DASH", style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }
  }
}

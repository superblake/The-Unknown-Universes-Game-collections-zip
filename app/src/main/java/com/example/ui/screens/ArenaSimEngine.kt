package com.example.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.data.local.AppSettings
import com.example.data.local.ParticleDensity
import com.example.data.model.ArmoryItem
import com.example.data.model.UniverseWorld
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class ArenaDropKind(val badge: String, val label: String, val color: Color) {
  DOUBLE_ESSENCE("2X", "DOUBLE ESSENCE (2X)", Color(0xFFFFD54F)),
  INSTA_KILL("IK", "INSTA-KILL ACTIVE!", Color(0xFFFF3D71)),
  NUKE("NK", "TACTICAL NUKE DETONATED!", Color(0xFF00E5FF)),
  MAX_ARMOR("MX", "MAX ARMOR REFILLED!", Color(0xFF00E676)),
}

data class ArenaHostile(
  var x: Float,
  var y: Float,
  var hp: Int,
  val maxHp: Int,
  val speed: Float,
  val isElite: Boolean = false,
  val isBoss: Boolean = false,
  val archetype: String = "standard",
  val bossName: String = "",
  var rangedCooldownTicks: Float = 90f,
)

data class ArenaBolt(
  var x: Float,
  var y: Float,
  val vx: Float,
  val vy: Float,
  var life: Float,
  val isSupercharged: Boolean = false,
  val isEnemyShot: Boolean = false,
  var pierceLeft: Int = 0,
  val damage: Int = 1,
  val color: Color = Color(0xFF00E5FF),
)

data class ArenaIntel(
  val x: Float,
  val y: Float,
  var collected: Boolean = false,
)

data class ArenaDrop(
  val x: Float,
  val y: Float,
  val kind: ArenaDropKind,
  var life: Float = 660f,
)

data class HitSpark(
  val x: Float,
  val y: Float,
  val color: Color,
  var life: Float,
  val maxLife: Float = 18f,
)

data class ArenaArcLine(
  val x1: Float,
  val y1: Float,
  val x2: Float,
  val y2: Float,
  val color: Color,
  var life: Float = 14f,
)

data class ArenaLoadoutStats(
  val maxShield: Int,
  val moveBoost: Float,
  val multiShot: Int,
  val initialPackTier: Int,
  val hasClawEdge: Boolean,
  val clawTier: Int,
  val powerCooldownTicks: Float,
  val activePowerId: String,
  val activePowerName: String,
  val activePowerColor: Color,
  val activePowerRadius: Float,
  val activePowerDamage: Int,
  val hasTendrilPower: Boolean,
  val hasRepulsePower: Boolean,
  val hasChevronPower: Boolean,
  val passiveRegenPerSec: Float,
  val consumableHeal: Int,
  val consumableCooldownTicks: Float,
  val fireCadenceTicks: Float,
  val activePerksSummary: String,
) {
  companion object {
    fun fromArmory(armory: List<ArmoryItem>, fallbackAccent: Color): ArenaLoadoutStats {
      val equipped = armory.filter { it.equipped }
      val perks = equipped.filter { it.category == "perk" }
      val powers = equipped.filter { it.category == "power" }
      val upgrades = equipped.filter { it.category == "upgrade" }
      val consumables = equipped.filter { it.category == "consumable" }

      val juggBonus = perks.sumOf { p ->
        if (p.id == "perk_juggernaut" || p.name.contains("Juggernaut", ignoreCase = true) || p.name.contains("Aegis", ignoreCase = true)) {
          (p.tier * 40).toInt()
        } else {
          (p.tier * 20).toInt()
        }
      }
      val maxShield = 100 + juggBonus
      val speedPerkCount = perks.sumOf { p ->
        if (p.id == "perk_overdrive" || p.name.contains("Speed", ignoreCase = true) || p.name.contains("Stamina", ignoreCase = true)) {
          p.tier.toInt()
        } else 0
      }
      val moveBoost = (1f + speedPerkCount * 0.14f + (perks.size * 0.05f)).coerceAtMost(1.85f)
      val multiShot = (1 + upgrades.sumOf { (it.tier / 2).toInt().coerceAtLeast(1) }).coerceAtMost(4)
      val packItem = upgrades.firstOrNull { it.id == "upgrade_packtier" || it.name.contains("Pack", ignoreCase = true) }
      val initialPackTier = when {
        packItem == null -> 1
        packItem.tier >= 4 -> 3
        packItem.tier >= 2 -> 2
        else -> 1
      }
      val clawItem = upgrades.firstOrNull { it.id == "upgrade_claws" || it.name.contains("Claw", ignoreCase = true) || it.name.contains("Melee", ignoreCase = true) }
      val primaryPower = powers.firstOrNull()
      val powerCdSec = (primaryPower?.cooldownSec?.toFloat() ?: 6f).coerceIn(2f, 30f)
      val customBonusDmg = ((primaryPower?.powerValue ?: 180L) / 70L).toInt().coerceIn(2, 8)
      val powerTierBonus = ((primaryPower?.tier ?: 1L) - 1L).toInt()
      val powerColor = primaryPower?.accentColorHex?.let { parseHexSafe(it, fallbackAccent) } ?: fallbackAccent

      val primaryConsumable = consumables.firstOrNull()
      val cHeal = (45 + ((primaryConsumable?.tier ?: 1L) * 18L) + ((primaryConsumable?.powerValue ?: 100L) / 15L)).toInt().coerceAtMost(220)
      val cCdSec = (primaryConsumable?.cooldownSec?.toFloat() ?: 12f).coerceIn(4f, 35f)
      val fireCadence = (11.5f - speedPerkCount * 1.1f).coerceIn(5.5f, 12f)

      return ArenaLoadoutStats(
        maxShield = maxShield,
        moveBoost = moveBoost,
        multiShot = multiShot,
        initialPackTier = initialPackTier,
        hasClawEdge = clawItem != null,
        clawTier = clawItem?.tier?.toInt() ?: 0,
        powerCooldownTicks = powerCdSec * 60f,
        activePowerId = primaryPower?.id ?: "surge",
        activePowerName = primaryPower?.name?.uppercase() ?: "MULTIVERSE SURGE",
        activePowerColor = powerColor,
        activePowerRadius = (0.27f + powerTierBonus * 0.03f).coerceAtMost(0.42f),
        activePowerDamage = customBonusDmg + powerTierBonus,
        hasTendrilPower = powers.any { it.id == "power_tendril" || it.name.contains("Tendril", ignoreCase = true) || it.name.contains("Bio", ignoreCase = true) },
        hasRepulsePower = powers.any { it.id == "power_repulse" || it.name.contains("Repulse", ignoreCase = true) || it.name.contains("Force", ignoreCase = true) },
        hasChevronPower = powers.any { it.id == "power_chevron" || it.name.contains("Chevron", ignoreCase = true) || it.name.contains("Chrono", ignoreCase = true) || it.name.contains("Stasis", ignoreCase = true) },
        passiveRegenPerSec = 1.5f + perks.size * 1.0f,
        consumableHeal = cHeal,
        consumableCooldownTicks = cCdSec * 60f,
        fireCadenceTicks = fireCadence,
        activePerksSummary = perks.take(2).joinToString(" • ") { it.name.substringBefore(" ") },
      )
    }
  }
}

@Stable
class ArenaSimState(
  val world: UniverseWorld,
  val modeId: String,
  val loadout: ArenaLoadoutStats,
  val hasConsumableEquipped: Boolean,
  initialSettings: AppSettings = AppSettings(),
) {
  var liveSettings: AppSettings = initialSettings
  // High-frequency Canvas state (read ONLY inside Canvas drawScope / simulation loop)
  var renderNonce by mutableLongStateOf(0L)
  var playerX = 0.5f
  var playerY = 0.58f
  var aimX = 1f
  var aimY = 0f
  var joyDx = 0f
  var joyDy = 0f
  var touchAimActive = false
  var touchAimX = 0.5f
  var touchAimY = 0.4f

  var elapsedTicks = 0L
  var frameTick = 0f
  var fireAutoTicker = 0f
  var powerCd = 0f
  var consumableCd = 0f
  var interactCd = 0f
  var dashCd = 0f
  var dashInvulnTicks = 0f
  var overdriveTicks = 0f
  var stasisTicks = 0f
  var doubleEssenceTicks = 0f
  var instaKillTicks = 0f
  var damageFlashTicks = 0f
  var pulseRingRadius = 0f
  var brRingRadius = 0.46f
  var exfilHoldTicks = 0f
  var calloutTicks = 0f
  var regenAccum = 0f
  var brDamageAccum = 0f
  var meleeDamageAccum = 0f

  val hostiles = ArrayList<ArenaHostile>(24)
  val bolts = ArrayList<ArenaBolt>(48)
  val intels = ArrayList<ArenaIntel>(4)
  val drops = ArrayList<ArenaDrop>(8)
  val sparks = ArrayList<HitSpark>(32)
  val arcs = ArrayList<ArenaArcLine>(16)

  // Low-frequency HUD Snapshot States (Compose HUD recomposes ONLY when discrete values change!)
  var hudShield by mutableIntStateOf(loadout.maxShield)
  var hudScore by mutableLongStateOf(0L)
  var hudWave by mutableLongStateOf(1L)
  var hudElims by mutableLongStateOf(0L)
  var hudPackTier by mutableIntStateOf(loadout.initialPackTier)
  var hudIntelCollected by mutableIntStateOf(0)
  var hudChevronsLocked by mutableIntStateOf(1)
  var hudPowerCdSec by mutableIntStateOf(0)
  var hudConsumableCdSec by mutableIntStateOf(0)
  var hudInteractCdSec by mutableIntStateOf(0)
  var hudDashReady by mutableStateOf(true)
  var hudOverdriveActive by mutableStateOf(false)
  var hudActiveDropText by mutableStateOf<String?>(null)
  var hudCallout by mutableStateOf<String?>(null)
  var hudBossName by mutableStateOf<String?>(null)
  var hudBossHpRatio by mutableFloatStateOf(0f)
  var hudExfilRatio by mutableFloatStateOf(0f)
  var hudAutoFire by mutableStateOf(initialSettings.autoFireEnabled)
  var hudRunEnded by mutableStateOf(false)
  var hudExtractedSuccess by mutableStateOf(false)

  private val accentColor = Color(world.accentHex)
  private val secondaryColor = Color(world.secondaryHex)

  init {
    spawnWave(1L)
  }

  fun postCallout(text: String) {
    hudCallout = text
    calloutTicks = 120f
  }

  private fun addSparkIfAllowed(x: Float, y: Float, color: Color, life: Float) {
    val cap = when {
      liveSettings.particleDensity == ParticleDensity.OFF -> 0
      liveSettings.reduceMotion || liveSettings.particleDensity == ParticleDensity.REDUCED -> 8
      else -> 32
    }
    if (cap <= 0 || sparks.size >= cap) return
    sparks.add(HitSpark(x, y, color, life))
  }

  fun spawnWave(w: Long) {
    val count = if (modeId == "sandbox") 4 else (3 + w * 2).toInt().coerceAtMost(18)
    for (i in 0 until count) {
      val angle = Random.nextFloat() * 2f * PI.toFloat()
      val r = 0.43f
      val isElite = (i == 0 && w >= 2)
      val archetype = chooseArchetype(world.id, w, i)
      val baseHp = 1 + (w.toInt() / 2) + if (archetype == "mech") 2 else if (isElite) 2 else 0
      val spd = when (archetype) {
        "rusher" -> (0.0023f + w * 0.00024f).coerceAtMost(0.0052f)
        "mech" -> (0.0010f + w * 0.00014f).coerceAtMost(0.0026f)
        else -> (0.0014f + w * 0.00020f).coerceAtMost(0.0040f)
      }
      hostiles.add(
        ArenaHostile(
          x = (0.5f + cos(angle) * r).coerceIn(0.07f, 0.93f),
          y = (0.5f + sin(angle) * r).coerceIn(0.11f, 0.89f),
          hp = baseHp,
          maxHp = baseHp,
          speed = spd,
          isElite = isElite,
          archetype = archetype,
          rangedCooldownTicks = 85f + Random.nextFloat() * 80f,
        )
      )
    }

    // Spawn Creator Lore Boss every 3rd wave (or on wave 4 in BR)
    if (w % 3L == 0L || (modeId == "br" && w == 4L)) {
      spawnBossForWorld(w)
    }

    if (modeId == "dmz" && intels.isEmpty()) {
      intels.add(ArenaIntel(0.20f, 0.28f))
      intels.add(ArenaIntel(0.80f, 0.30f))
      intels.add(ArenaIntel(0.76f, 0.78f))
    }
  }

  fun spawnBossForWorld(w: Long) {
    val (arch, name) = when (world.id) {
      "blacklight_conduit" -> "boss_hydra" to "WATER HYDRA ALPHA"
      "hellbreaker_citadel" -> if (w % 2L == 0L) "boss_mech" to "ASSAULT MECH TITAN" else "boss_croc" to "KILLER CROC JUGGERNAUT"
      "aegis_starblade" -> "boss_croc" to "BLACK GATE KILLER CROC"
      "chrono_vault" -> "boss_hydra" to "CHRONO RIFT WARDEN"
      else -> "boss_mech" to "GENTEK CONTAINMENT MECH"
    }
    val hp = 12 + w.toInt() * 4
    hostiles.add(
      ArenaHostile(
        x = 0.5f,
        y = 0.16f,
        hp = hp,
        maxHp = hp,
        speed = 0.00125f,
        isElite = true,
        isBoss = true,
        archetype = arch,
        bossName = name,
        rangedCooldownTicks = 65f,
      )
    )
    postCallout("BOSS INBOUND: $name!")
  }

  private fun chooseArchetype(worldId: String, wave: Long, index: Int): String =
    when (worldId) {
      "blacklight_conduit" -> when {
        index % 4 == 1 -> "trooper" // Blackwatch / DUP containment trooper
        index % 4 == 2 -> "slasher" // Necromorph anomaly
        index % 4 == 3 && wave >= 2 -> "rusher"
        else -> "standard"
      }
      "hellbreaker_citadel" -> when {
        index % 3 == 1 -> "slasher"
        index % 4 == 2 -> "mech"
        else -> "standard"
      }
      "chrono_vault" -> when {
        index % 3 == 1 -> "trooper"
        index % 4 == 2 -> "slasher"
        else -> "standard"
      }
      "aegis_starblade" -> when {
        index % 3 == 1 -> "trooper"
        index % 4 == 3 -> "mech"
        else -> "rusher"
      }
      else -> when {
        wave >= 2 && index % 4 == 1 -> "rusher" // Hellhound / Sprinter
        index % 4 == 2 -> "trooper" // Blackwatch / Rogue agent
        else -> "standard" // Quarantine Zombie
      }
    }

  private fun autoAimTowardNearestIfNeeded() {
    if (!liveSettings.autoAimEnabled || touchAimActive || hostiles.isEmpty()) return
    // Smart nearest-target lock whenever player is firing/standing or backing away
    var best: ArenaHostile? = null
    var bestDistSq = Float.MAX_VALUE
    for (i in 0 until hostiles.size) {
      val h = hostiles[i]
      val dx = h.x - playerX
      val dy = h.y - playerY
      val dSq = dx * dx + dy * dy
      if (dSq < bestDistSq) {
        bestDistSq = dSq
        best = h
      }
    }
    if (best != null) {
      val dx = best.x - playerX
      val dy = best.y - playerY
      val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
      aimX = dx / d
      aimY = dy / d
    }
  }

  fun setTouchAim(normX: Float, normY: Float, active: Boolean) {
    touchAimActive = active
    touchAimX = normX.coerceIn(0f, 1f)
    touchAimY = normY.coerceIn(0f, 1f)
    if (active) {
      val dx = touchAimX - playerX
      val dy = touchAimY - playerY
      val d = sqrt(dx * dx + dy * dy)
      if (d > 0.01f) {
        aimX = dx / d
        aimY = dy / d
      }
    }
  }

  fun fireVolley(forceManual: Boolean = false) {
    if (hudRunEnded) return
    if (!touchAimActive) {
      autoAimTowardNearestIfNeeded()
    }
    val baseAngle = atan2(aimY, aimX)
    val packLevel = hudPackTier.coerceIn(1, 3)
    val shots = (loadout.multiShot + (packLevel - 1) + if (overdriveTicks > 0f) 1 else 0).coerceAtMost(6)
    val spread = if (shots > 4) 0.13f else 0.16f
    val dmg = packLevel + if (overdriveTicks > 0f) 1 else 0
    val pierce = (packLevel - 1).coerceAtLeast(0)
    val boltColor = when {
      instaKillTicks > 0f -> Color(0xFFFF3D71)
      packLevel >= 3 -> Color(0xFFFFD54F)
      packLevel == 2 -> Color(0xFFE040FB)
      overdriveTicks > 0f -> Color(0xFFFFAB40)
      else -> accentColor
    }
    for (i in 0 until shots) {
      val offset = (i - (shots - 1) / 2f) * spread
      val a = baseAngle + offset
      bolts.add(
        ArenaBolt(
          x = playerX,
          y = playerY,
          vx = cos(a) * (0.019f + packLevel * 0.002f),
          vy = sin(a) * (0.019f + packLevel * 0.002f),
          life = 56f,
          isSupercharged = overdriveTicks > 0f || packLevel >= 2,
          pierceLeft = pierce,
          damage = dmg,
          color = boltColor,
        )
      )
    }
    if (forceManual) {
      fireAutoTicker = loadout.fireCadenceTicks * 0.6f
    }
  }

  fun triggerPower() {
    if (hudRunEnded || powerCd > 0f) return
    powerCd = loadout.powerCooldownTicks
    pulseRingRadius = 0.04f

    val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L
    val killRadiusSq = loadout.activePowerRadius * loadout.activePowerRadius
    val baseDmg = loadout.activePowerDamage

    // 1. Chrono-Stasis freezes entire wave if Chevron/Chrono power or Chrono-Vault
    if (loadout.hasChevronPower || world.id == "chrono_vault") {
      stasisTicks = 210f
    }

    // 2. Force Repulse knocks all hostiles away + clears enemy projectiles + radial Saber Throw
    if (loadout.hasRepulsePower || world.id == "aegis_starblade") {
      bolts.removeAll { it.isEnemyShot }
      for (i in 0 until 8) {
        val a = i * (2f * PI.toFloat() / 8f)
        bolts.add(
          ArenaBolt(
            x = playerX,
            y = playerY,
            vx = cos(a) * 0.022f,
            vy = sin(a) * 0.022f,
            life = 45f,
            isSupercharged = true,
            pierceLeft = 1,
            damage = 2,
            color = Color(0xFF00E5FF),
          )
        )
      }
      for (i in 0 until hostiles.size) {
        val h = hostiles[i]
        val dx = h.x - playerX
        val dy = h.y - playerY
        val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
        if (d < 0.38f) {
          h.x = (h.x + (dx / d) * 0.14f).coerceIn(0.06f, 0.94f)
          h.y = (h.y + (dy / d) * 0.14f).coerceIn(0.10f, 0.90f)
        }
      }
    }

    // 3. Blacklight Tendril chains lightning/crimson tendrils to nearby hostiles
    if (loadout.hasTendrilPower || world.id == "blacklight_conduit") {
      var chained = 0
      for (i in 0 until hostiles.size) {
        if (chained >= 6) break
        val h = hostiles[i]
        val dx = h.x - playerX
        val dy = h.y - playerY
        if (dx * dx + dy * dy <= 0.42f * 0.42f) {
          arcs.add(ArenaArcLine(playerX, playerY, h.x, h.y, loadout.activePowerColor, 18f))
          chained++
        }
      }
    }

    // Area impact damage
    val iter = hostiles.iterator()
    while (iter.hasNext()) {
      val h = iter.next()
      val dx = h.x - playerX
      val dy = h.y - playerY
      if (dx * dx + dy * dy <= killRadiusSq) {
        h.hp -= baseDmg
        addSparkIfAllowed(h.x, h.y, loadout.activePowerColor, 18f)
        if (h.hp <= 0) {
          iter.remove()
          onHostileDefeated(h, scoreMult)
        }
      }
    }
    postCallout("${loadout.activePowerName}!")
  }

  fun triggerWorldInteract() {
    if (hudRunEnded || interactCd > 0f) return
    interactCd = 420f
    overdriveTicks = 300f
    pulseRingRadius = 0.05f
    val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L

    when (world.id) {
      "chrono_vault" -> {
        val nextChevron = (hudChevronsLocked % 7) + 1
        hudChevronsLocked = nextChevron
        stasisTicks = 150f
        if (nextChevron == 7) {
          // 7-Chevron Wormhole Event Horizon Shockwave!
          val iter = hostiles.iterator()
          while (iter.hasNext()) {
            val h = iter.next()
            h.hp -= if (h.isBoss) 6 else 99
            arcs.add(ArenaArcLine(0.5f, 0.30f, h.x, h.y, Color(0xFF00E5FF), 22f))
            if (h.hp <= 0) {
              iter.remove()
              onHostileDefeated(h, scoreMult)
            }
          }
          postCallout("CHEVRON 7 LOCKED! EVENT HORIZON OPEN")
        } else {
          postCallout("CHEVRON $nextChevron/7 LOCKED • STASIS FIELD")
        }
      }
      "sector_zero" -> {
        if (hudPackTier < 3) {
          hudPackTier++
          postCallout("PACK-A-TIER ${formatRoundRoman(hudPackTier.toLong())} UPGRADED!")
        } else {
          hudShield = (hudShield + 40).coerceAtMost(loadout.maxShield)
          postCallout("MYSTERY CACHE OVERDRIVE! (+40 ARMOR)")
        }
      }
      "blacklight_conduit" -> {
        val limit = minOf(6, hostiles.size)
        for (i in 0 until limit) {
          val h = hostiles[i]
          h.hp -= 3
          arcs.add(ArenaArcLine(0.80f, 0.76f, h.x, h.y, Color(0xFF00E5FF), 20f))
          addSparkIfAllowed(h.x, h.y, Color(0xFF00E5FF), 22f)
        }
        val iter = hostiles.iterator()
        while (iter.hasNext()) {
          val h = iter.next()
          if (h.hp <= 0) {
            iter.remove()
            onHostileDefeated(h, scoreMult)
          }
        }
        postCallout("LIBERTY ARC PYLON DISCHARGE!")
      }
      "hellbreaker_citadel" -> {
        hudShield = (hudShield + 45).coerceAtMost(loadout.maxShield)
        instaKillTicks = maxOf(instaKillTicks, 180f)
        postCallout("CRUCIBLE FURY: GLORY SIPHON!")
      }
      else -> {
        bolts.removeAll { it.isEnemyShot }
        for (i in 0 until hostiles.size) {
          val h = hostiles[i]
          val dx = h.x - playerX
          val dy = h.y - playerY
          val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
          h.x = (h.x + (dx / d) * 0.16f).coerceIn(0.06f, 0.94f)
          h.y = (h.y + (dy / d) * 0.16f).coerceIn(0.10f, 0.90f)
        }
        postCallout("SHIP OF HEROES FORCE REPULSE!")
      }
    }
  }

  fun triggerConsumable() {
    if (hudRunEnded || !hasConsumableEquipped || consumableCd > 0f) return
    consumableCd = loadout.consumableCooldownTicks
    hudShield = (hudShield + loadout.consumableHeal).coerceAtMost(loadout.maxShield)
    powerCd = 0f
    overdriveTicks = maxOf(overdriveTicks, 180f)
    pulseRingRadius = 0.04f
    postCallout("OMNI-SURGE: +${loadout.consumableHeal} ARMOR & CD RESET")
  }

  fun triggerDash() {
    if (hudRunEnded || dashCd > 0f) return
    dashCd = if (loadout.hasClawEdge) 40f else 50f
    dashInvulnTicks = 24f
    val oldX = playerX
    val oldY = playerY
    playerX = (playerX + aimX * 0.14f).coerceIn(0.06f, 0.94f)
    playerY = (playerY + aimY * 0.14f).coerceIn(0.10f, 0.90f)
    arcs.add(ArenaArcLine(oldX, oldY, playerX, playerY, accentColor, 14f))

    if (loadout.hasClawEdge) {
      val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L
      val clawDmg = 2 + loadout.clawTier
      val iter = hostiles.iterator()
      while (iter.hasNext()) {
        val h = iter.next()
        val dx = h.x - playerX
        val dy = h.y - playerY
        if (dx * dx + dy * dy <= 0.15f * 0.15f) {
          h.hp -= clawDmg
          addSparkIfAllowed(h.x, h.y, Color(0xFFFF5252), 18f)
          if (h.hp <= 0) {
            iter.remove()
            onHostileDefeated(h, scoreMult)
          }
        }
      }
    }
  }

  private fun onHostileDefeated(h: ArenaHostile, scoreMult: Long) {
    hudElims++
    val base = when {
      h.isBoss -> 500L
      h.isElite -> 160L
      else -> 70L
    }
    hudScore += base * scoreMult

    // Multiplayer Mode: Scorestreak at 7/14/21 + Win at 25 elims
    if (modeId == "multiplayer") {
      if (hudElims > 0L && hudElims % 7L == 0L) {
        overdriveTicks = 240f
        postCallout("STREAK OVERDRIVE (${hudElims} ELIMS)!")
      }
      if (hudElims >= 25L && !hudExtractedSuccess) {
        hudExtractedSuccess = true
        hudRunEnded = true
        return
      }
    }

    // Spawn authentic Zombies / Crossover Power-Up Drops
    if (h.isBoss || (h.isElite && Random.nextFloat() < 0.65f) || Random.nextFloat() < 0.18f) {
      val kind = ArenaDropKind.entries[Random.nextInt(ArenaDropKind.entries.size)]
      drops.add(ArenaDrop(h.x, h.y, kind))
    }
  }

  private fun applyDrop(drop: ArenaDrop) {
    when (drop.kind) {
      ArenaDropKind.DOUBLE_ESSENCE -> {
        doubleEssenceTicks = 600f
        postCallout("2X ESSENCE ACTIVE (10s)")
      }
      ArenaDropKind.INSTA_KILL -> {
        instaKillTicks = 480f
        postCallout("INSTA-KILL ACTIVE (8s)")
      }
      ArenaDropKind.MAX_ARMOR -> {
        hudShield = loadout.maxShield
        dashCd = 0f
        postCallout("MAX ARMOR RESTORED!")
      }
      ArenaDropKind.NUKE -> {
        pulseRingRadius = 0.05f
        val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L
        val iter = hostiles.iterator()
        while (iter.hasNext()) {
          val h = iter.next()
          if (h.isBoss) {
            h.hp -= 8
            addSparkIfAllowed(h.x, h.y, Color(0xFF00E5FF), 20f)
          } else {
            addSparkIfAllowed(h.x, h.y, Color(0xFF00E5FF), 20f)
            iter.remove()
            hudElims++
          }
        }
        hudScore += 400L * scoreMult
        postCallout("TACTICAL NUKE • +${400L * scoreMult}")
      }
    }
  }

  fun stepFrame(dtSec: Float) {
    if (hudRunEnded) return
    val dt = (dtSec * 60f).coerceIn(0.1f, 2.5f)
    val pace = liveSettings.gameSpeedMultiplier.coerceIn(0.55f, 1.3f)
    val paceDt = dt * pace
    elapsedTicks++

    val moving = (joyDx * joyDx + joyDy * joyDy) > 0.0064f
    frameTick = (frameTick + (if (moving) 1.35f else 0.7f) * dt) % 60f

    if (powerCd > 0f) powerCd = (powerCd - dt).coerceAtLeast(0f)
    if (consumableCd > 0f) consumableCd = (consumableCd - dt).coerceAtLeast(0f)
    if (interactCd > 0f) interactCd = (interactCd - dt).coerceAtLeast(0f)
    if (dashCd > 0f) dashCd = (dashCd - dt).coerceAtLeast(0f)
    if (dashInvulnTicks > 0f) dashInvulnTicks = (dashInvulnTicks - dt).coerceAtLeast(0f)
    if (overdriveTicks > 0f) overdriveTicks = (overdriveTicks - dt).coerceAtLeast(0f)
    if (stasisTicks > 0f) stasisTicks = (stasisTicks - dt).coerceAtLeast(0f)
    if (doubleEssenceTicks > 0f) doubleEssenceTicks = (doubleEssenceTicks - dt).coerceAtLeast(0f)
    if (instaKillTicks > 0f) instaKillTicks = (instaKillTicks - dt).coerceAtLeast(0f)
    if (damageFlashTicks > 0f) damageFlashTicks = (damageFlashTicks - dt).coerceAtLeast(0f)

    if (calloutTicks > 0f) {
      calloutTicks = (calloutTicks - dt).coerceAtLeast(0f)
      if (calloutTicks <= 0f) hudCallout = null
    }
    if (pulseRingRadius > 0f) {
      pulseRingRadius += 0.018f * dt
      if (pulseRingRadius > 0.36f) pulseRingRadius = 0f
    }

    // Passive perk armor regeneration (+50% boost when Aegis Resilience Assist is active)
    if (hudShield < loadout.maxShield && damageFlashTicks <= 0f) {
      val assistMult = if (liveSettings.assistShieldBoost) 1.55f else 1.0f
      regenAccum += loadout.passiveRegenPerSec * assistMult * dtSec
      if (regenAccum >= 1f) {
        val add = regenAccum.toInt()
        regenAccum -= add
        hudShield = (hudShield + add).coerceAtMost(loadout.maxShield)
      }
    }

    // Player movement scaled by sensitivity setting
    if (moving) {
      val len = sqrt(joyDx * joyDx + joyDy * joyDy).coerceAtLeast(0.001f)
      if (!touchAimActive) {
        aimX = joyDx / len
        aimY = joyDy / len
      }
      val spd = 0.0064f * loadout.moveBoost * liveSettings.moveSensitivity.coerceIn(0.6f, 1.6f) * dt
      playerX = (playerX + (joyDx / len) * spd * len.coerceAtMost(1f)).coerceIn(0.06f, 0.94f)
      playerY = (playerY + (joyDy / len) * spd * len.coerceAtMost(1f)).coerceIn(0.10f, 0.90f)
    }

    // Auto-fire / Touch-drag fire
    if ((hudAutoFire && hostiles.isNotEmpty()) || touchAimActive) {
      fireAutoTicker -= dt
      if (fireAutoTicker <= 0f) {
        fireVolley()
        fireAutoTicker = if (overdriveTicks > 0f) loadout.fireCadenceTicks * 0.65f else loadout.fireCadenceTicks
      }
    }

    // Battle Royale shrinking zone
    if (modeId == "br") {
      brRingRadius = (brRingRadius - 0.000085f * paceDt).coerceAtLeast(0.17f)
      val dx = playerX - 0.5f
      val dy = playerY - 0.5f
      if (dx * dx + dy * dy > brRingRadius * brRingRadius) {
        brDamageAccum += paceDt
        if (brDamageAccum >= 22f) {
          brDamageAccum = 0f
          hudShield = (hudShield - 3).coerceAtLeast(0)
          damageFlashTicks = if (liveSettings.reduceFlashes) 0f else 10f
          if (hudShield <= 0) hudRunEnded = true
        }
      }
    }

    // Sparks & Arcs update
    val sIter = sparks.iterator()
    while (sIter.hasNext()) {
      val sp = sIter.next()
      sp.life -= dt
      if (sp.life <= 0f) sIter.remove()
    }
    val aIter = arcs.iterator()
    while (aIter.hasNext()) {
      val a = aIter.next()
      a.life -= dt
      if (a.life <= 0f) aIter.remove()
    }

    // Field Drops update & pickup check
    val dIter = drops.iterator()
    while (dIter.hasNext()) {
      val drop = dIter.next()
      drop.life -= dt
      val dx = drop.x - playerX
      val dy = drop.y - playerY
      if (dx * dx + dy * dy <= 0.055f * 0.055f) {
        dIter.remove()
        applyDrop(drop)
      } else if (drop.life <= 0f) {
        dIter.remove()
      }
    }

    // Bolts update + enemy shot hits player
    val bIter = bolts.iterator()
    while (bIter.hasNext()) {
      val b = bIter.next()
      val stepB = if (b.isEnemyShot) paceDt else dt
      b.x += b.vx * stepB
      b.y += b.vy * stepB
      b.life -= stepB
      if (b.isEnemyShot && dashInvulnTicks <= 0f && modeId != "sandbox") {
        val dx = b.x - playerX
        val dy = b.y - playerY
        if (dx * dx + dy * dy <= 0.036f * 0.036f) {
          b.life = 0f
          val shotDmg = if (liveSettings.assistShieldBoost) 5 else 8
          hudShield = (hudShield - shotDmg).coerceAtLeast(0)
          damageFlashTicks = if (liveSettings.reduceFlashes) 0f else 12f
          if (hudShield <= 0) hudRunEnded = true
        }
      }
      if (b.life <= 0f || b.x < 0f || b.x > 1f || b.y < 0f || b.y > 1f) {
        bIter.remove()
      }
    }

    // DMZ Intel & Active Exfil Chopper Timer
    if (modeId == "dmz") {
      for (i in 0 until intels.size) {
        val intel = intels[i]
        if (!intel.collected) {
          val dx = intel.x - playerX
          val dy = intel.y - playerY
          if (dx * dx + dy * dy < 0.06f * 0.06f) {
            intel.collected = true
            hudIntelCollected++
            hudScore += 250L
            postCallout(
              if (hudIntelCollected >= 3) "ALL INTEL SECURED! REACH EXFIL HELIPAD!"
              else "INTEL SECURED ($hudIntelCollected / 3)"
            )
          }
        }
      }
      if (hudIntelCollected >= 3) {
        val dx = playerX - 0.5f
        val dy = playerY - 0.16f
        if (dx * dx + dy * dy < 0.095f * 0.095f) {
          exfilHoldTicks += dt
          hudExfilRatio = (exfilHoldTicks / 150f).coerceIn(0f, 1f)
          if (exfilHoldTicks >= 150f) {
            hudExtractedSuccess = true
            hudRunEnded = true
          }
        } else {
          exfilHoldTicks = (exfilHoldTicks - dt * 0.5f).coerceAtLeast(0f)
          hudExfilRatio = (exfilHoldTicks / 150f).coerceIn(0f, 1f)
        }
      }
    }

    // Hostiles movement, ranged fire (troopers & bosses), and player-bolt collisions (squared distance!)
    val scoreMult = if (doubleEssenceTicks > 0f) 2L else 1L
    var activeBoss: ArenaHostile? = null
    val hIter = hostiles.iterator()
    while (hIter.hasNext()) {
      val h = hIter.next()
      if (stasisTicks <= 0f) {
        val dx = playerX - h.x
        val dy = playerY - h.y
        val dSq = dx * dx + dy * dy
        val d = sqrt(dSq).coerceAtLeast(0.001f)
        h.x += (dx / d) * h.speed * paceDt
        h.y += (dy / d) * h.speed * paceDt

        // Ranged troopers/mechs/bosses shoot telegraphed orbs
        if ((h.archetype == "trooper" || h.archetype == "mech" || h.isBoss) && d > 0.15f) {
          h.rangedCooldownTicks -= paceDt
          if (h.rangedCooldownTicks <= 0f) {
            h.rangedCooldownTicks = if (h.isBoss) 95f else 155f
            bolts.add(
              ArenaBolt(
                x = h.x,
                y = h.y,
                vx = (dx / d) * 0.009f,
                vy = (dy / d) * 0.009f,
                life = 90f,
                isEnemyShot = true,
                color = Color(0xFFFF5252),
              )
            )
          }
        }

        val contactThreshold = if (liveSettings.assistShieldBoost) 34f else 22f
        if (dSq < 0.045f * 0.045f && dashInvulnTicks <= 0f && modeId != "sandbox") {
          meleeDamageAccum += paceDt
          if (meleeDamageAccum >= contactThreshold) {
            meleeDamageAccum = 0f
            val baseDmg = if (h.isBoss) 14 else 9
            val dmg = if (liveSettings.assistShieldBoost) (baseDmg * 2 / 3).coerceAtLeast(4) else baseDmg
            hudShield = (hudShield - dmg).coerceAtLeast(0)
            damageFlashTicks = if (liveSettings.reduceFlashes) 0f else 12f
            if (hudShield <= 0) {
              hudRunEnded = true
            }
          }
        }
      }

      val hitR = if (h.isBoss) 0.068f else 0.050f
      val hitRSq = hitR * hitR
      var died = false
      for (i in 0 until bolts.size) {
        val b = bolts[i]
        if (b.isEnemyShot || b.life <= 0f) continue
        val bdx = b.x - h.x
        val bdy = b.y - h.y
        if (bdx * bdx + bdy * bdy < hitRSq) {
          val dmg = if (instaKillTicks > 0f && !h.isBoss) 999 else b.damage
          h.hp -= dmg
          addSparkIfAllowed(h.x, h.y, b.color, 14f)
          if (b.pierceLeft > 0) {
            b.pierceLeft--
          } else {
            b.life = 0f
          }
          if (h.hp <= 0) {
            died = true
            break
          }
        }
      }
      if (died) {
        hIter.remove()
        onHostileDefeated(h, scoreMult)
      } else if (h.isBoss) {
        activeBoss = h
      }
    }

    // Update discrete HUD state ONLY when changed
    if (activeBoss != null) {
      if (hudBossName != activeBoss.bossName) hudBossName = activeBoss.bossName
      hudBossHpRatio = (activeBoss.hp.toFloat() / activeBoss.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
    } else if (hudBossName != null) {
      hudBossName = null
      hudBossHpRatio = 0f
    }

    val pSec = if (powerCd <= 0f) 0 else (powerCd / 60f).toInt() + 1
    if (hudPowerCdSec != pSec) hudPowerCdSec = pSec
    val cSec = if (consumableCd <= 0f) 0 else (consumableCd / 60f).toInt() + 1
    if (hudConsumableCdSec != cSec) hudConsumableCdSec = cSec
    val iSec = if (interactCd <= 0f) 0 else (interactCd / 60f).toInt() + 1
    if (hudInteractCdSec != iSec) hudInteractCdSec = iSec
    val dReady = dashCd <= 0f
    if (hudDashReady != dReady) hudDashReady = dReady
    val ov = overdriveTicks > 0f
    if (hudOverdriveActive != ov) hudOverdriveActive = ov

    val dropTag = when {
      instaKillTicks > 0f && doubleEssenceTicks > 0f -> "2X ESSENCE • INSTA-KILL"
      instaKillTicks > 0f -> "INSTA-KILL (${(instaKillTicks / 60f).toInt() + 1}s)"
      doubleEssenceTicks > 0f -> "2X ESSENCE (${(doubleEssenceTicks / 60f).toInt() + 1}s)"
      else -> null
    }
    if (hudActiveDropText != dropTag) hudActiveDropText = dropTag

    // Wave transition
    if (hostiles.isEmpty() && !hudRunEnded) {
      if (modeId == "br" && hudWave >= 4L) {
        hudExtractedSuccess = true
        hudRunEnded = true
      } else {
        hudWave++
        hudScore += hudWave * 100L * scoreMult
        if (world.id == "chrono_vault") {
          hudChevronsLocked = ((hudWave.toInt() - 1) % 7) + 1
        }
        postCallout("ROUND ${formatRoundRoman(hudWave)} INCOMING")
        spawnWave(hudWave)
      }
    }

    renderNonce++
  }
}

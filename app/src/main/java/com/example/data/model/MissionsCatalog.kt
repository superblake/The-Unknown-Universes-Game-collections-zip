package com.example.data.model

import androidx.compose.runtime.Immutable

enum class MissionScope(val id: String, val label: String) {
  WORLD("world", "World Missions"),
  ARENA("arena", "Arena / Area Missions"),
}

enum class MissionStatKind(val unitLabel: String) {
  ELIMINATIONS("Hostiles Eliminated"),
  BOSS_ELIMS("Bosses Defeated"),
  WAVES("Waves Cleared"),
  WONDER_PICKUPS("Wonder Pickups"),
  EXTRACTIONS("Successful Extractions"),
  TRAP_KILLS("Trap Eliminations"),
  BUILDINGS_EXPLORED("Buildings Explored"),
  PACK_TIER("Pack-a-Tier Reached"),
  ESSENCE_EARNED("Colored Essence Gathered"),
  BLAST_SHARDS("Blast Shards Collected"),
}

@Immutable
data class MissionSpec(
  val id: String,
  val scope: MissionScope,
  val targetId: String,
  val targetTitle: String,
  val missionNumber: Int,
  val chapterLabel: String,
  val title: String,
  val description: String,
  val statKind: MissionStatKind,
  val targetCount: Long,
  val rewardBlueEssence: Long,
  val rewardPlayerXp: Long,
  val rewardShards: Long,
  val accentHex: Long,
)

@Immutable
data class MissionState(
  val spec: MissionSpec,
  val progress: Long,
  val claimed: Boolean,
) {
  val completed: Boolean get() = progress >= spec.targetCount
  val ratio: Float get() = (progress.toFloat() / spec.targetCount.coerceAtLeast(1L)).coerceIn(0f, 1f)
}

object MissionsCatalog {

  const val MISSIONS_PER_WORLD: Int = 45
  const val MISSIONS_PER_ARENA: Int = 45

  private data class TemplateDef(
    val statKind: MissionStatKind,
    val titleBuilder: (target: String, sector: String, idx: Int) -> String,
    val descBuilder: (target: String, sector: String, qty: Long) -> String,
    val targetCountBuilder: (idx: Int) -> Long,
  )

  private val templates: List<TemplateDef> = listOf(
    TemplateDef(
      MissionStatKind.ELIMINATIONS,
      { _, sector, i -> "Clear $sector Recon #${i + 1}" },
      { target, sector, q -> "Eliminate $q hostiles threatening $sector inside $target." },
      { i -> 15L + (i * 5L) },
    ),
    TemplateDef(
      MissionStatKind.WAVES,
      { _, sector, i -> "Hold $sector Perimeter #${i + 1}" },
      { target, sector, q -> "Reach Wave $q while defending $sector in $target." },
      { i -> 2L + (i / 3L) },
    ),
    TemplateDef(
      MissionStatKind.BOSS_ELIMS,
      { _, sector, i -> "Apex Hunt: $sector #${i + 1}" },
      { target, sector, q -> "Defeat $q Boss encounters emerging from $sector ($target)." },
      { i -> 1L + (i / 6L) },
    ),
    TemplateDef(
      MissionStatKind.BUILDINGS_EXPLORED,
      { _, sector, i -> "Breach $sector Vaults #${i + 1}" },
      { target, sector, q -> "Explore $q buildings and interiors near $sector in $target." },
      { i -> 2L + (i % 6) + (i / 10L) * 2L },
    ),
    TemplateDef(
      MissionStatKind.TRAP_KILLS,
      { _, sector, i -> "Overload Traps: $sector #${i + 1}" },
      { target, sector, q -> "Score $q Electric or Fire Trap kills around $sector ($target)." },
      { i -> 5L + (i * 2L) },
    ),
    TemplateDef(
      MissionStatKind.WONDER_PICKUPS,
      { _, sector, i -> "Wonder Cache Hunt: $sector #${i + 1}" },
      { target, sector, q -> "Collect $q Wonder Weapons deployed in $sector ($target)." },
      { i -> 1L + (i / 8L) },
    ),
    TemplateDef(
      MissionStatKind.ESSENCE_EARNED,
      { _, sector, i -> "Essence Siphon: $sector #${i + 1}" },
      { target, sector, q -> "Gather $q Colored Essence across $sector in $target." },
      { i -> 80L + (i * 35L) },
    ),
    TemplateDef(
      MissionStatKind.BLAST_SHARDS,
      { _, sector, i -> "Blast Shard Survey: $sector #${i + 1}" },
      { target, sector, q -> "Recover $q Blast Shards scattered near $sector ($target)." },
      { i -> 10L + (i * 3L) },
    ),
    TemplateDef(
      MissionStatKind.PACK_TIER,
      { _, sector, i -> "Pack-a-Tier Forge: $sector #${i + 1}" },
      { target, sector, q -> "Reach Pack-a-Punch Tier $q during combat in $target ($sector)." },
      { i -> (2L + (i / 2L)).coerceAtMost(25L) },
    ),
    TemplateDef(
      MissionStatKind.EXTRACTIONS,
      { _, sector, i -> "Exfil & Anchor: $sector #${i + 1}" },
      { target, sector, q -> "Complete $q successful extractions or story anchors from $target." },
      { i -> 1L + (i / 12L) },
    ),
  )

  private fun chapterForIndex(num: Int, isConvergence: Boolean = false, isCampaign: Boolean = false): String = when {
    isConvergence -> when {
      num <= 12 -> "Near-Finale I: Healing Dawn"
      num <= 24 -> "Near-Finale II: Fused Landmarks"
      num <= 36 -> "Near-Finale III: Reality Loom"
      else -> "Near-Finale IV: Final Restoration Brink"
    }
    isCampaign -> when {
      num <= 11 -> "Chapter I: Rift Breach"
      num <= 22 -> "Chapter II: Coalition Rally"
      num <= 34 -> "Chapter III: World Healing"
      else -> "Chapter IV: Path to Convergence"
    }
    else -> when {
      num <= 11 -> "Act I: Containment"
      num <= 22 -> "Act II: Escalation"
      num <= 34 -> "Act III: Sovereign Ops"
      else -> "Act IV: Reality Healing"
    }
  }

  val worldMissions: List<MissionSpec> by lazy {
    UniverseCatalog.worlds.flatMap { world ->
      val shortTitle = world.title.substringBefore(":")
      val isCampaign = world.id == "campaign_origins"
      (1..MISSIONS_PER_WORLD).map { num ->
        val sector = world.mapSectors[(num - 1) % world.mapSectors.size]
        val tpl = templates[(num - 1) % templates.size]
        val qty = tpl.targetCountBuilder(num - 1)
        val titlePrefix = if (isCampaign && num == 1) "Campaign Op 01: " else if (isCampaign) "Ch.$num: " else "M${num.toString().padStart(2, '0')}: "
        MissionSpec(
          id = "mis_world_${world.id}_${num.toString().padStart(2, '0')}",
          scope = MissionScope.WORLD,
          targetId = world.id,
          targetTitle = shortTitle,
          missionNumber = num,
          chapterLabel = chapterForIndex(num, isCampaign = isCampaign),
          title = titlePrefix + tpl.titleBuilder(shortTitle, sector, num - 1),
          description = tpl.descBuilder(shortTitle, sector, qty),
          statKind = tpl.statKind,
          targetCount = qty,
          rewardBlueEssence = 140L + (num * 25L),
          rewardPlayerXp = 220L + (num * 40L),
          rewardShards = 3L + (num / 5L),
          accentHex = world.accentHex,
        )
      }
    }
  }

  val arenaMissions: List<MissionSpec> by lazy {
    MultiverseArsenalCatalog.customAreas.flatMap { area ->
      val shortName = area.name.substringBefore(" (")
        val isConvergence = area.id == "area_mended_convergence"
        val isCampaignArea = area.id == "area_campaign_nexus"
        val isFrontier = area.id == "area_hero_frontier"
      (1..MISSIONS_PER_ARENA).map { num ->
        val bld = area.buildingNames[(num - 1) % area.buildingNames.size]
        val tpl = templates[(num + 2) % templates.size]
        val qty = tpl.targetCountBuilder(num - 1)
        val titlePrefix = when {
          isConvergence -> "Convergence $num: "
          isCampaignArea -> "Story Arena $num: "
          isFrontier -> "Hero Hunt $num: "
          else -> "A${num.toString().padStart(2, '0')}: "
        }
        val extraStoryFlavor = when {
          isConvergence -> " Stabilize the healing fusion before final reality restoration."
          isFrontier -> " Locate Character Beacons and harvest Shards to elevate hero rarity."
          else -> ""
        }
        MissionSpec(
          id = "mis_arena_${area.id}_${num.toString().padStart(2, '0')}",
          scope = MissionScope.ARENA,
          targetId = area.id,
          targetTitle = shortName,
          missionNumber = num,
          chapterLabel = chapterForIndex(num, isConvergence = isConvergence, isCampaign = isCampaignArea),
          title = titlePrefix + tpl.titleBuilder(shortName, bld, num - 1),
          description = tpl.descBuilder(shortName, bld, qty) + extraStoryFlavor,
          statKind = tpl.statKind,
          targetCount = qty,
          rewardBlueEssence = 160L + (num * 28L),
          rewardPlayerXp = 240L + (num * 42L),
          rewardShards = 4L + (num / 4L),
          accentHex = area.accentHex,
        )
      }
    }
  }

  val allMissions: List<MissionSpec> by lazy { worldMissions + arenaMissions }

  private val missionIndex: Map<String, MissionSpec> by lazy { allMissions.associateBy { it.id } }

  fun missionsForWorld(worldId: String): List<MissionSpec> =
    worldMissions.filter { it.targetId == worldId }

  fun missionsForArena(areaId: String): List<MissionSpec> =
    arenaMissions.filter { it.targetId == areaId }

  fun missionById(id: String): MissionSpec? = missionIndex[id]

  /**
   * Embedded No-APK Play Guide bundled with the Multiverse Missions catalog.
   */
  const val NO_APK_PLAY_GUIDE: String = UniverseCatalog.NO_APK_PLAY_GUIDE
}

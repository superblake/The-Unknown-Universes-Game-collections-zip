package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operator_profiles")
data class OperatorProfile(
  @PrimaryKey val id: String = "",
  val userId: String = "",
  val callsign: String = "Vanguard",
  val activeUniverseId: String = "sector_zero",
  val activeMode: String = "zombies",
  val activeRigId: String = "rig_operator",
  val consolePreset: String = "universal_web",
  val totalEssence: Long = 1500L,
  val highScore: Long = 0L,
  val highestWave: Long = 0L,
  val osWallpaperIndex: Long = 0L,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "armory_items")
data class ArmoryItem(
  @PrimaryKey val id: String = "",
  val userId: String = "",
  val name: String = "",
  val category: String = "perk",
  val universeTag: String = "Sector Zero",
  val description: String = "",
  val tier: Long = 1L,
  val maxTier: Long = 60L,
  val cooldownSec: Long = 0L,
  val powerValue: Long = 100L,
  val accentColorHex: String = "#00E5FF",
  val equipped: Boolean = false,
  val isCustom: Boolean = false,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "custom_rigs")
data class RigAsset(
  @PrimaryKey val id: String = "",
  val userId: String = "",
  val name: String = "",
  val archetype: String = "operator",
  val activeClip: String = "idle",
  val ikEnabled: Boolean = true,
  val rigScale: Double = 1.0,
  val playbackSpeed: Double = 1.0,
  val poseAngles: Map<String, Double> = emptyMap(),
  val keyframeFrames: List<Long> = listOf(0L, 15L, 30L, 45L, 60L),
  val isCompleted: Boolean = true,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "match_logs")
data class MatchRecord(
  @PrimaryKey val id: String = "",
  val userId: String = "",
  val universeId: String = "sector_zero",
  val mode: String = "zombies",
  val score: Long = 0L,
  val waveReached: Long = 1L,
  val eliminations: Long = 0L,
  val extracted: Boolean = false,
  val durationSec: Long = 0L,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "linked_accounts")
data class LinkedAccount(
  @PrimaryKey val id: String = "",
  val userId: String = "",
  val providerId: String = "",
  val displayName: String = "",
  val externalSubject: String = "",
  val linkedAtMillis: Long = System.currentTimeMillis(),
)

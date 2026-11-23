package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UniverseCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UniverseRepositoryLocalTest {

  private lateinit var db: UniverseDatabase
  private lateinit var repo: UniverseRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, UniverseDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repo = UniverseRepository(db.universeDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun ensureStarterData_createsProfileArmoryAndRigs() = runBlocking {
    val uid = "local_operator"
    val initRes = repo.ensureStarterData(uid, "BlakePrime")
    assertTrue(initRes.isSuccess)

    val profile = repo.observeProfile(uid).first { it != null }
    assertNotNull(profile)
    assertEquals("BlakePrime", profile!!.callsign)

    val armory = repo.observeArmoryItems(uid).first { it.isNotEmpty() }
    assertTrue(armory.size >= 5)

    val rigs = repo.observeCustomRigs(uid).first { it.isNotEmpty() }
    assertTrue(rigs.size >= 4)
  }

  @Test
  fun saveRigArmoryAndMatch_persistsUpdatesLocally() = runBlocking {
    val uid = "local_operator"
    repo.ensureStarterData(uid, "RigMaster").getOrThrow()

    val rigId = repo.saveRigAsset(
      userId = uid,
      rigId = null,
      name = "Chrono Librarian",
      archetype = "chrono",
      activeClip = "inspect",
      ikEnabled = true,
      rigScale = 1.1,
      playbackSpeed = 1.0,
      poseAngles = UniverseCatalog.defaultPoseMap(),
      keyframeFrames = listOf(0L, 20L, 40L, 60L),
      isCompleted = true,
    ).getOrThrow()
    assertTrue(rigId.isNotBlank())

    val rigs = repo.observeCustomRigs(uid).first()
    assertTrue(rigs.any { it.id == rigId && it.name == "Chrono Librarian" })

    val itemId = repo.addArmoryItem(
      userId = uid,
      name = "Gate Pulse",
      category = "power",
      universeTag = "Chrono-Vault",
      description = "Teleports and emits a pulse wave.",
      cooldownSec = 8L,
      powerValue = 210L,
      accentColorHex = "#FFD54F",
    ).getOrThrow()
    assertTrue(itemId.isNotBlank())

    val profile = repo.observeProfile(uid).first()!!
    repo.recordMatchResult(
      userId = uid,
      profile = profile,
      universeId = "chrono_vault",
      mode = "dmz",
      score = 1800L,
      waveReached = 4L,
      eliminations = 22L,
      extracted = true,
      durationSec = 95L,
      essenceEarned = 530L,
    ).getOrThrow()

    val updatedProfile = repo.observeProfile(uid).first()!!
    assertEquals(1800L, updatedProfile.highScore)
    assertEquals(4L, updatedProfile.highestWave)
    assertEquals(profile.totalEssence + 530L, updatedProfile.totalEssence)

    val logs = repo.observeMatchLogs(uid).first()
    assertEquals(1, logs.size)
    assertTrue(logs.first().extracted)
  }

  @Test
  fun exportAndImportSaveData_roundTripsAndSupportsMergeAndReplace() = runBlocking {
    val senderUid = "sender_uid_01"
    repo.ensureStarterData(senderUid, "SuperBlake").getOrThrow()
    repo.addArmoryItem(
      userId = senderUid,
      name = "Chrono Blade",
      category = "power",
      universeTag = "Chrono-Vault",
      description = "Slices hostiles across time.",
      cooldownSec = 5L,
      powerValue = 320L,
      accentColorHex = "#00E5FF",
    ).getOrThrow()
    val customRigId = repo.saveRigAsset(
      userId = senderUid,
      rigId = null,
      name = "Apex Conduit Rig",
      archetype = "biomorph",
      activeClip = "lunge",
      ikEnabled = true,
      rigScale = 1.2,
      playbackSpeed = 1.5,
      poseAngles = mapOf("torso" to 12.5, "head" to -5.0),
      keyframeFrames = listOf(0L, 15L, 30L),
      isCompleted = true,
    ).getOrThrow()
    val senderProfile = repo.observeProfile(senderUid).first()!!
    repo.recordMatchResult(
      userId = senderUid,
      profile = senderProfile,
      universeId = "chrono_vault",
      mode = "dmz",
      score = 9200L,
      waveReached = 14L,
      eliminations = 88L,
      extracted = true,
      durationSec = 310L,
      essenceEarned = 1270L,
    ).getOrThrow()

    val exportedJson = repo.exportSaveDataJson(senderUid).getOrThrow()
    assertTrue(exportedJson.contains("unknown_universes_save_v1"))
    assertTrue(exportedJson.contains("Chrono Blade"))
    assertTrue(exportedJson.contains("Apex Conduit Rig"))
    assertTrue(!exportedJson.contains("externalSubject"))

    // Friend imports save onto their own profile UID
    val friendUid = "friend_uid_99"
    repo.ensureStarterData(friendUid, "FriendOne").getOrThrow()
    val parsed = SaveDataSerializer.parseAndValidateSaveJson(exportedJson, friendUid).getOrThrow()
    assertEquals("SuperBlake", parsed.callsign)
    assertEquals(9200L, parsed.highScore)
    assertEquals(14L, parsed.highestWave)
    assertTrue(parsed.armoryCount >= 9)
    assertTrue(parsed.rigsCount >= 5)
    assertEquals(1, parsed.matchLogsCount)

    // Replace import adopts sender progression cleanly under friendUid
    val importRes = repo.importSaveBundle(friendUid, parsed.bundle, replaceExisting = true)
    assertTrue(importRes.isSuccess)

    val friendProfile = repo.observeProfile(friendUid).first()!!
    assertEquals(friendUid, friendProfile.userId)
    assertEquals("SuperBlake", friendProfile.callsign)
    assertEquals(9200L, friendProfile.highScore)
    assertEquals(14L, friendProfile.highestWave)
    val friendRigs = repo.observeCustomRigs(friendUid).first()
    assertTrue(friendRigs.any { it.id == customRigId && it.userId == friendUid })
    val friendLogs = repo.observeMatchLogs(friendUid).first()
    assertEquals(1, friendLogs.size)
    assertEquals(friendUid, friendLogs.first().userId)
  }

  @Test
  fun firestoreSyncCodec_mapsMatchSecurityRuleKeysExactly() {
    val uid = "user_alpha"
    val profile = com.example.data.model.OperatorProfile(
      id = uid,
      userId = uid,
      callsign = "SuperBlake",
    )
    val profileCreate = com.example.data.remote.FirestoreSyncCodec.run { profile.toFirestoreCreateMap(uid) }
    assertTrue("id" !in profileCreate)
    assertEquals(
      setOf(
        "userId", "callsign", "activeUniverseId", "activeMode", "activeRigId",
        "consolePreset", "totalEssence", "highScore", "highestWave",
        "osWallpaperIndex", "createdAt", "updatedAt",
      ),
      profileCreate.keys,
    )
    val profileUpdate = com.example.data.remote.FirestoreSyncCodec.run { profile.toFirestoreUpdateMap() }
    assertTrue("userId" !in profileUpdate && "createdAt" !in profileUpdate && "id" !in profileUpdate)
    assertTrue("updatedAt" in profileUpdate)

    val item = com.example.data.model.ArmoryItem(id = "custom_1", userId = uid, name = "Aegis", category = "perk")
    val itemUpdate = com.example.data.remote.FirestoreSyncCodec.run { item.toFirestoreUpdateMap() }
    assertTrue("userId" !in itemUpdate && "isCustom" !in itemUpdate && "createdAt" !in itemUpdate && "id" !in itemUpdate)
  }

  @Test
  fun ensureStarterData_preservesCustomCallsignAndSelection() = runBlocking {
    val uid = "op_persist_check"
    repo.ensureStarterData(uid, "InitialName").getOrThrow()
    repo.updateProfileSelection(
      userId = uid,
      callsign = "CustomCommander",
      activeUniverseId = "gotham_arkham",
      activeMode = "hero_patrol",
    ).getOrThrow()

    repo.ensureStarterData(uid, "InitialName").getOrThrow()
    val reloaded = repo.observeProfile(uid).first { it != null }!!
    assertEquals("CustomCommander", reloaded.callsign)
    assertEquals("gotham_arkham", reloaded.activeUniverseId)
    assertEquals("hero_patrol", reloaded.activeMode)
  }
}

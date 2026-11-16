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
}

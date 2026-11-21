package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettings
import com.example.data.local.ProgressionStore
import com.example.data.local.SaveDataSerializer
import com.example.data.model.CharacterUniverseGroup
import com.example.data.model.MultiverseArsenalCatalog
import com.example.data.model.OperatorProfile
import com.example.data.model.UniverseCatalog
import com.example.data.model.UpgradeCosts
import com.example.security.AppSecurityManager
import com.example.security.SaveIntegrityStatus
import com.example.ui.screens.ArenaLoadoutStats
import com.example.ui.screens.ArenaSimState
import com.example.ui.screens.CharacterCustomizationTab
import com.example.ui.screens.CloudSyncAndSaveCard
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.UnknownUniversesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class OriginalCharactersAndProtectionTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("${ProgressionStore.PREFS_PREFIX}_orig_sec_test", Context.MODE_PRIVATE)
      .edit().clear().commit()
    AppSecurityManager.clearAppPin(context)
  }

  @Test
  fun originalCharacters_fullRosterAreStrongerAndSyncSignatureEquipment() {
    val expectedOriginalGroups = setOf(
      CharacterUniverseGroup.MR_CROSSOVER,
      CharacterUniverseGroup.COLE_MCGRATH,
      CharacterUniverseGroup.PROTOTYPE,
      CharacterUniverseGroup.ORIGINALS_PRIME,
      CharacterUniverseGroup.SPACE_TIME,
      CharacterUniverseGroup.GOD_OF_WAR,
      CharacterUniverseGroup.SHIP_OF_HEROES,
    )
    val originals = MultiverseArsenalCatalog.outfits.filter { it.isOriginalCharacter }
    assertEquals(expectedOriginalGroups.size * 3, originals.size)
    assertTrue(originals.all { it.characterGroup in expectedOriginalGroups })

    val store = ProgressionStore(context, "orig_sec_test")
    // Default Lv.1 Mr. Crossover v1 should now receive +25% Original boost + base outfit stats
    val mrCrossoverLoadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current, 0L)
    assertTrue("Lv.1 Mr. Crossover must apply Original weaponBonusDamage", mrCrossoverLoadout.weaponBonusDamage >= 5)

    // Compare against non-original Iron Man v1 vs Original Vanguard v1 at Lv.1
    store.equipOutfit("ironman_v1", syncSignatureEquipment = false)
    val nonOrigLoadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current, 0L)

    val selectedRes = store.equipOutfit("origin_sentinel_v1", syncSignatureEquipment = true)
    assertEquals("Origin Vanguard v1", selectedRes.nameWithVersion)
    assertEquals("origin_sentinel_v1", store.current.equippedOutfitId)
    assertTrue(store.current.equippedLethalIds.contains("lethal_cluster"))
    assertTrue(store.current.equippedTacticalIds.contains("tactical_stim"))

    val origLoadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan, store.current, 0L)
    assertTrue("Original character should have higher max shield", origLoadout.maxShield > nonOrigLoadout.maxShield)
    assertTrue("Original character should deal higher bonus damage", origLoadout.weaponBonusDamage > nonOrigLoadout.weaponBonusDamage)
    assertTrue("Original character should have higher movement boost", origLoadout.moveBoost > nonOrigLoadout.moveBoost)
  }

  @Test
  fun calmerGameplayAndUpgradeCosts_workDeterministically() {
    assertEquals(1500L, UpgradeCosts.characterTier(3))
    assertEquals(1800L, UpgradeCosts.characterStar(3))
    assertEquals(430L, UpgradeCosts.outfitLevelUp(1))

    val world = UniverseCatalog.worldById("sector_zero")
    val loadout = ArenaLoadoutStats.fromArmory(emptyList(), Color.Cyan)
    val normalSim = ArenaSimState(
      world = world,
      modeId = "zombies",
      loadout = loadout,
      hasConsumableEquipped = false,
      initialSettings = AppSettings(gameSpeedMultiplier = 1.0f),
    )
    val relaxedSim = ArenaSimState(
      world = world,
      modeId = "zombies",
      loadout = loadout,
      hasConsumableEquipped = false,
      initialSettings = AppSettings(gameSpeedMultiplier = 0.6f),
    )

    val h1 = normalSim.hostiles.first()
    val h2 = relaxedSim.hostiles.first()
    h1.x = normalSim.playerX + 0.5f
    h1.y = normalSim.playerY
    h2.x = relaxedSim.playerX + 0.5f
    h2.y = relaxedSim.playerY

    normalSim.stepFrame(0.05f)
    relaxedSim.stepFrame(0.05f)
    val normalAdvance = (normalSim.playerX + 0.5f) - h1.x
    val relaxedAdvance = (relaxedSim.playerX + 0.5f) - h2.x
    assertTrue("Hostile at 0.6x game speed must advance slower", relaxedAdvance < normalAdvance)
  }

  @Test
  fun appSecurityManager_sealsSavesVerifiesProgressionDetectsTamperAndManagesPin() {
    // 1. Export & Verify Save JSON Integrity
    val profile = OperatorProfile(
      id = "sec_user",
      userId = "sec_user",
      callsign = "ShieldTester",
      totalEssence = 4200L,
      highScore = 9800L,
      highestWave = 14L,
    )
    val exported = SaveDataSerializer.exportSaveJson(
      profile = profile,
      armory = emptyList(),
      rigs = emptyList(),
      matchLogs = emptyList(),
      nowMillis = 1_750_000_000_000L,
    )
    val validPreview = SaveDataSerializer.parseAndValidateSaveJson(exported, "sec_user").getOrThrow()
    assertEquals(SaveIntegrityStatus.VERIFIED, validPreview.integrityStatus)

    val tamperedJson = exported.replace("\"totalEssence\": 4200", "\"totalEssence\": 9999999")
    val tamperedPreview = SaveDataSerializer.parseAndValidateSaveJson(tamperedJson, "sec_user").getOrThrow()
    assertEquals(SaveIntegrityStatus.TAMPERED, tamperedPreview.integrityStatus)

    // 2. ProgressionStore SHA-256 tamper seal
    val store = ProgressionStore(context, "orig_sec_test")
    store.equipOutfit("ship_heroes_v1", syncSignatureEquipment = true)
    assertTrue(store.verifyStoredIntegrity())
    // Simulate direct SharedPreferences edit bypassing seal
    context.getSharedPreferences("${ProgressionStore.PREFS_PREFIX}_orig_sec_test", Context.MODE_PRIVATE)
      .edit()
      .putString("progression_sha256_v1", "0000deadbeef")
      .commit()
    assertFalse(store.verifyStoredIntegrity())

    // 3. Salted App Lock PIN
    assertFalse(AppSecurityManager.isPinEnabled(context))
    assertTrue(AppSecurityManager.setAppPin(context, "12").isFailure)
    assertTrue(AppSecurityManager.setAppPin(context, "4829").isSuccess)
    assertTrue(AppSecurityManager.isPinEnabled(context))
    assertTrue(AppSecurityManager.verifyAppPin(context, "4829"))
    assertFalse(AppSecurityManager.verifyAppPin(context, "0000"))
    AppSecurityManager.clearAppPin(context)
    assertFalse(AppSecurityManager.isPinEnabled(context))

    // 4. Input & chat sanitization
    val cleaned = AppSecurityManager.sanitizeChatText("<script>alert(1)</script>  Hello team!  ", safetyFilterEnabled = true)
    assertEquals("alert(1) Hello team!", cleaned)
  }

  @Test
  fun ui_characterCustomizationSelectsOriginalHeroes_andSettingsSecurityConfiguresPin() {
    val store = ProgressionStore(context, "orig_sec_test")
    composeRule.setContent {
      var prog by remember { mutableStateOf(store.current) }
      UnknownUniversesTheme(darkTheme = true) {
        CharacterCustomizationTab(
          progression = prog,
          activeRig = null,
          onUpdateCharacterCustom = {},
          totalBlueEssence = 10_000L,
          onEquipOutfit = { id ->
            store.equipOutfit(id, syncSignatureEquipment = true)
            prog = store.current
          },
        )
      }
    }

    composeRule.onNodeWithTag("character_active_hero_name").assertIsDisplayed()
    composeRule.onNodeWithTag("character_original_badge").assertIsDisplayed()
    composeRule.onNodeWithTag("char_filter_originals").performClick()
    composeRule.onNodeWithTag("char_roster_row").performScrollToNode(hasTestTag("char_select_origin_sentinel_v1"))
    composeRule.onNodeWithTag("char_select_origin_sentinel_v1").performClick()
    assertEquals("origin_sentinel_v1", store.current.equippedOutfitId)
    composeRule.onNodeWithTag("character_active_hero_name").assertTextContains("Origin Vanguard v1", substring = true)
  }

  @Test
  fun ui_settingsSecurityTabManagesPinScreenShieldAndSaveIntegrityBadge() {
    var settings by mutableStateOf(AppSettings())
    var integrityCalls = 0
    val sampleExport = SaveDataSerializer.exportSaveJson(
      profile = OperatorProfile(id = "u", userId = "u", callsign = "Vanguard", totalEssence = 1234L),
      armory = emptyList(),
      rigs = emptyList(),
      matchLogs = emptyList(),
      nowMillis = 1_750_000_000_000L,
    )

    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        SettingsDialog(
          settings = settings,
          onUpdateSettings = { fn -> settings = fn(settings) },
          onReplaceSettings = { settings = it },
          onResetDefaults = { settings = AppSettings() },
          onDismiss = {},
          progressionIntegrityValid = true,
          onVerifyIntegrity = {
            integrityCalls++
            true
          },
        )
      }
    }

    composeRule.onNodeWithTag("settings_tab_protection").performScrollTo().performClick()
    composeRule.onNodeWithTag("switch_screen_shield").performScrollTo().performClick()
    assertTrue(settings.screenCaptureShield)

    composeRule.onNodeWithTag("btn_manage_app_pin").performScrollTo().performClick()
    composeRule.onNodeWithTag("pin_setup_input").performTextInput("4829")
    composeRule.onNodeWithTag("confirm_app_pin_btn").performClick()
    assertTrue(AppSecurityManager.isPinEnabled(context))
    assertTrue(AppSecurityManager.verifyAppPin(context, "4829"))

    composeRule.onNodeWithTag("btn_run_integrity_check").performScrollTo().performClick()
    assertEquals(1, integrityCalls)
    composeRule.onNodeWithTag("integrity_audit_result").assertTextContains("VALID", substring = true)

    // Verify SaveIntegrityStatus badge UI for VERIFIED bundle
    val preview = SaveDataSerializer.parseAndValidateSaveJson(sampleExport, "u").getOrThrow()
    assertEquals(SaveIntegrityStatus.VERIFIED, preview.integrityStatus)
  }
}

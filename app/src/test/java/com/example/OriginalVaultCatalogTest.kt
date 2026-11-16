package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.OriginalVaultCatalog
import com.example.data.model.UniverseCatalog
import com.example.ui.screens.MediaVaultScreen
import com.example.ui.screens.VaultArtworkCard
import com.example.ui.screens.VaultArtworkLightboxContent
import com.example.ui.screens.VaultArtworkLightboxDialog
import com.example.ui.theme.UnknownUniversesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class OriginalVaultCatalogTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun all37OriginalArtworksExistInAssetsAndMatchCatalog() {
    val assets = ApplicationProvider.getApplicationContext<Context>().assets
    val bundledFiles = assets.list("vault_art")?.toSet().orEmpty()
    assertEquals(37, bundledFiles.size)
    assertEquals(37, OriginalVaultCatalog.artworks.size)

    val catalogFileNames = OriginalVaultCatalog.artworks.map {
      it.assetPath.removePrefix("vault_art/")
    }.toSet()
    assertEquals(bundledFiles, catalogFileNames)

    OriginalVaultCatalog.artworks.forEach { art ->
      assets.open(art.assetPath).use { stream ->
        assertTrue("Artwork ${art.assetPath} should be readable", stream.read() != -1)
      }
      assertEquals(art.linkedWorldId, UniverseCatalog.worldById(art.linkedWorldId).id)
    }
    assertNotNull(OriginalVaultCatalog.artworkByAssetPath(OriginalVaultCatalog.artworks.first().assetPath))
  }

  @Test
  fun all6OriginalVideosExistInAssetsAndMatchCatalog() {
    val assets = ApplicationProvider.getApplicationContext<Context>().assets
    val bundledVideos = assets.list("vault_videos")?.toSet().orEmpty()
    assertEquals(6, bundledVideos.size)
    assertEquals(6, OriginalVaultCatalog.videos.size)

    val catalogVideos = OriginalVaultCatalog.videos.map {
      it.assetPath.removePrefix("vault_videos/")
    }.toSet()
    assertEquals(bundledVideos, catalogVideos)
  }

  @Test
  fun all8ContinuationSeriesResolveToValidWorldsAndModes() {
    assertEquals(8, OriginalVaultCatalog.continuationSeries.size)
    OriginalVaultCatalog.continuationSeries.forEach { series ->
      assertEquals(series.linkedWorldId, UniverseCatalog.worldById(series.linkedWorldId).id)
      assertEquals(series.linkedModeId, UniverseCatalog.modeById(series.linkedModeId).id)
    }
  }

  @Test
  fun mediaVaultScreenRendersHeaderAndArchiveCinema() {
    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        MediaVaultScreen(
          activeWallpaperPath = null,
          onSetWallpaper = {},
          onDeploySeries = { _, _ -> },
          onStatusMessage = {},
        )
      }
    }

    composeRule.onNodeWithText("ORIGINAL VAULT & LORE ARCHIVE").assertIsDisplayed()
    composeRule.onNodeWithText("The Unknown Universes — Chronicle Log I").assertIsDisplayed()
  }

  @Test
  fun vaultArtworkLightboxDialogInteractions() {
    var deployed: Pair<String, String>? = null
    var wallpaperToggled = false
    val sampleArt = OriginalVaultCatalog.artworks.first { it.id == "art_krita_prototype_comic" }

    composeRule.setContent {
      UnknownUniversesTheme(darkTheme = true) {
        VaultArtworkLightboxContent(
          artwork = sampleArt,
          isActiveWallpaper = false,
          hasPrev = true,
          hasNext = true,
          onPrev = {},
          onNext = {},
          onSetWallpaper = { wallpaperToggled = true },
          onDeployLinkedWorld = { deployed = sampleArt.linkedWorldId to "sandbox" },
          onDismiss = {},
        )
      }
    }

    composeRule.onNodeWithTag("vault_art_rotate_button").assertIsDisplayed()
    composeRule.onNodeWithTag("vault_art_set_wallpaper_button").performClick()
    assertTrue(wallpaperToggled)
    composeRule.onNodeWithTag("vault_art_mount_world_button").performClick()
    assertEquals("blacklight_conduit" to "sandbox", deployed)
  }
}

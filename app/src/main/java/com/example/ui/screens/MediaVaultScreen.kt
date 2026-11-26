package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.LruCache
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ContinuationSeries
import com.example.data.model.OriginalVaultCatalog
import com.example.data.model.UniverseCatalog
import com.example.data.model.VaultArtwork
import com.example.data.model.VaultVideo
import com.example.ui.theme.AppIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private object VaultBitmapCache {
  private val cache = object : LruCache<String, ImageBitmap>(20 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int {
      return value.width * value.height * 4
    }
  }

  fun get(key: String): ImageBitmap? = cache.get(key)
  fun put(key: String, bmp: ImageBitmap) {
    cache.put(key, bmp)
  }
}

@Composable
fun rememberAssetImageBitmap(
  assetPath: String?,
  maxDim: Int = 360,
): ImageBitmap? {
  val context = LocalContext.current
  val cacheKey = remember(assetPath, maxDim) {
    if (assetPath.isNullOrBlank()) null else "$assetPath@$maxDim"
  }
  val state = produceState<ImageBitmap?>(
    initialValue = cacheKey?.let { VaultBitmapCache.get(it) },
    key1 = cacheKey,
  ) {
    val key = cacheKey ?: run {
      value = null
      return@produceState
    }
    VaultBitmapCache.get(key)?.let {
      value = it
      return@produceState
    }
    val path = assetPath ?: run {
      value = null
      return@produceState
    }
    val decoded = withContext(Dispatchers.IO) {
      decodeAssetBitmap(context, path, maxDim)
    }
    if (decoded != null) {
      VaultBitmapCache.put(key, decoded)
    }
    value = decoded
  }
  return state.value
}

private fun decodeAssetBitmap(context: Context, assetPath: String, maxDim: Int): ImageBitmap? {
  return runCatching {
    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.assets.open(assetPath).use { stream ->
      BitmapFactory.decodeStream(stream, null, boundsOpts)
    }
    val maxSide = maxOf(boundsOpts.outWidth, boundsOpts.outHeight).coerceAtLeast(1)
    var sampleSize = 1
    while (maxSide / sampleSize > maxDim * 2) {
      sampleSize *= 2
    }
    val decodeOpts = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = Bitmap.Config.RGB_565
    }
    val bmp = context.assets.open(assetPath).use { stream ->
      BitmapFactory.decodeStream(stream, null, decodeOpts)
    }
    bmp?.asImageBitmap()
  }.getOrNull()
}

@Composable
fun MediaVaultScreen(
  activeWallpaperPath: String?,
  onSetWallpaper: (String?) -> Unit,
  onDeploySeries: (universeId: String, modeId: String) -> Unit,
  onStatusMessage: (String) -> Unit,
) {
  var selectedVideoIndex by remember { mutableIntStateOf(0) }
  var selectedCategory by remember { mutableStateOf(OriginalVaultCatalog.CATEGORY_ALL) }
  var selectedArtworkId by remember { mutableStateOf<String?>(null) }

  val filteredArtworks = remember(selectedCategory) {
    if (selectedCategory == OriginalVaultCatalog.CATEGORY_ALL) {
      OriginalVaultCatalog.artworks
    } else {
      OriginalVaultCatalog.artworks.filter { it.category == selectedCategory }
    }
  }

  val activeVideo = OriginalVaultCatalog.videos[selectedVideoIndex.coerceIn(0, OriginalVaultCatalog.videos.lastIndex)]
  val inspectedArtworkIndex = remember(selectedArtworkId, filteredArtworks) {
    filteredArtworks.indexOfFirst { it.id == selectedArtworkId }
  }
  val inspectedArtwork = if (inspectedArtworkIndex >= 0) filteredArtworks[inspectedArtworkIndex] else null

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    val columns = if (maxWidth >= 700.dp) 3 else 2
    val chunkedArt = remember(filteredArtworks, columns) {
      filteredArtworks.chunked(columns)
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Header Banner
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                modifier = Modifier.size(42.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = AppIcons.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "ORIGINAL VAULT & LORE ARCHIVE",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "${OriginalVaultCatalog.artworks.size} Original Artworks • ${OriginalVaultCatalog.videos.size} Videos • ${OriginalVaultCatalog.continuationSeries.size} Continuation Series",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Text(
              text = "Every original trailer, chronicle capture, Krita continuation cover, notebook map, and PENUP comic illustration preserved natively inside UnknownOS.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (activeWallpaperPath != null) {
              val wpArt = OriginalVaultCatalog.artworkByAssetPath(activeWallpaperPath)
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Active OS Wallpaper: ${wpArt?.title ?: "Custom Vault Art"}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF00E676),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                  onClick = {
                    onSetWallpaper(null)
                    onStatusMessage("Cleared custom Vault wallpaper")
                  },
                  modifier = Modifier.testTag("vault_clear_wallpaper_button"),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                  Text("Reset Wallpaper", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }

      // Cinema / Original Videos Player
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = AppIcons.VideoLibrary,
            contentDescription = null,
            tint = Color(activeVideo.accentHex),
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = "ORIGINAL TRAILERS & ARCHIVE CINEMA (${OriginalVaultCatalog.videos.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
      }

      item {
        VaultVideoPlayerCard(
          video = activeVideo,
          hasPrev = selectedVideoIndex > 0,
          hasNext = selectedVideoIndex < OriginalVaultCatalog.videos.lastIndex,
          onPrev = {
            if (selectedVideoIndex > 0) selectedVideoIndex -= 1
          },
          onNext = {
            if (selectedVideoIndex < OriginalVaultCatalog.videos.lastIndex) selectedVideoIndex += 1
          },
          onStatusMessage = onStatusMessage,
        )
      }

      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(OriginalVaultCatalog.videos.size) { index ->
            val vid = OriginalVaultCatalog.videos[index]
            val selected = index == selectedVideoIndex
            Surface(
              onClick = { selectedVideoIndex = index },
              shape = RoundedCornerShape(14.dp),
              color = if (selected) Color(vid.accentHex).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
              modifier = Modifier
                .width(232.dp)
                .border(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) Color(vid.accentHex) else MaterialTheme.colorScheme.outline,
                  shape = RoundedCornerShape(14.dp),
                )
                .testTag("vault_video_item_${vid.id}"),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color(vid.accentHex),
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = vid.dateLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(vid.accentHex),
                  )
                }
                Text(
                  text = vid.title,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = vid.subtitle,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }

      // 8 Original Continuation Series
      item {
        Text(
          text = "THE UNKNOWN UNIVERSES CONTINUATION (ALL 8 ORIGINAL SERIES)",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(OriginalVaultCatalog.continuationSeries, key = { it.id }) { series ->
            ContinuationSeriesCard(
              series = series,
              onDeploy = {
                onDeploySeries(series.linkedWorldId, series.linkedModeId)
              },
            )
          }
        }
      }

      // Artwork Filter Header & Chips
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "CONCEPT ART, KRITA COVERS & PENUP GALLERY (${filteredArtworks.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(OriginalVaultCatalog.categories) { category ->
              val count = if (category == OriginalVaultCatalog.CATEGORY_ALL) {
                OriginalVaultCatalog.artworks.size
              } else {
                OriginalVaultCatalog.artworks.count { it.category == category }
              }
              val selected = selectedCategory == category
              FilterChip(
                selected = selected,
                onClick = { selectedCategory = category },
                label = { Text("$category ($count)") },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                  selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.testTag("vault_filter_${category.lowercase().replace(" ", "_").replace("&", "and")}"),
              )
            }
          }
        }
      }

      // Multi-Column Artwork Grid Rows
      items(chunkedArt.size) { rowIndex ->
        val rowItems = chunkedArt[rowIndex]
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          rowItems.forEach { art ->
            VaultArtworkCard(
              artwork = art,
              isActiveWallpaper = activeWallpaperPath == art.assetPath,
              onClick = { selectedArtworkId = art.id },
              modifier = Modifier.weight(1f),
            )
          }
          repeat(columns - rowItems.size) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }

  if (inspectedArtwork != null) {
    VaultArtworkLightboxDialog(
      artwork = inspectedArtwork,
      isActiveWallpaper = activeWallpaperPath == inspectedArtwork.assetPath,
      hasPrev = inspectedArtworkIndex > 0,
      hasNext = inspectedArtworkIndex < filteredArtworks.lastIndex,
      onPrev = {
        if (inspectedArtworkIndex > 0) {
          selectedArtworkId = filteredArtworks[inspectedArtworkIndex - 1].id
        }
      },
      onNext = {
        if (inspectedArtworkIndex < filteredArtworks.lastIndex) {
          selectedArtworkId = filteredArtworks[inspectedArtworkIndex + 1].id
        }
      },
      onSetWallpaper = {
        if (activeWallpaperPath == inspectedArtwork.assetPath) {
          onSetWallpaper(null)
          onStatusMessage("Cleared custom OS wallpaper")
        } else {
          onSetWallpaper(inspectedArtwork.assetPath)
          onStatusMessage("Set '${inspectedArtwork.title}' as UnknownOS Wallpaper")
        }
      },
      onDeployLinkedWorld = {
        val world = UniverseCatalog.worldById(inspectedArtwork.linkedWorldId)
        selectedArtworkId = null
        onDeploySeries(world.id, world.defaultModeId)
      },
      onDismiss = { selectedArtworkId = null },
    )
  }
}

@Composable
private fun VaultVideoPlayerCard(
  video: VaultVideo,
  hasPrev: Boolean,
  hasNext: Boolean,
  onPrev: () -> Unit,
  onNext: () -> Unit,
  onStatusMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  var isPrepared by remember(video.assetPath) { mutableStateOf(false) }
  var isPlaying by remember(video.assetPath) { mutableStateOf(false) }
  var durationMs by remember(video.assetPath) { mutableIntStateOf(0) }
  var positionMs by remember(video.assetPath) { mutableIntStateOf(0) }
  var isSeeking by remember { mutableStateOf(false) }
  var sliderPosMs by remember(video.assetPath) { mutableFloatStateOf(0f) }
  var loadError by remember(video.assetPath) { mutableStateOf<String?>(null) }
  var videoAspectRatio by remember(video.assetPath) { mutableFloatStateOf(16f / 9f) }

  val mediaPlayer = remember(video.assetPath) { MediaPlayer() }
  var pendingSurface by remember { mutableStateOf<Surface?>(null) }

  DisposableEffect(video.assetPath) {
    var released = false
    runCatching {
      val afd = context.assets.openFd(video.assetPath)
      afd.use {
        mediaPlayer.setDataSource(it.fileDescriptor, it.startOffset, it.length)
      }
      mediaPlayer.setOnPreparedListener { mp ->
        if (!released) {
          isPrepared = true
          durationMs = mp.duration.coerceAtLeast(0)
          val w = mp.videoWidth
          val h = mp.videoHeight
          if (w > 0 && h > 0) {
            videoAspectRatio = (w.toFloat() / h.toFloat()).coerceIn(0.6f, 2.4f)
          }
          pendingSurface?.let { surf ->
            runCatching { mp.setSurface(surf) }
          }
        }
      }
      mediaPlayer.setOnCompletionListener {
        if (!released) {
          isPlaying = false
          positionMs = durationMs
        }
      }
      mediaPlayer.setOnErrorListener { _, what, extra ->
        if (!released) {
          loadError = "Playback error ($what/$extra)"
          onStatusMessage("Video playback unavailable on this device ($what)")
        }
        true
      }
      mediaPlayer.prepareAsync()
    }.onFailure { err ->
      loadError = err.localizedMessage ?: "Failed to open video asset"
    }

    onDispose {
      released = true
      runCatching {
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
      }
      runCatching { mediaPlayer.release() }
      pendingSurface?.release()
      pendingSurface = null
    }
  }

  LaunchedEffect(video.assetPath, isPlaying, isPrepared) {
    while (isActive && isPrepared) {
      if (!isSeeking) {
        val current = runCatching { mediaPlayer.currentPosition }.getOrDefault(positionMs)
        positionMs = current
        sliderPosMs = current.toFloat()
      }
      delay(250L)
    }
  }

  fun togglePlayback() {
    if (!isPrepared) return
    runCatching {
      if (mediaPlayer.isPlaying) {
        mediaPlayer.pause()
        isPlaying = false
      } else {
        if (positionMs >= durationMs - 200 && durationMs > 0) {
          mediaPlayer.seekTo(0)
        }
        mediaPlayer.start()
        isPlaying = true
      }
    }
  }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.5.dp, Color(video.accentHex).copy(alpha = 0.65f), RoundedCornerShape(18.dp)),
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
          .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
          .background(Color(0xFF03050A))
          .clickable(enabled = isPrepared) { togglePlayback() },
        contentAlignment = Alignment.Center,
      ) {
        AndroidView(
          factory = { ctx ->
            TextureView(ctx).apply {
              surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                  val s = Surface(st)
                  pendingSurface = s
                  runCatching { mediaPlayer.setSurface(s) }
                }

                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) = Unit

                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                  runCatching { mediaPlayer.setSurface(null) }
                  pendingSurface?.release()
                  pendingSurface = null
                  return true
                }

                override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
              }
            }
          },
          modifier = Modifier
            .fillMaxSize()
            .aspectRatio(videoAspectRatio, matchHeightConstraintsFirst = videoAspectRatio < 1.6f),
        )

        if (loadError != null) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
          ) {
            Text(
              text = loadError ?: "Unable to load video",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
            )
          }
        } else if (!isPrepared) {
          CircularProgressIndicator(
            color = Color(video.accentHex),
            modifier = Modifier.size(36.dp),
          )
        } else if (!isPlaying) {
          Surface(
            shape = CircleShape,
            color = Color(0xBB060A14),
            modifier = Modifier
              .size(60.dp)
              .border(1.5.dp, Color(video.accentHex), CircleShape),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play video",
                tint = Color(video.accentHex),
                modifier = Modifier.size(32.dp),
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(999.dp),
          color = Color(0xCC060912),
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(10.dp)
            .border(1.dp, Color(video.accentHex).copy(alpha = 0.6f), RoundedCornerShape(999.dp)),
        ) {
          Text(
            text = video.dateLabel,
            style = MaterialTheme.typography.labelSmall,
            color = Color(video.accentHex),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }

      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = video.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = video.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Seekbar + Transport Controls
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          val maxDur = durationMs.coerceAtLeast(1).toFloat()
          Slider(
            value = sliderPosMs.coerceIn(0f, maxDur),
            onValueChange = {
              isSeeking = true
              sliderPosMs = it
            },
            onValueChangeFinished = {
              if (isPrepared) {
                runCatching { mediaPlayer.seekTo(sliderPosMs.toInt()) }
                positionMs = sliderPosMs.toInt()
              }
              isSeeking = false
            },
            valueRange = 0f..maxDur,
            colors = SliderDefaults.colors(
              thumbColor = Color(video.accentHex),
              activeTrackColor = Color(video.accentHex),
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("vault_video_seek"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "${formatDurationMs(if (isSeeking) sliderPosMs.toInt() else positionMs)} / ${formatDurationMs(durationMs)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              IconButton(
                onClick = onPrev,
                enabled = hasPrev,
                modifier = Modifier.testTag("vault_video_prev"),
              ) {
                Icon(
                  imageVector = AppIcons.SkipPrevious,
                  contentDescription = "Previous video",
                  tint = if (hasPrev) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
              }
              Button(
                onClick = { togglePlayback() },
                enabled = isPrepared,
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(video.accentHex),
                  contentColor = Color(0xFF040A12),
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("vault_video_play_pause"),
              ) {
                Icon(
                  imageVector = if (isPlaying) AppIcons.Pause else Icons.Filled.PlayArrow,
                  contentDescription = if (isPlaying) "Pause" else "Play",
                  modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(if (isPlaying) "Pause" else "Play")
              }
              IconButton(
                onClick = onNext,
                enabled = hasNext,
                modifier = Modifier.testTag("vault_video_next"),
              ) {
                Icon(
                  imageVector = AppIcons.SkipNext,
                  contentDescription = "Next video",
                  tint = if (hasNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ContinuationSeriesCard(
  series: ContinuationSeries,
  onDeploy: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .width(248.dp)
      .border(1.dp, Color(series.accentHex).copy(alpha = 0.55f), RoundedCornerShape(16.dp))
      .testTag("series_card_${series.id}"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = series.sectorTag,
        style = MaterialTheme.typography.labelSmall,
        color = Color(series.accentHex),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = series.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = series.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.height(60.dp),
      )
      OutlinedButton(
        onClick = onDeploy,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("deploy_series_${series.id}"),
      ) {
        Icon(
          imageVector = Icons.Filled.PlayArrow,
          contentDescription = null,
          tint = Color(series.accentHex),
          modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text("Mount & Play", color = Color(series.accentHex))
      }
    }
  }
}

@Composable
internal fun VaultArtworkCard(
  artwork: VaultArtwork,
  isActiveWallpaper: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bitmap = rememberAssetImageBitmap(artwork.assetPath, maxDim = 360)
  val world = remember(artwork.linkedWorldId) { UniverseCatalog.worldById(artwork.linkedWorldId) }
  val accent = Color(world.accentHex)

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier
      .border(
        width = if (isActiveWallpaper) 2.dp else 1.dp,
        color = if (isActiveWallpaper) Color(0xFF00E676) else MaterialTheme.colorScheme.outline,
        shape = RoundedCornerShape(16.dp),
      )
      .testTag("vault_art_card_${artwork.id}"),
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(140.dp)
          .background(Color(0xFF090D18)),
        contentAlignment = Alignment.Center,
      ) {
        if (bitmap != null) {
          Image(
            bitmap = bitmap,
            contentDescription = artwork.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        } else {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0x18070A12), Color(0xCC070A12)))),
        )

        Surface(
          shape = RoundedCornerShape(999.dp),
          color = Color(0xCC070B14),
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(999.dp)),
        ) {
          Text(
            text = artwork.seriesTag,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }

        if (isActiveWallpaper) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFF00E676),
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp),
          ) {
            Text(
              text = "WALLPAPER",
              style = MaterialTheme.typography.labelSmall,
              color = Color.Black,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }

        Text(
          text = artwork.dateLabel,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFE2E8F0),
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        )
      }

      Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = artwork.title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = artwork.category,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
internal fun VaultArtworkLightboxDialog(
  artwork: VaultArtwork,
  isActiveWallpaper: Boolean,
  hasPrev: Boolean,
  hasNext: Boolean,
  onPrev: () -> Unit,
  onNext: () -> Unit,
  onSetWallpaper: () -> Unit,
  onDeployLinkedWorld: () -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    com.example.util.SyncDialogFullscreen()
    VaultArtworkLightboxContent(
      artwork = artwork,
      isActiveWallpaper = isActiveWallpaper,
      hasPrev = hasPrev,
      hasNext = hasNext,
      onPrev = onPrev,
      onNext = onNext,
      onSetWallpaper = onSetWallpaper,
      onDeployLinkedWorld = onDeployLinkedWorld,
      onDismiss = onDismiss,
    )
  }
}

@Composable
fun VaultArtworkLightboxContent(
  artwork: VaultArtwork,
  isActiveWallpaper: Boolean,
  hasPrev: Boolean,
  hasNext: Boolean,
  onPrev: () -> Unit,
  onNext: () -> Unit,
  onSetWallpaper: () -> Unit,
  onDeployLinkedWorld: () -> Unit,
  onDismiss: () -> Unit,
) {
  var rotationDeg by remember(artwork.id) { mutableFloatStateOf(0f) }
  val highResBitmap = rememberAssetImageBitmap(artwork.assetPath, maxDim = 1440)
  val linkedWorld = remember(artwork.linkedWorldId) { UniverseCatalog.worldById(artwork.linkedWorldId) }

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .fillMaxWidth()
      .padding(14.dp)
      .border(1.5.dp, Color(linkedWorld.accentHex).copy(alpha = 0.7f), RoundedCornerShape(22.dp)),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Top Header + Rotate + Close
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = artwork.seriesTag.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color(linkedWorld.accentHex),
          )
            Text(
              text = artwork.title,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { rotationDeg = (rotationDeg + 90f) % 360f },
              modifier = Modifier.testTag("vault_art_rotate_button"),
            ) {
              Icon(
                imageVector = AppIcons.RotateRight,
                contentDescription = "Rotate artwork 90 degrees",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.testTag("vault_art_close_button"),
            ) {
              Icon(
                imageVector = AppIcons.Close,
                contentDescription = "Close viewer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // High-res image container
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF04060C)),
          contentAlignment = Alignment.Center,
        ) {
          if (highResBitmap != null) {
            Image(
              bitmap = highResBitmap,
              contentDescription = artwork.title,
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationDeg },
            )
          } else {
            CircularProgressIndicator()
          }
        }

        Text(
          text = "${artwork.dateLabel} • ${artwork.note}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Navigation + Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = onPrev,
            enabled = hasPrev,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("vault_art_prev_button"),
          ) {
            Icon(AppIcons.SkipPrevious, contentDescription = "Previous artwork", modifier = Modifier.size(18.dp))
          }

          OutlinedButton(
            onClick = onSetWallpaper,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("vault_art_set_wallpaper_button"),
          ) {
            Icon(AppIcons.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
              text = if (isActiveWallpaper) "Clear Wallpaper" else "Set Wallpaper",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Button(
            onClick = onDeployLinkedWorld,
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(linkedWorld.accentHex),
              contentColor = Color(0xFF040A12),
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("vault_art_mount_world_button"),
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Play Sector", maxLines = 1)
          }

          OutlinedButton(
            onClick = onNext,
            enabled = hasNext,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("vault_art_next_button"),
          ) {
            Icon(AppIcons.SkipNext, contentDescription = "Next artwork", modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }

private fun formatDurationMs(ms: Int): String {
  val totalSec = (ms / 1000).coerceAtLeast(0)
  val mins = totalSec / 60
  val secs = totalSec % 60
  return "%02d:%02d".format(mins, secs)
}

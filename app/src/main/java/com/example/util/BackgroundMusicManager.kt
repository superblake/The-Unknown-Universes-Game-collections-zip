package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

data class MusicTrackSpec(
  val index: Int,
  val title: String,
  val assetPath: String,
)

/**
 * Plays the user-supplied soundtrack softly in the background from bundled Android assets.
 */
object BackgroundMusicManager {
  val tracks: List<MusicTrackSpec> = listOf(
    MusicTrackSpec(1, "My Feelings of Music", "music/My_feelings_of_music_e0690887.m4a"),
    MusicTrackSpec(2, "Destiny Legendary Stories 1", "music/Destiny_legendary_stories_1_2ece1c71.m4a"),
  )

  private var appContext: Context? = null
  private var mediaPlayer: MediaPlayer? = null
  private var playingState = false
  private var pausedByLifecycle = false
  private var currentTrackNumber = 1

  var musicEnabled: Boolean = true
    private set
  var currentVolume: Float = 0.22f
    private set
  var selectedTrackMode: Int = 0 // 0 = Auto Cycle, 1 = Track 1, 2 = Track 2
    private set

  fun startAmbientMusic(context: Context) {
    appContext = context.applicationContext
    if (!musicEnabled) return
    if (playingState && mediaPlayer != null) return
    val initialTrack = if (selectedTrackMode in 1..tracks.size) selectedTrackMode else 1
    playTrack(initialTrack)
  }

  fun applySettings(
    context: Context,
    enabled: Boolean,
    volume: Float,
    trackMode: Int,
  ) {
    appContext = context.applicationContext
    val cleanVol = if (volume.isFinite()) volume.coerceIn(0f, 0.70f) else 0.22f
    val cleanMode = trackMode.coerceIn(0, tracks.size)
    val trackChanged = cleanMode != selectedTrackMode && cleanMode in 1..tracks.size && cleanMode != currentTrackNumber

    musicEnabled = enabled
    currentVolume = cleanVol
    selectedTrackMode = cleanMode

    if (!enabled) {
      stopAmbientMusic()
      return
    }

    if (mediaPlayer == null || !playingState || trackChanged) {
      val nextTrack = if (cleanMode in 1..tracks.size) cleanMode else currentTrackNumber
      playTrack(nextTrack)
    } else {
      setVolume(cleanVol)
    }
  }

  fun setVolume(volume: Float) {
    currentVolume = if (volume.isFinite()) volume.coerceIn(0f, 0.70f) else 0.22f
    try {
      mediaPlayer?.setVolume(currentVolume, currentVolume)
    } catch (_: Exception) {
    }
  }

  fun pauseMusic() {
    try {
      val mp = mediaPlayer ?: return
      if (mp.isPlaying) {
        mp.pause()
        pausedByLifecycle = true
      }
    } catch (_: Exception) {
    }
  }

  fun resumeMusic() {
    if (!musicEnabled) return
    try {
      val mp = mediaPlayer
      if (mp != null && pausedByLifecycle) {
        mp.start()
        mp.setVolume(currentVolume, currentVolume)
        pausedByLifecycle = false
      } else if (mp == null) {
        appContext?.let { startAmbientMusic(it) }
      }
    } catch (_: Exception) {
    }
  }

  private fun playTrack(trackNumber: Int) {
    val ctx = appContext ?: return
    val spec = tracks.firstOrNull { it.index == trackNumber } ?: tracks.first()
    stopAmbientMusic()
    currentTrackNumber = spec.index
    try {
      val player = MediaPlayer()
      mediaPlayer = player
      ctx.assets.openFd(spec.assetPath).use { afd ->
        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
      }
      player.setVolume(currentVolume, currentVolume)
      player.isLooping = selectedTrackMode in 1..tracks.size
      player.setOnPreparedListener { mp ->
        if (musicEnabled && mediaPlayer === mp) {
          mp.setVolume(currentVolume, currentVolume)
          mp.start()
          playingState = true
          pausedByLifecycle = false
        }
      }
      player.setOnCompletionListener { mp ->
        if (musicEnabled && selectedTrackMode == 0 && mediaPlayer === mp) {
          val nextIndex = if (currentTrackNumber >= tracks.size) 1 else currentTrackNumber + 1
          playTrack(nextIndex)
        }
      }
      player.setOnErrorListener { mp, _, _ ->
        if (mediaPlayer === mp) {
          playingState = false
        }
        true
      }
      player.prepareAsync()
    } catch (e: Exception) {
      playingState = false
      Log.w("BackgroundMusicManager", "Asset music unavailable in runtime (${e.localizedMessage})")
    }
  }

  fun stopAmbientMusic() {
    try {
      mediaPlayer?.stop()
    } catch (_: Exception) {
    }
    try {
      mediaPlayer?.release()
    } catch (_: Exception) {
    }
    mediaPlayer = null
    playingState = false
    pausedByLifecycle = false
  }
}

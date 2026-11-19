package com.example.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.model.RepeatModeOption
import com.example.model.Song
import com.example.widget.TrakasWidgetUpdater
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow

data class PlaybackUiState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val smartShuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val volume: Float = 1.0f,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerRemainingSec: Int? = null
)

class PlaybackController(
    private val context: Context,
    private val audioProcessor: TrakasAudioProcessor,
    private val onTrackStarted: (Long) -> Unit,
    private val onListeningSession: (Long, Long) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    @Volatile
    var crossfadeSeconds: Int = 0

    @Volatile
    var gaplessEnabled: Boolean = true

    @Volatile
    var normalizeVolume: Boolean = true

    @Volatile
    var replayGainEnabled: Boolean = true

    @Volatile
    var smartShuffleEnabled: Boolean = false

    private var catalogList: List<Song> = emptyList()
    private var songLookup: Map<Long, Song> = emptyMap()
    private var favoriteIdsSnapshot: Set<Long> = emptySet()
    private var orderedQueue: List<Song> = emptyList()
    private var lastRecordedSongId: Long? = null

    private var activeSessionSongId: Long? = null
    private var activeSessionStartMs: Long = 0L

    fun updateCatalog(songs: List<Song>, favIds: Set<Long> = favoriteIdsSnapshot) {
        catalogList = songs
        songLookup = songs.associateBy { it.id }
        favoriteIdsSnapshot = favIds
        val current = _state.value.currentSong
        if (current != null) {
            val updated = songLookup[current.id]
            if (updated != null && updated != current) {
                _state.value = _state.value.copy(currentSong = updated)
            }
        }
    }

    fun currentCatalogSnapshot(): List<Song> = catalogList
    fun currentFavoriteIdsSnapshot(): Set<Long> = favoriteIdsSnapshot

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    mediaController = controller
                    controller.volume = _state.value.volume
                    controller.setPlaybackSpeed(_state.value.playbackSpeed)
                    controller.addListener(playerListener)
                    syncStateFromPlayer()
                    startProgressLoop()
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    fun disconnect() {
        flushActiveListeningSession()
        progressJob?.cancel()
        progressJob = null
        mediaController?.removeListener(playerListener)
        mediaController = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0, allowSmartExtras: Boolean = true) {
        if (songs.isEmpty()) return
        flushActiveListeningSession()
        val safeIdx = startIndex.coerceIn(0, songs.lastIndex)

        val finalQueue = if (allowSmartExtras && smartShuffleEnabled && _state.value.shuffleEnabled && catalogList.size > songs.size) {
            val existingIds = songs.map { it.id }.toSet()
            val anchor = songs[safeIdx]
            val smartExtras = catalogList
                .filter { it.id !in existingIds }
                .sortedByDescending { cand ->
                    var score = 0
                    if (cand.genre.equals(anchor.genre, ignoreCase = true)) score += 30
                    if (cand.id in favoriteIdsSnapshot) score += 20
                    if (cand.artist.equals(anchor.artist, ignoreCase = true)) score += 15
                    score
                }
                .take(5)
            songs + smartExtras
        } else {
            songs
        }

        orderedQueue = finalQueue
        val controller = mediaController ?: return
        val mediaItems = finalQueue.map { it.toMediaItem() }
        controller.setMediaItems(mediaItems, safeIdx, 0L)
        controller.prepare()
        controller.play()
        val chosen = finalQueue[safeIdx]
        applyTrackGain(chosen)
        _state.value = _state.value.copy(
            queue = finalQueue,
            currentIndex = safeIdx,
            currentSong = chosen,
            durationMs = chosen.durationMs,
            positionMs = 0L
        )
        pushWidgetUpdate()
    }

    fun playNext(song: Song) {
        val controller = mediaController ?: return
        if (orderedQueue.isEmpty()) {
            playSongs(listOf(song), 0)
            return
        }
        val insertIdx = (controller.currentMediaItemIndex + 1).coerceIn(0, orderedQueue.size)
        val newQueue = orderedQueue.toMutableList().apply { add(insertIdx, song) }
        orderedQueue = newQueue
        controller.addMediaItem(insertIdx, song.toMediaItem())
        _state.value = _state.value.copy(queue = newQueue)
    }

    fun playSongsNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        val controller = mediaController ?: return
        if (orderedQueue.isEmpty()) {
            playSongs(songs, 0, allowSmartExtras = false)
            return
        }
        val insertIdx = (controller.currentMediaItemIndex + 1).coerceIn(0, orderedQueue.size)
        val newQueue = orderedQueue.toMutableList().apply { addAll(insertIdx, songs) }
        orderedQueue = newQueue
        controller.addMediaItems(insertIdx, songs.map { it.toMediaItem() })
        _state.value = _state.value.copy(queue = newQueue)
    }

    fun addToQueue(song: Song) {
        val controller = mediaController ?: return
        if (orderedQueue.isEmpty()) {
            playSongs(listOf(song), 0)
            return
        }
        val newQueue = orderedQueue + song
        orderedQueue = newQueue
        controller.addMediaItem(song.toMediaItem())
        _state.value = _state.value.copy(queue = newQueue)
    }

    fun addSongsToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val controller = mediaController ?: return
        if (orderedQueue.isEmpty()) {
            playSongs(songs, 0, allowSmartExtras = false)
            return
        }
        val newQueue = orderedQueue + songs
        orderedQueue = newQueue
        controller.addMediaItems(songs.map { it.toMediaItem() })
        _state.value = _state.value.copy(queue = newQueue)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        if (fromIndex !in orderedQueue.indices || toIndex !in orderedQueue.indices || fromIndex == toIndex) return
        val mutable = orderedQueue.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        orderedQueue = mutable
        controller.moveMediaItem(fromIndex, toIndex)
        _state.value = _state.value.copy(
            queue = mutable,
            currentIndex = controller.currentMediaItemIndex
        )
    }

    fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        if (index !in orderedQueue.indices) return
        val mutable = orderedQueue.toMutableList().apply { removeAt(index) }
        orderedQueue = mutable
        controller.removeMediaItem(index)
        syncStateFromPlayer()
    }

    fun jumpToQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index !in orderedQueue.indices) return
        flushActiveListeningSession()
        controller.seekToDefaultPosition(index)
        controller.play()
    }

    fun clearQueue() {
        flushActiveListeningSession()
        orderedQueue = emptyList()
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _state.value = _state.value.copy(
            currentSong = null,
            queue = emptyList(),
            currentIndex = -1,
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L
        )
        pushWidgetUpdate()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE && orderedQueue.isNotEmpty()) {
                controller.prepare()
            }
            controller.play()
        }
    }

    fun autoPlayOnBluetoothIfIdle() {
        val controller = mediaController ?: return
        if (!controller.isPlaying) {
            if (orderedQueue.isNotEmpty()) {
                if (controller.playbackState == Player.STATE_IDLE) controller.prepare()
                controller.play()
            } else if (catalogList.isNotEmpty()) {
                playSongs(catalogList, 0)
            }
        }
    }

    fun skipNext() {
        val controller = mediaController ?: return
        flushActiveListeningSession()
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (orderedQueue.isNotEmpty()) {
            controller.seekToDefaultPosition(0)
        }
        controller.play()
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3500L) {
            controller.seekTo(0L)
        } else {
            flushActiveListeningSession()
            if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
            } else if (orderedQueue.isNotEmpty()) {
                controller.seekToDefaultPosition(orderedQueue.lastIndex)
            }
        }
        controller.play()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs.coerceAtLeast(0L))
        _state.value = _state.value.copy(positionMs = positionMs.coerceAtLeast(0L))
    }

    fun setShuffle(enabled: Boolean, smart: Boolean = smartShuffleEnabled) {
        smartShuffleEnabled = smart
        mediaController?.shuffleModeEnabled = enabled
        _state.value = _state.value.copy(shuffleEnabled = enabled, smartShuffleEnabled = smart)
    }

    fun setRepeatMode(mode: RepeatModeOption) {
        mediaController?.repeatMode = when (mode) {
            RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
            RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
            RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
        }
        _state.value = _state.value.copy(repeatMode = mode)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        mediaController?.volume = clamped
        _state.value = _state.value.copy(volume = clamped)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        mediaController?.setPlaybackSpeed(clamped)
        _state.value = _state.value.copy(playbackSpeed = clamped)
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _state.value = _state.value.copy(sleepTimerRemainingSec = null)
            return
        }
        var remaining = minutes * 60
        _state.value = _state.value.copy(sleepTimerRemainingSec = remaining)
        sleepTimerJob = scope.launch {
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _state.value = _state.value.copy(sleepTimerRemainingSec = remaining)
            }
            mediaController?.pause()
            _state.value = _state.value.copy(sleepTimerRemainingSec = null)
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _state.value = _state.value.copy(sleepTimerRemainingSec = null)
    }

    fun refreshNormalization() {
        applyTrackGain(_state.value.currentSong)
    }

    private fun applyTrackGain(song: Song?) {
        var gain = if (normalizeVolume) 1.08f else 1.0f
        if (replayGainEnabled && song != null && song.replayGainDb != 0f) {
            gain *= 10.0.pow(song.replayGainDb / 20.0).toFloat()
        }
        audioProcessor.normalizationGain = gain.coerceIn(0.4f, 1.8f)
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                activeSessionSongId = _state.value.currentSong?.id
                activeSessionStartMs = System.currentTimeMillis()
            } else {
                flushActiveListeningSession()
            }
            syncStateFromPlayer()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            flushActiveListeningSession()
            syncStateFromPlayer()
            if (mediaController?.isPlaying == true) {
                activeSessionSongId = _state.value.currentSong?.id
                activeSessionStartMs = System.currentTimeMillis()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncStateFromPlayer()
        }
    }

    private fun flushActiveListeningSession() {
        val songId = activeSessionSongId ?: return
        if (activeSessionStartMs > 0L) {
            val elapsed = System.currentTimeMillis() - activeSessionStartMs
            if (elapsed >= 1500L) {
                onListeningSession(songId, elapsed)
            }
        }
        activeSessionStartMs = 0L
        activeSessionSongId = null
    }

    private fun syncStateFromPlayer() {
        val controller = mediaController ?: return
        val idx = controller.currentMediaItemIndex
        val mediaId = controller.currentMediaItem?.mediaId?.toLongOrNull()
        val song = orderedQueue.getOrNull(idx) ?: mediaId?.let { songLookup[it] }
        val duration = controller.duration.takeIf { it > 0L } ?: song?.durationMs ?: 0L

        if (song != null && controller.isPlaying && lastRecordedSongId != song.id) {
            lastRecordedSongId = song.id
            applyTrackGain(song)
            onTrackStarted(song.id)
        }

        _state.value = _state.value.copy(
            currentSong = song,
            queue = orderedQueue,
            currentIndex = idx,
            isPlaying = controller.isPlaying,
            positionMs = controller.currentPosition.coerceAtLeast(0L),
            durationMs = duration
        )
        pushWidgetUpdate()
    }

    private fun pushWidgetUpdate() {
        val s = _state.value
        val song = s.currentSong
        val pct = if (s.durationMs > 0L) ((s.positionMs * 100L) / s.durationMs).toInt().coerceIn(0, 100) else 0
        TrakasWidgetUpdater.updateAll(
            context = context,
            title = song?.title ?: "Trakas Music Player",
            artist = song?.artist ?: "Tu música. Tu mezcla. Tu estilo.",
            isPlaying = s.isPlaying,
            progressPercent = pct,
            bars = VisualizerStateHolder.frame.value.bars
        )
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var tick = 0
            while (true) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0L } ?: _state.value.durationMs
                    _state.value = _state.value.copy(
                        positionMs = pos,
                        durationMs = dur
                    )
                    if (tick % 5 == 0) {
                        pushWidgetUpdate()
                    }
                    if (!gaplessEnabled || crossfadeSeconds > 0) {
                        val cfMs = crossfadeSeconds * 1000L
                        if (cfMs > 0L && dur > cfMs + 1000L && controller.hasNextMediaItem()) {
                            val remaining = dur - pos
                            if (remaining in 1..cfMs) {
                                val fade = (remaining.toFloat() / cfMs.toFloat()).coerceIn(0.15f, 1f)
                                controller.volume = _state.value.volume * fade
                            } else if (pos < 1200L) {
                                val fadeIn = (pos.toFloat() / 1200f).coerceIn(0.25f, 1f)
                                controller.volume = _state.value.volume * fadeIn
                            } else if (controller.volume != _state.value.volume) {
                                controller.volume = _state.value.volume
                            }
                        }
                    }
                }
                tick++
                delay(200L)
            }
        }
    }

    private fun Song.toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putLong("song_id", id)
            putString("format", format)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .setExtras(extras)
            .build()
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setMediaMetadata(metadata)
            .build()
    }
}

package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TrakasApp
import com.example.data.local.AppPreferences
import com.example.data.local.ListeningSessionEntity
import com.example.data.local.PlayHistoryEntity
import com.example.data.repository.AiPlaylistSuggestion
import com.example.data.repository.AiTaskState
import com.example.data.repository.ListeningStatsSummary
import com.example.data.repository.TrakasReplaySummary
import com.example.model.AlbumGroup
import com.example.model.AppLanguage
import com.example.model.ArtistGroup
import com.example.model.AudioQuality
import com.example.model.BuiltInThemes
import com.example.model.EqPreset
import com.example.model.FolderGroup
import com.example.model.GenreGroup
import com.example.model.IconStyle
import com.example.model.InterfaceMode
import com.example.model.LibraryGrouping
import com.example.model.LibraryNavigationTarget
import com.example.model.PlaylistWithSongs
import com.example.model.RepeatModeOption
import com.example.model.SmartPlaylistKind
import com.example.model.Song
import com.example.model.SongTechnicalInfo
import com.example.model.StatsPeriod
import com.example.model.TrakasThemeConfig
import com.example.model.VisualizerConfig
import com.example.model.VisualizerStyle
import com.example.player.AudioOutputStatus
import com.example.player.EqUiState
import com.example.player.PlaybackUiState
import com.example.player.VisualizerFrame
import com.example.player.VisualizerStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val allSongs: List<Song> = emptyList(),
    val visibleSongs: List<Song> = emptyList(),
    val albums: List<AlbumGroup> = emptyList(),
    val soloAlbums: List<AlbumGroup> = emptyList(),
    val compilations: List<AlbumGroup> = emptyList(),
    val artists: List<ArtistGroup> = emptyList(),
    val genres: List<GenreGroup> = emptyList(),
    val folders: List<FolderGroup> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val recentlyPlayed: List<Song> = emptyList(),
    val mostPlayed: List<Song> = emptyList(),
    val importedSongs: List<Song> = emptyList(),
    val playlists: List<PlaylistWithSongs> = emptyList(),
    val lyricsMap: Map<Long, String> = emptyMap(),
    val playHistory: List<PlayHistoryEntity> = emptyList(),
    val isScanning: Boolean = false
) {
    fun buildSmartPlaylist(kind: SmartPlaylistKind): List<Song> {
        val playedIds = playHistory.map { it.songId }.toSet()
        return when (kind) {
            SmartPlaylistKind.MOST_PLAYED -> mostPlayed.ifEmpty { visibleSongs.take(10) }
            SmartPlaylistKind.RECENT -> recentlyPlayed
            SmartPlaylistKind.FAVORITES -> favoriteSongs
            SmartPlaylistKind.RECENTLY_ADDED -> visibleSongs.sortedByDescending { it.dateAdded }.take(25)
            SmartPlaylistKind.NEVER_PLAYED -> visibleSongs.filter { it.id !in playedIds }
            SmartPlaylistKind.BY_ARTIST -> visibleSongs.sortedBy { it.artist.lowercase() }
            SmartPlaylistKind.BY_GENRE -> visibleSongs.sortedBy { it.genre.lowercase() }
            SmartPlaylistKind.BY_YEAR -> visibleSongs.sortedByDescending { it.year }
        }
    }
}

class TrakasViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TrakasApp
    private val repo = app.musicRepository
    private val aiRepo = app.trakasAiRepository
    private val prefsStore = app.settingsDataStore
    private val playback = app.playbackController
    private val eqController = app.audioEffectsController
    private val bluetoothMonitor = app.bluetoothMonitor

    val preferences: StateFlow<AppPreferences> = prefsStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())

    val visualizerConfig: StateFlow<VisualizerConfig> = preferences.map { p ->
        VisualizerConfig(
            style = p.visualizerStyle,
            intensity = p.visualizerIntensity,
            speed = p.visualizerSpeed,
            sensitivity = p.visualizerSensitivity,
            transparency = p.visualizerAlpha,
            particleCount = p.visualizerParticleCount,
            useAlbumColors = p.visualizerUseAlbumColors
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VisualizerConfig())

    val allThemes: StateFlow<List<TrakasThemeConfig>> = repo.allThemes
        .stateIn(viewModelScope, SharingStarted.Eagerly, BuiltInThemes.all)

    val activeTheme: StateFlow<TrakasThemeConfig> = combine(
        preferences,
        allThemes
    ) { prefs, themes ->
        themes.find { it.id == prefs.selectedThemeId } ?: BuiltInThemes.trakasDark
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BuiltInThemes.trakasDark)

    val playbackState: StateFlow<PlaybackUiState> = playback.state
    val eqState: StateFlow<EqUiState> = eqController.state
    val visualizerFrame: StateFlow<VisualizerFrame> = VisualizerStateHolder.frame
    val audioOutputStatus: StateFlow<AudioOutputStatus> = bluetoothMonitor.status

    private val _hasAudioPermission = MutableStateFlow(false)
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statsPeriod = MutableStateFlow(StatsPeriod.WEEKLY)
    val statsPeriod: StateFlow<StatsPeriod> = _statsPeriod.asStateFlow()

    private val _aiState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val aiState: StateFlow<AiTaskState> = _aiState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _libraryTarget = MutableStateFlow<LibraryNavigationTarget?>(null)
    val libraryTarget: StateFlow<LibraryNavigationTarget?> = _libraryTarget.asStateFlow()

    val libraryState: StateFlow<LibraryUiState> = combine(
        combine(repo.allSongs, repo.favoriteIds, repo.playHistory) { allSongs, favIds, history ->
            Triple(allSongs, favIds, history)
        },
        combine(repo.playlists, repo.lyricsMap) { playlists, lyrics ->
            playlists to lyrics
        },
        combine(repo.isScanning, preferences) { scanning, prefs ->
            scanning to prefs
        }
    ) { (allSongs, favIds, history), (playlists, lyricsMap), (scanning, prefs) ->
        val filteredAll = allSongs.filterNot { it.id.toString() in prefs.removedSongIds }
        playback.updateCatalog(filteredAll, favIds)

        val visibleSongs = filteredAll.filterNot { it.folderPath in prefs.excludedFolders }
        val songMap = visibleSongs.associateBy { it.id }

        val grouped = LibraryGrouping.buildLibraryGroups(visibleSongs, history)

        val genres = visibleSongs
            .groupBy { it.genre }
            .map { (genre, list) -> GenreGroup(name = genre, songs = list) }
            .sortedBy { it.name.lowercase() }

        val folders = allSongs
            .groupBy { it.folderPath to it.folderName }
            .map { (key, list) -> FolderGroup(name = key.second, path = key.first, songs = list) }
            .sortedBy { it.name.lowercase() }

        val favSongs = visibleSongs.filter { it.id in favIds }
        val recent = history.sortedByDescending { it.lastPlayedAt }.mapNotNull { songMap[it.songId] }
        val most = history.sortedByDescending { it.playCount }.mapNotNull { songMap[it.songId] }
        val imported = visibleSongs.filter { it.isImported }

        LibraryUiState(
            allSongs = allSongs,
            visibleSongs = visibleSongs,
            albums = grouped.allAlbums,
            soloAlbums = grouped.soloAlbums,
            compilations = grouped.compilations,
            artists = grouped.artists,
            genres = genres,
            folders = folders,
            favoriteSongs = favSongs,
            favoriteIds = favIds,
            recentlyPlayed = recent,
            mostPlayed = most,
            importedSongs = imported,
            playlists = playlists,
            lyricsMap = lyricsMap,
            playHistory = history,
            isScanning = scanning
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    val statsSummary: StateFlow<ListeningStatsSummary> = combine(
        statsPeriod,
        libraryState,
        repo.listeningSessions
    ) { period, lib, sessions ->
        aiRepo.computeLocalStats(
            period = period,
            songs = lib.visibleSongs,
            history = lib.playHistory,
            sessions = sessions,
            favoriteIds = lib.favoriteIds
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ListeningStatsSummary(0, 0f, 0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), "Noche", "")
    )

    val replaySummary: StateFlow<TrakasReplaySummary> = combine(
        libraryState,
        repo.listeningSessions
    ) { lib, sessions ->
        aiRepo.computeLocalReplay(lib.visibleSongs, lib.playHistory, sessions)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TrakasReplaySummary(null, 0, null, null, null, 0, 0, emptyList())
    )

    init {
        viewModelScope.launch {
            val savedEq = repo.eqSettings.first()
            eqController.applyPersisted(savedEq)
        }
        viewModelScope.launch {
            preferences.collect { prefs ->
                playback.gaplessEnabled = prefs.gaplessPlayback
                playback.crossfadeSeconds = prefs.crossfadeSeconds
                playback.normalizeVolume = prefs.normalizeVolume
                playback.replayGainEnabled = prefs.replayGainEnabled
                playback.refreshNormalization()
                playback.setShuffle(prefs.shuffleEnabled, prefs.smartShuffleEnabled)
                playback.setRepeatMode(prefs.repeatMode)
                playback.setVolume(prefs.playerVolume)
                playback.setPlaybackSpeed(prefs.playbackSpeed)
                app.mainAudioProcessor.visualizerSensitivity = prefs.visualizerSensitivity
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun onPermissionResult(granted: Boolean) {
        _hasAudioPermission.value = granted
        if (granted) {
            rescanLibrary()
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            repo.scanDeviceLibrary()
            bluetoothMonitor.refreshCurrentDevice()
        }
    }

    fun importAudioFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val count = repo.importAudioUris(uris)
            _statusMessage.value = "$count pistas importadas"
        }
    }

    fun createLocalDemoWavTracks() {
        viewModelScope.launch {
            val count = repo.createLocalDemoWavTracks()
            _statusMessage.value = "$count pistas locales listas"
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Playback Controls
    fun playSongs(songs: List<Song>, index: Int = 0) = playback.playSongs(songs, index)

    fun playOrdered(songs: List<Song>, index: Int = 0) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            prefsStore.setShuffleEnabled(false)
            playback.setShuffle(false, preferences.value.smartShuffleEnabled)
            playback.playSongs(songs, index, allowSmartExtras = false)
        }
    }

    fun playShuffled(songs: List<Song>) {
        if (songs.isEmpty()) return
        val randomized = songs.shuffled()
        playback.playSongs(randomized, 0, allowSmartExtras = false)
    }

    fun playSong(song: Song, fromList: List<Song> = libraryState.value.visibleSongs) {
        val list = if (fromList.isEmpty()) listOf(song) else fromList
        val idx = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playback.playSongs(list, idx)
    }

    fun togglePlayPause() = playback.togglePlayPause()
    fun skipNext() = playback.skipNext()
    fun skipPrevious() = playback.skipPrevious()
    fun seekTo(ms: Long) = playback.seekTo(ms)
    fun playNext(song: Song) = playback.playNext(song)
    fun playSongsNext(songs: List<Song>) = playback.playSongsNext(songs)
    fun addToQueue(song: Song) = playback.addToQueue(song)
    fun addSongsToQueue(songs: List<Song>) = playback.addSongsToQueue(songs)
    fun moveQueueItem(from: Int, to: Int) = playback.moveQueueItem(from, to)
    fun removeFromQueue(index: Int) = playback.removeFromQueue(index)
    fun jumpToQueueIndex(index: Int) = playback.jumpToQueueIndex(index)
    fun clearQueue() = playback.clearQueue()

    fun toggleAlbumFavorite(album: AlbumGroup) {
        if (album.songs.isEmpty()) return
        viewModelScope.launch {
            val favs = libraryState.value.favoriteIds
            val allFav = album.songs.all { it.id in favs }
            album.songs.forEach { song ->
                val currentlyFav = song.id in favs
                if (allFav && currentlyFav) {
                    repo.toggleFavorite(song.id)
                } else if (!allFav && !currentlyFav) {
                    repo.toggleFavorite(song.id)
                }
            }
            _statusMessage.value = if (allFav) {
                "'${album.name}' quitado de favoritos"
            } else {
                "'${album.name}' agregado a favoritos"
            }
        }
    }

    fun navigateToArtist(artistName: String) {
        val primary = LibraryGrouping.parseArtistCredits(artistName).primaryArtist
        _libraryTarget.value = LibraryNavigationTarget.Artist(primary)
    }

    fun navigateToAlbum(albumName: String, albumId: Long? = null) {
        _libraryTarget.value = LibraryNavigationTarget.Album(albumName, albumId)
    }

    fun consumeLibraryTarget() {
        _libraryTarget.value = null
    }

    fun removeFromLibrary(song: Song) {
        viewModelScope.launch {
            prefsStore.markSongRemoved(song.id)
            if (song.isImported) {
                repo.deleteImportedTrack(song.id)
            }
            val currentIdx = playback.state.value.queue.indexOfFirst { it.id == song.id }
            if (currentIdx >= 0) {
                playback.removeFromQueue(currentIdx)
            }
            _statusMessage.value = "'${song.title}' eliminada de la biblioteca"
        }
    }

    fun updateCoverArt(song: Song, customArtUri: String?) {
        viewModelScope.launch {
            repo.saveMetadataOverride(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                genre = song.genre,
                year = song.year,
                trackNumber = song.trackNumber,
                albumArtist = song.albumArtist,
                comment = song.comment,
                customArtUri = customArtUri
            )
            _statusMessage.value = if (customArtUri == null) "Portada restaurada" else "Portada actualizada"
        }
    }

    suspend fun loadTechnicalInfo(song: Song): SongTechnicalInfo {
        return repo.readTechnicalInfo(song)
    }

    fun toggleShuffle() {
        viewModelScope.launch {
            val next = !preferences.value.shuffleEnabled
            prefsStore.setShuffleEnabled(next)
            playback.setShuffle(next, preferences.value.smartShuffleEnabled)
        }
    }

    fun toggleSmartShuffle() {
        viewModelScope.launch {
            val next = !preferences.value.smartShuffleEnabled
            prefsStore.setSmartShuffleEnabled(next)
            if (next && !preferences.value.shuffleEnabled) {
                prefsStore.setShuffleEnabled(true)
                playback.setShuffle(true, true)
            } else {
                playback.setShuffle(preferences.value.shuffleEnabled, next)
            }
        }
    }

    fun cycleRepeatMode() {
        viewModelScope.launch {
            val next = when (preferences.value.repeatMode) {
                RepeatModeOption.OFF -> RepeatModeOption.ALL
                RepeatModeOption.ALL -> RepeatModeOption.ONE
                RepeatModeOption.ONE -> RepeatModeOption.OFF
            }
            prefsStore.setRepeatMode(next)
            playback.setRepeatMode(next)
        }
    }

    fun setVolume(volume: Float) {
        playback.setVolume(volume)
        viewModelScope.launch { prefsStore.setPlayerVolume(volume) }
    }

    fun setPlaybackSpeed(speed: Float) {
        playback.setPlaybackSpeed(speed)
        viewModelScope.launch { prefsStore.setPlaybackSpeed(speed) }
    }

    fun setNormalizeVolume(enabled: Boolean) {
        viewModelScope.launch { prefsStore.setNormalizeVolume(enabled) }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsStore.setReplayGainEnabled(enabled) }
    }

    fun startSleepTimer(minutes: Int) = playback.startSleepTimer(minutes)
    fun cancelSleepTimer() = playback.cancelSleepTimer()

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { repo.toggleFavorite(songId) }
    }

    // Lyrics & Metadata Editor
    fun saveLyrics(songId: Long, content: String) {
        viewModelScope.launch {
            repo.saveLyrics(songId, content)
            _statusMessage.value = "Letras guardadas"
        }
    }

    fun setLyricsKaraoke(enabled: Boolean) = viewModelScope.launch { prefsStore.setLyricsKaraokeMode(enabled) }
    fun setLyricsFontSize(sp: Float) = viewModelScope.launch { prefsStore.setLyricsFontSize(sp) }
    fun setLyricsFontKey(key: String) = viewModelScope.launch { prefsStore.setLyricsFontKey(key) }
    fun setLyricsTextColor(argb: Long) = viewModelScope.launch { prefsStore.setLyricsTextColor(argb) }
    fun setLyricsHighlightColor(argb: Long) = viewModelScope.launch { prefsStore.setLyricsHighlightColor(argb) }
    fun setLyricsDynamicBg(enabled: Boolean) = viewModelScope.launch { prefsStore.setLyricsDynamicBg(enabled) }

    fun saveMetadataOverride(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: Int,
        trackNumber: Int,
        albumArtist: String,
        comment: String,
        customArtUri: String?
    ) {
        viewModelScope.launch {
            repo.saveMetadataOverride(
                songId, title, artist, album, genre, year, trackNumber, albumArtist, comment, customArtUri
            )
            _statusMessage.value = "Cambios guardados"
        }
    }

    // Equalizer
    fun eqSetEnabled(enabled: Boolean) {
        val entity = eqController.setEnabled(enabled)
        viewModelScope.launch { repo.saveEqSettings(entity) }
    }

    fun eqSelectPreset(preset: EqPreset) {
        val entity = eqController.selectPreset(preset)
        viewModelScope.launch { repo.saveEqSettings(entity) }
    }

    fun eqSetBandLevel(index: Int, levelMb: Int) {
        val entity = eqController.setBandLevel(index, levelMb)
        viewModelScope.launch { repo.saveEqSettings(entity) }
    }

    fun eqSetBassBoost(strength: Int) {
        val entity = eqController.setBassBoost(strength)
        viewModelScope.launch { repo.saveEqSettings(entity) }
    }

    fun eqSetVirtualizer(strength: Int) {
        val entity = eqController.setVirtualizer(strength)
        viewModelScope.launch { repo.saveEqSettings(entity) }
    }

    fun eqSetBalance(balance: Float) {
        val entity = eqController.setBalance(balance)
        viewModelScope.launch { repo.saveEqSettings(entity) }
    }

    fun eqSetBassTone(bassDb: Float) {
        val entity = eqController.setBassTone(bassDb)
        viewModelScope.launch { repo.saveEqSettings(entity) }
    }

    fun eqSetTrebleTone(trebleDb: Float) {
        val entity = eqController.setTrebleTone(trebleDb)
        viewModelScope.launch { repo.saveEqSettings(entity) }
    }

    // Playlists
    fun createPlaylist(name: String, initialSongId: Long? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repo.createPlaylist(name)
            if (initialSongId != null) {
                repo.addSongToPlaylist(id, initialSongId)
            }
        }
    }

    fun createPlaylistFromSongs(name: String, songs: List<Song>) {
        if (name.isBlank() || songs.isEmpty()) return
        viewModelScope.launch {
            val id = repo.createPlaylist(name)
            songs.forEach { repo.addSongToPlaylist(id, it.id) }
            _statusMessage.value = "Playlist '$name' guardada"
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repo.renamePlaylist(playlistId, newName) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { repo.deletePlaylist(playlistId) }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repo.addSongToPlaylist(playlistId, songId) }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repo.removeSongFromPlaylist(playlistId, songId) }
    }

    fun movePlaylistSong(playlistId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch { repo.movePlaylistSong(playlistId, fromIndex, toIndex) }
    }

    // Trakas AI & Stats
    fun setStatsPeriod(period: StatsPeriod) {
        _statsPeriod.value = period
    }

    fun requestAiGeneration(promptGoal: String) {
        viewModelScope.launch {
            _aiState.value = AiTaskState.Loading
            val lib = libraryState.value
            val result = aiRepo.generateAiPlaylistOrInsight(
                promptGoal = promptGoal,
                songs = lib.visibleSongs,
                history = lib.playHistory,
                favoriteIds = lib.favoriteIds
            )
            _aiState.value = result.fold(
                onSuccess = { AiTaskState.Success(it) },
                onFailure = { AiTaskState.Error(it.localizedMessage ?: "Error al consultar Trakas AI") }
            )
        }
    }

    fun findLocalSimilar(anchor: Song): List<Song> {
        return aiRepo.findLocalSimilarSongs(anchor, libraryState.value.visibleSongs)
    }

    fun resetAiState() {
        _aiState.value = AiTaskState.Idle
    }

    // Themes, Interface Modes, Visualizers & Settings
    fun setInterfaceMode(mode: InterfaceMode) {
        viewModelScope.launch { prefsStore.setInterfaceMode(mode) }
    }

    fun selectTheme(theme: TrakasThemeConfig) {
        viewModelScope.launch {
            prefsStore.setThemeId(theme.id)
            prefsStore.setVisualizerStyle(VisualizerStyle.fromId(theme.visualizerStyleId))
            prefsStore.setAnimationsEnabled(theme.animationsEnabled)
        }
    }

    fun saveCustomTheme(theme: TrakasThemeConfig) {
        viewModelScope.launch {
            repo.saveCustomTheme(theme)
            prefsStore.setThemeId(theme.id)
            prefsStore.setVisualizerStyle(VisualizerStyle.fromId(theme.visualizerStyleId))
            prefsStore.setAnimationsEnabled(theme.animationsEnabled)
            _statusMessage.value = "Tema '${theme.name}' guardado"
        }
    }

    fun deleteCustomTheme(themeId: String) {
        viewModelScope.launch {
            repo.deleteCustomTheme(themeId)
            if (preferences.value.selectedThemeId == themeId) {
                prefsStore.setThemeId(BuiltInThemes.trakasDark.id)
            }
        }
    }

    fun setIconStyle(style: IconStyle) = viewModelScope.launch { prefsStore.setIconStyle(style) }
    fun setVisualizerStyle(style: VisualizerStyle) = viewModelScope.launch { prefsStore.setVisualizerStyle(style) }
    fun setVisualizerIntensity(v: Float) = viewModelScope.launch { prefsStore.setVisualizerIntensity(v) }
    fun setVisualizerSpeed(v: Float) = viewModelScope.launch { prefsStore.setVisualizerSpeed(v) }
    fun setVisualizerSensitivity(v: Float) = viewModelScope.launch { prefsStore.setVisualizerSensitivity(v) }
    fun setVisualizerAlpha(v: Float) = viewModelScope.launch { prefsStore.setVisualizerAlpha(v) }
    fun setVisualizerParticles(count: Int) = viewModelScope.launch { prefsStore.setVisualizerParticleCount(count) }
    fun setVisualizerUseAlbumColors(b: Boolean) = viewModelScope.launch { prefsStore.setVisualizerUseAlbumColors(b) }
    fun setAudioQuality(quality: AudioQuality) = viewModelScope.launch { prefsStore.setAudioQuality(quality) }
    fun setGaplessPlayback(enabled: Boolean) = viewModelScope.launch { prefsStore.setGaplessPlayback(enabled) }
    fun setCrossfadeSeconds(seconds: Int) = viewModelScope.launch { prefsStore.setCrossfadeSeconds(seconds) }
    fun setAnimationsEnabled(enabled: Boolean) = viewModelScope.launch { prefsStore.setAnimationsEnabled(enabled) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { prefsStore.setNotificationsEnabled(enabled) }
    fun setLockscreenControlsEnabled(enabled: Boolean) = viewModelScope.launch { prefsStore.setLockscreenControlsEnabled(enabled) }
    fun setBluetoothAutoPlay(enabled: Boolean) = viewModelScope.launch { prefsStore.setBluetoothAutoPlay(enabled) }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { prefsStore.setLanguage(language) }
    fun toggleFolderExcluded(folderPath: String) = viewModelScope.launch { prefsStore.toggleFolderExcluded(folderPath) }
    fun setPlayerDynamicBackground(enabled: Boolean) = viewModelScope.launch { prefsStore.setPlayerDynamicBackground(enabled) }
    fun setPlayerVisualizerEnabled(enabled: Boolean) = viewModelScope.launch { prefsStore.setPlayerVisualizerEnabled(enabled) }
    fun setImmersivePlayerMode(enabled: Boolean) = viewModelScope.launch { prefsStore.setImmersivePlayerMode(enabled) }
    fun setTextScaleFactor(scale: Float) = viewModelScope.launch { prefsStore.setTextScaleFactor(scale) }

    // Backup Export / Import (JSON)
    fun exportBackupToUri(uri: Uri, includePlaylists: Boolean, includeThemes: Boolean, includeSettings: Boolean) {
        viewModelScope.launch {
            runCatching {
                val jsonStr = repo.buildBackupJson(
                    prefs = preferences.value,
                    includePlaylists = includePlaylists,
                    includeThemes = includeThemes,
                    includeSettings = includeSettings
                )
                app.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonStr.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                _statusMessage.value = "Respaldo exportado correctamente"
            }.onFailure {
                _statusMessage.value = "Error al exportar respaldo"
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val raw = app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Archivo vacío")
                val doc = repo.restoreBackupJson(raw)
                doc.settings?.let { s ->
                    prefsStore.setThemeId(s.selectedThemeId)
                    prefsStore.setIconStyle(IconStyle.fromId(s.iconStyleId))
                    prefsStore.setVisualizerStyle(VisualizerStyle.fromId(s.visualizerStyleId))
                    prefsStore.setAudioQuality(AudioQuality.fromId(s.audioQualityId))
                    prefsStore.setGaplessPlayback(s.gaplessPlayback)
                    prefsStore.setCrossfadeSeconds(s.crossfadeSeconds)
                    prefsStore.setAnimationsEnabled(s.animationsEnabled)
                    prefsStore.setSmartShuffleEnabled(s.smartShuffleEnabled)
                    prefsStore.setNormalizeVolume(s.normalizeVolume)
                    prefsStore.setReplayGainEnabled(s.replayGainEnabled)
                    prefsStore.setPlaybackSpeed(s.playbackSpeed)
                    prefsStore.setLanguage(AppLanguage.fromCode(s.languageCode))
                }
            }.onSuccess {
                _statusMessage.value = "Respaldo restaurado"
            }.onFailure {
                _statusMessage.value = "No se pudo importar el respaldo"
            }
        }
    }
}

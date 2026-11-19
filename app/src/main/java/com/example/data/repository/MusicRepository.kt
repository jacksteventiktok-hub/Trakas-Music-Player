package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.data.local.AppPreferences
import com.example.data.local.CustomThemeEntity
import com.example.data.local.EqSettingsEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.ListeningSessionEntity
import com.example.data.local.LyricsEntity
import com.example.data.local.MetadataOverrideEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.data.local.TrakasDao
import com.example.data.repository.MusicScanner.toSong
import com.example.model.BackupPlaylistEntry
import com.example.model.BackupSettingsPayload
import com.example.model.BuiltInThemes
import com.example.model.PlaylistWithSongs
import com.example.model.Song
import com.example.model.SongTechnicalInfo
import com.example.model.TrakasBackupDocument
import com.example.model.TrakasThemeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MusicRepository(
    private val context: Context,
    private val dao: TrakasDao
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _deviceSongs = MutableStateFlow<List<Song>>(emptyList())
    val deviceSongs: StateFlow<List<Song>> = _deviceSongs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val allSongs: Flow<List<Song>> = combine(
        _deviceSongs,
        dao.observeImportedTracks(),
        dao.observeMetadataOverrides()
    ) { device, imported, overrides ->
        val overrideMap = overrides.associateBy { it.songId }
        val importedSongs = imported.map { it.toSong() }
        (device + importedSongs)
            .distinctBy { it.id }
            .map { song ->
                val ov = overrideMap[song.id]
                if (ov == null) {
                    song
                } else {
                    song.copy(
                        title = ov.title,
                        artist = ov.artist,
                        album = ov.album,
                        genre = ov.genre,
                        year = ov.year,
                        trackNumber = ov.trackNumber,
                        albumArtist = ov.albumArtist,
                        comment = ov.comment,
                        albumArtUri = ov.customArtUri?.let { Uri.parse(it) } ?: song.albumArtUri
                    )
                }
            }
    }

    val favoriteIds: Flow<Set<Long>> = dao.observeFavorites().map { list ->
        list.map { it.songId }.toSet()
    }

    val playHistory = dao.observePlayHistory()
    val listeningSessions: Flow<List<ListeningSessionEntity>> = dao.observeListeningSessions()

    val lyricsMap: Flow<Map<Long, String>> = dao.observeAllLyrics().map { list ->
        list.associate { it.songId to it.content }
    }

    val playlists: Flow<List<PlaylistWithSongs>> = combine(
        dao.observePlaylists(),
        dao.observeAllPlaylistSongs(),
        allSongs
    ) { playlistEntities, crossRefs, songs ->
        val songMap = songs.associateBy { it.id }
        val grouped = crossRefs.groupBy { it.playlistId }
        playlistEntities.map { pl ->
            val plSongs = grouped[pl.id]
                .orEmpty()
                .sortedBy { it.position }
                .mapNotNull { songMap[it.songId] }
            PlaylistWithSongs(
                id = pl.id,
                name = pl.name,
                createdAt = pl.createdAt,
                songs = plSongs
            )
        }
    }

    val allThemes: Flow<List<TrakasThemeConfig>> = dao.observeCustomThemes().map { custom ->
        BuiltInThemes.all + custom.map { entity ->
            TrakasThemeConfig(
                id = entity.id,
                name = entity.name,
                isBuiltIn = false,
                primaryArgb = entity.primaryArgb,
                secondaryArgb = entity.secondaryArgb,
                backgroundArgb = entity.backgroundArgb,
                buttonArgb = entity.buttonArgb,
                textArgb = entity.textArgb,
                cardArgb = entity.cardArgb,
                progressArgb = entity.progressArgb,
                visualizerArgb = entity.visualizerArgb,
                cardAlpha = entity.cardAlpha,
                buttonShapeId = entity.buttonShapeId,
                effectIntensity = entity.effectIntensity,
                visualizerStyleId = entity.visualizerStyleId,
                animationsEnabled = entity.animationsEnabled
            )
        }
    }

    val eqSettings: Flow<EqSettingsEntity> = dao.observeEqSettings().map {
        it ?: EqSettingsEntity()
    }

    suspend fun scanDeviceLibrary() {
        _isScanning.value = true
        try {
            _deviceSongs.value = MusicScanner.scanDeviceAudio(context)
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun importAudioUris(uris: List<Uri>): Int {
        val entities = uris.mapNotNull { MusicScanner.importUri(context, it) }
        if (entities.isNotEmpty()) {
            dao.insertImportedTracks(entities)
        }
        return entities.size
    }

    suspend fun createLocalDemoWavTracks(): Int {
        val tracks = SampleAudioGenerator.generateLocalWavTracks(context)
        dao.insertImportedTracks(tracks)
        return tracks.size
    }

    suspend fun deleteImportedTrack(songId: Long) {
        dao.deleteImportedTrack(songId)
    }

    suspend fun readTechnicalInfo(song: Song): SongTechnicalInfo = withContext(Dispatchers.IO) {
        var bitrateKbps = 0
        var sampleRateHz = 44100
        var actualSize = song.sizeBytes
        runCatching {
            if (actualSize <= 0L) {
                context.contentResolver.openAssetFileDescriptor(song.contentUri, "r")?.use { afd ->
                    if (afd.length > 0L) actualSize = afd.length
                }
            }
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, song.contentUri)
            val br = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            if (br > 0) bitrateKbps = br / 1000
            val sr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 0
            if (sr > 0) sampleRateHz = sr
            retriever.release()
        }
        if (bitrateKbps <= 0 && song.durationMs > 0L && actualSize > 0L) {
            bitrateKbps = ((actualSize * 8L) / song.durationMs).toInt().coerceAtLeast(64)
        } else if (bitrateKbps <= 0) {
            bitrateKbps = when (song.format.uppercase()) {
                "FLAC" -> 960
                "WAV" -> 1411
                else -> 320
            }
        }
        SongTechnicalInfo(
            format = song.format,
            bitrateKbps = bitrateKbps,
            sampleRateHz = sampleRateHz,
            durationMs = song.durationMs,
            sizeBytes = actualSize
        )
    }

    suspend fun toggleFavorite(songId: Long) {
        if (dao.isFavorite(songId)) {
            dao.removeFavorite(songId)
        } else {
            dao.addFavorite(FavoriteEntity(songId = songId))
        }
    }

    suspend fun recordPlay(songId: Long) {
        dao.recordPlay(songId)
    }

    suspend fun recordListeningSession(songId: Long, durationMs: Long) {
        if (durationMs < 1500L) return
        dao.insertListeningSession(
            ListeningSessionEntity(
                songId = songId,
                durationListenedMs = durationMs
            )
        )
    }

    suspend fun saveLyrics(songId: Long, content: String) {
        dao.saveLyrics(LyricsEntity(songId = songId, content = content.trim()))
    }

    suspend fun saveMetadataOverride(
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
        dao.saveMetadataOverride(
            MetadataOverrideEntity(
                songId = songId,
                title = title.trim().ifEmpty { "Sin título" },
                artist = artist.trim().ifEmpty { "Artista desconocido" },
                album = album.trim().ifEmpty { "Álbum desconocido" },
                genre = genre.trim().ifEmpty { "Audio Local" },
                year = year.coerceIn(1900, 2100),
                trackNumber = trackNumber.coerceAtLeast(1),
                albumArtist = albumArtist.trim(),
                comment = comment.trim(),
                customArtUri = customArtUri
            )
        )
    }

    suspend fun createPlaylist(name: String): Long {
        return dao.insertPlaylist(PlaylistEntity(name = name.trim()))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        dao.renamePlaylist(playlistId, newName.trim())
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        dao.addSongToPlaylist(playlistId, songId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        dao.deleteSongFromPlaylist(playlistId, songId)
    }

    suspend fun movePlaylistSong(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val entries = dao.getPlaylistSongs(playlistId).toMutableList()
        if (fromIndex !in entries.indices || toIndex !in entries.indices) return
        val item = entries.removeAt(fromIndex)
        entries.add(toIndex, item)
        val reindexed = entries.mapIndexed { idx, e ->
            PlaylistSongEntity(entryId = e.entryId, playlistId = e.playlistId, songId = e.songId, position = idx)
        }
        dao.updatePlaylistSongs(reindexed)
    }

    suspend fun saveCustomTheme(config: TrakasThemeConfig) {
        dao.insertCustomTheme(
            CustomThemeEntity(
                id = config.id,
                name = config.name,
                primaryArgb = config.primaryArgb,
                secondaryArgb = config.secondaryArgb,
                backgroundArgb = config.backgroundArgb,
                buttonArgb = config.buttonArgb,
                textArgb = config.textArgb,
                cardArgb = config.cardArgb,
                progressArgb = config.progressArgb,
                visualizerArgb = config.visualizerArgb,
                cardAlpha = config.cardAlpha,
                buttonShapeId = config.buttonShapeId,
                effectIntensity = config.effectIntensity,
                visualizerStyleId = config.visualizerStyleId,
                animationsEnabled = config.animationsEnabled
            )
        )
    }

    suspend fun deleteCustomTheme(themeId: String) {
        dao.deleteCustomTheme(themeId)
    }

    suspend fun saveEqSettings(settings: EqSettingsEntity) {
        dao.saveEqSettings(settings)
    }

    suspend fun buildBackupJson(
        prefs: AppPreferences,
        includePlaylists: Boolean = true,
        includeThemes: Boolean = true,
        includeSettings: Boolean = true
    ): String {
        val currentPlaylists = if (includePlaylists) {
            playlists.first().map { pl ->
                BackupPlaylistEntry(
                    name = pl.name,
                    songTitles = pl.songs.map { it.title },
                    songIds = pl.songs.map { it.id }
                )
            }
        } else emptyList()

        val customThemes = if (includeThemes) {
            dao.observeCustomThemes().first().map { e ->
                TrakasThemeConfig(
                    id = e.id,
                    name = e.name,
                    isBuiltIn = false,
                    primaryArgb = e.primaryArgb,
                    secondaryArgb = e.secondaryArgb,
                    backgroundArgb = e.backgroundArgb,
                    buttonArgb = e.buttonArgb,
                    textArgb = e.textArgb,
                    cardArgb = e.cardArgb,
                    progressArgb = e.progressArgb,
                    visualizerArgb = e.visualizerArgb,
                    cardAlpha = e.cardAlpha,
                    buttonShapeId = e.buttonShapeId,
                    effectIntensity = e.effectIntensity,
                    visualizerStyleId = e.visualizerStyleId,
                    animationsEnabled = e.animationsEnabled
                )
            }
        } else emptyList()

        val settingsPayload = if (includeSettings) {
            BackupSettingsPayload(
                selectedThemeId = prefs.selectedThemeId,
                iconStyleId = prefs.iconStyle.id,
                visualizerStyleId = prefs.visualizerStyle.id,
                audioQualityId = prefs.audioQuality.id,
                gaplessPlayback = prefs.gaplessPlayback,
                crossfadeSeconds = prefs.crossfadeSeconds,
                animationsEnabled = prefs.animationsEnabled,
                smartShuffleEnabled = prefs.smartShuffleEnabled,
                normalizeVolume = prefs.normalizeVolume,
                replayGainEnabled = prefs.replayGainEnabled,
                playbackSpeed = prefs.playbackSpeed,
                languageCode = prefs.language.code
            )
        } else null

        val doc = TrakasBackupDocument(
            version = 1,
            playlists = currentPlaylists,
            customThemes = customThemes,
            settings = settingsPayload
        )
        return json.encodeToString(TrakasBackupDocument.serializer(), doc)
    }

    suspend fun restoreBackupJson(rawJson: String): TrakasBackupDocument {
        val doc = json.decodeFromString(TrakasBackupDocument.serializer(), rawJson)
        val availableSongs = allSongs.first()
        val byId = availableSongs.associateBy { it.id }
        val byTitle = availableSongs.associateBy { it.title.lowercase() }

        doc.customThemes.forEach { theme ->
            saveCustomTheme(theme.copy(isBuiltIn = false))
        }

        doc.playlists.forEach { pl ->
            val newId = createPlaylist(pl.name)
            val resolvedIds = buildList {
                pl.songIds.forEachIndexed { idx, sId ->
                    val match = byId[sId] ?: pl.songTitles.getOrNull(idx)?.let { byTitle[it.lowercase()] }
                    if (match != null) add(match.id)
                }
            }.distinct()
            resolvedIds.forEach { sId -> addSongToPlaylist(newId, sId) }
        }

        return doc
    }
}

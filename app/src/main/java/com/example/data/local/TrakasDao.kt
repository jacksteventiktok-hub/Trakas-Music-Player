package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrakasDao {

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs ORDER BY playlistId, position ASC")
    fun observeAllPlaylistSongs(): Flow<List<PlaylistSongEntity>>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistSongs(playlistId: Long): List<PlaylistSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(entry: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deleteSongFromPlaylist(playlistId: Long, songId: Long)

    @Update
    suspend fun updatePlaylistSongs(entries: List<PlaylistSongEntity>)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPlaylistPosition(playlistId: Long): Int

    @Transaction
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val nextPos = getMaxPlaylistPosition(playlistId) + 1
        insertPlaylistSong(PlaylistSongEntity(playlistId = playlistId, songId = songId, position = nextPos))
    }

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun isFavorite(songId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)

    // Play History & Listening Sessions
    @Query("SELECT * FROM play_history ORDER BY lastPlayedAt DESC")
    fun observePlayHistory(): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history WHERE songId = :songId LIMIT 1")
    suspend fun getHistoryForSong(songId: Long): PlayHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayHistory(history: PlayHistoryEntity)

    @Transaction
    suspend fun recordPlay(songId: Long) {
        val existing = getHistoryForSong(songId)
        val newCount = (existing?.playCount ?: 0) + 1
        upsertPlayHistory(
            PlayHistoryEntity(
                songId = songId,
                playCount = newCount,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
    }

    @Query("SELECT * FROM listening_sessions ORDER BY timestamp DESC")
    fun observeListeningSessions(): Flow<List<ListeningSessionEntity>>

    @Insert
    suspend fun insertListeningSession(session: ListeningSessionEntity)

    // Lyrics
    @Query("SELECT * FROM lyrics")
    fun observeAllLyrics(): Flow<List<LyricsEntity>>

    @Query("SELECT * FROM lyrics WHERE songId = :songId LIMIT 1")
    fun observeLyricsForSong(songId: Long): Flow<LyricsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLyrics(lyrics: LyricsEntity)

    // Metadata Overrides
    @Query("SELECT * FROM metadata_overrides")
    fun observeMetadataOverrides(): Flow<List<MetadataOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadataOverride(override: MetadataOverrideEntity)

    // Custom Themes
    @Query("SELECT * FROM custom_themes ORDER BY createdAt DESC")
    fun observeCustomThemes(): Flow<List<CustomThemeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomTheme(theme: CustomThemeEntity)

    @Query("DELETE FROM custom_themes WHERE id = :themeId")
    suspend fun deleteCustomTheme(themeId: String)

    // EQ Settings
    @Query("SELECT * FROM eq_settings WHERE id = 1")
    fun observeEqSettings(): Flow<EqSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEqSettings(settings: EqSettingsEntity)

    // Imported / Local tracks
    @Query("SELECT * FROM imported_tracks ORDER BY addedAt DESC")
    fun observeImportedTracks(): Flow<List<ImportedTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportedTracks(tracks: List<ImportedTrackEntity>)

    @Query("DELETE FROM imported_tracks WHERE id = :songId")
    suspend fun deleteImportedTrack(songId: Long)
}

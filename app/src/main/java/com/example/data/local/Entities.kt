package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val playlistId: Long,
    val songId: Long,
    val position: Int
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey val songId: Long,
    val playCount: Int = 1,
    val lastPlayedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "listening_sessions")
data class ListeningSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val durationListenedMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val songId: Long,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "metadata_overrides")
data class MetadataOverrideEntity(
    @PrimaryKey val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val year: Int,
    val trackNumber: Int,
    val albumArtist: String,
    val comment: String,
    val customArtUri: String? = null
)

@Entity(tableName = "custom_themes")
data class CustomThemeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val primaryArgb: Long,
    val secondaryArgb: Long,
    val backgroundArgb: Long,
    val buttonArgb: Long,
    val textArgb: Long,
    val cardArgb: Long = 0xFF141B2D,
    val progressArgb: Long = primaryArgb,
    val visualizerArgb: Long = primaryArgb,
    val cardAlpha: Float = 0.82f,
    val buttonShapeId: String = "rounded",
    val effectIntensity: Float = 0.9f,
    val visualizerStyleId: String = "spectrum",
    val animationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "eq_settings")
data class EqSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = true,
    val presetId: String = "flat",
    val band0Mb: Int = 0,
    val band1Mb: Int = 0,
    val band2Mb: Int = 0,
    val band3Mb: Int = 0,
    val band4Mb: Int = 0,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val balance: Float = 0f,
    val bassDb: Float = 0f,
    val trebleDb: Float = 0f
)

@Entity(tableName = "imported_tracks")
data class ImportedTrackEntity(
    @PrimaryKey val id: Long,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String = "Electronic",
    val year: Int = 2025,
    val durationMs: Long,
    val format: String,
    val folderName: String,
    val addedAt: Long = System.currentTimeMillis()
)

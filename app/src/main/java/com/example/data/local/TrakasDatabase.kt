package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        FavoriteEntity::class,
        PlayHistoryEntity::class,
        ListeningSessionEntity::class,
        LyricsEntity::class,
        MetadataOverrideEntity::class,
        CustomThemeEntity::class,
        EqSettingsEntity::class,
        ImportedTrackEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TrakasDatabase : RoomDatabase() {
    abstract fun trakasDao(): TrakasDao

    companion object {
        @Volatile
        private var INSTANCE: TrakasDatabase? = null

        fun getInstance(context: Context): TrakasDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrakasDatabase::class.java,
                    "trakas_music_db"
                ).fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

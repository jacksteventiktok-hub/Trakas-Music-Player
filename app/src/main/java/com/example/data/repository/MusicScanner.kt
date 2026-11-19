package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.local.ImportedTrackEntity
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

object MusicScanner {

    private val supportedExtensions = setOf("mp3", "flac", "wav", "aac", "ogg", "m4a", "opus")

    suspend fun scanDeviceAudio(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATA)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.TRACK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.GENRE)
                add(MediaStore.Audio.Media.ALBUM_ARTIST)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 3000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        runCatching {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val displayCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else -1
                val albumArtistCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                } else -1

                val albumArtBase = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Sin título"
                    val artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Artista desconocido"
                    val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Álbum desconocido"
                    val albumId = cursor.getLong(albumIdCol)
                    val durationMs = cursor.getLong(durationCol)
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val displayName = if (displayCol >= 0) cursor.getString(displayCol) ?: "" else ""
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
                    val ext = displayName.substringAfterLast('.', path.substringAfterLast('.', "")).lowercase()
                    val format = resolveFormat(ext, mime)

                    if (ext.isNotEmpty() && ext !in supportedExtensions && !mime.startsWith("audio/")) {
                        continue
                    }

                    val folderFile = if (path.isNotBlank()) File(path).parentFile else null
                    val folderName = folderFile?.name ?: "Music"
                    val folderPath = folderFile?.absolutePath ?: "/Music"
                    val genre = if (genreCol >= 0) {
                        cursor.getString(genreCol)?.takeIf { it.isNotBlank() } ?: "Audio Local"
                    } else "Audio Local"
                    val year = if (yearCol >= 0) {
                        cursor.getInt(yearCol).takeIf { it in 1950..2100 } ?: 2024
                    } else 2024
                    val trackNumRaw = if (trackCol >= 0) cursor.getInt(trackCol) else 1
                    val trackNum = if (trackNumRaw > 1000) trackNumRaw % 1000 else trackNumRaw.coerceAtLeast(1)
                    val albumArtist = if (albumArtistCol >= 0) {
                        cursor.getString(albumArtistCol)?.takeIf { it.isNotBlank() } ?: artist
                    } else artist

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artUri = ContentUris.withAppendedId(albumArtBase, albumId)

                    songs.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            genre = genre,
                            folderName = folderName,
                            folderPath = folderPath,
                            durationMs = durationMs,
                            contentUri = contentUri,
                            albumArtUri = artUri,
                            format = format,
                            sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L,
                            dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L,
                            year = year,
                            trackNumber = trackNum,
                            albumArtist = albumArtist,
                            comment = "",
                            replayGainDb = 0f,
                            isImported = false
                        )
                    )
                }
            }
        }
        songs
    }

    suspend fun importUri(context: Context, uri: Uri): ImportedTrackEntity? = withContext(Dispatchers.IO) {
        runCatching {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            var displayName = "Audio Track"
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) {
                    displayName = c.getString(idx) ?: displayName
                }
            }

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: displayName.substringBeforeLast('.')
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: "Audio Importado"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
                ?: "Descargas Locales"
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.takeIf { it.isNotBlank() }
                ?: "Local Import"
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.take(4)?.toIntOrNull() ?: 2025
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val mime = context.contentResolver.getType(uri) ?: ""
            val ext = displayName.substringAfterLast('.', "").lowercase()
            retriever.release()

            val uniqueId = -(abs(uri.toString().hashCode().toLong()) + 10_000L)
            ImportedTrackEntity(
                id = uniqueId,
                uriString = uri.toString(),
                title = title,
                artist = artist,
                album = album,
                genre = genre,
                year = year,
                durationMs = durationMs,
                format = resolveFormat(ext, mime),
                folderName = "Importados"
            )
        }.getOrNull()
    }

    fun ImportedTrackEntity.toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = abs(id),
        genre = genre,
        folderName = folderName,
        folderPath = "/$folderName",
        durationMs = durationMs,
        contentUri = Uri.parse(uriString),
        albumArtUri = null,
        format = format,
        sizeBytes = 0L,
        dateAdded = addedAt / 1000L,
        year = year,
        trackNumber = 1,
        albumArtist = artist,
        comment = "",
        replayGainDb = 0f,
        isImported = true
    )

    private fun resolveFormat(ext: String, mime: String): String {
        return when {
            ext == "flac" || mime.contains("flac") -> "FLAC"
            ext == "wav" || mime.contains("wav") -> "WAV"
            ext == "aac" || mime.contains("aac") -> "AAC"
            ext == "ogg" || ext == "opus" || mime.contains("ogg") -> "OGG"
            ext == "m4a" || mime.contains("mp4") || mime.contains("m4a") -> "M4A"
            else -> "MP3"
        }
    }
}

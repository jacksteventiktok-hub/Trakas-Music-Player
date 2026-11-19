package com.example

import android.net.Uri
import com.example.data.local.PlayHistoryEntity
import com.example.model.AlbumSortOption
import com.example.model.LibraryGrouping
import com.example.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LibraryGroupingTest {

    private fun testSong(
        id: Long,
        title: String,
        artist: String,
        album: String,
        albumId: Long = 100L,
        year: Int = 2024,
        trackNumber: Int = 1,
        albumArtist: String = "",
        isImported: Boolean = false
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        genre = "Electronic",
        folderName = "Music",
        folderPath = "/Music",
        durationMs = 180_000L,
        contentUri = Uri.parse("content://media/external/audio/media/$id"),
        albumArtUri = null,
        format = "MP3",
        sizeBytes = 5_000_000L,
        dateAdded = 1000L,
        year = year,
        trackNumber = trackNumber,
        albumArtist = albumArtist,
        isImported = isImported
    )

    @Test
    fun `multi artist tracks do not duplicate albums`() {
        val songs = listOf(
            testSong(1, "Intro", "Nova", "Electro Nights", albumId = 10L, year = 2026, trackNumber = 1),
            testSong(2, "Pulse feat", "Nova feat. Echo", "Electro Nights", albumId = 10L, year = 2026, trackNumber = 2),
            testSong(3, "Bass collab", "Nova & Pulse", "Electro Nights", albumId = 10L, year = 2026, trackNumber = 3)
        )

        val result = LibraryGrouping.buildLibraryGroups(songs)

        assertEquals(1, result.soloAlbums.size)
        val album = result.soloAlbums.first()
        assertEquals("Electro Nights", album.name)
        assertEquals("Nova", album.artist)
        assertEquals(3, album.songs.size)

        val nova = result.artists.first { it.name == "Nova" }
        assertEquals(1, nova.albumCount)
        assertEquals(3, nova.songCount)

        val echo = result.artists.first { it.name == "Echo" }
        assertEquals(0, echo.albumCount)
        assertEquals(1, echo.songCount)
    }

    @Test
    fun `compilations are separated from solo artist albums`() {
        val songs = listOf(
            testSong(1, "Track 1", "Alpha", "Summer Hits", albumId = 20L, year = 2025, trackNumber = 1, albumArtist = "Various Artists"),
            testSong(2, "Track 2", "Beta", "Summer Hits", albumId = 20L, year = 2025, trackNumber = 2, albumArtist = "Various Artists"),
            testSong(3, "Solo Track", "Alpha", "Alpha LP", albumId = 21L, year = 2024, trackNumber = 1, albumArtist = "Alpha")
        )

        val result = LibraryGrouping.buildLibraryGroups(songs)

        assertEquals(1, result.compilations.size)
        assertTrue(result.compilations.first().isCompilation)
        assertEquals(1, result.soloAlbums.size)
        assertFalse(result.soloAlbums.first().isCompilation)

        val alpha = result.artists.first { it.name == "Alpha" }
        assertEquals(1, alpha.albumCount)
        assertEquals("Alpha LP", alpha.albums.first().name)
        assertEquals(2, alpha.songCount)
    }

    @Test
    fun `artist albums ordered newest to oldest and songs ordered track 1 to N`() {
        val songs = listOf(
            testSong(1, "Old T2", "Vortex", "First Tracks", albumId = 1L, year = 2022, trackNumber = 2),
            testSong(2, "Old T1", "Vortex", "First Tracks", albumId = 1L, year = 2022, trackNumber = 1),
            testSong(3, "New T2", "Vortex", "Electro Nights", albumId = 2L, year = 2026, trackNumber = 2),
            testSong(4, "New T1", "Vortex", "Electro Nights", albumId = 2L, year = 2026, trackNumber = 1)
        )
        val history = listOf(PlayHistoryEntity(songId = 1L, playCount = 12, lastPlayedAt = 100L))

        val result = LibraryGrouping.buildLibraryGroups(songs, history)
        val vortex = result.artists.first { it.name == "Vortex" }

        assertEquals(listOf("Electro Nights", "First Tracks"), vortex.albums.map { it.name })
        assertEquals(listOf("New T1", "New T2", "Old T1", "Old T2"), vortex.songsOrderedByYear.map { it.title })
        assertEquals("Old T2", vortex.topSongs.first().title)

        val oldestFirst = LibraryGrouping.sortAlbums(vortex.albums, AlbumSortOption.OLDEST)
        assertEquals(listOf("First Tracks", "Electro Nights"), oldestFirst.map { it.name })

        val az = LibraryGrouping.sortAlbums(vortex.albums, AlbumSortOption.AZ)
        assertEquals(listOf("Electro Nights", "First Tracks"), az.map { it.name })
    }
}

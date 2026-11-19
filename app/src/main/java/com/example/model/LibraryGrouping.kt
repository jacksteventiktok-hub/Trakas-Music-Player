package com.example.model

import android.net.Uri
import com.example.data.local.PlayHistoryEntity

data class ArtistCredits(
    val primaryArtist: String,
    val featuredArtists: List<String>,
    val allArtists: List<String>,
    val resolvedAlbumOwner: String,
    val isExplicitCompilation: Boolean
)

data class LibraryGroupsResult(
    val allAlbums: List<AlbumGroup>,
    val soloAlbums: List<AlbumGroup>,
    val compilations: List<AlbumGroup>,
    val artists: List<ArtistGroup>
)

object LibraryGrouping {

    private val compilationKeywords = setOf(
        "various artists",
        "varios artistas",
        "va",
        "v.a.",
        "v/a",
        "compilation",
        "compilación",
        "compilaciones",
        "soundtrack",
        "banda sonora",
        "ost",
        "original soundtrack"
    )

    private val featuredSplitRegex = Regex(
        pattern = """(?i)\s*(?:[\(\[]\s*)?(?:feat\.?|ft\.?|featuring|with|con)\s+([^\)\]]+)(?:[\)\]])?"""
    )

    private val collaboratorDelimiterRegex = Regex(
        pattern = """\s*(?:,|;|/|&|\bx\b|\bvs\.?\b)\s*""",
        option = RegexOption.IGNORE_CASE
    )

    fun isCompilationLabel(label: String): Boolean {
        val norm = label.trim().lowercase()
        return norm in compilationKeywords
    }

    fun parseArtistCredits(artistRaw: String, albumArtistRaw: String = ""): ArtistCredits {
        val cleanArtist = artistRaw.trim().ifBlank { "Artista desconocido" }
        val cleanAlbumArtist = albumArtistRaw.trim()
        val explicitCompilation = cleanAlbumArtist.isNotBlank() &&
            (isCompilationLabel(cleanAlbumArtist) || isCompilationLabel(cleanArtist))

        val featuredList = mutableListOf<String>()
        var mainPart = cleanArtist

        val featMatch = featuredSplitRegex.find(cleanArtist)
        if (featMatch != null) {
            mainPart = cleanArtist.substring(0, featMatch.range.first).trim()
            val afterFeat = featMatch.groupValues.getOrElse(1) { "" } +
                " " + cleanArtist.substring((featMatch.range.last + 1).coerceAtMost(cleanArtist.length))
            afterFeat.split(collaboratorDelimiterRegex)
                .map { it.trim().trim('(', ')', '[', ']') }
                .filter { it.isNotBlank() }
                .forEach { featuredList.add(it) }
        }

        val primaryTokens = mainPart.split(collaboratorDelimiterRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val primary = primaryTokens.firstOrNull() ?: cleanArtist
        if (primaryTokens.size > 1) {
            primaryTokens.drop(1).forEach { featuredList.add(it) }
        }

        val uniqueFeatured = featuredList
            .filter { !it.equals(primary, ignoreCase = true) }
            .distinctBy { it.lowercase() }

        val all = (listOf(primary) + uniqueFeatured).distinctBy { it.lowercase() }

        val resolvedOwner = when {
            explicitCompilation -> "Varios Artistas"
            cleanAlbumArtist.isNotBlank() &&
                !cleanAlbumArtist.equals(cleanArtist, ignoreCase = true) -> {
                // Explicit ALBUM_ARTIST tag present and distinct from track artist
                val aaTokens = cleanAlbumArtist
                    .replace(featuredSplitRegex, "")
                    .split(collaboratorDelimiterRegex)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                aaTokens.firstOrNull() ?: primary
            }
            else -> primary
        }

        return ArtistCredits(
            primaryArtist = primary,
            featuredArtists = uniqueFeatured,
            allArtists = all,
            resolvedAlbumOwner = resolvedOwner,
            isExplicitCompilation = explicitCompilation
        )
    }

    fun sortAlbums(albums: List<AlbumGroup>, option: AlbumSortOption): List<AlbumGroup> {
        return when (option) {
            AlbumSortOption.NEWEST -> albums.sortedWith(
                compareByDescending<AlbumGroup> { if (it.year in 1900..2100) it.year else -1 }
                    .thenBy { it.name.lowercase() }
            )
            AlbumSortOption.OLDEST -> albums.sortedWith(
                compareBy<AlbumGroup> { if (it.year in 1900..2100) it.year else Int.MAX_VALUE }
                    .thenBy { it.name.lowercase() }
            )
            AlbumSortOption.AZ -> albums.sortedBy { it.name.lowercase() }
            AlbumSortOption.ZA -> albums.sortedByDescending { it.name.lowercase() }
        }
    }

    fun buildLibraryGroups(
        visibleSongs: List<Song>,
        playHistory: List<PlayHistoryEntity> = emptyList()
    ): LibraryGroupsResult {
        if (visibleSongs.isEmpty()) {
            return LibraryGroupsResult(
                allAlbums = emptyList(),
                soloAlbums = emptyList(),
                compilations = emptyList(),
                artists = emptyList()
            )
        }

        val playCountMap = playHistory.associate { it.songId to it.playCount }
        val songCredits = visibleSongs.associate { it.id to parseArtistCredits(it.artist, it.albumArtist) }

        // 1. Cluster songs into raw album buckets.
        // MediaStore songs share albumId per album; imported tracks share normalized album name + owner (or album name for multi-artist albums).
        val rawBuckets = visibleSongs.groupBy { song ->
            val albumKey = song.album.trim().lowercase().ifBlank { "álbum desconocido" }
            if (!song.isImported && song.albumId > 0L) {
                "ms_${song.albumId}_$albumKey"
            } else {
                "imp_$albumKey"
            }
        }

        // 2. Build AlbumGroup items, detecting compilations & avoiding duplicate albums across multi-artist tracks
        val mergedAlbumMap = linkedMapOf<String, Pair<MutableList<Song>, Boolean>>()
        val bucketDisplayOwner = mutableMapOf<String, String>()

        rawBuckets.forEach { (_, clusterSongs) ->
            val creditsList = clusterSongs.mapNotNull { songCredits[it.id] }
            val hasExplicitComp = creditsList.any { it.isExplicitCompilation }
            val distinctOwners = creditsList.map { it.resolvedAlbumOwner.trim() }
                .filter { it.isNotBlank() && !isCompilationLabel(it) }
                .distinctBy { it.lowercase() }

            // Compilation if explicitly tagged Various Artists / Soundtrack OR >= 3 distinct primary owners share this album
            val isCompilation = hasExplicitComp || distinctOwners.size >= 3

            val albumDisplayName = clusterSongs.first().album.trim().ifBlank { "Álbum desconocido" }
            val ownerName = if (isCompilation) {
                "Varios Artistas"
            } else {
                // Majority owner if multiple tracks, keeping first seen capitalization
                creditsList
                    .groupingBy { it.resolvedAlbumOwner.lowercase() }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key
                    ?.let { keyLower ->
                        creditsList.firstOrNull { it.resolvedAlbumOwner.equals(keyLower, ignoreCase = true) }?.resolvedAlbumOwner
                    } ?: creditsList.firstOrNull()?.resolvedAlbumOwner ?: "Artista desconocido"
            }

            val mergeKey = if (isCompilation) {
                "comp_${albumDisplayName.lowercase()}"
            } else {
                "solo_${albumDisplayName.lowercase()}_${ownerName.lowercase()}"
            }

            val existing = mergedAlbumMap[mergeKey]
            if (existing == null) {
                mergedAlbumMap[mergeKey] = clusterSongs.toMutableList() to isCompilation
                bucketDisplayOwner[mergeKey] = ownerName
            } else {
                existing.first.addAll(clusterSongs)
            }
        }

        val allAlbums = mergedAlbumMap.entries.map { (mergeKey, pair) ->
            val (rawSongs, isCompilation) = pair
            val orderedSongs = rawSongs
                .distinctBy { it.id }
                .sortedWith(
                    compareBy<Song> { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                        .thenBy { it.title.lowercase() }
                )
            val validYear = orderedSongs
                .map { it.year }
                .filter { it in 1900..2100 }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: 0
            val totalDur = orderedSongs.sumOf { it.durationMs.coerceAtLeast(0L) }
            val artUri: Uri? = orderedSongs.firstNotNullOfOrNull { it.albumArtUri }
            val rep = orderedSongs.first()

            AlbumGroup(
                id = rep.albumId.takeIf { it > 0L } ?: rep.id,
                name = rep.album.trim().ifBlank { "Álbum desconocido" },
                artist = bucketDisplayOwner[mergeKey] ?: rep.artist,
                year = validYear,
                totalDurationMs = totalDur,
                isCompilation = isCompilation,
                artUri = artUri,
                songs = orderedSongs
            )
        }.let { sortAlbums(it, AlbumSortOption.NEWEST) }

        val soloAlbums = allAlbums.filterNot { it.isCompilation }
        val compilations = allAlbums.filter { it.isCompilation }

        // 3. Build ArtistGroup map using canonical artist names (including featured artists without duplicating albums)
        val canonicalArtistNames = linkedMapOf<String, String>()
        val artistSongBuckets = linkedMapOf<String, MutableList<Song>>()

        visibleSongs.forEach { song ->
            val credits = songCredits[song.id] ?: return@forEach
            credits.allArtists.forEach { artistName ->
                val norm = artistName.trim().lowercase()
                if (norm.isNotBlank() && !isCompilationLabel(norm)) {
                    canonicalArtistNames.putIfAbsent(norm, artistName.trim())
                    artistSongBuckets.getOrPut(norm) { mutableListOf() }.add(song)
                }
            }
        }

        val soloAlbumsByOwner = soloAlbums.groupBy { it.artist.trim().lowercase() }

        val artists = canonicalArtistNames.entries.map { (normKey, displayName) ->
            val rawSongs = artistSongBuckets[normKey].orEmpty().distinctBy { it.id }
            val ownedAlbums = sortAlbums(soloAlbumsByOwner[normKey].orEmpty(), AlbumSortOption.NEWEST)

            val albumSongIds = ownedAlbums.flatMap { it.songs }.map { it.id }.toSet()
            val orphanSongs = rawSongs
                .filterNot { it.id in albumSongIds }
                .sortedWith(
                    compareByDescending<Song> { if (it.year in 1900..2100) it.year else -1 }
                        .thenBy { it.trackNumber }
                        .thenBy { it.title.lowercase() }
                )

            // Order: newest album -> oldest album, track 1 -> last track, then remaining collaborations
            val orderedByYear = (ownedAlbums.flatMap { it.songs } + orphanSongs).distinctBy { it.id }

            // Top / most played songs first, falling back to newest & track number
            val topSongs = rawSongs.sortedWith(
                compareByDescending<Song> { playCountMap[it.id] ?: 0 }
                    .thenByDescending { if (it.year in 1900..2100) it.year else -1 }
                    .thenBy { it.trackNumber }
                    .thenBy { it.title.lowercase() }
            )

            val artistArt = ownedAlbums.firstNotNullOfOrNull { it.artUri }
                ?: orderedByYear.firstNotNullOfOrNull { it.albumArtUri }

            ArtistGroup(
                name = displayName,
                songCount = rawSongs.size,
                albumCount = ownedAlbums.size,
                artUri = artistArt,
                albums = ownedAlbums,
                songs = rawSongs,
                songsOrderedByYear = orderedByYear,
                topSongs = topSongs
            )
        }.sortedBy { it.name.lowercase() }

        return LibraryGroupsResult(
            allAlbums = allAlbums,
            soloAlbums = soloAlbums,
            compilations = compilations,
            artists = artists
        )
    }
}

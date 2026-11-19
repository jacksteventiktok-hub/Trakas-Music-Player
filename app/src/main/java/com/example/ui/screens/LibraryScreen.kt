package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AlbumGroup
import com.example.model.AlbumsFilterOption
import com.example.model.ArtistGroup
import com.example.model.LibraryNavigationTarget
import com.example.model.Song
import com.example.player.PlaybackUiState
import com.example.ui.components.ArtworkImage
import com.example.ui.components.TrakasIconType
import com.example.ui.components.TrakasStyledIcon
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import com.example.ui.theme.contrastText
import com.example.viewmodel.LibraryUiState

private enum class LibraryTab {
    SONGS, ALBUMS, ARTISTS, GENRES, FOLDERS, FAVORITES, RECENT, MOST_PLAYED, IMPORTED
}

private enum class DurationFilter(val label: String) {
    ALL("Duración"),
    SHORT("< 3 min"),
    MEDIUM("3 - 5 min"),
    LONG("> 5 min")
}

private enum class YearFilter(val label: String) {
    ALL("Año"),
    NEWEST("2025+"),
    RECENT("2020-2024"),
    CLASSIC("< 2020")
}

private sealed interface LibraryDetailPage {
    data class ArtistPage(val artistName: String) : LibraryDetailPage
    data class AlbumPage(val albumId: Long, val albumName: String) : LibraryDetailPage
    data class CollectionPage(val title: String, val songs: List<Song>) : LibraryDetailPage
}

@Composable
fun LibraryScreen(
    libraryState: LibraryUiState,
    playbackState: PlaybackUiState,
    searchQuery: String,
    pendingTarget: LibraryNavigationTarget? = null,
    onTargetConsumed: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayOrdered: (List<Song>, Int) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onToggleAlbumFavorite: (AlbumGroup) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onPlaySongsNext: (List<Song>) -> Unit,
    onAddSongsToQueue: (List<Song>) -> Unit,
    onAddAlbumToPlaylist: (Long, List<Song>) -> Unit,
    onCreatePlaylistFromAlbum: (String, List<Song>) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onRescanLibrary: () -> Unit,
    onImportAudio: () -> Unit,
    onCreateDemoTracks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableStateOf(LibraryTab.SONGS) }
    var albumsFilter by remember { mutableStateOf(AlbumsFilterOption.ALL) }
    var detailStack by remember { mutableStateOf<List<LibraryDetailPage>>(emptyList()) }

    fun pushDetail(page: LibraryDetailPage) {
        val current = detailStack.lastOrNull()
        if (current != page) {
            detailStack = detailStack + page
        }
    }

    fun popDetail() {
        if (detailStack.isNotEmpty()) {
            detailStack = detailStack.dropLast(1)
        }
    }

    LaunchedEffect(pendingTarget, libraryState.visibleSongs) {
        when (pendingTarget) {
            is LibraryNavigationTarget.Artist -> {
                selectedTab = LibraryTab.ARTISTS
                detailStack = listOf(LibraryDetailPage.ArtistPage(pendingTarget.name))
                onTargetConsumed()
            }
            is LibraryNavigationTarget.Album -> {
                selectedTab = LibraryTab.ALBUMS
                val matched = libraryState.albums.find {
                    (pendingTarget.albumId != null && it.id == pendingTarget.albumId) ||
                        it.name.equals(pendingTarget.name, ignoreCase = true)
                }
                detailStack = listOf(
                    LibraryDetailPage.AlbumPage(
                        albumId = matched?.id ?: pendingTarget.albumId ?: -1L,
                        albumName = matched?.name ?: pendingTarget.name
                    )
                )
                onTargetConsumed()
            }
            null -> Unit
        }
    }

    BackHandler(enabled = detailStack.isNotEmpty()) {
        popDetail()
    }

    val topDetail = detailStack.lastOrNull()
    when (topDetail) {
        is LibraryDetailPage.ArtistPage -> {
            val artistGroup = remember(libraryState.artists, libraryState.visibleSongs, topDetail.artistName) {
                libraryState.artists.find { it.name.equals(topDetail.artistName, ignoreCase = true) }
                    ?: ArtistGroup(
                        name = topDetail.artistName,
                        songCount = 0,
                        albumCount = 0,
                        artUri = null,
                        albums = emptyList(),
                        songs = emptyList(),
                        songsOrderedByYear = emptyList(),
                        topSongs = emptyList()
                    )
            }
            ArtistDetailScreen(
                artist = artistGroup,
                playbackState = playbackState,
                favoriteIds = libraryState.favoriteIds,
                onBack = ::popDetail,
                onPlayArtistOrdered = { songs -> onPlayOrdered(songs, 0) },
                onShuffleArtist = onShufflePlay,
                onOpenAlbum = { album -> pushDetail(LibraryDetailPage.AlbumPage(album.id, album.name)) },
                onPlaySong = onPlaySong,
                onToggleFavorite = onToggleFavorite,
                onAddToQueue = onAddToQueue,
                onSongMoreOptions = onSongLongClick,
                modifier = modifier
            )
            return
        }

        is LibraryDetailPage.AlbumPage -> {
            val albumGroup = remember(libraryState.albums, topDetail) {
                libraryState.albums.find {
                    it.id == topDetail.albumId && it.name.equals(topDetail.albumName, ignoreCase = true)
                } ?: libraryState.albums.find {
                    it.name.equals(topDetail.albumName, ignoreCase = true)
                }
            }
            if (albumGroup != null) {
                AlbumDetailScreen(
                    album = albumGroup,
                    playbackState = playbackState,
                    favoriteIds = libraryState.favoriteIds,
                    playlists = libraryState.playlists,
                    onBack = ::popDetail,
                    onArtistClick = { artistName ->
                        val prev = detailStack.dropLast(1).lastOrNull()
                        if (prev is LibraryDetailPage.ArtistPage && prev.artistName.equals(artistName, ignoreCase = true)) {
                            popDetail()
                        } else {
                            pushDetail(LibraryDetailPage.ArtistPage(artistName))
                        }
                    },
                    onPlayAlbumOrdered = onPlayOrdered,
                    onShuffleAlbum = onShufflePlay,
                    onToggleAlbumFavorite = onToggleAlbumFavorite,
                    onPlaySongsNext = onPlaySongsNext,
                    onAddSongsToQueue = onAddSongsToQueue,
                    onAddAlbumToPlaylist = onAddAlbumToPlaylist,
                    onCreatePlaylistFromAlbum = onCreatePlaylistFromAlbum,
                    onSongMoreOptions = onSongLongClick,
                    modifier = modifier
                )
                return
            } else {
                LaunchedEffect(topDetail) { popDetail() }
            }
        }

        is LibraryDetailPage.CollectionPage -> Unit
        null -> Unit
    }

    var showFilters by remember { mutableStateOf(false) }
    var formatFilter by remember { mutableStateOf<String?>(null) }
    var durationFilter by remember { mutableStateOf(DurationFilter.ALL) }
    var yearFilter by remember { mutableStateOf(YearFilter.ALL) }
    var onlyFavoritesFilter by remember { mutableStateOf(false) }

    val hasActiveFilter = searchQuery.isNotBlank() ||
        formatFilter != null ||
        durationFilter != DurationFilter.ALL ||
        yearFilter != YearFilter.ALL ||
        onlyFavoritesFilter

    val filteredSongs = remember(
        libraryState.visibleSongs,
        libraryState.favoriteIds,
        searchQuery,
        formatFilter,
        durationFilter,
        yearFilter,
        onlyFavoritesFilter
    ) {
        val q = searchQuery.trim().lowercase()
        libraryState.visibleSongs.filter { song ->
            val matchesQuery = q.isEmpty() ||
                song.title.lowercase().contains(q) ||
                song.artist.lowercase().contains(q) ||
                song.album.lowercase().contains(q) ||
                song.genre.lowercase().contains(q) ||
                song.folderName.lowercase().contains(q)

            val matchesFormat = formatFilter == null || song.format.equals(formatFilter, ignoreCase = true)
            val matchesDuration = when (durationFilter) {
                DurationFilter.ALL -> true
                DurationFilter.SHORT -> song.durationMs < 180_000L
                DurationFilter.MEDIUM -> song.durationMs in 180_000L..300_000L
                DurationFilter.LONG -> song.durationMs > 300_000L
            }
            val matchesYear = when (yearFilter) {
                YearFilter.ALL -> true
                YearFilter.NEWEST -> song.year >= 2025
                YearFilter.RECENT -> song.year in 2020..2024
                YearFilter.CLASSIC -> song.year < 2020
            }
            val matchesFav = !onlyFavoritesFilter || song.id in libraryState.favoriteIds

            matchesQuery && matchesFormat && matchesDuration && matchesYear && matchesFav
        }
    }

    val matchingArtists = remember(libraryState.artists, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else libraryState.artists.filter { it.name.lowercase().contains(q) }
    }

    val matchingAlbums = remember(libraryState.albums, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else libraryState.albums.filter {
            it.name.lowercase().contains(q) || it.artist.lowercase().contains(q)
        }
    }

    val matchingPlaylists = remember(libraryState.playlists, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else libraryState.playlists.filter { it.name.lowercase().contains(q) }
    }

    val collectionPage = topDetail as? LibraryDetailPage.CollectionPage

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = collectionPage?.title ?: strings.navLibrary,
                style = MaterialTheme.typography.headlineMedium,
                color = tokens.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (collectionPage != null) {
                    IconButton(onClick = ::popDetail) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tokens.primary)
                    }
                }
                IconButton(
                    onClick = { showFilters = !showFilters },
                    modifier = Modifier.testTag("library_filter_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Filtros",
                        tint = if (showFilters || hasActiveFilter) tokens.primary else tokens.mutedTextColor
                    )
                }
                IconButton(
                    onClick = onImportAudio,
                    modifier = Modifier.testTag("library_import_btn")
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = strings.importAudio, tint = tokens.primary)
                }
                IconButton(
                    onClick = onRescanLibrary,
                    modifier = Modifier.testTag("library_rescan_btn")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = strings.scanLibrary, tint = tokens.secondary)
                }
            }
        }

        // Global Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            singleLine = true,
            placeholder = { Text(strings.searchHint, color = tokens.mutedTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = tokens.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = tokens.mutedTextColor)
                    }
                }
            },
            shape = RoundedCornerShape(tokens.buttonCornerDp.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = tokens.primary,
                unfocusedBorderColor = tokens.textColor.copy(alpha = 0.2f),
                focusedTextColor = tokens.textColor,
                unfocusedTextColor = tokens.textColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("library_search_input")
        )

        if (showFilters || hasActiveFilter) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                item {
                    FilterChip(
                        selected = onlyFavoritesFilter,
                        onClick = { onlyFavoritesFilter = !onlyFavoritesFilter },
                        label = { Text(strings.favorites) }
                    )
                }
                item {
                    val formats = listOf(null, "MP3", "FLAC", "WAV", "AAC", "OGG", "M4A")
                    val nextFormat = formats[(formats.indexOf(formatFilter) + 1) % formats.size]
                    FilterChip(
                        selected = formatFilter != null,
                        onClick = { formatFilter = nextFormat },
                        label = { Text(formatFilter ?: "Formato") }
                    )
                }
                item {
                    val values = DurationFilter.entries
                    val nextDur = values[(values.indexOf(durationFilter) + 1) % values.size]
                    FilterChip(
                        selected = durationFilter != DurationFilter.ALL,
                        onClick = { durationFilter = nextDur },
                        label = { Text(durationFilter.label) }
                    )
                }
                item {
                    val values = YearFilter.entries
                    val nextYr = values[(values.indexOf(yearFilter) + 1) % values.size]
                    FilterChip(
                        selected = yearFilter != YearFilter.ALL,
                        onClick = { yearFilter = nextYr },
                        label = { Text(yearFilter.label) }
                    )
                }
            }
        }

        if (collectionPage == null && !hasActiveFilter) {
            val tabItems = listOf(
                Triple(LibraryTab.SONGS, strings.songs, TrakasIconType.SONGS),
                Triple(LibraryTab.ALBUMS, strings.albums, TrakasIconType.ALBUMS),
                Triple(LibraryTab.ARTISTS, strings.artists, TrakasIconType.ARTISTS),
                Triple(LibraryTab.GENRES, strings.genres, TrakasIconType.LIBRARY),
                Triple(LibraryTab.FOLDERS, strings.folders, TrakasIconType.LIBRARY),
                Triple(LibraryTab.FAVORITES, strings.favorites, TrakasIconType.FAVORITES),
                Triple(LibraryTab.RECENT, strings.recentlyPlayed, TrakasIconType.HISTORY),
                Triple(LibraryTab.MOST_PLAYED, strings.mostPlayed, TrakasIconType.HISTORY),
                Triple(LibraryTab.IMPORTED, strings.imported, TrakasIconType.DOWNLOADS)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(tabItems, key = { it.first.name }) { (tab, label, iconType) ->
                    val isSelected = selectedTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        label = { Text(label) },
                        leadingIcon = {
                            TrakasStyledIcon(
                                type = iconType,
                                contentDescription = null,
                                active = isSelected,
                                size = 16.dp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tokens.primary.copy(alpha = 0.22f),
                            selectedLabelColor = tokens.primary,
                            containerColor = tokens.surfaceCard,
                            labelColor = tokens.mutedTextColor
                        ),
                        modifier = Modifier.testTag("library_tab_${tab.name}")
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        if (collectionPage != null) {
            SongListContent(
                songs = collectionPage.songs,
                playbackState = playbackState,
                favoriteIds = libraryState.favoriteIds,
                onPlaySong = onPlaySong,
                onLongClick = onSongLongClick,
                onToggleFavorite = onToggleFavorite
            )
        } else if (hasActiveFilter) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Highlighted Artist Search Results
                if (matchingArtists.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.artists,
                            style = MaterialTheme.typography.titleSmall,
                            color = tokens.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(matchingArtists, key = { "search_art_${it.name}" }) { artist ->
                        SearchArtistHighlightCard(
                            artist = artist,
                            onClick = { pushDetail(LibraryDetailPage.ArtistPage(artist.name)) }
                        )
                    }
                }

                // Matching Albums
                if (matchingAlbums.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.albums,
                            style = MaterialTheme.typography.titleSmall,
                            color = tokens.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(matchingAlbums, key = { "search_alb_${it.id}_${it.name}" }) { album ->
                        SearchAlbumHighlightCard(
                            album = album,
                            onClick = { pushDetail(LibraryDetailPage.AlbumPage(album.id, album.name)) }
                        )
                    }
                }

                if (matchingPlaylists.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.playlists,
                            style = MaterialTheme.typography.titleSmall,
                            color = tokens.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(matchingPlaylists, key = { "search_pl_${it.id}" }) { pl ->
                        GroupCardRow(
                            title = pl.name,
                            subtitle = "${pl.songs.size} ${strings.songs.lowercase()}",
                            iconType = TrakasIconType.PLAYLISTS,
                            onClick = { pushDetail(LibraryDetailPage.CollectionPage(pl.name, pl.songs)) }
                        )
                    }
                }

                if (filteredSongs.isEmpty() && matchingArtists.isEmpty() && matchingAlbums.isEmpty() && matchingPlaylists.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Sin resultados para esta búsqueda o filtro", color = tokens.mutedTextColor)
                        }
                    }
                } else if (filteredSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.songs,
                            style = MaterialTheme.typography.titleSmall,
                            color = tokens.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(filteredSongs, key = { "flt_${it.id}" }) { song ->
                        SongRow(
                            song = song,
                            isPlayingCurrent = playbackState.currentSong?.id == song.id,
                            isFavorite = song.id in libraryState.favoriteIds,
                            onClick = { onPlaySong(song, filteredSongs) },
                            onLongClick = { onSongLongClick(song) },
                            onToggleFavorite = { onToggleFavorite(song.id) }
                        )
                    }
                }
            }
        } else {
            when (selectedTab) {
                LibraryTab.SONGS -> {
                    if (libraryState.visibleSongs.isEmpty()) {
                        EmptyLibraryBox(onImportAudio, onCreateDemoTracks, onRescanLibrary)
                    } else {
                        SongListContent(
                            songs = libraryState.visibleSongs,
                            playbackState = playbackState,
                            favoriteIds = libraryState.favoriteIds,
                            onPlaySong = onPlaySong,
                            onLongClick = onSongLongClick,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }

                LibraryTab.ALBUMS -> {
                    val displayedAlbums = when (albumsFilter) {
                        AlbumsFilterOption.ALL -> libraryState.albums
                        AlbumsFilterOption.ALBUMS -> libraryState.soloAlbums
                        AlbumsFilterOption.COMPILATIONS -> libraryState.compilations
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (libraryState.compilations.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 10.dp)
                            ) {
                                items(AlbumsFilterOption.entries, key = { it.name }) { opt ->
                                    val selected = albumsFilter == opt
                                    FilterChip(
                                        selected = selected,
                                        onClick = { albumsFilter = opt },
                                        label = {
                                            val count = when (opt) {
                                                AlbumsFilterOption.ALL -> libraryState.albums.size
                                                AlbumsFilterOption.ALBUMS -> libraryState.soloAlbums.size
                                                AlbumsFilterOption.COMPILATIONS -> libraryState.compilations.size
                                            }
                                            Text("${opt.labelEs} ($count)")
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = tokens.primary.copy(alpha = 0.22f),
                                            selectedLabelColor = tokens.primary,
                                            containerColor = tokens.surfaceCard,
                                            labelColor = tokens.mutedTextColor
                                        )
                                    )
                                }
                            }
                        }

                        if (displayedAlbums.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.Album, contentDescription = null, tint = tokens.mutedTextColor, modifier = Modifier.size(44.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Sin álbumes disponibles", color = tokens.mutedTextColor)
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 152.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 96.dp)
                            ) {
                                items(displayedAlbums, key = { "alb_${it.id}_${it.name}_${it.artist}" }) { album ->
                                    val meta = buildString {
                                        append(album.artist)
                                        if (album.year in 1900..2100) {
                                            append(" • ")
                                            append(album.year)
                                        }
                                        append(" • ")
                                        append(album.songs.size)
                                    }
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(tokens.surfaceCard)
                                            .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                            .clickable { pushDetail(LibraryDetailPage.AlbumPage(album.id, album.name)) }
                                            .padding(10.dp)
                                            .testTag("library_album_card_${album.id}")
                                    ) {
                                        ArtworkImage(
                                            artUri = album.artUri,
                                            contentDescription = album.name,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(128.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = album.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = tokens.textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = meta,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = tokens.mutedTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.ARTISTS -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(libraryState.artists, key = { it.name.lowercase() }) { artist ->
                            val albumWord = if (artist.albumCount == 1) "álbum" else "álbumes"
                            val songWord = if (artist.songCount == 1) "canción" else "canciones"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(tokens.surfaceCard)
                                    .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .clickable { pushDetail(LibraryDetailPage.ArtistPage(artist.name)) }
                                    .padding(12.dp)
                                    .testTag("library_artist_row_${artist.name}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArtistAvatar(
                                    artistName = artist.name,
                                    artUri = artist.artUri,
                                    size = 54.dp
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${artist.albumCount} $albumWord • ${artist.songCount} $songWord",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.mutedTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryTab.GENRES -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(libraryState.genres, key = { it.name }) { genre ->
                            GroupCardRow(
                                title = genre.name,
                                subtitle = "${genre.songs.size} ${strings.songs.lowercase()}",
                                iconType = TrakasIconType.SONGS,
                                onClick = { pushDetail(LibraryDetailPage.CollectionPage(genre.name, genre.songs)) }
                            )
                        }
                    }
                }

                LibraryTab.FOLDERS -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(libraryState.folders, key = { it.path }) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(tokens.surfaceCard)
                                    .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                    .clickable { pushDetail(LibraryDetailPage.CollectionPage(folder.name, folder.songs)) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Folder, contentDescription = null, tint = tokens.primary, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(folder.name, style = MaterialTheme.typography.titleSmall, color = tokens.textColor)
                                    Text("${folder.songs.size} pistas • ${folder.path}", style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                LibraryTab.FAVORITES -> SongListContent(
                    songs = libraryState.favoriteSongs,
                    playbackState = playbackState,
                    favoriteIds = libraryState.favoriteIds,
                    onPlaySong = onPlaySong,
                    onLongClick = onSongLongClick,
                    onToggleFavorite = onToggleFavorite
                )

                LibraryTab.RECENT -> SongListContent(
                    songs = libraryState.recentlyPlayed,
                    playbackState = playbackState,
                    favoriteIds = libraryState.favoriteIds,
                    onPlaySong = onPlaySong,
                    onLongClick = onSongLongClick,
                    onToggleFavorite = onToggleFavorite
                )

                LibraryTab.MOST_PLAYED -> SongListContent(
                    songs = libraryState.mostPlayed,
                    playbackState = playbackState,
                    favoriteIds = libraryState.favoriteIds,
                    onPlaySong = onPlaySong,
                    onLongClick = onSongLongClick,
                    onToggleFavorite = onToggleFavorite
                )

                LibraryTab.IMPORTED -> {
                    if (libraryState.importedSongs.isEmpty()) {
                        EmptyLibraryBox(onImportAudio, onCreateDemoTracks, onRescanLibrary)
                    } else {
                        SongListContent(
                            songs = libraryState.importedSongs,
                            playbackState = playbackState,
                            favoriteIds = libraryState.favoriteIds,
                            onPlaySong = onPlaySong,
                            onLongClick = onSongLongClick,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchArtistHighlightCard(
    artist: ArtistGroup,
    onClick: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.primary.copy(alpha = 0.12f))
            .border(1.dp, tokens.primary.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("search_artist_card_${artist.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtistAvatar(
            artistName = artist.name,
            artUri = artist.artUri,
            size = 56.dp
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                color = tokens.textColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tokens.primary.copy(alpha = 0.22f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = strings.artistLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "${artist.albumCount} ${strings.albums.lowercase()} • ${artist.songCount} ${strings.songs.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.mutedTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchAlbumHighlightCard(
    album: AlbumGroup,
    onClick: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.surfaceCard)
            .border(1.dp, tokens.textColor.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("search_album_card_${album.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(
            artUri = album.artUri,
            contentDescription = album.name,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleSmall,
                color = tokens.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val yearPart = if (album.year in 1900..2100) " • ${album.year}" else ""
            Text(
                text = "${strings.albumLabel} • ${album.artist}$yearPart",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.mutedTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SongListContent(
    songs: List<Song>,
    playbackState: PlaybackUiState,
    favoriteIds: Set<Long>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onLongClick: (Song) -> Unit,
    onToggleFavorite: (Long) -> Unit
) {
    val tokens = LocalTrakasTokens.current
    if (songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = tokens.mutedTextColor, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("Lista vacía", color = tokens.mutedTextColor)
            }
        }
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(songs, key = { "lib_${it.id}" }) { song ->
            SongRow(
                song = song,
                isPlayingCurrent = playbackState.currentSong?.id == song.id,
                isFavorite = song.id in favoriteIds,
                onClick = { onPlaySong(song, songs) },
                onLongClick = { onLongClick(song) },
                onToggleFavorite = { onToggleFavorite(song.id) }
            )
        }
    }
}

@Composable
private fun GroupCardRow(
    title: String,
    subtitle: String,
    iconType: TrakasIconType,
    onClick: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.surfaceCard)
            .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrakasStyledIcon(
            type = iconType,
            contentDescription = title,
            active = true,
            showBadgeContainer = true,
            size = 22.dp
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = tokens.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyLibraryBox(
    onImportAudio: () -> Unit,
    onCreateDemoTracks: () -> Unit,
    onRescanLibrary: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.surfaceCard)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(strings.noMusicTitle, style = MaterialTheme.typography.titleMedium, color = tokens.textColor)
        Text(strings.noMusicSubtitle, style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
        Button(
            onClick = onCreateDemoTracks,
            colors = ButtonDefaults.buttonColors(
                containerColor = tokens.buttonColor,
                contentColor = tokens.buttonColor.contrastText()
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.createDemoTracks)
        }
        OutlinedButton(onClick = onImportAudio, modifier = Modifier.fillMaxWidth()) {
            Text(strings.importAudio)
        }
        OutlinedButton(onClick = onRescanLibrary, modifier = Modifier.fillMaxWidth()) {
            Text(strings.scanLibrary)
        }
    }
}

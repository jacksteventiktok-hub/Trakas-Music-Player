package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.AlbumGroup
import com.example.model.AlbumSortOption
import com.example.model.ArtistGroup
import com.example.model.LibraryGrouping
import com.example.model.Song
import com.example.player.PlaybackUiState
import com.example.ui.components.ArtworkImage
import com.example.ui.components.formatDuration
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import com.example.ui.theme.contrastText
import kotlin.math.abs

@Composable
fun ArtistAvatar(
    artistName: String,
    artUri: Uri?,
    size: Dp,
    modifier: Modifier = Modifier,
    isCircular: Boolean = true
) {
    val context = LocalContext.current
    val tokens = LocalTrakasTokens.current
    val shape = if (isCircular) CircleShape else RoundedCornerShape(18.dp)

    if (artUri == null) {
        GeneratedArtistArtwork(
            artistName = artistName,
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(artUri)
                .size(384)
                .crossfade(tokens.animationsEnabled)
                .build(),
            contentDescription = artistName,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape),
            loading = {
                GeneratedArtistArtwork(
                    artistName = artistName,
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                GeneratedArtistArtwork(
                    artistName = artistName,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}

@Composable
private fun GeneratedArtistArtwork(
    artistName: String,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTrakasTokens.current
    val initials = remember(artistName) { extractArtistInitials(artistName) }
    val hash = remember(artistName) { abs(artistName.lowercase().hashCode()) }
    val palettePairs = remember(tokens.primary, tokens.secondary) {
        listOf(
            tokens.primary to tokens.secondary,
            Color(0xFF00B0FF) to Color(0xFF7C4DFF),
            Color(0xFFFF4081) to Color(0xFF7C4DFF),
            Color(0xFF00E676) to Color(0xFF00B0FF),
            Color(0xFFFF6D00) to Color(0xFFFF1744),
            Color(0xFF651FFF) to Color(0xFF00E5FF)
        )
    }
    val (c1, c2) = palettePairs[hash % palettePairs.size]

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        c1.copy(alpha = 0.85f),
                        c2.copy(alpha = 0.90f),
                        tokens.surfaceElevated
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = w * 0.42f,
                center = Offset(w * 0.25f, h * 0.25f),
                style = Stroke(width = w * 0.03f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = w * 0.36f,
                center = Offset(w * 0.80f, h * 0.78f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.20f),
                radius = w * 0.46f,
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(width = w * 0.02f)
            )
        }
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

private fun extractArtistInitials(name: String): String {
    val words = name.trim()
        .split(Regex("""\s+"""))
        .filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "TR"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "${words[0].first()}${words[1].first()}".uppercase()
    }
}

@Composable
fun ArtistDetailScreen(
    artist: ArtistGroup,
    playbackState: PlaybackUiState,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onPlayArtistOrdered: (List<Song>) -> Unit,
    onShuffleArtist: (List<Song>) -> Unit,
    onOpenAlbum: (AlbumGroup) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onSongMoreOptions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    var sortOption by remember(artist.name) { mutableStateOf(AlbumSortOption.NEWEST) }
    var showAllSongs by remember(artist.name) { mutableStateOf(false) }

    val sortedAlbums = remember(artist.albums, sortOption) {
        LibraryGrouping.sortAlbums(artist.albums, sortOption)
    }

    val groupByYear = sortOption == AlbumSortOption.NEWEST || sortOption == AlbumSortOption.OLDEST
    val yearBuckets = remember(sortedAlbums, groupByYear) {
        if (!groupByYear) {
            listOf("" to sortedAlbums)
        } else {
            sortedAlbums
                .groupBy { if (it.year in 1900..2100) it.year.toString() else "" }
                .entries
                .map { it.key to it.value }
        }
    }

    val displayedSongs = remember(artist.topSongs, showAllSongs) {
        if (showAllSongs || artist.topSongs.size <= 10) artist.topSongs
        else artist.topSongs.take(10)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("artist_detail_screen")
    ) {
        val isWide = maxWidth >= 600.dp
        val columns = when {
            maxWidth >= 900.dp -> 3
            isWide -> 2
            else -> 1
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top App Bar Row
            item(key = "artist_topbar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("artist_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Atrás",
                            tint = tokens.primary
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = strings.artistLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Artist Hero Header
            item(key = "artist_hero") {
                val albumLabel = if (artist.albumCount == 1) "1 álbum" else "${artist.albumCount} álbumes"
                val songLabel = if (artist.songCount == 1) "1 canción" else "${artist.songCount} canciones"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    tokens.primary.copy(alpha = 0.16f),
                                    tokens.surfaceCard
                                )
                            )
                        )
                        .border(1.dp, tokens.primary.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    if (isWide) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ArtistAvatar(
                                artistName = artist.name,
                                artUri = artist.artUri,
                                size = 136.dp,
                                modifier = Modifier.shadow(12.dp, CircleShape)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = tokens.textColor,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$albumLabel • $songLabel",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.mutedTextColor
                                )
                                Spacer(Modifier.height(4.dp))
                                ArtistActionButtons(
                                    hasSongs = artist.songsOrderedByYear.isNotEmpty(),
                                    onPlay = { onPlayArtistOrdered(artist.songsOrderedByYear) },
                                    onShuffle = { onShuffleArtist(artist.songs) }
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ArtistAvatar(
                                artistName = artist.name,
                                artUri = artist.artUri,
                                size = 122.dp,
                                modifier = Modifier.shadow(12.dp, CircleShape)
                            )
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = tokens.textColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$albumLabel • $songLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.mutedTextColor
                            )
                            Spacer(Modifier.height(2.dp))
                            ArtistActionButtons(
                                hasSongs = artist.songsOrderedByYear.isNotEmpty(),
                                onPlay = { onPlayArtistOrdered(artist.songsOrderedByYear) },
                                onShuffle = { onShuffleArtist(artist.songs) }
                            )
                        }
                    }
                }
            }

            // Albums Section Header + Sort Filter Chips
            item(key = "artist_albums_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.albums,
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.textColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (artist.albums.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AlbumSortOption.entries, key = { it.name }) { opt ->
                                val selected = sortOption == opt
                                FilterChip(
                                    selected = selected,
                                    onClick = { sortOption = opt },
                                    label = { Text(opt.labelEs) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = tokens.primary.copy(alpha = 0.22f),
                                        selectedLabelColor = tokens.primary,
                                        containerColor = tokens.surfaceCard,
                                        labelColor = tokens.mutedTextColor
                                    ),
                                    modifier = Modifier.testTag("artist_sort_${opt.name}")
                                )
                            }
                        }
                    }
                }
            }

            // Empty Albums State OR Year-Organized Album Cards
            if (sortedAlbums.isEmpty()) {
                item(key = "artist_no_albums") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(tokens.surfaceCard)
                            .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Album,
                                contentDescription = null,
                                tint = tokens.mutedTextColor,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = strings.noAlbumsForArtist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.mutedTextColor
                            )
                        }
                    }
                }
            } else {
                yearBuckets.forEach { (yearLabel, bucketAlbums) ->
                    if (groupByYear && yearLabel.isNotBlank()) {
                        item(key = "year_hdr_$yearLabel") {
                            Text(
                                text = yearLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    if (columns == 1) {
                        items(bucketAlbums, key = { "art_alb_${it.id}_${it.name}_${it.artist}" }) { album ->
                            ArtistAlbumRowCard(
                                album = album,
                                onClick = { onOpenAlbum(album) }
                            )
                        }
                    } else {
                        val rows = bucketAlbums.chunked(columns)
                        items(
                            items = rows,
                            key = { row -> "grid_row_${yearLabel}_${row.joinToString("_") { "${it.id}_${it.name}" }}" }
                        ) { rowAlbums ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowAlbums.forEach { album ->
                                    ArtistAlbumGridCard(
                                        album = album,
                                        onClick = { onOpenAlbum(album) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowAlbums.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Artist's Top / Key Songs Section
            if (artist.topSongs.isNotEmpty()) {
                item(key = "artist_songs_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.songs,
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.textColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (artist.topSongs.size > 10) {
                            TextButton(onClick = { showAllSongs = !showAllSongs }) {
                                Text(
                                    text = if (showAllSongs) "Ver menos" else "Ver todas (${artist.topSongs.size})",
                                    color = tokens.primary
                                )
                            }
                        }
                    }
                }

                items(displayedSongs, key = { "art_song_${it.id}" }) { song ->
                    ArtistTopSongRow(
                        song = song,
                        isPlayingCurrent = playbackState.currentSong?.id == song.id,
                        isFavorite = song.id in favoriteIds,
                        onPlay = { onPlaySong(song, artist.topSongs) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onAddToQueue = { onAddToQueue(song) },
                        onMoreOptions = { onSongMoreOptions(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistActionButtons(
    hasSongs: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    val shape = RoundedCornerShape(tokens.buttonCornerDp.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onPlay,
            enabled = hasSongs,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = tokens.buttonColor,
                contentColor = tokens.buttonColor.contrastText()
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("artist_play_btn")
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.playAll, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(
            onClick = onShuffle,
            enabled = hasSongs,
            shape = shape,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("artist_shuffle_btn")
        ) {
            Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = tokens.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.shufflePlay, color = tokens.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ArtistAlbumRowCard(
    album: AlbumGroup,
    onClick: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val countText = if (album.songs.size == 1) "1 canción" else "${album.songs.size} canciones"
    val metaText = if (album.year in 1900..2100) "${album.year} • $countText" else countText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.surfaceCard)
            .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("artist_album_card_${album.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(
            artUri = album.artUri,
            contentDescription = album.name,
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                color = tokens.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metaText,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.mutedTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistAlbumGridCard(
    album: AlbumGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTrakasTokens.current
    val countText = if (album.songs.size == 1) "1 canción" else "${album.songs.size} canciones"
    val metaText = if (album.year in 1900..2100) "${album.year} • $countText" else countText

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.surfaceCard)
            .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("artist_album_grid_${album.id}")
    ) {
        ArtworkImage(
            artUri = album.artUri,
            contentDescription = album.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleSmall,
            color = tokens.textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = metaText,
            style = MaterialTheme.typography.bodySmall,
            color = tokens.mutedTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistTopSongRow(
    song: Song,
    isPlayingCurrent: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToQueue: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val shape = RoundedCornerShape(tokens.buttonCornerDp.dp)
    val bg = if (isPlayingCurrent) tokens.primary.copy(alpha = 0.14f) else tokens.surfaceCard
    val border = if (isPlayingCurrent) tokens.primary.copy(alpha = 0.55f) else tokens.textColor.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onPlay)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            ArtworkImage(
                artUri = song.albumArtUri,
                contentDescription = song.title,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            if (isPlayingCurrent) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.52f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = "Reproduciendo", tint = tokens.primary)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isPlayingCurrent) tokens.primary else tokens.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.album} • ${formatDuration(song.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.mutedTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onAddToQueue, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = "Agregar a cola",
                tint = tokens.mutedTextColor,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorito",
                tint = if (isFavorite) tokens.secondary else tokens.mutedTextColor,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onMoreOptions, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Más opciones",
                tint = tokens.mutedTextColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

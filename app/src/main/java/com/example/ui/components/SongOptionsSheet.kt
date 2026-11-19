package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.PlaylistWithSongs
import com.example.model.Song
import com.example.model.SongTechnicalInfo
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
    song: Song,
    isFavorite: Boolean,
    playlists: List<PlaylistWithSongs>,
    onDismiss: () -> Unit,
    onPlayNow: (() -> Unit)? = null,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onCreatePlaylistWithSong: (String) -> Unit,
    onViewArtist: (String) -> Unit,
    onViewAlbum: (String) -> Unit,
    onEditMetadata: () -> Unit,
    onChangeCoverArt: (String?) -> Unit,
    onViewLyrics: () -> Unit,
    onLoadTechnicalInfo: suspend (Song) -> SongTechnicalInfo,
    onRemoveFromLibrary: (Song) -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current

    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var techInfo by remember(song.id) { mutableStateOf<SongTechnicalInfo?>(null) }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onChangeCoverArt(uri.toString())
            onDismiss()
        }
    }

    LaunchedEffect(showDetailsDialog, song.id) {
        if (showDetailsDialog && techInfo == null) {
            techInfo = onLoadTechnicalInfo(song)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.surfaceElevated,
        contentColor = tokens.textColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ArtworkImage(
                    artUri = song.albumArtUri,
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} • ${song.album} • ${song.format}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.mutedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = tokens.textColor.copy(alpha = 0.1f))
            Spacer(Modifier.height(6.dp))

            if (onPlayNow != null) {
                SheetOptionRow(Icons.Rounded.PlayArrow, strings.playNow, iconTint = tokens.primary, testTag = "option_play_now") {
                    onPlayNow()
                    onDismiss()
                }
            }
            SheetOptionRow(Icons.Rounded.PlayArrow, strings.playNext) {
                onPlayNext()
                onDismiss()
            }
            SheetOptionRow(Icons.AutoMirrored.Rounded.QueueMusic, strings.addToQueue, testTag = "option_add_queue") {
                onAddToQueue()
                onDismiss()
            }
            SheetOptionRow(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                strings.toggleFavorite,
                iconTint = if (isFavorite) tokens.secondary else tokens.textColor
            ) {
                onToggleFavorite()
                onDismiss()
            }
            SheetOptionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, strings.addToPlaylist, testTag = "option_add_playlist") {
                showPlaylistPicker = true
            }
            SheetOptionRow(Icons.Rounded.Person, strings.viewArtist, iconTint = tokens.primary, testTag = "option_view_artist") {
                onDismiss()
                onViewArtist(song.artist)
            }
            SheetOptionRow(Icons.Rounded.Album, strings.viewAlbum, iconTint = tokens.primary, testTag = "option_view_album") {
                onDismiss()
                onViewAlbum(song.album)
            }
            SheetOptionRow(Icons.Rounded.Lyrics, strings.lyrics, iconTint = tokens.secondary) {
                onDismiss()
                onViewLyrics()
            }
            SheetOptionRow(Icons.Rounded.Edit, strings.editMetadata, testTag = "option_edit_info") {
                onDismiss()
                onEditMetadata()
            }
            SheetOptionRow(Icons.Rounded.Image, strings.changeCoverArt, iconTint = tokens.primary, testTag = "option_change_cover") {
                coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            SheetOptionRow(Icons.Rounded.Share, strings.shareSong, testTag = "option_share") {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, song.contentUri)
                    putExtra(Intent.EXTRA_TEXT, "${song.title} — ${song.artist} (${song.album})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, strings.shareSong))
                onDismiss()
            }
            SheetOptionRow(Icons.Rounded.Info, strings.fileProperties, testTag = "option_file_properties") {
                showDetailsDialog = true
            }
            SheetOptionRow(
                Icons.Rounded.DeleteOutline,
                strings.removeFromLibrary,
                iconTint = MaterialTheme.colorScheme.error,
                testTag = "option_remove_library"
            ) {
                showDeleteConfirm = true
            }

            Spacer(Modifier.height(22.dp))
        }
    }

    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.addToPlaylist, color = tokens.textColor) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showPlaylistPicker = false
                                showCreatePlaylistDialog = true
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = tokens.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(strings.createPlaylist, color = tokens.primary)
                    }
                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(playlists, key = { it.id }) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onAddToPlaylist(pl.id)
                                        showPlaylistPicker = false
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(pl.name, color = tokens.textColor)
                                Text("${pl.songs.size}", color = tokens.mutedTextColor)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistPicker = false }) {
                    Text(strings.cancel, color = tokens.primary)
                }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.createPlaylist, color = tokens.textColor) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    singleLine = true,
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        onCreatePlaylistWithSong(newPlaylistName)
                        showCreatePlaylistDialog = false
                        onDismiss()
                    }
                }) {
                    Text(strings.save, color = tokens.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text(strings.cancel, color = tokens.mutedTextColor)
                }
            }
        )
    }

    if (showDetailsDialog) {
        val info = techInfo
        val sampleKhz = if (info != null) {
            String.format(Locale.US, "%.1f kHz (%d Hz)", info.sampleRateHz / 1000f, info.sampleRateHz)
        } else {
            "44.1 kHz"
        }
        val bitrateStr = if (info != null) "${info.bitrateKbps} kbps" else "Calculando..."
        val sizeStr = formatFileSize(info?.sizeBytes ?: song.sizeBytes)

        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.fileProperties, color = tokens.textColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Título: ${song.title}", color = tokens.textColor, style = MaterialTheme.typography.titleSmall)
                    Text("Artista: ${song.artist}", color = tokens.mutedTextColor)
                    Text("Álbum: ${song.album} (${song.year})", color = tokens.mutedTextColor)
                    HorizontalDivider(color = tokens.textColor.copy(alpha = 0.1f))
                    Text("Formato: ${song.format}", color = tokens.primary)
                    Text("Bitrate: $bitrateStr", color = tokens.textColor)
                    Text("Frecuencia de muestreo: $sampleKhz", color = tokens.textColor)
                    Text("Duración: ${formatDuration(song.durationMs)}", color = tokens.textColor)
                    Text("Tamaño del archivo: $sizeStr", color = tokens.textColor)
                    Text("Carpeta: ${song.folderPath}", color = tokens.mutedTextColor, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("OK", color = tokens.primary)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.removeFromLibrary, color = tokens.textColor) },
            text = {
                Text(
                    "¿Quitar '${song.title}' de tu biblioteca en Trakas Music Player?",
                    color = tokens.mutedTextColor
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveFromLibrary(song)
                    showDeleteConfirm = false
                    onDismiss()
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancel, color = tokens.mutedTextColor)
                }
            }
        )
    }
}

@Composable
private fun SheetOptionRow(
    icon: ImageVector,
    label: String,
    iconTint: Color? = null,
    testTag: String? = null,
    onClick: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint ?: tokens.textColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.textColor
        )
    }
}

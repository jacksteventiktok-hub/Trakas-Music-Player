package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppPreferences
import com.example.model.Song
import com.example.player.PlaybackUiState
import com.example.ui.components.ArtworkImage
import com.example.ui.components.rememberDynamicArtColors
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.contrastText

private data class LyricLine(
    val timestampMs: Long?,
    val text: String
)

private fun parseLyricsLines(raw: String): List<LyricLine> {
    val regex = Regex("^\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?](.*)$")
    return raw.lines().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@mapNotNull null
        val match = regex.find(trimmed)
        if (match != null) {
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val msPart = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            val ts = min * 60_000L + sec * 1000L + msPart
            LyricLine(ts, match.groupValues[4].trim())
        } else {
            LyricLine(null, trimmed)
        }
    }
}

@Composable
fun LyricsOverlayScreen(
    song: Song,
    rawLyrics: String?,
    playbackState: PlaybackUiState,
    prefs: AppPreferences,
    onDismiss: () -> Unit,
    onSaveLyrics: (Long, String) -> Unit,
    onSetKaraoke: (Boolean) -> Unit,
    onSetFontSize: (Float) -> Unit,
    onSetFontKey: (String) -> Unit,
    onSetTextColor: (Long) -> Unit,
    onSetHighlightColor: (Long) -> Unit,
    onSetDynamicBg: (Boolean) -> Unit
) {
    BackHandler(onBack = onDismiss)
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    var showEditDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    var bgPresetIdx by remember { mutableStateOf(if (prefs.lyricsDynamicBg) 0 else 1) }

    val (c1, c2) = rememberDynamicArtColors(song.albumArtUri, tokens.primary, tokens.secondary)
    val bgBrush = when (bgPresetIdx) {
        0 -> Brush.verticalGradient(listOf(c1.copy(alpha = 0.52f), c2.copy(alpha = 0.30f), tokens.background))
        2 -> Brush.verticalGradient(listOf(Color.Black, Color.Black))
        3 -> Brush.verticalGradient(listOf(Color(0xFF1A0933), Color(0xFF0A0514)))
        else -> Brush.verticalGradient(listOf(tokens.background, tokens.surfaceElevated))
    }

    val fontFamily = when (prefs.lyricsFontKey) {
        "mono" -> JetBrainsMonoFontFamily
        "jakarta" -> PlusJakartaFontFamily
        else -> SpaceGroteskFontFamily
    }

    val parsedLines = remember(rawLyrics) {
        if (rawLyrics.isNullOrBlank()) emptyList() else parseLyricsLines(rawLyrics)
    }

    val activeLineIndex = remember(parsedLines, playbackState.positionMs, playbackState.durationMs, prefs.lyricsKaraokeMode) {
        if (!prefs.lyricsKaraokeMode || parsedLines.isEmpty()) -1
        else if (parsedLines.any { it.timestampMs != null }) {
            parsedLines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= playbackState.positionMs }
        } else {
            val dur = playbackState.durationMs.coerceAtLeast(1L)
            val frac = (playbackState.positionMs.toFloat() / dur.toFloat()).coerceIn(0f, 0.999f)
            (frac * parsedLines.size).toInt().coerceIn(0, parsedLines.lastIndex)
        }
    }

    LaunchedEffect(activeLineIndex, prefs.lyricsKaraokeMode) {
        if (prefs.lyricsKaraokeMode && activeLineIndex >= 0 && activeLineIndex < parsedLines.size) {
            runCatching { listState.animateScrollToItem((activeLineIndex - 1).coerceAtLeast(0)) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Lyrics, null, tint = tokens.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, color = tokens.textColor, maxLines = 1)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor, maxLines = 1)
                }
            }
            IconButton(onClick = { showEditDialog = true }, modifier = Modifier.testTag("lyrics_edit_btn")) {
                Icon(Icons.Rounded.Edit, contentDescription = "Editar letras", tint = tokens.primary)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.testTag("lyrics_close_btn")) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = tokens.textColor)
            }
        }

        // Style Customization Strip
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = prefs.lyricsKaraokeMode,
                    onClick = { onSetKaraoke(!prefs.lyricsKaraokeMode) },
                    label = { Text(if (prefs.lyricsKaraokeMode) strings.karaokeMode else strings.normalMode) }
                )
            }
            item {
                val bgLabel = when (bgPresetIdx) {
                    0 -> "Fondo: Dinámico"
                    2 -> "Fondo: AMOLED"
                    3 -> "Fondo: Púrpura"
                    else -> "Fondo: Oscuro"
                }
                FilterChip(
                    selected = bgPresetIdx == 0,
                    onClick = {
                        bgPresetIdx = (bgPresetIdx + 1) % 4
                        onSetDynamicBg(bgPresetIdx == 0)
                    },
                    label = { Text(bgLabel) }
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = {
                        val next = if (prefs.lyricsFontSizeSp >= 28f) 16f else prefs.lyricsFontSizeSp + 2f
                        onSetFontSize(next)
                    },
                    label = { Text("Tamaño ${prefs.lyricsFontSizeSp.toInt()}sp") }
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = {
                        val keys = listOf("space", "jakarta", "mono")
                        val next = keys[(keys.indexOf(prefs.lyricsFontKey) + 1) % keys.size]
                        onSetFontKey(next)
                    },
                    label = { Text("Fuente: ${prefs.lyricsFontKey}") }
                )
            }
            val swatches = listOf(0xFFF3F7FF, 0xFF00E5FF, 0xFFFF4081, 0xFF00E676, 0xFFFFD600)
            items(swatches) { col ->
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(col))
                        .border(if (prefs.lyricsHighlightColorArgb == col) 2.5.dp else 1.dp, Color.White, CircleShape)
                        .clickable {
                            onSetHighlightColor(col)
                            onSetTextColor(0xFFF3F7FF)
                        }
                )
            }
        }

        if (parsedLines.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.noLyricsAvailable,
                        style = MaterialTheme.typography.headlineSmall,
                        color = tokens.mutedTextColor,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.buttonColor, contentColor = tokens.buttonColor.contrastText())
                    ) {
                        Text("Agregar letra o LRC")
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(parsedLines) { idx, line ->
                    val isActive = idx == activeLineIndex
                    Text(
                        text = line.text,
                        fontFamily = fontFamily,
                        fontSize = if (isActive) (prefs.lyricsFontSizeSp + 3f).sp else prefs.lyricsFontSizeSp.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) {
                            Color(prefs.lyricsHighlightColorArgb)
                        } else {
                            Color(prefs.lyricsTextColorArgb).copy(alpha = if (prefs.lyricsKaraokeMode) 0.55f else 0.92f)
                        },
                        lineHeight = (prefs.lyricsFontSizeSp + 9f).sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        var draft by remember(rawLyrics) { mutableStateOf(rawLyrics.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = tokens.surfaceElevated,
            title = { Text("${strings.lyrics} (LRC / Texto)", color = tokens.textColor) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    minLines = 6,
                    maxLines = 12,
                    placeholder = { Text("[00:12.00] Primera línea...\n[00:18.00] Segunda línea...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveLyrics(song.id, draft)
                    showEditDialog = false
                }) { Text(strings.save, color = tokens.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text(strings.cancel, color = tokens.mutedTextColor) }
            }
        )
    }
}

@Composable
fun MetadataEditorDialog(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current

    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var album by remember(song.id) { mutableStateOf(song.album) }
    var genre by remember(song.id) { mutableStateOf(song.genre) }
    var yearStr by remember(song.id) { mutableStateOf(song.year.toString()) }
    var trackStr by remember(song.id) { mutableStateOf(song.trackNumber.toString()) }
    var albumArtist by remember(song.id) { mutableStateOf(song.albumArtist.ifBlank { song.artist }) }
    var comment by remember(song.id) { mutableStateOf(song.comment) }
    var customArtUri by remember(song.id) { mutableStateOf<String?>(song.albumArtUri?.toString()) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            customArtUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surfaceElevated,
        title = { Text(strings.editMetadata, color = tokens.textColor) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ArtworkImage(
                        artUri = customArtUri?.let { Uri.parse(it) },
                        contentDescription = title,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                    )
                    OutlinedButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Portada del álbum")
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artista") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Álbum") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Género") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = yearStr, onValueChange = { yearStr = it }, label = { Text("Año") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = trackStr, onValueChange = { trackStr = it }, label = { Text("Pista #") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = albumArtist, onValueChange = { albumArtist = it }, label = { Text("Artista del álbum") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Comentarios") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        song.id,
                        title,
                        artist,
                        album,
                        genre,
                        yearStr.toIntOrNull() ?: 2024,
                        trackStr.toIntOrNull() ?: 1,
                        albumArtist,
                        comment,
                        customArtUri
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = tokens.buttonColor, contentColor = tokens.buttonColor.contrastText()),
                modifier = Modifier.testTag("save_metadata_changes_btn")
            ) {
                Text(strings.saveChanges)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel, color = tokens.mutedTextColor) }
        }
    )
}

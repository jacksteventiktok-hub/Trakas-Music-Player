package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.repository.AiTaskState
import com.example.data.repository.ListeningStatsSummary
import com.example.data.repository.TrakasReplaySummary
import com.example.model.AppLanguage
import com.example.model.Song
import com.example.model.StatsPeriod
import com.example.ui.components.ArtworkImage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import com.example.ui.theme.contrastText
import java.util.Locale

private enum class ExploreTab(val label: String) {
    AI("Trakas AI"),
    STATS("Trakas Stats"),
    REPLAY("Trakas Replay")
}

@Composable
fun ExploreAiStatsScreen(
    language: AppLanguage,
    allSongs: List<Song>,
    currentSong: Song?,
    aiState: AiTaskState,
    statsPeriod: StatsPeriod,
    statsSummary: ListeningStatsSummary,
    replaySummary: TrakasReplaySummary,
    onSelectStatsPeriod: (StatsPeriod) -> Unit,
    onRequestAi: (String) -> Unit,
    onFindLocalSimilar: (Song) -> List<Song>,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onSaveAiPlaylist: (String, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableStateOf(ExploreTab.AI) }
    var customGoal by remember { mutableStateOf("Mix energético y fluido con mis canciones favoritas") }
    var localSimilarSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(strings.navExplore, style = MaterialTheme.typography.headlineMedium, color = tokens.textColor)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExploreTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label, maxLines = 1) },
                        leadingIcon = {
                            val icon = when (tab) {
                                ExploreTab.AI -> Icons.Rounded.AutoAwesome
                                ExploreTab.STATS -> Icons.Rounded.BarChart
                                ExploreTab.REPLAY -> Icons.Rounded.EmojiEvents
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tokens.primary.copy(alpha = 0.24f),
                            selectedLabelColor = tokens.primary
                        ),
                        modifier = Modifier.weight(1f).testTag("explore_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        when (selectedTab) {
            ExploreTab.AI -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(tokens.surfaceCard)
                            .border(1.dp, tokens.primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(strings.trakasAi, style = MaterialTheme.typography.titleLarge, color = tokens.primary)
                        Text(
                            text = "Tu biblioteca local se procesa en tu dispositivo. Solo se consulta Trakas AI bajo demanda cuando presionas un botón.",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.mutedTextColor
                        )
                        OutlinedTextField(
                            value = customGoal,
                            onValueChange = { customGoal = it },
                            label = { Text("Describe tu playlist o estado de ánimo") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onRequestAi(customGoal) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.buttonColor,
                                    contentColor = tokens.buttonColor.contrastText()
                                ),
                                modifier = Modifier.weight(1f).testTag("ai_create_playlist_btn")
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Crear playlist IA", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    onRequestAi("Analiza mis hábitos de escucha, organízalos y recomienda 10 canciones ideales para mí.")
                                },
                                modifier = Modifier.weight(1f).testTag("ai_patterns_btn")
                            ) {
                                Text("Patrones y recs", maxLines = 1)
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val quickGoals = listOf(
                                "Organizar biblioteca por ambiente",
                                "Descubrir joyas poco reproducidas",
                                "Sesión nocturna relajada",
                                "Energía alta para entrenar"
                            )
                            items(quickGoals) { goal ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        customGoal = goal
                                        onRequestAi(goal)
                                    },
                                    label = { Text(goal) }
                                )
                            }
                        }
                        if (currentSong != null) {
                            OutlinedButton(
                                onClick = { localSimilarSongs = onFindLocalSimilar(currentSong) },
                                modifier = Modifier.fillMaxWidth().testTag("ai_local_similar_btn")
                            ) {
                                Text("Buscar similares locales a '${currentSong.title}'", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                if (localSimilarSongs.isNotEmpty()) {
                    item {
                        Text("Similares en tu dispositivo", style = MaterialTheme.typography.titleMedium, color = tokens.secondary)
                    }
                    itemsIndexed(localSimilarSongs, key = { idx, s -> "sim_${idx}_${s.id}" }) { idx, song ->
                        SongRow(
                            song = song,
                            isPlayingCurrent = currentSong?.id == song.id,
                            isFavorite = false,
                            onClick = { onPlaySongs(localSimilarSongs, idx) },
                            onLongClick = {},
                            onToggleFavorite = {}
                        )
                    }
                }

                item {
                    when (aiState) {
                        AiTaskState.Idle -> Unit
                        AiTaskState.Loading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(tokens.surfaceCard)
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = tokens.primary)
                                Text("Generando selección musical con Trakas AI...", color = tokens.textColor)
                            }
                        }
                        is AiTaskState.Error -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(tokens.surfaceCard)
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("No se pudo completar", style = MaterialTheme.typography.titleSmall, color = tokens.textColor)
                                    Text(aiState.message, style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
                                }
                            }
                        }
                        is AiTaskState.Success -> {
                            val sug = aiState.suggestion
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(tokens.surfaceCard)
                                    .border(1.dp, tokens.secondary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(sug.playlistName, style = MaterialTheme.typography.titleLarge, color = tokens.primary)
                                Text(sug.description, style = MaterialTheme.typography.bodyMedium, color = tokens.textColor)
                                sug.insights.forEach { insight ->
                                    Text("• $insight", style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onPlaySongs(sug.songs, 0) },
                                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primary, contentColor = tokens.primary.contrastText())
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Reproducir (${sug.songs.size})")
                                    }
                                    OutlinedButton(onClick = { onSaveAiPlaylist(sug.playlistName, sug.songs) }) {
                                        Icon(Icons.Rounded.Save, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.save)
                                    }
                                }
                                sug.songs.forEachIndexed { idx, s ->
                                    Text(
                                        text = "${idx + 1}. ${s.title} — ${s.artist}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.textColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onPlaySongs(sug.songs, idx) }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ExploreTab.STATS -> {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StatsPeriod.entries) { period ->
                            val lbl = if (language == AppLanguage.SPANISH) period.labelEs else period.labelEn
                            FilterChip(
                                selected = statsPeriod == period,
                                onClick = { onSelectStatsPeriod(period) },
                                label = { Text(lbl) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricBox("Minutos", "${statsSummary.totalMinutes}", Modifier.weight(1f))
                        StatMetricBox("Horas", String.format(Locale.US, "%.1f", statsSummary.totalHours), Modifier.weight(1f))
                        StatMetricBox("Reproducciones", "${statsSummary.totalPlays}", Modifier.weight(1f))
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(tokens.surfaceCard)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Patrones de escucha • ${statsSummary.peakHourLabel}", style = MaterialTheme.typography.titleSmall, color = tokens.primary)
                        Text(statsSummary.patternSummary, style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
                    }
                }

                if (statsSummary.topArtists.isNotEmpty()) {
                    item {
                        StatsRankedCard("Artistas más escuchados", statsSummary.topArtists)
                    }
                }
                if (statsSummary.topAlbums.isNotEmpty()) {
                    item {
                        StatsRankedCard("Álbumes más reproducidos", statsSummary.topAlbums)
                    }
                }
                if (statsSummary.topGenres.isNotEmpty()) {
                    item {
                        StatsRankedCard("Géneros más escuchados", statsSummary.topGenres)
                    }
                }
                if (statsSummary.topSongs.isNotEmpty()) {
                    item {
                        Text("Canciones más reproducidas", style = MaterialTheme.typography.titleMedium, color = tokens.textColor)
                    }
                    itemsIndexed(statsSummary.topSongs, key = { idx, p -> "st_${idx}_${p.first.id}" }) { idx, (song, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(tokens.surfaceCard)
                                .clickable { onPlaySongs(statsSummary.topSongs.map { it.first }, idx) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${idx + 1}. ${song.title} — ${song.artist}", color = tokens.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("$count rep.", style = MaterialTheme.typography.labelMedium, color = tokens.primary)
                        }
                    }
                }
            }

            ExploreTab.REPLAY -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(tokens.primary.copy(alpha = 0.25f), tokens.secondary.copy(alpha = 0.2f), tokens.surfaceCard)
                                )
                            )
                            .border(1.dp, tokens.primary.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(strings.trakasReplay, style = MaterialTheme.typography.headlineSmall, color = tokens.textColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatMetricBox("Minutos totales", "${replaySummary.totalMinutes}", Modifier.weight(1f))
                            StatMetricBox("Canciones", "${replaySummary.totalSongsPlayed}", Modifier.weight(1f))
                        }
                        val topSong = replaySummary.topSong
                        if (topSong != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(tokens.background.copy(alpha = 0.55f))
                                    .clickable { onPlaySongs(listOf(topSong), 0) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArtworkImage(
                                    artUri = topSong.albumArtUri,
                                    contentDescription = topSong.title,
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("CANCIÓN #1", style = MaterialTheme.typography.labelSmall, color = tokens.primary)
                                    Text(topSong.title, style = MaterialTheme.typography.titleMedium, color = tokens.textColor, maxLines = 1)
                                    Text("${topSong.artist} • ${replaySummary.topSongPlays} rep.", style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
                                }
                            }
                        }
                        Text("Artista #1: ${replaySummary.topArtist ?: "---"}", color = tokens.textColor)
                        Text("Álbum #1: ${replaySummary.topAlbum ?: "---"}", color = tokens.mutedTextColor)
                        Text("Género favorito: ${replaySummary.topGenre ?: "---"}", color = tokens.secondary)
                    }
                }

                if (replaySummary.topTenSongs.isNotEmpty()) {
                    item {
                        Text("Top 10 canciones más escuchadas", style = MaterialTheme.typography.titleMedium, color = tokens.textColor)
                    }
                    itemsIndexed(replaySummary.topTenSongs, key = { idx, p -> "rp_${idx}_${p.first.id}" }) { idx, (song, plays) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(tokens.surfaceCard)
                                .clickable { onPlaySongs(replaySummary.topTenSongs.map { it.first }, idx) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${idx + 1}", style = MaterialTheme.typography.titleSmall, color = tokens.primary, modifier = Modifier.width(34.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = tokens.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor, maxLines = 1)
                            }
                            Text("$plays", style = MaterialTheme.typography.labelLarge, color = tokens.secondary)
                        }
                    }
                } else if (allSongs.isEmpty()) {
                    item {
                        Text("Agrega y reproduce música local para generar tu Trakas Replay.", color = tokens.mutedTextColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    val tokens = LocalTrakasTokens.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.surfaceCard)
            .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = tokens.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = tokens.mutedTextColor)
    }
}

@Composable
private fun StatsRankedCard(title: String, items: List<Pair<String, Int>>) {
    val tokens = LocalTrakasTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.surfaceCard)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = tokens.primary)
        items.forEachIndexed { idx, (name, count) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${idx + 1}. $name", style = MaterialTheme.typography.bodyMedium, color = tokens.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("$count", style = MaterialTheme.typography.labelMedium, color = tokens.secondary)
            }
        }
    }
}

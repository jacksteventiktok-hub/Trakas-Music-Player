package com.example.data.repository

import android.util.Log
import com.example.data.local.ListeningSessionEntity
import com.example.data.local.PlayHistoryEntity
import com.example.model.Song
import com.example.model.StatsPeriod
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.abs

data class AiPlaylistSuggestion(
    val playlistName: String,
    val description: String,
    val songs: List<Song>,
    val insights: List<String>,
    val fromCloudAi: Boolean
)

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data object Loading : AiTaskState
    data class Success(val suggestion: AiPlaylistSuggestion) : AiTaskState
    data class Error(val message: String) : AiTaskState
}

data class ListeningStatsSummary(
    val totalMinutes: Int,
    val totalHours: Float,
    val totalPlays: Int,
    val topSongs: List<Pair<Song, Int>>,
    val topArtists: List<Pair<String, Int>>,
    val topAlbums: List<Pair<String, Int>>,
    val topGenres: List<Pair<String, Int>>,
    val topFavorites: List<Song>,
    val peakHourLabel: String,
    val patternSummary: String
)

data class TrakasReplaySummary(
    val topSong: Song?,
    val topSongPlays: Int,
    val topArtist: String?,
    val topAlbum: String?,
    val topGenre: String?,
    val totalMinutes: Int,
    val totalSongsPlayed: Int,
    val topTenSongs: List<Pair<Song, Int>>
)

class TrakasAiRepository {

    companion object {
        private const val TAG = "TrakasAiRepository"
    }

    private val playlistSchema = Schema.obj(
        properties = mapOf(
            "playlistName" to Schema.string(description = "Short creative playlist name in Spanish"),
            "description" to Schema.string(description = "One-sentence summary of the mood or goal"),
            "selectedSongIds" to Schema.array(
                items = Schema.string(description = "Exact numeric song ID from the provided catalog"),
                description = "IDs of the selected tracks in playback order"
            ),
            "insights" to Schema.array(
                items = Schema.string(description = "Concise bullet point insight in Spanish"),
                description = "2 to 4 musical observations or listening pattern tips"
            )
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = playlistSchema
            }
        )
    }

    suspend fun generateAiPlaylistOrInsight(
        promptGoal: String,
        songs: List<Song>,
        history: List<PlayHistoryEntity>,
        favoriteIds: Set<Long>
    ): Result<AiPlaylistSuggestion> {
        if (songs.isEmpty()) {
            return Result.failure(IllegalStateException("Escanea o importa música local primero."))
        }
        return try {
            val catalogPreview = songs.take(80).joinToString("\n") { s ->
                val plays = history.find { it.songId == s.id }?.playCount ?: 0
                val fav = if (s.id in favoriteIds) "fav" else ""
                "id=${s.id}|title=${s.title}|artist=${s.artist}|album=${s.album}|genre=${s.genre}|year=${s.year}|plays=$plays|$fav"
            }

            val fullPrompt = """
                Eres el motor Trakas AI integrado en un reproductor de música local.
                Reglas:
                - Selecciona únicamente IDs existentes en el catálogo proporcionado abajo.
                - Responde exclusivamente siguiendo el esquema JSON.
                - Mantén las recomendaciones enfocadas en playlists, estados de ánimo y disfrute musical.
                Objetivo del usuario: $promptGoal
                Catálogo local (máx 80 canciones):
                $catalogPreview
            """.trimIndent()

            val response = withTimeout(60_000L) {
                structuredModel.generateContent(fullPrompt)
            }
            val raw = response.text ?: throw IllegalStateException("La IA devolvió una respuesta vacía.")
            val json = JSONObject(raw)
            val name = json.optString("playlistName", "Mix Trakas AI")
            val desc = json.optString("description", "Selección inteligente basada en tu biblioteca local.")
            val idArr = json.optJSONArray("selectedSongIds")
            val songById = songs.associateBy { it.id.toString() }
            val picked = buildList<Song> {
                if (idArr != null) {
                    for (i in 0 until idArr.length()) {
                        val s = songById[idArr.optString(i).trim()]
                        if (s != null && none { existing -> existing.id == s.id }) add(s)
                    }
                }
            }.ifEmpty { songs.take(8) }

            val insightsArr = json.optJSONArray("insights")
            val insights = buildList {
                if (insightsArr != null) {
                    for (i in 0 until insightsArr.length()) {
                        val line = insightsArr.optString(i).trim()
                        if (line.isNotEmpty()) add(line)
                    }
                }
            }

            Result.success(
                AiPlaylistSuggestion(
                    playlistName = name,
                    description = desc,
                    songs = picked,
                    insights = insights,
                    fromCloudAi = true
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI timed out", e)
            Result.failure(IllegalStateException("La solicitud a Trakas AI tardó demasiado. Intenta de nuevo."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI failed", e)
            Result.failure(e)
        }
    }

    fun findLocalSimilarSongs(anchor: Song, catalog: List<Song>): List<Song> {
        return catalog
            .filter { it.id != anchor.id }
            .sortedByDescending { candidate ->
                var score = 0
                if (candidate.genre.equals(anchor.genre, ignoreCase = true)) score += 50
                if (candidate.artist.equals(anchor.artist, ignoreCase = true)) score += 40
                if (candidate.album.equals(anchor.album, ignoreCase = true)) score += 25
                if (abs(candidate.year - anchor.year) <= 2) score += 15
                val durDiff = abs(candidate.durationMs - anchor.durationMs)
                if (durDiff < 45_000L) score += 10
                score
            }
            .take(12)
    }

    fun computeLocalStats(
        period: StatsPeriod,
        songs: List<Song>,
        history: List<PlayHistoryEntity>,
        sessions: List<ListeningSessionEntity>,
        favoriteIds: Set<Long>
    ): ListeningStatsSummary {
        val cutoff = System.currentTimeMillis() - period.days * 24L * 60L * 60L * 1000L
        val songMap = songs.associateBy { it.id }

        val periodSessions = sessions.filter { it.timestamp >= cutoff }
        val totalMs = if (periodSessions.isNotEmpty()) {
            periodSessions.sumOf { it.durationListenedMs }
        } else {
            history.filter { it.lastPlayedAt >= cutoff }.sumOf { it.playCount * 180_000L }
        }
        val totalMinutes = (totalMs / 60_000L).toInt()
        val totalHours = totalMinutes / 60f

        val countsBySong = mutableMapOf<Long, Int>()
        if (periodSessions.isNotEmpty()) {
            periodSessions.forEach { s ->
                countsBySong[s.songId] = (countsBySong[s.songId] ?: 0) + 1
            }
        } else {
            history.filter { it.lastPlayedAt >= cutoff }.forEach { h ->
                countsBySong[h.songId] = h.playCount
            }
        }
        if (countsBySong.isEmpty() && history.isNotEmpty()) {
            history.forEach { h -> countsBySong[h.songId] = h.playCount }
        }

        val topSongs = countsBySong.entries
            .mapNotNull { (id, cnt) -> songMap[id]?.let { it to cnt } }
            .sortedByDescending { it.second }
            .take(10)

        val artistCounts = mutableMapOf<String, Int>()
        val albumCounts = mutableMapOf<String, Int>()
        val genreCounts = mutableMapOf<String, Int>()
        countsBySong.forEach { (id, cnt) ->
            val song = songMap[id] ?: return@forEach
            artistCounts[song.artist] = (artistCounts[song.artist] ?: 0) + cnt
            albumCounts[song.album] = (albumCounts[song.album] ?: 0) + cnt
            genreCounts[song.genre] = (genreCounts[song.genre] ?: 0) + cnt
        }

        val hourCounts = IntArray(24)
        periodSessions.forEach { sess ->
            val cal = Calendar.getInstance().apply { timeInMillis = sess.timestamp }
            hourCounts[cal.get(Calendar.HOUR_OF_DAY)]++
        }
        val peakHour = hourCounts.indices.maxByOrNull { hourCounts[it] } ?: 20
        val peakLabel = when (peakHour) {
            in 5..11 -> "Mañana (${peakHour}:00)"
            in 12..17 -> "Tarde (${peakHour}:00)"
            in 18..22 -> "Noche (${peakHour}:00)"
            else -> "Madrugada (${peakHour}:00)"
        }

        val topGenreName = genreCounts.entries.maxByOrNull { it.value }?.key
        val patternText = if (topGenreName != null) {
            "Tu género dominante en este período es $topGenreName y escuchas más durante la franja: $peakLabel."
        } else {
            "Reproduce canciones de tu biblioteca para detectar hábitos y horarios favoritos localmente."
        }

        return ListeningStatsSummary(
            totalMinutes = totalMinutes,
            totalHours = totalHours,
            totalPlays = countsBySong.values.sum(),
            topSongs = topSongs,
            topArtists = artistCounts.entries.sortedByDescending { it.value }.map { it.key to it.value }.take(5),
            topAlbums = albumCounts.entries.sortedByDescending { it.value }.map { it.key to it.value }.take(5),
            topGenres = genreCounts.entries.sortedByDescending { it.value }.map { it.key to it.value }.take(5),
            topFavorites = songs.filter { it.id in favoriteIds }.take(8),
            peakHourLabel = peakLabel,
            patternSummary = patternText
        )
    }

    fun computeLocalReplay(
        songs: List<Song>,
        history: List<PlayHistoryEntity>,
        sessions: List<ListeningSessionEntity>
    ): TrakasReplaySummary {
        val stats = computeLocalStats(StatsPeriod.YEARLY, songs, history, sessions, emptySet())
        val topPair = stats.topSongs.firstOrNull()
        return TrakasReplaySummary(
            topSong = topPair?.first,
            topSongPlays = topPair?.second ?: 0,
            topArtist = stats.topArtists.firstOrNull()?.first,
            topAlbum = stats.topAlbums.firstOrNull()?.first,
            topGenre = stats.topGenres.firstOrNull()?.first,
            totalMinutes = stats.totalMinutes,
            totalSongsPlayed = stats.totalPlays,
            topTenSongs = stats.topSongs.take(10)
        )
    }
}

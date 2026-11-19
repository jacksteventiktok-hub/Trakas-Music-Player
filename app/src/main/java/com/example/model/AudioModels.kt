package com.example.model

import android.net.Uri
import kotlinx.serialization.Serializable

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val genre: String,
    val folderName: String,
    val folderPath: String,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val format: String,
    val sizeBytes: Long,
    val dateAdded: Long,
    val year: Int = 2024,
    val trackNumber: Int = 1,
    val albumArtist: String = "",
    val comment: String = "",
    val replayGainDb: Float = 0f,
    val isImported: Boolean = false
)

data class AlbumGroup(
    val id: Long,
    val name: String,
    val artist: String,
    val year: Int,
    val totalDurationMs: Long,
    val isCompilation: Boolean,
    val artUri: Uri?,
    val songs: List<Song>
)

data class ArtistGroup(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val artUri: Uri?,
    val albums: List<AlbumGroup>,
    val songs: List<Song>,
    val songsOrderedByYear: List<Song>,
    val topSongs: List<Song>
)

enum class AlbumSortOption(val labelEs: String, val labelEn: String) {
    NEWEST("Más recientes", "Newest"),
    OLDEST("Más antiguos", "Oldest"),
    AZ("A-Z", "A-Z"),
    ZA("Z-A", "Z-A")
}

enum class AlbumsFilterOption(val labelEs: String, val labelEn: String) {
    ALL("Todos", "All"),
    ALBUMS("Álbumes", "Albums"),
    COMPILATIONS("Compilaciones", "Compilations")
}

data class GenreGroup(
    val name: String,
    val songs: List<Song>
)

data class FolderGroup(
    val name: String,
    val path: String,
    val songs: List<Song>
)

data class PlaylistWithSongs(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songs: List<Song>
)

sealed interface LibraryNavigationTarget {
    data class Artist(val name: String) : LibraryNavigationTarget
    data class Album(val name: String, val albumId: Long? = null) : LibraryNavigationTarget
}

data class SongTechnicalInfo(
    val format: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val durationMs: Long,
    val sizeBytes: Long
)

enum class TextScalePreset(val scale: Float, val labelEs: String, val labelEn: String) {
    NORMAL(1.0f, "Normal", "Normal"),
    LARGE(1.15f, "Grande", "Large"),
    EXTRA_LARGE(1.30f, "Extra grande", "Extra Large");

    companion object {
        fun fromScale(scale: Float): TextScalePreset = when {
            scale >= 1.22f -> EXTRA_LARGE
            scale >= 1.08f -> LARGE
            else -> NORMAL
        }
    }
}

enum class InterfaceMode(val id: String, val labelEs: String, val labelEn: String) {
    TRAKAS_PLAYER("player", "Trakas Player", "Trakas Player"),
    TRAKAS_PARTY("party", "Trakas Party", "Trakas Party"),
    TRAKAS_STUDIO("studio", "Trakas Studio", "Trakas Studio"),
    TRAKAS_DRIVE("drive", "Trakas Drive", "Trakas Drive"),
    TRAKAS_NIGHT("night", "Trakas Night", "Trakas Night");

    companion object {
        fun fromId(id: String): InterfaceMode = entries.find { it.id == id } ?: TRAKAS_PLAYER
    }
}

enum class VisualizerStyle(val id: String, val labelEs: String, val labelEn: String) {
    SPECTRUM("spectrum", "Spectrum", "Spectrum"),
    WAVEFORM("waveform", "Waveform", "Waveform"),
    BARS("bars", "Bars", "Bars"),
    CIRCLE("circle", "Circle", "Circle"),
    PARTICLES("particles", "Particles", "Particles"),
    ALBUM_COLORS("album_colors", "Album Colors", "Album Colors");

    companion object {
        fun fromId(id: String): VisualizerStyle = entries.find { it.id == id } ?: SPECTRUM
    }
}

data class VisualizerConfig(
    val style: VisualizerStyle = VisualizerStyle.SPECTRUM,
    val intensity: Float = 0.9f,
    val speed: Float = 1.0f,
    val sensitivity: Float = 1.0f,
    val transparency: Float = 0.9f,
    val particleCount: Int = 36,
    val useAlbumColors: Boolean = false
)

enum class IconStyle(val id: String, val labelEs: String, val labelEn: String) {
    MATERIAL("material", "Material", "Material"),
    MINIMAL("minimal", "Minimal", "Minimal"),
    NEON("neon", "Neon", "Neon"),
    CYBER("cyber", "Cyber", "Cyber"),
    RETRO("retro", "Retro", "Retro"),
    TRAKAS("trakas", "Trakas", "Trakas");

    companion object {
        fun fromId(id: String): IconStyle = entries.find { it.id == id } ?: NEON
    }
}

enum class ButtonShapeStyle(val id: String, val labelEs: String, val labelEn: String, val cornerDp: Int) {
    PILL("pill", "Píldora", "Pill", 28),
    ROUNDED("rounded", "Redondeado", "Rounded", 14),
    CHAMFERED("chamfered", "Técnico", "Sharp", 6);

    companion object {
        fun fromId(id: String): ButtonShapeStyle = entries.find { it.id == id } ?: ROUNDED
    }
}

enum class AudioQuality(val id: String, val labelEs: String, val labelEn: String) {
    STANDARD("standard", "Estándar (44.1 kHz)", "Standard (44.1 kHz)"),
    HIGH_FIDELITY("hifi", "Alta Fidelidad (48 kHz)", "High Fidelity (48 kHz)"),
    STUDIO_24BIT("studio", "Estudio Ultra (Hi-Res)", "Studio Ultra (Hi-Res)");

    companion object {
        fun fromId(id: String): AudioQuality = entries.find { it.id == id } ?: HIGH_FIDELITY
    }
}

enum class AppLanguage(val code: String, val label: String) {
    SPANISH("es", "Español"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String): AppLanguage = entries.find { it.code == code } ?: SPANISH
    }
}

enum class RepeatModeOption {
    OFF, ALL, ONE
}

enum class StatsPeriod(val labelEs: String, val labelEn: String, val days: Int) {
    DAILY("Diarias", "Daily", 1),
    WEEKLY("Semanales", "Weekly", 7),
    MONTHLY("Mensuales", "Monthly", 30),
    YEARLY("Anuales", "Yearly", 365)
}

enum class SmartPlaylistKind(val labelEs: String, val labelEn: String) {
    MOST_PLAYED("Más reproducidas", "Most Played"),
    RECENT("Recientes", "Recently Played"),
    FAVORITES("Favoritas", "Favorites"),
    RECENTLY_ADDED("Agregadas recientemente", "Recently Added"),
    NEVER_PLAYED("Nunca reproducidas", "Never Played"),
    BY_ARTIST("Por artista", "By Artist"),
    BY_GENRE("Por género", "By Genre"),
    BY_YEAR("Por año", "By Year")
}

enum class EqPreset(
    val id: String,
    val labelEs: String,
    val labelEn: String,
    val bandsMb: List<Int>
) {
    FLAT("flat", "Flat", "Flat", listOf(0, 0, 0, 0, 0)),
    ROCK("rock", "Rock", "Rock", listOf(450, 250, -100, 300, 500)),
    POP("pop", "Pop", "Pop", listOf(-150, 250, 450, 250, -100)),
    ELECTRONIC("electronic", "Electronic", "Electronic", listOf(550, 300, 0, 350, 600)),
    HIP_HOP("hiphop", "Hip-Hop", "Hip-Hop", listOf(600, 400, 50, 150, 300)),
    CLASSICAL("classical", "Classical", "Classical", listOf(350, 200, -50, 200, 400)),
    VOCAL("vocal", "Vocal", "Vocal", listOf(-250, 150, 550, 450, 150)),
    BASS_BOOST("bass", "Bass Boost", "Bass Boost", listOf(750, 500, 100, 0, 0)),
    BASS_TREBLE("bass_treble", "Bass & Treble", "Bass & Treble", listOf(650, 350, -100, 350, 700)),
    CUSTOM("custom", "Personalizado", "Custom", listOf(0, 0, 0, 0, 0));

    companion object {
        fun fromId(id: String): EqPreset = entries.find { it.id == id } ?: FLAT
    }
}

@Serializable
data class TrakasThemeConfig(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = true,
    val primaryArgb: Long,
    val secondaryArgb: Long,
    val backgroundArgb: Long,
    val buttonArgb: Long,
    val textArgb: Long,
    val cardArgb: Long = 0xFF141B2D,
    val progressArgb: Long = primaryArgb,
    val visualizerArgb: Long = primaryArgb,
    val cardAlpha: Float = 0.82f,
    val buttonShapeId: String = ButtonShapeStyle.ROUNDED.id,
    val effectIntensity: Float = 0.90f,
    val visualizerStyleId: String = VisualizerStyle.SPECTRUM.id,
    val animationsEnabled: Boolean = true
)

object BuiltInThemes {
    val trakasDark = TrakasThemeConfig(
        id = "trakas_dark",
        name = "Trakas Dark",
        primaryArgb = 0xFF00E5FF,
        secondaryArgb = 0xFF7C4DFF,
        backgroundArgb = 0xFF090D16,
        buttonArgb = 0xFF00E5FF,
        textArgb = 0xFFF3F7FF,
        cardArgb = 0xFF131A2C,
        progressArgb = 0xFF00E5FF,
        visualizerArgb = 0xFF00E5FF,
        cardAlpha = 0.84f,
        visualizerStyleId = VisualizerStyle.SPECTRUM.id
    )

    val neonBlue = TrakasThemeConfig(
        id = "neon_blue",
        name = "Neon Blue",
        primaryArgb = 0xFF2979FF,
        secondaryArgb = 0xFF00E5FF,
        backgroundArgb = 0xFF050B18,
        buttonArgb = 0xFF2979FF,
        textArgb = 0xFFECF4FF,
        cardArgb = 0xFF0D1730,
        progressArgb = 0xFF00E5FF,
        visualizerArgb = 0xFF2979FF,
        buttonShapeId = ButtonShapeStyle.PILL.id,
        visualizerStyleId = VisualizerStyle.BARS.id
    )

    val cyberPurple = TrakasThemeConfig(
        id = "cyber_purple",
        name = "Cyber Purple",
        primaryArgb = 0xFFB388FF,
        secondaryArgb = 0xFFFF4081,
        backgroundArgb = 0xFF0E0718,
        buttonArgb = 0xFF9C4DFF,
        textArgb = 0xFFF8F2FF,
        cardArgb = 0xFF1B102E,
        progressArgb = 0xFFFF4081,
        visualizerArgb = 0xFFB388FF,
        buttonShapeId = ButtonShapeStyle.CHAMFERED.id,
        visualizerStyleId = VisualizerStyle.CIRCLE.id
    )

    val electroGreen = TrakasThemeConfig(
        id = "electro_green",
        name = "Electro Green",
        primaryArgb = 0xFF00E676,
        secondaryArgb = 0xFFC6FF00,
        backgroundArgb = 0xFF06110B,
        buttonArgb = 0xFF00E676,
        textArgb = 0xFFECFFF3,
        cardArgb = 0xFF0E2116,
        progressArgb = 0xFF00E676,
        visualizerArgb = 0xFFC6FF00,
        buttonShapeId = ButtonShapeStyle.CHAMFERED.id,
        visualizerStyleId = VisualizerStyle.WAVEFORM.id
    )

    val synthwave = TrakasThemeConfig(
        id = "synthwave",
        name = "Synthwave",
        primaryArgb = 0xFFFF2E93,
        secondaryArgb = 0xFF00F0FF,
        backgroundArgb = 0xFF130921,
        buttonArgb = 0xFFFF2E93,
        textArgb = 0xFFFFF0FA,
        cardArgb = 0xFF221238,
        progressArgb = 0xFF00F0FF,
        visualizerArgb = 0xFFFF2E93,
        buttonShapeId = ButtonShapeStyle.PILL.id,
        visualizerStyleId = VisualizerStyle.PARTICLES.id
    )

    val amoledBlack = TrakasThemeConfig(
        id = "amoled_black",
        name = "AMOLED Black",
        primaryArgb = 0xFF00E5FF,
        secondaryArgb = 0xFFFF4081,
        backgroundArgb = 0xFF000000,
        buttonArgb = 0xFF00E5FF,
        textArgb = 0xFFFFFFFF,
        cardArgb = 0xFF0B0D12,
        progressArgb = 0xFF00E5FF,
        visualizerArgb = 0xFF00E5FF,
        cardAlpha = 0.94f,
        visualizerStyleId = VisualizerStyle.SPECTRUM.id
    )

    val matrix = TrakasThemeConfig(
        id = "matrix",
        name = "Matrix",
        primaryArgb = 0xFF00FF41,
        secondaryArgb = 0xFF008F11,
        backgroundArgb = 0xFF020A03,
        buttonArgb = 0xFF00FF41,
        textArgb = 0xFFD9FFE2,
        cardArgb = 0xFF071909,
        progressArgb = 0xFF00FF41,
        visualizerArgb = 0xFF00FF41,
        buttonShapeId = ButtonShapeStyle.CHAMFERED.id,
        visualizerStyleId = VisualizerStyle.BARS.id
    )

    val sunset = TrakasThemeConfig(
        id = "sunset",
        name = "Sunset",
        primaryArgb = 0xFFFF6D00,
        secondaryArgb = 0xFFFF4081,
        backgroundArgb = 0xFF140B12,
        buttonArgb = 0xFFFF6D00,
        textArgb = 0xFFFFF5EE,
        cardArgb = 0xFF24131E,
        progressArgb = 0xFFFF6D00,
        visualizerArgb = 0xFFFF4081,
        buttonShapeId = ButtonShapeStyle.PILL.id,
        visualizerStyleId = VisualizerStyle.WAVEFORM.id
    )

    val midnight = TrakasThemeConfig(
        id = "midnight",
        name = "Midnight",
        primaryArgb = 0xFF82B1FF,
        secondaryArgb = 0xFF1DE9B6,
        backgroundArgb = 0xFF071020,
        buttonArgb = 0xFF5386E4,
        textArgb = 0xFFEEF5FF,
        cardArgb = 0xFF101D36,
        progressArgb = 0xFF1DE9B6,
        visualizerArgb = 0xFF82B1FF,
        visualizerStyleId = VisualizerStyle.WAVEFORM.id
    )

    val classic = TrakasThemeConfig(
        id = "classic",
        name = "Classic",
        primaryArgb = 0xFFFFFFFF,
        secondaryArgb = 0xFFFF1744,
        backgroundArgb = 0xFF0A0A0D,
        buttonArgb = 0xFFFFFFFF,
        textArgb = 0xFFFFFFFF,
        cardArgb = 0xFF18181F,
        progressArgb = 0xFFFFFFFF,
        visualizerArgb = 0xFFFF1744,
        visualizerStyleId = VisualizerStyle.ALBUM_COLORS.id
    )

    val all: List<TrakasThemeConfig> = listOf(
        trakasDark,
        neonBlue,
        cyberPurple,
        electroGreen,
        synthwave,
        amoledBlack,
        matrix,
        sunset,
        midnight,
        classic
    )
}

@Serializable
data class BackupPlaylistEntry(
    val name: String,
    val songTitles: List<String>,
    val songIds: List<Long>
)

@Serializable
data class BackupSettingsPayload(
    val selectedThemeId: String,
    val iconStyleId: String,
    val visualizerStyleId: String,
    val audioQualityId: String,
    val gaplessPlayback: Boolean,
    val crossfadeSeconds: Int,
    val animationsEnabled: Boolean,
    val smartShuffleEnabled: Boolean,
    val normalizeVolume: Boolean,
    val replayGainEnabled: Boolean,
    val playbackSpeed: Float,
    val languageCode: String
)

@Serializable
data class TrakasBackupDocument(
    val version: Int = 1,
    val playlists: List<BackupPlaylistEntry> = emptyList(),
    val customThemes: List<TrakasThemeConfig> = emptyList(),
    val settings: BackupSettingsPayload? = null
)

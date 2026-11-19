package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.AudioQuality
import com.example.model.BuiltInThemes
import com.example.model.IconStyle
import com.example.model.InterfaceMode
import com.example.model.RepeatModeOption
import com.example.model.VisualizerStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.trakasDataStore: DataStore<Preferences> by preferencesDataStore(name = "trakas_prefs")

data class AppPreferences(
    val selectedThemeId: String = BuiltInThemes.trakasDark.id,
    val iconStyle: IconStyle = IconStyle.NEON,
    val interfaceMode: InterfaceMode = InterfaceMode.TRAKAS_PLAYER,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.SPECTRUM,
    val visualizerIntensity: Float = 0.9f,
    val visualizerSpeed: Float = 1.0f,
    val visualizerSensitivity: Float = 1.0f,
    val visualizerAlpha: Float = 0.9f,
    val visualizerParticleCount: Int = 36,
    val visualizerUseAlbumColors: Boolean = false,
    val audioQuality: AudioQuality = AudioQuality.HIGH_FIDELITY,
    val gaplessPlayback: Boolean = true,
    val crossfadeSeconds: Int = 3,
    val animationsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val lockscreenControlsEnabled: Boolean = true,
    val bluetoothAutoPlay: Boolean = false,
    val language: AppLanguage = AppLanguage.SPANISH,
    val excludedFolders: Set<String> = emptySet(),
    val shuffleEnabled: Boolean = false,
    val smartShuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val playerVolume: Float = 1.0f,
    val playbackSpeed: Float = 1.0f,
    val normalizeVolume: Boolean = true,
    val replayGainEnabled: Boolean = true,
    val lyricsKaraokeMode: Boolean = true,
    val lyricsFontSizeSp: Float = 20f,
    val lyricsFontKey: String = "space",
    val lyricsTextColorArgb: Long = 0xFFF3F7FF,
    val lyricsHighlightColorArgb: Long = 0xFF00E5FF,
    val lyricsDynamicBg: Boolean = true,
    val playerDynamicBackground: Boolean = true,
    val playerVisualizerEnabled: Boolean = true,
    val immersivePlayerMode: Boolean = false,
    val textScaleFactor: Float = 1.0f,
    val removedSongIds: Set<String> = emptySet()
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val ICON_STYLE = stringPreferencesKey("icon_style")
        val INTERFACE_MODE = stringPreferencesKey("interface_mode")
        val VISUALIZER_STYLE = stringPreferencesKey("visualizer_style")
        val VIS_INTENSITY = floatPreferencesKey("vis_intensity")
        val VIS_SPEED = floatPreferencesKey("vis_speed")
        val VIS_SENSITIVITY = floatPreferencesKey("vis_sensitivity")
        val VIS_ALPHA = floatPreferencesKey("vis_alpha")
        val VIS_PARTICLES = intPreferencesKey("vis_particles")
        val VIS_ALBUM_COLORS = booleanPreferencesKey("vis_album_colors")
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val GAPLESS = booleanPreferencesKey("gapless_playback")
        val CROSSFADE_SEC = intPreferencesKey("crossfade_seconds")
        val ANIMATIONS = booleanPreferencesKey("animations_enabled")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val LOCKSCREEN = booleanPreferencesKey("lockscreen_controls")
        val BT_AUTOPLAY = booleanPreferencesKey("bt_autoplay")
        val LANGUAGE = stringPreferencesKey("language")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
        val SHUFFLE = booleanPreferencesKey("shuffle_enabled")
        val SMART_SHUFFLE = booleanPreferencesKey("smart_shuffle_enabled")
        val REPEAT = stringPreferencesKey("repeat_mode")
        val VOLUME = floatPreferencesKey("player_volume")
        val SPEED = floatPreferencesKey("playback_speed")
        val NORMALIZE = booleanPreferencesKey("normalize_volume")
        val REPLAYGAIN = booleanPreferencesKey("replay_gain")
        val LYRICS_KARAOKE = booleanPreferencesKey("lyrics_karaoke")
        val LYRICS_SIZE = floatPreferencesKey("lyrics_size")
        val LYRICS_FONT = stringPreferencesKey("lyrics_font")
        val LYRICS_COLOR = longPreferencesKey("lyrics_color")
        val LYRICS_HIGHLIGHT = longPreferencesKey("lyrics_highlight")
        val LYRICS_DYN_BG = booleanPreferencesKey("lyrics_dyn_bg")
        val PLAYER_DYN_BG = booleanPreferencesKey("player_dyn_bg")
        val PLAYER_VIS_ENABLED = booleanPreferencesKey("player_vis_enabled")
        val IMMERSIVE_PLAYER = booleanPreferencesKey("immersive_player")
        val TEXT_SCALE_FACTOR = floatPreferencesKey("text_scale_factor")
        val REMOVED_SONG_IDS = stringSetPreferencesKey("removed_song_ids")
    }

    val preferencesFlow: Flow<AppPreferences> = context.trakasDataStore.data.map { prefs ->
        AppPreferences(
            selectedThemeId = prefs[Keys.THEME_ID] ?: BuiltInThemes.trakasDark.id,
            iconStyle = IconStyle.fromId(prefs[Keys.ICON_STYLE] ?: IconStyle.NEON.id),
            interfaceMode = InterfaceMode.fromId(prefs[Keys.INTERFACE_MODE] ?: InterfaceMode.TRAKAS_PLAYER.id),
            visualizerStyle = VisualizerStyle.fromId(prefs[Keys.VISUALIZER_STYLE] ?: VisualizerStyle.SPECTRUM.id),
            visualizerIntensity = prefs[Keys.VIS_INTENSITY] ?: 0.9f,
            visualizerSpeed = prefs[Keys.VIS_SPEED] ?: 1.0f,
            visualizerSensitivity = prefs[Keys.VIS_SENSITIVITY] ?: 1.0f,
            visualizerAlpha = prefs[Keys.VIS_ALPHA] ?: 0.9f,
            visualizerParticleCount = prefs[Keys.VIS_PARTICLES] ?: 36,
            visualizerUseAlbumColors = prefs[Keys.VIS_ALBUM_COLORS] ?: false,
            audioQuality = AudioQuality.fromId(prefs[Keys.AUDIO_QUALITY] ?: AudioQuality.HIGH_FIDELITY.id),
            gaplessPlayback = prefs[Keys.GAPLESS] ?: true,
            crossfadeSeconds = prefs[Keys.CROSSFADE_SEC] ?: 3,
            animationsEnabled = prefs[Keys.ANIMATIONS] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            lockscreenControlsEnabled = prefs[Keys.LOCKSCREEN] ?: true,
            bluetoothAutoPlay = prefs[Keys.BT_AUTOPLAY] ?: false,
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.SPANISH.code),
            excludedFolders = prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet(),
            shuffleEnabled = prefs[Keys.SHUFFLE] ?: false,
            smartShuffleEnabled = prefs[Keys.SMART_SHUFFLE] ?: false,
            repeatMode = runCatching {
                RepeatModeOption.valueOf(prefs[Keys.REPEAT] ?: RepeatModeOption.OFF.name)
            }.getOrDefault(RepeatModeOption.OFF),
            playerVolume = prefs[Keys.VOLUME] ?: 1.0f,
            playbackSpeed = prefs[Keys.SPEED] ?: 1.0f,
            normalizeVolume = prefs[Keys.NORMALIZE] ?: true,
            replayGainEnabled = prefs[Keys.REPLAYGAIN] ?: true,
            lyricsKaraokeMode = prefs[Keys.LYRICS_KARAOKE] ?: true,
            lyricsFontSizeSp = prefs[Keys.LYRICS_SIZE] ?: 20f,
            lyricsFontKey = prefs[Keys.LYRICS_FONT] ?: "space",
            lyricsTextColorArgb = prefs[Keys.LYRICS_COLOR] ?: 0xFFF3F7FF,
            lyricsHighlightColorArgb = prefs[Keys.LYRICS_HIGHLIGHT] ?: 0xFF00E5FF,
            lyricsDynamicBg = prefs[Keys.LYRICS_DYN_BG] ?: true,
            playerDynamicBackground = prefs[Keys.PLAYER_DYN_BG] ?: true,
            playerVisualizerEnabled = prefs[Keys.PLAYER_VIS_ENABLED] ?: true,
            immersivePlayerMode = prefs[Keys.IMMERSIVE_PLAYER] ?: false,
            textScaleFactor = prefs[Keys.TEXT_SCALE_FACTOR] ?: 1.0f,
            removedSongIds = prefs[Keys.REMOVED_SONG_IDS] ?: emptySet()
        )
    }

    suspend fun setThemeId(id: String) = context.trakasDataStore.edit { it[Keys.THEME_ID] = id }
    suspend fun setIconStyle(style: IconStyle) = context.trakasDataStore.edit { it[Keys.ICON_STYLE] = style.id }
    suspend fun setInterfaceMode(mode: InterfaceMode) = context.trakasDataStore.edit { it[Keys.INTERFACE_MODE] = mode.id }
    suspend fun setVisualizerStyle(style: VisualizerStyle) = context.trakasDataStore.edit { it[Keys.VISUALIZER_STYLE] = style.id }
    suspend fun setVisualizerIntensity(v: Float) = context.trakasDataStore.edit { it[Keys.VIS_INTENSITY] = v.coerceIn(0.2f, 1.5f) }
    suspend fun setVisualizerSpeed(v: Float) = context.trakasDataStore.edit { it[Keys.VIS_SPEED] = v.coerceIn(0.4f, 2.0f) }
    suspend fun setVisualizerSensitivity(v: Float) = context.trakasDataStore.edit { it[Keys.VIS_SENSITIVITY] = v.coerceIn(0.4f, 2.0f) }
    suspend fun setVisualizerAlpha(v: Float) = context.trakasDataStore.edit { it[Keys.VIS_ALPHA] = v.coerceIn(0.2f, 1.0f) }
    suspend fun setVisualizerParticleCount(c: Int) = context.trakasDataStore.edit { it[Keys.VIS_PARTICLES] = c.coerceIn(10, 80) }
    suspend fun setVisualizerUseAlbumColors(b: Boolean) = context.trakasDataStore.edit { it[Keys.VIS_ALBUM_COLORS] = b }
    suspend fun setAudioQuality(quality: AudioQuality) = context.trakasDataStore.edit { it[Keys.AUDIO_QUALITY] = quality.id }
    suspend fun setGaplessPlayback(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.GAPLESS] = enabled }
    suspend fun setCrossfadeSeconds(seconds: Int) = context.trakasDataStore.edit { it[Keys.CROSSFADE_SEC] = seconds.coerceIn(0, 12) }
    suspend fun setAnimationsEnabled(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.ANIMATIONS] = enabled }
    suspend fun setNotificationsEnabled(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    suspend fun setLockscreenControlsEnabled(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.LOCKSCREEN] = enabled }
    suspend fun setBluetoothAutoPlay(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.BT_AUTOPLAY] = enabled }
    suspend fun setLanguage(language: AppLanguage) = context.trakasDataStore.edit { it[Keys.LANGUAGE] = language.code }
    suspend fun toggleFolderExcluded(folderPath: String) = context.trakasDataStore.edit { prefs ->
        val current = prefs[Keys.EXCLUDED_FOLDERS]?.toMutableSet() ?: mutableSetOf()
        if (current.contains(folderPath)) current.remove(folderPath) else current.add(folderPath)
        prefs[Keys.EXCLUDED_FOLDERS] = current
    }
    suspend fun setShuffleEnabled(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.SHUFFLE] = enabled }
    suspend fun setSmartShuffleEnabled(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.SMART_SHUFFLE] = enabled }
    suspend fun setRepeatMode(mode: RepeatModeOption) = context.trakasDataStore.edit { it[Keys.REPEAT] = mode.name }
    suspend fun setPlayerVolume(volume: Float) = context.trakasDataStore.edit { it[Keys.VOLUME] = volume.coerceIn(0f, 1f) }
    suspend fun setPlaybackSpeed(speed: Float) = context.trakasDataStore.edit { it[Keys.SPEED] = speed.coerceIn(0.5f, 2.0f) }
    suspend fun setNormalizeVolume(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.NORMALIZE] = enabled }
    suspend fun setReplayGainEnabled(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.REPLAYGAIN] = enabled }
    suspend fun setLyricsKaraokeMode(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.LYRICS_KARAOKE] = enabled }
    suspend fun setLyricsFontSize(sp: Float) = context.trakasDataStore.edit { it[Keys.LYRICS_SIZE] = sp.coerceIn(14f, 30f) }
    suspend fun setLyricsFontKey(key: String) = context.trakasDataStore.edit { it[Keys.LYRICS_FONT] = key }
    suspend fun setLyricsTextColor(argb: Long) = context.trakasDataStore.edit { it[Keys.LYRICS_COLOR] = argb }
    suspend fun setLyricsHighlightColor(argb: Long) = context.trakasDataStore.edit { it[Keys.LYRICS_HIGHLIGHT] = argb }
    suspend fun setLyricsDynamicBg(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.LYRICS_DYN_BG] = enabled }
    suspend fun setPlayerDynamicBackground(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.PLAYER_DYN_BG] = enabled }
    suspend fun setPlayerVisualizerEnabled(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.PLAYER_VIS_ENABLED] = enabled }
    suspend fun setImmersivePlayerMode(enabled: Boolean) = context.trakasDataStore.edit { it[Keys.IMMERSIVE_PLAYER] = enabled }
    suspend fun setTextScaleFactor(scale: Float) = context.trakasDataStore.edit { it[Keys.TEXT_SCALE_FACTOR] = scale.coerceIn(0.85f, 1.35f) }
    suspend fun markSongRemoved(songId: Long) = context.trakasDataStore.edit { prefs ->
        val set = prefs[Keys.REMOVED_SONG_IDS]?.toMutableSet() ?: mutableSetOf()
        set.add(songId.toString())
        prefs[Keys.REMOVED_SONG_IDS] = set
    }
}

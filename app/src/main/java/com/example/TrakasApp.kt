package com.example

import android.app.Application
import com.example.data.local.SettingsDataStore
import com.example.data.local.TrakasDatabase
import com.example.data.repository.MusicRepository
import com.example.data.repository.TrakasAiRepository
import com.example.player.AudioEffectsController
import com.example.player.BluetoothAudioMonitor
import com.example.player.PlaybackController
import com.example.player.TrakasAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrakasApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: TrakasDatabase
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var musicRepository: MusicRepository
        private set
    lateinit var trakasAiRepository: TrakasAiRepository
        private set
    lateinit var mainAudioProcessor: TrakasAudioProcessor
        private set
    lateinit var audioEffectsController: AudioEffectsController
        private set
    lateinit var playbackController: PlaybackController
        private set
    lateinit var bluetoothMonitor: BluetoothAudioMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        database = TrakasDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        musicRepository = MusicRepository(this, database.trakasDao())
        trakasAiRepository = TrakasAiRepository()
        mainAudioProcessor = TrakasAudioProcessor()
        audioEffectsController = AudioEffectsController(mainAudioProcessor)
        playbackController = PlaybackController(
            context = this,
            audioProcessor = mainAudioProcessor,
            onTrackStarted = { songId ->
                appScope.launch { musicRepository.recordPlay(songId) }
            },
            onListeningSession = { songId, durationMs ->
                appScope.launch { musicRepository.recordListeningSession(songId, durationMs) }
            }
        )
        bluetoothMonitor = BluetoothAudioMonitor(this) {
            appScope.launch {
                val prefs = settingsDataStore.preferencesFlow.first()
                if (prefs.bluetoothAutoPlay) {
                    appScope.launch(Dispatchers.Main) {
                        playbackController.autoPlayOnBluetoothIfIdle()
                    }
                }
            }
        }
        bluetoothMonitor.start()
    }
}

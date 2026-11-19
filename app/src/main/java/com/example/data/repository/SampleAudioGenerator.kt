package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.ImportedTrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SampleAudioGenerator {

    private data class TrackSpec(
        val id: Long,
        val fileName: String,
        val title: String,
        val artist: String,
        val album: String,
        val genre: String,
        val year: Int,
        val pulseTempo: Float,
        val rootHz: Double,
        val durationSec: Int
    )

    private val specs = listOf(
        TrackSpec(
            id = -1001L,
            fileName = "neon_horizon_audio.wav",
            title = "Neon Horizon",
            artist = "Trakas Synth Lab",
            album = "Sonic Pulse Sessions",
            genre = "Synthwave",
            year = 2025,
            pulseTempo = 128f,
            rootHz = 55.0,
            durationSec = 24
        ),
        TrackSpec(
            id = -1002L,
            fileName = "cyber_bassline_audio.wav",
            title = "Cyber Bassline",
            artist = "Kinetix & Trakas",
            album = "Sonic Pulse Sessions",
            genre = "Electronic",
            year = 2025,
            pulseTempo = 140f,
            rootHz = 65.41,
            durationSec = 24
        ),
        TrackSpec(
            id = -1003L,
            fileName = "midnight_velvet_audio.wav",
            title = "Midnight Velvet",
            artist = "Vortex Audio",
            album = "Late Night Sessions",
            genre = "Chillout",
            year = 2024,
            pulseTempo = 112f,
            rootHz = 49.0,
            durationSec = 24
        ),
        TrackSpec(
            id = -1004L,
            fileName = "sunset_echo_audio.wav",
            title = "Sunset Echo",
            artist = "Solar Groove",
            album = "Late Night Sessions",
            genre = "Ambient",
            year = 2024,
            pulseTempo = 96f,
            rootHz = 73.42,
            durationSec = 24
        )
    )

    suspend fun generateLocalWavTracks(context: Context): List<ImportedTrackEntity> =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "trakas_samples").apply { mkdirs() }
            val sampleRate = 44100
            specs.map { spec ->
                val outFile = File(dir, spec.fileName)
                if (!outFile.exists() || outFile.length() < 1024) {
                    writeSynthWav(
                        file = outFile,
                        sampleRate = sampleRate,
                        durationSec = spec.durationSec,
                        tempo = spec.pulseTempo.toDouble(),
                        rootHz = spec.rootHz
                    )
                }
                ImportedTrackEntity(
                    id = spec.id,
                    uriString = Uri.fromFile(outFile).toString(),
                    title = spec.title,
                    artist = spec.artist,
                    album = spec.album,
                    genre = spec.genre,
                    year = spec.year,
                    durationMs = spec.durationSec * 1000L,
                    format = "WAV",
                    folderName = "Trakas Samples"
                )
            }
        }

    private fun writeSynthWav(
        file: File,
        sampleRate: Int,
        durationSec: Int,
        tempo: Double,
        rootHz: Double
    ) {
        val numSamples = sampleRate * durationSec
        val numChannels = 2
        val bytesPerSample = 2
        val dataSize = numSamples * numChannels * bytesPerSample
        val stepDuration = 60.0 / tempo
        val notes = doubleArrayOf(1.0, 1.1892, 1.3348, 1.4983)

        BufferedOutputStream(FileOutputStream(file), 32768).use { out ->
            fun writeStr(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
            fun writeIntLE(v: Int) {
                out.write(v and 0xFF)
                out.write((v shr 8) and 0xFF)
                out.write((v shr 16) and 0xFF)
                out.write((v shr 24) and 0xFF)
            }
            fun writeShortLE(v: Int) {
                out.write(v and 0xFF)
                out.write((v shr 8) and 0xFF)
            }

            writeStr("RIFF")
            writeIntLE(36 + dataSize)
            writeStr("WAVE")
            writeStr("fmt ")
            writeIntLE(16)
            writeShortLE(1)
            writeShortLE(numChannels)
            writeIntLE(sampleRate)
            writeIntLE(sampleRate * numChannels * bytesPerSample)
            writeShortLE(numChannels * bytesPerSample)
            writeShortLE(16)
            writeStr("data")
            writeIntLE(dataSize)

            val buffer = ByteArray(4096)
            var bufIdx = 0

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val pulsePos = (t % stepDuration) / stepDuration
                val step = ((t / (stepDuration * 0.5)).toInt()) % notes.size

                val lowEnv = exp(-pulsePos * 12.0)
                val lowFreq = 45.0 + 115.0 * exp(-pulsePos * 26.0)
                val kick = sin(2.0 * PI * lowFreq * (t % stepDuration)) * lowEnv * 0.55

                val bassFreq = rootHz * notes[step]
                val bassEnv = exp(-((t % (stepDuration * 0.5)) / (stepDuration * 0.5)) * 4.0)
                val bass = (sin(2.0 * PI * bassFreq * t) * 0.6 +
                    sin(2.0 * PI * bassFreq * 2.0 * t) * 0.25) * bassEnv * 0.35

                val arpFreq = rootHz * 4.0 * notes[(step + 1) % notes.size]
                val arpEnv = exp(-((t % (stepDuration * 0.25)) / (stepDuration * 0.25)) * 5.0)
                val arpL = sin(2.0 * PI * arpFreq * t) * arpEnv * 0.22
                val arpR = sin(2.0 * PI * (arpFreq * 1.005) * t + 0.6) * arpEnv * 0.22

                val shimmerPhase = ((t + stepDuration * 0.5) % stepDuration) / stepDuration
                val shimmerEnv = exp(-shimmerPhase * 35.0)
                val shimmer = sin(t * 17321.0) * sin(t * 9137.0) * shimmerEnv * 0.14

                val left = ((kick + bass + arpL + shimmer).coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt()
                val right = ((kick + bass + arpR - shimmer * 0.7).coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt()

                buffer[bufIdx++] = (left and 0xFF).toByte()
                buffer[bufIdx++] = ((left shr 8) and 0xFF).toByte()
                buffer[bufIdx++] = (right and 0xFF).toByte()
                buffer[bufIdx++] = ((right shr 8) and 0xFF).toByte()

                if (bufIdx >= buffer.size) {
                    out.write(buffer, 0, bufIdx)
                    bufIdx = 0
                }
            }
            if (bufIdx > 0) {
                out.write(buffer, 0, bufIdx)
            }
        }
    }
}

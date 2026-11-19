package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.example.data.local.EqSettingsEntity
import com.example.model.EqPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqBandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int,
    val minMb: Int,
    val maxMb: Int
)

data class EqUiState(
    val enabled: Boolean = true,
    val preset: EqPreset = EqPreset.FLAT,
    val bands: List<EqBandInfo> = defaultBands(),
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerStrength: Int = 0, // 0..1000
    val balance: Float = 0f, // -1f..1f
    val bassDb: Float = 0f, // -12f..12f
    val trebleDb: Float = 0f, // -12f..12f
    val minBandMb: Int = -1500,
    val maxBandMb: Int = 1500
) {
    companion object {
        fun defaultBands() = listOf(
            EqBandInfo(0, 60, 0, -1500, 1500),
            EqBandInfo(1, 230, 0, -1500, 1500),
            EqBandInfo(2, 910, 0, -1500, 1500),
            EqBandInfo(3, 3600, 0, -1500, 1500),
            EqBandInfo(4, 14000, 0, -1500, 1500)
        )
    }
}

class AudioEffectsController(
    private val audioProcessor: TrakasAudioProcessor
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var attachedSessionId: Int = 0

    private val _state = MutableStateFlow(EqUiState())
    val state: StateFlow<EqUiState> = _state.asStateFlow()

    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == attachedSessionId) return
        release()
        attachedSessionId = audioSessionId

        runCatching {
            val eq = Equalizer(0, audioSessionId)
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            val numBands = eq.numberOfBands.toInt().coerceAtLeast(5)
            val current = _state.value
            val updatedBands = (0 until numBands.coerceAtMost(5)).map { idx ->
                val freq = eq.getCenterFreq(idx.toShort()) / 1000
                val existingLevel = current.bands.getOrNull(idx)?.levelMb ?: 0
                val clamped = existingLevel.coerceIn(minMb, maxMb)
                eq.setBandLevel(idx.toShort(), clamped.toShort())
                EqBandInfo(idx, freq, clamped, minMb, maxMb)
            }
            eq.enabled = current.enabled
            equalizer = eq
            _state.value = current.copy(bands = updatedBands, minBandMb = minMb, maxBandMb = maxMb)
        }

        runCatching {
            val bb = BassBoost(0, audioSessionId)
            bb.setStrength(_state.value.bassBoostStrength.toShort())
            bb.enabled = _state.value.enabled && _state.value.bassBoostStrength > 0
            bassBoost = bb
        }

        runCatching {
            val virt = Virtualizer(0, audioSessionId)
            virt.setStrength(_state.value.virtualizerStrength.toShort())
            virt.enabled = _state.value.enabled && _state.value.virtualizerStrength > 0
            virtualizer = virt
        }
    }

    fun applyPersisted(entity: EqSettingsEntity) {
        val bands = listOf(entity.band0Mb, entity.band1Mb, entity.band2Mb, entity.band3Mb, entity.band4Mb)
        val current = _state.value
        val updatedBands = current.bands.mapIndexed { idx, info ->
            val level = bands.getOrElse(idx) { 0 }.coerceIn(current.minBandMb, current.maxBandMb)
            runCatching { equalizer?.setBandLevel(idx.toShort(), level.toShort()) }
            info.copy(levelMb = level)
        }
        audioProcessor.balance = entity.balance
        audioProcessor.bassDb = if (entity.enabled) entity.bassDb else 0f
        audioProcessor.trebleDb = if (entity.enabled) entity.trebleDb else 0f
        runCatching {
            equalizer?.enabled = entity.enabled
            bassBoost?.setStrength(entity.bassBoostStrength.toShort())
            bassBoost?.enabled = entity.enabled && entity.bassBoostStrength > 0
            virtualizer?.setStrength(entity.virtualizerStrength.toShort())
            virtualizer?.enabled = entity.enabled && entity.virtualizerStrength > 0
        }
        _state.value = current.copy(
            enabled = entity.enabled,
            preset = EqPreset.fromId(entity.presetId),
            bands = updatedBands,
            bassBoostStrength = entity.bassBoostStrength,
            virtualizerStrength = entity.virtualizerStrength,
            balance = entity.balance,
            bassDb = entity.bassDb,
            trebleDb = entity.trebleDb
        )
    }

    fun setEnabled(enabled: Boolean): EqSettingsEntity {
        audioProcessor.bassDb = if (enabled) _state.value.bassDb else 0f
        audioProcessor.trebleDb = if (enabled) _state.value.trebleDb else 0f
        runCatching {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled && _state.value.bassBoostStrength > 0
            virtualizer?.enabled = enabled && _state.value.virtualizerStrength > 0
        }
        _state.value = _state.value.copy(enabled = enabled)
        return toEntity()
    }

    fun selectPreset(preset: EqPreset): EqSettingsEntity {
        val current = _state.value
        val updatedBands = current.bands.mapIndexed { idx, info ->
            val targetMb = if (preset == EqPreset.CUSTOM) {
                info.levelMb
            } else {
                preset.bandsMb.getOrElse(idx) { 0 }.coerceIn(current.minBandMb, current.maxBandMb)
            }
            runCatching { equalizer?.setBandLevel(idx.toShort(), targetMb.toShort()) }
            info.copy(levelMb = targetMb)
        }
        _state.value = current.copy(preset = preset, bands = updatedBands)
        return toEntity()
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int): EqSettingsEntity {
        val current = _state.value
        val clamped = levelMb.coerceIn(current.minBandMb, current.maxBandMb)
        runCatching { equalizer?.setBandLevel(bandIndex.toShort(), clamped.toShort()) }
        val updatedBands = current.bands.map {
            if (it.index == bandIndex) it.copy(levelMb = clamped) else it
        }
        _state.value = current.copy(preset = EqPreset.CUSTOM, bands = updatedBands)
        return toEntity()
    }

    fun setBassBoost(strength: Int): EqSettingsEntity {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            bassBoost?.setStrength(clamped.toShort())
            bassBoost?.enabled = _state.value.enabled && clamped > 0
        }
        _state.value = _state.value.copy(bassBoostStrength = clamped)
        return toEntity()
    }

    fun setVirtualizer(strength: Int): EqSettingsEntity {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            virtualizer?.setStrength(clamped.toShort())
            virtualizer?.enabled = _state.value.enabled && clamped > 0
        }
        _state.value = _state.value.copy(virtualizerStrength = clamped)
        return toEntity()
    }

    fun setBalance(balance: Float): EqSettingsEntity {
        val clamped = balance.coerceIn(-1f, 1f)
        audioProcessor.balance = clamped
        _state.value = _state.value.copy(balance = clamped)
        return toEntity()
    }

    fun setBassTone(bassDb: Float): EqSettingsEntity {
        val clamped = bassDb.coerceIn(-12f, 12f)
        audioProcessor.bassDb = if (_state.value.enabled) clamped else 0f
        _state.value = _state.value.copy(bassDb = clamped)
        return toEntity()
    }

    fun setTrebleTone(trebleDb: Float): EqSettingsEntity {
        val clamped = trebleDb.coerceIn(-12f, 12f)
        audioProcessor.trebleDb = if (_state.value.enabled) clamped else 0f
        _state.value = _state.value.copy(trebleDb = clamped)
        return toEntity()
    }

    fun toEntity(): EqSettingsEntity {
        val s = _state.value
        return EqSettingsEntity(
            id = 1,
            enabled = s.enabled,
            presetId = s.preset.id,
            band0Mb = s.bands.getOrNull(0)?.levelMb ?: 0,
            band1Mb = s.bands.getOrNull(1)?.levelMb ?: 0,
            band2Mb = s.bands.getOrNull(2)?.levelMb ?: 0,
            band3Mb = s.bands.getOrNull(3)?.levelMb ?: 0,
            band4Mb = s.bands.getOrNull(4)?.levelMb ?: 0,
            bassBoostStrength = s.bassBoostStrength,
            virtualizerStrength = s.virtualizerStrength,
            balance = s.balance,
            bassDb = s.bassDb,
            trebleDb = s.trebleDb
        )
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        attachedSessionId = 0
    }
}

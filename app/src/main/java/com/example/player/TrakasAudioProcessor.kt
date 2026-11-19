package com.example.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class VisualizerFrame(
    val bars: FloatArray = FloatArray(32) { 0.08f },
    val waveform: FloatArray = FloatArray(48) { 0f },
    val peakLevel: Float = 0f
)

object VisualizerStateHolder {
    private val _frame = MutableStateFlow(VisualizerFrame())
    val frame: StateFlow<VisualizerFrame> = _frame.asStateFlow()

    @Volatile
    var isObserving: Boolean = true

    fun update(bars: FloatArray, waveform: FloatArray, peak: Float) {
        _frame.value = VisualizerFrame(bars, waveform, peak)
    }

    fun reset() {
        _frame.value = VisualizerFrame()
    }
}

/**
 * Real-time Media3 AudioProcessor providing:
 * 1) Left/Right Stereo Balance (-1f .. +1f)
 * 2) Bass & Treble shelving tone controls (-12dB .. +12dB)
 * 3) Soft Volume Normalization & ReplayGain scaling
 * 4) Permission-free real-time PCM audio metering for the 6 visualizer styles
 */
@OptIn(UnstableApi::class)
class TrakasAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var balance: Float = 0f

    @Volatile
    var bassDb: Float = 0f

    @Volatile
    var trebleDb: Float = 0f

    @Volatile
    var normalizationGain: Float = 1.0f

    @Volatile
    var visualizerSensitivity: Float = 1.0f

    private var lpL = 0f
    private var lpR = 0f
    private var hpL = 0f
    private var hpR = 0f
    private var prevInL = 0f
    private var prevInR = 0f
    private var alphaBass = 0.03f
    private var alphaTreble = 0.45f
    private var envelopePeak = 0.35f

    private var lastEmitNs = 0L
    private val smoothedBars = FloatArray(32)
    private val prevWaveform = FloatArray(48)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        val sr = inputAudioFormat.sampleRate.coerceAtLeast(8000).toFloat()
        val dt = 1f / sr
        val rcBass = 1f / (2f * PI.toFloat() * 180f)
        alphaBass = (dt / (rcBass + dt)).coerceIn(0.005f, 0.4f)
        val rcTreble = 1f / (2f * PI.toFloat() * 4000f)
        alphaTreble = (rcTreble / (rcTreble + dt)).coerceIn(0.1f, 0.92f)
        return inputAudioFormat
    }

    override fun onFlush() {
        lpL = 0f; lpR = 0f
        hpL = 0f; hpR = 0f
        prevInL = 0f; prevInR = 0f
        envelopePeak = 0.35f
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outBuffer = replaceOutputBuffer(remaining)
        outBuffer.order(ByteOrder.LITTLE_ENDIAN)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val bal = balance.coerceIn(-1f, 1f)
        val leftGain = if (bal > 0f) 1f - bal else 1f
        val rightGain = if (bal < 0f) 1f + bal else 1f
        val bDb = bassDb.coerceIn(-12f, 12f)
        val tDb = trebleDb.coerceIn(-12f, 12f)
        val bassDelta = if (bDb == 0f) 0f else (10.0.pow(bDb / 20.0).toFloat() - 1f)
        val trebleDelta = if (tDb == 0f) 0f else (10.0.pow(tDb / 20.0).toFloat() - 1f)
        val normGain = normalizationGain.coerceIn(0.25f, 2.2f)
        val hasDsp = bal != 0f || bassDelta != 0f || trebleDelta != 0f || normGain != 1.0f

        val shouldSample = VisualizerStateHolder.isObserving &&
            (System.nanoTime() - lastEmitNs >= 30_000_000L)

        var sampleCount = 0
        val waveAcc = if (shouldSample) FloatArray(48) else null
        val waveCounts = if (shouldSample) IntArray(48) else null
        var peak = 0f

        val totalShorts = remaining / 2
        val samplesPerBin = (totalShorts / channels).coerceAtLeast(1)

        if (channels == 2 && (hasDsp || shouldSample)) {
            val frames = totalShorts / 2
            for (f in 0 until frames) {
                var xL = inputBuffer.short.toFloat()
                var xR = inputBuffer.short.toFloat()

                if (bassDelta != 0f || trebleDelta != 0f) {
                    lpL += alphaBass * (xL - lpL)
                    lpR += alphaBass * (xR - lpR)
                    hpL = alphaTreble * (hpL + xL - prevInL)
                    hpR = alphaTreble * (hpR + xR - prevInR)
                    prevInL = xL
                    prevInR = xR
                    xL += lpL * bassDelta + hpL * trebleDelta
                    xR += lpR * bassDelta + hpR * trebleDelta
                }

                // Soft peak limiter when boost/normalization is active
                val rawPeak = maxOf(abs(xL), abs(xR)) / 32768f
                envelopePeak = if (rawPeak > envelopePeak) {
                    envelopePeak * 0.9f + rawPeak * 0.1f
                } else {
                    envelopePeak * 0.9995f + rawPeak * 0.0005f
                }
                val limiter = if (envelopePeak * normGain > 0.92f) {
                    0.92f / (envelopePeak * normGain).coerceAtLeast(0.01f)
                } else 1f
                val totalGain = normGain * limiter

                val adjL = (xL * leftGain * totalGain).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                val adjR = (xR * rightGain * totalGain).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                outBuffer.putShort(adjL)
                outBuffer.putShort(adjR)

                if (shouldSample && waveAcc != null && waveCounts != null) {
                    val mono = ((adjL + adjR) * 0.5f) / 32768f
                    val a = abs(mono)
                    if (a > peak) peak = a
                    val bin = ((f * 48) / frames.coerceAtLeast(1)).coerceIn(0, 47)
                    waveAcc[bin] += mono
                    waveCounts[bin]++
                }
                sampleCount++
            }
            while (inputBuffer.hasRemaining()) {
                outBuffer.put(inputBuffer.get())
            }
        } else {
            val dup = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            if (normGain == 1.0f) {
                outBuffer.put(inputBuffer)
            } else {
                while (inputBuffer.remaining() >= 2) {
                    val x = (inputBuffer.short * normGain).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    outBuffer.putShort(x)
                }
                while (inputBuffer.hasRemaining()) {
                    outBuffer.put(inputBuffer.get())
                }
            }
            if (shouldSample && waveAcc != null && waveCounts != null) {
                var idx = 0
                while (dup.remaining() >= 2) {
                    val s = (dup.short * normGain) / 32768f
                    val a = abs(s)
                    if (a > peak) peak = a
                    val bin = ((idx * 48) / totalShorts.coerceAtLeast(1)).coerceIn(0, 47)
                    waveAcc[bin] += s
                    waveCounts[bin]++
                    idx++
                }
            }
        }

        if (shouldSample && waveAcc != null && waveCounts != null && samplesPerBin > 0) {
            lastEmitNs = System.nanoTime()
            val sens = visualizerSensitivity.coerceIn(0.4f, 2.0f)
            for (i in 0 until 48) {
                val v = if (waveCounts[i] > 0) (waveAcc[i] / waveCounts[i]) * sens else 0f
                prevWaveform[i] = (prevWaveform[i] * 0.35f + v * 0.65f).coerceIn(-1f, 1f)
            }
            for (b in 0 until 32) {
                val freqFactor = (b + 1) * 0.28f
                var real = 0f
                var imag = 0f
                for (n in 0 until 48) {
                    val angle = 2f * PI.toFloat() * freqFactor * n / 48f
                    real += prevWaveform[n] * cos(angle)
                    imag += prevWaveform[n] * sin(angle)
                }
                val mag = ((sqrt(real * real + imag * imag) / 6.5f) * sens).coerceIn(0.05f, 1.0f)
                smoothedBars[b] = if (mag > smoothedBars[b]) {
                    smoothedBars[b] * 0.25f + mag * 0.75f
                } else {
                    smoothedBars[b] * 0.72f + mag * 0.28f
                }
            }
            VisualizerStateHolder.update(
                bars = smoothedBars.copyOf(),
                waveform = prevWaveform.copyOf(),
                peak = (peak * sens).coerceIn(0f, 1f)
            )
        }

        outBuffer.flip()
    }
}

package com.example.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioOutputStatus(
    val deviceName: String = "Altavoz del teléfono",
    val isBluetooth: Boolean = false,
    val isWiredHeadset: Boolean = false
)

class BluetoothAudioMonitor(
    private val context: Context,
    private val onBluetoothAudioConnected: () -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _status = MutableStateFlow(AudioOutputStatus())
    val status: StateFlow<AudioOutputStatus> = _status.asStateFlow()

    private var registered = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            val btAdded = addedDevices.any { isBluetoothAudioType(it.type) }
            refreshCurrentDevice()
            if (btAdded) {
                onBluetoothAudioConnected()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            refreshCurrentDevice()
        }
    }

    fun start() {
        refreshCurrentDevice()
        if (!registered) {
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
            registered = true
        }
    }

    fun stop() {
        if (registered) {
            runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
            registered = false
        }
    }

    fun refreshCurrentDevice() {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val btDevice = outputs.firstOrNull { isBluetoothAudioType(it.type) }
        val wiredDevice = outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE
        }
        val active = btDevice ?: wiredDevice
        val productName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            active?.productName?.toString()?.takeIf { it.isNotBlank() }
        } else null

        _status.value = when {
            btDevice != null -> AudioOutputStatus(
                deviceName = productName ?: "Audio Bluetooth",
                isBluetooth = true,
                isWiredHeadset = false
            )
            wiredDevice != null -> AudioOutputStatus(
                deviceName = productName ?: "Auriculares con cable",
                isBluetooth = false,
                isWiredHeadset = true
            )
            else -> AudioOutputStatus(
                deviceName = "Altavoz del teléfono",
                isBluetooth = false,
                isWiredHeadset = false
            )
        }
    }

    private fun isBluetoothAudioType(type: Int): Boolean {
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
    }
}

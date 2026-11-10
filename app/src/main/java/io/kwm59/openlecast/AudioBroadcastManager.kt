/*
 * OpenLE-Cast
 * Copyright (C) 2026 Kenneth William Mayle — https://5pm.io / https://kwm59.io
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See LICENSE and NOTICE.
 */

package io.kwm59.openlecast

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothLeAudio
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import timber.log.Timber
import kotlin.random.Random

/**
 * Manages an LE Audio broadcast using one of three paths. See docs/BROADCAST-PATHS.md.
 *
 *  SYSTEM_SHARE  — Hand off to the phone's built-in Auracast / "Audio sharing"
 *                  feature. Works on stock phones that ship it. The app plays the
 *                  audio; the OS does the broadcasting and shows its own QR code.
 *  EXTERNAL_TX   — Play into an external Auracast transmitter (USB-C dongle,
 *                  3.5 mm or Bluetooth-in box). Works on any Android phone.
 *  PRIVILEGED    — Drive the system BluetoothLeBroadcast API directly. Only works
 *                  when the app is installed as a privileged/system app (custom
 *                  ROM or rooted device with BLUETOOTH_PRIVILEGED granted).
 */
class AudioBroadcastManager(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {

    enum class Mode { SYSTEM_SHARE, EXTERNAL_TX, PRIVILEGED }

    data class BroadcastInfo(
        val mode: Mode,
        val broadcastId: Int,
        val broadcastName: String,
        val passkey: String,
        val codec: String = "LC3",
        val sampleRate: Int = 48000,
        val bitrate: Int = 96000
    )

    /** Called whenever the detected output route changes. */
    var onStatusChanged: ((String) -> Unit)? = null

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var leAudioProxy: BluetoothLeAudio? = null
    private var currentBroadcast: BroadcastInfo? = null
    private val privileged = PrivilegedBroadcaster(context, bluetoothAdapter)

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) = report()
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) = report()
    }

    init {
        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.LE_AUDIO) {
                    leAudioProxy = proxy as? BluetoothLeAudio
                    Timber.d("LE Audio proxy connected")
                }
            }
            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.LE_AUDIO) leAudioProxy = null
            }
        }, BluetoothProfile.LE_AUDIO)
        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
    }

    // ---------- capability checks ----------

    /** True if the phone reports LE Audio broadcast-source support. */
    fun isBroadcastSourceSupported(): Boolean =
        bluetoothAdapter?.isLeAudioBroadcastSourceSupported() == android.bluetooth.BluetoothStatusCodes.FEATURE_SUPPORTED

    fun isPrivilegedAvailable(): Boolean = privileged.isAvailable()

    /** Output devices that look like the OS broadcast sink (path A). */
    fun systemBroadcastActive(): Boolean =
        outputs().any { it.type == AudioDeviceInfo.TYPE_BLE_BROADCAST }

    /** Output devices an external transmitter would show up as (path B). */
    fun externalTransmitterOutput(): AudioDeviceInfo? = outputs().firstOrNull {
        it.type in setOf(
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        )
    }

    private fun outputs() = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()

    // ---------- start / stop ----------

    fun startBroadcast(mode: Mode, broadcastName: String, withPasskey: Boolean = false): BroadcastInfo? {
        currentBroadcast?.let {
            Timber.w("Broadcast already in progress")
            return it
        }
        val info = BroadcastInfo(
            mode = mode,
            broadcastId = Random.nextInt(1, 0xFFFFFF),
            broadcastName = broadcastName,
            passkey = if (withPasskey) generatePasskey() else ""
        )
        val ok = when (mode) {
            Mode.SYSTEM_SHARE -> {
                // The OS owns the broadcast; send the user to its sharing screen.
                openSystemAudioSharing()
                true
            }
            Mode.EXTERNAL_TX -> {
                val out = externalTransmitterOutput()
                if (out == null) Timber.w("No external transmitter output detected yet")
                true
            }
            Mode.PRIVILEGED -> privileged.start(info.broadcastName, info.passkey)
        }
        if (!ok) return null
        currentBroadcast = info
        Timber.i("Broadcast requested: $info")
        report()
        return info
    }

    fun stopBroadcast() {
        val b = currentBroadcast ?: return
        if (b.mode == Mode.PRIVILEGED) privileged.stop()
        if (b.mode == Mode.SYSTEM_SHARE) openSystemAudioSharing() // user turns it off there
        currentBroadcast = null
        report()
    }

    fun isBroadcasting(): Boolean = currentBroadcast != null
    fun getCurrentBroadcast(): BroadcastInfo? = currentBroadcast

    /**
     * Text encoded in the app's share QR code. This is NOT the official Auracast
     * QR format (that needs the adapter address, which apps can't read); it just
     * shows friends the stream name and passkey to pick in their Auracast menu.
     * In SYSTEM_SHARE mode, use the QR code the OS sharing screen displays.
     */
    fun getBroadcastQRData(): String? {
        val b = currentBroadcast ?: return null
        val key = if (b.passkey.isNotEmpty()) "\nPasskey: ${b.passkey}" else ""
        return "OpenLE-Cast\nStream: ${b.broadcastName}$key"
    }

    private fun generatePasskey(): String = Random.nextInt(100000, 999999).toString()

    private fun openSystemAudioSharing() {
        // There is no public intent for the audio-sharing page, so open
        // Bluetooth settings, where Pixel/Samsung place "Audio sharing"/"Auracast".
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun statusText(): String {
        val b = currentBroadcast
        val route = when {
            systemBroadcastActive() -> "Phone is broadcasting (Auracast output active)"
            externalTransmitterOutput() != null -> "Output: ${externalTransmitterOutput()?.productName}"
            else -> "Output: phone speaker / headset"
        }
        val head = if (b == null) "Not broadcasting" else "Mode: ${b.mode} — \"${b.broadcastName}\""
        return "$head\n$route\nBroadcast source supported: ${isBroadcastSourceSupported()}"
    }

    private fun report() { onStatusChanged?.invoke(statusText()) }

    fun cleanup() {
        stopBroadcast()
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        leAudioProxy?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.LE_AUDIO, it) }
        leAudioProxy = null
        privileged.close()
    }
}

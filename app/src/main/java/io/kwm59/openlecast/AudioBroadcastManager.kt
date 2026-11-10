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
import android.content.Context
import android.media.AudioManager
import android.os.Build
import timber.log.Timber
import kotlin.random.Random

/**
 * NOTE: This is a scaffold. startBroadcast() currently records broadcast
 * metadata but does not yet start a real Auracast (BIS) stream — see README.
 */
class AudioBroadcastManager(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {

    private var leAudioProxy: BluetoothLeAudio? = null
    private var isBroadcasting = false
    private var broadcastId: Int = 0
    private var broadcastPasskey: String = ""

    // Broadcast metadata
    data class BroadcastInfo(
        val broadcastId: Int,
        val broadcastName: String,
        val passkey: String,
        val codec: String = "LC3",
        val sampleRate: Int = 48000,
        val bitrate: Int = 128000,
        val numReceivers: Int = 0
    )

    private var currentBroadcast: BroadcastInfo? = null

    init {
        setupLeAudioProxy()
    }

    private fun setupLeAudioProxy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && bluetoothAdapter != null) {
            bluetoothAdapter.getProfileProxy(context, object : BluetoothAdapter.BluetoothProfileServiceListener {
                override fun onServiceConnected(profile: Int, proxy: android.bluetooth.BluetoothProfile) {
                    if (profile == BluetoothLeAudio.BLUETOOTH_PROFILE_LE_AUDIO) {
                        leAudioProxy = proxy as? BluetoothLeAudio
                        Timber.d("LE Audio proxy connected")
                    }
                }
                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothLeAudio.BLUETOOTH_PROFILE_LE_AUDIO) {
                        leAudioProxy = null
                        Timber.d("LE Audio proxy disconnected")
                    }
                }
            }, BluetoothLeAudio.BLUETOOTH_PROFILE_LE_AUDIO)
        } else {
            Timber.w("LE Audio not supported on this device or API level")
        }
    }

    fun startBroadcast(broadcastName: String, withPasskey: Boolean = false): BroadcastInfo? {
        if (isBroadcasting) {
            Timber.w("Broadcast already in progress")
            return currentBroadcast
        }

        if (leAudioProxy == null) {
            Timber.e("LE Audio not available")
            return null
        }

        try {
            // Generate broadcast ID and passkey
            broadcastId = Random.nextInt(1, 65536)
            broadcastPasskey = if (withPasskey) generatePasskey() else ""

            // System will handle actual broadcast start via AudioManager
            val audioManager = context.getSystemService(AudioManager::class.java)
            val devices = audioManager.getDevicesForStream(AudioManager.STREAM_MUSIC)

            Timber.d("Available audio devices: ${devices.size}")

            currentBroadcast = BroadcastInfo(
                broadcastId = broadcastId,
                broadcastName = broadcastName,
                passkey = broadcastPasskey,
                codec = "LC3",
                sampleRate = 48000,
                bitrate = 128000
            )

            isBroadcasting = true
            Timber.i("Broadcast started: ID=$broadcastId, Name=$broadcastName, Passkey=$broadcastPasskey")

            return currentBroadcast
        } catch (e: Exception) {
            Timber.e(e, "Failed to start broadcast")
            return null
        }
    }

    fun stopBroadcast() {
        if (!isBroadcasting) {
            Timber.w("No broadcast in progress")
            return
        }

        try {
            isBroadcasting = false
            currentBroadcast = null
            Timber.i("Broadcast stopped")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop broadcast")
        }
    }

    fun isBroadcasting(): Boolean = isBroadcasting

    fun getCurrentBroadcast(): BroadcastInfo? = currentBroadcast

    fun getBroadcastQRData(): String? {
        val broadcast = currentBroadcast ?: return null
        // Format: BroadcastID|BroadcastName|Passkey (QR code will encode this)
        return "${broadcast.broadcastId}|${broadcast.broadcastName}|${broadcast.passkey}"
    }

    private fun generatePasskey(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    fun updateBroadcastMetadata(name: String, bitrate: Int = 128000) {
        if (!isBroadcasting) {
            Timber.w("No broadcast in progress")
            return
        }

        currentBroadcast = currentBroadcast?.copy(
            broadcastName = name,
            bitrate = bitrate
        )
        Timber.d("Broadcast metadata updated")
    }

    fun estimateReceiverCount(): Int {
        // Placeholder: In production, query BT controller for actual count
        return currentBroadcast?.numReceivers ?: 0
    }

    fun cleanup() {
        stopBroadcast()
        leAudioProxy = null
    }
}

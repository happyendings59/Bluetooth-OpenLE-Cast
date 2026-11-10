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
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import timber.log.Timber

class BluetoothHelper(private val context: Context) {

    private val bluetoothManager: BluetoothManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(BluetoothManager::class.java)
    } else {
        null
    }

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
        ?: @Suppress("DEPRECATION") android.bluetooth.BluetoothAdapter.getDefaultAdapter()

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun enableBluetooth() {
        if (!isBluetoothEnabled()) {
            @Suppress("DEPRECATION")
            bluetoothAdapter?.enable()
            Timber.d("Bluetooth enable request sent")
        }
    }

    fun getBluetoothAdapter(): BluetoothAdapter? = bluetoothAdapter

    fun supportsLeAudio(): Boolean {
        val adapter = bluetoothAdapter ?: return false
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    fun getBluetoothInfo(): String {
        val adapter = bluetoothAdapter ?: return "No Bluetooth adapter found"
        val name = adapter.name ?: "Unknown"
        val address = adapter.address ?: "Unknown"
        val enabled = adapter.isEnabled
        val leAudioSupported = supportsLeAudio()

        return """
            Name: $name
            Address: $address
            Enabled: $enabled
            LE Audio Support: $leAudioSupported
            SDK: ${Build.VERSION.SDK_INT}
        """.trimIndent()
    }
}

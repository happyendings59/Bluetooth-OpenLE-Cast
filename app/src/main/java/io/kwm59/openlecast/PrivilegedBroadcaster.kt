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
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber
import java.lang.reflect.Proxy
import java.util.concurrent.Executors

/**
 * EXPERIMENTAL — path C (privileged / rooted / custom ROM).
 *
 * android.bluetooth.BluetoothLeBroadcast is a @SystemApi. A normal app cannot
 * call it: the hidden-API filter blocks the reflection below and the system
 * requires BLUETOOTH_PRIVILEGED. It only works when OpenLE-Cast is installed as
 * a privileged app (e.g. in /system/priv-app with a privapp-permissions XML, or
 * built into a custom ROM). On a normal phone isAvailable() returns false and
 * nothing here runs.
 *
 * Untested on hardware. Method names follow AOSP (Android 13–15).
 */
class PrivilegedBroadcaster(
    private val context: Context,
    private val adapter: BluetoothAdapter?
) {
    private companion object {
        const val LE_AUDIO_BROADCAST = 26 // hidden BluetoothProfile.LE_AUDIO_BROADCAST
        const val PRIVILEGED = "android.permission.BLUETOOTH_PRIVILEGED"
    }

    private var proxy: Any? = null
    private var broadcastId: Int? = null
    private val executor = Executors.newSingleThreadExecutor()

    fun isAvailable(): Boolean =
        adapter != null &&
            context.checkSelfPermission(PRIVILEGED) == PackageManager.PERMISSION_GRANTED

    init {
        if (isAvailable()) {
            adapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, p: BluetoothProfile) {
                    proxy = p
                    registerCallback(p)
                    Timber.d("LE broadcast proxy connected")
                }
                override fun onServiceDisconnected(profile: Int) { proxy = null }
            }, LE_AUDIO_BROADCAST)
        }
    }

    private fun registerCallback(p: Any) = try {
        val cbClass = Class.forName("android.bluetooth.BluetoothLeBroadcast\$Callback")
        val cb = Proxy.newProxyInstance(cbClass.classLoader, arrayOf(cbClass)) { self, m, args ->
            when (m.name) {
                "onBroadcastStarted" -> {
                    broadcastId = args?.get(1) as? Int
                    Timber.i("Broadcast started id=$broadcastId"); null
                }
                "onBroadcastStopped" -> { broadcastId = null; Timber.i("Broadcast stopped"); null }
                "hashCode" -> System.identityHashCode(self)
                "equals" -> self === args?.get(0)
                "toString" -> "OpenLE-Cast broadcast callback"
                else -> { Timber.d("Broadcast callback: ${m.name}"); null }
            }
        }
        p.javaClass.getMethod("registerCallback", java.util.concurrent.Executor::class.java, cbClass)
            .invoke(p, executor, cb)
    } catch (e: Exception) {
        Timber.e(e, "Could not register broadcast callback")
    }

    /** Starts a broadcast. passkey may be empty for an open stream. */
    fun start(name: String, passkey: String): Boolean {
        val p = proxy ?: run { Timber.e("Privileged broadcast not available"); return false }
        return try {
            val metaClass = Class.forName("android.bluetooth.BluetoothLeAudioContentMetadata")
            val builderClass = Class.forName("android.bluetooth.BluetoothLeAudioContentMetadata\$Builder")
            val builder = builderClass.getConstructor().newInstance()
            builderClass.getMethod("setProgramInfo", String::class.java).invoke(builder, name)
            val meta = builderClass.getMethod("build").invoke(builder)
            // Broadcast code must be 4–16 bytes, or null for no encryption.
            val code: ByteArray? = passkey.takeIf { it.length >= 4 }?.toByteArray(Charsets.UTF_8)
            p.javaClass.getMethod("startBroadcast", metaClass, ByteArray::class.java)
                .invoke(p, meta, code)
            true
        } catch (e: Exception) {
            Timber.e(e, "startBroadcast failed (not a privileged app?)")
            false
        }
    }

    fun stop() {
        val p = proxy ?: return
        val id = broadcastId ?: return
        try {
            p.javaClass.getMethod("stopBroadcast", Int::class.javaPrimitiveType).invoke(p, id)
        } catch (e: Exception) {
            Timber.e(e, "stopBroadcast failed")
        }
    }

    fun close() {
        (proxy as? BluetoothProfile)?.let { adapter?.closeProfileProxy(LE_AUDIO_BROADCAST, it) }
        proxy = null
        executor.shutdown()
    }
}

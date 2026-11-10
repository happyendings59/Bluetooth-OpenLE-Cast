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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.result.contract.ActivityResultContracts
import timber.log.Timber

class PermissionHandler(private val activity: FragmentActivity) {

    // Required permissions for LE Audio broadcast
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    private var onPermissionsGranted: (() -> Unit)? = null

    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        val allGranted = grantResults.values.all { it }
        if (allGranted) {
            Timber.d("All permissions granted")
            onPermissionsGranted?.invoke()
        } else {
            Timber.e("Some permissions denied")
            val denied = grantResults.filter { !it.value }.keys
            Timber.d("Denied: $denied")
        }
    }

    fun checkAndRequestPermissions(onGranted: () -> Unit) {
        onPermissionsGranted = onGranted

        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            Timber.d("All permissions already granted")
            onGranted()
        } else {
            Timber.d("Requesting permissions: $missingPermissions")
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    fun hasAllPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}

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

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.kwm59.openlecast.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissions: PermissionHandler
    private lateinit var bluetooth: BluetoothHelper
    private lateinit var player: AudioPlayer
    private var broadcaster: AudioBroadcastManager? = null
    private var audioUri: Uri? = null

    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        audioUri = uri
        player.stop()
        player.loadAudio(uri)
        binding.trackText.text = uri.lastPathSegment ?: uri.toString()
        binding.playButton.isEnabled = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Timber.treeCount == 0) Timber.plant(Timber.DebugTree())

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissions = PermissionHandler(this)
        bluetooth = BluetoothHelper(this)
        player = AudioPlayer(this)

        binding.pickButton.setOnClickListener { pickAudio.launch(arrayOf("audio/*")) }
        binding.playButton.setOnClickListener { togglePlay() }
        binding.startButton.setOnClickListener { startSharing() }
        binding.stopButton.setOnClickListener { stopSharing() }
        binding.aboutButton.setOnClickListener { showAbout() }

        permissions.checkAndRequestPermissions { onReady() }
    }

    private fun onReady() {
        if (!bluetooth.isBluetoothEnabled()) {
            binding.statusText.text = getString(R.string.status_bt_off)
        }
        broadcaster = AudioBroadcastManager(this, bluetooth.getBluetoothAdapter()).also { b ->
            b.onStatusChanged = { text -> runOnUiThread { binding.statusText.text = text } }
            binding.modePrivileged.isEnabled = b.isPrivilegedAvailable()
            binding.statusText.text = b.statusText()
        }
        binding.startButton.isEnabled = true
    }

    private fun selectedMode(): AudioBroadcastManager.Mode = when (binding.modeGroup.checkedRadioButtonId) {
        R.id.modeExternal -> AudioBroadcastManager.Mode.EXTERNAL_TX
        R.id.modePrivileged -> AudioBroadcastManager.Mode.PRIVILEGED
        else -> AudioBroadcastManager.Mode.SYSTEM_SHARE
    }

    private fun togglePlay() {
        if (player.isPlaying()) {
            player.pause(); binding.playButton.setText(R.string.play)
        } else {
            player.play(); binding.playButton.setText(R.string.pause)
        }
    }

    private fun startSharing() {
        val b = broadcaster ?: return
        val name = binding.nameInput.text?.toString()?.trim().orEmpty().ifEmpty { getString(R.string.default_stream_name) }
        val info = b.startBroadcast(selectedMode(), name, binding.passkeyCheck.isChecked)
        if (info == null) {
            Toast.makeText(this, R.string.start_failed, Toast.LENGTH_LONG).show()
            return
        }
        val qrData = b.getBroadcastQRData()
        val qr = qrData?.let { QRCodeGenerator.generateQRCode(it) }
        binding.qrImage.setImageBitmap(qr)
        binding.qrImage.visibility = if (qr != null && info.mode != AudioBroadcastManager.Mode.SYSTEM_SHARE) View.VISIBLE else View.GONE
        binding.hintText.setText(
            when (info.mode) {
                AudioBroadcastManager.Mode.SYSTEM_SHARE -> R.string.hint_system
                AudioBroadcastManager.Mode.EXTERNAL_TX -> R.string.hint_external
                AudioBroadcastManager.Mode.PRIVILEGED -> R.string.hint_privileged
            }
        )
        binding.stopButton.isEnabled = true
    }

    private fun stopSharing() {
        broadcaster?.stopBroadcast()
        binding.qrImage.visibility = View.GONE
        binding.hintText.text = ""
        binding.stopButton.isEnabled = false
    }

    // AGPL §7(b) attribution: keep this screen in any modified version.
    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.about_text)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onDestroy() {
        broadcaster?.cleanup()
        player.stop()
        super.onDestroy()
    }
}

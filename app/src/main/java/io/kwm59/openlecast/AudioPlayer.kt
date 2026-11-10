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

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import timber.log.Timber

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    private val audioManager: AudioManager = context.getSystemService(AudioManager::class.java)

    fun loadAudio(uri: Uri) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, uri)
                prepare()
            }
            Timber.d("Audio loaded: $uri")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load audio")
        }
    }

    fun play() {
        try {
            mediaPlayer?.start()
            isPlaying = true
            Timber.d("Audio playback started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to play audio")
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            isPlaying = false
            Timber.d("Audio playback paused")
        } catch (e: Exception) {
            Timber.e(e, "Failed to pause audio")
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            Timber.d("Audio playback stopped")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop audio")
        }
    }

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    fun isPlaying(): Boolean = isPlaying

    fun setVolume(volume: Int) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
        Timber.d("Volume set to $volume")
    }

    fun getMaxVolume(): Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
}

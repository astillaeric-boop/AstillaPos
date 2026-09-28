package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

class SoundManager {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
        } catch (e: Exception) {
            Log.w("SoundManager", "Could not initialize ToneGenerator: ${e.message}")
        }
    }

    fun playScanBeep(enabled: Boolean = true) {
        if (!enabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing scan tone", e)
        }
    }

    fun playSuccessCheckout(enabled: Boolean = true) {
        if (!enabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing checkout tone", e)
        }
    }

    fun playErrorTone(enabled: Boolean = true) {
        if (!enabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing error tone", e)
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.e("SoundManager", "Error releasing tone generator", e)
        }
    }
}

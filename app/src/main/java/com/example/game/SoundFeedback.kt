package com.example.game

import android.media.AudioManager
import android.media.ToneGenerator

class SoundFeedback {
    private val toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 45)
    } catch (_: Exception) {
        null
    }

    fun playPlace(enabled: Boolean) {
        if (!enabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 45)
        } catch (_: Exception) {
        }
    }

    fun playClear(enabled: Boolean, combo: Int) {
        if (!enabled) return
        try {
            val tone = when {
                combo >= 3 -> ToneGenerator.TONE_PROP_ACK
                else -> ToneGenerator.TONE_PROP_BEEP2
            }
            toneGenerator?.startTone(tone, 110)
        } catch (_: Exception) {
        }
    }

    fun playWin(enabled: Boolean) {
        if (!enabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 220)
        } catch (_: Exception) {
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (_: Exception) {
        }
    }
}

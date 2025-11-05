package com.secureops.app.ml.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import timber.log.Timber
import java.util.*

class TextToSpeechManager(
    private val context: Context
) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private val pendingUtterances = mutableListOf<String>()

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                isInitialized = true
                Timber.d("TextToSpeech initialized successfully")

                // Speak any pending utterances
                pendingUtterances.forEach { speak(it) }
                pendingUtterances.clear()
            } else {
                Timber.e("TextToSpeech initialization failed")
                isInitialized = false
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Timber.d("TTS started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Timber.d("TTS completed: $utteranceId")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Timber.e("TTS error: $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Timber.e("TTS error: $utteranceId, code: $errorCode")
            }
        })
    }

    /**
     * Speak text out loud
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized) {
            Timber.w("TTS not initialized, queuing utterance")
            pendingUtterances.add(text)
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        textToSpeech?.speak(text, queueMode, params, utteranceId)
    }

    /**
     * Stop speaking
     */
    fun stop() {
        textToSpeech?.stop()
    }

    /**
     * Check if currently speaking
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }

    /**
     * Set speech rate (0.5 = half speed, 1.0 = normal, 2.0 = double speed)
     */
    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate)
    }

    /**
     * Set pitch (0.5 = lower, 1.0 = normal, 2.0 = higher)
     */
    fun setPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch)
    }

    /**
     * Shutdown TTS engine
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
}

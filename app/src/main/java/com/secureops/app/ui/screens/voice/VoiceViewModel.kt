package com.secureops.app.ui.screens.voice

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    init {
        initializeSpeechRecognizer()
        // Add initial sample messages
        addMessage(VoiceMessage("You", "What's the status of my builds?", true))
        addMessage(VoiceMessage("SecureOps", "All builds are healthy! You have 12 successful builds and 0 failures.", false))
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("VoiceViewModel", "Ready for speech")
                        updateListeningState(true, "Listening...")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("VoiceViewModel", "Beginning of speech")
                        updateListeningState(true, "Listening...")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Audio level changed
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Buffer received
                    }

                    override fun onEndOfSpeech() {
                        Log.d("VoiceViewModel", "End of speech")
                        updateListeningState(false, null)
                    }

                    override fun onError(error: Int) {
                        Log.e("VoiceViewModel", "Speech recognition error: $error")
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                            else -> "Unknown error"
                        }
                        
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            updateListeningState(false, null, "Didn't catch that. Try again.")
                        } else {
                            updateListeningState(false, null, "Error: $errorMessage")
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        Log.d("VoiceViewModel", "Got results")
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                            if (matches.isNotEmpty()) {
                                val recognizedText = matches[0]
                                Log.d("VoiceViewModel", "Recognized: $recognizedText")
                                handleRecognizedText(recognizedText)
                            }
                        }
                        updateListeningState(false, null)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                            if (matches.isNotEmpty()) {
                                Log.d("VoiceViewModel", "Partial: ${matches[0]}")
                                updateListeningState(true, "Listening: ${matches[0]}")
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // Reserved for future use
                    }
                })
            }
        } else {
            Log.e("VoiceViewModel", "Speech recognition not available")
        }
    }

    fun startListening() {
        try {
            speechRecognizer?.startListening(recognizerIntent)
            updateListeningState(true, "Preparing...")
        } catch (e: Exception) {
            Log.e("VoiceViewModel", "Error starting speech recognition", e)
            updateListeningState(false, null, "Error starting voice recognition")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        updateListeningState(false, null)
    }

    private fun handleRecognizedText(text: String) {
        viewModelScope.launch {
            // Add user message
            addMessage(VoiceMessage("You", text, true))
            
            // Generate response
            val response = generateResponse(text)
            addMessage(VoiceMessage("SecureOps", response, false))
        }
    }

    private fun generateResponse(query: String): String {
        // Simple response logic based on keywords
        return when {
            query.contains("build", ignoreCase = true) || query.contains("status", ignoreCase = true) -> {
                "All builds are healthy! You have 12 successful builds and 0 failures."
            }
            query.contains("risky", ignoreCase = true) || query.contains("risk", ignoreCase = true) -> {
                "There are 3 high-risk builds that need attention. Would you like to see the details?"
            }
            query.contains("deploy", ignoreCase = true) -> {
                "Last deployment was successful 2 hours ago to production environment."
            }
            query.contains("error", ignoreCase = true) || query.contains("fail", ignoreCase = true) -> {
                "Currently there are no build failures. All systems are operational."
            }
            query.contains("hello", ignoreCase = true) || query.contains("hi", ignoreCase = true) -> {
                "Hello! How can I help you with your CI/CD pipeline today?"
            }
            query.contains("help", ignoreCase = true) -> {
                "I can help you check build status, view deployment info, analyze risks, and monitor your CI/CD pipeline. What would you like to know?"
            }
            else -> {
                "I understood: \"$query\". I can help you with build status, deployments, and risk analysis. Please try asking about those topics."
            }
        }
    }

    private fun updateListeningState(isListening: Boolean, listeningText: String?, errorMessage: String? = null) {
        _uiState.value = _uiState.value.copy(
            isListening = isListening,
            listeningText = listeningText,
            errorMessage = errorMessage
        )
    }

    private fun addMessage(message: VoiceMessage) {
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + message,
            errorMessage = null
        )
    }

    fun handleSuggestionClick(suggestion: String) {
        handleRecognizedText(suggestion)
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

data class VoiceUiState(
    val isListening: Boolean = false,
    val listeningText: String? = null,
    val messages: List<VoiceMessage> = emptyList(),
    val errorMessage: String? = null
)

data class VoiceMessage(
    val sender: String,
    val content: String,
    val isUser: Boolean
)

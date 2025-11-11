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
import com.secureops.app.ml.voice.VoiceActionExecutor
import com.secureops.app.data.repository.VoiceMessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import timber.log.Timber

class VoiceViewModel(
    application: Application,
    private val voiceActionExecutor: VoiceActionExecutor,
    private val voiceMessageRepository: VoiceMessageRepository
) : AndroidViewModel(application) {
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
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            try {
                voiceMessageRepository.getAllMessages().collect { messages ->
                    _uiState.value = _uiState.value.copy(messages = messages)
                    Timber.d("Loaded ${messages.size} voice messages from database")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load voice messages")
            }
        }
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

            // Show processing indicator
            _uiState.value = _uiState.value.copy(isProcessing = true)

            try {
                // Use actual voice action executor
                val result = voiceActionExecutor.processAndExecute(text)

                // Add response message
                addMessage(VoiceMessage("SecureOps", result.spokenResponse, false))
            } catch (e: Exception) {
                Timber.e(e, "Error processing voice command")
                addMessage(
                    VoiceMessage(
                        "SecureOps",
                        "I encountered an error processing your request. Please try again.",
                        false
                    )
                )
            } finally {
                _uiState.value = _uiState.value.copy(isProcessing = false)
            }
        }
    }

    private fun generateResponse(query: String): String {
        // This method is no longer used but kept for backward compatibility
        return "Processing your request..."
    }

    private fun updateListeningState(isListening: Boolean, listeningText: String?, errorMessage: String? = null) {
        _uiState.value = _uiState.value.copy(
            isListening = isListening,
            listeningText = listeningText,
            errorMessage = errorMessage
        )
    }

    private fun addMessage(message: VoiceMessage) {
        viewModelScope.launch {
            voiceMessageRepository.saveMessage(message)
        }
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
    val errorMessage: String? = null,
    val isProcessing: Boolean = false
)

data class VoiceMessage(
    val sender: String,
    val content: String,
    val isUser: Boolean
)

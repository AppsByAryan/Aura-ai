package com.example.aura.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    var isVoiceOutputEnabled = true
    private var pendingSpeakText: String? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && tts != null) {
            // Try default device locale, then US, then English
            var langResult = tts?.setLanguage(Locale.getDefault())
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                langResult = tts?.setLanguage(Locale.US)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
            }

            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)
            isTtsInitialized = true

            // Speak any utterance that was requested while TTS engine was initializing
            pendingSpeakText?.let { text ->
                pendingSpeakText = null
                speak(text)
            }
        }
    }

    fun speak(text: String) {
        if (!isVoiceOutputEnabled) return
        val cleanText = text
            .replace(Regex("[*#_`>]"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .trim()
            .take(350)

        if (cleanText.isEmpty()) return

        if (!isTtsInitialized || tts == null) {
            pendingSpeakText = cleanText
            if (tts == null) {
                try {
                    tts = TextToSpeech(context.applicationContext, this)
                } catch (_: Exception) {}
            }
            return
        }

        try {
            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            }
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "aura_tts_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            try {
                @Suppress("DEPRECATION")
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null)
            } catch (_: Exception) {}
        }
    }

    fun stopSpeaking() {
        pendingSpeakText = null
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device.")
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onListeningStateChanged(true)
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    onListeningStateChanged(false)
                }

                override fun onError(error: Int) {
                    onListeningStateChanged(false)
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_NETWORK -> "Network issue in speech engine"
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        else -> "Speech recognition ended ($error)"
                    }
                    if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                        onError(message)
                    }
                }

                override fun onResults(results: Bundle?) {
                    onListeningStateChanged(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val topMatch = matches?.firstOrNull()?.trim()
                    if (!topMatch.isNullOrEmpty()) {
                        onSpeechRecognized(topMatch)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onListeningStateChanged(false)
            onError("Could not start microphone: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        onListeningStateChanged(false)
    }

    fun release() {
        stopListening()
        stopSpeaking()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isTtsInitialized = false
    }
}

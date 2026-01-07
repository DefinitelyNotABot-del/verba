package com.imu.verba.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

/**
 * Playback state for TTS.
 */
enum class TtsPlaybackState {
    IDLE,
    PLAYING,
    PAUSED
}

/**
 * Represents a TTS voice with display-friendly info.
 */
data class TtsVoice(
    val voice: Voice,
    val displayName: String,
    val languageDisplay: String
)

/**
 * Callback interface for TTS events.
 */
interface TtsCallback {
    fun onReady(voices: List<TtsVoice>)
    fun onBlockStarted(blockId: Int)
    fun onBlockCompleted(blockId: Int)
    fun onPlaybackCompleted()
    fun onError(message: String)
}

/**
 * Controller for Text-to-Speech functionality.
 * Provides block-by-block playback with natural pacing.
 *
 * Key design decisions:
 * - Uses Android's built-in TextToSpeech engine (offline capable)
 * - Block-by-block playback with minimal inter-block delay
 * - Speed adjustable from 0.5x to 3.0x
 * - Voice selection from system-installed voices
 */
class TextToSpeechController(
    context: Context,
    private val callback: TtsCallback
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private var blocks: List<Pair<Int, String>> = emptyList() // (blockId, text)
    private var currentIndex = 0
    private var endIndex = Int.MAX_VALUE

    private var _playbackState = TtsPlaybackState.IDLE
    val playbackState: TtsPlaybackState get() = _playbackState

    private var _speechRate = 1.0f
    val speechRate: Float get() = _speechRate

    private var _currentVoice: Voice? = null
    val currentVoice: Voice? get() = _currentVoice

    private var pausedAtIndex: Int? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale.getDefault()
                _currentVoice = tts?.voice
                setupUtteranceListener()
                // Notify that TTS is ready with available voices
                callback.onReady(getAvailableVoices())
            } else {
                callback.onError("Failed to initialize TTS engine")
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                utteranceId?.toIntOrNull()?.let { blockId ->
                    callback.onBlockStarted(blockId)
                }
            }

            override fun onDone(utteranceId: String?) {
                utteranceId?.toIntOrNull()?.let { blockId ->
                    callback.onBlockCompleted(blockId)
                }
                // Auto-advance to next block
                if (_playbackState == TtsPlaybackState.PLAYING) {
                    currentIndex++
                    if (currentIndex <= endIndex && currentIndex < blocks.size) {
                        speakCurrentBlock()
                    } else {
                        _playbackState = TtsPlaybackState.IDLE
                        callback.onPlaybackCompleted()
                    }
                }
            }

            @Deprecated("Deprecated in API 21+")
            override fun onError(utteranceId: String?) {
                callback.onError("TTS error during playback")
                _playbackState = TtsPlaybackState.IDLE
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                callback.onError("TTS error: code $errorCode")
                _playbackState = TtsPlaybackState.IDLE
            }
        })
    }

    /**
     * Get available voices installed on the device.
     * Filters to voices that match common languages.
     */
    fun getAvailableVoices(): List<TtsVoice> {
        if (!isInitialized) return emptyList()

        return tts?.voices
            ?.filter { !it.isNetworkConnectionRequired && !it.features.contains("notInstalled") }
            ?.map { voice ->
                TtsVoice(
                    voice = voice,
                    displayName = voice.name.substringAfterLast("-").substringBefore("#")
                        .replaceFirstChar { it.uppercase() },
                    languageDisplay = voice.locale.displayName
                )
            }
            ?.sortedBy { it.languageDisplay }
            ?: emptyList()
    }

    /**
     * Set the TTS voice.
     * If called during playback, continues with new voice from current position.
     */
    fun setVoice(voice: Voice) {
        tts?.voice = voice
        _currentVoice = voice
    }

    /**
     * Set speech rate (0.5 to 3.0, where 1.0 is normal).
     * Applies immediately even during playback.
     */
    fun setSpeechRate(rate: Float) {
        _speechRate = rate.coerceIn(0.5f, 3.0f)
        tts?.setSpeechRate(_speechRate)
    }

    /**
     * Start playback from a specific block to an optional end block.
     *
     * @param blocksToSpeak List of (blockId, text) pairs
     * @param startBlockId Block ID to start from
     * @param endBlockId Block ID to stop at (inclusive), or null for end of document
     */
    fun startPlayback(
        blocksToSpeak: List<Pair<Int, String>>,
        startBlockId: Int,
        endBlockId: Int? = null
    ) {
        if (!isInitialized) {
            callback.onError("TTS not initialized")
            return
        }

        blocks = blocksToSpeak
        currentIndex = blocks.indexOfFirst { it.first == startBlockId }.coerceAtLeast(0)
        endIndex = if (endBlockId != null) {
            blocks.indexOfFirst { it.first == endBlockId }
        } else {
            blocks.lastIndex
        }

        if (endIndex < 0) endIndex = blocks.lastIndex

        pausedAtIndex = null
        _playbackState = TtsPlaybackState.PLAYING
        tts?.setSpeechRate(_speechRate)
        speakCurrentBlock()
    }

    /**
     * Jump to a specific block and continue playing from there.
     */
    fun jumpToBlock(blockId: Int) {
        val newIndex = blocks.indexOfFirst { it.first == blockId }
        if (newIndex >= 0) {
            tts?.stop()
            currentIndex = newIndex
            if (_playbackState == TtsPlaybackState.PLAYING) {
                speakCurrentBlock()
            }
        }
    }

    /**
     * Pause playback. Can be resumed with resume().
     */
    fun pause() {
        if (_playbackState == TtsPlaybackState.PLAYING) {
            tts?.stop()
            pausedAtIndex = currentIndex
            _playbackState = TtsPlaybackState.PAUSED
        }
    }

    /**
     * Resume playback from paused position.
     */
    fun resume() {
        if (_playbackState == TtsPlaybackState.PAUSED && pausedAtIndex != null) {
            currentIndex = pausedAtIndex!!
            _playbackState = TtsPlaybackState.PLAYING
            tts?.setSpeechRate(_speechRate)
            speakCurrentBlock()
        }
    }

    /**
     * Stop playback completely.
     */
    fun stop() {
        tts?.stop()
        _playbackState = TtsPlaybackState.IDLE
        pausedAtIndex = null
        currentIndex = 0
    }

    private fun speakCurrentBlock() {
        if (currentIndex < 0 || currentIndex >= blocks.size) {
            _playbackState = TtsPlaybackState.IDLE
            callback.onPlaybackCompleted()
            return
        }

        val (blockId, text) = blocks[currentIndex]
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            blockId.toString()
        )
    }

    /**
     * Release TTS resources. Call when done with the controller.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

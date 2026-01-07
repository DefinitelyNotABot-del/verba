package com.imu.verba.viewmodel

import android.content.Context
import android.net.Uri
import android.speech.tts.Voice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.imu.verba.document.model.DocBlock
import com.imu.verba.document.model.Document
import com.imu.verba.document.parser.MarkdownParser
import com.imu.verba.storage.FileAccessHelper
import com.imu.verba.tts.TextToSpeechController
import com.imu.verba.tts.TtsCallback
import com.imu.verba.tts.TtsPlaybackState
import com.imu.verba.tts.TtsVoice
import com.imu.verba.tts.DocumentContext
import com.imu.verba.ui.reader.DisplayMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * UI state for the Reader screen.
 */
data class ReaderUiState(
    val isLoading: Boolean = false,
    val document: Document? = null,
    val error: String? = null,
    val currentBlockId: Int? = null,
    val startBlockId: Int? = null,
    val endBlockId: Int? = null,
    val playbackState: TtsPlaybackState = TtsPlaybackState.IDLE,
    val speechRate: Float = 1.0f,
    val availableVoices: List<TtsVoice> = emptyList(),
    val currentVoice: Voice? = null,
    val detectedContext: DocumentContext = DocumentContext.GENERAL,
    val displayMode: DisplayMode = DisplayMode.READ
)

/**
 * ViewModel for the Reader screen.
 * Manages document loading, TTS playback, and UI state.
 */
class ReaderViewModel(
    private val applicationContext: Context
) : ViewModel(), TtsCallback {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val markdownParser = MarkdownParser()
    private var ttsController: TextToSpeechController? = null

    private var documentUri: Uri? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        ttsController = TextToSpeechController(applicationContext, this)
        // TTS will call onReady() when initialized
    }

    /**
     * Load a document from a content URI.
     */
    fun loadDocument(uri: Uri) {
        documentUri = uri
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = withContext(Dispatchers.IO) {
                try {
                    val fileName = FileAccessHelper.getFileName(applicationContext, uri)
                    val format = FileAccessHelper.detectFormat(fileName)

                    when (format) {
                        "markdown" -> {
                            val content = FileAccessHelper.readTextContent(applicationContext, uri)
                            if (content != null) {
                                Result.success(markdownParser.parse(content, fileName))
                            } else {
                                Result.failure(Exception("Failed to read file"))
                            }
                        }
                        "pdf" -> Result.failure(Exception("PDF support coming in Phase 2"))
                        "docx" -> Result.failure(Exception("DOCX support coming in Phase 2"))
                        else -> Result.failure(Exception("Unsupported file format"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            result.fold(
                onSuccess = { document ->
                    // Detect document context for TTS preprocessing
                    val fullText = document.blocks.joinToString(" ") { it.text }
                    val detectedContext = ttsController?.preprocessor?.detectContext(fullText) 
                        ?: DocumentContext.GENERAL
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        document = document,
                        error = null,
                        detectedContext = detectedContext
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error"
                    )
                }
            )
        }
    }

    /**
     * Set the start block for TTS range playback.
     */
    fun setStartBlock(blockId: Int) {
        _uiState.value = _uiState.value.copy(startBlockId = blockId)
    }

    /**
     * Set the end block for TTS range playback.
     */
    fun setEndBlock(blockId: Int) {
        _uiState.value = _uiState.value.copy(endBlockId = blockId)
    }

    /**
     * Clear range selection.
     */
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(startBlockId = null, endBlockId = null)
    }

    /**
     * Start TTS playback.
     * Uses range selection if set, otherwise plays from start.
     */
    fun play() {
        val document = _uiState.value.document ?: return
        val startId = _uiState.value.startBlockId ?: document.blocks.firstOrNull()?.id ?: return
        val endId = _uiState.value.endBlockId

        val blocksToSpeak = document.blocks.map { it.id to it.text }
        ttsController?.startPlayback(blocksToSpeak, startId, endId)

        _uiState.value = _uiState.value.copy(playbackState = TtsPlaybackState.PLAYING)
    }

    /**
     * Play from a specific block (tap-to-start).
     */
    fun playFromBlock(blockId: Int) {
        val document = _uiState.value.document ?: return
        val endId = _uiState.value.endBlockId

        val blocksToSpeak = document.blocks.map { it.id to it.text }
        ttsController?.startPlayback(blocksToSpeak, blockId, endId)

        _uiState.value = _uiState.value.copy(
            playbackState = TtsPlaybackState.PLAYING,
            startBlockId = blockId
        )
    }

    /**
     * Pause TTS playback.
     */
    fun pause() {
        ttsController?.pause()
        _uiState.value = _uiState.value.copy(playbackState = TtsPlaybackState.PAUSED)
    }

    /**
     * Resume TTS playback from paused position.
     */
    fun resume() {
        ttsController?.resume()
        _uiState.value = _uiState.value.copy(playbackState = TtsPlaybackState.PLAYING)
    }

    /**
     * Stop TTS playback.
     */
    fun stop() {
        ttsController?.stop()
        _uiState.value = _uiState.value.copy(
            playbackState = TtsPlaybackState.IDLE,
            currentBlockId = null
        )
    }

    /**
     * Set speech rate (0.5 to 3.0).
     */
    fun setSpeechRate(rate: Float) {
        ttsController?.setSpeechRate(rate)
        _uiState.value = _uiState.value.copy(speechRate = rate)
    }

    /**
     * Set TTS voice.
     */
    fun setVoice(voice: Voice) {
        ttsController?.setVoice(voice)
        _uiState.value = _uiState.value.copy(currentVoice = voice)
    }

    /**
     * Manually set the document context for TTS pronunciation.
     * Overrides auto-detection.
     */
    fun setDocumentContext(context: DocumentContext) {
        ttsController?.preprocessor?.setContext(context)
        _uiState.value = _uiState.value.copy(detectedContext = context)
    }

    /**
     * Toggle between Read and Edit display modes.
     */
    fun toggleDisplayMode() {
        val newMode = when (_uiState.value.displayMode) {
            DisplayMode.READ -> DisplayMode.EDIT
            DisplayMode.EDIT -> DisplayMode.READ
        }
        _uiState.value = _uiState.value.copy(displayMode = newMode)
    }

    /**
     * Set display mode directly.
     */
    fun setDisplayMode(mode: DisplayMode) {
        _uiState.value = _uiState.value.copy(displayMode = mode)
    }

    // TtsCallback implementation
    override fun onReady(voices: List<TtsVoice>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                availableVoices = voices,
                currentVoice = ttsController?.currentVoice
            )
        }
    }

    override fun onBlockStarted(blockId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(currentBlockId = blockId)
        }
    }

    override fun onBlockCompleted(blockId: Int) {
        // Handled automatically by TtsController
    }

    override fun onPlaybackCompleted() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                playbackState = TtsPlaybackState.IDLE,
                currentBlockId = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsController?.shutdown()
    }

    override fun onError(message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                error = message,
                playbackState = TtsPlaybackState.IDLE
            )
        }
    }

    /**
     * Factory for creating ReaderViewModel with application context.
     */
    class Factory(private val applicationContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
                return ReaderViewModel(applicationContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

package com.svcj91.naradavoicerecorder.presentation.recorder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.svcj91.naradavoicerecorder.domain.model.AudioRecorder
import com.svcj91.naradavoicerecorder.domain.usecase.GetRecordingStateUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.GetRecordingsUseCase
import com.svcj91.naradavoicerecorder.service.RecordingForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecorderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecorder: AudioRecorder,
    private val getRecordingStateUseCase: GetRecordingStateUseCase,
    private val getRecordingsUseCase: GetRecordingsUseCase
) : ViewModel() {

    // Recording status flow
    val isRecording: StateFlow<Boolean> = getRecordingStateUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Paused status flow
    val isPaused: StateFlow<Boolean> = audioRecorder.isPaused

    // Recent recording count
    val recentRecordingsCount: StateFlow<Int> = getRecordingsUseCase()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Exposed shared flow for hardware/storage error events
    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    // Flow emitting the elapsed recording time in seconds
    val elapsedTimeSeconds: StateFlow<Long> = audioRecorder.elapsedTimeSeconds

    init {
        viewModelScope.launch {
            audioRecorder.errorEvents.collect { error ->
                _errorEvents.emit(error)
            }
        }
    }

    fun startRecording() {
        RecordingForegroundService.startService(context)
    }

    fun stopRecording() {
        RecordingForegroundService.stopService(context)
    }

    fun pauseRecording() {
        audioRecorder.pause()
    }

    fun resumeRecording() {
        audioRecorder.resume()
    }

    fun discardRecording() {
        RecordingForegroundService.discardService(context)
    }
}

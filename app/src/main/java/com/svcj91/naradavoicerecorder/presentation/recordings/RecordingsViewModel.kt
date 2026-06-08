package com.svcj91.naradavoicerecorder.presentation.recordings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.svcj91.naradavoicerecorder.domain.model.AudioPlayer
import com.svcj91.naradavoicerecorder.domain.model.PlaybackState
import com.svcj91.naradavoicerecorder.domain.model.Recording
import com.svcj91.naradavoicerecorder.domain.usecase.DeleteRecordingUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.GetPlaybackStateUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.GetRecordingsUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.PauseAudioUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.PlayAudioUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.SeekAudioUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.ShareRecordingUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.StopAudioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingsViewModel @Inject constructor(
    private val audioPlayer: AudioPlayer,
    private val getRecordingsUseCase: GetRecordingsUseCase,
    private val getPlaybackStateUseCase: GetPlaybackStateUseCase,
    private val playAudioUseCase: PlayAudioUseCase,
    private val pauseAudioUseCase: PauseAudioUseCase,
    private val seekAudioUseCase: SeekAudioUseCase,
    private val stopAudioUseCase: StopAudioUseCase,
    private val deleteRecordingUseCase: DeleteRecordingUseCase,
    private val shareRecordingUseCase: ShareRecordingUseCase
) : ViewModel() {

    // Observe recordings list from repository
    val recordings: StateFlow<List<Recording>> = getRecordingsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Observe active playback state
    val playbackState: StateFlow<PlaybackState> = getPlaybackStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlaybackState()
        )

    // Exposed shared flow for playback error events
    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            audioPlayer.errorEvents.collect { error ->
                _errorEvents.emit(error)
            }
        }
    }

    fun play(recording: Recording) {
        playAudioUseCase(recording.uri)
    }

    fun pause() {
        pauseAudioUseCase()
    }

    fun seekTo(positionMs: Long) {
        seekAudioUseCase(positionMs)
    }

    fun stop() {
        stopAudioUseCase()
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            // Stop audio if we are deleting the playing recording
            if (playbackState.value.activeUri == recording.uri) {
                stop()
            }
            deleteRecordingUseCase(recording)
        }
    }

    fun getShareIntent(recording: Recording): Intent {
        return shareRecordingUseCase(recording)
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}

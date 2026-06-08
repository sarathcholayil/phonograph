package com.svcj91.naradavoicerecorder.presentation.recorder

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    // Recent recording count
    val recentRecordingsCount: StateFlow<Int> = getRecordingsUseCase()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Exposed shared flow for hardware/storage error events
    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    // Track the bound service instance reactively
    private val _boundService = MutableStateFlow<RecordingForegroundService?>(null)

    // Flow emitting the elapsed recording time in seconds
    val elapsedTimeSeconds: StateFlow<Long> = _boundService
        .flatMapLatest { service ->
            service?.elapsedTimeSeconds ?: flowOf(0L)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? RecordingForegroundService.LocalBinder
            _boundService.value = binder?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _boundService.value = null
            isBound = false
        }
    }

    init {
        viewModelScope.launch {
            audioRecorder.errorEvents.collect { error ->
                _errorEvents.emit(error)
            }
        }
        bindToService()
    }

    private fun bindToService() {
        val intent = Intent(context, RecordingForegroundService::class.java)
        try {
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unbindFromService() {
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _boundService.value = null
            isBound = false
        }
    }

    fun startRecording() {
        RecordingForegroundService.startService(context)
        if (!isBound) {
            bindToService()
        }
    }

    fun stopRecording() {
        RecordingForegroundService.stopService(context)
        // Reset local timer estimation since service will stop
        _boundService.value = null
    }

    override fun onCleared() {
        super.onCleared()
        unbindFromService()
    }
}

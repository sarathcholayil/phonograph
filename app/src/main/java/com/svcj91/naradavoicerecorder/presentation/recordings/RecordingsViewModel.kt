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
import com.svcj91.naradavoicerecorder.domain.usecase.RenameRecordingUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.SeekAudioUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.ShareRecordingUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.StopAudioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption(val displayName: String) {
    DATE_DESC("Newest first"),
    DATE_ASC("Oldest first"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    DURATION_DESC("Longest first"),
    DURATION_ASC("Shortest first"),
    SIZE_DESC("Largest first"),
    SIZE_ASC("Smallest first")
}

enum class DateFilter(val displayName: String) {
    ALL("All Time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days")
}

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
    private val renameRecordingUseCase: RenameRecordingUseCase,
    private val shareRecordingUseCase: ShareRecordingUseCase
) : ViewModel() {

    // Filter and sort states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilter.ALL)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    // Observe recordings list from repository and combine with search, filter, and sort options
    val recordings: StateFlow<List<Recording>> = combine(
        getRecordingsUseCase(),
        _searchQuery,
        _sortOption,
        _dateFilter
    ) { list, query, sort, dateFilter ->
        val now = System.currentTimeMillis()
        list.asSequence()
            .filter { recording ->
                query.isEmpty() || recording.name.contains(query, ignoreCase = true)
            }
            .filter { recording ->
                when (dateFilter) {
                    DateFilter.ALL -> true
                    DateFilter.TODAY -> {
                        val oneDayAgo = now - 24 * 60 * 60 * 1000L
                        recording.dateCreated >= oneDayAgo
                    }
                    DateFilter.LAST_7_DAYS -> {
                        val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000L
                        recording.dateCreated >= sevenDaysAgo
                    }
                    DateFilter.LAST_30_DAYS -> {
                        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000L
                        recording.dateCreated >= thirtyDaysAgo
                    }
                }
            }
            .sortedWith(
                when (sort) {
                    SortOption.DATE_DESC -> compareByDescending { it.dateCreated }
                    SortOption.DATE_ASC -> compareBy { it.dateCreated }
                    SortOption.NAME_ASC -> compareBy { it.name.lowercase() }
                    SortOption.NAME_DESC -> compareByDescending { it.name.lowercase() }
                    SortOption.DURATION_DESC -> compareByDescending { it.durationMs }
                    SortOption.DURATION_ASC -> compareBy { it.durationMs }
                    SortOption.SIZE_DESC -> compareByDescending { it.sizeBytes }
                    SortOption.SIZE_ASC -> compareBy { it.sizeBytes }
                }
            )
            .toList()
    }.stateIn(
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun setDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
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

    fun renameRecording(recording: Recording, newName: String) {
        viewModelScope.launch {
            val result = renameRecordingUseCase(recording, newName)
            if (!result) {
                _errorEvents.emit("Failed to rename file")
            }
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

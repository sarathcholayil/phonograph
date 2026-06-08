package com.svcj91.naradavoicerecorder

import com.svcj91.naradavoicerecorder.domain.model.AudioRecorder
import com.svcj91.naradavoicerecorder.domain.usecase.GetRecordingStateUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.StartRecordingUseCase
import com.svcj91.naradavoicerecorder.domain.usecase.StopRecordingUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * A Fake implementation of [AudioRecorder] for testing.
 */
class FakeAudioRecorder : AudioRecorder {
    private val _isRecording = MutableStateFlow(false)
    override val isRecording: Flow<Boolean> = _isRecording.asStateFlow()
    override val errorEvents: Flow<String> = kotlinx.coroutines.flow.MutableSharedFlow<String>()

    var lastOutputFile: File? = null
    var startCalledCount = 0
    var stopCalledCount = 0

    override fun start(outputFile: File) {
        startCalledCount++
        lastOutputFile = outputFile
        _isRecording.value = true
    }

    override fun stop() {
        stopCalledCount++
        _isRecording.value = false
    }
}

/**
 * Unit tests for recording Use Cases.
 */
class AudioRecorderUseCaseTest {

    private lateinit var fakeAudioRecorder: FakeAudioRecorder
    private lateinit var startRecordingUseCase: StartRecordingUseCase
    private lateinit var stopRecordingUseCase: StopRecordingUseCase
    private lateinit var getRecordingStateUseCase: GetRecordingStateUseCase

    @Before
    fun setUp() {
        fakeAudioRecorder = FakeAudioRecorder()
        startRecordingUseCase = StartRecordingUseCase(fakeAudioRecorder)
        stopRecordingUseCase = StopRecordingUseCase(fakeAudioRecorder)
        getRecordingStateUseCase = GetRecordingStateUseCase(fakeAudioRecorder)
    }

    @Test
    fun startRecording_callsStart_withCorrectFileExtension() = runBlocking {
        val testFile = File("test_audio.m4a")

        // Initial state should be false
        assertFalse(getRecordingStateUseCase().first())

        startRecordingUseCase(testFile)

        assertEquals(1, fakeAudioRecorder.startCalledCount)
        assertEquals(testFile, fakeAudioRecorder.lastOutputFile)
        assertTrue(getRecordingStateUseCase().first())

        // Verify configuration matches MPEG_4/AAC with .m4a extension requirement
        assertEquals("m4a", fakeAudioRecorder.lastOutputFile?.extension)
    }

    @Test
    fun stopRecording_callsStop() = runBlocking {
        fakeAudioRecorder.start(File("test_audio.m4a"))
        assertTrue(getRecordingStateUseCase().first())

        stopRecordingUseCase()

        assertEquals(1, fakeAudioRecorder.stopCalledCount)
        assertFalse(getRecordingStateUseCase().first())
    }
}

package com.benjakusa.malele.ui.screens.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjakusa.malele.audio.engine.StepSequencerEngine
import com.benjakusa.malele.audio.engine.VoiceRecorder
import com.benjakusa.malele.audio.export.M4aExporter
import com.benjakusa.malele.audio.export.WavExporter
import com.benjakusa.malele.data.local.PatternStore
import com.benjakusa.malele.data.local.VoiceStore
import com.benjakusa.malele.domain.model.DrumPattern
import com.benjakusa.malele.domain.model.InstrumentParams
import com.benjakusa.malele.domain.model.Note
import com.benjakusa.malele.domain.model.VoiceTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class StudioUiState(
    val pattern: DrumPattern = DrumPattern.defaultPattern(),
    val isPlaying: Boolean = false,
    val playheadStep: Int = 0,
    val selectedInstrument: String = "bass",
    val selectedEditorTrack: Int = 0,
    val savedPatterns: List<DrumPattern> = emptyList(),
    val voiceTracks: List<VoiceTrack> = emptyList(),
    val isRecordingVoice: Boolean = false,
    val voiceAmplitude: Float = 0f,
    val isSaving: Boolean = false,
    val isExporting: Boolean = false,
    val isAiLoading: Boolean = false,
    val exportedFile: File? = null,
    val message: String? = null,
)

@HiltViewModel
class StudioViewModel @Inject constructor(
    private val stepSequencerEngine: StepSequencerEngine,
    private val patternStore: PatternStore,
    private val voiceStore: VoiceStore,
    private val voiceRecorder: VoiceRecorder,
    private val wavExporter: WavExporter,
    private val m4aExporter: M4aExporter,
) : ViewModel() {

    private val _state = MutableStateFlow(StudioUiState())
    val state: StateFlow<StudioUiState> = _state.asStateFlow()

    private var currentRecordingFile: File? = null
    private var currentRecordingTakeId: String? = null

    init {
        stepSequencerEngine.setPattern(_state.value.pattern)

        viewModelScope.launch {
            stepSequencerEngine.isPlaying.collectLatest { playing ->
                _state.value = _state.value.copy(isPlaying = playing)
            }
        }

        viewModelScope.launch {
            stepSequencerEngine.playheadStep.collectLatest { step ->
                _state.value = _state.value.copy(playheadStep = step)
            }
        }

        viewModelScope.launch {
            voiceRecorder.isRecording.collectLatest { recording ->
                _state.value = _state.value.copy(isRecordingVoice = recording)
            }
        }

        viewModelScope.launch {
            voiceRecorder.amplitude.collectLatest { amp ->
                _state.value = _state.value.copy(voiceAmplitude = amp)
            }
        }

        loadSavedPatterns()
    }

    fun loadSavedPatterns() {
        viewModelScope.launch {
            val list = patternStore.list()
            _state.value = _state.value.copy(savedPatterns = list)
        }
    }

    fun toggleStep(track: Int, step: Int) {
        val updated = _state.value.pattern.toggle(track, step)
        _state.value = _state.value.copy(pattern = updated)
        stepSequencerEngine.setPattern(updated)
    }

    fun setSelectedInstrument(instrument: String) {
        _state.value = _state.value.copy(selectedInstrument = instrument)
    }

    fun setSelectedEditorTrack(track: Int) {
        _state.value = _state.value.copy(selectedEditorTrack = track)
    }

    fun updateInstrumentParams(track: Int, params: InstrumentParams) {
        val updated = _state.value.pattern.withInstrumentParams(track, params)
        _state.value = _state.value.copy(pattern = updated)
        stepSequencerEngine.setPattern(updated)
    }

    fun addTrack(trackId: String) {
        val current = _state.value.pattern
        val withNew = current.addTrack(trackId)
        if (withNew.trackIds.size == current.trackIds.size) return
        val resizedMixer = withNew.mixer.resize(withNew.trackIds.size)
        val updated = withNew.copy(mixer = resizedMixer)
        _state.value = _state.value.copy(pattern = updated)
        stepSequencerEngine.setPattern(updated)
    }

    fun removeTrack(trackId: String) {
        val current = _state.value.pattern
        val withRemoved = current.removeTrack(trackId)
        if (withRemoved.trackIds.size == current.trackIds.size) return
        val resizedMixer = withRemoved.mixer.resize(withRemoved.trackIds.size)
        val updated = withRemoved.copy(mixer = resizedMixer)
        _state.value = _state.value.copy(pattern = updated)
        stepSequencerEngine.setPattern(updated)
    }

    fun toggleNote(pitch: Int, step: Int) {
        val updated = _state.value.pattern.toggleNote(pitch, step, _state.value.selectedInstrument)
        _state.value = _state.value.copy(pattern = updated)
        stepSequencerEngine.setPattern(updated)
    }

    fun suggestBeat(style: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isAiLoading = true)
            delay(400) // Brief AI co-producer synthesis delay

            val bpm = when (style) {
                "Rhumba" -> 98
                "Soukous" -> 128
                "Highlife" -> 115
                "Afro-dance" -> 122
                else -> 120 // Benga
            }

            val steps = List(DrumPattern.DEFAULT_TRACK_IDS.size) { track ->
                MutableList(DrumPattern.STEP_COUNT) { false }.apply {
                    when (style) {
                        "Rhumba" -> when (track) {
                            DrumPattern.TRACK_KICK -> { this[0] = true; this[8] = true }
                            DrumPattern.TRACK_SNARE -> { this[6] = true; this[10] = true; this[14] = true }
                            DrumPattern.TRACK_HAT -> { for (i in 0 until 16 step 2) this[i] = true }
                            DrumPattern.TRACK_BASS -> { this[0] = true; this[6] = true; this[8] = true }
                        }
                        "Soukous" -> when (track) {
                            DrumPattern.TRACK_KICK -> { this[0] = true; this[3] = true; this[8] = true; this[11] = true }
                            DrumPattern.TRACK_SNARE -> { this[4] = true; this[12] = true }
                            DrumPattern.TRACK_HAT -> { for (i in 0 until 16) this[i] = true }
                            DrumPattern.TRACK_BASS -> { this[0] = true; this[3] = true; this[6] = true; this[10] = true; this[12] = true }
                        }
                        "Afro-dance" -> when (track) {
                            DrumPattern.TRACK_KICK -> { this[0] = true; this[4] = true; this[8] = true; this[12] = true }
                            DrumPattern.TRACK_SNARE -> { this[4] = true; this[12] = true }
                            DrumPattern.TRACK_HAT -> { this[2] = true; this[6] = true; this[10] = true; this[14] = true }
                            DrumPattern.TRACK_BASS -> { this[0] = true; this[6] = true; this[8] = true; this[14] = true }
                        }
                        else -> when (track) { // Benga
                            DrumPattern.TRACK_KICK -> { this[0] = true; this[6] = true; this[8] = true; this[12] = true }
                            DrumPattern.TRACK_SNARE -> { this[4] = true; this[10] = true; this[14] = true }
                            DrumPattern.TRACK_HAT -> { for (i in 0 until 16 step 2) this[i] = true }
                            DrumPattern.TRACK_BASS -> { this[0] = true; this[3] = true; this[8] = true; this[11] = true }
                        }
                    }
                }.toList()
            }

            val newPattern = _state.value.pattern.copy(
                name = "$style Beat",
                bpm = bpm,
                steps = steps,
            )
            _state.value = _state.value.copy(
                pattern = newPattern,
                isAiLoading = false,
                message = "AI suggested a $style beat!",
            )
            stepSequencerEngine.setPattern(newPattern)
        }
    }

    fun generateMelody() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isAiLoading = true)
            delay(300)

            val pentatonicPitches = listOf(60, 62, 64, 67, 69, 72) // C, D, E, G, A, C
            val newNotes = _state.value.pattern.notes.toMutableList()

            // Generate 4-note melodic motif
            val stepsToPlace = listOf(0, 4, 8, 12)
            stepsToPlace.forEach { step ->
                val pitch = pentatonicPitches.random()
                newNotes.add(
                    Note(
                        pitch = pitch,
                        startStep = step.toFloat(),
                        lengthSteps = 1f,
                        instrumentType = _state.value.selectedInstrument,
                    )
                )
            }

            val updated = _state.value.pattern.copy(notes = newNotes)
            _state.value = _state.value.copy(
                pattern = updated,
                isAiLoading = false,
                message = "AI generated a melodic motif!",
            )
            stepSequencerEngine.setPattern(updated)
        }
    }

    fun finishBeat() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isAiLoading = true)
            delay(300)

            val currentSteps = _state.value.pattern.steps.map { it.toMutableList() }
            // Add ghost snare on step 11 and hi-hat fills on offbeats
            currentSteps[DrumPattern.TRACK_SNARE][11] = true
            for (s in 1 until 16 step 2) {
                currentSteps[DrumPattern.TRACK_HAT][s] = true
            }

            val updated = _state.value.pattern.copy(steps = currentSteps.map { it.toList() })
            _state.value = _state.value.copy(
                pattern = updated,
                isAiLoading = false,
                message = "AI completed beat variations!",
            )
            stepSequencerEngine.setPattern(updated)
        }
    }

    fun setBpm(bpm: Int) {
        val updated = _state.value.pattern.withBpm(bpm)
        _state.value = _state.value.copy(pattern = updated)
        stepSequencerEngine.setPattern(updated)
    }

    fun setVolume(track: Int, volume: Float) {
        val updated = _state.value.pattern.setVolume(track, volume)
        _state.value = _state.value.copy(pattern = updated)
        stepSequencerEngine.setPattern(updated)
    }

    fun setName(name: String) {
        val updated = _state.value.pattern.withName(name)
        _state.value = _state.value.copy(pattern = updated)
        stepSequencerEngine.setPattern(updated)
    }

    fun setPan(track: Int, pan: Float) {
        val newMixer = _state.value.pattern.mixer.setPan(track, pan)
        val updated = _state.value.pattern.withMixer(newMixer)
        _state.value = _state.value.copy(pattern = updated)
        stepSequencerEngine.setPattern(updated)
    }

    fun toggleMute(track: Int) {
        val newMixer = _state.value.pattern.mixer.toggleMute(track)
        val updated = _state.value.pattern.withMixer(newMixer)
        _state.value = _state.value.copy(pattern = updated)
        stepSequencerEngine.setPattern(updated)
    }

    fun toggleSolo(track: Int) {
        val newMixer = _state.value.pattern.mixer.toggleSolo(track)
        val updated = _state.value.pattern.withMixer(newMixer)
        _state.value = _state.value.copy(pattern = updated)
        stepSequencerEngine.setPattern(updated)
    }

    fun setMasterVolume(vol: Float) {
        val newMixer = _state.value.pattern.mixer.setMasterVolume(vol)
        val updated = _state.value.pattern.withMixer(newMixer)
        _state.value = _state.value.copy(pattern = updated)
        stepSequencerEngine.setPattern(updated)
    }

    fun startVoiceRecording() {
        viewModelScope.launch {
            val takeId = UUID.randomUUID().toString()
            val file = voiceStore.createNewVoiceFile(takeId)
            currentRecordingFile = file
            currentRecordingTakeId = takeId
            voiceRecorder.startRecording(file)
            play()
        }
    }

    fun stopVoiceRecording() {
        voiceRecorder.stopRecording()
        stop()
        val file = currentRecordingFile
        val takeId = currentRecordingTakeId
        if (file != null && takeId != null) {
            val takeCount = _state.value.voiceTracks.size + 1
            val newTrack = VoiceTrack(
                takeId = takeId,
                name = "Vocal Take #$takeCount",
                filePath = file.absolutePath,
            )
            _state.value = _state.value.copy(
                voiceTracks = _state.value.voiceTracks + newTrack
            )
        }
        currentRecordingFile = null
        currentRecordingTakeId = null
    }

    fun deleteVoiceTrack(takeId: String) {
        viewModelScope.launch {
            val track = _state.value.voiceTracks.find { it.takeId == takeId }
            if (track != null) {
                voiceStore.deleteVoiceFile(track.filePath)
                _state.value = _state.value.copy(
                    voiceTracks = _state.value.voiceTracks.filter { it.takeId != takeId }
                )
            }
        }
    }

    fun setVoiceVolume(takeId: String, vol: Float) {
        val updated = _state.value.voiceTracks.map {
            if (it.takeId == takeId) it.copy(volume = vol) else it
        }
        _state.value = _state.value.copy(voiceTracks = updated)
    }

    fun play() {
        stepSequencerEngine.start()
    }

    fun stop() {
        stepSequencerEngine.stop()
    }

    fun savePattern() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            patternStore.save(_state.value.pattern)
            loadSavedPatterns()
            _state.value = _state.value.copy(
                isSaving = false,
                message = "Beat '${_state.value.pattern.name}' saved!",
            )
        }
    }

    fun exportWav(onDone: (File) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isExporting = true)
            val wavFile = wavExporter.exportToWav(_state.value.pattern)
            _state.value = _state.value.copy(
                isExporting = false,
                exportedFile = wavFile,
                message = "Exported to ${wavFile.name}",
            )
            onDone(wavFile)
        }
    }

    fun exportM4a(onDone: (File) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isExporting = true)
            val wavFile = wavExporter.exportToWav(_state.value.pattern)
            val m4aFile = File(wavFile.parent, wavFile.nameWithoutExtension + ".m4a")
            m4aExporter.convertWavToM4a(wavFile, m4aFile)
            _state.value = _state.value.copy(
                isExporting = false,
                exportedFile = m4aFile,
                message = "Exported to ${m4aFile.name}",
            )
            onDone(m4aFile)
        }
    }

    fun loadPattern(pattern: DrumPattern) {
        stop()
        _state.value = _state.value.copy(pattern = pattern)
        stepSequencerEngine.setPattern(pattern)
    }

    fun deletePattern(id: String) {
        viewModelScope.launch {
            patternStore.delete(id)
            loadSavedPatterns()
        }
    }

    fun newPattern() {
        stop()
        val fresh = DrumPattern.defaultPattern()
        _state.value = _state.value.copy(pattern = fresh)
        stepSequencerEngine.setPattern(fresh)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    override fun onCleared() {
        super.onCleared()
        stepSequencerEngine.release()
    }
}

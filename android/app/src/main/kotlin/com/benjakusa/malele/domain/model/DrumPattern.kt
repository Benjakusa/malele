package com.benjakusa.malele.domain.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class DrumPattern(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "My Beat",
    val bpm: Int = 120,
    val trackIds: List<String> = DEFAULT_TRACK_IDS,
    val steps: List<List<Boolean>> = List(DEFAULT_TRACK_IDS.size) { List(STEP_COUNT) { false } },
    val volumes: List<Float> = List(DEFAULT_TRACK_IDS.size) { 1.0f },
    val swing: Float = 0f,
    val mixer: MixerState = MixerState(),
    val notes: List<Note> = emptyList(),
    val instrumentParams: Map<Int, InstrumentParams> = emptyMap(),
) {
    init {
        require(bpm in MIN_BPM..MAX_BPM) { "BPM must be between $MIN_BPM and $MAX_BPM" }
        require(trackIds.isNotEmpty()) { "Must have at least one track" }
        require(trackIds.size <= MAX_TRACKS) { "At most $MAX_TRACKS tracks" }
        require(trackIds.distinct().size == trackIds.size) { "Track IDs must be unique" }
        require(trackIds.all { it in ALLOWED_TRACKS }) { "Unknown track id in $trackIds" }
        require(steps.size == trackIds.size) { "steps size ${steps.size} != trackIds size ${trackIds.size}" }
        require(volumes.size == trackIds.size) { "volumes size ${volumes.size} != trackIds size ${trackIds.size}" }
        steps.forEach { require(it.size == STEP_COUNT) { "Each track must have $STEP_COUNT steps" } }
    }

    fun toggle(track: Int, step: Int): DrumPattern {
        if (track !in trackIds.indices || step !in 0 until STEP_COUNT) return this
        val newSteps = steps.mapIndexed { tIndex, trackSteps ->
            if (tIndex == track) {
                trackSteps.mapIndexed { sIndex, active ->
                    if (sIndex == step) !active else active
                }
            } else {
                trackSteps
            }
        }
        return copy(steps = newSteps)
    }

    fun toggleNote(pitch: Int, step: Int, instrumentType: String): DrumPattern {
        val existingIndex = notes.indexOfFirst {
            it.pitch == pitch && it.startStep.toInt() == step && it.instrumentType == instrumentType
        }
        val newNotes = if (existingIndex >= 0) {
            notes.filterIndexed { index, _ -> index != existingIndex }
        } else {
            notes + Note(
                pitch = pitch,
                startStep = step.toFloat(),
                lengthSteps = 1.0f,
                instrumentType = instrumentType,
            )
        }
        return copy(notes = newNotes)
    }

    fun setVolume(track: Int, volume: Float): DrumPattern {
        if (track !in trackIds.indices) return this
        val clamped = volume.coerceIn(0f, 1f)
        val newVolumes = volumes.mapIndexed { tIndex, v ->
            if (tIndex == track) clamped else v
        }
        return copy(volumes = newVolumes)
    }

    fun addTrack(trackId: String): DrumPattern {
        if (trackId !in ALLOWED_TRACKS) return this
        if (trackId in trackIds) return this
        if (trackIds.size >= MAX_TRACKS) return this
        return copy(
            trackIds = trackIds + trackId,
            steps = steps + listOf(List<Boolean>(STEP_COUNT) { false }),
            volumes = volumes + 1.0f,
        )
    }

    fun removeTrack(trackId: String): DrumPattern {
        val idx = trackIds.indexOf(trackId)
        if (idx < 0) return this
        if (trackIds.size <= MIN_TRACKS) return this
        return copy(
            trackIds = trackIds.filterIndexed { i, _ -> i != idx },
            steps = steps.filterIndexed { i, _ -> i != idx },
            volumes = volumes.filterIndexed { i, _ -> i != idx },
        )
    }

    fun withBpm(newBpm: Int): DrumPattern {
        return copy(bpm = newBpm.coerceIn(MIN_BPM, MAX_BPM))
    }

    fun withName(newName: String): DrumPattern {
        return copy(name = newName.ifBlank { "My Beat" })
    }

    fun withMixer(newMixer: MixerState): DrumPattern {
        return copy(mixer = newMixer)
    }

    fun withInstrumentParams(track: Int, params: InstrumentParams): DrumPattern {
        val newMap = instrumentParams.toMutableMap()
        newMap[track] = params
        return copy(instrumentParams = newMap)
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("bpm", bpm)
        json.put("swing", swing.toDouble())

        val trackIdsArray = JSONArray()
        trackIds.forEach { trackIdsArray.put(it) }
        json.put("trackIds", trackIdsArray)

        val stepsArray = JSONArray()
        steps.forEach { trackSteps ->
            val trackArray = JSONArray()
            trackSteps.forEach { active -> trackArray.put(active) }
            stepsArray.put(trackArray)
        }
        json.put("steps", stepsArray)

        val volumesArray = JSONArray()
        volumes.forEach { vol -> volumesArray.put(vol.toDouble()) }
        json.put("volumes", volumesArray)

        json.put("mixer", mixer.toJson())

        val notesArray = JSONArray()
        notes.forEach { note -> notesArray.put(note.toJson()) }
        json.put("notes", notesArray)

        val paramsJson = JSONObject()
        instrumentParams.forEach { (track, params) ->
            paramsJson.put(track.toString(), params.toJson())
        }
        json.put("instrumentParams", paramsJson)

        return json.toString()
    }

    companion object {
        const val STEP_COUNT = 16
        const val MIN_BPM = 40
        const val MAX_BPM = 200
        const val MIN_TRACKS = 1
        const val MAX_TRACKS = 16

        const val TRACK_KICK = 0
        const val TRACK_SNARE = 1
        const val TRACK_HAT = 2
        const val TRACK_BASS = 3

        val DEFAULT_TRACK_IDS: List<String> = listOf("kick", "snare", "hat", "bass")

        val ALLOWED_TRACKS: Set<String> = setOf(
            "kick", "snare", "hat", "bass",
            "conga", "djembe", "shaker", "talking_drum", "kalimba"
        )

        val TRACK_NAME_BY_ID: Map<String, String> = mapOf(
            "kick" to "Kick",
            "snare" to "Snare",
            "hat" to "Hi-Hat",
            "bass" to "Bass",
            "conga" to "Conga",
            "djembe" to "Djembe",
            "shaker" to "Shaker",
            "talking_drum" to "Talking Drum",
            "kalimba" to "Kalimba"
        )

        fun displayName(trackId: String): String =
            TRACK_NAME_BY_ID[trackId] ?: trackId

        fun defaultPattern(): DrumPattern {
            val steps = List(DEFAULT_TRACK_IDS.size) { track ->
                MutableList(STEP_COUNT) { false }.apply {
                    when (track) {
                        TRACK_KICK -> { this[0] = true; this[6] = true; this[8] = true; this[12] = true }
                        TRACK_SNARE -> { this[4] = true; this[10] = true; this[14] = true }
                        TRACK_HAT -> { for (i in 0 until 16 step 2) this[i] = true }
                        TRACK_BASS -> { this[0] = true; this[3] = true; this[8] = true; this[11] = true }
                    }
                }.toList()
            }
            val defaultNotes = listOf(
                Note(pitch = 48, startStep = 0f, instrumentType = "bass"),
                Note(pitch = 52, startStep = 4f, instrumentType = "bass"),
                Note(pitch = 55, startStep = 8f, instrumentType = "bass"),
                Note(pitch = 53, startStep = 12f, instrumentType = "bass"),
            )
            return DrumPattern(name = "Benga Beat", bpm = 120, steps = steps, notes = defaultNotes)
        }

        fun fromJson(jsonStr: String): DrumPattern {
            val json = JSONObject(jsonStr)
            val id = json.optString("id", UUID.randomUUID().toString())
            val name = json.optString("name", "My Beat")
            val bpm = json.optInt("bpm", 120)
            val swing = json.optDouble("swing", 0.0).toFloat()

            val trackIds = mutableListOf<String>()
            val trackIdsArray = json.optJSONArray("trackIds")
            if (trackIdsArray != null) {
                for (i in 0 until trackIdsArray.length()) {
                    val tid = trackIdsArray.optString(i)
                    if (tid.isNotBlank() && tid in ALLOWED_TRACKS && tid !in trackIds) {
                        trackIds.add(tid)
                    }
                }
            }
            if (trackIds.isEmpty()) {
                trackIds.addAll(DEFAULT_TRACK_IDS)
            }

            val steps = mutableListOf<List<Boolean>>()
            val stepsArray = json.optJSONArray("steps")
            if (stepsArray != null) {
                val rowCount = minOf(stepsArray.length(), trackIds.size)
                for (i in 0 until rowCount) {
                    val trackArray = stepsArray.getJSONArray(i)
                    val trackSteps = mutableListOf<Boolean>()
                    for (j in 0 until minOf(trackArray.length(), STEP_COUNT)) {
                        trackSteps.add(trackArray.getBoolean(j))
                    }
                    while (trackSteps.size < STEP_COUNT) trackSteps.add(false)
                    steps.add(trackSteps)
                }
            }
            while (steps.size < trackIds.size) {
                steps.add(List(STEP_COUNT) { false })
            }

            val volumes = mutableListOf<Float>()
            val volumesArray = json.optJSONArray("volumes")
            if (volumesArray != null) {
                val volCount = minOf(volumesArray.length(), trackIds.size)
                for (i in 0 until volCount) {
                    volumes.add(volumesArray.getDouble(i).toFloat())
                }
            }
            while (volumes.size < trackIds.size) {
                volumes.add(1.0f)
            }

            val mixer = MixerState.fromJson(json.optJSONObject("mixer"))

            val notes = mutableListOf<Note>()
            val notesArray = json.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    Note.fromJson(notesArray.optJSONObject(i))?.let { notes.add(it) }
                }
            }

            val instrumentParams = mutableMapOf<Int, InstrumentParams>()
            val paramsJson = json.optJSONObject("instrumentParams")
            if (paramsJson != null) {
                paramsJson.keys().forEach { trackKey ->
                    val t = trackKey.toIntOrNull()
                    if (t != null) {
                        instrumentParams[t] = InstrumentParams.fromJson(paramsJson.optJSONObject(trackKey))
                    }
                }
            }

            return DrumPattern(
                id = id,
                name = name,
                bpm = bpm.coerceIn(MIN_BPM, MAX_BPM),
                trackIds = trackIds,
                steps = steps,
                volumes = volumes,
                swing = swing,
                mixer = mixer,
                notes = notes,
                instrumentParams = instrumentParams,
            )
        }
    }
}

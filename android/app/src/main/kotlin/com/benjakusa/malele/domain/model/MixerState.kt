package com.benjakusa.malele.domain.model

import org.json.JSONArray
import org.json.JSONObject

data class MixerState(
    val trackCount: Int = 4,
    val pans: List<Float> = List(trackCount) { 0.0f },
    val mutes: List<Boolean> = List(trackCount) { false },
    val solos: List<Boolean> = List(trackCount) { false },
    val masterVolume: Float = 1.0f,
) {
    fun setPan(track: Int, pan: Float): MixerState {
        if (track !in 0 until trackCount) return this
        val clamped = pan.coerceIn(-1.0f, 1.0f)
        val newPans = pans.mapIndexed { i, p -> if (i == track) clamped else p }
        return copy(pans = newPans)
    }

    fun toggleMute(track: Int): MixerState {
        if (track !in 0 until trackCount) return this
        val newMutes = mutes.mapIndexed { i, m -> if (i == track) !m else m }
        return copy(mutes = newMutes)
    }

    fun toggleSolo(track: Int): MixerState {
        if (track !in 0 until trackCount) return this
        val newSolos = solos.mapIndexed { i, s -> if (i == track) !s else s }
        return copy(solos = newSolos)
    }

    fun setMasterVolume(vol: Float): MixerState {
        return copy(masterVolume = vol.coerceIn(0f, 1f))
    }

    fun resize(newTrackCount: Int): MixerState {
        if (newTrackCount == trackCount) return this
        fun <T> padOrTrim(list: List<T>, default: T): List<T> =
            if (list.size >= newTrackCount) list.take(newTrackCount)
            else list + List(newTrackCount - list.size) { default }
        return copy(
            trackCount = newTrackCount,
            pans = padOrTrim(pans, 0.0f),
            mutes = padOrTrim(mutes, false),
            solos = padOrTrim(solos, false),
        )
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        val pansArr = JSONArray()
        pans.forEach { pansArr.put(it.toDouble()) }
        json.put("pans", pansArr)

        val mutesArr = JSONArray()
        mutes.forEach { mutesArr.put(it) }
        json.put("mutes", mutesArr)

        val solosArr = JSONArray()
        solos.forEach { solosArr.put(it) }
        json.put("solos", solosArr)

        json.put("masterVolume", masterVolume.toDouble())
        return json
    }

    companion object {
        fun fromJson(json: JSONObject?): MixerState {
            if (json == null) return MixerState()
            val pansArr = json.optJSONArray("pans")
            val mutesArr = json.optJSONArray("mutes")
            val solosArr = json.optJSONArray("solos")
            val masterVolume = json.optDouble("masterVolume", 1.0).toFloat()

            val inferredCount = listOfNotNull(
                pansArr?.length(),
                mutesArr?.length(),
                solosArr?.length()
            ).maxOrNull() ?: 4

            val pans = mutableListOf<Float>()
            if (pansArr != null) {
                for (i in 0 until minOf(pansArr.length(), inferredCount)) {
                    pans.add(pansArr.getDouble(i).toFloat())
                }
            }
            while (pans.size < inferredCount) pans.add(0.0f)

            val mutes = mutableListOf<Boolean>()
            if (mutesArr != null) {
                for (i in 0 until minOf(mutesArr.length(), inferredCount)) {
                    mutes.add(mutesArr.getBoolean(i))
                }
            }
            while (mutes.size < inferredCount) mutes.add(false)

            val solos = mutableListOf<Boolean>()
            if (solosArr != null) {
                for (i in 0 until minOf(solosArr.length(), inferredCount)) {
                    solos.add(solosArr.getBoolean(i))
                }
            }
            while (solos.size < inferredCount) solos.add(false)

            return MixerState(
                trackCount = inferredCount,
                pans = pans,
                mutes = mutes,
                solos = solos,
                masterVolume = masterVolume,
            )
        }
    }
}

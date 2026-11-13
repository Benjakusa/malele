package com.benjakusa.malele.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.benjakusa.malele.ui.screens.create.SectionInput
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.createDraftDataStore by preferencesDataStore(name = "create_draft")

data class CreateDraft(
    val prompt: String = "",
    val genre: String? = null,
    val vocals: Boolean = true,
    val instrumental: Boolean = false,
    val language: String = "en",
    val durationSeconds: Int = 120,
    val sections: List<SectionInput> = emptyList(),
) {
    fun isEmpty(): Boolean = prompt.isBlank() && sections.isEmpty()
}

@Singleton
class CreateDraftStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("draft_json")

    suspend fun save(draft: CreateDraft) {
        context.createDraftDataStore.edit { prefs ->
            prefs[key] = serialize(draft)
        }
    }

    suspend fun load(): CreateDraft? {
        val raw = context.createDraftDataStore.data.first()[key] ?: return null
        return try { deserialize(raw) } catch (e: Exception) { null }
    }

    suspend fun clear() {
        context.createDraftDataStore.edit { prefs -> prefs.remove(key) }
    }

    private fun serialize(draft: CreateDraft): String {
        val obj = JSONObject()
        obj.put("prompt", draft.prompt)
        obj.put("genre", draft.genre ?: JSONObject.NULL)
        obj.put("vocals", draft.vocals)
        obj.put("instrumental", draft.instrumental)
        obj.put("language", draft.language)
        obj.put("durationSeconds", draft.durationSeconds)
        val arr = JSONArray()
        draft.sections.forEach { s ->
            val so = JSONObject()
            so.put("id", s.id)
            so.put("name", s.name)
            so.put("durationSeconds", s.durationSeconds)
            so.put("lyrics", s.lyrics)
            so.put("instruments", s.instruments)
            so.put("vocals", s.vocals)
            so.put("mood", s.mood)
            so.put("negative", s.negative)
            arr.put(so)
        }
        obj.put("sections", arr)
        return obj.toString()
    }

    private fun deserialize(raw: String): CreateDraft {
        val obj = JSONObject(raw)
        val sections = mutableListOf<SectionInput>()
        val arr = obj.optJSONArray("sections")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val so = arr.getJSONObject(i)
                sections.add(
                    SectionInput(
                        id = so.optString("id", java.util.UUID.randomUUID().toString()),
                        name = so.optString("name", "Verse 1"),
                        durationSeconds = so.optInt("durationSeconds", 20),
                        lyrics = so.optString("lyrics", ""),
                        instruments = so.optString("instruments", ""),
                        vocals = so.optString("vocals", ""),
                        mood = so.optString("mood", ""),
                        negative = so.optString("negative", ""),
                    )
                )
            }
        }
        return CreateDraft(
            prompt = obj.optString("prompt", ""),
            genre = if (obj.isNull("genre")) null else obj.optString("genre", ""),
            vocals = obj.optBoolean("vocals", true),
            instrumental = obj.optBoolean("instrumental", false),
            language = obj.optString("language", "en"),
            durationSeconds = obj.optInt("durationSeconds", 120),
            sections = sections,
        )
    }
}

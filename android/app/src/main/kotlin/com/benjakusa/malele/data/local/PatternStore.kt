package com.benjakusa.malele.data.local

import android.content.Context
import com.benjakusa.malele.domain.model.DrumPattern
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatternStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val patternsDir: File
        get() = File(context.filesDir, "patterns").also { if (!it.exists()) it.mkdirs() }

    suspend fun save(pattern: DrumPattern): Unit = withContext(Dispatchers.IO) {
        val file = File(patternsDir, "${pattern.id}.pattern.json")
        file.writeText(pattern.toJson())
    }

    suspend fun list(): List<DrumPattern> = withContext(Dispatchers.IO) {
        patternsDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    DrumPattern.fromJson(file.readText())
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.name }
            ?: emptyList()
    }

    suspend fun load(id: String): DrumPattern? = withContext(Dispatchers.IO) {
        val file = File(patternsDir, "$id.pattern.json")
        if (!file.exists()) return@withContext null
        try {
            DrumPattern.fromJson(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(patternsDir, "$id.pattern.json")
        if (file.exists()) file.delete() else false
    }
}

package com.benjakusa.malele.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val voicesDir: File
        get() = File(context.filesDir, "voices").also { if (!it.exists()) it.mkdirs() }

    suspend fun createNewVoiceFile(takeId: String): File = withContext(Dispatchers.IO) {
        File(voicesDir, "voice_$takeId.wav")
    }

    suspend fun deleteVoiceFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) file.delete() else false
    }
}

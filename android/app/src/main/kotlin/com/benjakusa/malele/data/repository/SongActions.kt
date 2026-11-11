package com.benjakusa.malele.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.benjakusa.malele.BuildConfig
import com.benjakusa.malele.data.local.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongActions @Inject constructor(
    private val tokenStore: TokenStore,
) {
    private val httpClient = OkHttpClient()

    private fun audioUrl(jobId: String): String {
        val base = BuildConfig.BACKEND_URL.trimEnd('/')
        return "$base/v1/generations/$jobId/audio"
    }

    suspend fun downloadToCache(jobId: String, title: String): File? = withContext(Dispatchers.IO) {
        try {
            val token = tokenStore.getAccessToken() ?: return@withContext null
            val request = Request.Builder()
                .url(audioUrl(jobId))
                .header("Authorization", "Bearer $token")
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body ?: return@withContext null
            val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "_").take(50)
            val file = File(System.getProperty("java.io.tmpdir") ?: "/tmp", "malele_$safeTitle.wav")
            file.outputStream().use { body.byteStream().copyTo(it) }
            file
        } catch (e: Exception) { null }
    }

    fun shareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_TEXT, "Made with Malele")
        }
    }

    suspend fun saveToDevice(context: Context, jobId: String, title: String): Uri? = withContext(Dispatchers.IO) {
        val file = downloadToCache(jobId, title) ?: return@withContext null
        try {
            val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "_").take(50)
            val fileName = "malele_$safeTitle.wav"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Malele")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
                resolver.openOutputStream(uri)?.use { out -> file.inputStream().copyTo(out) }
                uri
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Malele")
                dir.mkdirs()
                val dest = File(dir, fileName)
                FileOutputStream(dest).use { out -> file.inputStream().copyTo(out) }
                Uri.fromFile(dest)
            }
        } catch (e: Exception) { null }
    }
}

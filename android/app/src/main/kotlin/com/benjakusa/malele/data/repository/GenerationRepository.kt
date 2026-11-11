package com.benjakusa.malele.data.repository

import com.benjakusa.malele.data.network.ApiClient
import com.benjakusa.malele.data.network.dto.ErrorBody
import com.benjakusa.malele.data.network.dto.GenerationSubmitRequest
import com.benjakusa.malele.data.network.dto.JobDto
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

sealed class GenerationResult {
    data class Success(val job: JobDto) : GenerationResult()
    data class Error(val code: String, val message: String) : GenerationResult()
}

@Singleton
class GenerationRepository @Inject constructor(
    private val apiClient: ApiClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun submit(
        prompt: String,
        genre: String?,
        vocals: Boolean,
        instrumental: Boolean,
        durationSeconds: Int,
        language: String?,
    ): GenerationResult {
        return try {
            val response = apiClient.generationApi.submit(
                GenerationSubmitRequest(
                    prompt = prompt,
                    genre = genre,
                    vocals = vocals,
                    instrumental = instrumental,
                    durationSeconds = durationSeconds,
                    language = language,
                )
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) GenerationResult.Success(body.job)
                else GenerationResult.Error("EMPTY", "Empty response")
            } else {
                val (code, msg) = parseError(response.code(), response.errorBody()?.string())
                GenerationResult.Error(code, msg)
            }
        } catch (e: Exception) {
            GenerationResult.Error("NETWORK_ERROR", e.message ?: "Network error")
        }
    }

    suspend fun getJob(jobId: String): JobDto? {
        return try {
            val response = apiClient.generationApi.getJob(jobId)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) { null }
    }

    suspend fun pollJob(jobId: String): JobDto? {
        return try {
            val response = apiClient.generationApi.pollJob(jobId)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) { null }
    }

    private fun parseError(code: Int, body: String?): Pair<String, String> {
        return try {
            val err = json.decodeFromString<ErrorBody>(body ?: "")
            val detail = err.error
            if (detail != null) detail.code to detail.message else "HTTP_$code" to "HTTP error"
        } catch (e: Exception) {
            "HTTP_$code" to "HTTP $code"
        }
    }
}

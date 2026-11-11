package com.benjakusa.malele.data.network

import com.benjakusa.malele.data.network.dto.LyricsGenerateRequest
import com.benjakusa.malele.data.network.dto.LyricsGenerateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LyricsApi {
    @POST("v1/lyrics/generate")
    suspend fun generate(@Body body: LyricsGenerateRequest): Response<LyricsGenerateResponse>
}

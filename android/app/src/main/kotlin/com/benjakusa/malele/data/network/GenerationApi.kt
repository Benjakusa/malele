package com.benjakusa.malele.data.network

import com.benjakusa.malele.data.network.dto.GenerationSubmitRequest
import com.benjakusa.malele.data.network.dto.GenerationSubmitResponse
import com.benjakusa.malele.data.network.dto.JobDto
import com.benjakusa.malele.data.network.dto.SaveSongRequest
import com.benjakusa.malele.data.network.dto.SongDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GenerationApi {
    @POST("v1/generations")
    suspend fun submit(@Body body: GenerationSubmitRequest): Response<GenerationSubmitResponse>

    @GET("v1/generations/{jobId}")
    suspend fun getJob(@Path("jobId") jobId: String): Response<JobDto>

    @POST("v1/generations/{jobId}/poll")
    suspend fun pollJob(@Path("jobId") jobId: String): Response<JobDto>

    @POST("v1/generations/{jobId}/cancel")
    suspend fun cancelJob(@Path("jobId") jobId: String): Response<JobDto>

    @POST("v1/generations/{jobId}/save")
    suspend fun saveJob(
        @Path("jobId") jobId: String,
        @Body body: SaveSongRequest,
    ): Response<SongDto>

    @GET("v1/songs")
    suspend fun listSongs(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): Response<List<SongDto>>
}

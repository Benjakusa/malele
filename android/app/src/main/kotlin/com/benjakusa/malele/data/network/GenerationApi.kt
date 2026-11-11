package com.benjakusa.malele.data.network

import com.benjakusa.malele.data.network.dto.GenerationSubmitRequest
import com.benjakusa.malele.data.network.dto.GenerationSubmitResponse
import com.benjakusa.malele.data.network.dto.JobDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GenerationApi {
    @POST("v1/generations")
    suspend fun submit(@Body body: GenerationSubmitRequest): Response<GenerationSubmitResponse>

    @GET("v1/generations/{jobId}")
    suspend fun getJob(@Path("jobId") jobId: String): Response<JobDto>

    @POST("v1/generations/{jobId}/poll")
    suspend fun pollJob(@Path("jobId") jobId: String): Response<JobDto>

    @POST("v1/generations/{jobId}/cancel")
    suspend fun cancelJob(@Path("jobId") jobId: String): Response<JobDto>
}

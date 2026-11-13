package com.benjakusa.malele.data.network

import com.benjakusa.malele.data.network.dto.UpdateMeRequest
import com.benjakusa.malele.data.network.dto.UserProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApi {
    @GET("v1/me")
    suspend fun getMe(): Response<UserProfileDto>

    @PATCH("v1/me")
    suspend fun updateMe(@Body body: UpdateMeRequest): Response<UserProfileDto>
}

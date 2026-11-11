package com.benjakusa.malele.data.network

import com.benjakusa.malele.data.network.dto.AuthResponse
import com.benjakusa.malele.data.network.dto.LoginRequest
import com.benjakusa.malele.data.network.dto.RefreshRequest
import com.benjakusa.malele.data.network.dto.RegisterRequest
import com.benjakusa.malele.data.network.dto.TokenPair
import com.benjakusa.malele.data.network.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<TokenPair>

    @POST("v1/auth/logout")
    suspend fun logout(@Body body: RefreshRequest): Response<Unit>

    @GET("v1/me")
    suspend fun me(): Response<UserDto>
}

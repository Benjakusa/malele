package com.benjakusa.malele.data.repository

import com.benjakusa.malele.data.network.ApiClient
import com.benjakusa.malele.data.network.dto.UpdateMeRequest
import com.benjakusa.malele.data.network.dto.UserProfileDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiClient: ApiClient,
) {
    suspend fun getMe(): UserProfileDto? {
        return try {
            val response = apiClient.userApi.getMe()
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) { null }
    }

    suspend fun updateDisplayName(name: String): UserProfileDto? {
        return try {
            val response = apiClient.userApi.updateMe(UpdateMeRequest(displayName = name))
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) { null }
    }
}

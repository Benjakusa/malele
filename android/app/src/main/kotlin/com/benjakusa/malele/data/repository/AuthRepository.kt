package com.benjakusa.malele.data.repository

import com.benjakusa.malele.data.local.TokenStore
import com.benjakusa.malele.data.network.ApiClient
import com.benjakusa.malele.data.network.dto.ErrorBody
import com.benjakusa.malele.data.network.dto.LoginRequest
import com.benjakusa.malele.data.network.dto.RefreshRequest
import com.benjakusa.malele.data.network.dto.RegisterRequest
import com.benjakusa.malele.data.network.dto.UserDto
import com.benjakusa.malele.domain.model.AuthResult
import com.benjakusa.malele.domain.model.User
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val tokenStore: TokenStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun register(email: String, password: String, displayName: String?): AuthResult {
        return try {
            val response = apiClient.authApi.register(
                RegisterRequest(email = email, password = password, displayName = displayName)
            )
            if (response.isSuccessful) {
                val body = response.body() ?: return AuthResult.Error("EMPTY", "Empty response")
                tokenStore.saveTokens(body.tokens.accessToken, body.tokens.refreshToken)
                AuthResult.Success(body.user.toDomain())
            } else {
                val (code, msg) = parseError(response.code(), response.errorBody()?.string())
                AuthResult.Error(code, msg)
            }
        } catch (e: Exception) {
            AuthResult.Error("NETWORK_ERROR", e.message ?: "Network error")
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = apiClient.authApi.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body() ?: return AuthResult.Error("EMPTY", "Empty response")
                tokenStore.saveTokens(body.tokens.accessToken, body.tokens.refreshToken)
                AuthResult.Success(body.user.toDomain())
            } else {
                val (code, msg) = parseError(response.code(), response.errorBody()?.string())
                AuthResult.Error(code, msg)
            }
        } catch (e: Exception) {
            AuthResult.Error("NETWORK_ERROR", e.message ?: "Network error")
        }
    }

    suspend fun logout() {
        try {
            val refresh = tokenStore.getRefreshToken()
            if (refresh != null) {
                apiClient.authApi.logout(RefreshRequest(refresh))
            }
        } catch (_: Exception) { }
        tokenStore.clear()
    }

    suspend fun refresh(): Boolean {
        val refresh = tokenStore.getRefreshToken() ?: return false
        return try {
            val response = apiClient.authApi.refresh(RefreshRequest(refresh))
            if (response.isSuccessful) {
                val body = response.body() ?: return false
                tokenStore.saveTokens(body.accessToken, body.refreshToken)
                true
            } else false
        } catch (e: Exception) { false }
    }

    suspend fun me(): User? {
        return try {
            val response = apiClient.authApi.me()
            if (response.isSuccessful) response.body()?.toDomain() else null
        } catch (e: Exception) { null }
    }

    fun isLoggedIn(): Boolean = tokenStore.hasTokens()

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

private fun UserDto.toDomain() = User(
    id = id,
    email = email,
    role = role,
    displayName = displayName,
    emailVerified = emailVerified,
    isActive = isActive,
)

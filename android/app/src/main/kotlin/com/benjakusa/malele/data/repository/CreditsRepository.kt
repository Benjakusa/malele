package com.benjakusa.malele.data.repository

import com.benjakusa.malele.data.network.ApiClient
import com.benjakusa.malele.data.network.dto.CreditBalanceDto
import com.benjakusa.malele.data.network.dto.CreditPurchaseRequest
import com.benjakusa.malele.data.network.dto.CreditPurchaseResponse
import com.benjakusa.malele.data.network.dto.CreditVerifyRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditsRepository @Inject constructor(
    private val apiClient: ApiClient,
) {
    suspend fun getBalance(): CreditBalanceDto? = try {
        val response = apiClient.creditsApi.getBalance()
        if (response.isSuccessful) response.body() else null
    } catch (e: Exception) { null }

    suspend fun purchase(phone: String, amountKes: Int): Result<CreditPurchaseResponse> = try {
        val response = apiClient.creditsApi.purchase(
            CreditPurchaseRequest(phone = phone, amountKes = amountKes)
        )
        if (response.isSuccessful) {
            response.body()?.let { Result.success(it) }
                ?: Result.failure(Exception("Empty response"))
        } else {
            Result.failure(Exception("HTTP ${response.code()}: ${response.errorBody()?.string().orEmpty()}"))
        }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun verify(checkoutRequestId: String): String? = try {
        val response = apiClient.creditsApi.verify(
            CreditVerifyRequest(checkoutRequestId = checkoutRequestId)
        )
        if (response.isSuccessful) response.body()?.status else null
    } catch (e: Exception) { null }
}

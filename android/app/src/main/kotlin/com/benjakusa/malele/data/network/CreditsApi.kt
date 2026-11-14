package com.benjakusa.malele.data.network

import com.benjakusa.malele.data.network.dto.CreditBalanceDto
import com.benjakusa.malele.data.network.dto.CreditPurchaseRequest
import com.benjakusa.malele.data.network.dto.CreditPurchaseResponse
import com.benjakusa.malele.data.network.dto.CreditVerifyRequest
import com.benjakusa.malele.data.network.dto.CreditVerifyResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CreditsApi {
    @GET("v1/credits/balance")
    suspend fun getBalance(): Response<CreditBalanceDto>

    @POST("v1/credits/purchase")
    suspend fun purchase(@Body body: CreditPurchaseRequest): Response<CreditPurchaseResponse>

    @POST("v1/credits/verify")
    suspend fun verify(@Body body: CreditVerifyRequest): Response<CreditVerifyResponse>
}

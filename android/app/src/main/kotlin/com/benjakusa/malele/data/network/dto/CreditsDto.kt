package com.benjakusa.malele.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreditBalanceDto(
    val balance: Int,
    val reserved: Int,
    val available: Int,
)

@Serializable
data class CreditPurchaseRequest(
    val phone: String,
    @SerialName("amount_kes") val amountKes: Int,
)

@Serializable
data class CreditPurchaseResponse(
    @SerialName("checkout_request_id") val checkoutRequestId: String,
    @SerialName("amount_kes") val amountKes: Int,
    @SerialName("amount_credits") val amountCredits: Int,
    val message: String = "",
)

@Serializable
data class CreditVerifyRequest(
    @SerialName("checkout_request_id") val checkoutRequestId: String,
)

@Serializable
data class CreditVerifyResponse(
    val status: String,
)

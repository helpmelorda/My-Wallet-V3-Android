package com.blockchain.api.trade.data

import kotlinx.serialization.Serializable

@Serializable
data class NextPaymentRecurringBuyResponse(
    val nextPayments: List<NextPaymentRecurringBuy>
)

@Serializable
data class NextPaymentRecurringBuy(
    val period: String,
    val nextPayment: String
)
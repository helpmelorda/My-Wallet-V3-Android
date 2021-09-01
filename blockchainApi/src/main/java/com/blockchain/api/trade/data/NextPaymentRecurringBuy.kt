package com.blockchain.api.trade.data

import kotlinx.serialization.Serializable

@Serializable
data class NextPaymentRecurringBuyResponse(
    val nextPayments: List<NextPaymentRecurringBuy>
)

@Serializable
data class NextPaymentRecurringBuy(
    val period: String,
    val nextPayment: String,
    val eligibleMethods: List<String>
) {
    companion object {
        const val PAYMENT_CARD = "PAYMENT_CARD"
        const val FUNDS = "FUNDS"
        const val BANK_TRANSFER = "BANK_TRANSFER"
    }
}

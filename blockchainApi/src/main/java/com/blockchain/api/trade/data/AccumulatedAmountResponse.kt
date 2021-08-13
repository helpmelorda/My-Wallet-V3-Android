package com.blockchain.api.trade.data

import kotlinx.serialization.Serializable

// https://www.notion.so/nabu-gateway-abbd68cebafd4070983b72565abaa2f2

@Serializable
data class AccumulatedInPeriodResponse(
    val tradesAccumulated: List<AccumulatedInPeriod>
)

@Serializable
data class AccumulatedInPeriod(
    val amount: AccumulatedAmount,
    val termType: String
) {
    companion object {
        const val DAY = "DAY"
        const val WEEK = "WEEK"
        const val MONTH = "MONTH"
        const val YEAR = "YEAR"
        const val ALL = "ALL"
    }
}

@Serializable
data class AccumulatedAmount(
    val symbol: String,
    val value: String
)

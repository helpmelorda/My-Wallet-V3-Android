package com.blockchain.api.services

import com.blockchain.api.trade.TradeApi

class TradeService internal constructor(
    private val api: TradeApi
) {
    fun isFirstTimeBuyer(authHeader: String) =
        api.isFirstTimeBuyer(authHeader)

    fun getNextPaymentDate(authHeader: String) =
        api.getNextPaymentDate(authHeader)
}

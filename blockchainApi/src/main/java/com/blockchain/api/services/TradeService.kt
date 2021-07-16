package com.blockchain.api.services

import com.blockchain.api.trade.TradeApi
import com.blockchain.api.wrapErrorMessage

class TradeService internal constructor(
    private val api: TradeApi
) {
    fun isFirstTimeBuyer(authHeader: String) =
        api.isFirstTimeBuyer(authHeader)
            .wrapErrorMessage()
}

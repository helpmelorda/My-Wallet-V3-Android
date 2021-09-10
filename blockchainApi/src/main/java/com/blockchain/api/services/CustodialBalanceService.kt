package com.blockchain.api.services

import com.blockchain.api.custodial.CustodialBalanceApi
import com.blockchain.api.custodial.data.TradingBalanceResponseDto
import com.blockchain.api.wrapErrorMessage
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger

data class TradingBalance(
    val assetTicker: String,
    val pending: BigInteger,
    val total: BigInteger,
    val actionable: BigInteger
)

typealias TradingBalanceList = List<TradingBalance>

class CustodialBalanceService internal constructor(
    private val api: CustodialBalanceApi
) {
    fun getTradingBalanceForAllAssets(
        authHeader: String
    ): Single<TradingBalanceList> = api.tradingBalanceForAllAssets(
        authHeader
    ).map {
        it.map { kv -> kv.value.toDomain(kv.key) }
    }.wrapErrorMessage()
}

private fun TradingBalanceResponseDto.toDomain(assetTicker: String): TradingBalance =
    TradingBalance(
        assetTicker = assetTicker,
        total = total.toBigInteger(),
        actionable = actionable.toBigInteger(),
        pending = pending.toBigInteger()
    )
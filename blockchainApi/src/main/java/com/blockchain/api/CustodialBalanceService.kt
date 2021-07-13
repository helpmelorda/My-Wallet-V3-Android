package com.blockchain.api

import com.blockchain.api.custodial.CustodialBalanceApiInterface
import com.blockchain.api.custodial.data.TradingBalanceResponseDto
import io.reactivex.rxjava3.core.Maybe
import retrofit2.HttpException

data class TradingBalance(
    val pending: String,
    val total: String,
    val actionable: String
)

typealias TradingBalanceMap = Map<String, TradingBalance>

class CustodialBalanceService internal constructor(
    private val api: CustodialBalanceApiInterface
) {

    fun getTradingBalanceForAsset(
        authHeader: String,
        assetTicker: String
    ) = api.tradingBalanceForAsset(
        authHeader,
        assetTicker
    ).flatMapMaybe {
        when (it.code()) {
            200 -> Maybe.just(it.body()?.toDomain())
            204 -> Maybe.empty()
            else -> Maybe.error(HttpException(it))
        }
    }.wrapErrorMessage()

    fun getTradingBalanceForAllAssets(
        authHeader: String
    ) = api.tradingBalanceForAllAssets(
        authHeader
    ).map {
        it.map { kv -> kv.key to kv.value.toDomain() }.toMap()
    }.wrapErrorMessage()
}

private fun TradingBalanceResponseDto.toDomain(): TradingBalance =
    TradingBalance(
        total = total,
        actionable = actionable,
        pending = pending
    )
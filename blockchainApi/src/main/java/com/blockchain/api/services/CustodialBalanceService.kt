package com.blockchain.api.services

import com.blockchain.api.HttpStatus
import com.blockchain.api.custodial.CustodialBalanceApi
import com.blockchain.api.custodial.data.TradingBalanceResponseDto
import com.blockchain.api.wrapErrorMessage
import io.reactivex.rxjava3.core.Maybe
import retrofit2.HttpException
import java.math.BigInteger

data class TradingBalance(
    val pending: BigInteger,
    val total: BigInteger,
    val actionable: BigInteger
)

typealias TradingBalanceMap = Map<String, TradingBalance>

class CustodialBalanceService internal constructor(
    private val api: CustodialBalanceApi
) {

    fun getTradingBalanceForAsset(
        authHeader: String,
        assetTicker: String
    ) = api.tradingBalanceForAsset(
        authHeader,
        assetTicker
    ).flatMapMaybe {
        when (it.code()) {
            HttpStatus.OK -> Maybe.just(it.body()?.toDomain())
            HttpStatus.NO_CONTENT -> Maybe.empty()
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
        total = total.toBigInteger(),
        actionable = actionable.toBigInteger(),
        pending = pending.toBigInteger()
    )
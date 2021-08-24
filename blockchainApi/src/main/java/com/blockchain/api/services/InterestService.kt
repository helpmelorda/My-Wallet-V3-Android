package com.blockchain.api.services

import com.blockchain.api.HttpStatus
import com.blockchain.api.interest.InterestApiInterface
import com.blockchain.api.interest.data.InterestAccountBalanceDto
import com.blockchain.api.wrapErrorMessage
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import retrofit2.HttpException
import java.math.BigInteger

data class InterestBalanceDetails(
    val assetTicker: String,
    val totalBalance: BigInteger,
    val pendingInterest: BigInteger,
    val pendingDeposit: BigInteger,
    val totalInterest: BigInteger,
    val pendingWithdrawal: BigInteger,
    val lockedBalance: BigInteger
)

typealias InterestBalanceDetailsList = List<InterestBalanceDetails>

class InterestService internal constructor(
    private val api: InterestApiInterface
) {
    fun getInterestAccountBalance(
        authHeader: String,
        assetTicker: String
    ): Maybe<InterestBalanceDetails> =
        api.getInterestAccountBalance(
            authHeader,
            assetTicker
        ).flatMapMaybe { response ->
            when (response.code()) {
                HttpStatus.OK -> response.body()?.toBalanceDetails(assetTicker)?.let {
                        Maybe.just(it)
                    } ?: Maybe.empty()
                HttpStatus.NO_CONTENT -> Maybe.empty()
                else -> Maybe.error(HttpException(response))
            }
        }.wrapErrorMessage()

    fun getAllInterestAccountBalances(
        authHeader: String
    ): Single<InterestBalanceDetailsList> =
        api.getAllInterestAccountBalances(authHeader)
            .map { response ->
                when (response.code()) {
                    HttpStatus.OK -> response.body() ?: emptyMap()
                    HttpStatus.NO_CONTENT -> emptyMap()
                    else -> throw HttpException(response)
                }
            }.map { balanceMap ->
                balanceMap.mapValues { (assetTicker, balance) ->
                    balance.toBalanceDetails(assetTicker)
                }.values.toList()
            }.wrapErrorMessage()
}

private fun InterestAccountBalanceDto.toBalanceDetails(assetTicker: String): InterestBalanceDetails =
    InterestBalanceDetails(
        assetTicker = assetTicker,
        totalBalance = totalBalance.toBigInteger(),
        pendingInterest = pendingInterest.toBigInteger(),
        pendingDeposit = pendingDeposit.toBigInteger(),
        pendingWithdrawal = pendingWithdrawal.toBigInteger(),
        totalInterest = totalInterest.toBigInteger(),
        lockedBalance = lockedBalance.toBigInteger()
    )
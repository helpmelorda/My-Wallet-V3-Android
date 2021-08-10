package com.blockchain.api.interest

import com.blockchain.api.interest.data.InterestAccountBalanceDto
import io.reactivex.rxjava3.core.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

internal interface InterestApiInterface {

    @GET("accounts/savings")
    fun getInterestAccountBalance(
        @Header("authorization") authorization: String,
        @Query("ccy") assetTicker: String
    ): Single<Response<InterestAccountBalanceDto>>

    @GET("accounts/savings")
    fun getAllInterestAccountBalances(
        @Header("authorization") authorization: String
    ): Single<Map<String, InterestAccountBalanceDto>>
}
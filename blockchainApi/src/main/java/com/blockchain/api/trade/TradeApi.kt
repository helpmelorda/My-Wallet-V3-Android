package com.blockchain.api.trade

import com.blockchain.api.trade.data.AccumulatedInPeriodResponse
import com.blockchain.api.trade.data.NextPaymentRecurringBuyResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

internal interface TradeApi {

    @GET("trades/accumulated")
    fun isFirstTimeBuyer(
        @Header("authorization") authorization: String,
        @Query("products") products: String? = "SIMPLEBUY"
    ): Single<AccumulatedInPeriodResponse>

    @GET("recurring-buy/next-payment")
    fun getNextPaymentDate(
        @Header("authorization") authorization: String
    ): Single<NextPaymentRecurringBuyResponse>
}
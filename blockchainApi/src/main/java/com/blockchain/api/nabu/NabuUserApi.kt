package com.blockchain.api.nabu

import com.blockchain.api.nabu.data.InterestEligibilityResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Header

interface NabuUserApi {

    @GET("eligible/product/savings")
    fun getInterestEligibility(
        @Header("authorization") authorization: String
    ): Single<Map<String, InterestEligibilityResponse>>
}
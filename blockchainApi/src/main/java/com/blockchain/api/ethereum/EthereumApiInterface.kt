package com.blockchain.api.ethereum

import com.blockchain.api.ethereum.data.Erc20AccountTransfersDto
import com.blockchain.api.ethereum.data.Erc20AccountsDto
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

internal interface EthereumApiInterface {
    @GET("eth/v2/account/{account}/tokens")
    @Headers("Accept: application/json")
    fun fetchErc20AccountBalances(
        @Path("account") account: String
    ): Single<Erc20AccountsDto>

    @GET("eth/v2/account/{account}/transfers")
    @Headers("Accept: application/json")
    fun fetchErc20AccountTransfers(
        @Path("account") account: String
    ): Single<Erc20AccountTransfersDto>

    @GET("eth/v2/account/{account}/token/{token}/transfers")
    @Headers("Accept: application/json")
    fun fetchErc20AccountTransfersForAsset(
        @Path("account") account: String,
        @Path("token") token: String
    ): Single<Erc20AccountTransfersDto>
}
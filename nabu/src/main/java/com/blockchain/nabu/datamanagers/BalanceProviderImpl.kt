package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.Authenticator
import com.blockchain.api.TradingBalanceMap
import com.blockchain.api.CustodialBalanceService
import io.reactivex.rxjava3.core.Single

class BalanceProviderImpl(
    private val balanceService: CustodialBalanceService,
    private val authenticator: Authenticator
) : BalancesProvider {
    override fun getCustodialWalletBalanceForAllAssets(): Single<TradingBalanceMap> =
        authenticator.authenticate {
            balanceService.getTradingBalanceForAllAssets(it.authHeader)
        }
}
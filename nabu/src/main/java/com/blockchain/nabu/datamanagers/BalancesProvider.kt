package com.blockchain.nabu.datamanagers

import com.blockchain.api.services.TradingBalanceMap
import io.reactivex.rxjava3.core.Single

interface BalancesProvider {
    fun getCustodialWalletBalanceForAllAssets(): Single<TradingBalanceMap>
}
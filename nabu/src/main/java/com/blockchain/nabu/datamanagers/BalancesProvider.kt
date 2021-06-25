package com.blockchain.nabu.datamanagers

import com.blockchain.api.TradingBalanceMap
import io.reactivex.Single

interface BalancesProvider {
    fun getCustodialWalletBalanceForAllAssets(): Single<TradingBalanceMap>
}
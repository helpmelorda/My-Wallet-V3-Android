package com.blockchain.nabu.datamanagers.repositories.swap

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.service.NabuService
import info.blockchain.balance.AssetCatalogue
import io.reactivex.rxjava3.core.Single

interface TradingPairsProvider {
    fun getAvailablePairs(): Single<List<CurrencyPair>>
}

class TradingPairsProviderImpl(
    private val assetCatalogue: AssetCatalogue,
    private val authenticator: Authenticator,
    private val nabuService: NabuService
) : TradingPairsProvider {
    override fun getAvailablePairs(): Single<List<CurrencyPair>> = authenticator.authenticate { sessionToken ->
        nabuService.getSwapAvailablePairs(sessionToken)
    }.map { response ->
        response.mapNotNull { pair ->
            val parts = pair.split("-")
            if (parts.size != 2) return@mapNotNull null
            val source = assetCatalogue.fromNetworkTicker(parts[0]) ?: return@mapNotNull null
            val destination = assetCatalogue.fromNetworkTicker(parts[1]) ?: return@mapNotNull null
            CurrencyPair.CryptoCurrencyPair(source, destination)
        }
    }
}
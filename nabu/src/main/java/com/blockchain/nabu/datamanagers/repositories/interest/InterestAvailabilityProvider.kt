package com.blockchain.nabu.datamanagers.repositories.interest

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.service.NabuService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

interface InterestAvailabilityProvider {
    fun getEnabledStatusForAllAssets(): Single<List<AssetInfo>>
}

class InterestAvailabilityProviderImpl(
    private val assetCatalogue: AssetCatalogue,
    private val nabuService: NabuService,
    private val authenticator: Authenticator
) : InterestAvailabilityProvider {
    override fun getEnabledStatusForAllAssets(): Single<List<AssetInfo>> =
        authenticator.authenticate { token ->
            nabuService.getInterestEnabled(token).map { instrumentsResponse ->
                instrumentsResponse.instruments.map {
                    assetCatalogue.fromNetworkTicker(it)
                }.mapNotNull { it }
            }
        }
}
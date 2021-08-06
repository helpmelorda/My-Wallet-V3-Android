package com.blockchain.nabu.datamanagers.repositories.interest

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.datamanagers.InterestAccountDetails
import com.blockchain.nabu.models.responses.interest.InterestAccountDetailsResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.rx.ParameteredMappedSinglesTimedRequests
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Single

interface InterestBalancesProvider {
    fun getBalanceForAsset(asset: AssetInfo): Single<InterestAccountDetails>
    fun clearBalanceForAsset(asset: AssetInfo)
    fun clearBalanceForAsset(ticker: String)
}

class InterestBalancesProviderImpl(
    private val assetCatalogue: AssetCatalogue,
    private val authenticator: Authenticator,
    private val nabuService: NabuService
) : InterestBalancesProvider {

    override fun getBalanceForAsset(asset: AssetInfo) =
        Single.just(cache.getCachedSingle(asset).blockingGet())

    override fun clearBalanceForAsset(asset: AssetInfo) {
        cache.invalidate(asset)
    }

    override fun clearBalanceForAsset(ticker: String) {
        val crypto = assetCatalogue.fromNetworkTicker(ticker)
        crypto?.let {
            cache.invalidate(it)
        }
    }

    private val refresh: (AssetInfo) -> Single<InterestAccountDetails> = { currency ->
        authenticator.authenticate {
            nabuService.getInterestAccountBalance(it, currency.ticker)
                .map { details ->
                    details?.toInterestAccountDetails(currency) ?: InterestAccountDetails(
                        balance = CryptoValue.zero(currency),
                        pendingInterest = CryptoValue.zero(currency),
                        pendingDeposit = CryptoValue.zero(currency),
                        totalInterest = CryptoValue.zero(currency),
                        lockedBalance = CryptoValue.zero(currency)
                    )
                }.defaultIfEmpty(
                    InterestAccountDetails(
                        balance = CryptoValue.zero(currency),
                        pendingInterest = CryptoValue.zero(currency),
                        pendingDeposit = CryptoValue.zero(currency),
                        totalInterest = CryptoValue.zero(currency),
                        lockedBalance = CryptoValue.zero(currency)
                    )
                )
        }
    }

    private val cache = ParameteredMappedSinglesTimedRequests(
        cacheLifetimeSeconds = 240L,
        refreshFn = refresh
    )

    private fun InterestAccountDetailsResponse.toInterestAccountDetails(asset: AssetInfo) =
        InterestAccountDetails(
            balance = CryptoValue.fromMinor(asset, balance.toBigInteger()),
            pendingInterest = CryptoValue.fromMinor(asset, pendingInterest.toBigInteger()),
            pendingDeposit = CryptoValue.fromMinor(asset, pendingDeposit.toBigInteger()),
            totalInterest = CryptoValue.fromMinor(asset, totalInterest.toBigInteger()),
            lockedBalance = CryptoValue.fromMinor(asset, locked.toBigInteger())
        )
}

package com.blockchain.nabu.datamanagers.repositories.interest

import com.blockchain.rx.TimedCacheRequest
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import io.reactivex.Maybe
import io.reactivex.Single

class InterestRepository(
    private val interestLimitsProvider: InterestLimitsProvider,
    private val interestAvailabilityProvider: InterestAvailabilityProvider,
    private val interestEligibilityProvider: InterestEligibilityProvider,
    private val interestAccountBalancesProvider: InterestBalancesProvider
) {
    private val limitsCache = TimedCacheRequest(
        cacheLifetimeSeconds = SHORT_LIFETIME,
        refreshFn = { interestLimitsProvider.getLimitsForAllAssets() }
    )

    private val availabilityCache = TimedCacheRequest(
        cacheLifetimeSeconds = LONG_LIFETIME,
        refreshFn = { interestAvailabilityProvider.getEnabledStatusForAllAssets() }
    )

    private val eligibilityCache = TimedCacheRequest(
        cacheLifetimeSeconds = LONG_LIFETIME,
        refreshFn = { interestEligibilityProvider.getEligibilityForCustodialAssets() }
    )

    fun getInterestAccountBalance(asset: AssetInfo) =
        interestAccountBalancesProvider.getBalanceForAsset(asset).map {
            it.balance
        }

    fun getInterestPendingBalance(asset: AssetInfo) =
        interestAccountBalancesProvider.getBalanceForAsset(asset).map {
            it.pendingDeposit
        }

    fun getInterestActionableBalance(asset: AssetInfo) =
        interestAccountBalancesProvider.getBalanceForAsset(asset).map {
            (it.balance - it.lockedBalance) as CryptoValue
        }

    fun clearBalanceForAsset(asset: AssetInfo) =
        interestAccountBalancesProvider.clearBalanceForAsset(asset)

    fun getInterestAccountDetails(asset: AssetInfo) =
        interestAccountBalancesProvider.getBalanceForAsset(asset)

    fun clearBalanceForAsset(ticker: String) =
        interestAccountBalancesProvider.clearBalanceForAsset(ticker)

    fun getLimitForAsset(ccy: AssetInfo): Maybe<InterestLimits> =
        limitsCache.getCachedSingle().flatMapMaybe { limitsList ->
            val limitsForAsset = limitsList.list.find { it.cryptoCurrency == ccy }
            limitsForAsset?.let {
                Maybe.just(it)
            } ?: Maybe.empty()
        }.onErrorResumeNext(Maybe.empty())

    fun getAvailabilityForAsset(ccy: AssetInfo): Single<Boolean> =
        availabilityCache.getCachedSingle().flatMap { enabledList ->
            Single.just(enabledList.contains(ccy))
        }.onErrorResumeNext(Single.just(false))

    fun getAvailableAssets(): Single<List<AssetInfo>> =
        availabilityCache.getCachedSingle()

    fun getEligibilityForAsset(ccy: AssetInfo): Single<Eligibility> =
        eligibilityCache.getCachedSingle().map { eligibilityList ->
            eligibilityList.find { it.cryptoCurrency == ccy }?.eligibility
                ?: Eligibility.notEligible()
        }

    companion object {
        private const val SHORT_LIFETIME = 240L
        private const val LONG_LIFETIME = 3600L
    }
}
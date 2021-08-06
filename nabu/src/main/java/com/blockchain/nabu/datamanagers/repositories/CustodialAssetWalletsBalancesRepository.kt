package com.blockchain.nabu.datamanagers.repositories

import com.blockchain.nabu.datamanagers.BalancesProvider
import info.blockchain.balance.AssetInfo
import com.blockchain.rx.TimedCacheRequest
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Maybe
import timber.log.Timber

class CustodialAssetWalletsBalancesRepository(balancesProvider: BalancesProvider) {

    private val custodialBalancesCache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = {
            balancesProvider.getCustodialWalletBalanceForAllAssets()
                .doOnSuccess { Timber.d("Custodial balance response: $it") }
        }
    )

    fun getTotalBalanceForAsset(asset: AssetInfo): Maybe<CryptoValue> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[asset.ticker]?.let { response ->
                Maybe.just(CryptoValue.fromMinor(asset, response.total.toBigInteger()))
            } ?: Maybe.empty()
        }.onErrorResumeNext { Maybe.empty() }

    fun getActionableBalanceForAsset(asset: AssetInfo): Maybe<CryptoValue> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[asset.ticker]?.let { response ->
                Maybe.just(CryptoValue.fromMinor(asset, response.actionable.toBigInteger()))
            } ?: Maybe.empty()
        }.onErrorResumeNext { Maybe.empty() }

    fun getPendingBalanceForAsset(asset: AssetInfo): Maybe<CryptoValue> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[asset.ticker]?.let { response ->
                Maybe.just(CryptoValue.fromMinor(asset, response.pending.toBigInteger()))
            } ?: Maybe.empty()
        }.onErrorResumeNext { Maybe.empty() }

    fun getFiatTotalBalanceForAsset(fiat: String): Maybe<FiatValue> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[fiat]?.let { response ->
                Maybe.just(FiatValue.fromMinor(fiat, response.total.toLong()))
            } ?: Maybe.empty()
        }.onErrorResumeNext { Maybe.empty() }

    fun getFiatActionableBalanceForAsset(fiat: String): Maybe<FiatValue> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[fiat]?.let { response ->
                Maybe.just(FiatValue.fromMinor(fiat, response.actionable.toLong()))
            } ?: Maybe.empty()
        }.onErrorResumeNext { Maybe.empty() }

    fun getFiatPendingBalanceForAsset(fiat: String): Maybe<FiatValue> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[fiat]?.let { response ->
                Maybe.just(FiatValue.fromMinor(fiat, response.pending.toLong()))
            } ?: Maybe.empty()
        }.onErrorResumeNext { Maybe.empty() }

    companion object {
        private const val CACHE_LIFETIME = 10L
    }
}

package com.blockchain.core.custodial

import com.blockchain.api.services.CustodialBalanceService
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.rx.TimedCacheRequest
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Maybe
import timber.log.Timber

internal class TradingBalanceCallCache(
    private val balanceService: CustodialBalanceService,
    private val authHeaderProvider: AuthHeaderProvider
) {
    private val custodialBalancesCache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = {
            authHeaderProvider.getAuthHeader()
                .flatMap { balanceService.getTradingBalanceForAllAssets(it) }
                .doOnSuccess { Timber.d("Custodial balance response: $it") }
        }
    )

    fun getBalanceForAsset(asset: AssetInfo): Maybe<Balance> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[asset.ticker]?.let { response ->
                Maybe.just(
                    Balance(
                        total = CryptoValue.fromMinor(asset, response.total),
                        actionable = CryptoValue.fromMinor(asset, response.actionable),
                        pending = CryptoValue.fromMinor(asset, response.pending)
                    )
                )
            } ?: Maybe.empty()
        }.onErrorResumeNext { Maybe.empty() }

    fun getBalanceForFiat(fiat: String): Maybe<Balance> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[fiat]?.let { response ->
                Maybe.just(
                    Balance(
                        total = FiatValue.fromMinor(fiat, response.total.toLong()),
                        actionable = FiatValue.fromMinor(fiat, response.actionable.toLong()),
                        pending = FiatValue.fromMinor(fiat, response.pending.toLong())
                    )
                )
            } ?: Maybe.empty()
        }.onErrorResumeNext { Maybe.empty() }

    companion object {
        private const val CACHE_LIFETIME = 10L
    }
}
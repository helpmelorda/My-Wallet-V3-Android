package com.blockchain.core.custodial

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

data class TradingAccountBalance(
    val total: Money,
    val actionable: Money,
    val pending: Money,
    val hasTransactions: Boolean = false
)

interface TradingBalanceDataManager {
    fun getBalanceForAsset(asset: AssetInfo): Observable<TradingAccountBalance>
    fun getBalanceForFiat(fiat: String): Observable<TradingAccountBalance>

    // Active means the BE has reported a balance to us, which may be zero, meaning that the
    // asset/fiat account associated with this has had a balance at some point.
    fun getActiveAssets(): Single<Set<AssetInfo>>
    fun getActiveFiats(): Single<Set<String>>
}

internal class TradingBalanceDataManagerImpl(
    private val balanceCallCache: TradingBalanceCallCache
) : TradingBalanceDataManager {
    override fun getBalanceForAsset(asset: AssetInfo): Observable<TradingAccountBalance> =
        balanceCallCache.getTradingBalances()
            .map { it.cryptoBalances.getOrDefault(asset, zeroBalance(asset)) }
            .toObservable()

    override fun getBalanceForFiat(fiat: String): Observable<TradingAccountBalance> =
        balanceCallCache.getTradingBalances()
            .map { it.fiatBalances.getOrDefault(fiat, zeroBalance(fiat)) }
            .toObservable()

    override fun getActiveAssets(): Single<Set<AssetInfo>> =
        balanceCallCache.getTradingBalances()
            .map { it.cryptoBalances.keys }

    override fun getActiveFiats(): Single<Set<String>> =
        balanceCallCache.getTradingBalances()
            .map { it.fiatBalances.keys }
}

private fun zeroBalance(asset: AssetInfo): TradingAccountBalance =
    TradingAccountBalance(
        total = CryptoValue.zero(asset),
        actionable = CryptoValue.zero(asset),
        pending = CryptoValue.zero(asset)
    )

private fun zeroBalance(fiat: String): TradingAccountBalance =
    TradingAccountBalance(
        total = FiatValue.zero(fiat),
        actionable = FiatValue.zero(fiat),
        pending = FiatValue.zero(fiat)
    )
package com.blockchain.core.custodial

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Maybe

data class Balance(
    val total: Money,
    val actionable: Money,
    val pending: Money
)

interface TradingBalanceDataManager {
    fun getBalanceForAsset(asset: AssetInfo): Maybe<Balance>
    fun getBalanceForFiat(fiat: String): Maybe<Balance>

    @Deprecated("Use getBalanceForAsset")
    fun getTotalBalanceForAsset(asset: AssetInfo): Maybe<CryptoValue>

    @Deprecated("Use getBalanceForAsset")
    fun getActionableBalanceForAsset(asset: AssetInfo): Maybe<CryptoValue>

    @Deprecated("Use getBalanceForAsset")
    fun getPendingBalanceForAsset(asset: AssetInfo): Maybe<CryptoValue>

    @Deprecated("Use getBalanceForFiat")
    fun getFiatTotalBalanceForAsset(fiat: String): Maybe<FiatValue>

    @Deprecated("Use getBalanceForFiat")
    fun getFiatActionableBalanceForAsset(fiat: String): Maybe<FiatValue>

    @Deprecated("Use getBalanceForFiat")
    fun getFiatPendingBalanceForAsset(fiat: String): Maybe<FiatValue>
}

internal class TradingBalanceDataManagerImpl(
    private val tradingBalanceCallCache: TradingBalanceCallCache
) : TradingBalanceDataManager {
    override fun getBalanceForAsset(asset: AssetInfo): Maybe<Balance> =
        tradingBalanceCallCache.getBalanceForAsset(asset)

    override fun getBalanceForFiat(fiat: String): Maybe<Balance> =
        tradingBalanceCallCache.getBalanceForFiat(fiat)

    override fun getTotalBalanceForAsset(asset: AssetInfo): Maybe<CryptoValue> =
        getBalanceForAsset(asset)
            .map { it.total as CryptoValue }.let { it } ?: Maybe.empty()

    override fun getActionableBalanceForAsset(asset: AssetInfo): Maybe<CryptoValue> =
        getBalanceForAsset(asset)
            .map { it.actionable as CryptoValue }.let { it } ?: Maybe.empty()

    override fun getPendingBalanceForAsset(asset: AssetInfo): Maybe<CryptoValue> =
        getBalanceForAsset(asset)
            .map { it.pending as CryptoValue }.let { it } ?: Maybe.empty()

    override fun getFiatTotalBalanceForAsset(fiat: String): Maybe<FiatValue> =
        getBalanceForFiat(fiat)
            .map { it.total as FiatValue }.let { it } ?: Maybe.empty()

    override fun getFiatActionableBalanceForAsset(fiat: String): Maybe<FiatValue> =
        getBalanceForFiat(fiat)
            .map { it.actionable as FiatValue }.let { it } ?: Maybe.empty()

    override fun getFiatPendingBalanceForAsset(fiat: String): Maybe<FiatValue> =
        getBalanceForFiat(fiat)
            .map { it.pending as FiatValue }.let { it } ?: Maybe.empty()
}
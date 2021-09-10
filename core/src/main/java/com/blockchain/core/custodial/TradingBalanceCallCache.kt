package com.blockchain.core.custodial

import com.blockchain.api.services.CustodialBalanceService
import com.blockchain.api.services.TradingBalance
import com.blockchain.api.services.TradingBalanceList
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.rx.TimedCacheRequest
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single

internal class TradingBalanceRecord(
    val cryptoBalances: Map<AssetInfo, TradingAccountBalance> = emptyMap(),
    val fiatBalances: Map<String, TradingAccountBalance> = emptyMap()
)

internal class TradingBalanceCallCache(
    private val balanceService: CustodialBalanceService,
    private val assetCatalogue: AssetCatalogue,
    private val authHeaderProvider: AuthHeaderProvider
) {
    private val refresh: () -> Single<TradingBalanceRecord> = {
        authHeaderProvider.getAuthHeader()
            .flatMap { balanceService.getTradingBalanceForAllAssets(it) }
            .map { buildRecordMap(it) }
            .onErrorReturn { TradingBalanceRecord() }
    }

    private fun buildRecordMap(balanceList: TradingBalanceList): TradingBalanceRecord =
        TradingBalanceRecord(
            cryptoBalances = balanceList.mapNotNull { balance ->
                assetCatalogue.fromNetworkTicker(balance.assetTicker)?.let { assetInfo ->
                    assetInfo to balance.toCryptoTradingAccountBalance(assetInfo)
                }
            }.toMap(),
            fiatBalances = balanceList.mapNotNull { balance ->
                if (assetCatalogue.isFiatTicker(balance.assetTicker)) {
                    balance.assetTicker to balance.toFiatTradingAccountBalance(balance.assetTicker)
                } else {
                    null
                }
            }.toMap()
        )

    private val custodialBalancesCache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = refresh
    )

    fun getTradingBalances() =
        custodialBalancesCache.getCachedSingle()

    companion object {
        private const val CACHE_LIFETIME = 10L
    }
}

private fun TradingBalance.toCryptoTradingAccountBalance(assetInfo: AssetInfo) =
    TradingAccountBalance(
        total = CryptoValue.fromMinor(assetInfo, total),
        actionable = CryptoValue.fromMinor(assetInfo, actionable),
        pending = CryptoValue.fromMinor(assetInfo, pending),
        hasTransactions = true
    )

private fun TradingBalance.toFiatTradingAccountBalance(fiatSymbol: String) =
    TradingAccountBalance(
        total = FiatValue.fromMinor(fiatSymbol, total.toLong()),
        actionable = FiatValue.fromMinor(fiatSymbol, actionable.toLong()),
        pending = FiatValue.fromMinor(fiatSymbol, pending.toLong()),
        hasTransactions = true
    )

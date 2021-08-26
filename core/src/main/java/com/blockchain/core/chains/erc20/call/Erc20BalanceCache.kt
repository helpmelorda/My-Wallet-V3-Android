package com.blockchain.core.chains.erc20.call

import com.blockchain.api.services.Erc20TokenBalance
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.core.chains.erc20.model.Erc20Balance
import com.blockchain.rx.TimedCacheRequest
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicReference

internal typealias Erc20BalanceMap = Map<AssetInfo, Erc20Balance>

internal class Erc20BalanceCallCache(
    private val erc20Service: NonCustodialErc20Service,
    private val assetCatalogue: AssetCatalogue
) {
    private val cacheRequest: TimedCacheRequest<Erc20BalanceMap> by lazy {
        TimedCacheRequest(
            cacheLifetimeSeconds = BALANCE_CACHE_TTL_SECONDS,
            refreshFn = ::refreshCache
        )
    }

    private val account = AtomicReference<String>()

    private fun refreshCache(): Single<Erc20BalanceMap> {
        return erc20Service.getTokenBalances(account.get())
            .map { balanceList ->
                balanceList.mapNotNull { balance ->
                    assetCatalogue.fromNetworkTickerWithL2Id(
                        balance.ticker,
                        CryptoCurrency.ETHER,
                        balance.contractAddress
                    )?.let { info ->
                        info to balance.mapBalance(info)
                    }
                }.toMap()
            }
    }

    fun getBalances(accountHash: String): Single<Erc20BalanceMap> {
        val oldAccountHash = account.getAndSet(accountHash)
        if (oldAccountHash != accountHash) {
            cacheRequest.invalidate()
        }
        return cacheRequest.getCachedSingle()
    }

    fun flush(asset: AssetInfo) {
        cacheRequest.invalidate()
    }

    companion object {
        private const val BALANCE_CACHE_TTL_SECONDS = 10L
    }
}

private fun Erc20TokenBalance?.mapBalance(asset: AssetInfo): Erc20Balance =
    this?.let {
        Erc20Balance(
            balance = CryptoValue.fromMinor(asset, it.balance),
            hasTransactions = it.transferCount > 0
        )
    } ?: Erc20Balance(
        balance = CryptoValue.zero(asset),
        hasTransactions = false
    )

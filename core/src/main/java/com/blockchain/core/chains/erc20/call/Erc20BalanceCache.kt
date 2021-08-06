package com.blockchain.core.chains.erc20.call

import com.blockchain.api.services.Erc20TokenBalance
import com.blockchain.api.services.Erc20TokenBalanceList
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.core.chains.erc20.model.Erc20Balance
import com.blockchain.rx.TimedCacheRequest
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.isErc20
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicReference

internal class Erc20BalanceCallCache(
    private val erc20Service: NonCustodialErc20Service
) {
    private val cacheRequest: TimedCacheRequest<Erc20TokenBalanceList> by lazy {
        TimedCacheRequest(
            cacheLifetimeSeconds = BALANCE_CACHE_TTL_SECONDS,
            refreshFn = ::refreshCache
        )
    }

    private val account = AtomicReference<String>()
    private fun refreshCache(): Single<Erc20TokenBalanceList> {
        return erc20Service.getTokenBalances(account.get())
    }

    fun fetch(accountHash: String, asset: AssetInfo): Single<Erc20Balance> {
        require(asset.isErc20())
        requireNotNull(asset.l2identifier)

        val oldAccountHash = account.getAndSet(accountHash)
        if (oldAccountHash != accountHash) {
            cacheRequest.invalidate()
        }

        return cacheRequest.getCachedSingle()
            .map { list ->
                list.firstOrNull {
                    asset.l2identifier?.compareTo(it.contractAddress, ignoreCase = true) == 0
                }.mapBalance(asset)
            }
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

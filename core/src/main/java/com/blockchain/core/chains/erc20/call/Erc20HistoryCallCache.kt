package com.blockchain.core.chains.erc20.call

import com.blockchain.api.services.Erc20Transfer
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.core.chains.erc20.model.Erc20HistoryEvent
import com.blockchain.core.chains.erc20.model.Erc20HistoryList
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.ethereum.EthDataManager

// This doesn't cache anything at this time, since it makes a call for a single
// asset. We can review this, when we look at activity caching in detail

internal class Erc20HistoryCallCache(
    private val ethDataManager: EthDataManager,
    private val erc20Service: NonCustodialErc20Service
) {
    fun fetch(accountHash: String, asset: AssetInfo): Single<Erc20HistoryList> {
        val contractAddress = asset.l2identifier
        checkNotNull(contractAddress)

        return erc20Service.getTokenTransfers(accountHash, contractAddress)
            .map { list ->
                list.map { tx ->
                    tx.toHistoryEvent(
                        asset,
                        getFeeFetcher(tx.transactionHash)
                    )
                }
            }
    }

    private fun getFeeFetcher(txHash: String): Single<CryptoValue> =
        ethDataManager.getTransaction(txHash)
            .map { transaction ->
                val fee = transaction.gasUsed * transaction.gasPrice
                CryptoValue.fromMinor(CryptoCurrency.ETHER, fee)
            }.firstOrError()

    fun flush(asset: AssetInfo) {
        // Do nothing
    }
}

private fun Erc20Transfer.toHistoryEvent(
    asset: AssetInfo,
    feeFetcher: Single<CryptoValue>
): Erc20HistoryEvent =
    Erc20HistoryEvent(
        transactionHash = transactionHash,
        value = CryptoValue.fromMinor(asset, value),
        from = from,
        to = to,
        blockNumber = blockNumber,
        timestamp = timestamp,
        fee = feeFetcher
    )

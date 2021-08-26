package com.blockchain.core.chains.erc20.model

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger

data class Erc20HistoryEvent(
    val transactionHash: String,
    val value: CryptoValue,
    val from: String,
    val to: String,
    val blockNumber: BigInteger,
    val timestamp: Long,
    val fee: Single<CryptoValue>
) {
    fun isFromAccount(accountHash: String): Boolean =
        accountHash.compareTo(from, ignoreCase = true) == 0

    fun isToAccount(accountHash: String): Boolean =
        accountHash.compareTo(to, ignoreCase = true) == 0
}

typealias Erc20HistoryList = List<Erc20HistoryEvent>

data class Erc20Balance(
    val balance: CryptoValue,
    val hasTransactions: Boolean
) {
    internal companion object {
        fun zero(assetInfo: AssetInfo): Erc20Balance =
            Erc20Balance(
                balance = CryptoValue.zero(assetInfo),
                hasTransactions = false
            )
    }
}

package com.blockchain.api.services

import com.blockchain.api.ethereum.EthereumApiInterface
import com.blockchain.api.ethereum.data.Erc20TokenBalanceDto
import com.blockchain.api.ethereum.data.Erc20TransferDto
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger

data class Erc20TokenBalance(
    val ticker: String,
    val contractAddress: String,
    val balance: BigInteger,
    val precisionDp: Int,
    val transferCount: Int
)

typealias Erc20TokenBalanceList = List<Erc20TokenBalance>

data class Erc20Transfer(
    val logIndex: String,
    val transactionHash: String,
    val value: BigInteger,
    val from: String,
    val to: String,
    val blockNumber: BigInteger,
    val timestamp: Long
)

typealias Erc20TransferList = List<Erc20Transfer>

class NonCustodialErc20Service internal constructor(
    private val api: EthereumApiInterface,
    private val apiCode: String
) {
    fun getTokenBalances(accountHash: String): Single<Erc20TokenBalanceList> =
        api.fetchErc20AccountBalances(accountHash)
            .map { dto ->
                dto.balances.map { it.toErc20TokenBalance(accountHash) }
            }

    fun getTokenTransfers(accountHash: String, contractAddress: String): Single<Erc20TransferList> =
        api.fetchErc20AccountTransfersForAsset(accountHash, contractAddress)
            .map { dto ->
                dto.transfers
            }.map { list ->
                list.map { it.toErc20Transfer() }
            }
}

private fun Erc20TokenBalanceDto.toErc20TokenBalance(
    accountHash: String
): Erc20TokenBalance {
    check(this.accountHash.compareTo(accountHash, ignoreCase = true) == 0)
    return Erc20TokenBalance(
        ticker = ticker,
        contractAddress = contractAddress,
        balance = balance,
        precisionDp = precisionDp, // Keep this for sanity checking in repo against AssetInfo
        transferCount = transferCount // If non-zero, this account has transactions
    )
}

private fun Erc20TransferDto.toErc20Transfer() =
    Erc20Transfer(
        logIndex,
        txHash,
        value,
        fromAddress,
        toAddress,
        blockNumber,
        timestamp
    )

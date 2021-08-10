package com.blockchain.core.interest

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Single

data class InterestBalance(
    val totalBalance: CryptoValue,
    val pendingInterest: CryptoValue,
    val pendingDeposit: CryptoValue,
    val totalInterest: CryptoValue,
    val lockedBalance: CryptoValue
) {
    val actionableBalance: CryptoValue
        get() = (totalBalance - lockedBalance) as CryptoValue
}

interface InterestBalanceDataManager {
    fun getBalanceForAsset(asset: AssetInfo): Single<InterestBalance>
    fun getAssetsWithBalance(): Single<Set<AssetInfo>>
    fun flushCaches(asset: AssetInfo)
}

internal class InterestBalanceDataManagerImpl(
    private val balanceCallCache: InterestBalanceCallCache
) : InterestBalanceDataManager {
    override fun getBalanceForAsset(asset: AssetInfo): Single<InterestBalance> =
        balanceCallCache.getBalances().map {
            it[asset] ?: zeroBalance(asset)
        }

    override fun getAssetsWithBalance(): Single<Set<AssetInfo>> =
        balanceCallCache.getBalances().map { it.keys }

    override fun flushCaches(asset: AssetInfo) {
        balanceCallCache.invalidate()
    }
}

private fun zeroBalance(asset: AssetInfo): InterestBalance =
    InterestBalance(
        totalBalance = CryptoValue.zero(asset),
        pendingInterest = CryptoValue.zero(asset),
        pendingDeposit = CryptoValue.zero(asset),
        totalInterest = CryptoValue.zero(asset),
        lockedBalance = CryptoValue.zero(asset)
    )

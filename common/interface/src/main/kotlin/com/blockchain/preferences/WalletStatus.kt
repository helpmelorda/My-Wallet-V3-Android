package com.blockchain.preferences

import info.blockchain.balance.AssetInfo

interface WalletStatus {
    var lastBackupTime: Long // Seconds since epoch
    val isWalletBackedUp: Boolean

    val isWalletFunded: Boolean
    fun setWalletFunded()

    var lastSwapTime: Long
    val hasSwapped: Boolean

    val hasMadeBitPayTransaction: Boolean
    fun setBitPaySuccess()

    fun setFeeTypeForAsset(asset: AssetInfo, type: Int)
    fun getFeeTypeForAsset(asset: AssetInfo): Int?

    val hasSeenSwapPromo: Boolean
    fun setSeenSwapPromo()

    val resendSmsRetries: Int
    fun setResendSmsRetries(retries: Int)

    val isNewUser: Boolean
    fun setNewUser()

    val hasSeenTradingSwapPromo: Boolean
    fun setSeenTradingSwapPromo()
}
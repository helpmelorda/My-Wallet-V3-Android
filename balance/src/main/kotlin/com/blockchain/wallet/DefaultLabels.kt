package com.blockchain.wallet

import info.blockchain.balance.AssetInfo

interface DefaultLabels {

    fun getAllWalletLabel(): String
    fun getAssetMasterWalletLabel(asset: AssetInfo): String
    fun getDefaultNonCustodialWalletLabel(): String
    fun getOldDefaultNonCustodialWalletLabel(asset: AssetInfo): String
    fun getDefaultCustodialWalletLabel(): String
    fun getDefaultInterestWalletLabel(): String
    fun getDefaultExchangeWalletLabel(): String
    fun getDefaultCustodialFiatWalletLabel(fiatCurrency: String): String
}

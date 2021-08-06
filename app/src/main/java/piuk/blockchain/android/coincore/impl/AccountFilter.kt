package piuk.blockchain.android.coincore.impl

import com.blockchain.extensions.exhaustive
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.SingleAccountList

fun SingleAccountList.makeAccountGroup(
    asset: AssetInfo,
    labels: DefaultLabels,
    assetFilter: AssetFilter
): AccountGroup? =
    when (assetFilter) {
        AssetFilter.All ->
            buildAssetMasterGroup(asset, labels, this)
        AssetFilter.NonCustodial ->
            buildNonCustodialGroup(asset, labels, this)
        AssetFilter.Custodial ->
            buildCustodialGroup(labels, this)
        AssetFilter.Interest ->
            buildInterestGroup(labels, this)
    }.exhaustive

private fun buildInterestGroup(
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    val grpAccounts = accountList.filterIsInstance<CryptoInterestAccount>()
    return if (grpAccounts.isNotEmpty())
        CryptoAccountCustodialGroup(
            labels.getDefaultInterestWalletLabel(), grpAccounts
        )
    else
        null
}

private fun buildCustodialGroup(
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    val grpAccounts = accountList.filterIsInstance<CustodialTradingAccount>()
    return if (grpAccounts.isNotEmpty())
        CryptoAccountCustodialGroup(
            labels.getDefaultCustodialWalletLabel(), grpAccounts
        )
    else
        null
}

private fun buildNonCustodialGroup(
    asset: AssetInfo,
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    val grpAccounts = accountList.filterIsInstance<CryptoNonCustodialAccount>()
    return if (grpAccounts.isNotEmpty())
        CryptoAccountNonCustodialGroup(
            asset, labels.getDefaultCustodialWalletLabel(), grpAccounts
        )
    else
        null
}

private fun buildAssetMasterGroup(
    asset: AssetInfo,
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? =
    if (accountList.isEmpty())
        null
    else
        CryptoAccountNonCustodialGroup(
            asset,
            labels.getAssetMasterWalletLabel(asset),
            accountList
        )
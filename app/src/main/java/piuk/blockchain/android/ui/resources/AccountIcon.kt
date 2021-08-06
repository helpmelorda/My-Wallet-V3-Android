package piuk.blockchain.android.ui.resources

import android.widget.ImageView
import androidx.annotation.DrawableRes
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.coincore.fiat.FiatAccountGroup
import piuk.blockchain.android.coincore.impl.AllWalletsAccount
import piuk.blockchain.android.coincore.impl.CryptoAccountCustodialGroup
import piuk.blockchain.android.coincore.impl.CryptoAccountNonCustodialGroup
import piuk.blockchain.android.coincore.impl.CryptoExchangeAccount
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount

class AccountIcon(
    private val account: BlockchainAccount,
    private val assetResources: AssetResources
) {
    fun loadAssetIcon(imageView: ImageView) {
        val assetTicker = assetForIcon
        if (assetTicker != null) {
            assetResources.loadAssetIcon(imageView, assetTicker)
        } else {
            val icon = standardIcon ?: throw IllegalStateException("$account is not supported")
            imageView.setImageResource(icon)
        }
    }

    private val standardIcon: Int?
        @DrawableRes get() = when (account) {
            is CryptoAccount -> null
            is FiatAccount -> assetResources.fiatCurrencyIcon(account.fiatCurrency)
            is AccountGroup -> accountGroupIcon(account)
            else -> throw IllegalStateException("$account is not supported")
        }

    private val assetForIcon: AssetInfo?
        get() = when (account) {
            is CryptoAccount -> account.asset
            is FiatAccount -> null
            is AccountGroup -> accountGroupTicker(account)
            else -> throw IllegalStateException("$account is not supported")
        }

    val indicator: Int?
        @DrawableRes get() = when (account) {
            is CryptoNonCustodialAccount -> R.drawable.ic_non_custodial_account_indicator
            is InterestAccount -> R.drawable.ic_interest_account_indicator
            is TradingAccount -> R.drawable.ic_custodial_account_indicator
            is CryptoExchangeAccount -> R.drawable.ic_exchange_indicator
            else -> null
        }

    private fun accountGroupIcon(account: AccountGroup): Int? {
        return when (account) {
            is AllWalletsAccount -> R.drawable.ic_all_wallets_white
            is CryptoAccountCustodialGroup -> null
            is CryptoAccountNonCustodialGroup -> null
            is FiatAccountGroup -> (account.accounts.getOrNull(0) as? FiatAccount)?.let {
                assetResources.fiatCurrencyIcon(it.fiatCurrency)
            } ?: DEFAULT_FIAT_ICON
            else -> throw IllegalArgumentException("$account is not a valid group")
        }
    }

    companion object {
        private const val DEFAULT_FIAT_ICON = R.drawable.ic_funds_usd

        private fun accountGroupTicker(account: AccountGroup): AssetInfo? {
            return when (account) {
                is AllWalletsAccount -> null
                is CryptoAccountCustodialGroup -> (account.accounts[0] as CryptoAccount).asset
                is CryptoAccountNonCustodialGroup -> account.asset
                is FiatAccountGroup -> null
                else -> throw IllegalArgumentException("$account is not a valid group")
            }
        }
    }
}

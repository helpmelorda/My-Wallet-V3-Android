package piuk.blockchain.android.coincore.fiat

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.Asset
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.SingleAccountList

class FiatAsset(
    private val labels: DefaultLabels,
    private val exchangeRateDataManager: ExchangeRatesDataManager,
    private val tradingBalanceDataManager: TradingBalanceDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs
) : Asset {

    override fun accountGroup(filter: AssetFilter): Maybe<AccountGroup> =
        when (filter) {
            AssetFilter.All,
            AssetFilter.Custodial -> fetchFiatWallets()
            AssetFilter.NonCustodial,
            AssetFilter.Interest -> Maybe.empty() // Only support single accounts
        }

    private fun setSelectedFiatFirst(fiatList: List<String>): List<String> {
        val fiatMutableList = fiatList.toMutableList()
        if (fiatMutableList.first() != currencyPrefs.selectedFiatCurrency) {
            fiatMutableList.firstOrNull { it == currencyPrefs.selectedFiatCurrency }?.let {
                fiatMutableList.remove(it)
                fiatMutableList.add(0, it)
            }
        }
        return fiatMutableList.toList()
    }

    private fun fetchFiatWallets(): Maybe<AccountGroup> =
        custodialWalletManager.getSupportedFundsFiats(
            currencyPrefs.selectedFiatCurrency
        )
            .flatMapMaybe { fiatList ->
                val mutable = fiatList.toMutableList()
                if (mutable.isNotEmpty()) {
                    val orderedList = setSelectedFiatFirst(fiatList)
                    Maybe.just(
                        FiatAccountGroup(
                            label = "Fiat Accounts",
                            accounts = orderedList.map { getAccount(it) }
                        )
                    )
                } else {
                    Maybe.empty()
                }
            }

    private val accounts = mutableMapOf<String, FiatAccount>()

    private fun getAccount(fiatCurrency: String): FiatAccount =
        accounts.getOrPut(fiatCurrency) {
            FiatCustodialAccount(
                label = labels.getDefaultCustodialFiatWalletLabel(fiatCurrency),
                fiatCurrency = fiatCurrency,
                tradingBalanceDataManager = tradingBalanceDataManager,
                exchangesRates = exchangeRateDataManager,
                custodialWalletManager = custodialWalletManager
            )
        }

    // we cannot transfer for fiat
    override fun transactionTargets(account: SingleAccount): Single<SingleAccountList> =
        Single.just(emptyList())

    override fun parseAddress(address: String, label: String?): Maybe<ReceiveAddress> = Maybe.empty()
}

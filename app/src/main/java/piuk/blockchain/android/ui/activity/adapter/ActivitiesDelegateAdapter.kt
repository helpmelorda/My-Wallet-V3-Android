package piuk.blockchain.android.ui.activity.adapter

import android.widget.TextView
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.CryptoActivitySummaryItem
import piuk.blockchain.android.data.historicRate.HistoricRateFetcher
import piuk.blockchain.android.ui.activity.CryptoActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.util.visible
import timber.log.Timber

class ActivitiesDelegateAdapter(
    prefs: CurrencyPrefs,
    historicRateFetcher: HistoricRateFetcher,
    onCryptoItemClicked: (AssetInfo, String, CryptoActivityType) -> Unit,
    onFiatItemClicked: (String, String) -> Unit
) : DelegationAdapter<ActivitySummaryItem>(AdapterDelegatesManager(), emptyList()) {

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(NonCustodialActivityItemDelegate(prefs, historicRateFetcher, onCryptoItemClicked))
            addAdapterDelegate(SwapActivityItemDelegate(onCryptoItemClicked))
            addAdapterDelegate(CustodialTradingActivityItemDelegate(prefs, historicRateFetcher, onCryptoItemClicked))
            addAdapterDelegate(SellActivityItemDelegate(onCryptoItemClicked))
            addAdapterDelegate(CustodialFiatActivityItemDelegate(onFiatItemClicked))
            addAdapterDelegate(CustodialInterestActivityItemDelegate(prefs, historicRateFetcher, onCryptoItemClicked))
            addAdapterDelegate(CustodialRecurringBuyActivityItemDelegate(onCryptoItemClicked))
            addAdapterDelegate(CustodialSendActivityItemDelegate(prefs, historicRateFetcher, onCryptoItemClicked))
        }
    }
}

fun TextView.bindAndConvertFiatBalance(
    tx: CryptoActivitySummaryItem,
    disposables: CompositeDisposable,
    selectedFiatCurrency: String,
    historicRateFetcher: HistoricRateFetcher
) {
    disposables += tx.totalFiatWhenExecuted(selectedFiatCurrency, historicRateFetcher).observeOn(AndroidSchedulers.mainThread())
        .subscribeBy(
            onSuccess = {
                text = it.toStringWithSymbol()
                visible()
            },
            onError = {
                Timber.e("Cannot convert to fiat")
            }
        )
}

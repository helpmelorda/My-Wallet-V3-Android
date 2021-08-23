package piuk.blockchain.android.ui.dashboard.adapter

import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.resources.AssetResources

class PricesDelegateAdapter(
    prefs: CurrencyPrefs,
    onCardClicked: (AssetInfo) -> Unit,
//    analytics: Analytics,
//    coincore: Coincore,
    assetResources: AssetResources
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
//            addAdapterDelegate(...)
        }
    }
}
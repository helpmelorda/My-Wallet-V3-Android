package piuk.blockchain.android.ui.dashboard.adapter

import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.resources.AssetResources

class PricesDelegateAdapter(
    prefs: CurrencyPrefs,
    onPriceRequest: (AssetInfo) -> Unit,
    onCardClicked: (AssetInfo) -> Unit,
    assetResources: AssetResources
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        with(delegatesManager) {
            addAdapterDelegate(
                PriceCardDelegate(
                    prefs,
                    assetResources,
                    onPriceRequest,
                    onCardClicked
                )
            )
        }
    }
}
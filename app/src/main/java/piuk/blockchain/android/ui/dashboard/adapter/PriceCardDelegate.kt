package piuk.blockchain.android.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.core.price.ExchangeRate
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.databinding.ItemDashboardPriceCardBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.PricesItem
import piuk.blockchain.android.ui.dashboard.asDeltaPercent
import piuk.blockchain.android.ui.dashboard.format
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.setOnClickListenerDebounced

class PriceCardDelegate<in T>(
    private val prefs: CurrencyPrefs,
    private val assetResources: AssetResources,
    private val onPriceRequest: (AssetInfo) -> Unit,
    private val onCardClicked: (AssetInfo) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is PricesItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        PriceCardViewHolder(ItemDashboardPriceCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as PriceCardViewHolder).bind(
        items[position] as PricesItem,
        prefs.selectedFiatCurrency,
        assetResources,
        onPriceRequest,
        onCardClicked
    )
}

private class PriceCardViewHolder(
    private val binding: ItemDashboardPriceCardBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: PricesItem,
        fiatSymbol: String,
        assetResources: AssetResources,
        onPriceRequest: (AssetInfo) -> Unit,
        onCardClicked: (AssetInfo) -> Unit
    ) {
        with(binding) {
            assetResources.loadAssetIcon(icon, item.asset)
            currency.text = item.assetName
            root.setOnClickListenerDebounced { onCardClicked(item.asset) }

            if (item.priceWithDelta == null) {
                onPriceRequest(item.asset)
                price.text = UNKNOWN_PRICE
                priceDelta.text = UNKNOWN_PRICE
            } else {
                val rate = item.priceWithDelta.currentRate as ExchangeRate.CryptoToFiat
                price.text = rate.price().format(fiatSymbol)
                priceDelta.asDeltaPercent(item.priceWithDelta.delta24h)
            }
        }
    }

    private companion object {
        const val UNKNOWN_PRICE = "--"
    }
}

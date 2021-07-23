package piuk.blockchain.android.ui.dashboard.assetdetails.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import info.blockchain.balance.isCustodialOnly
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.databinding.DialogDashboardAssetLabelItemBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsItem
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class LabelItemDelegate(private val token: CryptoAsset) : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.AssetLabel

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        LabelViewHolder(
            DialogDashboardAssetLabelItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as LabelViewHolder).bind(token)
}

private class LabelViewHolder(
    val binding: DialogDashboardAssetLabelItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(token: CryptoAsset) {
        with(binding) {
            when {
                token.asset.isCustodialOnly -> {
                    root.visible()
                    assetLabelDescription.text = context.getString(
                        R.string.custodial_only_asset_label,
                        token.asset.ticker
                    )
                }
                else -> root.gone()
            }
        }
    }
}